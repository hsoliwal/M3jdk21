/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.llm.model;

import java.nio.file.Path;
import java.util.Objects;

/** Explicitly loaded, primitive-only acceleration of the scalar nearest-code oracle. */
final class M3NcmNative {
    private static volatile boolean available;
    private static Path loadedPath;

    private M3NcmNative() { }

    static synchronized void load(Path library) {
        Path path = Objects.requireNonNull(library, "library").toAbsolutePath().normalize();
        if (available) {
            if (!path.equals(loadedPath)) {
                throw new IllegalStateException("a different NCM native library is already loaded");
            }
            return;
        }
        System.load(path.toString());
        if (abiVersion0() != 1) {
            throw new UnsatisfiedLinkError("unsupported NCM native ABI");
        }
        loadedPath = path;
        available = true;
    }

    static boolean available() {
        return available;
    }

    /**
     * Scalar oracle: squared Euclidean distance accumulated in ascending channel order
     * using binary64 multiplication and addition. Exact ties select the smallest code
     * index; signed zeros compare equal. Neither input is changed or retained.
     * All used components are validated before search. Later candidates stop once
     * their nonnegative partial sum reaches the incumbent distance: subsequent
     * additions cannot lower that sum, and a later code loses every exact tie.
     * A zero-distance winner also ends the row search after that full validation.
     */
    static int[] nearestJava(float[] vectors, int rows, int channels,
                             float[] codebooks, int offset, int size) {
        validate(vectors, rows, channels, codebooks, offset, size);
        int[] selected = new int[rows];
        for (int row = 0; row < rows; row++) {
            int inputBase = row * channels;
            double bestDistance = Double.POSITIVE_INFINITY;
            int best = 0;
            for (int code = 0; code < size; code++) {
                int codeBase = offset + code * channels;
                double distance = 0.0;
                for (int channel = 0; channel < channels; channel++) {
                    double difference = (double) vectors[inputBase + channel]
                            - (double) codebooks[codeBase + channel];
                    double square = difference * difference;
                    distance = distance + square;
                    if (distance >= bestDistance) {
                        break;
                    }
                }
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = code;
                    if (bestDistance == 0.0) {
                        break;
                    }
                }
            }
            selected[row] = best;
        }
        return selected;
    }

    private static void validate(float[] vectors, int rows, int channels,
                                 float[] codebooks, int offset, int size) {
        Objects.requireNonNull(vectors, "vectors");
        Objects.requireNonNull(codebooks, "codebooks");
        if (rows < 0 || channels <= 0 || size <= 0 || offset < 0
                || (long) rows * channels != vectors.length
                || (long) offset + (long) size * channels > codebooks.length) {
            throw new IllegalArgumentException("invalid nearest-code dimensions or range");
        }
        for (float value : vectors) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("nonfinite vector component");
            }
        }
        int end = offset + size * channels;
        for (int i = offset; i < end; i++) {
            if (!Float.isFinite(codebooks[i])) {
                throw new IllegalArgumentException("nonfinite codebook component");
            }
        }
    }

    static native int abiVersion0();

    /** Native entry independently validates every dimension and used component. */
    static native int[] nearest0(float[] vectors, int rows, int channels,
                                 float[] codebooks, int offset, int size);
}
