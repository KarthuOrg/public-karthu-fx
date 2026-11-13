package org.karthu.fx.markdown;

/**
 * What can hold the view's one selection: a table's selected cells, or the text selected across the document
 * ({@link DocumentSelection}), code blocks, source blocks and whole tables included. Each reports every selection it
 * starts to the view, which clears the selection held before. The view's context menu and its shortcut key plus C
 * copy a selection through this interface too, so <em>Copy</em> works alike on both.
 */
interface SelectionHolder {

    /**
     * Clears the block's selection: nothing in it is selected afterwards.
     */
    void clearSelection();

    /**
     * Whether the block holds a selection that copying would copy.
     *
     * @return {@code true} if something in the block is selected
     */
    boolean hasSelection();

    /**
     * Copies the block's selection as the shortcut key plus C does; copies nothing without a selection.
     */
    void copySelection();
}
