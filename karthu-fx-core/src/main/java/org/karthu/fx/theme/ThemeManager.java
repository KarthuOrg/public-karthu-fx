package org.karthu.fx.theme;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Applies the theme to JavaFX scenes, in the colour scheme the application chooses: Nebula Light or Nebula Dark.
 *
 * <p>The theme consists of two stylesheets loaded in order: {@code base.css} (structure and density) and the
 * stylesheet of the scheme in effect, {@code light.css} or {@code dark.css} (colour tokens). Call
 * {@link #apply(Scene)} for every scene, including dialog and secondary-window scenes.
 *
 * <p>The application chooses the scheme with {@link #setAppearance(Appearance)}: {@link Appearance#LIGHT},
 * {@link Appearance#DARK}, the default, or {@link Appearance#SYSTEM}, which follows the scheme the platform reports
 * while the application runs. The choice holds for the whole application and may change at any time: every scene
 * the theme was applied to, shown or not, takes the other scheme at once, and so do the open popups, menus and
 * tooltips of its windows. {@link #colorSchemeProperty()} holds the scheme in effect.
 *
 * <p>The root of every themed scene carries the pseudo-class {@code light} or {@code dark}, the scheme in effect, also
 * when the scene's root is replaced, so that an application's own stylesheet gives its own colours a value per
 * scheme, for instance {@code .root:light { -my-color: #1f2230; }}.
 *
 * <p>Threads: the theme manager changes scene state, which belongs to the JavaFX Application Thread, and is not safe
 * for use from two threads at once. Once a themed scene is shown, choose the scheme and apply the theme on that thread
 * only. Before, while the application starts (for instance in {@code Application.init()}) or in a test that shows no
 * scene, one other thread may choose the scheme and apply the theme to scenes that are not shown, as JavaFX lets a
 * scene be built off its thread, provided the JavaFX Application Thread does not use the theme manager meanwhile.
 * JavaFX lets the platform's scheme be read on its Application Thread only, so {@link Appearance#SYSTEM} chosen on
 * another thread before the theme was ever applied on the JavaFX Application Thread puts Nebula Dark in effect until
 * it is: that first call there reads the platform's scheme and switches every themed scene to it. The listeners of
 * {@link #appearanceProperty()} and {@link #colorSchemeProperty()} run on the thread that makes the change, the
 * JavaFX Application Thread for a change the platform reports.
 */
public final class ThemeManager {

    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

    private static final String CSS_BASE = "/org/karthu/fx/theme/css/base.css";
    private static final String CSS_DARK = "/org/karthu/fx/theme/css/dark.css";
    private static final String CSS_LIGHT = "/org/karthu/fx/theme/css/light.css";

    private static final PseudoClass LIGHT = PseudoClass.getPseudoClass("light");
    private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");

    /**
     * Every scene the theme was applied to, held weakly: a scene nobody else holds is not kept alive for a later
     * switch. The root listener refers to no scene, so a scene's reference to it keeps nothing alive either.
     */
    private static final Set<Scene> THEMED = Collections.newSetFromMap(new WeakHashMap<>());

    /** Moves the pseudo-class of the scheme from a scene's replaced root to its new one. */
    private static final ChangeListener<Parent> ROOT_REPLACED = (observable, oldRoot, newRoot) -> {
        unmark(oldRoot);
        mark(newRoot, getColorScheme());
    };

    /** Resolves the scheme again when the platform reports another one. */
    private static final ChangeListener<ColorScheme> PLATFORM_CHANGED =
            (observable, oldScheme, newScheme) -> resolve();

    private static final ReadOnlyObjectWrapper<ColorScheme> COLOR_SCHEME =
            new ReadOnlyObjectWrapper<>(ThemeManager.class, "colorScheme", ColorScheme.DARK);

    private static final ObjectProperty<Appearance> APPEARANCE =
            new SimpleObjectProperty<>(ThemeManager.class, "appearance", Appearance.DARK) {

                @Override
                public void set(Appearance appearance) {
                    Objects.requireNonNull(appearance, "appearance must not be null");
                    super.set(appearance);
                }

                @Override
                protected void invalidated() {
                    resolve();
                }
            };

    /**
     * The scheme the platform reports, or a test's stand-in for it. Null until the theme first needs it on the JavaFX
     * Application Thread, the only thread on which JavaFX lets the platform's preferences be read.
     */
    private static ObservableValue<ColorScheme> platformScheme;

    private ThemeManager() {
    }

    /**
     * The scheme the application chooses for the theme, for the whole application: {@link Appearance#DARK} until it
     * chooses another. Every themed scene follows a change at once. Setting null throws a
     * {@link NullPointerException}.
     *
     * @return the property of the chosen appearance
     * @since 1.1.0-RC2
     */
    public static ObjectProperty<Appearance> appearanceProperty() {
        return APPEARANCE;
    }

    /**
     * Returns the scheme the application chooses for the theme.
     *
     * @return the chosen appearance, {@link Appearance#DARK} until the application chooses another
     * @see #appearanceProperty()
     * @since 1.1.0-RC2
     */
    public static Appearance getAppearance() {
        return APPEARANCE.get();
    }

    /**
     * Chooses the scheme of the theme for the whole application. Every themed scene takes it at once. Called on the
     * JavaFX Application Thread, or on another thread while no themed scene is shown, as the class description says.
     *
     * @param appearance the appearance, not null
     * @throws NullPointerException if the appearance is null
     * @see #appearanceProperty()
     * @since 1.1.0-RC2
     */
    public static void setAppearance(Appearance appearance) {
        APPEARANCE.set(appearance);
    }

    /**
     * The scheme in effect: the chosen one, or for {@link Appearance#SYSTEM} the scheme the platform reports, Dark
     * when it reports none. An application that sets colours in code reads it to follow the scheme.
     *
     * @return the read-only property of the scheme in effect
     * @since 1.1.0-RC2
     */
    public static ReadOnlyObjectProperty<ColorScheme> colorSchemeProperty() {
        return COLOR_SCHEME.getReadOnlyProperty();
    }

    /**
     * Returns the scheme in effect.
     *
     * @return {@link ColorScheme#LIGHT} or {@link ColorScheme#DARK}
     * @see #colorSchemeProperty()
     * @since 1.1.0-RC2
     */
    public static ColorScheme getColorScheme() {
        return COLOR_SCHEME.get();
    }

    /**
     * Applies the theme stylesheets to the given scene, replacing any
     * stylesheets already present: {@code base.css} and the stylesheet of the
     * scheme in effect. The scene follows every later change of the scheme,
     * also while it is not shown, as long as the application holds it.
     *
     * <p>Apply the theme before the scene is shown or its CSS is applied. The
     * theme gives every scroll pane a skin of its own, and a scroll pane that
     * already has JavaFX's skin by then changes skins: JavaFX's scroll pane skin
     * keeps listening to the content's size after it is replaced and reports an
     * error at every change, until it is garbage collected.
     *
     * <p>Called on the JavaFX Application Thread; while no themed scene is shown, also on another thread for a scene
     * that is not shown, as the class description says.
     *
     * @param scene the scene to style, not null
     * @throws NullPointerException if the scene is null
     */
    public static void apply(Scene scene) {
        Objects.requireNonNull(scene, "scene must not be null");
        // A choice of System made off the application's thread could not read the platform's scheme yet.
        resolve();
        ColorScheme scheme = COLOR_SCHEME.get();
        scene.getStylesheets().clear();

        String baseUrl = getStylesheetUrl(CSS_BASE);
        String schemeUrl = getStylesheetUrl(schemeStylesheet(scheme));

        if (baseUrl != null) {
            scene.getStylesheets().add(baseUrl);
        }
        if (schemeUrl != null) {
            scene.getStylesheets().add(schemeUrl);
        }
        if (THEMED.add(scene)) {
            scene.rootProperty().addListener(ROOT_REPLACED);
        }
        mark(scene.getRoot(), scheme);

        log.debug("Applied stylesheets: base={}, {}={}", baseUrl != null, scheme, schemeUrl != null);
    }

    /**
     * Stands in for the scheme the platform reports, for tests, which cannot change the platform's setting. Null
     * returns to the platform's own.
     *
     * @param colorScheme the stand-in, or null for the platform's scheme
     */
    static void setPlatformColorScheme(ObservableValue<ColorScheme> colorScheme) {
        if (platformScheme != null) {
            platformScheme.removeListener(PLATFORM_CHANGED);
        }
        platformScheme = colorScheme;
        if (platformScheme != null) {
            platformScheme.addListener(PLATFORM_CHANGED);
        }
        resolve();
    }

    /**
     * Sets the scheme in effect from the choice and, for System, from the platform's scheme, and moves every themed
     * scene to it when it changed.
     */
    private static void resolve() {
        ColorScheme from = COLOR_SCHEME.get();
        ColorScheme to = schemeOf(APPEARANCE.get());
        if (to != from) {
            // The scenes first: a listener of the scheme in effect finds them in their new scheme.
            restyle(from, to);
            COLOR_SCHEME.set(to);
        }
    }

    private static ColorScheme schemeOf(Appearance appearance) {
        if (appearance == Appearance.LIGHT) {
            return ColorScheme.LIGHT;
        }
        if (appearance == Appearance.SYSTEM) {
            ObservableValue<ColorScheme> platform = platformScheme();
            if (platform != null && platform.getValue() == ColorScheme.LIGHT) {
                return ColorScheme.LIGHT;
            }
        }
        // Dark, System while the platform reports no light scheme, and a null choice that reached the property
        // through a binding.
        return ColorScheme.DARK;
    }

    private static ObservableValue<ColorScheme> platformScheme() {
        if (platformScheme == null && Platform.isFxApplicationThread()) {
            platformScheme = Platform.getPreferences().colorSchemeProperty();
            platformScheme.addListener(PLATFORM_CHANGED);
        }
        return platformScheme;
    }

    /**
     * Replaces the stylesheet of one scheme with the other's in every themed scene, in place, so that the
     * application's own stylesheets keep their places, and moves the root's pseudo-class. Open popups, menus and
     * tooltips copied the stylesheets of their window when they opened: their scenes are switched too, and a popup
     * that opens later copies the switched ones.
     */
    private static void restyle(ColorScheme from, ColorScheme to) {
        String fromUrl = getStylesheetUrl(schemeStylesheet(from));
        String toUrl = getStylesheetUrl(schemeStylesheet(to));
        List<Scene> scenes = new ArrayList<>(THEMED);
        for (Scene scene : scenes) {
            replace(scene.getStylesheets(), fromUrl, toUrl);
            mark(scene.getRoot(), to);
        }
        List<Window> windows = new ArrayList<>(Window.getWindows());
        for (Window window : windows) {
            Scene scene = window.getScene();
            if (scene != null && !THEMED.contains(scene)) {
                replace(scene.getStylesheets(), fromUrl, toUrl);
            }
        }
        log.debug("Switched {} themed scenes to {}", scenes.size(), to);
    }

    private static void replace(ObservableList<String> stylesheets, String fromUrl, String toUrl) {
        if (fromUrl == null || toUrl == null) {
            return;
        }
        int index = stylesheets.indexOf(fromUrl);
        while (index >= 0) {
            stylesheets.set(index, toUrl);
            index = stylesheets.indexOf(fromUrl);
        }
    }

    private static void mark(Parent root, ColorScheme scheme) {
        if (root != null) {
            root.pseudoClassStateChanged(LIGHT, scheme == ColorScheme.LIGHT);
            root.pseudoClassStateChanged(DARK, scheme == ColorScheme.DARK);
        }
    }

    private static void unmark(Parent root) {
        if (root != null) {
            root.pseudoClassStateChanged(LIGHT, false);
            root.pseudoClassStateChanged(DARK, false);
        }
    }

    private static String schemeStylesheet(ColorScheme scheme) {
        if (scheme == ColorScheme.LIGHT) {
            return CSS_LIGHT;
        }
        return CSS_DARK;
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
