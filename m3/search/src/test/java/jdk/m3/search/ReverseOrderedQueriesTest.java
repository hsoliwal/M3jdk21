// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3ReverseOrderedQueriesTest.java (blob 85f633086c2f) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Actual product-owner contract oracle for monotonic, descending and equal-key bound lanes. */
final class ReverseOrderedQueriesTest {
    private static int scalar(byte[] text, byte[] needle, int bound) {
        for (int start = Math.min(bound, text.length - needle.length); start >= 0; start--) {
            if (Arrays.equals(text, start, start + needle.length, needle, 0, needle.length)) {
                return start;
            }
        }
        return -1;
    }

    private static void compareAll(byte[] text, byte[] needle, int[] bounds) {
        int[] expected = new int[bounds.length];
        for (int i = 0; i < bounds.length; i++) expected[i] = scalar(text, needle, bounds[i]);
        byte[] beforeText = text.clone(), beforeNeedle = needle.clone();
        int[] beforeBounds = bounds.clone();
        assertArrayEquals(expected, BatchReverseSearch.lastIndexOfMany(text, needle, bounds));
        assertArrayEquals(expected, BatchReverseSearch.lastIndexOfMany(
                text, needle, bounds, BatchOffsetSearch.Backend.JAVA));
        for (int kind = 0; kind < 4; kind++) {
            ByteBuffer view = kind > 1 ? ByteBuffer.allocateDirect(text.length + 8)
                    : ByteBuffer.allocate(text.length + 8);
            view.position(3).put(text).limit(text.length + 3);
            view.position(3).mark();
            if ((kind & 1) == 1) view = view.asReadOnlyBuffer();
            assertArrayEquals(expected, BatchReverseSearch.lastIndexOfMany(
                    view, needle, bounds, BatchOffsetSearch.Backend.JAVA));
            assertEquals(3, view.position());
            assertEquals(text.length + 3, view.limit());
            assertEquals(3, view.reset().position());
        }
        assertArrayEquals(beforeText, text);
        assertArrayEquals(beforeNeedle, needle);
        assertArrayEquals(beforeBounds, bounds);
    }

    @Test
    void ascendingDuplicatesAndSignedExtremesPreserveOrdinals() {
        byte[] text = "aaaaa".getBytes(StandardCharsets.US_ASCII);
        byte[] needle = "aa".getBytes(StandardCharsets.US_ASCII);
        int[] bounds = {Integer.MIN_VALUE, -1, -1, 0, 1, 2, 2, 3, 4, Integer.MAX_VALUE};
        int[] expected = {-1, -1, -1, 0, 1, 2, 2, 3, 3, 3};
        assertArrayEquals(expected, BatchReverseSearch.lastIndexOfMany(text, needle, bounds));
        compareAll(text, needle, bounds);
    }

    @Test
    void descendingAndAlternatingBoundsRetainOriginalSortFallback() {
        byte[] text = "abababab".getBytes(StandardCharsets.US_ASCII);
        byte[] needle = "aba".getBytes(StandardCharsets.US_ASCII);
        compareAll(text, needle, new int[] {100, 7, 6, 5, 4, 3, 2, 1, 0, -1, Integer.MIN_VALUE});
        compareAll(text, needle, new int[] {-1, 100, -2, 7, -3, 6, -4, 5, -5, 4});
        compareAll(text, needle, new int[0]);
    }

    @Test
    void mixedRandomizedSortedAndDescendingQueryShapes() {
        Random random = new Random(0x5EED_20261103L);
        for (int trial = 0; trial < 512; trial++) {
            int length = random.nextInt(141), needleSize = 1 + random.nextInt(53);
            byte[] text = new byte[length], needle = new byte[needleSize];
            int alphabet = trial % 3 == 0 ? 256 : trial % 3 == 1 ? 1 : 7;
            for (int i = 0; i < text.length; i++) text[i] = (byte)random.nextInt(alphabet);
            for (int i = 0; i < needle.length; i++) needle[i] = (byte)random.nextInt(alphabet);
            if (needleSize <= length && trial % 3 == 0) {
                System.arraycopy(text, random.nextInt(length - needleSize + 1),
                        needle, 0, needleSize);
            }
            int[] bounds = new int[random.nextInt(71)];
            for (int i = 0; i < bounds.length; i++) {
                bounds[i] = switch (i % 7) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> -1;
                    default -> random.nextInt(length * 2 + 5) - length;
                };
            }
            Arrays.sort(bounds);
            if ((trial & 1) != 0) {
                for (int i = 0, j = bounds.length - 1; i < j; i++, j--) {
                    int temp = bounds[i]; bounds[i] = bounds[j]; bounds[j] = temp;
                }
            }
            compareAll(text, needle, bounds);
        }
    }
}
