// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexStructureQuery}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexStructureQuery.java}
 * blob {@code 7f5c913f118a0c4449e368fb350f3296babb4a99} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code a8fcce73176286aca70dc2f07cafc1f9e4cfb475a896a83493c4d999119938fa}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3StructureQuery}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S10}.
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
 *   <li>{@code nested types not received: PredicateKind, Workspace, Builder}</li>
 *   <li>{@code member builder() (return type)}</li>
 *   <li>{@code member predicateKind(int) (return type)}</li>
 *   <li>{@code member executeSubtreeInto(MIndexStructureTape, int, Workspace) (parameter workspace)}</li>
 *   <li>{@code member executeDirectChildrenInto(MIndexStructureTape, int, Workspace) (parameter workspace)}</li>
 * </ul>
 */
public final class M3StructureQuery {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code opcodes} of type {@code byte[]}. */
    private byte[] opcodes;

    /** primitive lane for source field {@code operands} of type {@code long[]}. */
    private long[] operands;

    private M3StructureQuery() {
        throw new M3NotYetReceived("M3StructureQuery#<init>/0");
    }

    public int predicateCount() {
        throw new M3NotYetReceived("M3StructureQuery#predicateCount/0");
    }

    public long predicateOperand(int predicate) {
        throw new M3NotYetReceived("M3StructureQuery#predicateOperand/1");
    }

    public int countSubtree(M3StructureTape tape, int root) {
        throw new M3NotYetReceived("M3StructureQuery#countSubtree/2");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3StructureQuery#retainedPayloadBytes/0");
    }
}
