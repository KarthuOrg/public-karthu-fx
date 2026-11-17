package org.karthu.fx.command;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.CommandTestSupport.RunCounter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.CommandTestSupport.keyPressed;
import static org.karthu.fx.command.CommandTestSupport.onFxThread;
import static org.karthu.fx.command.CommandTestSupport.themedScene;
import static org.karthu.fx.command.Shortcut.Modifier.ALT;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;

/**
 * Contexts through the registry's public API, without a window: parts of the window bound to nodes, whose commands run
 * while the focus is inside them, with the focus set on nodes and key presses sent as events in a themed scene that is
 * never shown.
 */
class CommandContextTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    // --- Contexts and their commands -----------------------------------------------------------------

    @Test
    @DisplayName("the window's context is always there; named contexts follow it with their ids, names and nodes")
    void contextsInOrder() {
        StackPane documentArea = new StackPane();
        StackPane canvas = new StackPane();
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(documentArea, canvas)));
        CommandContext window = registry.getWindowContext();

        CommandContext document = registry.addContext("document", "Document", documentArea);
        CommandContext drawing = registry.addContext("canvas", "Canvas", canvas);

        assertEquals(List.of(window, document, drawing), registry.getContexts());
        assertEquals("window", window.getId());
        assertEquals("Window", window.getName());
        assertNull(window.getNode());
        assertEquals("document", document.getId());
        assertEquals("Document", document.getName());
        assertSame(documentArea, document.getNode());
        document.nameProperty().set("Dokument");
        assertEquals("Dokument", document.getName());
        assertThrows(UnsupportedOperationException.class, () -> registry.getContexts().remove(document));
    }

    @Test
    @DisplayName("a command belongs to one context: the window's unless it is added to another")
    void commandBelongsToOneContext() {
        StackPane documentArea = new StackPane(new Label("text"));
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(documentArea)));
        CommandGroup edit = registry.addGroup("edit", "Edit");
        CommandContext document = registry.addContext("document", "Document", documentArea);

        Command undo = registry.addCommand(edit, "edit.undo", "Undo", Shortcut.of(KeyCode.Z, CTRL), new RunCounter());
        Command find = registry.addCommand(edit, document, "edit.find", "Find", Shortcut.of(KeyCode.F, CTRL),
                new RunCounter());

        assertSame(registry.getWindowContext(), undo.getContext());
        assertSame(document, find.getContext());
        assertEquals(List.of(undo, find), registry.getCommands());
    }

    @Test
    @DisplayName("a context needs an id of its own, a name and a node no other context has; a command one of its registry")
    void contextsAreChecked() {
        StackPane documentArea = new StackPane();
        StackPane canvas = new StackPane();
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(documentArea, canvas)));
        CommandRegistry other = new CommandRegistry(themedScene(new VBox()));
        CommandGroup edit = registry.addGroup("edit", "Edit");
        CommandContext foreign = other.addContext("canvas", "Canvas", new StackPane());
        registry.addContext("document", "Document", documentArea);

        assertThrows(IllegalArgumentException.class, () -> registry.addContext("document", "Other", canvas));
        assertThrows(IllegalArgumentException.class, () -> registry.addContext("window", "Window", canvas));
        assertThrows(IllegalArgumentException.class, () -> registry.addContext(" ", "Blank", canvas));
        assertThrows(IllegalArgumentException.class, () -> registry.addContext("text", "Text", documentArea));
        assertThrows(NullPointerException.class, () -> registry.addContext(null, "Canvas", canvas));
        assertThrows(NullPointerException.class, () -> registry.addContext("canvas", null, canvas));
        assertThrows(NullPointerException.class, () -> registry.addContext("canvas", "Canvas", null));
        assertThrows(IllegalArgumentException.class,
                () -> registry.addCommand(edit, foreign, "canvas.delete", "Delete", null, new RunCounter()));
        assertThrows(NullPointerException.class,
                () -> registry.addCommand(edit, null, "canvas.delete", "Delete", null, new RunCounter()));
        assertEquals(2, registry.getContexts().size());
        assertEquals(0, registry.getCommands().size());
        registry.addContext("canvas", "Canvas", canvas);
    }

    // --- Firing --------------------------------------------------------------------------------------

    @Test
    @DisplayName("with the focus inside a context its command runs, not the window's on the same key; outside, the window's")
    void contextCommandOverridesTheWindowsInside() {
        Button inDocument = new Button("in the document");
        Button outside = new Button("outside");
        StackPane documentArea = new StackPane(new VBox(inDocument));
        Scene scene = themedScene(new VBox(documentArea, outside));
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup edit = registry.addGroup("edit", "Edit");
        CommandContext document = registry.addContext("document", "Document", documentArea);
        RunCounter findInWindow = new RunCounter();
        RunCounter findInDocument = new RunCounter();
        RunCounter bookmark = new RunCounter();
        registry.addCommand(edit, "edit.find", "Find", Shortcut.of(KeyCode.F, CTRL), findInWindow);
        registry.addCommand(edit, document, "document.find", "Find in Document", Shortcut.of(KeyCode.F, CTRL),
                findInDocument);
        registry.addCommand(edit, document, "document.bookmark", "Bookmark", Shortcut.of(KeyCode.B, CTRL), bookmark);

        inDocument.requestFocus();
        pressOnFocusOwner(scene, KeyCode.F, CTRL);
        pressOnFocusOwner(scene, KeyCode.B, CTRL);
        assertEquals(0, findInWindow.runs());
        assertEquals(1, findInDocument.runs());
        assertEquals(1, bookmark.runs());

        outside.requestFocus();
        pressOnFocusOwner(scene, KeyCode.F, CTRL);
        pressOnFocusOwner(scene, KeyCode.B, CTRL);
        assertEquals(1, findInWindow.runs());
        assertEquals(1, findInDocument.runs());
        assertEquals(1, bookmark.runs());
    }

    @Test
    @DisplayName("the focus on the context's node itself is inside the context")
    void focusOnTheContextsNode() {
        Button canvas = new Button("canvas");
        Scene scene = themedScene(new VBox(canvas, new Button("outside")));
        CommandRegistry registry = new CommandRegistry(scene);
        RunCounter delete = new RunCounter();
        registry.addCommand(registry.addGroup("edit", "Edit"), registry.addContext("canvas", "Canvas", canvas),
                "canvas.delete", "Delete", Shortcut.of(KeyCode.DELETE), delete);

        canvas.requestFocus();
        pressOnFocusOwner(scene, KeyCode.DELETE);

        assertEquals(1, delete.runs());
    }

    @Test
    @DisplayName("nested contexts: the innermost with the focus wins, the enclosing one runs where the inner has no command")
    void innermostContextWins() {
        Button onCanvas = new Button("node on the canvas");
        StackPane canvas = new StackPane(onCanvas);
        Button inTabOnly = new Button("tab's tool bar");
        StackPane workflowTab = new StackPane(new VBox(inTabOnly, canvas));
        Scene scene = themedScene(new VBox(workflowTab, new Button("outside")));
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup edit = registry.addGroup("edit", "Edit");
        // The inner context is added first: nesting follows the nodes, not the order of adding.
        CommandContext canvasContext = registry.addContext("canvas", "Canvas", canvas);
        CommandContext tabContext = registry.addContext("workflow", "Workflow", workflowTab);
        RunCounter selectAllInWindow = new RunCounter();
        RunCounter selectAllInTab = new RunCounter();
        RunCounter selectAllOnCanvas = new RunCounter();
        RunCounter saveInTab = new RunCounter();
        registry.addCommand(edit, "edit.selectAll", "Select All", Shortcut.of(KeyCode.A, CTRL), selectAllInWindow);
        registry.addCommand(edit, tabContext, "workflow.selectAll", "Select All Nodes", Shortcut.of(KeyCode.A, CTRL),
                selectAllInTab);
        registry.addCommand(edit, canvasContext, "canvas.selectAll", "Select All on Canvas",
                Shortcut.of(KeyCode.A, CTRL), selectAllOnCanvas);
        registry.addCommand(edit, tabContext, "workflow.save", "Save Workflow", Shortcut.of(KeyCode.S, CTRL),
                saveInTab);

        onCanvas.requestFocus();
        pressOnFocusOwner(scene, KeyCode.A, CTRL);
        pressOnFocusOwner(scene, KeyCode.S, CTRL);
        assertEquals(List.of(0, 0, 1, 1),
                List.of(selectAllInWindow.runs(), selectAllInTab.runs(), selectAllOnCanvas.runs(), saveInTab.runs()));

        inTabOnly.requestFocus();
        pressOnFocusOwner(scene, KeyCode.A, CTRL);
        assertEquals(List.of(0, 1, 1),
                List.of(selectAllInWindow.runs(), selectAllInTab.runs(), selectAllOnCanvas.runs()));
    }

    @Test
    @DisplayName("a disabled command passes its key on: to the enclosing context's command, then to the window's")
    void disabledCommandPassesItsKeyOutward() {
        Button onCanvas = new Button("node on the canvas");
        StackPane canvas = new StackPane(onCanvas);
        StackPane workflowTab = new StackPane(canvas);
        Scene scene = themedScene(new VBox(workflowTab));
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup edit = registry.addGroup("edit", "Edit");
        CommandContext tabContext = registry.addContext("workflow", "Workflow", workflowTab);
        CommandContext canvasContext = registry.addContext("canvas", "Canvas", canvas);
        RunCounter pasteInWindow = new RunCounter();
        RunCounter pasteInTab = new RunCounter();
        RunCounter pasteOnCanvas = new RunCounter();
        Command window = registry.addCommand(edit, "edit.paste", "Paste", Shortcut.of(KeyCode.V, CTRL),
                pasteInWindow);
        Command tab = registry.addCommand(edit, tabContext, "workflow.paste", "Paste Into Workflow",
                Shortcut.of(KeyCode.V, CTRL), pasteInTab);
        Command onTheCanvas = registry.addCommand(edit, canvasContext, "canvas.paste", "Paste Nodes",
                Shortcut.of(KeyCode.V, CTRL), pasteOnCanvas);
        onCanvas.requestFocus();

        onTheCanvas.setDisabled(true);
        pressOnFocusOwner(scene, KeyCode.V, CTRL);
        assertEquals(List.of(0, 1, 0), List.of(pasteInWindow.runs(), pasteInTab.runs(), pasteOnCanvas.runs()));

        tab.setDisabled(true);
        pressOnFocusOwner(scene, KeyCode.V, CTRL);
        assertEquals(List.of(1, 1, 0), List.of(pasteInWindow.runs(), pasteInTab.runs(), pasteOnCanvas.runs()));

        window.setDisabled(true);
        pressOnFocusOwner(scene, KeyCode.V, CTRL);
        assertEquals(List.of(1, 1, 0), List.of(pasteInWindow.runs(), pasteInTab.runs(), pasteOnCanvas.runs()));

        onTheCanvas.setDisabled(false);
        pressOnFocusOwner(scene, KeyCode.V, CTRL);
        assertEquals(List.of(1, 1, 1), List.of(pasteInWindow.runs(), pasteInTab.runs(), pasteOnCanvas.runs()));
    }

    // --- Fixed commands ------------------------------------------------------------------------------

    @Test
    @DisplayName("a fixed command documents a component's key: shown with it, without an action, its key never changes")
    void fixedCommand() {
        StackPane documentArea = new StackPane();
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(documentArea)));
        CommandGroup edit = registry.addGroup("edit", "Edit");
        CommandContext document = registry.addContext("document", "Document", documentArea);
        Command save = registry.addCommand(edit, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), new RunCounter());

        Command copy = registry.addFixedCommand(edit, document, "document.copy", "Copy", Shortcut.of(KeyCode.C, CTRL),
                null);
        Command selectAll = registry.addFixedCommand(edit, document, "document.selectAll", "Select All",
                Shortcut.of(KeyCode.A, CTRL), new RunCounter());

        assertTrue(copy.isFixed());
        assertTrue(selectAll.isFixed());
        assertFalse(save.isFixed());
        assertEquals(List.of(save, copy, selectAll), registry.getCommands());
        assertSame(document, copy.getContext());
        assertEquals(Shortcut.of(KeyCode.C, CTRL), copy.getDefaultShortcut());
        assertEquals("Ctrl+C", copy.getShortcutDisplayText());
        assertThrows(IllegalStateException.class, () -> copy.setShortcut(Shortcut.of(KeyCode.C, CTRL, SHIFT)));
        assertThrows(IllegalStateException.class, () -> selectAll.setShortcut(null));
        assertEquals(Shortcut.of(KeyCode.C, CTRL), copy.getShortcut());
        assertEquals(Shortcut.of(KeyCode.A, CTRL), selectAll.getShortcut());
        // Without an action there is nothing a control could run.
        assertThrows(IllegalStateException.class, () -> copy.bind(new MenuItem("Copy")));
        assertThrows(IllegalStateException.class, () -> copy.bind(new Button("Copy")));
        selectAll.bind(new MenuItem("Select All"));

        assertThrows(NullPointerException.class,
                () -> registry.addFixedCommand(edit, document, "document.find", "Find", null, null));
        assertThrows(NullPointerException.class, () -> registry.addFixedCommand(edit, null, "document.find", "Find",
                Shortcut.of(KeyCode.F, CTRL), null));
        assertThrows(IllegalArgumentException.class, () -> registry.addFixedCommand(edit, document, "document.copy",
                "Copy", Shortcut.of(KeyCode.C, CTRL), null));
        assertEquals(3, registry.getCommands().size());
    }

    @Test
    @DisplayName("a fixed command without an action takes no key: its component handles it, else it goes on outward")
    void fixedCommandWithoutActionTakesNoKey() {
        Button copyingComponent = new Button("a view that copies its selection");
        copyingComponent.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.C && event.isShortcutDown()) {
                event.consume();
            }
        });
        Button plainComponent = new Button("a view that does not copy");
        StackPane documentArea = new StackPane(new VBox(copyingComponent, plainComponent));
        Scene scene = themedScene(new VBox(documentArea));
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup edit = registry.addGroup("edit", "Edit");
        CommandContext document = registry.addContext("document", "Document", documentArea);
        registry.addFixedCommand(edit, document, "document.copy", "Copy", Shortcut.of(KeyCode.C, CTRL), null);
        RunCounter applicationsAccelerator = new RunCounter();
        scene.getAccelerators().put(Shortcut.of(KeyCode.C, CTRL).toKeyCombination(), applicationsAccelerator);

        plainComponent.requestFocus();
        pressOnFocusOwner(scene, KeyCode.C, CTRL);
        // The registry consumed nothing: the key reached the scene's accelerators.
        assertEquals(1, applicationsAccelerator.runs());

        RunCounter copyInWindow = new RunCounter();
        registry.addCommand(edit, "edit.copy", "Copy", Shortcut.of(KeyCode.C, CTRL), copyInWindow);
        copyingComponent.requestFocus();
        pressOnFocusOwner(scene, KeyCode.C, CTRL);
        assertEquals(0, copyInWindow.runs());
        plainComponent.requestFocus();
        pressOnFocusOwner(scene, KeyCode.C, CTRL);
        assertEquals(1, copyInWindow.runs());
        assertEquals(1, applicationsAccelerator.runs());
    }

    @Test
    @DisplayName("a fixed command with an action runs on its key like any other")
    void fixedCommandWithActionRuns() {
        Button inDocument = new Button("in the document");
        StackPane documentArea = new StackPane(inDocument);
        Scene scene = themedScene(new VBox(documentArea));
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup edit = registry.addGroup("edit", "Edit");
        RunCounter selectAllInWindow = new RunCounter();
        RunCounter selectAllInDocument = new RunCounter();
        registry.addCommand(edit, "edit.selectAll", "Select All", Shortcut.of(KeyCode.A, CTRL), selectAllInWindow);
        registry.addFixedCommand(edit, registry.addContext("document", "Document", documentArea),
                "document.selectAll", "Select All Text", Shortcut.of(KeyCode.A, CTRL), selectAllInDocument);

        inDocument.requestFocus();
        pressOnFocusOwner(scene, KeyCode.A, CTRL);

        assertEquals(0, selectAllInWindow.runs());
        assertEquals(1, selectAllInDocument.runs());
    }

    // --- Bound menu items ----------------------------------------------------------------------------

    @Test
    @DisplayName("a menu item of a context's command shows its key, which runs the command only inside the context")
    void menuItemOfAContextCommand() throws InterruptedException {
        RunCounter rename = new RunCounter();
        onFxThread(() -> {
            Button onCanvas = new Button("node on the canvas");
            StackPane canvas = new StackPane(onCanvas);
            Button outside = new Button("outside");
            VBox root = new VBox(canvas, outside);
            Scene scene = themedScene(root);
            CommandRegistry registry = new CommandRegistry(scene);
            Command command = registry.addCommand(registry.addGroup("edit", "Edit"),
                    registry.addContext("canvas", "Canvas", canvas), "canvas.rename", "Rename Node",
                    Shortcut.of(KeyCode.F2), rename);
            MenuItem item = new MenuItem("Rename Node");

            command.bind(item);
            addMenuBar(root, item);
            assertEquals("F2", item.getAccelerator().getDisplayText());

            outside.requestFocus();
            pressOnFocusOwner(scene, KeyCode.F2);
            assertEquals(0, rename.runs());

            // Inside, the key runs the command once, not a second time through the item's accelerator.
            onCanvas.requestFocus();
            pressOnFocusOwner(scene, KeyCode.F2);
            assertEquals(1, rename.runs());

            // A click on the item is a choice of the command, as a click on a button is: it runs wherever the focus is.
            outside.requestFocus();
            item.fire();
            assertEquals(2, rename.runs());
        });
    }

    @Test
    @DisplayName("once a menu bar left the window, its items' keys are the window's again: another accelerator runs")
    void menuItemKeysLeaveWithTheirMenuBar() throws InterruptedException {
        RunCounter rename = new RunCounter();
        RunCounter save = new RunCounter();
        RunCounter renameAccelerator = new RunCounter();
        RunCounter saveAccelerator = new RunCounter();
        onFxThread(() -> {
            Button onCanvas = new Button("node on the canvas");
            StackPane canvas = new StackPane(onCanvas);
            Button outside = new Button("outside");
            VBox root = new VBox(canvas, outside);
            Scene scene = themedScene(root);
            CommandRegistry registry = new CommandRegistry(scene);
            CommandGroup edit = registry.addGroup("edit", "Edit");
            Command renameCommand = registry.addCommand(edit, registry.addContext("canvas", "Canvas", canvas),
                    "canvas.rename", "Rename Node", Shortcut.of(KeyCode.F2), rename);
            Command saveCommand = registry.addCommand(edit, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), save);
            saveCommand.setDisabled(true);
            MenuItem renameItem = new MenuItem("Rename Node");
            MenuItem saveItem = new MenuItem("Save");
            renameCommand.bind(renameItem);
            saveCommand.bind(saveItem);
            addMenuBar(root, renameItem, saveItem);

            // The menu bar leaves the window, as a page with a menu of its own does, and takes its accelerators.
            root.getChildren().remove(0);
            assertNull(scene.getAccelerators().get(renameItem.getAccelerator()));
            scene.getAccelerators().put(Shortcut.of(KeyCode.F2).toKeyCombination(), renameAccelerator);
            scene.getAccelerators().put(Shortcut.of(KeyCode.S, CTRL).toKeyCombination(), saveAccelerator);
            outside.requestFocus();
            pressOnFocusOwner(scene, KeyCode.F2);
            pressOnFocusOwner(scene, KeyCode.S, CTRL);

            assertEquals(List.of(1, 1), List.of(renameAccelerator.runs(), saveAccelerator.runs()));
            assertEquals(List.of(0, 0), List.of(rename.runs(), save.runs()));
        });
    }

    @Test
    @DisplayName("while a menu bar is in the window, the key of its item runs neither the command outside its context"
            + " nor an accelerator the application put on it before")
    void menuItemKeyWhileItsMenuBarIsIn() throws InterruptedException {
        RunCounter rename = new RunCounter();
        RunCounter applicationsAccelerator = new RunCounter();
        onFxThread(() -> {
            Button onCanvas = new Button("node on the canvas");
            StackPane canvas = new StackPane(onCanvas);
            Button outside = new Button("outside");
            VBox root = new VBox(canvas, outside);
            Scene scene = themedScene(root);
            CommandRegistry registry = new CommandRegistry(scene);
            Command command = registry.addCommand(registry.addGroup("edit", "Edit"),
                    registry.addContext("canvas", "Canvas", canvas), "canvas.rename", "Rename Node",
                    Shortcut.of(KeyCode.F2), rename);
            // The application's own accelerator first: the menu bar's item takes its place in the scene.
            scene.getAccelerators().put(Shortcut.of(KeyCode.F2).toKeyCombination(), applicationsAccelerator);
            MenuItem item = new MenuItem("Rename Node");
            command.bind(item);
            addMenuBar(root, item);

            outside.requestFocus();
            pressOnFocusOwner(scene, KeyCode.F2);

            assertEquals(List.of(0, 0), List.of(rename.runs(), applicationsAccelerator.runs()));
        });
    }

    // --- AltGr in text inputs ------------------------------------------------------------------------

    @Test
    @DisplayName("typing with AltGr (Ctrl+Alt on Windows) in a text input runs no command on Ctrl+Alt, nor its menu item")
    void altGrInATextInputRunsNoCommand() throws InterruptedException {
        RunCounter quickOpen = new RunCounter();
        RunCounter fold = new RunCounter();
        RunCounter rerun = new RunCounter();
        onFxThread(() -> {
            TextField field = new TextField();
            TextArea area = new TextArea();
            Button button = new Button("not a text input");
            VBox root = new VBox(field, area, button);
            Scene scene = themedScene(root);
            CommandRegistry registry = new CommandRegistry(scene);
            CommandGroup edit = registry.addGroup("edit", "Edit");
            // @ on a German keyboard, { on a German one, @ on a Swiss one with Shift held: AltGr+Q, AltGr+7, AltGr+2.
            Command quickOpenCommand = registry.addCommand(edit, "file.quickOpen", "Quick Open",
                    Shortcut.of(KeyCode.Q, CTRL, ALT), quickOpen);
            registry.addCommand(edit, "edit.fold", "Fold", Shortcut.of(KeyCode.DIGIT7, CTRL, ALT), fold);
            registry.addCommand(edit, "run.rerun", "Rerun", Shortcut.of(KeyCode.F5, CTRL, ALT), rerun);
            MenuItem quickOpenItem = new MenuItem("Quick Open");
            quickOpenCommand.bind(quickOpenItem);
            addMenuBar(root, quickOpenItem);

            for (Node textInput : List.of(field, area)) {
                textInput.requestFocus();
                pressOnFocusOwner(scene, KeyCode.Q, CTRL, ALT);
                pressOnFocusOwner(scene, KeyCode.DIGIT7, CTRL, ALT);
                // A function key types no character: its command runs.
                pressOnFocusOwner(scene, KeyCode.F5, CTRL, ALT);
            }
            assertEquals(List.of(0, 0, 2), List.of(quickOpen.runs(), fold.runs(), rerun.runs()));

            button.requestFocus();
            pressOnFocusOwner(scene, KeyCode.Q, CTRL, ALT);
            pressOnFocusOwner(scene, KeyCode.DIGIT7, CTRL, ALT);
            assertEquals(List.of(1, 1), List.of(quickOpen.runs(), fold.runs()));
        });
    }

    /**
     * Puts a menu bar with a menu of items at the top of a window's root. The menu bar puts the accelerators its items
     * have by then into the scene, as a menu bar built after its items does in an application, on the toolkit's thread
     * only. (An accelerator an item gets only later reaches the scene without its action.)
     */
    private static void addMenuBar(VBox root, MenuItem... items) {
        root.getChildren().add(0, new MenuBar(new Menu("Menu", null, items)));
        root.applyCss();
        root.layout();
        for (MenuItem item : items) {
            assertNotNull(root.getScene().getAccelerators().get(item.getAccelerator()),
                    "the accelerator of " + item.getText() + " is not in the scene");
        }
    }

    /**
     * Presses a key where the platform sends it: on the node with the focus.
     */
    private static void pressOnFocusOwner(Scene scene, KeyCode code, Shortcut.Modifier... modifiers) {
        Node focusOwner = scene.getFocusOwner();
        assertNotNull(focusOwner, "nothing has the focus");
        focusOwner.fireEvent(keyPressed(code, modifiers));
    }
}
