// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexValues}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexValues.java}
 * blob {@code 5ffe6abae243a9420ff86dfaf686056ecaa4a7f4} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code da38aa6882d4cddb1406131dc2002c53a087c48ba8ae86a4eeb789a653bec8c9}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3Values}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S11}.
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
 *   <li>{@code nested types not received: Kind, Builder, Key, MIndexStringPoolAccess}</li>
 *   <li>{@code member kind(int) (return type)}</li>
 *   <li>{@code member text(int) (return type)}</li>
 *   <li>{@code member findScalar(Kind, long, long, int) (parameter wanted)}</li>
 * </ul>
 */
public final class M3Values {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code kinds} of type {@code byte[]}. */
    private byte[] kinds;

    /** primitive lane for source field {@code first} of type {@code long[]}. */
    private long[] first;

    /** primitive lane for source field {@code second} of type {@code long[]}. */
    private long[] second;

    /** primitive lane for source field {@code links} of type {@code int[]}. */
    private int[] links;

    /** primitive lane for source field {@code binary} of type {@code byte[]}. */
    private byte[] binary;

    /** primitive lane for source field {@code digests} of type {@code byte[]}. */
    private byte[] digests;

    /** primitive lane for source field {@code mapSlotStarts} of type {@code int[]}. */
    private int[] mapSlotStarts;

    /** primitive lane for source field {@code mapSlotMasks} of type {@code int[]}. */
    private int[] mapSlotMasks;

    /** primitive lane for source field {@code mapSlots} of type {@code int[]}. */
    private int[] mapSlots;

    private M3Values() {
        throw new M3NotYetReceived("M3Values#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3Values#size/0");
    }

    public int kindCode(int id) {
        throw new M3NotYetReceived("M3Values#kindCode/1");
    }

    public long first(int id) {
        throw new M3NotYetReceived("M3Values#first/1");
    }

    public long second(int id) {
        throw new M3NotYetReceived("M3Values#second/1");
    }

    public int length(int id) {
        throw new M3NotYetReceived("M3Values#length/1");
    }

    public int child(int id, int position) {
        throw new M3NotYetReceived("M3Values#child/2");
    }

    public byte[] bytes(int id) {
        throw new M3NotYetReceived("M3Values#bytes/1");
    }

    public int mapValue(int mapId, CharSequence key) {
        throw new M3NotYetReceived("M3Values#mapValue/2");
    }

    public int mapValueByTextId(int mapId, int keyTextId) {
        throw new M3NotYetReceived("M3Values#mapValueByTextId/2");
    }

    public String semanticSha256(int id) {
        throw new M3NotYetReceived("M3Values#semanticSha256/1");
    }

    public byte[] semanticDigest(int id) {
        throw new M3NotYetReceived("M3Values#semanticDigest/1");
    }
}
