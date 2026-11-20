package org.karthu.fx.showcase.viewmodel;

import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The showcase's navigation as the view model builds it, without a window: its pages and the tree of sections, groups
 * and pages over them.
 */
class StyleShowcaseViewModelTest {

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
                "    Toggle Switch", "    Twin Column Selector", "    Key-Value Editor",
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

        assertEquals(43, viewModel.getShowcaseItems().size());
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
        assertEquals(43, leaves(root));
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

    // --- Helpers -------------------------------------------------------------------------------------

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
