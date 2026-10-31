// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Objects;

/** Canonical Java approximate similarity scorer. */
public final class M3SimilarityBatchJava implements M3SimilarityBatch {
    public static final M3SimilarityBatchJava INSTANCE = new M3SimilarityBatchJava();

    private M3SimilarityBatchJava() {}

    @Override
    public String name() {
        return "java-cpu";
    }

    @Override
    public boolean accelerated() {
        return false;
    }

    @Override
    public int[] scores(
            int queryUtf16Length,
            long querySimHash,
            int[] candidateUtf16Lengths,
            long[] candidateSimHashes,
            Limits limits,
            M3Progress monitor) {
        validate(queryUtf16Length, candidateUtf16Lengths, candidateSimHashes, limits);
        M3Progress progress = monitor == null ? M3Progress.none() : monitor;
        int[] result = new int[candidateUtf16Lengths.length];
        progress.begin("M3 similarity candidate scores", result.length);
        try {
            for (int row = 0; row < result.length; row++) {
                if ((row & 1023) == 0) progress.checkCanceled();
                result[row] =
                        score(
                                queryUtf16Length,
                                querySimHash,
                                candidateUtf16Lengths[row],
                                candidateSimHashes[row]);
                progress.worked(1);
            }
            progress.checkCanceled();
            return result;
        } finally {
            progress.done();
        }
    }

    static void validate(
            int queryUtf16Length,
            int[] candidateUtf16Lengths,
            long[] candidateSimHashes,
            Limits limits) {
        if (queryUtf16Length < 0) throw new IllegalArgumentException("negative query length");
        Objects.requireNonNull(candidateUtf16Lengths, "candidateUtf16Lengths");
        Objects.requireNonNull(candidateSimHashes, "candidateSimHashes");
        Objects.requireNonNull(limits, "limits");
        if (candidateUtf16Lengths.length != candidateSimHashes.length
                || candidateUtf16Lengths.length > limits.maxRows()) {
            throw new IllegalArgumentException("similarity row geometry");
        }
        long transfer =
                Math.addExact(
                        4L * candidateUtf16Lengths.length,
                        Math.addExact(
                                8L * candidateSimHashes.length,
                                4L * candidateUtf16Lengths.length));
        if (transfer > limits.maxTransferBytes()) {
            throw new IllegalArgumentException("similarity transfer budget exceeded");
        }
        if (64L * candidateUtf16Lengths.length > limits.maxWork()) {
            throw new IllegalArgumentException("similarity work budget exceeded");
        }
        for (int length : candidateUtf16Lengths) {
            if (length < 0) throw new IllegalArgumentException("negative candidate length");
        }
    }

    static int score(
            int queryUtf16Length,
            long querySimHash,
            int candidateUtf16Length,
            long candidateSimHash) {
        int hamming = Long.bitCount(querySimHash ^ candidateSimHash);
        long delta = Math.abs((long) queryUtf16Length - candidateUtf16Length);
        return hamming * 1024 + (int) Math.min(delta, 1023L);
    }
}
