package org.karthu.fx.markdown;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.blocks;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.image;
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
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * The Markdown view's update of its text seen from outside: {@code updateMarkdown} with a new version of the shown
 * text, the line at the top edge of the visible area found by the characters of the documented style classes' flows
 * before and after, and the search read through the view's API.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewUpdateTest {

    /**
     * Thirty sections of a heading and a paragraph that wraps over several lines.
     */
    private static final String SECTIONS = longParagraphs(30);

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a paragraph inserted above keeps the line at the top edge at the top, cut as far down")
    void paragraphInsertedAbove() {
        MarkdownView view = layOut(new MarkdownView(SECTIONS));
        TopLine before = TopLine.cut(view, paragraphs(view).get(12), 200, 0.4);

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + SECTIONS);
        relayOut(view);

        before.assertAtTheTop(view, paragraphs(view).get(13));
    }

    @Test
    @DisplayName("the paragraph at the top edge edited itself keeps its line at the top, found by its number")
    void topParagraphEdited() {
        MarkdownView view = layOut(new MarkdownView(SECTIONS));
        TopLine before = TopLine.cut(view, paragraphs(view).get(12), 200, 0.4);

        view.updateMarkdown(SECTIONS.replace("Paragraph number 13:", "Paragraph number 13 (edited):")
                .replace("lines of the view \n\n## Section 14", "lines of the view and more.\n\n## Section 14"));
        relayOut(view);

        assertTrue(textOf(paragraphs(view).get(12)).endsWith("and more."), "the paragraph was edited");
        before.assertAtTheTop(view, paragraphs(view).get(12));
    }

    @Test
    @DisplayName("a section removed above keeps the line at the top edge at the top")
    void sectionRemovedAbove() {
        MarkdownView view = layOut(new MarkdownView(SECTIONS));
        TopLine before = TopLine.cut(view, paragraphs(view).get(12), 200, 0.4);
        int start = SECTIONS.indexOf("## Section 3\n\n");

        view.updateMarkdown(SECTIONS.substring(0, start) + SECTIONS.substring(SECTIONS.indexOf("## Section 4")));
        relayOut(view);

        before.assertAtTheTop(view, paragraphs(view).get(11));
    }

    @Test
    @DisplayName("a shorter document without the paragraph at the top edge shows its end, as far as the scroll range"
            + " allows")
    void shorterDocument() {
        MarkdownView view = layOut(new MarkdownView(SECTIONS));
        TopLine.cut(view, paragraphs(view).get(25), 200, 0.4);

        view.updateMarkdown(manyParagraphs(12));
        relayOut(view);

        assertTrue(scrollRange(view) > 0, "taller than the visible area");
        assertEquals(scrollPane(view).getVmax(), scrollPane(view).getVvalue(), 1e-9, "at the end");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("at another zoom, a paragraph inserted above keeps a code block or a table at the top edge cut as far"
            + " down, and the zoom stays")
    @ValueSource(strings = {".markdown-code-block", ".markdown-table"})
    void blockInsertedAbove(String block) {
        MarkdownView view = layOut(new MarkdownView(blocks()));
        view.setZoom(1.25);
        relayOut(view);
        scrollToTop(view, sceneBounds(view.lookup(block)), 0.45);
        double cutBefore = cutFraction(view, view.lookup(block));

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + blocks());
        relayOut(view);

        assertEquals(1.25, view.getZoom());
        assertTrue(cutBefore > 0.3 && cutBefore < 0.6, () -> "the top edge cut the block: " + cutBefore);
        assertEquals(cutBefore, cutFraction(view, view.lookup(block)), 0.03, "the block is cut as far down");
    }

    @Test
    @DisplayName("while the text is bound, an update throws as setMarkdown does, and the next setMarkdown still starts"
            + " at the top")
    void boundText() {
        MarkdownView view = layOut(new MarkdownView(SECTIONS));
        TopLine.cut(view, paragraphs(view).get(12), 200, 0.4);
        StringProperty source = new SimpleStringProperty(SECTIONS);
        view.markdownProperty().bind(source);

        assertThrows(RuntimeException.class, () -> view.updateMarkdown("Another text."));
        assertThrows(RuntimeException.class, () -> view.updateMarkdown(SECTIONS), "the same text too");

        view.markdownProperty().unbind();
        view.setMarkdown("A paragraph inserted at the top.\n\n" + SECTIONS);
        relayOut(view);
        assertEquals(scrollPane(view).getVmin(), scrollPane(view).getVvalue(), "at the top");
    }

    @Test
    @DisplayName("before the first layout an update shows the new text from the top, as setMarkdown does")
    void beforeTheFirstLayout() {
        MarkdownView view = new MarkdownView(SECTIONS);

        view.updateMarkdown("The new first paragraph.\n\n" + SECTIONS);
        layOut(view);

        assertEquals("The new first paragraph.", textOf(paragraphs(view).getFirst()));
        assertEquals(scrollPane(view).getVmin(), scrollPane(view).getVvalue(), "at the top");
    }

    @Test
    @DisplayName("an update clears the selection, as a new text does")
    void selectionCleared() {
        MarkdownView view = layOut(new MarkdownView(SECTIONS));
        List<String> copied = recordCopies(view);
        TextFlow paragraph = paragraphs(view).getFirst();
        drag(runAt(paragraph, 2), textPoint(paragraph, 2), textPoint(paragraph, 12));
        shortcut(view, KeyCode.C);
        assertEquals(List.of("ragraph nu"), copied, "selected before");

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + SECTIONS);
        relayOut(view);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("ragraph nu"), copied, "nothing selected after");
    }

    @Test
    @DisplayName("an image above the line at the top edge that arrives after the update keeps the line at the top")
    void imageArrivingAfterTheUpdate() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 150, 0xFFFF0000)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        String text = "An introduction.\n\n![A shot](shot.png)\n\n" + SECTIONS;
        view.setMarkdown(text);
        layOut(view);
        work.runAll();
        relayOut(view);
        TopLine before = TopLine.cut(view, paragraphs(view).get(13), 200, 0.4);

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + text);
        relayOut(view);
        before.assertAtTheTop(view, paragraphs(view).get(14));

        work.runAll();
        relayOut(view);
        assertEquals(150, image(view).getLayoutBounds().getHeight(), "the image has arrived");
        before.assertAtTheTop(view, paragraphs(view).get(14));
    }

    @Test
    @DisplayName("an image above the line at the top edge that arrives while the new text is rendered keeps the line at"
            + " the top")
    void imageArrivingWhileRendered() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 150, 0xFFFF0000)), 1.0);
        String text = "An introduction.\n\n![A shot](shot.png)\n\n" + SECTIONS;
        view.setMarkdown(text);
        layOut(view);
        TopLine before = TopLine.cut(view, paragraphs(view).get(13), 200, 0.4);

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + text);
        relayOut(view);

        assertEquals(150, image(view).getLayoutBounds().getHeight(), "the image shows");
        before.assertAtTheTop(view, paragraphs(view).get(14));
    }

    @Test
    @DisplayName("an unchanged text changes nothing: the same flows, the line at the top where it was")
    void unchangedText() {
        MarkdownView view = layOut(new MarkdownView(SECTIONS));
        TextFlow paragraph = paragraphs(view).get(12);
        TopLine before = TopLine.cut(view, paragraph, 200, 0.4);

        view.updateMarkdown(SECTIONS);
        relayOut(view);

        assertSame(paragraph, paragraphs(view).get(12), "not rebuilt");
        before.assertAtTheTop(view, paragraph);
    }

    // --- The search -------------------------------------------------------------------------------------------------

    /**
     * Four paragraphs with {@code alpha}, the third one {@code alpha three}.
     */
    private static final String ALPHAS = "alpha one\n\nalpha two\n\nalpha three\n\nalpha two again";

    @Test
    @DisplayName("after an update the search goes on: a paragraph added above leaves the current match on its text")
    void searchKeepsTheCurrentMatch() {
        MarkdownView view = layOut(new MarkdownView(ALPHAS));
        view.setSearchText("alpha");
        view.nextSearchMatch();
        view.nextSearchMatch();
        assertEquals(2, view.getSearchMatchIndex(), "alpha three");

        view.updateMarkdown("alpha zero\n\n" + ALPHAS);
        relayOut(view);

        assertEquals("alpha", view.getSearchText());
        assertEquals(5, view.getSearchMatchCount());
        assertEquals(3, view.getSearchMatchIndex(), "still alpha three");
    }

    /**
     * The current match's own paragraph changed: it is found by its number, and the place in it is the old match's
     * start, which lies before the new match there.
     */
    @Test
    @DisplayName("the current match's paragraph edited: forward the first match at or after its place becomes current,"
            + " backward the last one at or before it")
    void currentMatchsParagraphEdited() {
        String edited = ALPHAS.replace("alpha three", "the alpha three");
        MarkdownView forward = layOut(new MarkdownView(ALPHAS));
        forward.setSearchText("alpha");
        forward.nextSearchMatch();
        forward.nextSearchMatch();
        MarkdownView backward = layOut(new MarkdownView(ALPHAS));
        backward.setSearchDirection(SearchDirection.BACKWARD);
        backward.setSearchText("alpha");
        backward.nextSearchMatch();
        assertEquals(2, backward.getSearchMatchIndex(), "alpha three");

        forward.updateMarkdown(edited);
        backward.updateMarkdown(edited);

        assertEquals(2, forward.getSearchMatchIndex(), "the alpha three, after the old start");
        assertEquals(1, backward.getSearchMatchIndex(), "alpha two, before it");
    }

    @Test
    @DisplayName("without a match at or after the old place the search wraps around to the first")
    void searchWrapsAround() {
        MarkdownView view = layOut(new MarkdownView(ALPHAS));
        view.setSearchText("alpha");
        view.previousSearchMatch();
        assertEquals(3, view.getSearchMatchIndex(), "alpha two again");

        view.updateMarkdown(ALPHAS.replace("alpha two again", "two again"));

        assertEquals(3, view.getSearchMatchCount());
        assertEquals(0, view.getSearchMatchIndex(), "wrapped around to alpha one");
    }

    @Test
    @DisplayName("an update does not scroll to the current match: the line at the top edge stays")
    void searchDoesNotScroll() {
        MarkdownView view = layOut(new MarkdownView(SECTIONS.replace("Paragraph number 25:",
                "Paragraph number 25 with the target:")));
        view.setSearchText("target");
        relayOut(view);
        TopLine before = TopLine.cut(view, paragraphs(view).get(5), 200, 0.4);

        view.updateMarkdown("The target inserted at the top.\n\n" + SECTIONS.replace("Paragraph number 25:",
                "Paragraph number 25 with the target:"));
        relayOut(view);

        assertEquals(2, view.getSearchMatchCount());
        assertEquals(1, view.getSearchMatchIndex(), "still the target in paragraph 25");
        before.assertAtTheTop(view, paragraphs(view).get(6));
    }

    /**
     * How far down a node the top edge of the visible area cuts it, as a fraction of its height.
     */
    private static double cutFraction(MarkdownView view, Node node) {
        Bounds bounds = sceneBounds(node);
        return (viewportBounds(view).getMinY() - bounds.getMinY()) / bounds.getHeight();
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
}
