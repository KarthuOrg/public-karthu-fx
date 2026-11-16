package org.karthu.fx.markdown;

import javafx.geometry.Bounds;

import java.util.List;

/**
 * A segment of the document the view's search runs through ({@link DocumentSearch}): a heading's or a paragraph's
 * text ({@link TextSegment}), a code or source block's text ({@link MonospaceBlock}), a table's cells
 * ({@link TableBlock}). It offers the search its texts, in its order, each a text of its own, so no match spans two:
 * the search finds the matches in each.
 *
 * <p>A segment of several texts, such as a table of cells, numbers them in the order it shows them; a match is the
 * number of its text and its range there. A block image offers none and is no searchable segment.
 *
 * <p>A table's texts change their order when the reader sorts it or moves its columns, and come and go when the reader
 * hides or shows a column. Such a segment tells the search ({@link #onSearchTextsChanged(Runnable)}) and names its
 * texts by something that stays with a text across the change ({@link #searchTextKey(int)}), so the current match
 * stays on its text. The texts of headings, paragraphs and blocks never change.
 */
interface Searchable extends Segment {

    /**
     * The texts the search runs through, in the segment's order, as the reader sees them.
     *
     * @return the texts, not empty strings necessarily; none for a segment without text
     */
    List<String> searchTexts();

    /**
     * What names a text of the segment across a change of its texts' order: equal for the same text before and after
     * the change, different for different texts. The text is given by its number among the texts of the segment's
     * last {@link #searchTexts()}, so asked before the search runs through the segment again it names a text as it was
     * numbered before the change.
     *
     * @param text the text's number among the texts of the last {@link #searchTexts()}
     * @return what names it; by default its number, for a segment whose texts never change
     */
    default Object searchTextKey(int text) {
        return text;
    }

    /**
     * The number of the text a key names ({@link #searchTextKey(int)}) among the texts of the segment's last
     * {@link #searchTexts()}: so the search finds the text of its current match again in the segment that takes an old
     * one's place in a new version of the document, as a new table, unsorted with every column, takes the place of
     * one the reader sorted or rearranged.
     *
     * @param key what names a text, as this segment or the one it takes the place of named it
     * @return the text's number; by default the key itself where it is a number, -1 where it is none
     */
    default int searchTextNumber(Object key) {
        return key instanceof Integer number ? number : -1;
    }

    /**
     * Has the segment run code whenever its texts change their order or come and go after the search ran through
     * them, as a table's do; a later call replaces the code. A segment whose texts never change does nothing.
     *
     * @param changed the code, which runs the search through the document again
     */
    default void onSearchTextsChanged(Runnable changed) {
    }

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
     * Has the parts of the segment near the visible area show their marks, for a segment of many flows that marks
     * only those, as a long table marks the rows near the area: the search calls it after
     * {@link #showSearchMatches(List, int)} and again as the view scrolls, while the segment reaches into the area. A
     * part that leaves the area keeps its marks until the matches change. A segment of one flow shows all its marks at
     * once and does nothing here.
     *
     * @param area the part of the document near the visible area
     */
    default void showSearchMatchesNear(NearArea area) {
    }

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
