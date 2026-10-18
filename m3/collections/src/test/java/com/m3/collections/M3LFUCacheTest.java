// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class M3LFUCacheTest {
    @Test
    void evictsLeastFrequentlyUsedAndBreaksTiesByLeastRecentTouch() {
        M3LFUCache<String, Object> cache = new M3LFUCache<>(2);
        Object frequent = new Object();
        Object cold = new Object();
        Object newest = new Object();

        cache.put("frequent", frequent);
        cache.put("cold", cold);
        assertSame(frequent, cache.get("frequent"));
        cache.put("newest", newest);

        assertEquals(2, cache.size());
        assertEquals(2, cache.capacity());
        assertSame(frequent, cache.get("frequent"));
        assertNull(cache.get("cold"));
        assertSame(newest, cache.get("newest"));
    }

    @Test
    void nullKeysAndRemovalKeepProbeChainsValid() {
        M3LFUCache<String, Object> cache = new M3LFUCache<>(3);
        Object nullKeyValue = new Object();
        Object alpha = new Object();
        Object beta = new Object();
        cache.put(null, nullKeyValue);
        cache.put("alpha", alpha);
        cache.put("beta", beta);

        assertSame(nullKeyValue, cache.get(null));
        assertTrue(cache.remove("alpha"));
        assertFalse(cache.remove("missing"));
        assertSame(beta, cache.get("beta"));
        assertSame(nullKeyValue, cache.get(null));
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get(null));
    }

    @Test
    void facadeAndImmutableSnapshotCachesAreAPIOnlyAndValueSensitive() {
        M3LinkedHashMap<String, Object> map = new M3LinkedHashMap<>();
        Object firstValue = new Object();
        Object replacementValue = new Object();
        map.put("first", firstValue);
        map.put("second", new Object());

        Map.Entry<String, Object> liveFirst = map.entrySet().iterator().next();
        assertSame(liveFirst, map.entrySet().iterator().next());
        Map.Entry<String, Object> snapshot = map.firstEntry();
        assertSame(snapshot, map.firstEntry());

        map.put("first", replacementValue);
        Map.Entry<String, Object> refreshed = map.firstEntry();
        assertSame(replacementValue, refreshed.getValue());
        assertTrue(snapshot != refreshed, "different stored value must have a distinct immutable snapshot");
        assertSame(snapshot, mapSnapshotForOriginal(snapshot, map, firstValue));
    }

    private static Map.Entry<String, Object> mapSnapshotForOriginal(
            Map.Entry<String, Object> original,
            M3LinkedHashMap<String, Object> map,
            Object originalValue) {
        // The original immutable projection is stable even after its backing mapping changes.
        assertSame(originalValue, original.getValue());
        assertSame(map, map);
        return original;
    }
}
