package org.karthu.fx.showcase.controller.showcase;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.karthu.fx.form.BindingCandidate;
import org.karthu.fx.form.FieldBinding;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.FormInput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * What the showcase pages of the form inputs share: the states card's rows, the try-out field's switches for its row,
 * its states and its binding, the binding line under it and the focused state. Each page keeps what is its own: its
 * fields, its labels and messages, its sample variables and its own switches.
 */
final class FormInputShowcaseSupport {

    private FormInputShowcaseSupport() {
    }

    // --- States ----------------------------------------------------------------------------------------

    /**
     * Adds a row to a page's states card: the state's name on the baseline of the row's label, then the field in a
     * form row made by the page (a field given as a form row, or any other node, stays as it is).
     *
     * @param grid    the states card's grid
     * @param name    the state's name
     * @param field   the input, or a form row or another node to show as it is
     * @param formRow the page's form row around an input
     */
    static void addState(GridPane grid, String name, Node field, Function<FormInput<?>, FormField> formRow) {
        Node shown = field;
        if (field instanceof FormInput<?> input) {
            shown = formRow.apply(input);
        }
        addState(grid, name, shown);
    }

    /**
     * Adds a row to a page's states card: the state's name on the baseline of the row's label, then the row.
     *
     * @param grid the states card's grid
     * @param name the state's name
     * @param row  the form row, or another node, to show
     */
    static void addState(GridPane grid, String name, Node row) {
        Label label = sectionLabel(name);
        GridPane.setValignment(label, VPos.BASELINE);
        grid.addRow(grid.getRowCount(), label, row);
    }

    /**
     * Creates the name of a row of a card, a state's or a comparison's.
     *
     * @param name the row's name
     * @return a label with the style class {@code section-label}
     */
    static Label sectionLabel(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("section-label");
        return label;
    }

    /**
     * Gives the field the focus once the page is in a scene, so the focused state shows when the page opens.
     *
     * @param field the field to focus
     */
    static void focusWhenShown(Node field) {
        field.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> field.requestFocus());
            }
        });
    }

    // --- Binding ---------------------------------------------------------------------------------------

    /**
     * Fills a binding's candidates with a page's sample variables.
     *
     * @param binding   the field's binding
     * @param variables the page's sample variables
     */
    static void offerVariables(FieldBinding binding, List<BindingCandidate> variables) {
        binding.getCandidates().setAll(variables);
    }

    /**
     * Creates the line under the try-out field that shows its bound name, kept in step with the binding.
     *
     * @param binding the try-out field's binding
     * @return the line, a label in the page description's style
     */
    static Label bindingLabel(FieldBinding binding) {
        Label label = new Label();
        label.getStyleClass().add("showcase-page-description");
        label.setText(describeBinding(binding));
        binding.boundNameProperty().addListener((observable, oldName, newName) ->
                label.setText(describeBinding(binding)));
        return label;
    }

    private static String describeBinding(FieldBinding binding) {
        return "bound name: " + (binding.isBound() ? "\"" + binding.getBoundName() + "\"" : "null (unbound)");
    }

    // --- Switches of the try-out field -----------------------------------------------------------------

    /**
     * Creates the switches of the try-out field's row: required, and an error and a warning message, which the row
     * shows in that order while switched on.
     *
     * @param row     the try-out field's row
     * @param error   the error message of the page
     * @param warning the warning message of the page
     * @return the switches, in their order
     */
    static List<CheckBox> rowSwitches(FormField row, FieldMessage error, FieldMessage warning) {
        CheckBox required = new CheckBox("Required");
        required.selectedProperty().bindBidirectional(row.requiredProperty());
        CheckBox errorSwitch = new CheckBox("Error message");
        CheckBox warningSwitch = new CheckBox("Warning message");
        Runnable showMessages = () -> {
            List<FieldMessage> messages = new ArrayList<>();
            if (errorSwitch.isSelected()) {
                messages.add(error);
            }
            if (warningSwitch.isSelected()) {
                messages.add(warning);
            }
            row.getMessages().setAll(messages);
        };
        errorSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> showMessages.run());
        warningSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> showMessages.run());
        return List.of(required, errorSwitch, warningSwitch);
    }

    /**
     * Creates the switches of the try-out field's own states: disabled and read-only.
     *
     * @param field the try-out field
     * @return the switches, in their order
     */
    static List<CheckBox> stateSwitches(FormInput<?> field) {
        CheckBox disabled = new CheckBox("Disabled");
        disabled.selectedProperty().bindBidirectional(field.getNode().disableProperty());
        CheckBox readOnly = new CheckBox("Read-only");
        readOnly.selectedProperty().bindBidirectional(field.readOnlyProperty());
        return List.of(disabled, readOnly);
    }

    /**
     * Creates the try-out field's binding switches: bindable, with the page's sample variables, and bound, to one of
     * them, kept in step with what the user does in the field.
     *
     * @param binding   the try-out field's binding
     * @param variables the page's sample variables, offered by the binding list
     * @param boundTo   the variable the switch binds to
     * @return the switches, in their order
     */
    static List<CheckBox> bindingSwitches(FieldBinding binding, List<BindingCandidate> variables,
                                          BindingCandidate boundTo) {
        offerVariables(binding, variables);
        CheckBox bindable = new CheckBox("Bindable");
        bindable.selectedProperty().bindBidirectional(binding.bindableProperty());
        CheckBox bound = new CheckBox("Bound");
        bound.setSelected(binding.isBound());
        bound.selectedProperty().addListener((observable, wasBound, isBound) -> {
            if (isBound && !binding.isBound()) {
                binding.setBoundName(boundTo.name());
            } else if (!isBound) {
                binding.setBoundName(null);
            }
        });
        binding.boundNameProperty().addListener((observable, oldName, newName) -> bound.setSelected(newName != null));
        return List.of(bindable, bound);
    }
}
