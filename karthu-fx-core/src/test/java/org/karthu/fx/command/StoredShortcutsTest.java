package org.karthu.fx.command;

import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.CommandTestSupport.RunCounter;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.CommandTestSupport.keyPressed;
import static org.karthu.fx.command.CommandTestSupport.themedScene;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;

/**
 * The user's keys kept by the application: the registry gives out the keys that differ from their defaults as texts,
 * reports every change, takes such texts back tolerantly and resets every key to its default.
 */
class StoredShortcutsTest {

    private CommandRegistry registry;

    private CommandGroup file;

    private Command open;

    private Command save;

    private Command exit;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    @BeforeEach
    void createRegistry() {
        registry = new CommandRegistry(themedScene(new VBox()));
        file = registry.addGroup("file", "File");
        open = registry.addCommand(file, "file.open", "Open", Shortcut.of(KeyCode.O, CTRL), new RunCounter());
        save = registry.addCommand(file, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), new RunCounter());
        exit = registry.addCommand(file, "file.exit", "Exit", null, new RunCounter());
    }

    @Test
    @DisplayName("the registry gives out only the keys that differ from their defaults: a changed key as its text,"
            + " a removed key as an empty text, in the order of the commands")
    void givesOutTheChangedKeys() {
        assertEquals(Map.of(), registry.getChangedShortcuts());

        exit.setShortcut(Shortcut.of(KeyCode.Q, CTRL));
        save.setShortcut(Shortcut.of(KeyCode.S, CTRL, SHIFT));
        open.setShortcut(null);

        assertEquals(Map.of("file.open", "", "file.save", "Ctrl+Shift+S", "file.exit", "Ctrl+Q"),
                registry.getChangedShortcuts());
        assertEquals(List.of("file.open", "file.save", "file.exit"),
                new ArrayList<>(registry.getChangedShortcuts().keySet()));

        save.setShortcut(Shortcut.of(KeyCode.S, CTRL));

        assertEquals(Map.of("file.open", "", "file.exit", "Ctrl+Q"), registry.getChangedShortcuts());
    }

    @Test
    @DisplayName("a map taken back sets the keys it names, an empty text removing a key, gives them out again, and"
            + " puts every key it does not name back to its default")
    void takesTheChangedKeysBack() {
        Map<String, String> stored = Map.of("file.open", "", "file.save", "Ctrl+Shift+S", "file.exit", "Ctrl+Q");

        registry.setChangedShortcuts(stored);

        assertNull(open.getShortcut());
        assertEquals(Shortcut.of(KeyCode.S, CTRL, SHIFT), save.getShortcut());
        assertEquals(Shortcut.of(KeyCode.Q, CTRL), exit.getShortcut());
        assertEquals("Ctrl+Shift+S", save.getShortcutDisplayText());
        assertEquals(stored, registry.getChangedShortcuts());

        registry.setChangedShortcuts(Map.of("file.exit", "F10"));

        assertEquals(Shortcut.of(KeyCode.O, CTRL), open.getShortcut());
        assertEquals(Shortcut.of(KeyCode.S, CTRL), save.getShortcut());
        assertEquals(Shortcut.of(KeyCode.F10), exit.getShortcut());
        assertEquals(Map.of("file.exit", "F10"), registry.getChangedShortcuts());
    }

    @Test
    @DisplayName("a map taken back skips and logs an unknown id, an unreadable text, a key the user may not record and"
            + " an entry for a fixed command: their defaults apply, and the other entries apply")
    void skipsAndLogsBadEntries() {
        RecordingLogger log = new RecordingLogger();
        registry.setLogger(log);
        CommandContext document = registry.addContext("document", "Document", new VBox());
        Command copy = registry.addFixedCommand(file, document, "edit.copy", "Copy", Shortcut.of(KeyCode.C, CTRL),
                null);
        Map<String, String> stored = new LinkedHashMap<>();
        stored.put("file.gone", "Ctrl+G");
        stored.put("file.open", "Ctrl+Nothing");
        stored.put("file.save", "S");
        stored.put("edit.copy", "Ctrl+Shift+C");
        stored.put("file.exit", "Ctrl+Q");

        registry.setChangedShortcuts(stored);

        assertEquals(Shortcut.of(KeyCode.O, CTRL), open.getShortcut());
        assertEquals(Shortcut.of(KeyCode.S, CTRL), save.getShortcut());
        assertEquals(Shortcut.of(KeyCode.C, CTRL), copy.getShortcut());
        assertEquals(Shortcut.of(KeyCode.Q, CTRL), exit.getShortcut());
        assertEquals(Map.of("file.exit", "Ctrl+Q"), registry.getChangedShortcuts());
        assertEquals(4, log.records.size(), String.valueOf(log.records));
        List<String> named = List.of("file.gone", "file.open", "file.save", "edit.copy");
        for (int index = 0; index < named.size(); index++) {
            RecordingLogger.Record record = log.records.get(index);
            assertEquals(Level.WARN, record.level());
            assertTrue(record.message().contains(named.get(index)), record.message());
        }
        assertTrue(log.records.get(1).message().contains("Ctrl+Nothing"), log.records.get(1).message());
    }

    @Test
    @DisplayName("an entry without an id or without a text is skipped and logged too; no map at all is refused")
    void skipsEntriesWithNull() {
        RecordingLogger log = new RecordingLogger();
        registry.setLogger(log);
        Map<String, String> stored = new HashMap<>();
        stored.put(null, "Ctrl+N");
        stored.put("file.save", null);
        stored.put("file.exit", "Ctrl+Q");

        registry.setChangedShortcuts(stored);

        assertEquals(Shortcut.of(KeyCode.S, CTRL), save.getShortcut());
        assertEquals(Map.of("file.exit", "Ctrl+Q"), registry.getChangedShortcuts());
        assertEquals(2, log.records.size(), String.valueOf(log.records));
        assertThrows(NullPointerException.class, () -> registry.setChangedShortcuts(null));
    }

    @Test
    @DisplayName("a stored key on the default of another command in the same context stays, and the other command"
            + " has no key and counts as changed, so the map reads back the same")
    void storedKeyTakesTheDefaultOfAnother() {
        registry.setChangedShortcuts(Map.of("file.exit", "Ctrl+S"));

        assertEquals(Shortcut.of(KeyCode.S, CTRL), exit.getShortcut());
        assertNull(save.getShortcut());
        assertEquals("", save.getShortcutDisplayText());
        assertEquals(Shortcut.of(KeyCode.O, CTRL), open.getShortcut());
        assertEquals(Map.of("file.save", "", "file.exit", "Ctrl+S"), registry.getChangedShortcuts());

        registry.setChangedShortcuts(registry.getChangedShortcuts());

        assertEquals(Shortcut.of(KeyCode.S, CTRL), exit.getShortcut());
        assertNull(save.getShortcut());
        assertEquals(Map.of("file.save", "", "file.exit", "Ctrl+S"), registry.getChangedShortcuts());
    }

    @Test
    @DisplayName("a stored key takes no key from a command of another context, nor from a fixed command, nor from"
            + " another stored key")
    void storedKeyTakesNothingElse() {
        CommandContext document = registry.addContext("document", "Document", new VBox());
        Command bold = registry.addCommand(file, document, "doc.bold", "Bold", Shortcut.of(KeyCode.B, CTRL),
                new RunCounter());
        Command find = registry.addCommand(file, document, "doc.find", "Find", Shortcut.of(KeyCode.F, CTRL),
                new RunCounter());
        Command copy = registry.addFixedCommand(file, document, "edit.copy", "Copy", Shortcut.of(KeyCode.C, CTRL),
                null);

        registry.setChangedShortcuts(Map.of("file.exit", "Ctrl+B", "doc.find", "Ctrl+C", "file.open", "Ctrl+K",
                "file.save", "Ctrl+K"));

        assertEquals(Shortcut.of(KeyCode.B, CTRL), exit.getShortcut());
        assertEquals(Shortcut.of(KeyCode.B, CTRL), bold.getShortcut());
        assertEquals(Shortcut.of(KeyCode.C, CTRL), find.getShortcut());
        assertEquals(Shortcut.of(KeyCode.C, CTRL), copy.getShortcut());
        assertEquals(Shortcut.of(KeyCode.K, CTRL), open.getShortcut());
        assertEquals(Shortcut.of(KeyCode.K, CTRL), save.getShortcut());
    }

    @Test
    @DisplayName("the application hears every change of the changed keys once, with the new map: a key changed, a map"
            + " taken back; nothing when nothing changed")
    void notifiesEveryChange() {
        List<Map<String, String>> heard = new ArrayList<>();
        registry.changedShortcutsProperty().addListener((observable, before, now) -> heard.add(now));

        save.setShortcut(Shortcut.of(KeyCode.S, CTRL, SHIFT));

        assertEquals(List.of(Map.of("file.save", "Ctrl+Shift+S")), heard);

        heard.clear();
        registry.setChangedShortcuts(Map.of("file.open", "", "file.exit", "Ctrl+S"));

        assertEquals(List.of(Map.of("file.open", "", "file.save", "", "file.exit", "Ctrl+S")), heard);
        assertEquals(heard.get(0), registry.getChangedShortcuts());
        assertEquals(heard.get(0), registry.changedShortcutsProperty().get());

        heard.clear();
        registry.setChangedShortcuts(Map.of("file.open", "", "file.exit", "Ctrl+S"));
        exit.setShortcut(Shortcut.of(KeyCode.S, CTRL));

        assertEquals(List.of(), heard);
    }

    @Test
    @DisplayName("resetting puts every key back to its default, and the application hears it once")
    void resetsEveryKey() {
        registry.setChangedShortcuts(Map.of("file.open", "", "file.exit", "Ctrl+S"));
        List<Map<String, String>> heard = new ArrayList<>();
        registry.changedShortcutsProperty().addListener((observable, before, now) -> heard.add(now));

        registry.resetShortcuts();

        assertEquals(Shortcut.of(KeyCode.O, CTRL), open.getShortcut());
        assertEquals(Shortcut.of(KeyCode.S, CTRL), save.getShortcut());
        assertNull(exit.getShortcut());
        assertEquals(Map.of(), registry.getChangedShortcuts());
        assertEquals(List.of(Map.of()), heard);

        registry.resetShortcuts();

        assertEquals(1, heard.size());
    }

    @Test
    @DisplayName("defaults of the application that clash in one context are logged as a warning when registered;"
            + " the same default in another context is an override, not a clash")
    void clashingDefaultsAreLogged() {
        CommandRegistry clashing = new CommandRegistry(themedScene(new VBox()));
        RecordingLogger log = new RecordingLogger();
        clashing.setLogger(log);
        CommandGroup edit = clashing.addGroup("edit", "Edit");
        CommandContext document = clashing.addContext("document", "Document", new VBox());
        clashing.addCommand(edit, "edit.copyPath", "Copy Path", Shortcut.of(KeyCode.C, CTRL), new RunCounter());
        clashing.addCommand(edit, document, "doc.bold", "Bold", Shortcut.of(KeyCode.B, CTRL), new RunCounter());
        clashing.addFixedCommand(edit, document, "doc.copy", "Copy", Shortcut.of(KeyCode.C, CTRL), null);
        clashing.addCommand(edit, document, "doc.none", "Nothing", null, new RunCounter());
        clashing.addCommand(edit, document, "doc.none2", "Nothing Either", null, new RunCounter());

        assertEquals(List.of(), log.records);

        clashing.addCommand(edit, document, "doc.bolder", "Bolder", Shortcut.of(KeyCode.B, CTRL), new RunCounter());
        clashing.addCommand(edit, document, "doc.copyAgain", "Copy Again", Shortcut.of(KeyCode.C, CTRL),
                new RunCounter());

        assertEquals(2, log.records.size(), String.valueOf(log.records));
        assertEquals(Level.WARN, log.records.get(0).level());
        assertTrue(log.records.get(0).message().contains("'doc.bolder'"), log.records.get(0).message());
        assertTrue(log.records.get(0).message().contains("'doc.bold'"), log.records.get(0).message());
        assertTrue(log.records.get(0).message().contains("Ctrl+B"), log.records.get(0).message());
        assertEquals(Level.WARN, log.records.get(1).level());
        assertTrue(log.records.get(1).message().contains("'doc.copy'"), log.records.get(1).message());
    }

    @Test
    @DisplayName("the keys one window gives out, taken back by another window's registry, run its commands there and"
            + " show in its menu items; the defaults they replace run nothing")
    void anotherWindowTakesTheKeys() {
        save.setShortcut(Shortcut.of(KeyCode.S, CTRL, SHIFT));
        exit.setShortcut(Shortcut.of('-', CTRL));
        Label content = new Label("content");
        CommandRegistry other = new CommandRegistry(themedScene(new VBox(content)));
        CommandGroup otherFile = other.addGroup("file", "File");
        RunCounter otherSave = new RunCounter();
        RunCounter otherExit = new RunCounter();
        Command saveThere = other.addCommand(otherFile, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), otherSave);
        other.addCommand(otherFile, "file.exit", "Exit", null, otherExit);
        MenuItem saveItem = new MenuItem("Save");
        saveThere.bind(saveItem);

        other.setChangedShortcuts(registry.getChangedShortcuts());

        content.fireEvent(keyPressed(KeyCode.S, CTRL));
        assertEquals(0, otherSave.runs());
        content.fireEvent(keyPressed(KeyCode.S, CTRL, SHIFT));
        assertEquals(1, otherSave.runs());
        content.fireEvent(keyPressed(KeyCode.SUBTRACT, CTRL));
        assertEquals(1, otherExit.runs());
        assertEquals("Ctrl+Shift+S", saveItem.getAccelerator().getDisplayText());
        assertEquals(registry.getChangedShortcuts(), other.getChangedShortcuts());
    }
}
