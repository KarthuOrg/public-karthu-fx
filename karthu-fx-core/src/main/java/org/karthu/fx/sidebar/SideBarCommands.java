package org.karthu.fx.sidebar;

import javafx.beans.binding.Bindings;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.karthu.fx.command.Command;
import org.karthu.fx.command.CommandContext;
import org.karthu.fx.command.CommandGroup;
import org.karthu.fx.command.CommandRegistry;
import org.karthu.fx.command.Shortcut;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The commands of a side bar in an application's registry: one per tab id, which shows that tab, Hide Side Bar in a
 * context of the side bar's own node, and the search of the selected tab. The registry removes no command, so a tab
 * that leaves the side bar or is hidden keeps its command, disabled, and gets it back when it returns.
 */
final class SideBarCommands {

    /** The digit keys of the suggested keys Alt+1 to Alt+9, in stripe order. */
    private static final List<KeyCode> DIGITS = List.of(KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
            KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9);

    private final SideBar sideBar;
    private final CommandRegistry registry;
    private final CommandGroup group;
    private final String idPrefix;
    private final boolean suggestedKeys;
    /** The command of each tab id, in the order they were added. */
    private final Map<String, Command> tabCommands = new LinkedHashMap<>();
    /** The tab each tab command is named after: the first tab of the side bar with its id. */
    private final Map<Command, SideBarTab> namedAfter = new IdentityHashMap<>();
    private final Command hide;
    private final Command search;
    /**
     * A menu item bound to Hide Side Bar and never shown: its accelerator is the command's current key as JavaFX's
     * combination, which matches a key press as the registry does, and firing it runs the command unless it is
     * disabled. The registry offers no other public way to either.
     */
    private final MenuItem hideKey = new MenuItem();

    /**
     * Registers a side bar's commands: a command per tab in the order of the tabs, then Hide Side Bar in a context of
     * the side bar's node, then the search. Every id is checked before the first is added, so a refused registration
     * leaves the registry as it was.
     */
    SideBarCommands(SideBar sideBar, CommandRegistry registry, CommandGroup group, String idPrefix,
                    boolean suggestedKeys) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(idPrefix, "idPrefix");
        if (idPrefix.isBlank()) {
            throw new IllegalArgumentException("the id prefix of a side bar's commands is blank");
        }
        this.sideBar = sideBar;
        this.registry = registry;
        this.group = group;
        this.idPrefix = idPrefix;
        this.suggestedKeys = suggestedKeys;
        checkFree(sideBar.getTabs());

        CommandContext context = registry.addContext(idPrefix, sideBar.getCommandContextName(), sideBar);
        context.nameProperty().bind(sideBar.commandContextNameProperty());
        addTabCommands(sideBar.getTabs());
        hide =registry.addCommand(group, context, idPrefix + ".hide", sideBar.getHideCommandName(),
                suggestedKeys ? Shortcut.of(KeyCode.ESCAPE, Shortcut.Modifier.SHIFT) : null, sideBar::hide);
        hide.nameProperty().bind(sideBar.hideCommandNameProperty());
        hide.bind(hideKey);
        sideBar.addEventFilter(KeyEvent.KEY_PRESSED, this::hideKeyPressed);
        search = registry.addCommand(group, idPrefix + ".search", sideBar.getSearchCommandName(), null,
                sideBar::searchSelectedTab);
        search.nameProperty().bind(sideBar.searchCommandNameProperty());
    }

    /**
     * Runs Hide Side Bar on its key pressed in a text input of the side bar, such as a navigation tree's search field,
     * before the input has it. A text input consumes every key press without Ctrl or Alt but the function keys, though
     * it has no use for Shift+Escape, so the registry, which hears a key after the focused control, would never hear
     * it there. A key with Ctrl or Alt reaches the registry anyway, and follows its rules.
     */
    private void hideKeyPressed(KeyEvent event) {
        KeyCombination key = hideKey.getAccelerator();
        if (key == null || hideKey.isDisable() || !(event.getTarget() instanceof TextInputControl)
                || event.isControlDown() || event.isAltDown() || event.isMetaDown()
                || event.getCode().isFunctionKey()) {
            return;
        }
        if (key.match(event)) {
            hideKey.fire();
            event.consume();
        }
    }

    /**
     * Refuses a registration whose group is not the registry's, or whose context or command ids the registry has
     * already, before anything is added.
     */
    private void checkFree(List<SideBarTab> tabs) {
        if (!registry.getGroups().contains(group)) {
            throw new IllegalArgumentException("the group " + group.getId() + " is not one of the registry's");
        }
        for (CommandContext context : registry.getContexts()) {
            if (context.getId().equals(idPrefix)) {
                throw new IllegalArgumentException("the registry has a context with the id " + idPrefix + " already");
            }
            if (context.getNode() == sideBar) {
                throw new IllegalArgumentException("the side bar is the context " + context.getId() + "'s node");
            }
        }
        Set<String> ids = new LinkedHashSet<>();
        for (SideBarTab tab : tabs) {
            ids.add(tabCommandId(tab.getId()));
        }
        ids.add(idPrefix + ".hide");
        ids.add(idPrefix + ".search");
        for (Command command : registry.getCommands()) {
            if (ids.contains(command.getId())) {
                throw new IllegalArgumentException("the registry has a command with the id " + command.getId()
                        + " already");
            }
        }
    }

    /**
     * Follows the side bar's tabs: a tab with a new id gets its command, each command is named after the first tab
     * with its id and is enabled while such a tab is visible, and the search is enabled while there is a selected tab
     * to search.
     *
     * @param tabs      the side bar's tabs
     * @param canSearch whether the side bar has a selected tab among its visible tabs
     */
    void update(List<SideBarTab> tabs, boolean canSearch) {
        addTabCommands(tabs);
        Map<String, SideBarTab> firstById = new HashMap<>();
        Set<String> visibleIds = new HashSet<>();
        for (SideBarTab tab : tabs) {
            firstById.putIfAbsent(tab.getId(), tab);
            if (tab.isVisible()) {
                visibleIds.add(tab.getId());
            }
        }
        for (Map.Entry<String, Command> entry : tabCommands.entrySet()) {
            Command command = entry.getValue();
            SideBarTab tab = firstById.get(entry.getKey());
            if (tab != null && namedAfter.get(command) != tab) {
                nameAfter(command, tab);
            }
            command.setDisabled(!visibleIds.contains(entry.getKey()));
        }
        search.setDisabled(!canSearch);
    }

    /** Adds a command for each tab whose id has none yet, in the order of the tabs. */
    private void addTabCommands(List<SideBarTab> tabs) {
        for (SideBarTab tab : tabs) {
            if (!tabCommands.containsKey(tab.getId())) {
                addTabCommand(tab);
            }
        }
    }

    /**
     * Returns the command of a tab id.
     *
     * @param tabId the tab's id
     * @return the command, null while the side bar has none for that id
     */
    Command tabCommand(String tabId) {
        return tabCommands.get(tabId);
    }

    /** Adds the command of a tab, on the first of Alt+1 to Alt+9 no other tab command of the side bar has. */
    private void addTabCommand(SideBarTab tab) {
        String tabId = tab.getId();
        Command command = registry.addCommand(group, tabCommandId(tabId), tabId, suggestedKeys ? freeDigit() : null,
                () -> sideBar.showTab(tabId));
        tabCommands.put(tabId, command);
        nameAfter(command, tab);
    }

    /** Binds a tab command's name to the side bar's format and the tab's title, the tab's id while it has none. */
    private void nameAfter(Command command, SideBarTab tab) {
        namedAfter.put(command, tab);
        command.nameProperty().bind(Bindings.createStringBinding(() -> {
            String title = tab.getTitle();
            String shown = title == null || title.isBlank() ? tab.getId() : title;
            String format = sideBar.getShowTabCommandFormat();
            return format == null ? "" : format.replace("{0}", shown);
        }, sideBar.showTabCommandFormatProperty(), tab.titleProperty()));
    }

    /** The first of Alt+1 to Alt+9 that no tab command of the side bar has as its default; null when all have one. */
    private Shortcut freeDigit() {
        for (KeyCode digit : DIGITS) {
            Shortcut key = Shortcut.of(digit, Shortcut.Modifier.ALT);
            boolean taken = false;
            for (Command command : tabCommands.values()) {
                if (key.equals(command.getDefaultShortcut())) {
                    taken = true;
                }
            }
            if (!taken) {
                return key;
            }
        }
        return null;
    }

    /** The id of a tab's command: the prefix, {@code show} and the tab's id. */
    private String tabCommandId(String tabId) {
        return idPrefix + ".show." + tabId;
    }
}
