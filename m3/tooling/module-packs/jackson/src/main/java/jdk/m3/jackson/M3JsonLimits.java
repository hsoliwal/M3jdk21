// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson;

import java.io.Serializable;

/** Explicit bounds for retained AST state and expanded JSON traversal work. */
public record M3JsonLimits(long maxTokens, int maxAtoms, long maxChildReferences,
                           long maxTextUnits, int maxDepth) implements Serializable {
    /** Bounded defaults; callers may supply an independently chosen larger budget. */
    public static final M3JsonLimits DEFAULT =
            new M3JsonLimits(1_000_000, 1_000_000, 2_000_000, 16_777_216, 256);

    /**
     * Explicit workload budget for the optional 100 MiB JSON qualification lane.
     * These are resource ceilings, not a guarantee for every possible 100 MiB shape.
     * A pathological input may need a larger budget or additional memory.
     */
    public static M3JsonLimits for100MiB() {
        return new M3JsonLimits(40_000_000L, 8_000_000, 16_000_000L,
                160_000_000L, 512);
    }

    /** Every bound must be positive; atom/child storage uses Java array indexes. */
    public M3JsonLimits {
        if (maxTokens < 1 || maxAtoms < 1 || maxChildReferences < 1
                || maxChildReferences > Integer.MAX_VALUE - 8L
                || maxTextUnits < 1 || maxDepth < 1) {
            throw new IllegalArgumentException("all JSON bounds must be positive; child indexes must fit Java arrays");
        }
    }
}
