// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexCharArrayRangeJoin}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexCharArrayRangeJoin.java}
 * blob {@code 70db4fbd97b9c70652cccfb30bae5004dd327d59} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 94e2fbc10bef47ae4b1c840dc8fd539b090f0c15279c4b4a64b9d2bf63b43d9b}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3CharArrayRangeJoin}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
 * Map family {@code ARRAYS}; naming-law family {@code ARRAYS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Range}</li>
 *   <li>{@code member javaJoin(Range...) (return type)}</li>
 *   <li>{@code member nativeJoin(int, Range...) (return type)}</li>
 * </ul>
 */
public final class M3CharArrayRangeJoin {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3CharArrayRangeJoin() {
        throw new M3NotYetReceived("M3CharArrayRangeJoin#<init>/0");
    }
}
