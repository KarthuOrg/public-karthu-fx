package org.karthu.fx.markdown;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * An SLF4J logger, as karthu-fx-core logs, that records what the view logs, so that no test writes to the
 * application's log. SLF4J's own base class turns every logging call into one call of
 * {@link #handleNormalizedLoggingCall}. The view's tests put it in through the view's package-private logger switch.
 */
final class RecordingLogger extends LegacyAbstractLogger {

    /**
     * What was logged, in order.
     */
    final transient List<Record> records = new ArrayList<>();

    RecordingLogger() {
        name = "recorder";
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern,
                                               Object[] arguments, Throwable throwable) {
        records.add(new Record(level, MessageFormatter.basicArrayFormat(messagePattern, arguments), throwable));
    }

    /**
     * One record: its level, its message and what was thrown with it.
     *
     * @param level   the level
     * @param message the message, its arguments filled in
     * @param thrown  what was thrown with it, {@code null} if nothing
     */
    record Record(Level level, String message, Throwable thrown) {
    }
}
