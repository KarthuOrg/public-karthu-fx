package org.karthu.fx.settings;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssParser;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import org.karthu.fx.form.PathFilter;
import org.karthu.fx.form.PathInput;
import org.karthu.fx.form.PathMode;
import org.karthu.fx.theme.Appearance;
import org.karthu.fx.theme.ThemeManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A path setting in the settings dialog: shown with a {@link PathInput} in the entry's mode and with its filters, the
 * stored value shown, a changed value saved. The dialog's scene is built and styled, never shown.
 */
class SettingsDialogPathTest {

    private static final List<PathFilter> PROGRAMS = List.of(new PathFilter("Programs", List.of("*.exe")));

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
    }

    @Test
    @DisplayName("a path entry has the type PATH, its mode, its filters and its default value")
    void pathEntryNamesModeAndFilters() {
        SettingEntry<String> entry = SettingEntry.ofPath("editor.program", "Program", "C:\\npp.exe",
                PathMode.OPEN, PROGRAMS);

        assertEquals(SettingType.PATH, entry.getType());
        assertEquals(String.class, entry.getValueClass());
        assertEquals("C:\\npp.exe", entry.getValue());
        assertEquals(PathMode.OPEN, entry.getPathMode());
        assertEquals(PROGRAMS, entry.getPathFilters());
        assertNull(entry.getDescriptionKey());

        SettingEntry<String> described = SettingEntry.ofPath("export.folder", "Folder", "Where exports go", "",
                PathMode.DIRECTORY, List.of());
        assertEquals("Where exports go", described.getDescriptionKey());
        assertEquals(PathMode.DIRECTORY, described.getPathMode());
        assertEquals(List.of(), described.getPathFilters());
    }

    @Test
    @DisplayName("a path entry starts empty without a default, copies its filters, and refuses a null mode or filters")
    void pathEntryArguments() {
        List<PathFilter> filters = new ArrayList<>(PROGRAMS);
        SettingEntry<String> entry = SettingEntry.ofPath("p", "P", null, PathMode.SAVE, filters);
        filters.clear();

        assertEquals("", entry.getValue());
        assertEquals(PROGRAMS, entry.getPathFilters());
        assertThrows(UnsupportedOperationException.class, () -> entry.getPathFilters().clear());
        assertThrows(NullPointerException.class,
                () -> SettingEntry.ofPath("p", "P", "", null, List.of()));
        assertThrows(NullPointerException.class,
                () -> SettingEntry.ofPath("p", "P", "", PathMode.OPEN, null));
    }

    @Test
    @DisplayName("an entry of another type has no path mode and no filters")
    void otherTypesHaveNoPathMode() {
        SettingEntry<String> text = SettingEntry.ofString("t", "T", "x");

        assertNull(text.getPathMode());
        assertEquals(List.of(), text.getPathFilters());
    }

    @Test
    @DisplayName("the dialog shows a path entry with a path input in its mode, with its filters, holding its value")
    void dialogShowsPathInput() throws Exception {
        onFxThread(() -> {
            SettingEntry<String> entry = SettingEntry.ofPath("editor.program", "Program", "C:\\npp.exe",
                    PathMode.OPEN, PROGRAMS);
            SettingsDialog dialog = dialogWith(entry, null);

            PathInput input = pathInput(dialog);
            assertEquals("C:\\npp.exe", input.getValue());
            assertEquals(PathMode.OPEN, input.getMode());
            assertEquals(PROGRAMS, input.getFilters());
            return null;
        });
    }

    @Test
    @DisplayName("the dialog keeps a directory mode and shows the filters' descriptions through the resource bundle")
    void dialogKeepsModeAndResolvesFilters() throws Exception {
        onFxThread(() -> {
            ResourceBundle bundle = new ListResourceBundle() {
                @Override
                protected Object[][] getContents() {
                    return new Object[][] {{"filter.programs", "Programme"}};
                }
            };
            SettingEntry<String> folder = SettingEntry.ofPath("export.folder", "Folder", "",
                    PathMode.DIRECTORY, List.of());
            SettingEntry<String> program = SettingEntry.ofPath("editor.program", "Program", "",
                    PathMode.OPEN, List.of(new PathFilter("filter.programs", List.of("*.exe", "*.cmd"))));
            SettingsDialog dialog = dialogWith(List.of(folder, program), bundle, null);

            List<PathInput> inputs = pathInputs(dialog);
            assertEquals(2, inputs.size());
            assertEquals(PathMode.DIRECTORY, inputs.get(0).getMode());
            assertEquals(List.of(), inputs.get(0).getFilters());
            assertEquals(PathMode.OPEN, inputs.get(1).getMode());
            assertEquals(List.of(new PathFilter("Programme", List.of("*.exe", "*.cmd"))),
                    inputs.get(1).getFilters());
            return null;
        });
    }

    @Test
    @DisplayName("a path typed in the dialog becomes the entry's value, and OK saves it")
    void dialogSavesChangedPath() throws Exception {
        onFxThread(() -> {
            SettingEntry<String> entry = SettingEntry.ofPath("editor.program", "Program", "",
                    PathMode.OPEN, PROGRAMS);
            AtomicInteger applied = new AtomicInteger();
            SettingsDialog dialog = dialogWith(entry, applied::incrementAndGet);
            Button apply = button(dialog, "Apply");
            assertTrue(apply.isDisabled(), "nothing changed yet");

            pathInput(dialog).setValue("D:\\Tools\\Editor With Spaces\\edit.exe");

            assertEquals("D:\\Tools\\Editor With Spaces\\edit.exe", entry.getValue());
            assertFalse(apply.isDisabled(), "the changed path can be applied");
            button(dialog, "OK").fire();
            assertEquals(1, applied.get(), "OK saves the changed path");
            return null;
        });
    }

    @Test
    @DisplayName("Cancel puts the stored path back, in the entry and in the field")
    void cancelRestoresPath() throws Exception {
        onFxThread(() -> {
            SettingEntry<String> entry = SettingEntry.ofPath("editor.program", "Program", "C:\\npp.exe",
                    PathMode.OPEN, PROGRAMS);
            SettingsDialog dialog = dialogWith(entry, null);
            PathInput input = pathInput(dialog);

            input.setValue("C:\\other.exe");
            button(dialog, "Cancel").fire();

            assertEquals("C:\\npp.exe", entry.getValue());
            assertEquals("C:\\npp.exe", input.getValue());
            return null;
        });
    }

    @Test
    @DisplayName("a value the application restores into the model shows in the field")
    void restoredValueShows() throws Exception {
        onFxThread(() -> {
            SettingEntry<String> entry = SettingEntry.ofPath("editor.program", "Program", "",
                    PathMode.OPEN, PROGRAMS);
            SettingsModel model = new SettingsModel(List.of(category(List.of(entry))), null);
            SettingsDialog dialog = new SettingsDialog.Builder().model(model).build();

            model.restore(Map.of("editor.program", "C:\\code.exe"));

            assertEquals("C:\\code.exe", pathInput(dialog).getValue());
            return null;
        });
    }

    @Test
    @DisplayName("in both schemes the path input is styled like the dialog's text field, without a CSS error")
    void bothSchemesStylePathLikeText() throws Exception {
        for (Appearance appearance : List.of(Appearance.LIGHT, Appearance.DARK)) {
            List<String> errors = onFxThread(() -> {
                ThemeManager.setAppearance(appearance);
                SettingEntry<String> text = SettingEntry.ofString("args", "Arguments", "\"{file}\"");
                SettingEntry<String> path = SettingEntry.ofPath("editor.program", "Program", "C:\\npp.exe",
                        PathMode.OPEN, PROGRAMS);
                SettingsDialog dialog = dialogWith(List.of(text, path), null, null);

                List<String> reported = new ArrayList<>();
                ListChangeListener<CssParser.ParseError> recorder = change -> {
                    while (change.next()) {
                        for (CssParser.ParseError error : change.getAddedSubList()) {
                            reported.add(error.getMessage());
                        }
                    }
                };
                ObservableList<CssParser.ParseError> cssErrors = CssParser.errorsProperty();
                cssErrors.addListener(recorder);
                try {
                    Parent root = dialog.scene().getRoot();
                    root.applyCss();
                    root.layout();
                } finally {
                    cssErrors.removeListener(recorder);
                }

                TextField textField = textField(dialog);
                PathInput input = pathInput(dialog);
                assertEquals(2, fills(input.getBackground()).size(), appearance + ": a border and a field colour");
                assertEquals(fills(textField.getBackground()), fills(input.getBackground()),
                        appearance + ": the path's frame has the text field's border and field colour");
                return reported;
            });
            assertEquals(List.of(), errors, appearance + ": CSS errors while styling the path setting");
        }
        onFxThread(() -> {
            ThemeManager.setAppearance(Appearance.DARK);
            return null;
        });
    }

    // --- Helpers ---

    private static SettingsDialog dialogWith(SettingEntry<?> entry, Runnable onApply) {
        return dialogWith(List.of(entry), null, onApply);
    }

    private static SettingsDialog dialogWith(List<SettingEntry<?>> entries, ResourceBundle bundle,
                                             Runnable onApply) {
        SettingsModel model = new SettingsModel(List.of(category(entries)), bundle);
        SettingsDialog dialog = new SettingsDialog.Builder()
                .model(model)
                .onApply(onApply)
                .build();
        dialog.scene().getRoot().applyCss();
        return dialog;
    }

    private static SettingCategory category(List<SettingEntry<?>> entries) {
        return new SettingCategory("editor", "Editor",
                List.of(new SettingGroup("Program", entries)), List.of());
    }

    private static PathInput pathInput(SettingsDialog dialog) {
        List<PathInput> inputs = pathInputs(dialog);
        assertEquals(1, inputs.size(), "one path input");
        return inputs.getFirst();
    }

    private static List<PathInput> pathInputs(SettingsDialog dialog) {
        Parent root = dialog.scene().getRoot();
        root.applyCss();
        List<PathInput> inputs = new ArrayList<>();
        Set<Node> found = root.lookupAll(".form-path-input");
        for (Node node : found) {
            inputs.add((PathInput) node);
        }
        return inputs;
    }

    private static TextField textField(SettingsDialog dialog) {
        Parent root = dialog.scene().getRoot();
        for (Node node : root.lookupAll(".text-field")) {
            if (node.getClass() == TextField.class && node.getStyleClass().size() == 2) {
                // The entry's own text field (text-input, text-field), not a frame's inner text.
                return (TextField) node;
            }
        }
        throw new AssertionError("no text field of a string entry");
    }

    private static Button button(SettingsDialog dialog, String text) {
        for (Node node : dialog.scene().getRoot().lookupAll(".settings-button")) {
            Button button = (Button) node;
            if (button.getText().equals(text)) {
                return button;
            }
        }
        throw new AssertionError("no button " + text);
    }

    private static List<String> fills(Background background) {
        List<String> fills = new ArrayList<>();
        if (background != null) {
            for (BackgroundFill fill : background.getFills()) {
                fills.add(fill.getFill() + " " + fill.getInsets());
            }
        }
        return fills;
    }

    /**
     * Runs the code on the JavaFX Application Thread, where a window has to be made even when it is never shown, and
     * waits for its result.
     */
    private static <T> T onFxThread(Callable<T> code) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(code.call());
            } catch (Exception | AssertionError failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get(30, TimeUnit.SECONDS);
    }
}
