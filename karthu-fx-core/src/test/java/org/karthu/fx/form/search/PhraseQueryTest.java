package org.karthu.fx.form.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The matching rule of the search in a document's text without JavaFX: the search as one piece, and where its matches
 * lie in one text.
 *
 * <p>The matches are written as the text with each match in brackets, {@code [Anna] Muster}, by hand, so a row never
 * computes its expectation the way the rule does. Letters beyond ASCII are Unicode escapes, so the build does not
 * depend on the source encoding: {@code \u00FC} is {@code u} with diaeresis (precomposed), {@code \u0308} the combining
 * diaeresis, {@code \u00E9} {@code e} with acute, {@code \u00C5} capital {@code a} with ring, {@code \u212B} the
 * angstrom sign, {@code \u00D8} capital {@code o} with stroke (no decomposition), {@code \u00DF} sharp s,
 * {@code \u0130} capital {@code I} with dot above, {@code \u00A0} the no-break space, {@code \u2009} the thin space,
 * {@code \u3000} the ideographic space, {@code \uFFFC} the object replacement character. In the tables a line feed is
 * written {@code \n}, a backslash and an {@code n}, so a row stays on its line.
 */
class PhraseQueryTest {

    /** The text with every match in brackets, from the matches the rule finds. */
    private static String found(String search, String text) {
        List<Mark> matches = PhraseQuery.of(search).find(text);
        StringBuilder marked = new StringBuilder();
        int position = 0;
        for (Mark match : matches) {
            marked.append(text, position, match.start()).append('[');
            marked.append(text, match.start(), match.end()).append(']');
            position = match.end();
        }
        return marked.append(text, position, text.length()).toString();
    }

    /** A value of a table with each {@code \n} written as a backslash and an {@code n} turned into a line feed. */
    private static String lines(String value) {
        return value.replace("\\n", "\n");
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("the search is one piece: its words in their order, next to each other")
    @CsvSource(delimiter = '|', textBlock = """
             1 | anna                     | Anna Muster              | [Anna] Muster
             2 | anna muster              | Anna Muster              | [Anna Muster]
             3 | muster anna              | Anna Muster              | Anna Muster
             4 | der bericht              | Der Bericht, der Text    | [Der Bericht], der Text
             5 | der                      | Der Bericht, der Text    | [Der] Bericht, [der] Text
             6 | annamuster               | Anna Muster              | Anna Muster
             7 | na mu                    | Anna Muster              | An[na Mu]ster
             8 | 'anna, m'                | Anna, Muster             | [Anna, M]uster
             9 | 'anna m'                 | Anna, Muster             | Anna, Muster
            """)
    void onePiece(int row, String search, String text, String expected) {
        assertEquals(expected, found(search, text));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("a run of whitespace in the search matches any run of whitespace in the text, the no-break spaces"
            + " included, and only whitespace")
    @CsvSource(delimiter = '|', textBlock = """
             1 | 'a b'                    | 'a b'                    | '[a b]'
             2 | 'a b'                    | 'a    b'                 | '[a    b]'
             3 | 'a b'                    | 'a\\nb'                  | '[a\\nb]'
             4 | 'a b'                    | 'a\tb'                   | '[a\tb]'
             5 | 'a b'                    | 'a\u00A0b'               | '[a\u00A0b]'
             6 | 'a b'                    | 'a \\n\t\u2009b'          | '[a \\n\t\u2009b]'
             7 | 'a b'                    | 'a\u3000b'               | '[a\u3000b]'
             8 | 'a   b'                  | 'a b'                    | '[a b]'
             9 | 'a\t\u00A0b'             | 'a b'                    | '[a b]'
            10 | 'a b'                    | 'ab'                     | 'ab'
            11 | 'a b'                    | 'a-b'                    | 'a-b'
            12 | 'a b'                    | 'a \uFFFC b'             | 'a \uFFFC b'
            13 | 'a b c'                  | 'a  b\\nc a b'           | '[a  b\\nc] a b'
            """)
    void whitespaceRuns(int row, String search, String text, String expected) {
        assertEquals(lines(expected), found(lines(search), lines(text)));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("whitespace at the start and the end of the search is dropped; whitespace alone is no search")
    @CsvSource(delimiter = '|', textBlock = """
             1 | '  anna  '               | 'Anna Muster'            | '[Anna] Muster'
             2 | '\tanna\u00A0'           | 'Anna Muster'            | '[Anna] Muster'
             3 | ' anna muster '          | 'Anna Muster'            | '[Anna Muster]'
             4 | '   '                    | 'a b'                    | 'a b'
             5 | ''                       | 'a b'                    | 'a b'
             6 | '\\n\u00A0\t'            | 'a b'                    | 'a b'
            """)
    void endsOfTheSearch(int row, String search, String text, String expected) {
        assertEquals(expected, found(lines(search), text));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("case and accents fold on both sides, as the search inputs fold them, positions in the original text")
    @CsvSource(delimiter = '|', textBlock = """
             1 | bericht                  | Der BERICHT              | Der [BERICHT]
             2 | zurich                   | Z\u00FCrich              | [Z\u00FCrich]
             3 | z\u00FCrich              | Zurich                   | [Zurich]
             4 | zu                       | Zu\u0308rich             | [Zu\u0308]rich
             5 | u                        | Zu\u0308rich             | Z[u\u0308]rich
             6 | e                        | Caf\u00E9 e              | Caf[\u00E9] [e]
             7 | \u00C5                   | \u212B                   | [\u212B]
             8 | i                        | \u0130stanbul            | [\u0130]stanbul
             9 | 'z\u00FCrich see'        | 'Zu\u0308rich\u00A0See'  | '[Zu\u0308rich\u00A0See]'
            10 | o                        | \u00D8resund             | \u00D8resund
            11 | ss                       | Stra\u00DFe              | Stra\u00DFe
            """)
    void folding(int row, String search, String text, String expected) {
        assertEquals(expected, found(search, text));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("matches do not overlap: the next one starts after the end of the one before")
    @CsvSource(delimiter = '|', textBlock = """
             1 | aa                       | aaaa                     | [aa][aa]
             2 | aa                       | aaa                      | [aa]a
             3 | ana                      | banana                   | b[ana]na
             4 | a a                      | 'a a a'                  | '[a a] a'
             5 | a                        | aaa                      | [a][a][a]
             6 | u                        | u\u0308u\u0308           | [u\u0308][u\u0308]
            """)
    void noOverlap(int row, String search, String text, String expected) {
        assertEquals(expected, found(search, text));
    }

    @Test
    @DisplayName("a match never splits a surrogate pair and ends after the combining marks of its last letter")
    void surrogatePairsAndCombiningMarks() {
        // A capital Deseret letter (two chars) folds to the small one.
        assertEquals(List.of(new Mark(1, 3)), PhraseQuery.of("\uD801\uDC28").find("x\uD801\uDC00y"));
        assertEquals(List.of(new Mark(0, 4)), PhraseQuery.of("\uD801\uDC28 y").find("\uD801\uDC00 y"));
        // o with a circumflex and an acute, both combining: the match takes both.
        assertEquals(List.of(new Mark(1, 4)), PhraseQuery.of("o").find("to\u0302\u0301t"));
        // A Hangul syllable decomposes into its letters: a match of one letter takes the whole syllable.
        assertEquals(List.of(new Mark(1, 2)), PhraseQuery.of("\uAD6D").find("\uD55C\uAD6D"));
    }

    @Test
    @DisplayName("the matches are given in the positions of the original text, in its order")
    void positions() {
        assertEquals(List.of(new Mark(0, 2), new Mark(8, 10)), PhraseQuery.of("zu").find("Z\u00FCrich, zu"));
        assertEquals(List.of(new Mark(1, 8)), PhraseQuery.of("zurich").find("\u00FCZu\u0308rich!"));
        assertEquals(List.of(new Mark(2, 8)), PhraseQuery.of("a  b").find("x a \n\t b y"));
    }

    @Test
    @DisplayName("an empty text, or none, has no matches, and neither has the empty search")
    void nothingFound() {
        assertEquals(List.of(), PhraseQuery.of("anna").find(""));
        assertEquals(List.of(), PhraseQuery.of("anna").find(null));
        assertEquals(List.of(), PhraseQuery.of(null).find("Anna"));
        assertEquals(List.of(), PhraseQuery.of("annas").find("Anna"));
    }

    @Test
    @DisplayName("no search: null, the empty text, whitespace only, or words that fold to nothing")
    void emptySearch() {
        assertTrue(PhraseQuery.of(null).isEmpty());
        assertTrue(PhraseQuery.of("").isEmpty());
        assertTrue(PhraseQuery.of(" \t\n\u00A0").isEmpty());
        assertTrue(PhraseQuery.of("\u0308 \u0301").isEmpty());
        assertFalse(PhraseQuery.of(" a ").isEmpty());
        // A word that folds to nothing is dropped, and the whitespace around it is one run.
        assertEquals("[a b]", found("a \u0308 b", "a b"));
    }

    @Test
    @DisplayName("the matches are an unmodifiable list")
    void matchesAreUnmodifiable() {
        List<Mark> matches = PhraseQuery.of("an").find("Anna Hanna");

        assertThrows(UnsupportedOperationException.class, () -> matches.add(new Mark(0, 1)));
    }
}
