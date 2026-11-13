package org.karthu.fx.markdown.parser;

/**
 * A line break in the source inside a paragraph: the lines flow together, so it is shown as a space.
 */
public record SoftBreak() implements Inline {
}
