package org.karthu.fx.command;

import java.util.List;
import java.util.Objects;

/**
 * A command's row in the shortcut editor: the command and its key in the editor's working copy, which the user
 * changes and the editor hands to the registry when it applies.
 */
final class CommandRow implements ShortcutListItem {

    private final Command command;

    /** The key in the working copy; null for none. */
    private Shortcut shortcut;

    /** The rows of the other commands of the context that hold the same key in the working copy, in their order. */
    private List<CommandRow> conflicts = List.of();

    /** The rows of the commands of enclosing contexts whose key this row's key overrides, the innermost first. */
    private List<CommandRow> overridden = List.of();

    /**
     * Creates the row of a command, its key in the working copy being the command's current key.
     *
     * @param command the command
     */
    CommandRow(Command command) {
        this.command = command;
        this.shortcut = command.getShortcut();
    }

    /**
     * Returns the command.
     *
     * @return the command
     */
    Command getCommand() {
        return command;
    }

    /**
     * Returns the command's key in the working copy.
     *
     * @return the key, null for none
     */
    Shortcut getShortcut() {
        return shortcut;
    }

    /**
     * Sets the command's key in the working copy.
     *
     * @param shortcut the key, null for none
     */
    void setShortcut(Shortcut shortcut) {
        this.shortcut = shortcut;
    }

    /**
     * Puts the command's default key back in the working copy. A fixed command's row keeps its key, which is its
     * default.
     */
    void reset() {
        if (isChangeable()) {
            shortcut = command.getDefaultShortcut();
        }
    }

    /**
     * Whether the user may change the command's key: not for a fixed command.
     *
     * @return true for a command that is not fixed
     */
    boolean isChangeable() {
        return !command.isFixed();
    }

    /**
     * Whether the key in the working copy differs from the command's default: what the user changed, in this editor
     * or earlier.
     *
     * @return true when the key differs from the default
     */
    boolean isModified() {
        return !Objects.equals(shortcut, command.getDefaultShortcut());
    }

    /**
     * Whether the key in the working copy differs from the command's current key: what applying would change.
     *
     * @return true when the key differs from the current key
     */
    boolean isChanged() {
        return !Objects.equals(shortcut, command.getShortcut());
    }

    /**
     * Returns the rows of the other commands of the same context that hold this row's key in the working copy: the
     * conflict this row is in, fixed commands included.
     *
     * @return the other rows, unmodifiable, empty while the row is in no conflict
     */
    List<CommandRow> getConflicts() {
        return conflicts;
    }

    /**
     * Whether another command of the same context holds this row's key in the working copy.
     *
     * @return true while the row is in a conflict
     */
    boolean isInConflict() {
        return !conflicts.isEmpty();
    }

    /**
     * Returns the rows of the commands of the contexts around this row's context that hold its key in the working
     * copy: the commands this row's key overrides while the focus is in its context.
     *
     * @return the overridden rows, unmodifiable, the innermost context's first; empty when the key overrides none
     */
    List<CommandRow> getOverridden() {
        return overridden;
    }

    /**
     * Sets what the row's key clashes with, as the editor finds it after every change of the working copy.
     *
     * @param conflicts  the rows of the other commands of the same context on the same key
     * @param overridden the rows of the commands of enclosing contexts on the same key
     */
    void setClashes(List<CommandRow> conflicts, List<CommandRow> overridden) {
        this.conflicts = List.copyOf(conflicts);
        this.overridden = List.copyOf(overridden);
    }
}
