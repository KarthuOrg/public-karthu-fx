/**
 * The Markdown view: {@link org.karthu.fx.markdown.MarkdownView} shows a Markdown text as a scrollable, themed
 * document of standard JavaFX nodes, without a {@code WebView}.
 *
 * <p>An application sets the text and decides what the view cannot know: where a link leads, and where images come
 * from ({@link org.karthu.fx.markdown.ImageLoader}). Code blocks are coloured by lexers: the view knows several
 * languages from the start, and an application adds its own with a {@link org.karthu.fx.markdown.CodeLexer}, which
 * cuts code into {@linkplain org.karthu.fx.markdown.CodePiece pieces} of a
 * {@linkplain org.karthu.fx.markdown.CodeCategory category} each.
 *
 * <p>The parser, its document model and the built-in lexers are internal to the module: an application reaches them
 * only through the view.
 */
package org.karthu.fx.markdown;
