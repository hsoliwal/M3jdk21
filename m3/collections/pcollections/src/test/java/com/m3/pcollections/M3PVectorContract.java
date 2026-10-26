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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;

/** Frozen, dependency-free contract oracle; run with Java 21. */
@SuppressWarnings("deprecation")
public final class M3PVectorContract {
    private static final long SEED = 0x4d335650L;
    private static final int[] BOUNDARIES = {
        0, 1, 15, 16, 17, 31, 32, 33, 63, 64, 65, 1023, 1024, 1025, 4097
    };
    private static int assertions;

    private M3PVectorContract() {}

    public static void main(String[] args) {
        factoriesAndBounds();
        replacementsAndRemoval();
        boundariesAndDeletion();
        deterministicHistories();
        slicesAndSharing();
        traversalAndArrays();
        unmodifiableDefaults();
        sourceCallbacks();
        serialization();
        System.out.println("M3PVectorContract PASS assertions=" + assertions
                + " seed=" + SEED + " boundaries=" + Arrays.toString(BOUNDARIES));
    }

    private static void factoriesAndBounds() {
        M3PVector<Integer> empty = M3PVector.empty();
        check(empty == M3PVector.<Integer>empty(), "canonical empty");
        same(empty, Collections.emptyList());
        check(empty.plusAll(List.of()) == empty, "empty append identity");
        check(empty.subList(0, 0) == empty, "empty slice identity");
        check(empty.minus((Object) 1) == empty, "absent removal identity");
        check(M3PVector.singleton(null).get(0) == null, "null singleton");

        ArrayList<Integer> input = new ArrayList<>(Arrays.asList(3, null, 3, 9));
        M3PVector<Integer> vector = M3PVector.from(input);
        input.set(0, 77);
        input.clear();
        same(vector, Arrays.asList(3, null, 3, 9));
        check(M3PVector.from(vector) == vector, "immutable conversion identity");
        check(vector.subList(0, vector.size()) == vector, "full slice identity");
        check(vector.subList(2, 2) == empty, "empty slice canonicalization");
        for (int index : new int[] {-1, 4, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            expect(IndexOutOfBoundsException.class, () -> vector.get(index));
            expect(IndexOutOfBoundsException.class, () -> vector.with(index, 0));
            expect(IndexOutOfBoundsException.class, () -> vector.minus(index));
        }
        for (int index : new int[] {-1, 5, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            expect(IndexOutOfBoundsException.class, () -> vector.plus(index, 0));
            expect(IndexOutOfBoundsException.class, () -> vector.plusAll(index, List.of()));
            expect(IndexOutOfBoundsException.class, () -> vector.listIterator(index));
        }
        expect(IndexOutOfBoundsException.class, () -> vector.subList(-1, 2));
        expect(IndexOutOfBoundsException.class, () -> vector.subList(0, 5));
        expect(IndexOutOfBoundsException.class, () -> vector.subList(5, 5));
        expect(IndexOutOfBoundsException.class, () -> vector.subList(0, -1));
        expect(IllegalArgumentException.class, () -> vector.subList(3, 2));
        expect(NullPointerException.class, () -> M3PVector.from(null));
        expect(NullPointerException.class, () -> vector.plusAll(null));
        expect(NullPointerException.class, () -> vector.plusAll(0, null));
        expect(NullPointerException.class, () -> vector.minusAll(null));
        expect(NoSuchElementException.class, empty::getFirst);
        expect(NoSuchElementException.class, empty::getLast);
    }

    private static void replacementsAndRemoval() {
        Box first = new Box(7);
        Box incoming = new Box(7);
        M3PVector<Box> original = M3PVector.singleton(first);
        M3PVector<Box> changed = original.with(0, incoming);
        check(changed != original, "equal distinct replacement creates version");
        check(changed.get(0) == incoming, "incoming replacement reference");
        check(original.get(0) == first, "old replacement reference");
        check(changed.with(0, incoming) == changed, "identical replacement no-op");
        M3PVector<Integer> repeated = M3PVector.from(Arrays.asList(1, null, 1, 2, null, 1));
        same(repeated.minus((Object) 1), Arrays.asList(null, 1, 2, null, 1));
        same(repeated.minus(null), Arrays.asList(1, 1, 2, null, 1));
        same(repeated.minusAll(Arrays.asList(1, null, 1)), List.of(2));
        List<Integer> absent = Collections.singletonList(99);
        check(repeated.minusAll(absent) == repeated, "absent minusAll identity");
        ArrayList<Integer> jdk = new ArrayList<>(Arrays.asList(1, null, 1, 2, null, 1));
        check(!jdk.removeAll(absent), "JDK null-tolerant absent membership is a no-op");
        // Preserve the supplied membership collection's null-query behavior.
        List<Integer> nullRejecting = List.of(99);
        expect(NullPointerException.class, () -> nullRejecting.contains(null));
        expect(NullPointerException.class, () -> jdk.removeAll(nullRejecting));
        expect(NullPointerException.class, () -> repeated.minusAll(nullRejecting));
        same(repeated.minusAll(repeated), List.of());
        same(repeated.plusAll(2, Arrays.asList(8, null, 9)),
                Arrays.asList(1, null, 8, null, 9, 1, 2, null, 1));
        same(repeated.plusAll(1, repeated),
                Arrays.asList(1, 1, null, 1, 2, null, 1, null, 1, 2, null, 1));
        same(repeated, Arrays.asList(1, null, 1, 2, null, 1));
    }

    private static void boundariesAndDeletion() {
        for (int count : BOUNDARIES) {
            ArrayList<Integer> expected = sequence(count);
            M3PVector<Integer> appended = M3PVector.empty();
            for (int value : expected) appended = appended.plus(value);
            M3PVector<Integer> built = M3PVector.from(expected);
            same(appended, expected);
            same(built, expected);
            for (int index : new int[] {0, count / 2, count}) {
                ArrayList<Integer> inserted = new ArrayList<>(expected);
                inserted.add(index, null);
                M3PVector<Integer> next = appended.plus(index, null);
                same(next, inserted);
                same(next.minus(index), expected);
                same(appended, expected);
            }
        }
        M3PVector<Integer> base = M3PVector.from(sequence(4097));
        for (int strategy = 0; strategy < 5; strategy++) {
            M3PVector<Integer> value = base;
            ArrayList<Integer> expected = sequence(base.size());
            Random random = new Random(SEED + strategy);
            int iteration = 0;
            while (!expected.isEmpty()) {
                int index = switch (strategy) {
                    case 0 -> 0;
                    case 1 -> expected.size() - 1;
                    case 2 -> expected.size() / 2;
                    case 3 -> (iteration & 1) == 0 ? 0 : expected.size() - 1;
                    default -> random.nextInt(expected.size());
                };
                expected.remove(index);
                value = value.minus(index);
                value.verifyInvariants();
                check(value.size() == expected.size(), "deletion size");
                if ((++iteration & 63) == 0 || expected.size() < 34) same(value, expected);
            }
            check(value == M3PVector.<Integer>empty(), "deletion canonical empty");
            same(base, sequence(4097));
        }
    }

    private static void deterministicHistories() {
        Random random = new Random(SEED);
        M3PVector<Integer> vector = M3PVector.empty();
        ArrayList<Integer> expected = new ArrayList<>();
        ArrayList<M3PVector<Integer>> oldVersions = new ArrayList<>();
        ArrayList<List<Integer>> oldContents = new ArrayList<>();
        for (int step = 0; step < 12000; step++) {
            if ((step & 63) == 0) {
                oldVersions.add(vector);
                oldContents.add(new ArrayList<>(expected));
            }
            int op = random.nextInt(9);
            Integer value = random.nextInt(7) == 0 ? null : random.nextInt(41) - 20;
            if (op == 0 || expected.isEmpty()) {
                vector = vector.plus(value);
                expected.add(value);
            } else if (op == 1) {
                int index = random.nextInt(expected.size() + 1);
                vector = vector.plus(index, value);
                expected.add(index, value);
            } else if (op == 2) {
                int index = random.nextInt(expected.size());
                vector = vector.with(index, value);
                expected.set(index, value);
            } else if (op == 3) {
                int index = random.nextInt(expected.size());
                vector = vector.minus(index);
                expected.remove(index);
            } else if (op == 4) {
                vector = vector.minus((Object) value);
                expected.remove(value);
            } else if (op == 5) {
                int index = random.nextInt(expected.size() + 1);
                List<Integer> batch = Arrays.asList(value, random.nextInt(41) - 20, null);
                vector = vector.plusAll(index, batch);
                expected.addAll(index, batch);
            } else if (op == 6) {
                List<Integer> removed = Arrays.asList(value, null);
                vector = vector.minusAll(removed);
                expected.removeIf(removed::contains);
            } else if (op == 7 && expected.size() > 12) {
                int from = random.nextInt(expected.size());
                int to = from + random.nextInt(expected.size() - from + 1);
                vector = vector.subList(from, to);
                expected = new ArrayList<>(expected.subList(from, to));
            } else {
                vector = vector.plusAll(Arrays.asList(value, value));
                expected.add(value);
                expected.add(value);
            }
            same(vector, expected);
        }
        for (int index = 0; index < oldVersions.size(); index++) {
            same(oldVersions.get(index), oldContents.get(index));
        }
    }

    private static void slicesAndSharing() {
        M3PVector<Integer> parent = M3PVector.from(sequence(4097));
        M3PVector<Integer> view = parent.subList(100, 200);
        check(view.sharesRoot(parent), "slice shares exact root");
        check(view.retainedSize() == parent.size(), "slice reports backing retention");
        check(view.sharedPageCount(parent) == parent.pageCount(), "slice shares all pages");
        M3PVector<Integer> nested = view.subList(20, 30);
        same(nested, sequence(4097).subList(120, 130));
        check(nested.sharesRoot(parent), "nested slice shares root");
        check(nested.subList(0, nested.size()) == nested, "nested full slice identity");
        ArrayList<Integer> expected = new ArrayList<>(sequence(4097).subList(100, 200));
        M3PVector<Integer> changed = view.plus(0, -1).plus(view.size() + 1, -2).with(1, -3);
        expected.add(0, -1);
        expected.add(-2);
        expected.set(1, -3);
        same(changed, expected);
        changed = changed.minus(0).minus(changed.size() - 2);
        expected.remove(0);
        expected.remove(expected.size() - 1);
        same(changed, expected);
        same(view, sequence(4097).subList(100, 200));
        same(parent, sequence(4097));
        M3PVector<Integer> detached = M3PVector.<Integer>empty().plusAll(view);
        same(detached, view);
        check(detached.sharedPageCount(view) == 0, "explicit materialization detaches pages");
        check(detached.retainedSize() == detached.size(), "detached slice retention");
        M3PVector<Integer> empty = view.minusAll(view);
        check(empty.retainedSize() == 0 && empty.pageCount() == 0, "empty view releases backing");

        M3PVector<Integer> replacement = parent.with(2048, -10);
        check(!replacement.sharesRoot(parent), "updated root is distinct");
        check(parent.pageCount() - parent.sharedPageCount(replacement) == parent.treeHeight(),
                "replacement copies exactly its path");
        check(replacement.get(2048) == -10 && parent.get(2048) == 2048, "version isolation");
        for (int index : new int[] {0, 31, 32, 1023, 1024, 2048, 4097}) {
            M3PVector<Integer> inserted = parent.plus(index, -11);
            inserted.verifyInvariants();
            int shared = parent.sharedPageCount(inserted);
            check(shared > 0, "insert retains disjoint pages");
            check(parent.pageCount() - shared <= parent.treeHeight(), "insert copies changed path");
        }
        check(parent.pageCount() < parent.size() / 8, "pages are blocks, not per-entry objects");
        check(parent.treeHeight() >= 3, "multi-level tree fixture");
        System.out.println("M3PVector sharing pages=" + parent.pageCount()
                + " height=" + parent.treeHeight() + " retained=" + view.retainedSize()
                + " visible=" + view.size() + " updatedShared=" + parent.sharedPageCount(replacement));
    }

    private static void traversalAndArrays() {
        M3PVector<Integer> vector = M3PVector.from(sequence(1025)).subList(17, 1001);
        List<Integer> expected = sequence(1025).subList(17, 1001);
        ListIterator<Integer> actual = vector.listIterator();
        ListIterator<Integer> reference = expected.listIterator();
        while (reference.hasNext()) {
            check(actual.nextIndex() == reference.nextIndex(), "forward nextIndex");
            check(Objects.equals(actual.next(), reference.next()), "forward value");
        }
        check(!actual.hasNext(), "forward exhaustion");
        expect(NoSuchElementException.class, actual::next);
        while (reference.hasPrevious()) {
            check(actual.previousIndex() == reference.previousIndex(), "backward previousIndex");
            check(Objects.equals(actual.previous(), reference.previous()), "backward value");
        }
        check(!actual.hasPrevious(), "backward exhaustion");
        expect(NoSuchElementException.class, actual::previous);
        actual = vector.listIterator(32);
        check(actual.next() == 49 && actual.previous() == 49 && actual.previous() == 48,
                "mixed direction across page boundary");
        Iterator<Integer> snapshot = vector.iterator();
        vector.plus(99);
        ArrayList<Integer> traversed = new ArrayList<>();
        snapshot.forEachRemaining(traversed::add);
        check(traversed.equals(expected), "iterator snapshot");
        Spliterator<Integer> spliterator = vector.spliterator();
        int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.SIZED;
        check(spliterator.hasCharacteristics(characteristics), "spliterator characteristics");
        ArrayList<Integer> splitContents = new ArrayList<>();
        Spliterator<Integer> prefix = spliterator.trySplit();
        if (prefix != null) prefix.forEachRemaining(splitContents::add);
        spliterator.forEachRemaining(splitContents::add);
        check(splitContents.equals(expected), "split encounter order");
        check(vector.parallelStream().toList().equals(expected), "parallel traversal order");

        M3PVector<String> strings = M3PVector.from(Arrays.asList("a", null, "c"));
        Object[] exported = strings.toArray();
        exported[0] = "changed";
        check(strings.get(0).equals("a"), "Object array does not alias storage");
        String[] oversized = new String[] {"x", "x", "x", "x", "x"};
        check(strings.toArray(oversized) == oversized, "typed oversized array reuse");
        check(oversized[3] == null && oversized[4].equals("x"), "typed array terminator");
        oversized[0] = "changed";
        check(strings.get(0).equals("a"), "typed array does not alias storage");
        check(Arrays.equals(strings.toArray(String[]::new), new String[] {"a", null, "c"}),
                "array generator");
        expect(ArrayStoreException.class, () -> strings.toArray(new Integer[0]));
        check(strings.getFirst().equals("a") && strings.getLast().equals("c"), "sequenced reads");
        check(strings.reversed().equals(Arrays.asList("c", null, "a")), "reverse snapshot order");
    }

    private static void unmodifiableDefaults() {
        for (M3PVector<Integer> value : List.of(M3PVector.<Integer>empty(), M3PVector.singleton(1))) {
            expect(UnsupportedOperationException.class, () -> value.add(2));
            expect(UnsupportedOperationException.class, () -> value.add(0, 2));
            expect(UnsupportedOperationException.class, () -> value.addAll(List.of()));
            expect(UnsupportedOperationException.class, () -> value.addAll(0, List.of()));
            expect(UnsupportedOperationException.class, value::clear);
            expect(UnsupportedOperationException.class, () -> value.remove(0));
            expect(UnsupportedOperationException.class, () -> value.remove((Object) 9));
            expect(UnsupportedOperationException.class, () -> value.removeAll(List.of()));
            expect(UnsupportedOperationException.class, () -> value.retainAll(value));
            expect(UnsupportedOperationException.class, () -> value.set(0, 2));
            expect(UnsupportedOperationException.class, () -> value.removeIf(item -> {
                throw new AssertionError("destructive predicate must not be called");
            }));
            expect(UnsupportedOperationException.class, () -> value.replaceAll(item -> {
                throw new AssertionError("destructive operator must not be called");
            }));
            expect(UnsupportedOperationException.class, () -> value.sort((left, right) -> {
                throw new AssertionError("destructive comparator must not be called");
            }));
            expect(UnsupportedOperationException.class, () -> value.addFirst(2));
            expect(UnsupportedOperationException.class, () -> value.addLast(2));
            expect(UnsupportedOperationException.class, value::removeFirst);
            expect(UnsupportedOperationException.class, value::removeLast);
            ListIterator<Integer> iterator = value.listIterator();
            expect(UnsupportedOperationException.class, iterator::remove);
            expect(UnsupportedOperationException.class, () -> iterator.set(2));
            expect(UnsupportedOperationException.class, () -> iterator.add(2));
            expect(UnsupportedOperationException.class, () -> value.reversed().removeAll(List.of()));
            expect(UnsupportedOperationException.class, () -> value.reversed().replaceAll(x -> x));
            expect(UnsupportedOperationException.class, () -> value.reversed().removeFirst());
        }
    }

    private static void sourceCallbacks() {
        M3PVector<Integer> vector = M3PVector.from(Arrays.asList(1, null, 2, 1, 3));
        ArrayList<Integer> visited = new ArrayList<>();
        Collection<Integer> membership = new AbstractCollection<>() {
            @Override public Iterator<Integer> iterator() { return List.of(1).iterator(); }
            @Override public int size() { return 1; }
            @Override public boolean contains(Object value) {
                visited.add((Integer) value);
                return Objects.equals(value, 1);
            }
        };
        same(vector.minusAll(membership), Arrays.asList(null, 2, 3));
        check(visited.equals(vector), "minusAll membership callbacks follow encounter order");
        RuntimeException failure = new IllegalStateException("fixture callback failure");
        Collection<Integer> brokenMembership = new AbstractCollection<>() {
            @Override public Iterator<Integer> iterator() { return List.of(1).iterator(); }
            @Override public int size() { return 1; }
            @Override public boolean contains(Object value) {
                if (Objects.equals(value, 2)) throw failure;
                return Objects.equals(value, 1);
            }
        };
        expectSame(failure, () -> vector.minusAll(brokenMembership));
        Collection<Integer> brokenSource = new AbstractCollection<>() {
            @Override public int size() { return 4; }
            @Override public Iterator<Integer> iterator() {
                return new Iterator<>() {
                    private int count;
                    @Override public boolean hasNext() { return true; }
                    @Override public Integer next() {
                        if (count++ == 2) throw failure;
                        return count;
                    }
                };
            }
        };
        expectSame(failure, () -> vector.plusAll(brokenSource));
        expectSame(failure, () -> vector.plusAll(2, brokenSource));
        expectSame(failure, () -> M3PVector.from(brokenSource));
        same(vector, Arrays.asList(1, null, 2, 1, 3));
        ArrayList<Integer> source = sequence(33);
        M3PVector<Integer> copied = M3PVector.from(source);
        source.replaceAll(x -> -1);
        same(copied, sequence(33));
    }

    private static void serialization() {
        check(roundTrip(M3PVector.empty()) == M3PVector.empty(), "serialized canonical empty");
        M3PVector<Integer> withNulls = M3PVector.from(Arrays.asList(1, null, 1, 2));
        same(roundTrip(withNulls), withNulls);
        M3PVector<Integer> full = M3PVector.from(sequence(4097));
        same(roundTrip(full), full);
        M3PVector<Integer> slice = full.subList(31, 65);
        M3PVector<Integer> restored = roundTrip(slice);
        same(restored, slice);
        check(restored.retainedSize() == restored.size(), "serialization contains visible sequence only");
        M3PVector<Object> parent = M3PVector.from(Arrays.asList(new Object(), "visible", new Object()));
        same(roundTrip(parent.subList(1, 2)), List.of("visible"));
        Box payload = new Box(11);
        M3PVector<Box> aliases = roundTrip(M3PVector.from(Arrays.asList(payload, payload)));
        check(aliases.get(0) == aliases.get(1), "serialization preserves payload aliases");
    }

    @SuppressWarnings("unchecked")
    private static <E> M3PVector<E> roundTrip(M3PVector<E> vector) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                output.writeObject(vector);
            }
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (M3PVector<E>) input.readObject();
            }
        } catch (Exception failure) {
            throw new AssertionError("serialization round trip", failure);
        }
    }

    private static ArrayList<Integer> sequence(int count) {
        ArrayList<Integer> values = new ArrayList<>(count);
        for (int value = 0; value < count; value++) values.add(value);
        return values;
    }

    private static void same(M3PVector<?> actual, List<?> expected) {
        actual.verifyInvariants();
        check(actual.size() == expected.size(), "size");
        check(actual.equals(expected) && expected.equals(actual), "List equality");
        check(actual.hashCode() == expected.hashCode(), "List hash code");
        check(Arrays.equals(actual.toArray(), expected.toArray()), "List array order");
        for (int index = 0; index < expected.size(); index++) {
            check(Objects.equals(actual.get(index), expected.get(index)), "indexed contents at " + index);
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        assertions++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void expectSame(RuntimeException expected, Runnable operation) {
        assertions++;
        try {
            operation.run();
        } catch (RuntimeException actual) {
            if (actual == expected) return;
            throw new AssertionError("callback exception identity", actual);
        }
        throw new AssertionError("callback failure did not propagate");
    }

    private static final class Box implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final int value;
        private Box(int value) { this.value = value; }
        @Override public boolean equals(Object other) {
            return other instanceof Box box && value == box.value;
        }
        @Override public int hashCode() { return value; }
    }
}
