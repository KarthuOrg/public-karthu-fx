package org.karthu.fx.form;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The shell of every input of the form collection: one border around the input's content (the JavaFX control it
 * wraps), its small trailing icons and its own controls, so that the input reads as one control, with everything an
 * input shares: its state, read-only, its binding to a variable with the chip, the binding list and the {@code {}}
 * icon, and the drop look. A field type puts its content into the shell and adds what only it has; {@link FieldFrame},
 * the single-line text frame, is the shell around one text field, and {@link PathInput} is built on it.
 *
 * <p><b>Children</b>, left to right: the type's {@linkplain #setLeadingGraphic(Node) leading graphic}, if it has one
 * (such as a search field's magnifier), the {@linkplain #setContent(Node) content}, the
 * {@linkplain #getIcons() trailing icons}, the type's {@linkplain #getOwnControls() own controls} (such as a
 * {@linkplain #createDivider() divider} and a chevron, or stacked arrows), and the {@code {}} icon, always last. While
 * bound, the binding's chip lies over the content where its text starts.
 *
 * <p><b>Layout.</b> By default the shell lays its children out on one line: the content fills the shell left of the
 * trailing nodes (the icons, the own controls and the {@code {}} icon), which sit at the shell's right end in their
 * order, 1px apart, each centred vertically and as high as the content allows up to its own maximum height: an icon
 * keeps its height, a divider spans the content's height. A leading graphic sits at the shell's left edge, centred
 * vertically in the same way, and the content starts right after it; without one the content starts at the left edge.
 * The shell's right padding is the room after the last trailing node: without one the content reaches the right edge,
 * so a right-aligned number ends where it ends while read-only. The shell is as high as its content, its leading
 * graphic or its tallest trailing node, never lower, never higher, and grows in width only, as a text field. A field
 * type with another shape, such as several lines with the icons at the top, overrides {@link #layoutChildren()} and the
 * size methods and places the parts with {@link #trailingNodes()}, {@link #layoutTrailing(List, double, double)} and
 * {@link #layoutChip(double, double, double, double)}; such a type has no leading graphic, or places it itself.
 *
 * <p><b>Focus.</b> The {@linkplain #getFocusTarget() focus target}, by default the content, takes the focus: the
 * shell's {@link #requestFocus()} passes it on, a form row names it as the input's accessible label, and a mouse press
 * in the shell that no control takes focuses it, also on the chip and the leading graphic. The leading graphic, the
 * icons and the own controls are not focus traversable, so Tab passes the shell in one step. While anything inside the
 * shell has the focus, the theme draws the focus border around content and icons together (the pseudo-class
 * {@code :focus-within}).
 *
 * <p><b>Icons.</b> An icon added to {@link #getIcons()} is styled as a trailing icon: it gets the style classes
 * {@code icon-button} and {@code form-frame-icon}, shows its graphic only, and leaves the tab order. The theme draws it
 * borderless and muted, highlighted with the hover background under the pointer, where its tooltip shows. An icon that
 * is not managed takes no room. The shell never changes an icon's disabling, visibility or management: an icon is
 * disabled by whoever added it, as the path input disables its browse icon while bound. The same holds for the own
 * controls, which the type styles itself ({@link #styleAsIcon(ButtonBase)} styles one as a trailing icon).
 *
 * <p><b>What the user may change.</b> One guard, {@link #userMayChangeValue()}, decides whether the user may change
 * the value now: not while the shell is disabled, read-only or bound. Every change of the value other than typing asks
 * it: a frame's paste and cut, the path input's dialog and drop, a type's arrows and lists. A type writes the value
 * the user gave through {@link #setValueByUser(Property, Object)}, which asks the guard and leaves a value property the
 * application bound alone. The binding may still be switched or removed while bound, but not while the shell is
 * disabled or read-only.
 *
 * <p><b>Binding.</b> The shell shows its {@linkplain #getBinding() binding} to a variable. While the binding is
 * bindable or bound and the shell is not read-only, a {@code {}} icon (Bootstrap {@code braces}) is the last trailing
 * node; it and Ctrl+Space in the shell open the {@linkplain #getBindingMenu() binding list} under the icon: one entry
 * per candidate (its name, then its detail muted, the bound one checked) or a disabled <i>No variable fits</i>, then,
 * while bound, a separator and <i>Remove binding</i>. Choosing an entry binds to its candidate or switches to it,
 * <i>Remove binding</i> unbinds. While bound, the shell shows a chip with the bound name and a small <i>&times;</i>
 * (Bootstrap {@code x}) in place of the content, and the {@code {}} icon takes the bound look. Binding never changes
 * the value: the content keeps it, hidden ({@link #updateContentHidden(boolean)}) and not editable
 * ({@link #updateContentEditable(boolean)}), and shows it again after unbinding. The focus target stays where it is:
 * a click on the chip gives it the focus, Backspace or Delete in the shell unbind, as the chip's <i>&times;</i> does.
 * A binding whose variable is missing shows the chip in the error colour with the missing tooltip. Binding, switching
 * and unbinding by the user set the binding's bound name, clearing its missing flag first. Not while the shell is
 * disabled or read-only.
 *
 * <p><b>Disabled.</b> A disabled shell greys out content, leading graphic and icons together, as a disabled button. A
 * binding list that shows closes, and its entries change nothing while the shell is disabled.
 *
 * <p><b>Read-only.</b> While {@linkplain #readOnlyProperty() read-only} (the pseudo-class {@code :readonly}), the value
 * can be selected and copied but not changed. The trailing icons leave the shell, their own properties untouched, and
 * come back when read-only ends, and the {@code {}} icon hides; the own controls stay as the type sets them, so a type
 * hides those that change the value and keeps the others. A bound shell shows its chip without the <i>&times;</i>.
 * Ctrl+Space, Backspace and Delete neither open the binding list nor unbind, and a binding list that shows closes. The
 * theme draws the shell on the read-only background of a read-only text field, keeping the focus border and the
 * state's border. The application still sets the value, the binding and the state. Unlike a disabled shell, a
 * read-only one is not greyed out and keeps its place in the tab order, so the user can focus it to copy its value.
 *
 * <p><b>State.</b> The shell shows its {@linkplain #stateProperty() state} as the pseudo-class {@code :warning} or
 * {@code :error}, which the theme draws as a border in the warning or the error colour around content and icons, also
 * while the shell has the focus. A {@link FormField} sets the state from its messages; an input that stands alone gets
 * it set directly. An input that accepts a dragged item shows it with {@link #setDropTarget(boolean)}.
 *
 * <p><b>Hooks for a field type</b>, all protected:
 * <ul>
 *   <li>{@link #setContent(Node)}: the control the type wraps, which may be replaced later (two text fields shown one
 *       at a time), the focus moving with it; {@link #getContent()} returns it</li>
 *   <li>{@link #setFocusTarget(Node)}: a focus target other than the content, such as a part of it</li>
 *   <li>{@link #setLeadingGraphic(Node)}: one graphic left of the content, such as a magnifier, which takes no
 *       clicks and no focus; {@link #getLeadingGraphic()} returns it</li>
 *   <li>{@link #getOwnControls()}: the type's own controls between the icons and {@code {}};
 *       {@link #createDivider()} makes the shell's divider, {@link #styleAsIcon(ButtonBase)} styles a button as a
 *       trailing icon, {@link #updateOwnControl(Node, boolean)} shows, hides and disables one</li>
 *   <li>{@link #updateContentEditable(boolean)}: how the content (and the own controls) follow read-only and the
 *       binding</li>
 *   <li>{@link #updateContentHidden(boolean)}: how the content's value is hidden while bound</li>
 *   <li>{@link #layoutChildren()} and the size methods, with {@link #trailingNodes()},
 *       {@link #layoutTrailing(List, double, double)}, {@link #trailingWidth(List)},
 *       {@link #trailingPrefHeight(List)}, {@link #layoutChip(double, double, double, double)} and
 *       {@link #chipInset()}: a layout of the type's own</li>
 *   <li>{@link #userMayChangeValue()}: the one guard; {@link #setValueByUser(Property, Object)}: the one way to write
 *       the value for the user; {@link #setDropTarget(boolean)}: the drop look</li>
 * </ul>
 * A type sets its content in its constructor, after the fields its hooks use, since the shell calls the hooks for the
 * content as soon as it is set; before that, the shell calls none.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change (they are also listed in
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>,
 * <i>Form fields</i>).
 * <ul>
 *   <li>{@code form-frame}: the shell</li>
 *   <li>{@code form-shell-content}: the content, the control the type wraps</li>
 *   <li>{@code form-shell-leading}: the leading graphic, where a type has one</li>
 *   <li>{@code form-frame-icon}: each trailing icon, which also carries {@code icon-button}</li>
 *   <li>{@code form-shell-divider}: the shell's divider, an own control of a type that has one</li>
 * </ul>
 *
 * <p><b>Pseudo-classes of the state, read-only and drop target</b>, on {@code form-frame}, part of the same contract:
 * <ul>
 *   <li>{@code :warning}: the state is {@link FieldState#WARNING}</li>
 *   <li>{@code :error}: the state is {@link FieldState#ERROR}</li>
 *   <li>{@code :readonly}: the shell is read-only</li>
 *   <li>{@code :drop-target}: the input accepts the drag that hovers over it</li>
 * </ul>
 *
 * <p>The binding's style classes and pseudo-classes, part of the same contract:
 * <ul>
 *   <li>{@code :bound}: the shell, while bound</li>
 *   <li>{@code form-frame-bind}: the {@code {}} icon, beside {@code form-frame-icon} and {@code icon-button}</li>
 *   <li>{@code form-chip}: the chip, a label with the bound name; {@code :missing} while its variable is missing</li>
 *   <li>{@code form-chip-remove}: the chip's <i>&times;</i>, its graphic, beside {@code icon-button}</li>
 *   <li>{@code form-binding-menu}: the binding list, beside {@code context-menu}</li>
 *   <li>{@code form-binding-candidate}: a candidate's entry, a check menu item whose graphic holds
 *       {@code form-binding-name}, the name, and {@code form-binding-detail}, the detail</li>
 *   <li>{@code form-binding-remove}: the entry <i>Remove binding</i></li>
 *   <li>{@code form-binding-empty}: the disabled entry <i>No variable fits</i></li>
 * </ul>
 *
 * <p>Like every karthu-fx control, the shell is used on the JavaFX Application Thread only.
 */
public abstract class FieldShell extends Region {

    /** The gap between two trailing nodes; the shell's right padding is the room after the last one, if any. */
    private static final double ICON_GAP = 1;

    private static final PseudoClass WARNING_PSEUDO_CLASS = PseudoClass.getPseudoClass("warning");
    private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");
    private static final PseudoClass READ_ONLY_PSEUDO_CLASS = PseudoClass.getPseudoClass("readonly");
    private static final PseudoClass DROP_TARGET_PSEUDO_CLASS = PseudoClass.getPseudoClass("drop-target");
    private static final PseudoClass BOUND_PSEUDO_CLASS = PseudoClass.getPseudoClass("bound");

    /** The content, the control the type wraps; null until the type sets it. */
    private Node content;
    /** The type's leading graphic left of the content, such as a magnifier; null for none (every input but a few). */
    private Node leadingGraphic;
    /** The type's own focus target, or null when the content is the focus target. */
    private Node focusTarget;
    private final ObservableList<ButtonBase> icons = FXCollections.observableArrayList();
    private final ObservableList<Node> ownControls = FXCollections.observableArrayList();
    /** The clipboard the inputs paste from and copy to: the system clipboard unless a test replaced it. */
    private ShellClipboard clipboard = ShellClipboard.system();

    private final ObjectProperty<FieldState> state = new SimpleObjectProperty<>(this, "state", FieldState.NORMAL);
    private final BooleanProperty readOnly = new SimpleBooleanProperty(this, "readOnly", false);

    // The binding (see "Binding" below): the {} icon after the trailing icons, the chip over the content while bound.
    private final FieldBinding binding = new FieldBinding();
    private final BindingControls bindingControls = new BindingControls(this, binding);
    /**
     * An empty clip, set on the content while bound: it hides the value, while the content stays visible and so keeps
     * the focus.
     */
    private final Rectangle emptyClip = new Rectangle();
    /**
     * Creates a shell without content, icons and own controls. A field type sets its content in its constructor.
     */
    protected FieldShell() {
        getStyleClass().add("form-frame");
        icons.addListener((ListChangeListener<ButtonBase>) change -> {
            for (ButtonBase icon : icons) {
                styleAsIcon(icon);
            }
            updateChildren();
        });
        ownControls.addListener((ListChangeListener<Node>) change -> {
            for (Node control : ownControls) {
                control.setFocusTraversable(false);
            }
            updateChildren();
        });
        buildBinding();
        updateChildren();
        state.addListener((observable, oldState, newState) -> updateStatePseudoClasses());
        readOnly.addListener((observable, wasReadOnly, isReadOnly) -> updateReadOnlyState());
    }

    // --- Content and focus ---------------------------------------------------------------------------

    /**
     * Puts the type's content into the shell, the control it wraps, in place of the content before: the shell's first
     * child (after the leading graphic, if there is one), with the style class {@code form-shell-content}, the focus
     * target unless the type set another one. The shell hides the old content's value no longer, then calls
     * {@link #updateContentHidden(boolean)} and {@link #updateContentEditable(boolean)} for the new content, so it
     * shows the binding and read-only at once. A type that shows one of two controls at a time (a masked and a plain
     * text field, a combo box and a text to copy) sets the shown one each time, also from
     * {@link #updateContentEditable(boolean)}. The focus moves with the content: when the content before was its
     * scene's focus owner (also while the window is in the background), the new content takes the focus, last, after
     * the hooks.
     *
     * @param newContent the control the type wraps, never null
     * @throws IllegalArgumentException if the content is null
     */
    protected final void setContent(Node newContent) {
        if (newContent == null) {
            throw new IllegalArgumentException("the content must not be null");
        }
        Node before = content;
        Scene scene = getScene();
        boolean focused = before != null && before != newContent && scene != null && scene.getFocusOwner() == before;
        if (before != null && before.getClip() == emptyClip) {
            before.setClip(null);
        }
        content = newContent;
        if (!content.getStyleClass().contains("form-shell-content")) {
            content.getStyleClass().add("form-shell-content");
        }
        updateChildren();
        updateContentHidden(binding.isBound());
        updateContentEditable(isEditable());
        if (focused) {
            // The content now: a hook may have put another one in place of the new one.
            content.requestFocus();
        }
    }

    /**
     * Returns the type's content, the control it wraps.
     *
     * @return the content, or null before the type set it
     */
    protected final Node getContent() {
        return content;
    }

    /**
     * Sets a focus target other than the content, such as a part of it; null makes the content the focus target
     * again.
     *
     * @param target the node that takes the focus, or null for the content
     */
    protected final void setFocusTarget(Node target) {
        focusTarget = target;
    }

    /**
     * Returns the node that takes the focus in the shell and that a label names as the input's accessible label: the
     * type's focus target, else its content (a frame's text field). An input built on the shell offers it as its
     * {@link FormInput#getFocusTarget()}.
     *
     * @return the focus target; the shell itself only while it has no content
     */
    public final Node getFocusTarget() {
        if (focusTarget != null) {
            return focusTarget;
        }
        return content != null ? content : this;
    }

    /**
     * Passes the focus on to the {@linkplain #getFocusTarget() focus target}.
     */
    @Override
    public void requestFocus() {
        Node target = getFocusTarget();
        if (target == this) {
            super.requestFocus();
        } else {
            target.requestFocus();
        }
    }

    // --- Leading graphic -----------------------------------------------------------------------------

    /**
     * Puts one graphic left of the content, in place of the one before, or removes it: a small sign of what the field
     * is, such as a search field's magnifier. It gets the style class {@code form-shell-leading} and is the shell's
     * first child; the single-line layout places it at the shell's left edge, centred vertically, as high as the
     * content allows up to its own maximum height (as a trailing node), and the content starts right after it. It is no
     * control: it leaves the tab order and takes no clicks (it is mouse transparent), so a press on it reaches the
     * shell, which focuses the focus target, as a press on the shell's empty space does. It greys out with a disabled
     * shell and stays while read-only or bound. Without one, the shell lays its children out as it always does: the
     * content starts at the shell's left edge.
     *
     * @param graphic the graphic, such as an icon in a region whose padding gives the room around it, or null for none
     */
    protected final void setLeadingGraphic(Node graphic) {
        leadingGraphic = graphic;
        if (graphic != null) {
            addStyleClass(graphic, "form-shell-leading");
            graphic.setFocusTraversable(false);
            graphic.setMouseTransparent(true);
        }
        updateChildren();
    }

    /**
     * Returns the type's leading graphic, left of the content.
     *
     * @return the leading graphic, or null when the shell has none
     */
    protected final Node getLeadingGraphic() {
        return leadingGraphic;
    }

    // --- Icons and own controls ----------------------------------------------------------------------

    /**
     * Returns the trailing icons, left to right, shown at the shell's right end after the content. Each icon added is
     * styled as a trailing icon (see the class description) and must appear at most once. The shell never changes an
     * icon's disabling: an icon is the application's (or the input's) to disable. While read-only the icons leave the
     * shell, this list unchanged.
     *
     * @return the live list of trailing icons
     */
    public final ObservableList<ButtonBase> getIcons() {
        return icons;
    }

    /**
     * Returns the type's own controls, left to right, shown between the trailing icons and the {@code {}} icon: a
     * divider and a chevron, or stacked arrows. Each one added leaves the tab order (a type keeps the nodes inside an
     * own control out of it itself) and must appear at most once; the type styles them. Unlike the icons they stay in
     * the shell while read-only: the type hides those that change the value (not managed, they take no room) and
     * disables them while bound, in {@link #updateContentEditable(boolean)}, with {@link #updateOwnControl(Node,
     * boolean)}.
     *
     * @return the live list of the type's own controls
     */
    protected final ObservableList<Node> getOwnControls() {
        return ownControls;
    }

    /**
     * Shows or hides one of the type's own controls, and disables it while the shell is bound: what every type does
     * with its own controls when read-only or the binding change. A hidden control is not managed, so it takes no
     * room; a shown one is disabled while bound, when it would change the hidden value, and enabled otherwise (a
     * disabled shell disables it with everything else). A control that stays while read-only and bound, such as the
     * text area's grip, is left to the type.
     *
     * @param control one of the type's own controls, or a node that holds several of them
     * @param shown   true to show the control
     */
    protected final void updateOwnControl(Node control, boolean shown) {
        control.setVisible(shown);
        control.setManaged(shown);
        control.setDisable(binding.isBound());
    }

    /**
     * Runs an action whenever the focus owner of the shell's scene moves from a node to another one, in whichever scene
     * the shell is: how an input notices that the user left a part of it (the password's revealed field, the editable
     * combo box with its list). The window losing the focus moves no focus owner; a type that cares watches the node's
     * own focused flag for that.
     *
     * @param node   the node the focus leaves
     * @param action what to do then
     */
    final void whenFocusLeaves(Node node, Runnable action) {
        ChangeListener<Node> listener = (observable, oldOwner, newOwner) -> {
            if (oldOwner == node && newOwner != node) {
                action.run();
            }
        };
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.focusOwnerProperty().removeListener(listener);
            }
            if (newScene != null) {
                newScene.focusOwnerProperty().addListener(listener);
            }
        });
        if (getScene() != null) {
            getScene().focusOwnerProperty().addListener(listener);
        }
    }

    /**
     * Creates the shell's divider for a type's own controls: a region with the style class
     * {@code form-shell-divider}, which the theme draws as a 1px line in the control border colour, as high as the
     * content, as before a chevron or the stacked arrows.
     *
     * @return a new divider, not focus traversable
     */
    protected static Region createDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("form-shell-divider");
        divider.setFocusTraversable(false);
        return divider;
    }

    /**
     * Styles a button as a trailing icon, as the shell styles each of {@link #getIcons()}: the style classes
     * {@code icon-button} and {@code form-frame-icon}, its graphic only, out of the tab order. A type styles its own
     * icon-like controls (a chevron, an eye) with it, so they look and behave like the icons.
     *
     * @param icon the button to style
     */
    protected static void styleAsIcon(ButtonBase icon) {
        addStyleClass(icon, "icon-button");
        addStyleClass(icon, "form-frame-icon");
        icon.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        icon.setFocusTraversable(false);
    }

    private static void addStyleClass(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    // --- State ---------------------------------------------------------------------------------------

    /**
     * The shell's state (default {@link FieldState#NORMAL}), shown as the pseudo-class {@code :warning} or
     * {@code :error}; null counts as normal. An input built on the shell offers it as its
     * {@link FormInput#stateProperty()}.
     *
     * @return the state property
     */
    public final ObjectProperty<FieldState> stateProperty() {
        return state;
    }

    /**
     * Returns the shell's state.
     *
     * @return the state, or null when it was set to null (which counts as normal)
     */
    public final FieldState getState() {
        return state.get();
    }

    /**
     * Sets the shell's state.
     *
     * @param value the new state; null counts as normal
     */
    public final void setState(FieldState value) {
        state.set(value);
    }

    private void updateStatePseudoClasses() {
        FieldState current = state.get();
        pseudoClassStateChanged(WARNING_PSEUDO_CLASS, current == FieldState.WARNING);
        pseudoClassStateChanged(ERROR_PSEUDO_CLASS, current == FieldState.ERROR);
    }

    // --- Read-only -----------------------------------------------------------------------------------

    /**
     * Whether the shell is read-only (default false), shown as the pseudo-class {@code :readonly}: the value can be
     * selected and copied but not changed, no icon shows, the chip has no remove button, and no shortcut changes the
     * value or the binding (see the class description). An input built on the shell offers it as its
     * {@link FormInput#readOnlyProperty()}.
     *
     * @return the read-only property
     */
    public final BooleanProperty readOnlyProperty() {
        return readOnly;
    }

    /**
     * Returns whether the shell is read-only.
     *
     * @return true while read-only
     */
    public final boolean isReadOnly() {
        return readOnly.get();
    }

    /**
     * Sets whether the shell is read-only.
     *
     * @param value true to make the shell read-only
     */
    public final void setReadOnly(boolean value) {
        readOnly.set(value);
    }

    /** Shows read-only: {@code :readonly}, no icon, the content not editable, and a binding list that shows closes. */
    private void updateReadOnlyState() {
        pseudoClassStateChanged(READ_ONLY_PSEUDO_CLASS, readOnly.get());
        if (readOnly.get()) {
            bindingControls.closeMenu();
        }
        bindingControls.update(readOnly.get());
        updateChildren();
        if (content != null) {
            updateContentEditable(isEditable());
        }
    }

    // --- What the user may change --------------------------------------------------------------------

    /**
     * Returns whether the user may change the input's value now: not while the shell is disabled or read-only, and not
     * while it is bound, when the value is hidden and not used. Every way the shell and the inputs built on it change
     * the value for the user, other than typing into an editable content, asks this one guard first: a paste or a cut
     * into a frame's text field, the path input's dialog and drop, a type's arrows and lists. A type that writes its
     * value property for the user writes it through {@link #setValueByUser(Property, Object)}, which asks it.
     *
     * @return true when the user may change the value now
     */
    protected final boolean userMayChangeValue() {
        return userMayChangeBinding() && !binding.isBound();
    }

    /**
     * Writes a value the user gave, the one way an input on the shell writes its value property for the user: a step
     * of the arrows, a choice from a list, a dialog or a drop, a chosen suggestion, the text of an editor the input
     * keeps apart from its value. Only while the user may change the value ({@link #userMayChangeValue()}) and the
     * property is not bound: an application that binds the value property decides the value, so the user's change is
     * left out and nothing fails; the input then shows its value again where it showed the user's change.
     *
     * @param property the input's value property
     * @param newValue the value the user gave
     * @param <T>      the type of the value
     * @return true when the value was written; false when the user's change was left out
     */
    protected final <T> boolean setValueByUser(Property<T> property, T newValue) {
        if (!userMayChangeValue() || property.isBound()) {
            return false;
        }
        property.setValue(newValue);
        return true;
    }

    /**
     * Whether the user may change the binding now: not while disabled or read-only. Unlike the value, a bound shell's
     * binding may be switched or removed. Package-private: the binding's controls ask it.
     *
     * @return true if the user may change the binding now
     */
    boolean userMayChangeBinding() {
        return !isDisabled() && !readOnly.get();
    }

    /** Whether the content may be edited: neither bound nor read-only. Disabled is the content's own, inherited. */
    private boolean isEditable() {
        return !binding.isBound() && !readOnly.get();
    }

    // --- Hooks for the content -----------------------------------------------------------------------

    /**
     * Makes the content follow read-only and the binding: the shell calls it when the content is set and whenever the
     * shell becomes or stops being read-only or bound, with true while it is neither (calls may repeat a value). By
     * default a text input control as content becomes editable or not; any other content is left as it is. A type
     * with another content, or with own controls, overrides it and tells read-only and bound apart through
     * {@link #isReadOnly()} and {@code getBinding().isBound()}: a combo opens no list, its chevron disabled while bound
     * and hidden while read-only.
     *
     * @param editable true while the user may edit the content: neither bound nor read-only
     */
    protected void updateContentEditable(boolean editable) {
        if (content instanceof TextInputControl text) {
            text.setEditable(editable);
        }
    }

    /**
     * Hides the content's value while bound, or shows it again: the shell calls it when the content is set and
     * whenever the shell becomes or stops being bound (calls may repeat a value). By default an empty clip hides the
     * content, which stays visible and so keeps the focus, and a text input control's selection is cleared, so
     * nothing of the hidden value stays selected. A type whose content needs more overrides it, calling this one too.
     *
     * @param hidden true while bound
     */
    protected void updateContentHidden(boolean hidden) {
        if (content == null) {
            return;
        }
        if (hidden) {
            content.setClip(emptyClip);
            if (content instanceof TextInputControl text) {
                text.deselect();
            }
        } else if (content.getClip() == emptyClip) {
            content.setClip(null);
        }
    }

    // --- Drop and clipboard --------------------------------------------------------------------------

    /**
     * Shows or ends the drop-target look, the pseudo-class {@code :drop-target}, which the theme draws as the accent
     * border around content and icons. An input built on the shell shows it while a drag it accepts hovers over it,
     * and ends it when the drag leaves or drops; a refused drag shows nothing.
     *
     * @param accepted true while an acceptable drag hovers over the input
     */
    protected final void setDropTarget(boolean accepted) {
        pseudoClassStateChanged(DROP_TARGET_PSEUDO_CLASS, accepted);
    }

    /**
     * Replaces the clipboard the inputs on the shell paste from and copy to, the system clipboard by default.
     * Package-private: the tests' seam, so that no test touches the clipboard of the machine it runs on; it is no part
     * of the API.
     *
     * @param replacement the clipboard to use
     */
    void setClipboard(ShellClipboard replacement) {
        clipboard = replacement;
    }

    // The guarded clipboard of the inputs' text controls: each text control an input on the shell wraps (a frame's text
    // field, the password's two fields, the text area, the combos' read-only text) overrides its copy, cut, paste and
    // select-all with these, so they read and write the clipboard through the seam above and keep to one guard.

    /**
     * Pastes the clipboard's text, as the adaptation makes it, in place of a text control's selection: nothing while
     * the user may not change the value ({@link #userMayChangeValue()}), also when code calls the paste, and nothing
     * without text on the clipboard.
     *
     * @param target     the text control pasted into
     * @param adaptation what is inserted for the clipboard's text, such as the frame's {@code pastedText}
     */
    final void guardedPaste(TextInputControl target, UnaryOperator<String> adaptation) {
        if (!userMayChangeValue()) {
            return;
        }
        String pasted = clipboard.text();
        if (pasted != null) {
            target.replaceSelection(adaptation.apply(pasted));
        }
    }

    /**
     * Pastes the clipboard's text unchanged in place of a text control's selection, as
     * {@link #guardedPaste(TextInputControl, UnaryOperator)} does.
     *
     * @param target the text control pasted into
     */
    final void guardedPaste(TextInputControl target) {
        guardedPaste(target, UnaryOperator.identity());
    }

    /**
     * Copies a text control's selection to the clipboard: nothing while bound, when its text is the hidden value, and
     * nothing without a selection.
     *
     * @param source the text control copied from
     */
    final void guardedCopy(TextInputControl source) {
        if (binding.isBound()) {
            return;
        }
        String selected = source.getSelectedText();
        if (!selected.isEmpty()) {
            clipboard.put(selected);
        }
    }

    /**
     * Cuts a text control's selection as JavaFX's cut does, through the control's own copy, then deleting the
     * selection: nothing while the user may not change the value, also when code calls the cut.
     *
     * @param source the text control cut from
     */
    final void guardedCut(TextInputControl source) {
        if (!userMayChangeValue()) {
            return;
        }
        source.copy();
        IndexRange selection = source.getSelection();
        source.deleteText(selection.getStart(), selection.getEnd());
    }

    /**
     * Selects a text control's whole text as JavaFX's select-all does, or nothing while bound: nothing of the hidden
     * value is selected, so nothing of it can be copied.
     *
     * @param target the text control
     */
    final void guardedSelectAll(TextInputControl target) {
        if (binding.isBound()) {
            target.deselect();
            return;
        }
        target.selectRange(0, target.getLength());
    }

    // --- Children ------------------------------------------------------------------------------------

    /**
     * Puts the children in their order: the leading graphic, the content, the binding's chip over it, the trailing
     * icons, the type's own controls, then the binding's {@code {}} icon. Every child of the shell is placed here, so a
     * later part of the shell is added in this one place. While read-only the trailing icons are left out: they leave
     * the shell rather than being hidden, so their own visibility, management and disabling stay as the application set
     * them.
     */
    private void updateChildren() {
        List<Node> children = new ArrayList<>();
        if (leadingGraphic != null) {
            children.add(leadingGraphic);
        }
        if (content != null) {
            children.add(content);
        }
        children.add(bindingControls.chip());
        if (!readOnly.get()) {
            children.addAll(icons);
        }
        children.addAll(ownControls);
        children.add(bindingControls.bindIcon());
        getChildren().setAll(children);
    }

    // --- Binding -------------------------------------------------------------------------------------

    /**
     * Returns the shell's binding to a variable, one per shell; an input built on the shell returns it as its
     * {@link FormInput#getBinding()}. See the class description for how the shell shows it.
     *
     * @return the shell's binding, never null
     */
    public final FieldBinding getBinding() {
        return binding;
    }

    /**
     * Returns the binding list, the popup menu the {@code {}} icon and Ctrl+Space open under the icon. Its entries are
     * the shell's: it fills them from the {@linkplain #getBinding() binding} each time it opens the list (and again
     * when the candidates change while the list shows), so an application styles the list but does not add to it.
     * The list shows only while the shell is in a shown window; elsewhere opening it fills it without showing it.
     *
     * @return the binding list
     */
    public final ContextMenu getBindingMenu() {
        return bindingControls.menu();
    }

    /** Keeps the shell in step with the binding: its controls (the {} icon, the chip, the list and keys), its look. */
    private void buildBinding() {
        binding.bindableProperty().addListener((observable, oldValue, newValue) -> updateBindingState());
        binding.missingProperty().addListener((observable, oldValue, newValue) -> updateBindingState());
        binding.boundNameProperty().addListener((observable, oldValue, newValue) -> updateBindingState());
        // Disabled, also through a parent: a binding list that shows closes, as when the shell becomes read-only.
        disabledProperty().addListener((observable, wasDisabled, isDisabled) -> {
            if (isDisabled) {
                bindingControls.closeMenu();
            }
        });
        addEventFilter(KeyEvent.KEY_PRESSED, bindingControls::handleKeys);
        // While bound the hidden content takes no clicks: a press on the chip or beside it, which no control takes,
        // gives the focus target the focus, so Backspace and Ctrl+Space work after a click too.
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            Node target = getFocusTarget();
            if (target != this && !target.isFocused()) {
                target.requestFocus();
            }
        });
        updateBindingState();
    }

    /**
     * Shows the binding: {@code :bound}, the chip (red while its variable is missing) in place of the content, whose
     * value is hidden and kept as it is; then the {} icon, the content's editability and the chip's remove button,
     * which the binding decides together with read-only.
     */
    private void updateBindingState() {
        boolean bound = binding.isBound();
        pseudoClassStateChanged(BOUND_PSEUDO_CLASS, bound);
        bindingControls.update(readOnly.get());
        if (content != null) {
            updateContentHidden(bound);
            updateContentEditable(isEditable());
        }
    }

    // --- Layout --------------------------------------------------------------------------------------

    /**
     * Lays the children out on one line, the default for a single-line input: the leading graphic, if any, at the left
     * end and the trailing nodes at the right end, centred vertically in the content's height, the content between them
     * over the whole height, the chip over the content where its text starts. A type with another shape overrides it
     * and places the parts with the protected layout methods below.
     */
    @Override
    protected void layoutChildren() {
        double top = snappedTopInset();
        double contentHeight = Math.max(0, getHeight() - top - snappedBottomInset());
        double left = layoutLeading(top, contentHeight);
        double right = layoutTrailing(trailingNodes(), top, contentHeight);
        if (content != null) {
            content.resizeRelocate(left, top, Math.max(0, right - left), contentHeight);
        }
        layoutChip(left, top, right, contentHeight);
    }

    /**
     * Places the leading graphic, if any, at the shell's left edge inside its left inset, at its preferred width,
     * centred vertically in a band as a trailing node is: as high as its maximum height, at least its preferred height,
     * never higher than the band.
     *
     * @return the left edge of the content: right of the graphic, or the left inset without one
     */
    private double layoutLeading(double top, double height) {
        double left = snappedLeftInset();
        if (leadingGraphic == null) {
            return left;
        }
        double graphicWidth = snapSizeX(leadingGraphic.prefWidth(-1));
        double graphicHeight = Math.min(snapSizeY(Math.max(leadingGraphic.prefHeight(-1),
                leadingGraphic.maxHeight(-1))), height);
        leadingGraphic.resizeRelocate(left, top + snapPositionY((height - graphicHeight) / 2), graphicWidth,
                graphicHeight);
        return left + graphicWidth;
    }

    /** The width the leading graphic takes, its preferred width; 0 without one. */
    private double leadingWidth() {
        return leadingGraphic == null ? 0 : snapSizeX(leadingGraphic.prefWidth(-1));
    }

    /** The preferred height of the leading graphic; 0 without one. */
    private double leadingPrefHeight() {
        return leadingGraphic == null ? 0 : snapSizeY(leadingGraphic.prefHeight(-1));
    }

    /**
     * Returns the nodes at the shell's right end that take room, left to right: the managed trailing icons (none while
     * read-only), the managed own controls, then the {@code {}} icon while it shows. A layout of a type's own may
     * place one of them elsewhere: it takes it out of this new list before it lays the rest out.
     *
     * @return a new list of the trailing nodes
     */
    protected final List<Node> trailingNodes() {
        List<Node> trailing = new ArrayList<>();
        if (!readOnly.get()) {
            for (ButtonBase icon : icons) {
                if (icon.isManaged()) {
                    trailing.add(icon);
                }
            }
        }
        for (Node control : ownControls) {
            if (control.isManaged()) {
                trailing.add(control);
            }
        }
        if (bindingControls.bindIcon().isManaged()) {
            trailing.add(bindingControls.bindIcon());
        }
        return trailing;
    }

    /**
     * Lays trailing nodes out at the shell's right end, inside its right inset, in their order and 1px apart, each at
     * its preferred width, centred vertically in a band: each as high as its maximum height, at least its preferred
     * height, and never higher than the band, so an icon (whose maximum is its preferred height) keeps its height and
     * a divider (a region without a maximum) spans the band. The right inset is the room after the last node: without
     * nodes there is none, and the content reaches the shell's right edge, as while read-only.
     *
     * @param nodes  the nodes, left to right, usually {@link #trailingNodes()}
     * @param top    the band's top
     * @param height the band's height
     * @return the left edge of the first node, where the content ends; the shell's right edge without nodes
     */
    protected final double layoutTrailing(List<Node> nodes, double top, double height) {
        if (nodes.isEmpty()) {
            return getWidth();
        }
        double right = getWidth() - snappedRightInset();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            double nodeWidth = snapSizeX(node.prefWidth(-1));
            double nodeHeight = Math.min(snapSizeY(Math.max(node.prefHeight(-1), node.maxHeight(-1))), height);
            right -= nodeWidth;
            node.resizeRelocate(right, top + snapPositionY((height - nodeHeight) / 2), nodeWidth, nodeHeight);
            if (i > 0) {
                right -= ICON_GAP;
            }
        }
        return right;
    }

    /**
     * Returns the width trailing nodes take, from their preferred widths, with the gaps between them.
     *
     * @param nodes the nodes, usually {@link #trailingNodes()}
     * @return their width, 0 without nodes
     */
    protected final double trailingWidth(List<Node> nodes) {
        double width = 0;
        for (Node node : nodes) {
            width += snapSizeX(node.prefWidth(-1));
        }
        if (nodes.size() > 1) {
            width += ICON_GAP * (nodes.size() - 1);
        }
        return width;
    }

    /**
     * Returns the preferred height of the tallest trailing node.
     *
     * @param nodes the nodes, usually {@link #trailingNodes()}
     * @return the tallest preferred height, 0 without nodes
     */
    protected final double trailingPrefHeight(List<Node> nodes) {
        double height = 0;
        for (Node node : nodes) {
            height = Math.max(height, node.prefHeight(-1));
        }
        return snapSizeY(height);
    }

    /**
     * Places the chip, while bound, over the content where its text starts ({@link #chipInset()} right of the
     * content's left edge), centred vertically in a band: never higher than the band, never wider than the room left
     * of {@code right} (the name is then shortened). Does nothing while unbound.
     *
     * @param left   the content's left edge
     * @param top    the band's top
     * @param right  the right end of the room, usually the left edge of the trailing nodes
     * @param height the band's height
     */
    protected final void layoutChip(double left, double top, double right, double height) {
        Label chip = bindingControls.chip();
        if (!chip.isManaged()) {
            return;
        }
        double chipLeft = left + chipInset();
        double room = Math.max(0, right - chipLeft - ICON_GAP);
        double chipWidth = Math.min(snapSizeX(chip.prefWidth(-1)), room);
        double chipHeight = Math.min(snapSizeY(chip.prefHeight(-1)), height);
        chip.resizeRelocate(chipLeft, top + snapPositionY((height - chipHeight) / 2), chipWidth, chipHeight);
    }

    /**
     * Returns how far right of the content's left edge its text starts, where the chip goes while bound: by default
     * the content's left padding when it is a region (a text field's), else 0. A type whose content shows its text
     * further in overrides it.
     *
     * @return the distance from the content's left edge to its text
     */
    protected double chipInset() {
        if (content instanceof Region region) {
            return snapSpaceX(region.getPadding().getLeft());
        }
        return 0;
    }

    @Override
    protected double computePrefWidth(double height) {
        double contentWidth = content == null ? 0 : snapSizeX(content.prefWidth(-1));
        return snappedLeftInset() + leadingWidth() + contentWidth + trailingRoom(trailingNodes());
    }

    @Override
    protected double computeMinWidth(double height) {
        double contentWidth = content == null ? 0 : snapSizeX(content.minWidth(-1));
        return snappedLeftInset() + leadingWidth() + contentWidth + trailingRoom(trailingNodes());
    }

    /**
     * The width the trailing nodes take with the room after the last one, the right inset; none without nodes, as
     * {@link #layoutTrailing(List, double, double)} lays them out.
     */
    private double trailingRoom(List<Node> nodes) {
        if (nodes.isEmpty()) {
            return 0;
        }
        return trailingWidth(nodes) + snappedRightInset();
    }

    /**
     * Returns the shell's preferred height on one line: its content's, its leading graphic's or its tallest trailing
     * node's, with its insets.
     *
     * @param width the width, ignored
     * @return the preferred height
     */
    @Override
    protected double computePrefHeight(double width) {
        double contentHeight = content == null ? 0 : content.prefHeight(-1);
        double height = Math.max(snapSizeY(contentHeight), trailingPrefHeight(trailingNodes()));
        height = Math.max(height, leadingPrefHeight());
        return snappedTopInset() + height + snappedBottomInset();
    }

    // On one line, as a text field: never lower and never higher than its preferred height.

    /**
     * Returns the shell's preferred height: on one line it is never lower.
     *
     * @param width the width, ignored
     * @return the preferred height
     */
    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    /**
     * Returns the shell's preferred height: on one line it is never higher.
     *
     * @param width the width, ignored
     * @return the preferred height
     */
    @Override
    protected double computeMaxHeight(double width) {
        return computePrefHeight(width);
    }
}
