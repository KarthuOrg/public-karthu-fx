package org.karthu.fx.form;

import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Remembers the directory the user chose a path in last, so that a {@link PathInput}'s dialog starts there when the
 * field gives no folder of its own.
 *
 * <p>A path input remembers a directory for every choice in its dialog (in the directory mode the chosen directory,
 * otherwise the chosen file's folder); typed and pasted text is never remembered. One memory serves all path inputs
 * of an application, in every mode: the {@linkplain #getShared() shared memory}, unless a field is given a
 * {@linkplain PathInput#directoryMemoryProperty() memory of its own}.
 *
 * <p><b>Keeping it across restarts.</b> The shared memory starts as an {@linkplain #inMemory() in-memory memory},
 * which forgets the directory when the application ends. An application that wants the last directory kept sets a
 * memory on {@linkplain #preferences(Preferences) its own Java Preferences node} at start-up, before its first form
 * is shown:
 *
 * <pre>{@code
 * DirectoryMemory.setShared(DirectoryMemory.preferences(Preferences.userNodeForPackage(MyApp.class)));
 * }</pre>
 *
 * <p>A host that cannot use Java Preferences (a native image without a Preferences backend, say) implements this
 * interface on its own store, such as its configuration file, and sets that. Path inputs never write to the
 * Preferences on their own.
 *
 * <p>Path inputs call a memory on the JavaFX Application Thread. The shared memory may be set from any thread, at
 * start-up as later.
 */
public interface DirectoryMemory {

    /**
     * Returns the directory remembered last. It may no longer exist; the path input then starts in its nearest
     * existing ancestor.
     *
     * @return the last directory, absolute, or empty when there is none
     */
    Optional<Path> lastDirectory();

    /**
     * Remembers a directory as the last one, replacing the one before.
     *
     * @param directory the directory, not null; kept absolute and normalised
     */
    void remember(Path directory);

    /**
     * Returns a new memory that keeps the last directory in memory only, for as long as the application runs.
     *
     * @return a new, empty in-memory memory
     */
    static DirectoryMemory inMemory() {
        return new DirectoryMemories.InMemory();
    }

    /**
     * Returns a memory on a Java Preferences node the caller chooses, usually the application's own: it keeps the
     * last directory's absolute path under the one key {@code lastDirectory} of that node, so that it survives a
     * restart. A stored value that is no absolute path counts as none. A directory whose path is longer than a
     * Preferences value may be ({@link Preferences#MAX_VALUE_LENGTH}) is not remembered, and neither is any when the
     * node has been removed; the memory never throws for the store. The node writes through to its backing store as
     * Java Preferences do, at the latest when the application ends.
     *
     * @param node the node to keep the last directory in, not null
     * @return a memory on that node
     */
    static DirectoryMemory preferences(Preferences node) {
        return new DirectoryMemories.OnPreferences(node);
    }

    /**
     * Returns the application-wide shared memory, which every path input uses while its own
     * {@linkplain PathInput#directoryMemoryProperty() memory} is null. It starts as an in-memory memory.
     *
     * @return the shared memory, never null
     */
    static DirectoryMemory getShared() {
        return DirectoryMemories.shared();
    }

    /**
     * Replaces the application-wide shared memory, usually once at start-up (see the class description). Path inputs
     * take the shared memory each time they use it, so the new one applies to existing fields too.
     *
     * @param memory the new shared memory, not null
     */
    static void setShared(DirectoryMemory memory) {
        DirectoryMemories.setShared(memory);
    }
}
