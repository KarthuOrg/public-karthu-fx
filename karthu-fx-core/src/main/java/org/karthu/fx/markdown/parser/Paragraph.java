package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * A paragraph: consecutive non-blank lines, also whatever block construct the parser does not know.
 *
 * @param content the paragraph's text, parsed into inlines
 */
public record Paragraph(List<Inline> content) implements Block {

    /**
     * Copies the content, so the paragraph cannot change afterwards.
     */
    public Paragraph {
        content = List.copyOf(content);
    }
}
