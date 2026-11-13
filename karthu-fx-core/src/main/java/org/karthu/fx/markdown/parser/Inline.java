package org.karthu.fx.markdown.parser;

/**
 * An inline element of a heading or a paragraph.
 *
 * <p>The set of inlines is closed, so a renderer's {@code switch} over it must handle every kind.
 */
public sealed interface Inline permits PlainText, CodeSpan, Emphasis, Strong, Link, Image, SoftBreak,
        HardBreak {
}
