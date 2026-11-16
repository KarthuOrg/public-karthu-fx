# Changelog

What changed in karthu-fx, version by version, the newest first. An entry says what changed for
someone who uses the library. The sections follow [Keep a Changelog](https://keepachangelog.com/):
*Added*, *Changed*, *Fixed*, *Removed*.

## [Unreleased]

### Added

- `MarkdownView` searches its text: an application sets the search text, reads the number of
  matches and the current match, goes to the next and the previous match in a `SearchDirection`
  and clears the search. Every match is marked, the current one apart, and the view scrolls to it.
  Tables are not searched yet.
- `MarkdownView.updateMarkdown` sets a new version of the shown text and keeps the reading
  position: the paragraph at the top stays there, also when text above it was added or removed,
  and a search goes on from its current match. Images keep showing their pictures until their
  files have been read again, so nothing flashes or jumps. `setMarkdown` still starts at the top.

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
