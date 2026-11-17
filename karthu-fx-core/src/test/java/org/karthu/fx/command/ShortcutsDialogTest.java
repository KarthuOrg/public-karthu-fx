package org.karthu.fx.command;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssParser;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.command.ShortcutsEditorTest.App;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.command.CommandTestSupport.onFxThread;
import static org.karthu.fx.command.Shortcut.Modifier.CTRL;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.assertShortcutColumn;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.button;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.layout;
import static org.karthu.fx.command.ShortcutsEditorTestSupport.select;

/**
 * The shortcut dialog through its public API and its documented style classes: built on the JavaFX Application
 * Thread, as a window has to be, its buttons fired, never shown.
 */
class ShortcutsDialogTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CommandTestSupport.initToolkit();
    }

    @Test
    @DisplayName("the dialog is a modal, decorated, resizable window of about 760 by 660 with the owner's icons")
    void dialogWindow() throws InterruptedException {
        onFxThread(() -> {
            Stage owner = new Stage();
            WritableImage icon = new WritableImage(16, 16);
            owner.getIcons().add(icon);
            ShortcutsDialog dialog = new ShortcutsDialog(owner, new App().registry);
            Stage window = window(dialog);

            assertEquals("Keyboard Shortcuts", window.getTitle());
            assertSame(owner, window.getOwner());
            assertEquals(Modality.APPLICATION_MODAL, window.getModality());
            assertEquals(StageStyle.DECORATED, window.getStyle());
            assertTrue(window.isResizable());
            assertEquals(760, window.getWidth());
            assertEquals(660, window.getHeight());
            assertEquals(List.of(icon), window.getIcons());
            assertFalse(window.isShowing());

            dialog.setTitle("Tastenkürzel");
            assertEquals("Tastenkürzel", window.getTitle());
            assertEquals("Tastenkürzel", dialog.getTitle());
        });
    }

    @Test
    @DisplayName("without an owner the dialog has no icons; a registry is required")
    void withoutOwner() throws InterruptedException {
        onFxThread(() -> {
            ShortcutsDialog dialog = new ShortcutsDialog(null, new App().registry);

            assertNull(window(dialog).getOwner());
            assertTrue(window(dialog).getIcons().isEmpty());
            assertThrows(NullPointerException.class, () -> new ShortcutsDialog(null, null));
        });
    }

    @Test
    @DisplayName("Cancel and Apply sit in the footer beside the counter, their texts following their properties")
    void footerButtons() throws InterruptedException {
        onFxThread(() -> {
            ShortcutsDialog dialog = new ShortcutsDialog(null, new App().registry);
            Parent footer = (Parent) root(dialog).lookup(".shortcuts-footer");
            Button cancel = (Button) footer.lookup(".shortcuts-dialog-cancel");
            Button apply = (Button) footer.lookup(".shortcuts-dialog-apply");

            assertEquals("Cancel", cancel.getText());
            assertEquals("Apply", apply.getText());
            assertTrue(cancel.isCancelButton(), "Esc cancels");
            assertFalse(apply.isDefaultButton(), "Enter records a key, it never applies");
            assertTrue(apply.getStyleClass().contains("primary-button"));

            dialog.setCancelText("Abbrechen");
            dialog.setApplyText("Anwenden");

            assertEquals("Abbrechen", cancel.getText());
            assertEquals("Anwenden", apply.getText());
            assertEquals("Abbrechen", dialog.getCancelText());
            assertEquals("Anwenden", dialog.getApplyText());
        });
    }

    @Test
    @DisplayName("Apply hands the keys to the registry")
    void applyApplies() throws InterruptedException {
        App app = new App();
        onFxThread(() -> {
            ShortcutsDialog dialog = new ShortcutsDialog(null, app.registry);
            root(dialog);
            ShortcutsEditor editor = dialog.getEditor();
            button(select(editor, "Save"), "shortcuts-remove").fire();
            assertTrue(editor.hasChanges());

            button(root(dialog), "shortcuts-dialog-apply").fire();

            assertNull(app.save.getShortcut());
            assertFalse(editor.hasChanges());
        });
    }

    @Test
    @DisplayName("Cancel drops the changes")
    void cancelDiscards() throws InterruptedException {
        App app = new App();
        onFxThread(() -> {
            ShortcutsDialog dialog = new ShortcutsDialog(null, app.registry);
            root(dialog);
            ShortcutsEditor editor = dialog.getEditor();
            button(select(editor, "Save"), "shortcuts-remove").fire();

            button(root(dialog), "shortcuts-dialog-cancel").fire();
            layout(root(dialog));

            assertEquals(Shortcut.of(KeyCode.S, CTRL), app.save.getShortcut());
            assertFalse(editor.hasChanges());
            assertShortcutColumn(editor, "Save", "[Ctrl] [S]");
        });
    }

    @Test
    @DisplayName("closing the window drops the changes, as Cancel does")
    void closingDiscards() throws InterruptedException {
        App app = new App();
        onFxThread(() -> {
            ShortcutsDialog dialog = new ShortcutsDialog(null, app.registry);
            root(dialog);
            ShortcutsEditor editor = dialog.getEditor();
            button(select(editor, "Save"), "shortcuts-remove").fire();

            window(dialog).fireEvent(new WindowEvent(window(dialog), WindowEvent.WINDOW_CLOSE_REQUEST));

            assertEquals(Shortcut.of(KeyCode.S, CTRL), app.save.getShortcut());
            assertFalse(editor.hasChanges());
        });
    }

    /**
     * JavaFX reports what goes wrong while it applies CSS to the public list of CSS errors, which the test watches
     * while the dialog's CSS is applied.
     */
    @Test
    @DisplayName("the theme styles the dialog without a CSS error")
    void themeStylesDialogWithoutError() throws InterruptedException {
        List<String> errors = new ArrayList<>();
        onFxThread(() -> {
            ListChangeListener<CssParser.ParseError> recorder = change -> {
                while (change.next()) {
                    for (CssParser.ParseError error : change.getAddedSubList()) {
                        errors.add(error.getMessage());
                    }
                }
            };
            ObservableList<CssParser.ParseError> cssErrors = CssParser.errorsProperty();
            cssErrors.addListener(recorder);
            try {
                ShortcutsDialog dialog = new ShortcutsDialog(null, new App().registry);
                root(dialog);
                select(dialog.getEditor(), "Save");
            } finally {
                cssErrors.removeListener(recorder);
            }
        });

        assertEquals(List.of(), errors, "CSS errors while styling the shortcut dialog");
    }

    /**
     * The window of a dialog, through its editor's scene.
     */
    private static Stage window(ShortcutsDialog dialog) {
        return (Stage) dialog.getEditor().getScene().getWindow();
    }

    /**
     * The root of a dialog's scene, laid out at the window's size, as it would be when shown.
     */
    private static Parent root(ShortcutsDialog dialog) {
        Parent root = dialog.getEditor().getScene().getRoot();
        ((Region) root).resize(760, 620);
        layout(root);
        return root;
    }
}
