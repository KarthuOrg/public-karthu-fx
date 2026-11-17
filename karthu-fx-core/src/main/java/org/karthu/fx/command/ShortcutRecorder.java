package org.karthu.fx.command;

import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The recording field of the shortcut editor: it takes the place of a row's key caps while the user records a new
 * key, holds the focus, and takes every key pressed in it. It shows the modifiers held down as caps, then
 * <i>Press keys…</i> or why the last key was refused, and <i>Esc to cancel</i> at its end. The first key the user may
 * record ({@link Shortcut#isRecordable()}) becomes the row's key; Esc without Ctrl and Alt ends the recording without
 * a key. A modifier key alone is no key: it shows as held.
 */
final class ShortcutRecorder extends HBox {

    private static final PseudoClass REFUSED = PseudoClass.getPseudoClass("refused");

    /** Keys that type no punctuation, recorded by the key beside the letters, the function and navigation keys. */
    private static final Set<KeyCode> NAMED_KEYS = EnumSet.of(KeyCode.ESCAPE, KeyCode.BACK_SPACE, KeyCode.DELETE,
            KeyCode.INSERT);

    /**
     * The characters a recorded key may be, in the order they are tried. A key of the layout may type two of them,
     * one with Shift and one with AltGr, and JavaFX finds a character's key without saying which: the characters a
     * layout types with Shift come first ({@code /} before {@code |} on the Swiss {@code 7}, {@code *} before
     * {@code #} on the Swiss {@code 3}), the ones it types with AltGr last.
     */
    private static final String CHARACTERS = "!\"$%&'()*+,-./:;<=>?^_`\u00B0\u00A3\u00A7@#|~{}[]\\\u20AC";

    private final ShortcutsEditor editor;

    private final HBox held = KeyCaps.row();

    private final Label message = new Label();

    /** The modifiers held down, which show as caps. */
    private final Set<Shortcut.Modifier> heldModifiers = EnumSet.noneOf(Shortcut.Modifier.class);

    /** Why the last key was refused; null while it was none. */
    private String refusal;

    /**
     * Creates the recording field of an editor.
     *
     * @param editor the editor
     */
    ShortcutRecorder(ShortcutsEditor editor) {
        this.editor = editor;
        getStyleClass().add("shortcuts-recorder");
        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(true);
        message.getStyleClass().add("shortcuts-recorder-message");
        message.setMinWidth(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label hint = new Label();
        hint.getStyleClass().add("shortcuts-recorder-hint");
        hint.setMinWidth(Region.USE_PREF_SIZE);
        hint.textProperty().bind(editor.escToCancelTextProperty());
        getChildren().addAll(held, message, spacer, hint);
        editor.pressKeysTextProperty().addListener((observable, oldText, newText) -> update());
        addEventHandler(KeyEvent.KEY_PRESSED, this::pressed);
        addEventHandler(KeyEvent.KEY_RELEASED, this::released);
        addEventHandler(KeyEvent.KEY_TYPED, Event::consume);
        // The field keeps the clicks into it: neither its row nor the list acts on them, so a click into the field
        // never selects, edits or moves the focus, which would end the recording.
        addEventHandler(MouseEvent.ANY, Event::consume);
        update();
    }

    /**
     * Starts a recording: no modifier held, no key refused.
     */
    void start() {
        heldModifiers.clear();
        refusal = null;
        update();
    }

    /**
     * A key pressed in the field, which takes it: a modifier shows as held, Esc ends the recording, any other key is
     * recorded or refused.
     */
    private void pressed(KeyEvent press) {
        press.consume();
        KeyCode code = press.getCode();
        if (code.isModifierKey()) {
            hold(press, true);
            refusal = null;
            update();
            return;
        }
        setHeld(Shortcut.Modifier.CTRL, press.isShortcutDown());
        setHeld(Shortcut.Modifier.ALT, press.isAltDown());
        setHeld(Shortcut.Modifier.SHIFT, press.isShiftDown());
        if (code == KeyCode.ESCAPE && !press.isShortcutDown() && !press.isAltDown()) {
            editor.cancelRecording(true);
            return;
        }
        Shortcut shortcut = recordedShortcut(press);
        if (shortcut == null) {
            refusal = editor.getUnusableKeyText();
        } else if (!shortcut.isRecordable()) {
            refusal = ShortcutsEditor.fill(editor.getNeedsCtrlOrAltFormat(), shortcut.getDisplayText());
        } else {
            editor.recorded(shortcut);
            return;
        }
        update();
    }

    /**
     * A key released in the field, which takes it: a modifier no longer shows as held.
     */
    private void released(KeyEvent release) {
        release.consume();
        if (release.getCode().isModifierKey()) {
            hold(release, false);
            update();
        }
    }

    /**
     * Takes the modifiers held from a modifier key's press or release. The event's own flags may or may not count the
     * key itself, so the key decides for itself.
     */
    private void hold(KeyEvent event, boolean pressed) {
        KeyCode code = event.getCode();
        setHeld(Shortcut.Modifier.CTRL, code == KeyCode.CONTROL ? pressed : event.isShortcutDown());
        setHeld(Shortcut.Modifier.ALT, code == KeyCode.ALT ? pressed : event.isAltDown());
        setHeld(Shortcut.Modifier.SHIFT, code == KeyCode.SHIFT ? pressed : event.isShiftDown());
    }

    /** Shows a modifier as held down or no longer. */
    private void setHeld(Shortcut.Modifier modifier, boolean down) {
        if (down) {
            heldModifiers.add(modifier);
        } else {
            heldModifiers.remove(modifier);
        }
    }

    /**
     * Shows the held modifiers, in the order of the constants as a key's caps show them, and the prompt or the reason
     * of the last refusal.
     */
    private void update() {
        List<String> modifiers = new ArrayList<>();
        for (Shortcut.Modifier modifier : heldModifiers) {
            modifiers.add(modifier.text());
        }
        KeyCaps.show(held, modifiers);
        held.setVisible(!modifiers.isEmpty());
        held.setManaged(!modifiers.isEmpty());
        message.setText(refusal != null ? refusal : editor.getPressKeysText());
        pseudoClassStateChanged(REFUSED, refusal != null);
    }

    /**
     * The key a press records: letters, digits, function keys and named keys by the key with Ctrl, Alt and Shift as
     * held; punctuation by the character it types, with Ctrl and Alt as held, since the key that types it differs
     * from layout to layout and Shift belongs to typing it.
     *
     * @param press a key press that is no modifier key
     * @return the shortcut, null when the press is a key no shortcut can be on
     */
    static Shortcut recordedShortcut(KeyEvent press) {
        KeyCode code = press.getCode();
        if (!isRecordedByKey(code, press.isShiftDown())) {
            Character character = typedCharacter(press);
            if (character != null) {
                return Shortcut.of(character, modifiers(press, false));
            }
        }
        if (code == KeyCode.UNDEFINED || code.isModifierKey() || code == KeyCode.SHORTCUT) {
            return null;
        }
        return Shortcut.of(code, modifiers(press, true));
    }

    /**
     * Whether a key is recorded by the key and never as a character: a letter, a function key, a navigation key, a
     * key of the keypad's digits, a key that types white space, Esc, Backspace, Delete and Insert, and a digit without
     * Shift. A digit with Shift types a character on most layouts ({@code +} on a Swiss keyboard).
     */
    private static boolean isRecordedByKey(KeyCode code, boolean shift) {
        return code.isLetterKey() || code.isFunctionKey() || code.isNavigationKey() || code.isKeypadKey()
                || code.isWhitespaceKey() || code.isMediaKey() || NAMED_KEYS.contains(code)
                || code.isDigitKey() && !shift;
    }

    /**
     * The character a press types, tried with each of {@link #CHARACTERS} as a shortcut on the press, so that it
     * takes the same keys a command on that character runs on, a character's fallback aside
     * ({@link Shortcut#matches(KeyEvent)}): Ctrl and {@code =} record <i>Ctrl+=</i> on a US keyboard, though they run
     * <i>Ctrl++</i> where no command is on <i>Ctrl+=</i>. Without Shift the press's text may name a character
     * the list does not hold, such as a letter with an accent. With Shift the text cannot be trusted (Windows sends
     * the key's character without Shift), so the key alone decides: first a character the layout types on it with
     * Shift, then one it types on it either way, such as the keypad's {@code +}.
     *
     * @return the character, null when the press types none
     */
    private static Character typedCharacter(KeyEvent press) {
        Shortcut.Modifier[] typing = modifiers(press, false);
        if (!press.isShiftDown()) {
            for (int index = 0; index < CHARACTERS.length(); index++) {
                char character = CHARACTERS.charAt(index);
                if (Shortcut.of(character, typing).matches(press)) {
                    return character;
                }
            }
            return characterOfText(press.getText(), typing);
        }
        KeyEvent keyOnly = copy(press, true);
        KeyEvent unshifted = copy(press, false);
        for (int index = 0; index < CHARACTERS.length(); index++) {
            char character = CHARACTERS.charAt(index);
            Shortcut shortcut = Shortcut.of(character, typing);
            if (shortcut.matches(keyOnly) && !shortcut.matches(unshifted)) {
                return character;
            }
        }
        for (int index = 0; index < CHARACTERS.length(); index++) {
            char character = CHARACTERS.charAt(index);
            if (Shortcut.of(character, typing).matches(unshifted)) {
                return character;
            }
        }
        return null;
    }

    /**
     * The character of a press's text when a shortcut can be on it.
     */
    private static Character characterOfText(String text, Shortcut.Modifier[] typing) {
        if (text == null || text.length() != 1) {
            return null;
        }
        char character = text.charAt(0);
        try {
            Shortcut.of(character, typing);
            return character;
        } catch (IllegalArgumentException notACharacter) {
            // A letter A to Z, a digit or a character no key cap shows: the key decides.
            return null;
        }
    }

    /**
     * A copy of a press without its text, with Shift down or not.
     */
    private static KeyEvent copy(KeyEvent press, boolean shift) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, press.getCharacter(), "", press.getCode(), shift,
                press.isControlDown(), press.isAltDown(), press.isMetaDown());
    }

    /**
     * The modifiers held down with a press, Shift only when asked for.
     */
    private static Shortcut.Modifier[] modifiers(KeyEvent press, boolean withShift) {
        List<Shortcut.Modifier> down = new ArrayList<>();
        if (press.isShortcutDown()) {
            down.add(Shortcut.Modifier.CTRL);
        }
        if (press.isAltDown()) {
            down.add(Shortcut.Modifier.ALT);
        }
        if (withShift && press.isShiftDown()) {
            down.add(Shortcut.Modifier.SHIFT);
        }
        return down.toArray(new Shortcut.Modifier[0]);
    }
}
