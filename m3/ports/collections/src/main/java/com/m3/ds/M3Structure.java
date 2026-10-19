// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexStructure}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexStructure.java}
 * blob {@code 1d7023d650cee46def54d84641bddafe1b262385} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code ea07dcc454a90994ba53ebab3185c746026cdd792b41bc79e8f7139602327bbb}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3Structure}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
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
 *   <li>{@code member resolver() (return type)}</li>
 *   <li>{@code member root() (return type)}</li>
 *   <li>{@code member node(int) (return type)}</li>
 * </ul>
 */
public final class M3Structure {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code resolver} of type {@code IndexResolver}. */
    private int[] resolverLane;

    /** primitive lane for source field {@code languageId} of type {@code int}. */
    private int languageId;

    /** primitive lane for source field {@code rootId} of type {@code int}. */
    private int rootId;

    /** primitive lane for source field {@code types} of type {@code byte[]}. */
    private byte[] types;

    /** primitive lane for source field {@code first} of type {@code long[]}. */
    private long[] first;

    /** primitive lane for source field {@code second} of type {@code long[]}. */
    private long[] second;

    /** primitive lane for source field {@code hashes} of type {@code long[]}. */
    private long[] hashes;

    /** primitive lane for source field {@code children} of type {@code int[]}. */
    private int[] children;

    /** int handle lane (offsets into parent-owned storage) for source field {@code texts} of type {@code MIndexString[]}. */
    private int[] textsLane;

    /** primitive lane for source field {@code binaryArena} of type {@code byte[]}. */
    private byte[] binaryArena;

    /** int handle lane (offsets into parent-owned storage) for source field {@code metadata} of type {@code MIndexRepresentationMetadata[]}. */
    private int[] metadataLane;

    /** primitive lane for source field {@code metadataStart} of type {@code int[]}. */
    private int[] metadataStart;

    /** primitive lane for source field {@code metadataCount} of type {@code int[]}. */
    private int[] metadataCount;

    /** primitive lane for source field {@code mapIndexStart} of type {@code int[]}. */
    private int[] mapIndexStart;

    /** primitive lane for source field {@code mapIndexMask} of type {@code int[]}. */
    private int[] mapIndexMask;

    /** primitive lane for source field {@code mapSlots} of type {@code int[]}. */
    private int[] mapSlots;

    private M3Structure() {
        throw new M3NotYetReceived("M3Structure#<init>/0");
    }

    public int languageId() {
        throw new M3NotYetReceived("M3Structure#languageId/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3Structure#size/0");
    }

    public int rootId() {
        throw new M3NotYetReceived("M3Structure#rootId/0");
    }
}
