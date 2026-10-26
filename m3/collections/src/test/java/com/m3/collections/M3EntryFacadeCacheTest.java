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
/*
 * Modified 2026 by Hitesh Soliwal and contributors: reconcile map projection tests with the no-retained-entry invariant; preserve generic LFU contracts.
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
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
    void strongMapEntryFacadesAreFreshLiveCallerProjections() throws ReflectiveOperationException {
        M3LinkedHashMap<Object, Integer> map = new M3LinkedHashMap<>();
        Object key = new String("same");
        map.put(key, 1);
        map.put(new String("same"), 1);

        Map.Entry<Object, Integer> first = map.entrySet().iterator().next();
        Map.Entry<Object, Integer> second = map.entrySet().iterator().next();
        assertNotSame(first, second);
        assertSame(key, second.getKey(), "equal lookup keys must preserve the stored key identity");
        assertEquals(1, second.getValue());
        assertEquals(1, second.setValue(2));
        assertEquals(2, first.getValue());
        assertEquals(2, map.get(key));
        assertNoOwnedEntries(map);

        assertEquals(2, map.remove(key));
        assertNull(first.getValue());
        assertThrows(IllegalStateException.class, () -> first.setValue(9));
        map.put(key, 3);
        Map.Entry<Object, Integer> afterReinsert = map.entrySet().iterator().next();
        assertNotSame(first, afterReinsert);
        assertNotSame(second, afterReinsert);
        assertEquals(3, afterReinsert.getValue());

        map.clear();
        map.put(null, null);
        Map.Entry<Object, Integer> nullEntry = map.entrySet().iterator().next();
        assertNotSame(nullEntry, map.entrySet().iterator().next());
        assertNull(nullEntry.getKey());
        assertNull(nullEntry.getValue());
        assertNull(nullEntry.setValue(4));
        assertEquals(4, map.get(null));
        assertNoOwnedEntries(map);
    }

    @Test
    void treeMapKeepsFreshLiveFacadesAndDetachedImmutableSnapshots() throws ReflectiveOperationException {
        M3TreeMap<String, Integer> map = new M3TreeMap<>();
        map.put("one", 1);
        var view = map.subMap("one", true, "one", true).descendingMap();

        Map.Entry<String, Integer> first = view.entrySet().iterator().next();
        Map.Entry<String, Integer> second = view.entrySet().iterator().next();
        Map.Entry<String, Integer> endpoint = view.sequencedEntrySet().getFirst();
        assertNotSame(first, second);
        assertNotSame(first, endpoint);
        assertNotSame(endpoint, view.sequencedEntrySet().getFirst());

        Map.Entry<String, Integer> snapshot = map.firstEntry();
        assertNotSame(snapshot, map.firstEntry());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.setValue(7));
        map.put("one", 2);
        assertEquals(1, snapshot.getValue(), "firstEntry must remain a detached immutable snapshot");
        assertEquals(2, first.getValue(), "each caller projection resolves the live mapping");
        assertEquals(2, endpoint.setValue(3));
        assertEquals(3, second.getValue());
        assertEquals(3, map.get("one"));
        assertNoOwnedEntries(map);
        assertNoOwnedEntries(view);
    }

    @Test
    void weakKeyMapHasFreshProjectionsWithoutStrongOwnerRetention() throws ReflectiveOperationException {
        M3WeakHashMap<Object, Integer> map = new M3WeakHashMap<>();
        Object key = new Object();
        map.put(key, 1);
        Map.Entry<Object, Integer> first = map.entrySet().iterator().next();
        Map.Entry<Object, Integer> second = map.entrySet().iterator().next();
        assertNotSame(first, second);
        assertSame(key, first.getKey());
        assertEquals(1, second.getValue());
        assertEquals(1, first.setValue(2));
        assertEquals(2, second.getValue());
        assertNoOwnedEntries(map);
    }

    @Test
    void compatibilityHookKeepsCallbackDemandAndExceptionIdentity() {
        HookMap map = new HookMap();
        map.put("one", 1);
        Map.Entry<String, Integer> first = map.entrySet().iterator().next();
        assertEquals(1, map.calls);
        map.advisory = false;
        assertNotSame(first, map.entrySet().iterator().next());
        assertEquals(2, map.calls);
        map.advisory = true;
        assertNotSame(first, map.entryFacade("one"));
        assertEquals(3, map.calls);

        RuntimeException marker = new IllegalStateException("projection callback");
        map.failure = marker;
        assertSame(marker, assertThrows(RuntimeException.class,
                () -> map.entrySet().iterator().next()));
        assertEquals(4, map.calls);
        assertEquals(1, map.get("one"));
    }

    private static final class HookMap extends M3SlotMap<String, Integer> {
        private final M3LinkedHashMap<String, Integer> storage = new M3LinkedHashMap<>();
        @Override int firstSlot() { return storage.firstSlot(); }
        @Override int nextSlot(int slot) { return storage.nextSlot(slot); }
        @Override int findSlot(Object key) { return storage.findSlot(key); }
        @Override String keyAt(int slot) { return storage.keyAt(slot); }
        @Override Integer valueAt(int slot) { return storage.valueAt(slot); }
        @Override Integer writeAt(int slot, Integer value) { return storage.writeAt(slot, value); }
        @Override Integer eraseAt(int slot) { return storage.eraseAt(slot); }
        @Override int revision() { return storage.revision(); }
        @Override public int size() { return storage.size(); }
        @Override public Integer put(String key, Integer value) { return storage.put(key, value); }
        int calls;
        boolean advisory = true;
        RuntimeException failure;

        @Override protected boolean cacheEntryFacades() {
            calls++;
            if (failure != null) throw failure;
            return advisory;
        }
    }

    private static void assertNoOwnedEntries(Object owner) throws ReflectiveOperationException {
        assertNoOwnedEntries(owner, new IdentityHashMap<>());
    }

    private static void assertNoOwnedEntries(Object value, IdentityHashMap<Object, Boolean> seen)
            throws ReflectiveOperationException {
        if (value == null || seen.put(value, Boolean.TRUE) != null) return;
        assertFalse(value instanceof Map.Entry<?, ?>,
                "map-owned object graph retained a caller Entry projection");
        Class<?> type = value.getClass();
        if (type.isArray()) {
            if (!type.getComponentType().isPrimitive()) {
                for (Object element : (Object[]) value) assertNoOwnedEntries(element, seen);
            }
            return;
        }
        // Include inherited owners: checking only the concrete map misses base-class caches.
        for (Class<?> owner = type; owner != null
                && owner.getName().startsWith("com.m3.collections."); owner = owner.getSuperclass()) {
            for (Field field : owner.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                field.setAccessible(true);
                assertNoOwnedEntries(field.get(value), seen);
            }
        }
    }
}
