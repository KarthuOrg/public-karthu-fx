package org.karthu.fx.form;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.karthu.fx.form.search.Mark;
import org.karthu.fx.form.search.SearchQuery;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A search field in the look of the form fields: a {@link FieldFrame} with a magnifier on the left, the text with the
 * placeholder <i>Search</i>, and a clear button (&times;) on the right as soon as there is text. It looks like the form
 * fields next to it, with the same frame, focus border and disabled look, so a form with a search reads as one piece.
 *
 * <p><b>Not a form input.</b> A search field does not implement {@link FormInput}: it has no value in the form sense,
 * no label row, no messages of its own and offers no binding. Its {@linkplain #getBinding() binding} stays not
 * bindable, so the {@code {}} icon never shows; an application leaves it so. The field does not enforce it: the
 * binding is the shell's, whose properties an application can still set, and the shell follows them before any
 * listener of the field could set them back (a search field made bindable or bound would show the {@code {}} icon or
 * the chip like any field).
 *
 * <p><b>Text.</b> {@link #textProperty()} is the text field's own text, exactly as typed. The
 * {@linkplain #promptTextProperty() placeholder} shows while it is empty, <i>Search</i> unless the application sets
 * another (null counts as empty).
 *
 * <p><b>Clear button.</b> The &times; (Bootstrap {@code X}) shows while there is text and the field is neither disabled
 * nor read-only. A click empties the field and keeps the focus in the text field, or gives it to the text field, so the
 * user can type a new search at once. It is not focus traversable, as the magnifier is not: Tab passes the field in one
 * step. Its tooltip and accessible text are {@linkplain #clearTextProperty() <i>Clear</i>}.
 *
 * <p><b>Keyboard</b>, in the text field, the field's {@linkplain #getFocusTarget() focus target}: the text field's own
 * keys, and Escape (without a modifier), which empties a field with text and is consumed, so a connected list shows
 * every entry again. In an empty field, and while the user may not change the text (read-only), Escape changes nothing
 * and passes on unconsumed, so a dialog around the field can still close on it.
 *
 * <p><b>Matching.</b> The text is a search: split at any run of whitespace into words. An entry matches when every word
 * occurs, as a contiguous part, in its text, ignoring case and accents ({@code zurich} finds {@code Zürich}, and
 * {@code zürich} finds {@code Zurich}). An empty search, or one of whitespace only, matches every entry and marks
 * nothing. The sharp s and {@code ss}, ligatures and scripts beyond their accents are not folded.
 *
 * <p><b>List filter.</b> {@link #filter(FilteredList, Function)} connects the field to one list, a
 * {@link FilteredList} over the application's list: from then on the field sets its predicate on every change of the
 * text, so the list keeps the matching entries as the user types, and shows every entry again when the field is
 * emptied. A second call replaces the connection, null ends it; the list connected before then shows every entry.
 *
 * <p><b>Counter.</b> While a list is connected and there is text, a counter between the text and the &times; shows how
 * many entries match and how many there are, <i>1 of 2</i> through the {@linkplain #matchCountFormatProperty() format}
 * <i>{0} of {1}</i>. It follows the text and every change of the list and of the application's list under it, and the
 * application never computes it. With no match it carries the pseudo-class {@code :no-match}, which the theme draws in
 * the warning colour. It stays while read-only and disabled, as it changes nothing.
 *
 * <p><b>Marks in the application's cells.</b> {@link #highlight(String)} hands out a text building block for the
 * application's own cells, such as the rows of the filtered list: a node that shows a text with every occurrence of the
 * search's words marked, following the search by itself, never kept alive by the field.
 *
 * <p><b>Read-only and disabled</b>, the shell's (see {@link FieldShell}): {@linkplain #readOnlyProperty() read-only},
 * the text can be selected and copied but not changed, and the &times; hides, as the shell does with its icons; the
 * magnifier stays. Disabled, also through a parent, the field is greyed out once with the magnifier, as a disabled
 * button, and the &times; hides. The application still sets the text.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The field carries {@code form-search-input} beside the shell's {@code form-frame}, and its text
 * field the frame's {@code form-frame-text} (see {@link FieldShell} and {@link FieldFrame} for their classes and
 * pseudo-classes).
 * <ul>
 *   <li>{@code form-search-icon}: the magnifier (Bootstrap {@code SEARCH}), a region holding the icon, the shell's
 *       leading graphic beside {@code form-shell-leading}</li>
 *   <li>{@code form-search-count}: the counter, a {@link Label}, an own control of the shell before the clear
 *       button</li>
 *   <li>{@code form-search-clear}: the clear button, a {@link Button} beside {@code form-frame-icon} and
 *       {@code icon-button}, an own control of the shell at its right end</li>
 *   <li>{@code search-highlight}: a highlight node, a {@link TextFlow} from {@link #highlight(String)}, wherever the
 *       application puts it</li>
 *   <li>{@code search-text}: each run of a highlight node, a {@link Text}</li>
 *   <li>{@code search-match}: a marked run, beside {@code search-text}</li>
 *   <li>{@code search-strong}: set by the application on a highlight node, beside {@code search-highlight}, whose text
 *       it shows bold, such as a name: the theme shows its runs bold and its marks underlined</li>
 * </ul>
 * The one pseudo-class of its own, part of the same contract: {@code :no-match} on {@code form-search-count}, while
 * nothing matches. Besides, the shell's {@code :readonly} and {@code :disabled} apply.
 *
 * <p>Like every karthu-fx control, the field is used on the JavaFX Application Thread only.
 */
public class SearchInput extends FieldFrame {

    private static final PseudoClass NO_MATCH_PSEUDO_CLASS = PseudoClass.getPseudoClass("no-match");

    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "Search");
    private final StringProperty clearText = new SimpleStringProperty(this, "clearText", "Clear");
    private final StringProperty matchCountFormat =
            new SimpleStringProperty(this, "matchCountFormat", "{0} of {1}");

    /**
     * The search the text holds now, parsed once per change of the text: the connected list's predicate and every
     * highlight node follow it.
     */
    private final ObjectProperty<SearchQuery> query = new SimpleObjectProperty<>(this, "query", SearchQuery.of(""));
    /** The connection to the list the field filters, or null while it filters none. */
    private ListFilter<?> listFilter;
    /**
     * Keeps the counter in step with the connected list and its source. Held here: the lists hold it weakly, so that
     * a long-lived list of the application never keeps the field alive.
     */
    private final InvalidationListener listChanged = observable -> updateOwnControls();
    /** The one weak listener the connected list and its source hold, added and removed as one. */
    private final WeakInvalidationListener weakListChanged = new WeakInvalidationListener(listChanged);

    /** The magnifier, the frame's leading graphic: an icon in a region, whose padding gives the room around it. */
    private final StackPane magnifier = new StackPane(FontIcon.of(BootstrapIcons.SEARCH));
    /** The counter, an own control before the clear button: the matches and all entries of the connected list. */
    private final Label counter = new Label();
    /** The clear button, the last of the field's own controls, at the frame's right end. */
    private final Button clear = new Button();

    /**
     * Creates an empty search field with the placeholder <i>Search</i>.
     */
    public SearchInput() {
        getStyleClass().add("form-search-input");
        magnifier.getStyleClass().add("form-search-icon");
        setLeadingGraphic(magnifier);
        getTextField().promptTextProperty().bind(Bindings.createStringBinding(this::shownPromptText, promptText));

        clear.getStyleClass().add("form-search-clear");
        styleAsIcon(clear);
        clear.setGraphic(FontIcon.of(BootstrapIcons.X));
        clear.setOnAction(event -> clearByUser());
        Tooltip clearTooltip = new Tooltip();
        clearTooltip.textProperty().bind(clearText);
        clear.setTooltip(clearTooltip);
        clear.accessibleTextProperty().bind(clearText);
        counter.getStyleClass().add("form-search-count");
        // The own controls, left to right: whatever shows the search's state comes before the x, which stays last.
        getOwnControls().addAll(counter, clear);

        textProperty().addListener((observable, oldText, newText) -> textChanged());
        matchCountFormat.addListener((observable, oldFormat, newFormat) -> updateOwnControls());
        // Disabled, also through a parent: the x hides; read-only reaches it through updateContentEditable.
        disabledProperty().addListener((observable, wasDisabled, isDisabled) -> updateOwnControls());
        // A filter, ahead of the text field's own Escape, which hands the key on to the parent unconsumed.
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleEscape);
        updateOwnControls();
    }

    // --- Text ----------------------------------------------------------------------------------------

    /**
     * The text in the field: the text field's own text property, exactly as typed. The application may set it, also
     * while the field is read-only or disabled.
     *
     * @return the text property
     */
    public final StringProperty textProperty() {
        return getTextField().textProperty();
    }

    /**
     * Returns the text in the field.
     *
     * @return the text, empty when there is none
     */
    public final String getText() {
        return getTextField().getText();
    }

    /**
     * Sets the text in the field, as it is.
     *
     * @param text the new text
     */
    public final void setText(String text) {
        getTextField().setText(text);
    }

    /**
     * The placeholder the empty field shows (default <i>Search</i>), a property so that an application can translate
     * it. Null counts as empty.
     *
     * @return the prompt text property
     */
    public final StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Returns the placeholder.
     *
     * @return the prompt text, empty or null when there is none
     */
    public final String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the placeholder.
     *
     * @param text the prompt text; null or empty for none
     */
    public final void setPromptText(String text) {
        promptText.set(text);
    }

    /** The prompt text the text field shows: the property's, null as empty. */
    private String shownPromptText() {
        String text = promptText.get();
        return text == null ? "" : text;
    }

    /**
     * The text changed, whoever changed it: the search is parsed once, the connected list filtered by it, the own
     * controls updated. Package-private: a field with a popup of suggestions follows the text here too, after the
     * search, so that it reads the new one.
     */
    void textChanged() {
        query.set(SearchQuery.of(getText()));
        if (listFilter != null) {
            listFilter.apply(query.get());
        }
        updateOwnControls();
    }

    /**
     * Returns the search the text holds now, parsed once per change of the text. Package-private: a field with a popup
     * of suggestions matches its entries against it.
     *
     * @return the current search
     */
    final SearchQuery query() {
        return query.get();
    }

    // --- List filter ---------------------------------------------------------------------------------

    /**
     * Connects the field to one list, which it filters from now on as the user types: on every change of the text the
     * field sets the list's predicate from the matching rule (see the class description), keeping the entries whose
     * text matches, and keeps the counter. Connected, the list is filtered by the current text at once.
     *
     * <p>The field filters one list at a time: a second call replaces the first connection, and the list connected
     * before gets back every entry (its predicate set to null, which matches every entry) and no longer follows the
     * text. A call with null disconnects in the same way. The field owns the list's predicate while connected: a
     * predicate the application sets is replaced at the next change of the text.
     *
     * @param list the list to filter, built over the application's list; null disconnects the list connected before
     * @param text the text of an entry, the one its row shows, which the search is matched against (a null result
     *             counts as the empty text); ignored when {@code list} is null
     * @param <T>  the type of the entries
     * @throws IllegalArgumentException when {@code list} is given without {@code text}
     */
    public final <T> void filter(FilteredList<T> list, Function<? super T, String> text) {
        if (list != null && text == null) {
            throw new IllegalArgumentException("the text of an entry must not be null");
        }
        if (listFilter != null) {
            listFilter.unwatch(weakListChanged);
            if (listFilter.list() != list) {
                listFilter.release();
            }
        }
        listFilter = list == null ? null : new ListFilter<>(list, text);
        if (listFilter != null) {
            listFilter.apply(query.get());
            listFilter.watch(weakListChanged);
        }
        updateOwnControls();
    }

    /**
     * The format of the counter (default <i>{0} of {1}</i>): {@code {0}} stands for the number of matches, the
     * connected list's filtered size, {@code {1}} for the number of all entries, its source's size, each wherever and
     * as often as it occurs; everything else is shown as it is. A property so that an application can translate it.
     * Null counts as empty.
     *
     * @return the match count format property
     */
    public final StringProperty matchCountFormatProperty() {
        return matchCountFormat;
    }

    /**
     * Returns the format of the counter.
     *
     * @return the match count format, empty or null when the counter shows no text
     */
    public final String getMatchCountFormat() {
        return matchCountFormat.get();
    }

    /**
     * Sets the format of the counter: {@code {0}} the number of matches, {@code {1}} the number of all entries.
     *
     * @param format the match count format; null or empty for no text
     */
    public final void setMatchCountFormat(String format) {
        matchCountFormat.set(format);
    }

    /**
     * Fills a count format: {@code {0}} becomes the number of matches, {@code {1}} the number of all entries, each
     * wherever it occurs, written as plain digits; everything else stays as it is, so a format never fails, whatever
     * an application or a translation puts into it. Package-private: the suggestion input's footer counts alike.
     *
     * @param format  the format; null counts as empty
     * @param matches the number of matches
     * @param all     the number of all entries
     * @return the text
     */
    static String countText(String format, int matches, int all) {
        if (format == null) {
            return "";
        }
        return format.replace("{0}", Integer.toString(matches)).replace("{1}", Integer.toString(all));
    }

    // --- Marks in the application's cells -------------------------------------------------------------

    /**
     * Returns a node that shows a text with the current search's marks, for the application's own cells: a
     * {@link TextFlow} with the style class {@code search-highlight}, whose runs show the text in order, every
     * occurrence of every word of the search (case and accents folded, overlapping and touching ones merged) in runs of
     * their own. Every run is a {@link Text} with the style class {@code search-text}, which the theme shows in the
     * text colour; a marked run also carries {@code search-match}, which the theme shows bold in the accent colour
     * (inside a suggestion popup in the popup's own look). An empty search marks nothing; an empty text is one empty
     * run, so the node keeps a line's height.
     *
     * <p>A text the application shows bold, such as the name on a card, is a highlight node to which the application
     * adds the style class {@code search-strong}: the theme shows its runs bold and its marks underlined, as a bold mark
     * would not show in a bold text.
     *
     * <p>The node follows the search text by itself: it shows the marks of the new search on every change of the text,
     * so a cell never shows an old search. It does so without the field keeping it alive: the field holds it only
     * weakly, so a cell that drops it, for a new one on its next update, leaks nothing. Create one per text and cell;
     * a node shows the text it was created with.
     *
     * @param text the text, such as the one a row shows; null counts as the empty text
     * @return a new highlight node, following this field's search
     */
    public final TextFlow highlight(String text) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("search-highlight");
        Highlight highlight = new Highlight(flow, text == null ? "" : text);
        // The node holds its highlight, and with it the listener, strongly; the field's search holds the listener
        // weakly, so the node lives as long as the application holds it, never longer.
        flow.getProperties().put(Highlight.class, highlight);
        query.addListener(new WeakChangeListener<>(highlight.queryChanged));
        highlight.show(query.get());
        return flow;
    }

    /**
     * What a highlight node shows and how it follows the search: the text, the marks it shows, and the listener on the
     * field's search, held by the node.
     */
    private static final class Highlight {

        /** The node the runs go into. */
        private final TextFlow flow;
        /** The text the node shows. */
        private final String text;
        /** The marks the runs show now; null before the first runs. */
        private List<Mark> shown;
        /** Shows the marks of each new search; the field's search holds it weakly. */
        private final ChangeListener<SearchQuery> queryChanged = (observable, oldSearch, newSearch) -> show(newSearch);

        Highlight(TextFlow flow, String text) {
            this.flow = flow;
            this.text = text;
        }

        /** Shows the text with the search's marks, unless the runs already show those marks. */
        void show(SearchQuery search) {
            List<Mark> marks = search.marks(text);
            if (marks.equals(shown)) {
                return;
            }
            shown = marks;
            List<Text> runs = new ArrayList<>();
            int at = 0;
            for (Mark mark : marks) {
                if (mark.start() > at) {
                    runs.add(run(text.substring(at, mark.start()), false));
                }
                runs.add(run(text.substring(mark.start(), mark.end()), true));
                at = mark.end();
            }
            if (at < text.length() || runs.isEmpty()) {
                runs.add(run(text.substring(at), false));
            }
            flow.getChildren().setAll(runs);
        }

        /** One run of the text: {@code search-text}, a marked one also {@code search-match}. */
        private static Text run(String part, boolean marked) {
            Text run = new Text(part);
            run.getStyleClass().add("search-text");
            if (marked) {
                run.getStyleClass().add("search-match");
            }
            return run;
        }
    }

    /**
     * The connection to the list the field filters: the list and the text of its entries.
     *
     * @param list the filtered list
     * @param text the text of an entry
     * @param <T>  the type of the entries
     */
    private record ListFilter<T>(FilteredList<T> list, Function<? super T, String> text) {

        /** Filters the list by the search: the entries whose text matches; every entry for the empty search. */
        void apply(SearchQuery search) {
            list.setPredicate(search.isEmpty() ? null : search.predicate(text));
        }

        /** Gives the list every entry back: the null predicate, which matches every entry. */
        void release() {
            list.setPredicate(null);
        }

        /**
         * Watches the list and its source: the source for a change the filtered list does not show (an entry added
         * that does not match), the list for one it shows, which it takes over from its source in a listener of its own,
         * perhaps after this one.
         */
        void watch(InvalidationListener listener) {
            list.addListener(listener);
            list.getSource().addListener(listener);
        }

        /** Stops watching the list and its source. */
        void unwatch(InvalidationListener listener) {
            list.removeListener(listener);
            list.getSource().removeListener(listener);
        }

        /** The number of matches: the entries the list shows. */
        int matches() {
            return list.size();
        }

        /** The number of all entries: the source's. */
        int all() {
            return list.getSource().size();
        }
    }

    // --- Clearing ------------------------------------------------------------------------------------

    /**
     * The clear button's tooltip and accessible text (default <i>Clear</i>), a property so that an application can
     * translate it.
     *
     * @return the clear text property
     */
    public final StringProperty clearTextProperty() {
        return clearText;
    }

    /**
     * Returns the clear button's tooltip and accessible text.
     *
     * @return the clear text
     */
    public final String getClearText() {
        return clearText.get();
    }

    /**
     * Sets the clear button's tooltip and accessible text.
     *
     * @param text the clear text
     */
    public final void setClearText(String text) {
        clearText.set(text);
    }

    /**
     * The x: closes a popup, empties the field and gives the text field the focus, or leaves it there. Only while the
     * user may change the text (the x is hidden otherwise; this guards a fire by code too).
     */
    private void clearByUser() {
        if (!userMayChangeValue()) {
            return;
        }
        hidePopup();
        getTextField().clear();
        getTextField().requestFocus();
    }

    /** Escape without a modifier: consumed when it did something, else it passes on, as to a dialog around the field. */
    private void handleEscape(KeyEvent event) {
        if (event.getCode() != KeyCode.ESCAPE || event.isAltDown() || event.isControlDown() || event.isMetaDown()
                || event.isShiftDown()) {
            return;
        }
        if (escape()) {
            event.consume();
        }
    }

    /**
     * What Escape does in the field, one step per press: a popup that shows closes and nothing else happens; else a
     * field with text the user may change becomes empty. Package-private: the step a field with a popup builds on.
     *
     * @return true when Escape did something, so the key is consumed; false otherwise, so it passes on
     */
    boolean escape() {
        if (hidePopup()) {
            return true;
        }
        if (!hasText() || !userMayChangeValue()) {
            return false;
        }
        getTextField().clear();
        return true;
    }

    /**
     * Closes the field's popup, if one shows. A search field has none; a field with a popup of suggestions overrides it.
     * Package-private: Escape and the clear button close a popup first.
     *
     * @return true when a popup showed and closed
     */
    boolean hidePopup() {
        return false;
    }

    // --- Own controls --------------------------------------------------------------------------------

    /**
     * Makes the text field editable or not, as a frame does, and shows the clear button only while the user may edit.
     *
     * @param editable true while the user may edit the text: neither bound nor read-only
     */
    @Override
    protected void updateContentEditable(boolean editable) {
        super.updateContentEditable(editable);
        // The frame's constructor sets its content, and so calls this hook, before this field's clear button exists;
        // the constructor shows the clear button then.
        if (clear != null) {
            updateOwnControls();
        }
    }

    /**
     * Shows the own controls as the text, the connected list and the state ask: the counter while a list is connected
     * and there is text, with the list's counts and {@code :no-match} at none; the clear button while there is text
     * and the field is neither disabled nor read-only.
     */
    private void updateOwnControls() {
        boolean counting = listFilter != null && hasText();
        if (counting) {
            int matches = listFilter.matches();
            counter.setText(countText(matchCountFormat.get(), matches, listFilter.all()));
            counter.pseudoClassStateChanged(NO_MATCH_PSEUDO_CLASS, matches == 0);
        } else {
            counter.pseudoClassStateChanged(NO_MATCH_PSEUDO_CLASS, false);
        }
        updateOwnControl(counter, counting);
        updateOwnControl(clear, hasText() && !isDisabled() && !isReadOnly());
    }

    /** Whether the field holds text: the empty text and null are none. */
    private boolean hasText() {
        String text = getText();
        return text != null && !text.isEmpty();
    }
}
