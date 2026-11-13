package org.karthu.fx.form;

import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.bindIcon;
import static org.karthu.fx.form.FormTestSupport.chip;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The collection's promise that every input type looks alike, checked across all of them at once in a scene with the
 * theme that is never shown: every single-line type as high as a plain text field in every state (the text area keeps
 * its rows), the {@code {}} icon and the chip in the same places, the same borders and backgrounds per state, the
 * disabled look dimmed once, the room after the last icon only where an icon is, and the icons out of the tab order.
 * Each type's own test checks its own behaviour; this one compares them, as the showcase's overview does by eye.
 */
class FormInputsAlikeTest {

    /** The width of every field compared, so that their right ends line up. */
    private static final double WIDTH = 240;

    /** JavaFX's own pseudo-class of a node that holds the focus, which the theme draws as the focus border. */
    private static final PseudoClass FOCUS_WITHIN = PseudoClass.getPseudoClass("focus-within");

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    /**
     * An input type as the overview shows it: its name, its own style class and a factory of a filled field.
     *
     * @param name       the type's name, for the messages
     * @param styleClass the type's own style class beside {@code form-frame}
     * @param factory    makes a filled field of the type
     */
    private record Type(String name, String styleClass, Supplier<FieldShell> factory) {

        /** A filled field of the type at the common width, put into a state. */
        FieldShell field(Consumer<FieldShell> state) {
            FieldShell field = factory.get();
            field.setPrefWidth(WIDTH);
            state.accept(field);
            return field;
        }
    }

    /** The single-line types, in the order of the overview's rows (the spinner is the number input with arrows). */
    private static List<Type> singleLineTypes() {
        return List.of(
                new Type("TextInput", "form-text-input", FormInputsAlikeTest::textInput),
                new Type("PasswordInput", "form-password-input", FormInputsAlikeTest::passwordInput),
                new Type("NumberInput", "form-number-input", FormInputsAlikeTest::numberInput),
                new Type("NumberInput with arrows", "form-number-input", FormInputsAlikeTest::spinner),
                new Type("ComboInput", "form-combo-input", FormInputsAlikeTest::comboInput),
                new Type("EditableComboInput", "form-editable-combo-input", FormInputsAlikeTest::editableComboInput),
                new Type("PathInput", "form-path-input", FormInputsAlikeTest::pathInput),
                new Type("CronInput", "form-cron-input", FormInputsAlikeTest::cronInput));
    }

    /** Every type: the single-line ones and the text area. */
    private static List<Type> allTypes() {
        List<Type> all = new ArrayList<>(singleLineTypes());
        all.add(new Type("TextAreaInput", "form-text-area-input", FormInputsAlikeTest::textAreaInput));
        return all;
    }

    // --- Style classes and tab order -----------------------------------------------------------------

    @Test
    @DisplayName("every type carries form-frame and its own form- class, never JavaFX's text-input")
    void everyTypeCarriesItsStyleClasses() {
        for (Type type : allTypes()) {
            FieldShell field = type.factory().get();

            assertTrue(field.getStyleClass().contains("form-frame"), type.name());
            assertTrue(field.getStyleClass().contains(type.styleClass()), type.name());
            assertFalse(field.getStyleClass().contains("text-input"), type.name());
        }
    }

    @Test
    @DisplayName("in every type the icons, chevrons, arrows, the eye and the grip are out of the tab order")
    void ownControlsAreOutOfTheTabOrder() {
        for (Type type : allTypes()) {
            FieldShell field = type.field(FormInputsAlikeTest::bindable);
            layOut(field);

            List<Node> controls = new ArrayList<>(field.lookupAll(".form-frame-icon"));
            controls.addAll(field.lookupAll(".form-text-area-grip"));
            assertFalse(controls.isEmpty(), type.name());
            for (Node control : controls) {
                assertFalse(control.isFocusTraversable(), type.name() + ": " + control.getStyleClass());
            }
            assertTrue(field.getFocusTarget().isFocusTraversable(), type.name());
        }
    }

    // --- Size and places -----------------------------------------------------------------------------

    @Test
    @DisplayName("every single-line type is exactly as high as a plain text field, in every state")
    void everySingleLineTypeIsAsHighAsATextField() {
        List<Consumer<FieldShell>> states = List.of(
                field -> { },
                FormInputsAlikeTest::bindable,
                field -> field.setState(FieldState.ERROR),
                field -> field.setState(FieldState.WARNING),
                field -> field.setDisable(true),
                FormInputsAlikeTest::bound,
                field -> {
                    bound(field);
                    field.getBinding().setMissing(true);
                },
                field -> field.setReadOnly(true),
                field -> {
                    bound(field);
                    field.setReadOnly(true);
                });
        for (Type type : singleLineTypes()) {
            for (int i = 0; i < states.size(); i++) {
                TextField plain = new TextField("plain");
                FieldShell field = type.field(states.get(i));
                layOut(plain, field);

                assertEquals(plain.getHeight(), field.getHeight(), 0.01, type.name() + ", state " + i);
            }
        }
    }

    @Test
    @DisplayName("the {} icon sits in the same place in every type: at the right end, as large, on the first line")
    void bindIconInTheSamePlace() {
        FieldShell reference = textInput();
        reference.setPrefWidth(WIDTH);
        bindable(reference);
        layOut(reference);
        Bounds expected = bindIcon(reference).getBoundsInParent();

        for (Type type : allTypes()) {
            FieldShell field = type.field(FormInputsAlikeTest::bindable);
            layOut(field);

            Bounds bind = bindIcon(field).getBoundsInParent();
            assertEquals(expected.getMaxX(), bind.getMaxX(), 0.01, type.name());
            assertEquals(expected.getWidth(), bind.getWidth(), 0.01, type.name());
            assertEquals(expected.getHeight(), bind.getHeight(), 0.01, type.name());
            // The text area's first line has a fractional height: its icon may sit a pixel apart.
            assertEquals(expected.getMinY(), bind.getMinY(), 1.01, type.name());
        }
    }

    @Test
    @DisplayName("the chip sits where the text starts, at the same place in every type")
    void chipInTheSamePlace() {
        FieldShell reference = textInput();
        reference.setPrefWidth(WIDTH);
        bound(reference);
        layOut(reference);
        Bounds expected = chip(reference).getBoundsInParent();
        assertEquals(((TextInput) reference).getTextField().getPadding().getLeft(), expected.getMinX(), 0.01);

        for (Type type : allTypes()) {
            for (boolean readOnly : new boolean[] {false, true}) {
                FieldShell field = type.field(each -> {
                    bound(each);
                    each.setReadOnly(readOnly);
                });
                layOut(field);

                Bounds chip = chip(field).getBoundsInParent();
                String message = type.name() + (readOnly ? ", read-only" : "");
                assertEquals(expected.getMinX(), chip.getMinX(), 0.01, message);
                assertEquals(expected.getHeight(), chip.getHeight(), 0.01, message);
                assertEquals(expected.getMinY(), chip.getMinY(), 1.01, message);
            }
        }
    }

    @Test
    @DisplayName("the room after the last icon is there only after an icon: without one the text reaches the border")
    void noRoomWithoutTrailingNodes() {
        List<Supplier<FieldShell>> withoutIcons = List.of(
                FormInputsAlikeTest::textInput, FormInputsAlikeTest::passwordWithoutEye,
                FormInputsAlikeTest::numberInput);
        for (Supplier<FieldShell> factory : withoutIcons) {
            FieldShell editable = factory.get();
            editable.setPrefWidth(WIDTH);
            FieldShell readOnly = factory.get();
            readOnly.setPrefWidth(WIDTH);
            readOnly.setReadOnly(true);
            FieldShell bindable = factory.get();
            bindable.setPrefWidth(WIDTH);
            bindable(bindable);
            layOut(editable, readOnly, bindable);
            String name = editable.getClass().getSimpleName();

            Node content = editable.getFocusTarget();
            assertEquals(WIDTH, content.getBoundsInParent().getMaxX(), 0.01, name);
            assertEquals(readOnly.getFocusTarget().getBoundsInParent().getMaxX(),
                    content.getBoundsInParent().getMaxX(), 0.01, name);
            // With an icon, the room after it stays.
            assertEquals(WIDTH - bindable.snappedRightInset(), bindIcon(bindable).getBoundsInParent().getMaxX(), 0.01,
                    name);
            assertTrue(bindable.snappedRightInset() > 0, name);
        }
    }

    @Test
    @DisplayName("a right-aligned number ends at the same place editable and read-only, without icons")
    void numberEndsInTheSamePlaceReadOnly() {
        NumberInput editable = numberInput();
        editable.setPrefWidth(WIDTH);
        NumberInput readOnly = numberInput();
        readOnly.setPrefWidth(WIDTH);
        readOnly.setReadOnly(true);
        layOut(editable, readOnly);

        assertEquals(textEnd(readOnly), textEnd(editable), 0.5);
        assertEquals(WIDTH - editable.getTextField().getPadding().getRight(), textEnd(editable), 1.0);
    }

    // --- Looks per state -----------------------------------------------------------------------------

    @Test
    @DisplayName("rest, focus, warning, error and read-only draw the same border and background in every type")
    void statesLookAlike() {
        Region border = tokenSwatch("-fx-control-border");
        Region surface = tokenSwatch("-fx-surface-color");
        Region accent = tokenSwatch("-fx-accent-color");
        Region warning = tokenSwatch("-fx-status-paused");
        Region error = tokenSwatch("-fx-status-error");
        Region readOnly = tokenSwatch("-fx-readonly-bg");
        layOut(border, surface, accent, warning, error, readOnly);

        for (Type type : allTypes()) {
            FieldShell rest = type.field(field -> { });
            FieldShell focused = type.field(field -> field.pseudoClassStateChanged(FOCUS_WITHIN, true));
            FieldShell warned = type.field(field -> field.setState(FieldState.WARNING));
            FieldShell failed = type.field(field -> field.setState(FieldState.ERROR));
            FieldShell locked = type.field(field -> field.setReadOnly(true));
            layOut(rest, focused, warned, failed, locked);

            assertBorderAndBackground(type, "rest", rest, fill(border), fill(surface));
            assertBorderAndBackground(type, "focused", focused, fill(accent), fill(surface));
            assertBorderAndBackground(type, "warning", warned, fill(warning), fill(surface));
            assertBorderAndBackground(type, "error", failed, fill(error), fill(surface));
            assertBorderAndBackground(type, "read-only", locked, fill(border), fill(readOnly));
        }
    }

    @Test
    @DisplayName("disabled, every type is dimmed once, as a button: each text and icon in it, also bound or read-only")
    void disabledIsDimmedOnce() {
        Button button = new Button("button");
        button.setDisable(true);
        layOut(button);
        double dimmed = button.getOpacity();
        assertTrue(dimmed < 1);

        List<Consumer<FieldShell>> states = List.of(
                FormInputsAlikeTest::bindable,
                FormInputsAlikeTest::bound,
                field -> field.setReadOnly(true));
        for (Type type : allTypes()) {
            for (int i = 0; i < states.size(); i++) {
                Consumer<FieldShell> state = states.get(i);
                FieldShell field = type.field(each -> {
                    state.accept(each);
                    each.setDisable(true);
                });
                layOut(field);

                List<Text> texts = new ArrayList<>();
                List<Double> opacities = new ArrayList<>();
                collectTexts(field, 1, texts, opacities);
                assertFalse(texts.isEmpty(), type.name());
                for (int t = 0; t < texts.size(); t++) {
                    assertEquals(dimmed, opacities.get(t), 0.001,
                            type.name() + ", state " + i + ": '" + texts.get(t).getText() + "' in "
                                    + texts.get(t).getParent().getStyleClass());
                }
            }
        }
    }

    // --- Helpers -------------------------------------------------------------------------------------

    private static void assertBorderAndBackground(Type type, String state, FieldShell field, Paint border,
                                                  Paint background) {
        assertEquals(border, fill(field, 0), type.name() + ", " + state + ": border");
        assertEquals(background, fill(field, 1), type.name() + ", " + state + ": background");
    }

    /**
     * Collects every shown text and icon glyph inside a node with its opacity on screen: the product of its own
     * opacity and every parent's, from the node down. Hidden nodes and their children are left out.
     */
    private static void collectTexts(Node node, double opacityAbove, List<Text> texts, List<Double> opacities) {
        if (!node.isVisible()) {
            return;
        }
        double opacity = opacityAbove * node.getOpacity();
        if (node instanceof Text text && text.getText() != null && !text.getText().isEmpty()) {
            texts.add(text);
            opacities.add(opacity);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectTexts(child, opacity, texts, opacities);
            }
        }
    }

    /** The right end of a number input's text, in the field's coordinates. */
    private static double textEnd(NumberInput field) {
        Text text = (Text) field.getTextField().lookup(".text");
        return field.sceneToLocal(text.localToScene(text.getLayoutBounds())).getMaxX();
    }

    private static void bindable(FieldShell field) {
        field.getBinding().getCandidates().setAll(new BindingCandidate("variable"));
        field.getBinding().setBindable(true);
    }

    private static void bound(FieldShell field) {
        bindable(field);
        field.getBinding().setBoundName("variable");
    }

    // --- The types' sample fields, as the overview fills them ----------------------------------------

    private static TextInput textInput() {
        TextInput field = new TextInput();
        field.setValue("Ada Lovelace");
        return field;
    }

    private static PasswordInput passwordInput() {
        PasswordInput field = new PasswordInput();
        field.setValue("s3cret-pass");
        field.setRevealAllowed(true);
        return field;
    }

    private static PasswordInput passwordWithoutEye() {
        PasswordInput field = new PasswordInput();
        field.setValue("s3cret-pass");
        return field;
    }

    private static NumberInput numberInput() {
        NumberInput field = new NumberInput();
        field.setValue("1250");
        return field;
    }

    private static NumberInput spinner() {
        NumberInput field = new NumberInput();
        field.setArrowsVisible(true);
        field.setMin(BigDecimal.ZERO);
        field.setValue("3");
        return field;
    }

    private static ComboInput<String> comboInput() {
        ComboInput<String> field = new ComboInput<>();
        field.getItems().addAll("UTF-8", "UTF-16", "ISO-8859-1");
        field.setValue("UTF-8");
        return field;
    }

    private static EditableComboInput editableComboInput() {
        EditableComboInput field = new EditableComboInput();
        field.getSuggestions().addAll("UTF-8", "UTF-16", "ISO-8859-1");
        field.setValue("UTF-8");
        return field;
    }

    private static TextAreaInput textAreaInput() {
        TextAreaInput field = new TextAreaInput();
        field.setValue("Reads the customers\nand keeps the active ones.");
        return field;
    }

    private static PathInput pathInput() {
        PathInput field = new PathInput();
        field.setValue("data/customers.csv");
        return field;
    }

    private static CronInput cronInput() {
        CronInput field = new CronInput();
        field.setValue("0 0 9 ? * MON-FRI");
        return field;
    }
}
