package org.karthu.fx.markdown;

import javafx.beans.property.SimpleDoubleProperty;

/**
 * The Markdown view's zoom: a factor, 1.0 being 100 %, that stores what is set limited and rounded to whole percent as
 * {@link ZoomPercent#of} does, not a number as 1.0. A bound property keeps the value it is bound to; {@link #normalize}
 * gives the zoom the view applies for any value.
 */
final class ZoomProperty extends SimpleDoubleProperty {

    /**
     * Creates the zoom of a view, 1.0.
     *
     * @param view the view the property belongs to
     */
    ZoomProperty(Object view) {
        super(view, "zoom", 1.0);
    }

    @Override
    public void set(double newValue) {
        super.set(normalize(newValue));
    }

    /**
     * The zoom a value stands for: limited to 0.5 to 2.0, rounded half up to whole percent (two decimals of the
     * factor), not a number as 1.0, all of it by {@link ZoomPercent}.
     *
     * @param value any factor
     * @return the factor the view applies
     */
    static double normalize(double value) {
        return ZoomPercent.factor(ZoomPercent.of(value));
    }
}
