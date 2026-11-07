// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.internal.m3.collections.M3PoolProgressMonitor;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Exact second stage. A lossy signal may only shortlist candidates; semantic truth stays in the
 * supplied exact predicate.
 */
//FLLDCIM: RELOCATE - donor blob 9f0ef929e435 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public final class M3ObjectSignalMatcher {
    private M3ObjectSignalMatcher() { }

    public static <T> M3ObjectSignalExactResult verify(
            List<? extends T> values,
            M3ObjectSignalFirstPassResult firstPass,
            Predicate<? super T> exactPredicate,
            M3PoolProgressMonitor monitor) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(firstPass, "firstPass");
        Objects.requireNonNull(exactPredicate, "exactPredicate");
        Objects.requireNonNull(monitor, "monitor");
        if (values.size() != firstPass.candidates().size()) {
            throw new IllegalArgumentException("value/candidate size mismatch");
        }

        long[] exactWords = new long[(values.size() + 63) >>> 6];
        int exactChecks = 0;
        long[] survivors = firstPass.candidates().copyWords();
        monitor.begin("Verify object-signal survivors exactly", firstPass.afterMetadata());
        try {
            for (int wordIndex = 0; wordIndex < survivors.length; wordIndex++) {
                long bits = survivors[wordIndex];
                while (bits != 0L) {
                    monitor.checkCancelled();
                    int bit = Long.numberOfTrailingZeros(bits);
                    int index = (wordIndex << 6) + bit;
                    exactChecks++;
                    if (exactPredicate.test(values.get(index))) {
                        exactWords[wordIndex] |= 1L << bit;
                    }
                    bits &= bits - 1L;
                    monitor.worked(1);
                }
            }
        } finally {
            monitor.done();
        }

        return new M3ObjectSignalExactResult(
            firstPass,
            new M3ObjectSignalBitmap(exactWords, values.size(), true),
            exactChecks);
    }
}
