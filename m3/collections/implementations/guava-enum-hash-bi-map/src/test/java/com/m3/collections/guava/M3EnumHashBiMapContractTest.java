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
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumBiMap;
import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.HashMultiset;
import com.m3.collections.guava.M3EnumHashBiMapDifferentialTest.Key;
import com.m3.collections.guava.M3EnumHashBiMapDifferentialTest.Other;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

/** Focused contract cases against the Guava provider, plus owned-only edge and invariant cases. */
final class M3EnumHashBiMapContractTest {
    @FunctionalInterface
    interface Scenario {
        Object run(BiMap<Key, Integer> m) throws Exception;
    }

    private static BiMap<Key, Integer> guava() {
        BiMap<Key, Integer> m = EnumHashBiMap.create(Key.class);
        m.put(Key.A, 1);
        m.put(Key.C, 3);
        m.put(Key.E, null);
        m.put(Key.G, 7);
        return m;
    }

    private static M3EnumHashBiMap<Key, Integer> owned() {
        return M3EnumHashBiMap.create(guava());
    }

    private static Object outcome(Scenario s, BiMap<Key, Integer> m) {
        try {
            Object o = s.run(m);
            return o instanceof Collection<?> c && !(o instanceof List<?>) ? HashMultiset.create(c) : o;
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void both(String name, Scenario s) {
        BiMap<Key, Integer> g = guava();
        M3EnumHashBiMap<Key, Integer> o = owned();
        assertEquals(outcome(s, g), outcome(s, o), name);
        assertEquals(g, o, name + " state");
        assertEquals(g.inverse(), o.inverse(), name + " inverse state");
        o.verifyInvariants();
        M3EnumHashBiMapDifferentialTest.CHECKS.addAndGet(3);
    }

    @Test
    void factoriesAndKeyType() {
        assertEquals(Key.class, M3EnumHashBiMap.create(Key.class).keyType());
        assertEquals(Key.class, M3EnumHashBiMap.create(owned()).keyType());
        assertEquals(Key.class, M3EnumHashBiMap.create(EnumHashBiMap.<Key, Integer>create(Key.class)).keyType());
        assertEquals(Key.class, M3EnumHashBiMap.create(EnumBiMap.<Key, Other>create(Key.class, Other.class)).keyType());
        assertEquals(Key.class, M3EnumHashBiMap.create(M3EnumHashBiMap.<Key, Integer>create(Key.class)).keyType());
        Map<Key, Integer> plain = new HashMap<>();
        IllegalArgumentException g = assertThrows(IllegalArgumentException.class, () -> EnumHashBiMap.create(plain));
        IllegalArgumentException o = assertThrows(IllegalArgumentException.class, () -> M3EnumHashBiMap.create(plain));
        assertEquals(g.getMessage(), o.getMessage());
        plain.put(Key.B, 2);
        assertEquals(EnumHashBiMap.create(plain), M3EnumHashBiMap.create(plain));
        assertThrows(NullPointerException.class, () -> M3EnumHashBiMap.create((Class<Key>) null));
        BiMap<Key, Integer> m = owned();
        assertSame(m.inverse().inverse(), m);
        assertSame(m.inverse(), m.inverse());
        assertSame(m.keySet(), m.keySet());
        assertSame(m.values(), m.values());
        assertSame(m.entrySet(), m.entrySet());
        assertSame(m.inverse().keySet(), m.inverse().keySet());
        assertSame(m.inverse().values(), m.inverse().values());
        assertSame(m.inverse().entrySet(), m.inverse().entrySet());
    }

    @Test
    void mapContract() {
        both("put new", m -> List.of(String.valueOf(m.put(Key.B, 2)), m.size()));
        both("put same", m -> m.put(Key.A, 1));
        both("put duplicate value", m -> m.put(Key.B, 3));
        both("forcePut duplicate value", m -> m.forcePut(Key.B, 3));
        both("put null key", m -> m.put(null, 9));
        both("put null value twice", m -> m.put(Key.B, null));
        both("forcePut null value", m -> m.forcePut(Key.B, null));
        both("remove absent", m -> List.of(String.valueOf(m.remove(Key.B)), String.valueOf(m.remove("x")), String.valueOf(m.remove(null))));
        both("get variants", m -> List.of(String.valueOf(m.get(Key.A)), String.valueOf(m.get(Key.B)), String.valueOf(m.get("x")), String.valueOf(m.get(null)),
                m.containsKey(null), m.containsKey("x"), m.containsValue(null), m.containsValue(9)));
        both("equality", m -> {
            Map<Key, Integer> same = new HashMap<>(m);
            Map<Key, Integer> nullMismatch = new HashMap<>(m);
            nullMismatch.remove(Key.E);
            nullMismatch.put(Key.F, null);
            Map<Key, Integer> valueMismatch = new HashMap<>(m);
            valueMismatch.put(Key.A, 9);
            Map<Key, Integer> missing = new HashMap<>(m);
            missing.remove(Key.A);
            return List.of(m.equals(m), m.equals("x"), m.equals(same), m.equals(nullMismatch), m.equals(valueMismatch), m.equals(missing), m.hashCode());
        });
        both("text", m -> List.of(m.toString(), m.keySet().toString(), m.values().toString(), m.entrySet().toString()));
        both("empty", m -> {
            m.clear();
            return List.of(m.isEmpty(), m.size(), m.toString(), m.inverse().isEmpty(), m.inverse().size(), m.hashCode());
        });
        both("forEach", m -> {
            List<String> seen = new ArrayList<>();
            m.forEach((k, v) -> seen.add(k + "=" + v));
            return seen;
        });
        both("forEach null", m -> {
            m.forEach((BiConsumer<Key, Integer>) null);
            return null;
        });
        both("forEach removes later", m -> {
            List<String> seen = new ArrayList<>();
            m.forEach((k, v) -> {
                seen.add(String.valueOf(k));
                if (k == Key.A) { m.remove(Key.G); }
            });
            return seen;
        });
        both("replaceAll conflict", m -> {
            m.replaceAll((k, v) -> 5);
            return null;
        });
        both("replaceAll null", m -> {
            m.replaceAll(null);
            return null;
        });
        both("replaceAll identity", m -> {
            m.replaceAll((k, v) -> v);
            return m.size();
        });
        both("putAll", m -> {
            m.putAll(Map.of(Key.B, 2, Key.D, 4));
            return m.size();
        });
        both("defaults", m -> List.of(String.valueOf(m.putIfAbsent(Key.E, 9)), String.valueOf(m.merge(Key.A, 5, Integer::sum)), m.remove(Key.C, 3),
                String.valueOf(m.getOrDefault(Key.B, -1))));
    }

    @Test
    void viewsAndEntries() {
        both("keySet clear", m -> {
            m.keySet().clear();
            return m.size();
        });
        both("values clear", m -> {
            m.values().clear();
            return m.size();
        });
        both("entrySet clear", m -> {
            m.entrySet().clear();
            return m.size();
        });
        both("inverse views clear", m -> {
            m.inverse().keySet().clear();
            return m.size();
        });
        both("inverse values clear", m -> {
            m.inverse().values().clear();
            return m.size();
        });
        both("inverse entries clear", m -> {
            m.inverse().entrySet().clear();
            return m.size();
        });
        both("inverse clear", m -> {
            m.inverse().clear();
            return m.size();
        });
        both("contains odd inputs", m -> List.of(m.entrySet().contains("x"), m.inverse().entrySet().contains("x"), m.keySet().remove(Key.B),
                m.values().remove(9), m.inverse().keySet().contains(3), m.inverse().keySet().contains(4), m.inverse().values().contains(Key.A),
                m.inverse().values().contains("x"), m.inverse().values().remove(Key.B), m.inverse().values().retainAll(List.of(Key.A, Key.C, Key.E)),
                m.inverse().entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(1, Key.A)), m.inverse().entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(1, Key.B)),
                m.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(Key.E, null)), m.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(Key.B, null)),
                m.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("x", 1))));
        both("forward iterator", m -> {
            Iterator<Key> it = m.keySet().iterator();
            List<Object> trace = new ArrayList<>();
            while (it.hasNext()) { trace.add(it.next()); }
            try {
                it.next();
            } catch (NoSuchElementException e) {
                trace.add("nsee");
            }
            return trace;
        });
        both("forward remove twice", m -> {
            Iterator<Integer> it = m.values().iterator();
            it.next();
            it.remove();
            it.remove();
            return null;
        });
        both("forward weakly consistent", m -> {
            List<Object> trace = new ArrayList<>();
            Iterator<Map.Entry<Key, Integer>> it = m.entrySet().iterator();
            trace.add(it.next().getKey());
            m.put(Key.B, 2);
            m.remove(Key.C);
            m.put(Key.J, 10);
            while (it.hasNext()) { trace.add(it.next().getKey()); }
            return trace;
        });
        both("entry after removal", m -> {
            Iterator<Map.Entry<Key, Integer>> it = m.entrySet().iterator();
            Map.Entry<Key, Integer> e = it.next();
            it.remove();
            return e.getValue();
        });
        both("entry key after removal", m -> {
            Iterator<Map.Entry<Key, Integer>> it = m.entrySet().iterator();
            Map.Entry<Key, Integer> e = it.next();
            it.remove();
            return e.getKey();
        });
        both("entry setValue after removal", m -> {
            Iterator<Map.Entry<Key, Integer>> it = m.entrySet().iterator();
            Map.Entry<Key, Integer> e = it.next();
            it.remove();
            return e.setValue(5);
        });
        both("entry setValue after external removal", m -> {
            Map.Entry<Key, Integer> e = m.entrySet().iterator().next();
            m.remove(e.getKey());
            return e.setValue(5);
        });
        both("entry live", m -> {
            Map.Entry<Key, Integer> e = m.entrySet().iterator().next();
            m.put(e.getKey(), 8);
            return List.of(String.valueOf(e.getValue()), e.toString(), e.hashCode(), e.equals(new AbstractMap.SimpleImmutableEntry<>(Key.A, 8)),
                    e.equals("x"), e.equals(new AbstractMap.SimpleImmutableEntry<>(Key.B, 8)), e.equals(new AbstractMap.SimpleImmutableEntry<>(Key.A, 9)));
        });
        both("entry setValue variants", m -> {
            Map.Entry<Key, Integer> e = m.entrySet().iterator().next();
            return List.of(String.valueOf(e.setValue(1)), String.valueOf(e.setValue(11)), m.get(Key.A));
        });
        both("entry setValue duplicate", m -> m.entrySet().iterator().next().setValue(3));
        both("inverse iterator", m -> {
            Iterator<Integer> it = m.inverse().keySet().iterator();
            List<Object> trace = new ArrayList<>();
            while (it.hasNext()) { trace.add(it.next()); }
            try {
                it.next();
            } catch (NoSuchElementException e) {
                trace.add("nsee");
            }
            return HashMultiset.create(trace);
        });
        both("inverse iterator cme", m -> {
            Iterator<Key> it = m.inverse().values().iterator();
            it.next();
            m.put(Key.B, 2);
            return it.next();
        });
        both("inverse iterator remove cme", m -> {
            Iterator<Key> it = m.inverse().values().iterator();
            it.next();
            m.remove(Key.G);
            it.remove();
            return null;
        });
        both("inverse iterator remove twice", m -> {
            Iterator<Map.Entry<Integer, Key>> it = m.inverse().entrySet().iterator();
            while (it.next().getValue() != Key.C) {
                continue;
            }
            it.remove();
            it.remove();
            return null;
        });
        both("inverse iterator same-value rebind keeps going", m -> {
            Iterator<Integer> it = m.inverse().keySet().iterator();
            it.next();
            m.inverse().put(7, Key.H);
            List<Integer> rest = new ArrayList<>();
            while (it.hasNext()) { rest.add(it.next()); }
            return rest.size();
        });
        both("inverse entry", m -> {
            Map.Entry<Integer, Key> e = null;
            for (Map.Entry<Integer, Key> x : m.inverse().entrySet()) {
                if (x.getKey() != null && x.getKey() == 3) { e = x; }
            }
            Map.Entry<Integer, Key> f = e;
            m.inverse().put(3, Key.D);
            Key live = f.getValue();
            m.remove(Key.D);
            Key last = f.getValue();
            return List.of(live, last, f.toString(), f.hashCode(), f.equals(new AbstractMap.SimpleImmutableEntry<>(3, Key.D)), f.equals("x"),
                    f.equals(new AbstractMap.SimpleImmutableEntry<>(4, Key.D)));
        });
        both("inverse entry setValue variants", m -> {
            Map.Entry<Integer, Key> e = null;
            for (Map.Entry<Integer, Key> x : m.inverse().entrySet()) {
                if (x.getKey() != null && x.getKey() == 1) { e = x; }
            }
            Map.Entry<Integer, Key> f = e;
            List<Object> trace = new ArrayList<>();
            trace.add(f.setValue(Key.A));
            trace.add(f.setValue(Key.B));
            trace.add(outcome(x -> f.setValue(Key.C), m));
            trace.add(outcome(x -> f.setValue(null), m));
            m.inverse().remove(1);
            trace.add(outcome(x -> f.setValue(Key.H), m));
            return trace;
        });
        both("inverse put variants", m -> List.of(String.valueOf(m.inverse().put(1, Key.A)), String.valueOf(m.inverse().put(1, Key.B)),
                outcome(x -> x.inverse().put(3, Key.B), m), String.valueOf(m.inverse().forcePut(3, Key.B)), String.valueOf(m.inverse().put(9, Key.I))));
        both("inverse forEach", m -> {
            List<String> seen = new ArrayList<>();
            m.inverse().forEach((v, k) -> seen.add(v + "=" + k));
            return HashMultiset.create(seen);
        });
        both("inverse forEach cme", m -> {
            m.inverse().forEach((v, k) -> m.put(Key.J, 99));
            return null;
        });
        both("inverse forEach null", m -> {
            m.inverse().forEach(null);
            return null;
        });
        both("inverse replaceAll bijection", m -> {
            m.inverse().replaceAll((v, k) -> k == Key.A ? Key.B : k == Key.B ? Key.A : k);
            return m.size();
        });
        both("inverse replaceAll null", m -> {
            m.inverse().replaceAll(null);
            return null;
        });
        both("inverse equality", m -> List.of(m.inverse().equals(new HashMap<>(m.inverse())), m.inverse().hashCode(), m.inverse().equals("x")));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void ownedOnlyDivergences() {
        M3EnumHashBiMap<Key, Integer> o = owned();
        RuntimeException conflict = assertThrows(IllegalArgumentException.class, () -> o.inverse().replaceAll((v, k) -> Key.A));
        assertTrue(conflict.getMessage().startsWith("value already present: "));
        o.verifyInvariants();
        assertEquals(1, o.size());
        M3EnumHashBiMap<Key, Integer> p = owned();
        assertThrows(NullPointerException.class, () -> p.inverse().replaceAll((v, k) -> null));
        BiMap raw = rawInverse(p);
        assertThrows(ClassCastException.class, () -> raw.replaceAll(wrongKeys()));
        assertThrows(ClassCastException.class, () -> raw.put(42, Other.Z));
        assertThrows(ClassCastException.class, () -> raw.forcePut(43, Other.Z));
        Map.Entry rawEntry = (Map.Entry) raw.entrySet().iterator().next();
        assertThrows(ClassCastException.class, () -> setRaw(rawEntry, Other.Z));
        assertEquals(owned(), p);
        p.verifyInvariants();
        Iterator<Map.Entry<Key, Integer>> it = owned().entrySet().iterator();
        Map.Entry<Key, Integer> removed = it.next();
        it.remove();
        assertEquals("Entry was removed", removed.toString());
        assertTrue(removed.equals(removed));
        assertFalse(removed.equals(new AbstractMap.SimpleImmutableEntry<>(Key.A, 1)));
        assertEquals(System.identityHashCode(removed), removed.hashCode());
    }

    @SuppressWarnings("rawtypes")
    private static BiMap rawInverse(M3EnumHashBiMap<Key, Integer> m) {
        return m.inverse();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static java.util.function.BiFunction wrongKeys() {
        return (v, k) -> Other.Z;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object setRaw(Map.Entry entry, Object value) {
        return entry.setValue(value);
    }

    enum Bodied {
        P {
            @Override int weight() {
                return 2;
            }
        },
        Q;

        int weight() {
            return 1;
        }
    }

    @Test
    void ownedOnlyEdges() throws Exception {
        M3EnumHashBiMap<Bodied, String> bodied = M3EnumHashBiMap.create(Bodied.class);
        bodied.put(Bodied.P, "p");
        bodied.put(Bodied.Q, "q");
        assertEquals("p", bodied.get(Bodied.P));
        assertEquals(Bodied.P, bodied.inverse().get("p"));
        assertEquals(EnumHashBiMap.create(bodied), bodied);
        assertEquals(2, Bodied.P.weight());
        M3EnumHashBiMap<Key, Integer> m = owned();
        Iterator<Key> keys = m.keySet().iterator();
        Key k = keys.next();
        m.remove(k);
        keys.remove();
        m.verifyInvariants();
        assertEquals(3, m.size());
        Map<Key, Integer> other = new HashMap<>(owned());
        other.put(Key.E, 9);
        assertFalse(owned().equals(other));
        assertFalse(owned().inverse().isEmpty());
        assertEquals(4, owned().inverse().entrySet().size());
        Map.Entry<Integer, Key> three = null;
        for (Map.Entry<Integer, Key> x : m.inverse().entrySet()) {
            if (Integer.valueOf(3).equals(x.getKey())) { three = x; }
        }
        assertFalse(three.equals(new AbstractMap.SimpleImmutableEntry<>(3, Key.A)));
        M3EnumHashBiMap<Key, Integer> last = owned();
        assertThrows(java.util.ConcurrentModificationException.class, () -> last.inverse().forEach((v, key) -> {
            if (key == Key.G) { last.remove(Key.A); }
        }));
        M3EnumHashBiMap<Key, Integer> nullType = M3EnumHashBiMap.create(Key.class);
        Field keyType = M3EnumHashBiMap.class.getDeclaredField("keyType");
        keyType.setAccessible(true);
        keyType.set(nullType, null);
        byte[] data = write(nullType);
        assertThrows(InvalidObjectException.class, () -> read(data));
    }

    @Test
    void serializationRoundTrip() throws Exception {
        M3EnumHashBiMap<Key, Integer> o = owned();
        Object back = read(write(o));
        assertEquals(o, back);
        @SuppressWarnings("unchecked") M3EnumHashBiMap<Key, Integer> copy = (M3EnumHashBiMap<Key, Integer>) back;
        copy.verifyInvariants();
        assertEquals(Key.class, copy.keyType());
        Object inverse = read(write(o.inverse()));
        assertEquals(o.inverse(), inverse);
        @SuppressWarnings("unchecked") BiMap<Integer, Key> inv = (BiMap<Integer, Key>) inverse;
        assertEquals(o, inv.inverse());
        assertThrows(InvalidObjectException.class, () -> read(serializedWith("java.lang.Integer")));
    }

    private static byte[] write(Object o) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(o); }
        return bytes.toByteArray();
    }

    private static Object read(byte[] data) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) { return in.readObject(); }
    }


    private static byte[] serializedWith(String keyTypeName) throws Exception {
        M3EnumHashBiMap<Key, Integer> m = M3EnumHashBiMap.create(Key.class);
        Field f = M3EnumHashBiMap.class.getDeclaredField("keyType");
        f.setAccessible(true);
        f.set(m, Class.forName(keyTypeName));
        return write(m);
    }

    @Test
    void corruptStreamsAreRejected() throws Exception {
        M3EnumHashBiMap<Key, Integer> m = owned();
        Field size = M3EnumHashBiMap.class.getDeclaredField("size");
        size.setAccessible(true);
        size.setInt(m, -1);
        byte[] negative = write(m);
        assertThrows(InvalidObjectException.class, () -> read(negative));
        size.setInt(m, 99);
        byte[] oversized = write(m);
        assertThrows(InvalidObjectException.class, () -> read(oversized));
        M3EnumHashBiMap<Key, Integer> n = M3EnumHashBiMap.create(Key.class);
        n.put(Key.A, 1);
        Field keyType = M3EnumHashBiMap.class.getDeclaredField("keyType");
        keyType.setAccessible(true);
        keyType.set(n, Other.class);
        byte[] badKey = write(n);
        assertThrows(InvalidObjectException.class, () -> read(badKey));
    }

    @Test
    void invariantCheckerDetectsCorruption() throws Exception {
        expectBroken("value index", m -> ints(m, "valueHashes")[0] ^= 1);
        expectBroken("absent slot", m -> objects(m, "vals")[1] = 5);
        expectBroken("dangling index", m -> {
            int[] index = ints(m, "index");
            for (int p = 0; p < index.length; p++) {
                if (index[p] == 0) {
                    index[p] = 2;
                    return;
                }
            }
        });
        expectBroken("counts", m -> setInt(m, "size", 9));
        expectBroken("counts", m -> {
            int[] index = ints(m, "index");
            for (int p = 0; p < index.length; p++) {
                if (index[p] == 0) {
                    index[p] = 1;
                    return;
                }
            }
        });
        expectBroken("value index", m -> {
            int[] index = ints(m, "index");
            for (int p = 0; p < index.length; p++) {
                if (index[p] == 1) { index[p] = 0; }
            }
        });
    }

    @Test
    void steadyStateHotPathsAllocateNothing() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        M3EnumHashBiMap<Key, Integer> m = owned();
        BiMap<Integer, Key> inv = m.inverse();
        Integer seven = 7;
        Integer eight = 8;
        long[] sink = new long[1];
        BiConsumer<Key, Integer> visit = (k, v) -> sink[0] += k.ordinal();
        Runnable op = () -> {
            m.put(Key.G, eight);
            m.put(Key.G, seven);
            sink[0] += m.containsKey(Key.G) ? 1 : 0;
            sink[0] += m.containsValue(seven) ? 1 : 0;
            sink[0] += inv.get(seven).ordinal();
            m.forEach(visit);
            sink[0] += m.size() + m.hashCode();
        };
        for (int i = 0; i < 20_000; i++) { op.run(); }
        long best = Long.MAX_VALUE;
        long id = Thread.currentThread().threadId();
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 20_000; i++) { op.run(); }
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state allocation in put/containsKey/containsValue/inverse.get/forEach/hashCode");
        assertNotEquals(0L, sink[0]);
    }

    private interface Corruption {
        void apply(M3EnumHashBiMap<Key, Integer> m) throws Exception;
    }

    private static void expectBroken(String message, Corruption corrupt) throws Exception {
        M3EnumHashBiMap<Key, Integer> m = M3EnumHashBiMap.create(Key.class);
        m.put(Key.A, 1);
        m.put(Key.C, 3);
        m.verifyInvariants();
        corrupt.apply(m);
        AssertionError e = assertThrows(AssertionError.class, m::verifyInvariants);
        assertEquals(message, e.getMessage());
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field f = M3EnumHashBiMap.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static void setInt(Object m, String name, int value) throws Exception {
        field(name).setInt(m, value);
    }

    private static int[] ints(Object m, String name) throws Exception {
        return (int[]) field(name).get(m);
    }

    private static Object[] objects(Object m, String name) throws Exception {
        return (Object[]) field(name).get(m);
    }
}
