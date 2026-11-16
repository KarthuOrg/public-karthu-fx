package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.document;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.image;
import static org.karthu.fx.markdown.MarkdownTestSupport.isInside;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.pickAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.recordCopies;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.runAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollToTop;
import static org.karthu.fx.markdown.MarkdownTestSupport.shortcut;
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * The Markdown view's left and right margins seen from outside: the margin properties set by an application, the
 * text's place found through the documented style classes, at every zoom and in a narrow view.
 *
 * <p>The view sits in a themed scene that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewMarginsTest {

    /**
     * A paragraph that wraps over several lines in every view of the tests.
     */
    private static final String PROSE = "Paragraph: " + "words that wrap over several lines of the view ".repeat(12);

    /**
     * A paragraph of a few lines, which needs no vertical scroll bar in the tests' narrow views.
     */
    private static final String SHORT_PROSE = "Paragraph: " + "words that wrap ".repeat(8);

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("unset, the margins are not a number and the theme's padding holds: 14 pixels left and right, 6 above")
    void unsetMarginsLeaveTheThemesPadding() {
        MarkdownView view = layOut(new MarkdownView(PROSE));

        assertTrue(Double.isNaN(view.getLeftMargin()), "left margin unset");
        assertTrue(Double.isNaN(view.getRightMargin()), "right margin unset");
        assertText(view, 14, 14, 1.0);
        assertEquals(6, sceneBounds(paragraphs(view).getFirst()).getMinY() - viewportBounds(view).getMinY(), 0.5,
                "the theme's top padding");
    }

    @Test
    @DisplayName("set, the margins replace the theme's left and right padding; the top stays the theme's")
    void setMarginsReplaceTheThemesSidePadding() {
        MarkdownView view = layOut(new MarkdownView(PROSE));

        view.setLeftMargin(40);
        view.setRightMargin(25);
        relayOut(view);

        assertEquals(40, view.getLeftMargin());
        assertEquals(25, view.getRightMargin());
        assertText(view, 40, 25, 1.0);
        assertEquals(6, sceneBounds(paragraphs(view).getFirst()).getMinY() - viewportBounds(view).getMinY(), 0.5,
                "the theme's top padding");
    }

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("the zoom scales the margins with the document")
    @ValueSource(doubles = {0.5, 0.75, 1.25, 1.5, 2.0})
    void zoomScalesTheMargins(double zoom) {
        MarkdownView view = layOut(new MarkdownView(PROSE), 1000);
        view.setLeftMargin(40);
        view.setRightMargin(25);

        view.setZoom(zoom);
        relayOut(view);

        assertText(view, 40 * zoom, 25 * zoom, zoom);
    }

    @ParameterizedTest(name = "margins {0} and {1} at zoom {2} in a view {3} wide: {4} and {5}")
    @DisplayName("a view narrower than both margins and 300 pixels of text, all at the zoom, shrinks the margins in"
            + " their proportion, so the text keeps 300 pixels; with less room they are 0")
    @CsvSource({
            // Room enough, also just so: the margins as set.
            "100, 50, 1.0, 450, 100, 50",
            "100, 50, 1.0, 600, 100, 50",
            "40, 20, 1.25, 450, 50, 25",
            // Narrower: the text keeps 300 pixels at the zoom, the margins share the rest in their proportion.
            "100, 50, 1.0, 400, 66.667, 33.333",
            "40, 20, 1.25, 400, 16.667, 8.333",
            "100, 100, 0.5, 200, 25, 25",
            "0, 80, 1.0, 340, 0, 40",
            // No room beyond the text: both 0.
            "100, 50, 1.0, 300, 0, 0",
            "100, 50, 1.0, 250, 0, 0",
            "40, 20, 2.0, 500, 0, 0"
    })
    void narrowViewShrinksTheMargins(double left, double right, double zoom, double width, double shownLeft,
                                     double shownRight) {
        // The view's frame is 1 pixel wide on each side; the short text needs no vertical scroll bar.
        MarkdownView view = new MarkdownView(SHORT_PROSE);
        view.setLeftMargin(left);
        view.setRightMargin(right);
        view.setZoom(zoom);
        layOut(view, width + 2);
        relayOut(view);

        assertEquals(width, scrollPane(view).getViewportBounds().getWidth(), 1e-6, "the visible width");
        assertText(view, shownLeft, shownRight, zoom);
    }

    @Test
    @DisplayName("the margins follow a new width of the view at once: wider, they grow back to their size")
    void marginsFollowTheWidth() {
        MarkdownView view = new MarkdownView(SHORT_PROSE);
        view.setLeftMargin(100);
        view.setRightMargin(50);
        layOut(view, 402);
        assertText(view, 66.667, 33.333, 1.0);

        resizeScene(view, 602);

        assertText(view, 100, 50, 1.0);
    }

    @ParameterizedTest(name = "from {0} to {1} pixels wide")
    @DisplayName("a new width that shrinks the margins anew scrolls the height of the text as it wraps between them,"
            + " in one layout: the document ends with the theme's 14 pixels below its last line")
    @CsvSource({"402, 352", "352, 402", "402, 602", "602, 352", "352, 302"})
    void scrolledHeightFollowsTheShrunkMargins(double from, double to) {
        MarkdownView view = new MarkdownView(PROSE + "\n\n" + PROSE + "\n\n" + PROSE);
        view.setLeftMargin(100);
        view.setRightMargin(50);
        layOut(view, from);

        resizeScene(view, to);

        Bounds document = sceneBounds(document(view));
        assertEquals(scrollPane(view).getContent().getLayoutBounds().getHeight(), document.getHeight(), 0.5,
                "the scrolled height is the document's");
        assertEquals(14, document.getMaxY() - sceneBounds(paragraphs(view).getLast()).getMaxY(), 0.5,
                "the document as tall as its text");
    }

    @ParameterizedTest(name = "margins {0} and {1} in a view {2} wide at zoom {3}")
    @DisplayName("the margins never scroll the view sideways, however large and at every zoom")
    @CsvSource({"1000, 1000, 402, 1.0", "1000, 1000, 120, 1.0", "500, 2000, 302, 2.0", "Infinity, 30, 402, 1.0"})
    void noSidewaysScrolling(double left, double right, double width, double zoom) {
        MarkdownView view = new MarkdownView(PROSE);
        view.setLeftMargin(left);
        view.setRightMargin(right);
        view.setZoom(zoom);
        layOut(view, width);
        relayOut(view);

        assertNoSidewaysScrolling(view);
    }

    @Test
    @DisplayName("a view that narrows from room for the full margins to less than the text shows no horizontal scroll"
            + " bar either")
    void noSidewaysScrollingAfterNarrowing() {
        MarkdownView view = new MarkdownView(PROSE);
        view.setLeftMargin(1000);
        view.setRightMargin(1000);
        layOut(view, 2600);
        assertText(view, 1000, 1000, 1.0);

        resizeScene(view, 402);

        assertNoSidewaysScrolling(view);
        // Equal margins share what the text's 300 pixels leave of the visible width.
        double margin = (scrollPane(view).getViewportBounds().getWidth() - 300) / 2;
        assertTrue(margin > 0 && margin <= 50, () -> "a narrow view: " + margin);
        assertText(view, margin, margin, 1.0);
    }

    /**
     * Asserts that the view does not scroll sideways: no horizontal scroll bar, the document as wide as the visible
     * area, its text inside.
     */
    private static void assertNoSidewaysScrolling(MarkdownView view) {
        ScrollPane scrollPane = scrollPane(view);
        assertFalse(horizontalBar(view).isVisible(), "no horizontal scroll bar");
        assertEquals(scrollPane.getViewportBounds().getWidth(), scrollPane.getContent().getLayoutBounds().getWidth(),
                0.5, "the document as wide as the visible area");
        assertTrue(sceneBounds(paragraphs(view).getFirst()).getMaxX() <= viewportBounds(view).getMaxX() + 0.5,
                "the text inside it");
    }

    @Test
    @DisplayName("a margin below 0 counts as 0, and not a number gives the side back to the theme's padding")
    void negativeAndNotANumber() {
        MarkdownView view = layOut(new MarkdownView(PROSE));

        view.setLeftMargin(-20);
        view.setRightMargin(-1);
        relayOut(view);

        assertEquals(-20, view.getLeftMargin(), "kept as set");
        assertText(view, 0, 0, 1.0);

        view.setLeftMargin(Double.NaN);
        view.setRightMargin(Double.NaN);
        relayOut(view);

        assertText(view, 14, 14, 1.0);
    }

    @Test
    @DisplayName("one margin set: the other side keeps the theme's padding, and a narrow view shrinks only the margin")
    void oneMarginSet() {
        MarkdownView view = new MarkdownView(SHORT_PROSE);
        view.setLeftMargin(30);
        layOut(view, 602);

        assertText(view, 30, 14, 1.0);

        view.setLeftMargin(150);
        resizeScene(view, 402);

        // The visible 400 pixels less the theme's 14 and the text's 300.
        assertText(view, 86, 14, 1.0);
    }

    @Test
    @DisplayName("a stylesheet's padding of markdown-document holds while no margin is set; set margins replace its"
            + " sides and keep its top and bottom")
    void stylesheetPadding() {
        MarkdownView view = layOut(new MarkdownView(SHORT_PROSE));
        view.getScene().getStylesheets().add(stylesheet(
                ".markdown-view .markdown-document { -fx-padding: 20 30 40 50; }"));
        relayOut(view);

        assertText(view, 50, 30, 1.0);
        assertTopAndBottom(view, 20, 40);

        view.setLeftMargin(10);
        view.setRightMargin(5);
        relayOut(view);

        assertText(view, 10, 5, 1.0);
        assertTopAndBottom(view, 20, 40);
    }

    // --- What follows the margins ----------------------------------------------------------------------------------

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("a table fits its columns between the margins, its long cells wrapping there")
    @ValueSource(doubles = {1.0, 1.5})
    void tableFitsBetweenTheMargins(double zoom) {
        MarkdownView view = new MarkdownView("| Name | Meaning |\n|---|---|\n"
                + ("| key | " + "a cell that wraps between the margins ".repeat(4) + "|\n").repeat(3));
        view.setLeftMargin(80);
        view.setRightMargin(40);
        view.setZoom(zoom);
        layOut(view, 1000);
        relayOut(view);

        Bounds viewport = viewportBounds(view);
        Bounds table = sceneBounds(view.lookup(".markdown-table"));
        assertEquals(viewport.getMinX() + 80 * zoom, table.getMinX(), zoom + 0.01, "the table's left edge");
        assertEquals(viewport.getMaxX() - 40 * zoom, table.getMaxX(), zoom + 0.01, "the table's right edge");
    }

    @Test
    @DisplayName("a wide image shrinks to the width between the margins")
    void imageShrinksBetweenTheMargins() {
        MarkdownView view = viewWithImages(Map.of("wide.png", png(1600, 400, 0xFFFF0000)), 1.0);
        view.setMarkdown("Some text.\n\n![wide](wide.png)");
        view.setLeftMargin(80);
        view.setRightMargin(40);
        layOut(view, 600);

        Bounds viewport = viewportBounds(view);
        Bounds image = sceneBounds(image(view));
        assertEquals(viewport.getMinX() + 80, image.getMinX(), 0.5, "the image's left edge");
        assertEquals(sceneBounds(paragraphs(view).getFirst()).getWidth(), image.getWidth(), 1e-6,
                "as wide as the text between the margins");
    }

    @Test
    @DisplayName("the search's marks stay on their characters when new margins wrap the text anew")
    void searchMarksFollowTheMargins() {
        MarkdownView view = layOut(new MarkdownView(PROSE), 600);
        TextFlow paragraph = paragraphs(view).getFirst();
        view.setSearchText("several lines");
        view.nextSearchMatch();
        view.nextSearchMatch();
        relayOut(view);
        assertEquals(2, view.getSearchMatchIndex(), "the third match");
        // Where the paragraph's text holds the third match: the prose without "Paragraph: ", 47 characters a sentence.
        int start = PROSE.indexOf("several lines") + 2 * 47;
        Bounds before = sceneBounds(paragraph.lookup(".markdown-search-current"));
        assertSameBox(markedShape(paragraph, start, start + 13), before, "on the current match");

        view.setLeftMargin(150);
        view.setRightMargin(90);
        relayOut(view);

        Bounds after = sceneBounds(paragraph.lookup(".markdown-search-current"));
        assertTrue(Math.abs(after.getMinX() - before.getMinX()) > 10 || Math.abs(after.getMinY() - before.getMinY()) > 5,
                () -> "the text wrapped anew: " + before + " " + after);
        assertSameBox(markedShape(paragraph, start, start + 13), after, "still on the current match");
    }

    @Test
    @DisplayName("a selection stays on its characters across new margins, and a press in a margin is a press in the"
            + " document: a plain click there clears the selection")
    void selectionFollowsTheMargins() {
        MarkdownView view = layOut(new MarkdownView(PROSE + "\n\nParagraph 2: the end."), 600);
        view.setLeftMargin(60);
        view.setRightMargin(30);
        relayOut(view);
        List<String> copied = recordCopies(view);
        TextFlow first = paragraphs(view).get(0);
        TextFlow second = paragraphs(view).get(1);
        drag(runAt(first, 20), textPoint(first, 20), textPoint(second, 12));
        shortcut(view, KeyCode.C);

        view.setLeftMargin(120);
        view.setRightMargin(100);
        relayOut(view);
        shortcut(view, KeyCode.C);

        assertEquals(2, copied.size());
        assertEquals(copied.getFirst(), copied.get(1), "the same text across new margins");
        Bounds highlight = sceneBounds(first.lookup(".markdown-selection"));
        Bounds firstSelected = sceneCharacterBounds(first, 20);
        assertEquals(firstSelected.getMinY(), highlight.getMinY(), 1, "highlighted from the same character");
        assertEquals(viewportBounds(view).getMinX() + 120, sceneBounds(first).getMinX(), 0.5, "the new margin");

        // In the left margin, at the height of the first paragraph's second line.
        Point2D inTheMargin = new Point2D(viewportBounds(view).getMinX() + 40, textPoint(first, 60).getY());
        Node pressed = pickAt(view.getScene().getRoot(), inTheMargin);
        assertTrue(isInside(pressed, document(view)), () -> "the document takes the press: " + pressed);
        drag(pressed, inTheMargin, inTheMargin);
        shortcut(view, KeyCode.C);

        assertEquals(2, copied.size(), "nothing selected, nothing copied");
    }

    @ParameterizedTest(name = "from {0} to {1}")
    @DisplayName("a new zoom keeps the line at the top of the visible area at the top, the margins shrinking or growing"
            + " with it")
    @CsvSource({"1.0, 1.5", "1.5, 1.0", "1.0, 0.5"})
    void zoomKeepsTheTopLine(double from, double to) {
        MarkdownView view = new MarkdownView(longParagraphs(30));
        view.setLeftMargin(80);
        view.setRightMargin(40);
        view.setZoom(from);
        layOut(view);
        relayOut(view);
        TopLine before = TopLine.cut(view, paragraphs(view).get(12), 200, 0.4);

        view.setZoom(to);
        relayOut(view);

        before.assertAtTheTop(view, paragraphs(view).get(12));
    }

    @Test
    @DisplayName("an update keeps the line at the top of the visible area at the top between the margins")
    void updateKeepsTheTopLine() {
        MarkdownView view = new MarkdownView(longParagraphs(30));
        view.setLeftMargin(80);
        view.setRightMargin(40);
        layOut(view, 600);
        TopLine before = TopLine.cut(view, paragraphs(view).get(12), 200, 0.4);

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + longParagraphs(30));
        relayOut(view);

        before.assertAtTheTop(view, paragraphs(view).get(13));
    }

    @ParameterizedTest(name = "{0} and {1} to {2} and {3}")
    @DisplayName("new margins keep the line at the top of the visible area at the top, though the text wraps anew")
    @CsvSource({"NaN, NaN, 100, 60", "100, 60, 20, 0", "20, 20, 200, 150"})
    void newMarginsKeepTheTopLine(double fromLeft, double fromRight, double toLeft, double toRight) {
        MarkdownView view = new MarkdownView(longParagraphs(30));
        view.setLeftMargin(fromLeft);
        view.setRightMargin(fromRight);
        layOut(view, 700);
        TopLine before = TopLine.cut(view, paragraphs(view).get(12), 200, 0.4);

        view.setLeftMargin(toLeft);
        view.setRightMargin(toRight);
        relayOut(view);

        before.assertAtTheTop(view, paragraphs(view).get(12));
    }

    /**
     * The box of the highlight of characters of a flow, in the scene: the shape the flow gives the range.
     */
    private static Bounds markedShape(TextFlow flow, int start, int end) {
        Path shape = new Path(flow.getRangeShape(start, end, true));
        shape.setStroke(null);
        shape.setFill(Color.BLACK);
        return flow.localToScene(shape.getLayoutBounds());
    }

    /**
     * Asserts that two boxes of the scene are the same, to a hundredth of a pixel.
     */
    private static void assertSameBox(Bounds expected, Bounds actual, String message) {
        assertEquals(expected.getMinX(), actual.getMinX(), 0.01, message + ", left");
        assertEquals(expected.getMinY(), actual.getMinY(), 0.01, message + ", top");
        assertEquals(expected.getMaxX(), actual.getMaxX(), 0.01, message + ", right");
        assertEquals(expected.getMaxY(), actual.getMaxY(), 0.01, message + ", bottom");
    }

    /**
     * The line the top edge of the visible area cuts: a character of a paragraph and how far down its line the edge
     * cuts it.
     *
     * @param character the index of a character of the line in the paragraph's text
     * @param cut       how far down the line the top edge cuts it, a fraction of its height
     */
    private record TopLine(int character, double cut) {

        /**
         * Scrolls the view so that the top edge cuts the line of a character of a paragraph a fraction of its height
         * down, and notes the line's first character there.
         */
        static TopLine cut(MarkdownView view, TextFlow paragraph, int character, double fraction) {
            scrollToTop(view, sceneCharacterBounds(paragraph, character), fraction);
            double top = viewportBounds(view).getMinY();
            Point2D atTop = paragraph.sceneToLocal(sceneBounds(paragraph).getMinX() + 4, top + 0.5);
            int first = paragraph.getHitInfo(atTop).getCharIndex();
            Bounds line = sceneCharacterBounds(paragraph, first);
            TopLine topLine = new TopLine(first, (top - line.getMinY()) / line.getHeight());
            assertTrue(topLine.cut() > 0 && topLine.cut() < 1, () -> "the line is cut by the top edge: " + topLine);
            return topLine;
        }

        /**
         * Asserts that the top edge cuts the line of the same character of a paragraph as far down.
         */
        void assertAtTheTop(MarkdownView view, TextFlow paragraph) {
            Bounds line = sceneCharacterBounds(paragraph, character);
            double cutAfter = (viewportBounds(view).getMinY() - line.getMinY()) / line.getHeight();
            assertEquals(cut, cutAfter, 0.05, "the line is still at the top, cut as far down");
        }
    }

    /**
     * Asserts that the first paragraph lies between margins of the visible area: its box from the left margin to the
     * right one, its wrapped text inside that box, within one pixel of the document: the layout snaps the box's
     * width to whole pixels of the document, which the zoom scales.
     */
    private static void assertText(MarkdownView view, double left, double right, double zoom) {
        Bounds viewport = viewportBounds(view);
        TextFlow paragraph = paragraphs(view).getFirst();
        Bounds box = sceneBounds(paragraph);
        assertEquals(viewport.getMinX() + left, box.getMinX(), zoom + 0.01, "the text's left edge");
        assertEquals(viewport.getMaxX() - right, box.getMaxX(), zoom + 0.01, "the text's right edge");
    }

    /**
     * Asserts the space above the first paragraph and below the last one, inside the document, at zoom 1.0.
     */
    private static void assertTopAndBottom(MarkdownView view, double top, double bottom) {
        Bounds document = sceneBounds(document(view));
        assertEquals(top, sceneBounds(paragraphs(view).getFirst()).getMinY() - document.getMinY(), 0.5, "top");
        assertEquals(bottom, document.getMaxY() - sceneBounds(paragraphs(view).getLast()).getMaxY(), 0.5, "bottom");
    }

    /**
     * Gives the scene's root a new width and lays the view out again, as a resized window does.
     */
    private static void resizeScene(MarkdownView view, double width) {
        ((StackPane) view.getScene().getRoot()).resize(width, view.getScene().getHeight());
        relayOut(view);
    }

    /**
     * The view's horizontal scroll bar, its scroll pane's own.
     */
    private static ScrollBar horizontalBar(MarkdownView view) {
        for (Node node : scrollPane(view).lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar bar && bar.getOrientation() == Orientation.HORIZONTAL
                    && bar.getParent() == scrollPane(view)) {
                return bar;
            }
        }
        throw new AssertionError("no horizontal scroll bar");
    }

    /**
     * A stylesheet of CSS text, as a data address.
     */
    private static String stylesheet(String css) {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }
}
