package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * An ordered list: consecutive items numbered with the same delimiter ({@code 1.} or {@code 1)}).
 *
 * @param start     the number of the first item, as the source gives it; the following items count on from it
 * @param delimiter the character after the numbers, {@code .} or {@code )}
 * @param tight     whether the list is tight in CommonMark's sense: no blank line between its items, and none between
 *                  two blocks of one item
 * @param items     the items in source order, at least one
 */
public record OrderedList(int start, char delimiter, boolean tight, List<ListItem> items) implements Block {

    /**
     * Copies the items, so the list cannot change afterwards.
     */
    public OrderedList {
        items = List.copyOf(items);
    }
}
