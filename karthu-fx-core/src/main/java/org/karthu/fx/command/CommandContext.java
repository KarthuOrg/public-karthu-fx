package org.karthu.fx.command;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

/**
 * Where the keys of commands hold: the whole window, or a part of it bound to a node, such as a document area or a
 * canvas. A command of a part runs on its key while the focus is on the part's node or inside it, and there it wins
 * over a command of the whole window on the same key. Parts nest as their nodes do. The context of the whole window is
 * {@link CommandRegistry#getWindowContext()}; a part is added with
 * {@link CommandRegistry#addContext(String, String, Node)}.
 *
 * @since 1.1.0-RC2
 */
public final class CommandContext {

    private final String id;

    private final StringProperty name;

    private final Node node;

    /**
     * Creates a context.
     *
     * @param id   the context's id, unique in its registry
     * @param name the context's name
     * @param node the node of the part of the window, null for the whole window
     */
    CommandContext(String id, String name, Node node) {
        this.id = id;
        this.name = new SimpleStringProperty(this, "name", name);
        this.node = node;
    }

    /**
     * Returns the context's id, unique in its registry: {@code window} for the context of the whole window.
     *
     * @return the id
     * @since 1.1.0-RC2
     */
    public String getId() {
        return id;
    }

    /**
     * The context's name, such as <i>Document</i>, which tells the user where the keys of its commands hold. A
     * property so that an application can translate it.
     *
     * @return the name property
     * @since 1.1.0-RC2
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the context's name.
     *
     * @return the name
     * @since 1.1.0-RC2
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the context's name.
     *
     * @param name the name
     * @since 1.1.0-RC2
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * Returns the node of the part of the window: the commands of the context run on their keys while the focus is on
     * this node or inside it.
     *
     * @return the node, null for the context of the whole window
     * @since 1.1.0-RC2
     */
    public Node getNode() {
        return node;
    }
}
