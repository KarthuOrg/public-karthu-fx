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
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.FormInput;
import org.karthu.fx.form.TextInput;

import java.util.List;

import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.focusWhenShown;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.offerVariables;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.rowSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.sectionLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.stateSwitches;

/**
 * Showcase page for the text input, in the group <i>Form inputs</i>: its states side by side, each in a form row with
 * its label, a text input beside a plain text field, and a field to try out in its row with a switch for each of its
 * states.
 *
 * <p>Each card is built by its own method: a later state is one more {@link #addState(String, Node)} call, a later
 * switch one more node in {@code tryOutSwitches}.
 */
public class TextInputShowcaseController {

    /** The width of every field on the page, so that the states line up. */
    private static final double FIELD_WIDTH = 360;

    /** The sample value of the filled states. */
    private static final String SAMPLE_NAME = "Ada Lovelace";

    /** The prompt text of the empty states. */
    private static final String PROMPT_TEXT = "First and last name";

    /** The variables the binding list offers on this page: a name and its type. */
    private static final List<BindingCandidate> SAMPLE_VARIABLES = List.of(
            new BindingCandidate("customerName", "String"),
            new BindingCandidate("companyName", "String"),
            new BindingCandidate("country", "String"));

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
     * Every state, in its order: plain (empty, filled, bindable with the {@code {}} icon, focused), then label and
     * messages, then disabled, read-only and binding.
     */
    private void buildStates() {
        addState("Default (empty)", textInput());
        addState("Filled", filledTextInput());

        TextInput bindable = filledTextInput();
        offerVariables(bindable.getBinding(), SAMPLE_VARIABLES);
        bindable.getBinding().setBindable(true);
        addState("Bindable (the {} icon)", bindable);

        TextInput focused = filledTextInput();
        addState("Focused (on page open)", focused);
        focusWhenShown(focused);

        buildMessageStates();

        TextInput disabled = filledTextInput();
        disabled.setDisable(true);
        addState("Disabled", disabled);

        TextInput readOnly = filledTextInput();
        readOnly.setReadOnly(true);
        addState("Read-only", readOnly);

        buildBindingStates();
    }

    /** The row's states: required with an error, a warning, and an info that leaves the border as it is. */
    private void buildMessageStates() {
        FormField required = formRow(textInput());
        required.setRequired(true);
        required.getMessages().add(FieldMessage.error("Enter the customer's name."));
        addState("Required, with an error", required);

        TextInput shortName = textInput();
        shortName.setValue("Ada");
        FormField warning = formRow(shortName);
        warning.getMessages().add(FieldMessage.warning("Only a first name: the letter will not be addressed in full."));
        addState("Warning", warning);

        FormField info = formRow(filledTextInput());
        info.getMessages().add(FieldMessage.info("Printed on the invoice as written."));
        addState("Info", info);
    }

    /**
     * The binding's states: bound, bound to a variable that does not exist any more, and bound while read-only (the
     * chip without its remove button). Each field is bindable, as an application's bindable setting is, so it keeps
     * its {@code {}} icon to bind again after the user removes the binding.
     */
    private void buildBindingStates() {
        TextInput bound = filledTextInput();
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        addState("Bound to a variable", bound);

        TextInput missing = filledTextInput();
        offerVariables(missing.getBinding(), SAMPLE_VARIABLES);
        missing.getBinding().setBindable(true);
        missing.getBinding().setBoundName("deletedVariable");
        missing.getBinding().setMissing(true);
        addState("Bound, variable missing", missing);

        TextInput readOnlyBound = filledTextInput();
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
        FormInputShowcaseSupport.addState(statesGrid, name, field, TextInputShowcaseController::formRow);
    }

    /** A form row around the input, labelled like a customer's name in an address form. */
    private static FormField formRow(FormInput<?> input) {
        FormField row = new FormField("Customer name", input);
        row.setDescription("The name the invoices are addressed to.");
        row.setMaxWidth(FIELD_WIDTH);
        return row;
    }

    // --- Beside a text field -------------------------------------------------------------------------

    /** A plain text field beside a text input: as they rest, empty with a prompt text, read-only and disabled. */
    private void buildComparison() {
        addComparison("Normal", new TextField("A plain text field"), filledTextInput());

        TextField promptText = new TextField();
        promptText.setPromptText(PROMPT_TEXT);
        addComparison("Prompt text", promptText, textInput());

        TextField readOnlyText = new TextField("A read-only text field");
        readOnlyText.setEditable(false);
        TextInput readOnlyInput = filledTextInput();
        readOnlyInput.setReadOnly(true);
        addComparison("Read-only", readOnlyText, readOnlyInput);

        TextField disabledText = new TextField("A disabled text field");
        disabledText.setDisable(true);
        TextInput disabledInput = filledTextInput();
        disabledInput.setDisable(true);
        addComparison("Disabled", disabledText, disabledInput);
    }

    private void addComparison(String name, TextField textField, TextInput textInput) {
        Label label = sectionLabel(name);
        textField.setPrefWidth(FIELD_WIDTH);
        comparisonGrid.addRow(comparisonGrid.getRowCount(), label, textField, textInput);
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field in its row, with its switches in this order: required, error message, warning message,
     * disabled, read-only, bindable, bound and prompt text.
     */
    private void buildTryOut() {
        TextInput field = textInput();
        FormField tryOutRow = formRow(field);

        tryOutSwitches.getChildren().addAll(rowSwitches(tryOutRow,
                FieldMessage.error("Enter the customer's name."),
                FieldMessage.warning("Only a first name: the letter will not be addressed in full.")));
        tryOutSwitches.getChildren().addAll(stateSwitches(field));
        tryOutSwitches.getChildren().addAll(
                bindingSwitches(field.getBinding(), SAMPLE_VARIABLES, SAMPLE_VARIABLES.getFirst()));
        tryOutSwitches.getChildren().add(promptTextSwitch(field));

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("showcase-page-description");
        valueLabel.setWrapText(true);
        valueLabel.setText(describe(field));
        field.valueProperty().addListener((observable, oldValue, newValue) -> valueLabel.setText(describe(field)));

        Label bindingLabel = bindingLabel(field.getBinding());

        tryOutContainer.getChildren().addAll(tryOutRow, valueLabel, bindingLabel);
    }

    /** A switch for the try-out field's prompt text, on at the start: the empty field shows it. */
    private static CheckBox promptTextSwitch(TextInput field) {
        CheckBox box = new CheckBox("Prompt text");
        box.setSelected(true);
        box.selectedProperty().addListener((observable, wasSelected, selected) ->
                field.setPromptText(selected ? PROMPT_TEXT : null));
        return box;
    }

    /** The value in quotes, so that spaces at its start and end show. */
    private static String describe(TextInput field) {
        return "value: \"" + field.getValue() + "\"   length: " + field.getValue().length();
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** An empty text input with the sample prompt text, at the page's field width. */
    private static TextInput textInput() {
        TextInput field = new TextInput();
        field.setPromptText(PROMPT_TEXT);
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }

    /** A text input with the sample name, as the filled states show it. */
    private static TextInput filledTextInput() {
        TextInput field = textInput();
        field.setValue(SAMPLE_NAME);
        return field;
    }
}
