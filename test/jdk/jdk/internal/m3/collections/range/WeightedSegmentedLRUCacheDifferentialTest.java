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
 *******************************************************************************/

/*
 * @test
 * @summary WeightedSegmentedLRUCache on array lanes behaves as the node-linked version it
 *          replaced: every result, both segments' order, the counters, the admission victims
 *          and the exceptions, on random operations, with keys whose hashCode throws, keys whose
 *          equals or hashCode and callbacks that change the cache, the same key object put back;
 *          same hashCode calls per lookup; same non-private members; removed values not retained;
 *          emptied lanes dropped; consistent lanes after a weight overflow
 * @modules jdk.m3.collections/jdk.internal.m3.collections.range:+open
 * @run main/othervm -Xmx512m WeightedSegmentedLRUCacheDifferentialTest
 */

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.internal.m3.collections.range.WeightedSegmentedLRUCache;

public class WeightedSegmentedLRUCacheDifferentialTest {

    static final String NAME = WeightedSegmentedLRUCache.class.getName();
    static final Method VICTIMS = victimsMethod();
    static final Field KEY_LANE = lane("keyLane");
    static final Field WEIGHT_LANE = lane("weightLane");
    static final Field SEGMENT_LANE = lane("segmentLane");
    static final Field SEGMENT_WEIGHT = lane("segmentWeight");
    static long checks;
    static String lastOp = "";
    /** Fresh caches after an overflow, a broken node walk or a corrupted reference. */
    static long restarts;
    /** The callback a weigher, an equals or a hashCode runs once, per side (0 reference). */
    static final Runnable[] WEIGHER_HOOK = {null, null};
    static final Runnable[] EQUALS_HOOK = {null, null};
    static final Runnable[] HASH_HOOK = {null, null};
    /** The key whose equals or hashCode runs the hook, per side. */
    static final Key[] HOOK_KEY = {null, null};
    /** The side running now. */
    static int side;
    /** The outcome of a step after which the two sides may differ by design: fresh caches. */
    static final String DIVERGED = "diverged";
    static final String EQUALS_CME = "threw java.util.ConcurrentModificationException: "
            + "a key's equals changed the cache";
    static final String WALK_CME = "threw java.util.ConcurrentModificationException: "
            + "the removeIf predicate removed a resident under the walk";

    static void same(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
            throw new AssertionError(what + ": reference " + expected + " array lanes " + actual);
        }
    }

    /** Compares the outcomes of an operation; returns the reference's outcome. */
    static String op(String what, Supplier<Object> reference, Supplier<Object> lanes) {
        lastOp = what;
        String expected = outcome(0, reference);
        same(expected, outcome(1, lanes), what);
        return expected;
    }

    /** As op, and both sides call hashCode the same number of times. */
    static String opCounted(String what, Supplier<Object> reference, Supplier<Object> lanes) {
        long before = Key.hashCalls;
        String expected = outcome(0, reference);
        long referenceCalls = Key.hashCalls - before;
        before = Key.hashCalls;
        String actual = outcome(1, lanes);
        same(expected + " / hashCode calls " + referenceCalls,
                actual + " / hashCode calls " + (Key.hashCalls - before), what);
        return expected;
    }

    /**
     * As op for a step whose callbacks change the cache: equal outcomes, or the lanes throw the
     * documented ConcurrentModificationException where the reference threw (a broken node walk),
     * or, for a key's equals that changed the cache, whatever the reference did.
     */
    static String opMutating(String what, Supplier<Object> reference, Supplier<Object> lanes) {
        lastOp = what;
        String expected = outcome(0, reference);
        String actual = outcome(1, lanes);
        if (!expected.equals(actual)
                && (actual.equals(EQUALS_CME) || expected.startsWith("threw ")
                        && actual.startsWith("threw java.util.ConcurrentModificationException"))) {
            checks++;
            return DIVERGED;
        }
        same(expected, actual, what);
        return expected;
    }

    /** Runs both sides; a difference is allowed and starts fresh caches. */
    static String opLoose(Supplier<Object> reference, Supplier<Object> lanes) {
        String expected = outcome(0, reference);
        checks++;
        return expected.equals(outcome(1, lanes)) ? expected : DIVERGED;
    }

    /** The outcome of an operation on a side: its value or the class and message it threw. */
    static String outcome(int on, Supplier<Object> operation) {
        side = on;
        try {
            return "value " + operation.get();
        } catch (RuntimeException e) {
            return "threw " + e.getClass().getName() + ": " + e.getMessage();
        } finally {
            WEIGHER_HOOK[on] = null;
            EQUALS_HOOK[on] = null;
            HASH_HOOK[on] = null;
        }
    }

    /** A key with a chosen hash; a boom key's hashCode throws; hooks run in equals or hashCode. */
    record Key(int id, int hash, boolean boom) {
        /** hashCode calls of every key. */
        static long hashCalls;

        @Override
        public int hashCode() {
            hashCalls++;
            Runnable h = HASH_HOOK[side];
            if (h != null && this == HOOK_KEY[side]) {
                HASH_HOOK[side] = null;
                h.run();
            }
            if (boom) {
                throw new UnsupportedOperationException("hashCode refuses " + id);
            }
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            Runnable h = EQUALS_HOOK[side];
            if (h != null && this == HOOK_KEY[side] && other instanceof Key) {
                EQUALS_HOOK[side] = null;
                h.run();
            }
            return other instanceof Key k && k.id == id;
        }
    }

    /** What a callback does to its own side's cache. */
    record Actions(Function<Key, Object> remove, BiFunction<Key, String, Object> put,
            Function<Key, Object> get, Runnable clear, Runnable trimToEmpty,
            Runnable removeIfAll) {
        static final int KINDS = 11;

        /**
         * 0 nothing, 1 remove the key the callback got, 2 remove, 3 put, 4 get another key,
         * 5 remove the key the callback got and put another, 6 clear, 7 trim to empty,
         * 8 removeIf all, 9 remove the key the callback got and put the same object back,
         * 10 remove another key and put the same object back.
         */
        void apply(int kind, Key given, Key other, String value) {
            switch (kind) {
                case 1 -> remove.apply(given);
                case 2 -> remove.apply(other);
                case 3 -> put.apply(other, value);
                case 4 -> get.apply(other);
                case 5 -> {
                    remove.apply(given);
                    put.apply(other, value);
                }
                case 6 -> clear.run();
                case 7 -> trimToEmpty.run();
                case 8 -> removeIfAll.run();
                case 9 -> {
                    remove.apply(given);
                    if (given != null && !given.boom()) {
                        put.apply(given, value);
                    }
                }
                case 10 -> {
                    remove.apply(other);
                    put.apply(other, value);
                }
                default -> { }
            }
        }
    }

    /** The test weigher: runs its side's hook once, refuses "boom" values, else the length. */
    static SegmentedLruReference.Weigher<Key, String> weigher(long scale, int on) {
        return (k, v) -> {
            Runnable h = WEIGHER_HOOK[on];
            if (h != null) {
                WEIGHER_HOOK[on] = null;
                h.run();
            }
            if (v.startsWith("boom")) {
                throw new IllegalStateException("weigher refuses " + v);
            }
            return (v.length() - 1) * scale;
        };
    }

    static Method victimsMethod() {
        try {
            Method m = WeightedSegmentedLRUCache.class.getDeclaredMethod(
                    "admissionVictims", long.class, int.class, long.class);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    static Field lane(String name) {
        try {
            Field f = WeightedSegmentedLRUCache.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    /** The length of the lanes: 0 once an emptied cache dropped them. */
    static int lanes(WeightedSegmentedLRUCache<?, ?> cache) {
        try {
            return ((Object[]) KEY_LANE.get(cache)).length;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /** The package-private admissionVictims(long, int, long) of the array-lane class. */
    static Object victims(WeightedSegmentedLRUCache<Key, String> cache, long w, int e, long l) {
        try {
            return VICTIMS.invoke(cache, w, e, l);
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw new AssertionError(x);
        } catch (IllegalAccessException x) {
            throw new AssertionError(x);
        }
    }

    public static void main(String[] args) {
        members();
        constructors();
        double[] fractions = {0.01d, 0.25d, 0.5d, 0.75d, 0.99d};
        for (long seed = 1; seed <= 15; seed++) {
            int maxEntries = 1 + (int) (seed % 7) * 9;
            long maxWeight = seed % 3 == 0 ? 50L : 2_000L;
            run(seed, 40_000, maxEntries, maxWeight, fractions[(int) seed % 5], (int) seed % 4, 1L,
                    1);
        }
        run(99L, 200_000, 4_000, 1_000_000L, 0.8d, 0, 1L, 1);
        run(100L, 100_000, 1, 10L, 0.5d, 1, 1L, 1);
        run(101L, 100_000, 2, 10L, 0.5d, 3, 1L, 1);
        run(102L, 30_000, 70_000, Long.MAX_VALUE / 4, 0.8d, 0, 1L, 997);
        overflowScenarios();
        for (long seed = 200; seed < 260; seed++) {
            run(seed, 2_000, 6, Long.MAX_VALUE, 0.5d, 0, Long.MAX_VALUE / 64, 1);
        }
        System.out.println("WeightedSegmentedLRUCacheDifferentialTest: " + checks
                + " checks passed, " + restarts + " restarts");
    }

    /**
     * Weight totals near Long.MAX_VALUE, one unit = Long.MAX_VALUE / 64: a replacement, a new entry
     * and a trim whose total overflows throw ArithmeticException on both sides, and the lanes stay
     * consistent.
     */
    static void overflowScenarios() {
        long unit = Long.MAX_VALUE / 64;
        String[][] scenarios = {
            {"put k1 32", "put k2 31", "put k1 40"},
            {"put k1 40", "put k2 30"},
            {"put k1 30", "get k1", "put k2 20", "put k3 20"},
            {"put k1 30", "get k1", "put k2 20", "put k1 50"},
        };
        for (String[] scenario : scenarios) {
            WeightedSegmentedLRUCache<Key, String> cache = new WeightedSegmentedLRUCache<>(
                    8, Long.MAX_VALUE, 0.5d, weigher(unit, 1)::weightOf);
            SegmentedLruReference<Key, String> ref =
                    new SegmentedLruReference<>(8, Long.MAX_VALUE, 0.5d, weigher(unit, 0));
            for (String action : scenario) {
                String[] part = action.split(" ");
                Key key = new Key(part[1].hashCode(), part[1].hashCode(), false);
                String at = String.join(", ", scenario) + ": " + action;
                String outcome = part[0].equals("get")
                        ? op(at, () -> ref.get(key), () -> cache.get(key))
                        : op(at, () -> ref.put(key, "x".repeat(Integer.parseInt(part[2]) + 1)),
                                () -> cache.put(key, "x".repeat(Integer.parseInt(part[2]) + 1)));
                if (outcome.startsWith("threw java.lang.ArithmeticException")) {
                    sound(at, cache);
                    break;
                }
            }
            same(true, outcome(1, cache::size).startsWith("value "), "scenario ran");
        }
    }

    /** The non-private constructors, methods and nested types are the same as the reference's. */
    static void members() {
        same(signatures(SegmentedLruReference.class), signatures(WeightedSegmentedLRUCache.class),
                "non-private members");
    }

    static TreeSet<String> signatures(Class<?> type) {
        TreeSet<String> all = new TreeSet<>();
        List<Executable> executables = new ArrayList<>(List.of(type.getDeclaredMethods()));
        executables.addAll(List.<Constructor<?>>of(type.getDeclaredConstructors()));
        executables.stream()
                .filter(e -> !Modifier.isPrivate(e.getModifiers()) && !e.isSynthetic())
                .forEach(e -> all.add(e.toGenericString()
                        .replace(NAME, "SegmentedLruReference")));
        for (Class<?> nested : type.getDeclaredClasses()) {
            if (!Modifier.isPrivate(nested.getModifiers())) {
                all.add(Modifier.toString(nested.getModifiers()) + " "
                        + nested.getName().replace(NAME, "SegmentedLruReference"));
            }
        }
        return all;
    }

    /** Constructor arguments accepted and refused alike, with the same derived targets. */
    static void constructors() {
        SegmentedLruReference.Weigher<Key, String> one = (k, v) -> 1L;
        WeightedSegmentedLRUCache.Weigher<Key, String> lanesOne = one::weightOf;
        Object[][] cases = {
            {0, 10L, 0.5d}, {-1, 10L, 0.5d}, {5, 0L, 0.5d}, {5, -3L, 0.5d},
            {5, 10L, 0.0d}, {5, 10L, 1.0d}, {5, 10L, -0.0d}, {5, 10L, Double.NaN},
            {5, 10L, Double.POSITIVE_INFINITY}, {5, 10L, 1e-300d}, {5, 10L, 0.9999999d},
            {1, 1L, 0.5d}, {2, 1L, 0.01d}, {Integer.MAX_VALUE, Long.MAX_VALUE, 0.5d},
            {Integer.MAX_VALUE, Long.MAX_VALUE, 0.9999999999d}, {3, 7L, 0.34d},
        };
        for (Object[] c : cases) {
            int entries = (Integer) c[0];
            long weight = (Long) c[1];
            double fraction = (Double) c[2];
            op("constructor " + entries + "/" + weight + "/" + fraction,
                    () -> new SegmentedLruReference<>(entries, weight, fraction, one).snapshot(),
                    () -> new WeightedSegmentedLRUCache<>(entries, weight, fraction, lanesOne)
                            .snapshot());
        }
        op("constructor without weigher",
                () -> new SegmentedLruReference<Key, String>(5, 10L, 0.5d, null),
                () -> new WeightedSegmentedLRUCache<Key, String>(5, 10L, 0.5d, null));
    }

    static void run(long seed, int steps, int maxEntries, long maxWeight, double fraction,
            int hashKind, long scale, int orderEvery) {
        Random random = new Random(seed);
        for (int step = 0; step < steps; ) {
            step = epoch(random, seed, step, steps, maxEntries, maxWeight, fraction, hashKind,
                    scale, orderEvery);
        }
    }

    /** Fresh caches from step first on, until the steps are done or the two may differ. */
    static int epoch(Random random, long seed, int first, int steps, int maxEntries,
            long maxWeight, double fraction, int hashKind, long scale, int orderEvery) {
        int keySpace = Math.max(4, maxEntries * 3);
        WeightedSegmentedLRUCache<Key, String> cache = new WeightedSegmentedLRUCache<>(
                maxEntries, maxWeight, fraction, weigher(scale, 1)::weightOf);
        SegmentedLruReference<Key, String> ref =
                new SegmentedLruReference<>(maxEntries, maxWeight, fraction, weigher(scale, 0));
        Actions refActions = new Actions(ref::remove, ref::put, ref::get, ref::clear,
                () -> ref.trimTo(0, 0L), () -> ref.removeIf(k -> true));
        Actions laneActions = new Actions(cache::remove, cache::put, cache::get, cache::clear,
                () -> cache.trimTo(0, 0L), () -> cache.removeIf(k -> true));
        boolean big = orderEvery > 1;
        if (big) {
            // a large cache: fill it past maxEntries first (several doublings of the lanes)
            for (int i = 0; i <= maxEntries; i++) {
                Key k = new Key(i, Integer.hashCode(i), false);
                String v = i % 7 == 0 ? "v" + i : "ww";
                same(ref.put(k, v), cache.put(k, v), "fill " + i);
                if (i % 5 == 0) {
                    same(ref.get(k), cache.get(k), "fill get " + i);
                }
            }
            same(ref.keysInLruOrder(), cache.keysInLruOrder(), "order after the fill");
        }
        for (int step = first; step < steps; step++) {
            int id = random.nextInt(keySpace);
            int hash = switch (hashKind) {
                case 1 -> id & 3;
                case 2 -> id << 16;
                case 3 -> 0;
                default -> Integer.hashCode(id);
            };
            Key key = random.nextInt(50) == 0 ? null : new Key(id, hash, random.nextInt(40) == 0);
            Key other = new Key(random.nextInt(keySpace), hash, false);
            String value = value(random);
            String at = "seed " + seed + " step " + step;
            String outcome = !big && random.nextInt(9) == 0
                    ? mutating(random, at, key, other, value, ref, cache, refActions, laneActions)
                    : step(random, at, key, value, maxEntries, maxWeight, scale, ref, cache, big);
            if (outcome.startsWith("threw java.lang.ArithmeticException")) {
                // the overflow was compared; the lanes stay consistent, the reference may not
                sound(at, cache);
                restarts++;
                return step + 1;
            }
            if (outcome.equals(DIVERGED)) {
                restarts++;
                return step + 1;
            }
            if (step % orderEvery == 0) {
                op(at + " order", ref::keysInLruOrder, cache::keysInLruOrder);
            }
            op(at + " snapshot", ref::snapshot, cache::snapshot);
            op(at + " size/weight", () -> ref.size() + "/" + ref.weight(),
                    () -> cache.size() + "/" + cache.weight());
            same(true, cache.size() > 0 || lanes(cache) == 0,
                    at + " an emptied cache drops its lanes");
            if (scale == 1L && random.nextInt(4_000) == 0) {
                retention(random, at, maxEntries, ref, cache);
            }
        }
        return steps;
    }

    /**
     * The lanes after an overflow: a valid snapshot, every resident on a segment list, and the
     * segment totals equal to the sum of the residents' weights (nothing half admitted or moved).
     */
    static void sound(String at, WeightedSegmentedLRUCache<Key, String> cache) {
        String snapshot = outcome(1, cache::snapshot);
        same(true, snapshot.startsWith("value Snapshot[") || snapshot.startsWith(
                "threw java.lang.ArithmeticException"), at + " lanes snapshot after the overflow");
        same(cache.size(), cache.keysInLruOrder().size(), at + " lanes order after the overflow");
        try {
            Object[] keys = (Object[]) KEY_LANE.get(cache);
            long[] weights = (long[]) WEIGHT_LANE.get(cache);
            byte[] segments = (byte[]) SEGMENT_LANE.get(cache);
            long[] totals = (long[]) SEGMENT_WEIGHT.get(cache);
            java.math.BigInteger[] sums = {java.math.BigInteger.ZERO, java.math.BigInteger.ZERO};
            for (int slot = 0; slot < keys.length; slot++) {
                if (keys[slot] != null) {
                    sums[segments[slot]] =
                            sums[segments[slot]].add(java.math.BigInteger.valueOf(weights[slot]));
                }
            }
            same(totals[0] + "/" + totals[1], sums[0] + "/" + sums[1],
                    at + " lanes segment weights after the overflow (" + lastOp + ")");
        } catch (ReflectiveOperationException | ArithmeticException e) {
            throw new AssertionError(at + " lanes after the overflow", e);
        }
    }

    static String step(Random random, String at, Key key, String value, int maxEntries,
            long maxWeight, long scale, SegmentedLruReference<Key, String> ref,
            WeightedSegmentedLRUCache<Key, String> cache, boolean big) {
        int what = random.nextInt(15);
        if (big && (what == 11 || what == 12 || what == 13)) {
            what = 3; // a large cache: no O(n) removeIf or clear per step
        }
        switch (what) {
            case 0, 1, 2:
                return op(at + " put", () -> ref.put(key, value), () -> cache.put(key, value));
            case 3, 4, 5:
                return opCounted(at + " get", () -> ref.get(key), () -> cache.get(key));
            case 6:
                return opCounted(at + " remove", () -> ref.remove(key), () -> cache.remove(key));
            case 7:
                return opCounted(at + " containsKey",
                        () -> ref.containsKey(key), () -> cache.containsKey(key));
            case 8: {
                long w = (random.nextInt(12) - 1) * scale;
                return op(at + " admissionVictims " + w,
                        () -> ref.admissionVictims(w), () -> cache.admissionVictims(w));
            }
            case 9: {
                long w = (random.nextInt(12) - 1) * scale;
                int e = random.nextInt(maxEntries + 3) - 1;
                long l = random.nextInt(8) == 0 ? maxWeight
                        : (long) (random.nextDouble() * maxWeight) + random.nextInt(3) - 1;
                return op(at + " admissionVictims " + w + "/" + e + "/" + l,
                        () -> ref.admissionVictims(w, e, l), () -> victims(cache, w, e, l));
            }
            case 10: {
                int below = random.nextInt(20) == 0 ? 1 : 0;
                // a large cache trims near its limit (a random limit would evict half each time)
                int entries = big ? maxEntries - random.nextInt(64)
                        : random.nextInt(maxEntries + 2) - below;
                long weights = random.nextInt(8) == 0 ? maxWeight
                        : (long) (random.nextDouble() * maxWeight) + random.nextInt(3) - 1;
                return op(at + " trimTo " + entries + "/" + weights,
                        () -> ref.trimTo(entries, weights), () -> cache.trimTo(entries, weights));
            }
            case 11: {
                int modulus = 2 + random.nextInt(6);
                List<Object> refCalls = new ArrayList<>();
                List<Object> calls = new ArrayList<>();
                Predicate<Key> refTest = k -> refCalls.add(k) && k.id % modulus == 0;
                Predicate<Key> test = k -> calls.add(k) && k.id % modulus == 0;
                String outcome = op(at + " removeIf",
                        () -> ref.removeIf(refTest), () -> cache.removeIf(test));
                same(refCalls, calls, at + " removeIf call order");
                return outcome;
            }
            case 12:
                if (random.nextInt(200) == 0) {
                    return op(at + " clear", ref::clear, cache::clear);
                }
                return "skipped";
            case 13:
                return op(at + " removeIf throws",
                        () -> ref.removeIf(k -> { throw new IllegalStateException("p"); }),
                        () -> cache.removeIf(k -> { throw new IllegalStateException("p"); }));
            default:
                return op(at + " put refused by the weigher",
                        () -> ref.put(key, "boom" + value), () -> cache.put(key, "boom" + value));
        }
    }

    /**
     * A put whose weigher, a removeIf whose predicate, or a lookup whose key's equals or hashCode
     * changes the cache first.
     */
    static String mutating(Random random, String at, Key key, Key other, String value,
            SegmentedLruReference<Key, String> ref, WeightedSegmentedLRUCache<Key, String> cache,
            Actions refActions, Actions laneActions) {
        int what = random.nextInt(4);
        int kind = 1 + random.nextInt(Actions.KINDS - 1);
        String inner = value(random);
        if (what == 0) {
            WEIGHER_HOOK[0] = () -> refActions.apply(kind, key, other, inner);
            WEIGHER_HOOK[1] = () -> laneActions.apply(kind, key, other, inner);
            String outcome = opMutating(at + " put, weigher " + kind + " " + other,
                    () -> ref.put(key, value), () -> cache.put(key, value));
            return outcome.equals(DIVERGED) || quiet(ref, cache) || consistent(ref)
                    ? outcome : DIVERGED;
        }
        if (what == 1) {
            return removeIfMutating(random, at, other, ref, cache, refActions, laneActions);
        }
        // a lookup whose key's equals (what 2) or hashCode (what 3) changes the cache first
        Key probe = key == null ? other : new Key(key.id(), key.hash(), false);
        Runnable[] hooks = what == 2 ? EQUALS_HOOK : HASH_HOOK;
        hooks[0] = () -> refActions.apply(kind, probe, other, inner);
        hooks[1] = () -> laneActions.apply(kind, probe, other, inner);
        HOOK_KEY[0] = probe;
        HOOK_KEY[1] = probe;
        String name = at + (what == 2 ? " equals " : " hashCode ") + kind + " " + other;
        // HashMap.get and containsKey read their table before they call hashCode, so a hashCode
        // that changes the cache makes the node version look in the old table; the lanes look in
        // the changed cache (put starts with get). Those may differ; remove hashes first in both.
        boolean loose = what == 3;
        String outcome = switch (random.nextInt(4)) {
            case 0 -> loose ? opLoose(() -> ref.get(probe), () -> cache.get(probe))
                    : opMutating(name + " get", () -> ref.get(probe), () -> cache.get(probe));
            case 1 -> opMutating(name + " remove", () -> ref.remove(probe),
                    () -> cache.remove(probe));
            case 2 -> loose ? opLoose(() -> ref.containsKey(probe), () -> cache.containsKey(probe))
                    : opMutating(name + " containsKey", () -> ref.containsKey(probe),
                            () -> cache.containsKey(probe));
            default -> loose ? opLoose(() -> ref.put(probe, value), () -> cache.put(probe, value))
                    : opMutating(name + " put", () -> ref.put(probe, value),
                            () -> cache.put(probe, value));
        };
        if (outcome.equals(DIVERGED)) {
            return outcome;
        }
        // a node-version lookup that found a node the callback removed may leave it corrupted
        return quiet(ref, cache) || consistent(ref) ? outcome : DIVERGED;
    }

    /** Same size and weight on both sides: no sign of a corrupted reference. */
    static boolean quiet(SegmentedLruReference<Key, String> ref,
            WeightedSegmentedLRUCache<Key, String> cache) {
        try {
            return ref.size() == cache.size() && ref.weight() == cache.weight();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Whether the reference's order holds all its residents and its snapshot is valid. */
    static boolean consistent(SegmentedLruReference<Key, String> ref) {
        try {
            ref.snapshot();
            return ref.keysInLruOrder().size() == ref.size();
        } catch (RuntimeException e) {
            return false;
        }
    }

    static String removeIfMutating(Random random, String at, Key other,
            SegmentedLruReference<Key, String> ref, WeightedSegmentedLRUCache<Key, String> cache,
            Actions refActions, Actions laneActions) {
        int calls = ref.size() + 1;
        int[] kinds = new int[calls];
        Key[] others = new Key[calls];
        String[] values = new String[calls];
        for (int i = 0; i < calls; i++) {
            kinds[i] = random.nextInt(3) == 0 ? 1 + random.nextInt(Actions.KINDS - 1) : 0;
            others[i] = new Key(random.nextInt(Math.max(4, calls * 2)), other.hash(), false);
            values[i] = value(random);
        }
        int modulus = 2 + random.nextInt(4);
        List<Object> refCalls = new ArrayList<>();
        List<Object> laneCalls = new ArrayList<>();
        boolean[] bulk = {false};
        String outcome = outcome(0, () -> ref.removeIf(
                test(refCalls, refActions, kinds, others, values, modulus, bulk)));
        String lanes = outcome(1, () -> cache.removeIf(
                test(laneCalls, laneActions, kinds, others, values, modulus, new boolean[1])));
        if (!outcome.equals(lanes) && lanes.startsWith(WALK_CME)
                && (outcome.startsWith("threw ") || bulk[0])) {
            // documented: the node walk broke, or went on over the nodes a clear, trimTo or
            // nested removeIf had detached; the lanes stop with ConcurrentModificationException
            checks++;
            return DIVERGED;
        }
        same(outcome, lanes, at + " removeIf, mutating");
        same(refCalls, laneCalls, at + " removeIf, mutating, call order");
        // a node walk over detached nodes (after a clear) can leave the reference corrupted
        return quiet(ref, cache) || consistent(ref) ? outcome : DIVERGED;
    }

    /**
     * A predicate that records its calls and runs the planned action of each call; {@code bulk}
     * notes a clear, trim to empty or nested removeIf.
     */
    static Predicate<Key> test(List<Object> calls, Actions actions, int[] kinds, Key[] others,
            String[] values, int modulus, boolean[] bulk) {
        return k -> {
            int i = calls.size();
            calls.add(k);
            if (i < kinds.length) {
                bulk[0] |= kinds[i] >= 6 && kinds[i] <= 8;
                actions.apply(kinds[i], k, others[i], values[i]);
            }
            return k.id % modulus == 0;
        };
    }

    /** A value put into both caches and taken out again is not kept reachable by the lanes. */
    static void retention(Random random, String at, int maxEntries,
            SegmentedLruReference<Key, String> ref, WeightedSegmentedLRUCache<Key, String> cache) {
        Key key = new Key(-1_000_000 - random.nextInt(1_000), 1, false);
        WeakReference<String> held = putBoth(at, key, ref, cache);
        if (held == null) {
            return;
        }
        int how = random.nextInt(5);
        String done = switch (how) {
            case 0 -> op(at + " retention remove", () -> ref.remove(key), () -> cache.remove(key));
            case 1 -> op(at + " retention trimTo", () -> ref.trimTo(0, 0L),
                    () -> cache.trimTo(0, 0L));
            case 2 -> op(at + " retention removeIf", () -> ref.removeIf(key::equals),
                    () -> cache.removeIf(key::equals));
            case 3 -> op(at + " retention clear", ref::clear, cache::clear);
            default -> {
                for (int i = 0; i <= maxEntries; i++) {
                    Key filler = new Key(-2_000_000 - i, -2_000_000 - i, false);
                    ref.put(filler, "ff");
                    cache.put(filler, "ff");
                }
                yield "evicted";
            }
        };
        if (cache.containsKey(key)) {
            return;
        }
        for (int i = 0; i < 50 && held.get() != null; i++) {
            System.gc();
        }
        same(null, held.get(), at + " value kept after " + done);
    }

    /** Puts a fresh value under key into both caches; a weak reference to the lanes' value. */
    static WeakReference<String> putBoth(String at, Key key, SegmentedLruReference<Key, String> ref,
            WeightedSegmentedLRUCache<Key, String> cache) {
        String laneValue = new String("retained value " + at);
        String refValue = new String(laneValue);
        String refPut = outcome(0, () -> ref.put(key, refValue));
        String lanePut = outcome(1, () -> cache.put(key, laneValue));
        same(refPut, lanePut, at + " retention put");
        return lanePut.startsWith("value PutResult[admitted=true")
                ? new WeakReference<>(laneValue) : null;
    }

    static String value(Random random) {
        int spread = random.nextInt(4) == 0 ? 60 : 8;
        int length = random.nextInt(10) == 0 ? 1 : 2 + random.nextInt(spread);
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) ('a' + random.nextInt(26));
        }
        return new String(chars);
    }
}

/** The reference: the node-linked WeightedSegmentedLRUCache of master, renamed only. */
final class SegmentedLruReference<K, V> {

    /** Caller-owned retained-weight function. */
    @FunctionalInterface
    public interface Weigher<K, V> {
        long weightOf(K key, V value);
    }

    /** Result of a pressure/admission trim. */
    public record TrimResult(
            int evictedEntries,
            long evictedWeight,
            int demotedEntries) {

        public TrimResult {
            if (evictedEntries < 0 || evictedWeight < 0L || demotedEntries < 0) {
                throw new IllegalArgumentException("negative segmented LRU trim result");
            }
        }

        public static TrimResult none() {
            return new TrimResult(0, 0L, 0);
        }

        public TrimResult plus(TrimResult other) {
            Objects.requireNonNull(other, "other");
            return new TrimResult(
                    Math.addExact(evictedEntries, other.evictedEntries),
                    Math.addExact(evictedWeight, other.evictedWeight),
                    Math.addExact(demotedEntries, other.demotedEntries));
        }
    }

    /** Result of an admission attempt. */
    public record PutResult(
            boolean admitted,
            boolean replaced,
            long admittedWeight,
            TrimResult trim) {

        public PutResult {
            if (admittedWeight < 0L) {
                throw new IllegalArgumentException("admittedWeight must be >= 0");
            }
            trim = Objects.requireNonNull(trim, "trim");
            if (!admitted && admittedWeight != 0L) {
                throw new IllegalArgumentException(
                        "rejected admission cannot report retained weight");
            }
        }
    }

    /** One current hard-budget victim in exact eviction priority order. */
    public record AdmissionVictim<K>(K key, long weight, boolean protectedResident) {
        public AdmissionVictim {
            Objects.requireNonNull(key, "key");
            if (weight <= 0L) {
                throw new IllegalArgumentException("victim weight must be > 0");
            }
        }
    }

    /** Result of explicit removal. */
    public record RemoveResult(int removedEntries, long removedWeight) {
        public RemoveResult {
            if (removedEntries < 0 || removedWeight < 0L) {
                throw new IllegalArgumentException("negative remove result");
            }
        }
    }

    /** Immutable diagnostics. */
    public record Snapshot(
            int entries,
            int probationEntries,
            int protectedEntries,
            long weight,
            long probationWeight,
            long protectedWeight,
            int maxEntries,
            long maxWeight,
            int protectedEntryTarget,
            long protectedWeightTarget,
            long hits,
            long probationHits,
            long protectedHits,
            long misses,
            long puts,
            long replacements,
            long promotions,
            long demotions,
            long evictions,
            long probationEvictions,
            long protectedEvictions,
            long oversizeRejections) {

        public Snapshot {
            if (entries < 0
                    || probationEntries < 0
                    || protectedEntries < 0
                    || weight < 0L
                    || probationWeight < 0L
                    || protectedWeight < 0L
                    || maxEntries <= 0
                    || maxWeight <= 0L
                    || protectedEntryTarget < 0
                    || protectedWeightTarget < 0L
                    || hits < 0L
                    || probationHits < 0L
                    || protectedHits < 0L
                    || misses < 0L
                    || puts < 0L
                    || replacements < 0L
                    || promotions < 0L
                    || demotions < 0L
                    || evictions < 0L
                    || probationEvictions < 0L
                    || protectedEvictions < 0L
                    || oversizeRejections < 0L) {
                throw new IllegalArgumentException("invalid segmented LRU snapshot");
            }
            if (probationEntries + protectedEntries != entries
                    || probationWeight + protectedWeight != weight
                    || probationHits + protectedHits != hits) {
                throw new IllegalArgumentException("segmented LRU accounting mismatch");
            }
        }
    }

    private enum Segment {
        PROBATION,
        PROTECTED
    }

    private final int maxEntries;
    private final long maxWeight;
    private final int protectedEntryTarget;
    private final long protectedWeightTarget;
    private final Weigher<K, V> weigher;
    private final Map<K, Node<K, V>> nodes;

    private final Node<K, V> probationHead = sentinel();
    private final Node<K, V> probationTail = sentinel();
    private final Node<K, V> protectedHead = sentinel();
    private final Node<K, V> protectedTail = sentinel();

    private int probationEntries;
    private int protectedEntries;
    private long probationWeight;
    private long protectedWeight;

    private long hits;
    private long probationHits;
    private long protectedHits;
    private long misses;
    private long puts;
    private long replacements;
    private long promotions;
    private long demotions;
    private long evictions;
    private long probationEvictions;
    private long protectedEvictions;
    private long oversizeRejections;

    /**
     * Build a segmented LRU.
     *
     * @param protectedFraction fraction of the configured count/weight budget reserved as the
     *     protected target; must be strictly between 0 and 1
     */
    public SegmentedLruReference(
            int maxEntries,
            long maxWeight,
            double protectedFraction,
            Weigher<K, V> weigher) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be > 0");
        }
        if (maxWeight <= 0L) {
            throw new IllegalArgumentException("maxWeight must be > 0");
        }
        if (!Double.isFinite(protectedFraction)
                || protectedFraction <= 0.0d
                || protectedFraction >= 1.0d) {
            throw new IllegalArgumentException("protectedFraction must be between 0 and 1");
        }

        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        this.protectedEntryTarget =
                maxEntries == 1
                        ? 1
                        : Math.max(1, Math.min(maxEntries - 1,
                                (int) Math.floor(maxEntries * protectedFraction)));
        this.protectedWeightTarget =
                Math.max(1L, Math.min(maxWeight,
                        (long) Math.floor(maxWeight * protectedFraction)));
        this.weigher = Objects.requireNonNull(weigher, "weigher");
        this.nodes = new HashMap<>();

        linkSentinels(probationHead, probationTail);
        linkSentinels(protectedHead, protectedTail);
    }

    /** Lookup, updating segment-local recency and promoting a probation hit. */
    public synchronized Optional<V> get(K key) {
        Node<K, V> node = nodes.get(key);
        if (node == null) {
            misses++;
            return Optional.empty();
        }

        hits++;
        if (node.segment == Segment.PROBATION) {
            probationHits++;
            promote(node);
            rebalanceProtected();
        } else {
            protectedHits++;
            moveToFront(node, protectedHead);
        }
        return Optional.of(node.value);
    }

    /**
     * Admit or replace one entry.
     *
     * <p>New entries enter probation as MRU. Replacing an existing entry preserves its segment and
     * promotes its recency. An entry heavier than the absolute cache budget is rejected without
     * disturbing the resident working set.</p>
     */
    public synchronized PutResult put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        long candidateWeight = requireWeight(weigher.weightOf(key, value));
        if (candidateWeight > maxWeight) {
            oversizeRejections++;
            return new PutResult(false, false, 0L, TrimResult.none());
        }

        Node<K, V> node = nodes.get(key);
        boolean replaced = node != null;
        if (node == null) {
            node = new Node<>(key, value, candidateWeight, Segment.PROBATION);
            nodes.put(key, node);
            addToFront(node, probationHead);
            probationEntries++;
            probationWeight = Math.addExact(probationWeight, candidateWeight);
        } else {
            subtractWeight(node);
            node.value = value;
            node.weight = candidateWeight;
            addWeight(node);
            moveToFront(
                    node,
                    node.segment == Segment.PROBATION ? probationHead : protectedHead);
            replacements++;
        }
        puts++;

        TrimResult trim = rebalanceProtected().plus(trimToInternal(maxEntries, maxWeight));
        return new PutResult(true, replaced, candidateWeight, trim);
    }

    /**
     * Return the exact entries a new candidate of the supplied weight would evict at the configured
     * hard limits, without changing recency, segment membership or statistics.
     *
     * <p>The order is the same order used by hard trimming: probation LRU first, then protected
     * LRU. This enables a frequency-aware admission layer to compare one candidate with every
     * resident it would displace before committing the mutation.</p>
     */
    public synchronized List<AdmissionVictim<K>> admissionVictims(long candidateWeight) {
        return admissionVictims(candidateWeight, maxEntries, maxWeight);
    }

    /**
     * Return the exact victims under caller-selected temporary limits.
     *
     * <p>This is used by composite cache policies whose other resident region consumes part of the
     * same global count/weight budget. The supplied limits may tighten, but never exceed, this
     * cache's configured hard limits.</p>
     */
    synchronized List<AdmissionVictim<K>> admissionVictims(
            long candidateWeight,
            int entryLimit,
            long weightLimit) {
        long checkedWeight = requireWeight(candidateWeight);
        if (entryLimit < 0 || entryLimit > maxEntries) {
            throw new IllegalArgumentException("entryLimit out of configured range");
        }
        if (weightLimit < 0L || weightLimit > maxWeight) {
            throw new IllegalArgumentException("weightLimit out of configured range");
        }
        if (entryLimit == 0 || checkedWeight > weightLimit) {
            return List.of();
        }

        int excessEntries = Math.max(0, nodes.size() + 1 - entryLimit);
        long freeWeight = Math.max(0L, weightLimit - weight());
        long excessWeight = checkedWeight > freeWeight ? checkedWeight - freeWeight : 0L;
        if (excessEntries == 0 && excessWeight == 0L) {
            return List.of();
        }

        ArrayList<AdmissionVictim<K>> victims = new ArrayList<>();
        Node<K, V> current = probationTail.previous;
        while ((excessEntries > 0 || excessWeight > 0L) && current != probationHead) {
            victims.add(new AdmissionVictim<>(current.key, current.weight, false));
            excessEntries = Math.max(0, excessEntries - 1);
            excessWeight = Math.max(0L, excessWeight - current.weight);
            current = current.previous;
        }

        current = protectedTail.previous;
        while ((excessEntries > 0 || excessWeight > 0L) && current != protectedHead) {
            victims.add(new AdmissionVictim<>(current.key, current.weight, true));
            excessEntries = Math.max(0, excessEntries - 1);
            excessWeight = Math.max(0L, excessWeight - current.weight);
            current = current.previous;
        }

        return List.copyOf(victims);
    }

    /** Explicitly remove one key without affecting hit/miss statistics. */
    public synchronized Optional<V> remove(K key) {
        Node<K, V> node = nodes.remove(key);
        if (node == null) return Optional.empty();
        detachAndAccount(node);
        return Optional.of(node.value);
    }

    /** Remove all matching keys in segment-LRU order. */
    public synchronized RemoveResult removeIf(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int removedEntries = 0;
        long removedWeight = 0L;

        RemoveAccumulator probation =
                removeIfFromList(probationTail.previous, probationHead, predicate);
        removedEntries = Math.addExact(removedEntries, probation.entries());
        removedWeight = Math.addExact(removedWeight, probation.weight());

        RemoveAccumulator protectedRemoved =
                removeIfFromList(protectedTail.previous, protectedHead, predicate);
        removedEntries = Math.addExact(removedEntries, protectedRemoved.entries());
        removedWeight = Math.addExact(removedWeight, protectedRemoved.weight());

        return new RemoveResult(removedEntries, removedWeight);
    }

    /**
     * Trim to temporary total limits.
     *
     * <p>Probation LRU entries are evicted first. This is intentional: one-hit scan entries should
     * leave before entries whose reuse earned protected residency.</p>
     */
    public synchronized TrimResult trimTo(int entryLimit, long weightLimit) {
        if (entryLimit < 0 || entryLimit > maxEntries) {
            throw new IllegalArgumentException(
                    "entryLimit must be between 0 and configured maxEntries");
        }
        if (weightLimit < 0L || weightLimit > maxWeight) {
            throw new IllegalArgumentException(
                    "weightLimit must be between 0 and configured maxWeight");
        }
        return trimToInternal(entryLimit, weightLimit);
    }

    /** Remove every cache-owned value. */
    public synchronized RemoveResult clear() {
        int entries = nodes.size();
        long weight = weight();
        nodes.clear();
        resetList(probationHead, probationTail);
        resetList(protectedHead, protectedTail);
        probationEntries = 0;
        protectedEntries = 0;
        probationWeight = 0L;
        protectedWeight = 0L;
        return new RemoveResult(entries, weight);
    }

    /** Membership check that does not alter recency or promotion state. */
    public synchronized boolean containsKey(K key) {
        return nodes.containsKey(key);
    }

    public synchronized int size() {
        return nodes.size();
    }

    public synchronized long weight() {
        return Math.addExact(probationWeight, protectedWeight);
    }

    public int maxEntries() {
        return maxEntries;
    }

    public long maxWeight() {
        return maxWeight;
    }

    public int protectedEntryTarget() {
        return protectedEntryTarget;
    }

    public long protectedWeightTarget() {
        return protectedWeightTarget;
    }

    /**
     * Deterministic eviction order: probation LRU→MRU followed by protected LRU→MRU.
     *
     * <p>This is an eviction-priority order, not a claim that the two segments form one global
     * recency clock.</p>
     */
    public synchronized List<K> keysInLruOrder() {
        ArrayList<K> keys = new ArrayList<>(nodes.size());
        appendKeys(probationTail.previous, probationHead, keys);
        appendKeys(protectedTail.previous, protectedHead, keys);
        return List.copyOf(keys);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(
                nodes.size(),
                probationEntries,
                protectedEntries,
                weight(),
                probationWeight,
                protectedWeight,
                maxEntries,
                maxWeight,
                protectedEntryTarget,
                protectedWeightTarget,
                hits,
                probationHits,
                protectedHits,
                misses,
                puts,
                replacements,
                promotions,
                demotions,
                evictions,
                probationEvictions,
                protectedEvictions,
                oversizeRejections);
    }

    private TrimResult rebalanceProtected() {
        int demoted = 0;
        while (protectedEntries > protectedEntryTarget
                || protectedWeight > protectedWeightTarget) {
            Node<K, V> candidate = protectedTail.previous;
            if (candidate == protectedHead) break;
            demote(candidate);
            demoted++;
        }
        return demoted == 0
                ? TrimResult.none()
                : new TrimResult(0, 0L, demoted);
    }

    private TrimResult trimToInternal(int entryLimit, long weightLimit) {
        int evictedEntries = 0;
        long evictedWeight = 0L;

        while (!nodes.isEmpty()
                && (nodes.size() > entryLimit || weight() > weightLimit)) {
            Node<K, V> victim = probationTail.previous;
            if (victim == probationHead) {
                victim = protectedTail.previous;
                if (victim == protectedHead) break;
            }
            long victimWeight = victim.weight;
            Segment segment = victim.segment;
            nodes.remove(victim.key);
            detachAndAccount(victim);
            evictedEntries++;
            evictedWeight = Math.addExact(evictedWeight, victimWeight);
            evictions++;
            if (segment == Segment.PROBATION) probationEvictions++;
            else protectedEvictions++;
        }

        return evictedEntries == 0
                ? TrimResult.none()
                : new TrimResult(evictedEntries, evictedWeight, 0);
    }

    private RemoveAccumulator removeIfFromList(
            Node<K, V> start,
            Node<K, V> sentinel,
            Predicate<? super K> predicate) {
        int count = 0;
        long removedWeight = 0L;
        Node<K, V> current = start;
        while (current != sentinel) {
            Node<K, V> previous = current.previous;
            if (predicate.test(current.key)) {
                nodes.remove(current.key);
                removedWeight = Math.addExact(removedWeight, current.weight);
                count++;
                detachAndAccount(current);
            }
            current = previous;
        }
        return new RemoveAccumulator(count, removedWeight);
    }

    private void promote(Node<K, V> node) {
        if (node.segment != Segment.PROBATION) {
            throw new IllegalStateException("only probation entries can be promoted");
        }
        unlink(node);
        probationEntries--;
        probationWeight -= node.weight;
        node.segment = Segment.PROTECTED;
        protectedEntries++;
        protectedWeight = Math.addExact(protectedWeight, node.weight);
        addToFront(node, protectedHead);
        promotions++;
    }

    private void demote(Node<K, V> node) {
        if (node.segment != Segment.PROTECTED) {
            throw new IllegalStateException("only protected entries can be demoted");
        }
        unlink(node);
        protectedEntries--;
        protectedWeight -= node.weight;
        node.segment = Segment.PROBATION;
        probationEntries++;
        probationWeight = Math.addExact(probationWeight, node.weight);
        addToFront(node, probationHead);
        demotions++;
    }

    private void detachAndAccount(Node<K, V> node) {
        unlink(node);
        if (node.segment == Segment.PROBATION) {
            probationEntries--;
            probationWeight -= node.weight;
        } else {
            protectedEntries--;
            protectedWeight -= node.weight;
        }
    }

    private void subtractWeight(Node<K, V> node) {
        if (node.segment == Segment.PROBATION) probationWeight -= node.weight;
        else protectedWeight -= node.weight;
    }

    private void addWeight(Node<K, V> node) {
        if (node.segment == Segment.PROBATION) {
            probationWeight = Math.addExact(probationWeight, node.weight);
        } else {
            protectedWeight = Math.addExact(protectedWeight, node.weight);
        }
    }

    private static <K, V> void moveToFront(
            Node<K, V> node,
            Node<K, V> head) {
        unlink(node);
        addToFront(node, head);
    }

    private static <K, V> void addToFront(
            Node<K, V> node,
            Node<K, V> head) {
        node.previous = head;
        node.next = head.next;
        head.next.previous = node;
        head.next = node;
    }

    private static <K, V> void unlink(Node<K, V> node) {
        node.previous.next = node.next;
        node.next.previous = node.previous;
        node.previous = null;
        node.next = null;
    }

    private static <K, V> void appendKeys(
            Node<K, V> start,
            Node<K, V> sentinel,
            List<K> target) {
        Node<K, V> current = start;
        while (current != sentinel) {
            target.add(current.key);
            current = current.previous;
        }
    }

    private static <K, V> Node<K, V> sentinel() {
        return new Node<>(null, null, 0L, null);
    }

    private static <K, V> void linkSentinels(
            Node<K, V> head,
            Node<K, V> tail) {
        head.next = tail;
        tail.previous = head;
    }

    private static <K, V> void resetList(
            Node<K, V> head,
            Node<K, V> tail) {
        head.previous = null;
        head.next = tail;
        tail.previous = head;
        tail.next = null;
    }

    private static long requireWeight(long weight) {
        if (weight <= 0L) {
            throw new IllegalArgumentException("entry weight must be > 0");
        }
        return weight;
    }

    private record RemoveAccumulator(int entries, long weight) {}

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private long weight;
        private Segment segment;
        private Node<K, V> previous;
        private Node<K, V> next;

        private Node(
                K key,
                V value,
                long weight,
                Segment segment) {
            this.key = key;
            this.value = value;
            this.weight = weight;
            this.segment = segment;
        }
    }
}
