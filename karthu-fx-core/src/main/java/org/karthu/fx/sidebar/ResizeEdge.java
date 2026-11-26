package org.karthu.fx.sidebar;

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * The resize edge on a side bar panel's inner side, between the panel and the application's content: a drag with the
 * primary button sets the side bar's content width, which keeps itself within its limits, so that a drag below the
 * smallest width stops there and never collapses the side bar. A width the application has bound is the binding's,
 * and the edge leaves it.
 */
final class ResizeEdge extends Region {

    /**
     * How wide the edge is, its line towards the content included: a grip the pointer finds without aiming at a single
     * pixel. Set in code, not by the theme, so that the edge has its grip without the theme too.
     */
    private static final double WIDTH = 5;

    /** The side bar's content width, which a drag sets. */
    private final DoubleProperty contentWidth;
    /** The side bar's side, which decides whether a move to the right widens the panel or narrows it. */
    private final ObservableValue<Side> side;
    /** Where the drag under way began; null while there is none. */
    private DragStart dragStart;

    /**
     * Creates the resize edge of a side bar.
     *
     * @param contentWidth the side bar's content width, which a drag sets
     * @param side         the side bar's side
     */
    ResizeEdge(DoubleProperty contentWidth, ObservableValue<Side> side) {
        this.contentWidth = contentWidth;
        this.side = side;
        getStyleClass().add("side-bar-resize-edge");
        setCursor(Cursor.H_RESIZE);
        setMinWidth(WIDTH);
        setPrefWidth(WIDTH);
        setMaxWidth(WIDTH);
        addEventHandler(MouseEvent.MOUSE_PRESSED, this::pressed);
        addEventHandler(MouseEvent.MOUSE_DRAGGED, this::dragged);
        addEventHandler(MouseEvent.MOUSE_RELEASED, this::released);
    }

    /** The primary button pressed: a drag begins, from the pointer's place and today's width, unless it is bound. */
    private void pressed(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && !contentWidth.isBound()) {
            dragStart = new DragStart(event.getSceneX(), contentWidth.get());
            event.consume();
        }
    }

    /**
     * The edge dragged: the panel's inner side follows the pointer, in whole pixels. On the left the inner side is the
     * panel's right, so a move to the right widens it; on the right mirrored. The platform sends the drag to the edge
     * also where the pointer has left it.
     */
    private void dragged(MouseEvent event) {
        if (dragStart != null) {
            double moved = event.getSceneX() - dragStart.sceneX();
            double wanted = side.getValue() == Side.RIGHT ? dragStart.width() - moved : dragStart.width() + moved;
            contentWidth.set(Math.round(wanted));
            event.consume();
        }
    }

    /** The button released: the drag ends where it is. */
    private void released(MouseEvent event) {
        if (dragStart != null) {
            dragStart = null;
            event.consume();
        }
    }

    /**
     * Where a drag began.
     *
     * @param sceneX the pointer's x in the scene
     * @param width  the content width then
     */
    private record DragStart(double sceneX, double width) {
    }
}
