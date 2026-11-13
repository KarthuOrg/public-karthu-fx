package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodeLexer;
import org.karthu.fx.markdown.CodePiece;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The lexer of Bash scripts: a scanner over the characters, without parsing, so a script that does not run is coloured
 * too. It marks:
 * <ul>
 *   <li>comments: a {@code #} at the start of a word, to the line's end (comment), so {@code $#}, {@code ${#x}} and
 *       {@code a#b} start none;</li>
 *   <li>strings in single and double quotes, and {@code $'...'} (string); a variable inside double quotes stays in
 *       the string's piece, a keyword inside a string or a comment stays in it, and an unterminated string runs to the
 *       end of the code;</li>
 *   <li>the body of a here-document, from the line after {@code <<WORD} through its last line {@code WORD}, and the
 *       word itself (string), so a quote in the body opens no string; unterminated, it runs to the end of the
 *       code;</li>
 *   <li>variables: {@code $NAME}, {@code ${...}} (braces inside included), the positional {@code $0} to {@code $9}
 *       and the special {@code $?}, {@code $#}, {@code $@}, {@code $*}, {@code $$}, {@code $!} and {@code $-}
 *       (variable);</li>
 *   <li>the keywords {@code if}, {@code then}, {@code else}, {@code elif}, {@code fi}, {@code for}, {@code while},
 *       {@code until}, {@code do}, {@code done}, {@code case}, {@code esac}, {@code in}, {@code function},
 *       {@code select}, {@code time}, {@code coproc} and {@code return}, each as a whole word (keyword);</li>
 *   <li>the name of a function's definition, before {@code (} or after the keyword {@code function} (function);</li>
 *   <li>numbers as whole words (number), operators ({@code | & < > = !}) and punctuation (brackets, braces,
 *       {@code ;}, a backtick, the {@code $} of {@code $(}).</li>
 * </ul>
 * A backslash outside quotes takes the character after it as plain text. Adjacent pieces of one category are one
 * piece. Everything else, commands, their arguments and white space, is plain.
 */
final class BashLexer implements CodeLexer {

    private static final Set<String> KEYWORDS = Set.of("if", "then", "else", "elif", "fi", "for", "while", "until",
            "do", "done", "case", "esac", "in", "function", "return", "select", "time", "coproc");

    private static final Pattern NUMBER = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");

    private static final String OPERATORS = "|&<>=!";
    private static final String PUNCTUATION = "(){}[];`";

    /**
     * The characters that end a shell word and start a comment after them: white space and the metacharacters.
     */
    private static final String METACHARACTERS = ";|&()<>";

    /**
     * The special parameters after a {@code $}, beside the digits.
     */
    private static final String SPECIAL_PARAMETERS = "?#@*$!-";

    @Override
    public List<CodePiece> lex(String code) {
        Pieces pieces = new Pieces();
        List<HereDocument> hereDocuments = new ArrayList<>();
        int length = code.length();
        int position = 0;
        while (position < length) {
            char character = code.charAt(position);
            if (character == '\n' && !hereDocuments.isEmpty()) {
                position = hereDocumentBodies(code, position + 1, hereDocuments, pieces);
                hereDocuments.clear();
            } else if (character == '#' && endsWord(code, position - 1)) {
                int end = Scanning.lineEnd(code, position);
                pieces.add(position, end, CodeCategory.COMMENT);
                position = end;
            } else if (character == '\'') {
                position = quoted(code, position, position + 1, '\'', false, pieces);
            } else if (character == '"') {
                position = quoted(code, position, position + 1, '"', true, pieces);
            } else if (character == '$') {
                position = dollar(code, position, pieces);
            } else if (character == '\\') {
                position = Math.min(length, position + 2);
            } else if (code.startsWith("<<<", position)) {
                pieces.add(position, position + 3, CodeCategory.OPERATOR);
                position += 3;
            } else if (code.startsWith("<<", position)) {
                position = hereDocument(code, position, hereDocuments, pieces);
            } else if (OPERATORS.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.OPERATOR);
                position++;
            } else if (PUNCTUATION.indexOf(character) >= 0) {
                pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                position++;
            } else if (isWordCharacter(character)) {
                position = word(code, position, pieces);
            } else {
                position++;
            }
        }
        return pieces.toList();
    }

    /**
     * A word: a keyword, a number, the name of a function's definition, else plain. After the keyword
     * {@code function}, the next word is the function's name.
     *
     * @return the position after the word, or after the function's name
     */
    private static int word(String code, int start, Pieces pieces) {
        int end = wordEnd(code, start);
        String word = code.substring(start, end);
        boolean whole = endsWord(code, start - 1) && (end >= code.length()
                || Character.isWhitespace(code.charAt(end)) || METACHARACTERS.indexOf(code.charAt(end)) >= 0);
        if (whole && KEYWORDS.contains(word)) {
            pieces.add(start, end, CodeCategory.KEYWORD);
            if (word.equals("function")) {
                return functionName(code, end, pieces);
            }
        } else if (isNumber(code, start, end, word)) {
            pieces.add(start, end, CodeCategory.NUMBER);
        } else if (isName(word) && Scanning.bracketFollows(code, end)) {
            pieces.add(start, end, CodeCategory.FUNCTION);
        }
        return end;
    }

    /**
     * The name after the keyword {@code function}, as function.
     *
     * @return the position after the name, or after the spaces if no name follows
     */
    private static int functionName(String code, int start, Pieces pieces) {
        int position = start;
        while (position < code.length() && (code.charAt(position) == ' ' || code.charAt(position) == '\t')) {
            position++;
        }
        if (position >= code.length() || !isWordCharacter(code.charAt(position))) {
            return position;
        }
        int end = wordEnd(code, position);
        if (isName(code.substring(position, end))) {
            pieces.add(position, end, CodeCategory.FUNCTION);
        }
        return end;
    }

    /**
     * Whether a word is a number: digits with an optional fraction, a whole word between white space, operators or
     * punctuation, so neither {@code -1} nor the {@code 0} after {@code $1} is one.
     */
    private static boolean isNumber(String code, int start, int end, String word) {
        if (!NUMBER.matcher(word).matches()) {
            return false;
        }
        return (start == 0 || isBoundary(code.charAt(start - 1))) && (end >= code.length()
                || isBoundary(code.charAt(end)));
    }

    /**
     * Whether a character separates a number from what is around it: white space, an operator or punctuation.
     */
    private static boolean isBoundary(char character) {
        return Character.isWhitespace(character) || OPERATORS.indexOf(character) >= 0
                || PUNCTUATION.indexOf(character) >= 0;
    }

    /**
     * Whether a word can name a function: it starts with a letter or {@code _}.
     */
    private static boolean isName(String word) {
        return !word.isEmpty() && (Character.isLetter(word.charAt(0)) || word.charAt(0) == '_');
    }

    /**
     * Whether a word ends at a position, so that the next one starts after it: the start of the code, white space or
     * a metacharacter.
     *
     * @param position the character before the one in question, -1 at the start of the code
     */
    private static boolean endsWord(String code, int position) {
        if (position < 0) {
            return true;
        }
        char character = code.charAt(position);
        return Character.isWhitespace(character) || METACHARACTERS.indexOf(character) >= 0;
    }

    /**
     * Whether a character belongs to a plain word: anything but white space, an operator, punctuation, a quote, a
     * {@code $} or a backslash. A {@code #} inside a word is part of it.
     */
    private static boolean isWordCharacter(char character) {
        return !Character.isWhitespace(character) && OPERATORS.indexOf(character) < 0
                && PUNCTUATION.indexOf(character) < 0 && "'\"$\\".indexOf(character) < 0;
    }

    /**
     * The end of a plain word from a position on.
     */
    private static int wordEnd(String code, int start) {
        int position = start;
        while (position < code.length() && isWordCharacter(code.charAt(position))) {
            position++;
        }
        return position;
    }

    /**
     * What follows a {@code $}: a string in {@code $'...'} or {@code $"..."}, a variable, the {@code $} of a command
     * substitution or arithmetic as punctuation, else a plain {@code $}.
     *
     * @return the position after it
     */
    private static int dollar(String code, int start, Pieces pieces) {
        int next = start + 1;
        if (next >= code.length()) {
            return next;
        }
        char character = code.charAt(next);
        if (character == '\'' || character == '"') {
            return quoted(code, start, next + 1, character, true, pieces);
        }
        if (character == '{') {
            return braces(code, start, pieces);
        }
        if (character == '(') {
            pieces.add(start, next, CodeCategory.PUNCTUATION);
            return next;
        }
        if (Character.isLetter(character) || character == '_') {
            int end = next;
            while (end < code.length() && (Character.isLetterOrDigit(code.charAt(end)) || code.charAt(end) == '_')) {
                end++;
            }
            pieces.add(start, end, CodeCategory.VARIABLE);
            return end;
        }
        if (character >= '0' && character <= '9' || SPECIAL_PARAMETERS.indexOf(character) >= 0) {
            pieces.add(start, next + 1, CodeCategory.VARIABLE);
            return next + 1;
        }
        return next;
    }

    /**
     * A variable in braces, {@code ${...}}, up to the brace that closes it, braces inside counted; unterminated, it
     * runs to the end of the code.
     *
     * @return the position after it
     */
    private static int braces(String code, int start, Pieces pieces) {
        int depth = 0;
        int position = start + 1;
        while (position < code.length()) {
            char character = code.charAt(position);
            position++;
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
        }
        pieces.add(start, position, CodeCategory.VARIABLE);
        return position;
    }

    /**
     * A string up to its closing quote, as one piece; unterminated, it runs to the end of the code.
     *
     * @param start        the string's first character, a quote or a {@code $}
     * @param contentStart the character after the opening quote
     * @param quote        the closing quote
     * @param escapes      whether a backslash takes the character after it, so that an escaped quote does not close
     * @return the position after the string
     */
    private static int quoted(String code, int start, int contentStart, char quote, boolean escapes, Pieces pieces) {
        int length = code.length();
        int position = contentStart;
        while (position < length) {
            char character = code.charAt(position);
            if (character == '\\' && escapes) {
                position += 2;
            } else if (character == quote) {
                position++;
                pieces.add(start, position, CodeCategory.STRING);
                return position;
            } else {
                position++;
            }
        }
        pieces.add(start, length, CodeCategory.STRING);
        return length;
    }

    /**
     * The operator {@code <<} or {@code <<-} and, if a word follows it, the start of a here-document: the word, in
     * quotes or not, as string, and its body read after the line's end. A digit after the operator makes it a shift
     * ({@code $((1 << 2))}).
     *
     * @return the position after the word, or after the operator if no word follows
     */
    private static int hereDocument(String code, int start, List<HereDocument> hereDocuments, Pieces pieces) {
        boolean tabs = code.startsWith("<<-", start);
        int operatorEnd = start + (tabs ? 3 : 2);
        pieces.add(start, operatorEnd, CodeCategory.OPERATOR);
        int position = operatorEnd;
        while (position < code.length() && (code.charAt(position) == ' ' || code.charAt(position) == '\t')) {
            position++;
        }
        if (position >= code.length()) {
            return position;
        }
        char first = code.charAt(position);
        int wordEnd;
        String delimiter;
        if (first == '\'' || first == '"') {
            int close = code.indexOf(first, position + 1);
            int newline = code.indexOf('\n', position + 1);
            if (close < 0 || newline >= 0 && newline < close) {
                return position;
            }
            wordEnd = close + 1;
            delimiter = code.substring(position + 1, close);
        } else if (Character.isLetter(first) || first == '_' || first == '\\') {
            int wordStart = first == '\\' ? position + 1 : position;
            wordEnd = wordEnd(code, wordStart);
            delimiter = code.substring(wordStart, wordEnd);
        } else {
            return position;
        }
        if (delimiter.isEmpty()) {
            return position;
        }
        pieces.add(position, wordEnd, CodeCategory.STRING);
        hereDocuments.add(new HereDocument(delimiter, tabs));
        return wordEnd;
    }

    /**
     * The bodies of the here-documents a line started, one after the other from the next line on, each through its
     * last line, as string; unterminated, a body runs to the end of the code.
     *
     * @param start the first character of the first body's first line
     * @return the position after the last body's last line, at its line feed
     */
    private static int hereDocumentBodies(String code, int start, List<HereDocument> hereDocuments, Pieces pieces) {
        int length = code.length();
        int bodyStart = start;
        int position = start;
        for (HereDocument hereDocument : hereDocuments) {
            bodyStart = position;
            int end = length;
            while (position < length) {
                int lineEnd = Scanning.lineEnd(code, position);
                if (hereDocument.isLastLine(code.substring(position, lineEnd))) {
                    end = lineEnd;
                    break;
                }
                position = lineEnd + 1;
            }
            pieces.add(bodyStart, end, CodeCategory.STRING);
            if (end >= length) {
                return length;
            }
            position = end + 1;
        }
        return position - 1;
    }

    /**
     * A here-document a line started, whose body follows the line.
     *
     * @param delimiter the word on its last line, without quotes
     * @param tabs      whether its lines may start with tabs ({@code <<-}), the last line too
     */
    private record HereDocument(String delimiter, boolean tabs) {

        /**
         * Whether a line is the here-document's last.
         */
        boolean isLastLine(String line) {
            String word = line;
            if (tabs) {
                int index = 0;
                while (index < word.length() && word.charAt(index) == '\t') {
                    index++;
                }
                word = word.substring(index);
            }
            return word.equals(delimiter);
        }
    }
}
