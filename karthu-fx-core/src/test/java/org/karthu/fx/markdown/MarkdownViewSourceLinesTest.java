package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.manyParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollToTop;
import static org.karthu.fx.markdown.MarkdownTestSupport.textFlows;

/**
 * The Markdown view's source lines seen from outside: {@link MarkdownView#topSourceLine()} after the visible area's
 * top edge was put into a block through the scroll pane, and {@link MarkdownView#searchMatchSourceLine()} after a
 * search through the view's API. Each section of the test texts takes four source lines: a heading on line
 * {@code 4i - 3}, a blank line, a paragraph on line {@code 4i - 1} and a blank line.
 *
 * <p>The view sits in a themed scene that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewSourceLinesTest {

    /**
     * A list after a paragraph: its items on lines 3 to 42, the item "Item 20" on line 22.
     */
    private static final String LIST = "Intro.\n\n" + items(40);

    /**
     * A table after a paragraph: its header on line 3, the delimiter row on line 4, its row {@code r} (from 0) on line
     * {@code 5 + r}, holding {@code Row r}.
     */
    private static final String TABLE = "Intro.\n\n" + table(30);

    /**
     * A code block after a paragraph: its fence on line 3, its code line {@code c} (from 0) on line {@code 4 + c},
     * holding {@code code line c}.
     */
    private static final String CODE = "Intro.\n\n" + code(40) + "\nAfter.\n";

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("without a document neither line is there, before and after the layout")
    void withoutDocument() {
        MarkdownView view = new MarkdownView();
        assertEquals(OptionalInt.empty(), view.topSourceLine());
        assertEquals(OptionalInt.empty(), view.searchMatchSourceLine());

        layOut(view);
        view.setSearchText("anything");
        assertEquals(OptionalInt.empty(), view.topSourceLine());
        assertEquals(OptionalInt.empty(), view.searchMatchSourceLine());

        view.setMarkdown("");
        assertEquals(OptionalInt.empty(), view.topSourceLine());
    }

    @Test
    @DisplayName("at the top of a document the first block's line, also before the layout")
    void topOfDocument() {
        MarkdownView view = new MarkdownView("\n\n# Title\n\nText.");
        assertEquals(OptionalInt.of(3), view.topSourceLine());

        layOut(view);
        assertEquals(OptionalInt.of(3), view.topSourceLine());
    }

    @Test
    @DisplayName("a heading cut by the top edge gives the paragraph below, a paragraph cut gives the next heading")
    void paragraphAndHeading() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));

        scrollToTop(view, sceneBounds(headings(view).get(9)), 0.5);
        assertEquals(OptionalInt.of(39), view.topSourceLine());

        scrollToTop(view, sceneBounds(paragraphs(view).get(9)), 0.5);
        assertEquals(OptionalInt.of(41), view.topSourceLine());
    }

    @Test
    @DisplayName("a list item's own line, not the list's")
    void listItem() {
        MarkdownView view = layOut(new MarkdownView(LIST));

        scrollToTop(view, sceneBounds(paragraphs(view).get(19)), 0.5);

        assertEquals(OptionalInt.of(22), view.topSourceLine());
    }

    @Test
    @DisplayName("in a table cut by the top edge the first row below it, also in a sorted table")
    void tableRow() {
        MarkdownView view = layOut(new MarkdownView(TABLE));
        TableView<?> table = (TableView<?>) view.lookup(".markdown-table");

        scrollToTop(view, sceneBounds(row(table, 10)), 0.5);
        assertEquals(OptionalInt.of(5 + 11), view.topSourceLine());

        sortDescending(table);
        relayOut(view);
        scrollToTop(view, sceneBounds(row(table, 10)), 0.5);
        // Shown row 11 is source row 29 - 11 = 18.
        assertEquals(OptionalInt.of(5 + 18), view.topSourceLine());
    }

    @Test
    @DisplayName("in a code block cut by the top edge the first code line below it; above the block, its fence")
    void codeLine() {
        MarkdownView view = layOut(new MarkdownView(CODE));
        TextFlow flow = codeFlow(view);

        scrollToTop(view, sceneCharacterBounds(flow, CODE_LINE_LENGTH * 20), 0.5);
        assertEquals(OptionalInt.of(4 + 21), view.topSourceLine());

        scrollToTop(view, sceneBounds(paragraphs(view).getFirst()), 0.5);
        assertEquals(OptionalInt.of(3), view.topSourceLine());
    }

    @Test
    @DisplayName("the top line holds after a zoom, new margins and a new version of the text, also before the next"
            + " pulse")
    void afterZoomMarginsAndUpdate() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        scrollToTop(view, sceneBounds(headings(view).get(9)), 0.5);
        assertEquals(OptionalInt.of(39), view.topSourceLine());

        view.setZoom(1.5);
        assertEquals(OptionalInt.of(39), view.topSourceLine(), "zoomed, before the next pulse");
        relayOut(view);
        assertEquals(OptionalInt.of(39), view.topSourceLine(), "zoomed");

        view.setLeftMargin(120);
        view.setRightMargin(80);
        relayOut(view);
        assertEquals(OptionalInt.of(39), view.topSourceLine(), "margins");

        view.updateMarkdown("Inserted.\n\nAnd one more.\n\n" + longParagraphs(30));
        assertEquals(OptionalInt.of(43), view.topSourceLine(), "updated, before the next pulse");
        relayOut(view);
        assertEquals(OptionalInt.of(43), view.topSourceLine(), "updated");
    }

    @Test
    @DisplayName("the search hit's line: a heading's, a paragraph's, a list item's, a table row's, a code line's")
    void searchHit() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(30)));
        view.setSearchText("Section 7");
        assertEquals(OptionalInt.of(25), view.searchMatchSourceLine());
        view.setSearchText("number 12.");
        assertEquals(OptionalInt.of(47), view.searchMatchSourceLine());

        view.setMarkdown(LIST);
        view.setSearchText("Item 20");
        assertEquals(OptionalInt.of(22), view.searchMatchSourceLine());

        view.setMarkdown(CODE);
        view.setSearchText("code line 17");
        assertEquals(OptionalInt.of(4 + 17), view.searchMatchSourceLine());

        view.setMarkdown(TABLE);
        relayOut(view);
        view.setSearchText("Row 3 ");
        assertEquals(OptionalInt.of(5 + 3), view.searchMatchSourceLine());
        view.setSearchText("Name");
        assertEquals(OptionalInt.of(3), view.searchMatchSourceLine(), "the header row");
    }

    @Test
    @DisplayName("the search hit's line follows next, a sorted table, a new version of the text and a cleared search")
    void searchHitFollows() {
        MarkdownView view = layOut(new MarkdownView(TABLE));
        TableView<?> table = (TableView<?>) view.lookup(".markdown-table");
        view.setSearchText("Row 1");
        // Row 1, then rows 10 to 19.
        assertEquals(OptionalInt.of(5 + 1), view.searchMatchSourceLine());
        view.nextSearchMatch();
        assertEquals(OptionalInt.of(5 + 10), view.searchMatchSourceLine());

        sortDescending(table);
        relayOut(view);
        assertEquals(OptionalInt.of(5 + 10), view.searchMatchSourceLine(), "the match stays on its row");

        view.updateMarkdown("New.\n\n" + TABLE);
        assertEquals(OptionalInt.of(7 + 10), view.searchMatchSourceLine(), "updated");

        view.setSearchText("no such text");
        assertEquals(OptionalInt.empty(), view.searchMatchSourceLine(), "no match");
        view.setSearchText("Row 1");
        view.clearSearch();
        assertEquals(OptionalInt.empty(), view.searchMatchSourceLine(), "cleared");
    }

    @Test
    @DisplayName("the line under a point: a list item's marker gives the item's, a code line its own, the margin none")
    void lineUnderPoint() {
        MarkdownView view = layOut(new MarkdownView("Intro.\n\n-\n  text on its own line\n\n" + code(5)));
        Label marker = (Label) view.lookup(".markdown-list-marker");
        TextFlow flow = codeFlow(view);
        Bounds third = sceneCharacterBounds(flow, CODE_LINE_LENGTH * 2);

        assertEquals(OptionalInt.of(3), SourceLines.at(marker, sceneBounds(marker).getCenterY()));
        assertEquals(OptionalInt.of(4), SourceLines.at(paragraphs(view).get(1),
                sceneBounds(paragraphs(view).get(1)).getCenterY()));
        assertEquals(OptionalInt.of(7 + 2), SourceLines.at(flow, third.getCenterY()));
        assertEquals(OptionalInt.empty(), SourceLines.at(view.lookup(".markdown-document"), third.getCenterY()));
    }

    // --- Texts -----------------------------------------------------------------------------------------------------

    /**
     * The length of a code line of {@link #code(int)} with its line feed, up to 99 lines.
     */
    private static final int CODE_LINE_LENGTH = "code line 00\n".length();

    private static String items(int count) {
        StringBuilder markdown = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            markdown.append("- Item ").append(i).append('\n');
        }
        return markdown.toString();
    }

    private static String table(int rows) {
        StringBuilder markdown = new StringBuilder("| Name | Value |\n|---|---|\n");
        for (int i = 0; i < rows; i++) {
            markdown.append("| Row ").append(i).append(" | ").append(String.format("%02d", i)).append(" |\n");
        }
        return markdown.toString();
    }

    private static String code(int lines) {
        StringBuilder markdown = new StringBuilder("```\n");
        for (int i = 0; i < lines; i++) {
            markdown.append("code line ").append(String.format("%02d", i)).append('\n');
        }
        return markdown.append("```\n").toString();
    }

    // --- The view's parts ------------------------------------------------------------------------------------------

    private static List<TextFlow> headings(MarkdownView view) {
        return textFlows(view, ".markdown-heading");
    }

    private static TextFlow codeFlow(MarkdownView view) {
        Node block = view.lookup(".markdown-code-block");
        assertNotNull(block, "code block");
        return (TextFlow) block.lookup(".markdown-block-content");
    }

    /**
     * The table's row at a shown index, as the table lays it out.
     */
    private static TableRow<?> row(TableView<?> table, int index) {
        for (Node node : table.lookupAll(".table-row-cell")) {
            if (node instanceof TableRow<?> row && row.getIndex() == index && row.isVisible()) {
                return row;
            }
        }
        throw new AssertionError("no row " + index);
    }

    /**
     * Sorts a table by its first column, descending, as two clicks on the header do.
     */
    private static <S> void sortDescending(TableView<S> table) {
        TableColumn<S, ?> column = table.getColumns().getFirst();
        column.setSortType(SortType.DESCENDING);
        table.getSortOrder().setAll(List.of(column));
    }
}
