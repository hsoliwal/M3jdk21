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
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.LongPredicate;
import java.util.function.Supplier;
import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongLongPredicate;
import jdk.m3.collections.LongLongSortedMap;
import jdk.m3.collections.LongSortedSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * PackedLongBatchSortedSet and PackedLongLongBatchSortedMap index their pending batch (slot lanes
 * with a
 * power-of-two bit-mask hash) instead of scanning it: against the master copies BatchSetReference
 * and
 * BatchMapReference driven by the same random, scripted, colliding and out-of-memory operations,
 * equal results,
 * exceptions, iteration and callback order, and equal private lanes except the new pending index,
 * whose
 * invariant is checked after every operation.
 */
//FLLDCIM: LEETCODIFY — differential proof for the pending-batch index (offset lanes + bit mask)
final class BatchSortedIndexTest {
    static final Set<String> EXTRA = Set.of("pendingSlots", "pendingIndexed");
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    static long ops;
    static long indexedChecks;
    static long[] collideWrap;
    static long[] collideLow;

    @BeforeAll
    static void collisionPools() {
        collideWrap = colliding(1023, 1023, 600);
        collideLow = colliding(63, 0, 300);
    }

    @Test
    void randomOperationsMatchMaster() throws Exception {
        int[] batches = {1, 2, 3, 16, 17, 31, 32, 33, 34, 35, 64, 65, 100, 257, 1024, 5000};
        for (int b : batches) {
            for (int domain = 0; domain < 4; domain++) {
                runSet(b, domain, 0x5E7L * 1_000_003L + b * 31L + domain);
                runMap(b, domain, 0xBA7C4L * 1_000_003L + b * 31L + domain);
            }
        }
        assertTrue(ops > 0, "operations compared");
    }

    @Test
    void forcedOutOfMemoryMatchesMaster() throws Exception {
        forcedOomSet();
        forcedOomMap();
    }

    @Test
    void scriptedCollisionsMatchMaster() throws Exception {
        for (int n : new int[] {33, 34, 40, 63, 64, 65, 100, 300}) {
            scriptedSet(n, collideWrap);
            scriptedSet(n, null);
            scriptedMap(n, collideWrap);
            scriptedMap(n, null);
        }
        assertTrue(indexedChecks > 0, "pending index invariant checked");
    }

    static long[] colliding(int mask, int target, int n) {
        long[] keys = new long[n];
        int found = 0;
        for (long k = 1; found < n; k++) {
            long key = k * 0x9E3779B97F4A7C15L;
            if ((PackedSupport.mix(key) & mask) == target) keys[found++] = key;
        }
        return keys;
    }

    static long[] pool(Random r, int domain) {
        switch (domain) {
            case 0: {
                long[] p = new long[201];
                for (int i = 0; i < p.length; i++) p[i] = i - 100;
                return p;
            }
            case 1: {
                long[] p = new long[400];
                for (int i = 0; i < p.length; i++) p[i] = r.nextLong();
                p[0] = Long.MIN_VALUE; p[1] = Long.MAX_VALUE; p[2] = 0;
                return p;
            }
            case 2:
                return collideWrap;
            default: {
                long[] p = new long[361];
                System.arraycopy(collideLow, 0, p, 0, 300);
                for (int i = 300; i < 361; i++) p[i] = i - 330;
                return p;
            }
        }
    }

    // ---------------------------------------------------------------- set
    static void runSet(int batch, int domain, long seed) throws Exception {
        Random r = new Random(seed);
        long[] pool = pool(r, domain);
        boolean reentrant = ((batch + domain) & 1) == 0;
        int expected = r.nextInt(4) * 8;
        LongSortedSet[] hr = new LongSortedSet[1];
        LongSortedSet[] ha = new LongSortedSet[1];
        BatchSetReference ref = new BatchSetReference(expected, batch, insertion(hr, reentrant));
        PackedLongBatchSortedSet act = new PackedLongBatchSortedSet(expected, batch, insertion(ha,
                reentrant));
        hr[0] = ref;
        ha[0] = act;
        for (int step = 0; step < 3000; step++) {
            boolean quiet = (step / 400) % 2 == 0;
            int op = quiet ? r.nextInt(4) : r.nextInt(17);
            long v = pool[r.nextInt(pool.length)];
            switch (op) {
                case 0, 1 -> same("add", () -> ref.add(v), () -> act.add(v));
                case 2 -> same("remove", () -> ref.remove(v), () -> act.remove(v));
                case 3 -> same("contains", () -> ref.contains(v), () -> act.contains(v));
                case 4 -> {
                    long[] values = new long[r.nextInt(40)];
                    for (int i = 0; i < values.length; i++) values[i] = pool[r.nextInt(pool.length)
                            ];
                    same("addAll", () -> ref.addAll(values), () -> act.addAll(values));
                }
                case 5 -> same("flush", () -> { ref.flush(); return "ok"; }, () -> { act.flush();
                        return "ok"; });
                case 6 -> {
                    int stopAt = r.nextInt(3) == 0 ? r.nextInt(40) : -1;
                    int m = r.nextInt(5) + 2;
                    same("filterInPlace", () -> ref.filterInPlace(retain(stopAt, m)),
                            () -> act.filterInPlace(retain(stopAt, m)));
                }
                case 7 -> same("first", () -> ref.first(), () -> act.first());
                case 8 -> same("last", () -> ref.last(), () -> act.last());
                case 9 -> {
                    long d = r.nextLong();
                    same("floor", () -> ref.floor(v, d), () -> act.floor(v, d));
                    same("ceiling", () -> ref.ceiling(v, d), () -> act.ceiling(v, d));
                    same("lower", () -> ref.lower(v, d), () -> act.lower(v, d));
                    same("higher", () -> ref.higher(v, d), () -> act.higher(v, d));
                }
                case 10 -> {
                    same("pendingSize", () -> ref.pendingSize(), () -> act.pendingSize());
                    same("batchSize", () -> ref.batchSize(), () -> act.batchSize());
                    same("isEmpty", () -> ref.isEmpty(), () -> act.isEmpty());
                    same("size", () -> ref.size(), () -> act.size());
                }
                case 11 -> same("toArray", () -> Arrays.toString(ref.toArray()), () ->
                        Arrays.toString(act.toArray()));
                case 12 -> same("iterator", () -> drain(ref.iterator()), () -> drain(act.iterator())
                        );
                case 13 -> {
                    if (r.nextInt(6) == 0) {
                        same("clear", () -> { ref.clear(); return "ok"; }, () -> { act.clear();
                                return "ok"; });
                    }
                }
                case 14 -> {
                    if (r.nextInt(6) == 0) {
                        same("trim", () -> { ref.trimToSize(); return "ok"; }, () -> {
                                act.trimToSize(); return "ok"; });
                    }
                }
                case 15 -> same("filterNull", () -> ref.filterInPlace(null), () -> act.filterInPlace
                        (null));
                default -> same("forEachLong", () -> each(ref), () -> each(act));
            }
            lanes(ref, act);
            indexInvariant(act, "pending");
            ops++;
        }
        same("final", () -> Arrays.toString(ref.toArray()), () -> Arrays.toString(act.toArray()));
    }

    static LongPredicate insertion(LongSortedSet[] h, boolean reentrant) {
        return value -> {
            if (h[0] instanceof PackedLongBatchSortedSet) invariantUnchecked(h[0], "pending");
            if (reentrant && h[0] != null && Math.floorMod(value, 13) == 0) {
                h[0].add(value + 1);
                h[0].remove(value - 1);
                h[0].contains(value + 2);
            }
            return Math.floorMod(value, 11) != 5;
        };
    }

    static LongPredicate retain(int stopAt, int m) {
        int[] calls = {0};
        return value -> {
            if (calls[0]++ == stopAt) throw new IllegalStateException("stop at " + stopAt);
            return Math.floorMod(value, m) != 0;
        };
    }

    static String drain(LongIterator it) {
        StringBuilder b = new StringBuilder();
        while (it.hasNext()) b.append(it.nextLong()).append(',');
        try {
            it.nextLong();
            b.append("|no-exception");
        } catch (RuntimeException e) {
            b.append('|').append(e.getClass().getName());
        }
        return b.toString();
    }

    static String each(LongSortedSet s) {
        StringBuilder b = new StringBuilder();
        s.forEachLong(value -> b.append(value).append(','));
        return b.toString();
    }

    // ---------------------------------------------------------------- map
    static void runMap(int batch, int domain, long seed) throws Exception {
        Random r = new Random(seed);
        long[] pool = pool(r, domain);
        boolean reentrant = ((batch + domain) & 1) == 1;
        int expected = r.nextInt(4) * 8;
        LongLongSortedMap[] hr = new LongLongSortedMap[1];
        LongLongSortedMap[] ha = new LongLongSortedMap[1];
        BatchMapReference ref = new BatchMapReference(expected, batch, admission(hr, reentrant));
        PackedLongLongBatchSortedMap act = new PackedLongLongBatchSortedMap(expected, batch,
                admission(ha, reentrant));
        hr[0] = ref;
        ha[0] = act;
        for (int step = 0; step < 3000; step++) {
            boolean quiet = (step / 400) % 2 == 0;
            int op = quiet ? r.nextInt(6) : r.nextInt(20);
            long k = pool[r.nextInt(pool.length)];
            long v = r.nextInt(4) == 0 ? r.nextLong() : r.nextInt(100);
            switch (op) {
                case 0, 1 -> same("put", () -> ref.put(k, v), () -> act.put(k, v));
                case 2 -> same("remove", () -> ref.remove(k), () -> act.remove(k));
                case 3 -> same("containsKey", () -> ref.containsKey(k), () -> act.containsKey(k));
                case 4 -> same("getOrDefault", () -> ref.getOrDefault(k, v), () -> act.getOrDefault(
                        k, v));
                case 5 -> same("getOrThrow", () -> ref.getOrThrow(k), () -> act.getOrThrow(k));
                case 6 -> {
                    int n = r.nextInt(30);
                    long[] keys = new long[n];
                    long[] values = new long[r.nextInt(10) == 0 ? n + 1 : n];
                    for (int i = 0; i < n; i++) keys[i] = pool[r.nextInt(pool.length)];
                    for (int i = 0; i < values.length; i++) values[i] = r.nextInt(100);
                    same("putAll", () -> ref.putAll(keys, values), () -> act.putAll(keys, values));
                }
                case 7 -> same("flush", () -> { ref.flush(); return "ok"; }, () -> { act.flush();
                        return "ok"; });
                case 8 -> {
                    same("firstKey", () -> ref.firstKey(), () -> act.firstKey());
                    same("lastKey", () -> ref.lastKey(), () -> act.lastKey());
                }
                case 9 -> {
                    same("floorKey", () -> ref.floorKey(k, v), () -> act.floorKey(k, v));
                    same("ceilingKey", () -> ref.ceilingKey(k, v), () -> act.ceilingKey(k, v));
                    same("lowerKey", () -> ref.lowerKey(k, v), () -> act.lowerKey(k, v));
                    same("higherKey", () -> ref.higherKey(k, v), () -> act.higherKey(k, v));
                }
                case 10 -> {
                    int stopAt = r.nextInt(3) == 0 ? r.nextInt(40) : -1;
                    boolean mutate = r.nextInt(5) == 0;
                    int m = r.nextInt(5) + 2;
                    same("filterInPlace", () -> ref.filterInPlace(keep(hr, stopAt, mutate, m)),
                            () -> act.filterInPlace(keep(ha, stopAt, mutate, m)));
                }
                case 11 -> {
                    boolean mutate = r.nextInt(5) == 0;
                    same("forEach", () -> each(ref, mutate), () -> each(act, mutate));
                }
                case 12 -> {
                    if (r.nextInt(6) == 0) {
                        same("clear", () -> { ref.clear(); return "ok"; }, () -> { act.clear();
                                return "ok"; });
                    }
                }
                case 13 -> {
                    if (r.nextInt(6) == 0) {
                        same("trim", () -> { ref.trimToSize(); return "ok"; }, () -> {
                                act.trimToSize(); return "ok"; });
                    }
                }
                case 14 -> {
                    same("keys", () -> Arrays.toString(ref.keysToArray()), () -> Arrays.toString(
                            act.keysToArray()));
                    same("values", () -> Arrays.toString(ref.valuesToArray()), () -> Arrays.toString
                            (act.valuesToArray()));
                }
                case 15 -> {
                    same("size", () -> ref.size(), () -> act.size());
                    same("isEmpty", () -> ref.isEmpty(), () -> act.isEmpty());
                    same("sortedSize", () -> ref.sortedSize(), () -> act.sortedSize());
                    same("pendingSize", () -> ref.pendingSize(), () -> act.pendingSize());
                    same("batchSize", () -> ref.batchSize(), () -> act.batchSize());
                    same("payload", () -> ref.backingArrayPayloadBytes(), () ->
                            act.backingArrayPayloadBytes());
                }
                case 16 -> same("get", () -> ref.get(k), () -> act.get(k));
                case 17 -> same("filterNull", () -> ref.filterInPlace(null), () -> act.filterInPlace
                        (null));
                default -> same("put", () -> ref.put(k, v), () -> act.put(k, v));
            }
            lanes(ref, act);
            indexInvariant(act, "pendingKeys");
            ops++;
        }
    }

    static LongLongPredicate admission(LongLongSortedMap[] h, boolean reentrant) {
        return (key, value) -> {
            if (h[0] instanceof PackedLongLongBatchSortedMap) invariantUnchecked(h[0],
                    "pendingKeys");
            if (reentrant && h[0] != null && Math.floorMod(key, 13) == 0) {
                h[0].containsKey(key + 1);
                h[0].getOrDefault(key - 1, 7L);
                if (Math.floorMod(key, 26) == 0) h[0].put(key + 1, 1L);
            }
            return Math.floorMod(value, 11) != 5;
        };
    }

    static LongLongPredicate keep(LongLongSortedMap[] h, int stopAt, boolean mutate, int m) {
        int[] calls = {0};
        return (key, value) -> {
            int call = calls[0]++;
            if (call == stopAt) throw new IllegalStateException("stop at " + stopAt);
            if (mutate && call == 1) h[0].clear();
            return Math.floorMod(key, m) != 0;
        };
    }

    static String each(LongLongSortedMap map, boolean mutate) {
        StringBuilder b = new StringBuilder();
        map.forEach((key, value) -> {
            b.append(key).append('=').append(value).append(',');
            if (mutate) map.remove(key);
        });
        return b.toString();
    }

    // ---------------------------------------------------------------- forced mid-flush failure
    static void forcedOomSet() throws Exception {
        BatchSetReference ref = new BatchSetReference(0, 1024);
        PackedLongBatchSortedSet act = new PackedLongBatchSortedSet(0, 1024);
        for (long i = 60; i > 0; i--) {
            long v = i;
            same("add", () -> ref.add(v), () -> act.add(v));
        }
        check((boolean) get(act, "pendingIndexed"), "set indexed before forced failure");
        set(ref, "sortedSize", MAX_ARRAY_SIZE);
        set(act, "sortedSize", MAX_ARRAY_SIZE);
        same("flushOom", () -> { ref.flush(); return "ok"; }, () -> { act.flush(); return "ok"; });
        set(ref, "sortedSize", 0);
        set(act, "sortedSize", 0);
        lanes(ref, act);
        indexInvariant(act, "pending");
        for (long i = 0; i <= 61; i++) {
            long v = i;
            same("contains", () -> ref.contains(v), () -> act.contains(v));
        }
        for (long i = 1; i <= 60; i += 3) {
            long v = i;
            same("remove", () -> ref.remove(v), () -> act.remove(v));
            same("contains", () -> ref.contains(v + 1), () -> act.contains(v + 1));
        }
        for (long i = 100; i < 130; i++) {
            long v = i;
            same("add", () -> ref.add(v), () -> act.add(v));
            lanes(ref, act);
            indexInvariant(act, "pending");
        }
        same("toArray", () -> Arrays.toString(ref.toArray()), () -> Arrays.toString(act.toArray()));
        ops++;
    }

    static void forcedOomMap() throws Exception {
        BatchMapReference ref = new BatchMapReference(0, 1024);
        PackedLongLongBatchSortedMap act = new PackedLongLongBatchSortedMap(0, 1024);
        for (long i = 60; i > 0; i--) {
            long k = i;
            same("put", () -> ref.put(k, k * 7), () -> act.put(k, k * 7));
        }
        check((boolean) get(act, "pendingIndexed"), "map indexed before forced failure");
        set(ref, "sortedSize", MAX_ARRAY_SIZE);
        set(act, "sortedSize", MAX_ARRAY_SIZE);
        same("flushOom", () -> { ref.flush(); return "ok"; }, () -> { act.flush(); return "ok"; });
        set(ref, "sortedSize", 0);
        set(act, "sortedSize", 0);
        lanes(ref, act);
        indexInvariant(act, "pendingKeys");
        for (long i = 0; i <= 61; i++) {
            long k = i;
            same("getOrDefault", () -> ref.getOrDefault(k, -1), () -> act.getOrDefault(k, -1));
            same("put", () -> ref.put(k, k + 1), () -> act.put(k, k + 1));
        }
        for (long i = 1; i <= 60; i += 3) {
            long k = i;
            same("remove", () -> ref.remove(k), () -> act.remove(k));
            same("containsKey", () -> ref.containsKey(k + 1), () -> act.containsKey(k + 1));
        }
        lanes(ref, act);
        indexInvariant(act, "pendingKeys");
        same("keys", () -> Arrays.toString(ref.keysToArray()), () -> Arrays.toString(act.keysToArray
                ()));
        same("values", () -> Arrays.toString(ref.valuesToArray()), () -> Arrays.toString(
                act.valuesToArray()));
        ops++;
    }

    // ---------------------------------------------------------------- scripted pending-lane edits
    // Remove last / first / middle while indexed, then look up the removed key and its neighbours.
    static void scriptedSet(int n, long[] keys) throws Exception {
        BatchSetReference ref = new BatchSetReference(0, 4096);
        PackedLongBatchSortedSet act = new PackedLongBatchSortedSet(0, 4096);
        long[] k = new long[n];
        for (int i = 0; i < n; i++) k[i] = keys == null ? 1000 - 7L * i : keys[i];
        for (long v : k) same("add", () -> ref.add(v), () -> act.add(v));
        int[] order = {n - 1, 0, n / 2, n - 2, 1, n / 3};
        for (int round = 0; round < 3; round++) {
            for (int at : order) {
                long v = k[at];
                same("remove", () -> ref.remove(v), () -> act.remove(v));
                for (int d = -2; d <= 2; d++) {
                    long w = k[Math.floorMod(at + d, n)];
                    same("contains", () -> ref.contains(w), () -> act.contains(w));
                    same("removeAbsent", () -> ref.remove(v), () -> act.remove(v));
                }
                lanes(ref, act);
            }
            for (int at : order) {
                long v = k[at];
                same("readd", () -> ref.add(v), () -> act.add(v));
            }
            lanes(ref, act);
            indexInvariant(act, "pending");
        }
        same("toArray", () -> Arrays.toString(ref.toArray()), () -> Arrays.toString(act.toArray()));
        ops++;
    }

    static void scriptedMap(int n, long[] keys) throws Exception {
        BatchMapReference ref = new BatchMapReference(0, 4096);
        PackedLongLongBatchSortedMap act = new PackedLongLongBatchSortedMap(0, 4096);
        long[] k = new long[n];
        for (int i = 0; i < n; i++) k[i] = keys == null ? 1000 - 7L * i : keys[i];
        for (long v : k) same("put", () -> ref.put(v, v ^ 5), () -> act.put(v, v ^ 5));
        int[] order = {n - 1, 0, n / 2, n - 2, 1, n / 3};
        for (int r0 = 0; r0 < 3; r0++) {
            int round = r0;
            for (int at : order) {
                long v = k[at];
                same("remove", () -> ref.remove(v), () -> act.remove(v));
                for (int d = -2; d <= 2; d++) {
                    long w = k[Math.floorMod(at + d, n)];
                    same("containsKey", () -> ref.containsKey(w), () -> act.containsKey(w));
                    same("getOrDefault", () -> ref.getOrDefault(w, -9), () -> act.getOrDefault(w, -9
                            ));
                    same("put", () -> ref.put(w, w + round), () -> act.put(w, w + round));
                }
                lanes(ref, act);
            }
            for (int at : order) {
                long v = k[at];
                same("readd", () -> ref.put(v, round), () -> act.put(v, round));
            }
            lanes(ref, act);
            indexInvariant(act, "pendingKeys");
        }
        same("keys", () -> Arrays.toString(ref.keysToArray()), () -> Arrays.toString(act.keysToArray
                ()));
        same("values", () -> Arrays.toString(ref.valuesToArray()), () -> Arrays.toString(
                act.valuesToArray()));
        ops++;
    }

    // ---------------------------------------------------------------- oracle atoms
    static void same(String what, Supplier<Object> ref, Supplier<Object> act) {
        String a = outcome(ref);
        String b = outcome(act);
        if (!a.equals(b)) throw new AssertionError(what + " ref=" + a + " act=" + b);
    }

    static String outcome(Supplier<Object> s) {
        try {
            return "R:" + s.get();
        } catch (Throwable t) {
            return "E:" + t.getClass().getName() + ":" + t.getMessage();
        }
    }

    static void lanes(Object ref, Object act) throws Exception {
        Set<String> names = new HashSet<>();
        for (Field f : ref.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            names.add(f.getName());
            Field g = act.getClass().getDeclaredField(f.getName());
            f.setAccessible(true);
            g.setAccessible(true);
            Object a = f.get(ref);
            Object b = g.get(act);
            boolean eq;
            if (a instanceof long[] x) eq = Arrays.equals(x, (long[]) b);
            else if (a instanceof int[] x) eq = Arrays.equals(x, (int[]) b);
            else if (a instanceof Number || a instanceof Boolean) eq = a.equals(b);
            else continue;
            if (!eq) throw new AssertionError("private lane " + f.getName() + " differs");
        }
        for (Field g : act.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(g.getModifiers()) && !names.contains(g.getName()) &&
                    !EXTRA.contains(g.getName())) {
                throw new AssertionError("unexpected field " + g.getName());
            }
        }
    }

    static final boolean NO_INDEX_CHECK = Boolean.getBoolean("probe.noIndexCheck");

    static void indexInvariant(Object act, String laneName) throws Exception {
        if (NO_INDEX_CHECK || !(boolean) get(act, "pendingIndexed")) return;
        indexedChecks++;
        int[] slots = (int[]) get(act, "pendingSlots");
        long[] lane = (long[]) get(act, laneName);
        int size = (int) get(act, "pendingSize");
        check(slots.length >= 2 && Integer.bitCount(slots.length) == 1,
                "slot table not a power of two");
        boolean[] seen = new boolean[size];
        int used = 0;
        for (int e : slots) {
            if (e == 0) continue;
            check(e >= 1 && e <= size && !seen[e - 1], "bad slot entry " + e + " size " + size);
            seen[e - 1] = true;
            used++;
        }
        check(used == size, "slot count " + used + " != pendingSize " + size);
        check(used <= slots.length >>> 1, "load above 1/2");
        for (int i = 0; i < size; i++) {
            check(PackedHashAtoms.findLaneIndex(slots, lane, lane[i]) == i, "find mismatch at " + i)
                    ;
        }
    }

    static void invariantUnchecked(Object act, String laneName) {
        try {
            indexInvariant(act, laneName);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static Object get(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void set(Object o, String name, int value) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(o, value);
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
