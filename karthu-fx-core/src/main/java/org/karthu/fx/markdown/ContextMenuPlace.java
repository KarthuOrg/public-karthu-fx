package org.karthu.fx.markdown;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Where a {@link MarkdownView}'s context menu was requested, as a {@link ContextMenuCallback} receives it: the source
 * line under the right click, or under the focused node for a request from the keyboard. The line is that of the
 * block, list item, table row (the header row too) or code or source line clicked, as <em>Source lines</em> in the
 * view's description counts them; a line inside a wrapped or multi-line paragraph is not told apart, the paragraph's
 * first line counts. Where the click hit no block, as in the margins or between blocks, there is no line.
 *
 * @param sourceLine the 1-based line of the Markdown text under the click; empty where the click hit no block
 * @since 1.1.0-RC2
 */
public record ContextMenuPlace(OptionalInt sourceLine) {

    /**
     * Creates a place.
     *
     * @param sourceLine the 1-based line of the Markdown text under the click, empty for none; not {@code null}
     * @throws NullPointerException if the line is {@code null}
     * @since 1.1.0-RC2
     */
    public ContextMenuPlace {
        Objects.requireNonNull(sourceLine, "sourceLine");
    }
}
