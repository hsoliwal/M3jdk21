// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexSubstringIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexSubstringIndex.java}
 * blob {@code 7e4b4bab8fcee9685db30692bf9260c40f5a703d} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2d4a974928427b9a220605c01a634e6474ae95e6dabb603ca7691e3da99b5884}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3SubstringIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S10}.
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
 *   <li>{@code member compile(MIndexString) (parameter source)}</li>
 * </ul>
 */
public final class M3SubstringIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code text} of type {@code char[]}. */
    private char[] text;

    /** primitive lane for source field {@code suffixes} of type {@code int[]}. */
    private int[] suffixes;

    /** primitive lane for source field {@code rank} of type {@code int[]}. */
    private int[] rank;

    /** primitive lane for source field {@code lcp} of type {@code int[]}. */
    private int[] lcp;

    /** primitive lane for source field {@code suffixMinimum} of type {@code int[]}. */
    private int[] suffixMinimum;

    /** primitive lane for source field {@code lcpMinimum} of type {@code int[]}. */
    private int[] lcpMinimum;

    /** primitive lane for source field {@code longestRepeat} of type {@code int}. */
    private int longestRepeat;

    /** primitive lane for source field {@code prefixHash} of type {@code long[]}. */
    private long[] prefixHash;

    /** primitive lane for source field {@code powers} of type {@code long[]}. */
    private long[] powers;

    private M3SubstringIndex() {
        throw new M3NotYetReceived("M3SubstringIndex#<init>/0");
    }

    public int length() {
        throw new M3NotYetReceived("M3SubstringIndex#length/0");
    }

    public int suffixAt(int sortedRank) {
        throw new M3NotYetReceived("M3SubstringIndex#suffixAt/1");
    }

    public int adjacentLcpAt(int sortedRank) {
        throw new M3NotYetReceived("M3SubstringIndex#adjacentLcpAt/1");
    }

    public int longestRepeatedSubstringLength() {
        throw new M3NotYetReceived("M3SubstringIndex#longestRepeatedSubstringLength/0");
    }

    public int[] suffixRange(CharSequence pattern) {
        throw new M3NotYetReceived("M3SubstringIndex#suffixRange/1");
    }

    public int firstIndexOf(CharSequence pattern) {
        throw new M3NotYetReceived("M3SubstringIndex#firstIndexOf/1");
    }

    public int[] occurrenceOffsets(CharSequence pattern) {
        throw new M3NotYetReceived("M3SubstringIndex#occurrenceOffsets/1");
    }

    public long rangeFingerprint(int from, int to) {
        throw new M3NotYetReceived("M3SubstringIndex#rangeFingerprint/2");
    }

    public int commonPrefix(int left, int right) {
        throw new M3NotYetReceived("M3SubstringIndex#commonPrefix/2");
    }

    public boolean sameRange(int aFrom, int aTo, int bFrom, int bTo) {
        throw new M3NotYetReceived("M3SubstringIndex#sameRange/4");
    }
}
