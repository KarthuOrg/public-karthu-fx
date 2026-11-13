package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.imageBlocks;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollToTop;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;
import static org.karthu.fx.markdown.TestImages.BLUE;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.colors;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * The Markdown view's inline images seen from outside: an image inside a sentence sits in its line, one node of the
 * text flow, with the size rule, the scaling, the placeholder and the error boxes of a block image.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown, its images' work runs at once on the test's thread,
 * and it assumes an output scale of the test's choosing.
 */
class MarkdownViewInlineImagesTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("an image in a sentence is one node of the paragraph's text flow, between its runs, at its natural size")
    void imageInASentence() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(16, 12, (x, y) -> x < 8 ? RED : BLUE)), 1.0);
        view.setMarkdown("See ![the icon](icon.png) here.");
        layOut(view);

        assertTrue(imageBlocks(view).isEmpty(), "no image block");
        TextFlow paragraph = paragraphs(view).get(0);
        List<Node> children = managedChildren(paragraph);
        assertEquals(3, children.size(), () -> "a run, the image, a run: " + children);
        assertEquals("See ", ((Text) children.get(0)).getText());
        assertEquals(" here.", ((Text) children.get(2)).getText());
        Node inline = children.get(1);
        assertTrue(inline.getStyleClass().contains("markdown-image-inline"), () -> "the inline image: " + inline);
        ImageView image = (ImageView) inline.lookup(".markdown-image");
        assertNotNull(image, "its picture");
        assertEquals(16, image.getLayoutBounds().getWidth(), "width");
        assertEquals(12, image.getLayoutBounds().getHeight(), "height");
        assertEquals(List.of(RED, BLUE), colors(image.getImage(), 2, 6, 13, 6), "the file's pixels");
    }

    @Test
    @DisplayName("an inline image sits on the baseline of its line, a tall one too, which makes its line taller")
    void imageSitsOnTheBaseline() {
        MarkdownView view = viewWithImages(Map.of("small.png", png(16, 12, RED), "tall.png", png(20, 40, BLUE)), 1.0);
        view.setMarkdown("A ![small](small.png) b\n\nC ![tall](tall.png) d");
        layOut(view);

        for (TextFlow paragraph : paragraphs(view)) {
            Text before = (Text) paragraph.getChildren().get(0);
            Node inline = paragraph.getChildren().get(1);
            double baseline = sceneBounds(before).getMinY() + before.getBaselineOffset();
            Bounds image = sceneBounds(inline.lookup(".markdown-image"));
            assertEquals(baseline, image.getMaxY(), 0.5, () -> "the image's bottom on the baseline: " + inline);
            assertTrue(image.getMinY() >= sceneBounds(paragraph).getMinY() - 1e-9, "inside its paragraph");
        }
        TextFlow tall = paragraphs(view).get(1);
        assertTrue(sceneBounds(tall).getHeight() >= 40, "the tall image's line is as tall as the image");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("a wide inline image shrinks to the width the text wraps at, keeping its aspect ratio, computed at"
            + " that size")
    @CsvSource(delimiter = '|', value = {
        "a paragraph | Wide ![w](wide.png) image.",
        "a list item | - Wide ![w](wide.png) image.",
        "a quote | > Wide ![w](wide.png) image.",
        "a heading | ## Wide ![w](wide.png) image"})
    void wideImageShrinksToTheTextWidth(String description, String markdown) {
        MarkdownView view = viewWithImages(Map.of("wide.png", png(1600, 400, RED)), 1.0);
        view.setMarkdown(markdown);
        layOut(view);

        Region flow = (Region) inline(view).getParent();
        double textWidth = flow.getWidth() - flow.snappedLeftInset() - flow.snappedRightInset();
        assertTrue(textWidth < 1600, "the test's text is narrower than the image");
        ImageView image = (ImageView) inline(view).lookup(".markdown-image");
        assertEquals(textWidth, image.getLayoutBounds().getWidth(), 1e-9, "width");
        // Whole screen pixels, so each pixel of the shrunk bitmap covers one: the aspect ratio kept to the pixel.
        double height = image.getLayoutBounds().getHeight();
        assertEquals(textWidth / 4, height, 0.5, "height");
        assertEquals(Math.rint(height), height, 1e-9, "height in whole screen pixels");
        assertEquals(Math.round(textWidth), image.getImage().getWidth(), "computed at the width it shows at");
    }

    @Test
    @DisplayName("a tall inline image shrinks to the visible height")
    void tallImageShrinksToTheVisibleHeight() {
        MarkdownView view = viewWithImages(Map.of("tall.png", png(100, 2000, RED)), 1.0);
        view.setMarkdown("Tall ![t](tall.png) image.");
        layOut(view);

        double visible = viewportBounds(view).getHeight();
        Bounds image = inline(view).lookup(".markdown-image").getLayoutBounds();
        assertEquals(visible, image.getHeight(), 1e-9, "height");
        // Whole screen pixels, so each pixel of the shrunk bitmap covers one: the aspect ratio kept to the pixel.
        assertEquals(visible / 20, image.getWidth(), 0.5, "width");
        assertEquals(Math.rint(image.getWidth()), image.getWidth(), 1e-4, "width in whole screen pixels");
    }

    @Test
    @DisplayName("a narrower view shrinks a wide inline image to the new text width and computes it anew there")
    void resizeShrinksTheInlineImage() {
        MarkdownView view = viewWithImages(Map.of("wide.png", png(1600, 400, RED)), 1.0);
        view.setMarkdown("Wide ![w](wide.png) image.");
        layOut(view);
        double wide = inline(view).lookup(".markdown-image").getLayoutBounds().getWidth();

        view.setMaxWidth(250);
        relayOut(view);

        TextFlow paragraph = paragraphs(view).get(0);
        ImageView image = (ImageView) inline(view).lookup(".markdown-image");
        double narrow = image.getLayoutBounds().getWidth();
        assertTrue(narrow < wide - 100, () -> "narrower: " + narrow + " after " + wide);
        assertEquals(paragraph.getWidth(), narrow, 1e-9, "as wide as the text");
        assertEquals(paragraph.getHeight(), sceneBounds(paragraph).getHeight(), 1e-9);
        assertTrue(paragraph.getHeight() >= narrow / 4, "the paragraph holds the image's new height");
        assertEquals(Math.round(narrow), image.getImage().getWidth(), "computed anew at the new width");
    }

    @ParameterizedTest(name = "output scale {0}")
    @DisplayName("an inline image's natural size counts one image pixel as one screen pixel")
    @CsvSource({"1.0, 30, 15", "1.5, 20, 10", "2.0, 15, 7.5"})
    void naturalSizeAtOutputScale(double scale, double width, double height) {
        MarkdownView view = viewWithImages(Map.of("a.png", png(30, 15, RED)), scale);
        view.setMarkdown("An ![a](a.png) icon.");
        layOut(view);

        Bounds image = sceneBounds(inline(view).lookup(".markdown-image"));
        assertEquals(width, image.getWidth(), 1e-9, "width");
        assertEquals(height, image.getHeight(), 1e-9, "height");
    }

    @Test
    @DisplayName("until the image is decoded, an inline image shows a small placeholder in the line: an image icon")
    void placeholderUntilDecoded() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(16, 12, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("Plain line.\n\nSee ![the icon](icon.png) here.");
        layOut(view);

        Label placeholder = (Label) inline(view).lookup(".markdown-image-placeholder");
        assertNotNull(placeholder, "a placeholder");
        assertEquals(BootstrapIcons.IMAGE, ((FontIcon) placeholder.getGraphic()).getIconCode(), "an image icon");
        assertTrue(placeholder.getText() == null || placeholder.getText().isEmpty(), "no text");
        assertEquals("the icon", placeholder.getAccessibleText(), "the alt text as its accessible text");
        assertLineHigh(view);

        work.runAll();
        relayOut(view);
        assertNull(inline(view).lookup(".markdown-image-placeholder"), "the placeholder is gone");
        assertEquals(16, inline(view).lookup(".markdown-image").getLayoutBounds().getWidth(), "the image shows");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("an inline image that cannot be shown leaves a small box in the line: an icon, the alt text, the address"
            + " as its tooltip")
    @CsvSource(delimiter = '|', value = {
        "a missing file | missing.png",
        "an SVG file | drawing.svg"})
    void errorBox(String description, String address) {
        MarkdownView view = viewWithImages(Map.of("drawing.svg", TestImages.svg()), 1.0);
        view.setMarkdown("Plain line.\n\nSee ![the icon](" + address + ") here.");
        layOut(view);

        Label box = (Label) inline(view).lookup(".markdown-image-error");
        assertNotNull(box, "the error box");
        assertEquals(BootstrapIcons.FILE_EARMARK_X, ((FontIcon) box.getGraphic()).getIconCode(), "missing-image icon");
        assertEquals("the icon", box.getText(), "the alt text");
        assertEquals(address, tooltip(box).getText(), "the address as tooltip");
        assertNull(inline(view).lookup(".markdown-image"), "no image");
        assertLineHigh(view);
    }

    @Test
    @DisplayName("an inline image that arrives above the line at the top edge keeps that line in place")
    void imageArrivingAboveKeepsTheTopLine() {
        MarkdownView view = viewWithImages(Map.of("tall.png", png(40, 150, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("An introduction with ![a tall icon](tall.png) inside.\n\n" + longParagraphs(6));
        layOut(view);
        TextFlow read = paragraphs(view).get(2);
        scrollToTop(view, sceneBounds(read), 0.5);
        double lineBefore = sceneBounds(read).getMinY();
        assertTrue(sceneBounds(inline(view)).getMaxY() < viewportBounds(view).getMinY(), "the image is above");

        work.runAll();
        relayOut(view);

        assertEquals(150, inline(view).lookup(".markdown-image").getLayoutBounds().getHeight(), "it has arrived");
        assertEquals(lineBefore, sceneBounds(read).getMinY(), 1, "the text at the top edge stays in place");
    }

    @Test
    @DisplayName("a change of the loader loads an inline image anew in place, in the same paragraph")
    void reloadInPlace() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(16, 12, RED)), 1.0);
        view.setMarkdown("See ![the icon](icon.png) here.");
        layOut(view);
        TextFlow paragraph = paragraphs(view).get(0);
        Node inline = inline(view);

        view.setImageLoader(TestImages.serving(Map.of("icon.png", png(24, 20, BLUE))));
        relayOut(view);

        assertSame(paragraph, paragraphs(view).get(0), "the same paragraph");
        assertSame(inline, inline(view), "the same inline image");
        ImageView image = (ImageView) inline.lookup(".markdown-image");
        assertEquals(24, image.getLayoutBounds().getWidth(), "the new file's width");
        assertEquals(List.of(BLUE), colors(image.getImage(), 12, 10), "the new file's pixels");
    }

    @Test
    @DisplayName("a 4K screenshot inside a sentence is kept as a bitmap of the size it shows at, at the output scale")
    void bitmapHasTheSizeInScreenPixels() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(3840, 2160, RED)), 1.5);
        view.setMarkdown("A ![screenshot](shot.png) in a sentence.");
        layOut(view);

        ImageView image = (ImageView) inline(view).lookup(".markdown-image");
        Bounds onScreen = sceneBounds(image);
        assertTrue(image.getImage().getWidth() < 3840, "shrunk, not the decoded image");
        assertEquals(Math.round(onScreen.getWidth() * 1.5), image.getImage().getWidth(), "pixels wide on screen");
        assertEquals(Math.round(onScreen.getHeight() * 1.5), image.getImage().getHeight(), "pixels high on screen");
    }

    @Test
    @DisplayName("inline and block images share one decode queue: the visible one first, then document order")
    void decodeOrder() {
        Map<String, byte[]> files = Map.of("1.png", png(20, 10, RED), "2.png", png(20, 10, RED),
                "3.png", png(20, 10, RED));
        MarkdownView view = viewWithImages(files, 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("Image ![one](1.png) inline.\n\n" + longParagraphs(5) + "![two](2.png)\n\n" + longParagraphs(5)
                + "Image ![three](3.png) inline.");
        layOut(view);
        TextFlow last = paragraphs(view).getLast();
        scrollToTop(view, sceneBounds(last), 0);
        work.runLoads();

        List<String> order = new ArrayList<>();
        while (work.runNextDecode()) {
            for (Node image : view.lookupAll(".markdown-image")) {
                String alt = image.getAccessibleText();
                if (!order.contains(alt)) {
                    order.add(alt);
                }
            }
        }
        assertEquals(List.of("three", "one", "two"), order);
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * Asserts that the view's second paragraph, which holds an inline box, is one line high: no higher than its first,
     * a plain line, by more than two pixels.
     */
    private static void assertLineHigh(MarkdownView view) {
        double plain = paragraphs(view).get(0).getHeight();
        double withBox = paragraphs(view).get(1).getHeight();
        assertTrue(withBox <= plain + 2, () -> "one line high: " + withBox + ", a plain line " + plain);
    }

    /**
     * The tooltip installed on a node: {@code Tooltip.install} keeps it in the node's properties under this key, the
     * one place JavaFX keeps it.
     */
    private static Tooltip tooltip(Node node) {
        return (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
    }

    /**
     * The view's one inline image, {@code markdown-image-inline}.
     */
    private static Node inline(MarkdownView view) {
        List<Node> inlines = new ArrayList<>(view.lookupAll(".markdown-image-inline"));
        assertEquals(1, inlines.size(), "one inline image");
        return inlines.get(0);
    }

    /**
     * The children a text flow lays out as its text: runs and embedded nodes, not the selection's layer.
     */
    private static List<Node> managedChildren(TextFlow flow) {
        List<Node> children = new ArrayList<>();
        for (Node child : flow.getChildren()) {
            if (child.isManaged()) {
                children.add(child);
            }
        }
        return children;
    }
}
