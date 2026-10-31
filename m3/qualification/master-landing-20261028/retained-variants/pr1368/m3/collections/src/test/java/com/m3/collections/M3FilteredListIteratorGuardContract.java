// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Maintained regression for the exact historical read-only iterator guard.
 * Reuses the admitted policy and read-effect assertions from Synexia #12289
 * and M3JDK21 #1291; pending traversal and fail-fast findings are excluded.
 */
public final class M3FilteredListIteratorGuardContract {
    private static int checks;

    private M3FilteredListIteratorGuardContract() { }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": reference identity differs");
    }

    private static void require(boolean accepted, String label) {
        checks++;
        if (!accepted) throw new AssertionError(label);
    }

    private static Throwable failure(Class<? extends Throwable> type,
            Runnable operation, String label) {
        checks++;
        try {
            operation.run();
        } catch (Throwable caught) {
            if (type.isInstance(caught)) return caught;
            throw new AssertionError(label + ": unexpected " + caught, caught);
        }
        throw new AssertionError(label + ": missing " + type.getSimpleName());
    }

    /** The ordinary iterator must use the same read-only boundary as listIterator. */
    private static void readOnlyRefusalAndIdentity() {
        String first = new String("equal");
        String second = new String("equal");
        List<String> owner = new ArrayList<>(Arrays.asList("hidden", null, first, second));
        AtomicInteger calls = new AtomicInteger();
        List<String> selected = M3FilteredLists.readOnlyFiltered(owner, value -> {
            calls.incrementAndGet();
            return value == null || value == first || value == second;
        });
        equal(0, calls.get(), "factory remains predicate-lazy");
        Iterator<String> cursor = selected.iterator();
        int preparedCalls = calls.get();
        failure(UnsupportedOperationException.class, cursor::remove, "remove before next");
        equal(preparedCalls, calls.get(), "read-only remove does not call predicates");
        equal(true, cursor.hasNext(), "null candidate exists");
        equal(true, cursor.hasNext(), "repeated lookahead");
        same(null, cursor.next(), "stored null");
        failure(UnsupportedOperationException.class, cursor::remove, "remove after null");
        same(first, cursor.next(), "first equal stored reference");
        same(second, cursor.next(), "second equal stored reference");
        equal(false, cursor.hasNext(), "iterator exhaustion");
        failure(UnsupportedOperationException.class, cursor::remove, "remove after exhaustion");
        equal(4, owner.size(), "owner unchanged by all refusals");
        same(first, owner.get(2), "first owner identity retained");
        same(second, owner.get(3), "second owner identity retained");

        List<Integer> empty = M3FilteredLists.readOnlyFiltered(new ArrayList<>(), value -> true);
        failure(UnsupportedOperationException.class, empty.iterator()::remove,
                "empty ordinary iterator removal");
        failure(UnsupportedOperationException.class, empty.reversed().iterator()::remove,
                "empty reversed ordinary iterator removal");
        failure(UnsupportedOperationException.class, empty.subList(0, 0).iterator()::remove,
                "empty ranged ordinary iterator removal");
    }

    /**
     * Freeze the old AbstractList read path using its unchanged filtered delegate.
     * The baseline intentionally has no iterator override; it is independent of
     * the new guard and does not import any proposed traversal optimization.
     */
    private static <E> List<E> baselineReadOnly(List<E> delegate) {
        return new AbstractList<>() {
            @Override public int size() { return delegate.size(); }
            @Override public E get(int index) { return delegate.get(index); }
        };
    }

    private static List<Object> readTrace(boolean candidate, boolean linked) {
        List<Integer> owner = linked
                ? new LinkedList<>(Arrays.asList(1, 2, null, 4, 5, 6))
                : new ArrayList<>(Arrays.asList(1, 2, null, 4, 5, 6));
        ArrayList<Integer> calls = new ArrayList<>();
        Predicate<Integer> predicate = value -> {
            calls.add(value);
            return value == null || (value & 1) == 0;
        };
        List<Integer> selected = candidate ? M3FilteredLists.readOnlyFiltered(owner, predicate)
                : baselineReadOnly(M3FilteredLists.filtered(owner, predicate));
        ArrayList<Object> trace = new ArrayList<>();
        Iterator<Integer> cursor = selected.iterator();
        trace.add(calls.size());
        failure(NullPointerException.class, () -> cursor.forEachRemaining(null),
                "forEachRemaining null action");
        trace.add(calls.size());
        trace.add(cursor.hasNext());
        trace.add(calls.size());
        trace.add(cursor.hasNext());
        trace.add(calls.size());
        trace.add(cursor.next());
        trace.add(calls.size());
        trace.add(cursor.hasNext());
        trace.add(calls.size());
        trace.add(cursor.next());
        trace.add(calls.size());
        ArrayList<Integer> remaining = new ArrayList<>();
        cursor.forEachRemaining(remaining::add);
        trace.add(remaining);
        trace.add(calls.size());
        trace.add(cursor.hasNext());
        trace.add(calls.size());
        trace.add(new ArrayList<>(calls));
        return trace;
    }

    private static Object outcome(Supplier<?> operation) {
        try { return operation.get(); }
        catch (RuntimeException | Error caught) { return caught; }
    }

    private static List<Object> predicateFailureTrace(boolean candidate,
            IllegalStateException marker) {
        ArrayList<Integer> owner = new ArrayList<>(List.of(1, 2, 4, 6));
        ArrayList<Integer> calls = new ArrayList<>();
        Predicate<Integer> predicate = value -> {
            calls.add(value);
            if (value == 4) throw marker;
            return (value & 1) == 0;
        };
        List<Integer> selected = candidate ? M3FilteredLists.readOnlyFiltered(owner, predicate)
                : baselineReadOnly(M3FilteredLists.filtered(owner, predicate));
        ArrayList<Object> trace = new ArrayList<>();
        Iterator<Integer> cursor = selected.iterator();
        trace.add(calls.size());
        trace.add(outcome(cursor::hasNext));
        trace.add(calls.size());
        trace.add(outcome(cursor::hasNext));
        trace.add(calls.size());
        // A failed size scan must not silently poison the original indexed cursor.
        trace.add(outcome(cursor::next));
        trace.add(calls.size());
        trace.add(outcome(cursor::hasNext));
        trace.add(calls.size());
        trace.add(outcome(cursor::next));
        trace.add(calls.size());
        trace.add(outcome(cursor::next));
        trace.add(calls.size());
        trace.add(new ArrayList<>(calls));
        return trace;
    }

    private static void readOnlyPreservedReadEffects() {
        for (boolean linked : new boolean[] {false, true}) {
            equal(readTrace(false, linked), readTrace(true, linked),
                    "ordinary read callback tape, linked=" + linked);
        }
        IllegalStateException marker = new IllegalStateException("same predicate-owned failure");
        List<Object> expected = predicateFailureTrace(false, marker);
        List<Object> actual = predicateFailureTrace(true, marker);
        equal(expected, actual, "predicate callback order, failure identity and retry behavior");
        require(expected.contains(marker) && actual.contains(marker),
                "same caller exception occurs in both read paths");
    }

    /** Run only the admitted guard policy or its preserved read-effect oracle. */
    public static void run(String mode) {
        checks = 0;
        switch (mode) {
            case "readonly-policy" -> {
                readOnlyRefusalAndIdentity();
                readOnlyPreservedReadEffects();
            }
            case "readonly-effects" -> readOnlyPreservedReadEffects();
            default -> throw new IllegalArgumentException("unknown iterator guard mode: " + mode);
        }
        System.out.println("PASS M3FilteredListIteratorGuardContract mode=" + mode + " checks=" + checks);
    }

    public static void main(String[] args) {
        run(args.length == 0 ? "readonly-policy" : args[0]);
    }
}
