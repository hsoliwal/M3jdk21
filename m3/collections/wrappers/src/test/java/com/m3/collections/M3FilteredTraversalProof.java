// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;

/** Finite real-owner counterexamples; no large element array or stub implementation. */
public final class M3FilteredTraversalProof {
    private static final long OVER_INT = (long) Integer.MAX_VALUE + 1;
    private static int checks;

    private M3FilteredTraversalProof() {}

    public static void main(String[] args) {
        String[] names = {"filteredSizeSaturates", "flattenedSizeSaturates",
                "flatCursorDoesNotCountOrBind", "oneShotIterableIsNotConsumedByMetadata",
                "shortCircuitDoesNotVisitSuffix", "nullAndEmptyTraversalContracts",
                "saturationPreservesLateFailures"};
        Runnable[] tests = {M3FilteredTraversalProof::filteredSizeSaturates,
                M3FilteredTraversalProof::flattenedSizeSaturates,
                M3FilteredTraversalProof::flatCursorDoesNotCountOrBind,
                M3FilteredTraversalProof::oneShotIterableIsNotConsumedByMetadata,
                M3FilteredTraversalProof::shortCircuitDoesNotVisitSuffix,
                M3FilteredTraversalProof::nullAndEmptyTraversalContracts,
                M3FilteredTraversalProof::saturationPreservesLateFailures};
        int failures = 0;
        for (int i = 0; i < tests.length; i++) {
            long start = System.nanoTime();
            try {
                tests[i].run();
                System.out.println("PASS " + names[i] + " elapsed_ms="
                        + (System.nanoTime() - start) / 1_000_000);
            } catch (Throwable failure) {
                failures++;
                System.out.println("FAIL " + names[i] + " " + failure);
            }
        }
        System.out.println("M3_FILTERED_TRAVERSAL checks=" + checks + " failures=" + failures);
        if (failures != 0) throw new AssertionError("counterexamples=" + failures);
    }

    static void filteredSizeSaturates() {
        HugeCollection source = new HugeCollection(OVER_INT);
        Collection<Integer> filtered = M3CollectionViews.filtered(source, value -> true);
        require(filtered.size() == Integer.MAX_VALUE, "filtered size must saturate");
        require(source.visited == OVER_INT, "filtered saturation must retain complete traversal");
    }

    static void flattenedSizeSaturates() {
        HugeCollection inner = new HugeCollection(OVER_INT);
        int[] mappings = {0};
        Collection<Integer> flat = M3CollectionWrappers.flatMappedView(List.of(1), value -> {
            mappings[0]++;
            return inner;
        });
        require(flat.size() == Integer.MAX_VALUE, "flattened size must saturate");
        require(inner.visited == OVER_INT && mappings[0] == 1, "flattened full traversal");
    }

    static void flatCursorDoesNotCountOrBind() {
        CountingCollection source = new CountingCollection(1, 2);
        int[] mappings = {0};
        Collection<Integer> flat = M3CollectionWrappers.flatMappedView(source, value -> {
            mappings[0]++;
            return List.of(value, value + 10);
        });
        Spliterator<Integer> cursor = flat.spliterator();
        require(cursor.estimateSize() == Long.MAX_VALUE, "flattened metadata must be unknown-size");
        require(cursor.getExactSizeIfKnown() == -1, "flattened cardinality must not be advertised exact");
        require((cursor.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED)) == 0,
                "no SIZED or SUBSIZED claim");
        require(cursor.trySplit() == null, "no eager split-buffer traversal");
        require(source.iterators == 0 && source.sizeCalls == 0 && mappings[0] == 0,
                "metadata must not bind source or invoke mapper");
        source.values.add(3);
        ArrayList<Integer> observed = new ArrayList<>();
        cursor.forEachRemaining(observed::add);
        require(observed.equals(List.of(1, 11, 2, 12, 3, 13)), "live before first traversal");
        require(source.iterators == 1 && source.sizeCalls == 0 && mappings[0] == 3,
                "one mapper invocation per traversed source occurrence");
        require(!cursor.tryAdvance(observed::add), "exhausted cursor");
    }

    static void oneShotIterableIsNotConsumedByMetadata() {
        Iterator<Integer> once = List.of(1, 2, 3).iterator();
        int[] mappings = {0};
        Collection<Integer> flat = M3CollectionWrappers.flatMappedView(List.of(7), value -> {
            mappings[0]++;
            return () -> once;
        });
        require(flat.stream().toList().equals(List.of(1, 2, 3)), "one-shot inner traversal");
        require(mappings[0] == 1, "no hidden cardinality pass");
    }

    static void shortCircuitDoesNotVisitSuffix() {
        int[] mappings = {0};
        Collection<Integer> flat = M3CollectionWrappers.flatMappedView(List.of(1, 2), value -> {
            mappings[0]++;
            if (value == 2) throw new LateFailure();
            return List.of(11, 12);
        });
        require(flat.stream().limit(1).toList().equals(List.of(11)), "short-circuit result");
        require(mappings[0] == 1, "short-circuit must not visit later mapped source");
    }

    static void nullAndEmptyTraversalContracts() {
        CountingCollection source = new CountingCollection();
        Spliterator<Integer> empty = M3CollectionWrappers.flatMappedView(source, List::of).spliterator();
        rejects(NullPointerException.class, () -> empty.tryAdvance((Consumer<Integer>) null));
        rejects(NullPointerException.class, () -> empty.forEachRemaining((Consumer<Integer>) null));
        require(source.iterators == 0 && source.sizeCalls == 0, "invalid callback must not bind");
        require(!empty.tryAdvance(value -> { throw new AssertionError("empty action"); }), "empty traversal");
        Collection<String> values = M3CollectionWrappers.flatMappedView(List.of(1, 2, 3), value ->
                value == 1 ? List.of() : value == 2 ? Arrays.asList(null, "x", "x") : List.of("z"));
        require(values.stream().toList().equals(Arrays.asList(null, "x", "x", "z")),
                "empty inners, nulls, duplicate references and order");
        rejects(NullPointerException.class, () -> M3CollectionWrappers.flatMappedView(
                List.of(1), value -> (Iterable<Integer>) null).stream().toList());
        rejects(NoSuchElementException.class, () -> M3CollectionWrappers.flatMappedView(
                List.<Integer>of(), List::of).iterator().next());
    }

    static void saturationPreservesLateFailures() {
        HugeCollection filteredSource = new HugeCollection(OVER_INT + 1);
        rejects(LateFailure.class, () -> M3CollectionViews.filtered(filteredSource, value -> {
            if (value == 1) throw new LateFailure();
            return true;
        }).size());
        require(filteredSource.visited == OVER_INT + 1, "late predicate still evaluated past saturation");
        HugeCollection flatSource = new HugeCollection(OVER_INT);
        rejects(LateFailure.class, () -> M3CollectionWrappers.flatMappedView(List.of(1, 2), value -> {
            if (value == 2) throw new LateFailure();
            return flatSource;
        }).size());
        require(flatSource.visited == OVER_INT, "late mapper still evaluated after saturated inner");
    }

    private static final class HugeCollection extends AbstractCollection<Integer> {
        private final long length;
        private long visited;
        private HugeCollection(long length) { this.length = length; }
        @Override public int size() { return Integer.MAX_VALUE; }
        @Override public Iterator<Integer> iterator() {
            return new Iterator<>() {
                private long remaining = length;
                @Override public boolean hasNext() { return remaining != 0; }
                @Override public Integer next() {
                    if (remaining == 0) throw new NoSuchElementException();
                    remaining--;
                    visited++;
                    return remaining == 0 ? 1 : 0;
                }
            };
        }
    }

    private static final class CountingCollection extends AbstractCollection<Integer> {
        private final ArrayList<Integer> values;
        private int iterators;
        private int sizeCalls;
        private CountingCollection(Integer... values) { this.values = new ArrayList<>(List.of(values)); }
        @Override public Iterator<Integer> iterator() { iterators++; return values.iterator(); }
        @Override public int size() { sizeCalls++; throw new AssertionError("unexpected source.size"); }
    }

    private static final class LateFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable body) {
        try {
            body.run();
        } catch (Throwable failure) {
            require(expected.isInstance(failure), "expected " + expected.getName() + ", saw " + failure);
            return;
        }
        throw new AssertionError("expected " + expected.getName());
    }
}
