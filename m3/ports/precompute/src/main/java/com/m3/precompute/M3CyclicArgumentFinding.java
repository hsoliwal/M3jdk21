// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexCyclicArgumentFinding}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexCyclicArgumentFinding.java}
 * blob {@code 6f0a863e58515f2fb2fd75b11982a0d653299ec8} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 30c59994ff42e744d385a581e024e0ab76d638cf71e1be9141148274e6692b03}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3CyclicArgumentFinding}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S21}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Kind}</li>
 *   <li>{@code record component fallacy erased to Object (source type MIndexReasoningLang.Fallacy)}</li>
 *   <li>{@code record component kind erased to Object (source type Kind)}</li>
 * </ul>
 */
public record M3CyclicArgumentFinding(Object fallacy, Object kind, int primaryRow, int secondaryRow, long subjectStatementId, long relatedStatementId, long responderStatementId, long provenanceSourceId, long confidenceQ32) {
    public boolean explicit() {
        throw new M3NotYetReceived("M3CyclicArgumentFinding#explicit/0");
    }
}
