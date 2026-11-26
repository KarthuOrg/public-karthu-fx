package org.karthu.fx.sidebar;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.karthu.fx.command.Command;
import org.karthu.fx.command.CommandGroup;
import org.karthu.fx.command.CommandRegistry;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A side bar for the left or right edge of a window, like the tool window bar of an IDE: a narrow stripe with one
 * button per {@linkplain SideBarTab tab} and, while it is expanded, the open tab's panel between the stripe and the
 * application's content. The application places it beside its content like any node, for example in the left region
 * of a border pane:
 * <pre>{@code
 * NavigationTree<Page> pages = new NavigationTree<>(root);
 * SideBarTab pagesTab = new SideBarTab("pages", "Pages", BootstrapIcons.BOOK, pages);
 * SideBar sideBar = new SideBar();
 * sideBar.getTabs().add(pagesTab);
 * sideBar.setSelectedTab(pagesTab);
 * window.setLeft(sideBar);
 * }</pre>
 *
 * <p><b>Tabs.</b> {@link #getTabs()} holds the tabs in the order of their stripe buttons; the application adds,
 * removes and hides tabs at any time, the user does not reorder them. Each visible tab has a stripe button that shows
 * its icon, its title as tooltip and as accessible text, so that a screen reader names the icon; once the side bar's
 * commands are registered, the tooltip names the key of the tab's command too, such as <i>Files (Alt+1)</i>. The
 * stripe is no stop of the Tab key, and a click on it leaves the focus where opening a tab puts it.
 *
 * <p><b>Open tab and collapsing.</b> The {@linkplain #selectedTabProperty() selected tab} is the one the panel shows,
 * and the {@linkplain #collapsedProperty() collapsed} flag hides the panel, so that only the stripe shows. At most one
 * tab is open: the selected tab, while the side bar is not collapsed and the tab is one of its visible tabs, and only
 * its stripe button is marked. A click on the button of another tab selects it and expands the side bar; a click on
 * the open tab's button collapses it, and while collapsed expands it again. The hide button in the panel's header and
 * the menu's <i>Hide</i> collapse it too. Collapsing keeps the selected tab, so expanding shows it again. When the
 * selected tab is removed or hidden, the side bar collapses and selects no tab. Everything happens at once, without
 * animation, and the panel always lies beside the content, which becomes narrower, never under the panel.
 *
 * <p><b>Width.</b> The panel has one {@linkplain #contentWidthProperty() content width} for every tab, 260 px by
 * default, so that switching tabs never moves the application's content. The user drags the resize edge on the
 * panel's inner side, the one towards the content, to make the panel wider or narrower, within the
 * {@linkplain #minContentWidthProperty() smallest} (160 px by default) and the
 * {@linkplain #maxContentWidthProperty() largest} width (600 px by default). A drag below the smallest width stops
 * there and never collapses the side bar.
 *
 * <p><b>Focus.</b> Opening a tab by a click on its stripe button or by its command puts the keyboard focus into its
 * content, where the user starts: a content that implements {@link SideBarContent} takes it as it says (a
 * {@link NavigationTree} in its tree, on the selected entry), any other content node takes it itself. A tab the
 * application selects moves no focus. When the panel hides while the focus is in it, however it hides, the focus goes
 * back to where it last was outside the side bar, such as the application's content the user worked in before, while
 * that node is still in the scene, shown and enabled.
 *
 * <p><b>Commands.</b> An application that has a {@link CommandRegistry} hands it to the side bar with
 * {@link #registerCommands(CommandRegistry, CommandGroup, String) registerCommands}: then a command per tab opens the
 * tab (Alt+1 to Alt+9 for the first nine), <i>Hide Side Bar</i> collapses the side bar on Shift+Escape while the focus
 * is in it, and <i>Search in Side Bar</i> puts the focus into the selected tab's search; the commands join the
 * application's list of shortcuts, where the user changes their keys, and the hide button's tooltip names the key of
 * <i>Hide Side Bar</i>, such as <i>Hide (Shift+Escape)</i>. Without a registry there are no commands.
 *
 * <p><b>Header.</b> The panel's header is one row: the tab's title on the left, then on the right the tab's own
 * {@linkplain SideBarTab#getHeaderButtons() header buttons}, the content's {@linkplain SideBarContent#getHeaderButtons()
 * header buttons} (Expand All and Collapse All for a navigation tree), the menu button and the hide button, without
 * separators. The content fills the rest of the panel below it.
 *
 * <p><b>Menu.</b> The menu button opens the open tab's {@linkplain #getMenu() menu}, and so does a right click on the
 * header; a right click on a stripe button opens the menu of that button's tab, without opening the tab. A tab's menu
 * lists the tab's own {@linkplain SideBarTab#getMenuItems() items}, then the content's
 * {@linkplain SideBarContent#getMenuItems() items}, then, below a separator, the built-in <i>Hide</i>, which is
 * disabled in the menu of a tab that is not open. With no items above it, <i>Hide</i> stands alone.
 *
 * <p><b>Side.</b> The {@linkplain #sideProperty() side} is {@link Side#LEFT} (the default), with the stripe at the
 * window's left edge and the panel to its right, or {@link Side#RIGHT}, mirrored. Two side bars, one on each side, are
 * two instances.
 *
 * <p><b>State.</b> The side bar stores nothing. An application that brings the layout back after a restart stores the
 * content width, the selected tab's {@linkplain SideBarTab#getId() id} and the collapsed flag from the properties, and
 * sets them again on the next start, the limits of the width before the width; a tab it selects before adding it
 * shows once it is added.
 *
 * <p><b>Texts.</b> The tooltips and accessible texts of the menu button ({@linkplain #moreTextProperty() More}) and
 * the hide button, which is also the text of the menu's item ({@linkplain #hideTextProperty() Hide}), are properties
 * with English defaults, so that an application can translate them, and so are the names of the commands and their
 * context (see {@link #registerCommands(CommandRegistry, CommandGroup, String, boolean)}).
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change. The theme draws the stripe on the
 * surface with a 1 px line towards the panel, its buttons flat and square with the icon 16 px in the secondary text
 * colour, the open tab's in the accent colour on the raised grey of a button; the panel on the surface, its resize
 * edge continuing it with a 1 px line towards the content, the header's title in regular weight and its icon buttons
 * 14 px in the secondary text colour.
 * <ul>
 *   <li>{@code side-bar}: the side bar, an {@link HBox}, with the pseudo-class {@code :left} or {@code :right} of its
 *       side</li>
 *   <li>{@code side-bar-stripe}: the stripe, a {@link VBox}</li>
 *   <li>{@code side-bar-tab}: a tab's stripe button, a {@link ToggleButton}, {@code :selected} while its tab is
 *       open</li>
 *   <li>{@code side-bar-panel}: the panel, a {@link VBox} of the header and the content</li>
 *   <li>{@code side-bar-header}: the header row, an {@link HBox}</li>
 *   <li>{@code side-bar-title}: the title, a {@link Label}</li>
 *   <li>{@code side-bar-menu-button}, {@code side-bar-hide-button}: the menu button and the hide button,
 *       {@link Button}s beside {@code icon-button}, with the Bootstrap icons {@code THREE_DOTS_VERTICAL} and
 *       {@code DASH}</li>
 *   <li>{@code side-bar-content}: the holder of the open tab's content, a {@link StackPane}</li>
 *   <li>{@code side-bar-resize-edge}: the resize edge on the panel's inner side, a {@link Region} a few pixels wide
 *       with the horizontal resize cursor, shown with the panel</li>
 *   <li>{@code side-bar-menu}: the menu, a {@link ContextMenu}, and {@code side-bar-hide-item}: its <i>Hide</i></li>
 * </ul>
 * A header button of the application's own, styled as an {@code icon-button}, takes the look of the header's.
 *
 * <p>Like every karthu-fx control, the side bar is used on the JavaFX Application Thread only.
 *
 * @since 1.1.0-RC2
 */
public class SideBar extends HBox {

    /** The content width until the application sets one. */
    private static final double DEFAULT_CONTENT_WIDTH = 260;
    /** The smallest and the largest content width until the application sets its own. */
    private static final double DEFAULT_MIN_CONTENT_WIDTH = 160;
    private static final double DEFAULT_MAX_CONTENT_WIDTH = 600;
    private static final PseudoClass LEFT_PSEUDO_CLASS = PseudoClass.getPseudoClass("left");
    private static final PseudoClass RIGHT_PSEUDO_CLASS = PseudoClass.getPseudoClass("right");

    private final ObservableList<SideBarTab> tabs = FXCollections.observableArrayList();
    // LEFT or RIGHT: set refuses any other, so a refused side never reaches the listeners. A line comment, as the
    // property's documentation is its method's.
    private final ObjectProperty<Side> side = new SimpleObjectProperty<>(this, "side", Side.LEFT) {
        @Override
        public void set(Side newSide) {
            if (newSide != Side.LEFT && newSide != Side.RIGHT) {
                throw new IllegalArgumentException("a side bar sits on the left or the right, not " + newSide);
            }
            super.set(newSide);
        }
    };
    private final ObjectProperty<SideBarTab> selectedTab = new SimpleObjectProperty<>(this, "selectedTab");
    private final BooleanProperty collapsed = new SimpleBooleanProperty(this, "collapsed", false);
    private final StringProperty moreText = new SimpleStringProperty(this, "moreText", "More");
    private final StringProperty hideText = new SimpleStringProperty(this, "hideText", "Hide");
    // The limits of the content width, each a WidthLimit, which refuses what is no width and keeps the content width
    // within the limits. Line comments here, as the properties' documentation is their methods'.
    private final DoubleProperty minContentWidth = new WidthLimit("minContentWidth", DEFAULT_MIN_CONTENT_WIDTH);
    private final DoubleProperty maxContentWidth = new WidthLimit("maxContentWidth", DEFAULT_MAX_CONTENT_WIDTH);
    // The width of the panel: set refuses NaN and puts any other width within the limits, so that the application
    // reads back the width the panel shows.
    private final DoubleProperty contentWidth = new SimpleDoubleProperty(this, "contentWidth", DEFAULT_CONTENT_WIDTH) {
        @Override
        public void set(double width) {
            if (Double.isNaN(width)) {
                throw new IllegalArgumentException("a content width is a number of pixels, not NaN");
            }
            super.set(withinLimits(width));
        }
    };
    /** The width the panel shows: the content width within the limits, also where a binding sets it. */
    private final DoubleBinding shownContentWidth = Bindings.createDoubleBinding(
            () -> withinLimits(contentWidth.get()), contentWidth, minContentWidth, maxContentWidth);
    /** The resize edge on the panel's inner side, between the panel and the application's content. */
    private final ResizeEdge resizeEdge = new ResizeEdge(contentWidth, side);

    private final VBox stripe = new VBox();
    private final Label titleLabel = new Label();
    private final Button menuButton = HeaderButtons.iconButton("side-bar-menu-button",
            BootstrapIcons.THREE_DOTS_VERTICAL, moreText, moreText, this::menuButtonFired);
    /** The display text of the current key of Hide Side Bar; empty while there is none or no commands. */
    private final StringProperty hideShortcutText = new SimpleStringProperty(this, "hideShortcutText", "");
    /** The hide button: its tooltip names the key of Hide Side Bar, as a stripe button's names its tab's. */
    private final Button hideButton = HeaderButtons.iconButton("side-bar-hide-button", BootstrapIcons.DASH, hideText,
            Bindings.createStringBinding(() -> tooltipText(hideText.get(), hideShortcutText.get()), hideText,
                    hideShortcutText),
            () -> collapsed.set(true));
    /** The header row: the title, the tab's buttons, the content's buttons, More and Hide. */
    private final HBox header = new HBox();
    /** The built-in last item of every tab's menu, which collapses the side bar. */
    private final MenuItem hideItem = new MenuItem();
    /** The menu, created when first needed. */
    private ContextMenu menu;
    /** Holds the open tab's content, below the header. */
    private final StackPane contentHolder = new StackPane();
    /** The open tab's header and content, beside the stripe. */
    private final VBox panel = new VBox(header, contentHolder);
    /** The stripe button of each tab among the tabs, by identity. */
    private final Map<SideBarTab, StripeButton> stripeButtons = new IdentityHashMap<>();
    /** The tabs whose visible flag the side bar listens to: the tabs of the list. */
    private final Set<SideBarTab> watchedTabs = Collections.newSetFromMap(new IdentityHashMap<>());
    private final InvalidationListener tabVisibilityChanged = this::tabVisibilityChanged;
    /** Held weakly by the tabs, so that a tab outliving the side bar never keeps it alive. */
    private final WeakInvalidationListener weakTabVisibilityChanged =
            new WeakInvalidationListener(tabVisibilityChanged);
    /**
     * The tab the panel is built for, whose title, buttons and content the panel holds, whether it shows or not: the
     * selected tab while it is among the tabs. The side bar listens to its buttons and its content.
     */
    private SideBarTab panelOwner;
    private final InvalidationListener panelOwnerChanged = observable -> updatePanel();
    /** Held weakly by the panel's owner, so that a tab outliving the side bar never keeps it alive. */
    private final WeakInvalidationListener weakPanelOwnerChanged = new WeakInvalidationListener(panelOwnerChanged);

    // The names of the commands and their context. Line comments, as each property's documentation is its method's.
    private final StringProperty showTabCommandFormat =
            new SimpleStringProperty(this, "showTabCommandFormat", "Show {0}");
    private final StringProperty hideCommandName = new SimpleStringProperty(this, "hideCommandName", "Hide Side Bar");
    private final StringProperty searchCommandName =
            new SimpleStringProperty(this, "searchCommandName", "Search in Side Bar");
    private final StringProperty commandContextName = new SimpleStringProperty(this, "commandContextName", "Side Bar");
    /** The side bar's commands in the application's registry; null until they are registered. */
    private SideBarCommands commands;
    /** Where the focus goes back to when the panel hides with the focus in it. */
    private final FocusReturn focusReturn = new FocusReturn(this);

    /**
     * Creates a side bar on the left without tabs: it shows an empty stripe until the application adds tabs, and the
     * panel once a tab is selected.
     *
     * @since 1.1.0-RC2
     */
    public SideBar() {
        getStyleClass().add("side-bar");
        stripe.getStyleClass().add("side-bar-stripe");
        panel.getStyleClass().add("side-bar-panel");
        panel.minWidthProperty().bind(shownContentWidth);
        panel.prefWidthProperty().bind(shownContentWidth);
        panel.maxWidthProperty().bind(shownContentWidth);
        header.getStyleClass().add("side-bar-header");
        header.setAlignment(Pos.CENTER_LEFT);
        titleLabel.getStyleClass().add("side-bar-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        header.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::headerMenuRequested);
        hideItem.getStyleClass().add("side-bar-hide-item");
        hideItem.textProperty().bind(hideText);
        hideItem.setOnAction(event -> collapsed.set(true));
        contentHolder.getStyleClass().add("side-bar-content");
        VBox.setVgrow(contentHolder, Priority.ALWAYS);
        arrange();
        side.addListener(observable -> arrange());
        tabs.addListener(this::tabsChanged);
        selectedTab.addListener(observable -> updatePanel());
        collapsed.addListener(observable -> updatePanel());
        updatePanel();
    }

    /**
     * Returns the tabs, in the order of their stripe buttons. A change shows at once; removing the selected tab
     * collapses the side bar and selects no tab. A tab belongs to one side bar and appears at most once.
     *
     * @return the live list of the tabs
     * @since 1.1.0-RC2
     */
    public final ObservableList<SideBarTab> getTabs() {
        return tabs;
    }

    /**
     * The side of the window the side bar sits on, {@link Side#LEFT} (the default) or {@link Side#RIGHT}: on the left
     * the stripe lies at the side bar's left edge and the panel to its right, on the right mirrored. The application
     * places the side bar on that side of its content. Setting another side, or null, is refused with an
     * {@link IllegalArgumentException}; a binding to a value that yields another side counts as {@link Side#LEFT}.
     *
     * @return the side property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<Side> sideProperty() {
        return side;
    }

    /**
     * Returns the side of the window the side bar sits on.
     *
     * @return {@link Side#LEFT} or {@link Side#RIGHT}
     * @since 1.1.0-RC2
     */
    public final Side getSide() {
        return side.get();
    }

    /**
     * Sets the side of the window the side bar sits on.
     *
     * @param side {@link Side#LEFT} or {@link Side#RIGHT}
     * @throws IllegalArgumentException when the side is {@link Side#TOP}, {@link Side#BOTTOM} or null
     * @since 1.1.0-RC2
     */
    public final void setSide(Side side) {
        this.side.set(side);
    }

    /**
     * The selected tab, the one the panel shows while the side bar is expanded, null for none (the default). A click
     * on another tab's stripe button selects it; collapsing keeps it; removing or hiding it selects no tab. A tab that
     * is not one of the side bar's visible tabs shows no panel, until it is. The application reads it to store the
     * open tab's id, and sets it to bring the tab back; selecting moves no focus.
     *
     * @return the selected tab property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<SideBarTab> selectedTabProperty() {
        return selectedTab;
    }

    /**
     * Returns the selected tab.
     *
     * @return the selected tab, null for none
     * @since 1.1.0-RC2
     */
    public final SideBarTab getSelectedTab() {
        return selectedTab.get();
    }

    /**
     * Sets the selected tab.
     *
     * @param tab the tab, null for none
     * @since 1.1.0-RC2
     */
    public final void setSelectedTab(SideBarTab tab) {
        selectedTab.set(tab);
    }

    /**
     * Whether the side bar is collapsed to its stripe (default false): collapsed, the panel is hidden, the content
     * beside the side bar gets its room, and no stripe button is marked. A click on the open tab's stripe button, the
     * hide button and the menu's <i>Hide</i> collapse the side bar, a click on a stripe button expands it; removing
     * or hiding the selected tab collapses it too. Expanded without a selected tab, only the stripe shows.
     *
     * @return the collapsed property
     * @since 1.1.0-RC2
     */
    public final BooleanProperty collapsedProperty() {
        return collapsed;
    }

    /**
     * Returns whether the side bar is collapsed to its stripe.
     *
     * @return true while collapsed
     * @since 1.1.0-RC2
     */
    public final boolean isCollapsed() {
        return collapsed.get();
    }

    /**
     * Sets whether the side bar is collapsed to its stripe.
     *
     * @param collapsed true to collapse
     * @since 1.1.0-RC2
     */
    public final void setCollapsed(boolean collapsed) {
        this.collapsed.set(collapsed);
    }

    /**
     * The width of the panel in pixels, one for every tab, so that switching tabs never moves the application's
     * content (default 260 px). A drag of the resize edge sets it in whole pixels. It stays within the
     * {@linkplain #minContentWidthProperty() smallest} and the {@linkplain #maxContentWidthProperty() largest} width:
     * a width set beyond them is set to the nearer one, and a change of the limits moves the width within them, so the
     * application reads back the width the panel shows. Setting NaN is refused with an
     * {@link IllegalArgumentException}. A bound width is the binding's: the panel shows it within the limits, and the
     * resize edge does not move it.
     *
     * @return the content width property
     * @since 1.1.0-RC2
     */
    public final DoubleProperty contentWidthProperty() {
        return contentWidth;
    }

    /**
     * Returns the width of the panel.
     *
     * @return the content width in pixels
     * @since 1.1.0-RC2
     */
    public final double getContentWidth() {
        return contentWidth.get();
    }

    /**
     * Sets the width of the panel; a width beyond the limits is set to the nearer limit.
     *
     * @param width the content width in pixels
     * @throws IllegalArgumentException when the width is NaN
     * @since 1.1.0-RC2
     */
    public final void setContentWidth(double width) {
        contentWidth.set(width);
    }

    /**
     * The smallest width of the panel in pixels (default 160 px): a drag of the resize edge stops there and never
     * collapses the side bar. Where it is larger than the {@linkplain #maxContentWidthProperty() largest} width, it
     * wins, as a node's minimum width does in a JavaFX layout. Setting a value that is no finite number of pixels,
     * zero or more, is refused with an {@link IllegalArgumentException}.
     *
     * @return the smallest content width property
     * @since 1.1.0-RC2
     */
    public final DoubleProperty minContentWidthProperty() {
        return minContentWidth;
    }

    /**
     * Returns the smallest width of the panel.
     *
     * @return the smallest content width in pixels
     * @since 1.1.0-RC2
     */
    public final double getMinContentWidth() {
        return minContentWidth.get();
    }

    /**
     * Sets the smallest width of the panel; a narrower content width becomes this width.
     *
     * @param width the smallest content width in pixels
     * @throws IllegalArgumentException when the width is negative, infinite or NaN
     * @since 1.1.0-RC2
     */
    public final void setMinContentWidth(double width) {
        minContentWidth.set(width);
    }

    /**
     * The largest width of the panel in pixels (default 600 px): a drag of the resize edge stops there. Setting a
     * value that is no finite number of pixels, zero or more, is refused with an {@link IllegalArgumentException}.
     *
     * @return the largest content width property
     * @since 1.1.0-RC2
     */
    public final DoubleProperty maxContentWidthProperty() {
        return maxContentWidth;
    }

    /**
     * Returns the largest width of the panel.
     *
     * @return the largest content width in pixels
     * @since 1.1.0-RC2
     */
    public final double getMaxContentWidth() {
        return maxContentWidth.get();
    }

    /**
     * Sets the largest width of the panel; a wider content width becomes this width.
     *
     * @param width the largest content width in pixels
     * @throws IllegalArgumentException when the width is negative, infinite or NaN
     * @since 1.1.0-RC2
     */
    public final void setMaxContentWidth(double width) {
        maxContentWidth.set(width);
    }

    /**
     * The tooltip and accessible text of the menu button (default <i>More</i>), a property so that an application can
     * translate it.
     *
     * @return the more text property
     * @since 1.1.0-RC2
     */
    public final StringProperty moreTextProperty() {
        return moreText;
    }

    /**
     * Returns the tooltip and accessible text of the menu button.
     *
     * @return the more text
     * @since 1.1.0-RC2
     */
    public final String getMoreText() {
        return moreText.get();
    }

    /**
     * Sets the tooltip and accessible text of the menu button.
     *
     * @param text the more text
     * @since 1.1.0-RC2
     */
    public final void setMoreText(String text) {
        moreText.set(text);
    }

    /**
     * The tooltip and accessible text of the hide button and the text of the menu's built-in item (default
     * <i>Hide</i>), a property so that an application can translate it. <i>Hide</i> rather than <i>Collapse</i>, so
     * that it is not taken for a content's Collapse All. Once the side bar's commands are registered, the hide button's
     * tooltip adds the key of <i>Hide Side Bar</i> in brackets, such as <i>Hide (Shift+Escape)</i>.
     *
     * @return the hide text property
     * @since 1.1.0-RC2
     */
    public final StringProperty hideTextProperty() {
        return hideText;
    }

    /**
     * Returns the tooltip and accessible text of the hide button and the text of the menu's built-in item.
     *
     * @return the hide text
     * @since 1.1.0-RC2
     */
    public final String getHideText() {
        return hideText.get();
    }

    /**
     * Sets the tooltip and accessible text of the hide button and the text of the menu's built-in item.
     *
     * @param text the hide text
     * @since 1.1.0-RC2
     */
    public final void setHideText(String text) {
        hideText.set(text);
    }

    /**
     * Returns the menu of the side bar's tabs, the popup menu that the menu button opens under itself and a right
     * click on the header or on a stripe button opens at the pointer. Its items are the side bar's: it fills them for
     * the tab each time it opens (the tab's items, the content's, a separator and <i>Hide</i>), so an application
     * styles the menu but adds its items to the tab or the content instead. The menu shows only while the side bar is
     * in a shown window; elsewhere opening it fills it without showing it.
     *
     * @return the menu, created when first needed
     * @since 1.1.0-RC2
     */
    public final ContextMenu getMenu() {
        if (menu == null) {
            menu = new ContextMenu();
            menu.getStyleClass().add("side-bar-menu");
        }
        return menu;
    }

    // --- Commands ------------------------------------------------------------------------------------

    /**
     * Registers the side bar's commands with the application's registry, with their suggested keys: Alt+1 to Alt+9
     * for the tabs and Shift+Escape for <i>Hide Side Bar</i>. See
     * {@link #registerCommands(CommandRegistry, CommandGroup, String, boolean)}.
     *
     * @param registry the command registry of the side bar's window
     * @param group    the group the commands are shown in, one of the registry's, such as <i>View</i>
     * @param idPrefix the start of the commands' ids, such as {@code sidebar}
     * @throws NullPointerException     when the registry, the group or the prefix is null
     * @throws IllegalArgumentException when the prefix is blank, the group is not one of the registry's, or the
     *                                  registry has a context or a command with one of the side bar's ids already, or
     *                                  a context of the side bar's node
     * @throws IllegalStateException    when the side bar's commands are registered already
     * @since 1.1.0-RC2
     */
    public final void registerCommands(CommandRegistry registry, CommandGroup group, String idPrefix) {
        registerCommands(registry, group, idPrefix, true);
    }

    /**
     * Registers the side bar's commands with the application's registry, so that the user opens its tabs with keys,
     * finds the commands in the application's list of shortcuts and changes their keys there. The ids start with the
     * prefix, which keeps two side bars apart:
     * <ul>
     *   <li><i>Show</i> and the tab's title, {@code <prefix>.show.<tab id>}, one per tab id, in the window's
     *       {@linkplain CommandRegistry#getWindowContext() context}: it selects the tab, expands the side bar and puts
     *       the focus into the tab's content, as a click on the tab's stripe button does, and collapses the side bar
     *       when the tab is open already. The first nine tabs suggest the keys Alt+1 to Alt+9 in the order of the
     *       tabs, a hidden tab keeping its place. A tab added later gets its command when it is added, with the first
     *       of Alt+1 to Alt+9 that no other tab of the side bar suggests. As the registry removes no command, a tab's
     *       command stays when the tab is removed or hidden, disabled, so that its key goes on to the window's other
     *       commands, until a visible tab with that id is among the tabs again.</li>
     *   <li><i>Hide Side Bar</i>, {@code <prefix>.hide}, suggesting Shift+Escape, in a context of the side bar's own
     *       node with the id {@code <prefix>}, named <i>Side Bar</i>: while the focus is in the side bar, its key
     *       collapses it.</li>
     *   <li><i>Search in Side Bar</i>, {@code <prefix>.search}, without a key: it expands the side bar and puts the
     *       focus into the selected tab's search ({@link SideBarContent#focusSearch()}), or into its content when the
     *       content has no search. It selects no tab, and is disabled while no visible tab is selected.</li>
     * </ul>
     * <p>Without suggested keys the tabs' commands come without keys, for a second side bar whose Alt+1 to Alt+9 would
     * clash with the first's: the user records keys for them. <i>Hide Side Bar</i> keeps Shift+Escape either way: it
     * holds in the side bar's own context, so it clashes with no other side bar's, and a user could not record it, as
     * it has neither Ctrl nor Alt. The registry's rules hold as for any command: one registry per window, a suggested
     * key that another command of the same context suggests too is logged, and the keys the user changed come back
     * through {@link CommandRegistry#setChangedShortcuts(java.util.Map)}, after the side bar's commands are registered.
     * A stripe button's tooltip names its tab's key, such as <i>Files (Alt+1)</i>, and the hide button's the key of
     * <i>Hide Side Bar</i>, such as <i>Hide (Shift+Escape)</i>, both following every change of the key.
     *
     * <p>The commands' names follow the side bar's texts {@linkplain #showTabCommandFormatProperty() Show {0}},
     * {@linkplain #hideCommandNameProperty() Hide Side Bar} and {@linkplain #searchCommandNameProperty() Search in Side
     * Bar}, and the context's its {@linkplain #commandContextNameProperty() Side Bar}: an application translates those,
     * as the names are bound to them. A side bar registers its commands once.
     *
     * @param registry      the command registry of the side bar's window
     * @param group         the group the commands are shown in, one of the registry's, such as <i>View</i>
     * @param idPrefix      the start of the commands' ids, such as {@code sidebar}
     * @param suggestedKeys whether the tabs' commands come with their suggested keys; false leaves them without keys,
     *                      while <i>Hide Side Bar</i> keeps Shift+Escape
     * @throws NullPointerException     when the registry, the group or the prefix is null
     * @throws IllegalArgumentException when the prefix is blank, the group is not one of the registry's, or the
     *                                  registry has a context or a command with one of the side bar's ids already, or
     *                                  a context of the side bar's node; nothing is registered then
     * @throws IllegalStateException    when the side bar's commands are registered already
     * @since 1.1.0-RC2
     */
    public final void registerCommands(CommandRegistry registry, CommandGroup group, String idPrefix,
                                       boolean suggestedKeys) {
        if (commands != null) {
            throw new IllegalStateException("the side bar's commands are registered already");
        }
        commands = new SideBarCommands(this, registry, group, idPrefix, suggestedKeys);
        hideShortcutText.bind(commands.hideCommand().shortcutDisplayTextProperty());
        updateCommands();
    }

    /**
     * The name of a tab's command (default <i>Show {0}</i>), a format in which {@code {0}} stands for the tab's title,
     * or its id while it has none, so that an application can translate it: <i>{0} anzeigen</i>.
     *
     * @return the show tab command format property
     * @since 1.1.0-RC2
     */
    public final StringProperty showTabCommandFormatProperty() {
        return showTabCommandFormat;
    }

    /**
     * Returns the format of the name of a tab's command.
     *
     * @return the format, {@code {0}} standing for the tab's title
     * @since 1.1.0-RC2
     */
    public final String getShowTabCommandFormat() {
        return showTabCommandFormat.get();
    }

    /**
     * Sets the format of the name of a tab's command.
     *
     * @param format the format, {@code {0}} standing for the tab's title
     * @since 1.1.0-RC2
     */
    public final void setShowTabCommandFormat(String format) {
        showTabCommandFormat.set(format);
    }

    /**
     * The name of the command that collapses the side bar (default <i>Hide Side Bar</i>), a property so that an
     * application can translate it, or tell a second side bar's command apart.
     *
     * @return the hide command name property
     * @since 1.1.0-RC2
     */
    public final StringProperty hideCommandNameProperty() {
        return hideCommandName;
    }

    /**
     * Returns the name of the command that collapses the side bar.
     *
     * @return the name
     * @since 1.1.0-RC2
     */
    public final String getHideCommandName() {
        return hideCommandName.get();
    }

    /**
     * Sets the name of the command that collapses the side bar.
     *
     * @param name the name
     * @since 1.1.0-RC2
     */
    public final void setHideCommandName(String name) {
        hideCommandName.set(name);
    }

    /**
     * The name of the command that puts the focus into the selected tab's search (default <i>Search in Side Bar</i>),
     * a property so that an application can translate it, or tell a second side bar's command apart.
     *
     * @return the search command name property
     * @since 1.1.0-RC2
     */
    public final StringProperty searchCommandNameProperty() {
        return searchCommandName;
    }

    /**
     * Returns the name of the command that puts the focus into the selected tab's search.
     *
     * @return the name
     * @since 1.1.0-RC2
     */
    public final String getSearchCommandName() {
        return searchCommandName.get();
    }

    /**
     * Sets the name of the command that puts the focus into the selected tab's search.
     *
     * @param name the name
     * @since 1.1.0-RC2
     */
    public final void setSearchCommandName(String name) {
        searchCommandName.set(name);
    }

    /**
     * The name of the side bar's command context (default <i>Side Bar</i>), which the list of shortcuts shows beside
     * <i>Hide Side Bar</i> to tell where its key holds; a property so that an application can translate it, or tell a
     * second side bar apart.
     *
     * @return the command context name property
     * @since 1.1.0-RC2
     */
    public final StringProperty commandContextNameProperty() {
        return commandContextName;
    }

    /**
     * Returns the name of the side bar's command context.
     *
     * @return the name
     * @since 1.1.0-RC2
     */
    public final String getCommandContextName() {
        return commandContextName.get();
    }

    /**
     * Sets the name of the side bar's command context.
     *
     * @param name the name
     * @since 1.1.0-RC2
     */
    public final void setCommandContextName(String name) {
        commandContextName.set(name);
    }

    // --- Inside --------------------------------------------------------------------------------------

    /**
     * Places the stripe at the window's edge and the panel between it and the application's content: on the left
     * stripe then panel, on the right mirrored, so the panel's inner side always faces the content. A bound side
     * other than RIGHT counts as LEFT.
     */
    private void arrange() {
        boolean right = side.get() == Side.RIGHT;
        List<Node> parts = right ? List.of(resizeEdge, panel, stripe) : List.of(stripe, panel, resizeEdge);
        setChildren(getChildren(), parts);
        pseudoClassStateChanged(LEFT_PSEUDO_CLASS, !right);
        pseudoClassStateChanged(RIGHT_PSEUDO_CLASS, right);
    }

    /**
     * The list of tabs changed: the side bar listens to the tabs now in it, and to no other. When the selected tab
     * left the list, the side bar collapses and selects no tab.
     */
    private void tabsChanged(ListChangeListener.Change<? extends SideBarTab> change) {
        SideBarTab selected = selectedTab.get();
        boolean selectedRemoved = false;
        while (change.next()) {
            if (change.wasRemoved() && change.getRemoved().contains(selected)) {
                selectedRemoved = true;
            }
        }
        if (selectedRemoved && !tabs.contains(selected)) {
            deselectAndCollapse();
        }
        Set<SideBarTab> current = Collections.newSetFromMap(new IdentityHashMap<>());
        current.addAll(tabs);
        for (SideBarTab tab : new ArrayList<>(watchedTabs)) {
            if (!current.contains(tab)) {
                tab.visibleProperty().removeListener(weakTabVisibilityChanged);
                watchedTabs.remove(tab);
                stripeButtons.remove(tab);
            }
        }
        for (SideBarTab tab : current) {
            if (watchedTabs.add(tab)) {
                tab.visibleProperty().addListener(weakTabVisibilityChanged);
            }
        }
        updateStripe();
    }

    /**
     * A tab of the list was hidden or shown: its stripe button follows. Hiding the selected tab collapses the side
     * bar and selects no tab.
     */
    private void tabVisibilityChanged(Observable visibleFlag) {
        SideBarTab selected = selectedTab.get();
        if (selected != null && visibleFlag == selected.visibleProperty() && !selected.isVisible()) {
            deselectAndCollapse();
        }
        updateStripe();
    }

    /** The selected tab went away, removed or hidden: the side bar collapses and selects no tab. */
    private void deselectAndCollapse() {
        collapsed.set(true);
        selectedTab.set(null);
    }

    /** Shows one stripe button per visible tab, in the order of the tabs. */
    private void updateStripe() {
        List<Node> buttons = new ArrayList<>();
        for (SideBarTab tab : tabs) {
            if (tab.isVisible()) {
                buttons.add(stripeButtons.computeIfAbsent(tab, StripeButton::new));
            }
        }
        setChildren(stripe.getChildren(), buttons);
        updatePanel();
    }

    /**
     * Shows the panel of the selected tab while the side bar is expanded, and marks its stripe button; collapsed, or
     * without a selected tab among the visible tabs, only the stripe shows and no button is marked.
     */
    private void updatePanel() {
        SideBarTab open = openTab();
        boolean focusWasInside = focusReturn.isFocusInside();
        panel.setVisible(open != null);
        panel.setManaged(open != null);
        resizeEdge.setVisible(open != null);
        resizeEdge.setManaged(open != null);
        SideBarTab selected = selectedTab.get();
        setPanelOwner(selected != null && tabs.contains(selected) ? selected : null);
        updateHeader();
        Node content = panelOwner == null ? null : panelOwner.getContent();
        List<Node> contentChildren = new ArrayList<>();
        if (content != null) {
            contentChildren.add(content);
        }
        setChildren(contentHolder.getChildren(), contentChildren);
        for (Map.Entry<SideBarTab, StripeButton> entry : stripeButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == open);
        }
        if (focusWasInside && open == null) {
            // The panel hid with the focus in it, which would leave the focus on a hidden node.
            focusReturn.returnFocus();
        }
        updateCommands();
    }

    /**
     * Builds the header row for the panel's owner: its title, its own buttons, the buttons of its content if the
     * content takes part in the contract, the menu button and the hide button.
     */
    private void updateHeader() {
        List<Node> row = new ArrayList<>();
        row.add(titleLabel);
        if (panelOwner != null) {
            row.addAll(panelOwner.getHeaderButtons());
            if (panelOwner.getContent() instanceof SideBarContent content) {
                row.addAll(content.getHeaderButtons());
            }
        }
        row.add(menuButton);
        row.add(hideButton);
        setChildren(header.getChildren(), row);
    }

    /**
     * Makes a tab the panel's owner: the header's title follows its title, and a change of its header buttons or its
     * content builds the panel again.
     */
    private void setPanelOwner(SideBarTab tab) {
        if (tab == panelOwner) {
            return;
        }
        if (panelOwner != null) {
            panelOwner.getHeaderButtons().removeListener(weakPanelOwnerChanged);
            panelOwner.contentProperty().removeListener(weakPanelOwnerChanged);
        }
        panelOwner = tab;
        if (tab == null) {
            titleLabel.textProperty().unbind();
            titleLabel.setText(null);
        } else {
            titleLabel.textProperty().bind(tab.titleProperty());
            tab.getHeaderButtons().addListener(weakPanelOwnerChanged);
            tab.contentProperty().addListener(weakPanelOwnerChanged);
        }
    }

    /**
     * Sets a parent's children, only when they change: a node taken out and put back would lose the focus it holds.
     */
    private static void setChildren(ObservableList<Node> children, List<Node> wanted) {
        if (!children.equals(wanted)) {
            children.setAll(wanted);
        }
    }

    /** The open tab, whose panel shows: the selected tab while it has a stripe button and the side bar is expanded. */
    private SideBarTab openTab() {
        return collapsed.get() ? null : selectedVisibleTab();
    }

    /**
     * The selected tab while it has a stripe button: one of the tabs, and visible. Package-private: the search command
     * is enabled while there is one.
     *
     * @return the selected tab, null while there is none or it has no stripe button
     */
    SideBarTab selectedVisibleTab() {
        SideBarTab selected = selectedTab.get();
        if (selected == null || !selected.isVisible() || !tabs.contains(selected)) {
            return null;
        }
        return selected;
    }

    /**
     * A click on a tab's stripe button: on the open tab it collapses the side bar, on another tab or while collapsed
     * it selects the tab, expands and puts the focus into the tab's content.
     */
    private void stripeButtonFired(SideBarTab tab) {
        boolean opening = tab != openTab();
        if (opening) {
            selectedTab.set(tab);
            collapsed.set(false);
        } else {
            collapsed.set(true);
        }
        // The toggle button toggled itself before its action: the side bar's state decides the marks.
        updatePanel();
        if (opening) {
            focusContent(tab);
        }
    }

    /**
     * A width within the limits. Where the limits cross, the smallest width wins, as a JavaFX layout gives a node's
     * minimum width precedence over its maximum.
     */
    private double withinLimits(double width) {
        return Math.max(minContentWidth.get(), Math.min(width, maxContentWidth.get()));
    }

    /**
     * Puts the focus into a tab's content, where the user starts: as the content says, if it takes part in the
     * contract, else into the content node itself.
     */
    private static void focusContent(SideBarTab tab) {
        Node content = tab.getContent();
        if (content instanceof SideBarContent participant) {
            participant.focusContent();
        } else if (content != null) {
            content.requestFocus();
        }
    }

    /**
     * Keeps the side bar's commands in line with its tabs, once they are registered: a command for every tab id, each
     * enabled while a visible tab has its id, the search while a visible tab is selected.
     */
    private void updateCommands() {
        if (commands == null) {
            return;
        }
        commands.update();
        for (Map.Entry<SideBarTab, StripeButton> entry : stripeButtons.entrySet()) {
            entry.getValue().showShortcutOf(commands.tabCommand(entry.getKey().getId()));
        }
    }

    /**
     * The command of a tab id: the first visible tab with that id opens, or the side bar collapses when that tab is
     * open, as a click on its stripe button does.
     */
    void showTab(String tabId) {
        for (SideBarTab tab : tabs) {
            if (tab.isVisible() && tab.getId().equals(tabId)) {
                stripeButtonFired(tab);
                return;
            }
        }
    }

    /** The command Hide Side Bar: the side bar collapses. */
    void hide() {
        collapsed.set(true);
    }

    /**
     * The command of the search: the side bar expands and the selected tab's content focuses its search, or takes the
     * focus itself when it has none. Without a visible selected tab nothing happens.
     */
    void searchSelectedTab() {
        SideBarTab selected = selectedVisibleTab();
        if (selected == null) {
            return;
        }
        collapsed.set(false);
        boolean searching = selected.getContent() instanceof SideBarContent content && content.focusSearch();
        if (!searching) {
            focusContent(selected);
        }
    }

    /** The menu button: the open tab's menu, under the button. */
    private void menuButtonFired() {
        if (panelOwner != null) {
            openMenu(panelOwner, menuButton, null);
        }
    }

    /** A right click on the header, on its title, its buttons or beside them: the open tab's menu, at the pointer. */
    private void headerMenuRequested(ContextMenuEvent event) {
        if (panelOwner != null) {
            openMenu(panelOwner, header, event);
            event.consume();
        }
    }

    /**
     * Fills the menu for a tab and shows it, when the side bar is in a shown window: under the anchor, or at the
     * pointer of a right click. Elsewhere it is filled without showing.
     */
    private void openMenu(SideBarTab tab, Node anchor, ContextMenuEvent request) {
        ContextMenu shown = getMenu();
        fillMenu(shown, tab);
        Scene scene = anchor.getScene();
        Window window = scene == null ? null : scene.getWindow();
        if (window == null || !window.isShowing()) {
            return;
        }
        if (request == null) {
            shown.show(anchor, Side.BOTTOM, 0, 0);
        } else {
            shown.show(anchor, request.getScreenX(), request.getScreenY());
        }
    }

    /**
     * Fills the menu of a tab: the tab's own items, the items of its content if the content takes part in the
     * contract, a separator, and Hide, which collapses the side bar while that tab is open and is disabled otherwise.
     * Without items above it, Hide stands alone, without a separator.
     */
    private void fillMenu(ContextMenu filled, SideBarTab tab) {
        List<MenuItem> items = new ArrayList<>(tab.getMenuItems());
        if (tab.getContent() instanceof SideBarContent content) {
            items.addAll(content.getMenuItems());
        }
        if (!items.isEmpty()) {
            items.add(new SeparatorMenuItem());
        }
        hideItem.setDisable(tab != openTab());
        items.add(hideItem);
        filled.getItems().setAll(items);
    }

    /** The icon a stripe button shows; null for none. */
    private static Node iconNode(Ikon ikon) {
        return ikon == null ? null : FontIcon.of(ikon);
    }

    /**
     * A limit of the content width, the smallest or the largest: it refuses a value that is no finite number of
     * pixels, zero or more, and a change puts the content width within the limits, unless the application has bound
     * the content width; the panel shows a bound width within the limits anyway.
     */
    private final class WidthLimit extends SimpleDoubleProperty {

        private WidthLimit(String name, double initialValue) {
            super(SideBar.this, name, initialValue);
        }

        @Override
        public void set(double limit) {
            if (Double.isNaN(limit) || Double.isInfinite(limit) || limit < 0) {
                throw new IllegalArgumentException("a limit of the content width is a finite number of pixels, zero "
                        + "or more, not " + limit);
            }
            super.set(limit);
        }

        @Override
        protected void invalidated() {
            if (!contentWidth.isBound()) {
                contentWidth.set(contentWidth.get());
            }
        }
    }

    /**
     * A tooltip that names the key of a command: the text, and the key in brackets while there is one, such as a
     * stripe button's <i>Files (Alt+1)</i>, the tab's title and the key of its command, or the hide button's
     * <i>Hide (Shift+Escape)</i>, the hide text and the key of Hide Side Bar; the key alone for an empty text.
     */
    private static String tooltipText(String text, String key) {
        if (key.isEmpty()) {
            return text;
        }
        return text == null || text.isEmpty() ? key : text + " (" + key + ")";
    }

    /**
     * A tab's button on the stripe: its icon, its title as tooltip and accessible text, the tooltip with the key of the
     * tab's command.
     */
    private final class StripeButton extends ToggleButton {

        /** The display text of the current key of the tab's command; empty while there is none. */
        private final StringProperty shortcutText = new SimpleStringProperty(this, "shortcutText", "");
        /** The command whose key the tooltip names; null for none. */
        private Command shortcutCommand;

        private StripeButton(SideBarTab tab) {
            getStyleClass().add("side-bar-tab");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            // As in an IDE's stripe: the Tab key passes the stripe, and a click leaves the focus to the content.
            setFocusTraversable(false);
            graphicProperty().bind(Bindings.createObjectBinding(() -> iconNode(tab.getIcon()), tab.iconProperty()));
            Tooltip tooltip = new Tooltip();
            tooltip.textProperty().bind(Bindings.createStringBinding(
                    () -> tooltipText(tab.getTitle(), shortcutText.get()), tab.titleProperty(), shortcutText));
            setTooltip(tooltip);
            accessibleTextProperty().bind(tab.titleProperty());
            setOnAction(event -> stripeButtonFired(tab));
            setOnContextMenuRequested(event -> {
                openMenu(tab, this, event);
                event.consume();
            });
        }

        /** Names a command's key in the tooltip, following its every change; null names none. */
        private void showShortcutOf(Command command) {
            if (command == shortcutCommand) {
                return;
            }
            shortcutCommand = command;
            if (command == null) {
                shortcutText.unbind();
                shortcutText.set("");
            } else {
                shortcutText.bind(command.shortcutDisplayTextProperty());
            }
        }
    }
}
