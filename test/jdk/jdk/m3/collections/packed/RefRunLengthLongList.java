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
import java.util.function.LongConsumer;

/**
 * RunLengthLongList as on master before the run-end offset lane, renamed: the reference of
 * RunLengthPrefixTest; test only.
 */
final class RefRunLengthLongList {
    @FunctionalInterface
    public interface RunConsumer {
        void accept(long value, int count);
    }

    private long[] runValues;
    private int[] runCounts;
    private int runCount;
    private long size;

    public RefRunLengthLongList() { this(8); }

    public RefRunLengthLongList(int initialRuns) {
        if (initialRuns < 0) throw new IllegalArgumentException("initialRuns < 0");
        int capacity = Math.max(1, initialRuns);
        runValues = new long[capacity];
        runCounts = new int[capacity];
    }

    public long size() { return size; }
    public int runCount() { return runCount; }
    public boolean isEmpty() { return size == 0L; }
    public long payloadBytes() {
        return (long) runValues.length * Long.BYTES
                + (long) runCounts.length * Integer.BYTES;
    }

    public void add(long value) {
        if (size == Long.MAX_VALUE) throw new IllegalStateException("size overflow");
        if (runCount > 0 && runValues[runCount - 1] == value
                && runCounts[runCount - 1] != Integer.MAX_VALUE) {
            runCounts[runCount - 1]++;
        } else {
            ensure(runCount + 1);
            runValues[runCount] = value;
            runCounts[runCount] = 1;
            runCount++;
        }
        size++;
    }

    public void addRepeated(long value, int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        if (count == 0) return;
        if (Long.MAX_VALUE - size < count) throw new IllegalStateException("size overflow");

        int remaining = count;
        if (runCount > 0 && runValues[runCount - 1] == value) {
            int available = Integer.MAX_VALUE - runCounts[runCount - 1];
            int merged = Math.min(available, remaining);
            runCounts[runCount - 1] += merged;
            remaining -= merged;
        }
        while (remaining > 0) {
            ensure(runCount + 1);
            int chunk = remaining;
            runValues[runCount] = value;
            runCounts[runCount] = chunk;
            runCount++;
            remaining -= chunk;
        }
        size += count;
    }

    public long get(long index) {
        if (index < 0L || index >= size) throw new IndexOutOfBoundsException(Long.toString(index));
        long offset = index;
        for (int run = 0; run < runCount; run++) {
            int count = runCounts[run];
            if (offset < count) return runValues[run];
            offset -= count;
        }
        throw new AssertionError(index);
    }

    public void forEachRun(RunConsumer consumer) {
        for (int run = 0; run < runCount; run++) {
            consumer.accept(runValues[run], runCounts[run]);
        }
    }

    public void forEach(LongConsumer consumer) {
        for (int run = 0; run < runCount; run++) {
            long value = runValues[run];
            int count = runCounts[run];
            for (int i = 0; i < count; i++) consumer.accept(value);
        }
    }

    public void clear() {
        Arrays.fill(runValues, 0, runCount, 0L);
        Arrays.fill(runCounts, 0, runCount, 0);
        runCount = 0;
        size = 0L;
    }

    private void ensure(int required) {
        if (required <= runValues.length) return;
        int capacity = runValues.length + (runValues.length >>> 1) + 1;
        if (capacity < required) capacity = required;
        runValues = Arrays.copyOf(runValues, capacity);
        runCounts = Arrays.copyOf(runCounts, capacity);
    }
}
