// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Same-source flat-master/bounded-clear proof; no GC scheduling assumptions. */
public final class M3LinkedListClearRangeProof {
    private static final Object PAYLOAD = new Object();
    private static final Object OTHER = new Object();
    private static long checks;

    private M3LinkedListClearRangeProof() { }

    public static void main(String[] arguments) throws ReflectiveOperationException {
        maskAndCapacityBoundaries();
        frontierCanExceedLiveSize();
        retainedPeakSparseReuse();
        emptyClearPreservesBookkeeping();
        System.out.println("PASS M3LinkedListClearRangeProof checks=" + checks);
    }

    private static void maskAndCapacityBoundaries() throws ReflectiveOperationException {
        for (int count : new int[] {0, 1, 8, 63, 64, 65, 127, 128, 129, 512, 65536}) {
            M3LinkedList<Object> actual = new M3LinkedList<>();
            LinkedList<Object> expected = new LinkedList<>();
            for (int index = 0; index < count; index++) {
                Object value = index % 7 == 0 ? null : PAYLOAD;
                actual.addLast(value);
                expected.addLast(value);
            }
            sameContents(actual, expected, "boundary before " + count);
            clearAndVerify(actual, expected, "boundary clear " + count);
            actual.addFirst(null);
            expected.addFirst(null);
            actual.reversed().addFirst(OTHER);
            expected.addLast(OTHER);
            sameContents(actual, expected, "boundary refill " + count);
            clearAndVerify(actual, expected, "boundary second clear " + count);
        }
    }

    private static void frontierCanExceedLiveSize() throws ReflectiveOperationException {
        // Exercise both the incumbent sparse walk and the refined dense fallback.
        for (int survivors : new int[] {3, 2048}) {
            M3LinkedList<Object> actual = new M3LinkedList<>();
            LinkedList<Object> expected = new LinkedList<>();
            for (int index = 0; index < 65536; index++) {
                actual.addLast(PAYLOAD);
                expected.addLast(PAYLOAD);
            }
            while (actual.size() > survivors) {
                equal(actual.removeFirst(), expected.removeFirst(), "erase low physical slot");
            }
            State before = snapshot(actual);
            equal(before.used, 65536, "deletion retains frontier");
            equal(before.size, survivors, "only high physical slots remain live");
            check(before.values[65535] == PAYLOAD, "high physical payload still live");
            // Bounding by size instead of used would leave high references retained.
            clearAndVerify(actual, expected, "high physical slots clear " + survivors);
        }
    }

    private static void retainedPeakSparseReuse() throws ReflectiveOperationException {
        M3LinkedList<Object> actual = new M3LinkedList<>();
        LinkedList<Object> expected = new LinkedList<>();
        for (int index = 0; index < 65536; index++) {
            actual.addLast(PAYLOAD);
            expected.addLast(PAYLOAD);
        }
        State peak = snapshot(actual);
        clearAndVerify(actual, expected, "peak clear");
        for (int count : new int[] {1, 64, 512, 2048, 4096, 16384, 65, 1}) {
            for (int index = 0; index < count; index++) {
                Object value = index % 5 == 0 ? null : PAYLOAD;
                if ((index & 1) == 0) {
                    actual.addFirst(value);
                    expected.addFirst(value);
                } else {
                    actual.reversed().addFirst(value);
                    expected.addLast(value);
                }
            }
            ListIterator<Object> a = actual.listIterator(actual.size() / 2);
            ListIterator<Object> e = expected.listIterator(expected.size() / 2);
            a.add(OTHER);
            e.add(OTHER);
            equal(a.previous(), e.previous(), "refill iterator previous");
            a.set(null);
            e.set(null);
            equal(a.next(), e.next(), "refill iterator next");
            a.remove();
            e.remove();
            sameContents(actual, expected, "sparse refill " + count);
            sameLanes(peak, snapshot(actual), "sparse refill retains peak lanes");
            clearAndVerify(actual, expected, "sparse clear " + count);
        }
    }

    private static void emptyClearPreservesBookkeeping() throws ReflectiveOperationException {
        M3LinkedList<Object> actual = new M3LinkedList<>();
        LinkedList<Object> expected = new LinkedList<>();
        clearAndVerify(actual, expected, "fresh empty clear");
        for (int index = 0; index < 2051; index++) {
            actual.addLast(PAYLOAD);
            expected.addLast(PAYLOAD);
        }
        while (!actual.isEmpty()) {
            equal(actual.removeLast(), expected.removeLast(), "erase to empty");
        }
        State erased = snapshot(actual);
        check(erased.used > 0 && erased.free >= 0, "empty owner retains free chain");
        clearAndVerify(actual, expected, "erased empty clear");
        State after = snapshot(actual);
        equal(after.used, erased.used, "empty clear used unchanged");
        equal(after.free, erased.free, "empty clear free unchanged");
        ListIterator<Object> survivor = actual.listIterator();
        actual.reversed().clear();
        survivor.add(null);
        expected.add(null);
        equal(survivor.previous(), null, "iterator survives empty clear");
        sameContents(actual, expected, "empty-clear iterator refill");
        clearAndVerify(actual, expected, "final clear");
    }

    private static void clearAndVerify(M3LinkedList<Object> actual,
            LinkedList<Object> expected, String label) throws ReflectiveOperationException {
        M3LinkedList<Object> reverse = actual.reversed();
        State before = snapshot(actual);
        Iterator<Object> forwardIterator = actual.iterator();
        Iterator<Object> reverseIterator = reverse.iterator();
        reverse.clear();
        expected.clear();
        State after = snapshot(actual);
        sameLanes(before, after, label);
        equal(after.size, 0, label + " size");
        equal(after.first, -1, label + " first");
        equal(after.last, -1, label + " last");
        int change = before.size == 0 ? 0 : 1;
        equal(after.modCount, before.modCount + change, label + " forward modCount");
        equal(after.reverseModCount, before.reverseModCount + change, label + " reverse modCount");
        if (before.size == 0) {
            equal(after.used, before.used, label + " empty frontier");
            equal(after.free, before.free, label + " empty free head");
            expect(NoSuchElementException.class, forwardIterator, label + " empty forward");
            expect(NoSuchElementException.class, reverseIterator, label + " empty reverse");
        } else {
            equal(after.used, 0, label + " reset frontier");
            equal(after.free, -1, label + " reset free head");
            expect(ConcurrentModificationException.class, forwardIterator, label + " stale forward");
            expect(ConcurrentModificationException.class, reverseIterator, label + " stale reverse");
        }
        for (Object value : after.values) check(value == null, label + " released payload");
        for (long word : after.mask) equal(word, 0L, label + " cleared mask");
        check(field(reverse, "storage") == after.storage, label + " shared reverse storage");
        sameContents(actual, expected, label);
    }

    private static void sameContents(M3LinkedList<Object> actual,
            LinkedList<Object> expected, String label) throws ReflectiveOperationException {
        equal(new ArrayList<>(actual), new ArrayList<>(expected), label + " forward");
        equal(new ArrayList<>(actual.reversed()), new ArrayList<>(expected.reversed()), label + " reverse");
        long bits = 0;
        for (long word : snapshot(actual).mask) bits += Long.bitCount(word);
        equal(bits, (long) actual.size(), label + " occupancy includes null values");
        actual.verifyInvariants();
        actual.reversed().verifyInvariants();
    }

    private static State snapshot(M3LinkedList<?> list) throws ReflectiveOperationException {
        Object owner = field(list, "storage");
        return new State(owner, (Object[]) field(owner, "values"),
                (int[]) field(owner, "before"), (int[]) field(owner, "after"),
                (long[]) field(owner, "liveMask"), list.size(),
                integer(owner, "used"), integer(owner, "free"),
                integer(owner, "first"), integer(owner, "last"),
                integer(list.listIterator(), "expected"),
                integer(list.reversed().listIterator(), "expected"));
    }

    private static void sameLanes(State before, State after, String label) {
        check(before.storage == after.storage, label + " storage identity");
        check(before.values == after.values, label + " payload capacity/identity");
        check(before.before == after.before && before.after == after.after, label + " link identities");
        check(before.mask == after.mask, label + " state capacity/identity");
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static int integer(Object owner, String name) throws ReflectiveOperationException {
        return (Integer) field(owner, name);
    }

    private static void expect(Class<? extends RuntimeException> type,
            Iterator<?> iterator, String label) {
        try {
            iterator.next();
            throw new AssertionError(label + ": expected " + type.getSimpleName());
        } catch (RuntimeException failure) {
            check(type.isInstance(failure), label + ": unexpected " + failure);
        }
    }

    private static void equal(Object actual, Object expected, String label) {
        checks++;
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private record State(Object storage, Object[] values, int[] before, int[] after,
            long[] mask, int size, int used, int free, int first, int last,
            int modCount, int reverseModCount) { }
}
