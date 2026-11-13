package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for hyperlink-showcase.fxml.
 * Creates icon hyperlinks and marks one as visited.
 */
public class HyperlinkShowcaseController {

    @FXML
    private Hyperlink visitedLink;

    @FXML
    private HBox iconLinksContainer;

    @FXML
    private void initialize() {
        visitedLink.setVisited(true);

        Hyperlink homeLink = new Hyperlink("Home");
        homeLink.setGraphic(FontIcon.of(BootstrapIcons.HOUSE, 14));

        Hyperlink settingsLink = new Hyperlink("Settings");
        settingsLink.setGraphic(FontIcon.of(BootstrapIcons.GEAR, 14));

        Hyperlink downloadLink = new Hyperlink("Download");
        downloadLink.setGraphic(FontIcon.of(BootstrapIcons.DOWNLOAD, 14));

        Hyperlink externalLink = new Hyperlink("External");
        externalLink.setGraphic(FontIcon.of(BootstrapIcons.BOX_ARROW_UP_RIGHT, 14));

        iconLinksContainer.getChildren().addAll(homeLink, settingsLink, downloadLink, externalLink);
    }
}
