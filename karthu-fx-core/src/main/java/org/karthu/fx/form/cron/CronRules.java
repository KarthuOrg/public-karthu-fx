package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The rules of the cron input, independent of JavaFX: whether a text is a valid Unix or Quartz cron expression, and
 * either its parsed form ({@link CronExpression}) or the message of the first rule it breaks.
 *
 * <p>The text is split into fields at any run of whitespace ({@link Character#isWhitespace}), whitespace around it
 * ignored. The rules are checked in this order, and the first broken one gives the message:
 * <ol>
 *   <li>the macro: a first field starting with {@code @} (Quartz has none, Unix knows those of {@link CronMacro});</li>
 *   <li>the field count: Unix 5, Quartz 6 or 7;</li>
 *   <li>each field from left to right: first the characters the format refuses in it, then its form (Quartz's
 *       {@code ?} and special forms stand alone), then its entries from left to right and their values;</li>
 *   <li>the rules across fields: in Quartz exactly one of day of month and weekday is {@code ?}.</li>
 * </ol>
 * A message is English, one sentence, without full stop, and names the field (<em>Minute must be 0 to 59, found
 * 60</em>). A text without fields, null included, is checked like any other: it has the wrong field count.
 *
 * <p>Where a detail is left open, the rules follow cronie's {@code crontab(5)} for Unix and Quartz's CronTrigger
 * documentation and {@code CronExpression} for Quartz; the comments at each rule say which.
 *
 * <p>This package is not exported: the rules are pinned down by their own tests (an example catalogue), and the cron
 * input is their only caller. What describes an expression in plain text ({@link CronDescription}) and what converts
 * it to the other format ({@link CronConversion}) work on the parsed form.
 */
public final class CronRules {

    private CronRules() {
    }

    /**
     * Checks a text against a format.
     *
     * @param text   the text in the field, may be null
     * @param format the format to check it against
     * @return the parsed expression when the text is valid, else the message of the first broken rule
     */
    public static CronCheck check(String text, CronFormat format) {
        List<String> words = split(text);
        try {
            return new CronCheck.Valid(expression(words, format));
        } catch (BrokenRule broken) {
            return new CronCheck.Invalid(broken.getMessage());
        }
    }

    private static CronExpression expression(List<String> words, CronFormat format) throws BrokenRule {
        if (!words.isEmpty() && words.get(0).startsWith("@")) {
            return macro(words, format);
        }
        if (format == CronFormat.UNIX) {
            return unix(words, Optional.empty());
        }
        return quartz(words);
    }

    /**
     * An expression whose first word starts with {@code @}: Quartz has no macros; Unix knows the macros of
     * {@link CronMacro} in any case, as the only word. {@code @reboot} is a crontab macro but no schedule.
     */
    private static CronExpression macro(List<String> words, CronFormat format) throws BrokenRule {
        if (format == CronFormat.QUARTZ) {
            throw new BrokenRule("Quartz cron has no macros");
        }
        String written = words.get(0);
        if (written.equalsIgnoreCase("@reboot")) {
            throw new BrokenRule(written + " is not a schedule");
        }
        CronMacro macro = null;
        for (CronMacro candidate : CronMacro.values()) {
            if (candidate.word().equalsIgnoreCase(written)) {
                macro = candidate;
            }
        }
        if (macro == null) {
            throw new BrokenRule("Unknown macro " + written);
        }
        // In a crontab line the words after a macro are the command; in a field for the schedule alone they are an
        // error.
        if (words.size() > 1) {
            throw new BrokenRule(written + " must stand alone, found " + words.size() + " fields");
        }
        return unix(split(macro.expansion()), Optional.of(macro));
    }

    private static CronExpression unix(List<String> words, Optional<CronMacro> macro) throws BrokenRule {
        if (words.size() != 5) {
            throw new BrokenRule("Unix cron has 5 fields, found " + words.size());
        }
        CronFormat format = CronFormat.UNIX;
        CronField minute = FieldParser.parse(CronFieldKind.MINUTE, format, words.get(0));
        CronField hour = FieldParser.parse(CronFieldKind.HOUR, format, words.get(1));
        CronField dayOfMonth = FieldParser.parse(CronFieldKind.DAY_OF_MONTH, format, words.get(2));
        CronField month = FieldParser.parse(CronFieldKind.MONTH, format, words.get(3));
        CronField weekday = FieldParser.parse(CronFieldKind.WEEKDAY, format, words.get(4));
        return new CronExpression(format, macro, Optional.empty(), minute, hour, dayOfMonth, month, weekday,
                Optional.empty());
    }

    private static CronExpression quartz(List<String> words) throws BrokenRule {
        // Quartz's CronExpression ignores words after the year; these rules count them, so eight fields are refused.
        if (words.size() < 6 || words.size() > 7) {
            throw new BrokenRule("Quartz cron has 6 or 7 fields, found " + words.size());
        }
        CronFormat format = CronFormat.QUARTZ;
        CronField second = FieldParser.parse(CronFieldKind.SECOND, format, words.get(0));
        CronField minute = FieldParser.parse(CronFieldKind.MINUTE, format, words.get(1));
        CronField hour = FieldParser.parse(CronFieldKind.HOUR, format, words.get(2));
        CronField dayOfMonth = FieldParser.parse(CronFieldKind.DAY_OF_MONTH, format, words.get(3));
        CronField month = FieldParser.parse(CronFieldKind.MONTH, format, words.get(4));
        CronField weekday = FieldParser.parse(CronFieldKind.WEEKDAY, format, words.get(5));
        Optional<CronField> year = Optional.empty();
        if (words.size() == 7) {
            year = Optional.of(FieldParser.parse(CronFieldKind.YEAR, format, words.get(6)));
        }
        // The rule across fields: Quartz supports a day of month or a weekday, never both, and needs one of them.
        boolean noDayOfMonth = dayOfMonth.isNoSpecificValue();
        boolean noWeekday = weekday.isNoSpecificValue();
        if (noDayOfMonth && noWeekday) {
            throw new BrokenRule("Only one of day of month and weekday can be ?");
        }
        if (!noDayOfMonth && !noWeekday) {
            throw new BrokenRule("Day of month or weekday must be ?");
        }
        return new CronExpression(format, Optional.empty(), Optional.of(second), minute, hour, dayOfMonth, month,
                weekday, year);
    }

    /** The fields of a text: split at any run of whitespace, none for null. */
    private static List<String> split(String text) {
        List<String> words = new ArrayList<>();
        if (text == null) {
            return words;
        }
        int i = 0;
        while (i < text.length()) {
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            int start = i;
            while (i < text.length() && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            if (i > start) {
                words.add(text.substring(start, i));
            }
        }
        return words;
    }
}
