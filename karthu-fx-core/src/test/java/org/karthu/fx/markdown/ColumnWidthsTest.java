package org.karthu.fx.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The rule that shares a table's width among its columns, with measured widths as numbers: each column's widest cell
 * on one line (its content) and its longest word (its minimum), in pixels.
 */
class ColumnWidthsTest {

    private static final double PIXEL = 1;

    @Test
    @DisplayName("when every column's widest cell fits, each gets it and the width left over in proportion to it")
    void proportionalShares() {
        assertArrayEquals(new double[] {100, 200},
                ColumnWidths.distribute(300, new double[] {100, 200}, new double[] {10, 10}, PIXEL));
        assertArrayEquals(new double[] {200, 400},
                ColumnWidths.distribute(600, new double[] {100, 200}, new double[] {10, 10}, PIXEL));
        assertArrayEquals(new double[] {60, 90, 150},
                ColumnWidths.distribute(300, new double[] {40, 60, 100}, new double[] {10, 10, 10}, PIXEL));
    }

    @Test
    @DisplayName("short columns next to a long one keep their widest cell on one line, the long column takes the rest")
    void shortColumnsKeepTheirFullWidth() {
        assertArrayEquals(new double[] {60, 50, 190},
                ColumnWidths.distribute(300, new double[] {60, 50, 1000}, new double[] {30, 20, 100}, PIXEL));
        assertArrayEquals(new double[] {60, 190, 50},
                ColumnWidths.distribute(300, new double[] {60, 1000, 50}, new double[] {30, 100, 20}, PIXEL));
        assertArrayEquals(new double[] {40, 40, 220},
                ColumnWidths.distribute(300, new double[] {10, 10, 1000}, new double[] {40, 40, 100}, PIXEL));
    }

    @Test
    @DisplayName("when the widest cells do not fit, the columns fill up evenly from their longest words, each up to its"
            + " widest cell")
    void evenFillFromTheMinimums() {
        double[] content = {80, 250, 2000};
        double[] minimum = {40, 50, 100};

        assertArrayEquals(new double[] {40, 50, 100}, ColumnWidths.distribute(190, content, minimum, PIXEL),
                "at the longest words' total, every column its longest word");
        assertArrayEquals(new double[] {70, 80, 130}, ColumnWidths.distribute(280, content, minimum, PIXEL),
                "every column 30 px above its longest word");
        assertArrayEquals(new double[] {80, 225, 275}, ColumnWidths.distribute(580, content, minimum, PIXEL),
                "the first column stays at its widest cell, the two others grow on by the same amount");
        assertArrayEquals(new double[] {80, 225, 274}, ColumnWidths.distribute(579, content, minimum, PIXEL),
                "a pixel narrower, the two half pixels make one, which goes to the leftmost");
        assertArrayEquals(new double[] {80, 250, 1999}, ColumnWidths.distribute(2329, content, minimum, PIXEL),
                "a pixel below the widest cells' total, only the long column is short of its widest cell");
    }

    @Test
    @DisplayName("every column's width changes smoothly with the table's width: a pixel more or less moves no column by"
            + " more than two pixels")
    void noColumnJumps() {
        assertSmooth(new double[] {80, 250, 2000}, new double[] {40, 50, 100}, PIXEL);
        assertSmooth(new double[] {80, 500, 520}, new double[] {40, 100, 100}, PIXEL);
        assertSmooth(new double[] {80, 500, 500}, new double[] {40, 100, 300}, PIXEL);
        assertSmooth(new double[] {60, 50, 1000}, new double[] {30, 20, 100}, PIXEL);
        assertSmooth(new double[] {50, 1000}, new double[] {10, 60}, PIXEL);
        assertSmooth(new double[] {40, 1000, 40}, new double[] {10, 60, 20}, PIXEL);
        assertSmooth(new double[] {100, 100, 100}, new double[] {150, 90, 10}, PIXEL);
        assertSmooth(new double[] {60.1, 1000, 75.3, 12}, new double[] {30, 100, 12.6, 12}, 0.8);
    }

    /**
     * Sweeps the table's width in steps of one device pixel, from nothing to well beyond the columns' widest cells
     * together, and fails where a column's width changes by more than two device pixels between neighbouring steps.
     */
    private static void assertSmooth(double[] content, double[] minimum, double pixel) {
        double maximumTotal = 0;
        for (int i = 0; i < content.length; i++) {
            maximumTotal += Math.max(content[i], minimum[i]);
        }
        long steps = (long) Math.ceil(maximumTotal / pixel) + 200;
        double[] previous = ColumnWidths.distribute(0, content, minimum, pixel);
        for (long step = 1; step <= steps; step++) {
            double width = step * pixel;
            double[] widths = ColumnWidths.distribute(width, content, minimum, pixel);
            for (int i = 0; i < widths.length; i++) {
                if (Math.abs(widths[i] - previous[i]) > 2 * pixel + 1e-9) {
                    fail("column " + i + " jumps from " + previous[i] + " to " + widths[i] + " at a width of "
                            + width);
                }
            }
            previous = widths;
        }
    }

    @Test
    @DisplayName("a column that reaches its widest cell stays there, the others grow on by the same amount")
    void fullColumnStaysWhileTheOthersGrowOn() {
        assertArrayEquals(new double[] {80, 235, 285},
                ColumnWidths.distribute(600, new double[] {80, 250, 2000}, new double[] {40, 50, 100}, PIXEL));
        assertArrayEquals(new double[] {80, 185, 235},
                ColumnWidths.distribute(500, new double[] {80, 250, 2000}, new double[] {40, 50, 100}, PIXEL),
                "narrower, the two others 50 px less each");
    }

    @Test
    @DisplayName("two long columns grow from their longest words by the same amount")
    void twoLongColumns() {
        assertArrayEquals(new double[] {80, 160, 360},
                ColumnWidths.distribute(600, new double[] {80, 500, 500}, new double[] {40, 100, 300}, PIXEL));
        assertArrayEquals(new double[] {80, 260, 260},
                ColumnWidths.distribute(600, new double[] {80, 500, 520}, new double[] {40, 100, 100}, PIXEL));
    }

    @Test
    @DisplayName("a short column's widest cell is rounded up to whole pixels, so it stays on one line")
    void fullWidthRoundsUp() {
        assertArrayEquals(new double[] {61, 239},
                ColumnWidths.distribute(300, new double[] {60.2, 1000}, new double[] {30, 100}, PIXEL));
        double[] scaled = ColumnWidths.distribute(300, new double[] {60.1, 1000}, new double[] {30, 100}, 0.8);
        assertArrayEquals(new double[] {76 * 0.8, 299 * 0.8}, scaled, 1e-9);
        assertEquals(300, scaled[0] + scaled[1], 1e-9);
    }

    @Test
    @DisplayName("near the longest words' total a short column is short of its widest cell too: every column grows"
            + " from its longest word by the same amount")
    void nearTheMinimumsEveryColumnGrows() {
        assertArrayEquals(new double[] {25, 75},
                ColumnWidths.distribute(100, new double[] {50, 1000}, new double[] {10, 60}, PIXEL));
        assertArrayEquals(new double[] {24, 73, 33},
                ColumnWidths.distribute(130, new double[] {40, 1000, 40}, new double[] {10, 60, 20}, PIXEL),
                "13 1/3 px each, the pixel of the three thirds to the leftmost");
    }

    @Test
    @DisplayName("a column whose share is below its longest word keeps that width, the others share the rest")
    void minimumWidths() {
        assertArrayEquals(new double[] {50, 250},
                ColumnWidths.distribute(300, new double[] {20, 1000}, new double[] {50, 60}, PIXEL));
        assertArrayEquals(new double[] {150, 75, 75},
                ColumnWidths.distribute(300, new double[] {100, 100, 100}, new double[] {150, 10, 10}, PIXEL));
    }

    @Test
    @DisplayName("a long word that takes width from the others leaves them growing evenly from their longest words,"
            + " each up to its widest cell")
    void longWordTakesFromTheOthers() {
        assertArrayEquals(new double[] {150, 100, 50},
                ColumnWidths.distribute(300, new double[] {100, 100, 100}, new double[] {150, 90, 10}, PIXEL));
    }

    @Test
    @DisplayName("when the minimums exceed the width, every column keeps its minimum and the table is wider")
    void minimumsWiderThanTheTable() {
        assertArrayEquals(new double[] {60, 70},
                ColumnWidths.distribute(100, new double[] {200, 300}, new double[] {60, 70}, PIXEL));
        assertArrayEquals(new double[] {60, 70},
                ColumnWidths.distribute(130, new double[] {200, 300}, new double[] {60, 70}, PIXEL));
        assertArrayEquals(new double[] {60, 70},
                ColumnWidths.distribute(0, new double[] {200, 300}, new double[] {60, 70}, PIXEL));
    }

    @Test
    @DisplayName("widths are whole pixels that add up to the width, the left-over pixels to the largest remainders")
    void wholePixels() {
        assertArrayEquals(new double[] {34, 33, 33},
                ColumnWidths.distribute(100, new double[] {1, 1, 1}, new double[] {0, 0, 0}, PIXEL));
        assertArrayEquals(new double[] {33, 34, 33},
                ColumnWidths.distribute(100, new double[] {10, 10.2, 10}, new double[] {0, 0, 0}, PIXEL));
        assertArrayEquals(new double[] {33, 33},
                ColumnWidths.distribute(66.7, new double[] {1, 1}, new double[] {0, 0}, PIXEL));
    }

    @Test
    @DisplayName("a minimum of a fraction of a pixel is rounded up, so the longest word always fits")
    void minimumsRoundUp() {
        assertArrayEquals(new double[] {11, 21},
                ColumnWidths.distribute(20, new double[] {30, 40}, new double[] {10.2, 20.01}, PIXEL));
        assertArrayEquals(new double[] {11, 89},
                ColumnWidths.distribute(100, new double[] {1, 100}, new double[] {10.2, 0}, PIXEL));
    }

    @Test
    @DisplayName("the table's narrowest width is its minimums together, each rounded up, as the columns get them")
    void minimumTotal() {
        assertEquals(32, ColumnWidths.minimumTotal(new double[] {10.2, 20.01, 0}, PIXEL));
        assertEquals(32, ColumnWidths.minimumTotal(new double[] {10, 22}, PIXEL));
        assertEquals(13 * 0.8 + 26 * 0.8, ColumnWidths.minimumTotal(new double[] {10, 20.5}, 0.8), 1e-9);
        assertEquals(0, ColumnWidths.minimumTotal(new double[] {}, PIXEL));
    }

    @Test
    @DisplayName("on a scaled screen the widths are whole device pixels, 0.8 at 125 %")
    void devicePixels() {
        double[] widths = ColumnWidths.distribute(100, new double[] {1, 1, 1}, new double[] {0, 0, 0}, 0.8);

        assertArrayEquals(new double[] {42 * 0.8, 42 * 0.8, 41 * 0.8}, widths, 1e-9);
        assertEquals(100, widths[0] + widths[1] + widths[2], 1e-9);
    }

    @Test
    @DisplayName("columns without content share the width equally, and no columns get no widths")
    void emptyColumns() {
        assertArrayEquals(new double[] {50, 50},
                ColumnWidths.distribute(100, new double[] {0, 0}, new double[] {0, 0}, PIXEL));
        assertArrayEquals(new double[] {},
                ColumnWidths.distribute(100, new double[] {}, new double[] {}, PIXEL));
    }

    @Test
    @DisplayName("a dragged column takes its new width from the column to its right, which gives it back on the way"
            + " back")
    void draggedColumnTakesFromItsNeighbour() {
        double[] minimum = {10, 10, 10};

        assertArrayEquals(new double[] {130, 70, 100},
                ColumnWidths.resize(new double[] {100, 100, 100}, minimum, 0, 30, PIXEL));
        assertArrayEquals(new double[] {70, 130, 100},
                ColumnWidths.resize(new double[] {100, 100, 100}, minimum, 0, -30, PIXEL));
        assertArrayEquals(new double[] {100, 120, 80},
                ColumnWidths.resize(new double[] {100, 100, 100}, minimum, 1, 20, PIXEL));
    }

    @Test
    @DisplayName("the columns to the right give, the nearest first, each down to its minimum, and no further")
    void neighboursGiveWithinTheirMinimums() {
        assertArrayEquals(new double[] {130, 90, 80},
                ColumnWidths.resize(new double[] {100, 100, 100}, new double[] {10, 90, 10}, 0, 30, PIXEL));
        assertArrayEquals(new double[] {150, 90, 60},
                ColumnWidths.resize(new double[] {100, 100, 100}, new double[] {10, 90, 60}, 0, 100, PIXEL));
        assertArrayEquals(new double[] {100, 90, 60},
                ColumnWidths.resize(new double[] {100, 90, 60}, new double[] {10, 90, 60}, 0, 5, PIXEL));
    }

    @Test
    @DisplayName("a dragged column shrinks down to its minimum, and the column to its right takes what it gives")
    void draggedColumnStopsAtItsMinimum() {
        assertArrayEquals(new double[] {80, 120, 100},
                ColumnWidths.resize(new double[] {100, 100, 100}, new double[] {80, 10, 10}, 0, -30, PIXEL));
        assertArrayEquals(new double[] {80, 120, 100},
                ColumnWidths.resize(new double[] {80, 120, 100}, new double[] {80, 10, 10}, 0, -30, PIXEL));
    }

    @Test
    @DisplayName("the last column's right edge is the table's: dragging it changes nothing")
    void lastColumnKeepsTheTableEdge() {
        assertArrayEquals(new double[] {100, 100},
                ColumnWidths.resize(new double[] {100, 100}, new double[] {10, 10}, 1, 30, PIXEL));
        assertArrayEquals(new double[] {100, 100},
                ColumnWidths.resize(new double[] {100, 100}, new double[] {10, 10}, 1, -30, PIXEL));
    }

    @Test
    @DisplayName("a drag moves by whole device pixels, and the widths still add up to the same width")
    void dragInDevicePixels() {
        assertArrayEquals(new double[] {110, 90},
                ColumnWidths.resize(new double[] {100, 100}, new double[] {10, 10}, 0, 10.4, PIXEL));
        double[] scaled = ColumnWidths.resize(new double[] {80, 80}, new double[] {8, 8}, 0, 8.3, 0.8);
        assertArrayEquals(new double[] {88, 72}, scaled, 1e-9);
        assertArrayEquals(new double[] {100, 100},
                ColumnWidths.resize(new double[] {100, 100}, new double[] {10, 10}, 0, 0.3, PIXEL));
    }

    @Test
    @DisplayName("the widths the reader set keep their shares at another width, none below its minimum")
    void readerWidthsKeepTheirShares() {
        double[] reader = {150, 50, 100};

        assertArrayEquals(reader, ColumnWidths.refit(300, reader, new double[] {10, 10, 10}, PIXEL));
        assertArrayEquals(new double[] {300, 100, 200},
                ColumnWidths.refit(600, reader, new double[] {10, 10, 10}, PIXEL));
        assertArrayEquals(new double[] {66, 40, 44},
                ColumnWidths.refit(150, reader, new double[] {10, 40, 10}, PIXEL));
        assertArrayEquals(new double[] {100, 40, 30},
                ColumnWidths.refit(150, reader, new double[] {100, 40, 30}, PIXEL));
    }

    @Test
    @DisplayName("a content narrower than its minimum counts as the minimum")
    void contentBelowMinimum() {
        assertArrayEquals(new double[] {100, 100},
                ColumnWidths.distribute(200, new double[] {10, 100}, new double[] {100, 10}, PIXEL));
    }
}
