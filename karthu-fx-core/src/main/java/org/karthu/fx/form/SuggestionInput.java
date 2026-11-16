package org.karthu.fx.form;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import org.karthu.fx.form.search.SearchQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A search field with a popup of suggestions: the user types, picks an entry from the popup under the field with the
 * arrows and Enter or a click, and the application learns the chosen entry. Built on {@link SearchInput}, so it looks
 * and behaves like a search field (the magnifier, the placeholder <i>Search</i>, the clear button, Escape), matches by
 * the same rule (every word of the search in one of an entry's texts, case and accents folded) and can also filter a
 * list, search a Markdown view and hand out highlight nodes; its normal use is the popup.
 *
 * <p><b>Entries.</b> {@link #getItems()} are the application's own objects, in their order. Of each entry the field
 * needs two things: its {@linkplain #itemTextProperty() item text}, the text that goes into the field when the entry
 * is chosen and that the popup's row shows (by default {@code String.valueOf(entry)}), and its
 * {@linkplain #searchTextsProperty() search texts}, the texts the search is matched against (by default the item text
 * alone). No entry may be null.
 *
 * <p><b>Opening.</b> The popup opens when the user types (also deletes or pastes) and the text then holds a word, a
 * character other than whitespace: with the entries the search matches, in the order of the items, the first one
 * preselected. While it is open it follows the typing, the first match preselected again after each change; when the
 * text holds no word any more it closes. Down (or Alt+Down) opens it as well: in the empty field with every entry, with
 * text with the entries the text matches. It never opens on the focus alone, never for a text the field puts in itself
 * (applying an entry, the chosen entry set), never for a text the application sets while the text field does not have
 * the focus (one set while it has the focus counts as typed), and never while the field is disabled or read-only. When
 * nothing matches, the popup stays open with one line, the {@linkplain #noMatchesTextProperty() <i>No matches</i>}, so
 * that it does not flicker away while the user types. It shows under the field, as wide as the field, up to
 * {@linkplain #visibleRowCountProperty() eight rows}, each as high as it is, then scrolls; its window shows only in a
 * shown window.
 *
 * <p><b>Rows.</b> By default a row shows the item text on one line, through {@link #highlight(String)}, its marks bold;
 * a text longer than the row is clipped after the last character that fits, with an ellipsis (&hellip;).
 * An application draws its own rows with a {@linkplain #cellFactoryProperty() cell factory}, a plain JavaFX one: rows
 * of any height, such as a card of a person with the name, the role, the address and the e-mail, each text through
 * {@link #highlight(String)} so that its marks follow the search, a name shown bold through a highlight node with
 * {@code search-strong}, whose marks the theme underlines. The field makes a click on any row apply its entry, and the
 * theme gives any row the popup's look, the preselected one the selection's, so the application's cell does nothing
 * for either. <b>No row is wider than the popup:</b> every row, the default one and the application's, gets the
 * list's width (beside the vertical scroll bar while the list scrolls), whatever it asks for, and the popup never
 * scrolls sideways; an application's row wraps or clips what does not fit, as it chooses (a text flow such as a
 * highlight node wraps its lines by itself).
 *
 * <p><b>Footer.</b> Turned on with {@link #footerVisibleProperty()}, the popup shows a footer under the rows: on the
 * left the number of matches and of all entries through the {@linkplain #footerCountFormatProperty() count format}
 * (<i>2 of 14</i>, or <i>2 of 14 people</i>), following the typing, on the right the
 * {@linkplain #footerHintTextProperty() key hints}, <i>&uarr;&darr; select &middot; Enter apply &middot; Esc
 * close</i>.
 *
 * <p><b>Keys while the popup is open</b>, all handled by the field: the text field keeps the focus and the caret
 * while the user moves through the popup. Up and Down move the preselection one row, Page Up and Page Down as many rows
 * as the popup shows at most ({@linkplain #visibleRowCountProperty() eight} by default), Home and End to the first and
 * the last row, each stopping at the ends; Enter applies the preselected entry (with
 * nothing to apply, when nothing matches, it only closes the popup); Escape closes the popup and keeps the text, and a
 * second Escape empties the field (see {@link SearchInput}); Tab closes the popup without applying and moves the focus
 * on as always. These keys are consumed, Tab excepted; with a modifier down (Shift, Control, Alt, the meta key) they
 * are the text field's, so Shift+Home still selects. With the popup closed every key is the text field's, Down and
 * Alt+Down excepted. A field that searches a view (see {@link SearchInput}) goes to the view's next match with Enter
 * only while the popup is closed; while it shows, Enter applies the preselected entry, and Shift+Enter goes to the
 * previous match.
 *
 * <p><b>Mouse.</b> A click on a row applies its entry. The popup closes when the text field loses the focus, the
 * window moves, the field becomes disabled or read-only, or the user presses the mouse in the window outside the field
 * and the popup (the press still does what it does there). A press in the field itself, such as one that moves the
 * caret, keeps the popup open; a click on the clear button closes it, as it empties the field.
 *
 * <p><b>Applying</b> an entry, in this order: the popup closes; the text becomes the entry's item text, the caret at
 * its end, without the popup opening again and without the chosen entry being cleared; the
 * {@linkplain #chosenItemProperty() chosen entry} becomes the entry; the {@linkplain #onItemChosenProperty() handler}
 * receives it. So a listener of the chosen entry and the handler both see the new text and the closed popup, and the
 * handler sees the new chosen entry.
 *
 * <p><b>Chosen entry.</b> {@link #chosenItemProperty()} is the entry the user applied last, read and write. It becomes
 * null as soon as the text changes otherwise, by the user or by the application, so the field and the chosen entry
 * never contradict each other. The application may set it: an entry puts its item text into the field, the caret at
 * its end, without opening the popup (it closes one that shows); null leaves the text alone. The handler hears only
 * the user's choices.
 *
 * <p><b>Changes of the entries</b> (the items, the item text, the search texts) while the popup is open filter it
 * again and keep the preselection on the same entry (by {@code equals}) if it still matches, else on the first.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The field carries {@code form-suggestion-input} beside the search field's
 * {@code form-search-input} and the shell's {@code form-frame} (see {@link SearchInput} for its parts).
 * <ul>
 *   <li>{@code form-suggestion-popup}: the popup's content, a {@link VBox} that draws the popup's frame</li>
 *   <li>{@code form-suggestion-list}: the popup's list, a {@link ListView} (keeps {@code list-view}); its rows are
 *       list cells ({@code list-cell}), the preselected one {@code :selected}, by default each showing the item text
 *       through {@link #highlight(String)}, its marks ({@code search-match}) bold; on the preselected row the runs of
 *       every highlight node in the selection text colour, the marks keeping their look</li>
 *   <li>{@code form-suggestion-ellipsis}: the ellipsis of a default row whose text does not fit, a {@link TextFlow}
 *       beside {@code search-highlight} with one run ({@code search-text}), so that it takes the look of the row's
 *       plain runs; shown only while the text is clipped</li>
 *   <li>{@code form-suggestion-empty}: the line <i>No matches</i> in the list's place, a {@link Label}</li>
 *   <li>{@code form-suggestion-footer}: the footer under the list or the line, an {@link HBox}, shown while turned
 *       on</li>
 *   <li>{@code form-suggestion-footer-count}: the footer's count on its left, a {@link Label}</li>
 *   <li>{@code form-suggestion-footer-hint}: the footer's key hints on its right, a {@link Label}</li>
 * </ul>
 * In a row of the application, {@code search-strong} on a highlight node (see {@link SearchInput}) shows its runs bold
 * and its marks underlined.
 * A suggestion input adds no pseudo-class of its own: the search field's and the shell's apply.
 *
 * <p>Like every karthu-fx control, the field is used on the JavaFX Application Thread only.
 *
 * @param <T> the type of the entries
 */
public class SuggestionInput<T> extends SearchInput {

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObjectProperty<Function<T, String>> itemText =
            new SimpleObjectProperty<>(this, "itemText", defaultItemText());
    private final ObjectProperty<Function<T, List<String>>> searchTexts =
            new SimpleObjectProperty<>(this, "searchTexts", defaultSearchTexts());
    private final ObjectProperty<T> chosenItem = new SimpleObjectProperty<>(this, "chosenItem");
    private final ObjectProperty<Consumer<T>> onItemChosen = new SimpleObjectProperty<>(this, "onItemChosen");
    private final StringProperty noMatchesText = new SimpleStringProperty(this, "noMatchesText", "No matches");
    private final ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactory =
            new SimpleObjectProperty<>(this, "cellFactory");
    private final IntegerProperty visibleRowCount = new SimpleIntegerProperty(this, "visibleRowCount", 8);
    private final BooleanProperty footerVisible = new SimpleBooleanProperty(this, "footerVisible", false);
    private final StringProperty footerCountFormat =
            new SimpleStringProperty(this, "footerCountFormat", "{0} of {1}");
    // The arrows up and down and the middle dot as escapes: the build does not depend on the source encoding.
    private final StringProperty footerHintText = new SimpleStringProperty(this, "footerHintText",
            "\u2191\u2193 select \u00B7 Enter apply \u00B7 Esc close");

    /** The entries the popup's list shows: those the search matches, in the order of the items. */
    private final ObservableList<T> matches = FXCollections.observableArrayList();
    /** The popup's list: never focused, so the keys stay with the text field; its selection is the preselection. */
    private final ListView<T> list = new ListView<>(matches);
    /** The line in the list's place when nothing matches. */
    private final Label emptyLine = new Label();
    /** The footer's count, on its left: the matches and all entries. */
    private final Label footerCount = new Label();
    /** The footer's key hints, on its right. */
    private final Label footerHint = new Label();
    /** The room between the footer's count and its hints, which pushes the hints to the right end. */
    private final Region footerGap = new Region();
    /** The footer under the list or the line, shown while the application turns it on. */
    private final HBox footer = new HBox(footerCount, footerGap, footerHint);
    /** The popup's content: the list, or the line in its place, and the footer under them. */
    private final VBox content = new VBox(list, emptyLine, footer);
    /**
     * The popup's window, which shows the content under the field in a shown window: it closes the popup when the
     * window around it moves, and hands every press in that window to {@link #pressedInWindow(MouseEvent)}.
     */
    private final SuggestionPopup popupWindow =
            new SuggestionPopup(this, content, this::hidePopup, this::popupWindowHidden, this::pressedInWindow);

    /** Whether the popup is open: from opening to closing, also where no window can show it. */
    private boolean popupOpen;
    /** True while the field puts an entry's item text into the text field itself: no edit, nothing opens. */
    private boolean settingItemText;
    /** True while the field sets the chosen entry for the user's choice, which already put the text. */
    private boolean applying;

    /**
     * Creates an empty suggestion input without entries, with the placeholder <i>Search</i>.
     */
    public SuggestionInput() {
        getStyleClass().add("form-suggestion-input");
        buildPopup();

        items.addListener((ListChangeListener<T>) change -> entriesChanged());
        itemText.addListener((observable, oldFunction, newFunction) -> entriesChanged());
        searchTexts.addListener((observable, oldFunction, newFunction) -> entriesChanged());
        chosenItem.addListener((observable, oldItem, newItem) -> chosenItemChanged(newItem));

        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
        // The focus gone from the text field (Tab, a click elsewhere), or the window gone to the background: the popup
        // closes.
        whenFocusLeaves(getTextField(), this::hidePopup);
        getTextField().focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (!focused) {
                hidePopup();
            }
        });
        disabledProperty().addListener((observable, wasDisabled, disabled) -> {
            if (disabled) {
                hidePopup();
            }
        });
        sceneProperty().addListener((observable, oldScene, newScene) -> hidePopup());
    }

    /** Builds the popup's content: the list, the line in its place and the footer. */
    private void buildPopup() {
        content.getStyleClass().add("form-suggestion-popup");
        // As wide as the field, whatever the list or the line would ask for.
        content.prefWidthProperty().bind(widthProperty());
        content.setMinWidth(Region.USE_PREF_SIZE);
        content.setMaxWidth(Region.USE_PREF_SIZE);

        list.getStyleClass().add("form-suggestion-list");
        list.setFocusTraversable(false);
        list.setSkin(new SuggestionRowsSkin<>(list, visibleRowCount, this::listWidth));
        list.setCellFactory(rows(null));
        // A new factory of the application: the list builds its rows anew, the shown ones included.
        cellFactory.addListener((observable, oldFactory, newFactory) -> list.setCellFactory(rows(newFactory)));

        emptyLine.getStyleClass().add("form-suggestion-empty");
        emptyLine.textProperty().bind(Bindings.createStringBinding(this::shownNoMatchesText, noMatchesText));
        emptyLine.setMaxWidth(Double.MAX_VALUE);
        showRows(false);
        buildFooter();
    }

    /**
     * Builds the footer: the count on the left, which follows the matches, all entries and the format; the gap that
     * pushes the hints to the right end; shown, and taking room, only while it is turned on.
     */
    private void buildFooter() {
        footer.getStyleClass().add("form-suggestion-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.visibleProperty().bind(footerVisible);
        footer.managedProperty().bind(footerVisible);
        HBox.setHgrow(footerGap, Priority.ALWAYS);

        footerCount.getStyleClass().add("form-suggestion-footer-count");
        // The count keeps its room; long hints are shortened first.
        footerCount.setMinWidth(Region.USE_PREF_SIZE);
        footerCount.textProperty().bind(
                Bindings.createStringBinding(this::footerCountText, footerCountFormat, matches, items));
        footerHint.getStyleClass().add("form-suggestion-footer-hint");
        footerHint.textProperty().bind(Bindings.createStringBinding(this::shownFooterHintText, footerHintText));
    }

    // --- Entries -------------------------------------------------------------------------------------

    /**
     * The entries the popup offers, the application's own objects, in their order (default empty). No entry may be
     * null. Changing them while the popup is open filters it again.
     *
     * @return the entries, a modifiable list
     */
    public final ObservableList<T> getItems() {
        return items;
    }

    /**
     * The item text of an entry: the text that goes into the field when the entry is chosen, and that the popup's row
     * shows (default {@code String.valueOf(entry)}). Null counts as the default; a function that returns null gives
     * the empty text.
     *
     * @return the item text property
     */
    public final ObjectProperty<Function<T, String>> itemTextProperty() {
        return itemText;
    }

    /**
     * Returns the item text function.
     *
     * @return the function; null only when it was set to null, which counts as the default
     */
    public final Function<T, String> getItemText() {
        return itemText.get();
    }

    /**
     * Sets the item text function.
     *
     * @param function the function from an entry to its item text; null for {@code String.valueOf(entry)}
     */
    public final void setItemText(Function<T, String> function) {
        itemText.set(function);
    }

    /**
     * The search texts of an entry: the texts the search is matched against (default the item text alone). An entry
     * matches when every word of the search occurs in one of them; give every text the row shows, so that every match
     * shows its mark. Null counts as the default; a function that returns null gives no text, a null text counts as
     * the empty one.
     *
     * @return the search texts property
     */
    public final ObjectProperty<Function<T, List<String>>> searchTextsProperty() {
        return searchTexts;
    }

    /**
     * Returns the search texts function.
     *
     * @return the function; null only when it was set to null, which counts as the default
     */
    public final Function<T, List<String>> getSearchTexts() {
        return searchTexts.get();
    }

    /**
     * Sets the search texts function.
     *
     * @param function the function from an entry to its search texts; null for the item text alone
     */
    public final void setSearchTexts(Function<T, List<String>> function) {
        searchTexts.set(function);
    }

    /** The item text of an entry: the function's, the empty text for a null result. */
    private String itemTextOf(T item) {
        Function<T, String> function = itemText.get();
        String text = function == null ? String.valueOf(item) : function.apply(item);
        return text == null ? "" : text;
    }

    /** The search texts of an entry: the function's, none for a null result; the item text alone by default. */
    private List<String> searchTextsOf(T item) {
        Function<T, List<String>> function = searchTexts.get();
        if (function == null) {
            return List.of(itemTextOf(item));
        }
        List<String> texts = function.apply(item);
        return texts == null ? List.of() : texts;
    }

    private static <V> Function<V, String> defaultItemText() {
        return item -> String.valueOf(item);
    }

    /** The default search texts: the item text alone, whichever item text function is set then. */
    private Function<T, List<String>> defaultSearchTexts() {
        return item -> List.of(itemTextOf(item));
    }

    /** The items or a function changed: an open popup filters again, keeping its preselection where it can. */
    private void entriesChanged() {
        if (popupOpen) {
            showMatches(true);
            list.refresh();
        }
    }

    // --- Chosen entry --------------------------------------------------------------------------------

    /**
     * The entry the user chose last, read and write: applying an entry sets it, any other change of the text, by the
     * user or by the application, makes it null. Set by the application, an entry puts its item text into the field,
     * the caret at its end, without opening the popup (one that shows closes); null leaves the text alone.
     *
     * @return the chosen item property
     */
    public final ObjectProperty<T> chosenItemProperty() {
        return chosenItem;
    }

    /**
     * Returns the entry the user chose last.
     *
     * @return the chosen entry, or null when there is none
     */
    public final T getChosenItem() {
        return chosenItem.get();
    }

    /**
     * Sets the chosen entry: its item text goes into the field, without opening the popup; null leaves the text alone.
     *
     * @param item the entry, or null for none
     */
    public final void setChosenItem(T item) {
        chosenItem.set(item);
    }

    /**
     * What the field does when the user chooses an entry (default none): it receives the entry, after the text and
     * the chosen entry are set and the popup closed. It hears only the user's choices, never the application's.
     *
     * @return the property of the handler
     */
    public final ObjectProperty<Consumer<T>> onItemChosenProperty() {
        return onItemChosen;
    }

    /**
     * Returns the handler of the user's choices.
     *
     * @return the handler, or null for none
     */
    public final Consumer<T> getOnItemChosen() {
        return onItemChosen.get();
    }

    /**
     * Sets the handler of the user's choices.
     *
     * @param handler the handler, or null for none
     */
    public final void setOnItemChosen(Consumer<T> handler) {
        onItemChosen.set(handler);
    }

    /**
     * The chosen entry changed. While the field applies the user's choice the text is set already; set by the
     * application, an entry puts its item text into the field (closing a popup that shows), null leaves the text.
     */
    private void chosenItemChanged(T item) {
        if (applying || item == null) {
            return;
        }
        hidePopup();
        putItemText(item);
    }

    /**
     * Puts an entry's item text into the field, the caret at its end, as the field's own change: it opens no popup and
     * keeps the chosen entry.
     */
    private void putItemText(T item) {
        settingItemText = true;
        try {
            setText(itemTextOf(item));
            getTextField().end();
        } finally {
            settingItemText = false;
        }
    }

    /**
     * The user applies an entry, through Enter or a click, in this order: the popup closes; the text becomes the item
     * text, the caret at its end, without the popup opening again or the chosen entry being cleared; the chosen entry
     * becomes the entry (unless the application bound it); the handler receives it.
     */
    private void apply(T item) {
        if (!userMayChangeValue()) {
            return;
        }
        hidePopup();
        putItemText(item);
        if (!chosenItem.isBound()) {
            applying = true;
            try {
                chosenItem.set(item);
            } finally {
                applying = false;
            }
        }
        Consumer<T> handler = onItemChosen.get();
        if (handler != null) {
            handler.accept(item);
        }
    }

    // --- Texts ---------------------------------------------------------------------------------------

    /**
     * The line the popup shows in place of its list when nothing matches (default <i>No matches</i>), a property so
     * that an application can translate it. Null counts as empty.
     *
     * @return the no matches text property
     */
    public final StringProperty noMatchesTextProperty() {
        return noMatchesText;
    }

    /**
     * Returns the line shown when nothing matches.
     *
     * @return the no matches text
     */
    public final String getNoMatchesText() {
        return noMatchesText.get();
    }

    /**
     * Sets the line shown when nothing matches.
     *
     * @param text the no matches text; null or empty for an empty line
     */
    public final void setNoMatchesText(String text) {
        noMatchesText.set(text);
    }

    /** The no matches text the line shows: the property's, null as empty. */
    private String shownNoMatchesText() {
        String text = noMatchesText.get();
        return text == null ? "" : text;
    }

    // --- Footer --------------------------------------------------------------------------------------

    /**
     * Whether the popup shows its footer under the rows (default false): on the left the number of matches and of all
     * entries through the {@linkplain #footerCountFormatProperty() count format}, following the typing, on the right
     * the {@linkplain #footerHintTextProperty() key hints}. Turned off, it takes no room.
     *
     * @return the footer visible property
     */
    public final BooleanProperty footerVisibleProperty() {
        return footerVisible;
    }

    /**
     * Returns whether the popup shows its footer.
     *
     * @return true while the footer shows
     */
    public final boolean isFooterVisible() {
        return footerVisible.get();
    }

    /**
     * Turns the popup's footer on or off.
     *
     * @param visible true to show the footer
     */
    public final void setFooterVisible(boolean visible) {
        footerVisible.set(visible);
    }

    /**
     * The format of the footer's count (default <i>{0} of {1}</i>), as the search field's
     * {@linkplain #matchCountFormatProperty() counter}: {@code {0}} stands for the number of matches, the entries the
     * popup offers, {@code {1}} for the number of all {@linkplain #getItems() entries}, each wherever and as often as
     * it occurs; everything else is shown as it is, such as <i>{0} of {1} people</i>. A property so that an application
     * can translate it. Null counts as empty.
     *
     * @return the footer count format property
     */
    public final StringProperty footerCountFormatProperty() {
        return footerCountFormat;
    }

    /**
     * Returns the format of the footer's count.
     *
     * @return the footer count format, empty or null when the count shows no text
     */
    public final String getFooterCountFormat() {
        return footerCountFormat.get();
    }

    /**
     * Sets the format of the footer's count: {@code {0}} the number of matches, {@code {1}} the number of all entries.
     *
     * @param format the footer count format; null or empty for no text
     */
    public final void setFooterCountFormat(String format) {
        footerCountFormat.set(format);
    }

    /**
     * The key hints on the right of the footer (default <i>&uarr;&darr; select &middot; Enter apply &middot; Esc
     * close</i>), a property so that an application can translate it. Null counts as empty.
     *
     * @return the footer hint text property
     */
    public final StringProperty footerHintTextProperty() {
        return footerHintText;
    }

    /**
     * Returns the footer's key hints.
     *
     * @return the footer hint text
     */
    public final String getFooterHintText() {
        return footerHintText.get();
    }

    /**
     * Sets the footer's key hints.
     *
     * @param text the footer hint text; null or empty for none
     */
    public final void setFooterHintText(String text) {
        footerHintText.set(text);
    }

    /** The footer's count: the matches the popup offers and all entries, through the format. */
    private String footerCountText() {
        return countText(footerCountFormat.get(), matches.size(), items.size());
    }

    /** The key hints the footer shows: the property's, null as empty. */
    private String shownFooterHintText() {
        String text = footerHintText.get();
        return text == null ? "" : text;
    }

    // --- The text and the popup ----------------------------------------------------------------------

    /**
     * The text changed, after the search: unless the field put an item text itself, the chosen entry is no longer
     * what the field shows, and the popup follows the text. A text without a word closes it; an open popup filters
     * again, the first match preselected; a closed one opens when the user typed.
     */
    @Override
    void textChanged() {
        super.textChanged();
        if (settingItemText) {
            return;
        }
        if (!chosenItem.isBound()) {
            chosenItem.set(null);
        }
        if (query().isEmpty()) {
            hidePopup();
        } else if (popupOpen) {
            showMatches(false);
        } else if (typedByUser()) {
            openPopup();
        }
    }

    /**
     * Whether the text field has its scene's focus, where the user types: a text set while it has none comes from the
     * application.
     */
    private boolean typedByUser() {
        Scene scene = getScene();
        return scene != null && scene.getFocusOwner() == getTextField();
    }

    /**
     * Opens the popup with the entries the search matches, the first preselected: only while the user may change the
     * text (neither disabled nor read-only).
     *
     * @return true when the popup opened
     */
    private boolean openPopup() {
        if (popupOpen || !userMayChangeValue()) {
            return false;
        }
        showMatches(false);
        popupOpen = true;
        showPopupWindow();
        return true;
    }

    /**
     * Closes the popup, if it is open; the text stays.
     *
     * @return true when the popup was open and closed
     */
    @Override
    boolean hidePopup() {
        if (!popupOpen) {
            return false;
        }
        popupOpen = false;
        hidePopupWindow();
        return true;
    }

    /**
     * Puts the entries the search matches into the list, in the order of the items, or the line in its place when
     * none does, and preselects: the entry preselected before if asked and it still matches, else the first.
     *
     * @param keepPreselection true to keep the preselected entry where it still matches
     */
    private void showMatches(boolean keepPreselection) {
        T preselected = keepPreselection ? list.getSelectionModel().getSelectedItem() : null;
        SearchQuery search = query();
        List<T> kept = new ArrayList<>();
        for (T item : items) {
            if (search.matches(searchTextsOf(item))) {
                kept.add(item);
            }
        }
        matches.setAll(kept);
        showRows(!kept.isEmpty());
        int index = preselected == null ? -1 : kept.indexOf(preselected);
        preselect(kept.isEmpty() ? -1 : Math.max(index, 0));
    }

    /** Shows the list, or the line in its place: a hidden one takes no room. */
    private void showRows(boolean rows) {
        list.setVisible(rows);
        list.setManaged(rows);
        emptyLine.setVisible(!rows);
        emptyLine.setManaged(!rows);
    }

    /** Preselects the row at the index, scrolled into view, or none for -1. */
    private void preselect(int index) {
        if (index < 0) {
            list.getSelectionModel().clearSelection();
            list.getFocusModel().focus(-1);
            return;
        }
        list.getSelectionModel().clearAndSelect(index);
        list.getFocusModel().focus(index);
        if (list.getSkin() instanceof SuggestionRowsSkin<?> rows) {
            rows.reveal(index);
        }
    }

    /** Moves the preselection by rows, down for a positive number, stopping at the first and the last row. */
    private void movePreselection(int rows) {
        if (matches.isEmpty()) {
            return;
        }
        int index = list.getSelectionModel().getSelectedIndex() + rows;
        preselect(Math.clamp(index, 0, matches.size() - 1));
    }

    /** Enter in the open popup: the preselected entry is applied; without one the popup only closes. */
    private void applyPreselected() {
        T preselected = list.getSelectionModel().getSelectedItem();
        if (preselected == null) {
            hidePopup();
        } else {
            apply(preselected);
        }
    }

    // --- Keys ----------------------------------------------------------------------------------------

    /**
     * The popup's keys, ahead of the text field (a filter): with the popup closed Down and Alt+Down open it; while it
     * is open the keys of the class description move, apply and close. Escape is the search field's (see
     * {@link SearchInput}), which closes the popup first.
     */
    private void handleKey(KeyEvent event) {
        KeyCode code = event.getCode();
        // With Control, Shift or the meta key down a key is the text field's (Shift+Home selects); Alt is asked apart,
        // as Alt+Down opens the popup like Down.
        boolean controlShiftOrMetaDown = event.isControlDown() || event.isMetaDown() || event.isShiftDown();
        if (!popupOpen) {
            if (code == KeyCode.DOWN && !controlShiftOrMetaDown && openPopup()) {
                event.consume();
            }
            return;
        }
        if (code == KeyCode.TAB) {
            // Closes without applying; the focus moves on as always.
            hidePopup();
            return;
        }
        if (controlShiftOrMetaDown || event.isAltDown()) {
            return;
        }
        switch (code) {
            case UP -> movePreselection(-1);
            case DOWN -> movePreselection(1);
            case PAGE_UP -> movePreselection(-SuggestionRowsSkin.rowsAtMost(visibleRowCount.get()));
            case PAGE_DOWN -> movePreselection(SuggestionRowsSkin.rowsAtMost(visibleRowCount.get()));
            case HOME -> movePreselection(-matches.size());
            case END -> movePreselection(matches.size());
            case ENTER -> applyPreselected();
            default -> {
                return;
            }
        }
        event.consume();
    }

    // --- Read-only -----------------------------------------------------------------------------------

    /**
     * Follows read-only as the search field does, and closes the popup while the user may not edit.
     *
     * @param editable true while the user may edit the text: neither bound nor read-only
     */
    @Override
    protected void updateContentEditable(boolean editable) {
        super.updateContentEditable(editable);
        if (!editable) {
            hidePopup();
        }
    }

    // --- The popup's window --------------------------------------------------------------------------

    /**
     * Returns whether the popup is open. Package-private: the search field leaves Enter to an open popup, and the
     * tests' seam, as its window shows only in a shown window.
     *
     * @return true while the popup is open
     */
    @Override
    boolean isPopupOpen() {
        return popupOpen;
    }

    /**
     * Returns the popup's content, built whether or not its window ever shows. Package-private: the tests' seam.
     *
     * @return the popup's content
     */
    Region popupContent() {
        return content;
    }

    /**
     * Shows the popup's window under the field, in a shown window only, as a popup needs one: the popup closes when
     * that window moves, and every press in it comes to {@link #pressedInWindow(MouseEvent)}. Package-private: the
     * tests' seam, which replaces it, so that a test opens no window.
     */
    void showPopupWindow() {
        popupWindow.show();
    }

    /**
     * Hides the popup's window if it shows. Package-private: the tests' seam, with {@link #showPopupWindow()}.
     */
    void hidePopupWindow() {
        popupWindow.hide();
    }

    /** The popup's window hid, by the field or by itself (the window around it hiding): the popup is closed. */
    private void popupWindowHidden() {
        popupOpen = false;
    }

    /**
     * A press of the mouse in the window around the popup while the popup's window shows, before the press reaches its
     * target: a press outside the field closes the popup, as a popup that hides by itself does, and still does what it
     * does there, such as focusing another field; a press in the field, such as one that moves the caret, or one on the
     * magnifier, keeps the popup open. A press in the popup is its own window's and never comes here. The press is
     * never consumed. Package-private: the tests' seam for this decision, as the popup's window shows only in a shown
     * window.
     *
     * @param event the press
     */
    void pressedInWindow(MouseEvent event) {
        if (!isInField(event.getTarget())) {
            hidePopup();
        }
    }

    /** Whether an event's target is the field or a node inside it. */
    private boolean isInField(EventTarget target) {
        Node node = target instanceof Node targetNode ? targetNode : null;
        while (node != null) {
            if (node == this) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    // --- Cells ---------------------------------------------------------------------------------------

    /**
     * The factory of the popup's rows (default null, the default row): a plain JavaFX cell factory, called with the
     * popup's list for each row it builds, which returns a new {@link ListCell} for the entries. The default row shows
     * the item text on one line through {@link #highlight(String)}, its marks bold, a text longer than the row clipped
     * with an ellipsis. An application's row may be of any height, several lines such as a card of a person, and shows
     * its texts through {@link #highlight(String)}, so that its marks follow the search; it gives every text it shows as
     * a {@linkplain #searchTextsProperty() search text}. Every row gets the list's width: the field sets the row's
     * preferred width to zero, so that no row pushes the list wider, and the row wraps or clips what does not fit, its
     * height measured at that width.
     *
     * <p>The row needs nothing else: the field makes a click on it apply its entry, and it takes the theme's look of
     * the popup's rows by its style class {@code list-cell}, the preselected one by {@code :selected}: the selection
     * colour under it, its highlight nodes' runs in the selection text colour, the marks keeping their look. A
     * highlight node the row shows bold carries {@code search-strong}: the theme draws its runs bold and its marks
     * underlined (see {@link SearchInput#highlight(String)}). A new factory, also null, builds the rows anew, the shown
     * ones included.
     *
     * @return the cell factory property
     */
    public final ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactoryProperty() {
        return cellFactory;
    }

    /**
     * Returns the factory of the popup's rows.
     *
     * @return the cell factory, or null for the default row
     */
    public final Callback<ListView<T>, ListCell<T>> getCellFactory() {
        return cellFactory.get();
    }

    /**
     * Sets the factory of the popup's rows.
     *
     * @param factory the cell factory, a new cell for each call; null for the default row
     */
    public final void setCellFactory(Callback<ListView<T>, ListCell<T>> factory) {
        cellFactory.set(factory);
    }

    /**
     * The rows the popup shows at most before it scrolls (default 8): rows of different heights each as high as it is,
     * so the popup is as tall as its first rows up to this count, shorter with fewer matches. Page Up and Page Down
     * move the preselection this many rows. A count below 1 counts as 1.
     *
     * @return the visible row count property
     */
    public final IntegerProperty visibleRowCountProperty() {
        return visibleRowCount;
    }

    /**
     * Returns the rows the popup shows at most before it scrolls.
     *
     * @return the visible row count
     */
    public final int getVisibleRowCount() {
        return visibleRowCount.get();
    }

    /**
     * Sets the rows the popup shows at most before it scrolls.
     *
     * @param rows the visible row count; below 1 counts as 1
     */
    public final void setVisibleRowCount(int rows) {
        visibleRowCount.set(rows);
    }

    /**
     * The popup list's own cell factory around the application's: each row the application's cell, or the default one
     * for none, as a row of the popup. A new instance for each factory, so that the list builds its rows anew.
     */
    private Callback<ListView<T>, ListCell<T>> rows(Callback<ListView<T>, ListCell<T>> factory) {
        return view -> asRow(createCell(factory, view));
    }

    /** A row of the popup's list: the application's cell, or the default one, which shows the item text marked. */
    private ListCell<T> createCell(Callback<ListView<T>, ListCell<T>> factory, ListView<T> view) {
        if (factory == null) {
            return new SuggestionCell();
        }
        ListCell<T> cell = factory.call(view);
        if (cell == null) {
            throw new IllegalStateException("the cell factory of a suggestion input returned no cell");
        }
        return cell;
    }

    /**
     * Makes the cell a row of the popup, the application's too: as wide as the list, and a click on it applies its
     * entry. A list makes its rows as wide as the widest one asks for and scrolls sideways when that is wider than the
     * list; a row that asks for no width never pushes the list wider, so every row gets the list's width, and the row
     * clips or wraps what does not fit.
     */
    private ListCell<T> asRow(ListCell<T> cell) {
        cell.setPrefWidth(0);
        cell.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && !cell.isEmpty() && cell.getItem() != null) {
                event.consume();
                apply(cell.getItem());
            }
        });
        return cell;
    }

    /**
     * The default row: the item text through {@link #highlight(String)}, so that its marks follow the search, which the
     * theme draws bold; on one line, a text longer than the row clipped with an ellipsis.
     */
    private final class SuggestionCell extends ListCell<T> {

        private SuggestionCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            String text = itemTextOf(item);
            setGraphic(new SuggestionLine(highlight(text), text));
        }
    }

    /**
     * The width of the popup's list: the field's, as the popup's content is, inside the content's insets. Known as soon
     * as the field is laid out, before the list ever is.
     */
    private double listWidth() {
        return getWidth() - content.snappedLeftInset() - content.snappedRightInset();
    }
}
