// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.nio.ByteBuffer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexAtomTape}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexAtomTape.java}
 * blob {@code 0b35f77d60f41cb1bc478cd5ac79d245293f7834} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 79be23beb7fad8766ded35123619048de3c829a13e6377bf592a2895ba82adfc}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3AtomTape}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S06}.
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
 *   <li>{@code nested types not received: Kind, Builder}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member grammar() (return type)}</li>
 *   <li>{@code member kind(int) (return type)}</li>
 *   <li>{@code member rawText(int) (return type)}</li>
 *   <li>{@code member text(int) (return type)}</li>
 *   <li>{@code member knownAtom(int) (return type)}</li>
 *   <li>{@code member contentMatches(int, MIndexPattern) (parameter pattern)}</li>
 *   <li>{@code member contentFinds(int, MIndexPattern) (parameter pattern)}</li>
 *   <li>{@code member firstContentMatch(int, MIndexPattern) (return type)}</li>
 *   <li>{@code member scanContent(int, MIndexMultiPattern, int, MIndexProgressMonitor, MIndexMultiPattern.MatchConsumer) (parameter patterns)}</li>
 *   <li>{@code member forEachContentMatch(int, MIndexPatternSet, MIndexPatternSet.Utf8MatchConsumer) (parameter patterns)}</li>
 *   <li>{@code member reconstruct() (return type)}</li>
 * </ul>
 */
public final class M3AtomTape {
    public static final int FLAG_KNOWN = 1;

    public static final int FLAG_QUOTED = 1 << 1;

    public static final int FLAG_ESCAPED = 1 << 2;

    public static final int FLAG_INTEGER = 1 << 3;

    public static final int FLAG_NEGATIVE = 1 << 4;

    public static final int FLAG_DECIMAL = 1 << 5;

    public static final int FLAG_EXPONENT = 1 << 6;

    public static final int FLAG_OVERFLOW = 1 << 7;

    public static final int FLAG_NON_ASCII = 1 << 8;

    public static final int FLAG_BLOCK_COMMENT = 1 << 9;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code source} of type {@code byte[]}. */
    private byte[] source;

    /** primitive lane for source field {@code kinds} of type {@code byte[]}. */
    private byte[] kinds;

    /** primitive lane for source field {@code flags} of type {@code int[]}. */
    private int[] flags;

    /** primitive lane for source field {@code starts} of type {@code int[]}. */
    private int[] starts;

    /** primitive lane for source field {@code ends} of type {@code int[]}. */
    private int[] ends;

    /** primitive lane for source field {@code contentStarts} of type {@code int[]}. */
    private int[] contentStarts;

    /** primitive lane for source field {@code contentEnds} of type {@code int[]}. */
    private int[] contentEnds;

    /** primitive lane for source field {@code atomIds} of type {@code int[]}. */
    private int[] atomIds;

    /** primitive lane for source field {@code hashes} of type {@code long[]}. */
    private long[] hashes;

    /** primitive lane for source field {@code scalars} of type {@code long[]}. */
    private long[] scalars;

    /** int handle lane (offsets into parent-owned storage) for source field {@code grammar} of type {@code MIndexAtomGrammar}. */
    private int[] grammarLane;

    private M3AtomTape() {
        throw new M3NotYetReceived("M3AtomTape#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3AtomTape#size/0");
    }

    public int sourceLength() {
        throw new M3NotYetReceived("M3AtomTape#sourceLength/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3AtomTape#retainedPrimitiveBytes/0");
    }

    public int kindCode(int atom) {
        throw new M3NotYetReceived("M3AtomTape#kindCode/1");
    }

    public int flags(int atom) {
        throw new M3NotYetReceived("M3AtomTape#flags/1");
    }

    public boolean hasFlag(int atom, int mask) {
        throw new M3NotYetReceived("M3AtomTape#hasFlag/2");
    }

    public int startByte(int atom) {
        throw new M3NotYetReceived("M3AtomTape#startByte/1");
    }

    public int endByte(int atom) {
        throw new M3NotYetReceived("M3AtomTape#endByte/1");
    }

    public int contentStartByte(int atom) {
        throw new M3NotYetReceived("M3AtomTape#contentStartByte/1");
    }

    public int contentEndByte(int atom) {
        throw new M3NotYetReceived("M3AtomTape#contentEndByte/1");
    }

    public int atomId(int atom) {
        throw new M3NotYetReceived("M3AtomTape#atomId/1");
    }

    public long hash64(int atom) {
        throw new M3NotYetReceived("M3AtomTape#hash64/1");
    }

    public long scalar(int atom) {
        throw new M3NotYetReceived("M3AtomTape#scalar/1");
    }

    public M3AtomCursor cursor() {
        throw new M3NotYetReceived("M3AtomTape#cursor/0");
    }

    public int atomContainingByte(int byteOffset) {
        throw new M3NotYetReceived("M3AtomTape#atomContainingByte/1");
    }

    public int nextSignificant(int fromAtom) {
        throw new M3NotYetReceived("M3AtomTape#nextSignificant/1");
    }

    public int previousSignificant(int fromAtom) {
        throw new M3NotYetReceived("M3AtomTape#previousSignificant/1");
    }

    public ByteBuffer rawSlice(int atom) {
        throw new M3NotYetReceived("M3AtomTape#rawSlice/1");
    }

    public ByteBuffer contentSlice(int atom) {
        throw new M3NotYetReceived("M3AtomTape#contentSlice/1");
    }

    public byte[] sourceCopy() {
        throw new M3NotYetReceived("M3AtomTape#sourceCopy/0");
    }
}
