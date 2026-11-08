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
 * Immutable long values over a shared ordinal index; no per-column stored key lane.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/FrozenOrdinalLongMap.java}
 * blob {@code 9e10bcd276f4fc0827439bee54ab3ba4c31b9176} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.collections batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FrozenOrdinalLongMap {
    /** Primitive traversal boundary: no boxed key, value or entry. */
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(int ordinal, long value);
    }

    private final FrozenOrdinalIndex index;
    private final long[] values;

    /** Values must follow ascending present ordinals and are defensively copied. */
    public FrozenOrdinalLongMap(FrozenOrdinalIndex index, long[] valuesInOrdinalOrder) {
        this.index = Objects.requireNonNull(index, "index");
        Objects.requireNonNull(valuesInOrdinalOrder, "valuesInOrdinalOrder");
        if (valuesInOrdinalOrder.length != index.size()) {
            throw new IllegalArgumentException("value count differs from index cardinality");
        }
        values = valuesInOrdinalOrder.clone();
    }

    public FrozenOrdinalIndex index() { return index; }
    public int size() { return values.length; }
    public long valuePayloadBytes() { return 8L * values.length; }
    public boolean containsKey(int ordinal) { return index.contains(ordinal); }

    /** In-domain absence returns the fallback. Every long value, including MIN_VALUE, is valid. */
    public long getOrDefault(int ordinal, long fallback) {
        int position = index.positionOf(ordinal);
        return position < 0 ? fallback : values[position];
    }

    public long valueAt(int position) {
        return values[Objects.checkIndex(position, values.length)];
    }

    /** Ascending ordinal order; consumer exceptions propagate without changing this map. */
    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int position = 0; position < values.length; position++) {
            consumer.accept(index.ordinalAt(position), values[position]);
        }
    }
}
