// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexLegacyCollections}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexLegacyCollections.java}
 * blob {@code f4edf72ef7608d4e673b081e7774be089b57bc81} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 8b415bf01ca14d8cfcb78f7c23de42c7b35543bbcfabe989b53acb9e8e9f697e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3LegacyCollections}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S02}.
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
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexList<E>, MIndexCompositeIndex) (return type)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexDeque<E>, MIndexCompositeIndex) (return type)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexPriorityQueue<E>, MIndexCompositeIndex) (parameter source)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexBag<E>, MIndexCompositeIndex) (return type)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexSet<E>, MIndexCompositeIndex) (return type)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexMap<K, V>, MIndexCompositeIndex) (return type)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexOrderedSet<E>, MIndexCompositeIndex) (parameter source)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexFrozenSet<E>, MIndexCompositeIndex) (parameter source)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexFrozenMap<K, V>, MIndexCompositeIndex) (return type)}</li>
 *   <li>{@code member canonicalize(com.synexia.mindex.collection.MIndexTuple<E>, MIndexCompositeIndex) (return type)}</li>
 * </ul>
 */
public final class M3LegacyCollections {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3LegacyCollections() {
        throw new M3NotYetReceived("M3LegacyCollections#<init>/0");
    }
}
