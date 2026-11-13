package org.karthu.fx.form.search;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The matching rule of the search and suggestion inputs, independent of JavaFX: one search, split into words, which
 * decides whether an entry matches ({@link #matches(List)}, {@link #matches(String)}, {@link #predicate}) and where
 * the marks of one text go ({@link #marks}).
 *
 * <p>The search is split at any run of whitespace into words; whitespace is what {@link Character#isWhitespace} or
 * {@link Character#isSpaceChar} calls so, the no-break spaces included. Words and texts are compared folded: lower case
 * in {@link Locale#ROOT}, then the canonical decomposition (NFD) with every combining mark removed (the Unicode
 * categories Mn, Mc and Me), so {@code zurich} finds {@code Zürich} and {@code zürich} finds {@code Zurich}, whether the
 * text holds the letter precomposed or decomposed. Nothing else folds: neither the sharp s and {@code ss}, nor a
 * ligature and its letters, nor a letter without a canonical decomposition ({@code ø} and {@code o}). A word that folds
 * to nothing (a combining mark alone) is dropped; a search without words is empty, matches every entry and marks
 * nothing.
 *
 * <p>An entry matches when every word occurs, as a contiguous part, in at least one of its texts; a word never spans
 * two texts. The marks of one text are every occurrence of every word in it, given in the positions of the original
 * text, overlapping and touching ones merged.
 *
 * <p>A query is immutable: build one with {@link #of} for each search text, and use it for every entry.
 *
 * <p>This package is not exported: the rule is pinned down by its own tests, and the search and
 * suggestion inputs are its only callers.
 */
public final class SearchQuery {

    /** The query of the empty search. */
    private static final SearchQuery EMPTY = new SearchQuery(List.of());

    /** The words of the search, folded, in their order; empty for the empty search. */
    private final List<String> words;

    private SearchQuery(List<String> words) {
        this.words = words;
    }

    /**
     * Returns the query of a search text.
     *
     * @param search the search text as typed; null counts as the empty search
     * @return the query of its words
     */
    public static SearchQuery of(String search) {
        if (search == null) {
            return EMPTY;
        }
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int codePoint : codePoints(search)) {
            if (isWhitespace(codePoint)) {
                addWord(words, word);
            } else {
                appendFolded(word, codePoint);
            }
        }
        addWord(words, word);
        return words.isEmpty() ? EMPTY : new SearchQuery(List.copyOf(words));
    }

    /** Adds the folded word, unless it is empty, and starts the next one. */
    private static void addWord(List<String> words, StringBuilder word) {
        if (!word.isEmpty()) {
            words.add(word.toString());
            word.setLength(0);
        }
    }

    /**
     * Returns whether the search has no words: it was empty, whitespace only, or its words fold to nothing. The empty
     * search matches every entry and marks nothing.
     *
     * @return true for the empty search
     */
    public boolean isEmpty() {
        return words.isEmpty();
    }

    /**
     * Returns whether an entry with one text matches: every word occurs in the text.
     *
     * @param text the entry's text; null counts as the empty text
     * @return true when every word occurs in the text, always true for the empty search
     */
    public boolean matches(String text) {
        // An entry with this one text; a list that allows the null text, which counts as the empty one.
        return matches(Collections.singletonList(text));
    }

    /**
     * Returns whether an entry with several texts matches: every word occurs, as a contiguous part, in at least one of
     * them. Each word may occur in another text; no word spans two texts.
     *
     * @param texts the entry's texts; a null text counts as an empty one
     * @return true when every word occurs in one of the texts, always true for the empty search, also without texts
     */
    public boolean matches(List<String> texts) {
        if (words.isEmpty()) {
            return true;
        }
        List<String> folded = new ArrayList<>(texts.size());
        for (String text : texts) {
            folded.add(fold(text));
        }
        for (String word : words) {
            if (!occursInOne(word, folded)) {
                return false;
            }
        }
        return true;
    }

    /** Whether the word occurs in one of the folded texts. */
    private static boolean occursInOne(String word, List<String> foldedTexts) {
        for (String folded : foldedTexts) {
            if (folded.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a predicate that keeps the entries whose text matches, as {@link #matches(String)} decides, for a
     * filtered list: every entry for the empty search.
     *
     * @param text the text of an entry, the one its row shows; a null result counts as the empty text
     * @param <T>  the type of the entries
     * @return the predicate of this query
     */
    public <T> Predicate<T> predicate(Function<? super T, String> text) {
        return entry -> matches(text.apply(entry));
    }

    /**
     * Returns the marks of one text: every occurrence of every word, as ranges in the positions of the original text,
     * overlapping and touching occurrences merged into one mark, in the order of the text.
     *
     * <p>Folding may change a text's length (a decomposed accent is removed, a Hangul syllable decomposes into its
     * letters); the marks still cover exactly the original characters whose folded form was found. A mark never splits
     * a surrogate pair, and it extends over the combining marks that follow its last letter, so a letter and its accents
     * are always marked together. The marks do not depend on whether the entry matches: a word that occurs is marked
     * even when another word does not occur.
     *
     * @param text the text; null counts as the empty text
     * @return an unmodifiable list of the marks, empty for the empty search
     */
    public List<Mark> marks(String text) {
        if (words.isEmpty() || text == null || text.isEmpty()) {
            return List.of();
        }
        FoldedText folded = FoldedText.of(text);
        List<Mark> found = new ArrayList<>();
        for (String word : words) {
            int at = folded.text().indexOf(word);
            while (at >= 0) {
                found.add(folded.original(at, at + word.length()));
                // One char further, not one word further: overlapping occurrences are all found ("ana" in "banana").
                at = folded.text().indexOf(word, at + 1);
            }
        }
        return Collections.unmodifiableList(merged(found));
    }

    /** The marks sorted by their start, overlapping and touching ones merged. */
    private static List<Mark> merged(List<Mark> marks) {
        if (marks.isEmpty()) {
            return marks;
        }
        marks.sort(Comparator.comparingInt(Mark::start));
        List<Mark> merged = new ArrayList<>();
        int start = marks.getFirst().start();
        int end = marks.getFirst().end();
        for (Mark mark : marks) {
            if (mark.start() <= end) {
                end = Math.max(end, mark.end());
            } else {
                merged.add(new Mark(start, end));
                start = mark.start();
                end = mark.end();
            }
        }
        merged.add(new Mark(start, end));
        return merged;
    }

    // --- Folding --------------------------------------------------------------------------------------

    /** The folded form of a text, without positions. */
    private static String fold(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder folded = new StringBuilder(text.length());
        for (int codePoint : codePoints(text)) {
            appendFolded(folded, codePoint);
        }
        return folded.toString();
    }

    /** The code points of a text, in order: a surrogate pair is one code point, as in every walk of this rule. */
    private static int[] codePoints(String text) {
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
     */
    private static void appendFolded(StringBuilder folded, int codePoint) {
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

    /** Whether the code point is a combining mark: Unicode category Mn, Mc or Me. */
    private static boolean isCombiningMark(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    /** Whether the code point separates words: Java's whitespace or a Unicode space, the no-break spaces included. */
    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    /**
     * A folded text with the way back: for each char of the folded text, the range of the original text it came from.
     *
     * <p>The range of a folded char is the whole original code point it came from (so a range never splits a surrogate
     * pair or a Hangul syllable), extended over the code points that follow it and fold to nothing (its combining
     * marks).
     */
    private static final class FoldedText {

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

        /** The folded text. */
        String text() {
            return text;
        }

        /** Folds a text and records where each folded char came from. */
        static FoldedText of(String original) {
            StringBuilder folded = new StringBuilder(original.length());
            int[] starts = new int[original.length() + 1];
            int[] ends = new int[original.length() + 1];
            // Where the folded chars of the last code point that folded to something begin; -1 before the first one.
            int lastGroup = -1;
            // Where the code point starts in the original text.
            int index = 0;
            for (int codePoint : codePoints(original)) {
                int next = index + Character.charCount(codePoint);
                int before = folded.length();
                appendFolded(folded, codePoint);
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

        /** The mark of the original text that the folded chars from {@code start} to {@code end} came from. */
        Mark original(int start, int end) {
            return new Mark(starts[start], ends[end - 1]);
        }
    }
}
