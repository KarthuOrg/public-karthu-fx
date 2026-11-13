package org.karthu.fx.form;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.altKeyPressed;
import static org.karthu.fx.form.FormTestSupport.fill;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.tokenSwatch;

/**
 * The path input through its public API, without a window: no dialog is opened, a recording chooser stands in for it.
 */
class PathInputTest {

    @TempDir
    Path temp;

    private PathInput field;
    private RecordingChooser chooser;
    private DirectoryMemory memory;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createField() {
        field = new PathInput();
        chooser = new RecordingChooser();
        field.setChooser(chooser);
        // A memory of the field's own: no test leaves a last directory in the shared memory for the next one.
        memory = DirectoryMemory.inMemory();
        field.setDirectoryMemory(memory);
    }

    @Test
    @DisplayName("the browse icon asks the chooser and enters its choice absolute into the value")
    void browseEntersTheChoice() throws IOException {
        Path chosen = Files.createFile(temp.resolve("input.csv"));
        chooser.answer = Optional.of(chosen);

        browseButton().fire();

        assertEquals(1, chooser.requests.size());
        assertEquals(chosen.toAbsolutePath().toString(), field.getValue());
        assertEquals(field.getValue(), field.getTextField().getText());
    }

    @Test
    @DisplayName("open mode: the request has the mode, the title, the field's folder, no file name and no filters")
    void openRequest() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        field.setValue(folder.resolve("input.csv").toString());
        field.setTitle("Choose the input");

        browseButton().fire();

        PathChooser.Request request = chooser.requests.getFirst();
        assertEquals(PathMode.OPEN, request.mode());
        assertEquals("Choose the input", request.title());
        assertEquals(folder, request.startDirectory());
        assertNull(request.initialFileName());
        assertTrue(request.filters().isEmpty());
        assertNull(request.owner());
    }

    @Test
    @DisplayName("save mode: the request has the save mode and starts in the folder of the new file")
    void saveRequest() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("out"));
        field.setMode(PathMode.SAVE);
        field.setValue(folder.resolve("result.csv").toString());

        browseButton().fire();

        PathChooser.Request request = chooser.requests.getFirst();
        assertEquals(PathMode.SAVE, request.mode());
        assertEquals(folder, request.startDirectory());
        assertNull(request.title());
    }

    @Test
    @DisplayName("directory mode: the request starts in the directory itself, or its nearest existing ancestor")
    void directoryRequest() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data").resolve("2026"));
        field.setMode(PathMode.DIRECTORY);

        field.setValue(folder.toString());
        browseButton().fire();
        field.setValue(folder.resolve("missing").resolve("deeper").toString());
        browseButton().fire();

        assertEquals(PathMode.DIRECTORY, chooser.requests.get(0).mode());
        assertEquals(folder, chooser.requests.get(0).startDirectory());
        assertEquals(folder, chooser.requests.get(1).startDirectory());
    }

    @Test
    @DisplayName("an empty field starts the dialog in the user's home directory")
    void emptyFieldStartsAtHome() {
        browseButton().fire();

        assertEquals(Path.of(System.getProperty("user.home")), chooser.requests.getFirst().startDirectory());
    }

    @Test
    @DisplayName("a chosen directory is entered absolute in the directory mode")
    void directoryChoiceIsEntered() throws IOException {
        Path chosen = Files.createDirectories(temp.resolve("data"));
        field.setMode(PathMode.DIRECTORY);
        chooser.answer = Optional.of(chosen);

        browseButton().fire();

        assertEquals(chosen.toAbsolutePath().toString(), field.getValue());
    }

    @Test
    @DisplayName("a cancelled dialog leaves the value as it was")
    void cancelLeavesTheValue() {
        field.setValue("typed by hand");
        chooser.answer = Optional.empty();

        browseButton().fire();

        assertEquals(1, chooser.requests.size());
        assertEquals("typed by hand", field.getValue());
    }

    @Test
    @DisplayName("Alt+Down in the field opens the chooser and is consumed; Down alone does not")
    void altDownOpensTheChooser() {
        KeyEvent altDown = altKeyPressed(KeyCode.DOWN);
        field.getTextField().fireEvent(altDown);
        assertEquals(1, chooser.requests.size());

        field.getTextField().fireEvent(keyPressed(KeyCode.DOWN));
        assertEquals(1, chooser.requests.size());
    }

    @Test
    @DisplayName("a disabled field opens no chooser, neither by the icon nor by Alt+Down")
    void disabledFieldDoesNotBrowse() {
        field.setDisable(true);

        browseButton().fire();
        field.getTextField().fireEvent(altKeyPressed(KeyCode.DOWN));

        assertTrue(chooser.requests.isEmpty());
        assertTrue(browseButton().isDisabled());
    }

    @Test
    @DisplayName("the value is the text exactly as typed, both ways, and starts empty")
    void valueIsTheText() {
        assertEquals("", field.getValue());

        field.getTextField().setText("  files/data.csv ");
        assertEquals("  files/data.csv ", field.getValue());

        field.setValue("C:/other.csv");
        assertEquals("C:/other.csv", field.getTextField().getText());
        assertSame(field.valueProperty(), ((FormInput<String>) field).valueProperty());
        assertSame(field, field.getNode());
    }

    @Test
    @DisplayName("toPath gives the text as a path, empty for empty or invalid text, relative paths kept relative")
    void toPathTakesTheTextAsItIs() {
        assertTrue(field.toPath().isEmpty());

        field.setValue("bad\u0000path");
        assertTrue(field.toPath().isEmpty());

        field.setValue("files/data.csv");
        assertEquals(Optional.of(Path.of("files/data.csv")), field.toPath());
    }

    @Test
    @DisplayName("the prompt text falls back to the mode's default while null")
    void promptTextPerMode() {
        assertEquals("Select a file", field.getTextField().getPromptText());

        field.setMode(PathMode.SAVE);
        assertEquals("Select a file", field.getTextField().getPromptText());

        field.setMode(PathMode.DIRECTORY);
        assertEquals("Select a folder", field.getTextField().getPromptText());

        field.setPromptText("Choose the output folder");
        assertEquals("Choose the output folder", field.getTextField().getPromptText());

        field.setPromptText(null);
        assertEquals("Select a folder", field.getTextField().getPromptText());
    }

    @Test
    @DisplayName("style classes: the frame, the inner text field and the browse icon")
    void styleClasses() {
        assertTrue(field.getStyleClass().containsAll(List.of("form-frame", "form-path-input")));
        assertTrue(field.getTextField().getStyleClass().containsAll(List.of("text-field", "form-frame-text")));
        assertTrue(browseButton().getStyleClass().containsAll(
                List.of("icon-button", "form-frame-icon", "form-path-browse")));
        assertSame(browseButton(), field.getIcons().getFirst());
    }

    @Test
    @DisplayName("the mode shows as exactly one of the pseudo-classes :open, :save and :directory")
    void modePseudoClasses() {
        assertEquals(Set.of("open"), shownModes());

        field.setMode(PathMode.SAVE);
        assertEquals(Set.of("save"), shownModes());

        field.setMode(PathMode.DIRECTORY);
        assertEquals(Set.of("directory"), shownModes());

        field.setMode(null);
        assertEquals(Set.of("open"), shownModes());
    }

    @Test
    @DisplayName("the browse icon is out of the tab order, with its tooltip and shortcut from a text property")
    void browseIconTooltip() {
        Button browse = browseButton();
        assertFalse(browse.isFocusTraversable());
        assertEquals("Browse\u2026 (Alt+Down)", browse.getTooltip().getText());
        assertEquals("Browse\u2026 (Alt+Down)", browse.getAccessibleText());

        field.setBrowseTooltipText("Durchsuchen\u2026 (Alt+Nach-unten)");
        assertEquals("Durchsuchen\u2026 (Alt+Nach-unten)", browse.getTooltip().getText());
    }

    @Test
    @DisplayName("the browse icon shows what the field wants: a file in the open and save modes, a folder in directory")
    void browseIconShowsTheMode() {
        assertEquals(BootstrapIcons.FILE_EARMARK, browseGlyph().getIconCode());

        field.setMode(PathMode.SAVE);
        assertEquals(BootstrapIcons.FILE_EARMARK, browseGlyph().getIconCode());

        field.setMode(PathMode.DIRECTORY);
        assertEquals(BootstrapIcons.FOLDER, browseGlyph().getIconCode());
        assertSame(browseGlyph(), browseButton().getGraphic());

        field.setMode(PathMode.OPEN);
        assertEquals(BootstrapIcons.FILE_EARMARK, browseGlyph().getIconCode());

        field.setMode(null);
        assertEquals(BootstrapIcons.FILE_EARMARK, browseGlyph().getIconCode());
    }

    @Test
    @DisplayName("a null chooser counts as the standard one")
    void nullChooserIsTheStandardOne() {
        field.setChooser(null);
        assertNull(field.getChooser());
        assertSame(PathChooser.standard(), new PathInput().getChooser());
    }

    // --- Base directory and directory memory -------------------------------------------------------

    @Test
    @DisplayName("a new field has no base directory and no memory of its own")
    void baseDirectoryAndMemoryDefaults() {
        PathInput fresh = new PathInput();

        assertNull(fresh.getBaseDirectory());
        assertNull(fresh.getDirectoryMemory());
    }

    @Test
    @DisplayName("a choice below the base directory is entered relative with /, the base itself as .")
    void choiceBelowTheBaseIsEnteredRelative() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        field.setBaseDirectory(base);

        chooser.answer = Optional.of(base.resolve("files").resolve("data.csv"));
        browseButton().fire();
        assertEquals("files/data.csv", field.getValue());

        field.setMode(PathMode.DIRECTORY);
        chooser.answer = Optional.of(base);
        browseButton().fire();
        assertEquals(".", field.getValue());
    }

    @Test
    @DisplayName("a choice outside the base directory is entered absolute")
    void choiceOutsideTheBaseIsEnteredAbsolute() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path outside = temp.resolve("other").resolve("data.csv");
        field.setBaseDirectory(base);
        chooser.answer = Optional.of(outside);

        browseButton().fire();

        assertEquals(outside.toString(), field.getValue());
    }

    @Test
    @DisplayName("the request starts in the folder of a relative path, resolved against the base directory")
    void requestStartsInTheFolderOfARelativePath() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path files = Files.createDirectories(base.resolve("files"));
        field.setBaseDirectory(base);
        field.setValue("files/data.csv");

        browseButton().fire();

        assertEquals(files, chooser.requests.getFirst().startDirectory());
    }

    @Test
    @DisplayName("an empty field starts in the memory's last directory, else in the base directory")
    void emptyFieldStartsInTheLastDirectoryElseTheBase() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path last = Files.createDirectories(temp.resolve("last"));
        field.setBaseDirectory(base);

        browseButton().fire();
        memory.remember(last);
        browseButton().fire();

        assertEquals(base, chooser.requests.get(0).startDirectory());
        assertEquals(last, chooser.requests.get(1).startDirectory());
    }

    @Test
    @DisplayName("save mode: the request suggests the file name in the field; open mode suggests none")
    void saveRequestSuggestsTheFileName() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        field.setBaseDirectory(base);
        field.setValue("out/result.csv");

        browseButton().fire();
        field.setMode(PathMode.SAVE);
        browseButton().fire();

        assertNull(chooser.requests.get(0).initialFileName());
        assertEquals("result.csv", chooser.requests.get(1).initialFileName());
    }

    @Test
    @DisplayName("a choice remembers the file's folder, in the directory mode the directory itself")
    void choiceUpdatesTheMemory() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Path directory = Files.createDirectories(temp.resolve("output"));

        chooser.answer = Optional.of(folder.resolve("input.csv"));
        browseButton().fire();
        assertEquals(Optional.of(folder), memory.lastDirectory());

        field.setMode(PathMode.DIRECTORY);
        chooser.answer = Optional.of(directory);
        browseButton().fire();
        assertEquals(Optional.of(directory), memory.lastDirectory());
    }

    @Test
    @DisplayName("typed text and a cancelled dialog leave the memory as it was")
    void typedTextAndCancelDoNotUpdateTheMemory() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));

        field.getTextField().setText(folder.resolve("typed.csv").toString());
        chooser.answer = Optional.empty();
        browseButton().fire();

        assertEquals(Optional.empty(), memory.lastDirectory());
    }

    @Test
    @DisplayName("while its memory is null, the field uses the shared memory at the time of use")
    void nullMemoryUsesTheSharedMemory() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Path last = Files.createDirectories(temp.resolve("last"));
        DirectoryMemory original = DirectoryMemory.getShared();
        try {
            field.setDirectoryMemory(null);
            DirectoryMemory shared = DirectoryMemory.inMemory();
            shared.remember(last);
            DirectoryMemory.setShared(shared);

            chooser.answer = Optional.of(folder.resolve("input.csv"));
            browseButton().fire();

            assertEquals(last, chooser.requests.getFirst().startDirectory());
            assertEquals(Optional.of(folder), shared.lastDirectory());
            assertEquals(Optional.empty(), memory.lastDirectory());
        } finally {
            DirectoryMemory.setShared(original);
        }
    }

    // --- File filters ------------------------------------------------------------------------------

    private static final PathFilter CSV = new PathFilter("CSV files", List.of("*.csv"));
    private static final PathFilter TEXT = new PathFilter("Text files", List.of("*.txt", "*.tsv"));

    @Test
    @DisplayName("open mode: the request carries the filters in their order, then All files with the pattern *")
    void openRequestCarriesTheFilters() {
        field.getFilters().addAll(CSV, TEXT);

        browseButton().fire();

        assertEquals(List.of(CSV, TEXT, new PathFilter("All files", List.of("*"))),
                chooser.requests.getFirst().filters());
    }

    @Test
    @DisplayName("save mode: the request carries the filters, then All files")
    void saveRequestCarriesTheFilters() {
        field.setMode(PathMode.SAVE);
        field.getFilters().add(CSV);

        browseButton().fire();

        assertEquals(List.of(CSV, new PathFilter("All files", List.of("*"))),
                chooser.requests.getFirst().filters());
    }

    @Test
    @DisplayName("directory mode: the request carries no filters, even when the field has some")
    void directoryRequestIgnoresTheFilters() {
        field.setMode(PathMode.DIRECTORY);
        field.getFilters().addAll(CSV, TEXT);

        browseButton().fire();

        assertTrue(chooser.requests.getFirst().filters().isEmpty());
    }

    @Test
    @DisplayName("the text of All files comes from its property, English by default")
    void allFilesText() {
        field.getFilters().add(CSV);
        assertEquals("All files", field.getAllFilesText());

        field.setAllFilesText("Alle Dateien");
        browseButton().fire();

        assertEquals(new PathFilter("Alle Dateien", List.of("*")), chooser.requests.getFirst().filters().getLast());
    }

    @Test
    @DisplayName("a null or empty All files text falls back to the English one, a dialog needs a description")
    void emptyAllFilesTextFallsBack() {
        field.getFilters().add(CSV);

        field.setAllFilesText(null);
        browseButton().fire();
        field.setAllFilesText("");
        browseButton().fire();

        PathFilter allFiles = new PathFilter("All files", List.of("*"));
        assertEquals(allFiles, chooser.requests.get(0).filters().getLast());
        assertEquals(allFiles, chooser.requests.get(1).filters().getLast());
    }

    // --- Drop, paste and long paths ----------------------------------------------------------------
    // The tests make no real drag gesture: they enter prepared files through the field's drop handling, which the
    // field's drag event handlers call with the dragged files.

    @Test
    @DisplayName("a dropped file is entered like a dialog choice: relative below the base, and remembered")
    void dropIsEnteredLikeAChoice() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path folder = Files.createDirectories(base.resolve("files"));
        Path dropped = Files.createFile(folder.resolve("data.csv"));
        field.setBaseDirectory(base);

        boolean entered = field.handleDragDropped(List.of(dropped));

        assertTrue(entered);
        assertEquals("files/data.csv", field.getValue());
        assertEquals(Optional.of(folder), memory.lastDirectory());
        assertTrue(chooser.requests.isEmpty());
    }

    @Test
    @DisplayName("a value the application bound is left alone: a choice or a drop changes nothing and fails nowhere")
    void boundValuePropertyIsLeftAlone() throws IOException {
        Path chosen = Files.createFile(temp.resolve("input.csv"));
        chooser.answer = Optional.of(chosen);
        StringProperty source = new SimpleStringProperty("config.csv");
        field.valueProperty().bind(source);

        browseButton().fire();
        boolean entered = field.handleDragDropped(List.of(chosen));

        assertFalse(entered);
        assertEquals("config.csv", field.getValue());
        assertEquals("config.csv", field.getTextField().getText());
        assertEquals(Optional.empty(), memory.lastDirectory());
        source.set("other.csv");
        assertEquals("other.csv", field.getTextField().getText());
    }

    @Test
    @DisplayName("directory mode: a dropped directory is entered absolute outside the base and remembered itself")
    void droppedDirectoryIsEntered() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path dropped = Files.createDirectories(temp.resolve("output"));
        field.setBaseDirectory(base);
        field.setMode(PathMode.DIRECTORY);

        assertTrue(field.handleDragDropped(List.of(dropped)));

        assertEquals(dropped.toString(), field.getValue());
        assertEquals(Optional.of(dropped), memory.lastDirectory());
    }

    @Test
    @DisplayName("a refused drop changes neither value nor memory: wrong kind, several entries, disabled field")
    void refusedDropChangesNothing() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        Path other = Files.createFile(temp.resolve("other.csv"));
        Path directory = Files.createDirectories(temp.resolve("data"));
        field.setValue("typed by hand");

        assertFalse(field.handleDragDropped(List.of(directory)));
        assertFalse(field.handleDragDropped(List.of(file, other)));
        assertFalse(field.handleDragDropped(List.of()));
        field.setMode(PathMode.DIRECTORY);
        assertFalse(field.handleDragDropped(List.of(file)));
        field.setDisable(true);
        assertFalse(field.handleDragDropped(List.of(directory)));

        assertEquals("typed by hand", field.getValue());
        assertEquals(Optional.empty(), memory.lastDirectory());
    }

    @Test
    @DisplayName("an acceptable drag shows :drop-target on the frame until it leaves; the drag changes no value")
    void acceptableDragShowsTheDropTarget() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        assertFalse(isDropTarget());

        assertTrue(field.handleDragOver(List.of(file)));
        assertTrue(isDropTarget());
        assertEquals("", field.getValue());

        field.handleDragExited();
        assertFalse(isDropTarget());
    }

    @Test
    @DisplayName("a refused drag shows nothing: wrong kind, several entries, nothing, disabled field")
    void refusedDragShowsNothing() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        Path other = Files.createFile(temp.resolve("other.csv"));
        Path directory = Files.createDirectories(temp.resolve("data"));

        assertFalse(field.handleDragOver(List.of(directory)));
        assertFalse(isDropTarget());
        assertFalse(field.handleDragOver(List.of(file, other)));
        assertFalse(field.handleDragOver(List.of()));
        assertFalse(isDropTarget());

        field.setDisable(true);
        assertFalse(field.handleDragOver(List.of(file)));
        assertFalse(isDropTarget());
    }

    @Test
    @DisplayName("a bound field takes no drop and shows nothing for the drag; the hidden path is kept")
    void boundFieldTakesNoDrop() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        field.setValue("files/kept.csv");
        field.getBinding().setBoundName("inputFile");

        assertFalse(field.handleDragOver(List.of(file)));
        assertFalse(isDropTarget());
        assertFalse(field.handleDragDropped(List.of(file)));

        assertEquals("files/kept.csv", field.getValue());
        assertEquals(Optional.empty(), memory.lastDirectory());
    }

    @Test
    @DisplayName("a refused drag and a refused drop pass the field unconsumed, so a container around it may take them")
    void refusedDropReachesTheContainer() {
        // A gesture without files, as one that brings text: the field refuses it.
        List<Event> passedOver = firesThrough(field, dragEvent(DragEvent.DRAG_OVER));
        List<Event> passedDrop = firesThrough(field, dragEvent(DragEvent.DRAG_DROPPED));

        assertEquals(1, passedOver.size());
        assertEquals(1, passedDrop.size());
        assertFalse(((DragEvent) passedDrop.getFirst()).isDropCompleted());
        assertEquals("", field.getValue());
    }

    /** A drag event of a gesture whose dragboard is not at hand: a test cannot make a dragboard. */
    private static DragEvent dragEvent(EventType<DragEvent> type) {
        return new DragEvent(type, null, 1, 1, 1, 1, TransferMode.COPY, null, null, null);
    }
    @Test
    @DisplayName("a drop ends the drop-target look, entered or refused")
    void dropEndsTheDropTarget() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));

        field.handleDragOver(List.of(file));
        field.handleDragDropped(List.of(file));

        assertFalse(isDropTarget());
    }

    @Test
    @DisplayName("a paste of a quoted path, as Copy as path gives it, inserts the path without its quotes")
    void quotedPasteIsUnquoted() {
        field.setClipboard(new RecordingClipboard("\"C:\\data\\input.csv\"\r\n"));

        field.getTextField().paste();

        assertEquals("C:\\data\\input.csv", field.getValue());
    }

    @Test
    @DisplayName("a paste replaces the selection; any other paste is inserted unchanged")
    void otherPasteIsUnchanged() {
        field.setValue("files/old.csv");
        field.getTextField().selectRange(6, 9);
        field.setClipboard(new RecordingClipboard("new \"one\""));

        field.getTextField().paste();

        assertEquals("files/new \"one\".csv", field.getValue());
    }

    @Test
    @DisplayName("typed and pasted text never updates the directory memory")
    void typedAndPastedTextIsNotRemembered() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Files.createFile(folder.resolve("input.csv"));
        field.setClipboard(new RecordingClipboard("\"" + folder.resolve("input.csv") + "\""));

        field.getTextField().paste();
        field.getTextField().appendText("x");

        assertEquals(folder.resolve("input.csv") + "x", field.getValue());
        assertEquals(Optional.empty(), memory.lastDirectory());
    }

    @Test
    @DisplayName("an empty clipboard pastes nothing")
    void emptyClipboardPastesNothing() {
        field.setValue("kept");
        field.setClipboard(new RecordingClipboard(null));

        field.getTextField().paste();

        assertEquals("kept", field.getValue());
    }

    @Test
    @DisplayName("the text's tooltip shows the whole value while it is not empty")
    void tooltipShowsTheWholeValue() {
        String path = longPath();
        assertNull(field.getTextField().getTooltip());

        field.setValue(path);
        assertEquals(path, field.getTextField().getTooltip().getText());

        field.getTextField().setText("files/data.csv");
        assertEquals("files/data.csv", field.getTextField().getTooltip().getText());

        field.setValue("");
        assertNull(field.getTextField().getTooltip());
    }

    @Test
    @DisplayName("a value change without focus puts the caret at the end")
    void valueChangeWithoutFocusPutsTheCaretAtTheEnd() {
        String path = longPath();

        field.setValue(path);

        assertFalse(field.getTextField().isFocused());
        assertEquals(path.length(), field.getTextField().getCaretPosition());
        assertEquals(path.length(), field.getTextField().getAnchor());
    }

    @Test
    @DisplayName("a narrow field shows the end of a long path, its file name, after a value change without focus")
    void narrowFieldShowsTheFileName() {
        field.setPrefWidth(160);
        field.setMaxWidth(160);
        layOut(field);

        field.setValue(longPath());
        field.getParent().layout();

        Bounds visible = field.getTextField().localToScene(field.getTextField().getLayoutBounds());
        Bounds text = shownText().localToScene(shownText().getLayoutBounds());
        assertTrue(text.getMinX() < visible.getMinX(), "the start of the path is scrolled out");
        assertTrue(text.getMaxX() <= visible.getMaxX(), "the end of the path is visible");
    }

    @Test
    @DisplayName("a choice and a drop put the caret at the end")
    void choiceAndDropPutTheCaretAtTheEnd() throws IOException {
        Path chosen = Files.createFile(temp.resolve("chosen.csv"));
        chooser.answer = Optional.of(chosen);

        browseButton().fire();
        assertEquals(field.getValue().length(), field.getTextField().getCaretPosition());

        Path dropped = Files.createFile(temp.resolve("dropped-file.csv"));
        field.handleDragDropped(List.of(dropped));
        assertEquals(field.getValue().length(), field.getTextField().getCaretPosition());
    }

    @Test
    @DisplayName("the theme draws the drop target with the accent border")
    void dropTargetBorderInTheTheme() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        PathInput resting = new PathInput();
        field.handleDragOver(List.of(file));
        Region accent = tokenSwatch("-fx-accent-color");
        Region border = tokenSwatch("-fx-control-border");
        layOut(field, resting, accent, border);

        assertEquals(fill(accent), fill(field));
        assertEquals(fill(border), fill(resting));
    }

    @Test
    @DisplayName("during a drag the accent border replaces the error and warning borders, also with the focus")
    void dropTargetBorderBeatsTheStateBorder() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        PathInput error = new PathInput();
        error.setState(FieldState.ERROR);
        PathInput warning = new PathInput();
        warning.setState(FieldState.WARNING);
        PathInput focusedError = new PathInput();
        focusedError.setState(FieldState.ERROR);
        // :focus-within is set by the toolkit only in a shown, focused window; the test sets it as the toolkit would.
        focusedError.pseudoClassStateChanged(PseudoClass.getPseudoClass("focus-within"), true);
        for (PathInput dragged : List.of(error, warning, focusedError)) {
            dragged.setDirectoryMemory(DirectoryMemory.inMemory());
            assertTrue(dragged.handleDragOver(List.of(file)));
        }
        Region accent = tokenSwatch("-fx-accent-color");
        layOut(error, warning, focusedError, accent);

        assertEquals(fill(accent), fill(error));
        assertEquals(fill(accent), fill(warning));
        assertEquals(fill(accent), fill(focusedError));
    }

    /** The text node that shows the field's value inside its text field. */
    private Text shownText() {
        for (Node node : field.getTextField().lookupAll(".text")) {
            if (node instanceof Text text && field.getValue().equals(text.getText())) {
                return text;
            }
        }
        throw new AssertionError("no text node shows the value");
    }

    /** A path longer than a narrow field shows, ending in its file name. */
    private String longPath() {
        return temp.resolve("workflows").resolve("customers").resolve("2026").resolve("november")
                .resolve("customers-2026-11.csv").toString();
    }

    private boolean isDropTarget() {
        return hasPseudoClass(field, "drop-target");
    }

    private Set<String> shownModes() {
        Set<String> names = new HashSet<>();
        for (PseudoClass pseudoClass : field.getPseudoClassStates()) {
            String name = pseudoClass.getPseudoClassName();
            if (name.equals("open") || name.equals("save") || name.equals("directory")) {
                names.add(name);
            }
        }
        return names;
    }

    private Button browseButton() {
        return (Button) field.lookup(".form-path-browse");
    }

    private FontIcon browseGlyph() {
        return (FontIcon) browseButton().getGraphic();
    }

    /** A chooser that opens nothing: it records each request and returns a prepared answer. */
    private static final class RecordingChooser implements PathChooser {

        private final List<Request> requests = new ArrayList<>();
        private Optional<Path> answer = Optional.empty();

        @Override
        public Optional<Path> choose(Request request) {
            requests.add(request);
            return answer;
        }
    }
}
