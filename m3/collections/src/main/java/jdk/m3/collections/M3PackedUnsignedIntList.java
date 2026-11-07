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

import java.util.Arrays;
import java.util.function.IntConsumer;

/**
 * Fixed-bit-width unsigned integer list packed into long[] words.
 *
 * <p>Useful for enum ordinals, state codes, small IDs and other narrow domains.
 * Each value consumes exactly {@code bitsPerValue} payload bits.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PackedUnsignedIntList.java}
 * blob {@code 64f0af2c91b4d08adad8bf6ae0c2b2b6bc3d3732} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PackedUnsignedIntList {
    private final int bitsPerValue;
    private final long valueMask;
    private long[] words;
    private int size;

    public M3PackedUnsignedIntList(int bitsPerValue) {
        this(bitsPerValue, 16);
    }

    public M3PackedUnsignedIntList(int bitsPerValue, int initialCapacity) {
        if (bitsPerValue < 1 || bitsPerValue > 32) {
            throw new IllegalArgumentException("bitsPerValue must be 1..32");
        }
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        this.bitsPerValue = bitsPerValue;
        valueMask = bitsPerValue == 32
                ? 0xffff_ffffL
                : (1L << bitsPerValue) - 1L;
        words = new long[Math.max(1, wordsFor(initialCapacity))];
    }

    public int bitsPerValue() { return bitsPerValue; }
    public long maxValue() { return valueMask; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() { return (long) words.length * Long.BYTES; }
    public int capacity() {
        return (int) Math.min(Integer.MAX_VALUE,
                ((long) words.length << 6) / bitsPerValue);
    }

    public void add(int value) {
        checkValue(value);
        ensure(size + 1);
        write(size++, Integer.toUnsignedLong(value));
    }

    public int get(int index) {
        return (int) read(check(index));
    }

    public int set(int index, int value) {
        checkValue(value);
        int checked = check(index);
        int old = (int) read(checked);
        write(checked, Integer.toUnsignedLong(value));
        return old;
    }

    public int removeAt(int index) {
        int checked = check(index);
        int old = (int) read(checked);
        for (int i = checked + 1; i < size; i++) write(i - 1, read(i));
        size--;
        write(size, 0L);
        return old;
    }

    public void clear() {
        Arrays.fill(words, 0L);
        size = 0;
    }

    public void trimToSize() {
        int wanted = Math.max(1, wordsFor(size));
        if (wanted != words.length) words = Arrays.copyOf(words, wanted);
    }

    public void forEach(IntConsumer consumer) {
        for (int i = 0; i < size; i++) consumer.accept((int) read(i));
    }

    public long[] toWordArray() {
        return Arrays.copyOf(words, wordsFor(size));
    }

    private long read(int index) {
        long bitIndex = (long) index * bitsPerValue;
        int wordIndex = (int) (bitIndex >>> 6);
        int shift = (int) bitIndex & 63;
        long value = words[wordIndex] >>> shift;
        int spill = shift + bitsPerValue - 64;
        if (spill > 0) value |= words[wordIndex + 1] << (bitsPerValue - spill);
        return value & valueMask;
    }

    private void write(int index, long value) {
        long bitIndex = (long) index * bitsPerValue;
        int wordIndex = (int) (bitIndex >>> 6);
        int shift = (int) bitIndex & 63;

        long lowMask = valueMask << shift;
        words[wordIndex] = (words[wordIndex] & ~lowMask)
                | ((value & valueMask) << shift);

        int spill = shift + bitsPerValue - 64;
        if (spill > 0) {
            long highMask = (1L << spill) - 1L;
            words[wordIndex + 1] = (words[wordIndex + 1] & ~highMask)
                    | ((value >>> (bitsPerValue - spill)) & highMask);
        }
    }

    private int check(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return index;
    }

    private void checkValue(int value) {
        long unsigned = Integer.toUnsignedLong(value);
        if ((unsigned & ~valueMask) != 0L) {
            throw new IllegalArgumentException("value exceeds packed width");
        }
    }

    private void ensure(int requiredElements) {
        int requiredWords = wordsFor(requiredElements);
        if (requiredWords <= words.length) return;
        int next = words.length + (words.length >>> 1) + 1;
        if (next < requiredWords) next = requiredWords;
        words = Arrays.copyOf(words, next);
    }

    private int wordsFor(int elements) {
        if (elements == 0) return 0;
        long bits = Math.multiplyExact((long) elements, bitsPerValue);
        long count = (bits + 63L) >>> 6;
        if (count > Integer.MAX_VALUE) throw new IllegalArgumentException("list too large");
        return (int) count;
    }
}
