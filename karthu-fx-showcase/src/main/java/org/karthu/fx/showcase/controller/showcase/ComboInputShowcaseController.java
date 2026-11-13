package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.karthu.fx.form.BindingCandidate;
import org.karthu.fx.form.ComboInput;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.FormInput;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.focusWhenShown;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.offerVariables;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.rowSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.sectionLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.stateSwitches;

/**
 * Showcase page for the combo input, in the group <i>Form inputs</i>: its states side by side, each in a form row with
 * its label, a combo input beside a plain text field, and a field to try out in its row with a switch for each of its
 * states.
 *
 * <p>Each card is built by its own method: a later state is one more {@link #addState(String, Node)} call, a later
 * switch one more node in {@code tryOutSwitches}.
 */
public class ComboInputShowcaseController {

    /** The width of every field on the page, so that the states line up. */
    private static final double FIELD_WIDTH = 360;

    /** The encodings of the states, each code with its label, in the list's order. */
    private static final Map<String, String> ENCODINGS = new LinkedHashMap<>();

    /** The delimiters of the try-out field, each character with its name, in the list's order. */
    private static final Map<String, String> DELIMITERS = new LinkedHashMap<>();

    static {
        ENCODINGS.put("UTF-8", "Unicode (UTF-8)");
        ENCODINGS.put("UTF-16", "Unicode (UTF-16)");
        ENCODINGS.put("ISO-8859-1", "Western European (ISO-8859-1)");
        ENCODINGS.put("windows-1252", "Western European (Windows-1252)");
        ENCODINGS.put("US-ASCII", "US-ASCII");

        DELIMITERS.put(",", "Comma");
        DELIMITERS.put(";", "Semicolon");
        DELIMITERS.put("\t", "Tab");
        DELIMITERS.put("|", "Pipe");
    }

    /** The variables the binding list offers on this page: a name and its type. */
    private static final List<BindingCandidate> SAMPLE_VARIABLES = List.of(
            new BindingCandidate("encoding", "String"),
            new BindingCandidate("delimiter", "String"),
            new BindingCandidate("charset", "String"));

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

    /**
     * Every state, in its order: plain, then the row's messages, disabled, read-only and binding, then the combo
     * input's own.
     */
    private void buildStates() {
        addState("Default (empty)", encodingInput());
        addState("Filled", filledEncodingInput());

        ComboInput<String> focused = filledEncodingInput();
        addState("Focused (on page open)", focused);
        focusWhenShown(focused);

        FormField error = formRow(encodingInput());
        error.setRequired(true);
        error.getMessages().add(FieldMessage.error("Choose the encoding of the file."));
        addState("Error", error);

        ComboInput<String> legacy = encodingInput();
        legacy.setValue("windows-1252");
        FormField warning = formRow(legacy);
        warning.getMessages().add(FieldMessage.warning("Windows-1252 is read as such on Windows only."));
        addState("Warning", warning);

        ComboInput<String> disabled = filledEncodingInput();
        disabled.setDisable(true);
        addState("Disabled", disabled);

        ComboInput<String> readOnly = filledEncodingInput();
        readOnly.setReadOnly(true);
        addState("Read-only", readOnly);

        buildBindingStates();
        buildOwnStates();
    }

    /**
     * The binding's states: bound, bound to a variable that does not exist any more, and bound while read-only (the
     * chip without its remove button). Each field is bindable, so it keeps its {@code {}} icon to bind again after the
     * user removes the binding.
     */
    private void buildBindingStates() {
        ComboInput<String> bound = filledEncodingInput();
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        addState("Bound to a variable", bound);

        ComboInput<String> missing = filledEncodingInput();
        offerVariables(missing.getBinding(), SAMPLE_VARIABLES);
        missing.getBinding().setBindable(true);
        missing.getBinding().setBoundName("deletedVariable");
        missing.getBinding().setMissing(true);
        addState("Bound, variable missing", missing);

        ComboInput<String> readOnlyBound = filledEncodingInput();
        offerVariables(readOnlyBound.getBinding(), SAMPLE_VARIABLES);
        readOnlyBound.getBinding().setBindable(true);
        readOnlyBound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        readOnlyBound.setReadOnly(true);
        addState("Read-only, bound", readOnlyBound);
    }

    /**
     * The combo input's own states: an optional choice with the empty entry and nothing chosen, codes shown without
     * labels, and a value that is not in the list.
     */
    private void buildOwnStates() {
        ComboInput<String> optional = encodingInput();
        optional.setEmptyAllowed(true);
        addState("Optional (empty entry)", optional);

        ComboInput<String> codes = filledEncodingInput();
        codes.setDisplayText(null);
        addState("Without labels", codes);

        ComboInput<String> withLabels = filledEncodingInput();
        withLabels.setValue("ISO-8859-1");
        addState("With labels", withLabels);

        ComboInput<String> notInList = encodingInput();
        notInList.setValue("UTF-32");
        notInList.setDisplayText(code -> ENCODINGS.getOrDefault(code, code + " (not offered)"));
        addState("Value not in the list", notInList);
    }

    /**
     * Adds a row to the states card: its name, then the field in a form row with a label (a field given as a form
     * row already stays as it is). The name sits on the baseline of the row's label.
     */
    private void addState(String name, Node field) {
        FormInputShowcaseSupport.addState(statesGrid, name, field, ComboInputShowcaseController::formRow);
    }

    /** A form row around the input, labelled like a reader's encoding setting. */
    private static FormField formRow(FormInput<?> input) {
        FormField row = new FormField("Encoding", input);
        row.setDescription("The character encoding the node reads the file with.");
        row.setMaxWidth(FIELD_WIDTH);
        return row;
    }

    // --- Beside a text field -------------------------------------------------------------------------

    /** A plain text field beside a combo input: as they rest, read-only and disabled. */
    private void buildComparison() {
        addComparison("Normal", new TextField("A plain text field"), filledEncodingInput());

        TextField readOnlyText = new TextField("A read-only text field");
        readOnlyText.setEditable(false);
        ComboInput<String> readOnlyCombo = filledEncodingInput();
        readOnlyCombo.setReadOnly(true);
        addComparison("Read-only", readOnlyText, readOnlyCombo);

        TextField disabledText = new TextField("A disabled text field");
        disabledText.setDisable(true);
        ComboInput<String> disabledCombo = filledEncodingInput();
        disabledCombo.setDisable(true);
        addComparison("Disabled", disabledText, disabledCombo);
    }

    private void addComparison(String name, TextField textField, ComboInput<String> comboInput) {
        Label label = sectionLabel(name);
        textField.setPrefWidth(FIELD_WIDTH);
        comboInput.setPrefWidth(FIELD_WIDTH);
        comboInput.setMaxWidth(FIELD_WIDTH);
        comparisonGrid.addRow(comparisonGrid.getRowCount(), label, textField, comboInput);
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field in its row, a delimiter setting, with its switches in this order: required, error message,
     * warning message, disabled, read-only, bindable, bound, labels and the empty entry.
     */
    private void buildTryOut() {
        ComboInput<String> field = new ComboInput<>();
        field.getItems().addAll(DELIMITERS.keySet());
        field.setValue(",");
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        FormField tryOutRow = new FormField("Delimiter", field);
        tryOutRow.setDescription("The character between two values of a line.");
        tryOutRow.setMaxWidth(FIELD_WIDTH);

        tryOutSwitches.getChildren().addAll(rowSwitches(tryOutRow,
                FieldMessage.error("Choose the delimiter of the file."),
                FieldMessage.warning("The file's first line has no such delimiter.")));
        tryOutSwitches.getChildren().addAll(stateSwitches(field));
        tryOutSwitches.getChildren().addAll(
                bindingSwitches(field.getBinding(), SAMPLE_VARIABLES, SAMPLE_VARIABLES.get(1)));
        addOwnSwitches(field);

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("showcase-page-description");
        valueLabel.setText(describe(field.getValue()));
        field.valueProperty().addListener((observable, oldValue, newValue) -> valueLabel.setText(describe(newValue)));

        Label bindingLabel = bindingLabel(field.getBinding());

        tryOutContainer.getChildren().addAll(tryOutRow, valueLabel, bindingLabel);
    }

    /** The try-out field's own switches: the delimiters by name, and the empty entry. */
    private void addOwnSwitches(ComboInput<String> field) {
        CheckBox labels = new CheckBox("Labels");
        labels.selectedProperty().addListener((observable, wasSelected, selected) ->
                field.setDisplayText(selected ? ComboInputShowcaseController::delimiterName : null));
        labels.setSelected(true);
        CheckBox empty = new CheckBox("Empty entry");
        empty.selectedProperty().bindBidirectional(field.emptyAllowedProperty());
        tryOutSwitches.getChildren().addAll(labels, empty);
    }

    /** A delimiter's name, as the list shows it while Labels is on. */
    private static String delimiterName(String delimiter) {
        return DELIMITERS.getOrDefault(delimiter, delimiter);
    }

    private static String describe(String value) {
        if (value == null) {
            return "value: null (none)";
        }
        return "value: \"" + value.replace("\t", "\\t") + "\"";
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** An encoding input without a value, its codes shown with their labels. */
    private static ComboInput<String> encodingInput() {
        ComboInput<String> field = new ComboInput<>();
        field.getItems().addAll(ENCODINGS.keySet());
        field.setDisplayText(code -> ENCODINGS.getOrDefault(code, code));
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }

    /** An encoding input with UTF-8 chosen. */
    private static ComboInput<String> filledEncodingInput() {
        ComboInput<String> field = encodingInput();
        field.setValue("UTF-8");
        return field;
    }
}
