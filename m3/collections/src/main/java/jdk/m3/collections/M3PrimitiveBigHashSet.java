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
import java.util.function.LongConsumer;

/**
 * Long-sized open-addressed primitive hash set using segmented arrays.
 *
 * <p>The normalized zero key bit-pattern is the empty-slot sentinel. A real
 * zero key is represented by one collection-level flag. The segmented BYTE
 * occupancy lane used by the previous implementation is eliminated entirely.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveBigHashSet.java}
 * blob {@code b420d46c9936ace3f547a3272f8a9b43842d7867} at com.synexia develop {@code eaacff5931ef16a1a52425043bb71b821eda3515} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveBigHashSet {
    private static final float DEFAULT_LOAD_FACTOR = 0.65f;
    private static final long MAX_CAPACITY = 1L << 40;

    private final PrimitiveKind kind;
    private final float loadFactor;
    private final int segmentShift;
    private final BigArray.Factory arrays;
    private BigArray keys;
    private long mask;
    private long threshold;
    private long size;
    private boolean hasZeroKey;

    public M3PrimitiveBigHashSet(PrimitiveKind kind) {
        this(kind, 16L, DEFAULT_LOAD_FACTOR, BigArray.DEFAULT_SEGMENT_SHIFT);
    }

    public M3PrimitiveBigHashSet(
            PrimitiveKind kind, long expected, float loadFactor, int segmentShift) {
        this(kind, expected, loadFactor, segmentShift, BigArray::new);
    }

    /** Package-private cold allocation seam; public construction retains ordinary arrays. */
    M3PrimitiveBigHashSet(
            PrimitiveKind kind, long expected, float loadFactor, int segmentShift,
            BigArray.Factory arrays) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor must be in (0,1)");
        }
        this.loadFactor = loadFactor;
        this.segmentShift = segmentShift;
        this.arrays = Objects.requireNonNull(arrays, "arrays");
        long capacity = capacityFor(expected, loadFactor);
        keys = allocate(capacity);
        mask = capacity - 1L;
        threshold = threshold(capacity, loadFactor);
    }

    public PrimitiveKind kind() { return kind; }
    public long size() { return size; }
    public boolean isEmpty() { return size == 0L; }
    public long capacity() { return mask + 1L; }
    public long payloadBytes() { return keys.payloadBytes(); }

    public boolean containsBits(long bits) {
        return isZeroKey(bits) ? hasZeroKey : findNonZero(bits) >= 0L;
    }

    public boolean addBits(long bits) {
        if (isZeroKey(bits)) {
            if (hasZeroKey) return false;
            if (size + 1L > threshold) rehash(capacity() << 1);
            hasZeroKey = true;
            size++;
            return true;
        }

        long position = insertionPositionNonZero(bits);
        if (position < 0L) return false;
        if (size + 1L > threshold) {
            rehash(capacity() << 1);
            position = insertionPositionNonZero(bits);
        }
        keys.setBits(position, bits);
        size++;
        return true;
    }

    public boolean removeBits(long bits) {
        if (isZeroKey(bits)) {
            if (!hasZeroKey) return false;
            hasZeroKey = false;
            size--;
            return true;
        }
        long position = findNonZero(bits);
        if (position < 0L) return false;
        shiftKeys(position);
        size--;
        return true;
    }

    public void clear() {
        keys.fillBits(0L, capacity(), 0L);
        hasZeroKey = false;
        size = 0L;
    }

    public void forEachBits(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (hasZeroKey) consumer.accept(0L);
        long remaining = size - (hasZeroKey ? 1L : 0L);
        for (long i = 0L; remaining > 0L && i < capacity(); i++) {
            long key = keys.getBits(i);
            if (key != 0L) {
                consumer.accept(key);
                remaining--;
            }
        }
    }

    private long findNonZero(long bits) {
        long position = mix64(kind.normalizeBits(bits)) & mask;
        long current;
        while ((current = keys.getBits(position)) != 0L) {
            if (kind.equalBits(current, bits)) return position;
            position = (position + 1L) & mask;
        }
        return -1L;
    }

    /** Returns slot, or -(existing+1). */
    private long insertionPositionNonZero(long bits) {
        long position = mix64(kind.normalizeBits(bits)) & mask;
        long current;
        while ((current = keys.getBits(position)) != 0L) {
            if (kind.equalBits(current, bits)) return -position - 1L;
            position = (position + 1L) & mask;
        }
        return position;
    }

    private void shiftKeys(long position) {
        for (;;) {
            long last = position;
            position = (position + 1L) & mask;
            long current;
            while ((current = keys.getBits(position)) != 0L) {
                long slot = mix64(kind.normalizeBits(current)) & mask;
                if (last <= position ? last >= slot || slot > position
                        : last >= slot && slot > position) {
                    break;
                }
                position = (position + 1L) & mask;
            }
            if (current == 0L) {
                keys.setBits(last, 0L);
                return;
            }
            keys.setBits(last, current);
        }
    }

    private void rehash(long newCapacity) {
        if (newCapacity > MAX_CAPACITY) throw new IllegalStateException("collection too large");
        BigArray oldKeys = keys;
        long oldCapacity = capacity();

        BigArray nextKeys = allocate(newCapacity);
        long nextMask = newCapacity - 1L;

        for (long i = 0; i < oldCapacity; i++) {
            long key = oldKeys.getBits(i);
            if (key == 0L) continue;
            long position = mix64(kind.normalizeBits(key)) & nextMask;
            while (nextKeys.getBits(position) != 0L) position = (position + 1L) & nextMask;
            nextKeys.setBits(position, key);
        }

        keys = nextKeys;
        mask = nextMask;
        threshold = threshold(newCapacity, loadFactor);
    }

    private BigArray allocate(long capacity) {
        BigArray result = Objects.requireNonNull(
                arrays.create(kind, capacity, segmentShift), "allocated array");
        if (result.kind() != kind || result.length() != capacity
                || result.segmentShift() != segmentShift) {
            throw new IllegalStateException("allocated array has wrong geometry or kind");
        }
        return result;
    }

    private boolean isZeroKey(long bits) {
        return kind.normalizeBits(bits) == 0L;
    }

    private static long capacityFor(long expected, float loadFactor) {
        if (expected < 0) throw new IllegalArgumentException("expected < 0");
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
