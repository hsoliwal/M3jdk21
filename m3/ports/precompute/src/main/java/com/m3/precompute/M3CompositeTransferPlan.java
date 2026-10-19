// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexCompositeTransferPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexCompositeTransferPlan.java}
 * blob {@code 614c7af4d6311c3db20cce91fba77a390e98670b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code ae27eff6d9174590f446cd001f8150e52c3fafe3313cf60e3348e15956d0382c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3CompositeTransferPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S16}.
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
 *   <li>{@code nested types not received: Binding}</li>
 *   <li>{@code member between(MIndexCompositeIndex, MIndexCompositeIndex) (parameter sourceIndex)}</li>
 *   <li>{@code member sourceIndex() (return type)}</li>
 *   <li>{@code member targetIndex() (return type)}</li>
 *   <li>{@code member bind(MIndexSpace<E>, MIndexSpace<E>, MIndexDomainTransfer<E>) (parameter sourceSpace)}</li>
 *   <li>{@code member bindObjects(MIndexObjectSpace<T>, MIndexObjectSpace<T>) (parameter sourceSpace)}</li>
 *   <li>{@code member transfer(MIndexCompositeRef) (return type)}</li>
 * </ul>
 */
public final class M3CompositeTransferPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sourceIndex} of type {@code MIndexCompositeIndex}. */
    private int[] sourceIndexLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code targetIndex} of type {@code MIndexCompositeIndex}. */
    private int[] targetIndexLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code bindings} of type {@code Map<Object, Binding>}. */
    private int[] bindingsLane;

    /** primitive lane for source field {@code targetCompositeIds} of type {@code int[]}. */
    private int[] targetCompositeIds;

    /** primitive lane for source field {@code states} of type {@code byte[]}. */
    private byte[] states;

    /** primitive lane for source field {@code mappedCompositeCount} of type {@code int}. */
    private int mappedCompositeCount;

    private M3CompositeTransferPlan() {
        throw new M3NotYetReceived("M3CompositeTransferPlan#<init>/0");
    }

    public int mappedCompositeCount() {
        throw new M3NotYetReceived("M3CompositeTransferPlan#mappedCompositeCount/0");
    }

    public int transferId(int sourceId) {
        throw new M3NotYetReceived("M3CompositeTransferPlan#transferId/1");
    }
}
