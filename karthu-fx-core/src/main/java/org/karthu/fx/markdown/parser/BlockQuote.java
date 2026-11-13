package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * A block quote: lines starting with {@code >}, whose content, without the markers, is parsed as blocks like a
 * document's, so a quote holds paragraphs, lists, further quotes and any other block.
 *
 * @param blocks the quote's blocks in source order; empty for an empty quote
 */
public record BlockQuote(List<Block> blocks) implements Block {

    /**
     * Copies the blocks, so the quote cannot change afterwards.
     */
    public BlockQuote {
        blocks = List.copyOf(blocks);
    }
}
