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
 * The layer that shows a selection of a text flow's characters: the paragraphs' and headings' flows
 * ({@link TextSegment}) and the flows of code and source blocks ({@link MonospaceBlock}).
 *
 * <p>The runs' own selection ({@code Text.selectionStart}) does not colour the characters of a run inside a text flow
 * (checked with JavaFX 25: a run's selection fill does not show there, at any offsets). So the flow lays this layer
 * over its runs: an unmanaged text flow as large as the flow, with the same insets, line spacing, alignment and tab
 * size, a copy of each run in its font and an empty space as large as each inline image among the runs, so it lays out
 * its text exactly as the flow does, clipped to the selection's shape. In it the highlight ({@code markdown-selection})
 * fills that shape with the selection colour, over the flow's selected characters, and the copies show the selected
 * characters on it in the selection's text colour. The shape leaves out the inline images, which show the highlight
 * over themselves, see-through ({@link ImageNode#showSelected(boolean)}). The layer lies outside the flow's text
 * layout: an unmanaged child is no part of a text flow's text.
 *
 * <p>The copies are made on the first selection, from the flow's runs then, so a flow never selected has none. The
 * copies and the highlight take their colours and fonts by binding, so no style class of the view's reaches the copies
 * and a lookup of the view's runs does not find them.
 */
final class SelectionLayer extends TextFlow {

    private final TextFlow flow;
    private final SelectionHighlight highlight = new SelectionHighlight();
    private final Path clip = new Path();

    /**
     * Whether the copies of the flow's runs are made.
     */
    private boolean copied;

    /**
     * The spaces of the nodes among the flow's runs, its inline images, in the layer's text.
     */
    private final List<NodeSpace> spaces = new ArrayList<>();

    private int selectionStart;
    private int selectionEnd;

    /**
     * Creates the layer of a flow's selection, hidden, with an empty highlight. The flow adds it as its last child,
     * after its runs, so it lies over them, and calls {@link #layOverFlow()} whenever it has laid out its runs.
     *
     * @param flow the flow whose characters it shows selected
     */
    SelectionLayer(TextFlow flow) {
        this.flow = flow;
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
     * Shows the characters from {@code start} (inclusive) to {@code end} (exclusive) of the flow's text selected; an
     * empty range hides the layer and empties its highlight.
     *
     * @param start the first selected character
     * @param end   the position after the last selected character
     */
    void select(int start, int end) {
        selectionStart = start;
        selectionEnd = end;
        if (start >= end) {
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
     * Whether characters of the flow are shown selected: the range last selected is not empty.
     *
     * @return whether the layer shows a selection
     */
    boolean hasSelection() {
        return selectionStart < selectionEnd;
    }

    /**
     * Lays the layer over the flow as the flow's text is laid out now: a new width wraps the text anew, and the
     * selection's shape with it. The shape is the selected range's, line spacing included, so the highlight of a
     * selection over several lines has no gaps.
     */
    void layOverFlow() {
        resizeRelocate(0, 0, flow.getWidth(), flow.getHeight());
        // The nodes among the flow's runs may have a new size, an image that has arrived: the layer measures their
        // spaces anew in its next layout, which follows the flow's within the same pass.
        for (NodeSpace space : spaces) {
            space.requestLayout();
        }
        if (selectionStart < selectionEnd) {
            // Each path gets path elements of its own.
            highlight.getElements().setAll(selectedShape());
            clip.getElements().setAll(selectedShape());
            setVisible(true);
        }
    }

    /**
     * The shape of the selected characters, without the nodes among the runs: an inline image in the selection shows
     * the highlight over itself, see-through, which an opaque highlight lying over it here would cover.
     */
    private List<PathElement> selectedShape() {
        List<PathElement> shape = new ArrayList<>();
        int from = selectionStart;
        int position = 0;
        for (Node child : flow.getChildren()) {
            if (child instanceof Text run) {
                position += run.getText().length();
            } else if (child.isManaged()) {
                if (position >= selectionStart && position < selectionEnd) {
                    if (from < position) {
                        shape.addAll(List.of(flow.getRangeShape(from, position, true)));
                    }
                    from = position + 1;
                }
                position++;
            }
        }
        if (from < selectionEnd) {
            shape.addAll(List.of(flow.getRangeShape(from, selectionEnd, true)));
        }
        return shape;
    }

    /**
     * Adds a copy of each of the flow's runs, in the run's font and the selection's text colour, after the highlight,
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
