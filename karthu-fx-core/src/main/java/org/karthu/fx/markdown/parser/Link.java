package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * A link: an inline link {@code [text](address "title")}, an autolink {@code <https://...>} or a bare URL in the
 * text. An autolink's and a bare URL's text is their address.
 *
 * @param destination the address as written in the source: the angle brackets of {@code <address>} removed and
 *                    backslash escapes resolved, nothing encoded or resolved; may be empty ({@code [text]()})
 * @param title       the link's title, backslash escapes resolved; empty when the link has none
 * @param content     the link text; it never holds a link, but may hold images
 */
public record Link(String destination, String title, List<Inline> content) implements Inline {

    /**
     * Copies the content, so the link cannot change afterwards.
     */
    public Link {
        content = List.copyOf(content);
    }

    /**
     * An autolink or a bare URL: a link to an address as written, without a title, whose text is the address.
     *
     * @param address the address as written in the source
     * @return the link
     */
    static Link autolink(String address) {
        return new Link(address, "", List.of(new PlainText(address)));
    }
}
