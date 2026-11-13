package org.karthu.fx.form;

import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.TraversalDirection;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

/**
 * A text the user can select and copy but not change, as a form row's label and messages are: a read-only text area
 * without frame, background or scroll bars that looks like a label, wrapped at its width and always as high as its
 * wrapped text, so no line is ever cut off or shortened with an ellipsis.
 *
 * <p><b>Selecting.</b> A drag, a double or triple click and the shortcut key plus A select, the shortcut key plus C
 * and the context menu's <i>Copy</i> copy, as in any text field. The text is not in the focus traversal: Tab passes it,
 * and Tab or Shift plus Tab inside it moves the focus on instead of inserting a tab.
 *
 * <p><b>Size.</b> Its preferred width is its text on one line, its height the text wrapped at the width it is given.
 * Asked for its height without a width, as a parent that ignores the content bias does, it answers for the width it
 * has, and when its width changes so that its wrapped height changes it asks its parent for a new layout. The
 * measuring assumes the theme's rules for {@code form-selectable-text}, which take every padding and inset away.
 *
 * <p>Style class {@code form-selectable-text}. Not exported: an implementation of the row's look.
 */
final class SelectableText extends TextArea {

    /** Room for the caret at the end of a line, so a text on one line at its preferred width never wraps. */
    private static final double CARET_ALLOWANCE = 2;

    /** Measures the text as the area's skin lays it out: same font, same wrapping width. */
    private final Text measure = new Text();

    /**
     * Creates an empty text: not editable, wrapping, and out of the tab order.
     */
    SelectableText() {
        getStyleClass().add("form-selectable-text");
        setEditable(false);
        setWrapText(true);
        setFocusTraversable(false);
        // The text area's skin lays its text out with these bounds; the plain logical bounds are a few pixels lower.
        measure.setBoundsType(TextBoundsType.LOGICAL_VERTICAL_CENTER);
        measure.setTextOrigin(VPos.TOP);

        textProperty().addListener((observable, oldText, newText) -> requestLayout());
        fontProperty().addListener((observable, oldFont, newFont) -> requestLayout());
        widthProperty().addListener((observable, oldWidth, newWidth) -> {
            if (Math.abs(heightFor(newWidth.doubleValue()) - getHeight()) > 0.5) {
                requestLayout();
            }
        });
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB && !event.isShortcutDown() && !event.isAltDown()) {
                event.consume();
                requestFocusTraversal(event.isShiftDown() ? TraversalDirection.PREVIOUS : TraversalDirection.NEXT);
            }
        });
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefWidth(double height) {
        measure.setFont(getFont());
        measure.setWrappingWidth(0);
        measure.setText(textOrEmpty());
        return snappedLeftInset() + Math.ceil(measure.getLayoutBounds().getWidth()) + CARET_ALLOWANCE
                + snappedRightInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        double forWidth = width;
        if (forWidth < 0) {
            forWidth = getWidth() > 0 ? getWidth() : prefWidth(-1);
        }
        return heightFor(forWidth);
    }

    @Override
    protected double computeMinWidth(double height) {
        return snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computeMinHeight(double width) {
        // Never squeezed below the wrapped text: a cut-off line would hide part of a message.
        return computePrefHeight(width);
    }

    @Override
    protected double computeMaxHeight(double width) {
        return computePrefHeight(width);
    }

    /** The height of the text wrapped at the given width, with the area's insets. */
    private double heightFor(double width) {
        double contentWidth = Math.max(1, width - snappedLeftInset() - snappedRightInset());
        measure.setFont(getFont());
        measure.setWrappingWidth(contentWidth);
        measure.setText(textOrEmpty());
        return snappedTopInset() + Math.ceil(measure.getLayoutBounds().getHeight()) + snappedBottomInset();
    }

    private String textOrEmpty() {
        String text = getText();
        return text == null ? "" : text;
    }
}
