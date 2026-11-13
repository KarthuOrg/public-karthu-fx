package org.karthu.fx.markdown.parser;

/**
 * Inline code between backtick runs of the same length.
 *
 * @param code the code as it is shown: literal, its line endings already turned into spaces
 */
public record CodeSpan(String code) implements Inline {
}
