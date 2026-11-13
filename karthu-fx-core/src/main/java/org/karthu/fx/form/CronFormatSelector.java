package org.karthu.fx.form;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * The cron input's format selector: a small non-editable combo box of the two formats, <i>Unix</i> and
 * <i>Quartz</i>, with the name of the chosen one and a chevron (Bootstrap {@code CHEVRON_DOWN}) on a background of its
 * own, style class {@code form-cron-format}. The theme takes JavaFX's own frame and arrow away and draws it as a small
 * rounded box ({@code base.css}, <i>Cron input</i>).
 *
 * <p><b>Width.</b> The selector is exactly as wide as its widest entry, whichever is chosen, so that the field does not
 * jump when the format changes: its cell holds the names of all formats on top of each other, only the chosen one
 * visible, and the selector is as wide as that cell, not as wide as JavaFX's combo box would make it (its list's width
 * and more). It is as high as its cell and never higher, so the shell centres it in the frame.
 *
 * <p><b>Opening and focus.</b> As every {@link GuardedComboBox}, the selector hands each request to open its list (a
 * click on it, a screen reader's expand) to its input, which decides (not while disabled, read-only or the format
 * fixed) and then opens it with {@link #showList()}, and its list closes when the input becomes disabled. The selector
 * never takes the focus itself: it is not focus traversable, and a request for the focus goes to the input's text
 * field, so that the keyboard stays there. In the open list the keys are JavaFX's own, which its popup takes before the
 * text field: Up and Down choose, Enter and Escape close it.
 *
 * <p>Not exported: the cron input's implementation.
 */
final class CronFormatSelector extends GuardedComboBox<CronFormat> {

    /** The gap between the format's name and the chevron. */
    private static final double CHEVRON_GAP = 4;

    /** What a request for the focus does instead: the input's text field takes it. */
    private final Runnable focuser;
    private final SelectorCell cell = new SelectorCell();

    /**
     * Creates the selector of a cron input, its value null until the input sets its format.
     *
     * @param input   the cron input the selector is part of
     * @param opener  what a request to open the list does: the input decides, then calls {@link #showList()}
     * @param closer  what closes the list when the input becomes disabled: the input's own closing
     * @param focuser what a request for the focus does: the input's text field takes it
     */
    CronFormatSelector(CronInput input, Runnable opener, Runnable closer, Runnable focuser) {
        super(input, FXCollections.observableArrayList(CronFormat.values()), opener, closer);
        this.focuser = focuser;
        getStyleClass().add("form-cron-format");
        setFocusTraversable(false);
        setButtonCell(cell);
        setCellFactory(list -> new EntryCell());
        valueProperty().addListener((observable, oldFormat, newFormat) -> cell.showValue());
    }

    /**
     * Gives the focus to the input's text field: the selector never takes it.
     */
    @Override
    public void requestFocus() {
        focuser.run();
    }

    // As wide as its cell, which is as wide as the widest entry, never as JavaFX's combo box would size it (at least its
    // list's width); as high as its cell and never higher, so the shell centres it in the frame.

    @Override
    protected double computePrefWidth(double height) {
        return snappedLeftInset() + snapSizeX(cell.prefWidth(-1)) + snappedRightInset();
    }

    @Override
    protected double computeMinWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return computePrefWidth(height);
    }

    @Override
    protected double computeMaxHeight(double width) {
        return prefHeight(width);
    }

    /**
     * The cell that shows the chosen format in the field: the names of all formats on top of each other, only the
     * chosen one visible, so that the cell is as wide as the widest name, and the chevron after them. It shows the
     * selector's value whatever the skin hands it.
     */
    private final class SelectorCell extends GuardedComboBox.ShellCell<CronFormat> {

        private final List<Label> names = new ArrayList<>();

        private SelectorCell() {
            super(CronFormatSelector.this);
            StackPane stack = new StackPane();
            stack.setAlignment(Pos.CENTER_LEFT);
            for (CronFormat format : CronFormat.values()) {
                Label name = new Label(format.displayName());
                names.add(name);
                stack.getChildren().add(name);
            }
            HBox content = new HBox(CHEVRON_GAP, stack, FontIcon.of(BootstrapIcons.CHEVRON_DOWN));
            content.setAlignment(Pos.CENTER_LEFT);
            setGraphic(content);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            showValue();
        }

        @Override
        protected void updateItem(CronFormat item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            showValue();
        }

        /** Shows the selector's value: its name visible, the others hidden but keeping their room. */
        private void showValue() {
            CronFormat current = getValue();
            CronFormat[] formats = CronFormat.values();
            for (int i = 0; i < formats.length; i++) {
                names.get(i).setVisible(formats[i] == current);
            }
        }
    }

    /** An entry of the list: the format's name. */
    private final class EntryCell extends GuardedComboBox.ShellCell<CronFormat> {

        private EntryCell() {
            super(CronFormatSelector.this);
        }

        @Override
        protected void updateItem(CronFormat item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.displayName());
        }
    }
}
