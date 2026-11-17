package org.karthu.fx.command;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.karthu.fx.theme.ThemeManager;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the command tests share: the toolkit started without a window, a themed scene that is never shown, key presses
 * as the platform sends them, and an action that counts its runs.
 */
final class CommandTestSupport {

    private CommandTestSupport() {
    }

    /**
     * Starts the JavaFX toolkit, once for all test classes, and waits until it has set its default stylesheet. No
     * stage is shown, so the controls may live on the test thread. The first control created off the toolkit's thread
     * has the toolkit set its default stylesheet later, on its own thread, so one control is created here and the
     * toolkit's thread is waited for.
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
     * Runs an action on the JavaFX Application Thread and waits for it: for a menu bar, whose skin builds its menus,
     * and with them puts its items' accelerators into the scene, only on that thread.
     *
     * @param action the action
     * @throws InterruptedException when the wait is interrupted
     */
    static void onFxThread(Runnable action) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable thrown) {
                failure.set(thrown);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run the action");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    /**
     * A scene with the theme around the root, laid out at the root's preferred size and never shown. The CSS pass
     * gives the controls their skins.
     *
     * @param root the scene's root
     * @return the scene
     */
    static Scene themedScene(Parent root) {
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        root.applyCss();
        root.autosize();
        root.layout();
        return scene;
    }

    /**
     * A key press as the platform sends it: Ctrl is the platform's shortcut key, Command on macOS, Control elsewhere.
     *
     * @param code      the key
     * @param modifiers the modifiers held down
     * @return the key event
     */
    static KeyEvent keyPressed(KeyCode code, Shortcut.Modifier... modifiers) {
        Set<Shortcut.Modifier> down = Set.of(modifiers);
        boolean ctrl = down.contains(Shortcut.Modifier.CTRL);
        boolean mac = System.getProperty("os.name", "").startsWith("Mac");
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, down.contains(Shortcut.Modifier.SHIFT),
                ctrl && !mac, down.contains(Shortcut.Modifier.ALT), ctrl && mac);
    }

    /**
     * An action that counts how often it ran.
     */
    static final class RunCounter implements Runnable {

        private int runs;

        @Override
        public void run() {
            runs++;
        }

        /**
         * Returns how often the action ran.
         *
         * @return the number of runs
         */
        int runs() {
            return runs;
        }
    }
}
