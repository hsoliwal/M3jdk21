<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# INTERFACE: the jdk.m3 CSV API, superset of univocity-parsers, commons-csv and jackson-dataformat-csv

Status: specification v1, spec level only. The Java types below are generated later through Chrome local AI, one
call per file (HOLD while it is unavailable). Capabilities: `CSV_CAPABILITIES.tsv` (78, from 386 donor keys);
synonyms and semantics: `CSV_CAPABILITY_SYNONYMS.tsv` (CSV-CAPABILITY v1).

## Rules

1. Superset: every CORE and EXTENDED capability has exactly one home in the API (table below); INTEGRATION
   capabilities have an adapter; INTERNAL has none. `tools/run_gates.sh` checks that every capability id appears in
   the table.
2. One implementation: the reader, writer and dialect are implemented once, in the JDK; the three donors keep their
   own public APIs (user rule "keep upstream + M3 facades") and are rewired to this implementation by recipe.
3. Donor defaults are presets, not the API's defaults. The API's own default is RFC 4180. `predefined(...)` returns
   each donor's exact defaults, so a facade never changes behaviour.
4. Immutable dialect values with builders (commons, jackson style); readers and writers are single-use and
   `AutoCloseable`. No reflection, no class-path scanning.
5. Naming (user decision): module and package `jdk.m3.csv`; product types `M3JdkCsv*`.

## Types (module `jdk.m3.csv`, package `jdk.m3.csv`, exported)

| Type | Kind | Role |
|---|---|---|
| `M3JdkCsvFormat` | final class, immutable | The dialect and every reader/writer option. `builder()`, `toBuilder()`, `predefined(String)`, `rfc4180()`, `detect(Reader, int sampleRows)`. |
| `M3JdkCsvFormat.Builder` | final class | One setter per option below; `build()` validates combinations (e.g. quote = delimiter is refused). |
| `M3JdkFixedWidthFormat` | final class, immutable | Fixed-width dialect: field lengths, alignment, padding, lookahead/lookbehind layouts. Same reader/writer. |
| `M3JdkCsvSchema` | final class, immutable | Typed columns: name, type (`STRING`, `NUMBER`, `BOOLEAN`, `ARRAY`), array element separator, any-property, selection; `of(Class<? extends Record>)`. |
| `M3JdkCsvReader` | final class | `Iterable<M3JdkCsvRecord>`, `AutoCloseable`. Opened from `Reader`, `Path`, `InputStream` + `Charset`, `String`. |
| `M3JdkCsvRecord` | final class, immutable | One record: values, header lookup, typed getters, position, attached comment. |
| `M3JdkCsvWriter` | final class | `Flushable`, `AutoCloseable`. Opened on `Appendable`, `Writer`, `Path`, `OutputStream` + `Charset`. |
| `M3JdkCsvPosition` | record | `line`, `recordNumber`, `characterOffset`, `column`. |
| `M3JdkCsvException` | class extends `UncheckedIOException` | Malformed input or a refused option; carries an `M3JdkCsvPosition` and an excerpt bounded by `errorContentLimit`. |
| `M3JdkCsvErrorHandler` | functional interface | `boolean onError(M3JdkCsvException e, M3JdkCsvRecord partial)`: return true to continue. |
| enums | | `QuotePolicy`, `Trim`, `UnescapedQuotes`, `ShortRows`, `ExtraColumns`, `DuplicateHeaders`, `HeaderMatch`, `TrailingDelimiter`, `WriteShortRows`, `Alignment` (values in the table). |

Sketch of the reading and writing surface (signatures only):

```java
M3JdkCsvFormat f = M3JdkCsvFormat.rfc4180().toBuilder().delimiter(";").headerFromFirstRecord(true).build();
try (M3JdkCsvReader r = M3JdkCsvReader.open(path, UTF_8, f)) {
    for (M3JdkCsvRecord rec : r) { String v = rec.get("name"); int n = rec.getInt(2); }
}
try (M3JdkCsvWriter w = M3JdkCsvWriter.open(out, f)) {
    w.writeHeader("id", "name");
    w.writeRecord(1, "a");
    w.value(2).value("b").endRecord();
}
```

## Capability homes

| Capability | Tier | Home in jdk.m3.csv |
|---|---|---|
| `dialect.delimiter` | CORE | `Builder.delimiter(String)` (one or more chars; `delimiter(char)` shorthand) |
| `dialect.quote` | CORE | `Builder.quote(char)`, `Builder.noQuote()` |
| `dialect.escape-char` | CORE | `Builder.escape(char)`, `Builder.noEscape()` (RFC 4180 doubling, the default) |
| `dialect.escape-escape` | EXTENDED | `Builder.escapeEscape(char)` |
| `dialect.escape-unquoted` | EXTENDED | `Builder.escapeUnquoted(boolean)` |
| `dialect.comment-marker` | CORE | `Builder.commentMarker(char)`, `Builder.noComments()` |
| `dialect.line-separator` | CORE | `Builder.recordSeparator(String)` (output; input accepts CR, LF, CRLF unless `strictRecordSeparator(true)`) |
| `dialect.line-separator-detection` | EXTENDED | `Builder.detectRecordSeparator(boolean)`; `M3JdkCsvReader.firstRecordSeparator()` |
| `dialect.normalized-newline` | EXTENDED | `Builder.normalizedNewline(char)` |
| `dialect.normalize-newlines-in-quotes` | EXTENDED | `Builder.normalizeNewlinesInQuotes(boolean)` |
| `dialect.trailing-delimiter` | EXTENDED | `Builder.trailingDelimiter(TrailingDelimiter.NONE / WRITE / TOLERATE)` |
| `dialect.tsv-escape` | EXTENDED | `Builder.tsvEscapes(char escape, char escapedTab)`; preset `TSV` |
| `dialect.tsv-line-joining` | EXTENDED | `Builder.tsvLineJoining(boolean)` |
| `fixed-width.padding-alignment` | EXTENDED | `M3JdkFixedWidthFormat.Builder.padding(char)`, `keepPadding(boolean)`, `alignment(Alignment)`, `headerPadding(char)` |
| `fixed-width.lookahead-formats` | EXTENDED | `M3JdkFixedWidthFormat.Builder.layoutForLookahead(String, ...)`, `layoutForLookbehind(String, ...)`, `lookupWildcard(char)` |
| `fixed-width.record-termination` | EXTENDED | `M3JdkFixedWidthFormat.Builder.recordEndsOnNewline(boolean)`, `skipTrailingCharsUntilNewline(boolean)`, `writeRecordSeparator(boolean)` |
| `format.predefined` | CORE | `M3JdkCsvFormat.predefined(name)`: `RFC4180`, `TSV`, commons `DEFAULT`, `EXCEL`, `INFORMIX_UNLOAD`, `INFORMIX_UNLOAD_CSV`, `MONGODB_CSV`, `MONGODB_TSV`, `MYSQL`, `ORACLE`, `POSTGRESQL_CSV`, `POSTGRESQL_TEXT`, `TDF`, and `UNIVOCITY_DEFAULT`, `JACKSON_DEFAULT` (each donor's defaults) |
| `format.builder` | CORE | `M3JdkCsvFormat.builder()`, `toBuilder()` |
| `format.dialect-object` | CORE | `M3JdkCsvFormat` itself (immutable, `equals`/`hashCode` by value); `M3JdkCsvReader.format()` |
| `header.names` | CORE | `Builder.header(String...)`, `Builder.header(Class<? extends Enum<?>>)` |
| `header.from-first-record` | CORE | `Builder.headerFromFirstRecord(boolean)` (writers: `writeHeader(boolean)`) |
| `header.skip-header-record` | CORE | `Builder.skipHeaderRecord(boolean)` |
| `header.access` | CORE | `M3JdkCsvReader.headerNames()`, `headerMap()`; `M3JdkCsvRecord.isMapped(String)` |
| `header.duplicate-names` | EXTENDED | `Builder.duplicateHeaders(DuplicateHeaders.ALLOW_ALL / ALLOW_EMPTY / DISALLOW)` |
| `header.completeness` | EXTENDED | `Builder.allowMissingHeaderNames(boolean)`, `Builder.failOnMissingHeaderColumns(boolean)` |
| `header.case-insensitive` | EXTENDED | `Builder.headerCaseInsensitive(boolean)` |
| `header.strict-match` | EXTENDED | `Builder.headerMatch(HeaderMatch.STRICT)` |
| `header.reorder-columns` | EXTENDED | `Builder.headerMatch(HeaderMatch.REORDER)` (default `LENIENT`) |
| `select.fields` | EXTENDED | `M3JdkCsvSchema.select(String...)`, `select(int...)`, `exclude(String...)`, `exclude(int...)`, `keepInputOrder(boolean)`; `M3JdkCsvWriter.select(...)` |
| `schema.columns-edit` | EXTENDED | `M3JdkCsvSchema.Builder.column(name, type)`, `remove`, `rename`, `replace`, `clear`, `sortedBy(Comparator)`, `append(M3JdkCsvSchema)` |
| `schema.column-access` | EXTENDED | `M3JdkCsvSchema.column(int)`, `column(String)`, `size()`, `columns()`: name, index, type, array flag |
| `schema.array-element-separator` | EXTENDED | `M3JdkCsvSchema.Builder.arrayElementSeparator(String)` per schema or column |
| `schema.any-property` | EXTENDED | `M3JdkCsvSchema.Builder.anyProperty(String)` |
| `schema.derive-from-type` | EXTENDED | `M3JdkCsvSchema.of(Class<? extends Record>)` (record components, in order); `M3JdkCsvRecord.as(Class<R>)` |
| `value.null-string` | CORE | `Builder.nullString(String)` |
| `value.empty-value` | EXTENDED | `Builder.emptyValue(String)`, `Builder.emptyAsNull(boolean)` |
| `value.trim-unquoted` | CORE | `Builder.trim(Trim.UNQUOTED)` |
| `value.trim-quoted` | EXTENDED | `Builder.trim(Trim.QUOTED)`, `Trim.ALL` (commons `trim`), `Trim.NONE` (default) |
| `value.keep-quotes` | EXTENDED | `Builder.keepQuotes(boolean)` |
| `value.keep-escape-sequences` | EXTENDED | `Builder.keepEscapeSequences(boolean)` |
| `value.skip-bits-as-whitespace` | EXTENDED | `Builder.bitsAsWhitespace(boolean)` |
| `parse.skip-empty-lines` | CORE | `Builder.skipEmptyLines(boolean)` |
| `parse.unescaped-quotes` | EXTENDED | `Builder.unescapedQuotes(UnescapedQuotes.RAISE_ERROR / STOP_AT_CLOSING_QUOTE / STOP_AT_DELIMITER / BACK_TO_DELIMITER / SKIP_VALUE / ACCEPT_TRAILING_DATA)` |
| `parse.lenient-eof` | EXTENDED | `Builder.lenientEof(boolean)` |
| `parse.format-detection` | EXTENDED | `M3JdkCsvFormat.detect(Reader, int sampleRows)`, `detect(Reader, int, String candidateDelimiters)` |
| `parse.missing-columns` | EXTENDED | `Builder.shortRows(ShortRows.AS_IS / FAIL / NULLS)`; `M3JdkCsvRecord.isConsistent()` |
| `parse.extra-columns` | EXTENDED | `Builder.extraColumns(ExtraColumns.KEEP / IGNORE / FAIL)` |
| `parse.skip-rows` | EXTENDED | `Builder.skipRows(long)`, `Builder.skipFirstDataRow(boolean)` |
| `parse.max-records` | EXTENDED | `Builder.maxRecords(long)` |
| `parse.input-control` | EXTENDED | `Builder.inputBufferSize(int)`, `Builder.readAhead(boolean)`; `M3JdkCsvReader.releaseBuffered(Writer)` |
| `parse.collect-comments` | EXTENDED | `Builder.collectComments(boolean)`; `M3JdkCsvRecord.comment()`, `M3JdkCsvReader.headerComment()`, `trailerComment()` |
| `limits` | EXTENDED | `Builder.maxCharsPerValue(int)`, `Builder.maxColumns(int)`, `Builder.maxRecordChars(long)` |
| `errors.handler` | EXTENDED | `Builder.errorHandler(M3JdkCsvErrorHandler)` |
| `errors.message-content-limit` | EXTENDED | `Builder.errorContentLimit(int)` |
| `read.open` | CORE | `M3JdkCsvReader.open(Reader / Path / InputStream, Charset / String, M3JdkCsvFormat [, M3JdkCsvSchema])` |
| `read.push` | EXTENDED | `M3JdkCsvReader.forEachRemaining(Consumer<M3JdkCsvRecord>)` |
| `read.all` | CORE | `M3JdkCsvReader.readAll()` |
| `read.stream` | CORE | `M3JdkCsvReader.next()` (`Optional`), `iterator()`, `stream()` |
| `read.single-line` | EXTENDED | `M3JdkCsvFormat.parseLine(String)` |
| `record.values` | CORE | `M3JdkCsvRecord.get(int)`, `get(String)`, `get(Enum<?>)`, `size()`, `isSet(int)`, `toList()`, `toMap()`, `putIn(Map)`, typed `getInt` / `getLong` / `getDouble` / `getBoolean` / `getBigDecimal` |
| `position.counters` | CORE | `M3JdkCsvReader.position()`, `M3JdkCsvRecord.position()`, `M3JdkCsvWriter.recordCount()` |
| `write.open` | CORE | `M3JdkCsvWriter.open(Appendable / Writer / Path / OutputStream, Charset, M3JdkCsvFormat [, M3JdkCsvSchema])`; `out()` |
| `write.record` | CORE | `M3JdkCsvWriter.writeRecord(Object...)`, `writeRecord(Iterable<?>)`; `M3JdkCsvFormat.format(Object...)` (one record as a String) |
| `write.records` | CORE | `M3JdkCsvWriter.writeRecords(Iterable<?>)`, `writeRecords(Object[][])`, `writeColumns(Map<String, ? extends Iterable<?>>)` |
| `write.incremental-row` | CORE | `M3JdkCsvWriter.value(Object)`, `endRecord()`, `discardRecord()`, `emptyRecord()` |
| `write.header` | CORE | `M3JdkCsvWriter.writeHeader(String...)`, `writeHeader()` (from format or schema), `Builder.writeHeader(boolean)` |
| `write.comments` | EXTENDED | `M3JdkCsvWriter.comment(String)`, `Builder.headerComments(String...)` |
| `write.quote-policy` | CORE | `Builder.quotePolicy(QuotePolicy.MINIMAL / ALL / ALL_NON_NULL / NON_NUMERIC / NONE / STRINGS / NON_EMPTY_STRINGS)`, `quoteColumns(String...)`, `quoteTriggers(String)`, `quoteNulls(boolean)`, `quoteValuesWithQuote(boolean)`, `strictQuoting(boolean)`, `quoteNumbers(boolean)` |
| `write.escaping` | EXTENDED | `Builder.escapeQuoteWithEscape(boolean)`, `escapeControlChars(boolean)`, `inputAlreadyEscaped(boolean)` |
| `write.short-rows` | EXTENDED | `Builder.writeShortRows(WriteShortRows.AS_IS / PAD / OMIT_TRAILING)` |
| `write.processed-records` | INTEGRATION | adapter `jdk.m3.csv.adapter.univocity`: univocity `RowWriterProcessor` over `M3JdkCsvWriter` |
| `lifecycle.close-flush` | CORE | `close()`, `flush()`, `Builder.autoFlush(boolean)`, `Builder.autoClose(boolean)`; `M3JdkCsvReader.isClosed()` |
| `integration.univocity-processors` | INTEGRATION | adapter `jdk.m3.csv.adapter.univocity`: `Processor` / `RowProcessor` fed by `M3JdkCsvReader` |
| `integration.jackson-streaming-read` | INTEGRATION | jackson-dataformat-csv `CsvParser` keeps the `JsonParser` protocol; its tokenizer delegates to `M3JdkCsvReader` |
| `integration.jackson-streaming-write` | INTEGRATION | jackson-dataformat-csv `CsvGenerator` keeps the `JsonGenerator` protocol; its encoder delegates to `M3JdkCsvWriter` |
| `integration.jackson-features` | INTEGRATION | stays in jackson; `CsvParser.Feature` / `CsvGenerator.Feature` map to `M3JdkCsvFormat` options (facade table below) |
| `integration.jackson-databind` | INTEGRATION | stays in jackson (`WRAP_AS_ARRAY`) |
| `internal` | INTERNAL | none |

## Facades: donor options to M3 options

`CSV_CAPABILITIES.tsv` lists, per capability, every donor method behind it; with the table above that is the facade
mapping, one recipe per donor:

| Donor type | Facade | Example rows |
|---|---|---|
| commons-csv `CSVFormat` / `CSVFormat.Builder` | builds an `M3JdkCsvFormat` from `predefined("DEFAULT")` | `setDelimiter(String)` -> `delimiter(String)`; `setQuoteMode(QuoteMode.ALL)` -> `quotePolicy(ALL)`; `setTrim(true)` -> `trim(Trim.ALL)`; `setIgnoreSurroundingSpaces(true)` -> `trim(Trim.UNQUOTED)` |
| univocity `CsvParserSettings` / `CsvWriterSettings` / `CsvFormat` | from `predefined("UNIVOCITY_DEFAULT")` | `getFormat().setQuoteEscape('\\')` -> `escape('\\')`; `setUnescapedQuoteHandling(STOP_AT_DELIMITER)` -> `unescapedQuotes(STOP_AT_DELIMITER)`; `setMaxColumns(n)` -> `maxColumns(n)` |
| jackson `CsvSchema`, `CsvParser.Feature`, `CsvGenerator.Feature` | from `predefined("JACKSON_DEFAULT")` | `withColumnSeparator(';')` -> `delimiter(";")`; `TRIM_SPACES` -> `trim(Trim.UNQUOTED)`; `ALWAYS_QUOTE_STRINGS` -> `quotePolicy(STRINGS)` |

False friends the facades must not merge (review of the v1 name matches): univocity `TsvFormat.escapeChar` is the
TSV escape (`tsvEscapes`), jackson `CsvSchema.escapeChar` is the CSV escape (`escape`); univocity
`CommonSettings.format` is the dialect object, commons `CSVFormat.format(Object...)` formats one record; commons
`header` is the list of names, jackson `header`/`useHeader` is the first-record flag.

## Next (not in this spec)

- Behaviour tests per capability: one discriminator input per option, run through each donor and through each
  facade on the M3 implementation, outputs equal (atoms from `m3/tooling/jdk-superset/csv/` in #2215).
- The generation inputs for Chrome: one LLD per type above, then one generation call per file.
