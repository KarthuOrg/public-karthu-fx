# Changelog

What changed in karthu-fx, version by version, the newest first. An entry says what changed for
someone who uses the library. The sections follow [Keep a Changelog](https://keepachangelog.com/):
*Added*, *Changed*, *Fixed*, *Removed*.

## [Unreleased]

### Added

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
  conflicts and those without a key. *Apply* hands the changes to the registry, which reports them
  once, *Cancel* and closing the window drop them. The same content is a node of its own,
  `ShortcutsEditor`, with `apply()`, `discard()`, `hasChangesProperty()` and
  `hasConflictsProperty()`, for a settings page in the application's own window. Every text of both
  is a property with an English default; the style classes and pseudo-classes are listed in
  STYLEGUIDE.md.
  The showcase's *Shortcuts* page is a small example application to try all of it: 30 commands
  with a menu bar, a toolbar, a document and a note field with keys of their own, read-outs of the
  last command and of the kept keys, the dialog and the editor. `docs/components.md` describes the
  package, and the agent guide has its rules and a tested sample.

### Fixed

- `SettingsDialog` no longer makes JavaFX log "Loop detected" warnings for its CSS: three rules of the
  theme set the background colour to a lookup of itself. The dialog looks as before.
- `MarkdownView`: a document with many tables no longer keeps the application busy with endless
  layout passes; it comes to rest after it is laid out.

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
