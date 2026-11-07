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
 * Long-sized segmented primitive-to-primitive open-addressed hash map.
 *
 * <p>The normalized zero key bit-pattern is the empty slot. A real zero key and
 * its value are carried by collection-level fields; no segmented occupancy lane
 * is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveBigHashMap.java}
 * blob {@code ce96c3f8002079ed1c501b2d14a6c2f92c536329} at com.synexia develop {@code eaacff5931ef16a1a52425043bb71b821eda3515} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveBigHashMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private static final float DEFAULT_LOAD_FACTOR = 0.65f;
    private static final long MAX_CAPACITY = 1L << 40;

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final float loadFactor;
    private final int segmentShift;
    private final BigArray.Factory arrays;
    private BigArray keys;
    private BigArray values;
    private long mask;
    private long threshold;
    private long size;
    private boolean hasZeroKey;
    private long zeroValueBits;

    public M3PrimitiveBigHashMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this(keyKind, valueKind, 16L, DEFAULT_LOAD_FACTOR,
                BigArray.DEFAULT_SEGMENT_SHIFT);
    }

    public M3PrimitiveBigHashMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind,
            long expected, float loadFactor, int segmentShift) {
        this(keyKind, valueKind, expected, loadFactor, segmentShift, BigArray::new);
    }

    /** Package-private cold allocation seam; public construction retains ordinary arrays. */
    M3PrimitiveBigHashMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind,
            long expected, float loadFactor, int segmentShift, BigArray.Factory arrays) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor must be in (0,1)");
        }
        this.loadFactor = loadFactor;
        this.segmentShift = segmentShift;
        this.arrays = Objects.requireNonNull(arrays, "arrays");
        long capacity = capacityFor(expected, loadFactor);
        keys = allocate(keyKind, capacity);
        values = allocate(valueKind, capacity);
        mask = capacity - 1L;
        threshold = threshold(capacity, loadFactor);
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public long size() { return size; }
    public boolean isEmpty() { return size == 0L; }
    public long capacity() { return mask + 1L; }
    public long payloadBytes() {
        return keys.payloadBytes() + values.payloadBytes();
    }

    public boolean containsKeyBits(long keyBits) {
        return isZeroKey(keyBits) ? hasZeroKey : findNonZero(keyBits) >= 0L;
    }

    public long getOrDefaultBits(long keyBits, long defaultBits) {
        if (isZeroKey(keyBits)) return hasZeroKey ? zeroValueBits : defaultBits;
        long position = findNonZero(keyBits);
        return position < 0L ? defaultBits : values.getBits(position);
    }

    public long putBits(long keyBits, long valueBits, long absentBits) {
        if (isZeroKey(keyBits)) {
            long old = hasZeroKey ? zeroValueBits : absentBits;
            if (!hasZeroKey) {
                if (size + 1L > threshold) rehash(capacity() << 1);
                hasZeroKey = true;
                size++;
            }
            zeroValueBits = valueKind.storageBits(valueBits);
            return old;
        }

        long position = hash(keyBits) & mask;
        long current;
        while ((current = keys.getBits(position)) != 0L) {
            if (keyKind.equalBits(current, keyBits)) {
                long old = values.getBits(position);
                values.setBits(position, valueBits);
                return old;
            }
            position = (position + 1L) & mask;
        }

        if (size + 1L > threshold) {
            rehash(capacity() << 1);
            position = hash(keyBits) & mask;
            while (keys.getBits(position) != 0L) position = (position + 1L) & mask;
        }

        keys.setBits(position, keyBits);
        values.setBits(position, valueBits);
        size++;
        return absentBits;
    }

    public long removeOrDefaultBits(long keyBits, long defaultBits) {
        if (isZeroKey(keyBits)) {
            if (!hasZeroKey) return defaultBits;
            long old = zeroValueBits;
            zeroValueBits = 0L;
            hasZeroKey = false;
            size--;
            return old;
        }
        long position = findNonZero(keyBits);
        if (position < 0L) return defaultBits;
        long old = values.getBits(position);
        shiftKeys(position);
        size--;
        return old;
    }

    public void clear() {
        keys.fillBits(0L, capacity(), 0L);
        values.fillBits(0L, capacity(), 0L);
        hasZeroKey = false;
        zeroValueBits = 0L;
        size = 0L;
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (hasZeroKey) consumer.accept(0L, zeroValueBits);
        long remaining = size - (hasZeroKey ? 1L : 0L);
        for (long i = 0L; remaining > 0L && i < capacity(); i++) {
            long key = keys.getBits(i);
            if (key != 0L) {
                consumer.accept(key, values.getBits(i));
                remaining--;
            }
        }
    }

    private long findNonZero(long keyBits) {
        long position = hash(keyBits) & mask;
        long current;
        while ((current = keys.getBits(position)) != 0L) {
            if (keyKind.equalBits(current, keyBits)) return position;
            position = (position + 1L) & mask;
        }
        return -1L;
    }

    private void shiftKeys(long position) {
        for (;;) {
            long last = position;
            position = (position + 1L) & mask;
            long current;
            while ((current = keys.getBits(position)) != 0L) {
                long slot = hash(current) & mask;
                if (last <= position ? last >= slot || slot > position
                        : last >= slot && slot > position) {
                    break;
                }
                position = (position + 1L) & mask;
            }
            if (current == 0L) {
                keys.setBits(last, 0L);
                values.setBits(last, 0L);
                return;
            }
            keys.setBits(last, current);
            values.setBits(last, values.getBits(position));
        }
    }

    private void rehash(long newCapacity) {
        if (newCapacity > MAX_CAPACITY) throw new IllegalStateException("collection too large");
        BigArray oldKeys = keys;
        BigArray oldValues = values;
        long oldCapacity = capacity();

        BigArray nextKeys = allocate(keyKind, newCapacity);
        BigArray nextValues = allocate(valueKind, newCapacity);
        long nextMask = newCapacity - 1L;

        for (long i = 0; i < oldCapacity; i++) {
            long key = oldKeys.getBits(i);
            if (key == 0L) continue;
            long position = hash(key) & nextMask;
            while (nextKeys.getBits(position) != 0L) position = (position + 1L) & nextMask;
            nextKeys.setBits(position, key);
            nextValues.setBits(position, oldValues.getBits(i));
        }

        // Allocation and population can fail without publishing a partially replaced owner.
        keys = nextKeys;
        values = nextValues;
        mask = nextMask;
        threshold = threshold(newCapacity, loadFactor);
    }

    private BigArray allocate(PrimitiveKind kind, long capacity) {
        BigArray result = Objects.requireNonNull(
                arrays.create(kind, capacity, segmentShift), "allocated array");
        if (result.kind() != kind || result.length() != capacity
                || result.segmentShift() != segmentShift) {
            throw new IllegalStateException("allocated array has wrong geometry or kind");
        }
        return result;
    }

    private boolean isZeroKey(long bits) {
        return keyKind.normalizeBits(bits) == 0L;
    }

    private long hash(long keyBits) {
        return mix64(keyKind.normalizeBits(keyBits));
    }

    private static long capacityFor(long expected, float loadFactor) {
        if (expected < 0L) throw new IllegalArgumentException("expected < 0");
        long needed = Math.max(8L, (long) Math.ceil(expected / (double) loadFactor));
        if (needed > MAX_CAPACITY) throw new IllegalArgumentException("collection too large");
        long capacity = 1L;
        while (capacity < needed) capacity <<= 1;
        return capacity;
    }

    private static long threshold(long capacity, float loadFactor) {
        return Math.min(capacity - 1L, Math.max(1L, (long) (capacity * loadFactor)));
    }

    private static long mix64(long value) {
        long z = value;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
