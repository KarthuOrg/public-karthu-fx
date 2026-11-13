package org.karthu.fx.form;

import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.control.Skinnable;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * The window of a suggestion input's popup: a {@link PopupControl} whose content is the input's popup content, shown
 * under the input, in a shown window only, as a popup needs one. While it shows, it watches the window around it: when
 * that window moves the popup closes, and every press of the mouse in that window goes to the input first, which
 * decides whether the press closes the popup. A press in the popup itself is the popup window's own and never reaches
 * the window around it.
 *
 * <p><b>No auto-hide of JavaFX's.</b> The popup does not hide by itself on a press outside it: the input decides, so
 * that the rule is the input's own and testable without a window (a press in the field, such as one that moves the
 * caret, keeps the popup open; any other press in the window closes it). The popup still hides with the window around
 * it, and the input closes it when the window goes to the background, as its text field then loses the focus.
 *
 * <p>Not exported: the suggestion input's implementation.
 */
final class SuggestionPopup {

    private final PopupControl popup = new PopupControl();
    /** The input the popup shows under: the popup's owner node. */
    private final Region field;
    /** What the input does when the popup's window hid, by the input or by itself (the window around it hiding). */
    private final Runnable hidden;
    /** Closes the input's popup when the window around it moves. */
    private final InvalidationListener windowMoved;
    /** Hands each press in the window around the popup to the input, ahead of the press's target. */
    private final EventHandler<MouseEvent> pressed;
    /** The window around the popup while the popup's window shows, else null. */
    private Window watchedWindow;

    /**
     * Creates the window of an input's popup, not showing.
     *
     * @param field   the input the popup shows under
     * @param content the popup's content, which the window shows
     * @param closer  what closes the input's popup, the input's own closing, which then hides this window
     * @param hidden  what the input does when the popup's window hid
     * @param pressed what a press in the window around the popup does, while the popup's window shows
     */
    SuggestionPopup(Region field, Region content, Runnable closer, Runnable hidden, EventHandler<MouseEvent> pressed) {
        this.field = field;
        this.hidden = hidden;
        this.pressed = pressed;
        this.windowMoved = observable -> closer.run();
        // The input decides on a press outside the popup (see the class description), and Escape is the input's too
        // (it closes the popup first, see SearchInput).
        popup.setAutoHide(false);
        popup.setAutoFix(true);
        popup.setHideOnEscape(false);
        popup.setSkin(new Skin<Skinnable>() {
            @Override
            public Skinnable getSkinnable() {
                return popup;
            }

            @Override
            public Node getNode() {
                return content;
            }

            @Override
            public void dispose() {
                // Nothing to release: the content stays the popup's.
            }
        });
        popup.setOnHidden(event -> windowHidden());
    }

    /**
     * Shows the popup's window under the input, in a shown window only, and watches that window: its moves and the
     * presses in it.
     */
    void show() {
        Scene scene = field.getScene();
        Window window = scene == null ? null : scene.getWindow();
        if (window == null || !window.isShowing()) {
            return;
        }
        Bounds bounds = field.localToScreen(field.getLayoutBounds());
        popup.show(field, bounds.getMinX(), bounds.getMaxY());
        watchedWindow = window;
        window.xProperty().addListener(windowMoved);
        window.yProperty().addListener(windowMoved);
        window.addEventFilter(MouseEvent.MOUSE_PRESSED, pressed);
    }

    /** Hides the popup's window if it shows. */
    void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    /**
     * The popup's window hid, by the input or by itself (the window around it hiding): the window around it is no
     * longer watched, and the input learns it.
     */
    private void windowHidden() {
        if (watchedWindow != null) {
            watchedWindow.xProperty().removeListener(windowMoved);
            watchedWindow.yProperty().removeListener(windowMoved);
            watchedWindow.removeEventFilter(MouseEvent.MOUSE_PRESSED, pressed);
            watchedWindow = null;
        }
        hidden.run();
    }
}
