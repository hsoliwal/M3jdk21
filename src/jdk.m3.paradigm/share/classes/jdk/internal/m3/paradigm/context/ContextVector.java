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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Immutable, deeply copied JSON-valued dimensions. Missing and explicit null remain distinct.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/grail/ContextVector.java}
 * blob {@code 3de4c3532a3da2f2c998beb123f40599ee3d2026} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ContextVector(Map<String, Object> values) {
    public ContextVector {
        Objects.requireNonNull(values, "values");
        values = copyMap(values, 0);
    }

    public static ContextVector empty() { return new ContextVector(Map.of()); }

    public ContextVector extend(Map<String, ?> updates) {
        var merged = new LinkedHashMap<>(values);
        Objects.requireNonNull(updates, "updates").forEach(merged::put);
        return new ContextVector(merged);
    }

    /** Projection omits absent dimensions rather than fabricating null values. */
    public ContextVector project(String... dimensions) {
        var result = new LinkedHashMap<String, Object>();
        java.util.Arrays.stream(dimensions).filter(values::containsKey)
                .forEach(key -> result.put(key, values.get(key)));
        return new ContextVector(result);
    }

    /** Strict union: neither operand wins a conflict, including null versus absent. */
    public Optional<ContextVector> unify(ContextVector other) {
        Objects.requireNonNull(other, "other");
        boolean conflict = other.values.entrySet().stream().anyMatch(entry ->
                values.containsKey(entry.getKey()) && !Objects.equals(values.get(entry.getKey()), entry.getValue()));
        return conflict ? Optional.empty() : Optional.of(extend(other.values));
    }

    public String json() { return jsonValue(values); }

    private static Map<String, Object> copyMap(Map<?, ?> source, int depth) {
        if (depth > 64) throw new IllegalArgumentException("JSON nesting exceeds 64");
        var result = new LinkedHashMap<String, Object>();
        source.forEach((key, value) -> {
            if (!(key instanceof String name) || name.isBlank())
                throw new IllegalArgumentException("Dimension/object keys must be nonblank strings");
            result.put(name, copyValue(value, depth + 1));
        });
        return Collections.unmodifiableMap(result);
    }

    private static Object copyValue(Object value, int depth) {
        if (depth > 64) throw new IllegalArgumentException("JSON nesting exceeds 64");
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof BigInteger || value instanceof BigDecimal) return value;
        if (value instanceof Double number && Double.isFinite(number)) return value;
        if (value instanceof Float number && Float.isFinite(number)) return value;
        if (value instanceof Map<?, ?> map) return copyMap(map, depth);
        if (value instanceof List<?> list) return list.stream().map(item -> copyValue(item, depth + 1)).toList();
        throw new IllegalArgumentException("Not an immutable JSON value: " + value.getClass().getName());
    }

    /** Only serializes already validated values; JSON parsing belongs to the host's existing codec. */
    private static String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Map<?, ?> map) return map.entrySet().stream()
                .sorted(java.util.Comparator.comparing(entry -> (String) entry.getKey()))
                .map(entry -> quote((String) entry.getKey()) + ":" + jsonValue(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
        if (value instanceof List<?> list) return list.stream().map(ContextVector::jsonValue)
                .collect(Collectors.joining(",", "[", "]"));
        return value.toString();
    }

    private static String quote(String text) {
        var out = new StringBuilder("\"");
        text.chars().forEach(ch -> {
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 32 || Character.isSurrogate((char) ch))
                        out.append(String.format(java.util.Locale.ROOT, "\\u%04x", ch));
                    else out.append((char) ch);
                }
            }
        });
        return out.append('"').toString();
    }
}
