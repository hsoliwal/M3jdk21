// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BitapExactSearchTest.java (blob 6ec1155b979e) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent byte-by-byte oracle for the candidate; no adaptive dispatch changes. */
final class BitapExactSearchTest {
    @Test
    void rejectsInvalidPatternWithoutRetainingCallerStorage() {
        assertThrows(NullPointerException.class, () -> new M3BitapExactSearch(null));
        assertThrows(IllegalArgumentException.class, () -> new M3BitapExactSearch(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new M3BitapExactSearch(new byte[64]));
        byte[] needle = {1, 2, 1};
        M3BitapExactSearch compiled = new M3BitapExactSearch(needle);
        needle[0] = 99;
        assertEquals(0, compiled.indexOf(new byte[] {1, 2, 1, 4}, 0));
        assertEquals(3, compiled.needleLength());
        assertEquals("BITAP_EXACT_CANDIDATE", compiled.strategy());
        assertThrows(NullPointerException.class, () -> compiled.indexOf((byte[]) null, 0));
        assertThrows(NullPointerException.class, () -> compiled.lastIndexOf((byte[]) null, 0));
        assertThrows(NullPointerException.class, () -> compiled.indexOf((ByteBuffer) null, 0));
        assertThrows(NullPointerException.class, () -> compiled.lastIndexOf((ByteBuffer) null, 0));
    }

    @Test
    void boundariesOverlapsAndUnsignedByteMasks() {
        byte[] source = {(byte) 0x80, (byte) 0xFF, (byte) 0x80, (byte) 0xFF, 0};
        M3BitapExactSearch compiled = new M3BitapExactSearch(new byte[] {(byte) 0x80, (byte) 0xFF});
        assertEquals(0, compiled.indexOf(source, Integer.MIN_VALUE));
        assertEquals(2, compiled.indexOf(source, 1));
        assertEquals(-1, compiled.indexOf(source, Integer.MAX_VALUE));
        assertEquals(-1, compiled.lastIndexOf(source, -1));
        assertEquals(0, compiled.lastIndexOf(source, 0));
        assertEquals(2, compiled.lastIndexOf(source, Integer.MAX_VALUE));
        M3BitapExactSearch overlaps = new M3BitapExactSearch(new byte[] {7, 7, 7});
        assertEquals(1, overlaps.indexOf(new byte[] {7, 7, 7, 7, 7}, 1));
        assertEquals(2, overlaps.lastIndexOf(new byte[] {7, 7, 7, 7, 7}, 10));
    }

    @Test
    void seededOracleForArraysAndAllByteBufferKinds() {
        Random random = new Random(0xB17A2026L);
        for (int caseNo = 0; caseNo < 25_000; caseNo++) {
            byte[] text = new byte[random.nextInt(160)];
            byte[] needle = new byte[1 + random.nextInt(63)];
            random.nextBytes(text);
            random.nextBytes(needle);
            if (caseNo % 5 == 0) {
                Arrays.fill(text, (byte) -1);
                Arrays.fill(needle, (byte) -1);
            }
            if (caseNo % 9 == 0 && text.length >= needle.length) {
                System.arraycopy(text, random.nextInt(text.length - needle.length + 1),
                        needle, 0, needle.length);
            }
            int from = switch (caseNo % 6) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> -1;
                case 3 -> 0;
                default -> random.nextInt(200);
            };
            M3BitapExactSearch compiled = new M3BitapExactSearch(needle);
            String evidence = "seed-case=" + caseNo + " from=" + from;
            int expectedFirst = oracle(text, needle, from, false);
            int expectedLast = oracle(text, needle, from, true);
            assertEquals(expectedFirst, compiled.indexOf(text, from), evidence);
            assertEquals(expectedLast, compiled.lastIndexOf(text, from), evidence);

            byte[] padded = new byte[text.length + 6];
            System.arraycopy(text, 0, padded, 3, text.length);
            ByteBuffer heap = ByteBuffer.wrap(padded);
            ByteBuffer direct = ByteBuffer.allocateDirect(padded.length);
            direct.put(padded).flip();
            for (ByteBuffer buffer : new ByteBuffer[] {heap, direct, heap.asReadOnlyBuffer(),
                    direct.asReadOnlyBuffer()}) {
                buffer.position(3);
                buffer.limit(3 + text.length);
                int position = buffer.position();
                int limit = buffer.limit();
                assertEquals(expectedFirst, compiled.indexOf(buffer, from), evidence);
                assertEquals(expectedLast, compiled.lastIndexOf(buffer, from), evidence);
                assertEquals(position, buffer.position(), "position preserved");
                assertEquals(limit, buffer.limit(), "limit preserved");
            }
        }
    }

    private static int oracle(byte[] text, byte[] needle, int from, boolean reverse) {
        Objects.requireNonNull(text);
        int maximum = text.length - needle.length;
        if (maximum < 0 || (reverse && from < 0)) return -1;
        int start = reverse ? 0 : Math.max(0, from);
        int end = reverse ? Math.min(from, maximum) : maximum;
        int result = -1;
        for (int offset = start; offset <= end; offset++) {
            boolean equal = true;
            for (int i = 0; i < needle.length; i++) {
                if (text[offset + i] != needle[i]) { equal = false; break; }
            }
            if (equal) {
                result = offset;
                if (!reverse) return result;
            }
        }
        return result;
    }
}
