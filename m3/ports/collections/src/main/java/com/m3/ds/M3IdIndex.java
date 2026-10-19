// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexIdIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexIdIndex.java}
 * blob {@code ebb3d0abadf1690b56d304a4cc3ff4b2f53f76e5} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 6ddf531439e2528cebaf58d9b3083c4a655ad78f73db2d596b06c26eb3557eba}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3IdIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S05}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member weights(long[]) (return type)}</li>
 *   <li>{@code member weightsById(IntToLongFunction) (return type)}</li>
 * </ul>
 */
public final class M3IdIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code owner} of type {@code MIndexCompositeIndex}. */
    private int[] ownerLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code space} of type {@code MIndexSpace<?>}. */
    private int[] spaceLane;

    /** primitive lane for source field {@code canonicalId} of type {@code int}. */
    private int canonicalId;

    /** primitive lane for source field {@code offset} of type {@code int}. */
    private int offset;

    /** primitive lane for source field {@code stride} of type {@code int}. */
    private int stride;

    /** primitive lane for source field {@code size} of type {@code int}. */
    private int size;

    /** primitive lane for source field {@code ids} of type {@code int[]}. */
    private int[] ids;

    /** primitive lane for source field {@code offsets} of type {@code int[]}. */
    private int[] offsets;

    /** primitive lane for source field {@code positions} of type {@code int[]}. */
    private int[] positions;

    private M3IdIndex() {
        throw new M3NotYetReceived("M3IdIndex#<init>/0");
    }

    public M3Space<?> space() {
        throw new M3NotYetReceived("M3IdIndex#space/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3IdIndex#size/0");
    }

    public int distinctSize() {
        throw new M3NotYetReceived("M3IdIndex#distinctSize/0");
    }

    public int idAt(int position) {
        throw new M3NotYetReceived("M3IdIndex#idAt/1");
    }

    public int distinctIdAt(int rank) {
        throw new M3NotYetReceived("M3IdIndex#distinctIdAt/1");
    }

    public int rank(int id) {
        throw new M3NotYetReceived("M3IdIndex#rank/1");
    }

    public boolean containsId(int id) {
        throw new M3NotYetReceived("M3IdIndex#containsId/1");
    }

    public int count(int id) {
        throw new M3NotYetReceived("M3IdIndex#count/1");
    }

    public int positionOf(int id, int occurrence) {
        throw new M3NotYetReceived("M3IdIndex#positionOf/2");
    }

    public int sortedPositionAt(int ordinal) {
        throw new M3NotYetReceived("M3IdIndex#sortedPositionAt/1");
    }

    public int sortedIdAt(int ordinal) {
        throw new M3NotYetReceived("M3IdIndex#sortedIdAt/1");
    }

    public int countInRange(int id, int fromInclusive, int toExclusive) {
        throw new M3NotYetReceived("M3IdIndex#countInRange/3");
    }

    public int intersectionSize(M3IdIndex other) {
        throw new M3NotYetReceived("M3IdIndex#intersectionSize/1");
    }

    public boolean isSubsetOf(M3IdIndex other) {
        throw new M3NotYetReceived("M3IdIndex#isSubsetOf/1");
    }

    public double jaccard(M3IdIndex other) {
        throw new M3NotYetReceived("M3IdIndex#jaccard/1");
    }

    public M3IdIndex slice(int fromInclusive, int toExclusive) {
        throw new M3NotYetReceived("M3IdIndex#slice/2");
    }

    public long primitiveBytes() {
        throw new M3NotYetReceived("M3IdIndex#primitiveBytes/0");
    }
}
