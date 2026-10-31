/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JDK-8371656 HashMap copy fast path preserves Map and subclass contracts
 * @run main/othervm -Dm3.hashmap.fastExpected=true M3HashMapBulkCopyTest
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public final class M3HashMapBulkCopyTest {
    private static final boolean FAST =
            Boolean.parseBoolean(System.getProperty("m3.hashmap.fastExpected", "true"));
    private static int checks;

    private static final class Key {
        static int hashCalls;
        final int id;
        final int hash;
        Key(int id, int hash) {
            this.id = id;
            this.hash = hash;
        }
        @Override public int hashCode() {
            hashCalls++;
            return hash;
        }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && id == key.id;
        }
    }

    private static final class Subclass extends HashMap<Key, Integer> {
        int entrySetCalls;
        @Override public Set<Map.Entry<Key, Integer>> entrySet() {
            entrySetCalls++;
            return super.entrySet();
        }
    }

    public static void main(String[] args) {
        for (int n : new int[]{1, 5, 25, 150, 200}) {
            exactHashMap(n, false);
            exactHashMap(n, true);
        }
        fallbackSubclassAndOrder();
        nullAndSelfCopy();
        randomized();
        System.out.println("M3_HASHMAP_BULK_COPY_PASS checks=" + checks + " fast=" + FAST);
    }

    private static void exactHashMap(int n, boolean collisions) {
        HashMap<Key, Integer> source = new HashMap<>();
        for (int i = 0; i < n; i++) {
            source.put(new Key(i, collisions ? 7 : i * 33 + 1), i * 7);
        }
        Key.hashCalls = 0;
        HashMap<Key, Integer> copy = new HashMap<>(source);
        checkCalls(Key.hashCalls, n, "constructor " + n);
        check(copy.equals(source), "constructor values " + n);

        Map<Key, Integer> wrapped = Collections.unmodifiableMap(source);
        Key.hashCalls = 0;
        HashMap<Key, Integer> wrappedCopy = new HashMap<>(wrapped);
        checkCalls(Key.hashCalls, n, "wrapped constructor " + n);
        check(wrappedCopy.equals(source), "wrapped values " + n);

        HashMap<Key, Integer> target = new HashMap<>();
        Key.hashCalls = 0;
        target.putAll(source);
        checkCalls(Key.hashCalls, n, "putAll " + n);
        check(target.equals(source), "putAll values " + n);

        target.clear();
        Key.hashCalls = 0;
        target.putAll(wrapped);
        checkCalls(Key.hashCalls, n, "wrapped putAll " + n);
        check(target.equals(source), "wrapped putAll values " + n);
    }

    private static void checkCalls(int calls, int count, String label) {
        check(FAST ? calls == 0 : calls >= count, label + " hashCalls=" + calls);
    }

    private static void fallbackSubclassAndOrder() {
        Subclass source = new Subclass();
        for (int i = 0; i < 40; i++) source.put(new Key(i, i), i);
        Key.hashCalls = 0;
        HashMap<Key, Integer> dest = new HashMap<>(source);
        check(source.entrySetCalls > 0, "subclass entrySet respected");
        check(Key.hashCalls > 0, "subclass remains generic");
        check(dest.equals(source), "subclass content");

        LinkedHashMap<Integer, Integer> ordered = new LinkedHashMap<>();
        for (int i = 0; i < 50; i++) ordered.put(i, i * 19);
        LinkedHashMap<Integer, Integer> receiving = new LinkedHashMap<>();
        receiving.putAll(ordered);
        check(receiving.keySet().equals(ordered.keySet()), "LinkedHashMap keys");
        check(receiving.keySet().stream().toList().equals(
                ordered.keySet().stream().toList()), "LinkedHashMap order");

        TreeMap<Integer, Integer> tree = new TreeMap<>();
        tree.putAll(ordered);
        check(new HashMap<>(tree).equals(ordered), "TreeMap generic");
    }

    private static void nullAndSelfCopy() {
        HashMap<Key, Integer> data = new HashMap<>();
        data.put(null, -1);
        data.put(new Key(1, 7), 10);
        data.put(new Key(2, 7), null);
        data.putAll(data);
        check(data.size() == 3 && data.get(null) == -1, "self copy/null");
        check(new HashMap<>(Collections.unmodifiableMap(data)).equals(data),
                "null wrapped source");
        try {
            data.putAll(null);
            throw new AssertionError("null putAll did not throw");
        } catch (NullPointerException expected) {
            checks++;
        }
        try {
            new HashMap<Key, Integer>((Map<Key, Integer>) null);
            throw new AssertionError("null constructor did not throw");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static void randomized() {
        Random random = new Random(0x8371656L);
        for (int trial = 0; trial < 256; trial++) {
            HashMap<Integer, Integer> src = new HashMap<>();
            int count = random.nextInt(300);
            for (int i = 0; i < count; i++) {
                int key = random.nextInt(190);
                src.put(key, random.nextInt(1000));
            }
            HashMap<Integer, Integer> dest = new HashMap<>();
            for (int i = 0; i < 40; i++) dest.put(random.nextInt(190), i);
            HashMap<Integer, Integer> reference = new HashMap<>(dest);
            for (Map.Entry<Integer, Integer> e : src.entrySet()) {
                reference.put(e.getKey(), e.getValue());
            }
            dest.putAll(src);
            check(dest.equals(reference), "random putAll=" + trial);
            check(new HashMap<>(src).equals(src), "random constructor=" + trial);
            check(new HashMap<>(Collections.unmodifiableMap(src)).equals(src),
                    "random wrapped=" + trial);
            check(Objects.equals(dest.get(-1), reference.get(-1)), "absent key");
        }
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
