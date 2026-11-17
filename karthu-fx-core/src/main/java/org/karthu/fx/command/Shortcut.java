package org.karthu.fx.command;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * One key combination of a {@link Command}: a key with the modifiers Ctrl, Alt and Shift held down, such as
 * <i>Ctrl+S</i>, <i>F5</i> or <i>Ctrl+Shift+Delete</i>. Immutable; two shortcuts are equal when they hold the same
 * key and the same modifiers.
 *
 * <p>A key press is the shortcut when it is the shortcut's key with exactly its modifiers held down: <i>Ctrl+S</i> is
 * neither <i>S</i> nor <i>Ctrl+Shift+S</i>. Ctrl stands for the platform's shortcut key, which is Ctrl on Windows and
 * Linux.
 *
 * @since 1.1.0-RC2
 */
public final class Shortcut {

    private final KeyCode key;

    private final Set<Modifier> modifiers;

    /**
     * The same combination as JavaFX knows it: what a bound menu item shows as its accelerator, and what a key press
     * is matched against.
     */
    private final KeyCombination combination;

    private Shortcut(KeyCode key, Set<Modifier> modifiers) {
        this.key = key;
        this.modifiers = modifiers;
        this.combination = new KeyCodeCombination(key, javaFxModifiers(modifiers));
    }

    /**
     * Returns the shortcut of a key with modifiers. The order of the modifiers does not matter, nor does a modifier
     * given twice.
     *
     * @param key       the key, such as {@link KeyCode#S} or {@link KeyCode#F5}
     * @param modifiers the modifiers held down with it, none for the key alone
     * @return the shortcut
     * @throws NullPointerException     when the key, the modifiers or one of them is null
     * @throws IllegalArgumentException when the key is a modifier key itself, such as {@link KeyCode#CONTROL}, or
     *                                  {@link KeyCode#UNDEFINED}
     * @since 1.1.0-RC2
     */
    public static Shortcut of(KeyCode key, Modifier... modifiers) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(modifiers, "modifiers");
        if (key == KeyCode.UNDEFINED || key.isModifierKey()) {
            throw new IllegalArgumentException("not a key a shortcut can be on: " + key);
        }
        Set<Modifier> down = EnumSet.noneOf(Modifier.class);
        for (Modifier modifier : modifiers) {
            down.add(Objects.requireNonNull(modifier, "modifier"));
        }
        return new Shortcut(key, Collections.unmodifiableSet(down));
    }

    /**
     * Returns the shortcut as the user reads it, the same text a menu item shows for it as its accelerator: the
     * modifiers in the order Ctrl, Alt, Shift, then the key's name, joined by {@code +}, such as <i>Ctrl+Shift+S</i>
     * or <i>F5</i>.
     *
     * @return the display text
     * @since 1.1.0-RC2
     */
    public String getDisplayText() {
        return combination.getDisplayText();
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
     * Whether a key press is this shortcut: its key with exactly its modifiers.
     *
     * @param event a key press
     * @return true when the press is this shortcut
     */
    boolean matches(KeyEvent event) {
        return combination.match(event);
    }

    /**
     * Whether another object is the same shortcut: the same key with the same modifiers.
     *
     * @param other the other object
     * @return true when it is the same shortcut
     * @since 1.1.0-RC2
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Shortcut shortcut && key == shortcut.key && modifiers.equals(shortcut.modifiers);
    }

    /**
     * Returns a hash code that agrees with {@link #equals(Object)}.
     *
     * @return the hash code
     * @since 1.1.0-RC2
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, modifiers);
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
        CTRL,

        /**
         * The Alt key.
         *
         * @since 1.1.0-RC2
         */
        ALT,

        /**
         * The Shift key.
         *
         * @since 1.1.0-RC2
         */
        SHIFT
    }
}
