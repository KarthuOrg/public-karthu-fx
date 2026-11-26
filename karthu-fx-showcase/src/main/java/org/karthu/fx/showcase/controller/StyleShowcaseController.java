package org.karthu.fx.showcase.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;

import org.karthu.fx.dialog.CustomDialog;
import org.karthu.fx.showcase.viewmodel.NavigationEntry;
import org.karthu.fx.showcase.viewmodel.ShowcaseItem;
import org.karthu.fx.showcase.viewmodel.StyleShowcaseViewModel;
import org.karthu.fx.sidebar.NavigationTree;
import org.karthu.fx.sidebar.OpenMode;
import org.karthu.fx.sidebar.SideBar;
import org.karthu.fx.sidebar.SideBarTab;
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
    private SideBar sideBar;

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

    /** The navigation: the sections, groups and pages, the content of the side bar's one tab. */
    private final NavigationTree<NavigationEntry> navigation = new NavigationTree<>();

    /** The side bar's one tab, which holds the navigation. */
    private SideBarTab componentsTab;

    private StyleShowcaseViewModel viewModel;
    private final Map<String, Parent> pageCache = new HashMap<>();

    /** Ctrl+F on the window's scene: the focus into the navigation's search, unless a handler before it took the key. */
    private final EventHandler<KeyEvent> findHandler = this::find;

    /** The scene {@link #findHandler} is added to, null while the window has none. */
    private Scene findScene;

    /**
     * Gives the side bar its one tab, the navigation, and binds the scheme switch of the toolbar to the theme's
     * choice, in both directions: a choice on the switch is the choice of every window, and a choice made elsewhere
     * shows on the switch. Called by the FXML loader.
     */
    @FXML
    private void initialize() {
        componentsTab = new SideBarTab("components", "Components", BootstrapIcons.GRID, navigation);
        sideBar.getTabs().add(componentsTab);
        sideBar.setSelectedTab(componentsTab);
        navigation.getSearchInput().setPromptText("Search pages");

        // Ctrl+F anywhere in the window. Inside the navigation the tree takes it itself, to the same end.
        // A handler on the scene, not a command of a CommandRegistry, which is where an application's keys belong
        // otherwise: a scene has one registry, and in the showcase's window that one is the Shortcuts page's, which
        // builds its example application's registry on the window's scene, with a Find of its own on Ctrl+F. A
        // registry of the window's own would leave the page none. So the window adds no registry and its side bar
        // registers no commands; this handler hears Ctrl+F only where neither the focused node nor a page took it
        // (see addFindHandler).
        sideBar.sceneProperty().addListener((obs, oldScene, newScene) -> addFindHandler(newScene));
        addFindHandler(sideBar.getScene());

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

        // A page row shows its icon and name, a section or group row its name. Selecting a page row opens the page; a
        // heading is handed over too and opens nothing, the open page stays.
        navigation.setTextFunction(entry -> entry.text());
        navigation.setIconFunction(entry -> entry instanceof ShowcaseItem page ? page.icon() : null);
        navigation.setOpenMode(OpenMode.SELECTION);
        navigation.setOnOpen(entry -> {
            if (entry instanceof ShowcaseItem page) {
                viewModel.selectItem(page);
            }
        });
        navigation.setRoot(viewModel.getNavigationRoot());

        // The search finds a page by its own, its group's and its section's names, a heading by its own. When the
        // search leaves the tree without a selection, the open page's row is selected again as soon as the tree shows
        // it, during the search and after it; that opens nothing.
        navigation.setSearchTextFunction(entry -> viewModel.searchText(entry));
        navigation.getSearchInput().textProperty().addListener((obs, oldText, newText) -> {
            if (navigation.getSelectedValue() == null && viewModel.getSelectedItem() != null) {
                navigation.select(viewModel.getSelectedItem());
            }
        });

        // ViewModel selection listener: load page when ViewModel selection changes
        viewModel.selectedItemProperty().addListener(
                (obs, oldItem, newItem) -> {
                    if (newItem != null) {
                        loadPage(newItem);
                    }
                }
        );

        // The first page, Buttons, opens at start, its row selected.
        if (!viewModel.getShowcaseItems().isEmpty()) {
            ShowcaseItem first = viewModel.getShowcaseItems().get(0);
            navigation.select(first);
            viewModel.selectItem(first);
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

    /**
     * Ctrl+F that nothing took before: the side bar shows the navigation, expanded if it was collapsed, and the focus
     * goes into the navigation's search, its text selected.
     */
    private void find(KeyEvent event) {
        if (event.isConsumed() || !FIND.match(event)) {
            return;
        }
        sideBar.setSelectedTab(componentsTab);
        sideBar.setCollapsed(false);
        navigation.focusSearch();
        event.consume();
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onAbout() {
        CustomDialog.info(sideBar.getScene().getWindow(),
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
        addFindHandler(sideBar.getScene());

        // Reset scroll to top
        contentScrollPane.setVvalue(0.0);
    }
}
