package org.karthu.fx.form;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The directory memories without JavaFX. The preferences memory runs on a {@code Preferences} tree the test builds in
 * memory, never on the user's.
 */
class DirectoryMemoryTest {

    @TempDir
    Path temp;

    // --- In memory -----------------------------------------------------------------------------------

    @Test
    @DisplayName("an in-memory memory starts empty and gives back the directory it remembered last")
    void inMemoryRemembersTheLastDirectory() {
        DirectoryMemory memory = DirectoryMemory.inMemory();
        assertEquals(Optional.empty(), memory.lastDirectory());

        memory.remember(temp.resolve("first"));
        memory.remember(temp.resolve("second"));

        assertEquals(Optional.of(temp.resolve("second")), memory.lastDirectory());
    }

    @Test
    @DisplayName("each in-memory memory is a memory of its own")
    void inMemoryMemoriesAreIndependent() {
        DirectoryMemory one = DirectoryMemory.inMemory();
        DirectoryMemory other = DirectoryMemory.inMemory();

        one.remember(temp);

        assertEquals(Optional.empty(), other.lastDirectory());
    }

    @Test
    @DisplayName("a memory keeps a directory absolute and normalised, and refuses null")
    void rememberedDirectoryIsAbsoluteAndNormalised() {
        DirectoryMemory memory = DirectoryMemory.inMemory();

        memory.remember(temp.resolve("data").resolve(".."));
        assertEquals(Optional.of(temp), memory.lastDirectory());

        memory.remember(Path.of("relative"));
        assertEquals(Optional.of(Path.of("relative").toAbsolutePath()), memory.lastDirectory());

        assertThrows(NullPointerException.class, () -> memory.remember(null));
    }

    // --- On a Preferences node -----------------------------------------------------------------------

    @Test
    @DisplayName("a preferences memory keeps the absolute path under one key of the node it is given")
    void preferencesMemoryWritesOneKey() throws BackingStoreException {
        Preferences node = new MemoryPreferences().node("org/example/app");
        DirectoryMemory memory = DirectoryMemory.preferences(node);
        assertEquals(Optional.empty(), memory.lastDirectory());

        memory.remember(temp.resolve("data"));

        assertEquals(Optional.of(temp.resolve("data")), memory.lastDirectory());
        assertEquals(1, node.keys().length);
        assertEquals(temp.resolve("data").toString(), node.get("lastDirectory", null));
    }

    @Test
    @DisplayName("a new preferences memory on the same node reads what the last one remembered, as after a restart")
    void preferencesMemorySurvivesARestart() {
        Preferences root = new MemoryPreferences();
        DirectoryMemory.preferences(root.node("app")).remember(temp);

        DirectoryMemory afterRestart = DirectoryMemory.preferences(root.node("app"));

        assertEquals(Optional.of(temp), afterRestart.lastDirectory());
    }

    @Test
    @DisplayName("a stored value that is no absolute path counts as no last directory")
    void preferencesMemoryIgnoresUnusableValues() {
        Preferences node = new MemoryPreferences().node("app");
        DirectoryMemory memory = DirectoryMemory.preferences(node);

        node.put("lastDirectory", "");
        assertEquals(Optional.empty(), memory.lastDirectory());

        // No valid path on Windows; elsewhere a relative path, which counts as none as well.
        node.put("lastDirectory", "bad<path>");
        assertEquals(Optional.empty(), memory.lastDirectory());

        node.put("lastDirectory", "relative/folder");
        assertEquals(Optional.empty(), memory.lastDirectory());
    }

    @Test
    @DisplayName("a removed node is no memory: nothing to read, remembering is a no-op")
    void preferencesMemoryOnARemovedNode() throws BackingStoreException {
        Preferences node = new MemoryPreferences().node("app");
        DirectoryMemory memory = DirectoryMemory.preferences(node);
        node.removeNode();

        memory.remember(temp);

        assertEquals(Optional.empty(), memory.lastDirectory());
    }

    @Test
    @DisplayName("a path longer than a Preferences value may be is not remembered, and keeps the last one")
    void preferencesMemoryWithATooLongPath() {
        Preferences node = new MemoryPreferences().node("app");
        DirectoryMemory memory = DirectoryMemory.preferences(node);
        memory.remember(temp);

        memory.remember(temp.resolve("x".repeat(Preferences.MAX_VALUE_LENGTH)));

        assertEquals(Optional.of(temp), memory.lastDirectory());
    }

    @Test
    @DisplayName("a preferences memory needs a node")
    void preferencesMemoryNeedsANode() {
        assertThrows(NullPointerException.class, () -> DirectoryMemory.preferences(null));
    }

    // --- Shared --------------------------------------------------------------------------------------

    @Test
    @DisplayName("the shared memory is there from the start, can be replaced, and refuses null")
    void sharedMemory() {
        DirectoryMemory original = DirectoryMemory.getShared();
        assertNotNull(original);
        try {
            DirectoryMemory replacement = DirectoryMemory.inMemory();
            DirectoryMemory.setShared(replacement);
            assertSame(replacement, DirectoryMemory.getShared());

            assertThrows(NullPointerException.class, () -> DirectoryMemory.setShared(null));
            assertSame(replacement, DirectoryMemory.getShared());
        } finally {
            DirectoryMemory.setShared(original);
        }
    }

    /**
     * A {@code Preferences} tree in memory: what the platform's store does, without touching it. A node of this tree
     * is what a test hands to {@link DirectoryMemory#preferences(Preferences)}.
     */
    private static final class MemoryPreferences extends AbstractPreferences {

        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryPreferences> children = new HashMap<>();

        /** The root of a new tree. */
        MemoryPreferences() {
            this(null, "");
        }

        private MemoryPreferences(MemoryPreferences parent, String name) {
            super(parent, name);
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            values.clear();
        }

        @Override
        protected String[] keysSpi() {
            return values.keySet().toArray(new String[0]);
        }

        @Override
        protected String[] childrenNamesSpi() {
            return children.keySet().toArray(new String[0]);
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            MemoryPreferences child = children.get(name);
            if (child == null) {
                child = new MemoryPreferences(this, name);
                children.put(name, child);
            }
            return child;
        }

        @Override
        protected void syncSpi() {
        }

        @Override
        protected void flushSpi() {
        }
    }
}
