package org.karthu.fx.markdown.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The inline level of the parser: turns the text of a heading or a paragraph into inlines.
 *
 * <p>The text is scanned once from left to right into a doubly linked list of nodes. Literal text collects in a
 * buffer and becomes a text node whenever another kind of node follows. Runs of {@code *} and {@code _} become text
 * nodes of their own and, if they can open or close emphasis, entries of a delimiter stack. After the scan, the
 * delimiter stack is resolved into emphasis by CommonMark's algorithm ("process emphasis"): every closer looks back
 * for the nearest matching opener, and the nodes between them move into an {@link Emphasis} or {@link Strong}
 * node. Delimiters that find no partner stay literal text.
 *
 * <p>Links use CommonMark's bracket stack the same way: a {@code [} (or an image's {@code ![}) becomes a text node and
 * a bracket entry, and a {@code ]} followed by an inline link's target closes the nearest bracket, resolving the
 * emphasis inside with {@link #processEmphasis(Delimiter, int)} from the bracket's own stack bottom. Autolinks are
 * read where the scan meets {@code <}, bare URLs where it meets an {@code h} ({@link BareUrls}): before the emphasis
 * and escapes in them, so both keep their address as written.
 *
 * <p>The scan dispatches on the character at the current position; a later construct adds its case there.
 */
final class InlineParser {

    /**
     * How deep unescaped parentheses may nest in a link destination (CommonMark asks for at least three).
     */
    private static final int MAX_PARENTHESES_DEPTH = 32;

    /**
     * How deep inlines may nest: an emphasis, a strong emphasis and a link are one level each. As deep as containers
     * ({@link BlockParser#MAX_DEPTH}); no real text comes near it.
     */
    private static final int MAX_DEPTH = 100;

    private final String text;
    private final StringBuilder pending = new StringBuilder();
    private Node first;
    private Node last;
    /** The top of the delimiter stack, {@code null} when it is empty. */
    private Delimiter lastDelimiter;
    /** The top of the bracket stack, {@code null} when it is empty. */
    private Bracket lastBracket;
    /** The depth in the bracket stack from which a {@code [} can open a link: those below precede a link. */
    private int linkOpenersFrom;
    private int position;

    private InlineParser(String text) {
        this.text = text;
    }

    /**
     * Parses the text of one heading or paragraph.
     *
     * @param text the text, lines joined by {@code \n}, without leading and trailing spaces
     * @return the inlines
     */
    static List<Inline> parseInlines(String text) {
        return new InlineParser(text).parse();
    }

    private List<Inline> parse() {
        while (position < text.length()) {
            char c = text.charAt(position);
            if (c == '\n') {
                lineBreak();
            } else if (c == '\\') {
                backslash();
            } else if (c == '`') {
                codeSpan();
            } else if (c == '*' || c == '_') {
                delimiterRun();
            } else if (c == '[') {
                openBracket(false);
            } else if (c == '!' && position + 1 < text.length() && text.charAt(position + 1) == '[') {
                openBracket(true);
            } else if (c == ']') {
                closeBracket();
            } else if (c == '<') {
                autolink();
            } else if (c == 'h') {
                bareUrl();
            } else {
                pending.append(c);
                position++;
            }
        }
        flushPending();
        processEmphasis(null, MAX_DEPTH);
        return toInlines(first, null).inlines();
    }

    /**
     * A line ending: a hard break after two or more spaces, else a soft one. The spaces before it are dropped, the
     * next line's leading spaces were dropped by the block parser already.
     */
    private void lineBreak() {
        int end = pending.length();
        while (end > 0 && pending.charAt(end - 1) == ' ') {
            end--;
        }
        boolean hard = pending.length() - end >= 2;
        pending.setLength(end);
        append(new InlineNode(hard ? new HardBreak() : new SoftBreak()));
        position++;
    }

    /**
     * A backslash: before ASCII punctuation an escape, which makes the punctuation literal; before a line ending a
     * hard break; else literal itself.
     */
    private void backslash() {
        char next = position + 1 < text.length() ? text.charAt(position + 1) : '\0';
        if (next == '\n') {
            append(new InlineNode(new HardBreak()));
            position += 2;
        } else if (Characters.isAsciiPunctuation(next)) {
            pending.append(next);
            position += 2;
        } else {
            pending.append('\\');
            position++;
        }
    }

    /**
     * A backtick run: a code span up to the next backtick run of exactly the same length, or, without one, literal
     * backticks. The content is literal; its line endings become spaces, and one space on both sides is stripped
     * when both are there and the content is not all spaces (so {@code `` ` ``} shows a single backtick).
     */
    private void codeSpan() {
        int openerLength = runLength(position);
        int contentStart = position + openerLength;
        int closer = contentStart;
        while (closer < text.length()) {
            if (text.charAt(closer) == '`') {
                int closerLength = runLength(closer);
                if (closerLength == openerLength) {
                    break;
                }
                closer += closerLength;
            } else {
                closer++;
            }
        }
        if (closer >= text.length()) {
            pending.append(text, position, contentStart);
            position = contentStart;
            return;
        }
        String code = text.substring(contentStart, closer).replace('\n', ' ');
        if (code.length() >= 2 && code.charAt(0) == ' ' && code.charAt(code.length() - 1) == ' '
                && !isAllSpaces(code)) {
            code = code.substring(1, code.length() - 1);
        }
        append(new InlineNode(new CodeSpan(code)));
        position = closer + openerLength;
    }

    /**
     * A run of {@code *} or {@code _}: a text node of its own, and a delimiter if it can open or close emphasis.
     *
     * <p>CommonMark's flanking rules decide that from the characters around the run (the start and the end of the
     * text count as whitespace). A run is left-flanking if it is not followed by whitespace and, if followed by
     * punctuation, preceded by whitespace or punctuation; right-flanking is the mirror image. A {@code *} run opens
     * if left-flanking and closes if right-flanking, so {@code 2 * 3 * 4} stays literal. A {@code _} run inside a
     * word (left- and right-flanking) neither opens nor closes unless punctuation is on the other side, so
     * {@code snake_case} keeps its underscore.
     */
    private void delimiterRun() {
        char character = text.charAt(position);
        int length = runLength(position);
        char before = position == 0 ? '\n' : text.charAt(position - 1);
        char after = position + length < text.length() ? text.charAt(position + length) : '\n';

        boolean whitespaceBefore = Characters.isWhitespace(before);
        boolean whitespaceAfter = Characters.isWhitespace(after);
        boolean punctuationBefore = Characters.isPunctuation(before);
        boolean punctuationAfter = Characters.isPunctuation(after);

        boolean leftFlanking = !whitespaceAfter && (!punctuationAfter || whitespaceBefore || punctuationBefore);
        boolean rightFlanking = !whitespaceBefore && (!punctuationBefore || whitespaceAfter || punctuationAfter);
        boolean canOpen;
        boolean canClose;
        if (character == '*') {
            canOpen = leftFlanking;
            canClose = rightFlanking;
        } else {
            canOpen = leftFlanking && (!rightFlanking || punctuationBefore);
            canClose = rightFlanking && (!leftFlanking || punctuationAfter);
        }

        TextNode node = new TextNode(text.substring(position, position + length));
        append(node);
        if (canOpen || canClose) {
            Delimiter delimiter = new Delimiter(node, character, length, canOpen, canClose);
            delimiter.previous = lastDelimiter;
            if (lastDelimiter != null) {
                lastDelimiter.next = delimiter;
            }
            lastDelimiter = delimiter;
        }
        position += length;
    }

    /**
     * A {@code [} or an image's {@code ![}: a text node of its own, and an entry of the bracket stack, which a later
     * {@code ]} may close. The entry remembers the top of the delimiter stack, so the emphasis inside the brackets is
     * resolved within them.
     */
    private void openBracket(boolean image) {
        TextNode node = new TextNode(image ? "![" : "[");
        append(node);
        int depth = lastBracket == null ? 0 : lastBracket.depth + 1;
        lastBracket = new Bracket(node, image, depth, lastDelimiter, lastBracket);
        position += image ? 2 : 1;
    }

    /**
     * Whether a bracket can still open a link: an image's always, a {@code [} unless a link formed after it.
     */
    private boolean isActive(Bracket bracket) {
        return bracket.image || bracket.depth >= linkOpenersFrom;
    }

    /**
     * Removes the top bracket from the stack. A bracket pushed later takes its depth and is active, so the floor of
     * active link openers comes down with it.
     */
    private void popBracket() {
        linkOpenersFrom = Math.min(linkOpenersFrom, lastBracket.depth);
        lastBracket = lastBracket.previous;
    }

    /**
     * A {@code ]}: closes the nearest open bracket if an inline link's target follows, as CommonMark's "look for link
     * or image" does. Else the {@code ]} is literal and the bracket is dropped from the stack, its {@code [} literal
     * too: with no link reference definitions, {@code [text]}, {@code [text][]} and {@code [text][label]} stay text.
     *
     * <p>A {@code [} becomes a link: the link text is the nodes after the bracket, its emphasis resolved first from
     * the bracket's delimiter stack bottom. Links do not nest: once a link is formed, the brackets before it can no
     * longer open one, so {@code [a [b](c)](d)} links only {@code b}. An image's {@code ![} becomes an image the same
     * way, its description the nodes after the bracket, flattened into its alt text ({@link #altText(List)}); an image
     * leaves the brackets before it active, so a link's text may hold an image ({@code [![a](b)](c)}).
     */
    private void closeBracket() {
        Bracket opener = lastBracket;
        LinkTarget target = opener != null && isActive(opener) ? linkTarget(position + 1) : null;
        if (target == null) {
            if (opener != null) {
                popBracket();
            }
            pending.append(']');
            position++;
            return;
        }
        flushPending();
        if (!opener.image) {
            // A link never holds a link: an autolink in the link text (the only link that can be there, as a link
            // makes the brackets before it inactive) becomes text of it.
            for (Node node = opener.node.next; node != null; node = node.next) {
                if (node instanceof InlineNode inlineNode && inlineNode.inline instanceof Link autolink) {
                    Node textNode = new TextNode(autolink.destination());
                    replace(node, textNode);
                    node = textNode;
                }
            }
        }
        // The emphasis in link text nests two levels less than the limit: the link adds one level, and the link must
        // stay less deep than the limit itself, as every node the emphasis around it is made of must.
        processEmphasis(opener.previousDelimiter, MAX_DEPTH - 2);
        popBracket();
        if (opener.image) {
            Content description = toInlines(opener.node.next, null);
            truncate(opener.node);
            // A leaf of the model: its description lives on as the alt text only.
            Image image = new Image(target.destination(), target.title(), altText(description.inlines()));
            appendNode(new InlineNode(image));
        } else {
            Content content = toInlines(opener.node.next, null);
            truncate(opener.node);
            appendNode(new InlineNode(new Link(target.destination(), target.title(), content.inlines()),
                    content.depth() + 1));
            // Every [ below the link can no longer open one. A floor instead of marking each of them keeps a long
            // run of [ followed by many links linear.
            linkOpenersFrom = opener.depth;
        }
        position = target.end();
    }

    /**
     * A {@code <}: an autolink if an absolute address in angle brackets follows, as CommonMark defines it (a scheme
     * of 2 to 32 ASCII letters, digits, {@code +}, {@code .} and {@code -} starting with a letter, a colon, then no
     * spaces, control characters or angle brackets up to the {@code >}). Its text is the address as written:
     * backslash escapes are not resolved in it. Else the {@code <} is literal, so e-mail autolinks and inline HTML
     * stay text.
     */
    private void autolink() {
        int end = autolinkEnd(position);
        if (end < 0) {
            pending.append('<');
            position++;
            return;
        }
        String address = text.substring(position + 1, end - 1);
        append(new InlineNode(Link.autolink(address), 1));
        position = end;
    }

    /**
     * The end of an autolink starting with the {@code <} at {@code start}.
     *
     * @return the index after the {@code >}, or -1 if no autolink starts there
     */
    private int autolinkEnd(int start) {
        int i = start + 1;
        while (i < text.length() && isSchemeCharacter(text.charAt(i), i == start + 1)) {
            i++;
        }
        int schemeLength = i - start - 1;
        if (schemeLength < 2 || schemeLength > 32 || i >= text.length() || text.charAt(i) != ':') {
            return -1;
        }
        for (i++; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '>') {
                return i + 1;
            } else if (c == '<' || c <= ' ' || c == '\u007F') {
                return -1;
            }
        }
        return -1;
    }

    private static boolean isSchemeCharacter(char c, boolean first) {
        if (first) {
            return Characters.isAsciiLetter(c);
        }
        return Characters.isAsciiLetter(c) || Characters.isDigit(c) || c == '+' || c == '.' || c == '-';
    }

    /**
     * An {@code h}: a bare URL if one starts here, else literal. None starts while a bracket is open, as in GFM: in
     * link text it would be a link in a link, and its address could run across the link's {@code ](}.
     */
    private void bareUrl() {
        int end = bareUrlEnd(position, text.length());
        if (end < 0) {
            pending.append('h');
            position++;
            return;
        }
        append(new InlineNode(Link.autolink(text.substring(position, end)), 1));
        position = end;
    }

    /**
     * The end of a bare URL starting at {@code start} and ending by {@code limit}, if a bare URL may start there.
     *
     * @return the index after the address, or -1 if no bare URL starts at {@code start}
     */
    private int bareUrlEnd(int start, int limit) {
        if (lastBracket != null) {
            return -1;
        }
        return BareUrls.end(text, start, limit);
    }

    /**
     * The alt text of an image's description: its plain text as CommonMark defines it, emphasis without its markers,
     * code without its backticks, a link as its text, an image inside as its own alt text, and every line break as
     * a space, so it is one line. The parser limits how deep inlines nest, so the recursion stays shallow.
     */
    private static String altText(List<Inline> description) {
        StringBuilder alt = new StringBuilder();
        for (Inline inline : description) {
            switch (inline) {
                case PlainText plainText -> alt.append(plainText.text());
                case CodeSpan codeSpan -> alt.append(codeSpan.code());
                case Emphasis emphasis -> alt.append(altText(emphasis.content()));
                case Strong strong -> alt.append(altText(strong.content()));
                case Link link -> alt.append(altText(link.content()));
                case Image image -> alt.append(image.alt());
                case SoftBreak softBreak -> alt.append(' ');
                case HardBreak hardBreak -> alt.append(' ');
            }
        }
        return alt.toString();
    }

    /**
     * Reads an inline link's target right after a {@code ]}: {@code (}, a destination, an optional title, {@code )},
     * with optional whitespace (line endings included) in between and whitespace required before the title, as
     * CommonMark does.
     *
     * @param start the index after the {@code ]}
     * @return the target, or {@code null} if none starts there
     */
    private LinkTarget linkTarget(int start) {
        if (start >= text.length() || text.charAt(start) != '(') {
            return null;
        }
        int destinationStart = skipWhitespace(start + 1);
        int destinationEnd = destinationEnd(destinationStart);
        if (destinationEnd < 0) {
            return null;
        }
        String destination = destinationEnd > destinationStart && text.charAt(destinationStart) == '<'
                ? unescape(text.substring(destinationStart + 1, destinationEnd - 1))
                : unescape(text.substring(destinationStart, destinationEnd));
        int index = skipWhitespace(destinationEnd);
        String title = "";
        if (index > destinationEnd) {
            int titleEnd = titleEnd(index);
            if (titleEnd >= 0) {
                title = unescape(text.substring(index + 1, titleEnd - 1));
                index = skipWhitespace(titleEnd);
            }
        }
        if (index >= text.length() || text.charAt(index) != ')') {
            return null;
        }
        return new LinkTarget(destination, title, index + 1);
    }

    /**
     * The end of a link destination: either {@code <...>}, without line endings and unescaped angle brackets, or a
     * non-empty run without spaces and control characters whose unescaped parentheses balance. Backslash escapes are
     * stepped over. The run may be empty right before the target's {@code )}, so {@code [link]()} links to nothing.
     * Parentheses nest at most {@value #MAX_PARENTHESES_DEPTH} deep, as in cmark: without a limit, every {@code ](}
     * of a long text full of unclosed ones would scan to its end.
     *
     * @return the index after the destination, or -1 if no destination starts at {@code start}
     */
    private int destinationEnd(int start) {
        if (start < text.length() && text.charAt(start) == '<') {
            int i = start + 1;
            while (i < text.length()) {
                char c = text.charAt(i);
                if (c == '>') {
                    return i + 1;
                } else if (c == '<' || c == '\n') {
                    return -1;
                } else if (c == '\\' && i + 1 < text.length() && text.charAt(i + 1) != '\n') {
                    i += 2;
                } else {
                    i++;
                }
            }
            return -1;
        }
        int depth = 0;
        int i = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && Characters.isAsciiPunctuation(text.charAt(i + 1))) {
                i += 2;
            } else if (c == '(') {
                depth++;
                if (depth > MAX_PARENTHESES_DEPTH) {
                    return -1;
                }
                i++;
            } else if (c == ')') {
                if (depth == 0) {
                    break;
                }
                depth--;
                i++;
            } else if (c <= ' ' || c == '\u007F') {
                break;
            } else {
                i++;
            }
        }
        if (depth != 0 || (i == start && (i >= text.length() || text.charAt(i) != ')'))) {
            return -1;
        }
        return i;
    }

    /**
     * The end of a link title: text in {@code "..."}, {@code '...'} or {@code (...)}, where a backslash steps over
     * the next character and a title in parentheses holds no unescaped {@code (}.
     *
     * @return the index after the closing character, or -1 if no title starts at {@code start}
     */
    private int titleEnd(int start) {
        char open = start < text.length() ? text.charAt(start) : '\0';
        char close;
        if (open == '"' || open == '\'') {
            close = open;
        } else if (open == '(') {
            close = ')';
        } else {
            return -1;
        }
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                i += 2;
            } else if (c == close) {
                return i + 1;
            } else if (open == '(' && c == '(') {
                return -1;
            } else {
                i++;
            }
        }
        return -1;
    }

    private int skipWhitespace(int start) {
        int i = start;
        while (i < text.length() && (Characters.isSpaceOrTab(text.charAt(i)) || text.charAt(i) == '\n')) {
            i++;
        }
        return i;
    }

    /**
     * Resolves backslash escapes: a backslash before ASCII punctuation is dropped, any other stays.
     */
    private static String unescape(String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder unescaped = new StringBuilder(value.length());
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length() && Characters.isAsciiPunctuation(value.charAt(i + 1))) {
                unescaped.append(value.charAt(i + 1));
                i += 2;
            } else {
                unescaped.append(c);
                i++;
            }
        }
        return unescaped.toString();
    }

    /**
     * Resolves the delimiters above {@code stackBottom} into emphasis, as CommonMark's "process emphasis" does, and
     * removes them from the stack.
     *
     * <p>Each closer, from the bottom up, looks back for the nearest opener of the same character. Two delimiters
     * each make strong emphasis, else one makes emphasis; whatever is left of a run stays for the next match, which
     * is how {@code ***x***} becomes strong emphasis inside emphasis. The "rule of three" stops a match when one of
     * the runs can both open and close and their original lengths add up to a multiple of three (unless both are
     * multiples of three), so {@code *foo**bar*} is one emphasis with literal asterisks inside. The search for
     * openers starts no lower than where a previous search for the same kind of closer failed, which keeps the
     * algorithm linear.
     *
     * <p>Emphasis nests at most {@code maxDepth} inlines deep, like containers ({@link BlockParser#MAX_DEPTH}): once
     * an emphasis is that deep, nothing before it can match anything after it, and those delimiters stay text. Else a
     * text of thousands of nested delimiters would build a model too deep for any walk over it.
     *
     * @param stackBottom the delimiter below the ones to resolve, {@code null} for the whole stack
     * @param maxDepth    how deep an emphasis made here may nest, the inlines inside it counted; the nodes it is made
     *                    of must nest less deep
     */
    private void processEmphasis(Delimiter stackBottom, int maxDepth) {
        // Index: character (2) x closer can open (2) x closer's original length modulo 3 (3).
        Delimiter[] openersBottom = new Delimiter[12];
        Arrays.fill(openersBottom, stackBottom);

        Delimiter closer = lastDelimiter == stackBottom ? null : lastDelimiter;
        while (closer != null && closer.previous != stackBottom) {
            closer = closer.previous;
        }
        while (closer != null) {
            if (!closer.canClose) {
                closer = closer.next;
                continue;
            }
            int bottomIndex = (closer.character == '*' ? 0 : 6) + (closer.canOpen ? 3 : 0) + closer.originalLength % 3;
            Delimiter opener = closer.previous;
            boolean found = false;
            while (opener != null && opener != stackBottom && opener != openersBottom[bottomIndex]) {
                boolean oddMatch = (closer.canOpen || opener.canClose)
                        && closer.originalLength % 3 != 0
                        && (opener.originalLength + closer.originalLength) % 3 == 0;
                if (opener.character == closer.character && opener.canOpen && !oddMatch) {
                    found = true;
                    break;
                }
                opener = opener.previous;
            }

            Delimiter oldCloser = closer;
            if (found) {
                Delimiter belowOpener = opener.previous;
                Delimiter next = closer.next;
                int depth = match(opener, closer);
                if (depth >= maxDepth) {
                    // No opener up to the new emphasis may match from now on: it would nest deeper. The delimiters
                    // between the opener and the closer are gone, so the opener (or the one below it, if the opener
                    // is used up) is the last delimiter before the emphasis, a floor for every kind of closer.
                    Arrays.fill(openersBottom, opener.length() > 0 ? opener : belowOpener);
                }
                closer = closer.length() > 0 ? closer : next;
            } else {
                closer = closer.next;
                openersBottom[bottomIndex] = oldCloser.previous;
                if (!oldCloser.canOpen) {
                    removeDelimiter(oldCloser);
                }
            }
        }

        while (lastDelimiter != null && lastDelimiter != stackBottom) {
            removeDelimiter(lastDelimiter);
        }
    }

    /**
     * Wraps the nodes between a matching opener and closer into emphasis. An opener or closer whose characters are
     * used up leaves the delimiter stack.
     *
     * @return how deep the new emphasis nests
     */
    private int match(Delimiter opener, Delimiter closer) {
        int used = opener.length() >= 2 && closer.length() >= 2 ? 2 : 1;
        opener.consume(used);
        closer.consume(used);

        Content content = toInlines(opener.node.next, closer.node);
        Inline emphasis = used == 2 ? new Strong(content.inlines()) : new Emphasis(content.inlines());
        InlineNode emphasisNode = new InlineNode(emphasis, content.depth() + 1);
        emphasisNode.previous = opener.node;
        emphasisNode.next = closer.node;
        opener.node.next = emphasisNode;
        closer.node.previous = emphasisNode;

        // Delimiters inside the new emphasis can no longer match anything outside it.
        Delimiter between = closer.previous;
        while (between != null && between != opener) {
            Delimiter below = between.previous;
            removeDelimiter(between);
            between = below;
        }

        if (opener.length() == 0) {
            unlink(opener.node);
            removeDelimiter(opener);
        }
        if (closer.length() == 0) {
            unlink(closer.node);
            removeDelimiter(closer);
        }
        return emphasisNode.depth;
    }

    private void removeDelimiter(Delimiter delimiter) {
        if (delimiter.previous != null) {
            delimiter.previous.next = delimiter.next;
        }
        if (delimiter.next != null) {
            delimiter.next.previous = delimiter.previous;
        } else {
            lastDelimiter = delimiter.previous;
        }
    }

    /**
     * Appends a node, the pending literal text first.
     */
    private void append(Node node) {
        flushPending();
        appendNode(node);
    }

    private void flushPending() {
        if (!pending.isEmpty()) {
            appendNode(new TextNode(pending.toString()));
            pending.setLength(0);
        }
    }

    /**
     * Appends a node to the end of the list as it is; {@link #append(Node)} writes the pending text first.
     */
    private void appendNode(Node node) {
        if (last == null) {
            first = node;
        } else {
            last.next = node;
            node.previous = last;
        }
        last = node;
    }

    /**
     * Removes the nodes from {@code from} to the end of the list.
     */
    private void truncate(Node from) {
        last = from.previous;
        if (last == null) {
            first = null;
        } else {
            last.next = null;
        }
    }

    private void replace(Node old, Node replacement) {
        replacement.previous = old.previous;
        replacement.next = old.next;
        if (old.previous == null) {
            first = replacement;
        } else {
            old.previous.next = replacement;
        }
        if (old.next == null) {
            last = replacement;
        } else {
            old.next.previous = replacement;
        }
    }

    private void unlink(Node node) {
        if (node.previous == null) {
            first = node.next;
        } else {
            node.previous.next = node.next;
        }
        if (node.next == null) {
            last = node.previous;
        } else {
            node.next.previous = node.previous;
        }
    }

    /**
     * Converts the nodes from {@code from} up to (not including) {@code until} into inlines, merging adjacent text
     * nodes and dropping empty ones.
     */
    private static Content toInlines(Node from, Node until) {
        List<Inline> inlines = new ArrayList<>();
        int depth = 0;
        StringBuilder textRun = new StringBuilder();
        for (Node node = from; node != null && node != until; node = node.next) {
            switch (node) {
                case TextNode textNode -> textRun.append(textNode.text);
                case InlineNode inlineNode -> {
                    if (!textRun.isEmpty()) {
                        inlines.add(new PlainText(textRun.toString()));
                        textRun.setLength(0);
                    }
                    inlines.add(inlineNode.inline);
                    depth = Math.max(depth, inlineNode.depth);
                }
            }
        }
        if (!textRun.isEmpty()) {
            inlines.add(new PlainText(textRun.toString()));
        }
        return new Content(inlines, depth);
    }

    /**
     * The length of the run of equal characters starting at {@code start}.
     */
    private int runLength(int start) {
        char c = text.charAt(start);
        int end = start;
        while (end < text.length() && text.charAt(end) == c) {
            end++;
        }
        return end - start;
    }

    private static boolean isAllSpaces(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * A node of the list under construction: literal text or a finished inline.
     */
    private abstract static sealed class Node permits TextNode, InlineNode {

        // Not private, so that they are members of the subclasses too.
        Node previous;
        Node next;
    }

    /**
     * Literal text, which stays mutable while delimiter runs are matched: a delimiter run's characters are used up
     * by the emphasis they make.
     */
    private static final class TextNode extends Node {

        private final StringBuilder text;

        private TextNode(String text) {
            this.text = new StringBuilder(text);
        }
    }

    /**
     * A finished inline, and how deep it nests: 0 for a code span or a break, 1 more than its content for an
     * emphasis or a link.
     */
    private static final class InlineNode extends Node {

        private final Inline inline;
        private final int depth;

        private InlineNode(Inline inline) {
            this(inline, 0);
        }

        private InlineNode(Inline inline, int depth) {
            this.inline = inline;
            this.depth = depth;
        }
    }

    /**
     * The inlines of a range of nodes, and how deep the deepest of them nests.
     */
    private record Content(List<Inline> inlines, int depth) {
    }

    /**
     * An entry of the delimiter stack: a run of {@code *} or {@code _} that can open or close emphasis. Its
     * characters are the text of its node, which shrinks as matches use them.
     */
    private static final class Delimiter {

        private final TextNode node;
        private final char character;
        private final int originalLength;
        private final boolean canOpen;
        private final boolean canClose;
        private Delimiter previous;
        private Delimiter next;

        private Delimiter(TextNode node, char character, int originalLength, boolean canOpen, boolean canClose) {
            this.node = node;
            this.character = character;
            this.originalLength = originalLength;
            this.canOpen = canOpen;
            this.canClose = canClose;
        }

        private int length() {
            return node.text.length();
        }

        /**
         * Uses up characters of the run for an emphasis: they leave its text.
         */
        private void consume(int count) {
            node.text.setLength(node.text.length() - count);
        }
    }

    /**
     * An entry of the bracket stack: a {@code [} that a later {@code ]} may close as a link, or an image's {@code ![}.
     */
    private static final class Bracket {

        private final TextNode node;
        private final boolean image;
        /** The number of brackets below this one in the stack. */
        private final int depth;
        /** The top of the delimiter stack when the bracket opened: the stack bottom for the link text's emphasis. */
        private final Delimiter previousDelimiter;
        private final Bracket previous;

        private Bracket(TextNode node, boolean image, int depth, Delimiter previousDelimiter, Bracket previous) {
            this.node = node;
            this.image = image;
            this.depth = depth;
            this.previousDelimiter = previousDelimiter;
            this.previous = previous;
        }
    }

    /**
     * An inline link's target as read after the {@code ]}.
     *
     * @param destination the address, escapes resolved
     * @param title       the title, escapes resolved, empty if none
     * @param end         the index after the closing {@code )}
     */
    private record LinkTarget(String destination, String title, int end) {
    }
}
