package org.karthu.fx.form.path;

import org.karthu.fx.form.PathMode;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The rules of the path input, independent of JavaFX: which text is a path, where the dialog starts, which file name
 * saving suggests, how a chosen path is entered into the field and which directory a choice leaves in the memory,
 * which drop the field accepts and what a paste inserts.
 *
 * <p>This package is not exported: the rules are pinned down by their own tests, and the path input
 * is their only caller.
 */
public final class PathRules {

    private PathRules() {
    }

    /**
     * Returns the field's text as a path: empty when the text is null, empty, or not a valid path on this platform.
     * The text is taken as it is, never trimmed or resolved.
     *
     * @param text the text in the field, may be null
     * @return the text as a path, or empty
     */
    public static Optional<Path> parse(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(text));
        } catch (InvalidPathException invalid) {
            return Optional.empty();
        }
    }

    /**
     * Returns the directory a dialog starts in, the first of these that gives one:
     *
     * <ol>
     *   <li>The folder of the path in the field. The path counts when it is valid and not empty; a relative path is
     *       resolved against the base directory, and skipped without one. In the open and save modes its folder is
     *       the path itself if it is an existing directory, else its parent; in the directory mode it is the path
     *       itself. A folder that does not exist is replaced by its nearest existing ancestor.</li>
     *   <li>The memory's last directory, or its nearest existing ancestor when it no longer exists.</li>
     *   <li>The base directory, if it exists.</li>
     *   <li>The home directory.</li>
     * </ol>
     *
     * <p>A relative base directory or last directory counts as none. A step of which nothing exists, not even the
     * root, is skipped.
     *
     * @param fieldText     the text in the field, may be null
     * @param mode          the field's mode
     * @param baseDirectory the field's base directory, or null for none
     * @param lastDirectory the memory's last directory, or null for none
     * @param home          the home directory, the last resort
     * @return the directory to start the dialog in
     */
    public static Path startDirectory(String fieldText, PathMode mode, Path baseDirectory, Path lastDirectory,
                                      Path home) {
        Path base = effectiveBase(baseDirectory);
        Optional<Path> fromField = fieldDirectory(fieldText, mode, base);
        if (fromField.isPresent()) {
            return fromField.get();
        }
        if (lastDirectory != null && lastDirectory.isAbsolute()) {
            Optional<Path> fromMemory = nearestExistingDirectory(lastDirectory.normalize());
            if (fromMemory.isPresent()) {
                return fromMemory.get();
            }
        }
        if (base != null && Files.isDirectory(base)) {
            return base;
        }
        return home;
    }

    /**
     * Returns the folder of the path in the field, or its nearest existing ancestor; empty when the field gives no
     * absolute path or nothing of it exists.
     */
    private static Optional<Path> fieldDirectory(String fieldText, PathMode mode, Path base) {
        Optional<Path> resolved = resolveField(fieldText, base);
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        Path path = resolved.get();
        Path folder = path;
        if (mode != PathMode.DIRECTORY && !Files.isDirectory(path) && path.getParent() != null) {
            folder = path.getParent();
        }
        return nearestExistingDirectory(folder);
    }

    /**
     * Returns the file name the save dialog suggests: in the save mode the file name of the path in the field, if
     * there is one. There is none in the other modes, for an empty or invalid field, for a root, {@code .} or
     * {@code ..}, and when the path names an existing directory (a relative one resolved against the base directory),
     * which is the start folder rather than a file.
     *
     * @param fieldText     the text in the field, may be null
     * @param mode          the field's mode
     * @param baseDirectory the field's base directory, or null for none
     * @return the file name to suggest, or null for none
     */
    public static String initialFileName(String fieldText, PathMode mode, Path baseDirectory) {
        if (mode != PathMode.SAVE) {
            return null;
        }
        Optional<Path> parsed = parse(fieldText);
        if (parsed.isEmpty()) {
            return null;
        }
        Path fileName = parsed.get().normalize().getFileName();
        if (fileName == null || fileName.toString().isEmpty() || fileName.toString().equals("..")) {
            return null;
        }
        Optional<Path> resolved = resolveField(fieldText, effectiveBase(baseDirectory));
        if (resolved.isPresent() && Files.isDirectory(resolved.get())) {
            return null;
        }
        return fileName.toString();
    }

    /**
     * Returns the path in the field absolute and normalised: an absolute path as it is, a relative one resolved
     * against the base directory; empty when the text is no path, or relative without a base directory.
     */
    private static Optional<Path> resolveField(String fieldText, Path base) {
        Optional<Path> parsed = parse(fieldText);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        Path path = parsed.get();
        if (!path.isAbsolute()) {
            if (base == null) {
                return Optional.empty();
            }
            path = base.resolve(path);
            // On Windows a path such as "D:data" is relative yet keeps its own drive when resolved: still relative.
            if (!path.isAbsolute()) {
                return Optional.empty();
            }
        }
        return Optional.of(path.normalize());
    }

    /**
     * Returns the path itself if it is an existing directory, else its nearest ancestor that is one; empty when none
     * is.
     *
     * @param path an absolute path
     * @return the nearest existing directory, or empty
     */
    public static Optional<Path> nearestExistingDirectory(Path path) {
        Path current = path;
        while (current != null) {
            if (Files.isDirectory(current)) {
                return Optional.of(current);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    /**
     * Returns the text a chosen path is entered as. The path is normalised. Below the base directory it is entered
     * relative to it, its names joined with {@code /} on every platform, or as {@code .} for the base itself;
     * otherwise, or without a base directory, absolute, as the platform writes it.
     *
     * @param chosen        the path chosen in the dialog
     * @param baseDirectory the field's base directory, or null for none
     * @return the field's new text
     */
    public static String choiceText(Path chosen, Path baseDirectory) {
        Path absolute = chosen.toAbsolutePath().normalize();
        Path base = effectiveBase(baseDirectory);
        if (base == null || !absolute.startsWith(base)) {
            return absolute.toString();
        }
        if (absolute.getNameCount() == base.getNameCount()) {
            return ".";
        }
        StringBuilder text = new StringBuilder();
        for (Path name : base.relativize(absolute)) {
            if (!text.isEmpty()) {
                text.append('/');
            }
            text.append(name);
        }
        return text.toString();
    }

    /**
     * Returns the directory a choice leaves in the directory memory, absolute and normalised: a directory itself, a
     * file its folder. The file system decides when the chosen path exists, whatever the mode. A path that does not
     * exist yet (a new file in the save mode) follows the mode: in the directory mode the path itself, in the open and
     * save modes its folder. Only a choice is remembered (from the dialog or a drop), never typed or pasted text.
     *
     * @param chosen the chosen path
     * @param mode   the field's mode
     * @return the directory to remember, empty for a missing root chosen as a file
     */
    public static Optional<Path> rememberedDirectory(Path chosen, PathMode mode) {
        Path absolute = chosen.toAbsolutePath().normalize();
        if (Files.isDirectory(absolute)) {
            return Optional.of(absolute);
        }
        if (mode == PathMode.DIRECTORY && !Files.exists(absolute)) {
            return Optional.of(absolute);
        }
        return Optional.ofNullable(absolute.getParent());
    }

    // --- Drop and paste ------------------------------------------------------------------------------

    /**
     * Returns whether the field accepts what is dragged onto it: exactly one file system entry, in the directory mode
     * an existing directory, in the open and save modes an existing regular file. A drop always brings an existing
     * entry, so the save mode takes no file name that does not exist yet. The field's filters do not apply, and the
     * field's own state (disabled, bound, read-only) is the field's to check.
     *
     * @param entries the dragged file system entries, may be null
     * @param mode    the field's mode
     * @return true when the field accepts the drop
     */
    public static boolean acceptsDrop(List<Path> entries, PathMode mode) {
        if (entries == null || entries.size() != 1 || entries.getFirst() == null) {
            return false;
        }
        Path entry = entries.getFirst();
        if (mode == PathMode.DIRECTORY) {
            return Files.isDirectory(entry);
        }
        return Files.isRegularFile(entry);
    }

    /**
     * Returns what a paste inserts into the field. If the pasted text, ignoring the whitespace around it, is wrapped
     * in one pair of double quotes, as the Windows Explorer's <i>Copy as path</i> gives a path, it is what is inside
     * the quotes, whitespace inside them kept. Any other text is pasted unchanged, with its whitespace; a text with
     * a further quote inside, such as two quoted paths, is no single quoted path.
     *
     * @param pasted the pasted text, never null
     * @return the text to insert
     */
    public static String pastedText(String pasted) {
        String stripped = pasted.strip();
        if (stripped.length() < 2 || stripped.charAt(0) != '"' || stripped.charAt(stripped.length() - 1) != '"') {
            return pasted;
        }
        String inside = stripped.substring(1, stripped.length() - 1);
        if (inside.indexOf('"') >= 0) {
            return pasted;
        }
        return inside;
    }

    /** The base directory normalised, or null when there is none: a relative path counts as none. */
    private static Path effectiveBase(Path baseDirectory) {
        if (baseDirectory == null || !baseDirectory.isAbsolute()) {
            return null;
        }
        return baseDirectory.normalize();
    }
}
