package org.karthu.fx.markdown;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * What the full size window shows ({@code markdown-image-window}): a bar on top and the image area below it, the
 * viewer ({@link ImageViewer}). The window's stage ({@link ImageWindow}) holds it; the view's tests lay it out in a
 * scene that is never shown.
 *
 * <p><b>Bar.</b> <em>Fit</em> ({@code markdown-image-fit}) fits the image into the area, <em>100 %</em>
 * ({@code markdown-image-actual-size}) shows it at one image pixel per screen pixel, and a label
 * ({@code markdown-image-zoom}) shows the zoom in whole percent. At the bar's right end <em>Copy image</em>
 * ({@code markdown-image-copy}) puts the whole decoded image on the clipboard, of an animated GIF its first frame as
 * a still image, enabled while an image shows, and
 * <em>Save as</em> ({@code markdown-image-save}) asks for a file, suggesting the file name of the address
 * ({@link ImageWindowRequest#fileName()}), and writes the image file's bytes there unchanged, enabled while the window
 * has the file, also one it cannot show. The file is written in the background, as a load is; if writing fails, an
 * error dialog titled like the button names the file and the reason. The labels are the view's text properties. The
 * clipboard, the file chooser and the error dialog are package-private switches, which the window's tests replace with
 * recorders.
 *
 * <p><b>Showing an image.</b> A request to open brings the image file's bytes; the content lets go of the image it
 * showed, decodes the new one whole on the decoding thread and shows it fitted; an animated GIF plays, whatever the
 * view's {@link MarkdownView#imageAnimationProperty() setting for the document}. Without bytes, or with bytes JavaFX
 * cannot decode or of more pixels than the view's limit, the area shows a note ({@code markdown-image-note}): the
 * view's note of a missing, unsupported or too large image. When the window closes, the content lets go of its image.
 *
 * <p><b>Keys.</b> Escape closes the window.
 */
final class ImageWindowContent extends BorderPane {

    private final MarkdownView view;
    private final ImageNotes notes;
    private final ImageWork work;
    private final ImageViewer viewer;
    private final HBox bar;
    private final Label note = new Label();

    /**
     * <em>Copy image</em>: enabled while an image shows.
     */
    private final Button copy = new Button();

    /**
     * <em>Save as</em>: enabled while the window has the image's file, also one it cannot show.
     */
    private final Button save = new Button();

    /**
     * The request shown or being decoded; a decode of an older one is dropped.
     */
    private ImageWindowRequest current;

    /**
     * Whether the image shown is an animated GIF, told on the decoding thread with its decode: <em>Copy image</em>
     * copies its first frame.
     */
    private boolean shownAnimated;

    /**
     * Where <em>Copy image</em> puts the image: the system clipboard, a recorder in tests.
     */
    private ImageClipboard imageClipboard = ImageClipboard.system();

    /**
     * Where <em>Save as</em> asks for the file: JavaFX's file chooser, a recorder in tests.
     */
    private SaveDialog saveDialog = SaveDialog.fileChooser();

    /**
     * Where a failed <em>Save as</em> is reported: karthu-fx's error dialog, a recorder in tests.
     */
    private ErrorDialog errorDialog = ErrorDialog.customDialog();

    /**
     * Creates the content of a view's full size window.
     *
     * @param view  the view, whose labels the bar follows
     * @param notes the view's notes of an image that cannot be shown and its pixel limit, which the window keeps to
     * @param work  where the decoding and the shrinking run, and the writing of a file the reader saves
     * @param close closes the window
     */
    ImageWindowContent(MarkdownView view, ImageNotes notes, ImageWork work, Runnable close) {
        this.view = view;
        this.notes = notes;
        this.work = work;
        viewer = new ImageViewer(work);
        getStyleClass().add("markdown-image-window");

        Button fit = new Button();
        fit.getStyleClass().add("markdown-image-fit");
        fit.textProperty().bind(view.fitImageTextProperty());
        fit.setOnAction(event -> viewer.fit());
        Button actualSize = new Button();
        actualSize.getStyleClass().add("markdown-image-actual-size");
        actualSize.textProperty().bind(view.actualImageSizeTextProperty());
        actualSize.setOnAction(event -> viewer.actualSize());
        Label zoom = new Label();
        zoom.getStyleClass().add("markdown-image-zoom");
        zoom.textProperty().bind(viewer.shownZoomProperty().map(
                factor -> ZoomPercent.format((int) Math.round(factor.doubleValue() * 100))));
        // The bar's right end: the buttons that copy and save the image.
        Region rest = new Region();
        HBox.setHgrow(rest, Priority.ALWAYS);
        copy.getStyleClass().add("markdown-image-copy");
        copy.textProperty().bind(view.copyImageTextProperty());
        copy.setDisable(true);
        copy.setOnAction(event -> copyImage());
        save.getStyleClass().add("markdown-image-save");
        save.textProperty().bind(view.saveImageTextProperty());
        save.setDisable(true);
        save.setOnAction(event -> saveImage());
        bar = new HBox(fit, actualSize, zoom, rest, copy, save);
        bar.getStyleClass().add("markdown-image-bar");
        setTop(bar);

        note.getStyleClass().add("markdown-image-note");
        note.setVisible(false);
        setCenter(new StackPane(viewer, note));

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                close.run();
            }
        });
    }

    /**
     * Shows the image of a request to open, fitted, letting go of the image shown before. The image is decoded on the
     * decoding thread; once it shows, or the note why it cannot, a callback runs.
     *
     * @param request the request
     * @param shown   called on the JavaFX Application Thread once the image or the note shows
     */
    void show(ImageWindowRequest request, Runnable shown) {
        setCurrent(request);
        showImage(null, false);
        note.setVisible(false);
        byte[] bytes = request.bytes();
        if (bytes == null) {
            showNote(notes.missing().getValue());
            shown.run();
            return;
        }
        int limit = notes.pixelLimit().get();
        work.compute(() -> {
            Image decoded = decode(bytes);
            boolean animated = GifFrames.isAnimated(bytes);
            work.show(() -> {
                if (current != request) {
                    return;
                }
                if (decoded == null) {
                    showNote(notes.unsupported().getValue());
                } else if (ImageFiles.aboveLimit(decoded, limit)) {
                    showNote(notes.tooLarge().getValue());
                } else {
                    showImage(decoded, animated);
                }
                shown.run();
            });
        });
    }

    /**
     * Lets go of the image shown, and of one being decoded: the window has closed.
     */
    void clear() {
        setCurrent(null);
        showImage(null, false);
        note.setVisible(false);
    }

    /**
     * Sets the output scale the content assumes instead of its window's: a scene that is never shown has no screen.
     *
     * @param scale screen pixels per pixel of the content; 0 for the window's
     */
    void setOutputScale(double scale) {
        viewer.setOutputScale(scale);
    }

    /**
     * Lays the image out anew: the window has moved to a screen of another output scale.
     */
    void outputScaleChanged() {
        viewer.requestLayout();
    }

    /**
     * Whether the content shows an image, not a note.
     *
     * @return {@code true} once an image is decoded and shown
     */
    boolean showsImage() {
        return viewer.image() != null;
    }

    /**
     * The width of the image shown.
     *
     * @return its width in pixels, 0 while none shows
     */
    double imagePixelWidth() {
        return showsImage() ? viewer.image().getWidth() : 0;
    }

    /**
     * The height of the image shown.
     *
     * @return its height in pixels, 0 while none shows
     */
    double imagePixelHeight() {
        return showsImage() ? viewer.image().getHeight() : 0;
    }

    /**
     * The height the bar asks for, with the theme's styles applied.
     *
     * @return the height
     */
    double barHeight() {
        return snapSizeY(bar.prefHeight(-1));
    }

    /**
     * The width the bar asks for, with the theme's styles applied: every label whole, in the view's language.
     *
     * @return the width
     */
    double barWidth() {
        return snapSizeX(bar.prefWidth(-1));
    }

    /**
     * Replaces where <em>Copy image</em> puts the image, by default the system clipboard. The window's tests put a
     * recorder here, so they never write to the system clipboard; the seam is no part of the API.
     *
     * @param clipboard the clipboard every copy from now on goes to
     */
    void setImageClipboard(ImageClipboard clipboard) {
        this.imageClipboard = clipboard;
    }

    /**
     * Replaces where <em>Save as</em> asks for the file to write, by default JavaFX's file chooser. The window's tests
     * put a recorder here, so they never open a real file chooser; the seam is no part of the API.
     *
     * @param dialog the dialog every save from now on asks
     */
    void setSaveDialog(SaveDialog dialog) {
        this.saveDialog = dialog;
    }

    /**
     * <em>Save as</em>: asks for a file, suggesting the file name of the address, and writes the image file's bytes
     * there as the loader returned them, in the background, as a load is: a slow disk or share never stops the
     * application. A failure comes back to the error dialog.
     */
    private void saveImage() {
        ImageWindowRequest request = current;
        if (request == null || request.bytes() == null) {
            return;
        }
        String title = view.getSaveImageText();
        Path file = saveDialog.choose(ownerWindow(), title, request.fileName());
        if (file == null) {
            return;
        }
        work.load(() -> {
            try {
                Files.write(file, request.bytes());
            } catch (IOException failure) {
                String reason = reason(failure);
                work.show(() -> errorDialog.show(ownerWindow(), title, file.toString(), reason));
            }
        });
    }

    /**
     * Replaces where a failed <em>Save as</em> is reported, by default karthu-fx's error dialog. The window's tests put
     * a recorder here, so they never open a real dialog; the seam is no part of the API.
     *
     * @param dialog the dialog every failed save from now on reports to
     */
    void setErrorDialog(ErrorDialog dialog) {
        this.errorDialog = dialog;
    }

    /**
     * Why writing a file failed, for the reader: the system's reason where it gives one, such as a file another
     * program holds, else the message, else the kind of failure, such as {@code AccessDeniedException}. A file
     * system's failure without a reason names only the file in its message, which the dialog names already.
     *
     * @param failure the failure
     * @return the reason
     */
    static String reason(IOException failure) {
        if (failure instanceof FileSystemException fileSystemFailure) {
            String reason = fileSystemFailure.getReason();
            return reason == null || reason.isBlank() ? failure.getClass().getSimpleName() : reason;
        }
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    /**
     * The window the content is in, {@code null} while it has none.
     */
    private Window ownerWindow() {
        Scene scene = getScene();
        return scene == null ? null : scene.getWindow();
    }

    /**
     * Takes the request the window shows, or none; <em>Save as</em> saves its file.
     */
    private void setCurrent(ImageWindowRequest request) {
        current = request;
        save.setDisable(request == null || request.bytes() == null);
    }

    /**
     * Shows a decoded image in the viewer, or none; <em>Copy image</em> copies the one shown.
     *
     * @param image    the whole decoded image, {@code null} for none
     * @param animated whether it is an animated GIF, which plays
     */
    private void showImage(Image image, boolean animated) {
        viewer.setImage(image, animated);
        shownAnimated = animated;
        copy.setDisable(image == null);
    }

    /**
     * <em>Copy image</em>: puts the whole decoded image the window shows on the clipboard. Of an animated GIF its first
     * frame: JavaFX reads no pixels of a playing image, which the clipboard needs, so the first frame's file
     * ({@link GifFrames#firstFrame}) is decoded as a still image on the decoding thread, never the playing image.
     */
    private void copyImage() {
        ImageWindowRequest request = current;
        Image image = viewer.image();
        if (request == null || image == null) {
            return;
        }
        if (!shownAnimated) {
            imageClipboard.put(image);
            return;
        }
        byte[] bytes = request.bytes();
        work.compute(() -> {
            Image firstFrame = decode(GifFrames.firstFrame(bytes));
            if (firstFrame != null) {
                work.show(() -> imageClipboard.put(firstFrame));
            }
        });
    }

    private void showNote(String text) {
        note.setText(text);
        note.setVisible(true);
    }

    /**
     * The image decoded from a file's bytes, whole; {@code null} if JavaFX cannot decode them.
     */
    private static Image decode(byte[] bytes) {
        try {
            return ImageFiles.decode(bytes);
        } catch (IOException undecodable) {
            return null;
        }
    }
}
