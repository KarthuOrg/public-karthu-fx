package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * A bullet list: consecutive items with the same bullet ({@code -}, {@code *} or {@code +}).
 *
 * @param tight whether the list is tight in CommonMark's sense: no blank line between its items, and none between two
 *              blocks of one item
 * @param items the items in source order, at least one
 */
public record BulletList(boolean tight, List<ListItem> items) implements Block {

    /**
     * Copies the items, so the list cannot change afterwards.
     */
    public BulletList {
        items = List.copyOf(items);
    }
}
