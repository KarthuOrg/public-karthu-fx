package org.karthu.fx.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * A segment of the shown document, noted before {@link MarkdownView#updateMarkdown} renders a new version of the text,
 * to be found again in the new document: the reader's place and the search's current match are moved to the segment
 * found. The one rule for both.
 *
 * <p>A segment is noted by its kind (its class: a heading's or a paragraph's text, a code or source block, a table),
 * its content ({@link Segment#contentKey}: its text as a copy gives it, a table's in the source's order with every
 * column) and its number. The segment found is the new one of the same kind with the same content nearest to the old
 * number, the one above on a tie: so a paragraph stays found when text above it is added or removed, and so does a
 * table the reader sorted or rearranged. Without one, the content of the segment was changed itself, and the segment
 * with the old number is found, the last one if the new document is shorter.
 *
 * <p>Block images are not counted: they hold no place of the reader's ({@link ReadingAnchor}) and no match of the
 * search, and an image added or removed above would shift the numbers of the text below.
 */
final class NotedSegment {

    private final Class<? extends Segment> kind;
    private final String content;
    private final int number;

    private NotedSegment(Class<? extends Segment> kind, String content, int number) {
        this.kind = kind;
        this.content = content;
        this.number = number;
    }

    /**
     * Notes a segment of a document.
     *
     * @param segments the document's segments in document order
     * @param segment  one of them, no block image
     * @return the noted segment
     */
    static NotedSegment of(List<Segment> segments, Segment segment) {
        List<Segment> counted = counted(segments);
        int number = 0;
        while (counted.get(number) != segment) {
            number++;
        }
        return new NotedSegment(segment.getClass(), segment.contentKey(), number);
    }

    /**
     * Finds the noted segment again in a new version of the document.
     *
     * @param segments the new document's segments in document order
     * @return the segment found; {@code null} for a document without segments other than block images
     */
    Segment findIn(List<Segment> segments) {
        List<Segment> counted = counted(segments);
        if (counted.isEmpty()) {
            return null;
        }
        // Outwards from the old number, above before below at the same distance; each segment's content is read once.
        for (int distance = 0; distance <= Math.max(number, counted.size() - 1 - number); distance++) {
            Segment above = sameAt(counted, number - distance);
            if (above != null) {
                return above;
            }
            Segment below = distance == 0 ? null : sameAt(counted, number + distance);
            if (below != null) {
                return below;
            }
        }
        return counted.get(Math.min(number, counted.size() - 1));
    }

    /**
     * The segment at a number if it is of the noted kind and has the noted content.
     *
     * @return the segment, {@code null} if it is not or the number lies outside the document
     */
    private Segment sameAt(List<Segment> counted, int at) {
        if (at < 0 || at >= counted.size()) {
            return null;
        }
        Segment segment = counted.get(at);
        return segment.getClass() == kind && segment.contentKey().equals(content) ? segment : null;
    }

    /**
     * The segments that count, in document order: all but the block images.
     */
    private static List<Segment> counted(List<Segment> segments) {
        List<Segment> counted = new ArrayList<>();
        for (Segment segment : segments) {
            if (!(segment instanceof ImageBlock)) {
                counted.add(segment);
            }
        }
        return counted;
    }
}
