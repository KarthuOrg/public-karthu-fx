package org.karthu.fx.command;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The commands of a window and their keys. An application creates one registry for its window's scene, adds its
 * {@linkplain #addGroup(String, String) groups}, the {@linkplain #addContext(String, String, javafx.scene.Node) parts
 * of the window} whose keys hold only there, and its {@linkplain #addCommand(CommandGroup, String, String, Shortcut,
 * Runnable) commands} in the order they are shown, and {@linkplain Command#bind(javafx.scene.control.MenuItem) binds}
 * its menu items and buttons to the commands. An application with several windows creates a registry for each.
 *
 * <p><b>Keys.</b> A key pressed in the window runs one command, and is consumed. The commands of the
 * {@linkplain CommandContext contexts} that hold the focus are asked from the innermost outward: first those of the
 * innermost part of the window the focus is in, then those of the parts around it, last those of the whole window,
 * and within a context the one added first. So a command of a part wins over a command of the whole window on the same
 * key while the focus is in the part, and only there. A disabled command does not take its key, nor does a
 * {@linkplain Command#isFixed() fixed} one without an action: the key goes on outward.
 *
 * <p>The registry hears a key only after the focused control and its parents had it: a control that consumes a key
 * keeps it, so a focused text field keeps its editing keys, such as Ctrl+C, Ctrl+V or Ctrl+Z, and a command on such a
 * key runs only while the focus is elsewhere. A text input keeps the keys of AltGr too, which arrives as Ctrl+Alt on
 * Windows: while a text input has the focus, no command runs on Ctrl+Alt (with or without Shift) with a key that may
 * type a character, so typing {@code @}, <code>{</code> or {@code |} on a German or Swiss keyboard never runs a
 * command. Function keys and the navigation keys, such as the arrows or Home, type no character: their commands on
 * Ctrl+Alt run there too.
 *
 * <p>A bound menu item shows its command's key as its accelerator, but only the registry runs a command on its key:
 * once, and only where the key holds.
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

    /** The contexts, the window's first. */
    private final List<CommandContext> contexts = new ArrayList<>();

    private final CommandContext windowContext = new CommandContext("window", "Window", null);

    /** The named contexts by their nodes. */
    private final Map<Node, CommandContext> contextsByNode = new HashMap<>();

    private final Set<String> groupIds = new HashSet<>();

    private final Set<String> commandIds = new HashSet<>();

    private final Set<String> contextIds = new HashSet<>();

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
        contexts.add(windowContext);
        contextIds.add(windowContext.getId());
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
     * Adds a context: a part of the window, bound to a node, whose commands run on their keys while the focus is on
     * the node or inside it.
     *
     * @param id   the context's id, unique in the registry; {@code window} is the window's context's
     * @param name the context's name, such as <i>Document</i>
     * @param node the node of the part of the window, which need not be in the scene yet
     * @return the context
     * @throws NullPointerException     when the id, the name or the node is null
     * @throws IllegalArgumentException when the id is blank or another context of the registry has it, or the node is
     *                                  another context's node
     * @since 1.1.0-RC2
     */
    public CommandContext addContext(String id, String name, Node node) {
        checkId(id, contextIds, "context");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(node, "node");
        if (contextsByNode.containsKey(node)) {
            throw new IllegalArgumentException("the node is the context " + contextsByNode.get(node).getId() + "'s");
        }
        CommandContext context = new CommandContext(id, name, node);
        contexts.add(context);
        contextIds.add(id);
        contextsByNode.put(node, context);
        return context;
    }

    /**
     * Returns the context of the whole window, which every registry has: its commands run on their keys wherever the
     * focus is in the window, unless a command of a part of the window that holds the focus takes the key.
     *
     * @return the window's context
     * @since 1.1.0-RC2
     */
    public CommandContext getWindowContext() {
        return windowContext;
    }

    /**
     * Returns the contexts: the window's first, then the others in the order they were added.
     *
     * @return the contexts, an unmodifiable view that follows the registry
     * @since 1.1.0-RC2
     */
    public List<CommandContext> getContexts() {
        return Collections.unmodifiableList(contexts);
    }

    /**
     * Adds a command of the whole window after the commands added before. Its current key is its default key.
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
        return addCommand(group, windowContext, id, name, defaultShortcut, action);
    }

    /**
     * Adds a command of a context after the commands added before. Its current key is its default key.
     *
     * @param group           the group the command is shown in, one of this registry's
     * @param context         the context the command's key holds in, one of this registry's
     * @param id              the command's id, unique in the registry; stable, since it names the command
     * @param name            the command's name, such as <i>Save</i>
     * @param defaultShortcut the command's default key, null for none
     * @param action          what the command does
     * @return the command
     * @throws NullPointerException     when the group, the context, the id, the name or the action is null
     * @throws IllegalArgumentException when the group or the context is not one of this registry's, or the id is
     *                                  blank or another command of the registry has it
     * @since 1.1.0-RC2
     */
    public Command addCommand(CommandGroup group, CommandContext context, String id, String name,
                              Shortcut defaultShortcut, Runnable action) {
        Objects.requireNonNull(action, "action");
        return add(group, context, id, name, defaultShortcut, action, false);
    }

    /**
     * Adds a fixed command after the commands added before: a key that a component of the window owns, such as
     * <i>Copy</i> on Ctrl+C in a document, shown with the other commands so that the user sees it and a clash with it.
     * Its key never changes. Without an action it never takes its key, which goes on as if the command were disabled:
     * the component that owns the key handles it before the registry hears it.
     *
     * @param group    the group the command is shown in, one of this registry's
     * @param context  the context the command's key holds in, one of this registry's
     * @param id       the command's id, unique in the registry; stable, since it names the command
     * @param name     the command's name, such as <i>Copy</i>
     * @param shortcut the command's key
     * @param action   what the command does, null when the component that owns the key does it
     * @return the command
     * @throws NullPointerException     when the group, the context, the id, the name or the shortcut is null
     * @throws IllegalArgumentException when the group or the context is not one of this registry's, or the id is
     *                                  blank or another command of the registry has it
     * @since 1.1.0-RC2
     */
    public Command addFixedCommand(CommandGroup group, CommandContext context, String id, String name,
                                   Shortcut shortcut, Runnable action) {
        Objects.requireNonNull(shortcut, "shortcut");
        return add(group, context, id, name, shortcut, action, true);
    }

    /**
     * Adds a command after the checks of its group, context, id and name.
     */
    private Command add(CommandGroup group, CommandContext context, String id, String name, Shortcut defaultShortcut,
                        Runnable action, boolean fixed) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(context, "context");
        checkId(id, commandIds, "command");
        Objects.requireNonNull(name, "name");
        if (!groups.contains(group)) {
            throw new IllegalArgumentException("the group " + group.getId() + " is not one of this registry's");
        }
        if (!contexts.contains(context)) {
            throw new IllegalArgumentException("the context " + context.getId() + " is not one of this registry's");
        }
        Command command = new Command(group, context, id, name, defaultShortcut, action, fixed);
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
     * looked for in the contexts that hold the focus from the innermost outward, and the key is consumed. A key no
     * command runs on is consumed too when it is a bound menu item's key: the item's accelerator, which the scene runs
     * after its handlers, would run the command where its key does not hold.
     */
    private void handleKeyPressed(KeyEvent event) {
        // The key's target is the node with the focus, or the text field inside it that an editable combo box or
        // spinner hands its keys on to.
        Node target = event.getTarget() instanceof Node node ? node : null;
        if (!isTypedWithAltGr(event, target)) {
            for (CommandContext context : contextsHolding(target)) {
                for (Command command : commands) {
                    if (command.getContext() == context && command.isPressed(event)) {
                        command.execute();
                        event.consume();
                        return;
                    }
                }
            }
        }
        // The scene consumes a key that is an accelerator, whatever its item does: consuming it here takes nothing
        // from the scene's own handling of keys, such as moving the focus.
        for (Command command : commands) {
            if (command.isMenuItemKey(event)) {
                event.consume();
                return;
            }
        }
    }

    /**
     * Whether a key press may be a character typed with AltGr into a text input. On Windows AltGr arrives as Ctrl+Alt,
     * and a text input takes the typed character, not the key press: typing {@code @}, <code>{</code> or {@code |} on
     * a German or Swiss keyboard would run a command on Ctrl+Alt with that key. Function keys and the navigation keys
     * type no character.
     *
     * @param event  a key press
     * @param target the node the key goes to, null for none
     * @return true when the press is Ctrl+Alt on a key that may type a character, in a text input
     */
    private static boolean isTypedWithAltGr(KeyEvent event, Node target) {
        KeyCode code = event.getCode();
        return target instanceof TextInputControl && event.isControlDown() && event.isAltDown()
                && !code.isFunctionKey() && !code.isNavigationKey();
    }

    /**
     * The contexts that hold the focus on a node: the contexts of the node and its parents, the innermost first, and
     * last the window's.
     *
     * @param focused the node with the focus, null for none
     * @return the contexts
     */
    private List<CommandContext> contextsHolding(Node focused) {
        List<CommandContext> holding = new ArrayList<>();
        for (Node node = focused; node != null; node = node.getParent()) {
            CommandContext context = contextsByNode.get(node);
            if (context != null) {
                holding.add(context);
            }
        }
        holding.add(windowContext);
        return holding;
    }
}
