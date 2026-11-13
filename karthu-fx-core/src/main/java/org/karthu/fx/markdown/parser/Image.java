package org.karthu.fx.markdown.parser;

/**
 * An image: an inline image {@code ![alt](address "title")}. A reference-style image ({@code ![alt][label]}) is no
 * image, it stays literal text, as reference-style links do.
 *
 * @param destination the address as written in the source: the angle brackets of {@code <address>} removed and
 *                    backslash escapes resolved, nothing encoded or resolved; may be empty ({@code ![alt]()})
 * @param title       the image's title, backslash escapes resolved; empty when the image has none
 * @param alt         the alt text: the plain text of the image's description as CommonMark defines it, emphasis
 *                    without its markers, code without its backticks, a link as its text, an image inside as its
 *                    alt text, and line breaks as spaces, so it is one line
 */
public record Image(String destination, String title, String alt) implements Inline {
}
