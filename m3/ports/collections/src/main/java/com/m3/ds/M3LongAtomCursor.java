// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexLongAtomCursor}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexLongAtomCursor.java}
 * blob {@code 7480d61a6dba4b993b321486a6d83f9f7602730e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code d4a0d1541de3734faea94c22a93a282380a96ed9dbba1b075899855e43f62f7f}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3LongAtomCursor}); projection
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
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member kind() (return type)}</li>
 *   <li>{@code member text() (return type)}</li>
 * </ul>
 */
public final class M3LongAtomCursor {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code tape} of type {@code MIndexLongAtomTape}. */
    private int[] tapeLane;

    /** primitive lane for source field {@code atom} of type {@code long}. */
    private long atom;

    private M3LongAtomCursor() {
        throw new M3NotYetReceived("M3LongAtomCursor#<init>/0");
    }

    public long position() {
        throw new M3NotYetReceived("M3LongAtomCursor#position/0");
    }

    public boolean positioned() {
        throw new M3NotYetReceived("M3LongAtomCursor#positioned/0");
    }

    public M3LongAtomCursor reset() {
        throw new M3NotYetReceived("M3LongAtomCursor#reset/0");
    }

    public M3LongAtomCursor seekAtom(long coordinate) {
        throw new M3NotYetReceived("M3LongAtomCursor#seekAtom/1");
    }

    public boolean seekByte(long byteOffset) {
        throw new M3NotYetReceived("M3LongAtomCursor#seekByte/1");
    }

    public boolean next() {
        throw new M3NotYetReceived("M3LongAtomCursor#next/0");
    }

    public boolean previous() {
        throw new M3NotYetReceived("M3LongAtomCursor#previous/0");
    }

    public boolean nextSignificant() {
        throw new M3NotYetReceived("M3LongAtomCursor#nextSignificant/0");
    }

    public boolean previousSignificant() {
        throw new M3NotYetReceived("M3LongAtomCursor#previousSignificant/0");
    }

    public int flags() {
        throw new M3NotYetReceived("M3LongAtomCursor#flags/0");
    }

    public int atomId() {
        throw new M3NotYetReceived("M3LongAtomCursor#atomId/0");
    }

    public long startByte() {
        throw new M3NotYetReceived("M3LongAtomCursor#startByte/0");
    }

    public long endByte() {
        throw new M3NotYetReceived("M3LongAtomCursor#endByte/0");
    }

    public long hash64() {
        throw new M3NotYetReceived("M3LongAtomCursor#hash64/0");
    }

    public long scalar() {
        throw new M3NotYetReceived("M3LongAtomCursor#scalar/0");
    }

    public ByteBuffer rawSlice() throws IOException {
        throw new M3NotYetReceived("M3LongAtomCursor#rawSlice/0");
    }

    public ByteBuffer contentSlice() throws IOException {
        throw new M3NotYetReceived("M3LongAtomCursor#contentSlice/0");
    }
}
