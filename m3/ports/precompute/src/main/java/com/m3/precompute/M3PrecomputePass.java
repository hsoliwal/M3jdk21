// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexPrecomputePass}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java}
 * blob {@code 4c8176e37704f7d28faf24f043c6e5273e9635ec} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 4494b40289d89f17c0cc1dd6b8aff313090d63f88973cbe6c47282899ecb662d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3PrecomputePass}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S24}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 */
public interface M3PrecomputePass<T> {
    M3PrecomputeKey<T> output();

    default List<M3PrecomputeKey<?>> dependencies() {
        throw new M3NotYetReceived("M3PrecomputePass#dependencies/0");
    }

    String implementationRevision();

    T compute(M3PrecomputeContext context);

    long retainedBytes(T value);

    String contentSha256(T value);
}
