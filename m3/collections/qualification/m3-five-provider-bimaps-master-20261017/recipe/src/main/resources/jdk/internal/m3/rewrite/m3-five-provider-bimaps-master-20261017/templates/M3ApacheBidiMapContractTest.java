/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.junit.jupiter.api.Test;

/** Real Apache Commons Collections 4.5.0 binary/provider differential. */
final class M3ApacheBidiMapContractTest {
    @Test void namedAdapterAndInverse() {
        BidiMap<String, String> map = M3LibraryCollections.apacheBidiMap();
        assertInstanceOf(M3ApacheBidiMap.class, map);
        assertSame(map, map.inverseBidiMap().inverseBidiMap());
        map.put("a", "one");
        map.put("b", "two");
        assertEquals("a", map.getKey("one"));
        assertEquals("b", map.removeValue("two"));
        assertFalse(map.containsKey("b"));
        map.inverseBidiMap().put("three", "c");
        assertEquals("three", map.get("c"));
        assertEquals(2, map.values().size());
        var iterator = map.mapIterator();
        assertTrue(iterator.hasNext());
        String key = iterator.next();
        assertEquals(map.get(key), iterator.getValue());
    }

    @Test void realApacheDualLinkedHashBidiMapTrace() {
        BidiMap<Integer, Integer> actual = M3LibraryCollections.apacheBidiMap();
        BidiMap<Integer, Integer> expected = new DualLinkedHashBidiMap<>();
        Random random = new Random(202610172030L);
        for (int step = 0; step < 4_000; step++) {
            Integer key = random.nextInt(23) == 0 ? null : random.nextInt(53) - 26;
            Integer value = random.nextInt(23) == 0 ? null : random.nextInt(53) - 26;
            switch (random.nextInt(6)) {
                case 0, 1 -> assertEquals(expected.put(key, value), actual.put(key, value));
                case 2 -> assertEquals(expected.remove(key), actual.remove(key));
                case 3 -> assertEquals(expected.removeValue(value), actual.removeValue(value));
                case 4 -> {
                    assertEquals(expected.inverseBidiMap().put(value, key),
                            actual.inverseBidiMap().put(value, key));
                }
                default -> {
                    if (random.nextInt(29) == 0) { expected.clear(); actual.clear(); }
                    else assertEquals(expected.get(key), actual.get(key));
                }
            }
            assertEquals(expected, actual, "forward at step " + step);
            assertEquals(expected.inverseBidiMap(), actual.inverseBidiMap(), "inverse at step " + step);
            assertEquals(expected.size(), actual.size(), "size at step " + step);
            assertEquals(expected.values().size(), actual.values().size(), "value view at step " + step);
            assertEquals(expected.hashCode(), actual.hashCode(), "hash at step " + step);
            assertSame(actual, actual.inverseBidiMap().inverseBidiMap());
        }
        var pairs = new ArrayList<>(actual.entrySet());
        for (Map.Entry<Integer, Integer> pair : pairs) {
            assertEquals(pair.getKey(), actual.getKey(pair.getValue()));
        }
    }
}
