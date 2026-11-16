package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

import java.util.function.IntFunction;

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

    /**
     * The first of nodes that lie one below the other in their order which reaches down into the area, found by
     * halving: the search's segments with matches, a table's rows. A long document or table measures a few of them,
     * not every one above the area.
     *
     * @param count  the number of nodes
     * @param nodeAt the node at an index of the order, {@code null} for one not laid out, which counts as reaching
     *               into the area
     * @return the index of the first node that reaches into the area, {@code count} if none does
     */
    int firstReaching(int count, IntFunction<Node> nodeAt) {
        int low = 0;
        int high = count;
        while (low < high) {
            int middle = (low + high) >>> 1;
            Node node = nodeAt.apply(middle);
            if (node != null && boundsOf(node).getMaxY() < top) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }
}
