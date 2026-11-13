package org.karthu.fx.form;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The standard chooser's dialogs as a request configures them; no dialog is shown.
 */
class StandardPathChooserTest {

    @TempDir
    Path temp;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        FormTestSupport.initToolkit();
    }

    @Test
    @DisplayName("the file dialog gets the title and starts in the start directory")
    void fileDialogFromTheRequest() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        PathChooser.Request request =
                new PathChooser.Request(PathMode.OPEN, "Choose the input", folder, null, List.of(), null);

        FileChooser fileChooser = StandardPathChooser.fileChooser(request);

        assertEquals("Choose the input", fileChooser.getTitle());
        assertEquals(folder.toFile(), fileChooser.getInitialDirectory());
    }

    @Test
    @DisplayName("the directory dialog gets the title and starts in the start directory")
    void directoryDialogFromTheRequest() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        PathChooser.Request request =
                new PathChooser.Request(PathMode.DIRECTORY, "Choose the folder", folder, null, List.of(), null);

        DirectoryChooser directoryChooser = StandardPathChooser.directoryChooser(request);

        assertEquals("Choose the folder", directoryChooser.getTitle());
        assertEquals(folder.toFile(), directoryChooser.getInitialDirectory());
    }

    @Test
    @DisplayName("a start directory removed in the meantime leaves the start to the platform")
    void missingStartDirectoryIsNotPassedOn() {
        Path missing = temp.resolve("gone");
        PathChooser.Request request = new PathChooser.Request(PathMode.SAVE, null, missing, null, List.of(), null);

        assertNull(StandardPathChooser.fileChooser(request).getInitialDirectory());
        assertNull(StandardPathChooser.directoryChooser(request).getInitialDirectory());
    }

    // --- File filters ------------------------------------------------------------------------------

    @Test
    @DisplayName("the file dialog lists the request's filters as extension filters in their order, the first selected")
    void fileDialogListsTheFilters() {
        PathFilter csv = new PathFilter("CSV files", List.of("*.csv"));
        PathFilter text = new PathFilter("Text files", List.of("*.txt", "*.tsv"));
        PathFilter allFiles = new PathFilter("All files", List.of("*"));
        PathChooser.Request request = new PathChooser.Request(
                PathMode.SAVE, null, temp, null, List.of(csv, text, allFiles), null);

        FileChooser fileChooser = StandardPathChooser.fileChooser(request);

        List<FileChooser.ExtensionFilter> shown = fileChooser.getExtensionFilters();
        assertEquals(3, shown.size());
        assertEquals("CSV files", shown.get(0).getDescription());
        assertEquals(List.of("*.csv"), shown.get(0).getExtensions());
        assertEquals("Text files", shown.get(1).getDescription());
        assertEquals(List.of("*.txt", "*.tsv"), shown.get(1).getExtensions());
        assertEquals("All files", shown.get(2).getDescription());
        assertEquals(List.of("*"), shown.get(2).getExtensions());
        assertSame(shown.get(0), fileChooser.getSelectedExtensionFilter());
    }

    @Test
    @DisplayName("a request without filters gives a file dialog without filters, which shows every file")
    void fileDialogWithoutFilters() {
        PathChooser.Request request = new PathChooser.Request(PathMode.OPEN, null, temp, null, List.of(), null);

        FileChooser fileChooser = StandardPathChooser.fileChooser(request);

        assertTrue(fileChooser.getExtensionFilters().isEmpty());
        assertNull(fileChooser.getSelectedExtensionFilter());
    }
}
