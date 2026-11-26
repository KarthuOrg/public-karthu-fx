package org.karthu.fx.sidebar;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.theme.ThemeManager;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The side bar through its public interface, in a themed scene that is never shown: the stripe's buttons, which tab
 * is open and whether the side bar is collapsed, the panel's header and menu, the focus, both sides and the look.
 */
class SideBarTest {

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

    // --- Stripe --------------------------------------------------------------------------------------

    @Test
    @DisplayName("the stripe shows one button per visible tab in the order of the tabs, the title as tooltip and "
            + "accessible text")
    void stripeButtonsForVisibleTabs() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBarTab search = tab("search", "Search", BootstrapIcons.SEARCH);
        SideBarTab outline = tab("outline", "Outline", BootstrapIcons.LIST_UL);
        outline.setVisible(false);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, search, outline);
        laidOut(sideBar);

        assertEquals(List.of("Files", "Search"), stripeTitles(sideBar));
        ToggleButton first = stripeButtons(sideBar).get(0);
        assertEquals("Files", first.getTooltip().getText());
        assertEquals("Files", first.getAccessibleText());
        assertEquals(BootstrapIcons.FOLDER, ((FontIcon) first.getGraphic()).getIconCode());
        assertTrue(sideBar.getStyleClass().contains("side-bar"));

        outline.setVisible(true);
        assertEquals(List.of("Files", "Search", "Outline"), stripeTitles(sideBar), "a tab shown again");
        sideBar.getTabs().remove(files);
        assertEquals(List.of("Search", "Outline"), stripeTitles(sideBar), "a removed tab");
        sideBar.getTabs().add(0, files);
        assertEquals(List.of("Files", "Search", "Outline"), stripeTitles(sideBar), "a tab added at the top");

        search.setTitle("Find");
        search.setIcon(BootstrapIcons.BINOCULARS);
        ToggleButton second = stripeButtons(sideBar).get(1);
        assertEquals("Find", second.getTooltip().getText(), "the tooltip follows the title");
        assertEquals("Find", second.getAccessibleText());
        assertEquals(BootstrapIcons.BINOCULARS, ((FontIcon) second.getGraphic()).getIconCode());
    }

    @Test
    @DisplayName("a tab has a stable id, never null or blank, and is visible by default")
    void tabId() {
        SideBarTab files = new SideBarTab("files", "Files", null, null);

        assertEquals("files", files.getId());
        assertTrue(files.isVisible());
        assertNull(files.getIcon());
        assertNull(files.getContent());
        assertThrows(NullPointerException.class, () -> new SideBarTab(null, "Files", null, null));
        assertThrows(IllegalArgumentException.class, () -> new SideBarTab(" ", "Files", null, null));
    }

    // --- Open tab and collapsing ---------------------------------------------------------------------

    @Test
    @DisplayName("a click opens a tab beside the content, a click on another switches, a click on the open one "
            + "collapses and expands again")
    void clickSelectsSwitchesCollapsesAndExpands() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBarTab search = tab("search", "Search", BootstrapIcons.SEARCH);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, search);
        laidOut(sideBar);

        assertNull(sideBar.getSelectedTab(), "no tab is selected at first");
        assertFalse(sideBar.isCollapsed());
        assertFalse(panelShows(sideBar), "without a selected tab only the stripe shows");
        assertEquals(List.of(false, false), stripeMarks(sideBar));

        click(stripeButton(sideBar, "Files"));
        assertSame(files, sideBar.getSelectedTab());
        assertFalse(sideBar.isCollapsed());
        assertTrue(panelShows(sideBar));
        assertSame(files.getContent(), shownContent(sideBar));
        assertEquals(List.of(true, false), stripeMarks(sideBar));

        click(stripeButton(sideBar, "Search"));
        assertSame(search, sideBar.getSelectedTab(), "a click on another tab switches");
        assertSame(search.getContent(), shownContent(sideBar));
        assertEquals(List.of(false, true), stripeMarks(sideBar));

        click(stripeButton(sideBar, "Search"));
        assertTrue(sideBar.isCollapsed(), "a click on the open tab collapses");
        assertSame(search, sideBar.getSelectedTab(), "collapsing keeps the selected tab");
        assertFalse(panelShows(sideBar));
        assertEquals(List.of(false, false), stripeMarks(sideBar), "collapsed, no button is marked");

        click(stripeButton(sideBar, "Search"));
        assertFalse(sideBar.isCollapsed(), "a click on the selected tab expands again");
        assertTrue(panelShows(sideBar));
        assertEquals(List.of(false, true), stripeMarks(sideBar));

        click(stripeButton(sideBar, "Search"));
        click(stripeButton(sideBar, "Files"));
        assertSame(files, sideBar.getSelectedTab(), "a click on another tab while collapsed selects it");
        assertFalse(sideBar.isCollapsed(), "and expands");
        assertEquals(List.of(true, false), stripeMarks(sideBar));
    }

    @Test
    @DisplayName("removing or hiding the selected tab collapses the side bar and selects no tab, another tab does not")
    void removingOrHidingTheSelectedTab() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBarTab search = tab("search", "Search", BootstrapIcons.SEARCH);
        SideBarTab outline = tab("outline", "Outline", BootstrapIcons.LIST_UL);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, search, outline);
        sideBar.setSelectedTab(files);
        laidOut(sideBar);
        assertTrue(panelShows(sideBar));

        sideBar.getTabs().remove(files);
        assertNull(sideBar.getSelectedTab(), "removing the open tab selects no tab");
        assertTrue(sideBar.isCollapsed(), "and collapses");
        assertFalse(panelShows(sideBar));
        assertNull(shownContent(sideBar), "the removed tab's content left the panel");

        click(stripeButton(sideBar, "Search"));
        search.setVisible(false);
        assertNull(sideBar.getSelectedTab(), "hiding the open tab selects no tab");
        assertTrue(sideBar.isCollapsed(), "and collapses");
        assertEquals(List.of("Outline"), stripeTitles(sideBar));

        click(stripeButton(sideBar, "Outline"));
        search.setVisible(true);
        sideBar.getTabs().add(files);
        files.setVisible(false);
        sideBar.getTabs().remove(search);
        assertSame(outline, sideBar.getSelectedTab(), "hiding or removing another tab keeps the open one");
        assertFalse(sideBar.isCollapsed());
        assertTrue(panelShows(sideBar));

        click(stripeButton(sideBar, "Outline"));
        assertSame(outline, sideBar.getSelectedTab());
        outline.setVisible(false);
        assertNull(sideBar.getSelectedTab(), "hiding the selected tab of a collapsed side bar selects no tab");
        assertTrue(sideBar.isCollapsed());
    }

    @Test
    @DisplayName("the application restores the selected tab and the collapsed flag, and reads back the user's")
    void stateProperties() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBarTab search = tab("search", "Search", BootstrapIcons.SEARCH);
        SideBar sideBar = new SideBar();
        sideBar.setSelectedTab(search);
        sideBar.setCollapsed(true);
        sideBar.getTabs().addAll(files, search);
        laidOut(sideBar);

        assertSame(search, sideBar.getSelectedTab(), "a tab selected before it was added stays selected");
        assertFalse(panelShows(sideBar), "restored collapsed");
        assertEquals(List.of(false, false), stripeMarks(sideBar));

        sideBar.setCollapsed(false);
        assertTrue(panelShows(sideBar), "expanded, the restored tab shows");
        assertEquals(List.of(false, true), stripeMarks(sideBar));

        List<String> changes = new ArrayList<>();
        sideBar.selectedTabProperty().addListener((observable, oldTab, newTab) -> changes.add("tab " + newTab.getId()));
        sideBar.collapsedProperty().addListener((observable, was, is) -> changes.add("collapsed " + is));
        click(stripeButton(sideBar, "Files"));
        click(stripeButton(sideBar, "Files"));
        assertEquals(List.of("tab files", "collapsed true"), changes, "the user's choices, for the application to "
                + "store");
        assertEquals("files", sideBar.getSelectedTab().getId());
    }

    @Test
    @DisplayName("the open panel sits beside the content, 260 px wide, and the content starts where its resize edge "
            + "ends")
    void panelDockedBesideTheContent() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        laidOut(sideBar);
        Node content = ((BorderPane) sideBar.getScene().getRoot()).getCenter();
        double collapsedWidth = sideBar.getWidth();

        sideBar.setSelectedTab(files);
        relayOut(sideBar);

        assertTrue(panelShows(sideBar), "selecting a tab of an expanded side bar shows its panel");
        assertEquals(260, panel(sideBar).getWidth());
        assertEquals(collapsedWidth + 260 + resizeEdge(sideBar).getWidth(), sideBar.getWidth(), 0.5,
                "the stripe, the panel and its resize edge");
        assertEquals(sideBar.getWidth(), content.getLayoutX(), 0.5, "the content is narrower, never covered");

        sideBar.setCollapsed(true);
        relayOut(sideBar);
        assertFalse(panelShows(sideBar));
        assertFalse(resizeEdge(sideBar).isVisible() || resizeEdge(sideBar).isManaged(), "nor its resize edge");
        assertEquals(collapsedWidth, content.getLayoutX(), 0.5, "collapsed, the content gets the room at once");
    }

    // --- Side ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("on the left the stripe lies at the window's left edge, on the right the side bar is mirrored")
    void bothSides() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        sideBar.setSelectedTab(files);
        assertEquals(Side.LEFT, sideBar.getSide(), "the default side");
        Scene left = laidOut(sideBar);
        Node leftContent = ((BorderPane) left.getRoot()).getCenter();

        assertEquals(0, sceneBounds(stripe(sideBar)).getMinX(), 0.5, "the stripe at the window's edge");
        assertEquals(sceneBounds(stripe(sideBar)).getMaxX(), sceneBounds(panel(sideBar)).getMinX(), 0.5,
                "the panel beside it");
        assertEquals(sceneBounds(panel(sideBar)).getMaxX(), sceneBounds(resizeEdge(sideBar)).getMinX(), 0.5,
                "the resize edge on the panel's inner side");
        assertEquals(sceneBounds(resizeEdge(sideBar)).getMaxX(), sceneBounds(leftContent).getMinX(), 0.5,
                "the content beside it");
        assertTrue(sideBar.getPseudoClassStates().contains(PseudoClass.getPseudoClass("left")));

        sideBar.setSide(Side.RIGHT);
        Scene right = laidOut(sideBar);
        Node rightContent = ((BorderPane) right.getRoot()).getCenter();

        assertEquals(Side.RIGHT, sideBar.getSide());
        assertEquals(900, sceneBounds(stripe(sideBar)).getMaxX(), 0.5, "the stripe at the window's right edge");
        assertEquals(sceneBounds(stripe(sideBar)).getMinX(), sceneBounds(panel(sideBar)).getMaxX(), 0.5,
                "the panel to its left");
        assertEquals(sceneBounds(panel(sideBar)).getMinX(), sceneBounds(resizeEdge(sideBar)).getMaxX(), 0.5,
                "the resize edge on the panel's inner side, its left");
        assertEquals(sceneBounds(resizeEdge(sideBar)).getMinX(), sceneBounds(rightContent).getMaxX(), 0.5,
                "the content to its left");
        assertTrue(sideBar.getPseudoClassStates().contains(PseudoClass.getPseudoClass("right")));
        assertFalse(sideBar.getPseudoClassStates().contains(PseudoClass.getPseudoClass("left")));
        assertEquals(List.of(true), stripeMarks(sideBar));
    }

    @Test
    @DisplayName("a side bar refuses the top and the bottom, and no side at all")
    void otherSidesRefused() {
        SideBar sideBar = new SideBar();
        sideBar.setSide(Side.RIGHT);

        assertThrows(IllegalArgumentException.class, () -> sideBar.setSide(Side.TOP));
        assertThrows(IllegalArgumentException.class, () -> sideBar.setSide(Side.BOTTOM));
        assertThrows(IllegalArgumentException.class, () -> sideBar.sideProperty().set(null));
        assertEquals(Side.RIGHT, sideBar.getSide(), "a refused side changes nothing");
    }

    // --- Width ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("dragging the resize edge on the panel's inner side makes the panel wider or narrower")
    void dragTheEdgeOnTheLeft() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        sideBar.setSelectedTab(files);
        Scene scene = laidOut(sideBar);
        Node content = ((BorderPane) scene.getRoot()).getCenter();
        Region edge = resizeEdge(sideBar);
        assertEquals(Cursor.H_RESIZE, edge.getCursor(), "the horizontal resize cursor");

        drag(edge, 20, 40);
        relayOut(sideBar);
        assertEquals(300, sideBar.getContentWidth(), "dragged to the right: wider");
        assertEquals(300, panel(sideBar).getWidth());
        assertEquals(sceneBounds(edge).getMaxX(), sceneBounds(content).getMinX(), 0.5, "the content narrower");

        drag(edge, -90);
        relayOut(sideBar);
        assertEquals(210, sideBar.getContentWidth(), "dragged to the left: narrower");
        assertEquals(210, panel(sideBar).getWidth());
        assertFalse(sideBar.isCollapsed());

        drag(MouseButton.SECONDARY, edge, 40);
        assertEquals(210, sideBar.getContentWidth(), "a drag with another button leaves the width");
    }

    @Test
    @DisplayName("on the right the resize edge lies on the panel's left: dragging it to the left makes the panel wider")
    void dragTheEdgeOnTheRight() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.setSide(Side.RIGHT);
        sideBar.getTabs().add(files);
        sideBar.setSelectedTab(files);
        Scene scene = laidOut(sideBar);
        Node content = ((BorderPane) scene.getRoot()).getCenter();
        Region edge = resizeEdge(sideBar);
        assertEquals(Cursor.H_RESIZE, edge.getCursor());

        drag(edge, -25, -50);
        relayOut(sideBar);
        assertEquals(310, sideBar.getContentWidth(), "dragged to the left: wider");
        assertEquals(310, panel(sideBar).getWidth());
        assertEquals(sceneBounds(edge).getMinX(), sceneBounds(content).getMaxX(), 0.5, "the content narrower");
        assertEquals(900, sceneBounds(stripe(sideBar)).getMaxX(), 0.5, "the stripe stays at the window's edge");

        drag(edge, 70.4);
        relayOut(sideBar);
        assertEquals(240, sideBar.getContentWidth(), "dragged to the right: narrower, in whole pixels");
        assertEquals(240, panel(sideBar).getWidth());
    }

    @Test
    @DisplayName("a drag stops at the largest and at the smallest width, and never collapses the side bar")
    void dragWithinTheLimits() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        sideBar.setSelectedTab(files);
        laidOut(sideBar);
        Region edge = resizeEdge(sideBar);
        assertEquals(160, sideBar.getMinContentWidth(), "the smallest width by default");
        assertEquals(600, sideBar.getMaxContentWidth(), "the largest width by default");

        drag(edge, 500);
        relayOut(sideBar);
        assertEquals(600, sideBar.getContentWidth(), "dragged beyond the largest width: it stops there");
        assertEquals(600, panel(sideBar).getWidth());

        drag(edge, -1000);
        assertEquals(160, sideBar.getContentWidth(), "dragged below the smallest width: it stops there");
        assertFalse(sideBar.isCollapsed(), "and does not collapse");
        assertTrue(panelShows(sideBar));
        assertEquals(160, panel(sideBar).getWidth());

        drag(edge, -100, 50);
        assertEquals(210, sideBar.getContentWidth(), "back from below the limit, the edge follows the pointer again");

        sideBar.setMinContentWidth(200);
        sideBar.setMaxContentWidth(400);
        drag(edge, 1000);
        assertEquals(400, sideBar.getContentWidth(), "the application's largest width");
        drag(edge, -1000);
        assertEquals(200, sideBar.getContentWidth(), "the application's smallest width");
    }

    @Test
    @DisplayName("the width holds for every tab: switching tabs, collapsing and expanding never move the content")
    void widthSharedByTheTabs() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        Region wideContent = new Region();
        wideContent.setPrefWidth(500);
        SideBarTab wide = new SideBarTab("wide", "Wide", BootstrapIcons.ARROWS_ANGLE_EXPAND, wideContent);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, wide);
        sideBar.setSelectedTab(files);
        Scene scene = laidOut(sideBar);
        Node content = ((BorderPane) scene.getRoot()).getCenter();
        drag(resizeEdge(sideBar), 60);
        relayOut(sideBar);
        double contentLeft = sceneBounds(content).getMinX();

        click(stripeButton(sideBar, "Wide"));
        relayOut(sideBar);
        assertEquals(320, sideBar.getContentWidth());
        assertEquals(320, panel(sideBar).getWidth(), "another tab, a content that would rather be wider");
        assertEquals(contentLeft, sceneBounds(content).getMinX(), 0.5, "the content did not move");

        click(stripeButton(sideBar, "Wide"));
        click(stripeButton(sideBar, "Files"));
        relayOut(sideBar);
        assertEquals(320, panel(sideBar).getWidth(), "collapsed and expanded on another tab");
        assertEquals(contentLeft, sceneBounds(content).getMinX(), 0.5);
    }

    @Test
    @DisplayName("the application sets the width, kept within the limits it sets, and reads back the user's")
    void widthSetByTheApplication() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        sideBar.setSelectedTab(files);
        assertEquals(260, sideBar.getContentWidth(), "the default width");

        sideBar.setContentWidth(340);
        laidOut(sideBar);
        assertEquals(340, panel(sideBar).getWidth(), "restored before the side bar shows");

        sideBar.setContentWidth(1000);
        assertEquals(600, sideBar.getContentWidth(), "beyond the largest width: the largest");
        sideBar.setContentWidth(50);
        assertEquals(160, sideBar.getContentWidth(), "below the smallest width: the smallest");
        relayOut(sideBar);
        assertEquals(160, panel(sideBar).getWidth());

        sideBar.setMinContentWidth(200);
        assertEquals(200, sideBar.getContentWidth(), "a smallest width above the width raises it");
        sideBar.setContentWidth(500);
        sideBar.setMaxContentWidth(450);
        assertEquals(450, sideBar.getContentWidth(), "a largest width below the width lowers it");
        sideBar.setMinContentWidth(480);
        assertEquals(480, sideBar.getContentWidth(), "limits that cross: the smallest wins, as in JavaFX's layout");
        relayOut(sideBar);
        assertEquals(480, panel(sideBar).getWidth());

        sideBar.setMaxContentWidth(600);
        sideBar.setMinContentWidth(160);
        assertEquals(480, sideBar.getContentWidth(), "wider limits leave the width");
        List<Double> stored = new ArrayList<>();
        sideBar.contentWidthProperty().addListener((observable, was, is) -> stored.add(is.doubleValue()));
        drag(resizeEdge(sideBar), 10, 20);
        assertEquals(List.of(490.0, 500.0), stored, "the user's width, for the application to store");
    }

    @Test
    @DisplayName("a bound width shows within the limits, and the resize edge leaves it to the binding")
    void boundWidth() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        sideBar.setSelectedTab(files);
        DoubleProperty applicationWidth = new SimpleDoubleProperty(300);
        sideBar.contentWidthProperty().bind(applicationWidth);
        laidOut(sideBar);
        assertEquals(300, panel(sideBar).getWidth());

        drag(resizeEdge(sideBar), 50);
        relayOut(sideBar);
        assertEquals(300, sideBar.getContentWidth(), "a drag does not move a bound width");
        assertEquals(300, panel(sideBar).getWidth());

        applicationWidth.set(900);
        relayOut(sideBar);
        assertEquals(600, panel(sideBar).getWidth(), "a bound width beyond the largest shows at the largest");
        applicationWidth.set(10);
        relayOut(sideBar);
        assertEquals(160, panel(sideBar).getWidth(), "below the smallest at the smallest");
    }

    @Test
    @DisplayName("a width that is no number, and a limit below zero, infinite or no number, are refused")
    void widthsRefused() {
        SideBar sideBar = new SideBar();

        assertThrows(IllegalArgumentException.class, () -> sideBar.setContentWidth(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> sideBar.setMinContentWidth(-1));
        assertThrows(IllegalArgumentException.class, () -> sideBar.setMinContentWidth(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> sideBar.setMinContentWidth(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> sideBar.setMaxContentWidth(-1));
        assertThrows(IllegalArgumentException.class, () -> sideBar.setMaxContentWidth(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> sideBar.setMaxContentWidth(Double.POSITIVE_INFINITY));
        assertEquals(260, sideBar.getContentWidth(), "a refused value changes nothing");
        assertEquals(160, sideBar.getMinContentWidth());
        assertEquals(600, sideBar.getMaxContentWidth());

        sideBar.setContentWidth(Double.POSITIVE_INFINITY);
        assertEquals(600, sideBar.getContentWidth(), "an infinite width is the largest");
        sideBar.setMinContentWidth(0);
        sideBar.setContentWidth(Double.NEGATIVE_INFINITY);
        assertEquals(0, sideBar.getContentWidth(), "a smallest width of zero is allowed");
    }

    // --- Focus ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("opening a tab by a click puts the focus into its content: a navigation tree's on its selection")
    void openingByClickFocusesTheContent() {
        TreeItem<String> root = new TreeItem<>("root");
        root.getChildren().add(new TreeItem<>("Apple"));
        root.getChildren().add(new TreeItem<>("Banana"));
        NavigationTree<String> tree = new NavigationTree<>(root);
        tree.select("Banana");
        SideBarTab fruits = new SideBarTab("fruits", "Fruits", BootstrapIcons.BASKET, tree);
        TextField note = new TextField();
        SideBarTab notes = new SideBarTab("notes", "Notes", BootstrapIcons.STICKY, note);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(fruits, notes);
        Scene scene = laidOut(sideBar);
        Node applicationContent = ((BorderPane) scene.getRoot()).getCenter();
        applicationContent.requestFocus();

        sideBar.setSelectedTab(fruits);
        assertSame(applicationContent, scene.getFocusOwner(), "the application's selection moves no focus");

        sideBar.setCollapsed(true);
        click(stripeButton(sideBar, "Fruits"));
        assertSame(tree.getTreeView(), scene.getFocusOwner(), "expanded by a click: into the tree");
        assertEquals("Banana", tree.getTreeView().getFocusModel().getFocusedItem().getValue(), "on its selection");

        click(stripeButton(sideBar, "Notes"));
        assertSame(note, scene.getFocusOwner(), "any other content takes the focus itself");

        click(stripeButton(sideBar, "Notes"));
        applicationContent.requestFocus();
        click(stripeButton(sideBar, "Notes"));
        assertSame(note, scene.getFocusOwner(), "expanded again by a click");
    }

    // --- Header --------------------------------------------------------------------------------------

    @Test
    @DisplayName("the header row holds the title, the tab's buttons, the content's buttons, More and Hide, the "
            + "content below")
    void headerOrder() {
        NavigationTree<String> tree = new NavigationTree<>(new TreeItem<>("root"));
        SideBarTab pages = new SideBarTab("pages", "Pages", BootstrapIcons.BOOK, tree);
        Button add = iconButton(BootstrapIcons.PLUS);
        pages.getHeaderButtons().add(add);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(pages);
        sideBar.setSelectedTab(pages);
        laidOut(sideBar);

        List<Node> row = headerRow(sideBar);
        assertEquals(6, row.size(), "title, the tab's button, Expand All, Collapse All, More, Hide");
        Label title = (Label) row.get(0);
        assertTrue(title.getStyleClass().contains("side-bar-title"));
        assertEquals("Pages", title.getText());
        assertSame(add, row.get(1), "the tab's own buttons first");
        assertSame(tree.getHeaderButtons().get(0), row.get(2), "then the content's: Expand All");
        assertSame(tree.getHeaderButtons().get(1), row.get(3), "and Collapse All");
        Button more = (Button) row.get(4);
        assertTrue(more.getStyleClass().containsAll(List.of("icon-button", "side-bar-menu-button")));
        assertEquals(BootstrapIcons.THREE_DOTS_VERTICAL, ((FontIcon) more.getGraphic()).getIconCode());
        assertEquals("More", more.getTooltip().getText());
        assertEquals("More", more.getAccessibleText());
        Button hide = (Button) row.get(5);
        assertTrue(hide.getStyleClass().containsAll(List.of("icon-button", "side-bar-hide-button")));
        assertEquals(BootstrapIcons.DASH, ((FontIcon) hide.getGraphic()).getIconCode());
        assertEquals("Hide", hide.getTooltip().getText());
        assertEquals("Hide", hide.getAccessibleText());

        double rowCentre = centreY(title);
        double left = -1;
        for (Node node : row) {
            assertEquals(rowCentre, centreY(node), 1, "one row");
            double nodeLeft = node.localToScene(node.getLayoutBounds()).getMinX();
            assertTrue(nodeLeft > left, "left to right in that order");
            left = nodeLeft;
        }
        Bounds panelBounds = panel(sideBar).localToScene(panel(sideBar).getLayoutBounds());
        assertTrue(panelBounds.getMaxX() - hide.localToScene(hide.getLayoutBounds()).getMaxX() < 12,
                "the buttons sit at the right end of the header");
        assertTrue(tree.localToScene(tree.getLayoutBounds()).getMinY()
                >= hide.localToScene(hide.getLayoutBounds()).getMaxY(), "the content lies below the header");
        assertEquals(panelBounds.getMaxY(), tree.localToScene(tree.getLayoutBounds()).getMaxY(), 0.5,
                "and fills the rest of the panel");
    }

    @Test
    @DisplayName("the header follows the tab: its title, its buttons, another content, another tab")
    void headerFollowsTheTab() {
        NavigationTree<String> tree = new NavigationTree<>(new TreeItem<>("root"));
        SideBarTab pages = new SideBarTab("pages", "Pages", BootstrapIcons.BOOK, tree);
        SideBarTab notes = tab("notes", "Notes", BootstrapIcons.STICKY);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(pages, notes);
        sideBar.setSelectedTab(notes);
        laidOut(sideBar);

        assertEquals(List.of("Notes", "More", "Hide"), headerTexts(sideBar), "a content of no contract adds nothing");

        click(stripeButton(sideBar, "Pages"));
        assertEquals(List.of("Pages", "Expand All", "Collapse All", "More", "Hide"), headerTexts(sideBar));

        Button add = iconButton(BootstrapIcons.PLUS);
        add.setAccessibleText("New Page");
        pages.getHeaderButtons().add(add);
        pages.setTitle("Book");
        assertEquals(List.of("Book", "New Page", "Expand All", "Collapse All", "More", "Hide"),
                headerTexts(sideBar));

        TextField plain = new TextField();
        pages.setContent(plain);
        assertEquals(List.of("Book", "New Page", "More", "Hide"), headerTexts(sideBar), "another content");
        assertSame(plain, shownContent(sideBar));

        sideBar.setMoreText("Mehr");
        sideBar.setHideText("Ausblenden");
        assertEquals(List.of("Book", "New Page", "Mehr", "Ausblenden"), headerTexts(sideBar));
        assertEquals("Mehr", ((Button) headerRow(sideBar).get(2)).getTooltip().getText());
        assertEquals("Ausblenden", ((Button) headerRow(sideBar).get(3)).getTooltip().getText());
    }

    @Test
    @DisplayName("the hide button collapses the side bar at once and keeps the selected tab")
    void hideButtonCollapses() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        laidOut(sideBar);
        click(stripeButton(sideBar, "Files"));

        click(headerButton(sideBar, "side-bar-hide-button"));

        assertTrue(sideBar.isCollapsed());
        assertSame(files, sideBar.getSelectedTab());
        assertFalse(panelShows(sideBar));
        assertEquals(List.of(false), stripeMarks(sideBar));
    }

    // --- Menu ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("the menu button fills the tab's menu without showing it: its items, the content's items, a "
            + "separator and Hide")
    void menuItems() {
        MenuContent content = new MenuContent();
        SideBarTab files = new SideBarTab("files", "Files", BootstrapIcons.FOLDER, content);
        MenuItem newFile = new MenuItem("New File");
        files.getMenuItems().add(newFile);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        sideBar.setSelectedTab(files);
        laidOut(sideBar);

        click(headerButton(sideBar, "side-bar-menu-button"));

        ContextMenu menu = sideBar.getMenu();
        assertTrue(menu.getStyleClass().contains("side-bar-menu"));
        assertFalse(menu.isShowing(), "a window that is not shown shows no menu");
        List<MenuItem> items = menu.getItems();
        assertEquals(4, items.size());
        assertSame(newFile, items.get(0), "the tab's items first");
        assertSame(content.sortItem, items.get(1), "then the content's");
        assertTrue(items.get(2) instanceof SeparatorMenuItem);
        MenuItem hide = items.get(3);
        assertTrue(hide.getStyleClass().contains("side-bar-hide-item"));
        assertEquals("Hide", hide.getText());
        assertFalse(hide.isDisable());

        hide.fire();
        assertTrue(sideBar.isCollapsed(), "Hide collapses the side bar");
        assertSame(files, sideBar.getSelectedTab(), "and keeps the selected tab");

        sideBar.setCollapsed(false);
        sideBar.setHideText("Ausblenden");
        files.getMenuItems().clear();
        click(headerButton(sideBar, "side-bar-menu-button"));
        assertEquals(List.of(content.sortItem, sideBar.getMenu().getItems().get(1), hide),
                sideBar.getMenu().getItems(), "filled anew on each opening");
        assertEquals("Ausblenden", hide.getText());
    }

    @Test
    @DisplayName("without items of the tab or its content the menu holds Hide alone, no separator")
    void menuWithHideAlone() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        sideBar.setSelectedTab(files);
        laidOut(sideBar);

        click(headerButton(sideBar, "side-bar-menu-button"));

        List<MenuItem> items = sideBar.getMenu().getItems();
        assertEquals(1, items.size());
        assertEquals("Hide", items.get(0).getText());
    }

    @Test
    @DisplayName("a right click on the header or on a stripe button opens the menu of that tab")
    void rightClickOpensTheMenu() {
        NavigationTree<String> tree = new NavigationTree<>(new TreeItem<>("root"));
        SideBarTab files = new SideBarTab("files", "Files", BootstrapIcons.FOLDER, tree);
        MenuItem newFile = new MenuItem("New File");
        files.getMenuItems().add(newFile);
        SideBarTab notes = tab("notes", "Notes", BootstrapIcons.STICKY);
        MenuItem pin = new MenuItem("Pin Note");
        notes.getMenuItems().add(pin);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, notes);
        sideBar.setSelectedTab(files);
        laidOut(sideBar);

        rightClick(headerRow(sideBar).get(0));
        assertEquals(List.of("New File", "", "Hide"), menuTexts(sideBar), "on the header's title");
        sideBar.getMenu().getItems().clear();
        rightClick(tree.getHeaderButtons().get(0));
        assertEquals(List.of("New File", "", "Hide"), menuTexts(sideBar), "on a button of the header");

        rightClick(stripeButton(sideBar, "Notes"));
        assertEquals(List.of("Pin Note", "", "Hide"), menuTexts(sideBar), "on another tab's stripe button");
        assertTrue(sideBar.getMenu().getItems().get(2).isDisable(), "Hide while that tab is not open");
        assertSame(files, sideBar.getSelectedTab(), "a right click opens no tab");
        assertEquals(List.of(true, false), stripeMarks(sideBar));

        rightClick(stripeButton(sideBar, "Files"));
        assertEquals(List.of("New File", "", "Hide"), menuTexts(sideBar), "on the open tab's stripe button");
        assertFalse(sideBar.getMenu().getItems().get(2).isDisable());
        assertFalse(sideBar.isCollapsed(), "a right click collapses nothing");
    }

    // --- Look ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("the theme draws the stripe on the surface with a line towards the panel, its buttons flat and "
            + "square, the open tab's icon in the accent colour on a raised background, the resize edge with the "
            + "panel's line towards the content")
    void look() {
        SideBarTab files = tab("files", "Files", BootstrapIcons.FOLDER);
        SideBarTab search = tab("search", "Search", BootstrapIcons.SEARCH);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, search);
        sideBar.setSelectedTab(files);
        Region surface = swatch("-fx-surface-color");
        Region border = swatch("-fx-control-border");
        Region accent = swatch("-fx-accent-color");
        Region secondary = swatch("-fx-text-secondary-color");
        Region text = swatch("-fx-text-color");
        Region raised = swatch("-fx-button-color");
        laidOut(sideBar, surface, border, accent, secondary, text, raised);

        Region stripe = stripe(sideBar);
        assertEquals(fill(surface), stripe.getBackground().getFills().get(0).getFill(), "the stripe on the surface");
        BorderStroke line = stripe.getBorder().getStrokes().get(0);
        assertEquals(fill(border), line.getRightStroke(), "a line towards the panel");
        assertEquals(1, line.getWidths().getRight());
        assertEquals(0, line.getWidths().getLeft());

        ToggleButton open = stripeButton(sideBar, "Files");
        ToggleButton other = stripeButton(sideBar, "Search");
        assertEquals(open.getWidth(), open.getHeight(), "square");
        assertEquals(other.getWidth(), open.getWidth(), "all of one size");
        FontIcon openIcon = (FontIcon) open.getGraphic();
        FontIcon otherIcon = (FontIcon) other.getGraphic();
        assertEquals(16, openIcon.getIconSize());
        assertEquals(16, otherIcon.getIconSize());
        assertEquals(fill(accent), openIcon.getIconColor(), "the open tab's icon in the accent colour");
        assertEquals(fill(raised), open.getBackground().getFills().get(0).getFill(), "on a raised background");
        assertEquals(fill(secondary), otherIcon.getIconColor(), "the other icons in the secondary text colour");
        assertNoBackground(other.getBackground());
        assertNoBorder(other.getBorder());
        assertFalse(open.isFocusTraversable(), "the stripe is no stop of the Tab key");

        Label title = (Label) headerRow(sideBar).get(0);
        assertFalse(title.getFont().getStyle().contains("Bold"), "the title in regular weight");
        assertEquals(fill(text), title.getTextFill());
        FontIcon hideIcon = (FontIcon) headerButton(sideBar, "side-bar-hide-button").getGraphic();
        assertEquals(14, hideIcon.getIconSize());
        assertEquals(fill(secondary), hideIcon.getIconColor());
        assertEquals(fill(surface), panel(sideBar).getBackground().getFills().get(0).getFill(),
                "the panel on the surface");
        assertNoBorder(panel(sideBar).getBorder());
        Region edge = resizeEdge(sideBar);
        assertEquals(fill(surface), edge.getBackground().getFills().get(0).getFill(),
                "the resize edge continues the panel's surface");
        BorderStroke edgeLine = edge.getBorder().getStrokes().get(0);
        assertEquals(fill(border), edgeLine.getRightStroke(), "and draws the panel's line towards the content");
        assertEquals(1, edgeLine.getWidths().getRight());
        assertEquals(0, edgeLine.getWidths().getLeft());

        sideBar.setSide(Side.RIGHT);
        relayOut(sideBar);
        BorderStroke mirrored = stripe(sideBar).getBorder().getStrokes().get(0);
        assertEquals(1, mirrored.getWidths().getLeft(), "on the right the line lies on the stripe's left");
        assertEquals(0, mirrored.getWidths().getRight());
        BorderStroke mirroredEdgeLine = resizeEdge(sideBar).getBorder().getStrokes().get(0);
        assertEquals(1, mirroredEdgeLine.getWidths().getLeft(), "and on the resize edge's left");
        assertEquals(0, mirroredEdgeLine.getWidths().getRight());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** A content that offers a menu item through the contract, and nothing else. */
    private static final class MenuContent extends Label implements SideBarContent {

        final MenuItem sortItem = new MenuItem("Sort by Name");

        @Override
        public List<MenuItem> getMenuItems() {
            return List.of(sortItem);
        }
    }

    /** A tab whose content is a plain text field. */
    private static SideBarTab tab(String id, String title, Ikon icon) {
        return new SideBarTab(id, title, icon, new TextField(title));
    }

    /**
     * Puts the side bar into a themed scene that is never shown, at the left or right of a border pane as its side
     * says, beside a text field as the application's content, and lays it out.
     */
    private static Scene laidOut(SideBar sideBar, Node... others) {
        if (sideBar.getParent() instanceof BorderPane previous) {
            previous.getChildren().remove(sideBar);
        }
        BorderPane root = new BorderPane(new TextField("the application's content"));
        root.setTop(new HBox(others));
        if (sideBar.getSide() == Side.RIGHT) {
            root.setRight(sideBar);
        } else {
            root.setLeft(sideBar);
        }
        Scene scene = new Scene(root, 900, 600);
        ThemeManager.apply(scene);
        root.applyCss();
        root.resize(900, 600);
        root.layout();
        return scene;
    }

    /** Lays the side bar's scene out again, after a change. */
    private static void relayOut(SideBar sideBar) {
        Parent root = sideBar.getScene().getRoot();
        root.applyCss();
        root.layout();
    }

    /** The stripe's buttons the side bar shows, top to bottom. */
    private static List<ToggleButton> stripeButtons(SideBar sideBar) {
        relayOut(sideBar);
        List<ToggleButton> buttons = new ArrayList<>();
        for (Node node : sideBar.lookupAll(".side-bar-tab")) {
            if (node instanceof ToggleButton button && shown(button, sideBar)) {
                buttons.add(button);
            }
        }
        buttons.sort((first, second) -> Double.compare(
                first.localToScene(0, 0).getY(), second.localToScene(0, 0).getY()));
        return buttons;
    }

    /** The accessible texts of the stripe's buttons, top to bottom: the titles of the tabs they stand for. */
    private static List<String> stripeTitles(SideBar sideBar) {
        List<String> titles = new ArrayList<>();
        for (ToggleButton button : stripeButtons(sideBar)) {
            titles.add(button.getAccessibleText());
        }
        return titles;
    }

    /** The stripe button of the tab with a title. */
    private static ToggleButton stripeButton(SideBar sideBar, String title) {
        for (ToggleButton button : stripeButtons(sideBar)) {
            if (title.equals(button.getAccessibleText())) {
                return button;
            }
        }
        throw new AssertionError("no stripe button for " + title);
    }

    /** Whether each stripe button is marked as the open tab's, top to bottom. */
    private static List<Boolean> stripeMarks(SideBar sideBar) {
        List<Boolean> marks = new ArrayList<>();
        for (ToggleButton button : stripeButtons(sideBar)) {
            marks.add(button.isSelected());
        }
        return marks;
    }

    /** The stripe: the column of the tabs' buttons. */
    private static Region stripe(SideBar sideBar) {
        return (Region) sideBar.lookup(".side-bar-stripe");
    }

    /** A node's bounds in the scene. */
    private static Bounds sceneBounds(Node node) {
        return node.localToScene(node.getLayoutBounds());
    }

    /** The panel: the header and the content of the open tab. */
    private static Region panel(SideBar sideBar) {
        return (Region) sideBar.lookup(".side-bar-panel");
    }

    /** Whether the side bar shows its panel. */
    private static boolean panelShows(SideBar sideBar) {
        relayOut(sideBar);
        Region panel = panel(sideBar);
        return panel != null && shown(panel, sideBar) && panel.getWidth() > 0;
    }

    /** The node the panel shows below its header; null for none. */
    private static Node shownContent(SideBar sideBar) {
        Parent holder = (Parent) sideBar.lookup(".side-bar-content");
        List<Node> children = holder.getChildrenUnmodifiable();
        return children.isEmpty() ? null : children.get(0);
    }

    /** The nodes of the panel's header row, left to right. */
    private static List<Node> headerRow(SideBar sideBar) {
        relayOut(sideBar);
        Parent header = (Parent) sideBar.lookup(".side-bar-header");
        return header.getChildrenUnmodifiable();
    }

    /** The texts of the header row: the title's text, then each button's accessible text. */
    private static List<String> headerTexts(SideBar sideBar) {
        List<String> texts = new ArrayList<>();
        for (Node node : headerRow(sideBar)) {
            texts.add(node instanceof Label label ? label.getText() : node.getAccessibleText());
        }
        return texts;
    }

    /** The header's button with a style class. */
    private static Button headerButton(SideBar sideBar, String styleClass) {
        for (Node node : headerRow(sideBar)) {
            if (node.getStyleClass().contains(styleClass)) {
                return (Button) node;
            }
        }
        throw new AssertionError("no header button " + styleClass);
    }

    /** An application's header button: an icon button. */
    private static Button iconButton(Ikon icon) {
        Button button = new Button();
        button.getStyleClass().add("icon-button");
        button.setGraphic(FontIcon.of(icon));
        return button;
    }

    /** The texts of the menu's items, top to bottom; a separator's is empty. */
    private static List<String> menuTexts(SideBar sideBar) {
        List<String> texts = new ArrayList<>();
        for (MenuItem item : sideBar.getMenu().getItems()) {
            texts.add(item instanceof SeparatorMenuItem ? "" : item.getText());
        }
        return texts;
    }

    /** A right click on a node as the platform reports it: a request for a context menu at its centre. */
    private static void rightClick(Node target) {
        Bounds bounds = target.localToScene(target.getLayoutBounds());
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        Event.fireEvent(target, new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, x, y, x, y, false,
                null));
    }

    /** The vertical centre of a node in the scene. */
    private static double centreY(Node node) {
        return node.localToScene(node.getLayoutBounds()).getCenterY();
    }

    /** A click of the primary button on a node as the platform sends it: pressed, released, clicked. */
    private static void click(Node target) {
        Bounds bounds = target.localToScene(target.getLayoutBounds());
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        fireMouse(target, MouseEvent.MOUSE_PRESSED, x, y);
        fireMouse(target, MouseEvent.MOUSE_RELEASED, x, y);
        fireMouse(target, MouseEvent.MOUSE_CLICKED, x, y);
    }

    /** The resize edge on the panel's inner side. */
    private static Region resizeEdge(SideBar sideBar) {
        return (Region) sideBar.lookup(".side-bar-resize-edge");
    }

    /** A drag of the primary button on a node, as {@link #drag(MouseButton, Node, double...)} describes it. */
    private static void drag(Node target, double... distances) {
        drag(MouseButton.PRIMARY, target, distances);
    }

    /**
     * A drag of a button on a node as the platform sends it: pressed at its centre, dragged through each horizontal
     * distance from there in turn, released at the last. The platform sends the whole gesture to the node it was
     * pressed on, also where the pointer leaves it.
     */
    private static void drag(MouseButton button, Node target, double... distances) {
        Bounds bounds = target.localToScene(target.getLayoutBounds());
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        fireMouse(target, MouseEvent.MOUSE_PRESSED, button, x, y);
        double last = 0;
        for (double distance : distances) {
            fireMouse(target, MouseEvent.MOUSE_DRAGGED, button, x + distance, y);
            last = distance;
        }
        fireMouse(target, MouseEvent.MOUSE_RELEASED, button, x + last, y);
    }

    private static void fireMouse(Node target, EventType<MouseEvent> type, double x, double y) {
        fireMouse(target, type, MouseButton.PRIMARY, x, y);
    }

    private static void fireMouse(Node target, EventType<MouseEvent> type, MouseButton button, double x, double y) {
        boolean down = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
        boolean primaryDown = down && button == MouseButton.PRIMARY;
        boolean secondaryDown = down && button == MouseButton.SECONDARY;
        Event.fireEvent(target, new MouseEvent(type, x, y, x, y, button, 1, false, false, false, false, primaryDown,
                false, secondaryDown, false, false, true, null));
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

    /** Whether the node and every parent up to another node are visible and managed. */
    private static boolean shown(Node node, Node upTo) {
        for (Node at = node; at != null && at != upTo; at = at.getParent()) {
            if (!at.isVisible() || !at.isManaged()) {
                return false;
            }
        }
        return true;
    }
}
