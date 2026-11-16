package org.karthu.fx.markdown;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * An image of the document as the view shows it, a block of its own ({@link ImageBlock}) or inside a line of text
 * ({@link InlineImage}): the one place of what both share. Until the image is decoded it shows a placeholder
 * ({@code markdown-image-placeholder}); then the image ({@code markdown-image}) at its shown size ({@link ImageSize}),
 * or the error box ({@code markdown-image-error}) if it cannot be shown. The kind decides how the placeholder and the
 * error box look and how wide the text is that the image shrinks to.
 *
 * <p><b>Bitmap.</b> The node keeps the bytes of the image file and a bitmap at the size the image shows at in screen
 * pixels, never the whole decoded image unless it shows at its full size. Each layout notes the node's
 * {@link ImageSize.Limits limits} for the decoding thread and fits the last bitmap to the new shown size; while that
 * bitmap no longer has the size in screen pixels, the view's images compute a new one once the size has settled. A
 * playing animated GIF is the exception: the node shows the decoded GIF itself, which JavaFX scales to every shown
 * size, and never computes a bitmap of it.
 *
 * <p><b>Loads.</b> Each load of the image, the first and every reload, is a token ({@link #newLoad()}). The node
 * keeps the bytes of a load with its token and shows the results of its latest load only ({@link #wants(Object)});
 * it knows which load what it shows comes from ({@link #shows(Object)}), so a new bitmap of the same load is a
 * recompute at the same size, and a result of a newer load replaces what it shows.
 *
 * <p><b>Selection.</b> While the view's selection includes the image, the node shows the selection's highlight over
 * what it shows ({@link #showSelected(boolean)}), whatever that is and however it changes.
 */
abstract sealed class ImageNode extends Region permits ImageBlock, InlineImage {

    private final String address;
    private final String title;
    private final String alt;

    /**
     * The view's images, which know the output scale and the visible height.
     */
    private final DocumentImages images;

    /**
     * Where the node comes among all images of all views: in document order within a view.
     */
    private final long order;

    /**
     * What the node shows instead of the image: the placeholder until the image is decoded, the error box if it
     * cannot be shown; {@code null} while it shows the image.
     */
    private Node box;

    /**
     * The image's node, {@code null} until it is decoded. Unmanaged: the node sizes it in its layout, and a new size
     * of it asks for no layout of the node again.
     */
    private ImageView picture;

    /**
     * The image's size in pixels, known once it is decoded.
     */
    private int pixelWidth;
    private int pixelHeight;

    /**
     * Whether the image node shows an animated GIF as JavaFX decoded it, which plays and which JavaFX scales to the
     * shown size: no bitmap of it is ever computed.
     */
    private boolean playing;

    /**
     * The load what the node shows comes from, the image or the error box; {@code null} while it shows the
     * placeholder. Read and set on the JavaFX thread.
     */
    private Object shownLoad;

    /**
     * Whether the view has replaced the node's document, so its image is no longer wanted. Set on the JavaFX thread,
     * read by the background work.
     */
    private volatile boolean discarded;

    /**
     * The latest load of the image, whose result alone the node shows: a reload's result replaces what it shows, an
     * older load's result arriving later is dropped. Set on the JavaFX thread, read by the background work.
     */
    private volatile Object load;

    /**
     * The bytes of the image file with the load they come from, {@code null} until a load has returned them. Set on
     * the JavaFX thread, read by the decoding thread.
     */
    private volatile Loaded loaded;

    /**
     * The limits of the node's last layout, {@code null} before its first. Set on the JavaFX thread, read by the
     * decoding thread.
     */
    private volatile ImageSize.Limits limits;

    /**
     * Whether the node lies in or near the visible area of its view, so its decode goes first. Set on the JavaFX
     * thread, read by the decoding thread.
     */
    private volatile boolean near;

    /**
     * Whether a decode of the image waits in the queue: a node waits there once.
     */
    private final AtomicBoolean decodeQueued = new AtomicBoolean();

    /**
     * The address of the link around the image, {@code null} outside a link.
     */
    private String linkAddress;

    /**
     * What a click on a linked image does: it gets the link's address.
     */
    private Consumer<String> linkAction;

    /**
     * The selection's highlight over what the node shows, {@code null} until a selection first includes the image.
     */
    private HighlightPath selectionHighlight;

    /**
     * What the highlight lies over, the picture or the box, followed as it changes its bounds; {@code null} before the
     * first selection.
     */
    private Node highlighted;

    private final InvalidationListener followHighlighted = observable -> placeSelectionHighlight();

    /**
     * Creates the node of an image, showing its placeholder.
     *
     * @param address     the image's address as written
     * @param title       the image's title, empty if none
     * @param alt         the image's alt text
     * @param images      the view's images
     * @param order       where the node comes among all images: in document order within a view
     * @param styleClass  the style class of the kind
     * @param placeholder what the node shows until the image is decoded
     */
    ImageNode(String address, String title, String alt, DocumentImages images, long order, String styleClass,
              Node placeholder) {
        this.address = address;
        this.title = title;
        this.alt = alt;
        this.images = images;
        this.order = order;
        getStyleClass().add(styleClass);
        // The pointer is over the node only where it is over what the node shows, the picture, a box or a block's
        // open button: a block is as wide as the text, and the empty space beside a narrow image is the document's,
        // where a press selects and a click opens nothing. The node has no background, so it picks nothing itself.
        setPickOnBounds(false);
        box = placeholder;
        getChildren().add(box);
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> clickedToOpen(event));
    }

    /**
     * Opens the image at full size on a single click with the primary button without moving since the press, as a
     * link follows such a click, while the node shows the image: a press followed by a drag selects and opens
     * nothing. A handler on the node, so the link around a linked image, whose handler on what the node shows
     * consumes the click, follows it instead.
     */
    private void clickedToOpen(MouseEvent event) {
        if (StillClick.matches(event) && showsPicture()) {
            event.consume();
            images.open(this);
        }
    }

    /**
     * Makes the image a link's: what it shows, the placeholder, the image or the error box, follows the link as the
     * link's text does, see {@link #followsLink(Node)}.
     *
     * @param address the link's address as written
     * @param action  called with the address when the image is clicked
     */
    final void linkTo(String address, Consumer<String> action) {
        linkAddress = address;
        linkAction = action;
        followsLink(picture != null ? picture : box);
    }

    /**
     * Makes what the node shows follow the link around the image, if there is one, as a run of the link's text does:
     * a hand cursor, and a single click with the primary button without moving since the press hands the address to
     * the link action; the second click of a double click and a click that ends a drag follow nothing. An image
     * without a title of its own shows the link's address as its tooltip, as the link's text does.
     */
    private void followsLink(Node content) {
        if (linkAddress == null) {
            return;
        }
        content.setCursor(Cursor.HAND);
        content.setOnMouseClicked(event -> {
            if (StillClick.matches(event)) {
                event.consume();
                linkAction.accept(linkAddress);
            }
        });
        if (content == picture && title.isEmpty()) {
            Tooltip.install(content, new Tooltip(linkAddress));
        }
    }

    /**
     * The error box of the kind, shown instead of the image.
     *
     * @param note why the image cannot be shown, a text property of the view, followed when it changes
     * @return the box, with the style class {@code markdown-image-error}
     */
    abstract Node errorBox(ObservableValue<String> note);

    /**
     * The image's address as written in the Markdown.
     *
     * @return the address
     */
    final String address() {
        return address;
    }

    /**
     * The image's alt text.
     *
     * @return the alt text
     */
    final String alt() {
        return alt;
    }

    /**
     * The view's images, which know the limits of the image's size beyond the width of the text.
     *
     * @return the view's images
     */
    final DocumentImages images() {
        return images;
    }

    /**
     * Where the node comes among all images of all views, the order of decodes after nearness.
     *
     * @return the node's number, ascending in document order within a view
     */
    final long order() {
        return order;
    }

    /**
     * Whether the node lies in or near the visible area of its view, so its decode goes first.
     *
     * @return {@code true} if it does
     */
    final boolean isNear() {
        return near;
    }

    /**
     * Notes whether the node lies in or near the visible area of its view.
     *
     * @param nearVisibleArea {@code true} if it does
     */
    final void setNear(boolean nearVisibleArea) {
        near = nearVisibleArea;
    }

    /**
     * Whether the image is no longer wanted: the view shows another text.
     *
     * @return {@code true} once the node's document is replaced
     */
    final boolean isDiscarded() {
        return discarded;
    }

    /**
     * Marks the image as no longer wanted: its pending work does nothing.
     */
    final void discard() {
        discarded = true;
    }

    /**
     * Starts a load of the image, which replaces any load before it: a reload, when where images come from has
     * changed. Called on the JavaFX thread.
     *
     * @return the load, which its work hands to {@link #wants(Object)}
     */
    final Object newLoad() {
        Object next = new Object();
        load = next;
        return next;
    }

    /**
     * Whether the node still wants the result of a load: its document is shown and no newer load has started.
     *
     * @param result the load the result comes from
     * @return {@code true} if the result is to be shown
     */
    final boolean wants(Object result) {
        return !discarded && load == result;
    }

    /**
     * Whether what the node shows, the image or the error box, comes from a load.
     *
     * @param result the load
     * @return {@code true} if it does; {@code false} while the node shows its placeholder or an older load's result
     */
    final boolean shows(Object result) {
        return shownLoad == result;
    }

    /**
     * The bytes of the image file with the load they come from and whether they are an animated GIF, which every
     * decode reads.
     *
     * @return the bytes and their load, {@code null} until a load has returned them
     */
    final Loaded loaded() {
        return loaded;
    }

    /**
     * Keeps the bytes a load returned, for its first decode and every one after a change of the size.
     *
     * @param result    the load the bytes come from
     * @param fileBytes the bytes of the image file
     * @param animated  whether the bytes are an animated GIF, told once in the background with the load
     */
    final void setLoaded(Object result, byte[] fileBytes, boolean animated) {
        loaded = new Loaded(result, fileBytes, animated);
    }

    /**
     * The limits of the node's last layout, which a decode computes the bitmap for.
     *
     * @return the limits, {@code null} before the node's first layout
     */
    final ImageSize.Limits limits() {
        return limits;
    }

    /**
     * Notes that a decode of the image waits in the queue.
     *
     * @return {@code true} if none waited, so the caller queues one
     */
    final boolean queueDecode() {
        return decodeQueued.compareAndSet(false, true);
    }

    /**
     * Notes that the decode waiting in the queue has started: a later change of the size queues another, which reads
     * the limits of that time.
     */
    final void decodeStarted() {
        decodeQueued.set(false);
    }

    /**
     * Whether the shown bitmap has the size in screen pixels the image shows at within the node's last limits.
     *
     * @return {@code true} if it has, if the node shows no image, or if it plays an animated GIF, which JavaFX scales
     */
    final boolean bitmapFits() {
        if (picture == null || limits == null || playing) {
            return true;
        }
        ImageSize.Pixels wanted = limits.pixels(pixelWidth, pixelHeight);
        Image bitmap = picture.getImage();
        return bitmap.getWidth() == wanted.width() && bitmap.getHeight() == wanted.height();
    }

    /**
     * Shows a bitmap of the image. A bitmap of the load the node shows already replaces the last one at the same
     * shown size, so the node needs no new layout. A bitmap of another load replaces what the node shows, the
     * placeholder, the error box or an older image, with the image's title, if it has one, as its tooltip and its alt
     * text as its accessible text.
     *
     * @param bitmap      the bitmap, the decoded image or a shrunk one
     * @param imageWidth  the image's width in pixels
     * @param imageHeight the image's height in pixels
     * @param result      the load the image comes from
     * @param animation   whether the bitmap is an animated GIF as JavaFX decoded it, which plays at every size
     */
    final void showBitmap(Image bitmap, int imageWidth, int imageHeight, Object result, boolean animation) {
        playing = animation;
        if (picture != null && shownLoad == result) {
            picture.setImage(bitmap);
            return;
        }
        pixelWidth = imageWidth;
        pixelHeight = imageHeight;
        ImageView view = new ImageView(bitmap);
        view.getStyleClass().add("markdown-image");
        view.setManaged(false);
        view.setSmooth(true);
        view.setAccessibleText(alt);
        if (!title.isEmpty()) {
            Tooltip.install(view, new Tooltip(title));
        }
        picture = view;
        box = null;
        shownLoad = result;
        followsLink(view);
        showContent(view);
        pictureShown();
        requestLayout();
    }

    /**
     * Called when the node has begun to show a new image, its node and the selection's highlight its only children: a
     * kind adds what lies over the image, a block its open button.
     */
    void pictureShown() {
    }

    /**
     * Shows the error box of the kind instead of the image.
     *
     * @param note   why the image cannot be shown, a text property of the view, followed when it changes
     * @param result the load the error comes from
     */
    final void showError(ObservableValue<String> note, Object result) {
        Node error = errorBox(note);
        picture = null;
        box = error;
        shownLoad = result;
        followsLink(error);
        showContent(error);
        requestLayout();
    }

    /**
     * Shows a node in place of what the node showed, the selection's highlight over it if there is one.
     */
    private void showContent(Node content) {
        getChildren().setAll(content);
        if (selectionHighlight != null) {
            getChildren().add(selectionHighlight);
            placeSelectionHighlight();
        }
    }

    // --- The selection's highlight -----------------------------------------------------------------------------------

    /**
     * Shows the selection's highlight over what the node shows, the image, the placeholder or the error box, or no
     * longer: a selection includes the image. The highlight ({@code markdown-selection}) is the one of selected text,
     * which the theme makes see-through over an image, so the image shows under it. It is made on the first selection
     * and kept after it, so an image never selected has none.
     *
     * @param selected whether a selection includes the image
     */
    final void showSelected(boolean selected) {
        if (selected && selectionHighlight == null) {
            selectionHighlight = new HighlightPath("markdown-selection");
            getChildren().add(selectionHighlight);
        }
        if (selectionHighlight != null) {
            selectionHighlight.setVisible(selected);
            placeSelectionHighlight();
        }
    }

    /**
     * Lays the highlight over what the node shows, and keeps it there as that changes its size or place: a picture
     * fitted anew, a box laid out.
     */
    private void placeSelectionHighlight() {
        Node content = picture != null ? picture : box;
        if (content != highlighted) {
            if (highlighted != null) {
                highlighted.boundsInParentProperty().removeListener(followHighlighted);
            }
            content.boundsInParentProperty().addListener(followHighlighted);
            highlighted = content;
        }
        Bounds bounds = content.getBoundsInParent();
        selectionHighlight.getElements().setAll(new MoveTo(bounds.getMinX(), bounds.getMinY()),
                new LineTo(bounds.getMaxX(), bounds.getMinY()), new LineTo(bounds.getMaxX(), bounds.getMaxY()),
                new LineTo(bounds.getMinX(), bounds.getMaxY()), new ClosePath());
    }

    /**
     * What the node shows instead of the image: the placeholder or the error box.
     *
     * @return the box, {@code null} while the node shows the image
     */
    final Node box() {
        return box;
    }

    /**
     * Whether the node shows the image.
     *
     * @return {@code true} once a bitmap of it is shown
     */
    final boolean showsPicture() {
        return picture != null;
    }

    /**
     * The size the image shows at within limits, made of whole screen pixels where it shrinks, so that each pixel of
     * its bitmap covers one screen pixel ({@link ImageSize.Limits#picture}).
     *
     * @param current the limits
     * @return the shown size; only meaningful while the node shows the image
     */
    final ImageSize shownSize(ImageSize.Limits current) {
        return current.picture(pixelWidth, pixelHeight);
    }

    /**
     * Notes the limits of a layout and whether the node lies near the visible area, and fits the image, if shown, to
     * its shown size within them at a point of the node; a bitmap that no longer has the size in screen pixels is
     * scaled to the shown size meanwhile, and the view's images compute a new one once the size has settled.
     *
     * @param current the limits of this layout
     * @param left    the image's left edge in the node
     * @param top     the image's top edge in the node
     */
    final void layOutPicture(ImageSize.Limits current, double left, double top) {
        limits = current;
        near = images.isNear(this);
        if (picture != null) {
            ImageSize size = shownSize(current);
            picture.setFitWidth(size.width());
            picture.setFitHeight(size.height());
            picture.relocate(left, top);
            if (!bitmapFits()) {
                images.bitmapOutdated(this);
            }
        }
    }

    /**
     * The bytes of an image file and the load they come from.
     *
     * @param load     the load
     * @param bytes    the bytes of the image file
     * @param animated whether the bytes are an animated GIF ({@link GifFrames#isAnimated}), told once in the
     *                 background with the load, so the JavaFX Application Thread never walks the file
     */
    record Loaded(Object load, byte[] bytes, boolean animated) {
    }
}
