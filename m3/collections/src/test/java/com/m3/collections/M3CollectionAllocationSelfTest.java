/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated through the source-sealed Synexia M3 collection allocation recipe.
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/** Deterministic behavioral oracle, runnable with the JDK alone. */
public final class M3CollectionAllocationSelfTest {
    private static long checks;
    private M3CollectionAllocationSelfTest() { }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
    private static M3ArrayList<Integer> list(int length) {
        M3ArrayList<Integer> result = new M3ArrayList<>(length + 13);
        for (int i = 0; i < length; i++) result.add(i);
        return result;
    }
    private static M3ObjectIndexSnapshot<Integer> snapshot(int length) {
        Object[] values = new Object[length];
        int[] slots = new int[length];
        byte[] states = new byte[length];
        for (int i = 0; i < length; i++) {
            values[i] = i; slots[i] = 3 * i + 2; states[i] = (byte) i;
        }
        return M3ObjectIndexSnapshot.capture(values, slots, states);
    }
    private static void filtering() {
        Random random = new Random(0x20261018L);
        for (int n : new int[]{0, 1, 2, 63, 64, 65, 127, 128, 129, 1025}) {
            for (int pass = 0; pass < 70; pass++) {
                boolean[] reject = new boolean[n];
                for (int i = 0; i < n; i++) reject[i] = switch (pass) {
                    case 0 -> false;
                    case 1 -> true;
                    case 2 -> i == n - 1;
                    case 3 -> i == 0;
                    case 4 -> i == 1 || i == 65 || i == n - 1;
                    case 5 -> i < 64 || i > 127;
                    case 6 -> i < 65 || i > 128;
                    case 7 -> i >= 3 && i < 67;
                    default -> random.nextBoolean();
                };
                M3ArrayList<Integer> owner = list(n);
                int capacity = owner.capacity();
                ArrayList<Integer> expected = new ArrayList<>();
                for (int i = 0; i < n; i++) if (!reject[i]) expected.add(i);
                int[] calls = {0};
                boolean changed = owner.removeIf(value -> {
                    check(value == calls[0]++, "predicate encounter order"); return reject[value];
                });
                check(calls[0] == n && changed == (expected.size() != n), "filter cardinality");
                check(owner.equals(expected), "stable compaction");
                check(owner.capacity() == capacity, "capacity unchanged");
                owner.add(9000); expected.add(9000);
                check(owner.equals(expected), "append after compaction");

                var original = snapshot(n + 6).subList(3, n + 3);
                calls[0] = 0;
                var selected = original.filtered(value -> {
                    check(value - 3 == calls[0]++, "snapshot callback order");
                    return !reject[value - 3];
                });
                check(calls[0] == n && selected.size() == expected.size() - 1, "snapshot count");
                if (selected.size() == n) check(selected == original, "all-pass identity");
                int k = 0;
                for (int i = 0; i < n; i++) if (!reject[i]) {
                    check(selected.get(k) == i + 3, "snapshot value");
                    check(selected.physicalSlotAt(k) == 3 * (i + 3) + 2, "physical slot");
                    check(selected.stateAt(k) == ((i + 3) & 255), "state correspondence");
                    k++;
                }
                check(original.size() == n, "snapshot untouched");
            }
        }
    }
    private static void failureAndReentrancy() {
        for (int at : new int[]{0, 1, 63, 64, 65, 128}) {
            M3ArrayList<Integer> owner = list(129);
            List<Integer> before = new ArrayList<>(owner);
            Iterator<Integer> cursor = owner.iterator();
            RuntimeException marker = new IllegalStateException("predicate marker");
            try {
                owner.removeIf(value -> { if (value == at) throw marker; return (value & 1) == 0; });
                throw new AssertionError("exception missing");
            } catch (RuntimeException failure) { check(failure == marker, "same throwable"); }
            check(owner.equals(before), "exception atomicity");
            check(cursor.next() == 0, "iterator valid after failed filter");
        }
        M3ArrayList<Integer> owner = list(4);
        owner.removeIf(value -> { if (value == 1) owner.set(0, 9); return value == 1; });
        check(owner.equals(List.of(9, 2, 3)), "callback write in retained prefix");
        Iterator<Integer> cursor = owner.iterator();
        check(!owner.removeIf(value -> false), "no removal");
        check(cursor.next() == 9, "iterator valid on no-op");
        expect(ConcurrentModificationException.class, () -> owner.removeIf(value -> {
            owner.add(7); return true;
        }));
        check(owner.equals(List.of(9, 2, 3, 7)), "only callback structural effect remains");
        expect(NullPointerException.class, () -> new M3ArrayList<>().removeIf(null));
        expect(NullPointerException.class, () -> snapshot(0).filtered(null));
        var snap = snapshot(129);
        expect(IllegalArgumentException.class, () -> snap.filtered(value -> {
            if (value == 65) throw new IllegalArgumentException(); return value % 2 == 0;
        }));
        check(snap.size() == 129 && snap.get(65) == 65, "snapshot exception leaves source");
    }
    private static void bulkCopies() {
        for (int n : new int[]{0, 1, 65}) {
            M3ArrayList<Integer> source = list(n);
            M3ArrayList<Integer> copy = new M3ArrayList<>(source);
            check(copy.equals(source) && copy.capacity() == n, "detached exact copy");
            source.add(-1);
            check(copy.size() == n, "copy ownership");
            for (int index = 0; index <= 3; index++) {
                M3ArrayList<Integer> target = list(3);
                ArrayList<Integer> model = new ArrayList<>(target);
                check(target.addAll(index, copy) == model.addAll(index, copy), "bulk result");
                check(target.equals(model), "bulk logical size, not source capacity");
                target.add(99);
                check(copy.size() == n, "bulk ownership");
            }
        }
        M3ArrayList<Integer> owner = list(6);
        owner.addAll(2, owner);
        check(owner.equals(List.of(0, 1, 0, 1, 2, 3, 4, 5, 2, 3, 4, 5)), "self-add snapshot");
        owner = list(6);
        owner.addAll(1, owner.subList(2, 5));
        check(owner.equals(List.of(0, 2, 3, 4, 1, 2, 3, 4, 5)), "aliased sublist snapshot");
        M3ArrayList<Object> foreign = new M3ArrayList<>(new AbstractCollection<String>() {
            @Override public Iterator<String> iterator() { return List.of("x").iterator(); }
            @Override public int size() { return 1; }
            @Override public Object[] toArray() { return new String[]{"x"}; }
        });
        foreign.add(3);
        check(foreign.equals(List.of("x", 3)), "foreign typed array normalized");
        M3ArrayList<Object> empty = new M3ArrayList<>();
        expect(IndexOutOfBoundsException.class, () -> empty.addAll(-1, null));
        expect(NullPointerException.class, () -> empty.addAll(0, null));
        M3ArrayList<Integer> nullable = new M3ArrayList<>(Arrays.asList(null, 1, null));
        check(new M3ArrayList<>(nullable).equals(nullable), "nulls copied");
        nullable.removeIf(value -> value == null);
        check(nullable.equals(List.of(1)), "null filtering");
    }
    private static void countedSnapshots() {
        M3CountedBag<Integer> bag = new M3CountedBag<>();
        for (int i = 0; i < 130; i++) bag.addOccurrences(i, i + 1);
        var whole = bag.sortedCounts(null);
        for (var source : List.of(whole, whole.slice(3, 65), whole.slice(0, 0))) {
            var copy = source.filtered(value -> true);
            check(copy != source, "fresh counted wrapper");
            check(Arrays.equals(copy.physicalIndexes(), source.physicalIndexes()), "counted order");
            check(copy.totalOccurrences() == source.totalOccurrences(), "counted multiplicity");
            int[] exported = copy.physicalIndexes();
            if (exported.length > 0) exported[0] = -1;
            check(Arrays.equals(copy.physicalIndexes(), source.physicalIndexes()), "defensive index export");
            check(source.filtered(value -> false).distinctSize() == 0, "empty counted projection");
            var even = source.filtered(value -> value % 2 == 0);
            for (int i = 0; i < even.distinctSize(); i++) {
                check(even.elementAt(i) % 2 == 0, "partial counted projection");
                check(even.countAt(i) == even.elementAt(i) + 1, "count/value correspondence");
            }
        }
    }
    public static void main(String[] args) {
        filtering(); failureAndReentrancy(); bulkCopies(); countedSnapshots();
        System.out.println("M3CollectionAllocationSelfTest checks=" + checks + " PASS seed=0x20261018");
    }
}
