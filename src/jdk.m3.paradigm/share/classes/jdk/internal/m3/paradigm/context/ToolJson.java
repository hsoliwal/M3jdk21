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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Deterministic explicit report projection; no reflective object or context serialization.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/tooling/ToolJson.java}
 * blob {@code 9c6bce4063d801f2ffc71cd6e1343050429c7cf0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ToolJson {
    private ToolJson() { }
    public static String encode(Object value) { return encode(value, 0); }
    private static String encode(Object value, int depth) {
        if (depth > 32) throw new IllegalArgumentException("JSON projection too deep");
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Enum<?> tag) return quote(tag.name());
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) return value.toString();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((k, v) -> {
                if (!(k instanceof String key)) throw new IllegalArgumentException("JSON key must be text");
                sorted.put(key, v);
            });
            return sorted.entrySet().stream().map(e -> quote(e.getKey()) + ":" + encode(e.getValue(), depth + 1))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value instanceof List<?> list) {
            return list.stream().map(v -> encode(v, depth + 1)).collect(Collectors.joining(",", "[", "]"));
        }
        throw new IllegalArgumentException("Explicit JSON projection required");
    }
    public static String quote(String text) {
        StringBuilder result = new StringBuilder("\"");
        text.chars().forEach(c -> append(result, (char) c));
        return result.append('"').toString();
    }
    private static void append(StringBuilder result, char c) {
        switch (c) {
            case '"' -> result.append("\\\"");
            case '\\' -> result.append("\\\\");
            case '\n' -> result.append("\\n");
            case '\r' -> result.append("\\r");
            case '\t' -> result.append("\\t");
            default -> {
                if (c < 32 || Character.isSurrogate(c)) {
                    java.util.HexFormat.of().toHexDigits(java.util.HexFormat.of().toHexDigits(result.append("\\u"), (byte) (c >>> 8)), (byte) c);
                } else result.append(c);
            }
        }
    }
}
