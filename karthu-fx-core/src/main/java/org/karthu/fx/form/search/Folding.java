package org.karthu.fx.form.search;

import java.text.Normalizer;
import java.util.Locale;

/**
 * The folding both matching rules share, the search inputs' ({@link SearchQuery}) and the document search's
 * ({@link PhraseQuery}): lower case in {@link Locale#ROOT}, then the canonical decomposition (NFD) with every combining
 * mark removed (the Unicode categories Mn, Mc and Me). Nothing else folds: neither the sharp s and {@code ss}, nor a
 * ligature and its letters, nor a letter without a canonical decomposition. Whitespace is what
 * {@link Character#isWhitespace} or {@link Character#isSpaceChar} calls so, the no-break spaces included; it folds
 * to whitespace.
 */
final class Folding {

    private Folding() {
    }

    /**
     * The folded form of a text, without positions.
     *
     * @param text the text; null counts as the empty text
     * @return the folded text
     */
    static String fold(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder folded = new StringBuilder(text.length());
        for (int codePoint : codePoints(text)) {
            appendFolded(folded, codePoint);
        }
        return folded.toString();
    }

    /**
     * The code points of a text, in order: a surrogate pair is one code point, as in every walk of the rules.
     *
     * @param text the text
     * @return its code points
     */
    static int[] codePoints(String text) {
        return text.codePoints().toArray();
    }

    /**
     * Appends the folded form of one code point: lower case in {@link Locale#ROOT}, decomposed (NFD), combining marks
     * removed.
     *
     * <p>Folding a text code point by code point gives the same as folding it as a whole: the decomposition of a text
     * is the decomposition of each code point, then a reordering of combining marks, which are all removed here. The
     * one context-dependent lower case mapping, the Greek final sigma, is not applied, so a capital sigma always folds to
     * the same small sigma.
     *
     * @param folded    where to append
     * @param codePoint the code point
     */
    static void appendFolded(StringBuilder folded, int codePoint) {
        if (codePoint < 0x80) {
            // ASCII: no decomposition, and its lower case in Locale.ROOT is the plain one.
            folded.append(Character.toLowerCase((char) codePoint));
            return;
        }
        String lower = Character.toString(codePoint).toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        for (int part : codePoints(decomposed)) {
            if (!isCombiningMark(part)) {
                folded.appendCodePoint(part);
            }
        }
    }

    /**
     * Whether a code point is whitespace: Java's whitespace or a Unicode space, the no-break spaces included.
     *
     * @param codePoint the code point
     * @return true for whitespace
     */
    static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    /** Whether the code point is a combining mark: Unicode category Mn, Mc or Me. */
    private static boolean isCombiningMark(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }
}
