package org.karthu.fx.command;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.karthu.fx.command.CommandTestSupport.RunCounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.karthu.fx.command.CommandTestSupport.onFxThread;
import static org.karthu.fx.command.CommandTestSupport.themedScene;
import static org.karthu.fx.command.Shortcut.Modifier.ALT;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;

/**
 * Commands on punctuation characters and on digit keys, fired by key presses as the keyboard layouts send them: the
 * character's key with and without Shift, the key that types it with Shift, and the numeric keypad's key.
 *
 * <p>A key press carries its key code and its text, the character the key types; on Windows the character without
 * Shift, and a platform may send no text while Ctrl is down. The tests send both forms.
 */
class CharacterKeyTest {

    private static final boolean MAC = System.getProperty("os.name", "").startsWith("Mac");

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    // --- Characters ----------------------------------------------------------------------------------

    @ParameterizedTest(name = "{1} with the text ''{0}'', Shift {2}")
    @DisplayName("a command on Ctrl++ runs on the plus key with Ctrl, as a German keyboard sends it, with Shift or"
            + " without, with the text or without")
    @CsvSource({
            "+, PLUS, false",
            "'', PLUS, false",
            "+, PLUS, true",
            "'', PLUS, true"
    })
    void plusKey(String text, KeyCode code, boolean shift) {
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        RunCounter zoomIn = new RunCounter();
        registry.addCommand(registry.addGroup("view", "View"), "view.zoomIn", "Zoom In", Shortcut.of('+', CTRL),
                zoomIn);

        content.fireEvent(press(text, code, shift, true, false));

        assertEquals(1, zoomIn.runs());
    }

    @ParameterizedTest(name = "{0} on {2} with the text ''{1}'', Shift {3}")
    @DisplayName("Ctrl++ and Ctrl+- run on their keys and on the numeric keypad's plus and minus with Ctrl, with the"
            + " text or without, Shift either way")
    @CsvSource({
            "+, +, ADD, false",
            "+, '', ADD, false",
            "+, +, ADD, true",
            "-, -, MINUS, false",
            "-, '', MINUS, false",
            "-, -, MINUS, true",
            "-, '', MINUS, true",
            "-, -, SUBTRACT, false",
            "-, '', SUBTRACT, false",
            "-, '', SUBTRACT, true"
    })
    void plusAndMinusWithTheKeypad(char character, String text, KeyCode code, boolean shift) {
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        RunCounter command = new RunCounter();
        registry.addCommand(registry.addGroup("view", "View"), "view.zoom", "Zoom", Shortcut.of(character, CTRL),
                command);

        content.fireEvent(press(text, code, shift, true, false));

        assertEquals(1, command.runs());
    }

    @ParameterizedTest(name = "Ctrl+{0} on {1}")
    @DisplayName("a command on another punctuation character runs on the key JavaFX names after it, also without text,"
            + " and on the numeric keypad's key for * and /")
    @CsvSource(delimiter = '|', value = {
            "#|NUMBER_SIGN",
            ",|COMMA",
            ".|PERIOD",
            ";|SEMICOLON",
            "<|LESS",
            "$|DOLLAR",
            "_|UNDERSCORE",
            "*|ASTERISK",
            "*|MULTIPLY",
            "/|SLASH",
            "/|DIVIDE"
    })
    void otherCharacters(char character, KeyCode code) {
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        RunCounter command = new RunCounter();
        registry.addCommand(registry.addGroup("edit", "Edit"), "edit.command", "Command",
                Shortcut.of(character, CTRL), command);

        content.fireEvent(press("", code, false, true, false));

        assertEquals(1, command.runs());
    }

    @Test
    @DisplayName("Ctrl++ runs on Ctrl, Shift and the key that types + with Shift on the keyboard layout (= on a US one,"
            + " 1 on a Swiss one); the same key without Shift types another character and does not run it")
    void plusTypedWithShift() {
        KeyCode key = keyTypingWithShift('+');
        Assumptions.assumeTrue(key != null, "the toolkit knows no key of the main keyboard for +");
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        RunCounter zoomIn = new RunCounter();
        registry.addCommand(registry.addGroup("view", "View"), "view.zoomIn", "Zoom In", Shortcut.of('+', CTRL),
                zoomIn);

        // On Windows the text of a key press is the key's character without Shift, which is not + where + needs Shift.
        content.fireEvent(press("", key, true, true, false));
        assertEquals(1, zoomIn.runs(), () -> "Ctrl+Shift+" + key);

        Assumptions.assumeTrue(key != KeyCode.PLUS, "the layout types + without Shift");
        content.fireEvent(press("", key, false, true, false));
        assertEquals(1, zoomIn.runs(), () -> "Ctrl+" + key + " without Shift");
    }

    @ParameterizedTest(name = "{1} with the text ''{0}'', Shift {2}, Ctrl {3}, Alt {4}")
    @DisplayName("Ctrl++ does not run on a press that does not type + with Ctrl alone: + without Ctrl, + with Ctrl"
            + " and Alt as AltGr sends it, = and 1 without Shift, another character, the keypad's other keys")
    @CsvSource({
            "+, PLUS, false, false, false",
            "+, PLUS, false, true, true",
            "+, ADD, false, false, false",
            "+, ADD, false, true, true",
            "=, EQUALS, false, true, false",
            "1, DIGIT1, false, true, false",
            "-, MINUS, false, true, false",
            "-, SUBTRACT, false, true, false",
            "*, MULTIPLY, false, true, false"
    })
    void otherPressesDoNotRunPlus(String text, KeyCode code, boolean shift, boolean ctrl, boolean alt) {
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        RunCounter zoomIn = new RunCounter();
        registry.addCommand(registry.addGroup("view", "View"), "view.zoomIn", "Zoom In", Shortcut.of('+', CTRL),
                zoomIn);

        content.fireEvent(press(text, code, shift, ctrl, alt));

        assertEquals(0, zoomIn.runs());
    }

    // --- Digits and other keys -----------------------------------------------------------------------

    @Test
    @DisplayName("a command on a digit key runs on the numeric keypad's digit with the same modifiers too")
    void digitKeyWithTheKeypad() {
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        CommandGroup view = registry.addGroup("view", "View");
        KeyCode[] digits = {KeyCode.DIGIT0, KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4,
                KeyCode.DIGIT5, KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9};
        KeyCode[] keypadDigits = {KeyCode.NUMPAD0, KeyCode.NUMPAD1, KeyCode.NUMPAD2, KeyCode.NUMPAD3,
                KeyCode.NUMPAD4, KeyCode.NUMPAD5, KeyCode.NUMPAD6, KeyCode.NUMPAD7, KeyCode.NUMPAD8, KeyCode.NUMPAD9};
        RunCounter[] commands = new RunCounter[digits.length];
        for (int digit = 0; digit < digits.length; digit++) {
            commands[digit] = new RunCounter();
            registry.addCommand(view, "view.digit" + digit, "Digit " + digit, Shortcut.of(digits[digit], CTRL),
                    commands[digit]);
        }
        RunCounter shiftedReset = new RunCounter();
        registry.addCommand(view, "view.shiftedReset", "Shifted Reset", Shortcut.of(KeyCode.DIGIT0, CTRL, ALT, SHIFT),
                shiftedReset);

        for (int digit = 0; digit < digits.length; digit++) {
            content.fireEvent(press("", keypadDigits[digit], false, true, false));
            assertEquals(1, commands[digit].runs(), "Ctrl+" + keypadDigits[digit]);
        }
        content.fireEvent(press("", KeyCode.NUMPAD0, true, true, true));
        assertEquals(1, shiftedReset.runs(), "Ctrl+Alt+Shift+NUMPAD0");
        assertEquals(1, commands[0].runs());
    }

    @ParameterizedTest(name = "{0} with {1} pressed on {2}, Shift {3}, Ctrl {4}, Alt {5}")
    @DisplayName("other keys have no variants: a digit runs on the keypad's digit with its own modifiers only, a"
            + " keypad key, the key JavaFX names PLUS and a letter run on their own key only, with exactly their"
            + " modifiers")
    @CsvSource({
            "DIGIT0, CTRL, NUMPAD0, false, false, false",
            "DIGIT0, CTRL, NUMPAD0, true, true, false",
            "DIGIT0, CTRL, NUMPAD0, false, true, true",
            "DIGIT0, CTRL, NUMPAD1, false, true, false",
            "NUMPAD0, CTRL, DIGIT0, false, true, false",
            "ADD, CTRL, PLUS, false, true, false",
            "PLUS, CTRL, ADD, false, true, false",
            "PLUS, CTRL, PLUS, true, true, false",
            "S, CTRL, S, true, true, false"
    })
    void otherKeysHaveNoVariants(KeyCode key, Shortcut.Modifier modifier, KeyCode pressed, boolean shift,
            boolean ctrl, boolean alt) {
        Label content = new Label("content");
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox(content)));
        RunCounter command = new RunCounter();
        registry.addCommand(registry.addGroup("view", "View"), "view.command", "Command", Shortcut.of(key, modifier),
                command);

        content.fireEvent(press("", pressed, shift, ctrl, alt));

        assertEquals(0, command.runs());
    }

    // --- Bound menu items ----------------------------------------------------------------------------

    @Test
    @DisplayName("a menu item bound to a command on Ctrl++ shows Ctrl++ as its accelerator, the command's display"
            + " text; the accelerators of Ctrl++ and Ctrl+- are two")
    void boundMenuItemShowsTheCharacter() {
        CommandRegistry registry = new CommandRegistry(themedScene(new VBox()));
        CommandGroup view = registry.addGroup("view", "View");
        Command zoomIn = registry.addCommand(view, "view.zoomIn", "Zoom In", Shortcut.of('+', CTRL), new RunCounter());
        Command zoomOut = registry.addCommand(view, "view.zoomOut", "Zoom Out", Shortcut.of('-', CTRL),
                new RunCounter());
        MenuItem zoomInItem = new MenuItem("Zoom In");
        MenuItem zoomOutItem = new MenuItem("Zoom Out");

        zoomIn.bind(zoomInItem);
        zoomOut.bind(zoomOutItem);

        assertEquals("Ctrl++", zoomIn.getShortcutDisplayText());
        assertEquals("Ctrl++", zoomInItem.getAccelerator().getDisplayText());
        assertEquals("Ctrl+-", zoomOutItem.getAccelerator().getDisplayText());
        assertNotEquals(zoomInItem.getAccelerator(), zoomOutItem.getAccelerator());
    }

    @Test
    @DisplayName("with the bound items in the window's menu bar, Ctrl++ and Ctrl+- run once on each of their keys, and"
            + " each item's accelerator runs its own command")
    void menuBarRunsEachCommandOnce() throws InterruptedException {
        RunCounter zoomIn = new RunCounter();
        RunCounter zoomOut = new RunCounter();
        // A menu bar builds its menus, and puts its items' accelerators into the scene, on the toolkit's thread only.
        onFxThread(() -> {
            Label content = new Label("content");
            VBox root = new VBox(content);
            Scene scene = themedScene(root);
            CommandRegistry registry = new CommandRegistry(scene);
            CommandGroup view = registry.addGroup("view", "View");
            MenuItem zoomInItem = new MenuItem("Zoom In");
            MenuItem zoomOutItem = new MenuItem("Zoom Out");
            registry.addCommand(view, "view.zoomIn", "Zoom In", Shortcut.of('+', CTRL), zoomIn).bind(zoomInItem);
            registry.addCommand(view, "view.zoomOut", "Zoom Out", Shortcut.of('-', CTRL), zoomOut).bind(zoomOutItem);
            addMenuBar(root, zoomInItem, zoomOutItem);

            content.fireEvent(press("+", KeyCode.PLUS, false, true, false));
            content.fireEvent(press("", KeyCode.ADD, false, true, false));
            content.fireEvent(press("-", KeyCode.MINUS, false, true, false));
            content.fireEvent(press("", KeyCode.SUBTRACT, false, true, false));
            assertEquals(2, zoomIn.runs());
            assertEquals(2, zoomOut.runs());

            scene.getAccelerators().get(zoomInItem.getAccelerator()).run();
            assertEquals(3, zoomIn.runs());
            scene.getAccelerators().get(zoomOutItem.getAccelerator()).run();
            assertEquals(3, zoomOut.runs());
        });
    }

    @Test
    @DisplayName("with the bound item in the window's menu bar, Ctrl+Shift and the key that types + with Shift run"
            + " Ctrl++ once; the same key without Shift runs it neither on its own nor through the item's accelerator")
    void menuBarAcceleratorFollowsTheLayout() throws InterruptedException {
        KeyCode key = keyTypingWithShift('+');
        Assumptions.assumeTrue(key != null && key != KeyCode.PLUS,
                "the layout types + without Shift, or the toolkit knows no key for it");
        RunCounter zoomIn = new RunCounter();
        onFxThread(() -> {
            Label content = new Label("content");
            VBox root = new VBox(content);
            Scene scene = themedScene(root);
            CommandRegistry registry = new CommandRegistry(scene);
            MenuItem zoomInItem = new MenuItem("Zoom In");
            registry.addCommand(registry.addGroup("view", "View"), "view.zoomIn", "Zoom In", Shortcut.of('+', CTRL),
                    zoomIn).bind(zoomInItem);
            addMenuBar(root, zoomInItem);
            assertNotNull(scene.getAccelerators().get(zoomInItem.getAccelerator()), "the item's accelerator acts");

            content.fireEvent(press("", key, true, true, false));
            assertEquals(1, zoomIn.runs(), "Ctrl+Shift+" + key);
            content.fireEvent(press("", key, false, true, false));
            assertEquals(1, zoomIn.runs(), "Ctrl+" + key + " without Shift");
        });
    }

    /**
     * Puts a menu bar with the items at the top of the window and lays it out, on the toolkit's thread. After the
     * items are bound, as an application builds its menus: the bar puts an item's accelerator into the scene with the
     * item's action when it builds its menus, and an accelerator an item gets later with none.
     */
    private static void addMenuBar(VBox root, MenuItem... items) {
        root.getChildren().addFirst(new MenuBar(new Menu("View", null, items)));
        root.applyCss();
        root.layout();
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
            if (combination.match(press("", code, true, true, false))) {
                return code;
            }
        }
        return null;
    }

    /**
     * A key press as the platform sends it: its text, its key, and the modifiers held down; Ctrl is the platform's
     * shortcut key, Command on macOS.
     */
    private static KeyEvent press(String text, KeyCode code, boolean shift, boolean ctrl, boolean alt) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", text, code, shift, ctrl && !MAC, alt, ctrl && MAC);
    }
}
