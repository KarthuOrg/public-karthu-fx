package org.karthu.fx.markdown;

import javafx.scene.Node;

import java.util.List;
import java.util.OptionalInt;

/**
 * Where in the Markdown source the rendered document lies: the source line of a place of the view.
 *
 * <p>The parser gives every block its first source line, a list item, a table row and a code block's line their own
 * ({@link org.karthu.fx.markdown.parser.Block#line()}). The renderer hands them to the nodes it builds: the document's
 * segments ({@link Segment#sourceLineAt(double)}), which tell the line of their part at a height (a code line, a table
 * row as the table shows it now, sorted or not), and the rows of list items ({@link ListItemBox#sourceLine()}). So the
 * line of a place is found from the nodes, whatever the zoom, the margins or the version of the text.
 *
 * <p>Two places are asked for:
 * <ul>
 *   <li>the top of the view ({@link #fromTop(List, double, double)}): the first block, code line or table row
 *       whose top lies in the visible area, the first fully visible one;</li>
 *   <li>a point under the pointer ({@link #at(Node, double)}): the line of the innermost block or list item the
 *       picked node lies in, the context menu's place.</li>
 * </ul>
 */
final class SourceLines {

    /**
     * How far, in scene pixels, a top may lie above a height and still count as at it: the layout snaps positions to
     * pixels, so a block scrolled to the top edge may lie a fraction above it.
     */
    private static final double TOLERANCE = 1;

    private SourceLines() {
    }

    /**
     * The source line of the first part of the visible area whose top lies at or below the area's top edge: a block
     * whole, else a code or source line, a table row. A heading or a paragraph cut by the edge does not count, its next
     * block does. Where no part starts in the area, the reader is in a part taller than the area: the line of the part
     * at the top edge counts, of the last part above it below the document's last block.
     *
     * @param segments the document's segments in document order
     * @param top      the top edge of the visible area in the scene
     * @param bottom   the bottom edge of the visible area in the scene
     * @return the 1-based source line; empty for a document without segments
     */
    static OptionalInt fromTop(List<Segment> segments, double top, double bottom) {
        if (segments.isEmpty()) {
            return OptionalInt.empty();
        }
        Segment cut = segments.getFirst();
        for (Segment segment : segments) {
            Node node = segment.node();
            if (node.localToScene(node.getLayoutBounds()).getMinY() > bottom) {
                break;
            }
            int line = segment.sourceLineFrom(top);
            if (line > 0) {
                return OptionalInt.of(line);
            }
            cut = segment;
        }
        return OptionalInt.of(cut.sourceLineAt(top));
    }

    /**
     * The source line of a point of the document: the line of the innermost segment the picked node lies in, at the
     * point's height (a code line, a table row), else the line of the list item it lies in (its marker or check box).
     *
     * @param picked the node under the point, the target of a mouse event
     * @param sceneY the point's height in the scene
     * @return the 1-based source line; empty where the point lies in no block, such as the margins
     */
    static OptionalInt at(Node picked, double sceneY) {
        for (Node node = picked; node != null; node = node.getParent()) {
            if (node instanceof Segment segment) {
                return OptionalInt.of(segment.sourceLineAt(sceneY));
            }
            if (node instanceof ListItemBox item) {
                return OptionalInt.of(item.sourceLine());
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Whether a node's top lies at or below a height.
     *
     * @param node   the node
     * @param sceneY the height in the scene
     * @return {@code true} if the node's top lies there, within the tolerance
     */
    static boolean startsFrom(Node node, double sceneY) {
        return isAtOrBelow(node.localToScene(node.getLayoutBounds()).getMinY(), sceneY);
    }

    /**
     * Whether a top lies at or below a height, within the tolerance.
     *
     * @param top    the top in the scene
     * @param sceneY the height in the scene
     * @return {@code true} if the top lies there
     */
    static boolean isAtOrBelow(double top, double sceneY) {
        return top >= sceneY - TOLERANCE;
    }
}
