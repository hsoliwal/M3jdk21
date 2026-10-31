// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;

class M3ArrayNativeTest {
    @Test
    void javaFallbackHasExactCompareAndMaterializationSemantics() {
        char[] left = "abcdef".toCharArray();
        char[] right = "abcxef".toCharArray();

        assertEquals(0, M3ArrayNative.compareUtf16(left, 0, left, 0, left.length));
        assertTrue(M3ArrayNative.compareUtf16(left, 0, right, 0, left.length) < 0);
        assertArrayEquals(
                "abcdef".toCharArray(),
                M3ArrayNative.concatenateUtf16("ab".toCharArray(), "cd".toCharArray(), "ef".toCharArray()));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> M3ArrayNative.compareUtf16(left, 2, right, 2, 10));
        assertThrows(
                NullPointerException.class,
                () -> M3ArrayNative.concatenateUtf16("a".toCharArray(), null));
    }

    @Test
    void optionalNativeProviderMatchesJavaAcrossChunkAndUnicodeBoundaries() {
        String path = System.getProperty("m3.arrays.native.path");
        if (path == null || path.isBlank()) return;

        M3ArrayNative.load(Path.of(path));
        Random random = new Random(0x5eed);
        for (int length : new int[] {0, 1, 1023, 1024, 1025, 4097}) {
            char[] left = new char[length];
            char[] right = new char[length];
            for (int index = 0; index < length; index++) {
                left[index] = (char) random.nextInt(65536);
                right[index] = left[index];
            }
            assertEquals(0, M3ArrayNative.compareUtf16(left, 0, right, 0, length));
            if (length > 0) {
                int changed = length / 2;
                right[changed] = (char) ((right[changed] + 1) & 0xffff);
                int expected = left[changed] - right[changed];
                assertEquals(expected, M3ArrayNative.compareUtf16(left, 0, right, 0, length));
            }
        }

        char[] emoji = "\ud83d\ude00".toCharArray();
        assertArrayEquals(
                "A\ud83d\ude00B".toCharArray(),
                M3ArrayNative.concatenateUtf16(new char[] {'A', emoji[0]}, new char[] {emoji[1], 'B'}));
    }
}
