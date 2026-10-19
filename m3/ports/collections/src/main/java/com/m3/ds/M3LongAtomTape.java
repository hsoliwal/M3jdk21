// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexLongAtomTape}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexLongAtomTape.java}
 * blob {@code b9db3e5769b774c864bf040990baed5bc34c8c1b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b5cb09637baa06532d94e8965e1fafca0b97f8e1335b5c8b18e38078a6d48930}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3LongAtomTape}); projection
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
 *   <li>{@code nested types not received: Chunk, Builder}</li>
 *   <li>{@code member source() (return type)}</li>
 *   <li>{@code member grammar() (return type)}</li>
 *   <li>{@code member kind(long) (return type)}</li>
 *   <li>{@code member rawText(long) (return type)}</li>
 *   <li>{@code member text(long) (return type)}</li>
 *   <li>{@code member knownAtom(long) (return type)}</li>
 * </ul>
 */
public final class M3LongAtomTape {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexSource}. */
    private int[] sourceLane;

    /** primitive lane for source field {@code sourceStart} of type {@code long}. */
    private long sourceStart;

    /** primitive lane for source field {@code sourceEnd} of type {@code long}. */
    private long sourceEnd;

    /** int handle lane (offsets into parent-owned storage) for source field {@code grammar} of type {@code MIndexAtomGrammar}. */
    private int[] grammarLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code chunks} of type {@code List<Chunk>}. */
    private int[] chunksLane;

    /** primitive lane for source field {@code size} of type {@code long}. */
    private long size;

    private M3LongAtomTape() {
        throw new M3NotYetReceived("M3LongAtomTape#<init>/0");
    }

    public long sourceStart() {
        throw new M3NotYetReceived("M3LongAtomTape#sourceStart/0");
    }

    public long sourceEnd() {
        throw new M3NotYetReceived("M3LongAtomTape#sourceEnd/0");
    }

    public long sourceLength() {
        throw new M3NotYetReceived("M3LongAtomTape#sourceLength/0");
    }

    public long size() {
        throw new M3NotYetReceived("M3LongAtomTape#size/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3LongAtomTape#retainedPrimitiveBytes/0");
    }

    public int kindCode(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#kindCode/1");
    }

    public int flags(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#flags/1");
    }

    public boolean hasFlag(long atom, int mask) {
        throw new M3NotYetReceived("M3LongAtomTape#hasFlag/2");
    }

    public int atomId(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#atomId/1");
    }

    public long startByte(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#startByte/1");
    }

    public long endByte(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#endByte/1");
    }

    public long contentStartByte(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#contentStartByte/1");
    }

    public long contentEndByte(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#contentEndByte/1");
    }

    public long hash64(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#hash64/1");
    }

    public long scalar(long atom) {
        throw new M3NotYetReceived("M3LongAtomTape#scalar/1");
    }

    public M3LongAtomCursor cursor() {
        throw new M3NotYetReceived("M3LongAtomTape#cursor/0");
    }

    public long nextSignificant(long fromAtom) {
        throw new M3NotYetReceived("M3LongAtomTape#nextSignificant/1");
    }

    public long previousSignificant(long fromAtom) {
        throw new M3NotYetReceived("M3LongAtomTape#previousSignificant/1");
    }

    public long atomContainingByte(long byteOffset) {
        throw new M3NotYetReceived("M3LongAtomTape#atomContainingByte/1");
    }

    public ByteBuffer rawSlice(long atom) throws IOException {
        throw new M3NotYetReceived("M3LongAtomTape#rawSlice/1");
    }

    public ByteBuffer contentSlice(long atom) throws IOException {
        throw new M3NotYetReceived("M3LongAtomTape#contentSlice/1");
    }

    public void verifyUnchanged() throws IOException {
        throw new M3NotYetReceived("M3LongAtomTape#verifyUnchanged/0");
    }

    public String sourceSha256() throws IOException {
        throw new M3NotYetReceived("M3LongAtomTape#sourceSha256/0");
    }
}
