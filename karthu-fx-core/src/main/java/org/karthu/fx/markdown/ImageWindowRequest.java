package org.karthu.fx.markdown;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * A request to open an image in the view's full size window: what the window shows, the image file's bytes, and its
 * title. The hover button over a block image, a click on an image and the image's context menu entry make one; the
 * view hands it to its full size window ({@link ImageWindow}), or in the view's tests to a recorder.
 *
 * @param title   the window's title, see {@link #of}
 * @param address the image's address as written in the Markdown
 * @param bytes   the bytes of the image file the loader returned, shared with the image, never changed;
 *                {@code null} if the loader has no file for the address
 */
record ImageWindowRequest(String title, String address, byte[] bytes) {

    /**
     * The name <em>Save as</em> suggests where the address names no file.
     */
    private static final String UNNAMED = "image";

    private static final String DATA = "data:";
    private static final String IMAGE_TYPE = "image/";

    /**
     * The characters no file name holds: the separators and those Windows refuses.
     */
    private static final String NOT_IN_FILE_NAMES = "/\\:*?\"<>|";

    /**
     * The request to open an image, titled by its alt text, else by the file name of its address: its last segment,
     * after the last {@code /} or {@code \}, without query and fragment, as written; else by the address itself. A
     * {@code data:} address names no file, and its content is no title: it is titled by its scheme and type, such as
     * {@code data:image/png}.
     *
     * @param alt     the image's alt text
     * @param address the image's address as written
     * @param bytes   the image file's bytes, {@code null} if there are none
     * @return the request
     */
    static ImageWindowRequest of(String alt, String address, byte[] bytes) {
        return new ImageWindowRequest(title(alt, address), address, bytes);
    }

    /**
     * The file name <em>Save as</em> suggests: the last segment of the address, after the last {@code /} or
     * {@code \}, without query and fragment, its percent escapes decoded as UTF-8 ({@code my%20shot.png} is
     * {@code my shot.png}); as written where it holds no valid escapes or decodes to no file name, such as
     * {@code a%2Fb.png}. A {@code data:} address names no file: its name is {@value #UNNAMED} and the extension of its
     * {@code image/} type ({@code image.png}, {@code image.svg} for {@code image/svg+xml}). An address that ends in a
     * slash is {@value #UNNAMED} too.
     *
     * @return the file name, never empty
     */
    String fileName() {
        if (address.toLowerCase(Locale.ROOT).startsWith(DATA)) {
            return UNNAMED + dataExtension();
        }
        String path = address.substring(0, endBefore(address, "?#"));
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String segment = path.substring(lastSlash + 1);
        if (segment.isEmpty()) {
            return UNNAMED;
        }
        String decoded = percentDecoded(segment);
        return decoded == null || !isFileName(decoded) ? segment : decoded;
    }

    /**
     * The extension of a {@code data:} address's type with its dot: the subtype of an {@code image/} type, in lower
     * case, without a suffix such as {@code +xml}; empty for any other type or none.
     */
    private String dataExtension() {
        String type = address.substring(DATA.length(), endBefore(address, ";,")).toLowerCase(Locale.ROOT);
        if (!type.startsWith(IMAGE_TYPE)) {
            return "";
        }
        String subtype = type.substring(IMAGE_TYPE.length(), endBefore(type, "+"));
        if (subtype.isEmpty()) {
            return "";
        }
        for (int i = 0; i < subtype.length(); i++) {
            char c = subtype.charAt(i);
            boolean plain = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_';
            if (!plain) {
                return "";
            }
        }
        return "." + subtype;
    }

    /**
     * A text with its percent escapes decoded as UTF-8; {@code null} if it holds none, an escape is not two hex
     * digits, or the bytes are no UTF-8.
     */
    private static String percentDecoded(String text) {
        if (text.indexOf('%') < 0) {
            return null;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int i = 0;
        while (i < text.length()) {
            int escape = text.indexOf('%', i);
            if (escape < 0) {
                escape = text.length();
            }
            // The plain run up to the next escape, whole, so a character outside the BMP stays one.
            bytes.writeBytes(text.substring(i, escape).getBytes(StandardCharsets.UTF_8));
            if (escape == text.length()) {
                break;
            }
            if (escape + 2 >= text.length()) {
                return null;
            }
            int high = Character.digit(text.charAt(escape + 1), 16);
            int low = Character.digit(text.charAt(escape + 2), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            bytes.write(high * 16 + low);
            i = escape + 3;
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray()))
                    .toString();
        } catch (CharacterCodingException notUtf8) {
            return null;
        }
    }

    /**
     * Whether a name is a file name on every platform the view runs on: no separator, none of the characters Windows
     * refuses, no control character.
     */
    private static boolean isFileName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < ' ' || c == '\u007F' || NOT_IN_FILE_NAMES.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    private static String title(String alt, String address) {
        if (!alt.isBlank()) {
            return alt;
        }
        if (address.toLowerCase(Locale.ROOT).startsWith("data:")) {
            return address.substring(0, endBefore(address, ";,"));
        }
        String path = address.substring(0, endBefore(address, "?#"));
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String fileName = path.substring(lastSlash + 1);
        return fileName.isEmpty() ? address : fileName;
    }

    /**
     * The index of the first of some characters in a text, its length if none is in it.
     */
    private static int endBefore(String text, String characters) {
        for (int i = 0; i < text.length(); i++) {
            if (characters.indexOf(text.charAt(i)) >= 0) {
                return i;
            }
        }
        return text.length();
    }
}
