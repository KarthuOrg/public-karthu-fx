package org.karthu.fx.markdown;

import javafx.scene.control.ScrollPane;

/**
 * A scroll pane's vertical position in its content's pixels. The scroll pane holds it as a value between its
 * {@code vmin} and {@code vmax}, which stand for the top and the bottom of the range its content can scroll: the
 * content's height less the viewport's. The view's reading position and the selection's scrolling along a drag
 * convert between the two here.
 */
final class VerticalScroll {

    private VerticalScroll() {
    }

    /**
     * How far a scroll pane's content can scroll: its height less the viewport's, in the content's pixels as the
     * scroll pane lays it out (a scaled content's scaled pixels).
     *
     * @param scrollPane the scroll pane
     * @return the range, 0 or less when the content fits
     */
    static double range(ScrollPane scrollPane) {
        return scrollPane.getContent().getLayoutBounds().getHeight() - scrollPane.getViewportBounds().getHeight();
    }

    /**
     * How far a scroll pane's content is scrolled down.
     *
     * @param scrollPane the scroll pane
     * @return the distance in the content's pixels, 0 when the content fits
     */
    static double offset(ScrollPane scrollPane) {
        double range = range(scrollPane);
        double span = scrollPane.getVmax() - scrollPane.getVmin();
        if (range <= 0 || span <= 0) {
            return 0;
        }
        return (scrollPane.getVvalue() - scrollPane.getVmin()) / span * range;
    }

    /**
     * Scrolls a scroll pane's content down by a distance from its top, as far as the scroll range allows.
     *
     * @param scrollPane the scroll pane
     * @param offset     the distance in the content's pixels
     */
    static void scrollTo(ScrollPane scrollPane, double offset) {
        double range = range(scrollPane);
        double fraction = range > 0 ? Math.clamp(offset / range, 0, 1) : 0;
        scrollPane.setVvalue(scrollPane.getVmin() + fraction * (scrollPane.getVmax() - scrollPane.getVmin()));
    }
}
