# Markdown view

The **Markdown view** shows a Markdown text with *standard JavaFX nodes*, in the
theme's look. Lines of a paragraph that are broken in the source flow together,
and the text wraps to the width of the view.

Select text with the mouse as in a web browser: drag from any word down across
headings, lists, quotes, code blocks and tables, and press Ctrl+C to copy what you
see as plain text, with list markers, code as written and tables as tab-separated
rows. A table the selection passes over is taken whole. Ctrl+A selects the whole
page, and a right click offers *Copy* as well.

## Text styles

Plain text, **bold**, *italic*, ***bold italic*** and `inline code` such as
`setting.name = "value"`. Emphasis nests: **bold with *italic* inside** and
*italic with **bold** inside*. Underscores work too: _italic_ and __bold__.

A **long bold phrase that spans a line break
in the source** stays bold, and so does *an italic one
across two lines*.

Code spans keep their content literal: `**not bold**`, ``a `backtick` inside``.

Literal characters stay text: 2 * 3 * 4, snake_case_names, \*escaped asterisks\*
and a backslash before punctuation: \# \_ \` \\.

## Links

A [link with **bold** and `code`](https://example.com/docs "Example docs") is shown
in the accent colour, with its address as a tooltip. A click hands the address to
the application, which this page shows below the editor; the view itself opens
nothing. A relative address reaches the handler as written: [Wait node](docs/WaitNode.md).

Bare addresses are links too, without a trailing period:
https://archive.ics.uci.edu/dataset/2/adult. So are autolinks such as
<https://freemarker.apache.org/>, and an address in parentheses
(https://en.wikipedia.org/wiki/Markdown) keeps its parenthesis out.

> A [link in a quote](https://example.com/quote) keeps the accent colour.

Reference-style links and images stay text: [docs][ref], ![logo][ref].

## Images

An image alone in its paragraph is a block of its own. The view never shows an image
larger than it was on the screen it was taken on: one image pixel is one screen pixel.
This dialog was taken on a 4K screen at 150 %, so on such a screen it shows exactly as
large as the dialog was, once the view is wide enough:

![A dialog on a 4K screen at 150 %](dialog.png "690 by 273 pixels, the dialog at 150 %")

A larger image shrinks to the width of the text, keeping its aspect ratio. The view
shrinks it itself, averaging the pixels each screen pixel covers, so the letters of a
screenshot stay whole and its thin lines free of moire. This one is a whole 4K screen,
3840 by 2160 pixels. Drag the divider to the left and it grows, up to its natural size.

![The showcase on a 4K screen at 150 %](screenshot-4k.png "A synthetic screenshot of the showcase, 3840 by 2160 pixels")

Point at an image and the button in its top right corner opens it at full size, and
so do a click on it and *Open image* in its context menu. The window fits a large
image first. Double-click for 100 %, pixel sharp, zoom with Ctrl and the wheel around
the pointer, drag to move it, press Escape to close it. Its bar copies the image and
saves its file.

A tall image shrinks to the height of the visible area, so it is seen whole:

![The showcase's side bar](sidebar.png "A tall image, 330 by 2841 pixels")

An animated GIF plays in the document while *Play animations* above the editor is on,
and in the full size window always. Switched off, the document shows its first frame:

![An export in progress](progress.gif "An animated GIF of twelve frames")

### An icon ![info](icon-info.png) in a heading

A small icon ![info](icon-info.png) sits in its line, on the baseline of the text. A
linked image follows its link on a click, while its button opens it:

[![karthu-fx for JavaFX 25](badge.png)](https://example.com/karthu-fx)

In a table an image is an icon and its alt text, so the columns keep their widths. A
click on it opens the image:

| Image | Kind |
|---|---|
| ![The badge](badge.png "A badge, 186 by 30 pixels") | a small PNG |
| ![The side bar](sidebar.png) | a tall PNG |

An image without a file, and one in a format the view cannot show, such as SVG, show
a box that says why, with the address as its tooltip:

![A missing image](missing.png)

![A diagram in SVG](diagram.svg)

An image from the internet loads only while *Images from the internet* above the
editor is on. Until then it shows as missing:

![The W3C logo](https://www.w3.org/Icons/w3c_home.png "From www.w3.org")

Select across the images and copy: each image gives its alt text, as a link gives its
text.

## Lists

Bullet lists use `-`, `*` or `+`, and nest when indented to the item's text:

- **in** — the table to read. A long item wraps to the width of the view, and its
  continuation line, indented to the item's text, stays part of the item.
- **Malformed rows** — what a row that does not fit does:
  - `fail` (the default): the node fails.
  - `skip and count`: the node leaves the row out
    and counts it.
    - A third level, indented further.
- **out** — the result, with a numbered list inside:
  1. read the file,
  2. split the lines,
  3. hand on the rows.

Numbered lists keep the number they start with, and their text stays aligned when the
numbers grow wider:

8. eight
9. nine
10. ten
11) a new list, as the delimiter changed

A list with blank lines between its items is *loose* and spaced like paragraphs:

- The first item.

- The second item, with a second paragraph.

  The second paragraph of the second item.

## Quotes and breaks

Block quotes start with `>` and set notes apart:

> **Note:** a quote holds any block, *emphasis* and `code` included. A line
without the marker right after a quoted paragraph continues it.
>
> - a list inside the quote,
> - with a **second** item.
>
> > And a quote inside the quote.

A line of three or more `-`, `*` or `_` is a thematic break:

---

The text after the break.

## Code blocks

A fenced code block keeps its lines and their indentation. A line wider than the
view stays whole: scroll the block sideways with shift and the mouse wheel, the
touchpad or its scroll bar. The plain wheel over the block keeps scrolling the page.
The button in a block's top right corner copies its code and stays put while the
code scrolls; a right click anywhere in the view offers *Copy Markdown*, the whole
source. To take out only a part, select it with the mouse and press Ctrl+C: drag
over the code, double-click a name such as `getFlowVariable`, or triple-click a
line; Ctrl+A selects the whole block. The same works in the source blocks below.

```java
// The three highest amounts, a missing amount last.
List<InRow> sorted = new java.util.ArrayList<>(allRows());
sorted.sort(java.util.Comparator.comparing((InRow r) -> r.get("amount", tLong), java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
for (InRow r : sorted.subList(0, Math.min(3, sorted.size()))) {
    out_customer = r.get("customer", tString);
    addRow();
}
```

Code blocks sit in list items too, and tildes fence a block as well as backticks:

1. Read the limit:

   ~~~java
   long limit = getFlowVariable("limit", tLong);
   ~~~

2. Stop the loop there:

   ```java
   for (InRow r : rows()) {
       if (r.index() >= limit) break;
   }
   ```

## Coloured code

A code block whose fence names a language the view knows shows its code in colours:
keywords, strings, numbers, comments, types and more, each in the colour of its
category. Seven languages are built in: `java`, `json`, `yaml` (or `yml`), `sql`,
`xml` (or `html`), `bash` (or `sh`) and `markdown` (or `md`). A block without a
language, or with one the view does not know, keeps the one code colour. The
*Code colours* card below the editor changes the colours at once. Selecting,
copying and zooming work in a coloured block as in any other.

```java
/** Reads the rows of a file, at most MAX_ROWS of them. */
@Override
public List<Row> read(Path file) throws IOException {
    // A missing value stays null.
    int limit = Math.min(MAX_ROWS, 0x7FFF);
    String header = "name\tamount";
    boolean done = false;
    Row last = null;
    return rows.subList(0, limit + 1);
}
```

```json
{
  "name": "orders",
  "path": "C:\\data\\orders.csv",
  "rows": 1200,
  "ratio": 0.75,
  "header": true,
  "note": null,
  "columns": ["id", "amount"]
}
```

```yaml
# The reader's settings
defaults: &defaults
  encoding: "UTF-8"
  limit: 500
  header: true
  missing: ~
reader:
  <<: *defaults
  path: "C:\\data\\orders.csv"
  columns:
    - id
    - 'amount'
```

```sql
-- Customers with more than ten orders since a date
SELECT c.name, count(*) AS orders
FROM customers c
JOIN orders o ON o.customer_id = c.id
WHERE o.created_at >= :since AND c.deleted_at IS NULL
GROUP BY c.name
HAVING count(*) > 10;

CREATE TABLE orders (
  id BIGINT PRIMARY KEY,
  amount NUMERIC(10, 2) DEFAULT 0,
  note VARCHAR(200) DEFAULT 'none'
);
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- The reader's settings -->
<reader encoding="UTF-8" limit="500">
  <path>C:\data\orders.csv</path>
  <title>Orders &amp; returns</title>
  <filter><![CDATA[amount > 100 && note != '']]></filter>
</reader>
```

```bash
#!/bin/bash
# Copies the newest CSV files to a backup folder.
backup() {
  target=${1:-/tmp/backup}
  for file in $SOURCE/*.csv; do
    if [ -f "$file" ]; then
      cp "$file" "$target" 2>&1 || return 1
    fi
  done
}
backup '/data/backup'
echo $?
```

```markdown
# Reader

Reads a *CSV* file with **headers**, see [the guide](docs/guide.md).

- `path`: the file to read
- `limit`: at most 500 rows

> Missing values stay empty.

---
```

## Tables

A table shows as a table. Its columns share the view's width as in a web browser: short columns
keep their cells on one line, long text wraps and its row grows with it, and every row shows: the
table never scrolls by itself, and the wheel over it scrolls the page. Make the window narrow to
see the rows grow:

| Setting | Values | Default |
|---|---|:-:|
| **Mode** | `Per row`, `Whole table` | `Per row` |
| **On error** | `Fail`, `Set missing and count` | `Fail` |
| **Locale** | a language tag such as `de-CH`, `fr` or `en-US`; empty, the output is locale-neutral | empty |

The colons in the row under the header align a column: `:--` left, `:-:` centred as the defaults
above, `--:` right as the row counts below.

Click a header to sort by its column: once ascending, again descending, a third time back to
the order of the source. Shift and a click sort by a second column too. Numbers in the text sort
by their value and case does not count, so `part2` comes before `part10`. Drag a header to move
its column, drag the edge between two headers to resize the column on its left, and use the
**+** at the right end of the header to show and hide columns:

| File | Rows | Encoding |
|:--|--:|---|
| `part10.csv` | 1200 | UTF-8 |
| `Part2.csv` | 980 | windows-1252 |
| `part1.csv` | 15000 | UTF-8 |
| `PART3.csv` | 75 | UTF-16 |
| `part21.csv` | 300 | ISO-8859-1 |

Select cells as in a spreadsheet: click a cell, drag over cells for a rectangle, Shift and a
click for the rectangle from the cell clicked before, Ctrl and a click to add or remove one cell.
Shift and the arrow keys extend the selection, Shift+Space makes it whole rows, Ctrl+A takes every
cell. A sort clears the selection. Ctrl+C copies the selected cells as tab-separated text that
pastes into a spreadsheet cell by cell; a right click on a cell or a header offers *Copy*, *Copy
with header* (the column titles first) and *Select column*, and a right click on a code block
*Copy* for its selected text.

A table inside a list item sits in the item:

- **Patterns** — `.format(pattern)` takes a Java date-time pattern:

  | Pattern | On `2026-10-31` |
  |---|---|
  | `dd.MM.yyyy` | `31.10.2026` |
  | `EEEE, d. MMMM yyyy` | `Saturday, 31. October 2026` |

- **Zones** — an instant formats in UTC unless you name a zone.

A column is never narrower than its longest word, so a code name never breaks. When even the
longest words do not fit, the table keeps them whole and scrolls sideways under a bar of its own:
Shift and the wheel, the touchpad or the bar move it, the plain wheel still scrolls the page. Make
the window narrow to see it:

| Type | Java type | Token |
|---|---|---|
| `local-date-time` | `LocalDateTime` | `tLocalDateTime` |
| `list-boolean` | `List<Boolean>` | `tListBoolean` |
| `instant` | `Instant` | `tInstant` |

## Other source

HTML blocks and code indented by four spaces show as their source lines, in a monospace block
that keeps every line whole and scrolls sideways like a code block:

<details>
  <summary>More</summary>
</details>

    // indented code keeps its layout
    int x = 1;

## Line breaks

A line that ends with two spaces  
breaks here, and one that ends with a backslash\
breaks here as well.

### Level 3 heading

Text under a level 3 heading.

#### Level 4 heading

##### Level 5 heading

###### Level 6 heading

The headings of levels 1 and 2 have a thin line underneath.
