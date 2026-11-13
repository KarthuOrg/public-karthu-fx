package org.karthu.fx.form.cron;

import java.util.ArrayList;
import java.util.List;

/**
 * The plain text of a valid cron expression: the English sentence a user reads under the field, without full stop.
 * It works on the parsed form ({@link CronExpression}) and never parses the text again.
 *
 * <p>The sentence is built from a time part, then a day part, a month part and a year part, joined by commas; a part
 * that restricts nothing is left out, except that a free day reads <em>every day</em> after fixed times (<em>At
 * 09:00, every day</em>). A macro is described as the expression it stands for.
 *
 * <ul>
 *   <li><b>Time</b>, the time of day in 24-hour form, with seconds for Quartz: fixed times (<em>At 09:00</em>,
 *       <em>At 08:00, 12:00 and 18:00</em>); recurring times from the smallest field that is not a single value
 *       (<em>Every minute</em>, <em>At 30 minutes past every hour</em>, <em>Every 15 minutes, starting at minute
 *       5</em>), narrowed by a single hour or a range of hours (<em>, between 09:00 and 17:59</em>); else each field
 *       named on its own (<em>At second 5, minutes 0 to 10, hours 08 and 12</em>).</li>
 *   <li><b>Day</b>: the weekday by name (<em>Monday to Friday</em>, <em>on Saturday and Sunday</em>, a step by the
 *       days it takes), the day of month (<em>on day 1 of the month</em>, <em>every 2 days</em>), Quartz's special
 *       forms (<em>on the last weekday of the month</em>, <em>on the third Friday of the month</em>), Unix's either
 *       day (<em>on day 1 of the month or on Monday</em>).</li>
 *   <li><b>Month</b> and <b>year</b>: <em>in January and July</em>, <em>January to March</em>, <em>every 3
 *       months</em>, <em>in 2027</em>, <em>2027 to 2029</em>.</li>
 * </ul>
 *
 * <p>This package is not exported: the sentences are pinned down by their own tests (an example catalogue), and the
 * cron input is their only caller.
 */
public final class CronDescription {

    private final CronExpression expression;

    private CronDescription(CronExpression expression) {
        this.expression = expression;
    }

    /**
     * Describes a valid expression in plain text. Every valid expression has a sentence, never an empty one.
     *
     * @param expression the expression, as {@link CronRules#check} parsed it into {@link CronCheck.Valid}
     * @return the sentence, English, without full stop
     */
    public static String describe(CronExpression expression) {
        return new CronDescription(expression).sentence();
    }

    private String sentence() {
        List<String> parts = new ArrayList<>();
        TimeText time = new TimeText(expression);
        parts.add(time.text());
        DateText date = new DateText(expression);
        String day = date.day();
        if (!day.isEmpty()) {
            parts.add(day);
        } else if (time.isFixed()) {
            parts.add("every day");
        }
        addUnlessEmpty(parts, date.month());
        addUnlessEmpty(parts, date.year());
        return String.join(", ", parts);
    }

    private static void addUnlessEmpty(List<String> parts, String part) {
        if (!part.isEmpty()) {
            parts.add(part);
        }
    }
}
