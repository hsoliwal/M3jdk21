/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Seeded differential run against the real Apache Commons Collections 4.5.0 DualLinkedHashBidiMap (test
 * oracle). Both directions are insertion ordered, so return values, exception classes, traversal orders and
 * the exact text of both sides are compared after every operation; each operation targets the forward map
 * or its inverse at random.
 */
final class M3DualLinkedHashBidiMapDifferentialTest {
    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final int OPS_PER_SEED = 12_000;

    private final BidiMap<Integer, Integer> oracle = new DualLinkedHashBidiMap<>();
    private final M3DualLinkedHashBidiMap<Integer, Integer> m3 = new M3DualLinkedHashBidiMap<>();

    @AfterAll
    static void report() {
        System.out.println("M3DualLinkedHashBidiMap differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {3L, 17L, 42L, 1_013L, 20_261_019L, 0xBEEFL, -9L, 777_777_777L})
    void seededOperationsMatchCommons(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(34);
            long opSeed = random.nextLong();
            same("op" + op + "@" + step, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(step);
        }
    }

    private static Integer element(Random r) {
        return r.nextInt(15) == 0 ? null : r.nextInt(18);
    }

    private static Object apply(BidiMap<Integer, Integer> map, int op, long opSeed) {
        Random r = new Random(opSeed);
        BidiMap<Integer, Integer> b = r.nextInt(3) == 0 ? map.inverseBidiMap() : map;
        Integer k = element(r);
        Integer v = element(r);
        List<Integer> list = new ArrayList<>();
        for (int i = r.nextInt(6); i > 0; i--) { list.add(element(r)); }
        int pick = r.nextInt(18);
        Predicate<Integer> chosen = x -> x != null && (x == pick || x == (pick + 5) % 18);
        switch (op) {
            case 0, 1, 2, 3, 4: return b.put(k, v);
            case 5: return b.remove(k);
            case 6: return b.removeValue(v);
            case 7: return List.of(String.valueOf(b.get(k)), String.valueOf(b.getKey(v)), b.containsKey(k), b.containsValue(v), b.size(), b.isEmpty());
            case 8: {
                Map<Integer, Integer> m = new LinkedHashMap<>();
                for (Integer x : list) { m.put(x, element(r)); }
                b.putAll(m);
                return null;
            }
            case 9: return b.keySet().remove(k);
            case 10: return b.values().remove(v);
            case 11: return b.entrySet().remove(new AbstractMap.SimpleEntry<>(k, v));
            case 12: return b.keySet().removeAll(list);
            case 13: return b.values().removeAll(list);
            case 14: return b.entrySet().removeIf(e -> chosen.test(e.getKey()) || chosen.test(e.getValue()));
            case 15: return b.keySet().retainAll(r.nextInt(8) == 0 ? List.of() : new HashSet<>(Arrays.asList(1, 2, 3, 5, 8, 13, null, 4, 6, 9, 10, 11, 12, 14, 15, 16)));
            case 16: return b.values().retainAll(r.nextInt(8) == 0 ? List.of() : new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 16, 17)));
            case 17: return b.keySet().removeIf(chosen);
            case 18: return b.values().removeIf(chosen);
            case 19: return walkMapIterator(b.mapIterator(), r);
            case 20: return walkEntries(b.entrySet().iterator(), r);
            case 21: return walkRemoving(b.keySet().iterator(), chosen);
            case 22: return walkRemoving(b.values().iterator(), chosen);
            case 23: {
                int bump = r.nextInt(18);
                b.replaceAll((x, y) -> y == null ? bump : (y + bump) % 18);
                return null;
            }
            case 24: return List.of(String.valueOf(b.putIfAbsent(k, v)), String.valueOf(b.getOrDefault(k, -1)), b.remove(v, k));
            case 25: return List.of(String.valueOf(b.replace(k, v)), b.replace(v, k, pick));
            case 26: return String.valueOf(r.nextBoolean() ? b.compute(k, (x, y) -> y == null ? v : null) : b.merge(k, pick, (x, y) -> x == null ? y : null));
            case 27: return forEachTrace(b, pick);
            case 28: if (r.nextInt(30) == 0) { b.clear(); } return null;
            case 29: return List.of(b.hashCode(), b.keySet().hashCode(), b.entrySet().hashCode(), b.equals(new LinkedHashMap<>(b)),
                    b.keySet().equals(new LinkedHashSet<>(b.keySet())), b.entrySet().equals(new LinkedHashSet<>(b.entrySet())), b.values().equals(b.values()));
            case 30: return List.of(Arrays.asList(b.keySet().toArray()), Arrays.asList(b.values().toArray(new Integer[0])), b.entrySet().toString(),
                    b.keySet().containsAll(list), b.values().containsAll(list), b.entrySet().contains(new AbstractMap.SimpleEntry<>(k, v)));
            case 31: return List.of(b.inverseBidiMap().inverseBidiMap() == b, b.values().size(), b.keySet().contains(k), b.values().contains(v));
            case 32: return walkResetting(b.mapIterator(), r);
            case 33: {
                if (r.nextBoolean()) { b.keySet().removeAll(List.of()); return b.values().retainAll(list); }
                return b.entrySet().removeAll(List.of(new AbstractMap.SimpleEntry<>(k, v), "x"));
            }
            default: throw new AssertionError(op);
        }
    }

    /** Map-iterator walk: reads, setValue (possibly refused) and remove, recorded step by step. */
    private static List<Object> walkMapIterator(MapIterator<Integer, Integer> it, Random r) {
        List<Object> trace = new ArrayList<>();
        while (it.hasNext()) {
            trace.add(it.next());
            trace.add(String.valueOf(it.getValue()));
            int action = r.nextInt(6);
            if (action == 0) {
                trace.add(outcome(() -> it.setValue(element(r))));
                trace.add(String.valueOf(it.getValue()));
            } else if (action == 1) {
                it.remove();
                trace.add(outcome(it::getKey));
            } else if (action == 2) {
                trace.add(it.toString());
            }
        }
        trace.add(it.toString());
        return trace;
    }

    private static List<Object> walkEntries(Iterator<Map.Entry<Integer, Integer>> it, Random r) {
        List<Object> trace = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> e = it.next();
            trace.add(e.toString());
            int action = r.nextInt(5);
            if (action == 0) {
                trace.add(outcome(() -> e.setValue(element(r))));
                trace.add(e.toString());
            } else if (action == 1) {
                it.remove();
                trace.add(String.valueOf(e.getValue()));
            } else if (action == 2) {
                trace.add(List.of(e.hashCode(), e.equals(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue()))));
            }
        }
        return trace;
    }

    private static <T> List<Object> walkRemoving(Iterator<T> it, Predicate<T> remove) {
        List<Object> trace = new ArrayList<>();
        while (it.hasNext()) {
            T x = it.next();
            trace.add(String.valueOf(x));
            if (remove.test(x)) { it.remove(); }
        }
        return trace;
    }

    private static List<Object> walkResetting(MapIterator<Integer, Integer> it, Random r) {
        List<Object> trace = new ArrayList<>();
        for (int i = r.nextInt(4); i > 0 && it.hasNext(); i--) { trace.add(it.next()); }
        ((ResettableIterator<?>) it).reset();
        trace.add(it.toString());
        trace.add(outcome(it::getValue));
        trace.add(outcome(() -> {
            it.remove();
            return null;
        }));
        while (it.hasNext()) { trace.add(it.next()); }
        trace.add(outcome(it::next));
        return trace;
    }

    /** forEach whose action may change the map: the concurrent change must surface at the same point. */
    private static List<Object> forEachTrace(BidiMap<Integer, Integer> b, int pick) {
        List<Object> seen = new ArrayList<>();
        Object outcome = outcome(() -> {
            b.forEach((x, y) -> {
                seen.add(x + "=" + y);
                if (x != null && x == pick) { b.remove((pick + 1) % 18); }
                if (y != null && y == pick) { b.put(x, (pick + 7) % 18); }
            });
            return "done";
        });
        seen.add(outcome);
        return seen;
    }

    private static Object outcome(Callable<?> action) {
        try {
            Object o = action.call();
            return o instanceof Set<?> s ? new ArrayList<>(s).toString() : o;
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
        BidiMap<Integer, Integer> oi = oracle.inverseBidiMap();
        BidiMap<Integer, Integer> mi = m3.inverseBidiMap();
        assertEquals(oracle.toString(), m3.toString(), "forward order@" + step);
        assertEquals(oi.toString(), mi.toString(), "inverse order@" + step);
        assertEquals(oracle.size(), m3.size(), "size@" + step);
        assertTrue(m3.equals(oracle) && oracle.equals(m3) && mi.equals(oi) && oi.equals(mi), "equals@" + step);
        assertEquals(oracle.hashCode(), m3.hashCode(), "hashCode@" + step);
        assertEquals(oi.hashCode(), mi.hashCode(), "inverse hashCode@" + step);
        assertEquals(oracle.keySet().toString(), m3.keySet().toString(), "keySet order@" + step);
        assertEquals(oracle.values().toString(), m3.values().toString(), "values order@" + step);
        assertSame(m3, mi.inverseBidiMap(), "inverse of inverse@" + step);
        CHECKS.addAndGet(9);
    }
}
