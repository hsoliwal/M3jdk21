// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexTextPalindromicTree}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexTextPalindromicTree.java}
 * blob {@code 47b86fa7343d29e89acb94c97a215c42ae3e2c8a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 47beebebaa9ced824719840787b95af032ad63c9b5eb3d873dd7aef0359beda5}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3TextPalindromicTree}); projection
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
 *   <li>{@code nested types not received: PalindromeSpan, Builder, Transitions}</li>
 *   <li>{@code member compile(MIndexText) (parameter source)}</li>
 *   <li>{@code member source() (return type)}</li>
 *   <li>{@code member longestSpan() (return type)}</li>
 * </ul>
 */
public final class M3TextPalindromicTree {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code source} of type {@code MIndexText}. */
    private int[] sourceLane;

    /** primitive lane for source field {@code length} of type {@code int}. */
    private int length;

    /** primitive lane for source field {@code occurrences} of type {@code long[]}. */
    private long[] occurrences;

    /** primitive lane for source field {@code minimumEnd} of type {@code int[]}. */
    private int[] minimumEnd;

    /** primitive lane for source field {@code maximumEnd} of type {@code int[]}. */
    private int[] maximumEnd;

    /** int handle lane (offsets into parent-owned storage) for source field {@code transitions} of type {@code MIndexUtf16TransitionIndex}. */
    private int[] transitionsLane;

    /** primitive lane for source field {@code distinctPalindromes} of type {@code long}. */
    private long distinctPalindromes;

    /** int handle lane (offsets into parent-owned storage) for source field {@code longest} of type {@code PalindromeSpan}. */
    private int[] longestLane;

    private M3TextPalindromicTree() {
        throw new M3NotYetReceived("M3TextPalindromicTree#<init>/0");
    }

    public int length() {
        throw new M3NotYetReceived("M3TextPalindromicTree#length/0");
    }

    public long distinctPalindromeCount() {
        throw new M3NotYetReceived("M3TextPalindromicTree#distinctPalindromeCount/0");
    }

    public long occurrenceCount(CharSequence pattern) {
        throw new M3NotYetReceived("M3TextPalindromicTree#occurrenceCount/1");
    }

    public int firstIndexOf(CharSequence pattern) {
        throw new M3NotYetReceived("M3TextPalindromicTree#firstIndexOf/1");
    }

    public int lastIndexOf(CharSequence pattern) {
        throw new M3NotYetReceived("M3TextPalindromicTree#lastIndexOf/1");
    }
}
