package org.karthu.fx.theme;

import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Utility for setting window frame icons on JavaFX stages.
 */
public class StageIcons {

    private StageIcons() {
        // utility class
    }

    /**
     * Adds the given icon to the stage's icon list.
     * The icon is displayed in the window title bar and taskbar.
     *
     * @param stage the stage
     * @param icon  the icon to add
     */
    public static void setIcon(Stage stage, Image icon) {
        stage.getIcons().add(icon);
    }
}
