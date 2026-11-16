package org.karthu.fx.markdown;

import javafx.geometry.Bounds;

import java.util.List;

/**
 * A segment of the document the view's search runs through ({@link DocumentSearch}): a heading's or a paragraph's
 * text ({@link TextSegment}), a code or source block's text ({@link MonospaceBlock}). It offers the search its texts,
 * in its order, each a text of its own, so no match spans two: the search finds the matches in each.
 *
 * <p>A segment of several texts, such as a table of cells, numbers them in the order it shows them; a match is the
 * number of its text and its range there. A block image offers none and is no searchable segment.
 */
interface Searchable extends Segment {

    /**
     * The texts the search runs through, in the segment's order, as the reader sees them.
     *
     * @return the texts, not empty strings necessarily; none for a segment without text
     */
    List<String> searchTexts();

    /**
     * Shows the search's matches in the segment: the current one in its own colour, every other one in the colour of
     * all matches. Called with the matches the segment holds whenever they or the current match change, and with none
     * to take the marks away.
     *
     * @param matches the segment's matches, in its order; none to show no marks
     * @param current the index of the current match among them, -1 if it lies elsewhere or there is none
     */
    void showSearchMatches(List<Match> matches, int current);

    /**
     * Where a match lies in the scene as the segment is laid out and scrolled now: the box around its characters.
     *
     * @param match one of the segment's matches
     * @return its bounds in the scene
     */
    Bounds searchMatchBounds(Match match);

    /**
     * Scrolls the segment sideways so that a match shows whole, if the segment scrolls sideways itself and the match
     * does not show whole: a code or source block whose line is wider than the block. Text that wraps does nothing.
     *
     * @param match one of the segment's matches, the one the search goes to
     */
    void revealSideways(Match match);

    /**
     * A match of the search in a segment: a range of one of its texts.
     *
     * @param text  the number of the text in {@link #searchTexts()}, from 0
     * @param start the match's first character in the text
     * @param end   the position after its last character
     */
    record Match(int text, int start, int end) {
    }
}
