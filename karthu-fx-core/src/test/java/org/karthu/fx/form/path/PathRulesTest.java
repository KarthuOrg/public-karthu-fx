package org.karthu.fx.form.path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.karthu.fx.form.PathMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The path rules without JavaFX, on real folders and files in a temporary directory.
 */
class PathRulesTest {

    @TempDir
    Path temp;

    private Path home;

    private Path home() throws IOException {
        if (home == null) {
            home = Files.createDirectories(temp.resolve("home"));
        }
        return home;
    }

    // --- Start directory: the path in the field ------------------------------------------------------

    @Test
    @DisplayName("open mode: an existing file in the field starts the dialog in its folder")
    void openModeStartsInTheFolderOfAnExistingFile() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Path file = Files.createFile(folder.resolve("input.csv"));

        assertEquals(folder, PathRules.startDirectory(file.toString(), PathMode.OPEN, null, null, home()));
    }

    @Test
    @DisplayName("open and save mode: an existing directory in the field is the start directory itself")
    void fileModesStartInAnExistingDirectoryItself() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));

        assertEquals(folder, PathRules.startDirectory(folder.toString(), PathMode.OPEN, null, null, home()));
        assertEquals(folder, PathRules.startDirectory(folder.toString(), PathMode.SAVE, null, null, home()));
    }

    @Test
    @DisplayName("save mode: a file that does not exist yet starts the dialog in its folder")
    void saveModeStartsInTheFolderOfANewFile() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("out"));

        String newFile = folder.resolve("result.csv").toString();

        assertEquals(folder, PathRules.startDirectory(newFile, PathMode.SAVE, null, null, home()));
    }

    @Test
    @DisplayName("open mode: a file in a folder that no longer exists starts in the nearest existing ancestor")
    void openModeFallsBackToTheNearestExistingAncestor() throws IOException {
        Path existing = Files.createDirectories(temp.resolve("projects"));
        Path missingFile = existing.resolve("gone").resolve("deeper").resolve("input.csv");

        assertEquals(existing, PathRules.startDirectory(missingFile.toString(), PathMode.OPEN, null, null, home()));
    }

    @Test
    @DisplayName("directory mode: an existing directory in the field is the start directory itself")
    void directoryModeStartsInTheDirectoryItself() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data").resolve("2026"));

        assertEquals(folder, PathRules.startDirectory(folder.toString(), PathMode.DIRECTORY, null, null, home()));
    }

    @Test
    @DisplayName("directory mode: a directory that no longer exists starts in its nearest existing ancestor")
    void directoryModeFallsBackToTheNearestExistingAncestor() throws IOException {
        Path existing = Files.createDirectories(temp.resolve("data"));
        Path missing = existing.resolve("2026").resolve("11");

        assertEquals(existing, PathRules.startDirectory(missing.toString(), PathMode.DIRECTORY, null, null, home()));
    }

    @Test
    @DisplayName("directory mode: a file in the field starts in its folder, the nearest directory")
    void directoryModeWithAFileStartsInItsFolder() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Path file = Files.createFile(folder.resolve("input.csv"));

        assertEquals(folder, PathRules.startDirectory(file.toString(), PathMode.DIRECTORY, null, null, home()));
    }

    @Test
    @DisplayName("the field's path is normalised before its folder is looked up")
    void fieldPathIsNormalised() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Path other = Files.createDirectories(temp.resolve("other"));
        String text = other + File.separator + ".." + File.separator + "data" + File.separator + "input.csv";

        assertEquals(folder, PathRules.startDirectory(text, PathMode.OPEN, null, null, home()));
    }

    // --- Start directory: the home fallback ----------------------------------------------------------

    @Test
    @DisplayName("an empty or null field starts in the home directory, in every mode")
    void emptyFieldStartsAtHome() throws IOException {
        for (PathMode mode : PathMode.values()) {
            assertEquals(home(), PathRules.startDirectory("", mode, null, null, home()));
            assertEquals(home(), PathRules.startDirectory(null, mode, null, null, home()));
        }
    }

    @Test
    @DisplayName("a field that is no valid path starts in the home directory")
    void invalidFieldStartsAtHome() throws IOException {
        assertEquals(home(), PathRules.startDirectory("bad\u0000path", PathMode.OPEN, null, null, home()));
    }

    @Test
    @DisplayName("a relative path in the field is skipped without a base directory: the home directory")
    void relativeFieldStartsAtHome() throws IOException {
        // "." exists relative to the working directory, and is skipped all the same.
        assertEquals(home(), PathRules.startDirectory("./input.csv", PathMode.OPEN, null, null, home()));
        assertEquals(home(), PathRules.startDirectory(".", PathMode.DIRECTORY, null, null, home()));
    }

    @Test
    @DisplayName("a path of which nothing exists, not even its root, starts in the home directory")
    void pathWithoutAnyExistingAncestorStartsAtHome() throws IOException {
        Optional<Path> missingRoot = missingWindowsDrive();
        assumeTrue(missingRoot.isPresent(), "needs a drive letter that does not exist (Windows only)");
        Path missing = missingRoot.get().resolve("data").resolve("input.csv");

        assertEquals(home(), PathRules.startDirectory(missing.toString(), PathMode.OPEN, null, null, home()));
        assertEquals(home(), PathRules.startDirectory(missing.toString(), PathMode.DIRECTORY, null, null, home()));
    }

    /** A drive root that does not exist on this machine, on Windows; elsewhere every absolute path has "/". */
    private static Optional<Path> missingWindowsDrive() {
        if (!File.separator.equals("\\")) {
            return Optional.empty();
        }
        for (char letter = 'Z'; letter >= 'D'; letter--) {
            Path root = Path.of(letter + ":\\");
            if (!Files.exists(root)) {
                return Optional.of(root);
            }
        }
        return Optional.empty();
    }

    // --- Parsing and entering a choice ---------------------------------------------------------------

    @Test
    @DisplayName("the field's text is a path unless it is null, empty or invalid; it is never trimmed")
    void parseTakesTheTextAsItIs() {
        assertTrue(PathRules.parse(null).isEmpty());
        assertTrue(PathRules.parse("").isEmpty());
        assertTrue(PathRules.parse("bad\u0000path").isEmpty());
        assertEquals(Optional.of(Path.of("files/data.csv")), PathRules.parse("files/data.csv"));
    }

    @Test
    @DisplayName("a chosen path is entered absolute and normalised, as the platform writes it")
    void choiceIsEnteredAbsoluteAndNormalised() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Path chosen = folder.resolve("..").resolve("data").resolve("input.csv");

        assertEquals(folder.resolve("input.csv").toString(), PathRules.choiceText(chosen, null));
        assertEquals(Path.of("relative.csv").toAbsolutePath().toString(),
                PathRules.choiceText(Path.of("relative.csv"), null));
    }

    // --- Entering a choice: the base directory -------------------------------------------------------

    @Test
    @DisplayName("a choice below the base directory is entered relative to it, with / separators")
    void choiceBelowTheBaseIsEnteredRelative() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path chosen = base.resolve("files").resolve("in").resolve("data.csv");

        assertEquals("files/in/data.csv", PathRules.choiceText(chosen, base));
        assertEquals("data.csv", PathRules.choiceText(base.resolve("data.csv"), base));
    }

    @Test
    @DisplayName("the base directory itself is entered as .")
    void baseItselfIsEnteredAsDot() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));

        assertEquals(".", PathRules.choiceText(base, base));
        assertEquals(".", PathRules.choiceText(base.resolve("files").resolve(".."), base));
    }

    @Test
    @DisplayName("a choice outside the base directory, or beside it with a common prefix, is entered absolute")
    void choiceOutsideTheBaseIsEnteredAbsolute() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path outside = temp.resolve("other").resolve("data.csv");
        Path prefixed = temp.resolve("workflow-old").resolve("data.csv");

        assertEquals(outside.toString(), PathRules.choiceText(outside, base));
        assertEquals(prefixed.toString(), PathRules.choiceText(prefixed, base));
        assertEquals(temp.toString(), PathRules.choiceText(temp, base));
    }

    @Test
    @DisplayName("the base directory is normalised before the choice is compared with it")
    void baseIsNormalised() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path unnormalised = base.resolve("files").resolve("..");

        assertEquals("files/data.csv", PathRules.choiceText(base.resolve("files").resolve("data.csv"), unnormalised));
    }

    @Test
    @DisplayName("a relative base directory counts as none: the choice is entered absolute")
    void relativeBaseCountsAsNone() throws IOException {
        Path chosen = Files.createDirectories(temp.resolve("workflow")).resolve("data.csv");

        assertEquals(chosen.toString(), PathRules.choiceText(chosen, Path.of("workflow")));
    }

    @Test
    @DisplayName("Windows: a choice below the base is relative also when the case of its names differs")
    void baseIsComparedAsThePlatformComparesPaths() throws IOException {
        assumeTrue(File.separator.equals("\\"), "Windows compares paths ignoring case");
        Path base = Files.createDirectories(temp.resolve("Workflow"));
        Path chosen = Path.of(base.toString().toUpperCase(Locale.ROOT)).resolve("Files").resolve("Data.csv");

        assertEquals("Files/Data.csv", PathRules.choiceText(chosen, base));
    }

    // --- Start directory: the base directory and the memory ------------------------------------------

    @Test
    @DisplayName("a relative path in the field is resolved against the base directory")
    void relativeFieldIsResolvedAgainstTheBase() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path files = Files.createDirectories(base.resolve("files"));

        assertEquals(files, PathRules.startDirectory("files/data.csv", PathMode.OPEN, base, null, home()));
        assertEquals(files, PathRules.startDirectory("files/new.csv", PathMode.SAVE, base, null, home()));
        assertEquals(files, PathRules.startDirectory("files", PathMode.DIRECTORY, base, null, home()));
        assertEquals(base, PathRules.startDirectory(".", PathMode.DIRECTORY, base, null, home()));
        assertEquals(base, PathRules.startDirectory("data.csv", PathMode.OPEN, base, null, home()));
    }

    @Test
    @DisplayName("a relative path in the field whose folder is missing starts in its nearest existing ancestor")
    void relativeFieldFallsBackToTheNearestExistingAncestor() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path files = Files.createDirectories(base.resolve("files"));

        assertEquals(files, PathRules.startDirectory("files/gone/data.csv", PathMode.OPEN, base, null, home()));
        assertEquals(base, PathRules.startDirectory("gone/deeper", PathMode.DIRECTORY, base, null, home()));
        assertEquals(temp, PathRules.startDirectory("../elsewhere/data.csv", PathMode.OPEN, base, null, home()));
    }

    @Test
    @DisplayName("an absolute path in the field is used as it is, with or without a base directory")
    void absoluteFieldIgnoresTheBase() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path folder = Files.createDirectories(temp.resolve("data"));
        String file = folder.resolve("input.csv").toString();

        assertEquals(folder, PathRules.startDirectory(file, PathMode.OPEN, base, null, home()));
        assertEquals(folder, PathRules.startDirectory(file, PathMode.OPEN, null, null, home()));
    }

    @Test
    @DisplayName("an empty field starts in the memory's last directory, in every mode")
    void emptyFieldStartsInTheLastDirectory() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Path last = Files.createDirectories(temp.resolve("last"));

        for (PathMode mode : PathMode.values()) {
            assertEquals(last, PathRules.startDirectory("", mode, base, last, home()));
            assertEquals(last, PathRules.startDirectory(null, mode, null, last, home()));
        }
    }

    @Test
    @DisplayName("a last directory that no longer exists is replaced by its nearest existing ancestor")
    void lastDirectoryFallsBackToTheNearestExistingAncestor() throws IOException {
        Path existing = Files.createDirectories(temp.resolve("last"));
        Path gone = existing.resolve("gone").resolve("deeper");

        assertEquals(existing, PathRules.startDirectory("", PathMode.OPEN, null, gone, home()));
    }

    @Test
    @DisplayName("the field's path comes before the memory's last directory")
    void fieldComesBeforeTheLastDirectory() throws IOException {
        Path last = Files.createDirectories(temp.resolve("last"));
        Path folder = Files.createDirectories(temp.resolve("data"));

        assertEquals(folder, PathRules.startDirectory(folder.resolve("input.csv").toString(), PathMode.OPEN,
                null, last, home()));
    }

    @Test
    @DisplayName("a field that gives no folder (invalid, relative without base) falls through to the last directory")
    void unusableFieldFallsThroughToTheLastDirectory() throws IOException {
        Path last = Files.createDirectories(temp.resolve("last"));

        assertEquals(last, PathRules.startDirectory("bad\u0000path", PathMode.OPEN, null, last, home()));
        assertEquals(last, PathRules.startDirectory("files/data.csv", PathMode.OPEN, null, last, home()));
    }

    @Test
    @DisplayName("without a last directory, an empty field starts in the base directory")
    void emptyFieldWithoutMemoryStartsInTheBase() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));

        for (PathMode mode : PathMode.values()) {
            assertEquals(base, PathRules.startDirectory("", mode, base, null, home()));
        }
    }

    @Test
    @DisplayName("a base directory that does not exist is skipped: the home directory, not an ancestor")
    void missingBaseIsSkipped() throws IOException {
        Path missingBase = temp.resolve("gone").resolve("workflow");

        assertEquals(home(), PathRules.startDirectory("", PathMode.OPEN, missingBase, null, home()));
    }

    // --- Initial file name ---------------------------------------------------------------------------

    @Test
    @DisplayName("save mode: the initial file name is the file name of the path in the field, absolute or relative")
    void saveModeSuggestsTheFileNameInTheField() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        String absolute = temp.resolve("out").resolve("result.csv").toString();

        assertEquals("result.csv", PathRules.initialFileName(absolute, PathMode.SAVE, null));
        assertEquals("result.csv", PathRules.initialFileName("files/result.csv", PathMode.SAVE, base));
        assertEquals("result.csv", PathRules.initialFileName("files/result.csv", PathMode.SAVE, null));
    }

    @Test
    @DisplayName("open and directory mode suggest no file name")
    void otherModesSuggestNoFileName() {
        assertNull(PathRules.initialFileName("files/result.csv", PathMode.OPEN, null));
        assertNull(PathRules.initialFileName("files/result.csv", PathMode.DIRECTORY, null));
    }

    @Test
    @DisplayName("save mode: no file name for an empty or invalid field, a root, . or ..")
    void saveModeWithoutAFileName() {
        assertNull(PathRules.initialFileName("", PathMode.SAVE, null));
        assertNull(PathRules.initialFileName(null, PathMode.SAVE, null));
        assertNull(PathRules.initialFileName("bad\u0000path", PathMode.SAVE, null));
        assertNull(PathRules.initialFileName(".", PathMode.SAVE, null));
        assertNull(PathRules.initialFileName("..", PathMode.SAVE, null));
        assertNull(PathRules.initialFileName(temp.getRoot().toString(), PathMode.SAVE, null));
    }

    @Test
    @DisplayName("save mode: no file name when the field names an existing directory, which is the start folder")
    void saveModeWithAnExistingDirectory() throws IOException {
        Path base = Files.createDirectories(temp.resolve("workflow"));
        Files.createDirectories(base.resolve("out"));

        assertNull(PathRules.initialFileName(base.toString(), PathMode.SAVE, null));
        assertNull(PathRules.initialFileName("out", PathMode.SAVE, base));
    }

    // --- Memory update -------------------------------------------------------------------------------

    @Test
    @DisplayName("an existing file chosen remembers its folder, absolute and normalised, in every mode")
    void existingFileRemembersItsFolder() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Files.createFile(folder.resolve("input.csv"));
        Path chosen = folder.resolve("..").resolve("data").resolve("input.csv");

        assertEquals(Optional.of(folder), PathRules.rememberedDirectory(chosen, PathMode.OPEN));
        assertEquals(Optional.of(folder), PathRules.rememberedDirectory(chosen, PathMode.SAVE));
        assertEquals(Optional.of(folder), PathRules.rememberedDirectory(chosen, PathMode.DIRECTORY));
    }

    @Test
    @DisplayName("an existing directory chosen remembers itself, absolute and normalised, in every mode")
    void existingDirectoryRemembersItself() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));

        assertEquals(Optional.of(folder), PathRules.rememberedDirectory(folder, PathMode.DIRECTORY));
        assertEquals(Optional.of(folder),
                PathRules.rememberedDirectory(folder.resolve("sub").resolve(".."), PathMode.DIRECTORY));
        assertEquals(Optional.of(folder), PathRules.rememberedDirectory(folder, PathMode.OPEN));
        assertEquals(Optional.of(folder), PathRules.rememberedDirectory(folder, PathMode.SAVE));
    }

    @Test
    @DisplayName("a path that does not exist follows the mode: a new file its folder, a new directory itself")
    void missingPathFollowsTheMode() throws IOException {
        Path folder = Files.createDirectories(temp.resolve("data"));
        Path missing = folder.resolve("new.csv");

        assertEquals(Optional.of(folder), PathRules.rememberedDirectory(missing, PathMode.SAVE));
        assertEquals(Optional.of(folder), PathRules.rememberedDirectory(missing, PathMode.OPEN));
        assertEquals(Optional.of(missing), PathRules.rememberedDirectory(missing, PathMode.DIRECTORY));
    }

    @Test
    @DisplayName("a root is a directory and remembers itself; a missing root chosen as a file remembers nothing")
    void rootRemembersItself() {
        assertEquals(Optional.of(temp.getRoot()), PathRules.rememberedDirectory(temp.getRoot(), PathMode.OPEN));
        assertEquals(Optional.of(temp.getRoot()), PathRules.rememberedDirectory(temp.getRoot(), PathMode.DIRECTORY));
        Optional<Path> missingRoot = missingWindowsDrive();
        assumeTrue(missingRoot.isPresent(), "needs a drive letter that does not exist (Windows only)");
        assertEquals(Optional.empty(), PathRules.rememberedDirectory(missingRoot.get(), PathMode.OPEN));
    }

    @Test
    @DisplayName("a relative last directory or base directory counts as none")
    void relativeLastAndBaseCountAsNone() throws IOException {
        assertEquals(home(), PathRules.startDirectory("", PathMode.OPEN, Path.of("."), Path.of("."), home()));
        assertEquals(home(), PathRules.startDirectory("data.csv", PathMode.OPEN, Path.of("."), null, home()));
    }

    // --- Drop acceptance -----------------------------------------------------------------------------

    @Test
    @DisplayName("open and save mode: one regular file is accepted, a directory is refused")
    void fileModesAcceptOneFile() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        Path directory = Files.createDirectories(temp.resolve("data"));

        for (PathMode mode : List.of(PathMode.OPEN, PathMode.SAVE)) {
            assertTrue(PathRules.acceptsDrop(List.of(file), mode), mode.toString());
            assertFalse(PathRules.acceptsDrop(List.of(directory), mode), mode.toString());
        }
    }

    @Test
    @DisplayName("directory mode: one directory is accepted, a regular file is refused")
    void directoryModeAcceptsOneDirectory() throws IOException {
        Path file = Files.createFile(temp.resolve("input.csv"));
        Path directory = Files.createDirectories(temp.resolve("data"));

        assertTrue(PathRules.acceptsDrop(List.of(directory), PathMode.DIRECTORY));
        assertFalse(PathRules.acceptsDrop(List.of(file), PathMode.DIRECTORY));
    }

    @Test
    @DisplayName("several entries are refused in every mode, also when each alone would be accepted")
    void severalEntriesAreRefused() throws IOException {
        Path first = Files.createFile(temp.resolve("first.csv"));
        Path second = Files.createFile(temp.resolve("second.csv"));
        Path data = Files.createDirectories(temp.resolve("data"));
        Path other = Files.createDirectories(temp.resolve("other"));

        for (PathMode mode : PathMode.values()) {
            assertFalse(PathRules.acceptsDrop(List.of(first, second), mode), mode.toString());
            assertFalse(PathRules.acceptsDrop(List.of(data, other), mode), mode.toString());
            assertFalse(PathRules.acceptsDrop(List.of(first, data), mode), mode.toString());
        }
    }

    @Test
    @DisplayName("nothing is refused: no entries, no list, an entry that does not exist (also in the save mode)")
    void nothingIsRefused() {
        Path missingFile = temp.resolve("missing.csv");

        for (PathMode mode : PathMode.values()) {
            assertFalse(PathRules.acceptsDrop(List.of(), mode), mode.toString());
            assertFalse(PathRules.acceptsDrop(null, mode), mode.toString());
            assertFalse(PathRules.acceptsDrop(List.of(missingFile), mode), mode.toString());
        }
    }

    @Test
    @DisplayName("the filters do not apply: a file of any extension is accepted")
    void anyExtensionIsAccepted() throws IOException {
        Path unexpected = Files.createFile(temp.resolve("data.tsv"));
        Path noExtension = Files.createFile(temp.resolve("README"));

        assertTrue(PathRules.acceptsDrop(List.of(unexpected), PathMode.OPEN));
        assertTrue(PathRules.acceptsDrop(List.of(noExtension), PathMode.SAVE));
    }

    // --- Paste ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("a path in one pair of double quotes, as the Explorer's Copy as path gives it, is pasted without them")
    void quotedPasteIsUnquoted() {
        assertEquals("C:\\data\\input.csv", PathRules.pastedText("\"C:\\data\\input.csv\""));
        assertEquals("C:\\My Data\\input.csv", PathRules.pastedText("\"C:\\My Data\\input.csv\""));
        assertEquals("/home/user/data.csv", PathRules.pastedText("\"/home/user/data.csv\""));
    }

    @Test
    @DisplayName("whitespace around the quotes is ignored and dropped, whitespace inside them is kept")
    void whitespaceAroundTheQuotesIsIgnored() {
        assertEquals("C:\\data\\input.csv", PathRules.pastedText("  \"C:\\data\\input.csv\"\r\n"));
        assertEquals("C:\\data\\input.csv", PathRules.pastedText("\t\"C:\\data\\input.csv\" "));
        assertEquals(" padded ", PathRules.pastedText("\" padded \""));
    }

    @Test
    @DisplayName("any other paste is unchanged: no quotes, one quote only, surrounding whitespace without quotes")
    void otherPastesAreUnchanged() {
        assertEquals("C:\\data\\input.csv", PathRules.pastedText("C:\\data\\input.csv"));
        assertEquals("  files/data.csv  ", PathRules.pastedText("  files/data.csv  "));
        assertEquals("\"C:\\data\\input.csv", PathRules.pastedText("\"C:\\data\\input.csv"));
        assertEquals("C:\\data\\input.csv\"", PathRules.pastedText("C:\\data\\input.csv\""));
        assertEquals("\"", PathRules.pastedText("\""));
        assertEquals("  \"  ", PathRules.pastedText("  \"  "));
        assertEquals("", PathRules.pastedText(""));
    }

    @Test
    @DisplayName("quotes inside the text are kept: more than one pair is no single quoted path")
    void quotesInsideAreKept() {
        assertEquals("C:\\a \"b\" c.csv", PathRules.pastedText("C:\\a \"b\" c.csv"));
        assertEquals("\"C:\\a.csv\" \"C:\\b.csv\"", PathRules.pastedText("\"C:\\a.csv\" \"C:\\b.csv\""));
        assertEquals("\"C:\\a.csv\"\r\n\"C:\\b.csv\"", PathRules.pastedText("\"C:\\a.csv\"\r\n\"C:\\b.csv\""));
        assertEquals("\"a\"b\"", PathRules.pastedText("\"a\"b\""));
    }

    @Test
    @DisplayName("an empty pair of quotes pastes nothing: what is inside them")
    void emptyQuotesPasteNothing() {
        assertEquals("", PathRules.pastedText("\"\""));
        assertEquals("", PathRules.pastedText(" \"\" "));
    }
}
