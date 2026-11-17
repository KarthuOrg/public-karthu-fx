package org.karthu.fx.command;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssParser;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.ShortcutsEditorTest.App;
import org.karthu.fx.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.CommandTestSupport.onFxThread;
import static org.karthu.fx.command.CommandTestSupport.themedScene;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.button;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.counter;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.is;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.keyPressed;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.layout;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.line;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.press;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.row;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.select;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.shown;

/**
 * Conflicts and overrides in the shortcut editor and the dialog, through their public API and their documented style
 * classes, in a themed scene that is never shown: a key on two commands of one context, its hint and its one-click fix,
 * the editor's has conflicts and the dialog's Apply, and a key of a part of the window that overrides the window's.
 */
class ShortcutConflictsTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    @Test
    @DisplayName("recording a key another command of the context holds marks both rows, each naming the other")
    void recordingATakenKeyMarksBothRows() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));

        record(editor, "Save", keyPressed(KeyCode.O, "o", false, true, false));

        assertEquals("  Save *: [Ctrl] [O] Also used by Open... <Remove there>", line(row(editor, "Save")));
        assertEquals("  Open...: [Ctrl] [O] Also used by Save <Remove there>", line(row(editor, "Open...")));
        assertTrue(is(row(editor, "Save"), "conflict"));
        assertTrue(is(row(editor, "Open..."), "conflict"));
        assertFalse(is(row(editor, "Undo"), "conflict"));
        assertEquals("  Undo: [Ctrl] [Z]", line(row(editor, "Undo")), "a row without a conflict shows no hint");
    }

    @Test
    @DisplayName("the hint's fix removes the key from the other command, and both rows lose the conflict")
    void fixRemovesTheKeyFromTheOtherCommand() {
        App app = new App();
        ShortcutsEditor editor = shown(new ShortcutsEditor(app.registry));
        record(editor, "Save", keyPressed(KeyCode.O, "o", false, true, false));

        fix(row(editor, "Save")).fire();
        layout(editor);

        assertEquals("  Save *: [Ctrl] [O]", line(row(editor, "Save")));
        assertEquals("  Open... *: Not assigned", line(row(editor, "Open...")));
        assertFalse(is(row(editor, "Save"), "conflict"));
        assertFalse(is(row(editor, "Open..."), "conflict"));

        editor.apply();

        assertEquals(Shortcut.of(KeyCode.O, CTRL), app.save.getShortcut());
        assertNull(app.open.getShortcut());
    }

    @Test
    @DisplayName("has conflicts is true while a conflict is left, and the counter counts the conflicts")
    void hasConflictsAndTheCounter() {
        ShortcutsEditor editor = shown(new ShortcutsEditor(new App().registry));
        List<Boolean> seen = new ArrayList<>();
        editor.hasConflictsProperty().addListener((observable, before, now) -> seen.add(now));
        assertFalse(editor.hasConflicts());

        record(editor, "Save", keyPressed(KeyCode.O, "o", false, true, false));

        assertTrue(editor.hasConflicts());
        assertEquals("9 commands \u00B7 1 modified \u00B7 1 conflict \u00B7 1 not assigned", counter(editor));
        assertTrue(is(editor.lookup(".shortcuts-count-conflicts"), "conflict"));

        record(editor, "Undo", keyPressed(KeyCode.N, "n", false, true, false));

        assertEquals("9 commands \u00B7 2 modified \u00B7 2 conflicts \u00B7 1 not assigned", counter(editor));

        fix(row(editor, "Save")).fire();
        fix(row(editor, "New workflow")).fire();
        layout(editor);

        assertFalse(editor.hasConflicts());
        assertEquals("9 commands \u00B7 3 modified \u00B7 0 conflicts \u00B7 3 not assigned", counter(editor));
        assertFalse(is(editor.lookup(".shortcuts-count-conflicts"), "conflict"));
        assertEquals(List.of(true, false), seen);
    }

    @Test
    @DisplayName("the dialog's Apply is disabled while the editor has conflicts")
    void applyIsDisabledWhileConflictsAreLeft() throws InterruptedException {
        App app = new App();
        onFxThread(() -> {
            ShortcutsDialog dialog = new ShortcutsDialog(null, app.registry);
            ShortcutsEditor editor = dialog.getEditor();
            Parent root = editor.getScene().getRoot();
            ((Region) root).resize(760, 620);
            layout(root);
            Button apply = button(root, "shortcuts-dialog-apply");
            assertFalse(apply.isDisabled());

            record(editor, "Save", keyPressed(KeyCode.O, "o", false, true, false));

            assertTrue(apply.isDisabled());
            assertEquals(editor.hasConflicts(), apply.isDisabled());

            fix(row(editor, "Open...")).fire();
            layout(root);

            assertFalse(apply.isDisabled());
            assertEquals(editor.hasConflicts(), apply.isDisabled());
            apply.fire();
            assertNull(app.save.getShortcut());
            assertEquals(Shortcut.of(KeyCode.O, CTRL), app.open.getShortcut());
        });
    }

    @Test
    @DisplayName("a conflict with a fixed command shows on both rows; only the fixed row's fix removes a key")
    void conflictWithAFixedCommand() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));

        record(editor, "Find", keyPressed(KeyCode.C, "c", false, true, false));

        assertEquals("  Find [Document] *: [Ctrl] [C] Also used by Copy", line(row(editor, "Find")),
                "no fix: the fixed command keeps its key");
        assertEquals("  Copy [Document]: [Ctrl] [C] Also used by Find <Remove there>", line(row(editor, "Copy")));
        assertTrue(is(row(editor, "Find"), "conflict"));
        assertTrue(is(row(editor, "Copy"), "conflict"));
        assertTrue(editor.hasConflicts());

        fix(row(editor, "Copy")).fire();
        layout(editor);

        assertEquals("  Find [Document] *: Not assigned", line(row(editor, "Find")));
        assertEquals("  Copy [Document]: [Ctrl] [C]", line(row(editor, "Copy")));
        assertFalse(editor.hasConflicts());
    }

    @Test
    @DisplayName("a fix removes the key from every other command of the conflict but a fixed one")
    void fixOfAConflictOfThree() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));
        record(editor, "Find", keyPressed(KeyCode.A, "a", false, true, false));
        record(editor, "Highlight", keyPressed(KeyCode.A, "a", false, true, false));

        assertEquals("  Highlight [Document] *: [Ctrl] [A] Also used by Select all, Find <Remove there>",
                line(row(editor, "Highlight")));
        assertEquals("9 commands \u00B7 2 modified \u00B7 1 conflict \u00B7 0 not assigned", counter(editor));

        fix(row(editor, "Highlight")).fire();
        layout(editor);

        assertEquals("  Find [Document] *: Not assigned", line(row(editor, "Find")));
        assertEquals("  Highlight [Document] *: [Ctrl] [A] Also used by Select all", line(row(editor, "Highlight")));
        assertEquals("  Select all [Document]: [Ctrl] [A] Also used by Highlight <Remove there>",
                line(row(editor, "Select all")));
    }

    @Test
    @DisplayName("clashing defaults of the application show as a conflict when the dialog opens, Apply disabled")
    void clashingDefaultsShowWhenTheDialogOpens() throws InterruptedException {
        CommandRegistry registry = new CommandRegistry(themedScene(new StackPane()));
        registry.setLogger(new RecordingLogger());
        CommandGroup view = registry.addGroup("view", "View");
        registry.addCommand(view, "view.actualSize", "Actual size (1:1)", Shortcut.of(KeyCode.DIGIT0, CTRL), () -> { });
        registry.addCommand(view, "view.fit", "Fit to window", Shortcut.of(KeyCode.DIGIT0, CTRL), () -> { });
        onFxThread(() -> {
            ShortcutsDialog dialog = new ShortcutsDialog(null, registry);
            ShortcutsEditor editor = dialog.getEditor();
            Parent root = editor.getScene().getRoot();
            ((Region) root).resize(760, 620);
            layout(root);

            assertEquals("  Fit to window: [Ctrl] [0] Also used by Actual size (1:1) <Remove there>",
                    line(row(editor, "Fit to window")));
            assertEquals("  Actual size (1:1): [Ctrl] [0] Also used by Fit to window <Remove there>",
                    line(row(editor, "Actual size (1:1)")));
            assertTrue(editor.hasConflicts());
            assertFalse(editor.hasChanges());
            assertTrue(button(root, "shortcuts-dialog-apply").isDisabled());
            assertEquals("2 commands \u00B7 0 modified \u00B7 1 conflict \u00B7 0 not assigned", counter(editor));
        });
    }

    @Test
    @DisplayName("a stored key on a fixed command's key shows as a conflict when the editor opens")
    void storedKeyOnAFixedKeyShowsAsAConflict() {
        Viewer viewer = new Viewer();
        viewer.registry.setChangedShortcuts(Map.of("edit.find", "Ctrl+C"));

        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));

        assertEquals("  Find [Document] *: [Ctrl] [C] Also used by Copy", line(row(editor, "Find")));
        assertTrue(is(row(editor, "Copy"), "conflict"));
        assertTrue(editor.hasConflicts());
    }

    @Test
    @DisplayName("a key the registry changes while the editor shows can make and resolve a conflict")
    void registryChangesReachTheConflicts() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));

        // Not on Highlight's key: the registry would take it from Highlight. A fixed command keeps its key.
        viewer.registry.setChangedShortcuts(Map.of("edit.find", "Ctrl+C"));

        assertTrue(editor.hasConflicts());
        assertTrue(is(row(editor, "Copy"), "conflict"));

        viewer.registry.resetShortcuts();

        assertFalse(editor.hasConflicts());
        assertFalse(is(row(editor, "Copy"), "conflict"));
    }

    // --- Overrides -------------------------------------------------------------------------------------

    @Test
    @DisplayName("a key of a part of the window that the window's command holds is an override, shown as a hint")
    void keyOfAPartOverridesTheWindowsKey() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));

        record(editor, "Find", keyPressed(KeyCode.O, "o", false, true, false));

        assertEquals("  Find [Document] *: [Ctrl] [O] Overrides Open", line(row(editor, "Find")));
        assertTrue(is(row(editor, "Find"), "override"));
        assertFalse(is(row(editor, "Find"), "conflict"));
        assertEquals("  Open: [Ctrl] [O]", line(row(editor, "Open")), "the window's row shows nothing");
        assertFalse(is(row(editor, "Open"), "override"));
        assertFalse(is(row(editor, "Open"), "conflict"));
        assertFalse(editor.hasConflicts());
        assertEquals("9 commands \u00B7 1 modified \u00B7 0 conflicts \u00B7 0 not assigned", counter(editor));
    }

    @Test
    @DisplayName("parts nest as their nodes do: a part's key overrides the keys of the parts around it, innermost first")
    void nestedPartsOverrideFromTheInnermost() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));

        record(editor, "Close tab", keyPressed(KeyCode.O, "o", false, true, false));
        record(editor, "Highlight", keyPressed(KeyCode.O, "o", false, true, false));

        assertEquals("  Close tab [Tab] *: [Ctrl] [O] Overrides Open", line(row(editor, "Close tab")));
        assertEquals("  Highlight [Document] *: [Ctrl] [O] Overrides Close tab, Open",
                line(row(editor, "Highlight")));
        assertFalse(editor.hasConflicts());
    }

    @Test
    @DisplayName("parts side by side neither conflict nor override, and a window's key never overrides a part's")
    void partsSideBySideDoNotClash() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));

        record(editor, "Hide panel", keyPressed(KeyCode.F, "f", false, true, false));
        record(editor, "Zoom in", keyPressed(KeyCode.C, "c", false, true, false));

        assertEquals("  Hide panel [Side panel] *: [Ctrl] [F]", line(row(editor, "Hide panel")));
        assertEquals("  Find [Document]: [Ctrl] [F]", line(row(editor, "Find")));
        assertEquals("  Zoom in *: [Ctrl] [C]", line(row(editor, "Zoom in")));
        assertEquals("  Copy [Document]: [Ctrl] [C] Overrides Zoom in", line(row(editor, "Copy")),
                "a fixed command overrides too");
        assertFalse(editor.hasConflicts());
    }

    @Test
    @DisplayName("a row in a conflict shows the conflict's hint, also when its key overrides another")
    void conflictWinsOverOverride() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));

        record(editor, "Find", keyPressed(KeyCode.O, "o", false, true, false));
        record(editor, "Highlight", keyPressed(KeyCode.O, "o", false, true, false));

        assertEquals("  Find [Document] *: [Ctrl] [O] Also used by Highlight <Remove there>",
                line(row(editor, "Find")));
        assertTrue(is(row(editor, "Find"), "conflict"));
        assertTrue(is(row(editor, "Find"), "override"));
    }

    @Test
    @DisplayName("the texts of the hints and the fix follow their properties")
    void hintTexts() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = shown(new ShortcutsEditor(viewer.registry));
        record(editor, "Find", keyPressed(KeyCode.H, "h", false, true, false));
        record(editor, "Close tab", keyPressed(KeyCode.O, "o", false, true, false));

        assertEquals("Also used by {0}", editor.getConflictHintFormat());
        assertEquals("Remove there", editor.getConflictFixText());
        assertEquals("Overrides {0}", editor.getOverrideHintFormat());

        editor.setConflictHintFormat("Auch f\u00FCr {0}");
        editor.setConflictFixText("Dort entfernen");
        editor.setOverrideHintFormat("\u00DCberdeckt {0}");

        assertEquals("  Find [Document] *: [Ctrl] [H] Auch f\u00FCr Highlight <Dort entfernen>",
                line(row(editor, "Find")));
        assertEquals("  Close tab [Tab] *: [Ctrl] [O] \u00DCberdeckt Open", line(row(editor, "Close tab")));
        assertEquals("Dort entfernen", fix(row(editor, "Find")).getText());
        assertEquals("Auch f\u00FCr {0}", editor.conflictHintFormatProperty().get());
        assertEquals("Dort entfernen", editor.conflictFixTextProperty().get());
        assertEquals("\u00DCberdeckt {0}", editor.overrideHintFormatProperty().get());
    }

    // --- The theme -------------------------------------------------------------------------------------

    @Test
    @DisplayName("the theme draws a conflict's key caps, hint and count in the error colour, an override's hint muted")
    void themeColours() {
        Viewer viewer = new Viewer();
        ShortcutsEditor editor = new ShortcutsEditor(viewer.registry);
        Region error = swatch("-fx-status-error");
        Region border = swatch("-fx-control-border");
        Region muted = swatch("-fx-text-secondary-color");
        Region accent = swatch("-fx-accent-color");
        VBox root = new VBox(editor, error, border, muted, accent);
        Scene scene = new Scene(root, 760, 700);
        ThemeManager.apply(scene);
        layout(root);
        record(editor, "Find", keyPressed(KeyCode.H, "h", false, true, false));
        record(editor, "Close tab", keyPressed(KeyCode.O, "o", false, true, false));
        select(editor, "Open");
        layout(root);

        ListCell<?> find = row(editor, "Find");
        assertEquals(colour(error), colour((Region) find.lookup(".shortcuts-key-cap")), "a conflict's key cap");
        assertEquals(colour(error), textFill(find.lookup(".shortcuts-key-cap")), "a conflict's key cap's text");
        assertEquals(colour(error), textFill(find.lookup(".shortcuts-hint")), "a conflict's hint");
        assertEquals(colour(accent), textFill(find.lookup(".shortcuts-fix")), "the fix is a link");
        assertEquals(colour(border), colour((Region) row(editor, "Hide panel").lookup(".shortcuts-key-cap")),
                "a key cap without a conflict");
        assertEquals(colour(muted), textFill(row(editor, "Close tab").lookup(".shortcuts-hint")), "an override's hint");
        assertEquals(colour(error), ((Label) editor.lookup(".shortcuts-count-conflicts")).getTextFill(),
                "the number of conflicts");
    }

    @Test
    @DisplayName("the theme styles conflicts and overrides without a CSS error")
    void themeStylesConflictsWithoutError() throws InterruptedException {
        List<String> errors = new ArrayList<>();
        onFxThread(() -> {
            ListChangeListener<CssParser.ParseError> recorder = change -> {
                while (change.next()) {
                    for (CssParser.ParseError parseError : change.getAddedSubList()) {
                        errors.add(parseError.getMessage());
                    }
                }
            };
            ObservableList<CssParser.ParseError> cssErrors = CssParser.errorsProperty();
            cssErrors.addListener(recorder);
            try {
                ShortcutsEditor editor = shown(new ShortcutsEditor(new Viewer().registry));
                record(editor, "Find", keyPressed(KeyCode.H, "h", false, true, false));
                record(editor, "Close tab", keyPressed(KeyCode.O, "o", false, true, false));
                layout(editor);
            } finally {
                cssErrors.removeListener(recorder);
            }
        });

        assertEquals(List.of(), errors, "CSS errors while styling conflicts and overrides");
    }

    // --- Helpers ---------------------------------------------------------------------------------------

    /**
     * A region whose background is a token of the theme, so that a test reads the token's colour.
     */
    private static Region swatch(String token) {
        Region swatch = new Region();
        swatch.setStyle("-fx-background-color: " + token + ";");
        return swatch;
    }

    /**
     * The colour of a region's first background layer: a swatch's token, a key cap's frame.
     */
    private static Paint colour(Region region) {
        return region.getBackground().getFills().get(0).getFill();
    }

    /**
     * The fill of the text a labeled node shows.
     */
    private static Paint textFill(Node labeled) {
        assertNotNull(labeled, "nothing to read the text of");
        return ((Text) labeled.lookup(".text")).getFill();
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
     * Records a key for a command: selects its row, presses Enter and then the key.
     */
    private static void record(ShortcutsEditor editor, String name, KeyEvent key) {
        select(editor, name);
        press(editor, keyPressed(KeyCode.ENTER, "\r", false, false, false));
        ListCell<?> recording = row(editor, name);
        assertTrue(is(recording, "recording"), name + " records");
        press(editor, key);
    }

    /**
     * A viewer with commands of the whole window and of three parts of it: a tab, a document inside the tab, and a
     * side panel beside the tab. The document has two fixed commands. No key clashes with another.
     */
    static final class Viewer {

        final StackPane documentArea = new StackPane(new Label("text"));

        final StackPane tabArea = new StackPane(documentArea);

        final StackPane sidePanel = new StackPane(new Label("panel"));

        final CommandRegistry registry = new CommandRegistry(themedScene(new VBox(tabArea, sidePanel)));

        final CommandContext tab = registry.addContext("tab", "Tab", tabArea);

        final CommandContext document = registry.addContext("document", "Document", documentArea);

        final CommandContext side = registry.addContext("side", "Side panel", sidePanel);

        final CommandGroup file = registry.addGroup("file", "File");

        final CommandGroup edit = registry.addGroup("edit", "Edit");

        final CommandGroup view = registry.addGroup("view", "View");

        final Command open = registry.addCommand(file, "file.open", "Open", Shortcut.of(KeyCode.O, CTRL), () -> { });

        final Command closeTab = registry.addCommand(file, tab, "tab.close", "Close tab", Shortcut.of(KeyCode.W, CTRL),
                () -> { });

        final Command copy = registry.addFixedCommand(edit, document, "edit.copy", "Copy", Shortcut.of(KeyCode.C, CTRL),
                null);

        final Command selectAll = registry.addFixedCommand(edit, document, "edit.selectAll", "Select all",
                Shortcut.of(KeyCode.A, CTRL), null);

        final Command find = registry.addCommand(edit, document, "edit.find", "Find", Shortcut.of(KeyCode.F, CTRL),
                () -> { });

        final Command highlight = registry.addCommand(edit, document, "edit.highlight", "Highlight",
                Shortcut.of(KeyCode.H, CTRL), () -> { });

        final Command zoomIn = registry.addCommand(view, "view.zoomIn", "Zoom in", Shortcut.of('+', CTRL), () -> { });

        final Command resetZoom = registry.addCommand(view, "view.resetZoom", "Reset zoom",
                Shortcut.of(KeyCode.DIGIT0, CTRL), () -> { });

        final Command hidePanel = registry.addCommand(view, side, "side.hide", "Hide panel",
                Shortcut.of(KeyCode.P, CTRL), () -> { });
    }
}
