/**
 * Navigation for the edge of a window: the {@link org.karthu.fx.sidebar.SideBar}, a stripe of
 * {@linkplain org.karthu.fx.sidebar.SideBarTab tabs} at the window's left or right edge that opens one tab's panel at a
 * time beside the application's content and collapses it again; the {@link org.karthu.fx.sidebar.NavigationTree}, a
 * tree of the application's entries that opens an entry the way the application chooses
 * ({@link org.karthu.fx.sidebar.OpenMode}), the ready-made content of a tab or a navigation on its own; and
 * {@link org.karthu.fx.sidebar.SideBarContent}, the small contract between a panel's content and the side bar that
 * shows it: the buttons of the panel's header, the items of its menu, and where the focus goes.
 *
 * <p>Every component takes its look from the theme: apply it to the scene with
 * {@link org.karthu.fx.theme.ThemeManager#apply(javafx.scene.Scene)}. Like all JavaFX controls, they are used on the
 * JavaFX Application Thread only.
 *
 * @since 1.1.0-RC2
 */
package org.karthu.fx.sidebar;
