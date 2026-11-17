package org.karthu.fx.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A group's line in the shortcut editor's list: the group, its commands' rows in their order, and whether the group
 * is open. Groups start open; their folding lives as long as the editor's working copy.
 */
final class ShortcutGroupEntry implements ShortcutListItem {

    private final CommandGroup group;

    private final List<ShortcutEntry> entries = new ArrayList<>();

    private boolean expanded = true;

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
}
