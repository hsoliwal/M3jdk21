// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexNodeRef}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexNodeRef.java}
 * blob {@code 14c9f2d4f041f44871f53451895872446e4d613e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b804163f4a9fe46377767d6e2785ebfe1cccc1250a215b26fa6cd4971b59055e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3NodeRef}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code record component store erased to Object (source type MIndexStructuralStore)}</li>
 * </ul>
 */
public record M3NodeRef(Object store, int id) {
    public M3NodeKind kind() {
        throw new M3NotYetReceived("M3NodeRef#kind/0");
    }

    public long semanticHash64() {
        throw new M3NotYetReceived("M3NodeRef#semanticHash64/0");
    }
}
