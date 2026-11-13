package org.karthu.fx.theme;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The theme's text rendering: every text node draws with LCD (subpixel) smoothing instead of JavaFX's default gray,
 * the text inside controls as well as plain text nodes.
 */
class TextSmoothingTest {

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
    @DisplayName("the text inside labels, buttons and text fields uses LCD smoothing")
    void controlTextUsesLcd() {
        Label label = new Label("Label");
        Button button = new Button("Button");
        TextField field = new TextField("Field");
        themed(label, button, field);

        assertEquals(FontSmoothingType.LCD, innerText(label).getFontSmoothingType());
        assertEquals(FontSmoothingType.LCD, innerText(button).getFontSmoothingType());
        assertEquals(FontSmoothingType.LCD, innerText(field).getFontSmoothingType());
    }

    @Test
    @DisplayName("plain text nodes, also the runs of a text flow, use LCD smoothing")
    void plainTextUsesLcd() {
        Text plain = new Text("Plain");
        Text run = new Text("Run");
        themed(plain, new TextFlow(run));

        assertEquals(FontSmoothingType.LCD, plain.getFontSmoothingType());
        assertEquals(FontSmoothingType.LCD, run.getFontSmoothingType());
    }

    /**
     * Puts the nodes into a scene with the theme that is never shown and applies the CSS and a layout pass, which
     * creates the controls' skins.
     */
    private static void themed(Node... nodes) {
        VBox root = new VBox(nodes);
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        root.applyCss();
        root.layout();
    }

    private static Text innerText(Node control) {
        Node text = control.lookup(".text");
        assertNotNull(text, "no text node inside " + control);
        assertTrue(text instanceof Text, "not a Text: " + text);
        return (Text) text;
    }
}
