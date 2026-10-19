// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexSourceSpan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexSourceSpan.java}
 * blob {@code 738d902d0191d7d87f3178d1e7f90690a3184083} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 21552e8d499ce090e87c603242e745ff6664869605126dd67aec798be3019364}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3SourceSpan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code constant UNKNOWN (M3SourceSpan)}</li>
 * </ul>
 */
public record M3SourceSpan(long startInclusive, long endExclusive, int line, int column) {
    public static M3SourceSpan of(long startInclusive, long endExclusive) {
        throw new M3NotYetReceived("M3SourceSpan#of/2");
    }

    public boolean known() {
        throw new M3NotYetReceived("M3SourceSpan#known/0");
    }
}
