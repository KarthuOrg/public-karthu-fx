# Using karthu-fx in another project: a guide for coding agents

This page is written for an AI coding agent (and reads just as well for a person) that is asked to
build a JavaFX screen with karthu-fx components in **another** project. It says how to set the
library up, which rules to keep, which component to take for what, and shows a working sample for
each. Every sample on this page is compiled and run by the build
(`AgentGuideSamplesTest` in `karthu-fx-showcase`), so the code is what the library accepts.

Status: karthu-fx is under development (see the [README](../README.md)). An API may change from
one version to the next; this page describes the version of the repository it lies in.

## 1. Setup

Requirements: JDK 25, JavaFX 25, Maven.

```xml
<dependency>
    <groupId>org.karthu</groupId>
    <artifactId>karthu-fx-core</artifactId>
    <version>1.1.0-RC1</version>
</dependency>
```

Take the newest published version. The dependency brings JavaFX (`javafx-controls`,
`javafx-graphics`), Ikonli with the Bootstrap Icons pack and `slf4j-api`. It brings no logging
backend: the application adds its own SLF4J binding.

In a modular application, `module-info.java` needs:

```java
requires org.karthu.fx;
requires javafx.controls;
```

Only these packages are API. Anything else in the jar is internal and not exported:

| Package | Holds |
|---|---|
| `org.karthu.fx.theme` | `ThemeManager`, `StageIcons` |
| `org.karthu.fx.form` | form inputs, the form row, search fields |
| `org.karthu.fx.markdown` | `MarkdownView` and what an application plugs into it |
| `org.karthu.fx.dialog` | `CustomDialog` |
| `org.karthu.fx.settings` | settings dialog and its model |
| `org.karthu.fx.components` | `ToggleSwitch`, `TwinColumnSelector`, `KeyValueEditor` |
| `org.karthu.fx.config` | `ConfigService`, `AppConfig`, `WindowState` |
| `org.karthu.fx.services` | `BaseService` |

## 2. Rules

Keep these; most mistakes come from breaking one of them.

1. **Apply the theme to every `Scene`, before it is shown:** `ThemeManager.apply(scene)`. That
   includes every window and dialog the application builds itself. The library's own dialogs
   (`CustomDialog`, `SettingsDialog`) apply it themselves.
2. **Never load or copy the stylesheets yourself.** They are internal to the module; the only way
   in is `ThemeManager`.
3. **Do not hardcode colours or sizes.** Use the theme's tokens (`-fx-accent-color` and the
   others) and style classes, all listed in [`STYLEGUIDE.md`](../STYLEGUIDE.md). The style classes
   and pseudo-classes a component documents are a contract and safe to style against.
4. **Use the standard JavaFX controls as they are.** `Button`, `TableView`, `TabPane` and the
   rest are themed. Do not wrap or restyle them to "match"; they already do.
5. **In a form, use the form inputs, each in a `FormField`.** Do not mix raw `TextField`s into a
   form built from `FormField`s: heights, frames and states would differ.
6. **The library never validates.** The application checks the values and sets the messages of
   the row (`FormField.getMessages()`); the row then colours the input. The one exception is
   `CronInput`, which checks its expression and reports about it itself.
7. **A form input's value is what the user typed.** `NumberInput` keeps a text that is no number;
   read the number with `toNumber()`. `PathInput` keeps the text; read the path with `toPath()`.
8. **The library stores nothing**, except `ConfigService` (window place and locale, Java
   Preferences) and a `DirectoryMemory` the application sets up. Settings, form values and the
   Markdown text are the application's to keep.
9. **Every text a component shows is a property with an English default.** For another language,
   set the properties (`setPromptText`, `setAddText`, `setCopyText` and so on).
10. **JavaFX Application Thread only**, as with every JavaFX control.
11. **Create the components in code.** The showcase builds them in its controllers; that is the
    tested way.
12. **Search a Markdown view through a `SearchInput`** connected with `search(view)`, in the
    application's own bar beside the view. The field and the view's search text then stay equal,
    and the field counts the matches and goes through them; do not bind the view's search text
    yourself as well. The view takes no keys for its search: Ctrl+F and F3 are the application's
    to add, if it wants them.
13. **A new version of the shown Markdown text goes through `updateMarkdown`.** `setMarkdown`, and
    a binding of `markdownProperty()`, show another document, from the top. `updateMarkdown` keeps
    the reader's place and the pictures, as wanted when a changed file is read again; it throws
    while the text property is bound.

## 3. Which component for what

| Need | Take |
|---|---|
| One line of text | `TextInput` |
| A password or other secret | `PasswordInput` |
| A number, optionally with spinner arrows | `NumberInput` |
| One of a fixed list of values | `ComboInput<T>` |
| Free text with suggestions | `EditableComboInput` |
| Several lines of text | `TextAreaInput` |
| A file or a directory | `PathInput` |
| A schedule as a cron expression | `CronInput` |
| Label, required mark, description and messages around any of the above | `FormField` |
| A field that filters a list as the user types | `SearchInput` |
| A field that proposes entries in a popup | `SuggestionInput<T>` |
| A field that searches the text of a `MarkdownView` | `SearchInput`, connected with `search(view)` |
| A value that comes from a variable instead of being typed | the input's `FieldBinding` |
| Rendered Markdown (help texts, release notes, documentation) | `MarkdownView` |
| A message, a question, a one-line text prompt | `CustomDialog` |
| A settings window with categories | `SettingsDialog` with a `SettingsModel` |
| On/off | `ToggleSwitch` |
| Choosing and ordering several items out of many | `TwinColumnSelector<T>` |
| A list of key-value pairs, such as HTTP headers | `KeyValueEditor` |
| Window position and size kept between runs | `ConfigService`, `WindowState` |
| The window's icon | `StageIcons.setIcon(stage, image)` |

## 4. Samples

Imports are left out; every type comes from one of the packages of section 1 or from JavaFX.

### Start of an application

```java
Scene scene = new Scene(new VBox(), 1000, 700);
ThemeManager.apply(scene);
config.getConfig().windowState().applyTo(stage);
stage.setScene(scene);
stage.setOnCloseRequest(event -> {
    config.setWindowState(WindowState.fromStage(stage));
    config.shutdown();
});
stage.show();
```

`config` is a `ConfigService` the application creates once and starts with `initialize()`. Leave
it out if the window's place need not be kept.

### A form row with validation

```java
TextInput name = new TextInput();
name.setPromptText("First and last name");

FormField nameRow = new FormField("Name", name);
nameRow.setRequired(true);
nameRow.setDescription("The name printed on the invoice");

name.valueProperty().addListener((observable, oldText, newText) -> {
    if (newText == null || newText.isBlank()) {
        nameRow.getMessages().setAll(FieldMessage.error("is required"));
    } else {
        nameRow.getMessages().clear();
    }
});
```

- `setRequired(true)` only shows the asterisk. The check is the listener's.
- A message is `FieldMessage.error`, `.warning` or `.info`. The row sets the input's state from
  its messages (`FieldState.ERROR`, `WARNING`, `NORMAL`); do not set the state yourself inside a
  row.
- Put the row (`nameRow`), not the input, into the layout.
- Every input has `setReadOnly(boolean)` (the value can be copied, not changed) and the node's
  own `setDisable(boolean)`.

### The other inputs

```java
PasswordInput password = new PasswordInput();
password.setRevealAllowed(true);

NumberInput quantity = new NumberInput();
quantity.setWholeNumbers(true);
quantity.setMin(BigDecimal.ONE);
quantity.setMax(BigDecimal.valueOf(99));
quantity.setStep(BigDecimal.ONE);
quantity.setArrowsVisible(true);
quantity.setValue("12");
Optional<BigDecimal> number = quantity.toNumber();

ComboInput<CronFormat> format = new ComboInput<>();
format.getItems().addAll(CronFormat.UNIX, CronFormat.QUARTZ);
format.setDisplayText(CronFormat::displayName);
format.setValue(CronFormat.UNIX);

EditableComboInput encoding = new EditableComboInput();
encoding.getSuggestions().addAll("UTF-8", "UTF-16", "ISO-8859-1");
encoding.setValue("UTF-8");

TextAreaInput notes = new TextAreaInput();
notes.setPrefRowCount(6);

VBox form = new VBox(8,
        new FormField("Password", password),
        new FormField("Quantity", quantity),
        new FormField("Format", format),
        new FormField("Encoding", encoding),
        new FormField("Notes", notes));
```

- `NumberInput`: minimum, maximum and step never stop the user from typing. `toNumber()` is empty
  when the text is no number; report that with a message.
- `ComboInput<T>` takes values of any type; `setDisplayText` turns a value into its label.
  `setEmptyAllowed(true)` adds an entry for "no value".
- `PasswordInput`: the eye shows only when `setRevealAllowed(true)`.

### File or directory

```java
PathInput file = new PathInput();
file.setMode(PathMode.OPEN);
file.getFilters().add(new PathFilter("CSV files", List.of("*.csv")));
file.setBaseDirectory(Path.of("data"));
file.setValue("orders/2026.csv");
Optional<Path> path = file.toPath();
```

`PathMode` is `OPEN`, `SAVE` or `DIRECTORY`. With a base directory, a chosen path below it is
entered relative. `DirectoryMemory.setShared(DirectoryMemory.preferences(node))` makes every path
input start in the directory used last, also after a restart.

### Cron expression

```java
CronInput schedule = new CronInput();
schedule.setFormat(CronFormat.UNIX);
schedule.setValue("0 9 * * 1-5");
String meaning = schedule.getDescription();
String problem = schedule.getProblem();
```

The input checks the expression itself and shows the meaning or the problem under the field.
`getDescription()` is empty for an invalid text, `getProblem()` is empty for a valid one.
`setFormatSelectable(false)` fixes the format.

### Binding an input to a variable

```java
TextInput customer = new TextInput();
customer.getBinding().setBindable(true);
customer.getBinding().getCandidates().setAll(
        new BindingCandidate("customer.name", "Text"),
        new BindingCandidate("customer.city", "Text"));
List<String> boundNames = new ArrayList<>();
customer.getBinding().boundNameProperty().addListener(
        (observable, oldName, newName) -> boundNames.add(newName));
```

A bindable input shows a `{}` icon that lets the user pick one of the candidates in place of a
typed value. karthu-fx knows a variable by its name only: what the name means, and its value at
run time, are the application's. Use this only when the application has such variables.

### Search

```java
ObservableList<String> cities = FXCollections.observableArrayList("Basel", "Bern", "Zürich");
FilteredList<String> shown = new FilteredList<>(cities);
ListView<String> list = new ListView<>(shown);
SearchInput search = new SearchInput();
search.filter(shown, city -> city);

SuggestionInput<String> city = new SuggestionInput<>();
city.getItems().setAll(cities);
city.setItemText(item -> item);
city.setSearchTexts(item -> List.of(item));
List<String> chosen = new ArrayList<>();
city.setOnItemChosen(chosen::add);
city.setChevronVisible(true);
city.setChevronText("Show all cities");
```

- `SearchInput.filter` sets the list's predicate from then on: do not set a predicate of your own
  on the same `FilteredList`. Matching ignores case and accents.
- `SearchInput` and `SuggestionInput` are no form inputs: they have a text, not a value, and do
  not go into a `FormField`.
- `SuggestionInput<T>`: `setItemText` is what goes into the field, `setSearchTexts` what is
  searched, `setOnItemChosen` what happens on Enter or a click.
- `setChevronVisible(true)` adds a chevron that opens and closes the list with the mouse, for a
  field that serves as a filterable combo box; off by default. `setChevronText` is its tooltip.
- A `SearchInput` searches a Markdown view instead of a list with `search(view)`, see
  *Search in a Markdown view* below. A field filters a list or searches a view, one at a time.

### Markdown

```java
MarkdownView view = new MarkdownView("# Release notes\n\nSee [the guide](https://example.com/guide).");
List<String> opened = new ArrayList<>();
view.setOnLinkAction(opened::add);
view.setImageBase(Path.of("docs").toUri().toString());
view.setRemoteImages(false);
view.getCodeLanguages().put("todo", code -> {
    List<CodePiece> pieces = new ArrayList<>();
    int at = code.indexOf("TODO");
    while (at >= 0) {
        pieces.add(new CodePiece(at, at + 4, CodeCategory.KEYWORD));
        at = code.indexOf("TODO", at + 4);
    }
    return pieces;
});
view.setZoom(1.25);
view.setLeftMargin(48);
view.setRightMargin(48);
view.updateMarkdown("# Release notes\n\nNow with search.\n\nSee [the guide](https://example.com/guide).");
```

- The view opens no link by itself: without `setOnLinkAction` a click does nothing. The handler
  gets the address as written.
- Relative image addresses resolve against `setImageBase`. Images from the internet load only
  after `setRemoteImages(true)`.
- Java, JSON, YAML, SQL, XML/HTML, Bash and Markdown are coloured without any setup. An own
  language is a `CodeLexer` in `getCodeLanguages()` under the name used after the fence.
- `setMarkdown` shows another document, from the top. `updateMarkdown` sets a new version of the
  shown one, such as a file read again after it changed, and keeps the reader's place and the
  pictures of images whose address stays. It throws while `markdownProperty()` is bound.
- The space left and right of the text is `setLeftMargin` and `setRightMargin`, in pixels at 100 %:
  the zoom scales them, and a narrow view shrinks them so the text keeps 300 pixels, so they need
  no computing against the view's width. Not a number, the default, keeps the theme's 14 pixels.
  Do not pad the view or a box around it for this.
- The view scrolls by itself: do not put it into a `ScrollPane`. Give it the room it may grow into
  (`VBox.setVgrow(view, Priority.ALWAYS)`).
- It renders a subset of CommonMark with GFM tables and GFM task lists; raw HTML shows as source.
- Task list items (`- [ ] open`, `- [x] done`) show as check boxes the reader cannot tick: the
  view never changes its text and has no event for a click on them. Their tooltip, also the
  screen reader's text, is `setTaskOpenText` and `setTaskDoneText`.

### Search in a Markdown view

```java
MarkdownView document = new MarkdownView("# Offices\n\nZürich and Bern.\n\nThe office in Zurich opens at nine.");
SearchInput searchField = new SearchInput();
searchField.search(document);
HBox.setHgrow(searchField, Priority.ALWAYS);
VBox reader = new VBox(8, new HBox(8, searchField), document);
VBox.setVgrow(document, Priority.ALWAYS);

MenuItem findNext = new MenuItem("Find next");
findNext.setAccelerator(new KeyCodeCombination(KeyCode.F3));
findNext.setOnAction(event -> document.nextSearchMatch());
CheckMenuItem upwards = new CheckMenuItem("Search upwards");
upwards.selectedProperty().addListener((observable, wasOn, isOn) ->
        document.setSearchDirection(isOn ? SearchDirection.BACKWARD : SearchDirection.FORWARD));
```

- From 1.1.0-RC2 on. `search(view)` is the one call: the field's text becomes the view's search,
  the view marks every match and scrolls to the current one, and the field counts them (*zurich*
  typed, it shows *1 of 2*), goes to the next and the previous match with Enter and Shift+Enter
  and has arrows for the mouse. `search(null)` disconnects; the view keeps its search.
- The search ignores case and accents and takes the text as one piece: *the office* finds the two
  words together, not every *the*. It finds the text of headings, paragraphs, lists, quotes, code
  blocks and tables, never link addresses, list markers or the alt text of pictures.
- For the application's own controls the view has `getSearchMatchCount()`,
  `getSearchMatchIndex()` (from 0, -1 without a match), `nextSearchMatch()`,
  `previousSearchMatch()`, `clearSearch()` and the direction, `FORWARD` by default. The field
  follows a search set or cleared on the view.
- Translate the arrows' tooltips with `setPreviousMatchText` and `setNextMatchText`, the counter
  with `setMatchCountFormat` (*{0} of {1}*, as for a list).

### Controls

```java
ToggleSwitch darkMode = new ToggleSwitch(true);
List<Boolean> switched = new ArrayList<>();
darkMode.selectedProperty().addListener((observable, wasOn, isOn) -> switched.add(isOn));

TwinColumnSelector<String> columns = new TwinColumnSelector<>();
columns.setAvailableHeader("Available");
columns.setSelectedHeader("Shown");
columns.setItems(List.of("Name", "City", "Phone"), List.of("Name"));
List<String> shownColumns = columns.getSelectedItems();

KeyValueEditor headers = new KeyValueEditor();
headers.setTitle("Headers");
headers.getEntries().add(new KeyValueEntry("Accept", "text/csv"));
List<KeyValueEntry.Snapshot> pairs = headers.snapshot();
```

- `TwinColumnSelector<T>`: for other types than `String`, set a `StringConverter` or a cell
  factory. `getSelectedItems()` is live and in the user's order.
- `KeyValueEditor`: `getEntries()` is the live list, `snapshot()` an immutable copy without empty
  rows. It marks duplicate and missing keys but never refuses input.

### Dialogs

```java
CustomDialog.info(owner, "Export", "The export is finished.");
CustomDialog.error(owner, "Export", "The export failed", "The file is in use by another program.");
boolean delete = CustomDialog.confirm(owner, "Delete", "Delete the job \"Nightly export\"?");
Optional<String> name = CustomDialog.input(owner, "Rename", "New name", "Nightly export");

CustomDialog.DialogResult answer = new CustomDialog.Builder(CustomDialog.DialogType.CONFIRMATION)
        .owner(owner)
        .title("Unsaved changes")
        .headerText("Save the changes before closing?")
        .contentText("Your changes are lost if you do not save them.")
        .yesButtonText("Save")
        .noButtonText("Discard")
        .build()
        .showAndWait();
```

Use these in place of JavaFX's `Alert` and `TextInputDialog`, which do not carry the theme. Pass
the owner window, so the dialog is centred on it and takes its icon. They block until closed.

### Settings dialog

```java
SettingEntry<Integer> fontSize = SettingEntry.ofInteger("editor.font-size", "Font size", 13, 8, 32, 1);
SettingEntry<Boolean> lineNumbers = SettingEntry.ofBoolean("editor.line-numbers", "Line numbers", true);
SettingEntry<String> density = SettingEntry.ofChoice("editor.density", "Density", "Compact",
        List.of("Compact", "Comfortable"));
SettingGroup editorGroup = new SettingGroup("Editor", List.of(fontSize, lineNumbers, density));
SettingCategory editor = new SettingCategory("editor", "Editor", List.of(editorGroup), List.of());
// No resource bundle: the label keys are shown as they are.
SettingsModel model = new SettingsModel(List.of(editor), null);

SettingsDialogResult result = new SettingsDialog.Builder()
        .owner(owner)
        .title("Settings")
        .model(model)
        .onApply(() -> store(model.snapshot()))
        .build()
        .showAndWait();
if (result == SettingsDialogResult.OK) {
    store(model.snapshot());
}
```

- `store` stands for the application's own storage: the dialog saves nothing.
  `model.snapshot()` gives every value by its key, `model.restore(map)` puts stored values back
  before the dialog opens.
- Entry kinds: `ofString`, `ofBoolean`, `ofInteger`, `ofChoice`, `ofSecret`, `ofDateTime`. Each
  has a variant with a description.
- A category's last argument is its child categories, for a tree.
- With a `ResourceBundle` as the model's second argument, labels are looked up as keys.

## 5. Where to look further

- **The JavaDoc** of each type says everything it does, including its style classes and
  keyboard handling. It ships as the `-javadoc` jar beside the library; the `-sources` jar holds
  the same text.
- **[`components.md`](components.md)** describes every component in prose.
- **[`STYLEGUIDE.md`](../STYLEGUIDE.md)** lists tokens, sizes and style classes.
- **The showcase** (`karthu-fx-showcase`, package
  `org.karthu.fx.showcase.controller.showcase`) has one controller per component with every
  state: read the controller of the component you are about to use.

## 6. Before you call it done

- Every `Scene` the application creates goes through `ThemeManager.apply`.
- No colour, font size or border is hardcoded where a token or a style class exists.
- Form inputs sit in `FormField`s; required fields and wrong values produce a `FieldMessage`.
- Numbers and paths are read with `toNumber()` and `toPath()`, and the empty case is handled.
- Nothing from a package outside section 1 is imported.
- Dialogs are `CustomDialog`s with an owner, not `Alert`s.
