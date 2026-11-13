package org.karthu.fx.form;

import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Chooses a path for a {@link PathInput}: the field hands it a {@link Request} when the user clicks the browse icon
 * or presses Alt+Down, and enters the chosen path into the field.
 *
 * <p>The {@linkplain #standard() standard chooser} opens JavaFX's {@code FileChooser} or {@code DirectoryChooser}. An
 * application replaces it to offer another dialog, and a test replaces it with a chooser that opens nothing and
 * returns a prepared path.
 */
@FunctionalInterface
public interface PathChooser {

    /**
     * Lets the user choose a path, usually in a modal dialog, and returns it.
     *
     * @param request what to choose and where to start
     * @return the chosen path, or empty when the user cancelled
     */
    Optional<Path> choose(Request request);

    /**
     * Returns the standard chooser: JavaFX's {@code FileChooser} for the open and save modes and its
     * {@code DirectoryChooser} for the directory mode, started in the request's start directory, titled with its
     * title when one is given (else the platform's own title), modal to its owner window.
     *
     * <p>The file dialogs list the request's filters as JavaFX extension filters in their order, the first selected;
     * without filters they show every file.
     *
     * @return the standard chooser
     */
    static PathChooser standard() {
        return StandardPathChooser.INSTANCE;
    }

    /**
     * What a path input asks its chooser for.
     *
     * @param mode            what to choose: a file to open, a file to save or a directory
     * @param title           the dialog's title, or null for the platform's own
     * @param startDirectory  the existing directory the dialog starts in
     * @param initialFileName the file name the save dialog suggests, or null for none
     * @param filters         the dialog's file filters in their order, the first selected; empty for none (an
     *                        immutable copy)
     * @param owner           the window the dialog is modal to, or null when the field is in none
     */
    record Request(PathMode mode, String title, Path startDirectory, String initialFileName,
                   List<PathFilter> filters, Window owner) {

        /**
         * Creates a request.
         *
         * @param mode            what to choose, not null
         * @param title           the dialog's title, or null
         * @param startDirectory  the directory the dialog starts in, not null
         * @param initialFileName the suggested file name, or null
         * @param filters         the file filters, not null; copied
         * @param owner           the owner window, or null
         * @throws NullPointerException if the mode, the start directory, the filters or a filter is null
         */
        public Request {
            Objects.requireNonNull(mode, "mode must not be null");
            Objects.requireNonNull(startDirectory, "startDirectory must not be null");
            filters = List.copyOf(filters);
        }
    }
}
