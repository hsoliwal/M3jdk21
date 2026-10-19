// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.query.MIndexQueryPredicateTable}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/query/MIndexQueryPredicateTable.java}
 * blob {@code d70afa6bb4095b9d037d1efdced77f6f81e168fc} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2d1dc116376378051c2626148a5192f2c81a417fe5e98097f70757fa36f85c9a}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.query.M3QueryPredicateTable}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
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
 *   <li>{@code nested types not received: Clause, Builder}</li>
 *   <li>{@code member builder() (return type)}</li>
 *   <li>{@code member matches(MIndexValueTape, int, int) (parameter tape)}</li>
 * </ul>
 */
public final class M3QueryPredicateTable {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code starts} of type {@code int[]}. */
    private int[] starts;

    /** primitive lane for source field {@code opcodes} of type {@code byte[]}. */
    private byte[] opcodes;

    /** primitive lane for source field {@code operands} of type {@code long[]}. */
    private long[] operands;

    private M3QueryPredicateTable() {
        throw new M3NotYetReceived("M3QueryPredicateTable#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3QueryPredicateTable#size/0");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3QueryPredicateTable#retainedPayloadBytes/0");
    }
}
