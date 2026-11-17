package org.karthu.fx.command;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.karthu.fx.form.SearchInput;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The keys of a window's commands for the user to change: every command of a {@link CommandRegistry} with its key,
 * grouped as the registry groups them. The content of the {@link ShortcutsDialog}, and a node of its own that an
 * application can place on a settings page of its own window, with its own buttons.
 *
 * <p><b>Working copy.</b> The editor works on a copy of the registry's keys: what the user changes shows at once in
 * the editor, and reaches the registry only with {@link #apply()}. {@link #discard()} drops the changes and reads the
 * registry again, its groups and commands too. {@link #hasChangesProperty()} tells whether the copy differs from the
 * registry, so that a window can enable its own buttons. A key the registry changes while the editor shows, such as
 * a list of keys taken back, reaches the rows the user did not change.
 *
 * <p><b>Layout</b>, from top to bottom: a bar with a search field, the filters <i>All</i>, <i>Modified</i> and
 * <i>Conflicts</i> and <i>Reset all</i>; a header row naming the columns <i>Command</i> and <i>Shortcut</i>, in
 * capitals; the list; and a footer with the counter, such as <i>30 commands · 1 modified · 0 conflicts · 1 not
 * assigned</i>. The list shows each group's line, with a fold arrow, the group's name and the number of its commands,
 * then the rows of its commands in their order; a group without commands has no line. A click on a group's line folds
 * or opens it; groups start open, and their folding is not kept. A command's row shows its name, a dot when its key differs from its default, a small mark
 * with its context's name when its key holds only in a part of the window, and its key as key caps or <i>Not
 * assigned</i>. The row under the pointer and the selected row show a pencil, which records a new key, and an
 * &times;, which removes the key; a modified row shows a reset button, which puts the default back. A
 * {@linkplain Command#isFixed() fixed} command's row shows its key and has none of the buttons.
 *
 * <p><b>Recording.</b> Enter, a double-click on a row or its pencil starts recording: a field takes the place of the
 * key caps, shows the modifiers held down, <i>Press keys…</i> and <i>Esc to cancel</i>. The first key the user may
 * record becomes the command's key at once. The user may record F1 to F12 alone or with any modifiers, and every
 * other key only with Ctrl or Alt ({@link Shortcut#isRecordable()}); a refused key shows why in the field, and
 * recording goes on. A modifier key alone shows as held and records nothing. Letters, digits,
 * function keys and named keys are recorded by the key, punctuation by the character it types: Ctrl and the plus key
 * record <i>Ctrl++</i>, whichever key types the plus on the user's keyboard. Esc, a click elsewhere or the field
 * losing the focus end the recording and keep the old key.
 *
 * <p><b>Keyboard</b>, in the list: the arrow keys move through the lines, Enter records a new key for the selected
 * command, Delete and Backspace remove its key. On a group's line Enter and Space fold or open the group, Left folds
 * and Right opens it.
 *
 * <p><b>Texts.</b> Every text is a property with an English default, so that an application translates the editor.
 * A count's text is a format whose {@code {0}} stands for the count, with a form for a count of one: <i>{0}
 * command</i>, <i>{0} commands</i>.
 *
 * <p><b>Style classes</b>, a public contract (also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>): the editor
 * {@code shortcuts-editor}; the bar {@code shortcuts-bar} with {@code shortcuts-search}, {@code shortcuts-filter}
 * (holding {@code shortcuts-filter-all}, {@code shortcuts-filter-modified}, {@code shortcuts-filter-conflicts}) and
 * {@code shortcuts-reset-all}; the header {@code shortcuts-header} with {@code shortcuts-header-command} and
 * {@code shortcuts-header-shortcut}; the list {@code shortcuts-list}; a group's line {@code shortcuts-group-row}
 * (a list cell) with its content {@code shortcuts-group-content} holding {@code shortcuts-group-arrow},
 * {@code shortcuts-group-name} and {@code shortcuts-group-count}; a command's row {@code shortcuts-row} (a list cell)
 * with its content {@code shortcuts-row-content} holding {@code shortcuts-command-column} (with
 * {@code shortcuts-command-name}, {@code shortcuts-modified-dot} and {@code shortcuts-context-mark}),
 * {@code shortcuts-shortcut-column} (with {@code shortcuts-key-caps} holding one {@code shortcuts-key-cap} per
 * modifier and key, or {@code shortcuts-not-assigned}), and the buttons {@code shortcuts-edit},
 * {@code shortcuts-remove} and {@code shortcuts-reset}; the recording field {@code shortcuts-recorder} with
 * {@code shortcuts-recorder-message} and {@code shortcuts-recorder-hint}; the footer {@code shortcuts-footer} with the
 * counter {@code shortcuts-count} (holding {@code shortcuts-count-commands}, {@code shortcuts-count-modified},
 * {@code shortcuts-count-conflicts}, {@code shortcuts-count-not-assigned} and the separators
 * {@code shortcuts-count-separator}) and {@code shortcuts-footer-buttons}, empty but in the dialog. Pseudo-classes: on a
 * command's row {@code :modified}, {@code :not-assigned}, {@code :fixed} and {@code :recording}, beside the list
 * cell's own {@code :selected} and {@code :hover}; on a group's line {@code :collapsed}; on the recording field
 * {@code :refused} while it shows why a key was refused.
 *
 * <p>Like the registry, the editor is used on the JavaFX Application Thread.
 *
 * @since 1.1.0-RC2
 */
public final class ShortcutsEditor extends VBox {

    /** The share of the list's width that the command column takes; the shortcut column takes the rest. */
    private static final double COMMAND_COLUMN_SHARE = 0.55;

    private final CommandRegistry registry;

    private final StringProperty searchPromptText =
            new SimpleStringProperty(this, "searchPromptText", "Filter by command or key");

    private final StringProperty allFilterText = new SimpleStringProperty(this, "allFilterText", "All");

    private final StringProperty modifiedFilterText =
            new SimpleStringProperty(this, "modifiedFilterText", "Modified");

    private final StringProperty conflictsFilterText =
            new SimpleStringProperty(this, "conflictsFilterText", "Conflicts");

    private final StringProperty resetAllText = new SimpleStringProperty(this, "resetAllText", "Reset all");

    private final StringProperty commandColumnText =
            new SimpleStringProperty(this, "commandColumnText", "Command");

    private final StringProperty shortcutColumnText =
            new SimpleStringProperty(this, "shortcutColumnText", "Shortcut");

    private final StringProperty notAssignedText =
            new SimpleStringProperty(this, "notAssignedText", "Not assigned");

    private final StringProperty pressKeysText = new SimpleStringProperty(this, "pressKeysText", "Press keys\u2026");

    private final StringProperty escToCancelText =
            new SimpleStringProperty(this, "escToCancelText", "Esc to cancel");

    private final StringProperty needsCtrlOrAltFormat =
            new SimpleStringProperty(this, "needsCtrlOrAltFormat", "{0} needs Ctrl or Alt");

    private final StringProperty unusableKeyText =
            new SimpleStringProperty(this, "unusableKeyText", "This key cannot be a shortcut");

    private final StringProperty editTooltipText =
            new SimpleStringProperty(this, "editTooltipText", "Change shortcut");

    private final StringProperty removeTooltipText =
            new SimpleStringProperty(this, "removeTooltipText", "Remove shortcut");

    private final StringProperty resetTooltipText =
            new SimpleStringProperty(this, "resetTooltipText", "Reset to default");

    private final StringProperty commandCountFormat =
            new SimpleStringProperty(this, "commandCountFormat", "{0} commands");

    private final StringProperty commandCountOneFormat =
            new SimpleStringProperty(this, "commandCountOneFormat", "{0} command");

    private final StringProperty modifiedCountFormat =
            new SimpleStringProperty(this, "modifiedCountFormat", "{0} modified");

    private final StringProperty modifiedCountOneFormat =
            new SimpleStringProperty(this, "modifiedCountOneFormat", "{0} modified");

    private final StringProperty conflictCountFormat =
            new SimpleStringProperty(this, "conflictCountFormat", "{0} conflicts");

    private final StringProperty conflictCountOneFormat =
            new SimpleStringProperty(this, "conflictCountOneFormat", "{0} conflict");

    private final StringProperty notAssignedCountFormat =
            new SimpleStringProperty(this, "notAssignedCountFormat", "{0} not assigned");

    private final StringProperty notAssignedCountOneFormat =
            new SimpleStringProperty(this, "notAssignedCountOneFormat", "{0} not assigned");

    private final ReadOnlyBooleanWrapper hasChanges = new ReadOnlyBooleanWrapper(this, "hasChanges");

    private final ListView<ShortcutListItem> list = new ListView<>();

    private final DoubleBinding commandColumnWidth =
            Bindings.createDoubleBinding(() -> Math.floor(list.getWidth() * COMMAND_COLUMN_SHARE),
                    list.widthProperty());

    private final Label commandCount = countLabel("shortcuts-count-commands");

    private final Label modifiedCount = countLabel("shortcuts-count-modified");

    private final Label conflictCount = countLabel("shortcuts-count-conflicts");

    private final Label notAssignedCount = countLabel("shortcuts-count-not-assigned");

    private final HBox footerButtons = new HBox();

    /** Counts up whenever the working copy changes: the list's cells draw their lines again. */
    private final IntegerProperty revision = new SimpleIntegerProperty(this, "revision");

    private final ShortcutRecorder recorder;

    /** The lines of every group, in the registry's order, each with the rows of its commands. */
    private final List<ShortcutGroupEntry> groups = new ArrayList<>();

    /**
     * The listeners on the registry's commands, held here since the commands hold them weakly: an editor the
     * application drops does not live on in the registry.
     */
    private final List<Object> registryListeners = new ArrayList<>();

    /** The row the user records a key for; null while no recording runs. */
    private ShortcutEntry recording;

    /** Whether the recording field had the focus since the recording started. */
    private boolean recorderFocused;

    /** Ends a recording when the focus leaves the recording field. */
    private final ChangeListener<Node> focusOwnerListener = (observable, oldOwner, newOwner) -> focusMoved(newOwner);

    /**
     * Creates the editor of a registry's keys, its working copy holding the registry's current keys.
     *
     * @param registry the registry whose keys the editor changes
     * @throws NullPointerException when the registry is null
     * @since 1.1.0-RC2
     */
    public ShortcutsEditor(CommandRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        getStyleClass().add("shortcuts-editor");
        recorder = new ShortcutRecorder(this);

        list.getStyleClass().add("shortcuts-list");
        list.setCellFactory(view -> new ShortcutCell(this));
        list.addEventFilter(KeyEvent.KEY_PRESSED, this::listKeyPressed);
        VBox.setVgrow(list, Priority.ALWAYS);

        getChildren().addAll(bar(), header(), list, footer());

        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (recording != null && !isInRecorder(event.getTarget())) {
                cancelRecording(true);
            }
        });
        sceneProperty().addListener((observable, oldScene, newScene) -> sceneChanged(oldScene, newScene));
        recorder.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (!isFocused && recording != null && recorderFocused) {
                cancelRecording(false);
            }
        });
        InvalidationListener countFormatChanged = observable -> updateCounter();
        for (StringProperty format : List.of(commandCountFormat, commandCountOneFormat, modifiedCountFormat,
                modifiedCountOneFormat, conflictCountFormat, conflictCountOneFormat, notAssignedCountFormat,
                notAssignedCountOneFormat)) {
            format.addListener(countFormatChanged);
        }
        rebuild();
        changed();
    }

    // --- Layout ----------------------------------------------------------------------------------------

    /**
     * The bar above the list: the search field, the filters and <i>Reset all</i>. They show, but take no input yet:
     * the search, the filters and resetting every key follow.
     */
    private HBox bar() {
        SearchInput search = new SearchInput();
        search.getStyleClass().add("shortcuts-search");
        search.promptTextProperty().bind(searchPromptText);
        HBox.setHgrow(search, Priority.ALWAYS);

        ToggleGroup filters = new ToggleGroup();
        ToggleButton all = filterButton("shortcuts-filter-all", allFilterText, filters);
        ToggleButton modified = filterButton("shortcuts-filter-modified", modifiedFilterText, filters);
        ToggleButton conflicts = filterButton("shortcuts-filter-conflicts", conflictsFilterText, filters);
        all.setSelected(true);
        HBox filter = new HBox(all, modified, conflicts);
        filter.getStyleClass().add("shortcuts-filter");

        Button resetAll = new Button();
        resetAll.getStyleClass().add("shortcuts-reset-all");
        resetAll.textProperty().bind(resetAllText);

        search.setDisable(true);
        filter.setDisable(true);
        resetAll.setDisable(true);

        HBox bar = new HBox(search, filter, resetAll);
        bar.getStyleClass().add("shortcuts-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private static ToggleButton filterButton(String styleClass, StringProperty text, ToggleGroup group) {
        ToggleButton button = new ToggleButton();
        button.getStyleClass().add(styleClass);
        button.textProperty().bind(text);
        button.setToggleGroup(group);
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    /**
     * The header row above the list, its column titles in capitals; the command column as wide as the rows', so that
     * the titles stand over the columns.
     */
    private HBox header() {
        Label command = new Label();
        command.getStyleClass().add("shortcuts-header-command");
        command.textProperty().bind(capitals(commandColumnText));
        command.minWidthProperty().bind(commandColumnWidth);
        command.prefWidthProperty().bind(commandColumnWidth);
        command.maxWidthProperty().bind(commandColumnWidth);
        Label shortcut = new Label();
        shortcut.getStyleClass().add("shortcuts-header-shortcut");
        shortcut.textProperty().bind(capitals(shortcutColumnText));
        HBox header = new HBox(command, shortcut);
        header.getStyleClass().add("shortcuts-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private static ObservableValue<String> capitals(StringProperty text) {
        return Bindings.createStringBinding(() -> text.get() == null ? "" : text.get().toUpperCase(Locale.ROOT), text);
    }

    /**
     * The footer below the list: the counter, then the buttons a dialog puts there.
     */
    private HBox footer() {
        HBox counter = new HBox(commandCount, separator(), modifiedCount, separator(), conflictCount, separator(),
                notAssignedCount);
        counter.getStyleClass().add("shortcuts-count");
        counter.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footerButtons.getStyleClass().add("shortcuts-footer-buttons");
        footerButtons.setAlignment(Pos.CENTER_RIGHT);
        HBox footer = new HBox(counter, spacer, footerButtons);
        footer.getStyleClass().add("shortcuts-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
    }

    private static Label countLabel(String styleClass) {
        Label label = new Label();
        label.getStyleClass().add(styleClass);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private static Label separator() {
        Label separator = new Label(" \u00B7 ");
        separator.getStyleClass().add("shortcuts-count-separator");
        separator.setMinWidth(Region.USE_PREF_SIZE);
        return separator;
    }

    // --- Working copy ----------------------------------------------------------------------------------

    /**
     * Hands the working copy to the registry: every command whose key the user changed takes its new key, all at
     * once, so that the registry reports the {@linkplain CommandRegistry#changedShortcutsProperty() changed keys} to
     * the application once. A recording that runs ends without a key first.
     *
     * @since 1.1.0-RC2
     */
    public void apply() {
        cancelRecording(false);
        // The registry takes the whole state as the keys that differ from their defaults: the working copy holds
        // every key, the ones the user left as the registry had them.
        Map<String, String> changed = new LinkedHashMap<>();
        for (ShortcutGroupEntry group : groups) {
            for (ShortcutEntry entry : group.getEntries()) {
                if (entry.isChangeable() && entry.isModified()) {
                    Shortcut shortcut = entry.getShortcut();
                    changed.put(entry.getCommand().getId(), shortcut == null ? "" : shortcut.toText());
                }
            }
        }
        registry.setChangedShortcuts(changed);
        changed();
    }

    /**
     * Drops the working copy: the editor shows the registry's groups, commands and keys again, every group open. A
     * recording that runs ends without a key.
     *
     * @since 1.1.0-RC2
     */
    public void discard() {
        cancelRecording(false);
        rebuild();
        changed();
    }

    /**
     * Whether the working copy differs from the registry's keys: what {@link #apply()} would change. False again after
     * {@link #apply()} and {@link #discard()}.
     *
     * @return the has changes property
     * @since 1.1.0-RC2
     */
    public ReadOnlyBooleanProperty hasChangesProperty() {
        return hasChanges.getReadOnlyProperty();
    }

    /**
     * Returns whether the working copy differs from the registry's keys.
     *
     * @return true while the working copy has changes
     * @since 1.1.0-RC2
     */
    public boolean hasChanges() {
        return hasChanges.get();
    }

    /**
     * Reads the registry's groups and commands into a new working copy.
     */
    private void rebuild() {
        registryListeners.clear();
        groups.clear();
        Map<CommandGroup, ShortcutGroupEntry> byGroup = new IdentityHashMap<>();
        InvalidationListener nameChanged = observable -> changed();
        registryListeners.add(nameChanged);
        for (CommandGroup group : registry.getGroups()) {
            ShortcutGroupEntry entry = new ShortcutGroupEntry(group);
            groups.add(entry);
            byGroup.put(group, entry);
            group.nameProperty().addListener(new WeakInvalidationListener(nameChanged));
        }
        for (CommandContext context : registry.getContexts()) {
            context.nameProperty().addListener(new WeakInvalidationListener(nameChanged));
        }
        for (Command command : registry.getCommands()) {
            ShortcutEntry entry = new ShortcutEntry(command);
            byGroup.get(command.getGroup()).add(entry);
            command.nameProperty().addListener(new WeakInvalidationListener(nameChanged));
            // A key the registry changes reaches the row while the user has not changed it.
            ChangeListener<Shortcut> keyChanged = (observable, oldKey, newKey) -> {
                if (Objects.equals(entry.getShortcut(), oldKey)) {
                    entry.setShortcut(newKey);
                }
                changed();
            };
            registryListeners.add(keyChanged);
            command.shortcutProperty().addListener(new WeakChangeListener<>(keyChanged));
        }
    }

    /**
     * Brings everything that depends on the working copy in line with it: the lines the list shows, the counter,
     * whether there are changes, and the cells, which draw their lines again.
     */
    private void changed() {
        refreshItems();
        updateCounter();
        boolean changes = false;
        for (ShortcutGroupEntry group : groups) {
            for (ShortcutEntry entry : group.getEntries()) {
                changes = changes || entry.isChanged();
            }
        }
        hasChanges.set(changes);
        revision.set(revision.get() + 1);
    }

    /**
     * Shows the lines that show: each group with a row that shows, then, while it is open, those rows. The selection
     * stays on its line; when its line is folded away, it moves to the line of the line's group.
     */
    private void refreshItems() {
        List<ShortcutListItem> shown = new ArrayList<>();
        for (ShortcutGroupEntry group : groups) {
            List<ShortcutEntry> entries = shownEntries(group);
            if (!entries.isEmpty()) {
                shown.add(group);
                if (group.isExpanded()) {
                    shown.addAll(entries);
                }
            }
        }
        if (shown.equals(list.getItems())) {
            return;
        }
        ShortcutListItem selected = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(shown);
        if (selected instanceof ShortcutEntry entry && !shown.contains(entry)) {
            selected = groupOf(entry);
        }
        if (selected != null && shown.contains(selected)) {
            list.getSelectionModel().select(selected);
        } else {
            list.getSelectionModel().clearSelection();
        }
    }

    /**
     * The rows of a group that show, in their order.
     */
    private List<ShortcutEntry> shownEntries(ShortcutGroupEntry group) {
        return group.getEntries();
    }

    private ShortcutGroupEntry groupOf(ShortcutEntry entry) {
        for (ShortcutGroupEntry group : groups) {
            if (group.getEntries().contains(entry)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Writes the counter: every command (fixed ones included), the modified ones, the conflicts and the ones without
     * a key.
     */
    private void updateCounter() {
        int commands = 0;
        int modified = 0;
        int notAssigned = 0;
        for (ShortcutGroupEntry group : groups) {
            for (ShortcutEntry entry : group.getEntries()) {
                commands++;
                if (entry.isModified()) {
                    modified++;
                }
                if (entry.getShortcut() == null) {
                    notAssigned++;
                }
            }
        }
        commandCount.setText(count(commands, commandCountOneFormat, commandCountFormat));
        modifiedCount.setText(count(modified, modifiedCountOneFormat, modifiedCountFormat));
        conflictCount.setText(count(0, conflictCountOneFormat, conflictCountFormat));
        notAssignedCount.setText(count(notAssigned, notAssignedCountOneFormat, notAssignedCountFormat));
    }

    private static String count(int count, StringProperty oneFormat, StringProperty format) {
        return fill(count == 1 ? oneFormat.get() : format.get(), Integer.toString(count));
    }

    /**
     * Fills a format: {@code {0}} becomes the first value, {@code {1}} the second, each wherever it occurs; everything
     * else stays as it is, so that a format never fails, whatever an application or a translator writes.
     *
     * @param format the format; null counts as empty
     * @param values the values
     * @return the text
     */
    static String fill(String format, String... values) {
        if (format == null) {
            return "";
        }
        String text = format;
        for (int index = 0; index < values.length; index++) {
            text = text.replace("{" + index + "}", values[index]);
        }
        return text;
    }

    // --- Rows ------------------------------------------------------------------------------------------

    /**
     * Folds or opens a group.
     *
     * @param group the group's line
     */
    void toggle(ShortcutGroupEntry group) {
        group.setExpanded(!group.isExpanded());
        changed();
    }

    /**
     * Removes a command's key in the working copy.
     *
     * @param entry the command's row
     */
    void remove(ShortcutEntry entry) {
        if (entry.isChangeable()) {
            entry.setShortcut(null);
            changed();
        }
    }

    /**
     * Puts a command's default key back in the working copy.
     *
     * @param entry the command's row
     */
    void reset(ShortcutEntry entry) {
        if (entry.isChangeable()) {
            entry.setShortcut(entry.getCommand().getDefaultShortcut());
            changed();
        }
    }

    /**
     * The keys the list takes before its own handling: on a command's row Enter records and Delete or Backspace
     * removes the key, on a group's line Enter or Space folds or opens and Left or Right folds or opens. While a
     * recording runs, its field takes every key.
     */
    private void listKeyPressed(KeyEvent press) {
        if (recording != null || press.isShortcutDown() || press.isAltDown() || press.isShiftDown()
                || press.isMetaDown()) {
            return;
        }
        ShortcutListItem selected = list.getSelectionModel().getSelectedItem();
        KeyCode code = press.getCode();
        if (selected instanceof ShortcutEntry entry && entry.isChangeable()) {
            if (code == KeyCode.ENTER) {
                startRecording(entry);
                press.consume();
            } else if (code == KeyCode.DELETE || code == KeyCode.BACK_SPACE) {
                remove(entry);
                press.consume();
            }
        } else if (selected instanceof ShortcutGroupEntry group) {
            if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                toggle(group);
                press.consume();
            } else if (code == KeyCode.LEFT && group.isExpanded() || code == KeyCode.RIGHT && !group.isExpanded()) {
                toggle(group);
                press.consume();
            }
        }
    }

    // --- Recording -------------------------------------------------------------------------------------

    /**
     * Starts recording a new key for a command; a recording for another command ends without a key.
     *
     * @param entry the command's row
     */
    void startRecording(ShortcutEntry entry) {
        if (!entry.isChangeable() || recording == entry) {
            return;
        }
        cancelRecording(false);
        recording = entry;
        recorderFocused = false;
        recorder.start();
        list.getSelectionModel().select(entry);
        changed();
    }

    /**
     * Ends the recording with a new key for its command.
     *
     * @param shortcut the key the user pressed
     */
    void recorded(Shortcut shortcut) {
        ShortcutEntry entry = recording;
        recording = null;
        entry.setShortcut(shortcut);
        changed();
        list.requestFocus();
    }

    /**
     * Ends a recording that runs and keeps the old key.
     *
     * @param focusList whether the list takes the focus back, as after Esc; not when the focus went elsewhere
     */
    void cancelRecording(boolean focusList) {
        if (recording == null) {
            return;
        }
        recording = null;
        changed();
        if (focusList) {
            list.requestFocus();
        }
    }

    /**
     * Whether a row records a new key.
     *
     * @param entry the command's row
     * @return true while it records
     */
    boolean isRecording(ShortcutEntry entry) {
        return recording == entry;
    }

    /**
     * Returns the recording field, which the row that records shows in place of its key caps.
     *
     * @return the recording field
     */
    ShortcutRecorder recorder() {
        return recorder;
    }

    /**
     * Whether an event's target is the recording field or inside it.
     *
     * @param target the target
     * @return true when it is
     */
    boolean isInRecorder(EventTarget target) {
        for (Node node = target instanceof Node found ? found : null; node != null; node = node.getParent()) {
            if (node == recorder) {
                return true;
            }
        }
        return false;
    }

    private void sceneChanged(Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.focusOwnerProperty().removeListener(focusOwnerListener);
        }
        if (newScene != null) {
            newScene.focusOwnerProperty().addListener(focusOwnerListener);
        }
    }

    /**
     * The focus moved in the editor's scene: to the recording field, or away from it, which ends the recording.
     */
    private void focusMoved(Node newOwner) {
        if (recording == null) {
            return;
        }
        if (newOwner == recorder) {
            recorderFocused = true;
        } else if (recorderFocused) {
            cancelRecording(false);
        }
    }

    // --- Package-private hooks of the cells, the recording field and the dialog ------------------------

    /**
     * Counts up whenever the working copy changes.
     *
     * @return the revision
     */
    Observable revision() {
        return revision;
    }

    /**
     * The width of the command column, in the header and in every row.
     *
     * @return the width
     */
    ObservableValue<Number> commandColumnWidth() {
        return commandColumnWidth;
    }

    /**
     * The context of the whole window, whose commands carry no mark.
     *
     * @return the registry's window context
     */
    CommandContext windowContext() {
        return registry.getWindowContext();
    }

    /**
     * The number of a group's rows that show.
     *
     * @param group the group's line
     * @return the number
     */
    int shownCount(ShortcutGroupEntry group) {
        return shownEntries(group).size();
    }

    /**
     * The buttons at the right end of the footer, beside the counter: a dialog puts its own there.
     *
     * @return the footer's buttons
     */
    ObservableList<Node> footerButtons() {
        return footerButtons.getChildren();
    }

    // --- Texts -----------------------------------------------------------------------------------------

    /**
     * The placeholder of the search field (default <i>Filter by command or key</i>).
     *
     * @return the search prompt text property
     * @since 1.1.0-RC2
     */
    public StringProperty searchPromptTextProperty() {
        return searchPromptText;
    }

    /**
     * Returns the placeholder of the search field.
     *
     * @return the search prompt text
     * @since 1.1.0-RC2
     */
    public String getSearchPromptText() {
        return searchPromptText.get();
    }

    /**
     * Sets the placeholder of the search field.
     *
     * @param text the search prompt text
     * @since 1.1.0-RC2
     */
    public void setSearchPromptText(String text) {
        searchPromptText.set(text);
    }

    /**
     * The filter that shows every command (default <i>All</i>).
     *
     * @return the all filter text property
     * @since 1.1.0-RC2
     */
    public StringProperty allFilterTextProperty() {
        return allFilterText;
    }

    /**
     * Returns the text of the filter that shows every command.
     *
     * @return the all filter text
     * @since 1.1.0-RC2
     */
    public String getAllFilterText() {
        return allFilterText.get();
    }

    /**
     * Sets the text of the filter that shows every command.
     *
     * @param text the all filter text
     * @since 1.1.0-RC2
     */
    public void setAllFilterText(String text) {
        allFilterText.set(text);
    }

    /**
     * The filter that shows the commands whose keys differ from their defaults (default <i>Modified</i>).
     *
     * @return the modified filter text property
     * @since 1.1.0-RC2
     */
    public StringProperty modifiedFilterTextProperty() {
        return modifiedFilterText;
    }

    /**
     * Returns the text of the filter that shows the modified commands.
     *
     * @return the modified filter text
     * @since 1.1.0-RC2
     */
    public String getModifiedFilterText() {
        return modifiedFilterText.get();
    }

    /**
     * Sets the text of the filter that shows the modified commands.
     *
     * @param text the modified filter text
     * @since 1.1.0-RC2
     */
    public void setModifiedFilterText(String text) {
        modifiedFilterText.set(text);
    }

    /**
     * The filter that shows the commands in a conflict (default <i>Conflicts</i>).
     *
     * @return the conflicts filter text property
     * @since 1.1.0-RC2
     */
    public StringProperty conflictsFilterTextProperty() {
        return conflictsFilterText;
    }

    /**
     * Returns the text of the filter that shows the commands in a conflict.
     *
     * @return the conflicts filter text
     * @since 1.1.0-RC2
     */
    public String getConflictsFilterText() {
        return conflictsFilterText.get();
    }

    /**
     * Sets the text of the filter that shows the commands in a conflict.
     *
     * @param text the conflicts filter text
     * @since 1.1.0-RC2
     */
    public void setConflictsFilterText(String text) {
        conflictsFilterText.set(text);
    }

    /**
     * The button that puts every key back to its default (default <i>Reset all</i>).
     *
     * @return the reset all text property
     * @since 1.1.0-RC2
     */
    public StringProperty resetAllTextProperty() {
        return resetAllText;
    }

    /**
     * Returns the text of the button that puts every key back to its default.
     *
     * @return the reset all text
     * @since 1.1.0-RC2
     */
    public String getResetAllText() {
        return resetAllText.get();
    }

    /**
     * Sets the text of the button that puts every key back to its default.
     *
     * @param text the reset all text
     * @since 1.1.0-RC2
     */
    public void setResetAllText(String text) {
        resetAllText.set(text);
    }

    /**
     * The title of the command column (default <i>Command</i>), shown in capitals.
     *
     * @return the command column text property
     * @since 1.1.0-RC2
     */
    public StringProperty commandColumnTextProperty() {
        return commandColumnText;
    }

    /**
     * Returns the title of the command column.
     *
     * @return the command column text
     * @since 1.1.0-RC2
     */
    public String getCommandColumnText() {
        return commandColumnText.get();
    }

    /**
     * Sets the title of the command column.
     *
     * @param text the command column text
     * @since 1.1.0-RC2
     */
    public void setCommandColumnText(String text) {
        commandColumnText.set(text);
    }

    /**
     * The title of the shortcut column (default <i>Shortcut</i>), shown in capitals.
     *
     * @return the shortcut column text property
     * @since 1.1.0-RC2
     */
    public StringProperty shortcutColumnTextProperty() {
        return shortcutColumnText;
    }

    /**
     * Returns the title of the shortcut column.
     *
     * @return the shortcut column text
     * @since 1.1.0-RC2
     */
    public String getShortcutColumnText() {
        return shortcutColumnText.get();
    }

    /**
     * Sets the title of the shortcut column.
     *
     * @param text the shortcut column text
     * @since 1.1.0-RC2
     */
    public void setShortcutColumnText(String text) {
        shortcutColumnText.set(text);
    }

    /**
     * What a command without a key shows in place of its key caps (default <i>Not assigned</i>).
     *
     * @return the not assigned text property
     * @since 1.1.0-RC2
     */
    public StringProperty notAssignedTextProperty() {
        return notAssignedText;
    }

    /**
     * Returns what a command without a key shows.
     *
     * @return the not assigned text
     * @since 1.1.0-RC2
     */
    public String getNotAssignedText() {
        return notAssignedText.get();
    }

    /**
     * Sets what a command without a key shows.
     *
     * @param text the not assigned text
     * @since 1.1.0-RC2
     */
    public void setNotAssignedText(String text) {
        notAssignedText.set(text);
    }

    /**
     * The recording field's prompt while it waits for a key (default <i>Press keys…</i>).
     *
     * @return the press keys text property
     * @since 1.1.0-RC2
     */
    public StringProperty pressKeysTextProperty() {
        return pressKeysText;
    }

    /**
     * Returns the recording field's prompt.
     *
     * @return the press keys text
     * @since 1.1.0-RC2
     */
    public String getPressKeysText() {
        return pressKeysText.get();
    }

    /**
     * Sets the recording field's prompt.
     *
     * @param text the press keys text
     * @since 1.1.0-RC2
     */
    public void setPressKeysText(String text) {
        pressKeysText.set(text);
    }

    /**
     * The hint at the recording field's end (default <i>Esc to cancel</i>).
     *
     * @return the esc to cancel text property
     * @since 1.1.0-RC2
     */
    public StringProperty escToCancelTextProperty() {
        return escToCancelText;
    }

    /**
     * Returns the hint at the recording field's end.
     *
     * @return the esc to cancel text
     * @since 1.1.0-RC2
     */
    public String getEscToCancelText() {
        return escToCancelText.get();
    }

    /**
     * Sets the hint at the recording field's end.
     *
     * @param text the esc to cancel text
     * @since 1.1.0-RC2
     */
    public void setEscToCancelText(String text) {
        escToCancelText.set(text);
    }

    /**
     * Why the recording field refuses a key without Ctrl or Alt, such as a plain letter, Shift and a letter, Esc, Tab,
     * Enter, Space or Backspace (default <i>{0} needs Ctrl or Alt</i>): {@code {0}} stands for the key pressed, such
     * as <i>Shift+A</i>.
     *
     * @return the needs Ctrl or Alt format property
     * @since 1.1.0-RC2
     */
    public StringProperty needsCtrlOrAltFormatProperty() {
        return needsCtrlOrAltFormat;
    }

    /**
     * Returns why the recording field refuses a key without Ctrl or Alt.
     *
     * @return the needs Ctrl or Alt format
     * @since 1.1.0-RC2
     */
    public String getNeedsCtrlOrAltFormat() {
        return needsCtrlOrAltFormat.get();
    }

    /**
     * Sets why the recording field refuses a key without Ctrl or Alt; {@code {0}} stands for the key pressed.
     *
     * @param format the needs Ctrl or Alt format
     * @since 1.1.0-RC2
     */
    public void setNeedsCtrlOrAltFormat(String format) {
        needsCtrlOrAltFormat.set(format);
    }

    /**
     * Why the recording field refuses a key no shortcut can be on, such as a key the platform does not name (default
     * <i>This key cannot be a shortcut</i>).
     *
     * @return the unusable key text property
     * @since 1.1.0-RC2
     */
    public StringProperty unusableKeyTextProperty() {
        return unusableKeyText;
    }

    /**
     * Returns why the recording field refuses a key no shortcut can be on.
     *
     * @return the unusable key text
     * @since 1.1.0-RC2
     */
    public String getUnusableKeyText() {
        return unusableKeyText.get();
    }

    /**
     * Sets why the recording field refuses a key no shortcut can be on.
     *
     * @param text the unusable key text
     * @since 1.1.0-RC2
     */
    public void setUnusableKeyText(String text) {
        unusableKeyText.set(text);
    }

    /**
     * The tooltip and accessible text of a row's pencil, which records a new key (default <i>Change shortcut</i>).
     *
     * @return the edit tooltip text property
     * @since 1.1.0-RC2
     */
    public StringProperty editTooltipTextProperty() {
        return editTooltipText;
    }

    /**
     * Returns the tooltip of a row's pencil.
     *
     * @return the edit tooltip text
     * @since 1.1.0-RC2
     */
    public String getEditTooltipText() {
        return editTooltipText.get();
    }

    /**
     * Sets the tooltip of a row's pencil.
     *
     * @param text the edit tooltip text
     * @since 1.1.0-RC2
     */
    public void setEditTooltipText(String text) {
        editTooltipText.set(text);
    }

    /**
     * The tooltip and accessible text of a row's &times;, which removes the key (default <i>Remove shortcut</i>).
     *
     * @return the remove tooltip text property
     * @since 1.1.0-RC2
     */
    public StringProperty removeTooltipTextProperty() {
        return removeTooltipText;
    }

    /**
     * Returns the tooltip of a row's &times;.
     *
     * @return the remove tooltip text
     * @since 1.1.0-RC2
     */
    public String getRemoveTooltipText() {
        return removeTooltipText.get();
    }

    /**
     * Sets the tooltip of a row's &times;.
     *
     * @param text the remove tooltip text
     * @since 1.1.0-RC2
     */
    public void setRemoveTooltipText(String text) {
        removeTooltipText.set(text);
    }

    /**
     * The tooltip and accessible text of a modified row's reset button, which puts the default key back (default
     * <i>Reset to default</i>).
     *
     * @return the reset tooltip text property
     * @since 1.1.0-RC2
     */
    public StringProperty resetTooltipTextProperty() {
        return resetTooltipText;
    }

    /**
     * Returns the tooltip of a row's reset button.
     *
     * @return the reset tooltip text
     * @since 1.1.0-RC2
     */
    public String getResetTooltipText() {
        return resetTooltipText.get();
    }

    /**
     * Sets the tooltip of a row's reset button.
     *
     * @param text the reset tooltip text
     * @since 1.1.0-RC2
     */
    public void setResetTooltipText(String text) {
        resetTooltipText.set(text);
    }

    /**
     * The counter's number of commands (default <i>{0} commands</i>): {@code {0}} stands for the number, every
     * command, fixed ones included. Used unless the number is one.
     *
     * @return the command count format property
     * @since 1.1.0-RC2
     */
    public StringProperty commandCountFormatProperty() {
        return commandCountFormat;
    }

    /**
     * Returns the counter's format of the number of commands.
     *
     * @return the command count format
     * @since 1.1.0-RC2
     */
    public String getCommandCountFormat() {
        return commandCountFormat.get();
    }

    /**
     * Sets the counter's format of the number of commands; {@code {0}} stands for the number.
     *
     * @param format the command count format
     * @since 1.1.0-RC2
     */
    public void setCommandCountFormat(String format) {
        commandCountFormat.set(format);
    }

    /**
     * The counter's number of commands when it is one (default <i>{0} command</i>).
     *
     * @return the command count one format property
     * @since 1.1.0-RC2
     */
    public StringProperty commandCountOneFormatProperty() {
        return commandCountOneFormat;
    }

    /**
     * Returns the counter's format of one command.
     *
     * @return the command count one format
     * @since 1.1.0-RC2
     */
    public String getCommandCountOneFormat() {
        return commandCountOneFormat.get();
    }

    /**
     * Sets the counter's format of one command; {@code {0}} stands for the number.
     *
     * @param format the command count one format
     * @since 1.1.0-RC2
     */
    public void setCommandCountOneFormat(String format) {
        commandCountOneFormat.set(format);
    }

    /**
     * The counter's number of commands whose keys differ from their defaults (default <i>{0} modified</i>):
     * {@code {0}} stands for the number. Used unless the number is one.
     *
     * @return the modified count format property
     * @since 1.1.0-RC2
     */
    public StringProperty modifiedCountFormatProperty() {
        return modifiedCountFormat;
    }

    /**
     * Returns the counter's format of the number of modified commands.
     *
     * @return the modified count format
     * @since 1.1.0-RC2
     */
    public String getModifiedCountFormat() {
        return modifiedCountFormat.get();
    }

    /**
     * Sets the counter's format of the number of modified commands; {@code {0}} stands for the number.
     *
     * @param format the modified count format
     * @since 1.1.0-RC2
     */
    public void setModifiedCountFormat(String format) {
        modifiedCountFormat.set(format);
    }

    /**
     * The counter's number of modified commands when it is one (default <i>{0} modified</i>).
     *
     * @return the modified count one format property
     * @since 1.1.0-RC2
     */
    public StringProperty modifiedCountOneFormatProperty() {
        return modifiedCountOneFormat;
    }

    /**
     * Returns the counter's format of one modified command.
     *
     * @return the modified count one format
     * @since 1.1.0-RC2
     */
    public String getModifiedCountOneFormat() {
        return modifiedCountOneFormat.get();
    }

    /**
     * Sets the counter's format of one modified command; {@code {0}} stands for the number.
     *
     * @param format the modified count one format
     * @since 1.1.0-RC2
     */
    public void setModifiedCountOneFormat(String format) {
        modifiedCountOneFormat.set(format);
    }

    /**
     * The counter's number of conflicts, keys that two commands of the same context hold (default <i>{0}
     * conflicts</i>): {@code {0}} stands for the number. Used unless the number is one.
     *
     * @return the conflict count format property
     * @since 1.1.0-RC2
     */
    public StringProperty conflictCountFormatProperty() {
        return conflictCountFormat;
    }

    /**
     * Returns the counter's format of the number of conflicts.
     *
     * @return the conflict count format
     * @since 1.1.0-RC2
     */
    public String getConflictCountFormat() {
        return conflictCountFormat.get();
    }

    /**
     * Sets the counter's format of the number of conflicts; {@code {0}} stands for the number.
     *
     * @param format the conflict count format
     * @since 1.1.0-RC2
     */
    public void setConflictCountFormat(String format) {
        conflictCountFormat.set(format);
    }

    /**
     * The counter's number of conflicts when it is one (default <i>{0} conflict</i>).
     *
     * @return the conflict count one format property
     * @since 1.1.0-RC2
     */
    public StringProperty conflictCountOneFormatProperty() {
        return conflictCountOneFormat;
    }

    /**
     * Returns the counter's format of one conflict.
     *
     * @return the conflict count one format
     * @since 1.1.0-RC2
     */
    public String getConflictCountOneFormat() {
        return conflictCountOneFormat.get();
    }

    /**
     * Sets the counter's format of one conflict; {@code {0}} stands for the number.
     *
     * @param format the conflict count one format
     * @since 1.1.0-RC2
     */
    public void setConflictCountOneFormat(String format) {
        conflictCountOneFormat.set(format);
    }

    /**
     * The counter's number of commands without a key (default <i>{0} not assigned</i>): {@code {0}} stands for the
     * number. Used unless the number is one.
     *
     * @return the not assigned count format property
     * @since 1.1.0-RC2
     */
    public StringProperty notAssignedCountFormatProperty() {
        return notAssignedCountFormat;
    }

    /**
     * Returns the counter's format of the number of commands without a key.
     *
     * @return the not assigned count format
     * @since 1.1.0-RC2
     */
    public String getNotAssignedCountFormat() {
        return notAssignedCountFormat.get();
    }

    /**
     * Sets the counter's format of the number of commands without a key; {@code {0}} stands for the number.
     *
     * @param format the not assigned count format
     * @since 1.1.0-RC2
     */
    public void setNotAssignedCountFormat(String format) {
        notAssignedCountFormat.set(format);
    }

    /**
     * The counter's number of commands without a key when it is one (default <i>{0} not assigned</i>).
     *
     * @return the not assigned count one format property
     * @since 1.1.0-RC2
     */
    public StringProperty notAssignedCountOneFormatProperty() {
        return notAssignedCountOneFormat;
    }

    /**
     * Returns the counter's format of one command without a key.
     *
     * @return the not assigned count one format
     * @since 1.1.0-RC2
     */
    public String getNotAssignedCountOneFormat() {
        return notAssignedCountOneFormat.get();
    }

    /**
     * Sets the counter's format of one command without a key; {@code {0}} stands for the number.
     *
     * @param format the not assigned count one format
     * @since 1.1.0-RC2
     */
    public void setNotAssignedCountOneFormat(String format) {
        notAssignedCountOneFormat.set(format);
    }
}
