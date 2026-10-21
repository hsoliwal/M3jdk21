/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Seeded differential run against the real Apache Commons Collections 4.5.0 HashBag (test oracle):
 * return values, exception classes and the full resulting state are compared after every operation.
 * Commons iterates in HashMap order, so traversals are compared as count maps and only
 * order-independent removals go through iterators.
 */
final class M3HashBagDifferentialTest {
    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(M3HashBagDifferentialTest.class.getName());
    private static final int OPS_PER_SEED = 12_000;

    private final Bag<Integer> oracle = new HashBag<>();
    private final M3HashBag<Integer> m3 = new M3HashBag<>();

    @AfterAll
    static void report() {
        LOG.info(() -> "M3HashBag differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {2L, 11L, 42L, 1_009L, 20_261_019L, 0xFEEDL, -5L, 555_555_555L})
    void seededOperationsMatchCommons(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(30);
            long opSeed = random.nextLong();
            same("op" + op + "@" + step, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(step);
            if (oracle.size() > 400) { same("trim", () -> oracle.remove(3), () -> m3.remove(3)); }
        }
    }

    private static Object apply(Bag<Integer> b, int op, long opSeed) {
        Random r = new Random(opSeed);
        Integer e = r.nextInt(13) == 0 ? null : r.nextInt(12);
        int n = r.nextInt(8) - 2;
        List<Integer> list = new ArrayList<>();
        for (int i = r.nextInt(6); i > 0; i--) { list.add(r.nextInt(10) == 0 ? null : r.nextInt(12)); }
        int pick = r.nextInt(12);
        Predicate<Integer> chosen = x -> x != null && (x == pick || x == pick + 3);
        switch (op) {
            case 0, 1, 2: return b.add(e);
            case 3, 4: return b.add(e, n);
            case 5: return b.remove(e);
            case 6, 7: return b.remove(e, n);
            case 8: return List.of(b.getCount(e), b.contains(e), b.size(), b.isEmpty());
            case 9: return b.addAll(list);
            case 10: return b.removeAll(list);
            case 11: return b.removeAll(new HashBag<>(list));
            case 12: return b.containsAll(list);
            case 13: return b.containsAll(new HashBag<>(list));
            case 14: return b.retainAll(list);
            case 15: return b.retainAll(new HashBag<>(Arrays.asList(0, 0, 1, 2, 2, 2, 3, 4, 5, 6, 6, 7, 8, 9, 10, 11, null)));
            case 16: return removeThrough(b.iterator(), chosen);
            case 17: return b.removeIf(chosen);
            case 18: if (r.nextInt(40) == 0) { b.clear(); } return null;
            case 19: return List.of(b.uniqueSet().size(), b.uniqueSet().contains(e), b.uniqueSet().isEmpty());
            case 20: return counts(Arrays.asList(b.toArray()));
            case 21: return counts(Arrays.asList(b.toArray(new Integer[0])));
            case 22: {
                Integer[] big = b.toArray(new Integer[b.size() + 3]);
                return List.of(counts(Arrays.asList(big).subList(0, b.size())), big[b.size()] == null, big[b.size() + 2] == null);
            }
            case 23: return List.of(b.hashCode(), b.uniqueSet().hashCode());
            case 24: return b.equals(new HashBag<>(b)) && new HashBag<>(b).equals(b);
            case 25: {
                Map<Object, Integer> seen = new HashMap<>();
                b.forEach(x -> seen.merge(x == null ? "null" : x, 1, Integer::sum));
                return seen;
            }
            case 26: return List.of(b.stream().count(), b.toString().length());
            case 27: return b.retainAll(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)));
            case 28: return b.addAll(new HashBag<>(list));
            case 29: return b.remove(e, Integer.MAX_VALUE);
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

    static Map<Object, Integer> counts(Collection<?> c) {
        Map<Object, Integer> out = new HashMap<>();
        for (Object x : c) { out.merge(x == null ? "null" : x, 1, Integer::sum); }
        return out;
    }

    private static Object outcome(Callable<?> action) {
        try {
            Object o = action.call();
            return o instanceof Collection<?> c ? counts(c) : o;
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getName();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void same(String what, Callable<?> commons, Callable<?> owned) {
        assertEquals(outcome(commons), outcome(owned), what);
        CHECKS.incrementAndGet();
    }

    private void compareState(int step) {
        m3.verifyInvariants();
        assertEquals(oracle.size(), m3.size(), "size@" + step);
        assertTrue(m3.equals(oracle), "m3.equals(commons)@" + step);
        assertTrue(oracle.equals(m3), "commons.equals(m3)@" + step);
        assertEquals(oracle.hashCode(), m3.hashCode(), "hashCode@" + step);
        assertEquals(oracle.uniqueSet(), m3.uniqueSet(), "uniqueSet@" + step);
        assertEquals(m3.uniqueSet(), oracle.uniqueSet(), "uniqueSet reverse@" + step);
        assertEquals(counts(oracle), counts(m3), "iteration@" + step);
        CHECKS.addAndGet(7);
    }
}
