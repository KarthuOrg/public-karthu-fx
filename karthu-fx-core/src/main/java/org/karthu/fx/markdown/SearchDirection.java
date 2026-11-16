package org.karthu.fx.markdown;

/**
 * The direction in which a {@link MarkdownView}'s search goes from match to match, see
 * {@link MarkdownView#searchDirectionProperty()}. The matches are numbered from the top of the document whatever the
 * direction; the direction decides which match a new search starts at and where the next one lies.
 *
 * @since 1.1.0-RC2
 */
public enum SearchDirection {

    /**
     * Towards the end of the document: a new search starts at the first match, and the next match is the one below.
     */
    FORWARD,

    /**
     * Towards the start of the document: a new search starts at the last match, and the next match is the one above.
     */
    BACKWARD
}
