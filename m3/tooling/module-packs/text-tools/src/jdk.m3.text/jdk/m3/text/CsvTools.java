// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * RFC-4180-style CSV/TSV projection over immutable UTF-16 CharSequence input.
 * The caller owns input immutability and lifetime. Original MIndexString and M3
 * String can be passed directly: parsing reads charAt without calling toString,
 * and fields hold logical ranges, not copies of the input payload.
 * Bare quotes, unclosed quoted fields, and junk after a closing quote refuse.
 * LF, CRLF and CR line endings are accepted; output framing is explicit.
 */
public final class CsvTools {
    private CsvTools() { }

    /** Hard admission budgets; not a JVM heap or CPU timeout guarantee. */
    public record Limits(int maxInputUnits, int maxRows, int maxColumns, int maxFieldUnits) {
        public Limits {
            if (maxInputUnits < 1 || maxRows < 1 || maxColumns < 1 || maxFieldUnits < 1)
                throw new IllegalArgumentException("limits must be positive");
        }
        public static Limits defaults() { return new Limits(4_000_000, 100_000, 512, 1_000_000); }
    }

    /** An immutable logical range. Each doubled quote maps to one output quote. */
    public static final class Cell implements CharSequence {
        private final CharSequence source;
        private final int start;
        private final int end;
        private final int[] escapes; // decoded positions of collapsed quote pairs

        private Cell(CharSequence source, int start, int end, int[] escapes) {
            this.source = Objects.requireNonNull(source, "source");
            Objects.checkFromToIndex(start, end, source.length());
            this.start = start;
            this.end = end;
            this.escapes = escapes;
        }
        @Override public int length() { return end - start - escapes.length; }
        @Override public char charAt(int index) {
            Objects.checkIndex(index, length());
            int low = 0, high = escapes.length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (escapes[middle] < index) low = middle + 1;
                else high = middle;
            }
            return source.charAt(start + index + low);
        }
        @Override public CharSequence subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length());
            return new Cell(this, from, to, new int[0]);
        }
        @Override public String toString() {
            StringBuilder result = new StringBuilder(length());
            for (int i = 0; i < length(); i++) result.append(charAt(i));
            return result.toString();
        }
        /** Exact, allocation-free comparison with caller text. */
        public boolean contentEquals(CharSequence other) {
            if (other == null || length() != other.length()) return false;
            for (int i = 0; i < length(); i++) if (charAt(i) != other.charAt(i)) return false;
            return true;
        }
    }

    public record Row(List<Cell> cells) {
        public Row { cells = List.copyOf(cells); }
    }
    public record Table(List<Row> rows) {
        public Table { rows = List.copyOf(rows); }
    }

    public static Table parse(CharSequence input, char delimiter, Limits limits) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(limits, "limits");
        delimiter(delimiter);
        int length = input.length();
        if (length > limits.maxInputUnits()) throw new IllegalArgumentException("input budget");
        List<Row> rows = new ArrayList<>();
        int at = 0;
        while (at < length) {
            List<Cell> cells = new ArrayList<>();
            while (true) {
                if (cells.size() == limits.maxColumns()) throw new IllegalArgumentException("column budget");
                int start = at;
                Cell cell;
                if (at < length && input.charAt(at) == '"') {
                    int rawStart = ++at;
                    int[] offsets = new int[4];
                    int used = 0;
                    boolean closed = false;
                    while (at < length) {
                        if (input.charAt(at) != '"') { at++; continue; }
                        if (at + 1 < length && input.charAt(at + 1) == '"') {
                            if (used == offsets.length) offsets = Arrays.copyOf(offsets, offsets.length * 2);
                            offsets[used] = at - rawStart - used;
                            used++;
                            at += 2;
                        } else { closed = true; break; }
                    }
                    if (!closed) throw new IllegalArgumentException("unterminated quoted field");
                    int rawEnd = at++;
                    cell = new Cell(input, rawStart, rawEnd, Arrays.copyOf(offsets, used));
                    if (at < length && input.charAt(at) != delimiter
                            && input.charAt(at) != '\r' && input.charAt(at) != '\n')
                        throw new IllegalArgumentException("characters after closing quote");
                } else {
                    while (at < length && input.charAt(at) != delimiter
                            && input.charAt(at) != '\r' && input.charAt(at) != '\n') {
                        if (input.charAt(at) == '"') throw new IllegalArgumentException("unquoted quote");
                        at++;
                    }
                    cell = new Cell(input, start, at, new int[0]);
                }
                if (cell.length() > limits.maxFieldUnits()) throw new IllegalArgumentException("field budget");
                cells.add(cell);
                if (at < length && input.charAt(at) == delimiter) { at++; continue; }
                if (rows.size() == limits.maxRows()) throw new IllegalArgumentException("row budget");
                rows.add(new Row(cells));
                if (at < length && input.charAt(at) == '\r') {
                    at++;
                    if (at < length && input.charAt(at) == '\n') at++;
                } else if (at < length && input.charAt(at) == '\n') at++;
                break;
            }
        }
        return new Table(rows);
    }

    /** Zero-based column projection, retaining the same immutable field views. */
    public static Table select(Table table, int... columns) {
        Objects.requireNonNull(table, "table");
        int[] selection = Objects.requireNonNull(columns, "columns").clone();
        for (int index : selection) if (index < 0) throw new IllegalArgumentException("negative column");
        List<Row> result = new ArrayList<>(table.rows().size());
        for (Row row : table.rows()) {
            List<Cell> picked = new ArrayList<>(selection.length);
            for (int index : selection) {
                if (index >= row.cells().size()) throw new IllegalArgumentException("missing column");
                picked.add(row.cells().get(index));
            }
            result.add(new Row(picked));
        }
        return new Table(result);
    }

    /** Regex finds against the CharSequence range; no complete row/string flatten. */
    public static Table whereMatches(Table table, int column, Pattern pattern) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(pattern, "pattern");
        if (column < 0) throw new IllegalArgumentException("negative column");
        List<Row> matched = new ArrayList<>();
        for (Row row : table.rows()) {
            if (column >= row.cells().size()) throw new IllegalArgumentException("missing column");
            if (pattern.matcher(row.cells().get(column)).find()) matched.add(row);
        }
        return new Table(matched);
    }

    /** Explicit materialization boundary, with RFC quote escaping and output budget. */
    public static String serialize(Table table, char delimiter, String newline, int maxOutputUnits) {
        Objects.requireNonNull(table, "table");
        delimiter(delimiter);
        if (!"\n".equals(newline) && !"\r\n".equals(newline))
            throw new IllegalArgumentException("newline must be LF or CRLF");
        if (maxOutputUnits < 1) throw new IllegalArgumentException("output budget");
        StringBuilder output = new StringBuilder();
        for (Row row : table.rows()) {
            for (int c = 0; c < row.cells().size(); c++) {
                if (c != 0) append(output, delimiter, maxOutputUnits);
                Cell cell = row.cells().get(c);
                boolean quote = false;
                for (int i = 0; i < cell.length(); i++) {
                    char unit = cell.charAt(i);
                    if (unit == delimiter || unit == '"' || unit == '\r' || unit == '\n') {
                        quote = true; break;
                    }
                }
                if (quote) append(output, '"', maxOutputUnits);
                for (int i = 0; i < cell.length(); i++) {
                    char unit = cell.charAt(i);
                    append(output, unit, maxOutputUnits);
                    if (unit == '"') append(output, unit, maxOutputUnits);
                }
                if (quote) append(output, '"', maxOutputUnits);
            }
            for (int i = 0; i < newline.length(); i++) append(output, newline.charAt(i), maxOutputUnits);
        }
        return output.toString();
    }

    private static void delimiter(char value) {
        if (value == '"' || value == '\r' || value == '\n' || Character.isSurrogate(value))
            throw new IllegalArgumentException("invalid delimiter");
    }
    private static void append(StringBuilder output, char unit, int maximum) {
        if (output.length() == maximum) throw new IllegalArgumentException("output budget");
        output.append(unit);
    }

    /** CLI: select specified zero-based columns from stdin; use --tsv for tab separation. */
    public static void main(String[] args) throws IOException {
        if (args.length == 1 && "--self-test".equals(args[0])) {
            selfTest();
            System.out.println("M3_JMOD_TEST_PASS text.csv");
            return;
        }
        char delimiter = ',';
        int[] columns = null;
        for (String option : args) {
            if ("--tsv".equals(option)) delimiter = '\t';
            else if (option.startsWith("--select=")) {
                String spec = option.substring("--select=".length());
                columns = Arrays.stream(spec.split(",", -1)).mapToInt(Integer::parseInt).toArray();
            } else throw new IllegalArgumentException("usage: [--tsv] [--select=0,2]");
        }
        byte[] input = System.in.readNBytes(4_000_001);
        if (input.length > 4_000_000) throw new IllegalArgumentException("stdin budget");
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(input)).toString();
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("stdin is not UTF-8", malformed);
        }
        Table table = parse(text, delimiter, Limits.defaults());
        if (columns != null) table = select(table, columns);
        System.out.print(serialize(table, delimiter, "\n", 4_000_000));
    }

    private static void selfTest() {
        Limits limits = new Limits(200, 10, 6, 80);
        String input = "id,\"a,b\",\"line1\r\nline2\",\"a\"\"b\"\r\n2,z,,\r\n";
        Table table = parse(input, ',', limits);
        require(table.rows().size() == 2, "row count");
        require(table.rows().get(0).cells().get(1).contentEquals("a,b"), "quoted comma");
        require(table.rows().get(0).cells().get(2).contentEquals("line1\r\nline2"), "embedded CRLF");
        require(table.rows().get(0).cells().get(3).contentEquals("a\"b"), "doubled quote");
        require(table.rows().get(0).cells().get(3).charAt(2) == 'b', "escaped-position map");
        require(table.rows().get(1).cells().size() == 4, "trailing empty cell");
        Table again = parse(serialize(table, ',', "\n", 200), ',', limits);
        require(again.rows().get(0).cells().get(3).contentEquals("a\"b"), "roundtrip");
        require(select(table, 1, 0).rows().get(0).cells().get(0) == table.rows().get(0).cells().get(1), "shared view");
        require(whereMatches(table, 1, Pattern.compile("^z$")).rows().size() == 1, "regex field");
        require(parse("", ',', limits).rows().isEmpty(), "empty input");
        require(parse("\n", ',', limits).rows().get(0).cells().get(0).length() == 0, "empty row");
        CharSequence composite = new CharSequence() {
            private final String left = "\uD83D", right = "\uDE00,\"x\"\"y\"\n";
            @Override public int length() { return left.length() + right.length(); }
            @Override public char charAt(int index) {
                Objects.checkIndex(index, length());
                return index < left.length() ? left.charAt(index) : right.charAt(index - left.length());
            }
            @Override public CharSequence subSequence(int start, int end) {
                throw new AssertionError("whole text must not be sliced or materialized");
            }
            @Override public String toString() { throw new AssertionError("no flatten"); }
        };
        Table view = parse(composite, ',', limits);
        require(view.rows().get(0).cells().get(0).contentEquals("\uD83D\uDE00"), "cross-atom surrogate");
        require(view.rows().get(0).cells().get(1).contentEquals("x\"y"), "composite quote");
        require(view.rows().get(0).cells().get(1).subSequence(1, 2).toString().equals("\""), "subsequence");
        refuse(() -> parse("a,\"unterminated", ',', limits));
        refuse(() -> parse("a,\"b\"oops", ',', limits));
        refuse(() -> parse("a,b\"c", ',', limits));
        refuse(() -> parse("a,b", ',', new Limits(2, 1, 1, 1)));
        refuse(() -> serialize(table, ',', "\n", 3));
        refuse(() -> select(table, 500));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void refuse(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("expected refusal");
    }

    /**
     * Prepare packed CSV/TSV metadata over one immutable String/M3 text owner.
     * The original parse API is unchanged. Raw UTF-16, quoted newlines, doubled
     * quotes and ragged rows have the same content semantics as that parser.
     * No source toString/subSequence or spelling copy. The owner must remain
     * immutable during both passes and throughout the result's lifetime.
     *
     * Aggregate cell and escaped-quote limits are in 0..1,000,000; input is also
     * bounded to 64 Mi UTF-16 units. Limits applies to decoded field lengths.
     * All syntax/count admission precedes descriptor allocation. These are
     * explicit logical budgets, not a JVM heap or elapsed-time guarantee.
     */
    public static IndexedTable index(CharSequence input, char delimiter, Limits limits,
                                     int maxCells, int maxEscapes) {
        Objects.requireNonNull(input, "input"); Objects.requireNonNull(limits, "limits");
        delimiter(delimiter);
        indexBudget(maxCells); indexBudget(maxEscapes);
        int length = input.length();
        if (length < 0 || length > limits.maxInputUnits() || length > 64 * 1024 * 1024)
            throw new IllegalArgumentException("indexed input budget");
        IndexCounts count = scanIndex(input, length, delimiter, limits, maxCells, maxEscapes, null, null);
        CellStore store = new CellStore(input, count.cells(), count.escapes());
        int[] rows = new int[count.rows() + 1];
        IndexCounts filled = scanIndex(input, length, delimiter, limits, maxCells, maxEscapes, store, rows);
        if (!count.equals(filled)) throw new IllegalStateException("input changed during indexing");
        return new IndexedTable(store, rows, null);
    }

    private static final int INDEX_LIMIT = 1_000_000;
    private static final int[] INDEX_NO_ESCAPES = new int[0];
    private static void indexBudget(int value) {
        if (value < 0 || value > INDEX_LIMIT) throw new IllegalArgumentException("indexed aggregate budget");
    }
    private record IndexCounts(int rows, int cells, int escapes) { }

    /** One lexical owner for counting and filling; never an object per field. */
    private static IndexCounts scanIndex(CharSequence input, int length, char delimiter, Limits limits,
                                        int maxCells, int maxEscapes, CellStore store, int[] rows) {
        int at = 0, row = 0, cell = 0, escaped = 0;
        while (at < length) {
            int columns = 0;
            while (true) {
                if (columns == limits.maxColumns()) throw new IllegalArgumentException("column budget");
                if (cell == maxCells) throw new IllegalArgumentException("cell budget");
                int start = at, end, escapeStart = escaped;
                if (at < length && input.charAt(at) == '"') {
                    start = ++at;
                    boolean closed = false;
                    while (at < length) {
                        if (input.charAt(at) != '"') { at++; continue; }
                        if (at + 1 < length && input.charAt(at + 1) == '"') {
                            if (escaped == maxEscapes) throw new IllegalArgumentException("escape budget");
                            if (store != null) {
                                if (escaped >= store.escapes.length) throw new IllegalStateException("input changed");
                                store.escapes[escaped] = at - start - (escaped - escapeStart);
                            }
                            escaped++; at += 2;
                        } else { closed = true; break; }
                    }
                    if (!closed) throw new IllegalArgumentException("unterminated quoted field");
                    end = at++;
                    if (at < length && input.charAt(at) != delimiter
                            && input.charAt(at) != '\r' && input.charAt(at) != '\n')
                        throw new IllegalArgumentException("characters after closing quote");
                } else {
                    while (at < length && input.charAt(at) != delimiter
                            && input.charAt(at) != '\r' && input.charAt(at) != '\n') {
                        if (input.charAt(at) == '"') throw new IllegalArgumentException("unquoted quote");
                        at++;
                    }
                    end = at;
                }
                if (end - start - (escaped - escapeStart) > limits.maxFieldUnits())
                    throw new IllegalArgumentException("field budget");
                if (store != null) {
                    if (cell >= store.starts.length) throw new IllegalStateException("input changed");
                    store.starts[cell] = start; store.ends[cell] = end;
                    store.escapeOffsets[cell] = escapeStart;
                }
                cell++; columns++;
                if (at < length && input.charAt(at) == delimiter) { at++; continue; }
                if (row == limits.maxRows()) throw new IllegalArgumentException("row budget");
                row++;
                if (rows != null) {
                    if (row >= rows.length) throw new IllegalStateException("input changed");
                    rows[row] = cell;
                }
                if (at < length && input.charAt(at) == '\r') {
                    at++; if (at < length && input.charAt(at) == '\n') at++;
                } else if (at < length && input.charAt(at) == '\n') at++;
                break;
            }
        }
        if (store != null) store.escapeOffsets[cell] = escaped;
        return new IndexCounts(row, cell, escaped);
    }

    /** Original owner and primitive lanes, shared by all coordinate projections. */
    private static final class CellStore {
        private final CharSequence source;
        private final int[] starts, ends, escapeOffsets, escapes;
        CellStore(CharSequence source, int cells, int escaped) {
            this.source = source; starts = new int[cells]; ends = new int[cells];
            escapeOffsets = new int[cells + 1]; escapes = new int[escaped];
        }
        int length(int cell) { return ends[cell] - starts[cell] - escapeOffsets[cell + 1] + escapeOffsets[cell]; }
        char unit(int cell, int index) {
            int first = escapeOffsets[cell], low = first, high = escapeOffsets[cell + 1];
            while (low < high) {
                int mid = low + ((high - low) >>> 1);
                if (escapes[mid] < index) low = mid + 1; else high = mid;
            }
            return source.charAt(starts[cell] + index + low - first);
        }
    }

    /**
     * Immutable packed table. Row/column projections share source and escape lanes;
     * they retain the complete original store, even when only one cell is selected.
     * No Cell/Row/String is stored per cell. Requested cell/column views are caller
     * facades; materialize() deliberately creates the old compatibility objects.
     */
    public static final class IndexedTable {
        private final CellStore store;
        private final int[] rows;
        private final int[] cellIds; // null is identity; projections compose directly to the original store
        private IndexedTable(CellStore store, int[] rows, int[] cellIds) {
            this.store = store; this.rows = rows; this.cellIds = cellIds;
        }
        public int rowCount() { return rows.length - 1; }
        public int cellCount() { return rows[rows.length - 1]; }
        public int columnCount(int row) {
            Objects.checkIndex(row, rowCount()); return rows[row + 1] - rows[row];
        }
        public int[] rowOffsets() { return rows.clone(); }
        /** Primitive arrays reachable from this table, excluding headers and source payload. */
        public long descriptorBytes() {
            return 4L * ((long) rows.length + store.starts.length + store.ends.length
                    + store.escapeOffsets.length + store.escapes.length
                    + (cellIds == null ? 0 : cellIds.length));
        }
        public int sourceStart(int row, int column) { return store.starts[id(row, column)]; }
        public int sourceEnd(int row, int column) { return store.ends[id(row, column)]; }
        public int decodedUnits(int row, int column) { return store.length(id(row, column)); }
        public int escapeCount(int row, int column) {
            int c = id(row, column); return store.escapeOffsets[c + 1] - store.escapeOffsets[c];
        }
        public CharSequence cellView(int row, int column) {
            int c = id(row, column); return new IndexedCell(store, c, 0, store.length(c));
        }
        /** Full ragged-column admission before creating any caller-owned cell facades. */
        public CharSequence[] columnViews(int column) {
            admitColumn(column);
            CharSequence[] result = new CharSequence[rowCount()];
            for (int row = 0; row < result.length; row++) result[row] = cellView(row, column);
            return result;
        }
        public IndexedTable sliceRows(int from, int to) {
            Objects.checkFromToIndex(from, to, rowCount());
            if (from == 0 && to == rowCount()) return this;
            int[] order = new int[to - from];
            for (int i = 0; i < order.length; i++) order[i] = from + i;
            return selectRows(order);
        }
        public IndexedTable head(int count) {
            if (count < 0) throw new IllegalArgumentException("negative count");
            return sliceRows(0, Math.min(rowCount(), count));
        }
        public IndexedTable tail(int count) {
            if (count < 0) throw new IllegalArgumentException("negative count");
            return sliceRows(rowCount() - Math.min(rowCount(), count), rowCount());
        }
        public IndexedTable reverse() {
            int[] order = new int[rowCount()];
            for (int i = 0; i < order.length; i++) order[i] = order.length - i - 1;
            return selectRows(order);
        }
        /** Ordered multiset selection; no source-character reads and no metadata chain. */
        public IndexedTable selectRows(int[] selection) {
            Objects.requireNonNull(selection, "selection"); indexBudget(selection.length);
            int[] order = selection.clone();
            long cells = 0;
            for (int row : order) cells += columnCount(row);
            if (cells > INDEX_LIMIT) throw new IllegalArgumentException("projected cell budget");
            int[] offsets = new int[order.length + 1], map = new int[(int) cells];
            int at = 0;
            for (int row = 0; row < order.length; row++) {
                for (int c = rows[order[row]]; c < rows[order[row] + 1]; c++) map[at++] = mapped(c);
                offsets[row + 1] = at;
            }
            return new IndexedTable(store, offsets, map);
        }
        public IndexedTable selectMask(long[] mask) {
            Objects.requireNonNull(mask, "mask");
            if (mask.length != (rowCount() + 63) / 64) throw new IllegalArgumentException("mask length");
            if ((rowCount() & 63) != 0 && mask[mask.length - 1] >>> (rowCount() & 63) != 0)
                throw new IllegalArgumentException("mask outside table");
            int count = 0; for (long bits : mask) count += Long.bitCount(bits);
            int[] selection = new int[count];
            for (int row = 0, at = 0; row < rowCount(); row++)
                if ((mask[row >>> 6] & 1L << (row & 63)) != 0) selection[at++] = row;
            return selectRows(selection);
        }
        /** Repeated columns allowed; empty selection retains zero-column rows. */
        public IndexedTable selectColumns(int... columns) {
            Objects.requireNonNull(columns, "columns"); indexBudget(columns.length);
            int[] selection = columns.clone();
            for (int column : selection) admitColumn(column);
            long cells = (long) rowCount() * selection.length;
            if (cells > INDEX_LIMIT) throw new IllegalArgumentException("projected cell budget");
            int[] offsets = new int[rowCount() + 1], map = new int[(int) cells];
            int at = 0;
            for (int row = 0; row < rowCount(); row++) {
                for (int column : selection) map[at++] = id(row, column);
                offsets[row + 1] = at;
            }
            return new IndexedTable(store, offsets, map);
        }
        /** Every row admitted first, then exactly one predicate invocation per row in order. */
        public IndexedTable where(int column, java.util.function.Predicate<? super CharSequence> predicate) {
            Objects.requireNonNull(predicate, "predicate"); admitColumn(column);
            int[] selection = new int[rowCount()]; int used = 0;
            for (int row = 0; row < rowCount(); row++)
                if (predicate.test(cellView(row, column))) selection[used++] = row;
            return selectRows(Arrays.copyOf(selection, used));
        }
        /** JDK regex find over decoded views; no regex time-bound guarantee. */
        public IndexedTable whereMatches(int column, Pattern pattern) {
            Objects.requireNonNull(pattern, "pattern");
            return where(column, value -> pattern.matcher(value).find());
        }
        /** Explicit projection to the original API; field spelling remains owner-backed. */
        public Table materialize() {
            List<Row> result = new ArrayList<>(rowCount());
            for (int row = 0; row < rowCount(); row++) {
                List<Cell> fields = new ArrayList<>(columnCount(row));
                for (int col = 0; col < columnCount(row); col++) {
                    CharSequence view = cellView(row, col);
                    fields.add(new Cell(view, 0, view.length(), INDEX_NO_ESCAPES));
                }
                result.add(new Row(fields));
            }
            return new Table(result);
        }
        /** Exact canonical output length in UTF-16 units, not UTF-8 bytes. */
        public long serializedUnits(char delimiter, String newline) {
            outputDialect(delimiter, newline);
            long result = 0;
            for (int row = 0; row < rowCount(); row++) {
                int columns = columnCount(row);
                result = Math.addExact(result, (long) Math.max(0, columns - 1) + newline.length());
                for (int col = 0; col < columns; col++) {
                    int c = id(row, col), n = store.length(c); boolean quote = false;
                    long units = n;
                    for (int i = 0; i < n; i++) {
                        char unit = store.unit(c, i);
                        quote |= unit == delimiter || unit == '"' || unit == '\r' || unit == '\n';
                        if (unit == '"') units++;
                    }
                    result = Math.addExact(result, units + (quote ? 2 : 0));
                }
            }
            return result;
        }
        /** Explicit spelling materialization, after exact budget admission. */
        public String serialize(char delimiter, String newline, int maxOutputUnits) {
            long units = admittedOutput(delimiter, newline, maxOutputUnits);
            StringBuilder result = new StringBuilder((int) units);
            try { emit(result, delimiter, newline); }
            catch (IOException impossible) { throw new AssertionError(impossible); }
            return result.toString();
        }
        /**
         * Count before the first sink mutation. Does not close or flush the sink.
         * A sink IOException propagates; an emitted prefix is not rolled back.
         */
        public void writeTo(Appendable sink, char delimiter, String newline, long maxOutputUnits) throws IOException {
            Objects.requireNonNull(sink, "sink"); admittedOutput(delimiter, newline, maxOutputUnits);
            emit(sink, delimiter, newline);
        }
        private long admittedOutput(char delimiter, String newline, long maxOutputUnits) {
            if (maxOutputUnits < 1) throw new IllegalArgumentException("output budget");
            long units = serializedUnits(delimiter, newline);
            if (units > maxOutputUnits) throw new IllegalArgumentException("output budget");
            return units;
        }
        private static void outputDialect(char delimiter, String newline) {
            CsvTools.delimiter(delimiter);
            if (!"\n".equals(newline) && !"\r\n".equals(newline))
                throw new IllegalArgumentException("newline must be LF or CRLF");
        }
        private void emit(Appendable sink, char delimiter, String newline) throws IOException {
            for (int row = 0; row < rowCount(); row++) {
                for (int col = 0; col < columnCount(row); col++) {
                    if (col != 0) sink.append(delimiter);
                    int c = id(row, col), length = store.length(c); boolean quote = false;
                    for (int i = 0; i < length; i++) {
                        char unit = store.unit(c, i);
                        if (unit == delimiter || unit == '"' || unit == '\r' || unit == '\n') { quote = true; break; }
                    }
                    if (quote) sink.append('"');
                    for (int i = 0; i < length; i++) {
                        char unit = store.unit(c, i); sink.append(unit);
                        if (unit == '"') sink.append('"');
                    }
                    if (quote) sink.append('"');
                }
                sink.append(newline);
            }
        }
        private int mapped(int cell) { return cellIds == null ? cell : cellIds[cell]; }
        private int id(int row, int column) {
            Objects.checkIndex(column, columnCount(row)); return mapped(rows[row] + column);
        }
        private void admitColumn(int column) {
            if (column < 0) throw new IllegalArgumentException("negative column");
            for (int row = 0; row < rowCount(); row++)
                if (column >= columnCount(row)) throw new IllegalArgumentException("missing column");
        }
    }

    /** Request-scoped decoded range; no per-cell object is retained by IndexedTable. */
    private static final class IndexedCell implements CharSequence {
        private final CellStore store;
        private final int cell, from, length;
        IndexedCell(CellStore store, int cell, int from, int length) {
            this.store = store; this.cell = cell; this.from = from; this.length = length;
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) { return store.unit(cell, from + Objects.checkIndex(index, length)); }
        @Override public CharSequence subSequence(int start, int end) {
            Objects.checkFromToIndex(start, end, length);
            return start == 0 && end == length ? this : new IndexedCell(store, cell, from + start, end - start);
        }
        @Override public String toString() {
            StringBuilder text = new StringBuilder(length);
            for (int i = 0; i < length; i++) text.append(store.unit(cell, from + i));
            return text.toString();
        }
    }
}
