/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * @test
 * @summary IdentityHashMap toString uses alternating array offsets without intermediate entries
 * @run main DirectOffsetString
 */

import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.ConcurrentModificationException;

/** JDK21 differential against the original AbstractMap entry-based formatting. */
public final class DirectOffsetString {
    private static long checks;
    private static final class Key {
        private final int value;
        Key(int value) { this.value = value; }
        @Override public boolean equals(Object o) {
            return o instanceof Key other && value == other.value;
        }
        @Override public int hashCode() { return value; }
        @Override public String toString() { return "K" + value; }
    }

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label + " check=" + checks);
    }

    private static void eq(String expected, String actual, String label) {
        check(Objects.equals(expected, actual),
                label + " expected=" + expected + " actual=" + actual);
    }

    private static String entryBased(IdentityHashMap<Object, Object> source) {
        StringBuilder builder = new StringBuilder("{");
        boolean comma = false;
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            if (comma) builder.append(", ");
            comma = true;
            Object key = entry.getKey(), value = entry.getValue();
            builder.append(key == source ? "(this Map)" : key).append('=')
                   .append(value == source ? "(this Map)" : value);
        }
        return builder.append('}').toString();
    }

    private static void randomized() {
        Random random = new Random(0x4d334944454e5459L);
        for (int trial = 0; trial < 360; trial++) {
            IdentityHashMap<Object, Object> map = new IdentityHashMap<>();
            Key[] keys = new Key[32];
            for (int i = 0; i < keys.length; i++) keys[i] = new Key(i % 11);
            for (int operation = 0; operation < 150; operation++) {
                int index = random.nextInt(keys.length);
                if (random.nextInt(5) == 0) {
                    map.remove(keys[index]);
                } else {
                    Object value = random.nextInt(7) == 0 ? null
                            : random.nextInt(3) == 0 ? map : new Key(random.nextInt(31));
                    map.put(keys[index], value);
                }
                if (operation % 11 == 0) {
                    // Equal-by-value keys must remain identity-distinct in the map.
                    eq(entryBased(map), map.toString(), "random identity array");
                    check(map.size() <= 32, "distinct identity slots");
                }
            }
            map.put(null, null);
            eq(entryBased(map), map.toString(), "null key/value mask");
            map.put(map, map);
            eq(entryBased(map), map.toString(), "self-key and self-value");
        }
    }

    private static final class EntryHidingIdentityMap extends IdentityHashMap<Object, Object> {
        @Override public java.util.Set<Map.Entry<Object, Object>> entrySet() {
            return java.util.Collections.emptySet();
        }
    }

    private static void subclassContract() {
        EntryHidingIdentityMap subclass = new EntryHidingIdentityMap();
        subclass.put(new Object(), "retained");
        eq("{}", subclass.toString(), "inherited AbstractMap string uses virtual EntrySet");
        check(subclass.equals(Collections.emptyMap()),
                "generic foreign-map equality uses subclass EntrySet semantics");
        check(!subclass.equals(Map.of("a", "b")),
                "subclass virtual empty EntrySet differs from nonempty foreign map");
        check(subclass.size() == 1, "physical mappings stay authoritative");
    }

    private static void foreignMapEquality() {
        Random random = new Random(0x4d334944454e4551L);
        for (int trial = 0; trial < 280; trial++) {
            IdentityHashMap<Object, Object> identity = new IdentityHashMap<>();
            HashMap<Object, Object> ordinary = new HashMap<>();
            Key[] keys = new Key[22];
            for (int i = 0; i < keys.length; i++) keys[i] = new Key(i % 9);
            for (int i = 0; i < 120; i++) {
                Key key = keys[random.nextInt(keys.length)];
                Object value = random.nextInt(6) == 0 ? null : new Key(random.nextInt(30));
                if (random.nextBoolean()) {
                    identity.put(key, value);
                    ordinary.put(key, value);
                } else {
                    identity.remove(key);
                    ordinary.remove(key);
                }
                boolean baseline = identity.entrySet().equals(ordinary.entrySet());
                check(baseline == identity.equals(ordinary),
                        "foreign Map identity equality matches old EntrySet semantics");
                check(baseline == identity.equals(Collections.unmodifiableMap(ordinary)),
                        "unmodifiable foreign Map entry contract");
            }
            identity.put(null, null);
            ordinary.put(null, null);
            check(identity.entrySet().equals(ordinary.entrySet())
                            == identity.equals(ordinary), "null mapping identity");
            check(identity.equals(identity), "reflexive identity shortcut");
            check(!identity.equals(null), "null not a Map");
        }
    }

    private static void failFastAfterUserString() {
        IdentityHashMap<Object, Object> map = new IdentityHashMap<>();
        Object first = new Object(), second = new Object(), third = new Object();
        map.put(first, "a");
        map.put(second, "b");
        map.put(third, "c");
        Iterator<Map.Entry<Object, Object>> entries = map.entrySet().iterator();
        check(entries.hasNext(), "nonempty");
        Object firstPhysicalKey = entries.next().getKey();
        Object newKey = new Object();
        map.put(firstPhysicalKey, new Object() {
            @Override public String toString() {
                map.put(newKey, "inserted");
                return "mutation";
            }
        });
        boolean detected = false;
        try {
            map.toString();
        } catch (ConcurrentModificationException expected) {
            detected = true;
        }
        check(detected, "match entry-iterator fail-fast after reentrant structural mutation");
    }

    public static void main(String[] args) {
        IdentityHashMap<Object, Object> empty = new IdentityHashMap<>();
        eq("{}", empty.toString(), "empty");
        randomized();
        subclassContract();
        foreignMapEquality();
        failFastAfterUserString();
        System.out.println("PASS DirectOffsetString checks=" + checks);
    }
}
