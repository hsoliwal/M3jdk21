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
import jdk.m3.collections.M3PrimitiveKind;
import jdk.m3.collections.M3PrimitivePrimitiveHashMap;

/**
 * Primitive-key long counter map.
 *
 * <p>The configured missing value is not retained. An update that reaches the
 * missing value removes the key, matching counter-map semantics without an
 * entry object or boxed counter.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveCounterMap.java}
 * blob {@code 4b9f1af80a14feacb8205b62aa8bb6f001d1454a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveCounterMap {
    @FunctionalInterface
    public interface CounterConsumer {
        void accept(long keyBits, long count);
    }

    private final M3PrimitiveKind keyKind;
    private final long missingValue;
    private final M3PrimitivePrimitiveHashMap counts;

    public M3PrimitiveCounterMap(M3PrimitiveKind keyKind) {
        this(keyKind, 0L);
    }

    public M3PrimitiveCounterMap(M3PrimitiveKind keyKind, long missingValue) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.missingValue = missingValue;
        counts = new M3PrimitivePrimitiveHashMap(keyKind, M3PrimitiveKind.LONG);
    }

    public M3PrimitiveKind keyKind() { return keyKind; }
    public long missingValue() { return missingValue; }
    public int size() { return counts.size(); }
    public boolean isEmpty() { return counts.isEmpty(); }
    public long payloadBytes() { return counts.payloadBytes(); }

    public boolean containsKeyBits(long keyBits) {
        return counts.containsKeyBits(keyBits);
    }

    public long get(long keyBits) {
        return counts.getOrDefaultBits(keyBits, missingValue);
    }

    public long put(long keyBits, long value) {
        if (value == missingValue) {
            throw new IllegalArgumentException("cannot store missingValue");
        }
        return counts.putBits(keyBits, value, missingValue);
    }

    public long getAndAdd(long keyBits, long delta) {
        long previous = get(keyBits);
        if (delta == 0L) return previous;
        long next = Math.addExact(previous, delta);
        if (next == missingValue) {
            counts.removeOrDefaultBits(keyBits, missingValue);
        } else {
            counts.putBits(keyBits, next, missingValue);
        }
        return previous;
    }

    public long addAndGet(long keyBits, long delta) {
        long previous = getAndAdd(keyBits, delta);
        return Math.addExact(previous, delta);
    }

    public long increment(long keyBits) { return addAndGet(keyBits, 1L); }
    public long decrement(long keyBits) { return addAndGet(keyBits, -1L); }
    public long getAndIncrement(long keyBits) { return getAndAdd(keyBits, 1L); }
    public long getAndDecrement(long keyBits) { return getAndAdd(keyBits, -1L); }

    public long minValue() {
        return counts.minValueOrDefaultBits(missingValue);
    }

    public long maxValue() {
        return counts.maxValueOrDefaultBits(missingValue);
    }

    public long remove(long keyBits) {
        return counts.removeOrDefaultBits(keyBits, missingValue);
    }

    public void clear() { counts.clear(); }

    public void forEach(CounterConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        counts.forEach(consumer::accept);
    }
}
