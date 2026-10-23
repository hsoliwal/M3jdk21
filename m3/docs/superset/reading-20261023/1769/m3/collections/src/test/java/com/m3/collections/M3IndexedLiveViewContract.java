// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Self-contained Java21 oracle; no JUnit or native binary needed. */
public final class M3IndexedLiveViewContract {
    private M3IndexedLiveViewContract() { }
    private static int checks;
    private static void assertEqual(Object actual, Object expected) {
        if (!Objects.equals(actual, expected))
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        checks++;
    }
    private static void fails(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable problem) {
            if (expected.isInstance(problem)) { checks++; return; }
            throw new AssertionError("expected " + expected + ", got " + problem, problem);
        }
        throw new AssertionError("did not throw " + expected);
    }

    private record Pair(int key, int original) { }

    public static void contracts() {
        M3ArrayList<Pair> rows = new M3ArrayList<>(List.of(
                new Pair(2, 0), new Pair(1, 1), new Pair(2, 2),
                new Pair(3, 3), new Pair(2, 4)));
        AtomicInteger calls = new AtomicInteger();
        M3IndexedLiveView<Pair> view = M3IndexedLiveView.filteredSorted(rows, p -> {
            calls.incrementAndGet();
            return p.key() >= 2;
        }, Comparator.comparingInt(Pair::key));
        assertEqual(view.stream().map(Pair::original).toList(), List.of(0, 2, 4, 3));
        assertEqual(Arrays.toString(view.physicalIndices()), "[0, 2, 4, 3]");
        assertEqual(view.physicalIndex(2), 4);
        assertEqual(calls.get(), 5);
        assertEqual(view.get(0).original(), 0);
        assertEqual(calls.get(), 5); // cached until owner mutation
        Iterator<Pair> cursor = view.iterator();
        assertEqual(cursor.next().original(), 0);
        rows.set(2, new Pair(0, 20));
        fails(ConcurrentModificationException.class, cursor::hasNext);
        assertEqual(view.stream().map(Pair::original).toList(), List.of(0, 4, 3));
        assertEqual(calls.get(), 10);
        rows.add(0, new Pair(5, 9));
        assertEqual(view.stream().map(Pair::original).toList(), List.of(0, 4, 3, 9));
        assertEqual(view.physicalIndex(3), 0);
        rows.remove(1);
        assertEqual(view.stream().map(Pair::original).toList(), List.of(4, 3, 9));
        assertEqual(view.reversed().stream().map(Pair::original).toList(), List.of(9, 3, 4));
        fails(UnsupportedOperationException.class, () -> view.clear());
        fails(UnsupportedOperationException.class, () -> view.addAll(List.of()));
        fails(UnsupportedOperationException.class, () -> view.removeIf(x -> false));
        fails(UnsupportedOperationException.class, () -> view.reversed().removeFirst());
        fails(UnsupportedOperationException.class, () -> view.listIterator().remove());
        M3ArrayList<Integer> nulls = new M3ArrayList<>(Arrays.asList(null, 2, null, 1, 2));
        M3IndexedLiveView<Integer> filtered = M3IndexedLiveView.filtered(nulls, x -> x == null || x == 2);
        assertEqual(filtered, Arrays.asList(null, 2, null, 2));
        assertEqual(Arrays.toString(filtered.physicalIndices()), "[0, 1, 2, 4]");
        M3IndexedLiveView<Integer> sorted = M3IndexedLiveView.sorted(nulls,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        assertEqual(sorted, Arrays.asList(null, null, 1, 2, 2));
        int[] detached = sorted.physicalIndices();
        detached[0] = 999;
        assertEqual(sorted.get(0), null);
        Iterator<Integer> invalidated = sorted.iterator();
        sorted.invalidate();
        fails(ConcurrentModificationException.class, invalidated::hasNext);
        assertEqual(sorted.get(0), null);
        AtomicInteger never = new AtomicInteger();
        M3IndexedLiveView<Integer> nativeRequired = M3IndexedLiveView.filteredSorted(nulls, x -> {
            never.incrementAndGet(); return true;
        }, null, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class, nativeRequired::size);
            assertEqual(never.get(), 0);
        }
        M3IndexedLiveView<Integer> reentrant = M3IndexedLiveView.filteredSorted(nulls, x -> {
            nulls.set(0, 10);
            return true;
        }, null);
        fails(ConcurrentModificationException.class, reentrant::size);
        M3ArrayList<Integer> ints = new M3ArrayList<>(List.of(3, 1, 2));
        M3IndexedLiveView<Integer> natural = M3IndexedLiveView.sorted(ints, null);
        assertEqual(natural, List.of(1, 2, 3));
        ints.sort(null);
        assertEqual(natural, List.of(1, 2, 3));
        assertEqual(natural.physicalIndex(0), 0);
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        reentrantPreparationContracts();
        strictSubListContracts();
        strictDerivedAfterReentrancyContracts();
        randomDifferential();
    }

    private static void reentrantPreparationContracts() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(3, 1, 2));

        // Invalidation during predicate execution must abort publication.
        AtomicReference<M3IndexedLiveView<Integer>> filteredRef = new AtomicReference<>();
        AtomicInteger filterCalls = new AtomicInteger();
        M3IndexedLiveView<Integer> filteredView = M3IndexedLiveView.filtered(owner, value -> {
            if (filterCalls.getAndIncrement() == 0) filteredRef.get().invalidate();
            return value > 0;
        });
        filteredRef.set(filteredView);
        fails(ConcurrentModificationException.class, filteredView::size);
        assertEqual(filteredView, List.of(3, 1, 2));

        // Sorting cannot publish an obsolete physical index after invalidation.
        AtomicReference<M3IndexedLiveView<Integer>> sortedRef = new AtomicReference<>();
        AtomicInteger comparisonCalls = new AtomicInteger();
        M3IndexedLiveView<Integer> sortedView = M3IndexedLiveView.sorted(owner, (left, right) -> {
            if (comparisonCalls.getAndIncrement() == 0) sortedRef.get().invalidate();
            return Integer.compare(left, right);
        });
        sortedRef.set(sortedView);
        fails(ConcurrentModificationException.class, sortedView::size);
        assertEqual(sortedView, List.of(1, 2, 3));
        assertEqual(Arrays.toString(sortedView.physicalIndices()), "[1, 2, 0]");

        // A client predicate must not recur into the same half-built projection.
        AtomicReference<M3IndexedLiveView<Integer>> recursiveRef = new AtomicReference<>();
        AtomicInteger recursionCalls = new AtomicInteger();
        M3IndexedLiveView<Integer> recursive = M3IndexedLiveView.filtered(owner, value -> {
            if (recursionCalls.getAndIncrement() == 0) recursiveRef.get().size();
            return true;
        });
        recursiveRef.set(recursive);
        fails(ConcurrentModificationException.class, recursive::size);
        assertEqual(recursive, List.of(3, 1, 2));

        // The fail-fast state must be cleared by finally, even after exceptions.
        assertEqual(recursive.size(), 3);
        assertEqual(sortedView.size(), 3);
        assertEqual(filteredView.size(), 3);
    }
=======
        strictSubListContracts();
        randomDifferential();
    }

>>>>>>> f0d77f40b3f2fa78820aea517072e6d72f4d6ee8

    private record RangeCase(String name,
            java.util.function.Function<List<Integer>, List<Integer>> select,
            List<Integer> expected) { }

    /**
     * A read-only range must retain the root's unconditional mutation boundary,
     * including empty/no-op calls and calls which would otherwise run callbacks.
     */
    private static void strictSubListContracts() {
        List<RangeCase> ranges = List.of(
                new RangeCase("whole", list -> list.subList(0, 3), List.of(2, 3, 4)),
                new RangeCase("middle", list -> list.subList(1, 3), List.of(3, 4)),
                new RangeCase("empty-first", list -> list.subList(0, 0), List.of()),
                new RangeCase("empty-last", list -> list.subList(3, 3), List.of()),
                new RangeCase("nested", list -> list.subList(0, 3).subList(1, 3), List.of(3, 4)),
                new RangeCase("nested-empty", list -> list.subList(0, 3).subList(1, 1), List.of()),
                new RangeCase("reversed", list -> list.subList(0, 3).reversed(), List.of(4, 3, 2)),
                new RangeCase("reversed-nested",
                        list -> list.subList(0, 3).reversed().subList(0, 2), List.of(4, 3)),
                new RangeCase("root-reversed", list -> list.reversed().subList(0, 2), List.of(4, 3)),
                new RangeCase("reversed-empty", list -> list.subList(1, 1).reversed(), List.of()),
                new RangeCase("nested-reversed-empty",
                        list -> list.subList(0, 3).reversed().subList(1, 1), List.of()),
                new RangeCase("double-reversed",
                        list -> list.subList(0, 3).reversed().reversed(), List.of(2, 3, 4)));
        ArrayList<String> failures = new ArrayList<>();
        AtomicInteger observations = new AtomicInteger();
        for (RangeCase range : ranges) {
            M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(4, 1, 3, 2));
            M3IndexedLiveView<Integer> root = M3IndexedLiveView.filteredSorted(
                    owner, value -> value >= 2, Comparator.naturalOrder());
            List<Integer> selected = range.select().apply(root);
            assertEqual(selected, range.expected());
            assertEqual(Arrays.asList(selected.toArray()), range.expected());
            assertEqual(selected.stream().toList(), range.expected());
            assertEqual(selected.reversed().reversed(), range.expected());
            AtomicInteger callbacks = new AtomicInteger();
            java.util.function.BiConsumer<String, Runnable> refuse = (operation, action) -> {
                observations.incrementAndGet();
                callbacks.set(0);
                try {
                    action.run();
                    failures.add(range.name() + "/" + operation + ": mutation returned normally");
                } catch (UnsupportedOperationException expected) {
                    // Exact strict boundary: no callback/input traversal preceded refusal.
                } catch (Throwable wrong) {
                    failures.add(range.name() + "/" + operation + ": wrong failure " + wrong.getClass().getName());
                }
                if (callbacks.get() != 0) {
                    failures.add(range.name() + "/" + operation + ": mutation invoked " + callbacks.get() + " callback(s)");
                }
                assertEqual(owner, List.of(4, 1, 3, 2));
                assertEqual(root, List.of(2, 3, 4));
                assertEqual(selected, range.expected());
                checks++;
            };
            // A mutator must reject before inspecting even an adversarial input collection.
            java.util.Collection<Integer> input = new java.util.AbstractCollection<>() {
                @Override public int size() { callbacks.incrementAndGet(); return 1; }
                @Override public Iterator<Integer> iterator() {
                    callbacks.incrementAndGet();
                    return List.of(8).iterator();
                }
                @Override public boolean contains(Object value) {
                    callbacks.incrementAndGet();
                    return false;
                }
            };
            refuse.accept("add", () -> selected.add(8));
            refuse.accept("add-indexed", () -> selected.add(0, 8));
            refuse.accept("add-invalid-index", () -> selected.add(-1, 8));
            refuse.accept("addAll-empty", () -> selected.addAll(List.of()));
            refuse.accept("addAll-self", () -> selected.addAll(selected));
            refuse.accept("addAll-input", () -> selected.addAll(input));
            refuse.accept("addAll-null", () -> selected.addAll(null));
            refuse.accept("addAll-indexed-empty", () -> selected.addAll(0, List.of()));
            refuse.accept("addAll-indexed-input", () -> selected.addAll(0, input));
            refuse.accept("addAll-invalid-index", () -> selected.addAll(-1, input));
            refuse.accept("addAll-indexed-null", () -> selected.addAll(0, null));
            refuse.accept("set", () -> selected.set(0, 8));
            refuse.accept("set-invalid-index", () -> selected.set(-1, 8));
            refuse.accept("remove-indexed", () -> selected.remove(0));
            refuse.accept("remove-invalid-index", () -> selected.remove(-1));
            refuse.accept("remove-value", () -> selected.remove(Integer.valueOf(3)));
            refuse.accept("remove-absent", () -> selected.remove(Integer.valueOf(99)));
            refuse.accept("removeAll-empty", () -> selected.removeAll(List.of()));
            refuse.accept("removeAll-matching", () -> selected.removeAll(List.of(2, 3, 4)));
            refuse.accept("removeAll-input", () -> selected.removeAll(input));
            refuse.accept("removeAll-null", () -> selected.removeAll(null));
            refuse.accept("retainAll-input", () -> selected.retainAll(input));
            refuse.accept("retainAll-self", () -> selected.retainAll(selected));
            refuse.accept("retainAll-null", () -> selected.retainAll(null));
            refuse.accept("removeIf", () -> selected.removeIf(value -> { callbacks.incrementAndGet(); return false; }));
            refuse.accept("removeIf-null", () -> selected.removeIf(null));
            refuse.accept("replaceAll", () -> selected.replaceAll(value -> { callbacks.incrementAndGet(); return value; }));
            refuse.accept("replaceAll-null", () -> selected.replaceAll(null));
            refuse.accept("sort", () -> selected.sort((left, right) -> {
                callbacks.incrementAndGet();
                return Integer.compare(right, left);
            }));
            refuse.accept("sort-natural", () -> selected.sort(null));
            refuse.accept("clear", selected::clear);
            refuse.accept("addFirst", () -> selected.addFirst(8));
            refuse.accept("addLast", () -> selected.addLast(8));
            refuse.accept("removeFirst", selected::removeFirst);
            refuse.accept("removeLast", selected::removeLast);
            refuse.accept("iterator-remove-initial", () -> selected.iterator().remove());
            refuse.accept("iterator-remove-exhausted", () -> {
                Iterator<Integer> cursor = selected.iterator();
                while (cursor.hasNext()) cursor.next();
                cursor.remove();
            });
            refuse.accept("listIterator-remove-initial", () -> selected.listIterator().remove());
            refuse.accept("listIterator-set-initial", () -> selected.listIterator().set(8));
            refuse.accept("listIterator-add-initial", () -> selected.listIterator().add(8));
            refuse.accept("listIterator-remove-forward", () -> {
                java.util.ListIterator<Integer> cursor = selected.listIterator();
                if (cursor.hasNext()) cursor.next();
                cursor.remove();
            });
            refuse.accept("listIterator-set-forward", () -> {
                java.util.ListIterator<Integer> cursor = selected.listIterator();
                if (cursor.hasNext()) cursor.next();
                cursor.set(8);
            });
            refuse.accept("listIterator-add-forward", () -> {
                java.util.ListIterator<Integer> cursor = selected.listIterator();
                if (cursor.hasNext()) cursor.next();
                cursor.add(8);
            });
            refuse.accept("listIterator-remove-backward", () -> {
                java.util.ListIterator<Integer> cursor = selected.listIterator(selected.size());
                if (cursor.hasPrevious()) cursor.previous();
                cursor.remove();
            });
            refuse.accept("listIterator-set-backward", () -> {
                java.util.ListIterator<Integer> cursor = selected.listIterator(selected.size());
                if (cursor.hasPrevious()) cursor.previous();
                cursor.set(8);
            });
            refuse.accept("listIterator-add-backward", () -> {
                java.util.ListIterator<Integer> cursor = selected.listIterator(selected.size());
                if (cursor.hasPrevious()) cursor.previous();
                cursor.add(8);
            });
        }
        // Read validation retains AbstractList's established exception classes.
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(4, 1, 3, 2));
        M3IndexedLiveView<Integer> root = M3IndexedLiveView.filteredSorted(
                owner, value -> value >= 2, Comparator.naturalOrder());
        fails(IndexOutOfBoundsException.class, () -> root.subList(-1, 0));
        fails(IndexOutOfBoundsException.class, () -> root.subList(0, 4));
        fails(IllegalArgumentException.class, () -> root.subList(2, 1));
        List<Integer> selected = root.subList(0, 2);
        fails(IndexOutOfBoundsException.class, () -> selected.get(-1));
        fails(IndexOutOfBoundsException.class, () -> selected.listIterator(3));
        assertEqual(selected, List.of(2, 3));
        // Stable-length backing replacements remain visible through the original root.
        owner.set(3, 6);
        assertEqual(root, List.of(3, 4, 6));
        assertEqual(selected, List.of(3, 4));
        // Repeated reversal must cancel rather than retaining an unbounded wrapper chain.
        List<Integer> repeated = selected;
        for (int i = 0; i < 10_000; i++) repeated = repeated.reversed();
        assertEqual(repeated, List.of(3, 4));
        Iterator<Integer> invalidated = selected.iterator();
        root.invalidate();
        fails(ConcurrentModificationException.class, invalidated::hasNext);
        if (!failures.isEmpty()) {
            throw new AssertionError("strict sublist violations=" + failures.size()
                    + " across " + observations.get() + " mutation observations: " + failures);
        }
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /** Both recipes in one original view: failed reentrant preparation, then strict ranges. */
    private static void strictDerivedAfterReentrancyContracts() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(4, 1, 3, 2));
        AtomicReference<M3IndexedLiveView<Integer>> current = new AtomicReference<>();
        AtomicInteger attempts = new AtomicInteger();
        M3IndexedLiveView<Integer> view = M3IndexedLiveView.filteredSorted(owner, value -> {
            if (attempts.getAndIncrement() == 0) current.get().invalidate();
            return value >= 2;
        }, Comparator.naturalOrder());
        current.set(view);
        fails(ConcurrentModificationException.class, view::size);
        List<Integer> derived = view.subList(0, 3).reversed().subList(0, 2);
        assertEqual(derived, List.of(4, 3));
        fails(UnsupportedOperationException.class, () -> derived.addAll(List.of()));
        Iterator<Integer> stale = derived.iterator();
        view.invalidate();
        fails(ConcurrentModificationException.class, stale::hasNext);
        owner.set(3, 5);
        assertEqual(view, List.of(3, 4, 5));
        assertEqual(derived, List.of(5, 4));
        fails(UnsupportedOperationException.class, () -> derived.removeIf(value -> false));
    }

=======
>>>>>>> f0d77f40b3f2fa78820aea517072e6d72f4d6ee8
    private static void randomDifferential() {
        Random random = new Random(0x3AF61L);
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of());
        M3IndexedLiveView<Integer> sorted = M3IndexedLiveView.filteredSorted(
                owner, x -> x == null || (x & 1) == 0,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        for (int i = 0; i < 1000; i++) {
            switch (random.nextInt(4)) {
                case 0 -> owner.add(randomValue(random));
                case 1 -> { if (!owner.isEmpty()) owner.set(random.nextInt(owner.size()), randomValue(random)); }
                case 2 -> { if (!owner.isEmpty()) owner.remove(random.nextInt(owner.size())); }
                default -> { if (!owner.isEmpty()) owner.sort(Comparator.nullsFirst(Comparator.naturalOrder())); }
            }
            ArrayList<Integer> oracle = new ArrayList<>();
            for (Integer value : owner) {
                if (value == null || (value & 1) == 0) oracle.add(value);
            }
            oracle.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
            assertEqual(sorted, oracle);
            int[] phys = sorted.physicalIndices();
            for (int index = 0; index < phys.length; index++) {
                assertEqual(sorted.get(index), owner.get(phys[index]));
            }
        }
    }

    private static Integer randomValue(Random r) { return r.nextInt(8) == 0 ? null : r.nextInt(13) - 6; }

    public static void main(String[] args) {
        contracts();
        System.out.println("M3_INDEXED_LIVE_VIEW_PASS checks=" + checks);
    }
}
