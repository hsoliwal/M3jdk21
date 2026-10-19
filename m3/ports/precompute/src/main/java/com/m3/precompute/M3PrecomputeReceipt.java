// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexPrecomputeReceipt}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexPrecomputeReceipt.java}
 * blob {@code 2de83dda63ff0ea7144c418f2f927ea43e524ee2} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 36c46c23238f5cb08c40aa7b66be750980ac95f68e707217b21b4bf3b670d03a}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3PrecomputeReceipt}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 */
public record M3PrecomputeReceipt(long requestedPlanMask, long executedPlanMask, int stringProfileMask, int canonicalStrings, int vocabularyEntries, int astKinds, int regexAutomata, int regexStates, int astExactNodes, int astShapes, long retainedStringPrecomputeBytes, int compiledImageWords, boolean nativeBatchAcceleration, long fingerprint64) {
    public M3PrecomputePlan requestedPlan() {
        throw new M3NotYetReceived("M3PrecomputeReceipt#requestedPlan/0");
    }

    public M3PrecomputePlan executedPlan() {
        throw new M3NotYetReceived("M3PrecomputeReceipt#executedPlan/0");
    }
}
