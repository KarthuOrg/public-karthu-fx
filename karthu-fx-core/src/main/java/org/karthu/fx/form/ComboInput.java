package org.karthu.fx.form;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A choice from a list: a {@link FieldShell} around a non-editable combo box, whose own arrow and frame are hidden, so
 * that the choice looks like every other input of the form collection. A chevron at the right end shows that the field
 * opens a list.
 *
 * <p><b>Value and list.</b> {@link #valueProperty()} is the chosen value, of any type, null for none. The list offers
 * the {@linkplain #getItems() items} in their order, each shown with its
 * {@linkplain #displayTextProperty() display text}, by default the value's {@code toString()}, so an application offers
 * objects or codes with readable labels. The field shows the value's display text, also for a value that is not in the
 * list (an item the application removed, a value it set), which stays the value until the user chooses another; without
 * a value it shows the {@linkplain #promptTextProperty() prompt text} (default <i>Select a value</i>) in the prompt
 * colour. Changing the items never changes the value.
 *
 * <p><b>The empty entry.</b> While {@linkplain #emptyAllowedProperty() empty is allowed} (default false), the list
 * starts with an entry that is no item, its text from {@link #emptyTextProperty()} (default <i>(none)</i>): choosing it
 * sets the value to null, so an optional choice can go back to nothing chosen.
 *
 * <p><b>Opening the list.</b> A click anywhere on the field opens the list (the pointer is a hand over it), on the
 * combo box, on the chevron and beside them; a click whose press closed the open list only closes it. Alt+Down, F4 and
 * Space in the field open it too, Alt+Up as well, as in JavaFX's combo box; in the closed field Up and Down choose the
 * entry before and after, as JavaFX's combo box does. In the open list, keys and clicks are JavaFX's own. The list
 * opens only while the user may change the value: not while the field is disabled, read-only or bound, and a list that
 * shows closes when the field becomes one of them. It shows only in a shown window.
 *
 * <p><b>Binding.</b> The field can take its value from a variable through its {@linkplain #getBinding() binding},
 * shown by its shell (see {@link FieldShell}): the {@code {}} icon after the chevron, the chip in place of the shown
 * value while bound. While bound the chevron is disabled and no list opens, and Up and Down change nothing; the value
 * is kept, hidden, and shows again after unbinding. The application may still set it.
 *
 * <p><b>Read-only.</b> A {@linkplain #readOnlyProperty() read-only} field (see {@link FieldShell}) shows the value's
 * display text as a text to select and copy in place of the combo box, without the chevron and the {@code {}} icon; no
 * list opens. The text takes the focus where the combo box had it, and gives it back when read-only ends; a form row's
 * label names either.
 *
 * <p><b>Texts</b>, properties with English defaults: the prompt text <i>Select a value</i> and the empty entry's text
 * <i>(none)</i>.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The field carries {@code form-combo-input} beside the shell's {@code form-frame} (see
 * {@link FieldShell} for the shell's classes and pseudo-classes); the combo box carries the shell's
 * {@code form-shell-content} beside JavaFX's {@code combo-box}.
 * <ul>
 *   <li>{@code form-combo-chevron}: the chevron, an own control of the shell, beside {@code form-frame-icon} and
 *       {@code icon-button}</li>
 *   <li>{@code form-combo-display}: the cell in the field that shows the value, beside JavaFX's {@code list-cell};
 *       with the pseudo-class {@code :prompt} while it shows the prompt text, without a value</li>
 *   <li>{@code form-combo-text}: the read-only text in place of the combo box, a text field (keeps
 *       {@code text-field}) that also carries {@code form-shell-content}</li>
 * </ul>
 *
 * <p>Like every karthu-fx control, the field is used on the JavaFX Application Thread only.
 *
 * @param <T> the type of the values to choose from
 */
public class ComboInput<T> extends FieldShell implements FormInput<T> {

    private static final PseudoClass PROMPT_PSEUDO_CLASS = PseudoClass.getPseudoClass("prompt");

    /** The key that opens the list besides JavaFX's own Alt+Down and F4: Space, without modifiers. */
    private static final KeyCombination OPEN_SHORTCUT = new KeyCodeCombination(KeyCode.SPACE);

    private static final String SELECT_A_VALUE_TEXT = "Select a value";
    private static final String NONE_TEXT = "(none)";

    private final ObjectProperty<T> value = new SimpleObjectProperty<>(this, "value");
    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObjectProperty<Function<T, String>> displayText =
            new SimpleObjectProperty<>(this, "displayText", defaultDisplayText());
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", SELECT_A_VALUE_TEXT);
    private final BooleanProperty emptyAllowed = new SimpleBooleanProperty(this, "emptyAllowed", false);
    private final StringProperty emptyText = new SimpleStringProperty(this, "emptyText", NONE_TEXT);

    /** The combo box's entries: the empty entry (null) first while it is allowed, then the items. */
    private final ObservableList<T> entries = FXCollections.observableArrayList();
    private final ShellComboBox<T> combo = new ShellComboBox<>(this, entries, this::openList, this::hideList);
    /** The cell that shows the value in the field, the combo box's button cell. */
    private final DisplayCell display = new DisplayCell();
    private final Button chevron = combo.getChevron();
    /** The content while read-only, in place of the combo box: the display text to select and copy. */
    private final ShellComboText readOnlyText = new ShellComboText(this, combo);
    /**
     * True while the input puts its value or its entries into the combo box: the combo box's own changes of its value
     * meanwhile are no choice of the user's.
     */
    private boolean syncing;

    /**
     * Creates an input without entries and without a value.
     */
    public ComboInput() {
        getStyleClass().add("form-combo-input");

        combo.setConverter(new DisplayConverter());
        combo.setButtonCell(display);
        combo.setCellFactory(list -> new EntryCell());
        combo.promptTextProperty().bind(promptText);
        combo.valueProperty().addListener((observable, oldValue, chosen) -> comboValueChanged(chosen));

        items.addListener((ListChangeListener<T>) change -> updateEntries());
        value.addListener((observable, oldValue, newValue) -> valueChanged());
        displayText.addListener((observable, oldFunction, newFunction) -> updateEntries());
        promptText.addListener((observable, oldText, newText) -> showValue());
        emptyAllowed.addListener((observable, wasAllowed, allowed) -> updateEntries());
        emptyText.addListener((observable, oldText, newText) -> updateEntries());

        getOwnControls().add(chevron);
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleOpenKey);
        // A click on the field beside the combo box (its right padding, between the icons): the controls inside take
        // their own clicks, the combo box opens the list on its own.
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getTarget() == this) {
                combo.openOnClick();
            }
        });

        setContent(combo);
    }

    // --- FormInput -----------------------------------------------------------------------------------

    /**
     * Returns the field itself, to place it in a layout.
     *
     * @return this field
     */
    @Override
    public Node getNode() {
        return this;
    }

    /**
     * The chosen value (default null: none). The user's choice sets it; the application may set any value, also one
     * that is not in the list, which the field shows with its display text. Neither the items, the binding nor
     * read-only change it.
     *
     * @return the value property
     */
    @Override
    public ObjectProperty<T> valueProperty() {
        return value;
    }

    /**
     * Returns the chosen value.
     *
     * @return the value, or null for none
     */
    public T getValue() {
        return value.get();
    }

    /**
     * Sets the chosen value.
     *
     * @param newValue the value, or null for none; it need not be in the list
     */
    public void setValue(T newValue) {
        value.set(newValue);
    }

    // --- Properties ----------------------------------------------------------------------------------

    /**
     * The values the list offers, in their order (default empty), after the empty entry while it is allowed. Changing
     * them never changes the value. No item may be null: the empty entry stands for no value.
     *
     * @return the items, a modifiable list
     */
    public ObservableList<T> getItems() {
        return items;
    }

    /**
     * The text a value is shown with, in the list and in the field (default: the value's {@code toString()}). Null
     * counts as the default; a function that returns null shows the empty text. The field never asks it for null: no
     * value shows the prompt text, the empty entry its own text.
     *
     * @return the display text property
     */
    public ObjectProperty<Function<T, String>> displayTextProperty() {
        return displayText;
    }

    /**
     * Returns the display text function.
     *
     * @return the function; null only when it was set to null, which counts as the default
     */
    public Function<T, String> getDisplayText() {
        return displayText.get();
    }

    /**
     * Sets the display text function.
     *
     * @param function the function from a value to its text; null for the value's {@code toString()}
     */
    public void setDisplayText(Function<T, String> function) {
        displayText.set(function);
    }

    /**
     * The text the field shows without a value, in the prompt colour (default <i>Select a value</i>); null shows
     * nothing.
     *
     * @return the prompt text property
     */
    public StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Returns the prompt text.
     *
     * @return the prompt text, or null
     */
    public String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the prompt text.
     *
     * @param text the prompt text, or null for none
     */
    public void setPromptText(String text) {
        promptText.set(text);
    }

    /**
     * Whether the list starts with the empty entry (default false), which sets the value to null: an optional choice.
     *
     * @return the empty allowed property
     */
    public BooleanProperty emptyAllowedProperty() {
        return emptyAllowed;
    }

    /**
     * Returns whether the list starts with the empty entry.
     *
     * @return true while the empty entry is offered
     */
    public boolean isEmptyAllowed() {
        return emptyAllowed.get();
    }

    /**
     * Sets whether the list starts with the empty entry.
     *
     * @param allowed true to offer the empty entry
     */
    public void setEmptyAllowed(boolean allowed) {
        emptyAllowed.set(allowed);
    }

    /**
     * The empty entry's text in the list (default <i>(none)</i>); null shows the empty text.
     *
     * @return the empty text property
     */
    public StringProperty emptyTextProperty() {
        return emptyText;
    }

    /**
     * Returns the empty entry's text.
     *
     * @return the empty entry's text, or null
     */
    public String getEmptyText() {
        return emptyText.get();
    }

    /**
     * Sets the empty entry's text.
     *
     * @param text the empty entry's text
     */
    public void setEmptyText(String text) {
        emptyText.set(text);
    }

    // --- Opening the list ----------------------------------------------------------------------------

    /**
     * Opens the list while the user may change the value: every way to open it passes here, JavaFX's own (a click on
     * the combo box, Alt+Down, Alt+Up, F4) through the combo box's {@code show()}, and Space, the chevron and a click
     * on the field around the combo box. The combo box takes the focus first, as JavaFX's own opening gives it.
     */
    private void openList() {
        if (!userMayChangeValue()) {
            return;
        }
        if (!combo.isFocused()) {
            combo.requestFocus();
        }
        showList();
    }

    /**
     * The list closed, so that the click whose press closed it does not open it again (see {@link ShellComboBox}).
     * Package-private: the tests' seam, as the list shows and closes only in a window.
     */
    void listClosed() {
        combo.listClosed();
    }

    /**
     * Shows the list, in a shown window only, as a popup needs one. Package-private: the tests' seam, so that a test
     * sees the list open without a window; it is no part of the API.
     */
    void showList() {
        combo.showList();
    }

    /** Closes the list if it shows. Package-private: the tests' seam, with {@link #showList()}. */
    void hideList() {
        combo.hideList();
    }

    /**
     * Space opens the closed list. JavaFX's combo box only arms itself on Space; Alt+Down and F4 it handles itself,
     * through {@link #openList()}. In the open list Space chooses the entry, which the list handles.
     */
    private void handleOpenKey(KeyEvent event) {
        if (OPEN_SHORTCUT.match(event) && !combo.isShowing() && userMayChangeValue()) {
            event.consume();
            openList();
        }
    }

    // --- Read-only and bound -------------------------------------------------------------------------

    /**
     * Follows read-only and the binding: while read-only the display text to select and copy in place of the combo box,
     * no list while read-only or bound, the chevron disabled while bound and hidden while read-only.
     *
     * @param editable true while the user may choose: neither bound nor read-only
     */
    @Override
    protected void updateContentEditable(boolean editable) {
        boolean readOnly = isReadOnly();
        Node shown = readOnly ? readOnlyText : combo;
        if (getContent() != shown) {
            // The shell calls this hook again for the new content, the one shown now, and moves the focus with it.
            setContent(shown);
            return;
        }
        updateOwnControl(chevron, !readOnly);
        if (!editable) {
            hideList();
        }
    }

    /**
     * Returns where the shown text starts, where the chip lies while bound: in the combo box, which the theme gives no
     * padding, its cell's padding in from the cell's left edge, as far in as a text field's text; in the read-only text
     * the shell's default, its padding.
     *
     * @return the distance from the content's left edge to its text
     */
    @Override
    protected double chipInset() {
        if (getContent() == combo) {
            return combo.snappedLeftInset() + display.snappedLeftInset();
        }
        return super.chipInset();
    }

    // --- Value and entries ---------------------------------------------------------------------------

    /** The value changed: the combo box takes it, the field shows it. */
    private void valueChanged() {
        if (!Objects.equals(combo.getValue(), value.get())) {
            pushValue();
        }
        showValue();
    }

    /** The items, or a text of the entries, changed: the combo box takes the new entries and keeps the value. */
    private void updateEntries() {
        List<T> next = new ArrayList<>();
        if (emptyAllowed.get()) {
            next.add(null);
        }
        next.addAll(items);
        syncing = true;
        try {
            entries.setAll(next);
        } finally {
            syncing = false;
        }
        pushValue();
        showValue();
    }

    /**
     * Puts the value into the combo box and its selection onto the value's entry, or onto no entry when the value is
     * not in the list. The combo box's own changes meanwhile are ignored.
     */
    private void pushValue() {
        T current = value.get();
        SingleSelectionModel<T> selection = combo.getSelectionModel();
        syncing = true;
        try {
            // Cleared first, so the selected item changes: after its entries were replaced, the combo box ignores the
            // next change of its selected item to null (JavaFX's guard for a list replaced with setAll), which would
            // otherwise be the user's choice of the empty entry.
            selection.clearSelection();
            int index = entries.indexOf(current);
            if (index >= 0) {
                selection.select(index);
            } else {
                selection.select(current);
            }
            combo.setValue(current);
        } finally {
            syncing = false;
        }
    }

    /**
     * The combo box's value changed: a choice of the user's becomes the value while the user may change it; any other
     * change of the combo box's own is undone.
     */
    private void comboValueChanged(T chosen) {
        if (syncing) {
            return;
        }
        if (!setValueByUser(value, chosen)) {
            pushValue();
        }
    }

    /** Shows the value in the field and in the read-only text. */
    private void showValue() {
        display.showValue();
        readOnlyText.setText(displayTextOf(value.get()));
    }

    /** The text a value is shown with: empty for null, else the display function's text. */
    private String displayTextOf(T shown) {
        if (shown == null) {
            return "";
        }
        Function<T, String> function = displayText.get();
        String text = function == null ? String.valueOf(shown) : function.apply(shown);
        return text == null ? "" : text;
    }

    private static <V> Function<V, String> defaultDisplayText() {
        return shown -> shown == null ? "" : shown.toString();
    }

    // --- Cells ---------------------------------------------------------------------------------------

    /** The display text as the combo box's converter, for the field and the screen reader. */
    private final class DisplayConverter extends StringConverter<T> {

        @Override
        public String toString(T shown) {
            return displayTextOf(shown);
        }

        @Override
        public T fromString(String text) {
            // Never used: the combo box is not editable.
            return null;
        }
    }

    /** An entry of the list: the empty entry's text for the empty entry, else the item's display text. */
    private final class EntryCell extends GuardedComboBox.ShellCell<T> {

        private EntryCell() {
            super(combo);
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
            } else if (item == null) {
                String text = emptyText.get();
                setText(text == null ? "" : text);
            } else {
                setText(displayTextOf(item));
            }
        }
    }

    /**
     * The cell that shows the value in the field: its display text, or the prompt text without a value. It shows the
     * input's value whatever the combo box's skin hands it, so it never shows a stale entry.
     */
    private final class DisplayCell extends GuardedComboBox.ShellCell<T> {

        private DisplayCell() {
            super(combo);
            getStyleClass().add("form-combo-display");
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            showValue();
        }

        /** Shows the input's value, or the prompt text as {@code :prompt}. */
        private void showValue() {
            T current = value.get();
            if (current == null) {
                String prompt = promptText.get();
                setText(prompt == null ? "" : prompt);
            } else {
                setText(displayTextOf(current));
            }
            pseudoClassStateChanged(PROMPT_PSEUDO_CLASS, current == null);
        }
    }
}
