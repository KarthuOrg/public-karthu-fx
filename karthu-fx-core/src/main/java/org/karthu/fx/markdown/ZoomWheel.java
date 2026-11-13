package org.karthu.fx.markdown;

import javafx.scene.input.ScrollEvent;

/**
 * Counts the notches of the wheel that zooms the Markdown view: the shortcut key plus a vertical wheel movement over
 * the document goes one preset per notch.
 *
 * <p><b>A notch</b> is the step the platform reports for the wheel: JavaFX gives a wheel event's delta in pixels, the
 * notches the platform sent times the event's multiplier (40 pixels at Windows' default speed of three lines, more at
 * a faster one), so the delta over the multiplier is the notches, whatever the speed. An event without a multiplier
 * above one pixel, such as one a touch screen sends, counts 40 pixels as a notch.
 *
 * <p><b>Smaller deltas add up.</b> A touchpad sends a scroll as many small deltas; they add up until they make a whole
 * notch, and what is left over stays for the next event. A movement in the other direction starts the count over, so
 * the zoom turns back at once, and so does a wheel movement without the shortcut key.
 */
final class ZoomWheel {

    /**
     * The pixels of a notch when an event gives no multiplier: JavaFX's default multiplier.
     */
    private static final double DEFAULT_NOTCH = 40;

    /**
     * How far short of a whole notch a sum of deltas may fall and still make it.
     */
    private static final double ROUNDING = 1e-9;

    /**
     * The notches counted but not yet stepped: positive up, negative down, always less than one either way.
     */
    private double counted;

    /**
     * Counts a wheel event's vertical movement and takes the whole notches it completes.
     *
     * @param event a wheel event with the shortcut key
     * @return the whole notches to step: positive for the wheel turned up (larger), negative for down (smaller), 0
     *         while the movement does not make a notch yet
     */
    int notches(ScrollEvent event) {
        double multiplier = event.getMultiplierY() > 1 ? event.getMultiplierY() : DEFAULT_NOTCH;
        double moved = event.getDeltaY() / multiplier;
        if (moved * counted < 0) {
            counted = 0;
        }
        counted += moved;
        // Ten deltas of a tenth make a notch, though their binary sum falls short of one by a rounding error.
        int whole = (int) (counted + Math.signum(counted) * ROUNDING);
        counted -= whole;
        return whole;
    }

    /**
     * Starts the count over: the wheel moved without the shortcut key.
     */
    void reset() {
        counted = 0;
    }
}
