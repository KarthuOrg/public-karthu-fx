package org.karthu.fx.markdown.parser;

/**
 * A thematic break: a line of three or more {@code -}, {@code *} or {@code _}, optionally with spaces between them.
 *
 * @param line the 1-based number of the break's source line
 */
public record ThematicBreak(int line) implements Block {
}
