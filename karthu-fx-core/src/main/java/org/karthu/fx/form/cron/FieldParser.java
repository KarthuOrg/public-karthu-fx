package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses one field of a cron expression, or finds the first rule it breaks. Within a field the rules are checked in
 * this order: first the characters the format refuses in it, left to right; then the field's form (Quartz's
 * {@code ?} and special forms, which stand alone); then its entries from left to right, each first as a whole (a
 * value, a range or a step) and then its values.
 */
final class FieldParser {

    /** Above every value of every field; a longer number is cut down to it, so that it never overflows. */
    private static final int TOO_BIG = 1_000_000;

    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT",
            "NOV", "DEC"};

    private final CronFieldKind kind;
    private final CronFormat format;
    private final String text;

    private FieldParser(CronFieldKind kind, CronFormat format, String text) {
        this.kind = kind;
        this.format = format;
        this.text = text;
    }

    /**
     * Parses a field.
     *
     * @param kind   which field
     * @param format the expression's format
     * @param text   the field's text, not empty and without whitespace
     * @return the field, parsed
     * @throws BrokenRule the first rule the field breaks
     */
    static CronField parse(CronFieldKind kind, CronFormat format, String text) throws BrokenRule {
        return new FieldParser(kind, format, text).parse();
    }

    private CronField parse() throws BrokenRule {
        refuseCharacters();
        CronPart form = quartzForm();
        if (form != null) {
            return new CronField(kind, text, List.of(form));
        }
        List<CronPart> parts = new ArrayList<>();
        // Quartz's CronExpression skips an empty entry (its tokenizer drops it), cronie refuses it; a list here is one
        // of values, ranges and steps, so an empty entry is refused in both formats.
        String[] entries = text.split(",", -1);
        for (String entry : entries) {
            if (entry.isEmpty()) {
                throw new BrokenRule(kind.label() + " list has an empty entry, found " + text);
            }
            parts.add(entry(entry));
        }
        return new CronField(kind, text, parts);
    }

    // --- Characters the format refuses -------------------------------------------------------------

    /**
     * Refuses, left to right, the first character the format does not allow in this field. Unix allows the digits,
     * names and {@code * , - /} only, so {@code ?}, {@code #}, {@code L} and {@code W} are refused with the format's
     * name: they hint at a Quartz expression. Quartz allows {@code ?} and {@code L} in day of month and weekday,
     * {@code W} in day of month and {@code #} in weekday, and refuses them elsewhere with the field's name. Any other
     * character is refused with the format's name.
     *
     * <p>{@code L} and {@code W} are letters, so they are found as words: a run of letters made of {@code L} and
     * {@code W} only, in any case ({@code L}, {@code LW}, {@code w}), is the special form, any other run a name
     * ({@code JUL} and {@code WED} contain them too).
     */
    private void refuseCharacters() throws BrokenRule {
        int i = 0;
        while (i < text.length()) {
            char character = text.charAt(i);
            if (Character.isLetter(character)) {
                int end = endOfWord(i);
                String word = text.substring(i, end).toUpperCase(Locale.ROOT);
                if (isSpecialWord(word)) {
                    refuseSpecialLetters(word);
                }
                i = end;
            } else if (isAsciiDigit(character) || character == '*' || character == ',' || character == '-'
                    || character == '/') {
                i++;
            } else if (character == '?') {
                refuseUnlessQuartzIn("?", CronFieldKind.DAY_OF_MONTH, CronFieldKind.WEEKDAY);
                i++;
            } else if (character == '#') {
                refuseUnlessQuartzIn("#", CronFieldKind.WEEKDAY);
                i++;
            } else {
                int codePoint = text.codePointAt(i);
                throw new BrokenRule(format.displayName() + " cron does not allow " + Character.toString(codePoint));
            }
        }
    }

    /** Whether a word in upper case is made of {@code L} and {@code W} only: a special form, not a name. */
    private static boolean isSpecialWord(String word) {
        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);
            if (letter != 'L' && letter != 'W') {
                return false;
            }
        }
        return true;
    }

    /** Refuses the first letter of a special word the format does not allow in this field. */
    private void refuseSpecialLetters(String word) throws BrokenRule {
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == 'L') {
                refuseUnlessQuartzIn("L", CronFieldKind.DAY_OF_MONTH, CronFieldKind.WEEKDAY);
            } else {
                refuseUnlessQuartzIn("W", CronFieldKind.DAY_OF_MONTH);
            }
        }
    }

    /** Refuses a special character unless the format is Quartz and the field one of those that allow it. */
    private void refuseUnlessQuartzIn(String special, CronFieldKind... allowedIn) throws BrokenRule {
        if (format == CronFormat.UNIX) {
            throw new BrokenRule("Unix cron does not allow " + special);
        }
        if (!List.of(allowedIn).contains(kind)) {
            throw new BrokenRule(kind.label() + " does not allow " + special);
        }
    }

    // --- Quartz's ? and special forms, each the field's only entry -----------------------------------

    /**
     * Returns the field's {@code ?} or special form, or null when it has none and is a list of entries. The characters
     * are checked already: {@code ?}, {@code L}, {@code W} and {@code #} occur only in Quartz and only in the fields
     * that allow them.
     *
     * <p>Quartz's CronExpression refuses {@code L} and {@code #} in a list, its documentation allows {@code W} with
     * a single day only and {@code ?} only alone, so each stands alone. Quartz's CronExpression also takes forms its
     * documentation does not describe ({@code L-3W}, an empty list entry, {@code /5}); the documentation decides, so
     * they are refused.
     */
    private CronPart quartzForm() throws BrokenRule {
        if (text.indexOf('?') >= 0) {
            if (!text.equals("?")) {
                throw new BrokenRule("? must stand alone in " + lowerLabel() + ", found " + text);
            }
            return new CronPart.NoSpecificValue();
        }
        if (format != CronFormat.QUARTZ) {
            return null;
        }
        if (kind == CronFieldKind.DAY_OF_MONTH) {
            return dayOfMonthForm();
        }
        if (kind == CronFieldKind.WEEKDAY) {
            return weekdayForm();
        }
        return null;
    }

    /** The day-of-month forms {@code L}, {@code L-n}, {@code LW} and {@code nW}, or null. */
    private CronPart dayOfMonthForm() throws BrokenRule {
        String upper = text.toUpperCase(Locale.ROOT);
        if (hasSpecialWordWith('L')) {
            if (upper.equals("L")) {
                return new CronPart.LastDayOfMonth(0);
            }
            if (upper.equals("LW")) {
                return new CronPart.LastWorkdayOfMonth();
            }
            String daysText = upper.substring(Math.min(2, upper.length()));
            if (upper.startsWith("L-") && isDigits(daysText)) {
                // Quartz's CronExpression: "Offset from last day must be <= 30".
                int daysBefore = number(daysText);
                if (daysBefore > 30) {
                    throw new BrokenRule("Days before the last day must be 0 to 30, found " + daysText);
                }
                return new CronPart.LastDayOfMonth(daysBefore);
            }
            if (text.indexOf(',') >= 0) {
                throw new BrokenRule("L must stand alone in day of month, found " + text);
            }
            throw invalid(text);
        }
        if (hasSpecialWordWith('W')) {
            String dayText = text.substring(0, text.length() - 1);
            if (upper.endsWith("W") && isDigits(dayText)) {
                return new CronPart.NearestWorkday(value(dayText, text));
            }
            throw new BrokenRule("W needs a single day of month, found " + text);
        }
        return null;
    }

    /**
     * The weekday forms {@code L}, {@code nL} and {@code n#k}, or null. Quartz's CronExpression takes a weekday name
     * for n too ({@code FRIL}, {@code FRI#3}), as it does everywhere else, and so do the rules.
     */
    private CronPart weekdayForm() throws BrokenRule {
        String upper = text.toUpperCase(Locale.ROOT);
        if (hasSpecialWordWith('L') || hasWeekdayNameWithL()) {
            if (upper.equals("L")) {
                return new CronPart.LastDayOfWeek();
            }
            String dayText = text.substring(0, text.length() - 1);
            if (upper.endsWith("L") && (isDigits(dayText) || Weekdays.ofName(dayText) >= 0)) {
                return new CronPart.LastDayOfWeekInMonth(value(dayText, text));
            }
            if (text.indexOf(',') >= 0) {
                throw new BrokenRule("L must stand alone in weekday, found " + text);
            }
            throw invalid(text);
        }
        int hash = text.indexOf('#');
        if (hash >= 0) {
            if (text.indexOf(',') >= 0) {
                throw new BrokenRule("# must stand alone in weekday, found " + text);
            }
            String dayText = text.substring(0, hash);
            String occurrenceText = text.substring(hash + 1);
            boolean dayWritten = isDigits(dayText) || isLetters(dayText);
            if (!dayWritten || !isDigits(occurrenceText)) {
                throw invalid(text);
            }
            int dayOfWeek = value(dayText, text);
            int occurrence = number(occurrenceText);
            if (occurrence < 1 || occurrence > 5) {
                throw new BrokenRule("Weekday occurrence must be 1 to 5, found " + occurrenceText);
            }
            return new CronPart.NthDayOfWeekInMonth(dayOfWeek, occurrence);
        }
        return null;
    }

    /** Whether the field has a special word ({@code L}, {@code W}, {@code LW} in any case) with the letter. */
    private boolean hasSpecialWordWith(char letter) {
        for (String word : words()) {
            if (isSpecialWord(word) && word.indexOf(letter) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** Whether the field has a weekday name with an {@code L} appended, {@code FRIL} in any case. */
    private boolean hasWeekdayNameWithL() {
        for (String word : words()) {
            if (word.length() == 4 && word.endsWith("L") && Weekdays.ofName(word.substring(0, 3)) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** The runs of letters of the field, in upper case. */
    private List<String> words() {
        List<String> words = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            if (Character.isLetter(text.charAt(i))) {
                int end = endOfWord(i);
                words.add(text.substring(i, end).toUpperCase(Locale.ROOT));
                i = end;
            } else {
                i++;
            }
        }
        return words;
    }

    /** The end of the run of letters that starts at a letter of the field: the index after its last letter. */
    private int endOfWord(int start) {
        int end = start;
        while (end < text.length() && Character.isLetter(text.charAt(end))) {
            end++;
        }
        return end;
    }

    // --- Entries: a value, a range or a step --------------------------------------------------------

    private CronPart entry(String entry) throws BrokenRule {
        int slash = entry.indexOf('/');
        if (slash < 0) {
            return stepStart(entry, entry);
        }
        // A step is x/n with x being *, a value or a range: Quartz's bare "/n" is not documented, so a step without a
        // start is invalid.
        String startText = entry.substring(0, slash);
        String stepText = entry.substring(slash + 1);
        if (startText.isEmpty() || !isDigits(stepText)) {
            throw invalid(entry);
        }
        CronPart start = stepStart(startText, entry);
        return new CronPart.Step(start, step(stepText));
    }

    /** What a step goes through, or an entry without step: {@code *}, a range or a value. */
    private CronPart stepStart(String start, String entry) throws BrokenRule {
        if (start.equals("*")) {
            return new CronPart.All();
        }
        if (start.indexOf('-') >= 0) {
            return range(start, entry);
        }
        return new CronPart.Value(value(start, entry));
    }

    /**
     * A step, at least 1 and at most the field's highest value: Quartz's CronExpression refuses a larger increment
     * (above 59 for second and minute, 23 for the hour, 31, 12 and 7), and the rules ask the same of Unix, where a step
     * beyond the field could only ever hit its start. The year has no such limit in Quartz; its highest value, 2099, is
     * taken as the limit.
     */
    private int step(String stepText) throws BrokenRule {
        int step = number(stepText);
        if (step < 1) {
            throw new BrokenRule(kind.label() + " step must be at least 1, found " + stepText);
        }
        int highest = kind.highest();
        if (step > highest) {
            throw new BrokenRule(kind.label() + " step must be at most " + highest + ", found " + stepText);
        }
        return step;
    }

    /**
     * A range {@code a-b}. A start above the end: cronie refuses it (its entry parser stops when the first number is
     * above the second), Quartz's CronExpression wraps around the field's end in every field but the year, which it
     * refuses.
     */
    private CronPart.Range range(String range, String entry) throws BrokenRule {
        int dash = range.indexOf('-');
        String startText = range.substring(0, dash);
        String endText = range.substring(dash + 1);
        int start = value(startText, entry);
        int end = value(endText, entry);
        boolean wrapAllowed = format == CronFormat.QUARTZ && kind != CronFieldKind.YEAR;
        if (start > end && !wrapAllowed) {
            throw new BrokenRule(kind.label() + " range must not start after its end, found " + range);
        }
        return new CronPart.Range(start, end);
    }

    /** A single value: a number in the field's range, or a name of a month or weekday. */
    private int value(String value, String entry) throws BrokenRule {
        if (isDigits(value)) {
            int number = number(value);
            int lowest = kind.lowest(format);
            int highest = kind.highest();
            if (number < lowest || number > highest) {
                throw new BrokenRule(kind.label() + " must be " + lowest + " to " + highest + ", found " + value);
            }
            return number;
        }
        if (isLetters(value) && kind == CronFieldKind.MONTH) {
            int index = indexOf(MONTHS, value);
            if (index < 0) {
                throw new BrokenRule("Unknown month " + value);
            }
            return index + 1;
        }
        if (isLetters(value) && kind == CronFieldKind.WEEKDAY) {
            int day = Weekdays.ofName(value);
            if (day < 0) {
                throw new BrokenRule("Unknown weekday " + value);
            }
            return Weekdays.value(day, format);
        }
        throw invalid(entry);
    }

    /** The index of a name in a list of names, ignoring case, or -1. */
    private static int indexOf(String[] names, String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** The rule broken by an entry that is none of the forms of its field. */
    private BrokenRule invalid(String entry) {
        return new BrokenRule("Invalid " + lowerLabel() + " " + entry);
    }

    /** The field's name inside a sentence: <em>day of month</em>. */
    private String lowerLabel() {
        return kind.label().toLowerCase(Locale.ROOT);
    }

    private static boolean isDigits(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!isAsciiDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLetters(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isLetter(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiDigit(char character) {
        return character >= '0' && character <= '9';
    }

    /** The number ASCII digits write, cut down to {@link #TOO_BIG} so that a long number never overflows. */
    private static int number(String digits) {
        int number = 0;
        for (int i = 0; i < digits.length(); i++) {
            number = number * 10 + (digits.charAt(i) - '0');
            if (number > TOO_BIG) {
                return TOO_BIG;
            }
        }
        return number;
    }
}
