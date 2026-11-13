package org.karthu.fx.markdown;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * How the reader selects a table's cells, as in a spreadsheet: a press on a cell selects it alone, a drag from there
 * selects the rectangle from the pressed cell to the cell under the pointer, shift plus a press the rectangle from the
 * anchor, the shortcut key plus a press adds or removes one cell, and shift plus an arrow key moves the rectangle's
 * far corner by a cell; shift plus space extends the selection to whole rows. A rectangle always spans the shown
 * columns between its corners in their shown order and the rows between them in the shown order, the order of the
 * table's items.
 *
 * <p>The <em>anchor</em> is the cell a rectangle starts from: the cell pressed last without shift. Selections made
 * otherwise (the table's own keys, such as a plain arrow key, or a sort that clears the selection) leave it behind; an
 * anchor no longer selected gives way to the focused cell, the rectangle's far corner, which the table's selection
 * model moves along with every selection.
 *
 * <p>The table's own selection behaviour does the rest: the shortcut key plus A selects all cells, a plain arrow key
 * moves to the next cell. Its press handling is replaced (it has no drag and keeps an anchor of its own), and so are
 * shift plus an arrow key, which there extends a selection cell by cell along a path, and shift plus space, which
 * there selects from its own anchor to the focused cell.
 */
final class CellSelection {

    private final MarkdownTable table;

    /**
     * The cell rectangles start from, {@code null} before the first press; see the class comment.
     */
    private TablePosition<MarkdownTable.Row, ?> anchor;

    /**
     * Where the rectangle of the drag in progress starts and where it last ended; {@code null} while the primary
     * button is not held down after a press on a cell.
     */
    private TablePosition<MarkdownTable.Row, ?> dragStart;
    private TablePosition<MarkdownTable.Row, ?> dragEnd;

    /**
     * Installs the selection gestures on a table. The table's event filters keep them.
     *
     * @param table the table
     * @return the gestures, for the selections the table makes itself ({@link #selectColumn(TableColumn)})
     */
    static CellSelection install(MarkdownTable table) {
        return new CellSelection(table);
    }

    private CellSelection(MarkdownTable table) {
        this.table = table;
        // Filters: the gestures take the events before the cells' and the table's own behaviour does.
        table.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> pressed(event));
        table.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> dragged(event));
        table.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> released(event));
        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> keyPressed(event));
    }

    /**
     * A press with the primary button on a body cell gives the table the keyboard focus and selects, see the class
     * comment. Consumed: neither the cell's behaviour nor the view's scroll pane, which would take the focus, sees it.
     * A press elsewhere, such as on a header, is the table's.
     */
    private void pressed(MouseEvent event) {
        dragStart = null;
        dragEnd = null;
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        TablePosition<MarkdownTable.Row, ?> pressed = bodyCell(event);
        if (pressed == null) {
            return;
        }
        event.consume();
        table.requestFocus();
        if (event.isShortcutDown()) {
            toggle(pressed);
            anchor = pressed;
        } else if (event.isShiftDown()) {
            TablePosition<MarkdownTable.Row, ?> start = anchor();
            anchor = start == null ? pressed : start;
            dragStart = anchor;
            selectRectangle(anchor, pressed);
        } else {
            anchor = pressed;
            dragStart = pressed;
            selectRectangle(pressed, pressed);
        }
        dragEnd = pressed;
    }

    /**
     * A drag after a press on a cell selects the rectangle from where the press started it to the cell under the
     * pointer, or the nearest cell when the pointer is beyond the table's edges. Consumed, so the view's scroll pane
     * does not pan the document.
     */
    private void dragged(MouseEvent event) {
        if (dragStart == null || !event.isPrimaryButtonDown()) {
            return;
        }
        event.consume();
        TablePosition<MarkdownTable.Row, ?> under = table.cellAt(event.getSceneX(), event.getSceneY());
        if (under != null && !under.equals(dragEnd)) {
            dragEnd = under;
            selectRectangle(dragStart, under);
        }
    }

    private void released(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            dragStart = null;
            dragEnd = null;
        }
    }

    /**
     * Shift plus an arrow key or space, see the class comment. Consumed, so the table's own behaviour does not handle
     * it too.
     */
    private void keyPressed(KeyEvent event) {
        if (!event.isShiftDown() || event.isShortcutDown() || event.isAltDown()) {
            return;
        }
        switch (event.getCode()) {
            case UP, KP_UP -> extend(-1, 0);
            case DOWN, KP_DOWN -> extend(1, 0);
            case LEFT, KP_LEFT -> extend(0, -1);
            case RIGHT, KP_RIGHT -> extend(0, 1);
            case SPACE -> wholeRows();
            default -> {
                return;
            }
        }
        event.consume();
    }

    /**
     * Moves the far corner of the rectangle from the anchor by rows and columns, within the table's cells.
     */
    private void extend(int rows, int columns) {
        TablePosition<MarkdownTable.Row, ?> start = anchor();
        if (start == null) {
            return;
        }
        TablePosition<MarkdownTable.Row, ?> corner = farCorner(start);
        List<TableColumn<MarkdownTable.Row, ?>> shown = table.getVisibleLeafColumns();
        int row = Math.clamp(corner.getRow() + rows, 0, table.getItems().size() - 1);
        int column = Math.clamp(shown.indexOf(corner.getTableColumn()) + columns, 0, shown.size() - 1);
        anchor = start;
        selectRectangle(start, new TablePosition<>(table, row, shown.get(column)));
    }

    /**
     * Extends the selection to every shown cell of the rows it touches, or of the focused cell's row while nothing is
     * selected. The anchor moves to the first shown column of its row and the focus to the last of its row: the
     * rectangle between them spans whole rows, so shift plus an arrow key up or down extends the selection by whole
     * rows from there.
     */
    private void wholeRows() {
        TablePosition<MarkdownTable.Row, ?> start = anchor();
        if (start == null) {
            return;
        }
        TablePosition<MarkdownTable.Row, ?> corner = farCorner(start);
        TableView.TableViewSelectionModel<MarkdownTable.Row> model = table.getSelectionModel();
        SortedSet<Integer> rows = new TreeSet<>();
        for (TablePosition<?, ?> cell : model.getSelectedCells()) {
            rows.add(cell.getRow());
        }
        if (rows.isEmpty()) {
            rows.add(corner.getRow());
        }
        List<TableColumn<MarkdownTable.Row, ?>> shown = table.getVisibleLeafColumns();
        for (int row : rows) {
            model.selectRange(row, shown.getFirst(), row, shown.getLast());
        }
        anchor = new TablePosition<>(table, start.getRow(), shown.getFirst());
        table.getFocusModel().focus(corner.getRow(), shown.getLast());
    }

    /**
     * Selects every cell of a shown column, as a rectangle from the column's first row, the anchor, to its last, the
     * focused far corner: shift plus a click or an arrow key goes on from the top of the column. Nothing happens for
     * a column the table does not show or a table without rows.
     *
     * @param column the column
     */
    void selectColumn(TableColumn<MarkdownTable.Row, ?> column) {
        if (table.getItems().isEmpty() || !table.getVisibleLeafColumns().contains(column)) {
            return;
        }
        anchor = new TablePosition<>(table, 0, column);
        selectRectangle(anchor, new TablePosition<>(table, table.getItems().size() - 1, column));
    }

    /**
     * Selects one cell more, or one less if it is selected; the focus moves to it.
     */
    private void toggle(TablePosition<MarkdownTable.Row, ?> cell) {
        TableView.TableViewSelectionModel<MarkdownTable.Row> model = table.getSelectionModel();
        if (model.isSelected(cell.getRow(), cell.getTableColumn())) {
            model.clearSelection(cell.getRow(), cell.getTableColumn());
            table.getFocusModel().focus(cell.getRow(), cell.getTableColumn());
        } else {
            model.select(cell.getRow(), cell.getTableColumn());
        }
    }

    /**
     * Makes the rectangle between two cells the whole selection; the focus moves to the second, its far corner.
     */
    private void selectRectangle(TablePosition<MarkdownTable.Row, ?> start, TablePosition<MarkdownTable.Row, ?> end) {
        TableView.TableViewSelectionModel<MarkdownTable.Row> model = table.getSelectionModel();
        model.clearSelection();
        model.selectRange(start.getRow(), start.getTableColumn(), end.getRow(), end.getTableColumn());
    }

    /**
     * The anchor while it is selected, else the focused cell; {@code null} if neither is a shown cell.
     */
    private TablePosition<MarkdownTable.Row, ?> anchor() {
        if (isShownCell(anchor) && table.getSelectionModel().isSelected(anchor.getRow(), anchor.getTableColumn())) {
            return anchor;
        }
        return focusedCell();
    }

    /**
     * The far corner of a rectangle from a start: the focused cell, or the start itself while no shown cell has the
     * focus.
     */
    private TablePosition<MarkdownTable.Row, ?> farCorner(TablePosition<MarkdownTable.Row, ?> start) {
        TablePosition<MarkdownTable.Row, ?> focused = focusedCell();
        return focused == null ? start : focused;
    }

    /**
     * The focused cell, {@code null} if the focus is on no shown cell.
     */
    private TablePosition<MarkdownTable.Row, ?> focusedCell() {
        TablePosition<MarkdownTable.Row, ?> focused = table.getFocusModel().getFocusedCell();
        return isShownCell(focused) ? focused : null;
    }

    /**
     * Whether a position is a cell the table shows: a row it has, a column it shows.
     */
    private boolean isShownCell(TablePosition<MarkdownTable.Row, ?> cell) {
        return cell != null && cell.getRow() >= 0 && cell.getRow() < table.getItems().size()
                && cell.getTableColumn() != null && table.getVisibleLeafColumns().contains(cell.getTableColumn());
    }

    /**
     * The body cell a mouse event happened on: the cell around the node it was picked on, the cell's text run as a
     * rule. {@code null} outside the table's body cells, on a header for example.
     */
    @SuppressWarnings("unchecked")
    private TablePosition<MarkdownTable.Row, ?> bodyCell(MouseEvent event) {
        TableCell<?, ?> cell = Ancestors.nearest(event.getTarget(), TableCell.class, table);
        if (cell == null || cell.getTableView() != table || cell.isEmpty() || cell.getTableColumn() == null) {
            return null;
        }
        TablePosition<MarkdownTable.Row, ?> position = new TablePosition<>(table, cell.getIndex(),
                (TableColumn<MarkdownTable.Row, ?>) cell.getTableColumn());
        return isShownCell(position) ? position : null;
    }
}
