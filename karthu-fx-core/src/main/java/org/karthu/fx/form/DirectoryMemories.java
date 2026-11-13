package org.karthu.fx.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * The {@link DirectoryMemory} implementations and the shared memory. Package-private; applications reach them through
 * the interface's factories.
 */
final class DirectoryMemories {

    private static final Logger log = LoggerFactory.getLogger(DirectoryMemories.class);

    /**
     * The application-wide shared memory. Volatile: an application may set it in {@code Application.init()}, on the
     * launcher thread, before the fields read it on the JavaFX Application Thread.
     */
    private static volatile DirectoryMemory shared = new InMemory();

    private DirectoryMemories() {
    }

    /**
     * The application-wide shared memory.
     *
     * @return the shared memory, never null
     */
    static DirectoryMemory shared() {
        return shared;
    }

    /**
     * Replaces the application-wide shared memory.
     *
     * @param memory the new shared memory, not null
     */
    static void setShared(DirectoryMemory memory) {
        shared = Objects.requireNonNull(memory, "memory must not be null");
    }

    /** A directory as every memory keeps it. */
    private static Path absolute(Path directory) {
        Objects.requireNonNull(directory, "directory must not be null");
        return directory.toAbsolutePath().normalize();
    }

    /** The last directory in a field, for as long as the application runs. */
    static final class InMemory implements DirectoryMemory {

        private Path lastDirectory;

        @Override
        public Optional<Path> lastDirectory() {
            return Optional.ofNullable(lastDirectory);
        }

        @Override
        public void remember(Path directory) {
            lastDirectory = absolute(directory);
        }
    }

    /** The last directory's absolute path under one key of a Preferences node. */
    static final class OnPreferences implements DirectoryMemory {

        /** The one key this memory writes. */
        private static final String KEY = "lastDirectory";

        private final Preferences node;

        /**
         * Creates a memory on a Preferences node.
         *
         * @param node the node, not null
         */
        OnPreferences(Preferences node) {
            this.node = Objects.requireNonNull(node, "node must not be null");
        }

        @Override
        public Optional<Path> lastDirectory() {
            String stored;
            try {
                stored = node.get(KEY, null);
            } catch (IllegalStateException removed) {
                return Optional.empty();
            }
            if (stored == null || stored.isEmpty()) {
                return Optional.empty();
            }
            try {
                Path path = Path.of(stored);
                return path.isAbsolute() ? Optional.of(path) : Optional.empty();
            } catch (InvalidPathException invalid) {
                return Optional.empty();
            }
        }

        @Override
        public void remember(Path directory) {
            Path path = absolute(directory);
            try {
                node.put(KEY, path.toString());
            } catch (IllegalArgumentException | IllegalStateException refused) {
                // Too long for a Preferences value, or the node was removed. The node's absolutePath(), never the
                // node itself: a node's toString() asks for the user root, which opens the platform's store.
                log.debug("Last directory not remembered in the Preferences node {}: {}",
                        safeNodePath(), refused.getMessage());
            }
        }

        private String safeNodePath() {
            try {
                return node.absolutePath();
            } catch (IllegalStateException removed) {
                return "(removed)";
            }
        }
    }
}
