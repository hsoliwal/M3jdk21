// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import jdk.m3.arrays.M3ArrayNative;
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
        Object[] first = M3ArrayNative.allocateObjectsRequired(7);
        assertArrayEquals(new Object[7], first);
        assertNotSame(first, M3ArrayNative.allocateObjectsRequired(7));
        assertTrue(M3ArrayNative.isLoaded(), "required JNI owner was not loaded");
        assertThrows(NegativeArraySizeException.class,
                () -> M3ArrayNative.allocateObjectsRequired(-1));
    }

    @Test
    void byteAllocationIsNativeAndZeroInitialized() {
        requireConfiguredNativeProvider();
        byte[] first = M3ArrayNative.allocateBytesRequired(9);
        assertArrayEquals(new byte[9], first);
        assertNotSame(first, M3ArrayNative.allocateBytesRequired(9));
        assertTrue(M3ArrayNative.isLoaded(), "required JNI owner was not loaded");
    }

    @Test
    void intAllocationIsNativeAndZeroInitialized() {
        requireConfiguredNativeProvider();
        int[] first = M3ArrayNative.allocateIntsRequired(11);
        assertArrayEquals(new int[11], first);
        assertNotSame(first, M3ArrayNative.allocateIntsRequired(11));
        assertTrue(M3ArrayNative.isLoaded(), "required JNI owner was not loaded");
    }

    @Test
    void longAllocationIsNativeAndZeroInitialized() {
        requireConfiguredNativeProvider();
        long[] first = M3ArrayNative.allocateLongsRequired(13);
        assertArrayEquals(new long[13], first);
        assertNotSame(first, M3ArrayNative.allocateLongsRequired(13));
        assertTrue(M3ArrayNative.isLoaded(), "required JNI owner was not loaded");
    }
}
