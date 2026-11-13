package org.karthu.fx.markdown.parser;

/**
 * Literal text, with backslash escapes already resolved.
 *
 * @param text the text as it is shown
 */
public record PlainText(String text) implements Inline {
}
