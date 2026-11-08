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
package jdk.m3.collections.packed;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.collections.PrimitiveArrayList;

/**
 * Mutable boolean list retaining exactly one bit per capacity element.
 *
 * <p>This is the minimum-memory boolean sequence. The universal
 * {@link PrimitiveArrayList} remains byte-backed for raw primitive-array
 * compatibility; new boolean-heavy code should prefer this type.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PackedBooleanList.java}
 * blob {@code 2402a10b81413cecf21a34aa7f76ec399d7be84a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PackedBooleanList {

    @FunctionalInterface
    public interface BooleanConsumer {
        void accept(boolean value);
    }

    private long[] words;
    private int size;

    public PackedBooleanList() {
        this(64);
    }

    public PackedBooleanList(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        words = new long[wordCount(initialCapacity)];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return words.length << 6; }
    public long payloadBytes() { return (long) words.length * Long.BYTES; }

    public void add(boolean value) {
        ensureCapacity(size + 1);
        write(size++, value);
    }

    public void add(int index, boolean value) {
        checkInsert(index);
        ensureCapacity(size + 1);
        for (int i = size; i > index; i--) {
            write(i, read(i - 1));
        }
        write(index, value);
        size++;
    }

    public boolean get(int index) {
        check(index);
        return read(index);
    }

    public boolean set(int index, boolean value) {
        check(index);
        boolean old = read(index);
        write(index, value);
        return old;
    }

    public boolean removeAt(int index) {
        check(index);
        boolean old = read(index);
        for (int i = index; i < size - 1; i++) {
            write(i, read(i + 1));
        }
        size--;
        write(size, false);
        return old;
    }

    public boolean remove(boolean value) {
        int index = indexOf(value);
        if (index < 0) return false;
        removeAt(index);
        return true;
    }

    public int indexOf(boolean value) {
        if (value) {
            int fullWords = size >>> 6;
            for (int word = 0; word < fullWords; word++) {
                long bits = words[word];
                if (bits != 0L) return (word << 6) + Long.numberOfTrailingZeros(bits);
            }
            int tail = size & 63;
            if (tail != 0) {
                long mask = (1L << tail) - 1L;
                long bits = words[fullWords] & mask;
                if (bits != 0L) return (fullWords << 6) + Long.numberOfTrailingZeros(bits);
            }
            return -1;
        }

        int fullWords = size >>> 6;
        for (int word = 0; word < fullWords; word++) {
            long zeroBits = ~words[word];
            if (zeroBits != 0L) return (word << 6) + Long.numberOfTrailingZeros(zeroBits);
        }
        int tail = size & 63;
        if (tail != 0) {
            long mask = (1L << tail) - 1L;
            long zeroBits = ~words[fullWords] & mask;
            if (zeroBits != 0L) return (fullWords << 6) + Long.numberOfTrailingZeros(zeroBits);
        }
        return -1;
    }

    /** Sorts false values before true values without allocating a second lane. */
    public void sort() {
        int trueCount = 0;
        for (int i = 0; i < wordCount(size); i++) {
            long bits = words[i];
            if (i == (size >>> 6) && (size & 63) != 0) {
                bits &= (1L << (size & 63)) - 1L;
            }
            trueCount += Long.bitCount(bits);
        }
        Arrays.fill(words, 0L);
        int firstTrue = size - trueCount;
        for (int i = firstTrue; i < size; i++) write(i, true);
    }

    /**
     * Binary search after {@link #sort()}; returns an arbitrary matching index
     * or the standard negative insertion-point encoding.
     */
    public int binarySearch(boolean value) {
        if (size == 0) return -1;
        int firstTrue = indexOf(true);
        if (firstTrue < 0) firstTrue = size;
        if (!value) {
            return firstTrue == 0 ? -1 : (firstTrue - 1) >>> 1;
        }
        return firstTrue == size ? -(size + 1) : firstTrue;
    }

    public void clear() {
        Arrays.fill(words, 0L);
        size = 0;
    }

    public void ensureCapacity(int required) {
        if (required < 0) throw new IllegalArgumentException("required < 0");
        int requiredWords = wordCount(required);
        if (requiredWords <= words.length) return;
        int next = Math.max(requiredWords, Math.max(1, words.length + (words.length >>> 1) + 1));
        words = Arrays.copyOf(words, next);
    }

    public void trimToSize() {
        words = Arrays.copyOf(words, wordCount(size));
    }

    public boolean[] toBooleanArray() {
        boolean[] result = new boolean[size];
        for (int i = 0; i < size; i++) result[i] = read(i);
        return result;
    }

    public long[] toLongArray() {
        long[] result = Arrays.copyOf(words, wordCount(size));
        int tail = size & 63;
        if (tail != 0 && result.length != 0) {
            result[result.length - 1] &= (1L << tail) - 1L;
        }
        return result;
    }

    public void forEach(BooleanConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) consumer.accept(read(i));
    }

    private boolean read(int index) {
        return (words[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    private void write(int index, boolean value) {
        int word = index >>> 6;
        long bit = 1L << (index & 63);
        if (value) words[word] |= bit;
        else words[word] &= ~bit;
    }

    private static int wordCount(int bits) {
        return bits == 0 ? 0 : ((bits - 1) >>> 6) + 1;
    }

    private void check(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private void checkInsert(int index) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
    }
}
