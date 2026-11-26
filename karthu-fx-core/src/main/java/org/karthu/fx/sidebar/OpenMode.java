package org.karthu.fx.sidebar;

/**
 * How the user opens an entry of a {@link NavigationTree}: by a double click or Enter, as in a file manager or an
 * IDE's project view, or by selecting it, as in a list of pages where every entry shows at once.
 *
 * <p>In either mode the disclosure arrow folds and unfolds a folder and opens nothing, and the tree's own keys (the
 * arrow keys, Home, End, Page Up and Page Down) move the selection as JavaFX's tree view does.
 *
 * @since 1.1.0-RC2
 */
public enum OpenMode {

    /**
     * A double click or Enter opens an entry; selecting it opens nothing. On an entry with children the double click
     * or Enter also folds or unfolds it. The default of a navigation tree.
     *
     * @since 1.1.0-RC2
     */
    DOUBLE_CLICK,

    /**
     * Selecting an entry opens it: a click, or an arrow key that moves the selection, whenever the user's gesture
     * changes the selection. A single click on an entry with children also folds or unfolds it, and so does Enter,
     * which selects the entry the keyboard focus is on, as a click would.
     *
     * @since 1.1.0-RC2
     */
    SELECTION
}
