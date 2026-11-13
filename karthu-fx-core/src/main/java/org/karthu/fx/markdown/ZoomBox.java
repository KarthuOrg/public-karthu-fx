package org.karthu.fx.markdown;

import javafx.geometry.Orientation;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

/**
 * The content of the view's scroll pane: the box of all blocks, scaled by the view's zoom, as browser zoom scales a
 * page.
 *
 * <p><b>Scaled, laid out at the width over the zoom.</b> The scroll pane fits this box to its viewport's width. The box
 * lays the document out at that width divided by the zoom and scales it by the zoom from its top left corner, so the
 * scaled document is exactly as wide as the viewport and its text wraps to the visible width at every zoom. Its
 * sizes are the document's times the zoom, so the scroll pane scrolls the scaled height. Everything inside the
 * document scales with it: text, tables, code blocks with their scroll bars, the copy buttons, all spacing. The
 * selection, links, the context menu and the copy buttons work in scene coordinates and need nothing more.
 *
 * <p><b>No scale at 1.0.</b> At 1.0 the document has no transform at all, so the view at 100 % is exactly as without
 * a zoom, its text's LCD smoothing included.
 *
 * <p>A new zoom takes effect at the next layout, the scale and the wrap width together: until then the document
 * stays as it is laid out, so the view can note its reading position in a consistent layout first.
 */
final class ZoomBox extends Region {

    private final Region document;
    private final Scale scale = new Scale(1, 1, 0, 0);
    private double zoom = 1.0;

    /**
     * Creates the box around the document, at zoom 1.0.
     *
     * @param document the box of all blocks
     */
    ZoomBox(Region document) {
        this.document = document;
        getChildren().add(document);
    }

    /**
     * The box of all blocks the box scales.
     *
     * @return the document
     */
    Region document() {
        return document;
    }

    /**
     * The zoom the box lays its document out at.
     *
     * @return the factor, 1.0 for 100 %
     */
    double zoom() {
        return zoom;
    }

    /**
     * Sets the zoom; the next layout scales the document and wraps it anew.
     *
     * @param zoom the factor, 0.5 to 2.0
     */
    void setZoom(double zoom) {
        if (this.zoom != zoom) {
            this.zoom = zoom;
            requestLayout();
        }
    }

    /**
     * The document's: a box of text flows is as tall as its width lets it be.
     */
    @Override
    public Orientation getContentBias() {
        return document.getContentBias();
    }

    @Override
    protected double computeMinWidth(double height) {
        return document.minWidth(unscaled(height)) * zoom;
    }

    @Override
    protected double computePrefWidth(double height) {
        return document.prefWidth(unscaled(height)) * zoom;
    }

    @Override
    protected double computeMinHeight(double width) {
        return document.minHeight(unscaled(width)) * zoom;
    }

    @Override
    protected double computePrefHeight(double width) {
        return document.prefHeight(unscaled(width)) * zoom;
    }

    @Override
    protected void layoutChildren() {
        document.resizeRelocate(0, 0, getWidth() / zoom, getHeight() / zoom);
        if (zoom == 1.0) {
            document.getTransforms().remove(scale);
        } else {
            scale.setX(zoom);
            scale.setY(zoom);
            if (!document.getTransforms().contains(scale)) {
                document.getTransforms().add(scale);
            }
        }
    }

    /**
     * A size of this box in the document's own, unscaled pixels; -1, no size given, stays -1.
     */
    private double unscaled(double size) {
        return size < 0 ? size : size / zoom;
    }
}
