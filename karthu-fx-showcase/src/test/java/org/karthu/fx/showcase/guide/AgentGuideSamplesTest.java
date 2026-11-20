package org.karthu.fx.showcase.guide;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.Command;
import org.karthu.fx.command.CommandContext;
import org.karthu.fx.command.CommandGroup;
import org.karthu.fx.command.CommandRegistry;
import org.karthu.fx.command.Shortcut;
import org.karthu.fx.command.ShortcutsDialog;
import org.karthu.fx.command.ShortcutsEditor;
import org.karthu.fx.components.KeyValueEditor;
import org.karthu.fx.components.KeyValueEntry;
import org.karthu.fx.components.ToggleSwitch;
import org.karthu.fx.components.TwinColumnSelector;
import org.karthu.fx.config.ConfigService;
import org.karthu.fx.config.WindowState;
import org.karthu.fx.dialog.CustomDialog;
import org.karthu.fx.form.BindingCandidate;
import org.karthu.fx.form.ComboInput;
import org.karthu.fx.form.CronFormat;
import org.karthu.fx.form.CronInput;
import org.karthu.fx.form.EditableComboInput;
import org.karthu.fx.form.FieldMessage;
import org.karthu.fx.form.FieldState;
import org.karthu.fx.form.FormField;
import org.karthu.fx.form.NumberInput;
import org.karthu.fx.form.PasswordInput;
import org.karthu.fx.form.PathFilter;
import org.karthu.fx.form.PathInput;
import org.karthu.fx.form.PathMode;
import org.karthu.fx.form.SearchInput;
import org.karthu.fx.form.SuggestionInput;
import org.karthu.fx.form.TextAreaInput;
import org.karthu.fx.form.TextInput;
import org.karthu.fx.markdown.CodeCategory;
import org.karthu.fx.markdown.CodePiece;
import org.karthu.fx.markdown.MarkdownView;
import org.karthu.fx.markdown.SearchDirection;
import org.karthu.fx.settings.SettingCategory;
import org.karthu.fx.settings.SettingEntry;
import org.karthu.fx.settings.SettingGroup;
import org.karthu.fx.settings.SettingsDialog;
import org.karthu.fx.settings.SettingsDialogResult;
import org.karthu.fx.settings.SettingsModel;
import org.karthu.fx.theme.Appearance;
import org.karthu.fx.theme.ThemeManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The code samples of {@code docs/agent-guide.md}, compiled and run against the library as an application sees it:
 * from another module, through the exported API alone. A sample that would show a window (a dialog) is compiled
 * only, and the guide's stylesheet sample is applied as the application's stylesheet. Whoever changes a sample
 * changes it in the guide too, and the other way round: the first test holds the guide to this class, line by line.
 */
class AgentGuideSamplesTest {

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

    /** The guide, from the module's directory, where the build runs the tests. */
    private static final Path GUIDE = Path.of("..", "docs", "agent-guide.md");

    /** This class's source. */
    private static final Path SAMPLES =
            Path.of("src", "test", "java", "org", "karthu", "fx", "showcase", "guide", "AgentGuideSamplesTest.java");

    @Test
    @DisplayName("every line of every Java and CSS sample in the guide's section Samples stands in this class")
    void guideShowsTheseSamples() throws IOException {
        List<String> here = new ArrayList<>();
        for (String line : Files.readAllLines(SAMPLES, StandardCharsets.UTF_8)) {
            here.add(line.strip());
        }
        boolean inSamples = false;
        boolean inCode = false;
        int checked = 0;
        for (String line : Files.readAllLines(GUIDE, StandardCharsets.UTF_8)) {
            if (line.startsWith("## ")) {
                inSamples = line.startsWith("## 4. Samples");
            } else if (inSamples && line.startsWith("```")) {
                inCode = !inCode && (line.startsWith("```java") || line.startsWith("```css"));
            } else if (inSamples && inCode && !line.isBlank()) {
                assertTrue(here.contains(line.strip()), "the guide shows a line this class does not hold: " + line);
                checked++;
            }
        }
        assertTrue(checked > 100, "the guide's samples were found: " + checked + " lines");
    }

    @Test
    @DisplayName("Theme: applied to a scene before it is shown")
    void theme() {
        VBox root = new VBox();
        Scene scene = new Scene(root, 800, 600);
        ThemeManager.apply(scene);

        assertEquals(2, scene.getStylesheets().size());
    }

    @Test
    @DisplayName("Colour scheme: Dark without a choice, the stored choice at start, a setting that changes it")
    void colorScheme() {
        assertEquals(Appearance.DARK, ThemeManager.getAppearance(), "without a choice the scheme is Dark");
        try {
            ThemeManager.setAppearance(Appearance.valueOf(storedAppearance()));
            Scene scene = new Scene(new VBox(), 1000, 700);
            ThemeManager.apply(scene);

            ComboInput<Appearance> theme = new ComboInput<>();
            theme.getItems().setAll(Appearance.values());
            theme.setDisplayText(appearance -> switch (appearance) {
                case LIGHT -> "Light";
                case DARK -> "Dark";
                case SYSTEM -> "System";
            });
            theme.setValue(ThemeManager.getAppearance());
            theme.valueProperty().addListener((observable, oldChoice, newChoice) -> {
                ThemeManager.setAppearance(newChoice);
                storeAppearance(newChoice.name());
            });

            assertEquals(Appearance.SYSTEM, ThemeManager.getAppearance(), "the stored choice");
            theme.setValue(Appearance.LIGHT);
            assertEquals(ColorScheme.LIGHT, ThemeManager.getColorScheme(), "the user's choice is in effect");
            assertTrue(scene.getStylesheets().get(1).endsWith("/light.css"),
                    "the scene follows: " + scene.getStylesheets());
            assertEquals("LIGHT", keptAppearance, "the application kept the choice");
        } finally {
            ThemeManager.setAppearance(Appearance.DARK);
        }
    }

    /** Stands for the application's own storage of the scheme the user chose. */
    private static String storedAppearance() {
        return "SYSTEM";
    }

    /** What the application kept last, by {@link #storeAppearance(String)}. */
    private String keptAppearance;

    /** Stands for the application's own storage of the scheme the user chose. */
    private void storeAppearance(String appearance) {
        keptAppearance = appearance;
    }

    /** The guide's sample of an application's own stylesheet: a colour of its own with a value per scheme. */
    private static final String APP_CSS = """
            .root:dark {
                -app-tag-color: #3b3360;
            }

            .root:light {
                -app-tag-color: #e2dcf5;
            }

            .tag {
                -fx-background-color: -app-tag-color;
                -fx-background-radius: 3;
                -fx-padding: 0 6 0 6;
            }
            """;

    @Test
    @DisplayName("Own colours: a token of the application's own per scheme, and a colour drawn in code that follows")
    void ownColors() {
        Label draft = new Label("Draft");
        draft.getStyleClass().add("tag");
        Scene scene = new Scene(new VBox(draft));
        ThemeManager.apply(scene);
        scene.getStylesheets().add(appStylesheet());

        Canvas chart = new Canvas(200, 80);
        ChangeListener<ColorScheme> redraw = (observable, oldScheme, newScheme) -> drawChart(chart, newScheme);
        ThemeManager.colorSchemeProperty().addListener(redraw);
        try {
            drawChart(chart, ThemeManager.getColorScheme());

            scene.getRoot().applyCss();
            assertEquals(Color.web("#3b3360"), draft.getBackground().getFills().get(0).getFill(), "the tag in Dark");
            assertEquals(Color.web("#5fb3d9"), chart.getGraphicsContext2D().getFill(), "the chart in Dark");

            ThemeManager.setAppearance(Appearance.LIGHT);
            scene.getRoot().applyCss();
            assertEquals(Color.web("#e2dcf5"), draft.getBackground().getFills().get(0).getFill(), "the tag in Light");
            assertEquals(Color.web("#1d6a8c"), chart.getGraphicsContext2D().getFill(), "the chart in Light");
            assertTrue(scene.getStylesheets().get(2).startsWith("data:"), "the application's stylesheet stays last");
        } finally {
            // The window closes, as the guide says: its listener goes first, so that going back to Dark draws nothing.
            ThemeManager.colorSchemeProperty().removeListener(redraw);
            ThemeManager.setAppearance(Appearance.DARK);
        }
    }

    /** Stands for the address of the application's own stylesheet: the guide's sample, as a data address. */
    private static String appStylesheet() {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(APP_CSS.getBytes(StandardCharsets.UTF_8));
    }

    /** The guide's sample of a colour set in code: a chart drawn on a canvas in a colour per scheme. */
    private static void drawChart(Canvas chart, ColorScheme scheme) {
        Color bars = scheme == ColorScheme.LIGHT ? Color.web("#1d6a8c") : Color.web("#5fb3d9");
        GraphicsContext pen = chart.getGraphicsContext2D();
        pen.clearRect(0, 0, chart.getWidth(), chart.getHeight());
        pen.setFill(bars);
        pen.fillRect(10, 20, 30, 60);
    }

    @Test
    @DisplayName("Form row: a text input in a row, the application validates and sets the messages")
    void formRow() {
        TextInput name = new TextInput();
        name.setPromptText("First and last name");

        FormField nameRow = new FormField("Name", name);
        nameRow.setRequired(true);
        nameRow.setDescription("The name printed on the invoice");

        name.valueProperty().addListener((observable, oldText, newText) -> {
            if (newText == null || newText.isBlank()) {
                nameRow.getMessages().setAll(FieldMessage.error("is required"));
            } else {
                nameRow.getMessages().clear();
            }
        });

        name.setValue("Anna Muster");
        name.setValue("");
        assertEquals(FieldState.ERROR, name.getState());
        name.setValue("Anna Muster");
        assertEquals(FieldState.NORMAL, name.getState());
    }

    @Test
    @DisplayName("The other inputs: password, number, combo, editable combo, text area")
    void inputs() {
        PasswordInput password = new PasswordInput();
        password.setRevealAllowed(true);

        NumberInput quantity = new NumberInput();
        quantity.setWholeNumbers(true);
        quantity.setMin(BigDecimal.ONE);
        quantity.setMax(BigDecimal.valueOf(99));
        quantity.setStep(BigDecimal.ONE);
        quantity.setArrowsVisible(true);
        quantity.setValue("12");
        Optional<BigDecimal> number = quantity.toNumber();

        ComboInput<CronFormat> format = new ComboInput<>();
        format.getItems().addAll(CronFormat.UNIX, CronFormat.QUARTZ);
        format.setDisplayText(CronFormat::displayName);
        format.setValue(CronFormat.UNIX);

        EditableComboInput encoding = new EditableComboInput();
        encoding.getSuggestions().addAll("UTF-8", "UTF-16", "ISO-8859-1");
        encoding.setValue("UTF-8");

        TextAreaInput notes = new TextAreaInput();
        notes.setPrefRowCount(6);

        VBox form = new VBox(8,
                new FormField("Password", password),
                new FormField("Quantity", quantity),
                new FormField("Format", format),
                new FormField("Encoding", encoding),
                new FormField("Notes", notes));

        assertEquals(new BigDecimal("12"), number.orElseThrow());
        assertEquals(5, form.getChildren().size());
    }

    @Test
    @DisplayName("Path input: mode, filters and base directory")
    void pathInput() {
        PathInput file = new PathInput();
        file.setMode(PathMode.OPEN);
        file.getFilters().add(new PathFilter("CSV files", List.of("*.csv")));
        file.setBaseDirectory(Path.of("data"));
        file.setValue("orders/2026.csv");
        Optional<Path> path = file.toPath();

        assertTrue(path.isPresent());
    }

    @Test
    @DisplayName("Cron input: the format, the problem and the description")
    void cronInput() {
        CronInput schedule = new CronInput();
        schedule.setFormat(CronFormat.UNIX);
        schedule.setValue("0 9 * * 1-5");
        String meaning = schedule.getDescription();
        String problem = schedule.getProblem();

        assertFalse(meaning.isEmpty());
        assertTrue(problem.isEmpty());
        schedule.setValue("0 24 * * *");
        assertFalse(schedule.getProblem().isEmpty());
    }

    @Test
    @DisplayName("Binding: the application offers variables and listens to the bound name")
    void binding() {
        TextInput customer = new TextInput();
        customer.getBinding().setBindable(true);
        customer.getBinding().getCandidates().setAll(
                new BindingCandidate("customer.name", "Text"),
                new BindingCandidate("customer.city", "Text"));
        List<String> boundNames = new ArrayList<>();
        customer.getBinding().boundNameProperty().addListener(
                (observable, oldName, newName) -> boundNames.add(newName));

        customer.getBinding().setBoundName("customer.name");
        assertTrue(customer.getBinding().isBound());
        assertEquals(List.of("customer.name"), boundNames);
    }

    @Test
    @DisplayName("Search: a field that filters a list, and a field with suggestions")
    void search() {
        ObservableList<String> cities = FXCollections.observableArrayList("Basel", "Bern", "Zürich");
        FilteredList<String> shown = new FilteredList<>(cities);
        ListView<String> list = new ListView<>(shown);
        SearchInput search = new SearchInput();
        search.filter(shown, city -> city);

        SuggestionInput<String> city = new SuggestionInput<>();
        city.getItems().setAll(cities);
        city.setItemText(item -> item);
        city.setSearchTexts(item -> List.of(item));
        List<String> chosen = new ArrayList<>();
        city.setOnItemChosen(chosen::add);
        city.setChevronVisible(true);
        city.setChevronText("Show all cities");

        search.setText("zurich");
        assertEquals(List.of("Zürich"), new ArrayList<>(list.getItems()));
        assertTrue(chosen.isEmpty());
        assertTrue(city.isChevronVisible());
    }

    @Test
    @DisplayName("Markdown view: the text, links, images, an own language, the zoom, the margins and the source"
            + " line")
    void markdownView() {
        MarkdownView view = new MarkdownView("# Release notes\n\nSee [the guide](https://example.com/guide).");
        List<String> opened = new ArrayList<>();
        view.setOnLinkAction(opened::add);
        view.setImageBase(Path.of("docs").toUri().toString());
        view.setRemoteImages(false);
        view.getCodeLanguages().put("todo", code -> {
            List<CodePiece> pieces = new ArrayList<>();
            int at = code.indexOf("TODO");
            while (at >= 0) {
                pieces.add(new CodePiece(at, at + 4, CodeCategory.KEYWORD));
                at = code.indexOf("TODO", at + 4);
            }
            return pieces;
        });
        view.setZoom(1.25);
        view.setLeftMargin(48);
        view.setRightMargin(48);
        view.updateMarkdown("# Release notes\n\nNow with search.\n\nSee [the guide](https://example.com/guide).");
        int editorLine = view.searchMatchSourceLine().orElse(view.topSourceLine().orElse(1));
        List<Integer> editorLines = new ArrayList<>();
        view.setContextMenuCallback((items, place) -> {
            MenuItem openInEditor = new MenuItem("Open in Editor");
            openInEditor.setOnAction(event -> editorLines.add(place.sourceLine().orElse(editorLine)));
            items.addAll(new SeparatorMenuItem(), openInEditor);
        });

        assertEquals(1, editorLine, "no search, the top block: the heading on line 1");
        assertNotNull(view.getContextMenuCallback());
        assertEquals(List.of(), editorLines, "nothing opened before a menu entry was chosen");
        assertTrue(view.getCodeLanguages().containsKey("java"));
        assertEquals(1.25, view.getZoom(), 0.001);
        assertEquals(48, view.getLeftMargin());
        assertEquals(48, view.getRightMargin());
        assertEquals("# Release notes\n\nNow with search.\n\nSee [the guide](https://example.com/guide).",
                view.getMarkdown());
        view.markdownProperty().bind(new SimpleStringProperty("# Bound"));
        assertThrows(RuntimeException.class, () -> view.updateMarkdown("# Another version"),
                "updateMarkdown throws while the text is bound");
    }

    @Test
    @DisplayName("Search in a Markdown view: a search field connected to the view, a menu item for F3 and the"
            + " direction")
    void markdownSearch() {
        MarkdownView document = new MarkdownView("# Offices\n\nZürich and Bern.\n\nThe office in Zurich opens at nine.");
        SearchInput searchField = new SearchInput();
        searchField.search(document);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        VBox reader = new VBox(8, new HBox(8, searchField), document);
        VBox.setVgrow(document, Priority.ALWAYS);

        MenuItem findNext = new MenuItem("Find next");
        findNext.setAccelerator(new KeyCodeCombination(KeyCode.F3));
        findNext.setOnAction(event -> document.nextSearchMatch());
        CheckMenuItem upwards = new CheckMenuItem("Search upwards");
        upwards.selectedProperty().addListener((observable, wasOn, isOn) ->
                document.setSearchDirection(isOn ? SearchDirection.BACKWARD : SearchDirection.FORWARD));

        searchField.setText("zurich");
        assertEquals("zurich", document.getSearchText());
        assertEquals(2, document.getSearchMatchCount(), "Zürich and Zurich");
        assertEquals(0, document.getSearchMatchIndex());
        findNext.fire();
        assertEquals(1, document.getSearchMatchIndex());
        upwards.setSelected(true);
        findNext.fire();
        assertEquals(0, document.getSearchMatchIndex(), "upwards, the match above");

        document.clearSearch();
        assertEquals("", searchField.getText(), "the field follows the view");
        assertEquals(-1, document.getSearchMatchIndex());
        assertEquals(2, reader.getChildren().size());
    }

    @Test
    @DisplayName("Commands: a registry for the window, groups, a context, bound controls, a tooltip, the kept keys")
    void commands() {
        MarkdownView documentView = new MarkdownView("# Notes");
        Button saveButton = new Button("Save");
        MenuItem saveItem = new MenuItem("Save");
        Scene scene = new Scene(new VBox(saveButton, documentView));
        List<String> saved = new ArrayList<>();

        CommandRegistry registry = new CommandRegistry(scene);
        CommandGroup file = registry.addGroup("file", "File");
        CommandGroup view = registry.addGroup("view", "View");
        CommandContext inDocument = registry.addContext("document", "Document", documentView);

        Command save = registry.addCommand(file, "file.save", "Save",
                Shortcut.of(KeyCode.S, Shortcut.Modifier.CTRL), () -> saved.add(documentView.getMarkdown()));
        registry.addCommand(view, "view.zoom-in", "Zoom in", Shortcut.of('+', Shortcut.Modifier.CTRL),
                () -> documentView.setZoom(documentView.getZoom() + 0.25));
        registry.addFixedCommand(view, inDocument, "document.copy", "Copy",
                Shortcut.of(KeyCode.C, Shortcut.Modifier.CTRL), null);
        save.bind(saveItem);
        save.bind(saveButton);
        Tooltip saveTip = new Tooltip();
        saveTip.textProperty().bind(Bindings.when(save.shortcutDisplayTextProperty().isEmpty())
                .then("Save")
                .otherwise(Bindings.concat("Save (", save.shortcutDisplayTextProperty(), ")")));
        saveButton.setTooltip(saveTip);

        registry.setChangedShortcuts(storedKeys());
        registry.changedShortcutsProperty().addListener(
                (observable, oldKeys, newKeys) -> storeKeys(newKeys));

        assertEquals("Save (Ctrl+Shift+S)", saveTip.getText(), "the stored key, handed back");
        assertEquals("Ctrl+Shift+S", saveItem.getAccelerator().getDisplayText());
        saveButton.requestFocus();
        saveButton.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.S, true, true, false, false));
        assertEquals(List.of("# Notes"), saved);
        registry.resetShortcuts();
        assertEquals("Save (Ctrl+S)", saveTip.getText());
        assertEquals(Map.of(), keptKeys, "the application heard the keys change and kept them");
    }

    @Test
    @DisplayName("Shortcuts editor: the dialog's content on a page of the application's own, with its own buttons")
    void shortcutsEditor() {
        CommandRegistry registry = new CommandRegistry(new Scene(new VBox()));
        CommandGroup file = registry.addGroup("file", "File");
        registry.addCommand(file, "file.open", "Open", Shortcut.of(KeyCode.O, Shortcut.Modifier.CTRL), () -> { });

        ShortcutsEditor editor = new ShortcutsEditor(registry);
        Button applyKeys = new Button("Apply");
        applyKeys.disableProperty().bind(editor.hasChangesProperty().not().or(editor.hasConflictsProperty()));
        applyKeys.setOnAction(event -> editor.apply());
        Button discardKeys = new Button("Discard");
        discardKeys.disableProperty().bind(editor.hasChangesProperty().not());
        discardKeys.setOnAction(event -> editor.discard());

        assertTrue(applyKeys.isDisabled(), "nothing changed yet");
        assertTrue(discardKeys.isDisabled());
    }

    /** Stands for the application's own storage of the keys the user changed. */
    private static Map<String, String> storedKeys() {
        return Map.of("file.save", "Ctrl+Shift+S");
    }

    /** What the application kept last, by {@link #storeKeys(Map)}. */
    private Map<String, String> keptKeys;

    /** Stands for the application's own storage of the keys the user changed. */
    private void storeKeys(Map<String, String> keys) {
        keptKeys = keys;
    }

    /** Compiled only: the dialog is a window. */
    @SuppressWarnings("unused")
    private static void shortcutsDialog(Window owner, CommandRegistry registry) {
        MenuItem keyboardShortcuts = new MenuItem("Keyboard shortcuts");
        keyboardShortcuts.setOnAction(event -> new ShortcutsDialog(owner, registry).showAndWait());
    }

    @Test
    @DisplayName("Controls: toggle switch, twin column selector, key-value editor")
    void controls() {
        ToggleSwitch darkMode = new ToggleSwitch(true);
        List<Boolean> switched = new ArrayList<>();
        darkMode.selectedProperty().addListener((observable, wasOn, isOn) -> switched.add(isOn));

        TwinColumnSelector<String> columns = new TwinColumnSelector<>();
        columns.setAvailableHeader("Available");
        columns.setSelectedHeader("Shown");
        columns.setItems(List.of("Name", "City", "Phone"), List.of("Name"));
        List<String> shownColumns = columns.getSelectedItems();

        KeyValueEditor headers = new KeyValueEditor();
        headers.setTitle("Headers");
        headers.getEntries().add(new KeyValueEntry("Accept", "text/csv"));
        List<KeyValueEntry.Snapshot> pairs = headers.snapshot();

        darkMode.setSelected(false);
        assertEquals(List.of(false), switched);
        assertEquals(List.of("Name"), shownColumns);
        assertEquals("Accept", pairs.get(0).key());
    }

    @Test
    @DisplayName("Settings: the model an application builds, and its values")
    void settingsModel() {
        SettingsModel model = sampleSettings();
        Map<String, Object> values = model.snapshot();

        assertEquals(13, values.get("editor.font-size"));
        assertEquals(true, values.get("editor.line-numbers"));
    }

    /** The settings model of the guide's sample. */
    private static SettingsModel sampleSettings() {
        SettingEntry<Integer> fontSize = SettingEntry.ofInteger("editor.font-size", "Font size", 13, 8, 32, 1);
        SettingEntry<Boolean> lineNumbers = SettingEntry.ofBoolean("editor.line-numbers", "Line numbers", true);
        SettingEntry<String> density = SettingEntry.ofChoice("editor.density", "Density", "Compact",
                List.of("Compact", "Comfortable"));
        SettingGroup editorGroup = new SettingGroup("Editor", List.of(fontSize, lineNumbers, density));
        SettingCategory editor = new SettingCategory("editor", "Editor", List.of(editorGroup), List.of());
        // No resource bundle: the label keys are shown as they are.
        return new SettingsModel(List.of(editor), null);
    }

    /** Compiled only: a settings dialog is a window. */
    @SuppressWarnings("unused")
    private static void settingsDialog(Window owner) {
        SettingEntry<Integer> fontSize = SettingEntry.ofInteger("editor.font-size", "Font size", 13, 8, 32, 1);
        SettingEntry<Boolean> lineNumbers = SettingEntry.ofBoolean("editor.line-numbers", "Line numbers", true);
        SettingEntry<String> density = SettingEntry.ofChoice("editor.density", "Density", "Compact",
                List.of("Compact", "Comfortable"));
        SettingGroup editorGroup = new SettingGroup("Editor", List.of(fontSize, lineNumbers, density));
        SettingCategory editor = new SettingCategory("editor", "Editor", List.of(editorGroup), List.of());
        // No resource bundle: the label keys are shown as they are.
        SettingsModel model = new SettingsModel(List.of(editor), null);

        SettingsDialogResult result = new SettingsDialog.Builder()
                .owner(owner)
                .title("Settings")
                .model(model)
                .onApply(() -> store(model.snapshot()))
                .build()
                .showAndWait();
        if (result == SettingsDialogResult.OK) {
            store(model.snapshot());
        }
    }

    /** Stands for the application's own storage. */
    private static void store(Map<String, Object> values) {
    }

    /** Compiled only: dialogs are windows. */
    @SuppressWarnings("unused")
    private static void dialogs(Window owner) {
        CustomDialog.info(owner, "Export", "The export is finished.");
        CustomDialog.error(owner, "Export", "The export failed", "The file is in use by another program.");
        boolean delete = CustomDialog.confirm(owner, "Delete", "Delete the job \"Nightly export\"?");
        Optional<String> name = CustomDialog.input(owner, "Rename", "New name", "Nightly export");

        CustomDialog.DialogResult answer = new CustomDialog.Builder(CustomDialog.DialogType.CONFIRMATION)
                .owner(owner)
                .title("Unsaved changes")
                .headerText("Save the changes before closing?")
                .contentText("Your changes are lost if you do not save them.")
                .yesButtonText("Save")
                .noButtonText("Discard")
                .build()
                .showAndWait();
    }

    /** Compiled only: the start of an application, with the window's place kept between runs. */
    @SuppressWarnings("unused")
    private static void applicationStart(Stage stage, ConfigService config) {
        Scene scene = new Scene(new VBox(), 1000, 700);
        ThemeManager.apply(scene);
        config.getConfig().windowState().applyTo(stage);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            config.setWindowState(WindowState.fromStage(stage));
            config.shutdown();
        });
        stage.show();
    }
}
