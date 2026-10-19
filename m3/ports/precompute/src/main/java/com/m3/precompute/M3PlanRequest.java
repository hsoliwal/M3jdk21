// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.foundation.catalog.MIndexPlanRequest}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation/catalog/MIndexPlanRequest.java}
 * blob {@code 40b9426f593675e357f2e1239863495eb0cafc8f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f014eb9226e24dbe613732f185675ec7d4136efd2dfd02b0583ac96473c6fd90}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.foundation.catalog.M3PlanRequest}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S19}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component weights erased to Object (source type MIndexProfile)}</li>
 *   <li>{@code member allows(MIndexDefinitionKind) (parameter kind)}</li>
 * </ul>
 */
public record M3PlanRequest(String inputSchema, String outputSchema, long availableFacts, long requiredFeatures, long allowedEffects, int elements, int maxSteps, boolean exactRequired, boolean allowAlgorithms, boolean allowTools, Object weights) {
    public static M3PlanRequest between(String inputSchema, String outputSchema, int elements, int maxSteps) {
        throw new M3NotYetReceived("M3PlanRequest#between/4");
    }
}
