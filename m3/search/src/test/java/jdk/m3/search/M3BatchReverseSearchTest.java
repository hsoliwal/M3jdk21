// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3BatchReverseSearchTest.java (blob c2b0ac1ae62d) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Exact scalar oracle for the new opt-in reverse batched-search API. */
final class M3BatchReverseSearchTest {
    private static int scalar(byte[] text, byte[] needle, int fromIndex) {
        for (int i = Math.min(fromIndex, text.length - needle.length); i >= 0; --i) {
            if (Arrays.equals(text, i, i + needle.length, needle, 0, needle.length)) return i;
        }
        return -1;
    }

    private static ByteBuffer window(byte[] text, boolean direct, boolean readOnly) {
        ByteBuffer buffer = direct ? ByteBuffer.allocateDirect(text.length + 6)
                : ByteBuffer.allocate(text.length + 6);
        buffer.position(2).put(text).limit(text.length + 2);
        buffer.position(2).mark();
        return readOnly ? buffer.asReadOnlyBuffer() : buffer;
    }

    @Test
    void JavaArrayAndFourByteBufferKindsPreserveReverseOracle() {
        Random random = new Random(0x5EED_2026_1103L);
        for (int trial = 0; trial < 4096; ++trial) {
            int n = random.nextInt(177);
            int m = 1 + random.nextInt(75);
            int q = random.nextInt(40);
            int alphabet = trial % 4 == 0 ? 256 : trial % 4 == 1 ? 2 : 13;
            byte[] text = new byte[n], needle = new byte[m];
            for (int i = 0; i < n; ++i) text[i] = (byte)random.nextInt(alphabet);
            for (int i = 0; i < m; ++i) needle[i] = (byte)random.nextInt(alphabet);
            if (m <= n && trial % 3 == 0) {
                System.arraycopy(text, random.nextInt(n - m + 1), needle, 0, m);
            }
            int[] starts = new int[q], oracle = new int[q];
            for (int i = 0; i < q; ++i) {
                starts[i] = switch (i % 7) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> -1;
                    case 3 -> n;
                    default -> random.nextInt(n * 2 + 19) - n;
                };
                oracle[i] = scalar(text, needle, starts[i]);
            }
            byte[] textBefore = text.clone(), needleBefore = needle.clone();
            int[] startsBefore = starts.clone();
            assertArrayEquals(oracle, M3BatchReverseSearch.lastIndexOfMany(text, needle, starts));
            assertArrayEquals(oracle, M3BatchReverseSearch.lastIndexOfMany(
                    text, needle, starts, M3BatchOffsetSearch.Backend.JAVA));
            for (int kind = 0; kind < 4; ++kind) {
                ByteBuffer view = window(text, kind >= 2, (kind & 1) == 1);
                assertArrayEquals(oracle, M3BatchReverseSearch.lastIndexOfMany(
                        view, needle, starts, M3BatchOffsetSearch.Backend.JAVA));
                assertArrayEquals(oracle, M3BatchReverseSearch.lastIndexOfMany(view, needle, starts));
                assertEquals(2, view.position());
                assertEquals(n + 2, view.limit());
                assertEquals(2, view.reset().position());
            }
            assertArrayEquals(textBefore, text);
            assertArrayEquals(needleBefore, needle);
            assertArrayEquals(startsBefore, starts);
        }
    }

    @Test
    void OverlappingReverseMatchesAndExtremeBounds() {
        byte[] text = {1, 1, 1, 1, 1};
        byte[] needle = {1, 1};
        int[] starts = {Integer.MIN_VALUE, -1, 0, 1, 2, 3, 4,
                Integer.MAX_VALUE, 2, 2, 3};
        int[] expected = {-1, -1, 0, 1, 2, 3, 3, 3, 2, 2, 3};
        assertArrayEquals(expected, M3BatchReverseSearch.lastIndexOfMany(text, needle, starts));
        assertArrayEquals(expected, M3BatchReverseSearch.lastIndexOfMany(
                window(text, true, true), needle, starts));
    }

    @Test
    void NullEmptyAndNativePolicyFailClosed() {
        byte[] needle = {1};
        int[] starts = {0};
        assertThrows(NullPointerException.class, () ->
                M3BatchReverseSearch.lastIndexOfMany((byte[])null, needle, starts));
        assertThrows(NullPointerException.class, () ->
                M3BatchReverseSearch.lastIndexOfMany(new byte[0], null, starts));
        assertThrows(NullPointerException.class, () ->
                M3BatchReverseSearch.lastIndexOfMany(new byte[0], needle, null));
        assertThrows(NullPointerException.class, () ->
                M3BatchReverseSearch.lastIndexOfMany(new byte[0], needle, starts, null));
        assertThrows(IllegalArgumentException.class, () ->
                M3BatchReverseSearch.lastIndexOfMany(new byte[0], new byte[0], starts));
        assertThrows(IllegalArgumentException.class, () ->
                M3BatchReverseSearch.lastIndexOfMany(window(new byte[]{1}, false, false),
                        needle, starts, M3BatchOffsetSearch.Backend.NATIVE));
    }
}
