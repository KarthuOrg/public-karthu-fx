package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The date parts of the plain text: the day (day of month and weekday together), the month and the year. A part that
 * restricts nothing is empty.
 */
final class DateText {

    private static final String[] ORDINALS = {"first", "second", "third", "fourth", "fifth"};

    private final CronExpression expression;

    /**
     * Creates the date parts of an expression's plain text.
     *
     * @param expression the valid expression
     */
    DateText(CronExpression expression) {
        this.expression = expression;
    }

    /**
     * The day part, or empty when the day is not restricted.
     *
     * <p>Quartz restricts the day by one of the two fields, the other one being {@code ?}. Unix runs on either day
     * when both fields are restricted (<em>on day 1 of the month or on Monday</em>), and on the days both allow when
     * one of them is not. As cronie decides it, a field counts as not restricted when it starts with {@code *}, so
     * {@code *}{@code /2} with weekday {@code 1} is every second day that is a Monday: <em>every 2 days, on
     * Monday</em>.
     *
     * @return the day part, or an empty text
     */
    String day() {
        CronField dayOfMonth = expression.dayOfMonth();
        CronField weekday = expression.weekday();
        boolean dayOfMonthFree = dayOfMonth.isFree(expression.format());
        boolean weekdayFree = weekday.isFree(expression.format());
        boolean either = expression.format() == CronFormat.UNIX && !dayOfMonth.text().startsWith("*")
                && !weekday.text().startsWith("*");
        if (either) {
            // Either day: a field that takes every day (5,* for instance) makes it every day.
            if (dayOfMonthFree || weekdayFree) {
                return "";
            }
            return dayOfMonth(dayOfMonth) + " or " + weekday(weekday);
        }
        List<String> parts = new ArrayList<>();
        if (!dayOfMonthFree) {
            parts.add(dayOfMonth(dayOfMonth));
        }
        if (!weekdayFree) {
            parts.add(weekday(weekday));
        }
        return String.join(", ", parts);
    }

    /**
     * The month part: <em>in January</em>, <em>January to March</em>, <em>every 3 months</em>, or empty.
     *
     * @return the month part, or an empty text
     */
    String month() {
        return list(expression.month());
    }

    /**
     * The year part, Quartz only: <em>in 2027</em>, <em>2027 to 2029</em>, or empty without a year.
     *
     * @return the year part, or an empty text
     */
    String year() {
        Optional<CronField> year = expression.year();
        if (year.isEmpty()) {
            return "";
        }
        return list(year.get());
    }

    /**
     * The day of month: Quartz's forms, else its values and ranges together (<em>on day 1 of the month</em>, <em>on
     * days 1 and 10 to 15 of the month</em>) and each step (<em>every 2 days</em>), joined by <em>and</em>.
     */
    private String dayOfMonth(CronField field) {
        CronPart only = field.parts().get(0);
        FieldWords words = words(CronFieldKind.DAY_OF_MONTH);
        if (only instanceof CronPart.LastDayOfMonth last) {
            if (last.daysBefore() == 0) {
                return "on the last day of the month";
            }
            return words.count(last.daysBefore()) + " before the last day of the month";
        }
        if (only instanceof CronPart.LastWorkdayOfMonth) {
            return "on the last weekday of the month";
        }
        if (only instanceof CronPart.NearestWorkday nearest) {
            return "on the weekday nearest day " + nearest.dayOfMonth() + " of the month";
        }
        return words.list(words.entries(field), "on ", " of the month");
    }

    /**
     * The weekday: Quartz's forms, else its days by name. A step is named by the days it takes (<em>on Sunday,
     * Tuesday, Thursday and Saturday</em>), so that the numbering of the format cannot confuse.
     */
    private String weekday(CronField field) {
        FieldWords words = words(CronFieldKind.WEEKDAY);
        CronPart only = field.parts().get(0);
        if (only instanceof CronPart.LastDayOfWeek) {
            // Quartz's L alone in the weekday is the last day of the week, Saturday.
            return "on " + words.name(7);
        }
        if (only instanceof CronPart.LastDayOfWeekInMonth last) {
            return "on the last " + words.name(last.dayOfWeek()) + " of the month";
        }
        if (only instanceof CronPart.NthDayOfWeekInMonth nth) {
            return "on the " + ORDINALS[nth.occurrence() - 1] + " " + words.name(nth.dayOfWeek()) + " of the month";
        }
        List<CronPart> days = new ArrayList<>();
        for (CronPart entry : words.entries(field)) {
            if (entry instanceof CronPart.Step step) {
                for (CronPart value : words.stepValues(step)) {
                    addOnce(days, value);
                }
            } else {
                addOnce(days, entry);
            }
        }
        return group(words, days, "on ");
    }

    /**
     * The month or the year: its values and ranges together, each step on its own, joined by <em>and</em>
     * (<em>in January and every 4 months</em>).
     */
    private String list(CronField field) {
        FieldWords words = words(field.kind());
        List<CronPart> group = new ArrayList<>();
        List<String> phrases = new ArrayList<>();
        for (CronPart entry : words.entries(field)) {
            if (entry instanceof CronPart.Step step) {
                phrases.add(words.step(step, "", ""));
            } else {
                group.add(entry);
            }
        }
        if (!group.isEmpty()) {
            phrases.add(0, group(words, group, "in "));
        }
        return String.join(" and ", phrases);
    }

    /**
     * Values and ranges by name, introduced by the preposition when a single value is among them (<em>in January and
     * March to May</em>, <em>on Saturday and Sunday</em>); ranges alone stand without it (<em>January to March</em>,
     * <em>Monday to Friday</em>).
     */
    private static String group(FieldWords words, List<CronPart> entries, String preposition) {
        List<String> names = new ArrayList<>();
        boolean anyValue = false;
        for (CronPart entry : entries) {
            names.add(words.name(entry));
            anyValue = anyValue || entry instanceof CronPart.Value;
        }
        return (anyValue ? preposition : "") + FieldWords.join(names);
    }

    private static void addOnce(List<CronPart> entries, CronPart entry) {
        if (!entries.contains(entry)) {
            entries.add(entry);
        }
    }

    private FieldWords words(CronFieldKind kind) {
        return new FieldWords(kind, expression.format());
    }
}
