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
 * Java String remains the public facade; on M3JDK21 its backing is owned by M3 String.
 * These are deliberately scoped formats, not emulations of GNU or external CLI tools.
 */
public final class StructuredTextTools {
    private StructuredTextTools() { }

    /**
     * RFC 4180-style CSV/TSV field parsing, including embedded line breaks and doubled quotes.
     * Accepts CRLF, LF and CR record terminators; rejects stray quotes and unterminated fields.
     */
    public static List<List<String>> delimited(TextFrame input, char separator, Limits limits) {
        Objects.requireNonNull(limits, "limits").check(Objects.requireNonNull(input, "input"));
        checkSeparator(separator);
        String text = input.text();
        if (text.isEmpty()) return List.of();
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false, afterQuote = false, started = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"'); i++;
                    } else {
                        quoted = false; afterQuote = true;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }
            if (c == separator) {
                addField(fields, field, limits);
                afterQuote = false; started = false;
            } else if (c == '\r' || c == '\n') {
                addField(fields, field, limits);
                addRecord(records, fields, limits);
                fields = new ArrayList<>();
                afterQuote = false; started = false;
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
            } else if (c == '"' && !started && !afterQuote) {
                quoted = true; started = true;
            } else if (c == '"' || afterQuote) {
                throw new IllegalArgumentException("unexpected text after CSV quote at offset " + i);
            } else {
                field.append(c); started = true;
            }
        }
        if (quoted) throw new IllegalArgumentException("unterminated quoted field");
        // The last record is already committed if its terminator was consumed.
        char last = text.charAt(text.length() - 1);
        if (last != '\n' && last != '\r') {
            addField(fields, field, limits);
            addRecord(records, fields, limits);
        }
        return List.copyOf(records);
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
}

