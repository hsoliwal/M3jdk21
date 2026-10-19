// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.type.MIndexClassSnapshot}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/type/MIndexClassSnapshot.java}
 * blob {@code dc88bc5963b114d78c6637ceb5ff790320cf1e2c} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 0d95622479a636117c71668bd2d8dfc599e66793d8adbd58cffb3a74d882a381}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.type.M3ClassSnapshot}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S21}.
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
 *   <li>{@code member pool() (return type)}</li>
 *   <li>{@code member directSubtypeAt(int, int) (return type)}</li>
 *   <li>{@code member topologicalClassAt(int) (return type)}</li>
 * </ul>
 */
public final class M3ClassSnapshot {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code pool} of type {@code MIndexClassPool}. */
    private int[] poolLane;

    /** primitive lane for source field {@code classCount} of type {@code int}. */
    private int classCount;

    /** primitive lane for source field {@code childStarts} of type {@code int[]}. */
    private int[] childStarts;

    /** primitive lane for source field {@code childHandles} of type {@code int[]}. */
    private int[] childHandles;

    /** primitive lane for source field {@code topologicalOrder} of type {@code int[]}. */
    private int[] topologicalOrder;

    /** primitive lane for source field {@code depths} of type {@code int[]}. */
    private int[] depths;

    /** primitive lane for source field {@code ancestorSignals} of type {@code long[]}. */
    private long[] ancestorSignals;

    /** int handle lane (offsets into parent-owned storage) for source field {@code topologySha256} of type {@code String}. */
    private int[] topologySha256Lane;

    private M3ClassSnapshot() {
        throw new M3NotYetReceived("M3ClassSnapshot#<init>/0");
    }

    public int classCount() {
        throw new M3NotYetReceived("M3ClassSnapshot#classCount/0");
    }

    public int edgeCount() {
        throw new M3NotYetReceived("M3ClassSnapshot#edgeCount/0");
    }

    public String topologySha256() {
        throw new M3NotYetReceived("M3ClassSnapshot#topologySha256/0");
    }

    public int directSubtypeCount(int handle) {
        throw new M3NotYetReceived("M3ClassSnapshot#directSubtypeCount/1");
    }

    public int[] directSubtypeHandles(int handle) {
        throw new M3NotYetReceived("M3ClassSnapshot#directSubtypeHandles/1");
    }

    public int depth(int handle) {
        throw new M3NotYetReceived("M3ClassSnapshot#depth/1");
    }

    public int[] topologicalHandles() {
        throw new M3NotYetReceived("M3ClassSnapshot#topologicalHandles/0");
    }

    public boolean isAssignableFrom(int targetSuperHandle, int candidateSubHandle) {
        throw new M3NotYetReceived("M3ClassSnapshot#isAssignableFrom/2");
    }

    public int inheritanceDistance(int targetSuperHandle, int candidateSubHandle) {
        throw new M3NotYetReceived("M3ClassSnapshot#inheritanceDistance/2");
    }

    public int[] ancestors(int handle) {
        throw new M3NotYetReceived("M3ClassSnapshot#ancestors/1");
    }

    public int[] descendants(int handle) {
        throw new M3NotYetReceived("M3ClassSnapshot#descendants/1");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3ClassSnapshot#primitivePayloadBytes/0");
    }
}
