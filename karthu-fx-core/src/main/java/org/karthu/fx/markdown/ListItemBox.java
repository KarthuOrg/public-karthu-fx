package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

/**
 * A list item's row: a box of the item's blocks, indented by its left padding, with the item's marker laid into that
 * padding.
 *
 * <p>The marker sits right-aligned against the blocks and on the baseline of their first line, as a browser puts a
 * list marker outside the item's box. So the blocks of all items of a list start at the same place even when the
 * markers differ in width ({@code 9.} and {@code 10.}), and a marker wider than the padding reaches further left
 * instead of being cut. The marker is unmanaged: the box's size is its blocks', the padding included.
 *
 * <p><b>Task items.</b> A task item's check box ({@link TaskBox}) sits in the padding too, {@link #TASK_BOX_GAP} left
 * of the blocks, and takes the place of a bullet: a bullet task item has no marker label. An ordered task item's
 * number stands left of the box, its right edge at the box, on the first line's baseline as always. Vertically the
 * box is centred on the middle of the first line's lowercase letters, a quarter of the line's font size above its
 * baseline, as a browser's {@code vertical-align: middle} does. The box is unmanaged like the marker.
 */
final class ListItemBox extends VBox {

    /**
     * The room between a task's check box and the item's blocks: with the theme's 14 pixel box it fills a bullet
     * item's 18 pixel indent exactly, so a bullet list keeps its indent with tasks in it.
     */
    static final double TASK_BOX_GAP = 4;

    /**
     * How far above the baseline the middle of the lowercase letters lies, in parts of the font size.
     */
    private static final double LOWERCASE_MIDDLE = 0.25;

    /**
     * The marker label, {@code null} for a bullet task item, whose check box takes its place.
     */
    private final Label marker;

    /**
     * The task's check box, {@code null} for an item that is no task.
     */
    private final TaskBox taskBox;

    /**
     * Creates the row of a plain item.
     *
     * @param marker the item's marker
     * @param blocks the nodes of the item's blocks
     */
    ListItemBox(Label marker, List<Node> blocks) {
        this(marker, null, blocks);
    }

    /**
     * Creates an item row.
     *
     * @param marker  the item's marker, {@code null} for a bullet task item
     * @param taskBox the task's check box, {@code null} for an item that is no task
     * @param blocks  the nodes of the item's blocks
     */
    ListItemBox(Label marker, TaskBox taskBox, List<Node> blocks) {
        this.marker = marker;
        this.taskBox = taskBox;
        getStyleClass().add("markdown-list-item");
        if (marker != null) {
            marker.setManaged(false);
            getChildren().add(marker);
        }
        if (taskBox != null) {
            getStyleClass().add("markdown-task-item");
            pseudoClassStateChanged(TaskBox.CHECKED, taskBox.isChecked());
            taskBox.setManaged(false);
            getChildren().add(taskBox);
        }
        getChildren().addAll(blocks);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double right = snappedLeftInset();
        if (taskBox != null) {
            double width = snapSizeX(taskBox.prefWidth(-1));
            double height = snapSizeY(taskBox.prefHeight(width));
            right = snapPositionX(right - TASK_BOX_GAP - width);
            taskBox.resizeRelocate(right, taskBoxY(height), width, height);
        }
        if (marker != null) {
            double width = snapSizeX(marker.prefWidth(-1));
            double height = snapSizeY(marker.prefHeight(width));
            marker.resizeRelocate(right - width, markerY(), width, height);
        }
    }

    /**
     * The first block: the first managed child, or {@code null} for an empty item.
     */
    private Node firstBlock() {
        for (Node child : getChildren()) {
            if (child.isManaged()) {
                return child;
            }
        }
        return null;
    }

    /**
     * The marker's top: on the baseline of the first block's first line, or at the top for an empty item or a first
     * block without baseline.
     */
    private double markerY() {
        double baselineY = firstBaselineY();
        if (Double.isNaN(baselineY)) {
            return firstBlockTop();
        }
        return baselineY - marker.getBaselineOffset();
    }

    /**
     * The check box's top: its middle on the middle of the first line's lowercase letters, or at the top for an
     * empty task or a first block without baseline.
     */
    private double taskBoxY(double height) {
        double baselineY = firstBaselineY();
        if (Double.isNaN(baselineY)) {
            return firstBlockTop();
        }
        return snapPositionY(baselineY - LOWERCASE_MIDDLE * fontSize(firstBlock()) - height / 2);
    }

    /**
     * The baseline of the first block's first line in this box, {@code NaN} for an empty item or a first block
     * without baseline.
     */
    private double firstBaselineY() {
        Node block = firstBlock();
        if (block == null) {
            return Double.NaN;
        }
        double baseline = block.getBaselineOffset();
        if (baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
            return Double.NaN;
        }
        return block.getLayoutY() + block.getLayoutBounds().getMinY() + baseline;
    }

    /**
     * Where the marker and the check box go without a baseline: the first block's top, the top inset for an empty
     * item.
     */
    private double firstBlockTop() {
        Node block = firstBlock();
        if (block == null) {
            return snappedTopInset();
        }
        return block.getLayoutY();
    }

    /**
     * The font size of the first line: of the first block's first text run, else of the marker, else the default.
     */
    private double fontSize(Node block) {
        if (block instanceof TextFlow flow) {
            for (Node child : flow.getChildren()) {
                if (child instanceof Text text) {
                    return text.getFont().getSize();
                }
            }
        }
        if (marker != null) {
            return marker.getFont().getSize();
        }
        return Font.getDefault().getSize();
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
     * A height raised to at least the marker's and the check box's, with the box's insets: an empty item has no
     * blocks, but its marker still needs its height, else the next item covers it.
     */
    private double atLeastMarkerHeight(double height) {
        double markerHeight = 0;
        if (marker != null) {
            markerHeight = marker.prefHeight(-1);
        }
        if (taskBox != null) {
            markerHeight = Math.max(markerHeight, taskBox.prefHeight(-1));
        }
        return Math.max(height, snappedTopInset() + markerHeight + snappedBottomInset());
    }
}
