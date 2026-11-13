package org.karthu.fx.markdown;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the tests of the Markdown view's zoom, its coloured code and its images share: the toolkit started without a
 * window, the view laid out in a themed scene that is never shown, its parts found by their documented style classes,
 * positions of the scene, the documents they show, the reader's mouse and keys on the view, the runs of a coloured
 * code block read by their text and their {@code code-} classes, and a view that loads its images from a map.
 */
final class MarkdownTestSupport {

    /**
     * The width of the test scene.
     */
    static final double SCENE_WIDTH = 400;

    /**
     * The height of the test scene.
     */
    static final double SCENE_HEIGHT = 300;

    /**
     * Whether the platform's shortcut key is Command (macOS) rather than Ctrl.
     */
    static final boolean MAC = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("mac");

    private MarkdownTestSupport() {
    }

    // --- The toolkit and the scene ---------------------------------------------------------------------------------

    /**
     * Starts the JavaFX toolkit, once for all test classes. As in {@code MarkdownViewTest}: the first control is
     * created on the FX thread, which loads JavaFX's default stylesheet there and then; no stage is ever shown, so the
     * nodes may live on the test thread.
     *
     * @throws InterruptedException when the wait for the toolkit is interrupted
     */
    static void initToolkit() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(1);
        Runnable loadDefaultStylesheet = () -> {
            new Label();
            ready.countDown();
        };
        try {
            Platform.startup(loadDefaultStylesheet);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(loadDefaultStylesheet);
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    /**
     * Runs code on the JavaFX Application Thread and waits for its result, at most 10 s: for a test of the view's own
     * background work, which shows its results there.
     *
     * @param code the code
     * @param <T>  the type of its result
     * @return its result
     * @throws Exception what the code threw, or the wait's failure
     */
    static <T> T onFxThread(Callable<T> code) throws Exception {
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
     * Lays the view out on the JavaFX Application Thread again and again until a condition holds there, at most
     * 10 s: for a test of the view's own background work.
     *
     * @param view      the view, in a scene
     * @param condition the condition, checked on the JavaFX Application Thread after each layout
     * @return whether the condition held within the time
     * @throws Exception when the wait is interrupted or the condition throws
     */
    static boolean relayOutUntil(MarkdownView view, Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            boolean holds = onFxThread(() -> {
                relayOut(view);
                return condition.call();
            });
            if (holds) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    /**
     * Puts the view into a themed scene of {@value #SCENE_WIDTH} by {@value #SCENE_HEIGHT} pixels that is never shown,
     * applies CSS and lays it out.
     *
     * @param view the view
     * @return the view
     */
    static MarkdownView layOut(MarkdownView view) {
        return layOut(view, SCENE_WIDTH);
    }

    /**
     * Puts the view into a themed scene of a width that is never shown, applies CSS and lays it out.
     *
     * @param view  the view
     * @param width the scene's width
     * @return the view
     */
    static MarkdownView layOut(MarkdownView view, double width) {
        StackPane root = new StackPane(view);
        Scene scene = new Scene(root, width, SCENE_HEIGHT);
        ThemeManager.apply(scene);
        root.applyCss();
        root.layout();
        return view;
    }

    /**
     * Applies CSS to the view's scene and lays it out again, as the next pulse does.
     *
     * @param view the view
     */
    static void relayOut(MarkdownView view) {
        view.getScene().getRoot().applyCss();
        view.getScene().getRoot().layout();
    }

    /**
     * A button beside the view in its scene, where the focus can go.
     *
     * @param view the view
     * @return the button
     */
    static Button elsewhere(MarkdownView view) {
        Button button = new Button("Elsewhere");
        ((StackPane) view.getScene().getRoot()).getChildren().add(button);
        relayOut(view);
        return button;
    }

    // --- The view's parts ------------------------------------------------------------------------------------------

    /**
     * The view's own scroll pane, its first.
     *
     * @param view the view
     * @return the scroll pane
     */
    static ScrollPane scrollPane(MarkdownView view) {
        return (ScrollPane) view.lookup(".scroll-pane");
    }

    /**
     * The box of all blocks, {@code markdown-document}.
     *
     * @param view the view
     * @return the box
     */
    static Region document(MarkdownView view) {
        return (Region) view.lookup(".markdown-document");
    }

    /**
     * The flows of a style class under a node, in document order.
     *
     * @param root     where to look
     * @param selector the selector, such as {@code .markdown-paragraph}
     * @return the flows
     */
    static List<TextFlow> textFlows(Node root, String selector) {
        List<TextFlow> flows = new ArrayList<>();
        for (Node node : root.lookupAll(selector)) {
            flows.add((TextFlow) node);
        }
        return flows;
    }

    /**
     * The view's paragraphs, in document order.
     *
     * @param view the view
     * @return the paragraphs' flows
     */
    static List<TextFlow> paragraphs(MarkdownView view) {
        return textFlows(view, ".markdown-paragraph");
    }

    // --- Positions in the scene ------------------------------------------------------------------------------------

    /**
     * A node's bounds in the scene.
     *
     * @param node the node
     * @return its layout bounds in the scene
     */
    static Bounds sceneBounds(Node node) {
        return node.localToScene(node.getLayoutBounds());
    }

    /**
     * The part of the document the view shows, in the scene: its scroll pane's viewport.
     *
     * @param view the view
     * @return the viewport's bounds in the scene
     */
    static Bounds viewportBounds(MarkdownView view) {
        return sceneBounds(scrollPane(view).lookup(".viewport"));
    }

    /**
     * How far the view can scroll, in the visible area's pixels.
     *
     * @param view the view
     * @return the scroll range
     */
    static double scrollRange(MarkdownView view) {
        ScrollPane scrollPane = scrollPane(view);
        return scrollPane.getContent().getLayoutBounds().getHeight() - scrollPane.getViewportBounds().getHeight();
    }

    /**
     * Scrolls the view so that the top edge of its visible area cuts a box of the document a fraction of its height
     * down, as far as the scroll range allows.
     *
     * @param view     the view
     * @param box      the box, in the scene
     * @param fraction how far down the box the top edge cuts it
     */
    static void scrollToTop(MarkdownView view, Bounds box, double fraction) {
        double scrolled = viewportBounds(view).getMinY() - sceneBounds(document(view)).getMinY();
        double wanted = scrolled + box.getMinY() + fraction * box.getHeight() - viewportBounds(view).getMinY();
        scrollPane(view).setVvalue(Math.clamp(wanted / scrollRange(view), 0, 1));
        relayOut(view);
    }

    /**
     * The box of one character of a text flow, in the scene.
     *
     * @param flow  the flow
     * @param index the character's index in the flow's text
     * @return the character's box
     */
    static Bounds sceneCharacterBounds(TextFlow flow, int index) {
        Path shape = new Path(flow.getRangeShape(index, index + 1, false));
        shape.setStroke(null);
        shape.setFill(Color.BLACK);
        return flow.localToScene(shape.getLayoutBounds());
    }

    /**
     * A scene point a quarter of a character right of the gap before a character of a text flow, on that character's
     * line: a press there puts the caret before the character.
     *
     * @param flow  the flow
     * @param index the character's index in the flow's text
     * @return the point in the scene
     */
    static Point2D textPoint(TextFlow flow, int index) {
        Bounds character = sceneCharacterBounds(flow, index);
        return new Point2D(character.getMinX() + 0.25 * character.getWidth(), character.getCenterY());
    }

    /**
     * The node JavaFX picks for the pointer at a scene point, the target of a press or a click there and the node the
     * pointer is over for entering and leaving, found as JavaFX's picking walks the scene: a node that is invisible,
     * disabled or mouse transparent is passed over with its children, the children of a parent are asked from the
     * topmost down (the last of the lowest view order first), and a parent no child of which is under the point is
     * picked itself only where it contains the point: on its bounds, unless it picks on its own shape alone
     * ({@link Node#isPickOnBounds()}), where a {@link Region} without a background or a border contains no point. A
     * clip is not followed: the tests pick in the visible area.
     *
     * @param root       where to start, such as the scene's root
     * @param scenePoint the point in the scene
     * @return the deepest node picked, {@code null} if none
     */
    static Node pickAt(Node root, Point2D scenePoint) {
        if (!root.isVisible() || root.isDisable() || root.isMouseTransparent()) {
            return null;
        }
        Point2D local = root.sceneToLocal(scenePoint);
        if (!(root instanceof Parent parent)) {
            return root.contains(local) ? root : null;
        }
        if (!parent.getBoundsInLocal().contains(local)) {
            return null;
        }
        List<Node> children = new ArrayList<>(parent.getChildrenUnmodifiable());
        // JavaFX draws the children by descending view order, keeping their order among equals, and picks the last
        // drawn first.
        children.sort(Comparator.comparingDouble(Node::getViewOrder).reversed());
        for (int i = children.size() - 1; i >= 0; i--) {
            Node picked = pickAt(children.get(i), scenePoint);
            if (picked != null) {
                return picked;
            }
        }
        boolean picksItself = parent instanceof Region ? parent.contains(local) : parent.isPickOnBounds();
        return picksItself ? parent : null;
    }

    /**
     * Whether a node is another or lies inside it.
     *
     * @param node     the node, {@code null} for none
     * @param ancestor the other node
     * @return {@code true} if the node is the other node or one of its descendants
     */
    static boolean isInside(Node node, Node ancestor) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }

    /**
     * The run of a text flow that holds a character, the node JavaFX picks for a press over it.
     *
     * @param flow  the flow
     * @param index the character's index in the flow's text
     * @return the run
     */
    static Text runAt(TextFlow flow, int index) {
        int start = 0;
        Text last = null;
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                int end = start + run.getText().length();
                if (index < end) {
                    return run;
                }
                start = end;
                last = run;
            }
        }
        return last;
    }

    // --- The reader's mouse and keys -------------------------------------------------------------------------------

    /**
     * Presses the primary button over a node, moves the pointer to another point and releases it there, as JavaFX
     * delivers a drag: every event goes to the node under the press, wherever the pointer is.
     *
     * @param pressed the node under the press
     * @param from    the press's point in the scene
     * @param to      the release's point in the scene
     */
    static void drag(Node pressed, Point2D from, Point2D to) {
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_PRESSED, from));
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_DRAGGED, to));
        Event.fireEvent(pressed, mouse(MouseEvent.MOUSE_RELEASED, to));
    }

    /**
     * Presses a key together with the platform's shortcut key (Ctrl, Command on macOS) where the keyboard focus is.
     *
     * @param view the view whose scene holds the focus
     * @param key  the key
     */
    static void shortcut(MarkdownView view, KeyCode key) {
        Node focusOwner = view.getScene().getFocusOwner();
        assertTrue(focusOwner != null, "no node has the focus");
        Event.fireEvent(focusOwner, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, !MAC, false, MAC));
    }

    /**
     * Replaces the view's clipboard with a recorder, so the test sees what the view copies and the system clipboard
     * stays untouched.
     *
     * @param view the view
     * @return the texts copied from now on, in order
     */
    static List<String> recordCopies(MarkdownView view) {
        List<String> copied = new ArrayList<>();
        view.setClipboard(text -> copied.add(text));
        return copied;
    }

    /**
     * A mouse event of the primary button at a scene point; a drag is the only event not still since the press.
     */
    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D at) {
        boolean buttonDown = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
        return new MouseEvent(type, at.getX(), at.getY(), at.getX(), at.getY(), MouseButton.PRIMARY, 1, false, false,
                false, false, buttonDown, false, false, false, false, type != MouseEvent.MOUSE_DRAGGED, null);
    }

    // --- Documents -------------------------------------------------------------------------------------------------

    /**
     * Sections with a paragraph each that wraps over several lines in the test scene.
     *
     * @param count the number of sections
     * @return the Markdown text
     */
    static String longParagraphs(int count) {
        StringBuilder markdown = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            markdown.append("## Section ").append(i).append("\n\nParagraph number ").append(i).append(": ")
                    .append("words that wrap over several lines of the view ".repeat(12)).append("\n\n");
        }
        return markdown.toString();
    }

    /**
     * Sections with a short paragraph each, one line in the test scene.
     *
     * @param count the number of sections
     * @return the Markdown text
     */
    static String manyParagraphs(int count) {
        StringBuilder markdown = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            markdown.append("## Section ").append(i).append("\n\nParagraph number ").append(i).append(".\n\n");
        }
        return markdown.toString();
    }

    /**
     * Paragraphs, a code block of twelve lines, a table whose cells wrap in the test scene, and paragraphs again.
     *
     * @return the Markdown text
     */
    static String blocks() {
        return longParagraphs(6) + "```\n" + "line\n".repeat(12) + "```\n\n" + "| Name | Meaning |\n|---|---|\n"
                + ("| key | " + "a cell that wraps in a narrow column ".repeat(3) + "|\n").repeat(6) + "\n"
                + longParagraphs(6);
    }

    // --- Coloured code ---------------------------------------------------------------------------------------------

    /**
     * The style classes of a piece of each category, by the category's name: its own and those of every category its
     * fallback chain passes; none for plain code. Written out by hand, not computed from {@link CodeCategory}, so that
     * the tests hold the view to this list.
     */
    static final Map<String, List<String>> CODE_CLASSES = Map.ofEntries(
            Map.entry("comment", List.of("code-comment")),
            Map.entry("keyword", List.of("code-keyword")),
            Map.entry("string", List.of("code-string")),
            Map.entry("number", List.of("code-number")),
            Map.entry("type", List.of("code-type")),
            Map.entry("function", List.of("code-function")),
            Map.entry("variable", List.of("code-variable")),
            Map.entry("property", List.of("code-property")),
            Map.entry("tag", List.of("code-tag")),
            Map.entry("doc", List.of("code-doc", "code-comment")),
            Map.entry("annotation", List.of("code-annotation", "code-keyword")),
            Map.entry("literal", List.of("code-literal", "code-number")),
            Map.entry("constant", List.of("code-constant", "code-literal", "code-number")),
            Map.entry("escape", List.of("code-escape", "code-string")),
            Map.entry("attribute", List.of("code-attribute", "code-number")),
            Map.entry("operator", List.of("code-operator")),
            Map.entry("punctuation", List.of("code-punctuation")),
            Map.entry("heading", List.of("code-heading", "code-function")),
            Map.entry("link", List.of("code-link", "code-property")),
            Map.entry("code", List.of("code-code", "code-string")),
            Map.entry("list", List.of("code-list", "code-keyword")),
            Map.entry("quote", List.of("code-quote", "code-comment")),
            Map.entry("emphasis", List.of("code-emphasis")),
            Map.entry("plain", List.of()));

    /**
     * Renders a block of code in a language and asserts its runs, each given as its finest category's name (or
     * {@code plain}), a colon and its text: every run has {@code markdown-text}, {@code markdown-code} and exactly the
     * classes of its category. The block's fence is longer than any run of backticks in the code, so a fence inside
     * Markdown code stays code.
     *
     * @param language the fence's info string
     * @param code     the block's code
     * @param expected the runs, such as {@code keyword:int} and {@code plain: x}
     */
    static void assertCodeRuns(String language, String code, List<String> expected) {
        String fence = "```";
        while (code.contains(fence)) {
            fence = fence + "`";
        }
        MarkdownView view = layOut(new MarkdownView(fence + language + "\n" + code + "\n" + fence));
        List<String> actual = new ArrayList<>();
        for (Text run : codeRuns(view.lookup(".markdown-code-block"))) {
            actual.add(describeCodeRun(run));
        }
        assertEquals(expected, actual, "runs of " + code);
    }

    /**
     * A run of a code block as its finest category's name and its text, after asserting that its classes are exactly
     * those of that category beside the code text's.
     *
     * @param run the run
     * @return the run as {@code category:text}
     */
    static String describeCodeRun(Text run) {
        // The finest category a run carries the class of is the one with the longest chain among them.
        String category = "plain";
        for (Map.Entry<String, List<String>> entry : CODE_CLASSES.entrySet()) {
            if (!entry.getValue().isEmpty() && run.getStyleClass().contains(entry.getValue().get(0))
                    && entry.getValue().size() > CODE_CLASSES.get(category).size()) {
                category = entry.getKey();
            }
        }
        Set<String> expectedClasses = new LinkedHashSet<>(List.of("markdown-text", "markdown-code"));
        expectedClasses.addAll(CODE_CLASSES.get(category));
        assertEquals(expectedClasses, new LinkedHashSet<>(run.getStyleClass()), "classes of '" + run.getText() + "'");
        return category + ":" + run.getText();
    }

    /**
     * The runs of a code block by their text, the first of equal texts.
     *
     * @param block the code block
     * @return its runs by their text
     */
    static Map<String, Text> codeRunsByText(Node block) {
        Map<String, Text> runs = new HashMap<>();
        for (Text run : codeRuns(block)) {
            runs.putIfAbsent(run.getText(), run);
        }
        return runs;
    }

    /**
     * The runs of a code block's flow, {@code markdown-block-content}, in order: its {@code Text} children.
     *
     * @param block the code block
     * @return its runs
     */
    static List<Text> codeRuns(Node block) {
        List<Text> runs = new ArrayList<>();
        for (Node child : ((TextFlow) block.lookup(".markdown-block-content")).getChildren()) {
            if (child instanceof Text run) {
                runs.add(run);
            }
        }
        return runs;
    }

    // --- Images ----------------------------------------------------------------------------------------------------

    /**
     * A view whose images' work runs at once on the calling thread, which assumes an output scale and loads its
     * images from a map by their address, read at each load, so a test may add files to it later. A click on an image
     * opens no window: the view's window switch takes the request and drops it, until a test records them.
     *
     * @param files       the image files by their address
     * @param outputScale the output scale the view assumes
     * @return the view
     */
    static MarkdownView viewWithImages(Map<String, byte[]> files, double outputScale) {
        MarkdownView view = new MarkdownView();
        openNoWindow(view);
        view.setImageWork(new ImmediateWork());
        view.setOutputScale(outputScale);
        view.setImageLoader(address -> {
            byte[] bytes = files.get(address);
            if (bytes == null) {
                throw new IOException("No test image " + address);
            }
            return bytes;
        });
        return view;
    }

    /**
     * The notes of an image that cannot be shown and the pixel limit of a view, as the view hands them to its full size
     * window: its text properties and its limit property, followed when they change.
     *
     * @param view the view
     * @return the notes
     */
    static ImageNotes notes(MarkdownView view) {
        return new ImageNotes(view.imageMissingTextProperty(), view.imageUnsupportedTextProperty(),
                view.imageTooLargeTextProperty(), view.imagePixelLimitProperty());
    }

    /**
     * Makes the view's full size window open nothing: its window switch drops every request, so a test that clicks an
     * image never opens a window on the desktop.
     *
     * @param view the view
     */
    static void openNoWindow(MarkdownView view) {
        view.setImageWindow(request -> {
        });
    }

    /**
     * The view's image nodes, {@code markdown-image}, in document order.
     *
     * @param view the view
     * @return the image nodes
     */
    static List<ImageView> images(MarkdownView view) {
        List<ImageView> images = new ArrayList<>();
        for (Node node : view.lookupAll(".markdown-image")) {
            images.add((ImageView) node);
        }
        return images;
    }

    /**
     * The view's one image node, {@code markdown-image}.
     *
     * @param view the view
     * @return the image node
     */
    static ImageView image(MarkdownView view) {
        List<ImageView> images = images(view);
        assertEquals(1, images.size(), "one image");
        return images.get(0);
    }

    /**
     * The view's image blocks, {@code markdown-image-block}, in document order.
     *
     * @param view the view
     * @return the blocks
     */
    static List<Node> imageBlocks(MarkdownView view) {
        return new ArrayList<>(view.lookupAll(".markdown-image-block"));
    }
}
