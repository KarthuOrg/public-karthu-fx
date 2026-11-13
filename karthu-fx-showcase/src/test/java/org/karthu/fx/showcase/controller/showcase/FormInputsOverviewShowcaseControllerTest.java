package org.karthu.fx.showcase.controller.showcase;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.FieldShell;
import org.karthu.fx.form.FieldState;
import org.karthu.fx.form.FormInput;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.form.TextAreaInput;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The overview page of the group <i>Form Inputs</i>, loaded from its FXML and laid out with the theme in the
 * showcase's scroll pane, in a scene that is never shown: one row per input type in the sidebar's order, one field per
 * state, the type names never shortened however narrow the window, and the fields of a row as high as each other. The
 * last row, the search input, is no form input: built through the shell, its <i>Bound</i> cell empty.
 */
class FormInputsOverviewShowcaseControllerTest {

    /** The rows' type names, in the sidebar's order (the spinner is the number input with its arrows). */
    private static final List<String> TYPE_NAMES = List.of("Text Input", "Password Input", "Number Input", "Spinner",
            "Combo Input", "Editable Combo Input", "Text Area Input", "Path Input", "Cron Input", "Search Input");

    /** The row of the search input, the last: no form input, so its Bound cell stays empty. */
    private static final int SEARCH_ROW = TYPE_NAMES.size();

    /** The states, the grid's columns. */
    private static final int STATES = 6;

    /** The grid's column of the Bound state: the fifth state, after the column of the names. */
    private static final int BOUND_COLUMN = 5;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        // The first control has the toolkit set its default stylesheet on its own thread: wait for it.
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @Test
    @DisplayName("one row per type in the sidebar's order, one field per state, each name shown whole at any width")
    void everyTypeByNameNeverShortened() throws IOException {
        // Narrower than the grid, and the widths at which the names were shortened before (1100px, 1200px).
        for (double width : new double[] {600, 1100, 1200, 1600}) {
            GridPane grid = layOutOverview(width);

            List<Label> names = new ArrayList<>();
            for (Node child : grid.getChildren()) {
                if (columnOf(child) == 0 && rowOf(child) > 0) {
                    names.add(assertInstanceOf(Label.class, child));
                }
            }
            List<String> texts = new ArrayList<>();
            for (Label name : names) {
                texts.add(name.getText());
                Text shown = (Text) name.lookup(".text");
                assertNotNull(shown, name.getText());
                assertEquals(name.getText(), shown.getText(), "at " + width + "px");
            }
            assertEquals(TYPE_NAMES, texts);
            for (int row = 1; row < SEARCH_ROW; row++) {
                assertEquals(STATES, fieldsOfRow(grid, row).size(), TYPE_NAMES.get(row - 1));
                for (FieldShell field : fieldsOfRow(grid, row)) {
                    assertInstanceOf(FormInput.class, field, TYPE_NAMES.get(row - 1));
                }
            }
            assertEquals(STATES - 1, fieldsOfRow(grid, SEARCH_ROW).size(), "Search Input");
        }
    }

    @Test
    @DisplayName("the search input's row is built through the shell: every state but Bound, whose cell stays empty")
    void searchInputRowThroughTheShell() throws IOException {
        GridPane grid = layOutOverview(1600);

        List<FieldShell> fields = fieldsOfRow(grid, SEARCH_ROW);
        for (Node child : grid.getChildren()) {
            assertFalse(rowOf(child) == SEARCH_ROW && columnOf(child) == BOUND_COLUMN, "the Bound cell is not empty");
        }
        for (FieldShell field : fields) {
            SearchInput search = assertInstanceOf(SearchInput.class, field);
            assertFalse(search instanceof FormInput<?>);
            assertFalse(search.getBinding().isBindable(), "a search input offers no binding");
            assertEquals("file", search.getText());
        }
        // Default, Focused, Disabled, Error and Read-only: the shell's states.
        assertFalse(fields.get(0).isDisabled() || fields.get(0).isReadOnly());
        assertTrue(fields.get(1).getPseudoClassStates().toString().contains("focus-within"));
        assertTrue(fields.get(2).isDisabled());
        assertEquals(FieldState.ERROR, fields.get(3).getState());
        assertTrue(fields.get(4).isReadOnly());
    }

    @Test
    @DisplayName("each field is as high as the others in its row; each row but the text area's as high as a text field")
    void fieldsOfARowAreEquallyHigh() throws IOException {
        GridPane grid = layOutOverview(1600);
        TextField plain = new TextField("plain");
        VBox root = new VBox(plain);
        ThemeManager.apply(new Scene(root));
        root.applyCss();
        root.autosize();
        root.layout();

        for (int row = 1; row <= TYPE_NAMES.size(); row++) {
            List<FieldShell> fields = fieldsOfRow(grid, row);
            double height = fields.get(0).getLayoutBounds().getHeight();
            for (int column = 0; column < fields.size(); column++) {
                assertEquals(height, fields.get(column).getLayoutBounds().getHeight(), 0.01,
                        TYPE_NAMES.get(row - 1) + ", state " + (column + 1));
            }
            if (!(fields.get(0) instanceof TextAreaInput)) {
                assertEquals(plain.getHeight(), height, 0.01, TYPE_NAMES.get(row - 1));
            }
        }
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /**
     * Loads the overview page and lays it out in the theme at a window's width, the way the showcase shows it: in a
     * scroll pane that fits its content to its width, never narrower than the content's minimum.
     *
     * @return the page's grid of every type by state
     */
    private static GridPane layOutOverview(double width) throws IOException {
        URL page = FormInputsOverviewShowcaseController.class.getResource(
                "/org/karthu/fx/showcase/showcase/form-inputs-overview-showcase.fxml");
        assertNotNull(page);
        Parent overview = FXMLLoader.load(page);
        StackPane content = new StackPane(overview);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        Scene scene = new Scene(scrollPane, width, 900);
        ThemeManager.apply(scene);
        scrollPane.resize(width, 900);
        // Two passes, as two pulses of a shown window: the second lays out what the first one's sizes changed.
        for (int pass = 0; pass < 2; pass++) {
            scrollPane.applyCss();
            scrollPane.layout();
        }
        return (GridPane) overview.lookup("#overviewGrid");
    }

    /** The fields of a row, left to right: every child of the row but its name, each a field in the shell. */
    private static List<FieldShell> fieldsOfRow(GridPane grid, int row) {
        List<FieldShell> fields = new ArrayList<>();
        for (Node child : grid.getChildren()) {
            if (rowOf(child) == row && columnOf(child) > 0) {
                fields.add(assertInstanceOf(FieldShell.class, child));
            }
        }
        return fields;
    }

    private static int rowOf(Node child) {
        Integer row = GridPane.getRowIndex(child);
        return row == null ? 0 : row;
    }

    private static int columnOf(Node child) {
        Integer column = GridPane.getColumnIndex(child);
        return column == null ? 0 : column;
    }
}
