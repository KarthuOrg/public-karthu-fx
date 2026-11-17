package org.karthu.fx.command;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.Shortcut.Modifier.ALT;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;

/**
 * The rule of what a user may record as a command's key: function keys F1 to F12 as they are, every other key and
 * every character only with Ctrl or Alt.
 */
class RecordableShortcutTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    @Test
    @DisplayName("F1 to F12 alone or with any modifiers")
    void functionKeys() {
        List<KeyCode> functionKeys = List.of(KeyCode.F1, KeyCode.F2, KeyCode.F3, KeyCode.F4, KeyCode.F5, KeyCode.F6,
                KeyCode.F7, KeyCode.F8, KeyCode.F9, KeyCode.F10, KeyCode.F11, KeyCode.F12);
        for (KeyCode key : functionKeys) {
            assertTrue(Shortcut.of(key).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, SHIFT).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, CTRL).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, ALT, SHIFT).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, CTRL, ALT, SHIFT).isRecordable(), key.getName());
        }
        // F13 and beyond are other keys.
        assertFalse(Shortcut.of(KeyCode.F13).isRecordable());
        assertTrue(Shortcut.of(KeyCode.F13, CTRL).isRecordable());
    }

    @Test
    @DisplayName("every other key only with Ctrl or Alt: Shift alone is not enough")
    void otherKeysNeedCtrlOrAlt() {
        for (KeyCode key : List.of(KeyCode.S, KeyCode.DIGIT0, KeyCode.DELETE, KeyCode.HOME, KeyCode.LEFT,
                KeyCode.INSERT, KeyCode.NUMPAD5, KeyCode.ADD, KeyCode.PLUS)) {
            assertFalse(Shortcut.of(key).isRecordable(), key.getName());
            assertFalse(Shortcut.of(key, SHIFT).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, CTRL).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, ALT).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, CTRL, SHIFT).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, ALT, SHIFT).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, CTRL, ALT, SHIFT).isRecordable(), key.getName());
        }
    }

    @Test
    @DisplayName("Esc, Tab, Enter, Space and Backspace are refused without Ctrl or Alt, taken with them")
    void typingKeysNeedCtrlOrAlt() {
        for (KeyCode key : List.of(KeyCode.ESCAPE, KeyCode.TAB, KeyCode.ENTER, KeyCode.SPACE, KeyCode.BACK_SPACE)) {
            assertFalse(Shortcut.of(key).isRecordable(), key.getName());
            assertFalse(Shortcut.of(key, SHIFT).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, CTRL).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, CTRL, SHIFT).isRecordable(), key.getName());
            assertTrue(Shortcut.of(key, ALT).isRecordable(), key.getName());
        }
    }

    @Test
    @DisplayName("a character only with Ctrl or Alt")
    void charactersNeedCtrlOrAlt() {
        assertFalse(Shortcut.of('+').isRecordable());
        assertFalse(Shortcut.of('#').isRecordable());
        assertTrue(Shortcut.of('+', CTRL).isRecordable());
        assertTrue(Shortcut.of('-', ALT).isRecordable());
        assertTrue(Shortcut.of('#', CTRL, ALT).isRecordable());
    }
}
