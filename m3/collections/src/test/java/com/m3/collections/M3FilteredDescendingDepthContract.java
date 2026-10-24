/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Independent descending results, callback tapes, and a deep private-owner witness. */
public final class M3FilteredDescendingDepthContract {
    private static int checks;
    private M3FilteredDescendingDepthContract() { }

    private static void check(boolean valid, String label) {
        checks++;
        if (!valid) throw new AssertionError(label);
    }

    private static final class ObservedTree extends TreeSet<Integer> {
        private static final long serialVersionUID = 1L;
        private final transient List<String> tape;
        ObservedTree(Comparator<Integer> order, List<String> tape) {
            super(order);
            this.tape = tape;
        }
        @Override public NavigableSet<Integer> descendingSet() {
            tape.add("owner.descendingSet");
            return super.descendingSet();
        }
        @Override public Iterator<Integer> descendingIterator() {
            tape.add("owner.descendingIterator");
            return super.descendingIterator();
        }
    }

    private static Predicate<Integer> filter(List<String> tape, String name,
                                              Predicate<Integer> accepted) {
        return value -> {
            tape.add(name + ":" + value);
            return accepted.test(value);
        };
    }

    private static void readonly(Iterator<Integer> cursor) {
        try { cursor.remove(); throw new AssertionError("writable descending iterator"); }
        catch (UnsupportedOperationException expected) { checks++; }
    }

    private static List<Integer> collect(Iterator<Integer> cursor) {
        List<Integer> output = new ArrayList<>();
        readonly(cursor);
        while (cursor.hasNext()) {
            check(cursor.hasNext(), "lookahead remains ready");
            output.add(cursor.next());
            readonly(cursor);
        }
        check(!cursor.hasNext(), "terminal exhaustion remains sticky");
        try { cursor.next(); throw new AssertionError("terminal next accepted"); }
        catch (NoSuchElementException expected) { checks++; }
        readonly(cursor);
        return output;
    }

    private static void bounded() {
        Comparator<Integer> middleNull = (left, right) -> {
            if (left == right) return 0;
            if (left == null) return right <= 0 ? 1 : -1;
            if (right == null) return left <= 0 ? -1 : 1;
            return Integer.compare(left, right);
        };
        List<Comparator<Integer>> orders = Arrays.asList(null, Comparator.reverseOrder(),
                Comparator.nullsFirst(Comparator.naturalOrder()), middleNull);
        List<List<Integer>> wanted = List.of(List.of(1, -1), List.of(-1, 1),
                Arrays.asList(1, -1, null), Arrays.asList(1, null, -1));
        // Hand-derived tapes also lock multiplicity, short-circuit order and null placement.
        List<List<String>> expectedTapes = List.of(
                List.of("owner.descendingSet", "inner:3", "middle:3", "outer:3",
                        "inner:2", "middle:2", "inner:1", "middle:1", "outer:1",
                        "inner:0", "middle:0", "inner:-1", "middle:-1", "outer:-1", "inner:-2"),
                List.of("owner.descendingSet", "inner:-2", "inner:-1", "middle:-1", "outer:-1",
                        "inner:0", "middle:0", "inner:1", "middle:1", "outer:1",
                        "inner:2", "middle:2", "inner:3", "middle:3", "outer:3"),
                List.of("owner.descendingSet", "inner:3", "middle:3", "outer:3",
                        "inner:2", "middle:2", "inner:1", "middle:1", "outer:1",
                        "inner:0", "middle:0", "inner:-1", "middle:-1", "outer:-1", "inner:-2",
                        "inner:null", "middle:null", "outer:null"),
                List.of("owner.descendingSet", "inner:3", "middle:3", "outer:3",
                        "inner:2", "middle:2", "inner:1", "middle:1", "outer:1",
                        "inner:null", "middle:null", "outer:null", "inner:0", "middle:0",
                        "inner:-1", "middle:-1", "outer:-1", "inner:-2"));
        for (int shape = 0; shape < orders.size(); shape++) {
            List<String> tape = new ArrayList<>();
            ObservedTree owner = new ObservedTree(orders.get(shape), tape);
            owner.addAll(List.of(-2, -1, 0, 1, 2, 3));
            if (shape >= 2) owner.add(null);
            NavigableSet<Integer> view = M3FilteredViews.filter(owner,
                    filter(tape, "inner", x -> x == null || x >= -1));
            view = M3FilteredViews.filter(view,
                    filter(tape, "middle", x -> x == null || (x & 1) != 0));
            view = M3FilteredViews.filter(view,
                    filter(tape, "outer", x -> x == null || x <= 2));
            Iterator<Integer> cursor = view.descendingIterator();
            check(tape.equals(List.of("owner.descendingSet")), "construction delegates once without callbacks");
            check(collect(cursor).equals(wanted.get(shape)), "hand-derived order and null membership " + shape);
            check(tape.equals(expectedTapes.get(shape)), "descending callback tape " + shape);
            System.out.println("SHAPE " + shape + " " + tape);

            // A range is established before the private filter chain; its bounds remain live.
            NavigableSet<Integer> range = orders.get(shape) == null
                    ? owner.subSet(-1, true, 2, true) : owner;
            NavigableSet<Integer> ranged = M3FilteredViews.filter(range, x -> x == null || x != 0);
            ranged = M3FilteredViews.filter(ranged, x -> true);
            List<Integer> rangedExpected = new ArrayList<>(range.descendingSet());
            rangedExpected.remove(Integer.valueOf(0));
            check(collect(ranged.descendingIterator()).equals(rangedExpected), "JDK range oracle " + shape);
            range = owner.descendingSet().tailSet(new ArrayList<>(owner).get(owner.size() / 2), true);
            ranged = M3FilteredViews.filter(range, x -> x == null || x != 0);
            ranged = M3FilteredViews.filter(ranged, x -> true);
            rangedExpected = new ArrayList<>(range.descendingSet());
            rangedExpected.remove(Integer.valueOf(0));
            check(collect(ranged.descendingIterator()).equals(rangedExpected), "JDK descending range oracle " + shape);
            check(owner.size() == (shape >= 2 ? 7 : 6), "all read-only probes retain owner");
        }

        List<List<String>> boundaryTapes = List.of(
                List.of("owner.descendingSet", "inner:3", "outer:3", "inner:2", "outer:2",
                        "inner:1", "outer:1"),
                List.of("owner.descendingSet", "facade:3", "inner:3", "outer:3", "facade:2",
                        "inner:2", "outer:2", "facade:1", "inner:1", "outer:1"));
        for (int mode = 0; mode < 2; mode++) {
            List<String> boundaryTape = new ArrayList<>();
            ObservedTree boundaryOwner = new ObservedTree(null, boundaryTape);
            boundaryOwner.addAll(List.of(1, 2, 3));
            NavigableSet<Integer> boundary = mode == 0
                    ? Collections.unmodifiableNavigableSet(boundaryOwner)
                    : M3FilteredNavigableSet.view(boundaryOwner, filter(boundaryTape, "facade", x -> true));
            NavigableSet<Integer> nested = M3FilteredViews.filter(boundary,
                    filter(boundaryTape, "inner", x -> true));
            nested = M3FilteredViews.filter(nested,
                    filter(boundaryTape, "outer", x -> (x & 1) != 0));
            check(collect(nested.descendingIterator()).equals(List.of(3, 1)), "opaque boundary values " + mode);
            check(boundaryTape.equals(boundaryTapes.get(mode)), "opaque boundary callback tape " + mode);
            System.out.println("BOUNDARY " + mode + " " + boundaryTape);
        }

        List<String> tape = new ArrayList<>();
        TreeSet<Integer> owner = new TreeSet<>(List.of(1, 2, 3));
        RuntimeException sentinel = new IllegalArgumentException("predicate marker");
        NavigableSet<Integer> view = M3FilteredViews.filter(owner, filter(tape, "inner", x -> true));
        view = M3FilteredViews.filter(view, filter(tape, "middle", x -> {
            if (x == 2) throw sentinel;
            return true;
        }));
        view = M3FilteredViews.filter(view, filter(tape, "outer", x -> true));
        Iterator<Integer> cursor = view.descendingIterator();
        check(cursor.next() == 3, "first value precedes throw");
        try { cursor.hasNext(); throw new AssertionError("predicate failure missing"); }
        catch (RuntimeException failure) { check(failure == sentinel, "same callback failure object"); }
        check(tape.equals(List.of("inner:3", "middle:3", "outer:3", "inner:2", "middle:2")),
                "inner-to-outer short-circuit and failure prefix");
        check(cursor.next() == 1, "failed candidate remains consumed as before");
        check(tape.equals(List.of("inner:3", "middle:3", "outer:3", "inner:2", "middle:2",
                "inner:1", "middle:1", "outer:1")), "resumed callback tape after same failure");
        System.out.println("THROW " + tape);

        owner = new TreeSet<>(List.of(1, 2, 3));
        view = M3FilteredViews.filter(owner, x -> true);
        view = M3FilteredViews.filter(view, x -> true);
        cursor = view.descendingIterator();
        check(cursor.next() == 3, "first descending delivery");
        owner.add(4);
        try { cursor.hasNext(); throw new AssertionError("lost owner fail-fast"); }
        catch (ConcurrentModificationException expected) { checks++; }
        check(collect(M3FilteredViews.filter(new TreeSet<Integer>(), x -> true)
                .descendingIterator()).isEmpty(), "empty traversal");
        System.out.println("FILTERED_DESCENDING_BOUNDED_PASS checks=" + checks);
    }

    private static void deep() {
        final int depth = 65_536;
        int[] count = {0};
        NavigableSet<Integer> view = new TreeSet<>(List.of(1, 2, 3));
        for (int layer = 0; layer < depth; layer++) {
            final int ordinal = layer;
            view = M3FilteredViews.filter(view, value -> {
                check(count[0] % depth == ordinal, "deep inner-to-outer predicate order");
                count[0]++;
                return true;
            });
        }
        check(count[0] == 0, "deep construction is lazy");
        Iterator<Integer> cursor = view.descendingIterator();
        check(count[0] == 0, "deep iterator construction is lazy");
        check(collect(cursor).equals(List.of(3, 2, 1)), "deep descending values");
        check(count[0] == depth * 3, "one predicate occurrence per retained value");
        System.out.println("FILTERED_DESCENDING_DEPTH_PASS depth=" + depth + " checks=" + checks);
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("bounded")) bounded();
        else deep();
    }
}
