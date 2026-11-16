package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * A layer that shows ranges of a text flow's characters highlighted: the paragraphs' and headings' flows
 * ({@link TextSegment}) and the flows of code and source blocks ({@link MonospaceBlock}). The selection shows in one,
 * the search's matches in two more, all matches and the current one, below it ({@link SearchMarks}).
 *
 * <p>The runs' own selection ({@code Text.selectionStart}) does not colour the characters of a run inside a text flow
 * (checked with JavaFX 25: a run's selection fill does not show there, at any offsets). So the flow lays this layer
 * over its runs: an unmanaged text flow as large as the flow, with the same insets, line spacing, alignment and tab
 * size, a copy of each run in its font and an empty space as large as each inline image among the runs, so it lays out
 * its text exactly as the flow does, clipped to the ranges' shape. In it the highlight ({@link HighlightPath}, its
 * style class given) fills that shape with its colour, over the flow's highlighted characters, and the copies show
 * those characters on it in the highlight's text colour. The shape leaves out the inline images, which show the
 * selection's highlight over themselves, see-through ({@link ImageNode#showSelected(boolean)}), and which the search
 * never matches. The layer lies outside the flow's text layout: an unmanaged child is no part of a text flow's text.
 *
 * <p>The copies are made when the layer first shows a range, from the flow's runs then, so a layer never shown has
 * none. The copies and the highlight take their colours and fonts by binding, so no style class of the view's reaches
 * the copies and a lookup of the view's runs does not find them.
 */
final class HighlightLayer extends TextFlow {

    private final TextFlow flow;
    private final HighlightPath highlight;
    private final Path clip = new Path();

    /**
     * Whether the copies of the flow's runs are made.
     */
    private boolean copied;

    /**
     * The spaces of the nodes among the flow's runs, its inline images, in the layer's text.
     */
    private final List<NodeSpace> spaces = new ArrayList<>();

    /**
     * The ranges shown, none empty, in the flow's order; none while the layer is hidden.
     */
    private List<Segment.Range> ranges = List.of();

    /**
     * Creates the layer of a flow, hidden, with an empty highlight. The flow adds it after its runs, so it lies over
     * them, and calls {@link #layOverFlow()} whenever it has laid out its runs.
     *
     * @param flow       the flow whose characters it shows highlighted
     * @param styleClass the style class of its highlight, such as {@code markdown-selection}
     */
    HighlightLayer(TextFlow flow, String styleClass) {
        this.flow = flow;
        highlight = new HighlightPath(styleClass);
        setManaged(false);
        setMouseTransparent(true);
        setVisible(false);
        paddingProperty().bind(flow.insetsProperty());
        lineSpacingProperty().bind(flow.lineSpacingProperty());
        textAlignmentProperty().bind(flow.textAlignmentProperty());
        tabSizeProperty().bind(flow.tabSizeProperty());
        getChildren().add(highlight);
        clip.setStroke(null);
        clip.setFill(Color.BLACK);
        setClip(clip);
    }

    /**
     * Shows the characters from {@code start} (inclusive) to {@code end} (exclusive) of the flow's text highlighted;
     * an empty range hides the layer and empties its highlight.
     *
     * @param start the first highlighted character
     * @param end   the position after the last highlighted character
     */
    void show(int start, int end) {
        show(start < end ? List.of(new Segment.Range(start, end)) : List.of());
    }

    /**
     * Shows ranges of the flow's text highlighted; none, or empty ones only, hide the layer and empty its highlight.
     *
     * @param shown the ranges, in the flow's order, not overlapping
     */
    void show(List<Segment.Range> shown) {
        List<Segment.Range> kept = new ArrayList<>();
        for (Segment.Range range : shown) {
            if (range.start() < range.end()) {
                kept.add(range);
            }
        }
        ranges = kept;
        if (kept.isEmpty()) {
            highlight.getElements().clear();
            clip.getElements().clear();
            setVisible(false);
            return;
        }
        if (!copied) {
            copyRuns();
            copied = true;
        }
        layOverFlow();
    }

    /**
     * Whether characters of the flow are shown highlighted: a range shown is not empty.
     *
     * @return whether the layer shows a range
     */
    boolean isShowing() {
        return !ranges.isEmpty();
    }

    /**
     * Lays the layer over the flow as the flow's text is laid out now: a new width wraps the text anew, and the
     * ranges' shape with it. The shape is the ranges', line spacing included, so the highlight of a range over several
     * lines has no gaps.
     */
    void layOverFlow() {
        resizeRelocate(0, 0, flow.getWidth(), flow.getHeight());
        // The nodes among the flow's runs may have a new size, an image that has arrived: the layer measures their
        // spaces anew in its next layout, which follows the flow's within the same pass.
        for (NodeSpace space : spaces) {
            space.requestLayout();
        }
        if (!ranges.isEmpty()) {
            List<Integer> nodes = nodePositions();
            // Each path gets path elements of its own.
            highlight.getElements().setAll(shape(nodes));
            clip.getElements().setAll(shape(nodes));
            setVisible(true);
        }
    }

    /**
     * The positions of the nodes among the flow's runs, its inline images, in order: each is one position of the
     * flow's text.
     */
    private List<Integer> nodePositions() {
        List<Integer> positions = new ArrayList<>();
        int position = 0;
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                position += run.getText().length();
            } else if (child.isManaged()) {
                positions.add(position);
                position++;
            }
        }
        return positions;
    }

    /**
     * The shape of the ranges' characters, without the nodes among the runs: an inline image in the selection shows
     * the highlight over itself, see-through, which an opaque highlight lying over it here would cover.
     */
    private List<PathElement> shape(List<Integer> nodes) {
        List<PathElement> shape = new ArrayList<>();
        for (Segment.Range range : ranges) {
            int from = range.start();
            for (int node : nodes) {
                if (node >= range.start() && node < range.end()) {
                    if (from < node) {
                        shape.addAll(List.of(flow.getRangeShape(from, node, true)));
                    }
                    from = node + 1;
                }
            }
            if (from < range.end()) {
                shape.addAll(List.of(flow.getRangeShape(from, range.end(), true)));
            }
        }
        return shape;
    }

    /**
     * Adds a copy of each of the flow's runs, in the run's font and the highlight's text colour, after the highlight,
     * and an empty space as large as each other node among them, an inline image: the layer lays out its text as the
     * flow does only with the same nodes between the runs.
     */
    private void copyRuns() {
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                Text copy = new Text(run.getText());
                copy.fontProperty().bind(run.fontProperty());
                copy.fillProperty().bind(highlight.highlightTextFillProperty());
                getChildren().add(copy);
            } else if (child.isManaged()) {
                NodeSpace space = new NodeSpace(child);
                spaces.add(space);
                getChildren().add(space);
            }
        }
    }

    /**
     * An empty space in the layer's text as large as a node among the flow's runs, sitting on the baseline as the
     * node does: it measures the node each time the layer lays out its text.
     */
    private static final class NodeSpace extends Region {

        private final Node node;

        NodeSpace(Node node) {
            this.node = node;
        }

        @Override
        protected double computePrefWidth(double height) {
            return node.prefWidth(-1);
        }

        @Override
        protected double computePrefHeight(double width) {
            return node.prefHeight(-1);
        }

        @Override
        protected double computeMinWidth(double height) {
            return computePrefWidth(height);
        }

        @Override
        protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override
        protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }

        @Override
        protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }

        /**
         * The node's baseline; for a node whose baseline is its bottom, its preferred height, which a text flow takes
         * for it, never the space's own height of its last layout.
         */
        @Override
        public double getBaselineOffset() {
            double baseline = node.getBaselineOffset();
            return baseline == BASELINE_OFFSET_SAME_AS_HEIGHT ? node.prefHeight(-1) : baseline;
        }
    }
}
