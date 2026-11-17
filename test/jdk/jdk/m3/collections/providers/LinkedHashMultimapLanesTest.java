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
 * @summary LinkedHashMultimap on array lanes against a model (LinkedHashMap of LinkedHashSet plus
 *          the multimap-wide insertion order): every order, size, lookup and view removal on
 *          random operations, colliding values (hash-flooding fallback), serialization, and the
 *          documented behaviour of an iteration suspended while the multimap changes
 * @modules jdk.m3.collections/jdk.m3.collections.guava.collect:+open
 * @run main/othervm -Xmx512m LinkedHashMultimapLanesTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import jdk.m3.collections.guava.collect.LinkedHashMultimap;

public class LinkedHashMultimapLanesTest {

    static long checks;

    static void same(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
            throw new AssertionError(what + ": expected " + expected + " actual " + actual);
        }
    }

    /** A value with a chosen hash, so that values collide and flood a key's value table. */
    record V(int id, int hash) implements Serializable {
        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof V v && v.id == id;
        }

        @Override
        public String toString() {
            return "v" + id;
        }
    }

    /** The model: keys in first-insertion order, values per key and all mappings in order. */
    static final class Model {
        final LinkedHashMap<Object, LinkedHashSet<Object>> byKey = new LinkedHashMap<>();
        final List<Map.Entry<Object, Object>> order = new ArrayList<>();

        boolean put(Object k, Object v) {
            if (byKey.computeIfAbsent(k, x -> new LinkedHashSet<>()).add(v)) {
                order.add(new java.util.AbstractMap.SimpleImmutableEntry<>(k, v));
                return true;
            }
            return false;
        }

        boolean remove(Object k, Object v) {
            LinkedHashSet<Object> values = byKey.get(k);
            if (values == null || !values.remove(v)) {
                return false;
            }
            if (values.isEmpty()) {
                byKey.remove(k);
            }
            order.removeIf(e -> Objects.equals(e.getKey(), k) && Objects.equals(e.getValue(), v));
            return true;
        }

        List<Object> removeAll(Object k) {
            LinkedHashSet<Object> values = byKey.remove(k);
            order.removeIf(e -> Objects.equals(e.getKey(), k));
            return values == null ? List.of() : new ArrayList<>(values);
        }

        List<Object> replaceValues(Object k, List<Object> values) {
            if (values.isEmpty()) {
                return removeAll(k);
            }
            LinkedHashSet<Object> set = byKey.computeIfAbsent(k, x -> new LinkedHashSet<>());
            List<Object> old = new ArrayList<>(set);
            set.clear();
            order.removeIf(e -> Objects.equals(e.getKey(), k));
            for (Object v : values) {
                if (set.add(v)) {
                    order.add(new java.util.AbstractMap.SimpleImmutableEntry<>(k, v));
                }
            }
            return old;
        }
    }

    public static void main(String[] args) throws Exception {
        for (long seed = 1; seed <= 30; seed++) {
            run(seed, 8_000, 1 + (int) (seed % 9), 2 + (int) (seed % 23), (int) (seed % 3));
        }
        run(100L, 80_000, 50, 500, 0);
        run(101L, 40_000, 2, 80, 2);
        suspended();
        retention();
        growth();
        valueStreams();
        copyReserved();
        forEachEmptied();
        emptiedByRemove();
        keyRetention();
        flooding();
        System.out.println("LinkedHashMultimapLanesTest: " + checks + " checks passed");
    }

    static Object key(Random random, int keys) {
        int k = random.nextInt(keys + 1);
        return k == keys ? null : "k" + k;
    }

    static Object value(Random random, int values, int hashKind) {
        int id = random.nextInt(values + 1);
        if (id == values) {
            return null;
        }
        int hash = switch (hashKind) {
            case 1 -> id & 1;
            case 2 -> 7;
            default -> id * 31;
        };
        return new V(id, hash);
    }

    static void run(long seed, int steps, int keys, int values, int hashKind) throws Exception {
        Random random = new Random(seed);
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        Model model = new Model();
        for (int step = 0; step < steps; step++) {
            String at = "seed " + seed + " step " + step;
            Object k = key(random, keys);
            Object v = value(random, values, hashKind);
            switch (random.nextInt(12)) {
                case 0, 1, 2, 3 -> same(model.put(k, v), m.put(k, v), at + " put");
                case 4, 5 -> same(model.remove(k, v), m.remove(k, v), at + " remove");
                case 6 -> same(model.removeAll(k), m.removeAll(k), at + " removeAll");
                case 7 -> {
                    List<Object> vs = new ArrayList<>();
                    for (int i = random.nextInt(6); i > 0; i--) {
                        vs.add(value(random, values, hashKind));
                    }
                    same(model.replaceValues(k, vs), m.replaceValues(k, vs), at + " replaceValues");
                }
                case 8 -> {
                    if (random.nextInt(25) == 0) {
                        m.clear();
                        model.byKey.clear();
                        model.order.clear();
                    }
                }
                case 9 -> {
                    int modulus = 2 + random.nextInt(3);
                    for (Iterator<Map.Entry<Object, Object>> it = m.entries().iterator();
                            it.hasNext(); ) {
                        Map.Entry<Object, Object> e = it.next();
                        if (Objects.hashCode(e.getValue()) % modulus == 0) {
                            it.remove();
                            model.remove(e.getKey(), e.getValue());
                        }
                    }
                }
                case 10 -> {
                    boolean removed = m.get(k).remove(v);
                    same(model.remove(k, v), removed, at + " get.remove");
                }
                default -> same(model.byKey.containsKey(k) && model.byKey.get(k).contains(v),
                        m.containsEntry(k, v), at + " containsEntry");
            }
            check(m, model, at);
            if (random.nextInt(500) == 0) {
                @SuppressWarnings("unchecked")
                LinkedHashMultimap<Object, Object> copy =
                        (LinkedHashMultimap<Object, Object>) roundTrip(m);
                check(copy, model, at + " serialized");
                same(m, copy, at + " equals after serialization");
            }
        }
    }

    static void check(LinkedHashMultimap<Object, Object> m, Model model, String at) {
        same(model.order.size(), m.size(), at + " size");
        same(model.order, new ArrayList<>(m.entries()), at + " entries");
        same(model.byKey.keySet(), new ArrayList<>(m.keySet()), at + " keySet");
        List<Object> values = new ArrayList<>();
        model.order.forEach(e -> values.add(e.getValue()));
        same(values, new ArrayList<>(m.values()), at + " values");
        List<Object> each = new ArrayList<>();
        m.forEach((key, value) -> each.add(key + "=" + value));
        same(model.order, each, at + " forEach");
        for (Map.Entry<Object, LinkedHashSet<Object>> e : model.byKey.entrySet()) {
            same(e.getValue(), new ArrayList<>(m.get(e.getKey())), at + " get " + e.getKey());
        }
    }

    static Object roundTrip(Object o) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        try (ObjectInputStream in =
                new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }

    /**
     * An entries() iteration suspended while the multimap changes: removing the entry just returned
     * or the next one behaves as upstream (the next one is still returned); a clear ends the
     * iteration; a next entry removed and its slot taken by a new entry throws.
     */
    static void suspended() {
        same("[k0=v0, k1=v1, k0=v2, k1=v3, k0=v4, k1=v5] size 5", scenario(1),
                "remove the entry just returned");
        same("[k0=v0, k1=v1, k0=v2, k1=v3, k0=v4, k1=v5] size 5", scenario(2),
                "remove the next entry");
        same("[k0=v0, k1=v1] size 0", scenario(3), "clear");
        // upstream also returns the removed k0=v2; the lanes skip an entry removed further ahead
        same("[k0=v0, k1=v1, k1=v3, k0=v4, k1=v5] size 4", scenario(4),
                "remove the next entry and the one after it");
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 9; i++) {
            m.put("k", "v" + i);
        }
        Iterator<Map.Entry<Object, Object>> it = m.entries().iterator();
        it.next();
        m.remove("k", "v1");
        m.put("k", "new");
        String outcome;
        try {
            while (it.hasNext()) {
                it.next();
            }
            outcome = "no exception";
        } catch (ConcurrentModificationException expected) {
            outcome = "ConcurrentModificationException";
        }
        same("ConcurrentModificationException", outcome, "next slot reused");
        same("[k0=a0] then ConcurrentModificationException", fifo(),
                "freed slots reused oldest first");
        same("[k=a0, k=a1] ended", clearedAndRefilled(), "an emptied multimap ends the iteration");
        same("[k=a0, k=a1, k=a2, k=a3, k=a4, k=a5, k=b1] ended", freshFirst(),
                "a fresh slot is taken while the lanes have one");
        same("[k=a0] then ConcurrentModificationException", forEachReuse(),
                "forEach whose callback frees the current slot and a put takes it");
        Iterator<Object> values = m.get("k").iterator();
        values.next();
        m.put("k", "other");
        try {
            values.next();
            outcome = "no exception";
        } catch (ConcurrentModificationException expected) {
            outcome = "ConcurrentModificationException";
        }
        same("ConcurrentModificationException", outcome, "value set iterator after a change");
    }

    /**
     * Six values in lanes of seven slots; the iteration is suspended on a1; a1 and then a2 are
     * removed and two values put: the first takes the fresh slot, the second the oldest freed slot,
     * a1's, so the iteration throws (with the newest freed slot, a2's, it would jump to the new
     * value and skip a3 to a5 silently).
     */
    static String fifo() {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 6; i++) {
            m.put("k" + (i % 2), "a" + i);
        }
        Iterator<Map.Entry<Object, Object>> it = m.entries().iterator();
        List<String> seen = new ArrayList<>();
        seen.add(String.valueOf(it.next()));
        m.remove("k1", "a1");
        m.remove("k0", "a2");
        for (int i = 1; i <= 2; i++) {
            m.put("k0", "b" + i);
        }
        try {
            while (it.hasNext()) {
                seen.add(String.valueOf(it.next()));
            }
            return seen + " ended";
        } catch (ConcurrentModificationException e) {
            return seen + " then ConcurrentModificationException";
        }
    }

    /**
     * Six values in lanes of seven slots; the iteration is suspended on a1; a1 is removed and b1
     * put into the fresh slot (not a1's), so the iteration goes on to the end.
     */
    static String freshFirst() {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 6; i++) {
            m.put("k", "a" + i);
        }
        Iterator<Map.Entry<Object, Object>> it = m.entries().iterator();
        List<String> seen = new ArrayList<>();
        seen.add(String.valueOf(it.next()));
        m.remove("k", "a1");
        m.put("k", "b1");
        try {
            while (it.hasNext()) {
                seen.add(String.valueOf(it.next()));
            }
            return seen + " ended";
        } catch (ConcurrentModificationException e) {
            return seen + " then ConcurrentModificationException";
        }
    }

    /** The iteration suspended on a1; clear; two values put into the kept lanes; it ends. */
    static String clearedAndRefilled() {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 4; i++) {
            m.put("k", "a" + i);
        }
        Iterator<Map.Entry<Object, Object>> it = m.entries().iterator();
        List<String> seen = new ArrayList<>();
        seen.add(String.valueOf(it.next()));
        m.clear();
        m.put("k", "n10");
        m.put("k", "n11");
        while (it.hasNext()) {
            seen.add(String.valueOf(it.next()));
        }
        return seen + " ended";
    }

    /** Nine values fill the lanes; forEach's callback removes the current value and puts one. */
    static String forEachReuse() {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 9; i++) {
            m.put("k", "a" + i);
        }
        List<String> seen = new ArrayList<>();
        try {
            m.forEach((key, value) -> {
                seen.add(key + "=" + value);
                if (value.equals("a0")) {
                    m.remove("k", "a0");
                    m.put("k", "w0");
                }
            });
            return seen + " ended";
        } catch (ConcurrentModificationException e) {
            return seen + " then ConcurrentModificationException";
        }
    }

    /** Removed, replaced and cleared values become collectable. */
    static void retention() {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 100; i++) {
            m.put("k" + (i % 7), "v" + i);
        }
        for (int how = 0; how < 4; how++) {
            WeakReference<Object> held = putFresh(m, how);
            switch (how) {
                case 0 -> m.remove("held", heldValue(m));
                case 1 -> m.removeAll("held");
                case 2 -> m.replaceValues("held", List.of("other"));
                default -> m.clear();
            }
            for (int i = 0; i < 50 && held.get() != null; i++) {
                System.gc();
            }
            same(null, held.get(), "value kept after removal " + how);
        }
    }

    static WeakReference<Object> putFresh(LinkedHashMultimap<Object, Object> m, int how) {
        Object value = new String("fresh " + how);
        m.put("held", value);
        return new WeakReference<>(value);
    }

    static Object heldValue(LinkedHashMultimap<Object, Object> m) {
        return m.get("held").iterator().next();
    }

    /** The lanes grow by two slots or a quarter, whichever is more, and are dropped when large. */
    static void growth() throws ReflectiveOperationException {
        java.lang.reflect.Field capacity = LinkedHashMultimap.class.getDeclaredField("capacity");
        capacity.setAccessible(true);
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        List<Integer> steps = new ArrayList<>();
        int last = 0;
        for (int i = 0; i < 400; i++) {
            m.put("k" + (i % 13), i);
            int now = capacity.getInt(m);
            if (now != last) {
                steps.add(now);
                last = now;
            }
        }
        same("[1, 3, 5, 7, 9, 11, 13, 16, 20, 25, 31, 38, 47, 58, 72, 90, 112, 140, 175, 218, 272, "
                + "340, 425]", steps, "lane capacities");
        m.clear();
        same(0, capacity.getInt(m), "lanes above 64 slots dropped when emptied");
        m.put("k", 1);
        m.remove("k", 1);
        same(1, capacity.getInt(m), "lanes of 64 slots or fewer kept when emptied");
        for (int i = 0; i < 58; i++) {
            m.put("k" + (i % 13), i);
        }
        same(58, capacity.getInt(m), "58 slots");
        m.clear();
        same(58, capacity.getInt(m), "lanes of 58 slots kept when emptied");
    }

    /** values() streams and spliterators: contents, characteristics, one value under two keys. */
    static void valueStreams() {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        m.put("k1", "v");
        m.put("k2", "v");
        m.put("k1", "w");
        same("[v, v, w]", m.values().stream().toList(), "values().stream() contents");
        same(2L, m.values().stream().distinct().count(), "distinct over a value under two keys");
        same(java.util.Spliterator.ORDERED | java.util.Spliterator.SIZED
                | java.util.Spliterator.SUBSIZED, m.values().spliterator().characteristics(),
                "values().spliterator() characteristics");
        List<Object> seen = new ArrayList<>();
        m.values().spliterator().forEachRemaining(seen::add);
        same("[v, v, w]", seen, "values().spliterator() contents");
    }

    /** create(Multimap) reserves the copied size at once (growth steps would give 112). */
    static void copyReserved() throws ReflectiveOperationException {
        java.lang.reflect.Field capacity = LinkedHashMultimap.class.getDeclaredField("capacity");
        capacity.setAccessible(true);
        LinkedHashMultimap<Object, Object> source = LinkedHashMultimap.create();
        for (int i = 0; i < 100; i++) {
            source.put("k" + (i % 7), i);
        }
        LinkedHashMultimap<Object, Object> copy = LinkedHashMultimap.create(source);
        same(100, capacity.getInt(copy), "a copy reserves its size at once");
        same(source.entries().toString(), copy.entries().toString(), "a copy keeps the order");
    }

    /** A forEach whose callback empties a multimap of 100 slots and puts values back ends. */
    static void forEachEmptied() {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 100; i++) {
            m.put("k" + (i % 3), i);
        }
        List<Object> seen = new ArrayList<>();
        m.forEach((k, v) -> {
            seen.add(v);
            if (seen.size() == 1) {
                m.clear();
                for (int i = 0; i < 3; i++) {
                    m.put("n", "n" + i);
                }
            }
        });
        same("[0]", seen, "forEach ends when its callback empties the multimap");
    }

    /** Emptying through remove() drops lanes above 64 slots and ends a suspended iteration. */
    static void emptiedByRemove() throws ReflectiveOperationException {
        java.lang.reflect.Field capacity = LinkedHashMultimap.class.getDeclaredField("capacity");
        capacity.setAccessible(true);
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 100; i++) {
            m.put("k" + (i % 3), i);
        }
        Iterator<Map.Entry<Object, Object>> it = m.entries().iterator();
        it.next();
        for (int i = 0; i < 100; i++) {
            m.remove("k" + (i % 3), i);
        }
        same(0, capacity.getInt(m), "lanes above 64 slots dropped when remove() empties");
        same("k1=1", it.next(), "the entry reached before the removals is still returned");
        same(false, it.hasNext(), "emptying through remove() then ends the iteration");
    }

    /** A removed key becomes collectable: a freed slot keeps no key. */
    static void keyRetention() {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 10; i++) {
            m.put("k" + i, i);
        }
        WeakReference<Object> held = putFreshKey(m);
        m.remove("held key", "v");
        for (int i = 0; i < 50 && held.get() != null; i++) {
            System.gc();
        }
        same(null, held.get(), "key kept after its removal");
    }

    static WeakReference<Object> putFreshKey(LinkedHashMultimap<Object, Object> m) {
        Object key = new String("held key");
        m.put(key, "v");
        return new WeakReference<>(key);
    }

    /** Values colliding in one bucket switch the key's value set to the HashMap fallback. */
    static void flooding() throws ReflectiveOperationException {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 40; i++) {
            m.put("k", new V(i, 7));
        }
        Class<?> base = Class.forName("jdk.m3.collections.guava.collect.AbstractMapBasedMultimap");
        java.lang.reflect.Method backing = base.getDeclaredMethod("backingMap");
        backing.setAccessible(true);
        Object valueSet = ((Map<?, ?>) backing.invoke(m)).get("k");
        java.lang.reflect.Method delegate = valueSet.getClass().getDeclaredMethod("delegateOrNull");
        delegate.setAccessible(true);
        same(true, delegate.invoke(valueSet) != null, "colliding values switch to the fallback");
        same(40, m.get("k").size(), "every colliding value kept");
        same(new V(0, 7), m.get("k").iterator().next(), "insertion order kept");
    }

    static String scenario(int row) {
        LinkedHashMultimap<Object, Object> m = LinkedHashMultimap.create();
        for (int i = 0; i < 6; i++) {
            m.put("k" + (i % 2), "v" + i);
        }
        List<String> seen = new ArrayList<>();
        Iterator<Map.Entry<Object, Object>> it = m.entries().iterator();
        Map.Entry<Object, Object> e = it.next();
        seen.add(String.valueOf(e));
        switch (row) {
            case 1 -> m.remove(e.getKey(), e.getValue());
            case 2 -> m.remove("k1", "v1");
            case 4 -> {
                m.remove("k1", "v1");
                m.remove("k0", "v2");
            }
            default -> m.clear();
        }
        while (it.hasNext()) {
            seen.add(String.valueOf(it.next()));
        }
        return seen + " size " + m.size();
    }
}
