// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/** Deterministic behavioral/density gate; domains are explicit test inputs, not runtime stubs. */
public final class MIndexArraySelfTest {
    private static long checks;
    private static final IntSpace SPACE = new IntSpace();
    private MIndexArraySelfTest() { }

    /** No ID operation may box, admit or project a payload through this instrumented domain. */
    private static final class IntSpace implements MIndexSpace<Integer> {
        private long projections;
        private long admissions;
        @Override public int id(Integer value) { admissions++; requireId(value); return value; }
        @Override public int findId(Integer value) { return value != null && containsId(value) ? value : -1; }
        @Override public Integer value(int id) { requireId(id); projections++; return id; }
        @Override public int size() { return 100_000; }
        @Override public boolean javaEqualityCompatible() { return true; }
    }

    public static void main(String[] args) throws Exception {
        list(); deque(); sets(); maps(); multiMap(); biMap(); priority(); table(); graph();
        check(SPACE.projections == 0 && SPACE.admissions == 0, "native paths project/admit no payload");
        javaViews(); density();
        System.out.println("PASS MIndexArraySelfTest checks=" + checks);
    }

    private static void list() throws Exception {
        MIndexList<Integer> actual = new MIndexList<>(SPACE, 0);
        List<Integer> expected = new ArrayList<>();
        Random r = new Random(31);
        MIndexCompositeIndex store = new MIndexCompositeIndex();
        for (int turn = 0; turn < 12_000; turn++) {
            int n = r.nextInt(9), position = r.nextInt(expected.size() + 1);
            int[] lane = randomIds(r, n + 2, 80);
            if (turn % 3 != 0) {
                actual.insertAllIds(position, lane, 1, n);
                for (int i = 0; i < n; i++) expected.add(position + i, lane[i + 1]);
                Arrays.fill(lane, -1); // caller array is never retained
            } else {
                int end = position + r.nextInt(expected.size() - position + 1);
                actual.removeRange(position, end);
                expected.subList(position, end).clear();
            }
            equal(actual.snapshotIds(), ints(expected), "list order");
            int sought = r.nextInt(100);
            check(actual.indexOfId(sought) == expected.indexOf(sought), "first index");
            check(actual.lastIndexOfId(sought) == expected.lastIndexOf(sought), "last index");
            if (turn % 101 == 0) {
                int identity = actual.canonicalId(store);
                MIndexFrozenList<Integer> frozen = actual.freeze(store);
                actual.compact();
                check(bytes(actual) == 4L * actual.size(), "list trims backing");
                check(actual.canonicalId(store) == identity, "list canonical preservation");
                equal(frozen.copyIds(), ints(expected), "frozen list stays detached");
            }
        }
        int[] before = actual.snapshotIds();
        expect(IndexOutOfBoundsException.class, () -> actual.addAllIds(new int[]{2, 100_000}, 0, 2));
        expect(IndexOutOfBoundsException.class, () -> actual.removeRange(-1, 0));
        equal(before, actual.snapshotIds(), "list validation before mutation");
        actual.clear(); actual.compact(); actual.addAllIds(new int[]{3, 3, 5}, 0, 3);
        equal(actual.snapshotIds(), new int[]{3, 3, 5}, "list regrows");
        check(actual.indexOfId(-1) == -1 && actual.lastIndexOfId(-1) == -1, "negative ID absent");
    }

    private static void deque() throws Exception {
        MIndexDeque<Integer> actual = new MIndexDeque<>(SPACE, 0);
        ArrayDeque<Integer> expected = new ArrayDeque<>();
        Random r = new Random(37);
        for (int turn = 0; turn < 14_000; turn++) {
            int n = r.nextInt(50);
            int[] lane = randomIds(r, n + 2, 100);
            switch (r.nextInt(5)) {
                case 0 -> {
                    actual.addAllFirstIds(lane, 1, n);
                    for (int i = n; i >= 1; i--) expected.addFirst(lane[i]);
                }
                case 1 -> {
                    actual.addAllLastIds(lane, 1, n);
                    for (int i = 1; i <= n; i++) expected.addLast(lane[i]);
                }
                case 2 -> {
                    int[] out = new int[n + 2]; Arrays.fill(out, -7);
                    int count = actual.drainFirstIds(out, 1, n);
                    check(count == Math.min(n, expected.size()), "drain count");
                    for (int i = 0; i < count; i++) check(out[i + 1] == expected.removeFirst(), "drain order");
                    check(out[0] == -7 && out[count + 1] == -7, "drain slice boundary");
                }
                case 3 -> {
                    for (int i = 0; i < n && !expected.isEmpty(); i++) {
                        check(actual.removeLastId() == expected.removeLast(), "remove last");
                    }
                }
                default -> {
                    long before = bytes(actual); actual.compact();
                    check(bytes(actual) <= before, "deque never grows during compact");
                }
            }
            Arrays.fill(lane, -1);
            equal(actual.snapshotIds(), ints(new ArrayList<>(expected)), "ring order/wrap");
        }
        int[] before = actual.snapshotIds();
        expect(IndexOutOfBoundsException.class, () -> actual.addAllFirstIds(new int[]{1, -1}, 0, 2));
        expect(IndexOutOfBoundsException.class, () -> actual.drainFirstIds(new int[2], 1, 2));
        equal(before, actual.snapshotIds(), "deque invalid input unchanged");
        actual.clear(); actual.compact();
        check(bytes(actual) == 32, "empty ring capacity");
        int[] large = randomIds(r, 6000, 100);
        actual.addAllLastIds(large, 0, large.length);
        equal(actual.snapshotIds(), large, "bulk ring grows beyond one doubling");
    }

    private static void sets() throws Exception {
        for (boolean dense : new boolean[]{false, true}) {
            MIndexSet<Integer> actual = dense ? MIndexSet.dense(SPACE, 128) : new MIndexSet<>(SPACE);
            Set<Integer> expected = new HashSet<>();
            Random r = new Random(43);
            for (int turn = 0; turn < 12_000; turn++) {
                int[] lane = randomIds(r, r.nextInt(20), 200);
                int changed = 0;
                if ((turn & 1) == 0) {
                    for (int v : lane) if (expected.add(v)) changed++;
                    check(actual.addAllIds(lane, 0, lane.length) == changed, "set bulk add count");
                    check(actual.containsAllIds(lane, 0, lane.length), "set contains all");
                } else {
                    for (int v : lane) if (expected.remove(v)) changed++;
                    check(actual.removeAllIds(lane, 0, lane.length) == changed, "set bulk remove count");
                }
                equal(actual.snapshotIdsSorted(), expected.stream().mapToInt(Integer::intValue).sorted().toArray(), "set content");
                if (turn % 109 == 0) actual.compact();
            }
            int[] before = actual.snapshotIdsSorted();
            expect(IndexOutOfBoundsException.class, () -> actual.addAllIds(new int[]{300, -1}, 0, 2));
            equal(before, actual.snapshotIdsSorted(), "set invalid tail atomic");
            check(!actual.containsAllIds(new int[]{-1}, 0, 1), "invalid query absent");
        }
    }

    private static void maps() throws Exception {
        for (boolean dense : new boolean[]{false, true}) {
            MIndexMap<Integer, Integer> actual = dense ? MIndexMap.dense(SPACE, SPACE, 128) : new MIndexMap<>(SPACE, SPACE);
            Map<Integer, Integer> expected = new HashMap<>();
            Random r = new Random(47);
            MIndexCompositeIndex store = new MIndexCompositeIndex();
            for (int turn = 0; turn < 14_000; turn++) {
                int k = r.nextInt(200), v = r.nextInt(200), old = r.nextInt(200);
                switch (r.nextInt(4)) {
                    case 0 -> {
                        Integer previous = expected.putIfAbsent(k, v);
                        check(actual.putIfAbsentIds(k, v) == (previous == null ? -1 : previous), "put if absent");
                    }
                    case 1 -> check(actual.replaceIds(k, old, v) == expected.replace(k, old, v), "conditional replace");
                    case 2 -> check(actual.removeIds(k, old) == expected.remove(k, old), "conditional remove");
                    default -> {
                        int[] keys = randomIds(r, 8, 200), values = randomIds(r, 8, 200);
                        int previous = expected.size();
                        for (int i = 1; i < 7; i++) expected.put(keys[i], values[i]);
                        check(actual.putAllIds(keys, 1, values, 1, 6) == expected.size() - previous, "map bulk count");
                    }
                }
                check(actual.size() == expected.size(), "map size");
                check(actual.valueIdOrDefault(k, -1) == expected.getOrDefault(k, -1), "map lookup");
                check(actual.containsValueId(v) == expected.containsValue(v), "map value scan");
                if (turn % 113 == 0) {
                    int[] before = actual.snapshotSortedLane();
                    int identity = actual.canonicalId(store); actual.compact();
                    equal(before, actual.snapshotSortedLane(), "map compaction contents");
                    check(identity == actual.canonicalId(store), "map canonical stable");
                }
            }
            int[] before = actual.snapshotSortedLane();
            expect(IndexOutOfBoundsException.class, () -> actual.putAllIds(new int[]{999, 998}, 0, new int[]{1, -1}, 0, 2));
            equal(before, actual.snapshotSortedLane(), "map invalid slice atomic");
            check(!actual.removeIds(-1, -1) && !actual.replaceIds(-1, -1, 0), "absent sentinels never match");
            check(!actual.containsValueId(-1), "negative value absent");
        }
    }

    private static void multiMap() throws Exception {
        MIndexMultiMap<Integer, Integer> actual = new MIndexMultiMap<>(SPACE, SPACE, 4096);
        Map<Integer, List<Integer>> expected = new HashMap<>();
        Random r = new Random(53);
        MIndexCompositeIndex store = new MIndexCompositeIndex();
        for (int turn = 0; turn < 15_000; turn++) {
            int k = r.nextInt(40), v = r.nextInt(40);
            switch (r.nextInt(5)) {
                case 0, 1 -> {
                    int[] lane = randomIds(r, 6, 40);
                    List<Integer> list = expected.computeIfAbsent(k, ignored -> new ArrayList<>());
                    int count = 0;
                    for (int x : lane) if (!list.contains(x)) { list.add(0, x); count++; }
                    check(actual.addAllIds(k, lane, 0, lane.length) == count, "unique multimap bulk");
                }
                case 2 -> {
                    List<Integer> list = expected.get(k);
                    boolean removed = list != null && list.remove(Integer.valueOf(v));
                    if (list != null && list.isEmpty()) expected.remove(k);
                    check(actual.removeIds(k, v) == removed, "multimap remove");
                }
                case 3 -> {
                    List<Integer> list = expected.remove(k);
                    check(actual.removeAllId(k) == (list == null ? 0 : list.size()), "multimap remove key");
                }
                default -> {
                    int[] lane = actual.snapshotSortedLane(); int id = actual.canonicalId(store);
                    long before = bytes(actual); actual.compact();
                    check(bytes(actual) <= before, "multimap shrinks");
                    equal(lane, actual.snapshotSortedLane(), "multimap canonical pairs stable");
                    check(id == actual.canonicalId(store), "multimap canonical stable");
                }
            }
            int size = 0;
            for (int key = 0; key < 40; key++) {
                List<Integer> list = expected.getOrDefault(key, List.of()); size += list.size();
                equal(actual.snapshotValueIds(key), ints(list), "multimap chain order/free slot reuse");
                check(actual.valueCountId(key) == list.size(), "multimap key count");
            }
            check(actual.size() == size && actual.keyCount() == expected.size(), "multimap sizes");
            check(actual.containsIds(k, v) == expected.getOrDefault(k, List.of()).contains(v), "multimap contains IDs");
            int[] seen = {0};
            actual.forEachId((key, value) -> {
                check(expected.getOrDefault(key, List.of()).contains(value), "direct pair traversal"); seen[0]++;
            });
            check(seen[0] == size, "direct traversal cardinality");
        }
        int[] before = actual.snapshotSortedLane();
        expect(IndexOutOfBoundsException.class, () -> actual.addAllIds(0, new int[]{999, -1}, 0, 2));
        equal(before, actual.snapshotSortedLane(), "multimap invalid tail atomic");
        actual.clear(); actual.compact();
        check(actual.isEmpty() && actual.keyCount() == 0 && bytes(actual) == 128, "multimap clears/releases");
        actual.addAllIds(2, new int[]{3, 4, 3, 5}, 0, 4);
        equal(actual.snapshotValueIds(2), new int[]{5, 4, 3}, "multimap regrows, duplicates retain position");
    }

    private static void biMap() throws Exception {
        IntSpace values = new IntSpace();
        MIndexBiMap<Integer, Integer> actual = new MIndexCollectionFactory().biMap(
                SPACE, values, MIndexCollectionFactory.Hints.balanced(0));
        MIndexBiMap<Integer, Integer> inverse = actual.inverse();
        check(inverse.keySpace() == values && inverse.valueSpace() == SPACE, "inverse domain authority");
        Map<Integer, Integer> forward = new HashMap<>(), reverse = new HashMap<>();
        Random r = new Random(71);
        for (int turn = 0; turn < 14_000; turn++) {
            int key = r.nextInt(80), value = r.nextInt(80);
            boolean swapped = r.nextBoolean();
            MIndexBiMap<Integer, Integer> view = swapped ? inverse : actual;
            Map<Integer, Integer> f = swapped ? reverse : forward, b = swapped ? forward : reverse;
            switch (r.nextInt(5)) {
                case 0, 1 -> {
                    boolean force = (turn & 1) == 0;
                    Integer previous = f.get(key), owner = b.get(value);
                    if (owner != null && owner != key && !force) {
                        int[] before = actual.snapshotSortedLane();
                        expect(IllegalArgumentException.class, () -> view.putIds(key, value));
                        equal(before, actual.snapshotSortedLane(), "bimap conflict unchanged");
                    } else {
                        if (owner != null) f.remove(owner);
                        if (previous != null) b.remove(previous);
                        f.put(key, value); b.put(value, key);
                        boolean inserted = force ? view.forcePutIds(key, value) : view.putIds(key, value);
                        check(inserted == (previous == null), "bimap inserted key result");
                    }
                }
                case 2 -> {
                    Integer previous = f.remove(key); if (previous != null) b.remove(previous);
                    check(view.removeKeyId(key) == (previous != null), "inverse remove shared");
                }
                case 3 -> {
                    Integer previous = f.get(key);
                    boolean removed = previous != null && previous == value;
                    if (removed) { f.remove(key); b.remove(value); }
                    check(view.removeIds(key, value) == removed, "bimap conditional remove");
                }
                default -> { actual.compact(); inverse.compact(); }
            }
            check(actual.size() == forward.size() && inverse.size() == reverse.size(), "bimap cardinalities");
            for (int id = 0; id < 80; id++) {
                check(actual.valueIdOrDefault(id, -1) == forward.getOrDefault(id, -1), "forward relation");
                check(actual.keyIdOrDefault(id, -1) == reverse.getOrDefault(id, -1), "reverse relation");
                check(inverse.valueIdOrDefault(id, -1) == reverse.getOrDefault(id, -1), "live inverse relation");
            }
        }
        inverse.clear(); check(actual.isEmpty(), "inverse clear shared");
        actual.putIds(1, 2); actual.putIds(3, 4);
        check(!actual.replaceIds(-1, -1, 0), "bimap sentinel mismatch");
        expect(IllegalArgumentException.class, () -> actual.replaceIds(1, 2, 4));
        check(actual.replaceIds(1, 2, 5) && inverse.valueIdOrDefault(5, -1) == 1, "bimap conditional replace");
        MIndexFrozenMap<Integer, Integer> snapshot = actual.freeze(new MIndexCompositeIndex());
        actual.forcePutIds(3, 5);
        check(actual.size() == 1 && !actual.containsKeyId(1), "force evicts conflicting key");
        equal(snapshot.copyLane(), new int[]{1, 5, 3, 4}, "bimap freeze detached");
        int[] before = actual.snapshotSortedLane();
        expect(IndexOutOfBoundsException.class, () -> inverse.forcePutIds(-1, 1));
        equal(before, actual.snapshotSortedLane(), "bimap invalid IDs unchanged");
        check(values.admissions == 0 && values.projections == 0, "reverse domain payload untouched");
    }

    private record Item(int id, long priority, long serial) { }
    private static void priority() throws Exception {
        MIndexPriorityQueue<Integer> actual = new MIndexPriorityQueue<>(SPACE, 0);
        PriorityQueue<Item> expected = new PriorityQueue<>((a, b) -> {
            int c = Long.compare(a.priority, b.priority); return c != 0 ? c : Long.compare(a.serial, b.serial);
        });
        Random r = new Random(59); long serial = 0;
        for (int turn = 0; turn < 8000; turn++) {
            if (turn % 3 == 0) {
                int n = r.nextInt(30); int[] lane = randomIds(r, n + 2, 100);
                long[] priorities = new long[n + 2];
                for (int i = 1; i <= n; i++) {
                    priorities[i] = r.nextInt(7) - 3;
                    expected.add(new Item(lane[i], priorities[i], serial++));
                }
                actual.addAllIds(lane, 1, priorities, 1, n);
            } else {
                int n = r.nextInt(20);
                for (int i = 0; i < n && !expected.isEmpty(); i++) {
                    Item first = expected.remove();
                    check(actual.firstPriority() == first.priority, "priority order");
                    check(actual.removeFirstId() == first.id, "stable priority tie");
                }
            }
            check(actual.size() == expected.size(), "heap size");
            if (turn % 31 == 0) {
                int[] lane = actual.snapshotPriorityLane(); actual.compact();
                equal(lane, actual.snapshotPriorityLane(), "heap compaction priorities and serials");
                check(bytes(actual) == 20L * actual.size(), "heap all lanes trim");
            }
        }
        int[] before = actual.snapshotPriorityLane();
        expect(IndexOutOfBoundsException.class, () -> actual.addAllIds(new int[]{1, -1}, 0, new long[]{1, 2}, 0, 2));
        expect(IndexOutOfBoundsException.class, () -> actual.addAllIds(new int[]{1, 2}, 0, new long[]{1}, 0, 2));
        equal(before, actual.snapshotPriorityLane(), "heap prevalidates all slices");
        Field nextSerial = MIndexPriorityQueue.class.getDeclaredField("nextSerial"); nextSerial.setAccessible(true);
        nextSerial.setLong(actual, Long.MAX_VALUE - 1);
        expect(IllegalStateException.class, () -> actual.addAllIds(new int[]{2, 3}, 0, new long[]{0, 0}, 0, 2));
        equal(before, actual.snapshotPriorityLane(), "serial overflow atomic");
        actual.addAllIds(new int[]{4}, 0, new long[]{0}, 0, 1);
        expect(IllegalStateException.class, () -> actual.addId(5, 0));
        actual.clear(); actual.compact(); check(bytes(actual) == 0, "heap empty release");
        actual.addAllIds(new int[]{3, 2, 1}, 0, new long[]{Long.MAX_VALUE, Long.MIN_VALUE, 0}, 0, 3);
        check(actual.removeFirstId() == 2 && actual.removeFirstId() == 1 && actual.removeFirstId() == 3, "priority extrema/regrow");
    }

    private static void table() throws Exception {
        for (int width : new int[]{1, 3, 17}) {
            MIndexTable<Integer> actual = new MIndexTable<>(SPACE, width, 0);
            List<Integer> expected = new ArrayList<>(); Random r = new Random(61);
            MIndexCompositeIndex store = new MIndexCompositeIndex();
            for (int turn = 0; turn < 4000; turn++) {
                if (turn % 3 != 0) {
                    int[] lane = randomIds(r, r.nextInt(8) * width, 100);
                    actual.addRowsIds(lane, 0, lane.length); for (int value : lane) expected.add(value);
                } else {
                    int start = r.nextInt(actual.rows() + 1), end = start + r.nextInt(actual.rows() - start + 1);
                    actual.removeRows(start, end); expected.subList(start * width, end * width).clear();
                }
                equal(actual.snapshotIds(), ints(expected), "row-major bulk/removal");
                if (turn % 37 == 0) {
                    int id = actual.canonicalId(store); actual.compact();
                    check(bytes(actual) == 4L * actual.size(), "table releases backing");
                    check(actual.canonicalId(store) == id && actual.freeze(store).canonicalId() == id, "table canonical stable");
                }
            }
            int[] before = actual.snapshotIds(); int[] invalid = new int[width * 2]; invalid[invalid.length - 1] = -1;
            expect(IndexOutOfBoundsException.class, () -> actual.addRowsIds(invalid, 0, invalid.length));
            if (width > 1) expect(IllegalArgumentException.class, () -> actual.addRowsIds(new int[1], 0, 1));
            equal(before, actual.snapshotIds(), "table invalid input atomic");
            actual.clear(); actual.compact(); check(bytes(actual) == 0, "table empty release");
            actual.addRowsIds(new int[width], 0, width); check(actual.rows() == 1, "table regrows");
        }
    }

    private static void graph() throws Exception {
        MIndexGraph<Integer, Integer> actual = new MIndexGraph<>(SPACE, SPACE, 0);
        List<Integer> triples = new ArrayList<>(); Random r = new Random(67);
        MIndexCompositeIndex store = new MIndexCompositeIndex();
        for (int turn = 0; turn < 4000; turn++) {
            if (turn % 3 != 0) {
                int[] lane = randomIds(r, r.nextInt(10) * 3, 20);
                actual.addEdgesIds(lane, 0, lane.length); for (int value : lane) triples.add(value);
            } else {
                int start = r.nextInt(actual.edgeCount() + 1), end = start + r.nextInt(actual.edgeCount() - start + 1);
                actual.removeEdges(start, end); triples.subList(start * 3, end * 3).clear();
            }
            MIndexGraph<Integer, Integer> oracle = new MIndexGraph<>(SPACE, SPACE);
            for (int i = 0; i < triples.size(); i += 3) oracle.addEdgeIds(triples.get(i), triples.get(i + 1), triples.get(i + 2));
            check(actual.edgeCount() * 3 == triples.size(), "builder duplicate count");
            equal(actual.freeze().copyCanonicalEdgeLane(), oracle.freeze().copyCanonicalEdgeLane(), "graph bulk/range semantics");
            if (turn % 43 == 0) {
                int id = actual.freeze(store).canonicalId(); actual.compact();
                check(actual.freeze(store).canonicalId() == id, "graph canonical stable");
                check(bytes(actual) == 12L * actual.edgeCount(), "graph all lanes release");
            }
        }
        int count = actual.edgeCount(); int[] before = actual.freeze().copyCanonicalEdgeLane();
        expect(IndexOutOfBoundsException.class, () -> actual.addEdgesIds(new int[]{1, 2, 3, 4, -1, 5}, 0, 6));
        expect(IllegalArgumentException.class, () -> actual.addEdgesIds(new int[2], 0, 2));
        equal(before, actual.freeze().copyCanonicalEdgeLane(), "graph invalid tail atomic");
        check(actual.edgeCount() == count, "graph failed batch retains multiplicity");
        actual.clear(); actual.compact(); check(bytes(actual) == 0, "graph empty release");
        actual.addEdgesIds(new int[]{1, 2, 3, 1, 2, 3}, 0, 6);
        check(actual.edgeCount() == 2 && actual.freeze().edgeCount() == 1, "builder duplicates/frozen set distinct");
    }

    private static void javaViews() throws Exception {
        MIndexMap<Integer, Integer> source = new MIndexMap<>(SPACE, SPACE);
        source.putAllIds(new int[]{4, 2, 9}, 0, new int[]{40, 20, 90}, 0, 3);
        Map<Integer, Integer> mutable = MIndexJavaViews.mutableMap(source);
        Map<Integer, Integer> frozen = MIndexJavaViews.readOnlyMap(source.freeze(new MIndexCompositeIndex()));
        List<Integer> order = new ArrayList<>();
        mutable.forEach((key, value) -> { order.add(key); order.add(value); source.clear(); });
        equal(ints(order), new int[]{2, 20, 4, 40, 9, 90}, "map forEach retains sorted snapshot under callback mutation");
        order.clear(); frozen.forEach((key, value) -> { order.add(key); order.add(value); });
        equal(ints(order), new int[]{2, 20, 4, 40, 9, 90}, "frozen traversal");
        expect(NullPointerException.class, () -> frozen.forEach(null));
        expect(NullPointerException.class, () -> mutable.forEach(null));
        RuntimeException marker = new RuntimeException("caller");
        try { frozen.forEach((key, value) -> { throw marker; }); throw new AssertionError("callback missing"); }
        catch (RuntimeException failure) { check(failure == marker, "callback exception preserved"); }
        MIndexFrozenBag<Integer> bag = MIndexFrozenBag.ofIdentityCounts(
                SPACE, new MIndexCompositeIndex(), new int[]{9, 300, 2, 5});
        Map<Integer, Integer> counts = MIndexJavaViews.readOnlyBagCounts(bag);
        order.clear(); counts.forEach((key, count) -> { order.add(key); order.add(count); });
        equal(ints(order), new int[]{2, 5, 9, 300}, "bag count projection traversal");
        expect(NullPointerException.class, () -> counts.forEach(null));
        Object[] frozenIterators = {
                frozen.entrySet().iterator(), counts.entrySet().iterator(),
                MIndexJavaViews.readOnlySet(MIndexFrozenSet.ofIds(SPACE, new MIndexCompositeIndex(), new int[]{9, 2})).iterator(),
                MIndexJavaViews.readOnlySet(MIndexFrozenOrderedSet.ofIds(SPACE, new MIndexCompositeIndex(), new int[]{9, 2})).iterator()};
        for (Object iterator : frozenIterators) {
            for (Field field : iterator.getClass().getDeclaredFields()) {
                check(!field.getType().isArray(), "preserve current frozen owner cursor without copied array");
            }
        }
        Map.Entry<Integer, Integer> first = frozen.entrySet().iterator().next();
        Map.Entry<Integer, Integer> second = frozen.entrySet().iterator().next();
        check(first != second && first.equals(second), "entries fresh caller-owned projections");
        expect(UnsupportedOperationException.class, () -> first.setValue(12));
    }

    private static void density() throws Exception {
        Class<?>[] owners = {MIndexList.class, MIndexSet.class, MIndexMap.class, MIndexMultiMap.class,
                MIndexDeque.class, MIndexPriorityQueue.class, MIndexTable.class, MIndexGraph.class,
                MIndexTuple.class, MIndexFrozenList.class, MIndexFrozenSet.class, MIndexFrozenMap.class,
                MIndexFrozenMultiMap.class, MIndexFrozenDeque.class, MIndexFrozenPriorityQueue.class,
                MIndexFrozenTable.class, MIndexFrozenGraph.class, MIndexFrozenTuple.class,
                MIndexFrozenBag.class, MIndexFrozenOrderedSet.class};
        for (Class<?> owner : owners) {
            for (Field field : owner.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Class<?> type = field.getType();
                check(type.isPrimitive() || type.isArray() && type.getComponentType().isPrimitive()
                        || MIndexSpace.class.isAssignableFrom(type) || type == MIndexCompositeIndex.class
                        || type == MIndexShapeIndex.class, owner + " only lanes/scalars/shared authorities");
                check(!type.isArray() || type.getComponentType().isPrimitive(), owner + " primitive occurrence lanes");
                check(!java.util.Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type)
                        && !Map.Entry.class.isAssignableFrom(type), owner + " no retained structural collection/entry");
            }
        }
        for (Field field : MIndexBiMap.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) check(field.getType() == MIndexMap.class, "bimap reuses map owners only");
        }
        MIndexList<Integer> list = new MIndexList<>(SPACE, 0);
        MIndexDeque<Integer> deque = new MIndexDeque<>(SPACE, 0);
        MIndexMultiMap<Integer, Integer> multi = new MIndexMultiMap<>(SPACE, SPACE, 0);
        MIndexPriorityQueue<Integer> heap = new MIndexPriorityQueue<>(SPACE, 0);
        MIndexTable<Integer> table = new MIndexTable<>(SPACE, 2, 0);
        MIndexGraph<Integer, Integer> graph = new MIndexGraph<>(SPACE, SPACE, 0);
        for (int i = 0; i < 4096; i++) {
            list.addId(i); deque.addLastId(i); multi.addIds(i, i); heap.addId(i, i);
            table.addRowIds(new int[]{i, i}); graph.addEdgeIds(i, 0, i);
        }
        list.removeRange(1, list.size());
        while (deque.size() > 1) deque.removeLastId();
        for (int i = 1; i < 4096; i++) multi.removeAllId(i);
        while (heap.size() > 1) heap.removeFirstId();
        table.removeRows(1, table.rows()); graph.removeEdges(1, graph.edgeCount());
        Object[] shapes = {list, deque, multi, heap, table, graph};
        long[] before = new long[shapes.length]; for (int i = 0; i < shapes.length; i++) before[i] = bytes(shapes[i]);
        list.compact(); deque.compact(); multi.compact(); heap.compact(); table.compact(); graph.compact();
        for (int i = 0; i < shapes.length; i++) {
            long after = bytes(shapes[i]); check(after < before[i], "actual memory capacity release");
            System.out.println("lane-bytes " + shapes[i].getClass().getSimpleName() + " " + before[i] + " -> " + after);
        }
    }

    private static int[] randomIds(Random random, int length, int bound) {
        int[] result = new int[length]; for (int i = 0; i < length; i++) result[i] = random.nextInt(bound); return result;
    }
    private static int[] ints(List<Integer> values) { return values.stream().mapToInt(Integer::intValue).toArray(); }
    private static long bytes(Object owner) throws Exception {
        long bytes = 0;
        for (Field field : owner.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !field.getType().isArray()) continue;
            field.setAccessible(true); Object lane = field.get(owner); if (lane == null) continue;
            Class<?> type = field.getType().getComponentType();
            check(type == int.class || type == long.class, "only primitive collection lanes");
            bytes += (long) Array.getLength(lane) * (type == int.class ? 4 : 8);
        }
        return bytes;
    }
    private static void equal(int[] actual, int[] expected, String message) {
        check(Arrays.equals(actual, expected), message + (Arrays.equals(actual, expected) ? "" : ": " + Arrays.toString(actual) + " != " + Arrays.toString(expected)));
    }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) { check(type.isInstance(failure), "wrong exception " + failure); return; }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
