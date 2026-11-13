package org.karthu.fx.form;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Dimension2D;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.TraversalDirection;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Several lines of text in the form collection's shell: a text area without a frame of its own, whose lines wrap, with
 * a grip at its bottom right corner that the user drags to make the field taller or wider.
 *
 * <p><b>Value.</b> {@link #valueProperty()} is the text exactly as it stands in the area, line breaks included; the
 * empty text is no value. The lines {@linkplain #wrapTextProperty() wrap} at the area's width by default, and the
 * area is as high as its {@linkplain #prefRowCountProperty() rows} (four by default) unless the user resized it. The
 * {@linkplain #promptTextProperty() prompt text} shows while the area is empty.
 *
 * <p><b>Layout.</b> The text fills the shell left of a column at its right end: the {@code {}} icon (and any
 * {@linkplain #getIcons() trailing icon}) at the top, in a band as high as the first line with the room above and
 * below it, so it sits where it sits in a single-line field and stays there however tall the field is; the grip at the
 * bottom. While bound, the chip lies at the top left, where the text starts, in the same band. The area's scroll bars
 * show inside the shell, left of the column.
 *
 * <p><b>Grip.</b> Dragging the grip with the primary button changes the field's width and height by the distance the
 * pointer moves, from the size the field shows when the drag starts. The width never exceeds the width its parent
 * gives it (from the field's left edge to the end of the parent's content, and never more than a maximum width the
 * application set) and never falls below the field's minimum width; the height never falls below two rows. A double
 * click on the grip goes back to the default size. The grip is not focus traversable, keeps the focus where it is, and
 * resizes also while the field is read-only or bound, since resizing changes no value; while the field is disabled it
 * is disabled and does nothing. Its tooltip, {@link #gripTooltipTextProperty()}, names the drag and the double click.
 *
 * <p><b>Size.</b> {@link #userSizeProperty()} is the size the user dragged to, null for the default size. The
 * application can read it to remember the size and set it to restore it; karthu-fx stores nothing. While it is set,
 * the field's preferred size is that size (its height at least two rows) and its maximum width is that width, so a
 * parent that fills its width with the field does not stretch it. Its width is bounded at every layout as a drag is:
 * never wider than the width the parent gives, so a restored size or a parent that narrows after a drag never widens
 * the form; the property keeps the size as set, and the field takes its width again when the parent has the room.
 * While it is null, the field's preferred width is
 * the text area's and its height the rows', and it grows in width as a text area does. A preferred width or height the
 * application sets on the field itself ({@code setPrefWidth}) takes precedence over the user's size, as for any
 * region: to let the user resize a field, size it through its parent or its maximum width instead.
 *
 * <p><b>Keyboard.</b> Tab and Shift+Tab move the focus to the next and the previous field, as in every other field;
 * Ctrl+Tab inserts a tab in place of the selection, while the user may change the value. Ctrl+Space opens the binding
 * list, Backspace and Delete unbind (see {@link FieldShell}). The field has no choice of its own, so Alt+Down does
 * nothing.
 *
 * <p><b>Bound, read-only, disabled.</b> While bound (see {@link FieldShell}), the chip shows at the top left and the
 * text is hidden and kept: not editable, nothing of it can be selected, copied or cut, and it shows again after
 * unbinding. While read-only, the text is not editable but can be selected and copied, the {@code {}} icon is hidden
 * and the grip stays. While disabled, text, icons and grip are greyed out together, and the grip does nothing. A
 * paste or a cut changes nothing while the user may not change the value, also when called by code.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * The field carries the shell's classes and pseudo-classes (see {@link FieldShell}), and:
 * <ul>
 *   <li>{@code form-text-area-input}: the field, beside {@code form-frame}</li>
 *   <li>{@code form-text-area-text}: the text area, which keeps {@code text-area} and carries the shell's
 *       {@code form-shell-content}</li>
 *   <li>{@code form-text-area-grip}: the grip, a region holding its icon (Bootstrap {@code grip-horizontal})</li>
 * </ul>
 *
 * <p>Like every karthu-fx control, the field is used on the JavaFX Application Thread only.
 */
public class TextAreaInput extends FieldShell implements FormInput<String> {

    private static final int DEFAULT_ROW_COUNT = 4;

    /** The fewest rows the user can drag the field to. */
    private static final int SMALLEST_ROW_COUNT = 2;

    private static final String GRIP_TOOLTIP_TEXT = "Drag to resize (double-click: default size)";

    private final StringProperty value = new SimpleStringProperty(this, "value", "");
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText", true);
    private final IntegerProperty prefRowCount = new SimpleIntegerProperty(this, "prefRowCount", DEFAULT_ROW_COUNT);
    private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "");
    private final ObjectProperty<Dimension2D> userSize = new SimpleObjectProperty<>(this, "userSize");
    private final StringProperty gripTooltipText =
            new SimpleStringProperty(this, "gripTooltipText", GRIP_TOOLTIP_TEXT);

    private final TextArea area = new InputTextArea();
    private final StackPane grip = new StackPane();

    /** Measures one line as the area's skin lays its lines out: same font, same bounds. */
    private final Text lineMeasure = new Text();

    /**
     * The scroll pane of the area's skin, which holds its lines: found once per skin, null until it is found. The
     * height and the text's inset are measured through it on every layout pass.
     */
    private ScrollPane cachedScrollPane;

    /** The width of the parent bounds the user's size: while one is set, a change lays the field out again. */
    private final ChangeListener<Number> parentWidthListener = (observable, oldWidth, newWidth) -> {
        if (userSize.get() != null) {
            requestLayout();
        }
    };

    /** Where a drag of the grip started; null while the grip is not dragged. */
    private Drag drag;

    /**
     * Creates an empty field whose lines wrap, four rows high, without prompt text, at the default size.
     */
    public TextAreaInput() {
        getStyleClass().add("form-text-area-input");

        area.getStyleClass().add("form-text-area-text");
        area.wrapTextProperty().bind(wrapText);
        area.prefRowCountProperty().bind(prefRowCount);
        area.promptTextProperty().bind(promptText);
        value.bindBidirectional(area.textProperty());
        area.addEventFilter(KeyEvent.KEY_PRESSED, this::handleTab);

        // The text area's skin lays its lines out with these bounds.
        lineMeasure.setBoundsType(TextBoundsType.LOGICAL_VERTICAL_CENTER);
        lineMeasure.setTextOrigin(VPos.TOP);

        grip.getStyleClass().add("form-text-area-grip");
        grip.getChildren().add(FontIcon.of(BootstrapIcons.GRIP_HORIZONTAL));
        Tooltip gripTooltip = new Tooltip();
        gripTooltip.textProperty().bind(gripTooltipText);
        Tooltip.install(grip, gripTooltip);
        grip.accessibleTextProperty().bind(gripTooltipText);
        grip.addEventHandler(MouseEvent.MOUSE_PRESSED, this::gripPressed);
        grip.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::gripDragged);
        grip.addEventHandler(MouseEvent.MOUSE_RELEASED, this::gripReleased);
        grip.addEventHandler(MouseEvent.MOUSE_CLICKED, this::gripClicked);
        // An own control, not a trailing icon: it stays while read-only, which takes the icons out of the shell.
        getOwnControls().add(grip);
        userSize.addListener((observable, oldSize, newSize) -> requestLayout());
        parentProperty().addListener((observable, oldParent, newParent) -> {
            if (oldParent instanceof Region region) {
                region.widthProperty().removeListener(parentWidthListener);
            }
            if (newParent instanceof Region region) {
                region.widthProperty().addListener(parentWidthListener);
            }
        });
        area.skinProperty().addListener((observable, oldSkin, newSkin) -> cachedScrollPane = null);

        setContent(area);
    }

    // --- FormInput -----------------------------------------------------------------------------------

    /**
     * Returns the field itself, to place it in a layout.
     *
     * @return this field
     */
    @Override
    public Node getNode() {
        return this;
    }

    /**
     * The field's value: the text exactly as it stands in the area, line breaks included, the empty text when there is
     * none. Setting it sets the text.
     *
     * @return the value property
     */
    @Override
    public StringProperty valueProperty() {
        return value;
    }

    /**
     * Returns the field's text.
     *
     * @return the text in the area
     */
    public String getValue() {
        return value.get();
    }

    /**
     * Sets the field's text, as it is.
     *
     * @param text the new text
     */
    public void setValue(String text) {
        value.set(text);
    }

    // --- Properties ----------------------------------------------------------------------------------

    /**
     * Whether the lines wrap at the area's width (default true). While false, a long line scrolls sideways inside the
     * field.
     *
     * @return the wrap text property
     */
    public BooleanProperty wrapTextProperty() {
        return wrapText;
    }

    /**
     * Returns whether the lines wrap.
     *
     * @return true while the lines wrap
     */
    public boolean isWrapText() {
        return wrapText.get();
    }

    /**
     * Sets whether the lines wrap.
     *
     * @param wrap true to wrap the lines at the area's width
     */
    public void setWrapText(boolean wrap) {
        wrapText.set(wrap);
    }

    /**
     * How many rows high the field is at its default size (default 4), as a text area's preferred row count. The
     * user's size ({@link #userSizeProperty()}) replaces it while set.
     *
     * @return the preferred row count property
     */
    public IntegerProperty prefRowCountProperty() {
        return prefRowCount;
    }

    /**
     * Returns the preferred row count.
     *
     * @return the rows of the default size
     */
    public int getPrefRowCount() {
        return prefRowCount.get();
    }

    /**
     * Sets the preferred row count.
     *
     * @param rows the rows of the default size
     */
    public void setPrefRowCount(int rows) {
        prefRowCount.set(rows);
    }

    /**
     * The text the empty field shows (default empty: none).
     *
     * @return the prompt text property
     */
    public StringProperty promptTextProperty() {
        return promptText;
    }

    /**
     * Returns the prompt text.
     *
     * @return the prompt text
     */
    public String getPromptText() {
        return promptText.get();
    }

    /**
     * Sets the prompt text.
     *
     * @param text the prompt text, empty for none
     */
    public void setPromptText(String text) {
        promptText.set(text);
    }

    /**
     * The size the user dragged the field to with its grip, width and height in pixels; null (the default) for the
     * default size, to which a double click on the grip goes back. The application can read it to remember the size
     * and set it to restore it; karthu-fx stores nothing. A height below two rows counts as two rows, a width beyond
     * the room the parent gives as that room; the property keeps the size as set. See the class description for how
     * the size lays the field out.
     *
     * @return the user size property
     */
    public ObjectProperty<Dimension2D> userSizeProperty() {
        return userSize;
    }

    /**
     * Returns the size the user dragged the field to.
     *
     * @return the user's size, or null for the default size
     */
    public Dimension2D getUserSize() {
        return userSize.get();
    }

    /**
     * Sets the field's size as if the user had dragged it there, such as a remembered one.
     *
     * @param size the size, or null for the default size
     */
    public void setUserSize(Dimension2D size) {
        userSize.set(size);
    }

    /**
     * The grip's tooltip and accessible text (default <i>Drag to resize (double-click: default size)</i>). An
     * application that translates it names the double click in its own words.
     *
     * @return the grip tooltip text property
     */
    public StringProperty gripTooltipTextProperty() {
        return gripTooltipText;
    }

    /**
     * Returns the grip's tooltip.
     *
     * @return the grip's tooltip
     */
    public String getGripTooltipText() {
        return gripTooltipText.get();
    }

    /**
     * Sets the grip's tooltip.
     *
     * @param text the grip's tooltip
     */
    public void setGripTooltipText(String text) {
        gripTooltipText.set(text);
    }

    // --- Clipboard -----------------------------------------------------------------------------------

    /**
     * The field's text area: it copies, cuts, pastes and selects all through the shell's guarded clipboard, and keeps a
     * bound field's hidden text in: while bound, select-all selects nothing, and copy and cut give nothing. A paste or
     * a cut called by code changes nothing while the user may not change the value (disabled, read-only or bound), as
     * the keyboard's and the context menu's never do.
     */
    private final class InputTextArea extends TextArea {

        @Override
        public void paste() {
            guardedPaste(this);
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

    // --- Keyboard ------------------------------------------------------------------------------------

    /**
     * Tab and Shift+Tab move the focus on, as in every other field, rather than inserting a tab as a text area does;
     * Ctrl+Tab inserts the tab in place of the selection while the user may change the value, else it is left to the
     * text area (which moves the focus on). With Alt or Meta, Tab is left to the text area.
     */
    private void handleTab(KeyEvent event) {
        if (event.getCode() != KeyCode.TAB || event.isAltDown() || event.isMetaDown()) {
            return;
        }
        if (event.isControlDown()) {
            if (!event.isShiftDown() && userMayChangeValue()) {
                event.consume();
                area.replaceSelection("\t");
            }
            return;
        }
        event.consume();
        area.requestFocusTraversal(event.isShiftDown() ? TraversalDirection.PREVIOUS : TraversalDirection.NEXT);
    }

    // --- The grip ------------------------------------------------------------------------------------

    /** The start of a drag of the grip: where the pointer was on the screen, and the field's size then. */
    private record Drag(double screenX, double screenY, double width, double height) {
    }

    /**
     * A press on the grip starts a drag from the size the field shows. Consumed, so the focus stays where it is, as on
     * an icon. Screen coordinates, since the grip moves while it is dragged.
     */
    private void gripPressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || grip.isDisabled()) {
            return;
        }
        drag = new Drag(event.getScreenX(), event.getScreenY(), getWidth(), getHeight());
        event.consume();
    }

    /**
     * The grip dragged: the field takes the size the pointer moved to, its width within the room its parent gives and
     * at least its minimum width, its height at least two rows.
     */
    private void gripDragged(MouseEvent event) {
        if (drag == null || grip.isDisabled()) {
            return;
        }
        event.consume();
        double width = drag.width() + event.getScreenX() - drag.screenX();
        double height = drag.height() + event.getScreenY() - drag.screenY();
        width = Math.max(minWidth(-1), Math.min(width, widthRoom()));
        height = Math.max(height, smallestHeight());
        userSize.set(new Dimension2D(snapSizeX(width), snapSizeY(height)));
    }

    /** The grip let go: the drag ends. */
    private void gripReleased(MouseEvent event) {
        if (drag != null) {
            drag = null;
            event.consume();
        }
    }

    /** A double click on the grip goes back to the default size. */
    private void gripClicked(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || grip.isDisabled()) {
            return;
        }
        event.consume();
        if (event.getClickCount() == 2) {
            userSize.set(null);
        }
    }

    /**
     * The widest the user's size may make the field, dragged or restored: the room its parent gives it, from the
     * field's left edge to the end of the parent's content, and never more than a maximum width the application set. A
     * parent not laid out yet (no width) gives no room to go by, so a size restored before the first layout is taken
     * as it is, and the layout bounds it once the parent has its width.
     */
    private double widthRoom() {
        double room = Double.MAX_VALUE;
        if (getParent() instanceof Region parent && parent.getWidth() > 0) {
            room = parent.getWidth() - parent.snappedRightInset() - getLayoutX();
        }
        if (getMaxWidth() >= 0) {
            room = Math.min(room, getMaxWidth());
        }
        return room;
    }

    // --- Layout --------------------------------------------------------------------------------------

    /**
     * Lays the field out: the icons and {@code {}} in the band of the first line at the top right, the grip at the
     * bottom right, the text left of their column over the whole height, the chip at the top left while bound.
     */
    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double right = getWidth() - snappedRightInset();
        double bottom = getHeight() - snappedBottomInset();
        List<Node> icons = iconNodes();
        double band = bandHeight(icons);
        layoutTrailing(icons, top, band);
        double gripWidth = snapSizeX(grip.prefWidth(-1));
        double gripHeight = snapSizeY(grip.prefHeight(-1));
        grip.resizeRelocate(right - gripWidth, bottom - gripHeight, gripWidth, gripHeight);
        double textRight = right - columnWidth(icons);
        area.resizeRelocate(left, top, Math.max(0, textRight - left), Math.max(0, bottom - top));
        layoutChip(left, top, textRight, band);
    }

    /**
     * Returns how far right of the area's left edge its text starts, through the area, its scroll pane and its
     * content, where the chip goes while bound.
     *
     * @return the distance from the area's left edge to its text
     */
    @Override
    protected double chipInset() {
        return textInset(Side.LEFT);
    }

    /**
     * Returns the field's preferred width: the user's while set, within the room the parent gives (also when the
     * application restored the size, or the parent narrowed after a drag) and at least the minimum width; else the text
     * area's with the column at the right.
     *
     * @param height the height, ignored
     * @return the preferred width
     */
    @Override
    protected double computePrefWidth(double height) {
        Dimension2D size = userSize.get();
        if (size != null) {
            double width = Math.min(snapSizeX(size.getWidth()), widthRoom());
            return Math.max(width, computeMinWidth(height));
        }
        return snappedLeftInset() + snapSizeX(area.prefWidth(-1)) + columnWidth(iconNodes()) + snappedRightInset();
    }

    /**
     * Returns the field's minimum width: the text area's with the column at the right.
     *
     * @param height the height, ignored
     * @return the minimum width
     */
    @Override
    protected double computeMinWidth(double height) {
        return snappedLeftInset() + snapSizeX(area.minWidth(-1)) + columnWidth(iconNodes()) + snappedRightInset();
    }

    /**
     * Returns the field's maximum width: the user's width while set (as the preferred width bounds it), so a parent
     * that fills its width does not stretch the field beyond it; else unbounded, as a text area's.
     *
     * @param height the height, ignored
     * @return the maximum width
     */
    @Override
    protected double computeMaxWidth(double height) {
        if (userSize.get() != null) {
            return computePrefWidth(height);
        }
        return Double.MAX_VALUE;
    }

    /**
     * Returns the field's preferred height: the user's while set (at least two rows), else as high as its rows; never
     * lower than its column of icons and grip.
     *
     * @param width the width, ignored
     * @return the preferred height
     */
    @Override
    protected double computePrefHeight(double width) {
        Dimension2D size = userSize.get();
        if (size != null) {
            return Math.max(snapSizeY(size.getHeight()), smallestHeight());
        }
        double rows = snappedTopInset() + snapSizeY(areaPrefHeight()) + snappedBottomInset();
        return Math.max(rows, columnHeight());
    }

    /**
     * The text area's preferred height, its rows measured as its lines are laid out now. JavaFX's text area skin
     * measures its rows (its scroll pane's preferred viewport height) again when its padding or its row count changes,
     * but not when its font or its lines' bounds type changes, both of which stylesheets set (the bounds type JavaFX's
     * own rule for {@code .text}): styles that come in after the padding leave the rows measured as before, so two
     * fields of one font could differ in height by the order their styles came in (by 6px in the showcase's overview).
     * So the rows are measured here, with the line the layout uses, in place of the skin's measure; the rest of the
     * area's preferred height (its insets, the room of a horizontal scroll bar) stays the skin's.
     */
    private double areaPrefHeight() {
        double prefHeight = area.prefHeight(-1);
        ScrollPane scrollPane = skinScrollPane();
        Region lines = skinLines();
        if (scrollPane != null && scrollPane.getPrefViewportHeight() > 0 && lines != null) {
            double rows = prefRowCount.get() * lineHeight() + lines.snappedTopInset() + lines.snappedBottomInset();
            prefHeight += rows - scrollPane.getPrefViewportHeight();
        }
        return prefHeight;
    }

    /**
     * The scroll pane of the area's skin, a child of the area: looked for until found, then kept until the skin
     * changes, so a layout pass does not search the area's nodes. Null before the skin exists.
     */
    private ScrollPane skinScrollPane() {
        if (cachedScrollPane == null) {
            for (Node child : area.getChildrenUnmodifiable()) {
                if (child instanceof ScrollPane scrollPane) {
                    cachedScrollPane = scrollPane;
                    break;
                }
            }
        }
        return cachedScrollPane;
    }

    /** The region that holds the area's lines, the skin's scroll pane's content; null before the skin exists. */
    private Region skinLines() {
        ScrollPane scrollPane = skinScrollPane();
        if (scrollPane != null && scrollPane.getContent() instanceof Region lines) {
            return lines;
        }
        return null;
    }

    /**
     * Returns the field's preferred height: a parent never squeezes it, the user resizes it.
     *
     * @param width the width, ignored
     * @return the preferred height
     */
    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    /**
     * Returns the field's preferred height: a parent never stretches it, the user resizes it.
     *
     * @param width the width, ignored
     * @return the preferred height
     */
    @Override
    protected double computeMaxHeight(double width) {
        return computePrefHeight(width);
    }

    /** The trailing nodes but the grip: the icons and {} in the band at the top. */
    private List<Node> iconNodes() {
        List<Node> icons = trailingNodes();
        icons.remove(grip);
        return icons;
    }

    /** The width of the column at the right end: the icons above, the grip below. */
    private double columnWidth(List<Node> icons) {
        return Math.max(trailingWidth(icons), snapSizeX(grip.prefWidth(-1)));
    }

    /** The height the column needs: the band of the icons, then the grip, with the field's insets. */
    private double columnHeight() {
        return snappedTopInset() + bandHeight(iconNodes()) + snapSizeY(grip.prefHeight(-1)) + snappedBottomInset();
    }

    /**
     * The band of the first line at the top: the first line with the room above it, and as much below it, so an icon
     * centred in it sits beside the first line, where it sits in a single-line field; at least as high as the
     * tallest icon.
     */
    private double bandHeight(List<Node> icons) {
        double lineBand = snapSizeY(2 * textInset(Side.TOP) + lineHeight());
        return Math.max(lineBand, trailingPrefHeight(icons));
    }

    /**
     * The lowest the user can drag the field: two rows, as the area would be with two preferred rows (its preferred
     * height less the rows above two), and never lower than the column.
     */
    private double smallestHeight() {
        double rows = areaPrefHeight() + (SMALLEST_ROW_COUNT - prefRowCount.get()) * lineHeight();
        return Math.max(snappedTopInset() + snapSizeY(rows) + snappedBottomInset(), columnHeight());
    }

    /** The height of one line of the area's text, in the area's font. */
    private double lineHeight() {
        lineMeasure.setFont(area.getFont());
        return lineMeasure.getLayoutBounds().getHeight();
    }

    /**
     * How far in from the area's edge its text starts, through the area, its scroll pane and its content, as the
     * area's skin lays them out; the area's own inset before the skin exists.
     */
    private double textInset(Side side) {
        double inset = inset(area, side);
        ScrollPane scrollPane = skinScrollPane();
        if (scrollPane != null) {
            inset += inset(scrollPane, side);
        }
        Region lines = skinLines();
        if (lines != null) {
            inset += inset(lines, side);
        }
        return inset;
    }

    private static double inset(Region region, Side side) {
        return side == Side.TOP ? region.snappedTopInset() : region.snappedLeftInset();
    }
}
