package org.karthu.fx.form.number;

import java.math.BigDecimal;

/**
 * What a step of the number input's arrows follows, the input's rules taken together at the moment of the step: how
 * its text is read, its range and its step. {@link NumberRules#stepUp(String, StepRule)} and
 * {@link NumberRules#stepDown(String, StepRule)} take it; the input builds one per step from its properties. Its
 * values are taken as they are, never corrected: {@link NumberRules#stepDown(String, StepRule)} says what a step does
 * where there is no rule to follow.
 *
 * @param wholeNumbers true when only whole numbers are numbers (for reading the text)
 * @param min          the range's minimum, or null for no lower bound
 * @param max          the range's maximum, or null for no upper bound
 * @param step         the step, null for the default step 1
 */
public record StepRule(boolean wholeNumbers, BigDecimal min, BigDecimal max, BigDecimal step) {
}
