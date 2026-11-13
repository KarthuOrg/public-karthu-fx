package org.karthu.fx.markdown;

import java.util.List;

/**
 * Cuts the code of a code block into {@link CodePiece pieces}, each of one {@link CodeCategory category}, so that the
 * {@link MarkdownView} can colour the block: code in, pieces out. A lexer is lexical: it does not parse, so code that
 * does not compile is coloured too.
 *
 * <pre>{@code
 * view.getCodeLanguages().put("rules", code -> code.startsWith("IF")
 *         ? List.of(new CodePiece(0, 2, CodeCategory.KEYWORD))
 *         : List.of());
 * }</pre>
 *
 * <p>The contract: the pieces come in the order of the text. They are not empty, lie inside the text and do not
 * overlap, though they may touch; text between pieces is plain code, shown in the code colour. A lexer is called on
 * the JavaFX Application Thread, once per block each time the view builds its document, and should hold no state
 * between calls. It is not called for a block with more lines than the view's
 * {@link MarkdownView#codeHighlightLineLimitProperty() line limit}. A lexer registers under the lower-case name of
 * its language in the view's {@link MarkdownView#getCodeLanguages() map of languages}; one lexer may serve several
 * names.
 *
 * <p>Robustness: a faulty lexer cannot break the document. If a lexer throws a runtime exception, or returns
 * {@code null}, a {@code null} piece, or a piece that is empty, lies outside the text, comes out of order or overlaps
 * the one before it, the view shows that whole block in one colour, as a block of a language it does not know, and
 * renders the rest of the document normally. It logs a warning naming the language, with the exception if there is
 * one, through SLF4J, as karthu-fx logs everywhere, to the logger named after {@link MarkdownView}'s class.
 */
@FunctionalInterface
public interface CodeLexer {

    /**
     * Cuts code into pieces.
     *
     * @param code the code of a block as the block shows it: its lines separated by line feeds, without a line feed
     *             after the last line
     * @return the pieces in the order of the text, possibly none
     */
    List<CodePiece> lex(String code);
}
