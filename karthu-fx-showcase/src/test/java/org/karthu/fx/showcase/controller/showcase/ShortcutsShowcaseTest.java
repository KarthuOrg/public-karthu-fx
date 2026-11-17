package org.karthu.fx.showcase.controller.showcase;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.Command;
import org.karthu.fx.command.CommandContext;
import org.karthu.fx.command.CommandGroup;
import org.karthu.fx.command.CommandRegistry;
import org.karthu.fx.command.ShortcutsEditor;
import org.karthu.fx.markdown.MarkdownView;
import org.karthu.fx.showcase.viewmodel.ShowcaseItem;
import org.karthu.fx.showcase.viewmodel.StyleShowcaseViewModel;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The <i>Shortcuts</i> page of the group <i>Navigation</i>, loaded from its FXML and laid out with the theme in a scene
 * that is never shown: the example application's commands, their keys sent as events to the focused node, the
 * read-outs, the tooltips, the stored text list and the embedded editor. The dialog is a window: its button is checked,
 * never pressed.
 */
class ShortcutsShowcaseTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @Test
    @DisplayName("a key pressed anywhere on the page runs the window's command, and the read-out names it")
    void keyRunsTheWindowsCommand() throws IOException {
        Parent root = layOut();
        assertEquals("Last command: none yet", lastCommand(root));

        press(root.lookup("#exampleToolbar"), KeyCode.S, true, false, false);

        assertEquals("Last command: Save (file.save), 1 command so far", lastCommand(root));
    }

    @Test
    @DisplayName("the example application: 30 commands in six groups, three contexts, three without a key, one fixed")
    void commandsOfTheExampleApplication() throws IOException {
        CommandRegistry registry = loaded().registry();

        List<String> groups = new ArrayList<>();
        for (CommandGroup group : registry.getGroups()) {
            groups.add(group.getName() + " " + commandsOf(registry, group).size());
        }
        assertEquals(List.of("File 6", "Edit 7", "Search 5", "Document 4", "View 5", "Help 3"), groups);
        assertEquals(30, registry.getCommands().size());
        List<String> contexts = new ArrayList<>();
        for (CommandContext context : registry.getContexts()) {
            contexts.add(context.getName());
        }
        assertEquals(List.of("Window", "Document", "Note"), contexts);
        List<String> withoutKey = new ArrayList<>();
        List<String> fixed = new ArrayList<>();
        for (Command command : registry.getCommands()) {
            if (command.getShortcut() == null) {
                withoutKey.add(command.getName());
            }
            if (command.isFixed()) {
                fixed.add(command.getName() + " " + command.getShortcutDisplayText() + " in "
                        + command.getContext().getName());
            }
        }
        assertEquals(List.of("Export as PDF\u2026", "Fit to width", "About"), withoutKey);
        assertEquals(List.of("Copy Ctrl+C in Document"), fixed);
    }

    @Test
    @DisplayName("in the document Ctrl+F finds in the document, elsewhere it runs the window's Find")
    void documentOverridesTheWindowsKey() throws IOException {
        Parent root = layOut();

        press(root.lookup("#document"), KeyCode.F, true, false, false);
        assertEquals("Last command: Find in document (document.find), 1 command so far", lastCommand(root));
        press(root.lookup("#exampleToolbar"), KeyCode.F, true, false, false);
        assertEquals("Last command: Find\u2026 (search.find), 2 commands so far", lastCommand(root));
    }

    @Test
    @DisplayName("the note keeps its editing keys, and its own commands win over the window's there")
    void noteKeepsItsEditingKeys() throws IOException {
        Parent root = layOut();
        TextField note = (TextField) root.lookup("#noteField");
        note.setText("Call back");

        press(note, KeyCode.Z, true, false, false);
        assertEquals("Last command: none yet", lastCommand(root), "Ctrl+Z is the note's own undo");
        press(note, KeyCode.D, true, false, false);
        assertEquals("Last command: Insert date (note.insert-date), 1 command so far", lastCommand(root));
        assertTrue(note.getText().contains(LocalDate.now().toString()), note.getText());
        press(note, KeyCode.ESCAPE, false, false, false);
        assertEquals("Last command: Clear note (note.clear), 2 commands so far", lastCommand(root));
        assertEquals("", note.getText());
        press(root.lookup("#exampleToolbar"), KeyCode.D, true, false, false);
        assertEquals("Last command: Duplicate (edit.duplicate), 3 commands so far", lastCommand(root));
    }

    @Test
    @DisplayName("a disabled command leaves its key to the next one outward, and its controls are disabled")
    void disabledCommandLeavesItsKey() throws IOException {
        Parent root = layOut();

        switchOf(root, "Find in document disabled").setSelected(true);
        press(root.lookup("#document"), KeyCode.F, true, false, false);
        assertEquals("Last command: Find\u2026 (search.find), 1 command so far", lastCommand(root));

        Button undo = toolbarButton(root, "Undo");
        assertFalse(undo.isDisabled());
        switchOf(root, "Nothing to undo").setSelected(true);
        assertTrue(undo.isDisabled());
        press(root.lookup("#exampleToolbar"), KeyCode.Z, true, false, false);
        assertEquals("Last command: Find\u2026 (search.find), 1 command so far", lastCommand(root));
    }

    @Test
    @DisplayName("the View commands zoom the document: Ctrl and the keypad's plus, Ctrl and minus, Ctrl+0")
    void viewCommandsZoomTheDocument() throws IOException {
        Parent root = layOut();
        MarkdownView document = (MarkdownView) root.lookup("#document");

        press(root.lookup("#exampleToolbar"), KeyCode.ADD, true, false, false);
        assertEquals(1.25, document.getZoom(), 0.001);
        press(document, KeyCode.ADD, true, false, false);
        press(document, KeyCode.MINUS, true, false, false);
        assertEquals(1.25, document.getZoom(), 0.001);
        press(document, KeyCode.DIGIT0, true, false, false);
        assertEquals(1.0, document.getZoom(), 0.001);
        assertEquals("Last command: Actual size (view.actual-size), 4 commands so far", lastCommand(root));
    }

    @Test
    @DisplayName("tooltips, the note's prompt and the menu items name the current key, and drop it when it is removed")
    void textsFollowTheKeys() throws IOException {
        Page page = loaded();
        Button save = toolbarButton(page.root, "Save");
        MenuItem saveItem = menuItem(page.root, "File", "Save");
        TextField note = (TextField) page.root.lookup("#noteField");
        assertEquals("Save (Ctrl+S)", save.getTooltip().getText());
        assertEquals("Ctrl+S", saveItem.getAccelerator().getDisplayText());
        assertEquals("A note: Ctrl+D inserts the date", note.getPromptText());

        page.registry().setChangedShortcuts(Map.of("file.save", "Ctrl+Alt+S", "note.insert-date", "Ctrl+Alt+D"));
        assertEquals("Save (Ctrl+Alt+S)", save.getTooltip().getText());
        assertEquals("Ctrl+Alt+S", saveItem.getAccelerator().getDisplayText());
        assertEquals("A note: Ctrl+Alt+D inserts the date", note.getPromptText());

        page.registry().setChangedShortcuts(Map.of("file.save", "", "note.insert-date", ""));
        assertEquals("Save", save.getTooltip().getText());
        assertNull(saveItem.getAccelerator());
        assertEquals("A note", note.getPromptText());
    }

    @Test
    @DisplayName("the menu bar has a menu for each group and an item for each command of the window and the document")
    void menuBarFollowsTheGroups() throws IOException {
        MenuBar menuBar = (MenuBar) layOut().lookup("#exampleMenuBar");

        List<String> menus = new ArrayList<>();
        for (Menu menu : menuBar.getMenus()) {
            menus.add(menu.getText() + " " + menu.getItems().size());
        }
        assertEquals(List.of("File 6", "Edit 4", "Search 5", "Document 4", "View 5", "Help 3"), menus,
                "Copy is the document's own key, Insert date and Clear note the note's");
    }

    @Test
    @DisplayName("the stored text list: empty at first, a list handed back skips what it cannot use, Reset all empties it")
    void storedTextList() throws IOException {
        Page page = loaded();
        assertEquals("(empty: every key is its default)", storedList(page.root));

        button(page.root, "#storedListButtons", "Hand back a stored list").fire();
        assertEquals(String.join("\n", "file.save-as=Ctrl+Alt+S", "edit.duplicate=", "view.actual-size=",
                "view.fit-width=Ctrl+0"), storedList(page.root),
                "Ctrl+0 is the user's key of Fit to width: Actual size loses it; the unknown and the unreadable"
                        + " entries are skipped");
        assertEquals("", command(page.registry(), "view.actual-size").getShortcutDisplayText());

        button(page.root, "#storedListButtons", "Reset all keys").fire();
        assertEquals("(empty: every key is its default)", storedList(page.root));
        assertEquals("Ctrl+0", command(page.registry(), "view.actual-size").getShortcutDisplayText());
    }

    @Test
    @DisplayName("the embedded editor: a key removed there is a change, Apply hands it to the registry, Discard drops it")
    void embeddedEditor() throws IOException {
        Page page = loaded();
        Button apply = button(page.root, "#editorButtons", "Apply");
        Button discard = button(page.root, "#editorButtons", "Discard");
        assertTrue(apply.isDisabled());
        assertTrue(discard.isDisabled());
        assertEquals("Has changes: no \u00B7 has conflicts: no", editorState(page.root));

        removeKeyInEditor(page.root, 1);
        assertFalse(apply.isDisabled());
        assertFalse(discard.isDisabled());
        assertEquals("Has changes: yes \u00B7 has conflicts: no", editorState(page.root));
        assertEquals("(empty: every key is its default)", storedList(page.root), "nothing applied yet");
        apply.fire();
        assertEquals("file.new=", storedList(page.root));
        assertTrue(apply.isDisabled());

        removeKeyInEditor(page.root, 2);
        discard.fire();
        assertEquals("file.new=", storedList(page.root));
        assertEquals("Ctrl+O", command(page.registry(), "file.open").getShortcutDisplayText());
    }

    @Test
    @DisplayName("the embedded editor's Apply is disabled while a key is on two commands of one context")
    void embeddedEditorRefusesConflicts() throws IOException {
        Page page = loaded();
        Button apply = button(page.root, "#editorButtons", "Apply");
        // Two stored entries on one key both stay: a conflict the user has to resolve.
        page.registry().setChangedShortcuts(Map.of("file.new", "Ctrl+Alt+N", "file.close", "Ctrl+Alt+N"));
        layOut(page.root);

        removeKeyInEditor(page.root, 2);
        assertEquals("Has changes: yes \u00B7 has conflicts: yes", editorState(page.root));
        assertTrue(apply.isDisabled());
    }

    @Test
    @DisplayName("the dialog's button runs the command Keyboard shortcuts, its tooltip names the key; never pressed here")
    void dialogButton() throws IOException {
        Button open = (Button) layOut().lookup("#openDialogButton");

        assertEquals("Keyboard shortcuts\u2026", open.getText());
        assertEquals("Keyboard shortcuts\u2026 (Ctrl+K)", open.getTooltip().getText());
        assertNotNull(open.getOnAction());
    }

    @Test
    @DisplayName("on another page of the showcase the application's keys do nothing, back on its page they run again")
    void commandsHoldWhileThePageShows() throws IOException {
        Page page = loaded();
        Scene scene = page.root.getScene();
        Button otherPage = new Button("Another page");
        scene.setRoot(new VBox(otherPage));

        press(otherPage, KeyCode.S, true, false, false);
        assertEquals("Last command: none yet", lastCommand(page.root));

        scene.setRoot(page.root);
        layOut(page.root);
        press(page.root.lookup("#exampleToolbar"), KeyCode.S, true, false, false);
        assertEquals("Last command: Save (file.save), 1 command so far", lastCommand(page.root));
    }

    @Test
    @DisplayName("the page is in the showcase's navigation, in the group Navigation")
    void pageInTheNavigation() {
        List<String> found = new ArrayList<>();
        for (ShowcaseItem item : new StyleShowcaseViewModel().getShowcaseItems()) {
            if (item.fxmlFileName().equals("shortcuts-showcase.fxml")) {
                found.add(item.displayName() + " in " + item.category());
            }
        }
        assertEquals(List.of("Shortcuts in Navigation"), found);
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** Loads the page and lays it out in the theme, in a scene that is never shown. */
    private static Parent layOut() throws IOException {
        return loaded().root;
    }

    /** The page loaded and laid out, with its controller. */
    private static Page loaded() throws IOException {
        URL url = ShortcutsShowcaseTest.class.getResource("/org/karthu/fx/showcase/showcase/shortcuts-showcase.fxml");
        assertNotNull(url);
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1400, 900);
        ThemeManager.apply(scene);
        layOut(root);
        return new Page(root, loader.getController());
    }

    /** A loaded page: its root and its controller. */
    private record Page(Parent root, ShortcutsShowcaseController controller) {

        CommandRegistry registry() {
            return controller.registry();
        }
    }

    private static List<Command> commandsOf(CommandRegistry registry, CommandGroup group) {
        List<Command> commands = new ArrayList<>();
        for (Command command : registry.getCommands()) {
            if (command.getGroup() == group) {
                commands.add(command);
            }
        }
        return commands;
    }

    private static CheckBox switchOf(Parent root, String text) {
        for (Node child : ((Pane) root.lookup("#exampleSwitches")).getChildren()) {
            if (child instanceof CheckBox box && box.getText().equals(text)) {
                return box;
            }
        }
        throw new AssertionError("no switch " + text);
    }

    private static Command command(CommandRegistry registry, String id) {
        for (Command command : registry.getCommands()) {
            if (command.getId().equals(id)) {
                return command;
            }
        }
        throw new AssertionError("no command " + id);
    }

    /** A menu item of the example application's menu bar, by its menu's and its own text. */
    private static MenuItem menuItem(Parent root, String menuText, String itemText) {
        for (Menu menu : ((MenuBar) root.lookup("#exampleMenuBar")).getMenus()) {
            for (MenuItem item : menu.getItems()) {
                if (menu.getText().equals(menuText) && itemText.equals(item.getText())) {
                    return item;
                }
            }
        }
        throw new AssertionError("no menu item " + menuText + " > " + itemText);
    }

    /** A button of a box of buttons, by its text. */
    private static Button button(Parent root, String boxId, String text) {
        for (Node child : ((Pane) root.lookup(boxId)).getChildren()) {
            if (child instanceof Button button && text.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("no button " + text + " in " + boxId);
    }

    private static String storedList(Parent root) {
        return ((Label) root.lookup("#storedListLabel")).getText();
    }

    private static String editorState(Parent root) {
        return ((Label) root.lookup("#editorStateLabel")).getText();
    }

    /**
     * Selects a line of the embedded editor's list and presses Delete there, which removes the key of the command on
     * that line.
     */
    private static void removeKeyInEditor(Parent root, int line) {
        ShortcutsEditor editor = (ShortcutsEditor) root.lookup(".shortcuts-editor");
        ListView<?> list = (ListView<?>) editor.lookup(".shortcuts-list");
        layOut(root);
        list.getSelectionModel().select(line);
        press(list, KeyCode.DELETE, false, false, false);
        layOut(root);
    }

    /** A button of the example application's toolbar, by the command's name its tooltip starts with. */
    private static Button toolbarButton(Parent root, String name) {
        for (Node child : ((Pane) root.lookup("#exampleToolbar")).getChildren()) {
            if (child instanceof Button button && button.getTooltip() != null
                    && button.getTooltip().getText().startsWith(name)) {
                return button;
            }
        }
        throw new AssertionError("no toolbar button " + name);
    }

    /** Two passes, as two pulses of a shown window: lists build their rows in the first, the second styles them. */
    private static void layOut(Parent root) {
        for (int pass = 0; pass < 2; pass++) {
            root.applyCss();
            root.autosize();
            root.layout();
        }
    }

    /**
     * Gives a node the focus and presses a key there, as the platform sends it on Windows and Linux: Ctrl is the
     * Control key.
     */
    private static void press(Node node, KeyCode code, boolean ctrl, boolean alt, boolean shift) {
        node.requestFocus();
        assertEquals(node, node.getScene().getFocusOwner(), "the node did not take the focus");
        node.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, ctrl, alt, false));
    }

    private static String lastCommand(Parent root) {
        return ((Label) root.lookup("#lastCommandLabel")).getText();
    }
}
