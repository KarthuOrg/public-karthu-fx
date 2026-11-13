package org.karthu.fx.markdown;

/**
 * The view's image work for its tests, every step at once on the calling thread, and a recompute at once at a change of
 * a size: a test sees the finished bitmap right after it lays the view out. Tests put it in through the view's
 * package-private work switch ({@link MarkdownView#setImageWork(ImageWork)}).
 */
final class ImmediateWork implements ImageWork {

    @Override
    public void load(Runnable task) {
        task.run();
    }

    @Override
    public void decode(ImageNode image, Runnable task) {
        task.run();
    }

    @Override
    public void show(Runnable task) {
        task.run();
    }

    @Override
    public void compute(Runnable task) {
        task.run();
    }

    @Override
    public Settling settling(Runnable task) {
        return () -> task.run();
    }
}
