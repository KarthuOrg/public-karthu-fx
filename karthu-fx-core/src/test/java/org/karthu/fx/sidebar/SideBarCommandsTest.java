package org.karthu.fx.sidebar;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.Command;
import org.karthu.fx.command.CommandContext;
import org.karthu.fx.command.CommandGroup;
import org.karthu.fx.command.CommandRegistry;
import org.karthu.fx.command.Shortcut;
import org.karthu.fx.theme.ThemeManager;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.Shortcut.Modifier.ALT;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;

/**
 * The side bar's commands through its public interface and the registry's, in a themed scene that is never shown: the
 * commands it registers with their keys, running them by their keys or a bound menu item, where Hide Side Bar holds,
 * the keys switched off, tabs added, removed and hidden after registering, the texts, the stripe's tooltips, and where
 * the focus goes when the panel hides.
 */
class SideBarCommandsTest {

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

    // --- Registering ---------------------------------------------------------------------------------

    @Test
    @DisplayName("registering adds Show and the title per tab on Alt+1 to Alt+9 in stripe order, Hide Side Bar on "
            + "Shift+Escape in the side bar's own context, and the search without a key")
    void commandsListedWithTheirKeys() {
        SideBar sideBar = new SideBar();
        SideBarTab outline = tab("outline", "Outline");
        outline.setVisible(false);
        sideBar.getTabs().addAll(tab("files", "Files"), tab("search", "Search"), outline);
        Scene scene = laidOut(sideBar);
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup view = registry.addGroup("view", "View");

        sideBar.registerCommands(registry, view, "sidebar");

        assertEquals(List.of("sidebar.show.files", "sidebar.show.search", "sidebar.show.outline", "sidebar.hide",
                "sidebar.search"), ids(registry));
        assertEquals(List.of("Show Files", "Show Search", "Show Outline", "Hide Side Bar", "Search in Side Bar"),
                names(registry));
        assertEquals(Arrays.asList(Shortcut.of(KeyCode.DIGIT1, ALT), Shortcut.of(KeyCode.DIGIT2, ALT),
                Shortcut.of(KeyCode.DIGIT3, ALT), Shortcut.of(KeyCode.ESCAPE, SHIFT), null), defaults(registry),
                "a hidden tab keeps its place in the order");
        for (Command command : registry.getCommands()) {
            assertSame(view, command.getGroup());
        }
        CommandContext sideBarContext = command(registry, "sidebar.hide").getContext();
        assertSame(sideBar, sideBarContext.getNode(), "Hide Side Bar holds while the focus is in the side bar");
        assertEquals("Side Bar", sideBarContext.getName());
        assertEquals("sidebar", sideBarContext.getId());
        assertSame(registry.getWindowContext(), command(registry, "sidebar.show.files").getContext());
        assertSame(registry.getWindowContext(), command(registry, "sidebar.search").getContext());
        assertFalse(command(registry, "sidebar.show.files").isDisabled());
        assertTrue(command(registry, "sidebar.show.outline").isDisabled(), "a hidden tab's command is disabled");
    }

    // --- Running ------------------------------------------------------------------------------------

    @Test
    @DisplayName("Alt+1 opens the first tab with the focus in its content, Alt+2 switches, the open tab's key "
            + "collapses and expands again, and a key the user chose runs it instead")
    void tabCommandsRunOnTheirKeys() {
        SideBarTab files = tab("files", "Files");
        SideBarTab search = tab("search", "Search");
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, search);
        Scene scene = laidOut(sideBar);
        CommandRegistry registry = registered(sideBar, scene);
        applicationContent(scene).requestFocus();

        pressOnFocusOwner(scene, KeyCode.DIGIT1, ALT);
        assertSame(files, sideBar.getSelectedTab());
        assertFalse(sideBar.isCollapsed());
        assertTrue(panelShows(sideBar));
        assertSame(files.getContent(), scene.getFocusOwner(), "the focus in the tab's content");

        pressOnFocusOwner(scene, KeyCode.DIGIT2, ALT);
        assertSame(search, sideBar.getSelectedTab(), "another tab's key switches");
        assertSame(search.getContent(), scene.getFocusOwner());

        pressOnFocusOwner(scene, KeyCode.DIGIT2, ALT);
        assertTrue(sideBar.isCollapsed(), "the open tab's key collapses the side bar");
        assertSame(search, sideBar.getSelectedTab());
        assertFalse(panelShows(sideBar));

        applicationContent(scene).requestFocus();
        pressOnFocusOwner(scene, KeyCode.DIGIT2, ALT);
        assertFalse(sideBar.isCollapsed(), "and expands it again");
        assertSame(search.getContent(), scene.getFocusOwner());

        registry.setChangedShortcuts(Map.of("sidebar.show.files", "Ctrl+1"));
        pressOnFocusOwner(scene, KeyCode.DIGIT1, ALT);
        assertSame(search, sideBar.getSelectedTab(), "the suggested key no longer runs it");
        pressOnFocusOwner(scene, KeyCode.DIGIT1, CTRL);
        assertSame(files, sideBar.getSelectedTab(), "the key the user chose does");
    }

    // --- Hide Side Bar -------------------------------------------------------------------------------

    @Test
    @DisplayName("Shift+Escape hides the side bar while the focus is in it, in the tree and in its search field too, "
            + "and the focus goes back to where it was before; outside the side bar it does nothing")
    void hideSideBarHoldsInTheSideBar() {
        TreeItem<String> root = new TreeItem<>("root");
        root.getChildren().add(new TreeItem<>("Apple"));
        NavigationTree<String> tree = new NavigationTree<>(root);
        SideBarTab fruits = new SideBarTab("fruits", "Fruits", BootstrapIcons.BASKET, tree);
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(fruits, tab("notes", "Notes"));
        Scene scene = laidOut(sideBar);
        registered(sideBar, scene);
        Node outside = applicationContent(scene);
        outside.requestFocus();

        pressOnFocusOwner(scene, KeyCode.DIGIT1, ALT);
        assertSame(tree.getTreeView(), scene.getFocusOwner());
        pressOnFocusOwner(scene, KeyCode.ESCAPE, SHIFT);
        assertTrue(sideBar.isCollapsed(), "Shift+Escape in the tree hides the side bar");
        assertSame(fruits, sideBar.getSelectedTab(), "and keeps the selected tab");
        assertFalse(panelShows(sideBar));
        assertSame(outside, scene.getFocusOwner(), "the focus is back where it was");

        pressOnFocusOwner(scene, KeyCode.DIGIT1, ALT);
        tree.getSearchInput().getTextField().setText("app");
        tree.focusSearch();
        pressOnFocusOwner(scene, KeyCode.ESCAPE, SHIFT);
        assertTrue(sideBar.isCollapsed(), "Shift+Escape in the search field hides the side bar");
        assertEquals("app", tree.getSearchInput().getTextField().getText(), "and keeps the search");
        assertSame(outside, scene.getFocusOwner());

        pressOnFocusOwner(scene, KeyCode.DIGIT2, ALT);
        pressOnFocusOwner(scene, KeyCode.ESCAPE, SHIFT);
        assertTrue(sideBar.isCollapsed(), "in any content");

        sideBar.setCollapsed(false);
        outside.requestFocus();
        pressOnFocusOwner(scene, KeyCode.ESCAPE, SHIFT);
        assertFalse(sideBar.isCollapsed(), "outside the side bar its key does nothing");
    }

    // --- Search in Side Bar -------------------------------------------------------------------------

    @Test
    @DisplayName("Search in Side Bar expands the side bar and focuses the selected tab's search, or its content "
            + "without a search; it selects no tab, and is disabled while none is selected")
    void searchCommandFocusesTheSearch() {
        TreeItem<String> root = new TreeItem<>("root");
        root.getChildren().add(new TreeItem<>("Apple"));
        NavigationTree<String> tree = new NavigationTree<>(root);
        SideBarTab fruits = new SideBarTab("fruits", "Fruits", BootstrapIcons.BASKET, tree);
        SideBarTab notes = tab("notes", "Notes");
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(fruits, notes);
        Scene scene = laidOut(sideBar);
        CommandRegistry registry = registered(sideBar, scene);
        Command search = command(registry, "sidebar.search");
        applicationContent(scene).requestFocus();

        assertTrue(search.isDisabled(), "without a selected tab");
        run(search);
        assertNull(sideBar.getSelectedTab(), "it selects no tab");

        sideBar.setSelectedTab(fruits);
        sideBar.setCollapsed(true);
        assertFalse(search.isDisabled(), "a selected tab, collapsed or not");
        TextField field = tree.getSearchInput().getTextField();
        field.setText("app");
        run(search);
        assertFalse(sideBar.isCollapsed(), "it expands the side bar");
        assertSame(fruits, sideBar.getSelectedTab());
        assertSame(field, scene.getFocusOwner(), "the focus in the tab's search field");
        assertEquals("app", field.getSelectedText(), "its text selected, to be typed over");

        sideBar.setSelectedTab(notes);
        run(search);
        assertSame(notes.getContent(), scene.getFocusOwner(), "a content without a search takes the focus itself");

        notes.setVisible(false);
        assertTrue(search.isDisabled(), "hiding the selected tab selects none");
    }

    // --- Keys switched off --------------------------------------------------------------------------

    @Test
    @DisplayName("a second side bar registers its commands without keys, beside the first's, and the user records "
            + "keys for them")
    void commandsWithoutSuggestedKeys() {
        SideBar left = new SideBar();
        left.getTabs().add(tab("files", "Files"));
        SideBar right = new SideBar();
        right.getTabs().addAll(tab("outline", "Outline"), tab("notes", "Notes"));
        BorderPane window = new BorderPane(new TextField("the application's content"));
        window.setLeft(left);
        window.setRight(right);
        Scene scene = new Scene(window, 900, 600);
        ThemeManager.apply(scene);
        window.applyCss();
        window.layout();
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup view = registry.addGroup("view", "View");
        left.registerCommands(registry, view, "left");

        right.registerCommands(registry, view, "right", false);

        assertEquals(List.of("left.show.files", "left.hide", "left.search", "right.show.outline", "right.show.notes",
                "right.hide", "right.search"), ids(registry));
        assertEquals(Arrays.asList(null, null, null, null), defaults(registry).subList(3, 7), "no keys at all");
        assertEquals(Shortcut.of(KeyCode.DIGIT1, ALT), command(registry, "left.show.files").getShortcut());
        assertEquals("Notes", stripeButton(right, "Notes").getTooltip().getText(), "a tooltip without a key");

        registry.setChangedShortcuts(Map.of("right.show.notes", "Alt+0"));
        applicationContent(scene).requestFocus();
        pressOnFocusOwner(scene, KeyCode.DIGIT0, ALT);
        assertEquals("notes", right.getSelectedTab().getId(), "the key the user recorded opens the tab");
        assertEquals("Notes (Alt+0)", stripeButton(right, "Notes").getTooltip().getText());
    }

    // --- Tabs after registering ---------------------------------------------------------------------

    @Test
    @DisplayName("a tab added after registering gets its command at once, on the first of Alt+1 to Alt+9 no other "
            + "tab suggests, and none after the ninth")
    void tabAddedLater() {
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(tab("files", "Files"), tab("search", "Search"));
        Scene scene = laidOut(sideBar);
        CommandRegistry registry = registered(sideBar, scene);
        SideBarTab notes = tab("notes", "Notes");

        sideBar.getTabs().add(0, notes);

        Command showNotes = command(registry, "sidebar.show.notes");
        assertEquals("Show Notes", showNotes.getName());
        assertEquals(Shortcut.of(KeyCode.DIGIT3, ALT), showNotes.getDefaultShortcut(), "the first free number, "
                + "wherever the tab sits, so that it clashes with no other tab's");
        assertSame(registry.getWindowContext(), showNotes.getContext());
        assertFalse(showNotes.isDisabled());
        applicationContent(scene).requestFocus();
        pressOnFocusOwner(scene, KeyCode.DIGIT3, ALT);
        assertSame(notes, sideBar.getSelectedTab());
        assertSame(notes.getContent(), scene.getFocusOwner());

        for (int number = 4; number <= 10; number++) {
            sideBar.getTabs().add(tab("tab" + number, "Tab " + number));
        }
        assertEquals(Shortcut.of(KeyCode.DIGIT9, ALT), command(registry, "sidebar.show.tab9").getDefaultShortcut());
        assertNull(command(registry, "sidebar.show.tab10").getDefaultShortcut(), "the tenth tab has none");

        SideBarTab hidden = tab("hidden", "Hidden");
        hidden.setVisible(false);
        sideBar.getTabs().add(hidden);
        assertTrue(command(registry, "sidebar.show.hidden").isDisabled(), "a tab added hidden");
    }

    @Test
    @DisplayName("a removed or hidden tab's command is disabled and runs on no key, and is back when the tab is")
    void removedOrHiddenTabsCommandDisabled() {
        SideBarTab files = tab("files", "Files");
        SideBarTab search = tab("search", "Search");
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, search);
        Scene scene = laidOut(sideBar);
        CommandRegistry registry = registered(sideBar, scene);
        Command showFiles = command(registry, "sidebar.show.files");
        Command showSearch = command(registry, "sidebar.show.search");
        applicationContent(scene).requestFocus();

        sideBar.getTabs().remove(files);
        assertTrue(showFiles.isDisabled(), "a removed tab's command");
        pressOnFocusOwner(scene, KeyCode.DIGIT1, ALT);
        assertNull(sideBar.getSelectedTab(), "runs on no key");

        search.setVisible(false);
        assertTrue(showSearch.isDisabled(), "a hidden tab's command");
        run(showSearch);
        assertNull(sideBar.getSelectedTab(), "runs on no menu item either");

        search.setVisible(true);
        assertFalse(showSearch.isDisabled(), "shown again");
        SideBarTab newFiles = tab("files", "Folders");
        sideBar.getTabs().add(newFiles);
        assertFalse(showFiles.isDisabled(), "a tab with the id again");
        assertEquals("Show Folders", showFiles.getName(), "named after the tab with the id now");
        assertEquals(List.of("sidebar.show.files", "sidebar.show.search", "sidebar.hide", "sidebar.search"),
                ids(registry), "the same commands, none added");
        pressOnFocusOwner(scene, KeyCode.DIGIT1, ALT);
        assertSame(newFiles, sideBar.getSelectedTab(), "its key runs it");
    }

    // --- Tooltips -----------------------------------------------------------------------------------

    @Test
    @DisplayName("a stripe button's tooltip names its tab's key once the commands are registered, and follows the "
            + "key and the title")
    void tooltipsWithShortcuts() {
        SideBarTab files = tab("files", "Files");
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(files);
        Scene scene = laidOut(sideBar);
        assertEquals("Files", stripeButton(sideBar, "Files").getTooltip().getText(), "no commands, no key");

        CommandRegistry registry = registered(sideBar, scene);
        assertEquals("Files (Alt+1)", stripeButton(sideBar, "Files").getTooltip().getText());
        assertEquals("Files", stripeButton(sideBar, "Files").getAccessibleText(), "the accessible text stays the "
                + "title");

        registry.setChangedShortcuts(Map.of("sidebar.show.files", "Ctrl+1"));
        assertEquals("Files (Ctrl+1)", stripeButton(sideBar, "Files").getTooltip().getText(), "the user's key");
        files.setTitle("Folders");
        assertEquals("Folders (Ctrl+1)", stripeButton(sideBar, "Folders").getTooltip().getText());
        registry.setChangedShortcuts(Map.of("sidebar.show.files", ""));
        assertEquals("Folders", stripeButton(sideBar, "Folders").getTooltip().getText(), "a removed key");

        sideBar.getTabs().add(tab("notes", "Notes"));
        assertEquals("Notes (Alt+2)", stripeButton(sideBar, "Notes").getTooltip().getText(), "a tab added later");
        SideBarTab outline = tab("outline", "Outline");
        outline.setVisible(false);
        sideBar.getTabs().add(outline);
        outline.setVisible(true);
        assertEquals("Outline (Alt+3)", stripeButton(sideBar, "Outline").getTooltip().getText(), "a tab shown "
                + "later");
    }

    // --- Texts --------------------------------------------------------------------------------------

    @Test
    @DisplayName("the commands' names and their context's follow the side bar's texts and the tabs' titles, set "
            + "before or after registering")
    void namesFollowTheTexts() {
        SideBarTab files = tab("files", "Files");
        SideBarTab untitled = new SideBarTab("misc", null, BootstrapIcons.FOLDER, new TextField());
        SideBar sideBar = new SideBar();
        assertEquals("Show {0}", sideBar.getShowTabCommandFormat());
        assertEquals("Hide Side Bar", sideBar.getHideCommandName());
        assertEquals("Search in Side Bar", sideBar.getSearchCommandName());
        assertEquals("Side Bar", sideBar.getCommandContextName());
        sideBar.getTabs().addAll(files, untitled);
        sideBar.setHideCommandName("Seitenleiste ausblenden");
        Scene scene = laidOut(sideBar);
        CommandRegistry registry = registered(sideBar, scene);

        assertEquals(List.of("Show Files", "Show misc", "Seitenleiste ausblenden", "Search in Side Bar"),
                names(registry), "a tab without a title by its id");

        sideBar.setShowTabCommandFormat("{0} anzeigen");
        sideBar.setSearchCommandName("In Seitenleiste suchen");
        sideBar.setCommandContextName("Seitenleiste");
        files.setTitle("Dateien");
        assertEquals(List.of("Dateien anzeigen", "misc anzeigen", "Seitenleiste ausblenden",
                "In Seitenleiste suchen"), names(registry));
        assertEquals("Seitenleiste", command(registry, "sidebar.hide").getContext().getName());
    }

    // --- Refusals -----------------------------------------------------------------------------------

    @Test
    @DisplayName("registering refuses a missing or foreign argument and an id the registry has, adding nothing, and "
            + "a second registering")
    void registeringRefused() {
        SideBar sideBar = new SideBar();
        sideBar.getTabs().add(tab("files", "Files"));
        Scene scene = laidOut(sideBar);
        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup view = registry.addGroup("view", "View");
        registry.addCommand(view, "sidebar.show.files", "Taken", null, () -> { });
        registry.addContext("taken", "Taken", new Label());
        SideBar claimed = new SideBar();
        registry.addContext("claimed", "Claimed", claimed);
        CommandGroup foreign = new CommandRegistry(new Scene(new Label())).addGroup("view", "View");

        assertThrows(NullPointerException.class, () -> sideBar.registerCommands(null, view, "sidebar"));
        assertThrows(NullPointerException.class, () -> sideBar.registerCommands(registry, null, "sidebar"));
        assertThrows(NullPointerException.class, () -> sideBar.registerCommands(registry, view, null));
        assertThrows(IllegalArgumentException.class, () -> sideBar.registerCommands(registry, view, " "));
        assertThrows(IllegalArgumentException.class, () -> sideBar.registerCommands(registry, foreign, "left"),
                "another registry's group");
        assertThrows(IllegalArgumentException.class, () -> sideBar.registerCommands(registry, view, "sidebar"),
                "a command id the registry has");
        assertThrows(IllegalArgumentException.class, () -> sideBar.registerCommands(registry, view, "taken"),
                "a context id the registry has");
        assertThrows(IllegalArgumentException.class, () -> claimed.registerCommands(registry, view, "right"),
                "a side bar that is a context's node already");
        assertEquals(List.of("sidebar.show.files"), ids(registry), "nothing added");
        assertEquals(3, registry.getContexts().size());

        sideBar.registerCommands(registry, view, "left");
        assertThrows(IllegalStateException.class, () -> sideBar.registerCommands(registry, view, "again"),
                "a side bar registers once");
        assertEquals(List.of("sidebar.show.files", "left.show.files", "left.hide", "left.search"), ids(registry));
    }

    // --- Focus after hiding -------------------------------------------------------------------------

    @Test
    @DisplayName("the panel hiding with the focus in it, by the hide button, the menu, the stripe, the application "
            + "or the open tab going, gives the focus back to where it last was outside the side bar")
    void focusGoesBackOnHiding() {
        SideBarTab files = tab("files", "Files");
        SideBarTab notes = tab("notes", "Notes");
        SideBar sideBar = new SideBar();
        sideBar.getTabs().addAll(files, notes);
        Scene scene = laidOut(sideBar);
        TextField toolbarField = new TextField("a field of the application's tool bar");
        ((BorderPane) scene.getRoot()).setTop(toolbarField);
        relayOut(sideBar);
        Node outside = applicationContent(scene);
        outside.requestFocus();

        click(stripeButton(sideBar, "Files"));
        assertSame(files.getContent(), scene.getFocusOwner());
        click(sideBar.lookup(".side-bar-hide-button"));
        assertSame(outside, scene.getFocusOwner(), "the hide button");

        click(stripeButton(sideBar, "Files"));
        click(stripeButton(sideBar, "Files"));
        assertTrue(sideBar.isCollapsed());
        assertSame(outside, scene.getFocusOwner(), "a click on the open tab's stripe button");

        toolbarField.requestFocus();
        click(stripeButton(sideBar, "Files"));
        click(sideBar.lookup(".side-bar-menu-button"));
        List<MenuItem> menu = sideBar.getMenu().getItems();
        menu.get(menu.size() - 1).fire();
        assertTrue(sideBar.isCollapsed());
        assertSame(toolbarField, scene.getFocusOwner(), "the menu's Hide, back to where the focus was last");

        click(stripeButton(sideBar, "Files"));
        sideBar.setCollapsed(true);
        assertSame(toolbarField, scene.getFocusOwner(), "the application collapsing the side bar");

        click(stripeButton(sideBar, "Files"));
        sideBar.getTabs().remove(files);
        assertSame(toolbarField, scene.getFocusOwner(), "removing the open tab");

        click(stripeButton(sideBar, "Notes"));
        notes.setVisible(false);
        assertSame(toolbarField, scene.getFocusOwner(), "hiding the open tab");

        notes.setVisible(true);
        click(stripeButton(sideBar, "Notes"));
        outside.requestFocus();
        sideBar.setCollapsed(true);
        assertSame(outside, scene.getFocusOwner(), "a focus outside the side bar stays where it is");
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** A registry on the scene with a group View, and the side bar's commands in it under the prefix sidebar. */
    private static CommandRegistry registered(SideBar sideBar, Scene scene) {
        CommandRegistry registry = new CommandRegistry(scene);
        sideBar.registerCommands(registry, registry.addGroup("view", "View"), "sidebar");
        return registry;
    }

    /** A tab whose content is a plain text field. */
    private static SideBarTab tab(String id, String title) {
        return new SideBarTab(id, title, BootstrapIcons.FOLDER, new TextField(title));
    }

    /**
     * Puts the side bar into a themed scene that is never shown, at the left of a border pane beside a text field as
     * the application's content, and lays it out.
     */
    private static Scene laidOut(SideBar sideBar) {
        BorderPane root = new BorderPane(new TextField("the application's content"));
        root.setLeft(sideBar);
        Scene scene = new Scene(root, 900, 600);
        ThemeManager.apply(scene);
        root.applyCss();
        root.resize(900, 600);
        root.layout();
        return scene;
    }

    /** The application's content beside the side bar. */
    private static Node applicationContent(Scene scene) {
        return ((BorderPane) scene.getRoot()).getCenter();
    }

    /** Lays the side bar's scene out again, after a change. */
    private static void relayOut(SideBar sideBar) {
        Parent root = sideBar.getScene().getRoot();
        root.applyCss();
        root.layout();
    }

    /** The ids of the registry's commands, in their order. */
    private static List<String> ids(CommandRegistry registry) {
        List<String> ids = new ArrayList<>();
        for (Command command : registry.getCommands()) {
            ids.add(command.getId());
        }
        return ids;
    }

    /** The names of the registry's commands, in their order. */
    private static List<String> names(CommandRegistry registry) {
        List<String> names = new ArrayList<>();
        for (Command command : registry.getCommands()) {
            names.add(command.getName());
        }
        return names;
    }

    /** The default keys of the registry's commands, in their order, null for none. */
    private static List<Shortcut> defaults(CommandRegistry registry) {
        List<Shortcut> keys = new ArrayList<>();
        for (Command command : registry.getCommands()) {
            keys.add(command.getDefaultShortcut());
        }
        return keys;
    }

    /** The registry's command with an id. */
    private static Command command(CommandRegistry registry, String id) {
        for (Command command : registry.getCommands()) {
            if (command.getId().equals(id)) {
                return command;
            }
        }
        throw new AssertionError("no command " + id);
    }

    /** Runs a command as a bound menu item does when the user chooses it. */
    private static void run(Command command) {
        MenuItem item = new MenuItem();
        command.bind(item);
        item.fire();
    }

    /** Presses a key where the platform sends it: on the node with the focus. Ctrl is the platform's shortcut key. */
    private static void pressOnFocusOwner(Scene scene, KeyCode code, Shortcut.Modifier... modifiers) {
        Node focusOwner = scene.getFocusOwner();
        assertTrue(focusOwner != null, "nothing has the focus");
        Set<Shortcut.Modifier> down = Set.of(modifiers);
        boolean ctrl = down.contains(Shortcut.Modifier.CTRL);
        boolean mac = System.getProperty("os.name", "").startsWith("Mac");
        focusOwner.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, down.contains(SHIFT), ctrl && !mac,
                down.contains(ALT), ctrl && mac));
    }

    /** The stripe button of the tab with a title, among the shown ones. */
    private static ToggleButton stripeButton(SideBar sideBar, String title) {
        relayOut(sideBar);
        for (Node node : sideBar.lookupAll(".side-bar-tab")) {
            if (node instanceof ToggleButton button && title.equals(button.getAccessibleText())
                    && button.getParent() != null) {
                return button;
            }
        }
        throw new AssertionError("no stripe button for " + title);
    }

    /** Whether the side bar shows its panel. */
    private static boolean panelShows(SideBar sideBar) {
        relayOut(sideBar);
        Node panel = sideBar.lookup(".side-bar-panel");
        return panel.isVisible() && panel.isManaged();
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

    private static void fireMouse(Node target, EventType<MouseEvent> type, double x, double y) {
        boolean primaryDown = type == MouseEvent.MOUSE_PRESSED;
        Event.fireEvent(target, new MouseEvent(type, x, y, x, y, MouseButton.PRIMARY, 1, false, false, false,
                false, primaryDown, false, false, false, false, true, null));
    }
}
