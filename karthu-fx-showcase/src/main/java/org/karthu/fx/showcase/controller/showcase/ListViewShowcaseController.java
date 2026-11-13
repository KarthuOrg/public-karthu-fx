package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

/**
 * Controller for list-view-showcase.fxml.
 * Populates a ListView with sample items.
 */
public class ListViewShowcaseController {

    @FXML
    private ListView<String> listView;

    @FXML
    private void initialize() {
        for (int i = 1; i <= 20; i++) {
            listView.getItems().add("List Item " + i);
        }
    }
}
