package org.karthu.fx.markdown;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.blocks;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.image;
import static org.karthu.fx.markdown.MarkdownTestSupport.imageBlocks;
import static org.karthu.fx.markdown.MarkdownTestSupport.images;
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
import static org.karthu.fx.markdown.TestImages.BLUE;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.colors;
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

    /**
     * Paragraphs, a table of six rows whose cells wrap in the test scene and which sorts differently by its first
     * column than its source has the rows, and paragraphs again.
     */
    private static final String SORTABLE_TABLE = longParagraphs(6) + "| Key | Meaning |\n|---|---|\n"
            + "| key 1 | a cell that wraps in a narrow column of the table, the first row |\n"
            + "| key 2 | a cell that wraps in a narrow column of the table, the second row |\n"
            + "| key 3 | a cell that wraps in a narrow column of the table, the third row |\n"
            + "| key 4 | a cell that wraps in a narrow column of the table, the fourth row |\n"
            + "| key 5 | a cell that wraps in a narrow column of the table, the fifth row |\n"
            + "| key 6 | a cell that wraps in a narrow column of the table, the sixth row |\n\n"
            + longParagraphs(6);

    @Test
    @DisplayName("a table the reader sorted is found again by its content: a paragraph inserted above keeps it at the"
            + " top edge, cut as far down")
    void sortedTableInsertedAbove() {
        MarkdownView view = layOut(new MarkdownView(SORTABLE_TABLE));
        TableView<?> table = (TableView<?>) view.lookup(".markdown-table");
        sortDescending(table, 0);
        relayOut(view);
        scrollToTop(view, sceneBounds(table), 0.45);
        double cutBefore = cutFraction(view, table);

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + SORTABLE_TABLE);
        relayOut(view);

        assertTrue(cutBefore > 0.3 && cutBefore < 0.6, () -> "the top edge cut the table: " + cutBefore);
        assertEquals(cutBefore, cutFraction(view, view.lookup(".markdown-table")), 0.03,
                "the table is cut as far down");
    }

    /**
     * Sorts a table by one of its columns, the largest first, through the table's sort order, as two clicks on the
     * column's header do.
     */
    private static <S> void sortDescending(TableView<S> table, int column) {
        TableColumn<S, ?> sorted = table.getColumns().get(column);
        sorted.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().setAll(List.of(sorted));
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

    // --- Images -----------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @DisplayName("while the update loads an image again, it keeps showing its picture at its size, then shows the new"
            + " one in place")
    @ValueSource(strings = {"![A shot](shot.png)", "See ![A shot](shot.png) here."})
    void imageKeepsItsPictureUntilLoaded(String image) {
        Map<String, byte[]> files = new HashMap<>();
        files.put("shot.png", png(60, 40, RED));
        MarkdownView view = viewWithImages(files, 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("An introduction.\n\n" + image);
        layOut(view);
        work.runAll();
        relayOut(view);
        files.put("shot.png", png(30, 50, BLUE));

        view.updateMarkdown("A new introduction.\n\n" + image);
        relayOut(view);

        assertNull(view.lookup(".markdown-image-placeholder"), "no placeholder");
        ImageView old = image(view);
        assertEquals(60, old.getLayoutBounds().getWidth(), "the old picture at its size");
        assertEquals(40, old.getLayoutBounds().getHeight());
        assertEquals(List.of(RED), colors(old.getImage(), 30, 20), "the old picture");

        work.runAll();
        relayOut(view);

        ImageView reloaded = image(view);
        assertEquals(30, reloaded.getLayoutBounds().getWidth(), "the new picture at its size");
        assertEquals(50, reloaded.getLayoutBounds().getHeight());
        assertEquals(List.of(BLUE), colors(reloaded.getImage(), 15, 25), "the new picture");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("an image that showed its error box keeps showing it while the update loads it again, then shows the"
            + " file found now")
    @ValueSource(strings = {"![A shot](shot.png)", "See ![A shot](shot.png) here."})
    void imageKeepsItsErrorBoxUntilLoaded(String image) {
        Map<String, byte[]> files = new HashMap<>();
        MarkdownView view = viewWithImages(files, 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("An introduction.\n\n" + image);
        layOut(view);
        work.runAll();
        relayOut(view);
        assertNotNull(view.lookup(".markdown-image-error"), "missing before");
        files.put("shot.png", png(30, 50, BLUE));

        view.updateMarkdown("A new introduction.\n\n" + image);
        relayOut(view);

        assertNull(view.lookup(".markdown-image-placeholder"), "no placeholder");
        assertNotNull(view.lookup(".markdown-image-error"), "still the error box");

        work.runAll();
        relayOut(view);

        assertNull(view.lookup(".markdown-image-error"), "the error box is gone");
        assertEquals(30, image(view).getLayoutBounds().getWidth(), "the file found now");
    }

    @Test
    @DisplayName("a picture whose file is gone keeps showing until the update's load, then shows the error box")
    void pictureWhoseFileIsGone() {
        Map<String, byte[]> files = new HashMap<>();
        files.put("shot.png", png(60, 40, RED));
        MarkdownView view = viewWithImages(files, 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("An introduction.\n\n![A shot](shot.png)");
        layOut(view);
        work.runAll();
        relayOut(view);
        files.remove("shot.png");

        view.updateMarkdown("A new introduction.\n\n![A shot](shot.png)");
        relayOut(view);
        assertEquals(60, image(view).getLayoutBounds().getWidth(), "the old picture");

        work.runAll();
        relayOut(view);
        assertTrue(images(view).isEmpty(), "no picture");
        assertNotNull(view.lookup(".markdown-image-error"), "the error box");
    }

    @Test
    @DisplayName("a playing animated GIF keeps playing at its size while the update loads it again")
    void playingGifKeepsPlaying() {
        MarkdownView view = viewWithImages(Map.of("recording.gif", testResource("images/animated.gif")), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("An introduction.\n\n![A recording](recording.gif)");
        layOut(view);
        work.runAll();
        relayOut(view);
        Image playing = image(view).getImage();
        assertFalse(playing instanceof WritableImage, "the decoded GIF itself, which plays");
        double width = sceneBounds(image(view)).getWidth();

        view.updateMarkdown("A new introduction.\n\n![A recording](recording.gif)");
        relayOut(view);

        assertSame(playing, image(view).getImage(), "the same decoded GIF, still playing");
        assertEquals(width, sceneBounds(image(view)).getWidth(), 1e-6, "at its size");
    }

    @Test
    @DisplayName("an image whose old twin showed its placeholder and one of a new address start with their"
            + " placeholders")
    void placeholdersStayPlaceholders() {
        Map<String, byte[]> files = new HashMap<>();
        files.put("a.png", png(60, 40, RED));
        files.put("b.png", png(60, 40, BLUE));
        files.put("c.png", png(60, 40, BLUE));
        MarkdownView view = viewWithImages(files, 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("Images.\n\n![A](a.png)\n\n![B](b.png)");
        layOut(view);
        work.runLoads();
        assertTrue(work.runNextDecode(), "the first image's decode");
        relayOut(view);
        assertEquals(1, images(view).size(), "the first image shows, the second its placeholder");

        view.updateMarkdown("New images.\n\n![A](a.png)\n\n![B](b.png)\n\n![C](c.png)");
        relayOut(view);

        assertEquals(1, images(view).size(), "the first image keeps its picture");
        assertEquals(2, view.lookupAll(".markdown-image-placeholder").size(), "the second and the new one");
    }

    @Test
    @DisplayName("setMarkdown shows a new document's images with their placeholders, also those of an address shown")
    void setMarkdownShowsPlaceholders() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(60, 40, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("An introduction.\n\n![A shot](shot.png)");
        layOut(view);
        work.runAll();
        relayOut(view);

        view.setMarkdown("A new introduction.\n\n![A shot](shot.png)");
        relayOut(view);

        assertTrue(images(view).isEmpty(), "no picture");
        assertNotNull(view.lookup(".markdown-image-placeholder"), "the placeholder");
    }

    @Test
    @DisplayName("an image above the line at the top edge keeps its height through the update, and its changed file"
            + " arriving keeps the line at the top")
    void changedImageAboveThePlace() {
        Map<String, byte[]> files = new HashMap<>();
        files.put("shot.png", png(200, 150, RED));
        MarkdownView view = viewWithImages(files, 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        String text = "An introduction.\n\n![A shot](shot.png)\n\n" + SECTIONS;
        view.setMarkdown(text);
        layOut(view);
        work.runAll();
        relayOut(view);
        TopLine before = TopLine.cut(view, paragraphs(view).get(13), 200, 0.4);
        files.put("shot.png", png(200, 200, BLUE));

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + text);
        relayOut(view);
        assertEquals(150, image(view).getLayoutBounds().getHeight(), "the old picture at its height");
        before.assertAtTheTop(view, paragraphs(view).get(14));

        work.runAll();
        relayOut(view);
        assertEquals(200, image(view).getLayoutBounds().getHeight(), "the new picture");
        before.assertAtTheTop(view, paragraphs(view).get(14));
    }

    @Test
    @DisplayName("of several images of one address, the first that showed its picture hands it to every image of that"
            + " address")
    void severalImagesOfOneAddress() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(60, 40, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        String text = "![A](a.png)\n\n" + longParagraphs(12) + "![A again](a.png)\n\n" + longParagraphs(12);
        view.setMarkdown(text);
        layOut(view);
        work.runLoads();
        scrollToTop(view, sceneBounds(imageBlocks(view).get(1)), 0);
        assertTrue(work.runNextDecode(), "the decode of the image in the visible area");
        relayOut(view);
        assertNotNull(imageBlocks(view).get(0).lookup(".markdown-image-placeholder"), "the first still waits");
        assertNotNull(imageBlocks(view).get(1).lookup(".markdown-image"), "the second shows its picture");

        view.updateMarkdown("A paragraph inserted at the top.\n\n" + text + "![A third time](a.png)");
        relayOut(view);

        assertEquals(3, images(view).size(), "every image of the address starts with the picture");
        assertNull(view.lookup(".markdown-image-placeholder"), "no placeholder");
    }

    @Test
    @DisplayName("a text of block images alone shows the update from the top, its images keeping what they showed")
    void blockImagesAlone() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(60, 40, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("![A](a.png)");
        layOut(view);
        work.runAll();
        relayOut(view);

        view.updateMarkdown("![A](a.png)\n\n![B](b.png)");
        relayOut(view);

        assertEquals(1, images(view).size(), "the first keeps its picture");
        assertNotNull(imageBlocks(view).get(1).lookup(".markdown-image-placeholder"), "the new one waits");
        assertEquals(scrollPane(view).getVmin(), scrollPane(view).getVvalue(), "at the top");
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
     * The bytes of a file of the test resources, beside this class.
     */
    private static byte[] testResource(String name) {
        try (InputStream in = MarkdownViewUpdateTest.class.getResourceAsStream(name)) {
            assertNotNull(in, "the test resource " + name);
            return in.readAllBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
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
