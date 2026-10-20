/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import org.junit.jupiter.api.Test;

/** Focused contract cases against the Guava provider, plus owned-only order, edge and invariant cases. */
final class M3LinkedHashMultisetContractTest {
    @FunctionalInterface
    interface Scenario {
        Object run(Multiset<Integer> m) throws Exception;
    }

    private static Multiset<Integer> guava() {
        Multiset<Integer> m = LinkedHashMultiset.create();
        m.add(1, 3);
        m.add(2);
        m.add(3, 2);
        return m;
    }

    private static Object outcome(Scenario s, Multiset<Integer> m) {
        try {
            return M3LinkedHashMultisetDifferentialTest.normalize(s.run(m));
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** Compares results only: Guava leaves an inconsistent state after this CME, the owned class does not. */
    private static void outcomeOnly(String name, List<Integer> ownedAfter, Scenario s) {
        M3LinkedHashMultiset<Integer> o = M3LinkedHashMultiset.create(guava());
        assertEquals(outcome(s, guava()), outcome(s, o), name);
        assertEquals(ownedAfter, new ArrayList<>(o), name + " owned state");
        o.verifyInvariants();
        M3LinkedHashMultisetDifferentialTest.CHECKS.addAndGet(2);
    }

    private static void both(String name, Scenario s) {
        Multiset<Integer> g = guava();
        M3LinkedHashMultiset<Integer> o = M3LinkedHashMultiset.create(guava());
        assertEquals(outcome(s, g), outcome(s, o), name);
        assertEquals(g.toString(), o.toString(), name + " state");
        assertEquals(g, o, name + " equality");
        o.verifyInvariants();
        M3LinkedHashMultisetDifferentialTest.CHECKS.addAndGet(3);
    }

    @Test
    void factoriesAndArguments() {
        IllegalArgumentException g = assertThrows(IllegalArgumentException.class, () -> LinkedHashMultiset.create(-1));
        IllegalArgumentException o = assertThrows(IllegalArgumentException.class, () -> M3LinkedHashMultiset.create(-1));
        assertEquals(g.getMessage(), o.getMessage());
        assertEquals(LinkedHashMultiset.create(List.of(2, 1, 1)).toString(), M3LinkedHashMultiset.create(List.of(2, 1, 1)).toString());
        assertEquals(guava().toString(), M3LinkedHashMultiset.create(guava()).toString());
        assertEquals(guava().toString(), M3LinkedHashMultiset.create(M3LinkedHashMultiset.create(guava())).toString());
        Iterable<Integer> iterable = List.of(5, 4, 4)::iterator;
        assertEquals(LinkedHashMultiset.create(iterable).toString(), M3LinkedHashMultiset.create(iterable).toString());
        M3LinkedHashMultiset<Integer> sized = M3LinkedHashMultiset.create(100);
        for (int i = 0; i < 1_000; i++) { sized.add(i % 300); }
        sized.verifyInvariants();
        assertEquals(1_000, sized.size());
        assertEquals(0, sized.elementSet().iterator().next());
        assertThrows(NullPointerException.class, () -> M3LinkedHashMultiset.create((Iterable<Integer>) null));
        both("add negative", m -> m.add(1, -1));
        both("remove negative", m -> m.remove(1, -1));
        both("setCount negative", m -> m.setCount(1, -1));
        both("setCount old negative", m -> m.setCount(1, -1, 0));
        both("setCount new negative", m -> m.setCount(1, 0, -1));
        both("add zero", m -> List.of(m.add(1, 0), m.add(9, 0)));
        both("remove zero", m -> List.of(m.remove(1, 0), m.remove(9, 0)));
        both("too many", m -> m.add(1, Integer.MAX_VALUE));
        both("max new", m -> List.of(m.add(9, Integer.MAX_VALUE), m.size()));
        both("saturated size", m -> {
            m.add(8, Integer.MAX_VALUE);
            m.add(9, Integer.MAX_VALUE);
            return List.of(m.size(), m.count(8), m.isEmpty());
        });
        both("setCount transitions", m -> List.of(m.setCount(1, 5), m.setCount(1, 0), m.setCount(7, 0), m.setCount(7, 2), m.count(7)));
        both("setCount cas", m -> List.of(m.setCount(2, 1, 4), m.setCount(2, 1, 0), m.setCount(9, 0, 3), m.setCount(9, 3, 0), m.setCount(5, 0, 0)));
        both("nulls", m -> List.of(m.add(null), m.add(null, 2), m.count(null), m.contains(null), m.remove(null, 1), m.setCount(null, 0)));
        both("addAll empty", m -> List.of(m.addAll(List.of()), m.addAll(LinkedHashMultiset.create()), m.addAll(M3LinkedHashMultiset.create())));
        both("addAll self", m -> m.addAll(m));
        both("addAll owned", m -> m.addAll(M3LinkedHashMultiset.create(List.of(9, 1, 9))));
        both("addAll immutable", m -> m.addAll(ImmutableMultiset.of(6, 2, 2)));
        both("addAll null", m -> m.addAll(null));
        both("removeAll null", m -> m.removeAll(null));
        both("retainAll null", m -> m.retainAll(null));
        both("retainAll multiset", m -> m.retainAll(HashMultiset.create(List.of(1, 3))));
        both("removeAll multiset", m -> m.removeAll(HashMultiset.create(List.of(1, 9))));
        both("elementSet removeAll multiset", m -> m.elementSet().removeAll(HashMultiset.create(List.of(2, 3))));
        both("elementSet removeAll null", m -> m.elementSet().removeAll(null));
        both("elementSet removeAll big set", m -> m.elementSet().removeAll(new java.util.HashSet<>(List.of(1, 5, 6, 7, 8))));
    }

    @Test
    void viewsEntriesAndText() {
        both("elementSet ops", m -> List.of(m.elementSet().contains(1), m.elementSet().remove(1), m.elementSet().remove(1), m.elementSet().size(),
                m.elementSet().isEmpty()));
        both("elementSet clear", m -> {
            m.elementSet().clear();
            return m.isEmpty();
        });
        both("entrySet clear", m -> {
            m.entrySet().clear();
            return m.size();
        });
        both("entrySet contains", m -> List.of(m.entrySet().contains(Multisets.immutableEntry(1, 3)), m.entrySet().contains(Multisets.immutableEntry(1, 0)),
                m.entrySet().contains(Multisets.immutableEntry(9, 0)), m.entrySet().contains("x"), m.entrySet().contains(Multisets.immutableEntry(1, 2))));
        both("entrySet remove", m -> List.of(m.entrySet().remove(Multisets.immutableEntry(1, 2)), m.entrySet().remove(Multisets.immutableEntry(1, 0)),
                m.entrySet().remove("x"), m.entrySet().remove(Multisets.immutableEntry(1, 3))));
        both("live entry", m -> {
            Multiset.Entry<Integer> e = m.entrySet().iterator().next();
            m.add(1, 2);
            int five = e.getCount();
            m.setCount(1, 0);
            int gone = e.getCount();
            return List.of(five, gone, e.toString(), e.equals(Multisets.immutableEntry(1, 0)), e.hashCode());
        });
        both("entry text", m -> {
            List<String> out = new ArrayList<>();
            for (Multiset.Entry<Integer> e : m.entrySet()) {
                out.add(e.toString() + "|" + e.hashCode() + "|" + e.equals(e) + "|" + e.equals("x") + "|" + e.equals(Multisets.immutableEntry(e.getElement(), 9))
                        + "|" + e.equals(Multisets.immutableEntry(9, e.getCount())));
            }
            return out;
        });
        both("equality", m -> List.of(m.equals(m), m.equals("x"), m.equals(HashMultiset.create(List.of(1, 2, 3))),
                m.equals(HashMultiset.create(List.of(1, 1, 1, 2, 3, 4))), m.equals(HashMultiset.create(List.of(1, 1, 1, 2, 2, 3))),
                m.equals(LinkedHashMultiset.create(List.of(3, 3, 2, 1, 1, 1)))));
        both("text", m -> {
            m.clear();
            m.add(4, 2);
            m.add(5);
            return List.of(m.toString(), m.entrySet().toString(), m.elementSet().toString());
        });
        both("streams", m -> List.of(m.stream().count(), m.spliterator().estimateSize(), m.spliterator().characteristics(), m.elementSet().spliterator().estimateSize(),
                m.entrySet().spliterator().estimateSize(), m.elementSet().spliterator().hasCharacteristics(Spliterator.DISTINCT),
                m.parallelStream().toList(), m.stream().toList()));
        both("spliterator walk", m -> {
            Spliterator<Integer> s = m.spliterator();
            List<Object> out = new ArrayList<>();
            while (s.tryAdvance(out::add)) { out.add(s.estimateSize()); }
            out.add(s.tryAdvance(out::add));
            out.add(s.estimateSize());
            return out;
        });
        both("spliterator null action", m -> m.spliterator().tryAdvance(null));
        both("toArray", m -> List.of(List.of(m.toArray(new Integer[0])), m.toArray(new Integer[9]).length));
        both("containsAll", m -> List.of(m.containsAll(List.of(1, 2)), m.containsAll(List.of(1, 9)), m.elementSet().containsAll(List.of(3))));
        both("forEachEntry", m -> {
            List<String> seen = new ArrayList<>();
            m.forEachEntry((x, n) -> seen.add(x + ":" + n));
            return seen;
        });
        both("forEach adds occurrences", m -> {
            List<Integer> seen = new ArrayList<>();
            m.forEach(x -> {
                seen.add(x);
                if (x == 2) { m.add(2); }
            });
            return List.of(seen, m.count(2));
        });
        both("empty", m -> {
            m.clear();
            return List.of(m.isEmpty(), m.size(), m.entrySet().isEmpty(), m.iterator().hasNext(), m.toString(), m.hashCode());
        });
    }

    @Test
    void iteratorsAndFailFast() {
        both("occurrence iterator", m -> {
            List<Integer> seen = new ArrayList<>();
            for (Integer x : m) { seen.add(x); }
            return seen;
        });
        both("iterator remove twice", m -> {
            Iterator<Integer> it = m.iterator();
            it.next();
            it.remove();
            it.remove();
            return null;
        });
        both("iterator remove before next", m -> {
            m.iterator().remove();
            return null;
        });
        both("iterator exhausted", m -> {
            Iterator<Integer> it = m.iterator();
            while (it.hasNext()) { it.next(); }
            return it.next();
        });
        both("element iterator exhausted", m -> {
            Iterator<Integer> it = m.elementSet().iterator();
            while (it.hasNext()) { it.next(); }
            return it.next();
        });
        both("new element during iteration", m -> {
            Iterator<Integer> it = m.elementSet().iterator();
            it.next();
            m.add(9);
            return it.next();
        });
        both("new element after exhaustion", m -> {
            Iterator<Integer> it = m.elementSet().iterator();
            while (it.hasNext()) { it.next(); }
            m.add(9);
            return List.of(it.hasNext());
        });
        both("count change during iteration", m -> {
            Iterator<Integer> it = m.elementSet().iterator();
            it.next();
            m.add(1, 4);
            m.remove(3, 1);
            List<Integer> rest = new ArrayList<>();
            while (it.hasNext()) { rest.add(it.next()); }
            return rest;
        });
        both("removed element during iteration", m -> {
            Iterator<Multiset.Entry<Integer>> it = m.entrySet().iterator();
            it.next();
            m.setCount(2, 0);
            return it.next().getElement();
        });
        both("occurrence remove after external removal", m -> {
            Iterator<Integer> it = m.iterator();
            Integer x = it.next();
            m.setCount(x, 0);
            m.add(x);
            it.remove();
            return null;
        });
        both("occurrence remove after count drop", m -> {
            Iterator<Integer> it = m.iterator();
            Integer x = it.next();
            m.remove(x, 1);
            it.remove();
            return List.of(m.count(x), m.toString());
        });
        both("occurrence remove after external removal of another element", m -> {
            Iterator<Integer> it = m.iterator();
            it.next();
            m.setCount(3, 0);
            it.remove();
            return m.toString();
        });
        outcomeOnly("stale iterator remove (Guava zeroes the count before throwing; owned refuses first)", List.of(1, 1, 1, 2, 3, 3, 9), m -> {
            Iterator<Integer> it = m.elementSet().iterator();
            it.next();
            m.add(9);
            it.remove();
            return null;
        });
        outcomeOnly("forEachEntry structural (Guava LinkedHashMap.forEach keeps visiting, then throws)", List.of(1, 1, 1, 2, 3, 3, 11), m -> {
            m.forEachEntry((x, n) -> m.add(x + 10));
            return null;
        });
        both("forEach structural", m -> {
            m.forEach(x -> m.setCount(x + 10, 1));
            return null;
        });
        both("forEachEntry null", m -> {
            m.forEachEntry((ObjIntConsumer<Integer>) null);
            return null;
        });
        both("forEach null", m -> {
            m.forEach((Consumer<Integer>) null);
            return null;
        });
        both("entry iterator remove twice", m -> {
            Iterator<Multiset.Entry<Integer>> it = m.entrySet().iterator();
            it.next();
            it.remove();
            it.remove();
            return null;
        });
        both("removeIf all", m -> List.of(m.removeIf(x -> true), m.isEmpty()));
    }

    @Test
    void insertionOrderIsKept() {
        both("re-add goes to the end", m -> {
            m.setCount(1, 0);
            m.add(1);
            return m.toString();
        });
        both("count changes keep position", m -> {
            m.add(1, 5);
            m.remove(3);
            m.setCount(2, 7);
            return List.of(m.toString(), new ArrayList<>(m.elementSet()), new ArrayList<>(m.entrySet()));
        });
        both("remove first and middle through iterators", m -> {
            m.add(4);
            m.add(5);
            Iterator<Integer> it = m.elementSet().iterator();
            it.next();
            it.remove();
            it.next();
            it.next();
            it.remove();
            List<Integer> rest = new ArrayList<>();
            while (it.hasNext()) { rest.add(it.next()); }
            return List.of(rest, m.toString());
        });
        both("remove everything through an entry iterator", m -> {
            m.add(4);
            Iterator<Multiset.Entry<Integer>> it = m.entrySet().iterator();
            List<Object> out = new ArrayList<>();
            while (it.hasNext()) {
                out.add(it.next().toString());
                it.remove();
                out.add(m.toString());
            }
            return out;
        });
        both("remove last offset then continue", m -> {
            Iterator<Integer> it = m.elementSet().iterator();
            it.next();
            it.next();
            m.add(4);
            return "guarded";
        });
        both("elementSet removeAll keeps the rest in order", m -> {
            m.add(4);
            m.add(5);
            m.add(6);
            m.elementSet().removeAll(new java.util.HashSet<>(List.of(1, 3, 5, 7, 8, 9, 10)));
            return m.toString();
        });
        both("retainAll keeps order", m -> {
            m.add(4);
            m.retainAll(List.of(4, 2));
            return m.toString();
        });
        both("addAll order", m -> {
            m.addAll(List.of(9, 2, 8));
            m.addAll(LinkedHashMultiset.create(List.of(7, 1, 7)));
            return m.toString();
        });
    }

    @Test
    void serializationRoundTrip() throws Exception {
        M3LinkedHashMultiset<Integer> o = M3LinkedHashMultiset.create(guava());
        o.add(null, 2);
        o.add(0);
        Object back = read(write(o));
        assertEquals(o, back);
        assertEquals(o.toString(), back.toString());
        @SuppressWarnings("unchecked") M3LinkedHashMultiset<Integer> copy = (M3LinkedHashMultiset<Integer>) back;
        copy.verifyInvariants();
        copy.add(1);
        assertEquals(4, copy.count(1));
        assertThrows(InvalidObjectException.class, () -> read(corrupt(-1, 1)));
        assertThrows(InvalidObjectException.class, () -> read(corrupt(1, 0)));
        assertEquals(HashMultiset.create(List.of(7)), read(corrupt(1, 1)));
    }

    private static byte[] write(Object o) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(o); }
        return bytes.toByteArray();
    }

    private static Object read(byte[] data) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) { return in.readObject(); }
    }

    /** Rewrites the element count and the first count (TC_BLOCKDATA int payloads 77 04 00 00 00 01). */
    private static byte[] corrupt(int elements, int count) throws Exception {
        M3LinkedHashMultiset<Integer> one = M3LinkedHashMultiset.create();
        one.add(7);
        byte[] data = write(one);
        List<Integer> at = new ArrayList<>();
        for (int i = 0; i + 6 <= data.length; i++) {
            if (data[i] == 0x77 && data[i + 1] == 4 && java.nio.ByteBuffer.wrap(data, i + 2, 4).getInt() == 1) { at.add(i + 2); }
        }
        assertEquals(2, at.size(), "owned stream layout");
        java.nio.ByteBuffer.wrap(data, at.get(0), 4).putInt(elements);
        java.nio.ByteBuffer.wrap(data, at.get(1), 4).putInt(count);
        return data;
    }

    @Test
    void capacityGuardsAndGenerationWrap() throws Exception {
        assertEquals(4, M3LinkedHashMultiset.tableFor(0));
        assertEquals(16, M3LinkedHashMultiset.tableFor(10));
        assertThrows(OutOfMemoryError.class, () -> M3LinkedHashMultiset.tableFor(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE - 8, M3LinkedHashMultiset.grownLanes(Integer.MAX_VALUE - 9, Integer.MAX_VALUE - 8));
        assertEquals(13, M3LinkedHashMultiset.grownLanes(8, 9));
        assertThrows(OutOfMemoryError.class, () -> M3LinkedHashMultiset.grownLanes(8, Integer.MAX_VALUE));
        M3LinkedHashMultiset<Integer> m = M3LinkedHashMultiset.create();
        set(m, "clock", -1);
        m.add(1);
        Iterator<Integer> it = m.iterator();
        it.next();
        it.remove();
        assertTrue(m.isEmpty());
        M3LinkedHashMultiset<Integer> churn = M3LinkedHashMultiset.create();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            int e = i % 97 * 31;
            if (churn.add(e)) { order.remove((Integer) e); order.add(e); }
            if (i % 3 == 0) {
                int gone = (i * 7) % 97 * 31;
                churn.setCount(gone, 0);
                order.remove((Integer) gone);
            }
            if (!churn.contains(e)) { order.remove((Integer) e); }
        }
        churn.verifyInvariants();
        assertEquals(order, new ArrayList<>(churn.elementSet()));
    }

    @Test
    void ownedOrderEdges() {
        both("occurrence remove after element removal", m -> {
            Iterator<Integer> it = m.iterator();
            Integer x = it.next();
            m.setCount(x, 0);
            it.remove();
            return null;
        });
        M3LinkedHashMultiset<Integer> m = M3LinkedHashMultiset.create();
        m.add(1);
        m.add(2);
        m.add(3);
        assertEquals("[1, 2, 3]", m.toString());
        assertThrows(java.util.ConcurrentModificationException.class, () -> m.forEach(x -> {
            if (x == 3) { m.setCount(1, 0); }
        }));
        m.verifyInvariants();
        assertEquals("[2, 3]", m.toString());
        m.add(1);
        assertEquals("[2, 3, 1]", m.toString());
        // Removing the first element moves the last dense offset (1) into offset 0; the chain and an open cursor follow it.
        Iterator<Integer> it = m.elementSet().iterator();
        assertEquals(2, it.next());
        it.remove();
        assertEquals(3, it.next());
        assertEquals(1, it.next());
        assertFalse(it.hasNext());
        m.verifyInvariants();
        assertEquals("[3, 1]", m.toString());
        Multiset.Entry<Integer> e = m.entrySet().iterator().next();
        assertFalse(e.equals(Multisets.immutableEntry(e.getElement(), 99)));
        assertFalse(e.equals(Multisets.immutableEntry(99, e.getCount())));
        assertTrue(e.equals(Multisets.immutableEntry(e.getElement(), e.getCount())));
    }

    @Test
    void invariantCheckerDetectsCorruption() {
        expectBroken("dense lanes", m -> ints(m, "counts")[0] = 0);
        expectBroken("dense lanes", m -> ints(m, "hashes")[0] ^= 1);
        expectBroken("dense lanes", m -> {
            int[] index = ints(m, "index");
            for (int p = 0; p < index.length; p++) {
                if (index[p] == 1) { index[p] = 2; }
                else if (index[p] == 2) { index[p] = 1; }
            }
        });
        expectBroken("index range", m -> {
            int[] index = ints(m, "index");
            for (int p = 0; p < index.length; p++) {
                if (index[p] == 0) {
                    index[p] = 9;
                    return;
                }
            }
        });
        expectBroken("totals", m -> setLong(m, "total", 99));
        expectBroken("totals", m -> {
            int[] index = ints(m, "index");
            for (int p = 0; p < index.length; p++) {
                if (index[p] == 0) {
                    index[p] = 1;
                    return;
                }
            }
        });
        expectBroken("tail", m -> objects(m, "elements")[3] = 5);
        expectBroken("tail", m -> ints(m, "counts")[3] = 1);
        expectBroken("links", m -> set(m, "first", 1));
        expectBroken("links", m -> set(m, "last", 1));
        expectBroken("links", m -> ints(m, "after")[2] = 0);
        expectBroken("links", m -> ints(m, "after")[2] = 7);
        expectBroken("links", m -> ints(m, "before")[1] = 2);
        expectBroken("links", m -> ints(m, "after")[0] = 2);
    }

    @Test
    void steadyStateHotPathsAllocateNothing() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        M3LinkedHashMultiset<Integer> m = M3LinkedHashMultiset.create();
        Integer[] boxes = new Integer[64];
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = i;
            m.add(boxes[i], 3);
        }
        long[] sink = new long[1];
        ObjIntConsumer<Integer> visit = (x, n) -> sink[0] += x + n;
        Integer k = boxes[7];
        Runnable op = () -> {
            m.add(k, 2);
            sink[0] += m.count(k);
            sink[0] += m.contains(k) ? 1 : 0;
            m.remove(k, 2);
            m.setCount(k, 3);
            m.forEachEntry(visit);
            sink[0] += m.size();
        };
        for (int i = 0; i < 20_000; i++) { op.run(); }
        long best = Long.MAX_VALUE;
        long id = Thread.currentThread().threadId();
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 20_000; i++) { op.run(); }
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state allocation in add/count/contains/remove/setCount/forEachEntry/size");
        assertTrue(sink[0] != 0);
        assertSame(m.elementSet(), m.elementSet());
        assertSame(m.entrySet(), m.entrySet());
        assertFalse(m.isEmpty());
    }

    private interface Corruption {
        void apply(M3LinkedHashMultiset<Integer> m) throws Exception;
    }

    private static void expectBroken(String message, Corruption corrupt) {
        M3LinkedHashMultiset<Integer> m = M3LinkedHashMultiset.create();
        m.add(2, 1);
        m.add(4, 2);
        m.add(6, 3);
        m.verifyInvariants();
        try {
            corrupt.apply(m);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        AssertionError e = assertThrows(AssertionError.class, m::verifyInvariants);
        assertEquals(message, e.getMessage());
    }

    private static Field field(String name) {
        try {
            Field f = M3LinkedHashMultiset.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private static void set(Object m, String name, int value) throws IllegalAccessException {
        field(name).setInt(m, value);
    }

    private static void setLong(Object m, String name, long value) throws IllegalAccessException {
        field(name).setLong(m, value);
    }

    private static Object get(Object m, String name) throws IllegalAccessException {
        return field(name).get(m);
    }

    private static int[] ints(Object m, String name) throws IllegalAccessException {
        return (int[]) get(m, name);
    }

    private static Object[] objects(Object m, String name) throws IllegalAccessException {
        return (Object[]) get(m, name);
    }
}
