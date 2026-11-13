package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller for scroll-pane-showcase.fxml.
 * Populates scroll content programmatically to demonstrate overflow.
 */
public class ScrollPaneShowcaseController {

    @FXML
    private VBox verticalContent;

    @FXML
    private VBox horizontalContent;

    @FXML
    private VBox bothContent;

    @FXML
    private void initialize() {
        // Vertical: many rows
        for (int i = 1; i <= 30; i++) {
            Label label = new Label("Item " + i + " — scrollable content row");
            verticalContent.getChildren().add(label);
        }

        // Horizontal: wide content
        HBox wideRow = new HBox(16);
        for (int i = 1; i <= 20; i++) {
            Label label = new Label("[Column " + i + "]");
            label.setMinWidth(100);
            wideRow.getChildren().add(label);
        }
        horizontalContent.getChildren().add(wideRow);

        // Both: wide and tall
        for (int i = 1; i <= 25; i++) {
            HBox row = new HBox(16);
            for (int j = 1; j <= 10; j++) {
                Label label = new Label("Cell " + i + "." + j);
                label.setMinWidth(80);
                row.getChildren().add(label);
            }
            bothContent.getChildren().add(row);
        }
    }
}
