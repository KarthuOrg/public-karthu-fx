package org.karthu.fx.markdown;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

/**
 * The view's own image loader, the initial value of {@link MarkdownView#imageLoaderProperty()}: it reads image files,
 * archive entries and {@code data:} addresses, and downloads images from the internet while the view allows it.
 *
 * <p><b>Addresses.</b> In this order:
 * <ul>
 *   <li>a {@code data:} address with base64 content of an {@code image/} type is decoded, any other is missing;</li>
 *   <li>an absolute file path of the platform, such as {@code C:\docs\a.png} or {@code /home/me/a.png}, is read;</li>
 *   <li>any other address is a URI, the characters a URI does not allow percent-encoded first ({@link #encode}): an
 *       absolute one as it is, a relative one resolved against the view's base address as a browser resolves it
 *       against a page, missing without a base. Its scheme decides: {@code file:} reads a file, {@code jar:} an entry
 *       of an archive file, {@code http:} and {@code https:} download while the view's internet switch is on; any
 *       other scheme is missing.</li>
 * </ul>
 * A file or an entry is read without the address's query and fragment, as a browser reads it, so
 * {@code shot.png?raw=true} finds {@code shot.png}. A {@code jar:} address reads an archive file only, never one on the
 * internet, which the internet switch would not stop.
 *
 * <p><b>Threading.</b> The view's base address and internet switch are mirrored here by the view on the JavaFX
 * Application Thread, into volatile fields the loading threads read at each call: the loader follows the view's
 * properties at the time of the call, however an application keeps or wraps it. It holds no other state, so several
 * threads may call it at once.
 */
final class ViewImageLoader implements ImageLoader {

    /**
     * The characters besides ASCII letters and digits a URI allows as they are: the unreserved marks and the reserved
     * delimiters. {@code %}, {@code #} and the square brackets have rules of their own, see {@link #encode}.
     */
    private static final String URI_CHARACTERS = "-_.!~*'();/?:@&=+$,";

    private static final String HEX_DIGITS = "0123456789ABCDEF";

    /**
     * The scheme of an address that holds the file itself, in lower case; it matches in any case.
     */
    private static final String DATA = "data:";

    /**
     * How long a download waits for the server's connection, in milliseconds: the number the view's JavaDoc names.
     */
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;

    /**
     * How long a download waits for the server's next bytes, in milliseconds: the number the view's JavaDoc names.
     */
    private static final int READ_TIMEOUT_MILLIS = 10_000;

    /**
     * The view's base address, {@code null} for none.
     */
    private volatile String base;

    /**
     * The view's internet switch: whether images load from {@code http:} and {@code https:} addresses.
     */
    private volatile boolean remote;

    /**
     * Follows the view's base address.
     *
     * @param base the base address, {@code null} for none
     */
    void setBase(String base) {
        this.base = base;
    }

    /**
     * Follows the view's internet switch.
     *
     * @param remote whether images load from {@code http:} and {@code https:} addresses
     */
    void setRemote(boolean remote) {
        this.remote = remote;
    }

    @Override
    public byte[] load(String address) throws IOException {
        if (address.regionMatches(true, 0, DATA, 0, DATA.length())) {
            return decodeData(address);
        }
        Path path = absolutePath(address);
        if (path != null) {
            return Files.readAllBytes(path);
        }
        URI target = target(address);
        String scheme = target.getScheme().toLowerCase(Locale.ROOT);
        return switch (scheme) {
            case "file" -> readFile(target);
            case "jar" -> readJarEntry(target);
            case "http", "https" -> download(target);
            default -> throw new IOException("The view loads no image from a " + scheme + ": address: " + address);
        };
    }

    /**
     * The bytes a {@code data:} address holds: {@code data:image/<type>[;<parameter>...];base64,<content>}. Any other
     * kind, text or content that is not base64, has no image.
     */
    private static byte[] decodeData(String address) throws IOException {
        int comma = address.indexOf(',');
        if (comma < 0) {
            throw new IOException("The data: address holds no content");
        }
        String[] header = address.substring(DATA.length(), comma).split(";", -1);
        String type = header[0].trim().toLowerCase(Locale.ROOT);
        if (!type.startsWith("image/")) {
            throw new IOException("The data: address holds no image but '" + type + "'");
        }
        if (header.length < 2 || !header[header.length - 1].trim().equalsIgnoreCase("base64")) {
            throw new IOException("The data: address holds no base64 content");
        }
        try {
            return Base64.getDecoder().decode(address.substring(comma + 1));
        } catch (IllegalArgumentException damaged) {
            throw new IOException("The data: address holds damaged base64 content: " + damaged.getMessage(), damaged);
        }
    }

    /**
     * The file an address names as an absolute file path of the platform; {@code null} for any other address. An
     * address that starts with {@code //} is a URI's reference to another host, never a path.
     */
    private static Path absolutePath(String address) {
        if (address.startsWith("//")) {
            return null;
        }
        try {
            Path path = Path.of(address);
            return path.isAbsolute() ? path : null;
        } catch (InvalidPathException notAPath) {
            return null;
        }
    }

    /**
     * The absolute address of an image: the address itself if it is absolute, else the address resolved against the
     * base.
     */
    private URI target(String address) throws IOException {
        URI uri = uri(address);
        if (uri.isAbsolute()) {
            return uri;
        }
        String baseAddress = base;
        if (baseAddress == null) {
            throw new IOException("The relative address " + address + " has no base to resolve against");
        }
        return resolve(uri(baseAddress), uri);
    }

    /**
     * A relative reference resolved against a base, as a browser resolves it against a page. A {@code jar:} address
     * is opaque to {@link URI#resolve}, so its entry's path, after the archive's {@code !/}, is resolved on its own.
     */
    private static URI resolve(URI base, URI relative) throws IOException {
        if (!"jar".equalsIgnoreCase(base.getScheme())) {
            return base.resolve(relative);
        }
        String spec = base.getRawSchemeSpecificPart();
        // The last one, as the JDK's own handler of jar: addresses resolves relative ones.
        int separator = spec.lastIndexOf("!/");
        if (separator < 0) {
            throw new IOException("The base " + base + " names no entry of an archive");
        }
        URI entry = uri(spec.substring(separator + 1)).resolve(relative);
        if (entry.getRawAuthority() != null) {
            throw new IOException("The address " + relative + " names a host, which a jar: base has not");
        }
        String query = entry.getRawQuery() == null ? "" : "?" + entry.getRawQuery();
        return uri("jar:" + spec.substring(0, separator + 1) + entry.getRawPath() + query);
    }

    /**
     * An address as a URI, the characters a URI does not allow encoded first.
     */
    private static URI uri(String address) throws IOException {
        try {
            return new URI(encode(address));
        } catch (URISyntaxException invalid) {
            throw new IOException("The address " + address + " is no valid URI: " + invalid.getMessage(), invalid);
        }
    }

    /**
     * An address with every character a URI does not allow percent-encoded as UTF-8, as a browser encodes an address
     * before it resolves it: spaces, characters beyond ASCII, a {@code %} that starts no escape, a second {@code #},
     * and square brackets outside the host, where they enclose an IPv6 address. Escapes stay as written, so encoding
     * an encoded address changes nothing.
     */
    private static String encode(String address) {
        int hostStart = hostStart(address);
        int hostEnd = hostStart;
        while (hostStart >= 0 && hostEnd < address.length() && "/?#".indexOf(address.charAt(hostEnd)) < 0) {
            hostEnd++;
        }
        StringBuilder encoded = new StringBuilder(address.length());
        boolean inFragment = false;
        int i = 0;
        while (i < address.length()) {
            int character = address.codePointAt(i);
            boolean allowed;
            if (character == '%') {
                allowed = i + 2 < address.length() && isHexDigit(address.charAt(i + 1))
                        && isHexDigit(address.charAt(i + 2));
            } else if (character == '#') {
                allowed = !inFragment;
                inFragment = true;
            } else if (character == '[' || character == ']') {
                allowed = i >= hostStart && i < hostEnd;
            } else {
                allowed = isAsciiLetter(character) || isAsciiDigit(character)
                        || (character < 128 && URI_CHARACTERS.indexOf(character) >= 0);
            }
            if (allowed) {
                encoded.appendCodePoint(character);
            } else {
                for (byte octet : Character.toString(character).getBytes(StandardCharsets.UTF_8)) {
                    encoded.append('%');
                    encoded.append(HEX_DIGITS.charAt((octet >> 4) & 0xF));
                    encoded.append(HEX_DIGITS.charAt(octet & 0xF));
                }
            }
            i += Character.charCount(character);
        }
        return encoded.toString();
    }

    /**
     * Where the host of an address starts, after {@code scheme://} or a leading {@code //}; -1 if it names none.
     */
    private static int hostStart(String address) {
        if (address.startsWith("//")) {
            return 2;
        }
        int colon = 0;
        while (colon < address.length() && isSchemeCharacter(address.charAt(colon), colon == 0)) {
            colon++;
        }
        if (colon > 0 && address.startsWith("://", colon)) {
            return colon + 3;
        }
        return -1;
    }

    /**
     * Whether a character may stand in a scheme: a letter first, then letters, digits, {@code +}, {@code -} and
     * {@code .}.
     */
    private static boolean isSchemeCharacter(char character, boolean first) {
        if (first) {
            return isAsciiLetter(character);
        }
        return isAsciiLetter(character) || isAsciiDigit(character) || character == '+' || character == '-'
                || character == '.';
    }

    private static boolean isAsciiLetter(int character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }

    private static boolean isAsciiDigit(int character) {
        return character >= '0' && character <= '9';
    }

    private static boolean isHexDigit(char character) {
        return isAsciiDigit(character) || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }

    /**
     * The bytes of a file at a {@code file:} address, without its query and fragment.
     */
    private static byte[] readFile(URI address) throws IOException {
        String authority = address.getRawAuthority() == null ? "" : "//" + address.getRawAuthority();
        String path = address.getRawPath() == null ? "" : address.getRawPath();
        Path file;
        try {
            file = Path.of(uri("file:" + authority + path));
        } catch (IllegalArgumentException notAFile) {
            throw new IOException("The address " + address + " names no file: " + notAFile.getMessage(), notAFile);
        }
        return Files.readAllBytes(file);
    }

    /**
     * The bytes of an archive's entry at a {@code jar:} address, without its query and fragment. The archive must be
     * a file: an archive on the internet would be downloaded by the JDK's handler past the internet switch. The
     * connection uses no cache, so the archive is closed with the stream and not held open, which on Windows would
     * lock its file.
     */
    private static byte[] readJarEntry(URI address) throws IOException {
        String spec = address.getRawSchemeSpecificPart();
        int separator = spec.lastIndexOf("!/");
        if (separator < 0 || !spec.regionMatches(true, 0, "file:", 0, "file:".length())) {
            throw new IOException("The address " + address + " names no entry of an archive file");
        }
        int query = spec.indexOf('?', separator);
        if (query >= 0) {
            spec = spec.substring(0, query);
        }
        URLConnection connection = uri("jar:" + spec).toURL().openConnection();
        connection.setUseCaches(false);
        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * The bytes of a file on the internet, without the address's fragment, only while the internet switch is on;
     * else no request leaves.
     */
    private byte[] download(URI address) throws IOException {
        if (!remote) {
            throw new IOException("Images from the internet are switched off: " + address);
        }
        URI withoutFragment = uri(address.getScheme() + ":" + address.getRawSchemeSpecificPart());
        URLConnection connection = withoutFragment.toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
