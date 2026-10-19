// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.collections.MIndexAffixCollection}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/collections/MIndexAffixCollection.java}
 * blob {@code b276c49860604a79cfa25e225075f4b8e4da9439} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 42d5dff15b0916c2f1d5a1b35e23d128babb675910eef343d0b500e5cc2ea579}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.collections.M3AffixCollection}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S04}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code source supertypes not received: implements MIndexCollection}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 * </ul>
 */
abstract class M3AffixCollection {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code catalog} of type {@code MIndexCollectionCatalog}. */
    private int[] catalogLane;

    /** primitive lane for source field {@code reversed} of type {@code boolean}. */
    private boolean reversed;

    private M3AffixCollection() {
        throw new M3NotYetReceived("M3AffixCollection#<init>/0");
    }

    public final M3CollectionCatalog catalog() {
        throw new M3NotYetReceived("M3AffixCollection#catalog/0");
    }

    public final M3Rows matching(CharSequence affix) {
        throw new M3NotYetReceived("M3AffixCollection#matching/1");
    }
}
