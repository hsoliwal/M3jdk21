// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexCollectionMetadata}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexCollectionMetadata.java}
 * blob {@code 16abe7831d0453948e425ae08ad2936724b7141f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 356c9b76608358ae65f437986df5db1ffd35a4771eea18cb3a24a9b66285c7d9}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3CollectionMetadata}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
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
 *   <li>{@code nested types not received: Role}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member compositeIndex() (return type)}</li>
 *   <li>{@code member hasRole(Role) (parameter role)}</li>
 *   <li>{@code member column(Role) (return type)}</li>
 *   <li>{@code member relations() (return type)}</li>
 *   <li>{@code member tableColumn(int) (return type)}</li>
 *   <li>{@code member tableRelations(int, int) (return type)}</li>
 * </ul>
 */
public final class M3CollectionMetadata {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexCanonicalCollection}. */
    private int[] sourceLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code first} of type {@code MIndexIdIndex}. */
    private int[] firstLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code second} of type {@code MIndexIdIndex}. */
    private int[] secondLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code third} of type {@code MIndexIdIndex}. */
    private int[] thirdLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code firstRole} of type {@code Role}. */
    private int[] firstRoleLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code relations} of type {@code MIndexRelations}. */
    private int[] relationsLane;

    private M3CollectionMetadata() {
        throw new M3NotYetReceived("M3CollectionMetadata#<init>/0");
    }

    public int canonicalId() {
        throw new M3NotYetReceived("M3CollectionMetadata#canonicalId/0");
    }

    public int kind() {
        throw new M3NotYetReceived("M3CollectionMetadata#kind/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3CollectionMetadata#size/0");
    }

    public long structuralHash64() {
        throw new M3NotYetReceived("M3CollectionMetadata#structuralHash64/0");
    }

    public long signal64() {
        throw new M3NotYetReceived("M3CollectionMetadata#signal64/0");
    }

    public long primitiveBytes() {
        throw new M3NotYetReceived("M3CollectionMetadata#primitiveBytes/0");
    }
}
