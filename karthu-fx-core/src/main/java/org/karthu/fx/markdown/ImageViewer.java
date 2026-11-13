package org.karthu.fx.markdown;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

/**
 * The image area of the full size window ({@code markdown-image-viewer}): one whole decoded image, fitted into the
 * area or zoomed, which the reader zooms and moves.
 *
 * <p><b>Zoom.</b> The zoom counts screen pixels per image pixel: 1.0, 100 %, shows one image pixel per screen pixel,
 * pixel sharp, the image's pixel size divided by the output scale of the window's screen in the area's own pixels.
 * <em>Fitted</em> the image is as large as fits the area, never beyond 100 %, and follows the area's size; a new image
 * starts fitted, which shows a small image at 100 %.
 *
 * <p><b>Rendering.</b> At 100 % and beyond the image node shows the decoded image itself, which JavaFX enlarges. Below
 * 100 % it shows a bitmap of the image shrunk by an {@link AreaAverage} to its exact size in screen pixels, which the
 * window's work computes ({@link ImageWork#compute}); until then the last bitmap is scaled to the new size. An animated
 * GIF shows as the decoded image at every zoom, which plays, scaled by JavaFX. The image is placed on whole screen
 * pixels, so at 100 % each of its pixels is one screen pixel.
 *
 * <p><b>Place.</b> An image smaller than the area on a side lies in its middle on that side; a larger one covers the
 * area on that side, never leaving a gap at its edges.
 */
final class ImageViewer extends Region {

    /**
     * The factor a notch of the wheel zooms by.
     */
    private static final double WHEEL_STEP = 1.25;

    /**
     * The largest zoom, 800 %.
     */
    private static final double MAX_ZOOM = 8;

    /**
     * The image node: unmanaged, sized and placed by the viewer.
     */
    private final ImageView picture = new ImageView();

    // The zoom the viewer shows, for the window's bar; documented at shownZoomProperty().
    private final ReadOnlyDoubleWrapper shownZoom = new ReadOnlyDoubleWrapper(this, "shownZoom", 1);

    /**
     * Where the shrinking runs.
     */
    private final ImageWork work;

    /**
     * The wait for a zoom by the wheel or a resize to settle before the image is shrunk to its new size.
     */
    private final ImageWork.Settling settling;

    /**
     * Counts the wheel's notches for the zoom.
     */
    private final ZoomWheel wheel = new ZoomWheel();

    /**
     * The whole decoded image, {@code null} while the viewer shows none.
     */
    private Image image;

    /**
     * Whether the image is an animated GIF, which plays at every zoom, scaled by JavaFX: no bitmap of it is computed.
     */
    private boolean animation;

    /**
     * Whether the image is fitted into the area, else shown at {@link #zoom}.
     */
    private boolean fitted = true;

    /**
     * The zoom while the image is not fitted.
     */
    private double zoom = 1;

    /**
     * Where the image's top left corner lies in the area, as laid out last.
     */
    private double imageX;
    private double imageY;

    /**
     * Whether the next layout places the image in the middle of the area, instead of where it lay.
     */
    private boolean centred = true;

    /**
     * The latest shrinking of the image and the size it computes; a result of an older one is dropped.
     */
    private Object shrinking;
    private int shrinkingWidth;
    private int shrinkingHeight;

    /**
     * Whether the next layout shrinks the image at once, after a step of the zoom such as <em>Fit</em>, instead of
     * waiting for the size to settle.
     */
    private boolean shrinkAtOnce = true;

    /**
     * Whether a drag with the primary button moves the image, where it started in the scene and where the image lay.
     */
    private boolean dragging;
    private double dragStartX;
    private double dragStartY;
    private double dragImageX;
    private double dragImageY;

    /**
     * The output scale the viewer assumes in tests; 0 for the scale of its window.
     */
    private double assumedOutputScale;

    /**
     * Creates the viewer, empty.
     *
     * @param work where the shrinking of the image runs
     */
    ImageViewer(ImageWork work) {
        this.work = work;
        settling = work.settling(() -> shrink());
        getStyleClass().add("markdown-image-viewer");
        setCursor(Cursor.OPEN_HAND);
        // The whole area takes the mouse and the wheel, also where no image covers it.
        setPickOnBounds(true);
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> pressed(event));
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> dragged(event));
        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> released(event));
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> clicked(event));
        addEventHandler(ScrollEvent.SCROLL, event -> scrolled(event));
        picture.setManaged(false);
        picture.setSmooth(true);
        getChildren().add(picture);
        // The image never shows beyond the area, over the window's bar.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
    }

    /**
     * Shows a decoded image, fitted, or nothing; the image shown before is let go.
     *
     * @param decoded  the whole decoded image, {@code null} for none
     * @param animated whether the image is an animated GIF, which plays at every zoom
     */
    void setImage(Image decoded, boolean animated) {
        image = decoded;
        animation = animated;
        shrinking = null;
        picture.setImage(decoded);
        fit();
    }

    /**
     * The whole decoded image the viewer shows.
     *
     * @return the image, {@code null} while it shows none
     */
    Image image() {
        return image;
    }

    /**
     * Fits the image into the area, in its middle; from now on it follows the area's size.
     */
    void fit() {
        fitted = true;
        centred = true;
        shrinkAtOnce = true;
        requestLayout();
    }

    /**
     * Shows the image at 100 %, one image pixel per screen pixel, around the middle of the area.
     */
    void actualSize() {
        zoomAround(1, getWidth() / 2, getHeight() / 2);
        shrinkAtOnce = true;
    }

    /**
     * A press with the primary button starts moving the image, with a closed hand.
     */
    private void pressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        event.consume();
        dragging = true;
        dragStartX = event.getSceneX();
        dragStartY = event.getSceneY();
        dragImageX = imageX;
        dragImageY = imageY;
        setCursor(Cursor.CLOSED_HAND);
    }

    /**
     * The drag moves the image with the pointer, as far as it covers the area.
     */
    private void dragged(MouseEvent event) {
        if (!dragging) {
            return;
        }
        event.consume();
        imageX = dragImageX + event.getSceneX() - dragStartX;
        imageY = dragImageY + event.getSceneY() - dragStartY;
        requestLayout();
    }

    private void released(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            dragging = false;
            setCursor(Cursor.OPEN_HAND);
        }
    }

    /**
     * A double click with the primary button toggles between 100 % and fitted, keeping the clicked point of the
     * image under the pointer.
     */
    private void clicked(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2 || !event.isStillSincePress()) {
            return;
        }
        event.consume();
        if (!fitted && zoom == 1) {
            fit();
        } else {
            zoomAround(1, event.getX(), event.getY());
            shrinkAtOnce = true;
        }
    }

    /**
     * The shortcut key plus the wheel zooms around the pointer, by {@value #WHEEL_STEP} per notch, from the fitted
     * zoom up to 800 %; the wheel alone scrolls the image vertically, shift plus the wheel sideways, which JavaFX
     * mostly delivers as a horizontal scroll already.
     */
    private void scrolled(ScrollEvent event) {
        event.consume();
        if (image == null) {
            return;
        }
        if (event.isShortcutDown()) {
            int notches = wheel.notches(event);
            if (notches != 0) {
                zoomByWheel(notches, event.getX(), event.getY());
            }
            return;
        }
        wheel.reset();
        double deltaX = event.getDeltaX();
        double deltaY = event.getDeltaY();
        if (event.isShiftDown() && deltaX == 0) {
            deltaX = deltaY;
            deltaY = 0;
        }
        imageX += deltaX;
        imageY += deltaY;
        requestLayout();
    }

    /**
     * Zooms by notches of the wheel around a point; down to the fitted zoom, which then follows the area's size.
     */
    private void zoomByWheel(int notches, double x, double y) {
        double smallest = fittedZoom();
        double next = Math.clamp(currentZoom() * Math.pow(WHEEL_STEP, notches), smallest, MAX_ZOOM);
        if (next <= smallest) {
            fitted = true;
            centred = true;
            requestLayout();
        } else {
            zoomAround(next, x, y);
        }
    }

    /**
     * Shows the image at a zoom, keeping the point of the image at a point of the area there, as far as the image
     * covers the area.
     *
     * @param newZoom the zoom
     * @param x       the point in the area
     * @param y       the point in the area
     */
    private void zoomAround(double newZoom, double x, double y) {
        if (image == null) {
            return;
        }
        // The point as a fraction of the image where the next layout would place it: several zooms may come before it.
        double scale = outputScale();
        double oldWidth = screenWidth(currentZoom()) / scale;
        double oldHeight = screenHeight(currentZoom()) / scale;
        double left = centred ? (getWidth() - oldWidth) / 2 : place(imageX, oldWidth, getWidth());
        double top = centred ? (getHeight() - oldHeight) / 2 : place(imageY, oldHeight, getHeight());
        double fractionX = (x - left) / oldWidth;
        double fractionY = (y - top) / oldHeight;
        fitted = false;
        zoom = newZoom;
        imageX = x - fractionX * screenWidth(newZoom) / scale;
        imageY = y - fractionY * screenHeight(newZoom) / scale;
        centred = false;
        requestLayout();
    }

    /**
     * Sets the output scale the viewer assumes instead of its window's: a scene that is never shown has no screen.
     *
     * @param scale screen pixels per pixel of the area; 0 for the window's
     */
    void setOutputScale(double scale) {
        assumedOutputScale = scale;
        requestLayout();
    }

    /**
     * The zoom the viewer shows: screen pixels per image pixel, 1.0 for 100 %.
     *
     * @return the zoom property, read only
     */
    ReadOnlyDoubleProperty shownZoomProperty() {
        return shownZoom.getReadOnlyProperty();
    }

    /**
     * The output scale of the viewer's window, screen pixels per pixel of the area: 1.0 while it has none.
     */
    private double outputScale() {
        if (assumedOutputScale > 0) {
            return assumedOutputScale;
        }
        Scene scene = getScene();
        Window window = scene == null ? null : scene.getWindow();
        return window == null ? 1.0 : window.getOutputScaleX();
    }

    /**
     * The zoom at which the image fits the area, at most 1.0; 1.0 before the area has a size.
     */
    private double fittedZoom() {
        double width = getWidth();
        double height = getHeight();
        if (image == null || width <= 0 || height <= 0) {
            return 1;
        }
        double scale = outputScale();
        double fitting = Math.min(width * scale / image.getWidth(), height * scale / image.getHeight());
        return Math.min(1, fitting);
    }

    /**
     * The zoom shown now.
     */
    private double currentZoom() {
        return fitted ? fittedZoom() : zoom;
    }

    /**
     * The image's width in screen pixels at a zoom: below 100 % that of the bitmap, whole pixels, at least one.
     */
    private double screenWidth(double atZoom) {
        double pixels = image.getWidth() * atZoom;
        return atZoom < 1 ? Math.clamp(Math.round(pixels), 1, image.getWidth()) : pixels;
    }

    private double screenHeight(double atZoom) {
        double pixels = image.getHeight() * atZoom;
        return atZoom < 1 ? Math.clamp(Math.round(pixels), 1, image.getHeight()) : pixels;
    }

    /**
     * The image at the zoom shown, in the middle of the area or where it lay, on whole screen pixels.
     */
    @Override
    protected void layoutChildren() {
        if (image == null) {
            return;
        }
        double scale = outputScale();
        double atZoom = currentZoom();
        shownZoom.set(atZoom);
        double width = screenWidth(atZoom) / scale;
        double height = screenHeight(atZoom) / scale;
        if (centred) {
            imageX = (getWidth() - width) / 2;
            imageY = (getHeight() - height) / 2;
            centred = false;
        }
        imageX = place(imageX, width, getWidth());
        imageY = place(imageY, height, getHeight());
        // On whole screen pixels of the scene, wherever the area lies in it.
        Point2D origin = localToScene(0, 0);
        imageX = Math.round((origin.getX() + imageX) * scale) / scale - origin.getX();
        imageY = Math.round((origin.getY() + imageY) * scale) / scale - origin.getY();
        picture.setFitWidth(width);
        picture.setFitHeight(height);
        picture.relocate(imageX, imageY);
        showBitmap(atZoom);
    }

    /**
     * Where the image lies on one side: in the middle of a longer side of the area, else covering the area.
     */
    private static double place(double position, double size, double areaSize) {
        if (size <= areaSize) {
            return (areaSize - size) / 2;
        }
        return Math.clamp(position, areaSize - size, 0);
    }

    /**
     * Shows the image itself at 100 % and beyond, and an animated GIF at every zoom, which plays; below, a bitmap
     * shrunk to the image's size in screen pixels, which is computed anew when the one shown has another size.
     */
    private void showBitmap(double atZoom) {
        if (atZoom >= 1 || animation) {
            shrinking = null;
            picture.setImage(image);
            return;
        }
        if (shrinkAtOnce) {
            shrink();
        } else {
            settling.restart();
        }
    }

    /**
     * Shrinks the image to its size in screen pixels at the zoom shown, below 100 %, by the area average on the
     * window's work, unless the bitmap shown has that size or a shrinking to it is under way.
     */
    private void shrink() {
        shrinkAtOnce = false;
        double atZoom = currentZoom();
        if (image == null || atZoom >= 1 || animation) {
            return;
        }
        int width = (int) screenWidth(atZoom);
        int height = (int) screenHeight(atZoom);
        Image shown = picture.getImage();
        boolean fits = shown != image && shown.getWidth() == width && shown.getHeight() == height;
        boolean underWay = shrinking != null && shrinkingWidth == width && shrinkingHeight == height;
        if (fits || underWay) {
            return;
        }
        Image source = image;
        Object next = new Object();
        shrinking = next;
        shrinkingWidth = width;
        shrinkingHeight = height;
        work.compute(() -> {
            Image shrunk = AreaAverage.shrink(source, width, height);
            work.show(() -> {
                if (shrinking == next && image == source) {
                    picture.setImage(shrunk);
                }
            });
        });
    }
}
