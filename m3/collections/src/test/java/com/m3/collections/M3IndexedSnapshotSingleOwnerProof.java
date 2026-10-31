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
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Real Java21 test of the original M3IndexedListSnapshot Object[] storage owner.
 *
 * <p>The source Collection.toArray() contract supplies a fresh safe Object[].
 * Test-only capture lives OUTSIDE the collection; the collection itself keeps
 * no reference to the returned array. Reflection observes the unchanged
 * PRIVATE snapshot payload; no production accessor or ABI surface is added.
 */
public final class M3IndexedSnapshotSingleOwnerProof {
    private static int checks;
    private static Object[] lastArray;
    private static final AtomicInteger TO_ARRAY_CALLS = new AtomicInteger();

    private M3IndexedSnapshotSingleOwnerProof() { }

    private static void yes(boolean success, String reason) {
        checks++;
        if (!success) throw new AssertionError(reason);
    }

    private static void refusal(Class<? extends Throwable> type, Runnable task, String reason) {
        checks++;
        try {
            task.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(reason + " wrong failure", thrown);
        }
        throw new AssertionError(reason + " was not refused");
    }

    private static Object[] ownedPayload(M3IndexedListSnapshot<?> snapshot) throws Exception {
        Field field = M3IndexedListSnapshot.class.getDeclaredField("values");
        field.setAccessible(true);
        return (Object[]) field.get(snapshot);
    }

    /** Admits the standard JDK toArray ownership guarantee, not a borrowed backing array. */
    private static final class Counted<E> extends AbstractList<E> {
        private final ArrayList<E> elements;
        Counted(Collection<? extends E> source) {
            this.elements = new ArrayList<>(source);
        }
        @Override public E get(int index) { return elements.get(index); }
        @Override public int size() { return elements.size(); }
        @Override public E set(int index, E value) { return elements.set(index, value); }
        @Override public Object[] toArray() {
            TO_ARRAY_CALLS.incrementAndGet();
            Object[] fresh = elements.toArray();
            // External proof sink; no new reference retained by Counted itself.
            lastArray = fresh;
            return fresh;
        }
    }

    private static void originalArrayOwnership() throws Exception {
        Object identity = new Object();
        Counted<Object> source = new Counted<>(Arrays.asList(identity, null, identity, "z"));
        lastArray = null;
        TO_ARRAY_CALLS.set(0);
        M3IndexedListSnapshot<Object> result = M3IndexedListSnapshot.copyOf(source);
        yes(TO_ARRAY_CALLS.get() == 1, "one source toArray invocation");
        yes(ownedPayload(result) == lastArray,
                "adopt the already-safe Collection.toArray payload, no second full array copy");
        yes(result.size() == 4 && result.get(0) == identity && result.get(1) == null
                && result.get(2) == identity, "nulls and duplicate reference identity");
        yes(Arrays.equals(result.physicalIndices(), new int[] {0, 1, 2, 3}),
                "canonical one-to-one encounter index owner");
        source.set(0, "changed");
        yes(result.get(0) == identity, "input collection remains independent of snapshot");
        M3IndexedListSnapshot<Object> second = M3IndexedListSnapshot.copyOf(source);
        yes(ownedPayload(second) == lastArray && ownedPayload(second) != ownedPayload(result),
                "new snapshot has a new safe source array, no alias between generations");
        yes(second.get(0).equals("changed") && result.get(0) == identity,
                "two generations retain independent immutable snapshots");
        M3IndexedListSnapshot<Object> range = result.subList(1, 3);
        yes(ownedPayload(range) == ownedPayload(result) && range.size() == 2
                && range.get(0) == null && range.get(1) == identity,
                "offset-only sublist shares original payload owner");
        refusal(UnsupportedOperationException.class, () -> result.addAll(List.of()),
                "immutable snapshot rejects empty addAll");
        refusal(UnsupportedOperationException.class, () -> result.removeFirst(),
                "immutable snapshot rejects terminal mutation");
        yes(result.includesPhysicalIndex(0) && result.includesPhysicalIndex(3)
                && !result.includesPhysicalIndex(4), "lazy physical occurrence membership");
    }

    private static void arrayOriginRemainsDefensive() throws Exception {
        String[] backing = {"first", "second"};
        M3IndexedListSnapshot<String> snapshot = M3IndexedListSnapshot.copyOf(backing);
        Object[] inside = ownedPayload(snapshot);
        yes(inside != backing && inside.getClass() == Object[].class,
                "array-origin overload retains exact Object[] defensive owner");
        backing[0] = "outside";
        yes(snapshot.get(0).equals("first"), "array-origin caller mutation isolated");
        M3IndexedListSnapshot<String> empty = M3IndexedListSnapshot.copyOf(List.<String>of());
        yes(empty.isEmpty() && ownedPayload(empty).length == 0,
                "empty Collection.toArray is already a complete safe snapshot");
    }

    private static void stablePrimitiveIndexSorting() throws Exception {
        record Item(int key, String label) { }
        Item a = new Item(2, "a");
        Item b = new Item(1, "b");
        Item c = new Item(2, "c");
        Item d = new Item(1, "d");
        Item e = new Item(4, "e");
        List<Item> items = new ArrayList<>(Arrays.asList(a, null, b, c, d, e));
        var base = M3IndexedListSnapshot.copyOf(items);
        var filtered = base.filtered(v -> v != null && v.key() <= 2);
        var sorted = base.filteredSorted(v -> v != null && v.key() <= 2,
                Comparator.comparingInt(Item::key));
        yes(ownedPayload(base) == ownedPayload(filtered)
                && ownedPayload(base) == ownedPayload(sorted),
                "mask-filter and stable-sort views share ONE physical Object[] payload");
        yes(Arrays.equals(filtered.physicalIndices(), new int[] {0, 2, 3, 4}),
                "selected source positions preserve encounter order");
        yes(Arrays.equals(sorted.physicalIndices(), new int[] {2, 4, 0, 3}),
                "stable primitive permutation preserves comparator ties");
        yes(sorted.get(0) == b && sorted.get(1) == d
                && sorted.get(2) == a && sorted.get(3) == c,
                "sorted views project original Java references, not entry objects");
        items.clear();
        yes(sorted.size() == 4 && base.size() == 6 && sorted.get(0) == b,
                "snapshot remains independent after structural source mutation");
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            AtomicInteger calls = new AtomicInteger();
            refusal(UnsatisfiedLinkError.class,
                    () -> base.filtered(value -> {
                        calls.incrementAndGet();
                        return true;
                    }, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),
                    "explicit native backend refuses before predicate evaluation");
            yes(calls.get() == 0, "no callbacks before required JNI admission");
        }
    }

    private static void randomizedDifferential() {
        Random random = new Random(20261021L);
        for (int iteration = 0; iteration < 256; iteration++) {
            int length = random.nextInt(151);
            List<Integer> source = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                source.add(random.nextInt(9) == 0 ? null : random.nextInt(23) - 11);
            }
            M3IndexedListSnapshot<Integer> original = M3IndexedListSnapshot.copyOf(source);
            yes(original.size() == source.size()
                    && Arrays.equals(original.toArray(), source.toArray()),
                    "snapshot exact encounter order iteration=" + iteration);
            Predicate<Integer> select = value -> value != null && (value & 1) == 0;
            Comparator<Integer> comparator = Comparator.comparingInt((Integer value) -> Math.abs(value));
            var selected = original.filteredSorted(select, comparator);
            ArrayList<Integer> oracle = new ArrayList<>();
            ArrayList<Integer> stablePhysical = new ArrayList<>();
            for (int i = 0; i < source.size(); i++) {
                if (select.test(source.get(i))) {
                    oracle.add(source.get(i));
                    stablePhysical.add(i);
                }
            }
            stablePhysical.sort(Comparator.comparingInt(i -> Math.abs(source.get(i))));
            oracle.sort(comparator);
            yes(Arrays.equals(selected.toArray(), oracle.toArray()),
                    "filtered stable sorted values iteration=" + iteration);
            int[] physical = selected.physicalIndices();
            yes(physical.length == stablePhysical.size(),
                    "stable index cardinality iteration=" + iteration);
            for (int i = 0; i < physical.length; i++) {
                yes(physical[i] == stablePhysical.get(i),
                        "stable physical occurrence ties iteration=" + iteration);
            }
        }
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 0) throw new IllegalArgumentException("no arguments");
        originalArrayOwnership();
        arrayOriginRemainsDefensive();
        stablePrimitiveIndexSorting();
        randomizedDifferential();
        System.out.println("M3_INDEXED_SNAPSHOT_SINGLE_OWNER_PASS checks=" + checks);
    }
}
