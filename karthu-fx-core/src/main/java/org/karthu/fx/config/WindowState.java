package org.karthu.fx.config;

import javafx.stage.Stage;

/**
 * Immutable record representing window position and size.
 *
 * @param x         the window's x position on screen
 * @param y         the window's y position on screen
 * @param width     the window's width
 * @param height    the window's height
 * @param maximized whether the window is maximized
 */
public record WindowState(
        double x,
        double y,
        double width,
        double height,
        boolean maximized
) {

    /**
     * Default window state for new windows.
     */
    public static final WindowState DEFAULT = new WindowState(100, 100, 1024, 768, false);

    /**
     * Creates a WindowState from the current state of a Stage.
     *
     * @param stage the stage to capture state from
     * @return a new WindowState reflecting the stage's current position and size
     */
    public static WindowState fromStage(Stage stage) {
        if (stage.isMaximized()) {
            return new WindowState(
                    stage.getX(),
                    stage.getY(),
                    stage.getWidth(),
                    stage.getHeight(),
                    true
            );
        }
        return new WindowState(
                stage.getX(),
                stage.getY(),
                stage.getWidth(),
                stage.getHeight(),
                false
        );
    }

    /**
     * Applies this window state to a Stage.
     *
     * @param stage the stage to apply the state to
     */
    public void applyTo(Stage stage) {
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setMaximized(maximized);
    }
}
