package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.document;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.isInside;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.pickAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.recordCopies;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.runAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.shortcut;
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * Opening an image at full size, seen from outside: the hover button over a block image, a click on an image and the
 * image's context menu entry hand the request to open the full size window, the image's file and its title, to the
 * view's window switch, which a test replaces with a recorder, so no window ever opens.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown, its images' work runs at once on the test's thread.
 * The tests fire the mouse events JavaFX would deliver.
 */
class MarkdownViewImageOpeningTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a still click with the primary button on a block image opens it with its file and its alt text as the"
            + " title")
    void clickOpensABlockImage() {
        byte[] file = png(40, 20, RED);
        MarkdownView view = viewWithImages(Map.of("shot.png", file), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("![The dialog](shot.png)");
        layOut(view);

        click(view.lookup(".markdown-image"), MouseButton.PRIMARY, 1, true);

        assertEquals(1, opened.size(), "one request");
        assertEquals("The dialog", opened.get(0).title());
        assertEquals("shot.png", opened.get(0).address());
        assertArrayEquals(file, opened.get(0).bytes(), "the file the view keeps");
    }

    @Test
    @DisplayName("a block image narrower than the text reacts on its picture alone: beside it the pointer is over the"
            + " document, where a click opens nothing, while the picture and its open button react")
    void narrowBlockImageReactsOnItsPictureAlone() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(120, 60, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("![Icon](icon.png)");
        layOut(view);
        Node root = view.getScene().getRoot();
        Node block = view.lookup(".markdown-image-block");
        Node picture = view.lookup(".markdown-image");
        Bounds image = sceneBounds(picture);
        Point2D on = new Point2D(image.getCenterX(), image.getCenterY());
        Point2D beside = new Point2D(image.getMaxX() + 100, image.getCenterY());
        assertTrue(sceneBounds(block).contains(beside), "the block is as wide as the text, beside the image too");

        Node besideImage = pickAt(root, beside);
        assertFalse(isInside(besideImage, block), () -> "beside the image the pointer is over " + besideImage);
        assertTrue(isInside(besideImage, document(view)), "over the document, as between blocks");
        clickAt(besideImage, beside);
        assertEquals(List.of(), opened, "a click beside the image opens nothing");

        assertSame(picture, pickAt(root, on), "over the picture the pointer is over the image");
        pointer(block, MouseEvent.MOUSE_ENTERED);
        relayOut(view);
        Button button = (Button) block.lookup(".markdown-image-open");
        Bounds buttonBounds = sceneBounds(button);
        Point2D onButton = new Point2D(buttonBounds.getCenterX(), buttonBounds.getCenterY());
        assertTrue(isInside(pickAt(root, onButton), button), "the open button lies over the picture");
        clickAt(pickAt(root, on), on);
        assertEquals(List.of("Icon"), titles(opened), "a click on the picture opens it");
    }

    @Test
    @DisplayName("in the build's tests the real full size window refuses to open: a click on an image of a view without"
            + " the window switch fails loudly instead of showing a window")
    void realWindowRefusesToOpenInTests() {
        assertEquals("true", System.getProperty("karthu.fx.test.noWindows"), "the build sets the guard");
        MarkdownView view = new MarkdownView();
        view.setImageWork(new ImmediateWork());
        view.setOutputScale(1.0);
        view.setImageLoader(TestImages.serving(Map.of("shot.png", png(40, 20, RED))));
        view.setMarkdown("![Shot](shot.png)");
        layOut(view);
        Node picture = view.lookup(".markdown-image");

        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> click(picture, MouseButton.PRIMARY, 1, true));
        assertTrue(refused.getMessage().contains("karthu.fx.test.noWindows"), refused::getMessage);
    }

    @Test
    @DisplayName("a block image's error box and placeholder react on themselves, not beside them")
    void boxesReactOnThemselves() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(40, 20, RED)), 1.0);
        view.setMarkdown("![Missing](missing.png)");
        layOut(view);
        assertBoxReactsOnItself(view, view.lookup(".markdown-image-error"));

        view.setImageWork(new QueuedWork());
        view.setMarkdown("![Shot](shot.png)");
        relayOut(view);
        assertBoxReactsOnItself(view, view.lookup(".markdown-image-placeholder"));
    }

    @Test
    @DisplayName("a still click opens an inline image; a right click, the second click of a double click and a click"
            + " that ends a drag open nothing")
    void clickRules() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(16, 16, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        recordCopies(view);
        view.setMarkdown("Press ![Save](icon.png) to save.");
        layOut(view);
        Node image = view.lookup(".markdown-image-inline").lookup(".markdown-image");

        click(image, MouseButton.SECONDARY, 1, true);
        click(image, MouseButton.PRIMARY, 2, true);
        Bounds bounds = sceneBounds(image);
        drag(image, new Point2D(bounds.getCenterX(), bounds.getCenterY()),
                new Point2D(bounds.getCenterX() + 80, bounds.getCenterY()));
        // JavaFX still sends a click when the release is on the pressed node, marked as not still since the press.
        click(image, MouseButton.PRIMARY, 1, false);
        assertEquals(List.of(), opened, "nothing opens");

        click(image, MouseButton.PRIMARY, 1, true);
        assertEquals(List.of("Save"), titles(opened), "a still click opens");
    }

    @Test
    @DisplayName("a press on a block image followed by a drag selects across it and opens nothing")
    void dragFromABlockImageSelects() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 100, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        List<String> copied = recordCopies(view);
        view.setMarkdown("![Shot](shot.png)\n\nAfter the image.");
        layOut(view);
        Node image = view.lookup(".markdown-image");
        Bounds bounds = sceneBounds(image);
        TextFlow after = paragraphs(view).get(0);

        drag(image, new Point2D(bounds.getMinX() + 2, bounds.getMinY() + 2), textPoint(after, 5));
        click(image, MouseButton.PRIMARY, 1, false);
        shortcut(view, KeyCode.C);

        assertEquals(List.of(), opened, "nothing opens");
        assertEquals(List.of("Shot\n\nAfter"), copied, "the drag selected from the image on");
    }

    @Test
    @DisplayName("a press, drag and release on the open button stay its own: the selection stays as it was")
    void pressOnTheButtonSelectsNothing() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 100, RED)), 1.0);
        recordOpens(view);
        List<String> copied = recordCopies(view);
        view.setMarkdown("Before the image.\n\n![Shot](shot.png)");
        layOut(view);
        TextFlow before = paragraphs(view).get(0);
        drag(runAt(before, 0), textPoint(before, 0), textPoint(before, 6));
        Node block = view.lookup(".markdown-image-block");
        pointer(block, MouseEvent.MOUSE_ENTERED);
        Button button = (Button) block.lookup(".markdown-image-open");
        Bounds bounds = sceneBounds(button);

        drag(button, new Point2D(bounds.getCenterX(), bounds.getCenterY()),
                new Point2D(bounds.getCenterX() - 50, bounds.getCenterY() - 300));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("Before"), copied, "the selection made before");
    }

    @Test
    @DisplayName("a click on a linked image follows the link and opens nothing")
    void linkedImageFollowsTheLink() {
        MarkdownView view = viewWithImages(Map.of("logo.png", png(40, 20, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        view.setMarkdown("[![Logo](logo.png)](https://example.com)\n\nSee [![Logo](logo.png)](https://example.com).");
        layOut(view);

        for (Node image : view.lookupAll(".markdown-image")) {
            click(image, MouseButton.PRIMARY, 1, true);
        }

        assertEquals(List.of("https://example.com", "https://example.com"), followed);
        assertEquals(List.of(), opened);
    }

    @Test
    @DisplayName("a click on a placeholder or an error box opens nothing")
    void boxesOpenNothing() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(40, 20, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("![Missing](missing.png)\n\nSee ![Missing](missing.png).");
        layOut(view);
        for (Node box : view.lookupAll(".markdown-image-error")) {
            click(box, MouseButton.PRIMARY, 1, true);
        }

        view.setImageWork(new QueuedWork());
        view.setMarkdown("![Shot](shot.png)");
        relayOut(view);
        click(view.lookup(".markdown-image-placeholder"), MouseButton.PRIMARY, 1, true);

        assertEquals(List.of(), opened);
    }

    @Test
    @DisplayName("the title is the alt text, else the file name of the address, else the address")
    void titles() {
        byte[] file = png(10, 10, RED);
        String data = "data:image/png;base64,iVBORw0KGgo";
        MarkdownView view = viewWithImages(Map.of("docs/shots/dialog.png?raw=true#top", file,
                "C:\\docs\\a b.png", file, "https://example.com/", file, data, file, "logo.png", file), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("![](docs/shots/dialog.png?raw=true#top)\n\n![](<C:\\\\docs\\\\a b.png>)\n\n"
                + "![](https://example.com/)\n\n![](" + data + ")\n\n![*Our* `logo`](logo.png)");
        layOut(view);

        for (Node image : view.lookupAll(".markdown-image")) {
            click(image, MouseButton.PRIMARY, 1, true);
        }

        assertEquals(List.of("dialog.png", "a b.png", "https://example.com/", "data:image/png", "Our logo"),
                titles(opened));
    }

    @Test
    @DisplayName("a block image shows its open button over its top right corner while the pointer is over it, and the"
            + " button opens it once")
    void hoverButton() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 100, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("![Shot](shot.png)");
        layOut(view);
        Node block = view.lookup(".markdown-image-block");
        Button button = (Button) block.lookup(".markdown-image-open");

        assertFalse(button.isVisible(), "hidden at first");
        pointer(block, MouseEvent.MOUSE_ENTERED);
        relayOut(view);
        assertTrue(button.isVisible(), "shown while the pointer is over the image");
        assertTrue(button.getStyleClass().contains("icon-button"), "the theme's icon button");
        assertEquals(BootstrapIcons.FULLSCREEN, ((FontIcon) button.getGraphic()).getIconCode());
        Bounds image = sceneBounds(view.lookup(".markdown-image"));
        Bounds corner = sceneBounds(button);
        assertTrue(image.contains(corner), () -> "over the image: " + corner + " in " + image);
        assertTrue(image.getMaxX() - corner.getMaxX() < 10 && corner.getMinY() - image.getMinY() < 10,
                () -> "in its top right corner: " + corner + " in " + image);

        button.fire();
        // JavaFX delivers the click on the button after its action: it reaches the block too.
        click(button, MouseButton.PRIMARY, 1, true);
        assertEquals(List.of("Shot"), titles(opened), "opened once");

        pointer(block, MouseEvent.MOUSE_EXITED);
        assertFalse(button.isVisible(), "hidden once the pointer leaves");
    }

    @Test
    @DisplayName("a press and a release of the primary button on the open button open the image once, as a reader's"
            + " click does, and start no selection")
    void hoverButtonOpensOnAPressAndRelease() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 100, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        List<String> copies = recordCopies(view);
        view.setMarkdown("Before\n\n![Shot](shot.png)\n\nAfter");
        layOut(view);
        Node block = view.lookup(".markdown-image-block");
        Button button = (Button) block.lookup(".markdown-image-open");
        pointer(block, MouseEvent.MOUSE_ENTERED);
        relayOut(view);

        // The whole gesture through JavaFX's dispatch, so the button's own behaviour sees it, as on the screen.
        primaryAt(button, MouseEvent.MOUSE_PRESSED, true);
        primaryAt(button, MouseEvent.MOUSE_RELEASED, false);
        primaryAt(button, MouseEvent.MOUSE_CLICKED, false);

        assertEquals(List.of("Shot"), titles(opened), "opened once");
        shortcut(view, KeyCode.C);
        assertEquals(List.of(), copies, "no selection started");
    }

    @Test
    @DisplayName("the open button's tooltip and accessible text are openImageText")
    void hoverButtonText() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 100, RED)), 1.0);
        view.setMarkdown("![Shot](shot.png)");
        layOut(view);
        Button button = (Button) view.lookup(".markdown-image-open");

        assertEquals("Open image", button.getTooltip().getText());
        assertEquals("Open image", button.getAccessibleText());
        view.setOpenImageText("Bild \u00f6ffnen");
        assertEquals("Bild \u00f6ffnen", button.getTooltip().getText());
        assertEquals("Bild \u00f6ffnen", button.getAccessibleText());
        assertEquals("Bild \u00f6ffnen", view.getOpenImageText());
    }

    @Test
    @DisplayName("inline images, placeholders and error boxes have no open button that shows")
    void noButtonElsewhere() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(16, 16, RED)), 1.0);
        view.setMarkdown("Press ![Save](icon.png) to save.\n\n![Missing](missing.png)");
        layOut(view);
        for (Node image : view.lookupAll(".markdown-image-inline, .markdown-image-block")) {
            pointer(image, MouseEvent.MOUSE_ENTERED);
        }
        assertNoVisibleButton(view);

        view.setImageWork(new QueuedWork());
        view.setMarkdown("![Shot](icon.png)");
        relayOut(view);
        pointer(view.lookup(".markdown-image-block"), MouseEvent.MOUSE_ENTERED);
        assertNoVisibleButton(view);
    }

    @Test
    @DisplayName("requested on a block or an inline image, the context menu has Open image above its other entries,"
            + " which opens the image")
    void contextMenuOpensAnImage() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(200, 100, RED), "icon.png", png(16, 16, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("![Shot](shot.png)\n\nPress ![Save](icon.png) to save, or [![Logo](icon.png)](/home).");
        layOut(view);
        List<Node> images = new ArrayList<>(view.lookupAll(".markdown-image"));

        for (Node image : images) {
            List<MenuItem> entries = contextMenuAt(view, image);
            assertEquals(List.of("Open image", "-", "Copy", "-", "Copy Markdown"), menuTexts(entries));
            assertFalse(entries.get(0).isDisable());
            entries.get(0).fire();
        }
        assertEquals(List.of("Shot", "Save", "Logo"), titles(opened), "a linked image opens too");

        assertEquals(List.of("Copy", "-", "Copy Markdown"), menuTexts(contextMenuAt(view, paragraphs(view).get(0))),
                "elsewhere no entry");
        view.setOpenImageText("Bild \u00f6ffnen");
        assertEquals("Bild \u00f6ffnen", contextMenuAt(view, images.get(0)).get(0).getText());
    }

    @Test
    @DisplayName("requested on a placeholder or an error box, Open image is there but disabled")
    void contextMenuOnBoxes() {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("![Missing](missing.png)");
        layOut(view);

        List<MenuItem> entries = contextMenuAt(view, view.lookup(".markdown-image-error"));
        assertEquals(List.of("Open image", "-", "Copy", "-", "Copy Markdown"), menuTexts(entries));
        assertTrue(entries.get(0).isDisable());
        entries.get(0).fire();
        assertEquals(List.of(), opened);
    }

    @Test
    @DisplayName("a still click on a table cell's image icon or alt text loads the image and opens it, a linked one"
            + " follows its link")
    void tableCellClick() {
        byte[] file = png(40, 20, RED);
        List<String> loads = new ArrayList<>();
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        view.setImageLoader(address -> {
            loads.add(address);
            return TestImages.serving(Map.of("logo.png", file)).load(address);
        });
        List<ImageWindowRequest> opened = recordOpens(view);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        view.setMarkdown("| Icon |\n|---|\n| ![The logo](logo.png) |\n| [![Linked](logo.png)](/home) |\n"
                + "| ![Gone](missing.png) |");
        layOut(view);
        assertEquals(List.of(), loads, "not loaded before");

        for (Node part : cellParts(view, "The logo")) {
            assertEquals(Cursor.HAND, part.getCursor(), "a hand cursor, as a link has");
            click(part, MouseButton.PRIMARY, 2, true);
            click(part, MouseButton.PRIMARY, 1, false);
            click(part, MouseButton.PRIMARY, 1, true);
        }
        assertEquals(List.of("logo.png", "logo.png"), loads, "loaded at each opening");
        assertEquals(List.of("The logo", "The logo"), titles(opened));
        assertArrayEquals(file, opened.get(0).bytes());

        for (Node part : cellParts(view, "Linked")) {
            click(part, MouseButton.PRIMARY, 1, true);
        }
        assertEquals(List.of("/home", "/home"), followed);
        assertEquals(2, opened.size(), "a linked cell image follows its link only");

        click(cellParts(view, "Gone").get(0), MouseButton.PRIMARY, 1, true);
        assertEquals("Gone", opened.get(2).title(), "a missing image opens the window, which says so");
        assertNull(opened.get(2).bytes(), "without a file");
    }

    @Test
    @DisplayName("a click on an image in a column header sorts and opens nothing, its context menu opens it")
    void headerImage() {
        MarkdownView view = viewWithImages(Map.of("h.png", png(16, 16, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("| ![Icon](h.png) |\n|---|\n| x |");
        layOut(view);

        for (Node part : cellParts(view, "Icon")) {
            click(part, MouseButton.PRIMARY, 1, true);
        }
        assertEquals(List.of(), opened, "a click on a header sorts");

        List<MenuItem> entries = contextMenuAt(view, cellParts(view, "Icon").get(0));
        assertEquals("Open image", entries.get(0).getText());
        entries.get(0).fire();
        assertEquals(List.of("Icon"), titles(opened));
    }

    @Test
    @DisplayName("requested on a table cell's image, linked or not, the context menu has Open image above the table's"
            + " entries")
    void tableCellContextMenu() {
        MarkdownView view = viewWithImages(Map.of("logo.png", png(40, 20, RED)), 1.0);
        List<ImageWindowRequest> opened = recordOpens(view);
        view.setMarkdown("| Icon |\n|---|\n| ![The logo](logo.png) |\n| [![Linked](logo.png)](/home) |");
        layOut(view);

        for (String alt : List.of("The logo", "Linked")) {
            for (Node part : cellParts(view, alt)) {
                List<MenuItem> entries = contextMenuAt(view, part);
                assertEquals(List.of("Open image", "-", "Copy", "Copy with header", "Select column", "-",
                        "Copy Markdown"), menuTexts(entries));
                entries.get(0).fire();
            }
        }
        assertEquals(List.of("The logo", "The logo", "Linked", "Linked"), titles(opened));
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * The icon and the alt text of an image in a table cell, {@code markdown-image-cell}, by the alt text.
     */
    private static List<Node> cellParts(MarkdownView view, String alt) {
        for (Node part : view.lookupAll(".markdown-image-cell")) {
            if (part instanceof Text text && !(part instanceof FontIcon)
                    && text.getText().replace('\u00a0', ' ').strip().equals(alt)) {
                List<Node> siblings = text.getParent().getChildrenUnmodifiable();
                return List.of(siblings.get(siblings.indexOf(text) - 1), text);
            }
        }
        throw new AssertionError("no image in a cell with the alt text " + alt);
    }

    /**
     * The entries of the view's context menu for a request at a node, built as the view builds them before the menu
     * shows.
     */
    private static List<MenuItem> contextMenuAt(MarkdownView view, Node target) {
        view.prepareContextMenu(target);
        return scrollPane(view).getContextMenu().getItems();
    }

    /**
     * The labels of a menu's entries, {@code -} for a separator.
     */
    private static List<String> menuTexts(List<MenuItem> items) {
        List<String> texts = new ArrayList<>();
        for (MenuItem item : items) {
            texts.add(item instanceof SeparatorMenuItem ? "-" : item.getText());
        }
        return texts;
    }

    /**
     * Asserts that no open button of the view shows.
     */
    private static void assertNoVisibleButton(MarkdownView view) {
        for (Node button : view.lookupAll(".markdown-image-open")) {
            assertFalse(button.isVisible(), "no open button shows");
        }
    }

    /**
     * Fires an event of the pointer entering or leaving a node, as JavaFX delivers it to the node itself.
     */
    private static void pointer(Node node, EventType<MouseEvent> type) {
        Bounds bounds = sceneBounds(node);
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        Event.fireEvent(node, new MouseEvent(type, x, y, x, y, MouseButton.NONE, 0, false, false, false, false,
                false, false, false, false, false, false, null));
    }

    /**
     * The titles of requests.
     */
    private static List<String> titles(List<ImageWindowRequest> requests) {
        List<String> titles = new ArrayList<>();
        for (ImageWindowRequest request : requests) {
            titles.add(request.title());
        }
        return titles;
    }

    /**
     * Replaces the view's full size window with a recorder.
     *
     * @return the requests to open, in order
     */
    private static List<ImageWindowRequest> recordOpens(MarkdownView view) {
        List<ImageWindowRequest> opened = new ArrayList<>();
        view.setImageWindow(request -> opened.add(request));
        return opened;
    }

    /**
     * Asserts that the pointer over the middle of an image's box is over the box, and beside the box over the
     * document, not the image.
     */
    private static void assertBoxReactsOnItself(MarkdownView view, Node box) {
        Node root = view.getScene().getRoot();
        Node block = view.lookup(".markdown-image-block");
        Bounds bounds = sceneBounds(box);
        Node onBox = pickAt(root, new Point2D(bounds.getCenterX(), bounds.getCenterY()));
        assertTrue(isInside(onBox, box), () -> "over the box: " + onBox);
        Point2D beside = new Point2D(bounds.getMaxX() + 100, bounds.getCenterY());
        assertTrue(sceneBounds(block).contains(beside), "the block is as wide as the text, beside the box too");
        Node besideBox = pickAt(root, beside);
        assertFalse(isInside(besideBox, block), () -> "beside the box: " + besideBox);
    }

    /**
     * Fires a still click with the primary button at a scene point on a node, as JavaFX delivers one after a press
     * and release on the node there.
     */
    /**
     * Fires a mouse event of the primary button at a node's middle through JavaFX's dispatch chain.
     *
     * @param down whether the primary button is down during the event
     */
    private static void primaryAt(Node node, EventType<MouseEvent> type, boolean down) {
        Bounds bounds = sceneBounds(node);
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        Event.fireEvent(node, new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false, false,
                down, false, false, false, false, true, null));
    }

    private static void clickAt(Node node, Point2D at) {
        Event.fireEvent(node, new MouseEvent(MouseEvent.MOUSE_CLICKED, at.getX(), at.getY(), at.getX(), at.getY(),
                MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false, false, true, null));
    }

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
}
