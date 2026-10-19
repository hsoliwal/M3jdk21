// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexLuceneTermDictionary}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexLuceneTermDictionary.java}
 * blob {@code 25a5090d6d55409ad3208b3da7ff644f389e249f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 61af9db772736c5bcb350049028c0b46ece7241dce5f91ef354bdf373e0bb5ba}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3LuceneTermDictionary}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S23}.
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
 *   <li>{@code nested types not received: OrdRange, Node}</li>
 *   <li>{@code member freeze(MIndexString[]) (parameter input)}</li>
 *   <li>{@code member freeze(MIndexString[], MIndexPrecomputeLimits, IProgressMonitor) (parameter input)}</li>
 *   <li>{@code member sourceSha256(MIndexString[]) (parameter input)}</li>
 *   <li>{@code member termAt(int) (return type)}</li>
 *   <li>{@code member prefixRange(CharSequence) (return type)}</li>
 * </ul>
 */
public final class M3LuceneTermDictionary {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code sourceSha256} of type {@code String}. */
    private int[] sourceSha256Lane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code terms} of type {@code MIndexString[]}. */
    private int[] termsLane;

    /** primitive lane for source field {@code firstArc} of type {@code int[]}. */
    private int[] firstArc;

    /** primitive lane for source field {@code arcCount} of type {@code int[]}. */
    private int[] arcCount;

    /** primitive lane for source field {@code terminalOrd} of type {@code int[]}. */
    private int[] terminalOrd;

    /** primitive lane for source field {@code firstOrd} of type {@code int[]}. */
    private int[] firstOrd;

    /** primitive lane for source field {@code lastOrdExclusive} of type {@code int[]}. */
    private int[] lastOrdExclusive;

    /** primitive lane for source field {@code arcLabel} of type {@code char[]}. */
    private char[] arcLabel;

    /** primitive lane for source field {@code arcTarget} of type {@code int[]}. */
    private int[] arcTarget;

    /** primitive lane for source field {@code primitivePayloadBytes} of type {@code long}. */
    private long primitivePayloadBytes;

    private M3LuceneTermDictionary() {
        throw new M3NotYetReceived("M3LuceneTermDictionary#<init>/0");
    }

    public String sourceSha256() {
        throw new M3NotYetReceived("M3LuceneTermDictionary#sourceSha256/0");
    }

    public int termCount() {
        throw new M3NotYetReceived("M3LuceneTermDictionary#termCount/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3LuceneTermDictionary#primitivePayloadBytes/0");
    }

    public int exactOrdinal(CharSequence term) {
        throw new M3NotYetReceived("M3LuceneTermDictionary#exactOrdinal/1");
    }

    public int seekCeilOrdinal(CharSequence term) {
        throw new M3NotYetReceived("M3LuceneTermDictionary#seekCeilOrdinal/1");
    }

    public int seekFloorOrdinal(CharSequence term) {
        throw new M3NotYetReceived("M3LuceneTermDictionary#seekFloorOrdinal/1");
    }

    public int longestPrefixOrdinal(CharSequence value) {
        throw new M3NotYetReceived("M3LuceneTermDictionary#longestPrefixOrdinal/1");
    }

    public String contentSha256() {
        throw new M3NotYetReceived("M3LuceneTermDictionary#contentSha256/0");
    }
}
