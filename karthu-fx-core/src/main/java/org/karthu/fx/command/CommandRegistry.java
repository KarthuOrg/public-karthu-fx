package org.karthu.fx.command;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.MapChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
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
 * {@linkplain Command#isFixed() fixed} one without an action: the key goes on outward. Two commands of one context
 * with the same default key are a fault of the application, which the registry logs as a warning when the second is
 * added: only one of them can run on the key.
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
 * <p><b>Keeping the user's keys.</b> The registry stores nothing. It gives out the keys that differ from their
 * defaults, {@link #getChangedShortcuts()}, as a map from command id to the key's
 * {@linkplain Shortcut#toText() text form}, an empty text for a removed key, such as
 * {@code file.save=Ctrl+Shift+S}. The application keeps that map where it keeps its other settings, writes it again
 * whenever {@link #changedShortcutsProperty()} changes, and hands it back on the next start with
 * {@link #setChangedShortcuts(Map)}, to each registry when it has several windows. Since only the changes are kept, a
 * new default of a later version of the application reaches every user who left that key alone. Taking a map back is
 * tolerant: an entry the registry cannot use costs only itself. {@link #resetShortcuts()} puts every key back to its
 * default.
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

    private final Scene scene;

    private final List<CommandGroup> groups = new ArrayList<>();

    private final List<Command> commands = new ArrayList<>();

    /** The contexts, the window's first. */
    private final List<CommandContext> contexts = new ArrayList<>();

    private final CommandContext windowContext = new CommandContext("window", "Window", null);

    /** The named contexts by their nodes. */
    private final Map<Node, CommandContext> contextsByNode = new HashMap<>();

    private final Set<String> groupIds = new HashSet<>();

    /** The commands by their ids. */
    private final Map<String, Command> commandsById = new HashMap<>();

    private final Set<String> contextIds = new HashSet<>();

    /**
     * Where skipped entries of a stored list and clashing defaults are reported: the SLF4J logger named after the
     * registry's class, as karthu-fx-core logs everywhere, a recorder in tests.
     */
    private Logger logger = LoggerFactory.getLogger(CommandRegistry.class);

    private final ReadOnlyObjectWrapper<Map<String, String>> changedShortcuts =
            new ReadOnlyObjectWrapper<>(this, "changedShortcuts", Map.of());

    /**
     * Whether the registry is changing several keys at once: the application hears the changed keys once, after the
     * last, never a state in between.
     */
    private boolean changingKeys;

    /**
     * The actions the scene runs on the accelerators of menu items bound to the commands, by identity. A menu bar, a
     * menu button or the control of a context menu puts its items' accelerators into the scene's accelerators, each
     * with an action that fires its item, while it is in the scene, and takes them out when it leaves; an action put
     * there with a command's very accelerator as its key is a bound item's. The scene runs whichever action its map
     * holds for a key, the application's own included, so it is the action that counts, not the key.
     */
    private final Set<Runnable> menuItemActions = Collections.newSetFromMap(new IdentityHashMap<>());

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
        this.scene = scene;
        contexts.add(windowContext);
        contextIds.add(windowContext.getId());
        // A handler, not a filter: the key reaches the registry after the focused control, which keeps the keys it
        // consumes, and before the scene's accelerators, which a bound menu item's key is one of.
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        scene.getAccelerators().addListener(this::acceleratorsChanged);
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
        checkId(id, commandsById.keySet(), "command");
        Objects.requireNonNull(name, "name");
        if (!groups.contains(group)) {
            throw new IllegalArgumentException("the group " + group.getId() + " is not one of this registry's");
        }
        if (!contexts.contains(context)) {
            throw new IllegalArgumentException("the context " + context.getId() + " is not one of this registry's");
        }
        Command command = new Command(group, context, id, name, defaultShortcut, action, fixed);
        warnOfClashingDefault(command);
        commands.add(command);
        commandsById.put(id, command);
        command.shortcutProperty().addListener(observable -> {
            if (!changingKeys) {
                updateChangedShortcuts();
            }
        });
        return command;
    }

    /**
     * Logs a warning when a new command's default key is the default of a command added before in the same context:
     * a fault of the application, since only one of them can run on the key there. Once, for the first such command.
     */
    private void warnOfClashingDefault(Command added) {
        Shortcut defaultShortcut = added.getDefaultShortcut();
        if (defaultShortcut == null) {
            return;
        }
        for (Command command : commands) {
            if (command.getContext() == added.getContext() && defaultShortcut.equals(command.getDefaultShortcut())) {
                logger.warn("The default shortcut {} of the command '{}' is the default of the command '{}' in the"
                        + " same context '{}' too", defaultShortcut.toText(), added.getId(), command.getId(),
                        added.getContext().getId());
                return;
            }
        }
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
     * The keys that differ from their defaults, the ones an application keeps: see {@link #getChangedShortcuts()}. It
     * changes whenever they do: when a command's key changes, and once when a map is taken back or the keys are reset,
     * never with the keys in between. An application listens to it to write the map to its settings.
     *
     * @return the changed shortcuts property, its value an unmodifiable map
     * @since 1.1.0-RC2
     */
    public ReadOnlyObjectProperty<Map<String, String>> changedShortcutsProperty() {
        return changedShortcuts.getReadOnlyProperty();
    }

    /**
     * Returns the keys that differ from their defaults: a map from command id to the key's
     * {@linkplain Shortcut#toText() text form} ({@code file.save=Ctrl+Shift+S}), or to an empty text where the command
     * has no key though it has a default ({@code file.open=}), in the order of the commands. A command whose key is its
     * default, and a fixed command, is not in it.
     *
     * @return the changed keys, an unmodifiable map; empty while every key is its default
     * @since 1.1.0-RC2
     */
    public Map<String, String> getChangedShortcuts() {
        return changedShortcuts.get();
    }

    /**
     * Sets the changed keys anew from the commands' keys, and so tells the application, when they differ from the
     * ones it was told last.
     */
    private void updateChangedShortcuts() {
        Map<String, String> changed = changedShortcuts(Map.of());
        if (!changed.equals(changedShortcuts.get())) {
            changedShortcuts.set(Collections.unmodifiableMap(changed));
        }
    }

    /**
     * The keys that differ from their defaults, as {@link #getChangedShortcuts()} gives them out, with some commands
     * on other keys than their current ones: what {@link #setChangedShortcuts(Map)} takes to put those commands on
     * those keys and leave every other command's key as it is.
     *
     * @param keys the other keys by command, null for a removed key; a command not in it keeps its current key
     * @return the changed keys by command id, in the order of the commands
     */
    Map<String, String> changedShortcuts(Map<Command, Shortcut> keys) {
        Map<String, String> changed = new LinkedHashMap<>();
        for (Command command : commands) {
            Shortcut key = keys.containsKey(command) ? keys.get(command) : command.getShortcut();
            if (!Objects.equals(key, command.getDefaultShortcut())) {
                changed.put(command.getId(), key == null ? "" : key.toText());
            }
        }
        return changed;
    }

    /**
     * Takes back the keys that differ from their defaults, as {@link #getChangedShortcuts()} gave them out: every
     * command the map names gets the key its text names, none for an empty text, and every other command its default.
     * Then {@link #changedShortcutsProperty()} changes once, if the keys did.
     *
     * <p>Taking a map back is tolerant, so that an application hands it over as it kept it, after an update of the
     * application too. An entry is skipped, and logged as a warning, when it names no command of the registry, when
     * its text is not a shortcut's {@linkplain Shortcut#parse(String) text form} or a shortcut a user may not
     * {@linkplain Shortcut#isRecordable() record}, or when it names a {@linkplain Command#isFixed() fixed} command; an
     * entry without an id or a text too. The command of a skipped entry keeps its default, and the other entries
     * apply.
     *
     * <p>A key the user chose stays when it is the default of another command in the same context, such as a new
     * default of a later version: the other command is left without a key, which counts as a change of its key. A
     * fixed command keeps its key, and keys of other contexts are not touched.
     *
     * @param changed the changed keys by command id, as {@link #getChangedShortcuts()} gives them out
     * @throws NullPointerException when the map is null
     * @since 1.1.0-RC2
     */
    public void setChangedShortcuts(Map<String, String> changed) {
        Objects.requireNonNull(changed, "changed");
        // The keys the entries give, a removed key as null; a command without a usable entry gets its default.
        Map<Command, Shortcut> chosen = new HashMap<>();
        for (Map.Entry<String, String> entry : changed.entrySet()) {
            String id = entry.getKey();
            String text = entry.getValue();
            Command command = id == null ? null : commandsById.get(id);
            if (command == null) {
                logger.warn("A stored shortcut of an unknown command '{}' is skipped: '{}'", id, text);
            } else if (text == null) {
                logger.warn("The stored shortcut of the command '{}' has no text and is skipped", id);
            } else if (command.isFixed()) {
                logger.warn("The command '{}' is fixed, so its stored shortcut '{}' is skipped", id, text);
            } else if (text.isEmpty()) {
                chosen.put(command, null);
            } else {
                Shortcut shortcut = read(id, text);
                if (shortcut != null) {
                    chosen.put(command, shortcut);
                }
            }
        }
        changingKeys = true;
        try {
            for (Command command : commands) {
                if (chosen.containsKey(command)) {
                    command.setShortcut(chosen.get(command));
                } else if (isTakenByChosenKey(command, chosen)) {
                    command.setShortcut(null);
                } else {
                    command.setShortcut(command.getDefaultShortcut());
                }
            }
        } finally {
            changingKeys = false;
        }
        updateChangedShortcuts();
    }

    /**
     * Puts every command's key back to its default: {@link #getChangedShortcuts()} is empty afterwards, and
     * {@link #changedShortcutsProperty()} changes once, if any key did.
     *
     * @since 1.1.0-RC2
     */
    public void resetShortcuts() {
        // No key changed: every command gets its default, the application hears the change once.
        setChangedShortcuts(Map.of());
    }

    /**
     * Whether a command without a stored entry loses its default to a stored key: another command of its context
     * has its default as a key the user chose. The user's key stays, the command is left without one. A fixed
     * command keeps its key whatever the user chose.
     */
    private static boolean isTakenByChosenKey(Command command, Map<Command, Shortcut> chosen) {
        Shortcut defaultShortcut = command.getDefaultShortcut();
        if (command.isFixed() || defaultShortcut == null) {
            return false;
        }
        for (Map.Entry<Command, Shortcut> entry : chosen.entrySet()) {
            Command other = entry.getKey();
            if (other.getContext() == command.getContext() && defaultShortcut.equals(entry.getValue())
                    && !defaultShortcut.equals(other.getDefaultShortcut())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads the text of a stored entry: the shortcut, or null, logged, when the text is not one or the user may not
     * record it.
     */
    private Shortcut read(String id, String text) {
        Shortcut shortcut;
        try {
            shortcut = Shortcut.parse(text);
        } catch (IllegalArgumentException unreadable) {
            logger.warn("The stored shortcut '{}' of the command '{}' cannot be read and is skipped: {}", text, id,
                    unreadable.getMessage());
            return null;
        }
        if (!shortcut.isRecordable()) {
            logger.warn("The stored shortcut '{}' of the command '{}' is not one a user may record and is skipped",
                    text, id);
            return null;
        }
        return shortcut;
    }

    /**
     * Replaces where skipped entries and clashing defaults are reported.
     *
     * @param logger the logger from now on
     */
    void setLogger(Logger logger) {
        this.logger = logger;
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
     * command runs on is consumed too while it is the accelerator of a bound menu item in the scene: the scene runs its
     * accelerators after its handlers, and the item's would run the command where its key does not hold. Once the
     * item's menu left the scene, the key goes on to the scene's other accelerators.
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
        // The scene consumes a key that is an accelerator with an action, whatever its item does, a disabled one
        // too: consuming it here takes nothing from the scene's own handling of keys, such as moving the focus.
        for (Map.Entry<KeyCombination, Runnable> accelerator : scene.getAccelerators().entrySet()) {
            if (menuItemActions.contains(accelerator.getValue()) && accelerator.getKey().match(event)) {
                event.consume();
                return;
            }
        }
    }

    /**
     * Keeps {@link #menuItemActions} in line with the scene's accelerators: an action leaves with its entry, and an
     * action put with a command's accelerator as its key is a bound menu item's.
     */
    private void acceleratorsChanged(MapChangeListener.Change<? extends KeyCombination, ? extends Runnable> change) {
        if (change.wasRemoved()) {
            menuItemActions.remove(change.getValueRemoved());
        }
        if (change.wasAdded() && change.getValueAdded() != null && isAccelerator(change.getKey())) {
            menuItemActions.add(change.getValueAdded());
        }
    }

    /**
     * Whether a combination is the accelerator of a command's bound menu items, the object itself.
     */
    private boolean isAccelerator(KeyCombination combination) {
        for (Command command : commands) {
            if (command.isAccelerator(combination)) {
                return true;
            }
        }
        return false;
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
