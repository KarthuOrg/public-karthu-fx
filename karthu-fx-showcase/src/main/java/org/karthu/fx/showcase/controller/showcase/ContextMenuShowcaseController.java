package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Region;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for context-menu-showcase.fxml.
 * Builds a full context menu programmatically.
 */
public class ContextMenuShowcaseController {

    @FXML
    private Region contextArea;

    @FXML
    private void initialize() {
        ContextMenu contextMenu = new ContextMenu();

        // Cut, Copy, Paste
        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setGraphic(FontIcon.of(BootstrapIcons.SCISSORS, 14));

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setGraphic(FontIcon.of(BootstrapIcons.FILES, 14));

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setGraphic(FontIcon.of(BootstrapIcons.CLIPBOARD, 14));

        // Export submenu
        Menu exportMenu = new Menu("Export");
        exportMenu.setGraphic(FontIcon.of(BootstrapIcons.BOX_ARROW_UP, 14));
        exportMenu.getItems().addAll(
                new MenuItem("PDF"),
                new MenuItem("CSV"),
                new MenuItem("JSON")
        );

        // View submenu
        Menu viewMenu = new Menu("View");
        viewMenu.setGraphic(FontIcon.of(BootstrapIcons.EYE, 14));
        viewMenu.getItems().addAll(
                new MenuItem("Details"),
                new MenuItem("List"),
                new MenuItem("Grid")
        );

        // Delete — destructive action, styled via the .destructive convention
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setGraphic(FontIcon.of(BootstrapIcons.TRASH, 14));
        deleteItem.getStyleClass().add("destructive");

        // Disabled item
        MenuItem disabledItem = new MenuItem("Disabled Item");
        disabledItem.setDisable(true);

        // Select All
        MenuItem selectAllItem = new MenuItem("Select All");

        contextMenu.getItems().addAll(
                cutItem, copyItem, pasteItem,
                new SeparatorMenuItem(),
                exportMenu, viewMenu,
                new SeparatorMenuItem(),
                deleteItem, disabledItem,
                new SeparatorMenuItem(),
                selectAllItem
        );

        contextArea.setOnContextMenuRequested(event ->
                contextMenu.show(contextArea, event.getScreenX(), event.getScreenY())
        );
    }
}
