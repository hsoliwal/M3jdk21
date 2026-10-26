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
import com.m3.collections.guava.M3EnumBiMapDifferentialTest.Key;
import com.m3.collections.guava.M3EnumBiMapDifferentialTest.Other;
import com.m3.collections.guava.M3EnumBiMapDifferentialTest.Val;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

/** Focused contract cases against the Guava provider, plus owned-only edge and invariant cases. */
final class M3EnumBiMapContractTest {
    @FunctionalInterface
    interface Scenario {
        Object run(BiMap<Key, Val> m) throws Exception;
    }

    private static BiMap<Key, Val> guava() {
        BiMap<Key, Val> m = EnumBiMap.create(Key.class, Val.class);
        m.put(Key.A, Val.V1);
        m.put(Key.C, Val.V3);
        m.put(Key.E, Val.V0);
        m.put(Key.G, Val.V7);
        return m;
    }

    private static M3EnumBiMap<Key, Val> owned() {
        return M3EnumBiMap.create(guava());
    }

    private static Object outcome(Scenario s, BiMap<Key, Val> m) {
        try {
            return s.run(m);
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void both(String name, Scenario s) {
        BiMap<Key, Val> g = guava();
        M3EnumBiMap<Key, Val> o = owned();
        assertEquals(outcome(s, g), outcome(s, o), name);
        assertEquals(g.toString(), o.toString(), name + " state");
        assertEquals(g.inverse().toString(), o.inverse().toString(), name + " inverse state");
        o.verifyInvariants();
        M3EnumBiMapDifferentialTest.CHECKS.addAndGet(3);
    }

    @Test
    void factoriesAndTypes() {
        M3EnumBiMap<Key, Val> m = M3EnumBiMap.create(Key.class, Val.class);
        assertEquals(Key.class, m.keyType());
        assertEquals(Val.class, m.valueType());
        assertEquals(Key.class, M3EnumBiMap.create(owned()).keyType());
        assertEquals(Val.class, M3EnumBiMap.create(owned()).valueType());
        assertEquals(Val.class, M3EnumBiMap.create(EnumBiMap.<Key, Val>create(Key.class, Val.class)).valueType());
        Map<Key, Val> plain = new HashMap<>();
        IllegalArgumentException g = assertThrows(IllegalArgumentException.class, () -> EnumBiMap.create(plain));
        IllegalArgumentException o = assertThrows(IllegalArgumentException.class, () -> M3EnumBiMap.create(plain));
        assertEquals(g.getMessage(), o.getMessage());
        assertThrows(IllegalArgumentException.class, () -> M3EnumBiMap.inferValueType(EnumHashBiMap.<Key, Val>create(Key.class)));
        assertEquals(Key.class, M3EnumBiMap.inferKeyType(EnumHashBiMap.<Key, Val>create(Key.class)));
        assertEquals(Key.class, M3EnumBiMap.inferKeyType(M3EnumBiMap.<Key, Val>create(Key.class, Val.class)));
        plain.put(Key.B, Val.V2);
        assertEquals(EnumBiMap.create(plain), M3EnumBiMap.create(plain));
        assertThrows(NullPointerException.class, () -> M3EnumBiMap.create(null, Val.class));
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
        both("put variants", m -> List.of(String.valueOf(m.put(Key.B, Val.V2)), String.valueOf(m.put(Key.A, Val.V1)), outcome(x -> x.put(Key.D, Val.V3), m),
                String.valueOf(m.forcePut(Key.D, Val.V3)), outcome(x -> x.put(null, Val.V4), m), outcome(x -> x.put(Key.F, null), m),
                outcome(x -> x.inverse().put(null, Key.F), m), outcome(x -> x.inverse().put(Val.V6, null), m)));
        both("remove variants", m -> List.of(String.valueOf(m.remove(Key.B)), String.valueOf(m.remove("x")), String.valueOf(m.remove(null)),
                String.valueOf(m.inverse().remove(Val.V7)), String.valueOf(m.inverse().remove("x"))));
        both("lookups", m -> List.of(String.valueOf(m.get(null)), m.containsKey(null), m.containsValue(null), m.containsValue("x"),
                m.inverse().containsKey(Val.V3), m.inverse().containsValue(Key.C), m.inverse().isEmpty(), m.isEmpty()));
        both("equality", m -> {
            Map<Key, Val> same = new HashMap<>(m);
            Map<Key, Val> mismatch = new HashMap<>(m);
            mismatch.put(Key.A, Val.V5);
            Map<Val, Key> inverseSame = new EnumMap<>(Val.class);
            inverseSame.putAll(m.inverse());
            return List.of(m.equals(m), m.equals("x"), m.equals(same), m.equals(mismatch), m.equals(Map.of()), m.inverse().equals(inverseSame),
                    m.inverse().equals(m.inverse()), m.inverse().equals("x"));
        });
        both("empty", m -> {
            m.clear();
            return List.of(m.isEmpty(), m.size(), m.toString(), m.inverse().isEmpty(), m.hashCode(), m.inverse().hashCode());
        });
        both("inverse clear", m -> {
            m.inverse().clear();
            return m.size();
        });
        both("forEach both sides", m -> {
            List<String> seen = new ArrayList<>();
            m.forEach((k, v) -> seen.add(k + "=" + v));
            m.inverse().forEach((v, k) -> seen.add(v + "=" + k));
            return seen;
        });
        both("forEach null", m -> {
            m.forEach((BiConsumer<Key, Val>) null);
            return null;
        });
        both("inverse forEach null", m -> {
            m.inverse().forEach(null);
            return null;
        });
        both("replaceAll conflict", m -> {
            m.replaceAll((k, v) -> Val.V5);
            return null;
        });
        both("inverse replaceAll conflict", m -> {
            m.inverse().replaceAll((v, k) -> Key.B);
            return null;
        });
        both("replaceAll null function", m -> {
            m.replaceAll(null);
            return null;
        });
        both("inverse replaceAll null function", m -> {
            m.inverse().replaceAll(null);
            return null;
        });
        both("defaults", m -> List.of(String.valueOf(m.putIfAbsent(Key.B, Val.V2)), String.valueOf(m.merge(Key.A, Val.V6, (a, b) -> b)), m.remove(Key.C, Val.V3),
                String.valueOf(m.getOrDefault(Key.H, Val.V4))));
    }

    @Test
    void viewsAndEntries() {
        both("view clears", m -> {
            m.keySet().clear();
            m.put(Key.A, Val.V1);
            m.values().clear();
            m.put(Key.B, Val.V2);
            m.entrySet().clear();
            m.put(Key.C, Val.V3);
            m.inverse().keySet().clear();
            m.put(Key.D, Val.V4);
            m.inverse().values().clear();
            m.put(Key.E, Val.V5);
            m.inverse().entrySet().clear();
            return m.size();
        });
        both("view membership", m -> List.of(m.keySet().contains(Key.A), m.keySet().contains("x"), m.keySet().remove(Key.B), m.values().contains(Val.V1),
                m.values().remove(Val.V6), m.entrySet().contains("x"), m.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(Key.A, Val.V2)),
                m.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(Key.B, Val.V1)), m.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>("x", Val.V1)),
                m.entrySet().remove("x"), m.inverse().entrySet().size(), m.inverse().keySet().size(), m.inverse().values().size(), m.keySet().size(),
                m.values().size(), m.entrySet().size()));
        both("iterator end and remove twice", m -> {
            List<Object> trace = new ArrayList<>();
            Iterator<Val> it = m.values().iterator();
            while (it.hasNext()) { trace.add(it.next()); }
            trace.add(outcome(x -> it.next(), m));
            Iterator<Key> keys = m.inverse().values().iterator();
            keys.next();
            keys.remove();
            trace.add(outcome(x -> {
                keys.remove();
                return null;
            }, m));
            return trace;
        });
        both("weakly consistent", m -> {
            List<Object> trace = new ArrayList<>();
            Iterator<Map.Entry<Key, Val>> it = m.entrySet().iterator();
            trace.add(it.next().getKey());
            m.put(Key.B, Val.V2);
            m.remove(Key.C);
            m.put(Key.I, Val.V6);
            while (it.hasNext()) { trace.add(it.next().getKey()); }
            Iterator<Val> inv = m.inverse().keySet().iterator();
            trace.add(inv.next());
            m.inverse().remove(Val.V7);
            m.inverse().put(Val.V4, Key.H);
            while (inv.hasNext()) { trace.add(inv.next()); }
            return trace;
        });
        both("removed entry", m -> {
            Iterator<Map.Entry<Key, Val>> it = m.entrySet().iterator();
            Map.Entry<Key, Val> e = it.next();
            it.remove();
            return List.of(outcome(x -> e.getKey(), m), outcome(x -> e.getValue(), m), outcome(x -> e.setValue(Val.V6), m), outcome(x -> e.setValue(null), m));
        });
        both("externally removed entry", m -> {
            Map.Entry<Key, Val> e = m.entrySet().iterator().next();
            m.remove(e.getKey());
            return List.of(String.valueOf(e.getValue()), e.toString(), outcome(x -> e.setValue(Val.V6), m));
        });
        both("entry setValue", m -> {
            Map.Entry<Key, Val> e = m.entrySet().iterator().next();
            return List.of(e.setValue(Val.V1), e.setValue(Val.V6), outcome(x -> e.setValue(Val.V3), m), e.toString(), e.hashCode(),
                    e.equals(new AbstractMap.SimpleImmutableEntry<>(Key.A, Val.V6)), e.equals("x"), e.equals(new AbstractMap.SimpleImmutableEntry<>(Key.B, Val.V6)),
                    e.equals(new AbstractMap.SimpleImmutableEntry<>(Key.A, Val.V5)));
        });
        both("inverse entry setValue", m -> {
            Map.Entry<Val, Key> e = m.inverse().entrySet().iterator().next();
            return List.of(e.setValue(Key.E), e.setValue(Key.F), outcome(x -> e.setValue(Key.A), m), outcome(x -> e.setValue(null), m), e.toString());
        });
    }

    @Test
    void ownedOnlyDivergencesAndEdges() throws Exception {
        M3EnumBiMap<Key, Val> p = owned();
        assertThrows(NullPointerException.class, () -> p.replaceAll((k, v) -> null));
        assertThrows(NullPointerException.class, () -> p.inverse().replaceAll((v, k) -> null));
        assertThrows(ClassCastException.class, () -> rawPut(p, Key.H, Other.Z));
        assertThrows(ClassCastException.class, () -> rawReplace(p, Other.Z));
        assertThrows(ClassCastException.class, () -> rawPut(p.inverse(), Other.Z, Key.H));
        assertThrows(ClassCastException.class, () -> rawPut(p, Other.Z, Val.V5));
        assertEquals(owned(), p);
        p.verifyInvariants();
        Iterator<Map.Entry<Key, Val>> it = owned().entrySet().iterator();
        Map.Entry<Key, Val> removed = it.next();
        it.remove();
        assertEquals("Entry was removed", removed.toString());
        assertTrue(removed.equals(removed));
        assertFalse(removed.equals(new AbstractMap.SimpleImmutableEntry<>(Key.A, Val.V1)));
        assertEquals(System.identityHashCode(removed), removed.hashCode());
        M3EnumBiMap<Bodied, Val> bodied = M3EnumBiMap.create(Bodied.class, Val.class);
        bodied.put(Bodied.P, Val.V1);
        bodied.put(Bodied.Q, Val.V2);
        assertEquals(Bodied.P, bodied.inverse().get(Val.V1));
        assertEquals(2, Bodied.P.weight() + Bodied.Q.weight() - 1);
        Iterator<Key> keys = p.keySet().iterator();
        Key first = keys.next();
        p.remove(first);
        keys.remove();
        p.verifyInvariants();
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object rawPut(Map m, Object key, Object value) {
        return m.put(key, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void rawReplace(Map m, Object value) {
        m.replaceAll((k, v) -> value);
    }

    @Test
    void serializationRoundTrip() throws Exception {
        M3EnumBiMap<Key, Val> o = owned();
        Object back = read(write(o));
        assertEquals(o, back);
        @SuppressWarnings("unchecked") M3EnumBiMap<Key, Val> copy = (M3EnumBiMap<Key, Val>) back;
        copy.verifyInvariants();
        assertEquals(Val.class, copy.valueType());
        Object inverse = read(write(o.inverse()));
        assertEquals(o.inverse(), inverse);
        corrupt("types", 0, Integer.class);
        corrupt("types", 0, null);
        corrupt("types", 1, Integer.class);
        corrupt("types", 1, null);
        M3EnumBiMap<Key, Val> sized = owned();
        Field size = M3EnumBiMap.class.getDeclaredField("size");
        size.setAccessible(true);
        size.setInt(sized, -1);
        byte[] negative = write(sized);
        assertThrows(InvalidObjectException.class, () -> read(negative));
        size.setInt(sized, 99);
        byte[] oversized = write(sized);
        assertThrows(InvalidObjectException.class, () -> read(oversized));
        M3EnumBiMap<Key, Val> swapped = owned();
        Field types = M3EnumBiMap.class.getDeclaredField("types");
        types.setAccessible(true);
        types.set(swapped, new Class<?>[] {Val.class, Key.class});
        byte[] badMapping = write(swapped);
        assertThrows(InvalidObjectException.class, () -> read(badMapping));
        M3EnumBiMap<Key, Val> halfSwapped = owned();
        types.set(halfSwapped, new Class<?>[] {Key.class, Key.class});
        byte[] badValue = write(halfSwapped);
        assertThrows(InvalidObjectException.class, () -> read(badValue));
    }

    private static void corrupt(String field, int slot, Class<?> type) throws Exception {
        M3EnumBiMap<Key, Val> m = M3EnumBiMap.create(Key.class, Val.class);
        Field f = M3EnumBiMap.class.getDeclaredField(field);
        f.setAccessible(true);
        Class<?>[] types = ((Class<?>[]) f.get(m)).clone();
        types[slot] = type;
        f.set(m, types);
        byte[] data = write(m);
        assertThrows(InvalidObjectException.class, () -> read(data));
    }

    private static byte[] write(Object o) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(o); }
        return bytes.toByteArray();
    }

    private static Object read(byte[] data) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) { return in.readObject(); }
    }

    @Test
    void invariantCheckerDetectsCorruption() throws Exception {
        expectBroken("mirror", lanes -> lanes[1][1] = 3);
        expectBroken("mirror", lanes -> lanes[1][5] = 0);
        expectBroken("mirror", lanes -> lanes[0][1] = 2);
        M3EnumBiMap<Key, Val> m = owned();
        Field size = M3EnumBiMap.class.getDeclaredField("size");
        size.setAccessible(true);
        size.setInt(m, 9);
        assertEquals("size", assertThrows(AssertionError.class, m::verifyInvariants).getMessage());
    }

    private interface Corruption {
        void apply(int[][] lanes);
    }

    private static void expectBroken(String message, Corruption corruption) throws Exception {
        M3EnumBiMap<Key, Val> m = M3EnumBiMap.create(Key.class, Val.class);
        m.put(Key.A, Val.V1);
        m.put(Key.C, Val.V5);
        m.verifyInvariants();
        Field f = M3EnumBiMap.class.getDeclaredField("lanes");
        f.setAccessible(true);
        corruption.apply((int[][]) f.get(m));
        assertEquals(message, assertThrows(AssertionError.class, m::verifyInvariants).getMessage());
    }

    @Test
    void steadyStateHotPathsAllocateNothing() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        M3EnumBiMap<Key, Val> m = owned();
        BiMap<Val, Key> inv = m.inverse();
        long[] sink = new long[1];
        BiConsumer<Key, Val> visit = (k, v) -> sink[0] += k.ordinal() + v.ordinal();
        Runnable op = () -> {
            m.put(Key.G, Val.V6);
            m.put(Key.G, Val.V7);
            sink[0] += m.containsKey(Key.G) ? 1 : 0;
            sink[0] += m.containsValue(Val.V7) ? 1 : 0;
            sink[0] += inv.get(Val.V7).ordinal();
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
        NoSuchElementException unused = new NoSuchElementException();
        assertTrue(unused.getMessage() == null);
    }
}
