package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import org.karthu.fx.components.ToggleSwitch;

/**
 * Controller for toggle-switch-showcase.fxml.
 * Creates ToggleSwitch instances programmatically.
 */
public class ToggleSwitchShowcaseController {

    @FXML
    private HBox switchContainer;

    @FXML
    private void initialize() {
        // Off switch
        ToggleSwitch offSwitch = new ToggleSwitch(false);
        Label offLabel = new Label("Off");

        // On switch
        ToggleSwitch onSwitch = new ToggleSwitch(true);
        Label onLabel = new Label("On");

        // Interactive switch with dynamic label
        ToggleSwitch interactiveSwitch = new ToggleSwitch(false);
        Label interactiveLabel = new Label("OFF");
        interactiveSwitch.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            interactiveLabel.setText(isSelected ? "ON" : "OFF");
        });

        switchContainer.getChildren().addAll(
                offLabel, offSwitch,
                onLabel, onSwitch,
                interactiveLabel, interactiveSwitch
        );
    }
}
