package org.karthu.fx.form;

import java.util.List;
import java.util.Objects;

/**
 * A file filter of a path dialog: a description and its patterns, such as <i>CSV files</i> with {@code *.csv}.
 *
 * <p>The patterns are those of JavaFX's {@code FileChooser.ExtensionFilter}: {@code *.csv} for the files ending in
 * {@code .csv}, {@code *} for every file. A filter refuses where it is made what the dialog would refuse only when it
 * opens: an empty description, no pattern, an empty pattern.
 *
 * <p>A {@link PathInput} lists its {@linkplain PathInput#getFilters() filters} in the open and save dialogs and adds
 * <i>All files</i> as the last one itself.
 *
 * @param description the text the dialog shows for the filter, such as <i>CSV files</i>
 * @param patterns    the filter's patterns in their order, at least one, such as {@code *.csv}; an immutable copy
 */
public record PathFilter(String description, List<String> patterns) {

    /**
     * Creates a filter.
     *
     * @param description the text the dialog shows for the filter, not null and not empty
     * @param patterns    the filter's patterns, not null and not empty, no pattern null or empty; copied
     * @throws NullPointerException     if the description, the patterns or a pattern is null
     * @throws IllegalArgumentException if the description is empty, there is no pattern, or a pattern is empty
     */
    public PathFilter {
        Objects.requireNonNull(description, "description must not be null");
        if (description.isEmpty()) {
            throw new IllegalArgumentException("description must not be empty");
        }
        patterns = List.copyOf(Objects.requireNonNull(patterns, "patterns must not be null"));
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("a filter needs at least one pattern");
        }
        for (String pattern : patterns) {
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException("a pattern must not be empty");
            }
        }
    }
}
