package org.karthu.fx.markdown;

import javafx.event.EventTarget;
import javafx.scene.Node;

/**
 * Finds what an event happened in: the walk from an event's target up through its parents, which the view's context
 * menu and a table's mouse handling share.
 */
final class Ancestors {

    private Ancestors() {
    }

    /**
     * The nearest node of a type from an event's target up through its parents, the target included, below the node
     * where the walk stops.
     *
     * @param target where the event happened
     * @param type   the type of node looked for, a class or an interface
     * @param stop   the node the walk stops at, not itself a candidate; {@code null} walks up to the scene's root
     * @param <T>    the type of node looked for
     * @return the nearest node of the type, {@code null} if none lies below {@code stop} or the target is no node
     */
    static <T> T nearest(EventTarget target, Class<T> type, Node stop) {
        if (!(target instanceof Node node)) {
            return null;
        }
        for (Node current = node; current != null && current != stop; current = current.getParent()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
        }
        return null;
    }
}
