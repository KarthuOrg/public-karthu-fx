package org.karthu.fx.markdown;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Where the work on a view's images runs: loading an image file through the application's loader, decoding it and
 * computing the bitmap it shows at, and showing the result in the scene. The view's own work is
 * {@link #background()}. The view's tests put work of their own in through a package-private switch of the view,
 * which runs every step at once on their own thread, or queues the steps until they run them.
 *
 * <p>The three steps follow each other for one image: a load hands its bytes to a decode, a decode its bitmap to a
 * show. Each step is a task that checks itself whether its image is still wanted. An image whose size has changed
 * computes its bitmap anew once its size has {@link #settling settled}.
 */
interface ImageWork {

    /**
     * Runs a load: a call of the application's loader, which may be slow; and the full size window's writing of a
     * file the reader saves, which may be slow too.
     *
     * @param task the load
     */
    void load(Runnable task);

    /**
     * Runs a decode of an image: the work on the bytes of one image file, which needs the memory of the whole
     * decoded image, and computing the bitmap it shows at.
     *
     * @param image the node of the image the task decodes
     * @param task  the decode
     */
    void decode(ImageNode image, Runnable task);

    /**
     * Runs a change of the scene: showing a bitmap or an error.
     *
     * @param task the change
     */
    void show(Runnable task);

    /**
     * Runs work of the full size window ({@link ImageWindowContent}): decoding the whole image it shows, and shrinking
     * it to a zoom below 100 %. On the decoding thread, so it never decodes beside an image of the document.
     *
     * @param task the work
     */
    void compute(Runnable task);

    /**
     * A wait for sizes to settle: a resize or a zoom changes the size of the images at every layout, and a task that
     * computes their bitmaps anew runs only once the sizes have not changed for a moment.
     *
     * @param task the task, run on the JavaFX Application Thread once the sizes have settled
     * @return the wait, restarted at each change, on the JavaFX Application Thread
     */
    Settling settling(Runnable task);

    /**
     * A wait for sizes to settle, see {@link #settling}.
     */
    @FunctionalInterface
    interface Settling {

        /**
         * Starts the wait anew: a size has changed.
         */
        void restart();
    }

    /**
     * The view's own work: loads on background threads, several at once; decodes one at a time on one background
     * thread shared by all views, so the memory of decoding peaks at that of the largest single image; changes of the
     * scene on the JavaFX Application Thread; a recompute 150 ms after the last change of a size. The threads are
     * daemon threads and end when idle.
     *
     * @return the background work, shared by all views
     */
    static ImageWork background() {
        return Background.INSTANCE;
    }

    /**
     * Loads on a pool of background threads, decodes on one background thread, shows on the JavaFX Application
     * Thread.
     */
    final class Background implements ImageWork {

        /**
         * How many loads run at once, for all views together.
         */
        private static final int LOADS_AT_ONCE = 4;

        /**
         * How long an idle thread waits for more work before it ends.
         */
        private static final long IDLE_SECONDS = 30;

        /**
         * How long a size must stay before its image is computed anew: long enough that a resize or a zoom by the
         * wheel does not decode at every step, short enough that the image is sharp as soon as the reader stops.
         */
        private static final Duration SETTLE_TIME = Duration.millis(150);

        private static final Background INSTANCE = new Background();

        private final ExecutorService loads = pool("markdown-image-load", LOADS_AT_ONCE);

        /**
         * The one decoding thread, which runs one decode per task it gets: the nearest one waiting at that time.
         */
        private final ExecutorService decodes = pool("markdown-image-decode", 1);
        private final DecodeQueue waitingDecodes = new DecodeQueue();

        private Background() {
        }

        @Override
        public void load(Runnable task) {
            loads.execute(task);
        }

        @Override
        public void decode(ImageNode image, Runnable task) {
            waitingDecodes.add(image, task);
            // As many runs as decodes queued: each takes the nearest decode waiting when it starts. A run finds none
            // when its decode was of a discarded image, dropped by an earlier run.
            decodes.execute(() -> {
                Runnable next = waitingDecodes.next();
                if (next != null) {
                    next.run();
                }
            });
        }

        @Override
        public void show(Runnable task) {
            Platform.runLater(task);
        }

        @Override
        public void compute(Runnable task) {
            // In turn with the runs of the document's decodes, on the one decoding thread.
            decodes.execute(task);
        }

        @Override
        public Settling settling(Runnable task) {
            return new Settling() {

                /**
                 * The pause, made at the first change, on the JavaFX Application Thread.
                 */
                private PauseTransition pause;

                @Override
                public void restart() {
                    if (pause == null) {
                        pause = new PauseTransition(SETTLE_TIME);
                        pause.setOnFinished(event -> task.run());
                    }
                    pause.playFromStart();
                }
            };
        }

        /**
         * A pool of daemon threads that end when idle, its tasks queued in the order they come.
         */
        private static ExecutorService pool(String name, int threads) {
            AtomicInteger count = new AtomicInteger();
            ThreadFactory factory = runnable -> {
                Thread thread = new Thread(runnable, name + "-" + count.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, IDLE_SECONDS, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), factory);
            pool.allowCoreThreadTimeOut(true);
            return pool;
        }
    }
}
