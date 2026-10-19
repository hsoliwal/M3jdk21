// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexWritableSearchIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexWritableSearchIndex.java}
 * blob {@code 0c84ab4c58563be64e3f232bca5acdea3b6acd56} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f6fc7683407b15881a057c5c027267e64c8752a0309475b9f9f4dd20b4be91ee}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3WritableSearchIndex}); projection
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
 * </ul>
 */
final class M3WritableSearchIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code runtime} of type {@code IndexRuntime}. */
    private int[] runtimeLane;

    /** primitive lane for source field {@code languageId} of type {@code int}. */
    private int languageId;

    /** int handle lane (offsets into parent-owned storage) for source field {@code rowByWord} of type {@code Map<String, Integer>}. */
    private int[] rowByWordLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code regexRows} of type {@code Map<String, int[]>}. */
    private int[] regexRowsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code prefixTrie} of type {@code MIndexMutableTrie}. */
    private int[] prefixTrieLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code suffixTrie} of type {@code MIndexMutableTrie}. */
    private int[] suffixTrieLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code ngrams} of type {@code MIndexMutableNGramIndex}. */
    private int[] ngramsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code words} of type {@code String[]}. */
    private int[] wordsLane;

    /** primitive lane for source field {@code ids} of type {@code long[]}. */
    private long[] ids;

    /** primitive lane for source field {@code size} of type {@code int}. */
    private int size;

    /** primitive lane for source field {@code generation} of type {@code long}. */
    private long generation;

    private M3WritableSearchIndex() {
        throw new M3NotYetReceived("M3WritableSearchIndex#<init>/0");
    }
}
