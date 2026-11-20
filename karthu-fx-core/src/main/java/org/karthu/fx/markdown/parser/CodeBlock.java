package org.karthu.fx.markdown.parser;

/**
 * A fenced code block: the lines between an opening fence of three or more backticks or tildes and a closing fence,
 * taken as written. An unclosed fence runs to the end of the document or of the container it is in.
 *
 * @param info    the info string after the opening fence, such as {@code java}, without the spaces around it; empty
 *                if there is none. It is read but not used by the view
 * @param literal the code: each content line followed by a line feed, as in CommonMark; the indentation of the
 *                opening fence is removed from each line, up to as many spaces as the line has. Empty for a block
 *                without content lines
 * @param line    the 1-based number of the opening fence's source line; the code's lines follow it one by one
 */
public record CodeBlock(String info, String literal, int line) implements Block {

    /**
     * The source line of one of the code's lines: the lines of a fenced code block follow its opening fence without
     * a gap.
     *
     * @param index the code line's index, from 0 for the line below the opening fence
     * @return the 1-based number of its source line
     */
    public int codeLine(int index) {
        return line + 1 + index;
    }
}
