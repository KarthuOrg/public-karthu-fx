package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * A paragraph: consecutive non-blank lines, also whatever block construct the parser does not know.
 *
 * @param content the paragraph's text, parsed into inlines
 * @param line    the 1-based number of the paragraph's first source line; for a task item's first paragraph, the
 *                item's line, where the marker stands
 */
public record Paragraph(List<Inline> content, int line) implements Block {

    /**
     * Copies the content, so the paragraph cannot change afterwards.
     */
    public Paragraph {
        content = List.copyOf(content);
    }
}
