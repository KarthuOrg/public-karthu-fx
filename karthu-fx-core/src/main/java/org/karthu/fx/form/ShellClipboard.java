package org.karthu.fx.form;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * The clipboard the inputs on a shell paste from and copy to: their text controls read and write the clipboard
 * through this seam only, the shell's guarded copy, cut and paste. By default the system clipboard; the tests replace
 * it ({@code FieldShell.setClipboard}, package-private), so that no test touches the clipboard of the machine it runs
 * on. Not part of the API.
 */
interface ShellClipboard {

    /**
     * Returns the clipboard's text.
     *
     * @return the text, or null when the clipboard holds none
     */
    String text();

    /**
     * Replaces the clipboard's content with a text, as plain text.
     *
     * @param text the text, never null
     */
    void put(String text);

    /**
     * The system clipboard, used on the JavaFX Application Thread only, as the system clipboard requires.
     *
     * @return the system clipboard
     */
    static ShellClipboard system() {
        return new ShellClipboard() {
            @Override
            public String text() {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                return clipboard.hasString() ? clipboard.getString() : null;
            }

            @Override
            public void put(String text) {
                ClipboardContent content = new ClipboardContent();
                content.putString(text);
                Clipboard.getSystemClipboard().setContent(content);
            }
        };
    }
}
