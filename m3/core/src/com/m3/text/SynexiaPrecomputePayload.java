/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The typed M3JDK boundary for Synexia's canonical {@code precompute_payload}.
 *
 * <p>The exporter owns the field map and source meaning. This class owns only
 * the lossless, bounded JSON value model needed by the JDK receiver. It never
 * turns an image coordinate into a source identity and it accepts no nested,
 * string, null, or non-finite values because the admitted Synexia field map is
 * limited to boolean, double, int, long, int[] and long[]. Unknown field names
 * remain readable with their JSON value kind, which keeps the receiver
 * forward-compatible without silently changing a known field's type.</p>
 */
public final class SynexiaPrecomputePayload {
    public enum Kind { BOOLEAN, INTEGER, DECIMAL, STRING, INTEGER_ARRAY }

    /** One immutable scalar or integer-array payload field. */
    public record Field(Kind kind, boolean booleanValue, long integerValue,
                        double decimalValue, String stringValue, long[] integerArrayValue) {
        public Field {
            Objects.requireNonNull(kind, "kind");
            if (kind == Kind.STRING) {
                Objects.requireNonNull(stringValue, "stringValue");
                if (integerArrayValue != null)
                    throw new IllegalArgumentException("array value on string field");
            } else if (kind == Kind.INTEGER_ARRAY) {
                Objects.requireNonNull(integerArrayValue, "integerArrayValue");
                integerArrayValue = integerArrayValue.clone();
            } else if (integerArrayValue != null) {
                throw new IllegalArgumentException("array value on scalar field");
            }
            if (kind != Kind.STRING && stringValue != null)
                throw new IllegalArgumentException("string value on non-string field");
            if (kind != Kind.DECIMAL && decimalValue != 0.0d)
                throw new IllegalArgumentException("decimal value on non-decimal field");
            if (kind != Kind.INTEGER && kind != Kind.INTEGER_ARRAY && integerValue != 0L)
                throw new IllegalArgumentException("integer value on non-integer field");
        }

        public long[] integerArrayValue() {
            return integerArrayValue == null ? null : integerArrayValue.clone();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Field value)) return false;
            return kind == value.kind && booleanValue == value.booleanValue
                    && integerValue == value.integerValue
                    && Double.doubleToLongBits(decimalValue) == Double.doubleToLongBits(value.decimalValue)
                    && Objects.equals(stringValue, value.stringValue)
                    && Arrays.equals(integerArrayValue, value.integerArrayValue);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(kind, booleanValue, integerValue, decimalValue, stringValue);
            return 31 * result + Arrays.hashCode(integerArrayValue);
        }
    }

    private enum Expected { BOOLEAN, DOUBLE, INT, LONG, INT_ARRAY, LONG_ARRAY }

    private static final Map<String, Expected> FIELD_TYPES = fieldTypes();
    private final Map<String, Field> fields;

    private SynexiaPrecomputePayload(Map<String, Field> fields) {
        this.fields = Collections.unmodifiableMap(new TreeMap<>(fields));
    }

    /** Parse the canonical JSON object retained by Synexia's records sidecar. */
    public static SynexiaPrecomputePayload parse(String json) {
        Objects.requireNonNull(json, "json");
        Parser parser = new Parser(json);
        Map<String, Field> fields = parser.object();
        parser.skipWhitespace();
        if (!parser.atEnd()) throw parser.error("trailing JSON");
        return new SynexiaPrecomputePayload(fields);
    }

    public Set<String> fieldNames() { return fields.keySet(); }
    public Map<String, Field> fields() { return fields; }
    public Optional<Field> field(String name) { return Optional.ofNullable(fields.get(Objects.requireNonNull(name))); }

    public int requireInt(String name) {
        Field field = require(name, Kind.INTEGER);
        if (field.integerValue() < Integer.MIN_VALUE || field.integerValue() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("field is outside int range: " + name);
        return (int) field.integerValue();
    }

    public long requireLong(String name) { return require(name, Kind.INTEGER).integerValue(); }
    public double requireDouble(String name) {
        Field field = Objects.requireNonNull(fields.get(name), "missing field: " + name);
        if (field.kind() != Kind.INTEGER && field.kind() != Kind.DECIMAL)
            throw new IllegalArgumentException("field is not numeric: " + name);
        return field.kind() == Kind.INTEGER ? field.integerValue() : field.decimalValue();
    }
    public boolean requireBoolean(String name) { return require(name, Kind.BOOLEAN).booleanValue(); }
    public String requireString(String name) { return require(name, Kind.STRING).stringValue(); }

    public int[] requireIntArray(String name) {
        long[] values = require(name, Kind.INTEGER_ARRAY).integerArrayValue();
        int[] result = new int[values.length];
        for (int at = 0; at < values.length; at++) {
            if (values[at] < Integer.MIN_VALUE || values[at] > Integer.MAX_VALUE)
                throw new IllegalArgumentException("array element is outside int range: " + name);
            result[at] = (int) values[at];
        }
        return result;
    }

    public long[] requireLongArray(String name) {
        return require(name, Kind.INTEGER_ARRAY).integerArrayValue();
    }

    /** Deterministic JSON useful for exact round-trip receipts and parity tests. */
    public String canonicalJson() {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            if (!first) out.append(',');
            first = false;
            appendString(out, entry.getKey()).append(':');
            appendValue(out, entry.getValue());
        }
        return out.append('}').toString();
    }

    private Field require(String name, Kind kind) {
        Objects.requireNonNull(name, "name");
        Field field = fields.get(name);
        if (field == null) throw new IllegalArgumentException("missing field: " + name);
        if (field.kind() != kind) throw new IllegalArgumentException("field has kind " + field.kind() + ": " + name);
        return field;
    }

    private static void appendValue(StringBuilder out, Field field) {
        switch (field.kind()) {
            case BOOLEAN -> out.append(field.booleanValue());
            case INTEGER -> out.append(field.integerValue());
            case DECIMAL -> out.append(Double.toString(field.decimalValue()));
            case STRING -> appendString(out, field.stringValue());
            case INTEGER_ARRAY -> {
                out.append('[');
                long[] values = field.integerArrayValue();
                for (int at = 0; at < values.length; at++) {
                    if (at != 0) out.append(',');
                    out.append(values[at]);
                }
                out.append(']');
            }
        }
    }

    private static StringBuilder appendString(StringBuilder out, String value) {
        out.append('"');
        for (int at = 0; at < value.length(); at++) {
            char current = value.charAt(at);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (current < 0x20) out.append(String.format("\\u%04x", (int) current));
                    else out.append(current);
                }
            }
        }
        return out.append('"');
    }

    private static Map<String, Expected> fieldTypes() {
        Map<String, Expected> result = new LinkedHashMap<>();
        for (String name : List.of("language_id", "word_count", "flags", "document_frequency",
                "pos_mask", "morphology_mask", "lexical_rank", "utf16_length", "code_point_length",
                "first_code_point", "last_code_point", "script_ordinal", "frequency_rank",
                "langdex_flags", "langdex_subject_id", "langdex_feature_bits", "langdex_evidence_mask",
                "langdex_confidence_permille", "si_decimal_exponent", "min_value", "max_value",
                "precomputed_value_count")) result.put(name, Expected.INT);
        for (String name : List.of("lexicon_fingerprint", "total_corpus_tokens", "total_documents",
                "corpus_count", "stem_id", "lemma_id", "phonetic_id", "presence64", "sim_hash64",
                "langdex_concept_id", "langdex_frequency", "langdex_lexical_class_mask",
                "langdex_semantic_class_mask", "langdex_target_lexeme_id", "si_dimension_packed"))
            result.put(name, Expected.LONG);
        result.put("si_offset", Expected.DOUBLE);
        result.put("si_prefixable", Expected.BOOLEAN);
        result.put("translation_grammar_supported", Expected.BOOLEAN);
        result.put("shared_utf16_storage", Expected.BOOLEAN);
        for (String name : List.of("canonical_decimal_spelling", "source_id", "record_id"))
            result.put(name, Expected.STRING);
        for (String name : List.of("memberships")) result.put(name, Expected.INT_ARRAY);
        for (String name : List.of("concept_ids", "subjects", "expansion_word_ids")) result.put(name, Expected.LONG_ARRAY);
        return Map.copyOf(result);
    }

    private static final class Parser {
        private final String input;
        private int at;

        private Parser(String input) { this.input = input; }
        private boolean atEnd() { return at == input.length(); }
        private void skipWhitespace() { while (!atEnd() && Character.isWhitespace(input.charAt(at))) at++; }
        private IllegalArgumentException error(String detail) { return new IllegalArgumentException(detail + " at " + at); }

        private Map<String, Field> object() {
            skipWhitespace(); expect('{');
            Map<String, Field> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) return result;
            while (true) {
                skipWhitespace();
                String name = string();
                if (!name.matches("[a-z][a-z0-9_]*")) throw error("invalid payload field name");
                if (result.containsKey(name)) throw error("duplicate payload field");
                skipWhitespace(); expect(':');
                result.put(name, value(name));
                skipWhitespace();
                if (consume('}')) return result;
                expect(',');
            }
        }

        private Field value(String name) {
            skipWhitespace();
            char current = peek();
            Field field;
            if (current == 't' || current == 'f') field = booleanValue();
            else if (current == '"') field = stringValue();
            else if (current == '[') field = array(name);
            else field = number(name);
            validate(name, field);
            return field;
        }

        private Field stringValue() {
            return new Field(Kind.STRING, false, 0L, 0.0d, string(), null);
        }

        private Field booleanValue() {
            if (input.startsWith("true", at)) { at += 4; return new Field(Kind.BOOLEAN, true, 0L, 0.0d, null, null); }
            if (input.startsWith("false", at)) { at += 5; return new Field(Kind.BOOLEAN, false, 0L, 0.0d, null, null); }
            throw error("invalid boolean");
        }

        private Field number(String name) {
            int begin = at;
            if (peek() == '-') at++;
            digits();
            boolean decimal = false;
            if (consume('.')) { decimal = true; digits(); }
            if (peekOr('\0') == 'e' || peekOr('\0') == 'E') {
                decimal = true; at++;
                if (peekOr('\0') == '+' || peekOr('\0') == '-') at++;
                digits();
            }
            String token = input.substring(begin, at);
            if (decimal) {
                try {
                    double value = Double.parseDouble(token);
                    if (!Double.isFinite(value)) throw error("non-finite number");
                    return new Field(Kind.DECIMAL, false, 0L, value, null, null);
                } catch (NumberFormatException failure) { throw error("invalid number"); }
            }
            try { return new Field(Kind.INTEGER, false, Long.parseLong(token), 0.0d, null, null); }
            catch (NumberFormatException failure) { throw error("integer outside long range"); }
        }

        private Field array(String name) {
            expect('['); skipWhitespace();
            List<Long> values = new ArrayList<>();
            if (!consume(']')) {
                while (true) {
                    Field value = number(name);
                    if (value.kind() != Kind.INTEGER) throw error("arrays must contain integers");
                    values.add(value.integerValue());
                    skipWhitespace();
                    if (consume(']')) break;
                    expect(','); skipWhitespace();
                }
            }
            long[] result = new long[values.size()];
            for (int index = 0; index < result.length; index++) result[index] = values.get(index);
            return new Field(Kind.INTEGER_ARRAY, false, 0L, 0.0d, null, result);
        }

        private void validate(String name, Field field) {
            Expected expected = FIELD_TYPES.get(name);
            if (expected == null) return;
            boolean valid = switch (expected) {
                case BOOLEAN -> field.kind() == Kind.BOOLEAN;
                case STRING -> field.kind() == Kind.STRING;
                case DOUBLE -> field.kind() == Kind.INTEGER || field.kind() == Kind.DECIMAL;
                case INT -> field.kind() == Kind.INTEGER && field.integerValue() >= Integer.MIN_VALUE
                        && field.integerValue() <= Integer.MAX_VALUE;
                case LONG -> field.kind() == Kind.INTEGER;
                case INT_ARRAY -> field.kind() == Kind.INTEGER_ARRAY && allInt(field.integerArrayValue());
                case LONG_ARRAY -> field.kind() == Kind.INTEGER_ARRAY;
            };
            if (!valid) throw error("payload type mismatch for " + name);
        }

        private static boolean allInt(long[] values) {
            for (long value : values) if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) return false;
            return true;
        }

        private String string() {
            expect('"'); StringBuilder result = new StringBuilder();
            while (!atEnd()) {
                char current = input.charAt(at++);
                if (current == '"') return result.toString();
                if (current != '\\') { result.append(current); continue; }
                if (atEnd()) throw error("unterminated escape");
                char escaped = input.charAt(at++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append((char) hex4());
                    default -> throw error("invalid escape");
                }
            }
            throw error("unterminated string");
        }

        private int hex4() {
            if (at + 4 > input.length()) throw error("short unicode escape");
            int value = 0;
            for (int count = 0; count < 4; count++) {
                int digit = Character.digit(input.charAt(at++), 16);
                if (digit < 0) throw error("invalid unicode escape");
                value = (value << 4) | digit;
            }
            return value;
        }

        private void digits() {
            int begin = at;
            while (!atEnd() && Character.isDigit(input.charAt(at))) at++;
            if (begin == at) throw error("digits required");
        }

        private char peek() { if (atEnd()) throw error("unexpected end"); return input.charAt(at); }
        private char peekOr(char fallback) { return atEnd() ? fallback : input.charAt(at); }
        private boolean consume(char expected) { if (!atEnd() && input.charAt(at) == expected) { at++; return true; } return false; }
        private void expect(char expected) { if (!consume(expected)) throw error("expected '" + expected + "'"); }
    }
}
