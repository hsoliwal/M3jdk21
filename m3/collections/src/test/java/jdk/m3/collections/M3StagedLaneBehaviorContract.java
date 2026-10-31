/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Task-local behavior oracle, frozen independently of recipe postimages. JDK models supply
 * values, membership and signed/unsigned order; boundary assertions preserve M3-specific APIs.
 * This is a finite executable witness, not an exhaustive proof or a performance benchmark.
 */
public final class M3StagedLaneBehaviorContract {
    private static final long MISSING = 0x5eed_dead_beef_4321L;
    private static final long[] EDGES = {0L, -1L, 1L, Long.MIN_VALUE, Long.MAX_VALUE,
            0x0000_0001_0000_0000L, 0xffff_ffff_0000_0000L};

    private M3StagedLaneBehaviorContract() { }

    public static void main(String[] arguments) throws Exception {
        run();
    }

    public static void run() throws Exception {
        intInt();
        intLong();
        identity();
        longHash();
        sortedMap();
        patricia();
        pagedSet();
        System.out.println("PASS M3 staged-lane behavior: 7 owners; fixed seeds; no timing claims");
    }

    // Primitive int map cases are below the shared helpers. Keeping their typed calls explicit
    // means javac also checks the frozen primitive signatures without reflective adaptation.


    public static void intInt() {
        Check c = new Check("M3IntIntHashMap");
        c.throwsType(IllegalArgumentException.class, () -> new M3IntIntHashMap(-1));
        c.throwsType(IllegalArgumentException.class, () -> new M3IntIntHashMap(Integer.MAX_VALUE));
        M3IntIntHashMap actual = new M3IntIntHashMap();
        Map<Integer, Integer> expected = new HashMap<>();
        c.eq(8, actual.capacity(), "initial capacity");
        actual.put(0, 0);
        for (int key = 1; key <= 5; key++) actual.put(key, key);
        c.eq(8, actual.capacity(), "zero sidecar does not consume threshold");
        actual.put(6, 6);
        c.eq(16, actual.capacity(), "six nonzero keys grow the initial table");
        actual.clear();
        for (int key = 0; key < 1024; key++) {
            int value = (~key);
            expected.put(key, value);
            c.eq(true, actual.put(key, value), "dense warmup");
        }
        Random random = new Random(0x494e54494e54L);
        for (int step = 0; step < 7000; step++) {
            c.step = step;
            int key = switch (step % 9) {
                case 0 -> 0;
                case 1 -> Integer.MIN_VALUE;
                case 2 -> Integer.MAX_VALUE;
                case 3 -> -1;
                case 4 -> random.nextInt();
                default -> random.nextInt(2049) - 1024;
            };
            int value = (int) value(random, step);
            switch (random.nextInt(8)) {
                case 0, 1 -> c.eq(expected.put(key, value) == null, actual.put(key, value), "put");
                case 2 -> c.eq(expected.remove(key) != null, actual.remove(key), "remove");
                case 3 -> {
                    int old = random.nextBoolean() ? expected.getOrDefault(key, (int) MISSING) : value;
                    boolean replace = expected.containsKey(key) && expected.get(key) == old;
                    if (replace) expected.put(key, value);
                    c.eq(replace, actual.replace(key, old, value), "replace");
                }
                case 4 -> {
                    int old = random.nextBoolean() ? expected.getOrDefault(key, (int) MISSING) : value;
                    boolean remove = expected.containsKey(key) && expected.get(key) == old;
                    if (remove) expected.remove(key);
                    c.eq(remove, actual.remove(key, old), "conditional remove");
                }
                case 5, 6 -> {
                    int next = (expected.getOrDefault(key, 0) + value);
                    expected.put(key, next);
                    c.eq(next, actual.addTo(key, value), "addTo Java wraparound");
                }
                default -> c.eq(expected.getOrDefault(key, (int) MISSING), actual.getOrDefault(key, (int) MISSING), "lookup");
            }
            c.eq(expected.size(), actual.size(), "size");
            if ((step & 63) == 0) intIntSnapshot(c, actual, expected);
        }
        intIntSnapshot(c, actual, expected);
        actual.clear();
        for (int key : new int[] {0, 7}) {
            actual.put(key, Integer.MAX_VALUE);
            c.eq(Integer.MIN_VALUE, actual.addTo(key, 1), "overflow wraps at max");
            c.eq(Integer.MAX_VALUE, actual.addTo(key, -1), "underflow wraps at min");
        }
        actual.clear();
        actual.put(0, 1);
        M3IntIterator cursor = actual.keyIterator();
        actual.put(0, 2);
        c.eq(true, cursor.hasNext(), "replacement preserves iterator");
        c.eq(0, cursor.nextInt(), "zero iterator key");
        c.eq(false, cursor.hasNext(), "exhausted iterator");
        c.throwsType(NoSuchElementException.class, cursor::nextInt);
        cursor = actual.keyIterator();
        actual.put(1, 3);
        M3IntIterator stale = cursor;
        c.throwsType(ConcurrentModificationException.class, stale::hasNext);
        c.throwsType(ConcurrentModificationException.class, stale::nextInt);
        c.throwsType(NullPointerException.class, () -> actual.forEach(null));
        int capacity = actual.capacity();
        actual.clear();
        c.eq(capacity, actual.capacity(), "clear retains capacity");
        c.eq(true, actual.isEmpty(), "clear empty");
        iterator(c, actual.keyIterator(), new int[0]);

        c.done();
    }

    private static void intIntSnapshot(Check c, M3IntIntHashMap actual, Map<Integer, Integer> expected) {
        int[] snapshot = actual.keysToArray();
        c.eq(expected.size(), snapshot.length, "snapshot size");
        HashSet<Integer> seen = new HashSet<>();
        for (int key : snapshot) {
            c.eq(true, seen.add(key), "unique key");
            c.eq(true, expected.containsKey(key), "key in model");
            c.eq(expected.get(key), actual.getOrDefault(key, (int) MISSING), "snapshot value");
        }
        for (var entry : expected.entrySet()) {
            c.eq(true, actual.containsKey(entry.getKey()), "all model keys");
            c.eq(entry.getValue(), actual.getOrDefault(entry.getKey(), (int) MISSING), "all model values");
        }
        List<Integer> visited = new ArrayList<>();
        actual.forEach((key, value) -> { c.eq(expected.get(key), value, "forEach value"); visited.add(key); });
        c.array(snapshot, visited.stream().mapToInt(Integer::intValue).toArray(), "forEach slot order");
        if (expected.containsKey(0)) c.eq(0, snapshot[0], "zero sidecar first");
        iterator(c, actual.keyIterator(), snapshot);
        c.eq(expected.isEmpty(), actual.isEmpty(), "empty");
        c.eq(8L * actual.capacity(), actual.payloadBytes(), "parallel payload bytes");
        for (int value : new int[] {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            c.eq(expected.containsValue(value), actual.containsValue(value), "containsValue");
        }
        c.powerOfTwo(actual.capacity());
    }

    public static void intLong() {
        Check c = new Check("M3IntLongHashMap");
        c.throwsType(IllegalArgumentException.class, () -> new M3IntLongHashMap(-1));
        c.throwsType(IllegalArgumentException.class, () -> new M3IntLongHashMap(Integer.MAX_VALUE));
        M3IntLongHashMap actual = new M3IntLongHashMap();
        Map<Integer, Long> expected = new HashMap<>();
        c.eq(8, actual.capacity(), "initial capacity");
        actual.put(0, 0);
        for (int key = 1; key <= 5; key++) actual.put(key, key);
        c.eq(8, actual.capacity(), "zero sidecar does not consume threshold");
        actual.put(6, 6);
        c.eq(16, actual.capacity(), "six nonzero keys grow the initial table");
        actual.clear();
        for (int key = 0; key < 1024; key++) {
            long value = (long) (~key);
            expected.put(key, value);
            c.eq(true, actual.put(key, value), "dense warmup");
        }
        Random random = new Random(0x494e544c4f4e47L);
        for (int step = 0; step < 7000; step++) {
            c.step = step;
            int key = switch (step % 9) {
                case 0 -> 0;
                case 1 -> Integer.MIN_VALUE;
                case 2 -> Integer.MAX_VALUE;
                case 3 -> -1;
                case 4 -> random.nextInt();
                default -> random.nextInt(2049) - 1024;
            };
            long value = value(random, step);
            switch (random.nextInt(8)) {
                case 0, 1 -> c.eq(expected.put(key, value) == null, actual.put(key, value), "put");
                case 2 -> c.eq(expected.remove(key) != null, actual.remove(key), "remove");
                case 3 -> {
                    long old = random.nextBoolean() ? expected.getOrDefault(key, MISSING) : value;
                    boolean replace = expected.containsKey(key) && expected.get(key) == old;
                    if (replace) expected.put(key, value);
                    c.eq(replace, actual.replace(key, old, value), "replace");
                }
                case 4 -> {
                    long old = random.nextBoolean() ? expected.getOrDefault(key, MISSING) : value;
                    boolean remove = expected.containsKey(key) && expected.get(key) == old;
                    if (remove) expected.remove(key);
                    c.eq(remove, actual.remove(key, old), "conditional remove");
                }
                case 5, 6 -> {
                    long next = (expected.getOrDefault(key, (long) 0) + value);
                    expected.put(key, next);
                    c.eq(next, actual.addTo(key, value), "addTo Java wraparound");
                }
                default -> c.eq(expected.getOrDefault(key, MISSING), actual.getOrDefault(key, MISSING), "lookup");
            }
            c.eq(expected.size(), actual.size(), "size");
            if ((step & 63) == 0) intLongSnapshot(c, actual, expected);
        }
        intLongSnapshot(c, actual, expected);
        actual.clear();
        for (int key : new int[] {0, 7}) {
            actual.put(key, Long.MAX_VALUE);
            c.eq(Long.MIN_VALUE, actual.addTo(key, 1), "overflow wraps at max");
            c.eq(Long.MAX_VALUE, actual.addTo(key, -1), "underflow wraps at min");
        }
        actual.clear();
        actual.put(0, 1);
        M3IntIterator cursor = actual.keyIterator();
        actual.put(0, 2);
        c.eq(true, cursor.hasNext(), "replacement preserves iterator");
        c.eq(0, cursor.nextInt(), "zero iterator key");
        c.eq(false, cursor.hasNext(), "exhausted iterator");
        c.throwsType(NoSuchElementException.class, cursor::nextInt);
        cursor = actual.keyIterator();
        actual.put(1, 3);
        M3IntIterator stale = cursor;
        c.throwsType(ConcurrentModificationException.class, stale::hasNext);
        c.throwsType(ConcurrentModificationException.class, stale::nextInt);
        c.throwsType(NullPointerException.class, () -> actual.forEach(null));
        int capacity = actual.capacity();
        actual.clear();
        c.eq(capacity, actual.capacity(), "clear retains capacity");
        c.eq(true, actual.isEmpty(), "clear empty");
        iterator(c, actual.keyIterator(), new int[0]);
        for (int expectedSize : new int[] {0, 1, 5, 6, 13, 1024}) c.eq(new M3IntLongHashMap(expectedSize).payloadBytes(), M3IntLongHashMap.payloadBytesFor(expectedSize), "static payload estimate");
        c.throwsType(IllegalArgumentException.class, () -> M3IntLongHashMap.payloadBytesFor(-1));
        c.throwsType(IllegalArgumentException.class, () -> M3IntLongHashMap.payloadBytesFor(Integer.MAX_VALUE));
        c.done();
    }

    private static void intLongSnapshot(Check c, M3IntLongHashMap actual, Map<Integer, Long> expected) {
        int[] snapshot = actual.keysToArray();
        c.eq(expected.size(), snapshot.length, "snapshot size");
        HashSet<Integer> seen = new HashSet<>();
        for (int key : snapshot) {
            c.eq(true, seen.add(key), "unique key");
            c.eq(true, expected.containsKey(key), "key in model");
            c.eq(expected.get(key), actual.getOrDefault(key, MISSING), "snapshot value");
        }
        for (var entry : expected.entrySet()) {
            c.eq(true, actual.containsKey(entry.getKey()), "all model keys");
            c.eq(entry.getValue(), actual.getOrDefault(entry.getKey(), MISSING), "all model values");
        }
        List<Integer> visited = new ArrayList<>();
        actual.forEach((key, value) -> { c.eq(expected.get(key), value, "forEach value"); visited.add(key); });
        c.array(snapshot, visited.stream().mapToInt(Integer::intValue).toArray(), "forEach slot order");
        if (expected.containsKey(0)) c.eq(0, snapshot[0], "zero sidecar first");
        iterator(c, actual.keyIterator(), snapshot);
        c.eq(expected.isEmpty(), actual.isEmpty(), "empty");
        c.eq(12L * actual.capacity(), actual.payloadBytes(), "parallel payload bytes");
        for (long value : new long[] {0, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            c.eq(expected.containsValue(value), actual.containsValue(value), "containsValue");
        }
        c.powerOfTwo(actual.capacity());
    }

    public static void identity() {
        Check c = new Check("M3ObjectIdentityMap");
        c.throwsType(IllegalArgumentException.class, () -> new M3ObjectIdentityMap<>(-1));
        c.throwsType(IllegalArgumentException.class, () -> new M3ObjectIdentityMap<>(Integer.MAX_VALUE));
        M3ObjectIdentityMap<Object, Object> actual = new M3ObjectIdentityMap<>();
        IdentityHashMap<Object, Object> expected = new IdentityHashMap<>();
        Object[] keys = new Object[513];
        Object[] values = new Object[65];
        for (int i = 1; i < keys.length; i++) keys[i] = new EqualBox(i % 13, i);
        for (int i = 1; i < values.length; i++) values[i] = new EqualBox(i % 7, 1000 + i);
        Object fallback = new EqualBox(0, -1);
        c.eq(8, actual.capacity(), "initial capacity");
        for (int i = 0; i < keys.length; i++) {
            Object value = values[i % values.length];
            c.same(expected.put(keys[i], value), actual.put(keys[i], value), "warmup put");
        }
        identitySnapshot(c, actual, expected, keys, values, fallback);
        Random random = new Random(0x4944454e54495459L);
        for (int step = 0; step < 6000; step++) {
            c.step = step;
            Object key = keys[random.nextInt(keys.length)];
            Object value = values[random.nextInt(values.length)];
            switch (random.nextInt(7)) {
                case 0, 1 -> c.same(expected.put(key, value), actual.put(key, value), "put previous");
                case 2 -> c.same(expected.remove(key), actual.remove(key), "remove previous");
                case 3 -> {
                    Object old = expected.get(key);
                    Object result = old == null ? expected.put(key, value) : old;
                    c.same(result, actual.putIfAbsent(key, value), "putIfAbsent");
                }
                case 4 -> {
                    Object wanted = random.nextBoolean() ? expected.get(key) : value;
                    boolean remove = expected.containsKey(key) && expected.get(key) == wanted;
                    if (remove) expected.remove(key);
                    c.eq(remove, actual.remove(key, wanted), "conditional identity remove");
                }
                case 5 -> c.same(expected.getOrDefault(key, fallback), actual.getOrDefault(key, fallback), "lookup");
                default -> c.eq(expected.containsValue(value), actual.containsValue(value), "contains identity value");
            }
            c.eq(expected.size(), actual.size(), "size");
            if ((step & 63) == 0) identitySnapshot(c, actual, expected, keys, values, fallback);
        }
        identitySnapshot(c, actual, expected, keys, values, fallback);
        // Equal-valued references remain independent; null-valued presence differs from absence.
        Object equalA = new EqualBox(12, 9001), equalB = new EqualBox(12, 9002);
        Object equalValueA = new EqualBox(3, 9003), equalValueB = new EqualBox(3, 9004);
        actual.clear();
        actual.put(equalA, equalValueA);
        actual.put(equalB, null);
        c.eq(2, actual.size(), "equal objects have separate identities");
        c.same(null, actual.getOrDefault(equalB, fallback), "present null value");
        c.same(fallback, actual.getOrDefault(new EqualBox(12, 9005), fallback), "equal absent identity");
        c.eq(false, actual.containsValue(equalValueB), "value equality is identity");
        c.eq(false, actual.remove(equalA, equalValueB), "conditional removal is identity");
        actual.put(null, null);
        c.same(null, actual.keysToArray()[0], "null key first");
        c.same(null, actual.putIfAbsent(null, equalValueA), "null-valued null key is replaced");
        c.same(equalValueA, actual.get(null), "null key new value");
        c.throwsType(NullPointerException.class, () -> actual.forEach(null));
        c.throwsType(ConcurrentModificationException.class,
                () -> actual.forEach((key, value) -> actual.clear()));
        actual.put(equalA, equalValueA);
        actual.forEach((key, value) -> actual.put(key, equalValueB));
        c.same(equalValueB, actual.get(equalA), "nonstructural callback update");
        int capacity = actual.capacity();
        actual.clear();
        c.eq(capacity, actual.capacity(), "clear retains capacity");
        c.eq(true, actual.isEmpty(), "clear is empty");
        c.done();
    }

    private static void identitySnapshot(Check c, M3ObjectIdentityMap<Object, Object> actual,
            IdentityHashMap<Object, Object> expected, Object[] keys, Object[] values, Object fallback) {
        c.eq(expected.isEmpty(), actual.isEmpty(), "empty");
        c.eq(expected.size(), actual.size(), "snapshot size");
        c.eq(2L * actual.capacity(), actual.referenceSlots(), "reference slots");
        c.powerOfTwo(actual.capacity());
        for (Object key : keys) {
            c.eq(expected.containsKey(key), actual.containsKey(key), "identity key presence");
            c.same(expected.getOrDefault(key, fallback), actual.getOrDefault(key, fallback), "identity value");
        }
        for (Object value : values) c.eq(expected.containsValue(value), actual.containsValue(value), "identity value presence");
        Object[] snapshot = actual.keysToArray();
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
        List<Object> visited = new ArrayList<>();
        actual.forEach((key, value) -> {
            c.eq(true, expected.containsKey(key), "forEach existing key");
            c.same(expected.get(key), value, "forEach value identity");
            c.same(null, seen.put(key, Boolean.TRUE), "forEach no duplicate");
            visited.add(key);
        });
        c.eq(expected.size(), snapshot.length, "snapshot size");
        c.eq(snapshot.length, visited.size(), "forEach size");
        for (int i = 0; i < snapshot.length; i++) c.same(snapshot[i], visited.get(i), "snapshot traversal order");
        if (expected.containsKey(null)) c.same(null, snapshot[0], "null key first");
    }

    public static void longHash() {
        Check c = new Check("M3LongLongHashMap");
        c.throwsType(IllegalArgumentException.class, () -> new M3LongLongHashMap(-1));
        c.throwsType(IllegalArgumentException.class, () -> new M3LongLongHashMap(Integer.MAX_VALUE));
        M3LongLongHashMap actual = new M3LongLongHashMap();
        Map<Long, Long> expected = new HashMap<>();
        c.eq(8, actual.capacity(), "initial capacity");
        for (long key = 0; key < 1024; key++) {
            expected.put(key, ~key);
            c.eq(true, actual.put(key, ~key), "dense warmup");
        }
        // Remove enough entries to exercise same-capacity tombstone compaction, then reuse it.
        int retainedCapacity = actual.capacity();
        for (long key = 0; key < 900; key++) {
            expected.remove(key);
            c.eq(true, actual.remove(key), "compaction deletion");
        }
        c.eq(retainedCapacity, actual.capacity(), "deletion compaction retains capacity");
        Random random = new Random(0x4c4f4e4748415348L);
        for (int step = 0; step < 8000; step++) {
            c.step = step;
            long key = key(random, step), value = value(random, step);
            switch (random.nextInt(7)) {
                case 0, 1, 2 -> c.eq(expected.put(key, value) == null, actual.put(key, value), "put");
                case 3 -> c.eq(expected.remove(key) != null, actual.remove(key), "remove");
                case 4 -> {
                    long old = random.nextBoolean() ? expected.getOrDefault(key, MISSING) : value;
                    boolean replace = expected.containsKey(key) && expected.get(key) == old;
                    if (replace) expected.put(key, value);
                    c.eq(replace, actual.replace(key, old, value), "replace");
                }
                case 5 -> {
                    long old = random.nextBoolean() ? expected.getOrDefault(key, MISSING) : value;
                    boolean remove = expected.containsKey(key) && expected.get(key) == old;
                    if (remove) expected.remove(key);
                    c.eq(remove, actual.remove(key, old), "conditional remove");
                }
                default -> c.eq(expected.getOrDefault(key, MISSING), actual.getOrDefault(key, MISSING), "lookup");
            }
            c.eq(expected.size(), actual.size(), "size");
            if ((step & 63) == 0) longHashSnapshot(c, actual, expected);
        }
        longHashSnapshot(c, actual, expected);
        actual.clear();
        actual.put(0, Long.MIN_VALUE);
        M3LongIterator cursor = actual.keyIterator();
        actual.put(0, Long.MAX_VALUE);
        c.eq(true, cursor.hasNext(), "value replacement preserves iterator");
        c.eq(0, cursor.nextLong(), "zero key");
        c.eq(false, cursor.hasNext(), "iterator exhaustion");
        c.throwsType(NoSuchElementException.class, cursor::nextLong);
        cursor = actual.keyIterator();
        actual.put(1, -1);
        M3LongIterator stale = cursor;
        c.throwsType(ConcurrentModificationException.class, stale::hasNext);
        c.throwsType(ConcurrentModificationException.class, stale::nextLong);
        c.throwsType(NullPointerException.class, () -> actual.forEach(null));
        c.throwsType(ConcurrentModificationException.class, () -> actual.forEach((k, v) -> actual.clear()));
        retainedCapacity = actual.capacity();
        actual.clear();
        c.eq(retainedCapacity, actual.capacity(), "clear retains capacity");
        c.eq(true, actual.isEmpty(), "clear empty");
        c.done();
    }

    private static void longHashSnapshot(Check c, M3LongLongHashMap actual, Map<Long, Long> expected) {
        long[] snapshot = actual.keysToArray();
        c.eq(expected.size(), snapshot.length, "snapshot size");
        HashSet<Long> seen = new HashSet<>();
        for (long key : snapshot) {
            c.eq(true, seen.add(key), "unique key");
            c.eq(true, expected.containsKey(key), "key belongs to model");
            c.eq(expected.get(key), actual.getOrDefault(key, MISSING), "snapshot value");
        }
        for (var entry : expected.entrySet()) c.eq(entry.getValue(), actual.getOrDefault(entry.getKey(), MISSING), "all mappings");
        List<Long> visited = new ArrayList<>();
        actual.forEach((key, value) -> {
            c.eq(expected.get(key), value, "forEach value");
            visited.add(key);
        });
        c.array(snapshot, longs(visited), "forEach slot order");
        iterator(c, actual.keyIterator(), snapshot);
        c.eq(expected.isEmpty(), actual.isEmpty(), "empty");
        for (long value : EDGES) c.eq(expected.containsValue(value), actual.containsValue(value), "containsValue");
        c.powerOfTwo(actual.capacity());
    }

    public static void sortedMap() throws Exception {
        Check c = new Check("M3LongLongTreeMap");
        c.throwsType(IllegalArgumentException.class, () -> new M3LongLongTreeMap(-1));
        M3LongLongTreeMap actual = new M3LongLongTreeMap();
        NavigableMap<Long, Long> expected = new TreeMap<>();
        c.eq(8, actual.capacity(), "initial capacity");
        c.throwsType(NoSuchElementException.class, actual::firstKey);
        c.throwsType(NoSuchElementException.class, actual::lastKey);
        c.throwsType(NoSuchElementException.class, actual::pollFirstKey);
        c.throwsType(NoSuchElementException.class, actual::pollLastKey);
        c.throwsType(IllegalArgumentException.class, () -> actual.getOrThrow(1));
        Random random = new Random(0x534f525445444d41L);
        for (int step = 0; step < 7000; step++) {
            c.step = step;
            long key = key(random, step), value = value(random, step);
            switch (random.nextInt(9)) {
                case 0, 1, 2 -> c.eq(expected.put(key, value) == null, actual.put(key, value), "put");
                case 3 -> c.eq(expected.remove(key) != null, actual.remove(key), "remove");
                case 4 -> { actual.trimToSize(); c.eq(Math.max(8, expected.size()), actual.capacity(), "trim capacity"); }
                case 5 -> {
                    long[] batch = {key, key, key == Long.MAX_VALUE ? key : key + 1};
                    long[] values = {value, ~value, value ^ 0x77777777L};
                    int before = expected.size();
                    for (int i = 0; i < batch.length; i++) expected.put(batch[i], values[i]);
                    c.eq(expected.size() - before, actual.putAllSorted(batch, values), "batch additions");
                }
                case 6 -> { if (!expected.isEmpty()) c.eq(expected.pollFirstEntry().getKey(), actual.pollFirstKey(), "poll first"); }
                case 7 -> { if (!expected.isEmpty()) c.eq(expected.pollLastEntry().getKey(), actual.pollLastKey(), "poll last"); }
                default -> c.eq(expected.getOrDefault(key, MISSING), actual.getOrDefault(key, MISSING), "lookup");
            }
            c.eq(expected.size(), actual.size(), "size");
            sortedNavigation(c, actual, expected, key);
            if ((step & 63) == 0) sortedSnapshot(c, actual, expected);
        }
        sortedSnapshot(c, actual, expected);
        long[] beforeKeys = actual.keysToArray(), beforeValues = actual.valuesToArray();
        int capacity = actual.capacity();
        c.throwsType(IllegalArgumentException.class, () -> actual.putAllSorted(new long[] {2, 1}, new long[] {7, 8}));
        c.throwsType(IllegalArgumentException.class, () -> actual.putAllSorted(new long[] {2}, new long[0]));
        c.throwsType(NullPointerException.class, () -> actual.putAllSorted(null, new long[0]));
        c.array(beforeKeys, actual.keysToArray(), "invalid batch leaves keys");
        c.array(beforeValues, actual.valuesToArray(), "invalid batch leaves values");
        c.eq(capacity, actual.capacity(), "invalid batch leaves capacity");
        c.eq(0, actual.putAllSorted(new long[0], new long[0]), "empty batch");
        Method grow = M3LongLongTreeMap.class.getDeclaredMethod("growCapacity", int.class, int.class);
        grow.setAccessible(true);
        c.throwsType(OutOfMemoryError.class, () -> invoke(grow, null, 8, Integer.MAX_VALUE));
        c.throwsType(OutOfMemoryError.class, () -> invoke(grow, null, 8, -1));
        actual.clear();
        actual.put(2, 3);
        M3LongIterator iterator = actual.keyIterator();
        actual.put(2, 4);
        actual.trimToSize();
        c.eq(2, iterator.nextLong(), "nonstructural replacement and trim");
        iterator = actual.keyIterator();
        actual.put(3, 9);
        M3LongIterator stale = iterator;
        c.throwsType(ConcurrentModificationException.class, stale::hasNext);
        c.throwsType(ConcurrentModificationException.class, stale::nextLong);
        c.throwsType(ConcurrentModificationException.class, () -> actual.forEach((k, v) -> actual.clear()));
        c.throwsType(NullPointerException.class, () -> actual.forEach(null));
        c.done();
    }

    private static void sortedSnapshot(Check c, M3LongLongTreeMap actual, NavigableMap<Long, Long> expected) {
        long[] keys = longs(expected.keySet());
        c.array(keys, actual.keysToArray(), "signed key order");
        c.array(longs(expected.values()), actual.valuesToArray(), "aligned value order");
        iterator(c, actual.keyIterator(), keys);
        List<Long> visited = new ArrayList<>();
        actual.forEach((key, value) -> { c.eq(expected.get(key), value, "forEach alignment"); visited.add(key); });
        c.array(keys, longs(visited), "forEach signed order");
        List<Long> range = new ArrayList<>();
        actual.forEachInRange(-100, 100, (key, value) -> { c.eq(expected.get(key), value, "range alignment"); range.add(key); });
        c.array(longs(expected.subMap(-100L, true, 100L, false).keySet()), longs(range), "half-open range");
        actual.forEachInRange(7, 7, (k, v) -> { throw new AssertionError("empty range callback"); });
        actual.forEachInRange(8, 7, (k, v) -> { throw new AssertionError("inverted range callback"); });
        for (var entry : expected.entrySet()) c.eq(entry.getValue(), actual.getOrThrow(entry.getKey()), "getOrThrow");
        c.eq(expected.isEmpty(), actual.isEmpty(), "empty");
        c.ok(actual.capacity() >= Math.max(8, expected.size()), "capacity covers live entries");
    }

    private static void sortedNavigation(Check c, M3LongLongTreeMap actual, NavigableMap<Long, Long> expected, long key) {
        c.eq(orMissing(expected.floorKey(key)), actual.floorKey(key, MISSING), "floor");
        c.eq(orMissing(expected.ceilingKey(key)), actual.ceilingKey(key, MISSING), "ceiling");
        c.eq(orMissing(expected.lowerKey(key)), actual.lowerKey(key, MISSING), "lower");
        c.eq(orMissing(expected.higherKey(key)), actual.higherKey(key, MISSING), "higher");
        if (!expected.isEmpty()) { c.eq(expected.firstKey(), actual.firstKey(), "first"); c.eq(expected.lastKey(), actual.lastKey(), "last"); }
    }

    public static void patricia() {
        Check c = new Check("M3LongPatriciaTrie");
        c.throwsType(IllegalArgumentException.class, () -> new M3LongPatriciaTrie(-1));
        M3LongPatriciaTrie actual = new M3LongPatriciaTrie();
        NavigableMap<Long, Long> expected = new TreeMap<>(Long::compareUnsigned);
        c.eq(8, actual.capacity(), "initial capacity");
        c.throwsType(M3NotYetReceived.class, () -> actual.put(0, 1));
        c.throwsType(M3NotYetReceived.class, () -> actual.remove(0));
        c.throwsType(M3NotYetReceived.class, () -> actual.selectKey(0));
        c.throwsType(IllegalArgumentException.class, () -> actual.prefixCount(0, -1));
        c.throwsType(IllegalArgumentException.class, () -> actual.prefixKeys(0, 65));
        c.throwsType(NoSuchElementException.class, actual::firstKey);
        c.throwsType(NoSuchElementException.class, actual::lastKey);
        Random random = new Random(0x5041545249434941L);
        for (int step = 0; step < 8000; step++) {
            c.step = step;
            long key = step < 128 ? (step < 64 ? 1L << step : ~(1L << (step - 64))) : key(random, step);
            long value = value(random, step);
            if (step < 128 || random.nextInt(4) < 2) c.eq(expected.put(key, value) == null, actual.putKey(key, value), "putKey");
            else c.eq(expected.remove(key) != null, actual.removeKey(key), "removeKey");
            c.eq(expected.size(), actual.size(), "size");
            c.eq(expected.containsKey(key), actual.containsKey(key), "contains key");
            c.eq(expected.getOrDefault(key, MISSING), actual.getOrDefault(key, MISSING), "value");
            c.eq(orMissing(expected.floorKey(key)), actual.floorKeyOrDefault(key, MISSING), "unsigned floor");
            c.eq(orMissing(expected.ceilingKey(key)), actual.ceilingKeyOrDefault(key, MISSING), "unsigned ceiling");
            c.eq(orMissing(expected.lowerKey(key)), actual.lowerKeyOrDefault(key, MISSING), "unsigned lower");
            c.eq(orMissing(expected.higherKey(key)), actual.higherKeyOrDefault(key, MISSING), "unsigned higher");
            if ((step & 63) == 0) {
                patriciaSnapshot(c, actual, expected);
                for (int bits : new int[] {0, 1, 31, 32, 63, 64}) {
                    long mask = bits == 0 ? 0 : -1L << (64 - bits);
                    long[] selected = expected.keySet().stream().filter(k -> (k & mask) == (key & mask)).mapToLong(Long::longValue).toArray();
                    c.eq(selected.length, actual.prefixCount(key, bits), "prefix count bits=" + bits);
                    c.array(selected, actual.prefixKeys(key, bits), "prefix keys bits=" + bits);
                }
            }
        }
        patriciaSnapshot(c, actual, expected);
        for (long key : longs(expected.keySet())) c.eq(true, actual.removeKey(key), "remove all leaves");
        c.eq(true, actual.isEmpty(), "remove-to-empty");
        c.eq(MISSING, actual.higherKeyOrDefault(-1L, MISSING), "unsigned upper endpoint");
        c.eq(MISSING, actual.lowerKeyOrDefault(0L, MISSING), "unsigned lower endpoint");
        actual.putKey(0, 1);
        actual.putKey(Long.MAX_VALUE, 2);
        actual.putKey(Long.MIN_VALUE, 3);
        actual.putKey(-1L, 4);
        c.array(new long[] {0, Long.MAX_VALUE, Long.MIN_VALUE, -1}, actual.keysToArray(), "unsigned edge order");
        c.eq(Long.MIN_VALUE, actual.higherKeyOrDefault(Long.MAX_VALUE, MISSING), "signed boundary successor");
        c.eq(Long.MAX_VALUE, actual.lowerKeyOrDefault(Long.MIN_VALUE, MISSING), "signed boundary predecessor");
        M3LongIterator iterator = actual.keyIterator();
        actual.putKey(0, 77);
        c.eq(0, iterator.nextLong(), "value replacement keeps iterator");
        actual.removeKey(0);
        c.throwsType(ConcurrentModificationException.class, iterator::hasNext);
        int capacity = actual.capacity();
        actual.clear();
        c.eq(capacity, actual.capacity(), "clear retains capacity");
        c.eq(true, actual.isEmpty(), "clear empty");
        iterator(c, actual.keyIterator(), new long[0]);
        c.done();
    }

    private static void patriciaSnapshot(Check c, M3LongPatriciaTrie actual, NavigableMap<Long, Long> expected) {
        long[] keys = longs(expected.keySet());
        c.array(keys, actual.keysToArray(), "unsigned key order");
        iterator(c, actual.keyIterator(), keys);
        for (var entry : expected.entrySet()) c.eq(entry.getValue(), actual.getOrDefault(entry.getKey(), MISSING), "all trie mappings");
        c.eq(expected.isEmpty(), actual.isEmpty(), "empty");
        if (!expected.isEmpty()) { c.eq(expected.firstKey(), actual.firstKey(), "unsigned first"); c.eq(expected.lastKey(), actual.lastKey(), "unsigned last"); }
        c.eq(33L * actual.capacity(), actual.payloadBytes(), "seven-lane payload");
        c.powerOfTwo(actual.capacity());
    }

    public static void pagedSet() throws Exception {
        Check c = new Check("M3LongTreeSet");
        c.throwsType(IllegalArgumentException.class, () -> new M3LongTreeSet(-1));
        c.throwsType(IllegalArgumentException.class, () -> new M3LongTreeSet(0, 3));
        c.throwsType(IllegalArgumentException.class, () -> new M3LongTreeSet(0, (1 << 24) + 1));
        for (int pageCapacity : new int[] {4, 7, 64}) {
            M3LongTreeSet actual = new M3LongTreeSet(0, pageCapacity);
            TreeSet<Long> expected = new TreeSet<>();
            c.eq(pageCapacity, actual.capacity(), "initial one-page capacity");
            c.eq(0, actual.pageCount(), "initial no live pages");
            c.throwsType(NoSuchElementException.class, actual::first);
            c.throwsType(NoSuchElementException.class, actual::last);
            for (long key = 0; key < 1024; key++) { expected.add(key); c.eq(true, actual.add(key), "ascending page splits"); }
            for (long key = 1023; key >= 0; key -= 2) { expected.remove(key); c.eq(true, actual.remove(key), "page merges/free slots"); }
            for (long key = -1024; key < 0; key++) { expected.add(key); c.eq(true, actual.add(key), "free-slot reuse"); }
            actual.trimToSize();
            c.eq((long) Math.max(1, actual.pageCount()) * pageCapacity, actual.capacity(), "packed page capacity");
            pageSnapshot(c, actual, expected);
            Random random = new Random(0x5041474544534554L + pageCapacity);
            for (int step = 0; step < 6000; step++) {
                c.step = step;
                long key = key(random, step);
                switch (random.nextInt(10)) {
                    case 0, 1, 2, 3 -> c.eq(expected.add(key), actual.add(key), "add");
                    case 4, 5, 6 -> c.eq(expected.remove(key), actual.remove(key), "remove");
                    case 7 -> actual.trimToSize();
                    case 8 -> c.eq(expected.contains(key), actual.contains(key), "contains");
                    default -> {
                        if ((step & 127) == 0) {
                            int before = expected.size();
                            expected.removeIf(k -> (k & 15) == 3);
                            c.eq(before - expected.size(), actual.filterInPlace(k -> (k & 15) != 3), "filter");
                        }
                    }
                }
                c.eq(expected.size(), actual.size(), "size");
                c.eq(orMissing(expected.floor(key)), actual.floor(key, MISSING), "floor");
                c.eq(orMissing(expected.ceiling(key)), actual.ceiling(key, MISSING), "ceiling");
                c.eq(orMissing(expected.lower(key)), actual.lower(key, MISSING), "lower");
                c.eq(orMissing(expected.higher(key)), actual.higher(key, MISSING), "higher");
                if ((step & 63) == 0) pageSnapshot(c, actual, expected);
            }
            pageSnapshot(c, actual, expected);
            // Overflow preflight is reflected only to avoid allocating multi-gigabyte arrays.
            Method ensure = M3LongTreeSet.class.getDeclaredMethod("ensurePages", int.class);
            ensure.setAccessible(true);
            long[] before = actual.toArray();
            int capacity = actual.capacity();
            c.throwsType(IllegalStateException.class, () -> invoke(ensure, actual, Integer.MAX_VALUE));
            c.array(before, actual.toArray(), "capacity refusal leaves contents");
            c.eq(capacity, actual.capacity(), "capacity refusal leaves capacity");
            actual.clear();
            for (long key = 0; key < 10; key++) actual.add(key);
            RuntimeException marker = new RuntimeException("predicate witness");
            c.throwsSame(marker, () -> actual.filterInPlace(k -> { if (k == 5) throw marker; return (k & 1) == 0; }));
            c.array(new long[] {0, 2, 4, 5, 6, 7, 8, 9}, actual.toArray(), "throwing filter partial commit");
            M3LongIterator iterator = actual.iterator();
            actual.add(0);
            actual.trimToSize();
            c.eq(0, iterator.nextLong(), "duplicate/trim keep iterator");
            actual.add(100);
            c.throwsType(ConcurrentModificationException.class, iterator::hasNext);
            c.throwsType(ConcurrentModificationException.class, iterator::nextLong);
            c.throwsType(NullPointerException.class, () -> actual.filterInPlace(null));
            capacity = actual.capacity();
            actual.clear();
            c.eq(capacity, actual.capacity(), "clear retains lanes");
            actual.trimToSize();
            c.eq(pageCapacity, actual.capacity(), "empty trim one page");
            c.eq(0, actual.pageCount(), "empty no live pages");
            iterator(c, actual.iterator(), new long[0]);
        }
        c.done();
    }

    private static void pageSnapshot(Check c, M3LongTreeSet actual, TreeSet<Long> expected) {
        long[] keys = longs(expected);
        c.array(keys, actual.toArray(), "signed set order");
        iterator(c, actual.iterator(), keys);
        for (long key : keys) c.eq(true, actual.contains(key), "every set member");
        if (!expected.isEmpty()) { c.eq(expected.first(), actual.first(), "first"); c.eq(expected.last(), actual.last(), "last"); }
        c.ok(actual.capacity() >= expected.size(), "payload covers all values");
        c.eq(0, actual.capacity() % actual.pageCapacity(), "whole physical pages");
        c.eq(8L * actual.capacity() + 20L * (actual.capacity() / actual.pageCapacity()), actual.payloadBytes(), "aligned payload accounting");
        c.ok(actual.pageCount() <= actual.capacity() / actual.pageCapacity(), "live pages fit physical pages");
        Spliterator.OfLong cursor = actual.longSpliterator();
        c.eq(expected.size(), cursor.estimateSize(), "spliterator initial size");
        int wanted = Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SIZED;
        c.eq(wanted, cursor.characteristics(), "spliterator characteristics");
        c.same(null, cursor.trySplit(), "unsplittable cursor");
        c.same(null, cursor.getComparator(), "natural order comparator");
        List<Long> visited = new ArrayList<>();
        cursor.forEachRemaining((long key) -> visited.add(key));
        c.array(keys, longs(visited), "spliterator signed order");
    }

    private static long key(Random random, int step) {
        return switch (step % 11) {
            case 0, 1, 2, 3, 4, 5, 6 -> EDGES[step % 11];
            case 7 -> random.nextLong();
            case 8 -> 1L << random.nextInt(64);
            default -> random.nextInt(2049) - 1024L;
        };
    }

    private static long value(Random random, int step) {
        return (step & 7) == 0 ? EDGES[(step >>> 3) % EDGES.length] : random.nextLong();
    }

    private static long[] longs(Iterable<Long> values) {
        ArrayList<Long> boxed = new ArrayList<>();
        values.forEach(boxed::add);
        return boxed.stream().mapToLong(Long::longValue).toArray();
    }

    private static long orMissing(Long value) { return value == null ? MISSING : value; }

    private static void iterator(Check c, M3LongIterator iterator, long[] expected) {
        for (long key : expected) { c.eq(true, iterator.hasNext(), "iterator availability"); c.eq(key, iterator.nextLong(), "iterator order"); }
        c.eq(false, iterator.hasNext(), "iterator exhausted");
        c.throwsType(NoSuchElementException.class, iterator::nextLong);
    }

    private static void iterator(Check c, M3IntIterator iterator, int[] expected) {
        for (int key : expected) { c.eq(true, iterator.hasNext(), "iterator availability"); c.eq(key, iterator.nextInt(), "iterator order"); }
        c.eq(false, iterator.hasNext(), "iterator exhausted");
        c.throwsType(NoSuchElementException.class, iterator::nextInt);
    }

    private static Object invoke(Method method, Object receiver, Object... args) throws Throwable {
        try { return method.invoke(receiver, args); }
        catch (InvocationTargetException wrapped) { throw wrapped.getCause(); }
    }

    private static final class EqualBox {
        private final int group;
        private final int identity;
        private EqualBox(int group, int identity) { this.group = group; this.identity = identity; }
        @Override public boolean equals(Object other) { return other instanceof EqualBox box && group == box.group; }
        @Override public int hashCode() { return group; }
        @Override public String toString() { return "box:" + group + ":" + identity; }
    }

    @FunctionalInterface private interface Action { void run() throws Throwable; }

    private static final class Check {
        private final String owner;
        private int step = -1;
        private long checks;
        private Check(String owner) { this.owner = owner; }
        private void ok(boolean condition, String detail) {
            checks++;
            if (!condition) throw new AssertionError(owner + " step=" + step + ": " + detail);
        }
        private void eq(long expected, long actual, String detail) { ok(expected == actual, detail + " expected=" + expected + " actual=" + actual); }
        private void eq(boolean expected, boolean actual, String detail) { ok(expected == actual, detail + " expected=" + expected + " actual=" + actual); }
        private void same(Object expected, Object actual, String detail) { ok(expected == actual, detail + " identity mismatch"); }
        private void array(long[] expected, long[] actual, String detail) { ok(Arrays.equals(expected, actual), detail + " array mismatch"); }
        private void array(int[] expected, int[] actual, String detail) { ok(Arrays.equals(expected, actual), detail + " array mismatch"); }
        private void powerOfTwo(int capacity) { ok(capacity >= 8 && (capacity & (capacity - 1)) == 0, "power-of-two capacity=" + capacity); }
        private void throwsType(Class<? extends Throwable> type, Action action) {
            checks++;
            try { action.run(); } catch (Throwable caught) {
                if (type.isInstance(caught)) return;
                throw new AssertionError(owner + " step=" + step + ": expected " + type.getName(), caught);
            }
            throw new AssertionError(owner + " step=" + step + ": expected " + type.getName() + ", no exception");
        }
        private void throwsSame(Throwable expected, Action action) {
            checks++;
            try { action.run(); } catch (Throwable caught) { if (caught == expected) return; throw new AssertionError("exception identity", caught); }
            throw new AssertionError("missing expected exception");
        }
        private void done() { System.out.println("PASS " + owner + " checks=" + checks); }
    }
}
