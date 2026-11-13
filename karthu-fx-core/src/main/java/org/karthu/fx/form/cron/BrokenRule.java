package org.karthu.fx.form.cron;

/**
 * The first broken rule found while checking an expression, carrying its message up to {@link CronRules#check}. No
 * stack trace: it is the parser's way out, not an error of the program.
 */
final class BrokenRule extends Exception {

    /**
     * Creates a broken rule.
     *
     * @param message the rule's message, as the input shows it
     */
    BrokenRule(String message) {
        super(message, null, false, false);
    }
}
