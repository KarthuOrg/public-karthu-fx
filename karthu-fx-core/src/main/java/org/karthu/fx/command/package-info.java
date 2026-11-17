/**
 * Commands and their keys: an application registers its commands with a
 * {@link org.karthu.fx.command.CommandRegistry} for its window, each a {@link org.karthu.fx.command.Command} with an
 * id, a name, a {@linkplain org.karthu.fx.command.CommandGroup group}, a default key (a
 * {@link org.karthu.fx.command.Shortcut}) and an action, and binds its menu items and buttons to them. The registry
 * runs a command when its key is pressed in the window, or, for a command of a
 * {@linkplain org.karthu.fx.command.CommandContext context} such as a document area, while the focus is in that part
 * of the window. The user changes the keys in a {@link org.karthu.fx.command.ShortcutsDialog}, or in its content,
 * a {@link org.karthu.fx.command.ShortcutsEditor}, that an application places in a window of its own.
 *
 * @since 1.1.0-RC2
 */
package org.karthu.fx.command;
