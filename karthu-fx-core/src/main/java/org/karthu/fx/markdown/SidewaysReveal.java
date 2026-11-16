package org.karthu.fx.markdown;

import javafx.geometry.Bounds;

/**
 * How a block that scrolls sideways shows the match the search goes to ({@link Searchable#revealSideways}): a code or
 * source block whose line is wider than the block ({@link MonospaceBlock}), a table wider than the view
 * ({@link TableBlock}). Both reveal a match alike, in their scrolled content's coordinates, with the same room beside
 * it; each turns the offset into its own scroll value.
 */
final class SidewaysReveal {

    /**
     * The room beside a match a block scrolls sideways to, in the document's pixels: about four characters of code,
     * enough to keep a match at the end of a code block's first line clear of the copy button over the block's top
     * right corner, and beyond a table cell's padding.
     */
    static final double MARGIN = 32;

    private SidewaysReveal() {
    }

    /**
     * Where a block scrolls sideways to show a match whole: nowhere if it shows whole already; else so that it shows
     * whole with the margin on the side it came in from, or from its start with the margin if it is wider than the
     * visible width less the margins.
     *
     * @param match        the match's bounds in the scrolled content's coordinates
     * @param offset       how far the content is scrolled sideways now, in the same coordinates
     * @param visibleWidth the width of the content the block shows
     * @return the offset to scroll to, not limited to the scroll range; {@code offset} itself if the match shows whole
     */
    static double offset(Bounds match, double offset, double visibleWidth) {
        if (match.getMinX() >= offset && match.getMaxX() <= offset + visibleWidth) {
            return offset;
        }
        if (match.getMinX() < offset || match.getWidth() + 2 * MARGIN > visibleWidth) {
            return match.getMinX() - MARGIN;
        }
        return match.getMaxX() + MARGIN - visibleWidth;
    }
}
