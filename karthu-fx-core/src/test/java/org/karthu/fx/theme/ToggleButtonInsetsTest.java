package org.karthu.fx.theme;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The theme's toggle buttons paint their background exactly inside their border in every state. The theme sets one
 * background layer, so modena's insets for its own layers (1 px below the border when selected, 0.2 px around it when
 * focused) let the accent colour show outside the frame.
 */
class ToggleButtonInsetsTest {

    private static final PseudoClass HOVER = PseudoClass.getPseudoClass("hover");
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        // The first control created off the toolkit's thread has the toolkit set its default stylesheet on its own
        // thread: wait for it, so the theme's CSS is not applied before it.
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @Test
    @DisplayName("a plain toggle button keeps its background inside the border: normal, hovered, selected, selected"
            + " and focused, selected and hovered")
    void plainToggleButton() {
        assertInsideBorderInEveryState(new VBox());
    }

    @Test
    @DisplayName("a toggle button of the shortcuts filter keeps its background inside the border in every state")
    void shortcutsFilterToggleButton() {
        HBox filter = new HBox();
        filter.getStyleClass().add("shortcuts-filter");
        assertInsideBorderInEveryState(filter);
    }

    private static void assertInsideBorderInEveryState(Pane parent) {
        ToggleButton button = new ToggleButton("Dark");
        parent.getChildren().add(button);
        VBox root = new VBox(parent);
        Scene scene = new Scene(root, 300, 200);
        ThemeManager.apply(scene);

        assertBackgroundInsideBorder(button, root, "normal");
        button.pseudoClassStateChanged(HOVER, true);
        assertBackgroundInsideBorder(button, root, "hovered");
        button.pseudoClassStateChanged(HOVER, false);
        button.setSelected(true);
        assertBackgroundInsideBorder(button, root, "selected");
        button.pseudoClassStateChanged(FOCUSED, true);
        assertBackgroundInsideBorder(button, root, "selected and focused");
        button.pseudoClassStateChanged(FOCUSED, false);
        button.pseudoClassStateChanged(HOVER, true);
        assertBackgroundInsideBorder(button, root, "selected and hovered");
    }

    private static void assertBackgroundInsideBorder(ToggleButton button, VBox root, String state) {
        root.applyCss();
        root.layout();
        assertNotNull(button.getBackground(), state + ": a background");
        assertFalse(button.getBackground().getFills().isEmpty(), state + ": a background fill");
        for (BackgroundFill fill : button.getBackground().getFills()) {
            assertEquals(Insets.EMPTY, fill.getInsets(), state + ": every fill exactly inside the border");
        }
    }
}
