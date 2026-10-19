/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.junit.jupiter.api.Test;

/** Focused contract cases against the Commons provider, plus owned-only edge and invariant cases. */
final class M3DualLinkedHashBidiMapContractTest {
    @FunctionalInterface
    interface Scenario {
        Object run(BidiMap<String, String> b) throws Exception;
    }

    private static Map<String, String> seed() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("a", "1");
        m.put("b", "2");
        m.put("c", "3");
        m.put("d", "2");
        m.put(null, "4");
        m.put("e", null);
        return m;
    }

    private static Object outcome(Scenario s, BidiMap<String, String> b) {
        try {
            return String.valueOf(s.run(b));
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void both(String name, Scenario s) {
        BidiMap<String, String> c = new DualLinkedHashBidiMap<>(seed());
        M3DualLinkedHashBidiMap<String, String> o = new M3DualLinkedHashBidiMap<>(seed());
        assertEquals(outcome(s, c), outcome(s, o), name);
        assertEquals(c.toString(), o.toString(), name + " forward");
        assertEquals(c.inverseBidiMap().toString(), o.inverseBidiMap().toString(), name + " inverse");
        o.verifyInvariants();
        M3DualLinkedHashBidiMapDifferentialTest.CHECKS.addAndGet(3);
    }

    @Test
    void constructionAndBasicOrder() {
        both("seed", b -> b);
        both("reput moves value in inverse", b -> b.put("a", "1") + " " + b.inverseBidiMap());
        both("put steals value", b -> b.put("e", "1") + " " + b);
        both("put both owned", b -> b.put("b", "3") + " " + b + " " + b.inverseBidiMap());
        both("remove and removeValue", b -> b.remove("zz") + b.remove("b") + b.removeValue("9") + b.removeValue("4"));
        both("inverse ops", b -> {
            BidiMap<String, String> i = b.inverseBidiMap();
            return i.put("7", "a") + " " + i.remove("3") + " " + i.removeValue("e") + " " + i.getKey("d") + " " + i.get("2")
                    + i.containsKey("2") + i.containsValue("d") + i.size() + i.isEmpty() + (i.inverseBidiMap() == b);
        });
        both("inverse putAll and clear", b -> {
            b.inverseBidiMap().putAll(Map.of("8", "x"));
            String s = b.toString();
            b.inverseBidiMap().clear();
            return s + b.isEmpty() + b.inverseBidiMap().isEmpty();
        });
        both("map defaults", b -> List.of(String.valueOf(b.putIfAbsent("q", "1")), String.valueOf(b.getOrDefault("zz", "dflt")),
                b.remove("a", "2"), b.replace("c", "3", "9"), String.valueOf(b.merge("c", "7", (x, y) -> x + y)),
                String.valueOf(b.compute("e", (x, y) -> y == null ? "e!" : null)), String.valueOf(b.computeIfAbsent("n", x -> "4"))));
        assertTrue(new M3DualLinkedHashBidiMap<String, String>().isEmpty());
        assertNull(new M3DualLinkedHashBidiMap<String, String>().get("x"));
    }

    @Test
    void mapIteratorMessagesAndStates() {
        both("before next", b -> {
            MapIterator<String, String> it = b.mapIterator();
            List<Object> out = new ArrayList<>();
            for (Runnable r : List.<Runnable>of(it::getKey, it::getValue, () -> it.setValue("z"), it::remove)) {
                out.add(outcome(x -> {
                    r.run();
                    return null;
                }, b));
            }
            out.add(it.toString());
            return out;
        });
        both("walk", b -> {
            MapIterator<String, String> it = b.mapIterator();
            List<Object> out = new ArrayList<>();
            out.add(it.next() + "=" + it.getValue() + " " + it);
            out.add(outcome(x -> it.setValue("3"), b));
            out.add(it.setValue("1"));
            out.add(it.setValue("11") + " " + it.getValue());
            it.remove();
            out.add(outcome(x -> {
                it.remove();
                return null;
            }, b));
            out.add(outcome(x -> it.getValue(), b));
            out.add(it.toString());
            while (it.hasNext()) { out.add(it.next()); }
            out.add(outcome(x -> it.next(), b));
            ((org.apache.commons.collections4.ResettableIterator<?>) it).reset();
            out.add(it.next());
            return out;
        });
        both("stale after external removal", b -> {
            MapIterator<String, String> it = b.mapIterator();
            it.next();
            b.remove("a");
            List<Object> out = new ArrayList<>();
            out.add(it.getKey() + "=" + it.getValue());
            out.add(it.setValue("5"));
            out.add(it.getValue());
            out.add(outcome(x -> it.next(), b));
            out.add(outcome(x -> {
                it.remove();
                return null;
            }, b));
            return out;
        });
        both("reused slot keeps old value", b -> {
            MapIterator<String, String> it = b.mapIterator();
            it.next();
            b.remove("a");
            b.put("a", "6");
            return it.getValue() + " " + b.get("a");
        });
        both("setValue on live reads back", b -> {
            MapIterator<String, String> it = b.mapIterator();
            it.next();
            it.setValue("8");
            b.put("b", "9");
            return it.getValue() + it.toString();
        });
        both("non-identical equal key refused", b -> {
            b.put(new String("c"), "3");
            MapIterator<String, String> it = b.mapIterator();
            it.next();
            it.next();
            return outcome(x -> it.setValue("3"), b);
        });
        both("concurrent structural change", b -> {
            MapIterator<String, String> it = b.mapIterator();
            it.next();
            b.put("new", "n");
            return outcome(x -> it.next(), b) + " " + outcome(x -> {
                it.remove();
                return null;
            }, b);
        });
        both("empty", b -> {
            b.clear();
            MapIterator<String, String> it = b.mapIterator();
            return it.hasNext() + outcome(x -> it.next(), b).toString();
        });
    }

    @Test
    void entriesAndViews() {
        both("entry setValue rules", b -> {
            Iterator<Map.Entry<String, String>> it = b.entrySet().iterator();
            Map.Entry<String, String> e = it.next();
            List<Object> out = new ArrayList<>();
            out.add(outcome(x -> e.setValue("2"), b));
            out.add(e.setValue("1"));
            out.add(e.setValue("10") + " " + e);
            it.remove();
            out.add(e.getValue() + " " + e.hashCode() + " " + e.equals(Map.entry("a", "10")));
            out.add(e.setValue("12") + " " + e.getValue());
            out.add(outcome(x -> {
                it.remove();
                return null;
            }, b));
            return out;
        });
        both("entry after reuse", b -> {
            Map.Entry<String, String> e = b.entrySet().iterator().next();
            b.remove("a");
            b.put("a", "7");
            return e.getValue() + " " + e.equals(e) + e.equals("x") + e.equals(Map.entry("z", "1")) + e.equals(Map.entry("a", "2"));
        });
        both("views basics", b -> List.of(b.keySet(), b.values(), b.entrySet(), b.keySet().size(), b.values().isEmpty(),
                b.keySet().contains(null), b.values().contains(null), b.entrySet().contains(Map.entry("a", "1")),
                b.entrySet().contains("a"), b.entrySet().contains(new AbstractMap.SimpleEntry<>("a", "9")),
                b.entrySet().contains(new AbstractMap.SimpleEntry<>("q", "1"))));
        both("view removals", b -> List.of(b.keySet().remove("a"), b.keySet().remove("zz"), b.values().remove("3"), b.values().remove("zz"),
                b.entrySet().remove("x"), b.entrySet().remove(Map.entry("d", "9")), b.entrySet().remove(Map.entry("q", "2")),
                b.entrySet().remove(new AbstractMap.SimpleEntry<>(null, "4")), b.toString()));
        both("view bulk rules", b -> List.of(b.keySet().removeAll(List.of()), b.keySet().removeIf(null), b.values().removeAll(Arrays.asList("1", "x")),
                b.keySet().retainAll(new HashSet<>(Arrays.asList("b", "c", null))), b.entrySet().removeIf(e -> "c".equals(e.getKey())),
                b.values().retainAll(List.of("4", "zz")), b.toString(), b.keySet().retainAll(List.of()), b.isEmpty(),
                b.keySet().retainAll(List.of()), b.values().removeAll(List.of("4")), b.entrySet().removeIf(e -> true)));
        both("view clear", b -> {
            b.values().clear();
            return b.isEmpty() + "" + b.inverseBidiMap().isEmpty() + b.keySet().isEmpty() + b.values().isEmpty();
        });
        both("view add refused", b -> outcome(x -> b.keySet().add("k"), b) + " " + b.keySet().addAll(List.of()) + " "
                + outcome(x -> b.values().addAll(List.of("q")), b));
        both("view equality", b -> List.of(b.keySet().equals(b.keySet()), b.keySet().equals(new HashSet<>(b.keySet())), b.keySet().equals(List.of()),
                b.keySet().equals(Set.of("a")), b.keySet().equals(new HashSet<>(Arrays.asList("a", "c", "d", "zz", "e"))),
                b.keySet().hashCode(), b.entrySet().equals(new HashSet<>(b.entrySet())), b.entrySet().hashCode(),
                b.values().equals(b.values()), b.values().equals(new HashSet<>(b.values())), b.keySet().equals(hostileSet(5))));
        both("view iterators", b -> {
            List<Object> out = new ArrayList<>();
            for (java.util.function.Supplier<Iterator<?>> views : List.<java.util.function.Supplier<Iterator<?>>>of(
                    () -> b.keySet().iterator(), () -> b.values().iterator(), () -> b.entrySet().iterator())) {
                Iterator<?> it = views.get();
                out.add(outcome(x -> {
                    it.remove();
                    return null;
                }, b));
                out.add(String.valueOf(it.next()));
                it.remove();
                out.add(outcome(x -> {
                    it.remove();
                    return null;
                }, b));
            }
            Iterator<String> k = b.keySet().iterator();
            k.next();
            b.remove("e");
            out.add(outcome(x -> k.next(), b));
            out.add(outcome(x -> {
                k.remove();
                return null;
            }, b));
            return out;
        });
    }

    /** A set whose hashCode-based probes throw, so equality must answer false instead of propagating. */
    private static Set<Object> hostileSet(int size) {
        hostile = false;
        Set<Object> s = new HashSet<>();
        for (int i = 0; i < size; i++) {
            int id = i;
            s.add(new Object() {
                @Override public int hashCode() {
                    if (hostile) { throw new NullPointerException("hostile"); }
                    return id;
                }
            });
        }
        hostile = true;
        return s;
    }

    private static volatile boolean hostile;

    @Test
    void equalityHashAndText() {
        both("equals forms", b -> {
            Map<String, String> copy = new LinkedHashMap<>(b);
            Map<String, String> nullValued = new HashMap<>(copy);
            nullValued.remove("e");
            nullValued.put("zz", null);
            Map<String, String> nonNull = new HashMap<>(copy);
            nonNull.put("e", "x");
            Map<String, String> otherValue = new HashMap<>(copy);
            otherValue.put("a", "x");
            Map<String, Object> sorted = new TreeMap<>();
            for (int i = 0; i < b.size(); i++) { sorted.put("k" + i, i); }
            return List.of(b.equals(b), b.equals(copy), b.equals("x"), b.equals(Map.of()), b.equals(nullValued), b.equals(nonNull),
                    b.equals(otherValue), b.equals(sorted), b.hashCode(), b.inverseBidiMap().hashCode(), b.inverseBidiMap().equals(b.inverseBidiMap()));
        });
        M3DualLinkedHashBidiMap<String, String> o = new M3DualLinkedHashBidiMap<>(seed());
        Map<Object, Object> tree = new TreeMap<>();
        for (int i = 0; i < o.size(); i++) { tree.put(i, i); }
        assertFalse(o.equals(tree));
        assertEquals("{}", new M3DualLinkedHashBidiMap<>().toString());
        assertNotEquals(new M3DualLinkedHashBidiMap<>(seed()).values(), new M3DualLinkedHashBidiMap<>(seed()).values());
        assertEquals(System.identityHashCode(o.values()), o.values().hashCode());
    }

    @Test
    void forEachFollowsTheEntryIterator() {
        both("forEach order", b -> {
            StringBuilder s = new StringBuilder();
            b.forEach((k, v) -> s.append(k).append(v));
            b.inverseBidiMap().forEach((k, v) -> s.append(k).append(v));
            return s;
        });
        both("forEach change on last is silent", b -> {
            b.forEach((k, v) -> {
                if ("e".equals(k)) { b.remove("a"); }
            });
            return b;
        });
        both("forEach change before last", b -> outcome(x -> {
            b.forEach((k, v) -> {
                if ("c".equals(k)) { b.remove("d"); }
            });
            return null;
        }, b));
        both("forEach non-structural put", b -> {
            b.forEach((k, v) -> {
                if ("a".equals(k)) { b.put("a", "77"); }
            });
            return b;
        });
        both("forEach null action", b -> outcome(x -> {
            b.forEach(null);
            return null;
        }, b));
        both("replaceAll refused midway", b -> outcome(x -> {
            b.replaceAll((k, v) -> "c".equals(k) ? "1!" : v + "!");
            return null;
        }, b));
    }

    @Test
    void serializationRoundTrip() throws Exception {
        M3DualLinkedHashBidiMap<String, String> o = new M3DualLinkedHashBidiMap<>(seed());
        Object back = read(write(o));
        assertEquals(o, back);
        @SuppressWarnings("unchecked") M3DualLinkedHashBidiMap<String, String> copy = (M3DualLinkedHashBidiMap<String, String>) back;
        copy.verifyInvariants();
        assertEquals(o.toString(), copy.toString());
        Object commonsBack = read(write(new DualLinkedHashBidiMap<>(seed())));
        assertEquals(((BidiMap<?, ?>) commonsBack).inverseBidiMap().toString(), copy.inverseBidiMap().toString(), "both rebuild the inverse order by re-putting");
        Object inverse = read(write(o.inverseBidiMap()));
        assertEquals(o.inverseBidiMap(), inverse);
        @SuppressWarnings("unchecked") BidiMap<String, String> inv = (BidiMap<String, String>) inverse;
        assertTrue(inv.inverseBidiMap() instanceof M3DualLinkedHashBidiMap);
        assertSame(inv, inv.inverseBidiMap().inverseBidiMap());
        assertThrows(InvalidObjectException.class, () -> read(corrupt(-1)));
        assertEquals(Map.of(7, 7), read(corrupt(1)));
    }

    private static byte[] write(Object o) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(o); }
        return bytes.toByteArray();
    }

    private static Object read(byte[] data) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) { return in.readObject(); }
    }

    private static byte[] corrupt(int size) throws Exception {
        M3DualLinkedHashBidiMap<Integer, Integer> one = new M3DualLinkedHashBidiMap<>();
        one.put(7, 7);
        byte[] data = write(one);
        List<Integer> at = new ArrayList<>();
        for (int i = 0; i + 6 <= data.length; i++) {
            if (data[i] == 0x77 && data[i + 1] == 4 && java.nio.ByteBuffer.wrap(data, i + 2, 4).getInt() == 1) { at.add(i + 2); }
        }
        assertEquals(1, at.size(), "owned stream layout");
        java.nio.ByteBuffer.wrap(data, at.get(0), 4).putInt(size);
        return data;
    }

    @Test
    void capacityGuardsAndChurn() {
        assertEquals(8, M3DualLinkedHashBidiMap.Table.grown(0));
        assertEquals(Integer.MAX_VALUE - 8, M3DualLinkedHashBidiMap.Table.grown(Integer.MAX_VALUE - 9));
        assertThrows(OutOfMemoryError.class, () -> M3DualLinkedHashBidiMap.Table.grown(Integer.MAX_VALUE - 8));
        assertEquals(32, M3DualLinkedHashBidiMap.Table.doubled(16));
        assertThrows(OutOfMemoryError.class, () -> M3DualLinkedHashBidiMap.Table.doubled(1 << 30));
        M3DualLinkedHashBidiMap<Integer, Integer> churn = new M3DualLinkedHashBidiMap<>();
        BidiMap<Integer, Integer> oracle = new DualLinkedHashBidiMap<>();
        for (int i = 0; i < 6_000; i++) {
            int k = i % 211 * 16;
            int v = (i * 7) % 223 * 32;
            assertEquals(oracle.put(k, v), churn.put(k, v));
            if (i % 3 == 0) { assertEquals(oracle.remove((i * 5) % 211 * 16), churn.remove((i * 5) % 211 * 16)); }
            if (i % 500 == 0) { churn.verifyInvariants(); }
        }
        for (int i = 0; i < 3_000; i++) {
            assertEquals(oracle.put(10_000 + i, -i), churn.put(10_000 + i, -i));
            assertEquals(oracle.removeValue(-i), churn.removeValue(-i));
        }
        churn.verifyInvariants();
        assertEquals(oracle.toString(), churn.toString());
        assertEquals(oracle.inverseBidiMap().toString(), churn.inverseBidiMap().toString());
        M3DualLinkedHashBidiMap<String, String> collide = new M3DualLinkedHashBidiMap<>();
        collide.put("Aa", "x");
        collide.put("BB", "y");
        assertEquals("y", collide.get("BB"));
        assertNull(collide.get("C#"));
        collide.clear();
        collide.put("BB", "z");
        collide.verifyInvariants();
        assertEquals("{BB=z}", collide.toString());
    }

    @Test
    void invariantCheckerDetectsCorruption() {
        expectBroken("links", (m, t, o) -> t.after[t.last] = t.first);
        expectBroken("links", (m, t, o) -> t.before[t.after[t.first]] = 5);
        expectBroken("links", (m, t, o) -> t.positions[t.first] = emptyBucket(t));
        expectBroken("links", (m, t, o) -> t.hashes[t.first] ^= 1);
        expectBroken("links", (m, t, o) -> o.erase(o.first));
        expectBroken("links", (m, t, o) -> o.values[o.first] = "zz");
        expectBroken("size", (m, t, o) -> t.size = 99);
        expectBroken("size", (m, t, o) -> t.last = t.first);
        expectBroken("free", (m, t, o) -> t.free = t.first);
        expectBroken("free", (m, t, o) -> t.keys[t.free] = "k");
        expectBroken("free", (m, t, o) -> t.positions[t.free] = 0);
        expectBroken("ownership", (m, t, o) -> t.used++);
        expectBroken("ownership", (m, t, o) -> o.used++);
    }

    private interface Corruption {
        void apply(M3DualLinkedHashBidiMap<String, String> m, M3DualLinkedHashBidiMap.Table forward, M3DualLinkedHashBidiMap.Table inverse);
    }

    private static int emptyBucket(M3DualLinkedHashBidiMap.Table t) {
        for (int p = 0; p < t.index.length; p++) {
            if (t.index[p] == 0) { return p; }
        }
        throw new AssertionError("no empty bucket");
    }

    private static void expectBroken(String message, Corruption corrupt) {
        M3DualLinkedHashBidiMap<String, String> m = new M3DualLinkedHashBidiMap<>();
        m.put("a", "1");
        m.put("b", "2");
        m.put("c", "3");
        m.put("d", "4");
        m.remove("d");
        m.verifyInvariants();
        M3DualLinkedHashBidiMap.Table[] tables = tables(m);
        corrupt.apply(m, tables[0], tables[1]);
        AssertionError e = assertThrows(AssertionError.class, m::verifyInvariants);
        assertEquals(message, e.getMessage());
    }

    private static M3DualLinkedHashBidiMap.Table[] tables(Object m) {
        try {
            Field f = M3DualLinkedHashBidiMap.class.getDeclaredField("tables");
            f.setAccessible(true);
            return (M3DualLinkedHashBidiMap.Table[]) f.get(m);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void steadyStateHotPathsAllocateNothing() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        M3DualLinkedHashBidiMap<Integer, Integer> map = new M3DualLinkedHashBidiMap<>();
        Integer[] boxes = new Integer[256];
        for (int i = 0; i < boxes.length; i++) { boxes[i] = 1_000 + i; }
        for (int i = 0; i < 64; i++) { map.put(boxes[i], boxes[128 + i]); }
        long[] sink = new long[1];
        BiConsumer<Integer, Integer> add = (k, v) -> sink[0] += k - v;
        Integer k = boxes[7];
        Integer v = boxes[135];
        Integer spare = boxes[250];
        Runnable op = () -> {
            map.put(k, spare);
            sink[0] += map.get(k) + map.getKey(spare);
            sink[0] += map.containsKey(k) && map.containsValue(spare) ? 1 : 0;
            map.put(k, v);
            map.removeValue(v);
            map.put(k, v);
            map.inverseBidiMap().remove(v);
            map.inverseBidiMap().put(v, k);
            sink[0] += map.size() + map.hashCode();
            map.forEach(add);
        };
        for (int i = 0; i < 20_000; i++) { op.run(); }
        long best = Long.MAX_VALUE;
        long id = Thread.currentThread().threadId();
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 20_000; i++) { op.run(); }
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state allocation in put/get/getKey/contains/removeValue/inverse/hashCode/forEach");
        assertTrue(sink[0] != 0);
        assertSame(map.keySet(), map.keySet());
        assertSame(map.values(), map.values());
        assertSame(map.entrySet(), map.entrySet());
        assertSame(map.inverseBidiMap(), map.inverseBidiMap());
        BidiMap<Integer, Integer> inverse = map.inverseBidiMap();
        assertSame(inverse.keySet(), inverse.keySet());
        assertSame(inverse.values(), inverse.values());
        assertSame(inverse.entrySet(), inverse.entrySet());
        map.verifyInvariants();
    }

    @Test
    void mutatedValueHashLeavesTheInverseStaleLikeCommons() {
        final class Box {
            int h;

            Box(int h) {
                this.h = h;
            }

            @Override public int hashCode() {
                return h;
            }
        }
        Box box = new Box(1);
        BidiMap<String, Box> c = new DualLinkedHashBidiMap<>();
        M3DualLinkedHashBidiMap<String, Box> o = new M3DualLinkedHashBidiMap<>();
        c.put("k", box);
        o.put("k", box);
        box.h = 99;
        assertSame(c.remove("k"), o.remove("k"));
        assertEquals(c.toString(), o.toString());
        assertEquals(c.inverseBidiMap().size(), o.inverseBidiMap().size());
        box.h = 1;
        assertEquals(c.inverseBidiMap().get(box), o.inverseBidiMap().get(box));
    }

    @Test
    void failFastIteratorsMatchLinkedHashMap() {
        both("entry iterator CME", b -> {
            Iterator<Map.Entry<String, String>> it = b.entrySet().iterator();
            it.next();
            b.inverseBidiMap().remove("3");
            return outcome(x -> it.next(), b) + " " + outcome(x -> {
                it.remove();
                return null;
            }, b);
        });
        both("value iterator survives value change", b -> {
            Iterator<String> it = b.values().iterator();
            List<String> out = new ArrayList<>();
            out.add(it.next());
            b.put("c", "20");
            while (it.hasNext()) { out.add(String.valueOf(it.next())); }
            return out + " " + outcome(x -> it.next(), b);
        });
        M3DualLinkedHashBidiMap<String, String> o = new M3DualLinkedHashBidiMap<>(seed());
        Iterator<String> it = o.keySet().iterator();
        o.clear();
        assertTrue(it.hasNext());
        assertThrows(ConcurrentModificationException.class, it::next);
        assertThrows(NoSuchElementException.class, () -> new M3DualLinkedHashBidiMap<>().entrySet().iterator().next());
    }
}
