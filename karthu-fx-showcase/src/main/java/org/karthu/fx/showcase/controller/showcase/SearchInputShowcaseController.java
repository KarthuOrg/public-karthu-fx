package org.karthu.fx.showcase.controller.showcase;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.form.SuggestionInput;

import java.util.List;

import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.focusWhenShown;
import static org.karthu.fx.showcase.controller.showcase.FormInputShowcaseSupport.sectionLabel;

/**
 * Showcase page for the search input and the suggestion input, in the group <i>Form inputs</i>: the search field's
 * states (empty with the placeholder, with text and the focus, disabled, read-only), a list filter over a sample list
 * of workflows whose rows mark the matches, simple suggestions over names, rich suggestions as person cards with the
 * footer, and a field to try out with a read-out of its text, its counter and its chosen entry.
 *
 * <p>The person cards are built here from the suggestion input's public API alone (a cell factory, highlight nodes,
 * {@code search-strong} and the footer), as an application builds its own: {@link Person} and {@link PersonCardCell}
 * are the showcase's, not karthu-fx's.
 */
public class SearchInputShowcaseController {

    /** The width of every field on the page, so that the cards line up. */
    static final double FIELD_WIDTH = 360;

    /** The sample workflows the list filter filters, TEST-FILE among them. */
    static final List<String> WORKFLOWS = List.of("TEST-FILE", "Customer import", "Nightly report",
            "Invoice export", "Z\u00FCrich branch sync", "Profile cleanup");

    /**
     * The names of the simple suggestions: three with <i>an</i>, others without, and one far longer than the field,
     * which its row clips with an ellipsis.
     */
    static final List<String> NAMES = List.of("Anna Muster", "Andreas Beispiel", "Jean Exemple", "Beat Keller",
            "Claudia Rossi", "David Weber", "Elisabeth Frei", "Hugo Z\u00FCrcher", "Irene Moser", "Lukas Meier",
            "Nicole Huber", "Marie-Christine von Allmen-Oberholzer de la Tour-W\u00FCthrich");

    /**
     * The fourteen invented people of the person cards, with invented roles and departments, Swiss-style placeholder
     * addresses and e-mails at {@code example.com}. Typing <i>an</i> finds two of them, Anna Muster and Andreas
     * Beispiel (no role or department holds <i>an</i>); a role finds only some, <i>developer</i> four of them; the last
     * one's lines are long enough to wrap in the card.
     */
    static final List<Person> PEOPLE = List.of(
            new Person("Anna Muster", "Developer", "Engineering", "Musterstrasse 1, 8000 Z\u00FCrich",
                    "anna.muster@example.com"),
            new Person("Andreas Beispiel", "Designer", "Product", "Beispielweg 2, 3000 Bern",
                    "andreas.beispiel@example.com"),
            new Person("Beat Keller", "Developer", "Engineering", "Kellergasse 7, 4051 Basel",
                    "beat.keller@example.com"),
            new Person("Claudia Rossi", "Controller", "Accounting", "Via Ospedale 4, 6500 Bellinzona",
                    "claudia.rossi@example.com"),
            new Person("David Weber", "Tester", "Quality", "Weberstrasse 3, 9000 St. Gallen",
                    "david.weber@example.com"),
            new Person("Elisabeth Frei", "Recruiter", "People", "Freiestrasse 9, 6003 Luzern",
                    "elisabeth.frei@example.com"),
            new Person("Felix Brunner", "Developer", "Platform", "Brunnmattstrasse 21, 3007 Bern",
                    "felix.brunner@example.com"),
            new Person("Gabriela Steiner", "Product Owner", "Product", "Rue du Rh\u00F4ne 5, 1204 Gen\u00E8ve",
                    "gabriela.steiner@example.com"),
            new Person("Hugo Z\u00FCrcher", "Support Engineer", "Customer Care", "Seestrasse 44, 8800 Thalwil",
                    "hugo.zuercher@example.com"),
            new Person("Irene Moser", "Designer", "Marketing", "Bahnhofplatz 3, 7000 Chur",
                    "irene.moser@example.com"),
            new Person("Lukas Meier", "Developer", "Engineering", "Marktgasse 15, 8400 Winterthur",
                    "lukas.meier@example.com"),
            new Person("Marco Bernasconi", "Lawyer", "Legal", "Via della Posta 8, 6600 Locarno",
                    "marco.bernasconi@example.com"),
            new Person("Nicole Huber", "Office Lead", "Operations", "Kirchweg 6, 5000 Aarau",
                    "nicole.huber@example.com"),
            new Person("Christophe W\u00FCthrich-Oberholzer", "Architect", "Research",
                    "Obere Bahnhofstrasse 117, 8640 Rapperswil-Jona", "christophe.wuethrich-oberholzer@example.com"));

    /** The footer's count of the person cards. */
    static final String PEOPLE_COUNT_FORMAT = "{0} of {1} people";

    @FXML private GridPane statesGrid;
    @FXML private VBox listFilterContainer;
    @FXML private HBox suggestionsRow;
    @FXML private FlowPane tryOutSwitches;
    @FXML private VBox tryOutContainer;

    @FXML
    private void initialize() {
        buildStates();
        buildListFilter();
        buildSuggestions();
        buildTryOut();
    }

    // --- States --------------------------------------------------------------------------------------

    /** The states in their order: empty, with text and the focus, disabled, then read-only. */
    private void buildStates() {
        addState("Empty", searchInput(""));

        SearchInput focused = searchInput("file");
        addState("Focused, with text (on page open)", focused);
        focusWhenShown(focused);

        SearchInput disabled = searchInput("");
        disabled.setDisable(true);
        addState("Disabled", disabled);

        SearchInput readOnly = searchInput("file");
        readOnly.setReadOnly(true);
        addState("Read-only, with text", readOnly);
    }

    private void addState(String name, Node field) {
        FormInputShowcaseSupport.addState(statesGrid, name, field);
    }

    // --- List filter ---------------------------------------------------------------------------------

    /**
     * The list filter: a search field over the sample workflows, connected with one call, the rows marking the matches
     * through the field's highlight nodes; <i>file</i> typed.
     */
    private void buildListFilter() {
        SearchInput field = searchInput("");
        FilteredList<String> workflows = new FilteredList<>(FXCollections.observableArrayList(WORKFLOWS));
        field.filter(workflows, workflow -> workflow);

        ListView<String> list = new ListView<>(workflows);
        list.getStyleClass().add("showcase-workflow-list");
        list.setCellFactory(view -> new HighlightCell(field));
        list.setPrefHeight(200);
        list.setPrefWidth(FIELD_WIDTH);
        list.setMaxWidth(FIELD_WIDTH);
        field.setText("file");

        listFilterContainer.getChildren().addAll(field, sectionLabel("Workflows"), list);
    }

    /**
     * A row of the workflow list: the workflow's name through the field's highlight node, so that its marks follow the
     * search; the theme draws them bold in the accent colour.
     */
    private static final class HighlightCell extends ListCell<String> {

        private final SearchInput field;

        private HighlightCell(SearchInput field) {
            this.field = field;
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(String workflow, boolean empty) {
            super.updateItem(workflow, empty);
            setText(null);
            setGraphic(empty || workflow == null ? null : field.highlight(workflow));
        }
    }

    // --- Suggestions ---------------------------------------------------------------------------------

    /** The two suggestion inputs side by side: simple names, and rich person cards with the footer. */
    private void buildSuggestions() {
        SuggestionInput<String> simple = new SuggestionInput<>();
        simple.getItems().setAll(NAMES);
        sized(simple);

        SuggestionInput<Person> people = new SuggestionInput<>();
        people.getItems().setAll(PEOPLE);
        people.setItemText(person -> person.name());
        people.setSearchTexts(person -> person.texts());
        people.setCellFactory(view -> new PersonCardCell(people));
        people.setFooterVisible(true);
        people.setFooterCountFormat(PEOPLE_COUNT_FORMAT);
        sized(people);

        suggestionsRow.getChildren().addAll(
                column("Suggestions, simple", simple),
                column("Suggestions, rich (person card)", people));
    }

    /** A column of the suggestions card: its name over the field. */
    private static VBox column(String name, SearchInput field) {
        VBox column = new VBox(8, sectionLabel(name), field);
        column.setPrefWidth(FIELD_WIDTH);
        return column;
    }

    // --- Try it out ----------------------------------------------------------------------------------

    /**
     * The try-out field: a suggestion input over the names that also filters them, so that its counter shows, with the
     * switches disabled and read-only, and under it the read-out of its text, its counter as it shows it, its chosen
     * entry and the last choice its handler heard.
     */
    private void buildTryOut() {
        SuggestionInput<String> field = new SuggestionInput<>();
        field.getItems().setAll(NAMES);
        field.filter(new FilteredList<>(FXCollections.observableArrayList(NAMES)), name -> name);
        sized(field);

        CheckBox disabled = new CheckBox("Disabled");
        disabled.selectedProperty().bindBidirectional(field.disableProperty());
        CheckBox readOnly = new CheckBox("Read-only");
        readOnly.selectedProperty().bindBidirectional(field.readOnlyProperty());
        tryOutSwitches.getChildren().addAll(disabled, readOnly);

        Label text = readOutLine();
        Label counter = readOutLine();
        Label chosen = readOutLine();
        Label lastChoice = readOutLine();
        lastChoice.setText("last choice (handler): (none yet)");
        // The counter as the field shows it: its label, found by its style class, which is a contract.
        Label shownCounter = (Label) field.lookup(".form-search-count");
        Runnable showReadOut = () -> {
            text.setText("text: \"" + field.getText() + "\"");
            counter.setText("counter: " + (shownCounter.isVisible() ? shownCounter.getText() : "(not shown)"));
            chosen.setText("chosen entry: " + quotedOrNull(field.getChosenItem()));
        };
        field.textProperty().addListener((observable, oldText, newText) -> showReadOut.run());
        shownCounter.textProperty().addListener((observable, oldText, newText) -> showReadOut.run());
        shownCounter.visibleProperty().addListener((observable, wasVisible, visible) -> showReadOut.run());
        field.chosenItemProperty().addListener((observable, oldItem, newItem) -> showReadOut.run());
        field.setOnItemChosen(name -> lastChoice.setText("last choice (handler): \"" + name + "\""));
        showReadOut.run();

        tryOutContainer.getChildren().addAll(field, text, counter, chosen, lastChoice);
    }

    /** A line of the try-out field's read-out, in the page description's style. */
    private static Label readOutLine() {
        Label line = new Label();
        line.getStyleClass().add("showcase-page-description");
        return line;
    }

    private static String quotedOrNull(String text) {
        return text == null ? "null (none chosen)" : "\"" + text + "\"";
    }

    // --- Helpers -------------------------------------------------------------------------------------

    /** A search field with a text, at the page's width. */
    private static SearchInput searchInput(String text) {
        SearchInput field = new SearchInput();
        field.setText(text);
        return sized(field);
    }

    /** Gives a field the page's width. */
    private static <F extends SearchInput> F sized(F field) {
        field.setPrefWidth(FIELD_WIDTH);
        field.setMaxWidth(FIELD_WIDTH);
        return field;
    }

}
