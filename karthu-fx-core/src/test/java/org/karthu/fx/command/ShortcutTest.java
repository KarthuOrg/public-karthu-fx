package org.karthu.fx.command;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.karthu.fx.command.Shortcut.Modifier.ALT;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;

/**
 * A shortcut as a value: equality, the keys it refuses, and its display text.
 */
class ShortcutTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    @Test
    @DisplayName("two shortcuts with the same key and modifiers are equal, in whatever order the modifiers were given")
    void equality() {
        Shortcut shortcut = Shortcut.of(KeyCode.S, CTRL, SHIFT);

        assertEquals(Shortcut.of(KeyCode.S, SHIFT, CTRL), shortcut);
        assertEquals(Shortcut.of(KeyCode.S, SHIFT, CTRL).hashCode(), shortcut.hashCode());
        assertEquals(Shortcut.of(KeyCode.S, CTRL, CTRL), Shortcut.of(KeyCode.S, CTRL));
        assertNotEquals(Shortcut.of(KeyCode.S, CTRL), shortcut);
        assertNotEquals(Shortcut.of(KeyCode.D, CTRL, SHIFT), shortcut);
        assertNotEquals(Shortcut.of(KeyCode.S, ALT, SHIFT), shortcut);
        assertNotEquals(Shortcut.of(KeyCode.F5), Shortcut.of(KeyCode.F6));
    }

    @Test
    @DisplayName("the display text: the modifiers in the order Ctrl, Alt, Shift, then the key's name")
    void displayText() {
        assertEquals("Ctrl+Alt+Shift+L", Shortcut.of(KeyCode.L, SHIFT, ALT, CTRL).getDisplayText());
        assertEquals("Ctrl+Shift+S", Shortcut.of(KeyCode.S, SHIFT, CTRL).getDisplayText());
        assertEquals("Alt+F4", Shortcut.of(KeyCode.F4, ALT).getDisplayText());
        assertEquals("F5", Shortcut.of(KeyCode.F5).getDisplayText());
        assertEquals("Ctrl+0", Shortcut.of(KeyCode.DIGIT0, CTRL).getDisplayText());
        assertEquals("Delete", Shortcut.of(KeyCode.DELETE).getDisplayText());
        assertEquals("Ctrl+Tab", Shortcut.of(KeyCode.TAB, CTRL).getDisplayText());
        assertEquals("Ctrl+Shift+S", Shortcut.of(KeyCode.S, SHIFT, CTRL).toString());
    }

    @Test
    @DisplayName("a punctuation key is the character it types, shown as the character: Ctrl++, Ctrl+-, Ctrl+Alt+#")
    void characterDisplayText() {
        assertEquals("Ctrl++", Shortcut.of('+', CTRL).getDisplayText());
        assertEquals("Ctrl+-", Shortcut.of('-', CTRL).getDisplayText());
        assertEquals("Ctrl+Alt+#", Shortcut.of('#', ALT, CTRL).getDisplayText());
        assertEquals("Alt+.", Shortcut.of('.', ALT).toString());
    }

    @Test
    @DisplayName("two character shortcuts are equal with the same character and modifiers; a key is another shortcut,"
            + " also the key JavaFX names after the character")
    void characterEquality() {
        Shortcut plus = Shortcut.of('+', CTRL);

        assertEquals(Shortcut.of('+', CTRL), plus);
        assertEquals(Shortcut.of('+', CTRL).hashCode(), plus.hashCode());
        assertEquals(Shortcut.of('#', ALT, CTRL), Shortcut.of('#', CTRL, ALT));
        assertNotEquals(Shortcut.of('-', CTRL), plus);
        assertNotEquals(Shortcut.of('+', CTRL, ALT), plus);
        assertNotEquals(Shortcut.of('+'), plus);
        assertNotEquals(Shortcut.of(KeyCode.PLUS, CTRL), plus);
        assertNotEquals(Shortcut.of(KeyCode.ADD, CTRL), plus);
        assertNotEquals(plus, Shortcut.of(KeyCode.PLUS, CTRL));
    }

    @Test
    @DisplayName("a letter A to Z or a digit is a key, not a character; a space or an invisible character is no"
            + " character either; Shift comes with the character; missing modifiers are refused")
    void refusedCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('s', CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('S', CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('0', CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('9'));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of(' ', CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('\t', CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('\n'));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('\u00A0', CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('\u200B', CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('\uD83D', CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of('+', CTRL, SHIFT));
        assertThrows(NullPointerException.class, () -> Shortcut.of('+', (Shortcut.Modifier) null));
        assertThrows(NullPointerException.class, () -> Shortcut.of('+', (Shortcut.Modifier[]) null));
        // A character typed with AltGr, or one without a key of its own, is a character too.
        assertEquals("Ctrl+@", Shortcut.of('@', CTRL).getDisplayText());
        assertEquals("Ctrl+\u00FC", Shortcut.of('\u00FC', CTRL).getDisplayText());
    }

    @Test
    @DisplayName("a modifier key or an undefined key is no shortcut's key, and missing arguments are refused")
    void refusedKeys() {
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of(KeyCode.CONTROL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of(KeyCode.SHIFT, CTRL));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of(KeyCode.ALT));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of(KeyCode.META));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of(KeyCode.COMMAND));
        assertThrows(IllegalArgumentException.class, () -> Shortcut.of(KeyCode.UNDEFINED, CTRL));
        assertThrows(NullPointerException.class, () -> Shortcut.of(null, CTRL));
        assertThrows(NullPointerException.class, () -> Shortcut.of(KeyCode.S, (Shortcut.Modifier) null));
        assertThrows(NullPointerException.class, () -> Shortcut.of(KeyCode.S, (Shortcut.Modifier[]) null));
    }
}
