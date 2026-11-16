package org.karthu.fx.markdown;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.recordCopies;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.runAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.shortcut;
import static org.karthu.fx.markdown.MarkdownTestSupport.textFlows;
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;

/**
 * The Markdown view's search seen from outside: the search text, the number of matches and the current match set and
 * read through the view's API, the marks found by their documented style classes and the scrolling by the scroll
 * pane's offset.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewSearchTest {

    /**
     * A document with the word {@code anna} in every kind of block the search runs through: a heading, a paragraph, a
     * list item, a nested quote, a code block and a source block (indented code), seven times in all.
     */
    private static final String EVERY_BLOCK = """
            # Anna's heading

            A paragraph about ANNA and anna.

            - an item with Anna
              > a quote in the item: anna

            ```
            code with anna
            ```

            A paragraph between.

                indented anna
            """;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("without a search there are no matches and no current match")
    void noSearch() {
        MarkdownView view = layOut(new MarkdownView(EVERY_BLOCK));

        assertEquals(null, view.getSearchText());
        assertEquals(0, view.getSearchMatchCount());
        assertEquals(-1, view.getSearchMatchIndex());
        assertEquals(SearchDirection.FORWARD, view.getSearchDirection());
    }

    @Test
    @DisplayName("a search counts its matches in headings, paragraphs, list items, quotes, code and source blocks, the"
            + " first one current")
    void searchCountsEveryBlock() {
        MarkdownView view = layOut(new MarkdownView(EVERY_BLOCK));

        view.setSearchText("anna");

        assertEquals(7, view.getSearchMatchCount());
        assertEquals(7, view.searchMatchCountProperty().get());
        assertEquals(0, view.getSearchMatchIndex());
        assertEquals(0, view.searchMatchIndexProperty().get());
    }

    @Test
    @DisplayName("forward, next goes down and previous up, both wrapping around at the ends")
    void nextAndPreviousForward() {
        MarkdownView view = layOut(new MarkdownView(EVERY_BLOCK));
        view.setSearchText("anna");

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 0, 1), indicesAfter(view, 8, true));
        assertEquals(List.of(0, 6, 5), indicesAfter(view, 3, false));
    }

    @Test
    @DisplayName("backward, a new search starts at the last match, next goes up and previous down, both wrapping"
            + " around; the indices still count from the top")
    void nextAndPreviousBackward() {
        MarkdownView view = layOut(new MarkdownView(EVERY_BLOCK));
        view.setSearchDirection(SearchDirection.BACKWARD);
        view.setSearchText("anna");

        assertEquals(6, view.getSearchMatchIndex());
        assertEquals(List.of(5, 4, 3, 2, 1, 0, 6), indicesAfter(view, 7, true));
        assertEquals(List.of(0, 1), indicesAfter(view, 2, false));
    }

    @Test
    @DisplayName("a change of the direction moves no match; null counts as forward")
    void directionChangeMovesNothing() {
        MarkdownView view = layOut(new MarkdownView(EVERY_BLOCK));
        view.setSearchText("anna");
        view.nextSearchMatch();
        view.nextSearchMatch();

        view.setSearchDirection(SearchDirection.BACKWARD);
        assertEquals(2, view.getSearchMatchIndex());
        view.nextSearchMatch();
        assertEquals(1, view.getSearchMatchIndex());

        view.setSearchDirection(null);
        assertEquals(null, view.getSearchDirection());
        assertEquals(1, view.getSearchMatchIndex());
        view.nextSearchMatch();
        assertEquals(2, view.getSearchMatchIndex());
        view.previousSearchMatch();
        assertEquals(1, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("without matches, next and previous do nothing")
    void nextWithoutMatches() {
        MarkdownView view = layOut(new MarkdownView(EVERY_BLOCK));
        view.setSearchText("bernhard");

        view.nextSearchMatch();
        view.previousSearchMatch();

        assertEquals(0, view.getSearchMatchCount());
        assertEquals(-1, view.getSearchMatchIndex());
    }

    /**
     * Four paragraphs that all start with {@code alpha}.
     */
    private static final String ALPHAS = "alpha one\n\nalpha two\n\nalpha three\n\nalpha two again";

    @Test
    @DisplayName("typing on keeps the current match while it still matches, else the next one forward becomes current,"
            + " wrapping to the first")
    void typingOnForward() {
        MarkdownView view = layOut(new MarkdownView(ALPHAS));
        view.setSearchText("alpha");
        view.nextSearchMatch();
        view.nextSearchMatch();
        assertEquals(2, view.getSearchMatchIndex(), "alpha three");

        view.setSearchText("alpha t");
        assertEquals(3, view.getSearchMatchCount());
        assertEquals(1, view.getSearchMatchIndex(), "still alpha three");

        view.setSearchText("alpha tw");
        assertEquals(2, view.getSearchMatchCount());
        assertEquals(1, view.getSearchMatchIndex(), "the next below, alpha two again");

        view.setSearchText("alpha thr");
        assertEquals(1, view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex(), "none below: from the top, alpha three");
    }

    @Test
    @DisplayName("typing on backward: the next one above becomes current, wrapping to the last")
    void typingOnBackward() {
        MarkdownView view = layOut(new MarkdownView(ALPHAS));
        view.setSearchDirection(SearchDirection.BACKWARD);
        view.setSearchText("alpha");
        view.nextSearchMatch();
        assertEquals(2, view.getSearchMatchIndex(), "alpha three");

        view.setSearchText("alpha tw");
        assertEquals(0, view.getSearchMatchIndex(), "the next above, alpha two");

        view.setSearchText("alpha two a");
        assertEquals(1, view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex(), "none above: from the bottom, alpha two again");
    }

    @Test
    @DisplayName("a search after one without matches is a new search: it starts at the first match")
    void searchAfterNoMatches() {
        MarkdownView view = layOut(new MarkdownView(ALPHAS));
        view.setSearchText("alpha");
        view.nextSearchMatch();
        view.nextSearchMatch();

        view.setSearchText("alphax");
        assertEquals(0, view.getSearchMatchCount());
        assertEquals(-1, view.getSearchMatchIndex());

        view.setSearchText("alpha");
        assertEquals(0, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("null, the empty text and whitespace only are no search; clearSearch ends the search")
    void noSearchTexts() {
        MarkdownView view = layOut(new MarkdownView(ALPHAS));
        for (String none : Arrays.asList(null, "", "  \t ", "\u00A0")) {
            view.setSearchText("alpha");
            view.setSearchText(none);
            assertEquals(0, view.getSearchMatchCount(), () -> "'" + none + "'");
            assertEquals(-1, view.getSearchMatchIndex(), () -> "'" + none + "'");
        }

        view.setSearchText(" alpha  two ");
        assertEquals(2, view.getSearchMatchCount(), "the ends dropped, the space one run");

        view.clearSearch();
        assertEquals(null, view.getSearchText());
        assertEquals(0, view.getSearchMatchCount());
        assertEquals(-1, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("the search text binds both ways; bound one way, clearSearch throws as setting a bound property does")
    void boundSearchText() {
        MarkdownView view = layOut(new MarkdownView(ALPHAS));
        StringProperty field = new SimpleStringProperty("two");

        view.searchTextProperty().bindBidirectional(field);
        assertEquals(2, view.getSearchMatchCount());
        view.clearSearch();
        assertEquals(null, field.get());

        view.searchTextProperty().unbindBidirectional(field);
        view.searchTextProperty().bind(field);
        field.set("three");
        assertEquals(1, view.getSearchMatchCount());
        assertThrows(RuntimeException.class, () -> view.clearSearch());
    }

    @Test
    @DisplayName("a new text keeps the search and runs it on the new text, the current match chosen as for a new"
            + " search, without scrolling to it")
    void newTextRunsTheSearch() {
        MarkdownView view = layOut(new MarkdownView(ALPHAS));
        view.setSearchText("alpha");
        view.nextSearchMatch();
        view.nextSearchMatch();

        view.setMarkdown(longParagraphs(20) + "\n\nalpha at the end\n\nand alpha again");
        relayOut(view);

        assertEquals("alpha", view.getSearchText());
        assertEquals(2, view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex());
        assertEquals(0, scrollPane(view).getVvalue(), "at the top");

        view.setSearchDirection(SearchDirection.BACKWARD);
        view.setMarkdown(longParagraphs(20) + "\n\nalpha at the end\n\nand alpha again\n\nalpha");
        relayOut(view);

        assertEquals(3, view.getSearchMatchCount());
        assertEquals(2, view.getSearchMatchIndex(), "backward, the last");
        assertEquals(0, scrollPane(view).getVvalue(), "still at the top");
    }

    @Test
    @DisplayName("a rebuild for a change of the code languages runs the search anew, as a new text does")
    void rebuildRunsTheSearch() {
        MarkdownView view = layOut(new MarkdownView(EVERY_BLOCK));
        view.setSearchText("anna");
        view.nextSearchMatch();
        view.nextSearchMatch();

        view.getCodeLanguages().remove("java");

        assertEquals(7, view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex());
    }

    @Test
    @DisplayName("an empty text has no matches; a text set later is searched")
    void emptyText() {
        MarkdownView view = layOut(new MarkdownView());
        view.setSearchText("alpha");

        assertEquals(0, view.getSearchMatchCount());
        assertEquals(-1, view.getSearchMatchIndex());

        view.setMarkdown(ALPHAS);

        assertEquals(4, view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex());
    }

    // --- Scrolling --------------------------------------------------------------------------------------------------

    /**
     * Thirty long paragraphs, one with {@code target} at the end of its first line, thirty more.
     */
    private static final String TARGET_IN_THE_MIDDLE = longParagraphs(30) + "\n\nThe target is here.\n\n"
            + longParagraphs(30);

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("a new search scrolls a current match that is not visible to the middle of the visible area")
    @ValueSource(doubles = {1.0, 1.5})
    void newSearchScrollsToTheMiddle(double zoom) {
        MarkdownView view = layOut(new MarkdownView(TARGET_IN_THE_MIDDLE));
        view.setZoom(zoom);
        relayOut(view);

        view.setSearchText("target");
        relayOut(view);

        assertInTheMiddle(view, "The target is here.", 4, 10);
    }

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("next and previous scroll to a current match that is not visible, and leave a visible one where it is")
    @ValueSource(doubles = {1.0, 1.25})
    void nextAndPreviousScroll(double zoom) {
        MarkdownView view = layOut(new MarkdownView("alpha and alpha\n\n" + longParagraphs(10) + "\n\nThe alpha in"
                + " the middle.\n\n" + longParagraphs(10)));
        view.setZoom(zoom);
        relayOut(view);
        view.setSearchText("alpha");
        relayOut(view);
        assertEquals(0, scrollPane(view).getVvalue(), "the first match is visible");

        view.nextSearchMatch();
        relayOut(view);
        assertEquals(0, scrollPane(view).getVvalue(), "so is the second");

        view.nextSearchMatch();
        relayOut(view);
        assertInTheMiddle(view, "The alpha in the middle.", 4, 9);

        double inTheMiddle = scrollPane(view).getVvalue();
        view.previousSearchMatch();
        relayOut(view);
        assertEquals(0, scrollPane(view).getVvalue(), 1e-9, "back up, as far as the range allows");

        view.previousSearchMatch();
        relayOut(view);
        assertEquals(0, view.getSearchMatchIndex());
        assertEquals(0, scrollPane(view).getVvalue(), 1e-9, "the first match is visible");

        view.previousSearchMatch();
        relayOut(view);
        assertEquals(inTheMiddle, scrollPane(view).getVvalue(), 1e-9, "wrapped around to the last");
    }

    @Test
    @DisplayName("a match near the end scrolls as far as the scroll range allows")
    void matchNearTheEnd() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(20) + "\n\nThe target at the end."));

        view.setSearchText("target");
        relayOut(view);

        assertEquals(scrollPane(view).getVmax(), scrollPane(view).getVvalue(), 1e-9);
    }

    @Test
    @DisplayName("a search set before the view is laid out scrolls to its current match at the first layout")
    void searchBeforeTheFirstLayout() {
        MarkdownView view = new MarkdownView(TARGET_IN_THE_MIDDLE);
        view.setSearchText("target");

        layOut(view);
        relayOut(view);

        assertInTheMiddle(view, "The target is here.", 4, 10);
    }

    @Test
    @DisplayName("a match beyond the right edge of a code block scrolls the block sideways until it shows whole")
    void codeBlockScrollsSideways() {
        MarkdownView view = layOut(new MarkdownView("Text.\n\n```\nshort line\n" + "x".repeat(150)
                + " target at the end\n```"));
        ScrollPane block = (ScrollPane) view.lookup(".markdown-code-block .scroll-pane");
        TextFlow code = (TextFlow) view.lookup(".markdown-code-block .markdown-block-content");
        int start = "short line\n".length() + 151;
        assertEquals(0, block.getHvalue());

        view.setSearchText("target");
        relayOut(view);

        assertTrue(block.getHvalue() > 0, "scrolled sideways");
        Bounds viewport = sceneBounds(block.lookup(".viewport"));
        Bounds match = union(sceneCharacterBounds(code, start), sceneCharacterBounds(code, start + 5));
        assertTrue(match.getMinX() >= viewport.getMinX() && match.getMaxX() <= viewport.getMaxX(),
                () -> "the match " + match + " shows whole in " + viewport);
        assertEquals(0, scrollPane(view).getVvalue(), "the block is visible: the view does not scroll");

        view.setSearchText("short");
        relayOut(view);

        assertEquals(0, block.getHvalue(), 1e-9, "back to the start of the line");
    }

    /**
     * Asserts that a match in the paragraph of a text is the current one and lies in the vertical middle of the
     * visible area, to a pixel.
     */
    private static void assertInTheMiddle(MarkdownView view, String paragraphText, int start, int end) {
        TextFlow paragraph = null;
        for (TextFlow flow : paragraphs(view)) {
            if (textOf(flow).equals(paragraphText)) {
                paragraph = flow;
            }
        }
        assertTrue(paragraph != null, paragraphText);
        Bounds match = union(sceneCharacterBounds(paragraph, start), sceneCharacterBounds(paragraph, end - 1));
        Bounds viewport = viewportBounds(view);
        assertTrue(scrollPane(view).getVvalue() > 0, "scrolled");
        assertEquals(viewport.getCenterY(), match.getCenterY(), 1, "the match in the middle");
    }

    /**
     * The text of a flow's runs.
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
     * The smallest box around two boxes.
     */
    private static Bounds union(Bounds first, Bounds second) {
        double minX = Math.min(first.getMinX(), second.getMinX());
        double minY = Math.min(first.getMinY(), second.getMinY());
        double maxX = Math.max(first.getMaxX(), second.getMaxX());
        double maxY = Math.max(first.getMaxY(), second.getMaxY());
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    // --- Marks ------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("every match but the current one is marked in the search colour, the current one in the current"
            + " colour, each highlight in the shape of its characters, the characters shown on it in its text colour")
    void marksInAParagraph() {
        MarkdownView view = layOut(new MarkdownView("Anna and anna and ANNA."));
        TextFlow paragraph = paragraphs(view).getFirst();

        view.setSearchText("anna");
        relayOut(view);

        Path matches = highlight(paragraph, "markdown-search-match");
        Path current = highlight(paragraph, "markdown-search-current");
        assertEquals(shape(paragraph, 9, 13, 18, 22), describe(matches.getElements()));
        assertEquals(shape(paragraph, 0, 4), describe(current.getElements()));
        assertEquals(themeColor(view, "-fx-markdown-search-color"), matches.getFill());
        assertEquals(themeColor(view, "-fx-markdown-search-current-color"), current.getFill());
        assertCopiesIn(matches, themeColor(view, "-fx-markdown-search-text-color"));
        assertCopiesIn(current, themeColor(view, "-fx-markdown-search-current-text-color"));

        view.nextSearchMatch();
        relayOut(view);

        assertEquals(shape(paragraph, 0, 4, 18, 22), describe(matches.getElements()));
        assertEquals(shape(paragraph, 9, 13), describe(current.getElements()));
    }

    @Test
    @DisplayName("the theme's colours of the marks: a muted amber behind the text colour, the current match in amber"
            + " behind a dark text")
    void markColours() {
        MarkdownView view = layOut(new MarkdownView("Anna"));

        assertEquals(Color.web("#e0a84c"), themeColor(view, "-fx-markdown-search-current-color"));
        assertEquals(Color.web("#1b1d2d"), themeColor(view, "-fx-markdown-search-current-text-color"));
        assertEquals(themeColor(view, "-fx-text-color"), themeColor(view, "-fx-markdown-search-text-color"));
        Color muted = (Color) themeColor(view, "-fx-markdown-search-color");
        Color amber = Color.web("#e0a84c");
        assertEquals(amber.getHue(), muted.getHue(), 1, "an amber");
        assertTrue(muted.getBrightness() < amber.getBrightness() - 0.2, "muted: darker than the current match's");
        assertNotEquals(themeColor(view, "-fx-selection-color"), muted, "apart from the selection's blue");
    }

    @Test
    @DisplayName("over the runs lie all matches, the current match, and the selection on top, whether the selection or"
            + " the search came first; searching leaves the selection as it is")
    void selectionLiesAboveTheMarks() {
        MarkdownView view = layOut(new MarkdownView("Anna and anna and ANNA.\n\nSecond anna."));
        List<String> copied = recordCopies(view);
        TextFlow first = paragraphs(view).get(0);
        TextFlow second = paragraphs(view).get(1);

        drag(runAt(first, 2), textPoint(first, 2), textPoint(first, 12));
        view.setSearchText("anna");
        view.nextSearchMatch();
        view.nextSearchMatch();
        view.nextSearchMatch();
        relayOut(view);
        drag(runAt(second, 1), textPoint(second, 1), textPoint(second, 5));
        relayOut(view);

        List<String> layers = List.of("markdown-search-match", "markdown-search-current", "markdown-selection");
        assertEquals(layers, highlightsInDrawOrder(first));
        assertEquals(layers, highlightsInDrawOrder(second));
        shortcut(view, KeyCode.C);
        assertEquals(List.of("econ"), copied, "the reader's selection, untouched by the search");
    }

    @Test
    @DisplayName("matches in code and source blocks are marked in their flows")
    void marksInCodeAndSourceBlocks() {
        MarkdownView view = layOut(new MarkdownView("```\nint anna = 1;\n```\n\nText.\n\n    anna and anna\n"));
        TextFlow code = (TextFlow) view.lookup(".markdown-code-block .markdown-block-content");
        TextFlow source = (TextFlow) view.lookup(".markdown-source-block .markdown-block-content");

        view.setSearchText("anna");
        relayOut(view);

        assertEquals(3, view.getSearchMatchCount());
        assertEquals(shape(code, 4, 8), describe(highlight(code, "markdown-search-current").getElements()));
        assertEquals(shape(source, 4, 8, 13, 17), describe(highlight(source, "markdown-search-match").getElements()));
        assertEquals(List.of("markdown-search-match", "markdown-search-current", "markdown-selection"),
                highlightsInDrawOrder(code));
    }

    @Test
    @DisplayName("an inline image is a position no match takes, and its alt text is not searched; matches after it"
            + " are marked at their characters")
    void inlineImages() {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        view.setMarkdown("Anna ![anna alt](a.png) anna, and ![x](b.png)anna![y](c.png)");
        layOut(view);
        TextFlow paragraph = paragraphs(view).getFirst();

        view.setSearchText("anna");
        relayOut(view);

        assertEquals(3, view.getSearchMatchCount(), "not the alt text");
        // Flow positions: "Anna " 0-4, the image 5, " anna, and " 6-16, the image 17, "anna" 18-21, the image 22.
        assertEquals(shape(paragraph, 7, 11, 18, 22), describe(highlight(paragraph, "markdown-search-match")
                .getElements()));

        view.setSearchText("anna anna");
        assertEquals(0, view.getSearchMatchCount(), "an image is no whitespace");
        view.setSearchText("andanna");
        assertEquals(0, view.getSearchMatchCount(), "nor nothing");
    }

    @Test
    @DisplayName("not searched: list markers, link addresses, the alt text of block images; a match never runs from"
            + " one paragraph into the next")
    void notSearched() {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        view.setMarkdown("1. first [link](https://anna.example/one \"anna title\")\n2. second\n\n"
                + "![anna](anna.png)\n\nend of one\n\nstart of two\n\n<https://example.org/anna>");
        layOut(view);

        for (String none : List.of("1.", "anna.example", "anna title", "one start")) {
            view.setSearchText(none);
            assertEquals(0, view.getSearchMatchCount(), none);
        }
        view.setSearchText("anna");
        assertEquals(1, view.getSearchMatchCount(), "only the autolink's text, which the reader sees");
    }

    @Test
    @DisplayName("only the flows near the visible area are marked, more as the view scrolls; a flow that leaves the"
            + " area keeps its marks until the search changes")
    void marksNearTheVisibleAreaOnly() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(60)));
        List<TextFlow> flows = paragraphs(view);

        view.setSearchText("wrap");
        relayOut(view);

        assertEquals(60 * 12, view.getSearchMatchCount(), "every match is counted");
        assertTrue(isMarked(flows.getFirst()), "the first paragraph, in view");
        assertTrue(!isMarked(flows.getLast()), "the last paragraph, far below");
        int markedAtTheTop = countMarked(flows);
        assertTrue(markedAtTheTop < 15, () -> "only those near: " + markedAtTheTop);

        view.previousSearchMatch();
        relayOut(view);

        assertEquals(60 * 12 - 1, view.getSearchMatchIndex(), "the last match");
        assertTrue(isMarked(flows.getLast()), "the last paragraph, scrolled into view");
        assertTrue(isMarked(flows.getFirst()), "the first keeps its marks");

        view.setSearchText("wrap over");
        relayOut(view);

        assertTrue(isMarked(flows.getLast()), "the last, with the new search");
        assertTrue(!isMarked(flows.getFirst()), "the first, far above now, lost the old search's marks");
    }

    /**
     * The matches of {@code words that} but the second, the current one, in the paragraph of
     * {@link #marksFollowWrapAndZoom()}: {@code Paragraph } and ten times {@code with words that wrap }, 21 characters
     * each.
     */
    private static final int[] OTHERS = {15, 25, 57, 67, 78, 88, 99, 109, 120, 130, 141, 151, 162, 172, 183, 193, 204,
        214};

    @Test
    @DisplayName("a scroll of the reader marks the flows it brings near at once, before the next layout")
    void scrollMarksAtOnce() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(60)));
        List<TextFlow> flows = paragraphs(view);
        view.setSearchText("wrap");
        relayOut(view);
        assertTrue(!isMarked(flows.getLast()));

        scrollPane(view).setVvalue(scrollPane(view).getVmax());

        assertTrue(isMarked(flows.getLast()), "the last paragraph, scrolled into view");
        assertEquals(0, view.getSearchMatchIndex(), "a scroll moves no match");
    }

    @Test
    @DisplayName("the marks stay on their characters when the view is resized and wraps anew, and at another zoom")
    void marksFollowWrapAndZoom() {
        MarkdownView view = layOut(new MarkdownView("Paragraph " + "with words that wrap ".repeat(10) + "end."));
        TextFlow paragraph = paragraphs(view).getFirst();
        view.setSearchText("words that");
        view.nextSearchMatch();
        relayOut(view);

        view.setMaxWidth(260);
        relayOut(view);

        assertEquals(shape(paragraph, OTHERS), describe(highlight(paragraph, "markdown-search-match").getElements()),
                "every match but the current one, wrapped anew");
        assertEquals(shape(paragraph, 36, 46), describe(highlight(paragraph, "markdown-search-current")
                .getElements()));

        view.setZoom(1.5);
        relayOut(view);

        assertEquals(shape(paragraph, 36, 46), describe(highlight(paragraph, "markdown-search-current")
                .getElements()), "at 150 %");
        assertEquals(shape(paragraph, OTHERS), describe(highlight(paragraph, "markdown-search-match").getElements()),
                "at 150 %");
    }

    @Test
    @DisplayName("clearing the search takes every mark away")
    void clearingTakesTheMarksAway() {
        MarkdownView view = layOut(new MarkdownView(EVERY_BLOCK));
        view.setSearchText("anna");
        relayOut(view);
        assertTrue(countMarked(textFlowsOfTheDocument(view)) >= 6);

        view.clearSearch();
        relayOut(view);

        assertEquals(0, countMarked(textFlowsOfTheDocument(view)));
    }

    /**
     * Every text flow of the view's document: headings, paragraphs, the flows of code and source blocks.
     */
    private static List<TextFlow> textFlowsOfTheDocument(MarkdownView view) {
        List<TextFlow> flows = new ArrayList<>(textFlows(view, ".markdown-heading"));
        flows.addAll(paragraphs(view));
        flows.addAll(textFlows(view, ".markdown-block-content"));
        return flows;
    }

    /**
     * The number of flows that show marks of the search.
     */
    private static int countMarked(List<TextFlow> flows) {
        int marked = 0;
        for (TextFlow flow : flows) {
            if (isMarked(flow)) {
                marked++;
            }
        }
        return marked;
    }

    /**
     * Whether a flow shows a mark of the search: a highlight of a match, shown, with a shape.
     */
    private static boolean isMarked(TextFlow flow) {
        List<Node> highlights = new ArrayList<>(flow.lookupAll(".markdown-search-match"));
        highlights.addAll(flow.lookupAll(".markdown-search-current"));
        for (Node node : highlights) {
            if (isShown(node) && !((Path) node).getElements().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The style classes of the highlights of a flow, in the order they are drawn: a depth-first walk of its children.
     */
    private static List<String> highlightsInDrawOrder(Node flow) {
        List<String> classes = new ArrayList<>();
        if (flow instanceof Path path) {
            for (String styleClass : path.getStyleClass()) {
                if (styleClass.startsWith("markdown-search-") || styleClass.equals("markdown-selection")) {
                    classes.add(styleClass);
                }
            }
        }
        if (flow instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                classes.addAll(highlightsInDrawOrder(child));
            }
        }
        return classes;
    }

    /**
     * The one highlight of a style class in a flow, with a shape.
     */
    private static Path highlight(TextFlow flow, String styleClass) {
        List<Path> found = new ArrayList<>();
        for (Node node : flow.lookupAll("." + styleClass)) {
            found.add((Path) node);
        }
        assertEquals(1, found.size(), () -> "highlights " + styleClass);
        assertTrue(isShown(found.getFirst()), () -> styleClass + " shown");
        return found.getFirst();
    }

    /**
     * Whether a node and all its parents are visible.
     */
    private static boolean isShown(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
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

    /**
     * Asserts that the characters shown on a highlight, the text runs beside it in its layer, take a colour.
     */
    private static void assertCopiesIn(Path highlight, Paint fill) {
        int copies = 0;
        for (Node node : highlight.getParent().getChildrenUnmodifiable()) {
            if (node instanceof Text copy) {
                assertEquals(fill, copy.getFill(), () -> "'" + copy.getText() + "'");
                copies++;
            }
        }
        assertTrue(copies > 0, "the characters are shown on the highlight");
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

    /**
     * The current match's index after each of a number of steps.
     *
     * @param next true for {@code nextSearchMatch}, false for {@code previousSearchMatch}
     */
    private static List<Integer> indicesAfter(MarkdownView view, int steps, boolean next) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            if (next) {
                view.nextSearchMatch();
            } else {
                view.previousSearchMatch();
            }
            indices.add(view.getSearchMatchIndex());
        }
        return indices;
    }
}
