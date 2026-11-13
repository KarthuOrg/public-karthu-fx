package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * An item of a bullet or ordered list: its content, parsed as blocks like a document's, so an item holds
 * paragraphs, nested lists and any other block.
 *
 * @param blocks the item's blocks in source order; empty for an empty item
 */
public record ListItem(List<Block> blocks) {

    /**
     * Copies the blocks, so the item cannot change afterwards.
     */
    public ListItem {
        blocks = List.copyOf(blocks);
    }
}
