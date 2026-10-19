// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexFixedRecordPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexFixedRecordPlan.java}
 * blob {@code 8e55596e63124c264718b7fa13041321dab4ac27} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code d4cbecd979978e5c2b21e2443593a2e55ebb0f75909206b0befcadb31c98a64b}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3FixedRecordPlan}); projection
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
 *   <li>{@code nested types not received: Trim, Codec, Field, Builder}</li>
 *   <li>{@code member builder(MIndexSchemaPlan, int) (return type)}</li>
 *   <li>{@code member schema() (return type)}</li>
 *   <li>{@code member field(int) (return type)}</li>
 * </ul>
 */
public final class M3FixedRecordPlan {
    public static final int MAX_FIELDS = 4_096;

    public static final int MAX_RECORD_WIDTH = 16 * 1024 * 1024;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code schema} of type {@code MIndexSchemaPlan}. */
    private int[] schemaLane;

    /** primitive lane for source field {@code recordWidth} of type {@code int}. */
    private int recordWidth;

    /** int handle lane (offsets into parent-owned storage) for source field {@code fields} of type {@code Field[]}. */
    private int[] fieldsLane;

    /** primitive lane for source field {@code offsets} of type {@code int[]}. */
    private int[] offsets;

    /** primitive lane for source field {@code widths} of type {@code int[]}. */
    private int[] widths;

    /** primitive lane for source field {@code trimCodes} of type {@code byte[]}. */
    private byte[] trimCodes;

    /** primitive lane for source field {@code trimBytes} of type {@code byte[]}. */
    private byte[] trimBytes;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sha256} of type {@code String}. */
    private int[] sha256Lane;

    private M3FixedRecordPlan() {
        throw new M3NotYetReceived("M3FixedRecordPlan#<init>/0");
    }

    public int fieldCount() {
        throw new M3NotYetReceived("M3FixedRecordPlan#fieldCount/0");
    }

    public int recordWidth() {
        throw new M3NotYetReceived("M3FixedRecordPlan#recordWidth/0");
    }

    public String sha256() {
        throw new M3NotYetReceived("M3FixedRecordPlan#sha256/0");
    }
}
