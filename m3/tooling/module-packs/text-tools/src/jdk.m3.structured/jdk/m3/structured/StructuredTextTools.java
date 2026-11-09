/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded, deterministic structured-text atoms over the existing immutable TextFrame.
 * Java String remains the public facade; on M3JDK 21 its backing is owned by M3 String.
 * These are deliberately scoped formats, not emulations of GNU or external CLI tools.
 */
public final class StructuredTextTools {
    private StructuredTextTools() { }

    /**
     * RFC 4180-style CSV/TSV field parsing, including embedded line breaks and doubled quotes.
     * Accepts CRLF, LF and CR record terminators; rejects stray quotes and unterminated fields.
     */
    public static List<List<String>> delimited(TextFrame input, char separator, Limits limits) {
        return prepareDelimited(input, separator, limits, Long.MAX_VALUE).materialize();
    }

    /** Zero-based CSV/TSV column projection; outputs canonical LF rows, retaining exact field values. */
    public static TextFrame selectDelimited(TextFrame input, char separator,
                                            int[] columns, Limits limits) {
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(limits, "limits");
        if (columns.length == 0 || columns.length > limits.maxNodes()) {
            throw new IllegalArgumentException("invalid projection width");
        }
        int[] requested = columns.clone();
        for (int c : requested) if (c < 0) throw new IllegalArgumentException("negative column index");
        List<List<String>> rows = delimited(input, separator, limits);
        StringBuilder result = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < requested.length; i++) {
                int index = requested[i];
                if (index >= row.size()) throw new IllegalArgumentException("missing column " + index);
                if (i > 0) result.append(separator);
                appendEscaped(result, row.get(index), separator);
                if (result.length() > limits.maxBytes()) throw new IllegalArgumentException("output budget");
            }
            result.append('\n');
            if (result.length() > limits.maxBytes()) throw new IllegalArgumentException("output budget");
        }
        TextFrame projected = TextFrame.fromString(result.toString());
        limits.check(projected);
        return projected;
    }

    /**
     * A strict, case-sensitive INI subset: [section], key=value, and # or ; full-line comments.
     * No interpolation, multiline values, escaping or duplicate key overwrite.
     * Empty section name in the result represents top-level keys.
     */
    public static Map<String, Map<String, String>> ini(TextFrame input, Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        String section = "";
        sections.put(section, new LinkedHashMap<>());
        for (TextLine line : input.lines()) {
            String value = line.content().strip();
            if (value.isEmpty() || value.startsWith("#") || value.startsWith(";")) continue;
            if (value.startsWith("[") && value.endsWith("]")) {
                section = value.substring(1, value.length() - 1).strip();
                if (section.isEmpty()) throw new IllegalArgumentException("empty INI section");
                sections.computeIfAbsent(section, ignored -> new LinkedHashMap<>());
            } else {
                int split = value.indexOf('=');
                if (split <= 0) throw new IllegalArgumentException("invalid INI entry");
                String key = value.substring(0, split).strip();
                if (key.isEmpty() || key.indexOf('[') >= 0 || key.indexOf(']') >= 0) {
                    throw new IllegalArgumentException("invalid INI key");
                }
                String previous = sections.get(section).putIfAbsent(key, value.substring(split + 1).strip());
                if (previous != null) throw new IllegalArgumentException("duplicate INI key: " + key);
            }
            if (sections.size() > limits.maxNodes()) throw new IllegalArgumentException("INI section budget");
            if (sections.get(section).size() > limits.maxNodes()) throw new IllegalArgumentException("INI key budget");
        }
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        sections.forEach((name, fields) -> result.put(name,
                java.util.Collections.unmodifiableMap(new LinkedHashMap<>(fields))));
        return java.util.Collections.unmodifiableMap(result);
    }

    /**
     * Small .env subset, with optional export prefix, literal single/double quotes and # comments.
     * Values are intentionally not expanded or evaluated, and repeated keys are rejected.
     */
    public static Map<String, String> dotenv(TextFrame input, Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        Map<String, String> values = new LinkedHashMap<>();
        for (TextLine line : input.lines()) {
            String entry = line.content().strip();
            if (entry.isEmpty() || entry.startsWith("#")) continue;
            if (entry.startsWith("export ")) entry = entry.substring(7).stripLeading();
            int split = entry.indexOf('=');
            if (split < 1) throw new IllegalArgumentException("invalid .env assignment");
            String key = entry.substring(0, split).strip();
            if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalArgumentException("invalid .env key");
            String raw = entry.substring(split + 1).strip();
            String value;
            if (raw.startsWith("\"") || raw.startsWith("'")) {
                char quote = raw.charAt(0);
                if (raw.length() < 2 || raw.charAt(raw.length() - 1) != quote)
                    throw new IllegalArgumentException("unterminated .env quote");
                value = raw.substring(1, raw.length() - 1);
            } else {
                int comment = -1;
                for (int i = 1; i < raw.length(); i++) {
                    if (raw.charAt(i) == '#' && Character.isWhitespace(raw.charAt(i - 1))) {
                        comment = i; break;
                    }
                }
                value = (comment < 0 ? raw : raw.substring(0, comment)).stripTrailing();
            }
            if (values.putIfAbsent(key, value) != null) throw new IllegalArgumentException("duplicate .env key: " + key);
            if (values.size() > limits.maxNodes()) throw new IllegalArgumentException(".env key budget");
        }
        return java.util.Collections.unmodifiableMap(values);
    }

    private static void checkSeparator(char separator) {
        if (separator == '"' || separator == '\r' || separator == '\n' || separator == 0
                || Character.isSurrogate(separator)) {
            throw new IllegalArgumentException("invalid delimiter");
        }
    }

    private static void addField(List<String> fields, StringBuilder field, Limits limits) {
        if (fields.size() >= limits.maxNodes()) throw new IllegalArgumentException("column budget");
        fields.add(field.toString());
        field.setLength(0);
    }

    private static void addRecord(List<List<String>> records, List<String> fields, Limits limits) {
        if (records.size() >= limits.maxLines()) throw new IllegalArgumentException("record budget");
        records.add(List.copyOf(fields));
    }

    private static void appendEscaped(StringBuilder out, String value, char separator) {
        boolean quote = value.indexOf(separator) >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (quote) out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') out.append('"');
            out.append(c);
        }
        if (quote) out.append('"');
    }

    /** Explicit opt-in native verification; JAVA never loads a library. */
    public enum DelimitedBackend { JAVA, NATIVE_REQUIRED }

    private static volatile boolean delimitedNativeLoaded;

    /** Load the matching CSV predicate kernel, without modifying other native owners. */
    public static synchronized void loadDelimitedNative(java.nio.file.Path library) {
        Objects.requireNonNull(library, "library");
        if (!delimitedNativeLoaded) {
            System.load(library.toAbsolutePath().normalize().toString());
            if (nativeDelimitedAbi() != 20261104) {
                throw new UnsatisfiedLinkError("M3_DELIMITED_JNI_ABI_MISMATCH");
            }
            delimitedNativeLoaded = true;
        }
    }

    /**
     * Parse once into immutable UTF-16 field ranges. The index retains the one String
     * produced by TextFrame.text(), not the frame, individual values or row objects.
     * maxIndexBytes bounds retained primitive lanes, including the row sentinel;
     * temporary growth/copy space, source text and VM/JNI overhead are not included.
     * Existing delimited() uses this same scanner without a new index-budget refusal.
     */
    public static DelimitedIndex prepareDelimited(TextFrame input, char separator,
                                                  Limits limits, long maxIndexBytes) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        checkSeparator(separator);
        if (maxIndexBytes < 0) throw new IllegalArgumentException("negative index budget");
        String text = input.text();
        DelimitedRanges ranges = new DelimitedRanges(limits, maxIndexBytes);
        boolean quoted = false, afterQuote = false, started = false;
        int start = 0, closed = 0, decoded = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        decoded++; i++;
                    } else {
                        quoted = false; afterQuote = true; closed = i;
                    }
                } else {
                    decoded++;
                }
                continue;
            }
            if (c == separator) {
                ranges.field(start, afterQuote ? closed : i, decoded);
                afterQuote = false; started = false; start = i + 1; decoded = 0;
            } else if (c == '\r' || c == '\n') {
                ranges.field(start, afterQuote ? closed : i, decoded);
                ranges.record();
                afterQuote = false; started = false; decoded = 0;
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                start = i + 1;
            } else if (c == '"' && !started && !afterQuote) {
                quoted = true; started = true; start = i + 1;
            } else if (c == '"' || afterQuote) {
                throw new IllegalArgumentException("unexpected text after CSV quote at offset " + i);
            } else {
                decoded++; started = true;
            }
        }
        if (quoted) throw new IllegalArgumentException("unterminated quoted field");
        if (!text.isEmpty()) {
            char last = text.charAt(text.length() - 1);
            if (last != '\n' && last != '\r') {
                ranges.field(start, afterQuote ? closed : text.length(), decoded);
                ranges.record();
            }
        }
        return new DelimitedIndex(text, separator, ranges.finishFields(), ranges.finishRows());
    }

    /** Primitive source coordinates: [start,end,decodedLength] per field and row offsets. */
    public static final class DelimitedIndex {
        private final String source;
        private final char separator;
        private final int[] fields;
        private final int[] rows;

        private DelimitedIndex(String source, char separator, int[] fields, int[] rows) {
            this.source = source;
            this.separator = separator;
            this.fields = fields;
            this.rows = rows;
        }
        public int records() { return rows.length - 1; }
        public int fieldCount() { return fields.length / 3; }
        public int columns(int row) {
            Objects.checkIndex(row, records());
            return rows[row + 1] - rows[row];
        }
        public long primitivePayloadBytes() { return 4L * ((long) fields.length + rows.length); }
        public String value(int row, int column) {
            Objects.checkIndex(column, columns(row));
            return fieldValue(rows[row] + column);
        }
        private String fieldValue(int field) {
            int base = field * 3, start = fields[base], end = fields[base + 1];
            if (end - start == fields[base + 2]) return source.substring(start, end);
            StringBuilder out = new StringBuilder(fields[base + 2]);
            for (int at = start; at < end; at++) {
                char c = source.charAt(at);
                out.append(c);
                if (c == '"') at++;
            }
            return out.toString();
        }
        /** Fresh immutable lists/values on demand; no materialized results are cached. */
        public List<List<String>> materialize() {
            List<List<String>> result = new ArrayList<>(records());
            for (int row = 0; row < records(); row++) {
                List<String> values = new ArrayList<>(columns(row));
                for (int at = rows[row]; at < rows[row + 1]; at++) values.add(fieldValue(at));
                result.add(List.copyOf(values));
            }
            return List.copyOf(result);
        }
        /** Exact case-sensitive decoded-field comparison with no per-field String creation. */
        public int countEquals(int column, String expected) {
            return countEqualsUpTo(column, expected, Integer.MAX_VALUE, DelimitedBackend.JAVA);
        }
        /**
         * Return min(limit, exact count). All rows must contain column, even at limit 0.
         * Required JNI executes the same predicate once per table and checks Java parity.
         * Input parsing is not repeated; JNI still validates its raw coordinate lanes.
         */
        public int countEqualsUpTo(int column, String expected, int limit, DelimitedBackend backend) {
            Objects.requireNonNull(expected, "expected");
            Objects.requireNonNull(backend, "backend");
            if (column < 0) throw new IllegalArgumentException("negative column index");
            if (limit < 0) throw new IllegalArgumentException("negative count limit");
            for (int row = 0; row < records(); row++) {
                if (column >= rows[row + 1] - rows[row]) {
                    throw new IllegalArgumentException("missing column " + column);
                }
            }
            if (backend == DelimitedBackend.NATIVE_REQUIRED && !delimitedNativeLoaded) {
                throw new UnsatisfiedLinkError("M3_DELIMITED_JNI_NOT_LOADED");
            }
            int count = 0;
            for (int row = 0; row < records() && count < limit; row++) {
                if (fieldEquals(rows[row] + column, expected)) count++;
            }
            if (backend == DelimitedBackend.NATIVE_REQUIRED) {
                int actual = nativeCountDelimitedEquals(source, fields, rows, column, expected, limit);
                if (actual != count) throw new IllegalStateException("M3_DELIMITED_JNI_JAVA_MISMATCH");
            }
            return count;
        }
        private boolean fieldEquals(int field, String value) {
            int base = field * 3;
            if (fields[base + 2] != value.length()) return false;
            int unit = 0;
            for (int at = fields[base]; at < fields[base + 1]; at++) {
                char c = source.charAt(at);
                if (value.charAt(unit++) != c) return false;
                if (c == '"') at++;
            }
            return true;
        }
        /** Project admitted fields without materializing intermediate value/row lists. */
        public TextFrame project(int[] columns, Limits limits) {
            return projectSelected(-1, null, columns, limits);
        }
        /** Filter by exact decoded field equality, then project; no per-row value objects. */
        public TextFrame projectWhereEquals(int column, String expected, int[] columns, Limits limits) {
            Objects.requireNonNull(expected, "expected");
            if (column < 0) throw new IllegalArgumentException("negative column index");
            for (int row = 0; row < records(); row++) {
                if (column >= rows[row + 1] - rows[row]) {
                    throw new IllegalArgumentException("missing column " + column);
                }
            }
            return projectSelected(column, expected, columns, limits);
        }
        private TextFrame projectSelected(int filterColumn, String expected, int[] columns, Limits limits) {
            Objects.requireNonNull(columns, "columns");
            Objects.requireNonNull(limits, "limits");
            if (columns.length == 0 || columns.length > limits.maxNodes()) {
                throw new IllegalArgumentException("invalid projection width");
            }
            int[] requested = columns.clone();
            for (int c : requested) if (c < 0) throw new IllegalArgumentException("negative column index");
            StringBuilder out = new StringBuilder();
            for (int row = 0; row < records(); row++) {
                if (filterColumn >= 0 && !fieldEquals(rows[row] + filterColumn, expected)) continue;
                for (int i = 0; i < requested.length; i++) {
                    int column = requested[i];
                    if (column >= rows[row + 1] - rows[row]) {
                        throw new IllegalArgumentException("missing column " + column);
                    }
                    if (i > 0) out.append(separator);
                    int base = (rows[row] + column) * 3;
                    int start = fields[base], end = fields[base + 1];
                    boolean quote = end - start != fields[base + 2];
                    for (int at = start; !quote && at < end; at++) {
                        char c = source.charAt(at);
                        quote = c == separator || c == '\r' || c == '\n' || c == '"';
                    }
                    if (quote) out.append('"');
                    out.append(source, start, end);
                    if (quote) out.append('"');
                    if (out.length() > limits.maxBytes()) throw new IllegalArgumentException("output budget");
                }
                out.append('\n');
                if (out.length() > limits.maxBytes()) throw new IllegalArgumentException("output budget");
            }
            TextFrame result = TextFrame.fromString(out.toString());
            limits.check(result);
            return result;
        }
    }

    /** Private growable primitive lanes. No object is retained per field or record. */
    private static final class DelimitedRanges {
        private final Limits limits;
        private final long budget;
        private int[] fields = new int[0];
        private int[] rows = new int[1];
        private int fieldCount;
        private int rowCount;
        DelimitedRanges(Limits limits, long budget) {
            this.limits = limits;
            this.budget = budget;
            require(0, 1);
        }
        void field(int start, int end, int decoded) {
            if (fieldCount - rows[rowCount] >= limits.maxNodes()) {
                throw new IllegalArgumentException("column budget");
            }
            if (fieldCount >= (Integer.MAX_VALUE - 8) / 3) {
                throw new IllegalArgumentException("index array capacity");
            }
            int needed = (fieldCount + 1) * 3;
            if (needed > fields.length) {
                int capacity = capacity(fields.length, needed);
                if (4L * ((long) capacity + rows.length) > budget) capacity = needed;
                require(capacity, rows.length);
                fields = java.util.Arrays.copyOf(fields, capacity);
            }
            int base = fieldCount++ * 3;
            fields[base] = start; fields[base + 1] = end; fields[base + 2] = decoded;
        }
        void record() {
            if (rowCount >= limits.maxLines()) throw new IllegalArgumentException("record budget");
            if (rowCount >= Integer.MAX_VALUE - 9) throw new IllegalArgumentException("index array capacity");
            int needed = rowCount + 2;
            if (needed > rows.length) {
                int capacity = capacity(rows.length, needed);
                if (4L * ((long) fields.length + capacity) > budget) capacity = needed;
                require(fields.length, capacity);
                rows = java.util.Arrays.copyOf(rows, capacity);
            }
            rows[++rowCount] = fieldCount;
        }
        private static int capacity(int old, int needed) {
            return (int) Math.min(Integer.MAX_VALUE - 8L, Math.max(needed, old + old / 2L + 3L));
        }
        private void require(int fieldCells, int rowCells) {
            if (4L * ((long) fieldCells + rowCells) > budget) {
                throw new IllegalArgumentException("delimited index budget");
            }
        }
        int[] finishFields() { return java.util.Arrays.copyOf(fields, fieldCount * 3); }
        int[] finishRows() { return java.util.Arrays.copyOf(rows, rowCount + 1); }
    }

    private static native int nativeDelimitedAbi();
    private static native int nativeCountDelimitedEquals(String source, int[] fields,
            int[] rows, int column, String expected, int limit);
}

