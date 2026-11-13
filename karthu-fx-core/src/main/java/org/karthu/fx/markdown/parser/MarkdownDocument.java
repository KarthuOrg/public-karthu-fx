package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * A parsed Markdown document: its top-level blocks in source order.
 *
 * <p>The document model is internal to karthu-fx (this package is not exported), so it can change with the parser.
 *
 * @param blocks the top-level blocks, never {@code null}
 */
public record MarkdownDocument(List<Block> blocks) {

    /**
     * Copies the blocks, so the document cannot change afterwards.
     */
    public MarkdownDocument {
        blocks = List.copyOf(blocks);
    }
}
