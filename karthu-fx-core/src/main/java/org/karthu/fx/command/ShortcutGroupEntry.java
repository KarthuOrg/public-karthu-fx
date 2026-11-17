package org.karthu.fx.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A group's line in the shortcut editor's list: the group, its commands' rows in their order, and whether the group
 * is open. Groups start open; their folding lives as long as the editor's working copy. A search keeps a folding of its
 * own, so that the user's folding without a search is there again when the search is cleared.
 */
final class ShortcutGroupEntry implements ShortcutListItem {

    private final CommandGroup group;

    private final List<ShortcutEntry> entries = new ArrayList<>();

    /** Whether the group is open while no search is on: the user's folding, which a search leaves alone. */
    private boolean expanded = true;

    /** Whether the user folded the group while the current search shows. */
    private boolean foldedInSearch;

    /**
     * Creates the line of a group, open and without rows.
     *
     * @param group the group
     */
    ShortcutGroupEntry(CommandGroup group) {
        this.group = group;
    }

    /**
     * Returns the group.
     *
     * @return the group
     */
    CommandGroup getGroup() {
        return group;
    }

    /**
     * Returns the rows of the group's commands, in their order.
     *
     * @return the rows, unmodifiable
     */
    List<ShortcutEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Adds the row of a command after the rows added before.
     *
     * @param entry the row
     */
    void add(ShortcutEntry entry) {
        entries.add(entry);
    }

    /**
     * Returns whether the group is open, its rows shown under its line.
     *
     * @return true while the group is open
     */
    boolean isExpanded() {
        return expanded;
    }

    /**
     * Opens or folds the group.
     *
     * @param expanded true to open the group
     */
    void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * Returns whether the user folded the group while the current search shows: a search opens every group with a
     * match, and the user may fold one of them until the search changes.
     *
     * @return true while the group is folded in the current search
     */
    boolean isFoldedInSearch() {
        return foldedInSearch;
    }

    /**
     * Folds or opens the group in the current search.
     *
     * @param foldedInSearch true to fold the group while the current search shows
     */
    void setFoldedInSearch(boolean foldedInSearch) {
        this.foldedInSearch = foldedInSearch;
    }
}
