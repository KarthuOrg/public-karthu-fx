package org.karthu.fx.markdown;

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

/**
 * The Markdown view's zoom at work, beside the rendering, the selection and the API the view keeps: it applies the
 * zoom property to the document, keeping the reader's place, and changes the zoom for the reader, from the zoom bar,
 * the wheel and the shortcuts.
 *
 * <p><b>Applying a zoom.</b> Every change of the zoom property, whoever made it, is applied to the {@link ZoomBox}
 * limited and rounded ({@link ZoomProperty#normalize}), keeping the line at the top of the visible area at the top.
 * The point of the document there is noted in the current layout ({@link ReadingAnchor}), and the view's next layout,
 * which scales the document and wraps it anew, scrolls it back there ({@link #restoreReadingPosition()}). A zoom
 * applied again before that layout notes the point anew in the same, not yet changed layout. An image that arrives
 * above the line at the top keeps that line in place the same way ({@link #keepReadingPosition()}), and so does a new
 * version of the text, whose new document the view finds the noted place in ({@link #readingPosition()},
 * {@link #keepReadingPosition(ReadingAnchor)}).
 *
 * <p><b>The reader's zoom.</b> The bar, the wheel and the shortcuts change the zoom through {@link #zoomTo(int)} and
 * {@link #step(int)}, the one place that knows the application's binding: while the application has bound the zoom
 * they set nothing, as the bar shows the bound value then. A step goes by {@link ZoomPercent#step}.
 *
 * <p><b>Wheel.</b> A filter on the view's scroll pane, so it runs before the code blocks and tables, which pass the
 * wheel on: the shortcut key plus a vertical wheel movement over the document goes one preset per notch
 * ({@link ZoomWheel}), keeping the point of the document under the pointer there, and is consumed, so nothing
 * scrolls. Without the shortcut key, without a vertical movement or while the reader may not zoom, the wheel scrolls
 * as it does without a zoom.
 *
 * <p><b>Shortcuts.</b> The view hands its key presses here ({@link #keyPressed}): the shortcut key plus {@code +}
 * one preset larger, plus {@code -} one smaller, plus {@code 0} to 100 % ({@link ZoomKey}), keeping the line at the
 * top. They are consumed while the reader may zoom and pass on otherwise.
 */
final class DocumentZoom {

    private final DoubleProperty zoom;
    private final ObservableBooleanValue readerZooms;
    private final ScrollPane scrollPane;
    private final ZoomBox zoomBox;
    private final DocumentSelection selection;
    private final Runnable layoutRequest;
    private final ZoomWheel wheel = new ZoomWheel();

    /**
     * The reader's place, noted before a new zoom and restored by the view's next layout; {@code null} while none
     * waits.
     */
    private ReadingAnchor readingAnchor;

    /**
     * The height in the viewport the reader's place is kept at, from the viewport's top edge.
     */
    private double readingAnchorHeight;

    /**
     * Creates the zoom of a view's document and applies every change of the zoom property from now on.
     *
     * @param zoom          the view's zoom property
     * @param readerZooms   whether the reader may zoom by the wheel and the shortcuts: the zoom bar's visibility
     * @param scrollPane    the view's scroll pane, whose content is the zoom box
     * @param zoomBox       the box that scales the document
     * @param selection     the document's selection, which knows its segments in document order
     * @param layoutRequest asks the view for a layout, which restores the reader's place
     */
    DocumentZoom(DoubleProperty zoom, ObservableBooleanValue readerZooms, ScrollPane scrollPane, ZoomBox zoomBox,
                 DocumentSelection selection, Runnable layoutRequest) {
        this.zoom = zoom;
        this.readerZooms = readerZooms;
        this.scrollPane = scrollPane;
        this.zoomBox = zoomBox;
        this.selection = selection;
        this.layoutRequest = layoutRequest;
        zoom.addListener(observable -> applyZoom(ZoomProperty.normalize(zoom.get()), 0));
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> wheelMoved(event));
    }

    /**
     * The view's zoom, to follow: every change, whoever made it.
     *
     * @return the zoom property, as a factor
     */
    ObservableDoubleValue zoom() {
        return zoom;
    }

    /**
     * The zoom the view shows, in percent.
     *
     * @return the whole percent of the zoom, limited to 50 to 200
     */
    int percent() {
        return ZoomPercent.of(zoom.get());
    }

    /**
     * Sets the zoom for the reader, keeping the line at the top; nothing while the application has bound it.
     *
     * @param percent the zoom in percent, 50 to 200
     */
    void zoomTo(int percent) {
        zoomTo(percent, 0);
    }

    /**
     * Steps the zoom for the reader by presets ({@link ZoomPercent#step}), keeping the line at the top; nothing while
     * the application has bound it.
     *
     * @param presets how many presets to go: positive for larger, negative for smaller
     */
    void step(int presets) {
        zoomTo(ZoomPercent.step(percent(), presets), 0);
    }

    /**
     * Zooms by a shortcut ({@link ZoomKey}) while the reader may zoom, and consumes the key press then, so an
     * accelerator of the application's menu with the same keys does not fire while the focus is in the view. Any
     * other key press, and every one while the reader may not zoom, passes on untouched.
     *
     * @param event a key press that reached the view
     */
    void keyPressed(KeyEvent event) {
        if (!readerZooms.get()) {
            return;
        }
        ZoomKey key = ZoomKey.of(event);
        if (key == null) {
            return;
        }
        event.consume();
        switch (key) {
            case IN -> step(1);
            case OUT -> step(-1);
            case RESET -> zoomTo(ZoomPercent.NORMAL);
        }
    }

    /**
     * Forgets a reader's place that waits for the next layout: the view shows a new text, which starts at the top.
     */
    void forgetReadingPosition() {
        readingAnchor = null;
    }

    /**
     * Keeps the line at the top of the visible area in place through the next layout, while something above it
     * changes its height: an image that arrives. The line is noted in the current layout, and the next layout scrolls
     * it back to the top. A place that waits already, for a zoom or an earlier image, stays: it was noted in the
     * layout before both changes.
     */
    void keepReadingPosition() {
        if (readingAnchor == null) {
            noteReadingPosition(0);
            layoutRequest.run();
        }
    }

    /**
     * The reader's place in the shown document, for a new version of the text: the place that waits for the next
     * layout, noted before a zoom or an arriving image, else the line at the top of the visible area now. It is noted
     * in the current layout; {@link #keepReadingPosition(ReadingAnchor)} keeps it, moved to the new document, at the
     * same height of the viewport.
     *
     * @return the place; {@code null} before the view is laid out and for a document without segments other than
     *         block images
     */
    ReadingAnchor readingPosition() {
        if (readingAnchor == null) {
            noteReadingPosition(0);
        }
        return readingAnchor;
    }

    /**
     * Keeps a place of a new version of the text, found from {@link #readingPosition()}, through the next layout at
     * the height of the viewport that place was noted at. Images above it that arrive before that layout keep it, as
     * they keep a zoom's place: it was noted before their change.
     *
     * @param anchor the place in the new document
     */
    void keepReadingPosition(ReadingAnchor anchor) {
        readingAnchor = anchor;
        layoutRequest.run();
    }

    /**
     * Scrolls the content so that the reader's place noted before a new zoom is at its height in the viewport again,
     * as far as the scroll range allows. The view calls it in its layout, once its scroll pane has its new size; it
     * lays the content out at the new zoom now, so the place can be found in it before anything is drawn, and the
     * scroll pane then only moves the content.
     */
    void restoreReadingPosition() {
        if (readingAnchor == null) {
            return;
        }
        ReadingAnchor anchor = readingAnchor;
        readingAnchor = null;
        scrollPane.layout();
        double contentY = zoomBox.document().localToParent(0, anchor.documentY()).getY();
        VerticalScroll.scrollTo(scrollPane, contentY - readingAnchorHeight);
    }

    /**
     * Sets the zoom for the reader, keeping the point of the document at a height of the viewport there. The one
     * place that respects the application's binding: a bound zoom is not set.
     */
    private void zoomTo(int percent, double anchorY) {
        if (zoom.isBound()) {
            return;
        }
        double factor = ZoomPercent.factor(percent);
        // Applied here first, so the property's change finds the zoom applied and keeps this point, not the top.
        applyZoom(factor, anchorY);
        zoom.set(factor);
    }

    /**
     * Applies a zoom to the content, keeping the point of the document at a height of the viewport at that height:
     * the point is noted in the current layout, and the next layout scrolls it back there. A zoom the content already
     * has changes nothing.
     *
     * @param factor  the zoom, already limited and rounded
     * @param anchorY the height in the viewport, from its top edge, whose point of the document stays there: 0 for the
     *                line at the top, the pointer's height for the wheel
     */
    private void applyZoom(double factor, double anchorY) {
        if (factor == zoomBox.zoom()) {
            return;
        }
        readingAnchor = null;
        noteReadingPosition(anchorY);
        zoomBox.setZoom(factor);
        layoutRequest.run();
    }

    /**
     * Notes the point of the document at a height of the viewport in the current layout, to be restored at that
     * height by the next layout; nothing before the view is laid out.
     *
     * @param anchorY the height in the viewport, from its top edge
     */
    private void noteReadingPosition(double anchorY) {
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        if (viewportHeight > 0) {
            Region document = zoomBox.document();
            double contentY = VerticalScroll.offset(scrollPane) + Math.clamp(anchorY, 0, viewportHeight);
            double documentY = document.parentToLocal(0, contentY).getY();
            readingAnchor = ReadingAnchor.at(selection.segments(), document, documentY);
            readingAnchorHeight = Math.clamp(anchorY, 0, viewportHeight);
        }
    }

    /**
     * The shortcut key plus a vertical wheel movement over the document zooms, see the class comment.
     */
    private void wheelMoved(ScrollEvent event) {
        if (!readerZooms.get() || event.getDeltaY() == 0) {
            return;
        }
        if (!event.isShortcutDown()) {
            wheel.reset();
            return;
        }
        event.consume();
        int notches = wheel.notches(event);
        if (notches == 0) {
            return;
        }
        // The pointer's height in the content, less the scroll offset: its height in the viewport.
        double pointerY = zoomBox.sceneToLocal(event.getSceneX(), event.getSceneY()).getY();
        zoomTo(ZoomPercent.step(percent(), notches), pointerY - VerticalScroll.offset(scrollPane));
    }
}
