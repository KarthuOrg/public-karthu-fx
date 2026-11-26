package org.karthu.fx.sidebar;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

import java.util.List;

/**
 * What the content of a side bar's panel may tell the side bar that shows it: the buttons for the panel's header,
 * the items for its menu, where the keyboard focus goes when the panel opens, and how to focus a search the content
 * has. A content node implements it to take part; every method has a default, so a node implements only what it
 * offers. A content node that does not implement it at all is shown as it is, with only the panel's own buttons and
 * items.
 *
 * <p>{@link NavigationTree} implements it: its header buttons are Expand All and Collapse All, it has no menu items,
 * and it takes the focus in its tree on the selected entry. An application's own content implements it the same
 * way, for example a list whose header offers a button that adds an entry:
 * <pre>{@code
 * class WorkflowList extends ListView<Workflow> implements SideBarContent {
 *     private final Button add = new Button();
 *
 *     public List<Node> getHeaderButtons() {
 *         return List.of(add);
 *     }
 * }
 * }</pre>
 *
 * <p>The side bar reads the buttons and items whenever it builds the panel's header or opens its menu, and puts them
 * there as they are: the content creates them once, styles them (a header button usually as an {@code icon-button})
 * and returns the same nodes on every call, since a node can sit in one place only. Like every karthu-fx control, it
 * is used on the JavaFX Application Thread only.
 *
 * @since 1.1.0-RC2
 */
public interface SideBarContent {

    /**
     * Returns the buttons the content offers for the header of its panel, left to right, after the panel's own
     * buttons. An application that places the content without a side bar may place them itself.
     *
     * @return the header buttons, the same nodes on every call; by default none
     * @since 1.1.0-RC2
     */
    default List<Node> getHeaderButtons() {
        return List.of();
    }

    /**
     * Returns the items the content offers for the menu of its panel, after the panel's own items and before the
     * side bar's built-in ones.
     *
     * @return the menu items, the same items on every call; by default none
     * @since 1.1.0-RC2
     */
    default List<MenuItem> getMenuItems() {
        return List.of();
    }

    /**
     * Puts the keyboard focus into the content, where the user starts working, as when its panel opens. By default
     * the content node itself requests the focus.
     *
     * @since 1.1.0-RC2
     */
    default void focusContent() {
        if (this instanceof Node node) {
            node.requestFocus();
        }
    }

    /**
     * Puts the keyboard focus into the content's search field, if it has one, ready for the user to type.
     *
     * @return true when the content has a search and focused it; by default false, as there is none
     * @since 1.1.0-RC2
     */
    default boolean focusSearch() {
        return false;
    }
}
