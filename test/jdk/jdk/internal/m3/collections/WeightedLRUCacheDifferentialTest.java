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
 * @summary WeightedLRUCache on array lanes behaves as the node-linked version it replaced: every
 *          result, the recency order, the counters and the exceptions, on random operations,
 *          with keys whose hashCode throws, keys whose equals or hashCode and callbacks that
 *          change the cache, the same key object put back; same hashCode calls per lookup; same
 *          non-private members; removed values not retained; emptied lanes dropped
 * @modules jdk.m3.collections/jdk.internal.m3.collections:+open
 * @run main/othervm -Xmx512m WeightedLRUCacheDifferentialTest
 */

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.internal.m3.collections.WeightedLRUCache;

public class WeightedLRUCacheDifferentialTest {

    static final String NAME = WeightedLRUCache.class.getName();
    static long checks;
    /** Fresh caches after an overflow, a broken node walk or a write into a detached node. */
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
    static final String HASH_CME = "threw java.util.ConcurrentModificationException: "
            + "a key's hashCode changed the cache";
    static final Field KEY_LANE = keyLane();
    static final Field STAMP_LANE = field("stampLane");
    static final Field INDEX = field("index");

    static void same(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
            throw new AssertionError(what + ": reference " + expected + " array lanes " + actual);
        }
    }

    /** Compares the outcomes of an operation; returns the reference's outcome. */
    static String op(String what, Supplier<Object> reference, Supplier<Object> lanes) {
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
        String expected = outcome(0, reference);
        String actual = outcome(1, lanes);
        if (!expected.equals(actual)
                && (actual.equals(EQUALS_CME) || actual.equals(HASH_CME)
                        || expected.startsWith("threw ")
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
    static LruReference.Weigher<Key, String> weigher(int on) {
        return (k, v) -> {
            Runnable h = WEIGHER_HOOK[on];
            if (h != null) {
                WEIGHER_HOOK[on] = null;
                h.run();
            }
            if (v.startsWith("boom")) {
                throw new IllegalStateException("weigher refuses " + v);
            }
            return v.length() - 1;
        };
    }

    static Field keyLane() {
        try {
            Field f = WeightedLRUCache.class.getDeclaredField("keyLane");
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    static Field field(String name) {
        try {
            Field f = WeightedLRUCache.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * The lanes' own invariants, checked whatever the reference did: every resident counted once
     * and on the recency list, no two equal, stamps unique and nonzero on residents and 0 on free
     * slots, the index sized for the lanes. Lanes above 64 slots are checked every 7 steps, above
     * 256 every 97, above 4,096 every 997.
     */
    static void lanesSound(String at, WeightedLRUCache<Key, String> cache, int step) {
        try {
            Object[] keys = (Object[]) KEY_LANE.get(cache);
            int every = keys.length <= 64 ? 1 : keys.length <= 256 ? 7
                    : keys.length <= 4_096 ? 97 : 997;
            if (step % every != 0) {
                return;
            }
            int[] stamps = (int[]) STAMP_LANE.get(cache);
            // keys equal by id with different hashes break the hashCode contract (the tests make
            // them on purpose); HashMap holds those twice too, so equal means same id and hash
            long[] ids = new long[keys.length];
            long[] residentStamps = new long[keys.length];
            int residents = 0;
            boolean freeStampsZero = true;
            boolean stampsNonzero = true;
            for (int slot = 0; slot < keys.length; slot++) {
                if (keys[slot] == null) {
                    freeStampsZero &= stamps[slot] == 0;
                } else {
                    Key k = (Key) keys[slot];
                    ids[residents] = (long) k.id() << 32 | k.hash() & 0xFFFF_FFFFL;
                    stampsNonzero &= stamps[slot] != 0;
                    residentStamps[residents++] = stamps[slot];
                }
            }
            same(cache.size(), residents, at + " lanes: residents counted once");
            same(cache.size(), cache.keysInLruOrder().size(), at + " lanes: recency list");
            same(true, freeStampsZero, at + " lanes: free slots have stamp 0");
            same(true, distinct(ids, residents), at + " lanes: no two equal residents");
            same(true, stampsNonzero && distinct(residentStamps, residents),
                    at + " lanes: resident stamps unique and nonzero");
            same(indexLength(keys.length, cache.maxEntries()), ((int[]) INDEX.get(cache)).length,
                    at + " lanes: index length");
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    static boolean distinct(long[] values, int n) {
        long[] sorted = java.util.Arrays.copyOf(values, n);
        java.util.Arrays.sort(sorted);
        for (int i = 1; i < n; i++) {
            if (sorted[i] == sorted[i - 1]) {
                return false;
            }
        }
        return true;
    }

    /** The index length grow() gives lanes of this length (none for dropped lanes). */
    static int indexLength(int lanes, int maxEntries) {
        if (lanes == 0) {
            return 0;
        }
        long cells = Math.max(2 * Math.min(lanes, (long) maxEntries), lanes + 1L);
        return 1 << (64 - Long.numberOfLeadingZeros(cells - 1));
    }

    /**
     * A key whose hashCode puts an equal key into a cache that never admitted a key: the outer put
     * gets a ConcurrentModificationException and the cache holds the inner key once.
     */
    static void firstAdmissionHashReentry() {
        WeightedLRUCache<Key, String> cache =
                new WeightedLRUCache<>(8, 100L, weigher(1)::weightOf);
        Key outer = new Key(7, 7, false);
        HOOK_KEY[1] = outer;
        HASH_HOOK[1] = () -> cache.put(new Key(7, 7, false), "inner");
        same(HASH_CME, outcome(1, () -> cache.put(outer, "outer")),
                "first admission: hashCode puts an equal key");
        HOOK_KEY[1] = null;
        same(1, cache.size(), "first admission: one resident");
        lanesSound("first admission", cache, 0);
        same(true, cache.containsKey(outer), "first admission: the inner key is resident");
        cache.remove(outer);
        same(false, cache.containsKey(outer), "first admission: removed once, gone");
    }

    /** The length of the lanes: 0 once an emptied cache dropped them. */
    static int lanes(WeightedLRUCache<?, ?> cache) {
        try {
            return ((Object[]) KEY_LANE.get(cache)).length;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void main(String[] args) {
        same(signatures(LruReference.class), signatures(WeightedLRUCache.class),
                "non-private members");
        for (long seed = 1; seed <= 12; seed++) {
            int maxEntries = 1 + (int) (seed % 7) * 9;
            run(seed, 40_000, maxEntries, seed % 3 == 0 ? 50L : 2_000L, (int) seed % 4, 1);
        }
        run(99L, 200_000, 4_000, 1_000_000L, 0, 1);
        run(100L, 100_000, 1, 10L, 1, 1);
        run(101L, 30_000, 70_000, Long.MAX_VALUE / 4, 0, 997);
        run(102L, 20_000, 8, 2_000L, 2, 1); // a power of two: the index for the extra slot
        firstAdmissionHashReentry();
        System.out.println("WeightedLRUCacheDifferentialTest: " + checks + " checks passed, "
                + restarts + " restarts");
    }

    static TreeSet<String> signatures(Class<?> type) {
        TreeSet<String> all = new TreeSet<>();
        List<Executable> executables = new ArrayList<>(List.of(type.getDeclaredMethods()));
        executables.addAll(List.<Constructor<?>>of(type.getDeclaredConstructors()));
        executables.stream()
                .filter(e -> !Modifier.isPrivate(e.getModifiers()) && !e.isSynthetic())
                .forEach(e -> all.add(e.toGenericString().replace(NAME, "LruReference")));
        for (Class<?> nested : type.getDeclaredClasses()) {
            if (!Modifier.isPrivate(nested.getModifiers())) {
                all.add(Modifier.toString(nested.getModifiers()) + " "
                        + nested.getName().replace(NAME, "LruReference"));
            }
        }
        return all;
    }

    static void run(long seed, int steps, int maxEntries, long maxWeight, int hashKind,
            int orderEvery) {
        Random random = new Random(seed);
        for (int step = 0; step < steps; ) {
            step = epoch(random, seed, step, steps, maxEntries, maxWeight, hashKind, orderEvery);
        }
    }

    /** Fresh caches from step first on, until the steps are done or the two may differ. */
    static int epoch(Random random, long seed, int first, int steps, int maxEntries,
            long maxWeight, int hashKind, int orderEvery) {
        int keySpace = Math.max(4, maxEntries * 3);
        WeightedLRUCache<Key, String> cache =
                new WeightedLRUCache<>(maxEntries, maxWeight, weigher(1)::weightOf);
        LruReference<Key, String> ref = new LruReference<>(maxEntries, maxWeight, weigher(0));
        Actions refActions = new Actions(ref::remove, ref::put, ref::get, ref::clear,
                () -> ref.trimTo(0, 0L), () -> ref.removeIf(k -> true));
        Actions laneActions = new Actions(cache::remove, cache::put, cache::get, cache::clear,
                () -> cache.trimTo(0, 0L), () -> cache.removeIf(k -> true));
        if (orderEvery > 1) {
            // a large cache: fill it past maxEntries first (several doublings of the lanes)
            for (int i = 0; i <= maxEntries; i++) {
                Key k = new Key(i, Integer.hashCode(i), false);
                String v = i % 7 == 0 ? "v" + i : "ww";
                same(ref.put(k, v), cache.put(k, v), "fill " + i);
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
            boolean big = orderEvery > 1;
            String outcome = !big && random.nextInt(9) == 0
                    ? mutating(random, at, key, other, value, ref, cache, refActions, laneActions)
                    : step(random, at, key, value, maxEntries, maxWeight, ref, cache, big);
            lanesSound(at, cache, step);
            if (outcome.equals(DIVERGED)) {
                restarts++;
                return step + 1;
            }
            if (step % orderEvery == 0) {
                same(ref.keysInLruOrder(), cache.keysInLruOrder(), at + " order");
            }
            same(ref.snapshot(), cache.snapshot(), at + " snapshot");
            same(ref.size() + "/" + ref.weight(), cache.size() + "/" + cache.weight(),
                    at + " size/weight");
            same(true, cache.size() > 0 || lanes(cache) == 0,
                    at + " an emptied cache drops its lanes");
            if (random.nextInt(4_000) == 0) {
                retention(random, at, maxEntries, ref, cache);
            }
        }
        return steps;
    }

    static String step(Random random, String at, Key key, String value, int maxEntries,
            long maxWeight, LruReference<Key, String> ref, WeightedLRUCache<Key, String> cache,
            boolean big) {
        int what = random.nextInt(14);
        if (big && (what == 11 || what == 12)) {
            what = 3; // a large cache: no O(n) removeIf or clear per step
        }
        switch (what) {
            case 0, 1, 2:
                return op(at + " put", () -> ref.put(key, value), () -> cache.put(key, value));
            case 3, 4:
                return opCounted(at + " get", () -> ref.get(key), () -> cache.get(key));
            case 5:
                return opCounted(at + " peek", () -> ref.peek(key), () -> cache.peek(key));
            case 6:
                return opCounted(at + " peekWeight",
                        () -> ref.peekWeight(key), () -> cache.peekWeight(key));
            case 7:
                return opCounted(at + " remove",
                        () -> ref.remove(key), () -> cache.remove(key));
            case 8:
                return opCounted(at + " containsKey",
                        () -> ref.containsKey(key), () -> cache.containsKey(key));
            case 9:
                return opCounted(at + " replace",
                        () -> ref.replaceIfLighterPreservingRecency(key, value),
                        () -> cache.replaceIfLighterPreservingRecency(key, value));
            case 10: {
                int below = random.nextInt(20) == 0 ? 1 : 0;
                int entries = random.nextInt(maxEntries + 2) - below;
                long weights = (long) (random.nextDouble() * (Math.min(maxWeight, 1L << 40) + 2))
                        - 1;
                return op(at + " trimTo", () -> ref.trimTo(entries, weights),
                        () -> cache.trimTo(entries, weights));
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
            default:
                return op(at + " put refused by the weigher",
                        () -> ref.put(key, "boom" + value), () -> cache.put(key, "boom" + value));
        }
    }

    /**
     * A put or a maintenance replace whose weigher, a removeIf whose predicate, or a lookup whose
     * key's equals or hashCode changes the cache first.
     */
    static String mutating(Random random, String at, Key key, Key other, String value,
            LruReference<Key, String> ref, WeightedLRUCache<Key, String> cache,
            Actions refActions, Actions laneActions) {
        int what = random.nextInt(5);
        int kind = 1 + random.nextInt(Actions.KINDS - 1);
        String inner = value(random);
        if (what < 2) {
            WEIGHER_HOOK[0] = () -> refActions.apply(kind, key, other, inner);
            WEIGHER_HOOK[1] = () -> laneActions.apply(kind, key, other, inner);
            String outcome = what == 0
                    ? opMutating(at + " put, weigher " + kind + " " + other,
                            () -> ref.put(key, value), () -> cache.put(key, value))
                    : opMutating(at + " replace, weigher " + kind + " " + other,
                            () -> ref.replaceIfLighterPreservingRecency(key, value),
                            () -> cache.replaceIfLighterPreservingRecency(key, value));
            if (outcome.equals(DIVERGED)) {
                return outcome;
            }
            return detachedWrite(at, ref, cache) ? DIVERGED : outcome;
        }
        if (what == 2) {
            return removeIfMutating(random, at, other, ref, cache, refActions, laneActions);
        }
        // a lookup whose key's equals (what 3) or hashCode (what 4) changes the cache first
        Key probe = key == null ? other : new Key(key.id(), key.hash(), false);
        Runnable[] hooks = what == 3 ? EQUALS_HOOK : HASH_HOOK;
        hooks[0] = () -> refActions.apply(kind, probe, other, inner);
        hooks[1] = () -> laneActions.apply(kind, probe, other, inner);
        HOOK_KEY[0] = probe;
        HOOK_KEY[1] = probe;
        String name = at + (what == 3 ? " equals " : " hashCode ") + kind + " " + other;
        // HashMap.get and containsKey read their table before they call hashCode, so a hashCode
        // that changes the cache makes the node version look in the old table; the lanes look in
        // the changed cache (put starts with get). Those may differ; remove hashes first in both.
        boolean loose = what == 4;
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

    /**
     * After a weigher callback: if the reference's total is not the sum of its residents' weights
     * (it wrote a detached node), the residents must still agree and the lanes' total must be the
     * sum of theirs; the caches may then differ, fresh ones follow.
     */
    static boolean detachedWrite(String at, LruReference<Key, String> ref,
            WeightedLRUCache<Key, String> cache) {
        if (ref.weight() == cache.weight() && ref.size() == cache.size() || consistent(ref)) {
            return false;
        }
        List<Key> order = ref.keysInLruOrder();
        same(order, cache.keysInLruOrder(), at + " order after a detached write");
        long sum = 0L;
        for (Key k : order) {
            same(ref.peek(k), cache.peek(k), at + " value after a detached write");
            same(ref.peekWeight(k), cache.peekWeight(k), at + " weight of " + k);
            sum += cache.peekWeight(k).orElseThrow();
        }
        same(sum, cache.weight(), at + " lanes weight after a detached write");
        return true;
    }

    /** Same size and weight on both sides: no sign of a corrupted reference. */
    static boolean quiet(LruReference<Key, String> ref, WeightedLRUCache<Key, String> cache) {
        try {
            return ref.size() == cache.size() && ref.weight() == cache.weight();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Whether the reference's total is the sum of its residents' weights and all are found. */
    static boolean consistent(LruReference<Key, String> ref) {
        try {
            long sum = 0L;
            for (Key k : ref.keysInLruOrder()) {
                OptionalLong w = ref.peekWeight(k);
                if (w.isEmpty()) {
                    return false;
                }
                sum += w.getAsLong();
            }
            return sum == ref.weight() && ref.keysInLruOrder().size() == ref.size();
        } catch (RuntimeException e) {
            return false;
        }
    }

    static String removeIfMutating(Random random, String at, Key other,
            LruReference<Key, String> ref, WeightedLRUCache<Key, String> cache,
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
            LruReference<Key, String> ref, WeightedLRUCache<Key, String> cache) {
        Key key = new Key(-1_000_000 - random.nextInt(1_000), 1, false);
        String laneValue = new String("retained value " + at);
        String refValue = new String(laneValue);
        LruReference.PutResult refPut = ref.put(key, refValue);
        WeightedLRUCache.PutResult lanePut = cache.put(key, laneValue);
        same(refPut, lanePut, at + " retention put");
        if (!lanePut.admitted()) {
            return;
        }
        WeakReference<String> held = new WeakReference<>(laneValue);
        laneValue = null;
        refValue = null;
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

/** The reference: the node-linked WeightedLRUCache of master, renamed only. */
final class LruReference<K, V> {

    /** Caller-owned retained-weight function. */
    @FunctionalInterface
    public interface Weigher<K, V> {
        long weightOf(K key, V value);
    }

    /** Result of an LRU trim or automatic admission trim. */
    public record TrimResult(int evictedEntries, long evictedWeight) {
        public TrimResult {
            if (evictedEntries < 0 || evictedWeight < 0L) {
                throw new IllegalArgumentException("negative trim result");
            }
        }

        public static TrimResult none() {
            return new TrimResult(0, 0L);
        }

        public TrimResult plus(TrimResult other) {
            Objects.requireNonNull(other, "other");
            return new TrimResult(
                    Math.addExact(evictedEntries, other.evictedEntries),
                    Math.addExact(evictedWeight, other.evictedWeight));
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

    /**
     * Result of a maintenance replacement that deliberately preserves the entry's current LRU
     * position.
     *
     * <p>This operation exists for pressure-driven representation demotion. It never admits an
     * absent key and never allows a heavier replacement.</p>
     */
    public record MaintenanceReplaceResult(
            boolean present,
            boolean replaced,
            long previousWeight,
            long replacementWeight,
            long releasedWeight) {
        public MaintenanceReplaceResult {
            if (previousWeight < 0L || replacementWeight < 0L || releasedWeight < 0L) {
                throw new IllegalArgumentException("negative maintenance replacement weight");
            }
            if (!present && (replaced || previousWeight != 0L || replacementWeight != 0L
                    || releasedWeight != 0L)) {
                throw new IllegalArgumentException("absent maintenance replacement has weight");
            }
            if (replaced && replacementWeight >= previousWeight) {
                throw new IllegalArgumentException("maintenance replacement must "
                        + "be strictly lighter");
            }
            if (replaced && releasedWeight != previousWeight - replacementWeight) {
                throw new IllegalArgumentException("maintenance replacement release mismatch");
            }
            if (!replaced && releasedWeight != 0L) {
                throw new IllegalArgumentException("rejected maintenance "
                        + "replacement released weight");
            }
        }

        public static MaintenanceReplaceResult absent() {
            return new MaintenanceReplaceResult(false, false, 0L, 0L, 0L);
        }
    }

    /** Result of explicit predicate removal. */
    public record RemoveResult(int removedEntries, long removedWeight) {
        public RemoveResult {
            if (removedEntries < 0 || removedWeight < 0L) {
                throw new IllegalArgumentException("negative remove result");
            }
        }
    }

    /** Immutable runtime statistics. */
    public record Snapshot(
            int entries,
            long weight,
            int maxEntries,
            long maxWeight,
            long hits,
            long misses,
            long puts,
            long replacements,
            long evictions,
            long oversizeRejections) {
        public Snapshot {
            if (entries < 0
                    || weight < 0L
                    || maxEntries <= 0
                    || maxWeight <= 0L
                    || hits < 0L
                    || misses < 0L
                    || puts < 0L
                    || replacements < 0L
                    || evictions < 0L
                    || oversizeRejections < 0L) {
                throw new IllegalArgumentException("invalid weighted LRU snapshot");
            }
        }
    }

    private final int maxEntries;
    private final long maxWeight;
    private final Weigher<K, V> weigher;
    private final Map<K, Node<K, V>> nodes;
    private final Node<K, V> head;
    private final Node<K, V> tail;

    private long weight;
    private long hits;
    private long misses;
    private long puts;
    private long replacements;
    private long evictions;
    private long oversizeRejections;

    public LruReference(
            int maxEntries,
            long maxWeight,
            Weigher<K, V> weigher) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be > 0");
        }
        if (maxWeight <= 0L) {
            throw new IllegalArgumentException("maxWeight must be > 0");
        }
        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        this.weigher = Objects.requireNonNull(weigher, "weigher");
        this.nodes = new HashMap<>();
        this.head = new Node<>(null, null, 0L);
        this.tail = new Node<>(null, null, 0L);
        head.next = tail;
        tail.previous = head;
    }

    /** Lookup and promote the key to most-recently-used position. */
    public synchronized Optional<V> get(K key) {
        Node<K, V> node = nodes.get(key);
        if (node == null) {
            misses++;
            return Optional.empty();
        }
        hits++;
        moveToFront(node);
        return Optional.of(node.value);
    }

    /**
     * Admit or replace one entry.
     *
     * <p>An entry heavier than the configured absolute weight limit is rejected without evicting
     * existing entries. Accepted entries are promoted to MRU and the cache trims to both configured
     * limits.</p>
     */
    public synchronized PutResult put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        long candidateWeight = requireWeight(weigher.weightOf(key, value));
        if (candidateWeight > maxWeight) {
            oversizeRejections++;
            return new PutResult(false, false, 0L, TrimResult.none());
        }

        Node<K, V> existing = nodes.get(key);
        boolean replaced = existing != null;
        if (existing == null) {
            existing = new Node<>(key, value, candidateWeight);
            nodes.put(key, existing);
            addToFront(existing);
        } else {
            weight -= existing.weight;
            existing.value = value;
            existing.weight = candidateWeight;
            moveToFront(existing);
            replacements++;
        }
        // The candidate is already MRU, but its new charge is not in weight yet.
        // Trim the other charge first: maxWeight-candidateWeight cannot overflow.
        // This is equivalent to add-then-trim over mathematical integers, without
        // overflowing a transient long total and leaving partially changed state.
        TrimResult trim = trimToInternal(maxEntries, maxWeight - candidateWeight);
        weight = Math.addExact(weight, candidateWeight);
        puts++;
        return new PutResult(true, replaced, candidateWeight, trim);
    }

    /**
     * Observe one resident without changing recency or hit/miss statistics.
     *
     * <p>This is intended for maintenance code such as pressure-driven representation demotion.
     * Normal callers should prefer {@code get(...)} so useful work contributes recency.</p>
     */
    public synchronized Optional<V> peek(K key) {
        Objects.requireNonNull(key, "key");
        Node<K, V> node = nodes.get(key);
        return node == null ? Optional.empty() : Optional.of(node.value);
    }

    /** Observe one resident's admitted weight without changing recency or statistics. */
    public synchronized OptionalLong peekWeight(K key) {
        Objects.requireNonNull(key, "key");
        Node<K, V> node = nodes.get(key);
        return node == null ? OptionalLong.empty() : OptionalLong.of(node.weight);
    }

    /**
     * Replace an existing value only when the new representation is strictly lighter, without
     * changing
     * its recency position.
     *
     * <p>The operation does not alter get/put hit, miss, put, replacement, or eviction counters.
     * It is a maintenance primitive for stripping rebuildable derived state while preserving the
     * semantic resident and its LRU ordering. An absent key is never admitted.</p>
     */
    public synchronized MaintenanceReplaceResult replaceIfLighterPreservingRecency(
            K key, V replacement) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(replacement, "replacement");
        Node<K, V> node = nodes.get(key);
        if (node == null) return MaintenanceReplaceResult.absent();

        long replacementWeight = requireWeight(weigher.weightOf(key, replacement));
        long previousWeight = node.weight;
        if (replacementWeight >= previousWeight) {
            return new MaintenanceReplaceResult(
                    true, false, previousWeight, replacementWeight, 0L);
        }

        node.value = replacement;
        node.weight = replacementWeight;
        long released = previousWeight - replacementWeight;
        weight -= released;
        return new MaintenanceReplaceResult(
                true, true, previousWeight, replacementWeight, released);
    }

    /** Explicitly remove one key. */
    public synchronized Optional<V> remove(K key) {
        Node<K, V> node = nodes.remove(key);
        if (node == null) return Optional.empty();
        unlink(node);
        weight -= node.weight;
        return Optional.of(node.value);
    }

    /** Remove all keys matching the supplied predicate. */
    public synchronized RemoveResult removeIf(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int removedEntries = 0;
        long removedWeight = 0L;
        Node<K, V> current = tail.previous;
        while (current != head) {
            Node<K, V> previous = current.previous;
            if (predicate.test(current.key)) {
                nodes.remove(current.key);
                unlink(current);
                removedEntries++;
                removedWeight = Math.addExact(removedWeight, current.weight);
                weight -= current.weight;
            }
            current = previous;
        }
        return new RemoveResult(removedEntries, removedWeight);
    }

    /**
     * Trim to caller-selected temporary limits.
     *
     * <p>Temporary limits must not exceed configured hard limits. This is intended for
     * memory-pressure policies that dynamically tighten the cache budget.</p>
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

    /** Remove all cache ownership. */
    public synchronized RemoveResult clear() {
        int removedEntries = nodes.size();
        long removedWeight = weight;
        nodes.clear();
        head.next = tail;
        tail.previous = head;
        weight = 0L;
        return new RemoveResult(removedEntries, removedWeight);
    }

    /** Membership check that does not alter recency. */
    public synchronized boolean containsKey(K key) {
        return nodes.containsKey(key);
    }

    public synchronized int size() {
        return nodes.size();
    }

    public synchronized long weight() {
        return weight;
    }

    public int maxEntries() {
        return maxEntries;
    }

    public long maxWeight() {
        return maxWeight;
    }

    /** LRU-to-MRU key order, primarily for deterministic diagnostics/tests. */
    public synchronized List<K> keysInLruOrder() {
        ArrayList<K> keys = new ArrayList<>(nodes.size());
        Node<K, V> current = tail.previous;
        while (current != head) {
            keys.add(current.key);
            current = current.previous;
        }
        return List.copyOf(keys);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(
                nodes.size(),
                weight,
                maxEntries,
                maxWeight,
                hits,
                misses,
                puts,
                replacements,
                evictions,
                oversizeRejections);
    }

    private TrimResult trimToInternal(int entryLimit, long weightLimit) {
        int evictedEntries = 0;
        long evictedWeight = 0L;
        while (!nodes.isEmpty()
                && (nodes.size() > entryLimit || weight > weightLimit)) {
            Node<K, V> lru = tail.previous;
            if (lru == head) break;
            nodes.remove(lru.key);
            unlink(lru);
            weight -= lru.weight;
            evictedEntries++;
            evictedWeight = Math.addExact(evictedWeight, lru.weight);
            evictions++;
        }
        return new TrimResult(evictedEntries, evictedWeight);
    }

    private void moveToFront(Node<K, V> node) {
        unlink(node);
        addToFront(node);
    }

    private void addToFront(Node<K, V> node) {
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

    private static long requireWeight(long value) {
        if (value <= 0L) {
            throw new IllegalArgumentException("entry weight must be > 0");
        }
        return value;
    }

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private long weight;
        private Node<K, V> previous;
        private Node<K, V> next;

        private Node(K key, V value, long weight) {
            this.key = key;
            this.value = value;
            this.weight = weight;
        }
    }
}
