package org.karthu.fx.markdown.parser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * The block level of the parser: turns lines into blocks.
 *
 * <p>It walks the lines once. At each line that is not blank, the first block kind whose start the line is reads
 * its lines and becomes a block; a line that starts no known block starts a paragraph, so whatever the parser does
 * not know stays text. A paragraph runs until a blank line or a line that starts another block
 * ({@link #interruptsParagraph(String)}).
 *
 * <p>A container block parses its content recursively: a list item hands its own lines, with its marker and
 * indentation removed, to a new block parser one level deeper, with the lines it took as lazy continuation lines
 * ({@link #item(ItemStart)}); a block quote does the same with its lines without their {@code >} markers
 * ({@link #quote()}).
 *
 * <p><b>Task list items.</b> A list item whose content starts with a GFM task marker ({@code [ ]}, {@code [x]} or
 * {@code [X]}, then a space, a tab or the end of the line) is a task, if that line starts the item's first paragraph
 * ({@link #taskMarker(String)}). The parser of the item's content takes the marker out of that paragraph's text before
 * the inline parse, once the block structure is settled: so {@code - [ ] # x} is a task with the text {@code # x}, as
 * in GFM, while a table that starts on the marker's line leaves the item a plain one.
 *
 * <p><b>Lazy lines.</b> CommonMark lets a paragraph inside a container continue on a line that is not indented
 * enough for the container, or lacks the quote's {@code >} (a <em>lazy continuation line</em>). Such a line belongs to
 * the innermost open paragraph, whatever its indentation or markers say in the inner containers. So the container
 * marks it as lazy when it hands the line on, and every parser down the line treats a lazy line as paragraph text
 * only: it never starts a block, never ends a paragraph, and is never re-read by its indentation or markers.
 *
 * <p><b>Precedence.</b> A line that is a thematic break is never a list item ({@code * * *}, {@code - - -}), and a
 * {@code ---} line is never a setext underline: setext headings are not in the subset.
 *
 * <p><b>Code.</b> The lines of a fenced code block are code, blank ones included, up to its closing fence
 * ({@link #fencedCode()}). Inside a container, the container decides first which lines are its own, so the end of a
 * list item or a quote closes a code block that is still open in it.
 *
 * <p><b>Tables and source blocks.</b> A GFM table ({@link #table()}) interrupts a paragraph as in GFM. The blocks the
 * view does not render become {@link SourceBlock}s that keep their lines as written: an HTML block
 * ({@link #htmlBlock()}), which interrupts no paragraph, and an indented code block ({@link #indentedCode()}), which
 * starts only where no paragraph is open. None of the three counts in {@link #startsBlock(String)}: a line that would
 * start one right after a container's open paragraph is a lazy continuation line of that paragraph, as in GFM.
 *
 * <p><b>Depth.</b> Each container level parses on one level deeper, and the recursion would overflow the stack on a
 * text of thousands of nested markers. So containers nest at most {@link #MAX_DEPTH} levels deep, as in markdown-it;
 * deeper, a container's marker is paragraph text. No real document comes near it.
 */
final class BlockParser {

    /**
     * The deepest container nesting: the content of a container at this depth starts no further container.
     */
    static final int MAX_DEPTH = 100;

    private final List<String> lines;
    private final BitSet lazyLines;
    private final int depth;
    private int index;

    /**
     * Whether a blank line separates two of the parsed blocks: a list item whose content has one makes its list
     * loose.
     */
    private boolean blankBetweenBlocks;

    /**
     * Whether the parsed lines end inside an open paragraph, also one nested in the last list's last item: the next
     * line of the container may then be a lazy continuation line.
     */
    private boolean endsInParagraph;

    /**
     * Whether this parse only probes the block structure ({@link #endsInParagraph(List, BitSet)}): then the inline
     * content of paragraphs is not parsed.
     */
    private final boolean probe;

    /**
     * The task marker the first line starts with, when the lines are a list item's content: the item is a task if
     * that line starts a paragraph. {@link ListItem.Task#NONE} for any other lines.
     */
    private final ListItem.Task taskMarker;

    /**
     * The task the lines turned out to be: the task marker once the first line started a paragraph, else
     * {@link ListItem.Task#NONE}.
     */
    private ListItem.Task task = ListItem.Task.NONE;

    /**
     * Creates a parser over lines.
     *
     * @param lines      the lines, without line endings
     * @param lazyLines  the indexes of the lines that are lazy continuation lines
     * @param depth      the number of containers around the lines, 0 for the document
     * @param probe      whether the parse only probes the block structure
     * @param taskMarker the task marker the first line starts with, if the lines are a list item's content
     */
    private BlockParser(List<String> lines, BitSet lazyLines, int depth, boolean probe, ListItem.Task taskMarker) {
        this.lines = lines;
        this.lazyLines = lazyLines;
        this.depth = depth;
        this.probe = probe;
        this.taskMarker = taskMarker;
    }

    /**
     * Parses lines into blocks.
     *
     * @param lines the lines, without line endings
     * @return the blocks in source order
     */
    static List<Block> parseBlocks(List<String> lines) {
        return new BlockParser(lines, new BitSet(), 0, false, ListItem.Task.NONE).parse();
    }

    private List<Block> parse() {
        List<Block> blocks = new ArrayList<>();
        boolean blankBefore = false;
        while (index < lines.size()) {
            String line = lines.get(index);
            if (isBlank(line)) {
                index++;
                blankBefore = true;
                endsInParagraph = false;
                continue;
            }
            // An empty task's paragraph is no block, but a blank line after it loosens the list as after any.
            if (blankBefore && (!blocks.isEmpty() || task != ListItem.Task.NONE)) {
                blankBetweenBlocks = true;
            }
            blankBefore = false;
            if (lazyLines.get(index)) {
                blocks.add(paragraph());
                endsInParagraph = true;
            } else if (indentation(line) >= 4) {
                blocks.add(indentedCode());
                endsInParagraph = false;
            } else if (fenceStart(line) != null) {
                blocks.add(fencedCode());
                endsInParagraph = false;
            } else if (headingLevel(line) > 0) {
                blocks.add(heading(line));
                index++;
                endsInParagraph = false;
            } else if (isThematicBreak(line)) {
                blocks.add(new ThematicBreak());
                index++;
                endsInParagraph = false;
            } else if (quoteContent(line) != null) {
                Container quote = quote();
                blocks.add(quote.block());
                endsInParagraph = quote.endsInParagraph();
            } else if (itemStart(line) != null) {
                Container list = list();
                blocks.add(list.block());
                endsInParagraph = list.endsInParagraph();
            } else if (isHtmlBlockStart(line)) {
                blocks.add(htmlBlock());
                endsInParagraph = false;
            } else if (isTableStart(index)) {
                blocks.add(table());
                endsInParagraph = false;
            } else if (index == 0 && taskMarker != ListItem.Task.NONE) {
                taskParagraph(blocks);
                endsInParagraph = true;
            } else {
                blocks.add(paragraph());
                endsInParagraph = true;
            }
        }
        return blocks;
    }

    /**
     * Whether a line that is not indented enough for a container starts a block of its own beside the container, so
     * it is no lazy continuation line. Unlike {@link #interruptsParagraph(String)}, any list item counts, an empty one
     * or one numbered other than 1 too: CommonMark reads the line against the container's parent, not against the
     * paragraph.
     */
    private boolean startsBlock(String line) {
        return headingLevel(line) > 0 || isThematicBreak(line) || quoteContent(line) != null
                || itemStart(line) != null || fenceStart(line) != null;
    }

    /**
     * Reads a list from the current line, an item start, on: items follow one another as long as the next item
     * start is of the same kind, the same bullet character or the same delimiter after the number. Another bullet or
     * delimiter starts a new list.
     */
    private Container list() {
        ItemStart first = itemStart(lines.get(index));
        List<ListItem> items = new ArrayList<>();
        boolean tight = true;
        boolean lastItemEndsInParagraph = false;
        ItemStart start = first;
        while (start != null) {
            Content item = item(start);
            items.add(new ListItem(item.blocks(), item.task()));
            lastItemEndsInParagraph = item.endsInParagraph();
            if (item.blankBetweenBlocks()) {
                tight = false;
            }
            start = null;
            int next = index;
            while (next < lines.size() && isBlank(lines.get(next))) {
                next++;
            }
            if (next < lines.size()) {
                ItemStart following = lazyLines.get(next) ? null : itemStart(lines.get(next));
                if (following != null && following.ordered() == first.ordered()
                        && following.marker() == first.marker()) {
                    if (next > index) {
                        tight = false;
                    }
                    index = next;
                    start = following;
                }
            }
        }
        if (first.ordered()) {
            return new Container(new OrderedList(first.number(), first.marker(), tight, items),
                    lastItemEndsInParagraph);
        }
        return new Container(new BulletList(tight, items), lastItemEndsInParagraph);
    }

    /**
     * Reads one list item from its start line on. The lines indented to the item's content column belong to it,
     * with that indentation removed, and so do blank lines followed by such a line, and lazy continuation lines; its
     * content is parsed as blocks, recursively. The item ends at its last line that is not blank: blank lines after
     * it are left to the list.
     *
     * @return the item's content
     */
    private Content item(ItemStart start) {
        int column = start.contentColumn();
        boolean blankStart = isBlank(start.firstLine());
        List<String> content = new ArrayList<>();
        BitSet contentLazyLines = new BitSet();
        content.add(start.firstLine());
        int kept = 1;
        int end = index + 1;
        boolean previousBlank = false;
        // Known to end in an open paragraph: true right after a lazy line, which only continues that paragraph, so
        // the content is parsed again only when other lines came in since. A long lazy paragraph stays linear.
        boolean knownOpenParagraph = false;
        for (int line = index + 1; line < lines.size(); line++) {
            String text = lines.get(line);
            boolean lazy = lazyLines.get(line);
            if (isBlank(text)) {
                // An item can begin with at most one blank line: a marker alone followed by a blank line is empty.
                if (blankStart && content.size() == 1) {
                    break;
                }
                content.add("");
                previousBlank = true;
                knownOpenParagraph = false;
                continue;
            }
            if (!lazy && indentation(text) >= column) {
                content.add(text.substring(column));
                knownOpenParagraph = false;
            } else if (!previousBlank && isLazyContinuation(text, lazy, knownOpenParagraph, content.subList(0, kept),
                    contentLazyLines)) {
                contentLazyLines.set(content.size());
                content.add(text);
                knownOpenParagraph = true;
            } else {
                break;
            }
            kept = content.size();
            end = line + 1;
            previousBlank = false;
        }
        index = end;
        // A probe needs the block structure only, which the task marker does not change.
        ListItem.Task marker = probe ? ListItem.Task.NONE : taskMarker(start.firstLine());
        return parseContent(content.subList(0, kept), contentLazyLines, probe, marker);
    }

    /**
     * The GFM task marker a list item's first line starts with: {@code [}, a space, {@code x} or {@code X}, then
     * {@code ]}, followed by a space, a tab or the end of the line. Exactly one character between the brackets, and
     * only GitHub's: {@code [/]}, {@code [-]} and the like stay text, and so does {@code [x]} followed by anything
     * else ({@code [x](link)}, {@code [x]done}).
     *
     * @param firstLine the item's content on its marker's line, after the spaces that follow the list marker
     * @return the task marker, {@link ListItem.Task#NONE} if the line starts with none
     */
    static ListItem.Task taskMarker(String firstLine) {
        if (firstLine.length() < 3 || firstLine.charAt(0) != '[' || firstLine.charAt(2) != ']') {
            return ListItem.Task.NONE;
        }
        if (firstLine.length() > 3 && !Characters.isSpaceOrTab(firstLine.charAt(3))) {
            return ListItem.Task.NONE;
        }
        return switch (firstLine.charAt(1)) {
            case ' ' -> ListItem.Task.OPEN;
            case 'x', 'X' -> ListItem.Task.DONE;
            default -> ListItem.Task.NONE;
        };
    }

    /**
     * Reads a task item's first paragraph, which starts on the first line with the task marker: the marker and the
     * space or tab after it are taken out, and the rest of the line starts the paragraph's text. A marker alone on its
     * line leaves the paragraph to start with the next line, and a marker alone in its paragraph leaves no paragraph,
     * so an empty task has no block for it.
     *
     * @param blocks the blocks read so far, none: the paragraph joins them unless it is empty
     */
    private void taskParagraph(List<Block> blocks) {
        task = taskMarker;
        String firstLine = lines.get(index);
        Paragraph paragraph = paragraph(firstLine.substring(Math.min(firstLine.length(), 4)));
        if (!paragraph.content().isEmpty()) {
            blocks.add(paragraph);
        }
    }

    /**
     * Reads a block quote from the current line, a quote line, on: the following lines that start with {@code >}
     * belong to it, without their markers, and so do lazy continuation lines. Its content is parsed as blocks,
     * recursively. A blank line, or any other line, ends it.
     */
    private Container quote() {
        List<String> content = new ArrayList<>();
        BitSet contentLazyLines = new BitSet();
        // As in item(): known to end in an open paragraph right after a lazy line, so the content is probed again
        // only when a quote line came in since.
        boolean knownOpenParagraph = false;
        while (index < lines.size()) {
            String text = lines.get(index);
            boolean lazy = lazyLines.get(index);
            String lineContent = lazy ? null : quoteContent(text);
            if (lineContent != null) {
                content.add(lineContent);
                knownOpenParagraph = false;
            } else if (isLazyContinuation(text, lazy, knownOpenParagraph, content, contentLazyLines)) {
                contentLazyLines.set(content.size());
                content.add(text);
                knownOpenParagraph = true;
            } else {
                break;
            }
            index++;
        }
        Content quoteContent = parseContent(content, contentLazyLines, probe, ListItem.Task.NONE);
        return new Container(new BlockQuote(quoteContent.blocks()), quoteContent.endsInParagraph());
    }

    /**
     * Whether a line that does not continue a container by its marker or indentation is a lazy continuation line of
     * the container's content read so far.
     *
     * <p>A line this parser was handed as lazy is one: the container around this parser found its content to end in
     * an open paragraph, and that paragraph is in the container read here, which holds every line up to this one.
     * So it needs no probe, which keeps nested containers from probing again at every level. Any other line is one
     * when it is not blank, starts no block and the content so far ends in an open paragraph.
     *
     * @param knownOpenParagraph whether the content is known to end in an open paragraph without a probe
     */
    private boolean isLazyContinuation(String line, boolean lazy, boolean knownOpenParagraph, List<String> content,
                                       BitSet contentLazyLines) {
        if (lazy) {
            return true;
        }
        return !isBlank(line) && !startsBlock(line)
                && (knownOpenParagraph || endsInParagraph(content, contentLazyLines));
    }

    /**
     * Reads a line as a line of a block quote, if a quote may start at this parser's depth: up to three spaces, then
     * {@code >} and an optional space or tab, which are left out of the content.
     *
     * @return the line's content after the marker, or {@code null} if the line is no quote line here
     */
    private String quoteContent(String line) {
        int marker = indentation(line);
        if (depth >= MAX_DEPTH || marker > 3 || marker >= line.length() || line.charAt(marker) != '>') {
            return null;
        }
        int contentStart = marker + 1;
        if (contentStart < line.length() && Characters.isSpaceOrTab(line.charAt(contentStart))) {
            contentStart++;
        }
        return line.substring(contentStart);
    }

    /**
     * Whether lines of a container's content, parsed as blocks, end inside an open paragraph. A probe: the blocks are
     * thrown away, so their inline content is not parsed.
     */
    private boolean endsInParagraph(List<String> lines, BitSet lazyLines) {
        return parseContent(lines, lazyLines, true, ListItem.Task.NONE).endsInParagraph();
    }

    /**
     * Parses the lines of a container's content as blocks, one level deeper.
     *
     * @param contentLines     the content's lines, without the container's markers or indentation
     * @param contentLazyLines the indexes of the content's lazy continuation lines
     * @param probeOnly        whether only the block structure counts: the inline content of paragraphs is then not
     *                         parsed
     * @param taskMarker       the task marker a list item's content starts with, {@link ListItem.Task#NONE} for none
     *                         and for a quote's content
     */
    private Content parseContent(List<String> contentLines, BitSet contentLazyLines, boolean probeOnly,
                                 ListItem.Task taskMarker) {
        BlockParser parser = new BlockParser(contentLines, contentLazyLines, depth + 1, probeOnly, taskMarker);
        List<Block> blocks = parser.parse();
        return new Content(blocks, parser.endsInParagraph, parser.blankBetweenBlocks, parser.task);
    }

    /**
     * A container's content, parsed.
     *
     * @param blocks             the blocks
     * @param endsInParagraph    whether the content ends inside an open paragraph, also one nested in the last
     *                           list's last item: the container's next line may then be a lazy continuation line
     * @param blankBetweenBlocks whether a blank line separates two of the blocks: a list item's makes its list loose
     * @param task               whether the content is a task item's, whose first paragraph started with the marker
     */
    private record Content(List<Block> blocks, boolean endsInParagraph, boolean blankBetweenBlocks,
                           ListItem.Task task) {
    }

    /**
     * A container block, a list or a block quote, and whether its content ends inside an open paragraph.
     */
    private record Container(Block block, boolean endsInParagraph) {
    }

    /**
     * Reads a line as the first line of a list item, if a list may start at this parser's depth. A thematic break
     * is never an item, though {@code * * *} and {@code - - -} look like one (CommonMark: the break takes
     * precedence); since every caller asks here, a break also ends a list and is never an item's lazy line.
     *
     * @return the item start, or {@code null} if the line starts no list item here
     */
    private ItemStart itemStart(String line) {
        if (depth >= MAX_DEPTH || isThematicBreak(line)) {
            return null;
        }
        return readItemStart(line);
    }

    /**
     * Reads a line as the first line of a list item: up to three spaces, a marker, then a space, a tab or the end of
     * the line. The marker is a bullet ({@code -}, {@code *}, {@code +}) or one to nine digits followed by {@code .}
     * or {@code )}.
     *
     * <p>The item's content starts after the marker and the one to four spaces that follow it. Five or more spaces
     * count as one, the rest belongs to the content (CommonMark: the content then starts with indented code), and a
     * marker alone on its line puts the content column one space after it.
     *
     * @return the item start, or {@code null} if the line starts no list item
     */
    private static ItemStart readItemStart(String line) {
        int indent = indentation(line);
        if (indent > 3 || indent >= line.length()) {
            return null;
        }
        boolean ordered = false;
        int number = 0;
        int markerEnd = indent;
        char marker = line.charAt(indent);
        if (marker == '-' || marker == '*' || marker == '+') {
            markerEnd++;
        } else {
            while (markerEnd < line.length() && markerEnd - indent < 10 && Characters.isDigit(line.charAt(markerEnd))) {
                markerEnd++;
            }
            int digits = markerEnd - indent;
            if (digits == 0 || digits > 9 || markerEnd >= line.length()) {
                return null;
            }
            marker = line.charAt(markerEnd);
            if (marker != '.' && marker != ')') {
                return null;
            }
            ordered = true;
            number = Integer.parseInt(line.substring(indent, markerEnd));
            markerEnd++;
        }
        if (markerEnd < line.length() && !Characters.isSpaceOrTab(line.charAt(markerEnd))) {
            return null;
        }
        int contentStart = markerEnd;
        while (contentStart < line.length() && Characters.isSpaceOrTab(line.charAt(contentStart))) {
            contentStart++;
        }
        int spaces = contentStart - markerEnd;
        if (contentStart == line.length()) {
            return new ItemStart(ordered, marker, number, markerEnd + 1, "");
        }
        if (spaces > 4) {
            return new ItemStart(ordered, marker, number, markerEnd + 1, line.substring(markerEnd + 1));
        }
        return new ItemStart(ordered, marker, number, contentStart, line.substring(contentStart));
    }

    /**
     * The first line of a list item.
     *
     * @param ordered       whether the item belongs to an ordered list
     * @param marker        the bullet character, or the delimiter after an ordered item's number
     * @param number        the ordered item's number, 0 for a bullet item
     * @param contentColumn the column where the item's content starts: continuation lines indented this far belong
     *                      to the item
     * @param firstLine     the content on the marker's line, empty if there is none
     */
    private record ItemStart(boolean ordered, char marker, int number, int contentColumn, String firstLine) {
    }

    /**
     * Reads a fenced code block from the current line, an opening fence, on: the following lines are its code, as
     * written, up to a closing fence, which is left out. Blank lines are code too, so a blank line inside a list
     * item's code block never loosens the list. Without a closing fence, the block runs to the end of the lines, the
     * end of the document or of the container it is in.
     */
    private CodeBlock fencedCode() {
        FenceStart fence = fenceStart(lines.get(index));
        index++;
        StringBuilder literal = new StringBuilder();
        while (index < lines.size()) {
            String line = lines.get(index);
            index++;
            if (isClosingFence(line, fence)) {
                break;
            }
            literal.append(line, Math.min(indentation(line), fence.indent()), line.length()).append('\n');
        }
        return new CodeBlock(fence.info(), literal.toString());
    }

    /**
     * Reads a line as the opening fence of a fenced code block: up to three spaces, then three or more backticks or
     * three or more tildes, then the info string, whose surrounding spaces are left out.
     *
     * @return the fence, or {@code null} if the line opens no fenced code block
     */
    private static FenceStart fenceStart(String line) {
        int indent = indentation(line);
        if (indent > 3 || indent >= line.length()) {
            return null;
        }
        char character = line.charAt(indent);
        if (character != '`' && character != '~') {
            return null;
        }
        int end = indent;
        while (end < line.length() && line.charAt(end) == character) {
            end++;
        }
        if (end - indent < 3) {
            return null;
        }
        String info = stripLeadingSpaces(stripTrailingSpaces(line.substring(end)));
        // CommonMark: a backtick fence's info string holds no backtick, so ```foo`` stays a code span in a paragraph.
        if (character == '`' && info.indexOf('`') >= 0) {
            return null;
        }
        return new FenceStart(character, end - indent, indent, info);
    }

    /**
     * Whether a line closes a fenced code block: up to three spaces, then at least as many of the opening fence's
     * character as it has, then nothing but spaces or tabs.
     */
    private static boolean isClosingFence(String line, FenceStart fence) {
        int indent = indentation(line);
        if (indent > 3) {
            return false;
        }
        int end = indent;
        while (end < line.length() && line.charAt(end) == fence.character()) {
            end++;
        }
        return end - indent >= fence.length() && isBlank(line.substring(end));
    }

    /**
     * The opening fence of a fenced code block.
     *
     * @param character the fence character, a backtick or a tilde
     * @param length    the number of fence characters, at least three: a closing fence needs as many or more
     * @param indent    the fence's indentation, up to three spaces, which the code lines lose as far as they have it
     * @param info      the info string after the fence, without the spaces around it
     */
    private record FenceStart(char character, int length, int indent, String info) {
    }

    /**
     * Reads an indented code block from the current line, indented four or more spaces, on: the following lines
     * indented as far, and blank lines between them, as written. Blank lines after its last indented line are not
     * part of it. It starts only where no paragraph is open (a paragraph takes an indented line as its own), and its
     * lines keep their indentation, so the source block shows them where the source has them.
     */
    private SourceBlock indentedCode() {
        int end = index;
        for (int line = index; line < lines.size() && !lazyLines.get(line); line++) {
            String text = lines.get(line);
            if (isBlank(text)) {
                continue;
            }
            if (indentation(text) < 4) {
                break;
            }
            end = line + 1;
        }
        List<String> source = new ArrayList<>(lines.subList(index, end));
        index = end;
        return new SourceBlock(SourceBlock.Kind.INDENTED_CODE, source);
    }

    /**
     * Reads an HTML block from the current line, its start, on: every line up to the next blank line, as written.
     */
    private SourceBlock htmlBlock() {
        List<String> source = new ArrayList<>();
        while (index < lines.size() && !isBlank(lines.get(index)) && !lazyLines.get(index)) {
            source.add(lines.get(index));
            index++;
        }
        return new SourceBlock(SourceBlock.Kind.HTML, source);
    }

    /**
     * Whether a line starts an HTML block: up to three spaces, then {@code <!}, or {@code <} or {@code </} followed
     * by a tag name (an ASCII letter, then letters, digits and {@code -}) that the end of the line, a space, a tab,
     * {@code >} or {@code />} follows.
     *
     * <p>Simpler than CommonMark's seven kinds of HTML block, all of which end at a blank line here. The tag name rule
     * keeps an autolink ({@code <https://...>}, {@code <name@example.com>}) at the start of a line in its paragraph.
     * An HTML block interrupts no paragraph, as CommonMark's general kind (7): a tag on a paragraph's next line stays
     * text of the paragraph, and a quote's or list item's lazy line.
     */
    private static boolean isHtmlBlockStart(String line) {
        int start = indentation(line);
        if (start > 3 || start + 1 >= line.length() || line.charAt(start) != '<') {
            return false;
        }
        int name = start + 1;
        if (line.charAt(name) == '!') {
            return true;
        }
        if (line.charAt(name) == '/') {
            name++;
        }
        if (name >= line.length() || !Characters.isAsciiLetter(line.charAt(name))) {
            return false;
        }
        int end = name + 1;
        while (end < line.length() && (Characters.isAsciiLetter(line.charAt(end))
                || Characters.isDigit(line.charAt(end)) || line.charAt(end) == '-')) {
            end++;
        }
        return end == line.length() || Characters.isSpaceOrTab(line.charAt(end)) || line.charAt(end) == '>'
                || line.startsWith("/>", end);
    }

    /**
     * Reads a GFM table from the current line, its header row, on: the header, the delimiter row and the rows that
     * follow, each a line with a {@code |} that starts no other block. A blank line, a line without {@code |} or a
     * line that starts a block ends it.
     *
     * <p>As in GFM, a body row with fewer cells than the header gets empty cells, and the cells beyond the header's
     * are dropped; each cell is inline Markdown ({@link #tableCell(String)}).
     */
    private Table table() {
        List<Table.Cell> header = tableCells(lines.get(index), -1);
        List<Table.Alignment> alignments = new ArrayList<>();
        for (String delimiter : splitCells(lines.get(index + 1))) {
            alignments.add(alignment(stripLeadingSpaces(stripTrailingSpaces(delimiter))));
        }
        index += 2;
        List<List<Table.Cell>> rows = new ArrayList<>();
        while (index < lines.size() && !lazyLines.get(index) && lines.get(index).indexOf('|') >= 0
                && !startsBlock(lines.get(index))) {
            rows.add(tableCells(lines.get(index), header.size()));
            index++;
        }
        return new Table(header, alignments, rows);
    }

    /**
     * Splits a table row into its cells and parses each.
     *
     * @param count how many cells the row gets, padded with empty cells or cut off; -1 to keep them all
     */
    private List<Table.Cell> tableCells(String line, int count) {
        List<String> texts = splitCells(line);
        List<Table.Cell> cells = new ArrayList<>();
        int size = count < 0 ? texts.size() : count;
        for (int i = 0; i < size; i++) {
            cells.add(i < texts.size() ? tableCell(texts.get(i)) : new Table.Cell(List.of()));
        }
        return cells;
    }

    /**
     * Parses a cell's text as inline Markdown, without the spaces around it. GFM turns every {@code \|} into a
     * {@code |} first, inside code spans too, so an escaped pipe is a pipe wherever it stands in the cell.
     */
    private Table.Cell tableCell(String text) {
        if (probe) {
            return new Table.Cell(List.of());
        }
        String content = stripLeadingSpaces(stripTrailingSpaces(text)).replace("\\|", "|");
        return new Table.Cell(InlineParser.parseInlines(content));
    }

    /**
     * The alignment a delimiter row's cell, without its spaces, gives its column: a colon on the left, the right or
     * both sides.
     */
    private static Table.Alignment alignment(String delimiter) {
        boolean left = delimiter.startsWith(":");
        boolean right = delimiter.endsWith(":");
        if (left && right) {
            return Table.Alignment.CENTER;
        }
        if (left) {
            return Table.Alignment.LEFT;
        }
        if (right) {
            return Table.Alignment.RIGHT;
        }
        return Table.Alignment.NONE;
    }

    /**
     * Whether the line at an index starts a GFM table: a line with a {@code |}, indented up to three spaces, followed
     * by a delimiter row with as many cells. A table interrupts a paragraph, as in GFM, but neither of its first two
     * lines may be a lazy line: a container's lazy lines are paragraph text.
     */
    private boolean isTableStart(int at) {
        if (at + 1 >= lines.size() || lazyLines.get(at) || lazyLines.get(at + 1)) {
            return false;
        }
        String header = lines.get(at);
        if (indentation(header) > 3 || header.indexOf('|') < 0) {
            return false;
        }
        int cells = delimiterCells(lines.get(at + 1));
        return cells > 0 && cells == splitCells(header).size();
    }

    /**
     * Reads a line as a table's delimiter row: up to three spaces, then cells of one or more {@code -}, each with an
     * optional {@code :} before and after and spaces around, separated by {@code |}, with an optional {@code |} at the
     * start and at the end.
     *
     * <p>A line of dashes alone is no delimiter row, nor is a thematic break or a list item: GFM reads a line such as
     * {@code --} under a paragraph as a setext underline, and a thematic break or list item as that block, before it
     * tries a table. Setext headings are not in the subset, so such a line stays what the rest of the parser makes of
     * it.
     *
     * @return the number of cells, 0 if the line is no delimiter row
     */
    private static int delimiterCells(String line) {
        if (indentation(line) > 3 || isThematicBreak(line) || readItemStart(line) != null
                || (line.indexOf('|') < 0 && line.indexOf(':') < 0)) {
            return 0;
        }
        List<String> cells = splitCells(line);
        for (String cell : cells) {
            if (!isDelimiterCell(stripLeadingSpaces(stripTrailingSpaces(cell)))) {
                return 0;
            }
        }
        return cells.size();
    }

    /**
     * Whether a delimiter row's cell, without its spaces, is one or more {@code -} with an optional {@code :} on
     * either side.
     */
    private static boolean isDelimiterCell(String cell) {
        int start = 0;
        int end = cell.length();
        if (start < end && cell.charAt(start) == ':') {
            start++;
        }
        if (end > start && cell.charAt(end - 1) == ':') {
            end--;
        }
        if (start == end) {
            return false;
        }
        for (int i = start; i < end; i++) {
            if (cell.charAt(i) != '-') {
                return false;
            }
        }
        return true;
    }

    /**
     * Splits a table row into its cells at each {@code |} that no backslash escapes, leaving out a leading and a
     * trailing {@code |} and the spaces around the row. As in GFM, a {@code |} inside a code span splits too; only an
     * escaped one, {@code \|}, stays in its cell.
     */
    private static List<String> splitCells(String line) {
        String row = stripLeadingSpaces(stripTrailingSpaces(line));
        int start = 0;
        int end = row.length();
        if (start < end && row.charAt(start) == '|') {
            start++;
        }
        if (end > start && isUnescapedPipe(row, end - 1)) {
            end--;
        }
        List<String> cells = new ArrayList<>();
        int cellStart = start;
        for (int i = start; i < end; i++) {
            if (isUnescapedPipe(row, i)) {
                cells.add(row.substring(cellStart, i));
                cellStart = i + 1;
            }
        }
        cells.add(row.substring(cellStart, end));
        return cells;
    }

    private static boolean isUnescapedPipe(String row, int at) {
        return row.charAt(at) == '|' && (at == 0 || row.charAt(at - 1) != '\\');
    }

    /**
     * Reads a paragraph from the current line on: every following line that is not blank and does not start
     * another block belongs to it. The lines lose their leading spaces, the paragraph its trailing ones.
     */
    private Paragraph paragraph() {
        return paragraph(lines.get(index));
    }

    /**
     * Reads a paragraph from the current line on, with the given text in place of the current line's.
     *
     * @param firstLine the text of the paragraph's first line; empty for a task marker alone on its line, whose
     *                  paragraph then starts with the next line
     */
    private Paragraph paragraph(String firstLine) {
        StringBuilder text = new StringBuilder(stripLeadingSpaces(firstLine));
        index++;
        while (index < lines.size() && !isBlank(lines.get(index))
                && (lazyLines.get(index) || (!interruptsParagraph(lines.get(index)) && !isTableStart(index)))) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(stripLeadingSpaces(lines.get(index)));
            index++;
        }
        if (probe) {
            return new Paragraph(List.of());
        }
        return new Paragraph(InlineParser.parseInlines(stripTrailingSpaces(text.toString())));
    }

    /**
     * Whether a line ends a paragraph by starting a block of its own.
     */
    private boolean interruptsParagraph(String line) {
        // A --- line under a paragraph is a break, never a setext underline: setext headings are not in the subset.
        if (headingLevel(line) > 0 || isThematicBreak(line) || quoteContent(line) != null
                || fenceStart(line) != null) {
            return true;
        }
        // CommonMark: an empty item does not interrupt a paragraph, nor does an ordered item numbered other than 1,
        // so a sentence's line may start with "2026." or a lone "-".
        ItemStart itemStart = itemStart(line);
        return itemStart != null && !isBlank(itemStart.firstLine())
                && (!itemStart.ordered() || itemStart.number() == 1);
    }

    /**
     * The level of an ATX heading line: up to three spaces, one to six {@code #}, then a space, a tab or the end of
     * the line. Setext underlines ({@code ===}, {@code ---}) are not headings in this subset.
     *
     * @return the level, 1 to 6, or 0 if the line is no heading
     */
    private static int headingLevel(String line) {
        int start = indentation(line);
        if (start > 3) {
            return 0;
        }
        int end = start;
        while (end < line.length() && line.charAt(end) == '#') {
            end++;
        }
        int level = end - start;
        if (level > 6 || (end < line.length() && !Characters.isSpaceOrTab(line.charAt(end)))) {
            return 0;
        }
        return level;
    }

    /**
     * Whether a line is a thematic break: up to three spaces, then three or more of one character, {@code -},
     * {@code *} or {@code _}, with any spaces or tabs between and after them, and nothing else.
     */
    private static boolean isThematicBreak(String line) {
        int start = indentation(line);
        if (start > 3 || start >= line.length()) {
            return false;
        }
        char character = line.charAt(start);
        if (character != '-' && character != '*' && character != '_') {
            return false;
        }
        int count = 0;
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == character) {
                count++;
            } else if (!Characters.isSpaceOrTab(c)) {
                return false;
            }
        }
        return count >= 3;
    }

    /**
     * Reads a heading line, whose level {@link #headingLevel(String)} has found.
     */
    private static Heading heading(String line) {
        int level = headingLevel(line);
        String afterOpening = line.substring(indentation(line) + level);
        return new Heading(level, InlineParser.parseInlines(headingText(afterOpening)));
    }

    /**
     * The text of a heading without its optional closing sequence: hashes that follow a space (or fill the whole
     * text) and are followed by spaces only. Other hashes, as in {@code # foo#} or {@code C#}, are text.
     */
    private static String headingText(String afterOpening) {
        String text = stripTrailingSpaces(afterOpening);
        int hashes = text.length();
        while (hashes > 0 && text.charAt(hashes - 1) == '#') {
            hashes--;
        }
        if (hashes < text.length() && (hashes == 0 || Characters.isSpaceOrTab(text.charAt(hashes - 1)))) {
            text = text.substring(0, hashes);
        }
        return stripLeadingSpaces(stripTrailingSpaces(text));
    }

    /**
     * The number of leading spaces of a line.
     */
    private static int indentation(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static boolean isBlank(String line) {
        return stripLeadingSpaces(line).isEmpty();
    }

    private static String stripLeadingSpaces(String text) {
        int start = 0;
        while (start < text.length() && Characters.isSpaceOrTab(text.charAt(start))) {
            start++;
        }
        return text.substring(start);
    }

    private static String stripTrailingSpaces(String text) {
        int end = text.length();
        while (end > 0 && Characters.isSpaceOrTab(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }
}
