package org.karthu.fx.theme;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;

/**
 * Applies the Nebula Dark theme to JavaFX scenes.
 *
 * <p>The library ships a single theme consisting of two stylesheets loaded in
 * order: {@code base.css} (structure and density) and {@code dark.css}
 * (color tokens). Call {@link #apply(Scene)} for every scene, including
 * dialog and secondary-window scenes.
 */
public final class ThemeManager {

    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

    private static final String CSS_BASE = "/org/karthu/fx/theme/css/base.css";
    private static final String CSS_DARK = "/org/karthu/fx/theme/css/dark.css";

    private ThemeManager() {
    }

    /**
     * Applies the theme stylesheets to the given scene, replacing any
     * stylesheets already present.
     *
     * <p>Apply the theme before the scene is shown or its CSS is applied. The
     * theme gives every scroll pane a skin of its own, and a scroll pane that
     * already has JavaFX's skin by then changes skins: JavaFX's scroll pane skin
     * keeps listening to the content's size after it is replaced and reports an
     * error at every change, until it is garbage collected.
     *
     * @param scene the scene to style, not null
     * @throws NullPointerException if the scene is null
     */
    public static void apply(Scene scene) {
        Objects.requireNonNull(scene, "scene must not be null");
        scene.getStylesheets().clear();

        String baseUrl = getStylesheetUrl(CSS_BASE);
        String darkUrl = getStylesheetUrl(CSS_DARK);

        if (baseUrl != null) {
            scene.getStylesheets().add(baseUrl);
        }
        if (darkUrl != null) {
            scene.getStylesheets().add(darkUrl);
        }

        log.debug("Applied stylesheets: base={}, dark={}", baseUrl != null, darkUrl != null);
    }

    /**
     * Gets the external form URL for a stylesheet resource.
     */
    private static String getStylesheetUrl(String resourcePath) {
        URL resource = ThemeManager.class.getResource(resourcePath);
        if (resource == null) {
            log.warn("Stylesheet not found: {}", resourcePath);
            return null;
        }
        return resource.toExternalForm();
    }
}
