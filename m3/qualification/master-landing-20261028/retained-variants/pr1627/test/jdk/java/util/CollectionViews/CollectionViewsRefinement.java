/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/*
 * @test
 * @summary Stored occurrences, captured admission, sorted metadata and nested callbacks
 * @run main CollectionViewsRefinement
 * @run main/othervm CollectionViewsRefinement size
 */

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class CollectionViewsRefinement {
    private static int checks;
    private CollectionViewsRefinement() { }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static void check(boolean condition, String label) { equal(true, condition, label); }
    private static final class Value {
        private final int key;
        private final boolean visible;
        private Value(int key, boolean visible) { this.key = key; this.visible = visible; }
        @Override public boolean equals(Object other) { return other instanceof Value v && key == v.key; }
        @Override public int hashCode() { return key; }
    }

    private static void occurrences() {
        for (boolean copyOnWrite : List.of(false, true)) {
            Value hidden = new Value(1, false);
            Value admitted = new Value(1, true);
            List<Value> owner = copyOnWrite ? new CopyOnWriteArrayList<>() : new ArrayList<>();
            owner.add(hidden); owner.add(admitted);
            Collection<Value> view = CollectionViews.filtered(owner, value -> value.visible);
            check(view.contains(new Value(1, false)), "stored admitted occurrence decides membership");
            check(view.remove(new Value(1, false)), "remove admitted equal occurrence");
            equal(1, owner.size(), "one physical removal");
            check(owner.getFirst() == hidden, "hidden equal occurrence preserved");
            check(!view.contains(new Value(1, true)), "visible probe cannot expose hidden occurrence");
            check(!view.remove(new Value(1, true)), "visible probe cannot delete hidden occurrence");
        }
    }

    private static void admission() {
        List<Integer> owner = new ArrayList<>();
        AtomicInteger traversals = new AtomicInteger();
        AtomicInteger callbacks = new AtomicInteger();
        Collection<Integer> changingInput = new AbstractCollection<>() {
            @Override public int size() { throw new AssertionError("no input size prepass"); }
            @Override public Iterator<Integer> iterator() {
                return (traversals.getAndIncrement() == 0 ? List.of(2, 4) : List.of(3, 5)).iterator();
            }
        };
        Collection<Integer> view = CollectionViews.filtered(owner, value -> {
            callbacks.incrementAndGet(); return value % 2 == 0;
        });
        check(view.addAll(changingInput), "captured admitted batch accepted");
        equal(List.of(2, 4), owner, "only first observed input sequence written");
        equal(1, traversals.get(), "input traversed once");
        equal(2, callbacks.get(), "one predicate call per incoming occurrence");
        try {
            view.addAll(List.of(6, 7, 8));
            throw new AssertionError("rejected batch accepted");
        } catch (IllegalArgumentException expected) {
            equal(List.of(2, 4), owner, "rejected batch writes no prefix");
        }
    }

    private static void metadata(Spliterator<Integer> cursor, Comparator<? super Integer> order) {
        check(cursor.hasCharacteristics(Spliterator.SORTED | Spliterator.ORDERED | Spliterator.DISTINCT),
                "sorted metadata retained for every split");
        check(cursor.getComparator() == order, "comparator object retained");
        check(!cursor.hasCharacteristics(Spliterator.SIZED), "filtered cardinality remains unknown");
        Spliterator<Integer> prefix = cursor.trySplit();
        if (prefix != null) { metadata(prefix, order); metadata(cursor, order); }
        else cursor.forEachRemaining(value -> check(value % 2 == 0, "split admits only selected values"));
    }

    private static void sortedSplits() {
        Comparator<Integer> order = Comparator.reverseOrder();
        TreeSet<Integer> owner = new TreeSet<>(order);
        for (int i = 0; i < 257; i++) owner.add(i);
        metadata(CollectionViews.filtered(owner, value -> value % 2 == 0).spliterator(), order);
        var descending = CollectionViews.filtered(owner.descendingSet(), value -> value % 2 == 0);
        metadata(descending.spliterator(), descending.comparator());
    }

    private static void reentrant() {
        AtomicReference<Spliterator<Integer>> cursor = new AtomicReference<>();
        List<Integer> seen = new ArrayList<>();
        cursor.set(CollectionViews.filtered(List.of(1, 2, 3), value -> {
            if (value == 1) check(cursor.get().tryAdvance(seen::add), "nested public advance");
            return true;
        }).spliterator());
        check(cursor.get().tryAdvance(seen::add), "outer public advance");
        cursor.get().forEachRemaining(seen::add);
        equal(List.of(2, 1, 3), seen, "public callbacks have separate invocation state");
        Spliterator<Integer> only = CollectionViews.filtered(List.of(7), value -> true).spliterator();
        check(only.tryAdvance(value -> check(!only.tryAdvance(ignored -> {
            throw new AssertionError("nested exhausted cursor");
        }), "public nested exhaustion")), "public outer delivery survives nested exhaustion");
    }

    /** Explicit large logical-count mode; no element array or per-entry allocation. */
    private static void saturatedSize() {
        long length = (long) Integer.MAX_VALUE + 2;
        long[] calls = {0};
        Collection<Integer> source = new AbstractCollection<>() {
            @Override public int size() { throw new AssertionError("source size prepass"); }
            @Override public Iterator<Integer> iterator() {
                return new Iterator<>() {
                    private long index;
                    @Override public boolean hasNext() { return index < length; }
                    @Override public Integer next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        index++; return 1;
                    }
                };
            }
        };
        equal(Integer.MAX_VALUE, CollectionViews.filtered(source, value -> {
            calls[0]++; return true;
        }).size(), "Collection.size saturates at Integer.MAX_VALUE");
        equal(length, calls[0], "saturation does not suppress later callbacks");
    }

    public static void main(String[] args) {
        checks = 0;
        String mode = args.length == 0 ? "all" : args[0];
        switch (mode) {
            case "occurrences" -> occurrences();
            case "admission" -> admission();
            case "splits" -> sortedSplits();
            case "reentrant" -> reentrant();
            case "size" -> saturatedSize();
            case "all" -> { occurrences(); admission(); sortedSplits(); reentrant(); }
            default -> throw new IllegalArgumentException(mode);
        }
        System.out.println("PASS CollectionViewsRefinement mode=" + mode + " checks=" + checks);
    }
}
