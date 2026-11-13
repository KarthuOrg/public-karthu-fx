package org.karthu.fx.showcase.controller.showcase;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.karthu.fx.form.BindingCandidate;
import org.karthu.fx.form.ComboInput;
import org.karthu.fx.form.CronFormat;
import org.karthu.fx.form.CronInput;
import org.karthu.fx.form.EditableComboInput;
import org.karthu.fx.form.FieldBinding;
import org.karthu.fx.form.FieldShell;
import org.karthu.fx.form.FieldState;
import org.karthu.fx.form.FormInput;
import org.karthu.fx.form.NumberInput;
import org.karthu.fx.form.PasswordInput;
import org.karthu.fx.form.PathInput;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.form.TextAreaInput;
import org.karthu.fx.form.TextInput;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Showcase page: the overview of the group <i>Form inputs</i>, its first page. Every input type is a row of one grid
 * and every state a column (default, focused, disabled, error, bound, read-only), so that the types can be compared
 * side by side.
 *
 * <p>A further type is one more {@link #addRow(String, String, Supplier)} call in {@link #buildRows()}, with a factory
 * of a filled field of that type; the columns put each form input into their state through {@link FormInput} alone. A
 * type that is no form input but sits in the shell, the search input, has no sample variable: its columns use the
 * shell alone, and its <i>Bound</i> cell stays empty, as it offers no binding.
 */
public class FormInputsOverviewShowcaseController {

    /** The width of every field in the grid, so that the columns line up. */
    private static final double FIELD_WIDTH = 190;

    /** JavaFX's own pseudo-class of a node that holds the focus, which the theme draws as the focus border. */
    private static final PseudoClass FOCUS_WITHIN = PseudoClass.getPseudoClass("focus-within");

    /** The states, the grid's columns, in their order. */
    private enum Column {
        DEFAULT("Default"),
        FOCUSED("Focused"),
        DISABLED("Disabled"),
        ERROR("Error"),
        BOUND("Bound"),
        READ_ONLY("Read-only");

        private final String title;

        Column(String title) {
            this.title = title;
        }
    }

    @FXML private GridPane overviewGrid;

    @FXML
    private void initialize() {
        buildHeader();
        buildRows();
    }

    // --- The grid ------------------------------------------------------------------------------------

    /** The grid's header: an empty corner, then each state's title. */
    private void buildHeader() {
        for (Column column : Column.values()) {
            Label title = new Label(column.title);
            title.getStyleClass().add("section-label");
            overviewGrid.add(title, column.ordinal() + 1, 0);
        }
    }

    /** One row per input type, in the order of the sidebar. */
    private void buildRows() {
        addRow("Text Input", "customerName", FormInputsOverviewShowcaseController::textInput);
        addRow("Password Input", "dbPassword", FormInputsOverviewShowcaseController::passwordInput);
        addRow("Number Input", "maxRows", FormInputsOverviewShowcaseController::numberInput);
        addRow("Spinner", "retries", FormInputsOverviewShowcaseController::spinner);
        addRow("Combo Input", "encoding", FormInputsOverviewShowcaseController::comboInput);
        addRow("Editable Combo Input", "charset", FormInputsOverviewShowcaseController::editableComboInput);
        addRow("Text Area Input", "description", FormInputsOverviewShowcaseController::textAreaInput);
        addRow("Path Input", "inputFile", FormInputsOverviewShowcaseController::pathInput);
        addRow("Cron Input", "schedule", FormInputsOverviewShowcaseController::cronInput);
        // No form input: no sample variable, its columns through the shell alone.
        addRow("Search Input", null, FormInputsOverviewShowcaseController::searchInput);
    }

    /**
     * Adds a type's row: its name, then one field per state, each made by the factory and put into its column's
     * state. A form input is put into it through the form input contract alone, every field bindable to the sample
     * variable, the bound column bound to it. A type that is no form input but sits in the shell has no variable: it is
     * put into its state through the shell alone, and its <i>Bound</i> cell stays empty, as it offers no binding.
     *
     * @param typeName    the type's name, as in the sidebar
     * @param variable    the sample variable a form input's fields may be bound to; null for a type that is no form
     *                    input
     * @param filledField makes a field of the type with a sample value
     */
    private void addRow(String typeName, String variable, Supplier<? extends FieldShell> filledField) {
        int row = overviewGrid.getRowCount();
        Label name = new Label(typeName);
        name.getStyleClass().add("section-label");
        // The type's name is never shortened: the fields give way, as a longer name than "Path Input" needs.
        name.setMinWidth(Region.USE_PREF_SIZE);
        overviewGrid.add(name, 0, row);
        for (Column column : Column.values()) {
            if (variable == null && column == Column.BOUND) {
                // No binding offered: the cell stays empty.
                continue;
            }
            FieldShell field = filledField.get();
            field.setPrefWidth(FIELD_WIDTH);
            field.setMaxWidth(FIELD_WIDTH);
            if (field instanceof FormInput<?> input) {
                showState(input, column, variable);
            } else {
                showState(field, column);
            }
            overviewGrid.add(field, column.ordinal() + 1, row);
        }
    }

    /** Puts a field that is no form input into its column's state, through the shell alone; Bound never comes here. */
    private static void showState(FieldShell field, Column column) {
        switch (column) {
            case DEFAULT -> {
                // at rest
            }
            case FOCUSED -> showFocused(field);
            case DISABLED -> field.setDisable(true);
            case ERROR -> field.setState(FieldState.ERROR);
            case READ_ONLY -> field.setReadOnly(true);
        }
    }

    /**
     * Puts a form input into its column's state, through the form input contract alone, bindable to the sample
     * variable.
     */
    private static void showState(FormInput<?> input, Column column, String variable) {
        FieldBinding binding = input.getBinding();
        binding.getCandidates().setAll(new BindingCandidate(variable));
        binding.setBindable(true);
        switch (column) {
            case DEFAULT -> {
                // at rest
            }
            case FOCUSED -> showFocused(input.getNode());
            case DISABLED -> input.getNode().setDisable(true);
            case ERROR -> input.stateProperty().set(FieldState.ERROR);
            case BOUND -> input.getBinding().setBoundName(variable);
            case READ_ONLY -> input.readOnlyProperty().set(true);
        }
    }

    /**
     * A stand-in for the focus: the field shows the focus border, JavaFX's pseudo-class {@code :focus-within}, without
     * the focus, since only one field of a page can have it. When the user focuses the field and leaves it, JavaFX
     * ends the pseudo-class, and the field shows it again.
     */
    private static void showFocused(Node field) {
        field.pseudoClassStateChanged(FOCUS_WITHIN, true);
        field.focusWithinProperty().addListener((observable, wasWithin, within) -> {
            if (!within) {
                field.pseudoClassStateChanged(FOCUS_WITHIN, true);
            }
        });
    }

    // --- The types' sample fields --------------------------------------------------------------------

    /** A text input with a short sample name, which fits the column. */
    private static TextInput textInput() {
        TextInput field = new TextInput();
        field.setValue("Ada Lovelace");
        return field;
    }

    /** A password input with a sample secret, its eye allowed, so its own control shows in every state. */
    private static PasswordInput passwordInput() {
        PasswordInput field = new PasswordInput();
        field.setValue("s3cret-pass");
        field.setRevealAllowed(true);
        return field;
    }

    /** A number input with a sample number, right-aligned in the column. */
    private static NumberInput numberInput() {
        NumberInput field = new NumberInput();
        field.setValue("1250");
        return field;
    }

    /** A spinner, the number input with its arrows, with a small sample number. */
    private static NumberInput spinner() {
        NumberInput field = new NumberInput();
        field.setArrowsVisible(true);
        field.setMin(BigDecimal.ZERO);
        field.setValue("3");
        return field;
    }

    /** A combo input of encodings with UTF-8 chosen, a short value that fits the column. */
    private static ComboInput<String> comboInput() {
        ComboInput<String> field = new ComboInput<>();
        field.getItems().addAll("UTF-8", "UTF-16", "ISO-8859-1", "windows-1252");
        field.setValue("UTF-8");
        return field;
    }

    /** An editable combo input with encodings as suggestions and UTF-8 typed, a short text that fits the column. */
    private static EditableComboInput editableComboInput() {
        EditableComboInput field = new EditableComboInput();
        field.getSuggestions().addAll("UTF-8", "UTF-16", "ISO-8859-1", "windows-1252");
        field.setValue("UTF-8");
        return field;
    }

    /** A text area input with two short lines, at its default four rows. */
    private static TextAreaInput textAreaInput() {
        TextAreaInput field = new TextAreaInput();
        field.setValue("Reads the customers\nand keeps the active ones.");
        return field;
    }

    /** A path input with a short relative sample path, which fits the column. */
    private static PathInput pathInput() {
        PathInput field = new PathInput();
        field.setValue("data/customers.csv");
        return field;
    }

    /** A cron input with a short Unix expression, which fits the column beside the format selector. */
    private static CronInput cronInput() {
        CronInput field = new CronInput();
        field.setFormat(CronFormat.UNIX);
        field.setValue("0 9 * * 1-5");
        return field;
    }

    /** A search input with a search typed, so that its clear button shows where the user may clear it. */
    private static SearchInput searchInput() {
        SearchInput field = new SearchInput();
        field.setText("file");
        return field;
    }
}
