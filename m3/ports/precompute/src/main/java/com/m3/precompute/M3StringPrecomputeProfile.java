// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexStringPrecomputeProfile}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexStringPrecomputeProfile.java}
 * blob {@code 3bf61751db3acbfd50f687e030bc9d6d98e49f84} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 45527b9177122a21303ecfc9c3d398c1fc6e6a2e7d8dea048ceba3696f619e11}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3StringPrecomputeProfile}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code constant METADATA (M3StringPrecomputeProfile)}</li>
 *   <li>{@code constant CORE (M3StringPrecomputeProfile)}</li>
 *   <li>{@code constant RANGE_HASH (M3StringPrecomputeProfile)}</li>
 *   <li>{@code constant LITERAL_PATTERN (M3StringPrecomputeProfile)}</li>
 *   <li>{@code constant SEARCH (M3StringPrecomputeProfile)}</li>
 *   <li>{@code constant INDEXED (M3StringPrecomputeProfile)}</li>
 *   <li>{@code constant FULL (M3StringPrecomputeProfile)}</li>
 * </ul>
 */
public record M3StringPrecomputeProfile(int mask) {
    public static final int HISTOGRAM = 1;

    public static final int PREFIX_FUNCTION = 1 << 1;

    public static final int Z_FUNCTION = 1 << 2;

    public static final int PALINDROME = 1 << 3;

    public static final int ROLLING_HASH = 1 << 4;

    public static final int SKETCH = 1 << 5;

    public static final int SUFFIX_ARRAY = 1 << 6;

    public boolean has(int lane) {
        throw new M3NotYetReceived("M3StringPrecomputeProfile#has/1");
    }

    public boolean includes(M3StringPrecomputeProfile other) {
        throw new M3NotYetReceived("M3StringPrecomputeProfile#includes/1");
    }

    public M3StringPrecomputeProfile union(M3StringPrecomputeProfile other) {
        throw new M3NotYetReceived("M3StringPrecomputeProfile#union/1");
    }
}
