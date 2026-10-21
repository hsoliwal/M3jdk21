/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Seeded differential run: every operation is applied to the real Guava 33.4.8-jre ArrayListMultimap
 * (test-scope oracle) and to M3ArrayListMultimap; returned values, thrown exception classes and the
 * complete resulting state are compared after each step. Guava leaves key order unspecified, so
 * whole-multimap traversals are compared order-insensitively and only order-independent removals are
 * applied through them; per-key value order is compared exactly.
 */
final class M3ArrayListMultimapDifferentialTest {
    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(M3ArrayListMultimapDifferentialTest.class.getName());
    private static final int OPS_PER_SEED = 12_000;
    private static final Comparator<Integer> ORDER = Comparator.nullsFirst(Comparator.naturalOrder());

    private final ListMultimap<Integer, Integer> oracle = ArrayListMultimap.create();
    private final M3ArrayListMultimap<Integer, Integer> m3 = M3ArrayListMultimap.create();
    private Random random;

    @AfterAll
    static void report() {
        LOG.info(() -> "M3ArrayListMultimap differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 7L, 42L, 1_001L, 20_261_018L, 0xC0FFEEL, -9L, 123_456_789L})
    void seededOperationsMatchGuava(long seed) {
        random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(45);
            long opSeed = random.nextLong();
            same("op" + op + "@" + step, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(step);
            if (oracle.size() > 240) {
                Integer k = key();
                same("trim", () -> oracle.removeAll(k), () -> m3.removeAll(k));
            }
        }
    }

    // ------------------------------------------------------------------ operation table

    private Integer key() {
        int k = random.nextInt(14) - 1;
        return k < 0 ? null : k;
    }

    private Integer value() {
        int v = random.nextInt(10) - 1;
        return v < 0 ? null : v;
    }

    private List<Integer> values(int max) {
        int n = random.nextInt(max + 1);
        List<Integer> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) { out.add(value()); }
        return out;
    }

    private Object apply(ListMultimap<Integer, Integer> m, int op, long opSeed) {
        Random r = new Random(opSeed);
        Integer k = r.nextInt(14) - 1 < 0 ? null : r.nextInt(13);
        Integer v = r.nextInt(10) == 0 ? null : r.nextInt(9);
        int a = r.nextInt(8) - 1;
        int b = r.nextInt(8) - 1;
        List<Integer> list = new ArrayList<>();
        for (int i = r.nextInt(4); i > 0; i--) { list.add(r.nextInt(9)); }
        Predicate<Integer> odd = x -> x != null && (x & 1) == 1;
        Function<Integer, Integer> bump = x -> x == null ? 0 : x + 1;
        switch (op) {
            case 0, 1, 2: return m.put(k, v);
            case 3: return m.remove(k, v);
            case 4: return m.removeAll(k);
            case 5: return m.replaceValues(k, list);
            case 6: return m.putAll(k, list);
            case 7: return m.putAll(k, (Iterable<Integer>) list::iterator);
            case 8: return m.get(k).add(v);
            case 9: m.get(k).add(a, v); return null;
            case 10: return m.get(k).set(a, v);
            case 11: return m.get(k).remove(a);
            case 12: return m.get(k).remove(v);
            case 13: m.get(k).clear(); return null;
            case 14: m.get(k).subList(Math.min(a, b), Math.max(a, b)).clear(); return null;
            case 15: {
                List<Integer> sub = m.get(k).subList(Math.min(a, b), Math.max(a, b));
                sub.add(v);
                Object set = sub.set(0, bump.apply(v));
                return List.of(sub.size(), String.valueOf(set), sub.indexOf(v), sub.contains(v));
            }
            case 16: return m.get(k).removeIf(odd);
            case 17: m.get(k).replaceAll(bump::apply); return null;
            case 18: m.get(k).sort(ORDER); return null;
            case 19: return walkListIterator(m.get(k), a, v);
            case 20: return List.of(m.get(k).indexOf(v), m.get(k).lastIndexOf(v), m.get(k).contains(v), m.get(k).size());
            case 21: return m.keySet().remove(k);
            case 22: return m.keys().remove(k, a);
            case 23: return m.keys().setCount(k, a);
            case 24: return List.of(m.keys().count(k), m.keys().contains(k), m.keys().size(), m.keys().elementSet().size());
            case 25: return removeUniqueValue(m, v);
            case 26: return m.values().removeIf(odd);
            case 27: return m.entries().remove(new AbstractMap.SimpleImmutableEntry<>(k, v));
            case 28: return m.entries().contains(new AbstractMap.SimpleImmutableEntry<>(k, v));
            case 29: return m.asMap().remove(k);
            case 30: return List.of(String.valueOf(m.asMap().get(k)), m.asMap().containsKey(k), m.asMap().size());
            case 31: return m.asMap().entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(k, list));
            case 32: return m.keys().entrySet().remove(Multisets.immutableEntry(k, Math.max(0, a)));
            case 33: return m.keySet().removeIf(x -> x != null && x % 3 == 0) | m.keySet().retainAll(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)));
            case 34: return m.entries().removeIf(e -> odd.test(e.getValue()) && e.getKey() != null && e.getKey() > 6);
            case 35: return List.of(m.containsKey(k), m.containsValue(v), m.containsEntry(k, v), m.isEmpty());
            case 36: {
                ListMultimap<Integer, Integer> other = ArrayListMultimap.create();
                for (Integer x : list) { other.put(x, x); }
                return m.putAll(other);
            }
            case 37: if (r.nextInt(40) == 0) { m.clear(); } return null;
            case 38: return List.of(m.get(k).addAll(Math.max(0, a), list), m.get(k).removeAll(Arrays.asList(1, 2)), m.get(k).retainAll(Arrays.asList(0, 1, 3, 5, 7, 8)));
            case 39: return m.keys().removeAll(Arrays.asList(2, 9)) | m.keys().retainAll(HashMultiset.create(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
            case 40: return walkBackwards(m.get(k));
            case 41: return removeThrough(m.entries().iterator(), e -> odd.test(e.getValue()) && e.getKey() != null && e.getKey() < 3);
            case 42: return removeThrough(m.values().iterator(), x -> x != null && x == 8);
            case 43: return removeThrough(m.keys().iterator(), x -> x != null && x == 11);
            case 44: return removeThrough(m.asMap().entrySet().iterator(), e -> e.getKey() != null && e.getKey() == 12)
                    | removeThrough(m.keySet().iterator(), x -> x != null && x == 10)
                    | removeThrough(m.keys().entrySet().iterator(), e -> e.getCount() > 7);
            default: throw new AssertionError(op);
        }
    }

    private static Object walkListIterator(List<Integer> list, int start, Integer v) {
        List<Object> trace = new ArrayList<>();
        ListIterator<Integer> it = list.listIterator(Math.max(0, Math.min(start, list.size())));
        int steps = 0;
        while (it.hasNext() && steps++ < 6) {
            Integer x = it.next();
            trace.add(it.nextIndex());
            if (x == null) { it.remove(); trace.add("removed"); } else if (x % 3 == 0) { it.set(x + 100); } else if (x % 4 == 1) { it.add(v); }
        }
        trace.add(it.previousIndex());
        trace.add(it.hasPrevious());
        return trace;
    }

    private static Object walkBackwards(List<Integer> list) {
        List<Object> trace = new ArrayList<>();
        ListIterator<Integer> it = list.listIterator(list.size());
        while (it.hasPrevious()) {
            trace.add(it.previousIndex());
            Integer x = it.previous();
            trace.add(x);
            if (x != null && x == 7) { it.remove(); }
        }
        return trace;
    }

    private static <T> boolean removeThrough(Iterator<T> it, Predicate<T> remove) {
        boolean changed = false;
        while (it.hasNext()) {
            if (remove.test(it.next())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    private static Object removeUniqueValue(ListMultimap<Integer, Integer> m, Integer v) {
        long holders = m.asMap().values().stream().filter(c -> c.contains(v)).count();
        return holders <= 1 ? m.values().remove(v) : m.values().contains(v);
    }

    // ------------------------------------------------------------------ comparison

    private static Object outcome(Callable<?> action) {
        try {
            return normalize(action.call());
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getName();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static Object normalize(Object o) {
        if (o instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object x : l) { out.add(normalize(x)); }
            return out;
        }
        if (o instanceof Multiset<?> m) { return HashMultiset.create(m); }
        if (o instanceof Collection<?> c) { return HashMultiset.create(c); }
        return o;
    }

    private static void same(String what, Callable<?> guava, Callable<?> owned) {
        Object expected = outcome(guava);
        Object actual = outcome(owned);
        assertEquals(expected, actual, what);
        CHECKS.incrementAndGet();
    }

    private void compareState(int step) {
        m3.verifyInvariants();
        assertEquals(oracle.size(), m3.size(), "size@" + step);
        assertEquals(oracle.keySet().size(), m3.keySet().size(), "keys@" + step);
        assertTrue(m3.equals(oracle), "m3.equals(guava)@" + step);
        assertTrue(oracle.equals(m3), "guava.equals(m3)@" + step);
        assertEquals(oracle.hashCode(), m3.hashCode(), "hashCode@" + step);
        assertEquals(oracle.keySet(), m3.keySet(), "keySet@" + step);
        assertEquals(HashMultiset.create(oracle.values()), HashMultiset.create(m3.values()), "values@" + step);
        assertEquals(oracle.keys(), m3.keys(), "keys()@" + step);
        assertEquals(HashMultiset.create(oracle.entries()), HashMultiset.create(m3.entries()), "entries@" + step);
        CHECKS.addAndGet(9);
        if (step % 16 == 0) {
            for (Map.Entry<Integer, Collection<Integer>> e : oracle.asMap().entrySet()) {
                assertEquals(e.getValue(), m3.get(e.getKey()), "list@" + step);
                assertEquals(e.getValue().hashCode(), m3.get(e.getKey()).hashCode(), "listHash@" + step);
                CHECKS.addAndGet(2);
            }
            assertEquals(oracle.asMap(), m3.asMap(), "asMap@" + step);
            assertEquals(m3.asMap(), oracle.asMap(), "asMap reverse@" + step);
            assertEquals(oracle.keys().hashCode(), m3.keys().hashCode(), "keys hash@" + step);
            CHECKS.addAndGet(3);
        }
    }

    static Multimap<Integer, Integer> sample() {
        ListMultimap<Integer, Integer> m = ArrayListMultimap.create();
        m.putAll(1, List.of(1, 2, 3));
        m.putAll(2, List.of(4));
        m.putAll(3, List.of(5, 6));
        return m;
    }
}
