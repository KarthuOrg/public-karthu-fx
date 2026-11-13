package org.karthu.fx.form.cron;

/**
 * The Unix macros the rules take, each with the expression it stands for. {@code @reboot} is none: it is no schedule.
 */
public enum CronMacro {

    /** {@code @yearly}: {@code 0 0 1 1 *}. */
    YEARLY("@yearly", "0 0 1 1 *"),

    /** {@code @annually}: {@code 0 0 1 1 *}, the same as {@code @yearly}. */
    ANNUALLY("@annually", "0 0 1 1 *"),

    /** {@code @monthly}: {@code 0 0 1 * *}. */
    MONTHLY("@monthly", "0 0 1 * *"),

    /** {@code @weekly}: {@code 0 0 * * 0}. */
    WEEKLY("@weekly", "0 0 * * 0"),

    /** {@code @daily}: {@code 0 0 * * *}. */
    DAILY("@daily", "0 0 * * *"),

    /** {@code @midnight}: {@code 0 0 * * *}, the same as {@code @daily}. */
    MIDNIGHT("@midnight", "0 0 * * *"),

    /** {@code @hourly}: {@code 0 * * * *}. */
    HOURLY("@hourly", "0 * * * *");

    private final String word;
    private final String expansion;

    CronMacro(String word, String expansion) {
        this.word = word;
        this.expansion = expansion;
    }

    /**
     * Returns the macro as written in lower case, {@code @daily}.
     *
     * @return the macro's word
     */
    public String word() {
        return word;
    }

    /**
     * Returns the Unix expression the macro stands for, {@code 0 0 * * *} for {@code @daily}.
     *
     * @return the expression, five fields
     */
    public String expansion() {
        return expansion;
    }
}
