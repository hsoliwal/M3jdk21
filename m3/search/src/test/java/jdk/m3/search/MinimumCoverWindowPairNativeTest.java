// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumCoverWindowPairNativeTest.java (blob 9bd20607633b) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Separately executable checked-JNI test suite for the native two-unit atom. */
@EnabledIfSystemProperty(named = "m3.minimum.cover.native.junit", matches = "true")
final class MinimumCoverWindowPairNativeTest {
    private static final char[] ALPHABET = {'a', 'b', 'X', '\0', '\u00e9', '\u4e2d', '\ud800', '\udc00', '\uffff'};

    @BeforeAll
    static void loadNative() {
        String path = System.getProperty("m3.minimum.cover.library");
        if (path == null || path.isBlank()) throw new IllegalStateException("native library required");
        System.load(path);
    }

    private static void parity(String source, String required) {
        char[] haystack = source.toCharArray();
        char[] needle = required.toCharArray();
        char[] sourceBefore = haystack.clone();
        char[] needleBefore = needle.clone();
        assertEquals(MinimumCoverWindowPairJavaTest.independent(source, required),
                MinimumCoverWindow.nativeSpan(haystack, needle), "source length=" + source.length());
        assertArrayEquals(sourceBefore, haystack);
        assertArrayEquals(needleBefore, needle);
    }

    @Test
    void repeatedDistinctSurrogateNulAndArrayAlias() {
        for (String[] sample : new String[][]{
                {"xaba", "aa"}, {"aXbXaa", "ab"}, {"bbXa", "ab"},
                {"abab", "ba"}, {"X", "aa"}, {"", "aa"},
                {"\uffff\0\ud800\udc00", "\uffff\0"}, {"\0aa\0", "\0\0"},
                {"ab", "ba"}, {"aaXaa", "aa"}, {"x", ""}}) {
            parity(sample[0], sample[1]);
        }
        char[] alias = {'\uffff', '\0'};
        assertEquals(MinimumCoverWindow.pack(0, alias.length),
                MinimumCoverWindow.nativeSpan(alias, alias));
        assertArrayEquals(new char[] {'\uffff', '\0'}, alias);
    }

    @Test
    void allUtf16UnitsWithUnmodifiedBorrowedArrays() {
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String source = "" + (char) unit + 'X' + (char) unit;
            String required = "" + (char) unit + (char) unit;
            parity(source, required);
        }
    }

    @Test
    void independentSeededRandomAndConcurrentSharedBuffers() throws Exception {
        final int workers = 4;
        final int cases = 1000;
        char[] shared = "a\uffff\0bb\ud800\udc00ab".toCharArray();
        char[] required = {'a', 'b'};
        char[] sharedBefore = shared.clone();
        char[] requiredBefore = required.clone();
        long sharedExpected = MinimumCoverWindowPairJavaTest.independent(
                new String(shared), new String(required));
        try (var pool = Executors.newFixedThreadPool(workers)) {
            CountDownLatch gate = new CountDownLatch(1);
            List<Future<?>> tasks = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                final int owner = worker;
                tasks.add(pool.submit(() -> {
                    SplittableRandom random = new SplittableRandom(0x4d335f504149524eL + owner);
                    gate.await();
                    for (int iteration = 0; iteration < cases; iteration++) {
                        StringBuilder source = new StringBuilder();
                        for (int n = random.nextInt(34); n > 0; n--) {
                            source.append(ALPHABET[random.nextInt(ALPHABET.length)]);
                        }
                        String requirement = "" + ALPHABET[random.nextInt(ALPHABET.length)]
                                + ALPHABET[random.nextInt(ALPHABET.length)];
                        parity(source.toString(), requirement);
                        assertEquals(sharedExpected, MinimumCoverWindow.nativeSpan(shared, required));
                    }
                    return null;
                }));
            }
            gate.countDown();
            for (Future<?> task : tasks) task.get(45, TimeUnit.SECONDS);
        }
        assertArrayEquals(sharedBefore, shared);
        assertArrayEquals(requiredBefore, required);
    }

    @Test
    void nativeNullBudgetAndMillionUnitBoundary() {
        assertThrows(NullPointerException.class,
                () -> MinimumCoverWindow.nativeSpan(null, new char[] {'a', 'b'}));
        assertThrows(NullPointerException.class,
                () -> MinimumCoverWindow.nativeSpan(new char[] {'a', 'b'}, null));
        assertThrows(IllegalArgumentException.class,
                () -> MinimumCoverWindow.nativeSpan(new char[1_000_001], new char[] {'a', 'b'}));
        assertThrows(IllegalArgumentException.class,
                () -> MinimumCoverWindow.nativeSpan(new char[] {'x'}, new char[1_000_001]));
        assertEquals(MinimumCoverWindow.pack(999998, 1_000_000),
                MinimumCoverWindow.nativeSpan(("x".repeat(999998) + "ab").toCharArray(),
                        new char[] {'a', 'b'}));
    }
}
