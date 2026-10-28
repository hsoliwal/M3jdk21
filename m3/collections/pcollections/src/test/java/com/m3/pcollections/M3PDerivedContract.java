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
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import org.pcollections.AmortizedPQueue;
import org.pcollections.ConsPStack;
import org.pcollections.OrderedPMap;
import org.pcollections.OrderedPSet;
import org.pcollections.TreePMap;
import org.pcollections.TreePSet;

/**
 * Independent collection-contract fixtures for the M3 derived owners.
 *
 * <p>The finite sorted domain enumerates every membership subset, orientation,
 * endpoint and inclusivity pair. Larger sequences use fixed arithmetic schedules,
 * with retained versions checked after later updates. Donor comparisons are limited
 * to compatible contracts; new null, empty-entry and strict-mutation extensions
 * have explicit expectations. Reflection checks only the documented retention bound.
 */
public final class M3PDerivedContract {
    private static final int[] PAGE_SIZES = {0, 1, 15, 16, 17, 31, 32, 33, 63, 64, 65, 257, 1_025};
    private static final int[] SORTED_MEMBERS = {-6, -2, 0, 3, 7};
    private static final int[] SORTED_QUERIES = {-9, -6, -5, -2, -1, 0, 1, 3, 4, 7, 9};
    private static long checks;

    private M3PDerivedContract() { }

    public static void main(String[] args) throws Exception {
        run();
        System.out.println("M3PDerivedContract PASS checks=" + checks);
    }

    public static void run() throws Exception {
        stackContracts();
        stackVersions();
        queueContracts();
        queueDrainRefill();
        sortedEnumeration();
        sortedRepresentativesAndSnapshots();
        sortedVersions();
        orderedMaps();
        orderedSets();
        streamEncounterOrder();
        mutationRefusal();
        serialization();
    }

    private static void stackContracts() {
        List<Integer> source = Arrays.asList(1, null, 2, 1, 3);
        M3PStack<Integer> stack = M3PStack.from(source);
        ConsPStack<Integer> donor = ConsPStack.from(source);
        listEquals(stack, source, "stack conversion preserves order");
        listEquals(stack, donor, "original stack conversion");
        listEquals(M3PStack.singleton(null), Collections.singletonList(null), "null stack singleton");
        listEquals(stack.plus(9), Arrays.asList(9, 1, null, 2, 1, 3), "stack push");
        List<Integer> pushed = Arrays.asList(6, 7, null);
        listEquals(stack.plusAll(pushed), Arrays.asList(null, 7, 6, 1, null, 2, 1, 3),
                "stack push-all reverses source");
        listEquals(stack.plusAll(pushed), donor.plusAll(pushed), "original stack push-all");
        listEquals(stack.plusAll(2, pushed), Arrays.asList(1, null, null, 7, 6, 2, 1, 3),
                "indexed stack push-all reverses source");
        listEquals(stack.plusAll(2, pushed), donor.plusAll(2, pushed), "original indexed stack push-all");
        listEquals(stack.minus((Object) 1), Arrays.asList(null, 2, 1, 3), "first occurrence removed");
        listEquals(stack.minusAll(Arrays.asList(1, null, 1)), List.of(2, 3),
                "stack minusAll removes every selected occurrence");
        listEquals(stack.minusAll(Arrays.asList(1, null, 1)), donor.minusAll(Arrays.asList(1, null, 1)),
                "original stack all-occurrence removal");
        require(stack.plusAll(List.of()) == stack, "empty stack push-all is identity");
        require(stack.plusAll(stack.size(), List.of()) == stack, "empty indexed push-all is identity");
        require(stack.minus("absent") == stack, "absent stack subtraction is identity");
        require(stack.minusAll(Collections.singletonList(99)) == stack, "unmatched stack minusAll is identity");
        expect(NullPointerException.class, () -> List.of(99).contains(null),
                "supplied immutable membership rejects null query");
        expect(NullPointerException.class, () -> stack.minusAll(List.of(99)),
                "stack propagates supplied membership null rejection");
        expect(NullPointerException.class, () -> stack.minusAll(List.of(1)),
                "stack membership rejection after a matching prefix");
        expect(NullPointerException.class, () -> donor.minusAll(List.of(99)),
                "original stack propagates supplied membership null rejection");
        listEquals(stack, source, "stack version preserved after failed membership query");
        listEquals(donor, source, "original stack version preserved after failed membership query");
        require(stack.subList(0, stack.size()) == stack, "whole stack slice is identity");
        listEquals(stack.subList(1, 4), Arrays.asList(null, 2, 1), "stack slice");
        listEquals(stack.subList(2), List.of(2, 1, 3), "stack suffix");
        listEquals(stack.subList(1, 4).plusAll(1, List.of(7, 8)), Arrays.asList(null, 8, 7, 2, 1),
                "indexed push into offset slice");
        listEquals(stack.subList(1, 4), Arrays.asList(null, 2, 1), "slice retained after update");
        expect(NullPointerException.class, () -> M3PStack.from(null), "null stack source");
        expect(NullPointerException.class, () -> stack.plusAll(null), "null stack push-all source");
        expect(NullPointerException.class, () -> stack.plusAll(0, null), "null indexed stack source");
        expect(NullPointerException.class, () -> stack.minusAll(null), "null stack subtraction source");
        expect(IndexOutOfBoundsException.class, () -> stack.get(-1), "negative stack lookup");
        expect(IndexOutOfBoundsException.class, () -> stack.get(stack.size()), "stack lookup at size");
        expect(IndexOutOfBoundsException.class, () -> stack.plus(-1, 9), "negative stack insertion");
        expect(IndexOutOfBoundsException.class, () -> stack.plus(stack.size() + 1, 9),
                "stack insertion past size");
        expect(IndexOutOfBoundsException.class, () -> stack.plusAll(-1, List.of()),
                "empty indexed insertion still validates negative index");
        expect(IndexOutOfBoundsException.class, () -> stack.plusAll(stack.size() + 1, List.of()),
                "empty indexed insertion still validates upper index");
        expect(IndexOutOfBoundsException.class, () -> stack.minus(-1), "negative stack deletion");
        expect(IndexOutOfBoundsException.class, () -> stack.minus(stack.size()), "stack deletion at size");
        expect(IndexOutOfBoundsException.class, () -> stack.with(-1, 9), "negative stack replacement");
        expect(IndexOutOfBoundsException.class, () -> stack.with(stack.size(), 9), "stack replacement at size");
        expect(IndexOutOfBoundsException.class, () -> stack.subList(-1, 2), "negative stack range");
        expect(IndexOutOfBoundsException.class, () -> stack.subList(0, stack.size() + 1),
                "stack range past size");
        expect(IllegalArgumentException.class, () -> stack.subList(3, 2), "reversed stack range");
        String first = new String("same");
        String replacement = new String("same");
        M3PStack<String> original = M3PStack.singleton(first);
        M3PStack<String> changed = original.with(0, replacement);
        require(changed != original && changed.get(0) == replacement && original.get(0) == first,
                "stack replacement preserves equal distinct reference and old version");
        require(original.with(0, first) == original, "identical stack replacement is identity");
        Object[] array = stack.toArray();
        array[0] = 99;
        equal(stack.get(0), 1, "stack export does not alias storage");
        Iterator<Integer> iterator = stack.iterator();
        M3PStack<Integer> later = stack.plus(42).minusAll(Collections.singletonList(1));
        equal(drain(iterator), source, "stack iterator retains captured version");
        listEquals(later, Arrays.asList(42, null, 2, 3), "later stack branch");
        Iterator<Integer> exhausted = stack.iterator();
        exhausted.forEachRemaining(ignored -> { });
        expect(NoSuchElementException.class, exhausted::next, "stack iterator exhaustion");
        List<Integer> reversed = new ArrayList<>(source);
        Collections.reverse(reversed);
        listEquals(stack.reversed(), reversed, "Java 21 stack reversed order");
        equal(stack.getFirst(), 1, "Java 21 stack first");
        equal(stack.getLast(), 3, "Java 21 stack last");
        expect(NoSuchElementException.class, M3PStack.empty()::getFirst, "empty stack first");
        expect(NoSuchElementException.class, M3PStack.empty()::getLast, "empty stack last");
    }

    private static void stackVersions() {
        List<Integer> batch = Arrays.asList(91, 92, null, 91);
        List<Integer> reversedBatch = new ArrayList<>(batch);
        Collections.reverse(reversedBatch);
        for (int size : PAGE_SIZES) {
            List<Integer> source = new ArrayList<>();
            for (int index = 0; index < size; index++) source.add(index % 17 == 0 ? null : index % 23);
            M3PStack<Integer> original = M3PStack.from(source);
            int[] positions = size <= 65 ? coordinates(size) : new int[] {0, 31, 32, size / 2, size};
            for (int index : positions) {
                List<Integer> expected = new ArrayList<>(source);
                expected.addAll(index, reversedBatch);
                M3PStack<Integer> changed = original.plusAll(index, batch);
                listEquals(changed, expected, "page-boundary stack insertion");
                listEquals(original, source, "stack insertion preserves old version");
                expected.remove(index);
                M3PStack<Integer> removed = changed.minus(index);
                listEquals(removed, expected, "page-boundary stack removal");
                listEquals(removed.subList(index, index + 3), reversedBatch, 1, 4,
                        "stack slice after insertion and removal");
            }
            List<Integer> kept = new ArrayList<>(source);
            kept.removeIf(value -> value == null || value == 7);
            listEquals(original.minusAll(Arrays.asList(null, 7)), kept, "stack page-boundary all-occurrence removal");
            listEquals(original, source, "stack retained after bulk removal");
        }
    }

    private static void queueContracts() {
        M3PQueue<Integer> empty = M3PQueue.empty();
        require(empty.minus() == empty, "persistent dequeue of empty queue is identity");
        equal(empty.peek(), null, "empty queue peek");
        expect(NoSuchElementException.class, empty::element, "empty queue element");
        expect(NoSuchElementException.class, empty.iterator()::next, "empty queue iterator");
        M3PQueue<Integer> queue = M3PQueue.from(Arrays.asList(null, 1, 2, 1, null));
        equal(queue.size(), 5, "queue nulls contribute size");
        equal(queue.peek(), null, "nonempty null-headed queue peek");
        equal(queue.element(), null, "nonempty null-headed queue element");
        collectionEquals(queue.minus(), Arrays.asList(1, 2, 1, null), "queue removes first null");
        collectionEquals(queue.plus(null), Arrays.asList(null, 1, 2, 1, null, null), "null enqueue");
        collectionEquals(queue.plusAll(Arrays.asList(3, null, 4)), Arrays.asList(null, 1, 2, 1, null, 3, null, 4),
                "queue bulk enqueue preserves source order");
        collectionEquals(queue.minus((Object) 1), Arrays.asList(null, 2, 1, null),
                "queue subtracts first equal occurrence");
        collectionEquals(queue.minusAll(Arrays.asList(1, null, 1)), List.of(2),
                "queue all-occurrence subtraction follows new PCollection contract");
        collectionEquals(M3PQueue.singleton(null), Collections.singletonList(null), "null queue singleton");
        collectionEquals(queue, Arrays.asList(null, 1, 2, 1, null), "original queue contents preserved");
        require(queue.plusAll(List.of()) == queue, "empty enqueue is identity");
        require(queue.minus("missing") == queue, "absent queue subtraction is identity");
        require(queue.minusAll(Collections.singletonList(91)) == queue, "unmatched queue minusAll is identity");
        expect(NullPointerException.class, () -> List.of(91).contains(null),
                "supplied queue membership rejects null query");
        expect(NullPointerException.class, () -> queue.minusAll(List.of(91)),
                "queue propagates supplied membership null rejection");
        collectionEquals(queue, Arrays.asList(null, 1, 2, 1, null),
                "queue version preserved after failed membership query");
        require(!queue.equals(M3PQueue.from(queue)), "queue equality remains object identity");
        expect(NullPointerException.class, () -> M3PQueue.from(null), "null queue source");
        expect(NullPointerException.class, () -> queue.plusAll(null), "null queue enqueue source");
        expect(NullPointerException.class, () -> queue.minusAll(null), "null queue subtraction source");
        Object[] exported = queue.toArray();
        exported[1] = 99;
        collectionEquals(queue, Arrays.asList(null, 1, 2, 1, null), "queue array export is independent");
        Iterator<Integer> captured = queue.iterator();
        queue.plus(9).minus();
        equal(drain(captured), Arrays.asList(null, 1, 2, 1, null), "queue iterator keeps old version");
    }

    private static void queueDrainRefill() {
        for (int initial : new int[] {0, 1, 31, 32, 33, 64, 65, 257}) {
            M3PQueue<Integer> actual = M3PQueue.empty();
            AmortizedPQueue<Integer> donor = AmortizedPQueue.empty();
            ArrayDeque<Integer> expected = new ArrayDeque<>();
            List<M3PQueue<Integer>> versions = new ArrayList<>();
            List<List<Integer>> snapshots = new ArrayList<>();
            for (int value = 0; value < initial; value++) {
                actual = actual.plus(value);
                donor = donor.plus(value);
                expected.addLast(value);
            }
            for (int step = 0; step < 1_024; step++) {
                if ((step % 7) < 4 && !expected.isEmpty()) {
                    equal(actual.element(), expected.element(), "queue FIFO head");
                    actual = actual.minus();
                    donor = donor.minus();
                    expected.removeFirst();
                } else {
                    int value = step + 10_000;
                    actual = actual.plus(value);
                    donor = donor.plus(value);
                    expected.addLast(value);
                }
                equal(actual.peek(), expected.peek(), "queue head after drain/refill");
                equal(actual.size(), expected.size(), "queue size after drain/refill");
                if (step % 31 == 0) {
                    collectionEquals(actual, expected, "queue JDK oracle");
                    collectionEquals(actual, donor, "original queue oracle");
                    versions.add(actual);
                    snapshots.add(new ArrayList<>(expected));
                }
            }
            while (!expected.isEmpty()) {
                equal(actual.element(), expected.removeFirst(), "queue complete drain");
                actual = actual.minus();
            }
            require(actual.isEmpty() && actual.minus() == actual, "queue drained identity");
            for (int index = 0; index < versions.size(); index++) {
                M3PQueue<Integer> old = versions.get(index);
                collectionEquals(old, snapshots.get(index), "queue older version survives drain/refill");
                List<Integer> branch = new ArrayList<>(snapshots.get(index));
                branch.add(-index);
                collectionEquals(old.plus(-index), branch, "queue branches from old version");
                collectionEquals(old, snapshots.get(index), "queue branch preserves old version");
            }
        }
    }

    private static void sortedEnumeration() {
        for (int membership = 0; membership < (1 << SORTED_MEMBERS.length); membership++) {
            M3PSortedSet<Integer> ascendingSet = M3PSortedSet.empty();
            M3PSortedMap<Integer, String> ascendingMap = M3PSortedMap.empty();
            TreeSet<Integer> expectedSet = new TreeSet<>();
            TreeMap<Integer, String> expectedMap = new TreeMap<>();
            for (int index = SORTED_MEMBERS.length - 1; index >= 0; index--) {
                if ((membership & (1 << index)) == 0) continue;
                int key = SORTED_MEMBERS[index];
                String value = key == 0 ? null : "v" + key;
                ascendingSet = ascendingSet.plus(key);
                ascendingMap = ascendingMap.plus(key, value);
                expectedSet.add(key);
                expectedMap.put(key, value);
            }
            setEquals(ascendingSet, TreePSet.from(expectedSet), "original sorted-set oracle");
            mapEquals(ascendingMap, TreePMap.from(expectedMap), "original sorted-map oracle");
            require(ascendingSet.comparator() != null && ascendingMap.comparator() != null,
                    "natural-order comparator is explicit");
            for (boolean reverse : new boolean[] {false, true}) {
                M3PSortedSet<Integer> actualSet = reverse ? ascendingSet.descendingSet() : ascendingSet;
                M3PSortedMap<Integer, String> actualMap = reverse ? ascendingMap.descendingMap() : ascendingMap;
                NavigableSet<Integer> jdkSet = reverse ? expectedSet.descendingSet() : expectedSet;
                NavigableMap<Integer, String> jdkMap = reverse ? expectedMap.descendingMap() : expectedMap;
                sortedContents(actualSet, actualMap, jdkSet, jdkMap, "sorted orientation");
                collectionEquals(actualSet.descendingSet(), jdkSet.descendingSet(), "descending set");
                equal(drain(actualSet.descendingIterator()), new ArrayList<>(jdkSet.descendingSet()),
                        "descending iterator");
                orderedMapEquals(actualMap.descendingMap(), jdkMap.descendingMap(), "descending map");
                collectionEquals(actualSet.reversed(), jdkSet.descendingSet(), "Java 21 sorted-set reversed");
                orderedMapEquals(actualMap.reversed(), jdkMap.descendingMap(), "Java 21 sorted-map reversed");
                collectionEquals(actualMap.navigableKeySet(), jdkMap.navigableKeySet(), "navigable keys");
                collectionEquals(actualMap.descendingKeySet(), jdkMap.descendingKeySet(), "descending keys");
                equal(actualMap.firstEntry(), jdkMap.firstEntry(), "first entry including empty null");
                equal(actualMap.lastEntry(), jdkMap.lastEntry(), "last entry including empty null");
                if (jdkSet.isEmpty()) {
                    expect(NoSuchElementException.class, actualSet::first, "empty sorted-set first");
                    expect(NoSuchElementException.class, actualSet::last, "empty sorted-set last");
                    expect(NoSuchElementException.class, actualMap::firstKey, "empty sorted-map firstKey");
                    expect(NoSuchElementException.class, actualMap::lastKey, "empty sorted-map lastKey");
                    expect(NoSuchElementException.class, actualSet::minusFirst, "empty persistent minusFirst");
                    expect(NoSuchElementException.class, actualSet::minusLast, "empty persistent minusLast");
                    expect(NoSuchElementException.class, actualMap::minusFirstEntry, "empty persistent minusFirstEntry");
                    expect(NoSuchElementException.class, actualMap::minusLastEntry, "empty persistent minusLastEntry");
                } else {
                    equal(actualSet.first(), jdkSet.first(), "sorted-set first");
                    equal(actualSet.last(), jdkSet.last(), "sorted-set last");
                    equal(actualMap.firstKey(), jdkMap.firstKey(), "sorted-map firstKey");
                    equal(actualMap.lastKey(), jdkMap.lastKey(), "sorted-map lastKey");
                    List<Integer> minusFirstKeys = new ArrayList<>(jdkSet);
                    minusFirstKeys.remove(0);
                    List<Integer> minusLastKeys = new ArrayList<>(jdkSet);
                    minusLastKeys.remove(minusLastKeys.size() - 1);
                    collectionEquals(actualSet.minusFirst(), minusFirstKeys, "persistent sorted first deletion");
                    collectionEquals(actualSet.minusLast(), minusLastKeys, "persistent sorted last deletion");
                    collectionEquals(actualMap.minusFirstEntry().keySet(), minusFirstKeys, "persistent first-entry deletion");
                    collectionEquals(actualMap.minusLastEntry().keySet(), minusLastKeys, "persistent last-entry deletion");
                    expect(UnsupportedOperationException.class, () -> actualMap.firstEntry().setValue("changed"),
                            "navigation entry is immutable");
                }
                for (int query : SORTED_QUERIES) {
                    navigation(actualSet, actualMap, jdkSet, jdkMap, query);
                    for (boolean inclusive : new boolean[] {false, true}) {
                        sortedContents(actualSet.headSet(query, inclusive), actualMap.headMap(query, inclusive),
                                jdkSet.headSet(query, inclusive), jdkMap.headMap(query, inclusive), "head inclusive flag");
                        sortedContents(actualSet.tailSet(query, inclusive), actualMap.tailMap(query, inclusive),
                                jdkSet.tailSet(query, inclusive), jdkMap.tailMap(query, inclusive), "tail inclusive flag");
                    }
                    sortedContents(actualSet.headSet(query), actualMap.headMap(query),
                            jdkSet.headSet(query, false), jdkMap.headMap(query, false), "default head exclusivity");
                    sortedContents(actualSet.tailSet(query), actualMap.tailMap(query),
                            jdkSet.tailSet(query, true), jdkMap.tailMap(query, true), "default tail inclusivity");
                    for (int end : SORTED_QUERIES) {
                        boolean invalid = actualSet.comparator().compare(query, end) > 0;
                        for (boolean fromInclusive : new boolean[] {false, true}) {
                            for (boolean toInclusive : new boolean[] {false, true}) {
                                if (invalid) {
                                    expect(IllegalArgumentException.class,
                                            () -> actualSet.subSet(query, fromInclusive, end, toInclusive),
                                            "inverted sorted-set endpoints");
                                    expect(IllegalArgumentException.class,
                                            () -> actualMap.subMap(query, fromInclusive, end, toInclusive),
                                            "inverted sorted-map endpoints");
                                } else {
                                    sortedContents(actualSet.subSet(query, fromInclusive, end, toInclusive),
                                            actualMap.subMap(query, fromInclusive, end, toInclusive),
                                            jdkSet.subSet(query, fromInclusive, end, toInclusive),
                                            jdkMap.subMap(query, fromInclusive, end, toInclusive), "subrange inclusivity");
                                }
                            }
                        }
                        if (!invalid) {
                            sortedContents(actualSet.subSet(query, end), actualMap.subMap(query, end),
                                    jdkSet.subSet(query, true, end, false), jdkMap.subMap(query, true, end, false),
                                    "default half-open subrange");
                        }
                    }
                }
                sortedContents(actualSet, actualMap, jdkSet, jdkMap, "ranges preserve source version");
            }
        }
    }

    private static <K, V> void navigation(M3PSortedSet<K> set, M3PSortedMap<K, V> map,
                                          NavigableSet<K> expectedSet, NavigableMap<K, V> expectedMap, K query) {
        equal(set.lower(query), expectedSet.lower(query), "sorted lower");
        equal(set.floor(query), expectedSet.floor(query), "sorted floor");
        equal(set.ceiling(query), expectedSet.ceiling(query), "sorted ceiling");
        equal(set.higher(query), expectedSet.higher(query), "sorted higher");
        equal(map.lowerKey(query), expectedMap.lowerKey(query), "map lower key");
        equal(map.floorKey(query), expectedMap.floorKey(query), "map floor key");
        equal(map.ceilingKey(query), expectedMap.ceilingKey(query), "map ceiling key");
        equal(map.higherKey(query), expectedMap.higherKey(query), "map higher key");
        equal(map.lowerEntry(query), expectedMap.lowerEntry(query), "map lower entry");
        equal(map.floorEntry(query), expectedMap.floorEntry(query), "map floor entry");
        equal(map.ceilingEntry(query), expectedMap.ceilingEntry(query), "map ceiling entry");
        equal(map.higherEntry(query), expectedMap.higherEntry(query), "map higher entry");
        equal(set.contains(query), expectedSet.contains(query), "sorted membership");
        equal(map.containsKey(query), expectedMap.containsKey(query), "sorted map membership");
        equal(map.get(query), expectedMap.get(query), "sorted map lookup");
    }

    private static void sortedRepresentativesAndSnapshots() {
        Comparator<String> order = String.CASE_INSENSITIVE_ORDER;
        String key = new String("Alpha");
        String equalKey = new String("ALPHA");
        String value = new String("value");
        String equalValue = new String("value");
        M3PSortedSet<String> set = M3PSortedSet.singleton(order, key);
        require(set.plus(equalKey) == set && set.first() == key, "sorted set retains comparator representative");
        M3PSortedMap<String, String> map = M3PSortedMap.singleton(order, key, value);
        M3PSortedMap<String, String> changed = map.plus(equalKey, equalValue);
        require(changed != map && changed.firstKey() == key && changed.get(equalKey) == equalValue,
                "sorted map retains first key and incoming equal value reference");
        require(map.firstKey() == key && map.get(equalKey) == value, "sorted map old representatives retained");
        require(map.plus(equalKey, value) == map, "identical sorted value update is identity");
        require(changed.minus(equalKey).isEmpty(), "comparator-equivalent subtraction");
        require(map.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(equalKey, equalValue)),
                "sorted entry membership uses key comparator and value equality");
        M3PSortedMap<String, String> withOther = changed.plus("beta", value);
        List<String> actualValues = new ArrayList<>(withOther.values());
        require(actualValues.get(0) == equalValue && actualValues.get(1) == value,
                "sorted values do not deduplicate equal objects");
        M3PSortedSet<Integer> numbers = M3PSortedSet.from(List.of(1, 2, 3, 4, 5));
        M3PSortedMap<Integer, String> numbered = M3PSortedMap.<Integer, String>empty()
                .plus(1, "one").plus(2, "two").plus(3, "three").plus(4, "four").plus(5, "five");
        orderedMapEquals(M3PSortedMap.from(numbered), numbered, "natural sorted-map factory");
        orderedMapEquals(M3PSortedMap.from(order, withOther), withOther, "custom sorted-map factory");
        collectionEquals(M3PSortedSet.from(order, withOther.keySet()), withOther.keySet(),
                "custom sorted-set factory");
        M3PSortedSet<Integer> clipped = numbers.subSet(2, true, 4, false);
        M3PSortedMap<Integer, String> clippedMap = numbered.subMap(2, true, 4, false);
        collectionEquals(clipped.headSet(99).tailSet(-99), List.of(2, 3), "range composition clips without fences");
        collectionEquals(clippedMap.headMap(99).tailMap(-99).keySet(), List.of(2, 3),
                "map range composition clips without fences");
        collectionEquals(clipped.plus(99).plus(-99), List.of(-99, 2, 3, 99),
                "persistent set additions may leave former bounds");
        collectionEquals(clippedMap.plus(99, "high").plus(-99, "low").keySet(), List.of(-99, 2, 3, 99),
                "persistent map additions may leave former bounds");
        collectionEquals(clipped.descendingSet().headSet(-99).tailSet(99), List.of(3, 2),
                "descending range composition has no fences");
        collectionEquals(clippedMap.descendingMap().headMap(-99).tailMap(99).keySet(), List.of(3, 2),
                "descending map range composition has no fences");
        collectionEquals(clipped, List.of(2, 3), "clipped set remains unchanged");
        collectionEquals(clippedMap.keySet(), List.of(2, 3), "clipped map remains unchanged");
        collectionEquals(numbers.intersect(List.of(4, 2, 2, 99)), List.of(2, 4), "sorted intersection order");
        collectionEquals(numbers.descendingSet().intersect(List.of(4, 2, 2)), List.of(4, 2),
                "descending intersection order");
        require(numbers.minus(99) == numbers, "absent sorted removal is identity");
        require(numbered.minus(99) == numbered, "absent sorted-map removal is identity");
        require(numbers.plusAll(List.of()) == numbers && numbered.plusAll(Map.of()) == numbered,
                "empty sorted additions are identity");
        require(numbers.minusAll(List.of()) == numbers && numbered.minusAll(List.of()) == numbered,
                "empty sorted subtraction is identity");
        expect(NullPointerException.class, () -> M3PSortedSet.<Integer>empty(null), "null sorted comparator");
        expect(NullPointerException.class, () -> M3PSortedMap.<Integer, String>empty(null), "null map comparator");
        expect(NullPointerException.class, () -> M3PSortedSet.<Integer>singleton(null), "natural null singleton set");
        expect(NullPointerException.class, () -> M3PSortedMap.<Integer, String>singleton(null, "x"),
                "natural null singleton map");
        expect(NullPointerException.class, () -> M3PSortedSet.<Integer>empty().contains(null), "natural null lookup");
        expect(NullPointerException.class, () -> M3PSortedMap.<Integer, String>empty().get(null), "natural null map lookup");
        expect(NullPointerException.class, () -> numbers.plus(null), "natural null set insertion");
        expect(NullPointerException.class, () -> numbered.plus(null, "x"), "natural null map insertion");
        expect(NullPointerException.class, () -> numbers.headSet(null), "natural null range endpoint");
        expect(NullPointerException.class, () -> numbered.tailMap(null), "natural null map endpoint");
        expect(NullPointerException.class, () -> numbers.plusAll(null), "null sorted source");
        expect(NullPointerException.class, () -> numbered.plusAll(null), "null sorted-map source");
        expect(NullPointerException.class, () -> numbers.minusAll(null), "null sorted subtraction");
        expect(NullPointerException.class, () -> numbered.minusAll(null), "null sorted-map subtraction");
        expect(NullPointerException.class, () -> numbers.intersect(null), "null sorted intersection");
        nullFriendlySorting();
    }

    private static void nullFriendlySorting() {
        Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.naturalOrder());
        List<Integer> members = Arrays.asList(3, null, -1, 0, 3);
        M3PSortedSet<Integer> set = M3PSortedSet.from(comparator, members);
        M3PSortedMap<Integer, String> map = M3PSortedMap.<Integer, String>empty(comparator)
                .plus(3, "three").plus(null, null).plus(-1, "negative").plus(0, "zero");
        TreeSet<Integer> jdkSet = new TreeSet<>(comparator);
        jdkSet.addAll(members);
        TreeMap<Integer, String> jdkMap = new TreeMap<>(comparator);
        jdkMap.put(3, "three");
        jdkMap.put(null, null);
        jdkMap.put(-1, "negative");
        jdkMap.put(0, "zero");
        for (boolean reverse : new boolean[] {false, true}) {
            M3PSortedSet<Integer> actualSet = reverse ? set.descendingSet() : set;
            M3PSortedMap<Integer, String> actualMap = reverse ? map.descendingMap() : map;
            NavigableSet<Integer> expectedSet = reverse ? jdkSet.descendingSet() : jdkSet;
            NavigableMap<Integer, String> expectedMap = reverse ? jdkMap.descendingMap() : jdkMap;
            sortedContents(actualSet, actualMap, expectedSet, expectedMap, "null-friendly comparator order");
            for (Integer query : Arrays.asList(null, -2, -1, 0, 1, 3, 4)) {
                navigation(actualSet, actualMap, expectedSet, expectedMap, query);
                for (boolean inclusive : new boolean[] {false, true}) {
                    sortedContents(actualSet.headSet(query, inclusive), actualMap.headMap(query, inclusive),
                            expectedSet.headSet(query, inclusive), expectedMap.headMap(query, inclusive),
                            "null-friendly head bound");
                    sortedContents(actualSet.tailSet(query, inclusive), actualMap.tailMap(query, inclusive),
                            expectedSet.tailSet(query, inclusive), expectedMap.tailMap(query, inclusive),
                            "null-friendly tail bound");
                }
            }
        }
        require(set.contains(null) && map.containsKey(null), "custom comparator permits null membership");
        require(!set.minus(null).contains(null) && !map.minus(null).containsKey(null),
                "null-friendly subtraction");
        require(map.plus(null, "nonnull").firstEntry().getKey() == null, "null key retained on update");
        equal(map.get(null), null, "null-valued original null key retained");
    }

    private static void sortedVersions() {
        M3PSortedMap<Integer, String> map = M3PSortedMap.empty();
        M3PSortedSet<Integer> set = M3PSortedSet.empty();
        TreeMap<Integer, String> expectedMap = new TreeMap<>();
        TreeSet<Integer> expectedSet = new TreeSet<>();
        List<M3PSortedMap<Integer, String>> mapVersions = new ArrayList<>();
        List<M3PSortedSet<Integer>> setVersions = new ArrayList<>();
        List<TreeMap<Integer, String>> mapSnapshots = new ArrayList<>();
        for (int step = 0; step < 2_048; step++) {
            int key = ((step * 73) ^ (step >>> 2)) & 511;
            if (step % 5 < 3) {
                String value = step % 13 == 0 ? null : "value-" + step;
                map = map.plus(key, value);
                set = set.plus(key);
                expectedMap.put(key, value);
                expectedSet.add(key);
            } else {
                map = map.minus(key);
                set = set.minus(key);
                expectedMap.remove(key);
                expectedSet.remove(key);
            }
            if (step % 37 == 0) {
                sortedContents(set, map, expectedSet, expectedMap, "sorted deterministic update schedule");
                mapVersions.add(map);
                setVersions.add(set);
                mapSnapshots.add(new TreeMap<>(expectedMap));
            }
        }
        sortedContents(set, map, expectedSet, expectedMap, "sorted final schedule");
        for (int index = 0; index < mapVersions.size(); index++) {
            M3PSortedMap<Integer, String> oldMap = mapVersions.get(index);
            M3PSortedSet<Integer> oldSet = setVersions.get(index);
            TreeMap<Integer, String> expected = mapSnapshots.get(index);
            sortedContents(oldSet, oldMap, expected.navigableKeySet(), expected, "sorted old version retained");
            TreeMap<Integer, String> branch = new TreeMap<>(expected);
            branch.put(10_000 + index, "branch");
            sortedContents(oldSet.plus(10_000 + index), oldMap.plus(10_000 + index, "branch"),
                    branch.navigableKeySet(), branch, "sorted branch from old version");
            sortedContents(oldSet, oldMap, expected.navigableKeySet(), expected, "sorted branch preserves original");
        }
    }

    private static void orderedMaps() throws Exception {
        M3POrderedMap<CollisionKey, String> actual = M3POrderedMap.empty();
        LinkedHashMap<CollisionKey, String> expected = new LinkedHashMap<>();
        OrderedPMap<CollisionKey, String> donor = OrderedPMap.empty();
        List<M3POrderedMap<CollisionKey, String>> versions = new ArrayList<>();
        List<LinkedHashMap<CollisionKey, String>> snapshots = new ArrayList<>();
        for (int index = 0; index < 513; index++) {
            CollisionKey key = index % 31 == 0 ? null : new CollisionKey(index);
            String value = index % 17 == 0 ? null : "v" + index;
            actual = actual.plus(key, value);
            expected.put(key, value);
            donor = donor.plus(key, value);
            if (index % 32 == 0) {
                orderedMapEquals(actual, expected, "ordered insertion boundaries");
                orderedMapEquals(actual, donor, "original ordered-map insertion");
                versions.add(actual);
                snapshots.add(new LinkedHashMap<>(expected));
            }
        }
        Iterator<Map.Entry<CollisionKey, String>> captured = actual.entrySet().iterator();
        List<Map.Entry<CollisionKey, String>> capturedExpected = new ArrayList<>(expected.entrySet());
        for (int index = 0; index < 513; index++) {
            CollisionKey key = index % 31 == 0 ? null : new CollisionKey(index);
            actual = actual.minus(key);
            expected.remove(key);
            donor = donor.minus(key);
            if (index % 13 == 0) {
                orderedMapEquals(actual, expected, "ordered deletion and compaction boundaries");
                orderedMapEquals(actual, donor, "original ordered-map deletion");
                orderedRetentionBound(actual);
            }
            if (index % 47 == 0) {
                versions.add(actual);
                snapshots.add(new LinkedHashMap<>(expected));
            }
        }
        require(actual.isEmpty(), "ordered full deletion");
        equal(drain(captured), capturedExpected, "ordered entry iterator retains pre-compaction version");
        for (int index = 0; index < versions.size(); index++) {
            M3POrderedMap<CollisionKey, String> old = versions.get(index);
            LinkedHashMap<CollisionKey, String> snapshot = snapshots.get(index);
            orderedMapEquals(old, snapshot, "ordered old version retained");
            CollisionKey newKey = new CollisionKey(index + 20_000);
            LinkedHashMap<CollisionKey, String> branch = new LinkedHashMap<>(snapshot);
            branch.put(newKey, "branch");
            orderedMapEquals(old.plus(newKey, "branch"), branch, "ordered branch from retained version");
            orderedMapEquals(old, snapshot, "ordered branch preserves old version");
        }
        String key = new String("key");
        String equalKey = new String("key");
        String value = new String("same");
        String equalValue = new String("same");
        M3POrderedMap<String, String> original = M3POrderedMap.<String, String>empty()
                .plus(key, value).plus("second", "other").plus(null, null);
        M3POrderedMap<String, String> replaced = original.plus(equalKey, equalValue);
        require(replaced.keySet().iterator().next() == key, "ordered map keeps first key reference");
        require(replaced.get(key) == equalValue && original.get(key) == value,
                "ordered map incoming value identity and old version");
        require(replaced.plus(equalKey, equalValue) == replaced, "identical ordered value update is identity");
        collectionEquals(replaced.keySet(), Arrays.asList(key, "second", null), "ordered update keeps ordinal");
        M3POrderedMap<String, String> reinserted = replaced.minus(equalKey).plus(equalKey, value);
        collectionEquals(reinserted.keySet(), Arrays.asList("second", null, equalKey), "ordered removal and reinsertion append");
        List<String> newKeys = new ArrayList<>(reinserted.keySet());
        require(newKeys.get(2) == equalKey, "reinsertion takes newly inserted key reference");
        require(original.containsKey(null) && original.get(null) == null, "ordered null mapping");
        LinkedHashMap<String, String> incoming = new LinkedHashMap<>();
        incoming.put("second", "updated");
        incoming.put("third", "third-value");
        incoming.put("key", "last-value");
        M3POrderedMap<String, String> batch = original.plusAll(incoming);
        collectionEquals(batch.keySet(), Arrays.asList(key, "second", null, "third"),
                "ordered plusAll uses source encounter order for new keys");
        require(batch.keySet().iterator().next() == key, "ordered plusAll retains stored key reference");
        equal(batch.get(key), "last-value", "ordered plusAll replacement");
        orderedMapEquals(M3POrderedMap.from(incoming), incoming, "ordered map factory encounter order");
        require(original.plusAll(Map.of()) == original, "empty ordered-map addition is identity");
        require(original.minus("missing") == original, "missing ordered-map subtraction is identity");
        require(original.minusAll(List.of()) == original, "empty ordered-map subtraction is identity");
        require(original.minusAll(original.keySet()).isEmpty(), "ordered minusAll accepts captured key view");
        expect(NullPointerException.class, () -> M3POrderedMap.from(null), "null ordered-map factory input");
        expect(NullPointerException.class, () -> original.plusAll(null), "null ordered-map addition source");
        expect(NullPointerException.class, () -> original.minusAll(null), "null ordered-map subtraction source");
        orderedMapEquals(M3POrderedMap.singleton(null, null), Collections.singletonMap(null, null),
                "ordered null singleton");
    }

    private static void orderedRetentionBound(M3POrderedMap<?, ?> map) throws Exception {
        Field slotsField = M3POrderedMap.class.getDeclaredField("slots");
        slotsField.setAccessible(true);
        Collection<?> physicalSlots = (Collection<?>) slotsField.get(map);
        require((physicalSlots.size() & 1) == 0, "ordered payload lanes have complete key/value pairs");
        require(physicalSlots.size() / 2L <= Math.max(64L, 2L * map.size()),
                "documented ordered dead-slot retention bound");
    }

    private static void orderedSets() {
        M3POrderedSet<Integer> actual = M3POrderedSet.empty();
        LinkedHashSet<Integer> expected = new LinkedHashSet<>();
        OrderedPSet<Integer> donor = OrderedPSet.empty();
        List<M3POrderedSet<Integer>> versions = new ArrayList<>();
        List<LinkedHashSet<Integer>> snapshots = new ArrayList<>();
        for (int step = 0; step < 3_072; step++) {
            Integer value = step % 31 == 0 ? null : ((step * 97) ^ (step >>> 3)) & 511;
            if (step % 7 < 4) {
                actual = actual.plus(value);
                expected.add(value);
                donor = donor.plus(value);
            } else {
                actual = actual.minus(value);
                expected.remove(value);
                donor = donor.minus(value);
            }
            if (step % 29 == 0) {
                setEquals(actual, expected, "ordered set deterministic membership");
                collectionEquals(actual, expected, "ordered set deterministic encounter order");
                collectionEquals(actual, donor, "original ordered-set oracle");
                versions.add(actual);
                snapshots.add(new LinkedHashSet<>(expected));
            }
        }
        for (int index = 0; index < versions.size(); index++) {
            M3POrderedSet<Integer> old = versions.get(index);
            LinkedHashSet<Integer> oldExpected = snapshots.get(index);
            setEquals(old, oldExpected, "ordered set retained membership");
            collectionEquals(old, oldExpected, "ordered set retained order");
            LinkedHashSet<Integer> branch = new LinkedHashSet<>(oldExpected);
            branch.add(10_000 + index);
            collectionEquals(old.plus(10_000 + index), branch, "ordered set old-version branch");
            collectionEquals(old, oldExpected, "ordered set original remains unchanged");
        }
        String first = new String("first");
        String equalFirst = new String("first");
        M3POrderedSet<String> original = M3POrderedSet.from(Arrays.asList(first, "second", null, first));
        require(original.plus(equalFirst) == original && original.iterator().next() == first,
                "ordered set duplicate preserves first representative");
        M3POrderedSet<String> reinserted = original.minus(equalFirst).plus(equalFirst);
        collectionEquals(reinserted, Arrays.asList("second", null, first), "ordered set reinsertion appends");
        List<String> reinsertedValues = new ArrayList<>(reinserted);
        require(reinsertedValues.get(2) == equalFirst, "ordered set reinsertion adopts new reference");
        collectionEquals(original.plusAll(Arrays.asList("second", "third", null, "fourth")),
                Arrays.asList(first, "second", null, "third", "fourth"), "ordered set plusAll order");
        collectionEquals(original.intersect(Arrays.asList(null, first, first, "missing")),
                Arrays.asList(first, null), "ordered intersection keeps original order");
        collectionEquals(original.minusAll(Arrays.asList(first, null, first)), List.of("second"),
                "ordered set bulk subtraction");
        require(original.plusAll(List.of()) == original, "empty ordered-set addition is identity");
        require(original.minus("missing") == original, "absent ordered-set subtraction is identity");
        require(original.minusAll(List.of()) == original, "empty ordered-set subtraction is identity");
        require(original.minusAll(original).isEmpty(), "ordered self-subtraction preserves input traversal");
        collectionEquals(M3POrderedSet.singleton(null), Collections.singletonList(null), "ordered null singleton set");
        expect(NullPointerException.class, () -> M3POrderedSet.from(null), "null ordered-set source");
        expect(NullPointerException.class, () -> original.plusAll(null), "null ordered-set addition source");
        expect(NullPointerException.class, () -> original.minusAll(null), "null ordered-set subtraction source");
        expect(NullPointerException.class, () -> original.intersect(null), "null ordered-set intersection");
    }

    private static void streamEncounterOrder() {
        ArrayList<Integer> fifo = new ArrayList<>();
        orderedStream(M3PQueue.empty(), fifo, "empty queue streams");
        for (int index = 0; index < 1_025; index++) fifo.add(index % 31 == 0 ? null : index);
        M3PQueue<Integer> queue = M3PQueue.from(fifo);
        M3PQueue<Integer> retainedQueue = queue;
        ArrayList<Integer> retainedFifo = new ArrayList<>(fifo);
        orderedStream(queue, fifo, "queue FIFO streams with null payloads");
        for (int count = 0; count < 800; count++) {
            queue = queue.minus();
            fifo.remove(0);
        }
        for (int index = 0; index < 129; index++) {
            Integer value = index % 17 == 0 ? null : -index;
            queue = queue.plus(value);
            fifo.add(value);
        }
        orderedStream(queue, fifo, "queue FIFO streams after drain and refill");
        orderedStream(retainedQueue, retainedFifo, "retained queue FIFO streams");
        while (!fifo.isEmpty()) {
            queue = queue.minus();
            fifo.remove(0);
        }
        orderedStream(queue, fifo, "fully drained queue streams");
        fifo.add(null);
        fifo.add(7);
        fifo.add(9);
        queue = queue.plusAll(fifo);
        orderedStream(queue, fifo, "queue FIFO streams refilled after empty");

        LinkedHashMap<Integer, String> inserted = new LinkedHashMap<>();
        for (int index = 0; index < 1_025; index++) {
            int key = (index * 73) % 1_025;
            inserted.put(key, key % 31 == 0 ? null : "v" + key);
        }
        M3POrderedMap<Integer, String> orderedMap = M3POrderedMap.from(inserted);
        M3POrderedSet<Integer> orderedSet = M3POrderedSet.from(inserted.keySet());
        LinkedHashSet<Integer> expectedSet = new LinkedHashSet<>(inserted.keySet());
        mapStreamOrder(orderedMap, inserted, "ordered map streams");
        orderedStream(orderedSet, expectedSet, "ordered set streams");
        for (int key = 0; key < 800; key++) {
            orderedMap = orderedMap.minus(key);
            orderedSet = orderedSet.minus(key);
            inserted.remove(key);
            expectedSet.remove(key);
        }
        orderedMap = orderedMap.plus(null, "null-key").plus(17, "reinserted");
        orderedSet = orderedSet.plus(null).plus(17);
        inserted.put(null, "null-key");
        inserted.put(17, "reinserted");
        expectedSet.add(null);
        expectedSet.add(17);
        mapStreamOrder(orderedMap, inserted, "ordered map streams after compaction and reinsertion");
        orderedStream(orderedSet, expectedSet, "ordered set streams after compaction and reinsertion");
        mapStreamOrder(M3POrderedMap.<Integer, String>empty(), Map.of(), "empty ordered map streams");
        orderedStream(M3POrderedSet.empty(), Set.of(), "empty ordered set streams");

        TreeMap<Integer, String> sorted = new TreeMap<>();
        for (int key = 0; key < 1_025; key++) sorted.put(key, key % 31 == 0 ? null : "v" + key);
        M3PSortedMap<Integer, String> sortedMap = M3PSortedMap.from(sorted);
        sortedStreamOrder(sortedMap, sorted, "sorted map streams");
        sortedStreamOrder(sortedMap.subMap(200, true, 900, false),
                sorted.subMap(200, true, 900, false), "sorted range streams");
        sortedStreamOrder(M3PSortedMap.<Integer, String>empty(), new TreeMap<>(), "empty sorted map streams");
    }

    private static <K, V> void sortedStreamOrder(M3PSortedMap<K, V> map, NavigableMap<K, V> expected,
                                                String context) {
        for (boolean reverse : new boolean[] {false, true}) {
            M3PSortedMap<K, V> actual = reverse ? map.descendingMap() : map;
            NavigableMap<K, V> reference = reverse ? expected.descendingMap() : expected;
            String orientation = context + (reverse ? " descending" : " ascending");
            mapStreamOrder(actual, reference, orientation);
            orderedStream(actual.sequencedKeySet(), reference.keySet(), orientation + " sequenced keys");
            orderedStream(actual.sequencedValues(), reference.values(), orientation + " sequenced values");
            orderedStream(actual.sequencedEntrySet(), reference.entrySet(), orientation + " sequenced entries");
            orderedStream(actual.sequencedKeySet().reversed(), reference.descendingKeySet(),
                    orientation + " reversed sequenced keys");
            orderedStream(actual.sequencedValues().reversed(), reference.descendingMap().values(),
                    orientation + " reversed sequenced values");
            orderedStream(actual.sequencedEntrySet().reversed(), reference.descendingMap().entrySet(),
                    orientation + " reversed sequenced entries");
        }
    }

    private static <K, V> void mapStreamOrder(Map<K, V> map, Map<K, V> expected, String context) {
        orderedStream(map.keySet(), expected.keySet(), context + " keys");
        orderedStream(map.values(), expected.values(), context + " values");
        orderedStream(map.entrySet(), expected.entrySet(), context + " entries");
    }

    private static void orderedStream(Collection<?> actual, Collection<?> expected, String context) {
        require(actual.spliterator().hasCharacteristics(Spliterator.ORDERED), context + " ORDERED characteristic");
        List<?> encounter = new ArrayList<>(expected);
        equal(actual.stream().toList(), encounter, context + " sequential stream encounter order");
        equal(actual.parallelStream().toList(), encounter, context + " parallel stream encounter order");
    }

    private static void mutationRefusal() {
        for (M3PStack<Integer> stack : List.of(M3PStack.<Integer>empty(), M3PStack.from(List.of(1, 2, 3)))) {
            unmodifiableList(stack, 1, "stack");
            unmodifiableList(stack.reversed(), 1, "reversed stack");
            unmodifiableList(stack.subList(0, stack.size()), 1, "stack slice");
        }
        for (M3PQueue<Integer> queue : List.of(M3PQueue.<Integer>empty(), M3PQueue.from(List.of(1, 2)))) {
            unmodifiableCollection(queue, 1, "queue");
            unmodifiableQueue(queue, 1);
        }
        for (M3PSortedSet<Integer> set : List.of(M3PSortedSet.<Integer>empty(), M3PSortedSet.from(List.of(1, 2, 3)))) {
            unmodifiableSortedSet(set, 1, "sorted set");
            unmodifiableSortedSet(set.descendingSet(), 1, "descending sorted set");
            unmodifiableSortedSet(set.headSet(2), 1, "sorted set range");
        }
        for (M3PSortedMap<Integer, String> map : List.of(M3PSortedMap.<Integer, String>empty(),
                M3PSortedMap.<Integer, String>empty().plus(1, "one").plus(2, "two"))) {
            unmodifiableSortedMap(map, 1, "one", "sorted map");
            unmodifiableSortedMap(map.descendingMap(), 1, "one", "descending sorted map");
            unmodifiableSortedMap(map.headMap(2), 1, "one", "sorted map range");
        }
        for (M3POrderedMap<Integer, String> map : List.of(M3POrderedMap.<Integer, String>empty(),
                M3POrderedMap.<Integer, String>empty().plus(1, "one").plus(2, "two"))) {
            unmodifiableMap(map, 1, "one", "ordered map");
        }
        for (M3POrderedSet<Integer> set : List.of(M3POrderedSet.<Integer>empty(), M3POrderedSet.from(List.of(1, 2)))) {
            unmodifiableCollection(set, 1, "ordered set");
        }
    }

    @SuppressWarnings("deprecation")
    private static <E> void unmodifiableCollection(Collection<E> collection, E sample, String context) {
        List<E> before = new ArrayList<>(collection);
        expect(UnsupportedOperationException.class, () -> collection.add(sample), context + " add");
        expect(UnsupportedOperationException.class, () -> collection.addAll(List.of()), context + " empty addAll");
        expect(UnsupportedOperationException.class, collection::clear, context + " clear");
        expect(UnsupportedOperationException.class, () -> collection.remove(sample), context + " remove");
        expect(UnsupportedOperationException.class, () -> collection.remove(new Object()), context + " absent remove");
        expect(UnsupportedOperationException.class, () -> collection.removeAll(List.of()), context + " empty removeAll");
        expect(UnsupportedOperationException.class, () -> collection.retainAll(before), context + " unchanged retainAll");
        expect(UnsupportedOperationException.class, () -> collection.removeIf(ignored -> false), context + " no-op removeIf");
        expect(UnsupportedOperationException.class, () -> collection.removeIf(null), context + " null removeIf");
        Iterator<E> iterator = collection.iterator();
        if (iterator.hasNext()) iterator.next();
        expect(UnsupportedOperationException.class, iterator::remove, context + " iterator remove");
        collectionEquals(collection, before, context + " remains unchanged after refused mutations");
    }

    @SuppressWarnings("deprecation")
    private static <E> void unmodifiableList(List<E> list, E sample, String context) {
        unmodifiableCollection(list, sample, context);
        expect(UnsupportedOperationException.class, () -> list.add(0, sample), context + " indexed add");
        expect(UnsupportedOperationException.class, () -> list.addAll(0, List.of()), context + " empty indexed addAll");
        expect(UnsupportedOperationException.class, () -> list.set(0, sample), context + " set");
        expect(UnsupportedOperationException.class, () -> list.remove(0), context + " indexed remove");
        expect(UnsupportedOperationException.class, () -> list.replaceAll(value -> value), context + " identity replaceAll");
        expect(UnsupportedOperationException.class, () -> list.sort((left, right) -> 0), context + " no-op sort");
        expect(UnsupportedOperationException.class, () -> list.addFirst(sample), context + " Java 21 addFirst");
        expect(UnsupportedOperationException.class, () -> list.addLast(sample), context + " Java 21 addLast");
        expect(UnsupportedOperationException.class, list::removeFirst, context + " Java 21 removeFirst");
        expect(UnsupportedOperationException.class, list::removeLast, context + " Java 21 removeLast");
        ListIterator<E> iterator = list.listIterator();
        if (iterator.hasNext()) {
            iterator.next();
            expect(UnsupportedOperationException.class, () -> iterator.set(sample), context + " iterator set");
            expect(UnsupportedOperationException.class, iterator::remove, context + " list iterator remove");
        }
        expect(UnsupportedOperationException.class, () -> iterator.add(sample), context + " iterator add");
    }

    @SuppressWarnings("deprecation")
    private static <E> void unmodifiableQueue(Queue<E> queue, E sample) {
        expect(UnsupportedOperationException.class, () -> queue.offer(sample), "queue offer");
        expect(UnsupportedOperationException.class, queue::poll, "queue poll including empty");
        expect(UnsupportedOperationException.class, queue::remove, "queue destructive remove including empty");
    }

    @SuppressWarnings("deprecation")
    private static <E> void unmodifiableSortedSet(NavigableSet<E> set, E sample, String context) {
        unmodifiableCollection(set, sample, context);
        expect(UnsupportedOperationException.class, set::pollFirst, context + " pollFirst");
        expect(UnsupportedOperationException.class, set::pollLast, context + " pollLast");
        unmodifiableSequenced(set, sample, context);
    }

    private static <E> void unmodifiableSequenced(SequencedCollection<E> collection, E sample, String context) {
        unmodifiableCollection(collection, sample, context + " sequenced collection");
        expect(UnsupportedOperationException.class, () -> collection.addFirst(sample), context + " sequenced addFirst");
        expect(UnsupportedOperationException.class, () -> collection.addLast(sample), context + " sequenced addLast");
        expect(UnsupportedOperationException.class, collection::removeFirst, context + " sequenced removeFirst");
        expect(UnsupportedOperationException.class, collection::removeLast, context + " sequenced removeLast");
    }

    @SuppressWarnings("deprecation")
    private static <K, V> void unmodifiableMap(Map<K, V> map, K key, V value, String context) {
        Map<K, V> before = new LinkedHashMap<>(map);
        expect(UnsupportedOperationException.class, () -> map.put(key, value), context + " put");
        expect(UnsupportedOperationException.class, () -> map.putAll(Map.of()), context + " empty putAll");
        expect(UnsupportedOperationException.class, map::clear, context + " clear");
        expect(UnsupportedOperationException.class, () -> map.remove(key), context + " remove");
        expect(UnsupportedOperationException.class, () -> map.remove(new Object()), context + " absent remove");
        expect(UnsupportedOperationException.class, () -> map.remove(key, new Object()), context + " conditional remove mismatch");
        expect(UnsupportedOperationException.class, () -> map.replace(key, value), context + " replace");
        expect(UnsupportedOperationException.class, () -> map.replace(key, null, value), context + " conditional replace");
        expect(UnsupportedOperationException.class, () -> map.putIfAbsent(key, value), context + " putIfAbsent");
        expect(UnsupportedOperationException.class, () -> map.replaceAll((k, v) -> {
            throw new AssertionError("replaceAll callback must not run");
        }), context + " replaceAll refuses before callback");
        expect(UnsupportedOperationException.class, () -> map.compute(key, (k, v) -> {
            throw new AssertionError("compute callback must not run");
        }), context + " compute refuses before callback");
        expect(UnsupportedOperationException.class, () -> map.computeIfAbsent(key, k -> {
            throw new AssertionError("computeIfAbsent callback must not run");
        }), context + " computeIfAbsent refuses before callback");
        expect(UnsupportedOperationException.class, () -> map.computeIfPresent(key, (k, v) -> {
            throw new AssertionError("computeIfPresent callback must not run");
        }), context + " computeIfPresent refuses before callback");
        expect(UnsupportedOperationException.class, () -> map.merge(key, value, (a, b) -> {
            throw new AssertionError("merge callback must not run");
        }), context + " merge refuses before callback");
        unmodifiableCollection(map.keySet(), key, context + " keys");
        unmodifiableCollection(map.values(), value, context + " values");
        unmodifiableCollection(map.entrySet(), new AbstractMap.SimpleImmutableEntry<>(key, value), context + " entries");
        for (Map.Entry<K, V> entry : map.entrySet()) {
            expect(UnsupportedOperationException.class, () -> entry.setValue(entry.getValue()), context + " entry setValue");
        }
        orderedMapEquals(map, before, context + " refused mutations preserve map and order");
    }

    @SuppressWarnings("deprecation")
    private static <K, V> void unmodifiableSortedMap(NavigableMap<K, V> map, K key, V value, String context) {
        unmodifiableMap(map, key, value, context);
        expect(UnsupportedOperationException.class, map::pollFirstEntry, context + " pollFirstEntry");
        expect(UnsupportedOperationException.class, map::pollLastEntry, context + " pollLastEntry");
        expect(UnsupportedOperationException.class, () -> map.putFirst(key, value), context + " Java 21 putFirst");
        expect(UnsupportedOperationException.class, () -> map.putLast(key, value), context + " Java 21 putLast");
        unmodifiableSortedSet(map.navigableKeySet(), key, context + " navigable keys");
        unmodifiableSortedSet(map.descendingKeySet(), key, context + " descending keys");
        unmodifiableSequenced(map.sequencedKeySet(), key, context + " sequenced keys");
        unmodifiableSequenced(map.sequencedValues(), value, context + " sequenced values");
        unmodifiableSequenced(map.sequencedEntrySet(), new AbstractMap.SimpleImmutableEntry<>(key, value),
                context + " sequenced entries");
        unmodifiableSequenced(map.sequencedKeySet().reversed(), key, context + " reversed sequenced keys");
        unmodifiableSequenced(map.sequencedValues().reversed(), value, context + " reversed sequenced values");
        unmodifiableSequenced(map.sequencedEntrySet().reversed(), new AbstractMap.SimpleImmutableEntry<>(key, value),
                context + " reversed sequenced entries");
    }

    private static void serialization() throws Exception {
        listEquals(roundTrip(M3PStack.empty()), List.of(), "empty stack serialization");
        collectionEquals(roundTrip(M3PQueue.empty()), List.of(), "empty queue serialization");
        setEquals(roundTrip(M3PSortedSet.<Integer>empty()), Set.of(), "empty sorted-set serialization");
        mapEquals(roundTrip(M3PSortedMap.<Integer, String>empty()), Map.of(), "empty sorted-map serialization");
        mapEquals(roundTrip(M3POrderedMap.empty()), Map.of(), "empty ordered-map serialization");
        setEquals(roundTrip(M3POrderedSet.empty()), Set.of(), "empty ordered-set serialization");
        Object hidden = new Object();
        M3PStack<Object> stackSlice = M3PStack.from(Arrays.asList(hidden, "first", null, "last", hidden)).subList(1, 4);
        listEquals(roundTrip(stackSlice), Arrays.asList("first", null, "last"),
                "stack serialization only visits visible offset range");
        M3PQueue<Object> queue = M3PQueue.from(Arrays.asList(hidden, "visible", null)).minus();
        collectionEquals(roundTrip(queue), Arrays.asList("visible", null), "queue serialization excludes removed head");
        Comparator<Integer> comparator = Comparator.nullsFirst(Comparator.naturalOrder());
        M3PSortedSet<Integer> set = M3PSortedSet.from(comparator, Arrays.asList(null, 3, 1, 2)).descendingSet();
        M3PSortedSet<Integer> copiedSet = roundTrip(set);
        collectionEquals(copiedSet, set, "descending null-friendly set serialization");
        equal(copiedSet.lower(2), set.lower(2), "serialized set comparator orientation");
        require(copiedSet.contains(null), "serialized set null comparator");
        M3PSortedMap<Integer, Object> map = M3PSortedMap.<Integer, Object>empty()
                .plus(0, hidden).plus(1, "one").plus(2, null).plus(3, "three").plus(4, hidden)
                .subMap(1, true, 3, true).descendingMap();
        M3PSortedMap<Integer, Object> copiedMap = roundTrip(map);
        orderedMapEquals(copiedMap, map, "sorted-map serialization excludes out-of-range payload");
        equal(copiedMap.lowerKey(2), map.lowerKey(2), "serialized sorted-map orientation");
        require(copiedMap.comparator() != null, "serialized sorted-map explicit comparator");
        M3PSortedMap<Integer, String> nullKeyMap = M3PSortedMap.<Integer, String>empty(comparator)
                .plus(null, "null-key").plus(1, null).descendingMap();
        orderedMapEquals(roundTrip(nullKeyMap), nullKeyMap, "descending null-key map serialization");
        String shared = new String("payload");
        String distinct = new String("payload");
        M3POrderedMap<Object, String> ordered = M3POrderedMap.<Object, String>empty()
                .plus(hidden, "removed").plus("one", shared).plus("two", distinct).plus("three", shared)
                .plus(null, null).minus(hidden);
        M3POrderedMap<Object, String> copiedOrdered = roundTrip(ordered);
        orderedMapEquals(copiedOrdered, ordered, "ordered-map serialization omits tombstones");
        require(copiedOrdered.get("one") == copiedOrdered.get("three"),
                "serialization preserves shared value identity within graph");
        require(copiedOrdered.get("one") != copiedOrdered.get("two"),
                "serialization preserves distinct equal value objects");
        M3POrderedSet<Object> orderedSet = M3POrderedSet.from(Arrays.asList(hidden, "one", null, "two")).minus(hidden);
        M3POrderedSet<Object> copiedOrderedSet = roundTrip(orderedSet);
        collectionEquals(copiedOrderedSet, orderedSet, "ordered-set serialization excludes removed nonserializable element");
        setEquals(copiedOrderedSet, orderedSet, "ordered-set serialization membership");
        M3PQueue<String> copiedQueue = roundTrip(M3PQueue.from(Arrays.asList(shared, distinct, shared)));
        List<String> copiedQueueValues = new ArrayList<>(copiedQueue);
        require(copiedQueueValues.get(0) == copiedQueueValues.get(2)
                        && copiedQueueValues.get(0) != copiedQueueValues.get(1),
                "queue serialization keeps equal-distinct and repeated references");
        M3PStack<String> copiedStack = roundTrip(M3PStack.from(Arrays.asList(shared, distinct, shared)));
        require(copiedStack.get(0) == copiedStack.get(2) && copiedStack.get(0) != copiedStack.get(1),
                "stack serialization keeps equal-distinct and repeated references");
        unmodifiableList(copiedStack, "payload", "deserialized stack");
        unmodifiableQueue(copiedQueue, "payload");
        unmodifiableSortedSet(copiedSet, 1, "deserialized sorted set");
        unmodifiableSortedMap(copiedMap, 1, "one", "deserialized sorted map");
        unmodifiableMap(copiedOrdered, "one", "payload", "deserialized ordered map");
        unmodifiableCollection(copiedOrderedSet, "one", "deserialized ordered set");
    }

    private static <K, V> void sortedContents(M3PSortedSet<K> actualSet, M3PSortedMap<K, V> actualMap,
                                             NavigableSet<K> expectedSet, NavigableMap<K, V> expectedMap,
                                             String context) {
        setEquals(actualSet, expectedSet, context + " set membership");
        collectionEquals(actualSet, expectedSet, context + " set order");
        orderedMapEquals(actualMap, expectedMap, context + " map");
        require(actualSet.comparator() != null && actualMap.comparator() != null, context + " comparators");
    }

    private static <K, V> void mapEquals(Map<K, V> actual, Map<K, V> expected, String context) {
        equal(actual.size(), expected.size(), context + " size");
        require(actual.equals(expected) && expected.equals(actual), context + " symmetric map equality");
        equal(actual.hashCode(), expected.hashCode(), context + " map hash");
        equal(actual.entrySet(), expected.entrySet(), context + " entry membership");
    }

    private static <K, V> void orderedMapEquals(Map<K, V> actual, Map<K, V> expected, String context) {
        mapEquals(actual, expected, context);
        collectionEquals(actual.keySet(), expected.keySet(), context + " keys");
        collectionEquals(actual.values(), expected.values(), context + " values");
        collectionEquals(actual.entrySet(), expected.entrySet(), context + " entries");
    }

    private static <E> void setEquals(Set<E> actual, Set<E> expected, String context) {
        equal(actual.size(), expected.size(), context + " size");
        require(actual.equals(expected) && expected.equals(actual), context + " symmetric set equality");
        equal(actual.hashCode(), expected.hashCode(), context + " set hash");
    }

    private static <E> void listEquals(List<E> actual, List<E> expected, String context) {
        collectionEquals(actual, expected, context);
        require(actual.equals(expected) && expected.equals(actual), context + " symmetric list equality");
        equal(actual.hashCode(), expected.hashCode(), context + " list hash");
    }

    private static <E> void listEquals(List<E> actual, List<E> expected, int from, int to, String context) {
        listEquals(actual, expected.subList(from, to), context);
    }

    private static void collectionEquals(Collection<?> actual, Collection<?> expected, String context) {
        equal(actual.size(), expected.size(), context + " size");
        equal(new ArrayList<>(actual), new ArrayList<>(expected), context + " contents");
        equal(Arrays.asList(actual.toArray()), new ArrayList<>(expected), context + " array");
        List<Object> split = new ArrayList<>();
        actual.spliterator().forEachRemaining(split::add);
        equal(split, new ArrayList<>(expected), context + " spliterator");
    }

    private static int[] coordinates(int size) {
        int[] result = new int[size + 1];
        for (int index = 0; index <= size; index++) result[index] = index;
        return result;
    }

    private static <E> List<E> drain(Iterator<E> iterator) {
        List<E> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(output)) {
            stream.writeObject(value);
        }
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            return (T) stream.readObject();
        }
    }

    private static void equal(Object actual, Object expected, String context) {
        checks++;
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(context + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void require(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }

    private static void expect(Class<? extends Throwable> type, Throwing action, String context) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(context + ": expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError(context + ": expected " + type.getName());
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }

    private static final class CollisionKey implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final int id;

        CollisionKey(int id) { this.id = id; }
        @Override public int hashCode() {
            return switch (id & 3) {
                case 0 -> 0;
                case 1 -> Integer.MIN_VALUE;
                case 2 -> Integer.MAX_VALUE;
                default -> -1;
            };
        }
        @Override public boolean equals(Object other) {
            return other instanceof CollisionKey key && id == key.id;
        }
        @Override public String toString() { return "key-" + id; }
    }
}
