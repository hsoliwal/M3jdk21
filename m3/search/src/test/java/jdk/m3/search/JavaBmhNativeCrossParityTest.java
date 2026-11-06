// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/JavaBmhNativeCrossParityTest.java (blob 40f3ce0e6409) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Same-corpus execution against first-party Java Horspool, the original ABI-1 JNI
 * provider, and an independent offset oracle. No challenge solution is copied.
 */
final class JavaBmhNativeCrossParityTest {
    private static M3JniSearchEngine nativeEngine() {
        String configured = System.getProperty("m3.native.library.path");
        assertTrue(configured != null && !configured.isBlank(),
                "actual native library path is required, not a Java fallback");
        Path library = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(library), "native ABI-1 library must exist");
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path", library.toString());
        M3JniSearchEngine engine = new M3JniSearchEngine();
        assertTrue(engine.available(), "JNI provider must be available and ABI-checked");
        return engine;
    }

    @Test
    void pathologicalForwardAndReverseByteSearchAgreeWithNativeAndOracle() {
        M3JniSearchEngine engine = nativeEngine();
        for (boolean reverse : new boolean[] {false, true}) {
            byte[] needle = alternating(1025);
            needle[reverse ? needle.length - 1 : 0] = (byte) 'X';
            byte[] text = alternating(32_768);
            try (M3CompiledSearch javaSearch = new M3BoyerMooreHorspoolSearch(needle);
                    M3CompiledSearch nativeSearch = engine.compile(needle)) {
                assertEquals("BOYER_MOORE_HORSPOOL", javaSearch.strategy());
                assertEquals("JNI_PRECOMPUTED_BMH", nativeSearch.strategy());
                assertEquals(-1, javaSearch.indexOf(text, 0));
                assertEquals(-1, nativeSearch.indexOf(text, 0));
                assertEquals(-1, javaSearch.lastIndexOf(text, Integer.MAX_VALUE));
                assertEquals(-1, nativeSearch.lastIndexOf(text, Integer.MAX_VALUE));

                System.arraycopy(needle, 0, text, 4096, needle.length);
                System.arraycopy(needle, 0, text, 24_576, needle.length);
                for (int from : new int[] {-5, 0, 4096, 4097, 24576,
                        24577, text.length, Integer.MAX_VALUE}) {
                    assertSameResults(javaSearch, nativeSearch, text, needle, from);
                }
                assertArrayEquals(needle, Arrays.copyOfRange(text, 4096, 4096 + needle.length));
                assertEquals(2, M3ByteOccurrences.count(javaSearch, text, 0, null));
                assertEquals(2, M3ByteOccurrences.count(nativeSearch, text, 0, null));
                assertEquals(2, M3JniSearchEngine.countIfNative(
                        nativeSearch, text, 0, new M3DefaultProgressMonitor()));
            }
        }
    }

    @Test
    void seededArrayDirectBufferAndOverlappingPositionOracles() {
        M3JniSearchEngine engine = nativeEngine();
        Random random = new Random(0x4d33424d484eL);
        for (int sample = 0; sample < 480; sample++) {
            byte[] text = new byte[random.nextInt(512)];
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
            byte[] snapshot = text.clone();
            try (M3CompiledSearch javaSearch = new M3BoyerMooreHorspoolSearch(needle);
                    M3CompiledSearch nativeSearch = engine.compile(needle)) {
                for (int from : new int[] {-3, 0, 1, text.length / 2, text.length,
                        Integer.MAX_VALUE}) {
                    assertSameResults(javaSearch, nativeSearch, text, needle, from);
                    List<Integer> expected = allOffsets(text, needle, from);
                    assertEquals(expected.size(),
                            M3ByteOccurrences.count(javaSearch, text, from, null));
                    assertEquals(expected.size(),
                            M3ByteOccurrences.count(nativeSearch, text, from, null));
                    assertEquals(expected.size(), M3JniSearchEngine.countIfNative(
                            nativeSearch, text, from, new M3DefaultProgressMonitor()));
                    int limit = Math.min(3, expected.size());
                    List<Integer> javaPositions = new ArrayList<>();
                    List<Integer> nativePositions = new ArrayList<>();
                    assertEquals(limit, M3ByteOccurrences.visit(javaSearch, text, from,
                            limit, javaPositions::add, null));
                    assertEquals(limit, M3ByteOccurrences.visit(nativeSearch, text, from,
                            limit, nativePositions::add, null));
                    assertEquals(expected.subList(0, limit), javaPositions);
                    assertEquals(expected.subList(0, limit), nativePositions);
                    if (limit > 0) {
                        int[] actualNative = M3JniSearchEngine.positionsIfNative(
                                nativeSearch, text, from, limit, new M3DefaultProgressMonitor());
                        assertTrue(actualNative != null,
                                "actual JNI positions symbol must run, never silently fall back");
                        assertEquals(expected.subList(0, limit),
                                Arrays.stream(actualNative).boxed().toList());
                    }
                }
            }
            assertArrayEquals(snapshot, text, "neither provider may mutate caller bytes");
        }
    }

    @Test
    void challengeCategoriesPreserveFirstCountAndAllOffsetsAcrossProviders() {
        M3JniSearchEngine engine = nativeEngine();
        byte[][] texts = {
                "sadbutsad".getBytes(StandardCharsets.US_ASCII),
                "ABCDCDC".getBytes(StandardCharsets.US_ASCII),
                "ababa".getBytes(StandardCharsets.US_ASCII),
                {(byte) 0x80, 0, (byte) 0x80, 0, (byte) 0x80}
        };
        byte[][] needles = {
                "sad".getBytes(StandardCharsets.US_ASCII),
                "CDC".getBytes(StandardCharsets.US_ASCII),
                "aba".getBytes(StandardCharsets.US_ASCII),
                {(byte) 0x80, 0, (byte) 0x80}
        };
        List<List<Integer>> expected = List.of(
                List.of(0, 6), List.of(2, 4), List.of(0, 2), List.of(0, 2));
        for (int sample = 0; sample < texts.length; sample++) {
            byte[] text = texts[sample];
            byte[] original = text.clone();
            byte[] needle = needles[sample];
            List<Integer> starts = expected.get(sample);
            try (M3CompiledSearch javaSearch = new M3BoyerMooreHorspoolSearch(needle);
                    M3CompiledSearch nativeSearch = engine.compile(needle)) {
                for (M3CompiledSearch search : new M3CompiledSearch[] {javaSearch, nativeSearch}) {
                    assertEquals(starts.get(0).intValue(), search.indexOf(text, 0),
                            "first offset for category " + sample);
                    assertEquals(starts.get(starts.size() - 1).intValue(),
                            search.lastIndexOf(text, Integer.MAX_VALUE),
                            "last offset for category " + sample);
                    assertEquals(starts.size(), M3ByteOccurrences.count(search, text, 0, null),
                            "overlapping count for category " + sample);
                    List<Integer> offsets = new ArrayList<>();
                    assertEquals(starts.size(), M3ByteOccurrences.visit(search, text, 0,
                            Integer.MAX_VALUE, offsets::add, null));
                    assertEquals(starts, offsets, "ordered positions for category " + sample);
                }
                int[] nativePositions = M3JniSearchEngine.positionsIfNative(
                        nativeSearch, text, 0, starts.size(), new M3DefaultProgressMonitor());
                assertTrue(nativePositions != null, "actual JNI positions required");
                assertEquals(starts, Arrays.stream(nativePositions).boxed().toList());
            }
            assertArrayEquals(original, text, "caller-owned bytes must remain unchanged");
        }
    }
    @Test
    void nativeHandleClosesWhileJavaOwnerRemainsIndependent() {
        M3JniSearchEngine engine = nativeEngine();
        byte[] needle = {(byte) 0xff, 0, 1, (byte) 0xff};
        byte[] text = {9, (byte) 0xff, 0, 1, (byte) 0xff, 2};
        M3CompiledSearch nativeSearch = engine.compile(needle);
        M3CompiledSearch javaSearch = new M3BoyerMooreHorspoolSearch(needle);
        assertEquals(1, nativeSearch.indexOf(text, 0));
        assertEquals(1, javaSearch.indexOf(text, 0));
        nativeSearch.close();
        nativeSearch.close();
        assertThrows(IllegalStateException.class, () -> nativeSearch.indexOf(text, 0));
        assertThrows(IllegalStateException.class, () -> nativeSearch.lastIndexOf(text, 0));
        assertEquals(1, javaSearch.indexOf(text, 0));
        javaSearch.close();
    }

    private static void assertSameResults(M3CompiledSearch javaSearch,
            M3CompiledSearch nativeSearch, byte[] text, byte[] needle, int from) {
        int expectedFirst = offset(text, needle, from, false);
        int expectedLast = offset(text, needle, from, true);
        assertEquals(expectedFirst, javaSearch.indexOf(text, from));
        assertEquals(expectedFirst, nativeSearch.indexOf(text, from));
        assertEquals(expectedLast, javaSearch.lastIndexOf(text, from));
        assertEquals(expectedLast, nativeSearch.lastIndexOf(text, from));

        ByteBuffer owner = ByteBuffer.allocateDirect(text.length + 11);
        owner.position(7).put(text).limit(7 + text.length).position(7);
        ByteBuffer readonly = owner.asReadOnlyBuffer();
        readonly.mark();
        assertEquals(expectedFirst, javaSearch.indexOf(readonly, from));
        assertEquals(expectedFirst, nativeSearch.indexOf(readonly, from));
        assertEquals(expectedLast, javaSearch.lastIndexOf(readonly, from));
        assertEquals(expectedLast, nativeSearch.lastIndexOf(readonly, from));
        readonly.reset();
        assertEquals(7, readonly.position(), "position and mark must survive");
        assertEquals(7 + text.length, readonly.limit(), "limit must survive");
    }

    private static int offset(byte[] text, byte[] needle, int from, boolean reverse) {
        int last = text.length - needle.length;
        if (last < 0 || reverse && from < 0) return -1;
        for (int at = reverse ? Math.min(from, last) : Math.max(0, from);
                reverse ? at >= 0 : at <= last; at += reverse ? -1 : 1) {
            int i = 0;
            while (i < needle.length && text[at + i] == needle[i]) i++;
            if (i == needle.length) return at;
        }
        return -1;
    }

    private static List<Integer> allOffsets(byte[] text, byte[] needle, int from) {
        List<Integer> positions = new ArrayList<>();
        for (int at = Math.max(0, from); at <= text.length - needle.length; at++) {
            if (offset(text, needle, at, false) == at) positions.add(at);
        }
        return positions;
    }

    private static byte[] alternating(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) bytes[i] = (byte) ((i & 1) == 0 ? 'a' : 'b');
        return bytes;
    }
}
