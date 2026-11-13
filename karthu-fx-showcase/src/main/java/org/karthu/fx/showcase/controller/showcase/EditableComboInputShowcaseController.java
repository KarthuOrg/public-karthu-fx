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
import org.karthu.fx.form.EditableComboInput;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.FormInput;

import java.util.List;

import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.focusWhenShown;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.offerVariables;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.rowSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.sectionLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.stateSwitches;

/**
 * Showcase page for the editable combo input, in the group <i>Form inputs</i>: its states side by side, each in a form
 * row with its label, an editable combo input beside a plain text field and a combo input, and a field to try out in
 * its row with a switch for each of its states.
 *
 * <p>Each card is built by its own method: a later state is one more {@link #addState(String, Node)} call, a later
 * switch one more node in {@code tryOutSwitches}.
 */
public class EditableComboInputShowcaseController {

    /** The width of every field on the page, so that the states line up. */
    private static final double FIELD_WIDTH = 360;

    /** The width of each of the three fields side by side, so that they fit the card. */
    private static final double COMPARISON_WIDTH = 240;

    /** The suggested encodings, more than the list shows at once, so that it scrolls. */
    private static final List<String> ENCODINGS = List.of(
            "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE", "UTF-32", "ISO-8859-1", "ISO-8859-15", "windows-1250",
            "windows-1252", "US-ASCII", "Shift_JIS", "EUC-JP", "GB18030", "Big5");

    /** The prompt text of the encoding fields. */
    private static final String PROMPT = "Encoding, such as UTF-8";

    /** The variables the binding list offers on this page: a name and its type. */
    private static final List<BindingCandidate> SAMPLE_VARIABLES = List.of(
            new BindingCandidate("encoding", "String"),
            new BindingCandidate("charset", "String"),
            new BindingCandidate("defaultEncoding", "String"));

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
     * Every state, in its order: plain, then the row's messages, disabled, read-only and binding, then the editable
     * combo input's own.
     */
    private void buildStates() {
        addState("Default (empty)", encodingInput());
        addState("Filled", filledEncodingInput());

        EditableComboInput focused = filledEncodingInput();
        addState("Focused (on page open)", focused);
        focusWhenShown(focused);

        FormField error = formRow(encodingInput());
        error.setRequired(true);
        error.getMessages().add(FieldMessage.error("Enter the encoding of the file."));
        addState("Error", error);

        EditableComboInput legacy = encodingInput();
        legacy.setValue("windows-1252");
        FormField warning = formRow(legacy);
        warning.getMessages().add(FieldMessage.warning("Windows-1252 is read as such on Windows only."));
        addState("Warning", warning);

        EditableComboInput disabled = filledEncodingInput();
        disabled.setDisable(true);
        addState("Disabled", disabled);

        EditableComboInput readOnly = filledEncodingInput();
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
        EditableComboInput bound = filledEncodingInput();
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        addState("Bound to a variable", bound);

        EditableComboInput missing = filledEncodingInput();
        offerVariables(missing.getBinding(), SAMPLE_VARIABLES);
        missing.getBinding().setBindable(true);
        missing.getBinding().setBoundName("deletedVariable");
        missing.getBinding().setMissing(true);
        addState("Bound, variable missing", missing);

        EditableComboInput readOnlyBound = filledEncodingInput();
        offerVariables(readOnlyBound.getBinding(), SAMPLE_VARIABLES);
        readOnlyBound.getBinding().setBindable(true);
        readOnlyBound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        readOnlyBound.setReadOnly(true);
        addState("Read-only, bound", readOnlyBound);
    }

    /**
     * The editable combo input's own states: a text that is no suggestion, kept as typed, and a field without
     * suggestions, whose chevron opens an empty list.
     */
    private void buildOwnStates() {
        EditableComboInput notSuggested = encodingInput();
        notSuggested.setValue("latin1");
        addState("Not a suggestion", notSuggested);

        EditableComboInput withoutSuggestions = encodingInput();
        withoutSuggestions.getSuggestions().clear();
        withoutSuggestions.setValue("UTF-8");
        addState("Without suggestions", withoutSuggestions);
    }

    /**
     * Adds a row to the states card: its name, then the field in a form row with a label (a field given as a form
     * row already stays as it is). The name sits on the baseline of the row's label.
     */
    private void addState(String name, Node field) {
        FormInputShowcaseSupport.addState(statesGrid, name, field, EditableComboInputShowcaseController::formRow);
    }

    /** A form row around the input, labelled like a reader's encoding setting. */
    private static FormField formRow(FormInput<?> input) {
        FormField row = new FormField("Encoding", input);
        row.setDescription("The character encoding the node reads the file with: one of the list, or any other.");
        row.setMaxWidth(FIELD_WIDTH);
        return row;
    }

    // --- Beside a text field and a combo input -------------------------------------------------------

    /** A plain text field and a combo input beside an editable combo input: as they rest, read-only and disabled. */
    private void buildComparison() {
        addComparison("Normal", new TextField("UTF-8"), filledEncodingInput(), comboInput());

        TextField readOnlyText = new TextField("UTF-8");
        readOnlyText.setEditable(false);
        EditableComboInput readOnlyEditable = filledEncodingInput();
        readOnlyEditable.setReadOnly(true);
        ComboInput<String> readOnlyCombo = comboInput();
        readOnlyCombo.setReadOnly(true);
        addComparison("Read-only", readOnlyText, readOnlyEditable, readOnlyCombo);

        TextField disabledText = new TextField("UTF-8");
        disabledText.setDisable(true);
        EditableComboInput disabledEditable = filledEncodingInput();
        disabledEditable.setDisable(true);
        ComboInput<String> disabledCombo = comboInput();
        disabledCombo.setDisable(true);
        addComparison("Disabled", disabledText, disabledEditable, disabledCombo);
    }

    private void addComparison(String name, TextField textField, EditableComboInput editable,
            ComboInput<String> combo) {
        Label label = sectionLabel(name);
        textField.setPrefWidth(COMPARISON_WIDTH);
        editable.setPrefWidth(COMPARISON_WIDTH);
        editable.setMaxWidth(COMPARISON_WIDTH);
        combo.setPrefWidth(COMPARISON_WIDTH);
        combo.setMaxWidth(COMPARISON_WIDTH);
        comparisonGrid.addRow(comparisonGrid.getRowCount(), label, textField, editable, combo);
    }

    /** A combo input of the same encodings with UTF-8 chosen. */
    private static ComboInput<String> comboInput() {
        ComboInput<String> field = new ComboInput<>();
        field.getItems().addAll(ENCODINGS);
        field.setValue("UTF-8");
        return field;
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field in its row, an encoding setting, with its switches in this order: required, error message,
     * warning message, disabled, read-only, bindable, bound and the suggestions.
     */
    private void buildTryOut() {
        EditableComboInput field = encodingInput();
        FormField tryOutRow = formRow(field);

        tryOutSwitches.getChildren().addAll(rowSwitches(tryOutRow,
                FieldMessage.error("Enter the encoding of the file."),
                FieldMessage.warning("This encoding is not one of the suggestions.")));
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

    /** The try-out field's own switch: the suggestions on and off. */
    private void addOwnSwitches(EditableComboInput field) {
        CheckBox suggestions = new CheckBox("Suggestions");
        suggestions.setSelected(true);
        suggestions.selectedProperty().addListener((observable, wasSelected, selected) -> {
            if (selected) {
                field.getSuggestions().setAll(ENCODINGS);
            } else {
                field.getSuggestions().clear();
            }
        });
        tryOutSwitches.getChildren().add(suggestions);
    }

    private static String describe(String value) {
        if (value == null) {
            return "value: null";
        }
        return "value: \"" + value + "\"";
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** An encoding input without a text, the encodings as its suggestions, with the prompt. */
    private static EditableComboInput encodingInput() {
        EditableComboInput field = new EditableComboInput();
        field.getSuggestions().addAll(ENCODINGS);
        field.setPromptText(PROMPT);
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }

    /** An encoding input with UTF-8 as its text. */
    private static EditableComboInput filledEncodingInput() {
        EditableComboInput field = encodingInput();
        field.setValue("UTF-8");
        return field;
    }
}
