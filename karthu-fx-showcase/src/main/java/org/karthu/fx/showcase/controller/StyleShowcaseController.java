package org.karthu.fx.showcase.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import org.kordamp.ikonli.javafx.FontIcon;

import org.karthu.fx.dialog.CustomDialog;
import org.karthu.fx.form.SearchInput;
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

    /** The key that puts the focus into the navigation's search: Ctrl+F, Cmd+F on macOS. */
    private static final KeyCombination FIND = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);

    @FXML
    private SearchInput navigationSearch;

    @FXML
    private TreeView<NavigationEntry> navigationTree;

    @FXML
    private Label noMatches;

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

    /** Ctrl+F on the window's scene: the focus into the search, unless a handler before it took the key. */
    private final EventHandler<KeyEvent> findHandler = this::find;

    /** The scene {@link #findHandler} is added to, null while the window has none. */
    private Scene findScene;

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

        // The search narrows the tree; "No matches" shows while it leaves no page.
        viewModel.connectSearch(navigationSearch);
        noMatches.visibleProperty().bind(viewModel.hasPagesProperty().not());

        // A page row opens its page; a section or group row opens nothing and leaves the open page as it is. While the
        // tree is rebuilt its selection moves on its own: that is no choice of the user's and opens nothing.
        navigationTree.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldNode, newNode) -> {
                    if (viewModel.rebuildingProperty().get()) {
                        return;
                    }
                    if (newNode != null && newNode.getValue() instanceof ShowcaseItem page) {
                        viewModel.selectItem(page);
                    }
                }
        );

        // After a rebuild the open page's row is selected again when the tree shows it; otherwise no row is selected
        // and the page stays open.
        viewModel.rebuildingProperty().addListener(
                (obs, wasRebuilding, isRebuilding) -> {
                    if (!isRebuilding && viewModel.getSelectedItem() != null) {
                        selectPageRow(viewModel.getSelectedItem());
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

        // The keyboard: Ctrl+F anywhere in the window, Arrow down and Enter in the search, Enter on a page row. Esc in
        // the search is the field's own: it empties a field with text and consumes the key.
        navigationTree.sceneProperty().addListener((obs, oldScene, newScene) -> addFindHandler(newScene));
        addFindHandler(navigationTree.getScene());
        navigationSearch.addEventFilter(KeyEvent.KEY_PRESSED, this::searchKeyPressed);
        navigationTree.addEventFilter(KeyEvent.KEY_PRESSED, this::treeKeyPressed);

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

    /**
     * Adds the Ctrl+F handler to the window's scene, after every handler the scene has now, and takes it from the scene
     * it was added to before. A handler, not a filter: the key reaches it after the focused node and its parents, so a
     * page that takes Ctrl+F where it has the focus keeps it. A page may add a handler of its own to the scene when it
     * enters it, as the Shortcuts page adds its command registry's; every handler of a node hears the key even once one
     * consumed it, so this one is added again after each page that enters the scene and checks whether the key was
     * taken.
     *
     * @param scene the window's scene, null for none
     */
    private void addFindHandler(Scene scene) {
        if (findScene != null) {
            findScene.removeEventHandler(KeyEvent.KEY_PRESSED, findHandler);
        }
        findScene = scene;
        if (scene != null) {
            scene.addEventHandler(KeyEvent.KEY_PRESSED, findHandler);
        }
    }

    /** Ctrl+F that nothing took before: the focus into the search field, its text selected. */
    private void find(KeyEvent event) {
        if (event.isConsumed() || !FIND.match(event)) {
            return;
        }
        TextField text = navigationSearch.getTextField();
        text.requestFocus();
        text.selectAll();
        event.consume();
    }

    /**
     * Arrow down and Enter in the search field, without a modifier: Arrow down moves the focus to the tree and selects
     * the first hit's row, Enter selects that row and keeps the focus in the field; selecting the row opens the page.
     * Without a hit both pass on. A filter, ahead of the text field's own keys; the field's own Enter acts only while it
     * searches a Markdown view, which this one never does.
     */
    private void searchKeyPressed(KeyEvent event) {
        if (event.isAltDown() || event.isControlDown() || event.isMetaDown() || event.isShiftDown()) {
            return;
        }
        if (event.getCode() != KeyCode.DOWN && event.getCode() != KeyCode.ENTER) {
            return;
        }
        ShowcaseItem firstHit = viewModel.firstHit();
        if (firstHit == null) {
            return;
        }
        if (event.getCode() == KeyCode.DOWN) {
            navigationTree.requestFocus();
        }
        selectPageRow(firstHit);
        navigationTree.scrollTo(navigationTree.getRow(viewModel.navigationNodeOf(firstHit)));
        event.consume();
    }

    /**
     * Enter on a page row, without a modifier: the row the tree's focus is on is selected, which opens its page. On a
     * section or group row Enter passes on and opens nothing. The tree's arrow keys stay its own.
     */
    private void treeKeyPressed(KeyEvent event) {
        if (event.getCode() != KeyCode.ENTER || event.isAltDown() || event.isControlDown() || event.isMetaDown()
                || event.isShiftDown()) {
            return;
        }
        TreeItem<NavigationEntry> focused = navigationTree.getFocusModel().getFocusedItem();
        if (focused != null && focused.getValue() instanceof ShowcaseItem) {
            navigationTree.getSelectionModel().select(focused);
            event.consume();
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
        // A page entering the scene may have added a handler of its own to it: Ctrl+F's comes after it.
        addFindHandler(navigationTree.getScene());

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
