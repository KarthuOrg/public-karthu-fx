package org.karthu.fx.form;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.altKeyPressed;
import static org.karthu.fx.form.FormTestSupport.bindIcon;
import static org.karthu.fx.form.FormTestSupport.chip;
import static org.karthu.fx.form.FormTestSupport.controlKeyPressed;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.shiftKeyPressed;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The text input through its public API, without a window: a single line of text in the form collection's shell, with
 * every state (filled, error, warning, bound, missing variable, read-only, disabled) and the keyboard every input
 * shares.
 */
class TextInputTest {

    private TextInput input;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInput() {
        input = new TextInput();
    }

    // --- Value and texts -----------------------------------------------------------------------------

    @Test
    @DisplayName("a new input is an empty single-line frame with form-text-input, its text field the focus target")
    void newInputIsAnEmptyFrame() {
        FormInput<String> formInput = input;

        assertSame(input, formInput.getNode());
        assertSame(input.getTextField(), formInput.getFocusTarget());
        assertEquals("", input.getValue());
        assertTrue(input.getStyleClass().containsAll(List.of("form-frame", "form-text-input")));
        // JavaFX gives every text control text-input; the field's own class never clashes with it.
        assertFalse(input.getStyleClass().contains("text-input"));
        assertFalse(input.getBinding().isBound());
        assertFalse(input.isReadOnly());
        assertEquals(FieldState.NORMAL, input.getState());
    }

    @Test
    @DisplayName("the value is the text exactly as it stands in the field: set, typed, whitespace kept, empty is none")
    void valueIsTheTextAsEntered() {
        List<String> values = new ArrayList<>();
        input.valueProperty().addListener((observable, oldValue, newValue) -> values.add(newValue));

        input.setValue("Customers 2026");
        assertEquals("Customers 2026", input.getTextField().getText());

        // The user types: the text field's text changes, the value follows as it stands, never trimmed.
        input.getTextField().setText("  Customers  ");
        assertEquals("  Customers  ", input.getValue());

        input.getTextField().clear();
        assertEquals("", input.getValue());
        assertEquals(List.of("Customers 2026", "  Customers  ", ""), values);
    }

    @Test
    @DisplayName("typed keys reach the text field: what the user types is the value")
    void typingSetsTheValue() {
        // The text field gets its skin, and with it its key handling, in a CSS pass.
        layOut(input);

        typeInto(input, "Ada");
        input.getTextField().fireEvent(keyPressed(KeyCode.BACK_SPACE));

        assertEquals("Ad", input.getValue());
    }

    @Test
    @DisplayName("the prompt text is a property, empty by default, that the empty field shows; null shows none")
    void promptTextIsAProperty() {
        assertEquals("", input.getPromptText());
        assertEquals("", input.getTextField().getPromptText());

        input.setPromptText("Customer name");
        assertEquals("Customer name", input.getTextField().getPromptText());

        input.promptTextProperty().set("Kundenname");
        assertEquals("Kundenname", input.getTextField().getPromptText());

        input.setPromptText(null);
        assertEquals("", input.getTextField().getPromptText());
    }

    @Test
    @DisplayName("a paste is inserted unchanged in place of the selection, quotes and whitespace included")
    void pasteIsInsertedUnchanged() {
        input.setValue("before after");
        input.getTextField().selectRange(6, 7);
        input.setClipboard(new RecordingClipboard(" \"quoted\" "));

        input.getTextField().paste();

        assertEquals("before \"quoted\" after", input.getValue());
    }

    // --- State ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("the state shows as :warning or :error on the input, the theme's border colour around text and icons")
    void stateShowsInTheTheme() {
        TextInput warning = filledInput();
        warning.stateProperty().set(FieldState.WARNING);
        input.getBinding().setBindable(true);
        ((FormInput<String>) input).stateProperty().set(FieldState.ERROR);
        Region paused = tokenSwatch("-fx-status-paused");
        Region statusError = tokenSwatch("-fx-status-error");
        layOut(warning, input, paused, statusError);

        assertTrue(hasPseudoClass(warning, "warning"));
        assertFalse(hasPseudoClass(warning, "error"));
        assertTrue(hasPseudoClass(input, "error"));
        assertEquals(fill(paused, 0), fill(warning, 0));
        assertEquals(fill(statusError, 0), fill(input, 0));

        input.setState(FieldState.NORMAL);
        assertFalse(hasPseudoClass(input, "error"));
    }

    @Test
    @DisplayName("in a form row: the label names the text field, an error message colours the input")
    void inAFormRow() {
        FormField row = new FormField("Customer name", input);

        row.getMessages().add(FieldMessage.error("Enter the customer's name."));

        assertEquals("Customer name", input.getTextField().getAccessibleText());
        assertEquals(FieldState.ERROR, input.getState());
        assertTrue(hasPseudoClass(input, "error"));
    }

    // --- Binding -------------------------------------------------------------------------------------

    @Test
    @DisplayName("bound: the chip with the name in place of the text, the value kept hidden, back as it was after")
    void boundKeepsTheValue() {
        input.setValue("Ada Lovelace");
        input.getBinding().setBindable(true);

        input.getBinding().setBoundName("customerName");

        assertTrue(hasPseudoClass(input, "bound"));
        assertTrue(chip(input).isVisible());
        assertEquals("customerName", chip(input).getText());
        assertEquals("Ada Lovelace", input.getValue());
        assertNotNull(input.getTextField().getClip());
        assertFalse(input.getTextField().isEditable());

        input.getBinding().setBoundName(null);

        assertFalse(chip(input).isVisible());
        assertEquals("Ada Lovelace", input.getTextField().getText());
        assertNull(input.getTextField().getClip());
        assertTrue(input.getTextField().isEditable());
        assertTrue(bindIcon(input).isVisible());
    }

    @Test
    @DisplayName("bound, the hidden text is not handed out: select-all selects nothing, copy and cut give nothing")
    void boundTextIsNotHandedOut() {
        RecordingClipboard clipboard = new RecordingClipboard(null);
        input.setClipboard(clipboard);
        input.setValue("Ada Lovelace");
        input.getBinding().setBoundName("customerName");

        input.getTextField().selectAll();
        assertEquals("", input.getTextField().getSelectedText());
        input.getTextField().selectRange(0, 3);
        input.getTextField().copy();
        input.getTextField().cut();

        assertTrue(clipboard.copied().isEmpty());
        assertEquals("Ada Lovelace", input.getValue());
    }

    @Test
    @DisplayName("bound to a missing variable: the chip shows :missing with its tooltip")
    void missingVariable() {
        input.getBinding().setBoundName("deletedVariable");
        input.getBinding().setMissing(true);

        assertTrue(hasPseudoClass(chip(input), "missing"));
        assertEquals("This variable does not exist", chip(input).getTooltip().getText());
    }

    // --- Read-only and disabled ----------------------------------------------------------------------

    @Test
    @DisplayName("read-only: the text is selectable and copyable, not changeable, and the {} icon hides")
    void readOnly() {
        RecordingClipboard clipboard = new RecordingClipboard("pasted");
        input.setClipboard(clipboard);
        input.setValue("Ada Lovelace");
        input.getBinding().setBindable(true);
        input.readOnlyProperty().set(true);
        layOut(input);

        assertTrue(hasPseudoClass(input, "readonly"));
        assertFalse(input.getTextField().isEditable());
        assertFalse(isShown(bindIcon(input)));
        input.getTextField().end();
        typeInto(input, "x");
        input.getTextField().fireEvent(keyPressed(KeyCode.BACK_SPACE));
        input.getTextField().fireEvent(shiftKeyPressed(KeyCode.HOME));
        assertEquals("Ada Lovelace", input.getTextField().getSelectedText());
        input.getTextField().copy();
        input.getTextField().cut();
        input.getTextField().paste();

        assertEquals(List.of("Ada Lovelace"), clipboard.copied());
        assertEquals("Ada Lovelace", input.getValue());
        // The application still sets the value.
        input.setValue("Grace Hopper");
        assertEquals("Grace Hopper", input.getTextField().getText());

        input.setReadOnly(false);
        assertTrue(input.getTextField().isEditable());
        assertTrue(isShown(bindIcon(input)));
    }

    @Test
    @DisplayName("read-only and bound: the chip without its x, the text hidden and kept")
    void readOnlyAndBound() {
        input.setValue("Ada Lovelace");
        input.getBinding().setBoundName("customerName");

        input.setReadOnly(true);

        assertTrue(chip(input).isVisible());
        assertNull(chip(input).getGraphic());
        assertNotNull(input.getTextField().getClip());
        assertEquals("Ada Lovelace", input.getValue());
    }

    @Test
    @DisplayName("disabled: the input is dimmed once, as a disabled button, its text field and icons inside at full opacity")
    void disabledIsDimmedOnce() {
        Button button = new Button("button");
        button.setDisable(true);
        input.getBinding().setBindable(true);
        input.setDisable(true);
        layOut(input, button);

        assertTrue(input.getOpacity() < 1);
        assertEquals(button.getOpacity(), input.getOpacity(), 0.001);
        assertEquals(1, input.getTextField().getOpacity(), 0.001);
        assertEquals(1, bindIcon(input).getOpacity(), 0.001);
        assertTrue(input.getTextField().isDisabled());
    }

    // --- Keyboard ------------------------------------------------------------------------------------

    @Test
    @DisplayName("Ctrl+Space in the field opens the binding list while bindable, and is consumed")
    void ctrlSpaceOpensTheBindingList() {
        input.getBinding().setBindable(true);
        input.getBinding().getCandidates().add(new BindingCandidate("customerName", "String"));

        assertTrue(firesThrough(input, controlKeyPressed(KeyCode.SPACE)).isEmpty());

        assertEquals(1, input.getBindingMenu().getItems().size());
        input.getBindingMenu().getItems().getFirst().fire();
        assertEquals("customerName", input.getBinding().getBoundName());
    }

    @Test
    @DisplayName("Backspace or Delete unbind while bound and are consumed; unbound they are the text field's")
    void backspaceAndDeleteUnbind() {
        input.setValue("Ada Lovelace");
        input.getBinding().setBoundName("customerName");

        assertTrue(firesThrough(input, keyPressed(KeyCode.BACK_SPACE)).isEmpty());
        assertNull(input.getBinding().getBoundName());

        input.getBinding().setBoundName("customerName");
        assertTrue(firesThrough(input, keyPressed(KeyCode.DELETE)).isEmpty());
        assertNull(input.getBinding().getBoundName());

        assertEquals(1, firesThrough(input, keyPressed(KeyCode.BACK_SPACE)).size());
        assertEquals("Ada Lovelace", input.getValue());
    }

    @Test
    @DisplayName("Alt+Down opens nothing: a text input has no choice, so the key passes the field and changes nothing")
    void altDownPassesTheField() {
        input.setValue("Ada Lovelace");
        input.getBinding().setBindable(true);

        assertEquals(1, firesThrough(input, altKeyPressed(KeyCode.DOWN)).size());

        assertEquals("Ada Lovelace", input.getValue());
        assertFalse(input.getBindingMenu().isShowing());
    }

    @Test
    @DisplayName("the text field is in the tab order, the {} icon and the chip's x are not")
    void iconsAreNotFocusTraversable() {
        input.getBinding().setBindable(true);
        input.getBinding().setBoundName("customerName");

        assertTrue(input.getTextField().isFocusTraversable());
        assertFalse(bindIcon(input).isFocusTraversable());
        assertFalse(chip(input).getGraphic().isFocusTraversable());
    }

    // --- Layout --------------------------------------------------------------------------------------

    @Test
    @DisplayName("as high as a plain text field: at rest, with the {} icon, bound and read-only")
    void asHighAsATextField() {
        TextField plain = new TextField("plain");
        TextInput empty = new TextInput();
        TextInput bindable = filledInput();
        bindable.getBinding().setBindable(true);
        TextInput bound = filledInput();
        bound.getBinding().setBoundName("customerName");
        TextInput readOnly = filledInput();
        readOnly.setReadOnly(true);
        layOut(plain, empty, bindable, bound, readOnly);

        assertTrue(plain.getHeight() > 0);
        for (TextInput each : List.of(empty, bindable, bound, readOnly)) {
            assertEquals(plain.getHeight(), each.getHeight(), 0.01);
        }
    }

    @Test
    @DisplayName("the text fills the field left of the {} icon at the right end; bound, the chip sits where the text starts")
    void textAndIconLayout() {
        input.setPrefWidth(300);
        input.setValue("Ada Lovelace");
        input.getBinding().setBindable(true);
        TextInput bound = filledInput();
        bound.setPrefWidth(300);
        bound.getBinding().setBoundName("customerName");
        layOut(input, bound);

        Bounds text = input.getTextField().getBoundsInParent();
        Bounds bind = bindIcon(input).getBoundsInParent();
        assertEquals(input.snappedLeftInset(), text.getMinX(), 0.01);
        assertTrue(text.getMaxX() <= bind.getMinX());
        assertTrue(bind.getMaxX() <= input.getWidth());
        assertTrue(bind.getMaxX() > input.getWidth() - 6);
        assertEquals(bound.getTextField().getPadding().getLeft(), chip(bound).getBoundsInParent().getMinX(), 0.01);
    }

    // --- Helpers -------------------------------------------------------------------------------------

    private static TextInput filledInput() {
        TextInput filled = new TextInput();
        filled.setValue("Ada Lovelace");
        return filled;
    }

    /** Whether the node is in the input and visible, so that it shows while the input does. */
    private static boolean isShown(Node node) {
        return node.getParent() != null && node.isVisible() && node.isManaged();
    }

    /** Types the text into the input's text field as the user would: a key typed event. */
    private static void typeInto(TextInput target, String text) {
        target.getTextField().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, text, "", KeyCode.UNDEFINED,
                false, false, false, false));
    }
}
