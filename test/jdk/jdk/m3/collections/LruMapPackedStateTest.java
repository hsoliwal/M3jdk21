/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary LRU dynamic-array state-mask packing preserves access order, callbacks and live views
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm --add-opens=jdk.m3.collections/jdk.internal.m3.collections=ALL-UNNAMED -Dm3.test.packed=true LruMapPackedStateTest
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import jdk.internal.m3.collections.LruMap;

public final class LruMapPackedStateTest {
    private static long checks;
    private static long callbackTrace;
    private static final class Key {
        private final int value;
        private Key(int value) { this.value = value; }
        @Override public int hashCode() {
            callbackTrace = Long.rotateLeft(callbackTrace ^ (long) value * 37, 5);
            return value >>> 1; // Deliberate two-key collision, avoiding HashMap treeification.
        }
        @Override public boolean equals(Object other) {
            callbackTrace = Long.rotateLeft(callbackTrace ^ (long) value * 13, 7);
            return other instanceof Key key && key.value == value;
        }
        @Override public String toString() { return "key" + value; }
    }

    public static void main(String[] args) throws Exception {
        for (int capacity : new int[]{1, 2, 3, 4, 7, 8, 16, 31, 32, 33, 63, 64, 65, 100, 128, 1025}) {
            for (int seed = 0; seed < 8; seed++) test(capacity, seed);
        }
        testIterators();
        expect(IllegalArgumentException.class, () -> new LruMap<>(0));
        expect(IllegalArgumentException.class, () -> new LruMap<>(-1));
        check(new LruMap<>().maxSize() == 100, "default maximum matches original");
        System.out.printf("LruMapPackedStateTest PASS %d checks callback=%016x%n", checks, callbackTrace);
    }

    private static void test(int capacity, int seed) throws Exception {
        LruMap<Key, String> actual = new LruMap<>(capacity);
        LinkedHashMap<Key, String> expected = new LinkedHashMap<>(16, 0.75f, true);
        Random random = new Random(0x5C0FFEE1234L ^ ((long) capacity << 31) ^ seed);
        for (int step = 0; step < 2400; step++) {
            int operation = random.nextInt(100);
            Key key = random.nextInt(20) == 0 ? null : new Key(random.nextInt(capacity * 2 + 11));
            if (operation < 48) {
                String value = random.nextInt(10) == 0 ? null : "value" + random.nextInt(1000);
                String old = expected.put(key, value);
                while (expected.size() > capacity) expected.remove(expected.keySet().iterator().next());
                check(Objects.equals(actual.put(key, value), old), "put old");
            } else if (operation < 68) {
                check(Objects.equals(actual.get(key), expected.get(key)), "read and promote");
            } else if (operation < 77) {
                // An oracle read must not change access order for a peek.
                String value = null;
                for (var entry : expected.entrySet()) {
                    if (Objects.equals(entry.getKey(), key)) { value = entry.getValue(); break; }
                }
                check(Objects.equals(actual.get(key, false), value), "peek without promotion");
            } else if (operation < 87) {
                check(Objects.equals(actual.remove(key), expected.remove(key)), "remove");
            } else if (operation < 92) {
                check(actual.containsKey(key) == expected.containsKey(key), "contains without promotion");
            } else if (operation < 96) {
                check(actual.asMap().equals(expected), "map equality");
            } else {
                actual.clear();
                expected.clear();
            }
            check(actual.size() == expected.size(), "size");
            check(actual.isFull() == (expected.size() == capacity), "isFull");
            check(actual.maxSize() == capacity, "maxSize");
            check(new ArrayList<>(actual.asMap().keySet()).equals(new ArrayList<>(expected.keySet())),
                    "LRU to MRU order");
            if (!expected.isEmpty()) {
                Key first = expected.keySet().iterator().next();
                Key last = null;
                for (Key current : expected.keySet()) last = current;
                check(Objects.equals(actual.firstKey(), first), "first key");
                check(Objects.equals(actual.lastKey(), last), "last key");
            }
            if ((step & 127) == 0) inspectMask(actual, Boolean.getBoolean("m3.test.packed"));
        }
        inspectMask(actual, Boolean.getBoolean("m3.test.packed"));
    }

    private static void testIterators() {
        LruMap<Integer, Integer> actual = new LruMap<>(4);
        for (int i = 0; i < 4; i++) actual.put(i, i + 10);
        Iterator<Map.Entry<Integer, Integer>> walk = actual.asMap().entrySet().iterator();
        check(walk.next().getKey() == 0, "iterator first");
        actual.get(0);
        expect(ConcurrentModificationException.class, walk::next);
        Iterator<Map.Entry<Integer, Integer>> next = actual.asMap().entrySet().iterator();
        Map.Entry<Integer, Integer> entry = next.next();
        check(entry.setValue(42) == 11, "live setValue old");
        check(Objects.equals(actual.get(entry.getKey(), false), 42), "live setValue value");
        next.remove();
        check(actual.size() == 3, "iterator removal");
        expect(IllegalStateException.class, next::remove);
        actual.clear();
        check(actual.asMap().isEmpty(), "cleared view");
    }

    /** Candidate-private invariant. Baseline behavior can run without this layout assertion. */
    private static void inspectMask(LruMap<?, ?> map, boolean requirePacked) throws Exception {
        Field f = LruMap.class.getDeclaredField("stateMaskWords");
        f.setAccessible(true);
        long[] words = (long[]) f.get(map);
        Field values = LruMap.class.getDeclaredField("keyLane");
        values.setAccessible(true);
        int capacity = ((Object[]) values.get(map)).length;
        if (requirePacked) {
            check(words.length == (capacity + 31) / 32, "two bits per physical slot");
            Field usedField = LruMap.class.getDeclaredField("used");
            usedField.setAccessible(true);
            int used = usedField.getInt(map);
            Field headField = LruMap.class.getDeclaredField("head");
            headField.setAccessible(true);
            Field nextField = LruMap.class.getDeclaredField("nextLane");
            nextField.setAccessible(true);
            int[] next = (int[]) nextField.get(map);
            boolean[] live = new boolean[capacity];
            int count = 0;
            for (int slot = headField.getInt(map); slot >= 0; slot = next[slot]) {
                check(slot < used && !live[slot], "live slot link");
                live[slot] = true;
                count++;
            }
            check(count == map.size(), "live slot count");
            for (int slot = 0; slot < capacity; slot++) {
                int state = (int) (words[slot >>> 5] >>> ((slot & 31) << 1) & 3L);
                check(state == (slot >= used ? 0 : live[slot] ? 1 : 2),
                        "packed occupancy/deletion state");
            }
        }
    }

    private static void check(boolean good, String label) {
        checks++;
        if (!good) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("expected " + kind + " got " + failure, failure);
        }
        throw new AssertionError("expected " + kind.getName());
    }
}
