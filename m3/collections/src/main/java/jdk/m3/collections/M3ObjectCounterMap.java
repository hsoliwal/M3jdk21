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
package jdk.m3.collections;

import java.util.Objects;

/**
 * Object-key long counter map with Agrona-style missing-value semantics.
 *
 * <p>Object keys occupy one flat reference lane; counts occupy one long[] lane.
 * There is no entry object per key.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ObjectCounterMap.java}
 * blob {@code 9f74856b95cfd3985d5893d9816c8bd9398cf1e9} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3ObjectCounterMap<K> {
    @FunctionalInterface
    public interface CounterConsumer<K> {
        void accept(K key, long count);
    }

    private final long missingValue;
    private final M3ObjectPrimitiveHashMap<K> counts;

    public M3ObjectCounterMap() {
        this(0L);
    }

    public M3ObjectCounterMap(long missingValue) {
        this.missingValue = missingValue;
        counts = new M3ObjectPrimitiveHashMap<>(PrimitiveKind.LONG);
    }

    public long missingValue() { return missingValue; }
    public int size() { return counts.size(); }
    public boolean isEmpty() { return counts.isEmpty(); }
    public long primitivePayloadBytes() { return counts.primitivePayloadBytes(); }
    public int referenceSlots() { return counts.referenceSlots(); }

    public boolean containsKey(Object key) { return counts.containsKey(key); }

    public long get(Object key) {
        return counts.getOrDefaultBits(key, missingValue);
    }

    public long putBits(K key, long value) {
        if (value == missingValue) {
            throw new IllegalArgumentException("cannot store missingValue");
        }
        return counts.putBits(key, value, missingValue);
    }

    public long getAndAdd(K key, long delta) {
        long previous = get(key);
        if (delta == 0L) return previous;
        long next = Math.addExact(previous, delta);
        if (next == missingValue) {
            counts.removeOrDefaultBits(key, missingValue);
        } else {
            counts.putBits(key, next, missingValue);
        }
        return previous;
    }

    public long addAndGet(K key, long delta) {
        long previous = getAndAdd(key, delta);
        return Math.addExact(previous, delta);
    }

    public long increment(K key) { return addAndGet(key, 1L); }
    public long decrement(K key) { return addAndGet(key, -1L); }
    public long getAndIncrement(K key) { return getAndAdd(key, 1L); }
    public long getAndDecrement(K key) { return getAndAdd(key, -1L); }

    public long remove(Object key) {
        return counts.removeOrDefaultBits(key, missingValue);
    }

    public long minValue() {
        if (counts.isEmpty()) return missingValue;
        long[] values = (long[]) counts.valueArray();
        Object[] keys = counts.keyArray();
        long best = counts.hasNullKey() ? counts.nullValueBits() : Long.MAX_VALUE;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && values[i] < best) best = values[i];
        }
        return best;
    }

    public long maxValue() {
        if (counts.isEmpty()) return missingValue;
        long[] values = (long[]) counts.valueArray();
        Object[] keys = counts.keyArray();
        long best = counts.hasNullKey() ? counts.nullValueBits() : Long.MIN_VALUE;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null && values[i] > best) best = values[i];
        }
        return best;
    }

    public void clear() { counts.clear(); }

    public void forEach(CounterConsumer<? super K> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        counts.forEach(consumer::accept);
    }
}
