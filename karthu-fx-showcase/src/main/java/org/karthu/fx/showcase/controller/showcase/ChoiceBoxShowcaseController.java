package org.karthu.fx.showcase.controller.showcase;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

/**
 * Controller for choice-box-showcase.fxml.
 * Populates ChoiceBox instances with sample data.
 */
public class ChoiceBoxShowcaseController {

    @FXML
    private ChoiceBox<String> normalChoice;

    @FXML
    private ChoiceBox<String> unselectedChoice;

    @FXML
    private ChoiceBox<String> disabledChoice;

    @FXML
    private void initialize() {
        normalChoice.setItems(FXCollections.observableArrayList("Choice 1", "Choice 2", "Choice 3", "Choice 4"));
        normalChoice.getSelectionModel().selectFirst();

        unselectedChoice.setItems(FXCollections.observableArrayList("Choice 1", "Choice 2", "Choice 3", "Choice 4"));

        disabledChoice.setItems(FXCollections.observableArrayList("Choice 1", "Choice 2", "Choice 3", "Choice 4"));
        disabledChoice.getSelectionModel().selectFirst();
        disabledChoice.setDisable(true);
    }
}
