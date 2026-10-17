// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import com.m3.collections.M3CollectionViews;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** One-pass capture, wrapper composition and original encounter-index differential proofs. */
public final class M3ReceiverInteropSelfTest {
    private static int checks;
    private M3ReceiverInteropSelfTest() { }
    private record Item(int id, int group, boolean visible) { }
    private static void eq(Object expected, Object actual) {
        checks++; if (!Objects.equals(expected, actual))
            throw new AssertionError("expected=" + expected + " actual=" + actual);
    }
    private static void ok(boolean condition) {
        checks++; if (!condition) throw new AssertionError("receiver interop failure");
    }
    /** A legitimate Collection that allows one iterator traversal, but no size/toArray calls. */
    private static final class OnePass<E> extends AbstractCollection<E> {
        private final List<E> values;
        private int count;
        OnePass(List<E> values) { this.values = values; }
        @Override public int size() { throw new AssertionError("size() must not be queried"); }
        @Override public Object[] toArray() { throw new AssertionError("toArray() must not be queried"); }
        @Override public <T> T[] toArray(T[] target) { throw new AssertionError("toArray(T[]) must not be queried"); }
        @Override public Iterator<E> iterator() {
            if (++count != 1) throw new AssertionError("source must be traversed exactly once");
            return values.iterator();
        }
    }
    private static void unSized() {
        List<Item> values = List.of(new Item(5, 1, true), new Item(2, 0, true),
                new Item(8, 1, false), new Item(1, 0, true));
        var keyed = M3Parallel.by(new OnePass<>(values), Item::group);
        eq(values, keyed.mapOrdered(x -> x));
        eq(List.of(1, 0), keyed.keys());
        Map<Integer, List<Item>> folded = keyed.foldByKey(ArrayList<Item>::new, (acc, item) -> {
            acc.add(item); return acc;
        });
        eq(List.of(values.get(0), values.get(2)), folded.get(1));
        eq(List.of(values.get(1), values.get(3)), folded.get(0));
        eq(values, M3Parallel.byInt(new OnePass<>(values), Item::group, 2).mapOrdered(x -> x));
        eq(values, M3Parallel.byKeys(new OnePass<>(values), row -> List.of(row.group())).mapOrdered(x -> x));
        eq(values, M3Parallel.byReadWrite(new OnePass<>(values), row -> List.of(row.group()),
                row -> List.<Integer>of()).mapOrdered(x -> x));
    }
    private static void masksAndViews() {
        List<Item> source = new ArrayList<>(List.of(new Item(9, 1, false), new Item(4, 0, true),
                new Item(3, 1, true), new Item(8, 0, false), new Item(7, 1, true),
                new Item(6, 0, true)));
        AtomicInteger predicates = new AtomicInteger();
        Collection<Item> filtered = M3CollectionViews.filtered(source, i -> {
            predicates.incrementAndGet(); return i.visible();
        });
        var grouped = M3Parallel.by(filtered, Item::group);
        eq(6, predicates.get()); // capture must not double-traverse a filtered owner
        eq(List.of(4, 3, 7, 6), grouped.mapOrdered(Item::id));
        eq(List.of(4, 6), grouped.filter(i -> i.group() == 0).stream().map(Item::id).toList());
        eq(List.of(0, 1), grouped.keys());
        eq(List.of(4, 6), grouped.foldByKey(ArrayList<Integer>::new, (acc, i) -> {
            acc.add(i.id()); return acc;
        }).get(0));
        source.add(new Item(10, 1, true));
        eq(List.of(4, 3, 7, 6), grouped.mapOrdered(Item::id)); // captured membership immutable

        NavigableSet<Integer> sorted = new TreeSet<>(Comparator.reverseOrder());
        sorted.addAll(List.of(1, 2, 3, 4, 5, 6, 7));
        var descending = M3CollectionViews.filtered(sorted, v -> (v & 1) == 1);
        eq(List.of(7, 5, 3, 1), M3Parallel.by(descending, x -> x % 3).mapOrdered(x -> x));
    }
    private static void nullAndEmpty() {
        eq(List.of(), M3Parallel.by(new OnePass<>(List.<String>of()), x -> x).mapOrdered(x -> x));
        var p = M3Parallel.by(new OnePass<>(java.util.Arrays.asList(null, "a", null)), x -> x);
        eq(java.util.Arrays.asList(null, "a", null), p.mapOrdered(x -> x));
        eq(2, p.keys().size());
        ok(p.keys().get(0) == null);
    }
    private static void nativeLayout(String[] args) {
        if (args.length != 1 || !args[0].equals("--native")) return;
        List<Integer> values = List.of(7, 5, 2, 5, 7, 2);
        var p = M3Parallel.by(new OnePass<>(values), i -> i % 2, M3Parallel.Backend.NATIVE);
        eq(values, p.mapOrdered(i -> i));
        eq(List.of(1, 0), p.keys());
    }
    public static void main(String[] args) {
        unSized(); masksAndViews(); nullAndEmpty(); nativeLayout(args);
        System.out.println("PASS M3_RECEIVER_INTEROP checks=" + checks);
    }
}
