package org.karthu.fx.form;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.karthu.fx.theme.ThemeManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The form row through its public API, without a window: its label, asterisk, description and messages, and the state
 * it sets on its input.
 */
class FormFieldTest {

    private FormField row;
    private PathInput input;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createRow() {
        row = new FormField();
        input = new PathInput();
        row.setInput(input);
    }

    @Test
    @DisplayName("the label stands above the input, the asterisk after it only while the field is required")
    void labelAndAsterisk() {
        row.setLabel("Input file");

        assertTrue(row.getStyleClass().contains("form-field"));
        assertEquals("Input file", label().getText());
        assertEquals("*", asterisk().getText());
        assertFalse(asterisk().isVisible());
        assertFalse(asterisk().isManaged());

        row.setRequired(true);
        layOut(row);

        assertTrue(asterisk().isVisible());
        assertTrue(asterisk().isManaged());
        Bounds labelBounds = boundsInRow(label());
        Bounds asteriskBounds = boundsInRow(asterisk());
        Bounds inputBounds = boundsInRow(input);
        assertTrue(labelBounds.getMaxY() <= inputBounds.getMinY());
        assertTrue(asteriskBounds.getMaxY() <= inputBounds.getMinY());
        assertTrue(labelBounds.getMaxX() <= asteriskBounds.getMinX());

        row.setRequired(false);
        assertFalse(asterisk().isVisible());
    }

    @Test
    @DisplayName("a row made with a label and an input shows both, not required, without description and messages")
    void rowWithLabelAndInput() {
        PathInput pathInput = new PathInput();

        FormField made = new FormField("Output folder", pathInput);

        assertEquals("Output folder", made.getLabel());
        assertSame(pathInput, made.getInput());
        assertSame(made, pathInput.getParent());
        assertFalse(made.isRequired());
        assertEquals("", made.getDescription());
        assertTrue(made.getMessages().isEmpty());
        assertEquals("", new FormField().getLabel());
        assertNull(new FormField().getInput());
    }

    @Test
    @DisplayName("the description is the label's tooltip, and there is none while the description is empty")
    void descriptionTooltip() {
        assertNull(label().getTooltip());

        row.setDescription("The CSV file the node reads.");
        assertEquals("The CSV file the node reads.", label().getTooltip().getText());

        row.setDescription("The CSV file, with a header line.");
        assertEquals("The CSV file, with a header line.", label().getTooltip().getText());

        row.setDescription("");
        assertNull(label().getTooltip());

        row.setDescription("Again.");
        row.setDescription(null);
        assertNull(label().getTooltip());
    }

    @Test
    @DisplayName("the label is the accessible text of the input's text field, the frame's focus target")
    void labelIsTheInputsAccessibleText() {
        row.setLabel("Input file");

        assertEquals("Input file", input.getTextField().getAccessibleText());

        row.setLabel("Output file");
        assertEquals("Output file", input.getTextField().getAccessibleText());

        row.setInput(null);
        assertNull(input.getTextField().getAccessibleText());
    }

    @Test
    @DisplayName("the label names any input's focus target: the node it names itself, by default the input's node")
    void labelNamesTheInputsFocusTarget() {
        TextField inner = new TextField();
        StubInput named = new StubInput(new HBox(inner), inner);
        StubInput plain = new StubInput(new HBox(), null);
        row.setLabel("Input");

        row.setInput(named);
        assertEquals("Input", inner.getAccessibleText());
        assertNull(named.getNode().getAccessibleText());
        assertSame(input.getTextField(), ((FormInput<String>) input).getFocusTarget());

        row.setInput(plain);
        assertNull(inner.getAccessibleText());
        assertEquals("Input", plain.getNode().getAccessibleText());
        assertSame(plain.getNode(), plain.getFocusTarget());
    }

    @Test
    @DisplayName("an accessible text the application set itself stays: the row neither replaces nor removes it")
    void applicationsOwnAccessibleTextStays() {
        PathInput own = new PathInput();
        own.getTextField().setAccessibleText("CSV file to read");
        row.setLabel("Input file");

        row.setInput(own);
        assertEquals("CSV file to read", own.getTextField().getAccessibleText());

        row.setInput(null);
        assertEquals("CSV file to read", own.getTextField().getAccessibleText());
    }

    @Test
    @DisplayName("label and messages are text the user can select and copy but not change, outside the tab order")
    void labelAndMessagesAreSelectable() {
        row.setLabel("Input file");
        row.getMessages().addAll(
                FieldMessage.error("is no path: C:\\data\\in|put.csv"), FieldMessage.warning("is an absolute path"));
        layOut(row);

        List<SelectableText> texts = new ArrayList<>();
        texts.add(label());
        texts.addAll(messageLabels());
        for (SelectableText text : texts) {
            assertFalse(text.isEditable());
            assertFalse(text.isFocusTraversable());
            assertTrue(text.getStyleClass().contains("form-selectable-text"));
            text.selectAll();
            assertEquals(text.getText(), text.getSelectedText());
            text.selectRange(0, 2);
            assertEquals(text.getText().substring(0, 2), text.getSelectedText());
        }
    }

    @Test
    @DisplayName("every message has its whole text as a wrapping tooltip")
    void messageTooltips() {
        String longText = "is an absolute path, which another host may not have: place the file in the workflow's"
                + " files directory and enter its path relative to the workflow, such as files/data.csv";
        row.getMessages().addAll(
                FieldMessage.error("a value is required"), FieldMessage.warning(longText), FieldMessage.info("UTF-8"));

        List<SelectableText> shown = messageLabels();
        assertEquals("a value is required", shown.get(0).getTooltip().getText());
        assertEquals(longText, shown.get(1).getTooltip().getText());
        assertEquals("UTF-8", shown.get(2).getTooltip().getText());
        for (SelectableText message : shown) {
            assertTrue(message.getTooltip().isWrapText());
        }
    }

    @Test
    @DisplayName("a long message in a narrow row shows every line: nothing is cut off, no scroll bar")
    void longMessageIsNeverCutOff() {
        input.setPrefWidth(160);
        String longText = "is no path: the text contains a character that is not allowed in a file name on this"
                + " platform, so the node cannot read it; choose the file with the folder icon instead";
        row.getMessages().add(FieldMessage.error(longText));
        layOut(row);

        assertShowsAllOfItsText(messageLabels().get(0));
    }

    @Test
    @DisplayName("in a grid that sizes rows without asking for the width, a long message still shows every line")
    void longMessageInAGrid() {
        String longText = "is an absolute path, which another host may not have: place the file in the workflow's"
                + " files directory and enter its path relative to the workflow, such as files/data.csv";
        row.setLabel("Input file");
        row.getMessages().add(FieldMessage.warning(longText));
        GridPane grid = new GridPane();
        ColumnConstraints column = new ColumnConstraints(200, 200, 200);
        grid.getColumnConstraints().add(column);
        grid.add(row, 0, 0);
        Scene scene = new Scene(grid);
        ThemeManager.apply(scene);
        for (int pass = 0; pass < 3; pass++) {
            grid.applyCss();
            grid.autosize();
            grid.layout();
        }

        SelectableText message = messageLabels().get(0);
        assertTrue(message.getWidth() <= 200.01);
        assertShowsAllOfItsText(message);
        assertTrue(boundsInRow(message).getMaxY() <= row.getHeight() + 0.5);
    }

    @Test
    @DisplayName("Tab in a selected message moves the focus on and changes nothing")
    void tabInAMessageMovesTheFocusOn() {
        row.getMessages().add(FieldMessage.error("a value is required"));
        TextField next = new TextField();
        VBox root = new VBox(row, next);
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        root.applyCss();
        root.autosize();
        root.layout();
        SelectableText message = messageLabels().get(0);
        message.requestFocus();

        KeyEvent tab = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, false, false, false, false);
        message.fireEvent(tab);

        assertEquals("a value is required", message.getText());
        assertFalse(message.isFocused());
    }

    @Test
    @DisplayName("the messages stand under the input in their order, one wrapped line each, exactly as given")
    void messagesInOrder() {
        row.getMessages().addAll(
                FieldMessage.error("a value is required"),
                FieldMessage.warning("is an absolute path: the workflow will not run on another computer"),
                FieldMessage.info("read as UTF-8"));
        layOut(row);

        List<SelectableText> shown = messageLabels();
        assertEquals(3, shown.size());
        assertEquals("a value is required", shown.get(0).getText());
        assertEquals("is an absolute path: the workflow will not run on another computer", shown.get(1).getText());
        assertEquals("read as UTF-8", shown.get(2).getText());
        assertEquals(Set.of("error"), severityPseudoClasses(shown.get(0)));
        assertEquals(Set.of("warning"), severityPseudoClasses(shown.get(1)));
        assertEquals(Set.of("info"), severityPseudoClasses(shown.get(2)));
        for (SelectableText message : shown) {
            assertTrue(message.getStyleClass().contains("form-field-message"));
            assertTrue(message.isWrapText());
        }
        assertTrue(messagesBox().isVisible());
        assertTrue(boundsInRow(input).getMaxY() <= boundsInRow(messagesBox()).getMinY());
        assertTrue(boundsInRow(shown.get(0)).getMaxY() <= boundsInRow(shown.get(1)).getMinY());
    }

    @Test
    @DisplayName("the messages follow the list: a change shows at once, an empty list shows nothing")
    void messagesFollowTheList() {
        assertFalse(messagesBox().isVisible());
        assertFalse(messagesBox().isManaged());
        assertTrue(messageLabels().isEmpty());

        row.getMessages().add(FieldMessage.warning("first"));
        row.getMessages().addFirst(FieldMessage.error("second, shown first"));
        assertEquals(List.of("second, shown first", "first"), messageTexts());
        assertTrue(messagesBox().isVisible());
        assertTrue(messagesBox().isManaged());

        row.getMessages().setAll(FieldMessage.info("only this"), null);
        assertEquals(List.of("only this"), messageTexts());

        row.getMessages().clear();
        assertFalse(messagesBox().isVisible());
        assertFalse(messagesBox().isManaged());
        assertTrue(messageLabels().isEmpty());
    }

    @Test
    @DisplayName("the row sets its input's state: error before warning, info does not count")
    void inputStateFromTheMessages() {
        assertEquals(FieldState.NORMAL, input.getState());

        row.getMessages().add(FieldMessage.info("read as UTF-8"));
        assertEquals(FieldState.NORMAL, input.getState());

        row.getMessages().add(FieldMessage.warning("is an absolute path"));
        assertEquals(FieldState.WARNING, input.getState());

        row.getMessages().add(FieldMessage.error("does not exist"));
        assertEquals(FieldState.ERROR, input.getState());

        row.getMessages().remove(FieldMessage.warning("is an absolute path"));
        assertEquals(FieldState.ERROR, input.getState());

        row.getMessages().remove(FieldMessage.error("does not exist"));
        assertEquals(FieldState.NORMAL, input.getState());

        row.getMessages().setAll(FieldMessage.warning("one"), FieldMessage.warning("two"));
        assertEquals(FieldState.WARNING, input.getState());

        row.getMessages().clear();
        assertEquals(FieldState.NORMAL, input.getState());
    }

    @Test
    @DisplayName("an input put into a row with messages takes their state at once")
    void inputTakesTheStateWhenAdded() {
        FormField other = new FormField();
        other.getMessages().add(FieldMessage.error("a value is required"));
        PathInput added = new PathInput();

        other.setInput(added);

        assertEquals(FieldState.ERROR, added.getState());
    }

    @Test
    @DisplayName("replacing the input moves it, its label and its state to the new input; the old one is normal again")
    void replacingTheInput() {
        row.setLabel("Output folder");
        row.getMessages().add(FieldMessage.warning("is an absolute path"));
        PathInput replacement = new PathInput();

        row.setInput(replacement);

        assertEquals(FieldState.WARNING, replacement.getState());
        assertEquals(FieldState.NORMAL, input.getState());
        assertNull(input.getParent());
        assertSame(row, replacement.getParent());
        assertEquals("Output folder", replacement.getTextField().getAccessibleText());
        assertNull(input.getTextField().getAccessibleText());
        List<Node> children = row.getChildrenUnmodifiable();
        int replacementIndex = children.indexOf(replacement);
        assertTrue(children.indexOf(label().getParent()) < replacementIndex);
        assertTrue(replacementIndex < children.indexOf(messagesBox()));

        row.getMessages().setAll(FieldMessage.error("is no path"));
        assertEquals(FieldState.ERROR, replacement.getState());
        assertEquals(FieldState.NORMAL, input.getState());

        row.setInput(null);
        assertEquals(FieldState.NORMAL, replacement.getState());
        assertNull(replacement.getParent());
    }

    @Test
    @DisplayName("a long message wraps at the input's width and never makes the row wider")
    void longMessageWraps() {
        input.setPrefWidth(240);
        row.getMessages().addAll(
                FieldMessage.info("short"),
                FieldMessage.error("is no path: the text contains a character that is not allowed in a file name on"
                        + " this platform, so the node cannot read it; choose the file with the folder icon instead"));
        layOut(row);

        assertEquals(240, row.getWidth(), 0.01);
        List<SelectableText> shown = messageLabels();
        assertTrue(shown.get(1).getWidth() <= 240.01);
        assertTrue(shown.get(1).getHeight() > 2 * shown.get(0).getHeight());
    }

    @Test
    @DisplayName("without a label and not required the row shows no label line: the input stands at the top")
    void noLabelNoLabelLine() {
        layOut(row);
        assertEquals(0, boundsInRow(input).getMinY(), 0.01);

        row.setRequired(true);
        layOut(row);
        assertTrue(boundsInRow(asterisk()).getMaxY() <= boundsInRow(input).getMinY());

        row.setRequired(false);
        row.setLabel("Input file");
        layOut(row);
        assertTrue(boundsInRow(label()).getMaxY() <= boundsInRow(input).getMinY());
    }

    @Test
    @DisplayName("the theme colours the label, the asterisk and each message's severity from its tokens")
    void lookInTheTheme() {
        row.setLabel("Input file");
        row.setRequired(true);
        row.getMessages().addAll(
                FieldMessage.error("does not exist"), FieldMessage.warning("is absolute"), FieldMessage.info("UTF-8"));
        Region secondary = tokenSwatch("-fx-text-secondary-color");
        Region error = tokenSwatch("-fx-status-error");
        Region warning = tokenSwatch("-fx-status-paused");
        layOut(row, secondary, error, warning);

        assertEquals(fill(secondary), textFill(label()));
        assertEquals(fill(error), asterisk().getTextFill());
        List<SelectableText> shown = messageLabels();
        assertEquals(fill(error), textFill(shown.get(0)));
        assertEquals(fill(warning), textFill(shown.get(1)));
        assertEquals(fill(secondary), textFill(shown.get(2)));
        for (SelectableText text : shown) {
            assertEquals(Color.TRANSPARENT, fill(text));
        }
    }

    @Test
    @DisplayName("an input reports no messages of its own by default: the same empty list every time, never changed")
    void inputReportsNoMessagesByDefault() {
        FormInput<String> plain = new StubInput(new HBox(), null);

        ObservableList<FieldMessage> own = plain.getInputMessages();

        assertTrue(own.isEmpty());
        assertSame(own, plain.getInputMessages());
        assertThrows(UnsupportedOperationException.class, () -> own.add(FieldMessage.info("note")));
        assertTrue(((FormInput<String>) input).getInputMessages().isEmpty());
    }

    @Test
    @DisplayName("the input's own messages stand first, then the application's, each in the look of a message")
    void inputsOwnMessagesFirst() {
        ReportingInput reporting = new ReportingInput();
        reporting.getInputMessages().add(FieldMessage.info("At 09:00, Monday to Friday"));
        row.getMessages().add(FieldMessage.warning("runs on the server's time zone"));

        row.setInput(reporting);
        layOut(row);

        assertEquals(List.of("At 09:00, Monday to Friday", "runs on the server's time zone"), messageTexts());
        List<SelectableText> shown = messageLabels();
        assertEquals(Set.of("info"), severityPseudoClasses(shown.get(0)));
        assertEquals(Set.of("warning"), severityPseudoClasses(shown.get(1)));
        assertEquals("At 09:00, Monday to Friday", shown.get(0).getTooltip().getText());
        assertTrue(shown.get(0).getTooltip().isWrapText());
        assertTrue(shown.get(0).getStyleClass().contains("form-field-message"));
        assertTrue(shown.get(0).getStyleClass().contains("form-selectable-text"));
        assertTrue(shown.get(0).isWrapText());
        assertTrue(boundsInRow(reporting).getMaxY() <= boundsInRow(shown.get(0)).getMinY());
        assertTrue(boundsInRow(shown.get(0)).getMaxY() <= boundsInRow(shown.get(1)).getMinY());
    }

    @Test
    @DisplayName("the input's own messages follow its list: a change shows at once, before the application's")
    void inputsOwnMessagesFollowItsList() {
        ReportingInput reporting = new ReportingInput();
        row.setInput(reporting);
        row.getMessages().add(FieldMessage.error("a value is required"));

        reporting.getInputMessages().add(FieldMessage.error("Hour must be 0 to 23, found 24"));
        assertEquals(List.of("Hour must be 0 to 23, found 24", "a value is required"), messageTexts());

        reporting.getInputMessages().setAll(FieldMessage.info("Every 15 minutes"), null);
        assertEquals(List.of("Every 15 minutes", "a value is required"), messageTexts());

        row.getMessages().clear();
        assertEquals(List.of("Every 15 minutes"), messageTexts());
        assertTrue(messagesBox().isVisible());

        reporting.getInputMessages().clear();
        assertTrue(messageLabels().isEmpty());
        assertFalse(messagesBox().isVisible());
        assertFalse(messagesBox().isManaged());
    }

    @Test
    @DisplayName("the row sets the input's state from its own messages and the application's together")
    void inputStateFromBothLists() {
        ReportingInput reporting = new ReportingInput();
        reporting.getInputMessages().add(FieldMessage.info("At 09:00, Monday to Friday"));
        row.setInput(reporting);
        assertEquals(FieldState.NORMAL, reporting.getState());

        row.getMessages().add(FieldMessage.warning("runs on the server's time zone"));
        assertEquals(FieldState.WARNING, reporting.getState());

        reporting.getInputMessages().setAll(FieldMessage.error("Hour must be 0 to 23, found 24"));
        assertEquals(FieldState.ERROR, reporting.getState());

        row.getMessages().clear();
        assertEquals(FieldState.ERROR, reporting.getState());

        reporting.getInputMessages().setAll(FieldMessage.warning("runs only in February"));
        assertEquals(FieldState.WARNING, reporting.getState());

        row.getMessages().add(FieldMessage.error("a value is required"));
        assertEquals(FieldState.ERROR, reporting.getState());

        row.getMessages().clear();
        reporting.getInputMessages().clear();
        assertEquals(FieldState.NORMAL, reporting.getState());
    }

    @Test
    @DisplayName("an input put into a row takes the state of its own messages at once")
    void inputTakesTheStateOfItsOwnMessagesWhenAdded() {
        ReportingInput reporting = new ReportingInput();
        reporting.getInputMessages().add(FieldMessage.error("Unix cron has 5 fields, found 6"));

        row.setInput(reporting);

        assertEquals(FieldState.ERROR, reporting.getState());
        assertEquals(List.of("Unix cron has 5 fields, found 6"), messageTexts());
    }

    @Test
    @DisplayName("replacing the input moves the row to the new input's own messages; the old one is normal again")
    void replacingTheInputFollowsTheNewInputsMessages() {
        ReportingInput first = new ReportingInput();
        first.getInputMessages().add(FieldMessage.error("Hour must be 0 to 23, found 24"));
        ReportingInput second = new ReportingInput();
        second.getInputMessages().add(FieldMessage.info("Every 15 minutes"));
        row.getMessages().add(FieldMessage.warning("runs on the server's time zone"));
        row.setInput(first);

        row.setInput(second);

        assertEquals(List.of("Every 15 minutes", "runs on the server's time zone"), messageTexts());
        assertEquals(FieldState.WARNING, second.getState());
        assertEquals(FieldState.NORMAL, first.getState());

        first.getInputMessages().setAll(FieldMessage.error("Minute must be 0 to 59, found 60"));
        assertEquals(List.of("Every 15 minutes", "runs on the server's time zone"), messageTexts());
        assertEquals(FieldState.NORMAL, first.getState());
        assertEquals(FieldState.WARNING, second.getState());

        second.getInputMessages().setAll(FieldMessage.error("Unknown weekday FRY"));
        assertEquals(List.of("Unknown weekday FRY", "runs on the server's time zone"), messageTexts());
        assertEquals(FieldState.ERROR, second.getState());

        row.setInput(null);
        assertEquals(List.of("runs on the server's time zone"), messageTexts());
        assertEquals(FieldState.NORMAL, second.getState());

        second.getInputMessages().setAll(FieldMessage.info("Every hour"));
        assertEquals(List.of("runs on the server's time zone"), messageTexts());
        assertEquals(FieldState.NORMAL, second.getState());
    }

    @Test
    @DisplayName("the row shows :input-disabled while its input is disabled, also through a parent, and on a new input")
    void inputDisabledPseudoClass() {
        assertFalse(hasPseudoClass(row, "input-disabled"));

        input.setDisable(true);
        assertTrue(hasPseudoClass(row, "input-disabled"));

        input.setDisable(false);
        assertFalse(hasPseudoClass(row, "input-disabled"));

        VBox form = new VBox(row);
        form.setDisable(true);
        assertTrue(hasPseudoClass(row, "input-disabled"));
        form.setDisable(false);
        assertFalse(hasPseudoClass(row, "input-disabled"));

        PathInput disabled = new PathInput();
        disabled.setDisable(true);
        row.setInput(disabled);
        assertTrue(hasPseudoClass(row, "input-disabled"));

        disabled.setDisable(false);
        assertFalse(hasPseudoClass(row, "input-disabled"));
        input.setDisable(true);
        row.setInput(input);
        assertTrue(hasPseudoClass(row, "input-disabled"));
        input.setDisable(false);
        assertFalse(hasPseudoClass(row, "input-disabled"));
        // The input that left no longer counts.
        disabled.setDisable(true);
        assertFalse(hasPseudoClass(row, "input-disabled"));

        input.setDisable(true);
        row.setInput(null);
        assertFalse(hasPseudoClass(row, "input-disabled"));
    }

    @Test
    @DisplayName("while its input is disabled the theme dims label, asterisk and every message as a disabled button")
    void disabledRowIsDimmed() {
        ReportingInput reporting = new ReportingInput();
        reporting.getInputMessages().add(FieldMessage.info("At 09:00, Monday to Friday"));
        row.setInput(reporting);
        row.setLabel("Schedule");
        row.setRequired(true);
        row.getMessages().addAll(FieldMessage.error("a value is required"), FieldMessage.warning("runs at night"));
        Button button = new Button("disabled");
        button.setDisable(true);
        layOut(row, button);
        double disabledOpacity = button.getOpacity();
        assertTrue(disabledOpacity < 1);
        assertEquals(1, label().getOpacity(), 0.001);
        assertEquals(1, asterisk().getOpacity(), 0.001);
        for (SelectableText message : messageLabels()) {
            assertEquals(1, message.getOpacity(), 0.001);
        }

        reporting.setDisable(true);
        row.applyCss();

        assertEquals(disabledOpacity, label().getOpacity(), 0.001);
        assertEquals(disabledOpacity, asterisk().getOpacity(), 0.001);
        List<SelectableText> shown = messageLabels();
        assertEquals(3, shown.size());
        for (SelectableText message : shown) {
            assertEquals(disabledOpacity, message.getOpacity(), 0.001);
        }
        assertEquals(disabledOpacity, reporting.getOpacity(), 0.001);
        assertEquals(1, row.getOpacity(), 0.001);

        reporting.setDisable(false);
        row.applyCss();
        assertEquals(1, label().getOpacity(), 0.001);
        assertEquals(1, asterisk().getOpacity(), 0.001);
        assertEquals(1, messageLabels().get(0).getOpacity(), 0.001);
    }

    @Test
    @DisplayName("a message reported while disabled is dimmed too, and so are the texts of a row in a disabled form")
    void dimmedOnceAlsoThroughAParent() {
        ReportingInput reporting = new ReportingInput();
        row.setInput(reporting);
        row.setLabel("Schedule");
        reporting.setDisable(true);
        Button button = new Button("disabled");
        button.setDisable(true);
        VBox form = new VBox(row);
        layOut(form, button);

        reporting.getInputMessages().add(FieldMessage.error("Hour must be 0 to 23, found 24"));
        form.applyCss();
        assertEquals(button.getOpacity(), messageLabels().get(0).getOpacity(), 0.001);

        reporting.setDisable(false);
        form.setDisable(true);
        form.applyCss();
        assertEquals(button.getOpacity(), label().getOpacity(), 0.001);
        assertEquals(button.getOpacity(), messageLabels().get(0).getOpacity(), 0.001);
        assertEquals(button.getOpacity(), reporting.getOpacity(), 0.001);
        assertEquals(1, form.getOpacity(), 0.001);
        assertEquals(1, row.getOpacity(), 0.001);
    }

    @Test
    @DisplayName("while its input is disabled, label and messages stay text the user can select and copy")
    void disabledRowTextsStaySelectable() {
        ReportingInput reporting = new ReportingInput();
        reporting.getInputMessages().add(FieldMessage.info("At 09:00, Monday to Friday"));
        row.setInput(reporting);
        row.setLabel("Schedule");
        row.getMessages().add(FieldMessage.error("a value is required"));
        reporting.setDisable(true);
        layOut(row);

        assertFalse(row.isDisabled());
        List<SelectableText> texts = new ArrayList<>();
        texts.add(label());
        texts.addAll(messageLabels());
        assertEquals(3, texts.size());
        for (SelectableText text : texts) {
            assertFalse(text.isDisabled());
            assertFalse(text.isEditable());
            text.selectAll();
            assertEquals(text.getText(), text.getSelectedText());
            text.selectRange(0, 2);
            assertEquals(text.getText().substring(0, 2), text.getSelectedText());
        }
    }

    @Test
    @DisplayName("a message is a text and a severity, made directly or by its severity's factory")
    void fieldMessage() {
        assertEquals(new FieldMessage("bad", MessageSeverity.ERROR), FieldMessage.error("bad"));
        assertEquals(new FieldMessage("careful", MessageSeverity.WARNING), FieldMessage.warning("careful"));
        assertEquals(new FieldMessage("note", MessageSeverity.INFO), FieldMessage.info("note"));
        assertThrows(NullPointerException.class, () -> new FieldMessage(null, MessageSeverity.INFO));
        assertThrows(NullPointerException.class, () -> new FieldMessage("text", null));
    }

    // --- Helpers -------------------------------------------------------------------------------------

    private Node messagesBox() {
        return row.lookup(".form-field-messages");
    }

    private List<SelectableText> messageLabels() {
        List<SelectableText> labels = new ArrayList<>();
        for (Node child : ((Parent) messagesBox()).getChildrenUnmodifiable()) {
            if (child.getStyleClass().contains("form-field-message")) {
                labels.add((SelectableText) child);
            }
        }
        return labels;
    }

    private List<String> messageTexts() {
        List<String> texts = new ArrayList<>();
        for (SelectableText message : messageLabels()) {
            texts.add(message.getText());
        }
        return texts;
    }

    /** The severity pseudo-classes a message shows. */
    private static Set<String> severityPseudoClasses(Node message) {
        Set<String> names = new HashSet<>();
        for (PseudoClass pseudoClass : message.getPseudoClassStates()) {
            String name = pseudoClass.getPseudoClassName();
            if (name.equals("error") || name.equals("warning") || name.equals("info")) {
                names.add(name);
            }
        }
        return names;
    }

    private SelectableText label() {
        return (SelectableText) row.lookup(".form-field-label");
    }

    private Label asterisk() {
        return (Label) row.lookup(".form-field-required");
    }

    private Bounds boundsInRow(Node node) {
        return row.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
    }

    /** The colour the text area's skin draws its text in. */
    private static Paint textFill(SelectableText text) {
        return shownText(text).getFill();
    }

    /** The text node the text area's skin shows its text with. */
    private static Text shownText(SelectableText text) {
        for (Node node : text.lookupAll(".text")) {
            if (node instanceof Text shown && text.getText().equals(shown.getText())) {
                return shown;
            }
        }
        throw new AssertionError("no text node shows: " + text.getText());
    }

    /**
     * Asserts that the text area is as high as its skin's wrapped text and wraps at its own width: no line is hidden
     * below its bottom edge and nothing needs a scroll bar.
     */
    private static void assertShowsAllOfItsText(SelectableText text) {
        Text shown = shownText(text);
        double textHeight = shown.getLayoutBounds().getHeight();
        assertTrue(textHeight > 0);
        assertTrue(text.getHeight() + 0.5 >= textHeight,
                "the area is " + text.getHeight() + " high, its text " + textHeight);
        assertTrue(shown.getWrappingWidth() <= text.getWidth() + 0.5,
                "the text wraps at " + shown.getWrappingWidth() + ", the area is " + text.getWidth() + " wide");
        for (Node node : text.lookupAll(".scroll-bar")) {
            assertFalse(node.isVisible(), "a scroll bar shows");
        }
    }

    /**
     * An input that reports messages of its own, as an input that checks its value does: a text input with a list of
     * messages the test changes in place of the input's own checking.
     */
    private static final class ReportingInput extends TextInput {

        private final ObservableList<FieldMessage> own = FXCollections.observableArrayList();

        @Override
        public ObservableList<FieldMessage> getInputMessages() {
            return own;
        }
    }

    /**
     * An input that is no frame, as an application's own: a node holding something that takes the focus, named as
     * the focus target, or no focus target of its own (the default, its node).
     */
    private static final class StubInput implements FormInput<String> {

        private final Node node;
        private final Node focusTarget;
        private final StringProperty value = new SimpleStringProperty("");
        private final FieldBinding binding = new FieldBinding();
        private final BooleanProperty readOnly = new SimpleBooleanProperty(false);
        private final ObjectProperty<FieldState> state = new SimpleObjectProperty<>(FieldState.NORMAL);

        private StubInput(Node node, Node focusTarget) {
            this.node = node;
            this.focusTarget = focusTarget;
        }

        @Override
        public Node getNode() {
            return node;
        }

        @Override
        public Node getFocusTarget() {
            return focusTarget == null ? FormInput.super.getFocusTarget() : focusTarget;
        }

        @Override
        public StringProperty valueProperty() {
            return value;
        }

        @Override
        public FieldBinding getBinding() {
            return binding;
        }

        @Override
        public BooleanProperty readOnlyProperty() {
            return readOnly;
        }

        @Override
        public ObjectProperty<FieldState> stateProperty() {
            return state;
        }
    }
}
