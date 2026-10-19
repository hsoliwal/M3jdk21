// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.type.MIndexClassPool}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/type/MIndexClassPool.java}
 * blob {@code 22aee35834c295af0d1da83fdf73ec55868e4045} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f29449f57a4e7927c2a5f5773cef5029de57628729316e1054d43fb79f8a5f27}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.type.M3ClassPool}); projection
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
 *   <li>{@code nested types not received: HashKind}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member MIndexClassPool(IndexResolver, int) (parameter resolver)}</li>
 *   <li>{@code member MIndexClassPool(MatIndexStringPool, int) (parameter textPool)}</li>
 *   <li>{@code member textPool() (return type)}</li>
 *   <li>{@code member value(int) (return type)}</li>
 *   <li>{@code member intern(MIndexClassSpec) (return type)}</li>
 *   <li>{@code member find(int, String, String) (return type)}</li>
 *   <li>{@code member snapshot() (return type)}</li>
 * </ul>
 */
public final class M3ClassPool {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code textPool} of type {@code MatIndexStringPool}. */
    private int[] textPoolLane;

    /** primitive lane for source field {@code languageId} of type {@code int}. */
    private int languageId;

    /** primitive lane for source field {@code namespaceIds} of type {@code int[]}. */
    private int[] namespaceIds;

    /** primitive lane for source field {@code kinds} of type {@code byte[]}. */
    private byte[] kinds;

    /** primitive lane for source field {@code modifiers} of type {@code int[]}. */
    private int[] modifiers;

    /** primitive lane for source field {@code binaryNameHandles} of type {@code int[]}. */
    private int[] binaryNameHandles;

    /** primitive lane for source field {@code packageNameHandles} of type {@code int[]}. */
    private int[] packageNameHandles;

    /** primitive lane for source field {@code moduleNameHandles} of type {@code int[]}. */
    private int[] moduleNameHandles;

    /** primitive lane for source field {@code superHandles} of type {@code int[]}. */
    private int[] superHandles;

    /** primitive lane for source field {@code interfaceStarts} of type {@code int[]}. */
    private int[] interfaceStarts;

    /** primitive lane for source field {@code interfaceCounts} of type {@code int[]}. */
    private int[] interfaceCounts;

    /** primitive lane for source field {@code memberStarts} of type {@code int[]}. */
    private int[] memberStarts;

    /** primitive lane for source field {@code memberCounts} of type {@code int[]}. */
    private int[] memberCounts;

    /** primitive lane for source field {@code identityHashes} of type {@code long[]}. */
    private long[] identityHashes;

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

    /** primitive lane for source field {@code interfaceArena} of type {@code int[]}. */
    private int[] interfaceArena;

    /** primitive lane for source field {@code interfaceSize} of type {@code int}. */
    private int interfaceSize;

    /** primitive lane for source field {@code memberOwners} of type {@code int[]}. */
    private int[] memberOwners;

    /** primitive lane for source field {@code memberKinds} of type {@code byte[]}. */
    private byte[] memberKinds;

    /** primitive lane for source field {@code memberNameHandles} of type {@code int[]}. */
    private int[] memberNameHandles;

    /** primitive lane for source field {@code memberDescriptorHandles} of type {@code int[]}. */
    private int[] memberDescriptorHandles;

    /** primitive lane for source field {@code memberModifierBits} of type {@code int[]}. */
    private int[] memberModifierBits;

    /** primitive lane for source field {@code memberExactHashes} of type {@code long[]}. */
    private long[] memberExactHashes;

    /** primitive lane for source field {@code memberStructuralHashes} of type {@code long[]}. */
    private long[] memberStructuralHashes;

    /** primitive lane for source field {@code memberLogicHashes} of type {@code long[]}. */
    private long[] memberLogicHashes;

    /** primitive lane for source field {@code memberSimHashes} of type {@code long[]}. */
    private long[] memberSimHashes;

    /** primitive lane for source field {@code memberSize} of type {@code int}. */
    private int memberSize;

    private M3ClassPool() {
        throw new M3NotYetReceived("M3ClassPool#<init>/0");
    }

    public int languageId() {
        throw new M3NotYetReceived("M3ClassPool#languageId/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3ClassPool#size/0");
    }

    public int retainedInterfaceHandleCount() {
        throw new M3NotYetReceived("M3ClassPool#retainedInterfaceHandleCount/0");
    }

    public int retainedMemberCount() {
        throw new M3NotYetReceived("M3ClassPool#retainedMemberCount/0");
    }

    public long retainedTextTokenCount() {
        throw new M3NotYetReceived("M3ClassPool#retainedTextTokenCount/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3ClassPool#primitivePayloadBytes/0");
    }
}
