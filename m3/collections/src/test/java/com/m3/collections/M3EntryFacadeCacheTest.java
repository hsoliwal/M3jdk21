/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * Modified 2026 by Hitesh Soliwal and contributors: qualify bounded API projection caching while retaining array-only structural storage and generic LFU contracts.
 * SPDX-License-Identifier: Apache-2.0
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
    void strongMapEntryFacadesReuseBoundedLiveCallerProjections() throws ReflectiveOperationException {
        M3LinkedHashMap<Object, Integer> map = new M3LinkedHashMap<>();
        Object key = new String("same");
        map.put(key, 1);
        map.put(new String("same"), 1);

        Map.Entry<Object, Integer> first = map.entrySet().iterator().next();
        Map.Entry<Object, Integer> second = map.entrySet().iterator().next();
        assertSame(first, second);
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
        assertSame(nullEntry, map.entrySet().iterator().next());
        assertNull(nullEntry.getKey());
        assertNull(nullEntry.getValue());
        assertNull(nullEntry.setValue(4));
        assertEquals(4, map.get(null));
        assertNoOwnedEntries(map);
    }

    @Test
    void treeMapReusesLiveFacadesAndKeepsDetachedImmutableSnapshots() throws ReflectiveOperationException {
        M3TreeMap<String, Integer> map = new M3TreeMap<>();
        map.put("one", 1);
        var view = map.subMap("one", true, "one", true).descendingMap();

        Map.Entry<String, Integer> first = view.entrySet().iterator().next();
        Map.Entry<String, Integer> second = view.entrySet().iterator().next();
        Map.Entry<String, Integer> endpoint = view.sequencedEntrySet().getFirst();
        assertSame(first, second);
        assertSame(first, endpoint);
        assertSame(endpoint, view.sequencedEntrySet().getFirst());

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
        assertNull(facadeCache(map), "weak-key owners must not retain a strong API cache");
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
        assertSame(first, map.entryFacade("one"));
        assertEquals(3, map.calls);

        RuntimeException marker = new IllegalStateException("projection callback");
        map.failure = marker;
        assertSame(marker, assertThrows(RuntimeException.class,
                () -> map.entrySet().iterator().next()));
        assertEquals(4, map.calls);
        assertEquals(1, map.get("one"));
    }

    @Test
    void falseAndThrowingHooksLeaveColdAndWarmLfuUntouched()
            throws ReflectiveOperationException {
        HookMap cold = new HookMap();
        cold.put("one", 1);
        cold.advisory = false;
        cold.forgetEntryFacade("one");
        cold.clearEntryFacades();
        assertNull(facadeCache(cold));
        Map.Entry<String, Integer> coldFirst = cold.entryFacade("one");
        assertNotSame(coldFirst, cold.entryFacade("one"));
        assertEquals(2, cold.calls);
        assertEquals(0, cold.revisionReads);
        assertEquals(0, cold.findCalls);
        assertEquals(0, cold.valueReads);
        assertNull(facadeCache(cold));
        RuntimeException marker = new IllegalStateException("projection callback");
        cold.failure = marker;
        assertSame(marker, assertThrows(RuntimeException.class, () -> cold.entryFacade("one")));
        assertEquals(3, cold.calls);
        assertEquals(0, cold.revisionReads);
        assertNull(facadeCache(cold));

        HookMap warm = new HookMap();
        warm.put("one", 1);
        Map.Entry<String, Integer> cached = warm.entryFacade("one");
        assertEquals(1, warm.calls);
        FacadeCacheState before = facadeCacheState(warm);
        int revisionsBeforeBypass = warm.revisionReads;
        warm.advisory = false;
        assertNotSame(cached, warm.entryFacade("one"));
        assertEquals(2, warm.calls);
        assertEquals(revisionsBeforeBypass, warm.revisionReads);
        assertUnchangedFacadeCache(before, warm);
        warm.advisory = true;
        warm.failure = marker;
        assertSame(marker, assertThrows(RuntimeException.class, () -> warm.entryFacade("one")));
        assertEquals(3, warm.calls);
        assertEquals(revisionsBeforeBypass, warm.revisionReads);
        assertUnchangedFacadeCache(before, warm);
        warm.failure = null;
        assertSame(cached, warm.entryFacade("one"));
        assertEquals(4, warm.calls);
        assertEquals(0, warm.findCalls);
        assertEquals(0, warm.valueReads);
    }

    @Test
    void backingHashMapRevisionInvalidatesBeforeReuseButValueWritesDoNot()
            throws ReflectiveOperationException {
        HookMap map = new HookMap();
        map.put("one", 1);
        Map.Entry<String, Integer> first = map.entryFacade("one");
        M3LFUCache<?, ?> cache = facadeCache(map);
        assertNotNull(cache);
        assertEquals(64, cache.capacity());
        assertEquals(1, cache.size());
        // Mutate the actual backing owner without invoking this wrapper's hooks.
        map.storage.put("one", 2);
        assertSame(first, map.entryFacade("one"));
        assertEquals(Integer.valueOf(2), first.getValue());
        map.storage.put("two", 20);
        Map.Entry<String, Integer> afterInsertion = map.entryFacade("one");
        assertNotSame(first, afterInsertion);
        assertSame(cache, facadeCache(map));
        assertEquals(1, cache.size());
        assertEquals(Integer.valueOf(2), afterInsertion.getValue());
        map.storage.remove("two");
        Map.Entry<String, Integer> afterUnrelatedRemoval = map.entryFacade("one");
        assertNotSame(afterInsertion, afterUnrelatedRemoval);
        assertSame(cache, facadeCache(map));
        assertEquals(1, cache.size());
        map.storage.remove("one");
        map.storage.put("one", 3);
        Map.Entry<String, Integer> afterReinsertion = map.entryFacade("one");
        assertNotSame(afterUnrelatedRemoval, afterReinsertion);
        assertEquals(Integer.valueOf(3), afterReinsertion.getValue());
        map.storage.clear();
        map.storage.put("one", 4);
        Map.Entry<String, Integer> afterBackingClear = map.entryFacade("one");
        assertNotSame(afterReinsertion, afterBackingClear);
        assertSame(cache, facadeCache(map));
        assertEquals(1, cache.size());
        assertEquals(Integer.valueOf(4), afterBackingClear.getValue());
    }

    @Test
    void entryProjectionDemandAvoidsSlotLookupAndValueRead() {
        HookMap map = new HookMap();
        String stored = new String("same");
        map.put(stored, 1);
        map.put(new String("same"), 2);
        assertEquals(0, map.findCalls);
        assertEquals(0, map.valueReads);
        Map.Entry<String, Integer> first = map.entrySet().iterator().next();
        Map.Entry<String, Integer> second = map.entrySet().iterator().next();
        assertSame(first, second);
        assertSame(stored, first.getKey());
        assertSame(first, map.entryFacade(stored));
        assertEquals(3, map.calls);
        assertEquals(0, map.findCalls, "admission must not look up the stored key again");
        assertEquals(0, map.valueReads, "admission/getKey must not request the value");
    }

    @Test
    void boundedEvictionDoesNotRetargetEscapedProjectionOrItsOwner()
            throws ReflectiveOperationException {
        M3HashMap<Object, Integer> map = new M3HashMap<>();
        Object[] keys = new Object[65];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = new Object();
            map.put(keys[i], i);
        }
        map.verifyInvariants();
        Map.Entry<Object, Integer> hot = map.entryFacade(keys[0]);
        assertSame(hot, map.entryFacade(keys[0]));
        Map.Entry<Object, Integer> escaped = map.entryFacade(keys[1]);
        for (int i = 2; i < 64; i++) map.entryFacade(keys[i]);
        M3LFUCache<?, ?> cache = facadeCache(map);
        assertNotNull(cache);
        assertEquals(64, cache.capacity());
        assertEquals(64, cache.size());
        // Every cold projection has frequency1; key1 is the oldest cold one.
        map.entryFacade(keys[64]);
        assertEquals(64, cache.size());
        assertSame(hot, map.entryFacade(keys[0]));
        Map.Entry<Object, Integer> replacement = map.entryFacade(keys[1]);
        assertNotSame(escaped, replacement);
        assertEquals(64, cache.size());
        M3HashMap<Object, Integer> other = new M3HashMap<>();
        other.put(keys[1], 201);
        Map.Entry<Object, Integer> otherOwner = other.entryFacade(keys[1]);
        assertNotSame(escaped, otherOwner);
        assertNotSame(replacement, otherOwner);
        assertSame(keys[1], escaped.getKey());
        assertEquals(Integer.valueOf(1), escaped.getValue());
        assertEquals(Integer.valueOf(1), escaped.setValue(101));
        assertEquals(Integer.valueOf(101), map.get(keys[1]));
        assertEquals(Integer.valueOf(101), replacement.getValue());
        assertEquals(Integer.valueOf(201), otherOwner.getValue());
        assertSame(keys[0], hot.getKey());
        assertEquals(Integer.valueOf(0), hot.getValue());
        map.verifyInvariants();
        other.verifyInvariants();
    }

    @Test
    void graphGuardRejectsAliasesOutsideTheExactApiCacheEdge()
            throws ReflectiveOperationException {
        M3HashMap<String, Integer> map = new M3HashMap<>();
        map.put("one", 1);
        Map.Entry<String, Integer> entry = map.entryFacade("one");
        M3LFUCache<?, ?> cache = facadeCache(map);
        assertNoOwnedEntries(map);
        // First visit the legitimate owner, then expose the same objects through an illicit edge.
        assertThrows(AssertionError.class,
                () -> assertNoOwnedEntries(new Object[] {map, cache}));
        assertThrows(AssertionError.class,
                () -> assertNoOwnedEntries(new Object[] {map,
                        facadeField(M3LFUCache.class, cache, "values")}));
        assertThrows(AssertionError.class,
                () -> assertNoOwnedEntries(new Object[] {map, entry}));
    }

    private record FacadeCacheState(
            M3LFUCache<?, ?> cache, Object[] keys, Object[] values, int[] frequencies,
            long[] accessStamps, byte[] states, int size, long clock) { }

    private static Object facadeField(Class<?> declaringType, Object owner, String name)
            throws ReflectiveOperationException {
        Field field = declaringType.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static M3LFUCache<?, ?> facadeCache(M3SlotMap<?, ?> owner)
            throws ReflectiveOperationException {
        return (M3LFUCache<?, ?>) facadeField(M3SlotMap.class, owner, "entryFacades");
    }

    private static FacadeCacheState facadeCacheState(M3SlotMap<?, ?> owner)
            throws ReflectiveOperationException {
        M3LFUCache<?, ?> cache = facadeCache(owner);
        assertNotNull(cache);
        return new FacadeCacheState(cache,
                ((Object[]) facadeField(M3LFUCache.class, cache, "keys")).clone(),
                ((Object[]) facadeField(M3LFUCache.class, cache, "values")).clone(),
                ((int[]) facadeField(M3LFUCache.class, cache, "frequencies")).clone(),
                ((long[]) facadeField(M3LFUCache.class, cache, "accessStamps")).clone(),
                ((byte[]) facadeField(M3LFUCache.class, cache, "states")).clone(),
                (Integer) facadeField(M3LFUCache.class, cache, "size"),
                (Long) facadeField(M3LFUCache.class, cache, "clock"));
    }

    private static void assertUnchangedFacadeCache(FacadeCacheState before, M3SlotMap<?, ?> owner)
            throws ReflectiveOperationException {
        FacadeCacheState after = facadeCacheState(owner);
        assertSame(before.cache(), after.cache());
        assertEquals(before.keys().length, after.keys().length);
        assertEquals(before.values().length, after.values().length);
        // Compare references without invoking arbitrary key equals or LiveEntry.getValue.
        for (int i = 0; i < before.keys().length; i++) assertSame(before.keys()[i], after.keys()[i]);
        for (int i = 0; i < before.values().length; i++) assertSame(before.values()[i], after.values()[i]);
        assertArrayEquals(before.frequencies(), after.frequencies());
        assertArrayEquals(before.accessStamps(), after.accessStamps());
        assertArrayEquals(before.states(), after.states());
        assertEquals(before.size(), after.size());
        assertEquals(before.clock(), after.clock());
    }

    private static final class HookMap extends M3SlotMap<String, Integer> {
        private final M3HashMap<String, Integer> storage = new M3HashMap<>();
        @Override int firstSlot() { return storage.firstSlot(); }
        @Override int nextSlot(int slot) { return storage.nextSlot(slot); }
        @Override int findSlot(Object key) { findCalls++; return storage.findSlot(key); }
        @Override String keyAt(int slot) { return storage.keyAt(slot); }
        @Override Integer valueAt(int slot) { valueReads++; return storage.valueAt(slot); }
        @Override Integer writeAt(int slot, Integer value) { return storage.writeAt(slot, value); }
        @Override Integer eraseAt(int slot) { return storage.eraseAt(slot); }
        @Override int revision() { revisionReads++; return storage.revision(); }
        @Override public int size() { return storage.size(); }
        @Override public Integer put(String key, Integer value) { return storage.put(key, value); }
        int calls;
        int findCalls;
        int valueReads;
        int revisionReads;
        boolean advisory = true;
        RuntimeException failure;

        @Override protected boolean cacheEntryFacades() {
            calls++;
            if (failure != null) throw failure;
            return advisory;
        }
    }

    private static void assertApiEntryCache(
            M3SlotMap<?, ?> owner, Object retained, IdentityHashMap<Object, Boolean> seen)
            throws ReflectiveOperationException {
        if (retained == null) return;
        assertSame(M3LFUCache.class, retained.getClass());
        M3LFUCache<?, ?> cache = (M3LFUCache<?, ?>) retained;
        assertEquals(64, cache.capacity());
        assertTrue(cache.size() >= 0 && cache.size() <= 64);
        Object[] keys = (Object[]) exactCacheLane(cache, "keys", Object[].class);
        Object[] values = (Object[]) exactCacheLane(cache, "values", Object[].class);
        int[] frequencies = (int[]) exactCacheLane(cache, "frequencies", int[].class);
        long[] accessStamps = (long[]) exactCacheLane(cache, "accessStamps", long[].class);
        byte[] states = (byte[]) exactCacheLane(cache, "states", byte[].class);
        Class<?> entryType = M3SlotMap.LiveEntry.class;
        Field key = entryType.getDeclaredField("key");
        Field enclosing = entryType.getDeclaredField("this$0");
        assertTrue(Modifier.isFinal(key.getModifiers()));
        assertTrue(Modifier.isFinal(enclosing.getModifiers()));
        assertTrue(enclosing.isSynthetic());
        assertSame(Object.class, key.getType());
        assertSame(M3SlotMap.class, enclosing.getType());
        key.setAccessible(true);
        enclosing.setAccessible(true);
        int occupied = 0;
        IdentityHashMap<Object, Boolean> identities = new IdentityHashMap<>();
        for (int slot = 0; slot < states.length; slot++) {
            assertTrue(states[slot] == 0 || states[slot] == 1);
            if (states[slot] == 0) {
                assertNull(keys[slot]);
                assertNull(values[slot]);
                assertEquals(0, frequencies[slot]);
                assertEquals(0L, accessStamps[slot]);
            } else {
                occupied++;
                assertNotNull(values[slot]);
                assertSame(entryType, values[slot].getClass());
                assertSame(keys[slot], key.get(values[slot]));
                assertSame(owner, enclosing.get(values[slot]));
                assertNull(identities.put(keys[slot], Boolean.TRUE), "duplicate cache key identity");
                assertTrue(frequencies[slot] >= 1);
                assertTrue(accessStamps[slot] >= 0L, "clock rebasing permits zero stamps");
            }
        }
        assertEquals(occupied, cache.size());
        // Exempt this one values edge, never these objects globally: aliases must still fail.
        for (Field field : M3LFUCache.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()
                    || field.getName().equals("values")) continue;
            field.setAccessible(true);
            assertNoOwnedEntries(field.get(cache), seen);
        }
    }

    private static Object exactCacheLane(M3LFUCache<?, ?> cache, String name, Class<?> type)
            throws ReflectiveOperationException {
        Field field = M3LFUCache.class.getDeclaredField(name);
        assertSame(type, field.getType());
        assertTrue(Modifier.isFinal(field.getModifiers()));
        field.setAccessible(true);
        Object lane = field.get(cache);
        assertNotNull(lane);
        assertEquals(64, java.lang.reflect.Array.getLength(lane));
        return lane;
    }

    static void assertNoOwnedEntries(Object owner) throws ReflectiveOperationException {
        assertNoOwnedEntries(owner, new IdentityHashMap<>());
    }

    private static void assertNoOwnedEntries(Object value, IdentityHashMap<Object, Boolean> seen)
            throws ReflectiveOperationException {
        if (value == null || seen.put(value, Boolean.TRUE) != null) return;
        assertFalse(value instanceof Map.Entry<?, ?>,
                "map-owned object graph retained a caller Entry projection");
        Class<?> type = value.getClass();
        assertFalse(type.getName().startsWith("com.m3.collections.")
                && M3NoRetainedStructuralEntryTest.isStructuralCarrier(type),
                "map-owned object graph retained a structural carrier");
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
                Object retained = field.get(value);
                if (field.getDeclaringClass() == M3SlotMap.class
                        && field.getName().equals("entryFacades")
                        && field.getType() == M3LFUCache.class) {
                    assertApiEntryCache((M3SlotMap<?, ?>) value, retained, seen);
                } else {
                    assertNoOwnedEntries(retained, seen);
                }
            }
        }
    }
}
