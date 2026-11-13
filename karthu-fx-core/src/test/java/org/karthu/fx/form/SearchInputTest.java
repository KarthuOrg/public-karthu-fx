package org.karthu.fx.form;

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.bindIcon;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The search field through its public API and its style classes, without a window: the magnifier, the text with its
 * placeholder, the clear button, Escape, read-only and disabled, in a scene with the theme that is never shown.
 */
class SearchInputTest {

    private SearchInput input;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInput() {
        input = new SearchInput();
    }

    // --- Parts and defaults --------------------------------------------------------------------------

    @Test
    @DisplayName("a new search field: a frame with form-search-input, the magnifier first, the text its focus target")
    void newSearchField() {
        Node magnifier = magnifier(input);

        assertTrue(input.getStyleClass().containsAll(List.of("form-frame", "form-search-input")));
        assertNotNull(magnifier);
        assertTrue(magnifier.getStyleClass().contains("form-shell-leading"));
        assertSame(magnifier, input.getChildrenUnmodifiable().getFirst());
        assertEquals(BootstrapIcons.SEARCH, glyph(magnifier).getIconCode());
        assertSame(input.getTextField(), input.getFocusTarget());
        assertEquals("", input.getText());
    }

    @Test
    @DisplayName("the text is the text field's, both ways; the placeholder is Search, a property, null shows none")
    void textAndPlaceholder() {
        input.setText("anna");
        assertEquals("anna", input.getTextField().getText());
        input.getTextField().setText("anna zurich");
        assertEquals("anna zurich", input.textProperty().get());
        assertSame(input.getTextField().textProperty(), input.textProperty());

        assertEquals("Search", input.getPromptText());
        assertEquals("Search", input.getTextField().getPromptText());
        input.setPromptText("Suchen");
        assertEquals("Suchen", input.getTextField().getPromptText());
        input.promptTextProperty().set(null);
        assertEquals("", input.getTextField().getPromptText());
    }

    @Test
    @DisplayName("not a form input: it offers no binding, so the {} icon never shows")
    void notAFormInput() {
        Object field = input;
        layOut(input);

        assertFalse(field instanceof FormInput<?>);
        assertFalse(input.getBinding().isBindable());
        assertFalse(bindIcon(input).isVisible() && bindIcon(input).isManaged());
    }

    @Test
    @DisplayName("in the theme the magnifier looks like a trailing icon at rest: as large, in the muted icon colour")
    void magnifierLooksLikeATrailingIcon() {
        input.setText("anna");
        Region muted = tokenSwatch("-fx-text-secondary-color");
        layOut(input, muted);

        FontIcon magnifierGlyph = glyph(magnifier(input));
        FontIcon clearGlyph = glyph(clearButton(input));
        assertEquals(fill(muted), magnifierGlyph.getIconColor());
        assertEquals(clearGlyph.getIconColor(), magnifierGlyph.getIconColor());
        assertEquals(clearGlyph.getIconSize(), magnifierGlyph.getIconSize());
        // Room before the magnifier, so it does not touch the border.
        assertTrue(((Region) magnifier(input)).getPadding().getLeft() > 0);
    }

    // --- Clear button --------------------------------------------------------------------------------

    @Test
    @DisplayName("left to right: the magnifier, the text, the clear button at the right end; all as high as a text field")
    void partsInTheirOrder() {
        TextField plain = plainTextField();
        input.setText("anna");
        input.setPrefWidth(300);
        layOut(plain, input);

        // The magnifier's layout box: its bounds in the parent would include the glyph's overhang.
        Region magnifier = (Region) magnifier(input);
        Bounds text = input.getTextField().getBoundsInParent();
        Bounds clear = clearButton(input).getBoundsInParent();
        assertEquals(0, magnifier.getLayoutX(), 0.01);
        assertTrue(magnifier.getWidth() > 0);
        assertEquals(magnifier.getLayoutX() + magnifier.getWidth(), text.getMinX(), 0.01);
        assertTrue(text.getMaxX() <= clear.getMinX());
        assertTrue(clear.getMaxX() <= input.getWidth());
        assertTrue(clear.getMaxX() > input.getWidth() - 6);
        assertEquals(input.getHeight() / 2, magnifier.getLayoutY() + magnifier.getHeight() / 2, 1);
        assertEquals(plain.getHeight(), input.getHeight(), 0.01);
    }

    @Test
    @DisplayName("the clear button, an x out of the tab order, shows while there is text and only then")
    void clearButtonShowsWithText() {
        Button clear = clearButton(input);
        assertNotNull(clear);
        assertEquals(BootstrapIcons.X, glyph(clear).getIconCode());
        assertFalse(clear.isFocusTraversable());
        assertFalse(isShown(clear));

        input.setText("a");
        assertTrue(isShown(clear));

        input.getTextField().clear();
        assertFalse(isShown(clear));
    }

    @Test
    @DisplayName("the x shows as the user types; Tab passes the field in one step, only its text field takes the focus")
    void typingAndTabOrder() {
        layOut(input);

        input.getTextField().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, "a", "", KeyCode.UNDEFINED,
                false, false, false, false));

        assertEquals("a", input.getText());
        assertTrue(isShown(clearButton(input)));
        assertTrue(input.getTextField().isFocusTraversable());
        assertFalse(magnifier(input).isFocusTraversable());
        assertFalse(clearButton(input).isFocusTraversable());
        assertFalse(input.isFocusTraversable());
    }

    @Test
    @DisplayName("the x's tooltip and accessible text: Clear, a property an application translates")
    void clearText() {
        Button clear = clearButton(input);
        assertEquals("Clear", input.getClearText());
        assertEquals("Clear", clear.getTooltip().getText());
        assertEquals("Clear", clear.getAccessibleText());

        input.setClearText("Leeren");
        assertEquals("Leeren", clear.getTooltip().getText());
        assertEquals("Leeren", clear.getAccessibleText());

        input.clearTextProperty().set("Suche leeren");
        assertEquals("Suche leeren", clear.getTooltip().getText());
        assertEquals("Suche leeren", clear.getAccessibleText());
    }

    @Test
    @DisplayName("a click on the x empties the field and keeps the focus in the text field")
    void clearKeepsTheFocus() {
        Scene scene = new Scene(new VBox(new TextField(), input));
        input.setText("anna");
        input.requestFocus();

        clearButton(input).fire();

        assertEquals("", input.getText());
        assertSame(input.getTextField(), scene.getFocusOwner());
        assertFalse(isShown(clearButton(input)));
    }

    @Test
    @DisplayName("a click on the x while another field has the focus gives it to the text field")
    void clearGivesTheFocus() {
        TextField other = new TextField();
        Scene scene = new Scene(new VBox(other, input));
        input.setText("anna");
        other.requestFocus();

        clearButton(input).fire();

        assertEquals("", input.getText());
        assertSame(input.getTextField(), scene.getFocusOwner());
    }

    // --- Escape --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Escape empties a field with text and is consumed")
    void escapeEmptiesTheField() {
        // The text field gets its skin, and with it its own key handling, in a CSS pass.
        layOut(input);
        input.setText("anna");

        List<Event> passed = firesThrough(input, keyPressed(KeyCode.ESCAPE));

        assertEquals("", input.getText());
        assertTrue(passed.isEmpty());
    }

    @Test
    @DisplayName("Escape in an empty field passes on unconsumed, so a dialog around it still closes")
    void escapeInAnEmptyFieldPassesOn() {
        layOut(input);

        List<Event> passed = firesThrough(input, keyPressed(KeyCode.ESCAPE));

        assertEquals(1, passed.size());
        assertEquals("", input.getText());
    }

    // --- Read-only and disabled ----------------------------------------------------------------------

    @Test
    @DisplayName("read-only: the text is selectable and copyable, not changeable; the x hides, the magnifier stays")
    void readOnly() {
        RecordingClipboard clipboard = new RecordingClipboard("pasted");
        input.setClipboard(clipboard);
        input.setText("anna");
        input.setReadOnly(true);
        TextField plain = plainTextField();
        layOut(plain, input);

        assertTrue(hasPseudoClass(input, "readonly"));
        assertFalse(input.getTextField().isEditable());
        assertFalse(isShown(clearButton(input)));
        assertTrue(isShown(magnifier(input)));
        input.getTextField().selectAll();
        input.getTextField().copy();
        input.getTextField().cut();
        input.getTextField().paste();
        clearButton(input).fire();
        List<Event> passed = firesThrough(input, keyPressed(KeyCode.ESCAPE));

        assertEquals(List.of("anna"), clipboard.copied());
        assertEquals("anna", input.getText());
        assertEquals(1, passed.size(), "Escape changes nothing, so it passes on");
        assertEquals(plain.getHeight(), input.getHeight(), 0.01);

        input.setReadOnly(false);
        assertTrue(input.getTextField().isEditable());
        assertTrue(isShown(clearButton(input)));
    }

    @Test
    @DisplayName("disabled: dimmed once as a disabled button, the magnifier with it; the x hides, back when enabled")
    void disabled() {
        Button button = new Button("button");
        button.setDisable(true);
        input.setText("anna");
        input.setDisable(true);
        TextField plain = plainTextField();
        layOut(plain, input, button);

        assertTrue(input.getOpacity() < 1);
        assertEquals(button.getOpacity(), input.getOpacity(), 0.001);
        assertTrue(magnifier(input).isDisabled());
        assertEquals(1, magnifier(input).getOpacity(), 0.001);
        assertEquals(1, glyph(magnifier(input)).getOpacity(), 0.001);
        assertEquals(1, input.getTextField().getOpacity(), 0.001);
        assertFalse(isShown(clearButton(input)));
        assertEquals(plain.getHeight(), input.getHeight(), 0.01);

        input.setDisable(false);
        assertTrue(isShown(clearButton(input)));
    }

    @Test
    @DisplayName("disabled through a parent, the x hides too")
    void disabledThroughAParent() {
        VBox parent = new VBox(input);
        input.setText("anna");

        parent.setDisable(true);
        assertFalse(isShown(clearButton(input)));

        parent.setDisable(false);
        assertTrue(isShown(clearButton(input)));
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** The clear button, the field's x. */
    private static Button clearButton(SearchInput field) {
        return (Button) field.lookup(".form-search-clear");
    }

    /** The magnifier, the field's leading graphic. */
    private static Node magnifier(SearchInput field) {
        return field.lookup(".form-search-icon");
    }

    /** The icon a part shows: the node itself, a button's graphic, or the first icon inside it. */
    private static FontIcon glyph(Node part) {
        if (part instanceof FontIcon icon) {
            return icon;
        }
        if (part instanceof Labeled labeled) {
            return glyph(labeled.getGraphic());
        }
        if (part instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                FontIcon icon = glyph(child);
                if (icon != null) {
                    return icon;
                }
            }
        }
        return null;
    }

    /** Whether the node is in the field and takes room, so that it shows while the field does. */
    private static boolean isShown(Node node) {
        return node != null && node.getParent() != null && node.isVisible() && node.isManaged();
    }

    /** A plain text field, to compare heights with. */
    private static TextField plainTextField() {
        return new TextField("plain");
    }
}
