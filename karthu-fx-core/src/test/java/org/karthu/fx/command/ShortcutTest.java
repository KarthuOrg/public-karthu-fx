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
