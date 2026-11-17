package org.karthu.fx.command;

import java.util.Objects;

/**
 * A command's row in the shortcut editor: the command and its key in the editor's working copy, which the user
 * changes and the editor hands to the registry when it applies.
 */
final class ShortcutEntry implements ShortcutListItem {

    private final Command command;

    /** The key in the working copy; null for none. */
    private Shortcut shortcut;

    /**
     * Creates the row of a command, its key in the working copy being the command's current key.
     *
     * @param command the command
     */
    ShortcutEntry(Command command) {
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
}
