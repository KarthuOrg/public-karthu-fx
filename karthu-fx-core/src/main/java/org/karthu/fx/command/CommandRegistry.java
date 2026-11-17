package org.karthu.fx.command;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The commands of a window and their keys. An application creates one registry for its window's scene, adds its
 * {@linkplain #addGroup(String, String) groups} and its {@linkplain #addCommand(CommandGroup, String, String, Shortcut,
 * Runnable) commands} in the order they are shown, and {@linkplain Command#bind(javafx.scene.control.MenuItem) binds}
 * its menu items and buttons to the commands. An application with several windows creates a registry for each.
 *
 * <p><b>Keys.</b> A key pressed anywhere in the window runs the first enabled command, in the order they were added,
 * whose current key it is, and is consumed. A disabled command does not take its key. The registry hears a key only
 * after the focused control and its parents had it: a control that consumes a key keeps it, so a focused text field
 * keeps its editing keys, such as Ctrl+C, Ctrl+V or Ctrl+Z, and a command on such a key runs only while the focus is
 * elsewhere. A bound menu item shows its command's key as its accelerator; the registry runs the command before the
 * accelerator could, so the command runs once.
 *
 * <p>Like the scene, the registry and its commands are used on the JavaFX Application Thread.
 *
 * @since 1.1.0-RC2
 */
public final class CommandRegistry {

    /**
     * The key of the scene's property that holds its registry: a scene has one at most. An object of its own, so that
     * no other property can take its place.
     */
    private static final Object SCENE_PROPERTY = new Object();

    private final List<CommandGroup> groups = new ArrayList<>();

    private final List<Command> commands = new ArrayList<>();

    private final Set<String> groupIds = new HashSet<>();

    private final Set<String> commandIds = new HashSet<>();

    /**
     * Creates the registry of a window and installs it on the window's scene.
     *
     * @param scene the window's scene
     * @throws NullPointerException     when the scene is null
     * @throws IllegalArgumentException when the scene has a registry already
     * @since 1.1.0-RC2
     */
    public CommandRegistry(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        if (scene.getProperties().containsKey(SCENE_PROPERTY)) {
            throw new IllegalArgumentException("the scene has a command registry already");
        }
        scene.getProperties().put(SCENE_PROPERTY, this);
        // A handler, not a filter: the key reaches the registry after the focused control, which keeps the keys it
        // consumes, and before the scene's accelerators, which a bound menu item's key is one of.
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    /**
     * Adds a group after the groups added before.
     *
     * @param id   the group's id, unique in the registry
     * @param name the group's name, such as <i>File</i>
     * @return the group
     * @throws NullPointerException     when the id or the name is null
     * @throws IllegalArgumentException when the id is blank or another group of the registry has it
     * @since 1.1.0-RC2
     */
    public CommandGroup addGroup(String id, String name) {
        checkId(id, groupIds, "group");
        Objects.requireNonNull(name, "name");
        CommandGroup group = new CommandGroup(id, name);
        groups.add(group);
        groupIds.add(id);
        return group;
    }

    /**
     * Adds a command after the commands added before. Its current key is its default key.
     *
     * @param group           the group the command is shown in, one of this registry's
     * @param id              the command's id, unique in the registry; stable, since it names the command
     * @param name            the command's name, such as <i>Save</i>
     * @param defaultShortcut the command's default key, null for none
     * @param action          what the command does
     * @return the command
     * @throws NullPointerException     when the group, the id, the name or the action is null
     * @throws IllegalArgumentException when the group is not one of this registry's, or the id is blank or another
     *                                  command of the registry has it
     * @since 1.1.0-RC2
     */
    public Command addCommand(CommandGroup group, String id, String name, Shortcut defaultShortcut, Runnable action) {
        Objects.requireNonNull(group, "group");
        checkId(id, commandIds, "command");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        if (!groups.contains(group)) {
            throw new IllegalArgumentException("the group " + group.getId() + " is not one of this registry's");
        }
        Command command = new Command(group, id, name, defaultShortcut, action);
        commands.add(command);
        commandIds.add(id);
        return command;
    }

    /**
     * Returns the groups in the order they were added, the order they are shown in.
     *
     * @return the groups, an unmodifiable view that follows the registry
     * @since 1.1.0-RC2
     */
    public List<CommandGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    /**
     * Returns the commands in the order they were added, the order they are shown in within their groups.
     *
     * @return the commands, an unmodifiable view that follows the registry
     * @since 1.1.0-RC2
     */
    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    /**
     * Refuses an id that is null, blank or taken.
     */
    private static void checkId(String id, Set<String> taken, String kind) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("a " + kind + " needs an id that is not blank");
        }
        if (taken.contains(id)) {
            throw new IllegalArgumentException("the registry has a " + kind + " with the id " + id + " already");
        }
    }

    /**
     * A key pressed in the window and not consumed by the focused control: the first enabled command on that key runs,
     * and the key is consumed.
     */
    private void handleKeyPressed(KeyEvent event) {
        for (Command command : commands) {
            if (command.isPressed(event)) {
                command.execute();
                event.consume();
                return;
            }
        }
    }
}
