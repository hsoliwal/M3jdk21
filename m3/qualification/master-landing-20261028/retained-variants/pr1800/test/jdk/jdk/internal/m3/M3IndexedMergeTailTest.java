/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Preserve stable indexed merge order and exact callback effects across primitive tail copies
 * @modules java.base/jdk.internal.m3
 * @compile --patch-module java.base=${test.src} M3IndexedMergeTailTest.java
 * @run main/othervm --patch-module java.base=${test.classes} jdk.internal.m3.M3IndexedMergeTailTest
 * @run main/othervm --patch-module java.base=${test.classes} -Xint jdk.internal.m3.M3IndexedMergeTailTest
 */
package jdk.internal.m3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/** Same immutable fixture runs against the exact baseline and recipe-emitted owner. */
public final class M3IndexedMergeTailTest {
    private record Row(int id, int key) { }
    private static long digest = 0x4d334d45524745L;
    private static long comparisons;
    private static long checks;
    private static int cases;
    private M3IndexedMergeTailTest() { }

    private static void observe(long value) {
        digest = Long.rotateLeft(digest, 9) ^ value;
        digest *= 0x9e3779b97f4a7c15L;
    }
    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
    private static int id(Row row) { return row == null ? -1 : row.id(); }
    private static Comparator<Row> order(boolean reverse) {
        Comparator<Row> keys = Comparator.comparingInt(Row::key);
        return Comparator.nullsFirst(reverse ? keys.reversed() : keys);
    }
    private static void sameIdentity(List<Row> actual, List<Row> expected) {
        check(actual.size() == expected.size(), "size");
        for (int i = 0; i < expected.size(); i++) {
            check(actual.get(i) == expected.get(i), "stable payload identity at " + i);
            observe(id(actual.get(i)));
        }
    }
    private static void vector(int n, int shape, boolean reverse, boolean filtered) {
        var owner = new M3IndexedArray<Row>();
        var original = new ArrayList<Row>();
        var random = new Random(0x4d335441494cL + n * 31L + shape);
        for (int i = 0; i < n; i++) {
            int key = switch (shape) {
                case 0 -> i / 3;
                case 1 -> (n - i) / 3;
                case 2 -> random.nextInt(23) - 11;
                case 3 -> (i / 64 % 2 == 0 ? i % 64 : 64 - i % 64);
                default -> 0;
            };
            Row row = i % 37 == 0 ? null : new Row(i, key);
            owner.append(row); original.add(row);
        }
        Comparator<Row> semantic = order(reverse);
        long[] localCalls = {0};
        Comparator<Row> traced = (left, right) -> {
            check(Thread.holdsLock(owner), "comparator monitor ownership");
            localCalls[0]++; comparisons++;
            observe(id(left)); observe(id(right));
            return semantic.compare(left, right);
        };
        List<Row> view = filtered
                ? owner.filteredSorted(row -> row == null || row.id() % 3 != 1, traced)
                : owner.sorted(traced);
        var expected = new ArrayList<Row>();
        for (Row row : original) if (!filtered || row == null || row.id() % 3 != 1) expected.add(row);
        expected.sort(semantic);
        sameIdentity(view, expected);
        long warmCalls = localCalls[0];
        sameIdentity(view, expected);
        check(localCalls[0] == warmCalls, "warm view repeated comparator calls");
        Row added = new Row(n + 3, -17);
        long handle = owner.append(added);
        if (!filtered || added.id() % 3 != 1) expected.add(added);
        expected.sort(semantic);
        sameIdentity(view, expected);
        owner.remove(handle);
        expected.removeIf(row -> row == added);
        sameIdentity(view, expected);
        observe(localCalls[0]); cases++;
    }
    private static final class Marker extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    private static void exceptions() {
        for (int throwAt : new int[]{1, 2, 3, 7, 11, 25, 63}) {
            var owner = new M3IndexedArray<Row>();
            var expected = new ArrayList<Row>();
            for (int i = 0; i < 129; i++) {
                Row row = new Row(i, (i * 71) % 29); owner.append(row); expected.add(row);
            }
            Marker marker = new Marker();
            AtomicBoolean enabled = new AtomicBoolean(true);
            int[] called = {0};
            Comparator<Row> comparator = (left, right) -> {
                called[0]++; observe(id(left)); observe(id(right));
                if (enabled.get() && called[0] == throwAt) throw marker;
                return Integer.compare(left.key(), right.key());
            };
            List<Row> view = owner.sorted(comparator);
            Throwable failure = null;
            try { view.size(); } catch (Throwable thrown) { failure = thrown; }
            check(failure == marker, "throwing comparator exception identity");
            check(called[0] == throwAt, "throwing comparator stopping point");
            enabled.set(false);
            expected.sort(Comparator.comparingInt(Row::key));
            sameIdentity(view, expected);
            cases++;
        }
    }
    private static void mutation() {
        for (int operation = 0; operation < 3; operation++) {
            var owner = new M3IndexedArray<Row>();
            long first = owner.append(new Row(0, 3));
            owner.append(new Row(1, 1)); owner.append(new Row(2, 2));
            int kind = operation;
            int[] called = {0};
            Comparator<Row> comparator = (left, right) -> {
                called[0]++; observe(id(left)); observe(id(right));
                if (called[0] == 1) {
                    if (kind == 0) owner.append(new Row(3, 4));
                    else if (kind == 1) owner.replace(first, new Row(0, 4));
                    else owner.retain(first);
                }
                return Integer.compare(left.key(), right.key());
            };
            List<Row> view = kind == 2 ? owner.maskedSortedStates(1, 0, comparator) : owner.sorted(comparator);
            Throwable failure = null;
            try { view.size(); } catch (Throwable thrown) { failure = thrown; }
            check(failure instanceof ConcurrentModificationException, "reentrant mutation must fail");
            check(called[0] == 1, "reentrant mutation stopping point");
            if (kind == 2) owner.release(first);
            cases++;
        }
        var owner = new M3IndexedArray<Row>();
        Row a = new Row(0, 3), b = new Row(1, 1), c = new Row(2, 2);
        long first = owner.append(a); owner.append(b); owner.append(c);
        AtomicBoolean retained = new AtomicBoolean();
        List<Row> view = owner.sorted((left, right) -> {
            if (retained.compareAndSet(false, true)) owner.retain(first);
            return Integer.compare(left.key(), right.key());
        });
        sameIdentity(view, List.of(b, c, a));
        owner.release(first);
        sameIdentity(view, List.of(b, c, a));
        cases++;
    }
    public static void main(String[] args) {
        int[] sizes = {0, 1, 2, 3, 7, 8, 9, 31, 32, 33, 63, 64, 65, 127, 128, 129, 255, 256, 257, 1023, 1024, 1025, 2049};
        for (int n : sizes) for (int shape = 0; shape < 5; shape++)
            for (boolean reverse : new boolean[]{false, true})
                for (boolean filtered : new boolean[]{false, true}) vector(n, shape, reverse, filtered);
        exceptions(); mutation();
        System.out.println("INDEXED_MERGE_PASS cases=" + cases + " checks=" + checks
                + " comparisons=" + comparisons + " digest=" + Long.toHexString(digest));
    }
}
