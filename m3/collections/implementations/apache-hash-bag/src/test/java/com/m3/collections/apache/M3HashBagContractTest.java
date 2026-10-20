/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.junit.jupiter.api.Test;

/** Focused contract cases against the Commons provider, plus owned-only edge and invariant cases. */
final class M3HashBagContractTest {
    @FunctionalInterface
    interface Scenario {
        Object run(Bag<Integer> b) throws Exception;
    }

    private static Bag<Integer> commons() {
        Bag<Integer> b = new HashBag<>();
        b.add(1, 3);
        b.add(2);
        b.add(3, 2);
        return b;
    }

    private static Object outcome(Scenario s, Bag<Integer> b) {
        try {
            Object o = s.run(b);
            return o instanceof Collection<?> c && !(o instanceof List<?>) ? M3HashBagDifferentialTest.counts(c) : o;
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void both(String name, Scenario s) {
        Bag<Integer> c = commons();
        M3HashBag<Integer> o = new M3HashBag<>(commons());
        assertEquals(outcome(s, c), outcome(s, o), name);
        assertEquals(c, o, name + " state");
        o.verifyInvariants();
        M3HashBagDifferentialTest.CHECKS.addAndGet(2);
    }

    @Test
    void constructorsAndArguments() {
        assertEquals(new HashBag<>(List.of(1, 1, 2)), new M3HashBag<>(List.of(1, 1, 2)));
        Iterable<Integer> iterable = List.of(4, 4, 5)::iterator;
        assertEquals(new HashBag<>(iterable), new M3HashBag<>(iterable));
        assertEquals(commons(), new M3HashBag<>(new M3HashBag<>(commons())));
        assertThrows(NullPointerException.class, () -> new M3HashBag<>((Collection<Integer>) null));
        assertThrows(NullPointerException.class, () -> new M3HashBag<>((Iterable<Integer>) null));
        both("add return values", b -> List.of(b.add(1), b.add(9), b.add(9, 2), b.add(8, 0), b.add(7, -1), b.getCount(9)));
        both("remove return values", b -> List.of(b.remove(1, 1), b.remove(1, 0), b.remove(1, -2), b.remove(9, 1), b.remove(3, 5), b.remove(2), b.remove(2)));
        both("addAll", b -> List.of(b.addAll(List.of(1, 9)), b.addAll(List.of(1)), b.addAll(List.of())));
        both("addAll null", b -> b.addAll(null));
        both("removeAll null", b -> b.removeAll(null));
        both("removeAll copies", b -> List.of(b.removeAll(List.of(1, 1, 9)), b.getCount(1)));
        both("retainAll null", b -> b.retainAll(null));
        both("retainAll counts", b -> List.of(b.retainAll(new HashBag<>(List.of(1, 1, 3, 3, 3))), b.getCount(1), b.getCount(3), b.getCount(2)));
        both("retainAll unchanged", b -> b.retainAll(new HashBag<>(List.of(1, 1, 1, 2, 3, 3))));
        both("containsAll null", b -> b.containsAll(null));
        both("containsAll counts", b -> List.of(b.containsAll(List.of(1, 1, 1)), b.containsAll(List.of(1, 1, 1, 1)), b.containsAll(new HashBag<>(List.of(3, 3)))));
        both("nulls", b -> List.of(b.add(null), b.add(null, 2), b.getCount(null), b.contains(null), b.remove(null, 1), b.remove(null)));
        both("addAll self", b -> b.addAll(b));
    }

    @Test
    void viewsTextAndArrays() {
        both("uniqueSet add", b -> b.uniqueSet().add(5));
        both("uniqueSet addAll", b -> b.uniqueSet().addAll(List.of(5)));
        both("uniqueSet remove", b -> b.uniqueSet().remove(1));
        both("uniqueSet removeAll", b -> b.uniqueSet().removeAll(List.of(1)));
        both("uniqueSet retainAll", b -> b.uniqueSet().retainAll(List.of(1)));
        both("uniqueSet removeIf", b -> b.uniqueSet().removeIf(x -> true));
        both("uniqueSet clear", b -> {
            b.uniqueSet().clear();
            return null;
        });
        both("uniqueSet iterator remove", b -> {
            Iterator<Integer> it = b.uniqueSet().iterator();
            it.next();
            it.remove();
            return null;
        });
        both("uniqueSet reads", b -> List.of(b.uniqueSet().size(), b.uniqueSet().contains(3), b.uniqueSet().contains(9),
                b.uniqueSet().equals(java.util.Set.of(1, 2, 3)), b.uniqueSet().hashCode(), b.uniqueSet().toString().length()));
        both("uniqueSet iterator exhausted", b -> {
            Iterator<Integer> it = b.uniqueSet().iterator();
            while (it.hasNext()) { it.next(); }
            return it.next();
        });
        both("uniqueSet structural", b -> {
            Iterator<Integer> it = b.uniqueSet().iterator();
            it.next();
            b.add(9);
            return it.next();
        });
        both("uniqueSet count change", b -> {
            Iterator<Integer> it = b.uniqueSet().iterator();
            it.next();
            b.add(1);
            return it.hasNext() ? "more" : "end";
        });
        both("text", b -> {
            b.clear();
            String empty = b.toString();
            b.add(4, 2);
            return List.of(empty, b.toString());
        });
        both("equality", b -> List.of(b.equals(b), b.equals("x"), b.equals(List.of(1, 1, 1, 2, 3, 3)), b.equals(new HashBag<>(List.of(1, 2, 3))),
                b.equals(new HashBag<>(List.of(1, 1, 1, 2, 3, 4))), b.hashCode()));
        both("toArray tail", b -> {
            Integer[] arr = new Integer[10];
            Arrays.fill(arr, 99);
            Integer[] out = b.toArray(arr);
            return List.of(out == arr, Arrays.asList(out).subList(6, 10), M3HashBagDifferentialTest.counts(Arrays.asList(out).subList(0, 6)));
        });
        both("toArray grow", b -> List.of(b.toArray(new Integer[1]).length, b.toArray(new Number[0]).getClass().getComponentType().getSimpleName()));
        both("toArray store", b -> b.toArray(new String[0]));
        both("streams", b -> List.of(b.stream().count(), b.spliterator().estimateSize(), M3HashBagDifferentialTest.counts(b.parallelStream().toList())));
    }

    @Test
    void iteratorsFailFast() {
        both("copies", b -> {
            List<Integer> seen = new ArrayList<>();
            for (Integer x : b) { seen.add(x); }
            return M3HashBagDifferentialTest.counts(seen);
        });
        both("remove twice", b -> {
            Iterator<Integer> it = b.iterator();
            it.next();
            it.remove();
            it.remove();
            return null;
        });
        both("remove before next", b -> {
            b.iterator().remove();
            return null;
        });
        both("exhausted", b -> {
            Iterator<Integer> it = b.iterator();
            while (it.hasNext()) { it.next(); }
            return it.next();
        });
        both("count change", b -> {
            Iterator<Integer> it = b.iterator();
            it.next();
            b.add(1);
            return it.next();
        });
        both("count change remove", b -> {
            Iterator<Integer> it = b.iterator();
            it.next();
            b.add(1);
            it.remove();
            return null;
        });
        both("noop add still counts", b -> {
            Iterator<Integer> it = b.iterator();
            it.next();
            b.add(5, 0);
            return it.next();
        });
        both("noop remove does not count", b -> {
            Iterator<Integer> it = b.iterator();
            it.next();
            b.remove(5, 1);
            b.remove(1, 0);
            return it.hasNext() ? it.next() >= 0 : "end";
        });
        both("forEach", b -> {
            List<Integer> seen = new ArrayList<>();
            b.forEach(seen::add);
            return M3HashBagDifferentialTest.counts(seen);
        });
        both("removeIf", b -> List.of(b.removeIf(x -> x == 1), b.removeIf(x -> x > 50)));
    }

    @Test
    void twoIteratorsFollowTheBackingMapRule() {
        M3HashBag<Integer> o = new M3HashBag<>();
        o.add(1, 2);
        o.add(2, 1);
        o.add(3, 1);
        Iterator<Integer> a = o.iterator();
        Iterator<Integer> b = o.iterator();
        Integer first = a.next();
        Integer bFirst = b.next();
        assertEquals(first, bFirst);
        while (a.hasNext()) {
            if (a.next() == 2) { a.remove(); }
        }
        assertEquals(0, o.getCount(2));
        b.remove();
        assertEquals(1, o.getCount(1));
        assertThrows(java.util.ConcurrentModificationException.class, () -> {
            while (b.hasNext()) { b.next(); }
        });
        Iterator<Integer> c = o.iterator();
        Iterator<Integer> d = o.iterator();
        Integer x = c.next();
        d.next();
        while (d.hasNext()) { d.next(); }
        for (Iterator<Integer> e = o.iterator(); e.hasNext();) {
            if (e.next().equals(x)) { e.remove(); }
        }
        assertThrows(java.util.ConcurrentModificationException.class, c::remove);
        o.verifyInvariants();
        M3HashBag<Integer> p = new M3HashBag<>(List.of(5, 6, 7));
        Iterator<Integer> f = p.iterator();
        Integer single = f.next();
        for (Iterator<Integer> g = p.iterator(); g.hasNext();) {
            if (!g.next().equals(single)) {
                g.remove();
                break;
            }
        }
        assertThrows(java.util.ConcurrentModificationException.class, f::remove);
        assertEquals(1, p.getCount(single));
        p.verifyInvariants();
    }

    @Test
    void serializationRoundTrip() throws Exception {
        M3HashBag<Integer> o = new M3HashBag<>(commons());
        o.add(null, 2);
        Object back = read(write(o));
        assertEquals(o, back);
        @SuppressWarnings("unchecked") M3HashBag<Integer> copy = (M3HashBag<Integer>) back;
        copy.verifyInvariants();
        Iterator<Integer> it = copy.iterator();
        it.next();
        assertEquals(4, copy.getCount(1) + 1);
        assertThrows(InvalidObjectException.class, () -> read(corrupt(-1, 1)));
        assertThrows(InvalidObjectException.class, () -> read(corrupt(1, 0)));
        assertEquals(new HashBag<>(List.of(7)), read(corrupt(1, 1)));
    }

    private static byte[] write(Object o) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(o); }
        return bytes.toByteArray();
    }

    private static Object read(byte[] data) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) { return in.readObject(); }
    }

    private static byte[] corrupt(int elements, int count) throws Exception {
        M3HashBag<Integer> one = new M3HashBag<>();
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
    void capacityGuardsAndChurn() {
        assertEquals(4, M3HashBag.tableFor(0));
        assertEquals(16, M3HashBag.tableFor(10));
        assertThrows(OutOfMemoryError.class, () -> M3HashBag.tableFor(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE - 8, M3HashBag.grownLanes(Integer.MAX_VALUE - 9, Integer.MAX_VALUE - 8));
        assertThrows(OutOfMemoryError.class, () -> M3HashBag.grownLanes(8, Integer.MAX_VALUE));
        M3HashBag<Integer> churn = new M3HashBag<>();
        for (int i = 0; i < 5_000; i++) {
            churn.add(i % 97 * 31);
            if (i % 3 == 0) { churn.remove((i * 7) % 97 * 31); }
        }
        churn.verifyInvariants();
    }

    @Test
    void invariantCheckerDetectsCorruption() {
        expectBroken("dense lanes", b -> ints(b, "counts")[0] = 0);
        expectBroken("dense lanes", b -> ints(b, "hashes")[0] ^= 1);
        expectBroken("dense lanes", b -> {
            int[] index = ints(b, "index");
            for (int p = 0; p < index.length; p++) {
                if (index[p] == 1) { index[p] = 2; } else if (index[p] == 2) { index[p] = 1; }
            }
        });
        expectBroken("index range", b -> putIntoEmptyBucket(b, 9));
        expectBroken("totals", b -> setInt(b, "size", 99));
        expectBroken("totals", b -> putIntoEmptyBucket(b, 1));
        expectBroken("tail", b -> ((Object[]) get(b, "elements"))[3] = 5);
        expectBroken("tail", b -> ints(b, "counts")[3] = 1);
    }

    @Test
    void steadyStateHotPathsAllocateNothing() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        M3HashBag<Integer> bag = new M3HashBag<>();
        Integer[] boxes = new Integer[64];
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = i;
            bag.add(boxes[i], 3);
        }
        long[] sink = new long[1];
        Integer k = boxes[7];
        Runnable op = () -> {
            bag.add(k, 2);
            sink[0] += bag.getCount(k);
            sink[0] += bag.contains(k) ? 1 : 0;
            bag.remove(k, 2);
            sink[0] += bag.size() + bag.hashCode();
        };
        for (int i = 0; i < 20_000; i++) { op.run(); }
        long best = Long.MAX_VALUE;
        long id = Thread.currentThread().threadId();
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 20_000; i++) { op.run(); }
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state allocation in add/getCount/contains/remove/size/hashCode");
        assertTrue(sink[0] != 0);
        assertSame(bag.uniqueSet(), bag.uniqueSet());
        assertFalse(bag.isEmpty());
    }

    private interface Corruption {
        void apply(M3HashBag<Integer> b);
    }

    private static void expectBroken(String message, Corruption corrupt) {
        M3HashBag<Integer> b = new M3HashBag<>();
        b.add(2, 1);
        b.add(4, 2);
        b.add(6, 3);
        b.verifyInvariants();
        corrupt.apply(b);
        AssertionError e = assertThrows(AssertionError.class, b::verifyInvariants);
        assertEquals(message, e.getMessage());
    }

    private static void putIntoEmptyBucket(Object b, int value) {
        int[] index = ints(b, "index");
        for (int p = 0; p < index.length; p++) {
            if (index[p] == 0) {
                index[p] = value;
                return;
            }
        }
    }

    private static Field field(String name) {
        try {
            Field f = M3HashBag.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private static void setInt(Object b, String name, int value) {
        try {
            field(name).setInt(b, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static Object get(Object b, String name) {
        try {
            return field(name).get(b);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static int[] ints(Object b, String name) {
        return (int[]) get(b, name);
    }
}
