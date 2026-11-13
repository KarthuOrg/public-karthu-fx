package org.karthu.fx.markdown;

import java.io.IOException;

/**
 * Where the images of a {@link MarkdownView} come from: given an image's address, the loader returns the bytes of
 * the image file, which the view decodes and shows. An application sets it with
 * {@link MarkdownView#setImageLoader(ImageLoader)} to serve images from anywhere, for example from resources held in
 * memory.
 *
 * <p><b>The view's own loader.</b> A view starts with a loader of its own, which loads files, archive entries,
 * {@code data:} addresses and, when switched on, images from the internet, relative addresses against the view's base
 * address (see {@link MarkdownView#imageLoaderProperty()}). An application may wrap it, handling a few addresses itself
 * and leaving the rest to it: it keeps the view's loader from {@link MarkdownView#getImageLoader()} before it sets its
 * own, and calls it. The view's own loader keeps to the threading rules below.
 *
 * <p><b>Threading.</b> The view calls the loader on a background thread, never on the JavaFX Application Thread, so a
 * slow source never freezes the user interface, and possibly for several addresses at once: a loader must be safe to
 * call from several threads at the same time. It must not touch the scene graph. Whatever it needs from the view, it
 * reads before the view calls it, or in a thread-safe way. All views together run at most four loads at once, so a
 * loader that blocks for long holds back the images of every view. The view may call it again for an address it
 * loaded before: when the view's sources change (see {@link MarkdownView#imageLoaderProperty()}), and when the reader
 * opens the image of a table cell at full size. A result that arrives after the view has let the image go, for a new
 * text or a newer load of the same image, is dropped.
 *
 * <p><b>Results.</b> The bytes are those of an image file as JavaFX decodes it: PNG, JPEG, GIF and BMP. A loader that
 * has no image for an address throws an {@link IOException}, and the view shows the image as missing. So it does for
 * {@code null}, an empty array and a runtime exception, which it also logs as a warning with the address. Bytes JavaFX
 * cannot decode, such as an SVG or WebP file, show as an image whose format is not supported.
 */
@FunctionalInterface
public interface ImageLoader {

    /**
     * Returns the bytes of the image file at an address. Called on a background thread, see the class description.
     *
     * @param address the image's address as written in the Markdown, {@code ![alt](address)}: the angle brackets of
     *                {@code <address>} removed and backslash escapes resolved, nothing else changed, so it may be
     *                relative, absolute or anything an application understands
     * @return the bytes of the image file
     * @throws IOException when the loader has no image for the address, for example a file that does not exist
     */
    byte[] load(String address) throws IOException;
}
