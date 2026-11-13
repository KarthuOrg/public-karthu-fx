package org.karthu.fx.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The order in which a table column sorts its cells' visible texts.
 */
class NaturalOrderTest {

    @Test
    @DisplayName("digit runs compare by their numeric value, so item2 comes before item10")
    void digitRunsByValue() {
        assertSorted("item1", "item2", "item10", "item20", "item100");
        assertSorted("2", "9", "10", "11", "100");
        assertSorted("v1.2", "v1.10", "v2.1");
        assertSorted("a2b3", "a2b20", "a10b1");
    }

    @Test
    @DisplayName("letters compare without their case, and texts equal but for case in a fixed order, upper case first")
    void caseIgnored() {
        assertSorted("alpha", "Beta", "gamma", "ZETA");
        assertSorted("Item2", "item10", "ITEM11");
        assertSorted("ABC", "Abc", "abc", "abd");
        assertSorted("csv", "CSV reader", "Csv Writer");
    }

    @Test
    @DisplayName("leading zeros do not change a number's value; texts of equal value in the order of the texts")
    void leadingZeros() {
        assertSorted("item007", "item07", "item7", "item8", "item010");
        assertSorted("0", "00", "01", "1", "2");
        assertSorted("v007a", "v7a", "v7b");
    }

    @Test
    @DisplayName("an empty cell comes first, and a text before every longer text it starts")
    void emptyCells() {
        assertSorted("", " ", "0", "a", "a ", "a b");
        assertSorted("", "item", "item1", "item1 ");
    }

    @Test
    @DisplayName("a number sorts after punctuation and spaces and before letters, by its first digit's place")
    void mixedTextAndNumbers() {
        assertSorted("-1", "1", "1-", "1a", "1b", "a1");
        assertSorted("a 2", "a2", "a10", "a_1", "ab");
        assertSorted("(3)", "3 rows", "30 rows", "rows: 3");
    }

    @Test
    @DisplayName("digit runs longer than any number type still compare by value")
    void longNumbers() {
        assertSorted("9", "99999999999999999999", "100000000000000000000", "0100000000000000000001");
    }

    @Test
    @DisplayName("the order is total: antisymmetric, transitive, and 0 only for equal texts")
    void totalOrder() {
        List<String> texts = List.of("", "0", "00", "007", "7", "07", "a", "A", "a0", "a00", "a7", "A07", "a07",
                "item2", "Item2", "item02", "ITEM02", "item10", "-", "-1", "1-", "x y", "x  y", "xy", "x1y", "x01y",
                "\u00E4", "\u00C4", "\u00DF", "SS", "ss", "\u0130", "i", "I", "\u0131", "_", "z", "Z", "9z", "z9");
        for (String a : texts) {
            for (String b : texts) {
                int ab = NaturalOrder.INSTANCE.compare(a, b);
                int ba = NaturalOrder.INSTANCE.compare(b, a);
                assertEquals(Integer.signum(ab), -Integer.signum(ba), () -> "antisymmetric: '" + a + "', '" + b + "'");
                assertEquals(a.equals(b), ab == 0, () -> "0 only for equal texts: '" + a + "', '" + b + "'");
                for (String c : texts) {
                    if (ab < 0 && NaturalOrder.INSTANCE.compare(b, c) < 0) {
                        assertTrue(NaturalOrder.INSTANCE.compare(a, c) < 0,
                                () -> "transitive: '" + a + "' < '" + b + "' < '" + c + "'");
                    }
                }
            }
        }
    }

    /**
     * Asserts that the natural order sorts the given texts, shuffled, into the given order.
     */
    private static void assertSorted(String... expected) {
        List<String> shuffled = new ArrayList<>(List.of(expected).reversed());
        shuffled.add(shuffled.removeFirst());
        shuffled.sort(NaturalOrder.INSTANCE);
        assertEquals(List.of(expected), shuffled);
    }
}
