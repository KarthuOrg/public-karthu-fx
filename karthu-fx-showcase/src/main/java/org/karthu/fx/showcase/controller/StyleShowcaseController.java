package org.karthu.fx.showcase.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import org.kordamp.ikonli.javafx.FontIcon;

import org.karthu.fx.dialog.CustomDialog;
import org.karthu.fx.showcase.viewmodel.GroupHeading;
import org.karthu.fx.showcase.viewmodel.NavigationEntry;
import org.karthu.fx.showcase.viewmodel.SectionHeading;
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
    private TreeView<NavigationEntry> navigationTree;

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

        // The tree of sections, groups and pages; its root is the view model's and stays hidden.
        navigationTree.setShowRoot(false);
        navigationTree.setRoot(viewModel.getNavigationRoot());
        navigationTree.setCellFactory(tree -> new NavigationCell());

        // A page row opens its page; a section or group row opens nothing and leaves the open page as it is.
        navigationTree.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldNode, newNode) -> {
                    if (newNode != null && newNode.getValue() instanceof ShowcaseItem page) {
                        viewModel.selectItem(page);
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

        // Select the first page's row, which opens the page
        if (!viewModel.getShowcaseItems().isEmpty()) {
            selectPageRow(viewModel.getShowcaseItems().get(0));
        }
    }

    /**
     * Selects the tree row of a page, which opens the page; clears the tree's selection when the tree does not show the
     * page, which leaves the open page as it is.
     *
     * @param page the page
     */
    private void selectPageRow(ShowcaseItem page) {
        TreeItem<NavigationEntry> node = viewModel.navigationNodeOf(page);
        if (node == null) {
            navigationTree.getSelectionModel().clearSelection();
        } else {
            navigationTree.getSelectionModel().select(node);
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onAbout() {
        CustomDialog.info(navigationTree.getScene().getWindow(),
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

    /**
     * A row of the navigation tree: a page with its icon and name, or a section or group heading. A heading row carries
     * the pseudo-class {@code :section-heading} or {@code :group-heading}, its label the style class
     * {@code navigation-section-header} or {@code navigation-group-header}.
     */
    private static final class NavigationCell extends TreeCell<NavigationEntry> {

        private static final PseudoClass SECTION_HEADING = PseudoClass.getPseudoClass("section-heading");
        private static final PseudoClass GROUP_HEADING = PseudoClass.getPseudoClass("group-heading");

        @Override
        protected void updateItem(NavigationEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            setText(null);
            pseudoClassStateChanged(SECTION_HEADING, !empty && entry instanceof SectionHeading);
            pseudoClassStateChanged(GROUP_HEADING, !empty && entry instanceof GroupHeading);
            if (empty || entry == null) {
                setGraphic(null);
                return;
            }
            switch (entry) {
                case ShowcaseItem page -> setGraphic(pageRow(page));
                case SectionHeading section -> setGraphic(heading(section.text(), "navigation-section-header"));
                case GroupHeading group -> setGraphic(heading(group.text(), "navigation-group-header"));
            }
        }

        private static HBox pageRow(ShowcaseItem page) {
            FontIcon icon = FontIcon.of(page.icon(), 14);
            icon.getStyleClass().add("sidebar-item-icon");
            Label name = new Label(page.displayName());
            name.getStyleClass().add("sidebar-item-label");
            HBox row = new HBox(8, icon, name);
            row.setAlignment(Pos.CENTER_LEFT);
            return row;
        }

        private static Label heading(String text, String styleClass) {
            Label heading = new Label(text);
            heading.getStyleClass().add(styleClass);
            return heading;
        }
    }
}
