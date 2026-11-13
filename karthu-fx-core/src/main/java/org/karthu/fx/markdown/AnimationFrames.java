package org.karthu.fx.markdown;

import javafx.animation.AnimationTimer;

import java.util.function.DoubleConsumer;

/**
 * A source of animation frames, which the view's drag scrolling steps by: a frame per pulse in the application, frames
 * advanced by hand in the view's tests, so these run on their own thread without a timer.
 */
interface AnimationFrames {

    /**
     * Calls a step once per frame from now on, with the seconds since the frame before; the first frame comes one
     * frame after the start. Replaces the step of an earlier start.
     *
     * @param step the step, called with the seconds since the last frame
     */
    void start(DoubleConsumer step);

    /**
     * Stops calling the step; nothing happens if none runs.
     */
    void stop();

    /**
     * Frames of the JavaFX pulse, through an {@link AnimationTimer}.
     *
     * @return the frames
     */
    static AnimationFrames pulses() {
        return new PulseFrames();
    }

    /**
     * Frames of the JavaFX pulse.
     */
    final class PulseFrames implements AnimationFrames {

        private static final double NANOS_PER_SECOND = 1e9;

        private AnimationTimer timer;

        private PulseFrames() {
        }

        @Override
        public void start(DoubleConsumer step) {
            stop();
            timer = new AnimationTimer() {

                private long last = -1;

                @Override
                public void handle(long now) {
                    if (last >= 0) {
                        step.accept((now - last) / NANOS_PER_SECOND);
                    }
                    last = now;
                }
            };
            timer.start();
        }

        @Override
        public void stop() {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
        }
    }
}
