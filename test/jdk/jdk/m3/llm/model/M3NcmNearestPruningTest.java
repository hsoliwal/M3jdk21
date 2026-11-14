/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.llm.model;

import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Compares exact pruning with the frozen, unpruned ordered-double distance contract. */
class M3NcmNearestPruningTest {
    @Test void partialTiesCannotHideALaterWinner() {
        float[] query = {0, 0, 0};
        float[] books = {1, 0, 0, 0, 1, 0, 0, 0, 0.5f};
        assertArrayEquals(new int[]{2}, M3NcmNative.nearestJava(query, 1, 3, books, 0, 3));
        assertArrayEquals(new int[]{0}, M3NcmNative.nearestJava(
                query, 1, 3, new float[]{1, 0, 0, 0, 1, 0}, 0, 2));
        assertArrayEquals(new int[]{0}, M3NcmNative.nearestJava(
                query, 1, 3, new float[]{-0.0f, 0, 0, 0, 0, 0}, 0, 2));
    }

    @Test void allComponentsAreValidatedBeforeAnyCandidateCanBePruned() {
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            // Code zero already matches; a partial sum at code one would also prune it.
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearestJava(
                    new float[]{0, 0, 0}, 1, 3, new float[]{0, 0, 0, 100, 0, invalid}, 0, 2));
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearestJava(
                    new float[]{0, 0, 0, 0, 0, invalid}, 2, 3, new float[]{0, 0, 0}, 0, 1));
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearestJava(
                    new float[0], 0, 3, new float[]{0, 0, 0, 100, 0, invalid}, 0, 2));
        }
    }

    @Test void javaPruningMatchesUnprunedOracleForFiniteBitPatterns() {
        differential(false);
    }

    @Test void nativePruningIndependentlyMatchesUnprunedOracle() {
        String library = System.getProperty("m3.ncm.library");
        assumeTrue(library != null && !library.isBlank(), "explicit NCM native library not supplied");
        M3NcmQuantizer.loadNative(Path.of(library));
        differential(true);
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearest0(
                    new float[]{0, 0, 0}, 1, 3, new float[]{0, 0, 0, 100, 0, invalid}, 0, 2));
            assertThrows(IllegalArgumentException.class, () -> M3NcmNative.nearest0(
                    new float[0], 0, 3, new float[]{0, 0, 0, 100, 0, invalid}, 0, 2));
        }
    }

    private static void differential(boolean nativeBackend) {
        Random random = new Random(0x4e434d5052554e45L);
        for (int trial = 0; trial < 4000; trial++) {
            int rows = random.nextInt(10);
            int channels = 1 + random.nextInt(48);
            int size = 1 + random.nextInt(60);
            int offset = 1 + random.nextInt(4);
            float[] query = new float[rows * channels];
            float[] books = new float[offset + size * channels + 2];
            for (int i = 0; i < query.length; i++) query[i] = finiteRandom(random);
            for (int i = offset; i < books.length - 2; i++) books[i] = finiteRandom(random);
            books[0] = Float.NaN;
            books[books.length - 1] = Float.POSITIVE_INFINITY;
            if (trial % 5 == 0 && rows != 0) {
                System.arraycopy(query, 0, books, offset, channels);
            }
            if (trial % 7 == 0 && size > 1) {
                System.arraycopy(books, offset, books, offset + channels, channels);
            }
            float[] beforeQuery = query.clone();
            float[] beforeBooks = books.clone();
            int[] expected = unpruned(query, rows, channels, books, offset, size);
            int[] actual = nativeBackend
                    ? M3NcmNative.nearest0(query, rows, channels, books, offset, size)
                    : M3NcmNative.nearestJava(query, rows, channels, books, offset, size);
            assertArrayEquals(expected, actual, "unpruned distance case " + trial);
            assertArrayEquals(beforeQuery, query);
            assertArrayEquals(beforeBooks, books);
        }
    }

    private static float finiteRandom(Random random) {
        float value;
        do { value = Float.intBitsToFloat(random.nextInt()); } while (!Float.isFinite(value));
        return value;
    }

    private static int[] unpruned(float[] query, int rows, int channels,
                                  float[] books, int offset, int size) {
        int[] answer = new int[rows];
        for (int row = 0; row < rows; row++) {
            double best = Double.POSITIVE_INFINITY;
            for (int code = 0; code < size; code++) {
                double distance = 0.0;
                for (int channel = 0; channel < channels; channel++) {
                    double delta = (double) query[row * channels + channel]
                            - (double) books[offset + code * channels + channel];
                    double square = delta * delta;
                    distance = distance + square;
                }
                if (distance < best) {
                    best = distance;
                    answer[row] = code;
                }
            }
        }
        return answer;
    }
}
