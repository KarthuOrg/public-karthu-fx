package org.karthu.fx.command;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import org.karthu.fx.form.search.SearchQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The shortcut editor's working copy of a registry's keys: a line for every group and a row for every command, read
 * from the registry, holding the keys the user changes until the editor applies them; and which of the rows show,
 * after the search, the chosen filter and the folding of the groups. The editor draws its list and its counter from
 * it.
 *
 * <p>A key the registry changes while the copy is held reaches the rows the user did not change, and a name the
 * registry changes, of a group, a context or a command, shows in the rows: either is reported to the editor, which
 * draws its list again.
 */
final class ShortcutsWorkingCopy {

    private final CommandRegistry registry;

    /** What the editor does when the registry changes a key or a name the copy shows. */
    private final Runnable registryChanged;

    /** The lines of every group, in the registry's order, each with the rows of its commands. */
    private final List<GroupLine> groups = new ArrayList<>();

    /**
     * The listener on the names of the registry's groups, contexts and commands, held here since the names hold it
     * weakly: an editor the application drops does not live on in the registry. A new one with every reading, so that
     * the old one goes.
     */
    private InvalidationListener nameListener;

    /** The listeners on the keys of the registry's commands, one per row, held here as the name listener is. */
    private final List<ChangeListener<Shortcut>> keyListeners = new ArrayList<>();

    /** The search typed into the search field, folded and split into words as the search inputs do. */
    private SearchQuery query = SearchQuery.of(null);

    /** The rows the chosen filter lets show. */
    private Filter filter = Filter.ALL;

    /** The number of keys that two or more commands of one context hold in the copy. */
    private int conflicts;

    /**
     * Creates the working copy of a registry's keys, empty until it {@linkplain #read() reads} the registry.
     *
     * @param registry        the registry
     * @param registryChanged what the editor does when the registry changes a key or a name the copy shows
     */
    ShortcutsWorkingCopy(CommandRegistry registry, Runnable registryChanged) {
        this.registry = registry;
        this.registryChanged = registryChanged;
    }

    /**
     * Reads the registry's groups and commands anew: the rows hold the commands' current keys, every group is open,
     * and the user's changes are gone. The search and the filter stay.
     */
    void read() {
        keyListeners.clear();
        groups.clear();
        Map<CommandGroup, GroupLine> byGroup = new IdentityHashMap<>();
        nameListener = observable -> registryChanged.run();
        for (CommandGroup group : registry.getGroups()) {
            GroupLine line = new GroupLine(group);
            groups.add(line);
            byGroup.put(group, line);
            group.nameProperty().addListener(new WeakInvalidationListener(nameListener));
        }
        for (CommandContext context : registry.getContexts()) {
            context.nameProperty().addListener(new WeakInvalidationListener(nameListener));
        }
        for (Command command : registry.getCommands()) {
            CommandRow row = new CommandRow(command);
            byGroup.get(command.getGroup()).add(row);
            command.nameProperty().addListener(new WeakInvalidationListener(nameListener));
            // A key the registry changes reaches the row while the user has not changed it.
            ChangeListener<Shortcut> keyChanged = (observable, oldKey, newKey) -> {
                if (Objects.equals(row.getShortcut(), oldKey)) {
                    row.setShortcut(newKey);
                }
                registryChanged.run();
            };
            keyListeners.add(keyChanged);
            command.shortcutProperty().addListener(new WeakChangeListener<>(keyChanged));
        }
    }

    /**
     * Hands the copy to the registry: every command with a row takes the row's key, every other command, added to
     * the registry after the copy read it, keeps its own; the registry reports the change once.
     */
    void apply() {
        // The registry takes the whole state as the keys that differ from their defaults: the rows' keys, and the
        // current keys of the commands that have no row.
        Map<Command, Shortcut> keys = new HashMap<>();
        for (CommandRow row : rows()) {
            keys.put(row.getCommand(), row.getShortcut());
        }
        registry.setChangedShortcuts(registry.changedShortcuts(keys));
    }

    /**
     * Finds where the rows' keys clash, and sets it on every row: run it after every change of the copy, before
     * asking which rows show, since the filter <i>Conflicts</i> asks each row.
     */
    void findClashes() {
        conflicts = ShortcutClashes.find(rows(), registry);
    }

    /**
     * Returns the number of conflicts the last {@link #findClashes()} found: of keys that two or more commands of one
     * context hold.
     *
     * @return the number of conflicts
     */
    int conflicts() {
        return conflicts;
    }

    /**
     * Returns the lines of every group, in the registry's order, also those of groups whose rows do not show.
     *
     * @return the group lines, unmodifiable
     */
    List<GroupLine> groups() {
        return Collections.unmodifiableList(groups);
    }

    /**
     * Returns every command's row, in the order of the groups and of the commands within them, also the rows that do
     * not show.
     *
     * @return the rows
     */
    List<CommandRow> rows() {
        List<CommandRow> rows = new ArrayList<>();
        for (GroupLine group : groups) {
            rows.addAll(group.getRows());
        }
        return rows;
    }

    /**
     * Returns the line of a row's group.
     *
     * @param row the row
     * @return the group's line, null for a row the copy does not hold
     */
    GroupLine groupOf(CommandRow row) {
        for (GroupLine group : groups) {
            if (group.getRows().contains(row)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Whether a row's key differs from the registry's: what applying would change.
     *
     * @return true when the copy has a change
     */
    boolean hasChanges() {
        for (CommandRow row : rows()) {
            if (row.isChanged()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a row's key differs from its command's default: what resetting every key would change.
     *
     * @return true when a row is modified
     */
    boolean hasModified() {
        for (CommandRow row : rows()) {
            if (row.isModified()) {
                return true;
            }
        }
        return false;
    }

    // --- What shows ------------------------------------------------------------------------------------

    /**
     * Returns the rows of a group that show: those the chosen filter and the search let pass, in their order. The
     * folding is not asked: a folded group's line counts the rows that would show.
     *
     * @param group the group's line
     * @return the rows that pass
     */
    List<CommandRow> shownRows(GroupLine group) {
        List<CommandRow> shown = new ArrayList<>();
        for (CommandRow row : group.getRows()) {
            if (passesFilter(row) && query.matches(searchedTexts(group, row))) {
                shown.add(row);
            }
        }
        return shown;
    }

    /**
     * Whether the chosen filter lets a row show: every row, the rows whose keys differ from their defaults, or the
     * rows in a conflict.
     */
    private boolean passesFilter(CommandRow row) {
        return switch (filter) {
            case ALL -> true;
            case MODIFIED -> row.isModified();
            case CONFLICTS -> row.isInConflict();
        };
    }

    /**
     * The texts of a row the search looks at, as the row shows them: the command's name, its group's, the name of its
     * context as the row's mark shows it, and the display text of its key in the copy (<i>Ctrl+0</i>, so that
     * <i>ctrl+0</i> finds every command on that key). The window's context is named in no row, so that a search for a
     * word such as <i>window</i> does not find every command of the window.
     */
    private List<String> searchedTexts(GroupLine group, CommandRow row) {
        Command command = row.getCommand();
        List<String> texts = new ArrayList<>();
        texts.add(command.getName());
        texts.add(group.getGroup().getName());
        if (command.getContext() != registry.getWindowContext()) {
            texts.add(command.getContext().getName());
        }
        if (row.getShortcut() != null) {
            texts.add(row.getShortcut().getDisplayText());
        }
        return texts;
    }

    /**
     * Takes a new search: the rows that show follow it, and every group with a match opens again.
     *
     * @param text the text of the search field
     */
    void search(String text) {
        query = SearchQuery.of(text);
        for (GroupLine group : groups) {
            group.setFoldedInSearch(false);
        }
    }

    /**
     * Returns the chosen filter.
     *
     * @return the filter
     */
    Filter getFilter() {
        return filter;
    }

    /**
     * Chooses a filter: the rows that show follow it. It opens no folded group.
     *
     * @param filter the filter
     */
    void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Whether a group shows its rows: while a search is on, every group with a match is open unless the user folded
     * it in this search; without a search, the groups the user left open.
     *
     * @param group the group's line
     * @return true while the group is open
     */
    boolean isOpen(GroupLine group) {
        return query.isEmpty() ? group.isExpanded() : !group.isFoldedInSearch();
    }

    /**
     * Folds or opens a group.
     *
     * @param group the group's line
     */
    void toggle(GroupLine group) {
        // A search folds on its own, so that the folding without it is there again when it is cleared.
        if (query.isEmpty()) {
            group.setExpanded(!group.isExpanded());
        } else {
            group.setFoldedInSearch(!group.isFoldedInSearch());
        }
    }

    /** Which rows the filters of the editor's bar let show. */
    enum Filter {
        /** Every row. */
        ALL,
        /** The rows whose keys differ from their defaults. */
        MODIFIED,
        /** The rows in a conflict. */
        CONFLICTS
    }
}
