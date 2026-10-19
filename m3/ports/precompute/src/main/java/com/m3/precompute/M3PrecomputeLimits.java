// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexPrecomputeLimits}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java}
 * blob {@code 663ff20f394e4fb190341bb4e66b37df16369af4} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 51cc3fea9d768172564d4870c7c006a574a64a250218eda25bfb63cc0620c095}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3PrecomputeLimits}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S24}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Work}</li>
 *   <li>{@code constant DEFAULT (M3PrecomputeLimits)}</li>
 * </ul>
 */
public record M3PrecomputeLimits(int maxNodes, int maxEdges, long maxCells, long maxWork, long maxBytes) {
}
