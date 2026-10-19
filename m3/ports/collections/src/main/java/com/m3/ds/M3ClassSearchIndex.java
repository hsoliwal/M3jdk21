// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.type.MIndexClassSearchIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/type/MIndexClassSearchIndex.java}
 * blob {@code 822408ede3c454c712fbea3e90d7b2208b67cfe8} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 489152a79ab16df57e4ad96ada4fc68b785c7293076846361442283cee083d14}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.type.M3ClassSearchIndex}); projection
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
 *   <li>{@code nested types not received: Key}</li>
 *   <li>{@code member binaryNamePrefix(CharSequence, int, int) (return type)}</li>
 *   <li>{@code member simpleNamePrefix(CharSequence, int, int) (return type)}</li>
 *   <li>{@code member classesInPackage(CharSequence, int, int) (return type)}</li>
 *   <li>{@code member binaryNameAtRank(int) (return type)}</li>
 *   <li>{@code member simpleNameAtRank(int) (return type)}</li>
 *   <li>{@code member packageAtRank(int) (return type)}</li>
 * </ul>
 */
public final class M3ClassSearchIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code pool} of type {@code MIndexClassPool}. */
    private int[] poolLane;

    /** primitive lane for source field {@code classCount} of type {@code int}. */
    private int classCount;

    /** primitive lane for source field {@code binaryNameOrder} of type {@code int[]}. */
    private int[] binaryNameOrder;

    /** primitive lane for source field {@code simpleNameOrder} of type {@code int[]}. */
    private int[] simpleNameOrder;

    /** primitive lane for source field {@code packageOrder} of type {@code int[]}. */
    private int[] packageOrder;

    private M3ClassSearchIndex() {
        throw new M3NotYetReceived("M3ClassSearchIndex#<init>/0");
    }

    public static M3ClassSearchIndex snapshot(M3ClassPool pool) {
        throw new M3NotYetReceived("M3ClassSearchIndex#snapshot/1");
    }

    public M3ClassPool pool() {
        throw new M3NotYetReceived("M3ClassSearchIndex#pool/0");
    }

    public int classCount() {
        throw new M3NotYetReceived("M3ClassSearchIndex#classCount/0");
    }

    public int[] binaryNamePrefixHandles(CharSequence prefix, int offset, int limit) {
        throw new M3NotYetReceived("M3ClassSearchIndex#binaryNamePrefixHandles/3");
    }

    public int[] simpleNamePrefixHandles(CharSequence prefix, int offset, int limit) {
        throw new M3NotYetReceived("M3ClassSearchIndex#simpleNamePrefixHandles/3");
    }

    public int[] packageHandles(CharSequence packageName, int offset, int limit) {
        throw new M3NotYetReceived("M3ClassSearchIndex#packageHandles/3");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3ClassSearchIndex#primitivePayloadBytes/0");
    }
}
