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
package com.m3.collections;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Java 21 standalone API/differential/concurrency oracle; no donor implementation code. */
public final class M3ConcurrentBitSetClaimSelfTest {
    private M3ConcurrentBitSetClaimSelfTest() { }

    public static void main(String[] args) throws Exception {
        runAll();
        System.out.println("PASS: bit claiming / capacity tails / reference model / concurrency");
    }

    public static void runAll() throws Exception {
        for (int capacity : new int[] { 0, 1, 2, 31, 32, 63, 64, 65, 66, 127, 128, 129, 256, 4097 }) {
            ascendingAndReuse(capacity);
            randomModel(capacity);
        }
        parallelClaims(4097, 8);
        parallelClaims(64, 16);
    }

    private static void ascendingAndReuse(int capacity) {
        M3ConcurrentBitSet bits = new M3ConcurrentBitSet(capacity);
        check(bits.bitCapacity() == capacity, "capacity");
        for (int expected = 0; expected < capacity; expected++) {
            check(bits.claimFirstClearBit() == expected, "ascending claim at " + expected);
            check(bits.get(expected), "claimed value");
        }
        check(bits.claimFirstClearBit() == -1, "exhausted");
        check(bits.cardinalityApprox() == capacity, "all bits claimed");
        int tailBits = capacity & 63;
        if (tailBits != 0) {
            long[] words = bits.snapshotWords();
            long valid = (1L << tailBits) - 1L;
            check((words[words.length - 1] & ~valid) == 0L, "trailing unused bits");
        }
        if (capacity != 0) {
            int reused = capacity >>> 1;
            check(bits.clear(reused), "clear existing claim");
            check(bits.claimFirstClearBit() == reused, "reuse lowest clear");
            check(bits.claimFirstClearBit() == -1, "exhausted after reclaim");
        }
        bits.clearAll();
        check(bits.claimFirstClearBit() == (capacity == 0 ? -1 : 0), "clearAll releases claims");
    }

    private static void randomModel(int capacity) {
        M3ConcurrentBitSet actual = new M3ConcurrentBitSet(capacity);
        BitSet model = new BitSet(capacity);
        Random random = new Random(0x4d33424cL + capacity);
        for (int op = 0; op < 500; op++) {
            if (capacity == 0 || random.nextInt(4) == 0) {
                int expected = model.nextClearBit(0);
                if (expected >= capacity) expected = -1;
                int claimed = actual.claimFirstClearBit();
                check(claimed == expected, "reference first clear bit");
                if (claimed != -1) model.set(claimed);
            } else {
                int bit = random.nextInt(capacity);
                switch (random.nextInt(3)) {
                    case 0 -> { check(actual.set(bit) == !model.get(bit), "set result"); model.set(bit); }
                    case 1 -> { check(actual.clear(bit) == model.get(bit), "clear result"); model.clear(bit); }
                    default -> { check(actual.toggle(bit) != model.get(bit), "toggle result"); model.flip(bit); }
                }
            }
            check(actual.cardinalityApprox() == model.cardinality(), "reference cardinality");
            check(actual.setBitStream().toArray().length == model.cardinality(), "stream count");
        }
    }

    private static void parallelClaims(int capacity, int workers) throws Exception {
        M3ConcurrentBitSet bits = new M3ConcurrentBitSet(capacity);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Integer>>> results = new ArrayList<>();
        try {
            for (int thread = 0; thread < workers; thread++) {
                Callable<List<Integer>> work = () -> {
                    start.await();
                    List<Integer> claimed = new ArrayList<>();
                    int index;
                    while ((index = bits.claimFirstClearBit()) >= 0) claimed.add(index);
                    return claimed;
                };
                results.add(pool.submit(work));
            }
            start.countDown();
            Set<Integer> unique = new HashSet<>();
            for (Future<List<Integer>> completed : results) {
                for (int index : completed.get(20, TimeUnit.SECONDS)) {
                    check(index >= 0 && index < capacity, "out of range concurrent claim");
                    check(unique.add(index), "duplicate claimed bit");
                }
            }
            check(unique.size() == capacity, "concurrent claims complete");
            check(bits.cardinalityApprox() == capacity, "concurrent cardinality");
            check(bits.parallelSetBitStream().count() == capacity, "concurrent stream order count");
        } finally {
            pool.shutdownNow();
        }
    }

    private static void check(boolean outcome, String reason) {
        if (!outcome) throw new AssertionError(reason);
    }
}
