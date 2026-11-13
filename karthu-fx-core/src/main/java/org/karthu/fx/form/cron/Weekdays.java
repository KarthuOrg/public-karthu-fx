package org.karthu.fx.form.cron;

import org.karthu.fx.form.CronFormat;

/**
 * The days of the week in one place: their three-letter names and how each format numbers them. Unix numbers Sunday 0
 * to Saturday 6, and Sunday 7 again; Quartz numbers Sunday 1 to Saturday 7. Between these methods a day of the week is
 * counted from Sunday as 0 to Saturday as 6, whatever the format, so that names and numbers never mix up the two
 * numberings.
 */
final class Weekdays {

    /** The three-letter names, by the day of the week counted from Sunday as 0. */
    private static final String[] NAMES = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    private Weekdays() {
    }

    /**
     * Returns the day of the week of a weekday value in a format: Unix 0 to 7 (7 Sunday again), Quartz 1 to 7.
     *
     * @param value  the value as the format numbers it
     * @param format the format
     * @return the day of the week, Sunday 0 to Saturday 6
     */
    static int ofValue(int value, CronFormat format) {
        if (format == CronFormat.UNIX) {
            return value % 7;
        }
        return value - 1;
    }

    /**
     * Returns the day of the week of a three-letter name in any case.
     *
     * @param name the name, {@code SUN} to {@code SAT}
     * @return the day of the week, Sunday 0 to Saturday 6, or -1 when the name is none
     */
    static int ofName(String name) {
        for (int day = 0; day < NAMES.length; day++) {
            if (NAMES[day].equalsIgnoreCase(name)) {
                return day;
            }
        }
        return -1;
    }

    /**
     * Returns the value a format gives a day of the week: Unix 0 to 6, Quartz 1 to 7.
     *
     * @param day    the day of the week, Sunday 0 to Saturday 6
     * @param format the format
     * @return the value as the format numbers it
     */
    static int value(int day, CronFormat format) {
        if (format == CronFormat.UNIX) {
            return day;
        }
        return day + 1;
    }

    /**
     * Returns the three-letter name of a day of the week, in capitals.
     *
     * @param day the day of the week, Sunday 0 to Saturday 6
     * @return {@code SUN} to {@code SAT}
     */
    static String name(int day) {
        return NAMES[day];
    }
}
