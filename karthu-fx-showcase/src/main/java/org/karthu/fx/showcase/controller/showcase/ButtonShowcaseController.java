package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for button-showcase.fxml.
 * Creates icon buttons programmatically.
 */
public class ButtonShowcaseController {

    @FXML
    private HBox iconButtonsContainer;

    @FXML
    private void initialize() {
        Button closeBtn = new Button();
        closeBtn.setGraphic(FontIcon.of(BootstrapIcons.X, 14));
        closeBtn.getStyleClass().add("icon-button");

        Button plusBtn = new Button();
        plusBtn.setGraphic(FontIcon.of(BootstrapIcons.PLUS, 14));
        plusBtn.getStyleClass().add("icon-button");

        Button dashBtn = new Button();
        dashBtn.setGraphic(FontIcon.of(BootstrapIcons.DASH, 14));
        dashBtn.getStyleClass().add("icon-button");

        Button questionBtn = new Button();
        questionBtn.setGraphic(FontIcon.of(BootstrapIcons.QUESTION, 14));
        questionBtn.getStyleClass().add("icon-button");
        questionBtn.setDisable(true);

        iconButtonsContainer.getChildren().addAll(closeBtn, plusBtn, dashBtn, questionBtn);
    }
}
