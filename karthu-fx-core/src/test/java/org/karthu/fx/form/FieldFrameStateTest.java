package org.karthu.fx.form;

import javafx.css.PseudoClass;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * An input's state set directly, as on an input that stands alone, and how the frame shows it.
 */
class FieldFrameStateTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a standalone input starts normal and shows a state set directly as :warning or :error")
    void stateSetDirectlyShowsAsPseudoClass() {
        PathInput field = new PathInput();
        assertEquals(FieldState.NORMAL, field.getState());
        assertEquals(Set.of(), shownStates(field));

        field.setState(FieldState.WARNING);
        assertEquals(Set.of("warning"), shownStates(field));

        field.setState(FieldState.ERROR);
        assertEquals(Set.of("error"), shownStates(field));

        field.setState(FieldState.NORMAL);
        assertEquals(Set.of(), shownStates(field));
    }

    @Test
    @DisplayName("a null state counts as normal")
    void nullStateIsNormal() {
        PathInput field = new PathInput();
        field.setState(FieldState.ERROR);

        field.setState(null);

        assertEquals(Set.of(), shownStates(field));
    }

    @Test
    @DisplayName("the input's state property is the frame's")
    void formInputStateIsTheFramesState() {
        PathInput field = new PathInput();
        FormInput<String> input = field;

        input.stateProperty().set(FieldState.WARNING);

        assertSame(field.stateProperty(), input.stateProperty());
        assertEquals(Set.of("warning"), shownStates(field));
    }

    @Test
    @DisplayName("the theme draws the error border in the error colour and the warning border in the warning colour")
    void stateBorderInTheTheme() {
        PathInput normal = new PathInput();
        PathInput warning = new PathInput();
        warning.setState(FieldState.WARNING);
        PathInput error = new PathInput();
        error.setState(FieldState.ERROR);
        Region errorColour = tokenSwatch("-fx-status-error");
        Region warningColour = tokenSwatch("-fx-status-paused");
        Region borderColour = tokenSwatch("-fx-control-border");
        layOut(normal, warning, error, errorColour, warningColour, borderColour);

        assertEquals(fill(borderColour), fill(normal));
        assertEquals(fill(warningColour), fill(warning));
        assertEquals(fill(errorColour), fill(error));
    }

    @Test
    @DisplayName("the error and warning borders stay while the frame has the focus")
    void stateBorderBeatsTheFocusBorder() {
        PathInput warning = new PathInput();
        warning.setState(FieldState.WARNING);
        PathInput error = new PathInput();
        error.setState(FieldState.ERROR);
        // :focus-within is set by the toolkit only in a shown, focused window; the test sets it as the toolkit would,
        // to check that the theme's state rules win over its focus rule.
        PseudoClass focusWithin = PseudoClass.getPseudoClass("focus-within");
        warning.pseudoClassStateChanged(focusWithin, true);
        error.pseudoClassStateChanged(focusWithin, true);
        Region errorColour = tokenSwatch("-fx-status-error");
        Region warningColour = tokenSwatch("-fx-status-paused");
        layOut(warning, error, errorColour, warningColour);

        assertEquals(fill(warningColour), fill(warning));
        assertEquals(fill(errorColour), fill(error));
    }

    /** The state pseudo-classes the frame shows. */
    private static Set<String> shownStates(FieldFrame frame) {
        Set<String> names = new HashSet<>();
        for (PseudoClass pseudoClass : frame.getPseudoClassStates()) {
            String name = pseudoClass.getPseudoClassName();
            if (name.equals("warning") || name.equals("error")) {
                names.add(name);
            }
        }
        return names;
    }
}
