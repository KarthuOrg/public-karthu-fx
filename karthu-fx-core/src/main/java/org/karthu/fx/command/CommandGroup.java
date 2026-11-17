package org.karthu.fx.command;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A group of commands, such as <i>File</i>, <i>Edit</i> or <i>View</i>: the commands of an area of the application,
 * shown together under the group's name. Created by {@link CommandRegistry#addGroup(String, String)}.
 *
 * @since 1.1.0-RC2
 */
public final class CommandGroup {

    private final String id;

    private final StringProperty name;

    /**
     * Creates a group.
     *
     * @param id   the group's id, unique in its registry
     * @param name the group's name
     */
    CommandGroup(String id, String name) {
        this.id = id;
        this.name = new SimpleStringProperty(this, "name", name);
    }

    /**
     * Returns the group's id, unique in its registry.
     *
     * @return the id
     * @since 1.1.0-RC2
     */
    public String getId() {
        return id;
    }

    /**
     * The group's name, such as <i>File</i>. A property so that an application can translate it.
     *
     * @return the name property
     * @since 1.1.0-RC2
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the group's name.
     *
     * @return the name
     * @since 1.1.0-RC2
     */
    public String getName() {
        return name.get();
    }

    /**
     * Sets the group's name.
     *
     * @param name the name
     * @since 1.1.0-RC2
     */
    public void setName(String name) {
        this.name.set(name);
    }
}
