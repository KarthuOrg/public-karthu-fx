package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * A list item's row: a box of the item's blocks, indented by its left padding, with the item's marker laid into that
 * padding.
 *
 * <p>The marker sits right-aligned against the blocks and on the baseline of their first line, as a browser puts a
 * list marker outside the item's box. So the blocks of all items of a list start at the same place even when the
 * markers differ in width ({@code 9.} and {@code 10.}), and a marker wider than the padding reaches further left
 * instead of being cut. The marker is unmanaged: the box's size is its blocks', the padding included.
 */
final class ListItemBox extends VBox {

    private final Label marker;

    /**
     * Creates an item row.
     *
     * @param marker the item's marker
     * @param blocks the nodes of the item's blocks
     */
    ListItemBox(Label marker, List<Node> blocks) {
        this.marker = marker;
        getStyleClass().add("markdown-list-item");
        marker.setManaged(false);
        getChildren().add(marker);
        getChildren().addAll(blocks);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double width = snapSizeX(marker.prefWidth(-1));
        double height = snapSizeY(marker.prefHeight(width));
        marker.resizeRelocate(snappedLeftInset() - width, markerY(), width, height);
    }

    /**
     * The marker's top: on the baseline of the first block's first line, or at the top for an empty item or a first
     * block without baseline.
     */
    private double markerY() {
        for (Node child : getChildren()) {
            if (child.isManaged()) {
                double baseline = child.getBaselineOffset();
                if (baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                    return child.getLayoutY();
                }
                return child.getLayoutY() + child.getLayoutBounds().getMinY() + baseline - marker.getBaselineOffset();
            }
        }
        return snappedTopInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        return atLeastMarkerHeight(super.computePrefHeight(width));
    }

    @Override
    protected double computeMinHeight(double width) {
        return atLeastMarkerHeight(super.computeMinHeight(width));
    }

    /**
     * A height raised to at least the marker's, with the box's insets: an empty item has no blocks, but its marker
     * still needs its height, else the next item covers it.
     */
    private double atLeastMarkerHeight(double height) {
        return Math.max(height, snappedTopInset() + marker.prefHeight(-1) + snappedBottomInset());
    }
}
