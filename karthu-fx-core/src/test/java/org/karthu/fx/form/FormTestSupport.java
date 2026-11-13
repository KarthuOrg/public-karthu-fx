package org.karthu.fx.form;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import org.karthu.fx.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the form tests share: the toolkit started without a window, a layout pass in a themed scene that is never
 * shown, events fired at a shell's focus target (a frame's text field), and the shell's nodes looked up by their style
 * classes.
 */
final class FormTestSupport {

    private FormTestSupport() {
    }

    /**
     * Starts the JavaFX toolkit, once for all test classes, and waits until it has set its default stylesheet.
     * Controls cannot be instantiated without the toolkit. No stage is shown, so the controls may live on the test
     * thread. The first control created off the toolkit's thread has the toolkit set its default stylesheet later, on
     * its own thread: a test that applies CSS first thing would race it and find the theme's lookups unresolved. So
     * one control is created here, and the toolkit's thread is waited for.
     *
     * @throws InterruptedException when the wait is interrupted
     */
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

    /**
     * Lays the nodes out one under the other at their preferred sizes, in a scene with the theme that is never shown.
     * A scene sizes its root only when it is shown, so the root is given its preferred size here.
     *
     * @param nodes the nodes to lay out
     */
    static void layOut(Node... nodes) {
        VBox root = new VBox(nodes);
        root.setFillWidth(false);
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        root.applyCss();
        root.autosize();
        root.layout();
    }

    /**
     * Returns whether the node shows the pseudo-class.
     *
     * @param node the node
     * @param name the pseudo-class's name, without the colon
     * @return true while the node shows it
     */
    static boolean hasPseudoClass(Node node, String name) {
        for (PseudoClass pseudoClass : node.getPseudoClassStates()) {
            if (pseudoClass.getPseudoClassName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fires an event at the shell's focus target (a frame's inner text field), where the user types and drops, inside
     * a parent that records what passes the shell unconsumed. The shell leaves the parent afterwards.
     *
     * @param shell the shell, such as a frame
     * @param event the event, such as a key press or a drop
     * @return the events of the event's type that passed the shell, empty when the shell consumed it
     */
    static List<Event> firesThrough(FieldShell shell, Event event) {
        List<Event> passed = new ArrayList<>();
        VBox parent = new VBox(shell);
        parent.addEventHandler(event.getEventType(), passed::add);
        shell.getFocusTarget().fireEvent(event);
        parent.getChildren().clear();
        return passed;
    }

    /**
     * A key press without modifiers.
     *
     * @param code the key
     * @return the key event
     */
    static KeyEvent keyPressed(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    /**
     * A key press with the Control key down.
     *
     * @param code the key
     * @return the key event
     */
    static KeyEvent controlKeyPressed(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, true, false, false);
    }

    /**
     * A key press with the Alt key down.
     *
     * @param code the key
     * @return the key event
     */
    static KeyEvent altKeyPressed(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, true, false);
    }

    /**
     * A key press with the platform's shortcut key down: Command on macOS, Control elsewhere.
     *
     * @param code the key
     * @return the key event
     */
    static KeyEvent shortcutKeyPressed(KeyCode code) {
        boolean mac = System.getProperty("os.name", "").startsWith("Mac");
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, !mac, false, mac);
    }

    /**
     * A key press with the Shift key down.
     *
     * @param code the key
     * @return the key event
     */
    static KeyEvent shiftKeyPressed(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, true, false, false, false);
    }

    /**
     * Returns the shell's {@code {}} icon.
     *
     * @param shell the shell, such as a frame
     * @return the {@code {}} icon
     */
    static Button bindIcon(FieldShell shell) {
        return (Button) shell.lookup(".form-frame-bind");
    }

    /**
     * Returns the shell's chip, the label with the bound name.
     *
     * @param shell the shell, such as a frame
     * @return the chip
     */
    static Label chip(FieldShell shell) {
        return (Label) shell.lookup(".form-chip");
    }

    /**
     * A region filled with a theme token, so that a test compares against the token, not against its hex value. It
     * takes the token's colour in a CSS pass with the theme, such as {@link #layOut(Node...)}.
     *
     * @param token the token, such as {@code -fx-accent-color}
     * @return the swatch
     */
    static Region tokenSwatch(String token) {
        Region swatch = new Region();
        swatch.setStyle("-fx-background-color: " + token + ";");
        return swatch;
    }

    /**
     * Returns the colour of a region's first background layer: a swatch's token, a frame's border.
     *
     * @param region the region, styled
     * @return the colour of its first background layer
     */
    static Paint fill(Region region) {
        return fill(region, 0);
    }

    /**
     * Returns the colour of a region's background layer: 0 a frame's border, 1 its background.
     *
     * @param region the region, styled
     * @param layer  the layer's index
     * @return the colour of that layer
     */
    static Paint fill(Region region, int layer) {
        return region.getBackground().getFills().get(layer).getFill();
    }

    /**
     * A stand-in for the clipboard: it holds a text to paste and records what is copied. No test reads or writes the
     * clipboard of the machine it runs on.
     */
    static final class RecordingClipboard implements ShellClipboard {

        private final String text;
        private final List<String> copied = new ArrayList<>();

        /**
         * Creates a clipboard that holds a text.
         *
         * @param text the text a paste reads, or null for none
         */
        RecordingClipboard(String text) {
            this.text = text;
        }

        @Override
        public String text() {
            return text;
        }

        @Override
        public void put(String copiedText) {
            copied.add(copiedText);
        }

        /**
         * Returns what was copied, in order.
         *
         * @return the copied texts
         */
        List<String> copied() {
            return copied;
        }
    }
}
