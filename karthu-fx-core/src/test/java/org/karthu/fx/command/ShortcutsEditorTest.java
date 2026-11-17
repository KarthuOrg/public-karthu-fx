package org.karthu.fx.command;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.CommandTestSupport.themedScene;
import static org.karthu.fx.command.Shortcut.Modifier.ALT;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.assertShortcutColumn;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.button;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.click;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.counter;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.groupRow;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.is;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.keyPressed;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.keyReleased;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.layout;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.line;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.lines;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.list;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.press;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.row;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.select;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.shown;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.text;

/**
 * The shortcut editor through its public API and its documented style classes, in a themed scene that is never
 * shown: the list of groups and commands, recording keys sent to the recording field, removing and resetting keys,
 * the counter, and applying or discarding the working copy.
 */
class ShortcutsEditorTest {

    private static final String PRESS_KEYS = "Press keys… Esc to cancel";

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    // --- The list --------------------------------------------------------------------------------------

    @Test
    @DisplayName("the list shows each group with its name and count, then its commands with their key caps")
    void listShowsGroupsAndCommands() {
        App app = new App();

        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));

        assertEquals(List.of(
                "File 3",
                "  New workflow: [Ctrl] [N]",
                "  Open...: [Ctrl] [O]",
                "  Save: [Ctrl] [S]",
                "Edit 3",
                "  Undo: [Ctrl] [Z]",
                "  Bind to variable: Not assigned",
                "  Copy [Document]: [Ctrl] [C]",
                "View 3",
                "  Zoom in: [Ctrl] [+]",
                "  Run: [F5]",
                "  Run all: [Shift] [F5]"), lines(editor));
        assertTrue(is(row(editor, "Bind to variable"), "not-assigned"));
        assertFalse(is(row(editor, "Save"), "not-assigned"));
        assertTrue(is(row(editor, "Copy"), "fixed"));
        assertFalse(is(row(editor, "Save"), "fixed"));
        assertFalse(editor.hasChanges());
    }

    @Test
    @DisplayName("the header names the columns in capitals, over the columns of the rows")
    void headerNamesTheColumns() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        Label command = (Label) editor.lookup(".shortcuts-header-command");
        Label shortcut = (Label) editor.lookup(".shortcuts-header-shortcut");

        assertEquals("COMMAND", command.getText());
        assertEquals("SHORTCUT", shortcut.getText());
        Bounds title = shortcut.localToScene(shortcut.getLayoutBounds());
        Node column = row(editor, "Save").lookup(".shortcuts-shortcut-column");
        Bounds keys = column.localToScene(column.getLayoutBounds());
        assertTrue(title.getMinX() > 300, "the shortcut column starts right of the names");
        assertEquals(title.getMinX(), keys.getMinX(), 0.5, "the title stands over the key caps");

        editor.setCommandColumnText("Befehl");
        editor.setShortcutColumnText("Tastenkürzel");

        assertEquals("BEFEHL", command.getText());
        assertEquals("TASTENKÜRZEL", shortcut.getText());
    }

    @Test
    @DisplayName("the names of commands, groups and contexts follow their properties")
    void namesFollowTheRegistry() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));

        app.save.setName("Save all");
        app.edit.setName("Bearbeiten");
        app.document.setName("Dokument");

        assertEquals("  Save all: [Ctrl] [S]", line(row(editor, "Save all")));
        assertEquals("Bearbeiten 3", line(groupRow(editor, "Bearbeiten")));
        assertEquals("  Copy [Dokument]: [Ctrl] [C]", line(row(editor, "Copy")));
    }

    @Test
    @DisplayName("a click on a group's line folds it and its rows go, a second click opens it")
    void clickFoldsAGroup() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));

        click(groupRow(editor, "File"), 1);

        assertEquals(List.of("File 3", "Edit 3"), lines(editor).subList(0, 2));
        assertTrue(is(groupRow(editor, "File"), "collapsed"));
        assertFalse(is(groupRow(editor, "Edit"), "collapsed"));

        click(groupRow(editor, "File"), 1);

        assertEquals("  New workflow: [Ctrl] [N]", lines(editor).get(1));
        assertFalse(is(groupRow(editor, "File"), "collapsed"));
    }

    @Test
    @DisplayName("on a group's line Left folds, Right opens, and Enter and Space fold or open")
    void keysFoldAGroup() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        select(editor, "Undo");
        press(editor, keyPressed(KeyCode.UP, "", false, false, false));
        assertTrue(groupRow(editor, "Edit").isSelected());

        press(editor, keyPressed(KeyCode.LEFT, "", false, false, false));
        assertTrue(is(groupRow(editor, "Edit"), "collapsed"));
        assertEquals("View 3", lines(editor).get(5));

        press(editor, keyPressed(KeyCode.RIGHT, "", false, false, false));
        assertFalse(is(groupRow(editor, "Edit"), "collapsed"));

        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));
        assertTrue(is(groupRow(editor, "Edit"), "collapsed"));

        press(editor, keyPressed(KeyCode.SPACE, " ", false, false, false));
        assertFalse(is(groupRow(editor, "Edit"), "collapsed"));
        assertTrue(groupRow(editor, "Edit").isSelected());
    }

    @Test
    @DisplayName("the arrow keys move through the rows")
    void arrowKeysMoveThroughTheRows() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        select(editor, "Open...");

        press(editor, keyPressed(KeyCode.DOWN, "", false, false, false));
        assertTrue(row(editor, "Save").isSelected());

        press(editor, keyPressed(KeyCode.DOWN, "", false, false, false));
        press(editor, keyPressed(KeyCode.DOWN, "", false, false, false));
        assertTrue(row(editor, "Undo").isSelected());
    }

    @Test
    @DisplayName("the pencil and the x show on the selected row, the x only while it has a key")
    void buttonsShowOnTheSelectedRow() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));

        ListCell<?> save = select(editor, "Save");

        assertTrue(button(save, "shortcuts-edit").isVisible());
        assertTrue(button(save, "shortcuts-remove").isVisible());
        assertFalse(button(save, "shortcuts-reset").isVisible());
        ListCell<?> open = row(editor, "Open...");
        assertFalse(button(open, "shortcuts-edit").isVisible());
        assertFalse(button(open, "shortcuts-remove").isVisible());

        ListCell<?> bind = select(editor, "Bind to variable");

        assertTrue(button(bind, "shortcuts-edit").isVisible());
        assertFalse(button(bind, "shortcuts-remove").isVisible());
        assertFalse(button(row(editor, "Save"), "shortcuts-edit").isVisible());
    }

    @Test
    @DisplayName("the row's buttons name what they do in their tooltips and accessible texts, which follow their texts")
    void buttonTexts() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        ListCell<?> save = row(editor, "Save");

        assertEquals("Change shortcut", button(save, "shortcuts-edit").getTooltip().getText());
        assertEquals("Remove shortcut", button(save, "shortcuts-remove").getTooltip().getText());
        assertEquals("Reset to default", button(save, "shortcuts-reset").getTooltip().getText());

        editor.setEditTooltipText("Tastenkürzel ändern");
        editor.setRemoveTooltipText("Tastenkürzel entfernen");
        editor.setResetTooltipText("Zurücksetzen");

        assertEquals("Tastenkürzel ändern", button(save, "shortcuts-edit").getTooltip().getText());
        assertEquals("Tastenkürzel ändern", button(save, "shortcuts-edit").getAccessibleText());
        assertEquals("Tastenkürzel entfernen", button(save, "shortcuts-remove").getAccessibleText());
        assertEquals("Zurücksetzen", button(save, "shortcuts-reset").getAccessibleText());
    }

    @Test
    @DisplayName("a fixed command's row shows its key and has no pencil, x or reset; no key changes it")
    void fixedRowCannotChange() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));

        ListCell<?> copy = select(editor, "Copy");

        assertFalse(button(copy, "shortcuts-edit").isVisible());
        assertFalse(button(copy, "shortcuts-remove").isVisible());
        assertFalse(button(copy, "shortcuts-reset").isVisible());
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));
        press(editor, keyPressed(KeyCode.DELETE, "", false, false, false));
        click(row(editor, "Copy"), 2);
        assertShortcutColumn(editor, "Copy", "[Ctrl] [C]");
        assertFalse(is(row(editor, "Copy"), "recording"));
        assertFalse(editor.hasChanges());
    }

    @Test
    @DisplayName("the texts of the bar show and follow their properties")
    void barTexts() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        SearchInput search = (SearchInput) editor.lookup(".shortcuts-search");
        ToggleButton all = (ToggleButton) editor.lookup(".shortcuts-filter-all");
        ToggleButton modified = (ToggleButton) editor.lookup(".shortcuts-filter-modified");
        ToggleButton conflicts = (ToggleButton) editor.lookup(".shortcuts-filter-conflicts");
        Button resetAll = (Button) editor.lookup(".shortcuts-reset-all");

        assertEquals("Filter by command or key", search.getPromptText());
        assertEquals("All", all.getText());
        assertTrue(all.isSelected());
        assertEquals("Modified", modified.getText());
        assertEquals("Conflicts", conflicts.getText());
        assertEquals("Reset all", resetAll.getText());

        editor.setSearchPromptText("Filtern");
        editor.setAllFilterText("Alle");
        editor.setModifiedFilterText("Geändert");
        editor.setConflictsFilterText("Konflikte");
        editor.setResetAllText("Alle zurücksetzen");

        assertEquals("Filtern", search.getPromptText());
        assertEquals("Alle", all.getText());
        assertEquals("Geändert", modified.getText());
        assertEquals("Konflikte", conflicts.getText());
        assertEquals("Alle zurücksetzen", resetAll.getText());
    }

    @Test
    @DisplayName("Not assigned follows its property")
    void notAssignedText() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));

        editor.setNotAssignedText("Nicht belegt");

        assertShortcutColumn(editor, "Bind to variable", "Nicht belegt");
    }

    // --- Recording -------------------------------------------------------------------------------------

    @Test
    @DisplayName("Enter records: the field shows the held modifiers, and the first valid key becomes the key at once")
    void enterRecordsAKey() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));
        select(editor, "Save");

        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));

        assertTrue(is(row(editor, "Save"), "recording"));
        assertShortcutColumn(editor, "Save", PRESS_KEYS);
        assertFalse(button(row(editor, "Save"), "shortcuts-edit").isVisible());
        assertFalse(button(row(editor, "Save"), "shortcuts-remove").isVisible());
        assertSame(editor.lookup(".shortcuts-recorder"), editor.getScene().getFocusOwner());

        press(editor, keyPressed(KeyCode.CONTROL, "", false, true, false));
        assertShortcutColumn(editor, "Save", "[Ctrl] " + PRESS_KEYS);
        press(editor, keyPressed(KeyCode.SHIFT, "", true, true, false));
        assertShortcutColumn(editor, "Save", "[Ctrl] [Shift] " + PRESS_KEYS);
        press(editor, keyReleased(KeyCode.SHIFT, false, true, false));
        assertShortcutColumn(editor, "Save", "[Ctrl] " + PRESS_KEYS);
        press(editor, keyPressed(KeyCode.SHIFT, "", true, true, false));

        press(editor, keyPressed(KeyCode.K, "k", true, true, false));

        assertFalse(is(row(editor, "Save"), "recording"));
        assertEquals("  Save *: [Ctrl] [Shift] [K]", line(row(editor, "Save")));
        assertTrue(is(row(editor, "Save"), "modified"));
        assertTrue(editor.hasChanges());
        assertEquals(Shortcut.of(KeyCode.S, CTRL), app.save.getShortcut(), "the registry waits for apply");
        assertSame(list(editor), editor.getScene().getFocusOwner());
        assertTrue(row(editor, "Save").isSelected());
    }

    @Test
    @DisplayName("a double-click on a row and its pencil start recording")
    void doubleClickAndPencilRecord() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));

        click(row(editor, "Undo"), 2);

        assertTrue(is(row(editor, "Undo"), "recording"));
        press(editor, keyPressed(KeyCode.F7, "", false, false, false));
        assertShortcutColumn(editor, "Undo", "[F7]");

        ListCell<?> open = select(editor, "Open...");
        button(open, "shortcuts-edit").fire();
        layout(editor);

        assertTrue(is(row(editor, "Open..."), "recording"));
        assertSame(editor.lookup(".shortcuts-recorder"), editor.getScene().getFocusOwner());
        press(editor, keyPressed(KeyCode.L, "l", true, true, true));
        assertShortcutColumn(editor, "Open...", "[Ctrl] [Alt] [Shift] [L]");
    }

    @Test
    @DisplayName("a key the rule refuses shows why in the field, and recording goes on")
    void refusedKeyShowsItsReason() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        select(editor, "Save");
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));
        Node recorder = editor.lookup(".shortcuts-recorder");

        press(editor, keyPressed(KeyCode.A, "A", true, false, false));

        assertShortcutColumn(editor, "Save", "[Shift] Shift+A needs Ctrl or Alt Esc to cancel");
        assertTrue(is(recorder, "refused"));
        assertTrue(is(row(editor, "Save"), "recording"));

        press(editor, keyPressed(KeyCode.TAB, "\t", false, false, false));
        assertEquals("Tab needs Ctrl or Alt", text(recorder, ".shortcuts-recorder-message"));
        for (KeyCode refused : List.of(KeyCode.S, KeyCode.DIGIT5, KeyCode.ENTER, KeyCode.SPACE, KeyCode.BACK_SPACE,
                KeyCode.DELETE, KeyCode.LEFT, KeyCode.F13)) {
            press(editor, keyPressed(refused, "", false, false, false));
            assertTrue(text(recorder, ".shortcuts-recorder-message").endsWith(" needs Ctrl or Alt"),
                    refused + " is refused");
            assertTrue(is(row(editor, "Save"), "recording"), "recording goes on after " + refused);
        }

        press(editor, keyPressed(KeyCode.CONTROL, "", false, true, false));
        assertFalse(is(recorder, "refused"), "a new key clears the reason");
        assertShortcutColumn(editor, "Save", "[Ctrl] " + PRESS_KEYS);

        press(editor, keyPressed(KeyCode.F5, "", false, false, false));
        assertShortcutColumn(editor, "Save", "[F5]");
    }

    @Test
    @DisplayName("a key the platform does not name is refused as no shortcut")
    void unnamedKeyIsRefused() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        select(editor, "Save");
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));

        press(editor, keyPressed(KeyCode.UNDEFINED, "", false, true, false));

        assertEquals("This key cannot be a shortcut",
                text(editor.lookup(".shortcuts-recorder"), ".shortcuts-recorder-message"));
        editor.setUnusableKeyText("Diese Taste geht nicht");
        press(editor, keyPressed(KeyCode.UNDEFINED, "", false, true, false));
        assertEquals("Diese Taste geht nicht",
                text(editor.lookup(".shortcuts-recorder"), ".shortcuts-recorder-message"));
    }

    @Test
    @DisplayName("the field's texts follow their properties")
    void recordingTexts() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        editor.setPressKeysText("Tasten drücken…");
        editor.setEscToCancelText("Esc bricht ab");
        editor.setNeedsCtrlOrAltFormat("{0} braucht Strg oder Alt");
        select(editor, "Save");
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));

        assertShortcutColumn(editor, "Save", "Tasten drücken… Esc bricht ab");

        press(editor, keyPressed(KeyCode.Q, "q", false, false, false));

        assertShortcutColumn(editor, "Save", "Q braucht Strg oder Alt Esc bricht ab");
    }

    @Test
    @DisplayName("Esc ends recording and keeps the old key; the list takes the focus back")
    void escapeCancels() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        select(editor, "Save");
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));
        press(editor, keyPressed(KeyCode.SHIFT, "", true, false, false));

        press(editor, keyPressed(KeyCode.ESCAPE, "\u001B", true, false, false));

        assertFalse(is(row(editor, "Save"), "recording"));
        assertShortcutColumn(editor, "Save", "[Ctrl] [S]");
        assertFalse(editor.hasChanges());
        assertSame(list(editor), editor.getScene().getFocusOwner());

        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));
        assertShortcutColumn(editor, "Save", PRESS_KEYS);
    }

    @Test
    @DisplayName("Ctrl and Esc is a key like any other while recording")
    void controlEscapeIsAKey() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        select(editor, "Save");
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));

        press(editor, keyPressed(KeyCode.ESCAPE, "\u001B", false, true, false));

        assertShortcutColumn(editor, "Save", "[Ctrl] [Escape]");
    }

    @Test
    @DisplayName("the focus leaving the field ends recording and keeps the old key")
    void focusLeavingCancels() {
        App app = new App();
        ShortcutsEditor editor = new ShortcutsEditor(app.registry);
        TextField elsewhere = new TextField();
        VBox root = new VBox(editor, elsewhere);
        Scene scene = new Scene(root, 760, 700);
        ThemeManager.apply(scene);
        layout(root);
        select(editor, "Save");
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));

        elsewhere.requestFocus();
        layout(root);

        assertFalse(is(row(editor, "Save"), "recording"));
        assertShortcutColumn(editor, "Save", "[Ctrl] [S]");
        assertSame(elsewhere, scene.getFocusOwner(), "the focus stays where it went");
    }

    @Test
    @DisplayName("a click elsewhere ends recording and keeps the old key; a click into the field does not")
    void clickElsewhereCancels() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        select(editor, "Save");
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));

        click(editor.lookup(".shortcuts-recorder"), 1);
        layout(editor);
        assertTrue(is(row(editor, "Save"), "recording"));

        click(row(editor, "Undo"), 1);
        layout(editor);

        assertFalse(is(row(editor, "Save"), "recording"));
        assertShortcutColumn(editor, "Save", "[Ctrl] [S]");
        assertTrue(row(editor, "Undo").isSelected());
    }

    @Test
    @DisplayName("punctuation is recorded by its character, from the main keyboard's key and from the keypad")
    void punctuationIsRecordedByItsCharacter() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));

        record(editor, "Save", keyPressed(KeyCode.PLUS, "+", false, true, false));
        assertShortcutColumn(editor, "Save", "[Ctrl] [+]");
        record(editor, "Open...", keyPressed(KeyCode.SUBTRACT, "-", false, true, false));
        assertShortcutColumn(editor, "Open...", "[Ctrl] [-]");
        record(editor, "Undo", keyPressed(KeyCode.COMMA, ",", false, false, true));
        assertShortcutColumn(editor, "Undo", "[Alt] [,]");
        record(editor, "New workflow", keyPressed(KeyCode.DIGIT1, "1", false, true, false));
        assertShortcutColumn(editor, "New workflow", "[Ctrl] [1]");

        editor.apply();

        assertEquals(Shortcut.of('+', CTRL), app.save.getShortcut());
        assertEquals(Shortcut.of('-', CTRL), app.open.getShortcut());
        assertEquals(Shortcut.of(',', ALT), app.undo.getShortcut());
        assertEquals(Shortcut.of(KeyCode.DIGIT1, CTRL), app.newWorkflow.getShortcut());
    }

    @Test
    @DisplayName("Ctrl, Shift and the key that types + with Shift on this layout record Ctrl++")
    void shiftedPunctuationIsRecordedByItsCharacter() {
        KeyCode key = keyTypingWithShift('+');
        Assumptions.assumeTrue(key != null && key != KeyCode.PLUS, "the layout types + without Shift");
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));

        record(editor, "Save", keyPressed(key, "", true, true, false));

        assertShortcutColumn(editor, "Save", "[Ctrl] [+]");
        editor.apply();
        assertEquals(Shortcut.of('+', CTRL), app.save.getShortcut());
    }

    // --- Removing and resetting ------------------------------------------------------------------------

    @Test
    @DisplayName("the x, Delete and Backspace remove a key; the row then says Not assigned and is modified")
    void removeAKey() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));

        button(select(editor, "Save"), "shortcuts-remove").fire();
        select(editor, "Open...");
        press(editor, keyPressed(KeyCode.DELETE, "\u007F", false, false, false));
        select(editor, "New workflow");
        press(editor, keyPressed(KeyCode.BACK_SPACE, "\b", false, false, false));

        assertEquals(List.of(
                "File 3",
                "  New workflow *: Not assigned",
                "  Open... *: Not assigned",
                "  Save *: Not assigned"), lines(editor).subList(0, 4));
        assertTrue(is(row(editor, "Save"), "not-assigned"));
        assertTrue(is(row(editor, "Save"), "modified"));
        assertTrue(editor.hasChanges());
        assertEquals(Shortcut.of(KeyCode.S, CTRL), app.save.getShortcut(), "the registry waits for apply");
    }

    @Test
    @DisplayName("a modified row shows the reset button, which puts the default key back")
    void resetPutsTheDefaultBack() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        assertFalse(button(row(editor, "Save"), "shortcuts-reset").isVisible());
        record(editor, "Save", keyPressed(KeyCode.F9, "", false, false, false));
        select(editor, "Undo");

        Button reset = button(row(editor, "Save"), "shortcuts-reset");
        assertTrue(reset.isVisible(), "also on a row that is not selected");
        reset.fire();
        layout(editor);

        assertEquals("  Save: [Ctrl] [S]", line(row(editor, "Save")));
        assertFalse(is(row(editor, "Save"), "modified"));
        assertFalse(button(row(editor, "Save"), "shortcuts-reset").isVisible());
        assertFalse(editor.hasChanges());
    }

    // --- The counter -----------------------------------------------------------------------------------

    @Test
    @DisplayName("the counter counts the commands, the modified ones, the conflicts and the ones without a key")
    void counterCounts() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));

        assertEquals("9 commands · 0 modified · 0 conflicts · 1 not assigned", counter(editor));

        button(select(editor, "Save"), "shortcuts-remove").fire();
        record(editor, "Bind to variable", keyPressed(KeyCode.B, "b", false, true, false));

        assertEquals("9 commands · 2 modified · 0 conflicts · 1 not assigned", counter(editor));
    }

    @Test
    @DisplayName("the counter's parts take their formats, the one forms for a count of one")
    void counterFormats() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));
        button(select(editor, "Save"), "shortcuts-remove").fire();
        assertEquals("9 commands · 1 modified · 0 conflicts · 2 not assigned", counter(editor));

        editor.setCommandCountFormat("{0} Befehle");
        editor.setModifiedCountOneFormat("{0} geändert");
        editor.setConflictCountFormat("{0} Konflikte");
        editor.setNotAssignedCountFormat("{0} ohne Taste");

        assertEquals("9 Befehle · 1 geändert · 0 Konflikte · 2 ohne Taste", counter(editor));

        CommandRegistry small = new CommandRegistry(themedScene(new StackPane()));
        small.addCommand(small.addGroup("file", "File"), "file.save", "Save", null, () -> { });
        ShortcutsEditor one = shown(new ShortcutsEditor(small));
        one.setCommandCountOneFormat("{0} Befehl");
        one.setNotAssignedCountOneFormat("{0} ohne Taste");
        one.setConflictCountFormat(null);

        assertEquals("1 Befehl · 0 modified ·  · 1 ohne Taste", counter(one));
    }

    // --- The working copy ------------------------------------------------------------------------------

    @Test
    @DisplayName("apply hands the working copy to the registry and leaves no changes")
    void applyHandsTheKeysToTheRegistry() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));
        record(editor, "Save", keyPressed(KeyCode.K, "k", true, true, false));
        button(select(editor, "Open..."), "shortcuts-remove").fire();

        editor.apply();

        assertEquals(Shortcut.of(KeyCode.K, CTRL, SHIFT), app.save.getShortcut());
        assertNull(app.open.getShortcut());
        assertEquals(Shortcut.of(KeyCode.N, CTRL), app.newWorkflow.getShortcut());
        assertFalse(editor.hasChanges());
        assertEquals("  Save *: [Ctrl] [Shift] [K]", line(row(editor, "Save")), "still modified: not the default");
    }

    @Test
    @DisplayName("has changes follows the working copy: true after a change, false once it is undone")
    void hasChangesFollowsTheWorkingCopy() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        List<Boolean> seen = new ArrayList<>();
        editor.hasChangesProperty().addListener((observable, before, now) -> seen.add(now));

        button(select(editor, "Save"), "shortcuts-remove").fire();
        button(row(editor, "Save"), "shortcuts-reset").fire();

        assertEquals(List.of(true, false), seen);
    }

    @Test
    @DisplayName("discard drops the working copy and reads the registry again, its commands too")
    void discardReadsTheRegistryAgain() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));
        record(editor, "Save", keyPressed(KeyCode.K, "k", true, true, false));
        click(groupRow(editor, "View"), 1);
        app.registry.addCommand(app.view, "view.reset", "Reset zoom", Shortcut.of(KeyCode.DIGIT0, CTRL), () -> { });

        editor.discard();

        assertFalse(editor.hasChanges());
        assertEquals("  Save: [Ctrl] [S]", line(row(editor, "Save")));
        assertEquals(Shortcut.of(KeyCode.S, CTRL), app.save.getShortcut());
        assertEquals("View 4", line(groupRow(editor, "View")));
        assertFalse(is(groupRow(editor, "View"), "collapsed"), "groups open again");
        assertEquals("  Reset zoom: [Ctrl] [0]", line(row(editor, "Reset zoom")));
    }

    @Test
    @DisplayName("a command whose key differs from its default shows as modified when the editor opens")
    void modifiedMeansDiffersFromTheDefault() {
        App app = new App();
        ShortcutsEditor earlier = shown(new ShortcutsEditor(app.registry));
        record(earlier, "Run", keyPressed(KeyCode.F6, "", false, false, false));
        earlier.apply();

        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));

        assertEquals("  Run *: [F6]", line(row(editor, "Run")));
        assertFalse(editor.hasChanges());
        assertEquals("9 commands · 1 modified · 0 conflicts · 1 not assigned", counter(editor));
    }

    @Test
    @DisplayName("a key the registry changes reaches the rows the user did not change")
    void registryChangesReachUntouchedRows() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));
        record(editor, "Open...", keyPressed(KeyCode.F8, "", false, false, false));
        ShortcutsEditor other = shown(new ShortcutsEditor(app.registry));
        record(other, "Save", keyPressed(KeyCode.F2, "", false, false, false));
        record(other, "Open...", keyPressed(KeyCode.F3, "", false, false, false));

        other.apply();

        assertShortcutColumn(editor, "Save", "[F2]");
        assertShortcutColumn(editor, "Open...", "[F8]");
        assertTrue(editor.hasChanges());
    }

    // --- Helpers ---------------------------------------------------------------------------------------

    /**
     * Records a key for a command: selects its row, presses Enter and then the key.
     */
    private static void record(ShortcutsEditor editor, String name, KeyEvent key) {
        select(editor, name);
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));
        assertTrue(is(row(editor, name), "recording"), name + " records");
        press(editor, key);
    }

    /**
     * The key of the main keyboard that types a character with Shift on this machine's keyboard layout, as JavaFX
     * finds it; null if the toolkit knows none.
     */
    private static KeyCode keyTypingWithShift(char character) {
        KeyCharacterCombination combination = new KeyCharacterCombination(String.valueOf(character),
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
        for (KeyCode code : KeyCode.values()) {
            boolean keypad = code.isKeypadKey() || code == KeyCode.ADD || code == KeyCode.SUBTRACT
                    || code == KeyCode.MULTIPLY || code == KeyCode.DIVIDE || code == KeyCode.DECIMAL
                    || code == KeyCode.SEPARATOR;
            if (keypad || code == KeyCode.UNDEFINED) {
                continue;
            }
            if (combination.match(keyPressed(code, "", true, true, false))) {
                return code;
            }
        }
        return null;
    }

    /**
     * An application with commands in three groups: some with a key, one without, a character key, a function key
     * and a fixed command of a document area.
     */
    static final class App {

        final StackPane documentArea = new StackPane(new Label("text"));

        final CommandRegistry registry = new CommandRegistry(themedScene(new VBox(documentArea)));

        final CommandContext document = registry.addContext("document", "Document", documentArea);

        final CommandGroup file = registry.addGroup("file", "File");

        final CommandGroup edit = registry.addGroup("edit", "Edit");

        final CommandGroup view = registry.addGroup("view", "View");

        final Command newWorkflow = registry.addCommand(file, "file.new", "New workflow",
                Shortcut.of(KeyCode.N, CTRL), () -> { });

        final Command open = registry.addCommand(file, "file.open", "Open...", Shortcut.of(KeyCode.O, CTRL),
                () -> { });

        final Command save = registry.addCommand(file, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), () -> { });

        final Command undo = registry.addCommand(edit, "edit.undo", "Undo", Shortcut.of(KeyCode.Z, CTRL), () -> { });

        final Command bind = registry.addCommand(edit, "edit.bind", "Bind to variable", null, () -> { });

        final Command copy = registry.addFixedCommand(edit, document, "edit.copy", "Copy",
                Shortcut.of(KeyCode.C, CTRL), null);

        final Command zoomIn = registry.addCommand(view, "view.zoomIn", "Zoom in", Shortcut.of('+', CTRL), () -> { });

        final Command run = registry.addCommand(view, "view.run", "Run", Shortcut.of(KeyCode.F5), () -> { });

        final Command runAll = registry.addCommand(view, "view.runAll", "Run all", Shortcut.of(KeyCode.F5, SHIFT),
                () -> { });
    }
}
