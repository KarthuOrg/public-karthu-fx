package org.karthu.fx.form;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.karthu.fx.form.FormTestSupport.RecordingClipboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.form.FormTestSupport.altKeyPressed;
import static org.karthu.fx.form.FormTestSupport.bindIcon;
import static org.karthu.fx.form.FormTestSupport.chip;
import static org.karthu.fx.form.FormTestSupport.controlKeyPressed;
import static org.karthu.fx.form.FormTestSupport.firesThrough;
import static org.karthu.fx.form.FormTestSupport.hasPseudoClass;
import static org.karthu.fx.form.FormTestSupport.keyPressed;
import static org.karthu.fx.form.FormTestSupport.layOut;
import static org.karthu.fx.form.FormTestSupport.shortcutKeyPressed;

/**
 * The binding of an input to a variable, as a path input's frame shows it, through the public API and without a
 * window: the binding list is built and its actions fired, never shown.
 */
class FieldBindingTest {

    private PathInput field;
    private FieldBinding binding;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @BeforeEach
    void createField() {
        field = new PathInput();
        binding = field.getBinding();
    }

    @Test
    @DisplayName("a new input has one binding, unbound, not bindable, without candidates, and shows no {} icon")
    void newInputIsUnboundAndNotBindable() {
        assertSame(binding, ((FormInput<String>) field).getBinding());
        assertSame(binding, field.getBinding());
        assertFalse(binding.isBindable());
        assertNull(binding.getBoundName());
        assertFalse(binding.isBound());
        assertFalse(binding.isMissing());
        assertTrue(binding.getCandidates().isEmpty());
        assertFalse(bindIcon(field).isVisible());
        assertFalse(bindIcon(field).isManaged());
    }

    @Test
    @DisplayName("the {} icon shows while the binding is bindable or bound, as the last trailing icon")
    void bindIconShowsWhileBindableOrBound() {
        binding.setBindable(true);
        assertTrue(bindIcon(field).isVisible());
        assertTrue(bindIcon(field).isManaged());

        binding.setBindable(false);
        binding.setBoundName("inputFile");
        assertTrue(bindIcon(field).isVisible());

        binding.setBoundName(null);
        assertFalse(bindIcon(field).isVisible());
        assertFalse(bindIcon(field).isManaged());
        assertSame(bindIcon(field), field.getChildrenUnmodifiable().getLast());
    }

    @Test
    @DisplayName("the {} icon opens the binding list: one entry per candidate with its name and muted detail")
    void listHasOneEntryPerCandidate() {
        binding.setBindable(true);
        binding.getCandidates().addAll(
                new BindingCandidate("inputFile", "Path"),
                new BindingCandidate("outputFolder"));

        bindIcon(field).fire();

        ContextMenu menu = field.getBindingMenu();
        assertTrue(menu.getStyleClass().contains("form-binding-menu"));
        List<MenuItem> items = menu.getItems();
        assertEquals(2, items.size());
        CheckMenuItem first = (CheckMenuItem) items.get(0);
        CheckMenuItem second = (CheckMenuItem) items.get(1);
        assertTrue(first.getStyleClass().contains("form-binding-candidate"));
        assertEquals("inputFile", nameOf(first));
        assertEquals("Path", detailOf(first).getText());
        assertEquals("outputFolder", nameOf(second));
        assertNull(detailOf(second));
        assertFalse(first.isSelected());
        assertFalse(second.isSelected());
    }

    @Test
    @DisplayName("without candidates the list holds one disabled entry: No variable fits")
    void listWithoutCandidates() {
        binding.setBindable(true);

        bindIcon(field).fire();

        List<MenuItem> items = field.getBindingMenu().getItems();
        assertEquals(1, items.size());
        assertTrue(items.getFirst().getStyleClass().contains("form-binding-empty"));
        assertEquals("No variable fits", items.getFirst().getText());
        assertTrue(items.getFirst().isDisable());
    }

    @Test
    @DisplayName("while bound the current candidate is checked, and a separator and Remove binding follow")
    void listWhileBound() {
        binding.getCandidates().addAll(new BindingCandidate("inputFile", "Path"), new BindingCandidate("dataDir"));
        binding.setBoundName("dataDir");

        bindIcon(field).fire();

        List<MenuItem> items = field.getBindingMenu().getItems();
        assertEquals(4, items.size());
        assertFalse(((CheckMenuItem) items.get(0)).isSelected());
        assertTrue(((CheckMenuItem) items.get(1)).isSelected());
        assertInstanceOf(SeparatorMenuItem.class, items.get(2));
        assertTrue(items.get(3).getStyleClass().contains("form-binding-remove"));
        assertEquals("Remove binding", items.get(3).getText());
    }

    @Test
    @DisplayName("bound without candidates: No variable fits, then a separator and Remove binding")
    void listWhileBoundWithoutCandidates() {
        binding.setBoundName("gone");

        bindIcon(field).fire();

        List<MenuItem> items = field.getBindingMenu().getItems();
        assertEquals(3, items.size());
        assertEquals("No variable fits", items.get(0).getText());
        assertInstanceOf(SeparatorMenuItem.class, items.get(1));
        assertEquals("Remove binding", items.get(2).getText());
    }

    @Test
    @DisplayName("the list is built when it opens, from the candidates and texts of that moment")
    void listIsBuiltWhenItOpens() {
        binding.setBindable(true);
        bindIcon(field).fire();
        assertEquals("No variable fits", field.getBindingMenu().getItems().getFirst().getText());

        binding.setNoCandidatesText("Keine Variable passt");
        binding.getCandidates().add(new BindingCandidate("later"));
        assertEquals(1, field.getBindingMenu().getItems().size());

        bindIcon(field).fire();
        assertEquals("later", nameOf(field.getBindingMenu().getItems().getFirst()));
    }

    @Test
    @DisplayName("choosing an entry binds to its candidate, choosing another switches, Remove binding unbinds")
    void chooseSwitchAndRemoveThroughTheList() {
        binding.setBindable(true);
        binding.getCandidates().addAll(new BindingCandidate("inputFile", "Path"), new BindingCandidate("dataDir"));
        List<String> boundNames = new ArrayList<>();
        binding.boundNameProperty().addListener((observable, oldName, newName) -> boundNames.add(newName));

        bindIcon(field).fire();
        field.getBindingMenu().getItems().get(0).fire();
        assertEquals("inputFile", binding.getBoundName());

        bindIcon(field).fire();
        field.getBindingMenu().getItems().get(1).fire();
        assertEquals("dataDir", binding.getBoundName());

        bindIcon(field).fire();
        removeEntry().fire();
        assertNull(binding.getBoundName());
        assertEquals(Arrays.asList("inputFile", "dataDir", null), boundNames);
    }

    @Test
    @DisplayName("choosing the current entry again changes nothing")
    void chooseTheCurrentEntryAgain() {
        binding.getCandidates().add(new BindingCandidate("inputFile"));
        binding.setBoundName("inputFile");
        List<String> boundNames = new ArrayList<>();
        binding.boundNameProperty().addListener((observable, oldName, newName) -> boundNames.add(newName));

        bindIcon(field).fire();
        field.getBindingMenu().getItems().getFirst().fire();

        assertEquals("inputFile", binding.getBoundName());
        assertTrue(boundNames.isEmpty());
    }

    @Test
    @DisplayName("binding or unbinding through the list clears missing before the name changes, so the app decides last")
    void choosingClearsMissingBeforeTheNameChanges() {
        binding.getCandidates().add(new BindingCandidate("inputFile"));
        binding.setBoundName("gone");
        binding.setMissing(true);
        List<Boolean> missingWhenNamed = new ArrayList<>();
        binding.boundNameProperty().addListener(
                (observable, oldName, newName) -> missingWhenNamed.add(binding.isMissing()));

        bindIcon(field).fire();
        field.getBindingMenu().getItems().getFirst().fire();
        assertFalse(binding.isMissing());

        binding.setMissing(true);
        bindIcon(field).fire();
        removeEntry().fire();
        assertFalse(binding.isMissing());
        assertEquals(List.of(false, false), missingWhenNamed);
    }

    @Test
    @DisplayName("while bound the frame shows :bound and a chip with the name in place of the text, else neither")
    void chipWhileBound() {
        assertFalse(chip(field).isVisible());
        assertFalse(hasPseudoClass(field, "bound"));

        binding.setBoundName("inputFile");

        assertTrue(chip(field).isVisible());
        assertTrue(chip(field).isManaged());
        assertEquals("inputFile", chip(field).getText());
        assertTrue(hasPseudoClass(field, "bound"));
        Button remove = chipRemove();
        assertTrue(remove.getStyleClass().contains("form-chip-remove"));
        assertFalse(remove.isFocusTraversable());
        assertEquals("Remove binding", remove.getTooltip().getText());
        assertEquals("Remove binding", remove.getAccessibleText());

        binding.setBoundName("dataDir");
        assertEquals("dataDir", chip(field).getText());

        binding.setBoundName(null);
        assertFalse(chip(field).isVisible());
        assertFalse(chip(field).isManaged());
        assertFalse(hasPseudoClass(field, "bound"));
    }

    @Test
    @DisplayName("the chip's x unbinds, clearing missing first")
    void chipRemoveUnbinds() {
        binding.setBoundName("gone");
        binding.setMissing(true);

        chipRemove().fire();

        assertNull(binding.getBoundName());
        assertFalse(binding.isMissing());
        assertFalse(chip(field).isVisible());
    }

    @Test
    @DisplayName("a bindable field keeps its {} icon after the chip's x unbinds it, also from a missing variable")
    void bindableFieldKeepsItsIconAfterUnbinding() {
        binding.setBindable(true);
        binding.setBoundName("deletedVariable");
        binding.setMissing(true);

        chipRemove().fire();

        assertNull(binding.getBoundName());
        assertTrue(bindIcon(field).isVisible());
        assertTrue(bindIcon(field).isManaged());
        assertFalse(chip(field).isVisible());
    }

    @Test
    @DisplayName("binding keeps the value: hidden and not editable while bound, the same text again after unbinding")
    void valueIsKeptWhileBound() {
        field.setValue("files/data.csv");

        binding.setBoundName("inputFile");
        assertEquals("files/data.csv", field.getValue());
        assertNotNull(field.getTextField().getClip());
        assertFalse(field.getTextField().isEditable());

        chipRemove().fire();
        assertEquals("files/data.csv", field.getValue());
        assertEquals("files/data.csv", field.getTextField().getText());
        assertNull(field.getTextField().getClip());
        assertTrue(field.getTextField().isEditable());
    }

    @Test
    @DisplayName("Backspace or Delete in the field unbinds while bound and is consumed")
    void backspaceAndDeleteUnbind() {
        field.setValue("files/data.csv");
        binding.setBoundName("inputFile");
        binding.setMissing(true);

        KeyEvent backspace = keyPressed(KeyCode.BACK_SPACE);
        assertTrue(firesThrough(field, backspace).isEmpty());
        assertNull(binding.getBoundName());
        assertFalse(binding.isMissing());

        binding.setBoundName("inputFile");
        assertTrue(firesThrough(field, keyPressed(KeyCode.DELETE)).isEmpty());
        assertNull(binding.getBoundName());
        assertEquals("files/data.csv", field.getValue());
    }

    @Test
    @DisplayName("Backspace and Delete of an unbound field are the text field's own")
    void backspaceOfAnUnboundFieldIsNotTaken() {
        binding.setBindable(true);

        assertEquals(1, firesThrough(field, keyPressed(KeyCode.BACK_SPACE)).size());
        assertEquals(1, firesThrough(field, keyPressed(KeyCode.DELETE)).size());
        assertNull(binding.getBoundName());
    }

    @Test
    @DisplayName("Ctrl+Space opens the binding list while the {} icon shows, and is consumed")
    void ctrlSpaceOpensTheList() {
        binding.setBindable(true);
        binding.getCandidates().add(new BindingCandidate("inputFile"));

        assertTrue(firesThrough(field, controlKeyPressed(KeyCode.SPACE)).isEmpty());

        assertEquals("inputFile", nameOf(field.getBindingMenu().getItems().getFirst()));
    }

    @Test
    @DisplayName("Ctrl+Space does nothing while the {} icon is hidden; Space alone is the text field's")
    void ctrlSpaceWithoutTheIcon() {
        binding.getCandidates().add(new BindingCandidate("inputFile"));

        assertEquals(1, firesThrough(field, controlKeyPressed(KeyCode.SPACE)).size());
        assertTrue(field.getBindingMenu().getItems().isEmpty());

        binding.setBindable(true);
        assertEquals(1, firesThrough(field, keyPressed(KeyCode.SPACE)).size());
        assertTrue(field.getBindingMenu().getItems().isEmpty());
    }

    @Test
    @DisplayName("while bound the browse icon is disabled and neither it nor Alt+Down opens the chooser")
    void browseIsDisabledWhileBound() {
        List<PathChooser.Request> requests = new ArrayList<>();
        field.setChooser(request -> {
            requests.add(request);
            return Optional.empty();
        });
        binding.setBoundName("inputFile");

        Button browse = (Button) field.lookup(".form-path-browse");
        assertTrue(browse.isDisabled());
        browse.fire();
        field.getTextField().fireEvent(altKeyPressed(KeyCode.DOWN));
        assertTrue(requests.isEmpty());
        assertFalse(bindIcon(field).isDisabled());

        binding.setBoundName(null);
        assertFalse(browse.isDisabled());
        browse.fire();
        assertEquals(1, requests.size());
    }

    @Test
    @DisplayName("binding leaves an application's own icons as the application set them, also a bound disable")
    void bindingLeavesTheApplicationsIcons() {
        FieldFrame frame = new FieldFrame();
        Button enabled = new Button();
        Button disabled = new Button();
        disabled.setDisable(true);
        Button followsTheApplication = new Button();
        BooleanProperty applicationDisables = new SimpleBooleanProperty(false);
        followsTheApplication.disableProperty().bind(applicationDisables);
        frame.getIcons().addAll(enabled, disabled, followsTheApplication);

        frame.getBinding().setBoundName("inputFile");
        assertFalse(enabled.isDisable());
        assertTrue(disabled.isDisable());
        applicationDisables.set(true);

        frame.getBinding().setBoundName(null);
        assertFalse(enabled.isDisable());
        assertTrue(disabled.isDisable());
        assertTrue(followsTheApplication.isDisable());
    }

    @Test
    @DisplayName("missing: the chip shows :missing and the missing tooltip, from a text property; else neither")
    void missingChip() {
        binding.setBoundName("gone");
        assertFalse(hasPseudoClass(chip(field), "missing"));
        assertNull(chip(field).getTooltip());

        binding.setMissing(true);
        assertTrue(hasPseudoClass(chip(field), "missing"));
        assertEquals("This variable does not exist", chip(field).getTooltip().getText());

        binding.setMissingTooltipText("Diese Variable gibt es nicht");
        assertEquals("Diese Variable gibt es nicht", chip(field).getTooltip().getText());

        binding.setMissing(false);
        assertFalse(hasPseudoClass(chip(field), "missing"));
        assertNull(chip(field).getTooltip());
    }

    @Test
    @DisplayName("texts: the {} icon's tooltip names Ctrl+Space, and every text follows its property")
    void textsAreProperties() {
        binding.setBindable(true);
        assertEquals("Bind to a variable (Ctrl+Space)", bindIcon(field).getTooltip().getText());
        assertEquals("Bind to a variable (Ctrl+Space)", bindIcon(field).getAccessibleText());
        assertFalse(bindIcon(field).isFocusTraversable());
        assertTrue(bindIcon(field).getStyleClass().containsAll(List.of("icon-button", "form-frame-icon", "form-frame-bind")));

        binding.setBindTooltipText("An Variable binden (Strg+Leertaste)");
        binding.setRemoveText("Bindung entfernen");
        binding.setBoundName("inputFile");
        bindIcon(field).fire();

        assertEquals("An Variable binden (Strg+Leertaste)", bindIcon(field).getTooltip().getText());
        assertEquals("Bindung entfernen", removeEntry().getText());
        assertEquals("Bindung entfernen", chipRemove().getTooltip().getText());
    }

    @Test
    @DisplayName("bound, the frame stays as high as a text field: the chip inside it where the text starts, shortened")
    void boundFrameLayout() {
        TextField plain = new TextField("plain");
        field.setPrefWidth(300);
        field.setValue("files/data.csv");
        binding.setBindable(true);
        binding.setBoundName("aVariableWithAVeryLongNameThatCannotFitIntoTheFieldAtAllNotEvenWhenItIsWide");
        layOut(plain, field);

        assertEquals(300, field.getWidth(), 0.01);
        assertEquals(plain.getHeight(), field.getHeight(), 0.01);
        Bounds chipBounds = chip(field).getBoundsInParent();
        Bounds browseBounds = field.lookup(".form-path-browse").getBoundsInParent();
        Bounds bindBounds = bindIcon(field).getBoundsInParent();
        // where the text starts, centred, a little room above and below
        assertEquals(field.getTextField().getPadding().getLeft(), chipBounds.getMinX(), 0.01);
        assertTrue(chipBounds.getMinY() >= 2 && chipBounds.getMaxY() <= field.getHeight() - 2);
        assertEquals(field.getHeight() - chipBounds.getMaxY(), chipBounds.getMinY(), 1);
        // left of the icons, the name shortened to fit
        assertTrue(chipBounds.getMaxX() <= browseBounds.getMinX());
        assertTrue(chip(field).getWidth() < chip(field).prefWidth(-1));
        assertTrue(browseBounds.getMaxX() <= bindBounds.getMinX());
        assertTrue(bindBounds.getMaxX() <= field.getWidth());
    }

    @Test
    @DisplayName("bound, the text field stays the focus target: a press on the chip gives it the focus, the x does not")
    void boundFieldKeepsItsFocusTarget() {
        binding.setBoundName("inputFile");
        TextField other = new TextField();
        layOut(other, field);
        Scene scene = field.getScene();
        other.requestFocus();

        chip(field).fireEvent(mousePressed());
        assertSame(field.getTextField(), scene.getFocusOwner());

        other.requestFocus();
        chipRemove().fireEvent(mousePressed());
        bindIcon(field).fireEvent(mousePressed());
        assertSame(other, scene.getFocusOwner());

        field.requestFocus();
        assertSame(field.getTextField(), scene.getFocusOwner());
        assertTrue(field.getTextField().isVisible());
    }

    @Test
    @DisplayName("disabled and bound: the frame is dimmed once, chip and icons inside it at full opacity")
    void disabledBoundLook() {
        binding.setBoundName("inputFile");
        field.setDisable(true);
        layOut(field);

        assertTrue(field.getOpacity() < 1);
        assertEquals(1, chip(field).getOpacity(), 0.001);
        assertEquals(1, chipRemove().getOpacity(), 0.001);
        assertEquals(1, bindIcon(field).getOpacity(), 0.001);
        assertTrue(chipRemove().isDisabled());
        assertTrue(bindIcon(field).isDisabled());
    }

    @Test
    @DisplayName("a disabled field takes no binding keys")
    void disabledFieldTakesNoBindingKeys() {
        binding.setBindable(true);
        binding.setBoundName("inputFile");
        field.setDisable(true);

        assertEquals(1, firesThrough(field, keyPressed(KeyCode.BACK_SPACE)).size());
        assertEquals(1, firesThrough(field, controlKeyPressed(KeyCode.SPACE)).size());
        assertEquals("inputFile", binding.getBoundName());
        assertTrue(field.getBindingMenu().getItems().isEmpty());
    }

    @Test
    @DisplayName("bound, the hidden path is not handed out: select-all selects nothing, copy and cut give nothing")
    void hiddenValueIsNotHandedOut() {
        RecordingClipboard clipboard = new RecordingClipboard(null);
        field.setClipboard(clipboard);
        field.setValue("files/data.csv");
        field.getTextField().selectRange(0, 5);

        binding.setBoundName("inputFile");
        assertEquals("", field.getTextField().getSelectedText());
        field.getTextField().selectAll();
        assertEquals("", field.getTextField().getSelectedText());
        // A selection made another way, such as Shift+Home, is neither copied nor cut.
        field.getTextField().selectRange(0, 5);
        field.getTextField().copy();
        field.getTextField().cut();
        assertTrue(clipboard.copied().isEmpty());
        assertEquals("files/data.csv", field.getValue());

        binding.setBoundName(null);
        field.getTextField().selectAll();
        field.getTextField().copy();
        assertEquals(List.of("files/data.csv"), clipboard.copied());
    }

    @Test
    @DisplayName("bound, the shortcuts select all and copy give nothing, through the text field's own key handling")
    void selectAllAndCopyShortcutsGiveNothingWhileBound() {
        RecordingClipboard clipboard = new RecordingClipboard(null);
        field.setClipboard(clipboard);
        field.setValue("files/data.csv");
        binding.setBoundName("inputFile");
        // The text field gets its skin, and with it its key handling, in a CSS pass.
        layOut(field);

        field.getTextField().fireEvent(shortcutKeyPressed(KeyCode.A));
        field.getTextField().fireEvent(shortcutKeyPressed(KeyCode.C));

        assertEquals("", field.getTextField().getSelectedText());
        assertTrue(clipboard.copied().isEmpty());

        binding.setBoundName(null);
        field.getTextField().fireEvent(shortcutKeyPressed(KeyCode.A));
        field.getTextField().fireEvent(shortcutKeyPressed(KeyCode.C));
        assertEquals(List.of("files/data.csv"), clipboard.copied());
    }

    @Test
    @DisplayName("a binding list built before the field was disabled binds, switches and unbinds nothing once disabled")
    void bindingListDoesNothingOnceDisabled() {
        binding.getCandidates().addAll(new BindingCandidate("inputFile"), new BindingCandidate("dataDir"));
        binding.setBoundName("inputFile");
        binding.setMissing(true);
        bindIcon(field).fire();
        List<MenuItem> items = new ArrayList<>(field.getBindingMenu().getItems());

        field.setDisable(true);
        items.get(1).fire();
        items.getLast().fire();

        assertEquals("inputFile", binding.getBoundName());
        assertTrue(binding.isMissing());

        field.setDisable(false);
        items.get(1).fire();
        assertEquals("dataDir", binding.getBoundName());
    }

    @Test
    @DisplayName("a binding list built inside a disabled parent binds nothing either")
    void bindingListDoesNothingInADisabledParent() {
        binding.setBindable(true);
        binding.getCandidates().add(new BindingCandidate("inputFile"));
        bindIcon(field).fire();
        MenuItem candidate = field.getBindingMenu().getItems().getFirst();
        VBox parent = new VBox(field);

        parent.setDisable(true);
        candidate.fire();

        assertNull(binding.getBoundName());
    }

    private MenuItem removeEntry() {
        for (MenuItem item : field.getBindingMenu().getItems()) {
            if (item.getStyleClass().contains("form-binding-remove")) {
                return item;
            }
        }
        throw new AssertionError("no Remove binding entry");
    }

    /** The chip's remove button, its graphic (a graphic joins the chip's children only with a skin). */
    private Button chipRemove() {
        return (Button) chip(field).getGraphic();
    }

    private static MouseEvent mousePressed() {
        return new MouseEvent(MouseEvent.MOUSE_PRESSED, 1, 1, 1, 1, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null);
    }

    /** The candidate's name, as its entry shows it. */
    private static String nameOf(MenuItem item) {
        return ((Label) item.getGraphic().lookup(".form-binding-name")).getText();
    }

    /** The candidate's detail label, muted after the name. */
    private static Label detailOf(MenuItem item) {
        return (Label) item.getGraphic().lookup(".form-binding-detail");
    }
}
