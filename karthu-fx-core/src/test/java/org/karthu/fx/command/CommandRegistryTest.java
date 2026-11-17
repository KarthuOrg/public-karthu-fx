package org.karthu.fx.command;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.CommandTestSupport.RunCounter;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * The command registry through its public API, without a window: commands registered in groups, fired by key presses
 * sent as events in a themed scene that is never shown.
 */
class CommandRegistryTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    // --- Groups and commands -------------------------------------------------------------------------

    @Test
    @DisplayName("groups and commands keep the order they were added in, with their ids, names, groups and keys")
    void groupsAndCommandsInDisplayOrder() {
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox()));
        CommandGroup file = registry.addGroup("file", "File");
        CommandGroup view = registry.addGroup("view", "View");
        Command zoomIn = registry.addCommand(view, "view.zoomIn", "Zoom In", Shortcut.of(KeyCode.ADD, CTRL),
                new RunCounter());
        Command open = registry.addCommand(file, "file.open", "Open", Shortcut.of(KeyCode.O, CTRL),
                new RunCounter());
        Command exit = registry.addCommand(file, "file.exit", "Exit", null, new RunCounter());

        assertEquals(List.of(file, view), registry.getGroups());
        assertEquals(List.of(zoomIn, open, exit), registry.getCommands());
        assertEquals("file", file.getId());
        assertEquals("File", file.getName());
        assertEquals("file.open", open.getId());
        assertEquals("Open", open.getName());
        assertSame(file, open.getGroup());
        assertSame(view, zoomIn.getGroup());
        assertEquals(Shortcut.of(KeyCode.O, CTRL), open.getDefaultShortcut());
        assertEquals(Shortcut.of(KeyCode.O, CTRL), open.getShortcut());
        assertNull(exit.getDefaultShortcut());
        assertNull(exit.getShortcut());
        assertThrows(UnsupportedOperationException.class, () -> registry.getGroups().add(file));
        assertThrows(UnsupportedOperationException.class, () -> registry.getCommands().remove(exit));
    }

    @Test
    @DisplayName("the names of groups and commands are properties, so that an application translates them")
    void namesAreProperties() {
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox()));
        CommandGroup file = registry.addGroup("file", "File");
        Command save = registry.addCommand(file, "file.save", "Save", null, new RunCounter());

        file.nameProperty().set("Datei");
        save.setName("Speichern");

        assertEquals("Datei", file.getName());
        assertEquals("Speichern", save.nameProperty().get());
        assertEquals("file", file.getId());
        assertEquals("file.save", save.getId());
    }

    @Test
    @DisplayName("one registry per scene: a second registry for the same scene is refused")
    void oneRegistryPerScene() {
        Scene scene = themedScene(new VBox());
        new CommandRegistry(scene);

        assertThrows(IllegalArgumentException.class, () -> new CommandRegistry(scene));
        assertThrows(NullPointerException.class, () -> new CommandRegistry(null));
        new CommandRegistry(themedScene(new VBox()));
    }

    @Test
    @DisplayName("ids are unique in a registry, not blank, and a command goes into a group of its own registry")
    void idsAndGroupsAreChecked() {
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox()));
        CommandRegistry other = new CommandRegistry(themedScene(new VBox()));
        CommandGroup file = registry.addGroup("file", "File");
        CommandGroup foreign = other.addGroup("view", "View");
        registry.addCommand(file, "file.save", "Save", null, new RunCounter());

        assertThrows(IllegalArgumentException.class, () -> registry.addGroup("file", "Other"));
        assertThrows(IllegalArgumentException.class, () -> registry.addGroup(" ", "Blank"));
        assertThrows(IllegalArgumentException.class,
                () -> registry.addCommand(file, "file.save", "Save As", null, new RunCounter()));
        assertThrows(IllegalArgumentException.class,
                () -> registry.addCommand(file, "", "Empty", null, new RunCounter()));
        assertThrows(IllegalArgumentException.class,
                () -> registry.addCommand(foreign, "view.zoom", "Zoom", null, new RunCounter()));
        // The same id in another registry is another window's command.
        other.addCommand(foreign, "file.save", "Save", null, new RunCounter());

        assertThrows(NullPointerException.class, () -> registry.addGroup(null, "File"));
        assertThrows(NullPointerException.class, () -> registry.addGroup("edit", null));
        assertThrows(NullPointerException.class,
                () -> registry.addCommand(null, "file.open", "Open", null, new RunCounter()));
        assertThrows(NullPointerException.class,
                () -> registry.addCommand(file, null, "Open", null, new RunCounter()));
        assertThrows(NullPointerException.class,
                () -> registry.addCommand(file, "file.open", null, null, new RunCounter()));
        assertThrows(NullPointerException.class, () -> registry.addCommand(file, "file.open", "Open", null, null));
        assertEquals(1, registry.getCommands().size());
        assertEquals(1, registry.getGroups().size());
    }

    // --- Firing --------------------------------------------------------------------------------------

    @Test
    @DisplayName("a command runs once when its key is pressed in the window")
    void commandRunsOnItsKey() {
        Label content = new Label("content");
        Scene scene = themedScene(new VBox(content));
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup file = registry.addGroup("file", "File");
        RunCounter save = new RunCounter();
        registry.addCommand(file, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), save);

        content.fireEvent(keyPressed(KeyCode.S, CTRL));

        assertEquals(1, save.runs());
    }

    @Test
    @DisplayName("the key runs the command wherever the focus is in the window")
    void commandRunsWhereverTheFocusIs() {
        Button deep = new Button("deep");
        TextField field = new TextField();
        Scene scene = themedScene(new VBox(field, new HBox(new VBox(deep))));
        CommandRegistry registry = new CommandRegistry(scene);
        RunCounter reload = new RunCounter();
        registry.addCommand(registry.addGroup("file", "File"), "file.reload", "Reload", Shortcut.of(KeyCode.F5),
                reload);

        deep.requestFocus();
        assertSame(deep, scene.getFocusOwner());
        scene.getFocusOwner().fireEvent(keyPressed(KeyCode.F5));
        field.requestFocus();
        assertSame(field, scene.getFocusOwner());
        scene.getFocusOwner().fireEvent(keyPressed(KeyCode.F5));

        assertEquals(2, reload.runs());
    }

    @Test
    @DisplayName("another key, or the key with other modifiers, does not run the command")
    void otherKeysDoNotRunTheCommand() {
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        RunCounter save = new RunCounter();
        registry.addCommand(registry.addGroup("file", "File"), "file.save", "Save", Shortcut.of(KeyCode.S, CTRL),
                save);

        content.fireEvent(keyPressed(KeyCode.S));
        content.fireEvent(keyPressed(KeyCode.D, CTRL));
        content.fireEvent(keyPressed(KeyCode.S, CTRL, SHIFT));
        content.fireEvent(keyPressed(KeyCode.S, CTRL, ALT));
        content.fireEvent(keyPressed(KeyCode.S, ALT));

        assertEquals(0, save.runs());
    }

    @Test
    @DisplayName("a disabled command does not run on its key; enabled again, it does")
    void disabledCommandDoesNotRun() {
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        RunCounter save = new RunCounter();
        Command command = registry.addCommand(registry.addGroup("file", "File"), "file.save", "Save",
                Shortcut.of(KeyCode.S, CTRL), save);

        assertFalse(command.isDisabled());
        command.setDisabled(true);
        content.fireEvent(keyPressed(KeyCode.S, CTRL));
        assertEquals(0, save.runs());

        command.disabledProperty().set(false);
        content.fireEvent(keyPressed(KeyCode.S, CTRL));
        assertEquals(1, save.runs());
    }

    @Test
    @DisplayName("a focused text field keeps its editing keys Ctrl+A, C, X, V and Z: the window's commands on them wait")
    void textFieldKeepsItsEditingKeys() {
        StandInTextField field = new StandInTextField("hello");
        Label content = new Label("content");
        Scene scene = themedScene(new VBox(field, content));
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup edit = registry.addGroup("edit", "Edit");
        List<KeyCode> editingKeys = List.of(KeyCode.A, KeyCode.C, KeyCode.X, KeyCode.V, KeyCode.Z);
        List<RunCounter> commands = new ArrayList<>();
        for (KeyCode key : editingKeys) {
            RunCounter command = new RunCounter();
            commands.add(command);
            registry.addCommand(edit, "edit." + key.getName(), key.getName(), Shortcut.of(key, CTRL), command);
        }
        field.appendText(" world");
        field.requestFocus();

        for (KeyCode key : editingKeys) {
            scene.getFocusOwner().fireEvent(keyPressed(key, CTRL));
            if (key == KeyCode.A) {
                assertEquals("hello world", field.getSelectedText());
            }
        }

        assertEquals(List.of(0, 0, 0, 0, 0), runs(commands));
        assertEquals(1, field.copies);
        assertEquals(1, field.cuts);
        assertEquals(1, field.pastes);
        assertEquals("hello", field.getText());
        // Outside the text field the keys are the window's.
        for (KeyCode key : editingKeys) {
            content.fireEvent(keyPressed(key, CTRL));
        }
        assertEquals(List.of(1, 1, 1, 1, 1), runs(commands));
    }

    // --- Display text -------------------------------------------------------------------------------

    @Test
    @DisplayName("the display text of a command's key: Ctrl+S, empty without a key, and it follows the key")
    void shortcutDisplayText() {
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox()));
        CommandGroup file = registry.addGroup("file", "File");
        Command save = registry.addCommand(file, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), new RunCounter());
        Command show = registry.addCommand(file, "file.show", "Show in Explorer", null, new RunCounter());
        List<String> seen = new ArrayList<>();
        save.shortcutDisplayTextProperty().addListener((observable, before, now) -> seen.add(now));

        assertEquals("Ctrl+S", save.getShortcutDisplayText());
        assertEquals("", show.getShortcutDisplayText());
        assertNull(show.getShortcut());

        save.setShortcut(Shortcut.of(KeyCode.S, CTRL, ALT, SHIFT));
        save.setShortcut(null);
        show.setShortcut(Shortcut.of(KeyCode.F5));

        assertEquals(List.of("Ctrl+Alt+Shift+S", ""), seen);
        assertEquals("F5", show.shortcutDisplayTextProperty().get());
    }

    // --- Bound controls ------------------------------------------------------------------------------

    @Test
    @DisplayName("a bound menu item runs the command, shows its key as its accelerator and its disabled state")
    void boundMenuItem() {
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox()));
        RunCounter save = new RunCounter();
        Command command = registry.addCommand(registry.addGroup("file", "File"), "file.save", "Save",
                Shortcut.of(KeyCode.S, CTRL), save);
        MenuItem item = new MenuItem("Save");

        command.bind(item);

        assertEquals("Ctrl+S", item.getAccelerator().getDisplayText());
        assertTrue(item.getAccelerator().match(keyPressed(KeyCode.S, CTRL)));
        item.fire();
        assertEquals(1, save.runs());
        assertFalse(item.isDisable());
        command.setDisabled(true);
        assertTrue(item.isDisable());
        // A menu item fires from code even while disabled: the command still does not run.
        item.fire();
        assertEquals(1, save.runs());
        command.setDisabled(false);
        assertFalse(item.isDisable());
    }

    @Test
    @DisplayName("a bound menu item's accelerator follows the command's key, and is none while the command has none")
    void menuItemFollowsTheKey() {
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox()));
        Command command = registry.addCommand(registry.addGroup("file", "File"), "file.save", "Save",
                Shortcut.of(KeyCode.S, CTRL), new RunCounter());
        MenuItem item = new MenuItem("Save");
        command.bind(item);

        command.setShortcut(Shortcut.of(KeyCode.S, CTRL, SHIFT));
        assertEquals("Ctrl+Shift+S", item.getAccelerator().getDisplayText());
        command.setShortcut(null);
        assertNull(item.getAccelerator());
        command.setShortcut(Shortcut.of(KeyCode.F12));
        assertEquals("F12", item.getAccelerator().getDisplayText());
    }

    @Test
    @DisplayName("a bound button runs the command and shows its disabled state")
    void boundButton() {
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox()));
        RunCounter reload = new RunCounter();
        Command command = registry.addCommand(registry.addGroup("file", "File"), "file.reload", "Reload",
                Shortcut.of(KeyCode.F5), reload);
        Button button = new Button("Reload");

        command.bind(button);

        button.fire();
        assertEquals(1, reload.runs());
        assertFalse(button.isDisable());
        command.setDisabled(true);
        assertTrue(button.isDisable());
        button.fire();
        assertEquals(1, reload.runs());
        command.setDisabled(false);
        assertFalse(button.isDisable());
        button.fire();
        assertEquals(2, reload.runs());
    }

    @Test
    @DisplayName("with the bound item in the window's menu bar the key runs the command once; disabled, not at all")
    void menuBarAcceleratorRunsTheCommandOnce() throws InterruptedException {
        RunCounter save = new RunCounter();
        // A menu bar builds its menus, and puts its items' accelerators into the scene, on the toolkit's thread only.
        onFxThread(() -> {
            Label content = new Label("content");
            MenuItem item = new MenuItem("Save");
            Scene scene = themedScene(new VBox(new MenuBar(new Menu("File", null, item)), content));
            CommandRegistry registry = new CommandRegistry(scene);
            Command command = registry.addCommand(registry.addGroup("file", "File"), "file.save", "Save",
                    Shortcut.of(KeyCode.S, CTRL), save);

            command.bind(item);
            // The item's accelerator is in the scene: the key could run the command twice.
            assertTrue(scene.getAccelerators().containsKey(item.getAccelerator()));
            content.fireEvent(keyPressed(KeyCode.S, CTRL));
            assertEquals(1, save.runs());

            command.setDisabled(true);
            content.fireEvent(keyPressed(KeyCode.S, CTRL));
            assertEquals(1, save.runs());
        });
    }

    private static List<Integer> runs(List<RunCounter> counters) {
        List<Integer> runs = new ArrayList<>();
        for (RunCounter counter : counters) {
            runs.add(counter.runs());
        }
        return runs;
    }

    /**
     * A text field that counts cut, copy and paste instead of using the clipboard: no test reads or writes the
     * clipboard of the machine it runs on.
     */
    private static final class StandInTextField extends TextField {

        private int cuts;
        private int copies;
        private int pastes;

        StandInTextField(String text) {
            super(text);
        }

        @Override
        public void cut() {
            cuts++;
        }

        @Override
        public void copy() {
            copies++;
        }

        @Override
        public void paste() {
            pastes++;
        }
    }
}
