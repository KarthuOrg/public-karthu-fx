package org.karthu.fx.form;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The cron format as a user sees it: two formats, shown as <em>Unix</em> and <em>Quartz</em>.
 */
class CronFormatTest {

    @Test
    @DisplayName("the formats are Unix and Quartz, in that order")
    void formats() {
        assertEquals(List.of(CronFormat.UNIX, CronFormat.QUARTZ), List.of(CronFormat.values()));
    }

    @Test
    @DisplayName("a format is shown by its name, also as its string, so a combo box needs no converter")
    void shownByItsName() {
        assertEquals("Unix", CronFormat.UNIX.displayName());
        assertEquals("Quartz", CronFormat.QUARTZ.displayName());
        assertEquals("Unix", CronFormat.UNIX.toString());
        assertEquals("Quartz", CronFormat.QUARTZ.toString());
    }
}
