// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.LongPredicate;

/** Executable Java 21 contract oracle for parent-owned page slots and packed keep bits. */
public final class M3LongTreeSetMaskCompactionContract {
    private static final long SEED = 0x4D33534F52544CL;
    private static final int[] SIZES = {0, 1, 2, 3, 4, 7, 8, 16, 31, 32, 63, 64, 65, 128, 129, 257};
    private static final int[] CAPACITIES = {4, 8, 16, 64};

    private M3LongTreeSetMaskCompactionContract() { }

    public static void main(String[] args) {
        run();
    }

    /** Repeatable fixed-seed owner/oracle comparison. No native library is required. */
    public static void run() {
        Random random = new Random(SEED);
        int scenarios = 0;
        for (int capacity : CAPACITIES) {
            for (int length : SIZES) {
                for (int run = 0; run < 20; run++) {
                    verifyScenario(random, capacity, length, run);
                    scenarios++;
                }
            }
        }
        if (scenarios != 1280) throw new AssertionError("incomplete scenario matrix: " + scenarios);
        System.out.println("M3_LONG_TREE_MASK_COMPACTION_PASS cases=" + scenarios);
    }

    private static void verifyScenario(Random random, int capacity, int length, int run) {
        M3LongTreeSet owner = new M3LongTreeSet(0, capacity);
        long[] values = inputs(random, length);
        int[] order = new int[length];
        for (int i = 0; i < length; i++) order[i] = i;
        for (int i = length - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            int previous = order[i];
            order[i] = order[swap];
            order[swap] = previous;
        }
        for (int index : order) require(owner.add(values[index]), "duplicate input");
        long[] before = owner.toArray();
        require(Arrays.equals(values, before), "insertion order changes sorted set");
        int beforePages = owner.pageCount();
        int beforeCapacity = owner.capacity();
        long beforeBytes = owner.payloadBytes();
        int stop = length == 0 ? 0 : random.nextInt(length + 1);
        int policy = run % 6;
        boolean poison = stop < length;
        long[] expected = expected(before, stop, policy);
        int[] visited = {0};
        M3LongIterator stale = owner.iterator();
        LongPredicate predicate = value -> {
            require(Arrays.equals(before, owner.toArray()), "predicate observed premature compaction");
            require(owner.pageCount() == beforePages && owner.capacity() == beforeCapacity
                    && owner.payloadBytes() == beforeBytes, "predicate observed metadata mutation");
            int ordinal = visited[0];
            if (poison && ordinal == stop) throw new ProbeFailure();
            require(ordinal < before.length && before[ordinal] == value,
                    "nonsequential predicate callback or repeated source value");
            visited[0]++;
            return keep(policy, ordinal, value);
        };
        boolean thrown = false;
        int removed = -1;
        try {
            removed = owner.filterInPlace(predicate);
        } catch (ProbeFailure expectedFailure) {
            thrown = true;
        }
        require(thrown == poison, "poison propagation");
        require(visited[0] == stop, "unexpected callback count");
        if (!poison) require(removed == length - expected.length, "removed count");
        require(Arrays.equals(owner.toArray(), expected), "sorted kept values mismatch");
        require(owner.size() == expected.length, "logical size mismatch");
        require(owner.pageCount() <= beforePages, "compaction grew page count");
        require(owner.capacity() == beforeCapacity && owner.payloadBytes() == beforeBytes,
                "compaction allocated new owner pages");
        if (expected.length < before.length) {
            expectConcurrentModification(stale);
        } else {
            require(!stale.hasNext() == (before.length == 0), "unchanged iterator invalidated");
        }
        TreeSet<Long> oracle = new TreeSet<>();
        for (long value : expected) oracle.add(value);
        checkNavigable(owner, oracle, before);
        for (int operation = 0; operation < 12; operation++) {
            long value = operation < before.length ? before[operation] : random.nextLong();
            if ((operation & 1) == 0) require(owner.add(value) == oracle.add(value), "add mismatch");
            else require(owner.remove(value) == oracle.remove(value), "remove mismatch");
            checkNavigable(owner, oracle, before);
        }
        owner.trimToSize();
        checkNavigable(owner, oracle, before);
    }

    private static long[] inputs(Random random, int length) {
        TreeSet<Long> unique = new TreeSet<>();
        if (length > 0) unique.add(Long.MIN_VALUE);
        if (length > 1) unique.add(Long.MAX_VALUE);
        if (length > 2) unique.add(0L);
        while (unique.size() < length) unique.add(random.nextLong());
        long[] result = new long[length];
        int at = 0;
        for (long value : unique) result[at++] = value;
        return result;
    }

    private static long[] expected(long[] before, int stop, int policy) {
        long[] selected = new long[before.length];
        int kept = 0;
        for (int i = 0; i < before.length; i++) {
            if (i >= stop || keep(policy, i, before[i])) selected[kept++] = before[i];
        }
        return Arrays.copyOf(selected, kept);
    }

    private static boolean keep(int policy, int ordinal, long value) {
        return switch (policy) {
            case 0 -> true;
            case 1 -> false;
            case 2 -> (ordinal & 1) == 0;
            case 3 -> ordinal % 3 != 0;
            case 4 -> (value & 7L) != 0L;
            default -> (value < 0L) || ordinal % 5 == 0;
        };
    }

    private static void checkNavigable(M3LongTreeSet owner, TreeSet<Long> oracle, long[] before) {
        require(Arrays.equals(owner.toArray(), oracle.stream().mapToLong(Long::longValue).toArray()),
                "owner/oracle array mismatch");
        if (oracle.isEmpty()) return;
        require(owner.first() == oracle.first() && owner.last() == oracle.last(), "endpoint mismatch");
        long[] probes = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, -1L, 1L,
                before.length == 0 ? 0L : before[before.length / 2]};
        for (long pivot : probes) {
            long fallback = 17L;
            require(owner.floor(pivot, fallback) == (oracle.floor(pivot) == null ? fallback : oracle.floor(pivot)),
                    "floor mismatch");
            require(owner.ceiling(pivot, fallback) == (oracle.ceiling(pivot) == null ? fallback : oracle.ceiling(pivot)),
                    "ceiling mismatch");
            require(owner.lower(pivot, fallback) == (oracle.lower(pivot) == null ? fallback : oracle.lower(pivot)),
                    "lower mismatch");
            require(owner.higher(pivot, fallback) == (oracle.higher(pivot) == null ? fallback : oracle.higher(pivot)),
                    "higher mismatch");
        }
    }

    private static void expectConcurrentModification(M3LongIterator cursor) {
        try {
            cursor.hasNext();
            throw new AssertionError("structural compaction should invalidate old iterator");
        } catch (ConcurrentModificationException expected) {
            // Structural page compaction changes precisely one iterator epoch.
        }
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    private static final class ProbeFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}