package org.karthu.fx.markdown;

/**
 * A block around other blocks, a block quote or a list item, as the copied text of a document selection writes it: a
 * prefix before each line of the blocks inside ({@link SelectionText}). Each container is its own object, compared by
 * identity: two segments share a container only if they lie in the same quote or item.
 */
abstract sealed class BlockContainer permits BlockContainer.Quote, BlockContainer.Item {

    /**
     * The prefix of a line of a block inside this container.
     *
     * @param leaf      the block the line belongs to: its segment, or another node of a leaf block
     * @param firstLine whether the line is the block's first line
     * @return the prefix
     */
    abstract String prefix(Object leaf, boolean firstLine);

    /**
     * The prefix of a line between two blocks of this container, such as the blank line between two paragraphs of a
     * quote, before trailing spaces are dropped.
     *
     * @return the prefix
     */
    abstract String continuation();

    /**
     * Told of every leaf block (not a list or a quote) inside this container as it is rendered, in document order.
     *
     * @param leaf the block's segment, or another node of a leaf block
     */
    void leafRendered(Object leaf) {
    }

    /**
     * A block quote: every line inside it starts with {@code > }.
     */
    static final class Quote extends BlockContainer {

        @Override
        String prefix(Object leaf, boolean firstLine) {
            return "> ";
        }

        @Override
        String continuation() {
            return "> ";
        }
    }

    /**
     * A list item: the first line of its first block starts with the item's marker, every other line inside it is
     * indented by two spaces, so a nested list's items are indented two spaces per level.
     */
    static final class Item extends BlockContainer {

        private final String marker;
        private final boolean tight;

        /**
         * The first leaf block inside the item, whose first line takes the marker; {@code null} until one is rendered.
         */
        private Object firstLeaf;

        /**
         * Creates an item.
         *
         * @param marker the marker as copied: {@code -} for a bullet, the number and delimiter for an ordered item
         * @param tight  whether the item's list is tight, its paragraphs written without blank lines between them
         */
        Item(String marker, boolean tight) {
            this.marker = marker;
            this.tight = tight;
        }

        /**
         * Whether the item's list is tight: no blank lines around its paragraphs.
         *
         * @return {@code true} for a tight list's item
         */
        boolean tight() {
            return tight;
        }

        @Override
        String prefix(Object leaf, boolean firstLine) {
            return firstLine && leaf == firstLeaf ? marker + " " : "  ";
        }

        @Override
        String continuation() {
            return "  ";
        }

        @Override
        void leafRendered(Object leaf) {
            if (firstLeaf == null) {
                firstLeaf = leaf;
            }
        }
    }
}
