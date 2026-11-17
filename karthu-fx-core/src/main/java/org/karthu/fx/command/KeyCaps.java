package org.karthu.fx.command;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Key caps: a key shown as a row of small caps, one per modifier and one for the key or the character, such as
 * <i>Ctrl</i> <i>Shift</i> <i>S</i>, each a {@link Label} with the style class {@code shortcuts-key-cap} in an
 * {@link HBox} with {@code shortcuts-key-caps}.
 */
final class KeyCaps {

    /**
     * The modifiers as the display text starts with them, in the order JavaFX writes them on Windows and Linux.
     */
    private static final List<String> MODIFIERS = List.of("Ctrl", "Alt", "Shift");

    private KeyCaps() {
    }

    /**
     * The texts of a key's caps: its modifiers, then its key's name or its character. They are cut from the display
     * text, so that the caps show exactly what a menu item shows as the accelerator: <i>Ctrl++</i> is <i>Ctrl</i> and
     * <i>+</i>, since the display text names each modifier with its {@code +} before the key's own name.
     *
     * @param shortcut the key
     * @return the texts of its caps
     */
    static List<String> partsOf(Shortcut shortcut) {
        String rest = shortcut.getDisplayText();
        List<String> parts = new ArrayList<>();
        for (String modifier : MODIFIERS) {
            String prefix = modifier + "+";
            if (rest.startsWith(prefix) && rest.length() > prefix.length()) {
                parts.add(modifier);
                rest = rest.substring(prefix.length());
            }
        }
        parts.add(rest);
        return parts;
    }

    /**
     * A row of caps.
     *
     * @return an empty row
     */
    static HBox row() {
        HBox row = new HBox();
        row.getStyleClass().add("shortcuts-key-caps");
        return row;
    }

    /**
     * Fills a row of caps.
     *
     * @param row   the row
     * @param parts the texts of the caps, see {@link #partsOf(Shortcut)}
     */
    static void show(HBox row, List<String> parts) {
        List<Node> caps = new ArrayList<>();
        for (String part : parts) {
            Label cap = new Label(part);
            cap.getStyleClass().add("shortcuts-key-cap");
            cap.setMinWidth(Label.USE_PREF_SIZE);
            caps.add(cap);
        }
        row.getChildren().setAll(caps);
    }
}
