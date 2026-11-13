package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for menu-bar-showcase.fxml.
 * Builds the full MenuBar programmatically.
 */
public class MenuBarShowcaseController {

    @FXML
    private VBox menuBarContainer;

    @FXML
    private void initialize() {
        MenuBar menuBar = new MenuBar();

        // ---- File menu ----
        Menu fileMenu = new Menu("File");

        MenuItem newItem = new MenuItem("New");
        newItem.setGraphic(FontIcon.of(BootstrapIcons.FILE_EARMARK_PLUS, 14));
        newItem.setAccelerator(KeyCombination.keyCombination("Shortcut+N"));

        MenuItem openItem = new MenuItem("Open");
        openItem.setGraphic(FontIcon.of(BootstrapIcons.FOLDER2_OPEN, 14));
        openItem.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));

        Menu openRecentMenu = new Menu("Open Recent");
        openRecentMenu.getItems().addAll(
                new MenuItem("document1.txt"),
                new MenuItem("document2.txt"),
                new MenuItem("document3.txt")
        );

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setGraphic(FontIcon.of(BootstrapIcons.CODE, 14));
        saveItem.setAccelerator(KeyCombination.keyCombination("Shortcut+S"));

        MenuItem saveAsItem = new MenuItem("Save As");
        saveAsItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Shift+S"));

        MenuItem exitItem = new MenuItem("Exit");

        fileMenu.getItems().addAll(
                newItem, openItem, openRecentMenu,
                new SeparatorMenuItem(),
                saveItem, saveAsItem,
                new SeparatorMenuItem(),
                exitItem
        );

        // ---- Edit menu ----
        Menu editMenu = new Menu("Edit");

        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setGraphic(FontIcon.of(BootstrapIcons.ARROW_COUNTERCLOCKWISE, 14));
        undoItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Z"));

        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setGraphic(FontIcon.of(BootstrapIcons.ARROW_CLOCKWISE, 14));
        redoItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Y"));

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(KeyCombination.keyCombination("Shortcut+X"));

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(KeyCombination.keyCombination("Shortcut+C"));

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(KeyCombination.keyCombination("Shortcut+V"));

        MenuItem disabledEditItem = new MenuItem("Disabled Item");
        disabledEditItem.setDisable(true);

        editMenu.getItems().addAll(
                undoItem, redoItem,
                new SeparatorMenuItem(),
                cutItem, copyItem, pasteItem,
                new SeparatorMenuItem(),
                disabledEditItem
        );

        // ---- View menu ----
        Menu viewMenu = new Menu("View");

        CheckMenuItem toolbarItem = new CheckMenuItem("Toolbar");
        toolbarItem.setSelected(true);

        CheckMenuItem statusBarItem = new CheckMenuItem("Status Bar");
        statusBarItem.setSelected(true);

        CheckMenuItem sidebarItem = new CheckMenuItem("Sidebar");

        viewMenu.getItems().addAll(toolbarItem, statusBarItem, sidebarItem);

        // ---- Options menu ----
        Menu optionsMenu = new Menu("Options");

        ToggleGroup viewToggleGroup = new ToggleGroup();

        RadioMenuItem listViewItem = new RadioMenuItem("List View");
        listViewItem.setToggleGroup(viewToggleGroup);
        listViewItem.setSelected(true);

        RadioMenuItem gridViewItem = new RadioMenuItem("Grid View");
        gridViewItem.setToggleGroup(viewToggleGroup);

        RadioMenuItem detailsViewItem = new RadioMenuItem("Details View");
        detailsViewItem.setToggleGroup(viewToggleGroup);

        optionsMenu.getItems().addAll(listViewItem, gridViewItem, detailsViewItem);

        // ---- Help menu ----
        Menu helpMenu = new Menu("Help");

        MenuItem helpItem = new MenuItem("Help");
        helpItem.setGraphic(FontIcon.of(BootstrapIcons.QUESTION_CIRCLE, 14));

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setGraphic(FontIcon.of(BootstrapIcons.INFO_CIRCLE, 14));

        helpMenu.getItems().addAll(helpItem, aboutItem);

        // Build menu bar
        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, optionsMenu, helpMenu);
        menuBarContainer.getChildren().addFirst(menuBar);
    }
}
