package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.image;
import static org.karthu.fx.markdown.MarkdownTestSupport.images;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.onFxThread;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOutUntil;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.colors;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * Animated GIFs in the Markdown view and its full size window, seen from outside: which image the document shows for
 * a GIF of several frames, JavaFX's own animated image or a bitmap of its first frame computed by the view, and which
 * the window shows. An animation needs no running pulse to be checked: the tests look at the image a node shows, not
 * at its frames over time.
 *
 * <p>The GIFs are small files of the test resources, 480 by 24 pixels: {@code animated.gif} has two frames, red then
 * blue, {@code still.gif} one red frame. The view sits in a themed {@link Scene} that is never shown; its images' work
 * runs at once on the test's thread, but for one test of the view's own background work. The window's content is
 * laid out on its own, in a themed scene that is never shown, so no window ever opens.
 */
class MarkdownViewAnimatedGifTest {

    /**
     * The test GIFs' size in pixels.
     */
    private static final int GIF_WIDTH = 480;
    private static final int GIF_HEIGHT = 24;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("an animated GIF plays in the document: JavaFX's own image with all its pixels, scaled by JavaFX alone")
    void animatedGifPlays() {
        MarkdownView view = viewWithImages(Map.of("recording.gif", gif("animated.gif")), 1.0);
        view.setMarkdown("![A recording](recording.gif)");
        layOut(view);

        ImageView image = image(view);
        double width = sceneBounds(image).getWidth();
        assertTrue(width < GIF_WIDTH - 50, () -> "shrunk to the text's width: " + width);
        assertPlays(image);
    }

    @Test
    @DisplayName("a playing GIF is kept decoded while it shows: a resize scales it anew and computes no bitmap")
    void playingGifIsKeptThroughAResize() {
        MarkdownView view = viewWithImages(Map.of("recording.gif", gif("animated.gif")), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("![A recording](recording.gif)");
        layOut(view);
        work.runAll();
        relayOut(view);
        Image playing = image(view).getImage();
        double wideWidth = sceneBounds(image(view)).getWidth();

        view.setMaxWidth(250);
        relayOut(view);
        work.runAll();
        relayOut(view);

        ImageView image = image(view);
        double narrowWidth = sceneBounds(image).getWidth();
        assertTrue(narrowWidth < wideWidth - 100, () -> "narrower: " + narrowWidth);
        assertSame(playing, image.getImage(), "the same decoded GIF, still playing");
        assertEquals(narrowWidth, image.getFitWidth(), 1e-6, "scaled by JavaFX to the new size");
    }

    @Test
    @DisplayName("animations play by default")
    void animationIsOnByDefault() {
        assertTrue(new MarkdownView().isImageAnimation());
        assertTrue(new MarkdownView().imageAnimationProperty().get());
    }

    @Test
    @DisplayName("with imageAnimation off the document shows the GIF's first frame as a bitmap the view computes at its"
            + " size in screen pixels, like any image")
    void firstFrameWhenAnimationIsOff() {
        MarkdownView view = viewWithImages(Map.of("recording.gif", gif("animated.gif")), 1.0);
        view.setImageAnimation(false);
        view.setMarkdown("![A recording](recording.gif)");
        layOut(view);

        ImageView image = image(view);
        double width = sceneBounds(image).getWidth();
        assertTrue(width < GIF_WIDTH - 50, () -> "shrunk to the text's width: " + width);
        assertFirstFrame(image);
    }

    @Test
    @DisplayName("a change of imageAnimation switches the shown GIFs in place, from the bytes the view keeps, and leaves"
            + " other images alone")
    void changeSwitchesTheShownGifsInPlace() {
        byte[] recording = gif("animated.gif");
        byte[] picture = png(600, 40, TestImages.BLUE);
        AtomicInteger loads = new AtomicInteger();
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        view.setImageLoader(address -> {
            loads.incrementAndGet();
            return address.endsWith(".gif") ? recording : picture;
        });
        view.setMarkdown("![A recording](recording.gif)\n\nWatch ![it](recording.gif) here.\n\n"
                + "![A picture](picture.png)");
        layOut(view);
        List<ImageView> nodes = images(view);
        assertEquals(3, nodes.size());
        Image pictureBitmap = nodes.get(2).getImage();
        assertTrue(pictureBitmap instanceof WritableImage, "the picture shrunk by the view");
        assertPlays(nodes.get(0));
        assertPlays(nodes.get(1));

        view.setImageAnimation(false);
        relayOut(view);
        assertEquals(nodes, images(view), "the same image nodes: nothing rebuilt");
        assertFirstFrame(nodes.get(0));
        assertFirstFrame(nodes.get(1));
        assertSame(pictureBitmap, nodes.get(2).getImage(), "the picture untouched");

        view.setImageAnimation(true);
        relayOut(view);
        assertEquals(nodes, images(view));
        assertPlays(nodes.get(0));
        assertPlays(nodes.get(1));
        assertSame(pictureBitmap, nodes.get(2).getImage(), "the picture untouched");
        assertEquals(3, loads.get(), "each image loaded once: the switch decodes the bytes kept");
    }

    @Test
    @DisplayName("an animated GIF in a sentence plays too")
    void inlineGifPlays() {
        MarkdownView view = viewWithImages(Map.of("recording.gif", gif("animated.gif")), 1.0);
        view.setMarkdown("Watch ![a recording](recording.gif) here.");
        layOut(view);

        Node inline = view.lookup(".markdown-image-inline");
        assertNotNull(inline, "an inline image");
        ImageView image = (ImageView) inline.lookup(".markdown-image");
        assertTrue(sceneBounds(image).getWidth() < GIF_WIDTH - 50, "shrunk to the text's width");
        assertFalse(image.getImage() instanceof WritableImage, "no bitmap computed by the view");
        assertEquals(GIF_WIDTH, image.getImage().getWidth(), "the decoded GIF itself");
    }

    @Test
    @DisplayName("the view's own background work plays an animated GIF decoded on its decoding thread, and switches it")
    void backgroundWorkPlaysAnimatedGif() throws Exception {
        byte[] recording = gif("animated.gif");
        // Built on the FX thread, which the background work shows its results on.
        MarkdownView view = onFxThread(() -> {
            MarkdownView backgroundView = new MarkdownView();
            backgroundView.setOutputScale(1.0);
            backgroundView.setImageLoader(TestImages.serving(Map.of("recording.gif", recording)));
            backgroundView.setMarkdown("![A recording](recording.gif)");
            return layOut(backgroundView);
        });

        assertTrue(relayOutUntil(view, () -> images(view).size() == 1), "the GIF shows within 10 s");
        onFxThread(() -> {
            assertPlays(image(view));
            view.setImageAnimation(false);
            return null;
        });

        assertTrue(relayOutUntil(view, () -> image(view).getImage() instanceof WritableImage),
                "its first frame shows within 10 s");
        onFxThread(() -> {
            assertFirstFrame(image(view));
            return null;
        });
    }

    @Test
    @DisplayName("a GIF of one frame is an ordinary image: shrunk by the view's area average")
    void singleFrameGifIsAnOrdinaryImage() {
        MarkdownView view = viewWithImages(Map.of("still.gif", gif("still.gif")), 1.0);
        view.setMarkdown("![A still](still.gif)");
        layOut(view);

        ImageView image = image(view);
        double width = sceneBounds(image).getWidth();
        assertTrue(width < GIF_WIDTH - 50, () -> "shrunk to the text's width: " + width);
        assertFirstFrame(image);
    }

    @Test
    @DisplayName("the full size window plays an animated GIF at every zoom, also below 100 % and while the document"
            + " shows its first frame")
    void windowPlaysAnimatedGif() {
        MarkdownView view = new MarkdownView();
        view.setImageAnimation(false);
        ImageWindowContent content = windowContent(view);
        content.show(ImageWindowRequest.of("A recording", "recording.gif", gif("animated.gif")), () -> {
        });
        layOutWindow(content);

        ImageView picture = windowPicture(content);
        assertTrue(picture.getFitWidth() < GIF_WIDTH - 50, () -> "fitted below 100 %: " + picture.getFitWidth());
        assertPlays(picture);

        Node viewer = content.lookup(".markdown-image-viewer");
        Bounds area = sceneBounds(viewer);
        Event.fireEvent(viewer, new ScrollEvent(ScrollEvent.SCROLL, area.getCenterX(), area.getCenterY(),
                area.getCenterX(), area.getCenterY(), false, !MarkdownTestSupport.MAC, false, MarkdownTestSupport.MAC,
                false, false, 0, 40, 0, 40, 40, 40, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                ScrollEvent.VerticalTextScrollUnits.LINES, 3, 0, null));
        layOutWindow(content);
        assertTrue(picture.getFitWidth() < GIF_WIDTH - 50, () -> "zoomed, still below 100 %: "
                + picture.getFitWidth());
        assertPlays(picture);
    }

    @Test
    @DisplayName("the window shows a GIF of one frame below 100 % as a bitmap shrunk by the area average")
    void windowShrinksSingleFrameGif() {
        ImageWindowContent content = windowContent(new MarkdownView());
        content.show(ImageWindowRequest.of("A still", "still.gif", gif("still.gif")), () -> {
        });
        layOutWindow(content);

        assertFirstFrame(windowPicture(content));
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * The full size window's content of a view in a themed scene of 200 by 150 pixels that is never shown, its work at
     * once, at the output scale 1.0: the test GIFs, 480 pixels wide, show in it fitted below 100 %.
     */
    private static ImageWindowContent windowContent(MarkdownView view) {
        ImageWindowContent content = new ImageWindowContent(view, MarkdownTestSupport.notes(view),
                new ImmediateWork(), () -> {
        });
        content.setOutputScale(1.0);
        Scene scene = new Scene(content, 200, 150);
        ThemeManager.apply(scene);
        layOutWindow(content);
        return content;
    }

    /**
     * Applies CSS to the window's content and lays it out, as the next pulse does.
     */
    private static void layOutWindow(ImageWindowContent content) {
        content.applyCss();
        content.layout();
    }

    /**
     * The image node of the window's viewer.
     */
    private static ImageView windowPicture(ImageWindowContent content) {
        return (ImageView) content.lookup(".markdown-image-viewer").lookup(".image-view");
    }

    /**
     * Asserts that an image node plays the test's animated GIF: JavaFX's own decoded image with all its pixels, scaled
     * by JavaFX to the shown size.
     */
    private static void assertPlays(ImageView image) {
        assertFalse(image.getImage() instanceof WritableImage, "no bitmap computed by the view");
        assertEquals(GIF_WIDTH, image.getImage().getWidth(), "the decoded GIF itself, all its pixels");
        assertEquals(GIF_HEIGHT, image.getImage().getHeight());
        assertEquals(sceneBounds(image).getWidth(), image.getFitWidth(), 1e-6, "scaled by JavaFX");
    }

    /**
     * Asserts that an image node shows the test's animated GIF as a bitmap of its first frame, computed by the view at
     * the size the node shows on screen, at the output scale 1.0.
     */
    private static void assertFirstFrame(ImageView image) {
        Image shown = image.getImage();
        assertTrue(shown instanceof WritableImage, "a bitmap computed by the view");
        int width = (int) shown.getWidth();
        int height = (int) shown.getHeight();
        assertEquals(Math.round(sceneBounds(image).getWidth()), width, "as many pixels wide as on screen");
        assertEquals(Math.round(sceneBounds(image).getHeight()), height, "as many pixels high as on screen");
        assertEquals(List.of(RED, RED, RED), colors(shown, 0, 0, width / 2, height / 2, width - 1, height - 1),
                "the first frame, red, not the second, blue");
    }

    /**
     * The bytes of a test GIF of the resources.
     */
    private static byte[] gif(String name) {
        try (InputStream in = MarkdownViewAnimatedGifTest.class.getResourceAsStream("images/" + name)) {
            assertNotNull(in, "the test resource images/" + name);
            return in.readAllBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
