package org.karthu.fx.markdown;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.event.Level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.image;
import static org.karthu.fx.markdown.MarkdownTestSupport.images;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;
import static org.karthu.fx.markdown.TestImages.BLUE;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.colors;
import static org.karthu.fx.markdown.TestImages.png;
import static org.karthu.fx.markdown.TestImages.serving;

/**
 * The Markdown view's images seen from outside: Markdown text in, image files handed to the view by a loader of the
 * test, the shown images found by their documented style classes, their sizes and their pixels.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown. Two of the view's package-private switches stand in
 * for what a test without a window lacks: the images' work runs at once on the test's thread, and the view assumes an
 * output scale of the test's choosing.
 */
class MarkdownViewImagesTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("an image alone in its paragraph shows as a block: the loader's picture at its natural size")
    void loneImageIsABlock() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(120, 80, (x, y) -> x < 60 ? RED : BLUE)), 1.0);
        view.setMarkdown("Before.\n\n![A shot](shot.png)\n\nAfter.");
        layOut(view);

        Node block = view.lookup(".markdown-image-block");
        assertNotNull(block, "an image block");
        ImageView image = (ImageView) block.lookup(".markdown-image");
        assertEquals(120, image.getLayoutBounds().getWidth(), "width");
        assertEquals(80, image.getLayoutBounds().getHeight(), "height");
        assertEquals(List.of(RED, BLUE), colors(image.getImage(), 10, 40, 110, 40), "the file's pixels");
        assertEquals(List.of("Before.", "After."), texts(paragraphs(view)), "the image is no paragraph");
    }

    @ParameterizedTest(name = "output scale {0}")
    @DisplayName("an image's natural size counts one image pixel as one screen pixel")
    @CsvSource({"1.0, 300, 150", "1.5, 200, 100", "2.0, 150, 75"})
    void naturalSizeAtOutputScale(double scale, double width, double height) {
        MarkdownView view = viewWithImages(Map.of("a.png", png(300, 150, RED)), scale);
        view.setMarkdown("![a](a.png)");
        layOut(view);

        Bounds image = sceneBounds(image(view));
        assertEquals(width, image.getWidth(), 1e-9, "width");
        assertEquals(height, image.getHeight(), 1e-9, "height");
    }

    @Test
    @DisplayName("a wide image shrinks to the width the text wraps at, keeping its aspect ratio")
    void wideImageShrinksToTheTextWidth() {
        MarkdownView view = viewWithImages(Map.of("wide.png", png(1600, 400, RED)), 1.0);
        view.setMarkdown("Some text.\n\n![wide](wide.png)");
        layOut(view);

        double textWidth = paragraphs(view).get(0).getLayoutBounds().getWidth();
        Bounds image = image(view).getLayoutBounds();
        assertTrue(textWidth < 1600, "the test's text is narrower than the image");
        assertEquals(textWidth, image.getWidth(), 1e-9, "width");
        // Whole screen pixels, so each pixel of the shrunk bitmap covers one: the aspect ratio kept to the pixel.
        assertEquals(textWidth / 4, image.getHeight(), 0.5, "height");
        assertEquals(Math.rint(image.getHeight()), image.getHeight(), 1e-9, "height in whole screen pixels");
    }

    @Test
    @DisplayName("a wide image in a list item shrinks to the item's text width")
    void wideImageInAListItem() {
        MarkdownView view = viewWithImages(Map.of("wide.png", png(1600, 400, RED)), 1.0);
        view.setMarkdown("- Some text.\n\n  ![wide](wide.png)");
        layOut(view);

        double textWidth = paragraphs(view).get(0).getLayoutBounds().getWidth();
        assertEquals(textWidth, image(view).getLayoutBounds().getWidth(), 1e-9);
    }

    @Test
    @DisplayName("a tall image shrinks to the visible height, keeping its aspect ratio")
    void tallImageShrinksToTheVisibleHeight() {
        MarkdownView view = viewWithImages(Map.of("tall.png", png(100, 2000, RED)), 1.0);
        view.setMarkdown("![tall](tall.png)");
        layOut(view);

        double visible = viewportBounds(view).getHeight();
        Bounds image = image(view).getLayoutBounds();
        assertTrue(visible < 2000, "the test's view is lower than the image");
        assertEquals(visible, image.getHeight(), 1e-9, "height");
        // Whole screen pixels, so each pixel of the shrunk bitmap covers one: the aspect ratio kept to the pixel.
        assertEquals(visible / 20, image.getWidth(), 0.5, "width");
        // The node's bounds are floats.
        assertEquals(Math.rint(image.getWidth()), image.getWidth(), 1e-4, "width in whole screen pixels");
    }

    @Test
    @DisplayName("a small image is never enlarged")
    void smallImageIsNeverEnlarged() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(16, 12, RED)), 1.0);
        view.setMarkdown("Some text.\n\n![icon](icon.png)");
        layOut(view);

        Bounds image = image(view).getLayoutBounds();
        assertEquals(16, image.getWidth(), "width");
        assertEquals(12, image.getHeight(), "height");
    }

    @Test
    @DisplayName("the zoom scales an image like text, and at 200 % it still fits the width and the visible height")
    void zoomScalesImages() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(40, 30, RED), "wide.png", png(1600, 400, BLUE),
                "tall.png", png(100, 2000, RED)), 1.0);
        view.setMarkdown("Some text.\n\n![icon](icon.png)\n\n![wide](wide.png)\n\n![tall](tall.png)");
        layOut(view);
        view.setZoom(2.0);
        relayOut(view);

        List<ImageView> images = images(view);
        Bounds icon = sceneBounds(images.get(0));
        assertEquals(80, icon.getWidth(), 1e-9, "the small image, twice as wide");
        assertEquals(60, icon.getHeight(), 1e-9, "the small image, twice as high");
        Bounds text = sceneBounds(paragraphs(view).get(0));
        assertEquals(text.getWidth(), sceneBounds(images.get(1)).getWidth(), 1e-6, "the wide image, the text's width");
        assertEquals(viewportBounds(view).getHeight(), sceneBounds(images.get(2)).getHeight(), 1e-6,
                "the tall image, the visible height");
    }

    @Test
    @DisplayName("a change of the output scale computes the images' size anew")
    void outputScaleChange() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(300, 150, RED)), 1.0);
        view.setMarkdown("![a](a.png)");
        layOut(view);
        view.setOutputScale(2.0);
        relayOut(view);

        Bounds image = image(view).getLayoutBounds();
        assertEquals(150, image.getWidth(), 1e-9, "width");
        assertEquals(75, image.getHeight(), 1e-9, "height");
    }

    @Test
    @DisplayName("until the image is decoded, its block shows a placeholder one line high: an image icon and the alt text")
    void placeholderUntilDecoded() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(120, 80, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("One line.\n\n![A shot](shot.png)");
        layOut(view);

        Node block = view.lookup(".markdown-image-block");
        Label placeholder = (Label) block.lookup(".markdown-image-placeholder");
        assertNotNull(placeholder, "a placeholder");
        assertEquals("A shot", placeholder.getText(), "the alt text");
        assertEquals(BootstrapIcons.IMAGE, ((FontIcon) placeholder.getGraphic()).getIconCode(), "an image icon");
        double line = paragraphs(view).get(0).getLayoutBounds().getHeight();
        assertTrue(placeholder.getLayoutBounds().getHeight() < 2 * line, "one line high");
        assertTrue(images(view).isEmpty(), "no image yet");

        work.runAll();
        relayOut(view);

        assertNull(block.lookup(".markdown-image-placeholder"), "the placeholder is gone");
        assertEquals(120, image(view).getLayoutBounds().getWidth(), "the image is shown");
    }

    /**
     * Loaders that have no image for an address, each in its own way.
     */
    static Stream<Arguments> missingImages() {
        ImageLoader throwing = address -> {
            throw new IOException("no such file");
        };
        return Stream.of(
                Arguments.of("a loader that throws an IOException", throwing),
                Arguments.of("a loader that returns null", (ImageLoader) address -> null),
                Arguments.of("a loader that returns no bytes", (ImageLoader) address -> new byte[0]),
                Arguments.of("no loader", null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("missingImages")
    @DisplayName("an image the loader has no file for shows the missing box: an icon, the alt text, the note, the address")
    void missingImage(String description, ImageLoader loader) {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        view.setImageLoader(loader);
        view.setMarkdown("![A shot](shots/a.png)");
        layOut(view);

        assertErrorBox(view, "A shot", "Image not found", "shots/a.png");
    }

    @Test
    @DisplayName("with the view's own loader, an image whose file does not exist shows the missing box")
    void ownLoaderWithoutTheFile() {
        MarkdownView view = new MarkdownView();
        view.setImageWork(new ImmediateWork());
        view.setMarkdown("![A shot](file:/shots/a.png)");
        layOut(view);

        assertErrorBox(view, "A shot", "Image not found", "file:/shots/a.png");
    }

    @Test
    @DisplayName("a loader that throws a runtime exception gives the missing box and a warning with the address")
    void throwingLoader() {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        RecordingLogger log = new RecordingLogger();
        view.setLogger(log);
        IllegalStateException bug = new IllegalStateException("a bug in the loader");
        view.setImageLoader(address -> {
            throw bug;
        });
        view.setMarkdown("Before.\n\n![A shot](shots/a.png)\n\nAfter.");
        layOut(view);

        assertErrorBox(view, "A shot", "Image not found", "shots/a.png");
        assertEquals(List.of("Before.", "After."), texts(paragraphs(view)), "the rest of the document renders");
        assertEquals(1, log.records.size(), () -> "one record: " + log.records);
        assertEquals(Level.WARN, log.records.get(0).level());
        assertTrue(log.records.get(0).message().contains("'shots/a.png'"), () -> "the address: " + log.records);
        assertSame(bug, log.records.get(0).thrown(), "with the loader's exception");
    }

    /**
     * Bytes JavaFX cannot decode.
     */
    static Stream<Arguments> undecodableImages() {
        byte[] png = png(20, 10, RED);
        return Stream.of(
                Arguments.of("an SVG file", TestImages.svg()),
                Arguments.of("a damaged PNG file", Arrays.copyOf(png, png.length / 2)),
                Arguments.of("text", "not an image".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("undecodableImages")
    @DisplayName("bytes JavaFX cannot decode show the unsupported box: an icon, the alt text, the note, the address")
    void undecodableImage(String description, byte[] bytes) {
        MarkdownView view = viewWithImages(Map.of("a.img", bytes), 1.0);
        view.setMarkdown("![A shot](a.img)");
        layOut(view);

        assertErrorBox(view, "A shot", "Image format not supported", "a.img");
    }

    @Test
    @DisplayName("the notes of the error boxes are the view's text properties, and a change applies at once")
    void errorTexts() {
        MarkdownView view = viewWithImages(Map.of("a.svg", TestImages.svg()), 1.0);
        assertEquals("Image not found", view.getImageMissingText());
        assertEquals("Image format not supported", view.getImageUnsupportedText());
        view.setImageMissingText("Bild nicht gefunden");
        view.setMarkdown("![missing](b.png)\n\n![svg](a.svg)");
        layOut(view);
        view.setImageUnsupportedText("Bildformat nicht unterst\u00fctzt");

        List<Node> boxes = new ArrayList<>(view.lookupAll(".markdown-image-error"));
        assertEquals(List.of("missing", "Bild nicht gefunden"), labelTexts(boxes.get(0)));
        assertEquals(List.of("svg", "Bildformat nicht unterst\u00fctzt"), labelTexts(boxes.get(1)));
    }

    @Test
    @DisplayName("an image's title is its tooltip and its alt text its accessible text; without a title, no tooltip")
    void titleAndAltText() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(20, 10, RED)), 1.0);
        view.setMarkdown("![The dialog](a.png \"Settings, after a click on Save\")\n\n![Plain](a.png)");
        layOut(view);

        List<ImageView> images = images(view);
        assertEquals("Settings, after a click on Save", tooltip(images.get(0)).getText(), "the title");
        assertEquals("The dialog", images.get(0).getAccessibleText(), "the alt text");
        assertNull(tooltip(images.get(1)), "no title, no tooltip");
        assertEquals("Plain", images.get(1).getAccessibleText());
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("a paragraph of one image, or of one link around one image, apart from white space, is an image block")
    @CsvSource(delimiter = '|', value = {
        "a lone image | ![A shot](a.png)",
        "a linked image | [![A shot](a.png)](https://example.com)",
        "a linked image with spaces in the link | [ ![A shot](a.png) ](https://example.com)",
        "an image on its own line in a quote | > ![A shot](a.png)",
        "an image alone in a list item | - ![A shot](a.png)"})
    void imageBlocks(String description, String markdown) {
        MarkdownView view = viewWithImages(Map.of("a.png", png(20, 10, RED)), 1.0);
        view.setMarkdown(markdown);
        layOut(view);

        assertEquals(1, view.lookupAll(".markdown-image-block").size(), "an image block");
        assertEquals(1, images(view).size(), "its image");
        assertTrue(paragraphs(view).isEmpty(), "no paragraph");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("any other image is an inline image of its paragraph, its address neither text nor a link")
    @CsvSource(delimiter = '|', value = {
        "an image in a sentence | See ![icon](a.png) here. | icon",
        "two images | ![a](a.png) ![b](a.png) | a+b",
        "an image and text in a link | [go ![a](a.png)](https://x.y) | a",
        "an image over two lines | x ![a\\nb](a.png) | a b"})
    void otherImagesAreInline(String description, String markdown, String alts) {
        MarkdownView view = viewWithImages(Map.of("a.png", png(20, 10, RED)), 1.0);
        view.setMarkdown(markdown.replace("\\n", "\n"));
        layOut(view);

        assertTrue(view.lookupAll(".markdown-image-block").isEmpty(), "no image block");
        assertEquals(1, paragraphs(view).size(), "one paragraph");
        List<String> shown = new ArrayList<>();
        for (Node inline : paragraphs(view).get(0).lookupAll(".markdown-image-inline")) {
            shown.add(inline.lookup(".markdown-image").getAccessibleText());
        }
        assertEquals(List.of(alts.split("\\+")), shown, "the inline images, by their alt texts");
        for (Node run : view.lookupAll(".markdown-text")) {
            assertFalse(((Text) run).getText().contains("a.png"), () -> "no address as text: " + run);
        }
    }

    @Test
    @DisplayName("an image in a heading is an inline image; in a table cell it is an icon with its alt text")
    void headingAndTableImages() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(20, 10, RED)), 1.0);
        view.setMarkdown("# A ![a](a.png) heading\n\n| Icon |\n|---|\n| ![b](a.png) |");
        layOut(view);

        assertTrue(view.lookupAll(".markdown-image-block").isEmpty(), "no image block");
        Node heading = view.lookup(".markdown-heading");
        assertEquals(20, heading.lookup(".markdown-image-inline .markdown-image").getLayoutBounds().getWidth(),
                "the heading's inline image");
        Node table = view.lookup(".markdown-table");
        assertNull(table.lookup(".markdown-image"), "no picture in the table");
        assertEquals(2, table.lookupAll(".markdown-image-cell").size(), "the cell's icon and alt text");
    }

    @Test
    @DisplayName("the view's own work calls the loader on a background thread and shows the image on the FX thread")
    void backgroundWork() throws Exception {
        List<Thread> loaderThreads = new CopyOnWriteArrayList<>();
        ImageLoader files = serving(Map.of("shot.png", png(120, 80, RED)));
        // Built on the FX thread, which the background work shows its results on.
        MarkdownView view = onFxThread(() -> {
            MarkdownView backgroundView = new MarkdownView();
            backgroundView.setOutputScale(1.0);
            backgroundView.setImageLoader(address -> {
                loaderThreads.add(Thread.currentThread());
                return files.load(address);
            });
            backgroundView.setMarkdown("![A shot](shot.png)");
            return layOut(backgroundView);
        });

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        boolean shown = false;
        while (!shown && System.nanoTime() < deadline) {
            shown = onFxThread(() -> {
                relayOut(view);
                return !images(view).isEmpty();
            });
            if (!shown) {
                Thread.sleep(20);
            }
        }

        assertTrue(shown, "the image is shown within 10 s");
        assertEquals(120.0, onFxThread(() -> image(view).getLayoutBounds().getWidth()), "at its natural size");
        assertEquals(1, loaderThreads.size(), "one call of the loader");
        Thread loaderThread = loaderThreads.get(0);
        assertNotSame(Thread.currentThread(), loaderThread, "not the test's thread");
        assertTrue(loaderThread.isDaemon(), "a daemon thread");
        assertFalse(onFxThread(() -> loaderThread == Thread.currentThread()), "not the FX thread");
    }

    @Test
    @DisplayName("a new text drops the work of the old text's images not done yet")
    void newTextDropsPendingWork() {
        List<String> loaded = new ArrayList<>();
        ImageLoader files = serving(Map.of("a.png", png(20, 10, RED)));
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        view.setImageLoader(address -> {
            loaded.add(address);
            return files.load(address);
        });
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("![a](a.png)");
        layOut(view);
        view.setMarkdown("No image.");
        work.runAll();

        assertEquals(List.of(), loaded, "the old text's image is not loaded");
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * Runs code on the JavaFX Application Thread and waits for its result, at most 10 s.
     */
    private static <T> T onFxThread(Callable<T> code) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(code.call());
            } catch (Exception | AssertionError failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    /**
     * Asserts that the view's one image block shows the error box: the missing-image icon, the alt text and the note,
     * the address as its tooltip, and neither the placeholder nor an image.
     */
    private static void assertErrorBox(MarkdownView view, String alt, String note, String address) {
        Node block = view.lookup(".markdown-image-block");
        assertNotNull(block, "an image block");
        Node box = block.lookup(".markdown-image-error");
        assertNotNull(box, "the error box");
        assertNotNull(box.lookup(".ikonli-font-icon"), "an icon");
        assertEquals(BootstrapIcons.FILE_EARMARK_X, ((FontIcon) box.lookup(".ikonli-font-icon")).getIconCode(),
                "the missing-image icon");
        assertEquals(List.of(alt, note), labelTexts(box), "the alt text and the note");
        assertEquals(address, tooltip(box).getText(), "the address as tooltip");
        assertNull(block.lookup(".markdown-image-placeholder"), "no placeholder");
        assertTrue(images(view).isEmpty(), "no image");
    }

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
     * The tooltip installed on a node: {@code Tooltip.install} keeps it in the node's properties under this key, the
     * one place JavaFX keeps it.
     */
    private static Tooltip tooltip(Node node) {
        return (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
    }

    /**
     * The texts of flows, their runs joined.
     */
    private static List<String> texts(List<TextFlow> flows) {
        List<String> texts = new ArrayList<>();
        for (TextFlow flow : flows) {
            StringBuilder text = new StringBuilder();
            for (Node child : flow.getChildren()) {
                if (child instanceof Text run) {
                    text.append(run.getText());
                }
            }
            texts.add(text.toString());
        }
        return texts;
    }
}
