package org.karthu.fx.font;

import javafx.scene.text.Font;

import java.util.List;

/**
 * The monospace font family of karthu-fx: the one family every component that shows code or code-like text uses, the
 * Markdown view for its code and the cron input for its expression, so that both pick the same font the same way.
 *
 * <p>JavaFX CSS reads only the first family of an {@code -fx-font-family} list and falls back to the system font
 * when that one is not installed, so a CSS list such as {@code "Cascadia Mono", "Consolas", monospace} gives a
 * proportional font on a machine without Cascadia Mono. The family is therefore picked here: the first installed
 * family of the preference list, and JavaFX's logical {@code Monospaced} font when none is installed. A component sets
 * it as an inline style ({@link #style()}), which names the family only, so the node keeps the font size, weight and
 * posture the theme gives it.
 *
 * <p>Every preferred family draws no ligatures: Cascadia Code and JetBrains Mono join {@code |---|}, {@code ->} or
 * {@code !=} into one glyph, which hides the source a reader is meant to see, so their variants without ligatures,
 * "Cascadia Mono" and "JetBrains Mono NL", stand in their place.
 *
 * <p>Not exported: a helper shared inside the module, so that the choice can change without an API change.
 */
public final class MonospaceFont {

    private static final List<String> PREFERRED_FAMILIES = List.of("JetBrains Mono NL", "Cascadia Mono", "Consolas");
    private static final String LOGICAL_MONOSPACE = "Monospaced";

    private static String family;

    private MonospaceFont() {
    }

    /**
     * Returns the family, chosen on the first call (the JavaFX toolkit must be running).
     *
     * @return the first installed preferred family, else {@code Monospaced}
     */
    public static String family() {
        if (family == null) {
            family = firstInstalled(Font.getFamilies());
        }
        return family;
    }

    /**
     * Returns an inline style that sets the family, for nodes that show code: the family only, so the node keeps the
     * size the theme gives it.
     *
     * @return the {@code -fx-font-family} declaration
     */
    public static String style() {
        return "-fx-font-family: \"" + family() + "\";";
    }

    /**
     * Returns the first preferred family among the installed ones.
     *
     * @param installed the installed font families
     * @return the first installed preferred family, else {@code Monospaced}
     */
    static String firstInstalled(List<String> installed) {
        for (String preferred : PREFERRED_FAMILIES) {
            if (installed.contains(preferred)) {
                return preferred;
            }
        }
        return LOGICAL_MONOSPACE;
    }
}
