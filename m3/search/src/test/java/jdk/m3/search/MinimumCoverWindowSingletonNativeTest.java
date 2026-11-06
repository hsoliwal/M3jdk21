// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumCoverWindowSingletonNativeTest.java (blob 60233da1c256) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Separate native C17/JNI per-atom range-offset proof, opt-in only. */
@EnabledIfSystemProperty(named = "m3.minimum.cover.native.junit", matches = "true")
final class MinimumCoverWindowSingletonNativeTest {
    @BeforeAll
    static void loadNative() {
        String path = System.getProperty("m3.minimum.cover.library");
        if (path == null || path.isBlank()) throw new IllegalStateException("native library required");
        System.load(path);
    }
    @Test
    void nativeNonzeroOffsetMatchesIndependentOracleWithoutArrayWrites() {
        String source = "\0a\uffffb\uD800\uDC00\u4e2d\uffffz\0\u4e2d";
        for (String required : new String[]{"\0", "\uffff", "\uffff\uffff",
                "\uD800", "\uDC00", "\u4e2d", "a", "ab", "\0\uffff",
                "a\uffff", "\u4e2d\u4e2d", "b\u4e2d"}) {
            char[] haystack = source.toCharArray();
            char[] pattern = required.toCharArray();
            char[] copySource = haystack.clone();
            char[] copyPattern = pattern.clone();
            assertEquals(MinimumCoverWindowJavaAtomTest.independent(source, required),
                    M3MinimumCoverWindow.nativeSpan(haystack, pattern), required);
            assertArrayEquals(copySource, haystack);
            assertArrayEquals(copyPattern, pattern);
        }
    }
    @Test
    void aliasedCharArraysAndHighSparseCodeUnitsAreStable() {
        char[] shared = "\uffff\0\u4e2d".toCharArray();
        char[] copy = shared.clone();
        assertEquals(M3MinimumCoverWindow.pack(0, shared.length),
                M3MinimumCoverWindow.nativeSpan(shared, shared));
        assertArrayEquals(copy, shared);
        assertEquals(M3MinimumCoverWindow.pack(1, 2),
                M3MinimumCoverWindow.nativeSpan("\0\uffff\0".toCharArray(), new char[]{'\uffff'}));
    }

    @Test
    void exhaustiveSingletonJniCodeUnitsWithoutMutation() {
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            char[] source = {'\uffff', (char) unit, (char) unit, '\0'};
            char[] required = {(char) unit};
            char[] before = source.clone();
            assertEquals(MinimumCoverWindowJavaAtomTest.independent(
                    new String(source), new String(required)),
                    M3MinimumCoverWindow.nativeSpan(source, required), "unit=" + unit);
            assertArrayEquals(before, source);
            assertArrayEquals(new char[] {(char) unit}, required);
        }
    }

    @Test
    void singletonNativeAliasedArrayAndConcurrentCalls() throws Exception {
        char[] alias = {'\uffff'};
        assertEquals(M3MinimumCoverWindow.pack(0, 1),
                M3MinimumCoverWindow.nativeSpan(alias, alias));
        assertArrayEquals(new char[] {'\uffff'}, alias);
        char[] shared = {'\uffff', '\0', 'a', '\uD800', '\uDC00'};
        char[] original = shared.clone();
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var gate = new java.util.concurrent.CountDownLatch(1);
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int worker = 0; worker < 4; worker++) {
                final int id = worker;
                tasks.add(pool.submit(() -> {
                    var random = new java.util.SplittableRandom(0x4d335f53494e474cL + id);
                    char[] alphabet = {'a', 'A', '\0', '\uD800', '\uDC00', '\uffff'};
                    gate.await();
                    for (int i = 0; i < 512; i++) {
                        StringBuilder buffer = new StringBuilder();
                        for (int n = random.nextInt(40); n > 0; n--) {
                            buffer.append(alphabet[random.nextInt(alphabet.length)]);
                        }
                        char[] source = buffer.toString().toCharArray();
                        char[] target = {alphabet[random.nextInt(alphabet.length)]};
                        assertEquals(MinimumCoverWindowJavaAtomTest.independent(
                                new String(source), new String(target)),
                                M3MinimumCoverWindow.nativeSpan(source, target));
                        assertEquals(M3MinimumCoverWindow.pack(0, shared.length),
                                M3MinimumCoverWindow.nativeSpan(shared, shared));
                    }
                    return null;
                }));
            }
            gate.countDown();
            for (var task : tasks) task.get(30, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertArrayEquals(original, shared);
    }
}
