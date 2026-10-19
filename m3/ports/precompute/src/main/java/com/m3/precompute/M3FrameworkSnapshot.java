// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.framework.MIndexFrameworkSnapshot}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/framework/src/main/java/com/synexia/mindex/framework/MIndexFrameworkSnapshot.java}
 * blob {@code f7682b127548572fe8edd957b1d9c44807f739aa} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code dd2b00e4367320f1e5b526b4bf68501207d4abc4016dfb911f7474f11ab6bbf6}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.framework.M3FrameworkSnapshot}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 */
public record M3FrameworkSnapshot(int templateCount, int toolCount, long libraryFingerprint) {
}
