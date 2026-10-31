/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3Flat3Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Independent differential for the eight shared methods of the M3 small-map owner.
 *
 * <p>The default provider is the existing interop dependency. Qualification against
 * the account-owned source sets {@code m3.flat3map.providerClass} to
 * {@code com.m3.collections.apache.map.Flat3Map}. A missing provider fails the test.
 * The ordinary cases use stable, symmetric keys; they cover null mappings, equal
 * query keys, identity of returned values, collisions, growth, deletion and reuse.
 *
 * <p>This smaller M3 API does not supply the provider's views, iterators, clone or
 * serialization contract. Hash/equality callback counts and ordering are recorded
 * separately, never equated. No parity for throwing callbacks or mutable-hash keys
 * is inferred from these cases.
 */
final class M3Flat3MapProviderTest {
    private static final String PROVIDER_PROPERTY = "m3.flat3map.providerClass";
    private static final String DEFAULT_PROVIDER = "org.apache.commons.collections4.map.Flat3Map";

    private record Key(int id) {
        @Override public int hashCode() { return id & 3; }
    }

    private record Value(int id) { }

    @Test
    void nullMappingsAndThreeToFourGrowthPreserveReturnIdentity() {
        State state = new State(8);
        Object first = new Value(100);
        Object equalButDistinct = new Value(100);
        Object replacement = new Value(101);
        state.put(null, first);
        state.put(new Key(1), null);
        state.put(new Key(2), equalButDistinct);
        state.verify();
        assertEquals(3, state.actual.size());
        state.put(new Key(2), replacement);
        assertEquals(3, state.actual.size());
        state.put(new Key(3), first);
        state.verify();
        assertEquals(4, state.actual.size());
        assertTrue(state.actual.containsKey(new Key(1)));
        assertSame(null, state.actual.get(new Key(1)));
        state.remove(new Key(1));
        assertFalse(state.actual.containsKey(new Key(1)));
        state.remove(null);
        state.put(null, null);
        state.verify();
        state.clear();
        state.verify();
        state.put(new Key(4), first);
        state.verify();
    }

    @Test
    void collidingKeysAcrossWordBoundariesSurviveDeletionAndReuse() {
        State state = new State(140);
        for (int id = 0; id < 137; id++) {
            state.put(new Key(id), new Value(id));
            if (id == 2 || id == 3 || id == 31 || id == 32 || id == 63
                    || id == 64 || id == 127 || id == 128) state.verify();
        }
        state.put(null, new Value(-1));
        state.verify();
        for (int id = 0; id < 137; id += 3) state.remove(new Key(id));
        state.verify();
        for (int id = 0; id < 137; id += 3) state.put(new Key(id), new Value(-id));
        state.verify();
        for (int id = 136; id >= 0; id--) state.remove(new Key(id));
        state.remove(null);
        state.verify();
        assertTrue(state.actual.isEmpty());
        for (int id = 0; id < 7; id++) state.put(new Key(id), new Value(id));
        state.clear();
        state.put(null, new Value(99));
        state.verify();
    }

    @Test
    void seededEightMethodDifferentialMatchesProviderAndJdk() {
        for (int seed = 0; seed < 64; seed++) {
            State state = new State(129);
            Random random = new Random(0x3f1a7L + seed * 104729L);
            List<Object> values = new ArrayList<>();
            values.add(null);
            for (int id = 0; id < 24; id++) values.add(new Value(id));
            for (int step = 0; step < 1024; step++) {
                int id = random.nextInt(130) - 1;
                Object key = id < 0 ? null : new Key(id);
                Object value = values.get(random.nextInt(values.size()));
                switch (random.nextInt(10)) {
                    case 0, 1, 2 -> state.put(key, value);
                    case 3 -> state.remove(key);
                    case 4 -> state.get(key);
                    case 5 -> state.containsKey(key);
                    case 6 -> state.containsValue(value);
                    case 7 -> {
                        if ((step & 127) == 0) state.clear();
                        else state.put(key, value);
                    }
                    case 8 -> state.size();
                    case 9 -> state.isEmpty();
                    default -> throw new AssertionError("unreachable choice");
                }
                state.size();
                state.isEmpty();
                if ((step & 63) == 0) state.verify();
            }
            state.verify();
        }
    }

    @Test
    void equalValuesAndObservedHashCallbacksDoNotChangeSharedResults() {
        State state = new State(5);
        state.remove(null);
        state.remove(new Key(4));
        state.containsValue(null);
        state.put(new Key(1), new Value(11));
        state.put(new Key(2), new Value(11));
        state.containsValue(new Value(11));
        state.containsValue(new Value(12));
        state.remove(new Key(1));
        state.containsValue(new Value(11));
        state.remove(new Key(2));
        state.containsValue(new Value(11));
        state.verify();

        M3Flat3Map<Object, Object> actual = new M3Flat3Map<>();
        Map<Object, Object> provider = provider();
        int actualHashes = 0;
        int providerHashes = 0;
        for (int id = 0; id < 3; id++) {
            CountedKey key = new CountedKey(id);
            Object value = new Value(id);
            key.hashCalls = 0;
            assertSame(null, actual.put(key, value));
            actualHashes += key.hashCalls;
            key.hashCalls = 0;
            assertSame(null, provider.put(key, value));
            providerHashes += key.hashCalls;
            assertEquals(provider.size(), actual.size());
            assertSame(provider.get(key), actual.get(key));
        }
        System.out.println("M3Flat3Map callback observation: three inserts, m3.hashCode="
                + actualHashes + ", provider.hashCode=" + providerHashes
                + "; callback count/order parity is not asserted");
    }

    private static final class CountedKey {
        private final int id;
        private int hashCalls;
        CountedKey(int id) { this.id = id; }
        @Override public int hashCode() { hashCalls++; return id; }
        @Override public boolean equals(Object other) {
            return other instanceof CountedKey key && id == key.id;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> provider() {
        String type = System.getProperty(PROVIDER_PROPERTY, DEFAULT_PROVIDER);
        try {
            Object value = Class.forName(type).getConstructor().newInstance();
            if (!(value instanceof Map<?, ?>)) throw new AssertionError(type + " is not a Map");
            return (Map<Object, Object>) value;
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Required Flat3Map provider unavailable: " + type, error);
        }
    }

    private static final class State {
        private final M3Flat3Map<Object, Object> actual = new M3Flat3Map<>();
        private final Map<Object, Object> provider = provider();
        private final Map<Object, Object> expected = new HashMap<>();
        private final int keyCount;
        State(int keyCount) { this.keyCount = keyCount; }
        void put(Object key, Object value) {
            Object old = expected.put(key, value);
            assertSame(old, provider.put(key, value), "provider put return");
            assertSame(old, actual.put(key, value), "M3 put return");
        }
        void remove(Object key) {
            Object old = expected.remove(key);
            assertSame(old, provider.remove(key), "provider remove return");
            assertSame(old, actual.remove(key), "M3 remove return");
        }
        void get(Object key) {
            Object value = expected.get(key);
            assertSame(value, provider.get(key), "provider get");
            assertSame(value, actual.get(key), "M3 get");
        }
        void containsKey(Object key) {
            boolean present = expected.containsKey(key);
            assertEquals(present, provider.containsKey(key), "provider containsKey");
            assertEquals(present, actual.containsKey(key), "M3 containsKey");
        }
        void containsValue(Object value) {
            boolean present = expected.containsValue(value);
            assertEquals(present, provider.containsValue(value), "provider containsValue");
            assertEquals(present, actual.containsValue(value), "M3 containsValue");
        }
        void size() {
            assertEquals(expected.size(), provider.size(), "provider size");
            assertEquals(expected.size(), actual.size(), "M3 size");
        }
        void isEmpty() {
            assertEquals(expected.isEmpty(), provider.isEmpty(), "provider isEmpty");
            assertEquals(expected.isEmpty(), actual.isEmpty(), "M3 isEmpty");
        }
        void clear() {
            expected.clear();
            provider.clear();
            actual.clear();
        }
        void verify() {
            size();
            isEmpty();
            get(null);
            containsKey(null);
            containsValue(null);
            for (int id = 0; id < keyCount; id++) {
                Object key = new Key(id);
                get(key);
                containsKey(key);
            }
            for (Object value : expected.values()) containsValue(value);
            containsValue(new Value(Integer.MIN_VALUE));
        }
    }
}
