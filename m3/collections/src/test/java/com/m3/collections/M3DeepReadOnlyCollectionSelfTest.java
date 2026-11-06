/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Predicate;

/** Explicit decorator stacks retain demand order without storing backing elements. */
public final class M3DeepReadOnlyCollectionSelfTest {
    private static long checks;

    private M3DeepReadOnlyCollectionSelfTest() { }

    private static void equal(Object expected, Object actual, String message) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": " + expected + " != " + actual);
    }

    private static <E> Collection<E> nested(Collection<E> source, Predicate<? super E> predicate, int depth) {
        Collection<E> view = source;
        for (int index = 0; index < depth; index++) view = M3FilteredViews.filter(view, predicate);
        return view;
    }

    static void deepTraversal(int depth) {
        long[] calls = {0};
        ArrayList<Integer> source = new ArrayList<>(List.of(1));
        Collection<Integer> view = nested(source, value -> { calls[0]++; return value > 0; }, depth);
        equal(0L, calls[0], "construction is lazy");
        equal(1, view.size(), "deep size");
        equal((long) depth, calls[0], "one occurrence per depth during size");
        Iterator<Integer> cursor = view.iterator();
        equal(true, cursor.hasNext(), "deep hasNext");
        equal(true, cursor.hasNext(), "cached hasNext");
        equal(1, cursor.next(), "deep next");
        equal(2L * depth, calls[0], "cached iterator does not repeat predicates");
        equal(false, cursor.hasNext(), "deep exhaustion");
        equal(false, view.contains(99), "deep missing contains");
        equal(2L * depth, calls[0], "missing membership does not run visibility callbacks");
        source.clear();
        equal(true, view.isEmpty(), "deep view remains live");
        source.add(-1);
        equal(false, view.contains(-1), "deep rejected membership");
        equal(2L * depth + 1, calls[0], "inner failure short-circuits pending membership operations");

        Set<Integer> set = new java.util.LinkedHashSet<>(List.of(1));
        for (int index = 0; index < depth; index++) set = M3FilteredViews.filter(set, value -> true);
        equal(1, set.size(), "deep set size");
        equal(false, set.contains(99), "deep set missing membership");
        equal(List.of(1), new ArrayList<>(set), "deep set array projection");
    }

    static void membershipDemand() {
        for (int depth = 1; depth <= 512; depth += depth < 16 ? 1 : 31) {
            long[] calls = {0};
            Collection<Integer> view = nested(List.of(1), value -> { calls[0]++; return true; }, depth);
            equal(true, view.contains(1), "visible member");
            equal((long) depth * (depth + 1) / 2, calls[0], "retain repeated logical membership scans");
        }
        List<String> seen = new ArrayList<>();
        RuntimeException marker = new IllegalStateException("predicate failure");
        Collection<Integer> inner = M3FilteredViews.filter(Arrays.asList(null, 1, 2, 3), value -> {
            seen.add("inner:" + value);
            return value != null;
        });
        Collection<Integer> outer = M3FilteredViews.filter(inner, value -> {
            seen.add("outer:" + value);
            if (value == 1) throw marker;
            return value % 2 == 0;
        });
        Iterator<Integer> cursor = outer.iterator();
        try {
            cursor.hasNext();
            throw new AssertionError("predicate failure lost");
        } catch (RuntimeException failure) {
            equal(marker, failure, "exception identity");
        }
        equal(2, cursor.next(), "iterator resumes after failed element");
        equal(false, cursor.hasNext(), "remaining hidden tail");
        equal(List.of("inner:null", "inner:1", "outer:1", "inner:2", "outer:2", "inner:3", "outer:3"),
                seen, "callback order, short circuit and exception continuation");
    }

    private static final class Key {
        private final int id;
        private final String label;
        private final List<String> tape;
        private Key(int id, String label, List<String> tape) { this.id = id; this.label = label; this.tape = tape; }
        @Override public boolean equals(Object other) {
            tape.add("equals:" + label + ":" + other);
            return other instanceof Key key && id == key.id;
        }
        @Override public int hashCode() { tape.add("hash:" + label); return id; }
        @Override public String toString() { return label; }
    }

    private static final class ObservedCollection extends AbstractCollection<Key> {
        private final List<Key> source;
        private final List<String> tape;
        private ObservedCollection(List<Key> source, List<String> tape) { this.source = source; this.tape = tape; }
        @Override public boolean contains(Object value) { tape.add("owner.contains:" + value); return source.contains(value); }
        @Override public int size() { tape.add("owner.size"); return source.size(); }
        @Override public Iterator<Key> iterator() {
            tape.add("owner.iterator");
            Iterator<Key> raw = source.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { tape.add("owner.hasNext"); return raw.hasNext(); }
                @Override public Key next() { Key value = raw.next(); tape.add("owner.next:" + value); return value; }
            };
        }
    }

    /** Retains duplicate equals-equal references in a deterministic identity-set order. */
    private static final class ObservedIdentitySet extends AbstractSet<Key> {
        private final ObservedCollection source;
        private ObservedIdentitySet(ObservedCollection source) { this.source = source; }
        @Override public int size() { return source.size(); }
        @Override public Iterator<Key> iterator() { return source.iterator(); }
        @Override public boolean contains(Object value) {
            source.tape.add("identity.contains:" + value);
            for (Key key : source.source) if (key == value) return true;
            return false;
        }
    }

    /** Original and transformed owners must produce the same complete observable event tape. */
    static void trace() {
        for (boolean asSet : new boolean[] {false, true}) {
            List<String> tape = new ArrayList<>();
            Key hidden = new Key(1, "hidden", tape);
            Key retained = new Key(1, "retained", tape);
            Key alias = new Key(1, "alias", tape);
            Key second = new Key(2, "second", tape);
            ObservedCollection raw = new ObservedCollection(Arrays.asList(null, hidden, retained, second), tape);
            Collection<Key> selected = asSet ? new ObservedIdentitySet(raw) : raw;
            for (int stage = 0; stage < 5; stage++) {
                int index = stage;
                Predicate<Key> predicate = value -> {
                    tape.add("predicate:" + index + ":" + value);
                    return value != null && value != hidden;
                };
                selected = asSet ? M3FilteredViews.filter((Set<Key>) selected, predicate)
                        : M3FilteredViews.filter(selected, predicate);
            }
            for (int operation = 0; operation < 11; operation++) {
                tape.clear();
                Object result = switch (operation) {
                    case 0 -> selected.size();
                    case 1 -> selected.isEmpty();
                    case 2 -> selected.contains(alias);
                    case 3 -> selected.contains(retained);
                    case 4 -> selected.contains(hidden);
                    case 5 -> selected.contains(null);
                    case 6 -> Arrays.toString(selected.toArray());
                    case 7 -> Arrays.toString(selected.toArray(new Key[5]));
                    case 8 -> {
                        Iterator<Key> cursor = selected.iterator();
                        List<String> values = new ArrayList<>();
                        values.add("has=" + cursor.hasNext());
                        values.add("cached=" + cursor.hasNext());
                        values.add("next=" + cursor.next());
                        cursor.forEachRemaining(value -> values.add("remaining=" + value));
                        try { cursor.next(); } catch (NoSuchElementException expected) { values.add("exhausted"); }
                        try { cursor.remove(); } catch (UnsupportedOperationException expected) { values.add("readonly"); }
                        yield values;
                    }
                    case 9 -> {
                        Spliterator<Key> cursor = selected.spliterator();
                        List<String> values = new ArrayList<>();
                        values.add("flags=" + cursor.characteristics());
                        values.add("estimate=" + cursor.estimateSize());
                        cursor.forEachRemaining(value -> values.add("value=" + value));
                        yield values;
                    }
                    case 10 -> selected.containsAll(List.of(retained, second));
                    default -> throw new AssertionError(operation);
                };
                System.out.println("TRACE " + asSet + ":" + operation + "=" + result + " " + tape);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--trace")) { trace(); return; }
        int depth = args.length == 0 ? 65536 : Integer.parseInt(args[0]);
        deepTraversal(depth);
        membershipDemand();
        System.out.println("PASS M3DeepReadOnlyCollectionSelfTest checks=" + checks + " depth=" + depth);
    }
}
