package org.karthu.fx.showcase.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The showcase window's navigation, loaded from the window's FXML in a scene that is never shown: the tree of sections,
 * groups and pages in the sidebar, the page it opens at start, and heading rows that open nothing.
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
        assertTrue(sidebar.getChildren().contains(window.tree), "the tree sits in the sidebar");
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
        Scene scene = new Scene(window.root, 1200, 3000);
        ThemeManager.apply(scene);
        window.root.applyCss();
        window.root.layout();

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

    // --- Helpers -------------------------------------------------------------------------------------

    /** The loaded window: its root, its view model and the parts the tests look at. */
    private record Window(Parent root, StyleShowcaseViewModel viewModel, TreeView<NavigationEntry> tree,
                          StackPane contentArea) {
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
        return new Window(root, viewModel, tree, contentArea);
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
