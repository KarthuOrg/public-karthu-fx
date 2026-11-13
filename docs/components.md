# What karthu-fx provides

Everything in `karthu-fx-core` (JPMS module `org.karthu.fx`), package by package. The API itself is
documented in the JavaDoc; the style classes, pseudo-classes and colour tokens are listed in
[`STYLEGUIDE.md`](../STYLEGUIDE.md). How to get, build and run it: [`getting-started.md`](getting-started.md).

- [Theming](#theming)
- [Configuration](#configuration)
- [Dialogs](#dialogs)
- [Settings](#settings)
- [Controls](#controls)
- [Markdown view](#markdown-view)
- [Form fields](#form-fields)
- [Icons](#icons)

## Theming

The library ships a single theme, **Nebula Dark** (see [`STYLEGUIDE.md`](../STYLEGUIDE.md)), as two
stylesheets loaded in order: `base.css` (structure/density) and `dark.css` (color tokens).

Apply it with the static `ThemeManager.apply(scene)`. Call it for every `Scene`, including dialogs
and secondary windows, before the scene is shown (the theme gives scroll panes a skin of its own,
which must be their first). The CSS lives module-internal at `/org/karthu/fx/theme/css/`; always
apply it through `ThemeManager`, never by resolving the path yourself.

## Configuration

`ConfigService` persists window state and locale via the Java Preferences API; `AppConfig` and
`WindowState` are the immutable config records.

## Dialogs

`CustomDialog`: themed replacements for `Alert`/`TextInputDialog` with stage-icon inheritance
(info, warning, error, confirm, input + builder API).

## Settings

`SettingsDialog` with a hierarchical model (`SettingCategory`, `SettingGroup`, `SettingEntry`). It
stores nothing: an application reads the values from the entries, or all at once with
`SettingsModel.snapshot()`, and keeps them wherever it keeps its data.

## Controls

`ToggleSwitch`, `TwinColumnSelector`, `KeyValueEditor` (with `KeyValueEntry`) and the
`BaseComponent` base class.

## Markdown view

`MarkdownView` (package `org.karthu.fx.markdown`, from 1.1.0) shows a Markdown text as a
scrollable, themed document of standard JavaFX nodes, without `WebView`.

- **What it renders.** Headings, paragraphs, bold, italic, inline code, nested lists, block quotes,
  fenced code blocks with a copy button, links and bare URLs (a click goes to the application's
  handler), thematic breaks and GFM tables. HTML blocks and indented code show as their source
  lines.
- **Selecting and copying.** The reader selects text with the mouse across blocks as in a browser
  and copies it with Ctrl+C as plain text, code blocks and whole tables included.
- **Tables.** GFM tables show as `TableView`s that share the view's width, show all their rows and
  sort in natural order, with columns the reader moves, resizes, shows and hides, and cells the
  reader selects as in a spreadsheet and copies as tab-separated text (Ctrl+C, or the context menu
  with or without the column titles). A table too wide for the view scrolls sideways.
- **Coloured code.** A code block whose fence names its language is coloured by a lexer: Java,
  JSON, YAML, SQL, XML and HTML, Bash and Markdown are built in, and an application adds, replaces
  or switches off languages per view through `getCodeLanguages()` and its own `CodeLexer`.
  Keywords, strings, comments, types and the other pieces each take the colour of one of 23
  categories (`CodeCategory`), nine base ones and fourteen fine ones that fall back to them, every
  colour a theme token (`-fx-code-keyword-color` and so on) that an application changes with CSS at
  runtime, and every piece a `code-` style class. A block longer than
  `codeHighlightLineLimitProperty()` (500 lines by default) stays in one colour, so a very long
  block cannot make the view slow.
- **Zoom.** The whole document zooms from 50 to 200 % as a browser zooms a page, the text wrapping
  to the view's width at every zoom and the place the reader is reading kept in view: a zoom bar on
  top of the view (minus and plus to the next of the presets 50, 75, 100, 125, 150 and 200 %, a
  percent field that takes a typed value and opens a list of the presets, a slider, *Reset*), Ctrl
  and the mouse wheel, and Ctrl+Plus, Ctrl+Minus and Ctrl+0. At 100 % the view looks exactly as
  without a zoom. The application sets and reads the zoom as a factor, 1.0 being 100 %
  (`zoomProperty()`, limited to 0.5 to 2.0 in whole percent, kept when the text changes); the bar
  shows by default, and `zoomBarVisibleProperty()` hides it, which also takes zooming from the
  reader.
- **Images.** Images show as pictures (PNG, JPEG, GIF, also animated, and BMP), never larger than
  they were on the screen they were taken on and shrunk to the text's width and the visible height
  by the view's own area average, so a 4K screenshot keeps its letters clean. The view loads them
  in the background through an `ImageLoader` the application may keep, wrap or replace. The view's
  own loader reads files, archive entries and `data:` addresses, relative ones against
  `imageBaseProperty()`, and images from the internet only while `remoteImagesProperty()` is on. A
  missing image or one in another format shows a box with its alt text, and a button, a click or
  the context menu opens any image in a window at full size that zooms, copies the image and saves
  its file.
- **Texts.** The labels of the copy button and the context menu (*Copy*, *Copy with header*,
  *Select column*, *Copy Markdown*), the zoom bar's texts (*Zoom out*, *Zoom in*, *Zoom levels*,
  *Reset*) and the texts of images (*Open image*, the window's buttons, the boxes' notes) are
  properties with English defaults.
- **Styling.** Its style classes (a table is `markdown-table`, the zoom bar `markdown-zoom-bar`,
  coloured code `code-keyword` and its siblings, a block image `markdown-image-block`) and tokens
  are listed in [`STYLEGUIDE.md`](../STYLEGUIDE.md).
- **In the showcase.** The *Markdown* page is a live editor with a *Zoom bar* switch, a read-out of
  the zoom, a block of every built-in language, images of every kind with the switches *Play
  animations* and *Images from the internet*, and a *Code colours* card whose CSS recolours the view
  as it is typed.

## Form fields

Package `org.karthu.fx.form` (from 1.1.0), a collection of standardized form inputs that look and
behave alike: text, password, number (with spinner arrows), combo, editable combo, text area, path
and cron, each in the same shell (same height, same frame, the same icons in the same places, the
same error, bound, read-only and disabled looks), and the form row that holds any of them. No new
dependency.

### The contract and the shell

- `FormInput`, the interface every form input implements: its node (and the node in it that takes
  the focus, which a row's label names), its value, its `FieldBinding`, read-only (the value can be
  selected and copied, not changed), its `FieldState` (`NORMAL`, `WARNING`, `ERROR`) and the
  messages it reports itself (`getInputMessages()`, by default none). Disabled is the node's own
  `disable`.
- `FieldBinding`, an input's binding to a variable, which karthu-fx knows by name only: bindable,
  the bound name (the application listens to it), the `BindingCandidate`s (a name and a detail,
  such as the type), missing, and its texts. An input in a shell shows a `{}` icon (or Ctrl+Space)
  that opens the list of candidates, and the bound variable as a chip with `×` in place of its
  value, which it keeps and shows again after unbinding. An input without a shell (a switch, a
  list) will show the `{}` icon at the right end of the row's label line and the chip in place of
  the input; that variant comes with the first component that needs it.
- `FieldShell`, the abstract shell of every input type: one border around the JavaFX control the
  type wraps, its trailing icons and the type's own controls (a divider, a chevron, arrows), with
  the focus, warning, error, drop target, read-only and disabled looks and the binding's `{}` icon
  and chip. A field type sets its content and hooks into read-only, binding and layout, and writes
  the value the user gives through one guarded path, which leaves a value property the application
  bound alone.
- `FieldFrame`, the single-line text frame: the shell around one borderless text field, exactly as
  high as a text field.
- `FormField`, the row of a form: the label above the input with a required asterisk and the
  description as its tooltip, the messages under it as `FieldMessage`s of a `MessageSeverity`
  (error, warning, info), the input's own first, then the application's, which together set the
  input's state. Label and messages can be selected and copied, and each message has its whole
  text as tooltip. While the input is disabled, the row dims its label, asterisk and messages with
  it (`:input-disabled`). It only displays; the application validates.

### The inputs

- `TextInput`, a single line of text, such as a name or a title, in the shell: a text field with
  the `{}` icon, the chip and the error look; its value is the text exactly as typed, its prompt
  text a property.
- `PasswordInput`, a field for a secret: the text stays masked, and where the application allows
  it, an eye inside the field reveals it to check it; leaving the field masks it again. A masked
  text never reaches the clipboard (copy and cut do nothing, by shortcut and context menu);
  revealed, it behaves like a text field.
- `NumberInput`, a field for a number, right-aligned: what the user types stays as typed, also
  when it is no number, so the application can report it; `toNumber()` reads the text as a
  `BigDecimal` (a dot, no grouping, an exponent as in `1.0E21`), empty for no number, and while
  whole numbers are on only a sign and digits count. Minimum, maximum and step are properties that
  never limit typing. With its arrows on it is a spinner: stacked up and down arrows behind a
  divider and the Up and Down keys step the value within the range, a held arrow repeats, the
  mouse wheel never steps.
- `ComboInput`, a choice from a list: values of any type, each shown with its display text (a
  function, by default `toString()`), so codes read as labels. A click anywhere on the field, or
  Alt+Down, F4 or Space, opens the list; a chevron shows it is a choice. An optional choice starts
  its list with an empty entry, *(none)*, which sets no value, and a value that is not in the list
  is still shown.
- `EditableComboInput`, free text with suggestions: the user types freely, and typing opens the
  list with the suggestions that contain the text, ignoring case; a click or Enter on one makes it
  the text, Escape closes the list and keeps what was typed, and no suggestion is ever filled in
  by itself. A divider and a chevron, or Alt+Down, open the full list.
- `TextAreaInput`, several lines whose text wraps, four rows high by default, with the `{}` icon
  at the top right. The user drags a grip at the bottom right to make it taller or wider (never
  wider than its parent gives, also when restored or when the parent narrows, never lower than two
  rows) and double-clicks it to go back to the default size; the dragged size is a property the
  application may store and restore. Tab moves on to the next field, Ctrl+Tab inserts a tab.
- `PathInput`, a field for choosing a file or a directory: a browse icon inside the field (a file
  icon, in the directory mode a folder icon), or Alt+Down, opens the dialog in the field's
  `PathMode` (open, save, directory), starting in the folder of the path in the field or the last
  used one (a `DirectoryMemory`, which an application keeps across restarts on its own Preferences
  node, or in its own store), and enters the choice, relative with `/` when it lies below the
  field's base directory. The dialog is a replaceable `PathChooser`, its file types `PathFilter`s
  with *All files* added. A file dragged onto the field is entered like a choice, a path pasted
  with the quotes of the Windows Explorer's *Copy as path* loses them, and a long path shows its
  file name.
- `CronInput`, a schedule as a Unix or Quartz cron expression (`CronFormat`), checked by rules of
  its own, without a library: the expression in a monospace font (the same font choice as the
  Markdown view's code), a small format selector inside the field (or the format as plain text
  where the application fixes it), and under the field, as the input's own message, what the
  expression means in plain English (*At 09:00, Monday to Friday*) or, once the user leaves the
  field or presses Enter, the one message of the first broken rule (*Hour must be 0 to 23, found
  24*). The value is the text as typed; the problem and the description are read-only properties
  an application reads at any time. The placeholder names the format's fields. The user's switch
  of the format converts a valid expression when nothing is lost, its weekdays written as names
  (`0 9 * * 1-5` becomes `0 0 9 ? * MON-FRI`); anything else stays as typed and is checked against
  the new format. A format the application sets never converts: it checks the text again and shows
  an error at once, so a saved expression loads the same whether the value or the format comes
  first.

### Search fields in the same look

- `SearchInput`, a search field in the look of the form fields, on the frame but no form input
  (no value in the form sense, no label row, no binding): a magnifier on the left, the placeholder
  *Search*, a clear button (×) on the right while there is text, Escape to empty it. An entry
  matches when every word typed occurs in one of its texts, ignoring case and accents (*zurich*
  finds *Zürich*). One call, `filter(FilteredList, Function)`, filters an application's list as
  the user types and keeps a counter such as *1 of 2* in the field, in the warning colour when
  nothing matches; `highlight(String)` hands out a text building block for the application's own
  rows that marks every match and follows the search by itself.
- `SuggestionInput<T>`, the search field with a popup of suggestions: the application's own
  objects as entries, with the text that goes into the field and the texts that are searched.
  Typing opens the popup under the field, as wide as the field, each suggestion one line with the
  typed parts bold (a line too long for the popup ends in an ellipsis), the first preselected; the
  arrows, Page Up and Down, Home and End move, Enter or a click applies, Escape closes and keeps
  the text, Down in the empty field shows every entry, and *No matches* keeps the popup open. The
  chosen entry is an event and a property, null again as soon as the text is edited. An
  application draws richer rows with its own cell factory, such as cards of people with the marks
  in every line, and turns on a footer with the count (*2 of 14 people*) and the key hints; no row
  is ever wider than the popup.

### Texts, styling and the showcase

Every text the components show is a property with an English default, except the cron input's:
its plain text and its messages are fixed English (the rules are internal, so a language can come
later without an API change), and its format names come from `CronFormat`. Its placeholders are
properties like the other texts.

The style classes, pseudo-classes and tokens are listed in [`STYLEGUIDE.md`](../STYLEGUIDE.md)
(*Form fields*).

The showcase's group *Form Inputs* starts with an *Overview* of every input type by state, then
one page per type (*Text Input*, *Password Input*, *Number Input*, *Combo Input*, *Editable Combo
Input*, *Text Area Input*, *Path Input*, *Cron Input*, *Search Input*) with every state and a field
to try out; the *Search Input* page also filters a list and shows person cards built from the
public API.

## Icons

Ikonli (`ikonli-javafx` + Bootstrap Icons pack) is used throughout.
