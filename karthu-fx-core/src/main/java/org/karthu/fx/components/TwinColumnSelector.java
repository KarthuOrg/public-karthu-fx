package org.karthu.fx.components;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A dual-list "twin column" picker: items move between an <em>Available</em>
 * list (left) and a <em>Selected</em> list (right) via chevron buttons,
 * double-click, or Enter. A filter field above each list narrows the view
 * (view-only — the underlying lists are never changed by filtering); the
 * move-all buttons operate on the currently filtered items only.
 *
 * <p>The order of {@link #getSelectedItems()} is significant: it is the
 * display order the consumer applies verbatim. Moves append at the end of
 * the target list. With {@link #setReorderable(boolean) reorderable} enabled,
 * up/down buttons reorder the Selected list.
 *
 * <p>Items must be unique in terms of {@code equals} — moves use
 * equals-based list operations.
 *
 * <p>Styling comes from the theme stylesheet via the style classes
 * {@code twin-column}, {@code twin-header}, {@code twin-filter},
 * {@code twin-list}, and {@code twin-move-button}.
 *
 * @param <T> the item type; labels and filtering are driven by the
 *            {@link #stringConverterProperty() string converter}
 */
public class TwinColumnSelector<T> extends GridPane {

    private static final double BUTTON_COLUMN_WIDTH = 46;

    private final ObservableList<T> availableItems = FXCollections.observableArrayList();
    private final ObservableList<T> selectedItems = FXCollections.observableArrayList();
    private final FilteredList<T> filteredAvailable = new FilteredList<>(availableItems);
    private final FilteredList<T> filteredSelected = new FilteredList<>(selectedItems);

    private final ObjectProperty<StringConverter<T>> stringConverter =
            new SimpleObjectProperty<>(defaultConverter());
    private final ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactory =
            new SimpleObjectProperty<>();
    private final StringProperty availableHeader = new SimpleStringProperty("Available");
    private final StringProperty selectedHeader = new SimpleStringProperty("Selected");
    private final BooleanProperty reorderable = new SimpleBooleanProperty(false);

    private final TextField availableFilter = new TextField();
    private final TextField selectedFilter = new TextField();
    private final ListView<T> availableList = new ListView<>(filteredAvailable);
    private final ListView<T> selectedList = new ListView<>(filteredSelected);

    private final Button moveRightButton =
            moveButton(BootstrapIcons.CHEVRON_RIGHT, "Add selected");
    private final Button moveAllRightButton =
            moveButton(BootstrapIcons.CHEVRON_DOUBLE_RIGHT, "Add all");
    private final Button moveLeftButton =
            moveButton(BootstrapIcons.CHEVRON_LEFT, "Remove selected");
    private final Button moveAllLeftButton =
            moveButton(BootstrapIcons.CHEVRON_DOUBLE_LEFT, "Remove all");
    private final Button moveUpButton =
            moveButton(BootstrapIcons.CHEVRON_UP, "Move up");
    private final Button moveDownButton =
            moveButton(BootstrapIcons.CHEVRON_DOWN, "Move down");

    /** Partition captured at the last {@link #setItems} — restored by {@link #reset()}. */
    private List<T> initialAvailable = List.of();
    private List<T> initialSelected = List.of();

    /**
     * Creates a selector with both lists empty, the headers "Available" and "Selected", the
     * items' {@code toString} as labels, and reordering off.
     */
    public TwinColumnSelector() {
        getStyleClass().add("twin-column");
        buildLayout();
        wireFiltering();
        wireMoves();
        wireEnablement();
        applyCellFactories();
        cellFactory.addListener((obs, oldFactory, newFactory) -> applyCellFactories());
    }

    // --- Public API --------------------------------------------------------

    /** {@return the pool of items not currently selected} */
    public ObservableList<T> getAvailableItems() {
        return availableItems;
    }

    /** {@return the chosen items, in display order (the order is the result)} */
    public ObservableList<T> getSelectedItems() {
        return selectedItems;
    }

    /**
     * Bulk initialization: partitions {@code all} into the two lists.
     * {@code selected} entries not contained in {@code all} are dropped;
     * the Selected order is preserved exactly as given; Available keeps the
     * order of {@code all}. The partition is snapshotted for {@link #reset()}.
     * Null arguments are treated as empty lists.
     *
     * @param all      every item, in the order Available shows them
     * @param selected the items to show as Selected, in their display order
     */
    public void setItems(List<T> all, List<T> selected) {
        List<T> allCopy = all == null ? new ArrayList<>() : new ArrayList<>(all);
        List<T> selectedCopy = selected == null ? new ArrayList<>() : new ArrayList<>(selected);
        selectedCopy.retainAll(allCopy);
        List<T> availableCopy = new ArrayList<>(allCopy);
        availableCopy.removeAll(selectedCopy);
        availableList.getSelectionModel().clearSelection();
        selectedList.getSelectionModel().clearSelection();
        availableItems.setAll(availableCopy);
        selectedItems.setAll(selectedCopy);
        initialAvailable = List.copyOf(availableCopy);
        initialSelected = List.copyOf(selectedCopy);
    }

    /**
     * Restores the partition captured at the last {@link #setItems} call
     * (both lists empty if it was never called). Filter texts are view-only
     * and deliberately left untouched.
     */
    public void reset() {
        availableList.getSelectionModel().clearSelection();
        selectedList.getSelectionModel().clearSelection();
        availableItems.setAll(initialAvailable);
        selectedItems.setAll(initialSelected);
    }

    /**
     * The converter that turns an item into its label. The filters match against the same
     * text. The default uses the item's {@code toString}.
     *
     * @return the string converter property
     */
    public ObjectProperty<StringConverter<T>> stringConverterProperty() {
        return stringConverter;
    }

    public StringConverter<T> getStringConverter() {
        return stringConverter.get();
    }

    public void setStringConverter(StringConverter<T> converter) {
        stringConverter.set(converter);
    }

    /**
     * A custom cell factory applied to both lists; null (the default) shows each item's label
     * from the {@link #stringConverterProperty() string converter}.
     *
     * @return the cell factory property
     */
    public ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactoryProperty() {
        return cellFactory;
    }

    public Callback<ListView<T>, ListCell<T>> getCellFactory() {
        return cellFactory.get();
    }

    /**
     * Custom cell factory applied to both lists; null restores the default.
     *
     * @param factory the cell factory, or null for the default
     */
    public void setCellFactory(Callback<ListView<T>, ListCell<T>> factory) {
        cellFactory.set(factory);
    }

    /**
     * The caption above the Available list, shown in upper case next to the list's item count
     * (default "Available").
     *
     * @return the available header property
     */
    public StringProperty availableHeaderProperty() {
        return availableHeader;
    }

    public String getAvailableHeader() {
        return availableHeader.get();
    }

    public void setAvailableHeader(String header) {
        availableHeader.set(header);
    }

    /**
     * The caption above the Selected list, shown in upper case next to the list's item count
     * (default "Selected").
     *
     * @return the selected header property
     */
    public StringProperty selectedHeaderProperty() {
        return selectedHeader;
    }

    public String getSelectedHeader() {
        return selectedHeader.get();
    }

    public void setSelectedHeader(String header) {
        selectedHeader.set(header);
    }

    /**
     * Whether up/down buttons reorder the Selected list (default false).
     *
     * @return the reorderable property
     */
    public BooleanProperty reorderableProperty() {
        return reorderable;
    }

    public boolean isReorderable() {
        return reorderable.get();
    }

    /**
     * Shows up/down buttons that reorder the Selected list (default false).
     *
     * @param value true to show the buttons
     */
    public void setReorderable(boolean value) {
        reorderable.set(value);
    }

    // --- Layout ------------------------------------------------------------

    private void buildLayout() {
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setHgrow(Priority.ALWAYS);
        leftColumn.setFillWidth(true);
        leftColumn.setMinWidth(120);

        ColumnConstraints buttonColumn = new ColumnConstraints(
                BUTTON_COLUMN_WIDTH, BUTTON_COLUMN_WIDTH, BUTTON_COLUMN_WIDTH);
        buttonColumn.setHgrow(Priority.NEVER);
        buttonColumn.setHalignment(HPos.CENTER);

        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setHgrow(Priority.ALWAYS);
        rightColumn.setFillWidth(true);
        rightColumn.setMinWidth(120);

        getColumnConstraints().addAll(leftColumn, buttonColumn, rightColumn);

        RowConstraints headerRow = new RowConstraints();
        RowConstraints filterRow = new RowConstraints();
        RowConstraints listRow = new RowConstraints();
        listRow.setVgrow(Priority.ALWAYS);
        listRow.setFillHeight(true);
        getRowConstraints().addAll(headerRow, filterRow, listRow);

        availableFilter.setPromptText("Filter…");
        availableFilter.getStyleClass().add("twin-filter");
        selectedFilter.setPromptText("Filter…");
        selectedFilter.getStyleClass().add("twin-filter");

        availableList.getStyleClass().add("twin-list");
        availableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        selectedList.getStyleClass().add("twin-list");
        selectedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Both lists share the same pref width so the grid distributes the
        // extra space symmetrically.
        availableList.setPrefWidth(240);
        availableList.setMaxWidth(Double.MAX_VALUE);
        selectedList.setPrefWidth(240);
        selectedList.setMaxWidth(Double.MAX_VALUE);

        VBox buttonBox = new VBox(6,
                moveRightButton, moveAllRightButton,
                moveLeftButton, moveAllLeftButton,
                moveUpButton, moveDownButton);
        buttonBox.getStyleClass().add("twin-move-column");
        buttonBox.setAlignment(Pos.CENTER);
        // Visual grouping: add | remove | reorder.
        VBox.setMargin(moveLeftButton, new Insets(12, 0, 0, 0));
        VBox.setMargin(moveUpButton, new Insets(12, 0, 0, 0));
        moveUpButton.visibleProperty().bind(reorderable);
        moveUpButton.managedProperty().bind(reorderable);
        moveDownButton.visibleProperty().bind(reorderable);
        moveDownButton.managedProperty().bind(reorderable);
        GridPane.setValignment(buttonBox, VPos.CENTER);

        // Children in focus-traversal order: Available filter → Available
        // list → buttons → Selected filter → Selected list.
        add(header(availableHeader, availableItems), 0, 0);
        add(availableFilter, 0, 1);
        add(availableList, 0, 2);
        add(buttonBox, 1, 2);
        add(header(selectedHeader, selectedItems), 2, 0);
        add(selectedFilter, 2, 1);
        add(selectedList, 2, 2);
    }

    private HBox header(StringProperty caption, ObservableList<T> sourceList) {
        Label captionLabel = new Label();
        captionLabel.getStyleClass().add("twin-header-caption");
        captionLabel.textProperty().bind(Bindings.createStringBinding(
                () -> caption.get() == null ? "" : caption.get().toUpperCase(Locale.ROOT),
                caption));
        Label countLabel = new Label();
        countLabel.getStyleClass().add("twin-header-count");
        // Counts follow the SOURCE lists, not the filtered views.
        countLabel.textProperty().bind(Bindings.size(sourceList).asString());
        HBox box = new HBox(6, captionLabel, countLabel);
        box.getStyleClass().add("twin-header");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Button moveButton(BootstrapIcons icon, String accessibleText) {
        Button button = new Button();
        button.setGraphic(FontIcon.of(icon, 12));
        button.getStyleClass().add("twin-move-button");
        button.setAccessibleText(accessibleText);
        button.setFocusTraversable(true);
        // Space activation is built in; Enter should activate too.
        button.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                button.fire();
                event.consume();
            }
        });
        return button;
    }

    // --- Filtering ----------------------------------------------------------

    private void wireFiltering() {
        availableFilter.textProperty().addListener(
                (obs, oldText, newText) -> updateFilterPredicate(availableFilter, filteredAvailable));
        selectedFilter.textProperty().addListener(
                (obs, oldText, newText) -> updateFilterPredicate(selectedFilter, filteredSelected));
        // The converter drives both labels and filter matching.
        stringConverter.addListener((obs, oldConverter, newConverter) -> {
            updateFilterPredicate(availableFilter, filteredAvailable);
            updateFilterPredicate(selectedFilter, filteredSelected);
            availableList.refresh();
            selectedList.refresh();
        });
    }

    private void updateFilterPredicate(TextField filterField, FilteredList<T> filtered) {
        String query = filterField.getText();
        if (query == null || query.trim().isEmpty()) {
            filtered.setPredicate(null);
            return;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        filtered.setPredicate(item -> itemText(item).toLowerCase(Locale.ROOT).contains(needle));
    }

    private String itemText(T item) {
        StringConverter<T> converter = stringConverter.get();
        if (converter == null) {
            return String.valueOf(item);
        }
        String text = converter.toString(item);
        return text == null ? "" : text;
    }

    // --- Moves --------------------------------------------------------------

    private void wireMoves() {
        moveRightButton.setOnAction(event -> moveRight());
        moveAllRightButton.setOnAction(event -> moveAllRight());
        moveLeftButton.setOnAction(event -> moveLeft());
        moveAllLeftButton.setOnAction(event -> moveAllLeft());
        moveUpButton.setOnAction(event -> moveUp());
        moveDownButton.setOnAction(event -> moveDown());

        availableList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                moveRight();
            }
        });
        selectedList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                moveLeft();
            }
        });
        availableList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                moveRight();
                event.consume();
            }
        });
        selectedList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                moveLeft();
                event.consume();
            }
        });
    }

    /** Moves the rows selected in Available to the end of Selected. */
    void moveRight() {
        moveItems(new ArrayList<>(availableList.getSelectionModel().getSelectedItems()),
                availableItems, selectedItems, availableList);
    }

    /** Moves ALL currently visible (filtered) Available items to Selected. */
    void moveAllRight() {
        moveItems(new ArrayList<>(filteredAvailable),
                availableItems, selectedItems, availableList);
    }

    /** Moves the rows selected in Selected back to Available. */
    void moveLeft() {
        moveItems(new ArrayList<>(selectedList.getSelectionModel().getSelectedItems()),
                selectedItems, availableItems, selectedList);
    }

    /** Moves ALL currently visible (filtered) Selected items to Available. */
    void moveAllLeft() {
        moveItems(new ArrayList<>(filteredSelected),
                selectedItems, availableItems, selectedList);
    }

    /**
     * Snapshot → clear selection → mutate: the live selection and the
     * FilteredList views must never be iterated while their source list is
     * structurally changed.
     */
    private void moveItems(List<T> toMove, ObservableList<T> source, ObservableList<T> target,
            ListView<T> sourceView) {
        if (toMove.isEmpty()) {
            return;
        }
        sourceView.getSelectionModel().clearSelection();
        source.removeAll(toMove);
        target.addAll(toMove);
    }

    /** Moves the single selected row one position up in the Selected list. */
    void moveUp() {
        T item = selectedList.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        int sourceIndex = selectedItems.indexOf(item);
        if (sourceIndex <= 0) {
            return;
        }
        selectedList.getSelectionModel().clearSelection();
        selectedItems.remove(sourceIndex);
        selectedItems.add(sourceIndex - 1, item);
        selectedList.getSelectionModel().select(filteredSelected.indexOf(item));
    }

    /** Moves the single selected row one position down in the Selected list. */
    void moveDown() {
        T item = selectedList.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        int sourceIndex = selectedItems.indexOf(item);
        if (sourceIndex < 0 || sourceIndex >= selectedItems.size() - 1) {
            return;
        }
        selectedList.getSelectionModel().clearSelection();
        selectedItems.remove(sourceIndex);
        selectedItems.add(sourceIndex + 1, item);
        selectedList.getSelectionModel().select(filteredSelected.indexOf(item));
    }

    // --- Enablement ----------------------------------------------------------

    private void wireEnablement() {
        moveRightButton.disableProperty().bind(
                Bindings.isEmpty(availableList.getSelectionModel().getSelectedItems()));
        moveAllRightButton.disableProperty().bind(Bindings.isEmpty(filteredAvailable));
        moveLeftButton.disableProperty().bind(
                Bindings.isEmpty(selectedList.getSelectionModel().getSelectedItems()));
        moveAllLeftButton.disableProperty().bind(Bindings.isEmpty(filteredSelected));
        moveUpButton.disableProperty().bind(
                selectedList.getSelectionModel().selectedItemProperty().isNull());
        moveDownButton.disableProperty().bind(
                selectedList.getSelectionModel().selectedItemProperty().isNull());
    }

    // --- Cells ---------------------------------------------------------------

    private void applyCellFactories() {
        Callback<ListView<T>, ListCell<T>> factory = cellFactory.get();
        if (factory == null) {
            factory = defaultCellFactory();
        }
        availableList.setCellFactory(factory);
        selectedList.setCellFactory(factory);
    }

    private Callback<ListView<T>, ListCell<T>> defaultCellFactory() {
        return listView -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null);
                setText(empty || item == null ? null : itemText(item));
            }
        };
    }

    private static <T> StringConverter<T> defaultConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(T object) {
                return object == null ? "" : object.toString();
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    // --- Package-visible test hooks ------------------------------------------

    /**
     * Test hook.
     *
     * @return the list of the available items
     */
    ListView<T> availableListView() {
        return availableList;
    }

    /**
     * Test hook.
     *
     * @return the list of the selected items
     */
    ListView<T> selectedListView() {
        return selectedList;
    }

    /**
     * Test hook.
     *
     * @return the filter field of the available items
     */
    TextField availableFilterField() {
        return availableFilter;
    }

    /**
     * Test hook.
     *
     * @return the filter field of the selected items
     */
    TextField selectedFilterField() {
        return selectedFilter;
    }

    /**
     * Test hook.
     *
     * @return the button that moves the chosen items to the right
     */
    Button moveRightButtonNode() {
        return moveRightButton;
    }

    /**
     * Test hook.
     *
     * @return the button that moves every item to the right
     */
    Button moveAllRightButtonNode() {
        return moveAllRightButton;
    }

    /**
     * Test hook.
     *
     * @return the button that moves the chosen items to the left
     */
    Button moveLeftButtonNode() {
        return moveLeftButton;
    }

    /**
     * Test hook.
     *
     * @return the button that moves every item to the left
     */
    Button moveAllLeftButtonNode() {
        return moveAllLeftButton;
    }

    /**
     * Test hook.
     *
     * @return the button that moves the chosen items up
     */
    Button moveUpButtonNode() {
        return moveUpButton;
    }

    /**
     * Test hook.
     *
     * @return the button that moves the chosen items down
     */
    Button moveDownButtonNode() {
        return moveDownButton;
    }
}
