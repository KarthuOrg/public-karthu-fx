package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

/**
 * Controller for tab-pane-showcase.fxml.
 * Populates closable and overflow tab examples programmatically.
 */
public class TabPaneShowcaseController {

    @FXML
    private TabPane closableTabPane;

    @FXML
    private TabPane manyTabsPane;

    @FXML
    private void initialize() {
        // Closable tabs
        for (int i = 1; i <= 4; i++) {
            Tab tab = new Tab("Document " + i);
            VBox content = new VBox(8);
            content.setPadding(new Insets(16));
            content.setMinHeight(100);
            content.getChildren().add(new Label("Content of document " + i + "."));
            tab.setContent(content);
            closableTabPane.getTabs().add(tab);
        }

        // Many tabs to demonstrate overflow / scroll behavior
        for (int i = 1; i <= 12; i++) {
            Tab tab = new Tab("Tab " + i);
            VBox content = new VBox(8);
            content.setPadding(new Insets(16));
            content.setMinHeight(100);
            content.getChildren().add(new Label("Content of tab " + i + "."));
            tab.setContent(content);
            manyTabsPane.getTabs().add(tab);
        }
    }
}
