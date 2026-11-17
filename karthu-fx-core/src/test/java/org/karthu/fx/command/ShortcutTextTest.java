package org.karthu.fx.command;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.Shortcut.Modifier.ALT;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;

/**
 * The text form of a shortcut, the one an application keeps in its settings: written, read back, and the texts it
 * refuses to read.
 */
class ShortcutTextTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a key is written as its modifiers in the order Ctrl, Alt, Shift, then JavaFX's name of the key,"
            + " joined by +")
    void writesKeys() {
        assertEquals("Ctrl+Shift+S", Shortcut.of(KeyCode.S, SHIFT, CTRL).toText());
        assertEquals("Ctrl+Alt+Shift+L", Shortcut.of(KeyCode.L, SHIFT, ALT, CTRL).toText());
        assertEquals("F5", Shortcut.of(KeyCode.F5).toText());
        assertEquals("Alt+F4", Shortcut.of(KeyCode.F4, ALT).toText());
        assertEquals("Ctrl+0", Shortcut.of(KeyCode.DIGIT0, CTRL).toText());
        assertEquals("Ctrl+Page Up", Shortcut.of(KeyCode.PAGE_UP, CTRL).toText());
        assertEquals("Ctrl+Numpad 0", Shortcut.of(KeyCode.NUMPAD0, CTRL).toText());
        assertEquals("Esc", Shortcut.of(KeyCode.ESCAPE).toText());
        assertEquals("Ctrl+Backspace", Shortcut.of(KeyCode.BACK_SPACE, CTRL).toText());
        // The keys JavaFX names after a character are written by their names, never as the character.
        assertEquals("Ctrl+Plus", Shortcut.of(KeyCode.PLUS, CTRL).toText());
        assertEquals("Ctrl+Add", Shortcut.of(KeyCode.ADD, CTRL).toText());
    }

    @Test
    @DisplayName("a character is written as its modifiers, then the character itself: Ctrl++, Ctrl+-, Ctrl+Alt+#")
    void writesCharacters() {
        assertEquals("Ctrl++", Shortcut.of('+', CTRL).toText());
        assertEquals("Ctrl+-", Shortcut.of('-', CTRL).toText());
        assertEquals("Ctrl+Alt+#", Shortcut.of('#', ALT, CTRL).toText());
        assertEquals("Alt+.", Shortcut.of('.', ALT).toText());
        assertEquals("+", Shortcut.of('+').toText());
        assertEquals("Ctrl+\u00FC", Shortcut.of('\u00FC', CTRL).toText());
    }

    @Test
    @DisplayName("the text form reads back: a key by its name, a single punctuation character as the character")
    void readsTexts() {
        assertEquals(Shortcut.of(KeyCode.S, CTRL, SHIFT), Shortcut.parse("Ctrl+Shift+S"));
        assertEquals(Shortcut.of(KeyCode.L, CTRL, ALT, SHIFT), Shortcut.parse("Ctrl+Alt+Shift+L"));
        assertEquals(Shortcut.of(KeyCode.F5), Shortcut.parse("F5"));
        assertEquals(Shortcut.of(KeyCode.DIGIT0, CTRL), Shortcut.parse("Ctrl+0"));
        assertEquals(Shortcut.of(KeyCode.PAGE_UP, CTRL), Shortcut.parse("Ctrl+Page Up"));
        assertEquals(Shortcut.of(KeyCode.NUMPAD0, CTRL), Shortcut.parse("Ctrl+Numpad 0"));
        assertEquals(Shortcut.of(KeyCode.PLUS, CTRL), Shortcut.parse("Ctrl+Plus"));
        assertEquals(Shortcut.of(KeyCode.ADD, CTRL), Shortcut.parse("Ctrl+Add"));
        assertEquals(Shortcut.of('+', CTRL), Shortcut.parse("Ctrl++"));
        assertEquals(Shortcut.of('+'), Shortcut.parse("+"));
        assertEquals(Shortcut.of('-', CTRL), Shortcut.parse("Ctrl+-"));
        assertEquals(Shortcut.of('#', CTRL, ALT), Shortcut.parse("Ctrl+Alt+#"));
        assertEquals(Shortcut.of('\u00FC', CTRL), Shortcut.parse("Ctrl+\u00FC"));
    }

    @Test
    @DisplayName("every key a shortcut can be on, with every set of modifiers, and every punctuation character with"
            + " Ctrl and Alt reads back as the shortcut it was written from")
    void roundTrips() {
        List<Set<Shortcut.Modifier>> modifierSets = new ArrayList<>();
        for (int mask = 0; mask < 8; mask++) {
            Set<Shortcut.Modifier> set = EnumSet.noneOf(Shortcut.Modifier.class);
            for (Shortcut.Modifier modifier : Shortcut.Modifier.values()) {
                if ((mask & (1 << modifier.ordinal())) != 0) {
                    set.add(modifier);
                }
            }
            modifierSets.add(set);
        }
        int keys = 0;
        for (KeyCode code : KeyCode.values()) {
            if (code == KeyCode.UNDEFINED || code == KeyCode.SHORTCUT || code.isModifierKey()) {
                continue;
            }
            for (Set<Shortcut.Modifier> set : modifierSets) {
                Shortcut shortcut = Shortcut.of(code, set.toArray(new Shortcut.Modifier[0]));
                assertEquals(shortcut, Shortcut.parse(shortcut.toText()), shortcut.toText());
                keys++;
            }
        }
        String characters = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~\u00A7\u00B0\u00A1\u20AC\u00FC\u00E9";
        for (int index = 0; index < characters.length(); index++) {
            for (Set<Shortcut.Modifier> set : modifierSets) {
                if (set.contains(SHIFT)) {
                    continue;
                }
                Shortcut shortcut = Shortcut.of(characters.charAt(index), set.toArray(new Shortcut.Modifier[0]));
                assertEquals(shortcut, Shortcut.parse(shortcut.toText()), shortcut.toText());
            }
        }
        assertTrue(keys > 1000, "every key code with every set of modifiers: " + keys);
    }

    @Test
    @DisplayName("a text in another form is refused: other case, another order, a modifier twice or alone, Shift with"
            + " a character, a name JavaFX does not know, spaces around the parts")
    void refusesOtherTexts() {
        for (String text : List.of("", " ", "Ctrl+", "Ctrl", "Ctrl+Alt", "Shift", "Ctrl+Shortcut", "Shortcut+S",
                "ctrl+s", "Ctrl+s", "CTRL+S", "Control+S", "Shift+Ctrl+S", "Alt+Ctrl+S", "Ctrl+Ctrl+S",
                "Ctrl+Shift++", "Shift++", "Ctrl+ S", "Ctrl+S ", " Ctrl+S", "Ctrl + S", "Ctrl+Page up",
                "Ctrl+PAGE_UP", "Ctrl+Numpad0", "Ctrl+Escape", "Ctrl+Undefined", "Ctrl+'+'", "Ctrl++S", "Ctrl+\t",
                "Ctrl+\u00A0", "Ctrl+\u200B", "Ctrl+\uD83D\uDE00")) {
            assertThrows(IllegalArgumentException.class, () -> Shortcut.parse(text), text);
        }
        assertThrows(NullPointerException.class, () -> Shortcut.parse(null));
    }
}
