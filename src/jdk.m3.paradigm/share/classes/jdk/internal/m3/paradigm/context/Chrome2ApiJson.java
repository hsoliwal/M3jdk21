/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded JSON codec for the small OpenAI-compatible subset used by this bridge.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/chrome/Chrome2ApiJson.java}
 * blob {@code 8d9c492aa19fd7c1919c03240d62d7d0fb922ea1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
final class Chrome2ApiJson {
    private static final int MAX_DEPTH = 64;
    private static final int MAX_VALUES = 100_000;

    private Chrome2ApiJson() {
    }

    static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value, 0);
        return out.toString();
    }

    static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.read(0);
        parser.space();
        if (!parser.end()) {
            throw new IllegalArgumentException("Trailing JSON data");
        }
        return value;
    }

    private static void write(StringBuilder out, Object value, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("JSON nesting limit exceeded");
        }
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            string(out, text);
        } else if (value instanceof Boolean flag) {
            out.append(flag);
        } else if (value instanceof Integer
                || value instanceof Long
                || value instanceof Short
                || value instanceof Byte) {
            out.append(value);
        } else if (value instanceof Float number) {
            finite(out, number.doubleValue(), number.toString());
        } else if (value instanceof Double number) {
            finite(out, number, number.toString());
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException(
                            "JSON object keys must be strings");
                }
                if (!first) {
                    out.append(',');
                }
                first = false;
                string(out, key);
                out.append(':');
                write(out, entry.getValue(), depth + 1);
            }
            out.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            out.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                write(out, item, depth + 1);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException(
                    "Unsupported JSON value: " + value.getClass().getName());
        }
    }

    private static void finite(
            StringBuilder out, double value, String text) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("non-finite JSON number");
        }
        out.append(text);
    }

    private static void string(StringBuilder out, String text) {
        out.append('"');
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;
        private int values;

        Parser(String text) {
            this.text = java.util.Objects.requireNonNull(text, "text");
        }

        Object read(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("JSON nesting limit exceeded");
            }
            if (++values > MAX_VALUES) {
                throw error("JSON value limit exceeded");
            }
            space();
            if (end()) {
                throw error("Unexpected end of JSON");
            }
            char ch = text.charAt(index);
            return switch (ch) {
                case '{' -> object(depth + 1);
                case '[' -> array(depth + 1);
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object(int depth) {
            index++;
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            space();
            if (take('}')) {
                return result;
            }
            for (;;) {
                space();
                if (end() || text.charAt(index) != '"') {
                    throw error("Expected JSON object key");
                }
                String key = string();
                space();
                require(':');
                if (result.containsKey(key)) {
                    throw error("Duplicate JSON object key");
                }
                result.put(key, read(depth));
                space();
                if (take('}')) {
                    return result;
                }
                require(',');
            }
        }

        private List<Object> array(int depth) {
            index++;
            ArrayList<Object> result = new ArrayList<>();
            space();
            if (take(']')) {
                return result;
            }
            for (;;) {
                result.add(read(depth));
                space();
                if (take(']')) {
                    return result;
                }
                require(',');
            }
        }

        private String string() {
            require('"');
            StringBuilder out = new StringBuilder();
            while (!end()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return out.toString();
                }
                if (ch != '\\') {
                    if (ch < 0x20) {
                        throw error("Control character in JSON string");
                    }
                    out.append(ch);
                    continue;
                }
                if (end()) {
                    throw error("Truncated JSON escape");
                }
                char escape = text.charAt(index++);
                switch (escape) {
                    case '"', '\\', '/' -> out.append(escape);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> out.append(unicode());
                    default -> throw error("Invalid JSON escape");
                }
            }
            throw error("Unterminated JSON string");
        }

        private char unicode() {
            if (index + 4 > text.length()) {
                throw error("Truncated unicode escape");
            }
            int value = 0;
            for (int offset = 0; offset < 4; offset++) {
                int digit = Character.digit(text.charAt(index++), 16);
                if (digit < 0) {
                    throw error("Invalid unicode escape");
                }
                value = value * 16 + digit;
            }
            return (char) value;
        }

        private Number number() {
            int start = index;
            take('-');
            digits();
            boolean floating = false;
            if (take('.')) {
                floating = true;
                digits();
            }
            if (!end()
                    && (text.charAt(index) == 'e'
                    || text.charAt(index) == 'E')) {
                floating = true;
                index++;
                if (!end()
                        && (text.charAt(index) == '+'
                        || text.charAt(index) == '-')) {
                    index++;
                }
                digits();
            }
            String number = text.substring(start, index);
            try {
                if (floating) {
                    double value = Double.parseDouble(number);
                    if (!Double.isFinite(value)) {
                        throw error("Non-finite JSON number");
                    }
                    return value;
                }
                return Long.parseLong(number);
            } catch (NumberFormatException exception) {
                throw error("Invalid JSON number");
            }
        }

        private void digits() {
            int start = index;
            while (!end() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("Expected digit");
            }
        }

        private Object literal(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw error("Invalid JSON literal");
            }
            index += literal.length();
            return value;
        }

        void space() {
            while (!end() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        boolean end() {
            return index >= text.length();
        }

        private boolean take(char expected) {
            if (!end() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void require(char expected) {
            if (!take(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(
                    message + " at index " + index);
        }
    }
}
