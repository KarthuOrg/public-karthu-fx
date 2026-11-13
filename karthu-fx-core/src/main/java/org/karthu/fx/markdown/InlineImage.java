package org.karthu.fx.markdown;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * An inline image ({@code markdown-image-inline}): an image inside a sentence, one node of its paragraph's or heading's
 * text flow, which the flow lays into the line as one character, the image's bottom on the line's baseline. It
 * follows the size rule of a block image ({@link ImageSize}), the width it shrinks to being the width the flow's text
 * wraps at. Until the image is decoded it shows a small placeholder ({@code markdown-image-placeholder}) with an image
 * icon, the alt text its accessible text; an image that cannot be shown leaves a small error box
 * ({@code markdown-image-error}) with a missing-image icon and the alt text, the address as its tooltip, each sitting
 * on the baseline like text. Loading, the bitmap and the loads are those of every image ({@link ImageNode}).
 */
final class InlineImage extends ImageNode {

    /**
     * Creates the node of an image inside a sentence.
     *
     * @param address the image's address as written
     * @param title   the image's title, empty if none
     * @param alt     the image's alt text
     * @param images  the view's images
     * @param order   where the node comes among all images: in document order within a view
     */
    InlineImage(String address, String title, String alt, DocumentImages images, long order) {
        super(address, title, alt, images, order, "markdown-image-inline", placeholder(alt));
    }

    /**
     * The placeholder: an image icon in a small box, the alt text its accessible text.
     */
    private static Label placeholder(String alt) {
        Label placeholder = new Label(null, FontIcon.of(BootstrapIcons.IMAGE));
        placeholder.getStyleClass().add("markdown-image-placeholder");
        placeholder.setAccessibleText(alt);
        return placeholder;
    }

    /**
     * The missing-image icon and the alt text in a small box, the address as its tooltip.
     */
    @Override
    Node errorBox(ObservableValue<String> note) {
        Label error = new Label(alt(), FontIcon.of(BootstrapIcons.FILE_EARMARK_X));
        error.getStyleClass().add("markdown-image-error");
        Tooltip.install(error, new Tooltip(address()));
        return error;
    }

    /**
     * The limits of the image's size now: the width the text of the flow it lies in wraps at, and those of every
     * image.
     */
    private ImageSize.Limits currentLimits() {
        double textWidth = getParent() instanceof TextSegment flow ? flow.wrapWidth() : -1;
        return images().limits(textWidth);
    }

    @Override
    protected double computePrefWidth(double height) {
        double insets = snappedLeftInset() + snappedRightInset();
        if (!showsPicture()) {
            return insets + box().prefWidth(-1);
        }
        return insets + shownSize(currentLimits()).width();
    }

    @Override
    protected double computePrefHeight(double width) {
        double insets = snappedTopInset() + snappedBottomInset();
        if (!showsPicture()) {
            return insets + box().prefHeight(-1);
        }
        return insets + shownSize(currentLimits()).height();
    }

    @Override
    protected double computeMinWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMaxHeight(double width) {
        return computePrefHeight(width);
    }

    /**
     * The image's bottom, or the baseline of the box's text: a text flow reads it before it lays the node out, so it
     * comes from the preferred sizes, not from the last layout.
     */
    @Override
    public double getBaselineOffset() {
        double top = snappedTopInset();
        if (showsPicture()) {
            return top + shownSize(currentLimits()).height();
        }
        double baseline = box().getBaselineOffset();
        if (baseline == BASELINE_OFFSET_SAME_AS_HEIGHT) {
            baseline = box().prefHeight(-1);
        }
        return top + baseline;
    }

    /**
     * The image at its shown size, or the box filling the node, at the node's top left.
     */
    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        layOutPicture(currentLimits(), left, top);
        if (!showsPicture()) {
            double width = getWidth() - left - snappedRightInset();
            double height = getHeight() - top - snappedBottomInset();
            box().resizeRelocate(left, top, width, height);
        }
    }
}
