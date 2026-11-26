# Changelog

What changed in karthu-fx, version by version, the newest first. An entry says what changed for
someone who uses the library. The sections follow [Keep a Changelog](https://keepachangelog.com/):
*Added*, *Changed*, *Fixed*, *Removed*.

## [Unreleased]

### Added

- A side bar and a navigation tree, in the new package `org.karthu.fx.sidebar`: panels at the
  edge of a window that the user opens one at a time and collapses again, like the tool window bar
  of an IDE, and a searchable tree of the application's entries as their ready-made content.

  `SideBar` sits at the left or the right edge of a window (`side`, `LEFT` by default, `RIGHT`
  mirrored, any other side refused), placed beside the application's content like any node. Its
  stripe shows one flat button per visible `SideBarTab`, the tab's Ikonli icon with its title as
  tooltip and accessible text. A click opens the tab's panel beside the content, which becomes
  narrower and is never covered, and puts the focus into the tab's content; a click on another tab
  switches, a click on the open tab collapses the side bar to its stripe and the next expands it
  again, at once, without animation. When the panel hides with the focus in it, the focus goes
  back to where it last was outside the side bar. A tab has a stable id, a title, an icon, any
  content node, a visible flag, and its own header buttons and menu items; the application adds,
  removes and hides tabs at any time, and removing or hiding the open tab collapses the side bar
  and selects no tab. The panel's header row shows the title, the tab's buttons, the content's
  buttons, a three-dot menu button and a hide button. The menu (`getMenu()`), which the menu
  button and a right click on the header or a stripe button open, lists the tab's items, the
  content's, a separator and *Hide*. The panel has one width for every tab (`contentWidth`,
  260 px by default), which the user drags at the resize edge on the panel's inner side, between
  `minContentWidth` (160 px) and `maxContentWidth` (600 px); a drag below the smallest width
  stops there and never collapses, and a width the application sets is kept within the limits.
  The side bar stores nothing: `contentWidth`, `selectedTab` and `collapsed` are properties the
  application keeps and sets again on its next start.

  `registerCommands(registry, group, idPrefix)` hands the side bar the application's
  `CommandRegistry`: per tab a command *Show* and its title (`<prefix>.show.<tab id>`) opens the
  tab with the focus in its content, or collapses the side bar when the tab is open, on Alt+1 to
  Alt+9 for the first nine tabs; *Hide Side Bar* (`<prefix>.hide`) collapses it on Shift+Escape
  while the focus is in it, in a text field of the panel too; *Search in Side Bar*
  (`<prefix>.search`), without a key, expands it and focuses the selected tab's search. A tab
  added later gets its command, a removed or hidden tab's is disabled. A second side bar
  registers with `registerCommands(registry, group, idPrefix, false)`: its tabs' keys are left to
  the user, while its *Hide Side Bar* keeps Shift+Escape, which holds only in its own side bar.
  The stripe's tooltips name a tab's key ("Files (Alt+1)"), the hide button's the key of *Hide
  Side Bar* ("Hide (Shift+Escape)"), both following every change of the key.

  `NavigationTree` is the ready-made content of a tab, or a navigation on its own in any layout:
  the application's own `TreeItem`s under a hidden root, each entry with an Ikonli icon and a text
  from the application's functions. The new enum `OpenMode` chooses whether a double click or
  Enter opens an entry (`DOUBLE_CLICK`, the default) or the user's change of the selection does
  (`SELECTION`); the open handler receives the entry's value. A double click or Enter (a single
  click while selection opens) also folds or unfolds a folder, the disclosure arrow always does.
  The tree offers the selected value (read-only), `select(value)`, `expandAll()` and
  `collapseAll()`, a placeholder for an empty tree and its inner `TreeView`. A `SearchInput` above
  the tree (`getSearchInput()`) narrows it to the entries that match by karthu-fx's search rule
  and the folders above them, all unfolded, a matching folder keeping all its entries; the cells
  and the open handler still receive the application's values. The default cell marks the
  matching characters, an application's own cells use the field's `highlight`, and the field
  counts the hits out of all entries ("3 of 43"); "No matches" (`noMatchesText`) shows when
  nothing matches. A search text function lets the search find an entry by a text it does not
  show. Emptying the field gives back the tree folded exactly as the user left it, the selected
  entry unfolded to and scrolled to. Ctrl+F inside the tree focuses the field, Arrow down moves
  to the first hit, Enter opens it, Escape clears.

  The new interface `SideBarContent` is what a panel's content tells the side bar: its header
  buttons, its menu items, where the focus goes when its tab opens and how to focus its search. A
  navigation tree's header buttons are *Expand All* and *Collapse All*; any other node is shown as
  it is, with the tab's own buttons and items. Every text is a property with an English default:
  *More*, *Hide*, the tooltips of *Expand All* and *Collapse All*, *No matches*, and the names of
  the commands and of their context (`showTabCommandFormat`, `hideCommandName`,
  `searchCommandName`, `commandContextName`). The theme draws the stripe on the surface with the
  open tab's icon in the accent colour, and the tree without border or own background, with the
  rows of the showcase's navigation. STYLEGUIDE.md lists the style classes and the rule for the
  panel's header: actions first, view options next, settings last, and only buttons more than 5 %
  of the users need. The module now requires `org.kordamp.ikonli.core` transitively, as a tab's
  icon and the tree's icon function are its `Ikon`.

  The showcase's *Side Bar* page, in the group *Components*, is an example window with a side bar
  of three tabs: a project's files in a navigation tree that opens a file on a double click or
  Enter, a guide in one that opens a page as soon as it is selected, and notes, a list with a
  header button and a menu item of its tab's own. A switch puts the side bar on the left or the
  right, and read-outs name what was opened and the state an application keeps.
  `docs/components.md` describes the package, and the agent guide has its rules and tested
  samples.
- `MarkdownView` colours PowerShell: a code block of the language `powershell`, `pwsh` or `ps1`
  shows comments (also `<# ... #>`), strings and here-strings, variables (`$env:PATH`, `${...}`,
  splatted `@args`), `$true`, `$false` and `$null` as literals, keywords in any case, the
  operators written as a dash and a word (`-eq`, `-match`, `-join`, with their `c` and `i`
  prefixes), a command's parameters such as `-Recurse` as attributes, types in brackets
  (`[string]`, `[int[]]`, generic ones), attributes such as `[CmdletBinding()]` as annotations,
  commands in the form verb-noun and methods as functions, and numbers.

- An application adds its own entries to `MarkdownView`'s context menu: `setContextMenuCallback`
  (with `contextMenuCallbackProperty()` and `getContextMenuCallback()`) takes the new functional
  interface `ContextMenuCallback`, called on every opening of the menu after the view's own entries
  are in place, with the menu's entries, to add to, remove from or reorder, and the new record
  `ContextMenuPlace`, whose `sourceLine()` is the line of the Markdown text under the right click
  (a block, list item, table row or code line, as `topSourceLine()` counts them), empty where the
  click hit no block, as in the margins. Without a callback the menu is as before.
- `MarkdownView` tells which line of the Markdown text the reader is looking at, for an
  application that opens the file in an editor there: `topSourceLine()`, the line of the first
  block whose top is in the visible area, and `searchMatchSourceLine()`, the line of the current
  search match, both an `OptionalInt` counted from 1 and empty without a document or a match. A
  list item, a table row (the header row too) and a code line have their own line, any other
  block its first; `\n`, `\r\n` and `\r` each end one line. Both follow the zoom, the margins,
  a sorted table and `updateMarkdown`.
- A path setting in `SettingsDialog`: the new type `SettingType.PATH`, made with
  `SettingEntry.ofPath(...)` (with and without a description), names its `PathMode` and its
  `PathFilter`s, read back with `getPathMode()` and `getPathFilters()`. The dialog shows it with a
  `PathInput` and its *Browse* icon, in the entry's mode, with its filters (their descriptions
  resolved like the labels) and the shared directory memory; it takes the stored path, saves the
  changed one and puts it back on *Cancel*, like every other setting. The value is the path's text,
  the empty text no path. The showcase's *Settings Dialog* page has a *Tools* category with a
  program and a folder.
- Nebula Light colours code after the default colours of Eclipse's light editors: keywords a
  wine red, strings a blue, comments a green, Javadoc a slate blue, fields and constants a deep
  blue, annotations a grey, XML tags, attributes and brackets in Eclipse's own colours, and
  numbers, types, method names, variables and plain code black. The new token
  `-fx-code-block-text-color` colours the plain code of a code block, in Nebula Dark the code
  colour as before, so inline code keeps its deep teal in light. Nebula Dark keeps every colour.
- Nebula Light, a light colour scheme beside Nebula Dark, with the same shapes, sizes and spacing:
  a cool grey chrome, white fields, dark text and a deep blue focus, for every component and
  every themed standard control, the Markdown view's coloured code and search marks included.
  A menu of commands (the menus of the menu bar, context menus) is the grey of its menu bar, as
  it takes the surface colour in both schemes. The list of a field (a combo box's, a choice
  box's, the suggestions of a search field, the calendar) is white like the field.
  Nebula Dark keeps every colour.
- `ThemeManager` changes the scheme while the application runs. `setAppearance(Appearance)` takes
  the new enum `Appearance`: `LIGHT`, `DARK` or `SYSTEM`, which follows the light or dark setting
  of the operating system, also when it changes while the application runs, and is Dark where
  the system reports none; `getAppearance()` and `appearanceProperty()` read the choice. Without
  a choice the scheme is Dark, so an application that chooses nothing looks as before. A change
  restyles at once every scene the theme was applied to, shown or not, with the open popups,
  menus and tooltips of its windows and karthu-fx's own dialogs, and replaces only the scheme's
  stylesheet, so the application's own stylesheets keep their places. `colorSchemeProperty()` and
  `getColorScheme()` give the scheme in effect as JavaFX's `ColorScheme`, System resolved, for
  colours an application sets in code. `apply(Scene)` keeps its contract.
- The pseudo-classes `light` and `dark` on the root of every themed scene, the scheme in effect,
  also on a root that replaces it: an application's stylesheet gives a colour of its own a value
  per scheme under `.root:light` and `.root:dark`.
- Colour tokens for the places Nebula Light tells apart, each with a value in both schemes and in
  Nebula Dark the colour its places had: `-fx-field-color` (every editable surface),
  `-fx-raised-color` (the Markdown view's content), `-fx-text-placeholder-color`,
  `-fx-text-disabled-color`, `-fx-accent-tint-text-color`, `-fx-row-divider-color`,
  `-fx-selection-neutral-color`, `-fx-shadow-color` and `-fx-shadow-strong-color`, the hover and
  pressed fills of the status buttons (`-fx-status-normal-hover`, `-fx-status-normal-pressed` and
  their siblings for running, paused, error and info), `-fx-danger-border`,
  `-fx-danger-border-hover`, `-fx-danger-text-hover-color`, `-fx-error-border`,
  `-fx-keycap-border` and `-fx-keycap-color`.
- Links and tooltips take colours of their own: `-fx-link-color` (the Markdown view's links and
  images in table cells, hyperlinks, the shortcuts editor's fix) and `-fx-tooltip-color`,
  `-fx-tooltip-text-color`, `-fx-tooltip-base-color` and `-fx-tooltip-shadow-color` (a tooltip's
  background, its text, the colour its content derives its text colours from, and its shadow). In
  Nebula Dark links keep the accent and tooltips JavaFX's own colours.
- STYLEGUIDE.md lists every colour token with its value in both schemes, the contrast noted for
  Nebula Light and the token of the colour design it follows. The agent guide shows how to choose
  the scheme and how to give an application's own colours a value per scheme.
- The showcase has a *Light* / *Dark* / *System* switch in its toolbar, which starts in Dark, and
  its *Code colours* card holds the colours of the scheme in effect.
- `MarkdownView` searches its text: an application sets the search text, reads the number of
  matches and the current match, goes to the next and the previous match in a `SearchDirection`
  and clears the search. Every match is marked, the current one apart, and the view scrolls to it.
  Tables are searched too, header and cells in the order they show, and the search follows the
  reader's sort and moved, hidden or shown columns.
- `MarkdownView.updateMarkdown` sets a new version of the shown text and keeps the reading
  position: the paragraph at the top stays there, also when text above it was added or removed,
  and a search goes on from its current match. Images keep showing their pictures until their
  files have been read again, so nothing flashes or jumps. `setMarkdown` still starts at the top.
- `SearchInput` searches a `MarkdownView` with `search(view)`: the field's text is the view's search,
  the counter shows the current match and the number of matches (*3 of 12*), Enter and Shift+Enter
  go to the next and the previous match, and two arrows in the field go up and down through the
  matches, their texts translatable with `previousMatchTextProperty()` and `nextMatchTextProperty()`.
  A field searches a view or filters a list, one at a time.
- `SuggestionInput` shows a chevron at its right end with `setChevronVisible(true)`, so that it works
  as a filterable combo box for the mouse too: a click opens the list as Down does (every entry in
  an empty field, the matches with text), a click on the open list's chevron closes it. Greyed out
  while the field is disabled or read-only, out of the tab order, its tooltip translatable with
  `chevronTextProperty()`. Off by default: existing fields look as before.
- `MarkdownView` takes a left and a right margin, `leftMarginProperty()` and `rightMarginProperty()`,
  in pixels at 100 % that the zoom scales. Unset, the theme's padding of `markdown-document` holds
  as before. A view narrower than both margins and 300 pixels of text shrinks them in their
  proportion, so the text keeps 300 pixels and nothing scrolls sideways.
- `MarkdownView` shows GFM task list items (`- [ ] open`, `- [x] done`, `1. [X] done`) as check
  boxes, in place of a bullet or beside a number, at any depth and inside quotes: open or ticked in
  the theme's check box colours, read only (a click changes nothing). The item's text starts after
  the marker, copying keeps the state (`- [ ] open`, `1. [x] done`), and the search finds the text,
  never the brackets. A check box names its state as its tooltip and accessible text, *Open* or
  *Done*, translatable with `taskOpenTextProperty()` and `taskDoneTextProperty()`. Other forms such
  as `[/]` stay text, as on GitHub. Style classes `markdown-task-list`, `markdown-task-item` and
  `markdown-task-box`, with `:checked` while a task is done.
- Commands and their keys, in the new package `org.karthu.fx.command`: an application creates a
  `CommandRegistry` for its window's scene and adds its groups and its commands, each `Command` with
  an id, a translatable name, a group, a default key and an action. A key is a `Shortcut`: a key
  with Ctrl, Alt and Shift (*Ctrl+Shift+S*, *F5*), or a punctuation character with Ctrl and Alt
  (*Ctrl++*, `Shortcut.of('+', CTRL)`), which runs on whichever key types the character on the
  user's keyboard layout, with Shift or without, and on the numeric keypad's key for it: *Ctrl++*
  runs on every plus key of a Swiss, German or US keyboard, so an application catches no variants
  of its own. A digit key runs on the keypad's digit too (*Ctrl+0*). A command runs when its key
  is pressed anywhere in the window, never while it is disabled, and a focused text field keeps its
  own editing keys (Ctrl+C, X, V, A, Z). A menu item or a button bound to a command with `bind`
  takes its action and its disabled state, and a menu item shows the command's key as its
  accelerator. The display text of a command's key is a property (*Ctrl+S*, empty without a key),
  so tooltips and prompts that name the key are built from it.

  A `CommandContext` names a part of the window, such as a document area or a canvas, bound to a
  node: its commands run while the focus is in that node, and there they win over the window's
  commands on the same key. Contexts nest as their nodes do, the innermost asked first, and a
  disabled command leaves its key to the next one outward. A menu item of such a command shows its
  key, which runs the command only inside its context. Fixed commands document the keys a component
  owns (*Copy* on Ctrl+C in a document): their keys never change, and without an action they leave
  the key to the component. While a text input has the focus, no command runs on Ctrl+Alt with a
  key that types a character, so typing `@`, `{` or `|` with AltGr never runs a command.

  The user's keys outlive a restart without the library touching a file: the registry gives out
  the keys that differ from their defaults as a map from command id to a text
  (`file.save=Ctrl+Shift+S`, an empty text for a removed key), reports every change through
  `changedShortcutsProperty()`, takes such a map back with `setChangedShortcuts`, where an entry it
  cannot use is skipped and logged while the others apply, and puts every key back with
  `resetShortcuts()`. Only changes are kept, so a new default reaches every user who left that key
  alone. A key's text form (`Shortcut.toText()`, `Shortcut.parse`) is the same in every language:
  the modifiers in the order Ctrl, Alt, Shift, then JavaFX's name of the key or the character
  (*Ctrl+Page Up*, *Ctrl+Plus* for the key, *Ctrl++* for the character). `Shortcut.isRecordable()`
  says whether a user may choose a key: F1 to F12 as they are, every other key only with Ctrl or
  Alt. Two defaults on one key in one context are logged as a warning.

  The user changes the keys in a `ShortcutsDialog` (`new ShortcutsDialog(owner, registry)
  .showAndWait()`): every command in its group, with its key as key caps, *Not assigned* without
  one, and a small mark with the context's name for a key that holds only in a part of the window;
  groups show their counts and fold with a click. Enter, a double-click or the pencil records a new
  key: the field shows the modifiers held down, and the first key the user may record becomes the
  command's key, punctuation by its character (*Ctrl++* whichever key types the plus). A refused
  key says why while recording goes on, and Esc, a click elsewhere or leaving the field cancel.
  The ×, Delete or Backspace remove a key; a key that differs from its default carries a dot and a
  reset button. A key on two commands of the same context, fixed ones included, marks every row
  involved in red with *Also used by …* and a one-click fix that takes the key from the other
  commands (never from a fixed one), and *Apply* stays disabled until no conflict is left; clashing
  defaults of the application show as a conflict as soon as the dialog opens. A key of a part of the
  window that a command of the window, or of a part around it, holds too is no conflict: the row
  says which command it overrides there. A counter shows the commands, the modified ones, the
  conflicts and those without a key. A search field finds a command by its name, its group, its
  context or its key (*ctrl+0* finds both commands of a conflict on Ctrl+0), ignoring case and
  accents as `SearchInput` does, and opens the groups with matches while it searches. The filters
  *All*, *Modified* and *Conflicts* show every command, those whose keys differ from their defaults
  (also keys set in an earlier session) or those in a conflict, and *Reset all* puts every key back
  to its default. *Apply* hands the changes to the registry, which reports them once, *Cancel* and
  closing the window drop them. The same content is a node of its own, `ShortcutsEditor`, with
  `apply()`, `discard()`, `hasChangesProperty()` and `hasConflictsProperty()`, for a settings page
  in the application's own window. Every text of both is a property with an English default; the
  style classes and pseudo-classes are listed in STYLEGUIDE.md.

  The showcase's *Shortcuts* page is a small example application to try all of it: 30 commands
  with a menu bar, a toolbar, a document and a note field with keys of their own, read-outs of the
  last command and of the kept keys, the dialog and the editor. `docs/components.md` describes the
  package, and the agent guide has its rules and a tested sample.

### Changed

- The showcase's navigation is a tree of three sections: *JavaFX* with the standard controls in
  the groups *Inputs*, *Selection*, *Data Display*, *Layout* and *Navigation*, *karthu-fx* with
  its own components in *Form Inputs*, *Components* and *Dialogs*, and *Examples* with the *Job
  Scheduler*; a page that would be alone in a group sits directly under its section. A click on a
  section or group expands or collapses it and leaves the open page open. A search field above the
  tree, karthu-fx's `SearchInput`, narrows it to the pages whose name, group or section matches,
  by karthu-fx's search rule, and shows *No matches* when none does. Ctrl+F puts the focus into
  the search, Esc clears it, Arrow down moves to the first hit and Enter opens it, also on a page
  row. Every page looks and works as before.
- The theme has a third stylesheet, `light.css`, beside `base.css` and `dark.css`. A native image
  that names the resources it includes one by one names `light.css` too, or the theme has no
  colours to load for Light.

### Fixed

- `SettingsDialog` no longer makes JavaFX log "Loop detected" warnings for its CSS: three rules of the
  theme set the background colour to a lookup of itself. The dialog looks as before.
- `MarkdownView`: a document with many tables no longer keeps the application busy with endless
  layout passes; it comes to rest after it is laid out.
- A selected `ToggleButton` no longer shows a strip of its colour below its border after it lost
  the focus, and no longer bleeds slightly around it while focused: the theme's toggle buttons
  paint their background exactly inside the border, in both schemes. No colour changes.

## [1.1.0-RC1] - 2026-11-14

The first version published to Maven Central.

### Added

- The theme Nebula Dark for the standard JavaFX controls, applied with `ThemeManager.apply(scene)`.
- Form inputs that look and behave alike: `TextInput`, `PasswordInput`, `NumberInput`,
  `ComboInput`, `EditableComboInput`, `TextAreaInput`, `PathInput` and `CronInput`, the form row
  `FormField` with its messages, and the binding of an input to a variable.
- `SearchInput`, which filters a list as the user types, and `SuggestionInput`, which proposes
  entries in a popup.
- `MarkdownView`: Markdown as themed JavaFX nodes, with GFM tables, coloured code blocks, images,
  text selection and zoom.
- `CustomDialog` for messages, questions and text prompts, and `SettingsDialog` with its model.
- The controls `ToggleSwitch`, `TwinColumnSelector` and `KeyValueEditor`.
- `ConfigService`, which keeps the window's place and the locale between runs.
- The showcase application, with a page for every component.
- A guide for coding agents that build with the library in another project
  (`docs/agent-guide.md`).
