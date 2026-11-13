package org.karthu.fx.markdown;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.MAC;
import static org.karthu.fx.markdown.MarkdownTestSupport.blocks;
import static org.karthu.fx.markdown.MarkdownTestSupport.elsewhere;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollRange;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollToTop;
import static org.karthu.fx.markdown.MarkdownTestSupport.viewportBounds;

/**
 * Zooming the Markdown view without the bar's buttons, seen from outside: the shortcut key plus the mouse wheel over
 * the document, and the shortcut key plus {@code +}, {@code -} and {@code 0} while the focus is inside the view.
 *
 * <p>The view sits in a themed {@link Scene} that is never shown; the tests fire wheel and key events at its parts,
 * found by their documented style classes, and apply CSS and lay it out themselves.
 */
class MarkdownViewZoomInputTest {

    /**
     * The pixels JavaFX reports per notch of a mouse wheel at the platform's default speed: its multiplier.
     */
    private static final double NOTCH = 40;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    // --- The wheel -------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{1} notch(es) from {0}% gives {2}%")
    @DisplayName("the shortcut key plus the wheel over a paragraph goes one preset per notch: up larger, down smaller,"
            + " also from a value in between, no further than the range")
    @CsvSource({
            "100, 1, 125",
            "100, -1, 75",
            "130, 1, 150",
            "130, -1, 125",
            "100, 3, 200",
            "100, -2, 50",
            "200, 1, 200",
            "50, -1, 50"
    })
    void wheelGoesOnePresetPerNotch(int from, int notches, int expected) {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        view.setZoom(from / 100.0);
        relayOut(view);
        TextFlow paragraph = paragraphs(view).getFirst();

        Event.fireEvent(paragraph, wheel(paragraph, notches * NOTCH, true));

        assertEquals(expected / 100.0, view.getZoom());
    }

    @ParameterizedTest(name = "from {0}, {1} notch(es)")
    @DisplayName("over a paragraph the line under the pointer stays under the pointer, cut as far down as before,"
            + " though the text wraps anew")
    @CsvSource({"1.0, 1", "1.0, -1", "1.5, 1", "0.75, -1"})
    void pointOverParagraphStays(double from, int notches) {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        view.setZoom(from);
        relayOut(view);
        TextFlow paragraph = paragraphs(view).get(12);
        scrollToTop(view, sceneBounds(paragraph), 0);
        Bounds shown = sceneBounds(paragraph);
        double lineY = Math.min(shown.getMinY() + 0.6 * shown.getHeight(), viewportBounds(view).getMaxY() - 20);
        int character = paragraph.getHitInfo(paragraph.sceneToLocal(shown.getMinX() + 4, lineY)).getCharIndex();
        Bounds before = sceneCharacterBounds(paragraph, character);
        // The pointer 40 % down the line of that character, the line's first.
        double pointerY = before.getMinY() + 0.4 * before.getHeight();

        Event.fireEvent(paragraph, wheelAt(shown.getCenterX(), pointerY, notches * NOTCH, NOTCH, true));
        relayOut(view);

        assertTrue(view.getZoom() != from, "zoomed");
        assertTrue(pointerY - viewportBounds(view).getMinY() > 50, "the pointer well below the top edge");
        Bounds after = sceneCharacterBounds(paragraph, character);
        assertEquals(0.4 * after.getHeight(), pointerY - after.getMinY(), 1,
                "the line is still under the pointer, cut as far down, to a pixel");
    }

    @ParameterizedTest(name = "{0}, {1} notch(es)")
    @DisplayName("over a code block or a table the view takes the wheel before the block: it zooms, consumes the event"
            + " and keeps the point of the block under the pointer")
    @CsvSource({"code, 1", "code, -1", "table, 1", "table, -1"})
    void pointOverBlockStays(String kind, int notches) {
        MarkdownView view = layOut(new MarkdownView(blocks()));
        Region block = (Region) view.lookup(kind.equals("code") ? ".markdown-code-block" : ".markdown-table");
        // The block's top 30px below the top edge of the visible area.
        scrollToTop(view, sceneBounds(block), 0);
        scrollPane(view).setVvalue(scrollPane(view).getVvalue() - 30 / scrollRange(view));
        relayOut(view);
        Node target = runNear(block, sceneBounds(block).getMinY() + 0.4 * sceneBounds(block).getHeight());
        Bounds targetBounds = sceneBounds(target);
        double pointerY = Math.min(targetBounds.getCenterY(), viewportBounds(view).getMaxY() - 10);
        double fractionBefore = fractionAt(block, pointerY);
        List<ScrollEvent> reachedRoot = scrollEventsAtRoot(view);

        Event.fireEvent(target, wheelAt(targetBounds.getCenterX(), pointerY, notches * NOTCH, NOTCH, true));
        relayOut(view);

        assertEquals(notches > 0 ? 1.25 : 0.75, view.getZoom());
        assertTrue(reachedRoot.isEmpty(), "consumed");
        assertTrue(fractionBefore > 0.1 && fractionBefore < 0.9, () -> "the pointer over the block: " + fractionBefore);
        assertEquals(fractionBefore, fractionAt(block, pointerY), 0.03, "the block's point under the pointer");
    }

    @ParameterizedTest(name = "over {0}")
    @DisplayName("without the shortcut key the wheel scrolls the document and does not zoom")
    @CsvSource({"paragraph", "code", "table"})
    void wheelWithoutShortcutScrolls(String over) {
        MarkdownView view = layOut(new MarkdownView(blocks()));
        ScrollPane scrollPane = scrollPane(view);
        Node target = over.equals("paragraph") ? paragraphs(view).getFirst()
                : runNear((Region) view.lookup(over.equals("code") ? ".markdown-code-block" : ".markdown-table"), 0);
        scrollToTop(view, sceneBounds(target), 0);
        double scrolled = scrollPane.getVvalue();

        Event.fireEvent(target, wheel(target, -NOTCH, false));
        relayOut(view);

        assertEquals(1.0, view.getZoom());
        assertTrue(scrollPane.getVvalue() > scrolled, "the document scrolls down");
    }

    @Test
    @DisplayName("smaller deltas, as a touchpad sends, add up to a notch; a movement the other way or without the"
            + " shortcut key starts the count over")
    void smallDeltasAddUp() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        TextFlow paragraph = paragraphs(view).getFirst();

        for (int i = 0; i < 3; i++) {
            Event.fireEvent(paragraph, wheel(paragraph, NOTCH / 4, true));
            assertEquals(1.0, view.getZoom(), "a quarter notch each, " + (i + 1) + " so far");
        }
        Event.fireEvent(paragraph, wheel(paragraph, NOTCH / 4, true));
        assertEquals(1.25, view.getZoom(), "the fourth quarter makes a notch");

        for (int i = 0; i < 10; i++) {
            Event.fireEvent(paragraph, wheel(paragraph, -NOTCH / 10, true));
        }
        assertEquals(1.0, view.getZoom(), "ten tenths make a notch");

        Event.fireEvent(paragraph, wheel(paragraph, NOTCH * 3 / 4, true));
        Event.fireEvent(paragraph, wheel(paragraph, -NOTCH * 3 / 4, true));
        assertEquals(1.0, view.getZoom(), "the other way starts over: three quarters down, no notch");
        Event.fireEvent(paragraph, wheel(paragraph, -NOTCH / 4, true));
        assertEquals(0.75, view.getZoom(), "and the fourth quarter down makes one");

        Event.fireEvent(paragraph, wheel(paragraph, -NOTCH * 3 / 4, true));
        Event.fireEvent(paragraph, wheel(paragraph, -NOTCH, false));
        Event.fireEvent(paragraph, wheel(paragraph, -NOTCH / 4, true));
        assertEquals(0.75, view.getZoom(), "a scroll without the shortcut key in between starts over");
    }

    @ParameterizedTest(name = "delta {0} with multiplier {1}")
    @DisplayName("a notch is the delta over the event's multiplier, whatever the wheel's speed; an event without a"
            + " multiplier counts 40 pixels as a notch")
    @CsvSource({"40, 40", "80, 80", "120, 120", "40, 1", "40, 0"})
    void notchIsTheDeltaOverTheMultiplier(double deltaY, double multiplier) {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        TextFlow paragraph = paragraphs(view).getFirst();
        Bounds bounds = sceneBounds(paragraph);

        Event.fireEvent(paragraph, wheelAt(bounds.getCenterX(), bounds.getCenterY(), deltaY, multiplier, true));

        assertEquals(1.25, view.getZoom());
    }

    @Test
    @DisplayName("while the bar is hidden the shortcut key plus the wheel scrolls as without a zoom")
    void hiddenBarWheelScrolls() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        view.setZoomBarVisible(false);
        relayOut(view);
        ScrollPane scrollPane = scrollPane(view);
        TextFlow paragraph = paragraphs(view).getFirst();

        Event.fireEvent(paragraph, wheel(paragraph, -NOTCH, true));
        relayOut(view);

        assertEquals(1.0, view.getZoom());
        assertTrue(scrollPane.getVvalue() > scrollPane.getVmin(), "the document scrolls down");
    }

    @Test
    @DisplayName("while the application has bound the zoom, the shortcut key plus the wheel sets nothing and scrolls"
            + " nothing")
    void boundZoomWheel() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        DoubleProperty source = new SimpleDoubleProperty(1.5);
        view.zoomProperty().bind(source);
        relayOut(view);
        ScrollPane scrollPane = scrollPane(view);
        TextFlow paragraph = paragraphs(view).getFirst();

        Event.fireEvent(paragraph, wheel(paragraph, -NOTCH, true));
        relayOut(view);

        assertEquals(1.5, view.getZoom());
        assertEquals(1.5, source.get());
        assertEquals(scrollPane.getVmin(), scrollPane.getVvalue(), "nothing scrolls");
    }

    // --- The shortcuts ---------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{1} ({2}) from {0}% gives {3}%")
    @DisplayName("with the focus in the view the shortcut key plus + or the keypad's plus goes one preset larger, plus"
            + " - or the keypad's minus one smaller, plus 0 or the keypad's 0 sets 100 %; the key is consumed")
    @CsvSource({
            "100, +, PLUS, 125",
            "100, +, ADD, 125",
            "130, +, PLUS, 150",
            "200, +, ADD, 200",
            "100, -, MINUS, 75",
            "100, -, SUBTRACT, 75",
            "130, -, MINUS, 125",
            "50, -, SUBTRACT, 50",
            "150, 0, DIGIT0, 100",
            "50, 0, NUMPAD0, 100",
            "130, 0, DIGIT0, 100",
            "100, '', PLUS, 125",
            "100, '', MINUS, 75",
            "150, '', DIGIT0, 100"
    })
    void shortcutsZoom(int from, String text, KeyCode code, int expected) {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        view.setZoom(from / 100.0);
        scrollPane(view).requestFocus();
        List<KeyEvent> reachedRoot = keyEventsAtRoot(view);

        press(view, text, code, false, false);

        assertEquals(expected / 100.0, view.getZoom());
        assertTrue(reachedRoot.isEmpty(), "consumed");
    }

    @Test
    @DisplayName("+ is matched by the character typed: the shortcut key plus Shift plus the key that types + with Shift"
            + " on the keyboard layout (= on a US one, 1 on a Swiss one) goes one preset larger")
    void plusTypedWithShift() {
        KeyCode key = keyTyping("+");
        Assumptions.assumeTrue(key != null, "the toolkit knows no key of the main keyboard for +");
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        scrollPane(view).requestFocus();

        // The text of a key press is the key's character without Shift, which is not + where + needs Shift.
        press(view, "", key, true, false);

        assertEquals(1.25, view.getZoom(), () -> "Shift plus " + key);
    }

    @ParameterizedTest(name = "{0} ({1}), Shift {2}, Alt {3}, shortcut {4}")
    @DisplayName("keys that do not type +, - or 0 with the shortcut key alone (and Shift) do not zoom and pass on: ="
            + " without Shift, 1, + without the shortcut key, + with Alt as AltGr sends it")
    @CsvSource({
            "=, EQUALS, false, false, true",
            "1, DIGIT1, false, false, true",
            "+, PLUS, false, false, false",
            "+, PLUS, false, true, true",
            "-, SUBTRACT, false, true, true",
            "0, DIGIT0, false, false, false"
    })
    void otherKeysDoNotZoom(String text, KeyCode code, boolean shift, boolean alt, boolean shortcut) {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        scrollPane(view).requestFocus();
        List<KeyEvent> reachedRoot = keyEventsAtRoot(view);

        Node focusOwner = view.getScene().getFocusOwner();
        Event.fireEvent(focusOwner, new KeyEvent(KeyEvent.KEY_PRESSED, "", text, code, shift, shortcut && !MAC, alt,
                shortcut && MAC));

        assertEquals(1.0, view.getZoom());
        assertEquals(1, reachedRoot.size(), "not consumed");
    }

    @ParameterizedTest(name = "focus in {0}")
    @DisplayName("the shortcuts work wherever the focus is inside the view: a code block, a table, the percent field")
    @CsvSource({"markdown-code-block", "markdown-table", "markdown-zoom-field"})
    void shortcutsWhereverTheFocusIs(String focused) {
        MarkdownView view = layOut(new MarkdownView(blocks()));
        Node part = view.lookup("." + focused);
        part.requestFocus();
        assertSame(part, view.getScene().getFocusOwner());

        press(view, "+", KeyCode.PLUS, false, false);
        assertEquals(1.25, view.getZoom());
        press(view, "-", KeyCode.SUBTRACT, false, false);
        press(view, "-", KeyCode.MINUS, false, false);
        assertEquals(0.75, view.getZoom());
        press(view, "0", KeyCode.DIGIT0, false, false);
        assertEquals(1.0, view.getZoom());
    }

    @Test
    @DisplayName("the view consumes the shortcuts, so an accelerator of the scene with the same keys does not fire"
            + " while the focus is inside the view, and fires with the focus elsewhere")
    void acceleratorDoesNotFireInTheView() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        List<String> fired = acceleratorsOf(view.getScene());
        Button elsewhere = elsewhere(view);

        scrollPane(view).requestFocus();
        press(view, "+", KeyCode.PLUS, false, false);
        press(view, "+", KeyCode.ADD, false, false);
        press(view, "-", KeyCode.SUBTRACT, false, false);
        press(view, "0", KeyCode.DIGIT0, false, false);

        assertEquals(List.of(), fired);
        assertEquals(1.0, view.getZoom());
        assertTrue(view.getScene().getFocusOwner() != elsewhere);

        elsewhere.requestFocus();
        press(view, "+", KeyCode.ADD, false, false);
        press(view, "-", KeyCode.SUBTRACT, false, false);
        press(view, "0", KeyCode.DIGIT0, false, false);

        assertEquals(List.of("in", "out", "reset"), fired);
        assertEquals(1.0, view.getZoom(), "the view does not zoom");
    }

    @Test
    @DisplayName("while the bar is hidden the shortcuts do nothing and pass on: the scene's accelerator fires")
    void hiddenBarShortcutsPassOn() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        view.setZoomBarVisible(false);
        relayOut(view);
        List<String> fired = acceleratorsOf(view.getScene());
        scrollPane(view).requestFocus();

        press(view, "+", KeyCode.ADD, false, false);
        press(view, "-", KeyCode.SUBTRACT, false, false);
        press(view, "0", KeyCode.DIGIT0, false, false);

        assertEquals(1.0, view.getZoom());
        assertEquals(List.of("in", "out", "reset"), fired);

        view.setZoom(1.5);
        press(view, "0", KeyCode.DIGIT0, false, false);

        assertEquals(1.5, view.getZoom(), "the application still sets the zoom, the reader does not");
    }

    @Test
    @DisplayName("while the application has bound the zoom, the shortcuts set nothing")
    void boundZoomShortcuts() {
        MarkdownView view = layOut(new MarkdownView(longParagraphs(30)));
        DoubleProperty source = new SimpleDoubleProperty(1.5);
        view.zoomProperty().bind(source);
        scrollPane(view).requestFocus();

        press(view, "+", KeyCode.PLUS, false, false);
        press(view, "0", KeyCode.DIGIT0, false, false);

        assertEquals(1.5, view.getZoom());
        assertEquals(1.5, source.get());
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * How far down a node a height of the scene is, as a fraction of the node's height.
     */
    private static double fractionAt(Node node, double sceneY) {
        Bounds bounds = sceneBounds(node);
        return (sceneY - bounds.getMinY()) / bounds.getHeight();
    }

    /**
     * The text run of a block nearest to a height of the scene: a code block's code, a table cell's run.
     */
    private static Node runNear(Region block, double sceneY) {
        Node nearest = null;
        double distance = Double.MAX_VALUE;
        for (Node run : block.lookupAll(".markdown-text")) {
            double runDistance = Math.abs(sceneBounds(run).getCenterY() - sceneY);
            if (runDistance < distance) {
                nearest = run;
                distance = runDistance;
            }
        }
        assertTrue(nearest != null, "a run in the block");
        return nearest;
    }

    /**
     * Presses a key where the keyboard focus is, with the platform's shortcut key down (Ctrl, Command on macOS) and
     * Shift and Alt as given: a key press's text is the character its key types.
     */
    private static void press(MarkdownView view, String text, KeyCode code, boolean shift, boolean alt) {
        Node focusOwner = view.getScene().getFocusOwner();
        assertTrue(focusOwner != null, "no node has the focus");
        Event.fireEvent(focusOwner, new KeyEvent(KeyEvent.KEY_PRESSED, "", text, code, shift, !MAC, alt, MAC));
    }

    /**
     * The key of the main keyboard that types a character with Shift on this machine's keyboard layout, as JavaFX
     * finds it for a {@link KeyCharacterCombination}; {@code null} if the toolkit knows none.
     */
    private static KeyCode keyTyping(String character) {
        KeyCharacterCombination combination = new KeyCharacterCombination(character, KeyCombination.SHORTCUT_DOWN,
                KeyCombination.SHIFT_DOWN);
        for (KeyCode code : KeyCode.values()) {
            boolean keypad = code.isKeypadKey() || code == KeyCode.ADD || code == KeyCode.SUBTRACT
                    || code == KeyCode.MULTIPLY || code == KeyCode.DIVIDE || code == KeyCode.DECIMAL
                    || code == KeyCode.SEPARATOR;
            if (keypad || code == KeyCode.UNDEFINED) {
                continue;
            }
            if (combination.match(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, true, !MAC, false, MAC))) {
                return code;
            }
        }
        return null;
    }

    /**
     * The key events that reach the scene's root from now on: those no node consumed.
     */
    private static List<KeyEvent> keyEventsAtRoot(MarkdownView view) {
        List<KeyEvent> reached = new ArrayList<>();
        view.getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, event -> reached.add(event));
        return reached;
    }

    /**
     * Gives a scene accelerators for the keypad's plus and minus and for 0 with the shortcut key, as an application's
     * menu has them.
     *
     * @return the names of the accelerators fired from now on, in order
     */
    private static List<String> acceleratorsOf(Scene scene) {
        List<String> fired = new ArrayList<>();
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ADD, KeyCombination.SHORTCUT_DOWN),
                () -> fired.add("in"));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.SHORTCUT_DOWN),
                () -> fired.add("out"));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN),
                () -> fired.add("reset"));
        return fired;
    }

    /**
     * The scroll events that reach the scene's root from now on: those no node consumed.
     */
    private static List<ScrollEvent> scrollEventsAtRoot(MarkdownView view) {
        List<ScrollEvent> reached = new ArrayList<>();
        view.getScene().getRoot().addEventHandler(ScrollEvent.SCROLL, event -> reached.add(event));
        return reached;
    }

    /**
     * A vertical wheel event over the middle of a node, as Windows sends it: the delta in pixels, the platform's
     * multiplier of {@value #NOTCH} pixels per notch; a positive delta is the wheel turned up (away from the reader).
     */
    private static ScrollEvent wheel(Node over, double deltaY, boolean shortcutDown) {
        Bounds bounds = sceneBounds(over);
        return wheelAt(bounds.getCenterX(), bounds.getCenterY(), deltaY, NOTCH, shortcutDown);
    }

    /**
     * A vertical wheel event at a point of the scene with a multiplier, see {@link #wheel}: JavaFX gives the delta as
     * the notches the platform sent times the multiplier.
     */
    private static ScrollEvent wheelAt(double sceneX, double sceneY, double deltaY, double multiplier,
            boolean shortcutDown) {
        return new ScrollEvent(ScrollEvent.SCROLL, sceneX, sceneY, sceneX, sceneY, false, shortcutDown && !MAC,
                false, shortcutDown && MAC, false, false, 0, deltaY, 0, deltaY, multiplier, multiplier,
                ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.LINES,
                multiplier > 0 ? deltaY / multiplier * 3 : 0, 0, null);
    }
}
