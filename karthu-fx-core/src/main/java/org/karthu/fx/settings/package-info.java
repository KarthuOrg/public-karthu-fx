/**
 * A settings dialog and its model. An application describes its settings as a tree of
 * {@linkplain org.karthu.fx.settings.SettingCategory categories}, each a page of
 * {@linkplain org.karthu.fx.settings.SettingGroup groups} of
 * {@linkplain org.karthu.fx.settings.SettingEntry entries}, whose {@link org.karthu.fx.settings.SettingType} decides
 * the control that edits them. The {@link org.karthu.fx.settings.SettingsModel} holds the tree, the
 * {@link org.karthu.fx.settings.SettingsDialog} shows and edits it and reports how it was closed
 * ({@link org.karthu.fx.settings.SettingsDialogResult}).
 *
 * <p>The package stores nothing: an application reads the values from the entries, or all at once with
 * {@link org.karthu.fx.settings.SettingsModel#snapshot()}, and keeps them wherever it keeps its data.
 */
package org.karthu.fx.settings;
