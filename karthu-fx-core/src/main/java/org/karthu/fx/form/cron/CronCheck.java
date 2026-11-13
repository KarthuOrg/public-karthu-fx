package org.karthu.fx.form.cron;

/**
 * What {@link CronRules#check} says of a text: a valid expression, parsed, or the message of the first broken rule.
 */
public sealed interface CronCheck permits CronCheck.Valid, CronCheck.Invalid {

    /**
     * The text is a valid expression of its format.
     *
     * @param expression the expression, parsed
     */
    record Valid(CronExpression expression) implements CronCheck {
    }

    /**
     * The text is no valid expression of its format.
     *
     * @param problem the message of the first broken rule: English, one sentence, no full stop
     */
    record Invalid(String problem) implements CronCheck {
    }
}
