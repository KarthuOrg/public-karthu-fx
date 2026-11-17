package org.karthu.fx.command;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.ShortcutsEditorTest.App;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.theme.ThemeManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.CommandTestSupport.onFxThread;
import static org.karthu.fx.command.CommandTestSupport.themedScene;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.button;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.click;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.counter;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.groupRow;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.is;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.keyPressed;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.layout;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.lines;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.press;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.row;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.select;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.shown;

/**
 * Finding a command in the shortcut editor, through its public API and its documented style classes, in a themed scene
 * that is never shown: the search field, the filters <i>All</i>, <i>Modified</i> and <i>Conflicts</i>, and
 * <i>Reset all</i>.
 */
class ShortcutsSearchTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    // --- The search ------------------------------------------------------------------------------------

    @Test
    @DisplayName("the search keeps the rows whose name matches, and the groups without a match go")
    void searchKeepsMatchingNames() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));

        search(editor, "save");

        assertEquals(List.of("File 1", "  Save: [Ctrl] [S]"), lines(editor));
    }

    @Test
    @DisplayName("the search ignores case and accents, as the search inputs do")
    void searchIgnoresCaseAndAccents() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));

        search(editor, "RESUME");

        assertEquals(List.of("Edit 1", "  Insert r\u00E9sum\u00E9: Not assigned"), lines(editor));

        search(editor, "r\u00C9sum\u00C9");

        assertEquals(List.of("Edit 1", "  Insert r\u00E9sum\u00E9: Not assigned"), lines(editor));
    }

    @Test
    @DisplayName("the search matches a command's group: every row of the group shows")
    void searchMatchesTheGroup() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));

        search(editor, "file");

        assertEquals(List.of(
                "File 3",
                "  Open: [Ctrl] [O]",
                "  Save: [Ctrl] [S]",
                "  Close tab: [Ctrl] [W]"), lines(editor));
    }

    @Test
    @DisplayName("the search matches the context a row's mark names; the window's own commands carry none")
    void searchMatchesTheContextMark() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));

        search(editor, "document");

        assertEquals(List.of(
                "Edit 2",
                "  Copy [Document]: [Ctrl] [C]",
                "  Find [Document]: [Ctrl] [F]"), lines(editor));

        search(editor, "window");

        assertEquals(List.of(
                "View 1",
                "  Fit to window: [Ctrl] [0] Also used by Actual size <Remove there>"), lines(editor),
                "the window's context is named in no row");
    }

    @Test
    @DisplayName("the search matches a key's display text: ctrl+0 finds both rows of the conflict on Ctrl+0")
    void searchMatchesTheKey() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));

        search(editor, "ctrl+0");

        assertEquals(List.of(
                "View 2",
                "  Actual size: [Ctrl] [0] Also used by Fit to window <Remove there>",
                "  Fit to window: [Ctrl] [0] Also used by Actual size <Remove there>"), lines(editor));

        search(editor, "F5");

        assertEquals(List.of("View 1", "  Run: [F5]"), lines(editor));

        search(editor, "ctrl++");

        assertEquals(List.of("View 1", "  Zoom in: [Ctrl] [+]"), lines(editor));
    }

    @Test
    @DisplayName("the search matches the key of the working copy, not the registry's")
    void searchMatchesTheWorkingCopysKey() {
        Workbench workbench = new Workbench();
        ShortcutsEditor editor = shown(new ShortcutsEditor(workbench.registry));
        record(editor, "Undo", keyPressed(KeyCode.F9, "", false, false, false));

        search(editor, "f9");

        assertEquals(List.of("Edit 1", "  Undo *: [F9]"), lines(editor));

        search(editor, "ctrl+z");

        assertEquals(List.of(), lines(editor));
    }

    @Test
    @DisplayName("every word of the search has to occur, each in the name, the group, the context or the key")
    void everyWordHasToOccur() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));

        search(editor, "  edit   docu  f ");

        assertEquals(List.of("Edit 1", "  Find [Document]: [Ctrl] [F]"), lines(editor));
    }

    @Test
    @DisplayName("a search without a match leaves the list empty, which says so")
    void searchWithoutMatchSaysSo() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));
        assertEquals(null, noMatch(editor), "nothing to say while rows show");

        search(editor, "nothing like this");

        assertEquals(List.of(), lines(editor));
        assertEquals("No matching commands", noMatch(editor));
        assertEquals("No matching commands", editor.getNoMatchText());

        editor.setNoMatchText("Keine passenden Befehle");
        layout(editor);

        assertEquals("Keine passenden Befehle", noMatch(editor));
        assertEquals("Keine passenden Befehle", editor.noMatchTextProperty().get());

        search(editor, "save");

        assertEquals(null, noMatch(editor));
    }

    @Test
    @DisplayName("the theme draws the empty list's text muted, as the other lists' placeholders")
    void noMatchTextIsMuted() {
        ShortcutsEditor editor = new ShortcutsEditor(new Workbench().registry);
        Region muted = new Region();
        muted.setStyle("-fx-background-color: -fx-text-secondary-color;");
        VBox root = new VBox(editor, muted);
        Scene scene = new Scene(root, 760, 700);
        ThemeManager.apply(scene);
        layout(root);

        search(editor, "nothing like this");
        layout(root);

        Label noMatch = (Label) editor.lookup(".shortcuts-no-match");
        assertEquals(muted.getBackground().getFills().get(0).getFill(), noMatch.getTextFill());
    }

    @Test
    @DisplayName("an editor without commands says nothing about matches")
    void emptyEditorSaysNothing() {
        CommandRegistry registry = new CommandRegistry(themedScene(new StackPane()));
        registry.addGroup("file", "File");

        ShortcutsEditor editor = shown(new ShortcutsEditor(registry));

        assertEquals(List.of(), lines(editor));
        assertEquals(null, noMatch(editor), "no text about a search that did not run");
    }

    // --- Folding while searching ------------------------------------------------------------------------

    @Test
    @DisplayName("while searching, folded groups with matches are open; clearing the search folds them again")
    void searchOpensFoldedGroupsWithMatches() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));
        click(groupRow(editor, "File"), 1);
        click(groupRow(editor, "Edit"), 1);

        search(editor, "o");

        assertEquals(List.of(
                "File 2",
                "  Open: [Ctrl] [O]",
                "  Close tab: [Ctrl] [W]",
                "Edit 3",
                "  Undo: [Ctrl] [Z]",
                "  Copy [Document]: [Ctrl] [C]",
                "  Find [Document]: [Ctrl] [F]",
                "View 2",
                "  Zoom in: [Ctrl] [+]",
                "  Fit to window: [Ctrl] [0] Also used by Actual size <Remove there>"), lines(editor));
        assertFalse(is(groupRow(editor, "File"), "collapsed"));

        search(editor, "");

        assertEquals(List.of("File 3", "Edit 4", "View 4"), lines(editor).subList(0, 3));
        assertTrue(is(groupRow(editor, "File"), "collapsed"));
        assertTrue(is(groupRow(editor, "Edit"), "collapsed"));
        assertFalse(is(groupRow(editor, "View"), "collapsed"));
    }

    @Test
    @DisplayName("a group folded while searching stays folded until the search changes, and the folding before it returns")
    void foldingWhileSearchingLastsForTheSearch() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));
        click(groupRow(editor, "File"), 1);
        search(editor, "o");

        click(groupRow(editor, "View"), 1);
        select(editor, "Undo");
        press(editor, keyPressed(KeyCode.UP, "", false, false, false));
        press(editor, keyPressed(KeyCode.LEFT, "", false, false, false));

        assertEquals(List.of("File 2", "  Open: [Ctrl] [O]", "  Close tab: [Ctrl] [W]", "Edit 3", "View 2"),
                lines(editor));
        assertTrue(is(groupRow(editor, "Edit"), "collapsed"));
        assertTrue(is(groupRow(editor, "View"), "collapsed"));

        search(editor, "o ");

        assertEquals(10, lines(editor).size(), "a new search opens every group with a match");

        click(groupRow(editor, "View"), 1);
        search(editor, "");

        assertEquals(List.of("File 3", "Edit 4", "  Undo: [Ctrl] [Z]"), lines(editor).subList(0, 3));
        assertTrue(is(groupRow(editor, "File"), "collapsed"), "folded before the search");
        assertFalse(is(groupRow(editor, "View"), "collapsed"), "open before the search");
    }

    // --- The filters ----------------------------------------------------------------------------------

    @Test
    @DisplayName("Modified shows the rows whose keys differ from their defaults, also keys set in an earlier session")
    void modifiedFilter() {
        Workbench workbench = new Workbench();
        workbench.registry.setChangedShortcuts(Map.of("view.run", "F6"));
        ShortcutsEditor editor = shown(new ShortcutsEditor(workbench.registry));
        button(select(editor, "Save"), "shortcuts-remove").fire();

        filter(editor, "shortcuts-filter-modified");

        assertEquals(List.of(
                "File 1",
                "  Save *: Not assigned",
                "View 1",
                "  Run *: [F6]"), lines(editor));
        assertTrue(filterButton(editor, "shortcuts-filter-modified").isSelected());
        assertFalse(filterButton(editor, "shortcuts-filter-all").isSelected());
    }

    @Test
    @DisplayName("Conflicts shows the rows in a conflict, and All every row again")
    void conflictsAndAllFilters() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));
        record(editor, "Close tab", keyPressed(KeyCode.O, "o", false, true, false));

        filter(editor, "shortcuts-filter-conflicts");

        assertEquals(List.of(
                "File 2",
                "  Open: [Ctrl] [O] Also used by Close tab <Remove there>",
                "  Close tab *: [Ctrl] [O] Also used by Open <Remove there>",
                "View 2",
                "  Actual size: [Ctrl] [0] Also used by Fit to window <Remove there>",
                "  Fit to window: [Ctrl] [0] Also used by Actual size <Remove there>"), lines(editor));

        filter(editor, "shortcuts-filter-all");

        assertEquals(14, lines(editor).size());
    }

    @Test
    @DisplayName("one filter is always chosen: a click on the chosen one keeps it")
    void oneFilterIsAlwaysChosen() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));

        filter(editor, "shortcuts-filter-all");

        assertTrue(filterButton(editor, "shortcuts-filter-all").isSelected());
        assertEquals(14, lines(editor).size());

        filter(editor, "shortcuts-filter-conflicts");
        filter(editor, "shortcuts-filter-conflicts");

        assertTrue(filterButton(editor, "shortcuts-filter-conflicts").isSelected());
        assertEquals(3, lines(editor).size());
    }

    @Test
    @DisplayName("a filter and the search together: the rows that pass the filter and match the search")
    void filterAndSearchTogether() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));
        record(editor, "Close tab", keyPressed(KeyCode.O, "o", false, true, false));
        filter(editor, "shortcuts-filter-conflicts");

        search(editor, "ctrl+0");

        assertEquals(List.of(
                "View 2",
                "  Actual size: [Ctrl] [0] Also used by Fit to window <Remove there>",
                "  Fit to window: [Ctrl] [0] Also used by Actual size <Remove there>"), lines(editor));

        filter(editor, "shortcuts-filter-modified");

        assertEquals(List.of(), lines(editor));
    }

    @Test
    @DisplayName("a row leaves the filter's rows as soon as it no longer passes: a fixed conflict, a reset key")
    void rowsFollowTheFilter() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));
        record(editor, "Close tab", keyPressed(KeyCode.O, "o", false, true, false));
        filter(editor, "shortcuts-filter-conflicts");

        fix(row(editor, "Close tab")).fire();
        layout(editor);

        assertEquals(List.of(
                "View 2",
                "  Actual size: [Ctrl] [0] Also used by Fit to window <Remove there>",
                "  Fit to window: [Ctrl] [0] Also used by Actual size <Remove there>"), lines(editor));

        filter(editor, "shortcuts-filter-modified");
        assertEquals(List.of("File 2", "  Open *: Not assigned", "  Close tab *: [Ctrl] [O]"), lines(editor));

        button(row(editor, "Open"), "shortcuts-reset").fire();
        layout(editor);

        assertEquals(List.of("File 1", "  Close tab *: [Ctrl] [O] Also used by Open <Remove there>"),
                lines(editor));
    }

    // --- Reset all -------------------------------------------------------------------------------------

    @Test
    @DisplayName("Reset all puts every key back to its default in the working copy; the registry waits for apply")
    void resetAllPutsTheDefaultsBack() {
        Workbench workbench = new Workbench();
        workbench.registry.setChangedShortcuts(Map.of("view.run", "F6", "edit.find", ""));
        ShortcutsEditor editor = shown(new ShortcutsEditor(workbench.registry));
        record(editor, "Save", keyPressed(KeyCode.F9, "", false, false, false));
        record(editor, "Insert r\u00E9sum\u00E9", keyPressed(KeyCode.R, "r", false, true, true));
        assertEquals("11 commands \u00B7 4 modified \u00B7 1 conflict \u00B7 1 not assigned", counter(editor));

        resetAll(editor);

        assertEquals(List.of(
                "File 3",
                "  Open: [Ctrl] [O]",
                "  Save: [Ctrl] [S]",
                "  Close tab: [Ctrl] [W]",
                "Edit 4",
                "  Undo: [Ctrl] [Z]",
                "  Copy [Document]: [Ctrl] [C]",
                "  Find [Document]: [Ctrl] [F]",
                "  Insert r\u00E9sum\u00E9: Not assigned",
                "View 4",
                "  Zoom in: [Ctrl] [+]",
                "  Actual size: [Ctrl] [0] Also used by Fit to window <Remove there>",
                "  Fit to window: [Ctrl] [0] Also used by Actual size <Remove there>",
                "  Run: [F5]"), lines(editor));
        assertEquals("11 commands \u00B7 0 modified \u00B7 1 conflict \u00B7 1 not assigned", counter(editor));
        assertTrue(editor.hasChanges(), "the registry still holds the keys of the earlier session");
        assertEquals(Shortcut.of(KeyCode.F6), workbench.run.getShortcut(), "the registry waits for apply");
        assertEquals(Shortcut.of(KeyCode.S, CTRL), workbench.save.getShortcut());

        editor.apply();

        assertEquals(Shortcut.of(KeyCode.F5), workbench.run.getShortcut());
        assertEquals(Shortcut.of(KeyCode.F, CTRL), workbench.find.getShortcut());
        assertEquals(Map.of(), workbench.registry.getChangedShortcuts());
        assertFalse(editor.hasChanges());
    }

    @Test
    @DisplayName("Reset all is disabled while every key is its default")
    void resetAllIsDisabledWithoutModifiedKeys() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));
        Button resetAll = (Button) editor.lookup(".shortcuts-reset-all");
        assertTrue(resetAll.isDisabled());

        button(select(editor, "Save"), "shortcuts-remove").fire();

        assertFalse(resetAll.isDisabled());

        resetAll.fire();

        assertTrue(resetAll.isDisabled());
        assertFalse(editor.hasChanges());
    }

    @Test
    @DisplayName("in the dialog, Reset all takes effect with Apply, and Cancel drops it")
    void resetAllInTheDialog() throws InterruptedException {
        App app = new App();
        app.registry.setChangedShortcuts(Map.of("file.save", "F9"));
        onFxThread(() -> {
            ShortcutsDialog dialog = new ShortcutsDialog(null, app.registry);
            ShortcutsEditor editor = dialog.getEditor();
            Parent root = editor.getScene().getRoot();
            ((Region) root).resize(760, 620);
            layout(root);

            resetAll(editor);
            button(root, "shortcuts-dialog-cancel").fire();

            assertEquals(Shortcut.of(KeyCode.F9), app.save.getShortcut());

            layout(root);
            resetAll(editor);
            button(root, "shortcuts-dialog-apply").fire();

            assertEquals(Shortcut.of(KeyCode.S, CTRL), app.save.getShortcut());
            assertEquals(Map.of(), app.registry.getChangedShortcuts());
        });
    }

    // --- A new working copy ---------------------------------------------------------------------------

    @Test
    @DisplayName("discard starts over with every row: the search empty and All chosen")
    void discardClearsTheSearchAndTheFilter() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new Workbench().registry));
        button(select(editor, "Save"), "shortcuts-remove").fire();
        filter(editor, "shortcuts-filter-modified");
        search(editor, "save");

        editor.discard();
        layout(editor);

        assertEquals("", ((SearchInput) editor.lookup(".shortcuts-search")).getText());
        assertTrue(filterButton(editor, "shortcuts-filter-all").isSelected());
        assertEquals(14, lines(editor).size());
        assertEquals("  Save: [Ctrl] [S]", lines(editor).get(2));
    }

    // --- Helpers ---------------------------------------------------------------------------------------

    /**
     * Puts every key back to its default with the bar's <i>Reset all</i>.
     */
    private static void resetAll(ShortcutsEditor editor) {
        Button resetAll = (Button) editor.lookup(".shortcuts-reset-all");
        assertNotNull(resetAll, "the bar has no Reset all");
        assertFalse(resetAll.isDisabled(), "Reset all is enabled");
        resetAll.fire();
        layout(editor);
    }

    /**
     * The text the empty list shows in place of rows, found by its style class: null while it does not show.
     */
    private static String noMatch(ShortcutsEditor editor) {
        layout(editor);
        Node found = editor.lookup(".shortcuts-no-match");
        if (found == null) {
            return null;
        }
        for (Node shown = found; shown != null; shown = shown.getParent()) {
            if (!shown.isVisible()) {
                return null;
            }
        }
        return ((Label) found).getText();
    }

    /**
     * The one-click fix of a row's conflict, found by its style class.
     */
    private static Hyperlink fix(ListCell<?> row) {
        Node found = row.lookup(".shortcuts-fix");
        assertNotNull(found, "the row has no fix");
        assertTrue(found.isVisible(), "the row's fix shows");
        return (Hyperlink) found;
    }

    /**
     * A filter of the editor's bar, found by its style class.
     */
    private static ToggleButton filterButton(ShortcutsEditor editor, String styleClass) {
        ToggleButton button = (ToggleButton) editor.lookup("." + styleClass);
        assertNotNull(button, "no filter is " + styleClass);
        return button;
    }

    /**
     * Chooses a filter of the editor's bar, as a click on it does.
     */
    private static void filter(ShortcutsEditor editor, String styleClass) {
        filterButton(editor, styleClass).fire();
        layout(editor);
    }

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
     * Types a search into the editor's search field.
     */
    private static void search(ShortcutsEditor editor, String text) {
        ((SearchInput) editor.lookup(".shortcuts-search")).setText(text);
        layout(editor);
    }

    /**
     * An application with commands in three groups, a document area with a fixed command and one of its own, a name
     * with accents, a command without a key, and two commands on Ctrl+0 by default, a conflict.
     */
    static final class Workbench {

        final StackPane documentArea = new StackPane(new Label("text"));

        final CommandRegistry registry;

        final CommandContext document;

        final CommandGroup file;

        final CommandGroup edit;

        final CommandGroup view;

        final Command open;

        final Command save;

        final Command closeTab;

        final Command undo;

        final Command copy;

        final Command find;

        final Command insertResume;

        final Command zoomIn;

        final Command actualSize;

        final Command fitToWindow;

        final Command run;

        Workbench() {
            registry = new CommandRegistry(themedScene(new VBox(documentArea)));
            // The clash of the two defaults on Ctrl+0 is logged: kept out of the test's output.
            registry.setLogger(new RecordingLogger());
            document = registry.addContext("document", "Document", documentArea);
            file = registry.addGroup("file", "File");
            edit = registry.addGroup("edit", "Edit");
            view = registry.addGroup("view", "View");
            open = registry.addCommand(file, "file.open", "Open", Shortcut.of(KeyCode.O, CTRL), () -> { });
            save = registry.addCommand(file, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), () -> { });
            closeTab = registry.addCommand(file, "file.close", "Close tab", Shortcut.of(KeyCode.W, CTRL), () -> { });
            undo = registry.addCommand(edit, "edit.undo", "Undo", Shortcut.of(KeyCode.Z, CTRL), () -> { });
            copy = registry.addFixedCommand(edit, document, "edit.copy", "Copy", Shortcut.of(KeyCode.C, CTRL), null);
            find = registry.addCommand(edit, document, "edit.find", "Find", Shortcut.of(KeyCode.F, CTRL), () -> { });
            insertResume = registry.addCommand(edit, "edit.resume", "Insert r\u00E9sum\u00E9", null, () -> { });
            zoomIn = registry.addCommand(view, "view.zoomIn", "Zoom in", Shortcut.of('+', CTRL), () -> { });
            actualSize = registry.addCommand(view, "view.actualSize", "Actual size", Shortcut.of(KeyCode.DIGIT0, CTRL),
                    () -> { });
            fitToWindow = registry.addCommand(view, "view.fit", "Fit to window", Shortcut.of(KeyCode.DIGIT0, CTRL),
                    () -> { });
            run = registry.addCommand(view, "view.run", "Run", Shortcut.of(KeyCode.F5), () -> { });
        }
    }
}
