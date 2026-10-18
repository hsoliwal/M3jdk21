/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class M3EntryFacadeCacheTest {
    @Test
    void lfuPrefersHotEntriesAndBreaksTiesByRecency() {
        M3LFUCache<Object, Object> cache = new M3LFUCache<>(2);
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        Object aFacade = new Object();
        Object bFacade = new Object();
        Object cFacade = new Object();

        assertSame(aFacade, cache.getOrCreate(a, ignored -> aFacade));
        assertSame(bFacade, cache.getOrCreate(b, ignored -> bFacade));
        assertSame(aFacade, cache.getOrCreate(a, ignored -> new Object()));
        assertSame(cFacade, cache.getOrCreate(c, ignored -> cFacade));

        // A has a greater frequency, so B is evicted when C enters.
        assertSame(aFacade, cache.getOrCreate(a, ignored -> new Object()));
        assertSame(cFacade, cache.getOrCreate(c, ignored -> new Object()));
        assertEquals(2, cache.size());
        assertEquals(2, cache.capacity());

        Object replacementB = new Object();
        assertSame(replacementB, cache.getOrCreate(b, ignored -> replacementB));
        assertEquals(2, cache.size());
    }

    @Test
    void identityKeyingAllowsNullAndEqualButDistinctKeys() {
        M3LFUCache<Object, Object> cache = new M3LFUCache<>(4);
        String first = new String("same");
        String second = new String("same");
        Object firstValue = new Object();
        Object secondValue = new Object();
        Object nullValue = new Object();

        assertSame(firstValue, cache.getOrCreate(first, ignored -> firstValue));
        assertSame(secondValue, cache.getOrCreate(second, ignored -> secondValue));
        assertNotSame(firstValue, cache.getOrCreate(second, ignored -> new Object()));
        assertSame(nullValue, cache.getOrCreate(null, ignored -> nullValue));
        assertSame(nullValue, cache.getOrCreate(null, ignored -> new Object()));
        assertEquals(3, cache.size());
        cache.removeIdentity(first);
        assertSame(secondValue, cache.getOrCreate(second, ignored -> new Object()));
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void strongMapEntryFacadesAreReusedUntilEvictionOrMappingRemoval() {
        M3LinkedHashMap<Object, Integer> map = new M3LinkedHashMap<>();
        Object key = new Object();
        map.put(key, 1);

        Map.Entry<Object, Integer> first = map.entrySet().iterator().next();
        Map.Entry<Object, Integer> second = map.entrySet().iterator().next();
        assertSame(first, second);
        assertSame(key, second.getKey());
        assertEquals(1, second.getValue());
        assertEquals(1, second.setValue(2));
        assertEquals(2, map.get(key));

        assertEquals(2, map.remove(key));
        map.put(key, 3);
        Map.Entry<Object, Integer> afterReinsert = map.entrySet().iterator().next();
        assertNotSame(first, afterReinsert);
        assertEquals(3, afterReinsert.getValue());
    }

    @Test
    void weakKeyMapBypassesStrongFacadeCache() throws ReflectiveOperationException {
        M3WeakHashMap<Object, Integer> map = new M3WeakHashMap<>();
        Object key = new Object();
        map.put(key, 1);
        assertEquals(1, map.entrySet().iterator().next().getValue());

        Field cache = M3SlotMap.class.getDeclaredField("entryFacades");
        cache.setAccessible(true);
        assertNull(cache.get(map),
                "a strong facade cache would keep weak keys alive and change map semantics");
    }
}
