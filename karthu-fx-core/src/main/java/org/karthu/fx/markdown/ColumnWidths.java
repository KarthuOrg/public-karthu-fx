package org.karthu.fx.markdown;

/**
 * The rule that shares a table's width among its columns, after a web browser's automatic table layout. A column's
 * <em>maximum</em> is the width of its widest cell on one line (its content), its <em>minimum</em> the width of its
 * longest word, both measured by the table with its padding.
 *
 * <p>When the maximums fit into the width, every column gets its maximum, and the width left over is shared in
 * proportion to the maximums: the table is as wide as the width given. Otherwise the columns fill up evenly from their
 * minimums: every column starts at its minimum, so a word (a code name above all) never breaks, and all grow by the
 * same amount until a column reaches its maximum, which then stays there while the others grow on, until the width
 * is used. A short column (names, types, flags) has little between its longest word and its widest cell, so it
 * reaches its maximum early and stays on one line, while the long columns take the rest and wrap. Every column's
 * width rises steadily with the width, also where the rule changes at the minimums' and the maximums' totals: while
 * the reader makes the view narrower or wider, no column jumps. When the minimums together are wider than the width,
 * every column keeps its minimum and the table is wider than the width: it scrolls sideways.
 *
 * <p>The widths are whole device pixels that add up to the width, so the columns end exactly at the table's right
 * edge: each share is rounded down and the pixels left over go to the columns that lost the most, the leftmost first
 * on a tie. A minimum and a maximum are rounded up, so neither a word nor a short column's cell wraps for a fraction
 * of a pixel.
 *
 * <p>The reader may drag a column's right edge ({@link #resize(double[], double[], int, double, double)}): the column
 * takes its new width from the columns to its right or gives it to its right neighbour, within every column's
 * minimum. From then on the widths the reader left are the shares: at another width the columns keep them, in
 * proportion and within the same minimums ({@link #refit(double, double[], double[], double)}).
 */
final class ColumnWidths {

    /**
     * Absorbs the floating-point error of a division that should come out whole.
     */
    private static final double EPSILON = 1e-9;

    private ColumnWidths() {
    }

    /**
     * Shares a width among columns by their content, see the class comment: the widths until the reader drags a
     * column's edge.
     *
     * @param available the width the columns share, without the table's insets
     * @param content   each column's widest cell on one line, its maximum
     * @param minimum   each column's longest word; a content narrower than it counts as the minimum
     * @param pixel     the width of a device pixel: 1, or 0.8 on a screen scaled to 125 %
     * @return each column's width, in the columns' order
     */
    static double[] distribute(double available, double[] content, double[] minimum, double pixel) {
        int count = content.length;
        long pixels = floorPixels(available, pixel);
        long[] minimumPixels = new long[count];
        long[] maximumPixels = new long[count];
        double[] maximum = new double[count];
        long minimumTotal = 0;
        long maximumTotal = 0;
        for (int i = 0; i < count; i++) {
            maximum[i] = Math.max(content[i], minimum[i]);
            minimumPixels[i] = ceilPixels(minimum[i], pixel);
            maximumPixels[i] = ceilPixels(maximum[i], pixel);
            minimumTotal += minimumPixels[i];
            maximumTotal += maximumPixels[i];
        }
        if (count == 0 || minimumTotal >= pixels) {
            return widths(minimumPixels, pixel);
        }
        if (maximumTotal <= pixels) {
            return widths(rounded(pixels, shares(pixels, maximum, maximumPixels), maximumPixels), pixel);
        }
        double growth = evenGrowth(pixels - minimumTotal, minimumPixels, maximumPixels);
        double[] shares = new double[count];
        for (int i = 0; i < count; i++) {
            shares[i] = Math.min(maximumPixels[i], minimumPixels[i] + growth);
        }
        return widths(rounded(pixels, shares, minimumPixels), pixel);
    }

    /**
     * How far every column grows above its minimum when the columns fill up evenly: a column that reaches its maximum
     * stays there, and the others grow on by the same amount. The growth is the width left above the minimums,
     * less what the columns at their maximums take, divided among the columns still growing; it is measured again
     * until no further column reaches its maximum. A column that stops takes no more than the growth, so the growth
     * of the others only rises. Not every column stops, as the maximums together are wider than the width.
     */
    private static double evenGrowth(long extra, long[] minimumPixels, long[] maximumPixels) {
        int count = minimumPixels.length;
        boolean[] full = new boolean[count];
        double growth = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            long left = extra;
            int growing = 0;
            for (int i = 0; i < count; i++) {
                if (full[i]) {
                    left -= maximumPixels[i] - minimumPixels[i];
                } else {
                    growing++;
                }
            }
            growth = (double) left / growing;
            for (int i = 0; i < count; i++) {
                if (!full[i] && maximumPixels[i] - minimumPixels[i] <= growth) {
                    full[i] = true;
                    changed = true;
                }
            }
        }
        return growth;
    }

    /**
     * Fits the widths the reader left the columns with to another width, as when the view becomes narrower: they keep
     * their shares, in proportion to these widths, within the same minimums and in whole device pixels as
     * {@link #distribute(double, double[], double[], double)} has them; a width below a column's minimum counts as the
     * minimum. At the width they add up to, they stay as they are.
     *
     * @param available the width the columns share, without the table's insets
     * @param widths    each column's width as the reader left it
     * @param minimum   each column's longest word
     * @param pixel     the width of a device pixel
     * @return each column's width, in the columns' order
     */
    static double[] refit(double available, double[] widths, double[] minimum, double pixel) {
        int count = widths.length;
        long[] minimumPixels = new long[count];
        double[] weights = new double[count];
        long minimumTotal = 0;
        for (int i = 0; i < count; i++) {
            minimumPixels[i] = ceilPixels(minimum[i], pixel);
            weights[i] = Math.max(widths[i], minimum[i]);
            minimumTotal += minimumPixels[i];
        }
        long pixels = floorPixels(available, pixel);
        if (count == 0 || minimumTotal >= pixels) {
            return widths(minimumPixels, pixel);
        }
        return widths(rounded(pixels, shares(pixels, weights, minimumPixels), minimumPixels), pixel);
    }

    /**
     * Resizes a column the reader drags by its right edge. The column grows by taking width from the columns to its
     * right, the nearest first, each down to its minimum and no further, so it grows only as far as they can give; it
     * shrinks down to its own minimum, and the column right of it takes what it gives. So the widths still add up to
     * the same width, and the columns left of the dragged one stay as they are. The last column's right edge is the
     * table's: it cannot be dragged. The drag moves by whole device pixels.
     *
     * @param widths  each column's width, whole device pixels
     * @param minimum each column's longest word
     * @param column  the index of the dragged column
     * @param delta   how far its right edge is dragged, to the right if positive
     * @param pixel   the width of a device pixel
     * @return each column's new width, in the columns' order
     */
    static double[] resize(double[] widths, double[] minimum, int column, double delta, double pixel) {
        int count = widths.length;
        long[] pixels = new long[count];
        long[] minimumPixels = new long[count];
        for (int i = 0; i < count; i++) {
            pixels[i] = Math.round(widths[i] / pixel);
            minimumPixels[i] = ceilPixels(minimum[i], pixel);
        }
        long change = Math.round(delta / pixel);
        if (column < 0 || column >= count - 1) {
            return widths(pixels, pixel);
        }
        if (change > 0) {
            long wanted = change;
            for (int i = column + 1; i < count && wanted > 0; i++) {
                long given = Math.min(wanted, Math.max(0, pixels[i] - minimumPixels[i]));
                pixels[i] -= given;
                wanted -= given;
            }
            pixels[column] += change - wanted;
        } else if (change < 0) {
            long given = Math.min(-change, Math.max(0, pixels[column] - minimumPixels[column]));
            pixels[column] -= given;
            pixels[column + 1] += given;
        }
        return widths(pixels, pixel);
    }

    /**
     * Rounds exact shares that add up to the width to whole device pixels that do too: each share rounded down, but
     * not below its column's floor, and the pixels left over to the columns that lost the most, the leftmost first on
     * a tie.
     */
    private static long[] rounded(long pixels, double[] shares, long[] floors) {
        int count = shares.length;
        long[] result = new long[count];
        long assigned = 0;
        for (int i = 0; i < count; i++) {
            result[i] = Math.max(floors[i], (long) Math.floor(shares[i] + EPSILON));
            assigned += result[i];
        }
        for (long left = pixels - assigned; left > 0; left--) {
            int best = -1;
            for (int i = 0; i < count; i++) {
                if (best < 0 || shares[i] - result[i] > shares[best] - result[best]) {
                    best = i;
                }
            }
            result[best]++;
        }
        return result;
    }

    /**
     * The narrowest width the columns can share: their minimums rounded up to whole device pixels, added up. At this
     * width or narrower, {@link #distribute(double, double[], double[], double)} gives every column its minimum.
     *
     * @param minimum each column's longest word
     * @param pixel   the width of a device pixel
     * @return the minimums together
     */
    static double minimumTotal(double[] minimum, double pixel) {
        long total = 0;
        for (double width : minimum) {
            total += ceilPixels(width, pixel);
        }
        return total * pixel;
    }

    /**
     * The exact shares, in device pixels: in proportion to the weights, every column whose share falls below its
     * floor (its minimum, or its maximum when all maximums fit) fixed at the floor and the rest shared again, until no
     * share falls below. Fixing a column only takes width from the others, so a column below its floor stays below it
     * in the next round, and at least one column stays free, since the floors together are no wider than the width.
     */
    private static double[] shares(long pixels, double[] weights, long[] floors) {
        int count = weights.length;
        boolean[] fixed = new boolean[count];
        double[] shares = new double[count];
        boolean changed = true;
        while (changed) {
            changed = false;
            double free = pixels;
            double freeWeight = 0;
            int freeColumns = 0;
            for (int i = 0; i < count; i++) {
                if (fixed[i]) {
                    free -= floors[i];
                } else {
                    freeWeight += weights[i];
                    freeColumns++;
                }
            }
            for (int i = 0; i < count; i++) {
                if (fixed[i]) {
                    shares[i] = floors[i];
                } else {
                    shares[i] = freeWeight > 0 ? free * weights[i] / freeWeight : free / freeColumns;
                }
            }
            for (int i = 0; i < count; i++) {
                if (!fixed[i] && shares[i] < floors[i]) {
                    fixed[i] = true;
                    changed = true;
                }
            }
        }
        return shares;
    }

    /**
     * A width in whole device pixels, rounded up: a minimum or a maximum, so that nothing wraps for a fraction of a
     * pixel.
     */
    private static long ceilPixels(double width, double pixel) {
        return (long) Math.ceil(width / pixel - EPSILON);
    }

    /**
     * A width in whole device pixels, rounded down: the width the columns share, which they must not exceed.
     */
    private static long floorPixels(double width, double pixel) {
        return (long) Math.floor(width / pixel + EPSILON);
    }

    private static double[] widths(long[] pixels, double pixel) {
        double[] widths = new double[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            widths[i] = pixels[i] * pixel;
        }
        return widths;
    }
}
