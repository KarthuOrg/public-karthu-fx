package org.karthu.fx.form;

import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * The binding's controls of a shell: the {@code {}} icon, the chip with its remove button, the binding list and the
 * binding keys. The shell owns one, places the icon and the chip among its children and lays them out, and tells it
 * when read-only or the binding changes; what they show and do is described in {@link FieldShell} (<i>Binding</i>).
 * The user's binding, switching and unbinding go through here, with the shell's guard: not while it is disabled or
 * read-only.
 *
 * <p>Not exported: the shell's implementation.
 */
final class BindingControls {

    /** The shortcut of the {} icon, pressed in the shell. */
    private static final KeyCombination BIND_SHORTCUT =
            new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN);

    private final FieldShell shell;
    private final FieldBinding binding;
    private final Button bindIcon = new Button();
    private final Chip chip = new Chip();
    /** The chip's remove button, its graphic while the shell is not read-only. */
    private final Button chipRemove = new Button();
    /** The binding list, created when first needed. */
    private ContextMenu menu;

    /**
     * Builds the controls of a shell's binding.
     *
     * @param shell   the shell that owns them
     * @param binding the shell's binding
     */
    BindingControls(FieldShell shell, FieldBinding binding) {
        this.shell = shell;
        this.binding = binding;
        bindIcon.getStyleClass().add("form-frame-bind");
        FieldShell.styleAsIcon(bindIcon);
        bindIcon.setGraphic(FontIcon.of(BootstrapIcons.BRACES));
        Tooltip bindTooltip = new Tooltip();
        bindTooltip.textProperty().bind(binding.bindTooltipTextProperty());
        bindIcon.setTooltip(bindTooltip);
        bindIcon.accessibleTextProperty().bind(binding.bindTooltipTextProperty());
        bindIcon.setOnAction(event -> openMenu());

        chip.textProperty().bind(binding.boundNameProperty());
        chipRemove.getStyleClass().addAll("icon-button", "form-chip-remove");
        chipRemove.setGraphic(FontIcon.of(BootstrapIcons.X));
        chipRemove.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        chipRemove.setFocusTraversable(false);
        Tooltip removeTooltip = new Tooltip();
        removeTooltip.textProperty().bind(binding.removeTextProperty());
        chipRemove.setTooltip(removeTooltip);
        chipRemove.accessibleTextProperty().bind(binding.removeTextProperty());
        chipRemove.setOnAction(event -> bindByUser(null));
        chip.missingTooltip.textProperty().bind(binding.missingTooltipTextProperty());

        binding.getCandidates().addListener((ListChangeListener<BindingCandidate>) change -> refillShownMenu());
    }

    /**
     * Returns the {@code {}} icon, the shell's last trailing node while it shows.
     *
     * @return the icon
     */
    Button bindIcon() {
        return bindIcon;
    }

    /**
     * Returns the chip, which the shell lays over its content while bound.
     *
     * @return the chip
     */
    Label chip() {
        return chip;
    }

    /**
     * Returns the binding list, created when first needed.
     *
     * @return the binding list
     */
    ContextMenu menu() {
        if (menu == null) {
            menu = new ContextMenu();
            menu.getStyleClass().add("form-binding-menu");
        }
        return menu;
    }

    /**
     * Shows the binding as read-only and the binding want it: the chip while bound (red while its variable is missing),
     * with its remove button while not read-only, and the {} icon while the binding is bindable or bound and the shell
     * is not read-only.
     *
     * @param readOnly whether the shell is read-only
     */
    void update(boolean readOnly) {
        boolean bound = binding.isBound();
        chip.setVisible(bound);
        chip.setManaged(bound);
        chip.setMissing(bound && binding.isMissing());
        chip.setGraphic(readOnly ? null : chipRemove);
        boolean iconShown = (binding.isBindable() || bound) && !readOnly;
        bindIcon.setVisible(iconShown);
        bindIcon.setManaged(iconShown);
    }

    /** Closes the binding list if it shows: the shell became read-only or disabled. */
    void closeMenu() {
        if (menu != null && menu.isShowing()) {
            menu.hide();
        }
    }

    /**
     * Ctrl+Space opens the binding list while the {} icon shows; Backspace or Delete unbind while bound. Neither while
     * the shell is disabled or read-only: then the keys pass on to the content, which changes nothing either.
     *
     * @param event a key pressed in the shell
     */
    void handleKeys(KeyEvent event) {
        if (!shell.userMayChangeBinding()) {
            return;
        }
        if (BIND_SHORTCUT.match(event)) {
            if (bindIcon.isVisible()) {
                event.consume();
                openMenu();
            }
        } else if (binding.isBound()
                && (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE)) {
            event.consume();
            bindByUser(null);
        }
    }

    /**
     * Keeps a shown binding list in step with candidates that arrive late; a hidden one is filled when it opens. Not
     * on a change of the bound name: the user's choice changes it while the list still handles the click.
     */
    private void refillShownMenu() {
        if (menu != null && menu.isShowing()) {
            fillMenu(menu);
        }
    }

    /**
     * Fills the binding list and shows it under the {} icon, when the shell is in a shown window. Not while disabled
     * or read-only, when the user may not change the binding.
     */
    private void openMenu() {
        if (!shell.userMayChangeBinding()) {
            return;
        }
        ContextMenu shown = menu();
        fillMenu(shown);
        Scene scene = bindIcon.getScene();
        Window window = scene == null ? null : scene.getWindow();
        if (window != null && window.isShowing()) {
            shown.show(bindIcon, Side.BOTTOM, 0, 0);
        }
    }

    /**
     * Fills the binding list: one entry per candidate (the current one checked) or the disabled no-candidates entry,
     * then, while bound, a separator and the entry that unbinds.
     */
    private void fillMenu(ContextMenu filled) {
        List<MenuItem> items = new ArrayList<>();
        String boundName = binding.getBoundName();
        for (BindingCandidate candidate : binding.getCandidates()) {
            if (candidate != null) {
                items.add(candidateItem(candidate, candidate.name().equals(boundName)));
            }
        }
        if (items.isEmpty()) {
            MenuItem empty = new MenuItem(binding.getNoCandidatesText());
            empty.getStyleClass().add("form-binding-empty");
            empty.setDisable(true);
            items.add(empty);
        }
        if (binding.isBound()) {
            items.add(new SeparatorMenuItem());
            MenuItem remove = new MenuItem(binding.getRemoveText());
            remove.getStyleClass().add("form-binding-remove");
            remove.setOnAction(event -> bindByUser(null));
            items.add(remove);
        }
        filled.getItems().setAll(items);
    }

    /** A candidate's entry: its name, then its detail muted, checked while it is the bound one. */
    private CheckMenuItem candidateItem(BindingCandidate candidate, boolean current) {
        Label name = new Label(candidate.name());
        name.getStyleClass().add("form-binding-name");
        HBox entry = new HBox(name);
        entry.setAlignment(Pos.CENTER_LEFT);
        String detailText = candidate.detail();
        if (detailText != null && !detailText.isEmpty()) {
            Label detail = new Label(detailText);
            detail.getStyleClass().add("form-binding-detail");
            entry.getChildren().add(detail);
        }
        CheckMenuItem item = new CheckMenuItem();
        item.getStyleClass().add("form-binding-candidate");
        item.setGraphic(entry);
        item.setSelected(current);
        item.setOnAction(event -> bindByUser(candidate.name()));
        return item;
    }

    /**
     * What the user's binding, switching and unbinding do: a candidate exists and an unbound input has no variable, so
     * missing is cleared first; the name changes last, so the application's listener on it decides last. Nothing while
     * disabled or read-only, also from a list or a remove button that was at hand before.
     */
    private void bindByUser(String name) {
        if (!shell.userMayChangeBinding()) {
            return;
        }
        binding.setMissing(false);
        binding.setBoundName(name);
    }

    /**
     * The chip: the bound name with the remove button as its graphic, right of the name. A class of its own, so its
     * pseudo-class can be set.
     */
    private static final class Chip extends Label {

        private static final PseudoClass MISSING_PSEUDO_CLASS = PseudoClass.getPseudoClass("missing");

        /** The tooltip of a missing binding, shown only while missing. */
        private final Tooltip missingTooltip = new Tooltip();

        private Chip() {
            getStyleClass().add("form-chip");
            setContentDisplay(ContentDisplay.RIGHT);
            setMnemonicParsing(false);
        }

        /** Shows a missing binding: {@code :missing} and the missing tooltip. */
        private void setMissing(boolean missing) {
            pseudoClassStateChanged(MISSING_PSEUDO_CLASS, missing);
            setTooltip(missing ? missingTooltip : null);
        }
    }
}
