package org.karthu.fx.markdown;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.text.BreakIterator;
import java.util.List;

/**
 * The text flow of a heading or a paragraph, which a document selection runs through: its positions are those of the
 * flow's text layout, each run's characters and one character for each inline image ({@link InlineImage}), as a text
 * flow counts a node among its runs. Copied text gives an inline image as its alt text.
 *
 * <p><b>Finding characters.</b> {@link TextFlow#getHitInfo} and {@link TextFlow#getRangeShape} read the flow's text
 * layout, padding and border included (the older {@code hitTest} and {@code rangeShape} leave the insets out).
 *
 * <p><b>Showing the selection.</b> A run's own selection does not show inside a text flow, so a selection lays a
 * {@link SelectionLayer} over the runs, which shows the selected characters on the highlight
 * ({@code markdown-selection}) in the selection's text colour. The layer is created on the first selection and kept
 * after it, so a paragraph never selected has none. A selected inline image shows the highlight over itself
 * ({@link ImageNode#showSelected(boolean)}), which the layer leaves out.
 */
final class TextSegment extends TextFlow implements Segment {

    /**
     * The character a text flow's text layout holds for a node among its runs, such as an inline image.
     */
    private static final char OBJECT_REPLACEMENT = '\uFFFC';

    private final List<BlockContainer> containers;
    private final boolean spaced;

    /**
     * The selection's layer, {@code null} until the first selection.
     */
    private SelectionLayer layer;

    /**
     * The width the text wraps at, which the flow's inline images shrink to: of the flow's last layout, or the width
     * its parent last measured it at; -1 before either.
     */
    private double wrapWidth = -1;

    /**
     * Creates the flow of a heading or a paragraph, with the text cursor; its runs are added after.
     *
     * @param containers the quotes and list items around it, the outermost first
     * @param spaced     whether copied text sets it apart by blank lines
     */
    TextSegment(List<BlockContainer> containers, boolean spaced) {
        this.containers = List.copyOf(containers);
        this.spaced = spaced;
        setCursor(Cursor.TEXT);
    }

    @Override
    public Node node() {
        return this;
    }

    @Override
    public int length() {
        return text().length();
    }

    @Override
    public int positionAt(double sceneX, double sceneY) {
        return Math.clamp(hit(sceneX, sceneY).getInsertionIndex(), 0, length());
    }

    /**
     * The word under a point as the platform's word iterator finds it in prose: a run of letters and digits (with
     * the apostrophes and dots inside it, as in {@code don't} and {@code java.util}), else a run of spaces, else one
     * character.
     */
    @Override
    public Range wordAt(double sceneX, double sceneY) {
        String text = text();
        if (text.isEmpty()) {
            return new Range(0, 0);
        }
        int index = Math.clamp(hit(sceneX, sceneY).getCharIndex(), 0, text.length() - 1);
        BreakIterator words = BreakIterator.getWordInstance();
        words.setText(text);
        int end = words.following(index);
        int start = words.isBoundary(index) ? index : words.preceding(index);
        return new Range(start, end == BreakIterator.DONE ? text.length() : end);
    }

    /**
     * The whole text: a triple click selects a heading or a paragraph, hard line breaks included.
     */
    @Override
    public Range paragraphAt(double sceneX, double sceneY) {
        return new Range(0, length());
    }

    /**
     * Shows the range's characters on the highlight of the flow's selection layer, and the highlight over each inline
     * image in the range, which the layer leaves out.
     */
    @Override
    public void select(int start, int end) {
        if (start < end && layer == null) {
            // The last child, so it lies over the runs.
            layer = new SelectionLayer(this);
            getChildren().add(layer);
        }
        if (layer != null) {
            layer.select(start, end);
        }
        int position = 0;
        for (Node child : getChildren()) {
            if (child instanceof Text run) {
                position += run.getText().length();
            } else if (child.isManaged()) {
                if (child instanceof ImageNode image) {
                    image.showSelected(position >= start && position < end);
                }
                position++;
            }
        }
    }

    /**
     * The runs' characters of the range, and the alt text of each inline image in it, where the image is: copied text
     * gives an image as its alt text, as a link gives its text.
     */
    @Override
    public String text(int start, int end) {
        StringBuilder text = new StringBuilder();
        int position = 0;
        for (Node child : getChildren()) {
            if (!child.isManaged()) {
                continue;
            }
            if (child instanceof Text run) {
                String runText = run.getText();
                int from = Math.max(start, position);
                int to = Math.min(end, position + runText.length());
                if (from < to) {
                    text.append(runText, from - position, to - position);
                }
                position += runText.length();
            } else {
                if (position >= start && position < end && child instanceof ImageNode image) {
                    text.append(image.alt());
                }
                position++;
            }
        }
        return text.toString();
    }

    @Override
    public List<BlockContainer> containers() {
        return containers;
    }

    @Override
    public boolean spaced() {
        return spaced;
    }

    /**
     * The flow's text as its text layout has it: its runs' text, and for each inline image, or any other node among the
     * runs, one character, the object replacement character, as a text flow counts an embedded node. The selection's
     * layer is no part of it: an unmanaged child is not.
     */
    private String text() {
        StringBuilder text = new StringBuilder();
        for (Node child : getChildren()) {
            if (child instanceof Text run) {
                text.append(run.getText());
            } else if (child.isManaged()) {
                text.append(OBJECT_REPLACEMENT);
            }
        }
        return text.toString();
    }

    /**
     * The width the flow's text wraps at, which its inline images shrink to.
     *
     * @return the width; -1 before the flow is measured or laid out at a width
     */
    double wrapWidth() {
        return wrapWidth;
    }

    /**
     * Measures the text at a width, telling the inline images that width first.
     */
    @Override
    protected double computePrefHeight(double width) {
        if (width >= 0) {
            wrapAt(width - snappedLeftInset() - snappedRightInset());
        }
        return super.computePrefHeight(width);
    }

    /**
     * Takes a new width, telling the inline images first.
     */
    @Override
    protected void setWidth(double value) {
        wrapAt(value - snappedLeftInset() - snappedRightInset());
        super.setWidth(value);
    }

    /**
     * Notes the width the text wraps at. A text flow measures its embedded nodes once and keeps their sizes until it
     * is asked for a layout, which a new width alone does not do, and a node keeps its own preferred size until it is
     * asked for one: so a new width asks each inline image for a layout, which passes the request on to the flow, and
     * the flow measures them anew at that width. Inside a layout of the flow's parent the request goes no further than
     * the flow, which that layout lays out next.
     */
    private void wrapAt(double width) {
        if (width == wrapWidth) {
            return;
        }
        wrapWidth = width;
        for (Node child : getChildren()) {
            if (child instanceof InlineImage image) {
                image.requestLayout();
            }
        }
    }

    private HitInfo hit(double sceneX, double sceneY) {
        Point2D point = sceneToLocal(sceneX, sceneY);
        return getHitInfo(point);
    }

    /**
     * Lays out the runs, then the selection's layer over them: a new width wraps the text anew, and the selection's
     * shape with it.
     */
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (layer != null) {
            layer.layOverFlow();
        }
    }
}
