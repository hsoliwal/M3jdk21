// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexInt8MatmulPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexInt8MatmulPlan.java}
 * blob {@code f4a049c2bd822d60715556ca0c713b2ca58c522f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 469e247f69bdc4c304095bbe7ce9909283ea70c5879d1a589d5621e5430669a6}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3Int8MatmulPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S16}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3Int8MatmulPlan {
    public static final int TILE = 8;

    public static final int MAX_PACKED_BYTES = 256 * 1024 * 1024;

    public static final int MAX_OUTPUT_CELLS = 16 * 1024 * 1024;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code inner} of type {@code int}. */
    private int inner;

    /** primitive lane for source field {@code columns} of type {@code int}. */
    private int columns;

    /** primitive lane for source field {@code paddedInner} of type {@code int}. */
    private int paddedInner;

    /** primitive lane for source field {@code paddedColumns} of type {@code int}. */
    private int paddedColumns;

    /** primitive lane for source field {@code packedRight} of type {@code byte[]}. */
    private byte[] packedRight;

    /** int handle lane (offsets into parent-owned storage) for source field {@code contentSha256} of type {@code String}. */
    private int[] contentSha256Lane;

    private M3Int8MatmulPlan() {
        throw new M3NotYetReceived("M3Int8MatmulPlan#<init>/0");
    }

    public static M3Int8MatmulPlan compile(byte[] rightRowMajor, int inner, int columns) {
        throw new M3NotYetReceived("M3Int8MatmulPlan#compile/3");
    }

    public int inner() {
        throw new M3NotYetReceived("M3Int8MatmulPlan#inner/0");
    }

    public int columns() {
        throw new M3NotYetReceived("M3Int8MatmulPlan#columns/0");
    }

    public int paddedInner() {
        throw new M3NotYetReceived("M3Int8MatmulPlan#paddedInner/0");
    }

    public int paddedColumns() {
        throw new M3NotYetReceived("M3Int8MatmulPlan#paddedColumns/0");
    }

    public int packedBytes() {
        throw new M3NotYetReceived("M3Int8MatmulPlan#packedBytes/0");
    }

    public String contentSha256() {
        throw new M3NotYetReceived("M3Int8MatmulPlan#contentSha256/0");
    }

    public byte[] packedRightCopy() {
        throw new M3NotYetReceived("M3Int8MatmulPlan#packedRightCopy/0");
    }

    public int[] multiply(byte[] leftRowMajor, int rows, int[] bias) {
        throw new M3NotYetReceived("M3Int8MatmulPlan#multiply/3");
    }

    public int[] multiply(String backendId, byte[] leftRowMajor, int rows, int[] bias) {
        throw new M3NotYetReceived("M3Int8MatmulPlan#multiply/4");
    }
}
