package org.karthu.fx.sidebar;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.lang.ref.WeakReference;

/**
 * Where the keyboard focus goes back to when a side bar's panel hides while the focus is in it: the node that last had
 * the focus outside the side bar, such as the application's content the user worked in before opening the panel. It
 * follows the focus of the scene the side bar is in, and holds that node weakly, so that a node the application
 * dropped is not kept alive.
 */
final class FocusReturn {

    /** The side bar whose inside the focus leaves. */
    private final Node sideBar;
    /** Where the focus last was outside the side bar; null until it was anywhere. */
    private WeakReference<Node> focusOutside;
    /** Follows the focus of the side bar's scene, while the side bar is in it. */
    private final ChangeListener<Node> focusOwnerChanged =
            (observable, oldOwner, newOwner) -> rememberFocusOutside(newOwner);

    /**
     * Follows the focus of the scenes a side bar enters.
     *
     * @param sideBar the side bar
     */
    FocusReturn(Node sideBar) {
        this.sideBar = sideBar;
        sideBar.sceneProperty().addListener(this::sceneChanged);
    }

    /**
     * Returns whether the focus owner of the side bar's scene lies in the side bar.
     *
     * @return true while the side bar holds the focus
     */
    boolean isFocusInside() {
        Scene scene = sideBar.getScene();
        return scene != null && scene.getFocusOwner() != null && isInside(scene.getFocusOwner());
    }

    /**
     * Puts the focus back where it last was outside the side bar, while that node is still in the side bar's scene,
     * shown and enabled; else leaves it to JavaFX to move it on.
     */
    void returnFocus() {
        Node target = focusOutside == null ? null : focusOutside.get();
        if (target == null || target.getScene() != sideBar.getScene() || target.isDisabled()) {
            return;
        }
        for (Node at = target; at != null; at = at.getParent()) {
            if (!at.isVisible()) {
                return;
            }
        }
        target.requestFocus();
    }

    /** The side bar entered another scene, or left its scene: it follows the focus of the scene it is in. */
    private void sceneChanged(ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.focusOwnerProperty().removeListener(focusOwnerChanged);
        }
        if (newScene != null) {
            newScene.focusOwnerProperty().addListener(focusOwnerChanged);
            rememberFocusOutside(newScene.getFocusOwner());
        }
    }

    /** Remembers the focus owner when it lies outside the side bar. */
    private void rememberFocusOutside(Node owner) {
        if (owner != null && !isInside(owner)) {
            focusOutside = new WeakReference<>(owner);
        }
    }

    /** Whether a node is the side bar or lies in it. */
    private boolean isInside(Node node) {
        for (Node at = node; at != null; at = at.getParent()) {
            if (at == sideBar) {
                return true;
            }
        }
        return false;
    }
}
