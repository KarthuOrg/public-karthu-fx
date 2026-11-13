package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for icons-showcase.fxml.
 * Creates icon displays with labels.
 */
public class IconsShowcaseController {

    @FXML
    private HBox commonIconsContainer;

    @FXML
    private HBox actionIconsContainer;

    @FXML
    private void initialize() {
        // Common icons
        addIconEntry(commonIconsContainer, BootstrapIcons.ENVELOPE, "Mail");
        addIconEntry(commonIconsContainer, BootstrapIcons.FOLDER, "Folder");
        addIconEntry(commonIconsContainer, BootstrapIcons.FILE_EARMARK, "File");
        addIconEntry(commonIconsContainer, BootstrapIcons.TRASH, "Delete");
        addIconEntry(commonIconsContainer, BootstrapIcons.ARROW_CLOCKWISE, "Refresh");
        addIconEntry(commonIconsContainer, BootstrapIcons.GEAR, "Settings");
        addIconEntry(commonIconsContainer, BootstrapIcons.SEARCH, "Search");
        addIconEntry(commonIconsContainer, BootstrapIcons.DOWNLOAD, "Download");

        // Action icons
        addIconEntry(actionIconsContainer, BootstrapIcons.REPLY, "Reply");
        addIconEntry(actionIconsContainer, BootstrapIcons.REPLY_ALL, "Reply All");
        addIconEntry(actionIconsContainer, BootstrapIcons.FORWARD, "Forward");
        addIconEntry(actionIconsContainer, BootstrapIcons.PRINTER, "Print");
        addIconEntry(actionIconsContainer, BootstrapIcons.PAPERCLIP, "Attach");
        addIconEntry(actionIconsContainer, BootstrapIcons.STAR, "Star");
        addIconEntry(actionIconsContainer, BootstrapIcons.FLAG, "Flag");
    }

    private void addIconEntry(HBox container, BootstrapIcons icon, String labelText) {
        FontIcon fontIcon = FontIcon.of(icon, 16);

        Label label = new Label(labelText);
        label.setFont(Font.font(10));

        VBox entry = new VBox(4, fontIcon, label);
        entry.setAlignment(Pos.CENTER);
        entry.setPrefWidth(50);

        container.getChildren().add(entry);
    }
}
