/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

/**
 * Seeded differential run against the real Spring Framework core 6.2.0 MultiValueMapAdapter (test oracle).
 * Each side adapts its own LinkedHashMap target, so return values, exception classes, the exact target
 * text and the class of every value list are compared after every operation, including the live
 * first-value view and its failures on null or empty lists.
 */
final class M3MultiValueMapAdapterDifferentialTest {
    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(M3MultiValueMapAdapterDifferentialTest.class.getName());
    private static final int OPS_PER_SEED = 12_000;

    private final Map<String, List<Integer>> oracleTarget = new LinkedHashMap<>();
    private final Map<String, List<Integer>> m3Target = new LinkedHashMap<>();
    private final MultiValueMap<String, Integer> oracle = new MultiValueMapAdapter<>(oracleTarget);
    private final MultiValueMap<String, Integer> m3 = new M3MultiValueMapAdapter<>(m3Target);

    @AfterAll
    static void report() {
        LOG.info(() -> "M3MultiValueMapAdapter differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {5L, 19L, 42L, 1_021L, 20_261_019L, 0xCAFEL, -13L, 888_888_888L})
    void seededOperationsMatchSpring(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(40);
            long opSeed = random.nextLong();
            same("op" + op + "@" + step, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(step);
        }
    }

    private static String key(Random r) {
        return r.nextInt(14) == 0 ? null : "k" + r.nextInt(9);
    }

    private static Integer value(Random r) {
        return r.nextInt(10) == 0 ? null : r.nextInt(20);
    }

    /** A fresh caller list per side: a mutable ArrayList, an empty list, null, or an immutable singleton. */
    private static List<Integer> list(Random r) {
        int shape = r.nextInt(10);
        if (shape == 0) { return null; }
        if (shape == 1) { return new ArrayList<>(); }
        if (shape == 2) { return Collections.singletonList(value(r)); }
        List<Integer> l = new ArrayList<>();
        for (int i = r.nextInt(4) + 1; i > 0; i--) { l.add(value(r)); }
        return l;
    }

    private static Object apply(MultiValueMap<String, Integer> m, int op, long opSeed) {
        Random r = new Random(opSeed);
        String k = key(r);
        Integer v = value(r);
        List<Integer> l = list(r);
        Map<String, Integer> single = m.asSingleValueMap();
        switch (op) {
            case 0, 1, 2, 3: m.add(k, v); return null;
            case 4: m.addAll(k, l); return null;
            case 5: {
                LinkedMultiValueMap<String, Integer> other = new LinkedMultiValueMap<>();
                for (int i = r.nextInt(4); i > 0; i--) { other.add(key(r), value(r)); }
                m.addAll(other);
                return null;
            }
            case 6: m.addIfAbsent(k, v); return null;
            case 7: m.set(k, v); return null;
            case 8: {
                Map<String, Integer> values = new LinkedHashMap<>();
                for (int i = r.nextInt(4); i > 0; i--) { values.put(key(r), value(r)); }
                m.setAll(values);
                return null;
            }
            case 9: return Arrays.asList(m.getFirst(k), m.get(k), m.containsKey(k), m.size(), m.isEmpty(), m.containsValue(l));
            case 10: return m.put(k, l);
            case 11: return m.putIfAbsent(k, l);
            case 12: return m.remove(k);
            case 13: return List.of(m.remove(k, l), m.replace(k, l, list(r)));
            case 14: return m.replace(k, l);
            case 15: {
                Map<String, List<Integer>> all = new LinkedHashMap<>();
                for (int i = r.nextInt(3); i > 0; i--) { all.put(key(r), list(r)); }
                m.putAll(all);
                return null;
            }
            case 16: if (r.nextInt(25) == 0) { m.clear(); } return null;
            case 17: return m.computeIfAbsent(k, x -> l);
            case 18: return m.computeIfPresent(k, (x, y) -> r.nextBoolean() ? null : l);
            case 19: return m.compute(k, (x, y) -> y == null ? l : null);
            case 20: return l == null ? "skip" : m.merge(k, l, (a, b) -> a.size() > b.size() ? a : null);
            case 21: {
                int pick = r.nextInt(9);
                m.replaceAll((x, y) -> ("k" + pick).equals(x) ? new ArrayList<>(List.of(pick)) : y);
                return null;
            }
            case 22: return m.getOrDefault(k, l);
            case 23: return List.of(m.toSingleValueMap().toString(), m.toSingleValueMap().getClass().getName());
            case 24: return List.of(m.keySet().remove(k), m.values().remove(l), m.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty()));
            case 25: {
                StringBuilder s = new StringBuilder();
                m.forEach((x, y) -> s.append(x).append(y));
                return s.toString();
            }
            case 26: return List.of(m.hashCode(), m.equals(new LinkedHashMap<>(m)), m.toString(), m.equals(m));
            case 27: return Arrays.asList(single.get(k), single.containsKey(k), single.size(), single.isEmpty(), single.getOrDefault(k, -1));
            case 28: return single.put(k, v);
            case 29: return single.remove(k);
            case 30: return outcome(() -> single.containsValue(v));
            case 31: return outcome(() -> new ArrayList<>(single.values()));
            case 32: return outcome(() -> new ArrayList<>(single.entrySet()).toString());
            case 33: return outcome(() -> {
                StringBuilder s = new StringBuilder();
                single.forEach((x, y) -> s.append(x).append('=').append(y));
                return s.toString();
            });
            case 34: return outcome(() -> List.of(single.equals(m.toSingleValueMap()), single.hashCode(), single.toString(), single.values().size(),
                    single.entrySet().size(), single.keySet().size()));
            case 35: {
                Map<String, Integer> all = new LinkedHashMap<>();
                for (int i = r.nextInt(3); i > 0; i--) { all.put(key(r), value(r)); }
                single.putAll(all);
                return null;
            }
            case 36: return outcome(() -> {
                Iterator<Integer> it = single.values().iterator();
                it.next();
                it.remove();
                return null;
            });
            case 37: return List.of(single.putIfAbsent(k, v) == null, single.replace(k, v) == null);
            case 38: if (r.nextInt(25) == 0) { single.clear(); } return null;
            case 39: return outcome(() -> single.equals(new HashSet<>()) || single.equals(single));
            default: throw new AssertionError(op);
        }
    }

    static Object outcome(Callable<?> action) {
        try {
            return String.valueOf(action.call());
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getName();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void same(String what, Callable<?> spring, Callable<?> owned) {
        assertEquals(outcome(spring), outcome(owned), what);
        CHECKS.incrementAndGet();
    }

    static String shapes(Map<String, List<Integer>> target) {
        StringBuilder s = new StringBuilder();
        target.forEach((k, l) -> s.append(k).append(':').append(l == null ? "null" : l.getClass().getName()).append(';'));
        return s.toString();
    }

    private void compareState(int step) {
        assertEquals(oracleTarget.toString(), m3Target.toString(), "target@" + step);
        assertEquals(shapes(oracleTarget), shapes(m3Target), "list classes@" + step);
        assertEquals(oracle.toString(), m3.toString(), "toString@" + step);
        assertEquals(oracle.hashCode(), m3.hashCode(), "hashCode@" + step);
        assertTrue(m3.equals(oracle) && oracle.equals(m3), "equals@" + step);
        CHECKS.addAndGet(5);
    }
}
