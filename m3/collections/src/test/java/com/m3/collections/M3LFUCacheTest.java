// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class M3LFUCacheTest {
    @Test
    void evictsLeastFrequentlyUsedAndBreaksTiesByLeastRecentTouch() {
        M3LFUCache<Object> cache = new M3LFUCache<>(2);
        Object frequent = new Object();
        Object cold = new Object();
        Object newest = new Object();

        cache.put(1, 7, frequent);
        cache.put(2, 7, cold);
        assertSame(frequent, cache.get(1, 7));
        cache.put(3, 7, newest);

        assertEquals(2, cache.size());
        assertEquals(2, cache.capacity());
        assertSame(frequent, cache.get(1, 7));
        assertNull(cache.get(1, 8), "a different map revision must never reuse an old facade");
        assertNull(cache.get(2, 7));
        assertSame(newest, cache.get(3, 7));
    }

    @Test
    void slotRemovalKeepsProbeChainsAndReleasesFacadeReferences() {
        M3LFUCache<Object> cache = new M3LFUCache<>(3);
        Object nullKeyValue = new Object();
        Object alpha = new Object();
        Object beta = new Object();
        cache.put(10, 1, nullKeyValue);
        cache.put(11, 1, alpha);
        cache.put(12, 1, beta);

        assertSame(nullKeyValue, cache.get(10, 1));
        assertTrue(cache.removeSlot(11));
        assertFalse(cache.removeSlot(50));
        assertSame(beta, cache.get(12, 1));
        assertSame(nullKeyValue, cache.get(10, 1));
        cache.removeSlot(10);
        assertNull(cache.get(10, 1));
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get(10, 1));
    }

    @Test
    void apiFacadeReuseNeverAliasesDifferentKeysAndSnapshotsStayDetached() {
        M3HashMap<String, Object> map = new M3HashMap<>();
        Object firstValue = new Object();
        Object replacementValue = new Object();
        map.put("first", firstValue);
        map.put("second", new Object());

        Map.Entry<String, Object> first = map.entrySet().iterator().next();
        assertSame(first, map.entrySet().iterator().next());
        var iterator = map.entrySet().iterator();
        Map.Entry<String, Object> one = iterator.next();
        Map.Entry<String, Object> two = iterator.next();
        assertNotSame(one, two, "different keys must never alias one cached facade");

        M3LinkedHashMap<String, Object> linked = new M3LinkedHashMap<>();
        linked.put("first", firstValue);
        Map.Entry<String, Object> snapshot = linked.firstEntry();
        assertSame(snapshot, linked.firstEntry());
        linked.put("first", replacementValue);
        Map.Entry<String, Object> refreshed = linked.firstEntry();
        assertNotSame(snapshot, refreshed);
        assertSame(firstValue, snapshot.getValue(), "old snapshot must remain detached");
        assertSame(replacementValue, refreshed.getValue());
    }
}
