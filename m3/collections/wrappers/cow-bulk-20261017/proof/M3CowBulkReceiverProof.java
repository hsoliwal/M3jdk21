/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Recipe fixture adapted to the existing Synexia filtered collection owner.
 */
package com.m3.collections;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/** Standalone Java 21 / optional real JNI regression for filtered bulk mutation. */
public final class M3CowBulkReceiverProof {
    private static long checks;
    private static boolean nativeEnabled;

    private M3CowBulkReceiverProof() {}

    public static void main(String[] args) {
        if (args.length == 1 && "--red".equals(args[0])) {
            redPreimage();
            return;
        }
        if (args.length > 0) {
            if (args.length != 1) throw new IllegalArgumentException("native library path");
            System.load(Path.of(args[0]).toAbsolutePath().normalize().toString());
            nativeEnabled = true;
        }
        copyOnWriteList();
        copyOnWriteSet();
        nestedView();
        sortedOwner();
        predicateExceptionIdentity();
        failures();
        System.out.println("M3_COW_BULK_GREEN Java21 checks=" + checks + " jni=" + nativeEnabled);
    }

    /** Red control: old view must reproduce Iterator.remove unsupported on COW. */
    private static void redPreimage() {
        Collection<Integer> filtered = M3CollectionViews.filtered(
                (Collection<Integer>) new CopyOnWriteArrayList<>(List.of(1, 2, 3, 4)),
                value -> value % 2 == 0);
        try {
            filtered.clear();
            throw new AssertionError("original copy-on-write defect not reproduced");
        } catch (UnsupportedOperationException expected) {
            System.out.println("M3_COW_BULK_RED reproduced Iterator.remove refusal");
        }
    }

    private static void copyOnWriteList() {
        CopyOnWriteArrayList<Integer> source = new CopyOnWriteArrayList<>();
        source.addAll(java.util.Arrays.asList(null, 1, 2, 3, 4, 5, 6));
        Collection<Integer> view = M3CollectionViews.filtered(
                (Collection<Integer>) source, value -> value == null || value % 2 == 0);
        equal(view, java.util.Arrays.asList(null, 2, 4, 6), "initial null and visible values");
        check(view.removeIf(value -> value != null && value >= 4), "removeIf changed list");
        equal(view, java.util.Arrays.asList(null, 2), "post-removeIf view");
        equal(source, java.util.Arrays.asList(null, 1, 2, 3, 5), "hidden values survive removeIf");
        check(view.removeAll(List.of(2)), "removeAll changed list");
        equal(view, java.util.Arrays.asList((Integer) null), "post-removeAll view");
        check(!view.retainAll(java.util.Arrays.asList((Integer) null)), "retainAll no-op");
        view.clear();
        equal(source, List.of(1, 3, 5), "clear preserves hidden list values");
        equal(view, List.of(), "post-clear view");
        check(!view.removeIf(value -> true), "removeIf empty visible set");

        CopyOnWriteArrayList<Integer> duplicate = new CopyOnWriteArrayList<>(List.of(2, 2, 3, 4, 4));
        Collection<Integer> even = M3CollectionViews.filtered((Collection<Integer>) duplicate,
                value -> value % 2 == 0);
        check(even.removeIf(value -> value == 2), "removeIf duplicate occurrences");
        equal(duplicate, List.of(3, 4, 4), "retain encounter order and duplicates");
    }

    private static void copyOnWriteSet() {
        CopyOnWriteArraySet<Integer> source = new CopyOnWriteArraySet<>(List.of(1, 2, 3, 4, 5, 6));
        Collection<Integer> view = M3CollectionViews.filtered(
                (Collection<Integer>) source, value -> value % 2 == 0);
        equal(view, List.of(2, 4, 6), "set projection");
        check(view.removeIf(value -> value >= 4), "set removeIf");
        equal(source, List.of(1, 2, 3, 5), "set preserves hidden values");
        check(view.retainAll(List.of(1)), "set retainAll removes remaining visible");
        equal(source, List.of(1, 3, 5), "set retainAll hidden elements remain");
        view.clear();
        equal(source, List.of(1, 3, 5), "empty view clear has no hidden effects");
    }

    private static void nestedView() {
        CopyOnWriteArrayList<Integer> source = new CopyOnWriteArrayList<>(List.of(2, 4, 6, 8));
        Collection<Integer> a = M3CollectionViews.filtered((Collection<Integer>) source,
                value -> value > 2);
        Collection<Integer> b = M3CollectionViews.filtered(a, value -> value < 8);
        equal(b, List.of(4, 6), "nested order");
        check(b.removeAll(List.of(4)), "nested removeAll");
        equal(source, List.of(2, 6, 8), "nested hidden members");
        check(b.retainAll(List.of(7)), "nested retainAll");
        equal(source, List.of(2, 8), "nested backing identity");
    }

    private static void sortedOwner() {
        NavigableSet<Integer> owner = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6, 7));
        NavigableSet<Integer> view = M3CollectionViews.filtered(owner, value -> value % 2 == 0);
        equal(view, List.of(2, 4, 6), "sorted view");
        equal(view.descendingSet(), List.of(6, 4, 2), "descending view");
        check(view.comparator() == null, "retain natural comparator");
        check(view.ceiling(3) == 4, "ceiling");
        check(view.floor(5) == 4, "floor");
        NavigableSet<Integer> range = view.subSet(3, false, 7, true);
        equal(range, List.of(4, 6), "sorted range view");
        check(range.removeIf(value -> value == 4), "sorted range original removal path");
        equal(owner, List.of(1, 2, 3, 5, 6, 7), "unaffected hidden sorted members");
        check(view.lower(6) == 2, "lower after removal");
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> descending = new TreeSet<>(reverse);
        descending.addAll(owner);
        NavigableSet<Integer> filtered = M3CollectionViews.filtered(descending, value -> value >= 3);
        equal(filtered, List.of(7, 6, 5, 3), "custom descending comparator");
    }

    private static void predicateExceptionIdentity() {
        RuntimeException sentinel = new RuntimeException("predicate sentinel");
        CopyOnWriteArrayList<Integer> owner = new CopyOnWriteArrayList<>(List.of(1, 2, 3, 4));
        Collection<Integer> view = M3CollectionViews.filtered(
                (Collection<Integer>) owner, value -> value % 2 == 0);
        try {
            view.removeIf(value -> {
                if (value == 4) throw sentinel;
                return true;
            });
            throw new AssertionError("sentinel exception should propagate");
        } catch (RuntimeException caught) {
            check(caught == sentinel, "exception object identity");
        }
        equal(owner, List.of(1, 2, 3, 4), "COW bulk mutation refuses partial predicate failures");
    }

    private static void failures() {
        Collection<Integer> view = M3CollectionViews.filtered(
                (Collection<Integer>) new CopyOnWriteArrayList<>(List.of(1, 2)), value -> true);
        try {
            view.removeIf(null);
            throw new AssertionError("null predicate accepted");
        } catch (NullPointerException expected) {
            checks++;
        }
        try {
            view.iterator().remove();
            throw new AssertionError("filtered iterator removal accepted");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }

    private static void equal(Collection<Integer> actual, List<Integer> expected, String label) {
        if (!Objects.equals(new ArrayList<>(actual), expected)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
        checks++;
        if (nativeEnabled) {
            long encoded = nativeCountAndSum(actual);
            long count = encoded >>> 32;
            long sum = (int) encoded;
            long expectedSum = actual.stream().filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
            check(count == actual.size() && sum == expectedSum,
                    "native iterator digest parity: " + label);
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static native long nativeCountAndSum(Collection<Integer> source);
}
