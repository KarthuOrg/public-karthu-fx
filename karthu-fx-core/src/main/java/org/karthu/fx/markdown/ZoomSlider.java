package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * The zoom bar's slider, {@code markdown-zoom-slider}: 50 to 200, the zoom in percent, compact (the theme draws no
 * tick marks and no labels on it), its track filled in the accent colour up to the thumb.
 *
 * <p><b>The filled track.</b> JavaFX's slider draws its track in one colour. The slider lays a region of its own,
 * {@code track-fill}, into the track (the skin's {@code StackPane} with the style class {@code track}, the documented
 * substructure of a slider): not managed, so the track ignores it, and mouse transparent, so a press on it is a press
 * on the track. It spans the track from its left end to the middle of the thumb. The skin moves the thumb without a
 * layout when the value changes, so the region follows the thumb's position and the track's bounds, not the layout.
 *
 * <p><b>No focus.</b> JavaFX's slider takes the focus on every press on its track or thumb, focus traversable or not.
 * This one never takes it: out of the tab order, and a request for the focus does nothing, so a click on it leaves the
 * focus where it is.
 */
final class ZoomSlider extends Slider {

    private final Region fill = new Region();

    /**
     * The skin's track and thumb, found at the first layout; {@code null} before.
     */
    private StackPane track;
    private Node thumb;

    /**
     * Creates the slider at 100 %.
     */
    ZoomSlider() {
        super(ZoomPercent.MIN, ZoomPercent.MAX, ZoomPercent.NORMAL);
        getStyleClass().add("markdown-zoom-slider");
        setFocusTraversable(false);
        fill.getStyleClass().add("track-fill");
        fill.setManaged(false);
        fill.setMouseTransparent(true);
    }

    /**
     * Does nothing: the slider never takes the focus, see the class comment.
     */
    @Override
    public void requestFocus() {
        // A press on the slider leaves the focus where it is.
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (track == null && lookup(".track") instanceof StackPane skinTrack && lookup(".thumb") != null) {
            track = skinTrack;
            thumb = lookup(".thumb");
            track.getChildren().add(fill);
            thumb.layoutXProperty().addListener(observable -> placeFill());
            thumb.layoutBoundsProperty().addListener(observable -> placeFill());
            track.layoutXProperty().addListener(observable -> placeFill());
            track.layoutBoundsProperty().addListener(observable -> placeFill());
        }
        placeFill();
    }

    /**
     * Lays the fill over the track from its left end to the middle of the thumb.
     */
    private void placeFill() {
        if (track == null) {
            return;
        }
        double thumbMiddle = thumb.getLayoutX() + thumb.getLayoutBounds().getWidth() / 2;
        // A slider narrower than its thumb (hidden by the bar, never given room) lays its track out with a negative
        // width.
        double trackWidth = Math.max(0, track.getWidth());
        double width = Math.clamp(thumbMiddle - track.getLayoutX(), 0, trackWidth);
        fill.resizeRelocate(0, 0, width, track.getHeight());
    }
}
