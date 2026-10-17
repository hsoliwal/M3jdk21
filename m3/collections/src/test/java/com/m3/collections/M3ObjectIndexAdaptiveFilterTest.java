/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Target-side proof: single predicate pass, stable occurrence indexes and offsets. */
public final class M3ObjectIndexAdaptiveFilterTest {
    private static long checks;
    private M3ObjectIndexAdaptiveFilterTest() { }
    private static void check(boolean valid) {
        checks++;
        if (!valid) throw new AssertionError("adaptive filtered view check " + checks);
    }
    private static void runCases(Object[] values, int[] physical, byte[] flags) {
        var snapshot = M3ObjectIndexSnapshot.<Object>capture(values, physical, flags);
        Random random = new Random(0x4d33564cL + values.length);
        for (int repeat = 0; repeat < 90; repeat++) {
            int start = random.nextInt(values.length + 1);
            int end = start + random.nextInt(values.length - start + 1);
            var span = snapshot.subList(start, end);
            int modulus = 1 + random.nextInt(29);
            int residue = random.nextInt(modulus);
            AtomicInteger called = new AtomicInteger();
            var selected = span.filtered(value -> {
                called.incrementAndGet();
                return value == null ? residue == 0 : (Integer) value % modulus == residue;
            });
            ArrayList<Object> expected = new ArrayList<>();
            ArrayList<Integer> positions = new ArrayList<>();
            ArrayList<Integer> states = new ArrayList<>();
            for (int index = start; index < end; index++) {
                Object value = values[index];
                if (value == null ? residue == 0 : (Integer) value % modulus == residue) {
                    expected.add(value);
                    positions.add(physical[index]);
                    states.add(flags[index] & 0xff);
                }
            }
            check(called.get() == end - start);
            check(selected.size() == expected.size());
            for (int i = 0; i < expected.size(); i++) {
                check(selected.get(i) == expected.get(i));
                check(selected.physicalSlotAt(i) == positions.get(i));
                check(selected.stateAt(i) == states.get(i));
            }
            if (expected.size() == end - start) check(selected == span);
        }
    }
    public static void main(String[] args) {
        runCases(new Object[]{null, 7, 7, -5, 7, null, Integer.MIN_VALUE, 7, 17},
                new int[]{90, 3, 3, 400, 1000, 24, 43, 19, 17},
                new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        Random random = new Random(0x4d334f42L);
        Object[] values = new Object[65536];
        int[] physical = new int[values.length];
        byte[] flags = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextInt(41) == 0 ? null : random.nextInt(1001) - 500;
            physical[i] = i * 3;
            flags[i] = (byte) i;
        }
        runCases(values, physical, flags);
        var snapshot = M3ObjectIndexSnapshot.<Object>capture(values, physical, flags);
        check(snapshot.filtered(item -> true) == snapshot);
        var none = snapshot.filtered(item -> false);
        check(none.size() == 0 && none != snapshot);
        var sparse = snapshot.filtered(item -> item instanceof Integer n && n == 123);
        check(sparse.size() > 0);
        var sorted = sparse.sorted(Comparator.comparingInt(item -> (Integer) item));
        check(sorted.size() == sparse.size());
        var slice = sparse.subList(1, Math.min(11, sparse.size()));
        check(slice.get(0) == sparse.get(1));
        AtomicInteger called = new AtomicInteger();
        try {
            snapshot.filtered(item -> {
                if (called.getAndIncrement() == 509) throw new IllegalStateException("sentinel");
                return true;
            });
            throw new AssertionError("predicate should throw");
        } catch (IllegalStateException expected) {
            check("sentinel".equals(expected.getMessage()));
        }
        check(called.get() == 510);
        try {
            snapshot.filtered(null);
            throw new AssertionError("null predicate accepted");
        } catch (NullPointerException expected) {
            check(true);
        }
        System.out.println("M3ObjectIndexAdaptiveFilterTest PASS checks=" + checks);
    }
}
