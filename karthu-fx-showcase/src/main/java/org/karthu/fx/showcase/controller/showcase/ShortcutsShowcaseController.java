package org.karthu.fx.showcase.controller.showcase;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.karthu.fx.command.Command;
import org.karthu.fx.command.CommandContext;
import org.karthu.fx.command.CommandGroup;
import org.karthu.fx.command.CommandRegistry;
import org.karthu.fx.command.Shortcut;
import org.karthu.fx.command.ShortcutsDialog;
import org.karthu.fx.command.ShortcutsEditor;
import org.karthu.fx.markdown.MarkdownView;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.karthu.fx.command.Shortcut.Modifier.ALT;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.Shortcut.Modifier.SHIFT;

/**
 * Showcase page of the command registry: a small example application of 30 commands in six groups, with a menu bar,
 * a toolbar, a note field and a document, whose commands run on their keys. The whole page is the application's
 * window; the document and the note field are parts of it with keys of their own. A read-out names the last command
 * that ran, another the text list the application keeps. A button opens the {@link ShortcutsDialog}, and a
 * {@link ShortcutsEditor} sits in a card with buttons of the page's own.
 */
public class ShortcutsShowcaseController {

    /** The text of the example application's document. */
    private static final String DOCUMENT_TEXT = """
            # Release notes

            ## Version 2.0

            A search in every document, and keys you choose yourself: *Help > Keyboard shortcuts* changes them.

            ## Version 1.0

            The first version: documents, notes and bookmarks.
            """;

    /**
     * A text list as an application reads it from its settings on a start: a changed key, a removed one, a key that is
     * another command's default, a command that no longer exists and a text that is not the text form.
     */
    private static final Map<String, String> STORED_LIST = Map.of(
            "file.save-as", "Ctrl+Alt+S",
            "edit.duplicate", "",
            "view.fit-width", "Ctrl+0",
            "file.print", "Ctrl+P",
            "search.replace", "ctrl+h");

    @FXML private VBox page;
    @FXML private VBox exampleContainer;
    @FXML private FlowPane exampleSwitches;
    @FXML private Label lastCommandLabel;
    @FXML private Label storedListLabel;
    @FXML private HBox storedListButtons;
    @FXML private HBox dialogButtons;
    @FXML private Label dialogResultLabel;
    @FXML private VBox editorContainer;

    private CommandRegistry registry;

    /** The dialog, built when it is first opened, then shown again. */
    private ShortcutsDialog dialog;

    /** How many commands ran on this page, for the read-out. */
    private int fired;

    @FXML
    private void initialize() {
        // The registry belongs to a window's scene: the page builds its example application when it first enters one,
        // the showcase's window.
        page.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null && registry == null) {
                install(newScene);
            }
        });
    }

    /**
     * The registry of the page's window, null until the page entered one.
     *
     * @return the registry
     */
    CommandRegistry registry() {
        return registry;
    }

    /**
     * Builds the example application in the window's scene, as an application builds its window: the registry, its
     * contexts, groups and commands, and the controls bound to them. Then the cards around it.
     */
    private void install(Scene scene) {
        registry = new CommandRegistry(scene);

        MarkdownView document = new MarkdownView(DOCUMENT_TEXT);
        document.setId("document");
        // The window's View commands zoom the document: the view's own zoom bar and keys stay off.
        document.setZoomBarVisible(false);
        // The showcase fits a page to its window's height: without a minimum the document would shrink to a line.
        document.setPrefHeight(320);
        document.setMinHeight(Region.USE_PREF_SIZE);
        TextField note = new TextField();
        note.setId("noteField");
        HBox.setHgrow(note, Priority.ALWAYS);

        // The whole page is the window. The document and the note field are parts of it whose keys hold while the
        // focus is inside them, and win over the window's keys there.
        CommandContext window = registry.getWindowContext();
        CommandContext inDocument = registry.addContext("document", "Document", document);
        CommandContext inNote = registry.addContext("note", "Note", note);

        CommandGroup file = registry.addGroup("file", "File");
        Command newDocument = command(file, window, "file.new", "New document", Shortcut.of(KeyCode.N, CTRL), null);
        Command open = command(file, window, "file.open", "Open\u2026", Shortcut.of(KeyCode.O, CTRL), null);
        Command save = command(file, window, "file.save", "Save", Shortcut.of(KeyCode.S, CTRL), null);
        command(file, window, "file.save-as", "Save as\u2026", Shortcut.of(KeyCode.S, CTRL, SHIFT), null);
        command(file, window, "file.export", "Export as PDF\u2026", null, null);
        command(file, window, "file.close", "Close", Shortcut.of(KeyCode.W, CTRL), null);

        CommandGroup edit = registry.addGroup("edit", "Edit");
        Command undo = command(edit, window, "edit.undo", "Undo", Shortcut.of(KeyCode.Z, CTRL), null);
        Command redo = command(edit, window, "edit.redo", "Redo", Shortcut.of(KeyCode.Y, CTRL), null);
        // The document copies its selection itself: a fixed command without an action shows its key in the dialog.
        registry.addFixedCommand(edit, inDocument, "document.copy", "Copy", Shortcut.of(KeyCode.C, CTRL), null);
        command(edit, window, "edit.paste", "Paste", Shortcut.of(KeyCode.V, CTRL), null);
        command(edit, window, "edit.duplicate", "Duplicate", Shortcut.of(KeyCode.D, CTRL), null);
        Command insertDate = command(edit, inNote, "note.insert-date", "Insert date", Shortcut.of(KeyCode.D, CTRL),
                () -> note.insertText(note.getCaretPosition(), LocalDate.now().toString()));
        // A default a user could not record: Esc belongs to the note field, as Delete or Esc belong to a canvas.
        command(edit, inNote, "note.clear", "Clear note", Shortcut.of(KeyCode.ESCAPE), note::clear);

        CommandGroup search = registry.addGroup("search", "Search");
        Command find = command(search, window, "search.find", "Find\u2026", Shortcut.of(KeyCode.F, CTRL), null);
        Command findInDocument = command(search, inDocument, "document.find", "Find in document",
                Shortcut.of(KeyCode.F, CTRL), null);
        command(search, window, "search.find-next", "Find next", Shortcut.of(KeyCode.F3), null);
        command(search, window, "search.find-previous", "Find previous", Shortcut.of(KeyCode.F3, SHIFT), null);
        command(search, window, "search.replace", "Replace\u2026", Shortcut.of(KeyCode.R, CTRL), null);

        CommandGroup documentGroup = registry.addGroup("document", "Document");
        command(documentGroup, inDocument, "document.next-heading", "Next heading", Shortcut.of(KeyCode.DOWN, ALT),
                null);
        command(documentGroup, inDocument, "document.previous-heading", "Previous heading",
                Shortcut.of(KeyCode.UP, ALT), null);
        command(documentGroup, inDocument, "document.bookmark", "Add bookmark", Shortcut.of(KeyCode.B, CTRL), null);
        command(documentGroup, inDocument, "document.comment", "Add comment", Shortcut.of(KeyCode.M, CTRL, ALT),
                null);

        CommandGroup view = registry.addGroup("view", "View");
        Command zoomIn = command(view, window, "view.zoom-in", "Zoom in", Shortcut.of('+', CTRL),
                () -> document.setZoom(document.getZoom() + 0.25));
        Command zoomOut = command(view, window, "view.zoom-out", "Zoom out", Shortcut.of('-', CTRL),
                () -> document.setZoom(document.getZoom() - 0.25));
        command(view, window, "view.actual-size", "Actual size", Shortcut.of(KeyCode.DIGIT0, CTRL),
                () -> document.setZoom(1.0));
        command(view, window, "view.fit-width", "Fit to width", null, null);
        command(view, window, "view.word-wrap", "Word wrap", Shortcut.of(KeyCode.Z, ALT), null);

        CommandGroup help = registry.addGroup("help", "Help");
        Command shortcuts = command(help, window, "help.shortcuts", "Keyboard shortcuts\u2026",
                Shortcut.of(KeyCode.K, CTRL), this::openDialog);
        command(help, window, "help.documentation", "Documentation", Shortcut.of(KeyCode.F1), null);
        command(help, window, "help.about", "About", null, null);

        // The showcase's window holds every page, so the application's commands hold while its page shows: hidden,
        // they are disabled, and a disabled command leaves its key to the window's other pages. In an application of
        // its own, the window would be the application's alone and this binding would not be needed.
        BooleanBinding pageHidden = Bindings.createBooleanBinding(() -> page.getScene() == null, page.sceneProperty());
        for (Command command : registry.getCommands()) {
            if (command != undo && command != findInDocument) {
                command.disabledProperty().bind(pageHidden);
            }
        }
        // Two commands the user switches off, as an application disables a command that cannot run now.
        CheckBox nothingToUndo = new CheckBox("Nothing to undo");
        CheckBox findInDocumentDisabled = new CheckBox("Find in document disabled");
        exampleSwitches.getChildren().addAll(nothingToUndo, findInDocumentDisabled);
        undo.disabledProperty().bind(pageHidden.or(nothingToUndo.selectedProperty()));
        findInDocument.disabledProperty().bind(pageHidden.or(findInDocumentDisabled.selectedProperty()));

        // A prompt that names a key names the command's current one, and drops it while the command has none.
        note.promptTextProperty().bind(Bindings.createStringBinding(() -> insertDate.getShortcutDisplayText().isEmpty()
                        ? "A note" : "A note: " + insertDate.getShortcutDisplayText() + " inserts the date",
                insertDate.shortcutDisplayTextProperty()));
        HBox toolbar = new HBox(2,
                toolbarButton(newDocument, BootstrapIcons.FILE_EARMARK_PLUS),
                toolbarButton(open, BootstrapIcons.FOLDER2_OPEN),
                toolbarButton(save, BootstrapIcons.SAVE),
                new Separator(Orientation.VERTICAL),
                toolbarButton(undo, BootstrapIcons.ARROW_COUNTERCLOCKWISE),
                toolbarButton(redo, BootstrapIcons.ARROW_CLOCKWISE),
                new Separator(Orientation.VERTICAL),
                toolbarButton(find, BootstrapIcons.SEARCH),
                toolbarButton(zoomOut, BootstrapIcons.ZOOM_OUT),
                toolbarButton(zoomIn, BootstrapIcons.ZOOM_IN),
                new Separator(Orientation.VERTICAL),
                toolbarButton(shortcuts, BootstrapIcons.KEYBOARD));
        toolbar.setId("exampleToolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox toolbarRow = new HBox(12, toolbar, note);
        toolbarRow.setAlignment(Pos.CENTER_LEFT);
        exampleContainer.getChildren().addAll(menuBar(inNote), toolbarRow, document);

        fillStoredList();
        Button openDialog = new Button();
        openDialog.setId("openDialogButton");
        openDialog.getStyleClass().add("primary-button");
        openDialog.textProperty().bind(shortcuts.nameProperty());
        shortcuts.bind(openDialog);
        tooltip(openDialog, shortcuts);
        dialogButtons.getChildren().add(openDialog);
        fillEditor();
    }

    /**
     * The example application's menu bar: a menu for each group, an item for each command of the window or the
     * document that has an action. The note's commands are the field's own keys and stay out of the menus.
     */
    private MenuBar menuBar(CommandContext inNote) {
        MenuBar menuBar = new MenuBar();
        menuBar.setId("exampleMenuBar");
        for (CommandGroup group : registry.getGroups()) {
            Menu menu = new Menu();
            menu.textProperty().bind(group.nameProperty());
            for (Command command : registry.getCommands()) {
                if (command.getGroup() == group && !command.isFixed() && command.getContext() != inNote) {
                    MenuItem item = new MenuItem();
                    item.textProperty().bind(command.nameProperty());
                    command.bind(item);
                    menu.getItems().add(item);
                }
            }
            menuBar.getMenus().add(menu);
        }
        return menuBar;
    }

    /** An icon button bound to a command, its tooltip the command's name and current key. */
    private static Button toolbarButton(Command command, Ikon icon) {
        Button button = new Button();
        button.setGraphic(FontIcon.of(icon, 16));
        button.getStyleClass().add("icon-button");
        command.bind(button);
        tooltip(button, command);
        return button;
    }

    /** Gives a control a tooltip of a command's name and its current key, such as <i>Save (Ctrl+S)</i>. */
    private static void tooltip(Control control, Command command) {
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(nameWithKey(command));
        control.setTooltip(tooltip);
    }

    /** A command's name with its current key in brackets, the name alone while it has none. */
    private static ObservableValue<String> nameWithKey(Command command) {
        return Bindings.createStringBinding(() -> {
            String key = command.getShortcutDisplayText();
            return key.isEmpty() ? command.getName() : command.getName() + " (" + key + ")";
        }, command.nameProperty(), command.shortcutDisplayTextProperty());
    }

    /**
     * The card of the stored text list: the keys that differ from their defaults, as the application would write them
     * to its settings whenever they change, and buttons that hand a stored list back and reset every key.
     */
    private void fillStoredList() {
        registry.changedShortcutsProperty().addListener(
                (observable, oldList, newList) -> storedListLabel.setText(lines(newList)));
        storedListLabel.setText(lines(registry.getChangedShortcuts()));
        Button handBack = new Button("Hand back a stored list");
        handBack.setOnAction(event -> registry.setChangedShortcuts(STORED_LIST));
        Button resetAll = new Button("Reset all keys");
        resetAll.setOnAction(event -> registry.resetShortcuts());
        storedListButtons.getChildren().addAll(handBack, resetAll);
    }

    /** A text list as lines {@code id=key text}, in the order the registry gives them. */
    private static String lines(Map<String, String> changed) {
        if (changed.isEmpty()) {
            return "(empty: every key is its default)";
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : changed.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join("\n", lines);
    }

    /**
     * Opens the dialog over the showcase's window, the action of <i>Keyboard shortcuts</i>, and tells whether the user
     * applied or cancelled.
     */
    private void openDialog() {
        if (dialog == null) {
            dialog = new ShortcutsDialog(page.getScene().getWindow(), registry);
        }
        boolean applied = dialog.showAndWait();
        dialogResultLabel.setText(applied ? "The dialog was applied." : "The dialog was cancelled.");
    }

    /**
     * The card of the embedded editor: the editor, a read-out of its state, and the page's own buttons, which the
     * editor's state drives.
     */
    private void fillEditor() {
        ShortcutsEditor editor = new ShortcutsEditor(registry);
        editor.setPrefHeight(460);
        editor.setMinHeight(Region.USE_PREF_SIZE);
        Button discard = new Button("Discard");
        discard.disableProperty().bind(editor.hasChangesProperty().not());
        discard.setOnAction(event -> editor.discard());
        Button apply = new Button("Apply");
        apply.getStyleClass().add("primary-button");
        // As the dialog's Apply: a key on two commands of one context would run only one of them.
        apply.disableProperty().bind(editor.hasChangesProperty().not().or(editor.hasConflictsProperty()));
        apply.setOnAction(event -> editor.apply());
        HBox buttons = new HBox(8, discard, apply);
        buttons.setId("editorButtons");
        Label state = new Label();
        state.setId("editorStateLabel");
        state.textProperty().bind(Bindings.createStringBinding(
                () -> "Has changes: " + yesOrNo(editor.hasChanges()) + " \u00B7 has conflicts: "
                        + yesOrNo(editor.hasConflicts()),
                editor.hasChangesProperty(), editor.hasConflictsProperty()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(8, state, spacer, buttons);
        footer.setAlignment(Pos.CENTER_LEFT);
        editorContainer.getChildren().addAll(editor, footer);
    }

    private static String yesOrNo(boolean value) {
        return value ? "yes" : "no";
    }

    /**
     * Adds a command whose action names it in the read-out, then does what the command does, if anything.
     *
     * @param effect what the command does beyond the read-out, null for nothing
     */
    private Command command(CommandGroup group, CommandContext context, String id, String name,
                            Shortcut defaultShortcut, Runnable effect) {
        Reporting action = new Reporting(effect);
        Command command = registry.addCommand(group, context, id, name, defaultShortcut, action);
        action.command = command;
        return command;
    }

    private void fired(Command command) {
        fired++;
        lastCommandLabel.setText("Last command: " + command.getName() + " (" + command.getId() + "), " + fired
                + (fired == 1 ? " command" : " commands") + " so far");
    }

    /** A command's action: it names the command in the read-out, then does what the command does. */
    private final class Reporting implements Runnable {

        private final Runnable effect;

        private Command command;

        private Reporting(Runnable effect) {
            this.effect = effect;
        }

        @Override
        public void run() {
            fired(command);
            if (effect != null) {
                effect.run();
            }
        }
    }
}
