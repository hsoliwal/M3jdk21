// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexPathIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexPathIndex.java}
 * blob {@code 641e10cde43e821cae4bb9641c5f09c24d3c4f12} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f09f8b4f00e0c2e4862efafa40ec37c2f2ea0cabfca23dc91308c5479977538d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3PathIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Co-donors with the same proposed name (BLOB_DIFF, not merged, review required):
 * <ul>
 *   <li>{@code synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation/MIndexPathIndex.java(at)720e5f63d3d2}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Builder}</li>
 *   <li>{@code member builder() (return type)}</li>
 *   <li>{@code member segmentKind(int, int) (return type)}</li>
 *   <li>{@code member indexOf(MIndexPath) (parameter path)}</li>
 *   <li>{@code member contains(MIndexPath) (parameter path)}</li>
 *   <li>{@code member descendantsOf(MIndexPath) (parameter prefix)}</li>
 *   <li>{@code member descendantCount(MIndexPath) (parameter prefix)}</li>
 *   <li>{@code member startsWith(int, MIndexPath) (parameter prefix)}</li>
 *   <li>{@code member commonPrefixLength(int, MIndexPath) (parameter other)}</li>
 * </ul>
 */
public final class M3PathIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code offsets} of type {@code int[]}. */
    private int[] offsets;

    /** primitive lane for source field {@code segmentIds} of type {@code int[]}. */
    private int[] segmentIds;

    /** primitive lane for source field {@code segmentKinds} of type {@code byte[]}. */
    private byte[] segmentKinds;

    /** primitive lane for source field {@code hashes} of type {@code long[]}. */
    private long[] hashes;

    /** primitive lane for source field {@code exactSlots} of type {@code int[]}. */
    private int[] exactSlots;

    /** primitive lane for source field {@code lexicalRows} of type {@code int[]}. */
    private int[] lexicalRows;

    private M3PathIndex() {
        throw new M3NotYetReceived("M3PathIndex#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3PathIndex#size/0");
    }

    public int depth(int pathIndex) {
        throw new M3NotYetReceived("M3PathIndex#depth/1");
    }

    public int segmentContentIndex(int pathIndex, int segmentIndex) {
        throw new M3NotYetReceived("M3PathIndex#segmentContentIndex/2");
    }

    public long structuralHash64(int pathIndex) {
        throw new M3NotYetReceived("M3PathIndex#structuralHash64/1");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3PathIndex#primitivePayloadBytes/0");
    }
}
