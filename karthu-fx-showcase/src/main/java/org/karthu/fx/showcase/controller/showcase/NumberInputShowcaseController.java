package org.karthu.fx.showcase.controller.showcase;

import javafx.beans.InvalidationListener;
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
import org.karthu.fx.form.NumberInput;
import org.karthu.fx.form.PathInput;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.focusWhenShown;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.offerVariables;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.sectionLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.stateSwitches;

/**
 * Showcase page for the number input, in the group <i>Form inputs</i>: its states side by side, each in a form row with
 * its label, a number input beside a plain text field and a path input, and a field to try out in its row with a
 * switch for each of its states.
 *
 * <p>The showcase plays the application here: it reports a text that is no number as an error and a number outside
 * the range as a warning, as an application reads {@link NumberInput#toNumber()} and reports its findings.
 *
 * <p>The spinner, the number input with its arrows, has its own states, a row beside the other fields, the try-out
 * switches <i>Arrows</i> and <i>Step 0.25</i>.
 *
 * <p>Each card is built by its own method: a later state is one more {@link #addState(String, Node)} call, a later
 * switch one more node in {@code tryOutSwitches}.
 */
public class NumberInputShowcaseController {

    /** The width of every field on the page, so that the states line up. */
    private static final double FIELD_WIDTH = 240;

    /** The range the try-out field's range switch sets. */
    private static final BigDecimal RANGE_MIN = BigDecimal.ZERO;
    private static final BigDecimal RANGE_MAX = BigDecimal.valueOf(100);

    /** The variables the binding list offers on this page: a name and its type. */
    private static final List<BindingCandidate> SAMPLE_VARIABLES = List.of(
            new BindingCandidate("maxRows", "Integer"),
            new BindingCandidate("threshold", "Double"),
            new BindingCandidate("retries", "Integer"));

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
     * Every state, in its order: plain, then the application's findings, then disabled, read-only and binding, then
     * the type's own.
     */
    private void buildStates() {
        addState("Default (empty)", numberInput());
        addState("Filled", numberInput("1250"));

        NumberInput focused = numberInput("1250");
        addState("Focused (on page open)", focused);
        focusWhenShown(focused);

        FormField required = formRow(numberInput());
        required.setRequired(true);
        required.getMessages().add(FieldMessage.error("Enter the number of rows to read."));
        addState("Required, with an error", required);

        FormField notANumber = formRow(numberInput("12,5"));
        notANumber.getMessages().add(FieldMessage.error("Not a number: use a dot as the decimal separator."));
        addState("Not a number (as typed)", notANumber);

        NumberInput outside = numberInput("250");
        outside.setMin(RANGE_MIN);
        outside.setMax(RANGE_MAX);
        FormField outsideRow = formRow(outside);
        outsideRow.getMessages().add(FieldMessage.warning("Above the maximum of 100."));
        addState("Outside the range (warning)", outsideRow);

        NumberInput disabled = numberInput("1250");
        disabled.setDisable(true);
        addState("Disabled", disabled);

        NumberInput readOnly = numberInput("1250");
        readOnly.setReadOnly(true);
        addState("Read-only", readOnly);

        buildBindingStates();

        addState("Decimal", numberInput("-0.125"));
        addState("With an exponent", numberInput("1.0E21"));

        NumberInput whole = numberInput("12.5");
        whole.setWholeNumbers(true);
        FormField wholeRow = formRow(whole);
        wholeRow.getMessages().add(FieldMessage.error("Not a whole number."));
        addState("Whole numbers (12.5 is none)", wholeRow);

        NumberInput prompt = numberInput();
        prompt.setPromptText("0 to 100");
        addState("With a prompt text", prompt);

        buildSpinnerStates();
    }

    /**
     * The spinner's states: the arrows at rest, with a decimal step, bound (arrows disabled), read-only (no arrows),
     * disabled, and with a text outside the range that the next arrow brings back.
     */
    private void buildSpinnerStates() {
        addState("Spinner", spinner("3"));

        NumberInput decimal = spinner("0.25");
        decimal.setStep(new BigDecimal("0.25"));
        addState("Spinner, step 0.25", decimal);

        NumberInput bound = spinner("3");
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.get(2).name());
        addState("Spinner, bound", bound);

        NumberInput readOnly = spinner("3");
        readOnly.setReadOnly(true);
        addState("Spinner, read-only", readOnly);

        NumberInput disabled = spinner("3");
        disabled.setDisable(true);
        addState("Spinner, disabled", disabled);

        NumberInput outside = spinner("250");
        FormField outsideRow = formRow(outside);
        outsideRow.getMessages().add(FieldMessage.warning("Above the maximum of 100: the next arrow sets 100."));
        addState("Spinner, outside the range", outsideRow);
    }

    /**
     * The binding's states: bound, bound to a variable that does not exist any more, and bound while read-only (the
     * chip without its remove button). Each field is bindable, as an application's bindable setting is, so it keeps
     * its {@code {}} icon to bind again after the user removes the binding.
     */
    private void buildBindingStates() {
        NumberInput bound = numberInput("1250");
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        addState("Bound to a variable", bound);

        NumberInput missing = numberInput("1250");
        offerVariables(missing.getBinding(), SAMPLE_VARIABLES);
        missing.getBinding().setBindable(true);
        missing.getBinding().setBoundName("deletedVariable");
        missing.getBinding().setMissing(true);
        addState("Bound, variable missing", missing);

        NumberInput readOnlyBound = numberInput("1250");
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
        FormInputShowcaseSupport.addState(statesGrid, name, field, NumberInputShowcaseController::formRow);
    }

    /** A form row around the input, labelled like a reader's row limit. */
    private static FormField formRow(FormInput<?> input) {
        FormField row = new FormField("Maximum rows", input);
        row.setDescription("How many rows the node reads at most.");
        row.setMaxWidth(FIELD_WIDTH);
        return row;
    }

    // --- Beside other fields -------------------------------------------------------------------------

    /** A plain text field and a path input beside a number input: as they rest, read-only and disabled. */
    private void buildComparison() {
        addComparison("Normal", new TextField("A plain text field"), numberInput("1250"), pathInput());

        TextField readOnlyText = new TextField("A read-only text field");
        readOnlyText.setEditable(false);
        NumberInput readOnlyNumber = numberInput("1250");
        readOnlyNumber.setReadOnly(true);
        PathInput readOnlyPath = pathInput();
        readOnlyPath.setReadOnly(true);
        addComparison("Read-only", readOnlyText, readOnlyNumber, readOnlyPath);

        TextField disabledText = new TextField("A disabled text field");
        disabledText.setDisable(true);
        NumberInput disabledNumber = numberInput("1250");
        disabledNumber.setDisable(true);
        PathInput disabledPath = pathInput();
        disabledPath.setDisable(true);
        addComparison("Disabled", disabledText, disabledNumber, disabledPath);

        addComparison("Spinner", new TextField("A plain text field"), spinner("3"), pathInput());
    }

    private void addComparison(String name, TextField textField, NumberInput numberInput, PathInput pathInput) {
        Label label = sectionLabel(name);
        textField.setPrefWidth(FIELD_WIDTH);
        comparisonGrid.addRow(comparisonGrid.getRowCount(), label, textField, numberInput, pathInput);
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field in its row, with its switches in this order: required, disabled, read-only, bindable, bound,
     * whole numbers and the range. The showcase checks the field as an application would, on every change of the
     * text, of whole numbers, of the range and of required.
     */
    private void buildTryOut() {
        NumberInput field = numberInput();
        FormField tryOutRow = formRow(field);

        CheckBox required = new CheckBox("Required");
        required.selectedProperty().bindBidirectional(tryOutRow.requiredProperty());
        tryOutSwitches.getChildren().add(required);
        tryOutSwitches.getChildren().addAll(stateSwitches(field));
        tryOutSwitches.getChildren().addAll(
                bindingSwitches(field.getBinding(), SAMPLE_VARIABLES, SAMPLE_VARIABLES.getFirst()));
        CheckBox wholeNumbers = new CheckBox("Whole numbers");
        wholeNumbers.selectedProperty().bindBidirectional(field.wholeNumbersProperty());
        tryOutSwitches.getChildren().addAll(wholeNumbers, rangeSwitch(field));
        addSpinnerSwitches(field);

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("showcase-page-description");
        valueLabel.setWrapText(true);

        Label bindingLabel = bindingLabel(field.getBinding());

        InvalidationListener check = observable -> checkTryOut(field, tryOutRow, valueLabel);
        field.valueProperty().addListener(check);
        field.wholeNumbersProperty().addListener(check);
        field.minProperty().addListener(check);
        field.maxProperty().addListener(check);
        tryOutRow.requiredProperty().addListener(check);
        checkTryOut(field, tryOutRow, valueLabel);

        tryOutContainer.getChildren().addAll(tryOutRow, valueLabel, bindingLabel);
    }

    /** Checks the try-out field as an application would: its row shows the findings, the label what was read. */
    private static void checkTryOut(NumberInput field, FormField row, Label valueLabel) {
        row.getMessages().setAll(findings(field, row.isRequired()));
        valueLabel.setText(describe(field));
    }

    /**
     * What an application would report for the field: a missing required number, a text that is no (whole) number
     * as an error, a number outside the range as a warning.
     */
    private static List<FieldMessage> findings(NumberInput field, boolean required) {
        List<FieldMessage> messages = new ArrayList<>();
        String text = field.getValue();
        if (text == null || text.isBlank()) {
            if (required) {
                messages.add(FieldMessage.error("Enter the number of rows to read."));
            }
            return messages;
        }
        Optional<BigDecimal> number = field.toNumber();
        if (number.isEmpty()) {
            messages.add(FieldMessage.error(field.isWholeNumbers()
                    ? "Not a whole number: \"" + text + "\"."
                    : "Not a number: \"" + text + "\". Use a dot as the decimal separator, without grouping."));
            return messages;
        }
        BigDecimal min = field.getMin();
        BigDecimal max = field.getMax();
        if (min != null && number.get().compareTo(min) < 0) {
            messages.add(FieldMessage.warning("Below the minimum of " + min.toPlainString() + "."));
        } else if (max != null && number.get().compareTo(max) > 0) {
            messages.add(FieldMessage.warning("Above the maximum of " + max.toPlainString() + "."));
        }
        return messages;
    }

    /** The spinner's switches: the arrows, and a decimal step of 0.25 in place of 1. */
    private void addSpinnerSwitches(NumberInput field) {
        CheckBox arrows = new CheckBox("Arrows");
        arrows.selectedProperty().bindBidirectional(field.arrowsVisibleProperty());
        CheckBox quarterStep = new CheckBox("Step 0.25");
        quarterStep.selectedProperty().addListener((observable, wasSelected, selected) ->
                field.setStep(selected ? new BigDecimal("0.25") : BigDecimal.ONE));
        tryOutSwitches.getChildren().addAll(arrows, quarterStep);
    }

    /** A switch that sets the range 0 to 100 with a prompt text that names it, and clears both again. */
    private static CheckBox rangeSwitch(NumberInput field) {
        CheckBox range = new CheckBox("Range 0 to 100");
        range.selectedProperty().addListener((observable, wasSelected, selected) -> {
            field.setMin(selected ? RANGE_MIN : null);
            field.setMax(selected ? RANGE_MAX : null);
            field.setPromptText(selected ? "0 to 100" : "");
        });
        return range;
    }

    private static String describe(NumberInput field) {
        Optional<BigDecimal> number = field.toNumber();
        String numberText = number.isPresent() ? number.get().toString() : "(empty: no number)";
        return "value: \"" + field.getValue() + "\"   toNumber(): " + numberText;
    }

    // --- Helpers -------------------------------------------------------------------------------------

    private static NumberInput numberInput() {
        NumberInput field = new NumberInput();
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }

    /** A number input with a sample text, as the filled states show it. */
    private static NumberInput numberInput(String text) {
        NumberInput field = numberInput();
        field.setValue(text);
        return field;
    }

    /** A spinner with a sample text: the number input with its arrows, in the range 0 to 100. */
    private static NumberInput spinner(String text) {
        NumberInput field = numberInput(text);
        field.setArrowsVisible(true);
        field.setMin(RANGE_MIN);
        field.setMax(RANGE_MAX);
        return field;
    }

    /** A path input with a short relative sample path, for the comparison. */
    private static PathInput pathInput() {
        PathInput field = new PathInput();
        field.setValue("data/customers.csv");
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }
}
