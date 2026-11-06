// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/MinimumCoverWindowNativeAtomTest.java (blob 1298163a6183) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Separate C17/JNI atom JUnit proof; explicitly enabled only with compiled native library. */
@EnabledIfSystemProperty(named = "m3.minimum.cover.native.junit", matches = "true")
final class MinimumCoverWindowNativeAtomTest {
    private static final char[] UNITS = {'a', 'b', 'A', '\0', '\u00e9', '\u4e2d', '\uD800', '\uDC00', '\uffff'};

    @BeforeAll
    static void loadNative() {
        String library = System.getProperty("m3.minimum.cover.library");
        if (library == null || library.isBlank()) throw new IllegalStateException("native library path required");
        System.load(library);
    }

    @Test
    void nativeMatchesIndependentOracleAndDoesNotMutateArrays() {
        SplittableRandom random = new SplittableRandom(0x4d335f4e41544956L);
        for (int iteration = 0; iteration < 4096; iteration++) {
            StringBuilder source = new StringBuilder();
            StringBuilder required = new StringBuilder();
            for (int n = random.nextInt(24); n > 0; n--) source.append(UNITS[random.nextInt(UNITS.length)]);
            for (int n = random.nextInt(9); n > 0; n--) required.append(UNITS[random.nextInt(UNITS.length)]);
            char[] haystack = source.toString().toCharArray();
            char[] needle = required.toString().toCharArray();
            char[] originalHaystack = haystack.clone();
            char[] originalNeedle = needle.clone();
            long expected = MinimumCoverWindowJavaAtomTest.independent(source.toString(), required.toString());
            assertEquals(expected, M3MinimumCoverWindow.nativeSpan(haystack, needle),
                    "iteration=" + iteration);
            assertArrayEquals(originalHaystack, haystack);
            assertArrayEquals(originalNeedle, needle);
        }
    }

    @Test
    void nativeEmptyAndBoundaryRefusal() {
        assertEquals(0L, M3MinimumCoverWindow.nativeSpan(new char[0], new char[0]));
        assertEquals(-1L, M3MinimumCoverWindow.nativeSpan(new char[0], "x".toCharArray()));
        assertEquals(M3MinimumCoverWindow.pack(0, 1),
                M3MinimumCoverWindow.nativeSpan("a\uffffb".toCharArray(), new char[]{'a'}));
        assertThrows(NullPointerException.class, () -> M3MinimumCoverWindow.nativeSpan(null, new char[]{'a'}));
        assertThrows(NullPointerException.class, () -> M3MinimumCoverWindow.nativeSpan(new char[]{'a'}, null));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumCoverWindow.nativeSpan(new char[1_000_001], new char[]{'a'}));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumCoverWindow.nativeSpan(new char[]{'a'}, new char[1_000_001]));
    }

    @Test
    void nativeSameArrayForSourceAndRequirementPreservesIdentity() {
        char[] shared = {'a', '\0', 'b', 'a', '\uD800', '\uDC00'};
        char[] original = shared.clone();
        assertEquals(M3MinimumCoverWindow.pack(0, shared.length),
                M3MinimumCoverWindow.nativeSpan(shared, shared));
        assertArrayEquals(original, shared);
    }

    @Test
    void concurrentJniCallsRemainIsolatedAndPreserveSharedArrays() throws Exception {
        final int workers = 4;
        final int casesPerWorker = 512;
        char[] commonSource = "a\0b\uffffa\uD800\uDC00a\u4e2db".toCharArray();
        char[] commonRequired = {'a', 'b'};
        char[] originalSource = commonSource.clone();
        char[] originalRequired = commonRequired.clone();
        long expectedCommon = MinimumCoverWindowJavaAtomTest.independent(
                new String(commonSource), new String(commonRequired));

        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(workers);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<?>> tasks = new java.util.ArrayList<>();
        try {
            for (int worker = 0; worker < workers; worker++) {
                final int workerId = worker;
                tasks.add(pool.submit(() -> {
                    SplittableRandom random =
                            new SplittableRandom(0x4d335f41544f4d4cL + workerId);
                    start.await();
                    for (int iteration = 0; iteration < casesPerWorker; iteration++) {
                        StringBuilder source = new StringBuilder();
                        StringBuilder required = new StringBuilder();
                        for (int n = random.nextInt(49); n > 0; n--) {
                            source.append(UNITS[random.nextInt(UNITS.length)]);
                        }
                        for (int n = random.nextInt(13); n > 0; n--) {
                            required.append(UNITS[random.nextInt(UNITS.length)]);
                        }
                        String haystack = source.toString();
                        String needle = required.toString();
                        char[] sourceArray = haystack.toCharArray();
                        char[] requiredArray = needle.toCharArray();
                        long expected = MinimumCoverWindowJavaAtomTest.independent(
                                haystack, needle);
                        assertEquals(expected,
                                M3MinimumCoverWindow.nativeSpan(sourceArray, requiredArray),
                                "worker=" + workerId + " case=" + iteration);
                        assertEquals(expected, M3MinimumCoverWindow.javaSpan(haystack, needle),
                                "Java oracle worker=" + workerId + " case=" + iteration);
                        assertEquals(expectedCommon,
                                M3MinimumCoverWindow.nativeSpan(commonSource, commonRequired),
                                "shared arrays worker=" + workerId + " case=" + iteration);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (java.util.concurrent.Future<?> task : tasks) {
                task.get(45, java.util.concurrent.TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
        assertArrayEquals(originalSource, commonSource);
        assertArrayEquals(originalRequired, commonRequired);
    }
}
