package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

/**
 * The fields of a cron expression, in the order they are written: Quartz has all seven (the year optional), Unix the
 * five from minute to weekday. Each field has the name a message uses and its range of values.
 */
public enum CronFieldKind {

    /** The second, 0 to 59, Quartz only. */
    SECOND("Second", 0, 59),

    /** The minute, 0 to 59. */
    MINUTE("Minute", 0, 59),

    /** The hour, 0 to 23. */
    HOUR("Hour", 0, 23),

    /** The day of the month, 1 to 31. */
    DAY_OF_MONTH("Day of month", 1, 31),

    /** The month, 1 to 12, or {@code JAN} to {@code DEC}. */
    MONTH("Month", 1, 12),

    /**
     * The day of the week, or {@code SUN} to {@code SAT}: Unix 0 to 7 (0 and 7 Sunday), Quartz 1 to 7 (1 Sunday, 7
     * Saturday).
     */
    WEEKDAY("Weekday", 1, 7),

    /** The year, 1970 to 2099, Quartz only and optional. */
    YEAR("Year", 1970, 2099);

    private final String label;
    private final int lowest;
    private final int highest;

    CronFieldKind(String label, int lowest, int highest) {
        this.label = label;
        this.lowest = lowest;
        this.highest = highest;
    }

    /**
     * Returns the field's name at the start of a message: <em>Second</em>, <em>Minute</em>, <em>Hour</em>, <em>Day of
     * month</em>, <em>Month</em>, <em>Weekday</em> or <em>Year</em>.
     *
     * @return the field's name, capitalised
     */
    public String label() {
        return label;
    }

    /**
     * Returns the lowest value of the field in a format: 0 for the Unix weekday (Sunday), else the same in both.
     *
     * @param format the format
     * @return the lowest value
     */
    public int lowest(CronFormat format) {
        if (this == WEEKDAY && format == CronFormat.UNIX) {
            return 0;
        }
        return lowest;
    }

    /**
     * Returns the highest value of the field, the same in both formats: 7 for the weekday is Sunday in Unix and
     * Saturday in Quartz.
     *
     * @return the highest value
     */
    public int highest() {
        return highest;
    }

    /**
     * Returns whether a step's start takes the whole field in a format: {@code *}, or the field's lowest value
     * ({@code 0} in the minute, so {@code 0/15} steps through the whole hour as {@code *}{@code /15} does).
     *
     * @param start  what the step goes through
     * @param format the format, which decides the weekday's lowest value
     * @return true when the step starts at the field's start and runs to its end
     */
    public boolean isWholeFieldStart(CronPart start, CronFormat format) {
        if (start instanceof CronPart.All) {
            return true;
        }
        return start instanceof CronPart.Value value && value.value() == lowest(format);
    }
}
