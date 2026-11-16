package org.karthu.fx.markdown;

import javafx.beans.value.ObservableDoubleValue;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.Border;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The box of all blocks ({@code markdown-document}): a {@code VBox} whose left and right padding the view's margins
 * replace, in the document's own pixels, so that the zoom scales them with everything else in the document.
 *
 * <p><b>The theme's padding.</b> The box takes {@code -fx-padding} from CSS into a property of its own, not into its
 * padding, which is the box's alone to set: CSS would otherwise overwrite a padding set in code. Its padding is the
 * theme's, its left and right side replaced by the margin set for that side. So a stylesheet that sets the document's
 * padding keeps working: its top and bottom always, a side as long as no margin is set for it. A margin is set while
 * it is a number; a value below 0 counts as 0.
 *
 * <p><b>Shrinking.</b> Where the box is narrower than its margins plus 300 pixels for the text ({@link #TEXT_WIDTH}),
 * the theme's padding of a side without a margin and a border, both margins shrink in their proportion, so that the
 * text keeps its 300 pixels; with no room beyond those they are 0. The box's width is the view's visible width over
 * the zoom, so in the view's pixels the rule reads the same with everything times the zoom. So the margins never make
 * the document wider than its box, and never bring a horizontal scroll bar: the box's minimum width leaves them out,
 * as they shrink to 0 rather than widen it.
 *
 * <p><b>Measured at the width asked.</b> The margins depend on the width, and the scroll pane asks the box's height for
 * a width before it gives it that width. A {@code VBox} measures with the padding it has, so the box measures at the
 * width that leaves its blocks as much room with that padding as the margins of the width asked leave them. It takes
 * the margins of its width as it gets that width, in the zoom box's layout, before it lays its blocks out, so the
 * scroll pane's measure and the box's layout agree within one pass.
 */
final class DocumentBox extends VBox {

    /**
     * The width the text keeps while the margins shrink, in the document's pixels.
     */
    static final double TEXT_WIDTH = 300;

    private static final CssMetaData<DocumentBox, Insets> THEME_PADDING =
            new CssMetaData<>("-fx-padding", StyleConverter.getInsetsConverter(), Insets.EMPTY) {

                @Override
                public boolean isSettable(DocumentBox box) {
                    return !box.themePadding.isBound();
                }

                @Override
                public StyleableProperty<Insets> getStyleableProperty(DocumentBox box) {
                    return box.themePadding;
                }
            };

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA = cssMetaData();

    /**
     * The padding CSS sets on the box, {@code -fx-padding}: the theme's or an application's stylesheet's.
     */
    private final StyleableObjectProperty<Insets> themePadding =
            new SimpleStyleableObjectProperty<>(THEME_PADDING, this, "themePadding", Insets.EMPTY);

    private final ObservableDoubleValue leftMargin;
    private final ObservableDoubleValue rightMargin;

    /**
     * Creates the box, which follows the view's margins.
     *
     * @param leftMargin  the view's left margin: not a number for the theme's padding
     * @param rightMargin the view's right margin: not a number for the theme's padding
     */
    DocumentBox(ObservableDoubleValue leftMargin, ObservableDoubleValue rightMargin) {
        this.leftMargin = leftMargin;
        this.rightMargin = rightMargin;
        themePadding.addListener(observable -> applyMargins());
        leftMargin.addListener(observable -> applyMargins());
        rightMargin.addListener(observable -> applyMargins());
        // The width comes from the zoom box's layout, before the box lays its blocks out.
        widthProperty().addListener(observable -> applyMargins());
    }

    private static List<CssMetaData<? extends Styleable, ?>> cssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> metaData = new ArrayList<>();
        for (CssMetaData<? extends Styleable, ?> data : VBox.getClassCssMetaData()) {
            if (!data.getProperty().equals(THEME_PADDING.getProperty())) {
                metaData.add(data);
            }
        }
        metaData.add(THEME_PADDING);
        return Collections.unmodifiableList(metaData);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA;
    }

    @Override
    protected double computePrefHeight(double width) {
        return super.computePrefHeight(measuredWidth(width));
    }

    @Override
    protected double computeMinHeight(double width) {
        return super.computeMinHeight(measuredWidth(width));
    }

    /**
     * The blocks' widest minimum with the theme's padding of a side without a margin, without the margins: they
     * shrink to 0 rather than make the document wider than its box.
     */
    @Override
    protected double computeMinWidth(double height) {
        Insets padding = getPadding();
        double margins = (isSet(leftMargin) ? snapSpaceX(padding.getLeft()) : 0)
                + (isSet(rightMargin) ? snapSpaceX(padding.getRight()) : 0);
        return super.computeMinWidth(height) - margins;
    }

    /**
     * The padding the box has at a width: the theme's, its sides replaced by the margins set, shrunk as the class
     * comment says.
     *
     * @param width the box's width
     * @return the padding
     */
    private Insets paddingAt(double width) {
        Insets theme = themePadding.get();
        boolean leftSet = isSet(leftMargin);
        boolean rightSet = isSet(rightMargin);
        if (!leftSet && !rightSet) {
            return theme;
        }
        double left = leftSet ? margin(leftMargin) : 0;
        double right = rightSet ? margin(rightMargin) : 0;
        // What the sides hold beside the margins: the theme's padding of a side without a margin, a border.
        Border border = getBorder();
        double fixed = (leftSet ? 0 : theme.getLeft()) + (rightSet ? 0 : theme.getRight())
                + (border == null ? 0 : border.getInsets().getLeft() + border.getInsets().getRight());
        double room = Math.max(0, width - fixed - TEXT_WIDTH);
        if (left + right > room) {
            // In their proportion, also for margins too large to add up: the left one's share of the room.
            double shrunkLeft = left == 0 ? 0 : room / (1 + right / left);
            left = shrunkLeft;
            right = room - shrunkLeft;
        }
        return new Insets(theme.getTop(), rightSet ? right : theme.getRight(), theme.getBottom(),
                leftSet ? left : theme.getLeft());
    }

    /**
     * Gives the box the padding of its width, if it does not have it yet.
     */
    private void applyMargins() {
        Insets padding = paddingAt(getWidth());
        if (!padding.equals(getPadding())) {
            setPadding(padding);
        }
    }

    /**
     * The width at which a {@code VBox} with the padding the box has now leaves its blocks the room the margins of a
     * width leave them: the width less the difference of the snapped sides, as {@code VBox} snaps them.
     *
     * @param width the width asked, -1 for none
     * @return the width to measure at
     */
    private double measuredWidth(double width) {
        if (width < 0) {
            return width;
        }
        Insets now = getPadding();
        Insets then = paddingAt(width);
        return width + snapSpaceX(now.getLeft()) + snapSpaceX(now.getRight()) - snapSpaceX(then.getLeft())
                - snapSpaceX(then.getRight());
    }

    /**
     * Whether a margin is set: a number.
     */
    private static boolean isSet(ObservableDoubleValue margin) {
        return !Double.isNaN(margin.get());
    }

    /**
     * A margin that is set as the box applies it: below 0 as 0, beyond the largest number as that.
     */
    private static double margin(ObservableDoubleValue margin) {
        return Math.clamp(margin.get(), 0, Double.MAX_VALUE);
    }
}
