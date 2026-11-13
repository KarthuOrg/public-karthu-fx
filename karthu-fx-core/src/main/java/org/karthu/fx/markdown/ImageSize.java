package org.karthu.fx.markdown;

/**
 * The size of an image in the document's own pixels, those before the zoom: the one place of the size rule.
 *
 * <p><b>Natural size.</b> An image's pixel size divided by the output scale of the view's window, so that one image
 * pixel is one screen pixel: a screenshot shows as large as it was on the screen it was taken on.
 *
 * <p><b>Shown size.</b> The natural size, shrunk if needed to the width the text wraps at and to the visible height
 * at the current zoom, keeping the aspect ratio; an image is never enlarged. The zoom then scales the document, the
 * image with it, as it scales text.
 *
 * <p><b>Screen pixels.</b> The shown size times the zoom and the output scale: the size of the bitmap the view
 * computes for an image, so that each of its pixels is one screen pixel ({@link #pixels}).
 *
 * @param width  the width
 * @param height the height
 */
record ImageSize(double width, double height) {

    /**
     * The natural size of an image: its pixels as screen pixels.
     *
     * @param pixelWidth  the image's width in pixels
     * @param pixelHeight the image's height in pixels
     * @param scaleX      the window's horizontal output scale, screen pixels per document pixel
     * @param scaleY      the window's vertical output scale
     * @return the natural size in document pixels
     */
    static ImageSize natural(double pixelWidth, double pixelHeight, double scaleX, double scaleY) {
        return new ImageSize(pixelWidth / scaleX, pixelHeight / scaleY);
    }

    /**
     * This size shrunk to fit limits, keeping its aspect ratio; never enlarged. A limit of 0 or below, or not a
     * number, is no limit: a view not yet laid out has no width and no visible height.
     *
     * @param maxWidth  the width the text wraps at
     * @param maxHeight the visible height at the current zoom: the visible area's height divided by the zoom
     * @return the shown size
     */
    ImageSize fitted(double maxWidth, double maxHeight) {
        double factor = 1;
        if (maxWidth > 0 && width > maxWidth) {
            factor = maxWidth / width;
        }
        if (maxHeight > 0 && height * factor > maxHeight) {
            factor = maxHeight / height;
        }
        if (factor == 1) {
            return this;
        }
        return new ImageSize(width * factor, height * factor);
    }

    /**
     * The screen pixels this shown size covers: times the zoom and the output scale, each side rounded to whole
     * pixels, at least one and at most the image's own. A bitmap of this size shows one of its pixels per screen
     * pixel; where the zoom shows an image beyond its natural size, the bitmap stays the image itself, which JavaFX
     * enlarges.
     *
     * @param zoom        the document's zoom
     * @param scaleX      the window's horizontal output scale
     * @param scaleY      the window's vertical output scale
     * @param pixelWidth  the image's width in pixels
     * @param pixelHeight the image's height in pixels
     * @return the size in screen pixels
     */
    Pixels pixels(double zoom, double scaleX, double scaleY, int pixelWidth, int pixelHeight) {
        int pixelsWide = (int) Math.clamp(Math.round(width * zoom * scaleX), 1, pixelWidth);
        int pixelsHigh = (int) Math.clamp(Math.round(height * zoom * scaleY), 1, pixelHeight);
        return new Pixels(pixelsWide, pixelsHigh);
    }

    /**
     * A size in whole pixels.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    record Pixels(int width, int height) {
    }

    /**
     * What the size of an image, a block or inline, depends on beyond the image, at one layout of its node: the width
     * the text wraps at, the visible height at the zoom, the zoom and the output scale. The node notes them at each
     * layout; the decoding thread reads them to compute an image at the size it shows at.
     *
     * @param maxWidth  the width the text wraps at, in document pixels; 0 or below before the node is laid out
     * @param maxHeight the visible height at the current zoom, in document pixels; 0 before the view is laid out
     * @param zoom      the document's zoom
     * @param scaleX    the window's horizontal output scale
     * @param scaleY    the window's vertical output scale
     */
    record Limits(double maxWidth, double maxHeight, double zoom, double scaleX, double scaleY) {

        /**
         * The size an image of a pixel size shows at within these limits.
         *
         * @param pixelWidth  the image's width in pixels
         * @param pixelHeight the image's height in pixels
         * @return the shown size in document pixels
         */
        ImageSize shown(int pixelWidth, int pixelHeight) {
            return natural(pixelWidth, pixelHeight, scaleX, scaleY).fitted(maxWidth, maxHeight);
        }

        /**
         * The size in screen pixels of the bitmap that shows an image within these limits.
         *
         * @param pixelWidth  the image's width in pixels
         * @param pixelHeight the image's height in pixels
         * @return the bitmap's size, at most the image's
         */
        Pixels pixels(int pixelWidth, int pixelHeight) {
            return shown(pixelWidth, pixelHeight).pixels(zoom, scaleX, scaleY, pixelWidth, pixelHeight);
        }

        /**
         * The size the picture of an image takes within these limits, in document pixels: the shown size, made of
         * whole screen pixels where the image shrinks. A shrunk image's bitmap has the shown size in screen pixels,
         * rounded ({@link #pixels}), so the picture is that bitmap's size divided by the zoom and the output scale:
         * each bitmap pixel covers exactly one screen pixel, where the unrounded shown size would resample every row
         * and column. Where the image shows at its full size or beyond, its bitmap is the image itself, and the
         * picture is the shown size, which JavaFX enlarges the image to.
         *
         * @param pixelWidth  the image's width in pixels
         * @param pixelHeight the image's height in pixels
         * @return the picture's size in document pixels
         */
        ImageSize picture(int pixelWidth, int pixelHeight) {
            ImageSize shown = shown(pixelWidth, pixelHeight);
            Pixels bitmap = shown.pixels(zoom, scaleX, scaleY, pixelWidth, pixelHeight);
            if (bitmap.width() >= pixelWidth && bitmap.height() >= pixelHeight) {
                return shown;
            }
            return new ImageSize(bitmap.width() / (zoom * scaleX), bitmap.height() / (zoom * scaleY));
        }
    }
}
