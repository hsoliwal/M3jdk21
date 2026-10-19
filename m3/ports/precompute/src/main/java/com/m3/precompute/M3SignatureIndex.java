// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexSignatureIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexSignatureIndex.java}
 * blob {@code 976e31fe74ae41b052b1f53b3f879c3370240e21} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code eaec8df62ab8e6eba97ee8cb97f77df9f47e478f2afba248b5325212526ffbd3}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3SignatureIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S25}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Draft, Builder}</li>
 *   <li>{@code member builder() (return type)}</li>
 * </ul>
 */
public final class M3SignatureIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code ownerIds} of type {@code long[]}. */
    private long[] ownerIds;

    /** primitive lane for source field {@code memberOffsets} of type {@code int[]}. */
    private int[] memberOffsets;

    /** primitive lane for source field {@code memberIds} of type {@code long[]}. */
    private long[] memberIds;

    /** primitive lane for source field {@code fingerprints} of type {@code long[]}. */
    private long[] fingerprints;

    /** primitive lane for source field {@code memberDictionary} of type {@code long[]}. */
    private long[] memberDictionary;

    /** primitive lane for source field {@code postingOffsets} of type {@code int[]}. */
    private int[] postingOffsets;

    /** primitive lane for source field {@code postingOwnerRows} of type {@code int[]}. */
    private int[] postingOwnerRows;

    /** primitive lane for source field {@code slots} of type {@code long[]}. */
    private long[] slots;

    private M3SignatureIndex() {
        throw new M3NotYetReceived("M3SignatureIndex#<init>/0");
    }

    public int ownerCount() {
        throw new M3NotYetReceived("M3SignatureIndex#ownerCount/0");
    }

    public boolean containsOwner(long ownerId) {
        throw new M3NotYetReceived("M3SignatureIndex#containsOwner/1");
    }

    public int memberCount(long ownerId) {
        throw new M3NotYetReceived("M3SignatureIndex#memberCount/1");
    }

    public long memberAt(long ownerId, int ordinal) {
        throw new M3NotYetReceived("M3SignatureIndex#memberAt/2");
    }

    public long[] members(long ownerId) {
        throw new M3NotYetReceived("M3SignatureIndex#members/1");
    }

    public long fingerprint(long ownerId) {
        throw new M3NotYetReceived("M3SignatureIndex#fingerprint/1");
    }

    public boolean containsMember(long ownerId, long memberId) {
        throw new M3NotYetReceived("M3SignatureIndex#containsMember/2");
    }

    public boolean isSubSignature(long leftOwnerId, long rightOwnerId) {
        throw new M3NotYetReceived("M3SignatureIndex#isSubSignature/2");
    }

    public boolean overlaps(long leftOwnerId, long rightOwnerId) {
        throw new M3NotYetReceived("M3SignatureIndex#overlaps/2");
    }

    public int intersectionCount(long leftOwnerId, long rightOwnerId) {
        throw new M3NotYetReceived("M3SignatureIndex#intersectionCount/2");
    }

    public long[] ownersContaining(long memberId) {
        throw new M3NotYetReceived("M3SignatureIndex#ownersContaining/1");
    }

    public long[] minimalUnion(long... requestedOwnerIds) {
        throw new M3NotYetReceived("M3SignatureIndex#minimalUnion/1");
    }

    public long[] ownerIds() {
        throw new M3NotYetReceived("M3SignatureIndex#ownerIds/0");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3SignatureIndex#retainedPayloadBytes/0");
    }
}
