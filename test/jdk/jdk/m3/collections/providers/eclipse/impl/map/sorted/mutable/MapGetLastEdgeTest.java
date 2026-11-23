/* *****************************************************************************
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
package jdk.m3.collections.eclipse.impl.map.sorted.mutable;

import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.assertLastMatches;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.check;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.compareToReference;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.modelLast;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.orderedLane;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.run;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.treeLane;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.IntStream;

import jdk.m3.collections.eclipse.api.map.sorted.MutableSortedMap;
import jdk.m3.collections.eclipse.impl.map.ordered.mutable.OrderedHashMap;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.Key;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.Val;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.Lane;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.ProbeOrderedHashMap;
import org.junit.jupiter.api.Test;

/**
 * Deterministic edges of TreeSortedMap.getLast and OrderedHashMap.getLast against the
 * verbatim pre-patch path: empty shapes, null values, trailing removals, deep spines, copies,
 * Externalizable round trips, unchanged sub-map and immutable paths, zero allocation.
 */
//FLLDCIM: LEETCODIFY - deterministic edge proof for TreeSortedMap/OrderedHashMap.getLast
class MapGetLastEdgeTest {

    private static final int GROWTH = 10_000;
    private static final int SHAPE = 4_096;
    private static final int CALLS = 10_000;

    @Test
    void orderedEmptyShapes() {
        List.of(new ProbeOrderedHashMap(), new ProbeOrderedHashMap(0)).forEach(map -> {
            Lane lane = orderedLane(map);
            check(lane, "fresh");
            putBoth(lane, new Key(7), new Val(1));
            check(lane, "one entry");
            assertSame(lane.model().remove(new Key(7)), map.remove(new Key(7)));
            check(lane, "emptied");
        });
    }

    @Test
    void treeEmptyShapes() {
        Lane lane = treeLane(null);
        check(lane, "fresh");
        putBoth(lane, new Key(7), new Val(1));
        check(lane, "one entry");
        lane.map().clear();
        lane.model().clear();
        check(lane, "cleared");
    }

    @Test
    void nullLastValueOptionalThrows() {
        List.of(treeLane(null), orderedLane(new ProbeOrderedHashMap())).forEach(lane -> {
            putBoth(lane, new Key(3), new Val(3));
            putBoth(lane, new Key(9), null);
            check(lane, "null last value");
            assertEquals(NullPointerException.class,
                    run(lane.ordered()::getLastOptional).thrown());
            assertNull(run(lane.ordered()::getLastOptional).message());
        });
    }

    @Test
    void orderedTrailingRemovals() {
        Lane lane = orderedLane(new ProbeOrderedHashMap());
        IntStream.range(1, 6).forEach(id -> putBoth(lane, new Key(id), new Val(id)));
        IntStream.of(2, 5, 4, 3).forEach(id -> {
            assertSame(lane.model().remove(new Key(id)), lane.map().remove(new Key(id)));
            check(lane, "removed " + id);
        });
        putBoth(lane, new Key(2), new Val(20));
        check(lane, "re-added interior key");
    }

    @Test
    void ascendingGrowthBothOwners() {
        List.of(treeLane(null), orderedLane(new ProbeOrderedHashMap())).forEach(lane -> {
            compareToReference(lane, "empty");
            IntStream.range(0, GROWTH).forEach(id -> {
                putBoth(lane, new Key(id), new Val(id));
                Object last = compareToReference(lane, "insert " + id).value();
                assertSame(modelLast(lane.model()), last, "model at " + id);
            });
        });
    }

    @Test
    void treeDescendingAndShuffledShapes() {
        List<Integer> shuffled = new ArrayList<>(IntStream.range(0, SHAPE).boxed().toList());
        Collections.shuffle(shuffled, new Random(SHAPE));
        List<Integer> descending = IntStream.range(0, SHAPE).map(i -> SHAPE - i).boxed().toList();
        List.of(shuffled, descending).forEach(ids -> {
            Lane lane = treeLane(Comparator.reverseOrder());
            Lane natural = treeLane(null);
            ids.forEach(id -> List.of(lane, natural).forEach(each -> {
                putBoth(each, new Key(id), new Val(id));
                Object last = compareToReference(each, "insert " + id).value();
                assertSame(modelLast(each.model()), last, "model at " + id);
            }));
        });
    }

    @Test
    void treeCopiesAndRoundTrip() throws IOException, ClassNotFoundException {
        Comparator<Key> order = Comparator.nullsFirst(Comparator.naturalOrder());
        TreeMap<Key, Val> source = new TreeMap<>(order);
        IntStream.range(0, 50).forEach(id -> source.put(new Key(id * 3), new Val(id)));
        HashMap<Key, Val> hashed = new HashMap<>(source);
        source.put(null, new Val(-1));
        List<TreeSortedMap<Key, Val>> copies = new ArrayList<>(List.of(
                new TreeSortedMap<>(source), TreeSortedMap.newMap(hashed),
                TreeSortedMap.newMap(source)));
        copies.add(copies.get(0).clone());
        copies.add(roundTrip(copies.get(0)));
        copies.forEach(copy -> assertLastMatches(copy, copy, "copy"));
        assertEquals(source.lastEntry().getValue(), roundTrip(copies.get(1)).getLast());
    }

    @SuppressWarnings("unchecked")
    private static TreeSortedMap<Key, Val> roundTrip(TreeSortedMap<Key, Val> map)
            throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(map);
        }
        try (ObjectInputStream in =
                new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (TreeSortedMap<Key, Val>) in.readObject();
        }
    }

    @Test
    void treeSubViewsKeepOldPath() {
        TreeSortedMap<Key, Val> map = new TreeSortedMap<>();
        TreeMap<Key, Val> model = new TreeMap<>();
        IntStream.range(0, 40).forEach(id -> {
            map.put(new Key(id * 2), new Val(id));
            model.put(new Key(id * 2), map.get(new Key(id * 2)));
        });
        IntStream.range(-1, 82).forEach(id -> {
            Key low = new Key(id);
            Key high = new Key(id + 9);
            assertView(map.headMap(low), model.headMap(low, false), "head " + id);
            assertView(map.tailMap(low), model.tailMap(low, true), "tail " + id);
            assertView(map.subMap(low, high), model.subMap(low, true, high, false), "sub " + id);
        });
    }

    private static void assertView(MutableSortedMap<Key, Val> view,
            NavigableMap<Key, Val> model, String at) {
        assertLastMatches(view, view, at);
        assertSame(modelLast(model), view.getLast(), at + " model");
    }

    @Test
    void immutableCopiesKeepOldPath() {
        List.of(treeLane(null), orderedLane(new ProbeOrderedHashMap())).forEach(lane -> {
            IntStream.range(0, 30).forEach(id -> putBoth(lane, new Key(id % 7 * 5), new Val(id)));
            assertSame(modelLast(lane.model()),
                    GetLastOracle.prePatchGetLast(lane.map().toImmutable()), "immutable");
            assertLastMatches(lane.map().toImmutable(), lane.ordered(), "immutable reference");
        });
        TreeSortedMap<Key, Val> tree = new TreeSortedMap<>();
        tree.put(new Key(1), new Val(1));
        assertLastMatches(tree.toImmutable(), tree.toImmutable(), "immutable tree");
    }

    @Test
    void orderedSelfValueNullKey() {
        OrderedHashMap<Object, Object> map = new OrderedHashMap<>();
        map.put("a", "b");
        map.put(null, map);
        assertSame(map, map.getLast(), "value is the map itself");
        assertLastMatches(map, map, "self value");
        map.put("c", null);
        map.removeKey(null);
        assertLastMatches(map, map, "null key removed");
    }

    @Test
    void getLastAllocatesNothing() {
        Lane tree = treeLane(null);
        Lane ordered = orderedLane(new ProbeOrderedHashMap());
        IntStream.range(0, 500).forEach(id -> List.of(tree, ordered)
                .forEach(lane -> putBoth(lane, new Key(id), new Val(id))));
        allocated(() -> { });
        long baseline = allocated(() -> { });
        assertEquals(baseline, allocated(tree.ordered()::getLast), "tree getLast bytes");
        assertEquals(baseline, allocated(ordered.ordered()::getLast), "ordered getLast bytes");
    }

    /** Thread-allocated bytes over CALLS warmed calls (com.sun.management, by reflection). */
    private static long allocated(Runnable action) {
        IntStream.range(0, CALLS).forEach(i -> action.run());
        long before = AllocationMeter.bytes();
        IntStream.range(0, CALLS).forEach(i -> action.run());
        return AllocationMeter.bytes() - before;
    }

    private static void putBoth(Lane lane, Key key, Val value) {
        assertSame(lane.model().put(key, value), lane.map().put(key, value), "put " + key);
    }

    /** Reads com.sun.management.ThreadMXBean.getCurrentThreadAllocatedBytes reflectively. */
    private static final class AllocationMeter {
        private static final Object BEAN;
        private static final Method READ;

        static {
            try {
                BEAN = Class.forName("java.lang.management.ManagementFactory")
                        .getMethod("getThreadMXBean").invoke(null);
                READ = Class.forName("com.sun.management.ThreadMXBean")
                        .getMethod("getCurrentThreadAllocatedBytes");
            } catch (ReflectiveOperationException failure) {
                throw new ExceptionInInitializerError(failure);
            }
        }

        static long bytes() {
            try {
                return (long) READ.invoke(BEAN);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(failure);
            }
        }
    }
}
