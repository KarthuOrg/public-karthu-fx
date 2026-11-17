package org.karthu.fx.command;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.karthu.fx.theme.ThemeManager;

import java.util.Objects;

/**
 * The dialog in which the user changes the keys of a window's commands: a modal window with the normal title bar,
 * holding a {@link ShortcutsEditor} with <i>Cancel</i> and <i>Apply</i> beside its counter. Like the other karthu-fx
 * dialogs it takes the theme, the owner's icons, and opens centred on its owner.
 *
 * <pre>{@code
 * new ShortcutsDialog(stage, registry).showAndWait();
 * }</pre>
 *
 * <p><i>Apply</i> hands the user's changes to the registry and closes the dialog. It is disabled while a key is on two
 * commands of the same context ({@link ShortcutsEditor#hasConflictsProperty()}), so that the user resolves every
 * conflict first. <i>Cancel</i>, Esc while no key is being recorded, and closing the window drop the changes. Every
 * showing starts from the registry's keys with every group open. The window opens at about 760 by 660 pixels, where
 * the list shows about 18 commands in four open groups beside two folded ones; with more, the list scrolls, and
 * folding a group makes room. The window is resizable and remembers nothing.
 *
 * <p>The window's title and the buttons' texts are properties of the dialog with English defaults; the editor's texts
 * are the editor's ({@link #getEditor()}). The dialog's content carries the style class {@code shortcuts-dialog}, its
 * buttons {@code shortcuts-dialog-cancel} and {@code shortcuts-dialog-apply} (also {@code primary-button}), in the
 * editor's {@code shortcuts-footer-buttons}.
 *
 * <p>The dialog is created and shown on the JavaFX Application Thread.
 *
 * @since 1.1.0-RC2
 */
public final class ShortcutsDialog {

    private static final double WIDTH = 760;

    private static final double HEIGHT = 660;

    private static final double MIN_WIDTH = 520;

    private static final double MIN_HEIGHT = 360;

    private final StringProperty title = new SimpleStringProperty(this, "title", "Keyboard Shortcuts");

    private final StringProperty cancelText = new SimpleStringProperty(this, "cancelText", "Cancel");

    private final StringProperty applyText = new SimpleStringProperty(this, "applyText", "Apply");

    private final Stage stage = new Stage();

    private final ShortcutsEditor editor;

    /** Whether the user applied in the current showing. */
    private boolean applied;

    /**
     * Creates the dialog of a registry's keys, its window built and not shown.
     *
     * @param owner    the window the dialog belongs to, centred on it and taking its icons; null for none
     * @param registry the registry whose keys the dialog changes
     * @throws NullPointerException when the registry is null
     * @since 1.1.0-RC2
     */
    public ShortcutsDialog(Window owner, CommandRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        editor = new ShortcutsEditor(registry);
        stage.initStyle(StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);
        stage.titleProperty().bind(title);
        if (owner != null) {
            stage.initOwner(owner);
            if (owner instanceof Stage ownerStage) {
                stage.getIcons().addAll(ownerStage.getIcons());
            }
        }

        Button cancel = new Button();
        cancel.getStyleClass().add("shortcuts-dialog-cancel");
        cancel.textProperty().bind(cancelText);
        cancel.setCancelButton(true);
        cancel.setOnAction(event -> {
            editor.discard();
            stage.close();
        });
        // Not a default button: Enter records a key in the list.
        Button apply = new Button();
        apply.getStyleClass().addAll("shortcuts-dialog-apply", "primary-button");
        apply.textProperty().bind(applyText);
        // A key on two commands of one context would run only one of them: the user resolves every conflict first.
        apply.disableProperty().bind(editor.hasConflictsProperty());
        apply.setOnAction(event -> {
            editor.apply();
            applied = true;
            stage.close();
        });
        editor.footerButtons().addAll(cancel, apply);

        VBox root = new VBox(editor);
        root.getStyleClass().add("shortcuts-dialog");
        VBox.setVgrow(editor, Priority.ALWAYS);
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        stage.setScene(scene);
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        // Closing the window counts as Cancel.
        stage.setOnCloseRequest(event -> editor.discard());
        if (owner != null) {
            stage.setOnShown(event -> {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
            });
        }
    }

    /**
     * Shows the dialog and waits until the user closes it. It starts from the registry's current keys.
     *
     * @return true when the user applied the changes, false when they cancelled or closed the window
     * @since 1.1.0-RC2
     */
    public boolean showAndWait() {
        applied = false;
        editor.discard();
        stage.showAndWait();
        return applied;
    }

    /**
     * Returns the editor the dialog holds, whose texts an application translates.
     *
     * @return the editor
     * @since 1.1.0-RC2
     */
    public ShortcutsEditor getEditor() {
        return editor;
    }

    /**
     * The window's title (default <i>Keyboard Shortcuts</i>).
     *
     * @return the title property
     * @since 1.1.0-RC2
     */
    public StringProperty titleProperty() {
        return title;
    }

    /**
     * Returns the window's title.
     *
     * @return the title
     * @since 1.1.0-RC2
     */
    public String getTitle() {
        return title.get();
    }

    /**
     * Sets the window's title.
     *
     * @param title the title
     * @since 1.1.0-RC2
     */
    public void setTitle(String title) {
        this.title.set(title);
    }

    /**
     * The text of the button that drops the changes and closes the dialog (default <i>Cancel</i>).
     *
     * @return the cancel text property
     * @since 1.1.0-RC2
     */
    public StringProperty cancelTextProperty() {
        return cancelText;
    }

    /**
     * Returns the text of the cancel button.
     *
     * @return the cancel text
     * @since 1.1.0-RC2
     */
    public String getCancelText() {
        return cancelText.get();
    }

    /**
     * Sets the text of the cancel button.
     *
     * @param text the cancel text
     * @since 1.1.0-RC2
     */
    public void setCancelText(String text) {
        cancelText.set(text);
    }

    /**
     * The text of the button that applies the changes and closes the dialog (default <i>Apply</i>).
     *
     * @return the apply text property
     * @since 1.1.0-RC2
     */
    public StringProperty applyTextProperty() {
        return applyText;
    }

    /**
     * Returns the text of the apply button.
     *
     * @return the apply text
     * @since 1.1.0-RC2
     */
    public String getApplyText() {
        return applyText.get();
    }

    /**
     * Sets the text of the apply button.
     *
     * @param text the apply text
     * @since 1.1.0-RC2
     */
    public void setApplyText(String text) {
        applyText.set(text);
    }
}
