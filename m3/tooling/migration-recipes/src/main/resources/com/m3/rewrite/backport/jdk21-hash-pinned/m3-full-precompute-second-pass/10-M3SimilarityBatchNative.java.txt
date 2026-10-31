// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/** Optional JNI accelerator for approximate candidate ranking. */
public final class M3SimilarityBatchNative implements M3SimilarityBatch {
    private M3SimilarityBatchNative() {}

    public static M3SimilarityBatchNative loadRequired() {
        M3CodeTextSignalBatchNative.loadRequired();
        return new M3SimilarityBatchNative();
    }

    @Override
    public String name() {
        return "jni-c11";
    }

    @Override
    public boolean accelerated() {
        return true;
    }

    @Override
    public int[] scores(
            int queryUtf16Length,
            long querySimHash,
            int[] candidateUtf16Lengths,
            long[] candidateSimHashes,
            Limits limits,
            M3Progress monitor) {
        if (!M3CodeTextSignalBatchNative.isLoaded()) {
            throw new IllegalStateException("M3 precompute JNI library is not loaded");
        }
        M3SimilarityBatchJava.validate(
                queryUtf16Length, candidateUtf16Lengths, candidateSimHashes, limits);
        M3Progress progress = monitor == null ? M3Progress.none() : monitor;
        progress.begin("M3 JNI similarity candidate scores", candidateUtf16Lengths.length);
        try {
            progress.checkCanceled();
            int[] result =
                    nativeScores(
                            queryUtf16Length,
                            querySimHash,
                            candidateUtf16Lengths,
                            candidateSimHashes);
            if (result == null || result.length != candidateUtf16Lengths.length) {
                throw new IllegalStateException("invalid JNI similarity result");
            }
            progress.worked(result.length);
            progress.checkCanceled();
            return result;
        } finally {
            progress.done();
        }
    }

    private static native int[] nativeScores(
            int queryUtf16Length,
            long querySimHash,
            int[] candidateUtf16Lengths,
            long[] candidateSimHashes);
}
