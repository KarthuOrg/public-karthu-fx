package org.karthu.fx.settings;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import org.karthu.fx.form.PathFilter;
import org.karthu.fx.form.PathInput;
import org.karthu.fx.theme.ThemeManager;

/**
 * IntelliJ-style settings dialog with a TreeView for category navigation
 * and a content panel for editing settings.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * SettingsDialogResult result = new SettingsDialog.Builder()
 *     .owner(primaryStage)
 *     .title("settings.dialog.title")
 *     .model(settingsModel)
 *     .onApply(() -> saveSettings())
 *     .prefSize(900, 600)
 *     .build()
 *     .showAndWait();
 * }</pre>
 */
public class SettingsDialog {

    private static final String BUTTON_OK_KEY = "settings.button.ok";
    private static final String BUTTON_CANCEL_KEY = "settings.button.cancel";
    private static final String BUTTON_APPLY_KEY = "settings.button.apply";

    private static final String BUTTON_OK_FALLBACK = "OK";
    private static final String BUTTON_CANCEL_FALLBACK = "Cancel";
    private static final String BUTTON_APPLY_FALLBACK = "Apply";

    private static final double TREE_MIN_WIDTH = 220.0;
    private static final double DIVIDER_POSITION = 0.25;

    private final Stage dialogStage;
    private final SettingsModel model;
    private final Runnable onApply;
    private final Map<String, Node> contentCache = new HashMap<>();
    private final Button applyButton;

    private SettingsDialogResult result = SettingsDialogResult.CANCEL;
    private Map<String, Object> currentSnapshot;

    private SettingsDialog(Builder builder) {
        this.model = builder.model;
        this.onApply = builder.onApply;

        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setResizable(true);
        dialogStage.setTitle(resolveTitle(builder.title));

        if (builder.owner != null) {
            dialogStage.initOwner(builder.owner);
            if (builder.owner instanceof Stage ownerStage) {
                dialogStage.getIcons().addAll(ownerStage.getIcons());
            }
        }

        // Take initial snapshot
        currentSnapshot = model.snapshot();

        // --- Left: TreeView ---
        TreeView<SettingCategory> treeView = buildTreeView();
        treeView.setMinWidth(TREE_MIN_WIDTH);
        treeView.getStyleClass().add("settings-tree-view");

        // --- Right: Breadcrumb + Content ---
        HBox breadcrumb = new HBox(4);
        breadcrumb.getStyleClass().add("settings-breadcrumb");
        breadcrumb.setAlignment(Pos.CENTER_LEFT);
        breadcrumb.setPadding(new Insets(8, 12, 8, 12));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("settings-scroll-pane");

        VBox rightPane = new VBox();
        rightPane.getStyleClass().add("settings-right-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        rightPane.getChildren().addAll(breadcrumb, new Separator(), scrollPane);

        // --- SplitPane ---
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().addAll(treeView, rightPane);
        splitPane.setDividerPositions(DIVIDER_POSITION);
        SplitPane.setResizableWithParent(treeView, false);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // --- Button bar ---
        applyButton = createButton(resolveButtonText(BUTTON_APPLY_KEY, BUTTON_APPLY_FALLBACK), null);
        applyButton.setDisable(true);
        applyButton.setOnAction(e -> handleApply());

        Button cancelButton = createButton(resolveButtonText(BUTTON_CANCEL_KEY, BUTTON_CANCEL_FALLBACK), null);
        cancelButton.setOnAction(e -> handleCancel());

        Button okButton = createButton(resolveButtonText(BUTTON_OK_KEY, BUTTON_OK_FALLBACK), "primary-button");
        okButton.setOnAction(e -> handleOk());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(10);
        buttonBar.getStyleClass().add("settings-button-bar");
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(8, 15, 8, 15));
        buttonBar.getChildren().addAll(spacer, okButton, cancelButton, applyButton);

        // --- Root ---
        VBox root = new VBox();
        root.getStyleClass().add("settings-dialog");
        root.getChildren().addAll(splitPane, buttonBar);

        // --- Scene ---
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);

        dialogStage.setScene(scene);
        dialogStage.setMinWidth(builder.minWidth);
        dialogStage.setMinHeight(builder.minHeight);
        if (builder.prefWidth > 0) {
            dialogStage.setWidth(builder.prefWidth);
        }
        if (builder.prefHeight > 0) {
            dialogStage.setHeight(builder.prefHeight);
        }

        // Center on owner
        if (builder.owner != null) {
            Window owner = builder.owner;
            dialogStage.setOnShown(e -> {
                dialogStage.setX(owner.getX() + (owner.getWidth() - dialogStage.getWidth()) / 2);
                dialogStage.setY(owner.getY() + (owner.getHeight() - dialogStage.getHeight()) / 2);
            });
        }

        // Wire tree selection to content + breadcrumb
        treeView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> {
                    if (newItem != null) {
                        SettingCategory category = newItem.getValue();
                        // Auto-select first child if category has no groups
                        if (category.groups().isEmpty() && !category.children().isEmpty()) {
                            TreeItem<SettingCategory> firstChild = newItem.getChildren().getFirst();
                            treeView.getSelectionModel().select(firstChild);
                            return;
                        }
                        updateBreadcrumb(breadcrumb, newItem);
                        scrollPane.setContent(getOrBuildContent(category));
                    }
                }
        );

        // Auto-select first leaf category
        selectFirstLeaf(treeView);
    }

    /**
     * Shows the dialog and waits for the user to close it.
     *
     * @return the dialog result
     */
    public SettingsDialogResult showAndWait() {
        dialogStage.showAndWait();
        return result;
    }

    /**
     * The dialog's scene, built and themed before its window shows: the tests of this package style it without
     * opening a window.
     *
     * @return the scene of the dialog's window
     */
    Scene scene() {
        return dialogStage.getScene();
    }

    // --- Tree building ---

    private TreeView<SettingCategory> buildTreeView() {
        TreeItem<SettingCategory> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);

        for (SettingCategory category : model.getCategories()) {
            rootItem.getChildren().add(buildTreeItem(category));
        }

        TreeView<SettingCategory> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(SettingCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(model.resolve(item.labelKey()));
                }
            }
        });

        return treeView;
    }

    private TreeItem<SettingCategory> buildTreeItem(SettingCategory category) {
        TreeItem<SettingCategory> item = new TreeItem<>(category);
        item.setExpanded(true);
        for (SettingCategory child : category.children()) {
            item.getChildren().add(buildTreeItem(child));
        }
        return item;
    }

    private void selectFirstLeaf(TreeView<SettingCategory> treeView) {
        TreeItem<SettingCategory> root = treeView.getRoot();
        if (root != null && !root.getChildren().isEmpty()) {
            TreeItem<SettingCategory> first = findFirstLeaf(root.getChildren().getFirst());
            treeView.getSelectionModel().select(first);
        }
    }

    private TreeItem<SettingCategory> findFirstLeaf(TreeItem<SettingCategory> item) {
        SettingCategory category = item.getValue();
        if (category.groups().isEmpty() && !item.getChildren().isEmpty()) {
            return findFirstLeaf(item.getChildren().getFirst());
        }
        return item;
    }

    // --- Breadcrumb ---

    private void updateBreadcrumb(HBox breadcrumb, TreeItem<SettingCategory> selectedItem) {
        breadcrumb.getChildren().clear();

        List<TreeItem<SettingCategory>> path = new ArrayList<>();
        TreeItem<SettingCategory> current = selectedItem;
        while (current != null && current.getValue() != null) {
            path.addFirst(current);
            current = current.getParent();
        }

        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                Label separator = new Label("\u203A"); // single right-pointing angle quotation mark
                separator.getStyleClass().add("settings-breadcrumb-separator");
                breadcrumb.getChildren().add(separator);
            }
            Label segment = new Label(model.resolve(path.get(i).getValue().labelKey()));
            segment.getStyleClass().add("settings-breadcrumb-segment");
            breadcrumb.getChildren().add(segment);
        }
    }

    // --- Content panel ---

    private Node getOrBuildContent(SettingCategory category) {
        return contentCache.computeIfAbsent(category.key(), k -> buildContentPanel(category));
    }

    private Node buildContentPanel(SettingCategory category) {
        VBox content = new VBox(12);
        content.getStyleClass().add("settings-content");
        content.setPadding(new Insets(12, 16, 12, 16));

        for (SettingGroup group : category.groups()) {
            // Group title + separator
            String groupTitle = model.resolve(group.labelKey());
            if (groupTitle != null && !groupTitle.isEmpty()) {
                Label titleLabel = new Label(groupTitle);
                titleLabel.getStyleClass().add("settings-group-title");

                Separator groupSeparator = new Separator();
                groupSeparator.getStyleClass().add("settings-group-separator");

                content.getChildren().addAll(titleLabel, groupSeparator);
            }

            // Settings grid
            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(8);

            ColumnConstraints labelCol = new ColumnConstraints();
            labelCol.setMinWidth(150);
            labelCol.setPrefWidth(200);
            ColumnConstraints controlCol = new ColumnConstraints();
            controlCol.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().addAll(labelCol, controlCol);

            int row = 0;
            for (SettingEntry<?> entry : group.entries()) {
                row = addEntryToGrid(grid, entry, row);
            }

            content.getChildren().add(grid);
        }

        return content;
    }

    private int addEntryToGrid(GridPane grid, SettingEntry<?> entry, int row) {
        // For BOOLEAN, the CheckBox includes its own label text
        if (entry.getType() == SettingType.BOOLEAN) {
            Node control = createControl(entry);
            GridPane.setColumnSpan(control, 2);
            grid.add(control, 0, row);
        } else {
            Label label = new Label(model.resolve(entry.getLabelKey()));
            label.getStyleClass().add("settings-entry-label");
            grid.add(label, 0, row);

            Node control = createControl(entry);
            grid.add(control, 1, row);
        }
        row++;

        // Description hint below the control
        if (entry.getDescriptionKey() != null) {
            Label description = new Label(model.resolve(entry.getDescriptionKey()));
            description.getStyleClass().add("settings-entry-description");
            description.setWrapText(true);
            GridPane.setColumnSpan(description, 2);
            grid.add(description, 0, row);
            row++;
        }

        return row;
    }

    @SuppressWarnings("unchecked")
    private Node createControl(SettingEntry<?> entry) {
        return switch (entry.getType()) {
            case STRING -> {
                SettingEntry<String> stringEntry = (SettingEntry<String>) entry;
                TextField textField = new TextField(stringEntry.getValue());
                textField.textProperty().addListener(createDirtyListener());
                textField.textProperty().addListener((obs, oldVal, newVal) -> stringEntry.setValue(newVal));
                stringEntry.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (!textField.getText().equals(newVal)) {
                        textField.setText(newVal);
                    }
                });
                yield textField;
            }
            case BOOLEAN -> {
                SettingEntry<Boolean> boolEntry = (SettingEntry<Boolean>) entry;
                CheckBox checkBox = new CheckBox(model.resolve(entry.getLabelKey()));
                checkBox.setSelected(boolEntry.getValue());
                checkBox.selectedProperty().addListener(createDirtyListener());
                checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> boolEntry.setValue(newVal));
                boolEntry.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (checkBox.isSelected() != newVal) {
                        checkBox.setSelected(newVal);
                    }
                });
                yield checkBox;
            }
            case INTEGER -> {
                SettingEntry<Integer> intEntry = (SettingEntry<Integer>) entry;
                SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(
                                intEntry.getMin(), intEntry.getMax(),
                                intEntry.getValue(), intEntry.getStep());
                Spinner<Integer> spinner = new Spinner<>(factory);
                spinner.setEditable(true);
                spinner.setPrefWidth(120);
                spinner.valueProperty().addListener(createDirtyListener());
                spinner.valueProperty().addListener((obs, oldVal, newVal) -> intEntry.setValue(newVal));
                intEntry.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (!intEntry.getValue().equals(factory.getValue())) {
                        factory.setValue(newVal);
                    }
                });
                yield spinner;
            }
            case CHOICE -> {
                SettingEntry<String> choiceEntry = (SettingEntry<String>) entry;
                ComboBox<String> comboBox = new ComboBox<>(
                        FXCollections.observableArrayList(choiceEntry.getChoices()));
                comboBox.setValue(choiceEntry.getValue());
                comboBox.valueProperty().addListener(createDirtyListener());
                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> choiceEntry.setValue(newVal));
                choiceEntry.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal.equals(comboBox.getValue())) {
                        comboBox.setValue(newVal);
                    }
                });
                yield comboBox;
            }
            case SECRET -> {
                SettingEntry<String> secretEntry = (SettingEntry<String>) entry;
                PasswordField passwordField = new PasswordField();
                passwordField.setText(secretEntry.getValue());
                passwordField.textProperty().addListener(createDirtyListener());
                passwordField.textProperty().addListener((obs, oldVal, newVal) -> secretEntry.setValue(newVal));
                secretEntry.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (!passwordField.getText().equals(newVal)) {
                        passwordField.setText(newVal);
                    }
                });
                yield passwordField;
            }
            case DATETIME -> {
                SettingEntry<LocalDate> dateEntry = (SettingEntry<LocalDate>) entry;
                DatePicker datePicker = new DatePicker(dateEntry.getValue());
                datePicker.setPrefWidth(180);
                datePicker.valueProperty().addListener(createDirtyListener());
                datePicker.valueProperty().addListener((obs, oldVal, newVal) -> dateEntry.setValue(newVal));
                dateEntry.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (!java.util.Objects.equals(datePicker.getValue(), newVal)) {
                        datePicker.setValue(newVal);
                    }
                });
                yield datePicker;
            }
            case PATH -> createPathInput((SettingEntry<String>) entry);
        };
    }

    /**
     * The path input of a {@link SettingType#PATH} entry: the entry's mode, its filters with their descriptions
     * resolved like the labels, and the path's text kept in step with the entry's value both ways. The field keeps
     * PathInput's own directory memory, so the dialog starts where the user chose last.
     */
    private PathInput createPathInput(SettingEntry<String> pathEntry) {
        PathInput pathInput = new PathInput();
        pathInput.setMode(pathEntry.getPathMode());
        for (PathFilter filter : pathEntry.getPathFilters()) {
            pathInput.getFilters().add(new PathFilter(model.resolve(filter.description()), filter.patterns()));
        }
        pathInput.setValue(pathEntry.getValue() != null ? pathEntry.getValue() : "");
        // The entry takes the text first, so the dirty check that follows compares the new value.
        pathInput.valueProperty().addListener((obs, oldVal, newVal) -> pathEntry.setValue(newVal));
        pathInput.valueProperty().addListener(createDirtyListener());
        pathEntry.valueProperty().addListener((obs, oldVal, newVal) -> {
            String shown = newVal != null ? newVal : "";
            if (!pathInput.getValue().equals(shown)) {
                pathInput.setValue(shown);
            }
        });
        return pathInput;
    }

    // --- Dirty tracking ---

    private <T> ChangeListener<T> createDirtyListener() {
        return (obs, oldVal, newVal) -> applyButton.setDisable(!model.isDirty(currentSnapshot));
    }

    // --- Button handlers ---

    private void handleOk() {
        result = SettingsDialogResult.OK;
        if (onApply != null && model.isDirty(currentSnapshot)) {
            onApply.run();
        }
        dialogStage.close();
    }

    private void handleCancel() {
        result = SettingsDialogResult.CANCEL;
        model.restore(currentSnapshot);
        dialogStage.close();
    }

    private void handleApply() {
        if (onApply != null) {
            onApply.run();
        }
        currentSnapshot = model.snapshot();
        applyButton.setDisable(true);
    }

    // --- Helpers ---

    private Button createButton(String text, String extraStyleClass) {
        Button button = new Button(text);
        button.getStyleClass().add("settings-button");
        if (extraStyleClass != null) {
            button.getStyleClass().add(extraStyleClass);
        }
        button.setMinWidth(80);
        return button;
    }

    private String resolveTitle(String titleKey) {
        String resolved = model.resolve(titleKey);
        return resolved.equals(titleKey) ? "Settings" : resolved;
    }

    private String resolveButtonText(String key, String fallback) {
        String resolved = model.resolve(key);
        return resolved.equals(key) ? fallback : resolved;
    }

    // --- Builder ---

    /**
     * Builder for creating a {@link SettingsDialog}.
     */
    public static class Builder {

        private Window owner;
        private String title = "Settings";
        private SettingsModel model;
        private Runnable onApply;
        private double minWidth = 600;
        private double minHeight = 400;
        private double prefWidth = 900;
        private double prefHeight = 600;

        /**
         * Creates a builder with the defaults: title "Settings", minimum size 600 x 400,
         * preferred size 900 x 600, no owner and no apply callback.
         */
        public Builder() {
        }

        /**
         * Sets the owner window. The dialog will be centered on it and inherit its stage icons.
         *
         * @param owner the owner window, or null for none (the default)
         * @return this builder
         */
        public Builder owner(Window owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Sets the dialog title. This can be an i18n key that will be resolved
         * through the model's ResourceBundle.
         *
         * @param title the title or its i18n key (default "Settings")
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the settings model containing all categories and entries.
         *
         * @param model the settings model; required
         * @return this builder
         */
        public Builder model(SettingsModel model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the callback invoked when the user clicks Apply or OK (if dirty).
         *
         * @param onApply the callback, or null for none (the default)
         * @return this builder
         */
        public Builder onApply(Runnable onApply) {
            this.onApply = onApply;
            return this;
        }

        /**
         * Sets the minimum dialog size.
         *
         * @param width  the minimum width (default 600)
         * @param height the minimum height (default 400)
         * @return this builder
         */
        public Builder minSize(double width, double height) {
            this.minWidth = width;
            this.minHeight = height;
            return this;
        }

        /**
         * Sets the preferred dialog size.
         *
         * @param width  the preferred width (default 900); 0 or less keeps the size the content needs
         * @param height the preferred height (default 600); 0 or less keeps the size the content needs
         * @return this builder
         */
        public Builder prefSize(double width, double height) {
            this.prefWidth = width;
            this.prefHeight = height;
            return this;
        }

        /**
         * Builds the settings dialog.
         *
         * @return the configured dialog
         * @throws IllegalStateException if no model has been set
         */
        public SettingsDialog build() {
            if (model == null) {
                throw new IllegalStateException("SettingsModel must be set before building the dialog");
            }
            return new SettingsDialog(this);
        }
    }
}
