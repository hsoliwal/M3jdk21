// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexMatrixQuery}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexMatrixQuery.java}
 * blob {@code 8c35e3b1f9dad8e3945afbb7ad7a55350795d183} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code e992cd3b9298cd5c09fdd695784af5426274b42c7a906ed5b5a26be3fb24c907}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3MatrixQuery}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S08}.
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
 *   <li>{@code member builder(int) (return type)}</li>
 *   <li>{@code member predicateKind(int) (return type)}</li>
 *   <li>{@code member executeInto(MIndexValueTape, MIndexValueMatrix, Workspace) (parameter tape)}</li>
 *   <li>{@code member count(MIndexValueTape, MIndexValueMatrix) (parameter tape)}</li>
 *   <li>{@code member projectCoordinates(MIndexValueMatrix, Workspace, int[], int) (parameter matrix)}</li>
 *   <li>{@code member sumLong(MIndexValueTape, MIndexValueMatrix, int, Workspace) (parameter tape)}</li>
 *   <li>{@code member minLong(MIndexValueTape, MIndexValueMatrix, int, Workspace) (parameter tape)}</li>
 *   <li>{@code member maxLong(MIndexValueTape, MIndexValueMatrix, int, Workspace) (parameter tape)}</li>
 * </ul>
 */
public final class M3MatrixQuery {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code expectedColumns} of type {@code int}. */
    private int expectedColumns;

    /** primitive lane for source field {@code opcodes} of type {@code byte[]}. */
    private byte[] opcodes;

    /** primitive lane for source field {@code columns} of type {@code int[]}. */
    private int[] columns;

    /** primitive lane for source field {@code operands} of type {@code long[]}. */
    private long[] operands;

    /** primitive lane for source field {@code projections} of type {@code int[]}. */
    private int[] projections;

    private M3MatrixQuery() {
        throw new M3NotYetReceived("M3MatrixQuery#<init>/0");
    }

    public int predicateCount() {
        throw new M3NotYetReceived("M3MatrixQuery#predicateCount/0");
    }

    public int projectionCount() {
        throw new M3NotYetReceived("M3MatrixQuery#projectionCount/0");
    }

    public int expectedColumns() {
        throw new M3NotYetReceived("M3MatrixQuery#expectedColumns/0");
    }

    public int predicateColumn(int predicate) {
        throw new M3NotYetReceived("M3MatrixQuery#predicateColumn/1");
    }

    public long predicateOperand(int predicate) {
        throw new M3NotYetReceived("M3MatrixQuery#predicateOperand/1");
    }

    public int projectionColumn(int projection) {
        throw new M3NotYetReceived("M3MatrixQuery#projectionColumn/1");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3MatrixQuery#retainedPayloadBytes/0");
    }
}
