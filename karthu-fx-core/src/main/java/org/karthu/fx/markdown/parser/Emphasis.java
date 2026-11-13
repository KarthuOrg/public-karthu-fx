package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * Emphasis, shown in italics: text between single {@code *} or {@code _} delimiters.
 *
 * @param content the emphasised inlines
 */
public record Emphasis(List<Inline> content) implements Inline {

    /**
     * Copies the content, so the emphasis cannot change afterwards.
     */
    public Emphasis {
        content = List.copyOf(content);
    }
}
