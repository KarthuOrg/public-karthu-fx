# Changelog

What changed in karthu-fx, version by version, the newest first. An entry says what changed for
someone who uses the library. The sections follow [Keep a Changelog](https://keepachangelog.com/):
*Added*, *Changed*, *Fixed*, *Removed*.

## [Unreleased]

### Fixed

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
