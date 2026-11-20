package org.karthu.fx.markdown.parser;

/**
 * A block of a Markdown document: a heading, a paragraph, a list, a block quote, a thematic break, a fenced code
 * block, a GFM table, or a source block, which keeps a block the view does not render as its source lines.
 *
 * <p>The set of blocks is closed, so a renderer's {@code switch} over it must handle every kind.
 *
 * <p><b>Source lines.</b> Every block knows the line of the Markdown text it starts on, counted from 1, so a view can
 * tell an application where in the source a block lies. Each line ending, {@code \n}, {@code \r\n} or {@code \r},
 * ends one line. A container's blocks count the lines of the whole text, not of the container. Blocks that hold
 * several lines of their own ({@link CodeBlock}, {@link Table}, {@link SourceBlock}) also tell the line of each, and
 * a list's items carry their own ({@link ListItem#line()}).
 */
public sealed interface Block permits Heading, Paragraph, BulletList, OrderedList, BlockQuote, ThematicBreak,
        CodeBlock, Table, SourceBlock {

    /**
     * The line of the Markdown text the block starts on: a heading's line, a paragraph's first line, a list's first
     * item's marker line, a quote's first line, a code block's opening fence, a table's header row.
     *
     * @return the 1-based line number
     */
    int line();
}
