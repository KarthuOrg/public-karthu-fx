package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * Strong emphasis, shown in bold: text between double {@code **} or {@code __} delimiters.
 *
 * @param content the strongly emphasised inlines
 */
public record Strong(List<Inline> content) implements Inline {

    /**
     * Copies the content, so the strong emphasis cannot change afterwards.
     */
    public Strong {
        content = List.copyOf(content);
    }
}
