package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.document;
import static org.karthu.fx.markdown.MarkdownTestSupport.image;
import static org.karthu.fx.markdown.MarkdownTestSupport.imageBlocks;
import static org.karthu.fx.markdown.MarkdownTestSupport.images;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollToTop;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;
import static org.karthu.fx.markdown.TestImages.BLUE;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * How the Markdown view's images arrive, seen from outside: the order in which they are decoded, the reader's place
 * while they arrive or a reload changes them, and the size limit.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown. Its images' work only queues
 * ({@link QueuedWork}): the test runs the loads, then the decodes one at a time, and sees which image arrives
 * after each.
 */
class MarkdownViewImageArrivalTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("the image in the visible area is decoded first, then the rest in document order")
    void visibleImageIsDecodedFirst() {
        MarkdownView view = viewWithImages(numberedImages(5), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown(imagesFarApart(5));
        layOut(view);
        work.runLoads();
        scrollToTop(view, sceneBounds(imageBlocks(view).get(3)), 0);

        assertEquals(List.of("image 4", "image 1", "image 2", "image 3", "image 5"), decodeOrder(view, work));
    }

    @Test
    @DisplayName("a scroll reorders the decodes still queued")
    void scrollReordersTheQueue() {
        MarkdownView view = viewWithImages(numberedImages(5), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown(imagesFarApart(5));
        layOut(view);
        work.runLoads();
        scrollToTop(view, sceneBounds(imageBlocks(view).get(3)), 0);
        work.runNextDecode();
        relayOut(view);
        scrollToTop(view, sceneBounds(imageBlocks(view).get(1)), 0);

        assertEquals(List.of("image 2", "image 1", "image 3", "image 5"), decodeOrder(view, work));
    }

    @Test
    @DisplayName("a new text cancels the decodes the old text has queued")
    void newTextCancelsQueuedDecodes() {
        MarkdownView view = viewWithImages(numberedImages(2), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown(imagesFarApart(2));
        layOut(view);
        work.runLoads();
        view.setMarkdown("No image.");

        assertFalse(work.runNextDecode(), "no decode left");
    }

    @Test
    @DisplayName("an image that arrives above the line at the top edge keeps that line in place")
    void imageArrivingAboveKeepsTheTopLine() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 150, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("An introduction.\n\n![A shot](shot.png)\n\n" + longParagraphs(6));
        layOut(view);
        TextFlow read = paragraphs(view).get(3);
        scrollToTop(view, sceneBounds(read), 0.5);
        double lineBefore = sceneBounds(read).getMinY();
        assertTrue(sceneBounds(imageBlocks(view).get(0)).getMaxY() < viewportBounds(view).getMinY(),
                "the image is above the visible area");

        work.runAll();
        relayOut(view);

        assertEquals(150, image(view).getLayoutBounds().getHeight(), "the image has arrived");
        assertEquals(lineBefore, sceneBounds(read).getMinY(), 1, "the text at the top edge stays in place");
    }

    @Test
    @DisplayName("an image that arrives in the visible area of a view at the top pushes the text below it down")
    void imageArrivingInTheVisibleAreaPushesTheTextDown() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 150, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("![A shot](shot.png)\n\n" + longParagraphs(6));
        layOut(view);
        TextFlow below = paragraphs(view).get(0);
        double before = sceneBounds(below).getMinY();

        work.runAll();
        relayOut(view);

        assertEquals(0, scrollPane(view).getVvalue(), "still at the top");
        assertEquals(viewportBounds(view).getMinY(), sceneBounds(document(view)).getMinY(), 0.5,
                "the document's top at the top edge");
        assertTrue(sceneBounds(below).getMinY() > before + 100, "the text pushed down below the image");
    }

    @ParameterizedTest(name = "limit {0}")
    @CsvSource({"0, true", "-1, true", "5001, true", "5000, true", "4999, false"})
    @DisplayName("an image of more pixels than the limit shows the too-large box; at or below it, or without one, it shows")
    void pixelLimit(int limit, boolean shown) {
        MarkdownView view = viewWithImages(Map.of("a.png", png(100, 50, RED)), 1.0);
        view.setImagePixelLimit(limit);
        view.setMarkdown("![A shot](a.png)");
        layOut(view);

        if (shown) {
            assertEquals(100, image(view).getImage().getWidth(), "the image is shown");
            assertNull(view.lookup(".markdown-image-error"), "no error box");
        } else {
            assertTrue(images(view).isEmpty(), "no image");
            Node box = view.lookup(".markdown-image-error");
            assertNotNull(box, "the error box");
            assertEquals(BootstrapIcons.FILE_EARMARK_X, ((FontIcon) box.lookup(".ikonli-font-icon")).getIconCode(),
                    "the missing-image icon");
            assertEquals(List.of("A shot", "Image too large"), labelTexts(box), "the alt text and the note");
            assertEquals("a.png", ((Tooltip) box.getProperties().get("javafx.scene.control.Tooltip")).getText(),
                    "the address as tooltip");
        }
    }

    @Test
    @DisplayName("there is no size limit by default, and the too-large note is a text property that applies at once")
    void pixelLimitDefaultsAndText() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(100, 50, RED)), 1.0);
        assertEquals(0, view.getImagePixelLimit(), "no limit");
        assertEquals("Image too large", view.getImageTooLargeText());
        view.setImagePixelLimit(100);
        view.setMarkdown("![A shot](a.png)");
        layOut(view);
        view.setImageTooLargeText("Bild zu gross");

        assertEquals(List.of("A shot", "Bild zu gross"), labelTexts(view.lookup(".markdown-image-error")));
        assertEquals(100, view.imagePixelLimitProperty().get());
        assertEquals("Bild zu gross", view.imageTooLargeTextProperty().get());
    }

    @Test
    @DisplayName("an image above the line at the top edge that a reload changes in size keeps that line in place")
    void reloadAboveKeepsTheTopLine() {
        Map<String, byte[]> files = new ConcurrentHashMap<>(Map.of("shot.png", png(200, 50, RED)));
        MarkdownView view = viewWithImages(files, 1.0);
        view.setMarkdown("An introduction.\n\n![A shot](shot.png)\n\n" + longParagraphs(6));
        layOut(view);
        TextFlow read = paragraphs(view).get(3);
        scrollToTop(view, sceneBounds(read), 0.5);
        double lineBefore = sceneBounds(read).getMinY();
        Node block = imageBlocks(view).get(0);
        assertTrue(sceneBounds(block).getMaxY() < viewportBounds(view).getMinY(), "the image is above the visible area");

        files.put("shot.png", png(200, 180, BLUE));
        view.setImageLoader(TestImages.serving(files));
        relayOut(view);

        assertSame(block, imageBlocks(view).get(0), "reloaded in place");
        assertEquals(180, image(view).getLayoutBounds().getHeight(), "the reloaded image, taller");
        assertEquals(lineBefore, sceneBounds(read).getMinY(), 1, "the text at the top edge stays in place");
    }

    @Test
    @DisplayName("a change of the limit loads the shown images anew, in place, keeping the line at the top edge")
    void limitChangeReloadsInPlace() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 150, RED)), 1.0);
        view.setMarkdown("An introduction.\n\n![A shot](shot.png)\n\n" + longParagraphs(6));
        layOut(view);
        TextFlow read = paragraphs(view).get(3);
        scrollToTop(view, sceneBounds(read), 0.5);
        double lineBefore = sceneBounds(read).getMinY();
        Node block = imageBlocks(view).get(0);

        view.setImagePixelLimit(100 * 100);
        relayOut(view);

        assertSame(block, imageBlocks(view).get(0), "the same block");
        assertTrue(images(view).isEmpty(), "no image above the limit");
        assertEquals(List.of("A shot", "Image too large"), labelTexts(block.lookup(".markdown-image-error")));
        assertEquals(lineBefore, sceneBounds(read).getMinY(), 1, "the text at the top edge stays in place");

        view.setImagePixelLimit(0);
        relayOut(view);

        assertEquals(150, image(view).getLayoutBounds().getHeight(), "the image again, without a limit");
        assertEquals(lineBefore, sceneBounds(read).getMinY(), 1, "the text at the top edge stays in place");
    }

    @Test
    @DisplayName("a decode of a load that a reload has replaced shows nothing; the reload's image arrives")
    void decodeOfAReplacedLoadShowsNothing() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(30, 20, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("![A shot](a.png)");
        layOut(view);
        work.runLoads();

        view.setImageLoader(TestImages.serving(Map.of("a.png", png(50, 10, BLUE))));
        assertTrue(work.runNextDecode(), "the first load's decode was queued");
        relayOut(view);
        assertTrue(images(view).isEmpty(), "its result is dropped");

        work.runAll();
        relayOut(view);
        assertEquals(50, image(view).getLayoutBounds().getWidth(), "the reload's image");
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * The texts of the labels under a node, in order.
     */
    private static List<String> labelTexts(Node root) {
        List<String> texts = new ArrayList<>();
        for (Node node : root.lookupAll(".label")) {
            texts.add(((Label) node).getText());
        }
        return texts;
    }

    /**
     * Small image files {@code 1.png} to {@code n.png}.
     */
    private static Map<String, byte[]> numberedImages(int count) {
        Map<String, byte[]> files = new HashMap<>();
        for (int i = 1; i <= count; i++) {
            files.put(i + ".png", png(20, 10, RED));
        }
        return files;
    }

    /**
     * Images {@code image 1} to {@code image n}, each followed by text several times the height of the test's view,
     * so that only one of them is in or near the visible area at a time.
     */
    private static String imagesFarApart(int count) {
        StringBuilder markdown = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            markdown.append("![image ").append(i).append("](").append(i).append(".png)\n\n").append(longParagraphs(5));
        }
        return markdown.toString();
    }

    /**
     * Runs the queued decodes one at a time and notes which image arrives after each, by its alt text; the images
     * shown before are left out.
     */
    private static List<String> decodeOrder(MarkdownView view, QueuedWork work) {
        List<String> shownBefore = altTexts(view);
        List<String> order = new ArrayList<>();
        while (work.runNextDecode()) {
            relayOut(view);
            for (String alt : altTexts(view)) {
                if (!shownBefore.contains(alt) && !order.contains(alt)) {
                    order.add(alt);
                }
            }
        }
        return order;
    }

    /**
     * The alt texts of the images the view shows, in document order.
     */
    private static List<String> altTexts(MarkdownView view) {
        List<String> alts = new ArrayList<>();
        for (ImageView image : images(view)) {
            alts.add(image.getAccessibleText());
        }
        return alts;
    }
}
