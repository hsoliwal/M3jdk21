// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.function.LongUnaryOperator;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.join.MIndexPairPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/join/MIndexPairPlan.java}
 * blob {@code 4a4d104329a5b96b85c330ac3cccef021d94542e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code a3ec1972b2a4e726dcb5eea9f692360d87c6cc3111025e15dbe7abba21ab4b7a}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.join.M3PairPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S24}.
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
 *   <li>{@code member cursor(IProgressMonitor) (return type)}</li>
 * </ul>
 */
public final class M3PairPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code size} of type {@code long}. */
    private long size;

    /** int handle lane (offsets into parent-owned storage) for source field {@code lookup} of type {@code LongUnaryOperator}. */
    private int[] lookupLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code cursorLookups} of type {@code Supplier<LongUnaryOperator>}. */
    private int[] cursorLookupsLane;

    private M3PairPlan() {
        throw new M3NotYetReceived("M3PairPlan#<init>/0");
    }

    public static M3PairPlan fromImmutableIndex(long size, LongUnaryOperator lookup) {
        throw new M3NotYetReceived("M3PairPlan#fromImmutableIndex/2");
    }

    public static M3PairPlan cartesian(int leftRows, int rightRows) {
        throw new M3NotYetReceived("M3PairPlan#cartesian/2");
    }

    public long size() {
        throw new M3NotYetReceived("M3PairPlan#size/0");
    }

    public long pairAt(long ordinal) {
        throw new M3NotYetReceived("M3PairPlan#pairAt/1");
    }

    public static long pack(int left, int right) {
        throw new M3NotYetReceived("M3PairPlan#pack/2");
    }

    public static int left(long pair) {
        throw new M3NotYetReceived("M3PairPlan#left/1");
    }

    public static int right(long pair) {
        throw new M3NotYetReceived("M3PairPlan#right/1");
    }
}
