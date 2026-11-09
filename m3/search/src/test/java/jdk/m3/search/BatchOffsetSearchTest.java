// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3BatchOffsetSearchTest.java (blob ad6992b4f22d) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent public contract oracle; native parity has a separately required executable gate. */
final class BatchOffsetSearchTest {
    private static int scalar(byte[] haystack, byte[] needle, int from) {
        for (int at = Math.max(0, from); at <= haystack.length - needle.length; at++) {
            if (Arrays.equals(haystack, at, at + needle.length, needle, 0, needle.length)) return at;
        }
        return -1;
    }

    @Test
    void javaBatchedQueriesPreserveSourceAndFirstHitSemantics() {
        Random random = new Random(0xBADB_EEF1_2026L);
        for (int trial = 0; trial < 4096; trial++) {
            int n = random.nextInt(257);
            int m = 1 + random.nextInt(73);
            int q = random.nextInt(42);
            byte[] text = new byte[n], needle = new byte[m];
            int alphabet = trial % 3 == 0 ? 256 : trial % 3 == 1 ? 2 : 11;
            for (int i = 0; i < n; i++) text[i] = (byte) random.nextInt(alphabet);
            for (int i = 0; i < m; i++) needle[i] = (byte) random.nextInt(alphabet);
            if (m <= n && trial % 4 == 0) {
                System.arraycopy(text, random.nextInt(n - m + 1), needle, 0, m);
            }
            int[] starts = new int[q], expected = new int[q];
            for (int i = 0; i < q; i++) {
                starts[i] = switch (i % 6) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> -1;
                    case 3 -> n;
                    default -> random.nextInt(n + 17) - 8;
                };
                expected[i] = scalar(text, needle, starts[i]);
            }
            byte[] textSnapshot = text.clone(), needleSnapshot = needle.clone();
            int[] startsSnapshot = starts.clone();
            assertArrayEquals(expected, BatchOffsetSearch.indexOfMany(text, needle, starts));
            assertArrayEquals(expected, BatchOffsetSearch.indexOfMany(
                    text, needle, starts, BatchOffsetSearch.Backend.JAVA));
            assertArrayEquals(textSnapshot, text);
            assertArrayEquals(needleSnapshot, needle);
            assertArrayEquals(startsSnapshot, starts);
        }
    }

    @Test
    void nullableAndEmptyNeedlesFailClosed() {
        assertThrows(NullPointerException.class,
                () -> BatchOffsetSearch.indexOfMany((byte[]) null, new byte[]{1}, new int[0]));
        assertThrows(NullPointerException.class,
                () -> BatchOffsetSearch.indexOfMany(new byte[0], null, new int[0]));
        assertThrows(NullPointerException.class,
                () -> BatchOffsetSearch.indexOfMany(new byte[0], new byte[]{1}, null));
        assertThrows(NullPointerException.class,
                () -> BatchOffsetSearch.indexOfMany(
                        new byte[0], new byte[]{1}, new int[0], null));
        assertThrows(IllegalArgumentException.class,
                () -> BatchOffsetSearch.indexOfMany(new byte[0], new byte[0], new int[0]));
    }

    @Test
    void duplicatesAndOverlapsAreStable() {
        byte[] text = "aaaaa".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] needle = "aa".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int[] starts = {3, -1, 1, 2, 0, 99, 1};
        assertArrayEquals(new int[]{3, 0, 1, 2, 0, -1, 1},
                BatchOffsetSearch.indexOfMany(text, needle, starts));
    }
}
