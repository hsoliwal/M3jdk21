// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.precompute.MIndexUniversalSnapshot}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/precompute-api/src/main/java/com/synexia/mindex/precompute/MIndexUniversalSnapshot.java}
 * blob {@code ea92a528ee161e5128514cbb7197ed21b1f48e76} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b66a1e478dbdcdeb8a4e3319992d9abbe8f8f2662b06c24f8f1314cfb133740f}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.precompute.M3UniversalSnapshot}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S20}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
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
 *   <li>{@code member api() (return type)}</li>
 *   <li>{@code member typePostings() (return type)}</li>
 *   <li>{@code member attributePostings() (return type)}</li>
 *   <li>{@code member relationForward() (return type)}</li>
 *   <li>{@code member relationReverse() (return type)}</li>
 *   <li>{@code member featurePostings() (return type)}</li>
 *   <li>{@code member postingIndex(MIndexPostingKind) (return type)}</li>
 *   <li>{@code member intersectPostings(MIndexPostingKind[], long[], int) (parameter kinds)}</li>
 *   <li>{@code member intersectPostings(MIndexPostingKind[], long[], int, MIndexPostingIntersectionKernel) (parameter kinds)}</li>
 * </ul>
 */
public final class M3UniversalSnapshot {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexUniversalSource}. */
    private int[] sourceLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code typePostings} of type {@code MIndexPostingIndex}. */
    private int[] typePostingsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code attributePostings} of type {@code MIndexPostingIndex}. */
    private int[] attributePostingsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code relationForward} of type {@code MIndexPostingIndex}. */
    private int[] relationForwardLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code relationReverse} of type {@code MIndexPostingIndex}. */
    private int[] relationReverseLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code featurePostings} of type {@code MIndexPostingIndex}. */
    private int[] featurePostingsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code apiTable} of type {@code MIndexApiPrecomputeTable}. */
    private int[] apiTableLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code contentSha256} of type {@code String}. */
    private int[] contentSha256Lane;

    private M3UniversalSnapshot() {
        throw new M3NotYetReceived("M3UniversalSnapshot#<init>/0");
    }

    public int entityCount() {
        throw new M3NotYetReceived("M3UniversalSnapshot#entityCount/0");
    }

    public int dictionarySize() {
        throw new M3NotYetReceived("M3UniversalSnapshot#dictionarySize/0");
    }

    public String text(int textId) {
        throw new M3NotYetReceived("M3UniversalSnapshot#text/1");
    }

    public int textId(CharSequence text) {
        throw new M3NotYetReceived("M3UniversalSnapshot#textId/1");
    }

    public int entityTypeTextId(int entityId) {
        throw new M3NotYetReceived("M3UniversalSnapshot#entityTypeTextId/1");
    }

    public String contentSha256() {
        throw new M3NotYetReceived("M3UniversalSnapshot#contentSha256/0");
    }

    public int[] entitiesByType(int typeTextId) {
        throw new M3NotYetReceived("M3UniversalSnapshot#entitiesByType/1");
    }

    public int[] entitiesByAttribute(int fieldTextId, int valueTextId) {
        throw new M3NotYetReceived("M3UniversalSnapshot#entitiesByAttribute/2");
    }

    public int[] entitiesWithFeature(int featureTextId) {
        throw new M3NotYetReceived("M3UniversalSnapshot#entitiesWithFeature/1");
    }

    public int[] relationTargets(int relationTypeTextId, int sourceEntityId) {
        throw new M3NotYetReceived("M3UniversalSnapshot#relationTargets/2");
    }

    public int[] relationSources(int relationTypeTextId, int targetEntityId) {
        throw new M3NotYetReceived("M3UniversalSnapshot#relationSources/2");
    }

    public int[] entitiesMatching(int[] attributeFieldTextIds, int[] attributeValueTextIds, int[] featureTextIds, int limit) {
        throw new M3NotYetReceived("M3UniversalSnapshot#entitiesMatching/4");
    }

    public long retainedBytes() {
        throw new M3NotYetReceived("M3UniversalSnapshot#retainedBytes/0");
    }

    public static int[] intersectSorted(int[] left, int[] right, int limit) {
        throw new M3NotYetReceived("M3UniversalSnapshot#intersectSorted/3");
    }
}
