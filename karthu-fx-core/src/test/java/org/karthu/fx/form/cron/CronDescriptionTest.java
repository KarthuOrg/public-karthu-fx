package org.karthu.fx.form.cron;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.karthu.fx.form.CronFormat;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The plain text of a valid cron expression, without JavaFX: the sentence a user reads under the field. The expected
 * sentences are written out by hand, from an example catalogue and its style.
 */
class CronDescriptionTest {

    /** The plain text of an expression, failing with the rules' message when they refuse it. */
    private static String describe(CronFormat format, String expression) {
        CronCheck check = CronRules.check(expression, format);
        if (check instanceof CronCheck.Invalid invalid) {
            fail(expression + ": " + invalid.problem());
        }
        return CronDescription.describe(((CronCheck.Valid) check).expression());
    }

    // --- The example catalogue: the acceptance of the plain text ---------------------------------------

    @ParameterizedTest(name = "valid #{0}: {1} {2}")
    @DisplayName("catalogue, valid expressions: every row gives exactly its plain text")
    @CsvSource(delimiter = '|', textBlock = """
             1 | UNIX   | '0 9 * * 1-5'              | At 09:00, Monday to Friday
             2 | QUARTZ | '0 0 9 ? * MON-FRI'        | At 09:00:00, Monday to Friday
             3 | QUARTZ | '0 0 9 ? * 2-6'            | At 09:00:00, Monday to Friday
             4 | UNIX   | '0 9 * * *'                | At 09:00, every day
             5 | UNIX   | '* * * * *'                | Every minute
             6 | QUARTZ | '* * * ? * *'              | Every second
             7 | UNIX   | '*/15 * * * *'             | Every 15 minutes
             8 | QUARTZ | '0 0/15 * * * ?'           | Every 15 minutes
             9 | QUARTZ | '0 5/15 * * * ?'           | Every 15 minutes, starting at minute 5
            10 | UNIX   | '0 * * * *'                | Every hour
            11 | UNIX   | '30 * * * *'               | At 30 minutes past every hour
            12 | QUARTZ | '0 0 */2 * * ?'            | Every 2 hours
            13 | UNIX   | '0 8,12,18 * * *'          | At 08:00, 12:00 and 18:00, every day
            14 | UNIX   | '*/15 9-17 * * 1-5'        | Every 15 minutes, between 09:00 and 17:59, Monday to Friday
            15 | UNIX   | '0 9-17 * * *'             | Every hour, between 09:00 and 17:00
            16 | UNIX   | '0 0 1 * *'                | At 00:00, on day 1 of the month
            17 | UNIX   | '0 0 1,15 * *'             | At 00:00, on days 1 and 15 of the month
            18 | UNIX   | '0 0 1 1 *'                | At 00:00, on day 1 of the month, in January
            19 | UNIX   | '0 0 1 */3 *'              | At 00:00, on day 1 of the month, every 3 months
            20 | UNIX   | '0 0 1 * 1'                | At 00:00, on day 1 of the month or on Monday
            21 | UNIX   | '0 0 * * 0'                | At 00:00, on Sunday
            22 | UNIX   | '0 0 * * 7'                | At 00:00, on Sunday
            23 | UNIX   | '0  0 * *  sat,sun'        | At 00:00, on Saturday and Sunday
            24 | UNIX   | '@daily'                   | At 00:00, every day
            25 | UNIX   | '@hourly'                  | Every hour
            26 | UNIX   | '@weekly'                  | At 00:00, on Sunday
            27 | UNIX   | '@yearly'                  | At 00:00, on day 1 of the month, in January
            28 | QUARTZ | '0 0 0 L * ?'              | At 00:00:00, on the last day of the month
            29 | QUARTZ | '0 0 0 L-3 * ?'            | At 00:00:00, 3 days before the last day of the month
            30 | QUARTZ | '0 0 9 LW * ?'             | At 09:00:00, on the last weekday of the month
            31 | QUARTZ | '0 0 9 15W * ?'            | At 09:00:00, on the weekday nearest day 15 of the month
            32 | QUARTZ | '0 0 9 ? * 6L'             | At 09:00:00, on the last Friday of the month
            33 | QUARTZ | '0 0 9 ? * 6#3'            | At 09:00:00, on the third Friday of the month
            34 | QUARTZ | '0 0 0 30 2 ?'             | At 00:00:00, on day 30 of the month, in February
            35 | QUARTZ | '0 0 12 * * ? 2027'        | At 12:00:00, every day, in 2027
            36 | QUARTZ | '5 0-10 8,12 * 1,7 ? 2027' | At second 5, minutes 0 to 10, hours 08 and 12, in January and July, in 2027
            """)
    void catalogueValid(int row, CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Time: fixed times ----------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("single values are a fixed time, a list in one field with single values in the others several")
    @CsvSource(delimiter = '|', textBlock = """
            QUARTZ | '30 15 9 * * ?'      | At 09:15:30, every day
            UNIX   | '0,30 9 * * *'       | At 09:00 and 09:30, every day
            QUARTZ | '0,30 0 9 * * ?'     | At 09:00:00 and 09:00:30, every day
            QUARTZ | '0 0 8,12,18 * * ?'  | At 08:00:00, 12:00:00 and 18:00:00, every day
            UNIX   | '0 18,8 * * *'       | At 18:00 and 08:00, every day
            UNIX   | '0 8,8 * * *'        | At 08:00, every day
            UNIX   | '0 0 * * *'          | At 00:00, every day
            UNIX   | '0,30 9,17 * * *'    | At minutes 0 and 30, hours 09 and 17
            """)
    void fixedTimes(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Time: every unit, and an offset past it -------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("a free field with single values below it is every unit, past it by their offset when not 0; a list too")
    @CsvSource(delimiter = '|', textBlock = """
            QUARTZ | '0 * * * * ?'        | Every minute
            QUARTZ | '30 * * * * ?'       | At 30 seconds past every minute
            QUARTZ | '1 * * * * ?'        | At 1 second past every minute
            QUARTZ | '0 0 * * * ?'        | Every hour
            QUARTZ | '0 30 * * * ?'       | At 30 minutes past every hour
            QUARTZ | '15 30 * * * ?'      | At 30 minutes and 15 seconds past every hour
            QUARTZ | '15 0 * * * ?'       | At 15 seconds past every hour
            UNIX   | '1 * * * *'          | At 1 minute past every hour
            UNIX   | '5-5 * * * *'        | At 5 minutes past every hour
            UNIX   | '*,5 * * * *'        | Every minute
            UNIX   | '0,30 * * * *'       | At 0 and 30 minutes past every hour
            UNIX   | '15,30,45 * * * *'   | At 15, 30 and 45 minutes past every hour
            QUARTZ | '0,30 * * * * ?'     | At 0 and 30 seconds past every minute
            QUARTZ | '0 0,30 * * * ?'     | At 0 and 30 minutes past every hour
            QUARTZ | '15 0,30 * * * ?'    | At second 15, minutes 0 and 30
            UNIX   | '0-10,30 * * * *'    | At minutes 0 to 10 and 30
            UNIX   | '*/1 * * * *'        | Every minute
            QUARTZ | '0 0/1 * * * ?'      | Every minute
            """)
    void everyUnit(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Time: steps and ranges ------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("a step or a range of the smallest recurring field, every field above free")
    @CsvSource(delimiter = '|', textBlock = """
            QUARTZ | '*/30 * * * * ?'     | Every 30 seconds
            QUARTZ | '5/15 * * * * ?'     | Every 15 seconds, starting at second 5
            UNIX   | '5/1 * * * *'        | Every minute, starting at minute 5
            UNIX   | '10-40/5 * * * *'    | Every 5 minutes, from minute 10 to 40
            QUARTZ | '10-40/5 * * * * ?'  | Every 5 seconds, from second 10 to 40
            UNIX   | '0-10 * * * *'       | Every minute, from minute 0 to 10
            QUARTZ | '0-10 * * * * ?'     | Every second, from second 0 to 10
            UNIX   | '0 */2 * * *'        | Every 2 hours
            UNIX   | '30 */2 * * *'       | Every 2 hours, starting at 00:30
            UNIX   | '0 5/2 * * *'        | Every 2 hours, starting at 05:00
            QUARTZ | '15 30 5/2 * * ?'    | Every 2 hours, starting at 05:30:15
            UNIX   | '0 9-17/2 * * *'     | Every 2 hours, between 09:00 and 17:00
            UNIX   | '30 9-17 * * *'      | Every hour, between 09:30 and 17:30
            UNIX   | '0 5/1 * * *'        | Every hour, starting at 05:00
            """)
    void stepsAndRanges(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("a single hour or a range of hours narrows a recurring time below the hour")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '* 9-17 * * *'       | Every minute, between 09:00 and 17:59
            UNIX   | '*/15 9 * * *'       | Every 15 minutes, between 09:00 and 09:59
            QUARTZ | '0 */15 9-17 * * ?'  | Every 15 minutes, between 09:00:00 and 17:59:00
            QUARTZ | '* * 9-17 ? * *'     | Every second, between 09:00:00 and 17:59:59
            QUARTZ | '*/10 * 9 * * ?'     | Every 10 seconds, between 09:00:00 and 09:59:59
            QUARTZ | '30 * 9-17 * * ?'    | At 30 seconds past every minute, between 09:00:30 and 17:59:30
            UNIX   | '5/15 9-17 * * *'    | Every 15 minutes, starting at minute 5, between 09:05 and 17:59
            UNIX   | '10-40/5 9-17 * * *' | Every 5 minutes, from minute 10 to 40, between 09:10 and 17:40
            QUARTZ | '* 30 9 * * ?'       | Every second, between 09:30:00 and 09:30:59
            """)
    void narrowedByTheHour(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Time: the enumeration per field ---------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("any other time names each restricting field, smallest first, a free field left out")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '* 8,12 * * *'          | Every minute, hours 08 and 12
            UNIX   | '*/15 8,12 * * *'       | Every 15 minutes, hours 08 and 12
            UNIX   | '*/15 */2 * * *'        | Every 15 minutes, every 2 hours
            UNIX   | '0-10 8,12 * * *'       | At minutes 0 to 10, hours 08 and 12
            QUARTZ | '* 5 * * * ?'           | Every second, minute 5
            QUARTZ | '* 0-10 8,12 * * ?'     | Every second, minutes 0 to 10, hours 08 and 12
            QUARTZ | '*/10 0-10 8,12 * * ?'  | Every 10 seconds, minutes 0 to 10, hours 08 and 12
            QUARTZ | '30 */15 * * * ?'       | At second 30, every 15 minutes
            QUARTZ | '30 5/15 * * * ?'       | At second 30, every 15 minutes from minute 5
            QUARTZ | '30 0-10 * * * ?'       | At second 30, minutes 0 to 10
            QUARTZ | '5 0-10 8 * * ?'        | At second 5, minutes 0 to 10, hour 08
            UNIX   | '1,5-10,*/20 * * * *'   | At minutes 1 and 5 to 10 and every 20 minutes
            UNIX   | '0 9-11,14 * * *'       | At minute 0, hours 09 to 11 and 14
            UNIX   | '0 12,*/3 * * *'        | At minute 0, hour 12 and every 3 hours
            UNIX   | '0,30 8-16/2 * * *'     | At minutes 0 and 30, every 2 hours from hour 08 to 16
            UNIX   | '0,30 5/2 * * *'        | At minutes 0 and 30, every 2 hours from hour 05
            QUARTZ | '5/10 0-10 8 * * ?'     | Every 10 seconds, starting at second 5, minutes 0 to 10, hour 08
            QUARTZ | '0 0,30 9-17/2 * * ?'   | At second 0, minutes 0 and 30, every 2 hours from hour 09 to 17
            """)
    void enumeration(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("a free or stepping smallest recurring field leads the enumeration in its own words")
    @CsvSource(delimiter = '|', textBlock = """
            QUARTZ | '0 * 12,*/3 * * ?'      | Every minute, hour 12 and every 3 hours
            QUARTZ | '5 * */2 * * ?'         | At 5 seconds past every minute, every 2 hours
            QUARTZ | '0 */15 */2 * * ?'      | Every 15 minutes, every 2 hours
            UNIX   | '5/15 8,12 * * *'       | Every 15 minutes, starting at minute 5, hours 08 and 12
            UNIX   | '10-40/5 8,12 * * *'    | Every 5 minutes, from minute 10 to 40, hours 08 and 12
            """)
    void enumerationLedByARecurringField(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Time: Quartz ranges that wrap round the end of the field --------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("a Quartz range wrapping round the end of its field reads as written, a window across it")
    @CsvSource(delimiter = '|', textBlock = """
            QUARTZ | '0 0 22-2 * * ?'        | Every hour, between 22:00:00 and 02:00:00
            QUARTZ | '0 */15 22-2 * * ?'     | Every 15 minutes, between 22:00:00 and 02:59:00
            QUARTZ | '0 50-10 * * * ?'       | Every minute, from minute 50 to 10
            QUARTZ | '0 50-10 9-17 * * ?'    | Every minute, from minute 50 to 10, between 09:00:00 and 17:59:00
            QUARTZ | '0 0 22-2/2 * * ?'      | Every 2 hours, between 22:00:00 and 02:00:00
            """)
    void quartzWrappingTimes(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Day: the day of month -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("the day of month: values, ranges and steps, and Quartz's L, L-n, LW and nW")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 0 1-7 * *'         | At 00:00, on days 1 to 7 of the month
            UNIX   | '0 0 1,10-15 * *'     | At 00:00, on days 1 and 10 to 15 of the month
            UNIX   | '0 0 */2 * *'         | At 00:00, every 2 days
            UNIX   | '0 0 1/2 * *'         | At 00:00, every 2 days
            UNIX   | '0 0 5/10 * *'        | At 00:00, every 10 days from day 5 of the month
            UNIX   | '0 0 1-15/2 * *'      | At 00:00, every 2 days from day 1 to 15 of the month
            UNIX   | '0 0 1,*/10 * *'      | At 00:00, on day 1 of the month and every 10 days
            UNIX   | '0 0 */1 * *'         | At 00:00, every day
            QUARTZ | '0 0 9 25-5 * ?'      | At 09:00:00, on days 25 to 5 of the month
            QUARTZ | '0 0 0 L-1 * ?'       | At 00:00:00, 1 day before the last day of the month
            QUARTZ | '0 0 0 L-0 * ?'       | At 00:00:00, on the last day of the month
            QUARTZ | '0 0 9 1W * ?'        | At 09:00:00, on the weekday nearest day 1 of the month
            QUARTZ | '0 0 9 lw * ?'        | At 09:00:00, on the last weekday of the month
            QUARTZ | '0 */15 9-17 L * ?'   | Every 15 minutes, between 09:00:00 and 17:59:00, on the last day of the month
            """)
    void dayOfMonth(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Day: the weekday ------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("the weekday: names for values, ranges and steps, and Quartz's L, nL and n#k")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 0 * * 1,3-5'       | At 00:00, on Monday and Wednesday to Friday
            UNIX   | '0 0 * * 1-2,4-5'     | At 00:00, Monday to Tuesday and Thursday to Friday
            UNIX   | '0 0 * * 5-7'         | At 00:00, Friday to Sunday
            UNIX   | '0 0 * * 0-7'         | At 00:00, Sunday to Saturday
            UNIX   | '0 0 * * 0,7'         | At 00:00, on Sunday
            UNIX   | '0 0 * * 7-7'         | At 00:00, on Sunday
            UNIX   | '0 0 * * MON-5'       | At 00:00, Monday to Friday
            UNIX   | '0 0 * * */2'         | At 00:00, on Sunday, Tuesday, Thursday and Saturday
            UNIX   | '0 0 * * 1-7/2'       | At 00:00, on Monday, Wednesday, Friday and Sunday
            UNIX   | '0 0 * * */7'         | At 00:00, on Sunday
            UNIX   | '0 0 * * 1,*/3'       | At 00:00, on Monday, Sunday, Wednesday and Saturday
            QUARTZ | '0 0 0 ? * */2'       | At 00:00:00, on Sunday, Tuesday, Thursday and Saturday
            QUARTZ | '0 0 0 ? * 2/2'       | At 00:00:00, on Monday, Wednesday and Friday
            QUARTZ | '0 0 0 ? * FRI-MON/2' | At 00:00:00, on Friday and Sunday
            QUARTZ | '0 0 9 ? * FRI-MON'   | At 09:00:00, Friday to Monday
            QUARTZ | '0 0 9 ? * 1,7'       | At 09:00:00, on Sunday and Saturday
            QUARTZ | '0 0 0 ? * L'         | At 00:00:00, on Saturday
            QUARTZ | '0 0 0 ? * FRIL'      | At 00:00:00, on the last Friday of the month
            QUARTZ | '0 0 0 ? * 1#1'       | At 00:00:00, on the first Sunday of the month
            QUARTZ | '0 0 0 ? * 2#2'       | At 00:00:00, on the second Monday of the month
            QUARTZ | '0 0 0 ? * MON#4'     | At 00:00:00, on the fourth Monday of the month
            QUARTZ | '0 0 0 ? * 7#5'       | At 00:00:00, on the fifth Saturday of the month
            """)
    void weekday(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Day: Unix's day of month and weekday together -------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("Unix: both restricted is either day, a field starting with * makes it both, as cronie decides")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 0 1 * 1-5'         | At 00:00, on day 1 of the month or Monday to Friday
            UNIX   | '0 0 1,15 * sat,sun'  | At 00:00, on days 1 and 15 of the month or on Saturday and Sunday
            UNIX   | '0 0 1-31/2 * 1'      | At 00:00, every 2 days from day 1 to 31 of the month or on Monday
            UNIX   | '0 0 */2 * 1'         | At 00:00, every 2 days, on Monday
            UNIX   | '0 0 1 * */2'         | At 00:00, on day 1 of the month, on Sunday, Tuesday, Thursday and Saturday
            UNIX   | '0 0 *,5 * 1'         | At 00:00, on Monday
            UNIX   | '0 0 5,* * 1'         | At 00:00, every day
            UNIX   | '0 0 1 * 5,*'         | At 00:00, every day
            UNIX   | '0 0 */1 * 1'         | At 00:00, on Monday
            """)
    void unixEitherDay(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Month and year ----------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("the month and the year: values, ranges and steps")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 0 1 1-3 *'                 | At 00:00, on day 1 of the month, January to March
            UNIX   | '0 0 1 1,3-5 *'               | At 00:00, on day 1 of the month, in January and March to May
            UNIX   | '0 0 1 1-3,7-9 *'             | At 00:00, on day 1 of the month, January to March and July to September
            UNIX   | '0 0 1 1/3 *'                 | At 00:00, on day 1 of the month, every 3 months
            UNIX   | '0 0 1 2/3 *'                 | At 00:00, on day 1 of the month, every 3 months from February
            UNIX   | '0 0 1 jan-jun/2 *'           | At 00:00, on day 1 of the month, every 2 months from January to June
            UNIX   | '0 0 1 1,*/4 *'               | At 00:00, on day 1 of the month, in January and every 4 months
            QUARTZ | '0 0 9 * NOV-FEB ?'           | At 09:00:00, every day, November to February
            QUARTZ | '0 0 9 * * ? *'               | At 09:00:00, every day
            QUARTZ | '0 0 9 * * ? 2027,2029'       | At 09:00:00, every day, in 2027 and 2029
            QUARTZ | '0 0 9 * * ? 2027-2029'       | At 09:00:00, every day, 2027 to 2029
            QUARTZ | '0 0 9 * * ? */2'             | At 09:00:00, every day, every 2 years
            QUARTZ | '0 0 9 * * ? 1970/2'          | At 09:00:00, every day, every 2 years
            QUARTZ | '0 0 9 * * ? 2027/2'          | At 09:00:00, every day, every 2 years from 2027
            QUARTZ | '0 0 9 * * ? 2027-2035/2'     | At 09:00:00, every day, every 2 years from 2027 to 2035
            QUARTZ | '0 0 9 * * ? 2027,2030-2032'  | At 09:00:00, every day, in 2027 and 2030 to 2032
            """)
    void monthAndYear(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Macros --------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("a macro is described as the expression it stands for, in any case")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '@annually'   | At 00:00, on day 1 of the month, in January
            UNIX   | '@monthly'    | At 00:00, on day 1 of the month
            UNIX   | '@midnight'   | At 00:00, every day
            UNIX   | '@HOURLY'     | Every hour
            UNIX   | ' @Daily '    | At 00:00, every day
            """)
    void macros(CronFormat format, String expression, String plainText) {
        assertEquals(plainText, describe(format, expression));
    }

    // --- Never valid without content -------------------------------------------------------------------

    private static final List<String> SECONDS_AND_MINUTES = List.of("*", "0", "5", "0,30", "0-10", "5-5", "*/15",
            "5/15", "10-40/5", "1,5-10,*/20", "*,5", "*/1", "50-10", "50-10/5");

    private static final List<String> HOURS = List.of("*", "0", "9", "8,12", "9-17", "*/2", "5/2", "9-17/2", "22-2",
            "12,*/3", "9-11,14");

    private static final List<String> DAYS_OF_MONTH = List.of("*", "?", "1", "1,15", "1-7", "*/2", "5/10", "1-15/2",
            "1,*/10", "25-5", "L", "L-3", "LW", "15W");

    private static final List<String> MONTHS = List.of("*", "1", "JAN,JUL", "1-3", "*/3", "2/3", "jan-jun/2",
            "NOV-FEB");

    private static final List<String> WEEKDAYS = List.of("*", "?", "0", "7", "1", "1-5", "MON-FRI", "sat,sun",
            "0-7", "*/2", "1,*/3", "5-7", "FRI-MON", "FRI-MON/2", "L", "6L", "6#3");

    private static final List<String> YEARS = List.of("", "*", "2027", "2027,2029", "2027-2029", "*/2", "2027/2",
            "2027-2035/2");

    @Test
    @DisplayName("every valid combination of many field forms gets a sentence, in both formats")
    void everyValidExpressionHasASentence() {
        int described = 0;
        for (String second : SECONDS_AND_MINUTES) {
            for (String minute : SECONDS_AND_MINUTES) {
                for (String hour : HOURS) {
                    described = described + checkSentence(CronFormat.UNIX, minute + " " + hour + " * * *");
                    described = described + checkSentence(CronFormat.QUARTZ,
                            second + " " + minute + " " + hour + " * * ?");
                }
            }
        }
        for (String dayOfMonth : DAYS_OF_MONTH) {
            for (String month : MONTHS) {
                for (String weekday : WEEKDAYS) {
                    described = described + checkSentence(CronFormat.UNIX,
                            "0 9 " + dayOfMonth + " " + month + " " + weekday);
                    for (String year : YEARS) {
                        described = described + checkSentence(CronFormat.QUARTZ,
                                "0 0 9 " + dayOfMonth + " " + month + " " + weekday + " " + year);
                    }
                }
            }
        }
        assertTrue(described > 5000, "only " + described + " valid expressions described");
    }

    /** Checks the sentence of an expression the rules accept, and returns 1; 0 for one they refuse. */
    private static int checkSentence(CronFormat format, String expression) {
        CronCheck check = CronRules.check(expression, format);
        if (!(check instanceof CronCheck.Valid valid)) {
            return 0;
        }
        String sentence = CronDescription.describe(valid.expression());
        String what = format + " " + expression + ": " + sentence;
        assertFalse(sentence.isBlank(), what);
        assertTrue(Character.isUpperCase(sentence.charAt(0)), what);
        assertFalse(sentence.endsWith(".") || sentence.endsWith(",") || sentence.endsWith(" "), what);
        assertFalse(sentence.contains(", ,") || sentence.contains("  ") || sentence.contains("null"), what);
        return 1;
    }
}
