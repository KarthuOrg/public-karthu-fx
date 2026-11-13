package org.karthu.fx.markdown;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Where the view puts the text a reader copies: a code block's code, the whole Markdown source. The view writes
 * through this seam only; by default it puts plain text on the system clipboard, and the view's tests replace it
 * with a recorder ({@code MarkdownView.setClipboard}, package-private), so no test touches the clipboard of the
 * machine it runs on. Not part of the API.
 */
@FunctionalInterface
interface TextClipboard {

    /**
     * Puts a text on the clipboard.
     *
     * @param text the text, never {@code null}
     */
    void put(String text);

    /**
     * The system clipboard: replaces its whole content with the text as plain text. Used on the JavaFX Application
     * Thread only, as the system clipboard requires.
     *
     * @return the system clipboard
     */
    static TextClipboard system() {
        return text -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        };
    }
}
