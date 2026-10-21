/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.lang.reflect.Field;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Owned-only edge cases: capacity guards, defensive paths and the invariant checker itself. */
final class M3ArrayListMultimapEdgeTest {
    private static final int MAX_ARRAY = Integer.MAX_VALUE - 8;

    @Test
    void capacityGuards() {
        assertEquals(32, M3ArrayListMultimap.doubled(16));
        assertThrows(OutOfMemoryError.class, () -> M3ArrayListMultimap.doubled(1 << 30));
        assertEquals(8, M3ArrayListMultimap.grownSlots(0));
        assertEquals(MAX_ARRAY, M3ArrayListMultimap.grownSlots(MAX_ARRAY - 1));
        assertThrows(OutOfMemoryError.class, () -> M3ArrayListMultimap.grownSlots(MAX_ARRAY));
        M3ArrayListMultimap.checkValueCapacity(MAX_ARRAY);
        assertThrows(OutOfMemoryError.class, () -> M3ArrayListMultimap.checkValueCapacity(MAX_ARRAY + 1L));
        M3ArrayListMultimap<Integer, Integer> big = M3ArrayListMultimap.create();
        for (int i = 0; i < 5_000; i++) { big.put(i % 700, i); }
        for (int i = 0; i < 700; i += 2) { big.removeAll(i); }
        for (int i = 0; i < 2_000; i++) { big.put(1_000 + i % 300, i); }
        big.verifyInvariants();
    }

    @Test
    void stampClockSkipsZeroOnWrap() throws Exception {
        M3ArrayListMultimap<Integer, Integer> m = M3ArrayListMultimap.create();
        set(m, "clock", -1);
        m.put(1, 1);
        Iterator<Integer> it = m.get(1).iterator();
        m.put(1, 2);
        assertThrows(ConcurrentModificationException.class, it::next);
        assertEquals(List.of(1, 2), m.get(1));
        m.verifyInvariants();
    }

    @Test
    void inconsistentArgumentCollectionsAreHandled() {
        M3ArrayListMultimap<Integer, Integer> m = sample();
        Collection<Integer> lying = new AbstractCollection<>() {
            @Override public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override public int size() {
                return 1;
            }
        };
        assertFalse(m.putAll(9, lying));
        assertFalse(m.get(9).addAll(0, lying));
        assertFalse(m.containsKey(9));
        assertFalse(m.get(6).equals(new SizeLiar(3, 2)));
        assertFalse(m.get(6).equals(new SizeLiar(3, 4)));
        assertTrue(m.get(6).equals(new SizeLiar(3, 3)));
        assertFalse(m.asMap().entrySet().contains(Map.entry(6, new SizeLiar(3, 2))));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(1).addAll(-1, List.of(1)));
        M3ArrayListMultimap<Integer, Integer> empty = M3ArrayListMultimap.create();
        assertFalse(empty.putAll(empty));
        m.verifyInvariants();
    }

    @Test
    void entryCursorRefusesStaleKeyRemoval() {
        M3ArrayListMultimap<Integer, Integer> m = sample();
        Iterator<Map.Entry<Integer, Integer>> it = m.entries().iterator();
        Map.Entry<Integer, Integer> e = it.next();
        while (m.get(e.getKey()).size() != 1) { e = it.next(); }
        m.put(42, 42);
        assertThrows(ConcurrentModificationException.class, it::remove);
        assertTrue(m.containsEntry(e.getKey(), e.getValue()));
        m.verifyInvariants();
    }

    @Test
    void keysMultisetBranches() {
        M3ArrayListMultimap<Integer, Integer> m = sample();
        Multiset<Integer> keys = m.keys();
        assertTrue(m.keySet().contains(2));
        assertFalse(m.keySet().contains(99));
        assertFalse(keys.remove(99));
        assertFalse(keys.isEmpty());
        assertEquals("[2, 4 x 2, 6 x 3]", keys.toString());
        assertFalse(keys.equals(HashMultiset.create(List.of(2, 4, 4, 6, 6, 7))));
        assertTrue(keys.equals(HashMultiset.create(List.of(2, 4, 4, 6, 6, 6))));
        assertFalse(keys.entrySet().contains(Multisets.immutableEntry(99, 1)));
        Multiset.Entry<Integer> entry = keys.entrySet().iterator().next();
        assertFalse(entry.equals("x"));
        assertFalse(entry.equals(Multisets.immutableEntry(entry.getElement(), entry.getCount() + 1)));
        assertFalse(entry.equals(Multisets.immutableEntry(99, entry.getCount())));
        assertTrue(entry.equals(Multisets.immutableEntry(entry.getElement(), entry.getCount())));
        assertTrue(keys.removeAll(HashMultiset.create(List.of(2))));
        assertTrue(keys.retainAll(List.of(4)));
        assertEquals(List.of(4), List.copyOf(keys.elementSet()));
        m.removeAll(4);
        assertEquals(0, entry.getCount());
        assertTrue(keys.isEmpty());
        assertTrue(keys.entrySet().isEmpty());
        Multiset.Entry<Integer> stale = sample().keys().entrySet().iterator().next();
        assertEquals(String.valueOf(stale.getElement()), stale.toString());
        M3ArrayListMultimap<Integer, Integer> gone = sample();
        Multiset.Entry<Integer> live = gone.keys().entrySet().iterator().next();
        gone.removeAll(live.getElement());
        assertEquals(0, live.getCount());
    }

    @Test
    void invariantCheckerDetectsEveryCorruption() throws Exception {
        expectBroken("key lanes", m -> set(m, "used", 2));
        expectBroken("key lanes", m -> ints(m, "after")[2] = 0);
        expectBroken("key lanes", m -> ints(m, "before")[1] = 2);
        expectBroken("key lanes", m -> {
            int[] positions = ints(m, "positions");
            positions[1] = positions[1] + 1 & ints(m, "index").length - 1;
        });
        expectBroken("key lanes", m -> objects(m, "keys")[1] = 99);
        expectBroken("key lanes", m -> longs(m, "live")[0] &= ~(1L << 1));
        expectBroken("key lanes", m -> ints(m, "counts")[1] = 0);
        expectBroken("counts", m -> set(m, "keyCount", 4));
        expectBroken("counts", m -> set(m, "last", 1));
        expectBroken("counts", m -> set(m, "size", 99));
        expectBroken("bucket", m -> {
            int[] index = ints(m, "index");
            index[emptyBucket(index)] = 2;
        });
        expectBroken("buckets", m -> {
            m.removeAll(6);
            int[] index = ints(m, "index");
            int q = emptyBucket(index);
            index[q] = 3;
            ints(m, "positions")[2] = q;
        });
        expectBroken("free lanes", m -> {
            m.removeAll(6);
            set(m, "free", 5);
        });
        expectBroken("free lanes", m -> {
            m.removeAll(6);
            set(m, "free", 0);
        });
        expectBroken("free lanes", m -> {
            m.removeAll(6);
            objects(m, "keys")[2] = 7;
        });
        expectBroken("free lanes", m -> {
            m.removeAll(6);
            ints(m, "counts")[2] = 1;
        });
        expectBroken("free lanes", m -> {
            m.removeAll(6);
            longs(m, "live")[0] |= 1L << 2;
        });
        expectBroken("slot ownership", m -> set(m, "used", 4));
    }

    private static int emptyBucket(int[] index) {
        for (int p = 0; p < index.length; p++) {
            if (index[p] == 0) { return p; }
        }
        throw new AssertionError("no empty bucket");
    }

    private static void expectBroken(String message, Consumer<M3ArrayListMultimap<Integer, Integer>> corrupt) {
        M3ArrayListMultimap<Integer, Integer> m = sample();
        m.verifyInvariants();
        corrupt.accept(m);
        AssertionError e = assertThrows(AssertionError.class, m::verifyInvariants);
        assertEquals(message, e.getMessage());
    }

    /** Keys 2, 4, 6 occupy slots 0, 1, 2 with 1, 2 and 3 values. */
    private static M3ArrayListMultimap<Integer, Integer> sample() {
        M3ArrayListMultimap<Integer, Integer> m = M3ArrayListMultimap.create();
        m.put(2, 20);
        m.putAll(4, List.of(40, 41));
        m.putAll(6, List.of(60, 61, 62));
        return m;
    }

    private static Field field(String name) {
        try {
            Field f = M3ArrayListMultimap.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private static void set(Object m, String name, int value) {
        try {
            field(name).setInt(m, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static Object get(Object m, String name) {
        try {
            return field(name).get(m);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static int[] ints(Object m, String name) {
        return (int[]) get(m, name);
    }

    private static long[] longs(Object m, String name) {
        return (long[]) get(m, name);
    }

    private static Object[] objects(Object m, String name) {
        return (Object[]) get(m, name);
    }

    /** A list whose size() and iteration disagree. */
    private static final class SizeLiar extends AbstractList<Integer> {
        private final int reported;
        private final List<Integer> actual = new ArrayList<>();

        SizeLiar(int reported, int actual) {
            this.reported = reported;
            for (int i = 0; i < actual; i++) { this.actual.add(60 + i); }
        }

        @Override public Integer get(int index) {
            return actual.get(index);
        }

        @Override public int size() {
            return reported;
        }

        @Override public Iterator<Integer> iterator() {
            return actual.iterator();
        }
    }
}
