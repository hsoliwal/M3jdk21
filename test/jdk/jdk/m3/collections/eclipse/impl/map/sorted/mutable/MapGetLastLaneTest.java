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

import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.check;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.orderedLane;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.treeLane;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import jdk.m3.collections.eclipse.api.block.predicate.Predicate2;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.Key;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.Val;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.Lane;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.ProbeOrderedHashMap;
import org.junit.jupiter.api.Test;

/**
 * TreeSortedMap.getLast reads the highest tree slot and OrderedHashMap.getLast the trailing
 * pair: against the verbatim pre-patch valuesView path on the same instance and a java.util
 * model driven in lockstep, equal values by identity, equal getLastOptional outcomes, equal
 * wrapper outcomes, no valuesView call, no key callback and no observable mutation.
 */
//FLLDCIM: LEETCODIFY - random lockstep proof for TreeSortedMap/OrderedHashMap.getLast
class MapGetLastLaneTest {

    private static final int SEEDS = 200;
    private static final int OPS_PER_SEED = 2_000;

    /** One random mutation applied to the owner and its model alike. */
    @FunctionalInterface
    private interface Op {
        void apply(Lane lane, Random random, Key key);
    }

    private static final List<Op> OPS = List.of(MapGetLastLaneTest::put,
            MapGetLastLaneTest::put, MapGetLastLaneTest::put, MapGetLastLaneTest::remove,
            MapGetLastLaneTest::removeKey, MapGetLastLaneTest::clear,
            MapGetLastLaneTest::putAllSorted, MapGetLastLaneTest::putAllHashed,
            MapGetLastLaneTest::keySetRemove, MapGetLastLaneTest::valuesRemove,
            MapGetLastLaneTest::iteratorRemove, MapGetLastLaneTest::removeIf,
            MapGetLastLaneTest::withoutKey, MapGetLastLaneTest::churn);

    @Test
    void treeRandomLockstep() {
        List<Comparator<Key>> orders = Arrays.asList(null, Comparator.reverseOrder(),
                Comparator.nullsFirst(Comparator.naturalOrder()));
        IntStream.range(0, SEEDS).forEach(seed ->
                drive(treeLane(orders.get(seed % 3)), seed, seed % 3 == 2));
    }

    @Test
    void orderedRandomLockstep() {
        IntStream.range(0, SEEDS).forEach(seed -> drive(orderedLane(seed % 3 == 0
                ? new ProbeOrderedHashMap() : new ProbeOrderedHashMap(seed % 3 == 1 ? 0 : 16)),
                seed, true));
    }

    private static void drive(Lane lane, long seed, boolean nulls) {
        Random random = new Random(seed);
        check(lane, "seed " + seed + " start");
        IntStream.range(0, OPS_PER_SEED).forEach(step -> {
            Op op = OPS.get(random.nextInt(OPS.size()));
            op.apply(lane, random, GetLastKeys.key(random, nulls));
            check(lane, "seed " + seed + " step " + step);
        });
    }

    private static void put(Lane lane, Random random, Key key) {
        Val value = GetLastKeys.val(random);
        assertSame(lane.model().put(key, value), lane.map().put(key, value), "put");
    }

    private static void remove(Lane lane, Random random, Key key) {
        assertSame(lane.model().remove(key), lane.map().remove(key), "remove");
    }

    private static void removeKey(Lane lane, Random random, Key key) {
        assertSame(lane.model().remove(key), lane.map().removeKey(key), "removeKey");
    }

    private static void clear(Lane lane, Random random, Key key) {
        if (random.nextInt(8) == 0) {
            lane.map().clear();
            lane.model().clear();
        }
    }

    /** Same-order source; into an empty TreeSortedMap this takes the sorted build path. */
    private static void putAllSorted(Lane lane, Random random, Key key) {
        if (random.nextBoolean()) {
            lane.map().clear();
            lane.model().clear();
        }
        Map<Key, Val> source = lane.source().get();
        IntStream.range(0, random.nextInt(9)).forEach(i ->
                source.put(GetLastKeys.key(random, false), GetLastKeys.val(random)));
        lane.map().putAll(source);
        lane.model().putAll(source);
    }

    private static void putAllHashed(Lane lane, Random random, Key key) {
        Map<Key, Val> source = new HashMap<>();
        IntStream.range(0, random.nextInt(9)).forEach(i ->
                source.put(GetLastKeys.key(random, false), GetLastKeys.val(random)));
        lane.map().putAll(source);
        lane.model().putAll(source);
    }

    private static void keySetRemove(Lane lane, Random random, Key key) {
        assertEquals(lane.model().keySet().remove(key), lane.map().keySet().remove(key));
    }

    private static void valuesRemove(Lane lane, Random random, Key key) {
        Val value = GetLastKeys.val(random);
        assertEquals(lane.model().values().remove(value), lane.map().values().remove(value));
    }

    /** Every step-th element is removed through the key, value or entry iterator. */
    private static void iteratorRemove(Lane lane, Random random, Key key) {
        int which = random.nextInt(3);
        int step = 1 + random.nextInt(4);
        Iterator<?> mine = view(lane.map(), which).iterator();
        Iterator<?> model = view(lane.model(), which).iterator();
        IntStream.range(0, lane.model().size()).forEach(i -> {
            assertEquals(model.next(), mine.next(), "iterator element " + i);
            if (i % step == step - 1) {
                model.remove();
                mine.remove();
            }
        });
        assertEquals(model.hasNext(), mine.hasNext(), "iterator end");
    }

    private static Collection<?> view(Map<?, ?> map, int which) {
        return which == 0 ? map.keySet() : which == 1 ? map.values() : map.entrySet();
    }

    private static void removeIf(Lane lane, Random random, Key key) {
        int modulus = 2 + random.nextInt(5);
        int residue = random.nextInt(modulus);
        Predicate2<Key, Val> predicate = (k, v) ->
                ((k == null ? 0 : k.id) + (v == null ? 1 : v.id())) % modulus == residue;
        boolean expected = lane.model().entrySet()
                .removeIf(entry -> predicate.accept(entry.getKey(), entry.getValue()));
        assertEquals(expected, lane.map().removeIf(predicate), "removeIf");
    }

    private static void withoutKey(Lane lane, Random random, Key key) {
        assertSame(lane.map(), lane.map().withoutKey(key), "withoutKey");
        lane.model().remove(key);
    }

    /** Removes and re-adds one existing key: interior tombstones and compacting rehash. */
    private static void churn(Lane lane, Random random, Key key) {
        if (lane.model().isEmpty()) {
            return;
        }
        List<Key> keys = new ArrayList<>(lane.model().keySet());
        Key interior = keys.get(random.nextInt(keys.size()));
        Val value = lane.model().remove(interior);
        assertSame(value, lane.map().remove(interior), "churn remove");
        lane.model().put(interior, value);
        lane.map().put(interior, value);
    }
}
