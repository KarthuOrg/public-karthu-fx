# Order Import

Reads the day's orders from a CSV file into the order book, every field as text.

## Result

- **orders** — one text field per name in the header line, or per field of the first line when the file has no
  header. Every value stays text: the import guesses no kinds. Turn fields into numbers or dates later, where you
  need them: the order book's Convert menu reads numbers, dates, times of day and dates with a time.

## Settings

- **File** — the CSV file. A relative path starts at the order book's folder, so the book and its files can move
  together. The file is read as UTF-8; a byte order mark at its very start is passed over.
- **Delimiter** — the character between two fields: `,` (the default), `;`, `tab` or `|`.
- **First line names the fields** — on (the default): the first line gives the field names. Off: the fields are
  called `field1`, `field2`, … and the first line is an order. A name that repeats gets a number: `bread`, `bread_1`.
  A first line that ends in a delimiter, `a,b,`, has a third field with the empty name.
- **Broken rows** — what happens to a row that does not fit:
  - `stop` (the default): the import stops, and its message names the line and the problem.
  - `skip and note`: the import passes over the row, writes its line number to the import log, and tells at the
    end how many rows it passed over. No row is dropped unnoticed.

## How the text is read

- Fields follow RFC 4180: a quote at the start of a field opens a quoted field, which may hold the delimiter and line
  breaks; inside it two quotes in a row stand for one quote, and after its closing quote comes the delimiter or the
  end of the line.
- Lines end in LF, CRLF or CR; a line break inside a quoted field reads as LF.
- An empty line holds no order and is passed over, except in a file of one field, where it is an order with an empty
  value.
- A row is broken when
  - it has more or fewer fields than the first line,
  - a quote stands inside a field that does not begin with one (`9" tin`), or text follows a closing quote
    (`"rye"loaf`),
  - a quote is never closed (the row then runs to the end of the file, or to the next line end once it is too
    long, below),
  - its bytes are not UTF-8 text (a byte that is not, such as `é` in a Windows-1252 file, reads as the replacement
    character U+FFFD, and a row holding that character counts as broken), or
  - it is longer than 16 777 216 characters: none of it is kept, and it ends at the next line end.
- In `skip and note` mode the first hundred skipped rows are written to the log one by one, the rest only counted.
- A problem of the first line stops the import in either mode, since that line gives the fields. A file that does
  not exist, or holds no line at all, stops it too.

The progress bar follows the bytes read so far.
