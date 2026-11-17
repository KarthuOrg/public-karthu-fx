package org.karthu.fx.command;

import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One key combination of a {@link Command}: a key or a typed character with the modifiers Ctrl, Alt and Shift held
 * down, such as <i>Ctrl+S</i>, <i>F5</i>, <i>Ctrl+Shift+Delete</i> or <i>Ctrl++</i>. Immutable; two shortcuts are
 * equal when they hold the same key, or the same character, and the same modifiers.
 *
 * <p><b>Keys.</b> Letters, digits, function keys and named keys, such as the arrows, Home, Delete or the keys of the
 * numeric keypad, are given by their key, with {@link #of(KeyCode, Modifier...)}. A key press is the shortcut when it
 * is the shortcut's key with exactly its modifiers held down: <i>Ctrl+S</i> is neither <i>S</i> nor
 * <i>Ctrl+Shift+S</i>. A digit of the main keyboard has one variant, the numeric keypad's digit with the same
 * modifiers: <i>Ctrl+0</i> also runs on Ctrl and the keypad's 0. Letters and the other keys have none; a keypad key
 * runs on the keypad's key alone.
 *
 * <p><b>Characters.</b> Punctuation is given by the character it types, with {@link #of(char, Modifier...)}, since
 * the key that types it differs from keyboard layout to layout: {@code +} is a key of its own on a German keyboard,
 * Shift plus {@code =} on a US one and Shift plus {@code 1} on a Swiss one. A key press is the shortcut when it has
 * exactly the shortcut's Ctrl and Alt, Shift down or not, and is
 * <ul>
 *   <li>the main keyboard's key that types the character without Shift;</li>
 *   <li>where the layout types the character with Shift, the key that types it, with Shift: <i>Ctrl++</i> runs on
 *       Ctrl, Shift and {@code =} on a US keyboard;</li>
 *   <li>the numeric keypad's key that types the character, for {@code +}, {@code -}, {@code *} and {@code /}.</li>
 * </ul>
 * <p>The key that types the character with Shift also runs the shortcut without Shift, as a fallback: Ctrl and
 * {@code =} on a US keyboard run <i>Ctrl++</i>, as they zoom in a web browser. The fallback gives way to the key's own
 * meaning. It is never a letter or a digit key, which mean themselves: Ctrl and {@code 1} on a Swiss keyboard, where
 * {@code +} is Shift plus {@code 1}, are <i>Ctrl+1</i>, never <i>Ctrl++</i>. And a command whose key the press is, or
 * whose character it types, runs before a command whose fallback the press is, whichever was added first: with a
 * command on <i>Ctrl+=</i>, Ctrl and {@code =} on a US keyboard run that one.
 * <p>So a command on <i>Ctrl++</i> runs on whichever plus key the user presses, and an application catches no
 * variants of its own. Shift belongs to typing a character, so it is no modifier of one. A key JavaFX names after a
 * character, such as {@link KeyCode#PLUS}, is a key like the others: it runs on that key alone, so give punctuation as
 * a character.
 *
 * <p>Ctrl stands for the platform's shortcut key, which is Ctrl on Windows and Linux.
 *
 * <p><b>Text form.</b> {@link #toText()} writes a shortcut as the text an application keeps in its settings, and
 * {@link #parse(String)} reads it back. The text is the same in every language and on every keyboard layout: the
 * modifiers in the order {@code Ctrl}, {@code Alt}, {@code Shift}, each followed by {@code +}, then the key's name or
 * the character.
 * <pre>
 *   shortcut = [ "Ctrl+" ] [ "Alt+" ] [ "Shift+" ] ( key name | character )
 * </pre>
 * <ul>
 *   <li>A key is written by JavaFX's name of it, {@link KeyCode#getName()}, the name JavaFX's
 *       {@link KeyCombination#valueOf(String)} reads too: a letter as its capital, a digit as the digit, the other keys
 *       by their names, spaces included: {@code Ctrl+Shift+S}, {@code F5}, {@code Ctrl+0}, {@code Alt+Left},
 *       {@code Ctrl+Page Up}, {@code Ctrl+Numpad 0}, {@code Esc}, {@code Ctrl+Backspace}.</li>
 *   <li>A character is written as itself, one character that is neither a letter A to Z nor a digit:
 *       {@code Ctrl++}, {@code Ctrl+-}, {@code Ctrl+Alt+#}. It never has {@code Shift}.</li>
 *   <li>The keys JavaFX names after a character are told from the character by their names: {@code Ctrl+Plus} is the
 *       key {@link KeyCode#PLUS}, {@code Ctrl+Add} the numeric keypad's plus, {@link KeyCode#ADD}, and {@code Ctrl++}
 *       the character {@code +}, which runs on both and on the plus key of every layout.</li>
 * </ul>
 * <p>{@code Ctrl} always stands for the platform's shortcut key. A text is read only as it is written: lower case, the
 * modifiers in another order, spaces around the parts, or a key's display text where its name differs
 * ({@code Escape} for {@code Esc}, {@code Numpad0} for {@code Numpad 0}) are not a shortcut's text form. The display
 * text, {@link #getDisplayText()}, is for the user to read, the text form for an application to keep.
 *
 * <p><b>What a user may record.</b> An application gives its commands any shortcuts as their defaults, such as Delete
 * or Esc on a canvas. A user who changes a command's key may choose a shortcut only when {@link #isRecordable()} says
 * so: a function key F1 to F12, alone or with any modifiers, and every other key and every character only with Ctrl or
 * Alt, Shift alone not being enough. So a plain letter, Esc, Tab, Enter, Space and Backspace are refused without Ctrl
 * or Alt: typing and moving through a window need them. A modifier key alone is no shortcut at all.
 *
 * @since 1.1.0-RC2
 */
public final class Shortcut {

    /**
     * The key codes JavaFX names after a character: it gives a key of the main keyboard that types one of these
     * characters without Shift that character's code, on every layout, wherever the key lies ({@code +} is a key of
     * its own on a German keyboard and gets {@link KeyCode#PLUS} there).
     */
    private static final Map<Character, KeyCode> MAIN_KEYS = Map.ofEntries(
            Map.entry('!', KeyCode.EXCLAMATION_MARK),
            Map.entry('"', KeyCode.QUOTEDBL),
            Map.entry('#', KeyCode.NUMBER_SIGN),
            Map.entry('$', KeyCode.DOLLAR),
            Map.entry('&', KeyCode.AMPERSAND),
            Map.entry('\'', KeyCode.QUOTE),
            Map.entry('(', KeyCode.LEFT_PARENTHESIS),
            Map.entry(')', KeyCode.RIGHT_PARENTHESIS),
            Map.entry('*', KeyCode.ASTERISK),
            Map.entry('+', KeyCode.PLUS),
            Map.entry(',', KeyCode.COMMA),
            Map.entry('-', KeyCode.MINUS),
            Map.entry('.', KeyCode.PERIOD),
            Map.entry('/', KeyCode.SLASH),
            Map.entry(':', KeyCode.COLON),
            Map.entry(';', KeyCode.SEMICOLON),
            Map.entry('<', KeyCode.LESS),
            Map.entry('=', KeyCode.EQUALS),
            Map.entry('>', KeyCode.GREATER),
            Map.entry('@', KeyCode.AT),
            Map.entry('[', KeyCode.OPEN_BRACKET),
            Map.entry('\\', KeyCode.BACK_SLASH),
            Map.entry(']', KeyCode.CLOSE_BRACKET),
            Map.entry('^', KeyCode.CIRCUMFLEX),
            Map.entry('_', KeyCode.UNDERSCORE),
            Map.entry('`', KeyCode.BACK_QUOTE),
            Map.entry('{', KeyCode.BRACELEFT),
            Map.entry('}', KeyCode.BRACERIGHT),
            Map.entry('\u00A1', KeyCode.INVERTED_EXCLAMATION_MARK),
            Map.entry('\u20AC', KeyCode.EURO_SIGN));

    /**
     * The keys of the numeric keypad by the character they type, the same on every layout.
     */
    private static final Map<Character, KeyCode> KEYPAD_KEYS = Map.of(
            '+', KeyCode.ADD,
            '-', KeyCode.SUBTRACT,
            '*', KeyCode.MULTIPLY,
            '/', KeyCode.DIVIDE);

    /**
     * The numeric keypad's digits by the main keyboard's digit keys.
     */
    private static final Map<KeyCode, KeyCode> KEYPAD_DIGITS = Map.of(
            KeyCode.DIGIT0, KeyCode.NUMPAD0,
            KeyCode.DIGIT1, KeyCode.NUMPAD1,
            KeyCode.DIGIT2, KeyCode.NUMPAD2,
            KeyCode.DIGIT3, KeyCode.NUMPAD3,
            KeyCode.DIGIT4, KeyCode.NUMPAD4,
            KeyCode.DIGIT5, KeyCode.NUMPAD5,
            KeyCode.DIGIT6, KeyCode.NUMPAD6,
            KeyCode.DIGIT7, KeyCode.NUMPAD7,
            KeyCode.DIGIT8, KeyCode.NUMPAD8,
            KeyCode.DIGIT9, KeyCode.NUMPAD9);

    /**
     * The function keys a user may record without Ctrl or Alt: they type nothing, and no control takes them for
     * itself.
     */
    private static final Set<KeyCode> FUNCTION_KEYS = EnumSet.of(KeyCode.F1, KeyCode.F2, KeyCode.F3, KeyCode.F4,
            KeyCode.F5, KeyCode.F6, KeyCode.F7, KeyCode.F8, KeyCode.F9, KeyCode.F10, KeyCode.F11, KeyCode.F12);

    /**
     * The key; null when the shortcut is on a character.
     */
    private final KeyCode key;

    /**
     * The character; null when the shortcut is on a key.
     */
    private final Character character;

    private final Set<Modifier> modifiers;

    /**
     * The same combination as JavaFX knows it: what a bound menu item shows as its accelerator, and what a key press
     * is matched against.
     */
    private final KeyCombination combination;

    /**
     * The numeric keypad's digit with the same modifiers, when the key is a digit of the main keyboard; else null.
     */
    private final KeyCombination keypadDigit;

    private Shortcut(KeyCode key, Character character, Set<Modifier> modifiers, KeyCombination combination,
            KeyCombination keypadDigit) {
        this.key = key;
        this.character = character;
        this.modifiers = modifiers;
        this.combination = combination;
        this.keypadDigit = keypadDigit;
    }

    /**
     * Returns the shortcut of a key with modifiers. The order of the modifiers does not matter, nor does a modifier
     * given twice.
     *
     * @param key       the key, such as {@link KeyCode#S} or {@link KeyCode#F5}
     * @param modifiers the modifiers held down with it, none for the key alone
     * @return the shortcut
     * @throws NullPointerException     when the key, the modifiers or one of them is null
     * @throws IllegalArgumentException when the key is a modifier key itself, such as {@link KeyCode#CONTROL} or
     *                                  {@link KeyCode#SHORTCUT}, or {@link KeyCode#UNDEFINED}
     * @since 1.1.0-RC2
     */
    public static Shortcut of(KeyCode key, Modifier... modifiers) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(modifiers, "modifiers");
        // JavaFX's key for the platform's shortcut key is a modifier too, though isModifierKey() does not say so.
        if (key == KeyCode.UNDEFINED || key == KeyCode.SHORTCUT || key.isModifierKey()) {
            throw new IllegalArgumentException("not a key a shortcut can be on: " + key);
        }
        Set<Modifier> down = modifierSet(modifiers);
        KeyCode keypadKey = KEYPAD_DIGITS.get(key);
        KeyCombination keypadDigit = keypadKey == null ? null
                : new KeyCodeCombination(keypadKey, javaFxModifiers(down));
        return new Shortcut(key, null, down, new KeyCodeCombination(key, javaFxModifiers(down)), keypadDigit);
    }

    /**
     * Returns the shortcut of a typed character with modifiers: a punctuation character, such as {@code +}, {@code -}
     * or {@code #}, on whichever key the keyboard layout types it, see the class's description. The order of the
     * modifiers does not matter, nor does a modifier given twice.
     *
     * @param character the character, such as {@code '+'}
     * @param modifiers the modifiers held down with it, Ctrl or Alt or both; none for the character alone
     * @return the shortcut
     * @throws NullPointerException     when the modifiers or one of them is null
     * @throws IllegalArgumentException when the character is a letter A to Z or a digit, which are keys, a space or
     *                                  another character no key cap shows, such as a control character, or when Shift
     *                                  is among the modifiers
     * @since 1.1.0-RC2
     */
    public static Shortcut of(char character, Modifier... modifiers) {
        Set<Modifier> down = modifierSet(modifiers);
        if (isKeyOfItsOwn(character)) {
            throw new IllegalArgumentException("a letter A to Z or a digit is a key, not a character: " + character);
        }
        if (!isVisible(character)) {
            throw new IllegalArgumentException("not a character a shortcut can be on: U+"
                    + Integer.toHexString(character).toUpperCase(Locale.ROOT));
        }
        if (down.contains(Modifier.SHIFT)) {
            throw new IllegalArgumentException("Shift comes with the character, it is no modifier of it: "
                    + character);
        }
        return new Shortcut(null, character, down, new CharacterCombination(character, javaFxModifiers(down)), null);
    }

    /**
     * Whether a character is a letter A to Z or a digit: JavaFX gives each of them a key of its own on every layout.
     */
    private static boolean isKeyOfItsOwn(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9';
    }

    /**
     * Whether a character is one the user sees on a key cap: not a space, a control or format character, nor half of
     * a character outside the Basic Multilingual Plane, which no key types alone.
     */
    private static boolean isVisible(char character) {
        return !Character.isWhitespace(character) && !Character.isSpaceChar(character)
                && !Character.isISOControl(character) && !Character.isSurrogate(character)
                && Character.getType(character) != Character.FORMAT;
    }

    /**
     * The modifiers as an unmodifiable set; refuses null.
     */
    private static Set<Modifier> modifierSet(Modifier... modifiers) {
        Objects.requireNonNull(modifiers, "modifiers");
        Set<Modifier> down = EnumSet.noneOf(Modifier.class);
        for (Modifier modifier : modifiers) {
            down.add(Objects.requireNonNull(modifier, "modifier"));
        }
        return Collections.unmodifiableSet(down);
    }

    /**
     * Returns the shortcut as the user reads it, the same text a menu item shows for it as its accelerator: the
     * modifiers in the order Ctrl, Alt, Shift, then the key's name or the character, joined by {@code +}, such as
     * <i>Ctrl+Shift+S</i>, <i>F5</i> or <i>Ctrl++</i>.
     *
     * @return the display text
     * @since 1.1.0-RC2
     */
    public String getDisplayText() {
        return combination.getDisplayText();
    }

    /**
     * Reads a shortcut from its text form, as {@link #toText()} writes it (see the class's description):
     * {@code Ctrl+Shift+S}, {@code F5}, {@code Ctrl+Page Up}, {@code Ctrl++}. It reads any shortcut, also one a user
     * may not record: see {@link #isRecordable()}.
     *
     * @param text the text form
     * @return the shortcut
     * @throws NullPointerException     when the text is null
     * @throws IllegalArgumentException when the text is not a shortcut's text form: not written as the class's
     *                                  description says, a key JavaFX does not name so, a modifier key alone, or a
     *                                  character with Shift
     * @since 1.1.0-RC2
     */
    public static Shortcut parse(String text) {
        Objects.requireNonNull(text, "text");
        Set<Modifier> down = EnumSet.noneOf(Modifier.class);
        String rest = text;
        // The modifiers in their fixed order: one in another order stays in the rest, which then names no key.
        for (Modifier modifier : Modifier.values()) {
            String prefix = modifier.text + "+";
            // Something must follow the prefix: the + of "Ctrl++" is the character, not a separator.
            if (rest.length() > prefix.length() && rest.startsWith(prefix)) {
                down.add(modifier);
                rest = rest.substring(prefix.length());
            }
        }
        Modifier[] modifiers = down.toArray(new Modifier[0]);
        if (rest.length() == 1 && !isKeyOfItsOwn(rest.charAt(0))) {
            return of(rest.charAt(0), modifiers);
        }
        KeyCode key = KeyCode.getKeyCode(rest);
        if (key == null) {
            throw new IllegalArgumentException("not a shortcut's text: " + text);
        }
        return of(key, modifiers);
    }

    /**
     * Returns the shortcut in its text form, the one an application keeps in its settings and {@link #parse(String)}
     * reads back (see the class's description): the same in every language and on every keyboard layout.
     *
     * @return the text form, such as {@code Ctrl+Shift+S}, {@code F5}, {@code Ctrl+Plus} for the key or
     *         {@code Ctrl++} for the character
     * @since 1.1.0-RC2
     */
    public String toText() {
        StringBuilder text = new StringBuilder();
        for (Modifier modifier : modifiers) {
            text.append(modifier.text).append('+');
        }
        if (key != null) {
            text.append(key.getName());
        } else {
            text.append(character.charValue());
        }
        return text.toString();
    }

    /**
     * Returns whether a user may choose the shortcut as a command's key: a function key F1 to F12, alone or with any
     * modifiers, or any other key or character with Ctrl or Alt. A plain letter, or Esc, Tab, Enter, Space or
     * Backspace without Ctrl or Alt, are not, and Shift alone is not enough. The rule binds what a user records and
     * the keys an application takes back from its settings
     * ({@link CommandRegistry#setChangedShortcuts(java.util.Map)}), never the defaults an application gives its
     * commands.
     *
     * @return true when a user may record it
     * @since 1.1.0-RC2
     */
    public boolean isRecordable() {
        return FUNCTION_KEYS.contains(key) || modifiers.contains(Modifier.CTRL) || modifiers.contains(Modifier.ALT);
    }

    /**
     * Returns the shortcut as JavaFX's combination, as a menu item takes it for its accelerator.
     *
     * @return the combination
     */
    KeyCombination toKeyCombination() {
        return combination;
    }

    /**
     * Whether a key press is this shortcut: its key with exactly its modifiers, a digit's keypad digit too, or a key
     * that types its character (see the class's description). The fallback of a character is not:
     * {@link #matchesAsFallback(KeyEvent)}.
     *
     * @param event a key press
     * @return true when the press is this shortcut
     */
    boolean matches(KeyEvent event) {
        if (combination instanceof CharacterCombination typed) {
            return typed.types(event);
        }
        return combination.match(event) || keypadDigit != null && keypadDigit.match(event);
    }

    /**
     * Whether a key press is the fallback of this shortcut's character: the key the layout types the character on
     * with Shift, pressed without Shift and with exactly the shortcut's Ctrl and Alt, unless that key is a letter or a
     * digit key. It runs the shortcut only where no shortcut {@linkplain #matches(KeyEvent) is the press}: Ctrl and
     * {@code =} on a US keyboard run <i>Ctrl++</i>, unless a command is on <i>Ctrl+=</i>. A shortcut on a key has no
     * fallback.
     *
     * @param event a key press
     * @return true when the press is this shortcut's fallback
     */
    boolean matchesAsFallback(KeyEvent event) {
        return combination instanceof CharacterCombination typed && typed.isFallback(event);
    }

    /**
     * Whether another object is the same shortcut: the same key, or the same character, with the same modifiers.
     *
     * @param other the other object
     * @return true when it is the same shortcut
     * @since 1.1.0-RC2
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Shortcut shortcut && key == shortcut.key
                && Objects.equals(character, shortcut.character) && modifiers.equals(shortcut.modifiers);
    }

    /**
     * Returns a hash code that agrees with {@link #equals(Object)}.
     *
     * @return the hash code
     * @since 1.1.0-RC2
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, character, modifiers);
    }

    /**
     * Returns the display text, see {@link #getDisplayText()}.
     *
     * @return the display text
     * @since 1.1.0-RC2
     */
    @Override
    public String toString() {
        return getDisplayText();
    }

    /**
     * JavaFX's modifiers for the shortcut's: Ctrl is the platform's shortcut key.
     */
    private static KeyCombination.Modifier[] javaFxModifiers(Set<Modifier> modifiers) {
        KeyCombination.Modifier[] javaFx = new KeyCombination.Modifier[modifiers.size()];
        int index = 0;
        for (Modifier modifier : modifiers) {
            javaFx[index] = switch (modifier) {
                case CTRL -> KeyCombination.SHORTCUT_DOWN;
                case ALT -> KeyCombination.ALT_DOWN;
                case SHIFT -> KeyCombination.SHIFT_DOWN;
            };
            index++;
        }
        return javaFx;
    }

    /**
     * A shortcut on a character as JavaFX's combination: Ctrl and Alt exactly as given, Shift either way, and a key
     * that types the character. JavaFX's own {@link KeyCharacterCombination} is not enough: it matches the key the
     * layout types the character on whether the press holds the Shift the layout needs for it or not, so Ctrl plus
     * {@code 1} on a Swiss keyboard, where {@code +} is Shift plus {@code 1}, would be Ctrl plus {@code +}; and it
     * misses the keypad's key. It is the bound menu items' accelerator too, so that a press the registry leaves runs
     * no command through a menu item either: it matches the fallback as well, every press the registry may run the
     * shortcut on.
     */
    private static final class CharacterCombination extends KeyCombination {

        private final String character;

        /**
         * The key code JavaFX gives a main keyboard's key that types the character without Shift, which it names
         * after the character; null when it has none.
         */
        private final KeyCode mainKey;

        /**
         * The numeric keypad's key that types the character; null when it has none.
         */
        private final KeyCode keypadKey;

        /**
         * The key the layout types the character on, with Shift down. JavaFX finds the key for the layout but does
         * not say whether the layout needs Shift or AltGr for it, so a character typed with AltGr counts on its key
         * with Shift as well.
         */
        private final KeyCharacterCombination shifted;

        /**
         * The same key with Shift up, the fallback's. As JavaFX does not say which Shift or AltGr the layout needs on
         * the key, it is any key that types the character, also one that types it with AltGr ({@code |} is AltGr plus
         * {@code <} on a German keyboard) or, for a character JavaFX names no key after, without Shift.
         */
        private final KeyCharacterCombination unshifted;

        /**
         * Creates the combination of a character with JavaFX's modifiers for the shortcut's, Shift not among them.
         */
        CharacterCombination(char character, KeyCombination.Modifier[] modifiers) {
            super(with(modifiers, KeyCombination.SHIFT_ANY));
            this.character = String.valueOf(character);
            this.mainKey = MAIN_KEYS.get(character);
            this.keypadKey = KEYPAD_KEYS.get(character);
            this.shifted = new KeyCharacterCombination(this.character, with(modifiers, KeyCombination.SHIFT_DOWN));
            this.unshifted = new KeyCharacterCombination(this.character, modifiers);
        }

        /**
         * Whether a key press types the character with the modifiers or is its fallback: every press the registry
         * may run the shortcut on.
         */
        @Override
        public boolean match(KeyEvent event) {
            return types(event) || isFallback(event);
        }

        /**
         * Whether a key press types the character with the modifiers: the key that types it without Shift, by its
         * code or by the press's text, or the keypad's key, Shift down or not; or the key the layout types it on with
         * Shift, Shift down.
         */
        boolean types(KeyEvent event) {
            if (!super.match(event)) {
                return false;
            }
            KeyCode code = event.getCode();
            return code == mainKey || code == keypadKey || character.equals(event.getText()) || shifted.match(event);
        }

        /**
         * Whether a key press is the fallback: the key the layout types the character on with Shift, pressed without
         * Shift and with the modifiers, unless it is a letter or a digit key. Such a key means its own letter or digit:
         * Ctrl plus {@code 1} is <i>Ctrl+1</i> on a Swiss keyboard, where {@code +} is Shift plus {@code 1}.
         */
        boolean isFallback(KeyEvent event) {
            KeyCode code = event.getCode();
            return !code.isLetterKey() && !code.isDigitKey() && unshifted.match(event);
        }

        /** The modifiers, then the character: <i>Ctrl++</i>. */
        @Override
        public String getDisplayText() {
            return super.getDisplayText() + character;
        }

        /** The modifiers, then the character in quotes, as JavaFX names a character's combination. */
        @Override
        public String getName() {
            return super.getName() + "+'" + character + "'";
        }

        /**
         * Equal with the same character and modifiers: a scene keeps its accelerators in a map, so the accelerators
         * of two characters must be two keys.
         */
        @Override
        public boolean equals(Object other) {
            return other instanceof CharacterCombination combination && character.equals(combination.character)
                    && super.equals(other);
        }

        /** A hash code that agrees with {@link #equals(Object)}. */
        @Override
        public int hashCode() {
            return 31 * super.hashCode() + character.hashCode();
        }

        /**
         * The modifiers and one more.
         */
        private static KeyCombination.Modifier[] with(KeyCombination.Modifier[] modifiers,
                KeyCombination.Modifier added) {
            KeyCombination.Modifier[] with = Arrays.copyOf(modifiers, modifiers.length + 1);
            with[modifiers.length] = added;
            return with;
        }
    }

    /**
     * A modifier key held down with a shortcut's key.
     *
     * @since 1.1.0-RC2
     */
    public enum Modifier {

        /**
         * The platform's shortcut key: Ctrl on Windows and Linux.
         *
         * @since 1.1.0-RC2
         */
        CTRL("Ctrl"),

        /**
         * The Alt key.
         *
         * @since 1.1.0-RC2
         */
        ALT("Alt"),

        /**
         * The Shift key. A modifier of a key only: a character's shortcut runs with Shift or without, as the
         * keyboard layout types the character.
         *
         * @since 1.1.0-RC2
         */
        SHIFT("Shift");

        /**
         * The modifier's word in the text form: JavaFX's names of the Ctrl, Alt and Shift keys, {@code Ctrl} also for
         * the platform's shortcut key, which JavaFX calls {@code Shortcut}. The constants' order is the text form's.
         */
        private final String text;

        /**
         * Creates a modifier with its word in the text form.
         *
         * @param text the word
         */
        Modifier(String text) {
            this.text = text;
        }

        /**
         * Returns the modifier's word: the text form's, and the one the display text names it by on Windows and
         * Linux, where the display text writes the modifiers in the constants' order too.
         *
         * @return the word, such as {@code Ctrl}
         */
        String text() {
            return text;
        }
    }
}
