/**
 * Karthu FX: a JavaFX component library with its own theme in two colour schemes, Nebula Light and Nebula Dark (see
 * <a href="https://github.com/KarthuOrg/public-karthu-fx/blob/main/STYLEGUIDE.md">STYLEGUIDE.md</a>).
 *
 * <p>It provides theming ({@link org.karthu.fx.theme.ThemeManager}, in the scheme an application chooses as an
 * {@link org.karthu.fx.theme.Appearance}), configuration persistence
 * ({@link org.karthu.fx.config.ConfigService}), themed dialogs ({@link org.karthu.fx.dialog.CustomDialog}),
 * a settings dialog ({@link org.karthu.fx.settings.SettingsDialog}), custom controls
 * ({@link org.karthu.fx.components.ToggleSwitch}, {@link org.karthu.fx.components.TwinColumnSelector},
 * {@link org.karthu.fx.components.KeyValueEditor}), a
 * Markdown view ({@link org.karthu.fx.markdown.MarkdownView}), which renders Markdown with standard JavaFX nodes,
 * and form inputs that look and behave alike: the contract ({@link org.karthu.fx.form.FormInput},
 * {@link org.karthu.fx.form.FieldBinding}), the shell every input sits in ({@link org.karthu.fx.form.FieldShell},
 * {@link org.karthu.fx.form.FieldFrame}), the inputs ({@link org.karthu.fx.form.TextInput},
 * {@link org.karthu.fx.form.PasswordInput}, {@link org.karthu.fx.form.NumberInput},
 * {@link org.karthu.fx.form.ComboInput}, {@link org.karthu.fx.form.EditableComboInput},
 * {@link org.karthu.fx.form.TextAreaInput}, {@link org.karthu.fx.form.PathInput},
 * {@link org.karthu.fx.form.CronInput} with its {@link org.karthu.fx.form.CronFormat}), the row of a form
 * ({@link org.karthu.fx.form.FormField}) and, in the same shell, a search field
 * ({@link org.karthu.fx.form.SearchInput}) and one with a popup of suggestions
 * ({@link org.karthu.fx.form.SuggestionInput}), and commands with their keys
 * ({@link org.karthu.fx.command.CommandRegistry}, {@link org.karthu.fx.command.Command},
 * {@link org.karthu.fx.command.Shortcut}), which run when their keys are pressed in a window or in a part of it
 * ({@link org.karthu.fx.command.CommandContext}) and drive the menu items and buttons bound to them, and whose keys
 * the user changes in a dialog ({@link org.karthu.fx.command.ShortcutsDialog}) or in its content placed in a window
 * of the application ({@link org.karthu.fx.command.ShortcutsEditor}), and a navigation for the edge of a window: a
 * side bar with a stripe of tabs that opens one panel at a time ({@link org.karthu.fx.sidebar.SideBar},
 * {@link org.karthu.fx.sidebar.SideBarTab}), a tree of the application's entries
 * ({@link org.karthu.fx.sidebar.NavigationTree}) and the contract of a side bar's content
 * ({@link org.karthu.fx.sidebar.SideBarContent}).
 *
 * <p>The module requires only {@code slf4j-api} for logging; the consuming application provides
 * the binding.
 */
module org.karthu.fx {
    requires javafx.controls;
    requires javafx.graphics;
    requires org.slf4j;
    // Transitive: DirectoryMemory.preferences(Preferences) takes a node of java.prefs in its exported signature.
    requires transitive java.prefs;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.bootstrapicons;
    // Transitive: NavigationTree's icon function returns an Ikon of ikonli's core in its exported signature.
    requires transitive org.kordamp.ikonli.core;

    exports org.karthu.fx.theme;
    // The theme's skins, which base.css names with -fx-skin: JavaFX's controls create them by reflection, so the
    // package is exported to javafx.controls, and to no one else. Not an API.
    exports org.karthu.fx.theme.skin to javafx.controls;
    exports org.karthu.fx.config;
    exports org.karthu.fx.services;
    exports org.karthu.fx.components;
    exports org.karthu.fx.dialog;
    exports org.karthu.fx.settings;
    exports org.karthu.fx.command;
    // The Markdown view. Its parser and document model (org.karthu.fx.markdown.parser) stay internal, so the parser
    // can be replaced without an API change, and so do the built-in lexers of its coloured code blocks
    // (org.karthu.fx.markdown.lexer): an application reaches them only through the view.
    exports org.karthu.fx.markdown;
    // Not exported: the monospace font choice (org.karthu.fx.font), shared by the Markdown view's code and the cron
    // input's expression, so that both pick the same font the same way and the choice can change without an API
    // change.
    // Form inputs: the shell, the frame, the row and the eight inputs. Their pure rules stay internal, pinned down by
    // their own tests, not by the API: the path rules (org.karthu.fx.form.path) of the path input, the number rules
    // (org.karthu.fx.form.number) of the number input, the suggestion filter (org.karthu.fx.form.suggestion) of
    // the editable combo input, the cron rules (org.karthu.fx.form.cron), which check, parse, describe and convert
    // the expressions of the cron input, and the matching rule (org.karthu.fx.form.search) of the search and
    // suggestion inputs, which decides which entries match a search and where their marks go and which the shortcut
    // editor's search shares, beside the Markdown view's rule of a search in a document's text, which shares its
    // folding, so that their parsed form, their messages, their plain text, their conversions and their folding can
    // change without an API change.
    exports org.karthu.fx.form;
    // Navigation for the edge of a window: the side bar and its tabs, the navigation tree and the contract of a side
    // bar's content.
    exports org.karthu.fx.sidebar;
}
