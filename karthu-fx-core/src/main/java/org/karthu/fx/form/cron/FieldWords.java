package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * The words of one field in the plain text: how its values are named, and its entries brought into the form the
 * sentences name them.
 */
final class FieldWords {

    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"};

    /** The weekday names in the sentences, by the day of the week counted from Sunday as 0 ({@link Weekdays}). */
    private static final String[] WEEKDAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
            "Saturday"};

    private final CronFieldKind kind;
    private final CronFormat format;

    /**
     * Creates the words of one field.
     *
     * @param kind   which field it is
     * @param format the format of the field's expression
     */
    FieldWords(CronFieldKind kind, CronFormat format) {
        this.kind = kind;
        this.format = format;
    }

    /**
     * The name of a value: a month or weekday by its name, an hour with two digits ({@code 08}), any other value as
     * its number.
     *
     * @param value the value, in the numbering of the expression's format
     * @return the value's name
     */
    String name(int value) {
        return switch (kind) {
            case MONTH -> MONTHS[value - 1];
            case WEEKDAY -> WEEKDAYS[Weekdays.ofValue(value, format)];
            case HOUR -> twoDigits(value);
            default -> Integer.toString(value);
        };
    }

    /**
     * A value or a range of values by name: {@code Monday} or {@code Monday to Friday}.
     *
     * @param part a value or a range
     * @return its name
     */
    String name(CronPart part) {
        if (part instanceof CronPart.Range range) {
            return name(range.start()) + " to " + name(range.end());
        }
        return name(((CronPart.Value) part).value());
    }

    /**
     * The field's unit in the singular: <em>second</em>, <em>minute</em>, <em>hour</em>, <em>day</em>, ...
     *
     * @return the unit
     */
    String unit() {
        return switch (kind) {
            case SECOND -> "second";
            case MINUTE -> "minute";
            case HOUR -> "hour";
            case DAY_OF_MONTH, WEEKDAY -> "day";
            case MONTH -> "month";
            case YEAR -> "year";
        };
    }

    /**
     * A count of the field's unit: <em>1 minute</em>, <em>15 minutes</em>.
     *
     * @param count the count
     * @return the count and the unit, singular or plural
     */
    String count(int count) {
        return count + " " + unit() + (count == 1 ? "" : "s");
    }

    /**
     * The unit every n-th of which a step takes: <em>minute</em> for 1, <em>15 minutes</em> for 15.
     *
     * @param step the step
     * @return the unit, with the step's number unless it is 1
     */
    String every(int step) {
        return step == 1 ? unit() : count(step);
    }

    /**
     * A step among the entries the sentences name: every n-th unit (<em>every 15 minutes</em>), from a value or
     * through a range, named after a lead and before a tail: <em>every 15 minutes from minute 5</em>, <em>every 2 days
     * from day 1 to 15 of the month</em>, <em>every 3 months from February</em>.
     *
     * @param step the step
     * @param lead what stands before the name of the step's start: <em>minute </em>, <em>day </em>, or nothing
     * @param tail what follows it: <em> of the month</em>, or nothing
     * @return the step's words
     */
    String step(CronPart.Step step, String lead, String tail) {
        String every = "every " + every(step.step());
        if (step.start() instanceof CronPart.Value value) {
            return every + " from " + lead + name(value.value()) + tail;
        }
        if (step.start() instanceof CronPart.Range range) {
            return every + " from " + lead + name(range) + tail;
        }
        return every;
    }

    /**
     * Entries named one after the other: the values and ranges together after the unit, singular for a single value
     * and plural else (<em>hour 08</em>, <em>minutes 0 to 10</em>, <em>on day 1 of the month</em>, <em>on days 1 and
     * 10 to 15 of the month</em>), then each step on its own after the unit (<em>every 2 days from day 1 to 15 of the
     * month</em>), joined by <em>and</em>.
     *
     * @param entries     the entries, as {@link #entries} gives them
     * @param preposition what stands before the unit of the values and ranges: <em>on </em>, or nothing
     * @param tail        what follows each name: <em> of the month</em>, or nothing
     * @return the entries' words
     */
    String list(List<CronPart> entries, String preposition, String tail) {
        List<String> names = new ArrayList<>();
        List<String> phrases = new ArrayList<>();
        boolean oneValue = false;
        for (CronPart entry : entries) {
            if (entry instanceof CronPart.Step step) {
                phrases.add(step(step, unit() + " ", tail));
            } else {
                names.add(name(entry));
                oneValue = names.size() == 1 && entry instanceof CronPart.Value;
            }
        }
        if (!names.isEmpty()) {
            String units = oneValue ? unit() : unit() + "s";
            phrases.add(0, preposition + units + " " + join(names) + tail);
        }
        return String.join(" and ", phrases);
    }

    /**
     * The field's entries in the form the sentences name them, each named once: an empty list when the field is
     * free ({@link CronField#isFree}). A step from the field's lowest value is a step through the whole field
     * ({@code 0/15} is {@code *}{@code /15}), a range of one value is that value, and the Unix weekday 7 is 0, both
     * being Sunday.
     *
     * @param field the field
     * @return the field's entries, each once
     */
    List<CronPart> entries(CronField field) {
        List<CronPart> entries = new ArrayList<>();
        if (field.isFree(format)) {
            return entries;
        }
        for (CronPart part : field.parts()) {
            CronPart entry = normalized(part);
            if (!entries.contains(entry)) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private CronPart normalized(CronPart part) {
        if (part instanceof CronPart.Value value) {
            return normalizedValue(value.value());
        }
        if (part instanceof CronPart.Range range) {
            return normalizedRange(range);
        }
        if (part instanceof CronPart.Step step) {
            if (kind.isWholeFieldStart(step.start(), format)) {
                return new CronPart.Step(new CronPart.All(), step.step());
            }
            if (step.start() instanceof CronPart.Range range && range.start() == range.end()) {
                return normalizedValue(range.start());
            }
        }
        return part;
    }

    /**
     * The values a step takes, in order. A Quartz range that wraps round the field's end is taken round it, as
     * Quartz's {@code CronExpression} does: {@code FRI-MON/2} is Friday and Sunday.
     *
     * @param step the step
     * @return the step's values
     */
    List<CronPart> stepValues(CronPart.Step step) {
        int lowest = kind.lowest(format);
        int highest = kind.highest();
        int start = lowest;
        int end = highest;
        if (step.start() instanceof CronPart.Value value) {
            start = value.value();
        } else if (step.start() instanceof CronPart.Range range) {
            start = range.start();
            end = range.end();
        }
        int size = highest - lowest + 1;
        if (end < start) {
            end = end + size;
        }
        List<CronPart> values = new ArrayList<>();
        for (int i = start; i <= end; i = i + step.step()) {
            values.add(normalizedValue(lowest + (i - lowest) % size));
        }
        return values;
    }

    private CronPart normalizedValue(int value) {
        if (isUnixWeekday() && value == 7) {
            return new CronPart.Value(0);
        }
        return new CronPart.Value(value);
    }

    private CronPart normalizedRange(CronPart.Range range) {
        if (range.start() == range.end()) {
            return normalizedValue(range.start());
        }
        // Unix 0-7 is every day of the week; named as written it would read Sunday to Sunday.
        if (isUnixWeekday() && range.start() == 0 && range.end() == 7) {
            return new CronPart.Range(0, 6);
        }
        return range;
    }

    private boolean isUnixWeekday() {
        return kind == CronFieldKind.WEEKDAY && format == CronFormat.UNIX;
    }

    /**
     * A number with at least two digits: {@code 09}.
     *
     * @param value the number
     * @return the number, with a leading zero below 10
     */
    static String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    /**
     * Joins names as English lists them: {@code a}, {@code a and b}, {@code a, b and c}.
     *
     * @param names the names
     * @return the names as one text
     */
    static String join(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }
}
