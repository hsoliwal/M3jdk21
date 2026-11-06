// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BmhByteBufferAbsoluteReadTest.java (blob b7a820bbb4ae) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** File-local, actual-owner Java21 first/last search and Buffer-state oracle. */
final class BmhByteBufferAbsoluteReadTest {
    @Test void heapDirectReadOnlyAndSliceParity() {
        Random random = new Random(0x4d33424d4844524cL);
        long checked = 0;
        for (int sample = 0; sample < 12_000; sample++) {
            byte[] bytes = new byte[random.nextInt(260)];
            byte[] needle = new byte[4 + random.nextInt(64)];
            random.nextBytes(bytes);
            random.nextBytes(needle);
            if (sample % 4 == 0 && bytes.length >= needle.length) {
                int at = random.nextInt(bytes.length - needle.length + 1);
                System.arraycopy(needle, 0, bytes, at, needle.length);
            }
            byte[] previous = bytes.clone();
            try (M3CompiledSearch compiled = new M3BoyerMooreHorspoolSearch(needle)) {
                for (int kind = 0; kind < 4; kind++) {
                    ByteBuffer backing = (kind & 1) == 0
                            ? ByteBuffer.allocate(bytes.length + 10)
                            : ByteBuffer.allocateDirect(bytes.length + 10);
                    for (int i = 0; i < bytes.length; i++) backing.put(i + 3, bytes[i]);
                    backing.position(3);
                    backing.limit(bytes.length + 3);
                    ByteBuffer view = kind < 2 ? backing : backing.slice().asReadOnlyBuffer();
                    view.mark();
                    int position = view.position(), limit = view.limit();
                    int[] froms = {Integer.MIN_VALUE, -1, 0, 1, bytes.length / 2,
                            bytes.length - 1, bytes.length, bytes.length + 1, Integer.MAX_VALUE};
                    for (int from : froms) {
                        assertEquals(first(bytes, needle, from), compiled.indexOf(view, from),
                                "index sample=" + sample + " kind=" + kind + " from=" + from);
                        assertEquals(last(bytes, needle, from), compiled.lastIndexOf(view, from),
                                "last sample=" + sample + " kind=" + kind + " from=" + from);
                        assertEquals(position, view.position());
                        assertEquals(limit, view.limit());
                        checked += 2;
                    }
                    view.reset();
                    assertEquals(position, view.position(), "buffer mark preserved");
                }
            }
            assertArrayEquals(previous, bytes);
        }
        assertEquals(864_000L, checked);
    }
    @Test void ownedNeedleAndReadOnlyDirectSlice() {
        byte[] needle = {'a','b','a','b'};
        try (M3CompiledSearch search = new M3BoyerMooreHorspoolSearch(needle)) {
            Arrays.fill(needle, (byte) 'x');
            ByteBuffer source = ByteBuffer.allocateDirect(20);
            byte[] body = {'z','a','b','a','b','x','a','b','a','b','z'};
            for (int i = 0; i < body.length; i++) source.put(i + 2, body[i]);
            source.position(2);
            source.limit(body.length + 2);
            ByteBuffer view = source.slice().asReadOnlyBuffer();
            view.position(1);
            view.mark();
            assertEquals(0, search.indexOf(view, -1));
            assertEquals(5, search.lastIndexOf(view, Integer.MAX_VALUE));
            view.reset();
            assertEquals(1, view.position());
            assertThrows(NullPointerException.class, () -> search.indexOf((ByteBuffer) null, 0));
            assertThrows(NullPointerException.class, () -> search.lastIndexOf((ByteBuffer) null, 0));
        }
    }
    private static int first(byte[] bytes, byte[] needle, int from) {
        for (int at = Math.max(0, from); at <= bytes.length - needle.length; at++)
            if (matches(bytes, needle, at)) return at;
        return -1;
    }
    private static int last(byte[] bytes, byte[] needle, int from) {
        for (int at = Math.min(from, bytes.length - needle.length); at >= 0; at--)
            if (matches(bytes, needle, at)) return at;
        return -1;
    }
    private static boolean matches(byte[] bytes, byte[] needle, int at) {
        for (int i = 0; i < needle.length; i++) if (bytes[at + i] != needle[i]) return false;
        return true;
    }
}
