// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: align API projection identity checks with the existing bounded facade cache.
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: verify fresh caller-owned live entries, inherited owner nonretention and detached immutable snapshots.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * <p>Live Map.Entry projections are fresh and caller-owned; no map or inherited owner retains
 * their identity. Canonical packed storage remains payload lanes plus primitive slot/index/topology
 * state. Navigation snapshots are fresh, detached and immutable.</p>
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
    void liveEntriesAreFreshAndUnretainedWhileSnapshotsRemainDetached() {
        var hash = new M3LinkedHashMap<Integer, Integer>();
        hash.put(1, 10);
        hash.put(2, 20);
        Map.Entry<Integer, Integer> hashEntry = hash.entrySet().iterator().next();
        Map.Entry<Integer, Integer> hashEntryAgain = hash.entrySet().iterator().next();
        assertNotSame(hashEntry, hashEntryAgain, "hash entries are fresh caller projections");
        assertEquals(hashEntry, hashEntryAgain, "fresh entries describe the same mapping");
        assertFalse(retainsIdentity(hash, hashEntry), "hash owner retained a live entry");
        assertFalse(retainsIdentity(hash, hashEntryAgain), "hash owner retained a repeated entry");
        Map.Entry<Integer, Integer> hashSnapshot = hash.firstEntry();
        Object hashStorage = canonicalStorage(hash, "table");
        assertFalse(
                retainsIdentity(hashStorage, hashEntry),
                "PackedHashMap retained an entry API projection as canonical storage");
        assertFalse(
                retainsIdentity(hashStorage, hashEntryAgain),
                "PackedHashMap retained a repeated entry API projection");

        var tree = new M3TreeMap<Integer, Integer>();
        tree.put(1, 10);
        tree.put(2, 20);
        Map.Entry<Integer, Integer> treeEntry = tree.entrySet().iterator().next();
        Map.Entry<Integer, Integer> treeEntryAgain = tree.entrySet().iterator().next();
        assertNotSame(treeEntry, treeEntryAgain, "tree entries are fresh caller projections");
        assertEquals(treeEntry, treeEntryAgain, "fresh entries describe the same mapping");
        assertFalse(retainsIdentity(tree, treeEntry), "tree owner retained a live entry");
        assertFalse(retainsIdentity(tree, treeEntryAgain), "tree owner retained a repeated entry");
        Map.Entry<Integer, Integer> treeSnapshot = tree.firstEntry();
        Object treeStorage = canonicalStorage(tree, "tree");
        assertFalse(
                retainsIdentity(treeStorage, treeEntry),
                "PackedTreeMap retained an entry API projection as canonical storage");
        assertFalse(
                retainsIdentity(treeStorage, treeEntryAgain),
                "PackedTreeMap retained a repeated entry API projection");

        assertEquals(10, hashEntry.setValue(11));
        assertEquals(10, treeEntry.setValue(11));
        assertEquals(11, hashEntryAgain.getValue());
        assertEquals(11, treeEntryAgain.getValue());
        assertEquals(11, hashEntryAgain.setValue(12));
        assertEquals(11, treeEntryAgain.setValue(12));
        assertEquals(12, hashEntry.getValue());
        assertEquals(12, treeEntry.getValue());
        assertEquals(12, hash.get(hashEntry.getKey()));
        assertEquals(12, tree.get(treeEntry.getKey()));
        assertEquals(10, hashSnapshot.getValue(), "hash navigation snapshot is detached");
        assertEquals(10, treeSnapshot.getValue(), "tree navigation snapshot is detached");
        assertThrows(UnsupportedOperationException.class, () -> hashSnapshot.setValue(13));
        assertThrows(UnsupportedOperationException.class, () -> treeSnapshot.setValue(13));

        assertFreshBurst(hash::firstEntry, "hash firstEntry");
        assertFreshBurst(tree::firstEntry, "tree firstEntry");
        assertFreshBurst(
                () -> hash.entrySet().iterator().next(),
                "hash entrySet iterator projection");
        assertFreshBurst(
                () -> tree.entrySet().iterator().next(),
                "tree entrySet iterator projection");
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

    private static Object canonicalStorage(Object map, String fieldName) {
        try {
            Field storage = map.getClass().getDeclaredField(fieldName);
            assertTrue(storage.trySetAccessible(), () -> "cannot inspect canonical " + fieldName);
            return storage.get(map);
        } catch (ReflectiveOperationException impossible) {
            throw new AssertionError(impossible);
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
                if (retainsIdentity(element, target, seen)) {
                    return true;
                }
            }
            return false;
        }
        if (!type.getName().startsWith(PACKAGE)) {
            return false;
        }

        // A cache on M3SlotMap is retained by every concrete map as inherited state.
        for (Class<?> ownerType = type; ownerType != null
                && ownerType.getName().startsWith(PACKAGE); ownerType = ownerType.getSuperclass()) {
            for (Field field : ownerType.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }
                assertTrue(field.trySetAccessible(), () -> "cannot inspect "
                        + field.getDeclaringClass().getName() + "." + field.getName());
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
