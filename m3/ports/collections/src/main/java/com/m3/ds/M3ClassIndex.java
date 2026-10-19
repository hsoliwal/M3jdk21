// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.type.MIndexClassIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/type/MIndexClassIndex.java}
 * blob {@code 320575cc253e6616bce9864893b6e08e123dc8e3} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 162b7acab210519c71fbdc9aa6aaa31ac6d04858630a567fe297255e29b4265d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.type.M3ClassIndex}); projection
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
 *   <li>{@code member findClass(int, CharSequence, CharSequence) (return type)}</li>
 *   <li>{@code member findMember(int, MIndexMemberKind, CharSequence, CharSequence) (return type)}</li>
 *   <li>{@code member findField(int, CharSequence, CharSequence) (return type)}</li>
 *   <li>{@code member findMethod(int, CharSequence, CharSequence) (return type)}</li>
 *   <li>{@code member membersNamed(int, MIndexMemberKind, CharSequence) (return type)}</li>
 *   <li>{@code member methodsNamed(int, CharSequence) (return type)}</li>
 *   <li>{@code member memberAtRow(int) (return type)}</li>
 * </ul>
 */
public final class M3ClassIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code pool} of type {@code MIndexClassPool}. */
    private int[] poolLane;

    /** primitive lane for source field {@code classCount} of type {@code int}. */
    private int classCount;

    /** primitive lane for source field {@code memberCount} of type {@code int}. */
    private int memberCount;

    /** primitive lane for source field {@code classHashes} of type {@code long[]}. */
    private long[] classHashes;

    /** primitive lane for source field {@code classSlots} of type {@code int[]}. */
    private int[] classSlots;

    /** primitive lane for source field {@code memberOwnerHandles} of type {@code int[]}. */
    private int[] memberOwnerHandles;

    /** primitive lane for source field {@code memberOrdinals} of type {@code int[]}. */
    private int[] memberOrdinals;

    /** primitive lane for source field {@code memberExactHashes} of type {@code long[]}. */
    private long[] memberExactHashes;

    /** primitive lane for source field {@code memberNameHashes} of type {@code long[]}. */
    private long[] memberNameHashes;

    /** primitive lane for source field {@code memberExactSlots} of type {@code int[]}. */
    private int[] memberExactSlots;

    /** primitive lane for source field {@code memberNameSlots} of type {@code int[]}. */
    private int[] memberNameSlots;

    /** primitive lane for source field {@code nextByName} of type {@code int[]}. */
    private int[] nextByName;

    private M3ClassIndex() {
        throw new M3NotYetReceived("M3ClassIndex#<init>/0");
    }

    public static M3ClassIndex snapshot(M3ClassPool pool) {
        throw new M3NotYetReceived("M3ClassIndex#snapshot/1");
    }

    public M3ClassPool pool() {
        throw new M3NotYetReceived("M3ClassIndex#pool/0");
    }

    public int classCount() {
        throw new M3NotYetReceived("M3ClassIndex#classCount/0");
    }

    public int memberCount() {
        throw new M3NotYetReceived("M3ClassIndex#memberCount/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3ClassIndex#primitivePayloadBytes/0");
    }
}
