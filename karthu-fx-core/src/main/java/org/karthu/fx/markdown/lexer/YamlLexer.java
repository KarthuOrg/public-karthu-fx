package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodeLexer;
import org.karthu.fx.markdown.CodePiece;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lexer of YAML: a scanner over the lines, without parsing, so YAML that does not parse is coloured too. It marks:
 * <ul>
 *   <li>comments: {@code #} at a line's start or after white space, to the line's end;</li>
 *   <li>keys: a scalar followed by {@code :} and white space or the line's end (property), plain or quoted;</li>
 *   <li>strings in single or double quotes, over lines too (string), the escapes of double quotes pieces of their own
 *       (escape); an unterminated string runs to the end of the code;</li>
 *   <li>a plain value that is a number as a whole ({@code 12}, {@code -2.5e3}, {@code 0x1F}, {@code .inf}) as a
 *       number, one that is {@code true}, {@code false}, {@code null} (in YAML's three spellings each, as
 *       {@code True} and {@code TRUE}) or {@code ~} as a literal; any other plain value is plain;</li>
 *   <li>anchors {@code &name} and aliases {@code *name} at a value's start (variable);</li>
 *   <li>document markers {@code ---} and {@code ...} at a line's start, list dashes, the {@code :} after a key, the
 *       brackets and commas of flow collections and the header of a block scalar ({@code |}, {@code >-}) as
 *       punctuation.</li>
 * </ul>
 * The lines of a block scalar, those after its header indented deeper than the header's line and blank ones between
 * them, are text: plain. Adjacent pieces of one category are one piece. Everything else is plain.
 */
final class YamlLexer implements CodeLexer {

    private static final Set<String> LITERALS = Set.of("true", "True", "TRUE", "false", "False", "FALSE", "null",
            "Null", "NULL", "~");

    /**
     * A number of YAML's core schema as a whole value: decimal with a sign, a fraction and an exponent, octal, hex,
     * infinity and not-a-number.
     */
    private static final Pattern NUMBER = Pattern.compile(
            "[-+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?"
                    + "|0o[0-7]+|0x[0-9a-fA-F]+"
                    + "|[-+]?\\.(?:inf|Inf|INF)|\\.(?:nan|NaN|NAN)");

    /**
     * The header of a block scalar after its indicator: chomping and indentation indicators, then white space up to
     * the line's end, or white space and a comment.
     */
    private static final Pattern BLOCK_SCALAR_HEADER = Pattern.compile("[-+0-9]*(?=[ \\t\\r]*(?:$|\\n)|[ \\t\\r]+#)");

    private static final String FLOW_INDICATORS = ",[]{}";

    @Override
    public List<CodePiece> lex(String code) {
        Pieces pieces = new Pieces();
        int length = code.length();
        int position = 0;
        // How deep flow collections ({...}, [...]) are nested at the position.
        int flowDepth = 0;
        // Whether only indentation and list dashes stand before the position on its line.
        boolean lineHead = true;
        // The indentation of the line that holds the current position.
        int lineIndent = 0;
        // While in a block scalar, the indentation of its header's line, which its lines must exceed; else -1.
        int blockScalarIndent = -1;
        while (position < length) {
            if (position == 0 || code.charAt(position - 1) == '\n') {
                lineIndent = indentation(code, position);
                int end = Scanning.lineEnd(code, position);
                boolean blank = isBlank(code, position, end);
                if (blockScalarIndent >= 0 && (blank || lineIndent > blockScalarIndent)) {
                    // A line of the block scalar's text, plain.
                    position = Math.min(end + 1, length);
                    continue;
                }
                blockScalarIndent = -1;
                lineHead = true;
                if (flowDepth == 0 && isDocumentMarker(code, position)) {
                    pieces.add(position, position + 3, CodeCategory.PUNCTUATION);
                    position += 3;
                    lineHead = false;
                    continue;
                }
            }
            char character = code.charAt(position);
            if (isBreakOrWhite(character)) {
                position++;
            } else if (character == '#' && (position == 0 || isBreakOrWhite(code.charAt(position - 1)))) {
                int end = Scanning.lineEnd(code, position);
                pieces.add(position, end, CodeCategory.COMMENT);
                position = end;
            } else if (character == '-' && lineHead && flowDepth == 0 && isBreakOrWhiteAt(code, position + 1)) {
                pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                position++;
            } else {
                lineHead = false;
                if (character == '[' || character == '{') {
                    pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                    flowDepth++;
                    position++;
                } else if (character == ']' || character == '}') {
                    pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                    flowDepth = Math.max(0, flowDepth - 1);
                    position++;
                } else if (character == ',' && flowDepth > 0) {
                    pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                    position++;
                } else if (character == ':' && (isBreakOrWhiteAt(code, position + 1) || flowDepth > 0)) {
                    pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                    position++;
                } else if ((character == '&' || character == '*') && anchorEnd(code, position + 1) > position + 1) {
                    int end = anchorEnd(code, position + 1);
                    pieces.add(position, end, CodeCategory.VARIABLE);
                    position = end;
                } else if ((character == '|' || character == '>') && flowDepth == 0
                        && blockScalarHeaderEnd(code, position + 1) >= 0) {
                    int end = blockScalarHeaderEnd(code, position + 1);
                    pieces.add(position, end, CodeCategory.PUNCTUATION);
                    blockScalarIndent = lineIndent;
                    position = end;
                } else if (character == '"' || character == '\'') {
                    position = quoted(code, position, pieces);
                } else {
                    position = plainScalar(code, position, flowDepth > 0, pieces);
                }
            }
        }
        return pieces.toList();
    }

    /**
     * A string in double or single quotes, from its opening quote to its closing one, over lines too: a key if a
     * {@code :} follows it on its line, the key a property and the {@code :} punctuation; else a string, with the
     * escapes of double quotes as pieces of their own. Unterminated, it runs to the end of the code.
     *
     * @return the position after the string, or after the {@code :} of a key
     */
    private static int quoted(String code, int start, Pieces pieces) {
        boolean doubleQuoted = code.charAt(start) == '"';
        int end = doubleQuoted ? doubleQuotedEnd(code, start) : singleQuotedEnd(code, start);
        int colon = end;
        while (colon < code.length() && isWhite(code.charAt(colon))) {
            colon++;
        }
        if (colon < code.length() && code.charAt(colon) == ':') {
            pieces.add(start, end, CodeCategory.PROPERTY);
            pieces.add(colon, colon + 1, CodeCategory.PUNCTUATION);
            return colon + 1;
        }
        if (!doubleQuoted) {
            pieces.add(start, end, CodeCategory.STRING);
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
     * The end of a string in double quotes: the position after its closing quote, or the end of the code.
     */
    private static int doubleQuotedEnd(String code, int start) {
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
     * The end of a string in single quotes, where {@code ''} is a quote: the position after its closing quote, or the
     * end of the code.
     */
    private static int singleQuotedEnd(String code, int start) {
        int position = start + 1;
        while (position < code.length()) {
            if (code.charAt(position) == '\'') {
                if (!code.startsWith("''", position)) {
                    return position + 1;
                }
                position += 2;
            } else {
                position++;
            }
        }
        return code.length();
    }

    /**
     * The end of an escape in double quotes at a backslash: {@code \x}, {@code \}{@code u} and {@code \U} with up to
     * two, four and eight hex digits, else the backslash and the character after it, whatever it is (a line feed
     * too).
     */
    private static int escapeEnd(String code, int backslash) {
        int length = code.length();
        int position = backslash + 1;
        if (position >= length) {
            return length;
        }
        char next = code.charAt(position);
        int maximum = next == 'x' ? 2 : next == 'u' ? 4 : next == 'U' ? 8 : 0;
        if (maximum > 0) {
            position++;
            int digits = 0;
            while (digits < maximum && position < length && Character.digit(code.charAt(position), 16) >= 0) {
                position++;
                digits++;
            }
            return position;
        }
        // Both halves of a surrogate pair, so a run never splits one.
        return Character.isHighSurrogate(next) && position + 1 < length ? position + 2 : position + 1;
    }

    /**
     * A plain scalar, unquoted text up to the line's end, a comment, a {@code :} that ends a key, or in a flow
     * collection its brackets and commas: a property if such a {@code :} ends it, else a number or a literal if it is
     * one as a whole, else plain.
     *
     * @return the position after the scalar, before the white space that ends it
     */
    private static int plainScalar(String code, int start, boolean inFlow, Pieces pieces) {
        int length = code.length();
        int position = start;
        while (position < length) {
            char character = code.charAt(position);
            if (character == '\n' || character == ':' && isKeyEnd(code, position + 1, inFlow)
                    || character == '#' && position > start && isWhite(code.charAt(position - 1))
                    || inFlow && FLOW_INDICATORS.indexOf(character) >= 0) {
                break;
            }
            position++;
        }
        int end = position;
        while (end > start && isWhite(code.charAt(end - 1))) {
            end--;
        }
        if (end == start) {
            return start + 1;
        }
        String scalar = code.substring(start, end);
        if (position < length && code.charAt(position) == ':') {
            pieces.add(start, end, CodeCategory.PROPERTY);
        } else if (NUMBER.matcher(scalar).matches()) {
            pieces.add(start, end, CodeCategory.NUMBER);
        } else if (LITERALS.contains(scalar)) {
            pieces.add(start, end, CodeCategory.LITERAL);
        }
        return end;
    }

    /**
     * The end of an anchor's or alias's name after its {@code &} or {@code *}: the first white space, line feed or
     * bracket or comma of a flow collection. The position itself if no name follows, so the {@code &} or {@code *}
     * is text.
     */
    private static int anchorEnd(String code, int nameStart) {
        int end = nameStart;
        while (end < code.length() && !isBreakOrWhite(code.charAt(end))
                && FLOW_INDICATORS.indexOf(code.charAt(end)) < 0) {
            end++;
        }
        return end;
    }

    /**
     * Whether a {@code :} before a position ends a key: white space, a line feed or the end of the code follow it, or
     * in a flow collection its brackets or a comma.
     */
    private static boolean isKeyEnd(String code, int position, boolean inFlow) {
        return isBreakOrWhiteAt(code, position) || inFlow && FLOW_INDICATORS.indexOf(code.charAt(position)) >= 0;
    }

    /**
     * The end of a block scalar's header after its indicator {@code |} or {@code >}: its chomping and indentation
     * indicators, if only white space and perhaps a comment follow them on the line; else -1, the indicator is text.
     */
    private static int blockScalarHeaderEnd(String code, int afterIndicator) {
        Matcher matcher = BLOCK_SCALAR_HEADER.matcher(code).region(afterIndicator, code.length());
        return matcher.lookingAt() ? matcher.end() : -1;
    }

    /**
     * Whether a document marker, {@code ---} or {@code ...} followed by white space or the line's end, starts at a
     * line's start.
     */
    private static boolean isDocumentMarker(String code, int lineStart) {
        return (code.startsWith("---", lineStart) || code.startsWith("...", lineStart))
                && isBreakOrWhiteAt(code, lineStart + 3);
    }

    /**
     * The number of spaces a line starts with.
     */
    private static int indentation(String code, int lineStart) {
        int position = lineStart;
        while (position < code.length() && code.charAt(position) == ' ') {
            position++;
        }
        return position - lineStart;
    }

    /**
     * Whether a line holds white space only.
     */
    private static boolean isBlank(String code, int lineStart, int lineEnd) {
        for (int position = lineStart; position < lineEnd; position++) {
            if (!isWhite(code.charAt(position))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the code ends at a position, or white space or a line feed stands there.
     */
    private static boolean isBreakOrWhiteAt(String code, int position) {
        return position >= code.length() || isBreakOrWhite(code.charAt(position));
    }

    private static boolean isBreakOrWhite(char character) {
        return character == '\n' || isWhite(character);
    }

    /**
     * Whether a character is white space inside a line: a space, a tab or a carriage return.
     */
    private static boolean isWhite(char character) {
        return character == ' ' || character == '\t' || character == '\r';
    }
}
