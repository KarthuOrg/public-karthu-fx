package org.karthu.fx.showcase.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.kordamp.ikonli.javafx.FontIcon;

import org.karthu.fx.dialog.CustomDialog;
import org.karthu.fx.showcase.viewmodel.ShowcaseItem;
import org.karthu.fx.showcase.viewmodel.StyleShowcaseViewModel;
import org.karthu.fx.theme.Appearance;
import org.karthu.fx.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Root controller for style-showcase.fxml.
 * Wires FXML elements to the StyleShowcaseViewModel.
 */
public class StyleShowcaseController {

    private static final Logger log = LoggerFactory.getLogger(StyleShowcaseController.class);

    @FXML
    private ListView<ShowcaseItem> componentList;

    @FXML
    private StackPane contentArea;

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ToggleButton lightScheme;

    @FXML
    private ToggleButton darkScheme;

    @FXML
    private ToggleButton systemScheme;

    private final ToggleGroup schemes = new ToggleGroup();

    /** Shows a choice of the theme's scheme made elsewhere on the switch. Held here: the theme holds it weakly. */
    private final ChangeListener<Appearance> showChoice = (observable, oldChoice, newChoice) -> showScheme(newChoice);

    private StyleShowcaseViewModel viewModel;
    private final Map<String, Parent> pageCache = new HashMap<>();

    /**
     * Binds the scheme switch of the toolbar to the theme's choice, in both directions: a choice on the switch is the
     * choice of every window, and a choice made elsewhere shows on the switch. Called by the FXML loader.
     */
    @FXML
    private void initialize() {
        lightScheme.setUserData(Appearance.LIGHT);
        darkScheme.setUserData(Appearance.DARK);
        systemScheme.setUserData(Appearance.SYSTEM);
        lightScheme.setToggleGroup(schemes);
        darkScheme.setToggleGroup(schemes);
        systemScheme.setToggleGroup(schemes);
        showScheme(ThemeManager.getAppearance());

        schemes.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // A click on the chosen one unselects it: the switch always shows a choice.
                oldToggle.setSelected(true);
            } else {
                ThemeManager.setAppearance((Appearance) newToggle.getUserData());
            }
        });
        ThemeManager.appearanceProperty().addListener(new WeakChangeListener<>(showChoice));
    }

    private void showScheme(Appearance appearance) {
        for (Toggle toggle : schemes.getToggles()) {
            if (toggle.getUserData() == appearance) {
                schemes.selectToggle(toggle);
            }
        }
    }

    /**
     * Initializes this controller with the given ViewModel.
     * Called by the composition root after FXML loading.
     *
     * @param viewModel the ViewModel to bind to
     */
    public void initViewModel(StyleShowcaseViewModel viewModel) {
        this.viewModel = viewModel;

        // Set items from ViewModel
        componentList.setItems(viewModel.getShowcaseItems());

        // Custom ListCell with category headers and icon + label rows
        componentList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ShowcaseItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                FontIcon icon = FontIcon.of(item.icon(), 14);
                icon.getStyleClass().add("sidebar-item-icon");
                Label name = new Label(item.displayName());
                name.getStyleClass().add("sidebar-item-label");
                HBox row = new HBox(8, icon, name);
                row.setAlignment(Pos.CENTER_LEFT);

                // Category header above the first item of each category
                int index = getIndex();
                if (index == 0 || !item.category().equals(getCategory(index - 1))) {
                    Label header = new Label(item.category());
                    header.getStyleClass().add("category-header");
                    setGraphic(new VBox(2, header, row));
                } else {
                    setGraphic(row);
                }
                setText(null);
            }

            private String getCategory(int index) {
                if (index >= 0 && index < viewModel.getShowcaseItems().size()) {
                    return viewModel.getShowcaseItems().get(index).category();
                }
                return "";
            }
        });

        // Selection listener: update ViewModel when user selects
        componentList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> {
                    if (newItem != null) {
                        viewModel.selectItem(newItem);
                    }
                }
        );

        // ViewModel selection listener: load page when ViewModel selection changes
        viewModel.selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> {
                    if (newItem != null) {
                        loadPage(newItem);
                    }
                }
        );

        // Select first item
        if (!viewModel.getShowcaseItems().isEmpty()) {
            componentList.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onAbout() {
        CustomDialog.info(componentList.getScene().getWindow(),
                "About Karthu FX", "Karthu FX Showcase",
                "Version 1.0.0\n\nA showcase for previewing and testing the Karthu FX component library.");
    }

    /**
     * Loads a showcase page for the given item.
     * Uses a cache to avoid reloading previously loaded pages.
     *
     * @param item the showcase item to load
     */
    private void loadPage(ShowcaseItem item) {
        String fxmlFileName = item.fxmlFileName();

        Parent page = pageCache.get(fxmlFileName);
        if (page == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/org/karthu/fx/showcase/showcase/" + fxmlFileName)
                );
                page = loader.load();
                pageCache.put(fxmlFileName, page);
            } catch (IOException e) {
                log.error("Failed to load showcase page: {}", fxmlFileName, e);
                return;
            }
        }

        contentArea.getChildren().setAll(page);

        // Reset scroll to top
        contentScrollPane.setVvalue(0.0);
    }
}
