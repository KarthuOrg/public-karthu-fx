package org.karthu.fx.form;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A combo box as the content of a shell, with the chevron that goes with it and what the combo inputs share about
 * opening and closing its list.
 *
 * <p><b>Opening.</b> As every {@link GuardedComboBox}, it hands each request to open its list to the input, which
 * decides (not while disabled, read-only or bound) and then opens it with {@link #showList()}. The
 * {@linkplain #getChevron() chevron} and any other click the input takes as opening ({@link #openOnClick()}) go the
 * same way.
 *
 * <p><b>The click that closed the list.</b> A press outside the open list closes it before the press reaches the
 * input (the popup's auto-hide, which lets the press through), so a click on the chevron or beside the combo box would
 * open it again at once. The combo box notes when its list closes ({@link #listClosed()}); a press anywhere in the
 * input while the note lives marks its click, which then only closes the list, as a click on the combo box does.
 *
 * <p><b>Closing.</b> A list that shows closes when the input becomes disabled, also through a parent (see
 * {@link GuardedComboBox}); the input closes it itself when it becomes bound or read-only, in its
 * {@code updateContentEditable}.
 *
 * <p><b>Look.</b> Inside a shell the theme takes the combo box's own frame, background, padding, arrow and focus look
 * away ({@code base.css}, <i>Combo input</i>: any combo box that is a shell's content, style class
 * {@code form-shell-content}); the shell draws the frame and the chevron stands for the arrow. The cells of a combo box
 * in a shell are {@link GuardedComboBox.ShellCell}s, which never show the focus outside the shown list. While the user
 * may not choose, a {@link ShellComboText} stands in for the combo box.
 *
 * <p>Not exported: the combo inputs' implementation, shared by {@link ComboInput} and {@link EditableComboInput}.
 *
 * @param <T> the type of the combo box's entries
 */
final class ShellComboBox<T> extends GuardedComboBox<T> {

    private final Button chevron = createChevron();
    /** True from the moment the list closes until the event that closed it is handled. */
    private boolean listJustClosed;
    /** True while the mouse press of the current click closed the list: the click then opens no list. */
    private boolean pressClosedList;

    /**
     * Creates the combo box of an input over the entries, and wires it into the input: the chevron opens through
     * {@link #openOnClick()}, a press in the input notes whether it closed the list, and the input's disabling closes
     * the list (see {@link GuardedComboBox}).
     *
     * @param input   the combo input the combo box is the content of
     * @param entries the list's entries
     * @param opener  what a request to open the list does: the input decides, then calls {@link #showList()}
     * @param closer  what closes the list when the input becomes disabled: the input's own closing
     */
    ShellComboBox(FieldShell input, ObservableList<T> entries, Runnable opener, Runnable closer) {
        super(input, entries, opener, closer);
        chevron.setOnAction(event -> openOnClick());
        showingProperty().addListener((observable, wasShowing, showing) -> {
            if (!showing) {
                listClosed();
            }
        });
        input.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> pressClosedList = listJustClosed);
    }

    /**
     * Returns the chevron of the combo input: a button with Bootstrap's chevron down, style class
     * {@code form-combo-chevron}, styled as a trailing icon (see {@link FieldShell#styleAsIcon}), out of the tab
     * order. Its click opens the list through {@link #openOnClick()}; the input adds it to its own controls.
     *
     * @return the chevron
     */
    Button getChevron() {
        return chevron;
    }

    /**
     * Opens the list on a click, the chevron's or one beside the combo box, unless the click's press just closed it:
     * through {@link #show()}, the input's guarded opening.
     */
    void openOnClick() {
        if (pressClosedList) {
            pressClosedList = false;
            return;
        }
        show();
    }

    /**
     * The list closed: notes it until the event that closed it is handled (see the class description). Called when
     * the list stops showing; package-private, the tests' seam, as the list shows and closes only in a window.
     */
    void listClosed() {
        listJustClosed = true;
        Platform.runLater(() -> listJustClosed = false);
    }

    /** Creates the chevron: Bootstrap's chevron down, styled as a trailing icon. */
    private static Button createChevron() {
        Button button = new Button();
        button.getStyleClass().add("form-combo-chevron");
        button.setGraphic(FontIcon.of(BootstrapIcons.CHEVRON_DOWN));
        FieldShell.styleAsIcon(button);
        return button;
    }
}
