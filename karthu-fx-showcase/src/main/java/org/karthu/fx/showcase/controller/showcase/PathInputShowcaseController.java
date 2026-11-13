package org.karthu.fx.showcase.controller.showcase;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.karthu.fx.form.BindingCandidate;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.FormInput;
import org.karthu.fx.form.PathInput;
import org.karthu.fx.form.PathFilter;
import org.karthu.fx.form.PathMode;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.focusWhenShown;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.offerVariables;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.rowSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.sectionLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.stateSwitches;

/**
 * Showcase page for the path input, in the group <i>Form inputs</i>: its states side by side, each in a form row with its
 * label, a path input beside a plain text field, and a field to try out in its row with a switch for each of its
 * states.
 *
 * <p>Each card is built by its own method: a later state is one more {@link #addState(String, Node)} call, a later
 * switch one more node in {@code tryOutSwitches}.
 */
public class PathInputShowcaseController {

    /** The width of every field on the page, so that the states line up. */
    private static final double FIELD_WIDTH = 360;

    /** The width of the try-out field's base directory control. */
    private static final double BASE_FIELD_WIDTH = 280;

    /** The variables the binding list offers on this page: a name and its type. */
    private static final List<BindingCandidate> SAMPLE_VARIABLES = List.of(
            new BindingCandidate("inputFile", "Path"),
            new BindingCandidate("dataFolder", "Path"),
            new BindingCandidate("fileName", "String"));

    @FXML private GridPane statesGrid;
    @FXML private GridPane comparisonGrid;
    @FXML private FlowPane tryOutSwitches;
    @FXML private VBox tryOutContainer;

    @FXML
    private void initialize() {
        buildStates();
        buildComparison();
        buildTryOut();
    }

    // --- States --------------------------------------------------------------------------------------

    /** Every state, in its order: plain, then label and messages, then disabled, read-only and binding. */
    private void buildStates() {
        addState("Default (empty)", pathInput());
        addState("Filled", filledPathInput());

        PathInput focused = filledPathInput();
        addState("Focused (on page open)", focused);
        focusWhenShown(focused);

        buildMessageStates();

        PathInput disabled = filledPathInput();
        disabled.setDisable(true);
        addState("Disabled", disabled);

        PathInput readOnly = filledPathInput();
        readOnly.setReadOnly(true);
        addState("Read-only", readOnly);

        buildBindingStates();

        PathInput directory = pathInput();
        directory.setMode(PathMode.DIRECTORY);
        addState("Directory mode", directory);
    }

    /** The row's states: required with an error, a warning, an info, and several messages in their order. */
    private void buildMessageStates() {
        FormField required = formRow(pathInput());
        required.setRequired(true);
        required.getMessages().add(FieldMessage.error("Choose the file to read."));
        addState("Required, with an error", required);

        FormField warning = formRow(filledPathInput());
        warning.getMessages().add(
                FieldMessage.warning("The path is absolute: the workflow will not run on another computer."));
        addState("Warning", warning);

        PathInput relative = pathInput();
        relative.setValue("files/customers-2026.csv");
        FormField info = formRow(relative);
        info.getMessages().add(FieldMessage.info("Read as UTF-8, the first line as the header."));
        addState("Info", info);

        FormField all = formRow(filledPathInput());
        all.setRequired(true);
        all.getMessages().addAll(
                FieldMessage.error("The file does not exist. Choose another one, or check that the drive is"
                        + " connected: a long message wraps at the field's width."),
                FieldMessage.warning("The path is absolute."),
                FieldMessage.info("messages are shown exactly as given, in their order."));
        addState("Messages in their order", all);
    }

    /**
     * The binding's states: bound, bound to a variable that does not exist any more, and bound while read-only (the
     * chip without its remove button). Each field is bindable, as an application's bindable setting is, so it keeps
     * its {@code {}} icon to bind again after the user removes the binding.
     */
    private void buildBindingStates() {
        PathInput bound = filledPathInput();
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        addState("Bound to a variable", bound);

        PathInput missing = filledPathInput();
        offerVariables(missing.getBinding(), SAMPLE_VARIABLES);
        missing.getBinding().setBindable(true);
        missing.getBinding().setBoundName("deletedVariable");
        missing.getBinding().setMissing(true);
        addState("Bound, variable missing", missing);

        PathInput readOnlyBound = filledPathInput();
        offerVariables(readOnlyBound.getBinding(), SAMPLE_VARIABLES);
        readOnlyBound.getBinding().setBindable(true);
        readOnlyBound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        readOnlyBound.setReadOnly(true);
        addState("Read-only, bound", readOnlyBound);
    }

    /**
     * Adds a row to the states card: its name, then the field in a form row with a label (a field given as a form
     * row already stays as it is). The name sits on the baseline of the row's label.
     */
    private void addState(String name, Node field) {
        FormInputShowcaseSupport.addState(statesGrid, name, field, PathInputShowcaseController::formRow);
    }

    /** A form row around the input, labelled like a reader's file setting (a writer's folder in directory mode). */
    private static FormField formRow(FormInput<?> input) {
        boolean folder = input instanceof PathInput pathInput && pathInput.getMode() == PathMode.DIRECTORY;
        FormField row = new FormField(folder ? "Output folder" : "Input file", input);
        row.setDescription(folder
                ? "The folder the node writes its files to."
                : "The CSV file the node reads, one record per line.");
        row.setMaxWidth(FIELD_WIDTH);
        return row;
    }

    // --- Beside a text field -------------------------------------------------------------------------

    /** A plain text field beside a path input: as they rest, read-only and disabled. */
    private void buildComparison() {
        addComparison("Normal", new TextField("A plain text field"), filledPathInput());

        TextField readOnlyText = new TextField("A read-only text field");
        readOnlyText.setEditable(false);
        PathInput readOnlyPath = filledPathInput();
        readOnlyPath.setReadOnly(true);
        addComparison("Read-only", readOnlyText, readOnlyPath);

        TextField disabledText = new TextField("A disabled text field");
        disabledText.setDisable(true);
        PathInput disabledPath = filledPathInput();
        disabledPath.setDisable(true);
        addComparison("Disabled", disabledText, disabledPath);
    }

    private void addComparison(String name, TextField textField, PathInput pathInput) {
        Label label = sectionLabel(name);
        textField.setPrefWidth(FIELD_WIDTH);
        comparisonGrid.addRow(comparisonGrid.getRowCount(), label, textField, pathInput);
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field in its row, with its switches in this order: required, error message, warning message,
     * disabled, read-only, bindable, bound, mode, base directory and the CSV filter.
     */
    private void buildTryOut() {
        PathInput field = pathInput();
        FormField tryOutRow = formRow(field);

        tryOutSwitches.getChildren().addAll(rowSwitches(tryOutRow,
                FieldMessage.error("The file does not exist."),
                FieldMessage.warning("The path is absolute: the workflow will not run on another computer.")));
        tryOutSwitches.getChildren().addAll(stateSwitches(field));
        tryOutSwitches.getChildren().addAll(
                bindingSwitches(field.getBinding(), SAMPLE_VARIABLES, SAMPLE_VARIABLES.getFirst()));
        tryOutSwitches.getChildren().add(modeSwitch(field));
        tryOutSwitches.getChildren().add(baseDirectorySwitch(field));
        tryOutSwitches.getChildren().add(csvFilterSwitch(field));

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("showcase-page-description");
        valueLabel.setWrapText(true);
        valueLabel.setText(describe(field));
        field.valueProperty().addListener((observable, oldValue, newValue) -> valueLabel.setText(describe(field)));

        Label bindingLabel = bindingLabel(field.getBinding());

        tryOutContainer.getChildren().addAll(tryOutRow, valueLabel, bindingLabel);
    }

    /** The try-out field's mode: open a file, save a file, choose a directory. */
    private static Node modeSwitch(PathInput field) {
        ChoiceBox<PathMode> mode = new ChoiceBox<>(FXCollections.observableArrayList(PathMode.values()));
        mode.valueProperty().bindBidirectional(field.modeProperty());
        HBox modeSwitch = new HBox(6, new Label("Mode"), mode);
        modeSwitch.setAlignment(Pos.CENTER_LEFT);
        return modeSwitch;
    }

    /**
     * The try-out field's base directory, chosen in a path input of its own in the directory mode. An empty or
     * relative entry means no base directory.
     */
    private static Node baseDirectorySwitch(PathInput field) {
        PathInput base = new PathInput();
        base.setMode(PathMode.DIRECTORY);
        base.setPromptText("No base directory");
        base.setTitle("Base directory");
        base.setPrefWidth(BASE_FIELD_WIDTH);
        base.valueProperty().addListener((observable, oldValue, newValue) -> {
            Optional<Path> path = base.toPath();
            field.setBaseDirectory(path.isPresent() && path.get().isAbsolute() ? path.get() : null);
        });
        HBox baseSwitch = new HBox(6, new Label("Base directory"), base);
        baseSwitch.setAlignment(Pos.CENTER_LEFT);
        return baseSwitch;
    }

    /**
     * A switch that gives the field a CSV filter: the open and save dialogs then list <i>CSV files</i> first and
     * selected, then <i>All files</i>. The directory mode ignores it.
     */
    private static CheckBox csvFilterSwitch(PathInput field) {
        PathFilter csv = new PathFilter("CSV files", List.of("*.csv"));
        CheckBox box = new CheckBox("CSV filter");
        box.selectedProperty().addListener((observable, wasSelected, selected) -> {
            if (selected) {
                field.getFilters().setAll(csv);
            } else {
                field.getFilters().clear();
            }
        });
        return box;
    }

    private static String describe(PathInput field) {
        Optional<Path> path = field.toPath();
        String pathText = path.isPresent() ? path.get().toString() : "(empty)";
        return "value: \"" + field.getValue() + "\"   toPath(): " + pathText;
    }

    // --- Helpers -------------------------------------------------------------------------------------

    private static PathInput pathInput() {
        PathInput field = new PathInput();
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }

    /** A path input with a long sample path, as the filled states show it. */
    private static PathInput filledPathInput() {
        PathInput field = pathInput();
        field.setValue(samplePath());
        return field;
    }

    /** A long absolute path on this machine, so the filled states look as they will in an application. */
    private static String samplePath() {
        return Path.of(System.getProperty("user.home"), "Documents", "workflows", "customers", "data",
                "customers-2026.csv").toString();
    }
}
