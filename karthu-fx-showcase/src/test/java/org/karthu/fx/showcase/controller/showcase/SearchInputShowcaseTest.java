package org.karthu.fx.showcase.controller.showcase;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.form.SuggestionInput;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The search input's page of the group <i>Form Inputs</i>, loaded from its FXML and laid out with the theme in a scene
 * that is never shown, and no popup ever shown: the states, the list filter over the workflows with its marks, the
 * simple suggestions, the person cards built from the public API, and the try-out field with its switches and its
 * read-out.
 */
class SearchInputShowcaseTest {

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
    @DisplayName("the states card: empty with the placeholder, focused with text, disabled, read-only with text")
    void statesInTheirOrder() throws IOException {
        Map<String, SearchInput> states = states(layOut());

        assertEquals(List.of("Empty", "Focused, with text (on page open)", "Disabled", "Read-only, with text"),
                new ArrayList<>(states.keySet()));
        SearchInput empty = states.get("Empty");
        assertEquals("", empty.getText());
        assertEquals("Search", empty.getTextField().getPromptText());
        assertEquals("file", states.get("Focused, with text (on page open)").getText());
        assertTrue(states.get("Disabled").isDisabled());
        assertTrue(states.get("Read-only, with text").isReadOnly());
        for (SearchInput field : states.values()) {
            assertFalse(field instanceof SuggestionInput<?>, "a state is a plain search field");
        }
    }

    @Test
    @DisplayName("the list filter: file typed, the workflows filtered, counted and marked in the rows")
    void listFilterOverTheWorkflows() throws IOException {
        Parent root = layOut();
        VBox container = (VBox) root.lookup("#listFilterContainer");
        SearchInput field = assertInstanceOf(SearchInput.class, container.getChildren().get(0));
        assertEquals("Workflows", ((Label) container.getChildren().get(1)).getText());
        @SuppressWarnings("unchecked")
        ListView<String> list = assertInstanceOf(ListView.class, container.getChildren().get(2));

        assertEquals("file", field.getText());
        assertEquals(List.of("TEST-FILE", "Profile cleanup"), list.getItems());
        assertEquals("2 of 6", counter(field).getText());
        assertTrue(counter(field).isVisible());
        assertEquals(List.of("TEST-[FILE]", "Pro[file] cleanup"), shownRows(root, list));

        field.setText("zurich");
        assertEquals(List.of("[Z\u00FCrich] branch sync"), shownRows(root, list));
        field.setText("");
        assertEquals(SearchInputShowcaseController.WORKFLOWS, list.getItems());
        assertFalse(counter(field).isVisible(), "the counter shows without text");
    }

    @Test
    @DisplayName("the suggestions: simple names, and the person cards with their own cells and the footer")
    void suggestionsSimpleAndPersonCards() throws IOException {
        HBox row = (HBox) layOut().lookup("#suggestionsRow");
        SuggestionInput<?> simple = suggestionInput(row, 0);
        SuggestionInput<?> people = suggestionInput(row, 1);

        assertEquals(SearchInputShowcaseController.NAMES, simple.getItems());
        assertNull(simple.getCellFactory(), "the simple suggestions use the default row");
        assertFalse(simple.isFooterVisible());

        assertEquals(SearchInputShowcaseController.PEOPLE, people.getItems());
        assertEquals(14, people.getItems().size());
        assertNotNull(people.getCellFactory());
        assertTrue(people.isFooterVisible());
        assertEquals("{0} of {1} people", people.getFooterCountFormat());
    }

    @Test
    @DisplayName("an finds two of the fourteen people, Anna Muster and Andreas Beispiel")
    void anFindsTwoPeople() {
        FilteredList<Person> people =
                new FilteredList<>(FXCollections.observableArrayList(SearchInputShowcaseController.PEOPLE));
        SearchInput search = new SearchInput();
        // Every text of a card on its own line, so that no word matches across two of them.
        search.filter(people, person -> String.join("\n", person.texts()));

        search.setText("an");
        List<String> names = new ArrayList<>();
        for (Person person : people) {
            names.add(person.name());
        }
        assertEquals(List.of("Anna Muster", "Andreas Beispiel"), names);
    }

    @Test
    @DisplayName("a role finds only some of the people: developer finds four of the fourteen")
    void aRoleFindsSomePeople() {
        FilteredList<Person> people =
                new FilteredList<>(FXCollections.observableArrayList(SearchInputShowcaseController.PEOPLE));
        SearchInput search = new SearchInput();
        search.filter(people, person -> String.join("\n", person.texts()));

        search.setText("developer");
        List<String> names = new ArrayList<>();
        for (Person person : people) {
            names.add(person.name());
        }
        assertEquals(List.of("Anna Muster", "Beat Keller", "Felix Brunner", "Lukas Meier"), names);
        search.setText("designer");
        assertEquals(2, people.size());
    }

    @Test
    @DisplayName("a person card, built from the public API: the initials, the name strong, role, address and e-mail marked")
    @SuppressWarnings("unchecked")
    void personCardFromThePublicApi() throws IOException {
        SuggestionInput<Person> people = (SuggestionInput<Person>) suggestionInput(
                (HBox) layOut().lookup("#suggestionsRow"), 1);
        people.setText("an");

        ListView<Person> view = new ListView<>(FXCollections.observableArrayList(SearchInputShowcaseController.PEOPLE));
        ListCell<Person> cell = people.getCellFactory().call(view);
        assertInstanceOf(PersonCardCell.class, cell);
        cell.updateListView(view);
        cell.updateIndex(0);

        HBox card = (HBox) cell.getGraphic();
        Label avatar = (Label) card.getChildren().get(0);
        assertEquals("AM", avatar.getText());
        VBox lines = (VBox) card.getChildren().get(1);
        List<String> marked = new ArrayList<>();
        for (Node line : lines.getChildren()) {
            marked.add(marked((TextFlow) line));
        }
        assertEquals(List.of("[An]na Muster", "Developer \u00B7 Engineering", "Musterstrasse 1, 8000 Z\u00FCrich",
                "[an]na.muster@example.com"), marked);
        assertTrue(lines.getChildren().get(0).getStyleClass().containsAll(List.of("search-highlight", "search-strong")));
        assertFalse(lines.getChildren().get(1).getStyleClass().contains("search-strong"));
    }

    @Test
    @DisplayName("the initials: the first letters of the first and the last word, in upper case")
    void initials() {
        assertEquals("AM", person("Anna Muster").initials());
        assertEquals("AB", person("andreas beispiel").initials());
        assertEquals("CW", person("Christophe W\u00FCthrich-Oberholzer").initials());
        assertEquals("MT", person("Marie von der Tann").initials());
        assertEquals("\u00C9", person("\u00E9lodie").initials());
    }

    @Test
    @DisplayName("in a narrow row a card wraps its long lines inside the row; the avatar keeps its size")
    void cardWrapsInANarrowRow() {
        Person longest = SearchInputShowcaseController.PEOPLE.getLast();
        SuggestionInput<Person> field = new SuggestionInput<>();
        PersonCardCell cell = new PersonCardCell(field);
        ListView<Person> view = new ListView<>(FXCollections.observableArrayList(longest));
        cell.updateListView(view);
        cell.updateIndex(0);
        VBox root = new VBox(cell);
        root.setFillWidth(false);
        ThemeManager.apply(new Scene(root));
        cell.setPrefWidth(240);
        for (int pass = 0; pass < 2; pass++) {
            root.applyCss();
            root.autosize();
            root.layout();
        }

        HBox card = (HBox) cell.getGraphic();
        Label avatar = (Label) card.getChildren().get(0);
        assertEquals(PersonCardCell.AVATAR_SIZE, avatar.getWidth(), 0.01);
        assertEquals(PersonCardCell.AVATAR_SIZE, avatar.getHeight(), 0.01);
        assertNotNull(avatar.getBackground(), "the avatar has no circle");
        double cellRight = cell.localToScene(cell.getLayoutBounds()).getMaxX();
        TextFlow email = (TextFlow) ((VBox) card.getChildren().get(1)).getChildren().get(3);
        TextFlow roleLine = (TextFlow) ((VBox) card.getChildren().get(1)).getChildren().get(1);
        assertTrue(email.localToScene(email.getLayoutBounds()).getMaxX() <= cellRight + 0.5, "the e-mail overflows");
        assertTrue(email.getHeight() > 1.5 * roleLine.getHeight(), "the long e-mail does not wrap");
    }

    @Test
    @DisplayName("the try-out field: the switches drive it, the read-out follows its text, counter and chosen entry")
    @SuppressWarnings("unchecked")
    void tryOut() throws IOException {
        Parent root = layOut();
        VBox container = (VBox) root.lookup("#tryOutContainer");
        SuggestionInput<String> field = assertInstanceOf(SuggestionInput.class, container.getChildren().get(0));

        switchOf(root, "Disabled").setSelected(true);
        assertTrue(field.isDisabled());
        switchOf(root, "Disabled").setSelected(false);
        switchOf(root, "Read-only").setSelected(true);
        assertTrue(field.isReadOnly());
        switchOf(root, "Read-only").setSelected(false);

        assertEquals(List.of("text: \"\"", "counter: (not shown)", "chosen entry: null (none chosen)",
                "last choice (handler): (none yet)"), readOut(container));
        field.setText("an");
        assertEquals(List.of("text: \"an\"", "counter: 3 of 12", "chosen entry: null (none chosen)",
                "last choice (handler): (none yet)"), readOut(container));
        field.setChosenItem("Anna Muster");
        assertEquals(List.of("text: \"Anna Muster\"", "counter: 1 of 12", "chosen entry: \"Anna Muster\"",
                "last choice (handler): (none yet)"), readOut(container));
        field.setText("Anna");
        assertEquals("chosen entry: null (none chosen)", readOut(container).get(2));
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** Loads the page and lays it out in the theme, in a scene that is never shown. */
    private static Parent layOut() throws IOException {
        URL url = SearchInputShowcaseTest.class.getResource(
                "/org/karthu/fx/showcase/showcase/search-input-showcase.fxml");
        assertNotNull(url);
        Parent root = FXMLLoader.load(url);
        Scene scene = new Scene(root, 1400, 900);
        ThemeManager.apply(scene);
        layOut(root);
        return root;
    }

    /**
     * Two passes, as two pulses of a shown window: a list builds its rows in the first, the second styles them. The
     * page takes its own size first, which a scene that is never shown does not give it.
     */
    private static void layOut(Parent root) {
        for (int pass = 0; pass < 2; pass++) {
            root.applyCss();
            root.autosize();
            root.layout();
        }
    }

    /** The states card's fields by their names, in their order. */
    private static Map<String, SearchInput> states(Parent root) {
        GridPane grid = (GridPane) root.lookup("#statesGrid");
        Map<String, SearchInput> states = new LinkedHashMap<>();
        String name = null;
        for (Node child : grid.getChildren()) {
            Integer column = GridPane.getColumnIndex(child);
            if (column == null || column == 0) {
                name = ((Label) child).getText();
            } else {
                states.put(name, assertInstanceOf(SearchInput.class, child));
            }
        }
        return states;
    }

    /** The field of a column of the suggestions card: its name above it. */
    private static SuggestionInput<?> suggestionInput(HBox row, int column) {
        VBox box = (VBox) row.getChildren().get(column);
        return assertInstanceOf(SuggestionInput.class, box.getChildren().get(1));
    }

    /** The field's counter, by its style class. */
    private static Label counter(SearchInput field) {
        return (Label) field.lookup(".form-search-count");
    }

    /** The rows the list shows, after a layout, each its highlight node's text with the marks in brackets. */
    private static List<String> shownRows(Parent root, ListView<String> list) {
        layOut(root);
        List<String> rows = new ArrayList<>();
        for (int index = 0; index < list.getItems().size(); index++) {
            for (Node node : list.lookupAll(".list-cell")) {
                ListCell<?> cell = (ListCell<?>) node;
                if (cell.isVisible() && cell.getIndex() == index && cell.getGraphic() instanceof TextFlow flow) {
                    rows.add(marked(flow));
                    break;
                }
            }
        }
        return rows;
    }

    /** The text of a highlight node, with its marks in brackets. */
    private static String marked(TextFlow flow) {
        StringBuilder marked = new StringBuilder();
        for (Node child : flow.getChildren()) {
            Text run = (Text) child;
            if (run.getStyleClass().contains("search-match")) {
                marked.append('[').append(run.getText()).append(']');
            } else {
                marked.append(run.getText());
            }
        }
        return marked.toString();
    }

    /** The try-out's read-out: its lines under the field. */
    private static List<String> readOut(VBox container) {
        List<String> lines = new ArrayList<>();
        for (Node child : container.getChildren()) {
            if (child instanceof Label label) {
                lines.add(label.getText());
            }
        }
        return lines;
    }

    private static CheckBox switchOf(Parent root, String text) {
        for (Node child : ((Pane) root.lookup("#tryOutSwitches")).getChildren()) {
            if (child instanceof CheckBox box && box.getText().equals(text)) {
                return box;
            }
        }
        throw new AssertionError("no switch " + text);
    }

    private static Person person(String name) {
        return new Person(name, "Developer", "Engineering", "", "");
    }
}
