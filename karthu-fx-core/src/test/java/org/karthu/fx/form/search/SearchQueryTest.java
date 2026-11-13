package org.karthu.fx.form.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The matching rule of the search input without JavaFX: whether an entry matches a search, and where the marks of one
 * text go.
 *
 * <p>The marks are written as the text with each mark in brackets, {@code [Anna] Muster}, by hand, so a row never
 * computes its expectation the way the rule does. Letters beyond ASCII are Unicode escapes, so the build
 * does not depend on the source encoding: {@code \u00FC} is {@code u} with diaeresis (precomposed), {@code \u0308} the
 * combining diaeresis, {@code \u00E9}/{@code \u00E8} {@code e} with acute/grave, {@code \u00C5}/{@code \u00E5} capital/
 * small {@code a} with ring, {@code \u212B} the angstrom sign, {@code \u00F6} {@code o} with diaeresis, {@code \u00D8}/
 * {@code \u00F8} capital/small {@code o} with stroke (no decomposition), {@code \u00DF} sharp s, {@code \u1E9E} capital
 * sharp s, {@code \u00C6} the ligature AE, {@code \uFB01} the ligature fi, {@code \u0130} capital {@code I} with dot
 * above, {@code \u00A0} the no-break space.
 */
class SearchQueryTest {

    /** The text with every mark in brackets, from the marks the rule gives. */
    private static String marked(String search, String text) {
        List<Mark> marks = SearchQuery.of(search).marks(text);
        StringBuilder marked = new StringBuilder();
        int position = 0;
        for (Mark mark : marks) {
            marked.append(text, position, mark.start()).append('[');
            marked.append(text, mark.start(), mark.end()).append(']');
            position = mark.end();
        }
        return marked.append(text, position, text.length()).toString();
    }

    /** The texts of one table cell, separated by semicolons. */
    private static List<String> texts(String cell) {
        List<String> texts = new ArrayList<>();
        for (String text : cell.split(";")) {
            texts.add(text.strip());
        }
        return texts;
    }

    // --- The marks of one text -----------------------------------------------------------------------

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("words and whitespace: every word is searched on its own, whatever whitespace separates them")
    @CsvSource(delimiter = '|', textBlock = """
             1 | anna                     | Anna Muster              | [Anna] Muster
             2 | anna muster              | Anna Muster              | [Anna] [Muster]
             3 | muster anna              | Anna Muster              | [Anna] [Muster]
             4 | '  anna   muster  '      | Anna Muster              | [Anna] [Muster]
             5 | 'anna\tmuster'           | Anna Muster              | [Anna] [Muster]
             6 | 'anna\u00A0muster'       | Anna Muster              | [Anna] [Muster]
             7 | anna z\u00FCrich         | Anna Muster, Z\u00FCrich | [Anna] Muster, [Z\u00FCrich]
             8 | anna bern                | Anna Muster, Z\u00FCrich | [Anna] Muster, Z\u00FCrich
             9 | annamuster               | Anna Muster              | Anna Muster
            """)
    void wordsAndWhitespace(int row, String search, String text, String expected) {
        assertEquals(expected, marked(search, text));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("case: folded on both sides, letters beyond ASCII and beyond the basic plane included")
    @CsvSource(delimiter = '|', textBlock = """
             1 | file                     | TEST-FILE                | TEST-[FILE]
             2 | FILE                     | test-file                | test-[file]
             3 | tEsT-fIlE                | TEST-file                | [TEST-file]
             4 | \u00E4rger               | \u00C4RGER               | [\u00C4RGER]
             5 | i                        | \u0130stanbul            | [\u0130]stanbul
             6 | \u0130st                 | istanbul                 | [ist]anbul
             7 | \u00DF                   | STRA\u1E9EE              | STRA[\u1E9E]E
             8 | \uD801\uDC28             | x\uD801\uDC00y           | x[\uD801\uDC00]y
            """)
    void caseFolds(int row, String search, String text, String expected) {
        assertEquals(expected, marked(search, text));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("accents: folded on both sides, the marks in the positions of the original text")
    @CsvSource(delimiter = '|', textBlock = """
             1 | zurich                   | Z\u00FCrich              | [Z\u00FCrich]
             2 | z\u00FCrich              | Zurich                   | [Zurich]
             3 | zu                       | Z\u00FCrich              | [Z\u00FC]rich
             4 | \u00FC                   | Zurich                   | Z[u]rich
             5 | rich                     | Z\u00FCrich              | Z\u00FC[rich]
             6 | e                        | Caf\u00E9 cr\u00E8me     | Caf[\u00E9] cr[\u00E8]m[e]
             7 | a                        | \u00C5sa                 | [\u00C5]s[a]
             8 | \u00E5                   | Aarhus                   | [Aa]rhus
             9 | angstrom                 | \u212Bngstr\u00F6m       | [\u212Bngstr\u00F6m]
            10 | \u00C5                   | \u212B                   | [\u212B]
            11 | zu                       | Zu\u0308rich             | [Zu\u0308]rich
            12 | z\u00FC                  | Zu\u0308rich             | [Zu\u0308]rich
            13 | u                        | Zu\u0308rich             | Z[u\u0308]rich
            14 | rich                     | Zu\u0308rich             | Zu\u0308[rich]
            15 | zurich                   | Zu\u0308rich             | [Zu\u0308rich]
            16 | zu\u0308                 | Z\u00FCrich              | [Z\u00FC]rich
            17 | o                        | to\u0302\u0301t          | t[o\u0302\u0301]t
            18 | \uAD6D                   | \uD55C\uAD6D             | \uD55C[\uAD6D]
            """)
    void accentsFold(int row, String search, String text, String expected) {
        assertEquals(expected, marked(search, text));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("not folded: letters without a canonical decomposition, sharp s and ss, ligatures")
    @CsvSource(delimiter = '|', textBlock = """
             1 | o                        | \u00D8resund             | \u00D8resund
             2 | \u00F8                   | Oslo                     | Oslo
             3 | ss                       | Stra\u00DFe              | Stra\u00DFe
             4 | \u00DF                   | Strasse                  | Strasse
             5 | ae                       | \u00C6ther               | \u00C6ther
             6 | fi                       | \uFB01le                 | \uFB01le
            """)
    void notFolded(int row, String search, String text, String expected) {
        assertEquals(expected, marked(search, text));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("every occurrence of every word is marked, overlapping and touching ones merged into one mark")
    @CsvSource(delimiter = '|', textBlock = """
             1 | an                       | Anna Hanna               | [An]na H[an]na
             2 | na                       | Anna Hanna               | An[na] Han[na]
             3 | ana                      | banana                   | b[anana]
             4 | aa                       | aaaa                     | [aaaa]
             5 | ann nna                  | Anna                     | [Anna]
             6 | anna anna                | Anna                     | [Anna]
             7 | ab cd                    | abcd                     | [abcd]
             8 | ab                       | abab                     | [abab]
             9 | ab ef                    | abcdef                   | [ab]cd[ef]
            10 | a b                      | a b                      | [a] [b]
            11 | n a                      | Anna                     | [Anna]
            """)
    void everyOccurrence(int row, String search, String text, String expected) {
        assertEquals(expected, marked(search, text));
    }

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}''")
    @DisplayName("nothing is marked: a word longer than the text, the empty search, a search of whitespace only")
    @CsvSource(delimiter = '|', textBlock = """
             1 | z\u00FCrichsee           | Z\u00FCrich              | Z\u00FCrich
             2 | annas                    | Anna                     | Anna
             3 | ''                       | Anna                     | Anna
             4 | '   '                    | Anna                     | Anna
             5 | '\t \u00A0'              | Anna                     | Anna
             6 | \u0308                   | Anna                     | Anna
            """)
    void nothingMarked(int row, String search, String text, String expected) {
        assertEquals(expected, marked(search, text));
    }

    @Test
    @DisplayName("a mark gives the positions of the original text, whatever folding did to its length")
    void positionsInTheOriginalText() {
        assertEquals(List.of(new Mark(0, 2)), SearchQuery.of("zu").marks("Z\u00FCrich"));
        assertEquals(List.of(new Mark(0, 3)), SearchQuery.of("zu").marks("Zu\u0308rich"));
        assertEquals(List.of(new Mark(1, 8)), SearchQuery.of("zurich").marks("\u00FCZu\u0308rich"));
        // Capital I with dot above, a capital Deseret letter (two chars), s, e with grave, n.
        assertEquals(List.of(new Mark(1, 4), new Mark(5, 6)),
                SearchQuery.of("\uD801\uDC28s n").marks("\u0130\uD801\uDC00s\u00E8n"));
        assertEquals(List.of(new Mark(0, 1)), SearchQuery.of("i").marks("\u0130"));
        assertEquals(List.of(new Mark(1, 2)), SearchQuery.of("\uAD6D").marks("\uD55C\uAD6D"));
    }

    @Test
    @DisplayName("an empty text, or none, has no marks")
    void noText() {
        assertEquals(List.of(), SearchQuery.of("anna").marks(""));
        assertEquals(List.of(), SearchQuery.of("anna").marks(null));
        assertEquals(List.of(), SearchQuery.of("").marks(null));
    }

    @Test
    @DisplayName("the marks are an unmodifiable list")
    void marksAreUnmodifiable() {
        List<Mark> marks = SearchQuery.of("an").marks("Anna Hanna");

        assertThrows(UnsupportedOperationException.class, () -> marks.add(new Mark(0, 1)));
    }

    // --- Whether an entry matches --------------------------------------------------------------------

    @ParameterizedTest(name = "#{0}: ''{1}'' in ''{2}'' -> {3}")
    @DisplayName("an entry matches when every word occurs, as a contiguous part, in at least one of its texts")
    @CsvSource(delimiter = '|', textBlock = """
             1 | anna z\u00FCrich         | Anna Muster ; Bahnhofstrasse 1, 8001 Z\u00FCrich | true
             2 | anna zurich              | Anna Muster ; Bahnhofstrasse 1, 8001 Z\u00FCrich | true
             3 | ANNA Z\u00DCRICH         | Anna Muster ; Bahnhofstrasse 1, 8001 Zurich      | true
             4 | muster bahnhof           | Anna Muster ; Bahnhofstrasse 1, 8001 Z\u00FCrich | true
             5 | anna bern                | Anna Muster ; Bahnhofstrasse 1, 8001 Z\u00FCrich | false
             6 | musterbahnhof            | Anna Muster ; Bahnhofstrasse 1, 8001 Z\u00FCrich | false
             7 | rbahn                    | Anna Muster ; Bahnhofstrasse                     | false
             8 | anna                     | Anna                                             | true
             9 | nna                      | Anna                                             | true
            10 | annas                    | Anna                                             | false
            11 | anna anna                | Anna                                             | true
            12 | file                     | TEST-FILE                                        | true
            13 | strasse                  | STRASSE                                          | true
            14 | ss                       | Stra\u00DFe                                      | false
            15 | \u00DF                   | Strasse                                          | false
            16 | ''                       | Anna                                             | true
            17 | '  '                     | Anna                                             | true
            """)
    void entryMatches(int row, String search, String cell, boolean expected) {
        assertEquals(expected, SearchQuery.of(search).matches(texts(cell)));
    }

    @Test
    @DisplayName("one text matches as an entry with that text alone")
    void oneText() {
        assertTrue(SearchQuery.of("anna zurich").matches("Anna Muster, Z\u00FCrich"));
        assertFalse(SearchQuery.of("anna bern").matches("Anna Muster, Z\u00FCrich"));
        assertTrue(SearchQuery.of("file").matches("TEST-FILE"));
        assertFalse(SearchQuery.of("file").matches(""));
        assertFalse(SearchQuery.of("file").matches((String) null));
        assertTrue(SearchQuery.of("").matches((String) null));
    }

    @Test
    @DisplayName("an entry without texts matches only the empty search; a missing text counts as empty")
    void noTexts() {
        assertTrue(SearchQuery.of("").matches(List.of()));
        assertFalse(SearchQuery.of("anna").matches(List.of()));
        assertTrue(SearchQuery.of("anna").matches(Arrays.asList(null, "Anna")));
        assertFalse(SearchQuery.of("anna").matches(Arrays.asList("Muster", null)));
    }

    @Test
    @DisplayName("the empty search: empty text, whitespace, none, or words that fold to nothing")
    void emptySearch() {
        assertTrue(SearchQuery.of("").isEmpty());
        assertTrue(SearchQuery.of(" \t\n\u00A0").isEmpty());
        assertTrue(SearchQuery.of(null).isEmpty());
        assertTrue(SearchQuery.of("\u0308 \u0301").isEmpty());
        assertFalse(SearchQuery.of(" a ").isEmpty());

        assertTrue(SearchQuery.of(" \n ").matches("Anna"));
        assertEquals(List.of(), SearchQuery.of(" \n ").marks("Anna"));
        assertEquals("[Anna]", marked("anna \u0308", "Anna"));
    }

    // --- The predicate of a list filter --------------------------------------------------------------

    /** An entry of a list, with the text the list shows. */
    private record Workflow(String name) {
    }

    @Test
    @DisplayName("the predicate keeps the entries whose text matches, every entry for the empty search")
    void predicate() {
        List<Workflow> workflows = List.of(new Workflow("TEST-FILE"), new Workflow("Z\u00FCrich report"),
                new Workflow("Daily import"), new Workflow(null));
        Function<Workflow, String> name = Workflow::name;

        assertEquals(List.of(workflows.get(0)), kept(workflows, SearchQuery.of("file").predicate(name)));
        assertEquals(List.of(workflows.get(1)), kept(workflows, SearchQuery.of("zurich rep").predicate(name)));
        assertEquals(List.of(workflows.get(1), workflows.get(2)),
                kept(workflows, SearchQuery.of("r").predicate(name)));
        assertEquals(List.of(), kept(workflows, SearchQuery.of("weekly").predicate(name)));
        assertEquals(workflows, kept(workflows, SearchQuery.of("  ").predicate(name)));
    }

    /** The entries a predicate keeps, in their order. */
    private static <T> List<T> kept(List<T> entries, Predicate<T> predicate) {
        List<T> kept = new ArrayList<>();
        for (T entry : entries) {
            if (predicate.test(entry)) {
                kept.add(entry);
            }
        }
        return kept;
    }

    // --- A mark --------------------------------------------------------------------------------------

    @Test
    @DisplayName("a mark covers at least one character from a position that is not negative")
    void markRange() {
        assertEquals(3, new Mark(3, 4).start());
        assertEquals(4, new Mark(3, 4).end());
        assertThrows(IllegalArgumentException.class, () -> new Mark(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> new Mark(2, 2));
        assertThrows(IllegalArgumentException.class, () -> new Mark(3, 2));
    }
}
