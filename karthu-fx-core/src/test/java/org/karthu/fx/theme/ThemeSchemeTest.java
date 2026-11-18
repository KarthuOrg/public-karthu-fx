package org.karthu.fx.theme;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The colour schemes of the theme, seen from outside: an application chooses Light, Dark or System on the theme
 * manager, and the nodes of its themed scenes, which are never shown, resolve to the colours of the scheme in effect.
 * The expected colours are the values of the schemes' stylesheets: the window, a text field's background and a
 * label's text.
 */
class ThemeSchemeTest {

    private static final Color DARK_WINDOW = Color.web("#1b1d30");
    private static final Color DARK_FIELD = Color.web("#24273c");
    private static final Color DARK_TEXT = Color.web("#d6d9e6");

    private static final Color LIGHT_WINDOW = Color.web("#E4E5E8");
    private static final Color LIGHT_FIELD = Color.web("#F3F1E8");
    private static final Color LIGHT_TEXT = Color.web("#1F2230");

    /** How long the test of a scene nobody holds asks for garbage collections at most. */
    private static final long COLLECTION_PATIENCE_SECONDS = 10;

    /**
     * An application's stylesheet that gives a colour of its own a value per scheme, keyed to the root's
     * pseudo-class.
     */
    private static final String OWN_COLORS = ".root:light .probe { -fx-background-color: #ff0000; }\n"
            + ".root:dark .probe { -fx-background-color: #00ff00; }\n";

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @AfterEach
    void backToTheDefault() {
        ThemeManager.setAppearance(Appearance.DARK);
        ThemeManager.setPlatformColorScheme(null);
    }

    @Test
    @DisplayName("without a choice the theme is Dark: the scheme in effect is dark and a themed scene has today's colours")
    void darkByDefault() {
        Themed themed = new Themed();

        assertEquals(Appearance.DARK, ThemeManager.getAppearance());
        assertEquals(ColorScheme.DARK, ThemeManager.getColorScheme());
        themed.assertColors(DARK_WINDOW, DARK_FIELD, DARK_TEXT);
    }

    @Test
    @DisplayName("with Light chosen, a scene themed afterwards has Nebula Light's colours")
    void lightChosen() {
        ThemeManager.setAppearance(Appearance.LIGHT);
        Themed themed = new Themed();

        assertEquals(Appearance.LIGHT, ThemeManager.getAppearance());
        assertEquals(ColorScheme.LIGHT, ThemeManager.getColorScheme());
        themed.assertColors(LIGHT_WINDOW, LIGHT_FIELD, LIGHT_TEXT);
    }

    @Test
    @DisplayName("a themed scene that is not shown follows every switch at once, there and back")
    void sceneSwitchesLive() {
        Themed themed = new Themed();
        themed.assertColors(DARK_WINDOW, DARK_FIELD, DARK_TEXT);

        ThemeManager.setAppearance(Appearance.LIGHT);
        themed.assertColors(LIGHT_WINDOW, LIGHT_FIELD, LIGHT_TEXT);

        ThemeManager.setAppearance(Appearance.DARK);
        themed.assertColors(DARK_WINDOW, DARK_FIELD, DARK_TEXT);
    }

    @Test
    @DisplayName("a switch replaces only the scheme's stylesheet, in place: the application's own keep their places")
    void ownStylesheetsStayInPlace() {
        Themed themed = new Themed();
        List<String> stylesheets = themed.scene.getStylesheets();
        stylesheets.add(0, css("/* first */"));
        stylesheets.add(css(OWN_COLORS));
        List<String> before = new ArrayList<>(stylesheets);
        assertTrue(before.get(2).endsWith("/dark.css"), "" + before);

        ThemeManager.setAppearance(Appearance.LIGHT);

        assertEquals(4, stylesheets.size(), "" + stylesheets);
        assertEquals(before.get(0), stylesheets.get(0), "the application's first");
        assertEquals(before.get(1), stylesheets.get(1), "the base stylesheet");
        assertTrue(stylesheets.get(2).endsWith("/light.css"), "the scheme's stylesheet: " + stylesheets.get(2));
        assertEquals(before.get(3), stylesheets.get(3), "the application's last");
        themed.assertColors(LIGHT_WINDOW, LIGHT_FIELD, LIGHT_TEXT);
    }

    @Test
    @DisplayName("the root carries the pseudo-class of the scheme in effect, and an application's rule keyed to it applies")
    void pseudoClassOnTheRoot() {
        Themed themed = new Themed();
        themed.scene.getStylesheets().add(css(OWN_COLORS));
        Region probe = themed.probe();

        assertSchemeClass(themed.root, "dark");
        assertEquals(Color.web("#00ff00"), fill(probe, 0));

        ThemeManager.setAppearance(Appearance.LIGHT);

        assertSchemeClass(themed.root, "light");
        themed.root.applyCss();
        assertEquals(Color.web("#ff0000"), fill(probe, 0));
    }

    @Test
    @DisplayName("a replaced root takes the pseudo-class, the old root loses it, and the new root follows a switch")
    void pseudoClassMovesToAReplacedRoot() {
        Themed themed = new Themed();
        ThemeManager.setAppearance(Appearance.LIGHT);
        StackPane newRoot = new StackPane();

        themed.scene.setRoot(newRoot);

        assertSchemeClass(newRoot, "light");
        assertSchemeClass(themed.root, null);

        ThemeManager.setAppearance(Appearance.DARK);

        assertSchemeClass(newRoot, "dark");
        assertSchemeClass(themed.root, null);
    }

    @Test
    @DisplayName("a null choice is rejected, through the setter and through the property, and the choice stays")
    void nullRejected() {
        ThemeManager.setAppearance(Appearance.LIGHT);

        assertThrows(NullPointerException.class, () -> ThemeManager.setAppearance(null));
        ObjectProperty<Appearance> property = ThemeManager.appearanceProperty();
        assertThrows(NullPointerException.class, () -> property.set(null));

        assertEquals(Appearance.LIGHT, ThemeManager.getAppearance());
        assertEquals(ColorScheme.LIGHT, ThemeManager.getColorScheme());
    }

    @Test
    @DisplayName("System takes the platform's scheme and follows its changes; no scheme reported is Dark")
    void systemFollowsThePlatform() {
        SimpleObjectProperty<ColorScheme> platform = new SimpleObjectProperty<>(ColorScheme.LIGHT);
        ThemeManager.setPlatformColorScheme(platform);
        Themed themed = new Themed();
        List<ColorScheme> changes = new ArrayList<>();
        ChangeListener<ColorScheme> recorder = (observable, oldScheme, newScheme) -> changes.add(newScheme);
        ThemeManager.colorSchemeProperty().addListener(recorder);
        try {
            followPlatform(platform, themed);
        } finally {
            ThemeManager.colorSchemeProperty().removeListener(recorder);
        }

        assertEquals(List.of(ColorScheme.LIGHT, ColorScheme.DARK, ColorScheme.LIGHT, ColorScheme.DARK), changes);
        assertEquals(Appearance.SYSTEM, ThemeManager.getAppearance(), "the choice stays System");
    }

    private static void followPlatform(ObjectProperty<ColorScheme> platform, Themed themed) {
        ThemeManager.setAppearance(Appearance.SYSTEM);

        assertEquals(ColorScheme.LIGHT, ThemeManager.getColorScheme());
        themed.assertColors(LIGHT_WINDOW, LIGHT_FIELD, LIGHT_TEXT);

        platform.set(ColorScheme.DARK);

        assertEquals(ColorScheme.DARK, ThemeManager.getColorScheme());
        themed.assertColors(DARK_WINDOW, DARK_FIELD, DARK_TEXT);

        platform.set(ColorScheme.LIGHT);
        assertEquals(ColorScheme.LIGHT, ThemeManager.getColorScheme());

        platform.set(null);
        assertEquals(ColorScheme.DARK, ThemeManager.getColorScheme(), "no scheme reported");
        themed.assertColors(DARK_WINDOW, DARK_FIELD, DARK_TEXT);
    }

    @Test
    @DisplayName("while Light or Dark is chosen, the platform's scheme changes nothing")
    void choiceWinsOverThePlatform() {
        SimpleObjectProperty<ColorScheme> platform = new SimpleObjectProperty<>(ColorScheme.DARK);
        ThemeManager.setPlatformColorScheme(platform);
        ThemeManager.setAppearance(Appearance.LIGHT);

        platform.set(ColorScheme.LIGHT);
        platform.set(ColorScheme.DARK);
        assertEquals(ColorScheme.LIGHT, ThemeManager.getColorScheme());

        ThemeManager.setAppearance(Appearance.DARK);
        platform.set(ColorScheme.LIGHT);
        assertEquals(ColorScheme.DARK, ThemeManager.getColorScheme());
    }

    /**
     * A popup copies the stylesheets of its window when it opens, and its content looks the theme's colours up
     * through the control that owns it. A test shows no window, so the list is read before it opens: with the colours
     * it opens with.
     */
    @Test
    @DisplayName("a combo box's list follows a switch of its window's scheme")
    void comboListFollows() {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("one", "two"));
        VBox root = new VBox(combo);
        ThemeManager.apply(new Scene(root));
        root.applyCss();
        ListView<?> list = (ListView<?>) ((ComboBoxListViewSkin<?>) combo.getSkin()).getPopupContent();
        list.applyCss();
        assertEquals(DARK_FIELD, fill(list, 0));

        ThemeManager.setAppearance(Appearance.LIGHT);

        list.applyCss();
        assertEquals(LIGHT_FIELD, fill(list, 0));
    }

    /**
     * A collection may leave a weakly held object for a later one, so the test asks for collections until the scene
     * is gone, for at most {@link #COLLECTION_PATIENCE_SECONDS} seconds: a scene the theme kept would stay for good.
     */
    @Test
    @DisplayName("a themed scene nobody holds any more is not kept alive by the theme")
    void forgetsScenesNobodyHolds() throws InterruptedException {
        WeakReference<Scene> scene = themedSceneNobodyHolds();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(COLLECTION_PATIENCE_SECONDS);

        while (scene.get() != null && System.nanoTime() < deadline) {
            System.gc();
            Thread.sleep(20);
        }

        assertNull(scene.get(), "the scene is still reachable after " + COLLECTION_PATIENCE_SECONDS
                + " seconds of garbage collection");
    }

    /**
     * A themed scene that nothing but the returned weak reference refers to: it is built in a method of its own, so
     * that no variable of the test holds it.
     */
    private static WeakReference<Scene> themedSceneNobodyHolds() {
        return new WeakReference<>(new Themed().scene);
    }

    private static void assertSchemeClass(Parent root, String expected) {
        boolean light = hasPseudoClass(root, "light");
        boolean dark = hasPseudoClass(root, "dark");
        assertEquals("light".equals(expected), light, "light on " + root);
        assertEquals("dark".equals(expected), dark, "dark on " + root);
    }

    private static boolean hasPseudoClass(Parent root, String name) {
        for (PseudoClass pseudoClass : root.getPseudoClassStates()) {
            if (pseudoClass.getPseudoClassName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * An application's stylesheet as a data URL.
     */
    private static String css(String rules) {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(rules.getBytes(StandardCharsets.UTF_8));
    }

    private static Paint fill(Region region, int layer) {
        return region.getBackground().getFills().get(layer).getFill();
    }

    /**
     * A scene with the theme applied that is never shown: its root stands for the window, a text field and a label in
     * it for the controls.
     */
    private static final class Themed {

        private final VBox root;
        private final TextField field = new TextField("text");
        private final Label label = new Label("label");
        private final Scene scene;

        Themed() {
            root = new VBox(field, label);
            scene = new Scene(root);
            ThemeManager.apply(scene);
        }

        /**
         * Adds a region with the style class {@code probe}, for an application's own rules, styled.
         */
        Region probe() {
            Region probe = new Region();
            probe.getStyleClass().add("probe");
            root.getChildren().add(probe);
            root.applyCss();
            return probe;
        }

        void assertColors(Color window, Color fieldBackground, Color text) {
            root.applyCss();
            assertEquals(window, fill(root, 0), "the window's background");
            assertEquals(fieldBackground, fill(field, 1), "the text field's background");
            assertEquals(text, label.getTextFill(), "the label's text");
        }
    }
}
