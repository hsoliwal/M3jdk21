// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Mechanical LEAN guard: logical collection entries are not retained as structural objects.
 *
 * <p>Canonical packed storage remains payload lanes plus primitive slot/index/topology state.
 * The incumbent bounded M3SlotMap.entryFacades cache holds only requested live API projections;
 * detached firstEntry/navigation snapshots remain fresh. Cache bounds and lifecycle are checked
 * separately from the canonical topology traversal.</p>
 */
final class M3NoRetainedStructuralEntryTest {
    private static final String PACKAGE = "com.m3.collections.";

    private static final List<Class<?>> STORAGE_ROOTS = List.of(
            M3LinkedHashMap.class,
            M3TreeMap.class,
            M3LinkedList.class,
            M3WeakHashMap.class,
            M3BlockingDeque.class,
            M3TransferQueue.class);

    @Test
    void storageRootsAndNestedOwnersHaveNoRetainedEntryNodeBucketOrCellFields() {
        for (Class<?> root : STORAGE_ROOTS) {
            inspectFieldsRecursively(root);
        }
    }

    @Test
    void onlySlotMapDefinesTheExplicitMapEntryCompatibilityProjection() {
        List<String> entryImplementations = new ArrayList<>();
        collectEntryImplementations(M3SlotMap.class, entryImplementations);
        collectEntryImplementations(M3OrderedSlotMap.class, entryImplementations);
        for (Class<?> root : STORAGE_ROOTS) {
            collectEntryImplementations(root, entryImplementations);
        }
        entryImplementations.sort(String::compareTo);
        assertEquals(
                List.of(M3SlotMap.class.getName() + "$LiveEntry"),
                entryImplementations,
                "new collection-owned Map.Entry implementations require explicit API-boundary review");
    }

    @Test
    void liveEntryFacadesAreBoundedOutsideCanonicalStorageAndSnapshotsStayFresh()
            throws ReflectiveOperationException {
        var hash = new M3LinkedHashMap<Integer, Integer>();
        hash.put(1, 10);
        hash.put(2, 20);
        Map.Entry<Integer, Integer> hashEntry = hash.entrySet().iterator().next();
        Map.Entry<Integer, Integer> hashEntryAgain = hash.entrySet().iterator().next();
        assertSame(
                hashEntry, hashEntryAgain,
                "resident hash entry facade is reused");
        assertFalse(
                retainsIdentity(hash, hashEntry),
                "PackedHashMap retained an entry API projection as canonical storage");
        assertFalse(
                retainsIdentity(hash, hashEntryAgain),
                "PackedHashMap retained a repeated entry API projection");

        var tree = new M3TreeMap<Integer, Integer>();
        tree.put(1, 10);
        tree.put(2, 20);
        Map.Entry<Integer, Integer> treeEntry = tree.entrySet().iterator().next();
        Map.Entry<Integer, Integer> treeEntryAgain = tree.entrySet().iterator().next();
        assertSame(
                treeEntry, treeEntryAgain,
                "resident tree entry facade is reused");
        assertFalse(
                retainsIdentity(tree, treeEntry),
                "PackedTreeMap retained an entry API projection as canonical storage");
        assertFalse(
                retainsIdentity(tree, treeEntryAgain),
                "PackedTreeMap retained a repeated entry API projection");

        hashEntry.setValue(11);
        treeEntry.setValue(11);
        assertEquals(11, hash.get(hashEntry.getKey()));
        assertEquals(11, tree.get(treeEntry.getKey()));

        assertFreshBurst(hash::firstEntry, "hash firstEntry");
        assertFreshBurst(tree::firstEntry, "tree firstEntry");
        assertFreshBurst(() -> tree.floorEntry(1), "tree floorEntry snapshot");
        assertCachedBurst(
                () -> hash.entrySet().iterator().next(),
                "hash entrySet iterator projection");
        assertCachedBurst(
                () -> tree.entrySet().iterator().next(),
                "tree entrySet iterator projection");
        assertBoundedFacadeCache(new M3LinkedHashMap<>(), "hash facade cache");
        assertBoundedFacadeCache(new M3TreeMap<>(), "tree facade cache");
    }

    private static void assertCachedBurst(
            Supplier<? extends Map.Entry<?, ?>> supplier, String label) {
        Map.Entry<?, ?> resident = supplier.get();
        for (int iteration = 0; iteration < 1_024; iteration++) {
            assertSame(resident, supplier.get(), label + " must reuse the resident facade");
        }
    }

    private static void assertBoundedFacadeCache(M3SlotMap<Integer, Integer> map,
            String label) throws ReflectiveOperationException {
        Field cacheField = M3SlotMap.class.getDeclaredField("entryFacades");
        assertTrue(cacheField.trySetAccessible(), "cannot inspect explicit facade cache");
        assertNull(cacheField.get(map), label + " is lazy");
        for (int key = 0; key < 129; key++) map.put(key, key);
        assertNull(cacheField.get(map), label + " is not canonical mapping storage");
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            assertEquals((Object) entry.getKey(), entry.getValue(), label + " live value");
            assertFalse(retainsIdentity(map, entry), label + " leaked into canonical topology");
        }
        M3LFUCache<?, ?> cache = (M3LFUCache<?, ?>) cacheField.get(map);
        assertEquals(64, cache.capacity(), label + " incumbent capacity");
        assertEquals(64, cache.size(), label + " remains bounded after 129 projections");
        Map.Entry<Integer, Integer> resident = map.entrySet().iterator().next();
        Integer key = resident.getKey();
        assertSame(resident, map.entryFacade(key), label + " identity-keyed hit");
        int cached = cache.size();
        map.remove(key);
        assertEquals(cached - 1, cache.size(), label + " removal invalidates resident facade");
        map.put(key, -1);
        assertNotSame(resident, map.entryFacade(key), label + " reinsertion creates a new facade");
        assertEquals(64, cache.size(), label + " reinsertion preserves bound");
        map.clear();
        assertEquals(0, cache.size(), label + " old cache is cleared");
        assertNull(cacheField.get(map), label + " owner releases cleared cache");
    }

    private static void assertFreshBurst(
            Supplier<? extends Map.Entry<?, ?>> supplier, String label) {
        Object[] seen = new Object[1_024];
        for (int iteration = 0; iteration < seen.length; iteration++) {
            Map.Entry<?, ?> current = supplier.get();
            for (int prior = 0; prior < iteration; prior++) {
                assertNotSame(
                        seen[prior],
                        current,
                        label + " reused/pool-cycled an Entry identity at " + iteration);
            }
            seen[iteration] = current;
        }
    }

    private static void inspectFieldsRecursively(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            Class<?> retained = field.getType();
            Class<?> component = retained.isArray() ? retained.getComponentType() : retained;
            assertFalse(
                    Map.Entry.class.isAssignableFrom(component),
                    () -> type.getName() + " retains Map.Entry field " + field.getName()
                            + (Modifier.isStatic(field.getModifiers()) ? " (static cache/pool)" : ""));
            assertFalse(
                    isStructuralCarrier(component),
                    () -> type.getName() + " retains structural object field "
                            + field.getName() + ":" + component.getName());
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            inspectFieldsRecursively(nested);
        }
    }

    private static boolean isStructuralCarrier(Class<?> type) {
        if (type.isPrimitive()) {
            return false;
        }
        String simple = type.getSimpleName();
        return simple.equals("Node")
                || simple.endsWith("Node")
                || simple.equals("Entry")
                || simple.endsWith("Entry")
                || simple.equals("Bucket")
                || simple.endsWith("Bucket")
                || simple.equals("Cell")
                || simple.endsWith("Cell");
    }

    private static void collectEntryImplementations(Class<?> type, List<String> result) {
        if (type != Map.Entry.class && Map.Entry.class.isAssignableFrom(type)) {
            result.add(type.getName());
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            collectEntryImplementations(nested, result);
        }
    }

    private static boolean retainsIdentity(Object owner, Object target) {
        return retainsIdentity(owner, target, new ArrayList<>());
    }

    private static boolean retainsIdentity(Object value, Object target, List<Object> seen) {
        if (value == null) {
            return false;
        }
        if (value == target) {
            return true;
        }
        if (seenIdentity(seen, value)) {
            return false;
        }
        seen.add(value);

        Class<?> type = value.getClass();
        if (type.isArray()) {
            if (type.getComponentType().isPrimitive()) {
                return false;
            }
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                Object element = Array.get(value, index);
                if (element == target) {
                    return true;
                }
                if (element != null
                        && (element.getClass().isArray()
                            || element.getClass().getName().startsWith(PACKAGE))
                        && retainsIdentity(element, target, seen)) {
                    return true;
                }
            }
            return false;
        }
        if (!type.getName().startsWith(PACKAGE)) {
            return false;
        }

        for (Class<?> declaring = type;
                declaring != null && declaring.getName().startsWith(PACKAGE);
                declaring = declaring.getSuperclass()) {
            for (Field field : declaring.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }
                // Exclude only the explicitly qualified API cache, including when
                // inherited. All other M3 fields and reference arrays remain traversed.
                if (field.getDeclaringClass() == M3SlotMap.class
                        && field.getName().equals("entryFacades")) {
                    continue;
                }
                assertTrue(field.trySetAccessible(),
                        () -> "cannot inspect " + field.getDeclaringClass().getName() + "." + field.getName());
                try {
                    Object retained = field.get(value);
                    if (retained == target || retainsIdentity(retained, target, seen)) {
                        return true;
                    }
                } catch (IllegalAccessException impossible) {
                    throw new AssertionError(impossible);
                }
            }
        }
        return false;
    }

    private static boolean seenIdentity(List<Object> seen, Object candidate) {
        for (Object value : seen) {
            if (value == candidate) {
                return true;
            }
        }
        return false;
    }
}
