package org.karthu.fx.form;

import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.altKeyPressed;
import static org.karthu.fx.form.FormTestSupport.chip;
import static org.karthu.fx.form.FormTestSupport.controlKeyPressed;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.shiftKeyPressed;
import static org.karthu.fx.form.FormTestSupport.shortcutKeyPressed;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The password input through its public API, without a window: the masked secret, the eye that reveals it where the
 * application allows it, the clipboard that a masked secret never reaches, and the input's binding and read-only.
 */
class PasswordInputTest {

    private PasswordInput field;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createField() {
        field = new PasswordInput();
    }

    @Test
    @DisplayName("a new input is a shell around a masked field, its focus target, that shows the value and its prompt")
    void maskedFieldShowsTheValue() {
        assertTrue(field.getStyleClass().containsAll(List.of("form-frame", "form-password-input")));
        assertSame(field, field.getNode());
        PasswordField masked = assertInstanceOf(PasswordField.class, field.getFocusTarget());
        assertSame(masked, field.getChildrenUnmodifiable().getFirst());
        assertEquals("", field.getValue());

        field.setValue("s3cret");
        assertEquals("s3cret", masked.getText());
        // The field gets its skin, and with it its key handling, in a CSS pass.
        layOut(field);
        typeInto(masked, "!");
        assertEquals("s3cret!", field.getValue());

        field.setPromptText("Your password");
        assertEquals("Your password", masked.getPromptText());
    }

    @Test
    @DisplayName("the eye shows only while revealing is allowed (default off): an icon out of the tab order")
    void eyeShowsOnlyWhileAllowed() {
        assertFalse(field.isRevealAllowed());
        assertFalse(eye().isVisible());
        assertFalse(eye().isManaged());
        assertFalse(hasPseudoClass(field, "reveal-allowed"));

        field.setRevealAllowed(true);

        assertTrue(eye().isVisible());
        assertTrue(eye().isManaged());
        assertTrue(hasPseudoClass(field, "reveal-allowed"));
        assertTrue(eye().getStyleClass().containsAll(
                List.of("form-password-reveal", "form-frame-icon", "icon-button")));
        assertFalse(eye().isFocusTraversable());
        assertEquals(BootstrapIcons.EYE, eyeGlyph().getIconCode());
        assertEquals("Show password", eye().getTooltip().getText());
        assertEquals("Show password", eye().getAccessibleText());

        field.setRevealAllowed(false);
        assertFalse(eye().isVisible());
        assertFalse(eye().isManaged());
    }

    @Test
    @DisplayName("the eye reveals: a plain field with the same text, caret and selection, the slashed eye, :revealed")
    void eyeRevealsAndMasks() {
        field.setRevealAllowed(true);
        field.setValue("s3cret");
        TextField masked = (TextField) field.getFocusTarget();
        masked.selectRange(1, 4);

        eye().fire();

        assertTrue(field.isRevealed());
        TextField plain = (TextField) field.getFocusTarget();
        assertFalse(plain instanceof PasswordField);
        assertSame(plain, field.getChildrenUnmodifiable().getFirst());
        assertFalse(field.getChildrenUnmodifiable().contains(masked));
        assertEquals("s3cret", plain.getText());
        assertEquals("3cr", plain.getSelectedText());
        assertEquals(4, plain.getCaretPosition());
        assertEquals(BootstrapIcons.EYE_SLASH, eyeGlyph().getIconCode());
        assertTrue(hasPseudoClass(eye(), "revealed"));
        assertEquals("Hide password", eye().getTooltip().getText());
        assertEquals("Hide password", eye().getAccessibleText());

        // Typing into the plain field changes the one text both fields share.
        plain.appendText("!");
        assertEquals("s3cret!", field.getValue());
        assertEquals("s3cret!", masked.getText());

        eye().fire();

        assertFalse(field.isRevealed());
        assertSame(masked, field.getFocusTarget());
        assertSame(masked, field.getChildrenUnmodifiable().getFirst());
        assertEquals(BootstrapIcons.EYE, eyeGlyph().getIconCode());
        assertFalse(hasPseudoClass(eye(), "revealed"));
        assertEquals("Show password", eye().getTooltip().getText());
    }

    @Test
    @DisplayName("the focus target is the shown field: the eye gives it the focus, revealing and masking")
    void focusMovesWithRevealing() {
        field.setRevealAllowed(true);
        Scene scene = new Scene(new VBox(new TextField(), field));

        eye().fire();
        assertSame(field.getFocusTarget(), scene.getFocusOwner());
        assertFalse(field.getFocusTarget() instanceof PasswordField);

        eye().fire();
        assertSame(field.getFocusTarget(), scene.getFocusOwner());
        assertInstanceOf(PasswordField.class, field.getFocusTarget());

        field.requestFocus();
        assertSame(field.getFocusTarget(), scene.getFocusOwner());
    }

    @Test
    @DisplayName("leaving the revealed field masks it again, the focus stays where it went")
    void leavingMasks() {
        field.setRevealAllowed(true);
        field.setValue("s3cret");
        TextField other = new TextField();
        Scene scene = new Scene(new VBox(other, field));
        eye().fire();

        other.requestFocus();

        assertFalse(field.isRevealed());
        assertInstanceOf(PasswordField.class, field.getFocusTarget());
        assertSame(other, scene.getFocusOwner());
        assertEquals("s3cret", field.getValue());
    }

    @Test
    @DisplayName("masked, copy and cut do nothing: the clipboard gets nothing, the text stays, also by shortcut")
    void maskedTextNeverReachesTheClipboard() {
        RecordingClipboard clipboard = new RecordingClipboard(null);
        field.setClipboard(clipboard);
        field.setValue("s3cret");
        TextField masked = (TextField) field.getFocusTarget();
        // The field gets its skin, and with it its key handling, in a CSS pass.
        layOut(field);

        // What the context menu's Copy and Cut call, and code.
        masked.selectAll();
        masked.copy();
        masked.cut();
        // The shortcuts, through the field's own key handling, which select-all shows to be at work.
        masked.deselect();
        masked.fireEvent(shortcutKeyPressed(KeyCode.A));
        assertEquals(6, masked.getSelection().getLength());
        masked.fireEvent(shortcutKeyPressed(KeyCode.C));
        masked.fireEvent(shortcutKeyPressed(KeyCode.X));
        masked.fireEvent(shiftKeyPressed(KeyCode.DELETE));
        masked.fireEvent(controlKeyPressed(KeyCode.INSERT));

        assertTrue(clipboard.copied().isEmpty());
        assertEquals("s3cret", field.getValue());
    }

    @Test
    @DisplayName("revealed, the text behaves like a text field: copy and cut reach the clipboard, also by shortcut")
    void revealedTextIsCopiedAndCut() {
        RecordingClipboard clipboard = new RecordingClipboard(null);
        field.setClipboard(clipboard);
        field.setRevealAllowed(true);
        field.setValue("s3cret");
        eye().fire();
        TextField plain = (TextField) field.getFocusTarget();
        layOut(field);

        plain.selectRange(0, 2);
        plain.copy();
        plain.fireEvent(shortcutKeyPressed(KeyCode.A));
        plain.fireEvent(shortcutKeyPressed(KeyCode.C));
        plain.selectRange(0, 2);
        plain.cut();

        assertEquals(List.of("s3", "s3cret", "s3"), clipboard.copied());
        assertEquals("cret", field.getValue());
    }

    @Test
    @DisplayName("a paste inserts the clipboard's text, masked or revealed; not while read-only, bound or disabled")
    void pasteMaskedOrRevealed() {
        field.setClipboard(new RecordingClipboard("pass"));
        field.setRevealAllowed(true);
        TextField masked = (TextField) field.getFocusTarget();

        masked.paste();
        assertEquals("pass", field.getValue());
        eye().fire();
        ((TextField) field.getFocusTarget()).paste();
        assertEquals("passpass", field.getValue());

        eye().fire();
        field.setReadOnly(true);
        masked.paste();
        field.setReadOnly(false);
        field.getBinding().setBoundName("secret");
        masked.paste();
        field.getBinding().setBoundName(null);
        field.setDisable(true);
        masked.paste();
        assertEquals("passpass", field.getValue());
    }

    @Test
    @DisplayName("read-only: the eye stays and reveals, the masked text is not copied, the revealed one is, never cut")
    void readOnlyKeepsTheEye() {
        RecordingClipboard clipboard = new RecordingClipboard(null);
        field.setClipboard(clipboard);
        field.setRevealAllowed(true);
        field.setValue("s3cret");
        field.setReadOnly(true);
        TextField masked = (TextField) field.getFocusTarget();

        assertTrue(field.getChildrenUnmodifiable().contains(eye()));
        assertTrue(eye().isVisible());
        assertFalse(eye().isDisabled());
        assertFalse(masked.isEditable());
        masked.selectAll();
        masked.copy();
        assertTrue(clipboard.copied().isEmpty());

        eye().fire();

        assertTrue(field.isRevealed());
        TextField plain = (TextField) field.getFocusTarget();
        assertFalse(plain.isEditable());
        plain.selectRange(2, 6);
        plain.copy();
        plain.cut();
        assertEquals(List.of("cret"), clipboard.copied());
        assertEquals("s3cret", field.getValue());
    }

    @Test
    @DisplayName("bound: a revealed field is masked, the eye disabled; the value is kept, masked after unbinding")
    void boundMasksAndDisablesTheEye() {
        field.setRevealAllowed(true);
        field.setValue("s3cret");
        Scene scene = new Scene(new VBox(new TextField(), field));
        eye().fire();

        field.getBinding().setBoundName("secret");

        assertFalse(field.isRevealed());
        PasswordField masked = assertInstanceOf(PasswordField.class, field.getFocusTarget());
        assertSame(masked, scene.getFocusOwner());
        assertNotNull(masked.getClip());
        assertFalse(masked.isEditable());
        assertTrue(eye().isVisible());
        assertTrue(eye().isDisabled());
        assertTrue(chip(field).isVisible());
        eye().fire();
        assertFalse(field.isRevealed());

        field.getBinding().setBoundName(null);

        assertNull(masked.getClip());
        assertTrue(masked.isEditable());
        assertFalse(eye().isDisabled());
        assertEquals("s3cret", masked.getText());
    }

    @Test
    @DisplayName("a revealed field is masked when revealing is no longer allowed, and when it is disabled")
    void disallowedOrDisabledMasks() {
        field.setRevealAllowed(true);
        eye().fire();
        field.setRevealAllowed(false);
        assertFalse(field.isRevealed());
        assertInstanceOf(PasswordField.class, field.getFocusTarget());

        field.setRevealAllowed(true);
        VBox parent = new VBox(field);
        eye().fire();
        parent.setDisable(true);
        assertFalse(field.isRevealed());
        assertInstanceOf(PasswordField.class, field.getFocusTarget());
        eye().fire();
        assertFalse(field.isRevealed());
    }

    @Test
    @DisplayName("the shared keys at either field: Ctrl+Space opens the list, Backspace unbinds, Alt+Down passes")
    void sharedKeysAtEitherField() {
        field.setRevealAllowed(true);
        field.getBinding().setBindable(true);
        field.getBinding().getCandidates().add(new BindingCandidate("secret"));

        for (int round = 0; round < 2; round++) {
            assertTrue(firesThrough(field, controlKeyPressed(KeyCode.SPACE)).isEmpty());
            assertEquals(1, field.getBindingMenu().getItems().size());
            assertEquals(1, firesThrough(field, altKeyPressed(KeyCode.DOWN)).size());
            field.getBinding().setBoundName("secret");
            assertTrue(firesThrough(field, keyPressed(KeyCode.BACK_SPACE)).isEmpty());
            assertNull(field.getBinding().getBoundName());
            // The second round at the revealed field.
            eye().fire();
        }
    }

    @Test
    @DisplayName("in a form row the label names the shown field, also after revealing")
    void rowLabelNamesTheShownField() {
        field.setRevealAllowed(true);
        new FormField("Password", field);
        assertEquals("Password", field.getFocusTarget().getAccessibleText());

        eye().fire();

        assertEquals("Password", field.getFocusTarget().getAccessibleText());
    }

    @Test
    @DisplayName("the eye's tooltips follow their properties")
    void eyeTooltipsFollowTheirProperties() {
        field.setRevealAllowed(true);
        field.setRevealTooltipText("Passwort anzeigen");
        field.setMaskTooltipText("Passwort verbergen");
        assertEquals("Passwort anzeigen", eye().getTooltip().getText());

        eye().fire();

        assertEquals("Passwort verbergen", eye().getTooltip().getText());
        assertEquals("Passwort verbergen", eye().getAccessibleText());
    }

    @Test
    @DisplayName("in the theme the input is as high as a text field, masked or revealed, its fields without frame")
    void asHighAsATextFieldWithoutInnerFrame() {
        TextField plainTextField = new TextField("plain");
        PasswordInput revealed = new PasswordInput();
        revealed.setRevealAllowed(true);
        field.setRevealAllowed(true);
        field.setPrefWidth(240);
        revealed.setPrefWidth(240);
        layOut(plainTextField, field, revealed);
        eyeOf(revealed).fire();
        layOut(revealed);

        assertEquals(plainTextField.getHeight(), field.getHeight(), 0.01);
        assertEquals(plainTextField.getHeight(), revealed.getHeight(), 0.01);
        for (PasswordInput each : List.of(field, revealed)) {
            TextField shown = (TextField) each.getFocusTarget();
            assertTrue(shown.getStyleClass().contains("form-frame-text"));
            assertEquals(Color.TRANSPARENT, fill(shown));
        }
        Bounds eye = eye().getBoundsInParent();
        assertTrue(eye.getMaxX() <= field.getWidth() - field.snappedRightInset());
        assertTrue(field.getFocusTarget().getBoundsInParent().getMaxX() <= eye.getMinX());
    }

    @Test
    @DisplayName("the theme shows the revealed eye with the hover background, the icon in the text colour")
    void revealedEyeInTheTheme() {
        field.setRevealAllowed(true);
        eye().fire();
        Region hover = tokenSwatch("-fx-hover-color");
        Region text = tokenSwatch("-fx-text-color");
        layOut(field, hover, text);

        assertEquals(fill(hover), fill(eye()));
        assertEquals(fill(text), eyeGlyph().getIconColor());
    }

    @Test
    @DisplayName("read-only: the eye keeps the room after it inside the border; without it the text reaches it")
    void readOnlyEyeStaysInsideTheBorder() {
        PasswordInput editable = new PasswordInput();
        editable.setRevealAllowed(true);
        PasswordInput readOnlyWithoutEye = new PasswordInput();
        readOnlyWithoutEye.setReadOnly(true);
        field.setRevealAllowed(true);
        field.setReadOnly(true);
        layOut(editable, field, readOnlyWithoutEye);

        assertEquals(editable.snappedRightInset(), field.snappedRightInset(), 0.01);
        assertTrue(field.snappedRightInset() > 0);
        assertEquals(readOnlyWithoutEye.getWidth(),
                readOnlyWithoutEye.getFocusTarget().getBoundsInParent().getMaxX(), 0.01);
    }

    // --- Helpers -------------------------------------------------------------------------------------

    private static Button eyeOf(PasswordInput input) {
        return (Button) input.lookup(".form-password-reveal");
    }

    /** The eye, the field's own control that reveals and masks the text. */
    private Button eye() {
        return eyeOf(field);
    }

    private FontIcon eyeGlyph() {
        return (FontIcon) eye().getGraphic();
    }

    /** Types a text at the end of a text field, as the user would, through a key event. */
    private static void typeInto(TextField target, String text) {
        target.end();
        target.fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, text, "", KeyCode.UNDEFINED, false, false, false, false));
    }
}
