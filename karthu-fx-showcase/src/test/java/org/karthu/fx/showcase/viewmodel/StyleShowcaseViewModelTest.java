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
 * The showcase's navigation as the view model builds it, without a window: its pages, the tree of sections, groups
 * and pages over them, and the text the navigation's search matches for each entry. The searching itself is the
 * navigation tree's.
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
    @DisplayName("every heading starts unfolded, the hidden root too")
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
    @DisplayName("the search matches a page by its name, its group's and its section's names, a heading by its name")
    void searchText() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        TreeItem<NavigationEntry> karthuFx = viewModel.getNavigationRoot().getChildren().get(1);
        TreeItem<NavigationEntry> formInputs = karthuFx.getChildren().get(0);
        TreeItem<NavigationEntry> pathInput = formInputs.getChildren().get(7);
        TreeItem<NavigationEntry> markdown = karthuFx.getChildren().get(3);

        assertEquals("Path Input Form Inputs karthu-fx", viewModel.searchText(pathInput.getValue()));
        assertEquals("Markdown karthu-fx", viewModel.searchText(markdown.getValue()), "a page without a group");
        assertEquals("Form Inputs", viewModel.searchText(formInputs.getValue()));
        assertEquals("karthu-fx", viewModel.searchText(karthuFx.getValue()));
    }

    @Test
    @DisplayName("the open page is the one selected last, none at start")
    void openPage() {
        StyleShowcaseViewModel viewModel = new StyleShowcaseViewModel();
        assertNull(viewModel.getSelectedItem());

        ShowcaseItem buttons = viewModel.getShowcaseItems().get(0);
        viewModel.selectItem(buttons);

        assertSame(buttons, viewModel.getSelectedItem());
        assertSame(buttons, viewModel.selectedItemProperty().get());
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
}
