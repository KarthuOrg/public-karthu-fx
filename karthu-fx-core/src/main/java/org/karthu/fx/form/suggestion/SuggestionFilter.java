package org.karthu.fx.form.suggestion;

import java.util.ArrayList;
import java.util.List;

/**
 * The suggestion filter of the editable combo input, independent of JavaFX: which suggestions a typed text keeps.
 *
 * <p>A suggestion is kept when it contains the text, ignoring case; the empty text keeps every suggestion. The text
 * counts as typed: whitespace is neither trimmed nor ignored, and nothing is completed or corrected. Case is ignored
 * letter by letter, as {@link String#regionMatches(boolean, int, String, int, int)} compares, so no locale's rules apply
 * and no letter matches through an expansion (the German sharp s matches only itself, never {@code ss}).
 *
 * <p>This package is not exported: the filter is pinned down by its own tests, and the
 * editable combo input is its only caller.
 */
public final class SuggestionFilter {

    private SuggestionFilter() {
    }

    /**
     * Returns the suggestions a text keeps: those that contain it, ignoring case, in their order and with their
     * duplicates; every suggestion for the empty text. A null suggestion is never kept.
     *
     * @param suggestions the suggestions, in their order
     * @param text        the typed text; null counts as the empty text
     * @return a new list of the kept suggestions
     */
    public static List<String> filter(List<String> suggestions, String text) {
        List<String> kept = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion != null && contains(suggestion, text == null ? "" : text)) {
                kept.add(suggestion);
            }
        }
        return kept;
    }

    /** Whether the suggestion contains the text somewhere, ignoring case letter by letter. */
    private static boolean contains(String suggestion, String text) {
        int lastStart = suggestion.length() - text.length();
        for (int start = 0; start <= lastStart; start++) {
            if (suggestion.regionMatches(true, start, text, 0, text.length())) {
                return true;
            }
        }
        return false;
    }
}
