package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.karthu.fx.components.TwinColumnSelector;

import java.util.List;

/**
 * Showcase page for the TwinColumnSelector dual-list picker.
 */
public class TwinColumnSelectorShowcaseController {

    @FXML private VBox basicContainer;
    @FXML private VBox reorderableContainer;

    @FXML
    private void initialize() {
        TwinColumnSelector<String> basic = new TwinColumnSelector<>();
        basic.setItems(
                List.of("disk_label", "path", "kind", "size", "created", "modified",
                        "sha256", "file_type", "error_code", "error_detail"),
                List.of("disk_label", "path", "kind", "size"));
        basic.setPrefHeight(260);
        basicContainer.getChildren().add(basic);

        TwinColumnSelector<String> reorderable = new TwinColumnSelector<>();
        reorderable.setAvailableHeader("Pool");
        reorderable.setSelectedHeader("Chosen");
        reorderable.setReorderable(true);
        reorderable.setItems(
                List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot"),
                List.of("Charlie", "Alpha"));
        reorderable.setPrefHeight(260);
        reorderableContainer.getChildren().add(reorderable);
    }
}
