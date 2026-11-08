// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import jdk.m3.arrays.ArrayNative;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each test exercises an independently owned, required-native array allocator.
 * The library is never mocked and missing native configuration fails closed.
 */
final class M3IndexedOccurrenceArrayNativeTest {
    private static void requireConfiguredNativeProvider() {
        String configured = System.getProperty("m3.arrays.native.path");
        assertNotNull(configured, "real m3-arrays JNI library path required");
        assertTrue(!configured.isBlank(), "blank JNI provider");
        assertTrue(Files.isRegularFile(Path.of(configured)), "native binary must exist");
    }

    @Test
    void objectAllocationIsNativeAndDetached() {
        requireConfiguredNativeProvider();
        Object[] first = ArrayNative.allocateObjectsRequired(7);
        assertArrayEquals(new Object[7], first);
        assertNotSame(first, ArrayNative.allocateObjectsRequired(7));
        assertTrue(ArrayNative.isLoaded(), "required JNI owner was not loaded");
        assertThrows(NegativeArraySizeException.class,
                () -> ArrayNative.allocateObjectsRequired(-1));
    }

    @Test
    void byteAllocationIsNativeAndZeroInitialized() {
        requireConfiguredNativeProvider();
        byte[] first = ArrayNative.allocateBytesRequired(9);
        assertArrayEquals(new byte[9], first);
        assertNotSame(first, ArrayNative.allocateBytesRequired(9));
        assertTrue(ArrayNative.isLoaded(), "required JNI owner was not loaded");
    }

    @Test
    void intAllocationIsNativeAndZeroInitialized() {
        requireConfiguredNativeProvider();
        int[] first = ArrayNative.allocateIntsRequired(11);
        assertArrayEquals(new int[11], first);
        assertNotSame(first, ArrayNative.allocateIntsRequired(11));
        assertTrue(ArrayNative.isLoaded(), "required JNI owner was not loaded");
    }

    @Test
    void longAllocationIsNativeAndZeroInitialized() {
        requireConfiguredNativeProvider();
        long[] first = ArrayNative.allocateLongsRequired(13);
        assertArrayEquals(new long[13], first);
        assertNotSame(first, ArrayNative.allocateLongsRequired(13));
        assertTrue(ArrayNative.isLoaded(), "required JNI owner was not loaded");
    }
}
