package org.karthu.fx.markdown;

import javafx.beans.value.ObservableIntegerValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which pieces of a code block the view colours: those the lexer of the block's language cuts its code into, by the
 * view's map of languages. A block whose language the view does not know is not coloured, nor a block with more lines
 * than the view's limit, whose lexer is not called, nor a block whose lexer fails: it throws a runtime exception or
 * returns pieces that break the lexer contract. A failure is logged as a warning naming the language, and the block
 * shows in one colour while the rest of the document renders.
 */
final class CodeColoring {

    /**
     * The view's languages, by their lower-case names, read at each block.
     */
    private final Map<String, CodeLexer> languages;

    /**
     * The view's limit of lines per block, read at each block.
     */
    private final ObservableIntegerValue lineLimit;

    /**
     * Where a failing lexer is reported: the SLF4J logger named after the view's class, as karthu-fx-core logs
     * everywhere, a recorder in tests.
     */
    private Logger logger = LoggerFactory.getLogger(MarkdownView.class);

    /**
     * Creates the colouring of a view's code blocks.
     *
     * @param languages the view's map of languages
     * @param lineLimit the view's limit of lines per block
     */
    CodeColoring(Map<String, CodeLexer> languages, ObservableIntegerValue lineLimit) {
        this.languages = languages;
        this.lineLimit = lineLimit;
    }

    /**
     * Replaces where a failing lexer is reported.
     *
     * @param logger the logger from now on
     */
    void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * The pieces of a code block's code, by the lexer of its language: the first word of its info string, which ends
     * at the first space or opening brace, in lower case. None if the view does not know the language, the code has
     * more lines than the limit or the lexer fails.
     *
     * @param info the block's info string
     * @param code the block's code as it shows it
     * @return the pieces to colour, possibly none
     */
    List<CodePiece> pieces(String info, String code) {
        String language = language(info);
        CodeLexer lexer = languages.get(language);
        if (lexer == null || longerThan(code, lineLimit.get())) {
            return List.of();
        }
        List<CodePiece> pieces;
        try {
            pieces = lexer.lex(code);
        } catch (RuntimeException failure) {
            logger.warn(failed(language, "threw an exception"), failure);
            return List.of();
        }
        String fault = fault(pieces, code.length());
        if (fault != null) {
            logger.warn(failed(language, fault));
            return List.of();
        }
        return List.copyOf(pieces);
    }

    /**
     * What breaks the lexer contract in the pieces a lexer returned for code of a length: no list, a null piece, or a
     * piece that is empty, lies outside the text, or starts before the piece ahead of it ends, out of order or
     * overlapping. The first fault found, {@code null} if there is none.
     */
    private static String fault(List<CodePiece> pieces, int length) {
        if (pieces == null) {
            return "returned null instead of a list";
        }
        int previousEnd = 0;
        for (CodePiece piece : pieces) {
            if (piece == null) {
                return "returned a null piece";
            }
            if (piece.end() <= piece.start()) {
                return "returned an empty piece, " + piece;
            }
            if (piece.start() < 0 || piece.end() > length) {
                return "returned a piece outside the code's " + length + " characters, " + piece;
            }
            if (piece.start() < previousEnd) {
                return "returned a piece out of order or overlapping the one before it, " + piece;
            }
            previousEnd = piece.end();
        }
        return null;
    }

    /**
     * Whether code has more lines than a limit: its line feeds plus one. Below one, every code has more.
     */
    private static boolean longerThan(String code, int limit) {
        int lines = 1;
        for (int index = 0; index < code.length() && lines <= limit; index++) {
            if (code.charAt(index) == '\n') {
                lines++;
            }
        }
        return lines > limit;
    }

    /**
     * The warning of a failing lexer, naming its language and what went wrong.
     */
    private static String failed(String language, String fault) {
        return "The lexer of the code language '" + language + "' " + fault
                + ", so the code block shows in one colour";
    }

    /**
     * The language an info string names: its first word, which ends at the first space or opening brace, in lower
     * case.
     */
    private static String language(String info) {
        int end = 0;
        while (end < info.length() && info.charAt(end) != ' ' && info.charAt(end) != '{') {
            end++;
        }
        return info.substring(0, end).toLowerCase(Locale.ROOT);
    }
}
