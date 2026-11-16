package org.karthu.fx.form;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.markdown.MarkdownView;
import org.karthu.fx.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The suggestion input through its API and its style classes, without a window: the entries, the popup's content
 * (built, never shown: only the opening and closing of its window are replaced), the keys and clicks that move through
 * it and apply an entry, and the chosen entry, in a scene with the theme that is never shown.
 */
class SuggestionInputTest {

    /** The entries of the recording input, in their order. */
    private static final List<String> NAMES =
            List.of("Anna Muster", "Ben Keller", "Hanna Meier", "Jan Novak", "Lea Z\u00FCrcher");

    private RecordingInput input;
    private TextField other;
    private VBox root;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createInput() {
        input = new RecordingInput();
        input.getItems().addAll(NAMES);
        other = new TextField();
        root = new VBox(input, other);
        root.setFillWidth(false);
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        root.applyCss();
        root.autosize();
        root.layout();
        input.requestFocus();
    }

    // --- Opening -------------------------------------------------------------------------------------

    @Test
    @DisplayName("typing opens the popup with the matching entries in the order of the items, the first preselected")
    void typingOpensThePopup() {
        type("an");

        assertTrue(input.isPopupOpen());
        assertEquals(1, input.windowsShown);
        assertEquals(List.of("Anna Muster", "Hanna Meier", "Jan Novak"), list().getItems());
        assertEquals(0, list().getSelectionModel().getSelectedIndex());
    }

    @Test
    @DisplayName("typing on filters the open popup, the first match preselected again; the window opened once")
    void typingFiltersTheOpenPopup() {
        type("an");
        press(FormTestSupport.keyPressed(KeyCode.DOWN));
        assertEquals("Hanna Meier", list().getSelectionModel().getSelectedItem());

        type("n");

        assertEquals(List.of("Anna Muster", "Hanna Meier"), list().getItems());
        assertEquals(0, list().getSelectionModel().getSelectedIndex());
        type("a m");
        assertEquals(List.of("Anna Muster", "Hanna Meier"), list().getItems());
        assertEquals(1, input.windowsShown);
        assertTrue(input.windowShowing);
    }

    @Test
    @DisplayName("whitespace alone opens nothing; deleting back to whitespace or nothing closes the popup")
    void whitespaceOpensNothing() {
        type("  ");
        assertFalse(input.isPopupOpen());
        assertEquals(0, input.windowsShown);

        type("b");
        assertTrue(input.isPopupOpen());
        input.getTextField().end();
        press(FormTestSupport.keyPressed(KeyCode.BACK_SPACE));
        assertEquals("  ", input.getText());
        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
    }

    @Test
    @DisplayName("never on the focus alone, nor for a text the application sets while the field has no focus")
    void neitherFocusNorTheApplicationOpens() {
        other.requestFocus();
        input.requestFocus();
        assertFalse(input.isPopupOpen());

        other.requestFocus();
        input.setText("anna");
        assertFalse(input.isPopupOpen());
        assertEquals(0, input.windowsShown);
    }

    @Test
    @DisplayName("Down in the empty field opens the popup with every entry, the first preselected; Alt+Down too")
    void downInTheEmptyFieldOpensEveryEntry() {
        assertTrue(consumes(KeyCode.DOWN));
        assertTrue(input.isPopupOpen());
        assertEquals(NAMES, list().getItems());
        assertEquals(0, list().getSelectionModel().getSelectedIndex());

        assertTrue(consumes(KeyCode.ESCAPE));
        assertFalse(input.isPopupOpen());
        assertTrue(press(FormTestSupport.altKeyPressed(KeyCode.DOWN)).isEmpty());
        assertTrue(input.isPopupOpen());
        assertEquals(NAMES, list().getItems());
        assertEquals(2, input.windowsShown);
    }

    @Test
    @DisplayName("Down with text and the popup closed opens it again with the matches of the text")
    void downWithTextOpensTheMatches() {
        type("an");
        consumes(KeyCode.ESCAPE);
        assertFalse(input.isPopupOpen());

        assertTrue(consumes(KeyCode.DOWN));

        assertTrue(input.isPopupOpen());
        assertEquals(List.of("Anna Muster", "Hanna Meier", "Jan Novak"), list().getItems());
        assertEquals("an", input.getText());
    }

    @Test
    @DisplayName("read-only or disabled nothing opens: neither typing, a text set in the field, nor Down")
    void readOnlyAndDisabledOpenNothing() {
        input.setReadOnly(true);
        type("an");
        input.setText("an");
        // Down is the text field's own then (the caret to the end); Alt+Down passes on, as in every single-line input.
        press(FormTestSupport.keyPressed(KeyCode.DOWN));
        assertFalse(press(FormTestSupport.altKeyPressed(KeyCode.DOWN)).isEmpty());
        assertFalse(input.isPopupOpen());

        input.setReadOnly(false);
        input.setText("");
        input.setDisable(true);
        input.setText("an");
        assertFalse(input.isPopupOpen());
        assertEquals(0, input.windowsShown);
    }

    @Test
    @DisplayName("becoming read-only or disabled closes the open popup")
    void readOnlyOrDisabledCloses() {
        type("an");
        input.setReadOnly(true);
        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);

        input.setReadOnly(false);
        consumes(KeyCode.DOWN);
        assertTrue(input.isPopupOpen());
        input.setDisable(true);
        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
    }

    @Test
    @DisplayName("losing the focus closes the popup and keeps the text")
    void losingTheFocusCloses() {
        type("an");

        other.requestFocus();

        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
        assertEquals("an", input.getText());
    }

    // --- Keys while open ------------------------------------------------------------------------------

    @Test
    @DisplayName("Up and Down move the preselection one row and stop at the ends; text, caret and focus stay")
    void upAndDownMoveThePreselection() {
        type("an");
        input.getTextField().positionCaret(1);

        assertTrue(consumes(KeyCode.DOWN));
        assertEquals("Hanna Meier", preselected());
        consumes(KeyCode.DOWN);
        assertTrue(consumes(KeyCode.DOWN));
        assertEquals("Jan Novak", preselected());
        assertTrue(consumes(KeyCode.UP));
        assertEquals("Hanna Meier", preselected());
        consumes(KeyCode.UP);
        assertTrue(consumes(KeyCode.UP));
        assertEquals("Anna Muster", preselected());

        assertEquals("an", input.getText());
        assertEquals(1, input.getTextField().getCaretPosition());
        assertSame(input.getTextField(), input.getScene().getFocusOwner());
        assertTrue(input.isPopupOpen());
        assertFalse(list().isFocusTraversable());
    }

    @Test
    @DisplayName("Page Down and Page Up move eight rows, Home and End to the first and the last; the caret stays")
    void pageHomeAndEndJump() {
        for (int number = 1; number <= 20; number++) {
            input.getItems().add("Entry " + number);
        }
        type("entry");
        input.getTextField().positionCaret(2);

        assertTrue(consumes(KeyCode.PAGE_DOWN));
        assertEquals("Entry 9", preselected());
        consumes(KeyCode.PAGE_DOWN);
        assertTrue(consumes(KeyCode.PAGE_DOWN));
        assertEquals("Entry 20", preselected());
        assertTrue(consumes(KeyCode.PAGE_UP));
        assertEquals("Entry 12", preselected());
        assertTrue(consumes(KeyCode.HOME));
        assertEquals("Entry 1", preselected());
        assertTrue(consumes(KeyCode.END));
        assertEquals("Entry 20", preselected());
        consumes(KeyCode.PAGE_UP);
        consumes(KeyCode.PAGE_UP);
        assertTrue(consumes(KeyCode.PAGE_UP));
        assertEquals("Entry 1", preselected());

        assertEquals(2, input.getTextField().getCaretPosition());
        assertEquals("entry", input.getText());
    }

    @Test
    @DisplayName("with Shift, Home and End are the text field's: they select, the preselection stays")
    void shiftHomeSelectsInTheText() {
        type("an");
        consumes(KeyCode.DOWN);

        press(FormTestSupport.shiftKeyPressed(KeyCode.HOME));

        assertEquals("an", input.getTextField().getSelectedText());
        assertEquals("Hanna Meier", preselected());
    }

    @Test
    @DisplayName("Escape closes the popup and keeps the text; a second Escape empties the field")
    void escapeClosesThenEmpties() {
        type("an");

        assertTrue(consumes(KeyCode.ESCAPE));
        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
        assertEquals("an", input.getText());

        assertTrue(consumes(KeyCode.ESCAPE));
        assertEquals("", input.getText());
        assertFalse(input.isPopupOpen());
        assertEquals(1, input.windowsShown);
    }

    @Test
    @DisplayName("Tab closes the popup without applying, and the focus moves on as always")
    void tabClosesWithoutApplying() {
        type("an");
        consumes(KeyCode.DOWN);

        press(FormTestSupport.keyPressed(KeyCode.TAB));

        assertSame(other, input.getScene().getFocusOwner());
        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
        assertEquals("an", input.getText());
        assertNull(input.getChosenItem());
    }

    @Test
    @DisplayName("with the popup closed Up, Enter, Home and End are the text field's: nothing opens or is applied")
    void keysOfTheClosedPopup() {
        other.requestFocus();
        input.setText("an");
        input.requestFocus();
        List<String> chosen = new ArrayList<>();
        input.setOnItemChosen(chosen::add);

        assertFalse(consumes(KeyCode.ENTER));
        press(FormTestSupport.keyPressed(KeyCode.UP));
        press(FormTestSupport.keyPressed(KeyCode.HOME));
        press(FormTestSupport.keyPressed(KeyCode.END));

        assertFalse(input.isPopupOpen());
        assertEquals(0, input.windowsShown);
        assertTrue(chosen.isEmpty());
        assertEquals("an", input.getText());
    }

    // --- Applying ------------------------------------------------------------------------------------

    @Test
    @DisplayName("Enter applies the preselected entry: its text, the caret at the end, chosen, the handler, closed")
    void enterApplies() {
        List<String> chosen = new ArrayList<>();
        input.setOnItemChosen(chosen::add);
        type("an");
        consumes(KeyCode.DOWN);

        assertTrue(consumes(KeyCode.ENTER));

        assertEquals("Hanna Meier", input.getText());
        assertEquals("Hanna Meier".length(), input.getTextField().getCaretPosition());
        assertEquals("Hanna Meier", input.getChosenItem());
        assertEquals(List.of("Hanna Meier"), chosen);
        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
        assertEquals(1, input.windowsShown);
        assertSame(input.getTextField(), input.getScene().getFocusOwner());
    }

    @Test
    @DisplayName("applying in order: closed, then the text, then the chosen entry, then the handler; nothing reopens")
    void applyingInOrder() {
        List<String> seen = new ArrayList<>();
        input.textProperty().addListener((observable, oldText, newText) ->
                seen.add("text " + newText + ", open " + input.isPopupOpen() + ", chosen " + input.getChosenItem()));
        input.chosenItemProperty().addListener((observable, oldItem, newItem) ->
                seen.add("chosen " + newItem + ", text " + input.getText() + ", open " + input.isPopupOpen()));
        input.setOnItemChosen(item ->
                seen.add("handler " + item + ", chosen " + input.getChosenItem() + ", open " + input.isPopupOpen()));
        type("jan");
        seen.clear();

        consumes(KeyCode.ENTER);

        assertEquals(List.of(
                "text Jan Novak, open false, chosen null",
                "chosen Jan Novak, text Jan Novak, open false",
                "handler Jan Novak, chosen Jan Novak, open false"), seen);
        assertEquals(1, input.windowsShown);
        assertFalse(input.windowShowing);
        assertEquals("Jan Novak", input.getChosenItem());
    }

    @Test
    @DisplayName("connected to a view, Enter applies the preselected entry while the popup shows, then goes to the"
            + " next match")
    void enterWithAViewAppliesWhileThePopupShows() {
        MarkdownView view = new MarkdownView("Anna Muster met Hanna Meier.\n\nAnna Muster again.");
        input.search(view);
        List<String> chosen = new ArrayList<>();
        input.setOnItemChosen(chosen::add);
        type("an");
        assertTrue(input.isPopupOpen());

        assertTrue(consumes(KeyCode.ENTER));
        assertEquals(List.of("Anna Muster"), chosen);
        assertEquals("Anna Muster", view.getSearchText());
        assertEquals(0, view.getSearchMatchIndex());
        assertFalse(input.isPopupOpen());

        assertTrue(consumes(KeyCode.ENTER));
        assertEquals(1, view.getSearchMatchIndex());
        assertEquals(List.of("Anna Muster"), chosen);
    }

    @Test
    @DisplayName("connected to a view, Shift+Enter goes to the previous match also while the popup shows")
    void shiftEnterWithAViewWhileThePopupShows() {
        MarkdownView view = new MarkdownView("Anna Muster met Hanna Meier.\n\nAnna Muster again.");
        input.search(view);
        type("anna");
        assertTrue(input.isPopupOpen());
        assertEquals(0, view.getSearchMatchIndex());

        assertTrue(press(FormTestSupport.shiftKeyPressed(KeyCode.ENTER)).isEmpty());

        assertEquals(2, view.getSearchMatchIndex());
        assertEquals("anna", input.getText());
        assertNull(input.getChosenItem());
    }

    @Test
    @DisplayName("a click on a row applies its entry, whichever row is preselected")
    void clickApplies() {
        List<String> chosen = new ArrayList<>();
        input.setOnItemChosen(chosen::add);
        type("an");

        cell(2).fireEvent(mouse(MouseEvent.MOUSE_CLICKED, MouseButton.PRIMARY));

        assertEquals("Jan Novak", input.getText());
        assertEquals("Jan Novak", input.getChosenItem());
        assertEquals(List.of("Jan Novak"), chosen);
        assertFalse(input.isPopupOpen());
        assertEquals(1, input.windowsShown);
    }

    @Test
    @DisplayName("a click with another button applies nothing")
    void secondaryClickAppliesNothing() {
        type("an");

        cell(2).fireEvent(mouse(MouseEvent.MOUSE_CLICKED, MouseButton.SECONDARY));

        assertEquals("an", input.getText());
        assertNull(input.getChosenItem());
        assertTrue(input.isPopupOpen());
    }

    @Test
    @DisplayName("a press in the field keeps the popup open, also one that moves the caret; the press goes on")
    void pressInTheFieldKeepsThePopupOpen() {
        type("an");
        Node insideTextField = input.getTextField().getChildrenUnmodifiable().getFirst();

        assertTrue(pressReaches(input.getTextField()));
        assertTrue(pressReaches(insideTextField));
        assertTrue(pressReaches(input));

        assertTrue(input.isPopupOpen());
        assertTrue(input.windowShowing);
        assertEquals("an", input.getText());
        assertEquals(1, input.windowsShown);
    }

    @Test
    @DisplayName("a press elsewhere in the window closes the popup and keeps the text; the press goes on to its target")
    void pressOutsideTheFieldCloses() {
        type("an");

        assertTrue(pressReaches(other));

        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
        assertEquals("an", input.getText());
        assertNull(input.getChosenItem());
    }

    @Test
    @DisplayName("the clear button closes the open popup, empties the field and keeps the focus in the text field")
    void clearButtonClosesThePopup() {
        type("an");
        assertTrue(input.isPopupOpen());

        ((Button) input.lookup(".form-search-clear")).fire();

        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
        assertEquals("", input.getText());
        assertSame(input.getTextField(), input.getScene().getFocusOwner());
        assertEquals(1, input.windowsShown);
    }

    // --- Chosen entry --------------------------------------------------------------------------------

    @Test
    @DisplayName("the chosen entry becomes null as soon as the user edits the text, and the popup opens again")
    void editingClearsTheChosenEntry() {
        type("ben");
        consumes(KeyCode.ENTER);
        assertEquals("Ben Keller", input.getChosenItem());

        press(FormTestSupport.keyPressed(KeyCode.BACK_SPACE));

        assertNull(input.getChosenItem());
        assertEquals("Ben Kelle", input.getText());
        assertTrue(input.isPopupOpen());
        assertEquals(2, input.windowsShown);
    }

    @Test
    @DisplayName("the chosen entry becomes null when the application changes the text, or the x empties it")
    void otherChangesClearTheChosenEntry() {
        input.setChosenItem("Ben Keller");
        input.setText("Ben");
        assertNull(input.getChosenItem());

        input.setChosenItem("Ben Keller");
        ((Button) input.lookup(".form-search-clear")).fire();
        assertNull(input.getChosenItem());
        assertEquals("", input.getText());
    }

    @Test
    @DisplayName("set by the application, an entry puts its text into the field without opening; null leaves the text")
    void applicationSetsTheChosenEntry() {
        List<String> chosen = new ArrayList<>();
        input.setOnItemChosen(chosen::add);

        input.setChosenItem("Lea Z\u00FCrcher");

        assertEquals("Lea Z\u00FCrcher", input.getText());
        assertEquals("Lea Z\u00FCrcher".length(), input.getTextField().getCaretPosition());
        assertEquals("Lea Z\u00FCrcher", input.getChosenItem());
        assertFalse(input.isPopupOpen());
        assertEquals(0, input.windowsShown);
        assertTrue(chosen.isEmpty());

        input.setChosenItem(null);
        assertEquals("Lea Z\u00FCrcher", input.getText());
    }

    @Test
    @DisplayName("set by the application while the popup is open, an entry closes it")
    void applicationSetsTheChosenEntryWhileOpen() {
        type("an");

        input.setChosenItem("Ben Keller");

        assertFalse(input.isPopupOpen());
        assertFalse(input.windowShowing);
        assertEquals("Ben Keller", input.getText());
        assertEquals(1, input.windowsShown);
    }

    @Test
    @DisplayName("a chosen entry the application bound is never set by the field: typing and applying leave it")
    void boundChosenEntry() {
        ObjectProperty<String> source = new SimpleObjectProperty<>("Ben Keller");
        input.chosenItemProperty().bind(source);
        List<String> chosen = new ArrayList<>();
        input.setOnItemChosen(chosen::add);
        assertEquals("Ben Keller", input.getText());

        input.setText("");
        type("jan");
        consumes(KeyCode.ENTER);

        assertEquals("Ben Keller", input.getChosenItem());
        assertEquals("Jan Novak", input.getText());
        assertEquals(List.of("Jan Novak"), chosen);
    }

    // --- No match and changing entries ---------------------------------------------------------------

    @Test
    @DisplayName("nothing matches: the popup stays open with the line No matches in place of the list")
    void noMatchKeepsThePopupOpen() {
        type("an");
        assertFalse(isShown(emptyLine()));
        assertTrue(isShown(list()));

        type("x");

        assertTrue(input.isPopupOpen());
        assertTrue(input.windowShowing);
        assertTrue(isShown(emptyLine()));
        assertFalse(isShown(list()));
        assertEquals("No matches", emptyLine().getText());
        assertTrue(list().getItems().isEmpty());
        assertNull(preselected());

        press(FormTestSupport.keyPressed(KeyCode.BACK_SPACE));
        assertTrue(isShown(list()));
        assertFalse(isShown(emptyLine()));
        assertEquals("Anna Muster", preselected());
        assertEquals(1, input.windowsShown);
    }

    @Test
    @DisplayName("nothing matches: the keys move nothing, Enter only closes the popup and applies nothing")
    void noMatchEnterOnlyCloses() {
        // Not "x" typed alone: the field's x in the theme's LCD text would stay in JavaFX's text layout cache, whose
        // whole-pixel line height a later MarkdownViewTest reads for its own unstyled "x"
        // (shortColumnsKeepTheirFullWidth).
        type("qqq");

        assertTrue(consumes(KeyCode.DOWN));
        assertTrue(consumes(KeyCode.END));
        assertTrue(consumes(KeyCode.ENTER));

        assertFalse(input.isPopupOpen());
        assertEquals("qqq", input.getText());
        assertNull(input.getChosenItem());
    }

    @Test
    @DisplayName("the line No matches is a property: an application translates it; null shows an empty line")
    void noMatchesText() {
        assertEquals("No matches", input.getNoMatchesText());
        input.setNoMatchesText("Keine Treffer");
        type("qqq");
        assertEquals("Keine Treffer", emptyLine().getText());
        input.noMatchesTextProperty().set(null);
        assertEquals("", emptyLine().getText());
    }

    @Test
    @DisplayName("changing the items while open filters again; the preselection stays on its entry while it matches")
    void changingItemsKeepsThePreselection() {
        type("an");
        consumes(KeyCode.DOWN);
        assertEquals("Hanna Meier", preselected());

        input.getItems().addFirst("Dana Frei");
        assertEquals(List.of("Dana Frei", "Anna Muster", "Hanna Meier", "Jan Novak"), list().getItems());
        assertEquals("Hanna Meier", preselected());

        input.getItems().remove("Hanna Meier");
        assertEquals(List.of("Dana Frei", "Anna Muster", "Jan Novak"), list().getItems());
        assertEquals("Dana Frei", preselected());

        input.getItems().clear();
        assertTrue(input.isPopupOpen());
        assertTrue(isShown(emptyLine()));
        assertEquals("an", input.getText());
    }

    // --- Item text and search texts -------------------------------------------------------------------

    @Test
    @DisplayName("defaults: the item text is String.valueOf, the search texts the item text alone; null counts as them")
    void itemTextDefaults() {
        SuggestionInput<Integer> numbers = new SuggestionInput<>();
        numbers.setChosenItem(1234);
        assertEquals("1234", numbers.getText());
        assertEquals("1234", numbers.getItemText().apply(1234));
        assertEquals(List.of("1234"), numbers.getSearchTexts().apply(1234));

        numbers.setItemText(number -> "#" + number);
        assertEquals(List.of("#1234"), numbers.getSearchTexts().apply(1234));
        numbers.setItemText(null);
        numbers.setSearchTexts(null);
        numbers.setChosenItem(99);
        assertEquals("99", numbers.getText());
    }

    @Test
    @DisplayName("the item text goes into the field and shows in the row; the search texts are what is matched")
    void itemTextAndSearchTexts() {
        RecordingPeople people = new RecordingPeople();
        Person anna = new Person("Anna Muster", "anna@example.org");
        Person ben = new Person("Ben Keller", "ben@example.org");
        people.getItems().addAll(anna, ben);
        people.setItemText(person -> person.name());
        inScene(people);

        type(people, "example");
        assertTrue(people.isPopupOpen());
        assertTrue(listOf(people).getItems().isEmpty(), "by default only the item text is searched");

        people.setSearchTexts(person -> List.of(person.name(), person.email()));
        assertEquals(List.of(anna, ben), listOf(people).getItems());
        type(people, " anna");
        assertEquals(List.of(anna), listOf(people).getItems());
        assertEquals("[Anna] Muster", marked(cellOf(people, 0)));

        people.getTextField().fireEvent(FormTestSupport.keyPressed(KeyCode.ENTER));
        assertEquals("Anna Muster", people.getText());
        assertSame(anna, people.getChosenItem());
    }

    @Test
    @DisplayName("changing the item text while open shows the new texts in the rows and filters by them")
    void changingTheItemTextWhileOpen() {
        type("an");

        input.setItemText(name -> name.toUpperCase(Locale.ROOT));

        assertEquals(List.of("Anna Muster", "Hanna Meier", "Jan Novak"), list().getItems());
        assertEquals("[AN]NA MUSTER", marked(cell(0)));
        input.setSearchTexts(name -> List.of("x" + name));
        assertTrue(isShown(list()));
        input.setSearchTexts(name -> name.startsWith("J") ? List.of(name) : null);
        assertEquals(List.of("Jan Novak"), list().getItems());
    }

    // --- The default row and the popup's look --------------------------------------------------------

    @Test
    @DisplayName("a row shows the item text through highlight: the typed parts marked, following the typing")
    void rowsMarkTheTypedParts() {
        type("an m");

        assertEquals("[An]na [M]uster", marked(cell(0)));
        assertEquals("H[an]na [M]eier", marked(cell(1)));
        TextFlow flow = line(cell(0));
        assertTrue(flow.getStyleClass().contains("search-highlight"));
        assertNull(cell(0).getText());

        type("u");
        assertEquals(List.of("Anna Muster"), list().getItems());
        assertEquals("[An]na [Mu]ster", marked(cell(0)));
    }

    @Test
    @DisplayName("accents are folded: zurch finds Z\u00FCrcher, its mark covering the \u00FC")
    void accentsAreFolded() {
        type("zurch");

        assertEquals(List.of("Lea Z\u00FCrcher"), list().getItems());
        assertEquals("Lea [Z\u00FCrch]er", marked(cell(0)));
    }

    @Test
    @DisplayName("the popup: its content draws the frame, the list and the line inside; as wide as the field")
    void popupParts() {
        input.setPrefWidth(320);
        root.applyCss();
        root.autosize();
        root.layout();
        type("an");

        assertTrue(popup().getStyleClass().contains("form-suggestion-popup"));
        assertTrue(list().getStyleClass().contains("list-view"));
        assertTrue(emptyLine().getStyleClass().contains("form-suggestion-empty"));
        assertEquals(320, input.getWidth(), 0.01);
        assertEquals(320, popup().prefWidth(-1), 0.01);
        assertTrue(input.getStyleClass().containsAll(List.of("form-frame", "form-search-input",
                "form-suggestion-input")));
    }

    @Test
    @DisplayName("in the theme a row's marks are bold in the text colour, on the preselected row in its text colour")
    void rowLook() {
        type("an");
        ThemeManager.apply(popup().getScene());
        Region text = FormTestSupport.tokenSwatch("-fx-text-color");
        Region selectedText = FormTestSupport.tokenSwatch("-fx-selection-text-color");
        VBox swatches = new VBox(text, selectedText);
        new Scene(swatches);
        ThemeManager.apply(swatches.getScene());
        swatches.applyCss();
        // The list builds its cells in the layout pass, the second CSS pass styles them.
        popup().applyCss();
        popup().autosize();
        popup().layout();
        popup().applyCss();

        ListCell<String> preselectedRow = shownCell(0);
        ListCell<String> otherRow = shownCell(1);
        assertTrue(preselectedRow.isSelected());
        Text mark = (Text) line(otherRow).getChildren().get(1);
        Text plain = (Text) line(otherRow).getChildren().get(0);
        assertEquals("an", mark.getText());
        assertTrue(isBold(mark.getFont()), () -> "not bold: " + mark.getFont());
        assertFalse(isBold(plain.getFont()), () -> "bold: " + plain.getFont());
        assertEquals(FormTestSupport.fill(text), mark.getFill());
        assertEquals(FormTestSupport.fill(text), plain.getFill());
        Text selectedMark = (Text) line(preselectedRow).getChildren().get(0);
        assertEquals("An", selectedMark.getText());
        assertTrue(isBold(selectedMark.getFont()), () -> "not bold: " + selectedMark.getFont());
        assertEquals(FormTestSupport.fill(selectedText), selectedMark.getFill());
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** Types the characters into the field's text field, one key at a time, as the user does. */
    private void type(String characters) {
        type(input, characters);
    }

    /** Types the characters into a field's text field, one key at a time, as the user does. */
    private static void type(SearchInput field, String characters) {
        for (int index = 0; index < characters.length(); index++) {
            String character = String.valueOf(characters.charAt(index));
            field.getTextField().fireEvent(new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED,
                    false, false, false, false));
        }
    }

    /** Puts a field alone into a scene with the theme that is never shown, its skins built, and focuses it. */
    private static void inScene(SearchInput field) {
        VBox parent = new VBox(field);
        Scene scene = new Scene(parent);
        ThemeManager.apply(scene);
        parent.applyCss();
        parent.autosize();
        parent.layout();
        field.requestFocus();
    }

    /**
     * Fires a key press at the text field, where the keyboard sends it, and returns the presses that passed the field
     * unconsumed: empty when the field consumed it.
     */
    private List<Event> press(KeyEvent event) {
        List<Event> passed = new ArrayList<>();
        EventHandler<Event> recorder = passed::add;
        root.addEventHandler(KeyEvent.KEY_PRESSED, recorder);
        input.getTextField().fireEvent(event);
        root.removeEventHandler(KeyEvent.KEY_PRESSED, recorder);
        return passed;
    }

    /** Whether a key press without modifiers is consumed by the field. */
    private boolean consumes(KeyCode code) {
        return press(FormTestSupport.keyPressed(code)).isEmpty();
    }

    /** The popup's content, built and never shown. */
    private Region popup() {
        return input.popupContent();
    }

    /** The popup's list. */
    private ListView<String> list() {
        return listOf(input);
    }

    /** The popup's list of a field. */
    @SuppressWarnings("unchecked")
    private static <E> ListView<E> listOf(SuggestionInput<E> field) {
        return (ListView<E>) field.popupContent().lookup(".form-suggestion-list");
    }

    /** The popup's line shown when nothing matches. */
    private Label emptyLine() {
        return (Label) popup().lookup(".form-suggestion-empty");
    }

    /** The entry the popup's list preselects, or null for none. */
    private String preselected() {
        return list().getSelectionModel().getSelectedItem();
    }

    /**
     * Presses the primary button on a node of the scene and returns whether the press reached the node. While the
     * popup's window shows, the window around it hands every press to the field first, a filter of the window; with no
     * window here, the same filter sits on the scene's root, so the press passes it on its way to the node.
     */
    private boolean pressReaches(Node target) {
        List<Event> reached = new ArrayList<>();
        EventHandler<MouseEvent> windowFilter = input::pressedInWindow;
        EventHandler<Event> recorder = reached::add;
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, windowFilter);
        target.addEventHandler(MouseEvent.MOUSE_PRESSED, recorder);
        target.fireEvent(mouse(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY));
        target.removeEventHandler(MouseEvent.MOUSE_PRESSED, recorder);
        root.removeEventFilter(MouseEvent.MOUSE_PRESSED, windowFilter);
        return !reached.isEmpty();
    }

    /** A mouse event of a button at the top left, still since its press. */
    private static MouseEvent mouse(EventType<MouseEvent> type, MouseButton button) {
        return new MouseEvent(type, 2, 2, 2, 2, button, 1, false, false, false, false,
                button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY, false, false, true, null);
    }

    /** A cell of the list's own cell factory, showing the entry at the index, as the popup shows it. */
    private ListCell<String> cell(int index) {
        return cellOf(input, index);
    }

    /** A cell of a field's list, from the list's own cell factory, showing the entry at the index. */
    private static <E> ListCell<E> cellOf(SuggestionInput<E> field, int index) {
        ListView<E> list = listOf(field);
        ListCell<E> cell = list.getCellFactory().call(list);
        cell.updateListView(list);
        cell.updateIndex(index);
        return cell;
    }

    /**
     * The cell the popup's list itself shows for the entry at the index, after a CSS and layout pass of the popup's
     * content.
     */
    @SuppressWarnings("unchecked")
    private ListCell<String> shownCell(int index) {
        for (Node node : list().lookupAll(".list-cell")) {
            ListCell<String> cell = (ListCell<String>) node;
            if (cell.isVisible() && cell.getIndex() == index) {
                return cell;
            }
        }
        throw new AssertionError("the list shows no cell for the entry at " + index);
    }

    /**
     * The text a row shows, with its marks in brackets: {@code [An]na Muster}. The row's line holds a highlight node,
     * whose runs carry {@code search-text}, the marked ones also {@code search-match}.
     */
    private static String marked(ListCell<?> cell) {
        TextFlow flow = line(cell);
        StringBuilder marked = new StringBuilder();
        for (Node child : flow.getChildren()) {
            Text run = (Text) child;
            assertTrue(run.getStyleClass().contains("search-text"), () -> "a run without search-text: " + run);
            if (run.getStyleClass().contains("search-match")) {
                marked.append('[').append(run.getText()).append(']');
            } else {
                marked.append(run.getText());
            }
        }
        return marked.toString();
    }

    /** The highlight node of a default row: the first in the row's line, before the ellipsis. */
    private static TextFlow line(ListCell<?> cell) {
        return (TextFlow) cell.getGraphic().lookup(".search-highlight");
    }

    /** Whether the node is in the popup and takes room, so that it shows while the popup does. */
    private static boolean isShown(Node node) {
        return node != null && node.getParent() != null && node.isVisible() && node.isManaged();
    }

    /** Whether the font is a bold one. */
    private static boolean isBold(Font font) {
        return font.getStyle().toLowerCase(Locale.ROOT).contains("bold");
    }

    /** A person of the tests: a name and an e-mail address. */
    private record Person(String name, String email) {
    }

    /** A suggestion input over people whose popup window neither opens nor closes, as {@link RecordingInput}. */
    private static final class RecordingPeople extends SuggestionInput<Person> {

        @Override
        void showPopupWindow() {
            // No window: the tests' seam.
        }

        @Override
        void hidePopupWindow() {
            // No window: the tests' seam.
        }
    }

    /**
     * A suggestion input over names whose popup window neither opens nor closes, as a popup needs a shown window: it
     * records each opening and whether the window would show, the tests' seam. Everything else is the field's own.
     */
    private static final class RecordingInput extends SuggestionInput<String> {

        private int windowsShown;
        private boolean windowShowing;

        @Override
        void showPopupWindow() {
            windowsShown++;
            windowShowing = true;
        }

        @Override
        void hidePopupWindow() {
            windowShowing = false;
        }
    }
}
