package org.karthu.fx.markdown;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.OptionalInt;

/**
 * The Markdown view's zoom bar ({@code markdown-zoom-bar}), on top of the view: from the left the zoom group, the
 * slider and <em>Reset</em>, in one line. Every part shows the view's zoom and sets it.
 *
 * <p><b>Zoom group</b> ({@code markdown-zoom-group}): one frame, drawn as a text field's, around minus
 * ({@code markdown-zoom-out}), the percent field ({@code markdown-zoom-field}) with its chevron
 * ({@code markdown-zoom-levels}) and plus ({@code markdown-zoom-in}); the theme draws the dividers as borders of minus
 * and plus. The frame shows the focus border, {@code :focus-within}, while the field has the focus or the list of zoom
 * levels is open: JavaFX sets the pseudo-class for the focus, the bar adds the open list. Minus and plus go to the next
 * preset in their direction, also from a value in between, and are disabled at 50 % and 200 %.
 *
 * <p><b>Percent field.</b> It shows the zoom as {@code 125%} and is exactly as wide as {@code 200%}. It takes a typed
 * value on Enter or when the focus moves from it to another node ({@link ZoomPercent#parse}); anything that is no
 * number brings back the zoom. Escape brings back the zoom without taking the text. Typing changes no zoom, and a zoom
 * set from outside the bar during an edit leaves the typed text alone until it is taken or abandoned; a part of the
 * bar the user operates shows its zoom in the field at once. Up and Down go to the next preset at once, Alt plus Down
 * opens the list.
 *
 * <p><b>Slider and Reset.</b> The slider ({@link ZoomSlider}) shows the zoom in percent and sets it in whole percent.
 * <em>Reset</em> ({@code markdown-zoom-reset}), a flat text button, sets 100 % and is disabled there.
 *
 * <p><b>Focus.</b> Only the field is in the tab order. Minus, plus, the chevron, the slider and <em>Reset</em> are not,
 * and a click on them leaves the focus where it is.
 *
 * <p><b>Narrow widths.</b> The slider takes its preferred width (the theme's 120px) where the bar has room for it,
 * else the room there is down to its minimum width (60px); below that it is hidden, then <em>Reset</em>. The zoom group
 * always stays at its own width, cut off by the bar's edge only when the bar is narrower than the group.
 *
 * <p><b>Bound zoom.</b> While the application has bound the view's zoom, the bar shows the bound value and sets
 * nothing.
 */
final class ZoomBar extends HBox {

    private static final PseudoClass FOCUS_WITHIN = PseudoClass.getPseudoClass("focus-within");

    private static final KeyCombination STEP_UP = new KeyCodeCombination(KeyCode.UP);
    private static final KeyCombination STEP_DOWN = new KeyCodeCombination(KeyCode.DOWN);
    private static final KeyCombination OPEN_LEVELS = new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN);
    private static final KeyCombination ABANDON = new KeyCodeCombination(KeyCode.ESCAPE);

    private final DocumentZoom documentZoom;
    private final HBox group = new HBox();
    private final Button zoomOut = new Button();
    private final PercentField field = new PercentField();
    private final Button levels = new Button();
    private final Button zoomIn = new Button();
    private final ZoomSlider slider = new ZoomSlider();
    private final Button reset = new Button();
    private final ZoomLevelsPopup levelsPopup = new ZoomLevelsPopup(percent -> setZoom(percent));

    /**
     * The text the bar last put into the field: the field is being edited while its text differs.
     */
    private String shownText = "";

    /**
     * True while the bar sets the slider's value itself, so the slider's listener does not set the zoom back.
     */
    private boolean updatingSlider;

    /**
     * Creates the bar of a view.
     *
     * @param documentZoom   the view's zoom, which the bar shows and sets for the reader
     * @param zoomOutText    the tooltip and accessible text of minus
     * @param zoomInText     the tooltip and accessible text of plus
     * @param zoomLevelsText the tooltip and accessible text of the chevron, the accessible text of the field
     * @param resetZoomText  the label of <em>Reset</em>
     */
    ZoomBar(DocumentZoom documentZoom, StringProperty zoomOutText, StringProperty zoomInText,
            StringProperty zoomLevelsText, StringProperty resetZoomText) {
        this.documentZoom = documentZoom;
        getStyleClass().add("markdown-zoom-bar");
        group.getStyleClass().add("markdown-zoom-group");
        group.setAlignment(Pos.CENTER_LEFT);
        group.setFillHeight(true);
        iconButton(zoomOut, "markdown-zoom-out", BootstrapIcons.DASH, zoomOutText);
        iconButton(levels, "markdown-zoom-levels", BootstrapIcons.CHEVRON_DOWN, zoomLevelsText);
        iconButton(zoomIn, "markdown-zoom-in", BootstrapIcons.PLUS, zoomInText);
        field.accessibleTextProperty().bind(zoomLevelsText);
        group.getChildren().addAll(zoomOut, field, levels, zoomIn);
        reset.getStyleClass().add("markdown-zoom-reset");
        reset.textProperty().bind(resetZoomText);
        reset.setFocusTraversable(false);
        reset.setMinWidth(Region.USE_PREF_SIZE);
        getChildren().addAll(group, slider, reset);

        zoomOut.setOnAction(event -> step(-1));
        zoomIn.setOnAction(event -> step(1));
        reset.setOnAction(event -> setZoom(ZoomPercent.NORMAL));
        levels.setOnAction(event -> toggleLevels());
        slider.valueProperty().addListener((observable, oldValue, value) -> {
            if (!updatingSlider) {
                setZoom((int) Math.round(value.doubleValue()));
            }
        });
        field.setOnAction(event -> {
            event.consume();
            take();
        });
        field.addEventFilter(KeyEvent.KEY_PRESSED, event -> fieldKeyPressed(event));
        whenFocusLeaves(field, () -> take());
        levelsPopup.openProperty().addListener(observable -> updateFocusWithin());
        group.focusWithinProperty().addListener(observable -> updateFocusWithin());
        documentZoom.zoom().addListener(observable -> showZoom(false));

        // The group is cut off at the bar's edge, never drawn beyond the view.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        showZoom(true);
    }

    /**
     * The list of zoom levels: the view's tests operate it, since it shows as a popup only in a shown window.
     *
     * @return the list of presets
     */
    ListView<Integer> levelsList() {
        return levelsPopup.list();
    }

    /**
     * Closes the list of zoom levels if it is open: when the view hides the bar.
     */
    void closeLevels() {
        levelsPopup.close();
    }

    /**
     * Sets the zoom from a part of the bar and shows it in every part, the field included, whatever it holds: the
     * user operated the bar. A bound zoom is not set ({@link DocumentZoom#zoomTo}); the bar shows its value.
     */
    private void setZoom(int percent) {
        documentZoom.zoomTo(percent);
        showZoom(true);
    }

    /**
     * Steps the zoom by presets from a part of the bar ({@link DocumentZoom#step}) and shows it in every part, as
     * {@link #setZoom} does.
     */
    private void step(int presets) {
        documentZoom.step(presets);
        showZoom(true);
    }

    /**
     * Shows the zoom in every part: the field (an edit only when asked to replace it), the slider, the list, and
     * whether minus, plus and <em>Reset</em> can act.
     */
    private void showZoom(boolean replaceEdit) {
        int percent = documentZoom.percent();
        if (replaceEdit || field.getText().equals(shownText)) {
            shownText = ZoomPercent.format(percent);
            field.setText(shownText);
        }
        updatingSlider = true;
        try {
            slider.setValue(percent);
        } finally {
            updatingSlider = false;
        }
        zoomOut.setDisable(percent <= ZoomPercent.MIN);
        zoomIn.setDisable(percent >= ZoomPercent.MAX);
        reset.setDisable(percent == ZoomPercent.NORMAL);
        levelsPopup.showZoom(percent);
    }

    /**
     * Takes the field's text: a number sets the zoom, anything else brings the zoom back.
     */
    private void take() {
        OptionalInt typed = ZoomPercent.parse(field.getText());
        if (typed.isPresent()) {
            setZoom(typed.getAsInt());
        } else {
            showZoom(true);
        }
    }

    /**
     * Up and Down go to the next preset at once, Alt plus Down opens the list of zoom levels, Escape abandons an edit.
     * Up and Down are consumed even at the ends of the range, so they never move the caret; Escape only while there is
     * an edit to abandon, so a dialog's Escape still works otherwise.
     */
    private void fieldKeyPressed(KeyEvent event) {
        if (STEP_UP.match(event)) {
            event.consume();
            step(1);
        } else if (STEP_DOWN.match(event)) {
            event.consume();
            step(-1);
        } else if (OPEN_LEVELS.match(event)) {
            event.consume();
            openLevels();
        } else if (ABANDON.match(event) && !field.getText().equals(shownText)) {
            event.consume();
            showZoom(true);
        }
    }

    private void toggleLevels() {
        if (levelsPopup.isOpen()) {
            levelsPopup.close();
        } else {
            openLevels();
        }
    }

    private void openLevels() {
        levelsPopup.open(documentZoom.percent(), group);
    }

    /**
     * The group shows {@code :focus-within} while anything in it has the focus, as JavaFX sets it, or the list of zoom
     * levels is open. JavaFX sets the pseudo-class before the listeners of the focus run, so this one has the last
     * word.
     */
    private void updateFocusWithin() {
        group.pseudoClassStateChanged(FOCUS_WITHIN, group.isFocusWithin() || levelsPopup.isOpen());
    }

    /**
     * Runs an action whenever the focus owner of the bar's scene moves from a node to another one: the user left the
     * field. The window losing the focus moves no focus owner, so an edit stays while the user is in another window.
     */
    private void whenFocusLeaves(Node node, Runnable action) {
        ChangeListener<Node> listener = (observable, oldOwner, newOwner) -> {
            if (oldOwner == node && newOwner != node) {
                action.run();
            }
        };
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.focusOwnerProperty().removeListener(listener);
            }
            if (newScene != null) {
                newScene.focusOwnerProperty().addListener(listener);
            }
        });
    }

    /**
     * An icon button of the zoom group: the theme's icon button, its Bootstrap icon only, out of the tab order, its
     * text the tooltip and the accessible text.
     */
    private static void iconButton(Button button, String styleClass, BootstrapIcons icon, StringProperty text) {
        button.getStyleClass().addAll("icon-button", styleClass);
        button.setGraphic(FontIcon.of(icon));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setFocusTraversable(false);
        // A button's minimum height is its font's line, even with its graphic only: the group would be higher than a
        // text field. The buttons are as high as the group, which the field makes as high as a text field.
        button.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        button.setMaxHeight(Double.MAX_VALUE);
        // Bound, not copied: a new text applies at once.
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(text);
        button.setTooltip(tooltip);
        button.accessibleTextProperty().bind(text);
    }

    // The bar lays its parts out itself, see the class comment: the slider and Reset give way, the group never.

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double width = getWidth() - left - snappedRightInset();
        double height = getHeight() - top - snappedBottomInset();
        double spacing = snapSpaceX(getSpacing());
        double groupWidth = snapSizeX(group.prefWidth(-1));
        double resetWidth = snapSizeX(reset.prefWidth(-1));
        double sliderRoom = width - groupWidth - spacing - spacing - resetWidth;
        boolean sliderShown = sliderRoom >= snapSizeX(slider.minWidth(-1));
        boolean resetShown = width >= groupWidth + spacing + resetWidth;
        slider.setVisible(sliderShown);
        reset.setVisible(resetShown);

        double x = left;
        layoutInArea(group, x, top, groupWidth, height, 0, HPos.LEFT, VPos.CENTER);
        x += groupWidth + spacing;
        if (sliderShown) {
            double sliderWidth = Math.min(snapSizeX(slider.prefWidth(-1)), sliderRoom);
            layoutInArea(slider, x, top, sliderWidth, height, 0, HPos.LEFT, VPos.CENTER);
            x += sliderWidth + spacing;
        }
        if (resetShown) {
            layoutInArea(reset, x, top, resetWidth, height, 0, HPos.LEFT, VPos.CENTER);
        }
    }

    /**
     * Any width: the slider and <em>Reset</em> give way, the group is cut off.
     */
    @Override
    protected double computeMinWidth(double height) {
        return snappedLeftInset() + snappedRightInset();
    }

    /**
     * The percent field: exactly as wide as its widest text, {@code 200%}, in its font, so the group never changes its
     * width with the zoom.
     */
    private static final class PercentField extends TextField {

        /**
         * The caret's room after the text, so the widest text never scrolls.
         */
        private static final double CARET_ROOM = 2;

        private PercentField() {
            getStyleClass().add("markdown-zoom-field");
        }

        @Override
        protected double computePrefWidth(double height) {
            Text probe = new Text(ZoomPercent.WIDEST);
            probe.setFont(getFont());
            return snappedLeftInset() + snapSizeX(probe.getLayoutBounds().getWidth() + CARET_ROOM)
                    + snappedRightInset();
        }

        @Override
        protected double computeMinWidth(double height) {
            return computePrefWidth(height);
        }

        @Override
        protected double computeMaxWidth(double height) {
            return computePrefWidth(height);
        }
    }
}
