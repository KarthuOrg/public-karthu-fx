package org.karthu.fx.markdown;

import javafx.event.EventTarget;
import javafx.scene.Node;

/**
 * An image in a table cell, which the cell shows as an image icon and its alt text, never as a picture: what opens it
 * in the full size window, its address and its alt text. The icon and the alt text carry it in their properties, so a
 * click on them and the context menu requested on them find it.
 *
 * @param address the image's address as written
 * @param alt     the image's alt text
 */
record CellImage(String address, String alt) {

    /**
     * The key of the image in the properties of the nodes that show it.
     */
    private static final Object KEY = new Object();

    /**
     * Notes that a node shows this image: the icon or the alt text of a cell.
     *
     * @param part the node
     */
    void markShownBy(Node part) {
        part.getProperties().put(KEY, this);
    }

    /**
     * The image in a table cell an event happened on: the one shown by the event's target or by one of its parents,
     * below the node where the walk stops.
     *
     * @param target where the event happened
     * @param stop   the node the walk stops at
     * @return the image, {@code null} if the event happened on none
     */
    static CellImage at(EventTarget target, Node stop) {
        if (!(target instanceof Node node)) {
            return null;
        }
        for (Node current = node; current != null && current != stop; current = current.getParent()) {
            // hasProperties first: reading the properties of a node without any would create its map.
            if (current.hasProperties() && current.getProperties().get(KEY) instanceof CellImage image) {
                return image;
            }
        }
        return null;
    }
}
