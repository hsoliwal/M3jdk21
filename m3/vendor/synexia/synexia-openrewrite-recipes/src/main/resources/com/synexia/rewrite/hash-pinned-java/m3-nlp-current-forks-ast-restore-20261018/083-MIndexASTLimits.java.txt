// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

/** Explicit admission limits; no process-global AST retention is required. */
public record MIndexASTLimits(int nodes, int edges, int attributes, int symbols,
        int tokens, long symbolChars, int depth, int imageBytes, int sourceChars) {
    public MIndexASTLimits {
        if (nodes < 1 || edges < 0 || attributes < 0 || symbols < 1 || tokens < 1
                || symbolChars < 1 || depth < 1 || imageBytes < 32 || sourceChars < 1) {
            throw new IllegalArgumentException("invalid AST limits");
        }
    }

    /** Bounded defaults; callers may select a smaller or larger explicit envelope. */
    public static MIndexASTLimits defaults() {
        return new MIndexASTLimits(100_000, 500_000, 500_000, 200_000,
                300_000, 16_000_000L, 10_000, 64_000_000, 4_000_000);
    }
}
