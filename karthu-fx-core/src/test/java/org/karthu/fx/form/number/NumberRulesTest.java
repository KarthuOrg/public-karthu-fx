package org.karthu.fx.form.number;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The number rules without JavaFX: which text is a number and which number it is. The expected numbers are built from
 * their unscaled value and scale, so that a test never reads a text the way the rules do; {@code BigDecimal.equals}
 * compares the scale too, so {@code 1.50} is not {@code 1.5}.
 */
class NumberRulesTest {

    // --- Decimal numbers -----------------------------------------------------------------------------

    @Test
    @DisplayName("digits with an optional sign are a number")
    void signedIntegers() {
        assertEquals(Optional.of(BigDecimal.valueOf(42, 0)), NumberRules.parse("42", false));
        assertEquals(Optional.of(BigDecimal.valueOf(-7, 0)), NumberRules.parse("-7", false));
        assertEquals(Optional.of(BigDecimal.valueOf(3, 0)), NumberRules.parse("+3", false));
        assertEquals(Optional.of(BigDecimal.valueOf(0, 0)), NumberRules.parse("-0", false));
        assertEquals(Optional.of(BigDecimal.valueOf(7, 0)), NumberRules.parse("007", false));
    }

    @Test
    @DisplayName("a dot is the decimal separator, and the number keeps the decimals as written")
    void dotIsTheDecimalSeparator() {
        assertEquals(Optional.of(BigDecimal.valueOf(15, 1)), NumberRules.parse("1.5", false));
        assertEquals(Optional.of(BigDecimal.valueOf(-25, 2)), NumberRules.parse("-0.25", false));
        assertEquals(Optional.of(BigDecimal.valueOf(150, 2)), NumberRules.parse("1.50", false));
        assertEquals(Optional.of(BigDecimal.valueOf(5, 1)), NumberRules.parse(".5", false));
        assertEquals(Optional.of(BigDecimal.valueOf(5, 0)), NumberRules.parse("5.", false));
    }

    @Test
    @DisplayName("whitespace around the number is ignored, whitespace inside it is no number")
    void surroundingWhitespaceIsIgnored() {
        assertEquals(Optional.of(BigDecimal.valueOf(12, 0)), NumberRules.parse("  12 ", false));
        assertEquals(Optional.of(BigDecimal.valueOf(35, 1)), NumberRules.parse("\t3.5\n", false));
        assertEquals(Optional.empty(), NumberRules.parse("1 000", false));
        assertEquals(Optional.empty(), NumberRules.parse("- 1", false));
    }

    @Test
    @DisplayName("no grouping and no comma: 1,5 and 1,000 and the like are no number")
    void noGroupingAndNoComma() {
        for (String text : List.of("1,5", "1,000", "1'000", "1_000", "1.000,5", "1,000.5")) {
            assertEquals(Optional.empty(), NumberRules.parse(text, false), text);
        }
    }

    @Test
    @DisplayName("an exponent is read as BigDecimal reads it, upper or lower case, with or without a sign")
    void exponent() {
        assertEquals(Optional.of(BigDecimal.valueOf(1, -3)), NumberRules.parse("1e3", false));
        assertEquals(Optional.of(BigDecimal.valueOf(1, -2)), NumberRules.parse("1E+2", false));
        assertEquals(Optional.of(BigDecimal.valueOf(25, 3)), NumberRules.parse("2.5E-2", false));
        assertEquals(Optional.of(BigDecimal.valueOf(10, -20)), NumberRules.parse("1.0E21", false));
        assertEquals(Optional.of(BigDecimal.valueOf(-15, -1)), NumberRules.parse(" -1.5e2 ", false));
    }

    @Test
    @DisplayName("an exponent without digits, with a fraction or beyond what BigDecimal holds is no number")
    void invalidExponent() {
        for (String text : List.of("e3", "1e", "1e+", "1e3.5", "1e3e4", "1ee3", "1e99999999999", ".e3")) {
            assertEquals(Optional.empty(), NumberRules.parse(text, false), text);
        }
    }

    @Test
    @DisplayName("every other text is no number: empty, blank, signs alone, letters, two dots, two signs, hex")
    void invalidTexts() {
        assertEquals(Optional.empty(), NumberRules.parse(null, false));
        for (String text : List.of("", "   ", "-", "+", ".", "-.", "abc", "12a", "a12", "1.2.3", "--1", "+-1", "1-",
                "0x1F", "NaN", "Infinity", "-Infinity", "1/2", "1d", "1f", "1L", "\u20AC5", "5%")) {
            assertEquals(Optional.empty(), NumberRules.parse(text, false), text);
        }
    }

    @Test
    @DisplayName("only ASCII digits: other digits and other spaces, which BigDecimal would partly read, are no number")
    void asciiDigitsOnly() {
        // Arabic-Indic one and two, full-width one, Devanagari one; a no-break space inside and around.
        for (String text : List.of("\u0661\u0662", "\uFF11", "\u0967", "1\u00A0000", "\u00A012")) {
            assertEquals(Optional.empty(), NumberRules.parse(text, false), text);
        }
    }

    // --- Whole numbers -------------------------------------------------------------------------------

    @Test
    @DisplayName("whole numbers: an optional sign and digits are a number, whitespace around them ignored")
    void wholeNumbers() {
        assertEquals(Optional.of(BigDecimal.valueOf(42, 0)), NumberRules.parse("42", true));
        assertEquals(Optional.of(BigDecimal.valueOf(-42, 0)), NumberRules.parse("-42", true));
        assertEquals(Optional.of(BigDecimal.valueOf(42, 0)), NumberRules.parse("+42", true));
        assertEquals(Optional.of(BigDecimal.valueOf(7, 0)), NumberRules.parse("007", true));
        assertEquals(Optional.of(BigDecimal.valueOf(12, 0)), NumberRules.parse(" 12\t", true));
    }

    @Test
    @DisplayName("whole numbers: a whole number can be longer than a long")
    void wholeNumbersBeyondLong() {
        Optional<BigDecimal> parsed = NumberRules.parse("123456789012345678901234567890", true);

        assertTrue(parsed.isPresent());
        assertEquals(0, parsed.get().scale());
        assertEquals("123456789012345678901234567890", parsed.get().toPlainString());
    }

    @Test
    @DisplayName("whole numbers: a dot or an exponent is no whole number, even where the value is whole")
    void wholeNumbersRefuseDotAndExponent() {
        for (String text : List.of("1.0", "1.", ".5", "1.5", "1e3", "1E+2", "10e-1")) {
            assertEquals(Optional.empty(), NumberRules.parse(text, true), text);
        }
    }

    @Test
    @DisplayName("whole numbers: every invalid text is no number either")
    void wholeNumbersInvalidTexts() {
        assertEquals(Optional.empty(), NumberRules.parse(null, true));
        for (String text : List.of("", "  ", "-", "+", "abc", "12a", "1 000", "1,000", "--1", "\u0661\u0662")) {
            assertEquals(Optional.empty(), NumberRules.parse(text, true), text);
        }
    }

    // --- Steps ---------------------------------------------------------------------------------------
    // The expected texts are written out by hand, never computed the way the rules compute them.

    private static final BigDecimal ONE = BigDecimal.valueOf(1, 0);

    /** The rule of a field for decimal numbers, the case of most steps here. */
    private static StepRule rule(BigDecimal min, BigDecimal max, BigDecimal step) {
        return new StepRule(false, min, max, step);
    }

    @Test
    @DisplayName("a step adds or subtracts the step, without a range")
    void stepAddsOrSubtractsTheStep() {
        assertEquals(Optional.of("6"), NumberRules.stepUp("5", rule(null, null, ONE)));
        assertEquals(Optional.of("4"), NumberRules.stepDown("5", rule(null, null, ONE)));
        assertEquals(Optional.of("-1"), NumberRules.stepDown("0", rule(null, null, ONE)));
        assertEquals(Optional.of("17"), NumberRules.stepUp("12", rule(null, null, BigDecimal.valueOf(5, 0))));
        assertEquals(Optional.of("13"), NumberRules.stepUp(" 12 ", new StepRule(true, null, null, ONE)));
    }

    @Test
    @DisplayName("the result is written without exponent, with as many decimals as the value or the step has")
    void resultDecimals() {
        assertEquals(Optional.of("2.50"), NumberRules.stepUp("1.50", rule(null, null, ONE)));
        assertEquals(Optional.of("1.25"), NumberRules.stepUp("1", rule(null, null, BigDecimal.valueOf(25, 2))));
        assertEquals(Optional.of("1.75"), NumberRules.stepUp("1.5", rule(null, null, BigDecimal.valueOf(25, 2))));
        assertEquals(Optional.of("0.0"), NumberRules.stepDown("0.5", rule(null, null, BigDecimal.valueOf(5, 1))));
        assertEquals(Optional.of("1001"), NumberRules.stepUp("1e3", rule(null, null, ONE)));
        assertEquals(Optional.of("1000000000000000000001"), NumberRules.stepUp("1.0E21", rule(null, null, ONE)));
        assertEquals(Optional.of("0.026"), NumberRules.stepUp("2.5E-2", rule(null, null, BigDecimal.valueOf(1, 3))));
        assertEquals(Optional.of("1100"), NumberRules.stepUp("1000", rule(null, null, BigDecimal.valueOf(1, -2))));
    }

    private static final BigDecimal ZERO = BigDecimal.valueOf(0, 0);
    private static final BigDecimal TEN = BigDecimal.valueOf(10, 0);

    @Test
    @DisplayName("a step from inside the range stops at the bound, and stays there")
    void stepFromInsideStopsAtTheBound() {
        BigDecimal three = BigDecimal.valueOf(3, 0);
        assertEquals(Optional.of("10"), NumberRules.stepUp("9", rule(ZERO, TEN, three)));
        assertEquals(Optional.of("10"), NumberRules.stepUp("10", rule(ZERO, TEN, three)));
        assertEquals(Optional.of("7"), NumberRules.stepDown("10", rule(ZERO, TEN, three)));
        assertEquals(Optional.of("0"), NumberRules.stepDown("2", rule(ZERO, TEN, three)));
        assertEquals(Optional.of("0"), NumberRules.stepDown("0", rule(ZERO, TEN, three)));
        // One bound alone.
        assertEquals(Optional.of("10"), NumberRules.stepUp("9", rule(null, TEN, three)));
        assertEquals(Optional.of("-20"), NumberRules.stepDown("9", rule(null, TEN, BigDecimal.valueOf(29, 0))));
        assertEquals(Optional.of("0"), NumberRules.stepDown("2", rule(ZERO, null, three)));
        assertEquals(Optional.of("102"), NumberRules.stepUp("99", rule(ZERO, null, three)));
        // A range of one value.
        assertEquals(Optional.of("10"), NumberRules.stepUp("10", rule(TEN, TEN, ONE)));
        assertEquals(Optional.of("10"), NumberRules.stepDown("10", rule(TEN, TEN, ONE)));
    }

    @Test
    @DisplayName("a bound the step stops at is written with its own decimals, and those of the value and the step")
    void boundKeepsItsDecimals() {
        BigDecimal max = BigDecimal.valueOf(125, 3);
        assertEquals(Optional.of("0.125"), NumberRules.stepUp("0.1", rule(null, max, BigDecimal.valueOf(1, 1))));
        assertEquals(Optional.of("100.0"), NumberRules.stepUp("99.5", rule(null, BigDecimal.valueOf(100, 0), ONE)));
        assertEquals(Optional.of("0.00"), NumberRules.stepDown("1", rule(ZERO, null, BigDecimal.valueOf(150, 2))));
        assertEquals(Optional.of("100"), NumberRules.stepUp("99", rule(null, BigDecimal.valueOf(1, -2), ONE)));
    }

    @Test
    @DisplayName("a step from outside the range lands on the nearer bound, up or down")
    void stepFromOutsideLandsOnTheNearerBound() {
        assertEquals(Optional.of("10"), NumberRules.stepUp("250", rule(ZERO, TEN, ONE)));
        assertEquals(Optional.of("10"), NumberRules.stepDown("250", rule(ZERO, TEN, ONE)));
        assertEquals(Optional.of("0"), NumberRules.stepUp("-4", rule(ZERO, TEN, ONE)));
        assertEquals(Optional.of("0"), NumberRules.stepDown("-4", rule(ZERO, TEN, ONE)));
        assertEquals(Optional.of("0.0"), NumberRules.stepUp("-4.5", rule(ZERO, null, ONE)));
        assertEquals(Optional.of("10"), NumberRules.stepDown("11", rule(null, TEN, ONE)));
    }

    @Test
    @DisplayName("on an empty or invalid text either arrow writes the start value 0, when the range holds it")
    void startValueIsZero() {
        for (String text : Arrays.asList(null, "", "  ", "12a", "1,5", "-")) {
            assertEquals(Optional.of("0"), NumberRules.stepUp(text, rule(null, null, ONE)), text);
            assertEquals(Optional.of("0"), NumberRules.stepDown(text, rule(null, null, ONE)), text);
            assertEquals(Optional.of("0"), NumberRules.stepUp(text, rule(BigDecimal.valueOf(-5, 0), TEN, ONE)), text);
        }
        // Not a whole number while whole numbers are on: as invalid as any other text.
        assertEquals(Optional.of("0"), NumberRules.stepUp("2.5", new StepRule(true, null, null, ONE)));
        // Bounds that are 0 hold it.
        assertEquals(Optional.of("0"), NumberRules.stepDown("", rule(ZERO, TEN, ONE)));
        assertEquals(Optional.of("0"), NumberRules.stepUp("", rule(BigDecimal.valueOf(-5, 0), ZERO, ONE)));
    }

    @Test
    @DisplayName("the start value is 0 forced into the range: the minimum above 0, the maximum below it")
    void startValueForcedIntoTheRange() {
        BigDecimal minAboveZero = BigDecimal.valueOf(15, 1);
        BigDecimal maxBelowZero = BigDecimal.valueOf(-3, 0);
        assertEquals(Optional.of("1.5"), NumberRules.stepUp("", rule(minAboveZero, null, ONE)));
        assertEquals(Optional.of("1.5"), NumberRules.stepDown("abc", rule(minAboveZero, TEN, ONE)));
        assertEquals(Optional.of("-3"), NumberRules.stepUp("", rule(null, maxBelowZero, ONE)));
        assertEquals(Optional.of("-3"),
                NumberRules.stepDown("abc", rule(BigDecimal.valueOf(-9, 0), maxBelowZero, ONE)));
    }

    @Test
    @DisplayName("the start value has as many decimals as the step, and the next step goes on from it")
    void startValueDecimalsAndTheNextStep() {
        BigDecimal quarter = BigDecimal.valueOf(25, 2);
        Optional<String> start = NumberRules.stepUp("", rule(null, null, quarter));
        assertEquals(Optional.of("0.00"), start);
        assertEquals(Optional.of("0.25"), NumberRules.stepUp(start.get(), rule(null, null, quarter)));
        assertEquals(Optional.of("1.50"), NumberRules.stepUp("", rule(BigDecimal.valueOf(15, 1), null, quarter)));
    }

    @Test
    @DisplayName("a null step counts as the default step 1")
    void nullStepCountsAsOne() {
        assertEquals(Optional.of("6"), NumberRules.stepUp("5", rule(null, null, null)));
        assertEquals(Optional.of("4.5"), NumberRules.stepDown("5.5", rule(null, null, null)));
        assertEquals(Optional.of("0"), NumberRules.stepUp("", rule(null, null, null)));
    }

    @Test
    @DisplayName("a step of zero or below changes nothing, not even an empty text")
    void stepOfZeroOrBelowChangesNothing() {
        for (BigDecimal step : List.of(ZERO, BigDecimal.valueOf(0, 2), BigDecimal.valueOf(-1, 0),
                BigDecimal.valueOf(-25, 2))) {
            assertEquals(Optional.empty(), NumberRules.stepUp("5", rule(null, null, step)), step.toString());
            assertEquals(Optional.empty(), NumberRules.stepDown("5", rule(null, null, step)), step.toString());
            assertEquals(Optional.empty(), NumberRules.stepUp("", rule(ZERO, TEN, step)), step.toString());
        }
    }

    @Test
    @DisplayName("a minimum above the maximum is an empty range: no step changes anything, not even an empty text")
    void minimumAboveMaximumChangesNothing() {
        for (String text : List.of("5", "-20", "20", "", "abc")) {
            assertEquals(Optional.empty(), NumberRules.stepUp(text, rule(TEN, ZERO, ONE)), text);
            assertEquals(Optional.empty(), NumberRules.stepDown(text, rule(TEN, ZERO, ONE)), text);
        }
    }

    @Test
    @DisplayName("a number too long to write out (more than 1000 digits) changes nothing, nor does a bound or step that is")
    void numbersTooLongToWriteOutChangeNothing() {
        BigDecimal huge = BigDecimal.valueOf(1, -999_999_999);
        BigDecimal tiny = BigDecimal.valueOf(1, 999_999_999);
        assertEquals(Optional.empty(), NumberRules.stepUp("1e999999999", rule(null, null, ONE)));
        assertEquals(Optional.empty(), NumberRules.stepDown("1e-999999999", rule(null, null, ONE)));
        assertEquals(Optional.empty(), NumberRules.stepUp("5", rule(null, null, tiny)));
        assertEquals(Optional.empty(), NumberRules.stepUp("5", rule(null, huge, ONE)));
        assertEquals(Optional.empty(), NumberRules.stepUp("", rule(tiny, null, ONE)));
        assertEquals(Optional.empty(), NumberRules.stepUp("5", rule(null, null, BigDecimal.valueOf(1, -1000))));
        // At the limit: 1000 digits written out, a double's smallest value (4.9E-324) far below it.
        assertEquals(Optional.of("1" + "0".repeat(998) + "1"),
                NumberRules.stepUp("1e999", rule(null, null, ONE)));
        assertEquals(Optional.of("1." + "0".repeat(323) + "49"),
                NumberRules.stepUp("4.9E-324", rule(null, null, ONE)));
    }
}
