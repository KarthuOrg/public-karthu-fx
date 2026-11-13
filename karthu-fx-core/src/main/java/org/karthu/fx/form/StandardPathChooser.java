package org.karthu.fx.form;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The standard {@link PathChooser}: JavaFX's {@code FileChooser} and {@code DirectoryChooser}. Package-private;
 * applications reach it through {@link PathChooser#standard()}.
 *
 * <p>The dialogs are configured in {@link #fileChooser(PathChooser.Request)} and
 * {@link #directoryChooser(PathChooser.Request)}, apart from showing them, so that a test checks the configuration
 * without opening a dialog.
 */
final class StandardPathChooser implements PathChooser {

    /** The one instance: the chooser holds no state, each request configures new dialogs. */
    static final StandardPathChooser INSTANCE = new StandardPathChooser();

    private StandardPathChooser() {
    }

    @Override
    public Optional<Path> choose(Request request) {
        File chosen;
        if (request.mode() == PathMode.DIRECTORY) {
            chosen = directoryChooser(request).showDialog(request.owner());
        } else if (request.mode() == PathMode.SAVE) {
            chosen = fileChooser(request).showSaveDialog(request.owner());
        } else {
            chosen = fileChooser(request).showOpenDialog(request.owner());
        }
        if (chosen == null) {
            return Optional.empty();
        }
        return Optional.of(chosen.toPath());
    }

    /**
     * A file chooser for the open and save modes, configured from the request but not shown.
     *
     * @param request the request
     * @return the file chooser
     */
    static FileChooser fileChooser(Request request) {
        FileChooser fileChooser = new FileChooser();
        if (request.title() != null) {
            fileChooser.setTitle(request.title());
        }
        fileChooser.setInitialDirectory(initialDirectory(request.startDirectory()));
        if (request.initialFileName() != null) {
            fileChooser.setInitialFileName(request.initialFileName());
        }
        addExtensionFilters(fileChooser, request.filters());
        return fileChooser;
    }

    /**
     * Lists the filters in the file chooser in their order and selects the first. Without filters the chooser keeps
     * none, so the dialog shows every file.
     */
    private static void addExtensionFilters(FileChooser fileChooser, List<PathFilter> filters) {
        for (PathFilter filter : filters) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(filter.description(), filter.patterns()));
        }
        if (!fileChooser.getExtensionFilters().isEmpty()) {
            fileChooser.setSelectedExtensionFilter(fileChooser.getExtensionFilters().getFirst());
        }
    }

    /**
     * A directory chooser for the directory mode, configured from the request but not shown.
     *
     * @param request the request
     * @return the directory chooser
     */
    static DirectoryChooser directoryChooser(Request request) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (request.title() != null) {
            directoryChooser.setTitle(request.title());
        }
        directoryChooser.setInitialDirectory(initialDirectory(request.startDirectory()));
        return directoryChooser;
    }

    /**
     * The start directory as the dialogs take it: a {@code File} of the default file system that exists as a
     * directory, else null (the platform's choice). A dialog given a folder that does not exist fails on some
     * platforms, and the start directory may have been removed since the field computed it.
     */
    private static File initialDirectory(Path startDirectory) {
        if (startDirectory.getFileSystem() != FileSystems.getDefault() || !Files.isDirectory(startDirectory)) {
            return null;
        }
        return startDirectory.toFile();
    }
}
