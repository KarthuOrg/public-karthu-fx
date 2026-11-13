package org.karthu.fx.showcase.controller.showcase;

import java.util.List;
import java.util.Locale;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller for job-scheduler-showcase.fxml.
 * Populates the job table with static sample data — the page is a pure
 * look-and-feel mock of a scheduler dashboard, without any functionality.
 */
public class JobSchedulerShowcaseController {

    private static final List<String> STATE_CLASSES = List.of(
            "state-normal", "state-running", "state-paused",
            "state-blocked", "state-error", "state-idle");

    private static final String MONO_STYLE = "-fx-font-family: 'Monospaced';";
    private static final String MONO_SECONDARY_STYLE =
            "-fx-font-family: 'Monospaced'; -fx-text-fill: -fx-text-secondary-color;";

    @FXML
    private TableView<JobRow> jobTable;

    @FXML
    private TableColumn<JobRow, String> nrCol;

    @FXML
    private TableColumn<JobRow, String> jobIdCol;

    @FXML
    private TableColumn<JobRow, String> descriptionCol;

    @FXML
    private TableColumn<JobRow, String> stateCol;

    @FXML
    private TableColumn<JobRow, String> runningCol;

    @FXML
    private TableColumn<JobRow, String> intervalCol;

    @FXML
    private TableColumn<JobRow, String> doNothingCol;

    @FXML
    private TableColumn<JobRow, String> durationCol;

    @FXML
    private TableColumn<JobRow, String> nextFireCol;

    @FXML
    private void initialize() {
        nrCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().nr()));
        jobIdCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().jobId()));
        descriptionCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().description()));
        stateCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().state()));
        runningCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().running()));
        intervalCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().interval()));
        doNothingCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().doNothing()));
        durationCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().duration()));
        nextFireCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().nextFire()));

        stateCol.setCellFactory(col -> new StateCell());
        runningCol.setCellFactory(col -> new YesNoCell("state-normal"));
        doNothingCol.setCellFactory(col -> new YesNoCell("state-paused"));
        jobIdCol.setCellFactory(col -> new StyledCell(MONO_STYLE));
        intervalCol.setCellFactory(col -> new StyledCell(MONO_SECONDARY_STYLE));
        nextFireCol.setCellFactory(col -> new StyledCell(MONO_SECONDARY_STYLE));

        jobTable.setItems(FXCollections.observableArrayList(
                new JobRow("999", "connectivity-test", "Tests the connectivity to external systems",
                        "PAUSED", "No", "0 0/15 * * ?", "No", "--", "2026-07-16\n13:30:00"),
                new JobRow("201", "nightly-sync", "Synchronizes every system end to end",
                        "BLOCKED", "Yes", "0 0/5 * * ?", "No", "1m 37s", "2026-07-17\n08:40:00"),
                new JobRow("90", "document-sync", "Synchronizes documents with the archive",
                        "NORMAL", "No", "0 0/1 * * ?", "No", "2s", "2026-07-17\n08:37:00"),
                new JobRow("91", "masterdata-sync", "Synchronizes master data from an external source",
                        "NORMAL", "No", "0 0/15 * * ?", "No", "79ms", "2026-07-17\n08:45:00"),
                new JobRow("103", "cache-persist", "Persists the in-memory cache to disk",
                        "NORMAL", "No", "0 1/15 * * ?", "No", "10s", "2026-07-17\n08:46:00"),
                new JobRow("101", "cache-update", "Updates the caches from the remote API",
                        "NORMAL", "No", "0 1/1 * * ?", "No", "1s", "2026-07-17\n08:37:00"),
                new JobRow("100", "cache-recreate", "Recreates the caches in memory",
                        "PAUSED", "No", "0 1/15 * * ?", "Yes", "--", "2026-07-16\n13:31:00"),
                new JobRow("202", "flag-update-retry", "Retries a flag update that failed several times",
                        "NORMAL", "No", "0 0/5 * * ?", "No", "3ms", "2026-07-17\n08:40:00"),
                new JobRow("92", "masterdata-csv-export", "Exports master data to CSV files",
                        "NORMAL", "No", "0 30 2 * ?", "No", "4s", "2026-07-18\n02:30:00")
        ));

        jobTable.getSelectionModel().selectFirst();
    }

    /**
     * Cell that colors its text via the semantic .state-* classes.
     * Cells are recycled, so stale state classes are removed on every update.
     */
    private static class StateCell extends TableCell<JobRow, String> {

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
     * Cell for Yes/No values: "Yes" is colored with the given state class,
     * "No" is dimmed via .state-idle.
     */
    private static class YesNoCell extends TableCell<JobRow, String> {

        private final String yesStateClass;

        YesNoCell(String yesStateClass) {
            this.yesStateClass = yesStateClass;
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll(STATE_CLASSES);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item);
                getStyleClass().add("Yes".equals(item) ? yesStateClass : "state-idle");
            }
        }
    }

    /**
     * Cell that renders its text with a fixed inline style (e.g. monospaced).
     */
    private static class StyledCell extends TableCell<JobRow, String> {

        StyledCell(String style) {
            setStyle(style);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty ? null : item);
        }
    }

    /**
     * Static sample row for the job table.
     */
    public record JobRow(
            String nr,
            String jobId,
            String description,
            String state,
            String running,
            String interval,
            String doNothing,
            String duration,
            String nextFire
    ) {
    }
}
