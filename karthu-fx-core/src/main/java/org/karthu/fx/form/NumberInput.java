package org.karthu.fx.form;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.karthu.fx.form.number.NumberRules;
import org.karthu.fx.form.number.StepRule;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * A field for a number: a single-line {@link FieldFrame} whose text the theme right-aligns, so that numbers read as
 * numbers, and with its {@linkplain #arrowsVisibleProperty() arrows} a spinner.
 *
 * <p><b>Value.</b> {@link #valueProperty()} is the text exactly as it stands in the field: what the user types stays as
 * typed, also when it is no number, so that the user sees the typo and the application can report it; nothing typed
 * is refused, corrected or reverted, and the empty text is no value. {@link #toNumber()} gives the text as a
 * {@code BigDecimal} when it is a number, else empty; reporting a text that is no number is the application's, as for
 * any other finding.
 *
 * <p><b>Numbers.</b> A number is written as it is stored: whitespace around it is ignored, a dot is the decimal
 * separator, there is no grouping, the digits are ASCII digits, and an exponent is read as {@code BigDecimal} reads it
 * ({@code 1.0E21}, as {@code Double.toString} writes a large number). The number keeps the decimals as written
 * ({@code 1.50} has two). While {@linkplain #wholeNumbersProperty() whole numbers} are on, only an optional sign and
 * digits are a number: {@code 1.0} and {@code 1e3} are none.
 *
 * <p><b>Range and step.</b> {@link #minProperty()}, {@link #maxProperty()} and {@link #stepProperty()} are the field's
 * rules for stepping its value. Typing is never limited by them: a typed value outside the range stays as typed, a
 * number for {@link #toNumber()}, for the application to report.
 *
 * <p><b>Spinner.</b> While {@linkplain #arrowsVisibleProperty() its arrows show} (default off), the field is a spinner:
 * after the number, a divider and two small stacked chevrons, up (Bootstrap {@code chevron-up}) above down
 * ({@code chevron-down}), each with its own hover background, out of the tab order. A press on an arrow gives the
 * field the focus and steps once; held, the arrow repeats after 300 ms, then every 60 ms (as JavaFX's spinner does),
 * until it is released. Up and Down in the field step once per key press; held with Shift, Ctrl or Alt they pass,
 * Alt+Down too, since the field has no choice. The mouse wheel never steps: it passes the field, so that scrolling a
 * form changes no number. A step writes:
 * <ul>
 *   <li>a number inside the range plus or minus the step, stopping at a bound and staying there;</li>
 *   <li>a number outside the range (as typed) the nearer bound, in either direction;</li>
 *   <li>an empty text or one that is no number the start value, 0 forced into the range (the minimum when 0 is below
 *       it, the maximum when above), and the next step goes on from there;</li>
 *   <li>always without exponent, with as many decimals as the number or the step has ({@code 1.50} up by 1 is
 *       {@code 2.50}, the start value with the step {@code 0.25} is {@code 0.00}), the caret after the number.</li>
 * </ul>
 * Where there is no rule to follow, a step changes nothing, not even an empty text: a step of zero or below (a null
 * step counts as the default 1), a minimum above the maximum (an empty range), and a number, bound or step with more
 * than 1000 digits written out ({@code 1e999999999}). Whole numbers decide only how the text is read: a whole-number
 * spinner wants a whole step and whole bounds from its application.
 *
 * <p><b>Keyboard.</b> The field has no choice of its own, so Alt+Down passes it. Up and Down step while the arrows
 * show. Ctrl+Space opens the binding list, Backspace or Delete unbind while bound (see {@link FieldShell}).
 *
 * <p><b>Binding and read-only.</b> As in every input on the shell (see {@link FieldShell}): bound, the chip lies over
 * the hidden number where the text starts, at the left, and the number is kept and shows again after unbinding;
 * read-only, the number can be selected and copied but not changed, and {@code {}} hides. A spinner's arrows are
 * disabled while bound and gone while read-only, with the divider; neither they nor Up and Down step then, nor while
 * the field is disabled, and a held arrow stops repeating as soon as the field can no longer be stepped (also when it
 * leaves its scene).
 *
 * <p><b>Texts</b>, properties with English defaults: the prompt text, empty by default (an application may show the
 * range or the unit there). The arrows have no tooltip, as JavaFX's spinner has none.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The field carries {@code form-number-input} beside the frame's {@code form-frame} (see
 * {@link FieldShell} for the shell's classes and pseudo-classes, and {@link FieldFrame} for {@code form-frame-text},
 * its text field, which the theme right-aligns). The spinner's own controls:
 * <ul>
 *   <li>{@code form-shell-divider}: the shell's divider before the arrows</li>
 *   <li>{@code form-number-arrows}: the box that stacks the two arrows, a {@code VBox}</li>
 *   <li>{@code form-number-up}: the up arrow, a button beside {@code icon-button} and {@code form-frame-icon}</li>
 *   <li>{@code form-number-down}: the down arrow, a button beside {@code icon-button} and {@code form-frame-icon}</li>
 * </ul>
 *
 * <p><b>Pseudo-class</b>, part of the same contract: {@code :spinner} on {@code form-number-input} while its arrows
 * show, that is while {@link #arrowsVisibleProperty()} is true and the field is not read-only.
 *
 * <p>Like every karthu-fx control, the field is used on the JavaFX Application Thread only.
 */
public class NumberInput extends FieldFrame implements FormInput<String> {

    private static final PseudoClass SPINNER_PSEUDO_CLASS = PseudoClass.getPseudoClass("spinner");

    /** A held arrow repeats after this delay, then once per interval: JavaFX's spinner's own defaults. */
    private static final Duration REPEAT_DELAY = Duration.millis(300);
    private static final Duration REPEAT_INTERVAL = Duration.millis(60);

    /** Up and Down without modifiers; a key combination matches only while no modifier is down. */
    private static final KeyCombination STEP_UP_KEY = new KeyCodeCombination(KeyCode.UP);
    private static final KeyCombination STEP_DOWN_KEY = new KeyCodeCombination(KeyCode.DOWN);

    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "");
    private final BooleanProperty wholeNumbers = new SimpleBooleanProperty(this, "wholeNumbers", false);
    private final ObjectProperty<BigDecimal> min = new SimpleObjectProperty<>(this, "min");
    private final ObjectProperty<BigDecimal> max = new SimpleObjectProperty<>(this, "max");
    private final ObjectProperty<BigDecimal> step = new SimpleObjectProperty<>(this, "step", BigDecimal.ONE);

    // The spinner: a divider and the stacked arrows, the field's own controls.
    private final BooleanProperty arrowsVisible = new SimpleBooleanProperty(this, "arrowsVisible", false);
    private final Region divider = createDivider();
    private final Button upArrow = new Button();
    private final Button downArrow = new Button();
    private final VBox arrows = new VBox(upArrow, downArrow);
    private StepRepeat stepRepeat = new TimelineRepeat();

    /**
     * Creates an empty field for decimal numbers, without bounds, with the step 1, no prompt text and no arrows.
     */
    public NumberInput() {
        getStyleClass().add("form-number-input");
        value.bindBidirectional(getTextField().textProperty());
        getTextField().promptTextProperty().bind(promptText);
        buildSpinner();
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
     * The field's value: the text exactly as it stands in the field, the empty text when there is none, whether it is
     * a number or not. Setting it sets the text.
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

    /**
     * Returns the field's text as a number, read as the class description says: empty when the text is empty or no
     * number, or, while {@linkplain #wholeNumbersProperty() whole numbers} are on, no whole number. The range does not
     * matter here. The text is read each time, also while the field is bound, when it is kept hidden.
     *
     * @return the number with the decimals as written, or empty
     */
    public Optional<BigDecimal> toNumber() {
        return NumberRules.parse(value.get(), wholeNumbers.get());
    }

    // --- Properties ----------------------------------------------------------------------------------

    /**
     * Whether only whole numbers are numbers (default false). While true, only an optional sign and digits are a
     * number for {@link #toNumber()}: {@code 1.0}, {@code 1.} and {@code 1e3} are none. Switching it never changes the
     * text.
     *
     * @return the whole numbers property
     */
    public BooleanProperty wholeNumbersProperty() {
        return wholeNumbers;
    }

    /**
     * Returns whether only whole numbers are numbers.
     *
     * @return true while only whole numbers are numbers
     */
    public boolean isWholeNumbers() {
        return wholeNumbers.get();
    }

    /**
     * Sets whether only whole numbers are numbers.
     *
     * @param value true to take only whole numbers as numbers
     */
    public void setWholeNumbers(boolean value) {
        wholeNumbers.set(value);
    }

    /**
     * The text the empty field shows (default empty: none), such as the range or the unit.
     *
     * @return the prompt text property
     */
    public StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Returns the prompt text.
     *
     * @return the prompt text
     */
    public String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the prompt text.
     *
     * @param value the prompt text; null or empty shows none
     */
    public void setPromptText(String value) {
        promptText.set(value);
    }

    // --- Range and step ------------------------------------------------------------------------------

    /**
     * The smallest value of the range for stepping the value (default null: no lower bound). It never limits typing
     * and never changes the value: a typed value below it stays as typed, and the next step lands on it. A minimum
     * above the maximum makes the range empty: the arrows and Up and Down then change nothing.
     *
     * @return the minimum property
     */
    public ObjectProperty<BigDecimal> minProperty() {
        return min;
    }

    /**
     * Returns the minimum.
     *
     * @return the minimum, or null for no lower bound
     */
    public BigDecimal getMin() {
        return min.get();
    }

    /**
     * Sets the minimum.
     *
     * @param value the minimum, or null for no lower bound
     */
    public void setMin(BigDecimal value) {
        min.set(value);
    }

    /**
     * The largest value of the range for stepping the value (default null: no upper bound). It never limits typing
     * and never changes the value: a typed value above it stays as typed, and the next step lands on it. A maximum
     * below the minimum makes the range empty: the arrows and Up and Down then change nothing.
     *
     * @return the maximum property
     */
    public ObjectProperty<BigDecimal> maxProperty() {
        return max;
    }

    /**
     * Returns the maximum.
     *
     * @return the maximum, or null for no upper bound
     */
    public BigDecimal getMax() {
        return max.get();
    }

    /**
     * Sets the maximum.
     *
     * @param value the maximum, or null for no upper bound
     */
    public void setMax(BigDecimal value) {
        max.set(value);
    }

    /**
     * The amount one step of the arrows or of Up and Down adds to or subtracts from the value (default 1), above zero.
     * It never changes the value by itself. Null counts as the default 1; a step of zero or below gives the arrows no
     * way to go, so they and Up and Down then change nothing.
     *
     * @return the step property
     */
    public ObjectProperty<BigDecimal> stepProperty() {
        return step;
    }

    /**
     * Returns the step.
     *
     * @return the step, or null when it counts as the default 1
     */
    public BigDecimal getStep() {
        return step.get();
    }

    /**
     * Sets the step.
     *
     * @param value the step, above zero; null counts as the default 1
     */
    public void setStep(BigDecimal value) {
        step.set(value);
    }

    // --- Spinner arrows ------------------------------------------------------------------------------

    /**
     * Whether the field is a spinner (default false): while true, a divider and the stacked up and down arrows show
     * after the number, Up and Down in the field step, and the field carries {@code :spinner} (see the class
     * description). Read-only hides the arrows whatever this says; switching it never changes the value.
     *
     * @return the arrows visible property
     */
    public BooleanProperty arrowsVisibleProperty() {
        return arrowsVisible;
    }

    /**
     * Returns whether the field is a spinner.
     *
     * @return true while the arrows are on
     */
    public boolean isArrowsVisible() {
        return arrowsVisible.get();
    }

    /**
     * Sets whether the field is a spinner.
     *
     * @param value true to show the arrows and step with Up and Down
     */
    public void setArrowsVisible(boolean value) {
        arrowsVisible.set(value);
    }

    /**
     * Builds the spinner: the divider and the box of arrows as the field's own controls (hidden until the arrows are
     * on), Up and Down, and the field's states the arrows follow besides read-only and the binding, which the shell's
     * hook passes on ({@link #updateContentEditable(boolean)}).
     */
    private void buildSpinner() {
        arrows.getStyleClass().add("form-number-arrows");
        buildArrow(upArrow, "form-number-up", BootstrapIcons.CHEVRON_UP, true);
        buildArrow(downArrow, "form-number-down", BootstrapIcons.CHEVRON_DOWN, false);
        getOwnControls().addAll(divider, arrows);
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleStepKeys);
        arrowsVisible.addListener((observable, wasVisible, visible) -> updateSpinner());
        disabledProperty().addListener((observable, wasDisabled, isDisabled) -> updateSpinner());
        // A held arrow whose field leaves its scene (a page switch) gets no release: its repeat ends here.
        sceneProperty().addListener((observable, oldScene, newScene) -> stepRepeat.stop());
        updateSpinner();
    }

    /**
     * Makes the text field follow read-only and the binding, as the frame does, and the spinner with it.
     *
     * @param editable true while the user may edit the number: neither bound nor read-only
     */
    @Override
    protected void updateContentEditable(boolean editable) {
        super.updateContentEditable(editable);
        // The frame's constructor sets its content, and so calls this hook, before this field's spinner exists;
        // buildSpinner shows the spinner then.
        if (arrows != null) {
            updateSpinner();
        }
    }

    /** Whether a step may change the value now: a spinner the user may change (not disabled, read-only or bound). */
    private boolean mayStep() {
        return arrowsVisible.get() && userMayChangeValue();
    }

    /** Up and Down without modifiers step a spinner; otherwise they pass to the text field. */
    private void handleStepKeys(KeyEvent event) {
        if (!mayStep()) {
            return;
        }
        if (STEP_UP_KEY.match(event)) {
            event.consume();
            step(true);
        } else if (STEP_DOWN_KEY.match(event)) {
            event.consume();
            step(false);
        }
    }

    /**
     * Builds an arrow: an icon-like button whose primary press steps and starts the repeat, whose release stops it,
     * and whose action ({@code fire()}, as a screen reader triggers it) steps once.
     */
    private void buildArrow(Button arrow, String styleClass, BootstrapIcons icon, boolean up) {
        arrow.getStyleClass().add(styleClass);
        styleAsIcon(arrow);
        arrow.setGraphic(FontIcon.of(icon));
        // A button's minimum height is its font's line, even with its graphic only: the arrows would make the field
        // higher than a text field. Each arrow is at least its graphic and padding, and grows to half the box.
        arrow.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        arrow.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(arrow, Priority.ALWAYS);
        arrow.setOnAction(event -> step(up));
        arrow.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> arrowPressed(event, up));
        arrow.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                stepRepeat.stop();
            }
        });
    }

    /**
     * A press on an arrow, as JavaFX's spinner takes it: the field takes the focus, the arrow steps at once and repeats
     * while held. The press is consumed in this filter, before the button's own handling, so the button is never armed
     * and its release does not fire it: a click steps once, not twice.
     */
    private void arrowPressed(MouseEvent event, boolean up) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        event.consume();
        Node target = getFocusTarget();
        if (!target.isFocused()) {
            target.requestFocus();
        }
        if (!mayStep()) {
            return;
        }
        step(up);
        stepRepeat.start(REPEAT_DELAY, REPEAT_INTERVAL, () -> repeatedStep(up));
    }

    /** A repeated step of a held arrow; the repeat ends as soon as the field can no longer be stepped. */
    private void repeatedStep(boolean up) {
        if (!mayStep()) {
            stepRepeat.stop();
            return;
        }
        step(up);
    }

    /** One step up or down by the step rule, if the user may change the value; the caret then ends the number. */
    private void step(boolean up) {
        if (!mayStep()) {
            return;
        }
        StepRule rule = new StepRule(wholeNumbers.get(), min.get(), max.get(), step.get());
        Optional<String> stepped = up ? NumberRules.stepUp(value.get(), rule) : NumberRules.stepDown(value.get(), rule);
        if (stepped.isPresent() && setValueByUser(value, stepped.get())) {
            // Setting the text puts the caret at its start; a right-aligned number is typed on at its end.
            getTextField().end();
        }
    }

    /**
     * Shows the spinner as the field's state wants it: {@code :spinner}, the divider and the arrows while the arrows
     * are on and the field is not read-only (hidden and not managed otherwise, so they take no room), the arrows
     * disabled while bound, as the shell's {@link #updateOwnControl(Node, boolean)} keeps an own control; and ends a
     * held arrow's repeat once the field can no longer be stepped.
     */
    private void updateSpinner() {
        boolean spinner = arrowsVisible.get() && !isReadOnly();
        pseudoClassStateChanged(SPINNER_PSEUDO_CLASS, spinner);
        updateOwnControl(divider, spinner);
        updateOwnControl(arrows, spinner);
        if (!mayStep()) {
            stepRepeat.stop();
        }
    }

    // --- The repeat of a held arrow ------------------------------------------------------------------

    /**
     * Replaces what repeats the step of a held arrow, by default a JavaFX timeline. Package-private: the tests' seam,
     * so that a test fires the repeated steps itself and never waits; it is no part of the API.
     *
     * @param replacement the repeat to use
     */
    void setStepRepeat(StepRepeat replacement) {
        stepRepeat.stop();
        stepRepeat = replacement;
    }

    /**
     * What repeats the step of a held arrow: after a first delay, then steadily, until stopped. Package-private, as the
     * seam is.
     */
    interface StepRepeat {

        /**
         * Starts repeating, in place of any repeat before: the first repeated step after the delay, then one per
         * interval.
         *
         * @param delay    the time from the start to the first repeated step
         * @param interval the time between two repeated steps
         * @param step     the step to repeat
         */
        void start(Duration delay, Duration interval, Runnable step);

        /** Stops repeating; nothing when no repeat runs. */
        void stop();
    }

    /**
     * The repeat on a JavaFX timeline, as JavaFX's spinner repeats: a key frame at the start of every cycle steps, the
     * cycle is the interval, the timeline's delay the first delay. It runs on the JavaFX Application Thread.
     */
    private static final class TimelineRepeat implements StepRepeat {

        private Timeline timeline;

        @Override
        public void start(Duration delay, Duration interval, Runnable step) {
            stop();
            timeline = new Timeline(new KeyFrame(Duration.ZERO, event -> step.run()), new KeyFrame(interval));
            timeline.setDelay(delay);
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
        }

        @Override
        public void stop() {
            if (timeline != null) {
                timeline.stop();
                timeline = null;
            }
        }
    }
}
