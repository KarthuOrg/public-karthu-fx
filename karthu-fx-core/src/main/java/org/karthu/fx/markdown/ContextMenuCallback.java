package org.karthu.fx.markdown;

import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;

/**
 * What an application adds to a {@link MarkdownView}'s context menu: called on every opening of the menu, after the
 * view has put its own entries in place, with the menu's entries and the place the menu was requested at. Set on the
 * view with {@link MarkdownView#setContextMenuCallback(ContextMenuCallback)}.
 *
 * <p>The callback may add entries, remove and reorder the view's own ones. The view builds its entries anew on every
 * opening, so what the callback changes lasts for that opening only, and an entry it adds is added on each opening:
 * <pre>{@code
 * view.setContextMenuCallback((items, place) -> {
 *     MenuItem open = new MenuItem("Open in Editor");
 *     open.setOnAction(event -> openInEditor(place.sourceLine().orElse(1)));
 *     items.addAll(new SeparatorMenuItem(), open);
 * });
 * }</pre>
 * It is called on the JavaFX Application Thread, before the menu shows.
 *
 * @since 1.1.0-RC2
 */
@FunctionalInterface
public interface ContextMenuCallback {

    /**
     * Called when the view's context menu opens, after the view's own entries are in place.
     *
     * @param items the menu's entries, the view's own ones in place: the list the menu shows, to add to, remove from
     *              or reorder
     * @param place where the menu was requested
     * @since 1.1.0-RC2
     */
    void contextMenuRequested(ObservableList<MenuItem> items, ContextMenuPlace place);
}
