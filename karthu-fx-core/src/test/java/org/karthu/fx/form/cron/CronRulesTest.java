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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The cron rules without JavaFX: which text is a valid Unix or Quartz cron expression, what it is parsed into, and the
 * message of the first broken rule of an invalid one. The expected messages are written out by hand, from an example
 * catalogue and its style.
 */
class CronRulesTest {

    /** The parsed form of an expression, failing with the rules' message when they refuse it. */
    private static CronExpression expression(CronFormat format, String expression) {
        CronCheck check = CronRules.check(expression, format);
        if (check instanceof CronCheck.Invalid invalid) {
            fail(expression + ": " + invalid.problem());
        }
        return ((CronCheck.Valid) check).expression();
    }

    /** The message the rules give for an expression, failing when they accept it. */
    private static String problem(CronFormat format, String expression) {
        CronCheck check = CronRules.check(expression, format);
        CronCheck.Invalid invalid = assertInstanceOf(CronCheck.Invalid.class, check, expression);
        return invalid.problem();
    }

    // --- The example catalogue: the acceptance of the rules -------------------------------------------

    @ParameterizedTest(name = "valid #{0}: {1} {2}")
    @DisplayName("catalogue, valid expressions: every row is accepted in its format")
    @CsvSource(delimiter = '|', textBlock = """
             1 | UNIX   | '0 9 * * 1-5'
             2 | QUARTZ | '0 0 9 ? * MON-FRI'
             3 | QUARTZ | '0 0 9 ? * 2-6'
             4 | UNIX   | '0 9 * * *'
             5 | UNIX   | '* * * * *'
             6 | QUARTZ | '* * * ? * *'
             7 | UNIX   | '*/15 * * * *'
             8 | QUARTZ | '0 0/15 * * * ?'
             9 | QUARTZ | '0 5/15 * * * ?'
            10 | UNIX   | '0 * * * *'
            11 | UNIX   | '30 * * * *'
            12 | QUARTZ | '0 0 */2 * * ?'
            13 | UNIX   | '0 8,12,18 * * *'
            14 | UNIX   | '*/15 9-17 * * 1-5'
            15 | UNIX   | '0 9-17 * * *'
            16 | UNIX   | '0 0 1 * *'
            17 | UNIX   | '0 0 1,15 * *'
            18 | UNIX   | '0 0 1 1 *'
            19 | UNIX   | '0 0 1 */3 *'
            20 | UNIX   | '0 0 1 * 1'
            21 | UNIX   | '0 0 * * 0'
            22 | UNIX   | '0 0 * * 7'
            23 | UNIX   | '0  0 * *  sat,sun'
            24 | UNIX   | '@daily'
            25 | UNIX   | '@hourly'
            26 | UNIX   | '@weekly'
            27 | UNIX   | '@yearly'
            28 | QUARTZ | '0 0 0 L * ?'
            29 | QUARTZ | '0 0 0 L-3 * ?'
            30 | QUARTZ | '0 0 9 LW * ?'
            31 | QUARTZ | '0 0 9 15W * ?'
            32 | QUARTZ | '0 0 9 ? * 6L'
            33 | QUARTZ | '0 0 9 ? * 6#3'
            34 | QUARTZ | '0 0 0 30 2 ?'
            35 | QUARTZ | '0 0 12 * * ? 2027'
            36 | QUARTZ | '5 0-10 8,12 * 1,7 ? 2027'
            """)
    void catalogueValid(int row, CronFormat format, String expression) {
        assertEquals(format, expression(format, expression).format());
    }

    @ParameterizedTest(name = "invalid #{0}: {1} {2}")
    @DisplayName("catalogue, invalid expressions: every row gives exactly its message")
    @CsvSource(delimiter = '|', textBlock = """
             1 | UNIX   | '0 0 0 L * ?'         | Unix cron has 5 fields, found 6
             2 | UNIX   | '0 9 * *'             | Unix cron has 5 fields, found 4
             3 | QUARTZ | '0 9 * * 1-5'         | Quartz cron has 6 or 7 fields, found 5
             4 | QUARTZ | '0 0 9 * * MON-FRI'   | Day of month or weekday must be ?
             5 | QUARTZ | '0 0 9 ? * ?'         | Only one of day of month and weekday can be ?
             6 | UNIX   | '60 * * * *'          | Minute must be 0 to 59, found 60
             7 | UNIX   | '0 24 * * *'          | Hour must be 0 to 23, found 24
             8 | UNIX   | '0 0 32 * *'          | Day of month must be 1 to 31, found 32
             9 | UNIX   | '0 0 * 13 *'          | Month must be 1 to 12, found 13
            10 | UNIX   | '0 0 * * 8'           | Weekday must be 0 to 7, found 8
            11 | QUARTZ | '0 0 0 ? * 0'         | Weekday must be 1 to 7, found 0
            12 | QUARTZ | '0 0 0 * * ? 1969'    | Year must be 1970 to 2099, found 1969
            13 | UNIX   | '0 9 * * FRY'         | Unknown weekday FRY
            14 | UNIX   | '0 0 1 JUNE *'        | Unknown month JUNE
            15 | UNIX   | '0 0 L * *'           | Unix cron does not allow L
            16 | UNIX   | '0 9 * * ?'           | Unix cron does not allow ?
            17 | UNIX   | '@reboot'             | @reboot is not a schedule
            18 | UNIX   | '@often'              | Unknown macro @often
            19 | QUARTZ | '@daily'              | Quartz cron has no macros
            20 | UNIX   | '*/0 * * * *'         | Minute step must be at least 1, found 0
            21 | QUARTZ | '0 0 9 ? * 6#6'       | Weekday occurrence must be 1 to 5, found 6
            22 | QUARTZ | '0 0 9 1-15W * ?'     | W needs a single day of month, found 1-15W
            """)
    void catalogueInvalid(int row, CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @Test
    @DisplayName("catalogue: the valid rows are parsed as written, three of them in full")
    void catalogueParsedForms() {
        CronExpression row9 = expression(CronFormat.QUARTZ, "0 5/15 * * * ?");

        assertEquals(Optional.of(new CronField(CronFieldKind.SECOND, "0", List.of(new CronPart.Value(0)))),
                row9.second());
        assertEquals(List.of(new CronPart.Step(new CronPart.Value(5), 15)), row9.minute().parts());
        assertTrue(row9.hour().isAll());
        assertTrue(row9.dayOfMonth().isAll());
        assertTrue(row9.weekday().isNoSpecificValue());

        CronExpression row23 = expression(CronFormat.UNIX, "0  0 * *  sat,sun");

        assertEquals(new CronField(CronFieldKind.WEEKDAY, "sat,sun", List.of(new CronPart.Value(6),
                new CronPart.Value(0))), row23.weekday());

        CronExpression row36 = expression(CronFormat.QUARTZ, "5 0-10 8,12 * 1,7 ? 2027");

        assertEquals(List.of(new CronPart.Value(5)), row36.second().orElseThrow().parts());
        assertEquals(List.of(new CronPart.Range(0, 10)), row36.minute().parts());
        assertEquals(List.of(new CronPart.Value(8), new CronPart.Value(12)), row36.hour().parts());
        assertTrue(row36.dayOfMonth().isAll());
        assertEquals(List.of(new CronPart.Value(1), new CronPart.Value(7)), row36.month().parts());
        assertTrue(row36.weekday().isNoSpecificValue());
        assertEquals(Optional.of(new CronField(CronFieldKind.YEAR, "2027", List.of(new CronPart.Value(2027)))),
                row36.year());
    }

    // --- Splitting and the field count ----------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("the field count of the format comes first")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 0 0 L * ?'        | Unix cron has 5 fields, found 6
            UNIX   | '0 9 * *'            | Unix cron has 5 fields, found 4
            QUARTZ | '0 9 * * 1-5'        | Quartz cron has 6 or 7 fields, found 5
            QUARTZ | '0 0 9 ? * * 2027 1' | Quartz cron has 6 or 7 fields, found 8
            UNIX   | ''                   | Unix cron has 5 fields, found 0
            QUARTZ | '   '                | Quartz cron has 6 or 7 fields, found 0
            """)
    void fieldCount(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @Test
    @DisplayName("a text without fields, null too, is checked like any other: it has no fields")
    void nullHasNoFields() {
        assertEquals("Unix cron has 5 fields, found 0", problem(CronFormat.UNIX, null));
        assertEquals("Quartz cron has 6 or 7 fields, found 0", problem(CronFormat.QUARTZ, null));
    }

    @Test
    @DisplayName("fields are split at any run of whitespace, whitespace around the text ignored, the fields kept")
    void splitAtAnyWhitespace() {
        CronExpression unix = expression(CronFormat.UNIX, " \t0\t9  * *\t\t1-5 \n");

        assertEquals("0", unix.minute().text());
        assertEquals("9", unix.hour().text());
        assertEquals("1-5", unix.weekday().text());

        CronExpression quartz = expression(CronFormat.QUARTZ, "0\t0   9 ?\t*  MON-FRI   ");

        assertEquals("MON-FRI", quartz.weekday().text());
        assertEquals(Optional.empty(), quartz.year());
    }

    // --- Values, ranges and *: the parsed form -----------------------------------------------------

    @Test
    @DisplayName("a Unix expression is parsed into its five fields, values and ranges as numbers")
    void unixFieldsParsed() {
        CronExpression expression = expression(CronFormat.UNIX, "0 9 * * 1-5");

        assertEquals(CronFormat.UNIX, expression.format());
        assertEquals(Optional.empty(), expression.macro());
        assertEquals(Optional.empty(), expression.second());
        assertEquals(new CronField(CronFieldKind.MINUTE, "0", List.of(new CronPart.Value(0))), expression.minute());
        assertEquals(new CronField(CronFieldKind.HOUR, "9", List.of(new CronPart.Value(9))), expression.hour());
        assertEquals(new CronField(CronFieldKind.DAY_OF_MONTH, "*", List.of(new CronPart.All())),
                expression.dayOfMonth());
        assertEquals(new CronField(CronFieldKind.MONTH, "*", List.of(new CronPart.All())), expression.month());
        assertEquals(new CronField(CronFieldKind.WEEKDAY, "1-5", List.of(new CronPart.Range(1, 5))),
                expression.weekday());
        assertEquals(Optional.empty(), expression.year());
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("a value outside its field's range names the field, the range and the value as written")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '60 * * * *'         | Minute must be 0 to 59, found 60
            UNIX   | '0 24 * * *'         | Hour must be 0 to 23, found 24
            UNIX   | '0 0 32 * *'         | Day of month must be 1 to 31, found 32
            UNIX   | '0 0 0 * *'          | Day of month must be 1 to 31, found 0
            UNIX   | '0 0 * 13 *'         | Month must be 1 to 12, found 13
            UNIX   | '0 0 * 0 *'          | Month must be 1 to 12, found 0
            UNIX   | '0 0 * * 8'          | Weekday must be 0 to 7, found 8
            QUARTZ | '60 0 0 ? * 1'       | Second must be 0 to 59, found 60
            QUARTZ | '0 0 0 ? * 0'        | Weekday must be 1 to 7, found 0
            QUARTZ | '0 0 0 ? * 8'        | Weekday must be 1 to 7, found 8
            QUARTZ | '0 0 0 * * ? 1969'   | Year must be 1970 to 2099, found 1969
            QUARTZ | '0 0 0 * * ? 2100'   | Year must be 1970 to 2099, found 2100
            UNIX   | '0 060 * * *'        | Hour must be 0 to 23, found 060
            UNIX   | '0 99999999999999999999 * * *' | Hour must be 0 to 23, found 99999999999999999999
            UNIX   | '0 1-24 * * *'       | Hour must be 0 to 23, found 24
            UNIX   | '0 24-1 * * *'       | Hour must be 0 to 23, found 24
            """)
    void valueOutsideItsRange(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("every value at the ends of its field's range is accepted, leading zeros too")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '59 23 31 12 7'
            UNIX   | '0 0 1 1 0'
            UNIX   | '00 05 01 01 00'
            QUARTZ | '59 59 23 31 12 ? 2099'
            QUARTZ | '0 0 0 ? 1 1 1970'
            QUARTZ | '0 0 0 ? 1 7'
            """)
    void valuesAtTheEndsAreAccepted(CronFormat format, String expression) {
        expression(format, expression);
    }

    // --- Names and lists ----------------------------------------------------------------------------

    @Test
    @DisplayName("names of months and weekdays in any case are their numbers, also in ranges and lists")
    void namesAreNumbers() {
        CronExpression unix = expression(CronFormat.UNIX, "0 0 * jan,Mar-MAY sun,Mon-fri");

        assertEquals(List.of(new CronPart.Value(1), new CronPart.Range(3, 5)), unix.month().parts());
        assertEquals("jan,Mar-MAY", unix.month().text());
        assertEquals(List.of(new CronPart.Value(0), new CronPart.Range(1, 5)), unix.weekday().parts());

        CronExpression quartz = expression(CronFormat.QUARTZ, "0 0 0 ? DEC sun,Mon-FRI,sat");

        assertEquals(List.of(new CronPart.Value(12)), quartz.month().parts());
        assertEquals(List.of(new CronPart.Value(1), new CronPart.Range(2, 6), new CronPart.Value(7)),
                quartz.weekday().parts());
    }

    @Test
    @DisplayName("a name and a number may be mixed in a range")
    void nameAndNumberMixedInARange() {
        assertEquals(List.of(new CronPart.Range(1, 5)), expression(CronFormat.UNIX, "0 0 * * MON-5").weekday().parts());

        CronExpression quartz = expression(CronFormat.QUARTZ, "0 0 0 ? 2-JUN 2-fri");

        assertEquals(List.of(new CronPart.Range(2, 6)), quartz.month().parts());
        assertEquals(List.of(new CronPart.Range(2, 6)), quartz.weekday().parts());
    }

    @Test
    @DisplayName("Unix weekday 7 is kept as written, Sunday like 0")
    void unixSevenIsKept() {
        assertEquals(List.of(new CronPart.Value(7)), expression(CronFormat.UNIX, "0 0 * * 7").weekday().parts());
        assertEquals(List.of(new CronPart.Range(1, 7)), expression(CronFormat.UNIX, "0 0 * * 1-7").weekday().parts());
    }

    @Test
    @DisplayName("a list keeps its entries in the order written, and * may be one of them")
    void listInOrder() {
        CronExpression expression = expression(CronFormat.UNIX, "30,0,15 8-10,12 * * *");

        assertEquals(List.of(new CronPart.Value(30), new CronPart.Value(0), new CronPart.Value(15)),
                expression.minute().parts());
        assertEquals(List.of(new CronPart.Range(8, 10), new CronPart.Value(12)), expression.hour().parts());
        assertEquals(List.of(new CronPart.All(), new CronPart.Value(5)),
                expression(CronFormat.UNIX, "*,5 * * * *").minute().parts());
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("a name that is no month or weekday is unknown; only the three-letter names count")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 9 * * FRY'         | Unknown weekday FRY
            UNIX   | '0 0 1 JUNE *'        | Unknown month JUNE
            UNIX   | '0 0 * * MONDAY'      | Unknown weekday MONDAY
            UNIX   | '0 0 * jan,feb,mrz *' | Unknown month mrz
            UNIX   | '0 0 * * MON-FRY'     | Unknown weekday FRY
            QUARTZ | '0 0 0 ? * MON-FRY'   | Unknown weekday FRY
            QUARTZ | '0 0 0 ? JUNE *'      | Unknown month JUNE
            UNIX   | '0 0 * * JAN'         | Unknown weekday JAN
            UNIX   | '0 0 * MON *'         | Unknown month MON
            UNIX   | '0 0 * * FRIL'        | Unknown weekday FRIL
            """)
    void unknownNames(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("an entry that is no value, range or step is invalid, named with its field; names only in their fields")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '1--5 * * * *'        | Invalid minute 1--5
            UNIX   | '1- * * * *'          | Invalid minute 1-
            UNIX   | '-5 * * * *'          | Invalid minute -5
            UNIX   | '1-2-3 * * * *'       | Invalid minute 1-2-3
            UNIX   | '*-5 * * * *'         | Invalid minute *-5
            UNIX   | '5* * * * *'          | Invalid minute 5*
            UNIX   | '** * * * *'          | Invalid minute **
            UNIX   | '0 abc * * *'         | Invalid hour abc
            UNIX   | '0 0 MON * *'         | Invalid day of month MON
            UNIX   | '0 0 1x * *'          | Invalid day of month 1x
            QUARTZ | '0 0 0 ? * * 20x7'    | Invalid year 20x7
            """)
    void invalidEntries(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("an empty entry of a list is refused, the whole field shown")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '1,,2 * * * *'        | Minute list has an empty entry, found 1,,2
            UNIX   | '0 1, * * *'          | Hour list has an empty entry, found 1,
            QUARTZ | '0 0 0 ? * ,MON'      | Weekday list has an empty entry, found ,MON
            UNIX   | '0 0 , * *'           | Day of month list has an empty entry, found ,
            """)
    void emptyListEntry(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    // --- Ranges whose start is above their end ------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("Unix and the Quartz year refuse a range whose start is above its end, as cronie does")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 22-6 * * *'             | Hour range must not start after its end, found 22-6
            UNIX   | '0 0 * * FRI-SUN'          | Weekday range must not start after its end, found FRI-SUN
            UNIX   | '0 0 * DEC-JAN *'          | Month range must not start after its end, found DEC-JAN
            QUARTZ | '0 0 0 * * ? 2029-2027'    | Year range must not start after its end, found 2029-2027
            """)
    void unixRefusesARangeGoingDown(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @Test
    @DisplayName("Quartz takes a range whose start is above its end as wrapping around the field's end")
    void quartzRangeWraps() {
        CronExpression expression = expression(CronFormat.QUARTZ, "50-10 0 22-2 ? NOV-FEB FRI-MON");

        assertEquals(List.of(new CronPart.Range(50, 10)), expression.second().orElseThrow().parts());
        assertEquals(List.of(new CronPart.Range(22, 2)), expression.hour().parts());
        assertEquals(List.of(new CronPart.Range(11, 2)), expression.month().parts());
        assertEquals(List.of(new CronPart.Range(6, 2)), expression.weekday().parts());
        assertTrue(new CronPart.Range(22, 2).wraps());
        assertFalse(new CronPart.Range(2, 2).wraps());
    }

    @Test
    @DisplayName("a range from a value to itself is that value's range, in both formats")
    void rangeToItself() {
        assertEquals(List.of(new CronPart.Range(5, 5)), expression(CronFormat.UNIX, "5-5 * * * *").minute().parts());
        assertEquals(List.of(new CronPart.Range(2027, 2027)),
                expression(CronFormat.QUARTZ, "0 0 0 * * ? 2027-2027").year().orElseThrow().parts());
    }

    // --- Steps --------------------------------------------------------------------------------------

    @Test
    @DisplayName("a step goes through *, from a value or through a range, in both formats")
    void stepsParsed() {
        CronExpression unix = expression(CronFormat.UNIX, "*/15 5/20 1-30/10 */3 MON-FRI/2");

        assertEquals(List.of(new CronPart.Step(new CronPart.All(), 15)), unix.minute().parts());
        assertEquals(List.of(new CronPart.Step(new CronPart.Value(5), 20)), unix.hour().parts());
        assertEquals(List.of(new CronPart.Step(new CronPart.Range(1, 30), 10)), unix.dayOfMonth().parts());
        assertEquals(List.of(new CronPart.Step(new CronPart.All(), 3)), unix.month().parts());
        assertEquals(List.of(new CronPart.Step(new CronPart.Range(1, 5), 2)), unix.weekday().parts());

        CronExpression quartz = expression(CronFormat.QUARTZ, "0/30 0/15,50 22-2/2 ? JAN/3 * 2027/2");

        assertEquals(List.of(new CronPart.Step(new CronPart.Value(0), 30)), quartz.second().orElseThrow().parts());
        assertEquals(List.of(new CronPart.Step(new CronPart.Value(0), 15), new CronPart.Value(50)),
                quartz.minute().parts());
        assertEquals(List.of(new CronPart.Step(new CronPart.Range(22, 2), 2)), quartz.hour().parts());
        assertEquals(List.of(new CronPart.Step(new CronPart.Value(1), 3)), quartz.month().parts());
        assertEquals(List.of(new CronPart.Step(new CronPart.Value(2027), 2)), quartz.year().orElseThrow().parts());
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("a step is at least 1 and at most the field's highest value, as Quartz's CronExpression demands")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '*/0 * * * *'          | Minute step must be at least 1, found 0
            QUARTZ | '0 0/0 * * * ?'        | Minute step must be at least 1, found 0
            UNIX   | '0 0-23/00 * * *'      | Hour step must be at least 1, found 00
            UNIX   | '*/60 * * * *'         | Minute step must be at most 59, found 60
            QUARTZ | '*/60 0 0 * * ?'       | Second step must be at most 59, found 60
            UNIX   | '0 */24 * * *'         | Hour step must be at most 23, found 24
            UNIX   | '0 0 */32 * *'         | Day of month step must be at most 31, found 32
            UNIX   | '0 0 * */13 *'         | Month step must be at most 12, found 13
            UNIX   | '0 0 * * */8'          | Weekday step must be at most 7, found 8
            QUARTZ | '0 0 0 ? * */8'        | Weekday step must be at most 7, found 8
            QUARTZ | '0 0 0 * * ? */2100'   | Year step must be at most 2099, found 2100
            UNIX   | '*/99999999999 * * * *' | Minute step must be at most 59, found 99999999999
            UNIX   | '60/0 * * * *'         | Minute must be 0 to 59, found 60
            UNIX   | '0 22-6/2 * * *'       | Hour range must not start after its end, found 22-6
            """)
    void stepLimits(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @Test
    @DisplayName("the highest step of each field is accepted")
    void highestStepsAccepted() {
        expression(CronFormat.UNIX, "*/59 */23 */31 */12 */7");
        expression(CronFormat.QUARTZ, "*/59 */59 */23 */31 */12 ? */2099");
        expression(CronFormat.QUARTZ, "0 0 0 ? * */7");
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("a step without a start or without a number is an invalid entry")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '*/ * * * *'           | Invalid minute */
            UNIX   | '/5 * * * *'           | Invalid minute /5
            UNIX   | '*/x * * * *'          | Invalid minute */x
            UNIX   | '*/5/2 * * * *'        | Invalid minute */5/2
            UNIX   | '*/-1 * * * *'         | Invalid minute */-1
            UNIX   | '*/MON * * * *'        | Invalid minute */MON
            UNIX   | '0 0 * * */MON'        | Invalid weekday */MON
            QUARTZ | '/5 0 0 * * ?'         | Invalid second /5
            """)
    void invalidSteps(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    // --- Characters the format refuses --------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("Unix refuses ?, L, W and # in every field, in any case, and every character but * , - /")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 0 L * *'           | Unix cron does not allow L
            UNIX   | '0 9 * * ?'           | Unix cron does not allow ?
            UNIX   | '0 0 15W * *'         | Unix cron does not allow W
            UNIX   | '0 0 LW * *'          | Unix cron does not allow L
            UNIX   | '0 0 l * *'           | Unix cron does not allow L
            UNIX   | '0 0 * * 5L'          | Unix cron does not allow L
            UNIX   | '0 0 * * 5#3'         | Unix cron does not allow #
            UNIX   | 'L * * * *'           | Unix cron does not allow L
            UNIX   | '0 0 * * 1~5'         | Unix cron does not allow ~
            UNIX   | '0 @ * * *'           | Unix cron does not allow @
            UNIX   | '0 0 1.5 * *'         | Unix cron does not allow .
            UNIX   | '0 0 1;2 * *'         | Unix cron does not allow ;
            """)
    void unixRefusals(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("in a field, the characters the format refuses come before its entries and values")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '0 0 60,L * *'        | Unix cron does not allow L
            UNIX   | '0 0 1,,? * *'        | Unix cron does not allow ?
            QUARTZ | '0 0 99,? * * ?'      | Hour does not allow ?
            UNIX   | '0 0 * * WED,FRI#2'   | Unix cron does not allow #
            UNIX   | '0 0 * JUL,WED *'     | Unknown month WED
            """)
    void refusedCharactersComeFirst(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("Quartz allows ? and L in day of month and weekday only, W in day of month only, # in weekday only")
    @CsvSource(delimiter = '|', textBlock = """
            QUARTZ | '? 0 0 * * ?'         | Second does not allow ?
            QUARTZ | '0 0 ? * * ?'         | Hour does not allow ?
            QUARTZ | '0 0 0 ? ? *'         | Month does not allow ?
            QUARTZ | '0 0 0 ? * * ?'       | Year does not allow ?
            QUARTZ | 'L 0 0 * * ?'         | Second does not allow L
            QUARTZ | '0 5L 0 * * ?'        | Minute does not allow L
            QUARTZ | '0 0 0 ? L *'         | Month does not allow L
            QUARTZ | '0 0 0 ? * * L'       | Year does not allow L
            QUARTZ | '0 0 0 ? * 5W'        | Weekday does not allow W
            QUARTZ | '0 0 0 ? * LW'        | Weekday does not allow W
            QUARTZ | '0 0 W * * ?'         | Hour does not allow W
            QUARTZ | '0 0 0 1#2 * ?'       | Day of month does not allow #
            QUARTZ | '0 0 # * * ?'         | Hour does not allow #
            QUARTZ | '0 0 0 ? * 1~3'       | Quartz cron does not allow ~
            QUARTZ | '0 0 0 ? * 1.3'       | Quartz cron does not allow .
            """)
    void quartzPlacement(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @Test
    @DisplayName("digits of other scripts and other characters beyond ASCII are refused as characters")
    void nonAsciiCharactersRefused() {
        // Built from code points, so that the source stays ASCII: an Arabic-Indic one, a full-width one, and an emoji
        // outside the basic plane (two chars in a String).
        String arabicIndicOne = Character.toString(0x0661);
        String fullWidthOne = Character.toString(0xFF11);
        String emoji = Character.toString(0x1F600);

        assertEquals("Unix cron does not allow " + arabicIndicOne,
                problem(CronFormat.UNIX, "0 0 " + arabicIndicOne + " * *"));
        assertEquals("Quartz cron does not allow " + fullWidthOne,
                problem(CronFormat.QUARTZ, "0 0 " + fullWidthOne + " * * ?"));
        assertEquals("Unix cron does not allow " + emoji, problem(CronFormat.UNIX, "0 0 " + emoji + " * *"));
    }

    // --- Quartz: ?, the special forms and exactly one ? -----------------------------------------------

    @Test
    @DisplayName("Quartz's ? and day-of-month forms L, L-n, LW and nW are parsed, in any case")
    void quartzDayOfMonthForms() {
        assertEquals(List.of(new CronPart.NoSpecificValue()), quartzDayOfMonth("0 0 9 ? * MON").parts());
        assertTrue(quartzDayOfMonth("0 0 9 ? * MON").isNoSpecificValue());
        assertEquals(List.of(new CronPart.LastDayOfMonth(0)), quartzDayOfMonth("0 0 0 L * ?").parts());
        assertEquals(List.of(new CronPart.LastDayOfMonth(0)), quartzDayOfMonth("0 0 0 l * ?").parts());
        assertEquals(List.of(new CronPart.LastDayOfMonth(3)), quartzDayOfMonth("0 0 0 L-3 * ?").parts());
        assertEquals(List.of(new CronPart.LastDayOfMonth(0)), quartzDayOfMonth("0 0 0 L-0 * ?").parts());
        assertEquals(List.of(new CronPart.LastDayOfMonth(30)), quartzDayOfMonth("0 0 0 l-30 * ?").parts());
        assertEquals(List.of(new CronPart.LastWorkdayOfMonth()), quartzDayOfMonth("0 0 9 LW * ?").parts());
        assertEquals(List.of(new CronPart.LastWorkdayOfMonth()), quartzDayOfMonth("0 0 9 lw * ?").parts());
        assertEquals(List.of(new CronPart.NearestWorkday(15)), quartzDayOfMonth("0 0 9 15W * ?").parts());
        assertEquals(List.of(new CronPart.NearestWorkday(1)), quartzDayOfMonth("0 0 9 01w * ?").parts());
        assertEquals("L-3", quartzDayOfMonth("0 0 0 L-3 * ?").text());
    }

    @Test
    @DisplayName("Quartz's weekday forms L, nL and n#k are parsed, the weekday as a number or a name, in any case")
    void quartzWeekdayForms() {
        assertEquals(List.of(new CronPart.LastDayOfWeek()), quartzWeekday("0 0 9 ? * L").parts());
        assertEquals(List.of(new CronPart.LastDayOfWeekInMonth(6)), quartzWeekday("0 0 9 ? * 6L").parts());
        assertEquals(List.of(new CronPart.LastDayOfWeekInMonth(6)), quartzWeekday("0 0 9 ? * FRIL").parts());
        assertEquals(List.of(new CronPart.LastDayOfWeekInMonth(1)), quartzWeekday("0 0 9 ? * 1l").parts());
        assertEquals(List.of(new CronPart.NthDayOfWeekInMonth(6, 3)), quartzWeekday("0 0 9 ? * 6#3").parts());
        assertEquals(List.of(new CronPart.NthDayOfWeekInMonth(6, 3)), quartzWeekday("0 0 9 ? * fri#3").parts());
        assertEquals(List.of(new CronPart.NthDayOfWeekInMonth(2, 1)), quartzWeekday("0 0 9 ? * 2#1").parts());
        assertEquals(List.of(new CronPart.NthDayOfWeekInMonth(7, 5)), quartzWeekday("0 0 9 ? * SAT#05").parts());
        assertTrue(quartzWeekday("0 0 9 1 * ?").isNoSpecificValue());
    }

    private static CronField quartzDayOfMonth(String expression) {
        return expression(CronFormat.QUARTZ, expression).dayOfMonth();
    }

    private static CronField quartzWeekday(String expression) {
        return expression(CronFormat.QUARTZ, expression).weekday();
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("Quartz's ? and special forms stand alone, W needs a single day, the values of a form in range")
    @CsvSource(delimiter = '|', textBlock = """
            QUARTZ | '0 0 9 1-15W * ?'     | W needs a single day of month, found 1-15W
            QUARTZ | '0 0 9 1W,15 * ?'     | W needs a single day of month, found 1W,15
            QUARTZ | '0 0 9 W * ?'         | W needs a single day of month, found W
            QUARTZ | '0 0 9 */2W * ?'      | W needs a single day of month, found */2W
            QUARTZ | '0 0 9 32W * ?'       | Day of month must be 1 to 31, found 32
            QUARTZ | '0 0 9 0W * ?'        | Day of month must be 1 to 31, found 0
            QUARTZ | '0 0 9 L,15 * ?'      | L must stand alone in day of month, found L,15
            QUARTZ | '0 0 9 LW,15W * ?'    | L must stand alone in day of month, found LW,15W
            QUARTZ | '0 0 9 L-3,5 * ?'     | L must stand alone in day of month, found L-3,5
            QUARTZ | '0 0 9 L-31 * ?'      | Days before the last day must be 0 to 30, found 31
            QUARTZ | '0 0 9 L- * ?'        | Invalid day of month L-
            QUARTZ | '0 0 9 L-3W * ?'      | Invalid day of month L-3W
            QUARTZ | '0 0 9 5L * ?'        | Invalid day of month 5L
            QUARTZ | '0 0 9 1-L * ?'       | Invalid day of month 1-L
            QUARTZ | '0 0 9 L/2 * ?'       | Invalid day of month L/2
            QUARTZ | '0 0 9 ? * 6#6'       | Weekday occurrence must be 1 to 5, found 6
            QUARTZ | '0 0 9 ? * 6#0'       | Weekday occurrence must be 1 to 5, found 0
            QUARTZ | '0 0 9 ? * 8#2'       | Weekday must be 1 to 7, found 8
            QUARTZ | '0 0 9 ? * FRY#2'     | Unknown weekday FRY
            QUARTZ | '0 0 9 ? * 6#3,2'     | # must stand alone in weekday, found 6#3,2
            QUARTZ | '0 0 9 ? * 6#'        | Invalid weekday 6#
            QUARTZ | '0 0 9 ? * #3'        | Invalid weekday #3
            QUARTZ | '0 0 9 ? * 1-5#3'     | Invalid weekday 1-5#3
            QUARTZ | '0 0 9 ? * 6#3#2'     | Invalid weekday 6#3#2
            QUARTZ | '0 0 9 ? * 6L,2'      | L must stand alone in weekday, found 6L,2
            QUARTZ | '0 0 9 ? * L,MON'     | L must stand alone in weekday, found L,MON
            QUARTZ | '0 0 9 ? * FRIL,2'    | L must stand alone in weekday, found FRIL,2
            QUARTZ | '0 0 9 ? * 8L'        | Weekday must be 1 to 7, found 8
            QUARTZ | '0 0 9 ? * 6-7L'      | Invalid weekday 6-7L
            QUARTZ | '0 0 9 ? * 6L#3'      | Invalid weekday 6L#3
            QUARTZ | '0 0 9 ? * FRYL'      | Unknown weekday FRYL
            QUARTZ | '0 0 9 ? * LL'        | Invalid weekday LL
            QUARTZ | '0 0 9 ?,5 * *'       | ? must stand alone in day of month, found ?,5
            QUARTZ | '0 0 9 * * ?/2'       | ? must stand alone in weekday, found ?/2
            QUARTZ | '0 0 9 * * ??'        | ? must stand alone in weekday, found ??
            """)
    void quartzFormRules(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("Quartz: exactly one of day of month and weekday is ?, checked after every field")
    @CsvSource(delimiter = '|', textBlock = """
            QUARTZ | '0 0 9 * * MON-FRI'      | Day of month or weekday must be ?
            QUARTZ | '0 0 9 ? * ?'            | Only one of day of month and weekday can be ?
            QUARTZ | '0 0 9 1 * 2'            | Day of month or weekday must be ?
            QUARTZ | '0 0 0 L * 6L'           | Day of month or weekday must be ?
            QUARTZ | '0 0 9 * * *'            | Day of month or weekday must be ?
            QUARTZ | '0 0 9 * * MON-FRI 1969' | Year must be 1970 to 2099, found 1969
            QUARTZ | '0 0 9 ? * ? 2027'       | Only one of day of month and weekday can be ?
            """)
    void quartzExactlyOneQuestionMark(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    // --- Macros -------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} {1}: {2}")
    @DisplayName("the macro comes first: @reboot and unknown words are refused, Quartz has none")
    @CsvSource(delimiter = '|', textBlock = """
            UNIX   | '@reboot'             | @reboot is not a schedule
            UNIX   | '@REBOOT'             | @REBOOT is not a schedule
            UNIX   | '@reboot 0 0'         | @reboot is not a schedule
            UNIX   | '@often'              | Unknown macro @often
            UNIX   | '@'                   | Unknown macro @
            UNIX   | '@daily0'             | Unknown macro @daily0
            UNIX   | '@daily 5'            | @daily must stand alone, found 2 fields
            UNIX   | '@Hourly * * * *'     | @Hourly must stand alone, found 5 fields
            QUARTZ | '@daily'              | Quartz cron has no macros
            QUARTZ | '@reboot'             | Quartz cron has no macros
            QUARTZ | '@yearly 0 0 0 1 1 ?' | Quartz cron has no macros
            """)
    void macroRefusals(CronFormat format, String expression, String message) {
        assertEquals(message, problem(format, expression));
    }

    @ParameterizedTest(name = "{0} stands for {2}")
    @DisplayName("a Unix macro in any case is parsed as the expression it stands for, the macro kept")
    @CsvSource(delimiter = '|', textBlock = """
            '@yearly'      | YEARLY   | '0 0 1 1 *'
            '@annually'    | ANNUALLY | '0 0 1 1 *'
            '@monthly'     | MONTHLY  | '0 0 1 * *'
            '@weekly'      | WEEKLY   | '0 0 * * 0'
            '@daily'       | DAILY    | '0 0 * * *'
            '@midnight'    | MIDNIGHT | '0 0 * * *'
            '@hourly'      | HOURLY   | '0 * * * *'
            ' @DAILY '     | DAILY    | '0 0 * * *'
            '@Weekly'      | WEEKLY   | '0 0 * * 0'
            """)
    void macrosStandForTheirExpression(String written, CronMacro macro, String expansion) {
        CronExpression expected = expression(CronFormat.UNIX, expansion);

        CronExpression parsed = expression(CronFormat.UNIX, written);

        assertEquals(Optional.of(macro), parsed.macro());
        assertEquals(expansion, macro.expansion());
        assertEquals(expected.minute(), parsed.minute());
        assertEquals(expected.hour(), parsed.hour());
        assertEquals(expected.dayOfMonth(), parsed.dayOfMonth());
        assertEquals(expected.month(), parsed.month());
        assertEquals(expected.weekday(), parsed.weekday());
        assertEquals(Optional.empty(), parsed.second());
        assertEquals(Optional.empty(), parsed.year());
    }

    @Test
    @DisplayName("@weekly holds Sunday as 0, its fields' texts are those of the expression it stands for")
    void weeklyMacroFields() {
        CronExpression weekly = expression(CronFormat.UNIX, "@weekly");

        assertEquals(new CronField(CronFieldKind.WEEKDAY, "0", List.of(new CronPart.Value(0))), weekly.weekday());
        assertEquals(new CronField(CronFieldKind.DAY_OF_MONTH, "*", List.of(new CronPart.All())),
                weekly.dayOfMonth());
    }
}
