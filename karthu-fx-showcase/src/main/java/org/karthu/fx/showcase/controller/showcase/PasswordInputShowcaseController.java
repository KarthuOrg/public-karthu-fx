package org.karthu.fx.showcase.controller.showcase;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.karthu.fx.form.BindingCandidate;
import org.karthu.fx.form.FieldBinding;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.FormInput;
import org.karthu.fx.form.PasswordInput;

import java.util.List;

import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.focusWhenShown;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.offerVariables;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.rowSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.sectionLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.stateSwitches;

/**
 * Showcase page for the password input, in the group <i>Form inputs</i>: its states side by side, each in a form row
 * with its label, a password input beside a plain text field and JavaFX's own password field, and a field to try out
 * in its row with a switch for each of its states.
 */
public class PasswordInputShowcaseController {

    /** The width of every field on the page, so that the states line up. */
    private static final double FIELD_WIDTH = 360;

    /** A sample secret, as the filled states show it. */
    private static final String SAMPLE_PASSWORD = "correct-horse-battery";

    /** The variables the binding list offers on this page: a name and its type. */
    private static final List<BindingCandidate> SAMPLE_VARIABLES = List.of(
            new BindingCandidate("dbPassword", "Secret"),
            new BindingCandidate("apiToken", "Secret"));

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

    /** Every state, in its order, then the type's own: the eye, revealed, read-only with the eye. */
    private void buildStates() {
        addState("Default (empty)", passwordInput());
        addState("Filled", filledPasswordInput());

        PasswordInput focused = filledPasswordInput();
        addState("Focused (on page open)", focused);
        focusWhenShown(focused);

        PasswordInput disabled = filledPasswordInput();
        disabled.setRevealAllowed(true);
        disabled.setDisable(true);
        addState("Disabled", disabled);

        FormField error = formRow(filledPasswordInput());
        error.getMessages().add(FieldMessage.error("The password is wrong."));
        addState("Error", error);

        FormField warning = formRow(filledPasswordInput());
        warning.getMessages().add(FieldMessage.warning("The password is stored in plain text in the workflow."));
        addState("Warning", warning);

        PasswordInput readOnly = filledPasswordInput();
        readOnly.setReadOnly(true);
        addState("Read-only", readOnly);

        buildBindingStates();

        PasswordInput allowed = filledPasswordInput();
        allowed.setRevealAllowed(true);
        addState("Reveal allowed", allowed);

        PasswordInput revealed = filledPasswordInput();
        revealed.setRevealAllowed(true);
        reveal(revealed);
        addState("Revealed", revealed);

        PasswordInput readOnlyAllowed = filledPasswordInput();
        readOnlyAllowed.setRevealAllowed(true);
        readOnlyAllowed.setReadOnly(true);
        addState("Read-only, reveal allowed", readOnlyAllowed);
    }

    /**
     * The binding's states: bound with the eye (disabled while bound), and bound to a variable that does not exist
     * any more. Each field is bindable, as an application's bindable setting is, so it keeps its {@code {}} icon to
     * bind again after the user removes the binding.
     */
    private void buildBindingStates() {
        PasswordInput bound = filledPasswordInput();
        bound.setRevealAllowed(true);
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        addState("Bound, reveal allowed", bound);

        PasswordInput missing = filledPasswordInput();
        offerVariables(missing.getBinding(), SAMPLE_VARIABLES);
        missing.getBinding().setBindable(true);
        missing.getBinding().setBoundName("deletedVariable");
        missing.getBinding().setMissing(true);
        addState("Bound, variable missing", missing);
    }

    /**
     * Adds a row to the states card: its name, then the field in a form row with a label (a field given as a form
     * row already stays as it is). The name sits on the baseline of the row's label.
     */
    private void addState(String name, Node field) {
        FormInputShowcaseSupport.addState(statesGrid, name, field, PasswordInputShowcaseController::formRow);
    }

    /** A form row around the input, labelled like a database node's password setting. */
    private static FormField formRow(FormInput<?> input) {
        FormField row = new FormField("Password", input);
        row.setDescription("The password of the database user.");
        row.setMaxWidth(FIELD_WIDTH);
        return row;
    }

    // --- Beside a text field -------------------------------------------------------------------------

    /** A plain text field and JavaFX's password field beside a password input: at rest, read-only, disabled. */
    private void buildComparison() {
        addComparison("Normal", new TextField("A plain text field"), javaFxPasswordField(), filledPasswordInput());

        TextField readOnlyText = new TextField("A read-only text field");
        readOnlyText.setEditable(false);
        PasswordField readOnlyPassword = javaFxPasswordField();
        readOnlyPassword.setEditable(false);
        PasswordInput readOnlyInput = filledPasswordInput();
        readOnlyInput.setRevealAllowed(true);
        readOnlyInput.setReadOnly(true);
        addComparison("Read-only", readOnlyText, readOnlyPassword, readOnlyInput);

        TextField disabledText = new TextField("A disabled text field");
        disabledText.setDisable(true);
        PasswordField disabledPassword = javaFxPasswordField();
        disabledPassword.setDisable(true);
        PasswordInput disabledInput = filledPasswordInput();
        disabledInput.setRevealAllowed(true);
        disabledInput.setDisable(true);
        addComparison("Disabled", disabledText, disabledPassword, disabledInput);
    }

    private void addComparison(String name, TextField textField, PasswordField passwordField, PasswordInput input) {
        Label label = sectionLabel(name);
        textField.setPrefWidth(FIELD_WIDTH / 2);
        passwordField.setPrefWidth(FIELD_WIDTH / 2);
        comparisonGrid.addRow(comparisonGrid.getRowCount(), label, textField, passwordField, input);
    }

    private static PasswordField javaFxPasswordField() {
        PasswordField field = new PasswordField();
        field.setText(SAMPLE_PASSWORD);
        return field;
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field in its row, with its switches in this order: required, error message, warning message,
     * disabled, read-only, reveal allowed, bindable, bound; under it, its value and state, and a text field to paste
     * into, to see what reached the clipboard.
     */
    private void buildTryOut() {
        PasswordInput field = passwordInput();
        field.setPromptText("Enter the password");
        FormField tryOutRow = formRow(field);

        tryOutSwitches.getChildren().addAll(rowSwitches(tryOutRow,
                FieldMessage.error("The password is wrong."),
                FieldMessage.warning("The password is stored in plain text in the workflow.")));
        addStateSwitches(field);
        tryOutSwitches.getChildren().addAll(
                bindingSwitches(field.getBinding(), SAMPLE_VARIABLES, SAMPLE_VARIABLES.getFirst()));

        Label stateLabel = new Label();
        stateLabel.getStyleClass().add("showcase-page-description");
        stateLabel.setWrapText(true);
        stateLabel.setText(describe(field));
        field.valueProperty().addListener((observable, oldValue, newValue) -> stateLabel.setText(describe(field)));
        field.revealedProperty().addListener((observable, wasRevealed, isRevealed) ->
                stateLabel.setText(describe(field)));
        field.getBinding().boundNameProperty().addListener((observable, oldName, newName) ->
                stateLabel.setText(describe(field)));

        TextField pasteTarget = new TextField();
        pasteTarget.setPromptText("Paste here to see what reached the clipboard");
        pasteTarget.setMaxWidth(FIELD_WIDTH);

        tryOutContainer.getChildren().addAll(tryOutRow, stateLabel, pasteTarget);
    }

    /** The switches of the try-out field's own states: disabled, read-only and reveal allowed. */
    private void addStateSwitches(PasswordInput field) {
        CheckBox revealAllowed = new CheckBox("Reveal allowed");
        revealAllowed.selectedProperty().bindBidirectional(field.revealAllowedProperty());
        tryOutSwitches.getChildren().addAll(stateSwitches(field));
        tryOutSwitches.getChildren().add(revealAllowed);
    }

    /** The try-out field's value (shown here, on a showcase page only), whether it is revealed, and its binding. */
    private static String describe(PasswordInput field) {
        FieldBinding binding = field.getBinding();
        String boundName = binding.isBound() ? "\"" + binding.getBoundName() + "\"" : "null (unbound)";
        return "value: \"" + field.getValue() + "\"   revealed: " + field.isRevealed() + "   bound name: " + boundName;
    }

    // --- Helpers -------------------------------------------------------------------------------------

    private static PasswordInput passwordInput() {
        PasswordInput field = new PasswordInput();
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }

    /** A password input with the sample password, as the filled states show it. */
    private static PasswordInput filledPasswordInput() {
        PasswordInput field = passwordInput();
        field.setValue(SAMPLE_PASSWORD);
        return field;
    }

    /**
     * Reveals a field's text as the user does, with its eye (the style class {@code form-password-reveal}, part of
     * the input's contract): only the user reveals a password, so the input offers no method for it. Revealed before
     * the page is shown, the field stays revealed until the user clicks into it and leaves it.
     */
    private static void reveal(PasswordInput field) {
        Node eye = field.lookup(".form-password-reveal");
        if (eye instanceof ButtonBase button) {
            button.fire();
        }
    }
}
