package org.karthu.fx.form;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;

import java.util.function.DoubleSupplier;

/**
 * The skin of a suggestion input's popup list: as tall as its rows up to the visible row count, measured with their own
 * heights, then it scrolls, where a list view would ask for 400 pixels; and it scrolls a row into view as little as
 * needed.
 *
 * <p>Not exported: the suggestion input's implementation.
 *
 * @param <E> the type of the entries
 */
final class SuggestionRowsSkin<E> extends ListViewSkin<E> {

    /** The field's visible row count: the rows the list is as tall as, at most. */
    private final ReadOnlyIntegerProperty visibleRowCount;
    /** The list's width as the field gives it, known before the list is laid out. */
    private final DoubleSupplier listWidth;

    /**
     * Creates the skin of the popup's list.
     *
     * @param list            the popup's list
     * @param visibleRowCount the field's visible row count, which the skin follows
     * @param listWidth       the list's width as the field gives it, known as soon as the field is laid out
     */
    SuggestionRowsSkin(ListView<E> list, ReadOnlyIntegerProperty visibleRowCount, DoubleSupplier listWidth) {
        super(list);
        this.visibleRowCount = visibleRowCount;
        this.listWidth = listWidth;
        // Another count: the list asks for another height, and the popup follows it.
        registerChangeListener(visibleRowCount, observable -> list.requestLayout());
    }

    /**
     * The rows the popup shows at most before it scrolls, and the rows Page Up and Page Down move: the visible row
     * count, at least one.
     *
     * @param visibleRowCount the field's visible row count
     * @return the rows, at least one
     */
    static int rowsAtMost(int visibleRowCount) {
        return Math.max(1, visibleRowCount);
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset,
                                       double leftInset) {
        VirtualFlow<ListCell<E>> flow = getVirtualFlow();
        int entries = getSkinnable().getItems().size();
        int rows = Math.min(entries, rowsAtMost(visibleRowCount.get()));
        double rowWidth = (width >= 0 ? width : listWidth.getAsDouble()) - leftInset - rightInset;
        if (entries > rows) {
            // It scrolls: the vertical scroll bar takes its room from the rows.
            rowWidth -= verticalBarWidth(flow);
        }
        // One pixel more, as a combo box's list has, so rounding never brings up a scroll bar.
        double height = 1;
        for (int i = 0; i < rows; i++) {
            height += rowHeight(flow.getCell(i), rowWidth);
        }
        return topInset + height + bottomInset;
    }

    /**
     * A row's height as the list lays it out, measured already before the list has laid out a row, when the popup's
     * window takes its first size from it: at the width the list gives every row, its own inside its insets, a row of
     * the application that wraps its lines as high as it is wrapped there. Not the size the flow gives its measuring
     * row: before its first layout the flow has no width yet, and a text flow at no width breaks its lines at every
     * character (a popup many times too high). Without a width yet, as the row asks.
     */
    private static double rowHeight(ListCell<?> row, double width) {
        double measured = width > 0 ? width : -1;
        double height = row.prefHeight(measured);
        return Math.max(row.minHeight(measured), Math.min(height, row.maxHeight(measured)));
    }

    /** The width of the flow's vertical scroll bar, which shows while the list scrolls. */
    private double verticalBarWidth(VirtualFlow<ListCell<E>> flow) {
        for (Node child : flow.getChildrenUnmodifiable()) {
            if (child instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                return snapSizeX(bar.prefWidth(-1));
            }
        }
        return 0;
    }

    /**
     * Scrolls the row into view, as little as needed.
     *
     * @param index the row's index
     */
    void reveal(int index) {
        getVirtualFlow().scrollTo(index);
    }
}
