package org.karthu.fx.showcase.controller.showcase;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.FormInput;
import org.karthu.fx.form.MessageSeverity;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The eight type pages of the group <i>Form Inputs</i>, each loaded from its FXML and laid out with the theme in a
 * scene that is never shown: every page builds its cards with the shared support, and the try-out switches drive the
 * try-out field.
 */
class FormInputPagesShowcaseTest {

    /** The type pages, by the name of their FXML file. */
    private static final List<String> PAGES = List.of("text-input", "password-input", "number-input", "combo-input",
            "editable-combo-input", "text-area-input", "path-input", "cron-input");

    /** The number input's page plays the application: its row's messages come from the value, not from switches. */
    private static final String PAGE_WITHOUT_ROW_SWITCHES = "number-input";

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
        // The first control has the toolkit set its default stylesheet on its own thread: wait for it.
        new Label();
        CountDownLatch started = new CountDownLatch(1);
        Platform.runLater(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "the toolkit's thread did not run");
    }

    @Test
    @DisplayName("every type page builds its cards: named states in form rows and the try-out switches")
    void everyPageBuildsItsCards() throws IOException {
        for (String page : PAGES) {
            Parent root = layOut(page);

            GridPane states = (GridPane) root.lookup("#statesGrid");
            assertTrue(states.getChildren().size() >= 8, page);
            for (Node child : states.getChildren()) {
                Integer column = GridPane.getColumnIndex(child);
                if (column == null || column == 0) {
                    assertTrue(assertInstanceOf(Label.class, child).getStyleClass().contains("section-label"), page);
                } else {
                    assertFalse(child instanceof FormInput<?>, page + ": a state's input stands in its form row");
                }
            }
            List<String> switches = switchTexts(root);
            assertTrue(switches.containsAll(List.of("Disabled", "Read-only", "Bindable", "Bound")), page);
            if (!page.equals(PAGE_WITHOUT_ROW_SWITCHES)) {
                assertTrue(switches.containsAll(List.of("Required", "Error message", "Warning message")), page);
            }
        }
    }

    @Test
    @DisplayName("the try-out switches drive the field: disabled, read-only, bindable, bound, and the row's messages")
    void switchesDriveTheTryOutField() throws IOException {
        for (String page : PAGES) {
            Parent root = layOut(page);
            FormField row = tryOutRow(root);
            FormInput<?> field = row.getInput();

            switchOf(root, "Disabled").setSelected(true);
            assertTrue(field.getNode().isDisable(), page);
            switchOf(root, "Disabled").setSelected(false);
            switchOf(root, "Read-only").setSelected(true);
            assertTrue(field.readOnlyProperty().get(), page);
            switchOf(root, "Read-only").setSelected(false);

            switchOf(root, "Bindable").setSelected(true);
            assertTrue(field.getBinding().isBindable(), page);
            assertFalse(field.getBinding().getCandidates().isEmpty(), page);
            switchOf(root, "Bound").setSelected(true);
            assertNotNull(field.getBinding().getBoundName(), page);
            field.getBinding().setBoundName(null);
            assertFalse(switchOf(root, "Bound").isSelected(), page + ": the switch follows the field");

            if (!page.equals(PAGE_WITHOUT_ROW_SWITCHES)) {
                switchOf(root, "Error message").setSelected(true);
                switchOf(root, "Warning message").setSelected(true);
                assertEquals(List.of(MessageSeverity.ERROR, MessageSeverity.WARNING), severities(row), page);
                switchOf(root, "Error message").setSelected(false);
                assertEquals(List.of(MessageSeverity.WARNING), severities(row), page);
            }
            assertNull(field.getBinding().getBoundName(), page);
        }
    }

    @Test
    @DisplayName("a disabled state dims its whole form row, label and messages with the field, on every page")
    void disabledStatesDimTheirRows() throws IOException {
        for (String page : PAGES) {
            Parent root = layOut(page);

            int disabledRows = 0;
            for (Node child : ((GridPane) root.lookup("#statesGrid")).getChildren()) {
                if (child instanceof FormField row && row.getInput() != null) {
                    boolean disabled = row.getInput().getNode().isDisabled();
                    assertEquals(disabled, hasPseudoClass(row, "input-disabled"), page);
                    if (disabled) {
                        disabledRows++;
                        assertDimmedWithItsInput(row, page);
                    }
                }
            }
            assertTrue(disabledRows >= 1, page + ": no disabled state in a form row");

            FormField tryOut = tryOutRow(root);
            switchOf(root, "Disabled").setSelected(true);
            root.applyCss();
            assertTrue(hasPseudoClass(tryOut, "input-disabled"), page);
            assertDimmedWithItsInput(tryOut, page);
            switchOf(root, "Disabled").setSelected(false);
            assertFalse(hasPseudoClass(tryOut, "input-disabled"), page);
        }
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** Asserts that the row's label and messages are dimmed as its disabled input is, and stay enabled themselves. */
    private static void assertDimmedWithItsInput(FormField row, String page) {
        double inputOpacity = row.getInput().getNode().getOpacity();
        assertTrue(inputOpacity < 1, page);
        List<Node> texts = new ArrayList<>(row.lookupAll(".form-field-label"));
        texts.addAll(row.lookupAll(".form-field-message"));
        assertFalse(texts.isEmpty(), page);
        for (Node text : texts) {
            assertEquals(inputOpacity, text.getOpacity(), 0.001, page);
            assertFalse(text.isDisabled(), page + ": the row's texts stay selectable");
        }
    }

    private static boolean hasPseudoClass(Node node, String name) {
        for (PseudoClass pseudoClass : node.getPseudoClassStates()) {
            if (pseudoClass.getPseudoClassName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Loads a type page and lays it out in the theme, in a scene that is never shown. */
    private static Parent layOut(String page) throws IOException {
        URL url = FormInputPagesShowcaseTest.class.getResource(
                "/org/karthu/fx/showcase/showcase/" + page + "-showcase.fxml");
        assertNotNull(url, page);
        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root, 1400, 900);
        ThemeManager.apply(scene);
        root.applyCss();
        root.layout();
        return root;
    }

    /** The try-out field's row, the first form row of the try-out card. */
    private static FormField tryOutRow(Parent root) {
        VBox container = (VBox) root.lookup("#tryOutContainer");
        for (Node child : container.getChildren()) {
            if (child instanceof FormField row) {
                return row;
            }
        }
        throw new AssertionError("no try-out row");
    }

    private static List<String> switchTexts(Parent root) {
        List<String> texts = new ArrayList<>();
        for (Node child : ((Pane) root.lookup("#tryOutSwitches")).getChildren()) {
            if (child instanceof CheckBox box) {
                texts.add(box.getText());
            }
        }
        return texts;
    }

    private static CheckBox switchOf(Parent root, String text) {
        for (Node child : ((Pane) root.lookup("#tryOutSwitches")).getChildren()) {
            if (child instanceof CheckBox box && box.getText().equals(text)) {
                return box;
            }
        }
        throw new AssertionError("no switch " + text);
    }

    private static List<MessageSeverity> severities(FormField row) {
        List<MessageSeverity> severities = new ArrayList<>();
        for (FieldMessage message : row.getMessages()) {
            severities.add(message.severity());
        }
        return severities;
    }
}
