// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3BatchOffsetDirectTest.java (blob a8edc03f8ea1) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Java21 public-API byte-buffer window oracle. Native exact-source parity is separately gated. */
final class M3BatchOffsetDirectTest {
    private static int first(byte[] text, byte[] pattern, int from) {
        for (int at = Math.max(0, from); at <= text.length - pattern.length; ++at) {
            if (Arrays.equals(text, at, at + pattern.length,
                    pattern, 0, pattern.length)) return at;
        }
        return -1;
    }

    private static ByteBuffer window(byte[] text, boolean direct, boolean readonly) {
        ByteBuffer value = direct ? ByteBuffer.allocateDirect(text.length + 6)
                : ByteBuffer.allocate(text.length + 6);
        value.position(2).put(text).limit(text.length + 2);
        value.position(2);
        if (readonly) value = value.asReadOnlyBuffer();
        return value.mark();
    }

    @Test
    void heapDirectAndReadOnlyWindowsPreservePositionLimitAndMark() {
        Random random = new Random(0x6A21_2026);
        for (int trial = 0; trial < 4096; ++trial) {
            int n = random.nextInt(196);
            int m = 1 + random.nextInt(91);
            int q = random.nextInt(65);
            int alphabet = trial % 4 == 0 ? 256 : trial % 4 == 1 ? 1 : 9;
            byte[] text = new byte[n], pattern = new byte[m];
            for (int i = 0; i < n; ++i) text[i] = (byte) random.nextInt(alphabet);
            for (int i = 0; i < m; ++i) pattern[i] = (byte) random.nextInt(alphabet);
            if (m <= n && trial % 3 == 0) {
                System.arraycopy(text, random.nextInt(n - m + 1), pattern, 0, m);
            }
            int[] offsets = new int[q], expected = new int[q];
            for (int i = 0; i < q; ++i) {
                offsets[i] = switch (i % 6) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> -1;
                    default -> random.nextInt(2 * n + 11) - n;
                };
                expected[i] = first(text, pattern, offsets[i]);
            }
            for (int mode = 0; mode < 4; ++mode) {
                ByteBuffer buffer = window(text, mode >= 2, (mode & 1) != 0);
                assertArrayEquals(expected, M3BatchOffsetSearch.indexOfMany(buffer, pattern, offsets));
                assertArrayEquals(expected, M3BatchOffsetSearch.indexOfMany(
                        buffer, pattern, offsets, M3BatchOffsetSearch.Backend.JAVA));
                assertEquals(2, buffer.position());
                assertEquals(n + 2, buffer.limit());
                assertEquals(2, buffer.reset().position());
            }
        }
    }

    @Test
    void duplicatesOverlapsAndNativePolicyAreStable() {
        byte[] text = {1, 1, 1, 1, 1};
        byte[] needle = {1, 1};
        int[] starts = {3, -1, 1, 2, 0, 99, 1};
        int[] expected = {3, 0, 1, 2, 0, -1, 1};
        for (int mode = 0; mode < 4; ++mode) {
            ByteBuffer buffer = window(text, mode >= 2, (mode & 1) != 0);
            assertArrayEquals(expected, M3BatchOffsetSearch.indexOfMany(
                    buffer, needle, starts, M3BatchOffsetSearch.Backend.JAVA));
        }
        assertThrows(IllegalArgumentException.class, () -> M3BatchOffsetSearch.indexOfMany(
                window(text, false, false), needle, starts, M3BatchOffsetSearch.Backend.NATIVE));
    }

    @Test
    void nullAndEmptyInputsRejectWithoutMutatingCallerBuffer() {
        ByteBuffer buffer = window(new byte[]{1}, true, true);
        assertThrows(NullPointerException.class,
                () -> M3BatchOffsetSearch.indexOfMany((ByteBuffer) null, new byte[]{1}, new int[0]));
        assertThrows(NullPointerException.class,
                () -> M3BatchOffsetSearch.indexOfMany(buffer, null, new int[0]));
        assertThrows(NullPointerException.class,
                () -> M3BatchOffsetSearch.indexOfMany(buffer, new byte[]{1}, null));
        assertThrows(NullPointerException.class,
                () -> M3BatchOffsetSearch.indexOfMany(
                        buffer, new byte[]{1}, new int[0], null));
        assertThrows(IllegalArgumentException.class,
                () -> M3BatchOffsetSearch.indexOfMany(buffer, new byte[0], new int[0]));
        assertEquals(2, buffer.reset().position());
    }
}
