package org.karthu.fx.form.cron;

/**
 * One entry of a field's comma-separated list, parsed. Values are numbers in the numbering of the expression's format,
 * names already turned into numbers: a month 1 to 12, a Unix weekday 0 to 7 (0 and 7 Sunday), a Quartz weekday 1 to 7
 * (1 Sunday, 7 Saturday).
 *
 * <p>{@link All}, {@link Value}, {@link Range} and {@link Step} occur in every field of both formats.
 * {@link NoSpecificValue} and the special forms are Quartz's: in the day of month {@link LastDayOfMonth},
 * {@link LastWorkdayOfMonth} and {@link NearestWorkday}, in the weekday {@link LastDayOfWeek},
 * {@link LastDayOfWeekInMonth} and {@link NthDayOfWeekInMonth}. {@code ?} and a special form are always the field's
 * only entry.
 */
public sealed interface CronPart permits CronPart.All, CronPart.NoSpecificValue, CronPart.Value, CronPart.Range,
        CronPart.Step, CronPart.LastDayOfMonth, CronPart.LastWorkdayOfMonth, CronPart.NearestWorkday,
        CronPart.LastDayOfWeek, CronPart.LastDayOfWeekInMonth, CronPart.NthDayOfWeekInMonth {

    /** {@code *}: every value of the field. */
    record All() implements CronPart {
    }

    /** {@code ?}: no specific value, Quartz's day of month or weekday while the other one is set. */
    record NoSpecificValue() implements CronPart {
    }

    /**
     * A single value, written as a number or a name.
     *
     * @param value the value
     */
    record Value(int value) implements CronPart {
    }

    /**
     * A range {@code a-b}, both ends included. In Quartz a range may wrap around the end of the field (every field but
     * the year): {@code 22-2} in the hour is 22, 23, 0, 1 and 2, {@code FRI-MON} in the weekday Friday to Monday. Unix
     * and the Quartz year refuse a range whose start is above its end.
     *
     * @param start the first value
     * @param end   the last value
     */
    record Range(int start, int end) implements CronPart {

        /**
         * Whether the range wraps around the end of the field, its start above its end (Quartz only).
         *
         * @return true when the start is above the end
         */
        public boolean wraps() {
            return start > end;
        }
    }

    /**
     * A step {@code x/n}: every n-th value, starting at the start of {@code x}. {@code x} is {@code *} (from the
     * field's lowest value to its highest), a value (from it to the field's highest value) or a range (through it).
     *
     * @param start what the step goes through: an {@link All}, a {@link Value} or a {@link Range}
     * @param step  the step, at least 1 and at most the field's highest value
     */
    record Step(CronPart start, int step) implements CronPart {
    }

    /**
     * Quartz's day of month {@code L} (the last day of the month) or {@code L-n} (n days before it).
     *
     * @param daysBefore how many days before the last day, 0 for {@code L}, at most 30
     */
    record LastDayOfMonth(int daysBefore) implements CronPart {
    }

    /** Quartz's day of month {@code LW}: the last working day (Monday to Friday) of the month. */
    record LastWorkdayOfMonth() implements CronPart {
    }

    /**
     * Quartz's day of month {@code nW}: the working day (Monday to Friday) nearest day n of the month, within the
     * month.
     *
     * @param dayOfMonth the day of the month, 1 to 31
     */
    record NearestWorkday(int dayOfMonth) implements CronPart {
    }

    /** Quartz's weekday {@code L} alone: the last day of the week, Saturday (7). */
    record LastDayOfWeek() implements CronPart {
    }

    /**
     * Quartz's weekday {@code nL}: the last weekday n of the month, {@code 6L} the last Friday.
     *
     * @param dayOfWeek the weekday, 1 to 7 (1 Sunday)
     */
    record LastDayOfWeekInMonth(int dayOfWeek) implements CronPart {
    }

    /**
     * Quartz's weekday {@code n#k}: the k-th weekday n of the month, {@code 6#3} the third Friday.
     *
     * @param dayOfWeek  the weekday, 1 to 7 (1 Sunday)
     * @param occurrence which one in the month, 1 to 5
     */
    record NthDayOfWeekInMonth(int dayOfWeek, int occurrence) implements CronPart {
    }
}
