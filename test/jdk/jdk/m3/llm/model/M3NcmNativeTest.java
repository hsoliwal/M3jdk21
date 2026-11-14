/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.llm.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Run separately with -Dm3.ncm.library=/absolute/path/libm3_ncm.so. */
class M3NcmNativeTest {
    @BeforeAll static void explicitLoad() {
        String library = System.getProperty("m3.ncm.library");
        assumeTrue(library != null && !library.isBlank(), "explicit NCM native library not supplied");
        M3NcmQuantizer.loadNative(Path.of(library));
    }

    @Test void abiAndExplicitLoadingAreStable() {
        assertTrue(M3NcmQuantizer.nativeAvailable());
        assertEquals(1, M3NcmNative.abiVersion0());
        M3NcmQuantizer.loadNative(Path.of(System.getProperty("m3.ncm.library")));
        assertThrows(IllegalStateException.class,
                () -> M3NcmQuantizer.loadNative(Path.of("/different/ncm/library.so")));
        assertThrows(NullPointerException.class, () -> M3NcmQuantizer.loadNative(null));
    }

    @Test void nativeEntryIndependentlyRejectsMalformedDimensionsAndValues() {
        assertThrows(NullPointerException.class,
                () -> M3NcmNative.nearest0(null, 1, 1, new float[]{0}, 0, 1));
        assertThrows(NullPointerException.class,
                () -> M3NcmNative.nearest0(new float[]{0}, 1, 1, null, 0, 1));
        int[][] invalidShapes = {
                {-1, 1, 0, 1}, {1, 0, 0, 1}, {1, -1, 0, 1}, {1, 1, -1, 1},
                {1, 1, 0, 0}, {1, 1, 0, -1}, {2, 1, 0, 1}, {1, 1, 1, 1},
                {Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 1},
                {1, 1, Integer.MAX_VALUE, Integer.MAX_VALUE}
        };
        for (int[] shape : invalidShapes) {
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearest0(
                    new float[]{0}, shape[0], shape[1], new float[]{0}, shape[2], shape[3]));
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearestJava(
                    new float[]{0}, shape[0], shape[1], new float[]{0}, shape[2], shape[3]));
        }
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearest0(
                    new float[]{invalid}, 1, 1, new float[]{0}, 0, 1));
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearest0(
                    new float[]{0}, 1, 1, new float[]{invalid}, 0, 1));
        }
        assertArrayEquals(new int[0], M3NcmNative.nearest0(
                new float[0], 0, 1, new float[]{0}, 0, 1));
        assertArrayEquals(new int[]{0}, M3NcmNative.nearest0(
                new float[]{0}, 1, 1, new float[]{Float.NaN, 0, Float.POSITIVE_INFINITY}, 1, 1));
    }

    @Test void signedZeroTiesExtremesAliasingAndMutationMatchJava() {
        float[] zeros = {-0.0f, 0.0f};
        assertArrayEquals(new int[]{0, 0}, M3NcmNative.nearest0(zeros, 2, 1, zeros, 0, 2));
        float[] values = {1, 2, 3, 4, 5, 6};
        float[] before = values.clone();
        assertArrayEquals(new int[]{0, 1}, M3NcmNative.nearest0(values, 2, 3, values, 0, 2));
        assertArrayEquals(before, values);
        float[] extremes = {-Float.MAX_VALUE, Float.MAX_VALUE, -Float.MIN_VALUE, Float.MIN_VALUE};
        assertArrayEquals(M3NcmNative.nearestJava(extremes, 4, 1, extremes, 0, 4),
                M3NcmNative.nearest0(extremes, 4, 1, extremes, 0, 4));
        assertArrayEquals(new int[]{0}, M3NcmNative.nearest0(
                new float[]{0}, 1, 1, new float[]{-1, 1}, 0, 2));
    }

    @Test void randomizedFiniteBitPatternsMatchTheJavaOracle() {
        Random random = new Random(0x4d334e434dL);
        for (int trial = 0; trial < 1800; trial++) {
            int rows = random.nextInt(18);
            int channels = 1 + random.nextInt(23);
            int size = 1 + random.nextInt(37);
            float[] vectors = new float[rows * channels];
            float[] books = new float[size * channels + 4];
            for (int i = 0; i < vectors.length; i++) vectors[i] = finiteRandom(random);
            for (int i = 2; i < books.length - 2; i++) books[i] = finiteRandom(random);
            books[0] = Float.NaN;
            books[books.length - 1] = Float.POSITIVE_INFINITY;
            float[] original = vectors.clone();
            assertArrayEquals(M3NcmNative.nearestJava(vectors, rows, channels, books, 2, size),
                    M3NcmNative.nearest0(vectors, rows, channels, books, 2, size),
                    "nearest-code differential case " + trial);
            assertArrayEquals(original, vectors);
        }
    }

    @Test void randomizedMultilevelJavaAndNativeResultsHaveIdenticalRoots() {
        Random random = new Random(0x713ac281L);
        M3ModelInput source = M3NcmQuantizerTest.input();
        for (int trial = 0; trial < 350; trial++) {
            int length = 1 + random.nextInt(17);
            int channels = 1 + random.nextInt(7);
            int[] levels = {1, (length + 1) / 2, length};
            int[] sizes = {1 + random.nextInt(11), 1 + random.nextInt(11), 1 + random.nextInt(11)};
            float[] books = new float[(sizes[0] + sizes[1] + sizes[2]) * channels];
            float[] latent = new float[length * channels];
            for (int i = 0; i < books.length; i++) books[i] = (random.nextInt(65) - 32) / 8.0f;
            for (int i = 0; i < latent.length; i++) latent[i] = (random.nextInt(65) - 32) / 8.0f;
            M3NcmQuantizer java = M3NcmQuantizer.of(length, channels, levels, sizes, books,
                    M3NcmQuantizer.Backend.JAVA);
            M3NcmQuantizer nativeBackend = M3NcmQuantizer.of(length, channels, levels, sizes, books,
                    M3NcmQuantizer.Backend.NATIVE);
            M3NcmCodes expected = java.encode(source, latent);
            M3NcmCodes actual = nativeBackend.encode(source, latent);
            assertEquals(expected, actual, "multilevel differential case " + trial);
            assertArrayEquals(java.reconstruct(expected), nativeBackend.reconstruct(actual));
        }
    }

    @Test void concurrentNativeCallsRetainNoPointersOrSharedWorkspace() throws Exception {
        float[] vectors = {1, 2, 3, 4, 5, 6};
        float[] books = {1, 2, 3, 4, 5, 6, 0, 0, 0};
        ArrayList<Callable<int[]>> jobs = new ArrayList<>();
        for (int i = 0; i < 160; i++) {
            jobs.add(() -> M3NcmNative.nearest0(vectors, 2, 3, books, 0, 3));
        }
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var result : executor.invokeAll(jobs)) assertArrayEquals(new int[]{0, 1}, result.get());
        }
        assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6}, vectors);
    }

    private static float finiteRandom(Random random) {
        float value;
        do {
            value = Float.intBitsToFloat(random.nextInt());
        } while (!Float.isFinite(value));
        return value;
    }
}
