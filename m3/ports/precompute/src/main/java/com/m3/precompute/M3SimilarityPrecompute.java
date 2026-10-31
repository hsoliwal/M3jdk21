// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.Objects;

/** One immutable pairwise comparison of safe lower bounds and approximate similarity sketches. */
public record M3SimilarityPrecompute(
        int utf16LengthDelta,
        int editLowerBound,
        int simHashDistance,
        double estimatedJaccard,
        boolean mayEqual,
        boolean anagram) {

    public M3SimilarityPrecompute {
        if (utf16LengthDelta < 0
                || editLowerBound < 0
                || simHashDistance < 0
                || estimatedJaccard < 0.0
                || estimatedJaccard > 1.0) {
            throw new IllegalArgumentException("invalid similarity precompute");
        }
    }

    public static M3SimilarityPrecompute compare(
            M3TextSignals left,
            M3TextSignals right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return new M3SimilarityPrecompute(
                (int)
                        Math.abs(
                                (long) left.metrics().utf16Length()
                                        - right.metrics().utf16Length()),
                left.editLowerBound(right),
                left.simHashDistance(right),
                left.estimatedJaccard(right),
                left.mayEqual(right),
                left.isAnagramOf(right));
    }
}
