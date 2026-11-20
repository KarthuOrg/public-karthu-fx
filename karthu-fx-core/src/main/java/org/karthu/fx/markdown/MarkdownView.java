package org.karthu.fx.markdown;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.karthu.fx.components.BaseComponent;
import org.karthu.fx.markdown.lexer.BuiltInLanguages;
import org.karthu.fx.markdown.parser.MarkdownParser;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Shows a Markdown text as a scrollable, themed document built from standard JavaFX nodes ({@code TextFlow},
 * {@code Text}, {@code Label}, {@code VBox}, {@code StackPane}, {@code Separator}, {@code ScrollPane},
 * {@code Button}, {@code Path}, {@code TableView}, {@code ScrollBar}, {@code ImageView}, {@code HBox}, and in its
 * zoom bar {@code TextField},
 * {@code Slider} and a {@code ListView} in a popup, in its full size window of images a {@code Stage} with a
 * {@code BorderPane}), without {@code WebView}.
 *
 * <pre>{@code
 * MarkdownView view = new MarkdownView("# Title\n\nSome **bold** text and `code`.");
 * view.markdownProperty().bind(selectedDocumentation);
 * }</pre>
 *
 * <p>The view scrolls by itself and wraps its text to its width; a new text rebuilds the whole content and scrolls
 * back to the top left, while a new version of the shown text set by {@link #updateMarkdown(String)} keeps the
 * reader's place (see <em>Text updates</em>). {@code null} and the empty text give an empty view. Its inner nodes are
 * not part of the API.
 * Its preferred size is fixed, 480 by 360 pixels plus its frame and, while it shows, the zoom bar, like a
 * {@code TextArea}'s: the content never sets it,
 * so a long paragraph does not ask the layout around the view for its width on one line. Like any JavaFX node, the
 * view is used on the JavaFX Application Thread only.
 *
 * <h2>Markdown</h2>
 *
 * <p>The view renders a subset of CommonMark: ATX headings ({@code #} to {@code ######}), paragraphs with soft and
 * hard line breaks, bullet lists ({@code -}, {@code *}, {@code +}) and ordered lists ({@code 1.}, {@code 1)},
 * numbered from the source's start number) and block quotes ({@code >}), nested in any combination up to 100 levels
 * deep (deeper, a marker stays text), fenced code blocks (three or more backticks or tildes; the first word of the info
 * string, such as {@code java}, names the code's language, see <em>Code colours</em>), thematic breaks ({@code ---},
 * {@code ***}, {@code ___}), emphasis ({@code *x*}, {@code _x_}), strong emphasis ({@code **x**}, {@code __x__}), both
 * ({@code ***x***}), code spans, backslash escapes, links, GFM tables and GFM task list items ({@code - [ ] open},
 * {@code - [x] done}, see <em>Task lists</em>). Tabs are not expanded: indentation counts spaces only. Whatever it
 * does not know stays visible as text; it never fails on any text.
 *
 * <p>Links are inline links {@code [text](address)} and {@code [text](address "title")}, whose text may hold
 * emphasis and code, autolinks {@code <https://...>} (any scheme), and bare {@code http://} and {@code https://}
 * addresses in the text, taken as written (emphasis and backslash escapes in them are not read): a bare address
 * starts after whitespace, {@code *}, {@code _}, {@code ~} or {@code (}, never after a {@code [} that no {@code ]}
 * has closed yet (as in GFM), ends before whitespace or {@code <}, and leaves out trailing {@code .}, {@code ,},
 * {@code :}, {@code ;}, {@code !}, {@code ?}, {@code *}, {@code _} and closing parentheses without a partner in it.
 * A link shows its address as a tooltip; a click hands the address to {@link #onLinkActionProperty() the link
 * handler}. Reference-style links and images, e-mail autolinks and inline HTML stay literal text, exactly as written;
 * the address of a link definition ({@code [x]: https://...}) reads as a bare address and is still a link.
 *
 * <p>An image ({@code ![alt](src "title")}) shows as a picture, see <em>Images</em>: alone in its paragraph, or alone
 * in a link alone in its paragraph ({@code [![alt](src)](address)}), as a block of its own, else inside its line, in a
 * heading too, and in a table cell as an image icon and its alt text. Its address is no link. An image inside a
 * link follows the link.
 *
 * <p>A block it does not render is shown unchanged as its source lines in a monospace <em>source block</em>, so
 * nothing disappears: an HTML block (a line starting with a tag, a closing tag or {@code <!}, up to a blank line) and
 * an indented code block (lines indented four or more spaces where no paragraph or list item takes them). Inside a
 * list item or a quote, the lines lose the item's indentation or the quote's {@code >}.
 *
 * <p>A code block keeps its lines whole: a line wider than the view scrolls sideways inside the block, which shows a
 * horizontal scroll bar for it. The mouse wheel over a block keeps scrolling the document (with the shortcut key down
 * it zooms, see <em>Zoom bar</em>); shift plus wheel, a touchpad's sideways swipe or the bar move the block. A source
 * block scrolls the same way. A tab in a code or source block advances to the next multiple of four columns.
 *
 * <h2>Images</h2>
 *
 * <p>An image alone in its paragraph is a block of its own ({@code markdown-image-block}) that shows the picture
 * ({@code markdown-image}), its title as a tooltip and its alt text as its accessible text. The application gives the
 * view its images through {@link #imageLoaderProperty() the image loader}, which returns the bytes of an image file
 * for the image's address. The view decodes PNG, JPEG, GIF and BMP files. The alt text is the plain text of the
 * image's description, as CommonMark defines it (emphasis without its markers, code without its backticks), on one
 * line: a line break in it reads as a space. The properties and texts of images are listed at the end of this section.
 *
 * <p><b>Inline.</b> Any other image, in a sentence or a heading, sits inside its line
 * ({@code markdown-image-inline}), one node of the line's {@code TextFlow}, the picture's bottom on the baseline. It
 * follows the size rule, the scaling, the loading and the reading position of a block image and shrinks to the
 * width its text wraps at. Its placeholder is a small box with the image icon, its error box a small box with the
 * missing-image icon and the alt text, the address as its tooltip.
 *
 * <p><b>Linked images.</b> An image inside a link, a block or inline, follows the link as the link's text does: a
 * hand cursor, and a click with the primary button without moving hands the link's address to
 * {@link #onLinkActionProperty() the link handler}, from the picture, the placeholder or the error box alike. A
 * press followed by a drag follows nothing. Without a title of its own the picture shows the link's address as its
 * tooltip.
 *
 * <p><b>Tables.</b> An image in a table cell, header or body, is never a picture and never loaded to show it: it shows
 * as an image icon followed by its alt text ({@code markdown-image-cell} on both), runs of the cell's text in the
 * link's colour, so the table stays compact and its column widths measure the image as text. The image's title is their
 * tooltip, and inside a link they are the link's runs. The cell sorts, names its column and copies by the alt text. A
 * click on them opens the image at full size (see below), with a hand cursor and an underline under the pointer, as a
 * link has. In a column header a click sorts, as on the header's text, and the context menu opens the image. A no-break
 * space joins the icon and the alt text, so the two never wrap apart.
 *
 * <p><b>Selecting and copying.</b> An inline image is one character of its line for the selection: a drag, shift plus
 * a click, a triple click on its line's text and select-all take it as one. A double or triple click on a picture
 * selects nothing, as its first click opens the image (see <em>Full size</em>), while one on a placeholder or an error
 * box selects the image, an inline one alone, a block one whole. A block image
 * takes part in the selection as a whole, as a table does: a selection that passes over it includes it, a drag that
 * ends in its upper half leaves it out, one that ends in its lower half takes it, and a press on it starts a drag as a
 * press between blocks does. A selected image shows the selection's highlight over its picture, placeholder or error
 * box, half see-through. Copying gives an image's alt text where the image is, as a link gives its text. A block
 * image's alt text is a block of its own, set apart like a paragraph, with the prefixes of the list items and quotes
 * around it. <em>Copy Markdown</em> copies the source with the images' Markdown as written.
 *
 * <p><b>Size.</b> An image is never enlarged. Its natural size counts one image pixel as one screen pixel: its pixel
 * size divided by the output scale of the view's window, so a screenshot shows as large as it was on the screen it
 * was taken on. A larger image shrinks to the width the text wraps at and to the height of the visible area at the
 * current zoom, keeping its aspect ratio. The zoom scales an image as it scales text, and a change of the window's
 * output scale computes every image's size anew.
 *
 * <p><b>Scaling.</b> The view shrinks an image itself, in the background, at its exact size in screen pixels (its shown
 * size times the zoom and the output scale, in whole pixels): each pixel is the average of the image pixels it covers,
 * weighted by how much of each it covers. A shrunk image shows at exactly that size, each of its pixels on one screen
 * pixel, so its shown size is made of whole screen pixels. A shrunk screenshot keeps its letters readable and its thin
 * lines free of moire, which JavaFX's own scaling does not. It averages the file's colour values, premultiplied by
 * their opacity, so a transparent pixel adds no colour to its neighbours. Where the zoom shows an image beyond its
 * natural size, JavaFX enlarges it. During a resize or a change of the zoom an image shows its last bitmap scaled to
 * the new size, and the view computes a new one once the size has not changed for 150 ms, and after a change of the
 * output scale.
 *
 * <p><b>Memory.</b> The view keeps the bytes of each image file and a bitmap at the size the image shows at, never the
 * whole decoded image unless it shows at its full size. It decodes one image at a time, on one background thread
 * shared by all views, so the memory of decoding peaks at that of the largest single image: the images in or near
 * the visible area first, then the rest in document order. A scroll reorders the images still waiting, and a new
 * text drops those of the old one. {@link #imagePixelLimitProperty() A limit}, none by default, shows an image of
 * more pixels as a box with {@link #imageTooLargeTextProperty() its own note}. Near the visible area means within one
 * visible height above or below it.
 *
 * <p><b>Animated GIFs.</b> A GIF file of more than one frame, as the view tells from the file's bytes, plays in the
 * document while {@link #imageAnimationProperty() animations play}, the default: JavaFX scales it to its shown size,
 * not the view's area average, and it stays decoded while it shows, an exception to the scaling and the memory rule
 * above. Switched off, the document shows its first frame, decoded, scaled and kept like any image, for a calm
 * document. A change switches the GIFs shown, in place, from the bytes the view keeps. The full size window always
 * plays an animated GIF, at every zoom. A GIF of one frame is an ordinary image. A GIF counts as animated as soon as a
 * second frame begins in its file. The pixel limit holds for an animated GIF by the size of its frames, in the
 * document and in the window.
 *
 * <p><b>Reading position.</b> An image that arrives above the line at the top edge of the visible area keeps that line
 * in place, and so does one that a reload or a {@link #updateMarkdown(String) text update} changes there. One in or
 * below the visible area pushes the text below it down. Only an image whose top lies above that line keeps it, so the
 * first image of a document read from its top pushes the text down as it arrives. A block image is no reading anchor
 * itself: a zoom or an arriving image keeps the place of the text around it.
 *
 * <p><b>Loading.</b> The text shows at once: the view loads and decodes its images in the background and shows a
 * placeholder ({@code markdown-image-placeholder}), an image icon and the alt text on one line, until an image is
 * decoded. An image the loader has no file for shows a box ({@code markdown-image-error}) with a missing-image icon,
 * the alt text and {@link #imageMissingTextProperty() a note}, the address as its tooltip. So does an image whose file
 * the view cannot decode, such as an SVG or WebP file, with {@link #imageUnsupportedTextProperty() its own note}. A
 * loader that throws a runtime exception is reported as a warning with the address through SLF4J, a missing or
 * undecodable image at debug level. A loader that returns {@code null} or an empty array counts as missing too. The
 * loader runs on at most four background threads at once, for all views together. A new version of the text set by
 * {@link #updateMarkdown(String)} shows no placeholder for an image whose address, as written, the shown text has: it
 * starts with what the shown image of that address showed (the first of them that showed one, where the address
 * occurs several times), its picture at its size, an animated GIF playing, or its error box, loads its file again and
 * shows the new result in place once it arrives, as a reload does (see <em>Sources</em>), so a changed file shows its
 * new picture and a missing one its error box. An image of an address whose images all still showed their
 * placeholders, and one of a new address, start with the placeholder.
 *
 * <p><b>Sources.</b> The image loader is at first the view's own: a relative address resolves against
 * {@link #imageBaseProperty() the base address}, as a browser resolves it against a page. Absolute {@code file:}
 * and {@code jar:} addresses and absolute file paths of the platform load as they are, {@code data:} addresses with
 * base64 content of an {@code image/} type are decoded, and {@code http:} and {@code https:} addresses load only
 * while {@link #remoteImagesProperty() images from the internet} are switched on. Any other address is missing. An
 * application may keep the view's own loader, wrap it or replace it. A change of the base, the internet switch, the
 * loader or the pixel limit loads the shown images anew, in place: the document is neither rebuilt nor scrolled, each
 * image shows what it showed until its new file arrives, and an image above the line at the top edge keeps that line
 * in place. Switching images from the internet off thus shows them as missing again. A
 * {@code jar:} address reads an archive file only: an archive on the internet ({@code jar:https:}) is missing even
 * while images from the internet are on, so no download passes the switch. An address that starts with {@code //} is
 * another host's address, never a file path.
 *
 * <p><b>Full size.</b> The reader opens any image in the view's full size window in three ways: by a button
 * ({@code markdown-image-open}, the theme's icon button with a fullscreen icon) over the top right corner of a block
 * image, shown while the pointer is over the image, by a click with the primary button without moving on an image (a
 * block, an inline image or the icon or alt text of one in a table cell), and by <em>Open image</em> in the context
 * menu requested on an image (see <em>Copying</em>). A press followed by a drag selects and opens nothing. A click on
 * a linked image follows its link instead, while its button and its menu entry open it. An image opens once it shows
 * its picture, not from its placeholder or error box. One in a table cell is loaded through the loader when it opens.
 * The tooltip and accessible text of the button and the label of the menu entry are {@link #openImageTextProperty()}.
 *
 * <p><b>The window.</b> One window per view ({@code markdown-image-window}), not modal, so the reader keeps working
 * beside it, owned by the view's window, so it closes with it, decorated, in the theme and with the application's
 * stage icon. Opening another image of the view shows it in the open window. It opens as large as the image at 100 %
 * plus its bar, at most 90 % of the visual bounds of the screen, at least 320 by 240 pixels and at least as wide as its
 * bar asks to be, so its labels show whole in any language, centred over the view's
 * window, and keeps the size the reader gives it while it stays open. Its title is the image's alt text, else the file
 * name of its address, else the address ({@code data:} and the type for a {@code data:} address). Its bar has
 * <em>Fit</em> ({@link #fitImageTextProperty()}) and <em>100 %</em> ({@link #actualImageSizeTextProperty()}), shows
 * the zoom in percent and has, at its right end, <em>Copy image</em> ({@link #copyImageTextProperty()},
 * {@code markdown-image-copy}), which puts the whole image on the system clipboard through JavaFX's clipboard, the
 * first frame of an animated GIF, and <em>Save as</em> ({@link #saveImageTextProperty()}, {@code markdown-image-save}),
 * which opens JavaFX's file chooser with the file name of the address, its percent escapes decoded ({@code image} and
 * the extension of its type for a {@code data:} address), and writes the bytes the loader returned, unchanged, in the
 * background. If writing fails, karthu-fx's error dialog names the file and the reason. <em>Copy image</em> is enabled
 * while the window shows an image, <em>Save as</em> while it has the image's file, also one it cannot show, such as an
 * SVG file. The image area ({@code markdown-image-viewer}) shows the image fitted, as large as fits,
 * never beyond 100 %, or zoomed: 100 % is one image pixel per screen pixel of the window's screen, placed on whole
 * screen pixels, pixel sharp. A large image starts fitted, a small one at 100 %. A double click toggles between 100 %
 * and fitted, keeping the clicked point under the pointer. The shortcut key plus the wheel zooms by 1.25 per notch
 * around the pointer, from the fitted zoom up to 800 %. The wheel scrolls the image vertically, shift plus the wheel
 * sideways, and a drag moves it, with an open and a closed hand. Below 100 % the image is shrunk by the same area
 * average as in the document once the zoom has settled, beyond 100 % JavaFX enlarges it. Escape closes the window. The
 * window decodes the whole image, on the view's decoding thread, while it shows it, and lets it go when it closes or
 * shows another image. Where an image has no file, cannot be decoded or has more pixels than the limit, the window
 * shows the note of the document's box instead, and opens at least 480 by 360 pixels.
 *
 * <p>Fitted follows the window's size, and zooming out with the wheel down to the fitted zoom goes back to fitted. A
 * step of the zoom (a new image, <em>Fit</em>, <em>100 %</em>, a double click) shrinks the image at once, the wheel
 * and a resize of the window after the 150 ms of the document. The window has no keys to zoom and none to copy:
 * Escape is its one key. The error dialog of <em>Save as</em> is modal to the window alone, so the application goes on
 * working while it shows.
 *
 * <p><b>Properties and texts.</b> The view's properties of images, each with a getter and a setter:
 * <table>
 *   <caption>Image properties of the Markdown view</caption>
 *   <tr><th>Property</th><th>Default</th><th>What it sets</th></tr>
 *   <tr><td>{@link #imageLoaderProperty() imageLoader}</td><td>the view's own loader</td><td>where the images
 *       come from, called on background threads ({@link ImageLoader})</td></tr>
 *   <tr><td>{@link #imageBaseProperty() imageBase}</td><td>{@code null}</td><td>the address the own loader
 *       resolves relative addresses against</td></tr>
 *   <tr><td>{@link #remoteImagesProperty() remoteImages}</td><td>{@code false}</td><td>whether the own loader
 *       loads images from {@code http:} and {@code https:} addresses</td></tr>
 *   <tr><td>{@link #imagePixelLimitProperty() imagePixelLimit}</td><td>0, no limit</td><td>the largest image the
 *       view shows, in pixels</td></tr>
 *   <tr><td>{@link #imageAnimationProperty() imageAnimation}</td><td>{@code true}</td><td>whether animated GIFs
 *       play in the document</td></tr>
 * </table>
 * A change of one of the first four loads the shown images anew, in place, a change of the last switches the shown
 * animated GIFs in place. The texts of images are English by default and can be replaced like the copy texts, for
 * example to match the application's language. A change applies at once:
 * <table>
 *   <caption>Image texts of the Markdown view</caption>
 *   <tr><th>Property</th><th>Default</th><th>Where it shows</th></tr>
 *   <tr><td>{@link #openImageTextProperty() openImageText}</td><td>{@code Open image}</td><td>the tooltip and
 *       accessible text of the open button, the context menu's entry</td></tr>
 *   <tr><td>{@link #fitImageTextProperty() fitImageText}</td><td>{@code Fit}</td><td>the window's button</td></tr>
 *   <tr><td>{@link #actualImageSizeTextProperty() actualImageSizeText}</td><td>{@code 100%}</td><td>the window's
 *       button</td></tr>
 *   <tr><td>{@link #copyImageTextProperty() copyImageText}</td><td>{@code Copy image}</td><td>the window's
 *       button</td></tr>
 *   <tr><td>{@link #saveImageTextProperty() saveImageText}</td><td>{@code Save as...}</td><td>the window's button,
 *       the title of the file chooser and of the error dialog</td></tr>
 *   <tr><td>{@link #imageMissingTextProperty() imageMissingText}</td><td>{@code Image not found}</td><td>the note of
 *       the box of a missing image and of the window</td></tr>
 *   <tr><td>{@link #imageUnsupportedTextProperty() imageUnsupportedText}</td><td>{@code Image format not
 *       supported}</td><td>the note of the box of an image the view cannot decode and of the window</td></tr>
 *   <tr><td>{@link #imageTooLargeTextProperty() imageTooLargeText}</td><td>{@code Image too large}</td><td>the note
 *       of the box of an image above the limit and of the window</td></tr>
 * </table>
 *
 * <h2>Code colours</h2>
 *
 * <p>A code block whose language the view knows is coloured: a lexer ({@link CodeLexer}) cuts its code into pieces
 * ({@link CodePiece}) such as keywords, strings, numbers and comments, and each piece shows in the colour of its
 * {@link CodeCategory category}. A lexer is lexical, it does not parse, so code that does not compile is coloured too.
 * The language is the first word of the fence's info string, which ends at the first space or
 * <code>&#123;</code>, in lower case: {@code java}, {@code Java} and {@code java title} are Java. A block without a
 * language or in one the view does not know shows in one colour, the code colour, as do source blocks and inline code.
 * Selecting, copying, the copy button, sideways scrolling and the zoom work in a coloured block as in any other, and
 * copying gives the code as written, without colours.
 *
 * <p><b>Languages.</b> The view knows the languages of its map, {@link #getCodeLanguages()}, which starts with the
 * languages built into the view, under their names and short names:
 * <ul>
 *   <li>Java: {@code java};</li>
 *   <li>JSON: {@code json};</li>
 *   <li>YAML: {@code yaml} and {@code yml};</li>
 *   <li>SQL: {@code sql};</li>
 *   <li>XML and HTML: {@code xml}, {@code html} and {@code xhtml};</li>
 *   <li>Bash: {@code bash}, {@code sh} and {@code shell};</li>
 *   <li>Markdown: {@code markdown} and {@code md}.</li>
 * </ul>
 * No language embeds another: the content of a {@code script} or {@code style} element in HTML and a fenced block in
 * Markdown are not coloured as their languages. The map belongs to the view, each view starting with the built-in
 * languages: an application adds a language of its own with {@code put}, replaces a built-in one the same way and
 * switches one off with {@code remove}, which leaves its blocks in one colour, and every change rebuilds the shown
 * document, as a new text does.
 *
 * <p><b>Lexers.</b> A lexer takes the code of a block as the block shows it and returns its pieces in the order of
 * the text, each with its start, its end and its category: not empty, inside the text, not overlapping. Text between
 * pieces is plain code. The view calls it on the JavaFX Application Thread, once per block each time it builds its
 * document. A lexer that throws a runtime exception or returns pieces that break this contract leaves its block in
 * one colour, the rest of the document renders, and the view logs a warning naming the language through SLF4J (see
 * {@link CodeLexer}). A block with more lines than {@link #codeHighlightLineLimitProperty() the line limit},
 * {@value #DEFAULT_CODE_HIGHLIGHT_LINE_LIMIT} by default, stays in one colour too and its lexer is not called, so that
 * a very long block cannot make the view slow.
 *
 * <p><b>Categories.</b> Every piece belongs to one of 23 categories ({@link CodeCategory}). Nine base categories have
 * a colour of their own, so nine colours colour every language completely. The fourteen fine ones take the colour of
 * the category they fall back to until they are set themselves, three of them the code colour
 * {@code -fx-code-text-color}. Nebula Light follows the default colours of Eclipse's light editors: some of its
 * fine categories have a colour of their own there, and plain code in a block is black. Each category is a CSS variable of the theme, with a colour in each of its two colour
 * schemes, Nebula Dark and Nebula Light ({@link org.karthu.fx.theme.ThemeManager}), and a style class:
 * <table>
 *   <caption>Categories of coloured code</caption>
 *   <tr><th>Category</th><th>Marks</th><th>Variable</th><th>Nebula Dark</th><th>Nebula Light</th>
 *       <th>Style classes of its runs</th></tr>
 *   <tr><td>comment</td><td>comments</td><td>{@code -fx-code-comment-color}</td><td>{@code #8a90a8}</td>
 *       <td>{@code #3F7F5F}</td><td>{@code code-comment}</td></tr>
 *   <tr><td>keyword</td><td>keywords of a language</td><td>{@code -fx-code-keyword-color}</td>
 *       <td>{@code #c88bff}</td><td>{@code #7F0055}</td><td>{@code code-keyword}</td></tr>
 *   <tr><td>string</td><td>text in quotes</td><td>{@code -fx-code-string-color}</td><td>{@code #e0a84c}</td>
 *       <td>{@code #2A00FF}</td><td>{@code code-string}</td></tr>
 *   <tr><td>number</td><td>numbers</td><td>{@code -fx-code-number-color}</td><td>{@code #ff9e64}</td>
 *       <td>{@code #000000}</td><td>{@code code-number}</td></tr>
 *   <tr><td>type</td><td>type names</td><td>{@code -fx-code-type-color}</td><td>{@code #4cd38a}</td>
 *       <td>{@code #000000}</td><td>{@code code-type}</td></tr>
 *   <tr><td>function</td><td>a name followed by {@code (}</td><td>{@code -fx-code-function-color}</td>
 *       <td>{@code #5d9bff}</td><td>{@code #000000}</td><td>{@code code-function}</td></tr>
 *   <tr><td>variable</td><td>Bash variables, SQL parameters, YAML anchors and aliases</td>
 *       <td>{@code -fx-code-variable-color}</td><td>{@code #ff8fa3}</td><td>{@code #000000}</td>
 *       <td>{@code code-variable}</td></tr>
 *   <tr><td>property</td><td>keys in JSON and YAML</td><td>{@code -fx-code-property-color}</td>
 *       <td>{@code #7b8cff}</td><td>{@code #0000C0}</td><td>{@code code-property}</td></tr>
 *   <tr><td>tag</td><td>XML and HTML tags</td><td>{@code -fx-code-tag-color}</td><td>{@code #ff8fa3}</td>
 *       <td>{@code #3F7F7F}</td><td>{@code code-tag}</td></tr>
 *   <tr><td>doc</td><td>documentation comments such as Javadoc</td><td>{@code -fx-code-doc-color}</td>
 *       <td>as comment</td><td>{@code #3F5FBF}</td><td>{@code code-doc}, {@code code-comment}</td></tr>
 *   <tr><td>annotation</td><td>Java annotations</td><td>{@code -fx-code-annotation-color}</td><td>as keyword</td>
 *       <td>{@code #646464}</td><td>{@code code-annotation}, {@code code-keyword}</td></tr>
 *   <tr><td>literal</td><td>{@code true}, {@code false}, {@code null}, SQL's {@code NULL}</td>
 *       <td>{@code -fx-code-literal-color}</td><td>as number</td><td>as keyword</td>
 *       <td>{@code code-literal}, {@code code-number}</td></tr>
 *   <tr><td>constant</td><td>Java names in capitals only, at least two characters</td>
 *       <td>{@code -fx-code-constant-color}</td><td>as literal</td><td>as property</td>
 *       <td>{@code code-constant}, {@code code-literal}, {@code code-number}</td></tr>
 *   <tr><td>escape</td><td>escapes in strings, XML entities</td><td>{@code -fx-code-escape-color}</td>
 *       <td>as string</td><td>as string</td><td>{@code code-escape}, {@code code-string}</td></tr>
 *   <tr><td>attribute</td><td>XML and HTML attribute names</td><td>{@code -fx-code-attribute-color}</td>
 *       <td>as number</td><td>{@code #7F007F}</td><td>{@code code-attribute}, {@code code-number}</td></tr>
 *   <tr><td>operator</td><td>operators</td><td>{@code -fx-code-operator-color}</td><td>the code colour</td>
 *       <td>the block's code colour</td><td>{@code code-operator}</td></tr>
 *   <tr><td>punctuation</td><td>brackets, separators</td><td>{@code -fx-code-punctuation-color}</td>
 *       <td>the code colour</td><td>{@code #008080}</td><td>{@code code-punctuation}</td></tr>
 *   <tr><td>heading</td><td>Markdown headings</td><td>{@code -fx-code-heading-color}</td><td>as function</td>
 *       <td>as function</td><td>{@code code-heading}, {@code code-function}</td></tr>
 *   <tr><td>link</td><td>Markdown links and addresses</td><td>{@code -fx-code-link-color}</td><td>as property</td>
 *       <td>as property</td><td>{@code code-link}, {@code code-property}</td></tr>
 *   <tr><td>code</td><td>Markdown inline code and fences</td><td>{@code -fx-code-code-color}</td><td>as string</td>
 *       <td>as string</td><td>{@code code-code}, {@code code-string}</td></tr>
 *   <tr><td>list</td><td>Markdown list markers</td><td>{@code -fx-code-list-color}</td><td>as keyword</td>
 *       <td>as keyword</td><td>{@code code-list}, {@code code-keyword}</td></tr>
 *   <tr><td>quote</td><td>Markdown quote lines</td><td>{@code -fx-code-quote-color}</td><td>as comment</td>
 *       <td>as comment</td><td>{@code code-quote}, {@code code-comment}</td></tr>
 *   <tr><td>emphasis</td><td>Markdown emphasis and strong emphasis</td><td>{@code -fx-code-emphasis-color}</td>
 *       <td>the code colour</td><td>the block's code colour</td><td>{@code code-emphasis}</td></tr>
 * </table>
 *
 * <p><b>Changing colours.</b> The variables are defined at the theme's root, in the stylesheet of each scheme, a base
 * one with the scheme's colour, a fine one in both schemes as a reference to the variable it falls back to
 * ({@code -fx-code-annotation-color: -fx-code-keyword-color}). An
 * application changes them as any colour of the theme, for one view with {@code setStyle} or for all views with a
 * stylesheet, and the view shows the change at once, without a new text. JavaFX resolves a reference from the
 * coloured run upwards, so a base colour set on one view also changes that view's fine categories that still fall back
 * to it, and a fine category set alone changes only its own runs. The theme's rules colour each {@code code-} class
 * from its variable, under {@code .markdown-view .markdown-code-block}, so they outweigh a plain rule such as
 * {@code .code-keyword { -fx-fill: red; }}: colours change through the variables. A coloured block inside a quote
 * keeps its colours.
 *
 * <p><b>Style classes and fonts.</b> Each piece's run carries the style class of its category and of every category
 * its fallback chain passes, as the table shows, so an own rule for {@code code-keyword} also reaches annotations and
 * list markers, and one for {@code code-annotation} only annotations. The defaults are colours only, no bold and no
 * italic. An own rule may make a category bold or italic, which shows only when the code font has those faces: JavaFX
 * finds no bold or italic face of a variable font such as "Cascadia Mono", while an underline always shows.
 *
 * <h2>Tables</h2>
 *
 * <p>A GFM table is a line with {@code |} followed by a delimiter row with as many cells, such as
 * {@code |---|:-:|}, and the rows below, up to a blank line, a line without {@code |} or the start of another block;
 * the outer pipes are optional, and it may interrupt a paragraph. The view shows it as a {@code TableView}, a column
 * per table column and a row per body row, inside a list item or a quote too. Each header and body cell is inline
 * Markdown, shown as a paragraph shows it (emphasis, code, links that call the link handler), and aligned left,
 * centred or right as its delimiter cell says ({@code :--}, {@code :-:}, {@code --:}); {@code \|} is a {@code |},
 * also inside code, a shorter row gets empty cells and the cells beyond the header's are left out.
 *
 * <p>The columns share the view's width as a web browser lays out a table: none narrower than its longest word, all
 * growing evenly from there, a short column (names, types, flags) up to the width of its widest cell on one line,
 * and the long columns taking the rest, so long text wraps in its cell, the row growing with it; when every cell fits
 * on one line, the columns share the width left over in proportion to their widest cells. While the view's width
 * changes, every column's width changes smoothly: none jumps. A table is as tall as its header and all its rows: it
 * never shows a vertical scroll bar, and the mouse wheel over it keeps scrolling the document. A table that cannot fit
 * even with every column at its longest word keeps them whole and scrolls sideways under a horizontal bar of its own,
 * as a code block's long line does.
 *
 * <p>The reader can sort, move, resize, show and hide a table's columns. A click on a header sorts the rows by that
 * column, ascending, the next click descending, the third back to the source order; shift plus a click on another
 * header sorts by that column too, within the first. A column sorts its cells' visible text (code without backticks,
 * emphasis without asterisks, a link as its text, an image as its alt text) in natural order: digit runs by their
 * numeric value, so {@code item2} comes before {@code item10}, letters without their case, and texts this leaves
 * equal (differing in case or leading zeros only) in a fixed order; rows equal in every sorting column keep their
 * source order. Dragging a header moves its column; dragging the edge between two headers resizes the column on its
 * left, which keeps the width it is dragged to while the columns right of it give or take the difference, none
 * narrower than its longest word (the table's right edge stays where it is), and from then on the columns keep their
 * shares when the view's width changes; a double click on the edge fits the column to its content as far as the
 * others can give. The menu button at the right end of the header lists the columns by their header's text, to show
 * or hide each. Every new text starts its tables fresh: unsorted, the columns where the author put them, all shown, at
 * their fitted widths.
 *
 * <p>The reader selects a table's cells as in a spreadsheet. A click selects one cell and gives the table the
 * keyboard focus; a drag with the primary button selects the rectangle from the pressed cell to the cell under the
 * pointer (the nearest cell beyond the table's edges); shift plus a click selects the rectangle from the anchor, the
 * cell clicked last without shift; the shortcut key (Ctrl, Command on macOS) plus a click adds a cell to the
 * selection or removes it. Shift plus an arrow key moves the far corner of the rectangle from the anchor by a cell,
 * shift plus space extends the selection to the whole rows it touches (shift plus up or down then extends it by
 * whole rows), and the shortcut key plus A selects every cell; <em>Select column</em> in the context menu selects a
 * whole column. A rectangle spans the shown columns in their shown order and the rows in their shown order. A sort
 * clears the table's selection. A click on a link in a cell follows the link and selects the cell.
 *
 * <h2>Task lists</h2>
 *
 * <p>A list item that starts with a GFM task marker is a task and shows a check box ({@code markdown-task-box})
 * instead of its bullet, or beside its number: open for {@code [ ]}, ticked for {@code [x]} and {@code [X]}. The
 * marker counts only at the very start of the item's first paragraph, on the list marker's line, with exactly one
 * space, {@code x} or {@code X} between the brackets, followed by a space, a tab or the end of the line; the item's
 * text starts after it. Anything else stays text, as on GitHub: other characters between the brackets
 * ({@code [/]}, {@code [-]}), a bracket followed by anything but white space ({@code [x](address)} is a link), an
 * escaped bracket ({@code \[x]}), a marker later in the text or on a later line, and one outside a list item. An item
 * with the marker alone ({@code - [ ]}) is an empty task, its check box alone. Tasks show in bullet and ordered lists
 * at any depth, in loose lists and inside quotes.
 *
 * <p>The check box only shows the state: the reader cannot tick it, and the view never changes its source. It has no
 * hover effect, no hand cursor and no keyboard focus, and a press on it starts a selection as a press beside the text
 * does. It is drawn from the theme's check box colours, right-aligned against the item's text, its middle on the middle
 * of the first line's lowercase letters, and scales with the zoom. In a bullet list it takes the bullet's place, so the
 * text of task and plain items stays aligned. In an ordered list the number stands left of the box ({@code 3.} and the
 * box), and an ordered list with a task indents all its items further, so their text stays aligned too, and the
 * numbers of its plain items stand where the numbers of its tasks do. A done item's text looks like any other text:
 * the item's row carries {@code markdown-task-item}, and the pseudo-class {@code :checked} while done, for a
 * stylesheet that strikes done items through or dims them.
 *
 * <p>The check box names its state in words, as its tooltip and as its accessible text: {@code Open} and {@code Done}
 * by default, {@link #taskOpenTextProperty()} and {@link #taskDoneTextProperty()}, which an application replaces to
 * match its language. For a screen reader the box is a check box, selected while the task is done.
 *
 * <h2>Copying</h2>
 *
 * <p>A code block has a copy button in its top right corner, laid over the code so that it stays in place when the
 * code scrolls sideways; it copies the block's code as the block shows it (a source block has none). The view's
 * context menu, requested anywhere in the view, has an entry <em>Copy Markdown</em> that copies the whole Markdown
 * source exactly as it was set; the entry is disabled while the text is {@code null} or empty. Requested on a table's
 * cell or column header, the menu has three entries above it, set apart by a separator: <em>Copy</em> copies the
 * table's selected cells as the shortcut key plus C does (see below), <em>Copy with header</em> the same with a first
 * line of the titles of the copied columns (their header's visible text), both disabled without a selection, and
 * <em>Select column</em> selects every cell of the column the menu was opened on; requested from the keyboard while a
 * table has the focus, the column of its focused cell; on a table without selected cells that the text selection
 * passes over (see below), <em>Copy</em> copies the text selection. Requested on a code or source block, the menu has
 * <em>Copy</em> above it, set apart the same way, which copies the text selection if it reaches into the block and is
 * disabled otherwise; requested anywhere else, in text, on an image or between blocks, the same <em>Copy</em> copies
 * the text selection wherever it lies and is disabled without one. Requested on an image, a block, an inline image or
 * the icon or alt text of one in a table cell, linked or not, the menu has <em>Open image</em> first, set apart from
 * the entries below it by a separator, which opens the image in the full size window (see <em>Images</em>). It is
 * disabled while the image shows its placeholder or its error box. Everything copied goes to the system
 * clipboard as plain text. The English labels, the button's tooltip ({@link #copyCodeTextProperty()}) and the menu
 * entries ({@link #copyMarkdownTextProperty()}, {@link #copyTextProperty()}, {@link #copyWithHeaderTextProperty()},
 * {@link #selectColumnTextProperty()}), can be replaced, for example to match the application's language.
 *
 * <p>An application adds entries of its own to the context menu through a {@link ContextMenuCallback}
 * ({@link #contextMenuCallbackProperty()}): called on every opening, after the view's entries above are in place, it
 * receives the menu's entries and the {@link ContextMenuPlace place} of the request with the source line under the
 * right click (see <em>Source lines</em>), for example to open the text in an editor at that line.
 *
 * <p>In a table with the keyboard focus, the shortcut key (Ctrl, Command on macOS) plus C copies the selected cells
 * as text that pastes into a spreadsheet cell by cell: one cell as its visible text (code without backticks, emphasis
 * without asterisks, a link as its text, an image as its alt text), several cells as their bounding rectangle, the
 * shown columns from the
 * leftmost to the rightmost selected one in their shown order and the rows from the first to the last selected one
 * in their shown order, a cell of the rectangle that is not selected empty, cells joined by a tab and rows by a line
 * feed, with no line feed at the end and no quoting. A hidden column is never copied; without selected cells the key
 * copies the view's selection as anywhere in the view (see below), the text selection perhaps passing over the table.
 * A right click on a cell that is not selected selects it alone, as in a spreadsheet, before the menu opens; only
 * the primary button sorts by a header, so a right click on a header opens the menu and leaves the order and the
 * selection as they are.
 *
 * <p>All text the view shows can be selected as in a web browser, across blocks, in document order: headings,
 * paragraphs, list items and quotes at any depth, the code of code blocks and the lines of source blocks, tables and
 * block images as a whole, an inline image as one character (see <em>Images</em>). A drag with the primary button
 * selects from the character pressed to the character under the pointer,
 * forwards or backwards, across any number of blocks; a press between blocks counts as the end of the block above
 * it. A table the selection passes over is part of it whole, shown with every cell and header in the selection's
 * colours; a drag that ends in a table's upper half leaves the table out, one that ends in its lower half takes it.
 * A press on a table's cell selects cells (see above), never text. Shift plus a click extends the selection from where
 * the last press without shift anchored it; a double click selects the word under the pointer, in text as the
 * platform's word iterator finds it in prose, in code a run of letters, digits and underscores (else of spaces, else
 * one character); a triple click selects a heading's or paragraph's whole text, in code its line without the line
 * feed; a plain click clears the selection. While a drag holds the pointer above or below the view's visible area,
 * the document scrolls along, the faster the further the pointer is beyond the edge. A click on a link without moving
 * follows the link; a press on a link followed by a drag selects text and follows nothing. The pointer is the text
 * cursor over text and code (a link keeps its hand cursor), and the selection shows as a highlight over the selected
 * characters, which show on it in the selection's text colour.
 *
 * <p>A press in a heading, a paragraph or between blocks gives the view the keyboard focus, a press in a code or
 * source block that block (Tab does not reach it). The shortcut key (Ctrl, Command on macOS) plus C, pressed wherever
 * the focus is inside the view, copies the view's selection as plain text; a code or source block with the focus
 * copies the text selection only if it reaches into the block, a table with selected cells those cells. Plus A
 * selects the whole document, code blocks and tables included; in a code or source block with the focus the block's
 * whole text, in a table with the focus every cell. The copied text is what the reader sees: inline Markdown resolved
 * (code without backticks, emphasis without asterisks, a link as its text, an image as its alt text, a block image's
 * as a block of its own), a soft line break as a space and a hard
 * one as a line feed, code as written without its fences, a table as its header line and every row in the shown
 * order, the shown columns in their shown order, cells joined by a tab (as <em>Copy with header</em> writes them);
 * blocks are separated by a line feed, a heading or a paragraph from its neighbours by a blank line (a tight list's
 * items are no paragraphs here); a list item starts with its marker ({@code - } for a bullet, the number and delimiter
 * such as {@code 3. } in an ordered list, a task's followed by its task marker, {@code - [ ] } or {@code 3. [x] }, so
 * the copy keeps the state) and its further lines, like a nested list's items and the lines of code or a
 * table inside it, are indented by two spaces per level; each line in a quote starts with {@code > }, a blank line
 * inside it with {@code >}. A block selected only in part gives its selected part, with its marker or prefix only if
 * the selection includes its first character; a selection within one code or source block gives its characters
 * exactly as written, without any. Without a selection nothing is copied.
 *
 * <p>One selection is held at a time across the view: the text selection or a table's selection of cells. A
 * selection started in one clears the other, and a new text clears it.
 *
 * <h2>Zoom</h2>
 *
 * <p>The whole document can be zoomed, as a web browser zooms a page, through {@link #zoomProperty()}: a factor from
 * 0.5 to 2.0 in whole percent, 1.0 being 100 %. Text, headings, lists, quotes, tables, code and source blocks with
 * their sideways scroll bars, the copy buttons and all spacing grow or shrink evenly: the box of all blocks
 * ({@code markdown-document}) is scaled from its top left corner, so its stylesheet sizes stay in the document's own
 * pixels. The text wraps to the width of the view at every zoom, laid out at the visible width divided by the zoom,
 * so prose never scrolls sideways, and the view scrolls the zoomed height. A new zoom keeps the reader's place: the
 * line at the top edge of the visible area stays there, cut as far down as before, though the text wraps anew (a code
 * or source block or a table cut by the edge stays cut as far down, the space between blocks scales), as far as the
 * scroll range allows. Selecting, copying, links, the context menu and the copy buttons work at every zoom. The zoom
 * stays when the text changes; a new text still scrolls to the top, an update keeps the place as a zoom does (see
 * <em>Text updates</em>). At 1.0 the document is not scaled at all and
 * looks exactly as without a zoom. A table fits its columns to the document's unscaled width and snaps its grid lines
 * to the window's device pixels without the zoom, so at some zooms a grid line may fall between two pixels.
 *
 * <h2>Zoom bar</h2>
 *
 * <p>A bar on top of the view, outside its scroll pane, lets the reader zoom: from the left a framed zoom group, a
 * slider and a <em>Reset</em> button. It shows by default; {@link #zoomBarVisibleProperty()} hides it, which also takes
 * zooming from the reader, while the application can still set the zoom. The bar always shows the zoom the view shows.
 * <ul>
 *   <li>The zoom group holds minus, the percent field with a chevron and plus. Minus and plus go to the next of the
 *       presets 50, 75, 100, 125, 150 and 200 % in their direction, also from a value in between (from 130 %, plus
 *       gives 150 % and minus 125 %); minus is disabled at 50 %, plus at 200 %.</li>
 *   <li>The percent field shows the zoom as {@code 125%}. It takes a typed value on Enter or when the focus moves from
 *       it to another node: the text without surrounding spaces and without a trailing {@code %} (and the spaces before
 *       it), read as a decimal number with a dot or a comma, rounded half up to whole percent and limited to 50 to 200;
 *       anything else, the empty text included, brings back the zoom. Escape brings back the zoom without taking the
 *       text. Typing changes no zoom, and a zoom the application sets meanwhile leaves the typed text alone until it is
 *       taken or abandoned. Up and Down go to the next larger or smaller preset at once, Alt plus Down opens the list
 *       of zoom levels.</li>
 *   <li>The chevron, or Alt plus Down in the field, opens the list of zoom levels under the group: the presets, the
 *       zoom selected if it is one. Up and Down move the selection, Enter or a click chooses a preset, Escape closes
 *       the list.</li>
 *   <li>The slider goes from 50 to 200 % in whole percent; <em>Reset</em> sets 100 % and is disabled there.</li>
 * </ul>
 * Only the percent field is in the tab order; a click on minus, plus, the chevron, the slider or <em>Reset</em> leaves
 * the focus where it is. In a narrow view the slider gets narrower and then disappears, then <em>Reset</em>; the zoom
 * group always stays. The tooltips of minus, plus and the chevron ({@link #zoomOutTextProperty()},
 * {@link #zoomInTextProperty()}, {@link #zoomLevelsTextProperty()}, the last also the field's accessible text) and the
 * label of <em>Reset</em> ({@link #resetZoomTextProperty()}) can be replaced like the copy texts.
 *
 * <p>The reader zooms without the bar's buttons too, as in a web browser:
 * <ul>
 *   <li>The shortcut key (Ctrl, Command on macOS) plus the mouse wheel anywhere over the document goes one preset
 *       larger per notch turned up and one smaller per notch turned down, also from a value in between, and keeps the
 *       point of the document under the pointer there: a line of text cut as far down, though the text wraps anew, a
 *       code or source block or a table at the same point of its height. A notch is the step the platform reports for
 *       the wheel, whatever its speed; the smaller steps of a touchpad add up to a notch, and a movement the other way
 *       or without the shortcut key starts the count over. The view takes the wheel before its code blocks and tables
 *       and consumes it, so nothing scrolls. Without the shortcut key the wheel scrolls as always.</li>
 *   <li>While the focus is anywhere inside the view, in a code block, a table or the percent field too, the shortcut
 *       key plus {@code +} goes one preset larger, plus {@code -} one smaller and plus {@code 0} sets 100 %, keeping the
 *       line at the top as minus and plus do. On the main keyboard a shortcut is the key that types its character on
 *       the reader's keyboard layout, with Shift where the layout needs it: {@code +} is a key of its own on a German
 *       keyboard, Shift plus {@code =} on a US one and Shift plus {@code 1} on a Swiss one, while the same keys without
 *       Shift type another character and do not zoom. On the keypad its plus, minus and 0 are the shortcuts. Alt must
 *       not be down, so AltGr never zooms. The view consumes the shortcuts, so an accelerator of the application's menu
 *       with the same keys does not fire while the focus is in the view; it fires while the focus is elsewhere.</li>
 * </ul>
 * Wheel and shortcuts zoom only while the bar shows: while it is hidden the shortcut key plus the wheel scrolls as
 * without a zoom and the shortcuts pass on, unconsumed. While the application has bound the zoom, they change nothing,
 * as the bar does.
 *
 * <h2>Margins</h2>
 *
 * <p>The text keeps a space to its left and its right, by default the theme's padding of {@code markdown-document},
 * 14 pixels on each side. An application sets margins of its own through {@link #leftMarginProperty()} and
 * {@link #rightMarginProperty()}, in pixels at 100 %:
 * <pre>{@code
 * view.setLeftMargin(80);
 * view.rightMarginProperty().bind(settings.rightMarginProperty());
 * }</pre>
 * A margin replaces the theme's padding on its side; a side without one keeps it, and the top and the bottom always
 * keep the theme's padding, so a stylesheet that sets the padding of {@code markdown-document} keeps working. Not a
 * number, the default, leaves a side to the theme, and a value below 0 counts as 0. The zoom scales the margins with
 * everything inside the document: 40 pixels show as 60 at 150 %.
 *
 * <p>A view narrower than both margins plus 300 pixels of text, all at the zoom, shrinks both margins in their
 * proportion, so that the text keeps its 300 pixels; with no room beyond those, the margins are 0. The theme's padding
 * of a side without a margin does not shrink. So the margins never make the view scroll sideways, and a wider view
 * gives them back their size. Everything that depends on the document's width follows the margins: the text wraps
 * between them, a table fits its columns and an image shrinks to the width between them, and the search's marks and
 * the selection stay on their characters. A new margin keeps the line at the top of the visible area at the top, as a
 * new zoom does, and a zoom and a {@link #updateMarkdown(String) text update} keep the reader's place with margins as
 * without them. A press in a margin is a press in the document, as one in the theme's padding is.
 *
 * <h2>Text updates</h2>
 *
 * <p>An application that shows a new version of the same document, such as one that reloads a file when it changes,
 * sets it with {@link #updateMarkdown(String)}: the view stays at the paragraph the reader was reading, also when text
 * above it was added or removed. The line at the top edge of the visible area is found again in the new text by its
 * block, the block of the same kind with the same text nearest to its old number (a table by its text in the source's
 * order, however the reader sorted or rearranged it), else the block with its old number, and stays at the top, cut
 * as far down as before. Images keep showing what they showed until their files have been
 * loaded again, so nothing flashes and the text does not jump (see <em>Images</em>, <em>Loading</em>). A search goes
 * on from its current match's place, without scrolling. Everything else starts fresh as with a new text: the
 * selection, the tables' sorting, columns and widths, the context menu; the zoom stays. {@link #setMarkdown(String)}
 * and the text property show another document, from the top.
 *
 * <h2>Search</h2>
 *
 * <p>An application searches the document's text from controls of its own: it sets {@link #searchTextProperty() the
 * search text}, reads {@link #searchMatchCountProperty() the number of matches} and
 * {@link #searchMatchIndexProperty() the current match}, goes to the {@link #nextSearchMatch() next} and the
 * {@link #previousSearchMatch() previous} match in {@link #searchDirectionProperty() the direction} it chose, and
 * {@link #clearSearch() clears} the search:
 * <pre>{@code
 * view.searchTextProperty().bind(searchField.textProperty());
 * nextButton.setOnAction(event -> view.nextSearchMatch());
 * counter.textProperty().bind(Bindings.format("%d of %d",
 *         view.searchMatchIndexProperty().add(1), view.searchMatchCountProperty()));
 * }</pre>
 * The search works with the zoom bar hidden and needs none of the view's controls; the view takes no keys for it.
 *
 * <p><b>What is searched.</b> The text the reader sees in headings, paragraphs, list items and quotes at any depth,
 * the text of code and source blocks as they show it, and the text of a table's header and body cells as they show
 * it, also in rows never drawn. An inline image is a position no match takes; an image in a table cell shows its alt
 * text, which is searched, after its icon, which no match takes. Not searched: a table's hidden columns, block images,
 * the alt text of block and inline images, list markers, link addresses, tooltips and buttons. A match never runs
 * from one heading, paragraph, block or table cell into the next.
 *
 * <p><b>Matching.</b> The search is one piece: its words in their order, next to each other, so {@code der Bericht}
 * finds the places where both stand together, not every {@code der}. Whitespace at its start and its end is dropped,
 * and a run of whitespace inside it matches any run of whitespace in the text, a line break, a tab and a no-break
 * space included, so a phrase that wraps in the source or in a code block is still found. Case and accents are
 * ignored as the search inputs ignore them when they filter a list: {@code bericht} finds {@code Bericht},
 * {@code zurich} finds {@code Zürich} and {@code zürich} finds {@code Zurich}. Both sides are compared in lower case
 * ({@link java.util.Locale#ROOT}) and canonically decomposed without their combining marks, nothing more: the sharp s
 * and {@code ss}, a ligature and its letters, and a letter without a decomposition such as {@code ø} and {@code o}
 * stay apart. {@code null}, the empty text and whitespace only are no search. Matches do not overlap: after a match the
 * next one starts after its end, so {@code aa} finds two matches in {@code aaaa}.
 *
 * <p><b>Order and direction.</b> The matches are numbered from 0 from the top of the document, whatever the
 * direction; in a table its header first, then its rows in the order it shows them, each row's cells in the order of
 * the shown columns, so <em>next</em> walks through a table as the reader reads it. With at least one match there is
 * always a current one. A new search, after none or after one without matches, makes the first match current,
 * backward the last. A changed search keeps the current match while it still matches: forward the first match that
 * starts at or after the old current match's start becomes current, backward the last one that starts at or before
 * it, wrapping around to the first (backward the last). <em>Next</em> goes to the next match in the direction,
 * <em>previous</em> to the next one against it, both wrapping around at the ends. A change of the direction moves no
 * match. A new text, and every rebuild for a change of the code languages or the line limit, keeps the search and runs
 * it on the new text, the current match chosen as for a new search; the view does not scroll to it. A new version of
 * the text set by {@link #updateMarkdown(String)} goes on from the old current match's place instead (see <em>Text
 * updates</em>).
 *
 * <p><b>Table changes.</b> When the reader sorts a table, or moves, hides or shows one of its columns, the matches are
 * numbered anew in the order the table shows now, and the current match stays on its text. If its column is hidden,
 * the first match after it that is still there becomes current (backward the first before it), wrapping around; the
 * view does not scroll for it. A column shown again has its matches counted again. A column resized changes nothing
 * but the shapes of the marks.
 *
 * <p><b>Scrolling.</b> After next, previous and a new or changed search, the view scrolls to the current match if it
 * is not fully visible: so that the match's middle is at the middle of the visible area, as far as the scroll range
 * allows, at every zoom, also to a table row the view has never shown. In a code or source block whose line is wider
 * than the block, and in a table wider than the view, the block first scrolls sideways so that the match shows whole,
 * with a margin. A fully visible match scrolls nothing, so going through the matches on screen is calm. A search
 * without matches leaves the view where it is. A search set before the view is laid out scrolls at its first layout.
 *
 * <p><b>Marks.</b> Every match but the current one is marked in a quiet amber ({@code markdown-search-match}), muted
 * behind the light text in Nebula Dark, pale behind the dark text in Nebula Light, and the current one in a full amber
 * behind a dark text in both schemes ({@code markdown-search-current}): highlights like the selection's, in two
 * layers over the text of a heading, a paragraph, a code or source block or a table's header or body cell, which show
 * the matched characters again in the highlight's text colour; in a table cell too only the matched characters. The
 * selection lies above both, so the search never hides it, and the search never changes it: in a selected table cell
 * and in a table the text selection passes over, the highlights let the selection colour through and the matched
 * characters take the selection's text colour. The marks stay on their characters when the view is resized, wraps its
 * text anew or zooms, when a table is sorted, which fills its cells anew, or its columns are resized, and go when the
 * search is cleared. The matches of the whole document are found at once, but only the blocks, and in a table the
 * rows, within one visible height above or below the visible area build their marks, more as the view scrolls, so a
 * one-letter search in a large document stays quick.
 *
 * <h2>Source lines</h2>
 *
 * <p>The view tells an application which line of the Markdown text the reader is looking at, so that it can open
 * the text in an editor at that line: {@link #topSourceLine()} the line at the top of the visible area,
 * {@link #searchMatchSourceLine()} the line of the current search match. Lines count from 1, and each line ending,
 * {@code \n}, {@code \r\n} or {@code \r}, ends one. A line is a block's first line: a heading, a
 * paragraph (also one that is a block image), a quote's or a list item's paragraph, a thematic break; or, finer, a list
 * item's marker, a table row (the header row too) or a line of a code or source block. A line inside a wrapped or
 * multi-line paragraph is not told apart: the paragraph's first line counts. Both are read from the document as it is
 * laid out, so they follow the zoom, the margins, the reader's sorting of a table and {@link #updateMarkdown(String)}.
 * The context menu's {@link ContextMenuPlace place} tells the line under a right click the same way (see
 * {@link #contextMenuCallbackProperty()}).
 *
 * <h2>Style classes</h2>
 *
 * <p>The look comes from karthu-fx's theme ({@link org.karthu.fx.theme.ThemeManager}). These style classes are a
 * stable contract an application may style in its own stylesheet:
 * <table>
 *   <caption>Style classes of the Markdown view</caption>
 *   <tr><th>Style class</th><th>On</th></tr>
 *   <tr><td>{@code markdown-view}</td><td>the view itself</td></tr>
 *   <tr><td>{@code markdown-zoom-bar}</td><td>the zoom bar on top of the view, outside its scroll pane, with a line
 *       below it</td></tr>
 *   <tr><td>{@code markdown-zoom-group}</td><td>the frame of the zoom group around minus, the percent field with its
 *       chevron, and plus; with the pseudo-class {@code :focus-within} while the field has the focus or the list of
 *       zoom levels is open (the bar sets it for the open list too), which the theme draws as a text field's focus
 *       border</td></tr>
 *   <tr><td>{@code markdown-zoom-out}, {@code markdown-zoom-in}</td><td>minus and plus, {@code Button}s with the
 *       theme's {@code icon-button} class and a Bootstrap icon; the theme draws the group's dividers as their
 *       borders</td></tr>
 *   <tr><td>{@code markdown-zoom-field}</td><td>the percent field, a {@code TextField}</td></tr>
 *   <tr><td>{@code markdown-zoom-levels}</td><td>the chevron, a {@code Button} with the theme's {@code icon-button}
 *       class, which opens the list of zoom levels: a {@code ListView} in a popup with the class
 *       {@code combo-box-popup}, so it takes the theme's look of a combo box's list</td></tr>
 *   <tr><td>{@code markdown-zoom-slider}</td><td>the slider, a {@code Slider}; a region {@code track-fill} inside its
 *       {@code track} fills the track up to the thumb</td></tr>
 *   <tr><td>{@code markdown-zoom-reset}</td><td>the <em>Reset</em> button, a flat text {@code Button}</td></tr>
 *   <tr><td>{@code markdown-document}</td><td>the box of all blocks inside the view's scroll pane (margins and
 *       spacing between blocks), scaled by the zoom; the view's margins, while set, replace its left and right
 *       padding (see <em>Margins</em>)</td></tr>
 *   <tr><td>{@code markdown-heading} and {@code markdown-heading-1} to {@code markdown-heading-6}</td>
 *       <td>a heading's {@code TextFlow}</td></tr>
 *   <tr><td>{@code markdown-paragraph}</td><td>a paragraph's {@code TextFlow}</td></tr>
 *   <tr><td>{@code markdown-text}</td><td>every {@code Text} run</td></tr>
 *   <tr><td>{@code markdown-strong}, {@code markdown-emphasis}, {@code markdown-code}</td>
 *       <td>{@code Text} runs with that style, combined as needed (bold code carries {@code markdown-strong} and
 *       {@code markdown-code})</td></tr>
 *   <tr><td>{@code markdown-link}</td><td>the {@code Text} runs of a link, combined with the classes of the styles
 *       inside it (bold link text carries {@code markdown-link} and {@code markdown-strong}); with a hand cursor and
 *       the address as tooltip</td></tr>
 *   <tr><td>{@code markdown-bullet-list}, {@code markdown-ordered-list}</td>
 *       <td>a list: a box of its item rows; with the pseudo-class {@code :loose} when blank lines separate its items
 *       or the blocks of an item in the source</td></tr>
 *   <tr><td>{@code markdown-list-item}</td><td>an item row: a box of the item's blocks, indented by its left
 *       padding, which holds the marker</td></tr>
 *   <tr><td>{@code markdown-list-marker}</td><td>an item's marker, a {@code Label}: <code>&bull;</code> in a bullet
 *       list, the number and its delimiter ({@code 3.}, {@code 3)}) in an ordered list; a bullet task item has
 *       none</td></tr>
 *   <tr><td>{@code markdown-task-list}</td><td>beside {@code markdown-bullet-list} or {@code markdown-ordered-list}: a
 *       list with at least one task item; an ordered one indents its items further, for number and box, and pads
 *       the numbers of its plain items so that they stand where its tasks' numbers do</td></tr>
 *   <tr><td>{@code markdown-task-item}</td><td>beside {@code markdown-list-item}: a task item's row, with the
 *       pseudo-class {@code :checked} while the task is done</td></tr>
 *   <tr><td>{@code markdown-task-box}</td><td>a task's check box in place of the bullet or right of the number, a
 *       {@code Region} drawn like the theme's check box, with a region {@code mark} inside it for the tick; with the
 *       pseudo-class {@code :checked} while the task is done</td></tr>
 *   <tr><td>{@code markdown-quote}</td><td>a block quote: a box of the quote's blocks, with a bar on its left</td></tr>
 *   <tr><td>{@code markdown-separator}</td><td>a thematic break, a horizontal {@code Separator}</td></tr>
 *   <tr><td>{@code markdown-code-block}</td><td>a fenced code block: a box with a {@code ScrollPane} of its own that
 *       scrolls the code sideways, and a copy button over its top right corner</td></tr>
 *   <tr><td>{@code markdown-code-copy}</td><td>a code block's copy button, a {@code Button} with the theme's
 *       {@code icon-button} class and a clipboard icon</td></tr>
 *   <tr><td>{@code markdown-table}</td><td>a table, a {@code TableView} with the theme's table look, inside the
 *       view in the body text's 13px, without zebra stripes, a cell's text at its top: each header and body cell is a
 *       {@code TextFlow} of {@code Text} runs with the classes above, as a paragraph's; the header shows the theme's
 *       sort arrow and its menu button ({@code show-hide-columns-button}, the theme's flat button with a plus sign,
 *       whose menu is the theme's context menu); selected cells take the theme's cell selection colours, the focused
 *       cell its accent frame, and their runs the selection's text colour; with the pseudo-class
 *       {@code :selected-whole} while the text selection passes over the table, which the theme draws with every
 *       body cell and column header in the selection colour and all runs in the selection's text colour; a table
 *       wider than the view lies in a box that scrolls it sideways under a horizontal {@code ScrollBar}</td></tr>
 *   <tr><td>{@code markdown-source-block}</td><td>a source block, which shows a block the view does not render (an
 *       HTML block, indented code) as its source lines: a box like a code block's, without a copy button</td></tr>
 *   <tr><td>{@code markdown-block-content}</td><td>the {@code TextFlow} inside a code or source block's scroll pane
 *       that pads its text, never wrapped, with tab stops every four columns: {@code Text} runs with
 *       {@code markdown-text} and {@code markdown-code} in a code block, {@code markdown-text} alone in a source
 *       block, one run for the whole text; in a coloured code block a run per piece and per plain stretch between
 *       pieces</td></tr>
 *   <tr><td>{@code code-comment}, {@code code-keyword}, {@code code-string}, {@code code-number},
 *       {@code code-type}, {@code code-function}, {@code code-variable}, {@code code-property}, {@code code-tag},
 *       {@code code-doc}, {@code code-annotation}, {@code code-literal}, {@code code-constant}, {@code code-escape},
 *       {@code code-attribute}, {@code code-operator}, {@code code-punctuation}, {@code code-heading},
 *       {@code code-link}, {@code code-code}, {@code code-list}, {@code code-quote}, {@code code-emphasis}</td>
 *       <td>the run of a piece in a coloured code block, beside {@code markdown-text} and {@code markdown-code}: the
 *       class of its {@link CodeCategory category} ({@link CodeCategory#styleClass()}) and of every category its
 *       fallback chain passes (see <em>Code colours</em>), its fill from the category's variable</td></tr>
 *   <tr><td>{@code markdown-selection}</td><td>the highlight behind selected characters, a {@code Path}, in a layer
 *       over the runs of a heading's, a paragraph's or a code or source block's flow (an unmanaged {@code TextFlow}
 *       child of the flow, clipped to the selection) that shows the selected characters again on the highlight. Its
 *       {@code -fx-fill} is the selection's colour, its own CSS property {@code -fx-highlight-text-fill} the colour of
 *       the selected characters. A selected image, block or inline, has one of its own, a {@code Path} over its
 *       picture, placeholder or error box, which the theme makes half see-through ({@code -fx-opacity})</td></tr>
 *   <tr><td>{@code markdown-search-match}</td><td>the highlight behind every match of the search but the current
 *       one, a {@code Path}, in a layer over the runs of a heading's, a paragraph's, a code or source block's or a
 *       table's header or body cell's flow (an unmanaged {@code TextFlow} child of the flow, clipped to the matches)
 *       that shows the matched characters again on the highlight, below the current match's layer and the
 *       selection's. Like {@code markdown-selection}, its {@code -fx-fill} is the highlight's colour and its
 *       {@code -fx-highlight-text-fill} the colour of the characters on it; in a selected table cell and a table
 *       {@code :selected-whole} the theme makes the fill transparent and the characters the selection's text
 *       colour</td></tr>
 *   <tr><td>{@code markdown-search-current}</td><td>the highlight behind the current match of the search, a
 *       {@code Path} in a layer of its own like the other matches', above theirs and below the selection's, with the
 *       same two properties</td></tr>
 *   <tr><td>{@code markdown-image-block}</td><td>a block image, a {@code Region} that shows its picture, its
 *       placeholder or its error box at its top left, and the open button over the picture's top right corner while
 *       the pointer is over the image</td></tr>
 *   <tr><td>{@code markdown-image}</td><td>the picture of a block or inline image, an {@code ImageView} the view
 *       sizes: a bitmap the view computed at the shown size, the decoded image where it shows at its natural size or
 *       beyond, or a playing GIF, the alt text its accessible text, the title its tooltip</td></tr>
 *   <tr><td>{@code markdown-image-open}</td><td>a block image's open button, a {@code Button} with the theme's
 *       {@code icon-button} class and the Bootstrap fullscreen icon, 4 pixels inside the picture's top right
 *       corner, shown while the pointer is over the image</td></tr>
 *   <tr><td>{@code markdown-image-inline}</td><td>an inline image, a {@code Region} that is one node of its line's
 *       {@code TextFlow}, its bottom on the baseline</td></tr>
 *   <tr><td>{@code markdown-image-placeholder}</td><td>what an image shows until it is decoded: in a block a
 *       {@code Label} with an image icon and the alt text, inline a {@code Label} with the icon alone</td></tr>
 *   <tr><td>{@code markdown-image-error}</td><td>what an image shows when it cannot be shown: in a block an
 *       {@code HBox} with a missing-image icon, a {@code Label} with the alt text and one with the note, inline a
 *       {@code Label} with the icon and the alt text, the image's address as the tooltip of both</td></tr>
 *   <tr><td>{@code markdown-image-cell}</td><td>an image in a table cell: its icon, a {@code FontIcon}, and its alt
 *       text, a {@code Text} run with {@code markdown-text}, in the link's colour, underlined under the
 *       pointer</td></tr>
 *   <tr><td>{@code markdown-image-window}</td><td>the root of the full size window's scene, a {@code BorderPane}:
 *       the bar on top, the image area below</td></tr>
 *   <tr><td>{@code markdown-image-bar}</td><td>the window's bar, an {@code HBox}: <em>Fit</em> and <em>100 %</em>,
 *       the zoom, and at its right end <em>Copy image</em> and <em>Save as</em></td></tr>
 *   <tr><td>{@code markdown-image-fit}, {@code markdown-image-actual-size}</td><td>the bar's <em>Fit</em> and
 *       <em>100 %</em>, the theme's {@code Button}s</td></tr>
 *   <tr><td>{@code markdown-image-zoom}</td><td>the bar's zoom in whole percent, a {@code Label}</td></tr>
 *   <tr><td>{@code markdown-image-copy}, {@code markdown-image-save}</td><td>the bar's <em>Copy image</em> and
 *       <em>Save as</em>, the theme's {@code Button}s, disabled while there is no image to copy or no file to
 *       save</td></tr>
 *   <tr><td>{@code markdown-image-viewer}</td><td>the window's image area, a {@code Region} that shows the image
 *       fitted or zoomed and moves it, with an open hand, a closed one while dragging</td></tr>
 *   <tr><td>{@code markdown-image-note}</td><td>a {@code Label} in the middle of the image area that shows the note
 *       of a missing, undecodable or too large image where no image can show</td></tr>
 * </table>
 *
 * <p>Colours come from the theme's tokens, some of them the view's own: {@code -fx-code-text-color}, the colour of
 * inline code, {@code -fx-code-block-text-color}, the colour of plain code in code blocks (the code colour in Nebula
 * Dark, black in Nebula Light), {@code -fx-code-background-color}, the background of code and source blocks and of
 * the copy buttons, {@code -fx-markdown-table-row-odd}, the colour of a table's odd rows, the colours of coloured
 * code, one per {@link CodeCategory category} (see <em>Code colours</em>), and the colours of the search's marks:
 * {@code -fx-markdown-search-color} and {@code -fx-markdown-search-text-color}, the highlight and the characters of
 * every match but the current one, a quiet amber behind the text colour (muted in Nebula Dark, pale in Nebula Light),
 * and {@code -fx-markdown-search-current-color} and {@code -fx-markdown-search-current-text-color}, those of the
 * current match, a full amber behind a dark text, both apart from the blue of the selection. Links take the theme's
 * {@code -fx-link-color}. A selection, of
 * characters or of table cells, takes the theme's {@code -fx-selection-color} and {@code -fx-selection-text-color},
 * as a selected row of a list does. The view's content lies on the theme's raised surface,
 * {@code -fx-raised-color}. A table's rows take the field colour of the theme's tables, {@code -fx-field-color}, the
 * odd ones too: inside the view the theme's {@code -fx-table-row-odd} is {@code -fx-markdown-table-row-odd}, which
 * the theme sets to the rows' colour. Images add no token: an image's placeholder and error box and the open button
 * take {@code -fx-code-background-color} and {@code -fx-text-secondary-color}, an image in a table cell
 * {@code -fx-link-color}, and the full size window {@code -fx-canvas-color} and, for its bar,
 * {@code -fx-surface-color}.
 *
 * <p>Code is shown in the first installed of "JetBrains Mono NL", "Cascadia Mono" and "Consolas", families that draw
 * no ligatures, else in JavaFX's {@code Monospaced} font: JavaFX CSS cannot fall back from one font family to the
 * next, so the view sets the family on code runs itself, as an inline style that a stylesheet does not override,
 * inline code, code blocks and source blocks alike.
 */
public class MarkdownView extends BaseComponent {

    private static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination SELECT_ALL = new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN);

    private static final double PREF_WIDTH = 480;
    private static final double PREF_HEIGHT = 360;
    private static final double MIN_SIZE = 40;

    private static final String DEFAULT_COPY_MARKDOWN_TEXT = "Copy Markdown";
    private static final String DEFAULT_COPY_CODE_TEXT = "Copy code";
    private static final String DEFAULT_COPY_TEXT = "Copy";
    private static final String DEFAULT_COPY_WITH_HEADER_TEXT = "Copy with header";
    private static final String DEFAULT_SELECT_COLUMN_TEXT = "Select column";
    private static final String DEFAULT_TASK_OPEN_TEXT = "Open";
    private static final String DEFAULT_TASK_DONE_TEXT = "Done";
    private static final String DEFAULT_ZOOM_OUT_TEXT = "Zoom out";
    private static final String DEFAULT_ZOOM_IN_TEXT = "Zoom in";
    private static final String DEFAULT_ZOOM_LEVELS_TEXT = "Zoom levels";
    private static final String DEFAULT_RESET_ZOOM_TEXT = "Reset";
    private static final String DEFAULT_IMAGE_MISSING_TEXT = "Image not found";
    private static final String DEFAULT_IMAGE_UNSUPPORTED_TEXT = "Image format not supported";
    private static final String DEFAULT_IMAGE_TOO_LARGE_TEXT = "Image too large";
    private static final String DEFAULT_OPEN_IMAGE_TEXT = "Open image";
    private static final String DEFAULT_FIT_IMAGE_TEXT = "Fit";
    private static final String DEFAULT_ACTUAL_IMAGE_SIZE_TEXT = "100%";
    private static final String DEFAULT_COPY_IMAGE_TEXT = "Copy image";
    private static final String DEFAULT_SAVE_IMAGE_TEXT = "Save as...";

    /**
     * The default of the line limit: a code block with more lines stays in one colour. The one place of the number,
     * which the JavaDoc of the property shows. A coloured block of 500 lines shows in about 0.1 s, one of 5000 in
     * about 15 s: the time goes into the layout of the block's one text flow.
     */
    private static final int DEFAULT_CODE_HIGHLIGHT_LINE_LIMIT = 500;

    private final StringProperty markdown = new SimpleStringProperty(this, "markdown");
    private final ObjectProperty<Consumer<String>> onLinkAction = new SimpleObjectProperty<>(this, "onLinkAction");
    private final ObjectProperty<ContextMenuCallback> contextMenuCallback =
            new SimpleObjectProperty<>(this, "contextMenuCallback");
    private final StringProperty copyMarkdownText = new SimpleStringProperty(this, "copyMarkdownText",
            DEFAULT_COPY_MARKDOWN_TEXT);
    private final StringProperty copyCodeText = new SimpleStringProperty(this, "copyCodeText",
            DEFAULT_COPY_CODE_TEXT);
    private final StringProperty copyText = new SimpleStringProperty(this, "copyText", DEFAULT_COPY_TEXT);
    private final StringProperty copyWithHeaderText = new SimpleStringProperty(this, "copyWithHeaderText",
            DEFAULT_COPY_WITH_HEADER_TEXT);
    private final StringProperty selectColumnText = new SimpleStringProperty(this, "selectColumnText",
            DEFAULT_SELECT_COLUMN_TEXT);
    private final StringProperty taskOpenText = new SimpleStringProperty(this, "taskOpenText",
            DEFAULT_TASK_OPEN_TEXT);
    private final StringProperty taskDoneText = new SimpleStringProperty(this, "taskDoneText",
            DEFAULT_TASK_DONE_TEXT);
    private final DoubleProperty zoom = new ZoomProperty(this);
    private final BooleanProperty zoomBarVisible = new SimpleBooleanProperty(this, "zoomBarVisible", true);
    private final StringProperty zoomOutText = new SimpleStringProperty(this, "zoomOutText", DEFAULT_ZOOM_OUT_TEXT);
    private final StringProperty zoomInText = new SimpleStringProperty(this, "zoomInText", DEFAULT_ZOOM_IN_TEXT);
    private final StringProperty zoomLevelsText = new SimpleStringProperty(this, "zoomLevelsText",
            DEFAULT_ZOOM_LEVELS_TEXT);
    private final StringProperty resetZoomText = new SimpleStringProperty(this, "resetZoomText",
            DEFAULT_RESET_ZOOM_TEXT);
    private final DoubleProperty leftMargin = new SimpleDoubleProperty(this, "leftMargin", Double.NaN);
    private final DoubleProperty rightMargin = new SimpleDoubleProperty(this, "rightMargin", Double.NaN);
    private final DocumentBox document = new DocumentBox(leftMargin, rightMargin);
    private final ZoomBox zoomBox = new ZoomBox(document);
    private final ScrollPane scrollPane = new ScrollPane(zoomBox);

    /**
     * The selection of text across the document: headings, paragraphs, code and source blocks, tables as a whole.
     */
    private final DocumentSelection documentSelection = new DocumentSelection(document, scrollPane,
            text -> copy(text), holder -> selectionStarted(holder));

    /**
     * The languages whose code blocks the view colours, by their lower-case names: the built-in ones at first.
     */
    private final ObservableMap<String, CodeLexer> codeLanguages = FXCollections.observableMap(
            BuiltInLanguages.lexers());

    private final IntegerProperty codeHighlightLineLimit = new SimpleIntegerProperty(this, "codeHighlightLineLimit",
            DEFAULT_CODE_HIGHLIGHT_LINE_LIMIT);

    /**
     * Which pieces of a code block the view colours, by its languages and its line limit.
     */
    private final CodeColoring codeColoring = new CodeColoring(codeLanguages, codeHighlightLineLimit);

    private final StringProperty imageBase = new SimpleStringProperty(this, "imageBase");

    private final BooleanProperty remoteImages = new SimpleBooleanProperty(this, "remoteImages", false);

    /**
     * The view's own image loader, which follows the base address.
     */
    private final ViewImageLoader viewImageLoader = new ViewImageLoader();

    private final ObjectProperty<ImageLoader> imageLoader = new SimpleObjectProperty<>(this, "imageLoader",
            viewImageLoader);

    private final StringProperty imageMissingText = new SimpleStringProperty(this, "imageMissingText",
            DEFAULT_IMAGE_MISSING_TEXT);
    private final StringProperty imageUnsupportedText = new SimpleStringProperty(this, "imageUnsupportedText",
            DEFAULT_IMAGE_UNSUPPORTED_TEXT);
    private final StringProperty imageTooLargeText = new SimpleStringProperty(this, "imageTooLargeText",
            DEFAULT_IMAGE_TOO_LARGE_TEXT);
    private final IntegerProperty imagePixelLimit = new SimpleIntegerProperty(this, "imagePixelLimit", 0);
    private final BooleanProperty imageAnimation = new SimpleBooleanProperty(this, "imageAnimation", true);
    private final StringProperty openImageText = new SimpleStringProperty(this, "openImageText",
            DEFAULT_OPEN_IMAGE_TEXT);
    private final StringProperty fitImageText = new SimpleStringProperty(this, "fitImageText",
            DEFAULT_FIT_IMAGE_TEXT);
    private final StringProperty actualImageSizeText = new SimpleStringProperty(this, "actualImageSizeText",
            DEFAULT_ACTUAL_IMAGE_SIZE_TEXT);
    private final StringProperty copyImageText = new SimpleStringProperty(this, "copyImageText",
            DEFAULT_COPY_IMAGE_TEXT);
    private final StringProperty saveImageText = new SimpleStringProperty(this, "saveImageText",
            DEFAULT_SAVE_IMAGE_TEXT);

    /**
     * The notes of an image that cannot be shown and the pixel limit, which the document's images and the full size
     * window share.
     */
    private final ImageNotes imageNotes = new ImageNotes(imageMissingText, imageUnsupportedText, imageTooLargeText,
            imagePixelLimit);

    /**
     * The images of the document at work: their blocks, their loading and decoding in the background, and what their
     * size depends on beyond their width.
     */
    private final DocumentImages images = new DocumentImages(this, imageLoader, imageNotes, openImageText, scrollPane,
            zoomBox, () -> keepReadingPosition());

    private final MarkdownRenderer renderer = new MarkdownRenderer(address -> followLink(address),
            text -> copy(text), copyCodeText, holder -> selectionStarted(holder), documentSelection, codeColoring,
            images, taskOpenText, taskDoneText);

    /**
     * The zoom at work: applies the zoom property to the document, keeping the reader's place, and zooms for the
     * reader by the bar, the wheel and the shortcuts while the bar shows.
     */
    private final DocumentZoom documentZoom = new DocumentZoom(zoom, zoomBarVisible, scrollPane, zoomBox,
            documentSelection, () -> requestLayout());

    private final ZoomBar zoomBar = new ZoomBar(documentZoom, zoomOutText, zoomInText, zoomLevelsText,
            resetZoomText);

    private final StringProperty searchText = new SimpleStringProperty(this, "searchText");
    private final ReadOnlyIntegerWrapper searchMatchCount = new ReadOnlyIntegerWrapper(this, "searchMatchCount");
    private final ReadOnlyIntegerWrapper searchMatchIndex = new ReadOnlyIntegerWrapper(this, "searchMatchIndex", -1);
    private final ObjectProperty<SearchDirection> searchDirection = new SimpleObjectProperty<>(this,
            "searchDirection", SearchDirection.FORWARD);

    /**
     * The search at work: the matches of the search text in the document, the current one, its marks and the
     * scrolling to it.
     */
    private final DocumentSearch documentSearch = new DocumentSearch(searchText, searchMatchCount, searchMatchIndex,
            searchDirection, documentSelection, scrollPane);

    /**
     * What holds the view's one selection: the document selection or a table; {@code null} if nothing has held one
     * since the text was set.
     */
    private SelectionHolder selectionHolder;

    /**
     * Whether the text changes by {@link #updateMarkdown(String)}: the rebuild keeps the reader's place.
     */
    private boolean updating;

    /**
     * The view's context menu and its entries. The menu belongs to the view's scroll pane; the entries it holds
     * depend on where the request comes from ({@link #prepareContextMenu(EventTarget)}).
     */
    private final ContextMenu contextMenu = new ContextMenu();
    private final MenuItem copyEntry = new MenuItem();
    private final MenuItem copyWithHeaderEntry = new MenuItem();
    private final MenuItem selectColumnEntry = new MenuItem();
    private final SeparatorMenuItem separator = new SeparatorMenuItem();
    private final MenuItem copyMarkdownEntry = new MenuItem();

    /**
     * The context menu's entry that opens the image the menu was requested on, and the separator below it.
     */
    private final MenuItem openImageEntry = new MenuItem();
    private final SeparatorMenuItem openImageSeparator = new SeparatorMenuItem();

    /**
     * The view's full size window, made when the reader first opens an image; {@code null} before.
     */
    private ImageWindow imageWindow;

    /**
     * Where copied text goes: the system clipboard, a recorder in tests.
     */
    private TextClipboard clipboard = TextClipboard.system();

    /**
     * Creates an empty view.
     */
    public MarkdownView() {
        this(null);
    }

    /**
     * Creates a view showing the given Markdown text.
     *
     * @param markdown the Markdown text; {@code null} gives an empty view
     */
    public MarkdownView(String markdown) {
        getStyleClass().add("markdown-view");
        document.getStyleClass().add("markdown-document");
        scrollPane.setFitToWidth(true);
        scrollPane.setContextMenu(contextMenu());
        // A filter: it runs before the scroll pane, where the request bubbles up to, shows the menu.
        addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> contextMenuRequested(event));
        // A handler: a code or source block and a table with the focus take their own shortcut keys first.
        addEventHandler(KeyEvent.KEY_PRESSED, event -> keyPressed(event));
        zoomBar.visibleProperty().bind(zoomBarVisible);
        zoomBar.managedProperty().bind(zoomBarVisible);
        zoomBarVisible.addListener((observable, wasVisible, visible) -> {
            if (!visible) {
                zoomBar.closeLevels();
            }
        });
        getChildren().addAll(zoomBar, scrollPane);
        this.markdown.set(markdown);
        rebuild();
        this.markdown.addListener((observable, oldText, newText) -> rebuild());
        codeLanguages.addListener((MapChangeListener<String, CodeLexer>) change -> rebuild());
        codeHighlightLineLimit.addListener((observable, oldLimit, newLimit) -> rebuild());
        // The visible height at the zoom limits the images' size.
        zoom.addListener(observable -> images.limitsChanged());
        // New margins wrap the text anew, as a new zoom does: the line at the top stays there.
        leftMargin.addListener(observable -> keepReadingPosition());
        rightMargin.addListener(observable -> keepReadingPosition());
        // Where images come from and the pixel limit: the view's own loader follows the base and the internet switch
        // first, then every change loads the shown images anew, in place. The one place of that rule; a new source of
        // images joins here.
        imageBase.addListener((observable, oldBase, newBase) -> {
            viewImageLoader.setBase(newBase);
            images.reload();
        });
        remoteImages.addListener((observable, wasOn, on) -> {
            viewImageLoader.setRemote(on);
            images.reload();
        });
        imageLoader.addListener((observable, oldLoader, newLoader) -> images.reload());
        imagePixelLimit.addListener((observable, oldLimit, newLimit) -> images.reload());
        // Whether animated GIFs play in the document: a change shows the GIFs shown anew from the bytes they keep.
        images.setAnimation(imageAnimation.get());
        imageAnimation.addListener((observable, wasOn, on) -> images.setAnimation(on));
        images.setWindow(request -> openImageWindow(request));
    }

    /**
     * The Markdown text the view shows. Every change rebuilds the rendered content and scrolls the view back to the
     * top left, except one by {@link #updateMarkdown(String)}, which keeps the reader's place; {@code null} and the
     * empty text give an empty view. A search keeps running on the new text, see <em>Search</em> in the class
     * description.
     *
     * @return the Markdown text property
     */
    public final StringProperty markdownProperty() {
        return markdown;
    }

    /**
     * Returns the Markdown text the view shows.
     *
     * @return the Markdown text, may be {@code null}
     */
    public final String getMarkdown() {
        return markdown.get();
    }

    /**
     * Sets the Markdown text the view shows, a new document, shown from the top left. A new version of the shown text
     * that should keep the reader's place is set with {@link #updateMarkdown(String)}.
     *
     * @param markdown the Markdown text; {@code null} gives an empty view
     */
    public final void setMarkdown(String markdown) {
        this.markdown.set(markdown);
    }

    /**
     * Sets a new version of the shown text and keeps the reader's place: the text changes as by
     * {@link #setMarkdown(String)}, but where a new document starts at the top, the view stays at the paragraph the
     * reader was reading, also when text above it was added or removed. Made for an application that reloads a
     * document when its file changes; another document is shown with {@code setMarkdown}. See <em>Text updates</em> in
     * the class description.
     *
     * <p>The place is the line or the point at the top edge of the visible area, noted as a zoom notes it, before the
     * new text is rendered. The new text's block in its place is the block of the same kind (a heading's or a
     * paragraph's text, a code or source block, a table) with the same text, as a copy gives it, nearest to the old
     * block's number in the document; a table's text taken in the source's order with every column, so a table the
     * reader sorted or rearranged is found too. Without one, the block with the old number, the last one if the new
     * text is shorter. Block images are not counted. A line keeps its character position, limited to the new block's
     * length, and the top edge cuts it as far down as before; a block is cut as far down, a gap between blocks keeps
     * its height. The next layout scrolls there, as far as the scroll range allows, and leaves the sideways scroll
     * position as it is. An image that arrives later above the place keeps it, as always.
     *
     * <p>An image of the new text whose address, as written, an image of the shown text has starts with what that
     * image showed, its picture at its size, an animated GIF playing, or its error box, the first of them that showed
     * one where the address occurs several times, instead of its placeholder. Every image of the new text is loaded
     * again through the loader of that moment and replaced in place when its result arrives, as a reload replaces it,
     * so nothing flashes and the text does not jump while the pictures arrive. See <em>Images</em>, <em>Loading</em>
     * in the class description.
     *
     * <p>A search goes on: it runs on the new text, and the first match at or after the old current match's place, its
     * block found again as the reader's place is found, becomes current, in the search's direction and wrapping around;
     * in a table the place lies in the same cell as the source has it, however the reader had sorted or rearranged the
     * table. The view does not scroll to it. Everything else is as after {@code setMarkdown}: the selection is cleared,
     * tables start unsorted with all columns shown at their fitted widths, the context menu forgets its block, and the
     * zoom stays.
     *
     * <p>The same text changes nothing, as setting the same value does. Before the view is laid out, and where the
     * shown or the new text has no block but block images, it shows the new text from the top, as {@code setMarkdown}
     * does; its images keep what they showed all the same.
     *
     * @param markdown the new version of the Markdown text; {@code null} gives an empty view
     * @throws RuntimeException while the text property is bound, as {@code setMarkdown} throws then
     * @since 1.1.0-RC2
     */
    public final void updateMarkdown(String markdown) {
        updating = true;
        try {
            this.markdown.set(markdown);
        } finally {
            updating = false;
        }
    }

    /**
     * What a click on a link does. The handler receives the link's address as written in the Markdown source: the
     * destination of {@code [text](address)} without the angle brackets of {@code <address>} and with backslash
     * escapes resolved, the address of an autolink {@code <https://...>} or a bare URL as it stands; nothing is
     * encoded, resolved or checked. The application decides what to do with it: open a browser, show another
     * document, nothing. Without a handler ({@code null}, the default), a click does nothing: the view never opens
     * anything itself.
     *
     * <p>A link follows a single click with the primary button; the second click of a double click is ignored, and so
     * is a click that ends a drag, which selects text. The handler is read at the time of the click, so it can be set
     * or changed at any time.
     *
     * @return the link handler property
     */
    public final ObjectProperty<Consumer<String>> onLinkActionProperty() {
        return onLinkAction;
    }

    /**
     * Returns the handler of link clicks.
     *
     * @return the handler, {@code null} if none is set
     */
    public final Consumer<String> getOnLinkAction() {
        return onLinkAction.get();
    }

    /**
     * Sets the handler of link clicks, see {@link #onLinkActionProperty()}.
     *
     * @param handler the handler, receiving a clicked link's address; {@code null} for none
     */
    public final void setOnLinkAction(Consumer<String> handler) {
        onLinkAction.set(handler);
    }

    /**
     * What the application adds to the view's context menu: a callback called on every opening of the menu, after
     * the view's own entries are in place (see <em>Copying</em> in the class description). It receives the menu's
     * entries, to add to, remove from or reorder, and the {@link ContextMenuPlace place} of the request, which tells
     * the source line under the right click (see <em>Source lines</em>), empty where the click hit no block, as in the
     * margins. The view builds its entries anew on every opening, so the callback's changes last for one opening.
     * Without a callback ({@code null}, the default), the menu holds the view's own entries alone. The callback is
     * read at each opening, so it can be set or changed at any time.
     *
     * @return the context menu callback property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<ContextMenuCallback> contextMenuCallbackProperty() {
        return contextMenuCallback;
    }

    /**
     * Returns what the application adds to the view's context menu, see {@link #contextMenuCallbackProperty()}.
     *
     * @return the callback, {@code null} if none is set
     * @since 1.1.0-RC2
     */
    public final ContextMenuCallback getContextMenuCallback() {
        return contextMenuCallback.get();
    }

    /**
     * Sets what the application adds to the view's context menu, see {@link #contextMenuCallbackProperty()}.
     *
     * @param callback the callback, called on every opening of the menu; {@code null} for none
     * @since 1.1.0-RC2
     */
    public final void setContextMenuCallback(ContextMenuCallback callback) {
        contextMenuCallback.set(callback);
    }

    /**
     * The zoom of the document, as a factor: 1.0, the default, shows it at 100 %, 1.25 at 125 %. A value outside 0.5
     * to 2.0 counts as the nearest limit, every value is rounded half up to whole percent (two decimals of the
     * factor), and not a number counts as 1.0. Setting the property stores the limited and rounded value, so
     * {@link #getZoom()} returns the zoom the view shows; a bound property keeps the value it is bound to, and the
     * view shows the limited and rounded value of it. A change keeps the line at the top of the visible area at the
     * top; the zoom stays when the text changes. See <em>Zoom</em> in the class description.
     *
     * <p>Bound both ways ({@code bindBidirectional}) to a property of the application, the view applies the limited and
     * rounded value of what the application sets, and this property holds it, while the application's property may
     * keep the value it was set to: set to 1.234, it stays 1.234 beside the view's 1.23, set to 5.0 it stays 5.0 beside
     * the view's 2.0. JavaFX's bidirectional binding does not hand a value changed on its way back to the property that
     * started the change. A zoom of the reader, by the zoom bar, the wheel or the shortcuts, reaches the application's
     * property as it is, always a whole percent within the range.
     *
     * @return the zoom property
     */
    public final DoubleProperty zoomProperty() {
        return zoom;
    }

    /**
     * Returns the zoom of the document, see {@link #zoomProperty()}.
     *
     * @return the factor, 1.0 for 100 %; the value it is bound to while the property is bound
     */
    public final double getZoom() {
        return zoom.get();
    }

    /**
     * Sets the zoom of the document, see {@link #zoomProperty()}: limited to 0.5 to 2.0 and rounded half up to whole
     * percent, not a number as 1.0.
     *
     * @param zoom the factor, 1.0 for 100 %
     */
    public final void setZoom(double zoom) {
        this.zoom.set(zoom);
    }

    /**
     * Whether the zoom bar shows on top of the view, true by default. While it is false the bar is neither shown nor
     * laid out, the document takes its place, and the reader cannot zoom: the shortcut key plus the mouse wheel
     * scrolls as without a zoom, and the zoom shortcuts do nothing and pass on to the application; the application
     * still can zoom, through {@link #zoomProperty()}. Hiding the bar closes its list of zoom levels. See <em>Zoom
     * bar</em> in the class description.
     *
     * @return the property of the bar's visibility
     */
    public final BooleanProperty zoomBarVisibleProperty() {
        return zoomBarVisible;
    }

    /**
     * Returns whether the zoom bar shows on top of the view.
     *
     * @return true while it shows, the default
     */
    public final boolean isZoomBarVisible() {
        return zoomBarVisible.get();
    }

    /**
     * Shows or hides the zoom bar, see {@link #zoomBarVisibleProperty()}.
     *
     * @param visible true to show it
     */
    public final void setZoomBarVisible(boolean visible) {
        zoomBarVisible.set(visible);
    }

    /**
     * The tooltip of the zoom bar's minus button, {@code Zoom out} by default; it is the button's accessible text
     * too. A change applies at once.
     *
     * @return the tooltip property of the minus button
     */
    public final StringProperty zoomOutTextProperty() {
        return zoomOutText;
    }

    /**
     * Returns the tooltip of the zoom bar's minus button.
     *
     * @return the tooltip, {@code Zoom out} by default
     */
    public final String getZoomOutText() {
        return zoomOutText.get();
    }

    /**
     * Sets the tooltip of the zoom bar's minus button, for example to match the application's language.
     *
     * @param text the tooltip
     */
    public final void setZoomOutText(String text) {
        zoomOutText.set(text);
    }

    /**
     * The tooltip of the zoom bar's plus button, {@code Zoom in} by default; it is the button's accessible text too.
     * A change applies at once.
     *
     * @return the tooltip property of the plus button
     */
    public final StringProperty zoomInTextProperty() {
        return zoomInText;
    }

    /**
     * Returns the tooltip of the zoom bar's plus button.
     *
     * @return the tooltip, {@code Zoom in} by default
     */
    public final String getZoomInText() {
        return zoomInText.get();
    }

    /**
     * Sets the tooltip of the zoom bar's plus button, for example to match the application's language.
     *
     * @param text the tooltip
     */
    public final void setZoomInText(String text) {
        zoomInText.set(text);
    }

    /**
     * The tooltip of the zoom bar's chevron, which opens the list of zoom levels, {@code Zoom levels} by default; it
     * is the accessible text of the chevron and of the percent field too. A change applies at once.
     *
     * @return the tooltip property of the chevron
     */
    public final StringProperty zoomLevelsTextProperty() {
        return zoomLevelsText;
    }

    /**
     * Returns the tooltip of the zoom bar's chevron.
     *
     * @return the tooltip, {@code Zoom levels} by default
     */
    public final String getZoomLevelsText() {
        return zoomLevelsText.get();
    }

    /**
     * Sets the tooltip of the zoom bar's chevron, also the accessible text of the percent field, for example to match
     * the application's language.
     *
     * @param text the tooltip
     */
    public final void setZoomLevelsText(String text) {
        zoomLevelsText.set(text);
    }

    /**
     * The label of the zoom bar's button that sets the zoom back to 100 %, {@code Reset} by default. A change applies
     * at once.
     *
     * @return the label property of the <em>Reset</em> button
     */
    public final StringProperty resetZoomTextProperty() {
        return resetZoomText;
    }

    /**
     * Returns the label of the zoom bar's <em>Reset</em> button.
     *
     * @return the label, {@code Reset} by default
     */
    public final String getResetZoomText() {
        return resetZoomText.get();
    }

    /**
     * Sets the label of the zoom bar's <em>Reset</em> button, for example to match the application's language.
     *
     * @param text the label
     */
    public final void setResetZoomText(String text) {
        resetZoomText.set(text);
    }

    /**
     * The space left of the document's text, in pixels at 100 % zoom; not a number, the default, leaves the theme's
     * left padding of {@code markdown-document} (14 pixels). The zoom scales it with the document, and a narrow view
     * shrinks it, so the text keeps 300 pixels; a value below 0 counts as 0. A change keeps the line at the top of the
     * visible area at the top, as a new zoom does. See <em>Margins</em> in the class description.
     *
     * @return the left margin property
     * @since 1.1.0-RC2
     */
    public final DoubleProperty leftMarginProperty() {
        return leftMargin;
    }

    /**
     * Returns the space left of the document's text, see {@link #leftMarginProperty()}.
     *
     * @return the margin in pixels at 100 % zoom, as set; not a number while the theme's padding holds
     * @since 1.1.0-RC2
     */
    public final double getLeftMargin() {
        return leftMargin.get();
    }

    /**
     * Sets the space left of the document's text, see {@link #leftMarginProperty()}.
     *
     * @param margin the margin in pixels at 100 % zoom, below 0 as 0; not a number for the theme's padding
     * @since 1.1.0-RC2
     */
    public final void setLeftMargin(double margin) {
        leftMargin.set(margin);
    }

    /**
     * The space right of the document's text, in pixels at 100 % zoom; not a number, the default, leaves the theme's
     * right padding of {@code markdown-document} (14 pixels). The zoom scales it with the document, and a narrow view
     * shrinks it, so the text keeps 300 pixels; a value below 0 counts as 0. A change keeps the line at the top of the
     * visible area at the top, as a new zoom does. See <em>Margins</em> in the class description.
     *
     * @return the right margin property
     * @since 1.1.0-RC2
     */
    public final DoubleProperty rightMarginProperty() {
        return rightMargin;
    }

    /**
     * Returns the space right of the document's text, see {@link #rightMarginProperty()}.
     *
     * @return the margin in pixels at 100 % zoom, as set; not a number while the theme's padding holds
     * @since 1.1.0-RC2
     */
    public final double getRightMargin() {
        return rightMargin.get();
    }

    /**
     * Sets the space right of the document's text, see {@link #rightMarginProperty()}.
     *
     * @param margin the margin in pixels at 100 % zoom, below 0 as 0; not a number for the theme's padding
     * @since 1.1.0-RC2
     */
    public final void setRightMargin(double margin) {
        rightMargin.set(margin);
    }

    /**
     * The text the view searches for, {@code null} by default: no search. {@code null}, the empty text and whitespace
     * only are no search. Each change runs the search through the document anew, see <em>Search</em> in the class
     * description: the number of matches and the current match follow, the marks show the new matches, and the view
     * scrolls to the current match if it is not fully visible. The property may be bound, also both ways, for
     * example to an application's search field.
     *
     * @return the search text property
     * @since 1.1.0-RC2
     */
    public final StringProperty searchTextProperty() {
        return searchText;
    }

    /**
     * Returns the text the view searches for, see {@link #searchTextProperty()}.
     *
     * @return the search text, {@code null} without a search
     * @since 1.1.0-RC2
     */
    public final String getSearchText() {
        return searchText.get();
    }

    /**
     * Sets the text the view searches for, see {@link #searchTextProperty()}.
     *
     * @param text the search text; {@code null}, the empty text and whitespace only for no search
     * @since 1.1.0-RC2
     */
    public final void setSearchText(String text) {
        searchText.set(text);
    }

    /**
     * The number of matches of the search in the document, 0 without a search or without matches.
     *
     * @return the read-only property of the number of matches
     * @since 1.1.0-RC2
     */
    public final ReadOnlyIntegerProperty searchMatchCountProperty() {
        return searchMatchCount.getReadOnlyProperty();
    }

    /**
     * Returns the number of matches of the search in the document, see {@link #searchMatchCountProperty()}.
     *
     * @return the number of matches, 0 without a search
     * @since 1.1.0-RC2
     */
    public final int getSearchMatchCount() {
        return searchMatchCount.get();
    }

    /**
     * The number of the current match in document order, from 0 for the first match from the top of the document,
     * whatever the direction; -1 without a current match. With at least one match there is always a current one. An
     * application shows it from 1, as in <em>3 of 12</em>.
     *
     * @return the read-only property of the current match's index
     * @since 1.1.0-RC2
     */
    public final ReadOnlyIntegerProperty searchMatchIndexProperty() {
        return searchMatchIndex.getReadOnlyProperty();
    }

    /**
     * Returns the number of the current match in document order, see {@link #searchMatchIndexProperty()}.
     *
     * @return the index from 0, -1 without a current match
     * @since 1.1.0-RC2
     */
    public final int getSearchMatchIndex() {
        return searchMatchIndex.get();
    }

    /**
     * The direction the search goes in, {@link SearchDirection#FORWARD} by default; {@code null} counts as
     * {@code FORWARD}. Forward, a new search starts at the first match and {@link #nextSearchMatch()} goes to the
     * one below; backward, a new search starts at the last match and the next match is the one above. A change moves
     * no match.
     *
     * @return the direction property
     * @since 1.1.0-RC2
     */
    public final ObjectProperty<SearchDirection> searchDirectionProperty() {
        return searchDirection;
    }

    /**
     * Returns the direction the search goes in, see {@link #searchDirectionProperty()}.
     *
     * @return the direction, {@code FORWARD} by default; {@code null} if set so, which counts as {@code FORWARD}
     * @since 1.1.0-RC2
     */
    public final SearchDirection getSearchDirection() {
        return searchDirection.get();
    }

    /**
     * Sets the direction the search goes in, see {@link #searchDirectionProperty()}.
     *
     * @param direction the direction; {@code null} counts as {@link SearchDirection#FORWARD}
     * @since 1.1.0-RC2
     */
    public final void setSearchDirection(SearchDirection direction) {
        searchDirection.set(direction);
    }

    /**
     * Makes the next match in the search's direction the current one, after the last the first again (backward,
     * before the first the last), and scrolls to it if it is not fully visible. Without matches it does nothing.
     *
     * @since 1.1.0-RC2
     */
    public final void nextSearchMatch() {
        documentSearch.next();
    }

    /**
     * Makes the next match against the search's direction the current one, wrapping around as
     * {@link #nextSearchMatch()} does, and scrolls to it if it is not fully visible. Without matches it does nothing.
     *
     * @since 1.1.0-RC2
     */
    public final void previousSearchMatch() {
        documentSearch.previous();
    }

    /**
     * Ends the search: sets the search text to {@code null}, so the marks go and there are no matches.
     *
     * @throws RuntimeException while the search text is bound one way, as setting a bound property throws
     * @since 1.1.0-RC2
     */
    public final void clearSearch() {
        searchText.set(null);
    }

    /**
     * The source line the reader is looking at: that of the first block whose top lies in the visible area, a block
     * fully visible at the top or one taller than the area that starts in it, see <em>Source lines</em> in the class
     * description. A heading or a paragraph cut by the top edge does not count, the block below it does; in a code or
     * source block or a table cut by the edge, the first code line or row below it counts. Where no block starts in
     * the visible area, as in a paragraph taller than it, the block at the top edge counts. Before the view is laid
     * out, the first block's line.
     *
     * <p>The view applies its CSS and lays itself out first if a change waits for the next pulse, such as a new zoom
     * or a new version of the text, so the answer holds for what the reader will see.
     *
     * @return the 1-based line of the Markdown text; empty while the view shows no document or a document without
     *         blocks
     * @since 1.1.0-RC2
     */
    public final OptionalInt topSourceLine() {
        applyCss();
        layout();
        double viewportHeight = Math.max(0, scrollPane.getViewportBounds().getHeight());
        double offset = VerticalScroll.offset(scrollPane);
        Node content = scrollPane.getContent();
        double top = content.localToScene(0, offset).getY();
        double bottom = content.localToScene(0, offset + viewportHeight).getY();
        return SourceLines.fromTop(documentSelection.segments(), top, bottom);
    }

    /**
     * The source line of the current search match: that of the heading, paragraph, table row or code or source line
     * that holds it, see <em>Source lines</em> in the class description. In a sorted table, the line of the match's
     * row as the source has it.
     *
     * @return the 1-based line of the Markdown text; empty without a current match
     * @since 1.1.0-RC2
     */
    public final OptionalInt searchMatchSourceLine() {
        return documentSearch.currentSourceLine();
    }

    /**
     * The languages whose code blocks the view colours: a map from a language's name to its {@link CodeLexer lexer},
     * owned by the view. It starts with the languages built into the view, such as {@code java} for Java, see
     * <em>Code colours</em> in the class description. A block's language is the first word of its fence's info
     * string, which ends at the first space or <code>&#123;</code>, in lower case ({@link java.util.Locale#ROOT}):
     * {@code Java}, {@code java title} and <code>java&#123;1&#125;</code> all name {@code java}. So the names in the
     * map are lower case; a name with a capital letter is never found.
     *
     * <p>An application adds a language of its own or replaces a built-in one with {@code put}, and switches one off
     * with {@code remove}: its blocks then show in one colour. Every change rebuilds the shown document, as a new text
     * does. Each view has a map of its own, so a change reaches only the view it is made on. A lexer that fails leaves
     * its blocks in one colour and is reported as a warning, see {@link CodeLexer}.
     *
     * @return the view's map of languages, by their lower-case names
     */
    public final ObservableMap<String, CodeLexer> getCodeLanguages() {
        return codeLanguages;
    }

    /**
     * The limit of lines per code block above which a block stays in one colour, so that a very long block cannot
     * make the view slow; {@value #DEFAULT_CODE_HIGHLIGHT_LINE_LIMIT} by default. A block with more lines than the
     * limit shows in the code colour, and its lexer is not called; a limit of 0 or below colours no block. The lines
     * of a block are its line feeds plus one, counted on the code as the block shows it, without the line feed after
     * its last line. Every change rebuilds the shown document, as a new text does.
     *
     * @return the line limit property
     */
    public final IntegerProperty codeHighlightLineLimitProperty() {
        return codeHighlightLineLimit;
    }

    /**
     * Returns the limit of lines per code block, see {@link #codeHighlightLineLimitProperty()}.
     *
     * @return the limit
     */
    public final int getCodeHighlightLineLimit() {
        return codeHighlightLineLimit.get();
    }

    /**
     * Sets the limit of lines per code block, see {@link #codeHighlightLineLimitProperty()}.
     *
     * @param limit the limit; 0 or below colours no block
     */
    public final void setCodeHighlightLineLimit(int limit) {
        codeHighlightLineLimit.set(limit);
    }

    /**
     * Where the view's images come from: the loader that returns the bytes of an image file for an image's address
     * as written in the Markdown ({@code ![alt](address)}). The view calls it on a background thread, never on the
     * JavaFX Application Thread, possibly for several addresses at once, see {@link ImageLoader}.
     *
     * <p>Its initial value is the view's own loader. It reads the view's {@link #imageBaseProperty() base address}
     * and {@link #remoteImagesProperty() internet switch} at the time of each call, so it follows them however it is
     * kept:
     * <ul>
     *   <li>a relative address resolves against the base address, as a browser resolves it against a page, and
     *       without a base it is missing,</li>
     *   <li>absolute {@code file:} and {@code jar:} addresses load as they are, and so does an absolute file path of
     *       the platform, such as {@code C:\docs\a.png} or {@code /home/me/a.png},</li>
     *   <li>a {@code data:} address with base64 content of an {@code image/} type is decoded, any other is
     *       missing,</li>
     *   <li>{@code http:} and {@code https:} addresses load only while the internet switch is on, with time limits
     *       for connecting and reading,</li>
     *   <li>any other scheme is missing.</li>
     * </ul>
     * Characters a URI does not allow, such as spaces or letters beyond ASCII, are percent-encoded before an address
     * resolves, and escapes stay as written. A file or an archive's entry is read without the address's query and
     * fragment, as a browser reads it.
     *
     * <p>An application may keep the view's own loader, wrap it, handling a few addresses itself and leaving the rest
     * to it, or replace it:
     * <pre>{@code
     * ImageLoader own = view.getImageLoader();
     * view.setImageLoader(address -> address.startsWith("bundle:") ? bundleImage(address) : own.load(address));
     * }</pre>
     * {@code null} loads no image: every image shows as missing. A new loader loads the shown images anew, in place:
     * the document is neither rebuilt nor scrolled, and each image shows what it showed until its new file arrives.
     *
     * @return the image loader property
     */
    public final ObjectProperty<ImageLoader> imageLoaderProperty() {
        return imageLoader;
    }

    /**
     * Returns where the view's images come from, see {@link #imageLoaderProperty()}.
     *
     * @return the loader, {@code null} if the view loads no image
     */
    public final ImageLoader getImageLoader() {
        return imageLoader.get();
    }

    /**
     * Sets where the view's images come from, see {@link #imageLoaderProperty()}.
     *
     * @param loader the loader, called on a background thread, {@code null} to load no image
     */
    public final void setImageLoader(ImageLoader loader) {
        imageLoader.set(loader);
    }

    /**
     * The base address the view's own image loader resolves relative image addresses against, as a browser resolves
     * them against a page, {@code null} by default: a URI with the scheme {@code file:}, {@code jar:}, {@code http:}
     * or {@code https:}. A document that sits next to its images shows them with one line:
     * <pre>{@code
     * view.setImageBase(documentFile.toUri().toString());                 // file:///C:/docs/readme.md
     * view.setImageBase(App.class.getResource("/docs/").toExternalForm()); // jar:file:/C:/app/app.jar!/docs/
     * }</pre>
     * The address of a folder ends with a slash. Without one its last segment is a file's name, which a relative
     * address replaces. Without a base a relative address is missing. A change loads the shown images anew, in place,
     * see {@link #imageLoaderProperty()}.
     *
     * @return the base address property
     */
    public final StringProperty imageBaseProperty() {
        return imageBase;
    }

    /**
     * Returns the base address relative image addresses resolve against, see {@link #imageBaseProperty()}.
     *
     * @return the base address, {@code null} if none
     */
    public final String getImageBase() {
        return imageBase.get();
    }

    /**
     * Sets the base address relative image addresses resolve against, see {@link #imageBaseProperty()}.
     *
     * @param base the base address, a URI, {@code null} for none
     */
    public final void setImageBase(String base) {
        imageBase.set(base);
    }

    /**
     * Whether the view's own image loader loads images from {@code http:} and {@code https:} addresses, absolute or
     * relative to an internet base address. It is {@code false} by default, so showing a document never sends a request
     * to a foreign server by itself. Switched on, a download waits at most 10 seconds for the connection and 10 seconds
     * for each read. A change loads the shown images anew, in place, see {@link #imageLoaderProperty()}: switched off,
     * the images from the internet show as missing.
     *
     * @return the property of images from the internet
     */
    public final BooleanProperty remoteImagesProperty() {
        return remoteImages;
    }

    /**
     * Returns whether the view's own loader loads images from the internet, see {@link #remoteImagesProperty()}.
     *
     * @return {@code true} if it loads them, {@code false} by default
     */
    public final boolean isRemoteImages() {
        return remoteImages.get();
    }

    /**
     * Sets whether the view's own loader loads images from the internet, see {@link #remoteImagesProperty()}.
     *
     * @param remote {@code true} to load them
     */
    public final void setRemoteImages(boolean remote) {
        remoteImages.set(remote);
    }

    /**
     * The note of the box an image shows when the loader has no file for its address, {@code Image not found} by
     * default: the loader threw an exception or returned nothing. A change applies to the boxes shown at once.
     *
     * @return the note property of a missing image
     */
    public final StringProperty imageMissingTextProperty() {
        return imageMissingText;
    }

    /**
     * Returns the note of the box of a missing image.
     *
     * @return the note, {@code Image not found} by default
     */
    public final String getImageMissingText() {
        return imageMissingText.get();
    }

    /**
     * Sets the note of the box of a missing image, for example to match the application's language.
     *
     * @param text the note
     */
    public final void setImageMissingText(String text) {
        imageMissingText.set(text);
    }

    /**
     * The note of the box an image shows when its file is in a format the view cannot show, {@code Image format not
     * supported} by default: an SVG or WebP file, a damaged file. A change applies to the boxes shown at once.
     *
     * @return the note property of an image in a format the view cannot show
     */
    public final StringProperty imageUnsupportedTextProperty() {
        return imageUnsupportedText;
    }

    /**
     * Returns the note of the box of an image in a format the view cannot show.
     *
     * @return the note, {@code Image format not supported} by default
     */
    public final String getImageUnsupportedText() {
        return imageUnsupportedText.get();
    }

    /**
     * Sets the note of the box of an image in a format the view cannot show, for example to match the application's
     * language.
     *
     * @param text the note
     */
    public final void setImageUnsupportedText(String text) {
        imageUnsupportedText.set(text);
    }

    /**
     * The note of the box an image shows when it has more pixels than {@link #imagePixelLimitProperty() the limit},
     * {@code Image too large} by default. A change applies to the boxes shown at once.
     *
     * @return the note property of an image above the limit
     */
    public final StringProperty imageTooLargeTextProperty() {
        return imageTooLargeText;
    }

    /**
     * Returns the note of the box of an image above the limit.
     *
     * @return the note, {@code Image too large} by default
     */
    public final String getImageTooLargeText() {
        return imageTooLargeText.get();
    }

    /**
     * Sets the note of the box of an image above the limit, for example to match the application's language.
     *
     * @param text the note
     */
    public final void setImageTooLargeText(String text) {
        imageTooLargeText.set(text);
    }

    /**
     * The largest image the view shows, in pixels (width times height), or 0, the default, or less for no limit. An
     * image with more pixels shows a box with {@link #imageTooLargeTextProperty() its note} instead, so an application
     * with little memory keeps no bitmap of it and computes none. The view reads an image's size from the decoded
     * image, so the limit does not spare the memory of that one decode. A change loads the shown images anew, in
     * place, see {@link #imageLoaderProperty()}.
     *
     * @return the limit property
     */
    public final IntegerProperty imagePixelLimitProperty() {
        return imagePixelLimit;
    }

    /**
     * Returns the largest image the view shows, see {@link #imagePixelLimitProperty()}.
     *
     * @return the limit in pixels, 0 or less for none
     */
    public final int getImagePixelLimit() {
        return imagePixelLimit.get();
    }

    /**
     * Sets the largest image the view shows, see {@link #imagePixelLimitProperty()}.
     *
     * @param pixels the limit in pixels, width times height, 0 or less for none
     */
    public final void setImagePixelLimit(int pixels) {
        imagePixelLimit.set(pixels);
    }

    /**
     * Whether animated GIFs play in the document, {@code true} by default. An animated GIF is a GIF file of more than
     * one frame, which the view tells from the file's bytes. A GIF of one frame is an ordinary image. On, an animated
     * GIF plays, scaled by JavaFX alone, not by the view's area average, and kept decoded while it shows: an exception
     * to the scaling and to the memory rule of the other images, as JavaFX cannot read the pixels of an animated
     * image. Off, the document shows its first frame, which the view decodes, scales and keeps like any image, for a
     * calm document. A change shows the animated GIFs anew, in place, from the bytes the view keeps, without loading
     * them again. The full size window always plays an animated GIF.
     *
     * @return the property of playing animations
     */
    public final BooleanProperty imageAnimationProperty() {
        return imageAnimation;
    }

    /**
     * Returns whether animated GIFs play in the document, see {@link #imageAnimationProperty()}.
     *
     * @return {@code true} if they play, the default
     */
    public final boolean isImageAnimation() {
        return imageAnimation.get();
    }

    /**
     * Sets whether animated GIFs play in the document, see {@link #imageAnimationProperty()}.
     *
     * @param animation {@code true} to play them, {@code false} to show their first frame
     */
    public final void setImageAnimation(boolean animation) {
        imageAnimation.set(animation);
    }

    /**
     * The tooltip of the button over a block image that opens it in the full size window, {@code Open image} by
     * default. It is the button's accessible text too, and the label of the context menu entry that opens an image. A
     * change applies at once.
     *
     * @return the property of the open button's tooltip and the context menu entry's label
     */
    public final StringProperty openImageTextProperty() {
        return openImageText;
    }

    /**
     * Returns the tooltip of the button that opens an image, also the label of the context menu entry.
     *
     * @return the text, {@code Open image} by default
     */
    public final String getOpenImageText() {
        return openImageText.get();
    }

    /**
     * Sets the tooltip of the button that opens an image, also the label of the context menu entry, for example to
     * match the application's language.
     *
     * @param text the text
     */
    public final void setOpenImageText(String text) {
        openImageText.set(text);
    }

    /**
     * The label of the full size window's button that fits the image into the window, {@code Fit} by default. A
     * change applies at once.
     *
     * @return the label property of the <em>Fit</em> button
     */
    public final StringProperty fitImageTextProperty() {
        return fitImageText;
    }

    /**
     * Returns the label of the full size window's <em>Fit</em> button.
     *
     * @return the label, {@code Fit} by default
     */
    public final String getFitImageText() {
        return fitImageText.get();
    }

    /**
     * Sets the label of the full size window's <em>Fit</em> button, for example to match the application's language.
     *
     * @param text the label
     */
    public final void setFitImageText(String text) {
        fitImageText.set(text);
    }

    /**
     * The label of the full size window's button that shows the image at 100 %, one image pixel per screen pixel,
     * {@code 100%} by default. A change applies at once.
     *
     * @return the label property of the <em>100 %</em> button
     */
    public final StringProperty actualImageSizeTextProperty() {
        return actualImageSizeText;
    }

    /**
     * Returns the label of the full size window's <em>100 %</em> button.
     *
     * @return the label, {@code 100%} by default
     */
    public final String getActualImageSizeText() {
        return actualImageSizeText.get();
    }

    /**
     * Sets the label of the full size window's <em>100 %</em> button, for example to match the application's
     * language.
     *
     * @param text the label
     */
    public final void setActualImageSizeText(String text) {
        actualImageSizeText.set(text);
    }

    /**
     * The label of the full size window's button that copies the image to the clipboard, {@code Copy image} by
     * default. A change applies at once.
     *
     * @return the label property of the <em>Copy image</em> button
     */
    public final StringProperty copyImageTextProperty() {
        return copyImageText;
    }

    /**
     * Returns the label of the full size window's <em>Copy image</em> button.
     *
     * @return the label, {@code Copy image} by default
     */
    public final String getCopyImageText() {
        return copyImageText.get();
    }

    /**
     * Sets the label of the full size window's <em>Copy image</em> button, for example to match the application's
     * language.
     *
     * @param text the label
     */
    public final void setCopyImageText(String text) {
        copyImageText.set(text);
    }

    /**
     * The label of the full size window's button that saves the image's file, {@code Save as...} by default. It is
     * the title of the file chooser that asks where to save it, and of the error dialog when saving fails. A change
     * applies at once.
     *
     * @return the label property of the <em>Save as</em> button
     */
    public final StringProperty saveImageTextProperty() {
        return saveImageText;
    }

    /**
     * Returns the label of the full size window's <em>Save as</em> button, also the file chooser's title.
     *
     * @return the label, {@code Save as...} by default
     */
    public final String getSaveImageText() {
        return saveImageText.get();
    }

    /**
     * Sets the label of the full size window's <em>Save as</em> button, also the file chooser's title, for example to
     * match the application's language.
     *
     * @param text the label
     */
    public final void setSaveImageText(String text) {
        saveImageText.set(text);
    }

    /**
     * The label of the context menu entry that copies the whole Markdown source, {@code Copy Markdown} by default. A
     * change applies to the menu at once.
     *
     * @return the label property of the context menu entry
     */
    public final StringProperty copyMarkdownTextProperty() {
        return copyMarkdownText;
    }

    /**
     * Returns the label of the context menu entry that copies the whole Markdown source.
     *
     * @return the label, {@code Copy Markdown} by default
     */
    public final String getCopyMarkdownText() {
        return copyMarkdownText.get();
    }

    /**
     * Sets the label of the context menu entry that copies the whole Markdown source, for example to match the
     * application's language.
     *
     * @param text the label
     */
    public final void setCopyMarkdownText(String text) {
        copyMarkdownText.set(text);
    }

    /**
     * The tooltip of a code block's copy button, {@code Copy code} by default; it is the button's accessible text
     * too. A change applies to the buttons already shown at once.
     *
     * @return the tooltip property of the copy buttons
     */
    public final StringProperty copyCodeTextProperty() {
        return copyCodeText;
    }

    /**
     * Returns the tooltip of a code block's copy button.
     *
     * @return the tooltip, {@code Copy code} by default
     */
    public final String getCopyCodeText() {
        return copyCodeText.get();
    }

    /**
     * Sets the tooltip of a code block's copy button, for example to match the application's language.
     *
     * @param text the tooltip
     */
    public final void setCopyCodeText(String text) {
        copyCodeText.set(text);
    }

    /**
     * The label of the context menu entry that copies a selection, {@code Copy} by default: in a table the selected
     * cells, as the shortcut key plus C copies them, in a code or source block the selected characters, elsewhere the
     * text selected across the document. A change applies to the menu at once.
     *
     * @return the label property of the <em>Copy</em> entry
     */
    public final StringProperty copyTextProperty() {
        return copyText;
    }

    /**
     * Returns the label of the context menu entry that copies a selection.
     *
     * @return the label, {@code Copy} by default
     */
    public final String getCopyText() {
        return copyText.get();
    }

    /**
     * Sets the label of the context menu entry that copies a selection, for example to match the application's
     * language.
     *
     * @param text the label
     */
    public final void setCopyText(String text) {
        copyText.set(text);
    }

    /**
     * The label of a table's context menu entry that copies the selected cells with a first line of their columns'
     * titles, {@code Copy with header} by default. A change applies to the menu at once.
     *
     * @return the label property of the <em>Copy with header</em> entry
     */
    public final StringProperty copyWithHeaderTextProperty() {
        return copyWithHeaderText;
    }

    /**
     * Returns the label of a table's context menu entry that copies the selected cells with their columns' titles.
     *
     * @return the label, {@code Copy with header} by default
     */
    public final String getCopyWithHeaderText() {
        return copyWithHeaderText.get();
    }

    /**
     * Sets the label of a table's context menu entry that copies the selected cells with their columns' titles, for
     * example to match the application's language.
     *
     * @param text the label
     */
    public final void setCopyWithHeaderText(String text) {
        copyWithHeaderText.set(text);
    }

    /**
     * The label of a table's context menu entry that selects every cell of the column the menu was opened on,
     * {@code Select column} by default. A change applies to the menu at once.
     *
     * @return the label property of the <em>Select column</em> entry
     */
    public final StringProperty selectColumnTextProperty() {
        return selectColumnText;
    }

    /**
     * Returns the label of a table's context menu entry that selects a whole column.
     *
     * @return the label, {@code Select column} by default
     */
    public final String getSelectColumnText() {
        return selectColumnText.get();
    }

    /**
     * Sets the label of a table's context menu entry that selects a whole column, for example to match the
     * application's language.
     *
     * @param text the label
     */
    public final void setSelectColumnText(String text) {
        selectColumnText.set(text);
    }

    /**
     * The text that names an open task's check box ({@code - [ ]}), {@code Open} by default: the box's tooltip and its
     * accessible text, which a screen reader announces with the check box. A change applies to the boxes already
     * shown at once. See <em>Task lists</em> in the class description.
     *
     * @return the text property of an open task's check box
     * @since 1.1.0-RC2
     */
    public final StringProperty taskOpenTextProperty() {
        return taskOpenText;
    }

    /**
     * Returns the text that names an open task's check box.
     *
     * @return the text, {@code Open} by default
     * @since 1.1.0-RC2
     */
    public final String getTaskOpenText() {
        return taskOpenText.get();
    }

    /**
     * Sets the text that names an open task's check box, its tooltip and accessible text, for example to match the
     * application's language.
     *
     * @param text the text
     * @since 1.1.0-RC2
     */
    public final void setTaskOpenText(String text) {
        taskOpenText.set(text);
    }

    /**
     * The text that names a done task's check box ({@code - [x]}), {@code Done} by default: the box's tooltip and its
     * accessible text, which a screen reader announces with the check box. A change applies to the boxes already
     * shown at once. See <em>Task lists</em> in the class description.
     *
     * @return the text property of a done task's check box
     * @since 1.1.0-RC2
     */
    public final StringProperty taskDoneTextProperty() {
        return taskDoneText;
    }

    /**
     * Returns the text that names a done task's check box.
     *
     * @return the text, {@code Done} by default
     * @since 1.1.0-RC2
     */
    public final String getTaskDoneText() {
        return taskDoneText.get();
    }

    /**
     * Sets the text that names a done task's check box, its tooltip and accessible text, for example to match the
     * application's language.
     *
     * @param text the text
     * @since 1.1.0-RC2
     */
    public final void setTaskDoneText(String text) {
        taskDoneText.set(text);
    }

    /**
     * Replaces the rendered content with the current text's and scrolls back to the top left. Nothing is selected in
     * the new content, and the context menu forgets the block it was built for last.
     *
     * <p>For {@link #updateMarkdown(String)} it keeps the reader's place instead of scrolling: what the new document
     * takes over from the old one is noted first, in the old document's layout, before anything of it is dropped, and
     * handed to the new one once it is rendered, its segments found again by {@link NotedSegment}: the reader's place
     * and the search's current match. What the images showed is noted by their address as the old images are
     * forgotten and handed to the new ones as the renderer creates them ({@link DocumentImages#forget(boolean)}).
     */
    private void rebuild() {
        // An update notes the reader's place in the shown document's layout, before anything of it goes.
        ReadingAnchor place = updating ? documentZoom.noteReadingPosition() : null;
        NotedSegment placeSegment = place == null ? null
                : NotedSegment.of(documentSelection.segments(), place.segment());
        DocumentSearch.Place searchPlace = place == null ? null : documentSearch.currentPlace();
        selectionHolder = null;
        documentZoom.forgetReadingPosition();
        documentSelection.reset();
        // An update's images start with what the old document's images of their address showed, then load again.
        images.forget(updating);
        document.getChildren().setAll(renderer.render(MarkdownParser.parse(markdown.get())));
        images.rendered();
        documentSearch.documentChanged(searchPlace);
        Segment found = placeSegment == null ? null : placeSegment.findIn(documentSelection.segments());
        if (found != null) {
            // The next layout scrolls there; the sideways scroll position stays.
            documentZoom.keepReadingPosition(place.movedTo(found));
        } else {
            scrollPane.setVvalue(scrollPane.getVmin());
            scrollPane.setHvalue(scrollPane.getHmin());
        }
        prepareContextMenu(null);
    }

    /**
     * Replaces where the view copies text to. The view's tests put a recorder here, so they never write to the
     * system clipboard; the seam is no part of the API.
     *
     * @param clipboard the clipboard every copy from now on goes to
     */
    void setClipboard(TextClipboard clipboard) {
        this.clipboard = clipboard;
    }

    /**
     * Replaces where the view reports a failing lexer, a missing or undecodable image and a failing image loader, by
     * default the SLF4J logger named after the view's class. The view's tests put a recorder here, so they never
     * write to the application's log; the seam is no part of the API.
     *
     * @param logger the logger every record from now on goes to
     */
    void setLogger(Logger logger) {
        codeColoring.setLogger(logger);
        images.setLogger(logger);
    }

    /**
     * Replaces where the work on the images runs: loading, decoding and showing them. The view's tests run it at once
     * on their own thread, so they see the finished image right after the view renders its text; the seam is no part of
     * the API.
     *
     * @param work the work for the images of every text from now on
     */
    void setImageWork(ImageWork work) {
        images.setWork(work);
    }

    /**
     * Sets the output scale the view assumes instead of its window's, screen pixels per document pixel: a scene that
     * is never shown has no screen. The view's tests set 1.0, 1.5 or 2.0; the seam is no part of the API.
     *
     * @param scale the scale; 0 for the window's
     */
    void setOutputScale(double scale) {
        images.setAssumedOutputScale(scale);
    }

    /**
     * Replaces what opens an image at full size, by default the view's full size window. The view's tests put a
     * recorder here, so no window ever opens; the seam is no part of the API.
     *
     * @param window what gets each request to open an image from now on
     */
    void setImageWindow(Consumer<ImageWindowRequest> window) {
        images.setWindow(window);
    }

    /**
     * Replaces the animation frames by which a drag beyond the visible area scrolls the document. The view's tests
     * advance frames by hand, so no timer touches the view from the JavaFX thread; the seam is no part of the API.
     *
     * @param frames the frames from now on
     */
    void setAnimationFrames(AnimationFrames frames) {
        documentSelection.setAnimationFrames(frames);
    }

    /**
     * The view's context menu. It belongs to the view's scroll pane, which fills the view: a request anywhere in the
     * view bubbles up to it, since no node inside handles one (the code blocks' scroll panes and buttons, the tables,
     * their cells and their column headers have no menu of their own). Its entries' labels follow the label
     * properties; the entry that copies the whole Markdown source is disabled while there is none.
     */
    private ContextMenu contextMenu() {
        copyEntry.textProperty().bind(copyText);
        copyWithHeaderEntry.textProperty().bind(copyWithHeaderText);
        selectColumnEntry.textProperty().bind(selectColumnText);
        copyMarkdownEntry.textProperty().bind(copyMarkdownText);
        copyMarkdownEntry.disableProperty().bind(markdown.isEmpty());
        copyMarkdownEntry.setOnAction(event -> copyMarkdown());
        openImageEntry.textProperty().bind(openImageText);
        return contextMenu;
    }

    /**
     * Builds the context menu's entries for a request at a node, before the scroll pane shows the menu. The node's
     * nearest block decides:
     * <ul>
     *   <li>a table, from a cell or a column header: <em>Copy</em> (see {@code copiedSelection}), <em>Copy with
     *       header</em>, disabled without selected cells, <em>Select column</em> for the cell's or header's column
     *       (requested at the table itself, from the keyboard at the focused table, the focused cell's column;
     *       disabled without a column or rows), and <em>Copy Markdown</em>;</li>
     *   <li>anywhere else, in a code or source block, in text or between blocks: <em>Copy</em> and <em>Copy
     *       Markdown</em>.</li>
     * </ul>
     * A separator sets <em>Copy Markdown</em>, which copies the whole document, apart from the entries above it.
     * Requested on an image, <em>Open image</em> comes first, set apart by a separator too (see
     * {@link #addOpenImageEntry}). The view's tests build the menu here, since a test without a window cannot show
     * it.
     *
     * @param target where the request comes from, the node under the pointer or the focus owner; {@code null}
     *               resets the menu to the entries for text
     */
    void prepareContextMenu(EventTarget target) {
        MarkdownTable table = Ancestors.nearest(target, MarkdownTable.class, this);
        SelectionHolder copied = copiedSelection(target, table);
        List<MenuItem> entries = new ArrayList<>();
        addOpenImageEntry(target, entries);
        // The actions refer to the block of this request only.
        copyWithHeaderEntry.setOnAction(null);
        selectColumnEntry.setOnAction(null);
        copyEntry.setDisable(copied == null);
        copyEntry.setOnAction(copied == null ? null : event -> copied.copySelection());
        entries.add(copyEntry);
        if (table != null) {
            copyWithHeaderEntry.setDisable(!table.hasSelection());
            copyWithHeaderEntry.setOnAction(event -> table.copySelectionWithHeader());
            TableColumn<MarkdownTable.Row, ?> selectable = table.selectableColumn(target);
            selectColumnEntry.setDisable(selectable == null);
            selectColumnEntry.setOnAction(event -> table.selectColumn(selectable));
            entries.add(copyWithHeaderEntry);
            entries.add(selectColumnEntry);
        }
        entries.add(separator);
        entries.add(copyMarkdownEntry);
        contextMenu.getItems().setAll(entries);
    }

    /**
     * Prepares the context menu for a request: the view's own entries for the node it comes from, then the
     * application's callback, if one is set, with the source line at the request's height in that node.
     *
     * @param event the request, from a right click or the keyboard
     */
    private void contextMenuRequested(ContextMenuEvent event) {
        EventTarget target = event.getTarget();
        prepareContextMenu(target);
        ContextMenuCallback callback = contextMenuCallback.get();
        if (callback == null) {
            return;
        }
        OptionalInt line = target instanceof Node node ? SourceLines.at(node, event.getSceneY()) : OptionalInt.empty();
        callback.contextMenuRequested(contextMenu.getItems(), new ContextMenuPlace(line));
    }

    /**
     * Adds <em>Open image</em> and a separator to the context menu's entries for a request on an image, a block or an
     * inline image, or the icon or alt text of an image in a table cell: the entry opens it in the full size window. It
     * is disabled while a block or inline image shows its placeholder or its error box. A linked image opens too.
     *
     * @param target  where the request comes from
     * @param entries the menu's entries, empty so far
     */
    private void addOpenImageEntry(EventTarget target, List<MenuItem> entries) {
        ImageNode image = Ancestors.nearest(target, ImageNode.class, this);
        CellImage cellImage = CellImage.at(target, this);
        openImageEntry.setOnAction(null);
        if (image != null) {
            openImageEntry.setDisable(!image.showsPicture());
            openImageEntry.setOnAction(event -> images.open(image));
        } else if (cellImage != null) {
            openImageEntry.setDisable(false);
            openImageEntry.setOnAction(event -> images.open(cellImage));
        } else {
            return;
        }
        entries.add(openImageEntry);
        entries.add(openImageSeparator);
    }

    /**
     * What the context menu's <em>Copy</em> copies for a request at a node: the view's one selection where the
     * request lies in it. In a table with selected cells, those; in a code or source block or a table the text
     * selected across the document reaches, that selection; in text, on a block image and between blocks, the text
     * selection wherever it lies.
     *
     * @return the selection, {@code null} if there is none to copy there
     */
    private SelectionHolder copiedSelection(EventTarget target, MarkdownTable table) {
        if (table != null && table.hasSelection()) {
            return table;
        }
        Segment block = Ancestors.nearest(target, Segment.class, this);
        boolean inSelection = block == null || block instanceof TextSegment || block instanceof ImageBlock
                || documentSelection.reaches(block);
        return inSelection && documentSelection.hasSelection() ? documentSelection : null;
    }

    /**
     * Keeps one selection in the view: a selection that starts in the document's text or in a table clears the one of
     * whatever held it before.
     */
    private void selectionStarted(SelectionHolder holder) {
        if (selectionHolder != null && selectionHolder != holder) {
            selectionHolder.clearSelection();
        }
        selectionHolder = holder;
    }

    /**
     * The shortcut key plus C copies the view's selection, whatever holds it; plus A selects the whole document. A key
     * event reaches this handler from wherever the focus is inside the view, unless a code or source block with the
     * focus took it, or a table with the focus and selected cells (plus A, any table with the focus). Both are
     * consumed, as a text field consumes them.
     *
     * <p>Every other key press goes to the zoom ({@link DocumentZoom#keyPressed}), which zooms by the zoom shortcuts
     * while the zoom bar shows.
     */
    private void keyPressed(KeyEvent event) {
        if (COPY.match(event)) {
            event.consume();
            if (selectionHolder != null) {
                selectionHolder.copySelection();
            }
        } else if (SELECT_ALL.match(event)) {
            event.consume();
            documentSelection.selectAll();
        } else {
            documentZoom.keyPressed(event);
        }
    }

    /**
     * Copies the Markdown source exactly as it was set, if there is one.
     */
    private void copyMarkdown() {
        String text = markdown.get();
        if (text != null && !text.isEmpty()) {
            copy(text);
        }
    }

    /**
     * Puts a text on the clipboard the view copies to at the time of the copy.
     */
    private void copy(String text) {
        clipboard.put(text);
    }

    /**
     * Keeps the line at the top of the visible area in place through the next layout: an image arrives above it.
     */
    private void keepReadingPosition() {
        documentZoom.keepReadingPosition();
    }

    /**
     * Shows an image in the view's full size window, which is made at the first request.
     */
    private void openImageWindow(ImageWindowRequest request) {
        if (imageWindow == null) {
            imageWindow = new ImageWindow(this, imageNotes, images.work());
        }
        imageWindow.open(request);
    }

    /**
     * Hands a clicked link's address to the handler, if there is one.
     */
    private void followLink(String address) {
        Consumer<String> handler = onLinkAction.get();
        if (handler != null) {
            handler.accept(address);
        }
    }

    /**
     * The zoom bar on top, as high as it asks to be, while it is visible; the scroll pane below it fills the rest.
     */
    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double width = getWidth() - left - snappedRightInset();
        double height = getHeight() - top - snappedBottomInset();
        if (zoomBarVisible.get()) {
            double barHeight = Math.min(zoomBarHeight(), height);
            zoomBar.resizeRelocate(left, top, width, barHeight);
            top += barHeight;
            height -= barHeight;
        }
        // Known here, before the scroll pane lays the document out: the visible height limits the images' size.
        images.setVisibleHeight(height - scrollPane.snappedTopInset() - scrollPane.snappedBottomInset());
        scrollPane.resizeRelocate(left, top, width, height);
        documentZoom.restoreReadingPosition();
        // After the reading position: the search marks what lies near the visible area where the document now is.
        documentSearch.layoutDone();
    }

    // The view scrolls, so its size does not follow its content: a long paragraph would otherwise ask its parent
    // for the width of the whole paragraph on one line. Fixed preferred sizes, like a TextArea's, and small minimum
    // ones let the layout decide.

    @Override
    protected double computePrefWidth(double height) {
        return snappedLeftInset() + PREF_WIDTH + snappedRightInset();
    }

    /**
     * The fixed height of the document plus the zoom bar's while it is visible.
     */
    @Override
    protected double computePrefHeight(double width) {
        return snappedTopInset() + (zoomBarVisible.get() ? zoomBarHeight() : 0) + PREF_HEIGHT + snappedBottomInset();
    }

    /**
     * The height the zoom bar asks for: one line of controls.
     */
    private double zoomBarHeight() {
        return snapSizeY(zoomBar.prefHeight(-1));
    }

    @Override
    protected double computeMinWidth(double height) {
        return snappedLeftInset() + MIN_SIZE + snappedRightInset();
    }

    @Override
    protected double computeMinHeight(double width) {
        return snappedTopInset() + MIN_SIZE + snappedBottomInset();
    }
}
