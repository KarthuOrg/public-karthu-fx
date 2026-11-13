package org.karthu.fx.markdown;

import java.util.Arrays;

/**
 * The frames of a GIF file, told from its bytes: whether it is animated, a GIF of more than one frame, and the file of
 * its first frame alone. The file's blocks are walked, never decoded: the header and the global colour table, then
 * extensions and images, each made of sub-blocks that carry their own length, up to the trailer.
 */
final class GifFrames {

    /**
     * The length of a GIF's header and logical screen descriptor: {@code GIF89a}, the screen's size, its flags, the
     * background colour and the aspect ratio.
     */
    private static final int HEADER_LENGTH = 13;

    /**
     * The length of an image descriptor: its introducer, its place, its size and its flags.
     */
    private static final int IMAGE_DESCRIPTOR_LENGTH = 10;

    private static final int EXTENSION = 0x21;
    private static final int IMAGE = 0x2C;
    private static final byte TRAILER = 0x3B;

    /**
     * The flag of a colour table, in the screen's and in an image's flags, and the bits of its size.
     */
    private static final int COLOUR_TABLE = 0x80;
    private static final int COLOUR_TABLE_SIZE = 0x07;

    private GifFrames() {
    }

    /**
     * Whether a file is an animated GIF: a GIF file with more than one frame.
     *
     * @param bytes the file's bytes
     * @return {@code true} if it is; {@code false} for one frame or any other file
     */
    static boolean isAnimated(byte[] bytes) {
        return endOfFrame(bytes, 2) >= 0;
    }

    /**
     * A GIF file of its first frame alone: the file up to the end of its first image's data, everything before it
     * included, followed by the trailer. JavaFX decodes it as a still image, the first frame as the animation shows it.
     *
     * @param bytes the bytes of a GIF file
     * @return the bytes of the first frame's file; the bytes given if they hold no whole first frame
     */
    static byte[] firstFrame(byte[] bytes) {
        int end = endOfFrame(bytes, 1);
        if (end < 0 || end > bytes.length) {
            return bytes;
        }
        byte[] first = Arrays.copyOf(bytes, end + 1);
        first[end] = TRAILER;
        return first;
    }

    /**
     * Where a frame of a GIF file ends, after its image data: a frame begins with its image descriptor, which may
     * follow extensions. The walk stops at the trailer, the file's end or a byte that starts no block.
     *
     * @param bytes the file's bytes
     * @param frame the frame's number, from 1
     * @return the index after the frame's last byte, beyond the file's end if the file ends inside it; -1 for a file
     *         that is no GIF or has fewer frames
     */
    private static int endOfFrame(byte[] bytes, int frame) {
        if (!isGif(bytes)) {
            return -1;
        }
        int frames = 0;
        int position = HEADER_LENGTH + colourTableLength(bytes[10]);
        while (position < bytes.length) {
            int block = bytes[position] & 0xFF;
            if (block == EXTENSION) {
                // The introducer and the label, then the extension's sub-blocks.
                position = afterSubBlocks(bytes, position + 2);
            } else if (block == IMAGE) {
                frames++;
                int flags = position + IMAGE_DESCRIPTOR_LENGTH - 1 < bytes.length
                        ? bytes[position + IMAGE_DESCRIPTOR_LENGTH - 1] : 0;
                // The descriptor, the local colour table, the LZW code size, then the image data's sub-blocks.
                position = afterSubBlocks(bytes, position + IMAGE_DESCRIPTOR_LENGTH + colourTableLength(flags) + 1);
                if (frames == frame) {
                    return position;
                }
            } else {
                // The trailer, or a byte that starts no block.
                return -1;
            }
        }
        return -1;
    }

    /**
     * Whether the bytes start as a GIF file: the signature and a whole logical screen descriptor.
     */
    private static boolean isGif(byte[] bytes) {
        return bytes.length >= HEADER_LENGTH && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
    }

    /**
     * The length in bytes of the colour table a screen's or an image's flags announce: three bytes per colour, two to
     * the power of one more than the size bits; none without the flag.
     */
    private static int colourTableLength(int flags) {
        if ((flags & COLOUR_TABLE) == 0) {
            return 0;
        }
        return 3 * (1 << ((flags & COLOUR_TABLE_SIZE) + 1));
    }

    /**
     * Where the sub-blocks starting at a position end: each is its length in a byte and that many bytes, the last one
     * of length zero.
     */
    private static int afterSubBlocks(byte[] bytes, int start) {
        int position = start;
        while (position < bytes.length) {
            int length = bytes[position] & 0xFF;
            position += 1 + length;
            if (length == 0) {
                return position;
            }
        }
        return position;
    }
}
