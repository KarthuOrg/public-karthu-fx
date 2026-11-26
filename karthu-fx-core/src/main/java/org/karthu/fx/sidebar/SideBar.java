package org.karthu.fx.sidebar;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
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
 * its icon, its title as tooltip and as accessible text, so that a screen reader names the icon. The stripe is no stop
 * of the Tab key, and a click on it leaves the focus where opening a tab puts it.
 *
 * <p><b>Open tab and collapsing.</b> The {@linkplain #selectedTabProperty() selected tab} is the one the panel shows,
 * and the {@linkplain #collapsedProperty() collapsed} flag hides the panel, so that only the stripe shows. At most one
 * tab is open: the selected tab, while the side bar is not collapsed and the tab is one of its visible tabs, and only
 * its stripe button is marked. A click on the button of another tab selects it and expands the side bar; a click on
 * the open tab's button collapses it, and while collapsed expands it again. The hide button in the panel's header and
 * the menu's <i>Hide</i> collapse it too. Collapsing keeps the selected tab, so expanding shows it again. When the
 * selected tab is removed or hidden, the side bar collapses and selects no tab. Everything happens at once, without
 * animation, and the panel always lies beside the content, which becomes narrower, never under the panel. The panel
 * is 260 px wide, for every tab.
 *
 * <p><b>Focus.</b> Opening a tab by a click on its stripe button puts the keyboard focus into its content, where the
 * user starts: a content that implements {@link SideBarContent} takes it as it says (a {@link NavigationTree} in its
 * tree, on the selected entry), any other content node takes it itself. A tab the application selects moves no focus.
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
 * selected tab's {@linkplain SideBarTab#getId() id} and the collapsed flag from the properties, and sets them again on
 * the next start; a tab it selects before adding it shows once it is added.
 *
 * <p><b>Texts.</b> The tooltips and accessible texts of the menu button ({@linkplain #moreTextProperty() More}) and
 * the hide button, which is also the text of the menu's item ({@linkplain #hideTextProperty() Hide}), are properties
 * with English defaults, so that an application can translate them.
 *
 * <p><b>Style classes</b>, a public contract: renaming one is a breaking change. The theme draws the stripe on the
 * surface with a 1 px line towards the panel, its buttons flat and square with the icon 16 px in the secondary text
 * colour, the open tab's in the accent colour on the raised grey of a button; the panel on the surface with a 1 px
 * line towards the content, the header's title in regular weight and its icon buttons 14 px in the secondary text
 * colour.
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
 *   <li>{@code side-bar-menu}: the menu, a {@link ContextMenu}, and {@code side-bar-hide-item}: its <i>Hide</i></li>
 * </ul>
 * A header button of the application's own, styled as an {@code icon-button}, takes the look of the header's.
 *
 * <p>Like every karthu-fx control, the side bar is used on the JavaFX Application Thread only.
 *
 * @since 1.1.0-RC2
 */
public class SideBar extends HBox {

    /** The width of the panel, the same for every tab. */
    private static final double PANEL_WIDTH = 260;
    private static final PseudoClass LEFT_PSEUDO_CLASS = PseudoClass.getPseudoClass("left");
    private static final PseudoClass RIGHT_PSEUDO_CLASS = PseudoClass.getPseudoClass("right");

    private final ObservableList<SideBarTab> tabs = FXCollections.observableArrayList();
    /** The side, LEFT or RIGHT: set refuses any other, so a refused side never reaches the listeners. */
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

    private final VBox stripe = new VBox();
    private final Label titleLabel = new Label();
    private final Button menuButton = headerButton("side-bar-menu-button", BootstrapIcons.THREE_DOTS_VERTICAL,
            moreText, this::menuButtonFired);
    private final Button hideButton = headerButton("side-bar-hide-button", BootstrapIcons.DASH, hideText,
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
    /** The tab whose panel the header and the content are built for, whose buttons and content it listens to. */
    private SideBarTab panelTab;
    private final InvalidationListener panelTabChanged = observable -> updatePanel();
    /** Held weakly by the panel's tab, so that a tab outliving the side bar never keeps it alive. */
    private final WeakInvalidationListener weakPanelTabChanged = new WeakInvalidationListener(panelTabChanged);

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
        panel.setMinWidth(PANEL_WIDTH);
        panel.setPrefWidth(PANEL_WIDTH);
        panel.setMaxWidth(PANEL_WIDTH);
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
     * that it is not taken for a content's Collapse All.
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

    // --- Inside --------------------------------------------------------------------------------------

    /**
     * Places the stripe at the window's edge and the panel between it and the application's content: on the left
     * stripe then panel, on the right mirrored, so the panel's inner side always faces the content. A bound side
     * other than RIGHT counts as LEFT.
     */
    private void arrange() {
        boolean right = side.get() == Side.RIGHT;
        List<Node> parts = right ? List.of(panel, stripe) : List.of(stripe, panel);
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
            putAway();
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
            putAway();
        }
        updateStripe();
    }

    /** The selected tab went away: the side bar collapses and selects no tab. */
    private void putAway() {
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
        SideBarTab shown = shownTab();
        panel.setVisible(shown != null);
        panel.setManaged(shown != null);
        SideBarTab selected = selectedTab.get();
        watchPanelTab(selected != null && tabs.contains(selected) ? selected : null);
        updateHeader();
        Node content = panelTab == null ? null : panelTab.getContent();
        List<Node> held = new ArrayList<>();
        if (content != null) {
            held.add(content);
        }
        setChildren(contentHolder.getChildren(), held);
        for (Map.Entry<SideBarTab, StripeButton> entry : stripeButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == shown);
        }
    }

    /**
     * Builds the header row for the panel's tab: its title, its own buttons, the buttons of its content if the
     * content takes part in the contract, the menu button and the hide button.
     */
    private void updateHeader() {
        List<Node> row = new ArrayList<>();
        row.add(titleLabel);
        if (panelTab != null) {
            row.addAll(panelTab.getHeaderButtons());
            if (panelTab.getContent() instanceof SideBarContent content) {
                row.addAll(content.getHeaderButtons());
            }
        }
        row.add(menuButton);
        row.add(hideButton);
        setChildren(header.getChildren(), row);
    }

    /**
     * Makes a tab the panel's: the header's title follows its title, and a change of its header buttons or its
     * content builds the panel again.
     */
    private void watchPanelTab(SideBarTab tab) {
        if (tab == panelTab) {
            return;
        }
        if (panelTab != null) {
            panelTab.getHeaderButtons().removeListener(weakPanelTabChanged);
            panelTab.contentProperty().removeListener(weakPanelTabChanged);
        }
        panelTab = tab;
        if (tab == null) {
            titleLabel.textProperty().unbind();
            titleLabel.setText(null);
        } else {
            titleLabel.textProperty().bind(tab.titleProperty());
            tab.getHeaderButtons().addListener(weakPanelTabChanged);
            tab.contentProperty().addListener(weakPanelTabChanged);
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

    /** The tab whose panel shows: the selected tab, while expanded and while it is one of the visible tabs. */
    private SideBarTab shownTab() {
        SideBarTab selected = selectedTab.get();
        if (selected == null || collapsed.get() || !selected.isVisible() || !tabs.contains(selected)) {
            return null;
        }
        return selected;
    }

    /**
     * A click on a tab's stripe button: on the open tab it collapses the side bar, on another tab or while collapsed
     * it selects the tab, expands and puts the focus into the tab's content.
     */
    private void stripeButtonFired(SideBarTab tab) {
        boolean opening = tab != shownTab();
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

    /** The menu button: the open tab's menu, under the button. */
    private void menuButtonFired() {
        if (panelTab != null) {
            openMenu(panelTab, menuButton, null);
        }
    }

    /** A right click on the header, on its title, its buttons or beside them: the open tab's menu, at the pointer. */
    private void headerMenuRequested(ContextMenuEvent event) {
        if (panelTab != null) {
            openMenu(panelTab, header, event);
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
        hideItem.setDisable(tab != shownTab());
        items.add(hideItem);
        filled.getItems().setAll(items);
    }

    /** The icon a stripe button shows; null for none. */
    private static Node iconNode(Ikon ikon) {
        return ikon == null ? null : FontIcon.of(ikon);
    }

    /** A button of the header: an icon button whose tooltip and accessible text follow a text property. */
    private static Button headerButton(String styleClass, Ikon ikon, StringProperty text, Runnable action) {
        Button button = new Button();
        button.getStyleClass().addAll("icon-button", styleClass);
        button.setGraphic(FontIcon.of(ikon));
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(text);
        button.setTooltip(tooltip);
        button.accessibleTextProperty().bind(text);
        button.setOnAction(event -> action.run());
        return button;
    }

    /** A tab's button on the stripe: its icon, its title as tooltip and accessible text. */
    private final class StripeButton extends ToggleButton {

        private StripeButton(SideBarTab tab) {
            getStyleClass().add("side-bar-tab");
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            // As in an IDE's stripe: the Tab key passes the stripe, and a click leaves the focus to the content.
            setFocusTraversable(false);
            graphicProperty().bind(Bindings.createObjectBinding(() -> iconNode(tab.getIcon()), tab.iconProperty()));
            Tooltip tooltip = new Tooltip();
            tooltip.textProperty().bind(tab.titleProperty());
            setTooltip(tooltip);
            accessibleTextProperty().bind(tab.titleProperty());
            setOnAction(event -> stripeButtonFired(tab));
            setOnContextMenuRequested(event -> {
                openMenu(tab, this, event);
                event.consume();
            });
        }
    }
}
