package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.recordCopies;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * Linked images seen from outside: an image inside a link, a block or inside a sentence, follows the link on a click
 * as the link's text does, through the view's link handler.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown, its images' work runs at once on the test's thread.
 * The tests fire the mouse events JavaFX would deliver on the image's node.
 */
class MarkdownViewLinkedImagesTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("a still click with the primary button on a linked image calls the link handler with the link's address")
    @CsvSource(delimiter = '|', value = {
        "a block | [![Logo](logo.png)](https://example.com/home) | .markdown-image-block",
        "an inline image | See [![Logo](logo.png)](https://example.com/home) here. | .markdown-image-inline",
        "an inline image in link text | [Go ![Logo](logo.png) home](https://example.com/home) | .markdown-image-inline"})
    void clickFollowsTheLink(String description, String markdown, String kind) {
        MarkdownView view = viewWithImages(Map.of("logo.png", png(40, 20, RED)), 1.0);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        view.setMarkdown(markdown);
        layOut(view);

        Node image = view.lookup(kind).lookup(".markdown-image");
        assertNotNull(image, "the image shows");
        click(image, MouseButton.SECONDARY, 1, true);
        click(image, MouseButton.PRIMARY, 2, true);
        assertEquals(List.of(), followed, "no right click, no second click of a double click");

        click(image, MouseButton.PRIMARY, 1, true);
        assertEquals(List.of("https://example.com/home"), followed, "a still click follows the link");
        assertEquals(Cursor.HAND, image.getCursor(), "a hand cursor");
    }

    @Test
    @DisplayName("a press on a linked image followed by a drag follows nothing")
    void dragFollowsNothing() {
        MarkdownView view = viewWithImages(Map.of("logo.png", png(40, 20, RED)), 1.0);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        recordCopies(view);
        view.setMarkdown("See [![Logo](logo.png)](https://example.com/home) here.\n\nNext line.");
        layOut(view);
        Node image = view.lookup(".markdown-image");
        Bounds bounds = sceneBounds(image);

        Point2D from = new Point2D(bounds.getCenterX(), bounds.getCenterY());
        Point2D to = new Point2D(bounds.getCenterX() + 60, bounds.getCenterY() + 30);
        drag(image, from, to);
        // JavaFX still sends a click when the release is on the pressed node, marked as not still since the press.
        click(image, MouseButton.PRIMARY, 1, false);

        assertEquals(List.of(), followed, "a drag follows nothing");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("a linked image's placeholder and error box follow the link too")
    @CsvSource(delimiter = '|', value = {
        "a block's error box | [![Logo](missing.png)](https://example.com/home)",
        "an inline error box | See [![Logo](missing.png)](https://example.com/home) here."})
    void errorBoxFollowsTheLink(String description, String markdown) {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        view.setMarkdown(markdown);
        layOut(view);

        Node box = view.lookup(".markdown-image-error");
        click(box, MouseButton.PRIMARY, 1, true);
        assertEquals(List.of("https://example.com/home"), followed);
        assertEquals("missing.png", tooltip(box).getText(), "the error box keeps the image's address as its tooltip");
    }

    @Test
    @DisplayName("a linked inline placeholder follows the link")
    void placeholderFollowsTheLink() {
        MarkdownView view = viewWithImages(Map.of("logo.png", png(40, 20, RED)), 1.0);
        view.setImageWork(new QueuedWork());
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        view.setMarkdown("See [![Logo](logo.png)](https://example.com/home) here.");
        layOut(view);

        click(view.lookup(".markdown-image-placeholder"), MouseButton.PRIMARY, 1, true);
        assertEquals(List.of("https://example.com/home"), followed);
    }

    @Test
    @DisplayName("a linked image without a title shows the link's address as its tooltip, one with a title its title")
    void linkedImageTooltip() {
        MarkdownView view = viewWithImages(Map.of("logo.png", png(40, 20, RED)), 1.0);
        view.setMarkdown("[![Logo](logo.png)](https://example.com/home) and [![Logo](logo.png \"The logo\")](/b)\n\n"
                + "[![Logo](logo.png)](https://example.com/block)");
        layOut(view);

        List<Node> images = new ArrayList<>(view.lookupAll(".markdown-image"));
        assertEquals("https://example.com/home", tooltip(images.get(0)).getText());
        assertEquals("The logo", tooltip(images.get(1)).getText());
        assertEquals("https://example.com/block", tooltip(images.get(2)).getText());
    }

    @Test
    @DisplayName("a click on an image outside a link follows nothing, and the text of a link around an image stays a link")
    void unlinkedImageAndLinkText() {
        MarkdownView view = viewWithImages(Map.of("logo.png", png(40, 20, RED)), 1.0);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        view.setMarkdown("See ![Logo](logo.png) and [go ![Logo](logo.png) now](https://x.y).\n\n![Logo](logo.png)");
        layOut(view);

        List<Node> images = new ArrayList<>(view.lookupAll(".markdown-image"));
        click(images.get(0), MouseButton.PRIMARY, 1, true);
        click(images.get(2), MouseButton.PRIMARY, 1, true);
        assertEquals(List.of(), followed, "images outside links follow nothing");
        assertNull(tooltip(images.get(0)), "no title, no link: no tooltip");

        List<Text> linkRuns = new ArrayList<>();
        for (Node run : view.lookupAll(".markdown-link")) {
            linkRuns.add((Text) run);
        }
        assertEquals(List.of("go ", " now"), texts(linkRuns), "the link's text around its image");
        click(linkRuns.get(1), MouseButton.PRIMARY, 1, true);
        assertEquals(List.of("https://x.y"), followed);
        assertTrue(images.get(1).getCursor() == Cursor.HAND, "the image in the link has the hand cursor");
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * Fires a mouse click at a node's middle, as JavaFX delivers one after a press and release on the node.
     *
     * @param still whether the pointer stayed where it was pressed
     */
    private static void click(Node node, MouseButton button, int clickCount, boolean still) {
        Bounds bounds = sceneBounds(node);
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_CLICKED, x, y, x, y, button, clickCount,
                false, false, false, false, button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY,
                false, false, still, null));
    }

    /**
     * The tooltip installed on a node: {@code Tooltip.install} keeps it in the node's properties under this key, the
     * one place JavaFX keeps it.
     */
    private static Tooltip tooltip(Node node) {
        return (Tooltip) node.getProperties().get("javafx.scene.control.Tooltip");
    }

    /**
     * The texts of runs.
     */
    private static List<String> texts(List<Text> runs) {
        List<String> texts = new ArrayList<>();
        for (Text run : runs) {
            texts.add(run.getText());
        }
        return texts;
    }
}
