package org.karthu.fx.markdown;

import java.util.Comparator;

/**
 * The order in which a table column sorts its cells: the cells' visible texts in natural order, as a reader expects
 * numbers in text to sort.
 *
 * <p>A text is read as a sequence of digit runs (the ASCII digits {@code 0} to {@code 9}) and single other characters.
 * Two texts compare by these, one after the other: two digit runs by their numeric value, of any length, so
 * {@code item2} comes before {@code item10} and leading zeros do not count; two other characters without their case;
 * a digit run and another character as the run's first digit and that character, so a number sorts after spaces and
 * punctuation and before letters. A text that another one starts comes first, the empty text before all.
 *
 * <p>Texts that this leaves equal (differing only in case or in leading zeros) are ordered by the texts themselves,
 * as {@link String#compareTo(String)} does ({@code ABC} before {@code abc}, {@code 007} before {@code 7}), so the
 * order is total: 0 only for equal texts. It is transitive too: no character other than a digit lies between
 * {@code 0} and {@code 9}, also after case folding, so every number has the same place among the other characters.
 */
final class NaturalOrder implements Comparator<String> {

    /**
     * The order; it holds no state.
     */
    static final NaturalOrder INSTANCE = new NaturalOrder();

    private NaturalOrder() {
    }

    @Override
    public int compare(String first, String second) {
        int natural = compareNatural(first, second);
        return natural != 0 ? natural : first.compareTo(second);
    }

    /**
     * Compares two texts by their digit runs and their other characters, see the class comment; 0 for texts that
     * differ only in case or in leading zeros.
     */
    private static int compareNatural(String first, String second) {
        int i = 0;
        int j = 0;
        while (i < first.length() && j < second.length()) {
            char a = first.charAt(i);
            char b = second.charAt(j);
            if (isDigit(a) && isDigit(b)) {
                int endA = digitRunEnd(first, i);
                int endB = digitRunEnd(second, j);
                int byValue = compareNumbers(first, i, endA, second, j, endB);
                if (byValue != 0) {
                    return byValue;
                }
                i = endA;
                j = endB;
            } else {
                int byCharacter = Character.compare(fold(a), fold(b));
                if (byCharacter != 0) {
                    return byCharacter;
                }
                i++;
                j++;
            }
        }
        return Integer.compare(first.length() - i, second.length() - j);
    }

    /**
     * Compares two digit runs by their value: without leading zeros, the longer run is the larger number, and runs
     * of equal length compare digit by digit.
     */
    private static int compareNumbers(String first, int startA, int endA, String second, int startB, int endB) {
        int a = skipZeros(first, startA, endA);
        int b = skipZeros(second, startB, endB);
        int byLength = Integer.compare(endA - a, endB - b);
        if (byLength != 0) {
            return byLength;
        }
        while (a < endA) {
            int byDigit = Character.compare(first.charAt(a), second.charAt(b));
            if (byDigit != 0) {
                return byDigit;
            }
            a++;
            b++;
        }
        return 0;
    }

    private static int skipZeros(String text, int start, int end) {
        int index = start;
        while (index < end && text.charAt(index) == '0') {
            index++;
        }
        return index;
    }

    private static int digitRunEnd(String text, int start) {
        int end = start;
        while (end < text.length() && isDigit(text.charAt(end))) {
            end++;
        }
        return end;
    }

    /**
     * A character without its case, as {@link String#CASE_INSENSITIVE_ORDER} folds it. A letter folds to a letter,
     * so no character folds into the digits.
     */
    private static char fold(char c) {
        return Character.toLowerCase(Character.toUpperCase(c));
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
