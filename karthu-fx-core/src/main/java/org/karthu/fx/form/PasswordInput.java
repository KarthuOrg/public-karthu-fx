package org.karthu.fx.form;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A field for a secret, such as a password: the text is masked while the user types it, and, where the application
 * allows it, an eye inside the field reveals it to check it.
 *
 * <p><b>Value.</b> {@link #valueProperty()} is the text exactly as entered, the empty text being no value. The field
 * holds two text fields of one text, of which one is shown at a time: a masked one (a {@link PasswordField}), shown
 * by default, and a plain one, shown while the text is {@linkplain #revealedProperty() revealed}. Typing, the caret
 * and the selection carry over from one to the other.
 *
 * <p><b>Revealing.</b> Only while the application {@linkplain #revealAllowedProperty() allows it} (default false, so a
 * secret stays masked without any work) does the eye (Bootstrap {@code EYE}) show among the field's own controls. It
 * reveals the text and gives the field the focus; while revealed it shows the slashed eye (Bootstrap
 * {@code EYE_SLASH}) on the hover background, and masks the text again. Its tooltip and accessible text are <i>Show
 * password</i> while masked and <i>Hide password</i> while revealed, both properties. The text is masked again
 * whenever the user leaves the field (another node takes the focus, or the window loses it), when revealing is no
 * longer allowed, when the field is bound and when it is disabled. The eye is not focus traversable, so Tab passes
 * the field in one step; the field has no choice, so Alt+Down passes it.
 *
 * <p><b>Clipboard.</b> A masked text never reaches the clipboard: copy and cut do nothing, by the shortcut, by the
 * context menu (whose <i>Copy</i> and <i>Cut</i> call the field's {@code copy()} and {@code cut()}) and when called
 * by code; JavaFX's text fields start no drag of their text either. Revealed, the text behaves like a text field's:
 * copy puts the selection on the clipboard, cut also removes it. A paste inserts the clipboard's text, masked or
 * revealed, but not while the user may not change the value ({@link #userMayChangeValue()}: disabled, read-only or
 * bound).
 *
 * <p><b>Focus.</b> The {@linkplain #getFocusTarget() focus target} is the field shown, so it changes with revealing,
 * and the focus moves with it. A {@link FormField} names the field shown as the input's accessible label: both fields
 * share one accessible text.
 *
 * <p><b>Binding.</b> The field can take its value from a variable through its {@linkplain #getBinding() binding},
 * shown by its shell (see {@link FieldShell}): the {@code {}} icon after the eye, the chip in place of the text while
 * bound. While bound the text is masked, hidden and kept, and the eye is disabled; after unbinding the text shows
 * again, masked.
 *
 * <p><b>Read-only.</b> A {@linkplain #readOnlyProperty() read-only} field (see {@link FieldShell}) keeps its eye,
 * since revealing changes no value: the masked text cannot be copied, the revealed one can be selected and copied but
 * not changed.
 *
 * <p><b>Texts</b>, properties with English defaults: the prompt text (default null: none), and the eye's tooltips
 * <i>Show password</i> and <i>Hide password</i>.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The field carries {@code form-password-input} beside the shell's {@code form-frame} (see
 * {@link FieldShell} for the shell's classes), and:
 * <ul>
 *   <li>{@code form-password-masked}: the masked field, a {@link PasswordField} (keeps {@code text-field} and
 *       {@code password-field})</li>
 *   <li>{@code form-password-plain}: the plain field, a {@link TextField} (keeps {@code text-field})</li>
 *   <li>{@code form-frame-text}: both fields, the inner text field of a frame (see {@link FieldFrame}), whose rules
 *       draw them without border and background; the one shown also carries the shell's {@code form-shell-content}</li>
 *   <li>{@code form-password-reveal}: the eye, a button beside the shell's {@code form-frame-icon} and
 *       {@code icon-button}</li>
 * </ul>
 *
 * <p><b>Pseudo-classes</b>, part of the same contract:
 * <ul>
 *   <li>{@code :reveal-allowed}: {@code form-password-input}, while revealing is allowed and the eye shows</li>
 *   <li>{@code :revealed}: {@code form-password-reveal}, while the text is revealed</li>
 * </ul>
 *
 * <p>Like every karthu-fx control, the field is used on the JavaFX Application Thread only.
 */
public class PasswordInput extends FieldShell implements FormInput<String> {

    private static final PseudoClass REVEAL_ALLOWED_PSEUDO_CLASS = PseudoClass.getPseudoClass("reveal-allowed");
    private static final PseudoClass REVEALED_PSEUDO_CLASS = PseudoClass.getPseudoClass("revealed");

    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText");
    private final BooleanProperty revealAllowed = new SimpleBooleanProperty(this, "revealAllowed", false);
    private final ReadOnlyBooleanWrapper revealed = new ReadOnlyBooleanWrapper(this, "revealed", false);
    private final StringProperty revealTooltipText =
            new SimpleStringProperty(this, "revealTooltipText", "Show password");
    private final StringProperty maskTooltipText = new SimpleStringProperty(this, "maskTooltipText", "Hide password");

    /** The masked field, shown unless the text is revealed. */
    private final PasswordField masked = new MaskedField();
    /** The plain field, shown while the text is revealed. */
    private final TextField plain = new PlainField();

    private final Button eye = new Button();
    /** The eye's glyph: open while masked, slashed while revealed. */
    private final FontIcon eyeGlyph = FontIcon.of(BootstrapIcons.EYE);

    /**
     * Creates an empty, masked field on which revealing is not allowed, with English texts.
     */
    public PasswordInput() {
        getStyleClass().add("form-password-input");
        masked.getStyleClass().addAll("form-frame-text", "form-password-masked");
        plain.getStyleClass().addAll("form-frame-text", "form-password-plain");

        // One text in two fields: the value is the masked field's text, and the plain field shares it.
        value.bindBidirectional(masked.textProperty());
        plain.textProperty().bindBidirectional(masked.textProperty());
        masked.promptTextProperty().bind(promptText);
        plain.promptTextProperty().bind(promptText);
        // A row's label names the focus target, the field shown at that moment: both fields carry the same name.
        plain.accessibleTextProperty().bindBidirectional(masked.accessibleTextProperty());

        buildEye();
        revealAllowed.addListener((observable, wasAllowed, allowed) -> updateRevealAllowed());
        revealed.addListener((observable, wasRevealed, isRevealed) -> updateEyeLook());
        updateRevealAllowed();

        // Leaving the revealed field masks it: another node of the scene takes the focus (the scene's focus owner),
        // or the window loses the focus (the field's own focused flag, which needs a focused window).
        whenFocusLeaves(plain, this::mask);
        plain.focusedProperty().addListener((observable, wasFocused, focused) -> {
            if (!focused) {
                mask();
            }
        });
        // Bound or disabled, the text is masked again; the eye's disabling follows in updateContentEditable.
        getBinding().boundNameProperty().addListener((observable, oldName, newName) -> {
            if (getBinding().isBound()) {
                mask();
            }
        });
        disabledProperty().addListener((observable, wasDisabled, isDisabled) -> {
            if (isDisabled) {
                mask();
            }
        });
        // Last: the shell calls the content hooks, which use the fields and the eye, as soon as the content is set.
        setContent(masked);
    }

    // --- FormInput -----------------------------------------------------------------------------------

    /**
     * Returns the field itself, to place it in a layout.
     *
     * @return this field
     */
    @Override
    public Node getNode() {
        return this;
    }

    /**
     * The field's value: the text exactly as entered, the empty text when there is none. Setting it sets the text,
     * masked or revealed as the field is.
     *
     * @return the value property
     */
    @Override
    public StringProperty valueProperty() {
        return value;
    }

    /**
     * Returns the field's text.
     *
     * @return the text in the field
     */
    public String getValue() {
        return value.get();
    }

    /**
     * Sets the field's text, as it is.
     *
     * @param text the new text
     */
    public void setValue(String text) {
        value.set(text);
    }

    // --- Properties ----------------------------------------------------------------------------------

    /**
     * The text the empty field shows, masked or revealed (default null: none).
     *
     * @return the prompt text property
     */
    public StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Returns the prompt text.
     *
     * @return the prompt text, or null for none
     */
    public String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the prompt text.
     *
     * @param text the prompt text, or null for none
     */
    public void setPromptText(String text) {
        promptText.set(text);
    }

    /**
     * Whether the user may reveal the text (default false): while true, the eye shows in the field (the pseudo-class
     * {@code :reveal-allowed}) and reveals and masks the text. Set to false while revealed, the text is masked again.
     * An application allows it per field, for the secrets it lets the user check.
     *
     * @return the reveal-allowed property
     */
    public BooleanProperty revealAllowedProperty() {
        return revealAllowed;
    }

    /**
     * Returns whether the user may reveal the text.
     *
     * @return true while the eye shows
     */
    public boolean isRevealAllowed() {
        return revealAllowed.get();
    }

    /**
     * Sets whether the user may reveal the text.
     *
     * @param allowed true to show the eye
     */
    public void setRevealAllowed(boolean allowed) {
        revealAllowed.set(allowed);
    }

    /**
     * Whether the text is revealed now (initially false): the user revealed it with the eye and has not left the
     * field since. Only the user reveals it; the field masks it again as described in the class description.
     *
     * @return the read-only revealed property
     */
    public ReadOnlyBooleanProperty revealedProperty() {
        return revealed.getReadOnlyProperty();
    }

    /**
     * Returns whether the text is revealed now.
     *
     * @return true while the plain field shows
     */
    public boolean isRevealed() {
        return revealed.get();
    }

    /**
     * The eye's tooltip and accessible text while the text is masked (default <i>Show password</i>).
     *
     * @return the reveal tooltip text property
     */
    public StringProperty revealTooltipTextProperty() {
        return revealTooltipText;
    }

    /**
     * Returns the eye's tooltip while the text is masked.
     *
     * @return the reveal tooltip text
     */
    public String getRevealTooltipText() {
        return revealTooltipText.get();
    }

    /**
     * Sets the eye's tooltip while the text is masked.
     *
     * @param text the reveal tooltip text
     */
    public void setRevealTooltipText(String text) {
        revealTooltipText.set(text);
    }

    /**
     * The eye's tooltip and accessible text while the text is revealed (default <i>Hide password</i>).
     *
     * @return the mask tooltip text property
     */
    public StringProperty maskTooltipTextProperty() {
        return maskTooltipText;
    }

    /**
     * Returns the eye's tooltip while the text is revealed.
     *
     * @return the mask tooltip text
     */
    public String getMaskTooltipText() {
        return maskTooltipText.get();
    }

    /**
     * Sets the eye's tooltip while the text is revealed.
     *
     * @param text the mask tooltip text
     */
    public void setMaskTooltipText(String text) {
        maskTooltipText.set(text);
    }

    // --- Revealing -----------------------------------------------------------------------------------

    /** Builds the eye, the field's own control: an icon whose tooltip and accessible text follow the state. */
    private void buildEye() {
        eye.getStyleClass().add("form-password-reveal");
        styleAsIcon(eye);
        eye.setGraphic(eyeGlyph);
        StringBinding eyeText = Bindings.when(revealed).then(maskTooltipText).otherwise(revealTooltipText);
        Tooltip eyeTooltip = new Tooltip();
        eyeTooltip.textProperty().bind(eyeText);
        eye.setTooltip(eyeTooltip);
        eye.accessibleTextProperty().bind(eyeText);
        eye.setOnAction(event -> toggleRevealed());
        getOwnControls().add(eye);
    }

    /**
     * The eye: reveals the text, or masks it again, and gives the field shown the focus. Revealing only while allowed,
     * neither bound nor disabled (the eye is disabled then; this guards a fire by code too).
     */
    private void toggleRevealed() {
        if (revealed.get()) {
            revealed.set(false);
            show(masked, true);
        } else if (revealAllowed.get() && !getBinding().isBound() && !isDisabled()) {
            show(plain, true);
            // Set last, so that the focus moving into the plain field is not taken for leaving it.
            revealed.set(true);
        }
    }

    /** Masks the text again, if it is revealed; the masked field takes the focus if the plain one had it. */
    private void mask() {
        if (revealed.get()) {
            revealed.set(false);
            show(masked, false);
        }
    }

    /**
     * Shows one of the two fields in place of the other, as the shell's content: the caret and the selection carry
     * over, and so does the focus (the shell moves it with the content), which the new field also takes when asked to.
     */
    private void show(TextField next, boolean takeFocus) {
        TextField shown = (TextField) getContent();
        if (shown == next) {
            return;
        }
        int anchor = shown.getAnchor();
        int caret = shown.getCaretPosition();
        setContent(next);
        if (takeFocus) {
            next.requestFocus();
        }
        // After the focus, which may select the whole text when it arrives.
        next.selectRange(anchor, caret);
    }

    /** Shows the eye while revealing is allowed, and masks the text when it no longer is. */
    private void updateRevealAllowed() {
        boolean allowed = revealAllowed.get();
        if (!allowed) {
            mask();
        }
        pseudoClassStateChanged(REVEAL_ALLOWED_PSEUDO_CLASS, allowed);
        updateOwnControl(eye, allowed);
    }

    /** The eye's look: open while masked, slashed with {@code :revealed} while revealed. */
    private void updateEyeLook() {
        boolean shown = revealed.get();
        eyeGlyph.setIconCode(shown ? BootstrapIcons.EYE_SLASH : BootstrapIcons.EYE);
        eye.pseudoClassStateChanged(REVEALED_PSEUDO_CLASS, shown);
    }

    // --- Hooks of the shell --------------------------------------------------------------------------

    /**
     * Makes both fields editable or not, and disables the eye while bound. Read-only keeps the eye enabled: revealing
     * changes no value.
     *
     * @param editable true while the user may edit the text: neither bound nor read-only
     */
    @Override
    protected void updateContentEditable(boolean editable) {
        masked.setEditable(editable);
        plain.setEditable(editable);
        updateOwnControl(eye, revealAllowed.get());
    }

    // --- Clipboard -----------------------------------------------------------------------------------

    /**
     * The masked field: copy and cut do nothing, whoever calls them (the shortcut, the context menu, code), so a
     * masked secret never reaches the clipboard; a paste is the shell's guarded paste.
     */
    private final class MaskedField extends PasswordField {

        @Override
        public void paste() {
            guardedPaste(this);
        }

        @Override
        public void copy() {
            // Nothing: a masked secret never reaches the clipboard.
        }

        @Override
        public void cut() {
            // Nothing: a masked secret never reaches the clipboard, and the selection stays.
        }
    }

    /**
     * The plain field, shown while revealed: a text field that copies, cuts and pastes through the shell's guarded
     * clipboard; a cut or a paste changes nothing while the user may not change the value. It is never shown while
     * bound.
     */
    private final class PlainField extends TextField {

        @Override
        public void paste() {
            guardedPaste(this);
        }

        @Override
        public void copy() {
            guardedCopy(this);
        }

        @Override
        public void cut() {
            guardedCut(this);
        }
    }
}
