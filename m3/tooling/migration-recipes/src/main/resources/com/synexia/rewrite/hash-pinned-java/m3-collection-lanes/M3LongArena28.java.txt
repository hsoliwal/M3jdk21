// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Append-only primitive-long arena with stable int offsets and no whole-arena copy on growth.
 *
 * <p>Storage grows by lazy 4,096-value blocks inside the 8/8/12 segmented address space. This is
 * useful for token arenas, CSR payloads, byte-offset companions and other append-heavy packed data.
 */
public final class M3LongArena28 {
    /** Creates an empty instance with no populated storage. */
    public M3LongArena28() {}

    private final M3LongLane28 values = new M3LongLane28();
    private int size;

    public int append(long value) {
        if (size == M3Address28.MAX_SLOTS) {
            throw new IllegalStateException("segmented arena exhausted");
        }
        int index = size++;
        values.set(index, value);
        return index;
    }

    public int appendAll(long[] source) {
        Objects.requireNonNull(source, "source");
        int start = size;
        if ((long) size + source.length > M3Address28.MAX_SLOTS) {
            throw new IllegalStateException("segmented arena exhausted");
        }
        for (long value : source) {
            values.set(size++, value);
        }
        return start;
    }

    public long get(int index) {
        Objects.checkIndex(index, size);
        return values.get(index);
    }

    public void set(int index, long value) {
        Objects.checkIndex(index, size);
        values.set(index, value);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int allocatedBlockCount() {
        return values.allocatedBlockCount();
    }

    public long payloadBytes() {
        return values.payloadBytes();
    }

    public long[] copyRange(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size);
        long[] result = new long[toExclusive - fromInclusive];
        values.copyTo(fromInclusive, result, 0, result.length);
        return result;
    }

    public void forEach(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) {
            consumer.accept(values.get(i));
        }
    }
}
