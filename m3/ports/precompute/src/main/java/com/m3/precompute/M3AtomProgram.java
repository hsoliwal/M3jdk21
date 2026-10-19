// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.util.function.IntConsumer;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexAtomProgram}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexAtomProgram.java}
 * blob {@code 9347970f6310731fcccd53ea8143ce39c0660bd7} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 9a546e35ed434e474f0183fe109b2ced86b83413a1048d3d5077ca934cfa9a10}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3AtomProgram}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S20}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3AtomProgram {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code apiId} of type {@code long}. */
    private long apiId;

    /** primitive lane for source field {@code classes} of type {@code byte[]}. */
    private byte[] classes;

    /** primitive lane for source field {@code classCount} of type {@code int}. */
    private int classCount;

    /** primitive lane for source field {@code states} of type {@code int}. */
    private int states;

    /** primitive lane for source field {@code next} of type {@code int[]}. */
    private int[] next;

    /** primitive lane for source field {@code counts} of type {@code long[]}. */
    private long[] counts;

    /** primitive lane for source field {@code masks} of type {@code long[]}. */
    private long[] masks;

    /** primitive lane for source field {@code maskIds} of type {@code long[]}. */
    private long[] maskIds;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sha256} of type {@code String}. */
    private int[] sha256Lane;

    public M3AtomProgram(long apiId, String profileSha256, byte[] utf16Classes, int classCount, int states, int[] next, long[] counts, long[] masks, long[] namedMaskIds) {
        throw new M3NotYetReceived("M3AtomProgram#<init>/9");
    }

    public static M3AtomProgram literal(long apiId, String profileSha256, CharSequence literal, long matchMaskId, IntConsumer progress) {
        throw new M3NotYetReceived("M3AtomProgram#literal/5");
    }

    public long apiId() {
        throw new M3NotYetReceived("M3AtomProgram#apiId/0");
    }

    public int stateCount() {
        throw new M3NotYetReceived("M3AtomProgram#stateCount/0");
    }

    public int maskCount() {
        throw new M3NotYetReceived("M3AtomProgram#maskCount/0");
    }

    public long maskIdAt(int bit) {
        throw new M3NotYetReceived("M3AtomProgram#maskIdAt/1");
    }

    public String contentSha256() {
        throw new M3NotYetReceived("M3AtomProgram#contentSha256/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3AtomProgram#retainedPrimitiveBytes/0");
    }
}
