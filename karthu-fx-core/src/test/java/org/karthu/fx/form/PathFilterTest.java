package org.karthu.fx.form;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The file filter record: what it keeps, and what it refuses where it is made rather than when a dialog opens.
 */
class PathFilterTest {

    @Test
    @DisplayName("a filter keeps its description and its patterns in their order, as a copy")
    void keepsDescriptionAndPatterns() {
        List<String> patterns = new ArrayList<>(List.of("*.txt", "*.tsv"));

        PathFilter filter = new PathFilter("Text files", patterns);
        patterns.add("*.csv");

        assertEquals("Text files", filter.description());
        assertEquals(List.of("*.txt", "*.tsv"), filter.patterns());
        assertThrows(UnsupportedOperationException.class, () -> filter.patterns().add("*.csv"));
    }

    @Test
    @DisplayName("a filter needs a description that is not empty")
    void refusesAMissingDescription() {
        assertThrows(NullPointerException.class, () -> new PathFilter(null, List.of("*.csv")));
        assertThrows(IllegalArgumentException.class, () -> new PathFilter("", List.of("*.csv")));
    }

    @Test
    @DisplayName("a filter needs at least one pattern, and no pattern may be null or empty")
    void refusesMissingPatterns() {
        List<String> withNull = new ArrayList<>();
        withNull.add(null);

        assertThrows(NullPointerException.class, () -> new PathFilter("CSV files", null));
        assertThrows(IllegalArgumentException.class, () -> new PathFilter("CSV files", List.of()));
        assertThrows(NullPointerException.class, () -> new PathFilter("CSV files", withNull));
        assertThrows(IllegalArgumentException.class, () -> new PathFilter("CSV files", List.of("*.csv", "")));
    }
}
