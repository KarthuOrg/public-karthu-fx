package org.karthu.fx.markdown.lexer;

import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodeLexer;
import org.karthu.fx.markdown.CodePiece;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lexer of XML and HTML: a scanner over the characters, without parsing, so markup that is not well formed is
 * coloured too. It marks:
 * <ul>
 *   <li>comments {@code <!-- -->} (comment);</li>
 *   <li>as tag: the opening bracket with the tag's name ({@code <a}, {@code </a}), the closing bracket ({@code >},
 *       {@code />}), processing instructions ({@code <?xml ... ?>}) and declarations such as the doctype, whole;</li>
 *   <li>attribute names (attribute), the equals sign after them (punctuation) and attribute values, in quotes or, as
 *       HTML allows, without (string);</li>
 *   <li>entities such as {@code &amp;}, {@code &#169;} and {@code &#x1F600;}, in text and in attribute values
 *       (escape);</li>
 *   <li>the content of a CDATA section (string), its delimiters {@code <![CDATA[} and {@code ]]>} as tag.</li>
 * </ul>
 * Text between tags stays plain, and so does the content of a {@code script} or {@code style} element, which is not
 * read as markup either: no language is embedded. Those two names are matched in lower case, as HTML and SVG write
 * them, so an XML element {@code Style} keeps its child elements. A comment, a CDATA section, a processing
 * instruction, a declaration, an attribute value or a script that is not closed runs to the end of the code. A tag
 * that is not closed ends at the next {@code <}. Adjacent pieces of one category are one piece.
 */
final class XmlLexer implements CodeLexer {

    /**
     * An entity from its ampersand on: named, decimal or hex.
     */
    private static final Pattern ENTITY = Pattern.compile("&(?:[A-Za-z][A-Za-z0-9]*|#[0-9]+|#[xX][0-9a-fA-F]+);");

    private static final String CDATA_OPEN = "<![CDATA[";
    private static final String CDATA_CLOSE = "]]>";

    @Override
    public List<CodePiece> lex(String code) {
        Pieces pieces = new Pieces();
        int length = code.length();
        int position = 0;
        while (position < length) {
            char character = code.charAt(position);
            if (code.startsWith("<!--", position)) {
                position = through(code, position, "-->", CodeCategory.COMMENT, pieces);
            } else if (code.startsWith(CDATA_OPEN, position)) {
                position = cdata(code, position, pieces);
            } else if (code.startsWith("<?", position)) {
                position = through(code, position, "?>", CodeCategory.TAG, pieces);
            } else if (code.startsWith("<!", position)) {
                position = declaration(code, position, pieces);
            } else if (code.startsWith("</", position) && isNameStart(code, position + 2)) {
                position = tag(code, position, nameEnd(code, position + 2), pieces);
            } else if (character == '<' && isNameStart(code, position + 1)) {
                int nameEnd = nameEnd(code, position + 1);
                String name = code.substring(position + 1, nameEnd);
                position = tag(code, position, nameEnd, pieces);
                // An element's content follows a start tag closed by ">", none follows an empty element tag "/>".
                boolean content = code.charAt(position - 1) == '>' && code.charAt(position - 2) != '/';
                if (content && isRawText(name)) {
                    position = rawText(code, position, name);
                }
            } else if (character == '&') {
                position = entity(code, position, pieces);
            } else {
                position++;
            }
        }
        return pieces.toList();
    }

    /**
     * Whether the content of an element of this name is raw text, which is neither markup nor coloured.
     */
    private static boolean isRawText(String name) {
        return name.equals("script") || name.equals("style");
    }

    /**
     * A piece from a position through a closing delimiter, or to the end of the code if none follows.
     *
     * @return the position after the piece
     */
    private static int through(String code, int start, String close, CodeCategory category, Pieces pieces) {
        int found = code.indexOf(close, start + 2);
        int end = found < 0 ? code.length() : found + close.length();
        pieces.add(start, end, category);
        return end;
    }

    /**
     * A CDATA section: its delimiters as tag, its content as string; unterminated, the content runs to the end of the
     * code.
     *
     * @return the position after the section
     */
    private static int cdata(String code, int start, Pieces pieces) {
        int contentStart = start + CDATA_OPEN.length();
        pieces.add(start, contentStart, CodeCategory.TAG);
        int close = code.indexOf(CDATA_CLOSE, contentStart);
        if (close < 0) {
            pieces.add(contentStart, code.length(), CodeCategory.STRING);
            return code.length();
        }
        pieces.add(contentStart, close, CodeCategory.STRING);
        pieces.add(close, close + CDATA_CLOSE.length(), CodeCategory.TAG);
        return close + CDATA_CLOSE.length();
    }

    /**
     * A declaration such as the doctype, whole, as tag: up to the {@code >} that is neither in quotes nor in an
     * internal subset's brackets; unterminated, to the end of the code.
     *
     * @return the position after it
     */
    private static int declaration(String code, int start, Pieces pieces) {
        int length = code.length();
        int position = start + 2;
        int brackets = 0;
        char quote = 0;
        while (position < length) {
            char character = code.charAt(position);
            position++;
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                }
            } else if (character == '"' || character == '\'') {
                quote = character;
            } else if (character == '[') {
                brackets++;
            } else if (character == ']') {
                brackets = Math.max(0, brackets - 1);
            } else if (character == '>' && brackets == 0) {
                break;
            }
        }
        pieces.add(start, position, CodeCategory.TAG);
        return position;
    }

    /**
     * A start or end tag from its bracket on: the bracket with the name as tag, then attribute names, equals signs
     * and values, up to the closing {@code >} or {@code />} as tag. Unclosed, the tag ends before the next {@code <},
     * or at the end of the code.
     *
     * @param nameEnd the position after the tag's name
     * @return the position after the tag
     */
    private static int tag(String code, int start, int nameEnd, Pieces pieces) {
        int length = code.length();
        pieces.add(start, nameEnd, CodeCategory.TAG);
        int position = nameEnd;
        while (position < length) {
            char character = code.charAt(position);
            if (character == '>') {
                pieces.add(position, position + 1, CodeCategory.TAG);
                return position + 1;
            } else if (code.startsWith("/>", position)) {
                pieces.add(position, position + 2, CodeCategory.TAG);
                return position + 2;
            } else if (character == '<') {
                return position;
            } else if (character == '"' || character == '\'') {
                position = quotedValue(code, position, pieces);
            } else if (character == '=') {
                pieces.add(position, position + 1, CodeCategory.PUNCTUATION);
                position = unquotedValue(code, skipSpaces(code, position + 1), pieces);
            } else if (Character.isWhitespace(character) || character == '/') {
                position++;
            } else {
                int end = attributeNameEnd(code, position);
                pieces.add(position, end, CodeCategory.ATTRIBUTE);
                position = end;
            }
        }
        return length;
    }

    /**
     * An attribute value in quotes as string, its entities as escape; unterminated, it runs to the end of the code.
     *
     * @return the position after the value
     */
    private static int quotedValue(String code, int start, Pieces pieces) {
        int length = code.length();
        char quote = code.charAt(start);
        int pieceStart = start;
        int position = start + 1;
        while (position < length) {
            char character = code.charAt(position);
            if (character == quote) {
                pieces.add(pieceStart, position + 1, CodeCategory.STRING);
                return position + 1;
            }
            int entityEnd = character == '&' ? entityEnd(code, position) : position;
            if (entityEnd > position) {
                pieces.add(pieceStart, position, CodeCategory.STRING);
                pieces.add(position, entityEnd, CodeCategory.ESCAPE);
                position = entityEnd;
                pieceStart = entityEnd;
            } else {
                position++;
            }
        }
        pieces.add(pieceStart, length, CodeCategory.STRING);
        return length;
    }

    /**
     * An attribute value without quotes, as HTML allows it, up to white space or the tag's end, as string; nothing
     * if a quote or the tag's end follows the equals sign.
     *
     * @return the position after the value
     */
    private static int unquotedValue(String code, int start, Pieces pieces) {
        int position = start;
        while (position < code.length()) {
            char character = code.charAt(position);
            if (Character.isWhitespace(character) || character == '>' || character == '<' || character == '"'
                    || character == '\'' || code.startsWith("/>", position)) {
                break;
            }
            position++;
        }
        pieces.add(start, position, CodeCategory.STRING);
        return position;
    }

    /**
     * The raw text of a {@code script} or {@code style} element up to its end tag, plain; unterminated, to the end of
     * the code.
     *
     * @return the position of its end tag, or the end of the code
     */
    private static int rawText(String code, int start, String name) {
        int end = code.indexOf("</" + name, start);
        return end < 0 ? code.length() : end;
    }

    /**
     * An entity as escape, or a lone {@code &}, which stays plain.
     *
     * @return the position after it
     */
    private static int entity(String code, int start, Pieces pieces) {
        int end = entityEnd(code, start);
        if (end > start) {
            pieces.add(start, end, CodeCategory.ESCAPE);
            return end;
        }
        return start + 1;
    }

    /**
     * The end of an entity at an ampersand, or the ampersand's position if none starts there.
     */
    private static int entityEnd(String code, int ampersand) {
        Matcher matcher = ENTITY.matcher(code).region(ampersand, code.length());
        return matcher.lookingAt() ? matcher.end() : ampersand;
    }

    /**
     * Whether a tag's name starts at a position: a letter, {@code _} or {@code :}.
     */
    private static boolean isNameStart(String code, int position) {
        if (position >= code.length()) {
            return false;
        }
        int character = code.codePointAt(position);
        return Character.isLetter(character) || character == '_' || character == ':';
    }

    /**
     * The end of a tag's name: letters, digits, {@code _}, {@code :}, {@code -} and {@code .}.
     */
    private static int nameEnd(String code, int start) {
        int position = start;
        while (position < code.length()) {
            int character = code.codePointAt(position);
            if (!Character.isLetterOrDigit(character) && character != '_' && character != ':' && character != '-'
                    && character != '.') {
                break;
            }
            position += Character.charCount(character);
        }
        return position;
    }

    /**
     * The end of an attribute's name: everything up to white space, a quote, an equals sign, a bracket or a slash.
     */
    private static int attributeNameEnd(String code, int start) {
        int position = start;
        while (position < code.length()) {
            char character = code.charAt(position);
            if (Character.isWhitespace(character) || "\"'=<>/".indexOf(character) >= 0) {
                break;
            }
            position++;
        }
        return position;
    }

    /**
     * The position after spaces, tabs and line feeds.
     */
    private static int skipSpaces(String code, int start) {
        int position = start;
        while (position < code.length() && Character.isWhitespace(code.charAt(position))) {
            position++;
        }
        return position;
    }
}
