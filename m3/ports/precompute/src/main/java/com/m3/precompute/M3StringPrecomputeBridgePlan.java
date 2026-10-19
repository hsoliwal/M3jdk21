// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexStringPrecomputeBridgePlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexStringPrecomputeBridgePlan.java}
 * blob {@code a68a137ba8e3d1c2bf6a07dd12b0bc46ac220f20} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code a200e01f53bd794b9e610eb5cc71fa92563dcb7189694f1b83a1b65a6bfad364}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3StringPrecomputeBridgePlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
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
 *   <li>{@code nested types not received: Receipt, Counters}</li>
 *   <li>{@code member forValue(CharSequence, MIndexStringPrecomputeBridge.Policy) (parameter policy)}</li>
 *   <li>{@code member forBinding(MIndexStringPrecomputeBridge.Binding, MIndexStringPrecomputeBridge.Policy) (parameter binding)}</li>
 *   <li>{@code member sourceBinding() (return type)}</li>
 *   <li>{@code member preparationPolicy() (return type)}</li>
 *   <li>{@code member relation(MIndexStringPrecomputeBridge.Binding) (parameter peer)}</li>
 *   <li>{@code member projection(MIndexStringUnaryProjectionPrecompute.Projection) (parameter projection)}</li>
 *   <li>{@code member literalTransition(MIndexStringPrecomputeBridge.Binding) (parameter pattern)}</li>
 *   <li>{@code member prepare() (return type)}</li>
 * </ul>
 */
public final class M3StringPrecomputeBridgePlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexStringPrecomputeBridge.Binding}. */
    private int[] sourceLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code policy} of type {@code MIndexStringPrecomputeBridge.Policy}. */
    private int[] policyLane;

    /** primitive lane for source field {@code singleRequested} of type {@code boolean}. */
    private boolean singleRequested;

    /** primitive lane for source field {@code singleMask} of type {@code int}. */
    private int singleMask;

    /** primitive lane for source field {@code rangeFingerprintRequested} of type {@code boolean}. */
    private boolean rangeFingerprintRequested;

    /** primitive lane for source field {@code projectionMask} of type {@code int}. */
    private int projectionMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code relationPeers} of type {@code MIndexStringPrecomputeBridge.Binding[]}. */
    private int[] relationPeersLane;

    /** primitive lane for source field {@code relationPeerCount} of type {@code int}. */
    private int relationPeerCount;

    /** int handle lane (offsets into parent-owned storage) for source field {@code transitionPatterns} of type {@code MIndexStringPrecomputeBridge.Binding[]}. */
    private int[] transitionPatternsLane;

    /** primitive lane for source field {@code transitionPatternCount} of type {@code int}. */
    private int transitionPatternCount;

    /** primitive lane for source field {@code declaredRequests} of type {@code int}. */
    private int declaredRequests;

    /** primitive lane for source field {@code deduplicatedDeclarations} of type {@code int}. */
    private int deduplicatedDeclarations;

    private M3StringPrecomputeBridgePlan() {
        throw new M3NotYetReceived("M3StringPrecomputeBridgePlan#<init>/0");
    }

    public M3StringPrecomputeBridgePlan single(M3StringPrecomputeProfile profile) {
        throw new M3NotYetReceived("M3StringPrecomputeBridgePlan#single/1");
    }

    public M3StringPrecomputeBridgePlan rangeFingerprint() {
        throw new M3NotYetReceived("M3StringPrecomputeBridgePlan#rangeFingerprint/0");
    }

    public M3StringPrecomputeBridgePlan relation(CharSequence peer) {
        throw new M3NotYetReceived("M3StringPrecomputeBridgePlan#relation/1");
    }

    public M3StringPrecomputeBridgePlan literalTransition(CharSequence pattern) {
        throw new M3NotYetReceived("M3StringPrecomputeBridgePlan#literalTransition/1");
    }
}
