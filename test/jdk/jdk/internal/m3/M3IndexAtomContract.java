/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Preserve index-copy, stable-tail, result-ownership and filtered-empty contracts
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3IndexAtomContract
 */

import jdk.internal.m3.M3IndexedArray;
import jdk.internal.m3.M3IntAlgorithms;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;

/** Identical source runs against baseline and candidate; stdout is the comparison tape. */
public final class M3IndexAtomContract {
    private static int checks;
    private static final StringBuilder TAPE = new StringBuilder();

    private record Item(int key, int occurrence) { }

    private M3IndexAtomContract() { }

    public static void main(String[] args) {
        sortedWindows();
        reusedPhysicalSlots();
        stateAndValueEpochs();
        callbackFailures();
        lonelyOwnership();
        filteredEmpty();
        System.out.print(TAPE);
        System.out.println("M3IndexAtomContract PASS checks=" + checks);
    }

    private static void sortedWindows() {
        for (int length : new int[] {0, 1, 2, 3, 5, 7, 8, 9, 15, 16, 17, 31, 33, 65}) {
            for (int shape = 0; shape < 3; shape++) {
                M3IndexedArray<Item> owner = new M3IndexedArray<>();
                List<Item> encounter = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    int key = shape == 0 ? (i * 17 + length * 5) % 11 - 5
                            : shape == 1 ? (length - i) / 3 : 1;
                    Item value = new Item(key, i);
                    owner.append(value);
                    encounter.add(value);
                }
                StringBuilder calls = new StringBuilder();
                Comparator<Item> order = (left, right) -> {
                    calls.append(left.occurrence()).append(':').append(right.occurrence()).append(',');
                    return Integer.compare(left.key(), right.key());
                };
                List<Item> sorted = owner.sorted(order);
                List<Item> expected = new ArrayList<>(encounter);
                expected.sort(Comparator.comparingInt(Item::key));
                sameOrder(expected, sorted, "unconditional stable sort");
                String prepared = calls.toString();
                sameOrder(expected, sorted, "warm stable sort");
                require(prepared.equals(calls.toString()), "unchanged epoch does not repeat callbacks");
                tape("sorted/" + length + "/" + shape, prepared + " -> " + expected);

                calls.setLength(0);
                StringBuilder predicates = new StringBuilder();
                List<Item> selected = owner.filteredSorted(value -> {
                    predicates.append(value.occurrence()).append(',');
                    return (value.occurrence() & 1) == 0;
                }, order);
                List<Item> selectedExpected = new ArrayList<>();
                for (Item item : expected) {
                    if ((item.occurrence() & 1) == 0) selectedExpected.add(item);
                }
                sameOrder(selectedExpected, selected, "filtered stable sort");
                StringBuilder expectedPredicates = new StringBuilder();
                for (Item item : encounter) expectedPredicates.append(item.occurrence()).append(',');
                require(expectedPredicates.toString().equals(predicates.toString()),
                        "predicate once in original encounter order");
                tape("filtered/" + length + "/" + shape, predicates + " | " + calls);
            }
        }
    }

    private static void reusedPhysicalSlots() {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        List<Item> encounter = new ArrayList<>();
        long[] handles = new long[11];
        for (int i = 0; i < handles.length; i++) {
            Item item = new Item(i % 3, i);
            handles[i] = owner.append(item);
            encounter.add(item);
        }
        require(owner.remove(handles[8]), "remove later physical slot");
        encounter.remove(8);
        require(owner.remove(handles[3]), "remove earlier physical slot");
        encounter.remove(3);
        for (int i = 11; i < 15; i++) {
            Item item = new Item(i % 3, i);
            owner.append(item);
            encounter.add(item);
        }
        List<Item> expected = new ArrayList<>(encounter);
        expected.sort(Comparator.comparingInt(Item::key));
        StringBuilder calls = new StringBuilder();
        List<Item> sorted = owner.sorted((left, right) -> {
            calls.append(left.occurrence()).append(':').append(right.occurrence()).append(',');
            return Integer.compare(left.key(), right.key());
        });
        sameOrder(expected, sorted, "logical order survives physical slot reuse");
        tape("reused-slots", calls + " -> " + expected);
        expect(IllegalArgumentException.class, () -> owner.get(handles[3]), "stale generation");
    }

    private static void stateAndValueEpochs() {
        M3IndexedArray<Item> owner = new M3IndexedArray<>();
        Item a = new Item(3, 0), b = new Item(1, 1), c = new Item(2, 2), d = new Item(1, 3);
        long ha = owner.append(a), hb = owner.append(b), hc = owner.append(c);
        owner.append(d);
        List<Item> sorted = owner.sorted(Comparator.comparingInt(Item::key));
        List<Item> masked = owner.maskedStates(M3IndexedArray.LIVE, 0);
        sameOrder(List.of(b, d, c, a), sorted, "value view initial order");
        sameOrder(List.of(a, b, c, d), masked, "state view initial order");
        Iterator<Item> valueCursor = sorted.iterator();
        Iterator<Item> stateCursor = masked.iterator();
        owner.retain(hb);
        owner.retain(hc);
        owner.retain(hc);
        require(valueCursor.hasNext() && valueCursor.next() == b,
                "lease-only changes preserve value-view epoch");
        expect(ConcurrentModificationException.class, stateCursor::hasNext, "state-view epoch");
        sameOrder(List.of(b, c), owner.maskedStates(M3IndexedArray.LEASED, 0), "leased slots");
        sameOrder(List.of(b, d, a), owner.maskedSortedStates(
                M3IndexedArray.LIVE, M3IndexedArray.SHARED, Comparator.comparingInt(Item::key)),
                "stable state-selected order");
        sameOrder(List.of(a, b, c, d), owner.maskedStates(0, 0), "zero masks preserve owner order");
        require(owner.maskedStates(Integer.MIN_VALUE, 0).isEmpty(), "full-width missing state bit");
        expect(IllegalArgumentException.class, () -> owner.maskedStates(8, 8), "overlapping masks");
        owner.release(hc);
        owner.release(hc);
        sameOrder(List.of(b), owner.maskedStates(M3IndexedArray.LEASED, 0), "lease mask clears");
        Item replacement = new Item(-1, 4);
        require(owner.replace(ha, replacement), "unleased replacement");
        expect(ConcurrentModificationException.class, valueCursor::hasNext, "value-view epoch");
        sameOrder(List.of(replacement, b, d, c), sorted, "replacement refresh");
        tape("epochs", "state=" + owner.stateMask(hb) + ",value-order=" + sorted);
    }

    private static void callbackFailures() {
        RuntimeException predicateFailure = new RuntimeException("predicate sentinel");
        M3IndexedArray<Integer> selectedOwner = new M3IndexedArray<>();
        selectedOwner.append(3);
        selectedOwner.append(1);
        StringBuilder predicateCalls = new StringBuilder();
        List<Integer> failingSelection = selectedOwner.filtered(value -> {
            predicateCalls.append(value).append(',');
            throw predicateFailure;
        });
        require(expect(RuntimeException.class, failingSelection::size, "predicate failure")
                == predicateFailure, "predicate throwable identity");
        tape("predicate-failure", predicateCalls.toString());

        RuntimeException comparatorFailure = new RuntimeException("comparator sentinel");
        M3IndexedArray<Integer> sortedOwner = new M3IndexedArray<>();
        for (int value : new int[] {4, 1, 4, 2, 0, 3, 1}) sortedOwner.append(value);
        StringBuilder comparatorCalls = new StringBuilder();
        int[] count = {0};
        List<Integer> failingSort = sortedOwner.sorted((left, right) -> {
            comparatorCalls.append(left).append(':').append(right).append(',');
            if (++count[0] == 3) throw comparatorFailure;
            return Integer.compare(left, right);
        });
        require(expect(RuntimeException.class, failingSort::size, "comparator failure")
                == comparatorFailure, "comparator throwable identity");
        require(count[0] == 3, "comparator failure point");
        tape("comparator-failure", comparatorCalls.toString());

        M3IndexedArray<Integer> mutationOwner = new M3IndexedArray<>();
        mutationOwner.append(2);
        List<Integer> mutatingFilter = mutationOwner.filtered(value -> {
            mutationOwner.append(7);
            return true;
        });
        expect(ConcurrentModificationException.class, mutatingFilter::size, "predicate mutation");
        require(mutationOwner.size() == 2, "callback mutation occurs once before refusal");

        M3IndexedArray<Integer> leaseOwner = new M3IndexedArray<>();
        long leased = leaseOwner.append(2);
        leaseOwner.append(1);
        List<Integer> valueOnly = leaseOwner.sorted((left, right) -> {
            leaseOwner.retain(leased);
            return Integer.compare(left, right);
        });
        require(valueOnly.equals(List.of(1, 2)), "value sorting tolerates state-only callback");
        require(leaseOwner.ownerCount(leased) == 1, "one comparison lease");
        List<Integer> stateAware = leaseOwner.maskedSortedStates(M3IndexedArray.LIVE, 0,
                (left, right) -> {
                    leaseOwner.retain(leased);
                    return Integer.compare(left, right);
                });
        expect(ConcurrentModificationException.class, stateAware::size, "state comparator mutation");
        require(leaseOwner.ownerCount(leased) == 2, "state callback mutation retained");
        tape("callback-state", "leases=" + leaseOwner.ownerCount(leased));
    }

    private static void lonelyOwnership() {
        int[][] cases = {
            {}, {42}, {0, 3, 6, 9, 12, 15}, {10, 6, 5, 8}, {1, 2, 3, 4},
            {7, 7, 11, 15, 15}, {Integer.MIN_VALUE, 0, Integer.MAX_VALUE},
            {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, 0, Integer.MAX_VALUE - 1, Integer.MAX_VALUE},
            {-1_000_000_000, -500_000_000, 0, 500_000_000, 1_000_000_000}
        };
        int number = 0;
        for (int[] values : cases) {
            checkLonely(values, 0, values.length, "whole/" + number);
            int[] guarded = new int[values.length + 4];
            Arrays.fill(guarded, 91);
            System.arraycopy(values, 0, guarded, 2, values.length);
            checkLonely(guarded, 2, values.length, "window/" + number++);
        }
        expect(NullPointerException.class,
                () -> M3IntAlgorithms.lonelyNumbers(null, -1, -1), "null before geometry");
        expect(IndexOutOfBoundsException.class,
                () -> M3IntAlgorithms.lonelyNumbers(new int[] {1}, 1, 1), "window past end");
        expect(IndexOutOfBoundsException.class,
                () -> M3IntAlgorithms.lonelyNumbers(new int[] {1}, Integer.MAX_VALUE, 1),
                "overflow-safe window");
    }

    private static void checkLonely(int[] input, int offset, int length, String label) {
        int[] before = input.clone();
        List<Integer> expectedList = new ArrayList<>();
        for (int i = offset; i < offset + length; i++) {
            int occurrences = 0;
            boolean neighbor = false;
            for (int j = offset; j < offset + length; j++) {
                if (input[i] == input[j]) occurrences++;
                long distance = (long) input[i] - input[j];
                if (distance == -1 || distance == 1) neighbor = true;
            }
            if (occurrences == 1 && !neighbor) expectedList.add(input[i]);
        }
        int[] expected = expectedList.stream().mapToInt(Integer::intValue).toArray();
        int[] first = M3IntAlgorithms.lonelyNumbers(input, offset, length);
        int[] second = M3IntAlgorithms.lonelyNumbers(input, offset, length);
        require(Arrays.equals(expected, first), label + " oracle values/order");
        require(Arrays.equals(expected, second), label + " repeat values/order");
        require(Arrays.equals(before, input), label + " input unchanged");
        require(first != input && first != second, label + " independently owned results");
        tape("lonely/" + label, Arrays.toString(first));
        if (first.length != 0) {
            first[0] ^= 0x5a5a5a5a;
            require(Arrays.equals(expected, second), label + " result mutation isolated");
            require(Arrays.equals(before, input), label + " result never aliases input");
        }
        if (input.length != 0) {
            input[0] ^= 0x12345678;
            require(Arrays.equals(expected, second), label + " later input mutation isolated");
            input[0] = before[0];
        }
    }

    private static void filteredEmpty() {
        emptyCase("empty", List.of(), value -> true, true, "I;H0;");
        emptyCase("first", Arrays.asList(null, 1, 2), Objects::isNull, false, "I;H0;N0;Pnull;");
        emptyCase("middle", Arrays.asList(null, 1, 2, 3),
                value -> value != null && value == 2, false,
                "I;H0;N0;Pnull;H1;N1;P1;H2;N2;P2;");
        emptyCase("none", Arrays.asList(null, 1, 2), value -> false, true,
                "I;H0;N0;Pnull;H1;N1;P1;H2;N2;P2;H3;");
        for (int failureKind = 0; failureKind < 3; failureKind++) {
            StringBuilder events = new StringBuilder();
            RuntimeException failure = new RuntimeException("empty sentinel " + failureKind);
            TraceCollection backing = new TraceCollection(List.of(1, 2, 3), events,
                    failureKind == 0 ? 1 : -1, failureKind == 1 ? 1 : -1, failure);
            int kind = failureKind;
            Collection<Integer> view = CollectionViews.filtered(backing, value -> {
                events.append('P').append(value).append(';');
                if (kind == 2 && value == 2) throw failure;
                return false;
            });
            require(expect(RuntimeException.class, view::isEmpty, "empty failure " + kind)
                    == failure, "empty failure throwable identity");
            tape("empty-failure/" + kind, events.toString());
        }
        StringBuilder nestedCalls = new StringBuilder();
        Collection<Integer> inner = CollectionViews.filtered(List.of(0, 1, 2, 3), value -> {
            nestedCalls.append('L').append(value).append(';');
            return (value & 1) == 0;
        });
        Collection<Integer> outer = CollectionViews.filtered(inner, value -> {
            nestedCalls.append('R').append(value).append(';');
            return value > 0;
        });
        require(!outer.isEmpty(), "nested first admitted occurrence");
        require(nestedCalls.toString().equals("L0;R0;L1;L2;R2;"), "nested callback order");
        tape("empty-nested", nestedCalls.toString());

        TreeSet<Integer> set = new TreeSet<>(List.of(1, 2, 3, 4));
        StringBuilder orderedCalls = new StringBuilder();
        Collection<Integer> descending = CollectionViews.filtered(set.descendingSet(), value -> {
            orderedCalls.append(value).append(',');
            return (value & 1) == 0;
        });
        require(!descending.isEmpty(), "descending sorted owner first hit");
        require(orderedCalls.toString().equals("4,"), "descending callback order");
        tape("empty-descending", orderedCalls.toString());
    }

    private static void emptyCase(String label, List<Integer> values, Predicate<Integer> predicate,
            boolean expected, String expectedTape) {
        StringBuilder events = new StringBuilder();
        TraceCollection backing = new TraceCollection(values, events, -1, -1, null);
        Collection<Integer> view = CollectionViews.filtered(backing, value -> {
            events.append('P').append(value).append(';');
            return predicate.test(value);
        });
        require(events.length() == 0, "construction stays lazy");
        require(view.isEmpty() == expected, label + " result");
        require(events.toString().equals(expectedTape), label + " exact owner/predicate trace");
        tape("empty/" + label, events.toString());
    }

    /** Exposes only the existing source iterator contract; size must never be consulted. */
    private static final class TraceCollection extends AbstractCollection<Integer> {
        private final List<Integer> values;
        private final StringBuilder events;
        private final int failHasNext;
        private final int failNext;
        private final RuntimeException failure;

        TraceCollection(List<Integer> values, StringBuilder events, int failHasNext,
                int failNext, RuntimeException failure) {
            this.values = values;
            this.events = events;
            this.failHasNext = failHasNext;
            this.failNext = failNext;
            this.failure = failure;
        }

        @Override public int size() { throw new AssertionError("unexpected source size query"); }

        @Override public Iterator<Integer> iterator() {
            events.append("I;");
            return new Iterator<>() {
                private int cursor;
                @Override public boolean hasNext() {
                    events.append('H').append(cursor).append(';');
                    if (cursor == failHasNext) throw failure;
                    return cursor < values.size();
                }
                @Override public Integer next() {
                    events.append('N').append(cursor).append(';');
                    if (cursor == failNext) throw failure;
                    if (cursor == values.size()) throw new NoSuchElementException();
                    return values.get(cursor++);
                }
            };
        }
    }

    private static void sameOrder(List<Item> expected, List<Item> actual, String label) {
        require(actual.size() == expected.size(), label + " size");
        for (int i = 0; i < expected.size(); i++) {
            require(actual.get(i) == expected.get(i), label + " occurrence " + i);
        }
    }

    private static Throwable expect(Class<? extends Throwable> type, Runnable operation, String label) {
        try {
            operation.run();
        } catch (Throwable failure) {
            require(type.isInstance(failure), label + " type: " + failure.getClass().getName());
            tape("exception/" + label, failure.getClass().getName());
            return failure;
        }
        throw new AssertionError(label + " did not throw " + type.getName());
    }

    private static void tape(String label, String value) {
        TAPE.append(label).append('=').append(value).append('\n');
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
