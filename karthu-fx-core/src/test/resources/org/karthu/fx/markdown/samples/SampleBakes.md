# Sample Bakes

Opens a frozen sample table from the Bakery Archive, for trying out the app without your own orders.

## Result

- **table** — the chosen table, exactly as the archive published it.

## Settings

- **Sample** — the table to open, one entry per table, sorted alphabetically. A fresh sample reports an error until
  a table is chosen.

## The data

- **Frozen:** the samples ship inside the app, byte for byte as the archive hands them out. An entry gives the same
  table on every run, machine and later version of the app; a correction comes as a new entry, the old one stays.
- **Original order:** the rows come as in the file — no shuffling, no sampling, no split (use **Split rows**).
- **Values as written:** numbers are numbers, everything else is text exactly as the archive wrote it.
- **Missing values:** an empty field, or a sample's missing-value mark such as `?`, is missing.
- **Origin:** the table carries the notes `sample`, `archive.id`, `source`, `license` and `citation`. Each sample's
  licence asks you to name its source; the app's licence page names them.

## Rye Bread

Test bakes of 2019 — flour, water, salt, proving time, oven temperature and more — for predicting whether a loaf
rises above 9 cm, split by the archive into a training and a test table. The test table's labels end in a period
(`>9cm.`).

- **Source:** https://example.org/archive/2/rye+bread
- **Licence:** CC BY 4.0

| Entry | Rows | Columns |
|---|---|---|
| Rye Bread - Test | 12480 | 14 |
| Rye Bread - Training | 37440 | 14 |

## Sourdough Starter Log

Daily notes of sourdough starters kept in home kitchens, with feeding times, flour, water, room temperature and
rise. The column kinds come from the data, since the archive page's kinds are shifted against the columns.

- **Source:** https://example.org/archive/555/sourdough+starter+log
- **Licence:** CC BY 4.0

| Entry | Rows | Columns |
|---|---|---|
| Starter Log - 10K | 10000 | 22 |

## Bakers' Shift Notes

Shifts of a cooperative of village bakeries — start time, oven, batches, breaks and the outcome `Sold out`, `Left
over` or `Given away`. The codes stay numbers, as the file writes them; the two weights are reals, against the
archive page, which kinds them whole.

- **Source:** https://example.org/archive/697/bakers+shift+notes+and+outcomes
- **Licence:** CC BY 4.0

| Entry | Rows | Columns |
|---|---|---|
| Shift Notes - Data | 4424 | 37 |

## Mill Records (Flour Deliveries)

Flour deliveries described by 20 attributes — mill, grain, grind, moisture, weight, price and more — each rated as
a good (`1`) or poor (`2`) bake. The values are the original codes (`A11`).

- **Source:** https://example.org/archive/144/mill+records+flour+deliveries
- **Licence:** CC BY 4.0

| Entry | Rows | Columns |
|---|---|---|
| Mill Records - Original | 1000 | 21 |

## Customer Tastings

Marks, comments and habits of the guests of two tasting days, one table per bakery: Town Square and Harbour.

- **Source:** https://example.org/archive/320/customer+tastings
- **Licence:** CC BY 4.0

| Entry | Rows | Columns |
|---|---|---|
| Customer Tastings - Harbour | 395 | 33 |
| Customer Tastings - Town Square | 649 | 33 |
