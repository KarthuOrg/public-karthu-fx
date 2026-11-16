package org.karthu.fx.markdown;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ConstrainedColumnResizeBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Skin;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.CellSkinBase;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import javafx.util.Callback;
import org.karthu.fx.markdown.parser.Inline;
import org.karthu.fx.markdown.parser.Table;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A GFM table as a {@link TableView} ({@code markdown-table}): a column per table column, a row per body row, every
 * cell (header cells too) a {@link TextFlow} of the cell's inlines, formatted like the body text and aligned as the
 * delimiter row says. It sits in a {@link TableBlock}, which scrolls it sideways when it is wider than the document.
 *
 * <p><b>Widths.</b> The columns share the table's width by the {@link ColumnWidths} rule, from what the cells measure
 * in the theme's fonts: each column's widest cell on one line and its longest word, both with what lies around the
 * text. The table is never narrower than its columns' minimums together ({@link #computeMinWidth(double)}), so its
 * columns always fill it exactly. It fits them to every new width before it lays them out ({@link ColumnFit} keeps
 * what they were fitted to and the reader's widths), and the cells wrap their text to their column's width.
 *
 * <p><b>Height.</b> The table is as tall as its header and all its rows, so its flow shows every row. A
 * {@code TableView} has a fixed preferred height of its own; this one computes it for the width it is asked about: it
 * computes the widths that width would give its columns, by the same rule, and adds up the header's and the rows'
 * heights at those widths from the cells' text flows, each row as tall as its tallest cell. It gives the columns no
 * width while it measures: only its layout does. A new width asks the table's parents for a layout, and a scroll pane
 * asks its content's height at two widths in every layout pass, with and without its vertical bar; a table that took
 * either width would start the next pass. The cells of a row the flow shows are measured where they are; a row it
 * does not show yet (before the table's first layout) is measured in a measure row, an unmanaged {@link TableRow} the
 * table adds to its own children for the measurement and removes again, as JavaFX's own fit-to-content of a column
 * does, without asking its parents for a layout. Either way the theme's fonts, padding and row rules apply, so the
 * height is right in the same layout pass in which the width changes.
 *
 * <p><b>No scroll bars of its own.</b> JavaFX's flow decides each scroll bar from the viewport of its previous layout:
 * content that fits exactly in both directions, as here, makes a bar shown once shrink the viewport and bring up the
 * other bar, back and forth with every layout. So the table never scrolls by itself: it is as tall as its rows and as
 * wide as its columns, and its column resize policy is a constrained one, which keeps the flow from ever showing a
 * horizontal bar. Its block scrolls it sideways.
 *
 * <p><b>Cells.</b> A cell's text flow wraps to the width its column offers it, the column's width less what lies
 * around the flow (the cell's padding and border, or in the header the label's and the header's padding, a shown
 * sort arrow and, in the last shown column, the menu button's corner). That width is the flow's maximum, which the
 * cell and the header label lay it out at, and its preferred height is the wrapped text's height, which sizes the
 * row; both follow the column's width as it changes. Its preferred width is its text on one line, which only JavaFX's
 * fit of a column to its content reads. A body cell is exactly as tall as its text flow and its padding: neither its
 * own empty text nor its font adds to its height.
 *
 * <p><b>Column functions.</b> A click on a header sorts the rows by that column's cells, compared by their visible
 * text in {@link NaturalOrder}; the next click sorts descending, the third brings back the source order, and shift
 * plus a click adds a column to the sort ({@code TableView}'s own header behaviour). Rows the sort leaves equal keep
 * their source order. Dragging a header moves its column, the header's menu button shows and hides columns, listed by
 * their plain header text, and dragging a column's right edge resizes it by the {@link ColumnWidths} rule: from then
 * on the table fits its columns in the shares the reader left them with, not by their content. All of it lives in
 * this table, so a new text, which brings new tables, starts it fresh.
 *
 * <p><b>Selection.</b> Cells are selected one by one (cell selection, multiple selection), by the gestures of a
 * spreadsheet that {@link CellSelection} adds, and the table finds the cell under a dragged pointer
 * ({@link #cellAt(double, double)}). Whatever selects cells makes the table the holder of the view's one selection:
 * it reports to the view, which clears the selection held before. A sort clears the selection, since it
 * moves rows under it.
 *
 * <p><b>Copying.</b> The shortcut key plus C, and the view's context menu through {@link SelectionHolder}, copy the
 * selected cells of the shown columns as tab-separated text ({@link #selectionText(boolean)}); the context menu
 * also copies them with their columns' titles and selects a whole column. Only the primary button reaches the column
 * headers, so a right click on one never sorts. Without selected cells the shortcut key goes on to the view.
 *
 * <p><b>In the document's selection.</b> A selection across the document that passes over the table takes it whole
 * (its block, {@link TableBlock}, is the selection's segment): the table then looks selected whole
 * ({@link #showSelectedWhole(boolean)}) and copies as {@link #wholeText()}, while its own selection of cells stays
 * empty.
 *
 * <p><b>Search.</b> The view's search runs through the table's texts in the order it shows them, which the table
 * numbers for it ({@link #numberSearchTexts()}), a cell's text derived from its inlines without drawing it. The table
 * keeps the matches by their cells as the source has them, and the flows of the headers and of the body cells of the
 * rows near the visible area show their cell's matches in the two layers of their {@link SearchMarks}
 * ({@link #showSearchMatches(List, int)}, {@link #showSearchMatchesNear(NearArea)}). A sort and a moved, hidden or
 * shown column call the search back ({@link #onSearchTextsChanged(Runnable)}), which numbers the matches anew; it finds
 * its current match again by the cell as the source has it ({@link #searchTextKey(int)}).
 */
final class MarkdownTable extends TableView<MarkdownTable.Row> implements SelectionHolder {

    private static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

    /**
     * The pseudo-class of a table a selection across the document passes over, see
     * {@link #showSelectedWhole(boolean)}.
     */
    private static final PseudoClass SELECTED_WHOLE = PseudoClass.getPseudoClass("selected-whole");

    /**
     * JavaFX's default cell size: a row's {@code -fx-cell-size} below it is the row's height whatever its cells, see
     * {@link #rowHeight(TableRow, List, double[])}.
     */
    private static final double DEFAULT_CELL_SIZE = 24;

    /**
     * The row of the header among the search's texts ({@link TextKey}, {@link SearchText}): before every body row.
     */
    private static final int HEADER_ROW = -1;

    /**
     * The measure row, created on first use; it is a child of the table only while it measures.
     */
    private TableRow<Row> measureRow;

    /**
     * Whether the measure row is a child of the table, see {@link #rowAt(TableSkin, int)}.
     */
    private boolean measuring;

    /**
     * The columns' widths: what they were last fitted to, and the reader's widths after a drag of a column's edge.
     */
    private final ColumnFit columnFit = new ColumnFit();

    /**
     * What copying the selected cells does with their text.
     */
    private final Consumer<String> copyAction;

    /**
     * The reader's selection gestures, which also select a whole column for the view's context menu.
     */
    private final CellSelection cellSelection;

    /**
     * The header's cells, in the source's column order.
     */
    private final List<Table.Cell> header;

    /**
     * The columns in the source's order, whatever order the reader has moved them to: a column's place here is the
     * index of its cells in the header and in each row.
     */
    private final List<TableColumn<Row, ?>> sourceColumns;

    /**
     * The rows in the source's order, whatever order the reader has sorted them to.
     */
    private final List<Row> sourceRows;

    /**
     * The texts of the table the view's search ran through last, in the order it numbered them, see
     * {@link #numberSearchTexts()}.
     */
    private List<SearchText> searchTexts = List.of();

    /**
     * The matches of the view's search the table shows, by their cells, each cell's in its order.
     */
    private final Map<SourceCell, List<CellMatch>> searchMatches = new HashMap<>();

    /**
     * The rows whose cells show the search's marks, by their place in the shown order: those that have come near the
     * visible area since the matches were last emptied. A place, not a row: a sort fills the cells of a marked place
     * anew, and they show the marks of the row they show then.
     */
    private final BitSet markedRows = new BitSet();

    /**
     * What runs when the search's texts change their order or come and go, {@code null} until the search watches the
     * table.
     */
    private Runnable searchTextsChanged;

    /**
     * Creates a table.
     *
     * @param table          the parsed table
     * @param runs           adds the styled runs of a cell's inlines to a text flow, as the renderer does for a
     *                       paragraph
     * @param copyAction     called with the text of the selected cells when the reader copies them
     * @param selectionStart called with this table whenever cells become selected, so that the view can clear the
     *                       selection of the block that held one before
     */
    MarkdownTable(Table table, BiConsumer<TextFlow, List<Inline>> runs, Consumer<String> copyAction,
                  Consumer<SelectionHolder> selectionStart) {
        this.copyAction = copyAction;
        getStyleClass().add("markdown-table");
        // A table of a header alone shows nothing below the header, not the "No content in table" placeholder.
        setPlaceholder(new Region());
        setColumnResizePolicy(new FittedColumns());
        setSortPolicy(tableView -> sortRows());
        // A sort moves rows under a selection: it is cleared before the sort policy runs, while the selection model
        // still reports each change (it holds back its events while the policy runs).
        addEventHandler(SortEvent.<TableView<Row>>sortEvent(), event -> getSelectionModel().clearSelection());
        setTableMenuButtonVisible(true);
        getSelectionModel().setCellSelectionEnabled(true);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cellSelection = CellSelection.install(this);
        // A column header sorts on the release of any button that is still since its press and no popup trigger.
        // Where the press is the popup trigger (Linux, macOS), a right click on a header would sort the rows, and
        // clear the selection, before the context menu opens; a middle click sorts everywhere. So only the primary
        // button reaches the headers: the press and the release of any other are consumed alike, which keeps the
        // header's drag lock (set on a press, lifted on a release) balanced.
        addEventFilter(MouseEvent.MOUSE_PRESSED, event -> keepFromHeaders(event));
        addEventFilter(MouseEvent.MOUSE_RELEASED, event -> keepFromHeaders(event));
        // The table's own key handling has no copy. Without selected cells the shortcut key plus C goes on to the
        // view, which copies the view's one selection: the text selected across the document, perhaps over this table.
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (COPY.match(event) && hasSelection()) {
                event.consume();
                copySelection();
            }
        });
        // Whatever selects cells (a gesture, the table's own keys) makes this table the view's one selection.
        getSelectionModel().getSelectedCells().addListener((InvalidationListener) observable -> {
            if (!getSelectionModel().getSelectedCells().isEmpty()) {
                selectionStart.accept(this);
            }
        });
        for (int i = 0; i < table.header().size(); i++) {
            getColumns().add(column(i, table.header().get(i), table.alignments().get(i), runs));
        }
        header = List.copyOf(table.header());
        sourceColumns = List.copyOf(getColumns());
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < table.rows().size(); i++) {
            rows.add(new Row(i, table.rows().get(i)));
        }
        sourceRows = List.copyOf(rows);
        getItems().setAll(rows);
        // A header's text flow wraps to what its column header leaves it, which shrinks when the sort arrow appears,
        // and in the last shown column by the menu button's corner.
        getSortOrder().addListener((ListChangeListener<TableColumn<Row, ?>>) change -> headersChanged());
        // The search's texts follow the shown order: a sort moves the rows, a moved, hidden or shown column the cells.
        getVisibleLeafColumns().addListener((ListChangeListener<TableColumn<Row, ?>>) change -> {
            headersChanged();
            searchTextsChanged();
        });
        getItems().addListener((ListChangeListener<Row>) change -> searchTextsChanged());
    }

    /**
     * Has the table run code whenever the search's texts change their order or come and go: the reader sorts the
     * table, moves a column, hides or shows one.
     *
     * @param changed the code; it replaces the code given before
     */
    void onSearchTextsChanged(Runnable changed) {
        searchTextsChanged = changed;
    }

    /**
     * Runs what the search asked to run when its texts change.
     */
    private void searchTextsChanged() {
        if (searchTextsChanged != null) {
            searchTextsChanged.run();
        }
    }

    /**
     * What names a text of the search across sorts and moved columns: its cell as the source has it, header or body,
     * and its stretch in the cell.
     *
     * @param text the text's number among the texts of the last {@link #numberSearchTexts()}
     * @return what names it, equal for the same text whatever the order
     */
    Object searchTextKey(int text) {
        return searchTexts.get(text).key();
    }

    /**
     * The number of the text of a cell and stretch among the texts of the last {@link #numberSearchTexts()}: a key
     * names the same cell in another rendering of the same table, so the search finds its current match's text again
     * in a new version of the document, whose table starts unsorted with every column.
     *
     * @param key what names a text, as {@link #searchTextKey(int)} gives it, of this table or another
     * @return the text's number, -1 if no text of the table has that key
     */
    int searchTextNumber(Object key) {
        for (int text = 0; text < searchTexts.size(); text++) {
            if (searchTexts.get(text).key().equals(key)) {
                return text;
            }
        }
        return -1;
    }

    /**
     * Consumes a press or release of a button other than the primary one on a column header, see the constructor.
     */
    private void keepFromHeaders(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY
                && Ancestors.nearest(event.getTarget(), TableColumnHeader.class, this) != null) {
            event.consume();
        }
    }

    /**
     * Sorts the rows by the sort order's columns, and rows those leave equal, or all rows when no column sorts, in
     * their source order: the third click on a header brings the source order back.
     */
    private boolean sortRows() {
        Comparator<Row> byColumns = getComparator();
        FXCollections.sort(getItems(), new SourceOrderAfter(byColumns));
        return true;
    }

    /**
     * Clears the selection of cells; the focused cell stays.
     */
    @Override
    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    /**
     * Whether a cell of a shown column is selected.
     */
    @Override
    public boolean hasSelection() {
        return selectionText(false) != null;
    }

    /**
     * Copies the selected cells, see {@link #selectionText(boolean)}; nothing without a selection.
     */
    @Override
    public void copySelection() {
        copy(false);
    }

    /**
     * Copies the selected cells with a first line of their columns' titles; nothing without a selection.
     */
    void copySelectionWithHeader() {
        copy(true);
    }

    private void copy(boolean withHeader) {
        String text = selectionText(withHeader);
        if (text != null) {
            copyAction.accept(text);
        }
    }

    /**
     * The selected cells as tab-separated text, which pastes into a spreadsheet cell by cell. One cell is its visible
     * text (the Markdown resolved, as a column sorts it). Several cells are their bounding rectangle: the
     * rows from the first to the last selected one in the shown order, the shown columns from the leftmost to the
     * rightmost selected one in their shown order; a cell of the rectangle that is not selected is empty, cells are
     * joined by a tab, rows by a line feed, with no line feed at the end and no quoting. With the header, a first line
     * holds the titles of the rectangle's columns, the header's visible text, joined the same way.
     *
     * <p>Only shown columns count: the selection model keeps the selected cells of a column hidden after they were
     * selected (and answers {@code isSelected} for a hidden column with whether any cell of the row is selected), so
     * the cells are taken from its list of selected cells and matched against the shown columns.
     *
     * @param withHeader whether a first line holds the columns' titles
     * @return the text, {@code null} when no cell of a shown column is selected
     */
    private String selectionText(boolean withHeader) {
        List<TableColumn<Row, ?>> shown = getVisibleLeafColumns();
        int minRow = Integer.MAX_VALUE;
        int maxRow = -1;
        int minColumn = Integer.MAX_VALUE;
        int maxColumn = -1;
        Set<CellKey> selected = new HashSet<>();
        for (TablePosition<?, ?> position : getSelectionModel().getSelectedCells()) {
            int row = position.getRow();
            int column = shown.indexOf(position.getTableColumn());
            if (column < 0 || row < 0 || row >= getItems().size()) {
                continue;
            }
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
            minColumn = Math.min(minColumn, column);
            maxColumn = Math.max(maxColumn, column);
            selected.add(new CellKey(row, column));
        }
        if (maxRow < 0) {
            return null;
        }
        return rectangleText(new CellKey(minRow, minColumn), new CellKey(maxRow, maxColumn), selected, withHeader);
    }

    /**
     * The whole table as text, as a selection across the document copies a table it passes over: the format of
     * <em>Copy with header</em> (see {@link #selectionText(boolean)}) for every cell, the header's titles and then
     * every row in the shown order, over the shown columns in their shown order.
     *
     * @return the text, empty while the table shows no column
     */
    String wholeText() {
        int columns = getVisibleLeafColumns().size();
        if (columns == 0) {
            return "";
        }
        return rectangleText(new CellKey(0, 0), new CellKey(getItems().size() - 1, columns - 1), null, true);
    }

    /**
     * The table's text as its source has it, whatever the reader made of it: in the format of {@link #wholeText()},
     * but the rows in the source's order and every column in the source's order, a hidden one too. So it is the same
     * for a table the reader sorted or rearranged and for the same table as a new text renders it, unsorted with every
     * column shown, whose {@link #wholeText()} it equals.
     *
     * @return the text
     */
    String sourceText() {
        StringBuilder text = new StringBuilder();
        appendSourceRow(text, header);
        for (Row row : sourceRows) {
            text.append('\n');
            appendSourceRow(text, row.cells());
        }
        return text.toString();
    }

    /**
     * Appends the visible texts of a row's cells in the source's column order, joined by a tab.
     */
    private static void appendSourceRow(StringBuilder text, List<Table.Cell> cells) {
        for (int column = 0; column < cells.size(); column++) {
            if (column > 0) {
                text.append('\t');
            }
            text.append(MarkdownRenderer.visibleText(cells.get(column).content()));
        }
    }

    /**
     * Numbers the texts the view's search runs through anew, in the order the table shows them now, keeps that
     * numbering and returns the texts: the header's cells of the shown columns in their shown order, then the rows in
     * their shown order, each row's cells in the shown columns' order. A hidden column is not searched. A cell's text
     * is what its flow shows, derived from its inlines without drawing it
     * ({@link MarkdownRenderer#cellSearchTexts(List)}), so a row never drawn is searched too; a cell with images gives
     * a text per stretch between their icons. The search's matches name a text by its number in the numbering kept,
     * which {@link #showSearchMatches(List, int)}, {@link #searchMatchBounds} and {@link #searchTextKey(int)} read, the
     * last naming it across a sort or a moved column.
     *
     * @return the texts, in the order the table shows them
     */
    List<String> numberSearchTexts() {
        List<SearchText> found = new ArrayList<>();
        List<TableColumn<Row, ?>> shown = getVisibleLeafColumns();
        for (TableColumn<Row, ?> column : shown) {
            int source = sourceColumns.indexOf(column);
            addSearchTexts(found, HEADER_ROW, HEADER_ROW, source, header.get(source));
        }
        for (int index = 0; index < getItems().size(); index++) {
            Row row = getItems().get(index);
            for (TableColumn<Row, ?> column : shown) {
                int source = sourceColumns.indexOf(column);
                addSearchTexts(found, row.sourceIndex(), index, source, row.cells().get(source));
            }
        }
        searchTexts = found;
        List<String> texts = new ArrayList<>(found.size());
        for (SearchText text : found) {
            texts.add(text.text());
        }
        return texts;
    }

    /**
     * Adds a cell's texts, one per stretch between its images' icons.
     */
    private static void addSearchTexts(List<SearchText> found, int sourceRow, int shownRow, int sourceColumn,
                                       Table.Cell cell) {
        List<String> stretches = MarkdownRenderer.cellSearchTexts(cell.content());
        SourceCell sourceCell = new SourceCell(sourceRow, sourceColumn);
        for (int stretch = 0; stretch < stretches.size(); stretch++) {
            found.add(new SearchText(new TextKey(sourceCell, stretch), shownRow, stretches.get(stretch)));
        }
    }

    /**
     * Shows the search's matches in the table's cells: the headers and the body cells of the rows marked so far show
     * those of their cells, the current one in its own colour, and a body cell of such a row filled later shows those
     * of its cell then ({@link BodyCell#updateItem}). A row is marked when it comes near the visible area
     * ({@link #showSearchMatchesNear(NearArea)}), so a one-letter search in a long table builds the marks of the rows
     * near the area only. The matches are kept by their cells as the source has them, not by where the cells show, so
     * a sort, which fills the cells anew, keeps the marks on their text. A cell whose matches stay as they were keeps
     * its marks: the next match in a table changes the marks of two cells.
     *
     * @param matches the table's matches, their texts numbered as the table's last {@link #numberSearchTexts()};
     *                none to show no marks, which unmarks every row
     * @param current the index of the current match among them, -1 if it lies elsewhere or there is none
     */
    void showSearchMatches(List<Searchable.Match> matches, int current) {
        searchMatches.clear();
        for (int i = 0; i < matches.size(); i++) {
            Searchable.Match match = matches.get(i);
            TextKey key = searchTexts.get(match.text()).key();
            List<CellMatch> inCell = searchMatches.get(key.cell());
            if (inCell == null) {
                inCell = new ArrayList<>();
                searchMatches.put(key.cell(), inCell);
            }
            inCell.add(new CellMatch(key.stretch(), match.start(), match.end(), i == current));
        }
        for (TableColumn<Row, ?> column : sourceColumns) {
            if (column.getGraphic() instanceof ColumnFlow flow) {
                flow.showSearchMatches(cellMatches(HEADER_ROW, column));
            }
        }
        if (getSkin() instanceof TableSkin skin) {
            for (int index = markedRows.nextSetBit(0); index >= 0; index = markedRows.nextSetBit(index + 1)) {
                showRowMatches(skin.visibleRow(index));
            }
        }
        if (matches.isEmpty()) {
            // The search changed or left the table: the rows are marked anew as they come near.
            markedRows.clear();
        }
    }

    /**
     * Marks the rows near the visible area that are not marked yet: their cells show the search's matches from now on,
     * until the matches are emptied. The rows lie one below the other in the shown order, so the first one near the
     * area is found by halving.
     *
     * @param area the part of the document near the visible area
     */
    void showSearchMatchesNear(NearArea area) {
        if (searchMatches.isEmpty() || !(getSkin() instanceof TableSkin skin)) {
            return;
        }
        // The rows lie one below the other in the shown order; one not laid out counts as reaching the area.
        int first = area.firstReaching(getItems().size(), row -> skin.visibleRow(row));
        for (int index = first; index < getItems().size(); index++) {
            TableRow<Row> row = skin.visibleRow(index);
            if (row == null) {
                continue;
            }
            if (area.boundsOf(row).getMinY() > area.bottom()) {
                break;
            }
            if (!markedRows.get(index)) {
                markedRows.set(index);
                showRowMatches(row);
            }
        }
    }

    /**
     * Has the body cells of a row show the search's matches of their cells.
     *
     * @param row the row, {@code null} for none
     */
    private static void showRowMatches(TableRow<Row> row) {
        if (row == null) {
            return;
        }
        for (Node child : row.getChildrenUnmodifiable()) {
            if (child instanceof BodyCell cell) {
                cell.showSearchMatches();
            }
        }
    }

    /**
     * Where a match lies in the scene as the table is laid out now: the box around its characters in its cell's flow.
     * A body cell is found in its row as the table lays it out; the table lays out every row, so a row is found though
     * the view has never shown it. Before that, the box is the table's.
     *
     * @param match one of the table's matches, its text numbered as the table's last {@link #numberSearchTexts()}
     * @return its bounds in the scene
     */
    Bounds searchMatchBounds(Searchable.Match match) {
        SearchText text = searchTexts.get(match.text());
        ColumnFlow flow = flowOf(text);
        if (flow == null) {
            return localToScene(getLayoutBounds());
        }
        return flow.localToScene(SearchMarks.boundsOf(flow, flow.rangeOf(text.key().stretch(), match.start(),
                match.end())));
    }

    /**
     * The flow that shows a text of the search: its column's header, or the body cell of its column in its row as the
     * table shows the rows now.
     *
     * @return the flow, {@code null} if the table has not laid out the text's row yet
     */
    private ColumnFlow flowOf(SearchText text) {
        TableColumn<Row, ?> column = sourceColumns.get(text.key().cell().column());
        if (text.shownRow() == HEADER_ROW) {
            return column.getGraphic() instanceof ColumnFlow header ? header : null;
        }
        if (!(getSkin() instanceof TableSkin skin)) {
            return null;
        }
        TableRow<Row> row = skin.visibleRow(text.shownRow());
        if (row == null) {
            return null;
        }
        for (Node child : row.getChildrenUnmodifiable()) {
            if (child instanceof BodyCell cell && cell.getTableColumn() == column
                    && cell.getGraphic() instanceof ColumnFlow flow) {
                return flow;
            }
        }
        return null;
    }

    /**
     * The search's matches in a cell, in its order.
     *
     * @param sourceRow the cell's row as the source has it, {@link #HEADER_ROW} for the header
     * @param column    the cell's column
     * @return the matches, none if the cell has none
     */
    private List<CellMatch> cellMatches(int sourceRow, TableColumn<?, ?> column) {
        List<CellMatch> inCell = searchMatches.get(new SourceCell(sourceRow, sourceColumns.indexOf(column)));
        return inCell == null ? List.of() : inCell;
    }

    /**
     * A rectangle of cells as text, see {@link #selectionText(boolean)}: rows from the first corner's to the second's
     * in the shown order, shown columns between the corners' in their shown order.
     *
     * @param selected the cells whose text is copied, {@code null} for all; any other cell of the rectangle is empty
     */
    private String rectangleText(CellKey first, CellKey last, Set<CellKey> selected, boolean withHeader) {
        List<TableColumn<Row, ?>> shown = getVisibleLeafColumns();
        StringBuilder text = new StringBuilder();
        if (withHeader) {
            for (int column = first.column(); column <= last.column(); column++) {
                if (column > first.column()) {
                    text.append('\t');
                }
                text.append(shown.get(column).getText());
            }
        }
        for (int row = first.row(); row <= last.row(); row++) {
            if (row > first.row() || withHeader) {
                text.append('\n');
            }
            for (int column = first.column(); column <= last.column(); column++) {
                if (column > first.column()) {
                    text.append('\t');
                }
                if ((selected == null || selected.contains(new CellKey(row, column)))
                        && shown.get(column).getCellData(row) instanceof Table.Cell cell) {
                    text.append(MarkdownRenderer.visibleText(cell.content()));
                }
            }
        }
        return text.toString();
    }

    /**
     * Shows the table as selected whole, or no longer, as a selection across the document does with a table it
     * passes over: the pseudo-class {@code selected-whole}, which the theme draws as if every cell and header were
     * selected. The table's own selection of cells stays as it is.
     *
     * @param selected whether the table looks selected whole
     */
    void showSelectedWhole(boolean selected) {
        pseudoClassStateChanged(SELECTED_WHOLE, selected);
    }

    /**
     * The column the view's <em>Select column</em> entry selects for a context menu request: the shown column of the
     * body cell or column header the request comes from; for a request at the table itself, from the keyboard, the
     * focused cell's column.
     *
     * @param target where the request comes from, the node under the pointer or the focus owner
     * @return the shown column, {@code null} if the request names none or the table has no rows
     */
    TableColumn<Row, ?> selectableColumn(EventTarget target) {
        if (getItems().isEmpty()) {
            return null;
        }
        // A cell lies in the flow, a column header in the header row: neither lies inside the other.
        TableCell<?, ?> cell = Ancestors.nearest(target, TableCell.class, this);
        if (cell != null) {
            return shownColumn(cell.getTableColumn());
        }
        TableColumnHeader header = Ancestors.nearest(target, TableColumnHeader.class, this);
        if (header != null) {
            return shownColumn(header.getTableColumn());
        }
        if (target == this) {
            return focusedColumn();
        }
        return null;
    }

    /**
     * The column of a cell or header as one of the table's shown columns, {@code null} if it is none.
     */
    private TableColumn<Row, ?> shownColumn(TableColumnBase<?, ?> column) {
        for (TableColumn<Row, ?> shown : getVisibleLeafColumns()) {
            if (shown == column) {
                return shown;
            }
        }
        return null;
    }

    /**
     * The focused cell's column, {@code null} if no cell of a shown column has the focus.
     */
    private TableColumn<Row, ?> focusedColumn() {
        TablePosition<Row, ?> focused = getFocusModel().getFocusedCell();
        return focused == null || focused.getRow() < 0 ? null : shownColumn(focused.getTableColumn());
    }

    /**
     * Selects every cell of a shown column, top to bottom, as the reader's <em>Select column</em> does; the
     * selection before is cleared.
     *
     * @param column the column
     */
    void selectColumn(TableColumn<Row, ?> column) {
        cellSelection.selectColumn(column);
    }

    /**
     * Lets every header's text flow measure again what lies around it.
     */
    private void headersChanged() {
        for (TableColumn<Row, ?> column : getColumns()) {
            if (column.getGraphic() instanceof ColumnFlow header) {
                header.requestLayout();
            }
        }
    }

    /**
     * A column of the table. Its text is the header's visible text, for the column's accessible name; the theme shows
     * the header's text flow, its graphic, in its place.
     */
    private static TableColumn<Row, Table.Cell> column(int index, Table.Cell header, Table.Alignment alignment,
                                                      BiConsumer<TextFlow, List<Inline>> runs) {
        TableColumn<Row, Table.Cell> column = new TableColumn<>(MarkdownRenderer.visibleText(header.content()));
        TextAlignment textAlignment = textAlignment(alignment);
        HeaderFlow flow = new HeaderFlow(column, textAlignment);
        runs.accept(flow, header.content());
        column.setGraphic(flow);
        column.setCellValueFactory(features -> new ReadOnlyObjectWrapper<>(features.getValue().cells().get(index)));
        column.setCellFactory(tableColumn -> new BodyCell(textAlignment, runs));
        column.setComparator(new CellOrder());
        return column;
    }

    /**
     * How a column's text is aligned: left unless the delimiter row says centre or right.
     */
    private static TextAlignment textAlignment(Table.Alignment alignment) {
        return switch (alignment) {
            case NONE, LEFT -> TextAlignment.LEFT;
            case CENTER -> TextAlignment.CENTER;
            case RIGHT -> TextAlignment.RIGHT;
        };
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new TableSkin(this);
    }

    /**
     * Horizontal: the table's height depends on its width, through the column widths and the wrapped text.
     */
    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    /**
     * The header and every row at the column widths the given width gives, see the class comment; without a width
     * (-1), at the columns' widths as they are. It sets no column width.
     */
    @Override
    protected double computePrefHeight(double width) {
        if (!(getSkin() instanceof TableSkin skin) || getScene() == null) {
            return snappedTopInset() + snappedBottomInset();
        }
        List<TableColumn<Row, ?>> columns = getVisibleLeafColumns();
        double[] widths;
        if (width < 0) {
            widths = new double[columns.size()];
            for (int i = 0; i < widths.length; i++) {
                widths[i] = columns.get(i).getWidth();
            }
        } else {
            widths = columnFit.widths(available(width), columns, measure(skin), pixel());
        }
        // The skin gives the flow the height below the header rounded down, so the rows' height is rounded up.
        return snappedTopInset() + skin.headerHeight(columns, widths) + Math.ceil(rowsHeight(skin, columns, widths))
                + snappedBottomInset();
    }

    /**
     * The columns' minimums together: the table is never narrower, so a word never breaks and the columns fill it.
     */
    @Override
    protected double computeMinWidth(double height) {
        double minimum = 0;
        if (getSkin() instanceof TableSkin skin && getScene() != null) {
            minimum = ColumnWidths.minimumTotal(measure(skin).minimum(), pixel());
        }
        return snappedLeftInset() + minimum + snappedRightInset();
    }

    @Override
    protected void layoutChildren() {
        if (getSkin() instanceof TableSkin skin) {
            fitColumns(getWidth(), skin);
        }
        super.layoutChildren();
    }

    /**
     * The header's first line: from the table's top inset, not from where the header row lies, which the first layout
     * pass sets only after a list item has placed its marker on this baseline.
     */
    @Override
    public double getBaselineOffset() {
        if (getSkin() instanceof TableSkin skin) {
            return snappedTopInset() + skin.headerBaseline();
        }
        return super.getBaselineOffset();
    }

    /**
     * Fits the shown columns to a width by the {@link ColumnWidths} rule, see {@link ColumnFit}.
     */
    private void fitColumns(double width, TableSkin skin) {
        columnFit.fit(available(width), getVisibleLeafColumns(), measure(skin), pixel());
    }

    /**
     * The width the columns share at a width of the table: without the table's insets.
     */
    private double available(double width) {
        return width - snappedLeftInset() - snappedRightInset();
    }

    /**
     * Resizes a column whose right edge the reader drags, see {@link ColumnFit}.
     *
     * @return whether a width changed
     */
    private boolean dragColumn(TableColumn<Row, ?> column, double delta) {
        List<TableColumn<Row, ?>> columns = getVisibleLeafColumns();
        int index = columns.indexOf(column);
        if (index < 0 || !(getSkin() instanceof TableSkin skin)) {
            return false;
        }
        return columnFit.drag(getColumns(), columns, index, delta, measure(skin).minimum(), pixel());
    }

    /**
     * Measures the shown columns: for each, the widest of its cells on one line and its longest word, header
     * included. The header row is asked for its height first: that creates the column headers, styled, so a header's
     * text flow knows what lies around it.
     */
    private ColumnFit.Measurement measure(TableSkin skin) {
        List<TableColumn<Row, ?>> columns = getVisibleLeafColumns();
        double[] content = new double[columns.size()];
        double[] minimum = new double[columns.size()];
        skin.headerHeight();
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getGraphic() instanceof ColumnFlow header) {
                content[i] = header.contentWidth();
                minimum[i] = header.minimumWidth();
            }
        }
        for (int index = 0; index < getItems().size(); index++) {
            for (Node child : rowAt(skin, index).getChildrenUnmodifiable()) {
                if (child instanceof TableCell<?, ?> cell && cell.getGraphic() instanceof ColumnFlow flow) {
                    int column = columns.indexOf(cell.getTableColumn());
                    if (column >= 0) {
                        content[column] = Math.max(content[column], flow.contentWidth());
                        minimum[column] = Math.max(minimum[column], flow.minimumWidth());
                    }
                }
            }
        }
        stopMeasuring();
        return new ColumnFit.Measurement(content, minimum);
    }

    /**
     * The height of all rows at the given widths of the shown columns, see {@link #rowHeight(TableRow, List, double[])}.
     */
    private double rowsHeight(TableSkin skin, List<TableColumn<Row, ?>> columns, double[] widths) {
        double height = 0;
        for (int index = 0; index < getItems().size(); index++) {
            height += rowHeight(rowAt(skin, index), columns, widths);
        }
        stopMeasuring();
        return height;
    }

    /**
     * A row's height at the given widths of the shown columns, by the rule of JavaFX's row skin: as tall as its
     * tallest cell, at the given width of the cell's column, and at least the theme's {@code -fx-cell-size}, both with
     * the row's insets. A cell size below JavaFX's default is every row's height whatever its cells.
     */
    private static double rowHeight(TableRow<Row> row, List<TableColumn<Row, ?>> columns, double[] widths) {
        double insets = row.snappedTopInset() + row.snappedBottomInset();
        double cellSize = row.getSkin() instanceof CellSkinBase<?> skin ? skin.getCellSize() : DEFAULT_CELL_SIZE;
        if (cellSize < DEFAULT_CELL_SIZE) {
            return insets + cellSize;
        }
        double tallest = 0;
        for (Node child : row.getChildrenUnmodifiable()) {
            if (child instanceof BodyCell cell) {
                int column = columns.indexOf(cell.getTableColumn());
                if (column >= 0) {
                    tallest = Math.max(tallest, cell.heightAt(widths[column]));
                }
            }
        }
        return insets + Math.max(tallest, cellSize);
    }

    /**
     * The row for an index with its cells up to date: the row the flow shows, or for a row it does not show yet the
     * measure row showing the row's cells, styled. The first use of the measure row adds it to the table, so the theme
     * styles it as one of the table's rows; it stays there until {@link #stopMeasuring()}.
     *
     * <p>The measure row is unmanaged, which makes it a layout root of its own: what changes in it asks no parent for
     * a layout, and the table does not lay it out. Added and removed, an unmanaged node asks the table for a layout
     * only while it waits for one itself, so it is laid out before it is added. A measurement during a layout pass,
     * when a parent asks the table's height, so asks none of the table's parents for another pass.
     */
    private TableRow<Row> rowAt(TableSkin skin, int index) {
        TableRow<Row> shown = skin.shownRow(index);
        if (shown != null) {
            return shown;
        }
        if (!measuring) {
            if (measureRow == null) {
                measureRow = new TableRow<>();
                measureRow.setManaged(false);
                measureRow.updateTableView(this);
            }
            measureRow.layout();
            getChildren().add(measureRow);
            measureRow.applyCss();
            measuring = true;
        }
        measureRow.updateIndex(index);
        // As for a shown row, see TableSkin.shownRow: the size creates the cell of a column shown again.
        measureRow.prefHeight(-1);
        measureRow.applyCss();
        return measureRow;
    }

    /**
     * Empties the measure row and takes it out of the table again, if a row was measured in it.
     */
    private void stopMeasuring() {
        if (measuring) {
            measureRow.updateIndex(-1);
            getChildren().remove(measureRow);
            measuring = false;
        }
    }

    /**
     * The body cell at a scene point, or the cell nearest to it when the point lies beyond the rows or the shown
     * columns: the row the point is beside, else the first or the last; the shown column it is beside, else the first
     * or the last. {@code null} while the table has no rows or shows no column.
     *
     * @param sceneX the point's x in the scene
     * @param sceneY the point's y in the scene
     * @return the cell's position, its row in the shown order and its column among the shown ones
     */
    TablePosition<Row, ?> cellAt(double sceneX, double sceneY) {
        List<TableColumn<Row, ?>> columns = getVisibleLeafColumns();
        if (getItems().isEmpty() || columns.isEmpty() || !(getSkin() instanceof TableSkin skin)) {
            return null;
        }
        int row = getItems().size() - 1;
        for (int index = 0; index < getItems().size(); index++) {
            TableRow<Row> shown = skin.visibleRow(index);
            if (shown != null && sceneY < shown.localToScene(shown.getLayoutBounds()).getMaxY()) {
                row = index;
                break;
            }
        }
        // The flow never scrolls sideways (the block moves the whole table), so the columns lie side by side from
        // the table's left inset.
        double x = sceneToLocal(sceneX, sceneY).getX() - snappedLeftInset();
        int column = columns.size() - 1;
        double right = 0;
        for (int index = 0; index < columns.size(); index++) {
            right += columns.get(index).getWidth();
            if (x < right) {
                column = index;
                break;
            }
        }
        return new TablePosition<>(this, row, columns.get(column));
    }

    /**
     * The width the header's menu button takes from a column's header: the button lies over the right end of the
     * last shown column's header, as wide as the flow's vertical bar would be, and that header lays its label out
     * left of it (the table never shows the bar).
     */
    private double cornerPadding(TableColumn<?, ?> column) {
        List<TableColumn<Row, ?>> shown = getVisibleLeafColumns();
        if (!isTableMenuButtonVisible() || shown.isEmpty() || shown.getLast() != column
                || !(getSkin() instanceof TableSkin skin)) {
            return 0;
        }
        return skin.cornerWidth();
    }

    /**
     * The width of a device pixel of the table's window, 1 without a window.
     */
    private double pixel() {
        Window window = getScene() == null ? null : getScene().getWindow();
        return window == null || window.getRenderScaleX() <= 0 ? 1 : 1 / window.getRenderScaleX();
    }

    /**
     * A body row of the table.
     *
     * @param sourceIndex the row's position in the source, 0 for the first body row
     * @param cells       the row's cells, one per column
     */
    record Row(int sourceIndex, List<Table.Cell> cells) {
    }

    /**
     * How a column sorts its cells: by their visible text, the Markdown resolved, in {@link NaturalOrder}.
     */
    private static final class CellOrder implements Comparator<Table.Cell> {

        @Override
        public int compare(Table.Cell first, Table.Cell second) {
            return NaturalOrder.INSTANCE.compare(MarkdownRenderer.visibleText(first.content()),
                    MarkdownRenderer.visibleText(second.content()));
        }
    }

    /**
     * The rows' order: by the sort order's columns if any sort, then by the rows' places in the source.
     *
     * @param byColumns the table's comparator of the sort order's columns, {@code null} while none sorts
     */
    private record SourceOrderAfter(Comparator<Row> byColumns) implements Comparator<Row> {

        @Override
        public int compare(Row first, Row second) {
            int order = byColumns == null ? 0 : byColumns.compare(first, second);
            return order != 0 ? order : Integer.compare(first.sourceIndex(), second.sourceIndex());
        }
    }

    /**
     * A cell as the source has it, whatever the reader's sort and column order.
     *
     * @param row    the row's place in the source ({@link Row#sourceIndex()}), {@link #HEADER_ROW} for the header
     * @param column the column's place in the source
     */
    private record SourceCell(int row, int column) {
    }

    /**
     * What names a text of the search across sorts and moved columns: its cell as the source has it and its stretch
     * in the cell.
     *
     * @param cell    the cell
     * @param stretch the text's place among the cell's stretches between its images' icons, from 0
     */
    private record TextKey(SourceCell cell, int stretch) {
    }

    /**
     * A match of the search in a cell.
     *
     * @param stretch the stretch of the cell's text between its images' icons it lies in, from 0
     * @param start   its first character in the stretch
     * @param end     the position after its last character in the stretch
     * @param current whether it is the current match
     */
    private record CellMatch(int stretch, int start, int end, boolean current) {
    }

    /**
     * A text of the search, see {@link #numberSearchTexts()}.
     *
     * @param key      what names it across sorts and moved columns
     * @param shownRow its row's place in the shown order when the search ran, {@link #HEADER_ROW} for the header
     * @param text     the text
     */
    private record SearchText(TextKey key, int shownRow, String text) {
    }

    /**
     * A cell of the selection's bounding rectangle, see {@link #selectionText(boolean)}.
     *
     * @param row    the cell's row, in the shown order
     * @param column the cell's place among the shown columns
     */
    private record CellKey(int row, int column) {
    }

    /**
     * The table's column resize policy. The table fits its columns itself, before every layout; the policy keeps the
     * flow from showing a horizontal bar and the last column's right edge from being dragged, as every constrained
     * policy does. A drag of a column's edge (a call with a column) goes to the table, which keeps the widths adding
     * up to its width; the calls without a column, which ask to fit the columns, change nothing.
     */
    private static final class FittedColumns extends ConstrainedColumnResizeBase
            implements Callback<TableView.ResizeFeatures, Boolean> {

        @Override
        @SuppressWarnings("unchecked")
        public Boolean call(TableView.ResizeFeatures features) {
            if (features.getColumn() != null && features.getTable() instanceof MarkdownTable table) {
                return table.dragColumn((TableColumn<Row, ?>) features.getColumn(), features.getDelta());
            }
            return false;
        }
    }

    /**
     * The table's skin: the standard one, with access to what the table measures through the skin's API for
     * subclasses.
     */
    private static final class TableSkin extends TableViewSkin<Row> {

        TableSkin(MarkdownTable table) {
            super(table);
        }

        /**
         * The header row's height at the columns' current widths.
         */
        double headerHeight() {
            return getTableHeaderRow().prefHeight(-1);
        }

        /**
         * The header row's height at the given widths of the shown columns, without the columns having them: what
         * lies around the column headers as now, the header row's height less its tallest column header's, and the
         * tallest column header at the given widths. A column header is as tall as its label, the label's padding
         * around the header's text flow, but no lower than its least height.
         *
         * @param columns the shown columns, in their shown order
         * @param widths  their widths, in the same order
         */
        double headerHeight(List<TableColumn<Row, ?>> columns, double[] widths) {
            double height = headerHeight();
            double tallestNow = 0;
            double tallest = 0;
            for (TableColumnHeader header : getTableHeaderRow().getRootHeader().getColumnHeaders()) {
                double now = header.prefHeight(-1);
                tallestNow = Math.max(tallestNow, now);
                int column = columns.indexOf(header.getTableColumn());
                if (column >= 0 && header.getTableColumn().getGraphic() instanceof ColumnFlow flow
                        && flow.getParent() instanceof Region label) {
                    double labelHeight = label.snappedTopInset() + flow.heightAt(widths[column])
                            + label.snappedBottomInset();
                    tallest = Math.max(tallest, Math.max(leastHeight(header), labelHeight));
                } else {
                    tallest = Math.max(tallest, now);
                }
            }
            return tallestNow == 0 ? height : height - tallestNow + tallest;
        }

        /**
         * A column header's least height, its {@code -fx-size}: JavaFX makes a column header as tall as its label but
         * no lower, and keeps the value to itself; its CSS property tells it.
         */
        @SuppressWarnings("unchecked")
        private static double leastHeight(TableColumnHeader header) {
            for (CssMetaData<? extends Styleable, ?> property : header.getCssMetaData()) {
                if (property.getProperty().equals("-fx-size")
                        && ((CssMetaData<TableColumnHeader, ?>) property).getStyleableProperty(header).getValue()
                        instanceof Number size) {
                    return size.doubleValue();
                }
            }
            return 0;
        }

        /**
         * The baseline of the header row's first line, from the header row's top.
         */
        double headerBaseline() {
            return getTableHeaderRow().getBaselineOffset();
        }

        /**
         * The width of the header's corner, where the menu button lies: the flow's vertical bar's preferred width,
         * as the header row lays the corner out.
         */
        double cornerWidth() {
            for (Node child : getVirtualFlow().getChildrenUnmodifiable()) {
                if (child instanceof ScrollBar bar && bar.getOrientation() == Orientation.VERTICAL) {
                    return getSkinnable().snapSizeX(bar.prefWidth(-1));
                }
            }
            return 0;
        }

        /**
         * The row the flow shows for an index, its cells up to date, {@code null} if it shows none. A row creates the
         * cell of a column shown again only when asked for its size or laid out, and the new cell takes the theme's
         * padding only from a CSS pass: measured before, the column would lack its body cells.
         */
        TableRow<Row> shownRow(int index) {
            TableRow<Row> row = getVirtualFlow().getVisibleCell(index);
            if (row != null) {
                row.prefHeight(-1);
                row.applyCss();
            }
            return row;
        }

        /**
         * The row the flow shows for an index, as it lies now, {@code null} if it shows none.
         */
        TableRow<Row> visibleRow(int index) {
            return getVirtualFlow().getVisibleCell(index);
        }
    }

    /**
     * A body cell: its graphic is a text flow of the cell's inlines; it has no text of its own.
     */
    private static final class BodyCell extends TableCell<Row, Table.Cell> {

        private final TextAlignment alignment;
        private final BiConsumer<TextFlow, List<Inline>> runs;

        BodyCell(TextAlignment alignment, BiConsumer<TextFlow, List<Inline>> runs) {
            this.alignment = alignment;
            this.runs = runs;
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Table.Cell cell, boolean empty) {
            super.updateItem(cell, empty);
            setText(null);
            if (empty || cell == null || getTableColumn() == null) {
                setGraphic(null);
            } else {
                CellFlow flow = new CellFlow(this, alignment);
                runs.accept(flow, cell.content());
                setGraphic(flow);
                showSearchMatches();
            }
        }

        /**
         * Shows the search's matches of the cell in its flow, if its row is marked
         * ({@link MarkdownTable#showSearchMatchesNear(NearArea)}): those of the cell its index and column name now.
         * After a sort the cell may still show the text of its row before until it is filled anew in the next layout,
         * which marks that flow again; a cell whose text the sort leaves the same is not filled anew, and its marks
         * are right from here.
         */
        void showSearchMatches() {
            int index = getIndex();
            if (getGraphic() instanceof ColumnFlow flow && getTableView() instanceof MarkdownTable table
                    && index >= 0 && index < table.getItems().size()) {
                flow.showSearchMatches(table.markedRows.get(index)
                        ? table.cellMatches(table.getItems().get(index).sourceIndex(), getTableColumn()) : List.of());
            }
        }

        /**
         * The text flow and the cell's padding and border; the label skin would add the height of the cell's empty
         * text in the cell's font.
         */
        @Override
        protected double computePrefHeight(double width) {
            if (getGraphic() instanceof ColumnFlow flow) {
                return snappedTopInset() + flow.prefHeight(-1) + snappedBottomInset();
            }
            return super.computePrefHeight(width);
        }

        /**
         * The cell's preferred height at a width of its column, without the column having it: the text flow's height
         * at that width and the cell's padding and border. A cell without a text flow is as tall at every width.
         */
        double heightAt(double columnWidth) {
            if (getGraphic() instanceof ColumnFlow flow) {
                return snappedTopInset() + flow.heightAt(columnWidth) + snappedBottomInset();
            }
            return prefHeight(-1);
        }

        /**
         * As tall as preferred: a row is as tall as its tallest cell's minimum too.
         */
        @Override
        protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }
    }

    /**
     * The text flow of a cell, header or body, which wraps to the width its column offers it, see the class comment.
     * Its measurements for the width rule are kept until the fonts of its runs change, as when the theme styles it, and
     * so are its heights at the last few widths ({@link #computePrefHeight(double)}).
     */
    private abstract static class ColumnFlow extends TextFlow {

        /**
         * Measures the words of a run, on the JavaFX Application Thread only.
         */
        private static final Text MEASURE = new Text();

        final TableColumnBase<?, ?> column;
        private final InvalidationListener columnWidthChanged = observable -> requestLayout();

        private List<Font> measuredFonts;
        private double lineWidth;
        private double longestWord;

        /**
         * The heights computed so far by the width offered, the most recently asked last, see
         * {@link #computePrefHeight(double)}, and what they were computed with.
         */
        private final RecentHeights heights = new RecentHeights();
        private HeightBasis heightBasis;

        /**
         * The search's marks, whose layers are made on the first match.
         */
        private final SearchMarks searchMarks = new SearchMarks(this);

        /**
         * The matches the marks show.
         */
        private List<CellMatch> shownMatches = List.of();

        ColumnFlow(TableColumnBase<?, ?> column, TextAlignment alignment) {
            this.column = column;
            setTextAlignment(alignment);
            // The cached sizes of the flow, and of the label or cell around it, depend on the column's width.
            column.widthProperty().addListener(new WeakInvalidationListener(columnWidthChanged));
        }

        /**
         * Shows the search's matches of the flow's cell in the two layers of its marks ({@link SearchMarks}), each
         * match's range of its stretch moved to the flow's positions; none hide the marks. The same matches as shown
         * change nothing. A cell's flow has no selection layer of its own (the theme colours a selected cell), so the
         * layers are its last children.
         */
        final void showSearchMatches(List<CellMatch> matches) {
            if (matches.equals(shownMatches)) {
                return;
            }
            shownMatches = matches;
            List<Integer> starts = stretchStarts();
            List<Segment.Range> ranges = new ArrayList<>(matches.size());
            int current = -1;
            for (CellMatch match : matches) {
                if (match.stretch() >= starts.size()) {
                    // A cell that shows another cell's text until the next layout fills it, see BodyCell.
                    continue;
                }
                if (match.current()) {
                    current = ranges.size();
                }
                int start = starts.get(match.stretch());
                ranges.add(new Segment.Range(start + match.start(), start + match.end()));
            }
            searchMarks.show(ranges, current, null);
        }

        /**
         * A range of a stretch of the flow's text between its images' icons, among the flow's positions.
         *
         * @param stretch the stretch, from 0
         * @param start   the range's start in the stretch
         * @param end     the range's end in the stretch
         * @return the range in the flow; at the flow's start for a stretch the flow does not have
         */
        final Segment.Range rangeOf(int stretch, int start, int end) {
            List<Integer> starts = stretchStarts();
            int offset = stretch < starts.size() ? starts.get(stretch) : 0;
            return new Segment.Range(offset + start, offset + end);
        }

        /**
         * Where the stretches of the flow's text between its images' icons start among its positions: the renderer
         * shows an image in a cell as an icon, a {@link FontIcon} run, and the alt text after it, and the search's
         * texts of a cell are those stretches ({@link MarkdownRenderer#cellSearchTexts(List)}).
         */
        private List<Integer> stretchStarts() {
            List<Integer> starts = new ArrayList<>();
            starts.add(0);
            int position = 0;
            for (Node child : getChildren()) {
                if (child instanceof Text run) {
                    position += run.getText().length();
                    if (run instanceof FontIcon) {
                        starts.add(position);
                    }
                }
            }
            return starts;
        }

        /**
         * Lays out the runs, then the search's layers over them: a new column width wraps the text anew, and the marks'
         * shapes with it.
         */
        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            searchMarks.layOverFlow();
        }

        /**
         * The width between the column's edges and the flow: padding, borders, a shown sort arrow.
         */
        abstract double chrome();

        /**
         * The width the column offers the flow.
         */
        final double offeredWidth() {
            return offeredWidth(column.getWidth());
        }

        /**
         * The width a width of the column would offer the flow.
         */
        private double offeredWidth(double columnWidth) {
            return Math.max(0, snapSizeX(columnWidth) - chrome());
        }

        /**
         * The flow's height at a width of its column, the column's own or one the table measures at: its preferred
         * height at the width the column would offer it, see {@link #computePrefHeight(double)}. Asking changes
         * nothing in the scene graph.
         */
        final double heightAt(double columnWidth) {
            return prefHeight(offeredWidth(columnWidth));
        }

        /**
         * The width the column needs to show the flow's text on one line.
         */
        final double contentWidth() {
            measure();
            return chrome() + lineWidth;
        }

        /**
         * The width the column needs to show the flow's longest word whole.
         */
        final double minimumWidth() {
            measure();
            return chrome() + longestWord;
        }

        /**
         * The text on one line. A label that shows its graphic alone lays it out at its own width within the graphic's
         * minimum and maximum, so this width sets no layout; it is what JavaFX's fit of a column to its content (a
         * double click on the column's resize handle) measures.
         */
        @Override
        protected double computePrefWidth(double height) {
            measure();
            return lineWidth;
        }

        /**
         * No wider than offered: a label showing its graphic alone stretches it to its own width, which in the last
         * column's header reaches under the menu button whenever JavaFX leaves out the corner's padding.
         */
        @Override
        protected double computeMaxWidth(double height) {
            return offeredWidth();
        }

        /**
         * The height at the width the column offers, or at a width given. JavaFX's text layout does not always answer
         * one width alike: asked about another width before, it may lay a wrapped line out a pixel or two taller or
         * lower. The table measures its rows at widths its columns do not have yet, and the rows' layout asks the
         * height at the columns' widths; so that the two agree, the flow computes its height at a width once and keeps
         * it, for the last few widths, as long as the fonts of its runs, its line spacing and its padding stay as they
         * were.
         */
        @Override
        protected double computePrefHeight(double width) {
            double offered = width < 0 ? offeredWidth() : width;
            HeightBasis basis = new HeightBasis(fonts(), getLineSpacing(), getInsets());
            if (!basis.equals(heightBasis)) {
                heightBasis = basis;
                heights.clear();
            }
            Double height = heights.get(offered);
            if (height == null) {
                height = snapSizeY(super.computePrefHeight(offered));
                heights.put(offered, height);
            }
            return height;
        }

        @Override
        protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        /**
         * The fonts of the runs, in their order.
         */
        private List<Font> fonts() {
            List<Font> fonts = new ArrayList<>();
            for (Node child : getChildren()) {
                if (child instanceof Text run) {
                    fonts.add(run.getFont());
                }
            }
            return fonts;
        }

        private void measure() {
            List<Font> fonts = fonts();
            if (fonts.equals(measuredFonts)) {
                return;
            }
            measuredFonts = fonts;
            lineWidth = super.computePrefWidth(-1);
            longestWord = snappedLeftInset() + longestWordWidth() + snappedRightInset();
        }

        /**
         * The width of the widest run of characters between whitespace; a word may span runs of different styles.
         */
        private double longestWordWidth() {
            double longest = 0;
            double word = 0;
            for (Node child : getChildren()) {
                if (!(child instanceof Text run)) {
                    continue;
                }
                String text = run.getText();
                int start = 0;
                for (int i = 0; i <= text.length(); i++) {
                    boolean space = i < text.length() && Character.isWhitespace(text.charAt(i));
                    if (space || i == text.length()) {
                        if (i > start) {
                            word += width(text.substring(start, i), run.getFont());
                        }
                        if (space) {
                            longest = Math.max(longest, word);
                            word = 0;
                        }
                        start = i + 1;
                    }
                }
            }
            return Math.max(longest, word);
        }

        private static double width(String text, Font font) {
            MEASURE.setFont(font);
            MEASURE.setText(text);
            return MEASURE.getLayoutBounds().getWidth();
        }

        /**
         * What a flow's height depends on besides the width: its runs' fonts, its line spacing and its padding.
         */
        private record HeightBasis(List<Font> fonts, double lineSpacing, Insets insets) {
        }

        /**
         * A flow's heights by the width offered, for the last few widths asked: in a layout pass the table asks a
         * width or two besides the columns' own, and a window resized step by step asks a new one each step.
         */
        private static final class RecentHeights extends LinkedHashMap<Double, Double> {

            private static final int KEPT = 8;

            RecentHeights() {
                super(16, 0.75f, true);
            }

            @Override
            protected boolean removeEldestEntry(Map.Entry<Double, Double> eldest) {
                return size() > KEPT;
            }
        }
    }

    /**
     * A body cell's text flow: the cell's padding and border lie around it.
     */
    private static final class CellFlow extends ColumnFlow {

        private final TableCell<?, ?> cell;

        CellFlow(TableCell<?, ?> cell, TextAlignment alignment) {
            super(cell.getTableColumn(), alignment);
            this.cell = cell;
        }

        @Override
        double chrome() {
            return cell.snappedLeftInset() + cell.snappedRightInset();
        }
    }

    /**
     * A header's text flow, the column's graphic: the column header lays out its label at its width less its padding
     * and the shown sort arrow, the label the flow within its own padding. Before the column header exists, nothing
     * lies around the flow.
     */
    private static final class HeaderFlow extends ColumnFlow {

        HeaderFlow(TableColumnBase<?, ?> column, TextAlignment alignment) {
            super(column, alignment);
        }

        @Override
        double chrome() {
            double chrome = 0;
            if (getParent() instanceof Region label) {
                chrome += label.snappedLeftInset() + label.snappedRightInset();
                if (label.getParent() instanceof Region header) {
                    chrome += header.snappedLeftInset() + header.snappedRightInset();
                    for (Node child : header.getChildrenUnmodifiable()) {
                        if (child != label && child.isVisible()) {
                            chrome += child.prefWidth(-1);
                        }
                    }
                }
            }
            if (column instanceof TableColumn<?, ?> tableColumn
                    && tableColumn.getTableView() instanceof MarkdownTable table) {
                chrome += table.cornerPadding(tableColumn);
            }
            return chrome;
        }
    }
}
