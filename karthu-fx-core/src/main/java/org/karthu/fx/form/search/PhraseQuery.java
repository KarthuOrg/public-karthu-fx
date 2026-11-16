package org.karthu.fx.form.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The matching rule of the search in a document's text, independent of JavaFX: one search, taken as one piece, and
 * where it matches in one text ({@link #find}).
 *
 * <p>Whitespace at the start and the end of the search is dropped. Every run of whitespace inside it matches every run
 * of one or more whitespace characters of the text, so {@code der Bericht} finds {@code der Bericht} also across a
 * line break, a tab or a no-break space, and nothing else does: {@code a b} does not find {@code ab}. Whitespace is
 * what {@link Character#isWhitespace} or {@link Character#isSpaceChar} calls so. Search and text are compared folded as
 * the search inputs fold them ({@link SearchQuery}): lower case in {@link java.util.Locale#ROOT}, then the canonical
 * decomposition (NFD) with every combining mark removed. A word of the search that folds to nothing (a combining mark
 * alone) is dropped, and the whitespace around it is one run; a search of whitespace only, {@code null} and the empty
 * search are no search, which finds nothing.
 *
 * <p>The matches of one text do not overlap: after a match the next one starts after its end, so {@code aa} finds two
 * matches in {@code aaaa}. They are given in the positions of the original text, in its order; a match never splits a
 * surrogate pair and ends after the combining marks of its last letter. The rule takes one text at a time, so a match
 * never spans two texts.
 *
 * <p>Unlike the search inputs' rule, which matches every word of a search anywhere in an entry, this one finds the
 * words only together, in their order: the text a reader looks for in a document.
 *
 * <p>A query is immutable: build one with {@link #of} for each search text, and use it for every text.
 */
public final class PhraseQuery {

    /** The query of no search. */
    private static final PhraseQuery EMPTY = new PhraseQuery(new String[0]);

    /** The words of the search, folded, in their order; none for no search. */
    private final String[] words;

    private PhraseQuery(String[] words) {
        this.words = words;
    }

    /**
     * Returns the query of a search text.
     *
     * @param search the search text as typed; null counts as no search
     * @return the query of the search as one piece
     */
    public static PhraseQuery of(String search) {
        if (search == null) {
            return EMPTY;
        }
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int codePoint : Folding.codePoints(search)) {
            if (Folding.isWhitespace(codePoint)) {
                addWord(words, word);
            } else {
                Folding.appendFolded(word, codePoint);
            }
        }
        addWord(words, word);
        return words.isEmpty() ? EMPTY : new PhraseQuery(words.toArray(new String[0]));
    }

    /** Adds the folded word, unless it is empty, and starts the next one. */
    private static void addWord(List<String> words, StringBuilder word) {
        if (!word.isEmpty()) {
            words.add(word.toString());
            word.setLength(0);
        }
    }

    /**
     * Returns whether this is no search: it was {@code null}, empty, whitespace only, or its words fold to nothing.
     * No search finds nothing.
     *
     * @return true for no search
     */
    public boolean isEmpty() {
        return words.length == 0;
    }

    /**
     * Returns the matches of the search in one text, in the order of the text, not overlapping.
     *
     * @param text the text; null counts as the empty text
     * @return an unmodifiable list of the matches in the positions of the original text, empty for no search
     */
    public List<Mark> find(String text) {
        if (words.length == 0 || text == null || text.isEmpty()) {
            return List.of();
        }
        FoldedText folded = FoldedText.of(text);
        String foldedText = folded.text();
        List<Mark> found = new ArrayList<>();
        int at = foldedText.indexOf(words[0]);
        while (at >= 0) {
            int end = endOfMatchAt(foldedText, at);
            if (end < 0) {
                at = foldedText.indexOf(words[0], at + 1);
                continue;
            }
            Mark match = folded.original(at, end);
            found.add(match);
            // The next match starts after this one's end in the original text: a match ends after the combining marks
            // of its last letter, and a folded letter may take its whole original code point (a Hangul syllable).
            int next = end;
            while (next < foldedText.length() && folded.originalStart(next) < match.end()) {
                next++;
            }
            at = foldedText.indexOf(words[0], next);
        }
        return Collections.unmodifiableList(found);
    }

    /**
     * Where a match that starts with the first word at a position of the folded text ends: each word follows the one
     * before after a run of one or more whitespace characters.
     *
     * @return the position after the match's last char, -1 if the search does not match there
     */
    private int endOfMatchAt(String foldedText, int at) {
        int position = at + words[0].length();
        for (int i = 1; i < words.length; i++) {
            int afterWhitespace = afterWhitespace(foldedText, position);
            if (afterWhitespace == position || !foldedText.startsWith(words[i], afterWhitespace)) {
                return -1;
            }
            position = afterWhitespace + words[i].length();
        }
        return position;
    }

    /**
     * The position after the run of whitespace at a position of a folded text: the position itself without
     * whitespace there. Whitespace folds to whitespace, so the folded text has the original's runs.
     */
    private static int afterWhitespace(String foldedText, int position) {
        int after = position;
        while (after < foldedText.length()) {
            int codePoint = foldedText.codePointAt(after);
            if (!Folding.isWhitespace(codePoint)) {
                break;
            }
            after += Character.charCount(codePoint);
        }
        return after;
    }
}
