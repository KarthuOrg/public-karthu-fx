package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodePiece;

import java.util.ArrayList;
import java.util.List;

/**
 * The pieces a lexer finds, collected in the order of the text. A piece that directly follows one of the same
 * category joins it, so a run of brackets such as {@code ());} or two escapes in a row are one piece, one run of the
 * view: the look is the same, with fewer nodes.
 */
final class Pieces {

    private final List<CodePiece> pieces = new ArrayList<>();

    /**
     * Adds a piece after the ones added so far; an empty range adds nothing.
     *
     * @param start    the piece's first character (inclusive)
     * @param end      the character after its last (exclusive)
     * @param category its category
     */
    void add(int start, int end, CodeCategory category) {
        if (end <= start) {
            return;
        }
        if (!pieces.isEmpty()) {
            CodePiece last = pieces.getLast();
            if (last.end() == start && last.category() == category) {
                pieces.set(pieces.size() - 1, new CodePiece(last.start(), end, category));
                return;
            }
        }
        pieces.add(new CodePiece(start, end, category));
    }

    /**
     * Returns the pieces added, in the order of the text.
     *
     * @return the pieces
     */
    List<CodePiece> toList() {
        return List.copyOf(pieces);
    }
}
