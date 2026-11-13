package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The conversion of a valid cron expression into the other format, Unix to Quartz or Quartz to Unix, when nothing is
 * lost: the result is valid in the other format by these rules ({@link CronRules}) and means the same schedule. It works
 * on the parsed form ({@link CronExpression}) and never parses the text again; only the result is checked.
 *
 * <p>The result is written with single spaces, its fields as they were written (case kept), except the weekday, whose
 * values are written as names ({@code MON-FRI}), so that the different numbering of the two formats cannot confuse,
 * and Quartz's {@code ?}. A macro converts as the expression it stands for.
 * <ul>
 *   <li><b>Unix to Quartz:</b> a leading {@code 0} for the seconds, no year. One of the two day fields becomes
 *       {@code ?}: the weekday when it restricts nothing, else the day of month when that restricts nothing. What
 *       restricts nothing is read as the plain text reads it ({@link CronField#isFree}: {@code *}, a list with
 *       {@code *} in it, a step of 1 through the whole field). The Unix day is read as cronie reads it: when one of the two fields starts with
 *       {@code *}, a day must match both, so the field that restricts nothing can go; when neither does, a day must
 *       match either, and only both restricting nothing (every day) converts.</li>
 *   <li><b>Quartz to Unix:</b> the seconds dropped when they are {@code 0}, the year dropped when it is absent or
 *       restricts nothing, {@code ?} written as {@code *}.</li>
 * </ul>
 * <i>Not convertible:</i> a step in the weekday (a step counts numbers, which the formats number differently),
 * Unix's day of month and weekday where neither can become {@code ?}, Quartz's seconds other than {@code 0}, a year,
 * {@code L}, {@code W} and {@code #}, a Quartz range that wraps round the field's end ({@code 22-2}, {@code FRI-MON}),
 * which Unix refuses, and a Unix weekday range whose names would not run the same way in Quartz ({@code 5-7} would be
 * {@code FRI-SUN}, {@code 0-7} {@code SUN-SUN}).
 *
 * <p>A step after a single value ({@code 5/15}) is written as it is in both directions: both formats take it, from the
 * value to the field's end, by these rules (cronie documents a step after a range or {@code *} only).
 *
 * <p>This package is not exported: the conversions are pinned down by their own tests (an example catalogue), and
 * the cron input is their only caller.
 */
public final class CronConversion {

    private CronConversion() {
    }

    /**
     * Converts a valid expression into the other format.
     *
     * @param expression the expression, as {@link CronRules#check} parsed it into {@link CronCheck.Valid}
     * @return the expression written in the other format, valid there and meaning the same schedule, or empty when it
     *         is not convertible without loss
     */
    public static Optional<String> convert(CronExpression expression) {
        CronFormat target = expression.format() == CronFormat.UNIX ? CronFormat.QUARTZ : CronFormat.UNIX;
        String converted;
        try {
            if (expression.format() == CronFormat.UNIX) {
                converted = toQuartz(expression);
            } else {
                converted = toUnix(expression);
            }
        } catch (NotConvertible notConvertible) {
            return Optional.empty();
        }
        // The result must hold by the target's own rules; what is refused above never gets here, this is the guard.
        if (!(CronRules.check(converted, target) instanceof CronCheck.Valid)) {
            return Optional.empty();
        }
        return Optional.of(converted);
    }

    // --- Unix to Quartz --------------------------------------------------------------------------------

    private static String toQuartz(CronExpression unix) throws NotConvertible {
        CronField dayOfMonth = unix.dayOfMonth();
        CronField weekday = unix.weekday();
        refuseStep(weekday);
        boolean dayOfMonthFree = dayOfMonth.isFree(CronFormat.UNIX);
        boolean weekdayFree = weekday.isFree(CronFormat.UNIX);
        // cronie's DOM_STAR and DOW_STAR: a field starting with * makes a day match both fields; else either will do.
        boolean bothMatch = dayOfMonth.text().startsWith("*") || weekday.text().startsWith("*");
        String dayOfMonthText;
        String weekdayText;
        if (weekdayFree && (bothMatch || dayOfMonthFree)) {
            // The days are the day of month's (also when both are every day).
            dayOfMonthText = dayOfMonth.text();
            weekdayText = "?";
        } else if (dayOfMonthFree && bothMatch) {
            // The days are the weekday's.
            dayOfMonthText = "?";
            weekdayText = weekdayNames(weekday, CronFormat.UNIX);
        } else {
            // Neither field can become ?: both restrict the day, which Quartz cannot say ("day 1 or Monday", "every
            // second day that is a Monday"), or a day must match either and only one field takes every day (5,* with
            // 1 is every day), so ? for it would leave the other field's days.
            throw new NotConvertible();
        }
        return String.join(" ", "0", unix.minute().text(), unix.hour().text(), dayOfMonthText,
                unix.month().text(), weekdayText);
    }

    // --- Quartz to Unix --------------------------------------------------------------------------------

    private static String toUnix(CronExpression quartz) throws NotConvertible {
        if (quartz.second().isEmpty() || !isZero(quartz.second().get())) {
            throw new NotConvertible();
        }
        if (quartz.year().isPresent() && !quartz.year().get().isFree(CronFormat.QUARTZ)) {
            throw new NotConvertible();
        }
        CronField dayOfMonth = quartz.dayOfMonth();
        CronField weekday = quartz.weekday();
        refuseStep(weekday);
        List<CronField> kept = List.of(quartz.minute(), quartz.hour(), dayOfMonth, quartz.month());
        for (CronField field : kept) {
            refuseWhatUnixLacks(field);
        }
        // Quartz's ? is Unix's *, which starts the field, so a day must match the other field alone, as in Quartz.
        String dayOfMonthText = dayOfMonth.isNoSpecificValue() ? "*" : dayOfMonth.text();
        String weekdayText = weekday.isNoSpecificValue() ? "*" : weekdayNames(weekday, CronFormat.QUARTZ);
        return String.join(" ", quartz.minute().text(), quartz.hour().text(), dayOfMonthText, quartz.month().text(),
                weekdayText);
    }

    /** Whether Quartz's seconds are {@code 0} alone, which Unix's minute-wise schedule matches. */
    private static boolean isZero(CronField second) {
        return second.parts().size() == 1 && second.parts().get(0) instanceof CronPart.Value value
                && value.value() == 0;
    }

    /**
     * Refuses what a field kept as written cannot say in Unix: a range that wraps round the field's end, Quartz's
     * {@code 22-2} or {@code 25-5}, also in a step (Unix refuses a range whose start is above its end), and Quartz's
     * special forms of the day of month, {@code L}, {@code L-n}, {@code LW} and {@code nW}.
     */
    private static void refuseWhatUnixLacks(CronField field) throws NotConvertible {
        for (CronPart part : field.parts()) {
            CronPart range = part instanceof CronPart.Step step ? step.start() : part;
            if (range instanceof CronPart.Range wrapping && wrapping.wraps()) {
                throw new NotConvertible();
            }
            if (isSpecialForm(part)) {
                throw new NotConvertible();
            }
        }
    }

    // --- The weekday ----------------------------------------------------------------------------------

    /** A step in the weekday counts numbers, which the two formats number differently: never converted. */
    private static void refuseStep(CronField weekday) throws NotConvertible {
        for (CronPart part : weekday.parts()) {
            if (part instanceof CronPart.Step) {
                throw new NotConvertible();
            }
        }
    }

    /**
     * The weekday with its values as names, its entries in the order written: {@code 1,3-5} is {@code MON,WED-FRI}.
     * A range keeps its ends, as names, only when they run the same way in the other format. Quartz's special forms
     * ({@code L}, {@code nL}, {@code n#k}) are not convertible.
     */
    private static String weekdayNames(CronField weekday, CronFormat from) throws NotConvertible {
        List<String> entries = new ArrayList<>();
        for (CronPart part : weekday.parts()) {
            if (part instanceof CronPart.All) {
                entries.add("*");
            } else if (part instanceof CronPart.Value value) {
                entries.add(Weekdays.name(Weekdays.ofValue(value.value(), from)));
            } else if (part instanceof CronPart.Range range) {
                int start = Weekdays.ofValue(range.start(), from);
                int end = Weekdays.ofValue(range.end(), from);
                // Quartz's wrapping FRI-MON, which Unix refuses, and Unix's 5-7 (FRI-SUN, backwards in Quartz) and 0-7
                // (SUN-SUN, a single day in Quartz) take a different number of days once written as names.
                if (end - start != range.end() - range.start()) {
                    throw new NotConvertible();
                }
                entries.add(Weekdays.name(start) + "-" + Weekdays.name(end));
            } else {
                throw new NotConvertible();
            }
        }
        return String.join(",", entries);
    }

    // --- Helpers --------------------------------------------------------------------------------------

    private static boolean isSpecialForm(CronPart part) {
        return part instanceof CronPart.LastDayOfMonth || part instanceof CronPart.LastWorkdayOfMonth
                || part instanceof CronPart.NearestWorkday || part instanceof CronPart.LastDayOfWeek
                || part instanceof CronPart.LastDayOfWeekInMonth || part instanceof CronPart.NthDayOfWeekInMonth;
    }

    /** The expression cannot be written in the other format without loss. */
    private static final class NotConvertible extends Exception {

        private NotConvertible() {
            super(null, null, false, false);
        }
    }
}
