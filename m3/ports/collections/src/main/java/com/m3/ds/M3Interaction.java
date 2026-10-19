// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.graph.MIndexInteraction}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/graph/MIndexInteraction.java}
 * blob {@code 35d3610a78b5b66f8efb0be9df8b25c6e8aca3d6} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2b00d04d01301f78195a01e8513442932186c401c01ab1b5d8ed560613f69917}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.graph.M3Interaction}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S08}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component left erased to Object (source type MIndexCoordinate)}</li>
 *   <li>{@code record component leftRole erased to Object (source type MIndexRole)}</li>
 *   <li>{@code record component right erased to Object (source type MIndexCoordinate)}</li>
 *   <li>{@code record component rightRole erased to Object (source type MIndexRole)}</li>
 *   <li>{@code record component relation erased to Object (source type MIndexString)}</li>
 * </ul>
 */
public record M3Interaction(int id, Object left, Object leftRole, Object right, Object rightRole, M3InteractionKind kind, Object relation, int flags, long structuralHash64) {
    public static final int DIRECTED = 1 << 0;

    public static final int SYMMETRIC = 1 << 1;

    public static final int EXPLICIT = 1 << 2;

    public static final int INFERRED = 1 << 3;

    public static final int SUPPORTING = 1 << 4;

    public static final int CONTRADICTING = 1 << 5;

    public boolean hasFlag(int mask) {
        throw new M3NotYetReceived("M3Interaction#hasFlag/1");
    }
}
