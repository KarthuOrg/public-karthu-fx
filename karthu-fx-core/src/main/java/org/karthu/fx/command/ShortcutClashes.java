package org.karthu.fx.command;

import javafx.scene.Node;

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
 * keyboard; which presses those are depends on the layout, so such keys are not found.
 */
final class ShortcutClashes {

    private ShortcutClashes() {
    }

    /**
     * Finds the clashes of the rows' keys in the working copy and sets them on every row.
     *
     * @param entries  every command's row
     * @param contexts every context of the registry, the window's among them
     * @return the number of conflicts: of keys that two or more commands of one context hold
     */
    static int find(List<ShortcutEntry> entries, List<CommandContext> contexts) {
        Map<Slot, List<ShortcutEntry>> holders = new HashMap<>();
        for (ShortcutEntry entry : entries) {
            Shortcut shortcut = entry.getShortcut();
            if (shortcut != null) {
                Slot slot = new Slot(entry.getCommand().getContext(), shortcut);
                List<ShortcutEntry> holding = holders.get(slot);
                if (holding == null) {
                    holding = new ArrayList<>();
                    holders.put(slot, holding);
                }
                holding.add(entry);
            }
        }
        int conflicts = 0;
        for (List<ShortcutEntry> holding : holders.values()) {
            if (holding.size() > 1) {
                conflicts++;
            }
        }
        Map<CommandContext, List<CommandContext>> enclosing = enclosingContexts(contexts);
        for (ShortcutEntry entry : entries) {
            List<ShortcutEntry> others = new ArrayList<>();
            List<ShortcutEntry> overridden = new ArrayList<>();
            Shortcut shortcut = entry.getShortcut();
            CommandContext context = entry.getCommand().getContext();
            if (shortcut != null) {
                for (ShortcutEntry holder : holders.get(new Slot(context, shortcut))) {
                    if (holder != entry) {
                        others.add(holder);
                    }
                }
                for (CommandContext outer : enclosing.get(context)) {
                    List<ShortcutEntry> holding = holders.get(new Slot(outer, shortcut));
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
     * The contexts around each context, the innermost first: the contexts of the ancestors of its node, then the
     * window's. The window's context has none around it.
     */
    private static Map<CommandContext, List<CommandContext>> enclosingContexts(List<CommandContext> contexts) {
        Map<Node, CommandContext> byNode = new HashMap<>();
        CommandContext window = null;
        for (CommandContext context : contexts) {
            if (context.getNode() == null) {
                window = context;
            } else {
                byNode.put(context.getNode(), context);
            }
        }
        Map<CommandContext, List<CommandContext>> enclosing = new HashMap<>();
        for (CommandContext context : contexts) {
            List<CommandContext> around = new ArrayList<>();
            if (context != window) {
                for (Node node = context.getNode().getParent(); node != null; node = node.getParent()) {
                    CommandContext outer = byNode.get(node);
                    if (outer != null) {
                        around.add(outer);
                    }
                }
                around.add(window);
            }
            enclosing.put(context, around);
        }
        return enclosing;
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
