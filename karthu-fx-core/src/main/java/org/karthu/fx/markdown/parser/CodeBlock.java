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
 */
public record CodeBlock(String info, String literal) implements Block {
}
