// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexTransferPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexTransferPlan.java}
 * blob {@code 6615b01159099da3b2baa4128c06bde91af4781e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 7b1433899269b0b6e605f03d13b615d284b9bc40d0cb97e7c39a39cdadaf0a0c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3TransferPlan}); projection
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
 *   <li>{@code source supertypes not received: implements MIndexDomainTransfer<E>}</li>
 *   <li>{@code member bind(MIndexSpace<E>, MIndexSpace<E>, MIndexDomainTransfer<E>) (parameter source)}</li>
 *   <li>{@code member source() (return type)}</li>
 *   <li>{@code member target() (return type)}</li>
 *   <li>{@code member transferId(MIndexSpace<E>, int, MIndexSpace<E>) (parameter actualSource)}</li>
 *   <li>{@code member transferIds(MIndexSpace<E>, int[], MIndexSpace<E>) (parameter actualSource)}</li>
 * </ul>
 */
public final class M3TransferPlan<E> {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexSpace<E>}. */
    private int[] sourceLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code target} of type {@code MIndexSpace<E>}. */
    private int[] targetLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code delegate} of type {@code MIndexDomainTransfer<E>}. */
    private int[] delegateLane;

    /** primitive lane for source field {@code identity} of type {@code boolean}. */
    private boolean identity;

    /** primitive lane for source field {@code targetIds} of type {@code int[]}. */
    private int[] targetIds;

    /** primitive lane for source field {@code mappedCount} of type {@code int}. */
    private int mappedCount;

    private M3TransferPlan() {
        throw new M3NotYetReceived("M3TransferPlan#<init>/0");
    }

    public int mappedCount() {
        throw new M3NotYetReceived("M3TransferPlan#mappedCount/0");
    }

    public boolean identity() {
        throw new M3NotYetReceived("M3TransferPlan#identity/0");
    }

    public int transferId(int sourceId) {
        throw new M3NotYetReceived("M3TransferPlan#transferId/1");
    }

    public int[] transferIds(int[] sourceIds) {
        throw new M3NotYetReceived("M3TransferPlan#transferIds/1");
    }
}
