# Recipe Lookup

Picks values out of a column of recipes written as JSON, with JSONPath queries: a recipe's name, its baker's name, the
ingredients above a weight, the number of steps. Each query gives one column with the name you choose. It works on any
text column that holds JSON, such as the recipes a supplier sends or the recipe card export, and on JSON columns. Where
the recipe card import turns every value into a column, the lookup takes only the ones you ask for.

The queries follow JSONPath as most libraries read it, so a path taken from a JSONPath tutorial works here too: filters
(`[?(@.grams < 10)]`, `==`, `=~`, `in`, `nin`, `size`, `empty`, …), slices (`[1:3]`, `[-2:]`), deep scans
(`$..flour`) and functions (`length()`, `min()`, `max()`, `avg()`, `sum()`, `keys()`, …).

## Recipes in

- **recipes** — the table with the column of JSON text.

## Lookups out

- **found** — the recipes with one new column per query, right after the JSON column, in the order of the queries; in
  its place when it is removed. Every other column and every row stay as they are: one row out for every row in. The
  columns depend on the settings and the table's columns alone, never on the recipes.

A query named like a column that stays gets ` (#1)`, ` (#2)`, … : with the columns `id`, `json` and `x` and the
queries `a` and `x`, the result is `id`, `json`, `a`, `x (#1)`, `x`. A removed JSON column frees its name: with
Remove source column on, a query may be called like the JSON column.

## Settings

- **JSON column** — the column whose JSON the queries run on. Only text columns and JSON columns are offered.
- **Remove source column** — off by default: the JSON column stays, and the new columns follow it. On, it is removed
  and the new columns take its place.
- **Lookups** — the queries, one per row of the table, at least one. A new query is `column`, `$..*`, JSON, List on —
  everything the JSON holds:
  - **Column** — the name of the query's column, `column` for a new query. Each name is used once.
  - **JSONPath** — the path: `$.recipe.name`, `$.ingredients[*].name`, `$.ingredients[?(@.grams > 500)].name`,
    `$['baker name']`.
  - **Kind** — what each value is read as: **Flag**, **Whole**, **Real**, **Text** or **JSON** (the
    default).
  - **List** — whether the query gives a list of values (on for a new query).
  - **Paths** — whether the query gives the paths that match instead of their values. The kind and List do not
    apply then, and are greyed out.
- **On invalid value** — **Stop** (the default) or **Leave empty**: what text that is no JSON, a value that does not
  fit its kind, or a query that cannot be evaluated does (see Invalid values below).

**What the dialog marks**, each in its cell, before anything runs: no query; an empty column name or JSONPath; a column
name used twice (names compare exactly, as column names do); a JSONPath that does not compile, with the reason and
the position, counted from 0 (`$.a[` — the `[` at position 3 is never closed); a path that can match several values
without List, unless the kind is JSON; and Paths on a path that ends in a function, whose result has no path. A
greyed-out field is not checked.

## Kinds

Each value is converted strictly to the query's kind; a value the kind cannot hold is invalid (see On invalid value):

| Value | Flag | Whole | Real | Text | JSON |
|---|---|---|---|---|---|
| `true` | `true` | invalid | invalid | `true` | `true` |
| `42` | invalid | `42` | `42.0` | `42` | `42` |
| `3.0` | invalid | `3` | `3.0` | `3.0` | `3.0` |
| `3.5` | invalid | invalid | `3.5` | `3.5` | `3.5` |
| `2.50` | invalid | invalid | `2.5` | `2.5` | `2.50` |
| `3.0000000000000001` | invalid | invalid | `3.0` | `3.0` | `3.0000000000000001` |
| a whole number beyond Whole | invalid | invalid | rounded | exact | exact |
| a number beyond Real, `1e400` | invalid | invalid | invalid | `Infinity` | `1e400` |
| `"7"` | invalid | invalid | invalid | `7` | `"7"` |
| an object or an array | invalid | invalid | invalid | its JSON text | its JSON text |

A text is never read as a number or a flag: `"7"` is no Whole. Use the Convert menu afterwards if you need that. Whole
takes a number whose exact value is a whole number within its range: `3.0` and `1e2` are, `3.0000000000000001` is not.
A number read as Text takes its shortest text: `2.5`, `1.0E10`. A JSON column keeps a number as the recipe wrote it,
every digit — `2.50`, `0.12345678901234567890` —; it holds valid JSON, so a text keeps its quotes there.

## Lists and paths

Whether a query gives one value or a list depends on its path and on **List**:

- A **definite** path — one that can reach one value at most, such as `$.recipe.id` or `$.steps[0]`, and one that ends
  in a function, such as `$..ingredients.length()` — gives that value. With List on, it gives a list: an array's
  elements (`$.tags` gives `[vegan, quick]`), or the one value alone.
- An **indefinite** path — one that can match several values, with `*`, `..`, a filter, a slice or several indices —
  gives a list with List on, read as the query's kind: `$.ingredients[*].grams` as Whole gives `[500, 10]`. With List
  off, only the kind JSON is allowed: it gives a JSON array always, also `[]` and `[500]`, so the column's shape never
  depends on how many values matched. Any other kind without List is marked in the dialog.
- Each element of a list is converted on its own; a `null` element stays a `null` element.
- **Paths** gives a list of the matching paths in bracket notation, `$['ingredients'][0]['grams']`, whatever the path:
  one path or none for a definite path. A function has no path; Paths on a path that ends in a function is marked in
  the dialog.

## Missing values

- A definite path that matches nothing gives a missing value: `$.steps[2].text` when a recipe has two steps.
- An indefinite path that matches nothing gives an empty list, or `[]` as JSON — not a missing value.
- A JSON `null` gives a missing value; inside a list, a `null` element.
- A missing cell, empty text and text of only white space are no recipe: missing values in every query column, no
  error and no warning.

## Invalid values

Three things make a value invalid: text that is no JSON — read strictly: `{a:1}` is no JSON —, a value that does not
fit its kind (`"abc"` or `3.5` as Whole, an object as Flag), and a query that cannot be evaluated on a row
(`$.ingredients.max()` when the ingredients are no numbers). **On invalid value** decides:

- **Stop** — the lookup stops at the first, naming the row (counted from 0) and, for a value or a query, the query:
  `Row 3: the value "abc" does not fit the kind Whole of the query 'grams' ($.grams)`.
- **Leave empty** — every row is kept. Text that is no JSON gives missing values in every query column of its row; a
  value that does not fit, or a query that cannot be evaluated, a missing value in its own cell alone — the row's
  other queries keep theirs. A warning at the end counts the rows concerned and names the first ten; the log names
  the first hundred values with their reason.

The lookup reports its progress as it goes through the rows, and stops between two rows when it is stopped.

## Changes from version 1

The lookup keeps version 1's dialog, its queries and its defaults, except where version 1's result depended on the
recipes or a problem was lost silently:

1. **Conversion is strict.** Version 1 parsed text: `"7"` read as a number was 7 there. Now a text is never parsed, so
   `"7"` is no Whole; the Convert menu turns it into a number afterwards.
2. **On invalid value** decides what a value that cannot be read or converted does: the lookup stops, naming the row
   and the query, or — with Leave empty — the value is missing and a warning counts it. Version 1 made every such
   value missing without a word.
3. **A path that can match several values, without List**, is marked in the dialog unless the kind is JSON, and as
   JSON it always gives an array — `[]`, `[x]`, `[x,y]`. Version 1 gave a missing value for no match, the value itself
   for one, and failed for more, so the column's shape depended on the recipes.
4. **List with a definite path to an array gives the array's elements**, `$.tags` gives `[vegan, quick]`; version 1
   gave a list of one element, the array.
5. **Whole only**: there is no small whole kind (32-bit), and no Base64.
6. **The new columns follow the JSON column**, in the order of the queries; version 1 appended them at the end of the
   table.
7. **The JSON column is required**, chosen by you; version 1 guessed one.
8. **JSON is read strictly**, with the parser the recipe card import uses, so both agree on what is valid JSON;
   version 1 used a lenient parser, which accepted `{a:1}`.
9. **Mistakes that can be seen without recipes are marked in the dialog**, before the run: a path that does not
   compile, an empty or duplicate name, a path that can match several values without List. Version 1 reported them
   when it ran.
