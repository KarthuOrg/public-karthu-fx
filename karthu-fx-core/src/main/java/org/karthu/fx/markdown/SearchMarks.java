package org.karthu.fx.markdown;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * The marks of the search over a text flow, a heading's, a paragraph's, a code or source block's or a table's header
 * or body cell's: two highlight layers ({@link HighlightLayer}) over its runs, one with every match but the current one
 * ({@code markdown-search-match}), above it one with the current match ({@code markdown-search-current}). Both lie
 * below the flow's selection layer, so the selection always shows on top; a table cell's flow has none, the theme
 * shows a selected cell's selection through its marks.
 *
 * <p>A flow holds its marks from the start; the layers are made when the flow first shows a match and kept after,
 * hidden while it shows none, so a flow never marked has none. The flow calls {@link #layOverFlow()} whenever it has
 * laid out its runs.
 */
final class SearchMarks {

    private final TextFlow flow;

    /**
     * The layer of every match but the current one; {@code null} until the first match.
     */
    private HighlightLayer matchLayer;

    /**
     * The layer of the current match, above the other; {@code null} until the first match.
     */
    private HighlightLayer currentLayer;

    /**
     * Creates the marks of a flow, without layers yet.
     *
     * @param flow the flow
     */
    SearchMarks(TextFlow flow) {
        this.flow = flow;
    }

    /**
     * Shows matches of the flow's text, the current one among them in its own layer: none hide the marks.
     *
     * @param matches the matches, as ranges of the flow's positions in their order
     * @param current the index of the current match among them, -1 if it lies elsewhere or there is none
     * @param below   the node the layers lie below, the flow's selection layer; {@code null} while the flow has none,
     *                which, made later, lies above them as the flow's last child
     */
    void show(List<Segment.Range> matches, int current, Node below) {
        if (matchLayer == null) {
            if (matches.isEmpty()) {
                return;
            }
            matchLayer = new HighlightLayer(flow, HighlightPath.SEARCH_MATCH);
            currentLayer = new HighlightLayer(flow, HighlightPath.SEARCH_CURRENT);
            int index = below == null ? flow.getChildren().size() : flow.getChildren().indexOf(below);
            flow.getChildren().addAll(index, List.of(matchLayer, currentLayer));
            if (flow.getScene() != null) {
                // Styled now: layers made during a layout would otherwise show one frame in a path's default black,
                // before the next pulse styles them.
                matchLayer.applyCss();
                currentLayer.applyCss();
            }
        }
        List<Segment.Range> others = new ArrayList<>(matches.size());
        for (int i = 0; i < matches.size(); i++) {
            if (i != current) {
                others.add(matches.get(i));
            }
        }
        matchLayer.show(others);
        currentLayer.show(current >= 0 && current < matches.size() ? List.of(matches.get(current)) : List.of());
    }

    /**
     * The box around a range of a flow's characters, in the flow's coordinates: from the shape of the range, each
     * line's box without the line spacing.
     *
     * @param flow  the flow, laid out
     * @param range the range, not empty
     * @return the box
     */
    static Bounds boundsOf(TextFlow flow, Segment.Range range) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (PathElement element : flow.getRangeShape(range.start(), range.end(), false)) {
            double x;
            double y;
            if (element instanceof MoveTo move) {
                x = move.getX();
                y = move.getY();
            } else if (element instanceof LineTo line) {
                x = line.getX();
                y = line.getY();
            } else {
                continue;
            }
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (minX > maxX) {
            return new BoundingBox(0, 0, 0, 0);
        }
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Lays the layers over the flow as its text is laid out now, after the flow has laid out its runs: a new width
     * wraps the text anew, and the marks' shapes with it. Nothing while the flow has never shown a match.
     */
    void layOverFlow() {
        if (matchLayer != null) {
            matchLayer.layOverFlow();
            currentLayer.layOverFlow();
        }
    }
}
