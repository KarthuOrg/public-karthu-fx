package org.karthu.fx.markdown;

import javafx.scene.Node;

import java.util.List;

/**
 * A part of the document a document selection ({@link DocumentSelection}) runs through: a heading's or a paragraph's
 * text with its inline images ({@link TextSegment}), a code or source block's text ({@link MonospaceBlock}), a table
 * as a whole ({@link TableBlock}), a block image as a whole ({@link ImageBlock}). A segment has positions from 0 to its
 * length, the gaps between its characters, and a selection covers a range of them in each segment it reaches. A table
 * and a block image have two, before and after them: a selection takes them whole or not at all.
 *
 * <p>The view's segments lie one below the other in document order, each over the whole width it is given, so the
 * selection finds the segment under the pointer by its node's vertical extent.
 */
interface Segment {

    /**
     * The node of the segment, whose bounds tell where it lies in the document.
     *
     * @return the node
     */
    Node node();

    /**
     * The number of positions after the first: the segment's length in characters.
     *
     * @return the length
     */
    int length();

    /**
     * The position nearest to a point: the gap between two characters where a text field would put its caret.
     *
     * @param sceneX the point's x in the scene
     * @param sceneY the point's y in the scene
     * @return the position, from 0 to {@link #length()}
     */
    int positionAt(double sceneX, double sceneY);

    /**
     * The range a double click at a point selects: the word under it.
     *
     * @param sceneX the point's x in the scene
     * @param sceneY the point's y in the scene
     * @return the range, empty for an empty segment
     */
    Range wordAt(double sceneX, double sceneY);

    /**
     * The range a triple click at a point selects: a heading's or a paragraph's whole text, a code line without its
     * line feed, a table or a block image whole.
     *
     * @param sceneX the point's x in the scene
     * @param sceneY the point's y in the scene
     * @return the range
     */
    Range paragraphAt(double sceneX, double sceneY);

    /**
     * Shows a range as selected, highlighted; an empty range shows nothing selected.
     *
     * @param start the first selected position
     * @param end   the position after the last selected character, not before {@code start}
     */
    void select(int start, int end);

    /**
     * The text of a range as the reader sees it: the Markdown resolved, a hard line break as a line feed, an image as
     * its alt text.
     *
     * @param start the range's first position
     * @param end   the range's end position, not before {@code start}
     * @return the text
     */
    String text(int start, int end);

    /**
     * What finds the segment again in a new version of the document ({@link NotedSegment}): its content, whatever the
     * reader made of how it shows. By default its whole text as a copy gives it. A table, which the reader sorts and
     * whose columns the reader moves, hides and shows, gives its text in the source's order with every column, as the
     * new document's table, unsorted with every column shown, gives it too.
     *
     * @return the content, equal for segments of the same content
     */
    default String contentKey() {
        return text(0, length());
    }

    /**
     * The quotes and list items around the segment, the outermost first.
     *
     * @return the containers, empty for a segment at the document's top level
     */
    List<BlockContainer> containers();

    /**
     * Whether the copied text sets the segment apart from its neighbours by a blank line, as a paragraph is; else a
     * line feed ends it, as it ends a tight list's item, a code block or a table.
     *
     * @return {@code true} for a heading and a paragraph outside a tight list's items
     */
    boolean spaced();

    /**
     * A range of positions of a segment.
     *
     * @param start the first position
     * @param end   the end position, not before {@code start}
     */
    record Range(int start, int end) {
    }
}
