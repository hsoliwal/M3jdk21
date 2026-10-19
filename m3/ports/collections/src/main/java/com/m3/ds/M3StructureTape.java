// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.nio.ByteBuffer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexStructureTape}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexStructureTape.java}
 * blob {@code a94d5dcc3464645e51bac501384c48f2bc8d8958} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 3bebb38b6eabc783eb1f41b9baa6aaa476fd286b048ddfdafe13a5df9cce3a5a}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3StructureTape}); projection
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
 *   <li>{@code nested types not received: Kind, Builder}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member kind(int) (return type)}</li>
 * </ul>
 */
public final class M3StructureTape {
    public static final int FLAG_ESCAPED = 1;

    public static final int FLAG_KNOWN_SYMBOL = 1 << 1;

    public static final int FLAG_INTEGER = 1 << 2;

    public static final int FLAG_INT32 = 1 << 3;

    public static final int FLAG_NEGATIVE = 1 << 4;

    public static final int FLAG_NUMBER_OVERFLOW = 1 << 5;

    public static final int FLAG_KEY_ESCAPED = 1 << 6;

    public static final int FLAG_KNOWN_KEY = 1 << 7;

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

    /** primitive lane for source field {@code subtreeEnds} of type {@code int[]}. */
    private int[] subtreeEnds;

    /** primitive lane for source field {@code parents} of type {@code int[]}. */
    private int[] parents;

    /** primitive lane for source field {@code childCounts} of type {@code int[]}. */
    private int[] childCounts;

    /** primitive lane for source field {@code keyIds} of type {@code int[]}. */
    private int[] keyIds;

    /** primitive lane for source field {@code keyStarts} of type {@code int[]}. */
    private int[] keyStarts;

    /** primitive lane for source field {@code keyEnds} of type {@code int[]}. */
    private int[] keyEnds;

    /** primitive lane for source field {@code symbolIds} of type {@code int[]}. */
    private int[] symbolIds;

    /** primitive lane for source field {@code scalars} of type {@code long[]}. */
    private long[] scalars;

    /** primitive lane for source field {@code root} of type {@code int}. */
    private int root;

    private M3StructureTape() {
        throw new M3NotYetReceived("M3StructureTape#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3StructureTape#size/0");
    }

    public int root() {
        throw new M3NotYetReceived("M3StructureTape#root/0");
    }

    public int kindCode(int node) {
        throw new M3NotYetReceived("M3StructureTape#kindCode/1");
    }

    public int flags(int node) {
        throw new M3NotYetReceived("M3StructureTape#flags/1");
    }

    public boolean hasFlag(int node, int mask) {
        throw new M3NotYetReceived("M3StructureTape#hasFlag/2");
    }

    public int startByte(int node) {
        throw new M3NotYetReceived("M3StructureTape#startByte/1");
    }

    public int endByte(int node) {
        throw new M3NotYetReceived("M3StructureTape#endByte/1");
    }

    public int subtreeEnd(int node) {
        throw new M3NotYetReceived("M3StructureTape#subtreeEnd/1");
    }

    public int parent(int node) {
        throw new M3NotYetReceived("M3StructureTape#parent/1");
    }

    public int childCount(int node) {
        throw new M3NotYetReceived("M3StructureTape#childCount/1");
    }

    public int keyId(int node) {
        throw new M3NotYetReceived("M3StructureTape#keyId/1");
    }

    public int keyStartByte(int node) {
        throw new M3NotYetReceived("M3StructureTape#keyStartByte/1");
    }

    public int keyEndByte(int node) {
        throw new M3NotYetReceived("M3StructureTape#keyEndByte/1");
    }

    public int symbolId(int node) {
        throw new M3NotYetReceived("M3StructureTape#symbolId/1");
    }

    public long scalar(int node) {
        throw new M3NotYetReceived("M3StructureTape#scalar/1");
    }

    public int firstChild(int node) {
        throw new M3NotYetReceived("M3StructureTape#firstChild/1");
    }

    public int nextSibling(int node) {
        throw new M3NotYetReceived("M3StructureTape#nextSibling/1");
    }

    public int child(int node, int ordinal) {
        throw new M3NotYetReceived("M3StructureTape#child/2");
    }

    public ByteBuffer rawSlice(int node) {
        throw new M3NotYetReceived("M3StructureTape#rawSlice/1");
    }

    public ByteBuffer rawKeySlice(int node) {
        throw new M3NotYetReceived("M3StructureTape#rawKeySlice/1");
    }

    public String materializeString(int node) {
        throw new M3NotYetReceived("M3StructureTape#materializeString/1");
    }

    public String materializeKey(int node) {
        throw new M3NotYetReceived("M3StructureTape#materializeKey/1");
    }

    public byte[] sourceCopy() {
        throw new M3NotYetReceived("M3StructureTape#sourceCopy/0");
    }
}
