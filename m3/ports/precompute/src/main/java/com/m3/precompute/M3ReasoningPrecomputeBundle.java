// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexReasoningPrecomputeBundle}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexReasoningPrecomputeBundle.java}
 * blob {@code dc0bc282e4db96ac52d5bdabfa9d213789ce5c7c} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code fec0ac8e09966a150786d2abef891b9f03add0baf944e7c8cb37f735c37fcec9}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3ReasoningPrecomputeBundle}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S25}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component semantic erased to Object (source type MIndexSemanticPrecomputeBundle)}</li>
 *   <li>{@code record component conditionals erased to Object (source type MIndexConditionalTable)}</li>
 *   <li>{@code record component causal erased to Object (source type MIndexCausalModel)}</li>
 * </ul>
 */
public record M3ReasoningPrecomputeBundle(Object semantic, Object conditionals, Object causal) {
    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3ReasoningPrecomputeBundle#retainedPayloadBytes/0");
    }
}
