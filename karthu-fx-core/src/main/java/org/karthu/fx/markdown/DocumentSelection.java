package org.karthu.fx.markdown;

import javafx.event.EventTarget;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The view's selection of text across blocks, as in a web browser: one range over the document's segments
 * ({@link Segment}) in document order, from the position pressed to the position under the pointer, forwards or
 * backwards. It covers each segment it reaches from its first to its last position, and the first and last segment
 * from or to the range's ends. The segments are the text of headings and paragraphs, an inline image one character of
 * it, the text of code and source blocks, tables and block images, each of which it takes whole or not at all.
 *
 * <p><b>Gestures.</b> Handlers on the document's box take the presses that reach it, which are those in text, on images
 * and between blocks; a code or source block's text hands its presses and drags here itself, before the block's scroll
 * pane sees them ({@link #pressed(MouseEvent, Node)}, {@link #dragged(MouseEvent)}), and a table keeps its own, which
 * select cells. A press with the primary button gives the keyboard focus to the view's scroll pane, so the view's
 * shortcut keys reach the view, or to the code or source block pressed in, and anchors the selection at the position
 * under the pointer (a plain click selects nothing, so it clears the selection); the second press of a double click
 * anchors it on the word under the pointer, the third of a triple click on the segment's paragraph (a heading's or a
 * paragraph's whole text, a code line); shift plus a press extends it from the anchor. On an image's picture the
 * second and third press count as a plain one: its first click opens the image, as the first click on a link follows
 * it, so a double or triple click there selects nothing; on its placeholder or error box they select the image, as on
 * a word. A drag extends the selection
 * from the anchor to the pointer. Press and drag are consumed: a scroll pane would take the focus on the press, and
 * pan on a drag where touch input is supported. A point between two segments belongs to the end of the upper one, a
 * point above the first segment to its start, a point below the last to its end.
 *
 * <p><b>Copying.</b> The selected parts as {@link SelectionText} writes them; a selection within one code or source
 * block is its characters as written, as a text field copies them, without the prefixes of the list items and quotes
 * around the block.
 *
 * <p><b>Scrolling along.</b> While a drag holds the pointer above or below the view's visible area, the document
 * scrolls that way on every animation frame, by a speed that grows with the pointer's distance beyond the edge
 * ({@link #SCROLL_RATE} pixels per second for each pixel, up to {@link #MAX_SCROLL_DISTANCE} pixels), and the selection
 * follows to the text at the edge: a point beyond the visible area counts as one on its edge. The pointer back inside,
 * the release or a new text stops it.
 *
 * <p><b>One selection per view.</b> Each press and select-all reports the selection as the view's one selection, and
 * the view clears the block that held one before; this selection is cleared the same way when a block starts its
 * own.
 */
final class DocumentSelection implements SelectionHolder {

    /**
     * How fast a drag scrolls the document: pixels per second for each pixel the pointer is beyond the edge.
     */
    private static final double SCROLL_RATE = 10;

    /**
     * The distance beyond the edge from which a drag scrolls no faster.
     */
    private static final double MAX_SCROLL_DISTANCE = 150;

    private final Node document;
    private final ScrollPane scrollPane;
    private final Consumer<String> copyAction;
    private final Consumer<SelectionHolder> selectionStart;

    /**
     * The document's segments in document order.
     */
    private final List<Segment> segments = new ArrayList<>();

    /**
     * The range a press anchored the selection on: one position for a plain press, a word or a segment for a double
     * or triple click. A drag or shift plus a press extends the selection from it. {@code null} while nothing is
     * anchored.
     */
    private Position anchorStart;
    private Position anchorEnd;

    /**
     * The selected range, {@code null} while nothing is selected; {@code start} is never after {@code end}.
     */
    private Position start;
    private Position end;

    /**
     * Whether the primary button went down in text and is still down.
     */
    private boolean dragging;

    /**
     * Where the pointer of the drag in progress was last, in the scene.
     */
    private double pointerX;
    private double pointerY;

    /**
     * The frames that scroll the document while a drag holds the pointer beyond the edge; whether they run.
     */
    private AnimationFrames frames = AnimationFrames.pulses();
    private boolean scrolling;

    /**
     * Creates the selection of a view's document and installs its gestures.
     *
     * @param document       the box of the document's blocks
     * @param scrollPane     the view's scroll pane, which takes the keyboard focus on a press in text
     * @param copyAction     called with the selected text when the reader copies it
     * @param selectionStart called with this selection when a press or select-all starts one
     */
    DocumentSelection(Node document, ScrollPane scrollPane, Consumer<String> copyAction,
                      Consumer<SelectionHolder> selectionStart) {
        this.document = document;
        this.scrollPane = scrollPane;
        this.copyAction = copyAction;
        this.selectionStart = selectionStart;
        document.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (!takesOwnPresses(event.getTarget())) {
                pressed(event, scrollPane);
            }
        });
        document.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> dragged(event));
        document.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> released(event));
    }

    /**
     * Forgets the segments and the selection, for a new document.
     */
    void reset() {
        stopScrolling();
        segments.clear();
        anchorStart = null;
        anchorEnd = null;
        start = null;
        end = null;
        dragging = false;
    }

    /**
     * Replaces the frames a drag scrolls by. The view's tests advance frames by hand.
     *
     * @param frames the frames from now on
     */
    void setAnimationFrames(AnimationFrames frames) {
        stopScrolling();
        this.frames = frames;
    }

    /**
     * Adds the next segment of the document, in document order.
     *
     * @param segment the segment
     */
    void add(Segment segment) {
        segments.add(segment);
    }

    /**
     * The document's segments in document order, one below the other.
     *
     * @return the segments, unmodifiable
     */
    List<Segment> segments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Selects the whole document and makes it the view's one selection.
     */
    void selectAll() {
        if (segments.isEmpty()) {
            return;
        }
        selectionStart.accept(this);
        anchorStart = new Position(0, 0);
        anchorEnd = endOf(segments.size() - 1);
        select(anchorStart, anchorEnd);
    }

    /**
     * Selects the whole of one segment, as the shortcut key plus A in a code block does, and makes it the view's one
     * selection.
     *
     * @param segment one of the document's segments
     */
    void selectAll(Segment segment) {
        int index = segments.indexOf(segment);
        if (index < 0) {
            return;
        }
        selectionStart.accept(this);
        anchorStart = new Position(index, 0);
        anchorEnd = endOf(index);
        select(anchorStart, anchorEnd);
    }

    /**
     * Whether the selection covers a part of a segment: a character of a text, a table.
     *
     * @param segment a node, one of the document's segments or not
     * @return {@code true} if it is a segment with a selected part
     */
    boolean reaches(Object segment) {
        if (start == null) {
            return false;
        }
        for (int i = start.segment(); i <= end.segment(); i++) {
            if (segments.get(i) == segment) {
                return startIn(i, start) < endIn(i, end);
            }
        }
        return false;
    }

    @Override
    public void clearSelection() {
        if (start != null) {
            for (int i = start.segment(); i <= end.segment(); i++) {
                segments.get(i).select(0, 0);
            }
        }
        start = null;
        end = null;
        anchorStart = null;
        anchorEnd = null;
        dragging = false;
        stopScrolling();
    }

    @Override
    public boolean hasSelection() {
        return !selectedText().isEmpty();
    }

    @Override
    public void copySelection() {
        String text = selectedText();
        if (!text.isEmpty()) {
            copyAction.accept(text);
        }
    }

    /**
     * The selected text as the reader copies it, see {@link SelectionText}; empty without a selection.
     *
     * @return the selected text, never null
     */
    String selectedText() {
        if (start == null) {
            return "";
        }
        if (start.segment() == end.segment() && segments.get(start.segment()) instanceof MonospaceBlock block) {
            // Within one code or source block, the characters as written, as a text field copies them: no list
            // marker or quote prefix, which only a selection across blocks writes.
            return block.text(start.offset(), end.offset());
        }
        List<SelectionText.Piece> pieces = new ArrayList<>();
        for (int i = start.segment(); i <= end.segment(); i++) {
            pieces.add(new SelectionText.Piece(segments.get(i), startIn(i, start), endIn(i, end)));
        }
        return SelectionText.of(pieces);
    }

    /**
     * A press in the document's text, see the class comment: in a heading or a paragraph, between blocks (from the
     * document's handler) or in a code or source block's text (from the block's own handler, which runs before the
     * block's scroll pane sees the press).
     *
     * @param event       the press
     * @param focusTarget what takes the keyboard focus on a press with the primary button: the view's scroll pane, or
     *                    the code or source block pressed in
     */
    void pressed(MouseEvent event, Node focusTarget) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        event.consume();
        focusTarget.requestFocus();
        selectionStart.accept(this);
        Position at = positionAt(event.getSceneX(), event.getSceneY());
        if (at == null) {
            return;
        }
        dragging = true;
        if (event.isShiftDown() && anchorStart != null) {
            extendTo(at);
            return;
        }
        int under = segmentUnder(event.getSceneY());
        int clicks = onPicture(event.getTarget()) ? 1 : event.getClickCount();
        if (clicks == 2 && under >= 0) {
            Segment.Range word = segments.get(under).wordAt(event.getSceneX(), event.getSceneY());
            anchorStart = new Position(under, word.start());
            anchorEnd = new Position(under, word.end());
        } else if (clicks >= 3 && under >= 0) {
            Segment.Range paragraph = segments.get(under).paragraphAt(event.getSceneX(), event.getSceneY());
            anchorStart = new Position(under, paragraph.start());
            anchorEnd = new Position(under, paragraph.end());
        } else {
            anchorStart = at;
            anchorEnd = at;
        }
        select(anchorStart, anchorEnd);
    }

    /**
     * Whether a press happened on an image that shows its picture, whose first click opens it: the second and third
     * press of a double or triple click there select nothing.
     */
    private boolean onPicture(EventTarget target) {
        ImageNode image = Ancestors.nearest(target, ImageNode.class, document);
        return image != null && image.showsPicture();
    }

    /**
     * A drag after a press in the document's text, from the document's handler or a code or source block's: extends
     * the selection to the pointer, and scrolls along while the pointer is beyond the visible area.
     *
     * @param event the drag
     */
    void dragged(MouseEvent event) {
        if (!dragging || !event.isPrimaryButtonDown()) {
            return;
        }
        event.consume();
        pointerX = event.getSceneX();
        pointerY = event.getSceneY();
        if (beyondEdge() == 0) {
            stopScrolling();
        } else if (!scrolling) {
            scrolling = true;
            frames.start(seconds -> scrollAlong(seconds));
        }
        followPointer();
    }

    private void released(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            dragging = false;
            stopScrolling();
        }
    }

    /**
     * Extends the selection to the position at the pointer, or at the visible area's edge for a pointer beyond it.
     */
    private void followPointer() {
        Bounds visible = visibleArea();
        Position at = positionAt(pointerX, Math.clamp(pointerY, visible.getMinY(), visible.getMaxY()));
        if (at != null) {
            extendTo(at);
        }
    }

    /**
     * One frame of scrolling along with a drag beyond the edge: the document moves towards the pointer by the time
     * since the last frame, and the selection follows.
     */
    private void scrollAlong(double seconds) {
        double distance = beyondEdge();
        if (distance == 0) {
            stopScrolling();
            return;
        }
        double speed = Math.signum(distance) * Math.min(Math.abs(distance), MAX_SCROLL_DISTANCE) * SCROLL_RATE;
        // In the pixels of the scroll pane's content, not the document's: at a zoom other than 1.0 the content is the
        // document scaled, and the speed is in the visible area's pixels.
        VerticalScroll.scrollTo(scrollPane, VerticalScroll.offset(scrollPane) + speed * seconds);
        followPointer();
    }

    private void stopScrolling() {
        if (scrolling) {
            scrolling = false;
            frames.stop();
        }
    }

    /**
     * How far the pointer is above (negative) or below (positive) the visible area, 0 inside it.
     */
    private double beyondEdge() {
        Bounds visible = visibleArea();
        if (pointerY < visible.getMinY()) {
            return pointerY - visible.getMinY();
        }
        if (pointerY > visible.getMaxY()) {
            return pointerY - visible.getMaxY();
        }
        return 0;
    }

    /**
     * The part of the document the view shows, in the scene: the scroll pane's viewport.
     */
    private Bounds visibleArea() {
        Node viewport = scrollPane.lookup(".viewport");
        Node area = viewport != null ? viewport : scrollPane;
        return area.localToScene(area.getLayoutBounds());
    }

    /**
     * Whether a press that reaches the document happened in a block that handles the mouse itself: a code or source
     * block, whose text hands its presses on before they get here, so one that arrives was on the block's bar or copy
     * button; a table, which selects its own cells, or the box that scrolls it sideways (its bar).
     */
    private boolean takesOwnPresses(EventTarget target) {
        return Ancestors.nearest(target, MonospaceBlock.class, document) != null
                || Ancestors.nearest(target, TableBlock.class, document) != null;
    }

    /**
     * Selects from the anchor to a position: the anchored range and the characters between it and the position.
     */
    private void extendTo(Position at) {
        if (at.compareTo(anchorStart) < 0) {
            select(at, anchorEnd);
        } else if (at.compareTo(anchorEnd) > 0) {
            select(anchorStart, at);
        } else {
            select(anchorStart, anchorEnd);
        }
    }

    /**
     * Selects a range and shows it in every segment it reaches, clearing what the previous range showed outside it.
     */
    private void select(Position from, Position to) {
        int low = from.segment();
        int high = to.segment();
        if (start != null) {
            low = Math.min(low, start.segment());
            high = Math.max(high, end.segment());
        }
        start = from;
        end = to;
        for (int i = low; i <= high; i++) {
            Segment segment = segments.get(i);
            if (i < from.segment() || i > to.segment()) {
                segment.select(0, 0);
            } else {
                segment.select(startIn(i, from), endIn(i, to));
            }
        }
    }

    /**
     * Where a range that starts at a position starts in a segment it reaches.
     */
    private static int startIn(int segment, Position from) {
        return segment == from.segment() ? from.offset() : 0;
    }

    /**
     * Where a range that ends at a position ends in a segment it reaches.
     */
    private int endIn(int segment, Position to) {
        return segment == to.segment() ? to.offset() : segments.get(segment).length();
    }

    /**
     * The position nearest to a point, see the class comment; {@code null} in a document without segments.
     */
    private Position positionAt(double sceneX, double sceneY) {
        if (segments.isEmpty()) {
            return null;
        }
        for (int i = 0; i < segments.size(); i++) {
            Bounds bounds = sceneBounds(segments.get(i));
            if (sceneY < bounds.getMinY()) {
                return i == 0 ? new Position(0, 0) : endOf(i - 1);
            }
            if (sceneY <= bounds.getMaxY()) {
                return new Position(i, segments.get(i).positionAt(sceneX, sceneY));
            }
        }
        return endOf(segments.size() - 1);
    }

    /**
     * The index of the segment whose vertical extent holds a point's y, -1 if none does.
     */
    private int segmentUnder(double sceneY) {
        for (int i = 0; i < segments.size(); i++) {
            Bounds bounds = sceneBounds(segments.get(i));
            if (sceneY >= bounds.getMinY() && sceneY <= bounds.getMaxY()) {
                return i;
            }
        }
        return -1;
    }

    private Position endOf(int segment) {
        return new Position(segment, segments.get(segment).length());
    }

    private static Bounds sceneBounds(Segment segment) {
        Node node = segment.node();
        return node.localToScene(node.getLayoutBounds());
    }

    /**
     * A position in the document: a segment's index and a position in it.
     *
     * @param segment the segment's index in document order
     * @param offset  the position in the segment
     */
    private record Position(int segment, int offset) implements Comparable<Position> {

        @Override
        public int compareTo(Position other) {
            if (segment != other.segment) {
                return Integer.compare(segment, other.segment);
            }
            return Integer.compare(offset, other.offset);
        }
    }
}
