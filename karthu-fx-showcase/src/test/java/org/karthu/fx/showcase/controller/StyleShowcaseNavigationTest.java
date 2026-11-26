package org.karthu.fx.showcase.controller;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.showcase.viewmodel.NavigationEntry;
import org.karthu.fx.showcase.viewmodel.ShowcaseItem;
import org.karthu.fx.showcase.viewmodel.StyleShowcaseViewModel;
import org.karthu.fx.sidebar.NavigationTree;
import org.karthu.fx.sidebar.OpenMode;
import org.karthu.fx.sidebar.SideBar;
import org.karthu.fx.sidebar.SideBarTab;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The showcase window's navigation, loaded from the window's FXML in a scene that is never shown: a side bar at the
 * window's left edge with one tab, a navigation tree of sections, groups and pages that opens a page when the user
 * selects it, the page it opens at start, headings that open nothing and fold with a click, the search above the tree
 * and the window's Ctrl+F.
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
    @DisplayName("the window's left column is a side bar with one open tab, Components, holding a navigation tree of "
            + "the view model's tree that opens a page on selection")
    void leftColumnIsASideBar() throws IOException {
        Window window = load();

        assertSame(window.sideBar, ((BorderPane) window.root).getLeft());
        assertEquals(Side.LEFT, window.sideBar.getSide());
        assertEquals(1, window.sideBar.getTabs().size());
        SideBarTab tab = window.sideBar.getTabs().get(0);
        assertEquals("Components", tab.getTitle());
        assertNotNull(tab.getIcon(), "the tab's icon on the stripe");
        assertSame(tab, window.sideBar.getSelectedTab());
        assertFalse(window.sideBar.isCollapsed());
        assertSame(window.navigation, tab.getContent());
        assertEquals(OpenMode.SELECTION, window.navigation.getOpenMode());
        assertSame(window.viewModel.getNavigationRoot(), window.navigation.getRoot());
        assertEquals("Search pages", window.search.getPromptText());
    }

    @Test
    @DisplayName("the column has no heading of its own: the side bar's header names the tab, Components")
    void noHeadingOfItsOwn() throws IOException {
        Window window = load();
        laidOut(window);

        Label title = (Label) window.sideBar.lookup(".side-bar-title");
        assertNotNull(title, "the side bar's header title");
        assertEquals("Components", title.getText());
        for (Node node : window.root.lookupAll(".label")) {
            if (node instanceof Label label) {
                assertFalse("COMPONENTS".equals(label.getText()), "the column's old heading");
            }
        }
        assertNull(window.root.lookup(".showcase-sidebar"), "the old column");
    }

    @Test
    @DisplayName("at start the first page, Buttons, is open and selected in the tree, the tree's focus on it")
    void firstPageOpenAtStart() throws IOException {
        Window window = load();

        ShowcaseItem buttons = window.viewModel.getShowcaseItems().get(0);
        assertEquals("Buttons", buttons.displayName());
        assertSame(buttons, window.navigation.getSelectedValue());
        assertSame(buttons, window.tree.getFocusModel().getFocusedItem().getValue());
        assertSame(buttons, window.viewModel.getSelectedItem());
        assertEquals("Buttons", openPageTitle(window));
    }

    @Test
    @DisplayName("the tree shows the sections, their groups and pages in the showcase's order, each page with its "
            + "icon and name, a heading with its name alone")
    void rowsInOrder() throws IOException {
        Window window = load();
        laidOut(window);

        List<String> rows = rows(window);
        assertEquals(List.of("JavaFX", "  Inputs", "    Buttons", "    Text Field"), rows.subList(0, 4));
        assertEquals(55, rows.size(), "3 sections, 8 groups and 44 pages");
        assertEquals("Job Scheduler", window.tree.getTreeItem(54).getValue().text());
        assertEquals(List.of("Buttons", "Text Field", "Text Area"), texts(window, 2, 5));
        assertEquals(List.of("JavaFX", "Inputs"), texts(window, 0, 2));
        assertNotNull(cellOf(window.tree, row(window, "Buttons")).lookup(".navigation-tree-icon"),
                "a page row shows its icon");
        assertNull(cellOf(window.tree, row(window, "Inputs")).lookup(".navigation-tree-icon"),
                "a heading row shows no icon");
    }

    @Test
    @DisplayName("a click on a page row opens its page and folds nothing")
    void clickOnAPageRowOpensIt() throws IOException {
        Window window = load();
        laidOut(window);
        TreeItem<NavigationEntry> textField = row(window, "Text Field");

        click(window, textField, false, 1);

        assertEquals("Text Field", window.viewModel.getSelectedItem().displayName());
        assertEquals("Text Field", openPageTitle(window));
        assertSame(textField.getValue(), window.navigation.getSelectedValue());
        assertTrue(textField.getParent().isExpanded());
    }

    @Test
    @DisplayName("a click on a section or group row selects and folds it, a second click unfolds it; the open page "
            + "stays")
    void clickOnAHeadingRowFoldsIt() throws IOException {
        Window window = load();
        laidOut(window);
        Node openPage = window.contentArea.getChildren().get(0);
        TreeItem<NavigationEntry> selection = row(window, "Selection");

        click(window, selection, false, 1);
        assertFalse(selection.isExpanded(), "the group folded");
        assertSame(selection.getValue(), window.navigation.getSelectedValue());
        assertFalse(rows(window).contains("    ComboBox"));
        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());
        assertSame(openPage, window.contentArea.getChildren().get(0));

        click(window, selection, false, 1);
        assertTrue(selection.isExpanded(), "the group unfolded again");
        assertTrue(rows(window).contains("    ComboBox"));

        TreeItem<NavigationEntry> javaFx = row(window, "JavaFX");
        click(window, javaFx, false, 1);
        assertFalse(javaFx.isExpanded(), "the section folded");
        assertFalse(rows(window).contains("    Buttons"));
        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());
        assertSame(openPage, window.contentArea.getChildren().get(0));
    }

    @Test
    @DisplayName("a click on a heading row's disclosure arrow folds and unfolds it once, and each click of a double "
            + "click on the row folds or unfolds it once")
    void arrowAndDoubleClickOnAHeadingRow() throws IOException {
        Window window = load();
        laidOut(window);
        TreeItem<NavigationEntry> selection = row(window, "Selection");

        click(window, selection, true, 1);
        assertFalse(selection.isExpanded(), "the arrow folded the group");
        click(window, selection, true, 1);
        assertTrue(selection.isExpanded(), "the arrow unfolded the group");

        click(window, selection, false, 1);
        assertFalse(selection.isExpanded());
        click(window, selection, false, 2);
        assertTrue(selection.isExpanded(), "the second click unfolded the group again");
        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());
    }

    @Test
    @DisplayName("the arrow keys: down onto the next page opens it, left and right on a heading fold and unfold it "
            + "and open nothing")
    void arrowKeys() throws IOException {
        Window window = load();
        laidOut(window);

        press(window.tree, KeyCode.DOWN, false);
        assertEquals("Text Field", window.viewModel.getSelectedItem().displayName());
        assertEquals("Text Field", openPageTitle(window));

        TreeItem<NavigationEntry> selection = row(window, "Selection");
        window.navigation.select(selection.getValue());
        press(window.tree, KeyCode.LEFT, false);
        assertFalse(selection.isExpanded(), "Arrow left folded the group");
        press(window.tree, KeyCode.RIGHT, false);
        assertTrue(selection.isExpanded(), "Arrow right unfolded the group");
        assertEquals("Text Field", window.viewModel.getSelectedItem().displayName());
    }

    @Test
    @DisplayName("Enter on a page row opens that page; on a heading row it folds the heading and opens nothing")
    void enterOnARow() throws IOException {
        Window window = load();
        laidOut(window);
        TreeItem<NavigationEntry> inputs = row(window, "Inputs");

        window.tree.getFocusModel().focus(window.tree.getRow(inputs));
        press(window.tree, KeyCode.ENTER, false);
        assertFalse(inputs.isExpanded(), "Enter folded the group");
        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());

        press(window.tree, KeyCode.ENTER, false);
        assertTrue(inputs.isExpanded(), "Enter unfolded the group");
        window.tree.getFocusModel().focus(window.tree.getRow(row(window, "Text Area")));
        press(window.tree, KeyCode.ENTER, false);
        assertEquals("Text Area", window.viewModel.getSelectedItem().displayName());
        assertEquals("Text Area", window.navigation.getSelectedValue().text());
        assertEquals("Text Area", openPageTitle(window));
    }

    @Test
    @DisplayName("typing narrows the tree to the hits under their sections and groups, all unfolded; several words "
            + "narrow further, across a page's name and its group's: \"form path\" leaves the Path Input")
    void typingNarrowsTheTree() throws IOException {
        Window window = load();
        laidOut(window);

        window.search.setText("form path");
        window.root.layout();

        assertEquals(List.of("karthu-fx", "  Form Inputs", "    Path Input"), rows(window));
    }

    @Test
    @DisplayName("a group's name finds all its pages and a section's name all of its own: \"form\" leaves every Form "
            + "Input page, \"karthu\" every karthu-fx page")
    void headingNamesFindTheirPages() throws IOException {
        Window window = load();
        laidOut(window);

        window.search.setText("form");
        assertEquals(List.of("karthu-fx", "  Form Inputs",
                "    Overview", "    Text Input", "    Password Input", "    Number Input", "    Combo Input",
                "    Editable Combo Input", "    Text Area Input", "    Path Input", "    Cron Input", "    Search Input"),
                rows(window));

        window.search.setText("karthu");
        List<String> rows = rows(window);
        assertEquals("karthu-fx", rows.get(0));
        assertEquals(23, rows.size(), "the section, its 3 groups and its 19 pages");
        assertEquals("  Icons", rows.get(rows.size() - 1));
    }

    @Test
    @DisplayName("the counter counts every matching entry, headings included, out of all entries")
    void counterCountsEveryMatchingEntry() throws IOException {
        Window window = load();
        laidOut(window);

        window.search.setText("dialog");
        window.root.applyCss();
        window.root.layout();

        Label counter = (Label) window.search.lookup(".form-search-count");
        assertNotNull(counter, "the search's counter");
        assertEquals("3 of 55", counter.getText(), "the group Dialogs, Dialog / Alert and Settings Dialog");
    }

    @Test
    @DisplayName("\"No matches\" shows while nothing matches and goes when something does")
    void noMatches() throws IOException {
        Window window = load();
        laidOut(window);
        Label noMatches = (Label) window.navigation.lookup(".navigation-tree-no-matches");
        assertNotNull(noMatches, "the no-matches label");
        assertFalse(noMatches.isVisible());

        window.search.setText("tabpanes");
        assertTrue(noMatches.isVisible());
        assertEquals("No matches", noMatches.getText());
        assertEquals(List.of(), rows(window));

        window.search.setText("tabpane");
        assertFalse(noMatches.isVisible());
        assertEquals(List.of("JavaFX", "  Layout", "    TabPane"), rows(window));
    }

    @Test
    @DisplayName("clearing the search gives the tree back folded as the user left it, the open page's row selected")
    void clearingKeepsTheUsersFolding() throws IOException {
        Window window = load();
        laidOut(window);
        TreeItem<NavigationEntry> selection = row(window, "Selection");
        TreeItem<NavigationEntry> karthuFx = row(window, "karthu-fx");
        click(window, selection, false, 1);
        click(window, karthuFx, false, 1);
        selectPage(window, "Text Field");

        window.search.setText("combo");
        assertEquals(List.of("JavaFX", "  Selection", "    ComboBox", "karthu-fx", "  Form Inputs", "    Combo Input",
                "    Editable Combo Input"), rows(window), "the search finds pages in folded headings");

        window.search.setText("");
        assertFalse(selection.isExpanded(), "Selection is folded again");
        assertFalse(karthuFx.isExpanded(), "karthu-fx is folded again");
        assertTrue(rows(window).contains("  Selection"));
        assertFalse(rows(window).contains("    ComboBox"));
        assertSame(page(window, "Text Field"), window.navigation.getSelectedValue());
        assertEquals("Text Field", openPageTitle(window));
    }

    @Test
    @DisplayName("the open page stays open while the search hides its row, and its row is selected again once the "
            + "tree shows it, during the search and after it")
    void openPageSurvivesTheSearch() throws IOException {
        Window window = load();
        laidOut(window);
        selectPage(window, "TabPane");
        Node openPage = window.contentArea.getChildren().get(0);

        window.search.setText("form");
        assertNull(window.navigation.getSelectedValue(), "no row selected while the page is hidden");
        assertEquals("TabPane", window.viewModel.getSelectedItem().displayName());
        assertSame(openPage, window.contentArea.getChildren().get(0));

        window.search.setText("tab");
        assertSame(window.viewModel.getSelectedItem(), window.navigation.getSelectedValue());
        assertSame(openPage, window.contentArea.getChildren().get(0));

        window.search.setText("form");
        window.search.setText("");
        assertSame(window.viewModel.getSelectedItem(), window.navigation.getSelectedValue());
        assertSame(openPage, window.contentArea.getChildren().get(0));
    }

    @Test
    @DisplayName("Esc in the search field clears it and brings the whole tree back")
    void escClearsTheSearch() throws IOException {
        Window window = load();
        laidOut(window);
        window.search.setText("dialog");

        press(window.search.getTextField(), KeyCode.ESCAPE, false);

        assertEquals("", window.search.getText());
        assertEquals(55, rows(window).size());
    }

    @Test
    @DisplayName("Arrow down in the search field moves the focus to the tree and selects the first hit, which opens")
    void arrowDownSelectsTheFirstHit() throws IOException {
        Window window = load();
        laidOut(window);
        window.search.setText("tab");

        press(window.search.getTextField(), KeyCode.DOWN, false);

        assertSame(window.tree, window.root.getScene().getFocusOwner());
        assertEquals("TableView", window.navigation.getSelectedValue().text());
        assertEquals("TableView", window.viewModel.getSelectedItem().displayName());
        assertEquals("TableView", openPageTitle(window));
        assertEquals("tab", window.search.getText(), "the search stays");
    }

    @Test
    @DisplayName("Enter in the search field opens the first hit; without hits it opens nothing")
    void enterInTheSearchOpensTheFirstHit() throws IOException {
        Window window = load();
        laidOut(window);

        window.search.setText("zebra");
        press(window.search.getTextField(), KeyCode.ENTER, false);
        assertEquals("Buttons", openPageTitle(window));

        window.search.setText("layout split");
        press(window.search.getTextField(), KeyCode.ENTER, false);
        assertEquals("SplitPane", window.viewModel.getSelectedItem().displayName());
        assertEquals("SplitPane", window.navigation.getSelectedValue().text());
        assertEquals("SplitPane", openPageTitle(window));
        assertSame(window.search.getTextField(), window.root.getScene().getFocusOwner(), "the focus stays");
    }

    @Test
    @DisplayName("Enter in the search field on a first hit that is a heading selects the heading and opens nothing")
    void enterOnAHeadingHitOpensNothing() throws IOException {
        Window window = load();
        laidOut(window);

        window.search.setText("dialog");
        press(window.search.getTextField(), KeyCode.ENTER, false);

        assertEquals("Dialogs", window.navigation.getSelectedValue().text());
        assertEquals("Buttons", window.viewModel.getSelectedItem().displayName());
        assertEquals("Buttons", openPageTitle(window));
    }

    @Test
    @DisplayName("Ctrl+F anywhere in the window puts the focus into the navigation's search and selects its text")
    void ctrlFFocusesTheSearch() throws IOException {
        Window window = load();
        laidOut(window);
        window.search.setText("tab");
        Node pageButton = window.contentArea.lookup(".button");
        assertNotNull(pageButton, "a button of the open page");

        press(pageButton, KeyCode.F, true);

        TextField text = window.search.getTextField();
        assertSame(text, window.root.getScene().getFocusOwner());
        assertEquals("tab", text.getSelectedText());
    }

    @Test
    @DisplayName("Ctrl+F while the side bar is collapsed expands it and puts the focus into the navigation's search")
    void ctrlFExpandsTheSideBar() throws IOException {
        Window window = load();
        laidOut(window);
        Node pageButton = window.contentArea.lookup(".button");
        window.sideBar.setCollapsed(true);
        window.root.layout();

        press(pageButton, KeyCode.F, true);

        assertFalse(window.sideBar.isCollapsed());
        assertSame(window.sideBar.getTabs().get(0), window.sideBar.getSelectedTab());
        assertSame(window.search.getTextField(), window.root.getScene().getFocusOwner());
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

        Label lastCommand = (Label) window.root.lookup("#lastCommandLabel");
        assertTrue(lastCommand.getText().contains("(search.find)"), lastCommand.getText());
        assertSame(toolbar, window.root.getScene().getFocusOwner());
    }

    @Test
    @DisplayName("Ctrl+F reaches the search again once another page than Shortcuts is open")
    void ctrlFAfterLeavingTheShortcutsPage() throws IOException {
        Window window = load();
        laidOut(window);
        selectPage(window, "Shortcuts");
        selectPage(window, "Buttons");
        Node pageButton = window.contentArea.lookup(".button");

        press(pageButton, KeyCode.F, true);

        assertSame(window.search.getTextField(), window.root.getScene().getFocusOwner());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /**
     * Clicks the primary button on the row of a tree item, as the platform sends it: pressed, released, clicked. The
     * click lands on the row's disclosure arrow, or on the row's right end, away from the arrow.
     */
    private static void click(Window window, TreeItem<NavigationEntry> item, boolean onArrow, int clickCount) {
        window.root.applyCss();
        window.root.layout();
        TreeCell<NavigationEntry> cell = cellOf(window.tree, item);
        Node target = cell;
        double x;
        double y;
        if (onArrow) {
            target = cell.getDisclosureNode();
            assertNotNull(target, "the row's disclosure arrow");
            Bounds arrow = target.localToScene(target.getLayoutBounds());
            x = arrow.getCenterX();
            y = arrow.getCenterY();
        } else {
            Bounds row = cell.localToScene(cell.getLayoutBounds());
            x = row.getMaxX() - 4;
            y = row.getCenterY();
        }
        fireMouse(target, MouseEvent.MOUSE_PRESSED, x, y, clickCount);
        fireMouse(target, MouseEvent.MOUSE_RELEASED, x, y, clickCount);
        fireMouse(target, MouseEvent.MOUSE_CLICKED, x, y, clickCount);
        window.root.applyCss();
        window.root.layout();
    }

    private static void fireMouse(Node target, EventType<MouseEvent> type, double x, double y, int clickCount) {
        boolean primaryDown = type == MouseEvent.MOUSE_PRESSED;
        Event.fireEvent(target, new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, clickCount, false, false, false,
                false, primaryDown, false, false, false, false, true, null));
    }

    /** The visible cell that shows a tree item. */
    @SuppressWarnings("unchecked")
    private static TreeCell<NavigationEntry> cellOf(TreeView<NavigationEntry> tree, TreeItem<NavigationEntry> item) {
        for (Node candidate : tree.lookupAll(".tree-cell")) {
            if (candidate instanceof TreeCell<?> cell && cell.getTreeItem() == item && cell.isVisible()) {
                return (TreeCell<NavigationEntry>) cell;
            }
        }
        throw new AssertionError("no cell shows " + item.getValue().text());
    }

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
    private record Window(Parent root, StyleShowcaseViewModel viewModel, SideBar sideBar,
                          NavigationTree<NavigationEntry> navigation, TreeView<NavigationEntry> tree,
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
        SideBar sideBar = assertInstanceOf(SideBar.class, ((BorderPane) root).getLeft(), "the window's side bar");
        assertFalse(sideBar.getTabs().isEmpty(), "the side bar's tab");
        NavigationTree<NavigationEntry> navigation = assertInstanceOf(NavigationTree.class,
                sideBar.getTabs().get(0).getContent(), "the navigation tree");
        StackPane contentArea = (StackPane) ((ScrollPane) root.lookup("#contentScrollPane")).getContent();
        assertNotNull(contentArea, "the content area");
        return new Window(root, viewModel, sideBar, navigation, navigation.getTreeView(), contentArea,
                navigation.getSearchInput());
    }

    /** Puts the window into a scene that is never shown, in the theme, laid out tall enough for every row. */
    private static Scene laidOut(Window window) {
        Scene scene = new Scene(window.root, 1200, 3000);
        ThemeManager.apply(scene);
        window.root.applyCss();
        window.root.layout();
        return scene;
    }

    /** The page of the given name. */
    private static ShowcaseItem page(Window window, String name) {
        for (ShowcaseItem page : window.viewModel.getShowcaseItems()) {
            if (page.displayName().equals(name)) {
                return page;
            }
        }
        throw new AssertionError("no page " + name);
    }

    /** Opens the named page as the user does: a click on its row. */
    private static void selectPage(Window window, String name) {
        click(window, row(window, name), false, 1);
        assertEquals(name, window.viewModel.getSelectedItem().displayName());
    }

    /** The item of the shown tree that holds the entry of the given text, sections and groups included. */
    private static TreeItem<NavigationEntry> row(Window window, String text) {
        for (int index = 0; index < window.tree.getExpandedItemCount(); index++) {
            TreeItem<NavigationEntry> item = window.tree.getTreeItem(index);
            if (item.getValue().text().equals(text)) {
                return item;
            }
        }
        throw new AssertionError("no row " + text);
    }

    /** The texts of the rows the tree shows, top to bottom, two spaces per level below the hidden root. */
    private static List<String> rows(Window window) {
        List<String> rows = new ArrayList<>();
        for (int index = 0; index < window.tree.getExpandedItemCount(); index++) {
            TreeItem<NavigationEntry> item = window.tree.getTreeItem(index);
            rows.add("  ".repeat(window.tree.getTreeItemLevel(item) - 1) + item.getValue().text());
        }
        return rows;
    }

    /**
     * The texts the cells of the given rows show, from the first row to the one before the last: the runs of each
     * row's text node, so that the text is what the user reads.
     */
    private static List<String> texts(Window window, int from, int to) {
        List<String> texts = new ArrayList<>();
        for (int index = from; index < to; index++) {
            TreeCell<NavigationEntry> cell = cellOf(window.tree, window.tree.getTreeItem(index));
            TextFlow text = (TextFlow) cell.lookup(".navigation-tree-text");
            assertNotNull(text, "the text of row " + index);
            StringBuilder shown = new StringBuilder();
            for (Node run : text.getChildren()) {
                shown.append(((Text) run).getText());
            }
            texts.add(shown.toString());
        }
        return texts;
    }

    private static String openPageTitle(Window window) {
        assertEquals(1, window.contentArea.getChildren().size(), "one page open");
        Label title = (Label) window.contentArea.getChildren().get(0).lookup(".showcase-page-title");
        assertNotNull(title, "the open page's title");
        return title.getText();
    }
}
