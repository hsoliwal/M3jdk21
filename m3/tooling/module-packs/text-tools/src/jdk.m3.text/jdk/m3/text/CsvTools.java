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
}
