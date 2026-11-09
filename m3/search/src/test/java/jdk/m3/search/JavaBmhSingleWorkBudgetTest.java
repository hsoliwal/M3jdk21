// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JavaBmhSingleWorkBudgetTest.java (blob 03134a8b5424) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Original Java Horspool owner: bounded first/last search and native-policy parity corpus. */
final class JavaBmhSingleWorkBudgetTest {
    @Test
    void forwardPathologicalNearMatchesFallBackWithoutChangingFirstHit() throws Exception {
        byte[] needle = alternating(1025);
        needle[0] = (byte) 'X';
        byte[] text = alternating(65_536);
        try (CompiledSearch search = new BoyerMooreHorspoolSearch(needle)) {
            assertEquals("BOYER_MOORE_HORSPOOL", search.strategy());
            assertNull(prepared(search), "KMP preparation must be lazy");
            assertEquals(-1, search.indexOf(text, 0));
            assertNotNull(prepared(search), "pathological verification must reuse KMP");
            System.arraycopy(needle, 0, text, 8192, needle.length);
            System.arraycopy(needle, 0, text, 49_152, needle.length);
            assertEquals(8192, search.indexOf(text, -7));
            assertEquals(49_152, search.indexOf(text, 8193));
            assertEquals(-1, search.indexOf(text, text.length));
            assertBuffers(search, text, 8192, 49_152, false);
        }
    }

    @Test
    void reversePathologicalNearMatchesFallBackWithoutChangingLastHit() throws Exception {
        byte[] needle = alternating(1025);
        needle[needle.length - 1] = (byte) 'X';
        byte[] text = alternating(65_536);
        try (CompiledSearch search = new BoyerMooreHorspoolSearch(needle)) {
            assertNull(prepared(search), "KMP preparation must be lazy");
            assertEquals(-1, search.lastIndexOf(text, Integer.MAX_VALUE));
            assertNotNull(prepared(search), "pathological reverse verification must reuse KMP");
            System.arraycopy(needle, 0, text, 8192, needle.length);
            System.arraycopy(needle, 0, text, 49_152, needle.length);
            assertEquals(49_152, search.lastIndexOf(text, Integer.MAX_VALUE));
            assertEquals(8192, search.lastIndexOf(text, 49_151));
            assertEquals(-1, search.lastIndexOf(text, -1));
            assertBuffers(search, text, 8192, 49_152, true);
        }
    }

    @Test
    void seededArrayAndBufferSearchesAgreeWithIndependentFirstAndLastOracles() {
        Random random = new Random(0x4d33424d4857L);
        for (int sample = 0; sample < 3000; sample++) {
            byte[] text = new byte[random.nextInt(320)];
            byte[] needle = new byte[1 + random.nextInt(112)];
            int alphabet = 2 + sample % 17;
            for (int i = 0; i < text.length; i++) {
                text[i] = (byte) (random.nextInt(alphabet) - 8);
            }
            for (int i = 0; i < needle.length; i++) {
                needle[i] = (byte) (random.nextInt(alphabet) - 8);
            }
            if (sample % 3 == 0 && needle.length <= text.length) {
                System.arraycopy(needle, 0, text,
                        random.nextInt(text.length - needle.length + 1), needle.length);
            }
            ByteBuffer direct = ByteBuffer.allocateDirect(text.length + 17);
            direct.position(7).put(text).limit(7 + text.length).position(7);
            ByteBuffer readonly = direct.asReadOnlyBuffer();
            readonly.mark();
            try (CompiledSearch search = new BoyerMooreHorspoolSearch(needle)) {
                for (int from : new int[] {-7, 0, 1, text.length / 2,
                        text.length, Integer.MAX_VALUE}) {
                    int expectedFirst = oracle(text, needle, from, false);
                    int expectedLast = oracle(text, needle, from, true);
                    assertEquals(expectedFirst, search.indexOf(text, from), "array first " + sample);
                    assertEquals(expectedLast, search.lastIndexOf(text, from), "array last " + sample);
                    assertEquals(expectedFirst, search.indexOf(readonly, from),
                            "read-only direct first " + sample);
                    assertEquals(expectedLast, search.lastIndexOf(readonly, from),
                            "read-only direct last " + sample);
                    readonly.reset();
                    assertEquals(7, readonly.position(), "buffer mark must survive");
                    assertEquals(7 + text.length, readonly.limit(), "buffer limit must survive");
                }
            }
        }
    }

    @Test
    void quickHitAndSparseMissDoNotPrepareTheLazyFallback() throws Exception {
        byte[] needle = alternating(512);
        byte[] sparse = new byte[65_536];
        try (CompiledSearch search = new BoyerMooreHorspoolSearch(needle)) {
            assertEquals(-1, search.indexOf(sparse, 0));
            assertEquals(-1, search.lastIndexOf(sparse, Integer.MAX_VALUE));
            assertNull(prepared(search), "sparse scans must stay on shift tables");
            System.arraycopy(needle, 0, sparse, 0, needle.length);
            assertEquals(0, search.indexOf(sparse, 0));
            assertEquals(0, search.lastIndexOf(sparse, 0));
            assertNull(prepared(search), "quick hits must stay on shift tables");
        }
    }

    private static void assertBuffers(CompiledSearch search, byte[] text,
            int first, int last, boolean reverse) {
        for (boolean direct : new boolean[] {false, true}) {
            ByteBuffer buffer = direct ? ByteBuffer.allocateDirect(text.length + 17)
                    : ByteBuffer.allocate(text.length + 17);
            buffer.position(7).put(text).limit(7 + text.length).position(7);
            ByteBuffer readonly = buffer.asReadOnlyBuffer();
            readonly.mark();
            if (reverse) {
                assertEquals(last, search.lastIndexOf(readonly, Integer.MAX_VALUE));
                assertEquals(first, search.lastIndexOf(readonly, last - 1));
            } else {
                assertEquals(first, search.indexOf(readonly, 0));
                assertEquals(last, search.indexOf(readonly, first + 1));
            }
            readonly.reset();
            assertEquals(7, readonly.position());
            assertEquals(7 + text.length, readonly.limit());
        }
    }

    private static Object prepared(CompiledSearch search) throws Exception {
        Field field = BoyerMooreHorspoolSearch.class.getDeclaredField("overlapSearch");
        field.setAccessible(true);
        return field.get(search);
    }

    private static byte[] alternating(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) result[i] = (byte) ((i & 1) == 0 ? 'a' : 'b');
        return result;
    }

    private static int oracle(byte[] haystack, byte[] needle, int from, boolean reverse) {
        int maximum = haystack.length - needle.length;
        if (maximum < 0 || reverse && from < 0) return -1;
        int start = reverse ? Math.min(from, maximum) : Math.max(0, from);
        for (int at = start; reverse ? at >= 0 : at <= maximum; at += reverse ? -1 : 1) {
            int matched = 0;
            while (matched < needle.length && haystack[at + matched] == needle[matched]) {
                matched++;
            }
            if (matched == needle.length) return at;
        }
        return -1;
    }
}
