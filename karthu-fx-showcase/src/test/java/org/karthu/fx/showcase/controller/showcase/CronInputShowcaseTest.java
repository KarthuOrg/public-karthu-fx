package org.karthu.fx.showcase.controller.showcase;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.CronFormat;
import org.karthu.fx.form.CronInput;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FieldState;
import org.karthu.fx.form.FormField;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cron input's page of the group <i>Form Inputs</i>, loaded from its FXML and laid out with the theme in a scene
 * that is never shown: the cron input's own six states and the collection's others, each in its form row with its info
 * line, and the try-out field with its own switch and its read-out of value, format, problem and description. What
 * every type page shares is checked by {@link FormInputPagesShowcaseTest}.
 */
class CronInputShowcaseTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @Test
    @DisplayName("the states card starts with the input's own six states, then bound, missing, read-only, fixed, empty")
    void statesInTheirOrder() throws IOException {
        Map<String, FormField> states = states(layOut());

        assertEquals(List.of("Unix, valid", "Quartz, valid", "Focused (on page open)", "Error, Quartz rule",
                "Error, wrong format", "Disabled", "Bound to a variable", "Bound, variable missing", "Read-only",
                "Fixed format", "Empty, with placeholder"), new ArrayList<>(states.keySet()));
    }

    @Test
    @DisplayName("each state shows its info line: the plain text, or the error with the error border")
    void statesShowTheirInfoLine() throws IOException {
        Map<String, FormField> states = states(layOut());

        CronInput unix = input(states, "Unix, valid");
        assertEquals(CronFormat.UNIX, unix.getFormat());
        assertEquals(List.of(FieldMessage.info("At 09:00, Monday to Friday")), unix.getInputMessages());
        assertEquals(List.of(FieldMessage.info("At 09:00:00, Monday to Friday")),
                input(states, "Quartz, valid").getInputMessages());

        CronInput quartzRule = input(states, "Error, Quartz rule");
        assertEquals(List.of(FieldMessage.error("Day of month or weekday must be ?")), quartzRule.getInputMessages());
        assertEquals(FieldState.ERROR, quartzRule.getState());
        assertEquals(List.of(FieldMessage.error("Unix cron has 5 fields, found 6")),
                input(states, "Error, wrong format").getInputMessages());

        CronInput disabled = input(states, "Disabled");
        assertTrue(disabled.isDisabled());
        assertEquals(List.of(FieldMessage.info("At 09:00, Monday to Friday")), disabled.getInputMessages());

        CronInput bound = input(states, "Bound to a variable");
        assertTrue(bound.getBinding().isBound());
        assertTrue(bound.getInputMessages().isEmpty());
        assertTrue(input(states, "Bound, variable missing").getBinding().isMissing());
        assertTrue(input(states, "Read-only").isReadOnly());
        assertFalse(input(states, "Fixed format").isFormatSelectable());
        CronInput empty = input(states, "Empty, with placeholder");
        assertEquals("", empty.getValue());
        assertFalse(empty.getTextField().getPromptText().isEmpty());
    }

    @Test
    @DisplayName("the try-out field: Format selectable drives it, and the read-out follows value, format and result")
    void tryOutReadOut() throws IOException {
        Parent root = layOut();
        FormField row = tryOutRow(root);
        CronInput field = (CronInput) row.getInput();

        CheckBox selectable = switchOf(root, "Format selectable");
        assertTrue(selectable.isSelected());
        selectable.setSelected(false);
        assertFalse(field.isFormatSelectable());
        selectable.setSelected(true);

        field.setFormat(CronFormat.UNIX);
        field.setValue("0 24 * * *");
        assertEquals(List.of("value: \"0 24 * * *\"", "format: Unix", "problem: Hour must be 0 to 23, found 24",
                "description: (none)"), readOut(root));

        field.setValue("0 9 * * 1-5");
        assertEquals(List.of("value: \"0 9 * * 1-5\"", "format: Unix", "problem: (none)",
                "description: At 09:00, Monday to Friday"), readOut(root));
    }

    @Test
    @DisplayName("the try-out field converts on the user's format change; the read-out says what it made of the text")
    void tryOutShowsTheConversion() throws IOException {
        Parent root = layOut();
        CronInput field = (CronInput) tryOutRow(root).getInput();
        assertEquals("format change: (none yet)", formatChangeLine(root));

        choose(field, CronFormat.UNIX);
        assertEquals(List.of("value: \"0 9 * * MON-FRI\"", "format: Unix", "problem: (none)",
                "description: At 09:00, Monday to Friday"), readOut(root));
        assertEquals("format change: Quartz to Unix, \"0 0 9 ? * MON-FRI\" converted to \"0 9 * * MON-FRI\"",
                formatChangeLine(root));

        field.setValue("0 0 1 * 1");
        choose(field, CronFormat.QUARTZ);
        assertEquals(List.of("value: \"0 0 1 * 1\"", "format: Quartz",
                "problem: Quartz cron has 6 or 7 fields, found 5", "description: (none)"), readOut(root));
        assertEquals("format change: Unix to Quartz, \"0 0 1 * 1\" kept", formatChangeLine(root));
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** Loads the page and lays it out in the theme, in a scene that is never shown. */
    private static Parent layOut() throws IOException {
        URL url = CronInputShowcaseTest.class.getResource("/org/karthu/fx/showcase/showcase/cron-input-showcase.fxml");
        assertNotNull(url);
        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root, 1400, 900);
        ThemeManager.apply(scene);
        root.applyCss();
        root.layout();
        return root;
    }

    /** The states card's rows by their names, in their order. */
    private static Map<String, FormField> states(Parent root) {
        GridPane grid = (GridPane) root.lookup("#statesGrid");
        Map<String, FormField> states = new LinkedHashMap<>();
        String name = null;
        for (Node child : grid.getChildren()) {
            Integer column = GridPane.getColumnIndex(child);
            if (column == null || column == 0) {
                name = ((Label) child).getText();
            } else {
                states.put(name, (FormField) child);
            }
        }
        return states;
    }

    private static CronInput input(Map<String, FormField> states, String name) {
        FormField row = states.get(name);
        assertNotNull(row, name);
        return (CronInput) row.getInput();
    }

    /** The try-out field's row, the first form row of the try-out card. */
    private static FormField tryOutRow(Parent root) {
        for (Node child : ((VBox) root.lookup("#tryOutContainer")).getChildren()) {
            if (child instanceof FormField row) {
                return row;
            }
        }
        throw new AssertionError("no try-out row");
    }

    /** The lines of the try-out field's read-out: value, format, problem and description. */
    private static List<String> readOut(Parent root) {
        List<String> lines = new ArrayList<>();
        for (Node child : ((VBox) root.lookup("#tryOutContainer")).getChildren()) {
            if (child instanceof Label label) {
                String text = label.getText();
                if (text.startsWith("value:") || text.startsWith("format:") || text.startsWith("problem:")
                        || text.startsWith("description:")) {
                    lines.add(text);
                }
            }
        }
        return lines;
    }

    /** The user chooses a format in the input's format selector, a combo box, as a click on an entry does. */
    @SuppressWarnings("unchecked")
    private static void choose(CronInput field, CronFormat format) {
        ((ComboBox<CronFormat>) field.lookup(".form-cron-format")).setValue(format);
    }

    /** The read-out's line of the last format change. */
    private static String formatChangeLine(Parent root) {
        for (Node child : ((VBox) root.lookup("#tryOutContainer")).getChildren()) {
            if (child instanceof Label label && label.getText().startsWith("format change:")) {
                return label.getText();
            }
        }
        throw new AssertionError("no format change line");
    }

    private static CheckBox switchOf(Parent root, String text) {
        for (Node child : ((Pane) root.lookup("#tryOutSwitches")).getChildren()) {
            if (child instanceof CheckBox box && box.getText().equals(text)) {
                return box;
            }
        }
        throw new AssertionError("no switch " + text);
    }
}
