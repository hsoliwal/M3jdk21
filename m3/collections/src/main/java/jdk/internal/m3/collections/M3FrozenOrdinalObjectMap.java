/*******************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Immutable reference column over shared ordinal keys. References are copied;
 * referenced objects are not deep-frozen. Lookup performs no key boxing.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/FrozenOrdinalObjectMap.java}
 * blob {@code 720a73b589c0e45a2b67144d70b8408cf4a4007a} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.collections batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3FrozenOrdinalObjectMap<V> {
    /** Primitive key traversal without a Map.Entry object. */
    @FunctionalInterface
    public interface EntryConsumer<V> {
        void accept(int ordinal, V value);
    }

    private final M3FrozenOrdinalIndex index;
    private final V[] values;

    public M3FrozenOrdinalObjectMap(M3FrozenOrdinalIndex index, V[] valuesInOrdinalOrder) {
        this.index = Objects.requireNonNull(index, "index");
        Objects.requireNonNull(valuesInOrdinalOrder, "valuesInOrdinalOrder");
        if (valuesInOrdinalOrder.length != index.size()) {
            throw new IllegalArgumentException("value count differs from index cardinality");
        }
        values = valuesInOrdinalOrder.clone();
        for (V value : values) {
            Objects.requireNonNull(value, "value");
        }
    }

    public M3FrozenOrdinalIndex index() { return index; }
    public int size() { return values.length; }
    /** Reference count; reference byte width is deliberately not guessed. */
    public int referenceSlots() { return values.length; }
    public boolean containsKey(int ordinal) { return index.contains(ordinal); }

    /** Null is permitted as an absent fallback; stored values are nonnull. */
    public V getOrDefault(int ordinal, V fallback) {
        int position = index.positionOf(ordinal);
        return position < 0 ? fallback : values[position];
    }

    public V valueAt(int position) {
        return values[Objects.checkIndex(position, values.length)];
    }

    /** Ascending ordinal order; immutable index and values are never exposed as writable arrays. */
    public void forEach(EntryConsumer<? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int position = 0; position < values.length; position++) {
            consumer.accept(index.ordinalAt(position), values[position]);
        }
    }
}
