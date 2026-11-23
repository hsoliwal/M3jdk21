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

import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.assertSameOutcome;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.check;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.compareToReference;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.orderedLane;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.prePatchGetLast;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.run;
import static jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.treeLane;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.IntStream;

import jdk.m3.collections.eclipse.api.block.predicate.Predicate2;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.Key;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.TrapOrder;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastKeys.Val;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.Lane;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.Outcome;
import jdk.m3.collections.eclipse.impl.map.sorted.mutable.GetLastOracle.ProbeOrderedHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Throwing and midway states of TreeSortedMap.getLast and OrderedHashMap.getLast: aborted
 * removeIf and put, reentrant getLast from key callbacks, the ghost size after a throwing
 * sorted putAll source and the partial rehash, all equal to the verbatim pre-patch path.
 */
//FLLDCIM: LEETCODIFY - fault-state proof for TreeSortedMap/OrderedHashMap.getLast
class MapGetLastFaultTest {

    private static final String AIOOBE_20 = "Index 20 out of bounds for length 20";

    @AfterEach
    void clearHook() {
        GetLastKeys.hook = null;
    }

    @Test
    void removeIfThrowingPredicate() {
        IntStream.rangeClosed(1, 33).forEach(limit -> List.of(treeLane(null),
                orderedLane(new ProbeOrderedHashMap())).forEach(lane -> {
            IntStream.range(0, 32).forEach(id -> putBoth(lane, new Key(id), new Val(id)));
            int[] calls = new int[2];
            Predicate2<Key, Val> mine = (k, v) -> trap(calls, 0, limit, k);
            Predicate2<Key, Val> model = (k, v) -> trap(calls, 1, limit, k);
            Outcome expected = run(() -> lane.model().entrySet()
                    .removeIf(entry -> model.accept(entry.getKey(), entry.getValue())));
            assertSameOutcome(expected, run(() -> lane.map().removeIf(mine)), "removeIf");
            assertEquals(calls[1], calls[0], "predicate calls at limit " + limit);
            check(lane, "after removeIf limit " + limit);
        }));
    }

    private static boolean trap(int[] calls, int lane, int limit, Key key) {
        if (++calls[lane] == limit) {
            throw new IllegalStateException("predicate trap " + key);
        }
        return key.id % 3 != 1;
    }

    @Test
    void throwingComparatorKeepsState() {
        IntStream.rangeClosed(1, 3).forEach(nth -> {
            TrapOrder order = new TrapOrder();
            Lane lane = treeLane(order);
            IntStream.range(0, 32).forEach(id -> putBoth(lane, new Key(id * 2), new Val(id)));
            order.countdown = nth;
            Key key = new Key(nth % 2 == 0 ? 99 : 31);
            assertThrows(IllegalStateException.class, () -> lane.map().put(key, new Val(-1)));
            check(lane, "comparator threw at " + nth);
        });
    }

    @Test
    void throwingHashEqualsKeepsState() {
        Lane lane = orderedLane(new ProbeOrderedHashMap());
        List<Key> stored = IntStream.range(0, 9).mapToObj(Key::new).toList();
        stored.forEach(key -> putBoth(lane, key, new Val(key.id)));
        Key hashing = new Key(70);
        hashing.mode = GetLastKeys.MODE_HASH;
        assertThrows(IllegalStateException.class, () -> lane.map().put(hashing, new Val(1)));
        check(lane, "hashCode threw");
        stored.get(5).mode = GetLastKeys.MODE_EQUALS;
        Key colliding = new Key(71, 5);
        assertThrows(IllegalStateException.class, () -> lane.map().put(colliding, new Val(2)));
        stored.get(5).mode = GetLastKeys.MODE_NORMAL;
        check(lane, "equals threw");
    }

    @Test
    void reentrantGetLastInComparator() {
        TrapOrder order = new TrapOrder();
        Lane lane = treeLane(order);
        IntStream.range(0, 20).forEach(id -> putBoth(lane, new Key(id), new Val(id)));
        List<Outcome[]> seen = new ArrayList<>();
        order.onCompare = () -> seen.add(observePair(lane));
        lane.map().put(new Key(50), new Val(50));
        lane.map().put(new Key(7), new Val(70));
        order.onCompare = null;
        assertFalse(seen.isEmpty(), "comparator ran");
        seen.forEach(pair -> assertSameOutcome(pair[1], pair[0], "inside compare"));
    }

    @Test
    void reentrantGetLastInEquals() {
        Lane lane = orderedLane(new ProbeOrderedHashMap());
        List<Key> stored = IntStream.range(0, 9).mapToObj(Key::new).toList();
        stored.forEach(key -> putBoth(lane, key, new Val(key.id)));
        List<Outcome[]> seen = new ArrayList<>();
        GetLastKeys.hook = () -> seen.add(observePair(lane));
        stored.get(4).mode = GetLastKeys.MODE_HOOK;
        lane.map().put(new Key(40, 4), new Val(40));
        lane.map().removeKey(new Key(41, 4));
        stored.get(4).mode = GetLastKeys.MODE_NORMAL;
        assertFalse(seen.isEmpty(), "equals ran");
        seen.forEach(pair -> assertSameOutcome(pair[1], pair[0], "inside equals"));
    }

    /** k2.hashCode runs getLast while rehash has moved only k0 and k1 (occupied 0). */
    @Test
    void reentrantGetLastInRehash() {
        Lane lane = orderedLane(new ProbeOrderedHashMap());
        List<Key> stored = IntStream.range(0, 5).mapToObj(Key::new).toList();
        stored.forEach(key -> putBoth(lane, key, new Val(key.id)));
        List<Outcome[]> seen = new ArrayList<>();
        GetLastKeys.hook = () -> seen.add(observePair(lane));
        stored.get(2).mode = GetLastKeys.MODE_HOOK;
        Val five = new Val(5);
        lane.map().put(new Key(5), five);
        stored.get(2).mode = GetLastKeys.MODE_NORMAL;
        seen.forEach(pair -> assertSameOutcome(pair[1], pair[0], "inside rehash"));
        assertTrue(seen.stream().anyMatch(pair -> pair[1].thrown() != null), "mid-rehash seen");
        lane.model().put(new Key(5), five);
        check(lane, "after rehash");
    }

    /** Patched getLast first, then the pre-patch path, at the same state. */
    private static Outcome[] observePair(Lane lane) {
        Outcome patched = run(lane.ordered()::getLast);
        return new Outcome[] {patched, run(() -> prePatchGetLast(lane.map()))};
    }

    /** A sorted putAll source that throws on its second entry leaves size 3 and no root. */
    @Test
    void treePutAllGhostSize() {
        Lane lane = treeLane(null);
        assertThrows(IllegalStateException.class, () -> lane.map().putAll(new FailingSource()));
        Outcome last = compareToReference(lane, "ghost size");
        assertNull(last.value(), "ghost getLast");
        Outcome optional = run(lane.ordered()::getLastOptional);
        assertEquals(NullPointerException.class, optional.thrown(), "ghost getLastOptional");
        assertFalse(lane.map().isEmpty(), "ghost size is not empty");
        assertEquals(3, lane.map().size(), "ghost size");
    }

    /** k2.hashCode throws inside rehash: size 5, occupied 0, a length-20 pair lane. */
    @Test
    void orderedPartialRehash() {
        Lane lane = orderedLane(new ProbeOrderedHashMap());
        List<Key> stored = IntStream.range(0, 5).mapToObj(Key::new).toList();
        stored.forEach(key -> putBoth(lane, key, new Val(key.id)));
        stored.get(2).mode = GetLastKeys.MODE_HASH;
        assertThrows(IllegalStateException.class, () -> lane.map().put(new Key(5), new Val(5)));
        Outcome broken = compareToReference(lane, "partial rehash", false);
        assertEquals(ArrayIndexOutOfBoundsException.class, broken.thrown(), "partial rehash");
        assertEquals(AIOOBE_20, broken.message(), "partial rehash message");
        lane.map().put(new Key(6), new Val(6));
        Outcome after = compareToReference(lane, "put after partial rehash", false);
        assertEquals(ArrayIndexOutOfBoundsException.class, after.thrown(), "after put");
        assertEquals(AIOOBE_20, after.message(), "after put message");
        assertEquals(6, lane.map().size(), "ghost size after put");
    }

    private static void putBoth(Lane lane, Key key, Val value) {
        assertSame(lane.model().put(key, value), lane.map().put(key, value), "put " + key);
    }

    /** Natural-order SortedMap of size 3 whose entry iterator fails on its second next(). */
    private static final class FailingSource extends AbstractMap<Key, Val>
            implements SortedMap<Key, Val> {
        @Override public Comparator<? super Key> comparator() { return null; }
        @Override public SortedMap<Key, Val> subMap(Key low, Key high) { return this; }
        @Override public SortedMap<Key, Val> headMap(Key high) { return this; }
        @Override public SortedMap<Key, Val> tailMap(Key low) { return this; }
        @Override public Key firstKey() { return new Key(1); }
        @Override public Key lastKey() { return new Key(3); }
        @Override public int size() { return 3; }

        @Override
        public Set<Entry<Key, Val>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return 3; }
                @Override public Iterator<Entry<Key, Val>> iterator() { return failing(); }
            };
        }

        private static Iterator<Entry<Key, Val>> failing() {
            Iterator<Integer> ids = IntStream.rangeClosed(1, 3).iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { return ids.hasNext(); }
                @Override public Entry<Key, Val> next() {
                    int id = ids.next();
                    if (id == 2) {
                        throw new IllegalStateException("source trap");
                    }
                    return new SimpleImmutableEntry<>(new Key(id), new Val(id));
                }
            };
        }
    }
}
