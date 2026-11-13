package org.karthu.fx.markdown;

import javafx.stage.Modality;
import javafx.stage.Window;
import org.karthu.fx.dialog.CustomDialog;

/**
 * Where the full size window tells the reader that saving an image failed. By default karthu-fx's error dialog
 * ({@link CustomDialog}); the window's tests replace it with a recorder ({@code ImageWindowContent.setErrorDialog},
 * package-private), so no test opens a real dialog. Not part of the API.
 */
@FunctionalInterface
interface ErrorDialog {

    /**
     * Shows an error, waiting until the reader has read it.
     *
     * @param owner  the window the dialog belongs to, {@code null} if there is none
     * @param title  the dialog's title
     * @param header what failed
     * @param text   why it failed
     */
    void show(Window owner, String title, String header, String text);

    /**
     * karthu-fx's error dialog, in the theme and with the owner's stage icon. It waits only in its owner, so the
     * application's other windows go on working. Used on the JavaFX Application Thread only, outside of layout and
     * animation.
     *
     * @return the error dialog
     */
    static ErrorDialog customDialog() {
        return (owner, title, header, text) -> new CustomDialog.Builder(CustomDialog.DialogType.ERROR)
                .owner(owner)
                .modality(owner == null ? Modality.APPLICATION_MODAL : Modality.WINDOW_MODAL)
                .title(title)
                .headerText(header)
                .contentText(text)
                .build()
                .showAndWait();
    }
}
