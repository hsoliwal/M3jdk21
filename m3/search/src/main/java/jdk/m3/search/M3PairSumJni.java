// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia synexia-fastsearch/com/synexia/fastsearch/problem/PairSumJni.java (blob 63b24396343a) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Opt-in JNI candidate for the Java 21 {@link M3PairSumSearch} sorted pair contract.
 *
 * <p>It accepts only a caller-selected regular native library, never loads on
 * class initialization, and holds no native pointers or caller-owned arrays.
 * Every candidate is checked against the independently executed Java oracle;
 * an incorrect candidate has no result authority. Native invocation is not
 * claimed to accelerate this validation-first mode.
 */
public final class M3PairSumJni {
    private static final int MAX_BATCH_QUERIES = 8_192;
    private static final int MAX_NATIVE_UNSORTED_ITEMS = 65_536;

    private M3PairSumJni() {}

    /** A stateless, thread-safe adapter; both methods preserve Java's exact result ABI. */
    public interface Provider {
        /** The first zero-based distinct pair packed in a long, or -1. */
        long firstSorted(int[] sorted, long target);

        /** One result per target, in query order, with no JNI-owned array aliases. */
        long[] firstSortedBatch(int[] sorted, long[] targets);

        /**
         * Unsorted search is an additive default: existing Provider implementations
         * retain their Java source and binary contracts without a new obligation.
         */
        default long firstUnsorted(int[] values, long target) {
            return M3PairSumSearch.firstUnsorted(values, target);
        }

        /** Unsorted batch, bounded exactly like its sorted counterpart. */
        default long[] firstUnsortedBatch(int[] values, long[] targets) {
            Objects.requireNonNull(values, "values");
            Objects.requireNonNull(targets, "targets");
            if (targets.length > MAX_BATCH_QUERIES) {
                throw new IllegalArgumentException("bounded JNI query batch exceeded");
            }
            return javaUnsortedBatch(values, targets);
        }
    }

    /**
     * Explicitly load one library path; no JNI dependency exists on ordinary
     * startup or when callers use {@link M3PairSumSearch} directly.
     */
    public static Provider load(Path library) {
        Path selected = Objects.requireNonNull(library, "library")
                .toAbsolutePath().normalize();
        if (!Files.isRegularFile(selected, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("native library must be a regular file");
        }
        System.load(selected.toString());
        return NativeProvider.INSTANCE;
    }

    private enum NativeProvider implements Provider {
        INSTANCE;

        @Override
        public long firstSorted(int[] sorted, long target) {
            int[] snapshot = Objects.requireNonNull(sorted, "sorted").clone();
            long expected = M3PairSumSearch.firstSorted(snapshot, target);
            return validated(expected, sortedPairNative(snapshot, target));
        }

        @Override
        public long[] firstSortedBatch(int[] sorted, long[] targets) {
            int[] snapshot = Objects.requireNonNull(sorted, "sorted").clone();
            long[] requests = Objects.requireNonNull(targets, "targets").clone();
            if (requests.length > MAX_BATCH_QUERIES) {
                throw new IllegalArgumentException("bounded JNI query batch exceeded");
            }
            // A sorted precondition violation is rejected before any native call.
            M3PairSumSearch.firstSorted(snapshot, 0L);
            return validatedBatch(snapshot, requests, sortedPairsNative(snapshot, requests));
        }

        @Override
        public long firstUnsorted(int[] values, long target) {
            int[] snapshot = Objects.requireNonNull(values, "values").clone();
            long expected = M3PairSumSearch.firstUnsorted(snapshot, target);
            if (snapshot.length > MAX_NATIVE_UNSORTED_ITEMS) return expected;
            return validated(expected, unsortedPairNative(snapshot, target));
        }

        @Override
        public long[] firstUnsortedBatch(int[] values, long[] targets) {
            int[] snapshot = Objects.requireNonNull(values, "values").clone();
            long[] requests = Objects.requireNonNull(targets, "targets").clone();
            if (requests.length > MAX_BATCH_QUERIES) {
                throw new IllegalArgumentException("bounded JNI query batch exceeded");
            }
            if (snapshot.length > MAX_NATIVE_UNSORTED_ITEMS) {
                return javaUnsortedBatch(snapshot, requests);
            }
            return validatedUnsortedBatch(snapshot, requests,
                    unsortedPairsNative(snapshot, requests));
        }
    }

    /** Isolated candidate repair, also covered with deliberately corrupt values. */
    static long validated(long javaOracle, long candidate) {
        return candidate == javaOracle ? candidate : javaOracle;
    }

    /** Reject output-shape corruption and repair incorrect native entries one by one. */
    static long[] validatedBatch(int[] sorted, long[] targets, long[] candidates) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.length != targets.length) {
            throw new IllegalArgumentException("candidate batch length mismatch");
        }
        long[] checked = candidates.clone();
        for (int index = 0; index < checked.length; index++) {
            checked[index] = validated(M3PairSumSearch.firstSorted(sorted, targets[index]),
                    checked[index]);
        }
        return checked;
    }

    /** Canonical Java fallback for oversize arrays and legacy Provider implementations. */
    private static long[] javaUnsortedBatch(int[] values, long[] targets) {
        long[] answers = new long[targets.length];
        for (int index = 0; index < targets.length; index++) {
            answers[index] = M3PairSumSearch.firstUnsorted(values, targets[index]);
        }
        return answers;
    }

    /** Candidate-only unsorted batch: a wrong packed index never gains authority. */
    static long[] validatedUnsortedBatch(int[] values, long[] targets, long[] candidates) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(targets, "targets");
        Objects.requireNonNull(candidates, "candidates");
        if (candidates.length != targets.length) {
            throw new IllegalArgumentException("candidate batch length mismatch");
        }
        long[] checked = candidates.clone();
        for (int index = 0; index < checked.length; index++) {
            checked[index] = validated(M3PairSumSearch.firstUnsorted(values, targets[index]),
                    checked[index]);
        }
        return checked;
    }

    /** Package-private raw native access exists only for the differential proof. */
    static long rawSorted(int[] sorted, long target) {
        return sortedPairNative(sorted, target);
    }

    /** Package-private raw native access exists only for the differential proof. */
    static long[] rawSortedBatch(int[] sorted, long[] targets) {
        return sortedPairsNative(sorted, targets);
    }

    static long rawUnsorted(int[] values, long target) {
        return unsortedPairNative(values, target);
    }

    static long[] rawUnsortedBatch(int[] values, long[] targets) {
        return unsortedPairsNative(values, targets);
    }

    private static native long sortedPairNative(int[] sorted, long target);

    private static native long[] sortedPairsNative(int[] sorted, long[] targets);

    private static native long unsortedPairNative(int[] values, long target);

    private static native long[] unsortedPairsNative(int[] values, long[] targets);
}
