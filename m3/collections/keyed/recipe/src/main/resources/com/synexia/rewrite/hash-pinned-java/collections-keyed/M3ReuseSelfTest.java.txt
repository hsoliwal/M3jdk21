// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Private-storage proofs and an independent JDK oracle; reference objects are test-only. */
public final class M3ReuseSelfTest {
    private M3ReuseSelfTest() { }
    private static int passed;
    private record Key(int id, int hash) { @Override public int hashCode() { return hash; } }
    private record Cell(Object value, long weight) { }
    private static final class Oracle {
        final LinkedHashMap<Key, Cell> values = new LinkedHashMap<>(16, 0.75f, true);
        final int limit; final long budget; long weight;
        Oracle(int limit, long budget) { this.limit = limit; this.budget = budget; }
        Object get(Key key) { Cell c = values.get(key); return c == null ? null : c.value(); }
        Object remove(Key key) {
            Cell c = values.remove(key); if (c == null) return null;
            weight -= c.weight(); return c.value();
        }
        Object put(Key key, Object value, long w) {
            if (value == null) throw new NullPointerException();
            if (w < 0 || w > budget || limit == 0) throw new IllegalArgumentException();
            Object old = remove(key);
            while (values.size() >= limit || weight > budget - w) remove(values.firstEntry().getKey());
            values.put(key, new Cell(value, w)); weight += w; return old;
        }
        int trim(long w) {
            if (w < 0) throw new IllegalArgumentException();
            int removed = 0;
            while (weight > w) { remove(values.firstEntry().getKey()); removed++; }
            return removed;
        }
        void clear() { values.clear(); weight = 0; }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        check("20,000 weighted access-order JDK-reference operations", M3ReuseSelfTest::referenceTraces);
        check("collision growth, slot recycling and tombstone churn", M3ReuseSelfTest::collisions);
        check("released references and empty-capacity clear", M3ReuseSelfTest::releaseAndClear);
        check("null/equal keys, key identity and no-mutation refusals", M3ReuseSelfTest::identityAndRefusals);
        check("zero-weight entries and Long.MAX_VALUE arithmetic", M3ReuseSelfTest::weightBoundaries);
        check("two direct reference lanes; primitive metadata only", M3ReuseSelfTest::layout);
        check("8,000 concurrent cache operations plus invariant checks", M3ReuseSelfTest::concurrent);
        check("hash-index overflow refuses before allocation", M3ReuseSelfTest::capacityBoundaries);
        System.out.println("PASS " + passed + " cache reuse cases");
    }
    private static void referenceTraces() throws Exception {
        Random r = new Random(0x4d334341434845L);
        for (int trial = 0; trial < 4; trial++) {
            int limit = new int[] {1, 17, 129, 257}[trial];
            M3PlanCache<Key, Object> cache = new M3PlanCache<>(limit, 1200);
            Oracle oracle = new Oracle(limit, 1200);
            Key[] keys = new Key[515];
            for (int i = 1; i < keys.length; i++) keys[i] = new Key(i, trial % 2 == 0 ? 0 : i % 17);
            Object[] payload = new Object[31]; for (int i = 0; i < payload.length; i++) payload[i] = new Object();
            for (int op = 0; op < 5000; op++) {
                Key key = keys[r.nextInt(keys.length)]; int action = r.nextInt(100);
                if (action < 62) {
                    long w = r.nextInt(20); Object value = payload[r.nextInt(payload.length)];
                    same(oracle.put(key, value, w), cache.put(key, value, w));
                } else if (action < 82) same(oracle.get(key), cache.get(key));
                else if (action < 94) same(oracle.remove(key), cache.invalidate(key));
                else if (action < 99) { long w = r.nextInt(1201); eq(oracle.trim(w), cache.trimTo(w)); }
                else { oracle.clear(); cache.clear(); }
                var stats = cache.explain(); eq(oracle.values.size(), stats.entries()); eq(oracle.weight, stats.weight());
                if (op % 31 == 0) { cache.verifyInvariants(); eq(new ArrayList<>(oracle.values.keySet()), order(cache)); }
            }
            cache.verifyInvariants(); eq(new ArrayList<>(oracle.values.keySet()), order(cache));
        }
    }
    private static void collisions() throws Exception {
        M3PlanCache<Key, Object> cache = new M3PlanCache<>(513, 1000);
        Object value = new Object();
        for (int i = 0; i < 513; i++) cache.put(new Key(i, -1), value, 1);
        cache.verifyInvariants();
        for (int i = 0; i < 513; i += 2) same(value, cache.invalidate(new Key(i, -1)));
        cache.verifyInvariants();
        for (int i = 0; i < 513; i += 2) cache.put(new Key(i, -1), value, 1);
        for (int i = 0; i < 513; i++) same(value, cache.get(new Key(i, -1)));
        for (int i = 0; i < 6000; i++) {
            cache.put(new Key(1000 + i, i * 0x9e3779b9), value, 1);
            if (i % 101 == 0) cache.verifyInvariants();
        }
        cache.verifyInvariants(); eq(513, cache.explain().entries());
        for (int i = 6000 - 513; i < 6000; i++) same(value, cache.get(new Key(1000 + i, i * 0x9e3779b9)));
    }
    private static void releaseAndClear() throws Exception {
        M3PlanCache<Object, Object> cache = new M3PlanCache<>(8, 8);
        Object key = new Object(), value = new Object(); cache.put(key, value, 4);
        same(value, cache.invalidate(key)); cache.verifyInvariants();
        for (Object k : (Object[]) field(cache, "keys")) require(k == null);
        for (Object v : (Object[]) field(cache, "values")) require(v == null);
        cache.clear(); cache.verifyInvariants();
        for (Field f : M3PlanCache.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType().isArray()) {
                f.setAccessible(true); eq(0, Array.getLength(f.get(cache)));
            }
        }
        M3PlanCache<Object, Object> other = new M3PlanCache<>(8, 8);
        same(field(cache, "keys"), field(other, "keys"));
        cache.put(null, value, 0); require(other.get(null) == null); other.verifyInvariants();
        cache.clear(); cache.put(key, value, 8); eq(1, cache.trimTo(0)); cache.verifyInvariants();
        for (Object v : (Object[]) field(cache, "values")) require(v == null);
    }
    private static void identityAndRefusals() throws Exception {
        M3PlanCache<Key, Object> cache = new M3PlanCache<>(3, 4);
        Key oldKey = new Key(1, Integer.MIN_VALUE), newKey = new Key(1, Integer.MIN_VALUE);
        Object first = new Object(), second = new Object();
        cache.put(null, first, 1); cache.put(oldKey, first, 1);
        same(first, cache.put(newKey, second, 2));
        List<Object> order = order(cache); require(order.getLast() == newKey);
        same(second, cache.get(oldKey)); List<Object> before = order(cache);
        expect(IllegalArgumentException.class, () -> cache.put(null, second, 5));
        expect(IllegalArgumentException.class, () -> cache.put(newKey, second, -1));
        expect(NullPointerException.class, () -> cache.put(newKey, null, 0));
        expect(IllegalArgumentException.class, () -> cache.trimTo(-1));
        eq(before, order(cache)); eq(3L, cache.explain().weight()); cache.verifyInvariants();
        same(first, cache.get(null)); same(second, cache.get(newKey));
        M3PlanCache<Key, Object> disabled = new M3PlanCache<>(0, 0);
        expect(IllegalArgumentException.class, () -> disabled.put(null, first, 0));
        disabled.verifyInvariants();
    }
    private static void weightBoundaries() {
        M3PlanCache<String, Object> cache = new M3PlanCache<>(3, Long.MAX_VALUE);
        Object v = new Object();
        cache.put("zero", v, 0); cache.put("max", v, Long.MAX_VALUE); cache.verifyInvariants();
        cache.put("one", v, 1); require(cache.get("max") == null); eq(1L, cache.explain().weight());
        cache.put("zero2", v, 0); cache.put("zero3", v, 0); cache.verifyInvariants();
        eq(1, cache.trimTo(0)); eq(2, cache.explain().entries());
        cache.put("max", v, Long.MAX_VALUE); cache.put("max", v, Long.MAX_VALUE);
        eq(Long.MAX_VALUE, cache.explain().weight()); cache.verifyInvariants();
    }
    private static void layout() throws Exception {
        M3PlanCache<Key, Object> cache = new M3PlanCache<>(300, 300); Object payload = new Object();
        for (int i = 0; i < 300; i++) cache.put(new Key(i, i), payload, 1);
        int referenceLanes = 0;
        for (Field f : M3PlanCache.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            Class<?> type = f.getType();
            if (type == Object[].class) referenceLanes++;
            else require(type.isPrimitive() || type.isArray() && type.componentType().isPrimitive());
        }
        eq(2, referenceLanes);
        for (Object v : (Object[]) field(cache, "values")) same(payload, v);
        eq(300, ((Object[]) field(cache, "keys")).length); cache.verifyInvariants();
    }
    private static void concurrent() throws Exception {
        M3PlanCache<Key, Object> cache = new M3PlanCache<>(257, 257); Object value = new Object();
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                final int seed = worker;
                futures.add(pool.submit(() -> {
                    try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                    Random random = new Random(seed);
                    for (int i = 0; i < 2000; i++) {
                        Key key = new Key(random.nextInt(700), 7);
                        switch (random.nextInt(4)) {
                            case 0 -> cache.put(key, value, 1);
                            case 1 -> cache.get(key);
                            case 2 -> cache.invalidate(key);
                            default -> cache.verifyInvariants();
                        }
                    }
                }));
            }
            start.countDown(); for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
        }
        cache.verifyInvariants(); require(cache.explain().entries() <= 257);
    }
    private static void capacityBoundaries() throws Exception {
        var doubled = M3PlanCache.class.getDeclaredMethod("doubled", int.class); doubled.setAccessible(true);
        eq(32, doubled.invoke(null, 16));
        try { doubled.invoke(null, 1 << 30); throw new AssertionError("missing overflow refusal"); }
        catch (java.lang.reflect.InvocationTargetException expected) { require(expected.getCause() instanceof OutOfMemoryError); }
        M3PlanCache<Object, Object> large = new M3PlanCache<>(Integer.MAX_VALUE, Long.MAX_VALUE);
        large.verifyInvariants(); eq(0, ((Object[]) field(large, "keys")).length);
    }
    private static List<Object> order(M3PlanCache<?, ?> cache) throws Exception {
        ArrayList<Object> result = new ArrayList<>(); Object[] keys = (Object[]) field(cache, "keys");
        int[] after = (int[]) field(cache, "after");
        for (int s = (int) field(cache, "first"); s >= 0; s = after[s]) result.add(keys[s]);
        return result;
    }
    private static Object field(Object owner, String name) throws Exception {
        Field f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    private static void require(boolean condition) { if (!condition) throw new AssertionError(); }
    private static void same(Object expected, Object actual) { if (expected != actual) throw new AssertionError("identity mismatch"); }
    private static void eq(Object expected, Object actual) { if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual); }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void check(String name, Checked test) throws Exception { test.run(); passed++; System.out.println("PASS " + name); }
    private static void expect(Class<? extends Throwable> type, Checked test) throws Exception {
        try { test.run(); } catch (Throwable error) { if (type.isInstance(error)) return; throw error; }
        throw new AssertionError("missing " + type.getName());
    }
}
