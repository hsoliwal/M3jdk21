/* *****************************************************************************
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
package jdk.m3.collections.packed;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongConsumer;
import jdk.internal.m3.collections.bits.PrimitiveBits;

/**
 * Mutable 1..64-bit signed/unsigned sequence in one primitive word array.
 * Unsigned64 values use raw long bits. Not safe for concurrent mutation.
 * The existing PackedUnsignedIntList retains its independent compatibility API.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PackedLongList.java}
 * blob {@code c8001a26bd2d33e64093a2fd87944cdf777197f8} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PackedLongList {
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
    private final int bitsPerValue;
    private final boolean signed;
    private final long valueMask;
    private long[] words;
    private int size;

    public PackedLongList(int bitsPerValue, boolean signed) {
        this(bitsPerValue, signed, 16);
    }

    public PackedLongList(int bitsPerValue, boolean signed, int initialCapacity) {
        valueMask = PrimitiveBits.mask(bitsPerValue);
        this.bitsPerValue = bitsPerValue;
        this.signed = signed;
        words = new long[PrimitiveBits.packedWordCount(initialCapacity, bitsPerValue)];
    }

    public int bitsPerValue() { return bitsPerValue; }
    public boolean signed() { return signed; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() { return (long) words.length * Long.BYTES; }
    public int capacity() {
        return (int) Math.min(Integer.MAX_VALUE,
                (long) words.length * Long.SIZE / bitsPerValue);
    }

    public void add(long value) {
        PrimitiveBits.checkValue(value, bitsPerValue, signed);
        int required = Math.incrementExact(size);
        ensureCapacity(required);
        writeRaw(size, value & valueMask);
        size = required;
    }

    public long get(int index) {
        long raw = readRaw(Objects.checkIndex(index, size));
        return signed ? PrimitiveBits.signExtend(raw, bitsPerValue) : raw;
    }

    public long set(int index, long value) {
        Objects.checkIndex(index, size);
        PrimitiveBits.checkValue(value, bitsPerValue, signed);
        long old = get(index);
        writeRaw(index, value & valueMask);
        return old;
    }

    public long removeAt(int index) {
        Objects.checkIndex(index, size);
        long old = get(index);
        for (int i = index + 1; i < size; i++) writeRaw(i - 1, readRaw(i));
        writeRaw(--size, 0L);
        return old;
    }

    public void clear() {
        Arrays.fill(words, 0L);
        size = 0;
    }

    public void ensureCapacity(int required) {
        int count = PrimitiveBits.packedWordCount(required, bitsPerValue);
        if (count <= words.length) return;
        long grown = (long) words.length + (words.length >>> 1) + 1L;
        int next = (int) Math.min(MAX_ARRAY_LENGTH, Math.max(count, grown));
        words = Arrays.copyOf(words, next);
    }

    public void trimToSize() {
        words = Arrays.copyOf(words, PrimitiveBits.packedWordCount(size, bitsPerValue));
    }

    public long[] toArray() {
        long[] result = new long[size];
        for (int i = 0; i < size; i++) result[i] = get(i);
        return result;
    }

    /** Fresh exact-length word snapshot with canonical zero tail bits. */
    public long[] toWordArray() {
        return Arrays.copyOf(words, PrimitiveBits.packedWordCount(size, bitsPerValue));
    }

    public void forEach(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) consumer.accept(get(i));
    }

    /** First index whose value is at least key; input must be sorted in this signedness. */
    public int lowerBound(long key) {
        PrimitiveBits.checkValue(key, bitsPerValue, signed);
        int low = 0;
        int high = size;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            int comparison = signed ? Long.compare(get(middle), key)
                    : Long.compareUnsigned(get(middle), key);
            if (comparison < 0) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    /** First matching index, or -(insertionPoint + 1); requires sorted input. */
    public int binarySearch(long key) {
        int index = lowerBound(key);
        return index < size && get(index) == key ? index : -(index + 1);
    }

    private long readRaw(int index) {
        return PrimitiveBits.readPacked(words, (long) index * bitsPerValue, bitsPerValue);
    }

    private void writeRaw(int index, long raw) {
        PrimitiveBits.writePacked(words, (long) index * bitsPerValue, bitsPerValue, raw);
    }
}
