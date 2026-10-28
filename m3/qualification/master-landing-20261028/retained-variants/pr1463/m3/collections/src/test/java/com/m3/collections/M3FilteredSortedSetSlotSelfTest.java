/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Standalone Java21 public-contract differential for M3TreeSet's original
 * physical AVL index paths through mutable and read-only filtered views.
 * All baseline operations use the same view API over JDK TreeSet.
 */
public final class M3FilteredSortedSetSlotSelfTest {
    private static final Predicate<Integer> VISIBLE =
            value -> value == null || Math.floorMod(value, 3) == 0;
    private static long checks;

    private M3FilteredSortedSetSlotSelfTest() { }

    private static void check(boolean valid, String context) {
        checks++;
        if (!valid) throw new AssertionError(context + " check=" + checks);
    }

    private static void same(Object expected, Object actual, String context) {
        checks++;
        if (!Objects.equals(expected, actual))
            throw new AssertionError(context + " expected=" + expected + " actual=" + actual);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation, String context) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(context + " expected " + type + ", got " + failure, failure);
        }
        throw new AssertionError(context + " expected " + type);
    }

    private static void compare(NavigableSet<Integer> reference, NavigableSet<Integer> m3,
            M3TreeSet<Integer> owner, String context) {
        same(new ArrayList<>(reference), new ArrayList<>(m3), context + " ascending");
        same(new ArrayList<>(reference.descendingSet()), new ArrayList<>(m3.descendingSet()),
                context + " descending");
        same(reference.size(), m3.size(), context + " size");
        same(reference.isEmpty(), m3.isEmpty(), context + " empty");
        same(reference, m3, context + " equality");
        same(reference.hashCode(), m3.hashCode(), context + " hash");
        same(reference.toString(), m3.toString(), context + " string");
        if (!reference.isEmpty()) {
            same(reference.first(), m3.first(), context + " first");
            same(reference.last(), m3.last(), context + " last");
        } else {
            expect(NoSuchElementException.class, reference::first, context + " empty-first reference");
            expect(NoSuchElementException.class, m3::first, context + " empty-first M3");
            expect(NoSuchElementException.class, reference::last, context + " empty-last reference");
            expect(NoSuchElementException.class, m3::last, context + " empty-last M3");
        }
        for (Integer value : Arrays.asList(null, -30, -21, -13, -3, -1, 0, 2, 3, 7, 12, 17, 21, 30)) {
            same(reference.contains(value), m3.contains(value),
                    context + " contains(" + value + ")");
            same(reference.lower(value), m3.lower(value), context + " lower(" + value + ")");
            same(reference.floor(value), m3.floor(value), context + " floor(" + value + ")");
            same(reference.ceiling(value), m3.ceiling(value), context + " ceiling(" + value + ")");
            same(reference.higher(value), m3.higher(value), context + " higher(" + value + ")");
        }
        owner.verifyInvariants();
    }

    private static void seed(TreeSet<Integer> jdk, M3TreeSet<Integer> m3) {
        for (int value = -18; value <= 18; value++) {
            if (value % 4 == 1) continue;
            check(jdk.add(value) == m3.add(value), "seed add");
        }
        check(jdk.add(null) == m3.add(null), "nullable comparator maps null key");
        m3.verifyInvariants();
    }

    private static NavigableSet<Integer> section(NavigableSet<Integer> source,
            boolean bounded, boolean descending) {
        NavigableSet<Integer> result = bounded
                ? source.subSet(-12, true, 13, false) : source;
        return descending ? result.descendingSet() : result;
    }

    private static void readOnly(boolean bounded, boolean descending, long seed) {
        Comparator<Integer> order = Comparator.nullsFirst(Integer::compare);
        TreeSet<Integer> jdk = new TreeSet<>(order);
        M3TreeSet<Integer> owner = new M3TreeSet<>(order);
        seed(jdk, owner);
        NavigableSet<Integer> reference = M3FilteredViews.filter(
                section(jdk, bounded, descending), VISIBLE);
        NavigableSet<Integer> m3 = M3FilteredViews.filter(
                section(owner, bounded, descending), VISIBLE);
        check(reference.comparator() != null && m3.comparator() != null, "comparator returned");
        compare(reference, m3, owner, "read only initial " + seed);

        expect(UnsupportedOperationException.class, () -> m3.add(3), "read only add");
        expect(UnsupportedOperationException.class, () -> m3.remove(3), "read only remove");
        expect(UnsupportedOperationException.class, m3::pollFirst, "read only poll");
        expect(UnsupportedOperationException.class, m3::clear, "read only clear");
        Iterator<Integer> readOnlyCursor = m3.iterator();
        if (readOnlyCursor.hasNext()) {
            readOnlyCursor.next();
            expect(UnsupportedOperationException.class,
                    readOnlyCursor::remove, "read only iterator remove");
        }

        Random random = new Random(seed);
        for (int phase = 0; phase < 180; phase++) {
            Integer value = random.nextInt(13) == 0 ? null : random.nextInt(51) - 25;
            if (random.nextBoolean())
                check(jdk.add(value) == owner.add(value), "direct owner add parity");
            else
                check(jdk.remove(value) == owner.remove(value), "direct owner remove parity");
            compare(reference, m3, owner, "read only phase " + phase);
        }

        // The JDK source baseline's iterator is fail-fast; M3 index cursors
        // must not retain stale coordinates after a structural owner change.
        Iterator<Integer> cursor = m3.iterator();
        if (cursor.hasNext()) {
            cursor.hasNext();
            owner.add(10000);
            expect(ConcurrentModificationException.class,
                    cursor::hasNext, "stale original slot cursor");
        }
    }

    private static void mutable(boolean bounded, boolean descending, long seed) {
        Comparator<Integer> order = Comparator.nullsFirst(Integer::compare);
        TreeSet<Integer> jdk = new TreeSet<>(order);
        M3TreeSet<Integer> owner = new M3TreeSet<>(order);
        seed(jdk, owner);
        NavigableSet<Integer> reference = M3MutableFilteredViews.filter(
                section(jdk, bounded, descending), VISIBLE);
        NavigableSet<Integer> m3 = M3MutableFilteredViews.filter(
                section(owner, bounded, descending), VISIBLE);
        compare(reference, m3, owner, "mutable initial " + seed);

        Random random = new Random(seed);
        for (int phase = 0; phase < 280; phase++) {
            Integer candidate = random.nextInt(17) == 0 ? null : random.nextInt(25) - 12;
            int operation = random.nextInt(11);
            switch (operation) {
                case 0 -> same(jdk.add(candidate), owner.add(candidate), "owner add");
                case 1 -> same(jdk.remove(candidate), owner.remove(candidate), "owner remove");
                case 2 -> {
                    if (!VISIBLE.test(candidate)) {
                        expect(IllegalArgumentException.class,
                                () -> reference.add(candidate), "reference rejects hidden add");
                        expect(IllegalArgumentException.class,
                                () -> m3.add(candidate), "M3 rejects hidden add");
                    } else if (bounded && candidate == null) {
                        // The source comparator accepts null, but this live range
                        // starts at -12. A valid value type is not automatically
                        // a valid subMap/subSet endpoint.
                        expect(IllegalArgumentException.class,
                                () -> reference.add(candidate), "bounded null add rejected by JDK");
                        expect(IllegalArgumentException.class,
                                () -> m3.add(candidate), "bounded null add rejected by M3");
                    } else {
                        same(reference.add(candidate), m3.add(candidate), "admitted add");
                    }
                }
                case 3 -> same(reference.remove(candidate), m3.remove(candidate), "visible remove");
                case 4 -> {
                    reference.clear();
                    m3.clear();
                }
                case 5 -> same(reference.removeIf(value -> value != null && value > 0),
                        m3.removeIf(value -> value != null && value > 0), "visible removeIf");
                case 6 -> {
                    Iterator<Integer> a = reference.iterator();
                    Iterator<Integer> b = m3.iterator();
                    same(a.hasNext(), b.hasNext(), "iterator next readiness");
                    if (a.hasNext()) {
                        same(a.next(), b.next(), "iterator returned stored representative");
                        a.remove();
                        b.remove();
                    }
                }
                case 7 -> {
                    Iterator<Integer> a = reference.descendingIterator();
                    Iterator<Integer> b = m3.descendingIterator();
                    same(a.hasNext(), b.hasNext(), "descending iterator readiness");
                    if (a.hasNext()) {
                        same(a.next(), b.next(), "descending iterator stored value");
                        a.remove();
                        b.remove();
                    }
                }
                case 8 -> same(reference.pollFirst(), m3.pollFirst(), "pollFirst");
                case 9 -> same(reference.pollLast(), m3.pollLast(), "pollLast");
                case 10 -> {
                    // Only admitted physical integer keys; both owners use the same
                    // iterator/bulk mutation order and range validation.
                    Collection<Integer> incoming = List.of(-6, 0, 6);
                    same(reference.addAll(incoming), m3.addAll(incoming), "bulk admitted addAll");
                }
                default -> throw new AssertionError(operation);
            }
            same(jdk, owner, "underlying canonical root parity");
            compare(reference, m3, owner, "mutable phase " + phase);
        }

        Iterator<Integer> cursor = m3.iterator();
        if (cursor.hasNext()) {
            cursor.next();
            owner.add(10000);
            expect(ConcurrentModificationException.class,
                    cursor::hasNext, "structural mutation invalidates M3 slot cursor");
        }
    }

    private static void retainedRepresentative() {
        Comparator<String> order = String.CASE_INSENSITIVE_ORDER;
        TreeSet<String> baseline = new TreeSet<>(order);
        M3TreeSet<String> owner = new M3TreeSet<>(order);
        for (String word : List.of("alpha", "beta", "gamma", "delta")) {
            baseline.add(word);
            owner.add(word);
        }
        Predicate<String> representative = value -> value.equals("beta") || value.equals("delta");
        NavigableSet<String> a = M3FilteredViews.filter(baseline, representative);
        NavigableSet<String> b = M3FilteredViews.filter(owner, representative);
        same(a.contains("BETA"), b.contains("BETA"), "query alias resolved to stored key");
        same(a.contains("ALPHA"), b.contains("ALPHA"), "hidden stored representative");
        same(a.lower("GAMMA"), b.lower("GAMMA"), "lower stored representative");
        same(a.ceiling("ALPHA"), b.ceiling("ALPHA"), "ceiling stored representative");
        same(a, b, "case insensitive visible set");
        NavigableSet<String> c = M3MutableFilteredViews.filter(baseline, representative);
        NavigableSet<String> d = M3MutableFilteredViews.filter(owner, representative);
        same(c.contains("BETA"), d.contains("BETA"), "mutable stored representative");
        same(c.remove("BETA"), d.remove("BETA"), "delete retained representative");
        same(baseline, owner, "root after aliased removal");
    }

    private static void iteratorState() {
        M3TreeSet<Integer> owner = new M3TreeSet<>();
        owner.addAll(List.of(1, 2, 3, 4, 5, 6));
        AtomicInteger calls = new AtomicInteger();
        NavigableSet<Integer> ro = M3FilteredViews.filter(owner, value -> {
            calls.incrementAndGet(); return value % 2 == 0;
        });
        Iterator<Integer> cursor = ro.iterator();
        check(cursor.hasNext(), "hasNext true");
        int first = calls.get();
        check(cursor.hasNext(), "hasNext cached");
        same(first, calls.get(), "predicate evaluated once for prepared value");
        same(2, cursor.next(), "cursor retrieves canonical slot");
        NavigableSet<Integer> rw = M3MutableFilteredViews.filter(owner,
                value -> value % 2 == 0);
        Iterator<Integer> mutableCursor = rw.iterator();
        expect(IllegalStateException.class, mutableCursor::remove,
                "remove before next illegal");
        if (mutableCursor.hasNext()) {
            mutableCursor.next();
            mutableCursor.remove();
            expect(IllegalStateException.class, mutableCursor::remove,
                    "duplicate iterator.remove illegal");
        }
        owner.verifyInvariants();
    }

    public static void main(String[] args) {
        readOnly(false, false, 0x4D3353455401L);
        readOnly(true, false, 0x4D3353455402L);
        readOnly(false, true, 0x4D3353455403L);
        readOnly(true, true, 0x4D3353455404L);
        mutable(false, false, 0x4D3353455411L);
        mutable(true, false, 0x4D3353455412L);
        mutable(false, true, 0x4D3353455413L);
        mutable(true, true, 0x4D3353455414L);
        retainedRepresentative();
        iteratorState();
        System.out.println("PASS M3FilteredSortedSetSlotSelfTest checks=" + checks);
    }
}
