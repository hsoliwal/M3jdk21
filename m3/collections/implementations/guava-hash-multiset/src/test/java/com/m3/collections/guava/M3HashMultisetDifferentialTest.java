/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Seeded differential run against the real Guava 33.4.8-jre HashMultiset (test-scope oracle): return
 * values, exception classes and the full resulting state are compared after every operation. Guava's
 * iteration order is the unspecified HashMap order, so traversals are compared order-insensitively and
 * only order-independent removals are applied through iterators.
 */
final class M3HashMultisetDifferentialTest {
    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(M3HashMultisetDifferentialTest.class.getName());
    private static final int OPS_PER_SEED = 12_000;

    private final Multiset<Integer> oracle = HashMultiset.create();
    private final M3HashMultiset<Integer> m3 = M3HashMultiset.create();

    @AfterAll
    static void report() {
        LOG.info(() -> "M3HashMultiset differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 7L, 42L, 1_001L, 20_261_019L, 0xBADCAFEL, -3L, 987_654_321L})
    void seededOperationsMatchGuava(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(34);
            long opSeed = random.nextLong();
            same("op" + op + "@" + step, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(step);
            if (oracle.size() > 400) { same("trim", () -> oracle.setCount(3, 0), () -> m3.setCount(3, 0)); }
        }
    }

    private static Object apply(Multiset<Integer> m, int op, long opSeed) {
        Random r = new Random(opSeed);
        Integer e = r.nextInt(13) == 0 ? null : r.nextInt(12);
        int n = r.nextInt(8) - 1;
        int n2 = r.nextInt(8) - 1;
        List<Integer> list = new ArrayList<>();
        for (int i = r.nextInt(5); i > 0; i--) { list.add(r.nextInt(10) == 0 ? null : r.nextInt(12)); }
        Predicate<Integer> odd = x -> x != null && (x & 1) == 1;
        int pick = r.nextInt(12);
        switch (op) {
            case 0, 1, 2: return m.add(e);
            case 3: return m.add(e, n);
            case 4: return m.remove(e);
            case 5: return m.remove(e, n);
            case 6: return m.setCount(e, n);
            case 7: return m.setCount(e, n, n2);
            case 8: return List.of(m.count(e), m.contains(e), m.size(), m.isEmpty());
            case 9: return m.addAll(list);
            case 10: return m.addAll(HashMultiset.create(list));
            case 11: return m.removeAll(list) | m.removeAll(HashMultiset.create(Arrays.asList(pick, pick + 1)));
            case 12: return m.retainAll(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, null)));
            case 13: return m.elementSet().remove(e);
            case 14: return m.elementSet().removeAll(new HashSet<>(list.size() > 2 ? Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, null) : list));
            case 15: return m.elementSet().retainAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, null));
            case 16: return m.entrySet().remove(Multisets.immutableEntry(e, Math.max(0, n)));
            case 17: return m.entrySet().contains(Multisets.immutableEntry(e, Math.max(0, n)));
            case 18: return removeThrough(m.iterator(), x -> x != null && x == pick);
            case 19: return removeThrough(m.elementSet().iterator(), x -> x != null && x == pick + 1);
            case 20: return removeThrough(m.entrySet().iterator(), x -> x.getCount() > 4);
            case 21: return m.removeIf(odd.and(x -> x == pick || x == pick + 2));
            case 22: if (r.nextInt(30) == 0) { m.clear(); } return null;
            case 23: {
                Map<Integer, Integer> seen = new HashMap<>();
                m.forEachEntry(seen::put);
                return seen;
            }
            case 24: {
                List<Integer> seen = new ArrayList<>();
                m.forEach(seen::add);
                return HashMultiset.create(seen);
            }
            case 25: return List.of(m.stream().count(), HashMultiset.create(m.stream().collect(Collectors.toList())));
            case 26: return List.of(HashMultiset.create(Arrays.asList(m.toArray())), HashMultiset.create(Arrays.asList(m.toArray(new Integer[0]))));
            case 27: return m.containsAll(list);
            case 28: return List.of(m.elementSet().contains(e), m.elementSet().size(), m.elementSet().isEmpty(), m.entrySet().size());
            case 29: return List.of(m.entrySet().hashCode(), m.elementSet().hashCode(), m.hashCode());
            case 30: return m.equals(HashMultiset.create(m)) && HashMultiset.create(m).equals(m);
            case 31: return m.count(pick) > 0 ? m.add(pick, Integer.MAX_VALUE) : m.setCount(pick, 2);
            case 32: return removeThrough(m.iterator(), odd.and(x -> x > 8));
            case 33: return m.remove(e, Integer.MAX_VALUE);
            default: throw new AssertionError(op);
        }
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
        assertEquals(outcome(guava), outcome(owned), what);
        CHECKS.incrementAndGet();
    }

    private void compareState(int step) {
        m3.verifyInvariants();
        assertEquals(oracle.size(), m3.size(), "size@" + step);
        assertTrue(m3.equals(oracle), "m3.equals(guava)@" + step);
        assertTrue(oracle.equals(m3), "guava.equals(m3)@" + step);
        assertEquals(oracle.hashCode(), m3.hashCode(), "hashCode@" + step);
        assertEquals(oracle.elementSet(), m3.elementSet(), "elementSet@" + step);
        assertEquals(oracle.entrySet(), m3.entrySet(), "entrySet@" + step);
        assertEquals(m3.entrySet(), oracle.entrySet(), "entrySet reverse@" + step);
        assertEquals(oracle, HashMultiset.create(m3), "iteration@" + step);
        CHECKS.addAndGet(8);
        if (step % 32 == 0) {
            assertEquals(oracle.toString().length(), m3.toString().length(), "toString length@" + step);
            CHECKS.incrementAndGet();
        }
    }
}
