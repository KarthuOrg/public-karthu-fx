package org.karthu.fx.markdown;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.runAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;

/**
 * The Markdown view's search in tables seen from outside: the search set and read through the view's API, the tables
 * sorted, their columns moved, hidden and shown through the table's API, the cells found by the table's documented
 * parts, the marks by their documented style classes and the scrolling by the scroll pane's offset.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewTableSearchTest {

    /**
     * A paragraph, a table with {@code anna} in a header and in three body cells, and a paragraph: six matches.
     */
    private static final String TABLE = """
            Before anna.

            | Anna | Name |
            |---|---|
            | x anna | anna |
            | y | z anna |

            After anna.
            """;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a search counts the matches in a table's header and body cells, also before the table is drawn")
    void countsTableMatches() {
        MarkdownView view = new MarkdownView(TABLE);
        view.setSearchText("anna");

        assertEquals(6, view.getSearchMatchCount(), "before the first layout");

        layOut(view);
        assertEquals(6, view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("the matches are numbered header first, then row by row, each row's cells in the columns' order, and"
            + " marked in their cells' flows at their characters only, the current one as current")
    void orderAndMarks() {
        MarkdownView view = layOut(new MarkdownView(TABLE));
        TableView<?> table = table(view);

        view.setSearchText("anna");
        relayOut(view);

        assertEquals(shape(headerFlow(table, 0), 0, 4), marks(headerFlow(table, 0), "markdown-search-match"));
        assertEquals(shape(cellFlow(table, 0, 0), 2, 6), marks(cellFlow(table, 0, 0), "markdown-search-match"));
        assertEquals(shape(cellFlow(table, 0, 1), 0, 4), marks(cellFlow(table, 0, 1), "markdown-search-match"));
        assertEquals(shape(cellFlow(table, 1, 1), 2, 6), marks(cellFlow(table, 1, 1), "markdown-search-match"));
        assertFalse(isMarked(headerFlow(table, 1)), "Name");
        assertFalse(isMarked(cellFlow(table, 1, 0)), "y");

        List<TextFlow> currentIn = new ArrayList<>();
        for (int step = 1; step <= 4; step++) {
            view.nextSearchMatch();
            relayOut(view);
            assertEquals(step, view.getSearchMatchIndex());
            currentIn.add(flowWithCurrent(table));
        }
        assertEquals(List.of(headerFlow(table, 0), cellFlow(table, 0, 0), cellFlow(table, 0, 1),
                cellFlow(table, 1, 1)), currentIn, "header, then row by row, each row left to right");
        assertEquals(shape(cellFlow(table, 1, 1), 2, 6), marks(cellFlow(table, 1, 1), "markdown-search-current"));
        assertEquals(List.of(), marks(cellFlow(table, 1, 1), "markdown-search-match"), "the current one only");

        view.nextSearchMatch();
        relayOut(view);
        assertEquals(5, view.getSearchMatchIndex(), "after the table");
        assertEquals(null, flowWithCurrent(table));
    }

    @Test
    @DisplayName("a cell's searched text is the text its flow shows, for every kind of inline: emphasis, code, links,"
            + " images; the icon of an image never matches, its alt text does")
    void searchedTextIsTheShownText() {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        view.setMarkdown("| Head ![logo alt](h.png) end |\n|---|\n"
                + "| a *em* `code` [link](https://x.example) **strong** ![alt *text*](img.png) z |");
        layOut(view);
        TableView<?> table = table(view);

        for (TextFlow flow : List.of(headerFlow(table, 0), cellFlow(table, 0, 0))) {
            String shown = textOf(flow);
            int icon = iconEnd(flow);
            assertTrue(icon > 0, "an icon in " + shown);
            for (int[] stretch : List.of(new int[] {0, icon - 1}, new int[] {icon, shown.length()})) {
                String text = shown.substring(stretch[0], stretch[1]);
                // The search drops its ends' whitespace, the no-break space after the icon included.
                int start = stretch[0];
                while (isSpace(shown.charAt(start))) {
                    start++;
                }
                int end = stretch[1];
                while (isSpace(shown.charAt(end - 1))) {
                    end--;
                }
                view.setSearchText(text);
                relayOut(view);
                assertEquals(1, view.getSearchMatchCount(), "'" + text + "'");
                assertEquals(shape(flow, start, end), marks(flow, "markdown-search-current"), "'" + text + "'");
            }
            view.setSearchText(shown.substring(icon - 1, icon));
            assertEquals(0, view.getSearchMatchCount(), "the icon never matches");
        }
        view.setSearchText("strong alt");
        assertEquals(0, view.getSearchMatchCount(), "an icon is no whitespace");
        view.setSearchText("https");
        assertEquals(0, view.getSearchMatchCount(), "no link address");
    }

    @Test
    @DisplayName("the matches are marked in the theme's colours in a table too; in a selected cell the selection shows"
            + " above the marks, the cells beside keep them")
    void selectedCellShowsTheSelection() {
        MarkdownView view = layOut(new MarkdownView(TABLE));
        TableView<?> table = table(view);
        view.setSearchText("anna");
        view.nextSearchMatch();
        view.nextSearchMatch();
        relayOut(view);
        assertMarkColours(view, cellFlow(table, 0, 0), "markdown-search-current",
                themeColor(view, "-fx-markdown-search-current-color"),
                themeColor(view, "-fx-markdown-search-current-text-color"));

        selectCell(table, 0, 0);
        relayOut(view);

        assertMarkColours(view, cellFlow(table, 0, 0), "markdown-search-current", Color.TRANSPARENT,
                themeColor(view, "-fx-selection-text-color"));
        assertMarkColours(view, cellFlow(table, 0, 1), "markdown-search-match",
                themeColor(view, "-fx-markdown-search-color"), themeColor(view, "-fx-markdown-search-text-color"));
        view.nextSearchMatch();
        relayOut(view);
        assertTrue(isCellSelected(table, 0, 0), "searching leaves the selection");
    }

    @Test
    @DisplayName("in a table selected whole by the text selection, the selection shows above the marks of its headers"
            + " and cells")
    void tableSelectedWholeShowsTheSelection() {
        MarkdownView view = layOut(new MarkdownView(TABLE));
        TableView<?> table = table(view);
        view.setSearchText("anna");
        view.nextSearchMatch();
        relayOut(view);
        List<TextFlow> paragraphs = paragraphs(view);
        TextFlow before = paragraphs.get(0);
        TextFlow after = paragraphs.get(1);

        drag(runAt(before, 2), textPoint(before, 2), textPoint(after, 5));
        relayOut(view);

        Paint selectionText = themeColor(view, "-fx-selection-text-color");
        assertMarkColours(view, headerFlow(table, 0), "markdown-search-current", Color.TRANSPARENT, selectionText);
        assertMarkColours(view, cellFlow(table, 1, 1), "markdown-search-match", Color.TRANSPARENT, selectionText);
        assertMarkColours(view, after, "markdown-search-match", themeColor(view, "-fx-markdown-search-color"),
                themeColor(view, "-fx-markdown-search-text-color"));
    }

    /**
     * Asserts the colours of a flow's highlight of a style class: its fill, and the fill of the characters shown on
     * it, the text runs beside it in its layer.
     */
    private static void assertMarkColours(MarkdownView view, TextFlow flow, String styleClass, Paint fill,
                                          Paint textFill) {
        Path highlight = (Path) flow.lookup("." + styleClass);
        assertTrue(highlight != null && !highlight.getElements().isEmpty(), styleClass + " shown");
        assertEquals(fill, highlight.getFill(), styleClass + " fill");
        int copies = 0;
        for (Node node : highlight.getParent().getChildrenUnmodifiable()) {
            if (node instanceof Text copy) {
                assertEquals(textFill, copy.getFill(), () -> "'" + copy.getText() + "'");
                copies++;
            }
        }
        assertTrue(copies > 0, "the characters shown on the highlight");
    }

    /**
     * The colour a theme token resolves to in the view's scene: a probe region beside the view takes the token as its
     * background, so the test does not repeat the theme's values.
     */
    private static Paint themeColor(MarkdownView view, String token) {
        Region probe = new Region();
        probe.setStyle("-fx-background-color: " + token + ";");
        StackPane root = (StackPane) view.getScene().getRoot();
        root.getChildren().add(probe);
        try {
            probe.applyCss();
            return probe.getBackground().getFills().get(0).getFill();
        } finally {
            root.getChildren().remove(probe);
        }
    }

    // --- Table changes ----------------------------------------------------------------------------------------------

    @Test
    @DisplayName("a sort numbers the matches anew in the shown order, keeps the current match on its text and the"
            + " marks on theirs")
    void sortKeepsTheCurrentMatch() {
        MarkdownView view = layOut(new MarkdownView(TABLE));
        TableView<?> table = table(view);
        view.setSearchText("anna");
        view.nextSearchMatch();
        view.nextSearchMatch();
        relayOut(view);
        assertEquals("x anna", textOf(flowWithCurrent(table)));

        sortBy(table, 0, SortType.DESCENDING);
        relayOut(view);

        assertEquals(List.of("y", "x anna"), shownColumn(table, 0), "sorted");
        assertEquals(6, view.getSearchMatchCount());
        assertEquals(3, view.getSearchMatchIndex(), "after the header and z anna, the first row now");
        assertEquals(cellFlow(table, 1, 0), flowWithCurrent(table), "x anna is still current");
        assertEquals(shape(cellFlow(table, 1, 0), 2, 6), marks(cellFlow(table, 1, 0), "markdown-search-current"));
        assertEquals(shape(cellFlow(table, 0, 1), 2, 6), marks(cellFlow(table, 0, 1), "markdown-search-match"),
                "z anna, filled anew in the first row");
        assertEquals(shape(cellFlow(table, 1, 1), 0, 4), marks(cellFlow(table, 1, 1), "markdown-search-match"));
        assertFalse(isMarked(cellFlow(table, 0, 0)), "y");

        view.nextSearchMatch();
        relayOut(view);
        assertEquals(cellFlow(table, 1, 1), flowWithCurrent(table), "next walks the rows as shown");
    }

    @Test
    @DisplayName("a moved column numbers the matches anew in the shown columns' order and keeps the current match on"
            + " its text")
    void columnMoveKeepsTheCurrentMatch() {
        MarkdownView view = layOut(new MarkdownView(TABLE));
        TableView<?> table = table(view);
        view.setSearchText("anna");
        view.nextSearchMatch();
        view.nextSearchMatch();

        moveColumn(table, 1, 0);
        relayOut(view);

        // Shown now: Name | Anna; rows "anna | x anna" and "z anna | y".
        assertEquals(6, view.getSearchMatchCount());
        assertEquals(3, view.getSearchMatchIndex(), "x anna after the header and anna, its row's first match now");
        assertEquals(cellFlow(table, 0, 1), flowWithCurrent(table));
        view.nextSearchMatch();
        relayOut(view);
        assertEquals(cellFlow(table, 1, 0), flowWithCurrent(table), "z anna, in the second row's first column");
    }

    @Test
    @DisplayName("a column resize changes nothing but the marks' shapes, which stay on their characters, at another"
            + " zoom too; a column shown again shows its marks")
    void marksFollowResizeAndZoom() {
        MarkdownView view = layOut(new MarkdownView("| Text | Other |\n|---|---|\n"
                + "| some words then anna and more words that wrap and anna at the end | anna |"));
        TableView<?> table = table(view);
        view.setSearchText("anna");
        view.nextSearchMatch();
        relayOut(view);
        int[] others = {16, 20};
        int[] current = {50, 54};

        assertTrue(resizeColumn(table, 0, -80), "narrower");
        relayOut(view);

        assertEquals(3, view.getSearchMatchCount());
        assertEquals(1, view.getSearchMatchIndex());
        TextFlow cell = cellFlow(table, 0, 0);
        assertEquals(shape(cell, current), marks(cell, "markdown-search-current"), "wrapped anew");
        assertEquals(shape(cell, others), marks(cell, "markdown-search-match"));

        view.setZoom(1.5);
        relayOut(view);

        cell = cellFlow(table, 0, 0);
        assertEquals(shape(cell, current), marks(cell, "markdown-search-current"), "at 150 %");
        assertEquals(shape(cell, others), marks(cell, "markdown-search-match"), "at 150 %");

        table.getColumns().get(1).setVisible(false);
        relayOut(view);
        table.getColumns().get(1).setVisible(true);
        relayOut(view);

        assertEquals(3, view.getSearchMatchCount());
        assertEquals(shape(cellFlow(table, 0, 1), 0, 4), marks(cellFlow(table, 0, 1), "markdown-search-match"),
                "shown again, with its mark");
    }

    /**
     * {@link #TABLE} with long paragraphs between the table and the last paragraph, so the last match lies far
     * below the table.
     */
    private static final String TABLE_FAR_ABOVE_THE_END = TABLE.replace("After anna.", longParagraphs(20)
            + "After anna.");

    @Test
    @DisplayName("hiding the column of the current match makes the first match after it current that still exists,"
            + " without scrolling; showing the column counts its matches again")
    void hidingTheCurrentColumnForward() {
        MarkdownView view = layOut(new MarkdownView(TABLE_FAR_ABOVE_THE_END));
        TableView<?> table = table(view);
        view.setSearchText("anna");
        view.nextSearchMatch();
        view.nextSearchMatch();
        relayOut(view);
        assertEquals("x anna", textOf(flowWithCurrent(table)));
        assertEquals(0, scrollPane(view).getVvalue());

        table.getColumns().get(0).setVisible(false);
        relayOut(view);

        assertEquals(4, view.getSearchMatchCount(), "the hidden column's two matches are not counted");
        assertEquals(1, view.getSearchMatchIndex(), "anna in the same row, the first match after x anna");
        assertEquals(cellFlow(table, 0, 1), flowWithCurrent(table));

        table.getColumns().get(1).setVisible(false);
        relayOut(view);

        assertEquals(2, view.getSearchMatchCount(), "no column shows");
        assertEquals(1, view.getSearchMatchIndex(), "z anna is hidden too: the paragraph far below the table");
        assertEquals(0, scrollPane(view).getVvalue(), "no scroll");

        table.getColumns().get(0).setVisible(true);
        table.getColumns().get(1).setVisible(true);
        relayOut(view);

        assertEquals(6, view.getSearchMatchCount(), "counted again");
        assertEquals(5, view.getSearchMatchIndex(), "the current match stays on its text");
        assertEquals(0, scrollPane(view).getVvalue(), "no scroll");
    }

    @Test
    @DisplayName("backward, hiding the column of the current match makes the first match before it current that still"
            + " exists, wrapping around to the last")
    void hidingTheCurrentColumnBackward() {
        MarkdownView view = layOut(new MarkdownView("""
                | A | B |
                |---|---|
                | anna | x |
                | y | anna |
                | anna | z |
                """));
        TableView<?> table = table(view);
        view.setSearchDirection(SearchDirection.BACKWARD);
        view.setSearchText("anna");
        assertEquals(2, view.getSearchMatchIndex(), "the last, in the third row");

        view.nextSearchMatch();
        view.nextSearchMatch();
        assertEquals(0, view.getSearchMatchIndex(), "the first row");
        table.getColumns().get(0).setVisible(false);
        relayOut(view);

        assertEquals(1, view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex(), "none before the first row: wrapped past the last row's, hidden"
                + " too, to the second row's in B");
        assertEquals(cellFlow(table, 1, 1), flowWithCurrent(table));
    }

    // --- Scrolling --------------------------------------------------------------------------------------------------

    /**
     * A paragraph and a table of sixty rows, the word {@code target} in the second cell of the row with index 45.
     */
    private static String longTable() {
        StringBuilder markdown = new StringBuilder("A paragraph.\n\n| Row | Text |\n|---|---|\n");
        for (int row = 0; row < 60; row++) {
            markdown.append("| row ").append(row).append(" | ").append(row == 45 ? "the target" : "words")
                    .append(" |\n");
        }
        return markdown.toString();
    }

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("a current match in a table row the visible area does not show is scrolled to its middle")
    @ValueSource(doubles = {1.0, 1.5})
    void scrollsToATableRow(double zoom) {
        MarkdownView view = layOut(new MarkdownView(longTable()));
        view.setZoom(zoom);
        relayOut(view);

        view.setSearchText("target");
        relayOut(view);

        TextFlow cell = cellFlow(table(view), 45, 1);
        Bounds match = union(sceneCharacterBounds(cell, 4), sceneCharacterBounds(cell, 9));
        assertTrue(scrollPane(view).getVvalue() > 0, "scrolled");
        assertEquals(viewportBounds(view).getCenterY(), match.getCenterY(), 1, "the match in the middle");
    }

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("a match beyond the right edge of a table wider than the view scrolls its block sideways until it"
            + " shows whole")
    @ValueSource(doubles = {1.0, 1.5})
    void wideTableScrollsSideways(double zoom) {
        MarkdownView view = layOut(new MarkdownView("| A | B | C | D | E |\n|---|---|---|---|---|\n"
                + "| Longwordnumberone | Longwordnumbertwo | Longwordnumberthree | Longwordnumberfour | the target |"));
        view.setZoom(zoom);
        relayOut(view);
        TableView<?> table = table(view);
        Region block = (Region) table.getParent();
        ScrollBar bar = blockBar(block);
        assertTrue(bar.isVisible(), "wider than the view");
        assertEquals(0, bar.getValue());

        view.setSearchText("target");
        relayOut(view);

        assertTrue(bar.getValue() > 0, "scrolled sideways");
        TextFlow cell = cellFlow(table, 0, 4);
        Bounds match = union(sceneCharacterBounds(cell, 4), sceneCharacterBounds(cell, 9));
        Bounds visible = sceneBounds(block);
        assertTrue(match.getMinX() >= visible.getMinX() && match.getMaxX() <= visible.getMaxX(),
                () -> "the match " + match + " shows whole in " + visible);

        view.setSearchText("Longwordnumberone");
        relayOut(view);

        assertEquals(0, bar.getValue(), 1e-9, "back to the table's start");
    }

    @Test
    @DisplayName("a table of hundreds of rows is searched completely, and every match can be reached and is marked")
    void longTableEveryMatch() {
        StringBuilder markdown = new StringBuilder("| Row | Text |\n|---|---|\n");
        for (int row = 0; row < 300; row++) {
            markdown.append("| row ").append(row).append(" | anna ").append(row).append(" |\n");
        }
        MarkdownView view = layOut(new MarkdownView(markdown.toString()));
        TableView<?> table = table(view);

        view.setSearchText("anna");
        assertEquals(300, view.getSearchMatchCount());

        view.previousSearchMatch();
        relayOut(view);
        assertEquals(299, view.getSearchMatchIndex(), "wrapped to the last row's");
        assertEquals(cellFlow(table, 299, 1), flowWithCurrent(table));
        assertInViewport(view, cellFlow(table, 299, 1));

        for (int i = 0; i < 150; i++) {
            view.nextSearchMatch();
        }
        relayOut(view);
        assertEquals(149, view.getSearchMatchIndex());
        assertEquals(cellFlow(table, 149, 1), flowWithCurrent(table));
        assertInViewport(view, cellFlow(table, 149, 1));
    }

    @Test
    @DisplayName("in a long table only the rows near the visible area are marked, more as the view scrolls; a row that"
            + " leaves the area keeps its marks until the search changes")
    void longTableMarksNearRowsOnly() {
        StringBuilder markdown = new StringBuilder("| Row | Text |\n|---|---|\n");
        for (int row = 0; row < 300; row++) {
            markdown.append("| row ").append(row).append(" | anna ").append(row).append(" |\n");
        }
        MarkdownView view = layOut(new MarkdownView(markdown.toString()));
        TableView<?> table = table(view);

        view.setSearchText("anna");
        relayOut(view);

        assertTrue(isMarked(cellFlow(table, 0, 1)), "the first row, in view");
        assertFalse(isMarked(cellFlow(table, 299, 1)), "the last row, far below");
        int markedAtTheTop = markedRows(table);
        assertTrue(markedAtTheTop < 60, () -> "only those near: " + markedAtTheTop);

        scrollPane(view).setVvalue(0.5);
        assertTrue(isMarked(cellFlow(table, 150, 1)), "a scroll marks the rows it brings near at once");

        view.previousSearchMatch();
        relayOut(view);

        assertTrue(isMarked(cellFlow(table, 299, 1)), "the last row, scrolled into view");
        assertTrue(isMarked(cellFlow(table, 0, 1)), "the first keeps its marks");

        view.setSearchText("anna 2");
        relayOut(view);

        assertEquals(shape(cellFlow(table, 299, 1), 0, 6), marks(cellFlow(table, 299, 1), "markdown-search-current"),
                "the current match stays");
        assertFalse(isMarked(cellFlow(table, 2, 1)), "the first rows, far above now, lost the old search's marks");
    }

    /**
     * The number of rows of a table with a marked cell.
     */
    private static int markedRows(TableView<?> table) {
        int marked = 0;
        for (int row = 0; row < table.getItems().size(); row++) {
            for (int column = 0; column < table.getColumns().size(); column++) {
                if (isMarked(cellFlow(table, row, column))) {
                    marked++;
                    break;
                }
            }
        }
        return marked;
    }

    /**
     * Asserts that a flow lies wholly in the view's visible area.
     */
    private static void assertInViewport(MarkdownView view, TextFlow flow) {
        Bounds bounds = sceneBounds(flow);
        Bounds viewport = viewportBounds(view);
        assertTrue(bounds.getMinY() >= viewport.getMinY() && bounds.getMaxY() <= viewport.getMaxY(),
                () -> bounds + " in " + viewport);
    }

    /**
     * The horizontal bar of a table's block, the one that scrolls a wide table sideways.
     */
    private static ScrollBar blockBar(Region block) {
        for (Node child : block.getChildrenUnmodifiable()) {
            if (child instanceof ScrollBar bar) {
                return bar;
            }
        }
        throw new AssertionError("no scroll bar in " + block);
    }

    /**
     * The smallest box around two boxes.
     */
    private static Bounds union(Bounds first, Bounds second) {
        double minX = Math.min(first.getMinX(), second.getMinX());
        double minY = Math.min(first.getMinY(), second.getMinY());
        double maxX = Math.max(first.getMaxX(), second.getMaxX());
        double maxY = Math.max(first.getMaxY(), second.getMaxY());
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    // --- The table's parts ------------------------------------------------------------------------------------------

    /**
     * The view's one table.
     */
    private static TableView<?> table(MarkdownView view) {
        List<Node> tables = new ArrayList<>(view.lookupAll(".markdown-table"));
        assertEquals(1, tables.size(), "tables");
        return (TableView<?>) tables.getFirst();
    }

    /**
     * Sorts a table by one column, through the table's sort order, as a click on the header does.
     */
    private static <S> void sortBy(TableView<S> table, int column, SortType type) {
        TableColumn<S, ?> sorted = table.getColumns().get(column);
        sorted.setSortType(type);
        table.getSortOrder().setAll(List.of(sorted));
    }

    /**
     * Selects a cell through the table's selection model, as a click on it does: its row by its index in the shown
     * order, its column by its index in the table's columns.
     */
    private static <S> void selectCell(TableView<S> table, int row, int column) {
        table.getSelectionModel().select(row, table.getColumns().get(column));
    }

    /**
     * Whether a cell is selected, given as for {@link #selectCell(TableView, int, int)}.
     */
    private static <S> boolean isCellSelected(TableView<S> table, int row, int column) {
        return table.getSelectionModel().isSelected(row, table.getColumns().get(column));
    }

    /**
     * Drags a column's right edge by a distance, as the resize handle on the edge of its header does through the
     * table's public API.
     *
     * @return whether the table resized a column
     */
    private static <S> boolean resizeColumn(TableView<S> table, int column, double delta) {
        return table.resizeColumn(table.getColumns().get(column), delta);
    }

    /**
     * Moves a column to another place in the table's columns in one change, as a drag of its header does.
     */
    private static <S> void moveColumn(TableView<S> table, int from, int to) {
        List<TableColumn<S, ?>> columns = new ArrayList<>(table.getColumns());
        columns.add(to, columns.remove(from));
        table.getColumns().setAll(columns);
    }

    /**
     * The texts a column shows in the rows, top to bottom; the column given by its index in the table's columns.
     */
    private static List<String> shownColumn(TableView<?> table, int column) {
        List<String> texts = new ArrayList<>();
        for (int row = 0; row < table.getItems().size(); row++) {
            texts.add(textOf(cellFlow(table, row, column)));
        }
        return texts;
    }

    /**
     * The text flow of a column's header, the column given by its index in the table's columns.
     */
    private static TextFlow headerFlow(TableView<?> table, int column) {
        return (TextFlow) table.getColumns().get(column).getGraphic();
    }

    /**
     * The text flow of a body cell: its row by its index in the shown order, its column by its index in the table's
     * columns.
     */
    private static TextFlow cellFlow(TableView<?> table, int row, int column) {
        TableColumn<?, ?> tableColumn = table.getColumns().get(column);
        for (Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> shown && shown.getIndex() == row && !shown.isEmpty()
                    && shown.isVisible()) {
                for (Node cellNode : shown.lookupAll(".table-cell")) {
                    if (cellNode instanceof TableCell<?, ?> cell && cell.getTableColumn() == tableColumn
                            && cell.getGraphic() instanceof TextFlow flow) {
                        return flow;
                    }
                }
            }
        }
        throw new AssertionError("no cell of column " + column + " in row " + row);
    }

    /**
     * The flows of the table, headers and body cells, the shown ones.
     */
    private static List<TextFlow> tableFlows(TableView<?> table) {
        List<TextFlow> flows = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) {
            flows.add((TextFlow) column.getGraphic());
        }
        for (Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> row && !row.isEmpty() && row.getIndex() >= 0 && row.isVisible()) {
                for (Node cellNode : row.lookupAll(".table-cell")) {
                    if (cellNode instanceof TableCell<?, ?> cell && cell.getGraphic() instanceof TextFlow flow) {
                        flows.add(flow);
                    }
                }
            }
        }
        return flows;
    }

    /**
     * The text a flow shows: its runs' text, an image's icon among them.
     */
    private static String textOf(TextFlow flow) {
        StringBuilder text = new StringBuilder();
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                text.append(run.getText());
            }
        }
        return text.toString();
    }

    /**
     * Whether a character is whitespace to the search: a space, a line break, a tab, a no-break space.
     */
    private static boolean isSpace(char character) {
        return Character.isWhitespace(character) || Character.isSpaceChar(character);
    }

    /**
     * The position after the one image icon a flow shows, its {@code markdown-image-cell} {@link FontIcon}.
     */
    private static int iconEnd(TextFlow flow) {
        int position = 0;
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                position += run.getText().length();
                if (run instanceof FontIcon && run.getStyleClass().contains("markdown-image-cell")) {
                    return position;
                }
            }
        }
        return -1;
    }

    /**
     * The one flow of the table that shows the current match, {@code null} if none does.
     */
    private static TextFlow flowWithCurrent(TableView<?> table) {
        TextFlow found = null;
        for (TextFlow flow : tableFlows(table)) {
            if (!marks(flow, "markdown-search-current").isEmpty()) {
                assertEquals(null, found, "one current match");
                found = flow;
            }
        }
        return found;
    }

    // --- Marks ------------------------------------------------------------------------------------------------------

    /**
     * Whether a flow shows a mark of the search: a highlight of a match, shown, with a shape.
     */
    private static boolean isMarked(TextFlow flow) {
        return !marks(flow, "markdown-search-match").isEmpty() || !marks(flow, "markdown-search-current").isEmpty();
    }

    /**
     * The shape of a flow's highlight of a style class, described; empty if the flow has none or does not show it.
     */
    private static List<String> marks(TextFlow flow, String styleClass) {
        List<String> described = new ArrayList<>();
        for (Node node : flow.lookupAll("." + styleClass)) {
            if (isShown(node, flow)) {
                described.addAll(describe(((Path) node).getElements()));
            }
        }
        return described;
    }

    /**
     * Whether a node and its parents up to a flow are visible.
     */
    private static boolean isShown(Node node, Node flow) {
        for (Node current = node; current != null && current != flow; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The shape of ranges of a flow's characters as the flow gives it, described: the expectation of a highlight.
     *
     * @param bounds the start and end of each range, in pairs
     */
    private static List<String> shape(TextFlow flow, int... bounds) {
        List<PathElement> elements = new ArrayList<>();
        for (int i = 0; i < bounds.length; i += 2) {
            elements.addAll(List.of(flow.getRangeShape(bounds[i], bounds[i + 1], true)));
        }
        return describe(elements);
    }

    /**
     * Path elements as text, to the hundredth of a pixel.
     */
    private static List<String> describe(List<PathElement> elements) {
        List<String> described = new ArrayList<>();
        for (PathElement element : elements) {
            if (element instanceof MoveTo move) {
                described.add(String.format(Locale.ROOT, "M %.2f %.2f", move.getX(), move.getY()));
            } else if (element instanceof LineTo line) {
                described.add(String.format(Locale.ROOT, "L %.2f %.2f", line.getX(), line.getY()));
            } else {
                described.add(element.getClass().getSimpleName());
            }
        }
        return described;
    }
}
