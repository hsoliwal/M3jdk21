/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Set-associative arrays use offset directories, packed occupancy and FIFO eviction
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED SetAssociativeCacheArrayMaskTest
 */

import jdk.internal.m3.collections.SetAssociativeCache;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone Java 21 contract and physical-layout oracle for a candidate only. */
public final class SetAssociativeCacheArrayMaskTest {
    private static long checks;

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void truth(boolean expression) {
        checks++;
        if (!expression) throw new AssertionError("assertion " + checks);
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable task) {
        try {
            task.run();
        } catch (Throwable exception) {
            truth(expected.isInstance(exception));
            return;
        }
        throw new AssertionError("missing " + expected.getName());
    }

    private record Entry(int key, String value) { }

    /** Independent per-set FIFO oracle; pairs exist only inside the test. */
    private static final class Model {
        final List<List<Entry>> sets;
        final int ways;
        final List<String> evictions = new ArrayList<>();
        long hits;
        long misses;
        long puts;

        Model(int numSets, int ways) {
            this.ways = ways;
            sets = new ArrayList<>();
            for (int s = 0; s < numSets; s++) sets.add(new ArrayList<>());
        }

        List<Entry> set(int key) {
            return sets.get((key ^ (key >>> 16)) & (sets.size() - 1));
        }

        String get(int key) {
            for (Entry entry : set(key)) {
                if (entry.key() == key) {
                    hits++;
                    return entry.value();
                }
            }
            misses++;
            return null;
        }

        boolean contains(int key) {
            for (Entry entry : set(key)) if (entry.key() == key) return true;
            return false;
        }

        String put(int key, String value) {
            List<Entry> slots = set(key);
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).key() == key) {
                    String old = slots.remove(i).value();
                    slots.add(0, new Entry(key, value));
                    puts++;
                    return old;
                }
            }
            if (slots.size() == ways) evictions.add(slots.remove(slots.size() - 1).value());
            slots.add(0, new Entry(key, value));
            puts++;
            return null;
        }

        String remove(int key) {
            List<Entry> slots = set(key);
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).key() == key) return slots.remove(i).value();
            }
            return null;
        }

        int size() { return sets.stream().mapToInt(List::size).sum(); }
        void clear() { sets.forEach(List::clear); }
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void layout(SetAssociativeCache<String> actual, Model model)
            throws ReflectiveOperationException {
        int[] bases = (int[]) field(actual, "setBaseLane");
        int[] counts = (int[]) field(actual, "setCountLane");
        int[] keys = (int[]) field(actual, "keyLane");
        Object[] values = (Object[]) field(actual, "valueLane");
        long[] masks = (long[]) field(actual, "stateMaskWords");
        int allocated = (Integer) field(actual, "allocatedRuns");
        int ways = model.ways;
        equal(keys.length, values.length);
        equal((keys.length + 63) >>> 6, masks.length);
        truth(allocated * ways <= keys.length);
        int present = 0;
        for (int set = 0; set < model.sets.size(); set++) {
            List<Entry> reference = model.sets.get(set);
            equal(reference.size(), counts[set]);
            int base = bases[set];
            if (base < 0) {
                truth(reference.isEmpty());
                continue;
            }
            truth(base % ways == 0 && base >= 0 && base + ways <= keys.length);
            for (int i = 0; i < ways; i++) {
                int slot = base + i;
                boolean occupied = (masks[slot >>> 6] & (1L << (slot & 63))) != 0;
                equal(i < reference.size(), occupied);
                if (occupied) {
                    equal(reference.get(i).key(), keys[slot]);
                    equal(reference.get(i).value(), values[slot]);
                    present++;
                } else {
                    equal(null, values[slot]);
                }
            }
        }
        equal(model.size(), present);
        equal(model.size(), actual.size());
    }

    private static void contract() throws ReflectiveOperationException {
        rejects(IllegalArgumentException.class, () -> new SetAssociativeCache<>(0, 4));
        rejects(IllegalArgumentException.class, () -> new SetAssociativeCache<>(8, 0));
        rejects(IllegalArgumentException.class, () -> new SetAssociativeCache<>(3, 4));
        rejects(IllegalArgumentException.class, () -> new SetAssociativeCache<>(4, 3));
        rejects(IllegalArgumentException.class, () -> new SetAssociativeCache<>(1 << 20, 1 << 20));
        SetAssociativeCache<String> cache = new SetAssociativeCache<>();
        equal(64, cache.capacity());
        equal(0, ((int[]) field(cache, "keyLane")).length);
        equal(0, ((long[]) field(cache, "stateMaskWords")).length);
        equal(0, cache.size());
        rejects(NullPointerException.class, () -> cache.put(42, null));
        equal(0, ((int[]) field(cache, "keyLane")).length);
        equal(0L, cache.cachePuts());
        equal(null, cache.get(42));
        equal(1L, cache.cacheMisses());
        equal(0L, cache.cacheHits());
        equal(null, cache.put(42, "first"));
        equal("first", cache.get(42));
        equal("first", cache.put(42, "second"));
        equal("second", cache.get(42));
        equal(1, cache.size());
        equal(2L, cache.cacheHits());
        equal(2L, cache.cachePuts());
        cache.clear();
        equal(0, cache.size());
        equal(0, ((int[]) field(cache, "keyLane")).length);
        equal(2L, cache.cacheHits());
        cache.resetCounters();
        equal(0L, cache.cacheHits());
        equal(0L, cache.cacheMisses());
        equal(0L, cache.cachePuts());
        truth(SetAssociativeCache.scaffoldDescriptor().contains("OCCUPIED"));
    }

    private static void fifoAndCallbacks() {
        List<String> evictions = new ArrayList<>();
        SetAssociativeCache<String> cache = new SetAssociativeCache<>(1, 4, evictions::add);
        for (int i = 0; i < 4; i++) cache.put(i, "v" + i);
        equal("v0", cache.get(0)); // A read does not make the oldest young again.
        equal("v2", cache.put(2, "replacement"));
        equal(null, cache.put(4, "v4"));
        equal(List.of("v0"), evictions);
        truth(!cache.containsKey(0));
        truth(cache.containsKey(2));
        equal(4, cache.size());
        equal("v1", cache.remove(1));
        equal(3, cache.size());
        equal(null, cache.put(5, "v5"));
        equal(List.of("v0"), evictions);
        // Exceptions from callbacks never leave a partially inserted key.
        AtomicInteger attempts = new AtomicInteger();
        @SuppressWarnings("unchecked")
        SetAssociativeCache<String>[] holder = (SetAssociativeCache<String>[]) new SetAssociativeCache<?>[1];
        holder[0] = new SetAssociativeCache<>(1, 1, v -> {
            attempts.incrementAndGet();
            truth(holder[0].containsKey(8));
            throw new IllegalStateException("callback");
        });
        holder[0].put(7, "old");
        rejects(IllegalStateException.class, () -> holder[0].put(8, "new"));
        equal(1, attempts.get());
        equal("new", holder[0].get(8));
        equal(null, holder[0].get(7));
    }

    private static void randomized(int sets, int ways, long seed, int operations)
            throws ReflectiveOperationException {
        Model model = new Model(sets, ways);
        // The callback trace is independent from the model's eviction trace.
        List<String> callbacks = new ArrayList<>();
        SetAssociativeCache<String> actual = new SetAssociativeCache<>(sets, ways, callbacks::add);
        Random random = new Random(seed);
        int[] fixed = {Integer.MIN_VALUE, -65536, -1, 0, 1, 65536, Integer.MAX_VALUE};
        for (int n = 0; n < operations; n++) {
            int key = (n % 13 == 0) ? fixed[(n / 13) % fixed.length]
                    : (random.nextInt(8) == 0 ? random.nextInt() : random.nextInt(160) - 80);
            int operation = random.nextInt(12);
            switch (operation) {
                case 0, 1, 2, 3, 4 -> {
                    String value = "v" + n;
                    equal(model.put(key, value), actual.put(key, value));
                }
                case 5, 6 -> equal(model.get(key), actual.get(key));
                case 7 -> equal(model.remove(key), actual.remove(key));
                case 8, 9 -> equal(model.contains(key), actual.containsKey(key));
                case 10 -> {
                    model.clear();
                    actual.clear();
                }
                default -> {
                    model.hits = 0;
                    model.misses = 0;
                    model.puts = 0;
                    actual.resetCounters();
                }
            }
            equal(model.size(), actual.size());
            equal(model.hits, actual.cacheHits());
            equal(model.misses, actual.cacheMisses());
            equal(model.puts, actual.cachePuts());
            equal(model.evictions.size(), callbacks.size());
            if (!callbacks.isEmpty()) {
                equal(model.evictions.get(model.evictions.size() - 1),
                        callbacks.get(callbacks.size() - 1));
            }
            if ((n & 127) == 0) layout(actual, model);
        }
        equal(model.evictions, callbacks);
        layout(actual, model);
    }

    public static void main(String[] args) throws Exception {
        contract();
        fifoAndCallbacks();
        randomized(1, 1, 0xC0FFEEL, 80_000);
        randomized(2, 4, 0xBAD5EEDL, 120_000);
        randomized(16, 4, 0x5E7A550CL, 120_000);
        randomized(32, 8, 0xD00F1234L, 120_000);
        System.out.println("PASS SetAssociativeCacheArrayMaskTest checks=" + checks);
    }
}
