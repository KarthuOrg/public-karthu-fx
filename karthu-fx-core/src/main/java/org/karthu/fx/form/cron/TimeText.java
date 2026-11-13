package org.karthu.fx.form.cron;

import java.util.ArrayList;
import java.util.List;

/**
 * The time part of the plain text, from the second (Quartz only), the minute and the hour. The fields are looked at
 * from the smallest to the largest.
 */
final class TimeText {

    private final CronExpression expression;

    /** The time fields, smallest first: second (Quartz only), minute, hour. */
    private final List<TimeField> fields = new ArrayList<>();

    /**
     * Creates the time part of an expression's plain text.
     *
     * @param expression the valid expression
     */
    TimeText(CronExpression expression) {
        this.expression = expression;
        if (expression.second().isPresent()) {
            add(expression.second().get());
        }
        add(expression.minute());
        add(expression.hour());
    }

    private void add(CronField field) {
        FieldWords words = new FieldWords(field.kind(), expression.format());
        fields.add(new TimeField(field.kind(), words, words.entries(field)));
    }

    /**
     * Whether the time part names fixed times of the day (<em>At 09:00</em>, <em>At 08:00 and 12:00</em>), after
     * which a free day reads <em>every day</em>.
     *
     * @return true if the time part names fixed times
     */
    boolean isFixed() {
        return !fixedTimes().isEmpty();
    }

    /**
     * The time part, never empty.
     *
     * @return the time part
     */
    String text() {
        List<int[]> fixed = fixedTimes();
        if (!fixed.isEmpty()) {
            List<String> clocks = new ArrayList<>();
            for (int[] time : fixed) {
                clocks.add(clock(time));
            }
            return "At " + FieldWords.join(clocks);
        }
        int recurring = smallestNotSingle();
        TimeField field = fields.get(recurring);
        if (field.isValueList() && isOffsetZero(recurring) && isFreeAbove(recurring)) {
            return "At " + pastEvery(recurring);
        }
        String lead = lead(recurring);
        if (lead == null) {
            return enumeration(0, "At " + fieldPhrase(0));
        }
        if (isFreeAbove(recurring)) {
            return lead;
        }
        String window = window(recurring);
        if (window != null) {
            return lead + ", between " + window;
        }
        // A range leads like the catalogue's At second 5, minutes 0 to 10, hours 08 and 12; free and stepping fields
        // keep their own words, so that it shows that the time recurs.
        if (field.isSingleRange()) {
            return enumeration(0, "At " + fieldPhrase(0));
        }
        return enumeration(recurring, lead);
    }

    // --- Fixed times -----------------------------------------------------------------------------------

    /**
     * The fixed times of the day the time part names, each as the values of the fields, smallest first: one when every
     * field is a single value (<em>At 09:00</em>), one per value when exactly one field is a list of single values and
     * the others single values (<em>At 08:00, 12:00 and 18:00</em>), else none.
     */
    private List<int[]> fixedTimes() {
        int[] time = new int[fields.size()];
        TimeField list = null;
        int listIndex = 0;
        for (int i = 0; i < fields.size(); i++) {
            TimeField field = fields.get(i);
            if (field.isSingle()) {
                time[i] = field.value();
            } else if (list == null && field.isValueList()) {
                list = field;
                listIndex = i;
            } else {
                return List.of();
            }
        }
        if (list == null) {
            return List.of(time);
        }
        List<int[]> times = new ArrayList<>();
        for (CronPart entry : list.entries()) {
            int[] listTime = time.clone();
            listTime[listIndex] = ((CronPart.Value) entry).value();
            times.add(listTime);
        }
        return times;
    }

    // --- Recurring times -------------------------------------------------------------------------------

    /**
     * The sentence of a recurring time from the smallest field that is not a single value, the fields below it being
     * single values that give the offset within its unit; null when that field is a list or has an offset the
     * sentence cannot name.
     *
     * <ul>
     *   <li>{@code *}: <em>Every minute</em>, or with an offset <em>At 30 minutes past every hour</em>.</li>
     *   <li>A step, or a range as a step of 1: <em>Every 15 minutes</em>, from a value <em>, starting at minute
     *       5</em>, through a range <em>, from minute 10 to 40</em>. Below the hour only without offset.</li>
     *   <li>A step or range of the hour names times of day, the offset in them: <em>Every 2 hours, starting at
     *       00:30</em>, <em>Every hour, between 09:00 and 17:00</em>.</li>
     * </ul>
     */
    private String lead(int index) {
        TimeField field = fields.get(index);
        FieldWords unit = field.words();
        boolean offsetZero = isOffsetZero(index);
        if (field.isFree()) {
            if (offsetZero) {
                return "Every " + unit.unit();
            }
            return "At " + offset(index) + " past every " + unit.unit();
        }
        if (field.entries().size() != 1) {
            return null;
        }
        CronPart entry = field.entries().get(0);
        int step = 1;
        CronPart start = entry;
        if (entry instanceof CronPart.Step stepEntry) {
            step = stepEntry.step();
            start = stepEntry.start();
        }
        String every = "Every " + unit.every(step);
        if (isHour(index)) {
            if (start instanceof CronPart.Value value) {
                return every + ", starting at " + clock(offsetValues(index, value.value()));
            }
            if (start instanceof CronPart.Range range) {
                return every + ", between " + clock(offsetValues(index, range.start())) + " and "
                        + clock(offsetValues(index, range.end()));
            }
            if (offsetZero) {
                return every;
            }
            return every + ", starting at " + clock(offsetValues(index, 0));
        }
        if (!offsetZero) {
            return null;
        }
        if (start instanceof CronPart.Value value) {
            return every + ", starting at " + unit.unit() + " " + value.value();
        }
        if (start instanceof CronPart.Range range) {
            return every + ", from " + unit.unit() + " " + range.start() + " to " + range.end();
        }
        return every;
    }

    /**
     * The window of hours that narrows a recurring time below the hour: <em>09:00 and 17:59</em> after
     * <em>between</em>. There is one when the hour is a single value or a range and every field between is free
     * (<em>Every 15 minutes, between 09:00 and 17:59</em>), or when the hour is a single value and every field between
     * a single value (<em>Every second, between 09:30:00 and 09:30:59</em>); else null. A field that recurs reaches
     * from its first value to its last, a free field from its lowest to its highest.
     */
    private String window(int index) {
        int hourIndex = fields.size() - 1;
        TimeField hour = fields.get(hourIndex);
        boolean betweenFree = true;
        boolean betweenSingle = true;
        for (int i = index + 1; i < hourIndex; i++) {
            betweenFree = betweenFree && fields.get(i).isFree();
            betweenSingle = betweenSingle && fields.get(i).isSingle();
        }
        boolean narrows = (betweenFree && (hour.isSingle() || hour.isSingleRange()))
                || (betweenSingle && hour.isSingle());
        if (!narrows) {
            return null;
        }
        int[] first = new int[fields.size()];
        int[] last = new int[fields.size()];
        for (int i = 0; i < hourIndex; i++) {
            first[i] = first(fields.get(i));
            last[i] = last(fields.get(i));
        }
        if (hour.isSingle()) {
            first[hourIndex] = hour.value();
            last[hourIndex] = hour.value();
        } else {
            CronPart.Range range = (CronPart.Range) hour.entries().get(0);
            first[hourIndex] = range.start();
            last[hourIndex] = range.end();
        }
        return clock(first) + " and " + clock(last);
    }

    /** The first value a field below the hour takes in the window of hours. */
    private int first(TimeField field) {
        if (field.isSingle()) {
            return field.value();
        }
        CronPart start = field.recurringStart();
        if (start instanceof CronPart.Value value) {
            return value.value();
        }
        if (start instanceof CronPart.Range range && !range.wraps()) {
            return range.start();
        }
        return field.kind().lowest(expression.format());
    }

    /** The last value a field below the hour takes in the window of hours. */
    private int last(TimeField field) {
        if (field.isSingle()) {
            return field.value();
        }
        CronPart start = field.recurringStart();
        if (start instanceof CronPart.Range range && !range.wraps()) {
            return range.end();
        }
        return field.kind().highest();
    }

    // --- Enumeration -----------------------------------------------------------------------------------

    /**
     * Any other time: a lead for the fields up to {@code index}, then each larger field that restricts the time named
     * on its own, a free field left out. The lead is the recurring field's own sentence (<em>Every minute, hour 12 and
     * every 3 hours</em>), or the smallest field named like the others (<em>At second 5, minutes 0 to 10, hours 08
     * and 12</em>).
     */
    private String enumeration(int index, String lead) {
        List<String> phrases = new ArrayList<>();
        phrases.add(lead);
        for (int i = index + 1; i < fields.size(); i++) {
            if (!fields.get(i).isFree()) {
                phrases.add(fieldPhrase(i));
            }
        }
        return String.join(", ", phrases);
    }

    /**
     * A field in the enumeration: its values and ranges after the unit, singular for a single value (<em>hour
     * 08</em>) and plural else (<em>minutes 0 to 10</em>), then each step (<em>every 15 minutes from minute 5</em>,
     * <em>every 2 hours from hour 08 to 16</em>), joined by <em>and</em>.
     */
    private String fieldPhrase(int index) {
        TimeField field = fields.get(index);
        return field.words().list(field.entries(), "", "");
    }

    /**
     * A list of values of a field below the hour, every field above free: <em>0 and 30 minutes past every hour</em>,
     * in the words of the catalogue's <em>At 30 minutes past every hour</em>.
     */
    private String pastEvery(int index) {
        FieldWords words = fields.get(index).words();
        List<String> names = new ArrayList<>();
        for (CronPart entry : fields.get(index).entries()) {
            names.add(words.name(entry));
        }
        return FieldWords.join(names) + " " + words.unit() + "s past every " + fields.get(index + 1).words().unit();
    }

    /** The offset of the fields below a field, largest first, those at 0 left out: <em>30 minutes</em>. */
    private String offset(int index) {
        List<String> counts = new ArrayList<>();
        for (int i = index - 1; i >= 0; i--) {
            TimeField below = fields.get(i);
            if (below.value() != 0) {
                counts.add(below.words().count(below.value()));
            }
        }
        return String.join(" and ", counts);
    }

    private boolean isOffsetZero(int index) {
        for (int i = 0; i < index; i++) {
            if (fields.get(i).value() != 0) {
                return false;
            }
        }
        return true;
    }

    /** The values of the fields up to a field: its own value given, the fields below it at their single values. */
    private int[] offsetValues(int index, int value) {
        int[] values = new int[fields.size()];
        for (int i = 0; i < index; i++) {
            values[i] = fields.get(i).value();
        }
        values[index] = value;
        return values;
    }

    // --- Helpers -----------------------------------------------------------------------------------------

    private int smallestNotSingle() {
        for (int i = 0; i < fields.size(); i++) {
            if (!fields.get(i).isSingle()) {
                return i;
            }
        }
        throw new IllegalStateException("every field is a single value");
    }

    /** A time of day from the values of the fields, smallest first: <em>09:00</em>, Quartz <em>09:00:00</em>. */
    private String clock(int[] values) {
        int hour = values[values.length - 1];
        int minute = values[values.length - 2];
        String clock = FieldWords.twoDigits(hour) + ":" + FieldWords.twoDigits(minute);
        if (values.length == 3) {
            clock = clock + ":" + FieldWords.twoDigits(values[0]);
        }
        return clock;
    }

    private boolean isFreeAbove(int index) {
        for (int i = index + 1; i < fields.size(); i++) {
            if (!fields.get(i).isFree()) {
                return false;
            }
        }
        return true;
    }

    private boolean isHour(int index) {
        return index == fields.size() - 1;
    }

    /**
     * A time field as the sentences see it: which field, its words, and its entries as {@link FieldWords#entries}
     * gives them, empty for a free field.
     *
     * @param kind    which field
     * @param words   the field's words
     * @param entries the field's entries in the form the sentences name them
     */
    private record TimeField(CronFieldKind kind, FieldWords words, List<CronPart> entries) {

        /** Whether the field restricts nothing. */
        boolean isFree() {
            return entries.isEmpty();
        }

        /** Whether the field is a single value. */
        boolean isSingle() {
            return entries.size() == 1 && entries.get(0) instanceof CronPart.Value;
        }

        /** Whether the field is a single range. */
        boolean isSingleRange() {
            return entries.size() == 1 && entries.get(0) instanceof CronPart.Range;
        }

        /** Whether the field is a list of single values, at least one. */
        boolean isValueList() {
            for (CronPart entry : entries) {
                if (!(entry instanceof CronPart.Value)) {
                    return false;
                }
            }
            return !entries.isEmpty();
        }

        /** The field's first value: its single value, or the first of its list of values. */
        int value() {
            return ((CronPart.Value) entries.get(0)).value();
        }

        /** What a recurring field goes through: {@code *} when free, a step's start, or the range itself. */
        CronPart recurringStart() {
            if (isFree()) {
                return new CronPart.All();
            }
            CronPart entry = entries.get(0);
            if (entry instanceof CronPart.Step step) {
                return step.start();
            }
            return entry;
        }
    }
}
