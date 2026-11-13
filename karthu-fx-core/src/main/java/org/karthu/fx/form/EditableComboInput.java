package org.karthu.fx.form;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.karthu.fx.form.suggestion.SuggestionFilter;

import java.util.List;

/**
 * Free text with suggestions: a {@link FieldShell} around an editable combo box, whose own arrow and frame are hidden,
 * so that the field looks like every other input of the collection. The user types freely; the suggestions help and
 * never overwrite what was typed. A divider and a chevron at the right end show that the field also offers a list.
 *
 * <p><b>Value.</b> {@link #valueProperty()} is the text exactly as it stands in the field, as in a text input: what
 * the user types stays as typed, the empty text is no value (null counts as the empty text). It need not be one of the
 * {@linkplain #getSuggestions() suggestions}, which only help.
 *
 * <p><b>Suggestions.</b> Typing (also deleting and pasting) opens the list with the suggestions that contain the text,
 * ignoring case letter by letter, every suggestion while the text is empty; when none contains it, the list closes.
 * The chevron and Alt+Down open the full list (F4 and Alt+Up too, as in JavaFX's combo box). The open list takes the
 * keys of JavaFX's list first, as JavaFX's combo box hands them: Up, Down, Page Up, Page Down, Home and End move the
 * highlight without writing anything into the field, and typing again starts without a highlight; Enter or a click
 * chooses the highlighted or clicked suggestion, which becomes the text, the caret after it, and closes the list;
 * Enter without a highlighted suggestion only closes it. Escape closes the list and keeps the text. No suggestion is
 * ever written into the field by itself: only the user's choice writes one. A click whose press closed the open list
 * only closes it. The list opens only while the user may change the value: not while the field is disabled, read-only
 * or bound, and a list that shows closes when the field becomes one of them or loses the focus. It shows only in a
 * shown window.
 *
 * <p><b>Binding.</b> The field can take its value from a variable through its {@linkplain #getBinding() binding},
 * shown by its shell (see {@link FieldShell}): the {@code {}} icon after the chevron, the chip in place of the text
 * while bound. While bound the chevron is disabled and no list opens; the text is kept, hidden, and shows again after
 * unbinding. The application may still set it.
 *
 * <p><b>Read-only.</b> A {@linkplain #readOnlyProperty() read-only} field (see {@link FieldShell}) shows the text to
 * select and copy, without the divider, the chevron and the {@code {}} icon; no list opens. While read-only or bound,
 * a plain text field stands in for the combo box (whose text could otherwise be selected and copied while hidden); it
 * takes the focus where the combo box had it, and gives it back afterwards; a form row's label names either.
 *
 * <p><b>Texts</b>: the {@linkplain #promptTextProperty() prompt text}, empty by default, as a text input's: the
 * application names what it asks for.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The field carries {@code form-editable-combo-input} beside the shell's {@code form-frame} (see
 * {@link FieldShell} for the shell's classes and pseudo-classes); the combo box carries the shell's
 * {@code form-shell-content} beside JavaFX's {@code combo-box}, its editor JavaFX's {@code text-field}.
 * <ul>
 *   <li>{@code form-shell-divider}: the shell's divider before the chevron, an own control of the shell</li>
 *   <li>{@code form-combo-chevron}: the chevron, the combo input's, an own control of the shell, beside
 *       {@code form-frame-icon} and {@code icon-button}</li>
 *   <li>{@code form-combo-text}: the text in place of the combo box while read-only or bound, a text field (keeps
 *       {@code text-field}) that also carries {@code form-shell-content}, as in the combo input</li>
 * </ul>
 * An editable combo input adds no pseudo-class: the shell's {@code :warning}, {@code :error}, {@code :readonly},
 * {@code :bound} and {@code :disabled} apply.
 *
 * <p>Like every karthu-fx control, the field is used on the JavaFX Application Thread only.
 */
public class EditableComboInput extends FieldShell implements FormInput<String> {

    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final ObservableList<String> suggestions = FXCollections.observableArrayList();
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "");

    /** The list's entries: the suggestions the last text kept, or all of them. */
    private final ObservableList<String> entries = FXCollections.observableArrayList();
    private final ShellComboBox<String> combo =
            new ShellComboBox<>(this, entries, this::openFullList, this::hideList);
    /** The combo box's editor, where the user types: JavaFX's, which hands its focus to the combo box. */
    private final TextField editor = combo.getEditor();
    private final Region divider = createDivider();
    private final Button chevron = combo.getChevron();
    /** The content while read-only or bound, in place of the combo box: the text to select and copy. */
    private final ShellComboText readOnlyText = new ShellComboText(this, combo);
    /** The list's view, built by the combo box's skin: the open list, its highlight; null until the skin is built. */
    private ListView<String> list;
    /** The text the entries are filtered by, or null for every suggestion (the full list). */
    private String filterText;
    /** True while the input puts the value into the editor and the combo box, or the editor's text into the value. */
    private boolean syncing;
    /** Enter in the open list chooses the highlighted suggestion. */
    private final EventHandler<KeyEvent> listEnterFilter = this::handleListEnter;

    /**
     * Creates an empty input without suggestions and without a prompt text.
     */
    public EditableComboInput() {
        getStyleClass().add("form-editable-combo-input");

        combo.setEditable(true);
        // The combo box's own selection is never the user's choice: JavaFX moves it with the list's highlight and
        // writes it into the editor, which would fill a suggestion in by itself. This one follows the value only.
        combo.setSelectionModel(new ValueOnlySelection());
        combo.setConverter(new TextConverter());
        combo.setCellFactory(listView -> new SuggestionCell());
        combo.setButtonCell(new GuardedComboBox.ShellCell<>(combo));
        combo.promptTextProperty().bind(Bindings.createStringBinding(this::shownPromptText, promptText));
        // The combo box's value is always the editor's text: whatever JavaFX writes back from it is the same text.
        combo.setValue("");
        combo.skinProperty().addListener((observable, oldSkin, newSkin) -> skinChanged(newSkin));

        value.addListener((observable, oldValue, newValue) -> valueChanged());
        editor.textProperty().addListener((observable, oldText, newText) -> editorTextChanged());
        suggestions.addListener((ListChangeListener<String>) change -> updateEntries());

        getOwnControls().addAll(divider, chevron);
        // The focus gone from the combo box (Tab, a click elsewhere): a list that shows closes, as when the input
        // becomes disabled (the combo box closes it then), bound or read-only.
        whenFocusLeaves(combo, this::hideList);

        updateEntries();
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
     * The field's value: the text exactly as it stands in the field, the empty text when there is none; null counts as
     * the empty text. The user's typing and choice set it; setting it sets the text, also while the field is bound or
     * read-only, and never opens the list.
     *
     * @return the value property
     */
    @Override
    public StringProperty valueProperty() {
        return value;
    }

    /**
     * Returns the field's text.
     *
     * @return the text in the field
     */
    public String getValue() {
        return value.get();
    }

    /**
     * Sets the field's text, as it is.
     *
     * @param text the new text
     */
    public void setValue(String text) {
        value.set(text);
    }

    // --- Properties ----------------------------------------------------------------------------------

    /**
     * The texts the list offers, in their order (default empty): all of them in the full list, those that contain the
     * typed text, ignoring case, while the user types. A null suggestion is never offered.
     *
     * @return the suggestions, a modifiable list
     */
    public ObservableList<String> getSuggestions() {
        return suggestions;
    }

    /**
     * The text the empty field shows (default empty: none), such as <i>Encoding, such as UTF-8</i>. Null counts as
     * empty.
     *
     * @return the prompt text property
     */
    public StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Returns the prompt text.
     *
     * @return the prompt text, empty or null when there is none
     */
    public String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the prompt text.
     *
     * @param text the prompt text; null or empty for none
     */
    public void setPromptText(String text) {
        promptText.set(text);
    }

    /** The prompt text the field shows: the property's, null as empty. */
    private String shownPromptText() {
        String text = promptText.get();
        return text == null ? "" : text;
    }

    // --- The value and the editor --------------------------------------------------------------------

    /**
     * The value changed (the application, or the user's choice): the editor and the combo box take it, the read-only
     * text shows it. Never a reason to open the list.
     */
    private void valueChanged() {
        String text = shownValue();
        readOnlyText.setText(text);
        if (syncing) {
            return;
        }
        syncing = true;
        try {
            if (!text.equals(editor.getText())) {
                editor.setText(text);
            }
            combo.setValue(text);
        } finally {
            syncing = false;
        }
    }

    /**
     * The editor's text changed by the user: typing, deleting, pasting, undoing. It becomes the value (unless the
     * application bound the value, which then wins), and the list offers the suggestions that contain it.
     */
    private void editorTextChanged() {
        if (syncing) {
            return;
        }
        String typed = editor.getText() == null ? "" : editor.getText();
        boolean taken;
        syncing = true;
        try {
            taken = setValueByUser(value, typed);
            if (!taken) {
                // The application's binding decides: the editor goes back to its value.
                editor.setText(shownValue());
            }
            combo.setValue(shownValue());
        } finally {
            syncing = false;
        }
        readOnlyText.setText(shownValue());
        if (taken) {
            suggest(typed);
        }
    }

    /** The value as the field shows it: null as the empty text. */
    private String shownValue() {
        String text = value.get();
        return text == null ? "" : text;
    }

    // --- The list ------------------------------------------------------------------------------------

    /**
     * The user typed: the list offers the suggestions that contain the text, opened if it was closed, closed when
     * none does.
     */
    private void suggest(String typed) {
        filterText = typed;
        updateEntries();
        if (entries.isEmpty()) {
            hideList();
        } else if (!isListShowing() && userMayChangeValue()) {
            showList();
        }
    }

    /**
     * Puts the suggestions the filter text keeps (all of them without one) into the list, with nothing highlighted:
     * also when the suggestions change.
     */
    private void updateEntries() {
        List<String> kept = SuggestionFilter.filter(suggestions, filterText);
        entries.setAll(kept);
        if (list != null) {
            list.getSelectionModel().clearSelection();
            list.getFocusModel().focus(-1);
        }
    }

    /**
     * Opens the full list while the user may change the value: the chevron, and JavaFX's own openings (Alt+Down,
     * Alt+Up, F4, a screen reader's expand) through the combo box's {@code show()}. The combo box takes the focus
     * first, as JavaFX's own opening gives it.
     */
    private void openFullList() {
        if (!userMayChangeValue()) {
            return;
        }
        filterText = null;
        updateEntries();
        if (!combo.isFocused()) {
            combo.requestFocus();
        }
        showList();
    }

    /**
     * The list closed, so that the click on the chevron whose press closed it does not open it again (see
     * {@link ShellComboBox}). Package-private: the tests' seam, as the list shows and closes only in a window.
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
     * Returns whether the list shows. Package-private: the tests' seam, with {@link #showList()}.
     *
     * @return true while the list shows
     */
    boolean isListShowing() {
        return combo.isShowing();
    }

    /**
     * Enter in the open list, which JavaFX's popup hands its list first: the highlighted suggestion becomes the text
     * (without one, the list only closes). Consumed, so it neither selects in the list nor reaches a dialog around the
     * field. With a modifier it is the list's.
     */
    private void handleListEnter(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && !event.isAltDown() && !event.isControlDown() && !event.isMetaDown()
                && !event.isShiftDown()) {
            event.consume();
            chooseHighlighted();
        }
    }

    /** Enter in the open list: the highlighted suggestion becomes the text; without one, the list only closes. */
    private void chooseHighlighted() {
        String highlighted = list == null ? null : list.getSelectionModel().getSelectedItem();
        hideList();
        if (highlighted != null) {
            choose(highlighted);
        }
    }

    /**
     * The user chose a suggestion: it becomes the text, the caret after it, and the list closes. Only while the user
     * may change the value, and not when the application bound the value property.
     */
    private void choose(String suggestion) {
        hideList();
        if (setValueByUser(value, suggestion)) {
            editor.positionCaret(editor.getLength());
        }
    }

    /**
     * The combo box's skin was built: its list's view is the open list, which JavaFX's popup hands the keys first (its
     * own Up, Down, Page Up, Page Down, Home and End move the highlight, its Escape closes it), Enter here.
     */
    @SuppressWarnings("unchecked")
    private void skinChanged(Skin<?> skin) {
        if (list != null) {
            list.removeEventFilter(KeyEvent.KEY_PRESSED, listEnterFilter);
        }
        list = null;
        if (skin instanceof ComboBoxListViewSkin<?> listSkin && listSkin.getPopupContent() instanceof ListView<?> view) {
            list = (ListView<String>) view;
            list.addEventFilter(KeyEvent.KEY_PRESSED, listEnterFilter);
        }
    }

    // --- Read-only and bound -------------------------------------------------------------------------

    /**
     * Follows read-only and the binding: the combo box while the user may edit, else the text to select and copy in
     * its place; no list while read-only or bound, the chevron disabled while bound, the divider and the chevron hidden
     * while read-only.
     *
     * @param editable true while the user may edit: neither bound nor read-only
     */
    @Override
    protected void updateContentEditable(boolean editable) {
        if (!editable) {
            hideList();
        }
        Node shown = editable ? combo : readOnlyText;
        if (getContent() != shown) {
            // The shell calls this hook again for the new content, the one shown now, and moves the focus with it.
            setContent(shown);
            return;
        }
        boolean readOnly = isReadOnly();
        updateOwnControl(divider, !readOnly);
        updateOwnControl(chevron, !readOnly);
    }

    // --- Parts ---------------------------------------------------------------------------------------

    /**
     * The combo box's selection: it follows the combo box's value, which is always the editor's text, and nothing
     * else. JavaFX's combo box sets it from its value through the selection model's own setters; every request to
     * select (the list's highlight, the keys of JavaFX's combo box) is ignored, as it would write the selected entry
     * into the editor.
     */
    private final class ValueOnlySelection extends SingleSelectionModel<String> {

        @Override
        protected String getModelItem(int index) {
            return index >= 0 && index < entries.size() ? entries.get(index) : null;
        }

        @Override
        protected int getItemCount() {
            return entries.size();
        }

        @Override
        public void select(int index) {
            // Never the user's choice: see the class description.
        }

        @Override
        public void select(String item) {
            // Never the user's choice: see the class description.
        }

        @Override
        public void clearAndSelect(int index) {
            // Never the user's choice: see the class description.
        }

        @Override
        public void clearSelection() {
            // The selection follows the value only.
        }

        @Override
        public void clearSelection(int index) {
            // The selection follows the value only.
        }

        @Override
        public void selectPrevious() {
            // Never the user's choice: see the class description.
        }

        @Override
        public void selectNext() {
            // Never the user's choice: see the class description.
        }

        @Override
        public void selectFirst() {
            // Never the user's choice: see the class description.
        }

        @Override
        public void selectLast() {
            // Never the user's choice: see the class description.
        }
    }

    /** The texts as they are, both ways: the combo box's value is the editor's text. Null as the empty text. */
    private static final class TextConverter extends StringConverter<String> {

        @Override
        public String toString(String text) {
            return text == null ? "" : text;
        }

        @Override
        public String fromString(String text) {
            return text == null ? "" : text;
        }
    }

    /** A suggestion in the list: its text; a click on it chooses it. */
    private final class SuggestionCell extends GuardedComboBox.ShellCell<String> {

        private SuggestionCell() {
            super(combo);
            // The release of a click: the popup's list closes on it as well, so a press on a suggestion, held and
            // released, chooses the suggestion pressed, as JavaFX's combo box does.
            addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
                if (event.getButton() == MouseButton.PRIMARY && !isEmpty() && getItem() != null) {
                    choose(getItem());
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty ? null : item);
        }
    }
}
