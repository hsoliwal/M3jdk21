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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Sparse immutable schema-bound object map: one presence word plus dense values.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextOrdinalMap.java}
 * blob {@code 56c0301e7c421f4218e5e4e6c3dcc52951b29665} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalMap<V> {
    private static final Object[] EMPTY = new Object[0];

    private final OrdinalSchema schema;
    private final long present;
    private final Object[] values;

    private OrdinalMap(OrdinalSchema schema, long present, Object[] values) {
        this.schema = schema;
        this.present = present;
        this.values = values;
    }

    public static <V> OrdinalMap<V> empty(OrdinalSchema schema) {
        return new OrdinalMap<>(OrdinalSchema.requireHot(schema), 0L, EMPTY);
    }

    public OrdinalSchema schema() {
        return schema;
    }

    public int size() {
        return values.length;
    }

    public boolean isEmpty() {
        return present == 0L;
    }

    public long presentBits() {
        return present;
    }

    public boolean containsOrdinal(int ordinal) {
        requireOrdinal(ordinal);
        return (present & (1L << ordinal)) != 0L;
    }

    public boolean containsKey(String key) {
        int ordinal = schema.ordinal(key);
        return ordinal >= 0 && containsOrdinal(ordinal);
    }

    @SuppressWarnings("unchecked")
    public V get(int ordinal) {
        requireOrdinal(ordinal);
        int dense = OrdinalSchema.denseIndex(present, ordinal);
        return dense < 0 ? null : (V) values[dense];
    }

    public V get(String key) {
        int ordinal = schema.ordinal(key);
        return ordinal < 0 ? null : get(ordinal);
    }

    public OrdinalMap<V> with(String key, V value) {
        return with(schema.requireOrdinal(key), value);
    }

    public OrdinalMap<V> with(int ordinal, V value) {
        requireOrdinal(ordinal);
        Objects.requireNonNull(value, "value");
        long bit = 1L << ordinal;
        int dense = OrdinalSchema.insertionIndex(present, ordinal);
        if ((present & bit) != 0L) {
            if (Objects.equals(values[dense], value)) {
                return this;
            }
            Object[] copy = values.clone();
            copy[dense] = value;
            return new OrdinalMap<>(schema, present, copy);
        }
        Object[] copy = new Object[values.length + 1];
        System.arraycopy(values, 0, copy, 0, dense);
        copy[dense] = value;
        System.arraycopy(values, dense, copy, dense + 1, values.length - dense);
        return new OrdinalMap<>(schema, present | bit, copy);
    }

    public OrdinalMap<V> without(String key) {
        int ordinal = schema.ordinal(key);
        return ordinal < 0 ? this : without(ordinal);
    }

    public OrdinalMap<V> without(int ordinal) {
        requireOrdinal(ordinal);
        long bit = 1L << ordinal;
        if ((present & bit) == 0L) {
            return this;
        }
        int dense = OrdinalSchema.insertionIndex(present, ordinal);
        if (values.length == 1) {
            return empty(schema);
        }
        Object[] copy = new Object[values.length - 1];
        System.arraycopy(values, 0, copy, 0, dense);
        System.arraycopy(values, dense + 1, copy, dense, values.length - dense - 1);
        return new OrdinalMap<>(schema, present & ~bit, copy);
    }

    public void forEach(OrdinalConsumer<? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        long bits = present;
        int dense = 0;
        while (bits != 0L) {
            int ordinal = Long.numberOfTrailingZeros(bits);
            @SuppressWarnings("unchecked") V value = (V) values[dense++];
            consumer.accept(ordinal, value);
            bits &= bits - 1L;
        }
    }

    public Map<String, V> toMap() {
        TreeMap<String, V> result = new TreeMap<>();
        forEach((ordinal, value) -> result.put(schema.name(ordinal), value));
        return Collections.unmodifiableMap(result);
    }

    public Object[] valuesCopy() {
        return Arrays.copyOf(values, values.length);
    }

    @FunctionalInterface
    public interface OrdinalConsumer<V> {
        void accept(int ordinal, V value);
    }

    private void requireOrdinal(int ordinal) {
        Objects.checkIndex(ordinal, schema.size());
    }
}
