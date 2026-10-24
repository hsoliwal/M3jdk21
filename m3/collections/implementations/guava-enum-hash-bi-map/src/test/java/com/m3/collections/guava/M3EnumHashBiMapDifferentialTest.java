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
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.HashMultiset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Seeded differential run against the real Guava 33.4.8-jre EnumHashBiMap (test-scope oracle). Forward
 * order is EnumMap ordinal order on both sides and is compared exactly (toString included); the inverse
 * is HashMap-ordered in Guava, so inverse traversals are compared as maps and only order-independent
 * removals go through inverse iterators.
 */
final class M3EnumHashBiMapDifferentialTest {
    enum Key { A, B, C, D, E, F, G, H, I, J }

    enum Other { Z }

    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(M3EnumHashBiMapDifferentialTest.class.getName());
    private static final int OPS_PER_SEED = 12_000;
    private static final Key[] KEYS = Key.values();

    private final BiMap<Key, Integer> oracle = EnumHashBiMap.create(Key.class);
    private final M3EnumHashBiMap<Key, Integer> m3 = M3EnumHashBiMap.create(Key.class);

    @AfterAll
    static void report() {
        LOG.info(() -> "M3EnumHashBiMap differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {3L, 13L, 42L, 1_013L, 20_261_019L, 0xABCDEFL, -7L, 246_813_579L})
    void seededOperationsMatchGuava(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(36);
            long opSeed = random.nextLong();
            same("op" + op + "@" + step, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(step);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object apply(BiMap<Key, Integer> m, int op, long opSeed) {
        Random r = new Random(opSeed);
        Key k = KEYS[r.nextInt(KEYS.length)];
        Key k2 = KEYS[r.nextInt(KEYS.length)];
        Integer v = r.nextInt(12) == 0 ? null : r.nextInt(11);
        int shift = 1 + r.nextInt(4);
        Predicate<Integer> odd = x -> x != null && (x & 1) == 1;
        BiMap<Integer, Key> inv = m.inverse();
        switch (op) {
            case 0, 1, 2: return m.put(k, v);
            case 3: return m.forcePut(k, v);
            case 4: return m.remove(k);
            case 5: return List.of(String.valueOf(m.get(k)), m.containsKey(k), m.containsValue(v), m.size(), m.isEmpty());
            case 6: {
                Map<Key, Integer> more = new LinkedHashMap<>();
                more.put(k, v);
                more.put(k2, r.nextInt(11));
                m.putAll(more);
                return null;
            }
            case 7: return inv.put(v, k);
            case 8: return inv.forcePut(v, k);
            case 9: return inv.remove(v);
            case 10: return List.of(String.valueOf(inv.get(v)), inv.containsKey(v), inv.containsValue(k), inv.size());
            case 11: return m.keySet().remove(k);
            case 12: return m.values().remove(v);
            case 13: return m.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(k, v));
            case 14: return List.of(m.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(k, v)), m.keySet().contains(k), m.values().contains(v));
            case 15: return inv.keySet().remove(v);
            case 16: return inv.values().remove(k);
            case 17: return inv.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(v, k));
            case 18: return removeThrough(m.keySet().iterator(), x -> x.ordinal() % 3 == shift % 3)
                    | removeThrough(m.values().iterator(), odd.and(x -> x > 6))
                    | removeThrough(m.entrySet().iterator(), e -> e.getValue() == null);
            case 19: return removeThrough(inv.keySet().iterator(), x -> x != null && x == shift)
                    | removeThrough(inv.values().iterator(), x -> x == k)
                    | removeThrough(inv.entrySet().iterator(), e -> e.getValue() == k2);
            case 20: {
                for (Map.Entry<Key, Integer> e : m.entrySet()) {
                    if (e.getKey() == k) { return e.setValue(v); }
                }
                return "absent";
            }
            case 21: {
                for (Map.Entry<Integer, Key> e : inv.entrySet()) {
                    if (java.util.Objects.equals(e.getKey(), v)) { return e.setValue(k2); }
                }
                return "absent";
            }
            case 22: m.replaceAll((key, val) -> val == null ? null : (val + shift) % 11); return null;
            case 23: inv.replaceAll((val, key) -> KEYS[(key.ordinal() + shift) % KEYS.length]); return null;
            case 24: if (r.nextInt(30) == 0) { m.clear(); } return null;
            case 25: return List.of(String.valueOf(m.putIfAbsent(k, v)), String.valueOf(m.replace(k2, v)), m.replace(k, v, v), m.remove(k2, v));
            case 26: return String.valueOf(m.merge(k, shift, (a, b) -> a + b));
            case 27: return List.of(String.valueOf(m.compute(k, (key, val) -> val == null ? shift : null)),
                    String.valueOf(m.computeIfAbsent(k2, key -> shift + 20)), String.valueOf(m.computeIfPresent(k, (key, val) -> val)));
            case 28: return List.of(String.valueOf(m.getOrDefault(k, -1)), String.valueOf(inv.getOrDefault(v, k2)));
            case 29: return m.keySet().removeAll(Arrays.asList(k, k2)) | m.values().retainAll(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, null)));
            case 30: return inv.keySet().retainAll(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, null))) | inv.values().removeAll(List.of(k));
            case 31: return ((Map) m).put(Other.Z, v);
            case 32: return m.put(null, v);
            case 33: return inv.put(v, null);
            case 34: return List.of(m.toString(), m.keySet().toString(), m.values().toString(), m.entrySet().toString());
            case 35: return List.of(inv.toString().length(), inv.inverse() == m, inv.keySet().size(), inv.values().size());
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
            Object o = action.call();
            if (o instanceof List<?> l) {
                List<Object> out = new ArrayList<>();
                for (Object x : l) { out.add(x); }
                return out;
            }
            return o instanceof Collection<?> c ? HashMultiset.create(c) : o;
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getName() + ": " + e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
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
        assertEquals(oracle.inverse(), m3.inverse(), "inverse@" + step);
        assertEquals(m3.inverse(), oracle.inverse(), "inverse reverse@" + step);
        assertEquals(oracle.inverse().hashCode(), m3.inverse().hashCode(), "inverse hash@" + step);
        assertEquals(new HashMap<>(oracle.inverse()), new HashMap<>(m3.inverse()), "inverse iteration@" + step);
        assertEquals(oracle.keySet(), m3.keySet(), "keySet@" + step);
        assertEquals(oracle.values(), m3.values(), "values@" + step);
        assertEquals(oracle.entrySet(), m3.entrySet(), "entrySet@" + step);
        CHECKS.addAndGet(12);
    }
}
