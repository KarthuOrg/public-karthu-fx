package org.karthu.fx.markdown;

import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Where the full size window puts the image a reader copies with <em>Copy image</em>. The window writes through this
 * seam only; by default it puts the image on the system clipboard, and the window's tests replace it with a recorder
 * ({@code ImageWindowContent.setImageClipboard}, package-private), so no test touches the clipboard of the machine it
 * runs on. Not part of the API.
 */
@FunctionalInterface
interface ImageClipboard {

    /**
     * Puts an image on the clipboard.
     *
     * @param image the whole decoded image, never {@code null}
     */
    void put(Image image);

    /**
     * The system clipboard through JavaFX's clipboard: replaces its whole content with the image. Used on the JavaFX
     * Application Thread only, as the system clipboard requires.
     *
     * @return the system clipboard
     */
    static ImageClipboard system() {
        return image -> {
            ClipboardContent content = new ClipboardContent();
            content.putImage(image);
            Clipboard.getSystemClipboard().setContent(content);
        };
    }
}
