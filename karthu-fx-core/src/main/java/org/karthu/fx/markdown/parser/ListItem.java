package org.karthu.fx.markdown.parser;

import java.util.List;
import java.util.Objects;

/**
 * An item of a bullet or ordered list: its content, parsed as blocks like a document's, so an item holds
 * paragraphs, nested lists and any other block, and whether it is a GFM task list item.
 *
 * <p>A task item's marker ({@code [ ]}, {@code [x]}) is not part of its blocks: the first paragraph starts after it,
 * and an empty task ({@code - [ ]} alone) has no paragraph for it.
 *
 * @param blocks the item's blocks in source order; empty for an empty item
 * @param task   whether the item is a task, and if so, whether it is done
 */
public record ListItem(List<Block> blocks, Task task) {

    /**
     * Whether a list item is a GFM task list item, and its state.
     */
    public enum Task {

        /**
         * A plain item, no task.
         */
        NONE,

        /**
         * A task to do: {@code [ ]}.
         */
        OPEN,

        /**
         * A task done: {@code [x]} or {@code [X]}.
         */
        DONE
    }

    /**
     * Copies the blocks, so the item cannot change afterwards.
     *
     * @throws NullPointerException if {@code task} is {@code null}
     */
    public ListItem {
        blocks = List.copyOf(blocks);
        Objects.requireNonNull(task, "task");
    }

    /**
     * Creates a plain item, no task.
     *
     * @param blocks the item's blocks in source order; empty for an empty item
     */
    public ListItem(List<Block> blocks) {
        this(blocks, Task.NONE);
    }
}
