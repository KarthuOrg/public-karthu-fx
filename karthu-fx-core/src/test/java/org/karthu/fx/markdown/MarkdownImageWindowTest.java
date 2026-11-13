package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.theme.ThemeManager;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * The full size window's content seen from outside: the bar and the viewer, laid out in a themed scene that is never
 * shown, so no window ever opens. The content decodes the image of a request to open, shows it fitted or at 100 %,
 * zooms, moves and closes. Its work runs at once on the test's thread, and it assumes an output scale, as a scene that
 * is never shown has no screen.
 */
class MarkdownImageWindowTest {

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
    @DisplayName("an image larger than the viewer starts fitted, never beyond 100 %, its zoom shown in percent")
    void startsFitted() {
        ImageWindowContent content = content(1.0, new ArrayList<>());
        content.show(ImageWindowRequest.of("Wide", "wide.png", png(2400, 120, RED)), () -> {
        });
        relayOut(content);

        assertEquals(WIDTH, viewer(content).getLayoutBounds().getWidth(), "the viewer fills the window's width");
        assertEquals("25%", zoomLabel(content).getText());
        ImageView picture = picture(content);
        assertEquals(600, picture.getFitWidth(), 1e-9);
        assertEquals(30, picture.getFitHeight(), 1e-9);
        Bounds shown = sceneBounds(picture);
        Bounds area = sceneBounds(viewer(content));
        assertEquals(area.getCenterY(), shown.getCenterY(), 0.5, "in the middle");
    }

    @Test
    @DisplayName("an image that fits starts at 100 %, one image pixel per screen pixel, on whole screen pixels, at the"
            + " scales 1.0, 1.5 and 2.0")
    void smallImageAtActualSize() {
        for (double scale : new double[] {1.0, 1.5, 2.0}) {
            ImageWindowContent content = content(scale, new ArrayList<>());
            byte[] file = png(301, 151, RED);
            content.show(ImageWindowRequest.of("Small", "small.png", file), () -> {
            });
            relayOut(content);

            assertEquals("100%", zoomLabel(content).getText(), "at " + scale);
            ImageView picture = picture(content);
            assertEquals(301, picture.getImage().getWidth(), "the decoded image itself at " + scale);
            assertEquals(301 / scale, picture.getFitWidth(), 1e-9, "one image pixel per screen pixel at " + scale);
            assertEquals(151 / scale, picture.getFitHeight(), 1e-9);
            Bounds shown = sceneBounds(picture);
            assertWholeScreenPixel(shown.getMinX() * scale, "left edge at " + scale);
            assertWholeScreenPixel(shown.getMinY() * scale, "top edge at " + scale);
            Bounds area = sceneBounds(viewer(content));
            assertEquals(area.getCenterX(), shown.getCenterX(), 1 / scale, "in the middle at " + scale);
        }
    }

    @Test
    @DisplayName("below 100 % the image shows as a bitmap of its exact size in screen pixels, shrunk by the area"
            + " average: a checkerboard of one pixel squares becomes an even grey")
    void shrunkByTheAreaAverage() {
        ImageWindowContent content = content(1.5, new ArrayList<>());
        // 3600 by 180 pixels at 1.5 in a viewer 600 wide: 900 screen pixels, a quarter.
        content.show(ImageWindowRequest.of("Checks", "checks.png",
                png(3600, 180, (x, y) -> (x + y) % 2 == 0 ? 0xFF000000 : 0xFFFFFFFF)), () -> {
        });
        relayOut(content);

        assertEquals("25%", zoomLabel(content).getText());
        Image bitmap = picture(content).getImage();
        assertEquals(900, bitmap.getWidth());
        assertEquals(45, bitmap.getHeight());
        assertEquals(600, picture(content).getFitWidth(), 1e-9);
        for (int x = 0; x < 900; x += 37) {
            for (int y = 0; y < 45; y += 7) {
                int grey = bitmap.getPixelReader().getArgb(x, y) & 0xFF;
                assertTrue(Math.abs(grey - 127.5) <= 1, "even grey at " + x + "," + y + ": " + grey);
            }
        }
    }

    @Test
    @DisplayName("100 % shows the image at one image pixel per screen pixel around its middle, Fit fits it again")
    void barButtons() {
        ImageWindowContent content = content(1.0, new ArrayList<>());
        content.show(ImageWindowRequest.of("Big", "big.png", png(1200, 900, RED)), () -> {
        });
        relayOut(content);
        double fittedWidth = picture(content).getFitWidth();

        button(content, ".markdown-image-actual-size").fire();
        relayOut(content);
        assertEquals("100%", zoomLabel(content).getText());
        assertEquals(1200, picture(content).getFitWidth(), 1e-9);
        assertEquals(1200, picture(content).getImage().getWidth(), "the image itself");
        Bounds area = sceneBounds(viewer(content));
        Bounds shown = sceneBounds(picture(content));
        assertEquals(area.getCenterX(), shown.getCenterX(), 1, "its middle in the middle");
        assertEquals(area.getCenterY(), shown.getCenterY(), 1, "its middle in the middle");

        button(content, ".markdown-image-fit").fire();
        relayOut(content);
        assertEquals(fittedWidth, picture(content).getFitWidth(), 1e-9);
        assertTrue(area.contains(sceneBounds(picture(content))), "the whole image in the area");
    }

    @Test
    @DisplayName("the bar's labels are the view's texts")
    void barTexts() {
        MarkdownView view = new MarkdownView();
        ImageWindowContent content = new ImageWindowContent(view, MarkdownTestSupport.notes(view),
                new ImmediateWork(), () -> {
        });
        assertEquals("Fit", button(content, ".markdown-image-fit").getText());
        assertEquals("100%", button(content, ".markdown-image-actual-size").getText());

        view.setFitImageText("Einpassen");
        view.setActualImageSizeText("Originalgr\u00f6\u00dfe");
        assertEquals("Einpassen", button(content, ".markdown-image-fit").getText());
        assertEquals("Originalgr\u00f6\u00dfe", button(content, ".markdown-image-actual-size").getText());
    }

    @Test
    @DisplayName("the shortcut key plus the wheel zooms by 1.25 per notch around the pointer, from the fitted zoom up"
            + " to 800 %")
    void wheelZoomsAroundThePointer() {
        ImageWindowContent content = content(1.0, new ArrayList<>());
        content.show(ImageWindowRequest.of("Wide", "wide.png", png(2400, 600, RED)), () -> {
        });
        relayOut(content);
        assertEquals("25%", zoomLabel(content).getText());
        Bounds area = sceneBounds(viewer(content));
        Point2D pointer = new Point2D(area.getMinX() + 450, area.getCenterY() + 20);
        Point2D before = imagePixelAt(content, pointer);

        wheel(viewer(content), pointer, 40, true, false);
        relayOut(content);
        assertEquals("31%", zoomLabel(content).getText(), "one notch: 1.25 times");
        Point2D after = imagePixelAt(content, pointer);
        assertEquals(before.getX(), after.getX(), 2, "the image's point stays under the pointer");
        // 187 pixels high, less than the area: in its middle vertically.
        assertEquals(area.getCenterY(), sceneBounds(picture(content)).getCenterY(), 0.5);

        for (int i = 0; i < 5; i++) {
            wheel(viewer(content), pointer, 40, true, false);
        }
        relayOut(content);
        assertEquals("95%", zoomLabel(content).getText(), "six notches");
        before = imagePixelAt(content, pointer);
        wheel(viewer(content), pointer, 40, true, false);
        relayOut(content);
        after = imagePixelAt(content, pointer);
        assertEquals(before.getX(), after.getX(), 1, "the image's point stays under the pointer");
        assertEquals(before.getY(), after.getY(), 1, "the image covers the area: on both sides");

        for (int i = 0; i < 20; i++) {
            wheel(viewer(content), pointer, 40, true, false);
            relayOut(content);
        }
        assertEquals("800%", zoomLabel(content).getText(), "at most 800 %");
        for (int i = 0; i < 40; i++) {
            wheel(viewer(content), pointer, -40, true, false);
            relayOut(content);
        }
        assertEquals("25%", zoomLabel(content).getText(), "at least the fitted zoom");
        assertTrue(area.contains(sceneBounds(picture(content))), "fitted again");
    }

    @Test
    @DisplayName("a double click toggles between fitted and 100 %, keeping the clicked point under the pointer")
    void doubleClickToggles() {
        ImageWindowContent content = content(1.0, new ArrayList<>());
        content.show(ImageWindowRequest.of("Wide", "wide.png", png(2400, 600, RED)), () -> {
        });
        relayOut(content);
        Bounds area = sceneBounds(viewer(content));
        Point2D pointer = new Point2D(area.getMinX() + 400, area.getCenterY() - 10);
        Point2D before = imagePixelAt(content, pointer);

        doubleClick(viewer(content), pointer);
        relayOut(content);
        assertEquals("100%", zoomLabel(content).getText());
        Point2D after = imagePixelAt(content, pointer);
        assertEquals(before.getX(), after.getX(), 4, "the clicked point stays under the pointer");
        assertEquals(before.getY(), after.getY(), 4, "the clicked point stays under the pointer");

        doubleClick(viewer(content), pointer);
        relayOut(content);
        assertEquals("25%", zoomLabel(content).getText(), "fitted again");
    }

    @Test
    @DisplayName("a drag moves the image with a closed hand, an open hand otherwise, as far as the image covers the"
            + " area")
    void dragMoves() {
        ImageWindowContent content = content(1.0, new ArrayList<>());
        content.show(ImageWindowRequest.of("Big", "big.png", png(2400, 1800, RED)), () -> {
        });
        button(content, ".markdown-image-actual-size").fire();
        relayOut(content);
        Node viewer = viewer(content);
        assertEquals(Cursor.OPEN_HAND, viewer.getCursor());
        Bounds start = sceneBounds(picture(content));
        Bounds area = sceneBounds(viewer);
        Point2D from = new Point2D(area.getCenterX(), area.getCenterY());

        Event.fireEvent(viewer, mouse(MouseEvent.MOUSE_PRESSED, from, 1));
        assertEquals(Cursor.CLOSED_HAND, viewer.getCursor(), "a closed hand while dragging");
        Event.fireEvent(viewer, mouse(MouseEvent.MOUSE_DRAGGED, from.add(30, 20), 1));
        relayOut(content);
        Bounds moved = sceneBounds(picture(content));
        assertEquals(start.getMinX() + 30, moved.getMinX(), 1e-4);
        assertEquals(start.getMinY() + 20, moved.getMinY(), 1e-4);

        Event.fireEvent(viewer, mouse(MouseEvent.MOUSE_DRAGGED, from.add(5000, 5000), 1));
        relayOut(content);
        Event.fireEvent(viewer, mouse(MouseEvent.MOUSE_RELEASED, from.add(5000, 5000), 1));
        assertEquals(Cursor.OPEN_HAND, viewer.getCursor(), "an open hand again");
        Bounds edge = sceneBounds(picture(content));
        assertEquals(area.getMinX(), edge.getMinX(), 1e-4, "no gap at the left edge");
        assertEquals(area.getMinY(), edge.getMinY(), 1e-4, "no gap at the top edge");
    }

    @Test
    @DisplayName("the wheel scrolls the image vertically, shift plus the wheel sideways")
    void wheelScrolls() {
        ImageWindowContent content = content(1.0, new ArrayList<>());
        content.show(ImageWindowRequest.of("Big", "big.png", png(2400, 1800, RED)), () -> {
        });
        button(content, ".markdown-image-actual-size").fire();
        relayOut(content);
        Bounds start = sceneBounds(picture(content));
        Bounds area = sceneBounds(viewer(content));
        Point2D pointer = new Point2D(area.getCenterX(), area.getCenterY());

        wheel(viewer(content), pointer, -40, false, false);
        relayOut(content);
        assertEquals(start.getMinY() - 40, sceneBounds(picture(content)).getMinY(), 1e-4, "down by the wheel");
        assertEquals("100%", zoomLabel(content).getText(), "no zoom");

        // Shift plus the wheel: JavaFX delivers it as a horizontal scroll on Windows and Linux.
        Event.fireEvent(viewer(content), new ScrollEvent(ScrollEvent.SCROLL, pointer.getX(), pointer.getY(),
                pointer.getX(), pointer.getY(), true, false, false, false, false, false, -40, 0, -40, 0, 40, 40,
                ScrollEvent.HorizontalTextScrollUnits.CHARACTERS, 3, ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0,
                null));
        relayOut(content);
        assertEquals(start.getMinX() - 40, sceneBounds(picture(content)).getMinX(), 1e-4, "sideways");
    }

    @Test
    @DisplayName("Escape closes the window")
    void escapeCloses() {
        List<String> closes = new ArrayList<>();
        ImageWindowContent content = content(1.0, closes);
        content.show(ImageWindowRequest.of("Small", "small.png", png(30, 20, RED)), () -> {
        });
        button(content, ".markdown-image-fit").requestFocus();

        Event.fireEvent(button(content, ".markdown-image-fit"), new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                KeyCode.ESCAPE, false, false, false, false));
        assertEquals(List.of("close"), closes);
    }

    @Test
    @DisplayName("the window decodes the image it shows and lets it go when it shows another one or closes")
    void memory() {
        ImageWindowContent content = content(1.0, new ArrayList<>());
        List<String> shown = new ArrayList<>();
        content.show(ImageWindowRequest.of("First", "first.png", png(2400, 600, RED)), () -> shown.add("first"));
        relayOut(content);
        content.show(ImageWindowRequest.of("Second", "second.png", png(30, 20, RED)), () -> shown.add("second"));
        relayOut(content);

        assertEquals(List.of("first", "second"), shown, "each once it shows");
        assertEquals(30, picture(content).getImage().getWidth(), "the second image, the first let go");

        content.clear();
        relayOut(content);
        assertNull(picture(content).getImage(), "no image once the window has closed");
    }

    @Test
    @DisplayName("without a file, with a file JavaFX cannot decode or above the view's limit, the window shows the"
            + " view's note instead")
    void notes() {
        MarkdownView view = new MarkdownView();
        ImageWindowContent content = content(view, 1.0, new ArrayList<>());
        Label note = (Label) content.lookup(".markdown-image-note");
        assertFalse(note.isVisible(), "no note at first");

        content.show(ImageWindowRequest.of("Gone", "gone.png", null), () -> {
        });
        assertTrue(note.isVisible());
        assertEquals("Image not found", note.getText());

        content.show(ImageWindowRequest.of("Drawing", "drawing.svg", TestImages.svg()), () -> {
        });
        assertEquals("Image format not supported", note.getText());
        assertNull(picture(content).getImage());

        view.setImagePixelLimit(399);
        content.show(ImageWindowRequest.of("Big", "big.png", png(20, 20, RED)), () -> {
        });
        assertEquals("Image too large", note.getText());

        view.setImagePixelLimit(400);
        content.show(ImageWindowRequest.of("Big", "big.png", png(20, 20, RED)), () -> {
        });
        relayOut(content);
        assertFalse(note.isVisible(), "an image within the limit shows");
        assertEquals(20, picture(content).getImage().getWidth());
    }

    @Test
    @DisplayName("the window first opens as large as the image at 100 % plus its bar, at most 90 % of the screen's"
            + " visual bounds, at least large enough for its bar")
    void firstSize() {
        Rectangle2D screen = new Rectangle2D(0, 0, 1920, 1040);

        assertEquals(new Dimension2D(800, 640), ImageWindow.firstSize(800, 600, 290, 40, screen), "the image fits");
        assertEquals(new Dimension2D(1728, 936), ImageWindow.firstSize(3840, 2160, 290, 40, screen),
                "90 % of the screen");
        assertEquals(new Dimension2D(1728, 340), ImageWindow.firstSize(3840, 300, 290, 40, screen), "wide");
        assertEquals(new Dimension2D(320, 240), ImageWindow.firstSize(16, 16, 290, 40, screen), "at least 320 by 240");
        assertEquals(new Dimension2D(350, 240), ImageWindow.firstSize(16, 16, 350, 40, screen), "as wide as the bar");
    }

    @Test
    @DisplayName("with long labels, such as German ones, the window opens as wide as its bar asks to be, every label"
            + " whole")
    void firstSizeFitsLongLabels() {
        MarkdownView view = new MarkdownView();
        view.setFitImageText("Einpassen");
        view.setActualImageSizeText("Originalgr\u00f6\u00dfe");
        view.setCopyImageText("Bild kopieren");
        view.setSaveImageText("Speichern unter...");
        ImageWindowContent measured = content(view, 1.0, new ArrayList<>());
        double barWidth = measured.barWidth();
        assertTrue(barWidth > 320, () -> "the labels ask for more than the smallest window: " + barWidth);

        Dimension2D size = ImageWindow.firstSize(16, 16, barWidth, measured.barHeight(),
                new Rectangle2D(0, 0, 1920, 1040));
        assertEquals(barWidth, size.getWidth(), 1e-9, "as wide as the bar");

        ImageWindowContent content = new ImageWindowContent(view, MarkdownTestSupport.notes(view),
                new ImmediateWork(), () -> {
        });
        Scene scene = new Scene(content, size.getWidth(), size.getHeight());
        ThemeManager.apply(scene);
        content.show(ImageWindowRequest.of("Icon", "icon.png", png(16, 16, RED)), () -> {
        });
        relayOut(content);
        for (String selector : List.of(".markdown-image-fit", ".markdown-image-actual-size", ".markdown-image-copy",
                ".markdown-image-save")) {
            Button button = button(content, selector);
            assertEquals(button.getText(), ((Text) button.lookup(".text")).getText(), "the label whole");
        }
    }

    @Test
    @DisplayName("the window is a decorated stage, not modal, owned by the view's window with its icons, themed, its"
            + " root the content; built once per view, never shown here")
    void stage() throws Exception {
        MarkdownTestSupport.onFxThread(() -> {
            MarkdownView view = new MarkdownView();
            Stage owner = new Stage();
            Image icon = new Image(new ByteArrayInputStream(png(16, 16, RED)));
            owner.getIcons().add(icon);
            owner.setScene(new Scene(new StackPane(view)));
            ImageWindow window = new ImageWindow(view, MarkdownTestSupport.notes(view), new ImmediateWork());

            Stage stage = window.stage();
            assertSame(stage, window.stage(), "one stage");
            assertFalse(stage.isShowing());
            assertEquals(Modality.NONE, stage.getModality());
            assertEquals(StageStyle.DECORATED, stage.getStyle());
            assertSame(owner, stage.getOwner());
            assertEquals(List.of(icon), stage.getIcons(), "the application's stage icon");
            Parent root = stage.getScene().getRoot();
            assertTrue(root.getStyleClass().contains("markdown-image-window"));
            assertNotNull(root.lookup(".markdown-image-viewer"));
            Scene themed = new Scene(new StackPane());
            ThemeManager.apply(themed);
            assertEquals(themed.getStylesheets(), stage.getScene().getStylesheets(), "the theme");
            return null;
        });
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * The pixel of the shown image under a scene point, in the image's own pixels: those of the zoom tests' image,
     * 2400 by 600.
     */
    private static Point2D imagePixelAt(ImageWindowContent content, Point2D scenePoint) {
        Bounds shown = sceneBounds(picture(content));
        return new Point2D((scenePoint.getX() - shown.getMinX()) / shown.getWidth() * 2400,
                (scenePoint.getY() - shown.getMinY()) / shown.getHeight() * 600);
    }

    /**
     * Turns the wheel over a node by a delta, 40 pixels a notch, with or without the shortcut key and shift.
     */
    private static void wheel(Node node, Point2D at, double deltaY, boolean shortcut, boolean shift) {
        boolean mac = MarkdownTestSupport.MAC;
        Event.fireEvent(node, new ScrollEvent(ScrollEvent.SCROLL, at.getX(), at.getY(), at.getX(), at.getY(), shift,
                shortcut && !mac, false, shortcut && mac, false, false, 0, deltaY, 0, deltaY, 40, 40,
                ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.LINES,
                deltaY / 40 * 3, 0, null));
    }

    /**
     * A double click with the primary button at a scene point, as JavaFX delivers its second click.
     */
    private static void doubleClick(Node node, Point2D at) {
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_PRESSED, at, 2));
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_RELEASED, at, 2));
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_CLICKED, at, 2));
    }

    /**
     * A mouse event of the primary button at a scene point; a drag is the only event not still since the press.
     */
    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D at, int clickCount) {
        boolean buttonDown = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
        return new MouseEvent(type, at.getX(), at.getY(), at.getX(), at.getY(), MouseButton.PRIMARY, clickCount,
                false, false, false, false, buttonDown, false, false, false, false,
                type != MouseEvent.MOUSE_DRAGGED, null);
    }

    /**
     * Asserts that a coordinate in screen pixels is a whole number.
     */
    private static void assertWholeScreenPixel(double screenPixels, String what) {
        // Bounds are floats in JavaFX: a whole pixel within their precision.
        assertEquals(Math.round(screenPixels), screenPixels, 1e-4, what);
    }

    private static Button button(ImageWindowContent content, String selector) {
        return (Button) content.lookup(selector);
    }

    /**
     * The window's content in a themed scene of {@value #WIDTH} by {@value #HEIGHT} pixels that is never shown, its
     * work at once, its texts those of a view, assuming an output scale.
     *
     * @param scale  the output scale
     * @param closes where the content's requests to close the window are noted
     */
    private static ImageWindowContent content(double scale, List<String> closes) {
        return content(new MarkdownView(), scale, closes);
    }

    /**
     * The window's content as {@link #content(double, List)} makes it, of a view.
     */
    private static ImageWindowContent content(MarkdownView view, double scale, List<String> closes) {
        ImageWindowContent content = new ImageWindowContent(view, MarkdownTestSupport.notes(view),
                new ImmediateWork(), () -> closes.add("close"));
        content.setOutputScale(scale);
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

    private static Node viewer(ImageWindowContent content) {
        return content.lookup(".markdown-image-viewer");
    }

    /**
     * The viewer's image node.
     */
    private static ImageView picture(ImageWindowContent content) {
        return (ImageView) viewer(content).lookup(".image-view");
    }

    private static Label zoomLabel(ImageWindowContent content) {
        return (Label) content.lookup(".markdown-image-zoom");
    }
}
