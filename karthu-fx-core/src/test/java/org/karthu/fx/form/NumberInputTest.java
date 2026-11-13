package org.karthu.fx.form;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.shiftKeyPressed;

/**
 * The number input through its public API, without a window.
 */
class NumberInputTest {

    private NumberInput field;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createField() {
        field = new NumberInput();
    }

    @Test
    @DisplayName("style classes: the frame with form-number-input, its text field the content and focus target")
    void styleClasses() {
        assertTrue(field.getStyleClass().containsAll(List.of("form-frame", "form-number-input")));
        assertTrue(field.getTextField().getStyleClass().containsAll(List.of("form-frame-text", "form-shell-content")));
        assertSame(field, field.getNode());
        assertSame(field.getTextField(), field.getFocusTarget());
        assertTrue(field.getIcons().isEmpty());
    }

    @Test
    @DisplayName("the value is the text exactly as typed, both ways, and starts empty")
    void valueIsTheText() {
        assertEquals("", field.getValue());

        field.getTextField().setText(" 12,5 ");
        assertEquals(" 12,5 ", field.getValue());

        field.setValue("-3.75");
        assertEquals("-3.75", field.getTextField().getText());
        assertEquals("-3.75", field.valueProperty().get());
    }

    @Test
    @DisplayName("a text that is no number stays in the field as typed, and toNumber is empty")
    void noNumberStaysAsTyped() {
        field.getTextField().setText("12a");

        assertEquals("12a", field.getValue());
        assertEquals("12a", field.getTextField().getText());
        assertEquals(Optional.empty(), field.toNumber());
    }

    @Test
    @DisplayName("toNumber reads the text: whitespace around it ignored, a dot, an exponent, empty for no number")
    void toNumberReadsTheText() {
        assertEquals(Optional.empty(), field.toNumber());

        field.setValue(" -1.50 ");
        assertEquals(Optional.of(BigDecimal.valueOf(-150, 2)), field.toNumber());

        field.setValue("1.0E21");
        assertEquals(Optional.of(BigDecimal.valueOf(10, -20)), field.toNumber());

        field.setValue("1,5");
        assertEquals(Optional.empty(), field.toNumber());
    }

    @Test
    @DisplayName("whole numbers, off by default: while on, only a sign and digits are a number; the text is kept")
    void wholeNumbers() {
        assertFalse(field.isWholeNumbers());
        field.setValue("2.5");
        assertEquals(Optional.of(BigDecimal.valueOf(25, 1)), field.toNumber());

        field.setWholeNumbers(true);
        assertTrue(field.wholeNumbersProperty().get());
        assertEquals(Optional.empty(), field.toNumber());
        assertEquals("2.5", field.getValue());

        field.setValue("+25");
        assertEquals(Optional.of(BigDecimal.valueOf(25, 0)), field.toNumber());
        field.setValue("1e3");
        assertEquals(Optional.empty(), field.toNumber());
    }

    @Test
    @DisplayName("minimum and maximum start without a bound, the step at 1; each is settable and null clears a bound")
    void boundsAndStep() {
        assertNull(field.getMin());
        assertNull(field.getMax());
        assertEquals(BigDecimal.valueOf(1, 0), field.getStep());

        field.setMin(BigDecimal.valueOf(-5, 0));
        field.setMax(BigDecimal.valueOf(10, 0));
        field.setStep(BigDecimal.valueOf(25, 2));

        assertEquals(BigDecimal.valueOf(-5, 0), field.minProperty().get());
        assertEquals(BigDecimal.valueOf(10, 0), field.maxProperty().get());
        assertEquals(BigDecimal.valueOf(25, 2), field.stepProperty().get());

        field.setMin(null);
        field.setMax(null);
        assertNull(field.getMin());
        assertNull(field.getMax());
    }

    @Test
    @DisplayName("bounds never change the value: a typed value outside the range stays as typed and is still a number")
    void valueOutsideTheRangeStays() {
        field.setValue("250");

        field.setMin(BigDecimal.valueOf(0, 0));
        field.setMax(BigDecimal.valueOf(100, 0));
        field.getTextField().setText("-4");

        assertEquals("-4", field.getValue());
        assertEquals(Optional.of(BigDecimal.valueOf(-4, 0)), field.toNumber());
    }

    @Test
    @DisplayName("the prompt text is empty by default and shows in the empty field")
    void promptText() {
        assertEquals("", field.getPromptText());
        assertEquals("", field.getTextField().getPromptText());

        field.setPromptText("0 to 100");

        assertEquals("0 to 100", field.promptTextProperty().get());
        assertEquals("0 to 100", field.getTextField().getPromptText());
    }

    // --- Look ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("the theme right-aligns the number: its text ends at the field's right end, not at its left")
    void numberIsRightAligned() {
        field.setValue("42");
        field.setPrefWidth(240);
        layOut(field);

        assertEquals(Pos.CENTER_RIGHT, field.getTextField().getAlignment());
        Text text = (Text) field.getTextField().lookup(".text");
        Bounds textBounds = field.sceneToLocal(text.localToScene(text.getLayoutBounds()));
        double textEnd = field.getTextField().getBoundsInParent().getMaxX()
                - field.getTextField().snappedRightInset();
        assertEquals(textEnd, textBounds.getMaxX(), 1.0);
        assertTrue(textBounds.getMinX() > field.getWidth() / 2);
    }

    @Test
    @DisplayName("a number input is exactly as high as a plain text field and a path input, also read-only")
    void asHighAsATextField() {
        TextField plain = new TextField("12");
        PathInput pathInput = new PathInput();
        NumberInput readOnly = new NumberInput();
        readOnly.setValue("12");
        readOnly.setReadOnly(true);
        layOut(plain, field, pathInput, readOnly);

        assertTrue(plain.getHeight() > 0);
        assertEquals(plain.getHeight(), field.getHeight(), 0.01);
        assertEquals(plain.getHeight(), pathInput.getHeight(), 0.01);
        assertEquals(plain.getHeight(), readOnly.getHeight(), 0.01);
    }

    @Test
    @DisplayName("disabled: dimmed as a disabled button, its text field inside it once")
    void disabledLook() {
        Button button = new Button("button");
        button.setDisable(true);
        field.setValue("12");
        field.setDisable(true);
        layOut(button, field);

        assertEquals(button.getOpacity(), field.getOpacity(), 0.001);
        assertTrue(field.getOpacity() < 1);
        assertEquals(1, field.getTextField().getOpacity(), 0.001);
    }

    // --- States --------------------------------------------------------------------------------------

    @Test
    @DisplayName("error and warning show on the number input, which keeps a text that is no number as typed")
    void errorAndWarning() {
        field.setValue("12a");

        field.setState(FieldState.ERROR);
        assertTrue(hasPseudoClass(field, "error"));
        assertSame(field.stateProperty(), ((FormInput<?>) field).stateProperty());

        field.setState(FieldState.WARNING);
        assertTrue(hasPseudoClass(field, "warning"));
        assertFalse(hasPseudoClass(field, "error"));
        assertEquals("12a", field.getValue());
    }

    @Test
    @DisplayName("bound: the chip in place of the number, the value kept hidden and not editable, the same after unbinding")
    void boundKeepsTheValue() {
        FieldBinding binding = field.getBinding();
        binding.setBindable(true);
        field.setValue("1250");

        binding.setBoundName("maxRows");

        assertTrue(hasPseudoClass(field, "bound"));
        assertTrue(chip(field).isVisible());
        assertEquals("maxRows", chip(field).getText());
        assertNotNull(field.getTextField().getClip());
        assertFalse(field.getTextField().isEditable());
        assertEquals("1250", field.getValue());
        assertEquals(Optional.of(BigDecimal.valueOf(1250, 0)), field.toNumber());

        binding.setBoundName(null);

        assertFalse(chip(field).isVisible());
        assertNull(field.getTextField().getClip());
        assertTrue(field.getTextField().isEditable());
        assertEquals("1250", field.getTextField().getText());
    }

    @Test
    @DisplayName("bound, the chip lies at the left where the text starts, as in every input, inside a frame as high as before")
    void boundChipAtTheLeft() {
        TextField plain = new TextField("12");
        field.setValue("1250");
        field.setPrefWidth(240);
        field.getBinding().setBoundName("maxRows");
        layOut(plain, field);

        Bounds chipBounds = chip(field).getBoundsInParent();
        assertEquals(field.snappedLeftInset() + field.getTextField().getPadding().getLeft(), chipBounds.getMinX(), 0.5);
        assertTrue(chipBounds.getMaxX() < field.getWidth() / 2);
        assertEquals(plain.getHeight(), field.getHeight(), 0.01);
    }

    @Test
    @DisplayName("bound to a missing variable: the chip shows :missing")
    void missingVariable() {
        field.getBinding().setBoundName("deletedVariable");
        field.getBinding().setMissing(true);

        assertTrue(hasPseudoClass(chip(field), "missing"));
        assertEquals("This variable does not exist", chip(field).getTooltip().getText());
    }

    @Test
    @DisplayName("read-only: the number can be selected but not changed, {} hides, a bound chip has no x")
    void readOnly() {
        field.getBinding().setBindable(true);
        field.setValue("3.14");

        field.setReadOnly(true);

        assertTrue(hasPseudoClass(field, "readonly"));
        assertFalse(field.getTextField().isEditable());
        assertFalse(bindIcon(field).isVisible());
        field.getTextField().selectAll();
        assertEquals("3.14", field.getTextField().getSelectedText());

        field.getBinding().setBoundName("ratio");
        assertTrue(chip(field).isVisible());
        assertNull(chip(field).getGraphic());

        field.getBinding().setBoundName(null);
        field.setReadOnly(false);
        assertTrue(field.getTextField().isEditable());
        assertTrue(bindIcon(field).isVisible());
        assertEquals("3.14", field.getValue());
    }

    // --- Keyboard ------------------------------------------------------------------------------------

    @Test
    @DisplayName("Alt+Down: the number input has no choice, so the key passes the field and changes nothing")
    void altDownPasses() {
        field.setValue("7");

        assertEquals(1, firesThrough(field, altKeyPressed(KeyCode.DOWN)).size());

        assertEquals("7", field.getValue());
    }

    @Test
    @DisplayName("Ctrl+Space opens the binding list while {} shows; {} is the last node, out of the tab order")
    void ctrlSpaceOpensTheBindingList() {
        field.getBinding().setBindable(true);
        field.getBinding().getCandidates().add(new BindingCandidate("maxRows", "Integer"));

        assertTrue(firesThrough(field, controlKeyPressed(KeyCode.SPACE)).isEmpty());

        assertEquals(1, field.getBindingMenu().getItems().size());
        assertSame(bindIcon(field), field.getChildrenUnmodifiable().getLast());
        assertFalse(bindIcon(field).isFocusTraversable());
    }

    @Test
    @DisplayName("Backspace or Delete unbind while bound and are consumed; unbound they are the text field's")
    void backspaceAndDeleteUnbind() {
        field.setValue("12");
        assertEquals(1, firesThrough(field, keyPressed(KeyCode.BACK_SPACE)).size());

        field.getBinding().setBoundName("maxRows");
        assertTrue(firesThrough(field, keyPressed(KeyCode.BACK_SPACE)).isEmpty());
        assertNull(field.getBinding().getBoundName());

        field.getBinding().setBoundName("maxRows");
        assertTrue(firesThrough(field, keyPressed(KeyCode.DELETE)).isEmpty());
        assertNull(field.getBinding().getBoundName());
        assertEquals("12", field.getValue());
    }

    // --- Spinner arrows ------------------------------------------------------------------------------

    @Test
    @DisplayName("arrows off by default: no :spinner, no divider, no arrows")
    void arrowsOffByDefault() {
        assertFalse(field.isArrowsVisible());
        assertFalse(hasPseudoClass(field, "spinner"));
        assertFalse(shown(divider(field)));
        assertFalse(shown(upArrow(field)));
        assertFalse(shown(downArrow(field)));
    }

    @Test
    @DisplayName("arrows on: :spinner, a divider and the stacked up and down chevrons as icons, before {}, out of the tab order")
    void arrowsOnShowTheSpinner() {
        field.getBinding().setBindable(true);

        field.setArrowsVisible(true);

        assertTrue(field.arrowsVisibleProperty().get());
        assertTrue(hasPseudoClass(field, "spinner"));
        assertTrue(shown(divider(field)));
        assertTrue(shown(upArrow(field)));
        assertTrue(shown(downArrow(field)));
        for (Button arrow : List.of(upArrow(field), downArrow(field))) {
            assertTrue(arrow.getStyleClass().containsAll(List.of("icon-button", "form-frame-icon")));
            assertFalse(arrow.isFocusTraversable());
        }
        assertEquals(BootstrapIcons.CHEVRON_UP, ((FontIcon) upArrow(field).getGraphic()).getIconCode());
        assertEquals(BootstrapIcons.CHEVRON_DOWN, ((FontIcon) downArrow(field).getGraphic()).getIconCode());
        // Stacked: one box holds the up arrow above the down arrow.
        Parent arrows = upArrow(field).getParent();
        assertSame(arrows, downArrow(field).getParent());
        assertTrue(arrows.getStyleClass().contains("form-number-arrows"));
        assertEquals(List.of(upArrow(field), downArrow(field)), arrows.getChildrenUnmodifiable());
        assertFalse(arrows.isFocusTraversable());
        // In the shell: the divider, the arrows, then {} last.
        List<Node> children = field.getChildrenUnmodifiable();
        assertTrue(children.indexOf(divider(field)) < children.indexOf(arrows));
        assertSame(bindIcon(field), children.getLast());

        field.setArrowsVisible(false);
        assertFalse(hasPseudoClass(field, "spinner"));
        assertFalse(shown(divider(field)));
        assertFalse(shown(upArrow(field)));
    }

    @Test
    @DisplayName("firing an arrow steps once by the field's step, within its range, read with its whole numbers")
    void firingAnArrowStepsOnce() {
        field.setArrowsVisible(true);
        field.setValue("1.50");

        upArrow(field).fire();
        assertEquals("2.50", field.getValue());

        downArrow(field).fire();
        downArrow(field).fire();
        assertEquals("0.50", field.getValue());

        field.setStep(BigDecimal.valueOf(25, 2));
        field.setMax(BigDecimal.valueOf(6, 1));
        upArrow(field).fire();
        assertEquals("0.60", field.getValue());

        field.setWholeNumbers(true);
        upArrow(field).fire();
        assertEquals("0.00", field.getValue());
    }

    @Test
    @DisplayName("an empty or invalid field gets the start value, 0 forced into the range, and the next arrow steps from there")
    void startValueThenStep() {
        field.setArrowsVisible(true);
        field.setMin(BigDecimal.valueOf(5, 0));
        field.setMax(BigDecimal.valueOf(10, 0));

        downArrow(field).fire();
        assertEquals("5", field.getValue());
        upArrow(field).fire();
        assertEquals("6", field.getValue());

        field.setValue("12a");
        upArrow(field).fire();
        assertEquals("5", field.getValue());
    }

    @Test
    @DisplayName("a typed value outside the range stays as typed; an arrow then lands on the nearer bound")
    void outsideTheRangeStaysUntilAnArrow() {
        field.setArrowsVisible(true);
        field.setMin(BigDecimal.valueOf(0, 0));
        field.setMax(BigDecimal.valueOf(100, 0));

        field.getTextField().setText("250");
        assertEquals("250", field.getValue());

        downArrow(field).fire();
        assertEquals("100", field.getValue());
    }

    @Test
    @DisplayName("holding an arrow: the press steps once and repeats after a short delay, then steadily, until the release")
    void holdingAnArrowRepeats() {
        ManualRepeat repeat = new ManualRepeat();
        field.setStepRepeat(repeat);
        field.setArrowsVisible(true);
        field.setValue("10");

        assertTrue(fire(upArrow(field), mouse(MouseEvent.MOUSE_PRESSED)).isConsumed());

        assertEquals("11", field.getValue());
        assertTrue(repeat.isRunning());
        assertEquals(Duration.millis(300), repeat.delay);
        assertEquals(Duration.millis(60), repeat.interval);
        repeat.tick();
        repeat.tick();
        assertEquals("13", field.getValue());

        fire(upArrow(field), mouse(MouseEvent.MOUSE_RELEASED));

        assertFalse(repeat.isRunning());
        assertEquals("13", field.getValue());

        fire(downArrow(field), mouse(MouseEvent.MOUSE_PRESSED));
        repeat.tick();
        fire(downArrow(field), mouse(MouseEvent.MOUSE_RELEASED));
        assertEquals("11", field.getValue());
    }

    @Test
    @DisplayName("a click on an arrow steps once: its release and click do not step again, also with the button's skin")
    void aClickStepsOnce() {
        ManualRepeat repeat = new ManualRepeat();
        field.setStepRepeat(repeat);
        field.setArrowsVisible(true);
        field.setValue("10");
        layOut(field);

        fire(upArrow(field), mouse(MouseEvent.MOUSE_PRESSED));
        fire(upArrow(field), mouse(MouseEvent.MOUSE_RELEASED));
        fire(upArrow(field), mouse(MouseEvent.MOUSE_CLICKED));

        assertEquals("11", field.getValue());
        assertFalse(repeat.isRunning());
    }

    @Test
    @DisplayName("a press with another mouse button steps nothing and starts no repeat")
    void otherMouseButtonsStepNothing() {
        ManualRepeat repeat = new ManualRepeat();
        field.setStepRepeat(repeat);
        field.setArrowsVisible(true);
        field.setValue("10");

        fire(upArrow(field), mouse(MouseEvent.MOUSE_PRESSED, MouseButton.SECONDARY));

        assertEquals("10", field.getValue());
        assertFalse(repeat.isRunning());
    }

    @Test
    @DisplayName("an arrow press gives the field the focus, so Up and Down go on from there")
    void arrowPressFocusesTheField() {
        field.setStepRepeat(new ManualRepeat());
        field.setArrowsVisible(true);
        layOut(field);

        fire(downArrow(field), mouse(MouseEvent.MOUSE_PRESSED));

        assertSame(field.getTextField(), field.getScene().getFocusOwner());
    }

    @Test
    @DisplayName("Up and Down in a spinner step and are consumed; with Shift, Alt or Ctrl they pass")
    void upAndDownStepInASpinner() {
        field.setArrowsVisible(true);
        field.setValue("7");

        assertTrue(firesThrough(field, keyPressed(KeyCode.UP)).isEmpty());
        assertEquals("8", field.getValue());
        assertTrue(firesThrough(field, keyPressed(KeyCode.DOWN)).isEmpty());
        assertTrue(firesThrough(field, keyPressed(KeyCode.DOWN)).isEmpty());
        assertEquals("6", field.getValue());

        assertEquals(1, firesThrough(field, altKeyPressed(KeyCode.DOWN)).size());
        assertEquals(1, firesThrough(field, controlKeyPressed(KeyCode.UP)).size());
        assertEquals(1, firesThrough(field, shiftKeyPressed(KeyCode.UP)).size());
        assertEquals("6", field.getValue());
    }

    @Test
    @DisplayName("without arrows, Up and Down step nothing and pass to the text field")
    void upAndDownWithoutArrows() {
        field.setValue("7");

        assertEquals(1, firesThrough(field, keyPressed(KeyCode.UP)).size());
        assertEquals(1, firesThrough(field, keyPressed(KeyCode.DOWN)).size());

        assertEquals("7", field.getValue());
    }

    @Test
    @DisplayName("the mouse wheel never steps: over the number or an arrow it passes, so the form scrolls")
    void mouseWheelNeverSteps() {
        field.setArrowsVisible(true);
        field.setValue("7");
        layOut(field);
        List<Event> scrolled = new ArrayList<>();
        field.getParent().addEventHandler(ScrollEvent.SCROLL, scrolled::add);

        Event.fireEvent(field.getTextField(), wheel(40));
        Event.fireEvent(upArrow(field), wheel(40));
        Event.fireEvent(downArrow(field), wheel(-40));

        assertEquals("7", field.getValue());
        assertEquals(3, scrolled.size());
    }

    @Test
    @DisplayName("bound: the arrows show but are disabled; neither they nor Up and Down change the kept value")
    void boundDisablesTheArrows() {
        ManualRepeat repeat = new ManualRepeat();
        field.setStepRepeat(repeat);
        field.setArrowsVisible(true);
        field.setValue("7");

        field.getBinding().setBoundName("retries");

        assertTrue(shown(upArrow(field)));
        assertTrue(shown(divider(field)));
        assertTrue(upArrow(field).isDisabled());
        assertTrue(downArrow(field).isDisabled());
        assertTrue(hasPseudoClass(field, "spinner"));
        upArrow(field).fire();
        fire(downArrow(field), mouse(MouseEvent.MOUSE_PRESSED));
        assertFalse(repeat.isRunning());
        assertEquals(1, firesThrough(field, keyPressed(KeyCode.UP)).size());
        assertEquals("7", field.getValue());

        field.getBinding().setBoundName(null);
        assertFalse(upArrow(field).isDisabled());
        assertFalse(downArrow(field).isDisabled());
        upArrow(field).fire();
        assertEquals("8", field.getValue());
    }

    @Test
    @DisplayName("a value the application bound is left alone: no arrow, held arrow, Up or Down changes it or fails")
    void boundValuePropertyIsLeftAlone() {
        ManualRepeat repeat = new ManualRepeat();
        field.setStepRepeat(repeat);
        field.setArrowsVisible(true);
        StringProperty source = new SimpleStringProperty("7");
        field.valueProperty().bind(source);

        upArrow(field).fire();
        fire(downArrow(field), mouse(MouseEvent.MOUSE_PRESSED));
        repeat.tick();
        repeat.tick();
        fire(downArrow(field), mouse(MouseEvent.MOUSE_RELEASED));
        firesThrough(field, keyPressed(KeyCode.UP));
        firesThrough(field, keyPressed(KeyCode.DOWN));

        assertEquals("7", field.getValue());
        assertEquals("7", field.getTextField().getText());
        source.set("12");
        assertEquals("12", field.getTextField().getText());
    }

    @Test
    @DisplayName("read-only: the divider and the arrows are gone, no :spinner, and Up and Down change nothing")
    void readOnlyHidesTheArrows() {
        field.setArrowsVisible(true);
        field.setValue("7");

        field.setReadOnly(true);

        assertFalse(hasPseudoClass(field, "spinner"));
        assertFalse(shown(divider(field)));
        assertFalse(shown(upArrow(field)));
        assertFalse(shown(downArrow(field)));
        assertEquals(1, firesThrough(field, keyPressed(KeyCode.UP)).size());
        upArrow(field).fire();
        assertEquals("7", field.getValue());

        field.setReadOnly(false);
        assertTrue(hasPseudoClass(field, "spinner"));
        assertTrue(shown(upArrow(field)));
        assertTrue(shown(divider(field)));
    }

    @Test
    @DisplayName("disabled: no arrow and no key steps")
    void disabledStepsNothing() {
        ManualRepeat repeat = new ManualRepeat();
        field.setStepRepeat(repeat);
        field.setArrowsVisible(true);
        field.setValue("7");

        field.setDisable(true);

        upArrow(field).fire();
        fire(upArrow(field), mouse(MouseEvent.MOUSE_PRESSED));
        firesThrough(field, keyPressed(KeyCode.DOWN));
        assertFalse(repeat.isRunning());
        assertEquals("7", field.getValue());
    }

    @Test
    @DisplayName("a held arrow stops repeating when the field becomes bound, read-only or disabled, or loses its arrows")
    void heldArrowStopsWhenStepsEnd() {
        ManualRepeat repeat = new ManualRepeat();
        field.setStepRepeat(repeat);
        field.setArrowsVisible(true);
        field.setValue("0");
        List<Runnable> endings = List.of(
                () -> field.getBinding().setBoundName("retries"),
                () -> field.setReadOnly(true),
                () -> field.setDisable(true),
                () -> field.setArrowsVisible(false));

        for (Runnable ending : endings) {
            fire(upArrow(field), mouse(MouseEvent.MOUSE_PRESSED));
            String held = field.getValue();

            ending.run();
            repeat.tick();

            assertFalse(repeat.isRunning());
            assertEquals(held, field.getValue());
            field.getBinding().setBoundName(null);
            field.setReadOnly(false);
            field.setDisable(false);
            field.setArrowsVisible(true);
        }
        assertEquals("4", field.getValue());
    }

    @Test
    @DisplayName("a held arrow stops repeating when the field leaves its scene, as on a page switch")
    void heldArrowStopsWhenTheFieldLeavesItsScene() {
        ManualRepeat repeat = new ManualRepeat();
        field.setStepRepeat(repeat);
        field.setArrowsVisible(true);
        layOut(field);
        fire(upArrow(field), mouse(MouseEvent.MOUSE_PRESSED));
        assertTrue(repeat.isRunning());

        ((Pane) field.getParent()).getChildren().remove(field);

        assertFalse(repeat.isRunning());
    }

    @Test
    @DisplayName("a spinner is as high as a text field; the number, the divider, the up arrow above the down arrow, then {}")
    void spinnerLayout() {
        TextField plain = new TextField("12");
        field.setArrowsVisible(true);
        field.getBinding().setBindable(true);
        field.setValue("42");
        field.setPrefWidth(240);
        layOut(plain, field);

        assertEquals(plain.getHeight(), field.getHeight(), 0.01);
        Bounds text = field.getTextField().getBoundsInParent();
        Bounds dividerBounds = divider(field).getBoundsInParent();
        Bounds arrowsBounds = upArrow(field).getParent().getBoundsInParent();
        Bounds bind = bindIcon(field).getBoundsInParent();
        assertTrue(text.getMaxX() <= dividerBounds.getMinX());
        assertTrue(dividerBounds.getMaxX() <= arrowsBounds.getMinX());
        assertTrue(arrowsBounds.getMaxX() <= bind.getMinX());
        // The divider spans the content's height; the arrows share it, up above down, each as wide as the box.
        double contentHeight = field.getHeight() - field.snappedTopInset() - field.snappedBottomInset();
        assertEquals(contentHeight, dividerBounds.getHeight(), 0.01);
        assertEquals(contentHeight, arrowsBounds.getHeight(), 0.01);
        Bounds up = upArrow(field).getBoundsInParent();
        Bounds down = downArrow(field).getBoundsInParent();
        assertEquals(0, up.getMinY(), 0.01);
        assertEquals(up.getMaxY(), down.getMinY(), 0.01);
        assertEquals(arrowsBounds.getHeight(), down.getMaxY(), 0.01);
        assertEquals(arrowsBounds.getWidth(), up.getWidth(), 0.01);
        assertEquals(arrowsBounds.getWidth(), down.getWidth(), 0.01);
    }

    @Test
    @DisplayName("a spinner is as high as a text field also read-only, bound and without {}")
    void spinnerAsHighAsATextFieldInEveryState() {
        TextField plain = new TextField("12");
        NumberInput readOnly = spinner();
        readOnly.setReadOnly(true);
        NumberInput bound = spinner();
        bound.getBinding().setBoundName("retries");
        NumberInput alone = spinner();
        layOut(plain, readOnly, bound, alone);

        assertEquals(plain.getHeight(), readOnly.getHeight(), 0.01);
        assertEquals(plain.getHeight(), bound.getHeight(), 0.01);
        assertEquals(plain.getHeight(), alone.getHeight(), 0.01);
    }

    @Test
    @DisplayName("disabled spinner: dimmed once, its arrows inside it at full opacity; bound, the arrows dimmed as disabled icons")
    void spinnerDisabledLook() {
        Button button = new Button("button");
        button.setDisable(true);
        NumberInput disabled = spinner();
        disabled.setDisable(true);
        NumberInput bound = spinner();
        bound.getBinding().setBoundName("retries");
        layOut(button, disabled, bound);

        assertEquals(button.getOpacity(), disabled.getOpacity(), 0.001);
        assertEquals(1, upArrow(disabled).getOpacity(), 0.001);
        assertEquals(1, downArrow(disabled).getOpacity(), 0.001);
        assertEquals(1, bound.getOpacity(), 0.001);
        assertEquals(button.getOpacity(), upArrow(bound).getOpacity(), 0.001);
    }

    @Test
    @DisplayName("after a step the caret is at the end of the number, without a selection, so typing goes on there")
    void caretAtTheEndAfterAStep() {
        field.setArrowsVisible(true);
        field.setValue("99");
        field.getTextField().selectRange(0, 1);

        firesThrough(field, keyPressed(KeyCode.UP));

        assertEquals("100", field.getValue());
        assertEquals(3, field.getTextField().getCaretPosition());
        assertEquals("", field.getTextField().getSelectedText());

        field.getTextField().positionCaret(0);
        downArrow(field).fire();
        assertEquals(2, field.getTextField().getCaretPosition());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** A filled number input with its arrows, bindable. */
    private static NumberInput spinner() {
        NumberInput spinner = new NumberInput();
        spinner.setArrowsVisible(true);
        spinner.getBinding().setBindable(true);
        spinner.setValue("3");
        spinner.setPrefWidth(240);
        return spinner;
    }

    private static ScrollEvent wheel(double deltaY) {
        return new ScrollEvent(ScrollEvent.SCROLL, 2, 2, 2, 2, false, false, false, false, false, false,
                0, deltaY, 0, 0, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null);
    }

    /**
     * The repeat of a held arrow without a clock: it records what the field asks for, and the test fires the
     * repeated steps itself, so no test waits.
     */
    private static final class ManualRepeat implements NumberInput.StepRepeat {

        private Duration delay;
        private Duration interval;
        private Runnable step;

        @Override
        public void start(Duration initialDelay, Duration repeatInterval, Runnable repeatedStep) {
            delay = initialDelay;
            interval = repeatInterval;
            step = repeatedStep;
        }

        @Override
        public void stop() {
            step = null;
        }

        boolean isRunning() {
            return step != null;
        }

        /** One repeated step, as the clock would fire it; nothing while stopped. */
        void tick() {
            if (step != null) {
                step.run();
            }
        }
    }

    /** Fires the event at the node and returns it as it arrived there, to tell whether it was consumed. */
    private static Event fire(Node node, Event event) {
        List<Event> arrived = new ArrayList<>();
        EventHandler<Event> recorder = arrived::add;
        node.addEventFilter(event.getEventType(), recorder);
        Event.fireEvent(node, event);
        node.removeEventFilter(event.getEventType(), recorder);
        return arrived.getFirst();
    }

    private static MouseEvent mouse(EventType<MouseEvent> type) {
        return mouse(type, MouseButton.PRIMARY);
    }

    private static MouseEvent mouse(EventType<MouseEvent> type, MouseButton button) {
        boolean down = type == MouseEvent.MOUSE_PRESSED;
        return new MouseEvent(type, 2, 2, 2, 2, button, 1, false, false, false, false,
                down && button == MouseButton.PRIMARY, false, down && button == MouseButton.SECONDARY, false, false,
                true, null);
    }

    private static Button upArrow(NumberInput input) {
        return (Button) input.lookup(".form-number-up");
    }

    private static Button downArrow(NumberInput input) {
        return (Button) input.lookup(".form-number-down");
    }

    private static Region divider(NumberInput input) {
        return (Region) input.lookup(".form-shell-divider");
    }

    /** Whether the node shows in its field: it and every parent up to the field visible and managed. */
    private static boolean shown(Node node) {
        Node current = node;
        while (current != null && !(current instanceof NumberInput)) {
            if (!current.isVisible() || !current.isManaged()) {
                return false;
            }
            current = current.getParent();
        }
        return current != null;
    }
}
