package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * An ATX heading ({@code #} to {@code ######}).
 *
 * @param level   the heading level, 1 to 6
 * @param content the heading's text, parsed into inlines
 */
public record Heading(int level, List<Inline> content) implements Block {

    /**
     * Copies the content, so the heading cannot change afterwards.
     */
    public Heading {
        content = List.copyOf(content);
    }
}
