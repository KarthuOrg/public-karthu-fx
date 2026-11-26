package org.karthu.fx.showcase.viewmodel;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.SearchInput;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The showcase's navigation as the view model builds it, without a window: its pages, the tree of sections, groups
 * and pages over them, and the search that narrows the tree. The search is karthu-fx's own field, never shown; the
 * toolkit runs only because the field is a control.
 */
class StyleShowcaseViewModelTest {

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
    @DisplayName("the tree holds the sections in order, their groups before their directly attached pages, pages in list order")
    void treeFollowsTheTable() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        List<String> expected = List.of(
                "JavaFX",
                "  Inputs",
                "    Buttons", "    Text Field", "    Text Area", "    Checkbox", "    Radio Button", "    Hyperlink",
                "  Selection",
                "    ComboBox", "    ChoiceBox", "    DatePicker", "    Spinner", "    Slider", "    ColorPicker",
                "  Data Display",
                "    TableView", "    ListView", "    TreeView", "    TreeTableView",
                "  Layout",
                "    Accordion", "    TabPane", "    SplitPane", "    ScrollPane",
                "  Navigation",
                "    Pagination", "    Menu Bar", "    Context Menu",
                "  Progress",
                "karthu-fx",
                "  Form Inputs",
                "    Overview", "    Text Input", "    Password Input", "    Number Input", "    Combo Input",
                "    Editable Combo Input", "    Text Area Input", "    Path Input", "    Cron Input", "    Search Input",
                "  Components",
                "    Toggle Switch", "    Twin Column Selector", "    Key-Value Editor", "    Side Bar",
                "  Dialogs",
                "    Dialog / Alert", "    Settings Dialog",
                "  Markdown", "  Shortcuts", "  Icons",
                "Examples",
                "  Job Scheduler");
        assertEquals(expected, outline(viewModel.getNavigationRoot()));
    }

    @Test
    @DisplayName("sections and groups are headings, the leaves are the pages")
    void headingsAndPages() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        TreeItem<NavigationEntry> javaFx = viewModel.getNavigationRoot().getChildren().get(0);
        assertEquals(new SectionHeading(ShowcaseSection.JAVAFX), javaFx.getValue());
        TreeItem<NavigationEntry> inputs = javaFx.getChildren().get(0);
        assertEquals(new GroupHeading(ShowcaseSection.JAVAFX, "Inputs"), inputs.getValue());
        assertSame(viewModel.getShowcaseItems().get(0), inputs.getChildren().get(0).getValue());

        TreeItem<NavigationEntry> progress = javaFx.getChildren().get(javaFx.getChildren().size() - 1);
        ShowcaseItem page = (ShowcaseItem) progress.getValue();
        assertEquals("Progress", page.displayName());
        assertNull(page.group(), "a directly attached page has no group");
        assertTrue(progress.isLeaf());
    }

    @Test
    @DisplayName("every node of the tree is expanded, the hidden root too")
    void everyNodeExpanded() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        List<String> collapsed = new ArrayList<>();
        collectCollapsed(viewModel.getNavigationRoot(), collapsed);
        assertEquals(List.of(), collapsed);
    }

    @Test
    @DisplayName("every page names its section, a page file of the showcase and an icon; names and files are unique")
    void pagesAsToday() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        assertEquals(44, viewModel.getShowcaseItems().size());
        Set<String> names = new HashSet<>();
        Set<String> files = new HashSet<>();
        for (ShowcaseItem item : viewModel.getShowcaseItems()) {
            assertNotNull(item.section(), item.displayName());
            assertNotNull(item.icon(), item.displayName());
            URL page = StyleShowcaseViewModel.class.getResource("/org/karthu/fx/showcase/showcase/" + item.fxmlFileName());
            assertNotNull(page, item.fxmlFileName());
            assertTrue(names.add(item.displayName()), "twice: " + item.displayName());
            assertTrue(files.add(item.fxmlFileName()), "twice: " + item.fxmlFileName());
        }
    }

    @Test
    @DisplayName("the node of a page is found in the tree, the first page's under JavaFX and Inputs")
    void nodeOfAPage() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        ShowcaseItem buttons = viewModel.getShowcaseItems().get(0);

        TreeItem<NavigationEntry> node = viewModel.navigationNodeOf(buttons);

        assertNotNull(node);
        assertSame(buttons, node.getValue());
        assertEquals(new GroupHeading(ShowcaseSection.JAVAFX, "Inputs"), node.getParent().getValue());
    }

    @Test
    @DisplayName("the tree follows the filtered pages: a section or group without a page among them is left out")
    void treeFollowsTheFilteredPages() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        TreeItem<NavigationEntry> root = viewModel.getNavigationRoot();

        viewModel.getFilteredItems().setPredicate(item -> item.displayName().contains("Tab"));
        assertEquals(List.of("JavaFX", "  Data Display", "    TableView", "    TreeTableView", "  Layout", "    TabPane"),
                outline(root));
        List<String> collapsed = new ArrayList<>();
        collectCollapsed(root, collapsed);
        assertEquals(List.of(), collapsed);

        viewModel.getFilteredItems().setPredicate(null);
        assertSame(root, viewModel.getNavigationRoot(), "the root stays the same node");
        assertEquals(44, leaves(root));
    }

    @Test
    @DisplayName("the open page does not change when the tree is rebuilt")
    void openPageUnaffectedByTheTree() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        ShowcaseItem buttons = viewModel.getShowcaseItems().get(0);
        viewModel.selectItem(buttons);

        viewModel.getFilteredItems().setPredicate(item -> item.section() == ShowcaseSection.EXAMPLES);

        assertSame(buttons, viewModel.getSelectedItem());
        assertNull(viewModel.navigationNodeOf(buttons), "its row is not in the tree");
    }

    @Test
    @DisplayName("\"tab\" leaves the pages with \"tab\" in their name, under their sections and groups")
    void searchTab() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        List<String> tree = search(viewModel, "tab");

        // "Editable" holds "tab" too: karthu-fx's rule matches every contiguous part of a word.
        assertEquals(List.of("JavaFX", "  Data Display", "    TableView", "    TreeTableView", "  Layout", "    TabPane",
                "karthu-fx", "  Form Inputs", "    Editable Combo Input"), tree);
    }

    @Test
    @DisplayName("\"form\" leaves every Form Input page through the group's name")
    void searchForm() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        List<String> tree = search(viewModel, "form");

        assertEquals(List.of("karthu-fx", "  Form Inputs",
                "    Overview", "    Text Input", "    Password Input", "    Number Input", "    Combo Input",
                "    Editable Combo Input", "    Text Area Input", "    Path Input", "    Cron Input", "    Search Input"),
                tree);
    }

    @Test
    @DisplayName("\"karthu\" leaves every karthu-fx page through the section's name")
    void searchKarthu() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        List<String> tree = search(viewModel, "karthu");

        assertEquals(List.of("karthu-fx",
                "  Form Inputs",
                "    Overview", "    Text Input", "    Password Input", "    Number Input", "    Combo Input",
                "    Editable Combo Input", "    Text Area Input", "    Path Input", "    Cron Input", "    Search Input",
                "  Components",
                "    Toggle Switch", "    Twin Column Selector", "    Key-Value Editor", "    Side Bar",
                "  Dialogs",
                "    Dialog / Alert", "    Settings Dialog",
                "  Markdown", "  Shortcuts", "  Icons"), tree);
    }

    @Test
    @DisplayName("several words narrow further: \"form path\" leaves the Path Input")
    void searchFormPath() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        assertEquals(List.of("karthu-fx", "  Form Inputs", "    Path Input"), search(viewModel, "form path"));
    }

    @Test
    @DisplayName("the search ignores accents and case")
    void searchIgnoresAccentsAndCase() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();

        assertEquals(List.of("karthu-fx", "  Dialogs", "    Dialog / Alert", "    Settings Dialog"),
                search(viewModel, "diälog"));
        assertEquals(List.of("JavaFX", "  Selection", "    Spinner"), search(viewModel, "SPINNER"));
    }

    @Test
    @DisplayName("a search without hits leaves an empty tree and no page; clearing it brings every page back")
    void searchWithoutHits() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        SearchInput field = new SearchInput();
        viewModel.connectSearch(field);
        assertTrue(viewModel.hasPagesProperty().get());

        field.setText("zebra");
        assertEquals(List.of(), outline(viewModel.getNavigationRoot()));
        assertFalse(viewModel.hasPagesProperty().get());

        field.setText("");
        assertEquals(44, leaves(viewModel.getNavigationRoot()));
        assertTrue(viewModel.hasPagesProperty().get());
    }

    @Test
    @DisplayName("every node built for a search is expanded, and the open page stays the open page")
    void searchKeepsNodesExpandedAndTheOpenPage() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        ShowcaseItem buttons = viewModel.getShowcaseItems().get(0);
        viewModel.selectItem(buttons);

        search(viewModel, "dialog");

        List<String> collapsed = new ArrayList<>();
        collectCollapsed(viewModel.getNavigationRoot(), collapsed);
        assertEquals(List.of(), collapsed);
        assertSame(buttons, viewModel.getSelectedItem());
        assertNull(viewModel.navigationNodeOf(buttons));
    }

    @Test
    @DisplayName("the first hit is the first page the tree shows, none while the search finds nothing")
    void firstHit() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        SearchInput field = new SearchInput();
        viewModel.connectSearch(field);
        assertEquals("Buttons", viewModel.firstHit().displayName());

        field.setText("tab");
        assertEquals("TableView", viewModel.firstHit().displayName());

        field.setText("dialog");
        assertEquals("Dialog / Alert", viewModel.firstHit().displayName());

        field.setText("zebra");
        assertNull(viewModel.firstHit());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** Types a search into a field connected to the view model and returns the tree's outline. */
    private static List<String> search(StyleShowcaseViewModel viewModel, String text) {
        SearchInput field = new SearchInput();
        viewModel.connectSearch(field);
        field.setText(text);
        return outline(viewModel.getNavigationRoot());
    }

    /** The tree below the hidden root, one line per node, indented two spaces per level. */
    private static List<String> outline(TreeItem<NavigationEntry> root) {
        List<String> lines = new ArrayList<>();
        for (TreeItem<NavigationEntry> child : root.getChildren()) {
            outline(child, "", lines);
        }
        return lines;
    }

    private static void outline(TreeItem<NavigationEntry> node, String indent, List<String> lines) {
        lines.add(indent + node.getValue().text());
        for (TreeItem<NavigationEntry> child : node.getChildren()) {
            outline(child, indent + "  ", lines);
        }
    }

    private static void collectCollapsed(TreeItem<NavigationEntry> node, List<String> collapsed) {
        if (!node.isLeaf() && !node.isExpanded()) {
            collapsed.add(node.getValue() == null ? "(root)" : node.getValue().text());
        }
        for (TreeItem<NavigationEntry> child : node.getChildren()) {
            collectCollapsed(child, collapsed);
        }
    }

    private static int leaves(TreeItem<NavigationEntry> node) {
        if (node.getValue() instanceof ShowcaseItem) {
            return 1;
        }
        int count = 0;
        for (TreeItem<NavigationEntry> child : node.getChildren()) {
            count = count + leaves(child);
        }
        return count;
    }
}
