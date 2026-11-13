package org.karthu.fx.markdown;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Shrinks an image by averaging pixel areas: each pixel of the shrunk image covers a rectangle of the source, and is
 * the average of the source pixels in it, each weighted by how much of it the rectangle covers. A screenshot shrunk
 * this way keeps its letters readable and its thin lines free of moire. Measured against an exact reference, it
 * matched where JavaFX's own scaling did not: an {@code ImageView} filters bilinearly without mipmaps, which picks
 * single pixels at large factors, and the loader's scaling averages boxes shifted by half a pixel.
 *
 * <p><b>Premultiplied.</b> The average is taken over premultiplied colours, so a transparent pixel adds no colour:
 * red beside transparent averages to half transparent red, not to dark red.
 *
 * <p><b>Memory.</b> The source is read one row at a time: a rectangle's weights are those of its columns times those
 * of its rows, so each source row is first reduced to the result's columns, then added to the result row it falls
 * in. Beside the source and the result, the work holds one row of each.
 */
final class AreaAverage {

    /**
     * Pixels as four 8-bit channels in an int, premultiplied: alpha in the highest byte, then red, green and blue.
     */
    private static final WritablePixelFormat<IntBuffer> FORMAT = PixelFormat.getIntArgbPreInstance();

    private AreaAverage() {
    }

    /**
     * An image shrunk to a size by averaging pixel areas. Any thread may call it.
     *
     * @param source the image, decoded
     * @param width  the width in pixels, 1 to the source's width
     * @param height the height in pixels, 1 to the source's height
     * @return the shrunk image
     */
    static WritableImage shrink(Image source, int width, int height) {
        int sourceWidth = (int) source.getWidth();
        int sourceHeight = (int) source.getHeight();
        Spans columns = new Spans(sourceWidth, width);
        Spans rows = new Spans(sourceHeight, height);
        PixelReader reader = source.getPixelReader();
        WritableImage result = new WritableImage(width, height);
        PixelWriter writer = result.getPixelWriter();

        int[] sourceRow = new int[sourceWidth];
        // A source row reduced to the result's columns: the weighted sums of its four channels per column.
        double[] reduced = new double[4 * width];
        int reducedRow = -1;
        double[] sums = new double[4 * width];
        int[] resultRow = new int[width];
        double area = columns.length() * rows.length();
        for (int y = 0; y < height; y++) {
            Arrays.fill(sums, 0);
            for (int sourceY = rows.first(y); sourceY <= rows.last(y); sourceY++) {
                // A source row on the border of two result rows counts for both: it is reduced once.
                if (sourceY != reducedRow) {
                    reader.getPixels(0, sourceY, sourceWidth, 1, FORMAT, sourceRow, 0, sourceWidth);
                    reduce(sourceRow, columns, reduced);
                    reducedRow = sourceY;
                }
                double weight = rows.weight(y, sourceY);
                for (int i = 0; i < sums.length; i++) {
                    sums[i] += weight * reduced[i];
                }
            }
            for (int x = 0; x < width; x++) {
                resultRow[x] = pixel(sums, x, area);
            }
            writer.setPixels(0, y, width, 1, FORMAT, resultRow, 0, width);
        }
        return result;
    }

    /**
     * Reduces a source row to the result's columns: for each, the sums of the channels of the source pixels it
     * covers, each weighted by its coverage.
     */
    private static void reduce(int[] sourceRow, Spans columns, double[] reduced) {
        int width = reduced.length / 4;
        for (int x = 0; x < width; x++) {
            double alpha = 0;
            double red = 0;
            double green = 0;
            double blue = 0;
            for (int sourceX = columns.first(x); sourceX <= columns.last(x); sourceX++) {
                double weight = columns.weight(x, sourceX);
                int argb = sourceRow[sourceX];
                alpha += weight * (argb >>> 24);
                red += weight * ((argb >> 16) & 0xFF);
                green += weight * ((argb >> 8) & 0xFF);
                blue += weight * (argb & 0xFF);
            }
            reduced[4 * x] = alpha;
            reduced[4 * x + 1] = red;
            reduced[4 * x + 2] = green;
            reduced[4 * x + 3] = blue;
        }
    }

    /**
     * A result pixel from the sums of its channels: each divided by the area it covers, rounded. A premultiplied
     * colour channel never exceeds alpha in the source, so its average never does, nor does its rounded average.
     */
    private static int pixel(double[] sums, int x, double area) {
        int alpha = channel(sums[4 * x] / area);
        int red = channel(sums[4 * x + 1] / area);
        int green = channel(sums[4 * x + 2] / area);
        int blue = channel(sums[4 * x + 3] / area);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int channel(double value) {
        return (int) Math.clamp(Math.round(value), 0, 255);
    }

    /**
     * How the pixels of a result's side cover the source's: result pixel {@code i} covers the source from
     * {@code i * sourceLength / length} to {@code (i + 1) * sourceLength / length}, a span of {@link #length()}
     * source pixels, all of them fully but perhaps the first and the last.
     */
    private static final class Spans {

        private final double length;
        private final int[] first;
        private final int[] last;
        private final double[] firstWeight;
        private final double[] lastWeight;

        Spans(int sourceLength, int resultLength) {
            length = (double) sourceLength / resultLength;
            first = new int[resultLength];
            last = new int[resultLength];
            firstWeight = new double[resultLength];
            lastWeight = new double[resultLength];
            for (int i = 0; i < resultLength; i++) {
                // Exact in a double for every image size, so a whole factor gives whole borders.
                double start = (double) i * sourceLength / resultLength;
                double end = (double) (i + 1) * sourceLength / resultLength;
                first[i] = (int) Math.floor(start);
                last[i] = Math.min(sourceLength, (int) Math.ceil(end)) - 1;
                firstWeight[i] = Math.min(first[i] + 1, end) - start;
                lastWeight[i] = end - Math.max(last[i], start);
            }
        }

        /**
         * How many source pixels a result pixel covers.
         */
        double length() {
            return length;
        }

        /**
         * The first source pixel result pixel {@code i} covers.
         */
        int first(int i) {
            return first[i];
        }

        /**
         * The last source pixel result pixel {@code i} covers.
         */
        int last(int i) {
            return last[i];
        }

        /**
         * How much of a source pixel result pixel {@code i} covers, 0 to 1: all of the pixels between its first and
         * its last.
         */
        double weight(int i, int sourceIndex) {
            if (sourceIndex == first[i]) {
                return firstWeight[i];
            }
            if (sourceIndex == last[i]) {
                return lastWeight[i];
            }
            return 1;
        }
    }
}
