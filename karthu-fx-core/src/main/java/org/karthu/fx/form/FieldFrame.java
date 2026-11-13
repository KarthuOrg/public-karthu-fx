package org.karthu.fx.form;

import javafx.scene.control.TextField;

/**
 * The frame of the form collection's single-line text inputs: a {@link FieldShell} whose content is one borderless
 * text field, so that the text and its small trailing icons read as one control. {@link PathInput} is built on it, and
 * so are later single-line inputs and an application's own.
 *
 * <p>Everything the inputs share comes from the shell: the trailing icons, the state, read-only, the binding with its
 * chip, list and {@code {}} icon, the guard {@link #userMayChangeValue()} and the drop look (see {@link FieldShell}).
 * The frame adds the text field and what only a text field has.
 *
 * <p><b>Layout.</b> The shell's single-line layout: the {@linkplain #getTextField() text field} fills the frame left
 * of the {@linkplain #getIcons() trailing icons}. The frame carries the border, the background and the radius of a
 * karthu-fx text field, and the inner text field keeps a text field's padding, so a frame is exactly as high as a plain
 * text field in the same theme. Its preferred width is the text field's plus the icons'; it grows in width but never
 * in height, as a text field.
 *
 * <p><b>Focus.</b> The text field is the frame's {@linkplain #getFocusTarget() focus target}: {@link #requestFocus()}
 * passes the focus on to it, and a mouse press in the frame that no button takes focuses it, also on the chip.
 *
 * <p><b>Paste and copy.</b> Every paste into the text field inserts what {@link #pastedText(String)} makes of the
 * clipboard's text, by default the text itself. While the user may not change the value ({@link #userMayChangeValue()}:
 * disabled, read-only or bound) a paste or a cut changes nothing, also when called by code.
 *
 * <p><b>Bound and read-only.</b> While bound the text field keeps its value, hidden: not editable, and nothing of it can
 * be selected, copied or cut (select-all selects nothing, copy and cut give nothing). After unbinding the text shows
 * again as it was. While read-only the text can be selected and copied but not changed: typing, cutting, pasting and
 * deleting change nothing.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The frame carries the shell's classes and pseudo-classes (see {@link FieldShell}), and:
 * <ul>
 *   <li>{@code form-frame-text}: the inner text field, which keeps {@code text-field} and carries the shell's
 *       {@code form-shell-content}</li>
 * </ul>
 *
 * <p>Like every karthu-fx control, the frame is used on the JavaFX Application Thread only.
 */
public class FieldFrame extends FieldShell {

    private final TextField textField = new FrameTextField();

    /**
     * Creates a frame with an empty text field and no icons.
     */
    public FieldFrame() {
        textField.getStyleClass().add("form-frame-text");
        setContent(textField);
    }

    /**
     * Returns the inner text field, the frame's content and focus target. An input built on the frame reads and writes
     * its text here; an application may set its column count or alignment, but should leave its border and background
     * to the frame.
     *
     * @return the inner text field
     */
    public final TextField getTextField() {
        return textField;
    }

    // --- Paste and copy ------------------------------------------------------------------------------

    /**
     * Returns what a paste into the text field inserts in place of its selection, given the text on the clipboard:
     * by default the text itself. An input built on the frame overrides it to adapt pasted text, as the path input
     * removes the quotes of a path copied with the Windows Explorer's <i>Copy as path</i>. Every paste passes here
     * (the shortcut, the context menu, {@link TextField#paste()}); typed text and text set through the API never do.
     * While the user may not change the value ({@link #userMayChangeValue()}), a paste inserts nothing.
     *
     * @param pasted the text on the clipboard, never null
     * @return the text to insert, never null
     */
    protected String pastedText(String pasted) {
        return pasted;
    }

    /**
     * The frame's text field: a text field that pastes what {@link #pastedText(String)} makes of the clipboard's text,
     * and that copies, cuts, pastes and selects all through the shell's guarded clipboard. Unlike JavaFX's own paste,
     * a paste may share an undo step with text typed just before it. A paste or a cut called by code changes nothing
     * while the user may not change the value (disabled, read-only or bound), as the keyboard's and the context
     * menu's never do. While bound the hidden value is not handed out: select-all selects nothing, and copy and cut
     * give nothing.
     */
    private final class FrameTextField extends TextField {

        @Override
        public void paste() {
            guardedPaste(this, FieldFrame.this::pastedText);
        }

        @Override
        public void copy() {
            guardedCopy(this);
        }

        @Override
        public void cut() {
            guardedCut(this);
        }

        @Override
        public void selectAll() {
            guardedSelectAll(this);
        }
    }
}
