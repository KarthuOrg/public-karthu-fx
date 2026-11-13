package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodeLexer;
import org.karthu.fx.markdown.CodePiece;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lexer of JSON: a scanner over the characters, without parsing, so JSON that does not parse is coloured too. It
 * marks:
 * <ul>
 *   <li>a string followed by {@code :}, after white space, as a property, the whole string with its escapes;</li>
 *   <li>other strings (string), their escapes pieces of their own (escape); an unterminated string runs to the end
 *       of the code;</li>
 *   <li>numbers as whole words, with a minus, a fraction and an exponent;</li>
 *   <li>the literals {@code true}, {@code false} and {@code null} (literal), as whole words;</li>
 *   <li>the punctuation {@code { } [ ] : ,}.</li>
 * </ul>
 * Adjacent pieces of one category are one piece. Everything else, white space and words JSON does not know, is
 * plain.
 */
final class JsonLexer implements CodeLexer {

    private static final Set<String> LITERALS = Set.of("true", "false", "null");

    /**
     * A number from its first character on: an optional minus, digits, a fraction and an exponent.
     */
    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");

    private static final String PUNCTUATION = "{}[]:,";

    @Override
    public List<CodePiece> lex(String code) {
        Pieces pieces = new Pieces();
        int length = code.length();
        int position = 0;
        while (position < length) {
            char character = code.charAt(position);
            if (character == '"') {
                position = string(code, position, pieces);
            } else if (PUNCTUATION.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                position++;
            } else if (isWordPart(character)) {
                position = word(code, position, pieces);
            } else {
                position++;
            }
        }
        return pieces.toList();
    }

    /**
     * A string from its opening quote to its closing one: a property if a {@code :} follows it, else a string with
     * its escapes as pieces of their own; unterminated, it runs to the end of the code.
     *
     * @return the position after the string
     */
    private static int string(String code, int start, Pieces pieces) {
        int end = stringEnd(code, start);
        if (colonFollows(code, end)) {
            pieces.add(start, end, CodeCategory.PROPERTY);
            return end;
        }
        int pieceStart = start;
        int position = start + 1;
        while (position < end) {
            if (code.charAt(position) == '\\') {
                int escapeEnd = escapeEnd(code, position);
                pieces.add(pieceStart, position, CodeCategory.STRING);
                pieces.add(position, escapeEnd, CodeCategory.ESCAPE);
                position = escapeEnd;
                pieceStart = escapeEnd;
            } else {
                position++;
            }
        }
        pieces.add(pieceStart, end, CodeCategory.STRING);
        return end;
    }

    /**
     * The end of a string: the position after its closing quote, or the end of the code.
     */
    private static int stringEnd(String code, int start) {
        int position = start + 1;
        while (position < code.length()) {
            char character = code.charAt(position);
            if (character == '\\') {
                position = escapeEnd(code, position);
            } else if (character == '"') {
                return position + 1;
            } else {
                position++;
            }
        }
        return code.length();
    }

    /**
     * The end of an escape at a backslash: {@code \}{@code u} and up to four hex digits, else the backslash and the
     * character after it, whatever it is.
     */
    private static int escapeEnd(String code, int backslash) {
        int length = code.length();
        int position = backslash + 1;
        if (position >= length) {
            return length;
        }
        char next = code.charAt(position);
        if (next == 'u') {
            position++;
            int digits = 0;
            while (digits < 4 && position < length && Character.digit(code.charAt(position), 16) >= 0) {
                position++;
                digits++;
            }
            return position;
        }
        // Both halves of a surrogate pair, so a run never splits one.
        return Character.isHighSurrogate(next) && position + 1 < length ? position + 2 : position + 1;
    }

    /**
     * A word: a number or a literal if it is one as a whole, else plain ({@code 12ab}, {@code nulls}, {@code True}).
     *
     * @return the position after the word
     */
    private static int word(String code, int start, Pieces pieces) {
        int end = start;
        while (end < code.length() && isWordPart(code.charAt(end))) {
            end++;
        }
        String word = code.substring(start, end);
        Matcher number = NUMBER.matcher(word);
        if (number.matches()) {
            pieces.add(start, end, CodeCategory.NUMBER);
        } else if (LITERALS.contains(word)) {
            pieces.add(start, end, CodeCategory.LITERAL);
        }
        return end;
    }

    /**
     * Whether a character belongs to a word: a number's ({@code -2.5E+3}) or a name's.
     */
    private static boolean isWordPart(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '.' || character == '-'
                || character == '+';
    }

    /**
     * Whether a {@code :} follows a position, after white space.
     */
    private static boolean colonFollows(String code, int position) {
        int next = position;
        while (next < code.length() && Character.isWhitespace(code.charAt(next))) {
            next++;
        }
        return next < code.length() && code.charAt(next) == ':';
    }
}
