// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.type.MIndexJvmTypePool}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/type/MIndexJvmTypePool.java}
 * blob {@code cd652b1019f75247235af65cee4413382547f439} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 48dd3d6f8860a2e1383413ba94eada0ca066590a61daa6575039154816bb2d4a}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.type.M3JvmTypePool}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S13}.
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
 *   <li>{@code nested types not received: Parsed}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member MIndexJvmTypePool(IndexResolver, int) (parameter resolver)}</li>
 *   <li>{@code member MIndexJvmTypePool(MatIndexStringPool, int) (parameter textPool)}</li>
 *   <li>{@code member textPool() (return type)}</li>
 *   <li>{@code member value(int) (return type)}</li>
 *   <li>{@code member internDescriptor(String) (return type)}</li>
 *   <li>{@code member findDescriptor(String) (return type)}</li>
 * </ul>
 */
public final class M3JvmTypePool {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code textPool} of type {@code MatIndexStringPool}. */
    private int[] textPoolLane;

    /** primitive lane for source field {@code languageId} of type {@code int}. */
    private int languageId;

    /** primitive lane for source field {@code kinds} of type {@code byte[]}. */
    private byte[] kinds;

    /** primitive lane for source field {@code descriptorHandles} of type {@code int[]}. */
    private int[] descriptorHandles;

    /** primitive lane for source field {@code declaredNameHandles} of type {@code int[]}. */
    private int[] declaredNameHandles;

    /** primitive lane for source field {@code componentHandles} of type {@code int[]}. */
    private int[] componentHandles;

    /** primitive lane for source field {@code dimensions} of type {@code int[]}. */
    private int[] dimensions;

    /** primitive lane for source field {@code exactHashes} of type {@code long[]}. */
    private long[] exactHashes;

    /** primitive lane for source field {@code structuralHashes} of type {@code long[]}. */
    private long[] structuralHashes;

    /** primitive lane for source field {@code logicHashes} of type {@code long[]}. */
    private long[] logicHashes;

    /** primitive lane for source field {@code simHashes} of type {@code long[]}. */
    private long[] simHashes;

    /** primitive lane for source field {@code slots} of type {@code int[]}. */
    private int[] slots;

    /** primitive lane for source field {@code size} of type {@code int}. */
    private int size;

    private M3JvmTypePool() {
        throw new M3NotYetReceived("M3JvmTypePool#<init>/0");
    }

    public int languageId() {
        throw new M3NotYetReceived("M3JvmTypePool#languageId/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3JvmTypePool#size/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3JvmTypePool#primitivePayloadBytes/0");
    }
}
