/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Clear/reuse and escaped-view contracts on the real owner, independent of its clear algorithm. */
public final class M3LinkedClearContract {
    private static long checks;
    private M3LinkedClearContract() { }

    private static void check(boolean good, String description) {
        checks++;
        if (!good) throw new AssertionError(description);
    }
    private static Object field(Object owner, String name) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(owner);
    }
    private static void invalidated(Iterator<?> iterator) {
        boolean failed = false;
        try { iterator.next(); } catch (ConcurrentModificationException expected) { failed = true; }
        check(failed, "iterator must be invalidated by a nonempty clear");
    }

    private static void clearAndInspect(M3LinkedList<Object> list, boolean backwards) throws Exception {
        var reverse = list.reversed();
        Object storage = field(list, "storage");
        Object[] values = (Object[]) field(storage, "values");
        int[] before = (int[]) field(storage, "before");
        int[] after = (int[]) field(storage, "after");
        long[] mask = (long[]) field(storage, "liveMask");
        var forwardIterator = list.iterator();
        var reverseIterator = reverse.iterator();
        List<Object> sub = list.subList(0, list.size());
        var bound = list.spliterator();
        check(bound.estimateSize() == list.size(), "bound spliterator size");
        var late = list.spliterator();
        if (backwards) reverse.clear(); else list.clear();
        list.verifyInvariants();
        check(list.isEmpty() && reverse.isEmpty(), "both live views become empty");
        check(field(list, "storage") == storage && field(reverse, "storage") == storage,
                "shared Storage identity retained");
        check(field(storage, "values") == values && field(storage, "before") == before
                && field(storage, "after") == after && field(storage, "liveMask") == mask,
                "all backing arrays retained for reuse");
        for (Object value : values) check(value == null, "no payload reference remains");
        for (long word : mask) check(word == 0, "no occupancy bit remains");
        invalidated(forwardIterator); invalidated(reverseIterator);
        boolean staleSub = false;
        try { sub.size(); } catch (ConcurrentModificationException expected) { staleSub = true; }
        check(staleSub, "sublist observes parent structural change");
        // The existing iterator spliterator checks hasNext before next; an emptied
        // owner reports exhaustion here. Fail-fast is not guaranteed for this path.
        check(!bound.tryAdvance(v -> { throw new AssertionError("stale payload"); }),
                "bound spliterator cannot expose a cleared payload");
        check(late.estimateSize() == 0 && !late.tryAdvance(v -> { }), "late binding sees empty list");
        var emptyIterator = list.listIterator();
        list.clear(); reverse.clear();
        emptyIterator.add(null);
        check(list.size() == 1 && list.getFirst() == null, "empty clear does not invalidate iterator");
        list.removeFirst();
        for (int i = 0; i < Math.min(values.length, 73); i++) list.addLast(i);
        list.verifyInvariants();
        check(field(storage, "values") == values, "refill uses original capacity");
        list.clear();
    }

    private static void mapAndSet() throws Exception {
        for (int extent : new int[] {1, 8, 65, 1024, 8192}) {
            for (int divisor : new int[] {1, 2, 16, 64}) {
                for (boolean accessOrder : new boolean[] {false, true}) {
                    M3LinkedHashMap<Integer, Object> map = new M3LinkedHashMap<>(accessOrder);
                    LinkedHashMap<Integer, Object> jdk = new LinkedHashMap<>(16, 0.75f, accessOrder);
                    for (int i = 0; i < extent; i++) {
                        Integer key = i == 0 ? null : i;
                        Object value = i % 7 == 0 ? null : new Object();
                        map.put(key, value); jdk.put(key, value);
                    }
                    int remove = extent - Math.max(1, extent / divisor);
                    var a = map.keySet().iterator(); var b = jdk.keySet().iterator();
                    for (int i = 0; i < remove; i++) {
                        check(Objects.equals(a.next(), b.next()), "map remove encounter order");
                        a.remove(); b.remove();
                    }
                    map.get(extent - 1); jdk.get(extent - 1);
                    check(new ArrayList<>(map.entrySet()).equals(new ArrayList<>(jdk.entrySet())), "map entries");
                    map.verifyInvariants();
                    Object table = field(map, "table");
                    Object[] keys = (Object[]) field(table, "keys"), values = (Object[]) field(table, "values");
                    int[] index = (int[]) field(table, "index");
                    var iter = map.keySet().iterator();
                    var reverse = map.reversed();
                    var entry = map.firstEntry();
                    if (accessOrder) reverse.values().clear(); else map.keySet().clear();
                    jdk.clear();
                    check(map.isEmpty() && reverse.isEmpty() && map.equals(jdk), "map views clear together");
                    invalidated(iter);
                    check(entry != null, "snapshot entry survives clear");
                    for (Object key : keys) check(key == null, "map key reference released");
                    for (Object value : values) check(value == null, "map value reference released");
                    for (int bucket : index) check(bucket == 0, "tombstones also cleared");
                    check(field(table, "keys") == keys && field(table, "values") == values
                            && field(table, "index") == index, "map capacity retained");
                    map.verifyInvariants();
                    map.put(null, null); map.put(1, 1); map.verifyInvariants();
                    check(map.containsKey(null) && Objects.equals(map.get(1), 1), "map refill");

                    M3LinkedHashSet<Integer> set = new M3LinkedHashSet<>();
                    LinkedHashSet<Integer> expected = new LinkedHashSet<>();
                    for (int i = 0; i < extent; i++) { set.add(i); expected.add(i); }
                    for (int i = 0; i < remove; i++) { set.remove(i); expected.remove(i); }
                    check(new ArrayList<>(set).equals(new ArrayList<>(expected)), "set encounter order");
                    Object setTable = field(field(set, "map"), "table");
                    check(((Object[]) field(setTable, "values")).length == 0, "key-only table has no values lane");
                    var setIter = set.iterator();
                    set.reversed().clear(); expected.clear(); invalidated(setIter);
                    check(set.equals(expected), "set reverse clear");
                    set.verifyInvariants(); set.add(null); set.add(1); set.verifyInvariants();
                    check(set.contains(null) && set.contains(1), "set refill");
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Random random = new Random(0x4d33434c454152L);
        for (int extent : new int[] {1, 8, 15, 16, 63, 64, 65, 127, 1024, 4097, 65536}) {
            for (int divisor : new int[] {1, 2, 8, 16, 17, 64}) {
                for (boolean reverse : new boolean[] {false, true}) {
                    M3LinkedList<Object> list = new M3LinkedList<>();
                    for (int i = 0; i < extent; i++) list.addLast(i % 7 == 0 ? null : new Object());
                    int keep = Math.max(1, extent / divisor);
                    // Spread surviving slots over the full physical range, including its high end.
                    int seen = 0, remaining = extent;
                    for (var it = list.listIterator(); it.hasNext();) {
                        it.next();
                        if (seen < keep && (remaining == keep - seen || random.nextInt(remaining) < keep - seen)) {
                            seen++;
                        } else it.remove();
                        remaining--;
                    }
                    check(list.size() == keep, "retained random subset size");
                    list.verifyInvariants();
                    clearAndInspect(list, reverse);
                }
            }
        }
        // Repeated churn reuses slots in an order unrelated to physical slot order.
        M3LinkedList<Object> list = new M3LinkedList<>();
        for (int i = 0; i < 1024; i++) list.addLast(i);
        for (int i = 0; i < 4096; i++) { list.removeFirst(); list.addLast(i); }
        while (list.size() > 3) list.removeLast();
        check(Objects.equals(list.getFirst(), 3072), "churn order");
        clearAndInspect(list, true);
        mapAndSet();
        System.out.println("M3_LINKED_CLEAR_PASS checks=" + checks);
    }
}
