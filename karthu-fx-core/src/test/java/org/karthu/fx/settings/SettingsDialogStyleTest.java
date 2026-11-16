package org.karthu.fx.settings;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssParser;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The theme's CSS on the settings dialog, applied in its scene, which is never shown.
 */
class SettingsDialogStyleTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running from another test class
        }
    }

    /**
     * JavaFX reports what goes wrong while it applies CSS, a lookup loop among it, to its log and, once someone asks
     * for it, to the public list of CSS errors, which the test watches while the dialog's CSS is applied.
     */
    @Test
    @DisplayName("the theme styles the dialog, its panes and their settings without a CSS error such as a lookup loop")
    void themeStylesDialogWithoutError() throws Exception {
        List<String> reported = onFxThread(() -> {
            SettingCategory general = new SettingCategory("general", "General",
                    List.of(new SettingGroup("Display", List.of(
                            SettingEntry.ofString("name", "Name", "Ada"),
                            SettingEntry.ofBoolean("compact", "Compact", true)))),
                    List.of());
            SettingsDialog dialog = new SettingsDialog.Builder()
                    .model(new SettingsModel(List.of(general), null))
                    .build();

            List<String> errors = new ArrayList<>();
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
                Parent root = dialog.scene().getRoot();
                root.applyCss();
                root.layout();
            } finally {
                cssErrors.removeListener(recorder);
            }
            return errors;
        });

        assertEquals(List.of(), reported, "CSS errors while styling the settings dialog");
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
