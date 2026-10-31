/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Deterministic independent graph-connectivity oracle for primitive array DSU. */
public final class M3DisjointIndexSelfTest {
    private static long checks;
    private M3DisjointIndexSelfTest() { }

    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && "--native".equals(args[0]);
        if (args.length > 1 || (args.length == 1 && !nativeRun))
            throw new IllegalArgumentException("--native or no arguments");
        if (nativeRun && !M3DisjointIndex.nativeAvailable())
            throw new AssertionError("real JNI disjoint library required");
        boundaryChecks(nativeRun);
        graphOracle(nativeRun);
        System.out.println("PASS M3DisjointIndexSelfTest checks=" + checks + " native=" + nativeRun);
    }

    private static void boundaryChecks(boolean nativeRun) {
        M3DisjointIndex d = new M3DisjointIndex();
        eq(0, d.size()); eq(0, d.componentCount());
        eq(0, d.add(0)); eq(1, d.add(Integer.MIN_VALUE));
        eq(2, d.size()); eq(2, d.componentCount());
        eq(0, d.find(0)); eq(1, d.find(1));
        eq(0, d.stateAt(0)); eq(Integer.MIN_VALUE, d.stateAt(1));
        eq(0, d.updateState(0, Integer.MIN_VALUE | 4, 4));
        eq(Integer.MIN_VALUE, d.stateAt(0));
        long before = d.revision();
        eq(Integer.MIN_VALUE, d.updateState(0, 0, 0));
        eq(before, d.revision());
        eq(true, d.union(0, 1));
        eq(false, d.union(1, 0));
        eq(1, d.componentCount()); eq(2, d.componentSize(0));
        eq(true, d.connected(0, 1));
        eq(new int[] {0}, d.selectedRoots(Integer.MIN_VALUE, 0));
        eq(new int[0], d.selectedRoots(Integer.MIN_VALUE, Integer.MIN_VALUE));
        eq(new boolean[] {true, true, true}, d.connectedBatch(
                new int[] {0, 0, 1}, new int[] {1, 0, 0}, M3DisjointIndex.Backend.JAVA));
        if (nativeRun) eq(new boolean[] {true, true, true}, d.connectedBatch(
                new int[] {0, 0, 1}, new int[] {1, 0, 0}, M3DisjointIndex.Backend.NATIVE_REQUIRED));
        long revision = d.revision();
        fails(IndexOutOfBoundsException.class, () -> d.union(0, 2));
        fails(IndexOutOfBoundsException.class, () -> d.union(-1, 0));
        fails(IndexOutOfBoundsException.class, () -> d.connectedBatch(
                new int[] {1, 2}, new int[] {0, 0}, M3DisjointIndex.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> d.connectedBatch(
                new int[] {0}, new int[] {0, 1}, M3DisjointIndex.Backend.JAVA));
        fails(NullPointerException.class, () -> d.connectedBatch(
                null, new int[0], M3DisjointIndex.Backend.JAVA));
        fails(NullPointerException.class, () -> d.connectedBatch(
                new int[0], new int[0], null));
        fails(IllegalArgumentException.class, () -> new M3DisjointIndex(-1));
        fails(IllegalArgumentException.class, () -> d.ensureCapacity(-1));
        eq(revision, d.revision()); eq(2, d.size()); eq(1, d.componentCount());
        eq(new boolean[0], d.connectedBatch(new int[0], new int[0],
                M3DisjointIndex.Backend.JAVA));
        if (nativeRun) eq(new boolean[0], d.connectedBatch(new int[0], new int[0],
                M3DisjointIndex.Backend.NATIVE_REQUIRED));
    }

    private static void graphOracle(boolean nativeRun) {
        final int n = 72;
        M3DisjointIndex d = new M3DisjointIndex(1);
        boolean[][] edges = new boolean[n][n];
        int[] states = new int[n];
        for (int i = 0; i < n; i++) {
            eq(i, d.add(0));
            edges[i][i] = true;
        }
        Random random = new Random(0x4d33445355323032L);
        for (int t = 0; t < 4_096; t++) {
            int a = random.nextInt(n), b = random.nextInt(n);
            int operation = random.nextInt(5);
            if (operation < 2) {
                boolean wasConnected = graphConnected(edges, a, b);
                long before = d.revision();
                eq(!wasConnected, d.union(a, b));
                if (!wasConnected) {
                    edges[a][b] = true;
                    edges[b][a] = true;
                    eq(before + 1, d.revision());
                } else eq(before, d.revision());
            } else if (operation == 2) {
                int set = random.nextInt(), clear = random.nextInt();
                int old = states[a];
                eq(old, d.updateState(a, set, clear));
                states[a] = (old | set) & ~clear;
            } else {
                eq(graphConnected(edges, a, b), d.connected(a, b));
                eq(graphSize(edges, a), d.componentSize(a));
            }
            if ((t & 31) == 0) {
                int[] left = new int[40], right = new int[40];
                boolean[] expected = new boolean[40];
                for (int j = 0; j < left.length; j++) {
                    left[j] = random.nextInt(n);
                    right[j] = random.nextInt(n);
                    expected[j] = graphConnected(edges, left[j], right[j]);
                }
                eq(expected, d.connectedBatch(left, right, M3DisjointIndex.Backend.JAVA));
                if (nativeRun) eq(expected, d.connectedBatch(
                        left, right, M3DisjointIndex.Backend.NATIVE_REQUIRED));
                eq(graphComponents(edges), d.componentCount());
                int required = 1 << random.nextInt(32), forbidden = 1 << random.nextInt(32);
                ArrayList<Integer> roots = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    eq(states[i], d.stateAt(i));
                    if (d.find(i) == i && (states[i] & required) == required
                            && (states[i] & forbidden) == 0) roots.add(i);
                }
                eq(roots.stream().mapToInt(Integer::intValue).toArray(),
                        d.selectedRoots(required, forbidden));
            }
        }
    }

    private static boolean graphConnected(boolean[][] edges, int from, int to) {
        boolean[] seen = new boolean[edges.length];
        int[] queue = new int[seen.length];
        int next = 0, length = 1;
        queue[0] = from;
        seen[from] = true;
        while (next < length) {
            int current = queue[next++];
            if (current == to) return true;
            for (int i = 0; i < seen.length; i++) {
                if (edges[current][i] && !seen[i]) {
                    seen[i] = true;
                    queue[length++] = i;
                }
            }
        }
        return false;
    }

    private static int graphSize(boolean[][] edges, int from) {
        int size = 0;
        for (int target = 0; target < edges.length; target++)
            if (graphConnected(edges, from, target)) size++;
        return size;
    }

    private static int graphComponents(boolean[][] edges) {
        int distinct = 0;
        boolean[] assigned = new boolean[edges.length];
        for (int i = 0; i < assigned.length; i++) {
            if (assigned[i]) continue;
            distinct++;
            for (int j = 0; j < assigned.length; j++)
                if (graphConnected(edges, i, j)) assigned[j] = true;
        }
        return distinct;
    }

    private static void eq(int expected, int actual) { checks++; if (expected != actual) throw new AssertionError(expected + " != " + actual); }
    private static void eq(long expected, long actual) { checks++; if (expected != actual) throw new AssertionError(expected + " != " + actual); }
    private static void eq(boolean expected, boolean actual) { checks++; if (expected != actual) throw new AssertionError(expected + " != " + actual); }
    private static void eq(int[] expected, int[] actual) { checks++; if (!Arrays.equals(expected, actual)) throw new AssertionError("int[] mismatch"); }
    private static void eq(boolean[] expected, boolean[] actual) { checks++; if (!Arrays.equals(expected, actual)) throw new AssertionError("boolean[] mismatch"); }
    private static void fails(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong failure type", failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
