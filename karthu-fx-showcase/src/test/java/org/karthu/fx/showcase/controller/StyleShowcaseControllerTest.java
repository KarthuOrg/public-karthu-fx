package org.karthu.fx.showcase.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.theme.Appearance;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The showcase window's scheme switch, loaded from the window's FXML in a scene that is never shown: Light, Dark and
 * System in the toolbar, bound to the theme's choice in both directions.
 */
class StyleShowcaseControllerTest {

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
    }

    @Test
    @DisplayName("the switch starts in Dark, the library's default, with its three choices in the toolbar")
    void startsInDark() throws IOException {
        Parent root = load();

        assertEquals(Appearance.DARK, ThemeManager.getAppearance());
        assertTrue(choice(root, "darkScheme").isSelected());
        assertTrue(!choice(root, "lightScheme").isSelected() && !choice(root, "systemScheme").isSelected());
        assertNotNull(root.lookup(".showcase-toolbar #lightScheme"), "the switch sits in the toolbar");
    }

    @Test
    @DisplayName("choosing on the switch changes the theme's choice; choosing the chosen one again keeps it")
    void switchChangesTheChoice() throws IOException {
        Parent root = load();

        choice(root, "lightScheme").fire();
        assertEquals(Appearance.LIGHT, ThemeManager.getAppearance());

        choice(root, "systemScheme").fire();
        assertEquals(Appearance.SYSTEM, ThemeManager.getAppearance());

        choice(root, "darkScheme").fire();
        assertEquals(Appearance.DARK, ThemeManager.getAppearance());

        choice(root, "darkScheme").fire();
        assertEquals(Appearance.DARK, ThemeManager.getAppearance());
        assertTrue(choice(root, "darkScheme").isSelected(), "the chosen one stays chosen");
    }

    @Test
    @DisplayName("a choice made elsewhere shows on the switch")
    void switchFollowsTheChoice() throws IOException {
        Parent root = load();

        ThemeManager.setAppearance(Appearance.LIGHT);

        assertTrue(choice(root, "lightScheme").isSelected());
        assertTrue(!choice(root, "darkScheme").isSelected());
    }

    private static Parent load() throws IOException {
        URL fxml = StyleShowcaseControllerTest.class.getResource("/org/karthu/fx/showcase/style-showcase.fxml");
        return new FXMLLoader(fxml).load();
    }

    private static ToggleButton choice(Parent root, String id) {
        ToggleButton choice = (ToggleButton) root.lookup("#" + id);
        assertNotNull(choice, id);
        return choice;
    }
}
