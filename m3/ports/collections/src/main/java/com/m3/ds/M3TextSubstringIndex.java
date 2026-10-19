// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexTextSubstringIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexTextSubstringIndex.java}
 * blob {@code 1f8e6d1945be6919d0070576632cb0384d5495ac} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 995e5436922838b61c4eaf03edeef2907d58892d63fd82e7dd0daf51abb63550}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3TextSubstringIndex}); projection
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
 *   <li>{@code nested types not received: CommonSubstring, Builder, Transitions}</li>
 *   <li>{@code member compile(MIndexText) (parameter source)}</li>
 *   <li>{@code member source() (return type)}</li>
 *   <li>{@code member longestCommonSubstring(CharSequence) (return type)}</li>
 * </ul>
 */
public final class M3TextSubstringIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexText}. */
    private int[] sourceLane;

    /** primitive lane for source field {@code length} of type {@code int}. */
    private int length;

    /** primitive lane for source field {@code maxLength} of type {@code int[]}. */
    private int[] maxLength;

    /** primitive lane for source field {@code suffixLink} of type {@code int[]}. */
    private int[] suffixLink;

    /** primitive lane for source field {@code occurrences} of type {@code int[]}. */
    private int[] occurrences;

    /** primitive lane for source field {@code minimumEnd} of type {@code int[]}. */
    private int[] minimumEnd;

    /** primitive lane for source field {@code maximumEnd} of type {@code int[]}. */
    private int[] maximumEnd;

    /** int handle lane (offsets into parent-owned storage) for source field {@code transitions} of type {@code MIndexUtf16TransitionIndex}. */
    private int[] transitionsLane;

    /** primitive lane for source field {@code distinctSubstrings} of type {@code long}. */
    private long distinctSubstrings;

    private M3TextSubstringIndex() {
        throw new M3NotYetReceived("M3TextSubstringIndex#<init>/0");
    }

    public int length() {
        throw new M3NotYetReceived("M3TextSubstringIndex#length/0");
    }

    public boolean contains(CharSequence pattern) {
        throw new M3NotYetReceived("M3TextSubstringIndex#contains/1");
    }

    public long occurrenceCount(CharSequence pattern) {
        throw new M3NotYetReceived("M3TextSubstringIndex#occurrenceCount/1");
    }

    public int firstIndexOf(CharSequence pattern) {
        throw new M3NotYetReceived("M3TextSubstringIndex#firstIndexOf/1");
    }

    public int lastIndexOf(CharSequence pattern) {
        throw new M3NotYetReceived("M3TextSubstringIndex#lastIndexOf/1");
    }

    public long distinctSubstringCount() {
        throw new M3NotYetReceived("M3TextSubstringIndex#distinctSubstringCount/0");
    }
}
