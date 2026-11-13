package org.karthu.fx.form;

import javafx.geometry.Point2D;
import javafx.scene.layout.Region;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.text.BreakIterator;

/**
 * The line of a suggestion input's default row: the highlight node of the item text on one line, as wide as its text,
 * and where the row is narrower, clipped after the last character that fits whole, an ellipsis
 * ({@code form-suggestion-ellipsis}) after it. The highlight node keeps the whole text, so its marks stay those of the
 * whole text and follow the search; the ellipsis is a run of its own in a {@code search-highlight} of its own, so that
 * the theme gives it the look of the line's plain runs, also on the preselected row. It asks for no more height than
 * one line, whatever its width.
 *
 * <p>Not exported: the suggestion input's implementation.
 */
final class SuggestionLine extends Region {

    /** The highlight node of the whole text, laid out on one line and clipped to the room before the ellipsis. */
    private final TextFlow line;
    /** The text the line shows, to place the cut between whole characters. */
    private final String text;
    /** What the line shows of its text: all of it, or the part before the cut. */
    private final Rectangle shown = new Rectangle();
    /** The ellipsis after the cut, shown only while the text does not fit. */
    private final TextFlow ellipsis = new TextFlow();

    /**
     * Creates the line of a default row.
     *
     * @param line the highlight node of the item text
     * @param text the item text the highlight node shows
     */
    SuggestionLine(TextFlow line, String text) {
        this.line = line;
        this.text = text;
        line.setClip(shown);
        // The ellipsis as an escape: the build does not depend on the source encoding.
        Text dots = new Text("\u2026");
        dots.getStyleClass().add("search-text");
        ellipsis.getChildren().add(dots);
        ellipsis.getStyleClass().addAll("search-highlight", "form-suggestion-ellipsis");
        ellipsis.setVisible(false);
        getChildren().addAll(line, ellipsis);
    }

    @Override
    protected double computeMinWidth(double height) {
        // As narrow as the row makes it: what does not fit is clipped.
        return snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computePrefWidth(double height) {
        return snappedLeftInset() + line.prefWidth(-1) + snappedRightInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        return snappedTopInset() + line.prefHeight(-1) + snappedBottomInset();
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double room = getWidth() - left - snappedRightInset();
        double lineWidth = line.prefWidth(-1);
        double lineHeight = line.prefHeight(-1);
        // At its own width the text flows on one line; the clip shows what fits.
        line.resizeRelocate(left, top, lineWidth, lineHeight);
        shown.setHeight(lineHeight);
        if (lineWidth <= room) {
            shown.setWidth(lineWidth);
            ellipsis.setVisible(false);
            return;
        }
        double ellipsisWidth = ellipsis.prefWidth(-1);
        double cut = cutBefore(Math.max(0, room - ellipsisWidth), lineHeight);
        shown.setWidth(cut);
        ellipsis.setVisible(true);
        ellipsis.resizeRelocate(left + cut, top, ellipsisWidth, ellipsis.prefHeight(-1));
    }

    /**
     * Where the line is cut so that its shown part ends before a position: the leading edge of the first character
     * that does not fit whole, never inside a character of several code units or before a combining mark, and before
     * the spaces in front of it, so no space stands before the ellipsis.
     *
     * @param room       the position the shown part may reach, in the line's coordinates
     * @param lineHeight the height of the line, whose middle the position is looked up in
     * @return the width of the shown part
     */
    private double cutBefore(double room, double lineHeight) {
        line.layout();
        int index = Math.clamp(line.hitTest(new Point2D(room, lineHeight / 2)).getCharIndex(), 0, text.length());
        BreakIterator characters = BreakIterator.getCharacterInstance();
        characters.setText(text);
        if (index < text.length() && !characters.isBoundary(index)) {
            index = characters.preceding(index);
        }
        while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        if (index <= 0) {
            return 0;
        }
        PathElement[] caret = line.caretShape(index, true);
        if (caret.length > 0 && caret[0] instanceof MoveTo start) {
            return Math.min(start.getX(), room);
        }
        return room;
    }
}
