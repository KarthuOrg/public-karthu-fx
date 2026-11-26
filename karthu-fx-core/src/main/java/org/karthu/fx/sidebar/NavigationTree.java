package org.karthu.fx.sidebar;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.form.search.SearchQuery;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A tree of the application's entries for the edge of a window: the ready-made content of a side bar's panel, or a
 * navigation on its own in any layout. It shows the application's own tree of JavaFX {@link TreeItem}s under a hidden
 * root, each entry with its icon and its text, under a search field that narrows it, and tells the application when
 * the user opens an entry, handing over the entry's value.
 *
 * <p><b>The application's tree.</b> The {@linkplain #rootProperty() root} is the application's, and so are its items
 * and values: the tree shows them as they are and follows every change of them, an item added or removed, a value
 * replaced, an item folded or unfolded by the application. The root itself is never shown, so the tree unfolds it
 * when it is set, since a folded hidden root would show nothing. An entry's text comes from the
 * {@linkplain #textFunctionProperty() text function} (by default the value's string form), its icon from the
 * {@linkplain #iconFunctionProperty() icon function}, an Ikonli icon or none.
 *
 * <p><b>Search.</b> A {@linkplain #getSearchInput() search field} lies above the tree over its full width. An entry is
 * a <i>hit</i> when its {@linkplain #searchTextFunctionProperty() search text} (by default the shown text) matches
 * the search by karthu-fx's search rule: the search's words each occur as a contiguous part, case and accents ignored
 * (see {@link SearchInput}). While the field holds a search, text other than whitespace only, the tree view shows a
 * copy of the application's tree instead of the tree itself: every hit, every folder above a hit, and for a hit with
 * children all its entries, so a folder whose own name matches keeps its whole content. Every folder of the copy is
 * unfolded, and every item of it carries the application's value, so the cells, the
 * {@linkplain #selectedValueProperty() selected value} and the open handler receive the application's values during a
 * search as at any other time. A change of the application's tree during the search (an item added or removed, a value
 * replaced) shows at once.
 * <ul>
 *   <li>The default cell marks the characters of the shown text that match, through the field's
 *       {@linkplain SearchInput#highlight(String) highlight}; cells of the application's own use the same helper:
 *       {@code tree.getSearchInput().highlight(text)}.</li>
 *   <li>During a search the field's counter shows the hits out of all entries, folded ones included and the hidden
 *       root not counted: <i>3 of 43</i>. While the search finds no entry, the tree area shows
 *       {@linkplain #noMatchesTextProperty() <i>No matches</i>}; while the application's tree has no entry, its
 *       placeholder instead.</li>
 *   <li>The selected entry stays selected through a change of the search while the copy shows it, as a hit, a folder
 *       above one or an entry of a matching folder; otherwise the selection is cleared and does not come back. Neither
 *       opens anything.</li>
 *   <li>Emptying the field shows the application's tree again, folded exactly as the user left it, since the search
 *       only ever folds and unfolds its copy: only the folders above the selected entry unfold, so that it shows,
 *       selected and scrolled to.</li>
 *   <li>A field of whitespace only holds no search, as by karthu-fx's search rule it matches every entry and marks
 *       nothing: the tree shows the application's tree as in an empty field, and the field shows no counter.</li>
 * </ul>
 *
 * <p><b>Keys of the search</b>, the tree's own, without a command registry: Ctrl+F (Cmd+F on macOS) while the focus
 * is inside the tree puts it into the search field and selects its text, as {@link #focusSearch()} does. In the field,
 * during a search, Arrow down and Enter take the first hit without children in the order of the rows, past a folder
 * whose own name matches too, and the first hit only when every hit has children. Arrow down moves the focus to the
 * tree and selects that hit, a change of the selection by the user, which opens it in {@link OpenMode#SELECTION};
 * Enter selects it and opens it in either mode, the focus staying in the field. Without a hit both pass on. Without a
 * search, in an empty field or one of whitespace only, Arrow down moves the focus to the tree, on its selected entry,
 * and Enter passes on. Escape empties the field, the field's own key.
 *
 * <p><b>Opening.</b> The {@linkplain #openModeProperty() open mode} decides how the user opens an entry, the
 * {@linkplain #onOpenProperty() open handler} receives the entry's value, the application's own, never a copy:
 * <ul>
 *   <li>{@link OpenMode#DOUBLE_CLICK} (the default): a double click or Enter opens the entry. On an entry with
 *       children it also folds or unfolds it, once per double click. A single click only selects.</li>
 *   <li>{@link OpenMode#SELECTION}: a change of the selection by the user opens the entry, by a click (also a right
 *       click, which selects as in JavaFX's tree view) or by a key that moves the selection. A single click on an
 *       entry with children also folds or unfolds it, once per click, and so does Enter, which selects the entry the
 *       keyboard focus is on, as a click would. A selection the application makes, through {@link #select(Object)}
 *       or the inner tree view's selection model, and a selection that moves because the application changed its
 *       tree, open nothing.</li>
 * </ul>
 * In either mode the disclosure arrow folds and unfolds an entry and opens nothing. Folders open too: the application
 * ignores an open request it has no use for. While the open handler runs, a change of the selection it causes opens
 * nothing more.
 *
 * <p><b>Selection.</b> {@link #selectedValueProperty()} is the value of the selected entry, read-only, and follows a
 * value the application replaces in the selected item. {@link #select(Object)} selects the entry that holds a value,
 * unfolds the folders above it and scrolls it into view.
 *
 * <p><b>Expand All and Collapse All.</b> {@link #expandAll()} unfolds every entry with children, {@link #collapseAll()}
 * folds every entry below the hidden root. Both act on the tree the tree view shows, during a search the copy, and both
 * are offered as buttons: as a {@link SideBarContent}, the tree's {@linkplain #getHeaderButtons() header buttons} are
 * Expand All and Collapse All, which a side bar puts into its panel's header and an application that uses the tree
 * alone places itself. Their tooltips are {@linkplain #expandAllTextProperty() properties}. The tree has no menu items,
 * opening its panel puts the focus into the tree, on the selected entry, and its search is focused as by Ctrl+F.
 *
 * <p><b>Placeholder.</b> While the application's tree has no entry, or there is no root, the tree shows the
 * application's {@linkplain #placeholderProperty() placeholder} at its top, such as a label that says what would
 * appear there, also during a search.
 *
 * <p><b>The inner tree view</b> is {@linkplain #getTreeView() exposed}, so an application can give it a context menu,
 * its own cells or anything else a tree view offers. Opening and folding work on the tree view, not in its cells, so
 * they stay with cells of the application's own. The tree's own keys (the arrow keys, Home, End, Page Up and Page
 * Down) stay as JavaFX provides them.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change. The theme draws the tree without
 * border and without a background of its own, so it lies on the panel around it, and its rows flat: a row under the
 * pointer in the hover colour, the selected row in the selection colour with a 2 px accent bar on its left, its icon
 * and text in the accent colour, the text bold. A mark of the search shows bold in the accent colour,
 * on the selected row underlined.
 * <ul>
 *   <li>{@code navigation-tree}: the tree, a {@link VBox}</li>
 *   <li>{@code navigation-tree-search-bar}: the holder of the search field, a {@link StackPane}, the tree's first
 *       child, which gives the field its room</li>
 *   <li>{@code navigation-tree-search}: the search field, a {@link SearchInput} with its own style classes</li>
 *   <li>{@code navigation-tree-area}: the area of the tree view, the placeholder and <i>No matches</i>, a
 *       {@link StackPane}</li>
 *   <li>{@code navigation-tree-entry}: what the default cell shows, an {@link HBox} of the icon and the text</li>
 *   <li>{@code navigation-tree-icon}: an entry's icon, a {@link FontIcon}, 14 px in the secondary text colour</li>
 *   <li>{@code navigation-tree-text}: an entry's text, a highlight node of the search field, a {@link TextFlow} beside
 *       {@code search-highlight}, whose runs carry {@code search-text} and, marked, {@code search-match}</li>
 *   <li>{@code navigation-tree-placeholder}: the holder of the placeholder, a {@link StackPane} at the top of the
 *       tree area, its label in the secondary text colour</li>
 *   <li>{@code navigation-tree-no-matches}: <i>No matches</i>, a {@link Label} at the top of the tree area, in the
 *       secondary text colour</li>
 *   <li>{@code navigation-tree-expand-all}, {@code navigation-tree-collapse-all}: the header buttons, {@link Button}s
 *       beside {@code icon-button}, with the Bootstrap icons {@code ARROWS_EXPAND} and {@code ARROWS_COLLAPSE}, 14 px
 *       in the secondary text colour</li>
 * </ul>
 *
 * <p>Like every karthu-fx control, the tree is used on the JavaFX Application Thread only.
 *
 * @param <T> the type of the values of the application's tree items
 * @since 1.1.0-RC2
 */
public class NavigationTree<T> extends VBox implements SideBarContent {

    /** Ctrl+F, Cmd+F on macOS: the focus into the search field. */
    private static final KeyCombination FIND = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
    /** Enter without a modifier: a combination without modifiers matches only while none is pressed. */
    private static final KeyCombination ENTER = new KeyCodeCombination(KeyCode.ENTER);
    /** Arrow down without a modifier. */
    private static final KeyCombination ARROW_DOWN = new KeyCodeCombination(KeyCode.DOWN);

    private final ObjectProperty<TreeItem<T>> root = new SimpleObjectProperty<>(this, "root");
    private final ObjectProperty<Function<? super T, String>> textFunction =
            new SimpleObjectProperty<>(this, "textFunction");
    private final ObjectProperty<Function<? super T, ? extends Ikon>> iconFunction =
            new SimpleObjectProperty<>(this, "iconFunction");
    private final ObjectProperty<OpenMode> openMode =
            new SimpleObjectProperty<>(this, "openMode", OpenMode.DOUBLE_CLICK);
    private final ObjectProperty<Consumer<? super T>> onOpen = new SimpleObjectProperty<>(this, "onOpen");
    private final ReadOnlyObjectWrapper<T> selectedValue = new ReadOnlyObjectWrapper<>(this, "selectedValue");
    private final ObjectProperty<Node> placeholder = new SimpleObjectProperty<>(this, "placeholder");
    private final StringProperty expandAllText = new SimpleStringProperty(this, "expandAllText", "Expand All");
    private final StringProperty collapseAllText = new SimpleStringProperty(this, "collapseAllText", "Collapse All");
    private final ObjectProperty<Function<? super T, String>> searchTextFunction =
            new SimpleObjectProperty<>(this, "searchTextFunction");
    private final StringProperty noMatchesText = new SimpleStringProperty(this, "noMatchesText", "No matches");

    private final SearchInput searchInput = new SearchInput();
    /** Holds the search field above the tree area, the tree's first child; the theme gives it its padding. */
    private final StackPane searchBar = new StackPane(searchInput);
    private final TreeView<T> treeView = new TreeView<>();
    /** Holds the placeholder over the tree view while the application's tree is empty. */
    private final StackPane placeholderHolder = new StackPane();
    /** Lies over the tree view while a search finds nothing. */
    private final Label noMatches = new Label();
    /** The tree view, the placeholder and "No matches", under the search field. */
    private final StackPane treeArea = new StackPane(treeView, placeholderHolder, noMatches);
    private final Button expandAllButton = HeaderButtons.iconButton("navigation-tree-expand-all",
            BootstrapIcons.ARROWS_EXPAND, expandAllText, expandAllText, this::expandAll);
    private final Button collapseAllButton = HeaderButtons.iconButton("navigation-tree-collapse-all",
            BootstrapIcons.ARROWS_COLLAPSE, collapseAllText, collapseAllText, this::collapseAll);

    // Whose a change of the selection is decides whether it opens the entry in selection mode: the user's opens it,
    // the tree's own and one the open handler causes do not. The phases mark the time of each.
    /**
     * The user's input events, handled by the tree view or by the search field's keys: a change of the selection
     * meanwhile is the user's.
     */
    private final Phase userInput = new Phase();
    /** The tree's own changes of the selection, which open nothing. */
    private final Phase quietSelection = new Phase();
    /** The open handler's run: a change of the selection it causes opens nothing more. */
    private final Phase openHandlerRun = new Phase();
    /** Whether the release of the primary button belongs to a press the tree handled itself. */
    private boolean releaseTaken;

    /** The selected item, whose value the selected value follows. */
    private TreeItem<T> watchedItem;
    private final InvalidationListener selectedItemValueChanged =
            observable -> selectedValue.set(watchedItem == null ? null : watchedItem.getValue());
    /** Held weakly by the application's item, so that an item outliving the tree never keeps it alive. */
    private final WeakInvalidationListener weakSelectedItemValueChanged =
            new WeakInvalidationListener(selectedItemValueChanged);
    private final InvalidationListener rootChildrenChanged = observable -> updatePlaceholder();
    /** Held weakly by the application's root, so that a root outliving the tree never keeps it alive. */
    private final WeakInvalidationListener weakRootChildrenChanged = new WeakInvalidationListener(rootChildrenChanged);
    private final ChangeListener<TreeItem<T>> selectionChanged =
            (observable, oldItem, newItem) -> selectionChanged(newItem);

    /**
     * Every entry of the application's tree below its root, in the order of the rows, folded or not, while a search
     * is under way; empty otherwise. The search field counts it as all entries.
     */
    private final ObservableList<TreeItem<T>> entries = FXCollections.observableArrayList();
    /**
     * The hits: the entries whose search text matches. The search field owns the predicate, so the hits follow
     * karthu-fx's search rule and the field counts them, "3 of 43", as it counts any list it filters.
     */
    private final FilteredList<TreeItem<T>> hits = new FilteredList<>(entries);
    /**
     * Whether the search field holds a search, text other than whitespace only, so that the tree view shows the copy
     * of the hits and the field filters and counts the entries.
     */
    private boolean searching;
    /** The refresh of the entries: the hits follow, and the copy is built once afterwards. */
    private final Phase entriesRefresh = new Phase();
    /** A switch of the tree view's root: the selected value follows once the new root shows. */
    private final Phase rootSwitch = new Phase();
    /** The copy the tree view shows during a search, null otherwise. */
    private SearchCopy<T> copy;
    private final EventHandler<TreeItem.TreeModificationEvent<T>> applicationTreeChanged =
            event -> searchAgain();
    /** Held weakly by the application's root, so that a root outliving the tree never keeps it alive. */
    private final WeakEventHandler<TreeItem.TreeModificationEvent<T>> weakApplicationTreeChanged =
            new WeakEventHandler<>(applicationTreeChanged);

    /**
     * Creates a navigation tree without a root: it shows no entry, and the placeholder once there is one.
     *
     * @since 1.1.0-RC2
     */
    public NavigationTree() {
        this(null);
    }

    /**
     * Creates a navigation tree over the application's tree, which opens an entry on a double click or Enter.
     *
     * @param root the root of the application's tree, which is not shown and is unfolded; null for none
     * @since 1.1.0-RC2
     */
    public NavigationTree(TreeItem<T> root) {
        getStyleClass().add("navigation-tree");
        treeView.setShowRoot(false);
        treeView.setCellFactory(view -> new EntryCell());
        treeArea.getStyleClass().add("navigation-tree-area");
        VBox.setVgrow(treeArea, Priority.ALWAYS);
        placeholderHolder.getStyleClass().add("navigation-tree-placeholder");
        placeholderHolder.setAlignment(Pos.TOP_LEFT);
        // Over the tree view, but clicks beside the placeholder's own nodes reach the tree view under it.
        placeholderHolder.setPickOnBounds(false);
        noMatches.getStyleClass().add("navigation-tree-no-matches");
        noMatches.textProperty().bind(noMatchesText);
        noMatches.setMouseTransparent(true);
        noMatches.setVisible(false);
        StackPane.setAlignment(noMatches, Pos.TOP_LEFT);
        searchBar.getStyleClass().add("navigation-tree-search-bar");
        searchInput.getStyleClass().add("navigation-tree-search");
        searchInput.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(searchBar, treeArea);

        // During a search the field filters the flat list of the entries: its predicate decides the hits by
        // karthu-fx's search rule, and its counter shows them out of all entries. The copy follows the hits, and the
        // text decides whether the copy or the application's tree shows; either listener may hear a change first, both
        // end in the same state. Without a search the field filters nothing, so it shows no counter.
        hits.addListener((InvalidationListener) observable -> hitsChanged());
        searchInput.textProperty().addListener((observable, oldText, newText) -> searchTextChanged());
        // The navigation's own keys, without a command registry: Ctrl+F wherever the focus is inside it, a handler, so
        // that a node inside that takes the key keeps it; Arrow down and Enter in the field, a filter ahead of the text
        // field's own keys. Escape is the field's own: it empties it, which ends the search.
        addEventHandler(KeyEvent.KEY_PRESSED, this::findPressed);
        searchInput.addEventFilter(KeyEvent.KEY_PRESSED, this::searchKeyPressed);

        // Filters on the tree view, not handlers in the cells: opening and folding stay with an application's own
        // cells, and the cells' own behaviour never sees a press the tree handled itself.
        treeView.addEventFilter(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        treeView.addEventFilter(MouseEvent.MOUSE_RELEASED, this::mouseReleased);
        treeView.addEventFilter(KeyEvent.KEY_PRESSED, this::keyPressed);
        // "A change of the selection by the user" is one that happens while the tree view dispatches an input event:
        // the tree view's own dispatcher, wrapped, marks the time, through public API alone. A selection made by the
        // application, or moved because it changed its tree, happens outside and opens nothing.
        EventDispatcher treeDispatcher = treeView.getEventDispatcher();
        treeView.setEventDispatcher((event, tail) -> dispatch(treeDispatcher, event, tail));
        treeView.getSelectionModel().selectedItemProperty().addListener(selectionChanged);
        treeView.selectionModelProperty().addListener((observable, oldModel, newModel) -> {
            if (oldModel != null) {
                oldModel.selectedItemProperty().removeListener(selectionChanged);
            }
            if (newModel != null) {
                newModel.selectedItemProperty().addListener(selectionChanged);
            }
            selectionChanged(newModel == null ? null : newModel.getSelectedItem());
        });

        this.root.addListener((observable, oldRoot, newRoot) -> rootChanged(oldRoot, newRoot));
        textFunction.addListener(observable -> {
            treeView.refresh();
            // The shown text is the default search text.
            searchAgain();
        });
        iconFunction.addListener(observable -> treeView.refresh());
        searchTextFunction.addListener(observable -> searchAgain());
        placeholder.addListener(observable -> updatePlaceholder());
        setRoot(root);
        updatePlaceholder();
    }

    // --- The application's tree ----------------------------------------------------------------------

    /**
     * The root of the application's tree. The root itself is not shown, its children are the tree's top entries; a
     * root that is set is unfolded, since a folded hidden root would show nothing.
     *
     * @return the root property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<TreeItem<T>> rootProperty() {
        return root;
    }

    /**
     * Returns the root of the application's tree.
     *
     * @return the root, null for none
     * @since 1.1.0-RC2
     */
    public final TreeItem<T> getRoot() {
        return root.get();
    }

    /**
     * Sets the root of the application's tree, which is unfolded. The selection is cleared. During a search the new
     * tree is searched at once.
     *
     * @param root the root, null for none
     * @since 1.1.0-RC2
     */
    public final void setRoot(TreeItem<T> root) {
        this.root.set(root);
    }

    /**
     * The text an entry shows, from its value. Null, the default, shows the value's string form, and the empty text
     * for a null value; a function's null result counts as the empty text. A change shows at once.
     *
     * @return the text function property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<Function<? super T, String>> textFunctionProperty() {
        return textFunction;
    }

    /**
     * Returns the text function.
     *
     * @return the text function, null for the value's string form
     * @since 1.1.0-RC2
     */
    public final Function<? super T, String> getTextFunction() {
        return textFunction.get();
    }

    /**
     * Sets the text an entry shows, from its value.
     *
     * @param function the text function, null for the value's string form
     * @since 1.1.0-RC2
     */
    public final void setTextFunction(Function<? super T, String> function) {
        textFunction.set(function);
    }

    /**
     * The icon an entry shows before its text, from its value: an Ikonli icon, such as one of the Bootstrap icons,
     * drawn and coloured by the theme. Null, the default, shows no icons; a function's null result shows none for
     * that entry. A change shows at once.
     *
     * @return the icon function property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<Function<? super T, ? extends Ikon>> iconFunctionProperty() {
        return iconFunction;
    }

    /**
     * Returns the icon function.
     *
     * @return the icon function, null for no icons
     * @since 1.1.0-RC2
     */
    public final Function<? super T, ? extends Ikon> getIconFunction() {
        return iconFunction.get();
    }

    /**
     * Sets the icon an entry shows, from its value.
     *
     * @param function the icon function, null for no icons
     * @since 1.1.0-RC2
     */
    public final void setIconFunction(Function<? super T, ? extends Ikon> function) {
        iconFunction.set(function);
    }

    // --- Opening -------------------------------------------------------------------------------------

    /**
     * How the user opens an entry (default {@link OpenMode#DOUBLE_CLICK}): by a double click or Enter, or by selecting
     * it. Null counts as {@link OpenMode#DOUBLE_CLICK}.
     *
     * @return the open mode property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<OpenMode> openModeProperty() {
        return openMode;
    }

    /**
     * Returns how the user opens an entry.
     *
     * @return the open mode
     * @since 1.1.0-RC2
     */
    public final OpenMode getOpenMode() {
        return openMode.get();
    }

    /**
     * Sets how the user opens an entry.
     *
     * @param mode the open mode; null counts as {@link OpenMode#DOUBLE_CLICK}
     * @since 1.1.0-RC2
     */
    public final void setOpenMode(OpenMode mode) {
        openMode.set(mode);
    }

    /**
     * What happens when the user opens an entry: the handler receives the entry's value, the application's own. It is
     * called for an entry with children too, which the application ignores when it has no use for it.
     *
     * @return the open handler property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<Consumer<? super T>> onOpenProperty() {
        return onOpen;
    }

    /**
     * Returns the open handler.
     *
     * @return the open handler, null for none
     * @since 1.1.0-RC2
     */
    public final Consumer<? super T> getOnOpen() {
        return onOpen.get();
    }

    /**
     * Sets what happens when the user opens an entry.
     *
     * @param handler the open handler, which receives the entry's value; null for none
     * @since 1.1.0-RC2
     */
    public final void setOnOpen(Consumer<? super T> handler) {
        onOpen.set(handler);
    }

    // --- Selection -----------------------------------------------------------------------------------

    /**
     * The value of the selected entry, the application's own, null without a selection. Read-only: the user selects,
     * and the application through {@link #select(Object)}. It follows a value the application replaces in the selected
     * item.
     *
     * @return the selected value property
     * @since 1.1.0-RC2
     */
    public final ReadOnlyObjectProperty<T> selectedValueProperty() {
        return selectedValue.getReadOnlyProperty();
    }

    /**
     * Returns the value of the selected entry.
     *
     * @return the selected value, null without a selection
     * @since 1.1.0-RC2
     */
    public final T getSelectedValue() {
        return selectedValue.get();
    }

    /**
     * Selects the entry that holds the value: the first one in the order of the rows whose value equals it, the hidden
     * root not counted. The folders above it unfold, the keyboard focus of the tree moves to it, and the tree scrolls
     * as far as needed to show it. When no entry holds the value, or the value is null, the selection is cleared. It
     * opens nothing, in either mode. During a search it looks among the entries the search shows.
     *
     * @param value the value; null clears the selection
     * @return true when an entry holds the value and is selected now, false when the selection was cleared
     * @since 1.1.0-RC2
     */
    public final boolean select(T value) {
        TreeItem<T> item = value == null ? null : findBelow(treeView.getRoot(), value);
        quietSelection.run(() -> revealAndSelect(item));
        return item != null;
    }

    // --- Search ---------------------------------------------------------------------------------------

    /**
     * Returns the search field above the tree, for what a search field offers beyond the tree's search: its text, its
     * placeholder, its counter's format, the highlight nodes for cells of the application's own
     * ({@link SearchInput#highlight(String)}). The tree connects it to its entries while it holds a search: an
     * application does not connect it to a list or a view of its own, which would end the tree's search.
     *
     * @return the search field
     * @since 1.1.0-RC2
     */
    public final SearchInput getSearchInput() {
        return searchInput;
    }

    /**
     * The text the search matches for an entry, from its value, which may hold more than the shown text, such as a
     * summary and keywords. Null, the default, matches the shown text (see {@link #textFunctionProperty()}); a
     * function's null result counts as the empty text. The marks of a hit show only in what the entry shows. A change
     * applies to a search under way at once.
     *
     * @return the search text function property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<Function<? super T, String>> searchTextFunctionProperty() {
        return searchTextFunction;
    }

    /**
     * Returns the search text function.
     *
     * @return the search text function, null for the shown text
     * @since 1.1.0-RC2
     */
    public final Function<? super T, String> getSearchTextFunction() {
        return searchTextFunction.get();
    }

    /**
     * Sets the text the search matches for an entry, from its value.
     *
     * @param function the search text function, null for the shown text
     * @since 1.1.0-RC2
     */
    public final void setSearchTextFunction(Function<? super T, String> function) {
        searchTextFunction.set(function);
    }

    /**
     * The text the tree area shows while a search finds no entry (default <i>No matches</i>), a property so that an
     * application can translate it.
     *
     * @return the no matches text property
     * @since 1.1.0-RC2
     */
    public final StringProperty noMatchesTextProperty() {
        return noMatchesText;
    }

    /**
     * Returns the text the tree area shows while a search finds no entry.
     *
     * @return the no matches text
     * @since 1.1.0-RC2
     */
    public final String getNoMatchesText() {
        return noMatchesText.get();
    }

    /**
     * Sets the text the tree area shows while a search finds no entry.
     *
     * @param text the no matches text
     * @since 1.1.0-RC2
     */
    public final void setNoMatchesText(String text) {
        noMatchesText.set(text);
    }

    // --- Expand All and Collapse All ----------------------------------------------------------------

    /**
     * Unfolds every entry with children, so that every entry shows. The Expand All button does the same. It opens
     * nothing.
     *
     * @since 1.1.0-RC2
     */
    public final void expandAll() {
        quietSelection.run(() -> {
            for (TreeItem<T> item : entriesBelow(treeView.getRoot())) {
                if (!item.isLeaf()) {
                    item.setExpanded(true);
                }
            }
        });
    }

    /**
     * Folds every entry below the hidden root, the folders inside folders too, so that only the top entries show and
     * unfolding one shows its children folded. The hidden root stays unfolded. A selected entry inside a folder hands
     * the selection to its top folder, as JavaFX's tree view does when a folder folds. The Collapse All button does the
     * same. It opens nothing.
     *
     * @since 1.1.0-RC2
     */
    public final void collapseAll() {
        quietSelection.run(() -> {
            for (TreeItem<T> item : entriesBelow(treeView.getRoot())) {
                item.setExpanded(false);
            }
        });
    }

    /**
     * The tooltip and accessible text of the Expand All button (default <i>Expand All</i>), a property so that an
     * application can translate it.
     *
     * @return the expand all text property
     * @since 1.1.0-RC2
     */
    public final StringProperty expandAllTextProperty() {
        return expandAllText;
    }

    /**
     * Returns the tooltip and accessible text of the Expand All button.
     *
     * @return the expand all text
     * @since 1.1.0-RC2
     */
    public final String getExpandAllText() {
        return expandAllText.get();
    }

    /**
     * Sets the tooltip and accessible text of the Expand All button.
     *
     * @param text the expand all text
     * @since 1.1.0-RC2
     */
    public final void setExpandAllText(String text) {
        expandAllText.set(text);
    }

    /**
     * The tooltip and accessible text of the Collapse All button (default <i>Collapse All</i>), a property so that an
     * application can translate it.
     *
     * @return the collapse all text property
     * @since 1.1.0-RC2
     */
    public final StringProperty collapseAllTextProperty() {
        return collapseAllText;
    }

    /**
     * Returns the tooltip and accessible text of the Collapse All button.
     *
     * @return the collapse all text
     * @since 1.1.0-RC2
     */
    public final String getCollapseAllText() {
        return collapseAllText.get();
    }

    /**
     * Sets the tooltip and accessible text of the Collapse All button.
     *
     * @param text the collapse all text
     * @since 1.1.0-RC2
     */
    public final void setCollapseAllText(String text) {
        collapseAllText.set(text);
    }

    // --- Side bar content ----------------------------------------------------------------------------

    /**
     * Returns the tree's header buttons: Expand All, then Collapse All, icon buttons whose tooltips are
     * {@link #expandAllTextProperty()} and {@link #collapseAllTextProperty()}. A side bar puts them into the header of
     * the tree's panel; an application that uses the tree alone may place them itself.
     *
     * @return Expand All and Collapse All, the same two buttons on every call
     * @since 1.1.0-RC2
     */
    @Override
    public List<Node> getHeaderButtons() {
        return List.of(expandAllButton, collapseAllButton);
    }

    /**
     * Puts the keyboard focus into the tree view, on the selected entry, and scrolls the entry into view.
     *
     * @since 1.1.0-RC2
     */
    @Override
    public void focusContent() {
        treeView.requestFocus();
        int row = treeView.getSelectionModel().getSelectedIndex();
        if (row >= 0) {
            treeView.getFocusModel().focus(row);
            showRow(row);
        }
    }

    /**
     * Puts the keyboard focus into the search field and selects its text, so that what the user types replaces the
     * search, as Ctrl+F inside the tree does.
     *
     * @return true, as the tree has a search
     * @since 1.1.0-RC2
     */
    @Override
    public boolean focusSearch() {
        TextField field = searchInput.getTextField();
        field.requestFocus();
        field.selectAll();
        return true;
    }

    // --- Placeholder ---------------------------------------------------------------------------------

    /**
     * The node the tree shows at its top while the application's tree has no entry, or there is no root, such as a
     * label that says what would appear there (a label directly in the placeholder takes the secondary text colour).
     * Null, the default, shows nothing.
     *
     * @return the placeholder property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<Node> placeholderProperty() {
        return placeholder;
    }

    /**
     * Returns the placeholder of an empty tree.
     *
     * @return the placeholder, null for none
     * @since 1.1.0-RC2
     */
    public final Node getPlaceholder() {
        return placeholder.get();
    }

    /**
     * Sets the placeholder of an empty tree.
     *
     * @param node the placeholder, null for none
     * @since 1.1.0-RC2
     */
    public final void setPlaceholder(Node node) {
        placeholder.set(node);
    }

    // --- Inner tree view -----------------------------------------------------------------------------

    /**
     * Returns the inner tree view, for what a tree view offers beyond the navigation tree: a context menu, cells of
     * the application's own, a selection read or made directly. Its root is managed by the navigation tree (set
     * {@link #rootProperty()} instead), and it never shows its root. During a search its root is the copy's, whose
     * items carry the application's values; a cell reads the value, not the item.
     *
     * @return the tree view
     * @since 1.1.0-RC2
     */
    public final TreeView<T> getTreeView() {
        return treeView;
    }

    // --- Inside --------------------------------------------------------------------------------------

    private void rootChanged(TreeItem<T> oldRoot, TreeItem<T> newRoot) {
        if (oldRoot != null) {
            oldRoot.getChildren().removeListener(weakRootChildrenChanged);
            oldRoot.removeEventHandler(TreeItem.childrenModificationEvent(), weakApplicationTreeChanged);
            oldRoot.removeEventHandler(TreeItem.valueChangedEvent(), weakApplicationTreeChanged);
        }
        if (newRoot != null) {
            newRoot.setExpanded(true);
            newRoot.getChildren().addListener(weakRootChildrenChanged);
            // A change anywhere in the application's tree reaches its root: JavaFX's tree items pass their events on
            // to their parents.
            newRoot.addEventHandler(TreeItem.childrenModificationEvent(), weakApplicationTreeChanged);
            newRoot.addEventHandler(TreeItem.valueChangedEvent(), weakApplicationTreeChanged);
        }
        if (searching) {
            searchAgain();
        } else {
            treeView.setRoot(newRoot);
        }
        updatePlaceholder();
    }

    /**
     * Shows the placeholder while the application's tree has no entry, and hides it otherwise; shows "No matches" while
     * a search shows no entry and the placeholder does not show.
     */
    private void updatePlaceholder() {
        TreeItem<T> applicationRoot = root.get();
        boolean empty = applicationRoot == null || applicationRoot.getChildren().isEmpty();
        Node node = placeholder.get();
        boolean placeholderShows = empty && node != null;
        if (placeholderShows) {
            placeholderHolder.getChildren().setAll(node);
            placeholderHolder.setVisible(true);
        } else {
            placeholderHolder.getChildren().clear();
            placeholderHolder.setVisible(false);
        }
        boolean copyEmpty = copy == null || copy.isEmpty();
        noMatches.setVisible(searching && copyEmpty && !placeholderShows);
    }

    // --- Search --------------------------------------------------------------------------------------

    /**
     * The search text changed: a search starts when the field gets a word, and ends when it holds none, empty or
     * whitespace only, which by karthu-fx's search rule is the empty search and matches every entry.
     */
    private void searchTextChanged() {
        boolean hasSearch = !SearchQuery.of(searchInput.getText()).isEmpty();
        if (hasSearch && !searching) {
            startSearch();
        } else if (!hasSearch && searching) {
            endSearch();
        }
    }

    /**
     * Starts a search: the field filters the entries, and the tree view shows the copy of the hits. The field is
     * connected before the search starts, while there are no entries yet, so that the hits are read once, with the
     * entries.
     */
    private void startSearch() {
        searchInput.filter(hits, item -> searchTextOf(item.getValue()));
        searching = true;
        searchAgain();
    }

    /** The hits changed, as the search field set a new predicate: the copy follows during a search. */
    private void hitsChanged() {
        if (searching && !entriesRefresh.isUnderWay()) {
            showCopy();
        }
    }

    /**
     * During a search, reads the entries of the application's tree again and builds the copy anew: when the search
     * starts, and when the tree changed somewhere (an item added or removed, a value replaced, a new root) or what the
     * search matches did. Without a search it does nothing.
     */
    private void searchAgain() {
        if (searching) {
            refreshEntries();
            showCopy();
        }
    }

    /** Reads every entry of the application's tree again; the hits follow, and the field counts them. */
    private void refreshEntries() {
        entriesRefresh.run(() -> entries.setAll(entriesBelow(root.get())));
    }

    /**
     * Shows a new copy of the application's tree for the hits (see {@link SearchCopy}). The selected entry stays
     * selected when the copy holds it.
     */
    private void showCopy() {
        TreeItem<T> selected = selectedOriginal();
        copy = new SearchCopy<>(root.get(), hits);
        showRoot(copy.root(), selected == null ? null : copy.copyOf(selected));
        updatePlaceholder();
    }

    /**
     * Ends the search: the tree view shows the application's tree again, folded as the user left it, except that the
     * folders above the selected entry unfold, so it shows, selected and scrolled to.
     */
    private void endSearch() {
        TreeItem<T> selected = selectedOriginal();
        searching = false;
        copy = null;
        // Without a search the field filters nothing, so that it shows no counter, and the entries are not kept, so
        // that they never hold an item the application removed.
        searchInput.filter(null, null);
        entries.clear();
        TreeItem<T> applicationRoot = root.get();
        if (selected != null && !isBelow(selected, applicationRoot)) {
            selected = null;
        }
        showRoot(applicationRoot, selected);
        updatePlaceholder();
    }

    /** The application's item of the selected entry: the selected item, or during a search its original. */
    private TreeItem<T> selectedOriginal() {
        TreeItem<T> shown = treeView.getSelectionModel().getSelectedItem();
        if (shown != null && copy != null && treeView.getRoot() == copy.root()) {
            return copy.originalOf(shown);
        }
        return shown;
    }

    /**
     * Lets the tree view show a root and selects an item of it, the folders above it unfolded, or nothing; nothing
     * opens, and the selected value changes once, to the value of the item selected at the end.
     */
    private void showRoot(TreeItem<T> newRoot, TreeItem<T> selected) {
        quietSelection.run(() -> rootSwitch.run(() -> {
            treeView.setRoot(newRoot);
            revealAndSelect(selected);
        }));
        selectedValue.set(watchedItem == null ? null : watchedItem.getValue());
    }

    /** Ctrl+F inside the tree: the focus into the search field, its text selected. */
    private void findPressed(KeyEvent event) {
        if (FIND.match(event)) {
            focusSearch();
            event.consume();
        }
    }

    /**
     * Arrow down and Enter in the search field, without a modifier. During a search, on the first hit (see
     * {@link #firstHit()}): Arrow down moves the focus to the tree and selects the hit, a change of the selection by
     * the user, which opens it in selection mode; Enter selects the hit and opens it, the focus staying in the field.
     * Without a hit both pass on. Without a search Arrow down moves the focus to the tree, on its selected entry, and
     * Enter passes on.
     */
    private void searchKeyPressed(KeyEvent event) {
        boolean arrowDown = ARROW_DOWN.match(event);
        if (!arrowDown && !ENTER.match(event)) {
            return;
        }
        if (!searching) {
            if (arrowDown) {
                focusContent();
                event.consume();
            }
            return;
        }
        TreeItem<T> firstHit = firstHit();
        if (firstHit == null) {
            return;
        }
        if (arrowDown) {
            treeView.requestFocus();
            userInput.run(() -> revealAndSelect(firstHit));
        } else {
            quietSelection.run(() -> revealAndSelect(firstHit));
            open(firstHit.getValue());
        }
        event.consume();
    }

    /**
     * The hit Arrow down and Enter in the search field take, as an item of the copy: the first hit without children
     * in the order of the rows, what the user most likely looks for, rather than a folder above it whose name happens
     * to match too; only when every hit has children, the first hit. The hits are in the order of the rows, as every
     * folder of the copy is unfolded. Null without a hit.
     */
    private TreeItem<T> firstHit() {
        if (hits.isEmpty()) {
            return null;
        }
        TreeItem<T> first = hits.getFirst();
        for (TreeItem<T> hit : hits) {
            if (hit.isLeaf()) {
                first = hit;
                break;
            }
        }
        return copy.copyOf(first);
    }

    /** Whether an item lies below a root, the root itself not counted. */
    private static boolean isBelow(TreeItem<?> item, TreeItem<?> root) {
        if (root == null) {
            return false;
        }
        for (TreeItem<?> parent = item.getParent(); parent != null; parent = parent.getParent()) {
            if (parent == root) {
                return true;
            }
        }
        return false;
    }

    /** The search text of an entry: the search text function's, or the shown text; never null. */
    private String searchTextOf(T value) {
        Function<? super T, String> function = searchTextFunction.get();
        if (function == null) {
            return textOf(value);
        }
        String text = function.apply(value);
        return text == null ? "" : text;
    }

    /** Dispatches an event through the tree view's own dispatcher, marking the time of an input event. */
    private Event dispatch(EventDispatcher treeDispatcher, Event event, EventDispatchChain tail) {
        if (!(event instanceof InputEvent)) {
            return treeDispatcher.dispatchEvent(event, tail);
        }
        return userInput.call(() -> treeDispatcher.dispatchEvent(event, tail));
    }

    /**
     * The selection changed: the selected value follows it, and in selection mode a change by the user opens the
     * entry, unless the tree changes the selection itself or the open handler runs.
     */
    private void selectionChanged(TreeItem<T> newItem) {
        if (watchedItem != null) {
            watchedItem.valueProperty().removeListener(weakSelectedItemValueChanged);
        }
        watchedItem = newItem;
        if (newItem != null) {
            newItem.valueProperty().addListener(weakSelectedItemValueChanged);
        }
        if (rootSwitch.isUnderWay()) {
            // The tree view switches between the application's tree and the copy, whose items carry the same values:
            // the selected value follows the selection at the end of the switch, without passing through null.
            return;
        }
        selectedValue.set(newItem == null ? null : newItem.getValue());
        if (newItem != null && userInput.isUnderWay() && !quietSelection.isUnderWay()
                && !openHandlerRun.isUnderWay() && openMode.get() == OpenMode.SELECTION) {
            open(newItem.getValue());
        }
    }

    /**
     * A press of the primary button on a row, away from its arrow. JavaFX's cells fold an entry with children on the
     * second click of a double click, and act on the release for a row that is already selected; a press the tree
     * handles itself is consumed with its release, so that the cells do nothing more and every gesture folds once.
     * <ul>
     *   <li>Selection mode, an entry with children: selected (which opens it) and folded or unfolded, on every
     *       click. An entry without children is the cells' own: their press selects it, which opens it.</li>
     *   <li>Double click mode, the second click: selected, folded or unfolded when it has children, opened. A single
     *       click is the cells' own: their press selects it.</li>
     * </ul>
     * The arrow is the cells' own in either mode: it folds and unfolds and selects nothing.
     */
    private void mousePressed(MouseEvent event) {
        releaseTaken = false;
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        TreeItem<T> item = rowItemUnder(event);
        if (item == null) {
            return;
        }
        if (openMode.get() == OpenMode.SELECTION) {
            if (!item.isLeaf()) {
                selectAsClicked(item);
                toggle(item);
                event.consume();
                releaseTaken = true;
            }
        } else if (event.getClickCount() == 2) {
            selectAsClicked(item);
            toggle(item);
            open(item.getValue());
            event.consume();
            releaseTaken = true;
        }
    }

    /** The release of a press the tree handled itself: consumed, the press did all there is to do. */
    private void mouseReleased(MouseEvent event) {
        if (releaseTaken && event.getButton() == MouseButton.PRIMARY) {
            releaseTaken = false;
            event.consume();
        }
    }

    /**
     * Enter without a modifier, on the entry the keyboard focus is on: selected and folded or unfolded when it has
     * children, as a click of the mode would; in double click mode also opened, in selection mode opened through the
     * change of the selection.
     */
    private void keyPressed(KeyEvent event) {
        if (!ENTER.match(event)) {
            return;
        }
        TreeItem<T> item = treeView.getFocusModel().getFocusedItem();
        if (item == null) {
            return;
        }
        selectAsClicked(item);
        toggle(item);
        if (openMode.get() != OpenMode.SELECTION) {
            open(item.getValue());
        }
        event.consume();
    }

    /**
     * The entry of the row under a mouse event, null when the event is on no filled row or on the row's disclosure
     * arrow. The row is found from the event's target, whatever cells the tree view has.
     */
    private TreeItem<T> rowItemUnder(MouseEvent event) {
        TreeCell<?> cell = null;
        Node at = event.getTarget() instanceof Node target ? target : null;
        while (at != null && at != treeView) {
            if (at instanceof TreeCell<?> candidate) {
                cell = candidate;
                break;
            }
            at = at.getParent();
        }
        if (cell == null || cell.isEmpty() || cell.getTreeView() != treeView) {
            return null;
        }
        TreeItem<T> item = treeView.getTreeItem(cell.getIndex());
        if (item == null || onDisclosureNode(cell, item, event)) {
            return null;
        }
        return item;
    }

    /**
     * Whether a mouse event lies on the row's disclosure arrow: the same test as JavaFX's cells make, the event's point
     * in the arrow's bounds in the cell, so the two never disagree about whose press it is. An entry without children
     * shows no arrow.
     */
    private static boolean onDisclosureNode(TreeCell<?> cell, TreeItem<?> item, MouseEvent event) {
        Node disclosure = cell.getDisclosureNode();
        if (item.isLeaf() || disclosure == null || disclosure.getParent() != cell) {
            return false;
        }
        Point2D point = cell.sceneToLocal(event.getSceneX(), event.getSceneY());
        return point != null && disclosure.getBoundsInParent().contains(point);
    }

    /**
     * Selects an item of the shown tree, the folders above it unfolded, the tree's keyboard focus on it, scrolled into
     * view as far as needed; null clears the selection. Whether the change opens anything is the caller's to mark.
     */
    private void revealAndSelect(TreeItem<T> item) {
        if (item == null) {
            treeView.getSelectionModel().clearSelection();
            return;
        }
        for (TreeItem<T> parent = item.getParent(); parent != null; parent = parent.getParent()) {
            parent.setExpanded(true);
        }
        int row = treeView.getRow(item);
        selectRow(row);
        showRow(row);
    }

    /**
     * Selects the entry of a row the user clicked or pressed Enter on, as a click does: the tree view takes the focus,
     * the row the selection. The row shows already, so nothing unfolds or scrolls.
     */
    private void selectAsClicked(TreeItem<T> item) {
        treeView.requestFocus();
        selectRow(treeView.getRow(item));
    }

    /** Selects a row of the tree view and puts its keyboard focus there. */
    private void selectRow(int row) {
        treeView.getSelectionModel().clearAndSelect(row);
        treeView.getFocusModel().focus(row);
    }

    /** Folds or unfolds an entry with children; an entry without children stays as it is. */
    private static void toggle(TreeItem<?> item) {
        if (!item.isLeaf()) {
            item.setExpanded(!item.isExpanded());
        }
    }

    /** Hands a value to the open handler, unless the handler runs already. */
    private void open(T value) {
        Consumer<? super T> handler = onOpen.get();
        if (handler == null || openHandlerRun.isUnderWay()) {
            return;
        }
        openHandlerRun.run(() -> handler.accept(value));
    }

    /**
     * Scrolls a row into view as far as needed. The skin's flow scrolls the least; the tree view's own
     * {@link TreeView#scrollTo(int)} would put the row at the top even when it shows already, and serves only before
     * the tree view has its skin.
     */
    private void showRow(int row) {
        if (row < 0) {
            return;
        }
        if (treeView.lookup(".virtual-flow") instanceof VirtualFlow<?> flow) {
            // The flow counts the rows at its next layout: after a folder unfolded or a new root, it would scroll by a
            // stale count, so the tree view lays out first.
            treeView.layout();
            flow.scrollTo(row);
        } else {
            treeView.scrollTo(row);
        }
    }

    /** The first entry below a parent, in the order of the rows, whose value equals the value; null for none. */
    private TreeItem<T> findBelow(TreeItem<T> parent, T value) {
        if (parent == null) {
            return null;
        }
        for (TreeItem<T> child : parent.getChildren()) {
            if (Objects.equals(child.getValue(), value)) {
                return child;
            }
            TreeItem<T> found = findBelow(child, value);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Every entry below the root, in the order of the rows, folded or not; empty without a root. */
    private List<TreeItem<T>> entriesBelow(TreeItem<T> root) {
        List<TreeItem<T>> entries = new ArrayList<>();
        if (root == null) {
            return entries;
        }
        Deque<TreeItem<T>> pending = new ArrayDeque<>(root.getChildren());
        while (!pending.isEmpty()) {
            TreeItem<T> item = pending.removeFirst();
            entries.add(item);
            List<TreeItem<T>> children = item.getChildren();
            for (int index = children.size() - 1; index >= 0; index--) {
                pending.addFirst(children.get(index));
            }
        }
        return entries;
    }

    /** The text of an entry: the text function's, or the value's string form; never null. */
    private String textOf(T value) {
        Function<? super T, String> function = textFunction.get();
        String text;
        if (function == null) {
            text = value == null ? "" : value.toString();
        } else {
            text = function.apply(value);
        }
        return text == null ? "" : text;
    }

    /** The icon of an entry: the icon function's; null for none. */
    private Ikon iconOf(T value) {
        Function<? super T, ? extends Ikon> function = iconFunction.get();
        return function == null ? null : function.apply(value);
    }

    /**
     * The default cell: the entry's icon, if it has one, and its text, side by side, the text a highlight node of the
     * search field, which marks the search's words by itself as the user types.
     */
    private final class EntryCell extends TreeCell<T> {

        @Override
        protected void updateItem(T value, boolean empty) {
            super.updateItem(value, empty);
            setText(null);
            if (empty) {
                setGraphic(null);
                return;
            }
            HBox entry = new HBox();
            entry.getStyleClass().add("navigation-tree-entry");
            entry.setAlignment(Pos.CENTER_LEFT);
            Ikon ikon = iconOf(value);
            if (ikon != null) {
                FontIcon icon = FontIcon.of(ikon);
                icon.getStyleClass().add("navigation-tree-icon");
                entry.getChildren().add(icon);
            }
            TextFlow text = searchInput.highlight(textOf(value));
            text.getStyleClass().add("navigation-tree-text");
            entry.getChildren().add(text);
            setGraphic(entry);
        }
    }

    /**
     * A stretch of the tree's own work that a change of the selection asks about, such as the user's input or the
     * tree's own selection: under way while any run of it is, as runs may nest.
     */
    private static final class Phase {

        /** How many runs are under way now. */
        private int runs;

        /** Whether a run is under way now. */
        boolean isUnderWay() {
            return runs > 0;
        }

        /** Runs a piece of work within the phase. */
        void run(Runnable work) {
            runs++;
            try {
                work.run();
            } finally {
                runs--;
            }
        }

        /** Runs a piece of work within the phase and returns its result. */
        <R> R call(Supplier<R> work) {
            runs++;
            try {
                return work.get();
            } finally {
                runs--;
            }
        }
    }
}
