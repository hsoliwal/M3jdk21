// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

/** Differential and adversarial tests without a third-party test/runtime owner. */
public final class M3SelectionBitsSelfTest {
    private static int checks;
    private M3SelectionBitsSelfTest() { }
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }
    private static void failIf(boolean error) {
        checks++;
        if (error) throw new AssertionError("mask/source alias or ordering violation");
    }
    private static void expectedFailure(Runnable runnable, Class<? extends Throwable> type) {
        checks++;
        try { runnable.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong failure " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
    private static void boundary() {
        for (int n : new int[] {0, 1, 2, 31, 32, 63, 64, 65, 127, 128, 129, 191, 192, 193, 1025}) {
            M3SelectionBits bits = new M3SelectionBits(n);
            Object[] source = new Object[n];
            for (int i = 0; i < n; i++) source[i] = i % 9 == 0 ? null : Integer.valueOf(i);
            var expected = new ArrayList<Object>();
            for (int i = 0; i < n; i++) {
                if (i % 3 == 0 || i % 7 == 0) {
                    bits.mark(i); bits.mark(i);
                    expected.add(source[i]);
                }
            }
            eq(expected.size(), bits.count());
            eq(expected, Arrays.asList(bits.compact(source)));
            for (int i = 0; i < n; i++) eq(i % 3 == 0 || i % 7 == 0, bits.contains(i));
            expectedFailure(() -> bits.contains(n), IndexOutOfBoundsException.class);
            expectedFailure(() -> bits.mark(-1), IndexOutOfBoundsException.class);
        }
        expectedFailure(() -> new M3SelectionBits(-1), IllegalArgumentException.class);
        expectedFailure(() -> new M3SelectionBits(7).compact(new Object[5]), IllegalArgumentException.class);
    }
    private static void parallelSameWord() {
        // 32 independent writers race over two packed words 400 times; lost
        // read-modify-write updates would make the expected cardinality fail.
        for (int repeat = 0; repeat < 400; repeat++) {
            M3SelectionBits bits = new M3SelectionBits(128);
            int workers = 32;
            CountDownLatch start = new CountDownLatch(1);
            Thread[] ts = new Thread[workers];
            AtomicReference<Throwable> first = new AtomicReference<>();
            for (int i = 0; i < workers; i++) {
                int row = i * 2 + ((i & 1) == 0 ? 0 : 64);
                // The final index is bounded and every writer has a distinct bit.
                row = (i % 16) + (i / 16) * 64;
                int selected = row;
                ts[i] = new Thread(() -> {
                    try { start.await(); bits.mark(selected); }
                    catch (Throwable e) { first.compareAndSet(null, e); }
                });
                ts[i].start();
            }
            start.countDown();
            for (Thread t : ts) {
                try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            }
            if (first.get() != null) throw new AssertionError(first.get());
            eq(workers, bits.count());
            for (int row=0; row <128; row++) eq((row < 16 || (row>=64 && row<80)), bits.contains(row));
        }
    }
    private static void differential() {
        Random random = new Random(20261017L);
        for (int trial = 0; trial < 260; trial++) {
            int n = 1 + random.nextInt(2500);
            List<Integer> source = new ArrayList<>(n);
            for (int i=0; i<n; i++) source.add(random.nextInt());
            int groupCount = 1 + random.nextInt(80);
            int divisor = 2 + random.nextInt(27);
            var keys = M3Parallel.by(source, x -> Math.floorMod(x, groupCount));
            List<Integer> expected = source.stream().filter(v -> Math.floorMod(v, divisor)==0).toList();
            List<Integer> actual = keys.filter(v -> Math.floorMod(v, divisor)==0);
            eq(expected, actual);
            expectedFailure(() -> actual.add(1), UnsupportedOperationException.class);
        }
    }
    private static void cancelledAndEmpty() {
        var empty = M3Parallel.by(List.<Integer>of(), x -> x);
        eq(List.of(), empty.filter(x -> true));
        var keyed = M3Parallel.by(List.of(0,1,2,3,4,5), x -> x % 3);
        eq(List.of(0,2,4), keyed.filter(x -> (x & 1) == 0));
        expectedFailure(() -> keyed.filter(x -> {throw new IllegalArgumentException("predicate");}), IllegalArgumentException.class);
        eq(List.of(0,2,4), keyed.filter(x -> (x & 1) == 0));
    }
    public static void main(String[] args) {
        boundary(); parallelSameWord(); differential(); cancelledAndEmpty();
        System.out.println("PASS M3_SELECTION_BITS checks="+checks+" randomTrials=260 concurrentRounds=400");
    }
}
