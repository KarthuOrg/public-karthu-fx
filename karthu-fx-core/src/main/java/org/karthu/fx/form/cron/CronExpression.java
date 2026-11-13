package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

import java.util.Optional;

/**
 * A valid cron expression, parsed: its format, the macro it was written as, and its fields. What describes it in plain
 * text or converts it to the other format works on this form and never parses the text again.
 *
 * @param format     the format it is valid in, which decides the numbering of the weekdays
 * @param macro      the Unix macro it was written as ({@code @daily}), whose expression the fields hold, or empty
 * @param second     the second, Quartz only (empty for Unix)
 * @param minute     the minute
 * @param hour       the hour
 * @param dayOfMonth the day of month
 * @param month      the month
 * @param weekday    the weekday
 * @param year       the year, Quartz only and optional (empty for Unix and for six Quartz fields)
 */
public record CronExpression(CronFormat format, Optional<CronMacro> macro, Optional<CronField> second,
                             CronField minute, CronField hour, CronField dayOfMonth, CronField month,
                             CronField weekday, Optional<CronField> year) {
}
