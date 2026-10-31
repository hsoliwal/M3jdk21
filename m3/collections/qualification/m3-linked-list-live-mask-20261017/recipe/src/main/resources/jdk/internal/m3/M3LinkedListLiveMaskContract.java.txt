/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;

/** Real-owner JDK21 List/Deque differential oracle for physical-slot mask lifecycle. */
public final class M3LinkedListLiveMaskContract {
    private static long checks;
    private M3LinkedListLiveMaskContract() { }
    private static void check(boolean good, String description) {
        checks++;
        if (!good) throw new AssertionError(description);
    }
    private static void equal(Object expected, Object actual, String description) {
        check(Objects.equals(expected, actual), description + ": " + expected + " vs " + actual);
    }
    private static Integer value(Random r) { return r.nextInt(11) == 0 ? null : r.nextInt(51) - 25; }

    private static void inspect(M3LinkedList<Integer> m3, LinkedList<Integer> jdk) {
        m3.verifyInvariants();
        equal(new ArrayList<>(jdk), new ArrayList<>(m3), "list order and nulls");
        equal(new ArrayList<>(jdk.reversed()), new ArrayList<>(m3.reversed()), "reverse backed order");
        equal(jdk.size(), m3.size(), "size");
        check(m3.reversed().reversed() == m3, "reverse identity");
        equal(jdk.peekFirst(), m3.peekFirst(), "head");
        equal(jdk.peekLast(), m3.peekLast(), "tail");
    }

    private static void iteratorMutation(M3LinkedList<Integer> m3, LinkedList<Integer> jdk, Random r) {
        int index = r.nextInt(jdk.size() + 1);
        ListIterator<Integer> a = m3.listIterator(index), b = jdk.listIterator(index);
        int direction = r.nextInt(3);
        if (direction == 0 && a.hasNext()) {
            equal(b.next(), a.next(), "iterator next");
            if (r.nextBoolean()) { a.remove(); b.remove(); }
            else { Integer replacement = value(r); a.set(replacement); b.set(replacement); }
        } else if (direction == 1 && a.hasPrevious()) {
            equal(b.previous(), a.previous(), "iterator previous");
            if (r.nextBoolean()) { a.remove(); b.remove(); }
            else { Integer replacement = value(r); a.set(replacement); b.set(replacement); }
        } else {
            Integer x = value(r); a.add(x); b.add(x);
        }
    }

    public static void main(String[] args) {
        final M3LinkedList<Integer> m3 = new M3LinkedList<>();
        final LinkedList<Integer> jdk = new LinkedList<>();
        Random r = new Random(0x3d2790c6L);
        inspect(m3, jdk);
        for (int t = 0; t < 25000; t++) {
            Integer x = value(r);
            switch (r.nextInt(14)) {
                case 0 -> { m3.addFirst(x); jdk.addFirst(x); }
                case 1 -> { m3.addLast(x); jdk.addLast(x); }
                case 2 -> equal(jdk.pollFirst(), m3.pollFirst(), "pollFirst");
                case 3 -> equal(jdk.pollLast(), m3.pollLast(), "pollLast");
                case 4 -> {
                    int i = r.nextInt(jdk.size() + 1);
                    m3.add(i, x); jdk.add(i, x);
                }
                case 5 -> {
                    if (!jdk.isEmpty()) {
                        int i = r.nextInt(jdk.size());
                        equal(jdk.remove(i), m3.remove(i), "remove(index)");
                    }
                }
                case 6 -> {
                    if (!jdk.isEmpty()) {
                        int i = r.nextInt(jdk.size());
                        equal(jdk.set(i, x), m3.set(i, x), "set(index)");
                    }
                }
                case 7 -> equal(jdk.removeFirstOccurrence(x),
                        m3.removeFirstOccurrence(x), "removeFirstOccurrence");
                case 8 -> equal(jdk.removeLastOccurrence(x),
                        m3.removeLastOccurrence(x), "removeLastOccurrence");
                case 9 -> { m3.reversed().addFirst(x); jdk.reversed().addFirst(x); }
                case 10 -> equal(jdk.reversed().pollFirst(), m3.reversed().pollFirst(), "reverse poll");
                case 11 -> iteratorMutation(m3, jdk, r);
                case 12 -> { if (r.nextBoolean()) { m3.clear(); jdk.clear(); } }
                case 13 -> {
                    m3.reversed().addLast(x); jdk.reversed().addLast(x);
                }
                default -> throw new AssertionError();
            }
            inspect(m3, jdk);
        }
        // A structural modification in the reverse view invalidates a normal iterator.
        ListIterator<Integer> m = m3.listIterator();
        ListIterator<Integer> j = jdk.listIterator();
        m3.reversed().addFirst(13);
        jdk.reversed().addFirst(13);
        boolean a = false, b = false;
        try { m.next(); } catch (ConcurrentModificationException expected) { a = true; }
        try { j.next(); } catch (ConcurrentModificationException expected) { b = true; }
        check(a && b, "JDK fail-fast parity across reversed facade");
        inspect(m3, jdk);

        // Exercise free-slot reuse and full reset after many physical allocations.
        for (int i = 0; i < 4097; i++) { m3.addLast(i); jdk.addLast(i); }
        inspect(m3, jdk);
        for (int i = 0; i < 4097; i++) {
            equal(jdk.removeFirst(), m3.removeFirst(), "page-scale recycle");
        }
        inspect(m3, jdk);
        m3.clear(); jdk.clear();
        for (int i = 0; i < 1025; i++) { m3.addFirst(i); jdk.addFirst(i); }
        inspect(m3, jdk);
        System.out.println("M3_LINKED_LIST_LIVE_MASK_PASS trials=25000 checks=" + checks
                + " owner=REAL_ARRAY_LINKED_DEQUE");
    }
}
