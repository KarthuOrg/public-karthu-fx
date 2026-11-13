package org.karthu.fx.showcase.controller.showcase;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

/**
 * Controller for combo-box-showcase.fxml.
 * Populates ComboBox instances with sample data.
 */
public class ComboBoxShowcaseController {

    @FXML
    private ComboBox<String> normalCombo;

    @FXML
    private ComboBox<String> unselectedCombo;

    @FXML
    private ComboBox<String> editableCombo;

    @FXML
    private ComboBox<String> disabledCombo;

    @FXML
    private void initialize() {
        normalCombo.setItems(FXCollections.observableArrayList("Option 1", "Option 2", "Option 3", "Option 4"));
        normalCombo.getSelectionModel().selectFirst();

        unselectedCombo.setItems(FXCollections.observableArrayList("Option 1", "Option 2", "Option 3", "Option 4"));

        editableCombo.setItems(FXCollections.observableArrayList("Option 1", "Option 2", "Option 3", "Option 4"));
        editableCombo.setEditable(true);
        editableCombo.getSelectionModel().selectFirst();

        disabledCombo.setItems(FXCollections.observableArrayList("Option 1", "Option 2", "Option 3", "Option 4"));
        disabledCombo.getSelectionModel().selectFirst();
        disabledCombo.setDisable(true);
    }
}
