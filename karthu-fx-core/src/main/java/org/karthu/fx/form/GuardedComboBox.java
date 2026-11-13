package org.karthu.fx.form;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

/**
 * A combo box inside a shell whose list opens only through its input's guard, and closes when the input becomes
 * disabled: what every combo box of the collection shares, the combo inputs' content ({@link ShellComboBox}) and the
 * cron input's format selector ({@link CronFormatSelector}).
 *
 * <p><b>Opening.</b> Every way JavaFX opens a combo box's list (a click on it, Alt+Down, Alt+Up, F4, a screen reader's
 * expand) calls {@link #show()}, which this combo box hands to its input instead: the input decides whether the list
 * may open now (for example not while disabled or read-only) and then opens it with {@link #showList()}, which only
 * shows it in a shown window, since a popup needs one.
 *
 * <p><b>Closing.</b> A list that shows closes when the input becomes disabled, also through a parent, through the
 * input's own closing, so that a test's seam sees it. The input closes it itself in its other cases (bound, read-only,
 * the cron input's fixed format).
 *
 * <p>Not exported: the inputs' implementation.
 *
 * @param <T> the type of the combo box's entries
 */
class GuardedComboBox<T> extends ComboBox<T> {

    /** What {@link #show()} does instead of opening the list: the input's own opening, with its guard. */
    private final Runnable opener;

    /**
     * Creates the combo box of an input over the entries: requests to open its list go to the opener, and the input's
     * disabling runs the closer.
     *
     * @param input   the input the combo box is part of
     * @param entries the list's entries
     * @param opener  what a request to open the list does: the input decides, then calls {@link #showList()}
     * @param closer  what closes the list when the input becomes disabled: the input's own closing
     */
    GuardedComboBox(FieldShell input, ObservableList<T> entries, Runnable opener, Runnable closer) {
        super(entries);
        this.opener = opener;
        input.disabledProperty().addListener((observable, wasDisabled, isDisabled) -> {
            if (isDisabled) {
                closer.run();
            }
        });
    }

    /**
     * Hands every request to open the list to the input, whose guard decides.
     */
    @Override
    public void show() {
        opener.run();
    }

    /**
     * Opens the list: JavaFX's own opening, without the input's guard. Only in a shown window: elsewhere it does
     * nothing, as a popup needs a window.
     */
    void showList() {
        if (getScene() != null && getScene().getWindow() != null && getScene().getWindow().isShowing()) {
            super.show();
        }
    }

    /**
     * Closes the list if it shows.
     */
    void hideList() {
        if (isShowing()) {
            hide();
        }
    }

    /**
     * A cell of a combo box in a shell: the one that shows the value in the field, or an entry of the list. A list cell
     * takes its list's focused entry as its own focus, and the combo box's list view lies in the field until its popup
     * is first shown (the field measures its entries there), as the field's own cell is handed the list's view: the
     * shell would show {@code :focus-within}, the focus border, without the focus. So a cell outside the shown list (in
     * the field's scene, or in none) never has the focus; a cell of the shown list, in the popup's scene, keeps
     * JavaFX's. Any combo box in a shell uses it: the combo inputs' content and the cron input's format selector.
     *
     * @param <T> the type of the combo box's entries
     */
    static class ShellCell<T> extends ListCell<T> {

        /**
         * Creates a cell of the combo box.
         *
         * @param comboBox the combo box whose field or list the cell is in
         */
        ShellCell(ComboBox<T> comboBox) {
            focusedProperty().addListener((observable, wasFocused, focused) -> {
                if (focused && (getScene() == null || getScene() == comboBox.getScene())) {
                    setFocused(false);
                }
            });
        }
    }
}
