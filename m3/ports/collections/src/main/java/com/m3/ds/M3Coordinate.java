// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.graph.MIndexCoordinate}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/graph/MIndexCoordinate.java}
 * blob {@code cb5328f04c48fa05d709e5b484639024a28c06d9} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 9fe2c4a81e8da874af9ed474c16cfce56b9d4463d9b61c38e38a6fbe0a636381}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.graph.M3Coordinate}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S07}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component domain erased to Object (source type MIndexValueDomain)}</li>
 *   <li>{@code member text(long, MIndexString) (parameter value)}</li>
 * </ul>
 */
public record M3Coordinate(long scopeId, Object domain, long localId, long structuralHash64) {
    public boolean sameIdentity(M3Coordinate other) {
        throw new M3NotYetReceived("M3Coordinate#sameIdentity/1");
    }
}
