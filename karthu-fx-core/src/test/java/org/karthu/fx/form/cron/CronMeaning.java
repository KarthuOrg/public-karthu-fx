package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

import java.util.BitSet;

/**
 * What a valid cron expression means, read by the tests on their own from its parsed form, the oracle for "the same
 * schedule": the seconds, minutes, hours, months and years it takes, and the days it runs on, each day being a day of
 * the month together with a day of the week (an index {@code (dayOfMonth - 1) * 7 + dayOfWeek}, Sunday 0). Two
 * expressions, also of different formats, mean the same schedule when their meanings are equal.
 *
 * <p>Unix has second 0 and every year. Its day is read as cronie reads it: when the day of month or the weekday starts
 * with {@code *}, a day must match both, else either. Quartz's day is the one field that is not {@code ?}. Quartz's
 * special forms ({@code L}, {@code W}, {@code #}) are not read: no conversion takes them.
 *
 * @param seconds the seconds, 0 to 59
 * @param minutes the minutes, 0 to 59
 * @param hours   the hours, 0 to 23
 * @param months  the months, 1 to 12
 * @param years   the years, 1970 to 2099
 * @param days    the days it runs on
 */
record CronMeaning(BitSet seconds, BitSet minutes, BitSet hours, BitSet months, BitSet years, BitSet days) {

    /**
     * Reads what an expression means.
     *
     * @param expression a valid expression without Quartz's special forms
     * @return its meaning
     */
    static CronMeaning of(CronExpression expression) {
        CronFormat format = expression.format();
        BitSet seconds = new BitSet();
        seconds.set(0);
        if (expression.second().isPresent()) {
            seconds = values(expression.second().get(), format);
        }
        BitSet years = new BitSet();
        years.set(1970, 2100);
        if (expression.year().isPresent()) {
            years = values(expression.year().get(), format);
        }
        return new CronMeaning(seconds, values(expression.minute(), format), values(expression.hour(), format),
                values(expression.month(), format), years, days(expression));
    }

    private static BitSet days(CronExpression expression) {
        CronFormat format = expression.format();
        CronField dayOfMonth = expression.dayOfMonth();
        CronField weekday = expression.weekday();
        BitSet daysOfMonth = values(dayOfMonth, format);
        BitSet daysOfWeek = new BitSet();
        BitSet weekdays = values(weekday, format);
        for (int value = weekdays.nextSetBit(0); value >= 0; value = weekdays.nextSetBit(value + 1)) {
            daysOfWeek.set(format == CronFormat.UNIX ? value % 7 : value - 1);
        }
        boolean both = dayOfMonth.text().startsWith("*") || weekday.text().startsWith("*");
        BitSet days = new BitSet();
        for (int day = 1; day <= 31; day++) {
            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                boolean byDayOfMonth = daysOfMonth.get(day);
                boolean byDayOfWeek = daysOfWeek.get(dayOfWeek);
                boolean runs;
                if (format == CronFormat.QUARTZ) {
                    runs = dayOfMonth.isNoSpecificValue() ? byDayOfWeek : byDayOfMonth;
                } else if (both) {
                    runs = byDayOfMonth && byDayOfWeek;
                } else {
                    runs = byDayOfMonth || byDayOfWeek;
                }
                if (runs) {
                    days.set((day - 1) * 7 + dayOfWeek);
                }
            }
        }
        return days;
    }

    /** The values a field takes, in the format's own numbering; {@code ?} takes every value. */
    private static BitSet values(CronField field, CronFormat format) {
        int lowest = field.kind().lowest(format);
        int highest = field.kind().highest();
        BitSet values = new BitSet();
        for (CronPart part : field.parts()) {
            if (part instanceof CronPart.All || part instanceof CronPart.NoSpecificValue) {
                walk(values, lowest, highest, lowest, highest, 1);
            } else if (part instanceof CronPart.Value value) {
                values.set(value.value());
            } else if (part instanceof CronPart.Range range) {
                walk(values, lowest, highest, range.start(), range.end(), 1);
            } else if (part instanceof CronPart.Step step) {
                int from = lowest;
                int to = highest;
                if (step.start() instanceof CronPart.Value value) {
                    from = value.value();
                } else if (step.start() instanceof CronPart.Range range) {
                    from = range.start();
                    to = range.end();
                }
                walk(values, lowest, highest, from, to, step.step());
            } else {
                throw new IllegalArgumentException("not read: " + part);
            }
        }
        return values;
    }

    /** Takes every n-th value from one value to another, round the field's end when the second is below the first. */
    private static void walk(BitSet values, int lowest, int highest, int from, int to, int step) {
        int size = highest - lowest + 1;
        int end = to < from ? to + size : to;
        for (int i = from; i <= end; i = i + step) {
            values.set(lowest + (i - lowest) % size);
        }
    }
}
