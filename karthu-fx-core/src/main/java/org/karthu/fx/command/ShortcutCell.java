package org.karthu.fx.command;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A line of the shortcut editor's list, drawn from the editor's working copy: a group's line (the fold arrow, the
 * group's name and its count) or a command's row (the name, the modified dot, the context mark, the key caps or what
 * shows in their place, and the row's buttons). The cell holds no state of its own: it draws its line again whenever
 * the editor's working copy changes, so that a cell the list reuses for another line shows that line's state.
 */
final class ShortcutCell extends ListCell<ShortcutListItem> {

    private static final String ROW_CLASS = "shortcuts-row";

    private static final String GROUP_ROW_CLASS = "shortcuts-group-row";

    private final ShortcutsEditor editor;

    // A command's row.
    private final HBox commandLine = new HBox();

    private final Label name = new Label();

    private final Region modifiedDot = new Region();

    private final Label contextMark = new Label();

    private final StackPane shortcutSlot = new StackPane();

    private final HBox caps = KeyCaps.row();

    private final Label hint = new Label();

    private final Hyperlink fix = new Hyperlink();

    /** The key caps with the hint and the fix after them. */
    private final HBox keyLine = new HBox(caps, hint, fix);

    private final Label notAssigned = new Label();

    private final Button edit = new Button();

    private final Button remove = new Button();

    private final Button reset = new Button();

    /** The caps' texts the row shows now, so that the caps are built again only when the key changed. */
    private List<String> shownCaps;

    // A group's line.
    private final HBox groupLine = new HBox();

    private final FontIcon arrow = FontIcon.of(BootstrapIcons.CHEVRON_DOWN);

    private final Label groupName = new Label();

    private final Label groupCount = new Label();

    /** Draws the line again; held here, since the editor holds it weakly. */
    private final InvalidationListener redraw = observable -> draw();

    /**
     * Creates a cell of an editor's list.
     *
     * @param editor the editor
     */
    ShortcutCell(ShortcutsEditor editor) {
        this.editor = editor;
        setText(null);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        name.getStyleClass().add("shortcuts-command-name");
        name.setMinWidth(0);
        modifiedDot.getStyleClass().add("shortcuts-modified-dot");
        contextMark.getStyleClass().add("shortcuts-context-mark");
        contextMark.setMinWidth(Region.USE_PREF_SIZE);
        HBox commandColumn = new HBox(name, modifiedDot, contextMark);
        commandColumn.getStyleClass().add("shortcuts-command-column");
        commandColumn.setAlignment(Pos.CENTER_LEFT);
        // The command column is as wide as the header's, so that the key caps line up under the header's
        // Shortcut.
        commandColumn.minWidthProperty().bind(editor.commandColumnWidth());
        commandColumn.prefWidthProperty().bind(editor.commandColumnWidth());
        commandColumn.maxWidthProperty().bind(editor.commandColumnWidth());

        notAssigned.getStyleClass().add("shortcuts-not-assigned");
        notAssigned.textProperty().bind(editor.notAssignedTextProperty());
        shortcutSlot.getStyleClass().add("shortcuts-shortcut-column");
        shortcutSlot.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(shortcutSlot, Priority.ALWAYS);
        hint.getStyleClass().add("shortcuts-hint");
        hint.setMinWidth(0);
        fix.getStyleClass().add("shortcuts-fix");
        fix.setMinWidth(Region.USE_PREF_SIZE);
        fix.setFocusTraversable(false);
        fix.textProperty().bind(editor.conflictFixTextProperty());
        fix.setOnAction(event -> {
            // A link that was followed shows as visited: this one stays a fix, whichever line the cell shows next.
            fix.setVisited(false);
            if (getItem() instanceof CommandRow entry) {
                editor.removeFromOthers(entry);
            }
        });
        keyLine.getStyleClass().add("shortcuts-key-line");
        keyLine.setAlignment(Pos.CENTER_LEFT);

        setUpButton(edit, "shortcuts-edit", BootstrapIcons.PENCIL, editor.editTooltipTextProperty());
        setUpButton(remove, "shortcuts-remove", BootstrapIcons.X, editor.removeTooltipTextProperty());
        setUpButton(reset, "shortcuts-reset", BootstrapIcons.ARROW_COUNTERCLOCKWISE,
                editor.resetTooltipTextProperty());
        edit.setOnAction(event -> {
            if (getItem() instanceof CommandRow entry) {
                editor.startRecording(entry);
            }
        });
        remove.setOnAction(event -> {
            if (getItem() instanceof CommandRow entry) {
                editor.remove(entry);
            }
        });
        reset.setOnAction(event -> {
            if (getItem() instanceof CommandRow entry) {
                editor.reset(entry);
            }
        });
        commandLine.getStyleClass().add("shortcuts-row-content");
        commandLine.setAlignment(Pos.CENTER_LEFT);
        commandLine.getChildren().addAll(commandColumn, shortcutSlot, edit, remove, reset);

        arrow.getStyleClass().add("shortcuts-group-arrow");
        groupName.getStyleClass().add("shortcuts-group-name");
        groupCount.getStyleClass().add("shortcuts-group-count");
        groupLine.getStyleClass().add("shortcuts-group-content");
        groupLine.setAlignment(Pos.CENTER_LEFT);
        groupLine.getChildren().addAll(arrow, groupName, groupCount);

        editor.revision().addListener(new WeakInvalidationListener(redraw));
        hoverProperty().addListener(redraw);
        selectedProperty().addListener(redraw);
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::clicked);
    }

    /**
     * A row's button: an icon button that is not focus traversable, so that a click leaves the focus where it is,
     * with its tooltip and accessible text.
     */
    private static void setUpButton(Button button, String styleClass, BootstrapIcons icon,
            ObservableValue<String> tooltipText) {
        button.getStyleClass().addAll("icon-button", styleClass);
        button.setGraphic(FontIcon.of(icon));
        button.setFocusTraversable(false);
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(tooltipText);
        button.setTooltip(tooltip);
        button.accessibleTextProperty().bind(tooltipText);
    }

    @Override
    protected void updateItem(ShortcutListItem item, boolean empty) {
        super.updateItem(item, empty);
        draw();
    }

    /**
     * Draws the cell's line from the working copy.
     */
    private void draw() {
        ShortcutListItem item = isEmpty() ? null : getItem();
        switch (item) {
            case CommandRow entry -> drawRow(entry);
            case GroupLine group -> drawGroup(group);
            case null -> drawEmpty();
        }
    }

    private void drawEmpty() {
        getStyleClass().removeAll(ROW_CLASS, GROUP_ROW_CLASS);
        showStates(EnumSet.noneOf(LineState.class));
        setGraphic(null);
    }

    private void drawGroup(GroupLine group) {
        getStyleClass().remove(ROW_CLASS);
        if (!getStyleClass().contains(GROUP_ROW_CLASS)) {
            getStyleClass().add(GROUP_ROW_CLASS);
        }
        boolean open = editor.isOpen(group);
        showStates(open ? EnumSet.noneOf(LineState.class) : EnumSet.of(LineState.COLLAPSED));
        arrow.setIconCode(open ? BootstrapIcons.CHEVRON_DOWN : BootstrapIcons.CHEVRON_RIGHT);
        groupName.setText(group.getGroup().getName());
        groupCount.setText(Integer.toString(editor.shownCount(group)));
        if (getGraphic() != groupLine) {
            setGraphic(groupLine);
        }
    }

    private void drawRow(CommandRow entry) {
        getStyleClass().remove(GROUP_ROW_CLASS);
        if (!getStyleClass().contains(ROW_CLASS)) {
            getStyleClass().add(ROW_CLASS);
        }
        Command command = entry.getCommand();
        boolean changeable = entry.isChangeable();
        boolean recording = editor.isRecording(entry);
        boolean modified = entry.isModified();
        Shortcut shortcut = entry.getShortcut();
        Set<LineState> states = EnumSet.noneOf(LineState.class);
        if (modified) {
            states.add(LineState.MODIFIED);
        }
        if (shortcut == null) {
            states.add(LineState.NOT_ASSIGNED);
        }
        if (!changeable) {
            states.add(LineState.FIXED);
        }
        if (recording) {
            states.add(LineState.RECORDING);
        }
        if (entry.isInConflict()) {
            states.add(LineState.CONFLICT);
        }
        if (!entry.getOverridden().isEmpty()) {
            states.add(LineState.OVERRIDE);
        }
        showStates(states);

        name.setText(command.getName());
        modifiedDot.setVisible(modified);
        modifiedDot.setManaged(modified);
        CommandContext context = command.getContext();
        boolean marked = context != editor.windowContext();
        contextMark.setText(marked ? context.getName() : "");
        contextMark.setVisible(marked);
        contextMark.setManaged(marked);

        if (recording) {
            showInSlot(editor.recorder());
            editor.recorder().requestFocus();
        } else if (shortcut == null) {
            showInSlot(notAssigned);
        } else {
            List<String> parts = KeyCaps.partsOf(shortcut);
            if (!parts.equals(shownCaps)) {
                KeyCaps.show(caps, parts);
                shownCaps = parts;
            }
            drawHint(entry);
            showInSlot(keyLine);
        }

        // The pencil and the x show on the row under the pointer and on the selected row, so that they serve the
        // mouse and the keyboard alike; the reset button shows on every modified row.
        boolean active = changeable && !recording && (isHover() || isSelected());
        edit.setVisible(active);
        remove.setVisible(active && shortcut != null);
        boolean resettable = changeable && !recording && modified;
        reset.setVisible(resettable);
        reset.setManaged(resettable);
        if (getGraphic() != commandLine) {
            setGraphic(commandLine);
        }
    }

    /**
     * Draws the hint after the key caps: the other commands of a conflict, with the fix while one of them may lose
     * the key; else the commands the row's key overrides; nothing while the row's key clashes with none.
     */
    private void drawHint(CommandRow entry) {
        String text = null;
        boolean fixable = false;
        if (entry.isInConflict()) {
            text = ShortcutsEditor.fill(editor.getConflictHintFormat(), names(entry.getConflicts()));
            for (CommandRow other : entry.getConflicts()) {
                fixable = fixable || other.isChangeable();
            }
        } else if (!entry.getOverridden().isEmpty()) {
            text = ShortcutsEditor.fill(editor.getOverrideHintFormat(), names(entry.getOverridden()));
        }
        hint.setText(text == null ? "" : text);
        hint.setVisible(text != null);
        hint.setManaged(text != null);
        fix.setVisible(fixable);
        fix.setManaged(fixable);
    }

    /**
     * The names of the commands of rows, separated by commas.
     */
    private static String names(List<CommandRow> entries) {
        List<String> names = new ArrayList<>();
        for (CommandRow entry : entries) {
            names.add(entry.getCommand().getName());
        }
        return String.join(", ", names);
    }

    /**
     * Shows a node in the shortcut column, unless it shows there already: the recording field keeps the focus only
     * while it stays in the scene.
     */
    private void showInSlot(Node node) {
        if (shortcutSlot.getChildren().size() != 1 || shortcutSlot.getChildren().get(0) != node) {
            shortcutSlot.getChildren().setAll(node);
        }
    }

    /**
     * Shows the states of the cell's line as its pseudo-classes: those given, and none of the others, which a line
     * the cell showed before may have left.
     */
    private void showStates(Set<LineState> states) {
        for (LineState state : LineState.values()) {
            pseudoClassStateChanged(state.pseudoClass, states.contains(state));
        }
    }

    /**
     * A click: one on a group's line folds or opens the group, a double-click on a command's row starts recording,
     * unless it is in the recording field.
     */
    private void clicked(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || isEmpty()) {
            return;
        }
        ShortcutListItem item = getItem();
        if (item instanceof GroupLine group && event.getClickCount() == 1) {
            editor.toggle(group);
        } else if (item instanceof CommandRow entry && event.getClickCount() == 2
                && !editor.isInRecorder(event.getTarget())) {
            editor.startRecording(entry);
        }
    }

    /**
     * A state of a line that its cell shows as a pseudo-class, documented in the editor's JavaDoc and STYLEGUIDE.md:
     * {@code :collapsed} on a group's line, the others on a command's row.
     */
    private enum LineState {
        /** The command's key differs from its default. */
        MODIFIED("modified"),
        /** The command has no key. */
        NOT_ASSIGNED("not-assigned"),
        /** The command is fixed. */
        FIXED("fixed"),
        /** The row records a new key. */
        RECORDING("recording"),
        /** The group is folded. */
        COLLAPSED("collapsed"),
        /** The command's key is another command's of the same context too. */
        CONFLICT("conflict"),
        /** The command's key overrides a command of an enclosing context. */
        OVERRIDE("override");

        private final PseudoClass pseudoClass;

        LineState(String name) {
            this.pseudoClass = PseudoClass.getPseudoClass(name);
        }
    }
}
