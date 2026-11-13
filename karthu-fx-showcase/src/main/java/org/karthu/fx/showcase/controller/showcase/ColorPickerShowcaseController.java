package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

/**
 * Controller for color-picker-showcase.fxml.
 * Updates the color label when the picker value changes.
 */
public class ColorPickerShowcaseController {

    @FXML
    private ColorPicker labelPicker;

    @FXML
    private Label colorLabel;

    @FXML
    private void initialize() {
        labelPicker.setOnAction(event -> {
            Color color = labelPicker.getValue();
            String hex = String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
            colorLabel.setText("Selected: " + hex);
        });
    }
}
