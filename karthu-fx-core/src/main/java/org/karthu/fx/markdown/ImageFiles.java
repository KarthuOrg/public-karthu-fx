package org.karthu.fx.markdown;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Decoding an image file and checking it against the pixel limit, the one place the images of the document
 * ({@link DocumentImages}) and the full size window ({@link ImageWindowContent}) share. Both run it on the view's
 * decoding thread.
 */
final class ImageFiles {

    private ImageFiles() {
    }

    /**
     * The image a file's bytes hold, decoded whole by JavaFX: a PNG, JPEG, GIF (an animated one with all its frames)
     * or BMP file.
     *
     * @param bytes the bytes of the image file
     * @return the decoded image, at least one pixel wide and high
     * @throws IOException when JavaFX cannot decode them, such as an SVG or WebP file or a damaged one, or they hold no
     *                     pixels; its message says why, for a log
     */
    static Image decode(byte[] bytes) throws IOException {
        Image image = new Image(new ByteArrayInputStream(bytes));
        if (image.isError()) {
            Exception failure = image.getException();
            throw new IOException(failure == null ? "not decodable" : failure.getMessage(), failure);
        }
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IOException("no pixels");
        }
        return image;
    }

    /**
     * Whether a decoded image has more pixels, width times height, than a limit.
     *
     * @param image the decoded image
     * @param limit the largest image in pixels that shows, 0 or less for no limit
     * @return {@code true} if the image is too large to show
     */
    static boolean aboveLimit(Image image, int limit) {
        return limit > 0 && image.getWidth() * image.getHeight() > limit;
    }
}
