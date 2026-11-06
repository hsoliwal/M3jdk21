/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Actual Guava 33.4.8-jre API/JAR differential, not a lookalike interface stub. */
final class M3GuavaBiMapContractTest {
    @Test void typedApiAndLiveInverse() {
        BiMap<String, String> map = M3LibraryCollections.guavaBiMap();
        assertInstanceOf(M3GuavaBiMap.class, map);
        assertSame(map, map.inverse().inverse());
        map.put(null, null);
        assertEquals(null, map.inverse().get(null));
        assertThrows(IllegalArgumentException.class, () -> map.put("different", null));
        map.forcePut("different", null);
        assertFalse(map.containsKey(null));
        assertTrue(map.inverse().containsKey(null));
        map.inverse().put("two", "other");
        assertEquals("two", map.get("other"));
        assertTrue(map.values().remove("two"));
        assertFalse(map.containsKey("other"));
        map.remove("different");
        map.put("x", "1");
        var it = map.entrySet().iterator();
        var item = it.next();
        assertEquals("1", item.setValue("2"));
        it.remove();
        assertFalse(map.inverse().containsKey("2"));
        assertTrue(map.isEmpty());
    }

    @Test void actualGuavaHashBiMapRandomizedContract() {
        BiMap<Integer, Integer> actual = M3LibraryCollections.guavaBiMap();
        BiMap<Integer, Integer> expected = HashBiMap.create();
        Random random = new Random(202610171845L);
        for (int step = 0; step < 4000; step++) {
            Integer key = random.nextInt(17) == 0 ? null : random.nextInt(33) - 16;
            Integer value = random.nextInt(17) == 0 ? null : random.nextInt(33) - 16;
            switch (random.nextInt(7)) {
                case 0, 1 -> {
                    Throwable expectedFailure = null;
                    Integer before = null;
                    try { before = expected.put(key, value); }
                    catch (IllegalArgumentException failure) { expectedFailure = failure; }
                    if (expectedFailure == null) assertEquals(before, actual.put(key, value));
                    else assertThrows(IllegalArgumentException.class, () -> actual.put(key, value));
                }
                case 2 -> assertEquals(expected.forcePut(key, value), actual.forcePut(key, value));
                case 3 -> assertEquals(expected.remove(key), actual.remove(key));
                case 4 -> assertEquals(expected.keySet().remove(key), actual.keySet().remove(key));
                case 5 -> assertEquals(expected.values().remove(value), actual.values().remove(value));
                default -> {
                    // No iteration-order requirement between Guava and the M3 index owner.
                    if (expected.isEmpty()) {
                        assertEquals(expected.inverse().put(value, key), actual.inverse().put(value, key));
                    } else if (step % 19 == 0) {
                        expected.clear(); actual.clear();
                    } else {
                        assertEquals(expected.get(key), actual.get(key));
                    }
                }
            }
            assertEquals(expected, actual, "mapping step " + step);
            assertEquals(expected.inverse(), actual.inverse(), "inverse step " + step);
            assertEquals(expected.hashCode(), actual.hashCode(), "hash step " + step);
            assertEquals(expected.size(), actual.values().size(), "values size step " + step);
            assertTrue(actual.values().containsAll(new ArrayList<>(expected.values())));
            assertSame(actual, actual.inverse().inverse());
        }
    }
}
