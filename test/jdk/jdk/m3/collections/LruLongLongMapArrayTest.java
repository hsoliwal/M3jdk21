/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Primitive LRU preserves bounded access order using reused array-indexed lanes
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 *          jdk.m3.collections/jdk.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED LruLongLongMapArrayTest
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import jdk.internal.m3.collections.LruLongLongMap;

public final class LruLongLongMapArrayTest {
    private static long checks;

    public static void main(String[] args) throws Exception {
        for (int capacity : new int[]{1, 2, 3, 4, 7, 8, 16, 31, 32, 33, 64, 65, 100, 1024}) {
            for (int seed = 0; seed < 12; seed++) run(capacity, seed);
        }
        check(new LruLongLongMap().maxSize() == 100, "default Commons-aligned bound");
        expect(IllegalArgumentException.class, () -> new LruLongLongMap(0));
        expect(IllegalArgumentException.class, () -> new LruLongLongMap(-1));
        LruLongLongMap empty = new LruLongLongMap();
        expect(NoSuchElementException.class, empty::firstKey);
        expect(NoSuchElementException.class, empty::lastKey);
        check(empty.put(Long.MIN_VALUE, Long.MAX_VALUE) == 0L, "signed extremes put");
        check(empty.getOrDefault(Long.MIN_VALUE, 9L) == Long.MAX_VALUE, "signed extremes get");
        check(empty.remove(Long.MIN_VALUE) == Long.MAX_VALUE, "signed extremes remove");
        check(empty.remove(Long.MIN_VALUE) == 0L, "absent primitive removal");
        reentrantMutation();
        System.out.println("LruLongLongMapArrayTest PASS " + checks + " assertions");
    }

    private static void run(int capacity, int seed) throws Exception {
        LruLongLongMap actual = new LruLongLongMap(capacity);
        LinkedHashMap<Long, Long> expected = new LinkedHashMap<>(16, 0.75f, true);
        Random random = new Random(0xD00F12345L ^ ((long) capacity << 32) ^ seed);
        for (int step = 0; step < 2500; step++) {
            long key = switch (random.nextInt(25)) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> 0L;
                default -> random.nextInt(capacity * 3 + 31) - 17L;
            };
            int operation = random.nextInt(100);
            if (operation < 51) {
                long value = switch (random.nextInt(12)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    default -> random.nextLong();
                };
                Long previous = expected.put(key, value);
                while (expected.size() > capacity) expected.remove(expected.keySet().iterator().next());
                check(actual.put(key, value) == (previous == null ? 0L : previous), "put old");
            } else if (operation < 74) {
                Long previous = expected.get(key);
                check(actual.getOrDefault(key, 81L) == (previous == null ? 81L : previous),
                        "get and promote");
            } else if (operation < 83) {
                Long value = null;
                for (Map.Entry<Long, Long> entry : expected.entrySet()) {
                    if (entry.getKey() == key) { value = entry.getValue(); break; }
                }
                check(actual.peekOrDefault(key, 37L) == (value == null ? 37L : value),
                        "peek without promotion");
            } else if (operation < 91) {
                Long value = expected.remove(key);
                check(actual.remove(key) == (value == null ? 0L : value), "remove");
            } else if (operation < 96) {
                check(actual.containsKey(key) == expected.containsKey(key), "contains no touch");
            } else {
                actual.clear();
                expected.clear();
            }
            check(actual.size() == expected.size(), "size");
            check(actual.isFull() == (expected.size() == capacity), "isFull");
            List<Long> actualKeys = new ArrayList<>();
            List<Long> actualValues = new ArrayList<>();
            actual.forEach((k, v) -> { actualKeys.add(k); actualValues.add(v); });
            check(actualKeys.equals(new ArrayList<>(expected.keySet())), "LRU order");
            check(actualValues.equals(new ArrayList<>(expected.values())), "value order");
            if (!expected.isEmpty()) {
                Long first = expected.keySet().iterator().next();
                Long last = null;
                for (Long value : expected.keySet()) last = value;
                check(actual.firstKey() == first, "eldest");
                check(actual.lastKey() == last, "newest");
            }
            if ((step & 255) == 0) checkPrimitiveOwner(actual);
        }
        checkPrimitiveOwner(actual);
    }

    private static void reentrantMutation() {
        LruLongLongMap q = new LruLongLongMap(8);
        for (int i = 0; i < 8; i++) q.put(i, i * 10L);
        expect(IllegalStateException.class, () -> q.forEach((k, v) -> q.put(100, 1)));
        check(q.size() == 8, "failed callback leaves all members");
        check(!q.containsKey(100), "failed callback no mutation");
    }

    private static void checkPrimitiveOwner(LruLongLongMap actual) throws Exception {
        Field field = LruLongLongMap.class.getDeclaredField("entries");
        field.setAccessible(true);
        Object ordered = field.get(actual);
        Class<?> owner = ordered.getClass();
        for (String name : new String[]{"keys", "values", "previous", "next"}) {
            Field lane = owner.getDeclaredField(name);
            lane.setAccessible(true);
            Class<?> type = lane.getType();
            check(type == long[].class || type == int[].class, "primitive storage lane");
        }
        for (Field f : LruLongLongMap.class.getDeclaredFields()) {
            check(f.getType().isPrimitive() || f.getType() == owner,
                    "no one-per-entry wrapper retained in facade");
        }
    }

    private static void check(boolean okay, String label) {
        checks++;
        if (!okay) throw new AssertionError(label);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable e) {
            if (type.isInstance(e)) return;
            throw new AssertionError("wrong exception " + e, e);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
