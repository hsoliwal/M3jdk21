// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The LFU is an API projection cache; canonical map state never lives in it. */
final class M3LFUCacheTest {
    @Test
    void cacheIsParallelArraysNotNodesOrStoredMapEntries() {
        for (Field field : M3LFUCache.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) { continue; }
            assertTrue(field.getType().isPrimitive() || field.getType().isArray(),
                    () -> "cache metadata must use lanes: " + field.getName());
        }
    }

    @Test
    void materializeOnlyOnMissAndEvictLowestFrequency() {
        M3LFUCache<StringBuilder> cache = new M3LFUCache<>(2);
        AtomicInteger builds = new AtomicInteger();
        StringBuilder one = cache.getOrCreate(10, 1,
                slot -> { builds.incrementAndGet(); return new StringBuilder("one"); });
        assertSame(one, cache.getOrCreate(10, 1, slot -> {
            throw new AssertionError("unexpected entry allocation on cache hit");
        }));
        cache.getOrCreate(20, 1,
                slot -> { builds.incrementAndGet(); return new StringBuilder("two"); });
        cache.getOrCreate(30, 1,
                slot -> { builds.incrementAndGet(); return new StringBuilder("three"); });
        assertEquals(2, cache.size());
        assertEquals(3, builds.get());
        assertSame(one, cache.getOrCreate(10, 1, slot -> {
            throw new AssertionError("more frequently used entry was evicted");
        }));
        cache.trimTo(0);
        assertEquals(0, cache.size());
        assertEquals("one", one.toString());
    }

    @Test
    void generationReuseDoesNotModifyEscapedProjection() {
        M3LFUCache<StringBuilder> cache = new M3LFUCache<>(4);
        StringBuilder old = cache.getOrCreate(5, 1, slot -> new StringBuilder("old"));
        StringBuilder newer = cache.getOrCreate(5, 2, slot -> new StringBuilder("new"));
        assertNotSame(old, newer);
        assertEquals("old", old.toString());
        assertEquals(1, cache.size());
        cache.invalidate(5);
        assertEquals(0, cache.size());
        assertEquals("new", newer.toString());
    }

    @Test
    void failedProjectionCreationPreservesOldCacheEntries() {
        M3LFUCache<Object> cache = new M3LFUCache<>(1);
        Object original = cache.getOrCreate(1, 1, slot -> new Object());
        assertThrows(IllegalStateException.class,
                () -> cache.getOrCreate(2, 1, slot -> { throw new IllegalStateException(); }));
        assertSame(original, cache.getOrCreate(1, 1, slot -> {
            throw new AssertionError("failure discarded live projection");
        }));
    }

    @Test
    void zeroCapacityAndArgumentChecksDoNotRetainProjections() {
        M3LFUCache<Object> cache = new M3LFUCache<>(0);
        Object first = cache.getOrCreate(0, 0, slot -> new Object());
        Object second = cache.getOrCreate(0, 0, slot -> new Object());
        assertNotSame(first, second);
        assertEquals(0, cache.size());
        assertThrows(IllegalArgumentException.class,
                () -> cache.getOrCreate(-1, 0, slot -> new Object()));
        assertThrows(IllegalArgumentException.class, () -> cache.trimTo(-1));
    }
}
