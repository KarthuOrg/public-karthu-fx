package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * The part of the document near the view's visible area: the visible area and one visible height above and below it,
 * in the current layout and scroll position. The images decode first there ({@link DocumentImages}), and the search
 * builds its marks there ({@link DocumentSearch}), so the work a scroll brings into view is done before it shows.
 *
 * <p>All of it in the pixels of the scroll pane's content, the zoom box: the document scaled, as the scroll offset is.
 *
 * @param content the scroll pane's content, the box that scales the document
 * @param top     the area's top, one visible height above the visible area's
 * @param bottom  the area's bottom, one visible height below the visible area's
 */
record NearArea(Node content, double top, double bottom) {

    /**
     * The area near what a scroll pane shows now.
     *
     * @param scrollPane the view's scroll pane
     * @return the area, {@code null} before the scroll pane is laid out
     */
    static NearArea of(ScrollPane scrollPane) {
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        if (viewportHeight <= 0) {
            return null;
        }
        double offset = VerticalScroll.offset(scrollPane);
        return new NearArea(scrollPane.getContent(), offset - viewportHeight, offset + 2 * viewportHeight);
    }

    /**
     * A node's bounds in the scroll pane's content.
     *
     * @param node a node of the document
     * @return its layout bounds in the content's pixels
     */
    Bounds boundsOf(Node node) {
        return content.sceneToLocal(node.localToScene(node.getLayoutBounds()));
    }

    /**
     * Whether a node lies in the area, wholly or in part.
     *
     * @param node a node of the document, in the scene
     * @return {@code true} if it does
     */
    boolean contains(Node node) {
        Bounds bounds = boundsOf(node);
        return bounds.getMaxY() >= top && bounds.getMinY() <= bottom;
    }
}
