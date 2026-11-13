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
import org.karthu.fx.form.CronFormat;
import org.karthu.fx.form.CronInput;
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
 * Showcase page for the cron input, in the group <i>Form inputs</i>: its own six states and the collection's other
 * states side by side, each in a form row with its info line, a cron input beside a text field and a combo input, a
 * field to try out with a switch for each of its states, and a read-out of what the input reads from the expression and
 * of what the last format change made of it.
 *
 * <p>Each card is built by its own method: a later state is one more {@link #addState(String, Node)} call, a later
 * switch one more node in {@code tryOutSwitches}.
 */
public class CronInputShowcaseController {

    /** The width of every field on the page, so that the states line up. */
    private static final double FIELD_WIDTH = 360;

    /** The sample Unix expression: at nine, Monday to Friday. */
    private static final String UNIX_WEEKDAYS = "0 9 * * 1-5";

    /** The sample Quartz expression: the same schedule, with seconds and names. */
    private static final String QUARTZ_WEEKDAYS = "0 0 9 ? * MON-FRI";

    /** The variables the binding list offers on this page: a name and its type. */
    private static final List<BindingCandidate> SAMPLE_VARIABLES = List.of(
            new BindingCandidate("schedule", "String"),
            new BindingCandidate("nightlySchedule", "String"),
            new BindingCandidate("reportCron", "String"));

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
     * The cron input's own six states first (valid in each format, focused, the two errors, disabled), then the
     * collection's others.
     */
    private void buildStates() {
        addState("Unix, valid", cronInput(CronFormat.UNIX, UNIX_WEEKDAYS));
        addState("Quartz, valid", cronInput(CronFormat.QUARTZ, QUARTZ_WEEKDAYS));

        CronInput focused = cronInput(CronFormat.QUARTZ, QUARTZ_WEEKDAYS);
        addState("Focused (on page open)", focused);
        focusWhenShown(focused);

        // Set by the page, as by an application loading a saved schedule: the error is due at once.
        addState("Error, Quartz rule", cronInput(CronFormat.QUARTZ, "0 0 9 * * MON-FRI"));
        addState("Error, wrong format", cronInput(CronFormat.UNIX, "0 0 0 L * ?"));

        CronInput disabled = cronInput(CronFormat.UNIX, UNIX_WEEKDAYS);
        disabled.setDisable(true);
        addState("Disabled", disabled);

        buildBindingStates();

        CronInput readOnly = cronInput(CronFormat.QUARTZ, QUARTZ_WEEKDAYS);
        readOnly.setReadOnly(true);
        addState("Read-only", readOnly);

        CronInput fixed = cronInput(CronFormat.QUARTZ, "0 0/15 * * * ?");
        fixed.setFormatSelectable(false);
        addState("Fixed format", fixed);

        addState("Empty, with placeholder", cronInput(CronFormat.QUARTZ, ""));
    }

    /**
     * The binding's states: bound, and bound to a variable that does not exist any more. Each field is bindable, so it
     * keeps its {@code {}} icon to bind again after the user removes the binding.
     */
    private void buildBindingStates() {
        CronInput bound = cronInput(CronFormat.QUARTZ, QUARTZ_WEEKDAYS);
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        addState("Bound to a variable", bound);

        CronInput missing = cronInput(CronFormat.QUARTZ, QUARTZ_WEEKDAYS);
        offerVariables(missing.getBinding(), SAMPLE_VARIABLES);
        missing.getBinding().setBoundName("deletedSchedule");
        missing.getBinding().setMissing(true);
        addState("Bound, variable missing", missing);
    }

    /**
     * Adds a row to the states card: its name, then the field in a form row with a label. The name sits on the baseline
     * of the row's label.
     */
    private void addState(String name, Node field) {
        FormInputShowcaseSupport.addState(statesGrid, name, field, CronInputShowcaseController::formRow);
    }

    /** A form row around the input, labelled like a scheduler's setting. */
    private static FormField formRow(FormInput<?> input) {
        FormField row = new FormField("Schedule", input);
        row.setDescription("When the job runs, as a cron expression.");
        row.setMaxWidth(FIELD_WIDTH);
        return row;
    }

    // --- Beside other fields -------------------------------------------------------------------------

    /** A plain text field and a combo input beside a cron input: as they rest, read-only and disabled. */
    private void buildComparison() {
        addComparison("Normal", new TextField("A plain text field"), comboInput(), cronInput(CronFormat.UNIX,
                UNIX_WEEKDAYS));

        TextField readOnlyText = new TextField("A read-only text field");
        readOnlyText.setEditable(false);
        ComboInput<String> readOnlyCombo = comboInput();
        readOnlyCombo.setReadOnly(true);
        CronInput readOnlyCron = cronInput(CronFormat.UNIX, UNIX_WEEKDAYS);
        readOnlyCron.setReadOnly(true);
        addComparison("Read-only", readOnlyText, readOnlyCombo, readOnlyCron);

        TextField disabledText = new TextField("A disabled text field");
        disabledText.setDisable(true);
        ComboInput<String> disabledCombo = comboInput();
        disabledCombo.setDisable(true);
        CronInput disabledCron = cronInput(CronFormat.UNIX, UNIX_WEEKDAYS);
        disabledCron.setDisable(true);
        addComparison("Disabled", disabledText, disabledCombo, disabledCron);
    }

    private void addComparison(String name, TextField textField, ComboInput<String> comboInput, CronInput cronInput) {
        Label label = sectionLabel(name);
        double width = FIELD_WIDTH / 2;
        textField.setPrefWidth(width);
        comboInput.setPrefWidth(width);
        comboInput.setMaxWidth(width);
        cronInput.setPrefWidth(FIELD_WIDTH);
        cronInput.setMaxWidth(FIELD_WIDTH);
        comparisonGrid.addRow(comparisonGrid.getRowCount(), label, textField, comboInput, cronInput);
    }

    /** A combo input of time units with one chosen, to compare its chevron with the format selector. */
    private static ComboInput<String> comboInput() {
        ComboInput<String> field = new ComboInput<>();
        field.getItems().addAll("Minutes", "Hours", "Days");
        field.setValue("Hours");
        return field;
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field in its row, with its switches in this order: required, error message, warning message,
     * disabled, read-only, bindable, bound and format selectable; under it the read-out of value, format, problem and
     * description, what the last format change made of the text, and the bound name.
     */
    private void buildTryOut() {
        CronInput field = cronInput(CronFormat.QUARTZ, QUARTZ_WEEKDAYS);
        FormField tryOutRow = formRow(field);

        tryOutSwitches.getChildren().addAll(rowSwitches(tryOutRow,
                FieldMessage.error("Enter when the job runs."),
                FieldMessage.warning("The job runs on the server's time zone.")));
        tryOutSwitches.getChildren().addAll(stateSwitches(field));
        tryOutSwitches.getChildren().addAll(
                bindingSwitches(field.getBinding(), SAMPLE_VARIABLES, SAMPLE_VARIABLES.get(1)));
        CheckBox selectable = new CheckBox("Format selectable");
        selectable.selectedProperty().bindBidirectional(field.formatSelectableProperty());
        tryOutSwitches.getChildren().add(selectable);

        Label value = readOutLine();
        Label format = readOutLine();
        Label problem = readOutLine();
        Label description = readOutLine();
        FormatChangeLine formatChange = new FormatChangeLine(field, readOutLine());
        Runnable showReadOut = () -> {
            value.setText("value: \"" + field.getValue() + "\"");
            format.setText("format: " + field.getFormat().displayName());
            problem.setText("problem: " + orNone(field.getProblem()));
            description.setText("description: " + orNone(field.getDescription()));
        };
        field.valueProperty().addListener((observable, oldValue, newValue) -> {
            formatChange.valueChanged(oldValue);
            showReadOut.run();
        });
        field.formatProperty().addListener((observable, oldFormat, newFormat) -> {
            formatChange.formatChanged(oldFormat, newFormat);
            showReadOut.run();
        });
        field.problemProperty().addListener((observable, oldProblem, newProblem) -> showReadOut.run());
        field.descriptionProperty().addListener((observable, oldText, newText) -> showReadOut.run());
        showReadOut.run();

        tryOutContainer.getChildren().addAll(tryOutRow, value, format, problem, description, formatChange.line,
                bindingLabel(field.getBinding()));
    }

    /** A line of the try-out field's read-out, in the page description's style. */
    private static Label readOutLine() {
        Label line = new Label();
        line.getStyleClass().add("showcase-page-description");
        return line;
    }

    private static String orNone(String text) {
        return text.isEmpty() ? "(none)" : text;
    }

    /**
     * The read-out's line of the last format change: what the input made of the text, converted or kept. The input
     * converts in its own listener of the format, registered before this page's, so the converted value arrives while
     * this page has not yet heard of the new format: a value change then is the conversion.
     */
    private static final class FormatChangeLine {

        private final CronInput field;
        private final Label line;
        /** The format this page last heard of. */
        private CronFormat heardFormat;
        /** The text before the conversion of the format change under way, or null when there is none. */
        private String convertedFrom;

        private FormatChangeLine(CronInput field, Label line) {
            this.field = field;
            this.line = line;
            heardFormat = field.getFormat();
            line.setText("format change: (none yet)");
        }

        private void valueChanged(String oldValue) {
            if (field.getFormat() != heardFormat) {
                convertedFrom = oldValue;
            }
        }

        private void formatChanged(CronFormat oldFormat, CronFormat newFormat) {
            String change = "format change: " + oldFormat.displayName() + " to " + newFormat.displayName() + ", ";
            if (convertedFrom != null) {
                line.setText(change + "\"" + convertedFrom + "\" converted to \"" + field.getValue() + "\"");
            } else {
                line.setText(change + "\"" + field.getValue() + "\" kept");
            }
            convertedFrom = null;
            heardFormat = newFormat;
        }
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** A bindable cron input in a format with an expression, at the page's width. */
    private static CronInput cronInput(CronFormat format, String expression) {
        CronInput field = new CronInput();
        field.setFormat(format);
        field.setValue(expression);
        field.getBinding().setBindable(true);
        offerVariables(field.getBinding(), SAMPLE_VARIABLES);
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }
}
