package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.ScrollEvent;

/**
 * Passes the vertical part of a scroll event over a block on to the block's parent, so the document around the block
 * scrolls as if the block were not there, and leaves the horizontal part to the block. The code and source blocks and
 * the tables do this with an event filter: a scroll pane, a table's too, consumes the whole of an event it scrolls by
 * any part, so without the split a touchpad's slightly diagonal swipe over a block would stop the document.
 */
final class WheelPassing {

    private WheelPassing() {
    }

    /**
     * Consumes an event with a vertical part, fires that part at the block's parent and the horizontal part, if there
     * is one, again at the event's target. The horizontal part passes the block's filter the second time, as it has
     * no vertical part.
     *
     * @param block the block whose filter got the event
     * @param event the scroll event
     */
    static void passVerticalPartOn(Node block, ScrollEvent event) {
        if (event.getDeltaY() == 0 || block.getParent() == null) {
            return;
        }
        event.consume();
        Event.fireEvent(block.getParent(), part(event, 0, event.getDeltaY()));
        if (event.getDeltaX() != 0) {
            Event.fireEvent(event.getTarget(), part(event, event.getDeltaX(), 0));
        }
    }

    /**
     * A copy of a scroll event with only the given deltas, its other values the same.
     */
    private static ScrollEvent part(ScrollEvent event, double deltaX, double deltaY) {
        boolean horizontal = deltaX != 0;
        boolean vertical = deltaY != 0;
        return new ScrollEvent(event.getEventType(), event.getSceneX(), event.getSceneY(),
                event.getScreenX(), event.getScreenY(),
                event.isShiftDown(), event.isControlDown(), event.isAltDown(), event.isMetaDown(),
                event.isDirect(), event.isInertia(), deltaX, deltaY,
                horizontal ? event.getTotalDeltaX() : 0, vertical ? event.getTotalDeltaY() : 0,
                event.getMultiplierX(), event.getMultiplierY(),
                event.getTextDeltaXUnits(), horizontal ? event.getTextDeltaX() : 0,
                event.getTextDeltaYUnits(), vertical ? event.getTextDeltaY() : 0,
                event.getTouchCount(), event.getPickResult());
    }
}
