package org.karthu.fx.form.number;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The rules of the number input, independent of JavaFX: which text is a number and which number it is, and which text
 * a step of the spinner's arrows writes ({@link #stepUp}, {@link #stepDown}, by a {@link StepRule}).
 *
 * <p>A number is written as it is stored: a dot as the decimal separator, no grouping, ASCII digits only, and in the
 * decimal case an optional exponent as {@link BigDecimal#BigDecimal(String)} reads it ({@code 1.0E21}, as
 * {@code Double.toString} writes it). Whitespace around the number is ignored, whitespace inside it makes the text no
 * number. Every other text is no number; nothing is rounded, corrected or completed.
 *
 * <p>This package is not exported: the rules are pinned down by their own tests, and the number
 * input is their only caller.
 */
public final class NumberRules {

    private NumberRules() {
    }

    /**
     * Returns the number a text is, or empty when it is none.
     *
     * <p>The text is stripped of the whitespace around it ({@link String#strip()}: what {@link Character#isWhitespace}
     * calls whitespace, so a no-break space is no whitespace). What remains must be:
     * <ul>
     *   <li>for whole numbers: an optional sign ({@code +} or {@code -}) and one or more digits, nothing else, so
     *       {@code 1.0} and {@code 1e3} are no whole numbers;</li>
     *   <li>else: a decimal number as {@link BigDecimal#BigDecimal(String)} reads it from ASCII characters, an optional
     *       sign, digits with an optional dot (at least one digit before or after it) and an optional exponent
     *       ({@code e} or {@code E}, an optional sign, digits). An exponent beyond what a {@code BigDecimal} holds is
     *       no number.</li>
     * </ul>
     * The number keeps its digits and decimals as written: {@code 1.50} is read with two decimals, {@code 007} as
     * {@code 7}.
     *
     * @param text         the text in the field, may be null
     * @param wholeNumbers true when only whole numbers are numbers
     * @return the number, or empty when the text is no number
     */
    public static Optional<BigDecimal> parse(String text, boolean wholeNumbers) {
        if (text == null) {
            return Optional.empty();
        }
        String number = text.strip();
        if (number.isEmpty()) {
            return Optional.empty();
        }
        boolean allowed = wholeNumbers ? isSignedDigits(number) : hasOnlyDecimalCharacters(number);
        if (!allowed) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(number));
        } catch (NumberFormatException noNumber) {
            // Characters in a wrong order ("1.2.3", "--1", "1e"), or an exponent beyond an int.
            return Optional.empty();
        }
    }

    /** Whether the text is an optional sign and one or more ASCII digits, nothing else. */
    private static boolean isSignedDigits(String text) {
        int start = text.charAt(0) == '+' || text.charAt(0) == '-' ? 1 : 0;
        if (start == text.length()) {
            return false;
        }
        for (int i = start; i < text.length(); i++) {
            if (!isAsciiDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the text holds only the ASCII characters of a decimal number: digits, signs, the dot and the exponent's
     * {@code e}. Their order is {@code BigDecimal}'s to check; this check keeps out what {@code BigDecimal} would also
     * read, digits of other scripts.
     */
    private static boolean hasOnlyDecimalCharacters(String text) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            boolean allowed = isAsciiDigit(character) || character == '+' || character == '-' || character == '.'
                    || character == 'e' || character == 'E';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }

    // --- Steps: the spinner's arrows and its Up and Down keys -----------------------------------------

    /**
     * The most digits a number the arrows step may have written out without exponent, before and after the decimal
     * point together. Far more than a {@code double} needs ({@code 4.9E-324} written out has 326), and few enough
     * that writing it out never stalls the UI: {@code 1e999999999} written out would have a billion digits.
     */
    private static final int MAX_WRITTEN_DIGITS = 1000;

    /**
     * Returns the text one step up gives: see {@link #stepDown(String, StepRule)} for the whole rule, the same in both
     * directions but for adding the step instead of subtracting it.
     *
     * @param text the text in the field, may be null
     * @param rule how the text is read, the range and the step
     * @return the text to write into the field, or empty when the step changes nothing
     */
    public static Optional<String> stepUp(String text, StepRule rule) {
        return step(text, rule, true);
    }

    /**
     * Returns the text one step down gives, the rule of the spinner's arrows and its Up and Down keys:
     * <ul>
     *   <li>A number inside the range (each bound optional) gets the step subtracted (one step up: added) with
     *       {@code BigDecimal} arithmetic. A result beyond a bound stops at it, so a step from inside never leaves the
     *       range, and at the bound it stays there.</li>
     *   <li>A number outside the range lands on the nearer bound, in either direction: below the minimum on the
     *       minimum, above the maximum on the maximum.</li>
     *   <li>An empty text or a text that is no number ({@link #parse(String, boolean)}, also a decimal while only
     *       whole numbers are numbers) gets the start value: 0 forced into the range, the minimum when 0 is below it,
     *       the maximum when 0 is above it. The next step goes on from there.</li>
     * </ul>
     * The text written is the result without exponent ({@link BigDecimal#toPlainString()}), with as many decimals as
     * the number in the field or the step has, or the bound it stopped at if that has more, never fewer than none:
     * {@code 1.50} up by 1 is {@code 2.50}, {@code 1} up by {@code 0.25} is {@code 1.25}, {@code 1e3} up by 1 is
     * {@code 1001}, the start value with the step {@code 0.25} is {@code 0.00}. Nothing is rounded.
     *
     * <p>Where there is no rule to follow, the step changes nothing (empty), not even an empty text:
     * <ul>
     *   <li>a step of zero or below (a null step counts as the default step 1): no step could move the number the
     *       arrow's way;</li>
     *   <li>a minimum above the maximum: the range is empty, so no value may be written;</li>
     *   <li>a number in the field, a bound or a step with more than 1000 digits written out
     *       ({@code 1e999999999}), which could not be written without exponent in a field.</li>
     * </ul>
     * Whole numbers decide only how the text is read: the step and the bounds are taken as they are, so a whole-number
     * field wants a whole step and whole bounds from its application.
     *
     * @param text the text in the field, may be null
     * @param rule how the text is read, the range and the step
     * @return the text to write into the field, or empty when the step changes nothing
     */
    public static Optional<String> stepDown(String text, StepRule rule) {
        return step(text, rule, false);
    }

    private static Optional<String> step(String text, StepRule rule, boolean up) {
        BigDecimal min = rule.min();
        BigDecimal max = rule.max();
        BigDecimal amount = rule.step() == null ? BigDecimal.ONE : rule.step();
        if (amount.signum() <= 0 || (min != null && max != null && min.compareTo(max) > 0)) {
            return Optional.empty();
        }
        if (tooLongToWrite(amount) || tooLongToWrite(min) || tooLongToWrite(max)) {
            return Optional.empty();
        }
        Optional<BigDecimal> number = parse(text, rule.wholeNumbers());
        if (number.isEmpty()) {
            BigDecimal start = startValue(min, max);
            return Optional.of(written(start, start, amount));
        }
        BigDecimal value = number.get();
        // Checked before any arithmetic: adding 1 to 1e999999999 would build a number of a billion digits.
        if (tooLongToWrite(value)) {
            return Optional.empty();
        }
        BigDecimal result;
        if (min != null && value.compareTo(min) < 0) {
            result = min;
        } else if (max != null && value.compareTo(max) > 0) {
            result = max;
        } else {
            result = up ? value.add(amount) : value.subtract(amount);
            if (max != null && result.compareTo(max) > 0) {
                result = max;
            } else if (min != null && result.compareTo(min) < 0) {
                result = min;
            }
        }
        return Optional.of(written(result, value, amount));
    }

    /** The start value on an empty or invalid text: 0 forced into the range (which is not empty here). */
    private static BigDecimal startValue(BigDecimal min, BigDecimal max) {
        if (min != null && min.signum() > 0) {
            return min;
        }
        if (max != null && max.signum() < 0) {
            return max;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Whether a number has more than {@link #MAX_WRITTEN_DIGITS} digits written out without exponent: its integer
     * digits (at least the one 0) and its decimals. Computed from precision and scale, never by writing it out.
     */
    private static boolean tooLongToWrite(BigDecimal number) {
        if (number == null) {
            return false;
        }
        long integerDigits = Math.max(1L, (long) number.precision() - number.scale());
        long decimals = Math.max(0L, number.scale());
        return integerDigits + decimals > MAX_WRITTEN_DIGITS;
    }

    /**
     * The result written out: without exponent, with the most decimals of the result, the number stepped from and
     * the step, at least none. Raising the scale only appends zeros, so nothing is rounded.
     */
    private static String written(BigDecimal result, BigDecimal value, BigDecimal step) {
        int scale = Math.max(0, Math.max(result.scale(), Math.max(value.scale(), step.scale())));
        return result.setScale(scale).toPlainString();
    }
}
