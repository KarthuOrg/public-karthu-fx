package org.karthu.fx.markdown;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.drag;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.recordCopies;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.runAt;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneCharacterBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollPane;
import static org.karthu.fx.markdown.MarkdownTestSupport.shortcut;
import static org.karthu.fx.markdown.MarkdownTestSupport.textFlows;
import static org.karthu.fx.markdown.MarkdownTestSupport.textPoint;

/**
 * GFM task list items seen from outside: their check boxes found by the documented style classes and pseudo-class,
 * where they sit beside the text, what they look like in the theme, that the reader cannot tick them, and what copying
 * and the search make of a task.
 *
 * <p>The view sits in a themed scene that is never shown; the tests apply CSS and lay it out themselves.
 */
class MarkdownViewTaskListTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a bullet task item shows a check box in place of its bullet, ticked for [x] and [X], and its text"
            + " without the marker")
    void bulletTaskItemsShowCheckBoxes() {
        MarkdownView view = layOut(new MarkdownView("- [ ] open\n- [x] done\n- [X] caps\n- plain"));

        Node list = view.lookup(".markdown-bullet-list");
        assertTrue(list.getStyleClass().contains("markdown-task-list"));
        List<Node> items = new ArrayList<>(list.lookupAll(".markdown-list-item"));
        assertEquals(4, items.size());
        List<Region> boxes = boxes(view);
        assertEquals(3, boxes.size());
        assertEquals(List.of(false, true, true), List.of(checked(boxes.get(0)), checked(boxes.get(1)),
                checked(boxes.get(2))));
        assertEquals(List.of(true, true, true, false), List.of(isTask(items.get(0)), isTask(items.get(1)),
                isTask(items.get(2)), isTask(items.get(3))));
        assertEquals(List.of(false, true, true, false), List.of(checked(items.get(0)), checked(items.get(1)),
                checked(items.get(2)), checked(items.get(3))));
        assertEquals(List.of("•"), markers(view), "only the plain item keeps its bullet");
        assertEquals(List.of("open", "done", "caps", "plain"), texts(view));
    }

    @Test
    @DisplayName("the check box sits right of the item's indent, 4 pixels from the text, which stays aligned with a"
            + " plain item's, its middle on the middle of the first line's lowercase letters")
    void checkBoxPlace() {
        MarkdownView view = layOut(new MarkdownView("- [ ] open\n- plain"));
        Region box = boxes(view).getFirst();
        List<TextFlow> paragraphs = paragraphs(view);

        Bounds boxBounds = sceneBounds(box);
        assertEquals(14, boxBounds.getWidth(), 0.01);
        assertEquals(14, boxBounds.getHeight(), 0.01);
        double textX = sceneBounds(paragraphs.get(0)).getMinX();
        assertEquals(textX, sceneBounds(paragraphs.get(1)).getMinX(), 0.01, "task and plain text aligned");
        assertEquals(textX - ListItemBox.TASK_BOX_GAP, boxBounds.getMaxX(), 0.51);
        Node item = view.lookup(".markdown-task-item");
        assertEquals(sceneBounds(item).getMinX(), boxBounds.getMinX(), 0.51, "box and gap fill the bullet's indent");
        double baseline = sceneBaseline(paragraphs.get(0));
        double fontSize = ((Text) paragraphs.get(0).getChildren().getFirst()).getFont().getSize();
        assertEquals(baseline - fontSize / 4, boxBounds.getCenterY(), 0.51);
    }

    @Test
    @DisplayName("a numbered task item shows its number and the check box side by side, the number on the text's"
            + " baseline, the box 4 pixels from the text")
    void numberedTaskItems() {
        MarkdownView view = layOut(new MarkdownView("1. [ ] build\n2. [x] test\n3. ship"));

        Node list = view.lookup(".markdown-ordered-list");
        assertTrue(list.getStyleClass().contains("markdown-task-list"));
        List<Region> boxes = boxes(view);
        assertEquals(2, boxes.size());
        assertEquals(List.of(false, true), List.of(checked(boxes.get(0)), checked(boxes.get(1))));
        assertEquals(List.of("1.", "2.", "3."), markers(view), "every item keeps its number");
        assertEquals(List.of("build", "test", "ship"), texts(view));
        List<TextFlow> paragraphs = paragraphs(view);
        List<Node> numbers = new ArrayList<>(view.lookupAll(".markdown-list-marker"));
        for (int i = 0; i < 2; i++) {
            Bounds box = sceneBounds(boxes.get(i));
            double textX = sceneBounds(paragraphs.get(i)).getMinX();
            assertEquals(textX - ListItemBox.TASK_BOX_GAP, box.getMaxX(), 0.51);
            assertEquals(box.getMinX(), sceneBounds(numbers.get(i)).getMaxX(), 0.51, "the number left of the box");
            assertEquals(sceneBaseline(paragraphs.get(i)), sceneBaseline(numbers.get(i)), 0.51);
            double fontSize = ((Text) paragraphs.get(i).getChildren().getFirst()).getFont().getSize();
            assertEquals(sceneBaseline(paragraphs.get(i)) - fontSize / 4, box.getCenterY(), 0.51);
        }
    }

    @Test
    @DisplayName("every item of a numbered task list starts its text at the same place, task or not, also when the"
            + " numbers grow wider, and the list makes room for number and box")
    void numberedTaskListAlignment() {
        MarkdownView view = layOut(new MarkdownView("9. [ ] nine\n10. [x] ten\n11. eleven\n\n"
                + "1) plain one\n2) plain two"));

        List<TextFlow> paragraphs = paragraphs(view);
        double textX = sceneBounds(paragraphs.get(0)).getMinX();
        assertEquals(textX, sceneBounds(paragraphs.get(1)).getMinX(), 0.01);
        assertEquals(textX, sceneBounds(paragraphs.get(2)).getMinX(), 0.01);
        double plainX = sceneBounds(paragraphs.get(3)).getMinX();
        assertTrue(textX > plainX, "a numbered task list indents further than a plain one");
        List<Node> numbers = new ArrayList<>(view.lookupAll(".markdown-list-marker"));
        Node taskList = view.lookup(".markdown-ordered-list.markdown-task-list");
        Bounds box = sceneBounds(boxes(view).getFirst());
        Bounds nine = sceneBounds(numbers.get(0));
        assertTrue(nine.getMinX() >= sceneBounds(taskList).getMinX() - 0.01, "1. to 9. fit within the indent");
        assertEquals(box.getMinX(), nine.getMaxX(), 0.51);
        assertTrue(sceneBounds(numbers.get(1)).getMinX() < nine.getMinX(), "10. reaches further left");
        assertEquals(nine.getMaxX(), sceneBounds(numbers.get(1)).getMaxX(), 0.51, "numbers end at the box");
    }

    @Test
    @DisplayName("numbered and bullet task lists nest in each other, with either delimiter")
    void nestedNumberedTasks() {
        MarkdownView view = layOut(new MarkdownView("- [ ] outer\n  1) [x] inner\n     - [ ] deeper\n"
                + "- plain\n  1. [ ] three"));

        assertEquals(4, boxes(view).size());
        assertEquals(List.of("1)", "\u2022", "1."), markers(view));
        assertEquals(List.of("outer", "inner", "deeper", "plain", "three"), texts(view));
    }

    @Test
    @DisplayName("a numbered task copies with its number, delimiter and task marker")
    void numberedCopy() {
        MarkdownView view = layOut(new MarkdownView("1. [ ] build\n2. [X] test\n3. ship\n\n7) [x] seven"));
        List<String> copied = recordCopies(view);
        TextFlow first = paragraphs(view).getFirst();

        click(runAt(first, 0), textPoint(first, 0));
        shortcut(view, KeyCode.A);
        shortcut(view, KeyCode.C);

        assertEquals(List.of("1. [ ] build\n2. [x] test\n3. ship\n7) [x] seven"), copied);
    }

    @Test
    @DisplayName("a wrapped task's second line starts under its text, not under the check box")
    void wrappedTaskLines() {
        MarkdownView view = layOut(new MarkdownView("- [x] " + "a long task that wraps ".repeat(12)), 240);
        TextFlow paragraph = paragraphs(view).getFirst();
        Region box = boxes(view).getFirst();

        Bounds first = sceneCharacterBounds(paragraph, 0);
        Bounds second = sceneCharacterBounds(paragraph, secondLineStart(paragraph));
        assertTrue(second.getMinY() > first.getMaxY() - 1, "the text wraps");
        assertEquals(first.getMinX(), second.getMinX(), 0.51);
        assertTrue(second.getMinX() > sceneBounds(box).getMaxX());
    }

    @Test
    @DisplayName("tasks show check boxes nested at any level, in a quote and in a loose list, and plain items keep"
            + " their bullets beside them")
    void tasksInContainers() {
        MarkdownView view = layOut(new MarkdownView("- plain\n  - [ ] sub\n    - [x] deep\n\n"
                + "> - [x] quoted\n\n- [ ] loose one\n\n- [x] loose two"));

        assertEquals(5, boxes(view).size());
        assertEquals(List.of("•"), markers(view));
        assertEquals(List.of("plain", "sub", "deep", "quoted", "loose one", "loose two"), texts(view));
        Region quoted = boxes(view).get(2);
        assertTrue(isInside(quoted, view.lookup(".markdown-quote")));
        Region plainBox = boxes(view).get(1);
        assertEquals(fill(plainBox), fill(quoted), "the same colours inside a quote");
    }

    @Test
    @DisplayName("an empty task shows its check box alone, and its row is high enough for it")
    void emptyTask() {
        MarkdownView view = layOut(new MarkdownView("- [ ]\n- after"));
        Region box = boxes(view).getFirst();
        List<Node> items = new ArrayList<>(view.lookupAll(".markdown-list-item"));

        assertEquals(List.of("after"), texts(view));
        assertTrue(sceneBounds(items.get(0)).getHeight() >= 14);
        assertTrue(sceneBounds(items.get(1)).getMinY() >= sceneBounds(box).getMaxY() - 0.01, "nothing covers the box");
    }

    @Test
    @DisplayName("other characters between the brackets, and markers not at the start, stay text with a bullet")
    void otherFormsStayText() {
        MarkdownView view = layOut(new MarkdownView("- [/] half\n- [-] dropped\n- [x](https://example.com)\n"
                + "- see [x] here"));

        assertTrue(boxes(view).isEmpty());
        assertFalse(view.lookup(".markdown-bullet-list").getStyleClass().contains("markdown-task-list"));
        assertEquals(List.of("[/] half", "[-] dropped", "x", "see [x] here"), texts(view));
    }

    @Test
    @DisplayName("the check box is drawn like the theme's check box, open and ticked")
    void themeLook() {
        MarkdownView view = new MarkdownView("- [ ] open\n- [x] done");
        CheckBox open = new CheckBox("open");
        CheckBox ticked = new CheckBox("ticked");
        ticked.setSelected(true);
        layOut(view);
        ((StackPane) view.getScene().getRoot()).getChildren().addAll(open, ticked);
        relayOut(view);
        List<Region> boxes = boxes(view);

        Region openBox = (Region) open.lookup(".box");
        Region tickedBox = (Region) ticked.lookup(".box");
        assertEquals(fill(openBox), fill(boxes.get(0)));
        assertEquals(stroke(openBox), stroke(boxes.get(0)));
        assertEquals(fill(tickedBox), fill(boxes.get(1)));
        assertEquals(stroke(tickedBox), stroke(boxes.get(1)));
        assertNotEquals(fill(boxes.get(0)), fill(boxes.get(1)));
        Region openMark = (Region) boxes.get(0).lookup(".mark");
        Region tickedMark = (Region) boxes.get(1).lookup(".mark");
        assertEquals(fill((Region) ticked.lookup(".mark")), fill(tickedMark), "the tick in the theme's colour");
        assertEquals(Color.TRANSPARENT, fill(openMark), "no tick when open");
        assertEquals(tickedMark.getShape().getClass(), ((Region) ticked.lookup(".mark")).getShape().getClass());
    }

    @Test
    @DisplayName("the reader cannot tick a box: a click changes nothing, no hand cursor, not in the tab order, and a"
            + " press on it reaches the view's selection")
    void readOnly() {
        MarkdownView view = layOut(new MarkdownView("- [ ] open item"));
        List<String> copied = recordCopies(view);
        Region box = boxes(view).getFirst();
        TextFlow paragraph = paragraphs(view).getFirst();
        Point2D middle = new Point2D(sceneBounds(box).getCenterX(), sceneBounds(box).getCenterY());

        click(box, middle);
        assertFalse(checked(box));
        assertFalse(checked(box.getParent()));
        assertEquals(scrollPane(view), view.getScene().getFocusOwner(), "the press reached the selection");
        assertFalse(box.isFocusTraversable());
        assertNotEquals(Cursor.HAND, box.getCursor());

        drag(box, middle, textPoint(paragraph, 8));
        shortcut(view, KeyCode.C);
        assertEquals(1, copied.size());
        assertTrue(copied.getFirst().endsWith("open ite"), copied::getFirst);
    }

    @Test
    @DisplayName("copying keeps the state as GFM writes it, [X] as [x], nested items two spaces further in, and a"
            + " selection inside a task's text copies no marker")
    void copy() {
        MarkdownView view = layOut(new MarkdownView("- [ ] open\n- [X] done\n- plain\n  - [x] nested\n\n"
                + "> - [ ] quoted"));
        List<String> copied = recordCopies(view);
        TextFlow first = paragraphs(view).getFirst();

        click(runAt(first, 0), textPoint(first, 0));
        shortcut(view, KeyCode.A);
        shortcut(view, KeyCode.C);
        drag(runAt(first, 1), textPoint(first, 1), textPoint(first, 3));
        shortcut(view, KeyCode.C);

        assertEquals(List.of("- [ ] open\n- [x] done\n- plain\n  - [x] nested\n> - [ ] quoted", "pe"), copied);
    }

    @Test
    @DisplayName("the search finds a task's words and never its marker")
    void search() {
        MarkdownView view = layOut(new MarkdownView("- [ ] open\n- [x] done\n- [/] half"));

        view.setSearchText("open");
        assertEquals(1, view.getSearchMatchCount());
        view.setSearchText("[x]");
        assertEquals(0, view.getSearchMatchCount());
        view.setSearchText("[ ]");
        assertEquals(0, view.getSearchMatchCount());
        view.setSearchText("[/]");
        assertEquals(1, view.getSearchMatchCount(), "a form that stays text is found");
    }

    @Test
    @DisplayName("a check box names its state, Open or Done, in its tooltip and its accessible text, and is a check box"
            + " for a screen reader, selected while done")
    void stateInWords() {
        MarkdownView view = layOut(new MarkdownView("- [ ] open\n1. [x] done"));
        List<Region> boxes = boxes(view);

        assertEquals("Open", view.getTaskOpenText());
        assertEquals("Done", view.getTaskDoneText());
        assertEquals(List.of("Open", "Done"), List.of(tooltip(boxes.get(0)), tooltip(boxes.get(1))));
        assertEquals(List.of("Open", "Done"), List.of(boxes.get(0).getAccessibleText(),
                boxes.get(1).getAccessibleText()));
        assertEquals(AccessibleRole.CHECK_BOX, boxes.get(0).getAccessibleRole());
        assertEquals(Boolean.FALSE, boxes.get(0).queryAccessibleAttribute(AccessibleAttribute.SELECTED));
        assertEquals(Boolean.TRUE, boxes.get(1).queryAccessibleAttribute(AccessibleAttribute.SELECTED));
    }

    @Test
    @DisplayName("an application translates the two texts, and a change reaches the check boxes already shown and those"
            + " of a later text")
    void translatedTexts() {
        MarkdownView view = layOut(new MarkdownView("- [ ] open\n- [x] done"));
        List<Region> shown = boxes(view);

        view.setTaskOpenText("Offen");
        view.taskDoneTextProperty().set("Erledigt");

        assertEquals(List.of("Offen", "Erledigt"), List.of(tooltip(shown.get(0)), tooltip(shown.get(1))));
        assertEquals(List.of("Offen", "Erledigt"), List.of(shown.get(0).getAccessibleText(),
                shown.get(1).getAccessibleText()));
        view.setMarkdown("1. [x] later");
        relayOut(view);
        Region later = boxes(view).getFirst();
        assertEquals("Erledigt", tooltip(later));
        assertEquals("Erledigt", later.getAccessibleText());
        assertEquals("Offen", view.getTaskOpenText());
    }

    @Test
    @DisplayName("the check box scales with the zoom")
    void zoom() {
        MarkdownView view = layOut(new MarkdownView("- [x] done"));

        view.setZoom(2.0);
        relayOut(view);

        assertEquals(28, sceneBounds(boxes(view).getFirst()).getWidth(), 0.01);
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    private static List<Region> boxes(Node root) {
        List<Region> boxes = new ArrayList<>();
        for (Node box : root.lookupAll(".markdown-task-box")) {
            boxes.add((Region) box);
        }
        return boxes;
    }

    /**
     * The text of the tooltip installed on a node.
     */
    private static String tooltip(Node node) {
        return ((Tooltip) node.getProperties().get("javafx.scene.control.Tooltip")).getText();
    }

    private static boolean checked(Node node) {
        return node.getPseudoClassStates().contains(TaskBox.CHECKED);
    }

    private static boolean isTask(Node item) {
        return item.getStyleClass().contains("markdown-task-item");
    }

    private static List<String> markers(Node root) {
        List<String> markers = new ArrayList<>();
        for (Node marker : root.lookupAll(".markdown-list-marker")) {
            markers.add(((Labeled) marker).getText());
        }
        return markers;
    }

    private static List<TextFlow> paragraphs(MarkdownView view) {
        return textFlows(view, ".markdown-paragraph");
    }

    private static List<String> texts(MarkdownView view) {
        List<String> texts = new ArrayList<>();
        for (TextFlow flow : paragraphs(view)) {
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

    private static double sceneBaseline(Node node) {
        return node.localToScene(0, node.getLayoutBounds().getMinY() + node.getBaselineOffset()).getY();
    }

    /**
     * The index of the first character on a wrapped paragraph's second line.
     */
    private static int secondLineStart(TextFlow flow) {
        double firstTop = sceneCharacterBounds(flow, 0).getMinY();
        int index = 1;
        while (sceneCharacterBounds(flow, index).getMinY() < firstTop + 1) {
            index++;
        }
        return index;
    }

    private static boolean isInside(Node node, Node ancestor) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent == ancestor) {
                return true;
            }
        }
        return false;
    }

    private static Paint fill(Region region) {
        return region.getBackground().getFills().getLast().getFill();
    }

    private static Paint stroke(Region region) {
        return region.getBorder().getStrokes().getFirst().getTopStroke();
    }

    /**
     * A click of the primary button: a press, a release and a click, as JavaFX delivers one.
     */
    private static void click(Node node, Point2D at) {
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_PRESSED, at));
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_RELEASED, at));
        Event.fireEvent(node, mouse(MouseEvent.MOUSE_CLICKED, at));
    }

    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D at) {
        return new MouseEvent(type, at.getX(), at.getY(), at.getX(), at.getY(), MouseButton.PRIMARY, 1, false, false,
                false, false, type == MouseEvent.MOUSE_PRESSED, false, false, false, false, true, null);
    }
}
