// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexWeights}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexWeights.java}
 * blob {@code faf620c6a7b83b632f646559352b9bc627a2b871} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 7a6d36d05370e3eef9c148a8d50b95518a7d1beab644dc39000cf22e0fb7b70e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3Weights}); projection
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
 *   <li>{@code member of(MIndexIdIndex, long[]) (parameter index)}</li>
 *   <li>{@code member index() (return type)}</li>
 *   <li>{@code member mutableCopy() (return type)}</li>
 * </ul>
 */
public final class M3Weights {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code index} of type {@code MIndexIdIndex}. */
    private int[] indexLane;

    /** primitive lane for source field {@code prefix} of type {@code long[]}. */
    private long[] prefix;

    /** primitive lane for source field {@code groupedPrefix} of type {@code long[]}. */
    private long[] groupedPrefix;

    private M3Weights() {
        throw new M3NotYetReceived("M3Weights#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3Weights#size/0");
    }

    public long total() {
        throw new M3NotYetReceived("M3Weights#total/0");
    }

    public long weightAt(int position) {
        throw new M3NotYetReceived("M3Weights#weightAt/1");
    }

    public long sum(int fromInclusive, int toExclusive) {
        throw new M3NotYetReceived("M3Weights#sum/2");
    }

    public long weightOfId(int id) {
        throw new M3NotYetReceived("M3Weights#weightOfId/1");
    }

    public int select(long weightOffset) {
        throw new M3NotYetReceived("M3Weights#select/1");
    }

    public int selectId(int id, long weightOffset) {
        throw new M3NotYetReceived("M3Weights#selectId/2");
    }

    public long primitiveBytes() {
        throw new M3NotYetReceived("M3Weights#primitiveBytes/0");
    }
}
