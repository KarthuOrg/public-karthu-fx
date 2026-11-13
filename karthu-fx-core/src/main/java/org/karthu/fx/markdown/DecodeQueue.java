package org.karthu.fx.markdown;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The decodes waiting for the decoding thread, taken nearest first: the decodes of images in or near the visible area
 * of their view, then the rest in document order ({@link ImageNode#order()}). The nearness is read when a decode is
 * taken, not when it is queued, so a scroll reorders what still waits. A decode of an image its view has discarded,
 * because the view shows another text, is dropped.
 *
 * <p>Thread-safe: views queue on the JavaFX Application Thread, the decoding thread takes.
 */
final class DecodeQueue {

    private final List<Waiting> waiting = new ArrayList<>();

    /**
     * Queues the decode of an image.
     *
     * @param image the node of the image
     * @param task  the decode
     */
    synchronized void add(ImageNode image, Runnable task) {
        waiting.add(new Waiting(image, task));
    }

    /**
     * Takes the decode to run next, dropping those of discarded images.
     *
     * @return the decode, {@code null} if none waits
     */
    synchronized Runnable next() {
        Waiting first = null;
        Iterator<Waiting> decodes = waiting.iterator();
        while (decodes.hasNext()) {
            Waiting decode = decodes.next();
            if (decode.image().isDiscarded()) {
                decodes.remove();
            } else if (first == null || decode.before(first)) {
                first = decode;
            }
        }
        if (first == null) {
            return null;
        }
        waiting.remove(first);
        return first.task();
    }

    /**
     * A queued decode.
     *
     * @param image the node of the image it decodes
     * @param task  the decode
     */
    private record Waiting(ImageNode image, Runnable task) {

        /**
         * Whether this decode goes before another: near the visible area before far from it, then in document
         * order.
         */
        boolean before(Waiting other) {
            boolean near = image.isNear();
            if (near != other.image.isNear()) {
                return near;
            }
            return image.order() < other.image.order();
        }
    }
}
