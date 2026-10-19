// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexLiteralSearchTree}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexLiteralSearchTree.java}
 * blob {@code 2214d5fbc20bbc88d797b03a05987ce331d3cc87} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code c75759a323bb8cc73ff92cb42e5a24ef7d406acd302a1fade27d800fc0ffc5d3}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3LiteralSearchTree}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
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
 *   <li>{@code nested types not received: Image, FastTransitions, MatchConsumer, Match, Builder}</li>
 *   <li>{@code member image() (return type)}</li>
 *   <li>{@code member fromImage(Image) (parameter image)}</li>
 *   <li>{@code member builder() (return type)}</li>
 *   <li>{@code member builder(int) (return type)}</li>
 *   <li>{@code member navigation() (return type)}</li>
 *   <li>{@code member navigation(IProgressMonitor) (return type)}</li>
 *   <li>{@code member navigation(MIndexTrieNavigation.Limits, IProgressMonitor) (return type)}</li>
 *   <li>{@code member exact(MIndexString) (parameter input)}</li>
 *   <li>{@code member findFirst(MIndexString) (return type)}</li>
 *   <li>{@code member findFirst(CharSequence) (return type)}</li>
 *   <li>{@code member visitMatches(CharSequence, MatchConsumer, IProgressMonitor) (parameter consumer)}</li>
 *   <li>{@code member visitMatches(CharSequence, MatchConsumer) (parameter consumer)}</li>
 *   <li>{@code member visitMatches(MIndexString, MatchConsumer, IProgressMonitor) (parameter input)}</li>
 *   <li>{@code member visitMatches(MIndexString, MatchConsumer) (parameter input)}</li>
 * </ul>
 */
public final class M3LiteralSearchTree {
    public static final int ROOT_STATE = 0;

    public static final int DEAD_STATE = -1;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code firstEdge} of type {@code int[]}. */
    private int[] firstEdge;

    /** primitive lane for source field {@code edgeCount} of type {@code int[]}. */
    private int[] edgeCount;

    /** primitive lane for source field {@code terminalPayload} of type {@code int[]}. */
    private int[] terminalPayload;

    /** primitive lane for source field {@code failure} of type {@code int[]}. */
    private int[] failure;

    /** primitive lane for source field {@code outputPayload} of type {@code int[]}. */
    private int[] outputPayload;

    /** primitive lane for source field {@code outputLength} of type {@code int[]}. */
    private int[] outputLength;

    /** primitive lane for source field {@code edgeChar} of type {@code char[]}. */
    private char[] edgeChar;

    /** primitive lane for source field {@code edgeTarget} of type {@code int[]}. */
    private int[] edgeTarget;

    /** primitive lane for source field {@code lookupMode} of type {@code byte[]}. */
    private byte[] lookupMode;

    /** primitive lane for source field {@code fastStart} of type {@code int[]}. */
    private int[] fastStart;

    /** primitive lane for source field {@code fastSpan} of type {@code int[]}. */
    private int[] fastSpan;

    /** primitive lane for source field {@code directBase} of type {@code char[]}. */
    private char[] directBase;

    /** primitive lane for source field {@code directTargets} of type {@code int[]}. */
    private int[] directTargets;

    /** int handle lane (offsets into parent-owned storage) for source field {@code navigation} of type {@code MIndexTrieNavigation}. */
    private int[] navigationLane;

    /** primitive lane for source field {@code buildingNavigation} of type {@code boolean}. */
    private boolean buildingNavigation;

    private M3LiteralSearchTree() {
        throw new M3NotYetReceived("M3LiteralSearchTree#<init>/0");
    }

    public int stateCount() {
        throw new M3NotYetReceived("M3LiteralSearchTree#stateCount/0");
    }

    public int edgeCount() {
        throw new M3NotYetReceived("M3LiteralSearchTree#edgeCount/0");
    }

    public int directAddressStateCount() {
        throw new M3NotYetReceived("M3LiteralSearchTree#directAddressStateCount/0");
    }

    public int binaryStateCount() {
        throw new M3NotYetReceived("M3LiteralSearchTree#binaryStateCount/0");
    }

    public int linearStateCount() {
        throw new M3NotYetReceived("M3LiteralSearchTree#linearStateCount/0");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3LiteralSearchTree#primitivePayloadBytes/0");
    }

    public int exact(CharSequence input) {
        throw new M3NotYetReceived("M3LiteralSearchTree#exact/1");
    }
}
