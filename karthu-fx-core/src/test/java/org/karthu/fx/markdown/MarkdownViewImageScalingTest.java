package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.image;
import static org.karthu.fx.markdown.MarkdownTestSupport.images;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.onFxThread;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOutUntil;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * How the Markdown view shrinks its images, seen from outside: an image file handed to the view by a loader of the
 * test, the shown image found by its documented style class, its size in pixels and its pixels read back.
 *
 * <p>The view computes a shrunk image itself, at its exact size in screen pixels, each pixel the coverage weighted
 * average of the file's pixels it covers. A checkerboard of one pixel squares shows where that differs from JavaFX's
 * own scaling: shrunk by a whole factor it is an even grey, where plain scaling picks single pixels and shows
 * stripes.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown; its images' work runs at once on the test's thread,
 * or waits in a queue the test runs, and the view assumes an output scale of the test's choosing.
 */
class MarkdownViewImageScalingTest {

    /**
     * Opaque white, as an ARGB value.
     */
    private static final int WHITE = 0xFFFFFFFF;

    /**
     * Opaque black, as an ARGB value.
     */
    private static final int BLACK = 0xFF000000;

    /**
     * The file of {@link #screenshot()}, made at its first use.
     */
    private static byte[] screenshot;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @ParameterizedTest(name = "shrunk by {0}")
    @ValueSource(ints = {4, 5, 6})
    @DisplayName("a checkerboard of one pixel squares shrinks to an even grey, where plain scaling shows stripes")
    void checkerboardShrinksToAnEvenGrey(int factor) {
        Image shown = shrunk(factor, (x, y) -> (x + y) % 2 == 0 ? WHITE : BLACK);

        PixelReader pixels = shown.getPixelReader();
        for (int y = 0; y < (int) shown.getHeight(); y++) {
            for (int x = 0; x < (int) shown.getWidth(); x++) {
                int argb = pixels.getArgb(x, y);
                int pixelX = x;
                int pixelY = y;
                assertEquals(0xFF, argb >>> 24, () -> "opaque at " + pixelX + ", " + pixelY);
                // Half white: 127.5. A square of 5 by 5 pixels holds 12 or 13 white ones, 122.4 or 132.6.
                for (int shift = 0; shift <= 16; shift += 8) {
                    int channel = (argb >> shift) & 0xFF;
                    assertEquals(127.5, channel, 6, () -> "grey at " + pixelX + ", " + pixelY + ": "
                            + Integer.toHexString(argb));
                }
            }
        }
    }

    @Test
    @DisplayName("a gradient in steps as wide as the factor keeps its values: no pixel is shifted into its neighbour")
    void gradientKeepsItsValues() {
        int factor = 4;
        // Each square of 4 by 4 pixels has one colour, which names its column and its row; a shift of half a pixel,
        // as JavaFX's own loader scaling has, would mix neighbouring squares.
        Image shown = shrunk(factor, (x, y) -> gradient(x / factor, y / factor));

        PixelReader pixels = shown.getPixelReader();
        for (int y = 0; y < (int) shown.getHeight(); y++) {
            for (int x = 0; x < (int) shown.getWidth(); x++) {
                int pixelX = x;
                int pixelY = y;
                assertEquals(Integer.toHexString(gradient(x, y)), Integer.toHexString(pixels.getArgb(x, y)),
                        () -> "at " + pixelX + ", " + pixelY);
            }
        }
    }

    @Test
    @DisplayName("transparent pixels add no colour: red beside transparent shrinks to half transparent red")
    void transparentPixelsAddNoColour() {
        Image shown = shrunk(4, (x, y) -> (x + y) % 2 == 0 ? TestImages.RED : 0x00000000);

        PixelReader pixels = shown.getPixelReader();
        for (int y = 0; y < (int) shown.getHeight(); y++) {
            for (int x = 0; x < (int) shown.getWidth(); x++) {
                int argb = pixels.getArgb(x, y);
                assertEquals(127.5, argb >>> 24, 1, "half transparent");
                assertEquals(0xFF, (argb >> 16) & 0xFF, 1, "red, not darkened");
                assertEquals(0, argb & 0xFFFF, "no green, no blue");
            }
        }
    }

    @ParameterizedTest(name = "output scale {0}, zoom {1}")
    @CsvSource({"1.0, 1.0", "1.5, 1.0", "2.0, 1.0", "1.0, 2.0", "1.5, 1.25", "2.0, 0.5"})
    @DisplayName("a 4K screenshot is kept as a bitmap of the size it shows at: one pixel per screen pixel")
    void bitmapHasTheSizeInScreenPixels(double scale, double zoom) {
        MarkdownView view = viewWithImages(Map.of("shot.png", screenshot()), scale);
        view.setMarkdown("![A screenshot](shot.png)");
        layOut(view);
        view.setZoom(zoom);
        relayOut(view);

        ImageView image = image(view);
        Image bitmap = image.getImage();
        assertTrue(bitmap.getWidth() < 3840, () -> "shrunk, not the decoded image: " + bitmap.getWidth());
        // Exactly, not rounded: a picture a fraction of a screen pixel larger or smaller than its bitmap resamples
        // every row and column.
        assertEquals(bitmap.getWidth(), image.getFitWidth() * zoom * scale, 1e-9,
                "as wide on screen as the bitmap's pixels: one pixel per screen pixel");
        assertEquals(bitmap.getHeight(), image.getFitHeight() * zoom * scale, 1e-9,
                "as high on screen as the bitmap's pixels: one pixel per screen pixel");
        Bounds onScreen = sceneBounds(image);
        assertEquals(bitmap.getWidth(), onScreen.getWidth() * scale, 1e-3, "in the scene too");
        assertEquals(bitmap.getHeight(), onScreen.getHeight() * scale, 1e-3, "in the scene too");
    }

    @Test
    @DisplayName("an image the zoom shows beyond its natural size keeps its own pixels, which JavaFX enlarges")
    void enlargingIsLeftToJavaFx() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(40, 30, TestImages.BLUE)), 1.0);
        view.setMarkdown("![An icon](icon.png)");
        layOut(view);
        view.setZoom(2.0);
        relayOut(view);

        ImageView image = image(view);
        assertEquals(80, sceneBounds(image).getWidth(), 1e-6, "twice as wide on screen");
        assertEquals(40, image.getImage().getWidth(), "the image's own width");
        assertEquals(30, image.getImage().getHeight(), "the image's own height");
    }

    @Test
    @DisplayName("during a resize the last bitmap is scaled to the new size; once the size has settled it is computed anew")
    void resizeComputesTheBitmapAnewOnceSettled() {
        MarkdownView view = viewWithImages(Map.of("shot.png", screenshot()), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("![A screenshot](shot.png)");
        layOut(view);
        work.runAll();
        relayOut(view);
        Image wide = image(view).getImage();
        double wideWidth = sceneBounds(image(view)).getWidth();
        assertEquals(Math.round(wideWidth), wide.getWidth(), "computed at the first size");

        view.setMaxWidth(250);
        relayOut(view);
        ImageView image = image(view);
        double narrowWidth = sceneBounds(image).getWidth();
        assertTrue(narrowWidth < wideWidth - 100, () -> "narrower: " + narrowWidth);
        assertSame(wide, image.getImage(), "meanwhile the last bitmap, scaled to the new size");

        work.runAll();
        assertEquals(Math.round(narrowWidth), image.getImage().getWidth(), "computed anew at the new size");
        assertEquals(Math.round(sceneBounds(image).getHeight()), image.getImage().getHeight());
    }

    @Test
    @DisplayName("a change of the output scale computes the bitmap anew for the new screen")
    void outputScaleChangeComputesTheBitmapAnew() {
        MarkdownView view = viewWithImages(Map.of("shot.png", screenshot()), 1.0);
        view.setMarkdown("![A screenshot](shot.png)");
        layOut(view);
        double width = sceneBounds(image(view)).getWidth();
        assertEquals(Math.round(width), image(view).getImage().getWidth());

        view.setOutputScale(2.0);
        relayOut(view);

        assertEquals(Math.round(2 * width), image(view).getImage().getWidth(), "twice the pixels on that screen");
    }

    @Test
    @DisplayName("the view's own work computes the bitmap in the background, and anew once a new size has settled")
    void backgroundWorkComputesTheBitmap() throws Exception {
        // Built on the FX thread, which the background work shows its results on.
        MarkdownView view = onFxThread(() -> {
            MarkdownView backgroundView = new MarkdownView();
            backgroundView.setOutputScale(1.0);
            backgroundView.setImageLoader(TestImages.serving(Map.of("shot.png", screenshot())));
            backgroundView.setMarkdown("![A screenshot](shot.png)");
            return layOut(backgroundView);
        });

        assertTrue(relayOutUntil(view, () -> showsBitmapOfItsSize(view)), "the bitmap is computed within 10 s");
        double wideWidth = onFxThread(() -> sceneBounds(image(view)).getWidth());

        onFxThread(() -> {
            view.setMaxWidth(250);
            relayOut(view);
            return null;
        });

        assertTrue(relayOutUntil(view, () -> showsBitmapOfItsSize(view)), "computed anew within 10 s");
        assertTrue(onFxThread(() -> image(view).getImage().getWidth()) < wideWidth - 100, "at the new size");
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * Whether the view shows its one image as a bitmap of the size the image has on screen.
     */
    private static boolean showsBitmapOfItsSize(MarkdownView view) {
        List<ImageView> shown = images(view);
        if (shown.size() != 1) {
            return false;
        }
        Image bitmap = shown.get(0).getImage();
        Bounds onScreen = sceneBounds(shown.get(0));
        return bitmap.getWidth() == Math.round(onScreen.getWidth())
                && bitmap.getHeight() == Math.round(onScreen.getHeight());
    }

    /**
     * A screenshot of a 4K screen, made once: 3840 by 2160 pixels in stripes of colour.
     */
    private static byte[] screenshot() {
        if (screenshot == null) {
            screenshot = png(3840, 2160, (x, y) -> (x / 7 + y / 5) % 2 == 0 ? TestImages.RED : TestImages.BLUE);
        }
        return screenshot;
    }

    /**
     * An opaque colour that names a column and a row: red the column, green the row, blue both, each modulo 256.
     */
    private static int gradient(int column, int row) {
        return 0xFF000000 | (column % 256) << 16 | (row % 256) << 8 | ((column + row) % 256);
    }

    /**
     * The image the view shows for a picture that it shrinks by a whole factor: the picture is that factor times as
     * wide as the text, which the test measures first, and 20 pixels high, so the view shows it as wide as the text
     * and 20 pixels high, at the output scale 1.0 and the zoom 100 %. Asserts that the shown image has exactly that
     * size in pixels.
     */
    private static Image shrunk(int factor, IntBinaryOperator argb) {
        Map<String, byte[]> files = new ConcurrentHashMap<>();
        MarkdownView view = viewWithImages(files, 1.0);
        view.setMarkdown("A paragraph as wide as the text.");
        layOut(view);
        double textWidth = paragraphs(view).get(0).getLayoutBounds().getWidth();
        int width = (int) textWidth;
        assertEquals(width, textWidth, "the test's text is a whole number of pixels wide");
        files.put("picture.png", png(factor * width, factor * 20, argb));
        view.setMarkdown("![A picture](picture.png)");
        relayOut(view);

        Image shown = image(view).getImage();
        assertEquals(width, shown.getWidth(), "one pixel per screen pixel, as wide as the text");
        assertEquals(20, shown.getHeight(), "one pixel per screen pixel, 20 high");
        return shown;
    }
}
