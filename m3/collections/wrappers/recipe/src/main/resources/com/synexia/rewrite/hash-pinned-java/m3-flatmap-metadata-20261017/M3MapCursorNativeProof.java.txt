// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verification-only JNI projection boundary; Java owns predicates and entries.
 * A single checked native call receives a detached primitive shadow, not Java
 * Entry objects, owner pointers, or callback handles.
 */
public final class M3MapCursorNativeProof {
    private static int checks;

    private M3MapCursorNativeProof() { }

    private static native long fold(int[] indices);

    private static long javaFold(int[] values) {
        long state = 0xcbf29ce484222325L;
        for (int value : values) {
            state ^= Integer.toUnsignedLong(value);
            state *= 0x100000001b3L;
        }
        return state;
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) {
        String library = System.getProperty("m3.cursor.native.library");
        if (library == null || library.isBlank()) {
            throw new IllegalStateException("absolute m3.cursor.native.library path required");
        }
        System.load(Path.of(library).toAbsolutePath().normalize().toString());

        for (int size : new int[] {0, 1, 2, 3, 255, 256, 257, 1023, 1024}) {
            Map<Integer, Integer> backing = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                int key = i == 0 ? Integer.MIN_VALUE :
                        i == 1 ? Integer.MAX_VALUE : i - 500;
                backing.put(key, i % 7 == 0 ? null : i);
            }
            Map<Integer, Integer> filtered = M3FilteredMaps.filtered(
                    backing, (key, value) -> (key & 1) == 0 || value == null);
            int[] projected = filtered.keySet().stream().mapToInt(Integer::intValue).toArray();
            check(fold(projected) == javaFold(projected), "Java/JNI primitive shadow parity");
            check(fold(projected) == fold(Arrays.copyOf(projected, projected.length)),
                    "JNI call did not retain caller array");
            Map<Integer, Integer> checkedReadOnly = M3CollectionWrappers.readOnly(
                    M3CollectionWrappers.checked(filtered, Integer.class, Integer.class));
            int[] purposeShadow = checkedReadOnly.keySet().stream()
                    .mapToInt(Integer::intValue).toArray();
            check(fold(purposeShadow) == javaFold(purposeShadow),
                    "purpose-composed map JNI primitive shadow parity");
            // Native sees only a detached primitive shadow, never the flatMapper.
            java.util.Collection<Integer> flattened = M3CollectionWrappers.flatMappedView(
                    java.util.List.of(projected, purposeShadow),
                    keys -> java.util.Arrays.stream(keys).boxed().toList());
            int[] flattenedIndices = flattened.stream().mapToInt(Integer::intValue).toArray();
            check(fold(flattenedIndices) == javaFold(flattenedIndices),
                    "lazy flatMap read-only projection JNI primitive shadow parity");
            // Reuse this native peer for the new fused M3 snapshot API:
            // predicates, sorting and reference projection stay in Java.
            java.util.List<Integer> frozen = M3CollectionViews.filteredSortedSnapshot(
                    backing.keySet(), key -> (key & 1) == 0,
                    java.util.Comparator.naturalOrder());
            int[] frozenIds = frozen.stream().mapToInt(Integer::intValue).toArray();
            check(fold(frozenIds) == javaFold(frozenIds),
                    "fused filtered/sorted snapshot JNI parity");
            if (size > 0) {
                backing.put(Integer.MAX_VALUE - size, size);
                int[] newer = filtered.keySet().stream().mapToInt(Integer::intValue).toArray();
                check(fold(newer) == javaFold(newer), "live source plus new native shadow");
            }
        }
        try {
            fold(null);
            throw new AssertionError("null native shadow accepted");
        } catch (NullPointerException expected) {
            checks++;
        }
        System.out.println("M3_MAP_CURSOR_JNI_PASS checks=" + checks);
    }
}
