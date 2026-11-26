package org.karthu.fx.sidebar;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.theme.ThemeManager;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The navigation tree through its public interface, in a themed scene that is never shown: the rows it shows, how an
 * entry opens in either mode, folding, the selected value, Expand All and Collapse All, the placeholder and its look.
 */
class NavigationTreeTest {

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

    // --- Rows ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("the tree shows the entries under a hidden root, each with its icon and its text")
    void rowsWithIconAndText() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();

        laidOut(tree);

        assertFalse(tree.getTreeView().isShowRoot());
        assertSame(sample.root, tree.getTreeView().getRoot());
        assertEquals(List.of("Fruits", "Apple", "Banana", "Vegetables", "Roots", "Leek", "Water"), rows(tree));
        assertEquals(List.of(BootstrapIcons.FOLDER, BootstrapIcons.FILE, BootstrapIcons.FILE, BootstrapIcons.FOLDER,
                BootstrapIcons.FOLDER, BootstrapIcons.FILE, BootstrapIcons.DROPLET), rowIcons(tree));
        assertTrue(tree.getStyleClass().contains("navigation-tree"));
    }

    @Test
    @DisplayName("without functions an entry shows its value's string form and no icon")
    void defaultTextWithoutIcon() {
        TreeItem<Integer> root = new TreeItem<>(0);
        root.getChildren().add(new TreeItem<>(1));
        root.getChildren().add(new TreeItem<>(2));
        NavigationTree<Integer> tree = new NavigationTree<>(root);

        laidOut(tree);

        assertEquals(List.of("1", "2"), rows(tree));
        assertEquals(Arrays.asList(null, null), rowIcons(tree));
    }

    @Test
    @DisplayName("a changed text or icon function shows at once")
    void changedFunctionsShow() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);

        tree.setTextFunction(food -> food.name().toUpperCase());
        tree.setIconFunction(null);

        assertEquals(List.of("FRUITS", "APPLE", "BANANA", "VEGETABLES", "ROOTS", "LEEK", "WATER"), rows(tree));
        assertNull(rowIcons(tree).get(0));
    }

    // --- Opening on a double click or Enter ----------------------------------------------------------

    @Test
    @DisplayName("by default a double click opens an entry with the application's value, a single click only selects")
    void doubleClickOpensALeaf() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        assertEquals(OpenMode.DOUBLE_CLICK, tree.getOpenMode());
        click(tree, sample.apple, false, 1);
        assertEquals(List.of(), opened, "a single click opens nothing");
        assertSame(sample.apple, tree.getTreeView().getSelectionModel().getSelectedItem());

        click(tree, sample.apple, false, 2);
        assertEquals(1, opened.size());
        assertSame(sample.appleValue, opened.get(0), "the application's value, not a copy");
    }

    @Test
    @DisplayName("a double click on a folder opens it and folds or unfolds it, once per double click")
    void doubleClickFoldsAFolder() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        doubleClick(tree, sample.fruits);
        assertFalse(sample.fruits.isExpanded(), "the double click folded the folder");
        assertEquals(List.of(sample.fruitsValue), opened);
        assertEquals(List.of("Fruits", "Vegetables", "Roots", "Leek", "Water"), rows(tree));

        doubleClick(tree, sample.fruits);
        assertTrue(sample.fruits.isExpanded(), "the second double click unfolded it again");
        assertEquals(List.of(sample.fruitsValue, sample.fruitsValue), opened);
    }

    @Test
    @DisplayName("Enter opens the focused entry, and on a folder also folds or unfolds it")
    void enterOpens() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        tree.getTreeView().getSelectionModel().select(sample.water);
        press(tree, KeyCode.ENTER);
        assertEquals(List.of(sample.waterValue), opened);

        tree.getTreeView().getSelectionModel().select(sample.fruits);
        press(tree, KeyCode.ENTER);
        assertFalse(sample.fruits.isExpanded(), "Enter folded the folder");
        assertEquals(List.of(sample.waterValue, sample.fruitsValue), opened);
        press(tree, KeyCode.ENTER);
        assertTrue(sample.fruits.isExpanded(), "Enter unfolded it again");
    }

    // --- Opening on selection ------------------------------------------------------------------------

    @Test
    @DisplayName("in selection mode a click opens an entry, and so does an arrow key that moves the selection")
    void selectionOpensALeaf() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        tree.setOpenMode(OpenMode.SELECTION);
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        click(tree, sample.apple, false, 1);
        assertEquals(List.of(sample.appleValue), opened);
        assertSame(sample.appleValue, opened.get(0), "the application's value, not a copy");

        press(tree, KeyCode.DOWN);
        assertEquals(List.of(sample.appleValue, sample.banana.getValue()), opened);

        click(tree, sample.banana, false, 1);
        assertEquals(2, opened.size(), "a click on the selected entry changes no selection and opens nothing");
    }

    @Test
    @DisplayName("in selection mode a single click on a folder folds or unfolds it, once per click, and opens it")
    void selectionClickFoldsAFolder() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        tree.setOpenMode(OpenMode.SELECTION);
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        click(tree, sample.fruits, false, 1);
        assertFalse(sample.fruits.isExpanded(), "the click folded the folder");
        assertSame(sample.fruits, tree.getTreeView().getSelectionModel().getSelectedItem());
        assertEquals(List.of(sample.fruitsValue), opened);

        click(tree, sample.fruits, false, 2);
        assertTrue(sample.fruits.isExpanded(), "the second click of a double click unfolded it again");
        assertEquals(List.of(sample.fruitsValue), opened, "the selection stayed, nothing opened again");
    }

    @Test
    @DisplayName("in selection mode Enter selects the focused entry, which opens it, and folds or unfolds a folder")
    void selectionEnter() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        tree.setOpenMode(OpenMode.SELECTION);
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        tree.getTreeView().getFocusModel().focus(tree.getTreeView().getRow(sample.water));
        press(tree, KeyCode.ENTER);
        assertSame(sample.water, tree.getTreeView().getSelectionModel().getSelectedItem());
        assertEquals(List.of(sample.waterValue), opened);

        tree.getTreeView().getFocusModel().focus(tree.getTreeView().getRow(sample.fruits));
        press(tree, KeyCode.ENTER);
        assertFalse(sample.fruits.isExpanded(), "Enter folded the folder");
        assertEquals(List.of(sample.waterValue, sample.fruitsValue), opened);
    }

    @Test
    @DisplayName("in selection mode a selection the application makes, or a change of its tree, opens nothing")
    void selectionByTheApplicationOpensNothing() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        tree.setOpenMode(OpenMode.SELECTION);
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        tree.getTreeView().getSelectionModel().select(sample.apple);
        sample.fruits.getChildren().remove(sample.apple);
        relayOut(tree);

        assertEquals(List.of(), opened);
    }

    @Test
    @DisplayName("opening and folding stay with cells of the application's own")
    void ownCellsOpen() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        tree.getTreeView().setCellFactory(view -> new TreeCell<>() {
            @Override
            protected void updateItem(Food food, boolean empty) {
                super.updateItem(food, empty);
                setText(empty || food == null ? null : "own " + food.name());
            }
        });
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        doubleClick(tree, sample.fruits);
        assertFalse(sample.fruits.isExpanded());
        doubleClick(tree, sample.water);
        assertEquals(List.of(sample.fruitsValue, sample.waterValue), opened);

        tree.setOpenMode(OpenMode.SELECTION);
        click(tree, sample.vegetables, false, 1);
        assertFalse(sample.vegetables.isExpanded());
        assertEquals(List.of(sample.fruitsValue, sample.waterValue, sample.vegetables.getValue()), opened);
    }

    @Test
    @DisplayName("the disclosure arrow folds and unfolds once per click and opens nothing, in either mode")
    void arrowFolds() {
        for (OpenMode mode : OpenMode.values()) {
            Sample sample = new Sample();
            NavigationTree<Food> tree = sample.tree();
            tree.setOpenMode(mode);
            List<Food> opened = new ArrayList<>();
            tree.setOnOpen(opened::add);
            laidOut(tree);

            click(tree, sample.fruits, true, 1);
            assertFalse(sample.fruits.isExpanded(), mode + ": the arrow folded the folder");
            click(tree, sample.fruits, true, 1);
            assertTrue(sample.fruits.isExpanded(), mode + ": the arrow unfolded it");
            click(tree, sample.fruits, true, 2);
            assertFalse(sample.fruits.isExpanded(), mode + ": the second click of a double click folded it once");
            assertEquals(List.of(), opened, mode + ": the arrow opens nothing");
        }
    }

    // --- Selected value ------------------------------------------------------------------------------

    @Test
    @DisplayName("the selected value is the application's value of the selected entry, null without a selection")
    void selectedValueFollowsTheSelection() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        ReadOnlyObjectProperty<Food> selectedValue = tree.selectedValueProperty();

        assertNull(tree.getSelectedValue());
        click(tree, sample.apple, false, 1);
        assertSame(sample.appleValue, tree.getSelectedValue());
        assertSame(sample.appleValue, selectedValue.get());

        Food renamed = new Food("Green Apple", BootstrapIcons.FILE);
        sample.apple.setValue(renamed);
        assertSame(renamed, tree.getSelectedValue(), "a value replaced in the selected entry");

        tree.getTreeView().getSelectionModel().clearSelection();
        assertNull(tree.getSelectedValue());
    }

    @Test
    @DisplayName("select finds the entry by its value, unfolds the folders above it, selects it and opens nothing")
    void selectAValue() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        tree.setOpenMode(OpenMode.SELECTION);
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);

        assertTrue(tree.select(new Food("Carrot", BootstrapIcons.FILE)), "an equal value finds the entry");

        assertTrue(sample.roots.isExpanded(), "the folder above the entry unfolded");
        assertSame(sample.carrot, tree.getTreeView().getSelectionModel().getSelectedItem());
        assertSame(sample.carrotValue, tree.getSelectedValue(), "the application's value, not the one asked for");
        assertEquals(List.of("Fruits", "Apple", "Banana", "Vegetables", "Roots", "Carrot", "Leek", "Water"),
                rows(tree));
        assertEquals(List.of(), opened);

        assertFalse(tree.select(new Food("Cheese", BootstrapIcons.FILE)), "no entry holds the value");
        assertNull(tree.getSelectedValue(), "the selection is cleared");

        tree.select(sample.waterValue);
        assertFalse(tree.select(null));
        assertNull(tree.getSelectedValue(), "null clears the selection");
    }

    @Test
    @DisplayName("select scrolls the tree so that the selected entry shows")
    void selectScrollsToTheEntry() {
        TreeItem<Integer> root = new TreeItem<>(0);
        for (int number = 1; number <= 80; number++) {
            root.getChildren().add(new TreeItem<>(number));
        }
        NavigationTree<Integer> tree = new NavigationTree<>(root);
        laidOut(tree);
        assertFalse(rows(tree).contains("80"), "the last entry lies below the visible rows");

        tree.select(80);

        assertTrue(rows(tree).contains("80"), "the selected entry shows");
        assertTrue(rows(tree).contains("70"), "the tree scrolled only as far as needed");
    }

    @Test
    @DisplayName("select scrolls to an entry inside a folded folder, which it unfolds")
    void selectScrollsIntoAFoldedFolder() {
        TreeItem<Integer> root = new TreeItem<>(0);
        TreeItem<Integer> folder = new TreeItem<>(100);
        for (int number = 1; number <= 80; number++) {
            folder.getChildren().add(new TreeItem<>(number));
        }
        root.getChildren().add(folder);
        NavigationTree<Integer> tree = new NavigationTree<>(root);
        laidOut(tree);
        assertEquals(List.of("100"), rows(tree));

        tree.select(77);

        assertTrue(folder.isExpanded());
        assertTrue(rows(tree).contains("77"), "the selected entry shows");
    }

    // --- Expand All and Collapse All ----------------------------------------------------------------

    @Test
    @DisplayName("Expand All unfolds every entry with children, Collapse All folds every entry below the hidden root")
    void expandAllAndCollapseAll() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);

        tree.expandAll();
        assertEquals(List.of("Fruits", "Apple", "Banana", "Vegetables", "Roots", "Carrot", "Leek", "Water"),
                rows(tree));

        tree.collapseAll();
        assertEquals(List.of("Fruits", "Vegetables", "Water"), rows(tree));
        assertTrue(sample.root.isExpanded(), "the hidden root stays unfolded");

        sample.vegetables.setExpanded(true);
        assertEquals(List.of("Fruits", "Vegetables", "Roots", "Leek", "Water"), rows(tree),
                "the folders inside were folded too");
    }

    @Test
    @DisplayName("the header buttons are Expand All and Collapse All, icon buttons whose tooltips are properties")
    void headerButtons() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);

        List<Node> buttons = tree.getHeaderButtons();
        assertEquals(2, buttons.size());
        Button expandAll = (Button) buttons.get(0);
        Button collapseAll = (Button) buttons.get(1);
        assertTrue(expandAll.getStyleClass().containsAll(List.of("icon-button", "navigation-tree-expand-all")));
        assertTrue(collapseAll.getStyleClass().containsAll(List.of("icon-button", "navigation-tree-collapse-all")));
        assertEquals(BootstrapIcons.ARROWS_EXPAND, ((FontIcon) expandAll.getGraphic()).getIconCode());
        assertEquals(BootstrapIcons.ARROWS_COLLAPSE, ((FontIcon) collapseAll.getGraphic()).getIconCode());
        assertEquals("Expand All", expandAll.getTooltip().getText());
        assertEquals("Expand All", expandAll.getAccessibleText());
        assertEquals("Collapse All", collapseAll.getTooltip().getText());
        assertEquals("Collapse All", collapseAll.getAccessibleText());
        assertSame(buttons.get(0), tree.getHeaderButtons().get(0), "the same buttons on every call");

        tree.setExpandAllText("Alles aufklappen");
        tree.setCollapseAllText("Alles zuklappen");
        assertEquals("Alles aufklappen", expandAll.getTooltip().getText());
        assertEquals("Alles zuklappen", collapseAll.getAccessibleText());
        assertEquals("Alles aufklappen", tree.expandAllTextProperty().get());
        assertEquals("Alles zuklappen", tree.getCollapseAllText());

        collapseAll.fire();
        assertEquals(List.of("Fruits", "Vegetables", "Water"), rows(tree));
        expandAll.fire();
        assertEquals(8, rows(tree).size());
    }

    // --- Placeholder ---------------------------------------------------------------------------------

    @Test
    @DisplayName("an empty tree shows the application's placeholder, a tree with entries does not")
    void placeholderForAnEmptyTree() {
        TreeItem<String> root = new TreeItem<>("root");
        NavigationTree<String> tree = new NavigationTree<>(root);
        Label placeholder = new Label("No workflows yet");
        tree.setPlaceholder(placeholder);
        laidOut(tree);

        assertTrue(shows(tree, placeholder), "the empty tree shows its placeholder");

        TreeItem<String> first = new TreeItem<>("First");
        root.getChildren().add(first);
        assertFalse(shows(tree, placeholder), "a tree with an entry hides it");
        assertEquals(List.of("First"), rows(tree));

        root.getChildren().remove(first);
        assertTrue(shows(tree, placeholder), "the tree became empty again");

        tree.setRoot(new TreeItem<>("other root"));
        assertTrue(shows(tree, placeholder));
        tree.getRoot().getChildren().add(new TreeItem<>("Second"));
        assertFalse(shows(tree, placeholder), "the new root's entries are followed");

        tree.setRoot(null);
        assertTrue(shows(tree, placeholder), "no root at all is an empty tree");

        Label other = new Label("Nothing");
        tree.setPlaceholder(other);
        assertTrue(shows(tree, other));
        assertFalse(shows(tree, placeholder), "the replaced placeholder is gone");
        assertSame(other, tree.getPlaceholder());
    }

    // --- Look ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("the theme draws the tree without border or own background, the selected row with a 2 px accent bar")
    void look() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        Region accent = swatch("-fx-accent-color");
        Region selection = swatch("-fx-selection-color");
        Region secondary = swatch("-fx-text-secondary-color");
        laidOut(tree, accent, selection, secondary);
        tree.select(sample.appleValue);
        relayOut(tree);

        assertNoBackground(tree.getBackground());
        assertNoBackground(tree.getTreeView().getBackground());
        assertNoBorder(tree.getTreeView().getBorder());

        TreeCell<?> selected = cellOf(tree, sample.apple);
        List<BackgroundFill> fills = selected.getBackground().getFills();
        assertEquals(2, fills.size(), "the accent bar under the selection colour");
        assertEquals(fill(accent), fills.get(0).getFill());
        assertEquals(fill(selection), fills.get(1).getFill());
        assertEquals(new Insets(0, 0, 0, 2), fills.get(1).getInsets(), "a 2 px bar shows on the left");
        assertEquals(fill(accent), ((FontIcon) selected.lookup(".navigation-tree-icon")).getIconColor());

        FontIcon other = (FontIcon) cellOf(tree, sample.banana).lookup(".navigation-tree-icon");
        assertEquals(fill(secondary), other.getIconColor());
        assertEquals(14, other.getIconSize());
        FontIcon expandAll = (FontIcon) ((Button) tree.getHeaderButtons().get(0)).getGraphic();
        tree.getChildren().add(tree.getHeaderButtons().get(0));
        relayOut(tree);
        assertEquals(fill(secondary), expandAll.getIconColor(), "a header button's icon in the secondary colour");
        assertEquals(14, expandAll.getIconSize());
    }

    // --- Search -------------------------------------------------------------------------------------

    @Test
    @DisplayName("the search field sits above the tree over its full width, and the inner field is exposed")
    void searchFieldAboveTheTree() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);

        SearchInput search = tree.getSearchInput();
        Bounds field = search.localToScene(search.getLayoutBounds());
        Bounds view = tree.getTreeView().localToScene(tree.getTreeView().getLayoutBounds());
        assertTrue(field.getMaxY() <= view.getMinY(), "the field lies above the tree view");
        assertTrue(field.getWidth() > tree.getWidth() - 24, "the field spans the tree's width");
        assertTrue(shows(tree, search));
    }

    @Test
    @DisplayName("a search shows the hits and the folders above them, every folder unfolded, the application's tree untouched")
    void searchShowsHitsAndTheirFolders() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);

        search(tree, "carrot");

        assertEquals(List.of("Vegetables", "Roots", "Carrot"), rows(tree));
        assertFalse(sample.roots.isExpanded(), "the application's folder stays folded");
        assertSame(sample.root, tree.getRoot());
    }

    @Test
    @DisplayName("a folder that matches keeps all its entries, its folders unfolded")
    void folderHitKeepsItsEntries() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);

        search(tree, "VEGETAB");

        assertEquals(List.of("Vegetables", "Roots", "Carrot", "Leek"), rows(tree));
    }

    @Test
    @DisplayName("the search follows karthu-fx's search rule: several words narrow, case and accents ignored")
    void searchRule() {
        TreeItem<String> root = new TreeItem<>("root");
        root.getChildren().add(new TreeItem<>("Zürich Main Station"));
        root.getChildren().add(new TreeItem<>("Zurich Airport"));
        root.getChildren().add(new TreeItem<>("Bern Station"));
        NavigationTree<String> tree = new NavigationTree<>(root);
        laidOut(tree);

        search(tree, "zurich");
        assertEquals(List.of("Zürich Main Station", "Zurich Airport"), rows(tree));

        search(tree, "STATION zür");
        assertEquals(List.of("Zürich Main Station"), rows(tree));
    }

    @Test
    @DisplayName("the field counts the hits out of all entries, folded ones included, the root not counted")
    void counter() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        assertNull(counter(tree), "no counter without a search");

        search(tree, "a");
        assertEquals("5 of 8", counter(tree), "Apple, Banana, Vegetables, Carrot and Water");

        search(tree, "carrot");
        assertEquals("1 of 8", counter(tree));

        search(tree, "");
        assertNull(counter(tree));
    }

    @Test
    @DisplayName("with no hits the tree area shows \"No matches\", a text property, until the search finds an entry")
    void noMatches() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        Label noMatches = (Label) tree.lookup(".navigation-tree-no-matches");
        assertFalse(shows(tree, noMatches), "not without a search");

        search(tree, "cheese");

        assertEquals(List.of(), rows(tree));
        assertTrue(shows(tree, noMatches));
        assertEquals("No matches", noMatches.getText());
        assertEquals("0 of 8", counter(tree));
        tree.setNoMatchesText("Keine Treffer");
        assertEquals("Keine Treffer", noMatches.getText());
        assertEquals("Keine Treffer", tree.noMatchesTextProperty().get());
        assertEquals("Keine Treffer", tree.getNoMatchesText());

        search(tree, "leek");
        assertFalse(shows(tree, noMatches), "the search finds an entry");
        search(tree, "cheese");
        search(tree, "");
        assertFalse(shows(tree, noMatches), "the search ended");
    }

    @Test
    @DisplayName("the default cell marks the matching characters of the shown text, following the search")
    void highlighting() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        assertEquals("Banana", marked(rowCell(tree, "Banana")), "no marks without a search");

        search(tree, "an");
        assertEquals("B[anan]a", marked(rowCell(tree, "Banana")));

        search(tree, "nana b");
        assertEquals("[B]a[nana]", marked(rowCell(tree, "Banana")));
        assertEquals("Fruits", marked(rowCell(tree, "Fruits")), "a folder above a hit has no marks");
    }

    @Test
    @DisplayName("a mark shows bold in the accent colour; on the selected row the text is, and its marks are underlined")
    void highlightLook() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        Region accent = swatch("-fx-accent-color");
        Region text = swatch("-fx-text-color");
        laidOut(tree, accent, text);
        search(tree, "a");
        tree.select(sample.banana.getValue());
        relayOut(tree);

        List<Text> apple = runs(rowCell(tree, "Apple"));
        assertLook("A", fill(accent), true, false, apple.get(0));
        assertLook("pple", fill(text), false, false, apple.get(1));

        List<Text> banana = runs(rowCell(tree, "Banana"));
        assertLook("B", fill(accent), true, false, banana.get(0));
        assertLook("a", fill(accent), true, true, banana.get(1));
    }

    @Test
    @DisplayName("clearing the search shows the application's tree folded as the user left it, whatever the search unfolded or folded")
    void foldingBackAfterClearing() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        sample.fruits.setExpanded(false);
        assertEquals(List.of("Fruits", "Vegetables", "Roots", "Leek", "Water"), rows(tree));

        search(tree, "e");
        assertEquals(List.of("Fruits", "Apple", "Vegetables", "Roots", "Carrot", "Leek", "Water"), rows(tree),
                "every folder of the copy unfolded");
        tree.collapseAll();
        assertEquals(List.of("Fruits", "Vegetables", "Water"), rows(tree), "Collapse All folds the copy");
        tree.expandAll();
        search(tree, "");

        assertSame(sample.root, tree.getTreeView().getRoot());
        assertEquals(List.of("Fruits", "Vegetables", "Roots", "Leek", "Water"), rows(tree));
        assertFalse(sample.fruits.isExpanded());
        assertTrue(sample.vegetables.isExpanded());
        assertFalse(sample.roots.isExpanded());
    }

    @Test
    @DisplayName("clearing the search unfolds the folders above the selected entry, selects it and scrolls to it")
    void selectedEntryShowsAfterClearing() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> numbers = new TreeItem<>("Numbers");
        for (int number = 1; number <= 80; number++) {
            numbers.getChildren().add(new TreeItem<>("Number " + number));
        }
        TreeItem<String> letters = new TreeItem<>("Letters");
        letters.getChildren().add(new TreeItem<>("Alpha"));
        root.getChildren().add(letters);
        root.getChildren().add(numbers);
        NavigationTree<String> tree = new NavigationTree<>(root);
        laidOut(tree);
        assertEquals(List.of("Letters", "Numbers"), rows(tree));

        search(tree, "number 77");
        click(tree, rowCell(tree, "Number 77"), 1);
        assertEquals("Number 77", tree.getSelectedValue());
        search(tree, "");

        assertTrue(numbers.isExpanded(), "the folder above the selected entry unfolded");
        assertFalse(letters.isExpanded(), "the other folder stays folded");
        assertSame(numbers.getChildren().get(76), tree.getTreeView().getSelectionModel().getSelectedItem());
        assertEquals("Number 77", tree.getSelectedValue());
        assertTrue(rows(tree).contains("Number 77"), "the selected entry shows");
    }

    @Test
    @DisplayName("the selection survives a search that shows its entry, and is cleared by one that does not")
    void selectionThroughASearch() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        tree.setOpenMode(OpenMode.SELECTION);
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);
        tree.select(sample.appleValue);
        List<Food> selectedValues = new ArrayList<>();
        tree.selectedValueProperty().addListener((observable, oldValue, newValue) -> selectedValues.add(newValue));

        search(tree, "app");
        assertSame(sample.appleValue, tree.getSelectedValue(), "a hit");
        assertTrue(rowCell(tree, "Apple").isSelected(), "its row shows selected");
        search(tree, "fruits");
        assertSame(sample.appleValue, tree.getSelectedValue(), "shown, as its folder is a hit");
        assertEquals(List.of(), selectedValues, "the selected value never changed on the way");

        search(tree, "ban");
        assertNull(tree.getSelectedValue(), "not shown: the selection is cleared");
        search(tree, "a");
        assertNull(tree.getSelectedValue(), "a cleared selection does not come back");
        search(tree, "");
        assertNull(tree.getSelectedValue());
        assertEquals(List.of(), opened, "the search opens nothing");
    }

    @Test
    @DisplayName("during a search the selected value and the open handler carry the application's values")
    void valuesDuringASearch() {
        for (OpenMode mode : OpenMode.values()) {
            Sample sample = new Sample();
            NavigationTree<Food> tree = sample.tree();
            tree.setOpenMode(mode);
            List<Food> opened = new ArrayList<>();
            tree.setOnOpen(opened::add);
            laidOut(tree);
            search(tree, "carrot");

            click(tree, rowCell(tree, "Carrot"), 1);
            if (mode == OpenMode.DOUBLE_CLICK) {
                click(tree, rowCell(tree, "Carrot"), 2);
            }

            assertEquals(1, opened.size(), mode + ": opened once");
            assertSame(sample.carrotValue, opened.get(0), mode + ": the application's value, not a copy");
            assertSame(sample.carrotValue, tree.getSelectedValue(), mode + ": the selected value");
            assertNotSame(sample.carrot, tree.getTreeView().getSelectionModel().getSelectedItem(),
                    mode + ": the tree view shows a copy of the application's item");
        }
    }

    @Test
    @DisplayName("a search text function lets the search find an entry by a text it does not show, which shows no marks")
    void ownSearchText() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        assertNull(tree.getSearchTextFunction(), "by default the shown text");
        tree.setSearchTextFunction(food -> food == sample.carrotValue ? "Carrot orange root" : food.name());

        search(tree, "orange");

        assertEquals(List.of("Vegetables", "Roots", "Carrot"), rows(tree));
        assertEquals("Carrot", marked(rowCell(tree, "Carrot")), "the marks show only in the shown text");
        assertEquals("1 of 8", counter(tree));

        tree.searchTextFunctionProperty().set(null);
        assertEquals(List.of(), rows(tree), "a changed function applies to the search under way");
        assertEquals("0 of 8", counter(tree));
    }

    @Test
    @DisplayName("cells of the application's own receive its values during a search and mark the hits with the same helper")
    void ownCellsDuringASearch() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        List<Food> shownValues = new ArrayList<>();
        tree.getTreeView().setCellFactory(view -> new TreeCell<>() {
            @Override
            protected void updateItem(Food food, boolean empty) {
                super.updateItem(food, empty);
                setText(null);
                if (empty || food == null) {
                    setGraphic(null);
                    return;
                }
                shownValues.add(food);
                TextFlow name = tree.getSearchInput().highlight(food.name());
                name.getStyleClass().add("own-name");
                setGraphic(name);
            }
        });
        laidOut(tree);

        search(tree, "ban");

        TreeCell<?> banana = null;
        for (TreeCell<?> cell : shownCells(tree)) {
            if (cell.getItem() == sample.banana.getValue()) {
                banana = cell;
            }
        }
        assertTrue(banana != null, "a row shows the application's value");
        TextFlow name = (TextFlow) banana.lookup(".own-name");
        assertEquals("[Ban]ana", marked(name));
        assertTrue(shownValues.contains(sample.fruitsValue));
    }

    @Test
    @DisplayName("during a search a change of the application's tree shows at once: an entry added or removed, a value replaced")
    void treeChangeDuringASearch() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        search(tree, "berry");
        assertEquals(List.of(), rows(tree));

        TreeItem<Food> strawberry = new TreeItem<>(new Food("Strawberry", BootstrapIcons.FILE));
        sample.fruits.getChildren().add(strawberry);
        assertEquals(List.of("Fruits", "Strawberry"), rows(tree));
        assertEquals("1 of 9", counter(tree));
        tree.select(strawberry.getValue());

        Food renamed = new Food("Wild Strawberry", BootstrapIcons.FILE);
        strawberry.setValue(renamed);
        assertEquals(List.of("Fruits", "Wild Strawberry"), rows(tree));
        assertSame(renamed, tree.getSelectedValue(), "the selected value follows the replaced value");

        sample.water.setValue(new Food("Berry Water", BootstrapIcons.DROPLET));
        assertEquals(List.of("Fruits", "Wild Strawberry", "Berry Water"), rows(tree));

        sample.fruits.getChildren().remove(strawberry);
        assertEquals(List.of("Berry Water"), rows(tree));
        assertNull(tree.getSelectedValue(), "the selected entry is gone");
        assertEquals("1 of 8", counter(tree));

        TreeItem<Food> other = new TreeItem<>(new Food("Other", BootstrapIcons.FOLDER));
        other.getChildren().add(new TreeItem<>(new Food("Blueberry", BootstrapIcons.FILE)));
        tree.setRoot(other);
        assertEquals(List.of("Blueberry"), rows(tree), "a new root is searched too");
        assertEquals("1 of 1", counter(tree));
        assertTrue(sample.fruits.isExpanded(), "the application's folding stayed as it was");
    }

    // --- Keys of the search ---------------------------------------------------------------------------

    @Test
    @DisplayName("Ctrl+F inside the tree focuses the search field and selects its text; outside it is not the tree's")
    void ctrlFFocusesTheSearch() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        TextField elsewhere = new TextField();
        laidOut(tree, elsewhere);
        tree.getSearchInput().setText("fru");
        TextField field = tree.getSearchInput().getTextField();

        pressAt(tree.getTreeView(), KeyCode.F, true);
        assertSame(field, tree.getScene().getFocusOwner());
        assertEquals("fru", field.getSelectedText());

        field.deselect();
        pressAt(field, KeyCode.F, true);
        assertEquals("fru", field.getSelectedText(), "in the field too");

        pressAt(elsewhere, KeyCode.F, true);
        assertSame(elsewhere, tree.getScene().getFocusOwner());
    }

    @Test
    @DisplayName("Arrow down in the field moves the focus to the tree on the first hit, which opens it only in selection mode")
    void arrowDownMovesToTheFirstHit() {
        for (OpenMode mode : OpenMode.values()) {
            Sample sample = new Sample();
            NavigationTree<Food> tree = sample.tree();
            tree.setOpenMode(mode);
            List<Food> opened = new ArrayList<>();
            tree.setOnOpen(opened::add);
            laidOut(tree);
            search(tree, "e");
            assertEquals("Fruits", rows(tree).get(0), "the folder above the first hit comes first");

            pressAt(tree.getSearchInput().getTextField(), KeyCode.DOWN, false);

            assertSame(tree.getTreeView(), tree.getScene().getFocusOwner(), mode.name());
            assertSame(sample.appleValue, tree.getSelectedValue(), mode + ": the first hit, not the folder above it");
            assertSame(sample.appleValue, tree.getTreeView().getFocusModel().getFocusedItem().getValue(), mode.name());
            List<Food> expected = mode == OpenMode.SELECTION ? List.of(sample.appleValue) : List.of();
            assertEquals(expected, opened, mode + ": selecting opens only where selection opens");
        }
    }

    @Test
    @DisplayName("Enter in the field selects the first hit and opens it, the focus staying in the field")
    void enterOpensTheFirstHit() {
        for (OpenMode mode : OpenMode.values()) {
            Sample sample = new Sample();
            NavigationTree<Food> tree = sample.tree();
            tree.setOpenMode(mode);
            List<Food> opened = new ArrayList<>();
            tree.setOnOpen(opened::add);
            laidOut(tree);
            search(tree, "lee");
            TextField field = tree.getSearchInput().getTextField();

            pressAt(field, KeyCode.ENTER, false);

            assertSame(field, tree.getScene().getFocusOwner(), mode.name());
            assertSame(sample.leek.getValue(), tree.getSelectedValue(), mode.name());
            assertEquals(List.of(sample.leek.getValue()), opened, mode + ": opened once");
        }
    }

    @Test
    @DisplayName("without a hit Arrow down and Enter do nothing; in an empty field Arrow down moves to the selected entry")
    void keysWithoutAHit() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        tree.setOpenMode(OpenMode.SELECTION);
        List<Food> opened = new ArrayList<>();
        tree.setOnOpen(opened::add);
        laidOut(tree);
        TextField field = tree.getSearchInput().getTextField();

        search(tree, "cheese");
        pressAt(field, KeyCode.DOWN, false);
        pressAt(field, KeyCode.ENTER, false);
        assertSame(field, tree.getScene().getFocusOwner());
        assertNull(tree.getSelectedValue());

        search(tree, "");
        tree.select(sample.waterValue);
        pressAt(field, KeyCode.ENTER, false);
        assertSame(field, tree.getScene().getFocusOwner(), "Enter in an empty field is no search");
        pressAt(field, KeyCode.DOWN, false);
        assertSame(tree.getTreeView(), tree.getScene().getFocusOwner());
        assertSame(sample.water, tree.getTreeView().getFocusModel().getFocusedItem());
        assertSame(sample.waterValue, tree.getSelectedValue(), "the selection stays");
        assertEquals(List.of(), opened);
    }

    @Test
    @DisplayName("Escape in the field clears the search and shows the application's tree again")
    void escapeClears() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        search(tree, "carrot");

        pressAt(tree.getSearchInput().getTextField(), KeyCode.ESCAPE, false);
        relayOut(tree);

        assertEquals("", tree.getSearchInput().getText());
        assertSame(sample.root, tree.getTreeView().getRoot());
        assertEquals(List.of("Fruits", "Apple", "Banana", "Vegetables", "Roots", "Leek", "Water"), rows(tree));
    }

    @Test
    @DisplayName("as a side bar's content the tree focuses its search field, its text selected")
    void contentContractFocusesTheSearch() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        laidOut(tree);
        tree.getSearchInput().setText("app");
        tree.getTreeView().requestFocus();

        assertTrue(tree.focusSearch());

        TextField field = tree.getSearchInput().getTextField();
        assertSame(field, tree.getScene().getFocusOwner());
        assertEquals("app", field.getSelectedText());
    }

    @Test
    @DisplayName("an empty tree shows its placeholder during a search, not \"No matches\"")
    void placeholderDuringASearch() {
        TreeItem<String> root = new TreeItem<>("root");
        NavigationTree<String> tree = new NavigationTree<>(root);
        Label placeholder = new Label("No workflows yet");
        tree.setPlaceholder(placeholder);
        laidOut(tree);
        Node noMatches = tree.lookup(".navigation-tree-no-matches");

        search(tree, "flow");

        assertTrue(shows(tree, placeholder));
        assertFalse(shows(tree, noMatches));
        root.getChildren().add(new TreeItem<>("Import"));
        assertFalse(shows(tree, placeholder));
        assertTrue(shows(tree, noMatches), "the tree has an entry now, which the search does not find");
    }

    // --- Content contract ----------------------------------------------------------------------------

    @Test
    @DisplayName("as a side bar's content the tree has no menu items, and takes the focus on its selection")
    void contentContract() {
        Sample sample = new Sample();
        NavigationTree<Food> tree = sample.tree();
        TextField elsewhere = new TextField();
        laidOut(tree, elsewhere);
        tree.select(sample.carrotValue);
        elsewhere.requestFocus();

        tree.focusContent();

        assertSame(tree.getTreeView(), tree.getScene().getFocusOwner());
        assertSame(sample.carrot, tree.getTreeView().getFocusModel().getFocusedItem());
        assertEquals(List.of(), tree.getMenuItems());
    }

    @Test
    @DisplayName("any node can be a side bar's content: by default no header buttons, no menu items, no search")
    void contentContractDefaults() {
        PlainContent content = new PlainContent();
        TextField elsewhere = new TextField();
        StackPane root = new StackPane(elsewhere, content);
        Scene scene = new Scene(root);
        elsewhere.requestFocus();

        content.focusContent();

        assertSame(content, scene.getFocusOwner(), "the node itself takes the focus");
        assertEquals(List.of(), content.getHeaderButtons());
        assertEquals(List.of(), content.getMenuItems());
        assertFalse(content.focusSearch());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** A node that is a side bar's content with the contract's defaults only. */
    private static final class PlainContent extends Label implements SideBarContent {
    }

    /** An entry of the sample tree: its name and its icon. */
    private record Food(String name, Ikon icon) {
    }

    /**
     * The sample tree under its hidden root: two folders, the second with a collapsed sub-folder, and a leaf.
     * <pre>
     * Fruits        (expanded)
     *   Apple
     *   Banana
     * Vegetables    (expanded)
     *   Roots       (collapsed)
     *     Carrot
     *   Leek
     * Water
     * </pre>
     */
    private static final class Sample {

        final Food fruitsValue = new Food("Fruits", BootstrapIcons.FOLDER);
        final Food appleValue = new Food("Apple", BootstrapIcons.FILE);
        final Food carrotValue = new Food("Carrot", BootstrapIcons.FILE);
        final Food waterValue = new Food("Water", BootstrapIcons.DROPLET);

        final TreeItem<Food> root = new TreeItem<>(new Food("Food", BootstrapIcons.FOLDER));
        final TreeItem<Food> fruits = new TreeItem<>(fruitsValue);
        final TreeItem<Food> apple = new TreeItem<>(appleValue);
        final TreeItem<Food> banana = new TreeItem<>(new Food("Banana", BootstrapIcons.FILE));
        final TreeItem<Food> vegetables = new TreeItem<>(new Food("Vegetables", BootstrapIcons.FOLDER));
        final TreeItem<Food> roots = new TreeItem<>(new Food("Roots", BootstrapIcons.FOLDER));
        final TreeItem<Food> carrot = new TreeItem<>(carrotValue);
        final TreeItem<Food> leek = new TreeItem<>(new Food("Leek", BootstrapIcons.FILE));
        final TreeItem<Food> water = new TreeItem<>(waterValue);

        Sample() {
            fruits.getChildren().add(apple);
            fruits.getChildren().add(banana);
            roots.getChildren().add(carrot);
            vegetables.getChildren().add(roots);
            vegetables.getChildren().add(leek);
            root.getChildren().add(fruits);
            root.getChildren().add(vegetables);
            root.getChildren().add(water);
            fruits.setExpanded(true);
            vegetables.setExpanded(true);
        }

        /** A navigation tree over the sample, with the entries' names and icons. */
        NavigationTree<Food> tree() {
            NavigationTree<Food> tree = new NavigationTree<>(root);
            tree.setTextFunction(food -> food.name());
            tree.setIconFunction(food -> food.icon());
            return tree;
        }
    }

    /**
     * Puts the tree into a themed scene that is never shown, sized like a side bar's panel, and lays it out; other
     * nodes, such as a field to put the focus elsewhere, go above it.
     */
    private static Scene laidOut(NavigationTree<?> tree, Node... others) {
        VBox root = new VBox(others);
        root.getChildren().add(tree);
        VBox.setVgrow(tree, Priority.ALWAYS);
        Scene scene = new Scene(root, 320, 600);
        ThemeManager.apply(scene);
        root.applyCss();
        root.resize(320, 600);
        root.layout();
        return scene;
    }

    /** Puts a text into the tree's search field, as the user types it, and lays the tree out again. */
    private static void search(NavigationTree<?> tree, String text) {
        tree.getSearchInput().setText(text);
        relayOut(tree);
    }

    /** The text of the search field's counter, null while it does not show. */
    private static String counter(NavigationTree<?> tree) {
        Label counter = (Label) tree.getSearchInput().lookup(".form-search-count");
        relayOut(tree);
        if (!counter.isVisible() || !counter.isManaged()) {
            return null;
        }
        return counter.getText();
    }

    /** Lays the tree's scene out again, after a change. */
    private static void relayOut(NavigationTree<?> tree) {
        Parent root = tree.getScene().getRoot();
        root.applyCss();
        root.layout();
    }

    /** The texts of the rows the tree shows, top to bottom. */
    private static List<String> rows(NavigationTree<?> tree) {
        List<String> texts = new ArrayList<>();
        for (TreeCell<?> cell : shownCells(tree)) {
            texts.add(rowText(cell));
        }
        return texts;
    }

    /** The text a row of the default cell shows: the runs of its highlight node, or an own cell's text. */
    private static String rowText(TreeCell<?> cell) {
        Node text = cell.lookup(".navigation-tree-text");
        if (text == null) {
            return cell.getText();
        }
        StringBuilder shown = new StringBuilder();
        for (Text run : runs(cell)) {
            shown.append(run.getText());
        }
        return shown.toString();
    }

    /** The shown cell whose row shows a text. */
    private static TreeCell<?> rowCell(NavigationTree<?> tree, String text) {
        for (TreeCell<?> cell : shownCells(tree)) {
            if (text.equals(rowText(cell))) {
                return cell;
            }
        }
        throw new AssertionError("no row shows " + text);
    }

    /** The runs of the highlight node of a row of the default cell, left to right. */
    private static List<Text> runs(TreeCell<?> cell) {
        TextFlow flow = (TextFlow) cell.lookup(".navigation-tree-text");
        assertTrue(flow.getStyleClass().contains("search-highlight"), "the text is a highlight node");
        List<Text> runs = new ArrayList<>();
        for (Node child : flow.getChildren()) {
            runs.add((Text) child);
        }
        return runs;
    }

    /** The text of a row of the default cell, with its marks in brackets. */
    private static String marked(TreeCell<?> cell) {
        return marked((TextFlow) cell.lookup(".navigation-tree-text"));
    }

    /** The text of a highlight node, with its marks in brackets. */
    private static String marked(TextFlow flow) {
        StringBuilder marked = new StringBuilder();
        for (Node child : flow.getChildren()) {
            Text run = (Text) child;
            if (run.getStyleClass().contains("search-match")) {
                marked.append('[').append(run.getText()).append(']');
            } else {
                marked.append(run.getText());
            }
        }
        return marked.toString();
    }

    /** Checks a run's text and its look in the theme: its fill, bold or not, underlined or not. */
    private static void assertLook(String expected, Paint fill, boolean bold, boolean underlined, Text run) {
        assertEquals(expected, run.getText());
        assertEquals(fill, run.getFill(), () -> "the fill of " + run);
        assertEquals(bold, run.getFont().getStyle().toLowerCase(Locale.ROOT).contains("bold"),
                () -> "bold " + !bold + ": " + run + ", " + run.getFont());
        assertEquals(underlined, run.isUnderline(), () -> "underlined " + !underlined + ": " + run);
    }

    /** The icons of the rows the tree shows, top to bottom; null for a row without an icon. */
    private static List<Ikon> rowIcons(NavigationTree<?> tree) {
        List<Ikon> icons = new ArrayList<>();
        for (TreeCell<?> cell : shownCells(tree)) {
            Node icon = cell.lookup(".navigation-tree-icon");
            icons.add(icon instanceof FontIcon fontIcon ? fontIcon.getIconCode() : null);
        }
        return icons;
    }

    /** The filled cells the tree shows, top to bottom. */
    private static List<TreeCell<?>> shownCells(NavigationTree<?> tree) {
        relayOut(tree);
        TreeView<?> view = tree.getTreeView();
        List<TreeCell<?>> cells = new ArrayList<>();
        for (Node node : view.lookupAll(".tree-cell")) {
            if (node instanceof TreeCell<?> cell && !cell.isEmpty() && cell.getTreeItem() != null
                    && shown(cell, view)) {
                cells.add(cell);
            }
        }
        cells.sort((first, second) -> Double.compare(
                first.localToScene(0, 0).getY(), second.localToScene(0, 0).getY()));
        return cells;
    }

    /** The shown cell of a tree item. */
    private static TreeCell<?> cellOf(NavigationTree<?> tree, TreeItem<?> item) {
        for (TreeCell<?> cell : shownCells(tree)) {
            if (cell.getTreeItem() == item) {
                return cell;
            }
        }
        throw new AssertionError("no row shows " + item.getValue());
    }

    /** A double click on a row, away from its arrow: two clicks, the second with the click count 2. */
    private static void doubleClick(NavigationTree<?> tree, TreeItem<?> item) {
        click(tree, item, false, 1);
        click(tree, item, false, 2);
    }

    /**
     * A click of the primary button on a row as the platform sends it: pressed, released, clicked. It lands on the
     * row's disclosure arrow, or on the row's right end, away from the arrow.
     */
    private static void click(NavigationTree<?> tree, TreeItem<?> item, boolean onArrow, int clickCount) {
        click(tree, cellOf(tree, item), onArrow, clickCount);
    }

    /** A click of the primary button on a shown row, away from its arrow, such as a row of a search's copy. */
    private static void click(NavigationTree<?> tree, TreeCell<?> cell, int clickCount) {
        click(tree, cell, false, clickCount);
    }

    /** A click of the primary button on a shown row: on its arrow, or on its right end. */
    private static void click(NavigationTree<?> tree, TreeCell<?> cell, boolean onArrow, int clickCount) {
        Node target = cell;
        double x;
        double y;
        if (onArrow) {
            target = cell.getDisclosureNode();
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
        relayOut(tree);
    }

    private static void fireMouse(Node target, EventType<MouseEvent> type, double x, double y, int clickCount) {
        boolean primaryDown = type == MouseEvent.MOUSE_PRESSED;
        Event.fireEvent(target, new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, clickCount, false, false, false,
                false, primaryDown, false, false, false, false, true, null));
    }

    /** Gives the inner tree view the focus and presses a key there, without modifiers. */
    private static void press(NavigationTree<?> tree, KeyCode code) {
        TreeView<?> view = tree.getTreeView();
        view.requestFocus();
        assertSame(view, view.getScene().getFocusOwner(), "the tree view did not take the focus");
        Event.fireEvent(view, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
        relayOut(tree);
    }

    /** Gives a node the focus and presses a key there, with Ctrl or without a modifier. */
    private static void pressAt(Node node, KeyCode code, boolean ctrl) {
        node.requestFocus();
        assertSame(node, node.getScene().getFocusOwner(), "the node did not take the focus");
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, ctrl, false, false));
    }

    /** A region filled with a theme token, so that a test compares against the token, not against its value. */
    private static Region swatch(String token) {
        Region swatch = new Region();
        swatch.setStyle("-fx-background-color: " + token + ";");
        return swatch;
    }

    /** The colour of a swatch, after a CSS pass with the theme. */
    private static Paint fill(Region swatch) {
        return swatch.getBackground().getFills().get(0).getFill();
    }

    /** No background, or only transparent fills. */
    private static void assertNoBackground(Background background) {
        if (background == null) {
            return;
        }
        for (BackgroundFill fill : background.getFills()) {
            assertEquals(Color.TRANSPARENT, fill.getFill(), "a fill of the background");
        }
    }

    /** No border, or only borders without width or colour. */
    private static void assertNoBorder(Border border) {
        if (border == null) {
            return;
        }
        for (BorderStroke stroke : border.getStrokes()) {
            boolean transparent = Color.TRANSPARENT.equals(stroke.getTopStroke())
                    && Color.TRANSPARENT.equals(stroke.getRightStroke())
                    && Color.TRANSPARENT.equals(stroke.getBottomStroke())
                    && Color.TRANSPARENT.equals(stroke.getLeftStroke());
            BorderWidths widths = stroke.getWidths();
            boolean noWidth = widths.getTop() == 0 && widths.getRight() == 0 && widths.getBottom() == 0
                    && widths.getLeft() == 0;
            assertTrue(transparent || noWidth, "a stroke of the border");
        }
    }

    /** Whether the navigation tree shows the node: it lies inside the tree, and it and its parents are visible. */
    private static boolean shows(NavigationTree<?> tree, Node node) {
        relayOut(tree);
        for (Node at = node; at != null; at = at.getParent()) {
            if (at == tree) {
                return true;
            }
            if (!at.isVisible()) {
                return false;
            }
        }
        return false;
    }

    /** Whether the node and every parent up to the tree view are visible. */
    private static boolean shown(Node node, Node upTo) {
        for (Node at = node; at != null && at != upTo; at = at.getParent()) {
            if (!at.isVisible()) {
                return false;
            }
        }
        return true;
    }
}
