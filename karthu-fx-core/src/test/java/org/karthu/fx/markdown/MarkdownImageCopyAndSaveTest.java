package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.TestImages.BLUE;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.colors;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * <em>Copy image</em> and <em>Save as</em> in the full size window, seen from outside: the window's content laid out
 * in a themed scene that is never shown, its image clipboard, its file chooser and its error dialog replaced by
 * recorders, so no test touches the system clipboard, a real file chooser or a real dialog. Saved files go into a
 * temporary folder of the test.
 */
class MarkdownImageCopyAndSaveTest {

    /**
     * The size of the test scene, which the content fills.
     */
    private static final double WIDTH = 600;
    private static final double HEIGHT = 400;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("Copy image puts the whole image on the clipboard, not the shrunk bitmap the window shows")
    void copyPutsTheWholeImage() {
        ImageWindowContent content = content(new MarkdownView());
        List<Image> copies = new ArrayList<>();
        content.setImageClipboard(image -> copies.add(image));
        content.show(ImageWindowRequest.of("Wide", "wide.png", png(2400, 600, (x, y) -> x < 1200 ? RED : BLUE)),
                () -> {
                });
        relayOut(content);

        button(content, ".markdown-image-copy").fire();

        assertEquals(1, copies.size(), "one copy");
        Image copied = copies.getFirst();
        assertEquals(2400, copied.getWidth(), "the whole image, though the window shows it at 25 %");
        assertEquals(600, copied.getHeight());
        assertEquals(List.of(RED, BLUE), colors(copied, 0, 0, 2399, 599));
    }

    @Test
    @DisplayName("Copy image of an animated GIF puts its first frame on the clipboard as a still image, whose pixels"
            + " can be read, while the window plays the GIF")
    void copyOfAnAnimatedGifIsItsFirstFrame() {
        ImageWindowContent content = content(new MarkdownView());
        List<Image> copies = new ArrayList<>();
        content.setImageClipboard(image -> copies.add(image));
        content.show(ImageWindowRequest.of("A recording", "recording.gif", gif("animated.gif")), () -> {
        });
        relayOut(content);

        button(content, ".markdown-image-copy").fire();

        assertEquals(1, copies.size(), "one copy");
        Image copied = copies.getFirst();
        assertNotNull(copied.getPixelReader(), "a still image: JavaFX reads no pixels of a playing one");
        assertEquals(480, copied.getWidth(), "the whole image");
        assertEquals(24, copied.getHeight());
        assertEquals(List.of(RED, RED, RED), colors(copied, 0, 0, 240, 12, 479, 23), "the first frame, red");
    }

    @Test
    @DisplayName("Save as of an animated GIF writes the original file, all its frames")
    void saveOfAnAnimatedGifIsTheWholeFile(@TempDir Path folder) throws IOException {
        ImageWindowContent content = content(new MarkdownView());
        Path chosen = folder.resolve("recording.gif");
        content.setSaveDialog((owner, title, fileName) -> chosen);
        byte[] recording = gif("animated.gif");
        content.show(ImageWindowRequest.of("A recording", "recording.gif", recording), () -> {
        });

        button(content, ".markdown-image-save").fire();

        assertArrayEquals(recording, Files.readAllBytes(chosen));
    }

    @Test
    @DisplayName("Copy image and Save as sit at the bar's right end, their labels the view's texts")
    void buttonsAndTexts() {
        MarkdownView view = new MarkdownView();
        ImageWindowContent content = content(view);
        Button copy = button(content, ".markdown-image-copy");
        Button save = button(content, ".markdown-image-save");
        assertEquals("Copy image", copy.getText());
        assertEquals("Save as...", save.getText());
        view.setCopyImageText("Bild kopieren");
        view.setSaveImageText("Speichern unter...");
        assertEquals("Bild kopieren", copy.getText());
        assertEquals("Speichern unter...", save.getText());

        relayOut(content);
        Bounds zoom = sceneBounds(content.lookup(".markdown-image-zoom"));
        Bounds copyBounds = sceneBounds(copy);
        Bounds saveBounds = sceneBounds(save);
        Bounds bar = sceneBounds(content.lookup(".markdown-image-bar"));
        assertTrue(copyBounds.getMinX() > zoom.getMaxX() + 100, "apart from the zoom, at the right end");
        assertTrue(saveBounds.getMinX() > copyBounds.getMaxX(), "Save as after Copy image");
        assertEquals(bar.getMaxX() - 4, saveBounds.getMaxX(), 1, "at the bar's right edge, inside its padding");
    }

    @Test
    @DisplayName("with its English texts in the theme's font, the whole bar fits the smallest window")
    void englishBarFitsTheSmallestWindow() {
        // The smallest window's width is chosen for the theme's font. In another font the bar may ask for more, and
        // a window is then as wide as its bar asks (ImageWindow.firstSize).
        ImageWindowContent content = content(new MarkdownView());
        Font shown = button(content, ".markdown-image-copy").getFont();
        assumeTrue(shown.getFamily().startsWith("Segoe UI"),
                "measures the bar in the theme's font, Segoe UI; shown in " + shown.getFamily());
        double smallestWindow = ImageWindow.firstSize(1, 1, 0, 0, new Rectangle2D(0, 0, 1920, 1040)).getWidth();
        assertTrue(content.lookup(".markdown-image-bar").prefWidth(-1) <= smallestWindow,
                "with its English texts the whole bar fits the smallest window");
    }

    @Test
    @DisplayName("Copy image is disabled while no image shows: while it is decoded, where a note shows instead, and"
            + " once the window has closed")
    void copyOnlyWhileAnImageShows() {
        QueuedWork work = new QueuedWork();
        ImageWindowContent content = content(new MarkdownView(), work);
        Button copy = button(content, ".markdown-image-copy");

        content.show(ImageWindowRequest.of("Small", "small.png", png(30, 20, RED)), () -> {
        });
        assertTrue(copy.isDisabled(), "while the image is decoded");
        work.runAll();
        assertFalse(copy.isDisabled(), "once it shows");

        content.show(ImageWindowRequest.of("Gone", "gone.png", null), () -> {
        });
        assertTrue(copy.isDisabled(), "no file");

        content.show(ImageWindowRequest.of("Drawing", "drawing.svg", TestImages.svg()), () -> {
        });
        work.runAll();
        assertTrue(copy.isDisabled(), "a file JavaFX cannot decode");

        content.show(ImageWindowRequest.of("Small", "small.png", png(30, 20, RED)), () -> {
        });
        work.runAll();
        content.clear();
        assertTrue(copy.isDisabled(), "the window has closed");
    }

    @Test
    @DisplayName("Save as asks the file chooser where to save, titled by the view's text and suggesting the file name"
            + " of the address, and writes the image file's bytes unchanged")
    void saveWritesTheFile(@TempDir Path folder) throws IOException {
        ImageWindowContent content = content(new MarkdownView());
        List<String> asked = new ArrayList<>();
        Path chosen = folder.resolve("chosen.png");
        content.setSaveDialog((owner, title, fileName) -> {
            asked.add(title + " | " + fileName);
            return chosen;
        });
        byte[] file = png(30, 20, RED);
        content.show(ImageWindowRequest.of("A dialog", "docs/shots/dialog.png", file), () -> {
        });

        button(content, ".markdown-image-save").fire();

        assertEquals(List.of("Save as... | dialog.png"), asked);
        assertArrayEquals(file, Files.readAllBytes(chosen), "the file as the loader returned it, byte for byte");
    }

    @Test
    @DisplayName("Save as is there while the window has the image's file: disabled without a file and once the window"
            + " has closed, enabled while the image is decoded and for a file JavaFX cannot show, which it saves too")
    void saveWhileThereIsAFile(@TempDir Path folder) throws IOException {
        QueuedWork work = new QueuedWork();
        ImageWindowContent content = content(new MarkdownView(), work);
        Path chosen = folder.resolve("drawing.svg");
        content.setSaveDialog((owner, title, fileName) -> chosen);
        Button save = button(content, ".markdown-image-save");
        assertTrue(save.isDisabled(), "nothing shown yet");

        content.show(ImageWindowRequest.of("Gone", "gone.png", null), () -> {
        });
        assertTrue(save.isDisabled(), "no file");

        content.show(ImageWindowRequest.of("Drawing", "drawing.svg", TestImages.svg()), () -> {
        });
        assertFalse(save.isDisabled(), "while the image is decoded");
        work.runAll();
        assertFalse(save.isDisabled(), "a file JavaFX cannot decode");
        save.fire();
        work.runAll();
        assertArrayEquals(TestImages.svg(), Files.readAllBytes(chosen), "saved as it is");

        content.clear();
        assertTrue(save.isDisabled(), "the window has closed");
    }

    @Test
    @DisplayName("the file is written by the window's background work, never on the JavaFX Application Thread, and a"
            + " failure reported back on it")
    void writesInTheBackground(@TempDir Path folder) throws IOException {
        QueuedWork work = new QueuedWork();
        ImageWindowContent content = content(new MarkdownView(), work);
        List<Path> targets = new ArrayList<>(List.of(folder.resolve("shot.png"), folder.resolve("gone/shot.png")));
        content.setSaveDialog((owner, title, fileName) -> targets.removeFirst());
        List<String> errors = new ArrayList<>();
        content.setErrorDialog((owner, title, header, text) -> errors.add(text));
        byte[] file = png(30, 20, RED);
        content.show(ImageWindowRequest.of("Shot", "shot.png", file), () -> {
        });
        work.runAll();

        button(content, ".markdown-image-save").fire();
        assertFalse(Files.exists(folder.resolve("shot.png")), "not on the JavaFX Application Thread");
        work.runAll();
        assertArrayEquals(file, Files.readAllBytes(folder.resolve("shot.png")));

        button(content, ".markdown-image-save").fire();
        assertEquals(List.of(), errors, "not yet");
        work.runAll();
        assertEquals(List.of("NoSuchFileException"), errors);
    }

    @Test
    @DisplayName("the suggested name is the file name of the address, its escapes decoded; for a data: address image"
            + " and the extension of its type; image where the address names no file")
    void suggestedNames() {
        assertEquals("dialog.png", suggestedName("docs/shots/dialog.png?raw=true#top"), "without query and fragment");
        assertEquals("a.png", suggestedName("C:\\docs\\a.png"), "a Windows path");
        assertEquals("logo.gif", suggestedName("jar:file:/app.jar!/img/logo.gif"));
        assertEquals("my screen shot.png", suggestedName("my%20screen%20shot.png"), "escapes decoded");
        assertEquals("\u00fcber.png", suggestedName("%C3%BCber.png"), "as UTF-8");
        assertEquals("a%2Fb.png", suggestedName("a%2Fb.png"), "as written where the decoded name is no file name");
        assertEquals("100%.png", suggestedName("100%.png"), "as written where it holds no escapes");
        assertEquals("image.png", suggestedName("data:image/png;base64,iVBORw0KGgo="));
        assertEquals("image.svg", suggestedName("data:image/svg+xml;base64,PHN2Zz4="), "the type without its suffix");
        assertEquals("image.jpeg", suggestedName("DATA:Image/JPEG;base64,/9j/"), "in lower case");
        assertEquals("image", suggestedName("data:;base64,AAAA"), "no type");
        assertEquals("image", suggestedName("https://example.com/"), "no file name");
    }

    @Test
    @DisplayName("if writing fails, the error dialog shows the file and the reason, titled by the view's text")
    void saveFailureShowsTheReason(@TempDir Path folder) {
        MarkdownView view = new MarkdownView();
        view.setSaveImageText("Speichern unter...");
        ImageWindowContent content = content(view);
        Path chosen = folder.resolve("gone").resolve("shot.png");
        content.setSaveDialog((owner, title, fileName) -> chosen);
        List<String> errors = new ArrayList<>();
        content.setErrorDialog((owner, title, header, text) -> errors.add(title + " | " + header + " | " + text));
        content.show(ImageWindowRequest.of("Shot", "shot.png", png(30, 20, RED)), () -> {
        });

        button(content, ".markdown-image-save").fire();

        assertEquals(List.of("Speichern unter... | " + chosen + " | NoSuchFileException"), errors,
                "the folder does not exist");
        assertFalse(Files.exists(chosen));
    }

    @Test
    @DisplayName("the reason of a failed write is the system's reason where it gives one, else what failed")
    void reasons() {
        assertEquals("The process cannot access the file", ImageWindowContent.reason(new FileSystemException(
                "C:\\shot.png", null, "The process cannot access the file")));
        assertEquals("AccessDeniedException", ImageWindowContent.reason(new AccessDeniedException("C:\\shot.png")));
        assertEquals("There is not enough space on the disk", ImageWindowContent.reason(
                new IOException("There is not enough space on the disk")));
        assertEquals("IOException", ImageWindowContent.reason(new IOException()));
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * The file name <em>Save as</em> suggests for an image of an address, read from the file chooser it asks, which
     * the reader cancels.
     */
    private static String suggestedName(String address) {
        ImageWindowContent content = content(new MarkdownView());
        List<String> names = new ArrayList<>();
        content.setSaveDialog((owner, title, fileName) -> {
            names.add(fileName);
            return null;
        });
        content.show(ImageWindowRequest.of("", address, png(3, 2, RED)), () -> {
        });
        button(content, ".markdown-image-save").fire();
        assertEquals(1, names.size(), "asked once for " + address);
        return names.getFirst();
    }

    /**
     * The bytes of a test GIF of the resources, 480 by 24 pixels: {@code animated.gif} has two frames, red then blue.
     */
    private static byte[] gif(String name) {
        try (InputStream in = MarkdownImageCopyAndSaveTest.class.getResourceAsStream("images/" + name)) {
            assertNotNull(in, "the test resource images/" + name);
            return in.readAllBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /**
     * The window's content of a view in a themed scene of {@value #WIDTH} by {@value #HEIGHT} pixels that is never
     * shown, its work at once, assuming the output scale 1.0.
     */
    private static ImageWindowContent content(MarkdownView view) {
        return content(view, new ImmediateWork());
    }

    /**
     * The window's content as {@link #content(MarkdownView)} makes it, with its work.
     */
    private static ImageWindowContent content(MarkdownView view, ImageWork work) {
        ImageWindowContent content = new ImageWindowContent(view, MarkdownTestSupport.notes(view),
                work, () -> {
        });
        content.setOutputScale(1.0);
        Scene scene = new Scene(content, WIDTH, HEIGHT);
        ThemeManager.apply(scene);
        relayOut(content);
        return content;
    }

    /**
     * Applies CSS and lays the content out, as the next pulse does.
     */
    private static void relayOut(ImageWindowContent content) {
        content.applyCss();
        content.layout();
    }

    private static Button button(ImageWindowContent content, String selector) {
        return (Button) content.lookup(selector);
    }
}
