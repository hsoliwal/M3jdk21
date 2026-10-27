/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.pcollections;

import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3IndexMaskCompactor.Backend;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/** Independent value/order/identity and real JNI parity acceptance fixtures. */
@SuppressWarnings("deprecation")
public final class M3PViewsContract {
    private static long assertions;
    private M3PViewsContract() { }

    public static void main(String[] arguments) throws Exception {
        String mode = arguments.length == 0 ? "--java" : arguments[0];
        switch (mode) {
            case "--java" -> run();
            case "--absent" -> absentNative();
            case "--native" -> {
                check(M3IndexMaskCompactor.nativeAvailable(), "Actual JNI library loaded");
                masks(Backend.NATIVE_REQUIRED);
                predicateOrder(Backend.NATIVE_REQUIRED);
            }
            default -> throw new IllegalArgumentException(mode);
        }
        System.out.println("M3PViewsContract PASS mode=" + mode + " assertions=" + assertions);
    }

    public static void run() throws Exception {
        factorySurface();
        stableComposition();
        predicateOrder(Backend.JAVA);
        masks(Backend.JAVA);
        serialization();
        liveWrappers();
        destructiveRefusal();
    }

    private static void factorySurface() {
        check(M3PCollections.vector().isEmpty(), "vector factory");
        equal(M3PCollections.vector(List.of(2, 1)), List.of(2, 1), "vector conversion");
        check(M3PCollections.stack().isEmpty(), "stack factory");
        equal(M3PCollections.stack(List.of(2, 1)), List.of(2, 1), "stack conversion");
        check(M3PCollections.queue().isEmpty(), "queue factory");
        equal(new ArrayList<>(M3PCollections.queue(List.of(2, 1))), List.of(2, 1), "queue conversion");
        check(M3PCollections.map().isEmpty(), "map factory");
        equal(M3PCollections.map(Map.of(1, 2)), Map.of(1, 2), "map conversion");
        check(M3PCollections.set().isEmpty(), "set factory");
        equal(M3PCollections.set(List.of(2, 1, 2)), Set.of(1, 2), "set conversion");
        check(M3PCollections.bag().isEmpty(), "bag factory");
        equal(M3PCollections.bag(List.of(2, 1, 2)).count(2), 2, "bag conversion");
        check(M3PCollections.orderedMap().isEmpty(), "ordered map factory");
        equal(M3PCollections.orderedMap(Map.of(1, 2)), Map.of(1, 2), "ordered map conversion");
        check(M3PCollections.orderedSet().isEmpty(), "ordered set factory");
        equal(new ArrayList<>(M3PCollections.orderedSet(List.of(2, 1, 2))), List.of(2, 1), "ordered set conversion");
        check(M3PCollections.<Integer>sortedSet().isEmpty(), "sorted set factory");
        equal(new ArrayList<>(M3PCollections.sortedSet(List.of(3, 1, 2), Comparator.reverseOrder())), List.of(3, 2, 1), "sorted set conversion");
        check(M3PCollections.sortedSet(Comparator.<Integer>naturalOrder()).isEmpty(), "comparator set factory");
        check(M3PCollections.<Integer, String>sortedMap().isEmpty(), "sorted map factory");
        check(M3PCollections.sortedMap(Comparator.<Integer>naturalOrder()).isEmpty(), "comparator map factory");
        equal(new ArrayList<>(M3PCollections.sortedMap(Map.of(1, "a", 2, "b"), Comparator.reverseOrder()).keySet()), List.of(2, 1), "sorted map conversion");
        equal(M3PCollections.copyOf(List.of(2, 1)).sorted(), List.of(1, 2), "natural projection");
    }

    private record Item(int group, int occurrence) { }

    private static void stableComposition() {
        Random random = new Random(0x4d3356494557L);
        ArrayList<Item> mutable = new ArrayList<>();
        for (int i = 0; i < 1057; i++) mutable.add(new Item(random.nextInt(13), i));
        mutable.set(32, mutable.get(31)); // Duplicate occurrence of the same object.
        ArrayList<Item> oracle = new ArrayList<>(mutable);
        M3PVector<Item> owner = M3PVector.from(mutable);
        M3PListView<Item> original = M3PCollections.view(owner);
        mutable.clear();
        equal(original.size(), 1057, "snapshot owner survives source mutation");
        Comparator<Item> comparator = Comparator.comparingInt(Item::group);
        for (int trial = 0; trial < 150; trial++) {
            M3PListView<Item> actual = original;
            List<Item> expected = new ArrayList<>(oracle);
            for (int step = 0; step < 12; step++) {
                switch (random.nextInt(4)) {
                    case 0 -> {
                        actual = actual.reversed();
                        expected = new ArrayList<>(expected.reversed());
                    }
                    case 1 -> {
                        int from = random.nextInt(expected.size() + 1);
                        int to = from + random.nextInt(expected.size() - from + 1);
                        actual = actual.subList(from, to);
                        expected = new ArrayList<>(expected.subList(from, to));
                    }
                    case 2 -> {
                        actual = actual.filter(item -> (item.group() & 1) == 0);
                        expected.removeIf(item -> (item.group() & 1) != 0);
                    }
                    default -> { actual = actual.sorted(comparator); expected.sort(comparator); }
                }
                sameOccurrences(actual, expected, "composed order/identity");
                equal(actual.toVector(), expected, "visible vector materialization");
                int[] indices = actual.sourceIndices();
                for (int index = 0; index < indices.length; index++) {
                    check(owner.get(indices[index]) == actual.get(index), "coordinate composition");
                }
                if (indices.length > 0) {
                    Item first = actual.get(0);
                    indices[0] = -1;
                    check(actual.get(0) == first, "derived index export isolated");
                }
            }
        }
        equal(original, oracle, "all composed versions preserve original");
        M3PListView<Integer> withNull = M3PListView.copyOf(Arrays.asList(3, null, 1, null, 1));
        equal(withNull.sorted(Comparator.nullsFirst(Comparator.naturalOrder())), Arrays.asList(null, null, 1, 1, 3), "null stable comparator");
        expect(NullPointerException.class, () -> withNull.sorted(null));
        expect(NullPointerException.class, withNull::sorted);
        expect(IndexOutOfBoundsException.class, () -> original.subList(-1, 1));
        expect(IndexOutOfBoundsException.class, () -> original.subList(0, 1058));
        expect(IllegalArgumentException.class, () -> original.subList(10, 9));
        expect(IndexOutOfBoundsException.class, () -> original.get(1057));
        RuntimeException failure = new IllegalStateException("comparator marker");
        try { original.sorted((left, right) -> { throw failure; }); throw new AssertionError("missing comparator failure"); }
        catch (RuntimeException observed) { check(observed == failure, "comparator failure identity"); }
        equal(original, oracle, "comparator failure preserves source");
    }

    private static void predicateOrder(Backend backend) {
        List<Integer> values = Arrays.asList(5, null, 2, 2, 9, 0);
        M3PListView<Integer> source = M3PListView.copyOf(values).reversed().subList(1, 6);
        ArrayList<Integer> trace = new ArrayList<>();
        M3PListView<Integer> selected = source.filter(value -> {
            trace.add(value);
            return value == null || (value & 1) == 0;
        }, backend);
        equal(trace, Arrays.asList(9, 2, 2, null, 5), "predicate once in encounter order");
        equal(selected, Arrays.asList(2, 2, null), "stable predicate selection");
        RuntimeException failure = new IllegalArgumentException("predicate marker");
        trace.clear();
        try {
            source.filter(value -> { trace.add(value); if (value == null) throw failure; return true; }, backend);
            throw new AssertionError("missing predicate failure");
        } catch (RuntimeException observed) { check(observed == failure, "predicate failure identity"); }
        equal(trace, Arrays.asList(9, 2, 2, null), "predicate stops at first failure");
        equal(source, Arrays.asList(9, 2, 2, null, 5), "predicate failure preserves source");
        expect(NullPointerException.class, () -> source.filter(null, backend));
        expect(NullPointerException.class, () -> source.filter(value -> true, null));
    }

    private static void masks(Backend backend) {
        int[] lengths = {0, 1, 31, 32, 33, 255, 256, 257, 1057};
        for (int length : lengths) {
            ArrayList<Object> payload = new ArrayList<>();
            for (int index = 0; index < length + 6; index++) payload.add(new Object());
            M3PListView<Object> view = M3PListView.copyOf(payload).subList(3, length + 3).reversed();
            byte[] states = new byte[length];
            for (int index = 0; index < length; index++) states[index] = (byte) (index * 73 + length * 31);
            for (int mask = 0; mask < 256; mask++) {
                ArrayList<Object> expected = new ArrayList<>();
                for (int index = 0; index < length; index++) {
                    if (((states[index] & 255) & mask) == mask) expected.add(payload.get(length + 2 - index));
                }
                byte[] before = states.clone();
                M3PListView<Object> result = view.select(states, mask, backend);
                sameOccurrences(result, expected, "unsigned-byte mask oracle");
                check(Arrays.equals(states, before), "state lane unchanged");
                sameOccurrences(result, view.select(states, mask, Backend.JAVA), "Java/native parity");
                Arrays.fill(states, (byte) 0);
                sameOccurrences(result, expected, "later state writes isolated");
                states = before;
            }
        }
        M3PListView<Integer> one = M3PListView.copyOf(List.of(1));
        expect(IllegalArgumentException.class, () -> one.select(new byte[0], 0, backend));
        expect(IllegalArgumentException.class, () -> one.select(new byte[1], -1, backend));
        expect(IllegalArgumentException.class, () -> one.select(new byte[1], 256, backend));
        expect(NullPointerException.class, () -> one.select(null, 0, backend));
        expect(NullPointerException.class, () -> one.select(new byte[1], 0, null));
    }

    private static void absentNative() {
        check(!M3IndexMaskCompactor.nativeAvailable(), "Absence probe must run without library");
        AtomicInteger calls = new AtomicInteger();
        M3PListView<Integer> view = M3PListView.copyOf(List.of(1, 2, 3));
        expect(UnsatisfiedLinkError.class, () -> view.filter(value -> { calls.incrementAndGet(); return true; }, Backend.NATIVE_REQUIRED));
        equal(calls.get(), 0, "missing native rejected before callback");
        expect(UnsatisfiedLinkError.class, () -> view.select(new byte[3], 0, Backend.NATIVE_REQUIRED));
        expect(UnsatisfiedLinkError.class, () -> M3PListView.copyOf(List.of()).filter(value -> true, Backend.NATIVE_REQUIRED));
        equal(view.filter(value -> value > 1), List.of(2, 3), "Java remains explicit default");
    }

    private static void serialization() throws Exception {
        Object hidden = new Object(); // Not serializable: proves hidden owner payload is excluded.
        M3PListView<Object> visible = M3PListView.copyOf(Arrays.asList(hidden, "b", "a", hidden))
                .subList(1, 3).reversed();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(visible); }
        Object result;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            result = input.readObject();
        }
        check(result instanceof M3PListView<?>, "view serialization type");
        equal(result, List.of("a", "b"), "visible-only serialization value");
    }

    private static void liveWrappers() {
        ArrayList<Integer> backing = new ArrayList<>(List.of(1, 2, 3, 4));
        Collection<Integer> live = M3PCollections.liveFiltered(backing, value -> (value & 1) == 0);
        M3PListView<Integer> frozen = M3PCollections.copyOf(live);
        backing.add(6);
        equal(new ArrayList<>(live), List.of(2, 4, 6), "live backing observation");
        equal(frozen, List.of(2, 4), "explicit snapshot");
        check(live.remove(2), "live remove delegates");
        check(!backing.contains(2), "live removal owns mutation");
        expect(IllegalArgumentException.class, () -> live.add(7));
        live.clear();
        equal(backing, List.of(1, 3), "clear removes only visible");
        NavigableSet<Integer> sortedOwner = new TreeSet<>(List.of(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> sorted = M3PCollections.liveFiltered(sortedOwner, value -> (value & 1) == 0);
        equal(new ArrayList<>(sorted.descendingSet()), List.of(6, 4, 2), "live sorted descending");
        equal(sorted.higher(2), 4, "filtered navigation");
        equal(sorted.subSet(2, true, 6, false), Set.of(2, 4), "filtered range");
        check(M3PCollections.liveFiltered((Collection<Integer>) sortedOwner, value -> true) instanceof NavigableSet<?>, "runtime capability collection");
        check(M3PCollections.liveFiltered((Set<Integer>) sortedOwner, value -> true) instanceof NavigableSet<?>, "runtime capability set");
        check(M3PCollections.liveFiltered((SortedSet<Integer>) sortedOwner, value -> true) instanceof NavigableSet<?>, "runtime capability sorted set");
        equal(sorted.pollFirst(), 2, "live poll delegates");
        check(!sortedOwner.contains(2), "live sorted poll updates backing");
    }

    private static void destructiveRefusal() {
        for (M3PListView<Integer> view : List.of(M3PListView.<Integer>copyOf(List.of()),
                M3PListView.copyOf(List.of(1, 2)), M3PListView.copyOf(List.of(1, 2)).reversed())) {
            expect(UnsupportedOperationException.class, () -> view.add(3));
            expect(UnsupportedOperationException.class, () -> view.addAll(List.of()));
            expect(UnsupportedOperationException.class, view::clear);
            expect(UnsupportedOperationException.class, () -> view.removeIf(value -> false));
            expect(UnsupportedOperationException.class, () -> view.replaceAll(value -> value));
            expect(UnsupportedOperationException.class, () -> view.sort(null));
            expect(UnsupportedOperationException.class, () -> view.removeAll(List.of()));
            expect(UnsupportedOperationException.class, () -> view.retainAll(view));
            expect(UnsupportedOperationException.class, () -> view.addFirst(3));
            expect(UnsupportedOperationException.class, () -> view.addLast(3));
            expect(UnsupportedOperationException.class, view::removeFirst);
            expect(UnsupportedOperationException.class, view::removeLast);
            expect(UnsupportedOperationException.class, view.iterator()::remove);
            expect(UnsupportedOperationException.class, () -> view.listIterator().set(4));
            expect(UnsupportedOperationException.class, () -> view.listIterator().add(4));
            ListIterator<Integer> reverseCursor = view.listIterator(view.size());
            for (int index = view.size() - 1; index >= 0; index--) {
                equal(reverseCursor.previousIndex(), index, "reverse iterator coordinate");
                check(reverseCursor.previous() == view.get(index), "reverse iterator identity");
            }
            check(!reverseCursor.hasPrevious(), "reverse cursor exhausted");
            if (!view.isEmpty()) {
                Iterator<Integer> iterator = view.iterator(); iterator.next();
                expect(UnsupportedOperationException.class, iterator::remove);
                ListIterator<Integer> cursor = view.listIterator(); cursor.next();
                expect(UnsupportedOperationException.class, () -> cursor.set(4));
                expect(UnsupportedOperationException.class, () -> cursor.add(4));
            }
        }
    }

    private static void sameOccurrences(List<?> actual, List<?> expected, String message) {
        equal(actual.size(), expected.size(), message + " size");
        for (int index = 0; index < actual.size(); index++) check(actual.get(index) == expected.get(index), message);
    }
    private static void equal(Object actual, Object expected, String message) {
        check(Objects.equals(actual, expected), message + ": " + actual + " != " + expected);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        assertions++;
        try { action.run(); }
        catch (Throwable observed) {
            if (type.isInstance(observed)) return;
            throw new AssertionError("Expected " + type.getName() + ", observed " + observed, observed);
        }
        throw new AssertionError("Expected " + type.getName());
    }
}
