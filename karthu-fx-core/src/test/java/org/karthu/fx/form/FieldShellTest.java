package org.karthu.fx.form;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.bindIcon;
import static org.karthu.fx.form.FormTestSupport.chip;
import static org.karthu.fx.form.FormTestSupport.controlKeyPressed;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;

/**
 * The shell through the API a field type builds on, without a window: field types of the test's own put a content
 * that is not a text field, own controls and a layout of their own into the shell, as the inputs on the shell do.
 */
class FieldShellTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a type's content is the shell's first child, carries form-shell-content and is its focus target")
    void contentIsTheFocusTarget() {
        ChoiceShell shell = new ChoiceShell();
        Scene scene = new Scene(new VBox(new TextField(), shell));

        assertTrue(shell.getStyleClass().contains("form-frame"));
        assertTrue(shell.choice.getStyleClass().contains("form-shell-content"));
        assertSame(shell.choice, shell.getChildrenUnmodifiable().getFirst());
        assertSame(shell.choice, shell.getFocusTarget());

        shell.requestFocus();
        assertSame(shell.choice, scene.getFocusOwner());
    }

    @Test
    @DisplayName("the content fills the shell left of the trailing nodes, the shell grows in width only")
    void contentFillsTheShell() {
        ChoiceShell shell = new ChoiceShell();
        Button icon = new Button();
        shell.getIcons().add(icon);
        shell.setPrefWidth(300);
        layOut(shell);

        Bounds content = shell.choice.getBoundsInParent();
        assertEquals(0, content.getMinX(), 0.01);
        assertEquals(icon.getBoundsInParent().getMinX(), content.getMaxX(), 0.01);
        assertEquals(shell.getHeight(), content.getHeight(), 0.01);
        assertEquals(shell.prefHeight(-1), shell.maxHeight(-1), 0.01);
    }

    @Test
    @DisplayName("the type's own controls sit between the icons and {}, which stays last, out of the tab order")
    void ownControlsSitBetweenIconsAndBind() {
        ChoiceShell shell = new ChoiceShell();
        Button icon = new Button();
        Button chevron = new Button();
        shell.getIcons().add(icon);
        shell.addOwnControl(chevron);
        shell.getBinding().setBindable(true);
        shell.setPrefWidth(300);
        layOut(shell);

        List<Node> children = shell.getChildrenUnmodifiable();
        assertTrue(children.indexOf(icon) < children.indexOf(chevron));
        assertSame(bindIcon(shell), children.getLast());
        assertFalse(chevron.isFocusTraversable());
        assertTrue(icon.getBoundsInParent().getMaxX() <= chevron.getBoundsInParent().getMinX());
        assertTrue(chevron.getBoundsInParent().getMaxX() <= bindIcon(shell).getBoundsInParent().getMinX());
    }

    @Test
    @DisplayName("the shell's divider is 1px wide and as high as the content; a button stays at its own height, centred")
    void dividerIsAsHighAsTheContent() {
        ChoiceShell shell = new ChoiceShell();
        Region divider = FieldShell.createDivider();
        Button chevron = new Button();
        shell.addOwnControl(divider);
        shell.addOwnControl(chevron);
        shell.setPrefWidth(300);
        layOut(shell);

        assertTrue(divider.getStyleClass().contains("form-shell-divider"));
        assertFalse(divider.isFocusTraversable());
        double contentHeight = shell.getHeight() - shell.snappedTopInset() - shell.snappedBottomInset();
        assertEquals(1, divider.getWidth(), 0.01);
        assertEquals(contentHeight, divider.getHeight(), 0.01);
        Bounds chevronBounds = chevron.getBoundsInParent();
        assertEquals(chevron.prefHeight(-1), chevronBounds.getHeight(), 0.01);
        assertEquals(shell.getHeight() / 2, (chevronBounds.getMinY() + chevronBounds.getMaxY()) / 2, 1);
    }

    @Test
    @DisplayName("read-only: the icons leave the shell, the own controls stay as the type set them")
    void readOnlyLeavesTheOwnControlsToTheType() {
        ChoiceShell shell = new ChoiceShell();
        Button icon = new Button();
        Button grip = new Button();
        shell.getIcons().add(icon);
        shell.addOwnControl(grip);

        shell.setReadOnly(true);

        assertFalse(shell.getChildrenUnmodifiable().contains(icon));
        assertSame(shell, grip.getParent());
        assertTrue(grip.isVisible());
    }

    @Test
    @DisplayName("disabled: the shell is dimmed as a disabled button, any content inside it once")
    void disabledShellDimsAnyContentOnce() {
        ChoiceShell shell = new ChoiceShell();
        shell.setDisable(true);
        ComboBox<String> plain = new ComboBox<>();
        plain.setDisable(true);
        Button button = new Button("button");
        button.setDisable(true);
        layOut(shell, plain, button);

        assertTrue(plain.getOpacity() < 1);
        assertEquals(button.getOpacity(), shell.getOpacity(), 0.001);
        assertEquals(1, shell.choice.getOpacity(), 0.001);
    }

    @Test
    @DisplayName("the editing hook hears whether the user may edit: not while read-only, not while bound")
    void editingHookFollowsReadOnlyAndBinding() {
        ChoiceShell shell = new ChoiceShell();
        assertEquals(true, shell.editableCalls.getLast());

        shell.setReadOnly(true);
        assertEquals(false, shell.editableCalls.getLast());
        shell.setReadOnly(false);
        assertEquals(true, shell.editableCalls.getLast());
        shell.getBinding().setBoundName("choice");
        assertEquals(false, shell.editableCalls.getLast());
        shell.setReadOnly(true);
        shell.getBinding().setBoundName(null);
        assertEquals(false, shell.editableCalls.getLast());
        shell.setReadOnly(false);
        assertEquals(true, shell.editableCalls.getLast());
    }

    @Test
    @DisplayName("bound: the shell hides any content behind its chip, and shows it again after unbinding")
    void boundHidesAnyContent() {
        ChoiceShell shell = new ChoiceShell();
        shell.choice.setValue("kept");

        assertEquals(false, shell.hiddenCalls.getLast());

        shell.getBinding().setBoundName("choice");
        assertNotNull(shell.choice.getClip());
        assertTrue(chip(shell).isVisible());
        assertEquals("kept", shell.choice.getValue());
        assertEquals(true, shell.hiddenCalls.getLast());

        shell.getBinding().setBoundName(null);
        assertNull(shell.choice.getClip());
        assertEquals(false, shell.hiddenCalls.getLast());
    }

    @Test
    @DisplayName("a type may swap its content: the focus target and the hiding move to the new content")
    void swappedContentTakesFocusAndHiding() {
        SwapShell shell = new SwapShell();
        Scene scene = new Scene(new VBox(new TextField(), shell));
        shell.getBinding().setBoundName("secret");

        shell.swap();

        assertSame(shell.second, shell.getFocusTarget());
        assertTrue(shell.getChildrenUnmodifiable().contains(shell.second));
        assertFalse(shell.getChildrenUnmodifiable().contains(shell.first));
        assertNull(shell.first.getClip());
        assertNotNull(shell.second.getClip());
        assertFalse(shell.second.isEditable());
        shell.requestFocus();
        assertSame(shell.second, scene.getFocusOwner());
    }

    @Test
    @DisplayName("a swapped content takes the focus when the content before had it, and only then")
    void swappedContentCarriesTheFocus() {
        SwapShell shell = new SwapShell();
        TextField other = new TextField();
        Scene scene = new Scene(new VBox(other, shell));

        shell.requestFocus();
        shell.swap();
        assertSame(shell.second, scene.getFocusOwner());

        other.requestFocus();
        shell.swapBack();
        assertSame(other, scene.getFocusOwner());
    }

    @Test
    @DisplayName("a focus target of the type's own takes the focus, the content stays the first child")
    void focusTargetOfTheTypesOwn() {
        TextField inner = new TextField();
        ChoiceShell shell = new ChoiceShell();
        shell.useFocusTarget(inner);

        assertSame(inner, shell.getFocusTarget());
        assertSame(shell.choice, shell.getChildrenUnmodifiable().getFirst());

        shell.useFocusTarget(null);
        assertSame(shell.choice, shell.getFocusTarget());
    }

    @Test
    @DisplayName("Ctrl+Space and Backspace work at a content that is no text field")
    void bindingKeysAtAnyContent() {
        ChoiceShell shell = new ChoiceShell();
        shell.getBinding().setBindable(true);
        shell.getBinding().getCandidates().add(new BindingCandidate("choice"));

        assertTrue(firesThrough(shell, controlKeyPressed(KeyCode.SPACE)).isEmpty());
        assertEquals(1, shell.getBindingMenu().getItems().size());

        shell.getBinding().setBoundName("choice");
        assertTrue(firesThrough(shell, keyPressed(KeyCode.BACK_SPACE)).isEmpty());
        assertNull(shell.getBinding().getBoundName());
    }

    @Test
    @DisplayName("a type's own layout: {} and the chip at the top of a tall content, the content below the band")
    void layoutOfTheTypesOwn() {
        TallShell shell = new TallShell();
        shell.getBinding().setBoundName("description");
        TextField plain = new TextField("plain");
        shell.setPrefWidth(300);
        layOut(plain, shell);

        assertTrue(shell.getHeight() > 2 * plain.getHeight());
        Bounds bind = bindIcon(shell).getBoundsInParent();
        assertTrue(bind.getMaxY() <= plain.getHeight());
        assertEquals(shell.getWidth() - shell.snappedRightInset(), bind.getMaxX(), 0.01);
        Bounds chipBounds = chip(shell).getBoundsInParent();
        assertTrue(chipBounds.getMaxY() <= plain.getHeight());
        assertTrue(shell.area.getBoundsInParent().getMaxX() <= bind.getMinX());
    }

    // --- Leading graphic -----------------------------------------------------------------------------

    @Test
    @DisplayName("a leading graphic sits before the content, centred vertically, the content starting after it")
    void leadingGraphicSitsBeforeTheContent() {
        ChoiceShell shell = new ChoiceShell();
        Region graphic = fixedGraphic();
        Button icon = new Button();
        shell.getIcons().add(icon);
        shell.useLeadingGraphic(graphic);
        shell.setPrefWidth(300);
        layOut(shell);

        assertTrue(graphic.getStyleClass().contains("form-shell-leading"));
        assertSame(graphic, shell.getLeading());
        assertSame(graphic, shell.getChildrenUnmodifiable().getFirst());
        Bounds graphicBounds = graphic.getBoundsInParent();
        Bounds content = shell.choice.getBoundsInParent();
        assertEquals(shell.snappedLeftInset(), graphicBounds.getMinX(), 0.01);
        assertEquals(10, graphicBounds.getHeight(), 0.01);
        assertEquals(shell.getHeight() / 2, (graphicBounds.getMinY() + graphicBounds.getMaxY()) / 2, 1);
        assertEquals(graphicBounds.getMaxX(), content.getMinX(), 0.01);
        assertEquals(icon.getBoundsInParent().getMinX(), content.getMaxX(), 0.01);
        assertEquals(shell.getHeight(), content.getHeight(), 0.01);
    }

    @Test
    @DisplayName("the leading graphic is no control: out of the tab order, clicks pass it, a press on it focuses the field")
    void leadingGraphicIsNoControl() {
        ChoiceShell shell = new ChoiceShell();
        Region graphic = fixedGraphic();
        shell.useLeadingGraphic(graphic);
        TextField other = new TextField();
        Scene scene = new Scene(new VBox(other, shell));
        other.requestFocus();

        graphic.fireEvent(mousePressed());

        assertFalse(graphic.isFocusTraversable());
        assertTrue(graphic.isMouseTransparent());
        assertSame(shell.choice, scene.getFocusOwner());
    }

    @Test
    @DisplayName("without a leading graphic the shell lays out as before: a graphic set and removed leaves no trace")
    void withoutLeadingGraphicNothingChanges() {
        ChoiceShell plain = new ChoiceShell();
        ChoiceShell removed = new ChoiceShell();
        removed.useLeadingGraphic(fixedGraphic());
        removed.useLeadingGraphic(null);
        Button plainIcon = new Button();
        Button removedIcon = new Button();
        plain.getIcons().add(plainIcon);
        removed.getIcons().add(removedIcon);
        layOut(plain, removed);

        assertNull(removed.getLeading());
        assertSame(removed.choice, removed.getChildrenUnmodifiable().getFirst());
        assertEquals(plain.getChildrenUnmodifiable().size(), removed.getChildrenUnmodifiable().size());
        assertEquals(0, removed.choice.getBoundsInParent().getMinX(), 0.01);
        assertEquals(plain.getWidth(), removed.getWidth(), 0.01);
        assertEquals(plain.getHeight(), removed.getHeight(), 0.01);
        assertEquals(plain.choice.getBoundsInParent(), removed.choice.getBoundsInParent());
        assertEquals(plainIcon.getBoundsInParent(), removedIcon.getBoundsInParent());
    }

    @Test
    @DisplayName("the leading graphic stays while read-only and bound, and greys out once with a disabled shell")
    void leadingGraphicInEveryState() {
        ChoiceShell readOnly = new ChoiceShell();
        Region readOnlyGraphic = fixedGraphic();
        readOnly.useLeadingGraphic(readOnlyGraphic);
        readOnly.setReadOnly(true);
        ChoiceShell bound = new ChoiceShell();
        Region boundGraphic = fixedGraphic();
        bound.useLeadingGraphic(boundGraphic);
        bound.getBinding().setBoundName("choice");
        ChoiceShell disabled = new ChoiceShell();
        Region disabledGraphic = fixedGraphic();
        disabled.useLeadingGraphic(disabledGraphic);
        disabled.setDisable(true);
        Button button = new Button("button");
        button.setDisable(true);
        layOut(readOnly, bound, disabled, button);

        assertSame(readOnly, readOnlyGraphic.getParent());
        assertSame(bound, boundGraphic.getParent());
        assertTrue(readOnlyGraphic.isVisible() && boundGraphic.isVisible());
        // Bound, the chip lies over the content where its text starts: right of the graphic.
        assertTrue(chip(bound).getBoundsInParent().getMinX() >= boundGraphic.getBoundsInParent().getMaxX());
        assertTrue(disabledGraphic.isDisabled());
        assertEquals(button.getOpacity(), disabled.getOpacity(), 0.001);
        assertEquals(1, disabledGraphic.getOpacity(), 0.001);
    }

    // --- Field types of the test's own ---------------------------------------------------------------

    /** A press of the primary button, as a click on a node starts. */
    private static MouseEvent mousePressed() {
        return new MouseEvent(MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, true, null);
    }

    /** A graphic of a fixed size, 10 by 10, whatever padding the theme gives a leading graphic. */
    private static Region fixedGraphic() {
        Region graphic = new Region();
        graphic.setMinSize(10, 10);
        graphic.setPrefSize(10, 10);
        graphic.setMaxSize(10, 10);
        return graphic;
    }

    /** A choice in the shell, as a combo input puts one in, recording what the shell asks of it. */
    private static final class ChoiceShell extends FieldShell {

        private final ComboBox<String> choice = new ComboBox<>();
        private final List<Boolean> editableCalls = new ArrayList<>();
        private final List<Boolean> hiddenCalls = new ArrayList<>();

        private ChoiceShell() {
            setContent(choice);
        }

        private void addOwnControl(Node control) {
            getOwnControls().add(control);
        }

        private void useFocusTarget(Node target) {
            setFocusTarget(target);
        }

        private void useLeadingGraphic(Node graphic) {
            setLeadingGraphic(graphic);
        }

        private Node getLeading() {
            return getLeadingGraphic();
        }

        @Override
        protected void updateContentEditable(boolean editable) {
            editableCalls.add(editable);
        }

        @Override
        protected void updateContentHidden(boolean hidden) {
            super.updateContentHidden(hidden);
            hiddenCalls.add(hidden);
        }
    }

    /** Two text fields, one shown at a time, as a password input swaps its masked and plain fields. */
    private static final class SwapShell extends FieldShell {

        private final TextField first = new TextField();
        private final TextField second = new TextField();

        private SwapShell() {
            setContent(first);
        }

        private void swap() {
            setContent(second);
        }

        private void swapBack() {
            setContent(first);
        }
    }

    /**
     * A text area in the shell with the icons at the top, as a text area input lays it out: the trailing nodes and
     * the chip in a band at the top, as high as the tallest trailing node, the area as high as its rows.
     */
    private static final class TallShell extends FieldShell {

        private final TextArea area = new TextArea();

        private TallShell() {
            area.setPrefRowCount(4);
            setContent(area);
        }

        /** The band of the icons at the top. */
        private double bandHeight() {
            return trailingPrefHeight(trailingNodes());
        }

        @Override
        protected void layoutChildren() {
            double left = snappedLeftInset();
            double top = snappedTopInset();
            double contentHeight = getHeight() - top - snappedBottomInset();
            double right = layoutTrailing(trailingNodes(), top, bandHeight());
            area.resizeRelocate(left, top, Math.max(0, right - left), contentHeight);
            layoutChip(left, top, right, bandHeight());
        }

        @Override
        protected double computePrefHeight(double width) {
            return snappedTopInset() + snapSizeY(area.prefHeight(-1)) + snappedBottomInset();
        }

        @Override
        protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }

        @Override
        protected double computeMaxHeight(double width) {
            return computePrefHeight(width);
        }
    }
}
