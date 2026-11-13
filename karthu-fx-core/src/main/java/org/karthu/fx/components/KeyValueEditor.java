package org.karthu.fx.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An editor for a list of key-value pairs, such as the headers of an HTTP request: a title, one row per pair and
 * an add button below.
 *
 * <p>Each row shows a CheckBox that switches the pair on or off, a key field, a value field, a lock that masks the
 * value and a delete button. The rows are {@link #getEntries()}, an observable list of {@link KeyValueEntry}:
 * filling it from outside shows the rows, and the row's fields write to their entry's properties.
 * {@link #snapshot()} hands the content over as an immutable list, without fully empty rows. An inactive row is
 * dimmed but stays editable, so a pair can be prepared before it is switched on.
 *
 * <p>The add button appends an active, unmasked, empty row and puts the focus in its key field. A row's delete
 * button removes the row at once, without confirmation, and moves the focus to the next row's delete button, else
 * the previous row's, else the add button; the last row can be removed too. Rows keep the order of the list; the
 * editor offers no reordering.
 *
 * <p>The lock is a toggle bound to the entry's {@link KeyValueEntry#maskedProperty() masked} flag: closed, the
 * value shows in a password field with the same text. What opening it does is the application's choice
 * ({@link #revealAllowedProperty()}): reveal the value (the default), or clear it and put the focus in the value
 * field, so a strict application never shows a secret again.
 *
 * <p>The editor never refuses input; it marks problems on the key field. An active row with a value but an empty
 * key carries {@code key-value-key-missing}. When {@link #duplicateKeysAllowedProperty() duplicate keys} are
 * forbidden, every active row whose key another active row has too carries {@code key-value-key-duplicate}, the
 * keys compared case-insensitively with {@link #ignoreKeyCaseProperty()}; inactive rows and empty keys never count
 * as duplicates — unless {@link #emptyKeyAllowedProperty()} makes the empty key a key like any other, never missing
 * and a duplicate when two active rows hold it. The CheckBox column and the lock column can be hidden, and {@link #editableProperty()} makes the
 * whole editor read-only.
 *
 * <p>All texts are properties: the title, the add button's text, the placeholders of the key and value fields and
 * the tooltips. Title, button text and tooltips have English defaults; the placeholders are empty unless set. An
 * empty title hides the title row.
 *
 * <p>Styling comes from the theme stylesheet through the style classes {@code key-value-editor} (the editor),
 * {@code key-value-title}, {@code key-value-rows} (the box of all rows), {@code key-value-row} (a row) and
 * {@code key-value-row-inactive} (a row whose pair is switched off), {@code key-value-enabled} (its CheckBox),
 * {@code key-value-key} with the marks {@code key-value-key-missing} and {@code key-value-key-duplicate},
 * {@code key-value-value} (a text field, or a password field while masked), {@code key-value-lock},
 * {@code key-value-delete} and {@code key-value-add}.
 */
public class KeyValueEditor extends VBox {

    /** The row style class of a row whose pair is switched off; the theme dims it. */
    private static final String INACTIVE_ROW_CLASS = "key-value-row-inactive";
    /** The key field's mark: an active row with a value but no key. */
    private static final String MISSING_KEY_CLASS = "key-value-key-missing";
    /** The key field's mark: a key that another active row has too, while duplicates are forbidden. */
    private static final String DUPLICATE_KEY_CLASS = "key-value-key-duplicate";

    private final ObservableList<KeyValueEntry> entries = FXCollections.observableArrayList();

    private final StringProperty title = new SimpleStringProperty(this, "title", "Entries");
    private final StringProperty addText = new SimpleStringProperty(this, "addText", "Add");
    private final StringProperty keyPromptText = new SimpleStringProperty(this, "keyPromptText", "");
    private final StringProperty valuePromptText = new SimpleStringProperty(this, "valuePromptText", "");
    private final StringProperty enabledTooltipText =
            new SimpleStringProperty(this, "enabledTooltipText", "Use this entry");
    private final StringProperty deleteTooltipText =
            new SimpleStringProperty(this, "deleteTooltipText", "Remove this entry");
    private final StringProperty lockTooltipText =
            new SimpleStringProperty(this, "lockTooltipText", "Mask the value");

    private final BooleanProperty revealAllowed = new SimpleBooleanProperty(this, "revealAllowed", true);
    private final BooleanProperty lockColumnVisible = new SimpleBooleanProperty(this, "lockColumnVisible", true);
    private final BooleanProperty enabledColumnVisible =
            new SimpleBooleanProperty(this, "enabledColumnVisible", true);
    private final BooleanProperty duplicateKeysAllowed =
            new SimpleBooleanProperty(this, "duplicateKeysAllowed", true);
    private final BooleanProperty ignoreKeyCase = new SimpleBooleanProperty(this, "ignoreKeyCase", false);
    private final BooleanProperty emptyKeyAllowed = new SimpleBooleanProperty(this, "emptyKeyAllowed", false);
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);

    private final Label titleLabel = new Label();
    private final VBox rowBox = new VBox();
    private final Button addButton = new Button();

    /** The row of each entry, by identity: two equal entries are still two rows. */
    private final Map<KeyValueEntry, Row> rowsByEntry = new IdentityHashMap<>();

    /**
     * Creates an editor without rows, titled "Entries", with the add button "Add" and English tooltips; both
     * columns shown, revealing allowed, duplicate keys allowed and compared case-sensitively, editable.
     */
    public KeyValueEditor() {
        getStyleClass().add("key-value-editor");

        titleLabel.getStyleClass().add("key-value-title");
        titleLabel.textProperty().bind(title);
        titleLabel.visibleProperty().bind(title.isNotEmpty());
        titleLabel.managedProperty().bind(titleLabel.visibleProperty());

        rowBox.getStyleClass().add("key-value-rows");

        addButton.getStyleClass().add("key-value-add");
        addButton.textProperty().bind(addText);
        addButton.setGraphic(FontIcon.of(BootstrapIcons.PLUS));
        addButton.setOnAction(event -> addEntry());
        addButton.disableProperty().bind(editable.not());

        getChildren().addAll(titleLabel, rowBox, addButton);
        entries.addListener((ListChangeListener<KeyValueEntry>) change -> syncRows());
        duplicateKeysAllowed.addListener((observable, oldValue, newValue) -> updateMarks());
        ignoreKeyCase.addListener((observable, oldValue, newValue) -> updateMarks());
        emptyKeyAllowed.addListener((observable, oldValue, newValue) -> updateMarks());
    }

    // --- Public API ----------------------------------------------------------------------------------

    /**
     * Returns the rows, in display order. Adding, removing or replacing entries updates the rows at once; an
     * entry must appear at most once.
     *
     * @return the live list of entries
     */
    public ObservableList<KeyValueEntry> getEntries() {
        return entries;
    }

    /**
     * Returns the current content as an immutable list, in display order, without fully empty rows (empty key and
     * empty value, whatever the flags).
     *
     * @return an immutable list of the rows' values
     */
    public List<KeyValueEntry.Snapshot> snapshot() {
        List<KeyValueEntry.Snapshot> result = new ArrayList<>();
        for (KeyValueEntry entry : entries) {
            if (!entry.isBlank()) {
                result.add(entry.toSnapshot());
            }
        }
        return List.copyOf(result);
    }

    /**
     * The title above the rows (default "Entries"). An empty or null title hides the title row.
     *
     * @return the title property
     */
    public StringProperty titleProperty() {
        return title;
    }

    /**
     * Returns the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title.get();
    }

    /**
     * Sets the title; empty or null hides the title row.
     *
     * @param value the title
     */
    public void setTitle(String value) {
        title.set(value);
    }

    /**
     * The add button's text (default "Add"), shown beside its plus icon.
     *
     * @return the add text property
     */
    public StringProperty addTextProperty() {
        return addText;
    }

    /**
     * Returns the add button's text.
     *
     * @return the add button's text
     */
    public String getAddText() {
        return addText.get();
    }

    /**
     * Sets the add button's text.
     *
     * @param value the add button's text
     */
    public void setAddText(String value) {
        addText.set(value);
    }

    /**
     * The placeholder of every row's key field (default empty).
     *
     * @return the key prompt text property
     */
    public StringProperty keyPromptTextProperty() {
        return keyPromptText;
    }

    /**
     * Returns the placeholder of the key fields.
     *
     * @return the placeholder of the key fields
     */
    public String getKeyPromptText() {
        return keyPromptText.get();
    }

    /**
     * Sets the placeholder of the key fields.
     *
     * @param value the placeholder of the key fields
     */
    public void setKeyPromptText(String value) {
        keyPromptText.set(value);
    }

    /**
     * The placeholder of every row's value field (default empty).
     *
     * @return the value prompt text property
     */
    public StringProperty valuePromptTextProperty() {
        return valuePromptText;
    }

    /**
     * Returns the placeholder of the value fields.
     *
     * @return the placeholder of the value fields
     */
    public String getValuePromptText() {
        return valuePromptText.get();
    }

    /**
     * Sets the placeholder of the value fields.
     *
     * @param value the placeholder of the value fields
     */
    public void setValuePromptText(String value) {
        valuePromptText.set(value);
    }

    /**
     * The tooltip of every row's CheckBox (default "Use this entry").
     *
     * @return the enabled tooltip text property
     */
    public StringProperty enabledTooltipTextProperty() {
        return enabledTooltipText;
    }

    /**
     * Returns the tooltip of the CheckBoxes.
     *
     * @return the tooltip of the CheckBoxes
     */
    public String getEnabledTooltipText() {
        return enabledTooltipText.get();
    }

    /**
     * Sets the tooltip of the CheckBoxes.
     *
     * @param value the tooltip of the CheckBoxes
     */
    public void setEnabledTooltipText(String value) {
        enabledTooltipText.set(value);
    }

    /**
     * The tooltip and accessible text of every row's delete button (default "Remove this entry").
     *
     * @return the delete tooltip text property
     */
    public StringProperty deleteTooltipTextProperty() {
        return deleteTooltipText;
    }

    /**
     * Returns the tooltip of the delete buttons.
     *
     * @return the tooltip of the delete buttons
     */
    public String getDeleteTooltipText() {
        return deleteTooltipText.get();
    }

    /**
     * Sets the tooltip of the delete buttons.
     *
     * @param value the tooltip of the delete buttons
     */
    public void setDeleteTooltipText(String value) {
        deleteTooltipText.set(value);
    }

    /**
     * The tooltip of every row's lock, which masks the value (default "Mask the value").
     *
     * @return the lock tooltip text property
     */
    public StringProperty lockTooltipTextProperty() {
        return lockTooltipText;
    }

    /**
     * Returns the tooltip of the locks.
     *
     * @return the tooltip of the locks
     */
    public String getLockTooltipText() {
        return lockTooltipText.get();
    }

    /**
     * Sets the tooltip of the locks.
     *
     * @param value the tooltip of the locks
     */
    public void setLockTooltipText(String value) {
        lockTooltipText.set(value);
    }

    /**
     * Whether opening a row's lock reveals the value (default true). When false, opening the lock clears the
     * value and puts the focus in the value field, so a masked value is never shown again; this holds for an
     * entry the application unmasks as well.
     *
     * @return the reveal allowed property
     */
    public BooleanProperty revealAllowedProperty() {
        return revealAllowed;
    }

    /**
     * Returns whether opening a lock reveals the value.
     *
     * @return true if opening a lock reveals the value
     */
    public boolean isRevealAllowed() {
        return revealAllowed.get();
    }

    /**
     * Sets whether opening a lock reveals the value; false clears it instead.
     *
     * @param value true to reveal, false to clear
     */
    public void setRevealAllowed(boolean value) {
        revealAllowed.set(value);
    }

    /**
     * Whether the rows show their lock (default true). Hidden, an entry's masked flag still applies.
     *
     * @return the lock column visible property
     */
    public BooleanProperty lockColumnVisibleProperty() {
        return lockColumnVisible;
    }

    /**
     * Returns whether the rows show their lock.
     *
     * @return true if the lock column is shown
     */
    public boolean isLockColumnVisible() {
        return lockColumnVisible.get();
    }

    /**
     * Shows or hides the lock column.
     *
     * @param value true to show the lock column
     */
    public void setLockColumnVisible(boolean value) {
        lockColumnVisible.set(value);
    }

    /**
     * Whether the rows show their CheckBox (default true). Hidden, the editor serves as a plain map editor; an
     * entry's enabled flag still applies.
     *
     * @return the enabled column visible property
     */
    public BooleanProperty enabledColumnVisibleProperty() {
        return enabledColumnVisible;
    }

    /**
     * Returns whether the rows show their CheckBox.
     *
     * @return true if the CheckBox column is shown
     */
    public boolean isEnabledColumnVisible() {
        return enabledColumnVisible.get();
    }

    /**
     * Shows or hides the CheckBox column.
     *
     * @param value true to show the CheckBox column
     */
    public void setEnabledColumnVisible(boolean value) {
        enabledColumnVisible.set(value);
    }

    /**
     * Whether active rows may share a key (default true, as HTTP headers do). When false, the key field of every
     * active row whose key another active row has too is marked with {@code key-value-key-duplicate}; the input
     * is never refused.
     *
     * @return the duplicate keys allowed property
     */
    public BooleanProperty duplicateKeysAllowedProperty() {
        return duplicateKeysAllowed;
    }

    /**
     * Returns whether active rows may share a key.
     *
     * @return true if duplicate keys are allowed
     */
    public boolean isDuplicateKeysAllowed() {
        return duplicateKeysAllowed.get();
    }

    /**
     * Allows or forbids duplicate keys among active rows.
     *
     * @param value true to allow duplicate keys
     */
    public void setDuplicateKeysAllowed(boolean value) {
        duplicateKeysAllowed.set(value);
    }

    /**
     * Whether keys are compared case-insensitively when duplicates are forbidden (default false), so that
     * {@code Accept} and {@code accept} count as the same key. With duplicates allowed it has no effect.
     *
     * @return the ignore key case property
     */
    public BooleanProperty ignoreKeyCaseProperty() {
        return ignoreKeyCase;
    }

    /**
     * Returns whether keys are compared case-insensitively.
     *
     * @return true if keys are compared case-insensitively
     */
    public boolean isIgnoreKeyCase() {
        return ignoreKeyCase.get();
    }

    /**
     * Makes the key comparison case-insensitive or case-sensitive.
     *
     * @param value true to compare keys case-insensitively
     */
    public void setIgnoreKeyCase(boolean value) {
        ignoreKeyCase.set(value);
    }

    /**
     * Whether the empty key is a key like any other (default false), for applications whose pairs are a map that may
     * hold it. When true, an active row with a value but no key is not marked as missing, and empty keys count for
     * the duplicate marking: two active rows with the empty key are marked as duplicates while duplicates are
     * forbidden. When false, an empty key is missing, never a duplicate.
     *
     * @return the empty key allowed property
     */
    public BooleanProperty emptyKeyAllowedProperty() {
        return emptyKeyAllowed;
    }

    /**
     * Returns whether the empty key is a key like any other.
     *
     * @return true if the empty key is allowed
     */
    public boolean isEmptyKeyAllowed() {
        return emptyKeyAllowed.get();
    }

    /**
     * Allows the empty key as a key like any other, or marks it as missing.
     *
     * @param value true to allow the empty key
     */
    public void setEmptyKeyAllowed(boolean value) {
        emptyKeyAllowed.set(value);
    }

    /**
     * Whether the pairs can be changed (default true). When false, the key and value fields are read-only and the
     * CheckBoxes, locks, delete buttons and the add button are disabled, so the pairs show read-only.
     *
     * @return the editable property
     */
    public BooleanProperty editableProperty() {
        return editable;
    }

    /**
     * Returns whether the pairs can be changed.
     *
     * @return true if the editor is editable
     */
    public boolean isEditable() {
        return editable.get();
    }

    /**
     * Makes the editor editable or read-only.
     *
     * @param value true to make the editor editable
     */
    public void setEditable(boolean value) {
        editable.set(value);
    }

    // --- Rows ----------------------------------------------------------------------------------------

    private void addEntry() {
        KeyValueEntry entry = new KeyValueEntry();
        entries.add(entry);
        rowsByEntry.get(entry).keyField.requestFocus();
    }

    private void deleteEntry(KeyValueEntry entry) {
        int index = indexOf(entry);
        if (index < 0) {
            return;
        }
        entries.remove(index);
        if (index < entries.size()) {
            rowsByEntry.get(entries.get(index)).deleteButton.requestFocus();
        } else if (index > 0) {
            rowsByEntry.get(entries.get(index - 1)).deleteButton.requestFocus();
        } else {
            addButton.requestFocus();
        }
    }

    /** The entry's position by identity: an equal entry elsewhere in the list is another row. */
    private int indexOf(KeyValueEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) == entry) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Brings the rows in line with the entries: an entry that stays keeps its row (its fields keep focus and
     * caret), a new entry gets a new row, and the row of an entry that left is released from it.
     */
    private void syncRows() {
        Map<KeyValueEntry, Row> previous = new IdentityHashMap<>(rowsByEntry);
        rowsByEntry.clear();
        List<Node> rowNodes = new ArrayList<>();
        for (KeyValueEntry entry : entries) {
            Row row = previous.remove(entry);
            if (row == null) {
                row = new Row(entry);
            }
            rowsByEntry.put(entry, row);
            rowNodes.add(row.box);
        }
        for (Row gone : previous.values()) {
            gone.dispose();
        }
        rowBox.getChildren().setAll(rowNodes);
        updateMarks();
    }

    /**
     * Recomputes the marks of every key field. A duplicate depends on the other rows, so any change of a key, a
     * value, an enabled flag, the list or the key options recomputes all marks; the lists are short.
     */
    private void updateMarks() {
        Map<String, Integer> activeKeyCounts = new HashMap<>();
        if (!duplicateKeysAllowed.get()) {
            for (KeyValueEntry entry : entries) {
                if (entry.isEnabled() && (emptyKeyAllowed.get() || !isEmpty(entry.getKey()))) {
                    String key = comparableKey(entry.getKey());
                    activeKeyCounts.put(key, activeKeyCounts.getOrDefault(key, 0) + 1);
                }
            }
        }
        for (KeyValueEntry entry : entries) {
            Row row = rowsByEntry.get(entry);
            if (row == null) {
                // The list changed and the rows follow in syncRows, which marks again.
                continue;
            }
            boolean active = entry.isEnabled();
            boolean emptyKey = isEmpty(entry.getKey());
            boolean missing = active && emptyKey && !isEmpty(entry.getValue()) && !emptyKeyAllowed.get();
            boolean duplicate = active && (!emptyKey || emptyKeyAllowed.get())
                    && activeKeyCounts.getOrDefault(comparableKey(entry.getKey()), 0) > 1;
            setStyleClass(row.keyField, MISSING_KEY_CLASS, missing);
            setStyleClass(row.keyField, DUPLICATE_KEY_CLASS, duplicate);
        }
    }

    private String comparableKey(String key) {
        if (ignoreKeyCase.get()) {
            return key.toLowerCase(Locale.ROOT);
        }
        return key;
    }

    private static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    private static void setStyleClass(Node node, String styleClass, boolean present) {
        if (!present) {
            node.getStyleClass().remove(styleClass);
        } else if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    /**
     * One row's nodes and their ties to its entry. The columns, left to right: CheckBox, key, value, lock,
     * delete. The value column holds the text field or, while the entry is masked, the password field; both are
     * bound to the value, so a swap keeps the text. Every tie to the entry made here is undone in
     * {@link #dispose()}, so a removed entry is free of the editor.
     */
    private final class Row {

        private final KeyValueEntry entry;
        private final HBox box = new HBox();
        private final CheckBox enabledBox = new CheckBox();
        private final TextField keyField = new TextField();
        private final StackPane valueSlot = new StackPane();
        private final TextField plainValueField = new TextField();
        private final PasswordField maskedValueField = new PasswordField();
        private final ToggleButton lockButton = new ToggleButton();
        private final Button deleteButton = new Button();
        private final ChangeListener<Boolean> enabledListener;
        private final ChangeListener<Boolean> maskedListener;
        private final ChangeListener<String> textListener;

        Row(KeyValueEntry entry) {
            this.entry = entry;
            box.getStyleClass().add("key-value-row");
            box.setAlignment(Pos.CENTER_LEFT);

            enabledBox.getStyleClass().add("key-value-enabled");
            enabledBox.selectedProperty().bindBidirectional(entry.enabledProperty());
            enabledBox.setTooltip(boundTooltip(enabledTooltipText));
            enabledBox.accessibleTextProperty().bind(enabledTooltipText);
            enabledBox.disableProperty().bind(editable.not());
            showWith(enabledBox, enabledColumnVisible);

            keyField.getStyleClass().add("key-value-key");
            keyField.textProperty().bindBidirectional(entry.keyProperty());
            keyField.promptTextProperty().bind(keyPromptText);
            keyField.editableProperty().bind(editable);
            HBox.setHgrow(keyField, Priority.ALWAYS);

            setUpValueField(plainValueField);
            setUpValueField(maskedValueField);
            valueSlot.getChildren().setAll(currentValueField());
            HBox.setHgrow(valueSlot, Priority.ALWAYS);

            lockButton.getStyleClass().add("key-value-lock");
            lockButton.selectedProperty().bindBidirectional(entry.maskedProperty());
            lockButton.setTooltip(boundTooltip(lockTooltipText));
            lockButton.accessibleTextProperty().bind(lockTooltipText);
            lockButton.disableProperty().bind(editable.not());
            showWith(lockButton, lockColumnVisible);
            updateLockIcon();

            deleteButton.getStyleClass().addAll("icon-button", "key-value-delete");
            deleteButton.setGraphic(FontIcon.of(BootstrapIcons.X));
            deleteButton.setTooltip(boundTooltip(deleteTooltipText));
            deleteButton.accessibleTextProperty().bind(deleteTooltipText);
            deleteButton.disableProperty().bind(editable.not());
            deleteButton.setOnAction(event -> deleteEntry(entry));

            enabledListener = (observable, wasEnabled, isEnabled) -> {
                updateInactiveClass();
                updateMarks();
            };
            maskedListener = (observable, wasMasked, isMasked) -> maskedChanged(isMasked);
            textListener = (observable, oldText, newText) -> updateMarks();
            entry.enabledProperty().addListener(enabledListener);
            entry.maskedProperty().addListener(maskedListener);
            entry.keyProperty().addListener(textListener);
            entry.valueProperty().addListener(textListener);
            updateInactiveClass();

            box.getChildren().addAll(enabledBox, keyField, valueSlot, lockButton, deleteButton);
        }

        private void setUpValueField(TextField field) {
            field.getStyleClass().add("key-value-value");
            field.textProperty().bindBidirectional(entry.valueProperty());
            field.promptTextProperty().bind(valuePromptText);
            field.editableProperty().bind(editable);
            field.setMaxWidth(Double.MAX_VALUE);
        }

        private TextField currentValueField() {
            if (entry.isMasked()) {
                return maskedValueField;
            }
            return plainValueField;
        }

        /**
         * Swaps the value field and keeps the focus in the value column. Opening the lock without
         * {@code revealAllowed} clears the value before the plain field shows, so it never shows the secret.
         */
        private void maskedChanged(boolean isMasked) {
            TextField previous = isMasked ? plainValueField : maskedValueField;
            boolean hadFocus = previous.isFocused();
            boolean clearing = !isMasked && !revealAllowed.get();
            if (clearing) {
                entry.setValue("");
            }
            TextField current = currentValueField();
            valueSlot.getChildren().setAll(current);
            updateLockIcon();
            if (hadFocus || clearing) {
                current.requestFocus();
            }
        }

        private void updateLockIcon() {
            BootstrapIcons icon = entry.isMasked() ? BootstrapIcons.LOCK : BootstrapIcons.UNLOCK;
            lockButton.setGraphic(FontIcon.of(icon));
        }

        private void updateInactiveClass() {
            setStyleClass(box, INACTIVE_ROW_CLASS, !entry.isEnabled());
        }

        void dispose() {
            enabledBox.selectedProperty().unbindBidirectional(entry.enabledProperty());
            keyField.textProperty().unbindBidirectional(entry.keyProperty());
            plainValueField.textProperty().unbindBidirectional(entry.valueProperty());
            maskedValueField.textProperty().unbindBidirectional(entry.valueProperty());
            lockButton.selectedProperty().unbindBidirectional(entry.maskedProperty());
            entry.enabledProperty().removeListener(enabledListener);
            entry.maskedProperty().removeListener(maskedListener);
            entry.keyProperty().removeListener(textListener);
            entry.valueProperty().removeListener(textListener);
        }
    }

    private static void showWith(Node node, BooleanProperty shown) {
        node.visibleProperty().bind(shown);
        node.managedProperty().bind(shown);
    }

    private static Tooltip boundTooltip(StringProperty text) {
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(text);
        return tooltip;
    }
}
