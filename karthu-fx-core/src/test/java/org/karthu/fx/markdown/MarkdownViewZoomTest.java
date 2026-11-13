package org.karthu.fx.markdown;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Transform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.blocks;
import static org.karthu.fx.markdown.MarkdownTestSupport.codeRuns;
import static org.karthu.fx.markdown.MarkdownTestSupport.describeCodeRun;
import static org.karthu.fx.markdown.MarkdownTestSupport.document;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.manyParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.recordCopies;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.runAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollRange;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollToTop;
import static org.karthu.fx.markdown.MarkdownTestSupport.shortcut;
import static org.karthu.fx.markdown.MarkdownTestSupport.textFlows;
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;

/**
 * The Markdown view's zoom seen from outside: the zoom property set by an application, the document's scale, wrap
 * width and scroll position found through the documented style classes.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewZoomTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @ParameterizedTest(name = "{0} is stored as {1}")
    @DisplayName("setting the zoom stores it limited to 0.5 to 2.0 and rounded half up to whole percent")
    @CsvSource({
            "1.0, 1.0",
            "1.25, 1.25",
            "0.5, 0.5",
            "2.0, 2.0",
            "0.3, 0.5",
            "-1, 0.5",
            "2.5, 2.0",
            "1.234, 1.23",
            "1.235, 1.24",
            "0.995, 1.0",
            "0.4949, 0.5",
            "1.999, 2.0",
            "NaN, 1.0",
            "Infinity, 2.0",
            "-Infinity, 0.5"
    })
    void zoomIsLimitedAndRounded(double set, double stored) {
        MarkdownView view = layOut(new MarkdownView("Text"));

        view.setZoom(set);

        assertEquals(stored, view.getZoom());
        assertEquals(stored, view.zoomProperty().get());
    }

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("the document is scaled by the zoom from its top left corner, wraps to the visible width and scrolls"
            + " its scaled height")
    @ValueSource(doubles = {0.5, 0.75, 1.25, 1.5, 2.0})
    void documentIsScaledAndWrapsToTheVisibleWidth(double zoom) {
        MarkdownView view = layOut(new MarkdownView("word ".repeat(300) + "\n\n" + manyParagraphs(20)));
        ScrollPane scrollPane = scrollPane(view);
        Region document = document(view);

        view.setZoom(zoom);
        relayOut(view);

        Transform transform = document.getLocalToSceneTransform();
        assertEquals(zoom, transform.getMxx(), 1e-9);
        assertEquals(zoom, transform.getMyy(), 1e-9);
        assertEquals(0, transform.getMxy(), 1e-9);
        assertEquals(0, transform.getMyx(), 1e-9);
        Bounds viewport = viewportBounds(view);
        Bounds shown = sceneBounds(document);
        assertEquals(viewport.getMinX(), shown.getMinX(), 0.5, "the left edge stays at the viewport's");
        assertEquals(viewport.getMinY(), shown.getMinY(), 0.5, "the top stays at the viewport's at the top");
        double visibleWidth = scrollPane.getViewportBounds().getWidth();
        assertEquals(visibleWidth / zoom, document.getWidth(), 1, "laid out at the visible width over the zoom");
        assertEquals(visibleWidth, shown.getWidth(), 1, "shown as wide as the visible area");
        TextFlow paragraph = textFlows(view, ".markdown-paragraph").getFirst();
        assertTrue(sceneBounds(paragraph).getMaxX() <= viewport.getMaxX() + 0.5, "the paragraph wraps inside it");

        scrollPane.setVvalue(scrollPane.getVmax());
        relayOut(view);

        assertEquals(viewport.getMaxY(), sceneBounds(document).getMaxY(), 1, "scrolled to its scaled bottom");
    }

    @Test
    @DisplayName("the zoom is 1.0 by default, and at 1.0 the document has no scale, also back from another zoom")
    void noScaleAtOne() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(5)));
        Region document = document(view);

        assertEquals(1.0, view.getZoom());
        assertTrue(document.getLocalToParentTransform().isIdentity(), () -> "" + document.getLocalToParentTransform());

        view.setZoom(1.5);
        relayOut(view);
        view.setZoom(1.0);
        relayOut(view);

        assertTrue(document.getLocalToParentTransform().isIdentity(), () -> "" + document.getLocalToParentTransform());
        assertEquals(scrollPane(view).getViewportBounds().getWidth(), document.getWidth(), 0.5);
    }

    @Test
    @DisplayName("a bound zoom keeps the value it is bound to, and the view applies it limited and rounded")
    void boundZoomIsAppliedLimitedAndRounded() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(5)));
        DoubleProperty source = new SimpleDoubleProperty(3.0);

        view.zoomProperty().bind(source);
        relayOut(view);

        assertEquals(3.0, view.getZoom());
        assertEquals(2.0, scaleOf(view), 1e-9);

        source.set(1.333);
        relayOut(view);

        assertEquals(1.333, view.getZoom());
        assertEquals(1.33, scaleOf(view), 1e-9);

        source.set(Double.NaN);
        relayOut(view);

        assertTrue(document(view).getLocalToParentTransform().isIdentity(), "not a number counts as 1.0");
    }

    @Test
    @DisplayName("a bidirectionally bound zoom: the view applies the limited and rounded value while the application's"
            + " property may keep the raw one, and a zoom of the reader reaches the application")
    void bidirectionallyBoundZoom() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(5)));
        DoubleProperty application = new SimpleDoubleProperty(1.0);
        view.zoomProperty().bindBidirectional(application);
        TextField field = (TextField) view.lookup(".markdown-zoom-field");

        application.set(1.234);
        relayOut(view);

        assertEquals(1.234, application.get(), "the application's property keeps the raw value");
        assertEquals(1.23, view.getZoom(), "the view's property holds the limited and rounded value");
        assertEquals(1.23, scaleOf(view), 1e-9, "the view applies it");
        assertEquals("123%", field.getText());

        application.set(5.0);
        relayOut(view);

        assertEquals(5.0, application.get(), "the application's property keeps the raw value");
        assertEquals(2.0, view.getZoom());
        assertEquals(2.0, scaleOf(view), 1e-9);

        ((Button) view.lookup(".markdown-zoom-out")).fire();

        assertEquals(1.5, view.getZoom());
        assertEquals(1.5, application.get(), "the reader's zoom reaches the application");
    }

    @ParameterizedTest(name = "{0} to {1}")
    @DisplayName("a text selection stays selected across a zoom change, as across a new width: the same characters are"
            + " highlighted, and the copy gives the same text")
    @CsvSource({"property, 1.5", "property, 0.5", "shortcut, 1.25"})
    void selectionStaysAcrossAZoom(String how, double zoom) {
        // Both paragraphs in the visible area, the first wrapping over several lines.
        MarkdownView view = layOut(new MarkdownView("Paragraph number 1: "
                + "words that wrap over several lines of the view ".repeat(4) + "\n\nParagraph number 2: the end."));
        List<String> copied = recordCopies(view);
        TextFlow first = paragraphs(view).get(0);
        TextFlow second = paragraphs(view).get(1);
        drag(runAt(first, 20), textPoint(first, 20), textPoint(second, 27));
        shortcut(view, KeyCode.C);

        if (how.equals("shortcut")) {
            shortcut(view, KeyCode.PLUS);
        } else {
            view.setZoom(zoom);
        }
        relayOut(view);
        shortcut(view, KeyCode.C);

        assertEquals(zoom, view.getZoom());
        assertEquals(2, copied.size());
        assertTrue(copied.getFirst().startsWith("words that wrap")
                        && copied.getFirst().endsWith("of the view\n\nParagraph number 2: the end"),
                () -> "the selection from the first paragraph into the second: " + copied.getFirst());
        assertEquals(copied.getFirst(), copied.get(1), "the same text after the zoom");
        Bounds firstHighlight = sceneBounds(first.lookup(".markdown-selection"));
        Bounds secondHighlight = sceneBounds(second.lookup(".markdown-selection"));
        Bounds firstSelected = sceneCharacterBounds(first, 20);
        Bounds lastSelected = sceneCharacterBounds(second, 26);
        assertEquals(firstSelected.getMinY(), firstHighlight.getMinY(), zoom, "highlighted from the same character");
        assertEquals(sceneBounds(first).getMaxY(), firstHighlight.getMaxY(), 2 * zoom, "to the first one's end");
        assertEquals(sceneBounds(second).getMinY(), secondHighlight.getMinY(), zoom, "from the second one's start");
        assertEquals(lastSelected.getMaxY(), secondHighlight.getMaxY(), 2 * zoom, "to the same character");
        assertEquals(lastSelected.getMaxX(), secondHighlight.getMaxX(), zoom, "to the same character");
    }

    @ParameterizedTest(name = "zoom {0}")
    @DisplayName("a coloured java block keeps its runs, their classes and their colours at another zoom, grows and"
            + " shrinks with the document, keeps its selection across the zoom, and a drag and the shortcut key plus C"
            + " select and copy its code at the zoom as at 1.0")
    @ValueSource(doubles = {0.5, 1.25, 1.5, 2.0})
    void coloredBlockAtAnotherZoom(double zoom) {
        MarkdownView view = layOut(new MarkdownView(
                "Text.\n\n```java\nint total = price * count;\nreturn total;\n```"));
        List<String> copied = recordCopies(view);
        Region block = (Region) view.lookup(".markdown-code-block");
        TextFlow flow = (TextFlow) block.lookup(".markdown-block-content");
        List<String> runs = List.of("keyword:int", "plain: total ", "operator:=", "plain: price ", "operator:*",
                "plain: count", "punctuation:;", "plain:\n", "keyword:return", "plain: total", "punctuation:;");
        assertEquals(runs, describeRuns(block), "coloured at 1.0");
        List<Paint> fills = fills(block);
        drag(runAt(flow, 4), textPoint(flow, 4), textPoint(flow, 17));
        shortcut(view, KeyCode.C);

        view.setZoom(zoom);
        relayOut(view);
        shortcut(view, KeyCode.C);

        assertEquals(runs, describeRuns(block), "the same runs with the same classes");
        assertEquals(fills, fills(block), "in the same colours");
        assertEquals(zoom, flow.getLocalToSceneTransform().getMxx(), 1e-9, "scaled with the document");
        assertEquals(zoom, flow.getLocalToSceneTransform().getMyy(), 1e-9, "scaled with the document");
        Bounds highlight = sceneBounds(block.lookup(".markdown-selection"));
        assertEquals(sceneCharacterBounds(flow, 4).getMinX(), highlight.getMinX(), zoom, "still from the fifth");
        assertEquals(sceneCharacterBounds(flow, 16).getMaxX(), highlight.getMaxX(), zoom, "to the seventeenth");
        assertEquals(sceneCharacterBounds(flow, 4).getHeight(), highlight.getHeight(), zoom, "on one line");

        drag(runAt(flow, 32), textPoint(flow, 32), textPoint(flow, 2));
        relayOut(view);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("total = price", "total = price", "t total = price * count;\nretur"), copied);
        assertEquals(2 * sceneCharacterBounds(flow, 2).getHeight(),
                sceneBounds(block.lookup(".markdown-selection")).getHeight(), 2 * zoom, "highlighted over both lines");
    }

    @ParameterizedTest(name = "from {0} to {1}")
    @DisplayName("a new zoom keeps the line at the top of the visible area at the top, cut as far down as before,"
            + " though the text wraps anew")
    @CsvSource({"1.0, 1.5", "1.0, 0.5", "1.5, 2.0", "2.0, 0.75", "0.5, 1.25"})
    void topLineStaysAtTheTop(double from, double to) {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        view.setZoom(from);
        relayOut(view);
        TextFlow paragraph = textFlows(view, ".markdown-paragraph").get(12);
        scrollToTop(view, sceneCharacterBounds(paragraph, 200), 0.4);
        double top = viewportBounds(view).getMinY();
        Point2D atTop = paragraph.sceneToLocal(sceneBounds(paragraph).getMinX() + 4, top + 0.5);
        int character = paragraph.getHitInfo(atTop).getCharIndex();
        Bounds before = sceneCharacterBounds(paragraph, character);
        double cutBefore = (top - before.getMinY()) / before.getHeight();

        view.setZoom(to);
        relayOut(view);

        Bounds after = sceneCharacterBounds(paragraph, character);
        double cutAfter = (top - after.getMinY()) / after.getHeight();
        assertTrue(cutBefore > 0 && cutBefore < 1, () -> "the line was cut by the top edge: " + cutBefore);
        assertEquals(cutBefore, cutAfter, 0.05, "the line is still at the top, cut as far down");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("a new zoom keeps a code block, a table or the gap between blocks at the top as far down as before")
    @ValueSource(strings = {"code", "table", "gap"})
    void topBlockStaysAtTheTop(String cut) {
        MarkdownView view = layOut(new MarkdownView(blocks()));
        view.setZoom(1.25);
        relayOut(view);
        Region code = (Region) view.lookup(".markdown-code-block");
        Region table = (Region) view.lookup(".markdown-table");
        Node below;
        double cutBefore;
        if (cut.equals("gap")) {
            // The top edge 4px (at 1.25) above the code block, in the spacing between it and the paragraph above.
            below = code;
            scrollToTop(view, sceneBounds(code), 0);
            scrollPane(view).setVvalue(scrollPane(view).getVvalue() - 4 / scrollRange(view));
            relayOut(view);
            cutBefore = sceneBounds(below).getMinY() - viewportBounds(view).getMinY();
        } else {
            below = cut.equals("code") ? code : table;
            scrollToTop(view, sceneBounds(below), 0.45);
            cutBefore = cutFraction(view, below);
        }

        view.setZoom(1.75);
        relayOut(view);

        if (cut.equals("gap")) {
            double cutAfter = sceneBounds(below).getMinY() - viewportBounds(view).getMinY();
            assertTrue(cutBefore > 1, () -> "the top edge was above the block: " + cutBefore);
            assertEquals(cutBefore * 1.75 / 1.25, cutAfter, 1, "the gap above the block scales with the zoom");
        } else {
            assertTrue(cutBefore > 0.3 && cutBefore < 0.6, () -> "the top edge cut the block: " + cutBefore);
            assertEquals(cutBefore, cutFraction(view, below), 0.03, "the block is cut as far down");
        }
    }

    @Test
    @DisplayName("at the end of the document a smaller zoom keeps the end in view, as far as the scroll range allows")
    void smallerZoomAtTheEnd() {
        // Taller than the visible area below the zoom bar at 75 %, shorter at 50 %.
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(6)));
        view.setZoom(1.5);
        relayOut(view);
        ScrollPane scrollPane = scrollPane(view);
        scrollPane.setVvalue(scrollPane.getVmax());
        relayOut(view);

        view.setZoom(0.75);
        relayOut(view);

        assertTrue(scrollRange(view) > 0, "still taller than the visible area");
        assertEquals(scrollPane.getVmax(), scrollPane.getVvalue(), 1e-9);
        assertEquals(viewportBounds(view).getMaxY(), sceneBounds(document(view)).getMaxY(), 1);

        view.setZoom(0.5);
        relayOut(view);

        assertTrue(sceneBounds(document(view)).getHeight() < viewportBounds(view).getHeight(), "all of it fits");
        assertEquals(viewportBounds(view).getMinY(), sceneBounds(document(view)).getMinY(), 0.5, "shown from its top");
    }

    @Test
    @DisplayName("the zoom stays when the text changes, and a new text still scrolls to the top")
    void zoomSurvivesNewText() {
        MarkdownView view = layOut(new MarkdownView(manyParagraphs(30)));
        view.setZoom(1.5);
        relayOut(view);
        ScrollPane scrollPane = scrollPane(view);
        scrollPane.setVvalue(0.6);
        relayOut(view);

        view.setMarkdown(longParagraphs(10));
        relayOut(view);

        assertEquals(1.5, view.getZoom());
        assertEquals(1.5, scaleOf(view), 1e-9);
        assertEquals(scrollPane.getVmin(), scrollPane.getVvalue());
        assertEquals(viewportBounds(view).getMinY(), sceneBounds(document(view)).getMinY(), 0.5);
        assertEquals(scrollPane.getViewportBounds().getWidth() / 1.5, document(view).getWidth(), 1);
    }

    @Test
    @DisplayName("zooming twice before the next layout keeps the line at the top that the first zoom found there")
    void twoZoomsBeforeALayout() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        TextFlow paragraph = textFlows(view, ".markdown-paragraph").get(12);
        scrollToTop(view, sceneCharacterBounds(paragraph, 200), 0.5);
        double top = viewportBounds(view).getMinY();
        int character = paragraph.getHitInfo(paragraph.sceneToLocal(sceneBounds(paragraph).getMinX() + 4, top + 0.5))
                .getCharIndex();

        view.setZoom(1.5);
        view.setZoom(2.0);
        relayOut(view);

        Bounds after = sceneCharacterBounds(paragraph, character);
        assertTrue(after.getMinY() <= top && after.getMaxY() > top, () -> "the line at the top: " + after);
    }

    /**
     * The scale the document is shown at, in the scene.
     */
    private static double scaleOf(MarkdownView view) {
        return document(view).getLocalToSceneTransform().getMxx();
    }

    /**
     * How far down a node the top edge of the visible area cuts it, as a fraction of its height.
     */
    private static double cutFraction(MarkdownView view, Node node) {
        Bounds bounds = sceneBounds(node);
        return (viewportBounds(view).getMinY() - bounds.getMinY()) / bounds.getHeight();
    }

    /**
     * The runs of a code block as their finest category's name and their text, each checked to carry exactly the
     * classes of its category, see {@link MarkdownTestSupport#describeCodeRun}.
     */
    private static List<String> describeRuns(Node block) {
        List<String> runs = new ArrayList<>();
        for (Text run : codeRuns(block)) {
            runs.add(describeCodeRun(run));
        }
        return runs;
    }

    /**
     * The fills of a code block's runs, in order.
     */
    private static List<Paint> fills(Node block) {
        List<Paint> fills = new ArrayList<>();
        for (Text run : codeRuns(block)) {
            fills.add(run.getFill());
        }
        return fills;
    }
}
