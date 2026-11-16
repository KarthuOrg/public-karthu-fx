package org.karthu.fx.markdown;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * A point of the document that a new zoom keeps at the same height in the viewport: the reader's place. It is noted
 * in the layout before the zoom and found again in the layout after it, where the text has wrapped anew.
 *
 * <p>The point is held by what it lies in, not by its distance from the document's top, which a new wrap width
 * changes:
 * <ul>
 *   <li>in a heading's or a paragraph's text: by the line it cuts, as the first character of that line and how far
 *       down the line it lies, so the same line is cut as far down after the zoom, wherever the wrap has put it;</li>
 *   <li>in a code or source block or a table: by how far down the block it lies, as a fraction of its height;</li>
 *   <li>between blocks, and in a block image: by its distance in the document's own pixels from the top of the next
 *       block below, or from the bottom of the last block below the last one. Spacing and padding do not change with
 *       the zoom in the document's own pixels, only the scale does.</li>
 * </ul>
 * All of it is in the document's own, unscaled coordinates, so the scale between the two layouts does not matter.
 *
 * <p>A new version of the text keeps the place too: the anchor noted in the old document is moved to the segment the
 * new document has in its segment's place ({@link #movedTo}).
 */
final class ReadingAnchor {

    private final Node document;
    private final Segment segment;
    private final Kind kind;

    /**
     * The first character of the cut line, for {@link Kind#LINE}.
     */
    private final int character;

    /**
     * How far down: a fraction of the line's or the block's height, or a distance in the document's pixels from the
     * block's top or bottom.
     */
    private final double amount;

    private ReadingAnchor(Node document, Segment segment, Kind kind, int character, double amount) {
        this.document = document;
        this.segment = segment;
        this.kind = kind;
        this.character = character;
        this.amount = amount;
    }

    /**
     * Notes the point of the document at a height, in the document's current layout.
     *
     * @param allSegments the document's segments in document order, one below the other
     * @param document    the box of all blocks
     * @param documentY   the point's height in the document's own coordinates
     * @return the anchor, {@code null} for a document without segments other than block images
     */
    static ReadingAnchor at(List<Segment> allSegments, Node document, double documentY) {
        // A block image is a segment of the selection, but no place of the reader's: a point in it is held by the
        // block below, so an image that arrives or changes its size at the top edge leaves the text below in place.
        List<Segment> segments = new ArrayList<>();
        for (Segment segment : allSegments) {
            if (!(segment instanceof ImageBlock)) {
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            return null;
        }
        for (Segment segment : segments) {
            Bounds bounds = bounds(document, segment);
            if (documentY < bounds.getMinY()) {
                return new ReadingAnchor(document, segment, Kind.ABOVE, 0, documentY - bounds.getMinY());
            }
            if (documentY <= bounds.getMaxY()) {
                ReadingAnchor line = inLine(document, segment, documentY);
                if (line != null) {
                    return line;
                }
                double fraction = bounds.getHeight() > 0 ? (documentY - bounds.getMinY()) / bounds.getHeight() : 0;
                return new ReadingAnchor(document, segment, Kind.INSIDE, 0, fraction);
            }
        }
        Segment last = segments.getLast();
        return new ReadingAnchor(document, last, Kind.BELOW, 0, documentY - bounds(document, last).getMaxY());
    }

    /**
     * The segment the point is held by: the one it lies in, the next one below it, or the last one above it.
     *
     * @return the segment, no block image
     */
    Segment segment() {
        return segment;
    }

    /**
     * The same point held by another segment, the one a new version of the text has in place of this one's
     * ({@link NotedSegment}): as far down the line of the same character, limited to the new segment's length, as far
     * down the block, or as far above or below it. A line of a segment that has no lines now, an empty text or a block,
     * becomes the segment's top.
     *
     * @param newSegment the segment of the new document, no block image
     * @return the anchor in the new document
     */
    ReadingAnchor movedTo(Segment newSegment) {
        if (kind != Kind.LINE) {
            return new ReadingAnchor(document, newSegment, kind, 0, amount);
        }
        if (!(newSegment instanceof TextSegment) || newSegment.length() == 0) {
            return new ReadingAnchor(document, newSegment, Kind.INSIDE, 0, 0);
        }
        return new ReadingAnchor(document, newSegment, Kind.LINE, Math.min(character, newSegment.length() - 1),
                amount);
    }

    /**
     * Where the noted point lies now, in the document's current layout.
     *
     * @return its height in the document's own coordinates
     */
    double documentY() {
        Bounds bounds = bounds(document, segment);
        return switch (kind) {
            case ABOVE -> bounds.getMinY() + amount;
            case BELOW -> bounds.getMaxY() + amount;
            case INSIDE -> bounds.getMinY() + amount * bounds.getHeight();
            case LINE -> lineY();
        };
    }

    /**
     * The anchor of a point in a heading's or a paragraph's text: the line it cuts; {@code null} for another segment
     * or a text without lines.
     */
    private static ReadingAnchor inLine(Node document, Segment segment, double documentY) {
        if (!(segment instanceof TextSegment flow) || segment.length() == 0) {
            return null;
        }
        Point2D local = flow.sceneToLocal(document.localToScene(0, documentY));
        int character = Math.clamp(flow.getHitInfo(new Point2D(0, local.getY())).getCharIndex(), 0,
                segment.length() - 1);
        LineExtent line = lineExtent(flow, character);
        if (line.height() <= 0) {
            return null;
        }
        double fraction = (local.getY() - line.top()) / line.height();
        return new ReadingAnchor(document, segment, Kind.LINE, character, fraction);
    }

    /**
     * The noted point in the line of the noted character as the flow is laid out now.
     */
    private double lineY() {
        TextFlow flow = (TextFlow) segment.node();
        LineExtent line = lineExtent(flow, character);
        double localY = line.top() + amount * line.height();
        return document.sceneToLocal(flow.localToScene(0, localY)).getY();
    }

    /**
     * The top and bottom of the line a character lies in, line spacing included, in the flow's coordinates.
     */
    private static LineExtent lineExtent(TextFlow flow, int character) {
        double top = Double.POSITIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (PathElement element : flow.getRangeShape(character, character + 1, true)) {
            double y;
            if (element instanceof MoveTo move) {
                y = move.getY();
            } else if (element instanceof LineTo line) {
                y = line.getY();
            } else {
                continue;
            }
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
        }
        return new LineExtent(top, bottom);
    }

    /**
     * A segment's bounds in the document's own coordinates.
     */
    private static Bounds bounds(Node document, Segment segment) {
        Node node = segment.node();
        return document.sceneToLocal(node.localToScene(node.getLayoutBounds()));
    }

    /**
     * The top and bottom of a line of a flow, in the flow's coordinates.
     *
     * @param top    the line's top
     * @param bottom the line's bottom
     */
    private record LineExtent(double top, double bottom) {

        /**
         * The line's height, 0 or less for a line without extent.
         */
        double height() {
            return bottom - top;
        }
    }

    /**
     * What the point is held by.
     */
    private enum Kind {
        /** Between blocks: a distance above the top of the next block. */
        ABOVE,
        /** Below the last block: a distance below its bottom. */
        BELOW,
        /** In a code or source block or a table: a fraction of its height. */
        INSIDE,
        /** In a heading's or paragraph's text: a fraction of the height of the line it cuts. */
        LINE
    }
}
