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
import com.google.common.collect.EnumBiMap;
import com.google.common.collect.HashMultiset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
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
 * Seeded differential run against the real Guava 33.4.8-jre EnumBiMap (test-scope oracle). Both
 * directions are EnumMap-ordered on both sides, so every traversal, including both toString forms,
 * is compared exactly.
 */
final class M3EnumBiMapDifferentialTest {
    enum Key { A, B, C, D, E, F, G, H, I }

    enum Val { V0, V1, V2, V3, V4, V5, V6, V7 }

    enum Other { Z }

    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(M3EnumBiMapDifferentialTest.class.getName());
    private static final int OPS_PER_SEED = 12_000;
    private static final Key[] KEYS = Key.values();
    private static final Val[] VALS = Val.values();

    private final BiMap<Key, Val> oracle = EnumBiMap.create(Key.class, Val.class);
    private final M3EnumBiMap<Key, Val> m3 = M3EnumBiMap.create(Key.class, Val.class);

    @AfterAll
    static void report() {
        LOG.info(() -> "M3EnumBiMap differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {5L, 17L, 42L, 1_019L, 20_261_019L, 0x5EEDL, -11L, 135_792_468L})
    void seededOperationsMatchGuava(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(34);
            long opSeed = random.nextLong();
            same("op" + op + "@" + step, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(step);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object apply(BiMap<Key, Val> m, int op, long opSeed) {
        Random r = new Random(opSeed);
        Key k = KEYS[r.nextInt(KEYS.length)];
        Key k2 = KEYS[r.nextInt(KEYS.length)];
        Val v = VALS[r.nextInt(VALS.length)];
        Val v2 = VALS[r.nextInt(VALS.length)];
        int shift = 1 + r.nextInt(5);
        BiMap<Val, Key> inv = m.inverse();
        Predicate<Val> evenVal = x -> x.ordinal() % 2 == 0;
        switch (op) {
            case 0, 1, 2: return m.put(k, v);
            case 3: return m.forcePut(k, v);
            case 4: return m.remove(k);
            case 5: return List.of(String.valueOf(m.get(k)), m.containsKey(k), m.containsValue(v), m.size(), m.isEmpty(), String.valueOf(m.get("x")));
            case 6: {
                Map<Key, Val> more = new LinkedHashMap<>();
                more.put(k, v);
                more.put(k2, v2);
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
            case 14: return List.of(m.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(k, v)), inv.entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(v, k)));
            case 15: return inv.keySet().remove(v);
            case 16: return inv.values().remove(k);
            case 17: return inv.entrySet().remove(new AbstractMap.SimpleImmutableEntry<>(v, k));
            case 18: return removeThrough(m.keySet().iterator(), x -> x.ordinal() % 3 == shift % 3)
                    | removeThrough(m.values().iterator(), evenVal.and(x -> x.ordinal() > 5))
                    | removeThrough(m.entrySet().iterator(), e -> e.getValue() == v2);
            case 19: return removeThrough(inv.keySet().iterator(), x -> x == v)
                    | removeThrough(inv.values().iterator(), x -> x == k)
                    | removeThrough(inv.entrySet().iterator(), e -> e.getValue() == k2);
            case 20: {
                for (Map.Entry<Key, Val> e : m.entrySet()) {
                    if (e.getKey() == k) { return e.setValue(v); }
                }
                return "absent";
            }
            case 21: {
                for (Map.Entry<Val, Key> e : inv.entrySet()) {
                    if (e.getKey() == v) { return e.setValue(k2); }
                }
                return "absent";
            }
            case 22: m.replaceAll((key, val) -> VALS[(val.ordinal() + shift) % VALS.length]); return null;
            case 23: inv.replaceAll((val, key) -> KEYS[(key.ordinal() + shift) % KEYS.length]); return null;
            case 24: if (r.nextInt(30) == 0) { m.clear(); } return null;
            case 25: return List.of(String.valueOf(m.putIfAbsent(k, v)), String.valueOf(m.replace(k2, v)), m.replace(k, v, v2), m.remove(k2, v));
            case 26: return List.of(String.valueOf(m.compute(k, (key, val) -> val == null ? v : null)), String.valueOf(m.computeIfAbsent(k2, key -> v2)),
                    String.valueOf(m.getOrDefault(k, Val.V7)), String.valueOf(inv.getOrDefault(v, Key.A)));
            case 27: return m.keySet().removeAll(Arrays.asList(k, k2)) | m.values().retainAll(EnumSet.of(Val.V0, Val.V1, Val.V2, Val.V3, Val.V4, Val.V5));
            case 28: return inv.keySet().retainAll(EnumSet.of(Val.V1, Val.V2, Val.V3, Val.V4, Val.V5, Val.V6, Val.V7)) | inv.values().removeAll(List.of(k));
            case 29: return ((Map) m).put(Other.Z, v);
            case 30: return m.put(null, v) == null;
            case 31: return m.put(k, null) == null;
            case 32: return List.of(m.toString(), inv.toString(), m.keySet().toString(), m.values().toString(), m.entrySet().toString(),
                    inv.keySet().toString(), inv.values().toString(), inv.entrySet().toString());
            case 33: return List.of(m.hashCode(), inv.hashCode(), inv.inverse() == m, m.keySet().hashCode(), inv.entrySet().hashCode());
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
            if (o instanceof List<?> l) { return new ArrayList<>(l); }
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
        assertEquals(oracle.inverse().toString(), m3.inverse().toString(), "inverse toString@" + step);
        assertTrue(m3.inverse().equals(oracle.inverse()), "inverse equals@" + step);
        assertTrue(oracle.inverse().equals(m3.inverse()), "inverse reverse@" + step);
        assertEquals(oracle.inverse().hashCode(), m3.inverse().hashCode(), "inverse hash@" + step);
        CHECKS.addAndGet(9);
    }
}
