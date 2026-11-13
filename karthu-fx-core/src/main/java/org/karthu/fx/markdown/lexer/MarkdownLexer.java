package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodeLexer;
import org.karthu.fx.markdown.CodePiece;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lexer of Markdown source: a scanner over the lines and their characters, without parsing, so Markdown that is
 * not well formed is coloured too. A line's leading spaces and tabs are passed over, so the structures inside a list
 * item are found too. It marks:
 * <ul>
 *   <li>a fenced code block, from its opening fence of three or more backticks or tildes through its closing fence,
 *       whole (code), its content not coloured as its language; unterminated, it runs to the end of the code;</li>
 *   <li>an ATX heading, one to six {@code #} and a space, the whole line (heading);</li>
 *   <li>a thematic break, three or more {@code *}, {@code -} or {@code _} alone on their line (punctuation);</li>
 *   <li>a quote line, from its {@code >} to its end (quote);</li>
 *   <li>a link reference definition, {@code [label]: address}, the whole line (link);</li>
 *   <li>a list marker, {@code -}, {@code *}, {@code +}, {@code 1.} or {@code 1)} followed by a space (list);</li>
 *   <li>inside the other lines: inline code between equal runs of backticks (code), links {@code [text](address)},
 *       images {@code ![text](address)}, reference links {@code [text][label]} and addresses in angle brackets
 *       {@code <https://...>} or {@code <name@host>} (link), emphasis and strong emphasis between equal runs of
 *       {@code *} or {@code _} (emphasis).</li>
 * </ul>
 * Inline pieces stay on their line. Emphasis opens at a run followed by a character other than white space and closes
 * at a run as long, preceded by one; an underscore neither opens after a letter or digit nor closes before one, so
 * {@code snake_case} has none. A backslash takes the character after it as plain text. A heading, a quote line or a
 * link reference definition is one piece, the emphasis, code or links in it included. HTML stays plain. Adjacent
 * pieces of one category are one piece.
 */
final class MarkdownLexer implements CodeLexer {

    private static final Pattern HEADING = Pattern.compile("#{1,6}(?:[ \\t].*)?");
    private static final Pattern THEMATIC_BREAK = Pattern.compile(
            "(?:\\*[ \\t]*){3,}|(?:-[ \\t]*){3,}|(?:_[ \\t]*){3,}");
    private static final Pattern LIST_MARKER = Pattern.compile("(?:[-*+]|[0-9]{1,9}[.)])(?=[ \\t]|$)");
    private static final Pattern REFERENCE_DEFINITION = Pattern.compile("\\[(?:[^\\]\\\\]|\\\\.)+]:[ \\t]*\\S.*");

    /**
     * An address in angle brackets from its {@code <} on: a scheme and what follows it, or an e-mail address.
     */
    private static final Pattern AUTOLINK = Pattern.compile(
            "<(?:[A-Za-z][A-Za-z0-9+.-]{1,31}:[^\\s<>]*|[^\\s<>@]+@[^\\s<>@]+)>");

    @Override
    public List<CodePiece> lex(String code) {
        Pieces pieces = new Pieces();
        int length = code.length();
        int lineStart = 0;
        while (lineStart < length) {
            int lineEnd = Scanning.lineEnd(code, lineStart);
            int indentEnd = lineStart;
            while (indentEnd < lineEnd && (code.charAt(indentEnd) == ' ' || code.charAt(indentEnd) == '\t')) {
                indentEnd++;
            }
            String rest = code.substring(indentEnd, lineEnd);
            int fence = fenceLength(rest);
            if (fence > 0) {
                lineEnd = fencedBlockEnd(code, lineEnd, rest.charAt(0), fence);
                pieces.add(indentEnd, lineEnd, CodeCategory.CODE);
            } else if (HEADING.matcher(rest).matches()) {
                pieces.add(indentEnd, lineEnd, CodeCategory.HEADING);
            } else if (THEMATIC_BREAK.matcher(rest).matches()) {
                pieces.add(indentEnd, lineEnd, CodeCategory.PUNCTUATION);
            } else if (rest.startsWith(">")) {
                pieces.add(indentEnd, lineEnd, CodeCategory.QUOTE);
            } else if (REFERENCE_DEFINITION.matcher(rest).matches()) {
                pieces.add(indentEnd, lineEnd, CodeCategory.LINK);
            } else {
                Matcher marker = LIST_MARKER.matcher(rest);
                int textStart = indentEnd;
                if (marker.lookingAt()) {
                    textStart = indentEnd + marker.end();
                    pieces.add(indentEnd, textStart, CodeCategory.LIST);
                }
                inline(code, textStart, lineEnd, pieces);
            }
            lineStart = lineEnd + 1;
        }
        return pieces.toList();
    }

    /**
     * The length of the opening fence a line starts with, three or more backticks or tildes, or 0 if it opens none.
     * A backtick fence's info string holds no backtick.
     *
     * @param rest the line without its leading spaces and tabs
     */
    private static int fenceLength(String rest) {
        if (rest.isEmpty() || rest.charAt(0) != '`' && rest.charAt(0) != '~') {
            return 0;
        }
        char character = rest.charAt(0);
        int length = run(rest, 0, rest.length(), character);
        if (length < 3 || character == '`' && rest.indexOf('`', length) >= 0) {
            return 0;
        }
        return length;
    }

    /**
     * The end of a fenced code block whose opening fence ends a line: the end of its closing fence's line, a line of
     * at least as many of the fence's characters and nothing else but white space, or the end of the code.
     *
     * @param openingEnd the end of the opening fence's line
     * @return the end of the block's last line, before its line feed
     */
    private static int fencedBlockEnd(String code, int openingEnd, char character, int fence) {
        int lineStart = openingEnd + 1;
        while (lineStart < code.length()) {
            int lineEnd = Scanning.lineEnd(code, lineStart);
            String line = code.substring(lineStart, lineEnd).strip();
            if (line.length() >= fence && run(line, 0, line.length(), character) == line.length()) {
                return lineEnd;
            }
            lineStart = lineEnd + 1;
        }
        return code.length();
    }

    /**
     * The inline pieces of a line's text: inline code, links and emphasis.
     */
    private static void inline(String code, int start, int end, Pieces pieces) {
        int position = start;
        while (position < end) {
            char character = code.charAt(position);
            if (character == '\\') {
                position = Math.min(end, position + 2);
            } else if (character == '`') {
                position = codeSpan(code, position, end, pieces);
            } else if (character == '[' || character == '!' && position + 1 < end && code.charAt(position + 1) == '[') {
                position = link(code, position, end, pieces);
            } else if (character == '<') {
                position = autolink(code, position, end, pieces);
            } else if (character == '*' || character == '_') {
                position = emphasis(code, position, end, pieces);
            } else {
                position++;
            }
        }
    }

    /**
     * Inline code from a run of backticks to the next run as long on the line, as code; a run without one is plain.
     *
     * @return the position after the code, or after the run
     */
    private static int codeSpan(String code, int start, int end, Pieces pieces) {
        int length = run(code, start, end, '`');
        int position = start + length;
        while (position < end) {
            if (code.charAt(position) == '`') {
                int closing = run(code, position, end, '`');
                if (closing == length) {
                    pieces.add(start, position + closing, CodeCategory.CODE);
                    return position + closing;
                }
                position += closing;
            } else {
                position++;
            }
        }
        return start + length;
    }

    /**
     * A link {@code [text](address)}, an image {@code ![text](address)} or a reference link {@code [text][label]},
     * whole, as link; brackets that make none are plain, and what is inside them is read on.
     *
     * @return the position after the link, or after its first character
     */
    private static int link(String code, int start, int end, Pieces pieces) {
        int open = code.charAt(start) == '!' ? start + 1 : start;
        int textEnd = closing(code, open, end, '[', ']');
        if (textEnd < end && textEnd + 1 < end) {
            char next = code.charAt(textEnd + 1);
            int linkEnd = -1;
            if (next == '(') {
                linkEnd = closing(code, textEnd + 1, end, '(', ')');
            } else if (next == '[') {
                linkEnd = closing(code, textEnd + 1, end, '[', ']');
            }
            if (linkEnd >= 0 && linkEnd < end) {
                pieces.add(start, linkEnd + 1, CodeCategory.LINK);
                return linkEnd + 1;
            }
        }
        return start + 1;
    }

    /**
     * The position of the bracket that closes the one at a position, brackets inside counted and escaped ones passed
     * over, or the line's end if none does.
     */
    private static int closing(String code, int open, int end, char opening, char close) {
        int depth = 0;
        int position = open;
        while (position < end) {
            char character = code.charAt(position);
            if (character == '\\') {
                position += 2;
                continue;
            }
            if (character == opening) {
                depth++;
            } else if (character == close) {
                depth--;
                if (depth == 0) {
                    return position;
                }
            }
            position++;
        }
        return end;
    }

    /**
     * An address in angle brackets as link; any other {@code <}, an HTML tag's for one, is plain.
     *
     * @return the position after the address, or after the bracket
     */
    private static int autolink(String code, int start, int end, Pieces pieces) {
        Matcher matcher = AUTOLINK.matcher(code).region(start, end);
        if (matcher.lookingAt()) {
            pieces.add(start, matcher.end(), CodeCategory.LINK);
            return matcher.end();
        }
        return start + 1;
    }

    /**
     * Emphasis from a run of {@code *} or {@code _} that can open it to the next run of the same character and length
     * on the line that can close it, as emphasis; a run without one is plain.
     *
     * @return the position after the emphasis, or after the run
     */
    private static int emphasis(String code, int start, int end, Pieces pieces) {
        char character = code.charAt(start);
        int length = run(code, start, end, character);
        int after = start + length;
        boolean opens = after < end && !Character.isWhitespace(code.charAt(after))
                && (character == '*' || start == 0 || !Character.isLetterOrDigit(code.charAt(start - 1)));
        if (!opens) {
            return after;
        }
        int position = after;
        while (position < end) {
            char current = code.charAt(position);
            if (current == '\\') {
                position += 2;
            } else if (current == character) {
                int closing = run(code, position, end, character);
                int closingEnd = position + closing;
                boolean closes = closing == length && !Character.isWhitespace(code.charAt(position - 1))
                        && (character == '*' || closingEnd >= end
                        || !Character.isLetterOrDigit(code.charAt(closingEnd)));
                if (closes) {
                    pieces.add(start, closingEnd, CodeCategory.EMPHASIS);
                    return closingEnd;
                }
                position = closingEnd;
            } else {
                position++;
            }
        }
        return after;
    }

    /**
     * The length of the run of a character from a position on, up to an end.
     */
    private static int run(String text, int start, int end, char character) {
        int position = start;
        while (position < end && text.charAt(position) == character) {
            position++;
        }
        return position - start;
    }
}
