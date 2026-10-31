/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

/** Dependency-free Java21 original JDK contract differential; no vendor adapters. */
public final class M3GenericReferenceOwnersProof {
    private static long checks;
    private M3GenericReferenceOwnersProof() { }
    private static void eq(Object a, Object b) {
        checks++;
        if (!java.util.Objects.deepEquals(a, b)) {
            throw new AssertionError("want=" + a + " got=" + b);
        }
    }
    private static void check(boolean yes) { checks++; if (!yes) throw new AssertionError(); }
    private static void deque() {
        var j = new ArrayDeque<Integer>();
        var m = new M3ArrayDeque<Integer>();
        Random r = new Random(0x4d334445515545L);
        for (int i = 0; i < 5000; i++) {
            int value = r.nextInt(49) - 24;
            switch (r.nextInt(8)) {
                case 0 -> { j.addFirst(value); m.addFirst(value); }
                case 1 -> { j.addLast(value); m.addLast(value); }
                case 2 -> eq(j.pollFirst(), m.pollFirst());
                case 3 -> eq(j.pollLast(), m.pollLast());
                case 4 -> eq(j.removeFirstOccurrence(value), m.removeFirstOccurrence(value));
                case 5 -> eq(j.removeLastOccurrence(value), m.removeLastOccurrence(value));
                case 6 -> {
                    int cutoff = r.nextInt(10);
                    eq(j.removeIf(x -> x < cutoff), m.removeIf(x -> x < cutoff));
                }
                default -> eq(j.contains(value), m.contains(value));
            }
            if ((i & 15) == 0) {
                eq(Arrays.asList(j.toArray()), Arrays.asList(m.toArray()));
                eq(j.size(), m.size());
            }
        }
        m.addLast(901);
        m.updateState(m.size() - 1, M3ArrayDeque.VISIBLE, 0);
        int[] ids = m.selectedPhysicalIndices(M3ArrayDeque.VISIBLE, 0,
                M3IndexMaskCompactor.Backend.JAVA);
        check(ids.length > 0);
        eq(m.physicalIndex(m.size() - 1), ids[ids.length - 1]);
        eq(901, m.reversed().removeFirst());
    }
    private static void priority() {
        var j = new PriorityQueue<Integer>();
        var m = new M3PriorityQueue<Integer>();
        Random r = new Random(0x4d33505155455545L);
        for (int i = 0; i < 5000; i++) {
            int value = r.nextInt(59) - 29;
            switch (r.nextInt(8)) {
                case 0, 1, 2 -> { j.offer(value); m.offer(value); }
                case 3 -> eq(j.poll(), m.poll());
                case 4 -> eq(j.remove(value), m.remove(value));
                case 5 -> {
                    int cutoff = r.nextInt(9) - 4;
                    eq(j.removeIf(x -> x < cutoff), m.removeIf(x -> x < cutoff));
                }
                case 6 -> { if (r.nextInt(27) == 0) { j.clear(); m.clear(); } }
                default -> eq(j.contains(value), m.contains(value));
            }
            if (i % 17 == 0) {
                eq(j.peek(), m.peek());
                eq(j.size(), m.size());
                Integer[] ref = j.toArray(Integer[]::new);
                Arrays.sort(ref, Comparator.naturalOrder());
                eq(Arrays.asList(ref), Arrays.asList(m.toSortedArray()));
            }
        }
        m.offer(777);
        for (int i = 0; i < m.size(); i++) {
            if (m.toArray()[i].equals(777)) {
                m.updateState(i, M3PriorityQueue.DIRTY, 0);
                break;
            }
        }
        int[] ids = m.selectedPhysicalIndices(M3PriorityQueue.DIRTY, 0,
                M3IndexMaskCompactor.Backend.JAVA);
        check(ids.length > 0);
    }
    private static void nativeMaskParity() {
        check(M3IndexMaskCompactor.nativeAvailable());
        var d = new M3ArrayDeque<Integer>();
        var q = new M3PriorityQueue<Integer>();
        for (int i = 0; i < 256; i++) { d.addLast(i); q.add(i); }
        for (int i = 0; i < 31; i++) d.removeFirst();
        for (int i = 0; i < d.size(); i += 2) d.updateState(i, 2, 0);
        for (int i = 0; i < q.size(); i += 3) q.updateState(i, 2, 0);
        for (int required : new int[] {0, 1, 2, 4, 128}) {
            eq(Arrays.toString(d.selectedPhysicalIndices(required, 0,
                    M3IndexMaskCompactor.Backend.JAVA)),
                    Arrays.toString(d.selectedPhysicalIndices(required, 0,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED)));
            eq(Arrays.toString(q.selectedPhysicalIndices(required, 0,
                    M3IndexMaskCompactor.Backend.JAVA)),
                    Arrays.toString(q.selectedPhysicalIndices(required, 0,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED)));
        }
    }
    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && args[0].equals("--native");
        if (args.length > 1 || args.length == 1 && !nativeRun) {
            throw new IllegalArgumentException("usage [--native]");
        }
        deque(); priority();
        if (nativeRun) nativeMaskParity();
        System.out.println("PASS M3GenericReferenceOwnersProof checks=" + checks + " native=" + nativeRun);
    }
}
