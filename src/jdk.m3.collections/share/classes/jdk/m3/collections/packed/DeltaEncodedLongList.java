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
import java.util.function.LongConsumer;

/**
 * Monotonic signed-long sequence encoded as first value + unsigned varint deltas.
 *
 * <p>Large positive deltas spanning the signed range are supported through
 * two's-complement modular subtraction and unsigned varint encoding.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DeltaEncodedLongList.java}
 * blob {@code 131170707df4af00ab0b8caf7ed9b5dd424e9b06} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class DeltaEncodedLongList {
    private byte[] data;
    private int dataSize;
    private int size;
    private long first;
    private long last;

    /* Sparse primitive-only query facts: value and byte offset at every 128th item.
     * Encoded deltas remain the only canonical wire representation. */
    private static final int CHECKPOINT_SHIFT = 7;
    private static final int CHECKPOINT_MASK = (1 << CHECKPOINT_SHIFT) - 1;
    private int[] checkpointOffsets = new int[0];
    private long[] checkpointValues = new long[0];
    private int checkpointCount;

    public DeltaEncodedLongList() { this(32); }

    public DeltaEncodedLongList(int initialBytes) {
        if (initialBytes < 0) throw new IllegalArgumentException("initialBytes < 0");
        data = new byte[Math.max(1, initialBytes)];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int encodedBytes() { return dataSize + (size == 0 ? 0 : Long.BYTES); }
    public long payloadBytes() { return data.length; }

    public void add(long value) {
        if (size == 0) {
            first = value;
            last = value;
            size = 1;
            return;
        }
        if (Long.compare(value, last) < 0) {
            throw new IllegalArgumentException("values must be nondecreasing");
        }
        // The next element's index is the old size. Reserve before changing
        // encoded state so checkpoint growth failure cannot publish half a value.
        boolean checkpoint = (size & CHECKPOINT_MASK) == 0;
        if (checkpoint) ensureCheckpointCapacity(checkpointCount + 1);
        long delta = value - last;
        writeUnsignedVarLong(delta);
        last = value;
        if (checkpoint) {
            checkpointValues[checkpointCount] = value;
            checkpointOffsets[checkpointCount] = dataSize;
            checkpointCount++;
        }
        size++;
    }

    public long first() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return first;
    }

    public long last() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return last;
    }

    public long get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        if (index == 0) return first;
        int block = index >>> CHECKPOINT_SHIFT;
        long value = block == 0 ? first : checkpointValues[block - 1];
        int cursor = block == 0 ? 0 : checkpointOffsets[block - 1];
        for (int item = block << CHECKPOINT_SHIFT; item < index; item++) {
            long delta = 0L;
            int shift = 0;
            byte current;
            do {
                current = data[cursor++];
                delta |= (long) (current & 0x7f) << shift;
                shift += 7;
            } while ((current & 0x80) != 0);
            value += delta;
        }
        return value;
    }

    public void forEach(LongConsumer consumer) {
        if (size == 0) return;
        long value = first;
        consumer.accept(value);
        int cursor = 0;
        for (int item = 1; item < size; item++) {
            long delta = 0L;
            int shift = 0;
            byte current;
            do {
                current = data[cursor++];
                delta |= (long) (current & 0x7f) << shift;
                shift += 7;
            } while ((current & 0x80) != 0);
            value += delta;
            consumer.accept(value);
        }
    }

    public long[] toArray() {
        long[] result = new long[size];
        if (size == 0) return result;
        result[0] = first;
        long value = first;
        int cursor = 0;
        for (int item = 1; item < size; item++) {
            long delta = 0L;
            int shift = 0;
            byte current;
            do {
                current = data[cursor++];
                delta |= (long) (current & 0x7f) << shift;
                shift += 7;
            } while ((current & 0x80) != 0);
            value += delta;
            result[item] = value;
        }
        return result;
    }

    public void clear() {
        Arrays.fill(data, 0, dataSize, (byte) 0);
        Arrays.fill(checkpointOffsets, 0, checkpointCount, 0);
        Arrays.fill(checkpointValues, 0, checkpointCount, 0L);
        checkpointCount = 0;
        dataSize = 0;
        size = 0;
        first = 0L;
        last = 0L;
    }

    private void ensureCheckpointCapacity(int required) {
        if (required <= checkpointOffsets.length) return;
        int capacity = checkpointOffsets.length == 0 ? 8 : checkpointOffsets.length +
                (checkpointOffsets.length >>> 1) + 1;
        if (capacity < required) capacity = required;
        // The two lanes never expose references outside this class.
        int[] offsets = Arrays.copyOf(checkpointOffsets, capacity);
        long[] values = Arrays.copyOf(checkpointValues, capacity);
        checkpointOffsets = offsets;
        checkpointValues = values;
    }

    private void writeUnsignedVarLong(long value) {
        ensure(dataSize + 10);
        while ((value & ~0x7fL) != 0L) {
            data[dataSize++] = (byte) ((value & 0x7fL) | 0x80L);
            value >>>= 7;
        }
        data[dataSize++] = (byte) value;
    }

    private void ensure(int required) {
        if (required <= data.length) return;
        int capacity = data.length + (data.length >>> 1) + 1;
        if (capacity < required) capacity = required;
        data = Arrays.copyOf(data, capacity);
    }
}
