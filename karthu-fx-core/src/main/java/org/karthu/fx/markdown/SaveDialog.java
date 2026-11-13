package org.karthu.fx.markdown;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;

/**
 * Where the full size window asks the reader which file <em>Save as</em> writes. By default JavaFX's file chooser;
 * the window's tests replace it with a recorder ({@code ImageWindowContent.setSaveDialog}, package-private), so no
 * test opens a real file chooser. Not part of the API.
 */
@FunctionalInterface
interface SaveDialog {

    /**
     * Asks the reader for the file to save to, waiting for the answer.
     *
     * @param owner    the window the dialog belongs to, {@code null} if there is none
     * @param title    the dialog's title
     * @param fileName the file name the dialog suggests
     * @return the file the reader chose, {@code null} if the reader cancelled
     */
    Path choose(Window owner, String title, String fileName);

    /**
     * JavaFX's file chooser in its save mode, which asks before it lets the reader replace a file. Used on the JavaFX
     * Application Thread only.
     *
     * @return the file chooser
     */
    static SaveDialog fileChooser() {
        return (owner, title, fileName) -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(title);
            chooser.setInitialFileName(fileName);
            File file = chooser.showSaveDialog(owner);
            return file == null ? null : file.toPath();
        };
    }
}
