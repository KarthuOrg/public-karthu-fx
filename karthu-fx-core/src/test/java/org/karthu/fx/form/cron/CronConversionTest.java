package org.karthu.fx.form.cron;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.karthu.fx.form.CronFormat;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The conversion of a valid cron expression into the other format, without JavaFX: the text a format change puts into
 * the field, or <i>not convertible</i> when the expression cannot be written in the other format without loss. The
 * expected texts are written out by hand, from an example catalogue and its style; that a conversion means the same
 * schedule is read by the tests' own {@link CronMeaning}.
 */
class CronConversionTest {

    private static final String NOT_CONVERTIBLE = "not convertible";

    /** The conversion of an expression the rules accept, or {@link #NOT_CONVERTIBLE}. */
    private static String convert(CronFormat format, String expression) {
        CronCheck check = CronRules.check(expression, format);
        if (check instanceof CronCheck.Invalid invalid) {
            fail(format + " " + expression + ": " + invalid.problem());
        }
        Optional<String> converted = CronConversion.convert(((CronCheck.Valid) check).expression());
        return converted.orElse(NOT_CONVERTIBLE);
    }

    /** The plain text of an expression the rules accept. */
    private static String describe(CronFormat format, String expression) {
        return CronDescription.describe(((CronCheck.Valid) CronRules.check(expression, format)).expression());
    }

    /** What an expression the rules accept means, by the tests' own reading; fails when the rules refuse it. */
    private static CronMeaning meaning(CronFormat format, String expression) {
        CronCheck check = CronRules.check(expression, format);
        if (check instanceof CronCheck.Invalid invalid) {
            fail(format + " " + expression + ": " + invalid.problem());
        }
        return CronMeaning.of(((CronCheck.Valid) check).expression());
    }

    private static CronFormat other(CronFormat format) {
        return format == CronFormat.UNIX ? CronFormat.QUARTZ : CronFormat.UNIX;
    }

    // --- The example catalogue: the acceptance of the conversion ---------------------------------------

    @ParameterizedTest(name = "conversion #{0}: {1} {2}")
    @DisplayName("catalogue, conversions: every row gives exactly its result")
    @CsvSource(delimiter = '|', textBlock = """
             1 | UNIX   | '0 9 * * 1-5'       | '0 0 9 ? * MON-FRI'
             2 | QUARTZ | '0 0 9 ? * MON-FRI' | '0 9 * * MON-FRI'
             3 | UNIX   | '*/5 * * * *'       | '0 */5 * * * ?'
             4 | UNIX   | '0 0 * * *'         | '0 0 0 * * ?'
             5 | UNIX   | '0 0 1 * *'         | '0 0 0 1 * ?'
             6 | UNIX   | '0 0 * * 0'         | '0 0 0 ? * SUN'
             7 | UNIX   | '0 0 * * 7'         | '0 0 0 ? * SUN'
             8 | UNIX   | '0 0 * * 1,3-5'     | '0 0 0 ? * MON,WED-FRI'
             9 | UNIX   | '@daily'            | '0 0 0 * * ?'
            10 | QUARTZ | '0 0 12 ? * 2,4'    | '0 12 * * MON,WED'
            11 | QUARTZ | '0 0 9 * * ? *'     | '0 9 * * *'
            12 | UNIX   | '0 0 1 * 1'         | not convertible
            13 | UNIX   | '0 0 * * */2'       | not convertible
            14 | QUARTZ | '0 0 0 L * ?'       | not convertible
            15 | QUARTZ | '30 0 9 * * ?'      | not convertible
            16 | QUARTZ | '0 0 9 * * ? 2027'  | not convertible
            """)
    void catalogue(int row, CronFormat format, String expression, String result) {
        assertEquals(result, convert(format, expression));
    }

    @ParameterizedTest(name = "conversion #{0}: {1} {2}")
    @DisplayName("catalogue, conversions: every convertible row converts back to an expression of the same meaning")
    @CsvSource(delimiter = '|', textBlock = """
             1 | UNIX   | '0 9 * * 1-5'       | '0 9 * * MON-FRI'
             2 | QUARTZ | '0 0 9 ? * MON-FRI' | '0 0 9 ? * MON-FRI'
             3 | UNIX   | '*/5 * * * *'       | '*/5 * * * *'
             4 | UNIX   | '0 0 * * *'         | '0 0 * * *'
             5 | UNIX   | '0 0 1 * *'         | '0 0 1 * *'
             6 | UNIX   | '0 0 * * 0'         | '0 0 * * SUN'
             7 | UNIX   | '0 0 * * 7'         | '0 0 * * SUN'
             8 | UNIX   | '0 0 * * 1,3-5'     | '0 0 * * MON,WED-FRI'
             9 | UNIX   | '@daily'            | '0 0 * * *'
            10 | QUARTZ | '0 0 12 ? * 2,4'    | '0 0 12 ? * MON,WED'
            11 | QUARTZ | '0 0 9 * * ? *'     | '0 0 9 * * ?'
            """)
    void catalogueRoundTrip(int row, CronFormat format, String expression, String back) {
        String there = convert(format, expression);

        assertEquals(back, convert(other(format), there));
        assertEquals(meaning(format, expression), meaning(format, back));
        assertEquals(describe(format, expression), describe(format, back));
    }

    // --- Beyond the catalogue: how the result is written ----------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("the result has single spaces, the fields as written, weekdays as names in capitals, macros expanded")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0  0 * *  sat,sun'   | '0 0 0 ? * SAT,SUN'
            UNIX   | ' 0 0 1 jan * '       | '0 0 0 1 jan ?'
            QUARTZ | '0 0 9 ? jan mon-fri' | '0 9 * jan MON-FRI'
            QUARTZ | '00 0 9 * * ?'        | '0 9 * * *'
            QUARTZ | '0 0 9 ? * 7'         | '0 9 * * SAT'
            QUARTZ | '0 0 9 ? * 1-7'       | '0 9 * * SUN-SAT'
            QUARTZ | '0 0 9 ? * *'         | '0 9 * * *'
            QUARTZ | '0 0 9 ? * *,2'       | '0 9 * * *,MON'
            UNIX   | '0 0 * * 0-6'         | '0 0 0 ? * SUN-SAT'
            UNIX   | '@weekly'             | '0 0 0 ? * SUN'
            UNIX   | '@HOURLY'             | '0 0 * * * ?'
            UNIX   | '@yearly'             | '0 0 0 1 1 ?'
            """)
    void writtenAsTheyWere(CronFormat format, String expression, String result) {
        assertEquals(result, convert(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("a step after a single value stays as written in both directions")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '5/15 * * * *'   | '0 5/15 * * * ?'
            QUARTZ | '0 5/15 * * * ?' | '5/15 * * * *'
            QUARTZ | '0 0 9 1/5 * ?'  | '0 9 1/5 * *'
            UNIX   | '0 9/2 * 2/3 *'  | '0 0 9/2 * 2/3 ?'
            """)
    void stepAfterAValue(CronFormat format, String expression, String result) {
        assertEquals(result, convert(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("Unix's day as cronie reads it: the field that restricts nothing becomes ?, the weekday first")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX | '0 0 * * 1'       | '0 0 0 ? * MON'
            UNIX | '0 0 1 * *,1'     | '0 0 0 1 * ?'
            UNIX | '0 0 */1 * 1'     | '0 0 0 ? * MON'
            UNIX | '0 0 *,5 * 1-5'   | '0 0 0 ? * MON-FRI'
            UNIX | '0 9 * * mon,*'   | '0 0 9 * * ?'
            UNIX | '0 0 1/1 * MON,*' | '0 0 0 1/1 * ?'
            UNIX | '0 0 1-15 * *,1'  | '0 0 0 1-15 * ?'
            UNIX | '0 0 */2 * 1'     | not convertible
            UNIX | '0 0 5,* * 1'     | not convertible
            UNIX | '0 0 1 * MON,*'   | not convertible
            UNIX | '0 0 1-31 * 0-6'  | not convertible
            UNIX | '0 0 * * */1'     | not convertible
            """)
    void unixDays(CronFormat format, String expression, String result) {
        assertEquals(result, convert(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("a weekday range converts only when its names run the same way in the other format")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 0 * * 5-7'       | not convertible
            UNIX   | '0 0 * * 1-7'       | not convertible
            UNIX   | '0 0 * * 0-7'       | not convertible
            UNIX   | '0 0 * * FRI-7'     | not convertible
            UNIX   | '0 0 * * 0-2,5'     | '0 0 0 ? * SUN-TUE,FRI'
            QUARTZ | '0 0 9 ? * FRI-MON' | not convertible
            QUARTZ | '0 0 9 ? * 7-1'     | not convertible
            QUARTZ | '0 0 9 ? * 2-6'     | '0 9 * * MON-FRI'
            """)
    void weekdayRanges(CronFormat format, String expression, String result) {
        assertEquals(result, convert(format, expression));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Quartz that Unix cannot say is not convertible: seconds, a year, L, W, #, a weekday step, a wrapping range")
    @CsvSource(delimiter = '|', textBlock = """
            '* 0 9 * * ?'
            '0,30 0 9 * * ?'
            '0/30 0 9 * * ?'
            '0 0 9 * * ? 2027-2029'
            '0 0 9 * * ? */2'
            '0 0 0 L-3 * ?'
            '0 0 9 LW * ?'
            '0 0 9 15W * ?'
            '0 0 9 ? * L'
            '0 0 9 ? * 6L'
            '0 0 9 ? * FRIL'
            '0 0 9 ? * 6#3'
            '0 0 9 ? * */2'
            '0 0 9 ? * 2/2'
            '0 0 9 ? * MON-FRI/2'
            '0 0 22-2 * * ?'
            '0 0 22-2/2 * * ?'
            '0 50-10 * * * ?'
            '0 0 0 25-5 * ?'
            '0 0 0 1 NOV-FEB ?'
            """)
    void notConvertibleToUnix(String expression) {
        assertEquals(NOT_CONVERTIBLE, convert(CronFormat.QUARTZ, expression));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("a Quartz year that restricts nothing is dropped like *")
    @CsvSource(delimiter = '|', textBlock = """
            '0 0 9 * * ? */1'
            '0 0 9 * * ? *,2027'
            '0 0 9 * * ? 1970/1'
            """)
    void yearThatRestrictsNothing(String expression) {
        assertEquals("0 9 * * *", convert(CronFormat.QUARTZ, expression));
    }

    // --- Every convertible expression: valid, the same schedule, and back again -----------------------

    private static final List<String> MINUTES = List.of("*", "0", "5", "0,30", "0-10", "*/15", "5/15", "10-40/5",
            "1,5-10,*/20", "*,5", "*/1", "50-10");

    private static final List<String> SECONDS = List.of("0", "00", "5", "*", "0,30", "0/30");

    private static final List<String> HOURS = List.of("*", "9", "8,12", "9-17", "*/2", "5/2", "9-17/2", "22-2");

    private static final List<String> DAYS_OF_MONTH = List.of("*", "?", "1", "1,15", "1-7", "*/2", "*/1", "1/1",
            "5/10", "1-15/2", "1,*/10", "*,5", "5,*", "1-31", "25-5", "L", "L-3", "LW", "15W");

    private static final List<String> MONTHS = List.of("*", "1", "jan,JUL", "1-3", "*/3", "2/3", "NOV-FEB");

    private static final List<String> WEEKDAYS = List.of("*", "?", "0", "7", "1", "1-5", "MON-FRI", "sat,sun",
            "0-6", "0-7", "5-7", "1-7", "1,3-5", "*,1", "MON,*", "*/2", "*/1", "FRI-MON", "L", "6L", "6#3");

    private static final List<String> YEARS = List.of("", "*", "2027", "*/1", "*,2027");

    @Test
    @DisplayName("every convertible expression of many field forms converts to the same schedule and back")
    void everyConversionKeepsTheSchedule() {
        int[] counts = new int[2];
        for (String minute : MINUTES) {
            for (String hour : HOURS) {
                checkConversion(CronFormat.UNIX, minute + " " + hour + " * * *", counts);
                for (String second : SECONDS) {
                    checkConversion(CronFormat.QUARTZ, second + " " + minute + " " + hour + " * * ?", counts);
                }
            }
        }
        for (String dayOfMonth : DAYS_OF_MONTH) {
            for (String month : MONTHS) {
                for (String weekday : WEEKDAYS) {
                    checkConversion(CronFormat.UNIX, "0 9 " + dayOfMonth + " " + month + " " + weekday, counts);
                    for (String year : YEARS) {
                        checkConversion(CronFormat.QUARTZ,
                                "0 0 9 " + dayOfMonth + " " + month + " " + weekday + " " + year, counts);
                    }
                }
            }
        }
        assertTrue(counts[0] > 1000, "only " + counts[0] + " expressions converted");
        assertTrue(counts[1] > 1000, "only " + counts[1] + " expressions not convertible");
    }

    /**
     * Converts an expression the rules accept and counts it, converted in {@code counts[0]}, not convertible in
     * {@code counts[1]}: a conversion is valid in the other format, means the same schedule, is written with single
     * spaces and weekday names, and converts back to the same schedule. An expression the rules refuse is skipped.
     */
    private static void checkConversion(CronFormat format, String expression, int[] counts) {
        if (!(CronRules.check(expression, format) instanceof CronCheck.Valid)) {
            return;
        }
        String there = convert(format, expression);
        if (there.equals(NOT_CONVERTIBLE)) {
            counts[1]++;
            return;
        }
        counts[0]++;
        String what = format + " " + expression + " -> " + there;
        CronFormat other = other(format);
        assertEquals(meaning(format, expression), meaning(other, there), what);
        assertEquals(there.strip(), there, what);
        assertFalse(there.contains("  "), what);
        String weekday = there.split(" ")[other == CronFormat.UNIX ? 4 : 5];
        assertFalse(weekday.matches(".*[0-9a-z].*"), what);

        String back = convert(other, there);
        assertNotEquals(NOT_CONVERTIBLE, back, what);
        assertEquals(meaning(format, expression), meaning(format, back), what + " -> " + back);
    }
}
