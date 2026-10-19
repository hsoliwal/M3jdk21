// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexStructuralStore}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStructuralStore.java}
 * blob {@code 5c54171dbe1da428a9e2a608831564fc869efedb} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code dbad6d7bb4fbb80c2e6677fac5d6ec28d4f325f05c9c31184107f3ff75232b04}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3StructuralStore}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Co-donors with the same proposed name (BLOB_DIFF, not merged, review required):
 * <ul>
 *   <li>{@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexStructuralStore.java(at)e9de61ae98a8}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: TextKey, AstKey, EdgeKey}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member MIndexStructuralStore(IndexResolver) (parameter resolver)}</li>
 *   <li>{@code member resolver() (return type)}</li>
 *   <li>{@code member text(MIndexString) (return type)}</li>
 *   <li>{@code member textValue(MIndexRef) (return type)}</li>
 *   <li>{@code member textTokenCount(MIndexRef) (parameter ref)}</li>
 *   <li>{@code member textTokenAt(MIndexRef, int) (parameter ref)}</li>
 *   <li>{@code member token(long) (return type)}</li>
 *   <li>{@code member ast(long, long, long, MIndexRef, MIndexRef...) (return type)}</li>
 *   <li>{@code member astGrammar(MIndexRef) (parameter atom)}</li>
 *   <li>{@code member astKind(MIndexRef) (parameter atom)}</li>
 *   <li>{@code member astFlags(MIndexRef) (parameter atom)}</li>
 *   <li>{@code member astPayload(MIndexRef) (return type)}</li>
 *   <li>{@code member astChildCount(MIndexRef) (parameter atom)}</li>
 *   <li>{@code member astChildAt(MIndexRef, int) (return type)}</li>
 *   <li>{@code member astAtomCount(MIndexRef) (parameter atom)}</li>
 *   <li>{@code member astHeight(MIndexRef) (parameter atom)}</li>
 *   <li>{@code member edge(MIndexRef, MIndexRef, MIndexRef, int, long) (return type)}</li>
 *   <li>{@code member edgeFrom(MIndexRef) (return type)}</li>
 *   <li>{@code member edgeRelation(MIndexRef) (return type)}</li>
 *   <li>{@code member edgeTo(MIndexRef) (return type)}</li>
 *   <li>{@code member edgeOrdinal(MIndexRef) (parameter edge)}</li>
 *   <li>{@code member edgeFlags(MIndexRef) (parameter edge)}</li>
 *   <li>and 3 more</li>
 * </ul>
 */
public final class M3StructuralStore {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code resolver} of type {@code IndexResolver}. */
    private int[] resolverLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code textIndex} of type {@code Map<TextKey, Long>}. */
    private int[] textIndexLane;

    /** primitive lane for source field {@code textTokens} of type {@code long[]}. */
    private long[] textTokens;

    /** primitive lane for source field {@code textTokenSize} of type {@code int}. */
    private int textTokenSize;

    /** primitive lane for source field {@code textOffsets} of type {@code int[]}. */
    private int[] textOffsets;

    /** primitive lane for source field {@code textCounts} of type {@code int[]}. */
    private int[] textCounts;

    /** primitive lane for source field {@code textHashes} of type {@code long[]}. */
    private long[] textHashes;

    /** primitive lane for source field {@code textSize} of type {@code int}. */
    private int textSize;

    /** int handle lane (offsets into parent-owned storage) for source field {@code astIndex} of type {@code Map<AstKey, Long>}. */
    private int[] astIndexLane;

    /** primitive lane for source field {@code astGrammar} of type {@code long[]}. */
    private long[] astGrammar;

    /** primitive lane for source field {@code astKind} of type {@code long[]}. */
    private long[] astKind;

    /** primitive lane for source field {@code astFlags} of type {@code long[]}. */
    private long[] astFlags;

    /** primitive lane for source field {@code astPayloadSpace} of type {@code int[]}. */
    private int[] astPayloadSpace;

    /** primitive lane for source field {@code astPayloadId} of type {@code long[]}. */
    private long[] astPayloadId;

    /** primitive lane for source field {@code astChildOffset} of type {@code int[]}. */
    private int[] astChildOffset;

    /** primitive lane for source field {@code astChildCount} of type {@code int[]}. */
    private int[] astChildCount;

    /** primitive lane for source field {@code astHash} of type {@code long[]}. */
    private long[] astHash;

    /** primitive lane for source field {@code astAtomCount} of type {@code int[]}. */
    private int[] astAtomCount;

    /** primitive lane for source field {@code astHeight} of type {@code int[]}. */
    private int[] astHeight;

    /** primitive lane for source field {@code astChildSpaces} of type {@code int[]}. */
    private int[] astChildSpaces;

    /** primitive lane for source field {@code astChildIds} of type {@code long[]}. */
    private long[] astChildIds;

    /** primitive lane for source field {@code astChildSize} of type {@code int}. */
    private int astChildSize;

    /** primitive lane for source field {@code astSize} of type {@code int}. */
    private int astSize;

    /** int handle lane (offsets into parent-owned storage) for source field {@code edgeIndex} of type {@code Map<EdgeKey, Long>}. */
    private int[] edgeIndexLane;

    /** primitive lane for source field {@code edgeFromSpace} of type {@code int[]}. */
    private int[] edgeFromSpace;

    /** primitive lane for source field {@code edgeFromId} of type {@code long[]}. */
    private long[] edgeFromId;

    /** primitive lane for source field {@code edgeRelationSpace} of type {@code int[]}. */
    private int[] edgeRelationSpace;

    /** primitive lane for source field {@code edgeRelationId} of type {@code long[]}. */
    private long[] edgeRelationId;

    /** primitive lane for source field {@code edgeToSpace} of type {@code int[]}. */
    private int[] edgeToSpace;

    /** primitive lane for source field {@code edgeToId} of type {@code long[]}. */
    private long[] edgeToId;

    /** primitive lane for source field {@code edgeOrdinal} of type {@code int[]}. */
    private int[] edgeOrdinal;

    /** primitive lane for source field {@code edgeFlags} of type {@code long[]}. */
    private long[] edgeFlags;

    /** primitive lane for source field {@code edgeHash} of type {@code long[]}. */
    private long[] edgeHash;

    /** primitive lane for source field {@code edgeSize} of type {@code int}. */
    private int edgeSize;

    private M3StructuralStore() {
        throw new M3NotYetReceived("M3StructuralStore#<init>/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3StructuralStore#retainedPrimitiveBytes/0");
    }
}
