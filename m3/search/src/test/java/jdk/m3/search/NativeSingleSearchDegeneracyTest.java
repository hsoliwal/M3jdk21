// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/NativeSingleSearchDegeneracyTest.java (blob 7cca8e949868) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Native-only qualification fixture: the host must explicitly enable the real JNI provider. */
final class NativeSingleSearchDegeneracyTest {
    private static CompiledSearch compile(byte[] needle) {
        JniSearchEngine engine = new JniSearchEngine();
        assertTrue(engine.available(), "qualification requires the actual native library");
        return engine.compile(needle);
    }

    @Test
    void longFalseCandidatesThenHitsPreserveBothDirectionsAndReuse() {
        byte[] needle = new byte[2048];
        Arrays.fill(needle, (byte) 'a');
        needle[needle.length - 2] = 'b';
        byte[] text = new byte[65536];
        Arrays.fill(text, (byte) 'a');
        try (CompiledSearch compiled = compile(needle)) {
            assertEquals(-1, compiled.indexOf(text, 0));
            assertEquals(-1, compiled.lastIndexOf(text, Integer.MAX_VALUE));
            System.arraycopy(needle, 0, text, 8192, needle.length);
            System.arraycopy(needle, 0, text, 49152, needle.length);
            assertEquals(8192, compiled.indexOf(text, -1));
            assertEquals(49152, compiled.indexOf(text, 8193));
            assertEquals(49152, compiled.lastIndexOf(text, Integer.MAX_VALUE));
            assertEquals(8192, compiled.lastIndexOf(text, 49151));
            assertEquals(-1, compiled.lastIndexOf(text, 8191));
            assertEquals(2, ByteOccurrences.count(compiled, text, 0, null));
        }
    }

    @Test
    void directReadOnlyWindowRetainsBoundsAndPositionAfterFallback() {
        byte[] needle = new byte[1024];
        for (int i = 0; i < needle.length; i++) needle[i] = (byte) ("abc".charAt(i % 3));
        needle[needle.length - 2] = 'z';
        byte[] text = new byte[32768];
        for (int i = 0; i < text.length; i++) text[i] = (byte) ("abc".charAt(i % 3));
        ByteBuffer owner = ByteBuffer.allocateDirect(text.length + 19);
        owner.position(7).put(text).limit(7 + text.length).position(7);
        ByteBuffer view = owner.asReadOnlyBuffer();
        try (CompiledSearch compiled = compile(needle)) {
            assertEquals(-1, compiled.indexOf(view, 0));
            assertEquals(-1, compiled.lastIndexOf(view, Integer.MAX_VALUE));
            owner.position(7 + 16384).put(needle).position(7);
            assertEquals(16384, compiled.indexOf(view, 0));
            assertEquals(16384, compiled.lastIndexOf(view, Integer.MAX_VALUE));
            assertEquals(-1, compiled.indexOf(view, 16385));
            assertEquals(-1, compiled.lastIndexOf(view, 16383));
            assertEquals(7, view.position());
            assertEquals(7 + text.length, view.limit());
        }
    }

    @Test
    void seededBinaryCasesAgreeWithIndependentOracleForArrayAndDirectViews() {
        Random random = new Random(0x724bf19aL);
        for (int trial = 0; trial < 1200; trial++) {
            byte[] text = new byte[random.nextInt(384)];
            byte[] needle = new byte[1 + random.nextInt(96)];
            random.nextBytes(text);
            random.nextBytes(needle);
            if (trial % 3 == 0 && needle.length <= text.length) {
                System.arraycopy(needle, 0, text, random.nextInt(text.length - needle.length + 1), needle.length);
            }
            ByteBuffer direct = ByteBuffer.allocateDirect(text.length + 4);
            direct.position(2).put(text).limit(2 + text.length).position(2);
            try (CompiledSearch compiled = compile(needle)) {
                for (int from : new int[] {-1, 0, text.length / 2, text.length, Integer.MAX_VALUE}) {
                    int first = naive(text, needle, from, false);
                    int last = naive(text, needle, from, true);
                    assertEquals(first, compiled.indexOf(text, from));
                    assertEquals(last, compiled.lastIndexOf(text, from));
                    assertEquals(first, compiled.indexOf(direct, from));
                    assertEquals(last, compiled.lastIndexOf(direct, from));
                }
            }
        }
    }

    @Test
    void quickBoundaryHitsAndClosedHandleRetainExistingBehavior() {
        byte[] needle = new byte[4096];
        Arrays.fill(needle, (byte) 0x81);
        needle[needle.length - 2] = 0;
        byte[] text = new byte[32768];
        System.arraycopy(needle, 0, text, 0, needle.length);
        System.arraycopy(needle, 0, text, text.length - needle.length, needle.length);
        CompiledSearch compiled = compile(needle);
        assertEquals(0, compiled.indexOf(text, 0));
        assertEquals(text.length - needle.length, compiled.lastIndexOf(text, Integer.MAX_VALUE));
        compiled.close();
        compiled.close();
        assertThrows(IllegalStateException.class, () -> compiled.indexOf(text, 0));
        assertThrows(IllegalStateException.class, () -> compiled.lastIndexOf(text, 0));
    }

    private static int naive(byte[] text, byte[] needle, int from, boolean reverse) {
        if (needle.length > text.length || reverse && from < 0) return -1;
        int last = text.length - needle.length;
        int start = reverse ? Math.min(from, last) : Math.max(from, 0);
        for (int at = start; reverse ? at >= 0 : at <= last; at += reverse ? -1 : 1) {
            int i = 0;
            while (i < needle.length && text[at + i] == needle[i]) i++;
            if (i == needle.length) return at;
        }
        return -1;
    }
}
