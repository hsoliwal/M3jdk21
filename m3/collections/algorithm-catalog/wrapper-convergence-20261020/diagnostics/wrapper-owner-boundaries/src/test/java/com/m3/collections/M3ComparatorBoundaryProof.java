// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.function.Supplier;

/** Isolated diagnostic witnesses; does not modify or qualify product code. */
public final class M3ComparatorBoundaryProof {
    private M3ComparatorBoundaryProof() { }

    private static Object outcome(Supplier<?> operation) {
        try { return operation.get(); }
        catch (RuntimeException | Error failure) { return failure; }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static String describe(Object result, Throwable marker) {
        return result == marker ? "same caller exception" : String.valueOf(result);
    }

    private static void originalVersusAnchor() {
        IllegalArgumentException marker = new IllegalArgumentException("caller marker");
        ArrayList<String> tape = new ArrayList<>();
        boolean[] armed = {false};
        Comparator<Integer> comparator = (left, right) -> {
            if (armed[0]) {
                tape.add(left + ":" + right);
                if (left == 5 && right == 2) throw marker;
            }
            return Integer.compare(left, right);
        };
        TreeMap<Integer, Integer> root = new TreeMap<>(comparator);
        for (int key : new int[] {2, 4, 6, 8}) root.put(key, key);
        NavigableMap<Integer, Integer> bounded = root.subMap(2, true, 8, false);
        armed[0] = true;
        Object range = outcome(() -> bounded.headMap(5, true).descendingMap().entrySet().iterator());
        check(range == marker, "original range operation throws caller marker");
        check(tape.equals(List.of("5:2")), "original failure callback tape");
        System.out.println("original-range result=" + describe(range, marker) + " tape=" + tape);
        tape.clear();
        Object anchored = outcome(() -> bounded.floorEntry(5));
        check(anchored != marker, "neighbor anchor misses original failure");
        System.out.println("neighbor-anchor result=" + describe(anchored, marker) + " tape=" + tape);
    }

    private static final class Endpoint implements Comparable<Object> {
        private final int value;
        private final ArrayList<String> tape;
        private final IllegalArgumentException marker;
        Endpoint(int value, ArrayList<String> tape, IllegalArgumentException marker) {
            this.value = value;
            this.tape = tape;
            this.marker = marker;
        }
        @Override public int compareTo(Object right) {
            tape.add("endpoint:" + (right == this ? "self" : right));
            if (Integer.valueOf(5).equals(right)) throw marker;
            return Integer.compare(value, right == this ? value : ((Number) right).intValue());
        }
    }

    private static void safeQueryDoesNotProveSafeBounds() {
        ArrayList<String> tape = new ArrayList<>();
        IllegalArgumentException marker = new IllegalArgumentException("endpoint-owned marker");
        TreeMap<Object, Integer> root = new TreeMap<>();
        NavigableMap<Object, Integer> bounded = root.headMap(new Endpoint(8, tape, marker), false);
        check(bounded.comparator() == null, "natural-order submap");
        check(bounded.isEmpty(), "legal empty bounded map");
        tape.clear();
        Object direct = outcome(() -> bounded.headMap(5, false));
        check(direct == marker, "final Integer query still invokes arbitrary retained endpoint");
        check(tape.equals(List.of("endpoint:5")), "endpoint callback tape");
        System.out.println("natural-order exact-JDK-submap class=" + bounded.getClass().getName()
                + " query-class=" + Integer.valueOf(5).getClass().getName()
                + " result=" + describe(direct, marker) + " tape=" + tape);
        tape.clear();
        NavigableMap<Object, Integer> selected = M3FilteredViews.filterEntries(bounded, (key, value) -> true);
        Object filtered = outcome(() -> selected.lowerEntry(5));
        check(filtered == null, "KNOWN DEFECT: filtered map swallows the endpoint's exception");
        check(tape.equals(List.of("endpoint:5", "endpoint:self")),
                "KNOWN DEFECT: map recovery invokes the retained endpoint again after its failure");
        System.out.println("filtered-natural-endpoint result=" + describe(filtered, marker) + " tape=" + tape);
        tape.clear();
        NavigableSet<Object> selectedSet = M3MutableFilteredViews.filter(bounded.navigableKeySet(), value -> true);
        Object filteredSet = outcome(() -> selectedSet.lower(5));
        check(filteredSet == null, "KNOWN DEFECT: mutable filtered set swallows the endpoint's exception");
        check(tape.equals(List.of("endpoint:5")), "the swallowed set failure came from the retained endpoint");
        System.out.println("mutable-natural-endpoint result=" + describe(filteredSet, marker) + " tape=" + tape);
    }

    /** A public root subclass can hide the actual comparator behind an exact JDK submap. */
    private static final class ComparatorFacade extends TreeMap<Integer, Integer> {
        @java.io.Serial private static final long serialVersionUID = 1L;
        ComparatorFacade(Comparator<Integer> actual) { super(actual); }
        @Override public Comparator<? super Integer> comparator() { return null; }
    }

    private static void exactSubmapDoesNotProveExactRoot() {
        ArrayList<String> tape = new ArrayList<>();
        IllegalArgumentException marker = new IllegalArgumentException("hidden comparator marker");
        boolean[] armed = {false};
        Comparator<Integer> comparator = (left, right) -> {
            if (armed[0]) {
                tape.add(left + ":" + right);
                if (left == 5 && right == 2) throw marker;
            }
            return Integer.compare(left, right);
        };
        ComparatorFacade root = new ComparatorFacade(comparator);
        for (int key : new int[] {2, 4, 6, 8}) root.put(key, key);
        NavigableMap<Integer, Integer> bounded = root.subMap(2, true, 8, false);
        check(bounded.comparator() == null, "submap delegates exposed comparator to root override");
        armed[0] = true;
        Object failure = outcome(() -> bounded.headMap(5, true));
        check(failure == marker, "submap uses hidden actual comparator for range validation");
        check(tape.equals(List.of("5:2")), "hidden comparator failure callback tape");
        System.out.println("overridden-root exact-submap class=" + bounded.getClass().getName()
                + " result=" + describe(failure, marker) + " tape=" + tape);
    }

    private static void capturedRangeExceptionDoesNotProveCurrentOrigin() {
        ArrayList<String> tape = new ArrayList<>();
        IllegalArgumentException[] marker = {null};
        Comparator<Integer> comparator = (left, right) -> {
            if (marker[0] != null) {
                tape.add(left + ":" + right);
                if (left == 5 && right == 2) throw marker[0];
            }
            return Integer.compare(left, right);
        };
        TreeMap<Integer, Integer> root = new TreeMap<>(comparator);
        for (int key : new int[] {2, 4, 6, 8}) root.put(key, key);
        NavigableMap<Integer, Integer> bounded = root.subMap(2, true, 8, false);
        Object original = outcome(() -> bounded.headMap(-1, true));
        check(original instanceof IllegalArgumentException, "obtain a genuine JDK-owned range exception");
        marker[0] = (IllegalArgumentException) original;
        Object current = outcome(() -> bounded.headMap(5, true));
        check(current == original, "caller comparator can rethrow the exact previously captured range exception");
        check(tape.equals(List.of("5:2")), "replayed object now originates in current comparator callback");
        System.out.println("replayed-range-exception same-object=true current-origin=comparator tape=" + tape);
    }

    public static void main(String[] args) {
        originalVersusAnchor();
        safeQueryDoesNotProveSafeBounds();
        exactSubmapDoesNotProveExactRoot();
        capturedRangeExceptionDoesNotProveCurrentOrigin();
        System.out.println("DIAGNOSTIC OBSERVATIONS REPRODUCED comparator failure boundaries; not acceptance");
    }
}
