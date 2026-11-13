package org.karthu.fx.markdown;

import java.util.Objects;

/**
 * A piece of a code block's code that a {@link CodeLexer} found: a range of the code's characters and its
 * {@link CodeCategory category}, which the {@link MarkdownView} colours it by. The record takes any range; the view
 * checks the pieces of a block against the code, and a piece that breaks the lexer contract leaves the whole block in
 * one colour (see {@link CodeLexer}).
 *
 * @param start    the index of the piece's first character in the code (inclusive)
 * @param end      the index after the piece's last character (exclusive), greater than {@code start}
 * @param category the piece's category
 */
public record CodePiece(int start, int end, CodeCategory category) {

    /**
     * Creates a piece.
     *
     * @param start    the index of the piece's first character in the code (inclusive)
     * @param end      the index after the piece's last character (exclusive), greater than {@code start}
     * @param category the piece's category, not {@code null}
     * @throws NullPointerException if the category is {@code null}
     */
    public CodePiece {
        Objects.requireNonNull(category, "category");
    }
}
