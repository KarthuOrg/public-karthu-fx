package org.karthu.fx.form.search;

/**
 * One mark of a text: the characters from {@code start} (inclusive) to {@code end} (exclusive), in the positions of
 * the original text, never the folded one.
 *
 * <p>A mark covers at least one character. It never splits a character of two chars (a surrogate pair), and it ends
 * after the combining marks that follow its last letter, so a letter and its accents always stay in one mark.
 *
 * @param start the position of the first marked character, at least 0
 * @param end   the position after the last marked character, greater than {@code start}
 */
public record Mark(int start, int end) {

    /**
     * Checks the range.
     *
     * @throws IllegalArgumentException when {@code start} is negative or {@code end} is not greater than {@code start}
     */
    public Mark {
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("A mark needs 0 <= start < end, found " + start + " to " + end);
        }
    }
}
