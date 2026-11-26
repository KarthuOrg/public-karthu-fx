package org.karthu.fx.sidebar;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.Ikon;

import java.util.Objects;

/**
 * A tab of a {@link SideBar}: a button on the side bar's stripe and, while it is open, the panel beside the
 * application's content with its header and its content. Like JavaFX's own tab, it is no node itself; the side bar
 * draws it.
 *
 * <p>A tab has an {@linkplain #getId() id}, stable for its whole life, by which an application stores which tab was
 * open; a {@linkplain #titleProperty() title}, the stripe button's tooltip and the panel header's text; an
 * {@linkplain #iconProperty() icon}, an Ikonli icon drawn on the stripe; and its {@linkplain #contentProperty()
 * content}, any node, shown below the header while the tab is open. A content that implements {@link SideBarContent},
 * such as a {@link NavigationTree}, adds its own buttons to the header and its own items to the menu, and says where the
 * focus goes when the tab opens.
 *
 * <p>The tab's own {@linkplain #getHeaderButtons() header buttons} go into its header before the content's, and its
 * own {@linkplain #getMenuItems() menu items} into its menu before the content's, for actions such as <i>New</i> that
 * belong to the panel rather than to its content. An {@linkplain #visibleProperty() invisible} tab has no stripe
 * button; hiding the open tab collapses the side bar, as removing it does. Every change of a tab shows at once.
 *
 * <p>Like every karthu-fx control, a tab is used on the JavaFX Application Thread only.
 *
 * @since 1.1.0-RC2
 */
public class SideBarTab {

    private final String id;
    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private final ObjectProperty<Ikon> icon = new SimpleObjectProperty<>(this, "icon");
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content");
    private final BooleanProperty visible = new SimpleBooleanProperty(this, "visible", true);
    private final ObservableList<Node> headerButtons = FXCollections.observableArrayList();
    private final ObservableList<MenuItem> menuItems = FXCollections.observableArrayList();

    /**
     * Creates a visible tab without own header buttons or menu items.
     *
     * @param id      the tab's id, stable, by which an application stores which tab was open
     * @param title   the tab's title, the tooltip of its stripe button and the text of its header; null for none
     * @param icon    the icon of its stripe button, such as one of the Bootstrap icons; null for none
     * @param content the node its panel shows below the header; null for none
     * @throws NullPointerException     when the id is null
     * @throws IllegalArgumentException when the id is blank
     * @since 1.1.0-RC2
     */
    public SideBarTab(String id, String title, Ikon icon, Node content) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("the id of a side bar tab is blank");
        }
        this.id = id;
        setTitle(title);
        setIcon(icon);
        setContent(content);
    }

    /**
     * Returns the tab's id, which never changes: an application stores it to open the same tab after a restart.
     *
     * @return the id
     * @since 1.1.0-RC2
     */
    public final String getId() {
        return id;
    }

    /**
     * The tab's title: the tooltip and accessible text of its stripe button and the text of its panel's header. A
     * short name, such as <i>Project</i>, in title case. A change shows at once.
     *
     * @return the title property
     * @since 1.1.0-RC2
     */
    public final StringProperty titleProperty() {
        return title;
    }

    /**
     * Returns the tab's title.
     *
     * @return the title, null for none
     * @since 1.1.0-RC2
     */
    public final String getTitle() {
        return title.get();
    }

    /**
     * Sets the tab's title.
     *
     * @param title the title, null for none
     * @since 1.1.0-RC2
     */
    public final void setTitle(String title) {
        this.title.set(title);
    }

    /**
     * The icon of the tab's stripe button: an Ikonli icon, such as one of the Bootstrap icons, drawn and coloured by
     * the theme. Null shows a button without an icon, which only its tooltip tells apart.
     *
     * @return the icon property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<Ikon> iconProperty() {
        return icon;
    }

    /**
     * Returns the icon of the tab's stripe button.
     *
     * @return the icon, null for none
     * @since 1.1.0-RC2
     */
    public final Ikon getIcon() {
        return icon.get();
    }

    /**
     * Sets the icon of the tab's stripe button.
     *
     * @param icon the icon, null for none
     * @since 1.1.0-RC2
     */
    public final void setIcon(Ikon icon) {
        this.icon.set(icon);
    }

    /**
     * The node the tab's panel shows below its header, filling the rest of the panel: a {@link NavigationTree} or any
     * other node. A node can sit in one place only, so it belongs to this tab alone. Null shows an empty panel.
     *
     * @return the content property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<Node> contentProperty() {
        return content;
    }

    /**
     * Returns the node the tab's panel shows.
     *
     * @return the content, null for none
     * @since 1.1.0-RC2
     */
    public final Node getContent() {
        return content.get();
    }

    /**
     * Sets the node the tab's panel shows.
     *
     * @param content the content, null for none
     * @since 1.1.0-RC2
     */
    public final void setContent(Node content) {
        this.content.set(content);
    }

    /**
     * Whether the tab has a button on the stripe (default true). Hiding the open tab collapses the side bar and
     * selects no tab, as removing it does; showing it again shows its button, without opening it.
     *
     * @return the visible property
     * @since 1.1.0-RC2
     */
    public final BooleanProperty visibleProperty() {
        return visible;
    }

    /**
     * Returns whether the tab has a button on the stripe.
     *
     * @return true when the tab is visible
     * @since 1.1.0-RC2
     */
    public final boolean isVisible() {
        return visible.get();
    }

    /**
     * Sets whether the tab has a button on the stripe.
     *
     * @param visible true to show the tab's button
     * @since 1.1.0-RC2
     */
    public final void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    /**
     * Returns the tab's own header buttons, which its panel's header shows left to right after the title and before
     * the content's buttons. The application creates them and styles them, usually as an {@code icon-button} with an
     * icon and a tooltip; a change of the list shows at once.
     *
     * @return the live list of the header buttons
     * @since 1.1.0-RC2
     */
    public final ObservableList<Node> getHeaderButtons() {
        return headerButtons;
    }

    /**
     * Returns the tab's own menu items, which its menu lists first, before the content's items and the side bar's
     * built-in <i>Hide</i>. The side bar reads them each time it opens the menu.
     *
     * @return the live list of the menu items
     * @since 1.1.0-RC2
     */
    public final ObservableList<MenuItem> getMenuItems() {
        return menuItems;
    }

    /**
     * Returns a text that names the tab by its id and title, for logs and tests.
     *
     * @return the tab's id and title
     * @since 1.1.0-RC2
     */
    @Override
    public String toString() {
        return "SideBarTab[" + id + ", " + getTitle() + "]";
    }
}
