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
  with Ctrl, Alt and Shift (*Ctrl+Shift+S*, *F5*). A command runs when its key is pressed anywhere
  in the window, never while it is disabled, and a focused text field keeps its own editing keys
  (Ctrl+C, X, V, A, Z). A menu item or a button bound to a command with `bind` takes its action and
  its disabled state, and a menu item shows the command's key as its accelerator. The display text
  of a command's key is a property (*Ctrl+S*, empty without a key), so tooltips and prompts that
  name the key are built from it.

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
