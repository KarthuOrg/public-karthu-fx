package org.karthu.fx.form;

import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.theme.ThemeManager;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.layOut;

/**
 * The frame through its public API: its icons, its layout and its look in the theme, in a scene that is never shown.
 */
class FieldFrameTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @Test
    @DisplayName("an added icon is styled as a trailing icon and leaves the tab order")
    void addedIconIsStyled() {
        FieldFrame frame = new FieldFrame();
        Button icon = new Button("x");

        frame.getIcons().add(icon);

        assertTrue(icon.getStyleClass().containsAll(List.of("icon-button", "form-frame-icon")));
        assertFalse(icon.isFocusTraversable());
        assertSame(frame, icon.getParent());
        assertTrue(frame.getStyleClass().contains("form-frame"));
        assertTrue(frame.getTextField().getStyleClass().containsAll(List.of("form-frame-text", "form-shell-content")));
        assertSame(frame.getTextField(), frame.getFocusTarget());
    }

    @Test
    @DisplayName("a frame is exactly as high as a plain text field in the theme, with and without an icon")
    void frameIsAsHighAsATextField() {
        TextField plain = new TextField("plain");
        FieldFrame bare = new FieldFrame();
        PathInput pathInput = new PathInput();
        layOut(plain, bare, pathInput);

        assertTrue(plain.getHeight() > 0);
        assertEquals(plain.getHeight(), bare.getHeight(), 0.01);
        assertEquals(plain.getHeight(), pathInput.getHeight(), 0.01);
    }

    @Test
    @DisplayName("a skin that changes the text field's padding changes the frame's alike: still as high as a text field")
    void textFieldPaddingCarriesOverToTheFrame() {
        TextField plain = new TextField("plain");
        PathInput pathInput = new PathInput();
        VBox root = new VBox(plain, pathInput);
        root.setFillWidth(false);
        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        String skin = ".text-field { -fx-padding: 9; }";
        scene.getStylesheets().add("data:text/css;base64,"
                + Base64.getEncoder().encodeToString(skin.getBytes(StandardCharsets.UTF_8)));
        root.applyCss();
        root.autosize();
        root.layout();

        assertEquals(9, plain.getPadding().getTop(), 0.01);
        assertEquals(plain.getHeight(), pathInput.getHeight(), 0.01);
    }

    @Test
    @DisplayName("the icons sit at the right end in their order, inside the frame, the text field left of them")
    void iconsSitAtTheRightEnd() {
        FieldFrame frame = new FieldFrame();
        Button first = new Button();
        Button second = new Button();
        frame.getIcons().addAll(first, second);
        frame.setPrefWidth(300);
        layOut(frame);

        Bounds text = frame.getTextField().getBoundsInParent();
        Bounds firstBounds = first.getBoundsInParent();
        Bounds secondBounds = second.getBoundsInParent();
        assertEquals(0, text.getMinX(), 0.01);
        assertTrue(text.getMaxX() <= firstBounds.getMinX());
        assertTrue(firstBounds.getMaxX() <= secondBounds.getMinX());
        assertTrue(secondBounds.getMaxX() <= frame.getWidth());
        assertTrue(secondBounds.getMaxX() > frame.getWidth() - 6);
        assertTrue(firstBounds.getMinY() >= 0 && firstBounds.getMaxY() <= frame.getHeight());
    }

    @Test
    @DisplayName("an icon that is not managed takes no room: the text field reaches the frame's right end")
    void unmanagedIconTakesNoRoom() {
        FieldFrame frame = new FieldFrame();
        Button hidden = new Button();
        hidden.setManaged(false);
        hidden.setVisible(false);
        frame.getIcons().add(hidden);
        frame.setPrefWidth(300);
        layOut(frame);

        // No trailing node, so no room after one: the right padding is left out.
        assertEquals(frame.getWidth(), frame.getTextField().getBoundsInParent().getMaxX(), 0.01);
    }

    @Test
    @DisplayName("focusing the frame focuses its text field")
    void requestFocusGoesToTheTextField() {
        FieldFrame frame = new FieldFrame();
        Scene scene = new Scene(new VBox(new TextField(), frame));

        frame.requestFocus();

        assertSame(frame.getTextField(), scene.getFocusOwner());
    }

    @Test
    @DisplayName("disabled: a plain text field and a frame are dimmed as a disabled button, the frame's inside once")
    void disabledLook() {
        TextField plain = new TextField("plain");
        plain.setDisable(true);
        PathInput pathInput = new PathInput();
        pathInput.setDisable(true);
        Button button = new Button("button");
        button.setDisable(true);
        layOut(plain, pathInput, button);

        assertEquals(button.getOpacity(), plain.getOpacity(), 0.001);
        assertEquals(button.getOpacity(), pathInput.getOpacity(), 0.001);
        assertTrue(pathInput.getOpacity() < 1);
        // Opacity multiplies down the scene graph: the inside keeps full opacity, or it would be dimmed twice.
        assertEquals(1, pathInput.getTextField().getOpacity(), 0.001);
        assertEquals(1, pathInput.lookup(".form-path-browse").getOpacity(), 0.001);
    }

    @Test
    @DisplayName("a plain frame pastes the clipboard's text unchanged, quotes included, in place of the selection")
    void plainFramePastesUnchanged() {
        FieldFrame frame = new FieldFrame();
        frame.getTextField().setText("before after");
        frame.getTextField().selectRange(6, 7);
        // A stand-in for the clipboard: no test reads or writes the clipboard of the machine it runs on.
        frame.setClipboard(new RecordingClipboard(" \"quoted\" "));

        frame.getTextField().paste();

        assertEquals("before \"quoted\" after", frame.getTextField().getText());
    }

    @Test
    @DisplayName("copy puts the selected text on the clipboard, cut also removes it; nothing selected copies nothing")
    void copyAndCutTheSelection() {
        FieldFrame frame = new FieldFrame();
        RecordingClipboard clipboard = new RecordingClipboard(null);
        frame.setClipboard(clipboard);
        frame.getTextField().setText("files/data.csv");

        frame.getTextField().copy();
        frame.getTextField().selectRange(6, 10);
        frame.getTextField().copy();
        frame.getTextField().cut();

        assertEquals(List.of("data", "data"), clipboard.copied());
        assertEquals("files/.csv", frame.getTextField().getText());
    }

    @Test
    @DisplayName("the frame owns :drop-target: an input built on it shows and ends the drop-target look")
    void dropTargetIsTheFrames() {
        FieldFrame frame = new FieldFrame();
        assertFalse(hasPseudoClass(frame, "drop-target"));

        frame.setDropTarget(true);
        assertTrue(hasPseudoClass(frame, "drop-target"));

        frame.setDropTarget(false);
        assertFalse(hasPseudoClass(frame, "drop-target"));
    }

    @Test
    @DisplayName("a disabled frame pastes and cuts nothing, also when called by code")
    void disabledFramePastesAndCutsNothing() {
        FieldFrame frame = new FieldFrame();
        frame.setClipboard(new RecordingClipboard("pasted"));
        frame.getTextField().setText("kept");
        frame.setDisable(true);

        frame.getTextField().selectAll();
        frame.getTextField().paste();
        frame.getTextField().cut();

        assertEquals("kept", frame.getTextField().getText());
    }
}
