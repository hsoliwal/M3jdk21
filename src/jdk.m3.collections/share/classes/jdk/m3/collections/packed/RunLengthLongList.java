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
 * Repeated-long sequence retained as parallel runValues[] and runEnds[] lanes.
 *
 * <p>runEnds[r] is the exclusive end of run r (the prefix sum of the run counts), so
 * get(long) is an O(log runs) search and run r holds runEnds[r] - runEnds[r - 1] items.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/RunLengthLongList.java}
 * blob {@code b5eca794aa1d6234c642405e41497d1be6d818a1} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class RunLengthLongList {
    @FunctionalInterface
    public interface RunConsumer {
        void accept(long value, int count);
    }

    private long[] runValues;
    private long[] runEnds;
    private int runCount;
    private long size;

    public RunLengthLongList() { this(8); }

    public RunLengthLongList(int initialRuns) {
        if (initialRuns < 0) throw new IllegalArgumentException("initialRuns < 0");
        int capacity = Math.max(1, initialRuns);
        runValues = new long[capacity];
        runEnds = new long[capacity];
    }

    public long size() { return size; }
    public int runCount() { return runCount; }
    public boolean isEmpty() { return size == 0L; }
    public long payloadBytes() {
        // logical payload: one long value and one int count per run slot; runEnds is the
        // prefix (offset) form of those int counts, so the accounting is unchanged
        return (long) runValues.length * Long.BYTES
                + (long) runEnds.length * Integer.BYTES;
    }

    public void add(long value) {
        if (size == Long.MAX_VALUE) throw new IllegalStateException("size overflow");
        if (runCount > 0 && runValues[runCount - 1] == value
                && runLength(runCount - 1) != Integer.MAX_VALUE) {
            runEnds[runCount - 1]++;
        } else {
            appendRun(value, 1);
        }
        size++;
    }

    public void addRepeated(long value, int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        if (count == 0) return;
        if (Long.MAX_VALUE - size < count) throw new IllegalStateException("size overflow");

        int remaining = count - mergeTail(value, count);
        if (remaining > 0) appendRun(value, remaining);
        size += count;
    }

    public long get(long index) {
        if (index < 0L || index >= size) throw new IndexOutOfBoundsException(Long.toString(index));
        int run = runOf(index);
        if (run == runCount) throw new AssertionError(index);
        return runValues[run];
    }

    public void forEachRun(RunConsumer consumer) {
        for (int run = 0; run < runCount; run++) {
            consumer.accept(runValues[run], (int) runLength(run));
        }
    }

    public void forEach(LongConsumer consumer) {
        for (int run = 0; run < runCount; run++) {
            long value = runValues[run];
            int count = (int) runLength(run);
            for (int i = 0; i < count; i++) consumer.accept(value);
        }
    }

    public void clear() {
        Arrays.fill(runValues, 0, runCount, 0L);
        Arrays.fill(runEnds, 0, runCount, 0L);
        runCount = 0;
        size = 0L;
    }

    private void ensure(int required) {
        if (required <= runValues.length) return;
        int capacity = runValues.length + (runValues.length >>> 1) + 1;
        if (capacity < required) capacity = required;
        runValues = Arrays.copyOf(runValues, capacity);
        runEnds = Arrays.copyOf(runEnds, capacity);
    }

    /** OFFSET_ARRAY: exclusive start of {@code run}, i.e. the end of the run before it. */
    private long endBefore(int run) {
        return run == 0 ? 0L : runEnds[run - 1];
    }

    /** OFFSET_ARRAY: item count of {@code run} as end minus start (CSR degree). */
    private long runLength(int run) {
        return runEnds[run] - endBefore(run);
    }

    /**
     * CSR_INDEX: first run whose end exceeds {@code index}, or runCount when none does.
     * Branch-free halving (the answer stays in [base, base + n]); the ternary lowers to cmov.
     */
    private int runOf(long index) {
        int n = runCount;
        if (n == 0) return 0;
        int base = 0;
        while (n > 1) {
            int half = n >>> 1;
            base = runEnds[base + half - 1] <= index ? base + half : base;
            n -= half;
        }
        return runEnds[base] <= index ? base + 1 : base;
    }

    /** OFFSET_ARRAY: grows the tail run by up to {@code count} items; returns the merged count. */
    private int mergeTail(long value, int count) {
        if (runCount == 0 || runValues[runCount - 1] != value) return 0;
        int merged = Math.min(Integer.MAX_VALUE - (int) runLength(runCount - 1), count);
        runEnds[runCount - 1] += merged;
        return merged;
    }

    /** OFFSET_ARRAY: appends a run of {@code count} items; end = previous end + count. */
    private void appendRun(long value, int count) {
        ensure(runCount + 1);
        runValues[runCount] = value;
        runEnds[runCount] = endBefore(runCount) + count;
        runCount++;
    }
}
