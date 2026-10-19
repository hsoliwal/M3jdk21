// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexPrecomputeFabricReceipt}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexPrecomputeFabricReceipt.java}
 * blob {@code 366a976559d70de91ae28cd130fa638ad24506f7} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 45beaf7882caf66d6092225cc6ee0b7896073b90a7f30945570f31971ccd3e36}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3PrecomputeFabricReceipt}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member skipped(MIndexPrecomputePlan.Pass) (parameter pass)}</li>
 *   <li>{@code member skippedPasses() (return type)}</li>
 * </ul>
 */
public record M3PrecomputeFabricReceipt(long requestedPlanMask, long executedPlanMask, long skippedPlanMask, M3PrecomputeReceipt coreReceipt, int treeNodes, int treeEdges, long treeIndexBytes, long treeReachabilityBytes, int knowledgeEntities, int knowledgeComponents, int knowledgeEdges, int argumentCount, int attackCount, long argumentationBaseBytes, long argumentationAdditionalBytes, String donorProvenanceSha256, boolean coralAvailable, int coralDeviceCount, int coralUsedDeviceCount, int coralRows, int coralOutputBytes, long fingerprint64) {
    public M3PrecomputePlan requestedPlan() {
        throw new M3NotYetReceived("M3PrecomputeFabricReceipt#requestedPlan/0");
    }

    public M3PrecomputePlan executedPlan() {
        throw new M3NotYetReceived("M3PrecomputeFabricReceipt#executedPlan/0");
    }

    public boolean complete() {
        throw new M3NotYetReceived("M3PrecomputeFabricReceipt#complete/0");
    }
}
