package org.karthu.fx.markdown;

import java.util.List;

/**
 * The text a document selection copies: what the reader sees, laid out as plain text.
 *
 * <ul>
 *   <li>Each segment's selected part as its visible text: inline Markdown resolved, an image as its alt text, a soft
 *       line break a space, a hard line break a line feed; code as written; a table as its header line and rows, cells
 *       joined by tabs; a block image as its alt text, set apart like the paragraph it stands for.</li>
 *   <li>A line feed between two segments, a blank line where a heading or a paragraph meets its neighbour (a tight
 *       list's items are no paragraphs here, as in HTML, where they hold no {@code <p>}).</li>
 *   <li>Each line prefixed by its containers, the outermost first: {@code > } for a quote; for a list item the marker
 *       and a space on the first line of its first block, two spaces on every other line. A blank line between two
 *       blocks of a container takes the prefix they share, without trailing spaces ({@code >} inside a quote).</li>
 *   <li>A segment whose selected part starts within a line gives that line no prefix: only a line whose first
 *       character is selected has one.</li>
 * </ul>
 */
final class SelectionText {

    private SelectionText() {
    }

    /**
     * A segment's selected part.
     *
     * @param segment the segment
     * @param start   the part's first position
     * @param end     the part's end position
     */
    record Piece(Segment segment, int start, int end) {
    }

    /**
     * The text of selected parts.
     *
     * @param pieces the parts in document order; empty ones add nothing
     * @return the text, empty if every part is empty
     */
    static String of(List<Piece> pieces) {
        StringBuilder text = new StringBuilder();
        Piece previous = null;
        for (Piece piece : pieces) {
            if (piece.start() >= piece.end()) {
                continue;
            }
            if (previous != null) {
                text.append('\n');
                if (previous.segment().spaced() || piece.segment().spaced()) {
                    text.append(sharedPrefix(previous.segment(), piece.segment())).append('\n');
                }
            }
            appendLines(text, piece);
            previous = piece;
        }
        return text.toString();
    }

    private static void appendLines(StringBuilder text, Piece piece) {
        Segment segment = piece.segment();
        String before = segment.text(0, piece.start());
        boolean startsLine = before.isEmpty() || before.endsWith("\n");
        boolean firstLine = before.indexOf('\n') < 0;
        String[] lines = segment.text(piece.start(), piece.end()).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                text.append('\n');
            }
            if (i > 0 || startsLine) {
                String prefix = prefix(segment, firstLine && i == 0);
                text.append(lines[i].isEmpty() ? prefix.stripTrailing() : prefix);
            }
            text.append(lines[i]);
        }
    }

    /**
     * The prefix of a segment's line from its containers.
     */
    private static String prefix(Segment segment, boolean firstLine) {
        StringBuilder prefix = new StringBuilder();
        for (BlockContainer container : segment.containers()) {
            prefix.append(container.prefix(segment, firstLine));
        }
        return prefix.toString();
    }

    /**
     * The prefix of a blank line between two segments: the continuation of the containers both lie in, without
     * trailing spaces.
     */
    private static String sharedPrefix(Segment upper, Segment lower) {
        List<BlockContainer> upperContainers = upper.containers();
        List<BlockContainer> lowerContainers = lower.containers();
        StringBuilder prefix = new StringBuilder();
        int shared = Math.min(upperContainers.size(), lowerContainers.size());
        for (int i = 0; i < shared && upperContainers.get(i) == lowerContainers.get(i); i++) {
            prefix.append(upperContainers.get(i).continuation());
        }
        return prefix.toString().stripTrailing();
    }
}
