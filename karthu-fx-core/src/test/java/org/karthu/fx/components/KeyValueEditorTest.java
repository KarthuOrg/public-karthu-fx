package org.karthu.fx.components;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyValueEditorTest {

    private KeyValueEditor editor;

    @BeforeAll
    static void initToolkit() {
        // Controls cannot be instantiated without the JavaFX toolkit. No Stage is shown, so the
        // controls may live on the test thread.
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
    }

    @BeforeEach
    void createEditor() {
        editor = new KeyValueEditor();
    }

    @Test
    @DisplayName("entries filled from outside show as rows with their key and value")
    void entriesShowAsRows() {
        editor.getEntries().addAll(
                new KeyValueEntry(true, "key1", "value1", false),
                new KeyValueEntry(false, "key2", "value2", false));

        List<Node> rows = rows();
        assertEquals(2, rows.size());
        assertEquals("key1", keyField(rows.get(0)).getText());
        assertEquals("value1", valueField(rows.get(0)).getText());
        assertEquals("key2", keyField(rows.get(1)).getText());
        assertEquals("value2", valueField(rows.get(1)).getText());
    }

    @Test
    @DisplayName("the snapshot holds every row's four values in order and leaves fully empty rows out")
    void snapshotLeavesOutEmptyRows() {
        editor.getEntries().addAll(
                new KeyValueEntry(true, "key1", "value1", false),
                new KeyValueEntry(true, "", "", false),
                new KeyValueEntry(false, "", "only a value", true),
                new KeyValueEntry(false, null, null, true),
                new KeyValueEntry(true, "only a key", "", false));

        List<KeyValueEntry.Snapshot> snapshot = editor.snapshot();

        assertEquals(List.of(
                new KeyValueEntry.Snapshot(true, "key1", "value1", false),
                new KeyValueEntry.Snapshot(false, "", "only a value", true),
                new KeyValueEntry.Snapshot(true, "only a key", "", false)), snapshot);
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new KeyValueEntry.Snapshot(true, "k", "v", false)));
    }

    @Test
    @DisplayName("typing into a row's fields writes through to its entry")
    void typingWritesToEntry() {
        KeyValueEntry entry = new KeyValueEntry("a", "b");
        editor.getEntries().add(entry);

        keyField(rows().get(0)).setText("Accept");
        valueField(rows().get(0)).setText("text/plain");

        assertEquals("Accept", entry.getKey());
        assertEquals("text/plain", entry.getValue());
    }

    @Test
    @DisplayName("add appends an active, unmasked, empty row and focuses its key field")
    void addAppendsEmptyRowAndFocusesKey() {
        Scene scene = new Scene(editor);
        editor.getEntries().add(new KeyValueEntry(false, "key1", "value1", true));

        addButton().fire();

        assertEquals(2, editor.getEntries().size());
        KeyValueEntry added = editor.getEntries().get(1);
        assertTrue(added.isEnabled());
        assertFalse(added.isMasked());
        assertEquals("", added.getKey());
        assertEquals("", added.getValue());
        assertSame(keyField(rows().get(1)), scene.getFocusOwner());
    }

    @Test
    @DisplayName("delete removes its row at once and focuses the next row's delete button")
    void deleteFocusesNextRow() {
        Scene scene = new Scene(editor);
        KeyValueEntry first = new KeyValueEntry("a", "1");
        KeyValueEntry second = new KeyValueEntry("b", "2");
        KeyValueEntry third = new KeyValueEntry("c", "3");
        editor.getEntries().addAll(first, second, third);

        deleteButton(rows().get(1)).fire();

        assertEquals(List.of(first, third), editor.getEntries());
        assertEquals(2, rows().size());
        assertSame(deleteButton(rows().get(1)), scene.getFocusOwner());
    }

    @Test
    @DisplayName("deleting the last row focuses the previous row, deleting the only row the add button")
    void deleteFocusFallsBackToPreviousThenAddButton() {
        Scene scene = new Scene(editor);
        editor.getEntries().addAll(new KeyValueEntry("a", "1"), new KeyValueEntry("b", "2"));

        deleteButton(rows().get(1)).fire();
        assertSame(deleteButton(rows().get(0)), scene.getFocusOwner());

        deleteButton(rows().get(0)).fire();
        assertTrue(editor.getEntries().isEmpty());
        assertTrue(rows().isEmpty());
        assertSame(addButton(), scene.getFocusOwner());
    }

    @Test
    @DisplayName("an entry removed from outside loses its row, and its fields no longer write to it")
    void removedEntryIsReleased() {
        KeyValueEntry entry = new KeyValueEntry("a", "1");
        editor.getEntries().add(entry);
        TextField oldKeyField = keyField(rows().get(0));

        editor.getEntries().clear();
        oldKeyField.setText("changed");

        assertTrue(rows().isEmpty());
        assertEquals("a", entry.getKey());
    }

    @Test
    @DisplayName("the title shows its text, and an empty or null title hides the title row")
    void emptyTitleHidesTitleRow() {
        assertEquals("Entries", title().getText());
        assertTrue(title().isVisible());
        assertTrue(title().isManaged());

        editor.setTitle("Request Header");
        assertEquals("Request Header", title().getText());

        editor.setTitle("");
        assertFalse(title().isVisible());
        assertFalse(title().isManaged());

        editor.setTitle(null);
        assertFalse(title().isVisible());

        editor.setTitle("Query");
        assertTrue(title().isVisible());
    }

    @Test
    @DisplayName("the add button's text has an English default and follows its property")
    void addButtonText() {
        assertEquals("Add", addButton().getText());

        editor.setAddText("Add header");

        assertEquals("Add header", addButton().getText());
    }

    @Test
    @DisplayName("placeholders are empty by default and apply to every row, also rows added later")
    void placeholders() {
        editor.getEntries().add(new KeyValueEntry("a", "1"));
        assertEquals("", keyField(rows().get(0)).getPromptText());
        assertEquals("", valueField(rows().get(0)).getPromptText());

        editor.setKeyPromptText("Name");
        editor.setValuePromptText("Value");
        editor.getEntries().add(new KeyValueEntry());

        for (Node row : rows()) {
            assertEquals("Name", keyField(row).getPromptText());
            assertEquals("Value", valueField(row).getPromptText());
        }
    }

    @Test
    @DisplayName("tooltips have English defaults and follow their properties")
    void tooltips() {
        editor.getEntries().add(new KeyValueEntry("a", "1"));
        Node row = rows().get(0);
        assertEquals("Use this entry", enabledBox(row).getTooltip().getText());
        assertEquals("Remove this entry", deleteButton(row).getTooltip().getText());
        assertEquals("Mask the value", editor.getLockTooltipText());

        editor.setEnabledTooltipText("Aktiv");
        editor.setDeleteTooltipText("Entfernen");

        assertEquals("Aktiv", enabledBox(row).getTooltip().getText());
        assertEquals("Entfernen", deleteButton(row).getTooltip().getText());
    }

    @Test
    @DisplayName("the CheckBox switches the entry, and an inactive row carries the dimming class but stays editable")
    void inactiveRowIsMarkedAndEditable() {
        KeyValueEntry entry = new KeyValueEntry(false, "a", "1", false);
        editor.getEntries().add(entry);
        Node row = rows().get(0);
        assertFalse(enabledBox(row).isSelected());
        assertTrue(row.getStyleClass().contains("key-value-row-inactive"));
        assertTrue(keyField(row).isEditable());
        assertFalse(keyField(row).isDisabled());
        assertTrue(valueField(row).isEditable());
        assertFalse(valueField(row).isDisabled());

        enabledBox(row).setSelected(true);

        assertTrue(entry.isEnabled());
        assertFalse(row.getStyleClass().contains("key-value-row-inactive"));

        entry.setEnabled(false);

        assertFalse(enabledBox(row).isSelected());
        assertTrue(row.getStyleClass().contains("key-value-row-inactive"));
    }

    @Test
    @DisplayName("the masked flag is kept on the entry and in the snapshot")
    void maskedIsKept() {
        KeyValueEntry entry = new KeyValueEntry("token", "secret");
        editor.getEntries().add(entry);

        entry.setMasked(true);

        assertEquals(List.of(new KeyValueEntry.Snapshot(true, "token", "secret", true)), editor.snapshot());
    }

    // --- Lock, key marking, options --------------------------------------------------------------

    @Test
    @DisplayName("closing the lock masks the entry and shows the value in a password field with the same text")
    void lockMasksValue() {
        KeyValueEntry entry = new KeyValueEntry("token", "s3cret");
        editor.getEntries().add(entry);
        Node row = rows().get(0);
        assertFalse(lockButton(row).isSelected());
        assertFalse(valueField(row) instanceof PasswordField);

        lockButton(row).fire();

        assertTrue(entry.isMasked());
        assertTrue(lockButton(row).isSelected());
        assertInstanceOf(PasswordField.class, valueField(row));
        assertEquals("s3cret", valueField(row).getText());

        valueField(row).setText("other");
        assertEquals("other", entry.getValue());
    }

    @Test
    @DisplayName("an entry masked from outside shows locked, and opening the lock reveals the value by default")
    void openingRevealsByDefault() {
        assertTrue(editor.isRevealAllowed());
        KeyValueEntry entry = new KeyValueEntry(true, "token", "s3cret", true);
        editor.getEntries().add(entry);
        Node row = rows().get(0);
        assertTrue(lockButton(row).isSelected());
        assertInstanceOf(PasswordField.class, valueField(row));

        lockButton(row).fire();

        assertFalse(entry.isMasked());
        assertFalse(valueField(row) instanceof PasswordField);
        assertEquals("s3cret", valueField(row).getText());
    }

    @Test
    @DisplayName("without revealAllowed, opening the lock clears the value and focuses the value field")
    void openingClearsWhenRevealNotAllowed() {
        Scene scene = new Scene(editor);
        editor.setRevealAllowed(false);
        KeyValueEntry entry = new KeyValueEntry(true, "token", "s3cret", true);
        editor.getEntries().add(entry);
        Node row = rows().get(0);

        lockButton(row).fire();

        assertFalse(entry.isMasked());
        assertEquals("", entry.getValue());
        assertEquals("", valueField(row).getText());
        assertSame(valueField(row), scene.getFocusOwner());
    }

    @Test
    @DisplayName("the lock and CheckBox columns are visible by default and can be hidden, also in rows added later")
    void columnsCanBeHidden() {
        editor.getEntries().add(new KeyValueEntry("a", "1"));
        assertTrue(editor.isLockColumnVisible());
        assertTrue(editor.isEnabledColumnVisible());
        assertShown(lockButton(rows().get(0)), true);
        assertShown(enabledBox(rows().get(0)), true);

        editor.setLockColumnVisible(false);
        editor.setEnabledColumnVisible(false);
        editor.getEntries().add(new KeyValueEntry("b", "2"));

        for (Node row : rows()) {
            assertShown(lockButton(row), false);
            assertShown(enabledBox(row), false);
        }

        editor.setLockColumnVisible(true);
        assertShown(lockButton(rows().get(1)), true);
    }

    @Test
    @DisplayName("duplicate keys are allowed by default and stay unmarked")
    void duplicatesAllowedByDefault() {
        assertTrue(editor.isDuplicateKeysAllowed());
        assertFalse(editor.isIgnoreKeyCase());
        editor.getEntries().addAll(new KeyValueEntry("Accept", "1"), new KeyValueEntry("Accept", "2"));

        for (Node row : rows()) {
            assertFalse(isDuplicate(row));
        }
    }

    @Test
    @DisplayName("forbidden duplicates among active rows are marked on every key field involved")
    void forbiddenDuplicatesAreMarked() {
        editor.setDuplicateKeysAllowed(false);
        KeyValueEntry third = new KeyValueEntry("Accept", "3");
        editor.getEntries().addAll(
                new KeyValueEntry("Accept", "1"),
                new KeyValueEntry("accept", "2"),
                third,
                new KeyValueEntry(false, "Accept", "4", false),
                new KeyValueEntry("", "x"),
                new KeyValueEntry("", "y"));
        List<Node> rows = rows();

        assertTrue(isDuplicate(rows.get(0)));
        assertFalse(isDuplicate(rows.get(1)), "case counts unless ignoreKeyCase");
        assertTrue(isDuplicate(rows.get(2)));
        assertFalse(isDuplicate(rows.get(3)), "an inactive row is no duplicate");
        assertFalse(isDuplicate(rows.get(4)), "empty keys are missing, not duplicate");
        assertFalse(isDuplicate(rows.get(5)));

        editor.setIgnoreKeyCase(true);
        assertTrue(isDuplicate(rows.get(1)));

        third.setKey("Content-Type");
        assertTrue(isDuplicate(rows.get(0)));
        assertFalse(isDuplicate(rows.get(2)));

        editor.getEntries().remove(1);
        assertFalse(isDuplicate(rows().get(0)), "the mark follows removals");

        editor.getEntries().get(0).setEnabled(false);
        editor.getEntries().get(2).setEnabled(true);
        assertFalse(isDuplicate(rows().get(0)));
        assertFalse(isDuplicate(rows().get(2)));

        editor.getEntries().get(0).setEnabled(true);
        assertTrue(isDuplicate(rows().get(0)), "the mark follows the enabled flag");
        assertTrue(isDuplicate(rows().get(2)));

        editor.setDuplicateKeysAllowed(true);
        assertFalse(isDuplicate(rows().get(0)));
    }

    @Test
    @DisplayName("an active row with a value but no key is marked; an inactive or fully empty row is not")
    void missingKeyIsMarked() {
        KeyValueEntry active = new KeyValueEntry("", "value");
        editor.getEntries().addAll(
                active,
                new KeyValueEntry(false, "", "value", false),
                new KeyValueEntry("", ""));
        List<Node> rows = rows();

        assertTrue(isMissing(rows.get(0)));
        assertFalse(isMissing(rows.get(1)));
        assertFalse(isMissing(rows.get(2)));

        keyField(rows.get(0)).setText("Accept");
        assertFalse(isMissing(rows.get(0)));

        active.setKey("");
        assertTrue(isMissing(rows.get(0)));
        active.setEnabled(false);
        assertFalse(isMissing(rows.get(0)));
    }

    @Test
    @DisplayName("with empty keys allowed, an empty key is a key like any other: never missing, a duplicate when twice")
    void emptyKeyAllowed() {
        assertFalse(editor.isEmptyKeyAllowed());
        editor.setDuplicateKeysAllowed(false);
        editor.getEntries().addAll(
                new KeyValueEntry("", "x"),
                new KeyValueEntry("", "y"),
                new KeyValueEntry("Accept", "1"));
        List<Node> rows = rows();
        assertTrue(isMissing(rows.get(0)));
        assertFalse(isDuplicate(rows.get(0)));

        editor.setEmptyKeyAllowed(true);

        assertFalse(isMissing(rows.get(0)));
        assertFalse(isMissing(rows.get(1)));
        assertTrue(isDuplicate(rows.get(0)), "two empty keys are one key twice");
        assertTrue(isDuplicate(rows.get(1)));
        assertFalse(isDuplicate(rows.get(2)));

        editor.getEntries().remove(1);
        assertFalse(isDuplicate(rows().get(0)), "one empty key is no duplicate");

        editor.setEmptyKeyAllowed(false);
        assertTrue(isMissing(rows().get(0)));
    }

    @Test
    @DisplayName("a non-editable editor locks every field and button, and editable restores them")
    void editableLocksEverything() {
        assertTrue(editor.isEditable());
        editor.getEntries().add(new KeyValueEntry("a", "1"));

        editor.setEditable(false);
        editor.getEntries().add(new KeyValueEntry(true, "b", "2", true));

        for (Node row : rows()) {
            assertFalse(keyField(row).isEditable());
            assertFalse(valueField(row).isEditable());
            assertTrue(enabledBox(row).isDisabled());
            assertTrue(lockButton(row).isDisabled());
            assertTrue(deleteButton(row).isDisabled());
        }
        assertTrue(addButton().isDisabled());

        editor.setEditable(true);

        for (Node row : rows()) {
            assertTrue(keyField(row).isEditable());
            assertTrue(valueField(row).isEditable());
            assertFalse(enabledBox(row).isDisabled());
            assertFalse(lockButton(row).isDisabled());
            assertFalse(deleteButton(row).isDisabled());
        }
        assertFalse(addButton().isDisabled());
    }

    // --- helpers: the style classes are the control's public styling contract ------------------

    private List<Node> rows() {
        return new ArrayList<>(editor.lookupAll(".key-value-row"));
    }

    private static TextField keyField(Node row) {
        return (TextField) row.lookup(".key-value-key");
    }

    private static TextField valueField(Node row) {
        return (TextField) row.lookup(".key-value-value");
    }

    private static Button deleteButton(Node row) {
        return (Button) row.lookup(".key-value-delete");
    }

    private static CheckBox enabledBox(Node row) {
        return (CheckBox) row.lookup(".key-value-enabled");
    }

    private static ToggleButton lockButton(Node row) {
        return (ToggleButton) row.lookup(".key-value-lock");
    }

    private static boolean isDuplicate(Node row) {
        return keyField(row).getStyleClass().contains("key-value-key-duplicate");
    }

    private static boolean isMissing(Node row) {
        return keyField(row).getStyleClass().contains("key-value-key-missing");
    }

    private static void assertShown(Node node, boolean shown) {
        assertEquals(shown, node.isVisible());
        assertEquals(shown, node.isManaged());
    }

    private Button addButton() {
        return (Button) editor.lookup(".key-value-add");
    }

    private Label title() {
        return (Label) editor.lookup(".key-value-title");
    }
}
