package org.karthu.fx.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The rule of the shortcut editor that finds where keys clash in its working copy. A <b>conflict</b> is one key on two
 * or more commands of the same context, fixed commands included: only one of them could run on the key. An
 * <b>override</b> is a key of a part of the window that a command of an enclosing context holds too: while the focus
 * is in the part, its command runs on the key, elsewhere the enclosing one. The window's context encloses every other,
 * and a part encloses another when its node is an ancestor of the other's node at the time the rule runs.
 *
 * <p>Keys clash when they are equal ({@link Shortcut#equals(Object)}). A key and a character can run on the same press
 * on some keyboard layouts without being equal, such as Ctrl, Shift and the {@code 1} key and <i>Ctrl++</i> on a Swiss
 * keyboard. So can two characters, since a character runs with Shift or without: <i>Ctrl+=</i> and <i>Ctrl++</i> both
 * run on Ctrl, Shift and the {@code =} key of a US keyboard, <i>Ctrl++</i> and <i>Ctrl+*</i> on Ctrl, Shift and the
 * {@code +} key of a German one, where the command added first takes the press. Which presses those are depends on
 * the layout, so such keys are not found.
 */
final class ShortcutClashes {

    private ShortcutClashes() {
    }

    /**
     * Finds the clashes of the rows' keys in the working copy and sets them on every row.
     *
     * @param entries  every command's row
     * @param registry the registry of the commands, which knows the contexts around each context
     * @return the number of conflicts: of keys that two or more commands of one context hold
     */
    static int find(List<CommandRow> entries, CommandRegistry registry) {
        Map<Slot, List<CommandRow>> holders = new HashMap<>();
        for (CommandRow entry : entries) {
            Shortcut shortcut = entry.getShortcut();
            if (shortcut != null) {
                Slot slot = new Slot(entry.getCommand().getContext(), shortcut);
                List<CommandRow> holding = holders.get(slot);
                if (holding == null) {
                    holding = new ArrayList<>();
                    holders.put(slot, holding);
                }
                holding.add(entry);
            }
        }
        int conflicts = 0;
        for (List<CommandRow> holding : holders.values()) {
            if (holding.size() > 1) {
                conflicts++;
            }
        }
        Map<CommandContext, List<CommandContext>> enclosing = new HashMap<>();
        for (CommandContext context : registry.getContexts()) {
            enclosing.put(context, enclosingContexts(context, registry));
        }
        for (CommandRow entry : entries) {
            List<CommandRow> others = new ArrayList<>();
            List<CommandRow> overridden = new ArrayList<>();
            Shortcut shortcut = entry.getShortcut();
            CommandContext context = entry.getCommand().getContext();
            if (shortcut != null) {
                for (CommandRow holder : holders.get(new Slot(context, shortcut))) {
                    if (holder != entry) {
                        others.add(holder);
                    }
                }
                for (CommandContext outer : enclosing.get(context)) {
                    List<CommandRow> holding = holders.get(new Slot(outer, shortcut));
                    if (holding != null) {
                        overridden.addAll(holding);
                    }
                }
            }
            entry.setClashes(others, overridden);
        }
        return conflicts;
    }

    /**
     * The contexts around a context, the innermost first: those that hold the focus on its node's parent, as the
     * registry asks them for a key, the window's last. The window's context has none around it.
     */
    private static List<CommandContext> enclosingContexts(CommandContext context, CommandRegistry registry) {
        if (context == registry.getWindowContext()) {
            return List.of();
        }
        return registry.contextsHolding(context.getNode().getParent());
    }

    /**
     * A key in a context: the place where only one command can run on the key. A context is equal only to itself.
     *
     * @param context  the context
     * @param shortcut the key
     */
    private record Slot(CommandContext context, Shortcut shortcut) {
    }
}
