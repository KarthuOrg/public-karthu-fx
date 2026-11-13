package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * The block of a table in the document. It gives its table ({@link MarkdownTable}) the document's width, or the
 * table's minimum width when that is wider: the columns cannot be narrower than their longest words. A wider table
 * scrolls sideways under a horizontal bar below it, as a long line of a code block does, and the block is as tall as
 * the table and the bar, so the bar covers no row.
 *
 * <p>The table never scrolls by itself (see {@link MarkdownTable}), so the block does it: it moves the table, clipped
 * to the block, by the bar's value. The vertical part of every scroll event over the block goes on to the document,
 * the horizontal part (shift plus wheel, a touchpad's sideways swipe) moves the bar.
 *
 * <p>The block is a segment of the view's selection across the document ({@link DocumentSelection}), which takes the
 * table whole: it has two positions, before and after the table. A selection that passes over it shows the table as
 * selected whole ({@link MarkdownTable#showSelectedWhole(boolean)}) and copies all of it
 * ({@link MarkdownTable#wholeText()}). The table keeps its own presses: a press on a cell selects cells.
 */
final class TableBlock extends Region implements Segment {

    private final MarkdownTable table;
    private final ScrollBar bar = new ScrollBar();
    private final List<BlockContainer> containers;

    /**
     * Creates the block of a table.
     *
     * @param table      the table
     * @param containers the quotes and list items around the table, the outermost first
     */
    TableBlock(MarkdownTable table, List<BlockContainer> containers) {
        this.table = table;
        this.containers = List.copyOf(containers);
        bar.setOrientation(Orientation.HORIZONTAL);
        bar.setVisible(false);
        bar.setUnitIncrement(20);
        bar.valueProperty().addListener(observable -> placeTable());
        getChildren().addAll(table, bar);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        addEventFilter(ScrollEvent.SCROLL, event -> WheelPassing.passVerticalPartOn(this, event));
        addEventHandler(ScrollEvent.SCROLL, event -> scrollSideways(event));
    }

    /**
     * Horizontal: the table's height depends on the width it gets.
     */
    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    /**
     * Small: a wide table scrolls inside the block instead of making the document wider.
     */
    @Override
    protected double computeMinWidth(double height) {
        return snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computePrefWidth(double height) {
        return snappedLeftInset() + table.minWidth(-1) + snappedRightInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        double inside = width < 0 ? table.minWidth(-1) : width - snappedLeftInset() - snappedRightInset();
        double tableWidth = tableWidth(inside);
        double barHeight = tableWidth > inside ? snapSizeY(bar.prefHeight(-1)) : 0;
        return snappedTopInset() + snapSizeY(table.prefHeight(tableWidth)) + barHeight + snappedBottomInset();
    }

    @Override
    protected void layoutChildren() {
        double inside = getWidth() - snappedLeftInset() - snappedRightInset();
        double tableWidth = tableWidth(inside);
        double overflow = tableWidth - inside;
        double tableHeight = snapSizeY(table.prefHeight(tableWidth));
        table.resize(tableWidth, tableHeight);
        bar.setVisible(overflow > 0);
        if (overflow > 0) {
            bar.setMax(overflow);
            bar.setVisibleAmount(overflow * inside / tableWidth);
            bar.setBlockIncrement(inside);
            bar.setValue(Math.clamp(bar.getValue(), 0, overflow));
            bar.resizeRelocate(snappedLeftInset(), snappedTopInset() + tableHeight, inside,
                    snapSizeY(bar.prefHeight(-1)));
        } else {
            bar.setValue(0);
        }
        placeTable();
    }

    /**
     * The table's width for the width inside the block: that width, or the table's minimum when it is wider.
     */
    private double tableWidth(double inside) {
        return Math.max(inside, snapSizeX(table.minWidth(-1)));
    }

    /**
     * Moves the table left by the bar's value.
     */
    private void placeTable() {
        table.relocate(snappedLeftInset() - bar.getValue(), snappedTopInset());
    }

    @Override
    public Node node() {
        return this;
    }

    /**
     * One: the table is one piece of the selection.
     */
    @Override
    public int length() {
        return 1;
    }

    /**
     * Before the table for a point above its vertical middle, after it for any other: a drag that ends in the
     * table's upper half leaves it out, one that ends in its lower half takes it, whichever way it goes.
     */
    @Override
    public int positionAt(double sceneX, double sceneY) {
        Bounds bounds = localToScene(getLayoutBounds());
        return sceneY < bounds.getCenterY() ? 0 : 1;
    }

    /**
     * The whole table. A press on the table selects its cells, so no double click selects a word here.
     */
    @Override
    public Range wordAt(double sceneX, double sceneY) {
        return new Range(0, 1);
    }

    /**
     * The whole table, as {@link #wordAt(double, double)}.
     */
    @Override
    public Range paragraphAt(double sceneX, double sceneY) {
        return new Range(0, 1);
    }

    @Override
    public void select(int start, int end) {
        table.showSelectedWhole(start < end);
    }

    /**
     * The table as text for the range {@code 0} to {@code 1}, see {@link MarkdownTable#wholeText()}; nothing for an
     * empty range.
     */
    @Override
    public String text(int start, int end) {
        return start < end ? table.wholeText() : "";
    }

    @Override
    public List<BlockContainer> containers() {
        return containers;
    }

    /**
     * Not spaced: a line feed sets the table apart from its neighbours, a blank line from a heading or a paragraph.
     */
    @Override
    public boolean spaced() {
        return false;
    }

    /**
     * Moves the bar by the horizontal part of a scroll event, if the table is wider than the block.
     */
    private void scrollSideways(ScrollEvent event) {
        if (!bar.isVisible() || event.getDeltaX() == 0) {
            return;
        }
        bar.setValue(Math.clamp(bar.getValue() - event.getDeltaX(), 0, bar.getMax()));
        event.consume();
    }
}
