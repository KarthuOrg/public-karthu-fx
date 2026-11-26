package org.karthu.fx.showcase.controller.showcase;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.showcase.controller.showcase.SideBarShowcaseController.SampleEntry;
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
 * The page of the side bar in the group <i>Components</i>, loaded from its FXML and laid out with the theme in a scene
 * that is never shown, no menu ever shown: the side bar of the example window with its three tabs, what the trees
 * open, the notes' own header button and menu item, the switch of the side and the read-out of the state an
 * application keeps.
 */
class SideBarShowcaseTest {

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
    @DisplayName("the example window holds a side bar at the left of its content with the tabs Project, Guide and "
            + "Notes, Project open; the trees open by double click and by selection, Notes is a list of its own")
    void threeTabsProjectOpen() throws IOException {
        Parent root = layOut();
        SideBar sideBar = sideBar(root);

        assertSame(sideBar, exampleWindow(root).getLeft());
        assertEquals(Side.LEFT, sideBar.getSide());
        assertEquals(List.of("project", "guide", "notes"), ids(sideBar));
        assertEquals(List.of("Project", "Guide", "Notes"), titles(sideBar));
        assertEquals("project", sideBar.getSelectedTab().getId());
        assertFalse(sideBar.isCollapsed());
        assertTrue(stripeButton(sideBar, "Project").isSelected(), "the open tab's stripe button is marked");

        NavigationTree<?> project = assertInstanceOf(NavigationTree.class, tab(sideBar, "project").getContent());
        assertEquals(OpenMode.DOUBLE_CLICK, project.getOpenMode());
        NavigationTree<?> guide = assertInstanceOf(NavigationTree.class, tab(sideBar, "guide").getContent());
        assertEquals(OpenMode.SELECTION, guide.getOpenMode());
        assertInstanceOf(ListView.class, tab(sideBar, "notes").getContent());
        assertEquals("Opened: nothing yet", openedLabel(root).getText());
    }

    @Test
    @DisplayName("Project opens a file with Enter and names it in the read-out, a folder only unfolds; its search "
            + "finds a file by its kind too")
    void projectOpensAFile() throws IOException {
        Parent root = layOut();
        NavigationTree<SampleEntry> project = tree(sideBar(root), "project");

        project.select(new SampleEntry("README.md", false));
        press(project.getTreeView(), KeyCode.ENTER);
        assertEquals("Opened: README.md (Project)", openedLabel(root).getText());

        project.select(SampleEntry.folder("docs"));
        TreeItem<SampleEntry> docs = project.getTreeView().getSelectionModel().getSelectedItem();
        assertFalse(docs.isExpanded(), "docs starts folded");
        press(project.getTreeView(), KeyCode.ENTER);
        assertTrue(docs.isExpanded(), "Enter unfolds the folder");
        assertEquals("Opened: README.md (Project)", openedLabel(root).getText(), "a folder opens nothing");

        project.getSearchInput().setText("markdown");
        press(project.getSearchInput().getTextField(), KeyCode.ENTER);
        assertEquals("Opened: components.md (Project)", openedLabel(root).getText(), "the first Markdown file");
    }

    @Test
    @DisplayName("Guide opens a page as soon as the user selects it, here the first hit of its search by Enter; a "
            + "chapter opens nothing")
    void guideOpensOnSelection() throws IOException {
        Parent root = layOut();
        SideBar sideBar = sideBar(root);
        click(stripeButton(sideBar, "Guide"));
        assertEquals("guide", sideBar.getSelectedTab().getId());
        NavigationTree<SampleEntry> guide = tree(sideBar, "guide");
        assertSame(guide.getTreeView(), root.getScene().getFocusOwner(), "the focus in the tab's tree");

        guide.getSearchInput().setText("components");
        press(guide.getSearchInput().getTextField(), KeyCode.ENTER);
        assertEquals("Opened: nothing yet", openedLabel(root).getText(), "the chapter Components opens nothing");

        guide.getSearchInput().setText("first");
        press(guide.getSearchInput().getTextField(), KeyCode.ENTER);
        assertEquals("Opened: First Window (Guide)", openedLabel(root).getText());
    }

    @Test
    @DisplayName("Notes has a New Note button of its tab in the header, before the menu and hide buttons, and Clear "
            + "Notes in its menu above Hide; a navigation tree adds Expand All and Collapse All instead")
    void notesHeaderButtonAndMenuItem() throws IOException {
        Parent root = layOut();
        SideBar sideBar = sideBar(root);
        assertEquals(List.of("Project", "Expand All", "Collapse All", "More", "Hide"), header(sideBar));

        click(stripeButton(sideBar, "Notes"));
        @SuppressWarnings("unchecked")
        ListView<String> notes = (ListView<String>) tab(sideBar, "notes").getContent();
        assertEquals(List.of("Notes", "New Note", "More", "Hide"), header(sideBar));
        assertEquals(3, notes.getItems().size());

        click(headerButton(sideBar, "New Note"));
        assertEquals(List.of("Call the printer service", "Review the release notes", "Order new keyboards", "Note 4"),
                notes.getItems());
        assertEquals("Note 4", notes.getSelectionModel().getSelectedItem());

        click(sideBar.lookup(".side-bar-menu-button"));
        List<MenuItem> menu = sideBar.getMenu().getItems();
        assertEquals(3, menu.size());
        assertEquals("Clear Notes", menu.get(0).getText());
        assertInstanceOf(SeparatorMenuItem.class, menu.get(1));
        assertEquals("Hide", menu.get(2).getText());
        menu.get(0).fire();
        assertEquals(List.of(), notes.getItems(), "Clear Notes empties the list");
    }

    @Test
    @DisplayName("the switch puts the side bar at the right of the content, mirrored, and back at the left")
    void sideSwitch() throws IOException {
        Parent root = layOut();
        SideBar sideBar = sideBar(root);
        BorderPane window = exampleWindow(root);
        RadioButton left = sideButton(root, "Left");
        RadioButton right = sideButton(root, "Right");
        assertTrue(left.isSelected());

        click(right);
        assertEquals(Side.RIGHT, sideBar.getSide());
        assertSame(sideBar, window.getRight());
        assertNull(window.getLeft());

        click(left);
        assertEquals(Side.LEFT, sideBar.getSide());
        assertSame(sideBar, window.getLeft());
        assertNull(window.getRight());
    }

    @Test
    @DisplayName("the read-out names what an application keeps, the selected tab's id, collapsed and the width, and "
            + "follows every change")
    void stateReadOut() throws IOException {
        Parent root = layOut();
        SideBar sideBar = sideBar(root);
        Label state = assertInstanceOf(Label.class, root.lookup("#stateLabel"));
        assertEquals("To keep: selected tab project, collapsed false, content width 260 px", state.getText());

        click(sideBar.lookup(".side-bar-hide-button"));
        assertEquals("To keep: selected tab project, collapsed true, content width 260 px", state.getText());
        click(stripeButton(sideBar, "Guide"));
        assertEquals("To keep: selected tab guide, collapsed false, content width 260 px", state.getText());
        sideBar.setContentWidth(320);
        assertEquals("To keep: selected tab guide, collapsed false, content width 320 px", state.getText());
        tab(sideBar, "guide").setVisible(false);
        assertEquals("To keep: selected tab none, collapsed true, content width 320 px", state.getText());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** The switch of the side, a radio button by its text. */
    private static RadioButton sideButton(Parent root, String text) {
        for (Node child : assertInstanceOf(HBox.class, root.lookup("#sideSwitch")).getChildren()) {
            if (child instanceof RadioButton button && button.getText().equals(text)) {
                return button;
            }
        }
        throw new AssertionError("no switch " + text);
    }

    /** The header row of the open tab, left to right: the title, then each button by its tooltip. */
    private static List<String> header(SideBar sideBar) {
        layOut(sideBar.getScene().getRoot());
        HBox header = assertInstanceOf(HBox.class, sideBar.lookup(".side-bar-header"));
        List<String> parts = new ArrayList<>();
        for (Node part : header.getChildren()) {
            if (part instanceof Label title) {
                parts.add(title.getText());
            } else {
                parts.add(((Button) part).getTooltip().getText());
            }
        }
        return parts;
    }

    /** The button of the open tab's header with a tooltip. */
    private static Button headerButton(SideBar sideBar, String tooltip) {
        HBox header = assertInstanceOf(HBox.class, sideBar.lookup(".side-bar-header"));
        for (Node part : header.getChildren()) {
            if (part instanceof Button button && tooltip.equals(button.getTooltip().getText())) {
                return button;
            }
        }
        throw new AssertionError("no header button " + tooltip);
    }

    /** The navigation tree of a tab. */
    @SuppressWarnings("unchecked")
    private static NavigationTree<SampleEntry> tree(SideBar sideBar, String id) {
        return (NavigationTree<SampleEntry>) assertInstanceOf(NavigationTree.class, tab(sideBar, id).getContent());
    }

    /** Presses a key without modifiers on a node, as the platform sends it to the node with the focus. */
    private static void press(Node node, KeyCode code) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }

    /** Loads the page and lays it out in the theme, in a scene that is never shown. */
    private static Parent layOut() throws IOException {
        URL url = SideBarShowcaseTest.class.getResource("/org/karthu/fx/showcase/showcase/side-bar-showcase.fxml");
        assertNotNull(url, "the page's FXML");
        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root, 1400, 900);
        ThemeManager.apply(scene);
        layOut(root);
        return root;
    }

    /**
     * Two passes, as two pulses of a shown window: a tree builds its rows in the first, the second styles them. The
     * page takes its own size first, which a scene that is never shown does not give it.
     */
    private static void layOut(Parent root) {
        for (int pass = 0; pass < 2; pass++) {
            root.applyCss();
            root.autosize();
            root.layout();
        }
    }

    private static BorderPane exampleWindow(Parent root) {
        return assertInstanceOf(BorderPane.class, root.lookup("#exampleWindow"));
    }

    private static SideBar sideBar(Parent root) {
        return assertInstanceOf(SideBar.class, root.lookup(".side-bar"));
    }

    private static Label openedLabel(Parent root) {
        return assertInstanceOf(Label.class, root.lookup("#openedLabel"));
    }

    private static List<String> ids(SideBar sideBar) {
        List<String> ids = new ArrayList<>();
        for (SideBarTab tab : sideBar.getTabs()) {
            ids.add(tab.getId());
        }
        return ids;
    }

    private static List<String> titles(SideBar sideBar) {
        List<String> titles = new ArrayList<>();
        for (SideBarTab tab : sideBar.getTabs()) {
            titles.add(tab.getTitle());
        }
        return titles;
    }

    private static SideBarTab tab(SideBar sideBar, String id) {
        for (SideBarTab tab : sideBar.getTabs()) {
            if (tab.getId().equals(id)) {
                return tab;
            }
        }
        throw new AssertionError("no tab " + id);
    }

    /** The stripe button of the tab with a title, among the shown ones. */
    private static ToggleButton stripeButton(SideBar sideBar, String title) {
        layOut(sideBar.getScene().getRoot());
        for (Node node : sideBar.lookupAll(".side-bar-tab")) {
            if (node instanceof ToggleButton button && title.equals(button.getAccessibleText())
                    && button.getParent() != null) {
                return button;
            }
        }
        throw new AssertionError("no stripe button for " + title);
    }

    /** A click of the primary button on a node as the platform sends it: pressed, released, clicked. */
    private static void click(Node target) {
        Bounds bounds = target.localToScene(target.getLayoutBounds());
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        fireMouse(target, MouseEvent.MOUSE_PRESSED, x, y);
        fireMouse(target, MouseEvent.MOUSE_RELEASED, x, y);
        fireMouse(target, MouseEvent.MOUSE_CLICKED, x, y);
        layOut(target.getScene().getRoot());
    }

    private static void fireMouse(Node target, EventType<MouseEvent> type, double x, double y) {
        boolean primaryDown = type == MouseEvent.MOUSE_PRESSED;
        Event.fireEvent(target, new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false,
                false, primaryDown, false, false, false, false, true, null));
    }
}
