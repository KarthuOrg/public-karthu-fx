package org.karthu.fx.form;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.Window;
import org.karthu.fx.form.path.PathRules;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A field for choosing a file or a directory: a text field with a browse icon inside its frame that opens the file or
 * directory dialog.
 *
 * <p><b>Browse icon.</b> It shows what the field wants: a file (Bootstrap {@code FILE_EARMARK}) in the open and save
 * modes, a folder (Bootstrap {@code FOLDER}) in the directory mode, and it changes with the mode.
 *
 * <p><b>Value.</b> {@link #valueProperty()} is the text exactly as it stands in the field: what the user types stays as
 * typed, nothing typed is refused, and the empty text is no value. {@link #toPath()} gives the text as a {@code Path}
 * when it is one; invalid text is the application's to report.
 *
 * <p><b>Choosing.</b> The browse icon, or Alt+Down in the field, opens the {@linkplain #chooserProperty() chooser} in
 * the field's {@linkplain #modeProperty() mode}: a dialog for opening a file (the default), for saving a file, or for
 * choosing a directory. Not while the field is disabled, read-only or bound. Saving suggests the file name of the path
 * in the field.
 *
 * <p><b>Base directory and memory.</b> A chosen path below the {@linkplain #baseDirectoryProperty() base directory}
 * is entered relative to it with {@code /} (such as {@code files/data.csv}), the base itself as {@code .}; any other
 * path absolute, as the platform writes it. Each choice is remembered in the field's
 * {@linkplain #directoryMemoryProperty() directory memory} (the {@linkplain DirectoryMemory#getShared() shared one}
 * while it is null): a chosen directory itself, a chosen file its folder, as the file system says; a path that does
 * not exist yet (a new file to save) its folder, in the directory mode the path itself. The dialog starts
 * in the first of these that gives a folder: the folder of the path in the field (in the directory mode the path
 * itself; a relative path resolved against the base directory, skipped without one), the memory's last directory,
 * either of them replaced by its nearest existing ancestor when it no longer exists; the base directory, if it exists;
 * the user's home directory.
 *
 * <p><b>File filters.</b> In the open and save modes the dialog lists the field's {@linkplain #getFilters() filters}
 * in their order, the first selected, and <i>All files</i> (pattern {@code *}, text from
 * {@link #allFilesTextProperty()}) as the last, so that a file with an unexpected extension can still be chosen.
 * Without filters the dialog shows every file, and the directory mode ignores them.
 *
 * <p><b>Drop.</b> A file dragged from the file manager onto the field, in the directory mode a directory, is entered
 * exactly like a dialog choice: relative below the base directory, else absolute, and remembered. The field accepts
 * exactly one existing entry of its mode's kind, whatever the filters say, and not while it is disabled, read-only or
 * bound; while it accepts the drag, the frame shows the accent border (the pseudo-class {@code :drop-target}), a
 * refused drag shows nothing. The drop copies the path, never the file: the field accepts it as a copy only. A refused
 * drag or drop passes the field unconsumed, so a container around it may still take it.
 *
 * <p><b>Paste.</b> A path pasted in one pair of double quotes, as the Windows Explorer's <i>Copy as path</i> gives it,
 * is inserted without the quotes and the whitespace around them; any other paste is inserted unchanged. Typed and
 * pasted text is never remembered in the directory memory.
 *
 * <p><b>Long paths.</b> When the field loses the focus, or its value changes while it has none, and after each choice
 * or drop, the caret goes to the end, so a narrow field shows the end of a long path, its file name. The text's
 * tooltip shows the whole value while it is not empty.
 *
 * <p><b>Binding.</b> The field can take its value from a variable through its {@linkplain #getBinding() binding},
 * shown by its shell (see {@link FieldShell}): the {@code {}} icon after the browse icon, the chip in place of the
 * path while bound. While bound the field disables its browse icon (the frame leaves an icon's disabling to whoever
 * added it), Alt+Down opens nothing and a drop is refused; the path is kept, hidden, and shows again after unbinding.
 *
 * <p><b>Read-only.</b> A {@linkplain #readOnlyProperty() read-only} field (see {@link FieldShell}) shows its path to
 * select and copy, without the folder and {@code {}} icons: the dialog does not open, by neither icon nor Alt+Down, a
 * drop is refused and shows nothing, and a paste inserts nothing. A path the application sets still shows, its end
 * visible as always.
 *
 * <p><b>Texts</b>, properties with English defaults: the prompt text, which falls back to <i>Select a file</i> (open,
 * save) or <i>Select a folder</i> (directory) while it is null, the dialog's title (null: the platform's own) and the
 * browse icon's tooltip <i>Browse&hellip; (Alt+Down)</i>.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The field carries {@code form-path-input} beside the frame's {@code form-frame} (see
 * {@link FieldShell} for the shell's classes), and the pseudo-class of its mode, {@code :open}, {@code :save} or
 * {@code :directory}; while it accepts a drag, the frame's {@code :drop-target}. The browse icon carries
 * {@code form-path-browse} beside the frame's {@code form-frame-icon}.
 *
 * <p>Like every karthu-fx control, the field is used on the JavaFX Application Thread only.
 */
public class PathInput extends FieldFrame implements FormInput<String> {

    private static final PseudoClass OPEN_PSEUDO_CLASS = PseudoClass.getPseudoClass("open");
    private static final PseudoClass SAVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("save");
    private static final PseudoClass DIRECTORY_PSEUDO_CLASS = PseudoClass.getPseudoClass("directory");

    /** The shortcut of the browse icon, pressed in the field. */
    private static final KeyCombination BROWSE_SHORTCUT = new KeyCodeCombination(KeyCode.DOWN, KeyCombination.ALT_DOWN);

    private static final String SELECT_FILE_TEXT = "Select a file";
    private static final String SELECT_FOLDER_TEXT = "Select a folder";

    private static final String ALL_FILES_TEXT = "All files";
    /** The pattern of the <i>All files</i> filter: every file, also one without an extension. */
    private static final String ALL_FILES_PATTERN = "*";

    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final ObjectProperty<PathMode> mode = new SimpleObjectProperty<>(this, "mode", PathMode.OPEN);
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText");
    private final StringProperty title = new SimpleStringProperty(this, "title");
    private final ObjectProperty<PathChooser> chooser =
            new SimpleObjectProperty<>(this, "chooser", PathChooser.standard());
    private final StringProperty browseTooltipText =
            new SimpleStringProperty(this, "browseTooltipText", "Browse\u2026 (Alt+Down)");

    // Base directory and directory memory
    private final ObjectProperty<Path> baseDirectory = new SimpleObjectProperty<>(this, "baseDirectory");
    private final ObjectProperty<DirectoryMemory> directoryMemory =
            new SimpleObjectProperty<>(this, "directoryMemory");

    private final Button browseButton = new Button();
    /** The browse icon's glyph: a file or a folder, by the mode. */
    private final FontIcon browseGlyph = FontIcon.of(BootstrapIcons.FILE_EARMARK);

    private final ObservableList<PathFilter> filters = FXCollections.observableArrayList();
    private final StringProperty allFilesText = new SimpleStringProperty(this, "allFilesText", ALL_FILES_TEXT);

    /** The text's tooltip with the whole value, set on the text field while the value is not empty. */
    private final Tooltip valueTooltip = new Tooltip();

    /**
     * Creates an empty field in the open mode, with the standard chooser and English texts.
     */
    public PathInput() {
        getStyleClass().add("form-path-input");

        value.bindBidirectional(getTextField().textProperty());
        getTextField().promptTextProperty().bind(Bindings.createStringBinding(
                this::shownPromptText, promptText, mode));

        browseButton.getStyleClass().add("form-path-browse");
        browseButton.setGraphic(browseGlyph);
        Tooltip browseTooltip = new Tooltip();
        browseTooltip.textProperty().bind(browseTooltipText);
        browseButton.setTooltip(browseTooltip);
        browseButton.accessibleTextProperty().bind(browseTooltipText);
        browseButton.setOnAction(event -> browse());
        getIcons().add(browseButton);
        // The path is not used while bound: the browse icon, which acts on it, is disabled. The frame leaves the
        // disabling of the icons in getIcons() to whoever added them.
        getBinding().boundNameProperty().addListener(
                (observable, oldName, newName) -> browseButton.setDisable(getBinding().isBound()));

        addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
        mode.addListener((observable, oldMode, newMode) -> updateModeLook());
        updateModeLook();

        // Drop, paste and long paths
        addEventHandler(DragEvent.DRAG_OVER, this::onDragOver);
        addEventHandler(DragEvent.DRAG_EXITED, event -> handleDragExited());
        addEventHandler(DragEvent.DRAG_DROPPED, this::onDragDropped);
        valueTooltip.textProperty().bind(value);
        // Registered after the binding to the text field, so the text is in place when it runs.
        value.addListener((observable, oldValue, newValue) -> valueChanged());
        getTextField().focusedProperty().addListener((observable, wasFocused, focused) -> focusChanged(focused));
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
     * The field's value: the text exactly as it stands in the field, the empty text when there is none. Setting it
     * sets the text.
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
     * Returns the field's text as a path: empty when the text is empty or not a valid path on this platform. The
     * text is taken as it is, neither trimmed nor resolved, so a relative path stays relative.
     *
     * @return the text as a path, or empty
     */
    public Optional<Path> toPath() {
        return PathRules.parse(value.get());
    }

    // --- Properties ----------------------------------------------------------------------------------

    /**
     * What the field chooses (default {@link PathMode#OPEN}): a file to open, a file to save or a directory. Null
     * counts as {@code OPEN}.
     *
     * @return the mode property
     */
    public ObjectProperty<PathMode> modeProperty() {
        return mode;
    }

    /**
     * Returns the field's mode.
     *
     * @return the mode
     */
    public PathMode getMode() {
        return mode.get();
    }

    /**
     * Sets the field's mode.
     *
     * @param value the new mode; null counts as {@code OPEN}
     */
    public void setMode(PathMode value) {
        mode.set(value);
    }

    /**
     * The text the empty field shows (default null). While null, the field shows its mode's default: <i>Select a
     * file</i> in the open and save modes, <i>Select a folder</i> in the directory mode.
     *
     * @return the prompt text property
     */
    public StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Returns the prompt text, null when the mode's default is shown.
     *
     * @return the prompt text, or null
     */
    public String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the prompt text; null shows the mode's default.
     *
     * @param value the prompt text, or null
     */
    public void setPromptText(String value) {
        promptText.set(value);
    }

    /**
     * The dialog's title (default null: the platform's own title).
     *
     * @return the title property
     */
    public StringProperty titleProperty() {
        return title;
    }

    /**
     * Returns the dialog's title.
     *
     * @return the title, or null
     */
    public String getTitle() {
        return title.get();
    }

    /**
     * Sets the dialog's title.
     *
     * @param value the title, or null for the platform's own
     */
    public void setTitle(String value) {
        title.set(value);
    }

    /**
     * The chooser the browse icon and Alt+Down open (default {@link PathChooser#standard()}, JavaFX's file and
     * directory dialogs). Null counts as the standard chooser. Replace it to offer another dialog, or in a test, so
     * that no dialog opens.
     *
     * @return the chooser property
     */
    public ObjectProperty<PathChooser> chooserProperty() {
        return chooser;
    }

    /**
     * Returns the chooser.
     *
     * @return the chooser, or null for the standard one
     */
    public PathChooser getChooser() {
        return chooser.get();
    }

    /**
     * Sets the chooser.
     *
     * @param value the chooser; null counts as the standard one
     */
    public void setChooser(PathChooser value) {
        chooser.set(value);
    }

    /**
     * The browse icon's tooltip and accessible text, its shortcut included (default <i>Browse&hellip;
     * (Alt+Down)</i>). An application that translates it names the shortcut in its own words.
     *
     * @return the browse tooltip text property
     */
    public StringProperty browseTooltipTextProperty() {
        return browseTooltipText;
    }

    /**
     * Returns the browse icon's tooltip.
     *
     * @return the browse icon's tooltip
     */
    public String getBrowseTooltipText() {
        return browseTooltipText.get();
    }

    /**
     * Sets the browse icon's tooltip.
     *
     * @param value the browse icon's tooltip
     */
    public void setBrowseTooltipText(String value) {
        browseTooltipText.set(value);
    }

    // --- Base directory and directory memory ---------------------------------------------------------

    /**
     * The directory the field's paths are relative to (default null: none), usually the application's project or
     * workspace folder; an absolute path. A chosen path below it is entered relative to it with {@code /}, the base
     * itself as {@code .}, and a relative path in the field is resolved against it to find the dialog's start folder.
     * A relative path counts as none. The base never changes what the user typed.
     *
     * @return the base directory property
     */
    public ObjectProperty<Path> baseDirectoryProperty() {
        return baseDirectory;
    }

    /**
     * Returns the base directory.
     *
     * @return the base directory, or null for none
     */
    public Path getBaseDirectory() {
        return baseDirectory.get();
    }

    /**
     * Sets the base directory.
     *
     * @param value an absolute directory, or null for none
     */
    public void setBaseDirectory(Path value) {
        baseDirectory.set(value);
    }

    /**
     * The memory of the last used directory (default null). While null, the field uses the application-wide
     * {@linkplain DirectoryMemory#getShared() shared memory}, taken each time the dialog opens or a choice is
     * entered; that is what an application usually wants. Set a memory of the field's own to keep it apart.
     *
     * @return the directory memory property
     */
    public ObjectProperty<DirectoryMemory> directoryMemoryProperty() {
        return directoryMemory;
    }

    /**
     * Returns the field's own directory memory.
     *
     * @return the field's own memory, or null when it uses the shared memory
     */
    public DirectoryMemory getDirectoryMemory() {
        return directoryMemory.get();
    }

    /**
     * Sets the field's own directory memory.
     *
     * @param value the memory, or null for the shared memory
     */
    public void setDirectoryMemory(DirectoryMemory value) {
        directoryMemory.set(value);
    }

    // --- File filters --------------------------------------------------------------------------------

    /**
     * The file filters of the open and save dialogs, such as <i>CSV files</i> with {@code *.csv} (default empty). The
     * dialog lists them in their order with the first selected, and adds <i>All files</i> as the last filter itself,
     * so the application never adds it. Empty: the dialog shows every file, without filters. The directory mode
     * ignores them. No entry may be null.
     *
     * @return the filters, a modifiable list
     */
    public ObservableList<PathFilter> getFilters() {
        return filters;
    }

    /**
     * The text of the <i>All files</i> filter the dialog lists after the field's filters (default <i>All files</i>).
     * Null or empty counts as the default, since a dialog's filter needs a text.
     *
     * @return the all files text property
     */
    public StringProperty allFilesTextProperty() {
        return allFilesText;
    }

    /**
     * Returns the text of the <i>All files</i> filter.
     *
     * @return the text of the <i>All files</i> filter
     */
    public String getAllFilesText() {
        return allFilesText.get();
    }

    /**
     * Sets the text of the <i>All files</i> filter.
     *
     * @param value the text of the <i>All files</i> filter; null or empty counts as the default
     */
    public void setAllFilesText(String value) {
        allFilesText.set(value);
    }

    /**
     * The filters of a request: none in the directory mode or without filters, else the field's filters and
     * <i>All files</i> last.
     */
    private List<PathFilter> requestFilters(PathMode currentMode) {
        if (currentMode == PathMode.DIRECTORY || filters.isEmpty()) {
            return List.of();
        }
        List<PathFilter> result = new ArrayList<>(filters);
        String text = allFilesText.get();
        if (text == null || text.isEmpty()) {
            text = ALL_FILES_TEXT;
        }
        result.add(new PathFilter(text, List.of(ALL_FILES_PATTERN)));
        return result;
    }

    // --- Choosing ------------------------------------------------------------------------------------

    private void handleShortcut(KeyEvent event) {
        if (BROWSE_SHORTCUT.match(event) && canBrowse()) {
            event.consume();
            browse();
        }
    }

    /** Whether the dialog may open: while the user may change the value (not disabled, read-only or bound). */
    private boolean canBrowse() {
        return userMayChangeValue();
    }

    /** Asks the chooser for a path and enters the choice; a cancelled dialog changes nothing. */
    private void browse() {
        if (!canBrowse()) {
            return;
        }
        PathMode currentMode = effectiveMode();
        Path startDirectory = PathRules.startDirectory(value.get(), currentMode, baseDirectory.get(),
                lastDirectory(), homeDirectory());
        String initialFileName = PathRules.initialFileName(value.get(), currentMode, baseDirectory.get());
        PathChooser.Request request = new PathChooser.Request(
                currentMode, title.get(), startDirectory, initialFileName, requestFilters(currentMode), ownerWindow());
        PathChooser current = chooser.get() == null ? PathChooser.standard() : chooser.get();
        Optional<Path> chosen = current.choose(request);
        if (chosen != null && chosen.isPresent()) {
            enterChoice(chosen.get());
        }
    }

    /**
     * Enters a chosen path: relative below the base directory, else absolute, and remembers its directory. The one
     * way a choice reaches the field, so that every kind of choice gives the same result. A value property the
     * application bound is left alone, and nothing is remembered then.
     *
     * @return true when the choice was entered
     */
    private boolean enterChoice(Path chosen) {
        if (!setValueByUser(value, PathRules.choiceText(chosen, baseDirectory.get()))) {
            return false;
        }
        // A choice shows its file name also while the field has the focus.
        showEnd();
        Optional<Path> remembered = PathRules.rememberedDirectory(chosen, effectiveMode());
        if (remembered.isPresent()) {
            effectiveDirectoryMemory().remember(remembered.get());
        }
        return true;
    }

    // --- Drop ----------------------------------------------------------------------------------------
    // The drag event handlers only unpack the dragged files and call the three handle methods, which hold the field's
    // drop behaviour. They are package-private: the tests' seam, since a test cannot make a drag gesture.

    /**
     * A drag over the field: shows {@code :drop-target} while the field accepts the dragged entries.
     *
     * @param entries the dragged file system entries
     * @return true when the field accepts them
     */
    boolean handleDragOver(List<Path> entries) {
        boolean accepted = acceptsDrop(entries);
        setDropTarget(accepted);
        return accepted;
    }

    /** The drag left the field: the drop-target look ends. */
    void handleDragExited() {
        setDropTarget(false);
    }

    /**
     * A drop on the field: enters the dropped entry like a dialog choice when the field accepts it, and ends the
     * drop-target look either way.
     *
     * @param entries the dropped file system entries
     * @return true when the entry was entered
     */
    boolean handleDragDropped(List<Path> entries) {
        handleDragExited();
        if (!acceptsDrop(entries)) {
            return false;
        }
        return enterChoice(entries.getFirst());
    }

    /** Whether the field takes the entries: the drop rule, while the user may change the value. */
    private boolean acceptsDrop(List<Path> entries) {
        return userMayChangeValue() && PathRules.acceptsDrop(entries, effectiveMode());
    }

    private void onDragOver(DragEvent event) {
        if (handleDragOver(draggedEntries(event.getDragboard()))) {
            // Copy only: the field takes the path, the file stays where it is. A move accepted here would let the
            // source (the Explorer) delete the file after the drop.
            event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        }
    }

    /** An accepted drop is completed and consumed; a refused one passes on, so a container may still take it. */
    private void onDragDropped(DragEvent event) {
        if (handleDragDropped(draggedEntries(event.getDragboard()))) {
            event.setDropCompleted(true);
            event.consume();
        }
    }

    /**
     * The dragged files as paths, empty when the gesture brings no files or a file that is no path on this platform.
     */
    private static List<Path> draggedEntries(Dragboard dragboard) {
        if (dragboard == null || !dragboard.hasFiles()) {
            return List.of();
        }
        List<Path> entries = new ArrayList<>();
        try {
            for (File file : dragboard.getFiles()) {
                entries.add(file.toPath());
            }
        } catch (InvalidPathException invalid) {
            return List.of();
        }
        return entries;
    }

    // --- Paste ---------------------------------------------------------------------------------------

    /**
     * Returns what a paste inserts: a path in one pair of double quotes, as the Windows Explorer's <i>Copy as path</i>
     * gives it, without the quotes and the whitespace around them; any other text unchanged.
     *
     * @param pasted the text on the clipboard
     * @return the text to insert
     */
    @Override
    protected String pastedText(String pasted) {
        return PathRules.pastedText(pasted);
    }

    // --- Long paths ----------------------------------------------------------------------------------

    /**
     * The tooltip shows the whole value while there is one, and a value that changes while the field is not being
     * edited shows its end, the file name.
     */
    private void valueChanged() {
        String text = value.get();
        getTextField().setTooltip(text == null || text.isEmpty() ? null : valueTooltip);
        if (!getTextField().isFocused()) {
            showEnd();
        }
    }

    /**
     * The text field lost the focus: it shows the end of its text. Not when only the window lost the focus (the user
     * switched to another application) and the text field is still the scene's focus owner: the user is still editing
     * and finds the caret where it was on return.
     */
    private void focusChanged(boolean focused) {
        Scene scene = getScene();
        if (!focused && (scene == null || scene.getFocusOwner() != getTextField())) {
            showEnd();
        }
    }

    /** Puts the caret at the end of the text, without a selection: the text field scrolls to show the file name. */
    private void showEnd() {
        getTextField().end();
    }

    /** The memory in use: the field's own, else the shared memory as it is now. */
    private DirectoryMemory effectiveDirectoryMemory() {
        DirectoryMemory own = directoryMemory.get();
        return own == null ? DirectoryMemory.getShared() : own;
    }

    /** The memory's last directory, or null when there is none. */
    private Path lastDirectory() {
        Optional<Path> last = effectiveDirectoryMemory().lastDirectory();
        return last == null ? null : last.orElse(null);
    }

    private PathMode effectiveMode() {
        PathMode current = mode.get();
        return current == null ? PathMode.OPEN : current;
    }

    private String shownPromptText() {
        if (promptText.get() != null) {
            return promptText.get();
        }
        return effectiveMode() == PathMode.DIRECTORY ? SELECT_FOLDER_TEXT : SELECT_FILE_TEXT;
    }

    /** Shows the mode: its pseudo-class, and the browse icon as a file or, in the directory mode, a folder. */
    private void updateModeLook() {
        PathMode current = effectiveMode();
        pseudoClassStateChanged(OPEN_PSEUDO_CLASS, current == PathMode.OPEN);
        pseudoClassStateChanged(SAVE_PSEUDO_CLASS, current == PathMode.SAVE);
        pseudoClassStateChanged(DIRECTORY_PSEUDO_CLASS, current == PathMode.DIRECTORY);
        browseGlyph.setIconCode(current == PathMode.DIRECTORY ? BootstrapIcons.FOLDER : BootstrapIcons.FILE_EARMARK);
    }

    private Window ownerWindow() {
        Scene scene = getScene();
        return scene == null ? null : scene.getWindow();
    }

    private static Path homeDirectory() {
        return Path.of(System.getProperty("user.home"));
    }
}
