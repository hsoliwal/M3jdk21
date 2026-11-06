/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/** Deterministic independent union/connected/component oracle for the unchanged public API. */
public final class M3IntDisjointSetSelfTest {
    private static long assertions;
    private static long operations;
    private static MessageDigest digest;
    private static final byte[] LANE = new byte[Long.BYTES];
    private static final int[] SIZES = {0, 1, 2, 3, 5, 16, 31, 127, 256, 1024, 4096};

    private M3IntDisjointSetSelfTest() { }

    private static void observed(long value) {
        for (int i = 0; i < Long.BYTES; i++) {
            LANE[i] = (byte) (value >>> (i * Byte.SIZE));
        }
        digest.update(LANE);
    }

    private static void eq(long expected, long actual) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError("comparison " + assertions + " expected=" + expected + " got=" + actual);
        }
        observed(actual);
    }

    private static void ok(boolean good) {
        assertions++;
        if (!good) throw new AssertionError("comparison " + assertions);
    }

    private static void invalid(Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (IndexOutOfBoundsException expected) {
            observed(0x198defL);
            return;
        }
        throw new AssertionError("invalid index admitted");
    }

    private static final class Oracle {
        final int[] group;
        int components;
        Oracle(int n) {
            group = new int[n];
            reset();
        }
        void reset() {
            for (int i = 0; i < group.length; i++) group[i] = i;
            components = group.length;
        }
        boolean union(int left, int right) {
            int g1 = group[left];
            int g2 = group[right];
            if (g1 == g2) return false;
            for (int i = 0; i < group.length; i++) {
                if (group[i] == g2) group[i] = g1;
            }
            components--;
            return true;
        }
        int size(int at) {
            int label = group[at];
            int count = 0;
            for (int id : group) if (id == label) count++;
            return count;
        }
        boolean connected(int a, int b) { return group[a] == group[b]; }
    }

    private static void runSize(int size, long seed) {
        M3IntDisjointSet actual = new M3IntDisjointSet(size);
        Oracle reference = new Oracle(size);
        Random random = new Random(seed);
        eq(size, actual.size());
        eq(size, actual.components());
        eq(size * (long) Integer.BYTES * 2L, actual.payloadBytes());
        invalid(() -> actual.find(-1));
        invalid(() -> actual.find(size));
        invalid(() -> actual.connected(-1, size));
        if (size == 0) {
            invalid(() -> actual.componentSize(0));
            return;
        }
        int count = 50_000;
        for (int step = 0; step < count; step++) {
            int a = random.nextInt(size);
            int b = random.nextInt(size);
            int kind = random.nextInt(100);
            if (kind < 6) {
                actual.reset();
                reference.reset();
                eq(size, actual.components());
            } else if (kind < 39) {
                boolean expected = reference.union(a, b);
                boolean found = actual.union(a, b);
                eq(expected ? 1 : 0, found ? 1 : 0);
                eq(reference.components, actual.components());
            } else if (kind < 66) {
                eq(reference.connected(a, b) ? 1 : 0, actual.connected(a, b) ? 1 : 0);
            } else if (kind < 85) {
                eq(reference.size(a), actual.componentSize(a));
            } else {
                int root = actual.find(a);
                ok(root >= 0 && root < size);
                eq(root, actual.find(root));
                observed(root); // Original union-by-size must retain identical representative.
            }
            operations++;
            if ((step & 255) == 0) {
                eq(reference.components, actual.components());
                int count1 = reference.size(a);
                eq(count1, actual.componentSize(a));
                eq(reference.connected(a, b) ? 1 : 0, actual.connected(a, b) ? 1 : 0);
            }
        }
        actual.reset();
        reference.reset();
        eq(size, actual.components());
        for (int at = 0; at < size; at++) {
            eq(at, actual.find(at));
            eq(1, actual.componentSize(at));
        }
    }

    private static void adversarial() {
        // Large balanced unions create actual maximum-height union-by-size trees.
        final int n = 1 << 14;
        M3IntDisjointSet actual = new M3IntDisjointSet(n);
        for (int step = 1; step < n; step <<= 1) {
            for (int i = 0; i < n; i += step << 1) {
                ok(actual.union(i, i + step));
            }
        }
        eq(1, actual.components());
        for (int i = n - 1; i >= 0; i--) {
            eq(0, actual.find(i));
            eq(n, actual.componentSize(i));
        }
        for (int i = 0; i < n; i++) {
            eq(0, actual.find(i));
        }
        actual.reset();
        eq(n, actual.components());
        for (int i = 0; i < n; i++) eq(i, actual.find(i));
    }

    public static String run() {
        assertions = 0L;
        operations = 0L;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        for (int n : SIZES) runSize(n, 0x4d33445355483121L ^ n * 97L);
        adversarial();
        assertions++;
        if (operations != (SIZES.length - 1L) * 50_000L) {
            throw new AssertionError("missing operations");
        }
        return "checks=" + assertions + ",operations=" + operations + ",sha256="
                + HexFormat.of().formatHex(digest.digest());
    }

    public static void main(String[] args) {
        System.out.println("M3IntDisjointSetSelfTest PASS " + run());
    }
}
