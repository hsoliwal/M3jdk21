// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.foundation.MIndexIntPrefixIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation/MIndexIntPrefixIndex.java}
 * blob {@code bde000b2701f7a0352f5cbcfd5084a7792c6d52a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f993e5b9cc9512c9e91fdd92033ec38c32663eddb679cbe144af2bb34971942d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.foundation.M3IntPrefixIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S12}.
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
 *   <li>{@code member compile(MIndexDataStructure.IntSequence) (parameter sequence)}</li>
 * </ul>
 */
public final class M3IntPrefixIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code prefix} of type {@code long[]}. */
    private long[] prefix;

    /** primitive lane for source field {@code nonnegative} of type {@code boolean}. */
    private boolean nonnegative;

    private M3IntPrefixIndex() {
        throw new M3NotYetReceived("M3IntPrefixIndex#<init>/0");
    }

    public static M3IntPrefixIndex compile(int... values) {
        throw new M3NotYetReceived("M3IntPrefixIndex#compile/1");
    }

    public int size() {
        throw new M3NotYetReceived("M3IntPrefixIndex#size/0");
    }

    public long prefixAt(int exclusive) {
        throw new M3NotYetReceived("M3IntPrefixIndex#prefixAt/1");
    }

    public long sum(int fromInclusive, int toExclusive) {
        throw new M3NotYetReceived("M3IntPrefixIndex#sum/2");
    }

    public int firstPrefixAtLeast(long target) {
        throw new M3NotYetReceived("M3IntPrefixIndex#firstPrefixAtLeast/1");
    }
}
