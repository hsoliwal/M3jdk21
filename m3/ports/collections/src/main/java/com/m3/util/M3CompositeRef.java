// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexCompositeRef}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexCompositeRef.java}
 * blob {@code ed1366f07f15afaf24234c69bb9b6b976bd83a99} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 8128e9d9bf32c8c2051906a63033474d2a024c75a9c34245991c7635bdc018de}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3CompositeRef}); projection
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
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member compositeIndex() (return type)}</li>
 * </ul>
 */
public final class M3CompositeRef {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code index} of type {@code MIndexCompositeIndex}. */
    private int[] indexLane;

    /** primitive lane for source field {@code canonicalId} of type {@code int}. */
    private int canonicalId;

    private M3CompositeRef() {
        throw new M3NotYetReceived("M3CompositeRef#<init>/0");
    }

    public int canonicalId() {
        throw new M3NotYetReceived("M3CompositeRef#canonicalId/0");
    }

    public int kind() {
        throw new M3NotYetReceived("M3CompositeRef#kind/0");
    }

    public String kindName() {
        throw new M3NotYetReceived("M3CompositeRef#kindName/0");
    }

    public int laneLength() {
        throw new M3NotYetReceived("M3CompositeRef#laneLength/0");
    }

    public int valueAt(int position) {
        throw new M3NotYetReceived("M3CompositeRef#valueAt/1");
    }

    public int[] copyLane() {
        throw new M3NotYetReceived("M3CompositeRef#copyLane/0");
    }

    public long structuralHash64() {
        throw new M3NotYetReceived("M3CompositeRef#structuralHash64/0");
    }

    public long signal64() {
        throw new M3NotYetReceived("M3CompositeRef#signal64/0");
    }

    public boolean equals(Object other) {
        throw new M3NotYetReceived("M3CompositeRef#equals/1");
    }

    public int hashCode() {
        throw new M3NotYetReceived("M3CompositeRef#hashCode/0");
    }

    public String toString() {
        throw new M3NotYetReceived("M3CompositeRef#toString/0");
    }
}
