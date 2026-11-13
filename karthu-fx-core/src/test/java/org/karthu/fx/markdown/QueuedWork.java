package org.karthu.fx.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * The view's image work for its tests, which only queues: loads, decodes and recomputes wait until the test runs them,
 * on its own thread, so a test sees the view before an image arrives, the order of the decodes and a bitmap before and
 * after its size has settled. The full size window's work waits with the loads. Changes of the scene run at once.
 * Tests put it in through the view's package-private work switch ({@link MarkdownView#setImageWork(ImageWork)}), one
 * per test.
 */
final class QueuedWork implements ImageWork {

    private final List<Runnable> loads = new ArrayList<>();
    private final DecodeQueue decodes = new DecodeQueue();
    private final List<Runnable> settles = new ArrayList<>();

    @Override
    public void load(Runnable task) {
        loads.add(task);
    }

    @Override
    public void decode(ImageNode image, Runnable task) {
        decodes.add(image, task);
    }

    @Override
    public void show(Runnable task) {
        task.run();
    }

    @Override
    public void compute(Runnable task) {
        loads.add(task);
    }

    @Override
    public Settling settling(Runnable task) {
        return () -> {
            if (!settles.contains(task)) {
                settles.add(task);
            }
        };
    }

    /**
     * Runs the waiting loads and those they add, in order.
     */
    void runLoads() {
        while (!loads.isEmpty()) {
            loads.removeFirst().run();
        }
    }

    /**
     * Runs the decode the decoding thread would take next: the nearest to the visible area.
     *
     * @return {@code true} if a decode ran, {@code false} if none waited
     */
    boolean runNextDecode() {
        Runnable next = decodes.next();
        if (next == null) {
            return false;
        }
        next.run();
        return true;
    }

    /**
     * Runs everything that waits and everything that adds, until nothing waits: loads first, then decodes, then the
     * recomputes of sizes that have settled.
     */
    void runAll() {
        boolean ran = true;
        while (ran) {
            if (!loads.isEmpty()) {
                loads.removeFirst().run();
            } else if (!runNextDecode()) {
                ran = !settles.isEmpty();
                if (ran) {
                    settles.removeFirst().run();
                }
            }
        }
    }
}
