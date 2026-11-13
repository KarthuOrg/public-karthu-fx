package org.karthu.fx.markdown;

import javafx.scene.control.TableColumn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The widths of a {@link MarkdownTable}'s columns: fits the shown columns to the table's width by the
 * {@link ColumnWidths} rule, from what the table measures, and remembers what they were fitted to, so a layout pass
 * that changes nothing changes no width. By their content until the reader drags a column's edge; from then on by the
 * widths the reader left them with, the shares. The table measures its columns and lays them out; this keeps the
 * fitting's state.
 */
final class ColumnFit {

    /**
     * The preferred width JavaFX gives a column nobody has sized, which it takes for "not set", see
     * {@link #setFittedWidth(TableColumn, double)}.
     */
    private static final double UNSET_WIDTH = 80;

    /**
     * What the columns were last fitted to, {@code null} before the first fit.
     */
    private Fitted fitted;

    /**
     * The width of every column, by column, as the reader left it with the last drag of a column's edge; empty until
     * the first drag. The columns are fitted to a new width in these shares.
     */
    private final Map<TableColumn<?, ?>, Double> readerWidths = new HashMap<>();

    /**
     * Fits the shown columns to a width, unless they are fitted to it, to the same measurement and to the same widths
     * of the reader already: by their content until the reader drags a column, by the shares the reader left them with
     * from then on.
     *
     * @param available   the width the columns share, without the table's insets
     * @param columns     the shown columns, in their shown order
     * @param measurement what the shown columns measure, in the same order
     * @param pixel       the width of a device pixel
     */
    void fit(double available, List<? extends TableColumn<?, ?>> columns, Measurement measurement, double pixel) {
        double[] shares = readerShares(columns);
        Fitted fit = new Fitted(available, measurement, shares);
        if (fit.equals(fitted)) {
            return;
        }
        fitted = fit;
        double[] widths = shares == null
                ? ColumnWidths.distribute(available, measurement.content(), measurement.minimum(), pixel)
                : ColumnWidths.refit(available, shares, measurement.minimum(), pixel);
        for (int i = 0; i < columns.size(); i++) {
            setFittedWidth(columns.get(i), widths[i]);
        }
    }

    /**
     * Gives a column its fitted width, as its preferred width, which its width follows.
     *
     * <p>A width of exactly {@value #UNSET_WIDTH} must not reach the column as it is. That is the preferred width of
     * a column nobody has sized, and JavaFX takes a column with exactly this preferred width for one without a width
     * of its own: when it first lays out the column's header, it fits the column to its content by itself. The column
     * would get another width than the one fitted here, and the columns would no longer add up to the table's width,
     * so the table would show a horizontal bar of its own over its last row. Such a column gets the next larger
     * number instead, which differs in no pixel.
     */
    private static void setFittedWidth(TableColumn<?, ?> column, double width) {
        column.setPrefWidth(width == UNSET_WIDTH ? Math.nextUp(UNSET_WIDTH) : width);
    }

    /**
     * Resizes a shown column whose right edge the reader drags, by the {@link ColumnWidths} rule, and keeps the widths
     * of all columns, the hidden ones too, as the reader's from now on.
     *
     * @param all     every column of the table, the hidden ones too
     * @param shown   the shown columns, in their shown order
     * @param index   the dragged column's place among the shown columns
     * @param delta   how far its right edge is dragged, to the right if positive
     * @param minimum each shown column's longest word, in their shown order
     * @param pixel   the width of a device pixel
     * @return whether a width changed
     */
    boolean drag(List<? extends TableColumn<?, ?>> all, List<? extends TableColumn<?, ?>> shown, int index,
                 double delta, double[] minimum, double pixel) {
        double[] widths = new double[shown.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = shown.get(i).getWidth();
        }
        double[] resized = ColumnWidths.resize(widths, minimum, index, delta, pixel);
        if (Arrays.equals(widths, resized)) {
            return false;
        }
        for (TableColumn<?, ?> each : all) {
            readerWidths.put(each, each.getWidth());
        }
        for (int i = 0; i < shown.size(); i++) {
            readerWidths.put(shown.get(i), resized[i]);
            setFittedWidth(shown.get(i), resized[i]);
        }
        return true;
    }

    /**
     * The widths the reader left the shown columns with, in their shown order; {@code null} until the reader drags a
     * column.
     */
    private double[] readerShares(List<? extends TableColumn<?, ?>> columns) {
        if (readerWidths.isEmpty()) {
            return null;
        }
        double[] shares = new double[columns.size()];
        for (int i = 0; i < shares.length; i++) {
            Double share = readerWidths.get(columns.get(i));
            shares[i] = share == null ? columns.get(i).getWidth() : share;
        }
        return shares;
    }

    /**
     * What the shown columns measure, in their shown order.
     *
     * @param content each column's widest cell on one line, with what lies around its text
     * @param minimum each column's longest word, with what lies around it
     */
    record Measurement(double[] content, double[] minimum) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Measurement measurement && Arrays.equals(content, measurement.content)
                    && Arrays.equals(minimum, measurement.minimum);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(content) + Arrays.hashCode(minimum);
        }

        @Override
        public String toString() {
            return "Measurement[content=" + Arrays.toString(content) + ", minimum=" + Arrays.toString(minimum) + "]";
        }
    }

    /**
     * What the columns were fitted to.
     *
     * @param width       the width, without the table's insets
     * @param measurement what the shown columns measured
     * @param shares      the reader's widths of the shown columns, {@code null} before the reader's first drag
     */
    private record Fitted(double width, Measurement measurement, double[] shares) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Fitted fit && Double.compare(width, fit.width) == 0
                    && measurement.equals(fit.measurement) && Arrays.equals(shares, fit.shares);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * Double.hashCode(width) + measurement.hashCode()) + Arrays.hashCode(shares);
        }

        @Override
        public String toString() {
            return "Fitted[width=" + width + ", measurement=" + measurement + ", shares=" + Arrays.toString(shares)
                    + "]";
        }
    }
}
