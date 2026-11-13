package org.karthu.fx.form.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The suggestion filter without JavaFX: which suggestions a typed text keeps, in which order.
 */
class SuggestionFilterTest {

    private static final List<String> ENCODINGS = List.of("UTF-8", "UTF-16", "ISO-8859-1", "windows-1252", "US-ASCII");

    @Test
    @DisplayName("a suggestion that contains the text is kept, anywhere in it")
    void containsTheText() {
        assertEquals(List.of("UTF-8", "UTF-16"), SuggestionFilter.filter(ENCODINGS, "UTF"));
        assertEquals(List.of("UTF-16", "ISO-8859-1", "windows-1252"), SuggestionFilter.filter(ENCODINGS, "-1"));
        assertEquals(List.of("ISO-8859-1"), SuggestionFilter.filter(ENCODINGS, "8859"));
        assertEquals(List.of("US-ASCII"), SuggestionFilter.filter(ENCODINGS, "ASCII"));
        assertEquals(List.of("windows-1252"), SuggestionFilter.filter(ENCODINGS, "windows-1252"));
    }

    @Test
    @DisplayName("case is ignored, in the text and in the suggestions")
    void ignoresCase() {
        assertEquals(List.of("UTF-8", "UTF-16"), SuggestionFilter.filter(ENCODINGS, "utf"));
        assertEquals(List.of("windows-1252"), SuggestionFilter.filter(ENCODINGS, "WINDOWS"));
        assertEquals(List.of("US-ASCII"), SuggestionFilter.filter(ENCODINGS, "aScIi"));
        assertEquals(List.of("Ärger", "ähnlich"),
                SuggestionFilter.filter(List.of("Ärger", "ähnlich", "Arger"), "ä"));
    }

    @Test
    @DisplayName("the empty text keeps every suggestion, and so does null")
    void emptyTextKeepsAll() {
        assertEquals(ENCODINGS, SuggestionFilter.filter(ENCODINGS, ""));
        assertEquals(ENCODINGS, SuggestionFilter.filter(ENCODINGS, null));
    }

    @Test
    @DisplayName("a text no suggestion contains keeps none; nothing is ever completed or corrected")
    void noMatchKeepsNone() {
        assertEquals(List.of(), SuggestionFilter.filter(ENCODINGS, "UTF-32"));
        assertEquals(List.of(), SuggestionFilter.filter(ENCODINGS, "UTF8"));
        assertEquals(List.of(), SuggestionFilter.filter(ENCODINGS, " utf"));
        assertEquals(List.of(), SuggestionFilter.filter(List.of(), "UTF"));
    }

    @Test
    @DisplayName("whitespace counts as typed: it is neither trimmed nor ignored")
    void whitespaceCounts() {
        List<String> names = List.of("first name", "last name", "name");

        assertEquals(List.of("first name", "last name"), SuggestionFilter.filter(names, " name"));
        assertEquals(List.of("first name", "last name", "name"), SuggestionFilter.filter(names, "name"));
        assertEquals(List.of(), SuggestionFilter.filter(names, "name "));
    }

    @Test
    @DisplayName("the kept suggestions keep their order and their duplicates; null suggestions are never kept")
    void keepsOrderAndDuplicates() {
        List<String> suggestions = Arrays.asList("b-two", null, "a-two", "b-two", "c-one");

        assertEquals(List.of("b-two", "a-two", "b-two"), SuggestionFilter.filter(suggestions, "two"));
        assertEquals(List.of("b-two", "a-two", "b-two", "c-one"), SuggestionFilter.filter(suggestions, ""));
    }

    @Test
    @DisplayName("the result is a new list: changing it changes neither the suggestions nor a later result")
    void resultIsANewList() {
        List<String> suggestions = new ArrayList<>(ENCODINGS);

        List<String> kept = SuggestionFilter.filter(suggestions, "");
        kept.clear();

        assertEquals(ENCODINGS, suggestions);
        assertEquals(ENCODINGS, SuggestionFilter.filter(suggestions, ""));
    }

    @Test
    @DisplayName("letters are compared one by one: a letter whose capital is two letters matches only itself")
    void comparesLetterByLetter() {
        // The German sharp s upper-cases to SS in a whole text; letter by letter it stays one letter, so neither text
        // matches the other through such an expansion.
        assertEquals(List.of("Straße"), SuggestionFilter.filter(List.of("Straße", "Strasse"), "ß"));
        assertEquals(List.of("Strasse"), SuggestionFilter.filter(List.of("Straße", "Strasse"), "ss"));
        assertEquals(List.of("Strasse"), SuggestionFilter.filter(List.of("Straße", "Strasse"), "SS"));
    }
}
