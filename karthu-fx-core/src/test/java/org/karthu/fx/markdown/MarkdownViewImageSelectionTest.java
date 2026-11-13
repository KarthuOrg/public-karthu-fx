package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.imageBlocks;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.recordCopies;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollToTop;
import static org.karthu.fx.markdown.MarkdownTestSupport.shortcut;
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewWithImages;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.png;

/**
 * Selecting and copying over images, seen from outside: an inline image is one character of its text flow, a block
 * image takes part in the document selection as a whole, as a table does, a selected image shows the selection's
 * highlight over it, and copying gives an image's alt text where the image is.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown, its images' work runs at once on the test's thread,
 * and its copies go to a recorder through the view's clipboard switch. The tests fire the mouse events JavaFX would
 * deliver.
 */
class MarkdownViewImageSelectionTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a drag over the text after an inline image selects and copies the characters between press and pointer")
    void textAfterAnInlineImage() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(16, 12, RED)), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("See ![the icon](icon.png) here now.");
        layOut(view);
        TextFlow paragraph = paragraphs(view).get(0);

        // The flow's positions: "See " 0 to 3, the image 4, " here now." 5 to 14.
        drag(paragraph, textPoint(paragraph, 6), textPoint(paragraph, 10));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("here"), copied);
    }

    @Test
    @DisplayName("an inline image is one character for the selection: a drag over it, a double click on the word after"
            + " it, a triple click and shift plus a click copy its alt text where it is")
    void inlineImageIsOneCharacter() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(16, 12, RED)), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("See ![the icon](icon.png) here now.\n\nNext.");
        layOut(view);
        TextFlow paragraph = paragraphs(view).get(0);

        drag(paragraph, textPoint(paragraph, 1), textPoint(paragraph, 10));
        shortcut(view, KeyCode.C);
        clicks(paragraph, textPoint(paragraph, 11), 2);
        shortcut(view, KeyCode.C);
        clicks(paragraph, textPoint(paragraph, 2), 3);
        shortcut(view, KeyCode.C);
        clicks(paragraph, textPoint(paragraph, 3), 1);
        shiftClick(paragraph, textPoint(paragraph, 6));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("ee the icon here", "now", "See the icon here now.", " the icon "), copied);
    }

    @Test
    @DisplayName("a double or triple click on a picture, block or inline, selects nothing: its first click opens the"
            + " image, as the first click on a link follows it")
    void doubleAndTripleClicksOnAPictureSelectNothing() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(120, 80, RED), "icon.png", png(16, 12, RED)), 1.0);
        List<ImageWindowRequest> opened = new ArrayList<>();
        view.setImageWindow(request -> opened.add(request));
        List<String> copied = recordCopies(view);
        view.setMarkdown("![A screenshot](shot.png)\n\nSee ![the icon](icon.png) here now.");
        layOut(view);
        Node block = view.lookup(".markdown-image-block .markdown-image");
        Node inline = view.lookup(".markdown-image-inline .markdown-image");

        for (Node picture : List.of(block, inline)) {
            clicks(picture, center(picture), 2);
            shortcut(view, KeyCode.C);
            clicks(picture, center(picture), 3);
            shortcut(view, KeyCode.C);
        }

        assertEquals(List.of(), copied, "nothing selected");
        assertEquals(4, opened.size(), "the first click of each opened the image");
    }

    @Test
    @DisplayName("a double or triple click on an image's placeholder or error box selects the image: a block whole, an"
            + " inline one alone")
    void doubleAndTripleClicksOnABoxSelectTheImage() {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("![A missing shot](shot.png)\n\nSee ![the icon](icon.png) here now.");
        layOut(view);
        Node blockBox = view.lookup(".markdown-image-block .markdown-image-error");
        Node inlineBox = view.lookup(".markdown-image-inline .markdown-image-error");

        clicks(blockBox, center(blockBox), 2);
        shortcut(view, KeyCode.C);
        clicks(blockBox, center(blockBox), 3);
        shortcut(view, KeyCode.C);
        clicks(inlineBox, center(inlineBox), 2);
        shortcut(view, KeyCode.C);

        view.setImageWork(new QueuedWork());
        view.setMarkdown("![A shot](shot.png)");
        relayOut(view);
        Node placeholder = view.lookup(".markdown-image-placeholder");
        clicks(placeholder, center(placeholder), 2);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("A missing shot", "A missing shot", "the icon", "A shot"), copied);
    }

    @Test
    @DisplayName("the selected characters after an inline image show on the highlight where the flow's characters are,"
            + " also after the image arrives and changes its size")
    void highlightedCharactersAfterAnInlineImage() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(40, 12, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("See ![the icon](icon.png) here now.");
        layOut(view);
        TextFlow paragraph = paragraphs(view).get(0);

        drag(paragraph, textPoint(paragraph, 1), textPoint(paragraph, 13));
        relayOut(view);
        assertCharactersShownInPlace(paragraph, "with the placeholder");

        work.runAll();
        relayOut(view);
        assertEquals(40, view.lookup(".markdown-image").getLayoutBounds().getWidth(), "the image arrived");
        assertCharactersShownInPlace(paragraph, "with the image");
    }

    @Test
    @DisplayName("a drag over a block image takes it whole and copies its alt text as a block of its own, separated"
            + " like a paragraph, forwards and backwards")
    void dragOverABlockImage() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(120, 80, RED)), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("Above the image.\n\n![A screenshot](shot.png)\n\nBelow the image.");
        layOut(view);
        TextFlow above = paragraphs(view).get(0);
        TextFlow below = paragraphs(view).get(1);

        drag(above, textPoint(above, 6), textPoint(below, 5));
        shortcut(view, KeyCode.C);
        drag(below, textPoint(below, 5), textPoint(above, 6));
        shortcut(view, KeyCode.C);

        String expected = "the image.\n\nA screenshot\n\nBelow";
        assertEquals(List.of(expected, expected), copied);
    }

    @Test
    @DisplayName("a drag that ends in a block image's upper half leaves it out, one that ends in its lower half takes"
            + " it, whichever way it goes")
    void halfRule() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(120, 80, RED)), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("Above.\n\n![A screenshot](shot.png)\n\nBelow.");
        layOut(view);
        TextFlow above = paragraphs(view).get(0);
        TextFlow below = paragraphs(view).get(1);
        Bounds image = sceneBounds(view.lookup(".markdown-image"));
        Point2D upper = new Point2D(image.getCenterX(), image.getMinY() + 0.25 * image.getHeight());
        Point2D lower = new Point2D(image.getCenterX(), image.getMinY() + 0.75 * image.getHeight());

        drag(above, textPoint(above, 0), upper);
        shortcut(view, KeyCode.C);
        drag(above, textPoint(above, 0), lower);
        shortcut(view, KeyCode.C);
        drag(below, textPoint(below, 5), lower);
        shortcut(view, KeyCode.C);
        drag(below, textPoint(below, 5), upper);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("Above.", "Above.\n\nA screenshot", "Below", "A screenshot\n\nBelow"), copied);
    }

    @Test
    @DisplayName("a press on a block image starts a drag selection as a press between blocks does, and shift plus a"
            + " click on it extends the selection by the half rule")
    void pressOnABlockImage() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(120, 80, RED)), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("Above.\n\n![A screenshot](shot.png)\n\nBelow.");
        layOut(view);
        TextFlow below = paragraphs(view).get(1);
        Node image = view.lookup(".markdown-image");
        Bounds bounds = sceneBounds(image);
        Point2D upper = new Point2D(bounds.getCenterX(), bounds.getMinY() + 0.25 * bounds.getHeight());
        Point2D lower = new Point2D(bounds.getCenterX(), bounds.getMinY() + 0.75 * bounds.getHeight());

        drag(image, upper, textPoint(below, 5));
        shortcut(view, KeyCode.C);
        drag(image, lower, textPoint(below, 5));
        shortcut(view, KeyCode.C);
        clicks(below, textPoint(below, 5), 1);
        shiftClick(image, upper);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("A screenshot\n\nBelow", "Below", "A screenshot\n\nBelow"), copied);
    }

    @Test
    @DisplayName("a press on a linked block image followed by a drag selects and follows nothing")
    void dragFromALinkedBlockImage() {
        MarkdownView view = viewWithImages(Map.of("logo.png", png(120, 80, RED)), 1.0);
        List<String> copied = recordCopies(view);
        List<String> followed = new ArrayList<>();
        view.setOnLinkAction(address -> followed.add(address));
        view.setMarkdown("[![Logo](logo.png)](https://example.com)\n\nBelow.");
        layOut(view);
        TextFlow below = paragraphs(view).get(0);
        Node image = view.lookup(".markdown-image");
        Bounds bounds = sceneBounds(image);
        Point2D upper = new Point2D(bounds.getCenterX(), bounds.getMinY() + 0.25 * bounds.getHeight());

        drag(image, upper, textPoint(below, 5));
        // JavaFX still sends a click when the release is on the pressed node, marked as not still since the press.
        Event.fireEvent(image, new MouseEvent(MouseEvent.MOUSE_CLICKED, upper.getX(), upper.getY(), upper.getX(),
                upper.getY(), MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false,
                false, null));
        shortcut(view, KeyCode.C);

        assertEquals(List.of(), followed, "a drag follows nothing");
        assertEquals(List.of("Logo\n\nBelow"), copied);
    }

    @Test
    @DisplayName("a block image cut by the top edge that changes its size keeps the text below it in place: taking part"
            + " in the selection does not make it the reader's place")
    void blockImageAtTheTopEdgeKeepsTheTextBelow() {
        Map<String, byte[]> files = new ConcurrentHashMap<>(Map.of("shot.png", png(200, 100, RED)));
        MarkdownView view = viewWithImages(files, 1.0);
        view.setMarkdown(longParagraphs(2) + "![A shot](shot.png)\n\n" + longParagraphs(6));
        layOut(view);
        Node block = imageBlocks(view).get(0);
        scrollToTop(view, sceneBounds(block), 0.5);
        TextFlow below = paragraphs(view).get(2);
        double belowBefore = sceneBounds(below).getMinY();

        files.put("shot.png", png(200, 180, RED));
        view.setImageLoader(TestImages.serving(files));
        relayOut(view);

        assertEquals(180, sceneBounds(view.lookup(".markdown-image")).getHeight(), "the new size");
        assertEquals(belowBefore, sceneBounds(below).getMinY(), 1.0, "the text below in place");
    }

    @Test
    @DisplayName("a selected block image shows the selection's highlight over it, see-through, as large as the picture;"
            + " a plain click clears it")
    void highlightOverABlockImage() {
        MarkdownView view = viewWithImages(Map.of("shot.png", png(120, 80, RED)), 1.0);
        view.setMarkdown("Above.\n\n![A screenshot](shot.png)\n\nBelow.");
        layOut(view);
        TextFlow above = paragraphs(view).get(0);
        TextFlow below = paragraphs(view).get(1);
        Parent block = (Parent) view.lookup(".markdown-image-block");
        Node picture = view.lookup(".markdown-image");

        drag(above, textPoint(above, 2), textPoint(below, 2));
        relayOut(view);

        Path highlight = imageHighlight(block);
        assertTrue(isShown(highlight), "the highlight shows");
        assertEquals(themeColor(view, "-fx-selection-color"), highlight.getFill(), "the selection's colour");
        assertTrue(highlight.getOpacity() > 0 && highlight.getOpacity() < 1, "see-through, so the picture shows");
        assertTrue(block.getChildrenUnmodifiable().indexOf(highlight)
                > block.getChildrenUnmodifiable().indexOf(picture), "over the picture");
        assertBoundsEqual(sceneBounds(picture), sceneBounds(highlight), "as large as the picture");

        clicks(above, textPoint(above, 1), 1);
        relayOut(view);
        assertFalse(isShown(highlight), "a plain click clears it");
    }

    @Test
    @DisplayName("a selected inline image shows the selection's highlight over it, see-through, which the highlight of"
            + " the characters around it leaves out")
    void highlightOverAnInlineImage() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(40, 12, RED)), 1.0);
        view.setMarkdown("See ![the icon](icon.png) here now.");
        layOut(view);
        TextFlow paragraph = paragraphs(view).get(0);
        Node inline = view.lookup(".markdown-image-inline");
        Node picture = inline.lookup(".markdown-image");

        drag(paragraph, textPoint(paragraph, 1), textPoint(paragraph, 8));
        relayOut(view);

        Path highlight = imageHighlight(inline);
        assertTrue(isShown(highlight), "the image's highlight shows");
        assertEquals(themeColor(view, "-fx-selection-color"), highlight.getFill(), "the selection's colour");
        assertTrue(highlight.getOpacity() > 0 && highlight.getOpacity() < 1, "see-through, so the picture shows");
        assertBoundsEqual(sceneBounds(picture), sceneBounds(highlight), "as large as the picture");
        Path characters = null;
        for (Node node : paragraph.lookupAll(".markdown-selection")) {
            if (node != highlight) {
                characters = (Path) node;
            }
        }
        assertNotNull(characters, "the characters' highlight");
        Bounds image = sceneBounds(picture);
        assertFalse(characters.contains(characters.sceneToLocal(image.getCenterX(), image.getCenterY())),
                "the characters' highlight leaves the image out");
        Bounds selected = sceneCharacterBounds(paragraph, 6);
        assertTrue(characters.contains(characters.sceneToLocal(selected.getCenterX(), selected.getCenterY())),
                "and covers the characters after it");

        drag(paragraph, textPoint(paragraph, 6), textPoint(paragraph, 8));
        relayOut(view);
        assertFalse(isShown(highlight), "an image outside the selection shows none");
    }

    @Test
    @DisplayName("a block image's error box and an inline placeholder show the highlight over them when selected")
    void highlightOverBoxes() {
        MarkdownView view = viewWithImages(Map.of("icon.png", png(40, 12, RED)), 1.0);
        QueuedWork work = new QueuedWork();
        view.setImageWork(work);
        view.setMarkdown("![Missing](missing.png)\n\nSee ![the icon](icon.png) here.");
        layOut(view);
        work.runLoads();
        relayOut(view);
        Node errorBox = view.lookup(".markdown-image-block .markdown-image-error");
        Node placeholder = view.lookup(".markdown-image-inline .markdown-image-placeholder");
        assertNotNull(errorBox, "the block's error box");
        assertNotNull(placeholder, "the inline placeholder");
        TextFlow paragraph = paragraphs(view).get(0);

        clicks(paragraph, textPoint(paragraph, 1), 1);
        shortcut(view, KeyCode.A);
        relayOut(view);

        assertBoundsEqual(sceneBounds(errorBox), sceneBounds(imageHighlight(errorBox.getParent())), "the error box");
        assertBoundsEqual(sceneBounds(placeholder), sceneBounds(imageHighlight(placeholder.getParent())),
                "the placeholder");
        assertTrue(isShown(imageHighlight(errorBox.getParent())), "the error box highlighted");
        assertTrue(isShown(imageHighlight(placeholder.getParent())), "the placeholder highlighted");
    }

    @Test
    @DisplayName("inside list items and quotes an image copies with the usual prefixes, a block image as the paragraph it"
            + " stands for: in a tight list without blank lines, in a loose one and a quote set apart by them")
    void listItemsAndQuotes() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(60, 30, RED), "i.png", png(10, 10, RED)), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("- ![Item image](a.png)\n- text ![icon](i.png) more\n\n"
                + "> ![Quoted](a.png)\n>\n> quote ![icon](i.png) end\n\n"
                + "1. ![Loose](a.png)\n\n2. last");
        layOut(view);
        TextFlow paragraph = paragraphs(view).get(0);

        clicks(paragraph, textPoint(paragraph, 1), 1);
        shortcut(view, KeyCode.A);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("- Item image\n- text icon more\n\n> Quoted\n>\n> quote icon end\n\n1. Loose\n\n2. last"),
                copied);
    }

    @Test
    @DisplayName("a table cell copies an image as its alt text, without its icon: Copy, Copy with header and a selection"
            + " across the table")
    void tableCellsCopyTheAltText() {
        MarkdownView view = viewWithImages(Map.of(), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("Before.\n\n| Name | ![Picture](p.png) |\n|---|---|\n| one | ![First](a.png) |\n"
                + "| two | see ![Second](b.png) here |\n\nAfter.");
        layOut(view);
        TableView<?> table = (TableView<?>) view.lookup(".markdown-table");
        TextFlow before = paragraphs(view).get(0);
        TextFlow after = paragraphs(view).get(1);

        table.getSelectionModel().selectAll();
        List<MenuItem> menu = contextMenuAt(view, table);
        menuEntry(menu, view.getCopyText()).fire();
        menuEntry(menu, view.getCopyWithHeaderText()).fire();
        drag(before, textPoint(before, 0), textPoint(after, 5));
        shortcut(view, KeyCode.C);

        String rows = "one\tFirst\ntwo\tsee Second here";
        assertEquals(List.of(rows, "Name\tPicture\n" + rows, "Before.\n\nName\tPicture\n" + rows + "\n\nAfter"),
                copied);
    }

    @Test
    @DisplayName("Copy Markdown still copies the source with the images' Markdown unchanged, requested on an image too")
    void copyMarkdownKeepsTheSource() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(60, 30, RED)), 1.0);
        List<String> copied = recordCopies(view);
        String markdown = "Text ![icon](a.png \"Title\") here.\n\n[![Logo](a.png)](https://example.com)\n";
        view.setMarkdown(markdown);
        layOut(view);

        for (Node image : view.lookupAll(".markdown-image")) {
            menuEntry(contextMenuAt(view, image), view.getCopyMarkdownText()).fire();
        }

        assertEquals(List.of(markdown, markdown), copied);
    }

    @Test
    @DisplayName("requested on an image, block or inline, the context menu's Copy copies the text selection wherever it"
            + " lies, as in text")
    void contextMenuCopyOnAnImage() {
        MarkdownView view = viewWithImages(Map.of("a.png", png(60, 30, RED)), 1.0);
        List<String> copied = recordCopies(view);
        view.setMarkdown("![Block](a.png)\n\nSome ![icon](a.png) text.\n\nSelected words.");
        layOut(view);
        TextFlow selected = paragraphs(view).get(1);
        drag(selected, textPoint(selected, 0), textPoint(selected, 8));

        for (Node image : view.lookupAll(".markdown-image")) {
            MenuItem copy = menuEntry(contextMenuAt(view, image), view.getCopyText());
            assertFalse(copy.isDisable(), () -> "Copy enabled on " + image.getParent());
            copy.fire();
        }

        assertEquals(List.of("Selected", "Selected"), copied);
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * The entries of the view's context menu as a request at a node builds them. A test without a window cannot show
     * the menu, so it builds the entries for the node and fires them.
     */
    private static List<MenuItem> contextMenuAt(MarkdownView view, Node target) {
        view.prepareContextMenu(target);
        return scrollPane(view).getContextMenu().getItems();
    }

    /**
     * The one entry of a menu with a label.
     */
    private static MenuItem menuEntry(List<MenuItem> items, String text) {
        MenuItem found = null;
        for (MenuItem item : items) {
            if (text.equals(item.getText())) {
                found = item;
            }
        }
        assertNotNull(found, () -> "no entry " + text);
        return found;
    }

    /**
     * The highlight of an image node, block or inline: exactly one.
     */
    private static Path imageHighlight(Node image) {
        List<Node> highlights = new ArrayList<>(image.lookupAll(".markdown-selection"));
        assertEquals(1, highlights.size(), () -> "selection highlights in " + image);
        return (Path) highlights.get(0);
    }

    /**
     * Whether a node shows: it and every node above it are visible.
     */
    private static boolean isShown(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Asserts two boxes of the scene are the same, to half a pixel.
     */
    private static void assertBoundsEqual(Bounds expected, Bounds actual, String what) {
        assertEquals(expected.getMinX(), actual.getMinX(), 0.5, what + ": left");
        assertEquals(expected.getMinY(), actual.getMinY(), 0.5, what + ": top");
        assertEquals(expected.getMaxX(), actual.getMaxX(), 0.5, what + ": right");
        assertEquals(expected.getMaxY(), actual.getMaxY(), 0.5, what + ": bottom");
    }

    /**
     * The colour a theme token resolves to in the view's scene: a probe region beside the view takes the token as its
     * background, so the test does not repeat the theme's values.
     */
    private static Paint themeColor(MarkdownView view, String token) {
        Region probe = new Region();
        probe.setStyle("-fx-background-color: " + token + ";");
        StackPane root = (StackPane) view.getScene().getRoot();
        root.getChildren().add(probe);
        try {
            probe.applyCss();
            return probe.getBackground().getFills().get(0).getFill();
        } finally {
            root.getChildren().remove(probe);
        }
    }

    /**
     * Asserts that the characters a paragraph's selection layer shows on the highlight lie exactly over the
     * paragraph's own runs: each copy of a run where its run is.
     */
    private static void assertCharactersShownInPlace(TextFlow paragraph, String when) {
        List<Text> runs = new ArrayList<>();
        Parent layer = null;
        for (Node child : paragraph.getChildren()) {
            if (child instanceof Text run) {
                runs.add(run);
            } else if (!child.isManaged()) {
                layer = (Parent) child;
            }
        }
        assertNotNull(layer, "the selection's layer");
        List<Text> copies = new ArrayList<>();
        for (Node child : layer.getChildrenUnmodifiable()) {
            if (child instanceof Text copy) {
                copies.add(copy);
            }
        }
        assertEquals(runs.size(), copies.size(), "a copy per run");
        for (int i = 0; i < runs.size(); i++) {
            Bounds run = sceneBounds(runs.get(i));
            Bounds copy = sceneBounds(copies.get(i));
            String name = "'" + runs.get(i).getText() + "' " + when;
            assertEquals(run.getMinX(), copy.getMinX(), 0.5, name + ": x");
            assertEquals(run.getMinY(), copy.getMinY(), 0.5, name + ": y");
        }
    }

    /**
     * The middle of a node, in the scene.
     */
    private static Point2D center(Node node) {
        Bounds bounds = sceneBounds(node);
        return new Point2D(bounds.getCenterX(), bounds.getCenterY());
    }

    /**
     * Clicks the primary button at a point as often as given, as JavaFX delivers a double or triple click: a press, a
     * release and a click per click, each with the count of clicks so far.
     */
    private static void clicks(Node node, Point2D at, int count) {
        for (int clickCount = 1; clickCount <= count; clickCount++) {
            Event.fireEvent(node, mouse(MouseEvent.MOUSE_PRESSED, at, clickCount, false));
            Event.fireEvent(node, mouse(MouseEvent.MOUSE_RELEASED, at, clickCount, false));
            Event.fireEvent(node, mouse(MouseEvent.MOUSE_CLICKED, at, clickCount, false));
        }
    }

    /**
     * Clicks the primary button at a point with the shift key down: a press, a release and a click.
     */
    private static void shiftClick(Node node, Point2D at) {
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_PRESSED, at, 1, true));
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_RELEASED, at, 1, true));
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_CLICKED, at, 1, true));
    }

    /**
     * A mouse event of the primary button at a scene point; a drag is the only event not still since the press.
     */
    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D at, int clickCount, boolean shiftDown) {
        boolean buttonDown = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
        return new MouseEvent(type, at.getX(), at.getY(), at.getX(), at.getY(), MouseButton.PRIMARY, clickCount,
                shiftDown, false, false, false, buttonDown, false, false, false, false,
                type != MouseEvent.MOUSE_DRAGGED, null);
    }
}
