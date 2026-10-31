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
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Seeded differential run of M3PassiveExpiringMap against the Apache Commons Collections 4.5.0 PassiveExpiringMap on a
 * shared fake clock: 20 runs of 5,000 random operations (100,000 in total) over all eight constructors, constant and
 * per-entry policies, clock jumps, clock steps back and, in every third run, a clock that advances on each read.
 *
 * <p>Each operation runs on the oracle, the clock is rewound, and the same operation runs on the M3 map: the return
 * value or exception class and the clock after the operation (the number of clock reads) must agree. After every step
 * the full state is compared without expiring anything (live views taken at the start, hashCode, toString tokens,
 * equality with a snapshot), the comparison must not read the clock, and the M3 lane and expiry-index invariants are
 * checked. The provider iterates in HashMap order, so traversals are compared as sorted lists and only
 * order-independent removals go through iterators. One operation serializes the M3 map: the copy must re-serialize to
 * the same bytes (mappings, expiration times, policy and slot order) and hold the same mappings, without a clock read.
 */
final class M3PassiveExpiringMapDifferentialTest {
    private static final Logger LOG = Logger.getLogger(M3PassiveExpiringMapDifferentialTest.class.getName());
    private static final int RUNS = 20;
    private static final int OPS_PER_RUN = 5_000;
    private static final int OPS = 45;
    private static final long BASE = 1_000_000L;
    private static final long NEAR_MAX = Long.MAX_VALUE - 400_000L;
    private static final long[] TTLS = {7, 0, 25, 120, -1, Long.MAX_VALUE, 3, 1, 60};
    private static final long[] UNIT_TTLS = {2, 1_500_000, 7_000, 1, -3, Long.MAX_VALUE, 0};
    private static final TimeUnit[] UNITS = {TimeUnit.SECONDS, TimeUnit.NANOSECONDS, TimeUnit.MICROSECONDS,
        TimeUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.DAYS, TimeUnit.HOURS};
    private static final String POLICY = M3PassiveExpiringTestPolicy.class.getName();
    private static final M3PassiveExpiringOracle ORACLE = new M3PassiveExpiringOracle(POLICY);

    private static final AtomicLong OPERATIONS = new AtomicLong();
    private static final AtomicLong CHECKS = new AtomicLong();
    private static final AtomicLong RING_STEPS = new AtomicLong();
    private static final AtomicLong HEAP_STEPS = new AtomicLong();
    private static final AtomicLong PURGED = new AtomicLong();
    private static final AtomicLong MAX_RECORDS = new AtomicLong();
    private static final AtomicLong ROUND_TRIPS = new AtomicLong();

    enum Variant { DEFAULT, TTL, TTL_UNIT, TTL_MAP, TTL_UNIT_MAP, POLICY, POLICY_MAP, MAP }

    @AfterAll
    static void report() {
        LOG.info(() -> "M3PassiveExpiringMap differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get()
                + " ringSteps=" + RING_STEPS.get() + " heapSteps=" + HEAP_STEPS.get() + " purgedByWholeMapCalls="
                + PURGED.get() + " maxIndexRecords=" + MAX_RECORDS.get() + " serializationRoundTrips="
                + ROUND_TRIPS.get());
        assertEquals((long) RUNS * OPS_PER_RUN, OPERATIONS.get(), "every run completes");
        assertTrue(RING_STEPS.get() > 0 && HEAP_STEPS.get() > 0, "both index modes exercised");
        assertTrue(PURGED.get() > 0 && MAX_RECORDS.get() >= 64, "expiry and compaction exercised");
        assertTrue(ROUND_TRIPS.get() > 0, "serialization exercised");
    }

    static Stream<Arguments> runs() {
        return IntStream.range(0, RUNS).mapToObj(run -> Arguments.of(run, 0x5EED_0000L + 7919L * run));
    }

    @ParameterizedTest(name = "run {0}")
    @MethodSource("runs")
    void seededOperationsMatchCommons(int run, long seed) {
        Random r = new Random(seed);
        Variant variant = Variant.values()[run % 8];
        long ttl = TTLS[run % TTLS.length];
        long unitTtl = UNIT_TTLS[run % UNITS.length];
        TimeUnit unit = UNITS[run % UNITS.length];
        long tick = run % 3 == 2 ? 1 : 0;
        long start = run % 7 == 6 ? NEAR_MAX : BASE + run * 1_000L;
        long scale = scale(variant, ttl, unitTtl, unit);
        M3PassiveExpiringTestClock.set(start, tick);
        Map<Integer, String> initial = new HashMap<>();
        for (int i = r.nextInt(6); i > 0; i--) {
            initial.put(key(r), value(r));
        }
        Subject oracle = oracleSubject(variant, ttl, unitTtl, unit, initial);
        Subject m3 = m3Subject(variant, ttl, unitTtl, unit, initial);
        same("views", () -> oracle.captureViews(), () -> m3.captureViews());
        compareState(oracle, m3, run, -1);
        for (int step = 0; step < OPS_PER_RUN; step++) {
            advanceClock(r, scale, start);
            int op = r.nextInt(OPS);
            long opSeed = r.nextLong();
            int before = m3.liveEntries.size();
            String what = "run " + run + " step " + step + " op " + op;
            same(what, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(oracle, m3, run, step);
            M3PassiveExpiringMap<Integer, String> map = (M3PassiveExpiringMap<Integer, String>) m3.map;
            (map.orderIsHeap() ? HEAP_STEPS : RING_STEPS).incrementAndGet();
            MAX_RECORDS.accumulateAndGet(map.orderRecords(), Math::max);
            if (op == 13 || op == 14) {
                PURGED.addAndGet(before - m3.liveEntries.size());
            }
        }
    }

    private static long scale(Variant variant, long ttl, long unitTtl, TimeUnit unit) {
        long millis = switch (variant) {
            case TTL, TTL_MAP -> ttl;
            case TTL_UNIT, TTL_UNIT_MAP -> M3PassiveExpiringMap.toMillis(unitTtl, unit);
            case POLICY, POLICY_MAP -> 32;
            default -> 400;
        };
        return millis < 0 || millis > 3_200 ? 400 : Math.max(2, millis / 8);
    }

    private static void advanceClock(Random r, long scale, long start) {
        int dice = r.nextInt(400);
        long now = M3PassiveExpiringTestClock.peek();
        long delta;
        if (dice < 2) {
            delta = -r.nextInt((int) Math.min(3 * scale + 1, 1_000));
        } else if (dice < 6) {
            delta = 10 * scale;
        } else {
            delta = r.nextInt(4) == 0 ? 0 : r.nextInt((int) scale);
        }
        if (now + delta < start || now + delta > Long.MAX_VALUE - 200_000L) {
            delta = 0;
        }
        M3PassiveExpiringTestClock.advance(delta);
    }

    // ------------------------------------------------------------------ subjects

    private static final class Subject {
        final Map<Integer, String> map;
        final Function<Map<Integer, String>, M3PassiveExpiringOracle.MapCursor> cursors;
        Set<Map.Entry<Integer, String>> liveEntries;
        Set<Integer> liveKeys;
        Collection<String> liveValues;

        Subject(Map<Integer, String> map, Function<Map<Integer, String>, M3PassiveExpiringOracle.MapCursor> cursors) {
            this.map = map;
            this.cursors = cursors;
        }

        Object captureViews() {
            liveEntries = map.entrySet();
            liveKeys = map.keySet();
            liveValues = map.values();
            return null;
        }

        M3PassiveExpiringOracle.MapCursor cursor() {
            return cursors.apply(map);
        }
    }

    private static Subject oracleSubject(Variant v, long ttl, long unitTtl, TimeUnit unit, Map<Integer, String> init) {
        Map<Integer, String> map = switch (v) {
            case DEFAULT -> ORACLE.create();
            case TTL -> ORACLE.create(ttl);
            case TTL_UNIT -> ORACLE.create(unitTtl, unit);
            case TTL_MAP -> ORACLE.create(ttl, new HashMap<>(init));
            case TTL_UNIT_MAP -> ORACLE.create(unitTtl, unit, new HashMap<>(init));
            case POLICY -> ORACLE.createWithPolicy(ORACLE.isolated(POLICY, new Class<?>[0]));
            case POLICY_MAP -> ORACLE.createWithPolicy(ORACLE.isolated(POLICY, new Class<?>[0]), new HashMap<>(init));
            case MAP -> ORACLE.create(new HashMap<>(init));
        };
        return new Subject(map, ORACLE::cursor);
    }

    private static Subject m3Subject(Variant v, long ttl, long unitTtl, TimeUnit unit, Map<Integer, String> init) {
        LongSupplier clock = M3PassiveExpiringTestClock::currentTimeMillis;
        long unitMillis = M3PassiveExpiringMap.toMillis(unitTtl, unit);
        M3PassiveExpiringMap<Integer, String> map = switch (v) {
            case DEFAULT -> new M3PassiveExpiringMap<>(null, -1L, Map.of(), clock);
            case TTL -> new M3PassiveExpiringMap<>(null, ttl, Map.of(), clock);
            case TTL_UNIT -> new M3PassiveExpiringMap<>(null, unitMillis, Map.of(), clock);
            case TTL_MAP -> new M3PassiveExpiringMap<>(null, ttl, new HashMap<>(init), clock);
            case TTL_UNIT_MAP -> new M3PassiveExpiringMap<>(null, unitMillis, new HashMap<>(init), clock);
            case POLICY -> new M3PassiveExpiringMap<>(new M3PassiveExpiringTestPolicy(), 0L, Map.of(), clock);
            case POLICY_MAP -> new M3PassiveExpiringMap<>(new M3PassiveExpiringTestPolicy(), 0L, new HashMap<>(init),
                    clock);
            case MAP -> new M3PassiveExpiringMap<>(null, -1L, new HashMap<>(init), clock);
        };
        return new Subject(map, m -> M3PassiveExpiringOracle.cursor(((M3PassiveExpiringMap<Integer, String>) m)
                .mapIterator()));
    }

    // ------------------------------------------------------------------ operations

    private static Integer key(Random r) {
        return r.nextInt(20) == 0 ? null : r.nextInt(48) - 6;
    }

    private static String value(Random r) {
        return r.nextInt(16) == 0 ? null : "v" + r.nextInt(24);
    }

    private static Object apply(Subject s, int op, long opSeed) {
        Random r = new Random(opSeed);
        Map<Integer, String> m = s.map;
        Integer k = key(r);
        String v = value(r);
        switch (op) {
            case 0, 1, 2, 3, 4: return m.put(k, v);
            case 5, 6, 7: return m.get(k);
            case 8, 9: return m.containsKey(k);
            case 10: return m.containsValue(v);
            case 11, 12: return m.remove(k);
            case 13: return m.size();
            case 14: return m.isEmpty();
            case 15: m.putAll(batch(r)); return "putAll";
            case 16: if (r.nextInt(30) == 0) { m.clear(); return "cleared"; } return m.size();
            case 17: return entries(m.entrySet());
            case 18: return sorted(m.keySet());
            case 19: return sorted(m.values());
            case 20: return walk(s.cursor(), r);
            case 21: return removeThroughCursor(s.cursor(), r);
            case 22: return removeThroughIterator(m.entrySet().iterator(), r);
            case 23: return s.liveKeys.remove(k);
            case 24: return s.liveValues.removeIf(x -> Objects.equals(x, v));
            case 25: return List.of(s.liveEntries.contains(new SimpleEntry<>(k, v)),
                    s.liveEntries.remove(new SimpleEntry<>(k, v)));
            case 26: return s.liveKeys.retainAll(keepSet(r));
            case 27: return m.getOrDefault(k, "dflt");
            case 28: return m.putIfAbsent(k, v);
            case 29: return m.remove(k, v);
            case 30: return m.replace(k, v);
            case 31: return m.replace(k, value(r), v);
            case 32: return m.computeIfAbsent(k, x -> x == null || x % 4 == 0 ? null : "c" + x);
            case 33: return m.computeIfPresent(k, (x, old) -> old.length() % 3 == 0 ? null : old + "+");
            case 34: return m.compute(k, (x, old) -> old == null ? (x == null || x % 5 == 0 ? null : "n" + x)
                    : old.length() % 4 == 0 ? null : old + "*");
            case 35: return m.merge(k, v == null ? "m" : v, (a, b) -> a.length() + b.length() > 9 ? null : a + b);
            case 36: return forEachPairs(m);
            case 37: m.replaceAll((a, b) -> b == null ? (a == null ? null : "z" + a) : b.length() > 7 ? "r" : b + "r");
                return "replaceAll";
            case 38: return equalities(s, r);
            case 39: return List.of(s.liveKeys.size(), s.liveEntries.isEmpty(), s.liveKeys.contains(k),
                    s.liveValues.contains(v), sorted(Arrays.asList(s.liveValues.toArray())));
            case 40: return failFast(s, r, k, v);
            case 41: return removeUniqueValue(s, v);
            case 42: return viewClear(s, r);
            case 43: return nullArguments(m, r, k);
            case 44: return roundTrip(s);
            default: throw new AssertionError(op);
        }
    }

    /** M3 only: serialize, deserialize, re-serialize; the oracle side has nothing to check and returns the same text. */
    private static Object roundTrip(Subject s) {
        if (!(s.map instanceof M3PassiveExpiringMap<Integer, String> own)) {
            return "round trip";
        }
        byte[] bytes = serialize(own);
        @SuppressWarnings("unchecked")
        M3PassiveExpiringMap<Integer, String> copy = (M3PassiveExpiringMap<Integer, String>) deserialize(bytes);
        copy.verifyInvariants();
        assertArrayEquals(bytes, serialize(copy), "re-serialized bytes");
        assertEquals(own.hashCode(), copy.hashCode(), "copy hashCode");
        assertEquals(own.toString(), copy.toString(), "copy toString in slot order");
        assertTrue(copy.equals(raw(s)), "copy mappings");
        ROUND_TRIPS.incrementAndGet();
        return "round trip";
    }

    private static byte[] serialize(Object o) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] b) {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(b))) {
            return in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<Integer, String> batch(Random r) {
        Map<Integer, String> b = new LinkedHashMap<>();
        for (int i = r.nextInt(5); i > 0; i--) {
            b.put(key(r), value(r));
        }
        return b;
    }

    private static Set<Integer> keepSet(Random r) {
        Set<Integer> keep = new HashSet<>();
        for (int i = 10 + r.nextInt(30); i > 0; i--) {
            keep.add(key(r));
        }
        return keep;
    }

    private static List<String> entries(Collection<Map.Entry<Integer, String>> c) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<Integer, String> e : c) {
            out.add(e.getKey() + "=" + e.getValue());
        }
        Collections.sort(out);
        return out;
    }

    private static List<String> sorted(Collection<?> c) {
        List<String> out = new ArrayList<>();
        for (Object o : c) {
            out.add(String.valueOf(o));
        }
        Collections.sort(out);
        return out;
    }

    private static boolean pick(Object key, int mod) {
        return key != null && Math.floorMod((Integer) key, mod) == 0;
    }

    private static Object walk(M3PassiveExpiringOracle.MapCursor c, Random r) {
        int mod = 2 + r.nextInt(3);
        List<String> seen = new ArrayList<>();
        while (c.hasNext()) {
            Object key = c.next();
            Object value = c.getValue();
            if (pick(key, mod)) {
                Object old = c.setValue(value == null ? "s" : value + "s");
                seen.add(c.getKey() + ":" + old + "->" + c.getValue());
            } else {
                seen.add(key + "=" + value);
            }
        }
        Collections.sort(seen);
        String afterEnd = outcome(c::next).toString();
        c.reset();
        int count = 0;
        while (c.hasNext()) {
            c.next();
            count++;
        }
        return List.of(seen, afterEnd, count);
    }

    private static Object removeThroughCursor(M3PassiveExpiringOracle.MapCursor c, Random r) {
        int mod = 2 + r.nextInt(4);
        Object before = outcome(c::getKey);
        Object removeBefore = outcome(() -> {
            c.remove();
            return "removed";
        });
        List<String> removed = new ArrayList<>();
        Set<Object> after = new HashSet<>();
        while (c.hasNext()) {
            Object key = c.next();
            if (pick(key, mod)) {
                c.remove();
                removed.add(String.valueOf(key));
                after.add(outcome(c::getKey));
            }
        }
        Collections.sort(removed);
        return List.of(before, removeBefore, removed, after);
    }

    private static Object removeThroughIterator(Iterator<Map.Entry<Integer, String>> it, Random r) {
        int mod = 2 + r.nextInt(4);
        Object removeBefore = outcome(() -> {
            it.remove();
            return "removed";
        });
        List<String> removed = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (pick(e.getKey(), mod)) {
                removed.add(e.toString());
                it.remove();
            }
        }
        Collections.sort(removed);
        return List.of(removeBefore, removed);
    }

    private static List<String> forEachPairs(Map<Integer, String> m) {
        List<String> seen = new ArrayList<>();
        m.forEach((a, b) -> seen.add(a + "=" + b));
        Collections.sort(seen);
        return seen;
    }

    private static Object equalities(Subject s, Random r) {
        Map<Integer, String> snapshot = raw(s);
        if (r.nextBoolean()) {
            snapshot.put(key(r), value(r));
        }
        return List.of(s.map.equals(snapshot), s.map.equals(s.map), s.map.equals("x"), s.liveEntries.equals(
                snapshot.entrySet()), s.liveKeys.equals(snapshot.keySet()));
    }

    private static Object failFast(Subject s, Random r, Integer k, String v) {
        Iterator<Map.Entry<Integer, String>> it = s.liveEntries.iterator();
        boolean had = it.hasNext();
        if (had) {
            it.next();
        }
        switch (r.nextInt(5)) {
            case 0 -> s.map.put(k, v);
            case 1 -> s.map.remove(k);
            case 2 -> s.map.size();
            case 3 -> s.map.get(k);
            default -> s.map.containsValue(v);
        }
        boolean hasNextAfter = it.hasNext();
        Object next = outcome(() -> {
            it.next();
            return "next";
        });
        Object remove = "next".equals(next) ? "skipped" : outcome(() -> {
            it.remove();
            return "removed";
        });
        return List.of(had, hasNextAfter, next, remove);
    }

    private static Object removeUniqueValue(Subject s, String v) {
        long copies = s.liveValues.stream().filter(x -> Objects.equals(x, v)).count();
        return copies > 1 ? "skipped" : s.liveValues.remove(v);
    }

    private static Object viewClear(Subject s, Random r) {
        if (r.nextInt(25) != 0) {
            return s.liveKeys.size();
        }
        switch (r.nextInt(3)) {
            case 0 -> s.liveKeys.clear();
            case 1 -> s.liveValues.clear();
            default -> s.liveEntries.clear();
        }
        return "view cleared";
    }

    private static Object nullArguments(Map<Integer, String> m, Random r, Integer k) {
        return switch (r.nextInt(6)) {
            case 0 -> outcome(() -> {
                m.forEach(null);
                return "ok";
            });
            case 1 -> outcome(() -> {
                m.replaceAll(null);
                return "ok";
            });
            case 2 -> outcome(() -> m.computeIfAbsent(k, null));
            case 3 -> outcome(() -> m.merge(k, null, (a, b) -> a));
            case 4 -> outcome(() -> {
                m.putAll(null);
                return "ok";
            });
            default -> outcome(() -> m.entrySet().removeIf(null));
        };
    }

    // ------------------------------------------------------------------ comparison

    private static Object outcome(Supplier<?> action) {
        try {
            Object o = action.get();
            return o == null ? "null" : o;
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getName();
        }
    }

    private static void same(String what, Supplier<?> oracle, Supplier<?> m3) {
        long t0 = M3PassiveExpiringTestClock.peek();
        Object expected = outcome(oracle);
        long t1 = M3PassiveExpiringTestClock.peek();
        M3PassiveExpiringTestClock.rewind(t0);
        Object actual = outcome(m3);
        assertEquals(expected, actual, what);
        assertEquals(t1, M3PassiveExpiringTestClock.peek(), what + ": clock reads differ");
        CHECKS.addAndGet(2);
    }

    /** Raw mappings through the live entry view: no expiry, no clock read. */
    private static Map<Integer, String> raw(Subject s) {
        Map<Integer, String> out = new HashMap<>();
        for (Map.Entry<Integer, String> e : s.liveEntries) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    private static List<String> tokens(String text) {
        String body = text.substring(1, text.length() - 1);
        List<String> out = body.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(body.split(", ")));
        Collections.sort(out);
        return out;
    }

    private static void compareState(Subject oracle, Subject m3, int run, int step) {
        String at = "run " + run + " step " + step;
        long clock = M3PassiveExpiringTestClock.peek();
        ((M3PassiveExpiringMap<Integer, String>) m3.map).verifyInvariants();
        Map<Integer, String> expected = raw(oracle);
        assertEquals(expected, raw(m3), "raw mappings@" + at);
        assertEquals(oracle.liveEntries.size(), m3.liveEntries.size(), "raw size@" + at);
        assertEquals(oracle.map.hashCode(), m3.map.hashCode(), "hashCode@" + at);
        assertEquals(tokens(oracle.map.toString()), tokens(m3.map.toString()), "toString@" + at);
        assertTrue(oracle.map.equals(expected) && m3.map.equals(expected), "equals snapshot@" + at);
        assertEquals(entries(oracle.liveEntries), entries(m3.liveEntries), "entry view@" + at);
        assertEquals(sorted(oracle.liveValues), sorted(m3.liveValues), "value view@" + at);
        assertEquals(clock, M3PassiveExpiringTestClock.peek(), "state comparison read the clock@" + at);
        CHECKS.addAndGet(9);
    }
}
