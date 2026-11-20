package org.karthu.fx.markdown;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.OptionalInt;

/**
 * A block image ({@code markdown-image-block}): an image alone in its paragraph, shown as a block of its own. Until
 * the image is decoded it shows a placeholder ({@code markdown-image-placeholder}), one line with an image icon and
 * the alt text; then the image ({@code markdown-image}) at its shown size ({@link ImageSize}): its natural size,
 * shrunk to the block's width, the width the text wraps at, and to the visible height at the current zoom. An image
 * that cannot be shown leaves the error box ({@code markdown-image-error}): a missing-image icon, the alt text and a
 * note why, the address as its tooltip. Loading, the bitmap and the loads are those of every image
 * ({@link ImageNode}).
 *
 * <p>The block is a segment of the view's selection across the document ({@link DocumentSelection}), which takes the
 * image whole, as it takes a table: it has two positions, before and after the image. A selection that passes over it
 * includes it, and copies it as its alt text, a block of its own set apart like the paragraph it stands for.
 *
 * <p><b>Open button.</b> While the block shows the image and the pointer is over it, a button
 * ({@code markdown-image-open}) over the image's top right corner opens it in the full size window, as the code
 * block's copy button lies over its block. It lies above the selection's highlight, and a press on it stays its own,
 * never starting a selection.
 */
final class ImageBlock extends ImageNode implements Segment {

    private final List<BlockContainer> containers;
    private final boolean spaced;

    /**
     * The source line of the paragraph the image stands for.
     */
    private final int sourceLine;

    /**
     * The space between the image's edges and its open button.
     */
    private static final double OPEN_BUTTON_MARGIN = 4;

    /**
     * The button over the image's top right corner that opens it at full size, a child while the block shows the
     * image, visible while the pointer is over the block.
     */
    private final Button openButton;

    /**
     * Creates the block of an image.
     *
     * @param address    the image's address as written
     * @param title      the image's title, empty if none
     * @param alt        the image's alt text
     * @param images     the view's images
     * @param order      where the block comes among all images: in document order within a view
     * @param containers the quotes and list items around the block, the outermost first
     * @param spaced     whether copied text sets the block apart by blank lines, as the paragraph it stands for
     * @param sourceLine the 1-based source line of the paragraph the image stands for
     */
    ImageBlock(String address, String title, String alt, DocumentImages images, long order,
               List<BlockContainer> containers, boolean spaced, int sourceLine) {
        super(address, title, alt, images, order, "markdown-image-block", placeholder(alt));
        this.containers = List.copyOf(containers);
        this.spaced = spaced;
        this.sourceLine = sourceLine;
        openButton = openButton(images.openImageText());
        openButton.setOnAction(event -> images.open(this));
        // The pointer is over the block exactly while it is over what the block shows, the image or the button: the
        // block picks no point of its own, so its empty part beside a narrow image is never picked (ImageNode).
        addEventHandler(MouseEvent.MOUSE_ENTERED, event -> openButton.setVisible(true));
        addEventHandler(MouseEvent.MOUSE_EXITED, event -> openButton.setVisible(false));
    }

    /**
     * The open button: the theme's icon button with the fullscreen icon, laid out by the block, hidden at first, drawn
     * and picked above the selection's highlight. The mouse on it is its own: a press, a drag and a release never
     * reach the document's selection, and the click after its action never reaches the block, which would open the
     * image a second time.
     */
    private static Button openButton(ObservableValue<String> text) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", "markdown-image-open");
        button.setGraphic(FontIcon.of(BootstrapIcons.FULLSCREEN));
        // Bound, not copied: a new text applies to the buttons shown. A binding refers to its target weakly, so the
        // view's property does not keep the buttons of a replaced document alive.
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(text);
        button.setTooltip(tooltip);
        button.accessibleTextProperty().bind(text);
        button.setManaged(false);
        button.setVisible(false);
        // Above the highlight, which comes as a later child: a lower view order lies on top.
        button.setViewOrder(-1);
        // Consumed in the button's property handlers, which JavaFX calls after every handler added to the button: the
        // button's own behaviour, which its skin adds, must see the press and the release first, or it ignores the
        // consumed press, never arms and never fires.
        button.setOnMousePressed(event -> event.consume());
        button.setOnMouseDragged(event -> event.consume());
        button.setOnMouseReleased(event -> event.consume());
        button.setOnMouseClicked(event -> event.consume());
        return button;
    }

    /**
     * The open button lies over the image.
     */
    @Override
    void pictureShown() {
        getChildren().add(openButton);
    }

    /**
     * The placeholder: an image icon and the alt text, on one line.
     */
    private static Label placeholder(String alt) {
        Label placeholder = new Label(alt, FontIcon.of(BootstrapIcons.IMAGE));
        placeholder.getStyleClass().add("markdown-image-placeholder");
        return placeholder;
    }

    /**
     * The missing-image icon, the alt text and the note, the address as its tooltip.
     */
    @Override
    Node errorBox(ObservableValue<String> note) {
        Label altLabel = new Label(alt());
        Label noteLabel = new Label();
        // Bound, not copied: a new text applies to the boxes shown. A binding refers to its target weakly, so the
        // view's property does not keep the boxes of a replaced document alive.
        noteLabel.textProperty().bind(note);
        HBox error = new HBox(FontIcon.of(BootstrapIcons.FILE_EARMARK_X), altLabel, noteLabel);
        error.getStyleClass().add("markdown-image-error");
        Tooltip.install(error, new Tooltip(address()));
        return error;
    }

    /**
     * The size the image shows at for a width of the block's content.
     */
    private ImageSize shownSize(double contentWidth) {
        return shownSize(images().limits(contentWidth));
    }

    /**
     * A box of an image is as tall as its width lets it be.
     */
    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefWidth(double height) {
        double insets = snappedLeftInset() + snappedRightInset();
        if (!showsPicture()) {
            return insets + box().prefWidth(-1);
        }
        return insets + shownSize(-1).width();
    }

    @Override
    protected double computePrefHeight(double width) {
        double insets = snappedTopInset() + snappedBottomInset();
        if (!showsPicture()) {
            return insets + box().prefHeight(-1);
        }
        double contentWidth = width < 0 ? -1 : width - snappedLeftInset() - snappedRightInset();
        return insets + shownSize(contentWidth).height();
    }

    /**
     * The image at its shown size, or the box as wide as it asks to be, at most the block's width, at the block's
     * top left. The limits of this layout are those of the block's width.
     */
    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double contentWidth = getWidth() - left - snappedRightInset();
        ImageSize.Limits limits = images().limits(contentWidth);
        layOutPicture(limits, left, top);
        if (!showsPicture()) {
            Node box = box();
            double width = Math.min(box.prefWidth(-1), contentWidth);
            box.resizeRelocate(left, top, width, box.prefHeight(width));
            return;
        }
        double imageWidth = shownSize(limits).width();
        double buttonWidth = snapSizeX(openButton.prefWidth(-1));
        double buttonHeight = snapSizeY(openButton.prefHeight(-1));
        openButton.resizeRelocate(snapPositionX(left + imageWidth - OPEN_BUTTON_MARGIN - buttonWidth),
                snapPositionY(top + OPEN_BUTTON_MARGIN), buttonWidth, buttonHeight);
    }

    // --- The block as a segment of the document selection ----------------------------------------------------------

    @Override
    public Node node() {
        return this;
    }

    /**
     * One: the image is one piece of the selection.
     */
    @Override
    public int length() {
        return 1;
    }

    /**
     * Before the image for a point above the block's vertical middle, after it for any other: a drag that ends in the
     * upper half leaves the image out, one that ends in the lower half takes it, whichever way it goes.
     */
    @Override
    public int positionAt(double sceneX, double sceneY) {
        Bounds bounds = localToScene(getLayoutBounds());
        return sceneY < bounds.getCenterY() ? 0 : 1;
    }

    /**
     * The whole image.
     */
    @Override
    public Range wordAt(double sceneX, double sceneY) {
        return new Range(0, 1);
    }

    /**
     * The whole image, as {@link #wordAt(double, double)}.
     */
    @Override
    public Range paragraphAt(double sceneX, double sceneY) {
        return new Range(0, 1);
    }

    /**
     * The selection's highlight over the image for the range {@code 0} to {@code 1}, none for an empty range.
     */
    @Override
    public void select(int start, int end) {
        showSelected(start < end);
    }

    /**
     * The alt text for the range {@code 0} to {@code 1}; nothing for an empty range.
     */
    @Override
    public String text(int start, int end) {
        return start < end ? alt() : "";
    }

    @Override
    public List<BlockContainer> containers() {
        return containers;
    }

    @Override
    public boolean spaced() {
        return spaced;
    }

    /**
     * The line of the paragraph the image stands for.
     */
    @Override
    public int sourceLineAt(double sceneY) {
        return sourceLine;
    }

    @Override
    public OptionalInt sourceLineFrom(double sceneY) {
        return SourceLines.wholeFrom(this, sceneY, sourceLine);
    }
}
