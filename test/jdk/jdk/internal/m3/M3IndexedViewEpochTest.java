/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Lazy filtered/sorted view binding, index ranges and backing epochs
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3IndexedViewEpochTest
 * @run main/othervm -Xint M3IndexedViewEpochTest
 */

import jdk.internal.m3.M3IndexedArray;

import java.util.ArrayList;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;

public class M3IndexedViewEpochTest {
    private static int checks;
    private static final Comparator<Item> ORDER = Comparator.nullsFirst(
            Comparator.comparingInt(Item::rank));

    private record Item(int rank, int occurrence) { }

    public static void main(String[] args) {
        coldTraversal();
        lateBinding();
        rangesAndIterators();
        ownerEpochInvalidation();
        unchangedPayloadEpoch();
        mutationOrdering();
        callbackFailures();
        stableIdentityOracle();
        System.out.println("M3IndexedViewEpochTest PASS checks=" + checks);
    }

    private static void coldTraversal() {
        for (int kind = 0; kind < 3; kind++) {
            for (int size : new int[] {0, 1, 2, 3, 32, 257}) {
                ArrayList<Item> expected = input(size);
                List<Item> view = view(store(expected), kind);
                List<Item> oracle = oracle(expected, kind);
                sameReferences(oracle, view.stream().toList(), "cold stream");
                sameReferences(oracle, view(store(expected), kind).parallelStream().toList(),
                        "cold parallel stream");
                ArrayList<Item> splitValues = new ArrayList<>();
                collectSplits(view(store(expected), kind).spliterator(), splitValues);
                sameReferences(oracle, splitValues, "cold split tree");
                sameReferences(oracle.reversed(),
                        view(store(expected), kind).reversed().stream().toList(),
                        "cold reversed stream");
            }
        }
    }

    private static void lateBinding() {
        M3IndexedArray<Integer> owner = new M3IndexedArray<>();
        owner.append(3);
        owner.append(1);
        AtomicInteger predicates = new AtomicInteger();
        AtomicInteger comparisons = new AtomicInteger();
        List<Integer> view = owner.filteredSorted(value -> {
            predicates.incrementAndGet();
            return value % 2 != 0;
        }, (left, right) -> {
            comparisons.incrementAndGet();
            return Integer.compare(left, right);
        });
        Spliterator<Integer> cursor = view.spliterator();
        require(cursor.characteristics() == (Spliterator.ORDERED
                | Spliterator.SIZED | Spliterator.SUBSIZED), "honest List characteristics");
        expect(IllegalStateException.class, cursor::getComparator);
        expect(NullPointerException.class, () -> cursor.tryAdvance(null));
        expect(NullPointerException.class, () -> cursor.forEachRemaining(null));
        require(predicates.get() == 0 && comparisons.get() == 0, "metadata/null action do not bind");
        owner.append(5);
        require(cursor.estimateSize() == 3, "first estimate sees latest owner");
        require(predicates.get() == 3, "single filter pass on binding");
        int comparisonsAtBind = comparisons.get();
        require(comparisonsAtBind > 0, "sorting performed on binding");
        require(cursor.getExactSizeIfKnown() == 3, "exact bound cardinality");
        ArrayList<Integer> actual = new ArrayList<>();
        collectSplits(cursor, actual);
        require(actual.equals(List.of(1, 3, 5)), "bound sorted order");
        require(predicates.get() == 3 && comparisons.get() == comparisonsAtBind,
                "splitting/traversal reuses selected indexes");
        require(cursor.estimateSize() == 0, "consumed cardinality");

        AtomicInteger never = new AtomicInteger();
        List<Integer> unused = owner.filtered(value -> {
            never.incrementAndGet();
            return true;
        });
        try (var stream = unused.stream()) {
            require(!stream.isParallel(), "default stream is sequential");
            require(never.get() == 0, "stream construction is lazy");
        }
        require(never.get() == 0, "unused stream close is lazy");
    }

    private static void rangesAndIterators() {
        ArrayList<Item> expected = input(71);
        for (int kind = 0; kind < 3; kind++) {
            List<Item> oracle = oracle(expected, kind);
            List<Item> view = view(store(expected), kind);
            for (int begin = 0; begin <= oracle.size(); begin++) {
                int end = (begin + oracle.size()) >>> 1;
                List<Item> range = view.subList(begin, end);
                require(range instanceof RandomAccess, "range retains RandomAccess");
                sameReferences(oracle.subList(begin, end), range, "range contents");
                sameReferences(oracle.subList(begin, end), range.stream().toList(), "range stream");
                sameReferences(oracle.subList(begin, end).reversed(), range.reversed(), "range reverse");
                sameReferences(oracle.subList(begin, end), range.subList(0, range.size()), "nested range");
                ArrayList<Item> splitValues = new ArrayList<>();
                collectSplits(range.spliterator(), splitValues);
                sameReferences(oracle.subList(begin, end), splitValues, "range split tree");
                ListIterator<Item> cursor = range.listIterator(range.size());
                for (int index = range.size() - 1; index >= 0; index--) {
                    require(cursor.hasPrevious(), "reverse iterator readiness");
                    require(cursor.previousIndex() == index, "reverse iterator relative index");
                    require(cursor.previous() == oracle.get(begin + index), "reverse iterator identity");
                    require(cursor.nextIndex() == index, "forward iterator relative index");
                }
                require(!cursor.hasPrevious(), "reverse iterator exhausted");
                expect(NoSuchElementException.class, cursor::previous);
                for (Item item : range) {
                    require(cursor.hasNext(), "forward iterator readiness");
                    require(cursor.next() == item, "forward iterator identity");
                }
                require(!cursor.hasNext(), "forward iterator exhausted");
                expect(NoSuchElementException.class, cursor::next);
                expect(UnsupportedOperationException.class, cursor::remove);
                expect(UnsupportedOperationException.class, () -> cursor.add(null));
                expect(UnsupportedOperationException.class, () -> cursor.set(null));
                expect(UnsupportedOperationException.class, () -> range.add(null));
                if (!range.isEmpty()) {
                    expect(UnsupportedOperationException.class, () -> range.remove(0));
                    expect(UnsupportedOperationException.class, () -> range.set(0, null));
                }
            }
            expect(IndexOutOfBoundsException.class, () -> view.subList(-1, 0));
            expect(IndexOutOfBoundsException.class, () -> view.subList(0, view.size() + 1));
            expect(IllegalArgumentException.class, () -> view.subList(1, 0));
            expect(IndexOutOfBoundsException.class, () -> view.listIterator(-1));
            expect(IndexOutOfBoundsException.class, () -> view.listIterator(view.size() + 1));
        }
        List<Item> deep = view(store(expected), 2);
        for (int depth = 0; depth < 10000; depth++) deep = deep.subList(0, deep.size());
        sameReferences(oracle(expected, 2), deep, "deep ranges share flat index coordinates");
    }

    private static void ownerEpochInvalidation() {
        for (int kind = 0; kind < 3; kind++) {
            for (int mutation = 0; mutation < 4; mutation++) {
                M3IndexedArray<Item> owner = store(input(33));
                List<Item> live = view(owner, kind);
                List<Item> range = live.subList(1, 8);
                List<Item> nested = range.subList(1, 3);
                List<Item> empty = range.subList(0, 0);
                ListIterator<Item> iterator = range.listIterator();
                Iterator<Item> rootIterator = live.iterator();
                Spliterator<Item> bound = live.spliterator();
                bound.estimateSize();
                Spliterator<Item> prefix = bound.trySplit();
                Spliterator<Item> rangeCursor = range.spliterator();
                Spliterator<Item> unboundRoot = live.spliterator();
                long handle = owner.handleAt(1);
                if (mutation == 0) owner.append(new Item(-9, 500));
                if (mutation == 1) owner.replace(handle, new Item(-8, 501));
                if (mutation == 2) owner.remove(handle);
                if (mutation == 3) {
                    owner.remove(handle);
                    long reused = owner.append(new Item(-7, 502));
                    require((int) handle == (int) reused, "physical slot was reused");
                    require(handle != reused, "slot generation changed");
                }
                // No intervening live.size/get may refresh the root before these guards.
                expect(ConcurrentModificationException.class, range::size);
                expect(ConcurrentModificationException.class, () -> range.get(0));
                expect(ConcurrentModificationException.class, () -> range.listIterator(0));
                expect(ConcurrentModificationException.class, () -> range.subList(0, 0));
                expect(ConcurrentModificationException.class, nested::size);
                expect(ConcurrentModificationException.class, empty::size);
                expect(ConcurrentModificationException.class, iterator::hasNext);
                expect(ConcurrentModificationException.class, iterator::next);
                expect(ConcurrentModificationException.class, iterator::nextIndex);
                expect(ConcurrentModificationException.class, rootIterator::hasNext);
                expect(ConcurrentModificationException.class, bound::estimateSize);
                expect(ConcurrentModificationException.class, bound::trySplit);
                expect(ConcurrentModificationException.class, prefix::estimateSize);
                expect(ConcurrentModificationException.class, rangeCursor::estimateSize);
                AtomicInteger delivered = new AtomicInteger();
                expect(ConcurrentModificationException.class,
                        () -> prefix.tryAdvance(item -> delivered.incrementAndGet()));
                expect(ConcurrentModificationException.class,
                        () -> bound.forEachRemaining(item -> delivered.incrementAndGet()));
                require(delivered.get() == 0, "changed version rejected before delivery");
                ArrayList<Item> currentOwner = new ArrayList<>();
                for (int index = 0; index < owner.size(); index++) currentOwner.add(owner.at(index));
                ArrayList<Item> currentValues = new ArrayList<>();
                unboundRoot.forEachRemaining(currentValues::add);
                sameReferences(oracle(currentOwner, kind), currentValues,
                        "unbound root remains live after old cursors expire");
            }
        }
    }

    private static void callbackFailures() {
        M3IndexedArray<Integer> owner = new M3IndexedArray<>();
        owner.append(3);
        owner.append(1);
        owner.append(2);
        List<Integer> view = owner.sorted(Integer::compare);
        Spliterator<Integer> cursor = view.spliterator();
        RuntimeException marker = new RuntimeException("consumer marker");
        expectSame(marker, () -> cursor.tryAdvance(value -> { throw marker; }));
        ArrayList<Integer> rest = new ArrayList<>();
        cursor.forEachRemaining(rest::add);
        require(rest.equals(List.of(2, 3)), "throwing consumer advances exactly once");

        AtomicInteger effects = new AtomicInteger();
        Spliterator<Integer> mutating = view.spliterator();
        expect(ConcurrentModificationException.class, () -> mutating.forEachRemaining(value -> {
            effects.incrementAndGet();
            owner.append(4);
        }));
        require(effects.get() == 1, "reentrant mutation stops before second callback");
        Spliterator<Integer> mutatingThrows = view.spliterator();
        expectSame(marker, () -> mutatingThrows.tryAdvance(value -> {
            owner.append(5);
            throw marker;
        }));
        expect(ConcurrentModificationException.class, mutatingThrows::trySplit);

        AtomicInteger remainingFailures = new AtomicInteger(1);
        List<Integer> retry = owner.filteredSorted(value -> {
            if (remainingFailures.getAndDecrement() > 0) throw marker;
            return true;
        }, Integer::compare);
        Spliterator<Integer> retryCursor = retry.spliterator();
        expectSame(marker, retryCursor::estimateSize);
        require(retryCursor.estimateSize() == owner.size(), "failed filter does not publish indexes");
        rest.clear();
        retryCursor.forEachRemaining(rest::add);
        require(rest.equals(List.of(1, 2, 3, 4, 5)), "retry selects complete stable result");

        AtomicInteger comparatorFailures = new AtomicInteger(1);
        List<Integer> retrySort = owner.sorted((left, right) -> {
            if (comparatorFailures.getAndDecrement() > 0) throw marker;
            return Integer.compare(left, right);
        });
        Spliterator<Integer> retrySortCursor = retrySort.spliterator();
        expectSame(marker, retrySortCursor::trySplit);
        rest.clear();
        retrySortCursor.forEachRemaining(rest::add);
        require(rest.equals(List.of(1, 2, 3, 4, 5)), "failed comparator never publishes partial order");

        List<Integer> invalidPredicate = owner.filtered(value -> {
            owner.append(10);
            return true;
        });
        expect(ConcurrentModificationException.class, () -> invalidPredicate.spliterator().trySplit());
        List<Integer> invalidComparator = owner.sorted((left, right) -> {
            owner.append(11);
            return 0;
        });
        expect(ConcurrentModificationException.class, () -> invalidComparator.spliterator().estimateSize());

        // Normal read reentrancy may reuse the already bound view and owner.
        List<Integer> readable = owner.sorted(Integer::compare);
        AtomicInteger reads = new AtomicInteger();
        readable.spliterator().forEachRemaining(value -> {
            require(readable.getFirst() <= value, "reentrant read preserves sorted order");
            require(readable.size() == owner.size(), "reentrant read preserves owner count");
            reads.incrementAndGet();
        });
        require(reads.get() == owner.size(), "all reentrant read callbacks visited");
    }

    private static void unchangedPayloadEpoch() {
        M3IndexedArray<Integer> owner = new M3IndexedArray<>();
        long handle = owner.append(7);
        List<Integer> view = owner.filtered(value -> true);
        List<Integer> range = view.subList(0, 1);
        Iterator<Integer> iterator = view.iterator();
        Spliterator<Integer> split = view.spliterator();
        require(split.estimateSize() == 1, "bind before lease changes");
        long expectedVersion = owner.version();
        owner.retain(handle);
        owner.retain(handle);
        require(!owner.replace(handle, 9), "active lease refuses replacement");
        require(!owner.remove(handle), "active lease refuses removal");
        owner.release(handle);
        owner.release(handle);
        require(owner.version() == expectedVersion, "lease and refused mutations preserve value epoch");
        require(range.getFirst() == 7 && iterator.next() == 7, "lease changes preserve bound cursors");
        ArrayList<Integer> values = new ArrayList<>();
        split.forEachRemaining(values::add);
        require(values.equals(List.of(7)), "lease changes preserve bound splits");

        M3IndexedArray<Integer> emptyOwner = new M3IndexedArray<>();
        Spliterator<Integer> empty = emptyOwner.filtered(value -> true).spliterator();
        require(empty.estimateSize() == 0, "empty split bound before mutation");
        emptyOwner.append(1);
        AtomicInteger effects = new AtomicInteger();
        expect(ConcurrentModificationException.class,
                () -> empty.tryAdvance(value -> effects.incrementAndGet()));
        require(effects.get() == 0, "expired empty split does not admit new value");
    }

    private static void mutationOrdering() {
        M3IndexedArray<Integer> owner = new M3IndexedArray<>();
        for (int value : new int[] {7, 3, 1, 5}) owner.append(value);
        List<Integer> root = owner.sorted(Integer::compare);
        List<Integer> range = root.subList(1, 3).subList(0, 2);
        List<Integer> empty = range.subList(0, 0);
        expect(IndexOutOfBoundsException.class, () -> range.set(-1, 9));
        expect(IndexOutOfBoundsException.class, () -> range.set(2, 9));
        expect(IndexOutOfBoundsException.class, () -> range.add(-1, 9));
        expect(IndexOutOfBoundsException.class, () -> range.add(3, 9));
        expect(IndexOutOfBoundsException.class, () -> range.remove(-1));
        expect(IndexOutOfBoundsException.class, () -> range.remove(2));
        expect(IndexOutOfBoundsException.class, () -> range.addAll(-1, null));
        expect(IndexOutOfBoundsException.class, () -> range.addAll(3, null));
        expect(NullPointerException.class, () -> range.addAll(0, null));
        expect(NullPointerException.class, () -> range.addAll(null));
        expect(IndexOutOfBoundsException.class, () -> empty.set(0, 9));
        expect(IndexOutOfBoundsException.class, () -> empty.remove(0));
        empty.clear();
        require(empty.isEmpty() && owner.size() == 4, "empty range clear is a permitted no-op");
        expect(UnsupportedOperationException.class, range::clear);
        require(range.equals(List.of(3, 5)), "failed clear retains range");

        ArrayList<String> events = new ArrayList<>();
        Collection<Integer> emptyInput = observedInput(owner, List.of(), events, null);
        require(!range.addAll(emptyInput), "reported empty batch remains a no-op");
        require(events.equals(List.of("size")), "empty addAll does not open input iterator");
        events.clear();
        require(!empty.addAll(0, emptyInput), "empty range accepts empty no-op batch");
        require(events.equals(List.of("size")), "indexed empty batch keeps one size probe");
        events.clear();
        expect(IndexOutOfBoundsException.class, () -> range.addAll(-1, emptyInput));
        require(events.isEmpty(), "bounds precede input callbacks");
        Collection<Integer> values = observedInput(owner, List.of(9, 11), events, null);
        expect(UnsupportedOperationException.class, () -> range.addAll(values));
        require(events.equals(List.of("size", "iterator", "hasNext", "next")),
                "nonempty addAll retains callback order before owner refusal");
        events.clear();
        RuntimeException marker = new RuntimeException("size callback marker");
        Collection<Integer> throwing = observedInput(owner, List.of(9), events, marker);
        expectSame(marker, () -> range.addAll(throwing));
        require(events.equals(List.of("size")), "size exception precedes iterator and owner");

        AtomicInteger callbacks = new AtomicInteger();
        expect(UnsupportedOperationException.class, () -> range.removeIf(value -> {
            require(!Thread.holdsLock(owner), "remove predicate outside owner monitor");
            callbacks.incrementAndGet();
            return true;
        }));
        require(callbacks.get() == 1, "remove predicate runs before iterator refusal");
        expect(UnsupportedOperationException.class, () -> range.replaceAll(value -> {
            require(!Thread.holdsLock(owner), "replacement callback outside owner monitor");
            callbacks.incrementAndGet();
            return value;
        }));
        require(callbacks.get() == 2, "replacement callback runs before iterator refusal");
        require(root.equals(List.of(1, 3, 5, 7)), "unsupported mutations preserve owner values");
    }

    private static Collection<Integer> observedInput(M3IndexedArray<Integer> owner,
            List<Integer> values, List<String> events, RuntimeException sizeFailure) {
        return new AbstractCollection<>() {
            private void record(String event) {
                require(!Thread.holdsLock(owner), "input callback outside owner monitor: " + event);
                events.add(event);
            }

            @Override public int size() {
                record("size");
                if (sizeFailure != null) throw sizeFailure;
                return values.size();
            }

            @Override public Iterator<Integer> iterator() {
                record("iterator");
                Iterator<Integer> source = values.iterator();
                return new Iterator<>() {
                    @Override public boolean hasNext() { record("hasNext"); return source.hasNext(); }
                    @Override public Integer next() { record("next"); return source.next(); }
                };
            }
        };
    }

    private static void stableIdentityOracle() {
        Random random = new Random(0x4d3345504f43484cL);
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        ArrayList<Item> physical = new ArrayList<>();
        ArrayList<Long> handles = new ArrayList<>();
        List<Item> filtered = view(owner, 0);
        List<Item> sorted = view(owner, 1);
        List<Item> combined = view(owner, 2);
        for (int trial = 0; trial < 2000; trial++) {
            int operation = physical.isEmpty() ? 0 : random.nextInt(physical.size() > 120 ? 2 : 3);
            Item item = random.nextInt(7) == 0 ? null : new Item(random.nextInt(13) - 6, trial);
            if (!physical.isEmpty() && random.nextInt(8) == 0) {
                item = physical.get(random.nextInt(physical.size()));
            }
            if (operation == 0 && physical.size() <= 120) {
                handles.add(owner.append(item));
                physical.add(item);
            } else if (operation == 1) {
                int index = random.nextInt(physical.size());
                require(owner.remove(handles.remove(index)), "oracle removal");
                physical.remove(index);
            } else {
                int index = random.nextInt(physical.size());
                require(owner.replace(handles.get(index), item), "oracle replacement");
                physical.set(index, item);
            }
            for (int kind = 0; kind < 3; kind++) {
                List<Item> view = kind == 0 ? filtered : kind == 1 ? sorted : combined;
                List<Item> expected = oracle(physical, kind);
                sameReferences(expected, view.stream().toList(), "mutating owner oracle");
                int from = random.nextInt(expected.size() + 1);
                int to = from + random.nextInt(expected.size() - from + 1);
                List<Item> range = view.subList(from, to);
                ArrayList<Item> divided = new ArrayList<>();
                collectSplits(range.spliterator(), divided);
                sameReferences(expected.subList(from, to), divided, "random split range");
                if ((trial & 31) == 0) {
                    sameReferences(expected, view.parallelStream().toList(), "parallel encounter order");
                }
            }
            require(owner.size() == physical.size(), "no view mutates owner size");
            for (int index = 0; index < physical.size(); index++) {
                require(owner.at(index) == physical.get(index), "owner identity/order unchanged");
            }
        }
    }

    private static ArrayList<Item> input(int size) {
        ArrayList<Item> values = new ArrayList<>();
        for (int i = 0; i < size; i++) values.add(i % 13 == 0 ? null : new Item((i * 7) % 11 - 5, i));
        return values;
    }

    private static M3IndexedArray<Item> store(List<Item> values) {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        for (Item value : values) owner.append(value);
        return owner;
    }

    private static boolean selected(Item item) {
        return item == null || (item.rank() & 1) == 0;
    }

    private static List<Item> view(M3IndexedArray<Item> owner, int kind) {
        return switch (kind) {
            case 0 -> owner.filtered(M3IndexedViewEpochTest::selected);
            case 1 -> owner.sorted(ORDER);
            default -> owner.filteredSorted(M3IndexedViewEpochTest::selected, ORDER);
        };
    }

    private static List<Item> oracle(List<Item> input, int kind) {
        ArrayList<Item> expected = new ArrayList<>();
        for (Item value : input) if (kind == 1 || selected(value)) expected.add(value);
        if (kind != 0) expected.sort(ORDER);
        return expected;
    }

    private static <E> void collectSplits(Spliterator<E> remaining, List<E> target) {
        long before = remaining.getExactSizeIfKnown();
        require(before >= 0, "every index split has an exact size");
        Spliterator<E> prefix = remaining.trySplit();
        if (prefix != null) {
            require(prefix.getExactSizeIfKnown() + remaining.getExactSizeIfKnown() == before,
                    "split cardinality conserved");
            collectSplits(prefix, target);
            collectSplits(remaining, target);
        } else {
            remaining.forEachRemaining(target::add);
            require(remaining.estimateSize() == 0, "leaf exhausted");
        }
    }

    private static void sameReferences(List<Item> expected, List<Item> actual, String message) {
        require(expected.size() == actual.size(), message + " size");
        for (int i = 0; i < expected.size(); i++) {
            require(expected.get(i) == actual.get(i), message + " occurrence " + i);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable problem) {
            require(type.isInstance(problem), "expected " + type + ", got " + problem);
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void expectSame(RuntimeException expected, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException actual) {
            require(actual == expected, "same callback exception instance");
            return;
        }
        throw new AssertionError("expected original callback exception");
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
