package org.karthu.fx.command;

import javafx.css.PseudoClass;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.karthu.fx.theme.ThemeManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * What the tests of the shortcut editor and dialog share: the editor in a themed scene of the dialog's size that is
 * never shown, its lines read through the documented style classes, and key and mouse events sent to its nodes.
 */
final class ShortcutsEditorTestSupport {

    private ShortcutsEditorTestSupport() {
    }

    /**
     * Puts an editor into a themed scene of the dialog's size, which is never shown, and lays it out, so that its
     * list has its cells.
     *
     * @param editor the editor
     * @return the editor
     */
    static ShortcutsEditor shown(ShortcutsEditor editor) {
        Scene scene = new Scene(editor, 760, 660);
        ThemeManager.apply(scene);
        layout(editor);
        return editor;
    }

    /**
     * Lays out a node and everything in it after a change, as a pulse would: twice, since the list creates and fills
     * its cells while it is laid out and the new cells take their CSS in the next pass.
     *
     * @param root the node
     */
    static void layout(Parent root) {
        for (int pass = 0; pass < 3; pass++) {
            root.applyCss();
            root.layout();
        }
    }

    /**
     * The lines of the editor's list from top to bottom, as the user reads them: a group as its name and count, a
     * command indented with its name, its context mark in brackets, a dot when modified, and its key caps or the
     * text that shows in their place.
     *
     * @param editor the editor
     * @return the lines
     */
    static List<String> lines(ShortcutsEditor editor) {
        layout(editor);
        List<String> lines = new ArrayList<>();
        for (ListCell<?> cell : cells(editor)) {
            lines.add(line(cell));
        }
        return lines;
    }

    /**
     * A cell of the list as a line, see {@link #lines(ShortcutsEditor)}.
     *
     * @param cell the cell
     * @return the line
     */
    static String line(ListCell<?> cell) {
        if (cell.getStyleClass().contains("shortcuts-group-row")) {
            return text(cell, ".shortcuts-group-name") + " " + text(cell, ".shortcuts-group-count");
        }
        StringBuilder line = new StringBuilder("  ").append(text(cell, ".shortcuts-command-name"));
        Node mark = cell.lookup(".shortcuts-context-mark");
        if (mark != null && mark.isVisible()) {
            line.append(" [").append(((Label) mark).getText()).append("]");
        }
        Node dot = cell.lookup(".shortcuts-modified-dot");
        if (dot != null && dot.isVisible()) {
            line.append(" *");
        }
        line.append(": ").append(shortcutText(cell));
        return line.toString();
    }

    /**
     * What a command's row shows in its shortcut column: the key caps in brackets, the text in place of a key, or the
     * recording field's held caps and its texts. A hint after the key caps follows them, and the one-click fix after
     * it in angle brackets: <i>[Ctrl] [O] Also used by Save &lt;Remove there&gt;</i>.
     *
     * @param cell the command's row
     * @return the shortcut column's text
     */
    static String shortcutText(ListCell<?> cell) {
        Node recorder = cell.lookup(".shortcuts-recorder");
        if (recorder != null) {
            List<String> parts = new ArrayList<>(caps(recorder));
            parts.add(text(recorder, ".shortcuts-recorder-message"));
            parts.add(text(recorder, ".shortcuts-recorder-hint"));
            return String.join(" ", parts);
        }
        Node notAssigned = cell.lookup(".shortcuts-not-assigned");
        if (notAssigned != null && notAssigned.isVisible()) {
            return ((Label) notAssigned).getText();
        }
        List<String> parts = new ArrayList<>(caps(cell));
        Node hint = cell.lookup(".shortcuts-hint");
        if (hint != null && hint.isVisible()) {
            parts.add(((Label) hint).getText());
        }
        Node fix = cell.lookup(".shortcuts-fix");
        if (fix != null && fix.isVisible()) {
            parts.add("<" + ((Labeled) fix).getText() + ">");
        }
        return String.join(" ", parts);
    }

    /**
     * The key caps inside a node, each in brackets: <i>[Ctrl] [S]</i>.
     */
    private static List<String> caps(Node node) {
        List<String> caps = new ArrayList<>();
        for (Node cap : node.lookupAll(".shortcuts-key-cap")) {
            if (cap.isVisible()) {
                caps.add("[" + ((Label) cap).getText() + "]");
            }
        }
        return caps;
    }

    /**
     * The visible cells of the editor's list that show a line, from top to bottom.
     *
     * @param editor the editor
     * @return the cells
     */
    static List<ListCell<?>> cells(ShortcutsEditor editor) {
        List<ListCell<?>> cells = new ArrayList<>();
        for (Node node : editor.lookupAll(".list-cell")) {
            if (node instanceof ListCell<?> cell && cell.isVisible() && !cell.isEmpty()
                    && (cell.getStyleClass().contains("shortcuts-row")
                    || cell.getStyleClass().contains("shortcuts-group-row"))) {
                cells.add(cell);
            }
        }
        cells.sort(Comparator.comparingDouble(Node::getLayoutY));
        return cells;
    }

    /**
     * The row of a command, found by the name it shows.
     *
     * @param editor the editor
     * @param name   the command's name
     * @return the command's row
     */
    static ListCell<?> row(ShortcutsEditor editor, String name) {
        layout(editor);
        for (ListCell<?> cell : cells(editor)) {
            if (cell.getStyleClass().contains("shortcuts-row") && name.equals(text(cell, ".shortcuts-command-name"))) {
                return cell;
            }
        }
        return fail("no row shows the command " + name);
    }

    /**
     * The line of a group, found by the name it shows.
     *
     * @param editor the editor
     * @param name   the group's name
     * @return the group's line
     */
    static ListCell<?> groupRow(ShortcutsEditor editor, String name) {
        layout(editor);
        for (ListCell<?> cell : cells(editor)) {
            if (cell.getStyleClass().contains("shortcuts-group-row")
                    && name.equals(text(cell, ".shortcuts-group-name"))) {
                return cell;
            }
        }
        return fail("no line shows the group " + name);
    }

    /**
     * Whether a node is in a pseudo-class state.
     *
     * @param node        the node
     * @param pseudoClass the pseudo-class's name
     * @return true when it is
     */
    static boolean is(Node node, String pseudoClass) {
        return node.getPseudoClassStates().contains(PseudoClass.getPseudoClass(pseudoClass));
    }

    /**
     * The text of the label a selector finds inside a node.
     *
     * @param node     the node
     * @param selector the label's selector
     * @return its text
     */
    static String text(Node node, String selector) {
        Node found = node.lookup(selector);
        assertNotNull(found, "nothing is " + selector);
        return ((Label) found).getText();
    }

    /**
     * Sends a key press to the scene's focus owner, as the platform sends it to the focused node.
     *
     * @param editor the editor whose scene has the focus owner
     * @param press  the key press
     */
    static void press(ShortcutsEditor editor, KeyEvent press) {
        Node focusOwner = editor.getScene().getFocusOwner();
        assertNotNull(focusOwner, "no node has the focus");
        focusOwner.fireEvent(press);
        layout(editor);
    }

    /**
     * A key press as the platform sends it, Ctrl being the platform's shortcut key.
     *
     * @param code  the key
     * @param text  the press's text
     * @param shift whether Shift is down
     * @param ctrl  whether Ctrl is down
     * @param alt   whether Alt is down
     * @return the key event
     */
    static KeyEvent keyPressed(KeyCode code, String text, boolean shift, boolean ctrl, boolean alt) {
        boolean mac = System.getProperty("os.name", "").startsWith("Mac");
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", text, code, shift, ctrl && !mac, alt, ctrl && mac);
    }

    /**
     * A key release as the platform sends it, Ctrl being the platform's shortcut key: the modifiers still held after
     * the release.
     *
     * @param code  the key
     * @param shift whether Shift is still down
     * @param ctrl  whether Ctrl is still down
     * @param alt   whether Alt is still down
     * @return the key event
     */
    static KeyEvent keyReleased(KeyCode code, boolean shift, boolean ctrl, boolean alt) {
        boolean mac = System.getProperty("os.name", "").startsWith("Mac");
        return new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, shift, ctrl && !mac, alt, ctrl && mac);
    }

    /**
     * Clicks a node with the primary button as often as given in a row: a press, a release and a click, each with
     * the count.
     *
     * @param node   the node
     * @param clicks 1 for a click, 2 for a double-click
     */
    static void click(Node node, int clicks) {
        assertNotNull(node, "nothing to click");
        // In the middle of the node: a list cell takes a press only inside its bounds.
        Bounds bounds = node.getLayoutBounds();
        Point2D point = node.localToScene(bounds.getCenterX(), bounds.getCenterY());
        for (int count = 1; count <= clicks; count++) {
            node.fireEvent(mouse(MouseEvent.MOUSE_PRESSED, point, count));
            node.fireEvent(mouse(MouseEvent.MOUSE_RELEASED, point, count));
            node.fireEvent(mouse(MouseEvent.MOUSE_CLICKED, point, count));
        }
    }

    /**
     * A mouse event of the primary button at a point of the scene, which the event's dispatch turns into each node's
     * own coordinates.
     */
    private static MouseEvent mouse(EventType<MouseEvent> type, Point2D scenePoint, int count) {
        boolean down = type == MouseEvent.MOUSE_PRESSED;
        return new MouseEvent(type, scenePoint.getX(), scenePoint.getY(), scenePoint.getX(), scenePoint.getY(),
                MouseButton.PRIMARY, count, false, false, false, false, down, false, false, false, false, true, null);
    }

    /**
     * Asserts the shortcut column of a command's row.
     *
     * @param editor   the editor
     * @param name     the command's name
     * @param expected what the column shows, see {@link #shortcutText(ListCell)}
     */
    static void assertShortcutColumn(ShortcutsEditor editor, String name, String expected) {
        assertEquals(expected, shortcutText(row(editor, name)), "the shortcut column of " + name);
    }

    /**
     * The editor's list.
     *
     * @param editor the editor
     * @return its list
     */
    static ListView<?> list(ShortcutsEditor editor) {
        return (ListView<?>) editor.lookup(".shortcuts-list");
    }

    /**
     * Selects a command's row with a click, as the user does; the list takes the focus.
     *
     * @param editor the editor
     * @param name   the command's name
     * @return the command's row
     */
    static ListCell<?> select(ShortcutsEditor editor, String name) {
        click(row(editor, name), 1);
        layout(editor);
        return row(editor, name);
    }

    /**
     * A button of a row, found by its style class.
     *
     * @param row        the row
     * @param styleClass the button's style class
     * @return the button
     */
    static Button button(Node row, String styleClass) {
        Node found = row.lookup("." + styleClass);
        assertNotNull(found, "the row has no " + styleClass);
        return (Button) found;
    }

    /**
     * The text of the counter in the editor's footer, its parts joined as they show.
     *
     * @param editor the editor
     * @return the counter's text
     */
    static String counter(ShortcutsEditor editor) {
        StringBuilder text = new StringBuilder();
        for (Node part : ((Parent) editor.lookup(".shortcuts-count")).getChildrenUnmodifiable()) {
            text.append(((Label) part).getText());
        }
        return text.toString();
    }
}
