package org.karthu.fx.markdown;

import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 * The Markdown view's zoom shortcuts: the shortcut key (Ctrl, Command on macOS) plus {@code +}, {@code -} or
 * {@code 0}, on the main keyboard or the keypad. Shift may be down; Alt, and the other of Ctrl and Command, may not,
 * so AltGr (Ctrl plus Alt on Windows), which types characters of its own, never zooms.
 *
 * <p><b>By the character typed.</b> On the main keyboard a shortcut is the key that types its character on the
 * reader's keyboard layout, not a fixed key: {@code +} is a key of its own on a German keyboard, Shift plus {@code =} on
 * a US one and Shift plus {@code 1} on a Swiss one. A key that types the character without Shift counts with Shift or
 * without: its press carries the character ({@link KeyEvent#getText()}, on Windows the key's character without Shift),
 * and JavaFX gives it the character's own key code ({@link KeyCode#PLUS}, {@link KeyCode#MINUS} on every layout whose
 * key types them so, {@link KeyCode#DIGIT0}), which also holds where a platform sends no text with the shortcut key.
 * Where the layout needs Shift for the character, a press with Shift of the key that types it with Shift counts, as
 * JavaFX's {@link KeyCharacterCombination} finds that key for the layout. So the same key without Shift, Ctrl plus
 * {@code =} on a US keyboard or Ctrl plus {@code 1} on a Swiss one, types another character and does not zoom.
 *
 * <p><b>On the keypad</b> the shortcut is the key itself: plus ({@link KeyCode#ADD}), minus
 * ({@link KeyCode#SUBTRACT}) and 0 ({@link KeyCode#NUMPAD0}).
 */
enum ZoomKey {

    /**
     * One preset larger: {@code +}.
     */
    IN("+", KeyCode.PLUS, KeyCode.ADD),

    /**
     * One preset smaller: {@code -}.
     */
    OUT("-", KeyCode.MINUS, KeyCode.SUBTRACT),

    /**
     * Back to 100 %: {@code 0}.
     */
    RESET("0", KeyCode.DIGIT0, KeyCode.NUMPAD0);

    /**
     * The shortcut key down, Shift either way, the other modifiers up: any key.
     */
    private static final KeyCombination MODIFIERS =
            new KeyCombination(KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY) {
            };

    private final String character;

    /**
     * The key code JavaFX gives a main keyboard's key that types the character without Shift.
     */
    private final KeyCode mainKey;

    private final KeyCode keypadKey;

    /**
     * The key that types the character with Shift on the reader's layout, with the shortcut key.
     */
    private final KeyCharacterCombination shifted;

    ZoomKey(String character, KeyCode mainKey, KeyCode keypadKey) {
        this.character = character;
        this.mainKey = mainKey;
        this.keypadKey = keypadKey;
        this.shifted = new KeyCharacterCombination(character, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    }

    /**
     * The zoom shortcut a key press is.
     *
     * @param event a key press
     * @return the shortcut, {@code null} if the press is none
     */
    static ZoomKey of(KeyEvent event) {
        if (!MODIFIERS.match(event)) {
            return null;
        }
        for (ZoomKey key : values()) {
            if (key.isPressed(event)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Whether a key press with the right modifiers is this shortcut: its keypad key, the key that types its character
     * without Shift (by its code or its text), or with Shift the key that types its character with Shift.
     */
    private boolean isPressed(KeyEvent event) {
        return event.getCode() == keypadKey || event.getCode() == mainKey || character.equals(event.getText())
                || event.isShiftDown() && shifted.match(event);
    }
}
