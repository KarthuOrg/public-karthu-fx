package org.karthu.fx.markdown;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * The click a link follows and an image opens on, the one place of its rule: a single click with the primary button
 * without moving since the press. The second click of a double click is none, so a double click follows a link once,
 * and neither is a click after the pointer moved since the press (JavaFX sends one when the release is on the pressed
 * node), which ends a drag that selects text.
 */
final class StillClick {

    private StillClick() {
    }

    /**
     * Whether a click follows a link or opens an image.
     *
     * @param event the click
     * @return {@code true} for a single click with the primary button without moving since the press
     */
    static boolean matches(MouseEvent event) {
        return event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1 && event.isStillSincePress();
    }
}
