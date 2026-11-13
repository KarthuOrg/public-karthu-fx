package org.karthu.fx.showcase.controller.showcase;

import java.util.List;
import java.util.Locale;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller for table-view-showcase.fxml.
 * Populates a TableView with sample data and demonstrates the semantic
 * state cell classes (.state-normal, .state-running, ...) on the State column.
 * A check box switches the table to cell selection, to show the theme's
 * selected and focused cell.
 */
public class TableViewShowcaseController {

    private static final List<String> STATE_CLASSES = List.of(
            "state-normal", "state-running", "state-paused",
            "state-blocked", "state-error", "state-idle");

    @FXML
    private TableView<SampleData> table;

    @FXML
    private CheckBox cellSelectionCheck;

    @FXML
    private TableColumn<SampleData, String> nameCol;

    @FXML
    private TableColumn<SampleData, String> emailCol;

    @FXML
    private TableColumn<SampleData, String> statusCol;

    @FXML
    private void initialize() {
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        emailCol.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setCellFactory(column -> new StateCell());

        ObservableList<SampleData> data = FXCollections.observableArrayList();
        String[] names = {"Alice Johnson", "Bob Smith", "Charlie Brown", "Diana Prince", "Eve Davis"};
        String[] statuses = {"Normal", "Running", "Paused", "Blocked", "Error", "Idle"};

        for (int i = 0; i < 25; i++) {
            String name = names[i % names.length];
            String email = name.toLowerCase().replace(" ", ".") + (i + 1) + "@example.com";
            String status = statuses[i % statuses.length];
            data.add(new SampleData(name, email, status));
        }

        table.setItems(data);

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cellSelectionCheck.selectedProperty().addListener((obs, oldValue, cells) -> {
            table.getSelectionModel().clearSelection();
            table.getSelectionModel().setCellSelectionEnabled(cells);
        });
    }

    /**
     * Cell that colors its text via the semantic .state-* classes.
     * Cells are recycled, so stale state classes are removed on every update.
     */
    private static class StateCell extends TableCell<SampleData, String> {

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll(STATE_CLASSES);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item);
                getStyleClass().add("state-" + item.toLowerCase(Locale.ROOT));
            }
        }
    }

    /**
     * Sample data model for the TableView.
     */
    public static class SampleData {

        private final SimpleStringProperty name;
        private final SimpleStringProperty email;
        private final SimpleStringProperty status;

        public SampleData(String name, String email, String status) {
            this.name = new SimpleStringProperty(name);
            this.email = new SimpleStringProperty(email);
            this.status = new SimpleStringProperty(status);
        }

        public String getName() {
            return name.get();
        }

        public StringProperty nameProperty() {
            return name;
        }

        public String getEmail() {
            return email.get();
        }

        public StringProperty emailProperty() {
            return email;
        }

        public String getStatus() {
            return status.get();
        }

        public StringProperty statusProperty() {
            return status;
        }
    }
}
