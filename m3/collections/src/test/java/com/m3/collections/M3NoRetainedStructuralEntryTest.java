// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
 * <p>Map.Entry is permitted only as a transient Java API projection. Canonical packed storage must
 * remain payload lanes plus primitive slot/index/topology state.</p>
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
    void returnedMapEntriesAreFreshAndNeverRetainedByPackedMapStorage() {
        var hash = new M3LinkedHashMap<Integer, Integer>();
        hash.put(1, 10);
        hash.put(2, 20);
        Map.Entry<Integer, Integer> hashEntry = hash.entrySet().iterator().next();
        Map.Entry<Integer, Integer> hashEntryAgain = hash.entrySet().iterator().next();
        assertFalse(
                hashEntry == hashEntryAgain,
                "PackedHashMap reused/cached an API Entry object");
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
        assertFalse(
                treeEntry == treeEntryAgain,
                "PackedTreeMap reused/cached an API Entry object");
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
                        && element.getClass().getName().startsWith(PACKAGE)
                        && retainsIdentity(element, target, seen)) {
                    return true;
                }
            }
            return false;
        }
        if (!type.getName().startsWith(PACKAGE)) {
            return false;
        }

        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                continue;
            }
            assertTrue(field.trySetAccessible(), () -> "cannot inspect " + type.getName() + "." + field.getName());
            try {
                Object retained = field.get(value);
                if (retained == target || retainsIdentity(retained, target, seen)) {
                    return true;
                }
            } catch (IllegalAccessException impossible) {
                throw new AssertionError(impossible);
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
