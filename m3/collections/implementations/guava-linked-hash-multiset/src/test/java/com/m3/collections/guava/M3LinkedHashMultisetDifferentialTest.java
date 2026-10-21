/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Seeded differential run against the real Guava 33.4.8-jre LinkedHashMultiset (test-scope oracle): return
 * values, exception classes and the full resulting state are compared after every operation. Both iterate in
 * insertion order, so traversals, text and iterator-driven removals are compared exactly.
 */
final class M3LinkedHashMultisetDifferentialTest {
    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(M3LinkedHashMultisetDifferentialTest.class.getName());
    private static final int OPS_PER_SEED = 12_000;

    private final Multiset<Integer> oracle = LinkedHashMultiset.create();
    private final M3LinkedHashMultiset<Integer> m3 = M3LinkedHashMultiset.create();

    @AfterAll
    static void report() {
        LOG.info(() -> "M3LinkedHashMultiset differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 7L, 42L, 1_001L, 20_261_019L, 0xBADCAFEL, -3L, 987_654_321L})
    void seededOperationsMatchGuava(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(38);
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
            case 10: return m.addAll(LinkedHashMultiset.create(list));
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
                List<String> seen = new ArrayList<>();
                m.forEachEntry((x, c) -> seen.add(x + ":" + c));
                return seen;
            }
            case 24: {
                List<Integer> seen = new ArrayList<>();
                m.forEach(seen::add);
                return seen;
            }
            case 25: return List.of(m.stream().count(), m.stream().collect(Collectors.toList()), m.stream().skip(1).limit(3).toList());
            case 26: return List.of(Arrays.asList(m.toArray()), Arrays.asList(m.toArray(new Integer[0])), m.toArray(new Integer[m.size() + 1]).length);
            case 27: return m.containsAll(list);
            case 28: return List.of(m.elementSet().contains(e), m.elementSet().size(), m.elementSet().isEmpty(), m.entrySet().size());
            case 29: return List.of(m.entrySet().hashCode(), m.elementSet().hashCode(), m.hashCode(), m.toString(), m.entrySet().toString(), m.elementSet().toString());
            case 30: return m.equals(HashMultiset.create(m)) && HashMultiset.create(m).equals(m) && m.equals(LinkedHashMultiset.create(m));
            case 31: return m.count(pick) > 0 ? m.add(pick, Integer.MAX_VALUE) : m.setCount(pick, 2);
            case 32: return removeThrough(m.iterator(), odd.and(x -> x > 8));
            case 33: return m.remove(e, Integer.MAX_VALUE);
            case 34: return removeSomeThrough(m, r);
            case 35: {
                // Split points are implementation-defined (Guava splits its entry spliterator, the owned one does
                // not), so only the unsplit characteristics, the size estimate and the concatenated walk are compared.
                Spliterator<Integer> s = m.spliterator();
                List<Object> out = new ArrayList<>(List.of(s.characteristics(), s.estimateSize()));
                if (r.nextBoolean()) {
                    s.tryAdvance(out::add);
                    out.add(s.estimateSize());
                }
                Spliterator<Integer> prefix = s.trySplit();
                if (prefix != null) { prefix.forEachRemaining(out::add); }
                s.forEachRemaining(out::add);
                return out;
            }
            case 36: return liveEntries(m, pick);
            case 37: {
                // Remove, then re-add: the element moves to the end in both.
                int c = m.count(pick);
                m.setCount(pick, 0);
                m.add(pick, Math.max(1, c));
                return m.toString();
            }
            default: throw new AssertionError(op);
        }
    }

    /** Removes through an iterator at seeded positions, with reads between removals; order-exact in both. */
    private static Object removeSomeThrough(Multiset<Integer> m, Random r) {
        List<Object> out = new ArrayList<>();
        Iterator<?> it = switch (r.nextInt(3)) {
            case 0 -> m.iterator();
            case 1 -> m.elementSet().iterator();
            default -> m.entrySet().iterator();
        };
        int budget = 1 + r.nextInt(6);
        while (it.hasNext() && budget-- > 0) {
            Object x = it.next();
            out.add(String.valueOf(x));
            if (r.nextInt(3) == 0) {
                it.remove();
                out.add("removed " + m.size());
            }
        }
        return out;
    }

    /** Entries read their counts live. */
    private static Object liveEntries(Multiset<Integer> m, int pick) {
        List<Object> out = new ArrayList<>();
        for (Multiset.Entry<Integer> entry : new ArrayList<>(m.entrySet())) {
            if (entry.getElement() != null && entry.getElement() == pick) { m.add(pick, 2); }
            out.add(entry + "|" + entry.getCount() + "|" + entry.hashCode());
        }
        return out;
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

    static Object outcome(Callable<?> action) {
        try {
            return normalize(action.call());
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getName();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** Collections become ordered lists, so the comparison includes iteration order. */
    static Object normalize(Object o) {
        if (o instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object x : l) { out.add(normalize(x)); }
            return out;
        }
        if (o instanceof Collection<?> c) { return new ArrayList<>(c); }
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
        assertEquals(oracle.toString(), m3.toString(), "toString@" + step);
        assertEquals(new ArrayList<>(oracle), new ArrayList<>(m3), "iteration@" + step);
        assertEquals(new ArrayList<>(oracle.elementSet()), new ArrayList<>(m3.elementSet()), "elementSet@" + step);
        assertEquals(new ArrayList<>(oracle.entrySet()), new ArrayList<>(m3.entrySet()), "entrySet@" + step);
        assertEquals(m3.entrySet(), oracle.entrySet(), "entrySet reverse@" + step);
        CHECKS.addAndGet(9);
    }
}
