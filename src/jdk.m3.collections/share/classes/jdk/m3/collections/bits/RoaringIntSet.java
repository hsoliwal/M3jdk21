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
package jdk.m3.collections.bits;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Roaring-style compressed unsigned-int set.
 *
 * <p>The 32-bit space is split into 16-bit chunks. Sparse chunks use sorted
 * char[] containers; dense chunks use fixed long[1024] bitmaps. There is one
 * container object per populated chunk, never one object per retained int.
 *
 * <p>Iteration order is unsigned-int order because chunk keys are the unsigned
 * high 16 bits.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveRoaringIntSet.java}
 * blob {@code 6b9b17a4e38de9b4566e955d106ca440e9d0bde7} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class RoaringIntSet {
    /** Creates an empty set (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public RoaringIntSet() {
    }

    private static final byte ARRAY = 0;
    private static final byte BITMAP = 1;
    private static final int ARRAY_TO_BITMAP = 4096;
    private static final int BITMAP_TO_ARRAY = 2048;
    private static final int BITMAP_WORDS = 1 << 10;

    private int[] highKeys = new int[4];
    private byte[] types = new byte[4];
    private Object[] containers = new Object[4];
    private int[] cardinalities = new int[4];
    private int containerCount;
    private long cardinality;

    public int containerCount() { return containerCount; }
    public long cardinality() { return cardinality; }
    public boolean isEmpty() { return cardinality == 0; }
    public int containerReferenceSlots() { return containers.length; }

    public long primitivePayloadBytes() {
        long bytes = (long) highKeys.length * Integer.BYTES
                + types.length
                + (long) cardinalities.length * Integer.BYTES;
        for (int i = 0; i < containerCount; i++) {
            bytes += types[i] == ARRAY
                    ? (long) ((char[]) containers[i]).length * Character.BYTES
                    : (long) BITMAP_WORDS * Long.BYTES;
        }
        return bytes;
    }

    public boolean contains(int value) {
        int high = value >>> 16;
        int index = Arrays.binarySearch(highKeys, 0, containerCount, high);
        if (index < 0) return false;
        char low = (char) value;
        if (types[index] == ARRAY) {
            char[] values = (char[]) containers[index];
            return Arrays.binarySearch(values, 0, cardinalities[index], low) >= 0;
        }
        long[] words = (long[]) containers[index];
        int ordinal = low;
        return (words[ordinal >>> 6] & (1L << (ordinal & 63))) != 0L;
    }

    public boolean add(int value) {
        int high = value >>> 16;
        char low = (char) value;
        int index = Arrays.binarySearch(highKeys, 0, containerCount, high);
        if (index < 0) {
            index = -index - 1;
            insertContainer(index, high);
        }
        boolean added = types[index] == ARRAY
                ? addArray(index, low)
                : addBitmap(index, low);
        if (added) cardinality++;
        return added;
    }

    public boolean remove(int value) {
        int high = value >>> 16;
        char low = (char) value;
        int index = Arrays.binarySearch(highKeys, 0, containerCount, high);
        if (index < 0) return false;
        boolean removed = types[index] == ARRAY
                ? removeArray(index, low)
                : removeBitmap(index, low);
        if (!removed) return false;
        cardinality--;
        if (cardinalities[index] == 0) removeContainer(index);
        return true;
    }

    public void clear() {
        Arrays.fill(containers, 0, containerCount, null);
        Arrays.fill(highKeys, 0, containerCount, 0);
        Arrays.fill(types, 0, containerCount, (byte) 0);
        Arrays.fill(cardinalities, 0, containerCount, 0);
        containerCount = 0;
        cardinality = 0L;
    }

    public void forEach(IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int container = 0; container < containerCount; container++) {
            int high = highKeys[container] << 16;
            if (types[container] == ARRAY) {
                char[] values = (char[]) containers[container];
                int count = cardinalities[container];
                for (int i = 0; i < count; i++) consumer.accept(high | values[i]);
            } else {
                long[] words = (long[]) containers[container];
                for (int wordIndex = 0; wordIndex < BITMAP_WORDS; wordIndex++) {
                    long word = words[wordIndex];
                    while (word != 0L) {
                        int bit = Long.numberOfTrailingZeros(word);
                        consumer.accept(high | (wordIndex << 6) | bit);
                        word &= word - 1L;
                    }
                }
            }
        }
    }

    public RoaringIntSet union(RoaringIntSet other) {
        Objects.requireNonNull(other, "other");
        RoaringIntSet result = copy();
        other.forEach(result::add);
        return result;
    }

    public RoaringIntSet intersection(RoaringIntSet other) {
        Objects.requireNonNull(other, "other");
        RoaringIntSet result = new RoaringIntSet();
        RoaringIntSet smaller = cardinality <= other.cardinality ? this : other;
        RoaringIntSet larger = smaller == this ? other : this;
        smaller.forEach(value -> {
            if (larger.contains(value)) result.add(value);
        });
        return result;
    }

    public RoaringIntSet copy() {
        RoaringIntSet result = new RoaringIntSet();
        result.ensureContainerCapacity(containerCount);
        result.containerCount = containerCount;
        result.cardinality = cardinality;
        System.arraycopy(highKeys, 0, result.highKeys, 0, containerCount);
        System.arraycopy(types, 0, result.types, 0, containerCount);
        System.arraycopy(cardinalities, 0, result.cardinalities, 0, containerCount);
        for (int i = 0; i < containerCount; i++) {
            result.containers[i] = types[i] == ARRAY
                    ? ((char[]) containers[i]).clone()
                    : ((long[]) containers[i]).clone();
        }
        return result;
    }

    private void insertContainer(int index, int high) {
        ensureContainerCapacity(containerCount + 1);
        int moved = containerCount - index;
        if (moved > 0) {
            System.arraycopy(highKeys, index, highKeys, index + 1, moved);
            System.arraycopy(types, index, types, index + 1, moved);
            System.arraycopy(containers, index, containers, index + 1, moved);
            System.arraycopy(cardinalities, index, cardinalities, index + 1, moved);
        }
        highKeys[index] = high;
        types[index] = ARRAY;
        containers[index] = new char[4];
        cardinalities[index] = 0;
        containerCount++;
    }

    private void removeContainer(int index) {
        int moved = containerCount - index - 1;
        if (moved > 0) {
            System.arraycopy(highKeys, index + 1, highKeys, index, moved);
            System.arraycopy(types, index + 1, types, index, moved);
            System.arraycopy(containers, index + 1, containers, index, moved);
            System.arraycopy(cardinalities, index + 1, cardinalities, index, moved);
        }
        int last = --containerCount;
        highKeys[last] = 0;
        types[last] = 0;
        containers[last] = null;
        cardinalities[last] = 0;
    }

    private boolean addArray(int index, char low) {
        char[] values = (char[]) containers[index];
        int count = cardinalities[index];
        int position = Arrays.binarySearch(values, 0, count, low);
        if (position >= 0) return false;
        position = -position - 1;
        if (count == values.length) {
            int next = Math.min(ARRAY_TO_BITMAP + 1, values.length + (values.length >>> 1) + 1);
            values = Arrays.copyOf(values, next);
            containers[index] = values;
        }
        System.arraycopy(values, position, values, position + 1, count - position);
        values[position] = low;
        cardinalities[index] = count + 1;
        if (count + 1 > ARRAY_TO_BITMAP) promoteToBitmap(index);
        return true;
    }

    private boolean removeArray(int index, char low) {
        char[] values = (char[]) containers[index];
        int count = cardinalities[index];
        int position = Arrays.binarySearch(values, 0, count, low);
        if (position < 0) return false;
        int moved = count - position - 1;
        if (moved > 0) System.arraycopy(values, position + 1, values, position, moved);
        values[count - 1] = 0;
        cardinalities[index] = count - 1;
        return true;
    }

    private boolean addBitmap(int index, char low) {
        long[] words = (long[]) containers[index];
        int ordinal = low;
        int word = ordinal >>> 6;
        long mask = 1L << (ordinal & 63);
        if ((words[word] & mask) != 0L) return false;
        words[word] |= mask;
        cardinalities[index]++;
        return true;
    }

    private boolean removeBitmap(int index, char low) {
        long[] words = (long[]) containers[index];
        int ordinal = low;
        int word = ordinal >>> 6;
        long mask = 1L << (ordinal & 63);
        if ((words[word] & mask) == 0L) return false;
        words[word] &= ~mask;
        int count = --cardinalities[index];
        if (count <= BITMAP_TO_ARRAY) demoteToArray(index);
        return true;
    }

    private void promoteToBitmap(int index) {
        char[] values = (char[]) containers[index];
        long[] words = new long[BITMAP_WORDS];
        int count = cardinalities[index];
        for (int i = 0; i < count; i++) {
            int ordinal = values[i];
            words[ordinal >>> 6] |= 1L << (ordinal & 63);
        }
        containers[index] = words;
        types[index] = BITMAP;
    }

    private void demoteToArray(int index) {
        long[] words = (long[]) containers[index];
        int count = cardinalities[index];
        char[] values = new char[Math.max(4, count)];
        int out = 0;
        for (int wordIndex = 0; wordIndex < BITMAP_WORDS; wordIndex++) {
            long word = words[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                values[out++] = (char) ((wordIndex << 6) | bit);
                word &= word - 1L;
            }
        }
        containers[index] = values;
        types[index] = ARRAY;
    }

    private void ensureContainerCapacity(int required) {
        if (required <= highKeys.length) return;
        int next = highKeys.length + (highKeys.length >>> 1) + 1;
        if (next < required) next = required;
        highKeys = Arrays.copyOf(highKeys, next);
        types = Arrays.copyOf(types, next);
        containers = Arrays.copyOf(containers, next);
        cardinalities = Arrays.copyOf(cardinalities, next);
    }
}
