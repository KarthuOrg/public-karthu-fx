package org.karthu.fx.markdown.parser;

import java.util.List;

/**
 * A block the view does not render, kept as its source lines so that nothing of a document disappears: an HTML block
 * or an indented code block.
 *
 * @param kind  what the block is in Markdown
 * @param lines the block's lines as written, without line endings and without the indentation or markers of the
 *              containers it is in (a list item's indentation, a quote's {@code >}); blank lines at its end are not
 *              part of it
 * @param line  the 1-based number of the block's first source line; its other lines follow it one by one
 */
public record SourceBlock(Kind kind, List<String> lines, int line) implements Block {

    /**
     * Copies the lines, so the block cannot change afterwards.
     */
    public SourceBlock {
        lines = List.copyOf(lines);
    }

    /**
     * The source line of one of the block's lines.
     *
     * @param index the line's index in {@link #lines()}, from 0
     * @return the 1-based number of its source line
     */
    public int sourceLine(int index) {
        return line + index;
    }

    /**
     * What a source block is in Markdown.
     */
    public enum Kind {
        /**
         * An HTML block: a line that starts with a tag, a closing tag or {@code <!}, up to the next blank line.
         */
        HTML,
        /**
         * An indented code block: lines indented four or more spaces where no paragraph or list item takes them.
         */
        INDENTED_CODE
    }
}
