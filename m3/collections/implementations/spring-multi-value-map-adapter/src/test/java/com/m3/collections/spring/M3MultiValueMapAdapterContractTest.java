/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

/** Focused contract cases against the Spring provider, plus owned-only allocation and serialization cases. */
final class M3MultiValueMapAdapterContractTest {
    @FunctionalInterface
    interface Scenario {
        Object run(MultiValueMap<String, String> m, Map<String, List<String>> target) throws Exception;
    }

    private static Map<String, List<String>> seed() {
        Map<String, List<String>> t = new LinkedHashMap<>();
        t.put("a", new ArrayList<>(List.of("1", "2")));
        t.put("empty", new ArrayList<>());
        t.put("none", null);
        t.put(null, new ArrayList<>(Arrays.asList((String) null)));
        t.put("fixed", List.of("f"));
        return t;
    }

    private static void both(String name, Scenario s) {
        Map<String, List<String>> ct = seed();
        Map<String, List<String>> ot = seed();
        Object c = outcome(s, new MultiValueMapAdapter<>(ct), ct);
        Object o = outcome(s, new M3MultiValueMapAdapter<>(ot), ot);
        assertEquals(c, o, name);
        assertEquals(ct.toString(), ot.toString(), name + " target");
        assertEquals(shapes(ct), shapes(ot), name + " list classes");
        M3MultiValueMapAdapterDifferentialTest.CHECKS.addAndGet(3);
    }

    private static String shapes(Map<String, List<String>> target) {
        StringBuilder s = new StringBuilder();
        target.forEach((k, l) -> s.append(k).append(':').append(l == null ? "null" : l.getClass().getName()).append(';'));
        return s.toString();
    }

    private static Object outcome(Scenario s, MultiValueMap<String, String> m, Map<String, List<String>> target) {
        try {
            return String.valueOf(s.run(m, target));
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getName() + ": " + e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Object quiet(java.util.concurrent.Callable<?> action) {
        return M3MultiValueMapAdapterDifferentialTest.outcome(action);
    }

    @Test
    void constructionAndWriteThrough() {
        assertEquals(assertThrows(IllegalArgumentException.class, () -> new MultiValueMapAdapter<String, String>(null)).getMessage(),
                assertThrows(IllegalArgumentException.class, () -> new M3MultiValueMapAdapter<String, String>(null)).getMessage());
        both("seed views", (m, t) -> List.of(m, m.size(), m.keySet(), m.values(), m.entrySet()));
        both("adds land in the target and alias caller lists", (m, t) -> {
            List<String> caller = t.get("a");
            m.add("a", "3");
            m.add("none", "n");
            m.add("new", null);
            return caller + " " + quiet(() -> {
                m.add("fixed", "x");
                return null;
            });
        });
        both("target writes are visible", (m, t) -> {
            t.put("direct", new ArrayList<>(List.of("d")));
            return m.getFirst("direct") + m.get("direct") + m.containsKey("direct") + m.size();
        });
        both("getFirst shapes", (m, t) -> Arrays.asList(m.getFirst("a"), m.getFirst("empty"), m.getFirst("none"), m.getFirst(null), m.getFirst("zz")));
        both("addAll null list", (m, t) -> quiet(() -> {
            m.addAll("absent", null);
            return null;
        }) + " " + quiet(() -> {
            m.addAll("a", null);
            return null;
        }) + " " + m.containsKey("absent"));
        both("addAll lists and maps", (m, t) -> {
            m.addAll("a", List.of("x", "y"));
            m.addAll("fresh", List.of("z"));
            MultiValueMap<String, String> other = new M3MultiValueMapAdapter<>(new LinkedHashMap<>());
            other.add("fresh", "w");
            other.add("q", "v");
            m.addAll(other);
            return m;
        });
        both("addIfAbsent", (m, t) -> {
            m.addIfAbsent("a", "no");
            m.addIfAbsent("none", "no");
            m.addIfAbsent("b", "yes");
            return m;
        });
        both("set and setAll", (m, t) -> {
            m.set("a", null);
            Map<String, String> all = new LinkedHashMap<>();
            all.put("fixed", "g");
            all.put("s", "t");
            m.setAll(all);
            return m;
        });
        both("toSingleValueMap", (m, t) -> m.toSingleValueMap() + " " + m.toSingleValueMap().getClass().getName());
        both("map surface", (m, t) -> List.of(String.valueOf(m.put("p", new ArrayList<>())), String.valueOf(m.putIfAbsent("p", List.of())),
                String.valueOf(m.remove("p")), m.containsValue(List.of("f")), m.containsValue(null), m.equals(m), m.equals(seed()), m.equals("x"),
                m.hashCode() == seed().hashCode(), m.toString()));
        both("putAll and clear", (m, t) -> {
            Map<String, List<String>> more = new LinkedHashMap<>();
            more.put("m", List.of("1"));
            m.putAll(more);
            String before = m.toString();
            m.clear();
            return before + m.isEmpty() + t.isEmpty();
        });
        both("forEach", (m, t) -> {
            StringBuilder s = new StringBuilder();
            m.forEach((k, v) -> s.append(k).append(v));
            return s;
        });
        both("map defaults dispatch through the adapter", (m, t) -> List.of(String.valueOf(m.computeIfAbsent("none", k -> new ArrayList<>(List.of("c")))),
                String.valueOf(m.merge("a", List.of("m"), (x, y) -> null)), String.valueOf(m.getOrDefault("zz", List.of())),
                String.valueOf(m.compute("empty", (k, v) -> v == null ? null : List.of("e")))));
    }

    @Test
    void singleValueViewProtocol() {
        both("fresh view per call, cached subviews", (m, t) -> {
            Map<String, String> v = m.asSingleValueMap();
            return (m.asSingleValueMap() != v) + " " + (v.values() == v.values()) + (v.entrySet() == v.entrySet()) + " " + v.keySet();
        });
        both("adapted reads", (m, t) -> {
            Map<String, String> v = m.asSingleValueMap();
            return Arrays.asList(v.get("a"), v.get("empty"), v.get("none"), v.get(null), v.get("zz"), v.size(), v.isEmpty(), v.containsKey("none"),
                    v.getOrDefault("none", "dflt"), v.hashCode(), v.toString());
        });
        both("adapted writes", (m, t) -> {
            Map<String, String> v = m.asSingleValueMap();
            List<Object> out = Arrays.asList(v.put("a", "9"), v.put("b", null), v.remove("a"), v.remove("zz"), v.remove("empty"));
            Map<String, String> more = new LinkedHashMap<>();
            more.put("c", "3");
            v.putAll(more);
            return out + " " + shapes(t);
        });
        both("first-value walks fail on empty lists", (m, t) -> {
            Map<String, String> v = m.asSingleValueMap();
            return List.of(quiet(() -> new ArrayList<>(v.values())), quiet(() -> new ArrayList<>(v.entrySet())), quiet(() -> v.containsValue("1")),
                    quiet(() -> {
                        v.forEach((k, x) -> { });
                        return null;
                    }), quiet(() -> v.equals(new HashMap<>(v))));
        });
        both("first-value walks fail on null lists", (m, t) -> {
            t.remove("empty");
            Map<String, String> v = m.asSingleValueMap();
            return List.of(quiet(() -> new ArrayList<>(v.values())), quiet(() -> v.containsValue("1")), quiet(() -> v.equals(new HashMap<>())),
                    quiet(() -> {
                        Map<String, String> same = new HashMap<>();
                        for (int i = 0; i < v.size(); i++) { same.put("x" + i, "y"); }
                        return v.equals(same);
                    }));
        });
        both("clean walks", (m, t) -> {
            t.remove("empty");
            t.remove("none");
            Map<String, String> v = m.asSingleValueMap();
            StringBuilder s = new StringBuilder();
            v.forEach((k, x) -> s.append(k).append('=').append(x).append(';'));
            return List.of(new ArrayList<>(v.values()), new ArrayList<>(v.entrySet()), v.containsValue("f"), v.containsValue(null),
                    v.containsValue("zz"), s, v.values().size(), v.entrySet().size(), quiet(() -> {
                        v.values().iterator().remove();
                        return null;
                    }), quiet(() -> {
                        var it = v.entrySet().iterator();
                        it.next();
                        it.remove();
                        return null;
                    }));
        });
        both("single view equality", (m, t) -> {
            t.remove("empty");
            t.remove("none");
            Map<String, String> v = m.asSingleValueMap();
            Map<String, String> copy = m.toSingleValueMap();
            Map<String, String> nullMismatch = new LinkedHashMap<>(copy);
            nullMismatch.put(null, "x");
            Map<String, String> nullAbsent = new LinkedHashMap<>(copy);
            nullAbsent.remove(null);
            nullAbsent.put("other", null);
            Map<String, String> valueMismatch = new LinkedHashMap<>(copy);
            valueMismatch.put("a", "zz");
            Map<Object, Object> typed = new TreeMap<>();
            for (int i = 0; i < v.size(); i++) { typed.put(i, i); }
            return List.of(v.equals(v), v.equals(copy), v.equals("x"), v.equals(Map.of()), v.equals(nullMismatch), v.equals(nullAbsent),
                    v.equals(valueMismatch), quiet(() -> v.equals(typed)));
        });
        both("single view clear", (m, t) -> {
            Map<String, String> v = m.asSingleValueMap();
            v.clear();
            return v.isEmpty() + "" + t.isEmpty();
        });
    }

    @Test
    void serializationRoundTrip() throws Exception {
        Map<String, List<String>> target = new LinkedHashMap<>();
        target.put("a", new ArrayList<>(List.of("1", "2")));
        M3MultiValueMapAdapter<String, String> o = new M3MultiValueMapAdapter<>(target);
        MultiValueMap<String, String> spring = new MultiValueMapAdapter<>(new LinkedHashMap<>(target));
        Object back = read(write(o));
        assertEquals(o, back);
        assertEquals(read(write(spring)).toString(), back.toString());
        @SuppressWarnings("unchecked") MultiValueMap<String, String> copy = (MultiValueMap<String, String>) back;
        copy.add("a", "3");
        assertEquals(List.of("1", "2"), target.get("a"), "a deserialized adapter owns a copied target");
        Object view = read(write(o.asSingleValueMap()));
        assertEquals(read(write(spring.asSingleValueMap())).toString(), view.toString());
        assertEquals(o.asSingleValueMap(), view);
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
    void steadyStateHotPathsAllocateNothing() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        Map<Integer, List<Integer>> target = new HashMap<>();
        M3MultiValueMapAdapter<Integer, Integer> m = new M3MultiValueMapAdapter<>(target);
        Integer[] boxes = new Integer[64];
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = 1_000 + i;
            m.add(boxes[i], boxes[i]);
            m.add(boxes[i], boxes[i]);
        }
        Map<Integer, Integer> single = m.asSingleValueMap();
        long[] sink = new long[1];
        BiConsumer<Integer, List<Integer>> count = (k, v) -> sink[0] += v.size();
        Integer k = boxes[9];
        Runnable op = () -> {
            m.add(k, k);
            List<Integer> values = m.get(k);
            values.remove(values.size() - 1);
            sink[0] += m.getFirst(k) + single.get(k) + m.size();
            sink[0] += m.containsKey(k) && single.containsKey(k) ? 1 : 0;
            m.forEach(count);
        };
        for (int i = 0; i < 20_000; i++) { op.run(); }
        long best = Long.MAX_VALUE;
        long id = Thread.currentThread().threadId();
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 20_000; i++) { op.run(); }
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state allocation in add/get/getFirst/single get/containsKey/size/forEach");
        assertTrue(sink[0] != 0);
        assertNotSame(m.asSingleValueMap(), m.asSingleValueMap());
        assertSame(single.values(), single.values());
        assertFalse(m.isEmpty());
        assertEquals(2, target.get(k).size());
    }
}
