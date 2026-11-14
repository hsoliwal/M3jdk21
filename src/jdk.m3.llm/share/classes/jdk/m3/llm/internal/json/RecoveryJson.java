// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Derivation: PINNED_JSON_HELPER_ATOM_EXTRACTION; see SOURCE_PROVENANCE.tsv.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.internal.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Small strict, bounded JSON data codec for the dependency-free recovery command. */
final class RecoveryJson {
    private final String source;
    private int position;
    private int values;
    private RecoveryJson(String source) { this.source = source; }

    static Object parse(String source) {
        JsonSupport.require(source != null && source.length() <= 16 * 1024 * 1024, "JSON_SIZE_LIMIT");
        RecoveryJson parser = new RecoveryJson(source);
        Object value = parser.value(0);
        parser.space();
        JsonSupport.require(parser.position == source.length(), "JSON_TRAILING_DATA");
        return value;
    }

    private Object value(int depth) {
        JsonSupport.require(depth <= 64 && ++values <= 500000, "JSON_COMPLEXITY_LIMIT");
        space();
        JsonSupport.require(position < source.length(), "JSON_TRUNCATED");
        char next = source.charAt(position);
        if (next == '"') return string();
        if (next == '{') {
            position++;
            Map<String, Object> map = new LinkedHashMap<>();
            space();
            if (take('}')) return map;
            do {
                space();
                String key = string();
                space();
                expect(':');
                JsonSupport.require(!map.containsKey(key), "JSON_DUPLICATE_KEY");
                map.put(key, value(depth + 1));
                space();
                if (take('}')) return map;
                expect(',');
            } while (true);
        }
        if (next == '[') {
            position++;
            List<Object> list = new ArrayList<>();
            space();
            if (take(']')) return list;
            do {
                list.add(value(depth + 1));
                space();
                if (take(']')) return list;
                expect(',');
            } while (true);
        }
        for (String literal : List.of("true", "false", "null")) {
            if (source.startsWith(literal, position)) {
                position += literal.length();
                return literal.equals("null") ? null : Boolean.valueOf(literal);
            }
        }
        int start = position;
        if (take('-')) JsonSupport.require(position < source.length(), "JSON_NUMBER");
        if (!take('0')) {
            JsonSupport.require(position < source.length() && source.charAt(position) >= '1'
                    && source.charAt(position) <= '9', "JSON_NUMBER");
            digits();
        }
        if (take('.')) {
            int mark = position;
            digits();
            JsonSupport.require(position > mark, "JSON_NUMBER");
        }
        if (take('e') || take('E')) {
            if (!take('+')) take('-');
            int mark = position;
            digits();
            JsonSupport.require(position > mark, "JSON_NUMBER");
        }
        JsonSupport.require(position - start <= 128, "JSON_NUMBER_LIMIT");
        try { return new BigDecimal(source.substring(start, position)); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException("JSON_NUMBER"); }
    }

    private String string() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (position < source.length()) {
            char c = source.charAt(position++);
            if (c == '"') {
                String text = result.toString();
                validUnicode(text);
                return text;
            }
            JsonSupport.require(c >= 0x20, "JSON_CONTROL_CHARACTER");
            if (c == '\\') {
                JsonSupport.require(position < source.length(), "JSON_TRUNCATED_ESCAPE");
                c = source.charAt(position++);
                switch (c) {
                    case '"', '\\', '/' -> result.append(c);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        JsonSupport.require(position + 4 <= source.length(), "JSON_UNICODE_ESCAPE");
                        int unit = 0;
                        for (int i = 0; i < 4; i++) {
                            char hex = source.charAt(position++);
                            int digit = hex <= 127 ? Character.digit(hex, 16) : -1;
                            JsonSupport.require(digit >= 0, "JSON_UNICODE_ESCAPE");
                            unit = unit * 16 + digit;
                        }
                        result.append((char) unit);
                    }
                    default -> throw new IllegalArgumentException("JSON_ESCAPE");
                }
            } else result.append(c);
        }
        throw new IllegalArgumentException("JSON_UNTERMINATED_STRING");
    }

    private void digits() {
        while (position < source.length() && source.charAt(position) >= '0'
                && source.charAt(position) <= '9') position++;
    }
    private void space() {
        while (position < source.length() && " \r\n\t".indexOf(source.charAt(position)) >= 0) position++;
    }
    private boolean take(char c) {
        if (position < source.length() && source.charAt(position) == c) { position++; return true; }
        return false;
    }
    private void expect(char c) { JsonSupport.require(take(c), "JSON_SYNTAX"); }
    private static void validUnicode(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                JsonSupport.require(++i < text.length() && Character.isLowSurrogate(text.charAt(i)), "JSON_SURROGATE");
            } else JsonSupport.require(!Character.isLowSurrogate(c), "JSON_SURROGATE");
        }
    }

    static Map<String, Object> object(Object value) {
        JsonSupport.require(value instanceof Map<?, ?>, "JSON_OBJECT_REQUIRED");
        Map<String, Object> result = new TreeMap<>();
        ((Map<?, ?>) value).forEach((key, item) -> {
            JsonSupport.require(key instanceof String, "JSON_STRING_KEY_REQUIRED");
            result.put((String) key, item);
        });
        return result;
    }
    static String string(Map<String, Object> map, String key) {
        Object value = map.get(key);
        JsonSupport.require(value instanceof String, "JSON_STRING_REQUIRED");
        return (String) value;
    }
    static List<?> array(Object value) {
        JsonSupport.require(value instanceof List<?>, "JSON_ARRAY_REQUIRED");
        return (List<?>) value;
    }
    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.toString();
    }
    private static void write(Object value, StringBuilder out, int depth) {
        JsonSupport.require(depth <= 64, "JSON_COMPLEXITY_LIMIT");
        if (value == null) out.append("null");
        else if (value instanceof String text) {
            validUnicode(text);
            out.append('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '"' || c == '\\') out.append('\\').append(c);
                else if (c < 32) out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                else out.append(c);
            }
            out.append('"');
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof BigDecimal) out.append(value);
        else if (value instanceof Map<?, ?>) {
            out.append('{');
            boolean first = true;
            for (var entry : object(value).entrySet()) {
                if (!first) out.append(','); first = false;
                write(entry.getKey(), out, depth + 1); out.append(':'); write(entry.getValue(), out, depth + 1);
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(',');
                write(list.get(i), out, depth + 1);
            }
            out.append(']');
        } else throw new IllegalArgumentException("JSON_UNSUPPORTED_VALUE");
    }
}
