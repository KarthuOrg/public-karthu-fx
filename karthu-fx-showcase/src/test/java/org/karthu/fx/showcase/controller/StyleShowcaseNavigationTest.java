package org.karthu.fx.showcase.controller;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.showcase.viewmodel.NavigationEntry;
import org.karthu.fx.showcase.viewmodel.ShowcaseItem;
import org.karthu.fx.showcase.viewmodel.StyleShowcaseViewModel;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The showcase window's navigation, loaded from the window's FXML in a scene that is never shown: the tree of sections,
 * groups and pages in the sidebar, the page it opens at start, heading rows that open nothing, and the search above
 * the tree that narrows it.
 */
class StyleShowcaseNavigationTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @Test
    @DisplayName("the sidebar is a tree with a hidden root under the COMPONENTS heading")
    void sidebarIsATree() throws IOException {
        Window window = load();

        assertFalse(window.tree.isShowRoot());
        assertSame(window.viewModel.getNavigationRoot(), window.tree.getRoot());
        VBox sidebar = (VBox) window.root.lookup(".showcase-sidebar");
        Label header = (Label) sidebar.getChildren().get(0);
        assertEquals("COMPONENTS", header.getText());
        assertTrue(sidebar.getChildren().contains(window.tree.getParent()), "the tree sits in the sidebar");
    }

    @Test
    @DisplayName("at start the first page, Buttons, is selected in the tree and open")
    void firstPageOpenAtStart() throws IOException {
        Window window = load();

        ShowcaseItem buttons = window.viewModel.getShowcaseItems().get(0);
        assertEquals("Buttons", buttons.displayName());
        assertSame(buttons, window.tree.getSelectionModel().getSelectedItem().getValue());
        assertSame(buttons, window.viewModel.getSelectedItem());
        assertEquals("Buttons", openPageTitle(window));
    }

    @Test
    @DisplayName("selecting a section or group row opens nothing and keeps the open page; a page row opens its page")
    void headingRowsOpenNothing() throws IOException {
        Window window = load();
        Node openPage = window.contentArea.getChildren().get(0);

        TreeItem<NavigationEntry> javaFx = window.tree.getRoot().getChildren().get(0);
        window.tree.getSelectionModel().select(javaFx);
        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());
        assertSame(openPage, window.contentArea.getChildren().get(0));

        TreeItem<NavigationEntry> selection = javaFx.getChildren().get(1);
        window.tree.getSelectionModel().select(selection);
        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());
        assertSame(openPage, window.contentArea.getChildren().get(0));

        window.tree.getSelectionModel().select(javaFx.getChildren().get(0).getChildren().get(1));
        assertEquals("Text Field", window.viewModel.getSelectedItem().displayName());
        assertEquals("Text Field", openPageTitle(window));
    }

    @Test
    @DisplayName("heading rows carry their own style classes; page rows keep their icon and name")
    void rowsLaidOut() throws IOException {
        Window window = load();
        laidOut(window);

        assertEquals(List.of("JavaFX", "karthu-fx", "Examples"), texts(window.tree, ".navigation-section-header"));
        assertEquals(List.of("Inputs", "Selection", "Data Display", "Layout", "Navigation", "Form Inputs", "Components",
                "Dialogs"), texts(window.tree, ".navigation-group-header"));
        List<String> pages = texts(window.tree, ".sidebar-item-label");
        assertEquals(43, pages.size());
        assertEquals("Buttons", pages.get(0));
        assertEquals(43, window.tree.lookupAll(".sidebar-item-icon").size());

        // The showcase's navigation sheet is applied: a section heading larger than a page name, a group heading smaller.
        Label section = (Label) window.tree.lookup(".navigation-section-header");
        Label group = (Label) window.tree.lookup(".navigation-group-header");
        assertEquals(13, section.getFont().getSize(), 0.01);
        assertEquals(11, group.getFont().getSize(), 0.01);
    }

    @Test
    @DisplayName("a search field with an English prompt sits above the tree in the sidebar")
    void searchAboveTheTree() throws IOException {
        Window window = load();

        VBox sidebar = (VBox) window.root.lookup(".showcase-sidebar");
        int search = sidebar.getChildren().indexOf(window.search);
        assertTrue(search >= 0, "the search sits in the sidebar");
        int tree = sidebar.getChildren().indexOf(window.tree.getParent());
        assertTrue(search < tree, "the search sits above the tree");
        assertEquals("Search pages", window.search.getPromptText());
    }

    @Test
    @DisplayName("typing narrows the tree to the hits, under their sections and groups, all expanded")
    void typingNarrowsTheTree() throws IOException {
        Window window = load();
        laidOut(window);

        window.search.setText("form path");
        window.root.layout();

        assertEquals(List.of("karthu-fx"), texts(window.tree, ".navigation-section-header"));
        assertEquals(List.of("Form Inputs"), texts(window.tree, ".navigation-group-header"));
        assertEquals(List.of("Path Input"), texts(window.tree, ".sidebar-item-label"));
    }

    @Test
    @DisplayName("\"No matches\" shows while no page is left and goes when one is")
    void noMatches() throws IOException {
        Window window = load();
        laidOut(window);
        Label noMatches = (Label) window.root.lookup(".navigation-no-matches");
        assertNotNull(noMatches, "the no-matches label");
        assertEquals("No matches", noMatches.getText());
        assertFalse(noMatches.isVisible());

        window.search.setText("tabpanes");
        window.root.layout();
        assertTrue(noMatches.isVisible());
        assertEquals(List.of(), texts(window.tree, ".sidebar-item-label"));

        window.search.setText("tabpane");
        window.root.layout();
        assertFalse(noMatches.isVisible());
        assertEquals(List.of("TabPane"), texts(window.tree, ".sidebar-item-label"));
    }

    @Test
    @DisplayName("the open page stays open while its row is hidden; its row is selected again when it is among the hits")
    void openPageSurvivesTheSearch() throws IOException {
        Window window = load();
        selectPage(window, "TabPane");
        Node openPage = window.contentArea.getChildren().get(0);

        window.search.setText("form");
        assertNull(window.tree.getSelectionModel().getSelectedItem(), "no row selected while the page is hidden");
        assertEquals("TabPane", window.viewModel.getSelectedItem().displayName());
        assertSame(openPage, window.contentArea.getChildren().get(0));

        window.search.setText("tab");
        assertSame(window.viewModel.getSelectedItem(), window.tree.getSelectionModel().getSelectedItem().getValue());
        assertSame(openPage, window.contentArea.getChildren().get(0));
    }

    @Test
    @DisplayName("clearing the search restores the full, expanded tree with the open page's row selected")
    void clearingRestoresTheTree() throws IOException {
        Window window = load();
        laidOut(window);
        selectPage(window, "Text Field");

        window.search.setText("dialog");
        window.root.layout();
        assertEquals(2, texts(window.tree, ".sidebar-item-label").size());

        window.search.setText("");
        window.root.layout();
        assertEquals(43, texts(window.tree, ".sidebar-item-label").size());
        assertEquals(List.of("JavaFX", "karthu-fx", "Examples"), texts(window.tree, ".navigation-section-header"));
        assertEquals("Text Field", window.tree.getSelectionModel().getSelectedItem().getValue().text());
        assertEquals("Text Field", openPageTitle(window));
    }

    @Test
    @DisplayName("Ctrl+F puts the focus into the search field and selects its text")
    void ctrlFFocusesTheSearch() throws IOException {
        Window window = load();
        laidOut(window);
        window.search.setText("tab");
        window.tree.requestFocus();

        press(window.tree, KeyCode.F, true);

        TextField text = window.search.getTextField();
        assertSame(text, window.root.getScene().getFocusOwner());
        assertEquals("tab", text.getSelectedText());
    }

    @Test
    @DisplayName("a node that consumes Ctrl+F keeps it: the focus stays where it is")
    void ctrlFConsumedByANodeStaysThere() throws IOException {
        Window window = load();
        laidOut(window);
        TextField pageField = new TextField();
        pageField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F && event.isControlDown()) {
                event.consume();
            }
        });
        window.contentArea.getChildren().setAll(pageField);

        press(pageField, KeyCode.F, true);

        assertSame(pageField, window.root.getScene().getFocusOwner());
    }

    @Test
    @DisplayName("on the Shortcuts page Ctrl+F stays the demo's: its Find runs and the focus stays in the page")
    void ctrlFOnTheShortcutsPageStaysTheDemos() throws IOException {
        Window window = load();
        laidOut(window);
        selectPage(window, "Shortcuts");
        window.root.applyCss();
        window.root.layout();
        Node toolbar = window.root.lookup("#exampleToolbar");
        assertNotNull(toolbar, "the demo's toolbar");

        press(toolbar, KeyCode.F, true);

        assertTrue(((Label) window.root.lookup("#lastCommandLabel")).getText().contains("(search.find)"),
                ((Label) window.root.lookup("#lastCommandLabel")).getText());
        assertSame(toolbar, window.root.getScene().getFocusOwner());
    }

    @Test
    @DisplayName("Ctrl+F reaches the search again once another page than Shortcuts is open")
    void ctrlFAfterLeavingTheShortcutsPage() throws IOException {
        Window window = load();
        laidOut(window);
        selectPage(window, "Shortcuts");
        selectPage(window, "Buttons");

        press(window.tree, KeyCode.F, true);

        assertSame(window.search.getTextField(), window.root.getScene().getFocusOwner());
    }

    @Test
    @DisplayName("Esc in the search field clears it and brings the full tree back")
    void escClearsTheSearch() throws IOException {
        Window window = load();
        laidOut(window);
        window.search.setText("dialog");
        window.root.layout();

        press(window.search.getTextField(), KeyCode.ESCAPE, false);
        window.root.layout();

        assertEquals("", window.search.getText());
        assertEquals(43, texts(window.tree, ".sidebar-item-label").size());
    }

    @Test
    @DisplayName("Arrow down in the search field moves the focus to the tree and selects the first hit, which opens")
    void arrowDownSelectsTheFirstHit() throws IOException {
        Window window = load();
        laidOut(window);
        window.search.setText("dialog");

        press(window.search.getTextField(), KeyCode.DOWN, false);

        assertSame(window.tree, window.root.getScene().getFocusOwner());
        assertEquals("Dialog / Alert", window.tree.getSelectionModel().getSelectedItem().getValue().text());
        assertEquals("Dialog / Alert", window.viewModel.getSelectedItem().displayName());
        assertEquals("dialog", window.search.getText(), "the search stays");
    }

    @Test
    @DisplayName("Enter in the search field opens the first hit")
    void enterInTheSearchOpensTheFirstHit() throws IOException {
        Window window = load();
        laidOut(window);
        window.search.setText("form path");

        press(window.search.getTextField(), KeyCode.ENTER, false);

        assertEquals("Path Input", window.viewModel.getSelectedItem().displayName());
        assertEquals("Path Input", window.tree.getSelectionModel().getSelectedItem().getValue().text());
        assertEquals("Path Input", openPageTitle(window));
    }

    @Test
    @DisplayName("Enter in the search field without hits opens nothing")
    void enterWithoutHitsOpensNothing() throws IOException {
        Window window = load();
        laidOut(window);
        window.search.setText("zebra");

        press(window.search.getTextField(), KeyCode.ENTER, false);

        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());
        assertEquals("Buttons", openPageTitle(window));
    }

    @Test
    @DisplayName("Enter on a page row opens that page; on a heading row it opens nothing")
    void enterOnARow() throws IOException {
        Window window = load();
        laidOut(window);
        TreeItem<NavigationEntry> javaFx = window.tree.getRoot().getChildren().get(0);
        TreeItem<NavigationEntry> inputs = javaFx.getChildren().get(0);

        window.tree.getFocusModel().focus(window.tree.getRow(inputs));
        press(window.tree, KeyCode.ENTER, false);
        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());

        window.tree.getFocusModel().focus(window.tree.getRow(inputs.getChildren().get(2)));
        press(window.tree, KeyCode.ENTER, false);
        assertEquals("Text Area", window.viewModel.getSelectedItem().displayName());
        assertEquals("Text Area", window.tree.getSelectionModel().getSelectedItem().getValue().text());
        assertEquals("Text Area", openPageTitle(window));
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /**
     * Gives a node the focus and presses a key there, as the platform sends it on Windows and Linux: Ctrl is the
     * Control key.
     */
    private static void press(Node node, KeyCode code, boolean ctrl) {
        node.requestFocus();
        assertSame(node, node.getScene().getFocusOwner(), "the node did not take the focus");
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, ctrl, false, false));
    }

    /** The loaded window: its root, its view model and the parts the tests look at. */
    private record Window(Parent root, StyleShowcaseViewModel viewModel, TreeView<NavigationEntry> tree,
                          StackPane contentArea, SearchInput search) {
    }

    @SuppressWarnings("unchecked")
    private static Window load() throws IOException {
        URL fxml = StyleShowcaseNavigationTest.class.getResource("/org/karthu/fx/showcase/style-showcase.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        StyleShowcaseController controller = loader.getController();
        controller.initViewModel(viewModel);
        TreeView<NavigationEntry> tree = (TreeView<NavigationEntry>) root.lookup("#navigationTree");
        assertNotNull(tree, "the navigation tree");
        StackPane contentArea = (StackPane) ((ScrollPane) root.lookup("#contentScrollPane")).getContent();
        assertNotNull(contentArea, "the content area");
        SearchInput search = (SearchInput) root.lookup("#navigationSearch");
        assertNotNull(search, "the navigation's search");
        return new Window(root, viewModel, tree, contentArea, search);
    }

    /** Puts the window into a scene that is never shown, in the theme, laid out tall enough for every row. */
    private static Scene laidOut(Window window) {
        Scene scene = new Scene(window.root, 1200, 3000);
        ThemeManager.apply(scene);
        window.root.applyCss();
        window.root.layout();
        return scene;
    }

    /** Selects the row of the named page, which opens it. */
    private static void selectPage(Window window, String name) {
        for (ShowcaseItem page : window.viewModel.getShowcaseItems()) {
            if (page.displayName().equals(name)) {
                window.tree.getSelectionModel().select(window.viewModel.navigationNodeOf(page));
            }
        }
        assertEquals(name, window.viewModel.getSelectedItem().displayName());
    }

    private static String openPageTitle(Window window) {
        assertEquals(1, window.contentArea.getChildren().size(), "one page open");
        Label title = (Label) window.contentArea.getChildren().get(0).lookup(".showcase-page-title");
        assertNotNull(title, "the open page's title");
        return title.getText();
    }

    /** The texts of the labels with the given style class, in the order of the tree's rows. */
    private static List<String> texts(TreeView<NavigationEntry> tree, String selector) {
        List<Label> labels = new ArrayList<>();
        for (Node node : tree.lookupAll(selector)) {
            if (node instanceof Label label && label.getParent() != null && node.isVisible()) {
                labels.add(label);
            }
        }
        labels.sort((first, second) -> Double.compare(
                first.localToScene(0, 0).getY(), second.localToScene(0, 0).getY()));
        List<String> texts = new ArrayList<>();
        for (Label label : labels) {
            texts.add(label.getText());
        }
        return texts;
    }
}
