package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodeLexer;
import org.karthu.fx.markdown.CodePiece;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lexer of Java code: a scanner over the characters, without parsing, so code that does not compile is coloured
 * too. It marks:
 * <ul>
 *   <li>comments: {@code //} to the line's end and block comments (comment), doc comments that start with
 *       {@code /**} (doc);</li>
 *   <li>strings, text blocks and characters (string), their escapes pieces of their own (escape); a keyword inside
 *       a string or a comment stays inside it, and an unterminated string, character, text block or comment runs to
 *       the end of the code;</li>
 *   <li>numbers as whole words: decimal, hex, binary and octal, with underscores, fractions, exponents and
 *       suffixes;</li>
 *   <li>keywords, the contextual ones of classes and statements too ({@code var}, {@code record}, {@code yield},
 *       {@code sealed}, {@code non-sealed}, {@code permits}, {@code when}), and the literals {@code true},
 *       {@code false} and {@code null} (literal);</li>
 *   <li>annotations, {@code @} and a name, qualified or not (annotation; {@code @interface} is a keyword);</li>
 *   <li>a name followed by spaces or tabs and {@code (} as a function, unless it is a keyword; else a name in
 *       capitals only of two characters or more as a constant ({@code MAX_ROWS}), a name with a capital first as a
 *       type ({@code Row}, a type parameter {@code T});</li>
 *   <li>operators and punctuation (brackets, {@code ;}, {@code ,}, {@code .}).</li>
 * </ul>
 * Adjacent pieces of one category are one piece. Everything else, other names and white space, is plain.
 */
final class JavaLexer implements CodeLexer {

    private static final Set<String> KEYWORDS = Set.of("abstract", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
            "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short",
            "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
            "void", "volatile", "while", "_",
            // Contextual keywords of classes and statements ("non-sealed" is read from "non"). Those of a module's
            // declaration (module, requires, exports, opens, to, uses, provides, with, transitive, open) are left out:
            // a lexer sees no module declaration, and "to", "with" or "open" are common names in ordinary code.
            "var", "record", "yield", "sealed", "permits", "when");

    private static final Set<String> LITERALS = Set.of("true", "false", "null");

    /**
     * A number from its first character on: hex (with a hex fraction and a binary exponent, as in {@code 0x1.8p3}),
     * binary, or decimal and octal (with a fraction and an exponent), each with underscores and a suffix.
     */
    private static final Pattern NUMBER = Pattern.compile(
            "0[xX](?:[0-9a-fA-F_]+\\.?[0-9a-fA-F_]*|\\.[0-9a-fA-F_]+)(?:[pP][+-]?[0-9_]+)?[lLfFdD]?"
                    + "|0[bB][01_]+[lL]?"
                    + "|(?:[0-9][0-9_]*(?:\\.[0-9_]*)?|\\.[0-9][0-9_]*)(?:[eE][+-]?[0-9_]+)?[lLfFdD]?");

    private static final String OPERATORS = "=+-*/%!~?:<>&|^";
    private static final String PUNCTUATION = "()[]{};,.";

    @Override
    public List<CodePiece> lex(String code) {
        Pieces pieces = new Pieces();
        int length = code.length();
        int position = 0;
        while (position < length) {
            char character = code.charAt(position);
            if (code.startsWith("//", position)) {
                int end = Scanning.lineEnd(code, position);
                pieces.add(position, end, CodeCategory.COMMENT);
                position = end;
            } else if (code.startsWith("/*", position)) {
                position = blockComment(code, position, pieces);
            } else if (code.startsWith("\"\"\"", position)) {
                position = quoted(code, position, "\"\"\"", pieces);
            } else if (character == '"') {
                position = quoted(code, position, "\"", pieces);
            } else if (character == '\'') {
                position = quoted(code, position, "'", pieces);
            } else if (character == '@') {
                position = annotation(code, position, pieces);
            } else if (Character.isJavaIdentifierStart(code.codePointAt(position))) {
                position = name(code, position, pieces);
            } else if (Scanning.isDigit(character) || character == '.' && position + 1 < length
                    && Scanning.isDigit(code.charAt(position + 1))) {
                position = number(code, position, pieces);
            } else if (OPERATORS.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.OPERATOR);
                position++;
            } else if (PUNCTUATION.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                position++;
            } else {
                position++;
            }
        }
        return pieces.toList();
    }

    /**
     * A name: a keyword, a literal, a function if a bracket follows it, a constant in capitals, a type with a capital
     * first, else plain.
     *
     * @return the position after the name
     */
    private static int name(String code, int start, Pieces pieces) {
        int end = identifierEnd(code, start);
        String name = code.substring(start, end);
        if (name.equals("non") && code.startsWith("-sealed", end) && identifierEnd(code, end + 1) == end + 7) {
            pieces.add(start, end + 7, CodeCategory.KEYWORD);
            return end + 7;
        }
        if (KEYWORDS.contains(name)) {
            pieces.add(start, end, CodeCategory.KEYWORD);
        } else if (LITERALS.contains(name)) {
            pieces.add(start, end, CodeCategory.LITERAL);
        } else if (Scanning.bracketFollows(code, end)) {
            pieces.add(start, end, CodeCategory.FUNCTION);
        } else if (isConstant(name)) {
            pieces.add(start, end, CodeCategory.CONSTANT);
        } else if (Character.isUpperCase(name.codePointAt(0))) {
            pieces.add(start, end, CodeCategory.TYPE);
        }
        return end;
    }

    /**
     * An annotation: {@code @} and a name, qualified or not ({@code @Override}, {@code @java.lang.Deprecated}), or the
     * keyword {@code @interface}; a lone {@code @} is plain.
     *
     * @return the position after it
     */
    private static int annotation(String code, int at, Pieces pieces) {
        int position = at + 1;
        if (position >= code.length() || !Character.isJavaIdentifierStart(code.codePointAt(position))) {
            return position;
        }
        int end = identifierEnd(code, position);
        if (code.substring(position, end).equals("interface")) {
            pieces.add(at, end, CodeCategory.KEYWORD);
            return end;
        }
        while (end + 1 < code.length() && code.charAt(end) == '.'
                && Character.isJavaIdentifierStart(code.codePointAt(end + 1))) {
            end = identifierEnd(code, end + 1);
        }
        pieces.add(at, end, CodeCategory.ANNOTATION);
        return end;
    }

    /**
     * A number, if it is a whole word: decimal, hex, binary or octal, with underscores, a fraction, an exponent and a
     * suffix. A number that runs into letters or digits ({@code 12ab}, {@code 0x}) is no number: the whole word stays
     * plain.
     *
     * @return the position after the number or the word
     */
    private static int number(String code, int start, Pieces pieces) {
        Matcher matcher = NUMBER.matcher(code).region(start, code.length());
        int end = matcher.lookingAt() ? matcher.end() : start;
        if (end > start && (end >= code.length() || !Character.isJavaIdentifierPart(code.codePointAt(end)))) {
            pieces.add(start, end, CodeCategory.NUMBER);
            return end;
        }
        return identifierEnd(code, start + 1);
    }

    /**
     * The end of the identifier characters from a position on.
     */
    private static int identifierEnd(String code, int start) {
        int position = start;
        while (position < code.length() && Character.isJavaIdentifierPart(code.codePointAt(position))) {
            position += Character.charCount(code.codePointAt(position));
        }
        return position;
    }

    /**
     * Whether a name is a constant's: in capitals only, starting with a capital and at least two characters long
     * ({@code MAX_ROWS}, {@code ID}, {@code UTF_8}).
     */
    private static boolean isConstant(String name) {
        if (name.length() < 2 || !Character.isUpperCase(name.codePointAt(0))) {
            return false;
        }
        for (int index = 0; index < name.length(); index++) {
            if (Character.isLowerCase(name.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * A block comment from {@code /*} to its end, a doc comment if it starts with {@code /**} (but {@code /**}{@code /}
     * is an empty block comment); unterminated, it runs to the end of the code.
     *
     * @return the position after the comment
     */
    private static int blockComment(String code, int start, Pieces pieces) {
        boolean doc = code.startsWith("/**", start) && !code.startsWith("/**/", start);
        int close = code.indexOf("*/", start + 2);
        int end = close < 0 ? code.length() : close + 2;
        pieces.add(start, end, doc ? CodeCategory.DOC : CodeCategory.COMMENT);
        return end;
    }

    /**
     * A string, text block or character from its opening delimiter to its closing one, its escapes pieces of their
     * own; unterminated, it runs to the end of the code.
     *
     * @param delimiter {@code "}, {@code """} or {@code '}
     * @return the position after it
     */
    private static int quoted(String code, int start, String delimiter, Pieces pieces) {
        int length = code.length();
        int pieceStart = start;
        int position = start + delimiter.length();
        while (position < length) {
            if (code.charAt(position) == '\\') {
                int end = escapeEnd(code, position);
                pieces.add(pieceStart, position, CodeCategory.STRING);
                pieces.add(position, end, CodeCategory.ESCAPE);
                position = end;
                pieceStart = end;
            } else if (code.startsWith(delimiter, position)) {
                position += delimiter.length();
                pieces.add(pieceStart, position, CodeCategory.STRING);
                return position;
            } else {
                position++;
            }
        }
        pieces.add(pieceStart, length, CodeCategory.STRING);
        return length;
    }

    /**
     * The end of an escape at a backslash: a unicode escape (the backslash, one {@code u} or more and four hex
     * digits), an octal one ({@code \0} to {@code \377}), else the backslash and the character after it, whatever it is
     * (a line feed in a text block, or a character Java does not allow there).
     */
    private static int escapeEnd(String code, int backslash) {
        int length = code.length();
        int position = backslash + 1;
        if (position >= length) {
            return length;
        }
        char next = code.charAt(position);
        if (next == 'u') {
            while (position < length && code.charAt(position) == 'u') {
                position++;
            }
            int digits = 0;
            while (digits < 4 && position < length && Character.digit(code.charAt(position), 16) >= 0) {
                position++;
                digits++;
            }
            return position;
        }
        if (next >= '0' && next <= '7') {
            int maximum = next <= '3' ? 3 : 2;
            int digits = 0;
            while (digits < maximum && position < length && code.charAt(position) >= '0'
                    && code.charAt(position) <= '7') {
                position++;
                digits++;
            }
            return position;
        }
        // Both halves of a surrogate pair, so a run never splits one.
        return Character.isHighSurrogate(next) && position + 1 < length ? position + 2 : position + 1;
    }
}
