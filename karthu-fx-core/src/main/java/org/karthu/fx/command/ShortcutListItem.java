package org.karthu.fx.command;

/**
 * A line of the shortcut editor's list: a group's line, which folds, or a command's row. The list's items are the
 * lines that show; the editor keeps every line and picks those that show, so that folding, the search and the filters
 * choose from the same lines and a line keeps its state while it is hidden.
 */
sealed interface ShortcutListItem permits ShortcutGroupEntry, ShortcutEntry {
}
