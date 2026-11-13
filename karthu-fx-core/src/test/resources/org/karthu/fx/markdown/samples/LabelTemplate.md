# Label Template

Fills a label template once for every order and prints the text on the order's box — a greeting, the toppings, the
pick-up time, a file name for the receipt, a line for the delivery list —, or once over the whole day, for one text
built from all orders. The placeholders look like those of [Mustache](https://mustache.github.io/), and the template
has a small language of its own — conditions (`{{#if}}`), loops (`{{#each}}`), definitions (`{{#set}}`), and built-ins
such as `:upper`, `:replace` or `:json`.

Without an order book the template makes one text, from the shop values and its own values.

## Orders in

- **orders** — optional: the order book whose orders fill the template, one text per order or one text from all.

## Labels out

- **labels** — *Per order*: every column of the book in its order, with the label column, `TEXT`, appended at the end
  or in the place of the column it replaces (see *Result*). *Whole day*: one row with the label column alone. Without
  a book: one row with the label column alone.

## Settings

- **Mode** — **Per order**, one text per order; or **Whole day**, one text from all orders (see *Whole day*
  below).
- **Template** — the label template.
- **Values** — texts by name, such as the shop's name, a separator or a greeting, which the template reads as
  `{{name}}`. They are text; a number or a date belongs in the template (`:number`, `{{#set}}`). A name is a plain
  name — a letter or `_`, then letters, digits and `_` — and none of the data model's names (`order`, `orders`,
  `shop`, `columns`, `orderIndex`, `orderCount`) or the template's words (`true`, `false`, `and`, `or`, `not`, `as`,
  `in`, `else`).
- **Result** — **Append column** or **Replace column**, see *Result* below. *Replace column* applies per order only:
  with *Whole day* it is a problem before the template runs.
- **Column** — with *Append column* or *Whole day*: the label column's name, `label` by default.
- **Replaced column** — with *Replace column*: the book's column the text replaces.
- **On error** — **Stop** or **Leave empty and count**, see *Errors* below.
- **Locale** — a language tag such as `de-CH`, `fr` or `en-US`: number patterns (`:number("#,##0.00")`) and month
  and day names (`.format("EEEE, d. MMMM")`) follow it. Empty, the default, output is locale-neutral. A tag that is no
  language tag (`de_CH`) is a problem before the template runs.
- **Keep the day in memory** — *Whole day* only: makes `orders` a list you can count, index, sort and loop over more
  than once. Off by default, so a book of any size takes little memory. **On, the whole book must fit into memory.**
- **Result shop value** — *Whole day* only: a shop value's name. The text is also set as a `TEXT` shop value for the
  tools that follow. Empty, the default, sets none.

## Result

The text is always `TEXT`.

- **Append column** — the text becomes a new column named **Column** at the end; every column of the book stays where
  it was. A name the book has already stops the template, naming the column: there is no silent numbering. (The
  template sees the book's columns only when it runs, so this is no problem before the run.)
- **Replace column** — the text takes the place of **Replaced column**, which becomes a `TEXT` column; every other
  column stays where it was. The template still reads the replaced column's old value, `{{order.price}}` in a
  template replacing `price`. Without a chosen column, or with one the book lacks, the template does not run.

## Editor

The panel shows the settings, the template read-only and the problems found in the template and the values.
**Edit template …** opens the editor, a large window:

- **Left:** the book's **Columns**, the **Shop values** and your **Values**, each with a search field. Values is a
  table: **Add** a value, click a selected name or text to edit it, **Remove** the selected one. A name the template
  cannot read is marked, its tooltip says why.
- **Centre:** the template, with line numbers and the template language highlighted — blocks, placeholders, `{{` and
  `}}`, built-ins, `else`, comments, literals, keywords and the data model's names. The text around them, your label
  text, HTML or CSV, stays plain.
- **Below:** *Problems* and *Preview*.

A **double-click** on a name, or Enter, puts its placeholder at the caret:

| Double-clicked | Inserted |
|---|---|
| a column whose name is a plain name | `{{order.customer}}` |
| a column whose name is not | `{{order["Customer Name"]}}` |
| a shop value | `{{shop.city}}` |
| a value | `{{greeting}}` |

In *Whole day* mode, which has no `order`, a column double-clicked inside `{{#each orders as o}}…{{/each}}` inserts
`{{o.customer}}`; outside every such loop it inserts a loop of its own, `{{#each orders as o}}{{o.customer}}{{/each}}`,
and leaves the caret in it, so the next column joins it.

About 300 ms after you stop typing the editor checks the template:

- a **syntax error** is marked at its line and listed with its line and column, as the template would report it;
- a **warning**, yellow, marks a name that is not known: `order.x` or `order["x"]` (and `o.x` in a loop over
  `orders`) that is no column of the book, `shop.x` that is no shop value, and `{{x}}` that is no value, no name of
  the data model and nothing the template defines (`{{#set}}`, a loop variable, a definition); and `order` in *Whole
  day* mode or `orders` in *Per order* mode, which that mode's data model lacks. Warnings never stop the template.
  Dynamic accesses such as `order[c.name]` are not checked, and columns are not checked while the book is empty.

A double-click on a problem moves the caret to it. **Apply** writes the template and the values into the settings and
saves them; **Cancel** drops the edits.

## Saved templates

The editor's tab **Templates** keeps whole set-ups — the template, the values and every other setting — under a
name, a category and a description:

- **Create from current …** saves what the editor shows, its edits included. A template of the same category and name
  is replaced after a confirmation.
- **Apply** loads the selected template into the editor, after a confirmation: every setting is replaced. The
  editor's **Apply** writes it into the settings, its **Cancel** keeps them as they were.
- **Delete** removes a template of yours; **Export …** writes one into a file to share, by mail for example, and
  **Import …** reads such a file.

Your templates live in the folder `label-templates` of the app's user data directory, one file each, format
`label-template`, version 1. A file is read strictly: one with a setting the app does not have, a value of the wrong
kind or a choice that is none of the setting's options is refused, and the editor says why.

The category **Examples** comes with the app. Its templates can be applied and exported, but not deleted or replaced,
and none of yours is saved into it; a template of yours named like an example lives in your own category. Each one is
commented: open it and read the template.

| Example | Mode | What it shows |
|---|---|---|
| **Delivery list** | Whole day | one list over all orders, grouped by street; texts trimmed, a missing street as `(pick-up)`; the value `driver` names the driver |
| **Box label** | Per order | one label per order: `:upper`, `:trim`, the toppings as a list, `none` for a missing note |
| **CSV** | Whole day | a header and a line per order, generic over `columns`; fields quoted as RFC 4180 asks; the value `separator` |
| **Markdown table** | Whole day | a table generic over `columns`, `\|` and `<br>` escaped |
| **HTML table** | Whole day | a table generic over `columns`; `{{#escape "html"}}` escapes every value |
| **File name** | Per order | `{{shop.folder}}/{{order.day}}/{{order.id}}.pdf` from a shop value and columns |
| **Missing values** | Per order | `else`, `else "default"`, `{{#if order.note}}` and parentheses around an expression |
| **Dates and times** | Per order | the section *Dates and times* below as a commented tutorial; try it with the **Locale** `de-CH` and `fr-CH` |

## Preview

The editor's *Preview* tab fills the template from one **test order**, as the template is filled from an order of the
book:

- a field per **column** of the book and per **shop value**, filled in with a typical value of its kind: `42`, `3.14`,
  `true`, `text`, today's date such as `2027-03-14`, the time, an id. A value is written as the order book shows it.
  **An empty field is missing**, so you see what `else`, `{{#if}}` or a missing value's error do;
- a field whose text is no value of its kind is marked red and says why; the preview waits until it is fixed.

The text is made by the template's own code, about 300 ms after you stop typing in the template, the values or a
field: the **Locale**, your **Values** and the **Mode** apply as set, the formatting is the template's, and an error
is the template's message with its line and column. In *Whole day* mode `orders` holds the one test order and
`orderCount` is 1 — streaming, as the real `orders`, unless *Keep the day in memory* is ticked. Without a book the
template is filled without `order` and `orders`, as it is filled without a book, and the preview says so.

The test values live in the window only: they are never saved. A long text is shown up to its first 100000
characters, and a template that runs longer than 2 s is stopped. Real orders in the preview come with a later
version.

## The data model

| Name | What it holds |
|---|---|
| `order` | *Per order*: the current order, its columns by their **original names**: `{{order.customer}}`, and `{{order["Customer Name"]}}` for a name with a space, an accent or another character a plain name does not take |
| `orders` | *Whole day*: the orders, each like `order`: `{{#each orders as o}}{{o.customer}}{{/each}}` |
| `shop` | the shop values by name: `{{shop.city}}` |
| your values | top-level names: `{{greeting}}` |
| `columns` | the book's columns, each with its `name` and its `kind`: `{{#each columns as c}}{{c.name}}{{/each}}` |
| `orderIndex` | the current order's number, counted from 0; 0 in *Whole day* mode |
| `orderCount` | the book's number of orders, known from the first order on |

The columns are under `order` and the shop values under `shop`, so a column, a shop value and one of your values
never clash, however they are named. `order`, `orders`, `shop`, `columns`, `orderIndex` and `orderCount` cannot name
one of your values. Without a book, `order` and `orders` are absent (`{{#if order}}` is false), `columns` is empty and
`orderIndex` and `orderCount` are 0.

`columns` makes templates that fit any book, for example a Markdown table's header:
`| {{#each columns as c}}{{c.name}} | {{/each}}`, and with `order[c.name]` each order's cells. `orderIndex` and
`orderCount` number the orders and find the last one: `{{orderIndex + 1}}/{{orderCount}}`, `{{#if orderIndex ==
orderCount - 1}}`.

## Kinds

Values keep their kind, so the template compares and formats them:

| Kind | In the template | Printed with `{{…}}` | For example |
|---|---|---|---|
| COUNT8 to COUNT64, REAL32, REAL64, money | number | `42000`, `0.1`, `10000000000` | `{{#if order.loaves > 100}}`, `{{order.price:number("0.00")}}`, `{{order.loaves * 2}}` |
| FLAG | boolean | `true`, `false` | `{{#if order.gift}}`, `{{order.gift:then("yes", "no")}}` |
| TEXT, id, id-compact, file, json | string | the text; a file with `/` | `{{order.name:upper}}`, `{{order.payload:json.id}}` |
| BYTES | string | Base64 (`AQID`) | `{{order.photo:length}}` |
| list-count, list-real, list-flag, list-text, list-json | sequence | `[1, null, 3]`, `["rye", "spelt"]` | `{{#each order.toppings as t}}`, `{{order.toppings:size}}`, `{{order.toppings:join(", ")}}` |
| day, day-time, clock, moment (and their -micros and -nanos variants) | date and time, see the next section | `2027-03-14` | `{{order.day.format("dd.MM.yyyy")}}` |
| shop values TEXT, WHOLE, REAL, FLAG | string, number, number, boolean | as above | `{{shop.ovens + 1}}` |

- **Numbers** print in a fixed format, the same on every machine: no grouping, a `.` as the decimal point, no
  exponent. Money stays exact: `0.10 * 3` is `0.3`. For a pattern, use `:number`: `:number("0.00")`,
  `:number("#,##0.00")` — its grouping and decimal point follow the **Locale**.
- **json** is text; `:json` parses it into maps, lists, strings, numbers and booleans:
  `{{#set p = order.payload:json}}{{p.customer.name}}`.
- **Lists** keep a missing element missing: `{{#each order.weights as w}}{{w else 0}}{{/each}}`.
- **Values are data only.** A template cannot call a Java method of a value: `{{order.name.length()}}` fails, use
  `{{order.name:length}}`.

## Dates and times

The template gives dates and times a model of its own, which keeps them exact and unambiguous: a day is no moment,
and a moment has no wall-clock time until you name a zone. The example **Dates and times** shows all of this section
on an order of its own; the outputs below are its outputs:

| Kind | Printed with `{{…}}` | Parts |
|---|---|---|
| day | `2027-03-14` | `year`, `month`, `dayOfMonth`, `dayOfWeek` |
| day-time (-micros, -nanos) | `2027-03-14T06:30:00`, with a fraction when there is one | all seven |
| clock | `06:30:00` | `hour`, `minute`, `second` |
| moment (-micros, -nanos) | `2027-03-14T05:30:00Z`, always in UTC | all seven, in UTC |

The printed text is the ISO text the order book shows everywhere, the same on every machine and in every locale. A
clock always prints its seconds, also when they are 0: `06:30:00`; for `06:30` write `.format("HH:mm")`.

- **`.format(pattern)`** formats with a Java date-time pattern: `d` and `dd` the day, `M` and `MM` the month, `MMMM`
  its name, `yyyy` the year, `EEEE` the day's name, `HH` the hour 0 to 23, `mm` the minute, `ss` the second, text in
  single quotes as it is (`'T'`). A pattern must fit the value: a day has no hour, a clock no year.

  | Pattern | On `2027-03-14`, `2027-03-14T06:30:00`, `06:30:00` |
  |---|---|
  | `dd.MM.yyyy` | `14.03.2027` |
  | `d. MMMM yyyy` | `14. Mar 2027`; with `fr-CH` `14. mars 2027` |
  | `EEEE, d. MMMM yyyy` | `Sun, 14. Mar 2027`; with `fr-CH` `dimanche, 14. mars 2027` |
  | `yyyyMMdd` | `20270314` |
  | `yyyy-MM-dd'T'HH:mm` | `2027-03-14T06:30` |
  | `HH:mm:ss` | `06:30:00` |

- **The Locale** decides the language of month and day names, and nothing else. Without a locale they are
  locale-neutral short forms, **also for `EEEE` and `MMMM`**: `Sun`, `Mar`. With `fr-CH` `dimanche`, `mars` (and
  `dim.`, `mars` for `EEE`, `MMM`); with `en` `Sunday`, `March`.
- **A moment** is a point in time, not a wall-clock time: it formats in UTC unless you name a zone,
  `{{order.placed.format("dd.MM.yyyy HH:mm", "Europe/Zurich")}}`. A zone is an ID such as `Europe/Zurich`, `UTC` or
  `+02:00`. `Europe/Zurich` knows summer time: `2027-03-14T05:30:00Z` is `06:30` there, `2027-07-01T10:00:00Z` is
  `12:00`; the pattern letters `XXX` print the offset, `+01:00` and `+02:00`. Days, day-times and clocks take no zone.
- **The parts** are numbers: `{{order.day.year}}`, `{{order.day.month}}` (1 to 12), `{{order.day.dayOfMonth}}`,
  `{{order.day.dayOfWeek}}` (1 is Monday, 7 is Sunday), `{{order.ready.hour}}`, `{{order.ready.minute}}`,
  `{{order.ready.second}}`. They calculate: `{{#if order.day.dayOfWeek >= 6}}weekend{{/if}}`, the quarter
  `{{(order.day.month - 1) / 3 + 1}}`.
- **Comparisons**: `order.ready.isBefore(order.closing)` and `isAfter`, with a value of the same kind or its ISO text:
  `{{#if order.day.isBefore("2028-01-01")}}`.
- **Missing dates** are missing like any value: `{{order.day.format("dd.MM.yyyy") else "–"}}`, `{{#if order.day}}`.
- **`:text(…)`, `:date`, `:time`, `:datetime` and `:utc` do not apply.** They are built-ins for text; to them a date
  here is no text, so `{{order.day:text("dd.MM.yyyy")}}` and `:utc` fail, and `:date` fails too or, given a pattern,
  parses the printed text anew, a detour that gains nothing. Instead:

  | Instead of | Write |
  |---|---|
  | `{{order.day:text("dd.MM.yyyy")}}` | `{{order.day.format("dd.MM.yyyy")}}` |
  | `{{order.placed:utc}}` | `{{order.placed}}`, ISO in UTC already |
  | `{{order.day:date}}`, `{{order.ready:time}}` | `{{order.day}}`, `{{order.ready}}`: a date and a time already |

  A date held as text in a `TEXT` column, such as `14.03.2027`, is best turned into a day column before it reaches
  the template.

Errors say what is wrong: a pattern that is no pattern, a pattern that asks a day for its hour, an unknown zone, a
part the kind lacks (`A clock has no 'year'`), an ISO text that is none.

## Whole day

With **Mode** *Whole day* the template runs once and loops over the orders itself. One text comes out, in one row of
the column named by **Column**. A delivery list, from the columns `customer` and `loaves`:

```
DELIVERIES FOR {{shop.city}}
{{#each orders as o}}{{o.customer}} ({{o.loaves}}){{#sep}}, {{/sep}}{{/each}}
```

gives the line `Anna (2), Ben (12), Carla (1)` below the heading `DELIVERIES FOR Basel`, with the shop value `city`
set to `Basel`. Set **Result shop value** to `deliveries`, and the driver's tablet that follows reads it as a shop value.

**`orders` streams by default**: the orders are read while the template loops over them, so a book of millions of
orders takes no more memory than a few. The price: `orders` can be looped over **once**. A second
`{{#each orders}}`, `orders:size`, `orders[0]`, `orders:sort("name")` and `orders:any` fail with a message that points
to **Keep the day in memory**. For the number of orders use `orderCount`, which streaming knows up front; for an
empty book, `{{#each orders as o}}…{{#else}}no orders{{/each}}`.

**Keep the day in memory** reads all orders before the template runs, and `orders` becomes an ordinary list: sort it,
count it, index it, loop over it twice. The whole book is then held in memory — fine for thousands of orders, not for
millions.

Without a book, `orders` is absent (`{{#if orders}}` is false) and `orderCount` is 0; a book without orders gives an
empty `orders`.

## Missing values

A missing value is missing in the template too, as is a name that is neither a column, a shop value nor one of your
values. Printing it fails the order, so a gap never passes as text. Say what a gap means where it may occur:

- `{{order.note else ""}}` — nothing;
- `{{order.note else "none"}}` — a default;
- `{{#if order.note}}…{{#else}}…{{/if}}` — a branch.

## Errors

- **Syntax errors** are found before the template runs. It does not run, and the problem names the line and column
  in the template.
- **Other problems found before the run**, each at its setting: an empty template; *Replace column* without a column
  or with one the book lacks; a value name that is no plain name or is reserved; a locale that is no language tag; a
  blank result shop value name.
- **An error while an order is filled** — a missing value printed without `else`, a built-in on the wrong kind of
  value — goes by **On error**:
  - **Stop**, the default — the template stops. The message names the order, counted from 0, then the line and
    column in the template and the explanation: `Order 3: line 2, column 8: …`.
  - **Leave empty and count** — the order's label is missing and the template goes on. The first 100 failing orders
    are logged one by one as warnings, the rest counted in one more warning, and a note counts them all and names
    the first ten orders. Without a book, and in *Whole day* mode, the one text is missing, logged as a warning and
    counted by a note, and no shop value is set.

  An error message never ends up in a cell as if it were a label.
- **In *Whole day* mode** an error while the text is filled — a streaming `orders` looped over twice among them —
  goes by **On error** too, naming the line and column but no order.

## Safety

An order book's template comes from whoever wrote it, so the template runs restrictively:

- a built-in creates no Java object, and there is no way into the app's own classes;
- there is no file access: `{{#include}}` and `{{#import}}` read no file and fail;
- values are data only: a template cannot call a Java method of a value.

A template can only turn the order, the shop values and your values into text.

## Formats

Output is the same on every machine: numbers in a fixed format (`42000`, `0.1`), booleans as `true` and `false`, text
in UTF-8, moments in UTC — never the format of the computer's locale. The **Locale** changes number patterns and month
and day names only, never what `{{…}}` prints.
