package org.karthu.fx.markdown.parser;

/**
 * A block of a Markdown document: a heading, a paragraph, a list, a block quote, a thematic break, a fenced code
 * block, a GFM table, or a source block, which keeps a block the view does not render as its source lines.
 *
 * <p>The set of blocks is closed, so a renderer's {@code switch} over it must handle every kind.
 */
public sealed interface Block permits Heading, Paragraph, BulletList, OrderedList, BlockQuote, ThematicBreak,
        CodeBlock, Table, SourceBlock {
}
