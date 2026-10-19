// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.precompute.MIndexApiPrecomputeTable}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/precompute-api/src/main/java/com/synexia/mindex/precompute/MIndexApiPrecomputeTable.java}
 * blob {@code 91bbf8db7219388cd10b748546fc1895f87280fa} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b3b1a4c56c23ca03b297441e1bbc65a130aa3b872a0fd9f9bc24f9002657dcc8}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.precompute.M3ApiPrecomputeTable}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
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
 *   <li>{@code nested types not received: Builder, Row}</li>
 *   <li>{@code source supertypes not received: implements MIndexApiPrecomputeView}</li>
 *   <li>{@code member builder(MIndexUniversalLimits) (return type)}</li>
 *   <li>{@code member kind(int) (return type)}</li>
 * </ul>
 */
public final class M3ApiPrecomputeTable {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code apiIds} of type {@code long[]}. */
    private long[] apiIds;

    /** primitive lane for source field {@code argumentOffsets} of type {@code int[]}. */
    private int[] argumentOffsets;

    /** primitive lane for source field {@code argumentLengths} of type {@code int[]}. */
    private int[] argumentLengths;

    /** primitive lane for source field {@code arguments} of type {@code long[]}. */
    private long[] arguments;

    /** primitive lane for source field {@code kinds} of type {@code byte[]}. */
    private byte[] kinds;

    /** primitive lane for source field {@code flags} of type {@code byte[]}. */
    private byte[] flags;

    /** primitive lane for source field {@code scalars} of type {@code long[]}. */
    private long[] scalars;

    /** primitive lane for source field {@code auxiliaries} of type {@code long[]}. */
    private long[] auxiliaries;

    /** primitive lane for source field {@code payloadOffsets} of type {@code int[]}. */
    private int[] payloadOffsets;

    /** primitive lane for source field {@code payloadLengths} of type {@code int[]}. */
    private int[] payloadLengths;

    /** primitive lane for source field {@code payload} of type {@code long[]}. */
    private long[] payload;

    /** primitive lane for source field {@code maxArgumentCount} of type {@code int}. */
    private int maxArgumentCount;

    /** primitive lane for source field {@code contentFingerprint} of type {@code long}. */
    private long contentFingerprint;

    /** int handle lane (offsets into parent-owned storage) for source field {@code contentSha256} of type {@code String}. */
    private int[] contentSha256Lane;

    private M3ApiPrecomputeTable() {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#<init>/0");
    }

    public static M3ApiPrecomputeTable empty() {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#empty/0");
    }

    public int rowCount() {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#rowCount/0");
    }

    public int maxArgumentCount() {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#maxArgumentCount/0");
    }

    public long contentFingerprint() {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#contentFingerprint/0");
    }

    public String contentSha256() {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#contentSha256/0");
    }

    public long apiId(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#apiId/1");
    }

    public int argumentCount(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#argumentCount/1");
    }

    public long argumentAt(int row, int index) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#argumentAt/2");
    }

    public int find(long apiId, long[] candidateArguments, int offset, int length) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#find/4");
    }

    public byte flags(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#flags/1");
    }

    public long scalar(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#scalar/1");
    }

    public long auxiliary(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#auxiliary/1");
    }

    public int payloadLength(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#payloadLength/1");
    }

    public long payloadAt(int row, int index) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#payloadAt/2");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#retainedPrimitiveBytes/0");
    }

    public long[] argumentsCopy(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#argumentsCopy/1");
    }

    public long[] payloadCopy(int row) {
        throw new M3NotYetReceived("M3ApiPrecomputeTable#payloadCopy/1");
    }
}
