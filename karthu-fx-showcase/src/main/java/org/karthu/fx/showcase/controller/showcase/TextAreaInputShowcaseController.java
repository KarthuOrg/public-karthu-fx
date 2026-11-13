package org.karthu.fx.showcase.controller.showcase;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.karthu.fx.form.BindingCandidate;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.TextAreaInput;

import java.util.List;

import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingLabel;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.bindingSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.focusWhenShown;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.offerVariables;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.rowSwitches;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.stateSwitches;

/**
 * Showcase page for the text area input, in the group <i>Form inputs</i>: its states side by side, each in a form row
 * with its label, and a field to try out in its row with a switch for each of its states and buttons that remember
 * and restore the dragged size.
 *
 * <p>Each card is built by its own method: a later state is one more {@link #addState(String, FormField)} call, a
 * later switch one more node in {@code tryOutSwitches}.
 */
public class TextAreaInputShowcaseController {

    /** The width of every form row in the states card, so that the states line up. */
    private static final double FIELD_WIDTH = 360;

    /** The widest the try-out row grows, so that dragging the grip wider has an end on a wide window. */
    private static final double TRY_OUT_WIDTH = 640;

    /** The rows the try-out field offers. */
    private static final List<Integer> ROW_COUNTS = List.of(2, 3, 4, 6, 8, 12);

    /** The variables the binding list offers on this page: a name and its type. */
    private static final List<BindingCandidate> SAMPLE_VARIABLES = List.of(
            new BindingCandidate("description", "String"),
            new BindingCandidate("mailBody", "String"),
            new BindingCandidate("sqlQuery", "String"));

    /** A description of a few lines, the last one long enough to wrap. */
    private static final String SAMPLE_TEXT = "Reads the customers of 2026.\n"
            + "Keeps the active ones.\n"
            + "Writes them to the CRM export, one line per customer, with the address and the date of the last order.";

    /** More lines than the field has rows, so that its scroll bar shows. */
    private static final String LONG_TEXT = "SELECT c.id, c.name, c.city\n"
            + "FROM customers c\n"
            + "JOIN orders o ON o.customer_id = c.id\n"
            + "WHERE o.created >= DATE '2026-01-01'\n"
            + "  AND c.active\n"
            + "GROUP BY c.id, c.name, c.city\n"
            + "ORDER BY c.name";

    @FXML private GridPane statesGrid;
    @FXML private FlowPane tryOutSwitches;
    @FXML private VBox tryOutContainer;

    /** The size the try-out field's Remember size button kept, null before. */
    private Dimension2D rememberedSize;

    @FXML
    private void initialize() {
        buildStates();
        buildTryOut();
    }

    // --- States --------------------------------------------------------------------------------------

    /**
     * Every state, in its order: plain, then the messages, disabled, read-only and binding, then the type's own.
     */
    private void buildStates() {
        TextAreaInput empty = new TextAreaInput();
        empty.setPromptText("What the step does, for the workflow's readers");
        addState("Default (empty)", formRow(empty));
        addState("Filled", formRow(filled()));

        TextAreaInput focused = filled();
        addState("Focused (on page open)", formRow(focused));
        focusWhenShown(focused);

        FormField error = formRow(new TextAreaInput());
        error.setRequired(true);
        error.getMessages().add(FieldMessage.error("Describe what the step does."));
        addState("Required, with an error", error);

        FormField warning = formRow(filled());
        warning.getMessages().add(FieldMessage.warning("The description names a column that no longer exists."));
        addState("Warning", warning);

        TextAreaInput disabled = filled();
        disabled.setDisable(true);
        addState("Disabled", formRow(disabled));

        TextAreaInput readOnly = filled();
        readOnly.setReadOnly(true);
        addState("Read-only", formRow(readOnly));

        buildBindingStates();
        buildOwnStates();
    }

    /**
     * The binding's states: bound, bound to a variable that does not exist any more, and bound while read-only (the
     * chip without its remove button). Each field is bindable, so it keeps its {@code {}} icon after unbinding.
     */
    private void buildBindingStates() {
        TextAreaInput bound = filled();
        offerVariables(bound.getBinding(), SAMPLE_VARIABLES);
        bound.getBinding().setBindable(true);
        bound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        addState("Bound to a variable", formRow(bound));

        TextAreaInput missing = filled();
        offerVariables(missing.getBinding(), SAMPLE_VARIABLES);
        missing.getBinding().setBindable(true);
        missing.getBinding().setBoundName("deletedVariable");
        missing.getBinding().setMissing(true);
        addState("Bound, variable missing", formRow(missing));

        TextAreaInput readOnlyBound = filled();
        offerVariables(readOnlyBound.getBinding(), SAMPLE_VARIABLES);
        readOnlyBound.getBinding().setBindable(true);
        readOnlyBound.getBinding().setBoundName(SAMPLE_VARIABLES.getFirst().name());
        readOnlyBound.setReadOnly(true);
        addState("Read-only, bound", formRow(readOnlyBound));
    }

    /** The type's own states: no wrapping, more lines than rows, two rows, and a size the user dragged to. */
    private void buildOwnStates() {
        TextAreaInput noWrap = filled();
        noWrap.setWrapText(false);
        addState("No wrapping", formRow(noWrap));

        TextAreaInput scrolled = new TextAreaInput();
        scrolled.setValue(LONG_TEXT);
        offerVariables(scrolled.getBinding(), SAMPLE_VARIABLES);
        scrolled.getBinding().setBindable(true);
        addState("More lines than rows", formRow(scrolled));

        TextAreaInput twoRows = new TextAreaInput();
        twoRows.setPrefRowCount(2);
        twoRows.setValue("Two rows high.");
        addState("Two rows", formRow(twoRows));

        TextAreaInput resized = filled();
        resized.setUserSize(new Dimension2D(300, 140));
        addState("Resized (300 \u00D7 140)", formRow(resized));
    }

    /** Adds a row to the states card: its name, then the form row. The name sits on the baseline of the row's label. */
    private void addState(String name, FormField row) {
        FormInputShowcaseSupport.addState(statesGrid, name, row);
    }

    /**
     * A form row around the field, labelled like a step's description setting, as wide as the states card's column:
     * the field fills it, so its grip makes it narrower and back.
     */
    private static FormField formRow(TextAreaInput field) {
        FormField row = new FormField("Description", field);
        row.setDescription("What the step does, shown to the workflow's readers.");
        row.setPrefWidth(FIELD_WIDTH);
        row.setMaxWidth(FIELD_WIDTH);
        return row;
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field in its row, with its switches in this order: required, error message, warning message,
     * disabled, read-only, bindable, bound, wrap text, prompt text and rows; then the size buttons.
     */
    private void buildTryOut() {
        TextAreaInput field = new TextAreaInput();
        field.setValue(SAMPLE_TEXT);
        FormField tryOutRow = new FormField("Description", field);
        tryOutRow.setDescription("What the step does, shown to the workflow's readers.");
        tryOutRow.setMaxWidth(TRY_OUT_WIDTH);

        tryOutSwitches.getChildren().addAll(rowSwitches(tryOutRow,
                FieldMessage.error("Describe what the step does."),
                FieldMessage.warning("The description names a column that no longer exists.")));
        tryOutSwitches.getChildren().addAll(stateSwitches(field));
        tryOutSwitches.getChildren().addAll(
                bindingSwitches(field.getBinding(), SAMPLE_VARIABLES, SAMPLE_VARIABLES.getFirst()));
        addTextSwitches(field);

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("showcase-page-description");
        valueLabel.setText(describe(field));
        field.valueProperty().addListener((observable, oldValue, newValue) -> valueLabel.setText(describe(field)));

        Label sizeLabel = new Label();
        sizeLabel.getStyleClass().add("showcase-page-description");
        sizeLabel.setText(describeSize(field.getUserSize()));
        field.userSizeProperty().addListener((observable, oldSize, newSize) -> sizeLabel.setText(describeSize(newSize)));

        Label bindingLabel = bindingLabel(field.getBinding());

        tryOutContainer.getChildren().addAll(tryOutRow, sizeButtons(field, sizeLabel), valueLabel, bindingLabel);
    }

    /** The switches of the text itself: wrap text, a prompt text (seen when the field is emptied) and the rows. */
    private void addTextSwitches(TextAreaInput field) {
        CheckBox wrap = new CheckBox("Wrap text");
        wrap.selectedProperty().bindBidirectional(field.wrapTextProperty());

        CheckBox prompt = new CheckBox("Prompt text");
        prompt.selectedProperty().addListener((observable, wasSelected, selected) ->
                field.setPromptText(selected ? "What the step does, for the workflow's readers" : ""));

        ChoiceBox<Integer> rows = new ChoiceBox<>(FXCollections.observableArrayList(ROW_COUNTS));
        rows.setValue(field.getPrefRowCount());
        rows.valueProperty().addListener((observable, oldRows, newRows) -> {
            if (newRows != null) {
                field.setPrefRowCount(newRows);
            }
        });
        HBox rowsSwitch = new HBox(6, new Label("Rows"), rows);
        rowsSwitch.setAlignment(Pos.CENTER_LEFT);

        tryOutSwitches.getChildren().addAll(wrap, prompt, rowsSwitch);
    }

    /**
     * The size buttons under the try-out field: remember the dragged size, restore it, and go back to the default
     * size, as an application that stores the size per field would; then the size itself.
     */
    private Node sizeButtons(TextAreaInput field, Label sizeLabel) {
        Button remember = new Button("Remember size");
        Button restore = new Button("Restore size");
        Button reset = new Button("Default size");
        restore.setDisable(true);
        remember.setOnAction(event -> {
            rememberedSize = field.getUserSize();
            restore.setDisable(rememberedSize == null);
        });
        restore.setOnAction(event -> field.setUserSize(rememberedSize));
        reset.setOnAction(event -> field.setUserSize(null));
        HBox buttons = new HBox(8, remember, restore, reset, sizeLabel);
        buttons.setAlignment(Pos.CENTER_LEFT);
        return buttons;
    }

    private static String describe(TextAreaInput field) {
        String text = field.getValue();
        int lines = text.isEmpty() ? 0 : text.split("\n", -1).length;
        return "value: " + text.length() + " characters, " + lines + " lines";
    }

    private static String describeSize(Dimension2D size) {
        if (size == null) {
            return "userSize: null (default size)";
        }
        return "userSize: " + Math.round(size.getWidth()) + " \u00D7 " + Math.round(size.getHeight());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** A text area input with the sample description. */
    private static TextAreaInput filled() {
        TextAreaInput field = new TextAreaInput();
        field.setValue(SAMPLE_TEXT);
        return field;
    }
}
