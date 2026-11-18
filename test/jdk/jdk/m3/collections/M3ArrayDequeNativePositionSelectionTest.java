/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Existing position-JNI authority, no duplicate native selector or object pins. */
class M3ArrayDequeNativePositionSelectionTest {
    private static final boolean NATIVE = Boolean.getBoolean("m3.test.native");

    @Test void alignedOffsetsAndMaskCombinationsUseExistingPositionOwner() {
        if (NATIVE) {
            assertTrue(IndexMaskCompactor.nativeAvailable(),
                    "native profile requires real m3_collections_mask JNI");
        }
        Random random = new Random(0x6a812517L);
        int[] masks = {0, 1, 2, 3, 4, 7, 8, 15, 16, 31, 32, 63, 64, 127, 128, 255};
        for (int cap = 0; cap <= 48; cap++) {
            var deque = new M3ArrayDeque<Integer>(cap);
            for (int i = 0; i < 90; i++) {
                deque.addLast(i);
                if ((i % 3) == 0) deque.pollFirst();
            }
            for (int i = 0; i < deque.size(); i++) {
                int bits = random.nextInt(128) << 1;
                deque.updateState(i, bits, 0);
            }
            for (int required : masks) for (int excluded : new int[]{0,4,8,32,128}) {
                // The public API rejects overlapping masks before JNI admission.
                if ((required & excluded) != 0) continue;
                byte[] admitted = new byte[deque.size()];
                for (int i = 0; i < admitted.length; i++) {
                    int flags = deque.stateAt(i);
                    admitted[i] = (byte) (((flags & required) == required
                            && (flags & excluded) == 0) ? 1 : 0);
                }
                int[] oldOwner = deque.selectedPhysicalIndices(required, excluded,
                        IndexMaskCompactor.Backend.JAVA);
                int[] logicalProjection = deque.selectedPhysicalIndicesFromAdmitted(
                        admitted, IndexMaskCompactor.Backend.JAVA);
                assertArrayEquals(oldOwner, logicalProjection,
                        "logical ordinals mapped to physical ring indices");
                if (NATIVE) {
                    assertArrayEquals(oldOwner, deque.selectedPhysicalIndices(
                            required, excluded, IndexMaskCompactor.Backend.NATIVE_REQUIRED),
                            "real JNI parity, no silent fallback");
                }
            }
        }
    }

    @Test void admissionRefusalDoesNotChangeParentState() {
        var deque = new M3ArrayDeque<String>(3);
        deque.addFirst("a");
        deque.addLast("b");
        int[] original = deque.selectedPhysicalIndices(1, 0, IndexMaskCompactor.Backend.JAVA);
        assertThrows(IllegalArgumentException.class,
                () -> deque.selectedPhysicalIndicesFromAdmitted(new byte[1],
                        IndexMaskCompactor.Backend.JAVA));
        assertThrows(NullPointerException.class,
                () -> deque.selectedPhysicalIndicesFromAdmitted(null,
                        IndexMaskCompactor.Backend.JAVA));
        assertEquals(2, deque.size());
        assertArrayEquals(original, deque.selectedPhysicalIndices(1, 0,
                IndexMaskCompactor.Backend.JAVA));
    }
}
