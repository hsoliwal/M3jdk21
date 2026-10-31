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
package com.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.collections4.map.PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy;
import org.apache.commons.collections4.map.PassiveExpiringMap.ExpirationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Focused contract cases: constructor checks and the system clock against the real Commons 4.5.0 class, the purge
 * points, views, fail-fast rules, the map-iterator adapter, entry flyweights, serialization, the expiry-index modes and
 * the lane invariants. Scenarios that need time control run on both the M3 map and the fake-clock oracle.
 */
final class M3PassiveExpiringMapContractTest {
    private static final LongSupplier CLOCK = M3PassiveExpiringTestClock::currentTimeMillis;
    private static final M3PassiveExpiringOracle ORACLE = new M3PassiveExpiringOracle();

    @BeforeEach
    void resetClock() {
        M3PassiveExpiringTestClock.set(1_000L, 0);
    }

    /** Absolute expiration time carried by a Long value; any other value never expires. */
    static final class Absolute implements ExpirationPolicy<Object, Object> {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public long expirationTime(Object key, Object value) {
            return value instanceof Long l ? l : -1L;
        }
    }

    /** Puts {@code extra} other keys into its map while computing an expiration time (re-entrant policy). */
    static final class Reentrant implements ExpirationPolicy<Object, Object> {
        @Serial
        private static final long serialVersionUID = 1L;
        transient Map<Object, Object> target;
        transient int extra;

        @Override
        public long expirationTime(Object key, Object value) {
            Map<Object, Object> m = target;
            int n = extra;
            target = null;
            for (int i = 0; m != null && i < n; i++) {
                m.put("r" + i, i);
            }
            return -1L;
        }
    }

    /** Key whose equals accepts an equal String, while String.equals rejects it: an asymmetric equality. */
    static final class Loose {
        private final String text;

        Loose(String text) {
            this.text = text;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Loose l ? l.text.equals(text) : text.equals(o);
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }
    }

    private static <K, V> M3PassiveExpiringMap<K, V> fake(long ttl) {
        return new M3PassiveExpiringMap<>(null, ttl, Map.of(), CLOCK);
    }

    private static void sameThrow(Executable commons, Executable m3) {
        Throwable expected = assertThrows(Throwable.class, commons);
        Throwable actual = assertThrows(Throwable.class, m3);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.getMessage(), actual.getMessage());
    }

    /** Runs one scenario on a fake-clock oracle and on the M3 map from the same clock state. */
    private static void sameScenario(Supplier<Map<Object, Object>> oracle, Supplier<Map<Object, Object>> m3,
            Function<Map<Object, Object>, List<Object>> scenario) {
        long t0 = M3PassiveExpiringTestClock.peek();
        List<Object> expected = scenario.apply(oracle.get());
        M3PassiveExpiringTestClock.rewind(t0);
        Map<Object, Object> map = m3.get();
        List<Object> actual = scenario.apply(map);
        assertEquals(expected, actual);
        ((M3PassiveExpiringMap<Object, Object>) map).verifyInvariants();
    }

    private static Object outcome(Supplier<?> action) {
        try {
            return String.valueOf(action.get());
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName();
        }
    }

    private static Object run(Runnable action) {
        return outcome(() -> {
            action.run();
            return "ok";
        });
    }

    private static List<String> sorted(Collection<?> c) {
        List<String> out = new ArrayList<>();
        for (Object o : c) {
            out.add(String.valueOf(o));
        }
        Collections.sort(out);
        return out;
    }

    // ------------------------------------------------------------------ constructors and the system clock

    @Test
    void constructorsRejectNullsLikeTheProvider() {
        Map<String, String> noMap = null;
        TimeUnit noUnit = null;
        ExpirationPolicy<String, String> noPolicy = null;
        ExpirationPolicy<String, String> never = new ConstantTimeToLiveExpirationPolicy<>(-1L);
        Map<String, String> map = new HashMap<>();
        sameThrow(() -> new PassiveExpiringMap<>(noMap), () -> new M3PassiveExpiringMap<>(noMap));
        sameThrow(() -> new PassiveExpiringMap<>(5L, noMap), () -> new M3PassiveExpiringMap<>(5L, noMap));
        sameThrow(() -> new PassiveExpiringMap<>(5L, noUnit), () -> new M3PassiveExpiringMap<>(5L, noUnit));
        sameThrow(() -> new PassiveExpiringMap<>(5L, noUnit, noMap), () -> new M3PassiveExpiringMap<>(5L, noUnit, noMap));
        sameThrow(() -> new PassiveExpiringMap<>(5L, TimeUnit.SECONDS, noMap),
                () -> new M3PassiveExpiringMap<>(5L, TimeUnit.SECONDS, noMap));
        sameThrow(() -> new PassiveExpiringMap<>(noPolicy), () -> new M3PassiveExpiringMap<>(noPolicy));
        sameThrow(() -> new PassiveExpiringMap<>(noPolicy, noMap), () -> new M3PassiveExpiringMap<>(noPolicy, noMap));
        sameThrow(() -> new PassiveExpiringMap<>(never, noMap), () -> new M3PassiveExpiringMap<>(never, noMap));
        sameThrow(() -> new PassiveExpiringMap<>(noPolicy, map), () -> new M3PassiveExpiringMap<>(noPolicy, map));
        assertThrows(NullPointerException.class, () -> new M3PassiveExpiringMap<>(null, 1L, Map.of(), null));
    }

    @Test
    void publicConstructorsMatchTheProviderOnTheSystemClock() {
        Map<Object, Object> init = new HashMap<>(Map.of("i", "init"));
        ExpirationPolicy<Object, Object> abs = new Absolute();
        List<Supplier<Map<Object, Object>>> commons = List.of(PassiveExpiringMap::new,
                () -> new PassiveExpiringMap<>(0L), () -> new PassiveExpiringMap<>(Long.MAX_VALUE),
                () -> new PassiveExpiringMap<>(-5L), () -> new PassiveExpiringMap<>(1L, TimeUnit.DAYS),
                () -> new PassiveExpiringMap<>(999_999L, TimeUnit.NANOSECONDS),
                () -> new PassiveExpiringMap<>(0L, new HashMap<>(init)),
                () -> new PassiveExpiringMap<>(0L, TimeUnit.DAYS, new HashMap<>(init)),
                () -> new PassiveExpiringMap<>(new HashMap<>(init)), () -> new PassiveExpiringMap<>(abs),
                () -> new PassiveExpiringMap<>(abs, new HashMap<>(init)),
                () -> new PassiveExpiringMap<>(new ConstantTimeToLiveExpirationPolicy<>(0L)),
                () -> new PassiveExpiringMap<>(new ConstantTimeToLiveExpirationPolicy<>(1L, TimeUnit.DAYS)));
        List<Supplier<Map<Object, Object>>> m3 = List.of(M3PassiveExpiringMap::new,
                () -> new M3PassiveExpiringMap<>(0L), () -> new M3PassiveExpiringMap<>(Long.MAX_VALUE),
                () -> new M3PassiveExpiringMap<>(-5L), () -> new M3PassiveExpiringMap<>(1L, TimeUnit.DAYS),
                () -> new M3PassiveExpiringMap<>(999_999L, TimeUnit.NANOSECONDS),
                () -> new M3PassiveExpiringMap<>(0L, new HashMap<>(init)),
                () -> new M3PassiveExpiringMap<>(0L, TimeUnit.DAYS, new HashMap<>(init)),
                () -> new M3PassiveExpiringMap<>(new HashMap<>(init)), () -> new M3PassiveExpiringMap<>(abs),
                () -> new M3PassiveExpiringMap<>(abs, new HashMap<>(init)),
                () -> new M3PassiveExpiringMap<>(new ConstantTimeToLiveExpirationPolicy<>(0L)),
                () -> new M3PassiveExpiringMap<>(new ConstantTimeToLiveExpirationPolicy<>(1L, TimeUnit.DAYS)));
        for (int i = 0; i < commons.size(); i++) {
            assertEquals(systemClockScenario(commons.get(i).get()), systemClockScenario(m3.get(i).get()), "ctor " + i);
        }
    }

    private static List<Object> systemClockScenario(Map<Object, Object> m) {
        List<Object> out = new ArrayList<>();
        out.add(m.put("a", "1"));
        out.add(m.put("past", 0L));
        out.add(m.put("never", -1L));
        out.add(m.put("future", Long.MAX_VALUE));
        out.add(m.get("a"));
        out.add(m.containsKey("past"));
        out.add(m.get("i"));
        out.add(m.size());
        out.add(sorted(m.keySet()));
        out.add(m.put("a", "2"));
        return out;
    }

    @Test
    void constantRuleOnTheFakeClock() {
        M3PassiveExpiringTestClock.set(Long.MAX_VALUE - 10_000L, 0);
        sameScenario(() -> ORACLE.create(1_000_000L), () -> fake(1_000_000L), m -> {
            m.put("overflow", "never");
            M3PassiveExpiringTestClock.advance(5_000L);
            return List.of(m.size(), m.get("overflow"));
        });
        M3PassiveExpiringTestClock.set(1_000L, 0);
        sameScenario(() -> ORACLE.create(10L), () -> fake(10L), m -> {
            List<Object> out = new ArrayList<>();
            m.put("a", "1");
            M3PassiveExpiringTestClock.advance(9);
            out.add(m.get("a"));
            M3PassiveExpiringTestClock.advance(1);
            out.add(m.containsKey("a"));
            m.put("b", "2");
            M3PassiveExpiringTestClock.advance(10);
            out.add(m.put("b", "3"));
            out.add(m.get("b"));
            return out;
        });
        sameScenario(() -> ORACLE.createWithPolicy(ORACLE.constantPolicy(0L)),
                () -> new M3PassiveExpiringMap<>(null, 0L, Map.of(), CLOCK), m -> {
                    m.put("z", "0");
                    return List.of(m.size(), m.isEmpty(), String.valueOf(m.get("z")));
                });
    }

    @Test
    void clockReadsFollowTheProvider() {
        M3PassiveExpiringTestClock.set(5_000L, 1);
        sameScenario(() -> ORACLE.create(-1L), () -> fake(-1L), m -> {
            m.put("a", "1");
            m.get("a");
            m.containsKey("a");
            m.containsValue("1");
            m.size();
            m.isEmpty();
            m.keySet();
            m.values();
            m.entrySet();
            m.remove("a");
            m.clear();
            m.hashCode();
            return List.of(M3PassiveExpiringTestClock.peek());
        });
        sameScenario(() -> ORACLE.create(30L), () -> fake(30L), m -> {
            m.put("a", "1");
            m.putAll(Map.of("b", "2"));
            m.getOrDefault("c", "d");
            m.putIfAbsent("c", "3");
            return List.of(M3PassiveExpiringTestClock.peek(), sorted(m.keySet()));
        });
    }

    // ------------------------------------------------------------------ purge points and views

    @Test
    void viewsRemoveEqualsHashCodeAndToStringExpireNothing() {
        sameScenario(() -> ORACLE.create(10L), () -> fake(10L), m -> {
            List<Object> out = new ArrayList<>();
            m.put("a", "1");
            m.put("b", "2");
            Set<Object> keys = m.keySet();
            Collection<Object> vals = m.values();
            Set<Map.Entry<Object, Object>> entries = m.entrySet();
            M3PassiveExpiringTestClock.advance(10);
            out.add(keys.size());
            out.add(keys.contains("a"));
            out.add(vals.contains("1"));
            out.add(entries.contains(new SimpleEntry<>("b", "2")));
            out.add(m.hashCode());
            out.add(m.equals(Map.of("a", "1", "b", "2")));
            out.add(sorted(Arrays.asList(m.toString().replaceAll("[{}]", "").split(", "))));
            out.add(m.remove("a"));
            out.add(keys.size());
            out.add(m.size());
            out.add(keys.size());
            out.add(m.toString());
            return out;
        });
    }

    @Test
    void wholeMapCallsExpireEverythingFirst() {
        sameScenario(() -> ORACLE.create(5L), () -> fake(5L), m -> {
            List<Object> out = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                m.put(i, "v" + i);
                M3PassiveExpiringTestClock.advance(1);
            }
            out.add(m.containsValue("v0"));
            out.add(sorted(m.values()));
            out.add(sorted(m.entrySet()));
            M3PassiveExpiringTestClock.advance(2);
            List<String> visited = new ArrayList<>();
            m.forEach((k, v) -> visited.add(k + "=" + v));
            out.add(sorted(visited));
            m.replaceAll((k, v) -> v + "!");
            M3PassiveExpiringTestClock.advance(1);
            MapIterator<?, ?> unused = null;
            out.add(unused == null);
            out.add(sorted(m.keySet()));
            out.add(m.get(5));
            M3PassiveExpiringTestClock.advance(10);
            out.add(m.isEmpty());
            return out;
        });
    }

    @Test
    void iteratorsFailFastLikeTheProvidersHashMap() {
        sameScenario(() -> ORACLE.create(-1L), () -> fake(-1L), m -> {
            List<Object> out = new ArrayList<>();
            m.put("a", "1");
            m.put("b", "2");
            Iterator<Object> it = m.keySet().iterator();
            out.add(run(it::remove));
            it.next();
            m.put("a", "replaced");
            m.put("b", "replaced");
            out.add(outcome(() -> it.next() != null));
            out.add(run(it::remove));
            out.add(run(it::remove));
            out.add(outcome(it::next));
            m.put("c", "3");
            Iterator<Object> stale = m.values().iterator();
            m.put("d", "4");
            out.add(stale.hasNext());
            out.add(outcome(stale::next));
            out.add(run(stale::remove));
            Iterator<Map.Entry<Object, Object>> cleared = m.entrySet().iterator();
            m.clear();
            out.add(cleared.hasNext());
            out.add(outcome(cleared::next));
            Iterator<Object> empty = m.keySet().iterator();
            m.clear();
            out.add(outcome(empty::next));
            m.put("x", "1");
            out.add(run(() -> m.keySet().forEach(k -> m.put("y", "2"))));
            out.add(run(() -> m.values().forEach(v -> m.remove("y"))));
            out.add(run(() -> m.entrySet().forEach(e -> m.put("z", "3"))));
            out.add(run(() -> m.forEach((k, v) -> m.remove("zz"))));
            out.add(run(() -> m.forEach((k, v) -> m.put(k + "+", "n"))));
            out.add(m.size());
            return out;
        });
    }

    @Test
    void replaceAllWithStructuralChangesLikeTheProvider() {
        sameScenario(() -> ORACLE.create(-1L), () -> fake(-1L), m -> {
            m.put("only", "1");
            Object first = run(() -> m.replaceAll((k, v) -> {
                m.remove(k);
                return "lost";
            }));
            m.put("p", "1");
            Object second = run(() -> m.replaceAll((k, v) -> {
                m.put("q", "new");
                return "kept";
            }));
            Object kept = String.valueOf(m.get("p"));
            m.clear();
            m.put("t", "1");
            Object third = run(() -> m.replaceAll((k, v) -> {
                m.remove(k);
                m.put("u", "reused");
                return "dropped";
            }));
            return List.of(first, second, kept, third, String.valueOf(m.get("u")), m.size());
        });
        sameScenario(() -> ORACLE.create(-1L), () -> fake(-1L), m -> {
            m.put("p", "1");
            m.put("q", "2");
            return List.of(run(() -> m.replaceAll((k, v) -> {
                m.put("r", "3");
                return v;
            })), run(() -> m.forEach((k, v) -> m.put("s", "4"))));
        });
    }

    @Test
    void mapIteratorFollowsTheProvidersAdapter() {
        sameScenario(() -> ORACLE.create(-1L), () -> fake(-1L), m -> {
            List<Object> out = new ArrayList<>();
            m.put("k", "v");
            M3PassiveExpiringOracle.MapCursor c = cursorOf(m);
            out.add(outcome(c::getKey));
            out.add(outcome(c::getValue));
            out.add(outcome(() -> c.setValue("x")));
            out.add(run(c::remove));
            out.add(c.hasNext());
            out.add(c.next());
            out.add(c.getKey());
            out.add(c.setValue("w"));
            out.add(c.getValue());
            out.add(m.get("k"));
            m.put("k", "changed");
            out.add(c.getValue());
            out.add(outcome(c::next));
            c.reset();
            out.add(c.getKey());
            out.add(run(c::remove));
            out.add(c.next());
            c.remove();
            out.add(outcome(c::getKey));
            out.add(m.size());
            m.put("d", "1");
            M3PassiveExpiringOracle.MapCursor d = cursorOf(m);
            d.next();
            m.remove("d");
            m.put("e2", "z");
            out.add(d.getKey());
            out.add(d.getValue());
            out.add(d.setValue("detached"));
            out.add(d.getValue());
            out.add(m.containsKey("d"));
            out.add(run(d::remove));
            return out;
        });
    }

    private static M3PassiveExpiringOracle.MapCursor cursorOf(Map<Object, Object> m) {
        if (m instanceof M3PassiveExpiringMap<Object, Object> own) {
            MapIterator<Object, Object> it = own.mapIterator();
            assertTrue(it instanceof ResettableIterator<?>);
            return M3PassiveExpiringOracle.cursor(it);
        }
        return ORACLE.cursor(m);
    }

    @Test
    void entryViewAndFlyweightEntries() {
        sameScenario(() -> ORACLE.create(-1L), () -> fake(-1L), m -> {
            List<Object> out = new ArrayList<>();
            String value = "v";
            m.put("k", value);
            m.put("n", null);
            Set<Map.Entry<Object, Object>> es = m.entrySet();
            out.add(es.contains("k"));
            out.add(es.contains(new SimpleEntry<>("missing", "v")));
            out.add(es.contains(new SimpleEntry<>("k", "other")));
            out.add(es.contains(new SimpleEntry<>("n", null)));
            out.add(es.remove("k"));
            out.add(es.remove(new SimpleEntry<>("missing", "v")));
            out.add(es.remove(new SimpleEntry<>("k", "other")));
            out.add(es.remove(new SimpleEntry<>("k", null)));
            out.add(es.remove(new SimpleEntry<>("n", "x")));
            out.add(es.remove(new SimpleEntry<>("n", null)));
            out.add(es.remove(new SimpleEntry<>("k", value)));
            m.put("eq", "12");
            out.add(es.remove(new SimpleEntry<>("eq", new String("12"))));
            m.put("x", "1");
            out.add(es.contains(new SimpleEntry<>(new Loose("x"), "1")));
            out.add(m.remove("x"));
            m.put("e", "1");
            Map.Entry<Object, Object> e = es.iterator().next();
            out.add(e.equals(e));
            out.add(e.equals("e=1"));
            out.add(e.equals(new SimpleEntry<>("e", "1")));
            out.add(e.equals(new SimpleEntry<>("f", "1")));
            out.add(e.equals(new SimpleEntry<>("e", "2")));
            out.add(e.hashCode());
            out.add(e.toString());
            out.add(e.setValue("2"));
            out.add(m.get("e"));
            m.remove("e");
            m.put("e3", "z");
            out.add(e.getValue());
            out.add(e.setValue("3"));
            out.add(e.getValue());
            out.add(m.containsKey("e"));
            out.add(m.get("e3"));
            m.remove("e3");
            out.add(m.keySet().remove("absent"));
            m.put("g", "1");
            out.add(m.keySet().remove("g"));
            out.add(m.values().contains(null));
            return out;
        });
    }

    @Test
    void equalsHashCodeAndToStringFormats() {
        M3PassiveExpiringMap<Object, Object> m = fake(-1L);
        assertEquals("{}", m.toString());
        assertTrue(m.equals(m));
        assertFalse(m.equals("text"));
        assertTrue(m.equals(Map.of()));
        m.put("a", "1");
        assertEquals("{a=1}", m.toString());
        assertEquals(Map.of("a", "1").hashCode(), m.hashCode());
        assertFalse(m.equals(Map.of()));
        assertFalse(m.equals(Map.of("a", "2")));
        m.put(null, null);
        assertFalse(m.equals(new TreeMap<>(Map.of("a", "1", "b", "2"))), "TreeMap.get(null) throws NPE, caught");
        Map<Object, Object> withNull = new HashMap<>();
        withNull.put("a", "1");
        withNull.put(null, null);
        assertTrue(m.equals(withNull));
        withNull.put(null, "not null");
        assertFalse(m.equals(withNull));
        withNull.remove(null);
        withNull.put("z", null);
        assertFalse(m.equals(withNull));
        fake(-1L).keySet().forEach(k -> {
            throw new AssertionError("empty map visits nothing");
        });
        M3PassiveExpiringMap<Object, Object> ints = fake(-1L);
        ints.put(1, "1");
        TreeMap<Object, Object> strings = new TreeMap<>();
        strings.put("1", "1");
        assertFalse(ints.equals(strings), "TreeMap.get(Integer) on String keys throws CCE, caught");
        ints.put("self", ints);
        assertTrue(ints.toString().contains("self=(this Map)"));
        M3PassiveExpiringMap<Object, Object> keyed = fake(-1L);
        keyed.put(keyed, "x");
        assertEquals("{(this Map)=x}", keyed.toString());
    }

    // ------------------------------------------------------------------ storage

    @Test
    void collidingKeysCopiesAndChurnKeepTheLanesConsistent() {
        M3PassiveExpiringMap<Object, Object> m = fake(-1L);
        assertEquals("Aa".hashCode(), "BB".hashCode());
        m.put("Aa", 1);
        m.put("BB", 2);
        assertEquals(1, m.get("Aa"));
        assertEquals(2, m.get("BB"));
        assertEquals(1, m.remove("Aa"));
        assertEquals(2, m.get("BB"));
        assertNull(m.remove("Aa"));
        for (int i = 0; i < 2_000; i++) {
            m.put(i, i);
        }
        for (int i = 1; i < 2_000; i += 2) {
            m.remove(i);
        }
        for (int i = 0; i < 2_000; i += 2) {
            assertEquals(i, m.get(i));
        }
        m.verifyInvariants();
        for (int i = 3_000; i < 3_500; i++) {
            m.put(i, i);
        }
        m.verifyInvariants();
        assertEquals(1_501, m.size());
        Map<Object, Object> identity = new IdentityHashMap<>();
        identity.put(Integer.valueOf(1000), "a");
        identity.put(Integer.valueOf(1000), "b");
        assertEquals(2, identity.size());
        M3PassiveExpiringMap<Object, Object> copy = new M3PassiveExpiringMap<>(identity);
        assertEquals(1, copy.size());
        assertTrue(Set.of("a", "b").contains(copy.get(1000)));
        copy.verifyInvariants();
        assertEquals(16, M3PassiveExpiringMap.tableFor(10));
        assertEquals(4, M3PassiveExpiringMap.tableFor(1));
        assertThrows(OutOfMemoryError.class, () -> M3PassiveExpiringMap.tableFor(Integer.MAX_VALUE));
        assertEquals(8, M3PassiveExpiringMap.grownLanes(0));
        assertEquals(Integer.MAX_VALUE - 8, M3PassiveExpiringMap.grownLanes(Integer.MAX_VALUE - 9));
        assertThrows(OutOfMemoryError.class, () -> M3PassiveExpiringMap.grownLanes(Integer.MAX_VALUE - 8));
    }

    @Test
    void reentrantPolicyThatGrowsTheTable() {
        Reentrant commonsPolicy = new Reentrant();
        PassiveExpiringMap<Object, Object> commons = new PassiveExpiringMap<>(commonsPolicy);
        Reentrant m3Policy = new Reentrant();
        M3PassiveExpiringMap<Object, Object> m3 = new M3PassiveExpiringMap<>(m3Policy);
        for (Map<Object, Object> m : List.<Map<Object, Object>>of(commons, m3)) {
            m.put("a", "1");
        }
        commonsPolicy.target = commons;
        commonsPolicy.extra = 40;
        m3Policy.target = m3;
        m3Policy.extra = 40;
        assertEquals(commons.put("b", "2"), m3.put("b", "2"));
        commonsPolicy.target = commons;
        commonsPolicy.extra = 3;
        m3Policy.target = m3;
        m3Policy.extra = 3;
        assertEquals(commons.put("a", "9"), m3.put("a", "9"));
        assertEquals(new HashMap<>(commons), new HashMap<>(m3));
        m3.verifyInvariants();
    }

    @Test
    void orderIndexModesAndCompaction() {
        M3PassiveExpiringMap<Object, Object> m = fake(100L);
        for (int i = 0; i < 50; i++) {
            m.put(i, i);
            M3PassiveExpiringTestClock.advance(1);
        }
        assertFalse(m.orderIsHeap(), "one ttl on a monotone clock keeps the FIFO ring");
        M3PassiveExpiringTestClock.advance(-30);
        m.put("late", 0);
        assertTrue(m.orderIsHeap(), "a clock step back switches to the heap");
        m.verifyInvariants();
        M3PassiveExpiringTestClock.advance(1_000);
        assertEquals(0, m.size());
        assertFalse(m.orderIsHeap(), "an emptied index returns to the ring");
        for (int i = 0; i < 1_000; i++) {
            m.put("hot", i);
            M3PassiveExpiringTestClock.advance(1);
        }
        assertEquals(1, m.orderRecords(), "refreshing the first record pops it, so no stale record is left");
        m.put("anchor", 0);
        for (int i = 0; i < 1_000; i++) {
            m.put("hot", i);
            M3PassiveExpiringTestClock.advance(1);
        }
        assertTrue(m.orderRecords() <= 64, "stale refresh records behind the first are compacted: " + m.orderRecords());
        M3PassiveExpiringTestClock.advance(-5);
        m.put("hot", -1);
        m.put("same", 1);
        m.put("same", 2);
        m.verifyInvariants();
        M3PassiveExpiringMap<Object, Object> never = fake(-1L);
        never.put("x", 1);
        never.put("x", 2);
        assertEquals(0, never.orderRecords());
        M3PassiveExpiringMap<Object, Object> policy = new M3PassiveExpiringMap<>(new Absolute(), 0L, Map.of(), CLOCK);
        policy.put("a", 5_000L);
        policy.put("b", 3_000L);
        policy.put("a", -1L);
        policy.put("c", "never");
        policy.put("c", 2_000L);
        assertTrue(policy.orderIsHeap());
        policy.verifyInvariants();
        M3PassiveExpiringTestClock.set(4_000L, 0);
        assertEquals(1, policy.size());
        assertEquals(-1L, policy.get("a"));
        policy.clear();
        policy.clear();
        assertEquals(0, policy.orderRecords());
        policy.put("d", 1L);
        policy.verifyInvariants();
    }

    @Test
    void removalsAndRefreshesInExpiryOrderPopTheFirstRecordAtOnce() {
        M3PassiveExpiringMap<Object, Object> m = fake(100L);
        m.put("a", 1);
        M3PassiveExpiringTestClock.advance(1);
        m.put("s", 2);
        M3PassiveExpiringTestClock.advance(1);
        assertEquals(2, m.orderRecords());
        m.put("s", 3);
        assertEquals(3, m.orderRecords(), "the refreshed record is not first, so it stays as a stale record");
        m.remove("a");
        assertEquals(2, m.orderRecords(), "the removed first record is popped at once");
        m.remove("s");
        assertEquals(2, m.orderRecords(), "a stale first record of the removed slot is left for purge");
        m.verifyInvariants();
        assertEquals(0, m.size());
        assertEquals(0, m.orderRecords());
        for (int i = 0; i < 1_000; i++) {
            m.put(i, i);
            if (i >= 10) {
                m.remove(i - 10);
            }
        }
        assertEquals(10, m.orderRecords(), "first-in first-out churn leaves no stale record");
        m.verifyInvariants();
        M3PassiveExpiringMap<Object, Object> heap = new M3PassiveExpiringMap<>(new Absolute(), 0L, Map.of(), CLOCK);
        heap.put("x", 5_000L);
        heap.put("y", 3_000L);
        assertTrue(heap.orderIsHeap());
        heap.remove("y");
        assertEquals(1, heap.orderRecords(), "the heap minimum is popped when its mapping is removed");
        heap.put("x", 6_000L);
        assertEquals(1, heap.orderRecords(), "refreshing the heap minimum replaces its record");
        heap.remove("x");
        assertEquals(0, heap.orderRecords());
        heap.verifyInvariants();
    }

    @Test
    void putAllUsesTheArgumentOrderAndRefreshes() {
        sameScenario(() -> ORACLE.create(10L), () -> fake(10L), m -> {
            Map<Object, Object> batch = new LinkedHashMap<>();
            batch.put("a", "1");
            batch.put("b", "2");
            m.putAll(batch);
            M3PassiveExpiringTestClock.advance(6);
            m.putAll(Map.of("a", "3"));
            M3PassiveExpiringTestClock.advance(6);
            return List.of(sorted(m.entrySet()), run(() -> m.putAll(null)));
        });
    }

    // ------------------------------------------------------------------ serialization

    @Test
    void serializationRoundTripKeepsMappingsAndExpirationTimes() throws Exception {
        ExpirationPolicy<Object, Object> abs = new Absolute();
        PassiveExpiringMap<Object, Object> commons = new PassiveExpiringMap<>(abs);
        M3PassiveExpiringMap<Object, Object> m3 = new M3PassiveExpiringMap<>(abs);
        for (Map<Object, Object> m : List.<Map<Object, Object>>of(commons, m3)) {
            m.put("past", 0L);
            m.put("never", -1L);
            m.put("future", Long.MAX_VALUE);
            m.put("plain", "text");
            m.put(null, null);
        }
        Map<Object, Object> commonsCopy = roundTrip(commons);
        @SuppressWarnings("unchecked")
        M3PassiveExpiringMap<Object, Object> m3Copy = (M3PassiveExpiringMap<Object, Object>) roundTrip(m3);
        m3Copy.verifyInvariants();
        assertEquals(commonsCopy.hashCode(), m3Copy.hashCode());
        assertEquals(sorted(Arrays.asList(commonsCopy.toString().replaceAll("[{}]", "").split(", "))),
                sorted(Arrays.asList(m3Copy.toString().replaceAll("[{}]", "").split(", "))));
        assertEquals(commonsCopy.size(), m3Copy.size());
        assertEquals(sorted(commonsCopy.keySet()), sorted(m3Copy.keySet()));
        assertEquals(commonsCopy.put("again", 0L), m3Copy.put("again", 0L));
        assertEquals(commonsCopy.size(), m3Copy.size());
        M3PassiveExpiringMap<Object, Object> ttl = new M3PassiveExpiringMap<>(0L);
        @SuppressWarnings("unchecked")
        M3PassiveExpiringMap<Object, Object> ttlCopy = (M3PassiveExpiringMap<Object, Object>) roundTrip(ttl);
        ttlCopy.put("gone", "1");
        assertNull(ttlCopy.get("gone"));
        M3PassiveExpiringMap<Object, Object> empty = new M3PassiveExpiringMap<>();
        assertEquals(empty, roundTrip(empty));
    }

    @Test
    void corruptStreamsAreRejected() throws Exception {
        byte[] empty = serialize(new M3PassiveExpiringMap<>());
        byte[] negative = replace(empty, new byte[] {0x77, 0x04, 0, 0, 0, 0, 0x78},
                new byte[] {0x77, 0x04, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x78});
        assertThrows(InvalidObjectException.class, () -> deserialize(negative));
        M3PassiveExpiringMap<Object, Object> two = new M3PassiveExpiringMap<>();
        two.put("ka", "1");
        two.put("kb", "2");
        byte[] duplicate = replace(serialize(two), new byte[] {0x74, 0, 2, 'k', 'b'}, new byte[] {0x74, 0, 2, 'k', 'a'});
        assertThrows(InvalidObjectException.class, () -> deserialize(duplicate));
    }

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] b) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(b))) {
            return in.readObject();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> roundTrip(Map<Object, Object> m) throws IOException, ClassNotFoundException {
        return (Map<Object, Object>) deserialize(serialize(m));
    }

    private static byte[] replace(byte[] data, byte[] from, byte[] to) {
        for (int i = data.length - from.length; i >= 0; i--) {
            if (Arrays.equals(data, i, i + from.length, from, 0, from.length)) {
                byte[] out = data.clone();
                System.arraycopy(to, 0, out, i, to.length);
                return out;
            }
        }
        throw new AssertionError("pattern not found");
    }

    // ------------------------------------------------------------------ invariants

    @Test
    void verifyInvariantsRejectsCorruptLanes() throws ReflectiveOperationException {
        assertThrows(IllegalStateException.class, () -> corrupted(m -> setInt(m, "size", 7)));
        assertThrows(IllegalStateException.class, () -> corrupted(m -> ((int[]) get(m, "hashes"))[0] ^= 1));
        assertThrows(IllegalStateException.class, () -> corrupted(m -> {
            int[] hashes = (int[]) get(m, "hashes");
            hashes[1] = 1;
            setInt(m, "freeHead", 1);
        }));
        assertThrows(IllegalStateException.class, () -> corrupted(m -> {
            long[] table = (long[]) get(m, "table");
            Arrays.fill(table, 0L);
        }));
        assertThrows(IllegalStateException.class, () -> corrupted(m -> {
            long[] table = (long[]) get(m, "table");
            for (int p = 0; p < table.length; p++) {
                table[p] = table[p] == 0L ? 0L : table[p] ^ 1L << 40;
            }
        }));
        assertThrows(IllegalStateException.class, () -> corrupted(m -> setInt(m, "indexed", 0)));
        assertThrows(IllegalStateException.class, () -> corrupted(m -> {
            M3ExpiryOrderIndex order = (M3ExpiryOrderIndex) get(m, "order");
            order.add(Long.MAX_VALUE, 0, ((int[]) get(m, "stamps"))[0]);
        }));
        assertThrows(IllegalStateException.class, () -> corrupted(m -> ((long[]) get(m, "deadlines"))[0] = 1L));
        assertThrows(IllegalStateException.class, () -> corrupted(m -> setInt(m, "freeHead", 0)));
    }

    @FunctionalInterface
    private interface Corruption {
        void apply(M3PassiveExpiringMap<Object, Object> m) throws ReflectiveOperationException;
    }

    private static void corrupted(Corruption corruption) throws ReflectiveOperationException {
        M3PassiveExpiringMap<Object, Object> m = fake(1_000L);
        m.put("a", "1");
        m.put("b", "2");
        m.put("c", "3");
        m.remove("b");
        m.verifyInvariants();
        corruption.apply(m);
        m.verifyInvariants();
    }

    private static Object get(Object target, String name) throws ReflectiveOperationException {
        Field f = M3PassiveExpiringMap.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void setInt(Object target, String name, int value) throws ReflectiveOperationException {
        Field f = M3PassiveExpiringMap.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    @Test
    void viewsAreCachedAndKeepTheMapIdentity() {
        M3PassiveExpiringMap<Object, Object> m = fake(-1L);
        assertSame(m.keySet(), m.keySet());
        assertSame(m.values(), m.values());
        assertSame(m.entrySet(), m.entrySet());
        m.put("a", "1");
        m.keySet().clear();
        assertEquals(0, m.size());
        m.put("b", "2");
        m.values().clear();
        m.put("c", "3");
        m.entrySet().clear();
        assertTrue(m.isEmpty());
        assertThrows(NoSuchElementException.class, () -> m.values().iterator().next());
        assertThrows(ConcurrentModificationException.class, () -> {
            Iterator<Object> it = m.keySet().iterator();
            m.put("d", "4");
            it.next();
        });
    }

    // ------------------------------------------------------------------ node identity

    /**
     * One flyweight per mapping in each scenario: two provider references to one detached HashMap node share its value,
     * which flyweights over a freed slot cannot, and the Map.Entry contract leaves that case undefined.
     */
    @Test
    void entriesDetachLikeHashMapNodesWhenTheirSlotIsReusedForTheSameKey() {
        sameScenario(() -> ORACLE.create(-1L), () -> fake(-1L), m -> {
            List<Object> out = new ArrayList<>();
            String key = "k";
            m.put(key, "1");
            M3PassiveExpiringOracle.MapCursor c = cursorOf(m);
            out.add(c.next());
            m.remove(key);
            m.put(key, "2");
            out.add(c.getValue());
            out.add(c.setValue("y"));
            out.add(c.getValue());
            out.add(m.get(key));
            out.add(run(c::remove));
            out.add(c.getKey());
            return out;
        });
        sameScenario(() -> ORACLE.create(-1L), () -> fake(-1L), m -> {
            List<Object> out = new ArrayList<>();
            String key = "k";
            m.put(key, "1");
            Map.Entry<Object, Object> e = m.entrySet().iterator().next();
            m.remove(key);
            m.put(key, "2");
            out.add(e.getValue());
            out.add(e.setValue("x"));
            out.add(e.getValue());
            out.add(m.get(key));
            Map.Entry<Object, Object> f = m.entrySet().iterator().next();
            m.put(key, "3");
            out.add(f.getValue());
            out.add(f.setValue("4"));
            out.add(m.get(key));
            m.clear();
            m.put(key, "5");
            out.add(f.getValue());
            out.add(f.setValue("6"));
            out.add(m.get(key));
            out.add(run(() -> m.replaceAll((k, v) -> {
                m.remove(k);
                m.put(k, "7");
                return "lost";
            })));
            out.add(m.get(key));
            out.add(run(() -> m.replaceAll((k, v) -> {
                m.put(k, "8");
                return v + "!";
            })));
            out.add(m.get(key));
            return out;
        });
        sameScenario(() -> ORACLE.create(5L), () -> fake(5L), m -> {
            List<Object> out = new ArrayList<>();
            m.put("t", "1");
            Map.Entry<Object, Object> e = m.entrySet().iterator().next();
            M3PassiveExpiringTestClock.advance(2);
            m.put("t", "2");
            out.add(e.getValue());
            out.add(e.setValue("3"));
            M3PassiveExpiringTestClock.advance(5);
            out.add(m.put("t", "4"));
            out.add(e.getValue());
            out.add(e.setValue("5"));
            out.add(m.get("t"));
            return out;
        });
    }

    @Test
    void wholeViewTraversalsAcrossEmptyMaskWordsAndDetachedSlotsWithoutReuse() {
        sameScenario(() -> ORACLE.create(1_000L), () -> fake(1_000L), m -> {
            List<Object> out = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                m.put(i, "v" + i);
            }
            for (int i = 10; i <= 150; i++) {
                m.remove(i);
            }
            out.add(sorted(m.keySet()));
            List<Object> seen = new ArrayList<>();
            m.keySet().forEach(seen::add);
            m.values().forEach(seen::add);
            m.entrySet().forEach(e -> seen.add(e.getKey() + "=" + e.getValue()));
            out.add(sorted(seen));
            Map.Entry<Object, Object> five = null;
            Map.Entry<Object, Object> last = null;
            for (Map.Entry<Object, Object> e : m.entrySet()) {
                five = Integer.valueOf(5).equals(e.getKey()) ? e : five;
                last = Integer.valueOf(199).equals(e.getKey()) ? e : last;
            }
            M3PassiveExpiringOracle.MapCursor c = cursorOf(m);
            Object key;
            do {
                key = c.next();
            } while (!Integer.valueOf(7).equals(key));
            m.remove(5);
            m.remove(7);
            out.add(five.getValue());
            out.add(five.setValue("w"));
            out.add(five.getValue());
            out.add(c.getKey());
            out.add(c.getValue());
            out.add(c.setValue("u"));
            out.add(c.getValue());
            out.add(m.containsKey(5));
            out.add(m.containsKey(7));
            m.clear();
            m.put("only", "1");
            out.add(last.getValue());
            out.add(last.setValue("z"));
            out.add(m.get("only"));
            out.add(m.size());
            return out;
        });
    }

    @Test
    void clockRedirectionRefusesClassesThatUseSystemOtherwise() throws IOException {
        assertEquals(2, ORACLE.patchedClasses());
        assertEquals(PassiveExpiringMap.class.getName(), ORACLE.mapClass().getName());
        assertTrue(ORACLE.mapClass() != PassiveExpiringMap.class, "the oracle class lives in its own loader");
        byte[] arraycopy = classBytes(M3PassiveExpiringMapContractTest.class);
        IllegalStateException other = assertThrows(IllegalStateException.class,
                () -> M3PassiveExpiringOracle.redirectClock(arraycopy));
        assertTrue(other.getMessage().contains("other than currentTimeMillis"), other.getMessage());
        byte[] noSystem = classBytes(M3ExpiryOrderIndexTest.class);
        IllegalStateException none = assertThrows(IllegalStateException.class,
                () -> M3PassiveExpiringOracle.redirectClock(noSystem));
        assertTrue(none.getMessage().contains("no java/lang/System"), none.getMessage());
        assertThrows(IllegalStateException.class, () -> M3PassiveExpiringOracle.redirectClock(new byte[16]));
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        try (InputStream in = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            assertTrue(in != null, type.getName());
            return in.readAllBytes();
        }
    }
}
