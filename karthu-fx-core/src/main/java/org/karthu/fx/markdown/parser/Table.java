package org.karthu.fx.markdown.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A GFM table: a header row, a delimiter row that gives each column its alignment, and the body rows below.
 *
 * <p>Every row has exactly as many cells as the header: GFM pads a shorter body row with empty cells and drops the
 * cells of a longer one beyond the header's.
 *
 * @param header     the header row's cells, one per column
 * @param alignments the alignment of each column, as many as the header has cells
 * @param rows       the body rows in source order, each with one cell per column; empty for a table of a header
 *                   alone
 */
public record Table(List<Cell> header, List<Alignment> alignments, List<List<Cell>> rows) implements Block {

    /**
     * Copies the cells, so the table cannot change afterwards.
     */
    public Table {
        header = List.copyOf(header);
        alignments = List.copyOf(alignments);
        List<List<Cell>> copiedRows = new ArrayList<>();
        for (List<Cell> row : rows) {
            copiedRows.add(List.copyOf(row));
        }
        rows = List.copyOf(copiedRows);
    }

    /**
     * A cell of a table, header or body.
     *
     * @param content the cell's text, parsed into inlines; empty for an empty cell
     */
    public record Cell(List<Inline> content) {

        /**
         * Copies the content, so the cell cannot change afterwards.
         */
        public Cell {
            content = List.copyOf(content);
        }
    }

    /**
     * How a column's cells are aligned, as its cell of the delimiter row says.
     */
    public enum Alignment {
        /**
         * No colon ({@code ---}): the renderer's default, left.
         */
        NONE,
        /**
         * A colon on the left ({@code :---}).
         */
        LEFT,
        /**
         * A colon on both sides ({@code :---:}).
         */
        CENTER,
        /**
         * A colon on the right ({@code ---:}).
         */
        RIGHT
    }
}
