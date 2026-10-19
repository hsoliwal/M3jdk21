// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.precompute.MIndexPostingIntersectionKernel}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/precompute-api/src/main/java/com/synexia/mindex/precompute/MIndexPostingIntersectionKernel.java}
 * blob {@code 82a55024c12b23a8ad0faa48fc556307a3edc6f0} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code c4eb30ead09d13b7ea909d029258e7bd4519e5305048adcdf4197fe4e268af65}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.precompute.M3PostingIntersectionKernel}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S19}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: JavaKernel}</li>
 * </ul>
 */
public interface M3PostingIntersectionKernel {
    String id();

    boolean nativeBackend();

    int[] intersectSorted(int[] left, int leftFrom, int leftTo, int[] right, int rightFrom, int rightTo, int limit);

    static M3PostingIntersectionKernel javaKernel() {
        throw new M3NotYetReceived("M3PostingIntersectionKernel#javaKernel/0");
    }
}
