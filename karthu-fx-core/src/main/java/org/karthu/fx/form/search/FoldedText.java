package org.karthu.fx.form.search;

import java.util.Arrays;

/**
 * A text folded as {@link Folding} folds it, with the way back: for each char of the folded text, the range of the
 * original text it came from.
 *
 * <p>The range of a folded char is the whole original code point it came from (so a range never splits a surrogate
 * pair or a Hangul syllable), extended over the code points that follow it and fold to nothing (its combining marks).
 */
final class FoldedText {

    /** The folded text. */
    private final String text;
    /** For each folded char, where its original code point starts. */
    private final int[] starts;
    /** For each folded char, where its original code point and its trailing combining marks end. */
    private final int[] ends;

    private FoldedText(String text, int[] starts, int[] ends) {
        this.text = text;
        this.starts = starts;
        this.ends = ends;
    }

    /**
     * The folded text.
     *
     * @return the text, folded
     */
    String text() {
        return text;
    }

    /**
     * Folds a text and records where each folded char came from.
     *
     * @param original the text
     * @return the folded text with its way back
     */
    static FoldedText of(String original) {
        StringBuilder folded = new StringBuilder(original.length());
        int[] starts = new int[original.length() + 1];
        int[] ends = new int[original.length() + 1];
        // Where the folded chars of the last code point that folded to something begin; -1 before the first one.
        int lastGroup = -1;
        // Where the code point starts in the original text.
        int index = 0;
        for (int codePoint : Folding.codePoints(original)) {
            int next = index + Character.charCount(codePoint);
            int before = folded.length();
            Folding.appendFolded(folded, codePoint);
            int after = folded.length();
            if (after > starts.length) {
                int capacity = Math.max(after, starts.length * 2);
                starts = Arrays.copyOf(starts, capacity);
                ends = Arrays.copyOf(ends, capacity);
            }
            if (after > before) {
                for (int position = before; position < after; position++) {
                    starts[position] = index;
                    ends[position] = next;
                }
                lastGroup = before;
            } else if (lastGroup >= 0) {
                // A combining mark: it belongs to the letter before it, whose chars now end after it.
                for (int position = lastGroup; position < after; position++) {
                    ends[position] = next;
                }
            }
            index = next;
        }
        return new FoldedText(folded.toString(), starts, ends);
    }

    /**
     * The mark of the original text that the folded chars from {@code start} to {@code end} came from.
     *
     * @param start the first folded char
     * @param end   the folded char after the last one, greater than {@code start}
     * @return the original range
     */
    Mark original(int start, int end) {
        return new Mark(starts[start], ends[end - 1]);
    }

    /**
     * Where the original code point of a folded char starts.
     *
     * @param folded the folded char, less than the folded text's length
     * @return its position in the original text
     */
    int originalStart(int folded) {
        return starts[folded];
    }
}
