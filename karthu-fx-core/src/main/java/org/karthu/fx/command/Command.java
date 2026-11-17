package org.karthu.fx.command;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.Objects;

/**
 * A command of the application, such as <i>Save</i> or <i>Zoom In</i>: what it does, the key that runs it and the
 * {@linkplain CommandContext context} the key holds in. Created by
 * {@link CommandRegistry#addCommand(CommandGroup, CommandContext, String, String, Shortcut, Runnable)} or, for a key
 * that a component owns, by
 * {@link CommandRegistry#addFixedCommand(CommandGroup, CommandContext, String, String, Shortcut, Runnable)}.
 *
 * @since 1.1.0-RC2
 */
public final class Command {

    private final String id;

    private final StringProperty name;

    private final CommandGroup group;

    private final CommandContext context;

    private final Shortcut defaultShortcut;

    /** What the command does; null for a fixed command whose component does it. */
    private final Runnable action;

    private final boolean fixed;

    private final ReadOnlyObjectWrapper<Shortcut> shortcut;

    private final ReadOnlyStringWrapper shortcutDisplayText = new ReadOnlyStringWrapper(this, "shortcutDisplayText");

    private final BooleanProperty disabled = new SimpleBooleanProperty(this, "disabled");

    /** Whether a menu item is bound to the command, which shows the command's key as its accelerator. */
    private boolean boundToMenuItem;

    /**
     * The current key as JavaFX's combination, the accelerator of every bound menu item; null while there is no key.
     */
    private final ObjectBinding<KeyCombination> accelerator;

    /**
     * Creates a command whose key is its default.
     *
     * @param group           the command's group
     * @param context         the context the command's key holds in
     * @param id              the command's id, unique in its registry
     * @param name            the command's name
     * @param defaultShortcut the command's default key, null for none
     * @param action          what the command does, null for a fixed command whose component does it
     * @param fixed           whether the command's key never changes
     */
    Command(CommandGroup group, CommandContext context, String id, String name, Shortcut defaultShortcut,
            Runnable action, boolean fixed) {
        this.group = group;
        this.context = context;
        this.id = id;
        this.name = new SimpleStringProperty(this, "name", name);
        this.defaultShortcut = defaultShortcut;
        this.action = action;
        this.fixed = fixed;
        this.shortcut = new ReadOnlyObjectWrapper<>(this, "shortcut", defaultShortcut);
        this.accelerator = Bindings.createObjectBinding(this::currentKeyCombination, shortcut);
        shortcutDisplayText.bind(Bindings.createStringBinding(this::currentDisplayText, shortcut));
    }

    /**
     * Returns the command's id: stable, unique in its registry.
     *
     * @return the id
     * @since 1.1.0-RC2
     */
    public String getId() {
        return id;
    }

    /**
     * The command's name, such as <i>Save</i>. A property so that an application can translate it.
     *
     * @return the name property
     * @since 1.1.0-RC2
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the command's name.
     *
     * @return the name
     * @since 1.1.0-RC2
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the command's name.
     *
     * @param name the name
     * @since 1.1.0-RC2
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * Returns the group the command is shown in.
     *
     * @return the group
     * @since 1.1.0-RC2
     */
    public CommandGroup getGroup() {
        return group;
    }

    /**
     * Returns the context the command's key holds in: the whole window, or a part of it while the focus is there.
     *
     * @return the context
     * @since 1.1.0-RC2
     */
    public CommandContext getContext() {
        return context;
    }

    /**
     * Returns whether the command is fixed: a key that a component of the window owns, shown with the other commands,
     * whose key never changes. See {@link CommandRegistry#addFixedCommand(CommandGroup, CommandContext, String, String,
     * Shortcut, Runnable)}.
     *
     * @return true for a fixed command
     * @since 1.1.0-RC2
     */
    public boolean isFixed() {
        return fixed;
    }

    /**
     * Returns the command's default key, the key the application gave it.
     *
     * @return the default shortcut, null when the command has none by default
     * @since 1.1.0-RC2
     */
    public Shortcut getDefaultShortcut() {
        return defaultShortcut;
    }

    /**
     * The command's current key: its default until it is changed; null while the command has none.
     *
     * @return the shortcut property
     * @since 1.1.0-RC2
     */
    public ReadOnlyObjectProperty<Shortcut> shortcutProperty() {
        return shortcut.getReadOnlyProperty();
    }

    /**
     * Returns the command's current key.
     *
     * @return the shortcut, null while the command has none
     * @since 1.1.0-RC2
     */
    public Shortcut getShortcut() {
        return shortcut.get();
    }

    /**
     * The display text of the command's current key, such as <i>Ctrl+S</i> (see {@link Shortcut#getDisplayText()});
     * empty while the command has no key. It follows every change of the key, so that an application builds the texts
     * that name the key from it, such as a tooltip <i>Save (Ctrl+S)</i>.
     *
     * @return the shortcut display text property
     * @since 1.1.0-RC2
     */
    public ReadOnlyStringProperty shortcutDisplayTextProperty() {
        return shortcutDisplayText.getReadOnlyProperty();
    }

    /**
     * Returns the display text of the command's current key.
     *
     * @return the display text, empty while the command has no key
     * @since 1.1.0-RC2
     */
    public String getShortcutDisplayText() {
        return shortcutDisplayText.get();
    }

    /**
     * Whether the command is disabled right now: a disabled command does not run, neither on its key nor through a
     * bound control, and the bound controls show it disabled. The application sets or binds it as the state of its
     * work changes.
     *
     * @return the disabled property
     * @since 1.1.0-RC2
     */
    public BooleanProperty disabledProperty() {
        return disabled;
    }

    /**
     * Returns whether the command is disabled right now.
     *
     * @return true while the command is disabled
     * @since 1.1.0-RC2
     */
    public boolean isDisabled() {
        return disabled.get();
    }

    /**
     * Sets whether the command is disabled right now.
     *
     * @param disabled true to disable the command
     * @since 1.1.0-RC2
     */
    public void setDisabled(boolean disabled) {
        this.disabled.set(disabled);
    }

    /**
     * Binds a menu item to the command: the item runs the command, is disabled while the command is, and shows the
     * command's current key as its accelerator, following every change of the key. The binding replaces the item's own
     * action, disabled state and accelerator; the item keeps its text.
     *
     * <p>The key runs the command only where it holds, in the command's {@linkplain #getContext() context}: the
     * registry keeps the item's accelerator from running it while the focus is outside. A click on the item chooses
     * the command, as a click on a button does, and runs it wherever the focus is.
     *
     * @param item the menu item
     * @throws NullPointerException  when the item is null
     * @throws IllegalStateException when the command is a fixed one without an action
     * @since 1.1.0-RC2
     */
    public void bind(MenuItem item) {
        Objects.requireNonNull(item, "item");
        checkAction();
        item.setOnAction(event -> execute());
        item.disableProperty().bind(disabled);
        item.acceleratorProperty().bind(accelerator);
        boundToMenuItem = true;
    }

    /**
     * Binds a button to the command: the button runs the command and is disabled while the command is. The binding
     * replaces the button's own action and disabled state; the button keeps its text.
     *
     * @param button the button, or another control that fires an action, such as a toggle button or a hyperlink
     * @throws NullPointerException  when the button is null
     * @throws IllegalStateException when the command is a fixed one without an action
     * @since 1.1.0-RC2
     */
    public void bind(ButtonBase button) {
        Objects.requireNonNull(button, "button");
        checkAction();
        button.setOnAction(event -> execute());
        button.disableProperty().bind(disabled);
    }

    /**
     * Sets the command's current key.
     *
     * @param shortcut the key, null for none
     * @throws IllegalStateException when the command is fixed and the key another than its own
     */
    void setShortcut(Shortcut shortcut) {
        if (fixed && !Objects.equals(shortcut, this.shortcut.get())) {
            throw new IllegalStateException("the key of the fixed command " + id + " never changes");
        }
        this.shortcut.set(shortcut);
    }

    /**
     * Whether a key press runs the command: the command is enabled, has an action and a key, and the press is it.
     *
     * @param event a key press
     * @return true when the press runs the command
     */
    boolean isPressed(KeyEvent event) {
        Shortcut current = shortcut.get();
        return !isDisabled() && action != null && current != null && current.matches(event);
    }

    /**
     * Whether a key press is the key a bound menu item shows as its accelerator, through which the item would run the
     * command.
     *
     * @param event a key press
     * @return true when a menu item is bound to the command and the press is its key
     */
    boolean isMenuItemKey(KeyEvent event) {
        Shortcut current = shortcut.get();
        return boundToMenuItem && current != null && current.matches(event);
    }

    /** The current key as JavaFX's combination, null while the command has none. */
    private KeyCombination currentKeyCombination() {
        Shortcut current = shortcut.get();
        return current == null ? null : current.toKeyCombination();
    }

    /** The display text of the current key, empty while the command has none. */
    private String currentDisplayText() {
        Shortcut current = shortcut.get();
        return current == null ? "" : current.getDisplayText();
    }

    /**
     * Runs the command's action, unless the command is disabled: a menu item fires from code even while it is
     * disabled.
     */
    void execute() {
        if (!isDisabled() && action != null) {
            action.run();
        }
    }

    /** Refuses to bind a control to a command that has no action to run. */
    private void checkAction() {
        if (action == null) {
            throw new IllegalStateException("the fixed command " + id + " has no action a control could run");
        }
    }
}
