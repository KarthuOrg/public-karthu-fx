package org.karthu.fx.markdown;

import javafx.scene.image.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

/**
 * Image files made in memory for the tests of the view's images: JavaFX reads images but cannot write them, so the
 * tests write their own PNG files with the JDK's zip classes, hand their bytes to the view through a loader of their
 * own and read the shown image's pixels back.
 */
final class TestImages {

    /**
     * Opaque red, as an ARGB value.
     */
    static final int RED = 0xFFFF0000;

    /**
     * Opaque blue, as an ARGB value.
     */
    static final int BLUE = 0xFF0000FF;

    /**
     * The first bytes of every PNG file.
     */
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private TestImages() {
    }

    /**
     * A PNG file of 8-bit RGBA pixels.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     * @param argb   the colour of each pixel as an ARGB value, by its x and y
     * @return the file's bytes
     */
    static byte[] png(int width, int height, IntBinaryOperator argb) {
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.writeBytes(SIGNATURE);
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        writeInt(header, width);
        writeInt(header, height);
        // Bit depth 8, colour type 6 (RGBA), deflate, filter method 0, no interlace.
        header.writeBytes(new byte[] {8, 6, 0, 0, 0});
        chunk(file, "IHDR", header.toByteArray());
        chunk(file, "IDAT", pixelData(width, height, argb));
        chunk(file, "IEND", new byte[0]);
        return file.toByteArray();
    }

    /**
     * A PNG file of one colour.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     * @param argb   the colour as an ARGB value
     * @return the file's bytes
     */
    static byte[] png(int width, int height, int argb) {
        return png(width, height, (x, y) -> argb);
    }

    /**
     * An SVG file, a format JavaFX cannot decode.
     *
     * @return the file's bytes
     */
    static byte[] svg() {
        return ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\">"
                + "<rect width=\"10\" height=\"10\" fill=\"red\"/></svg>").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * A loader that serves files from a map by their address, as written in the Markdown, and has no image for any
     * other address.
     *
     * @param files the files by their address
     * @return the loader
     */
    static ImageLoader serving(Map<String, byte[]> files) {
        Map<String, byte[]> copy = new HashMap<>(files);
        return address -> {
            byte[] bytes = copy.get(address);
            if (bytes == null) {
                throw new IOException("No test image " + address);
            }
            return bytes;
        };
    }

    /**
     * The colours of an image's pixels at points.
     *
     * @param image  the image
     * @param points the points as x and y, one after the other
     * @return the ARGB values in the order of the points
     */
    static List<Integer> colors(Image image, int... points) {
        Integer[] colors = new Integer[points.length / 2];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = image.getPixelReader().getArgb(points[2 * i], points[2 * i + 1]);
        }
        return List.of(colors);
    }

    /**
     * The filtered scan lines of the pixels (filter type 0, none), deflated.
     */
    private static byte[] pixelData(int width, int height, IntBinaryOperator argb) {
        ByteArrayOutputStream deflated = new ByteArrayOutputStream();
        try (DeflaterOutputStream out = new DeflaterOutputStream(deflated)) {
            byte[] line = new byte[1 + 4 * width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = argb.applyAsInt(x, y);
                    line[1 + 4 * x] = (byte) (color >> 16);
                    line[2 + 4 * x] = (byte) (color >> 8);
                    line[3 + 4 * x] = (byte) color;
                    line[4 + 4 * x] = (byte) (color >>> 24);
                }
                out.write(line);
            }
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
        return deflated.toByteArray();
    }

    /**
     * Writes a chunk: its length, its type, its data and the CRC of type and data.
     */
    private static void chunk(ByteArrayOutputStream file, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        writeInt(file, data.length);
        file.writeBytes(typeBytes);
        file.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt(file, (int) crc.getValue());
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value >>> 24);
        out.write(value >>> 16);
        out.write(value >>> 8);
        out.write(value);
    }
}
