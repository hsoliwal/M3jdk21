/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Independent JDK HashSet differential and growth-boundary regression for the
 * original primitive owner. No test oracle code enters the production runtime.
 */
public final class IntHashSetGrowthSelfTest {
    private static long checks;
    private IntHashSetGrowthSelfTest() { }

    public static void main(String[] args) {
        growthAndZero();
        randomizedMembership();
        failFastAndDuplicates();
        System.out.println("PASS IntHashSetGrowthSelfTest checks=" + checks);
    }

    private static void growthAndZero() {
        IntHashSet set = new IntHashSet();
        Set<Integer> oracle = new HashSet<>();
        same(8, set.capacity(), "initial power-of-two capacity");
        for (int key : new int[] {0, Integer.MIN_VALUE, Integer.MAX_VALUE,
                17, -17, 1, 2, 3, 4, -1, 0, Integer.MIN_VALUE}) {
            check(set.add(key) == oracle.add(key), "insert/duplicate truth");
            verify(set, oracle);
        }
        check(set.capacity() >= 16, "sixth nonzero insertion grows before publishing");
        int originalCapacity = set.capacity();
        for (int key : new int[] {17, -17, 1, 2, 3, 4, -1}) {
            check(!set.add(key), "duplicate must not enlarge the table");
            same(originalCapacity, set.capacity(), "duplicate growth refusal");
        }
        check(set.remove(0), "remove zero marker");
        check(!set.contains(0), "removed zero absent");
        check(set.add(0), "re-add separate zero marker");
        check(set.contains(0), "zero present again");
        check(set.remove(Integer.MIN_VALUE), "remove signed minimum");
        check(set.add(Integer.MIN_VALUE), "re-add signed minimum");
        int[] detached = set.toArray();
        Arrays.fill(detached, 777);
        check(set.contains(17) && set.contains(-1), "detached toArray");
        expect(IllegalArgumentException.class, () -> new IntHashSet(-1));
        expect(IllegalArgumentException.class,
                () -> new IntHashSet(Integer.MAX_VALUE));
    }

    private static void randomizedMembership() {
        Random random = new Random(0x4D33484153485345L);
        IntHashSet set = new IntHashSet();
        Set<Integer> oracle = new HashSet<>();
        for (int step = 0; step < 65_000; step++) {
            int key = switch (step % 13) {
                case 0 -> 0;
                case 1 -> Integer.MIN_VALUE;
                case 2 -> Integer.MAX_VALUE;
                case 3 -> step;
                case 4 -> -step;
                case 5 -> random.nextInt(512) - 256;
                default -> random.nextInt();
            };
            switch (step % 7) {
                case 0, 1, 4 -> check(set.add(key) == oracle.add(key), "random add");
                case 2, 5 -> check(set.remove(key) == oracle.remove(key), "random remove");
                case 3 -> check(set.contains(key) == oracle.contains(key), "random contains");
                default -> {
                    set.clear();
                    oracle.clear();
                }
            }
            if (step % 31 == 0) verify(set, oracle);
        }
        set.clear();
        oracle.clear();
        for (int i = -16_000; i < 16_000; i++) {
            check(set.add(i), "bulk distinct insertion " + i);
            oracle.add(i);
        }
        verify(set, oracle);
        for (int i = 15_999; i >= -16_000; i -= 3) {
            check(set.remove(i) == oracle.remove(i), "reverse removal");
        }
        verify(set, oracle);
    }

    private static void failFastAndDuplicates() {
        IntHashSet set = new IntHashSet();
        for (int i = 0; i < 5; i++) set.add(i + 1);
        IntIterator iterator = set.iterator();
        check(iterator.hasNext(), "iterator has elements");
        check(!set.add(1), "duplicate set membership");
        check(iterator.hasNext(), "duplicate is not structural");
        check(set.add(6), "growth-triggering insertion");
        expect(ConcurrentModificationException.class, iterator::hasNext);
        IntIterator afterGrowth = set.iterator();
        check(afterGrowth.hasNext(), "new iterator valid after growth");
        check(set.remove(6), "remove after growth");
        expect(ConcurrentModificationException.class, afterGrowth::nextInt);
    }

    private static void verify(IntHashSet set, Set<Integer> oracle) {
        same(oracle.size(), set.size(), "size parity");
        int[] exported = set.toArray();
        same(set.size(), exported.length, "toArray exact length");
        Set<Integer> seen = new HashSet<>();
        for (int key : exported) {
            check(seen.add(key), "toArray must not duplicate keys");
            check(oracle.contains(key), "exported key in oracle");
            check(set.contains(key), "exported key present");
        }
        check(seen.equals(oracle), "complete exported membership");
        Set<Integer> viaIterator = new HashSet<>();
        IntIterator iterator = set.iterator();
        while (iterator.hasNext()) {
            int next = iterator.nextInt();
            check(viaIterator.add(next), "iterator duplicate");
        }
        check(viaIterator.equals(oracle), "iterator membership");
        check((set.capacity() & (set.capacity() - 1)) == 0, "power-of-two capacity");
        check(set.payloadBytes() == (long) set.capacity() * Integer.BYTES,
                "primitive retained payload");
    }

    private static void expect(Class<? extends Throwable> type, Runnable body) {
        checks++;
        try {
            body.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("unexpected exception " + thrown, thrown);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void same(long expected, long actual, String reason) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(reason + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason + " check=" + checks);
    }
}
