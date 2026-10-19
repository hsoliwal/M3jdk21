// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexCompositeSearchImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexCompositeSearchImage.java}
 * blob {@code ae0c809fb60df096358a63a9ba118cab75c8667a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code ef65eadaaf1383fca066b9b458d8188e6e030498f4d0fad0ddaed37b48f9be74}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3CompositeSearchImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S16}.
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
 *   <li>{@code nested types not received: IntCollector}</li>
 *   <li>{@code member compile(MIndexCompositeString, MIndexString) (parameter source)}</li>
 *   <li>{@code member pattern() (return type)}</li>
 * </ul>
 */
public final class M3CompositeSearchImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code sourceLength} of type {@code int}. */
    private int sourceLength;

    /** primitive lane for source field {@code patternContentIndex} of type {@code int}. */
    private int patternContentIndex;

    /** primitive lane for source field {@code patternLength} of type {@code int}. */
    private int patternLength;

    /** primitive lane for source field {@code segmentCount} of type {@code int}. */
    private int segmentCount;

    /** primitive lane for source field {@code occurrenceCount} of type {@code int}. */
    private int occurrenceCount;

    /** primitive lane for source field {@code positions} of type {@code int[]}. */
    private int[] positions;

    /** primitive lane for source field {@code atomRelationLookups} of type {@code int}. */
    private int atomRelationLookups;

    /** primitive lane for source field {@code reusedAtomOccurrences} of type {@code int}. */
    private int reusedAtomOccurrences;

    /** primitive lane for source field {@code boundaryOccurrences} of type {@code int}. */
    private int boundaryOccurrences;

    /** primitive lane for source field {@code boundaryCodeUnitsScanned} of type {@code long}. */
    private long boundaryCodeUnitsScanned;

    /** primitive lane for source field {@code retainedPrimitiveBytes} of type {@code long}. */
    private long retainedPrimitiveBytes;

    /** int handle lane (offsets into parent-owned storage) for source field {@code structuralSha256} of type {@code String}. */
    private int[] structuralSha256Lane;

    private M3CompositeSearchImage() {
        throw new M3NotYetReceived("M3CompositeSearchImage#<init>/0");
    }

    public int sourceLength() {
        throw new M3NotYetReceived("M3CompositeSearchImage#sourceLength/0");
    }

    public int patternLength() {
        throw new M3NotYetReceived("M3CompositeSearchImage#patternLength/0");
    }

    public int segmentCount() {
        throw new M3NotYetReceived("M3CompositeSearchImage#segmentCount/0");
    }

    public int occurrenceCount() {
        throw new M3NotYetReceived("M3CompositeSearchImage#occurrenceCount/0");
    }

    public boolean contains() {
        throw new M3NotYetReceived("M3CompositeSearchImage#contains/0");
    }

    public int atomRelationLookups() {
        throw new M3NotYetReceived("M3CompositeSearchImage#atomRelationLookups/0");
    }

    public int reusedAtomOccurrences() {
        throw new M3NotYetReceived("M3CompositeSearchImage#reusedAtomOccurrences/0");
    }

    public int boundaryOccurrences() {
        throw new M3NotYetReceived("M3CompositeSearchImage#boundaryOccurrences/0");
    }

    public long boundaryCodeUnitsScanned() {
        throw new M3NotYetReceived("M3CompositeSearchImage#boundaryCodeUnitsScanned/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3CompositeSearchImage#retainedPrimitiveBytes/0");
    }

    public String structuralSha256() {
        throw new M3NotYetReceived("M3CompositeSearchImage#structuralSha256/0");
    }

    public int firstOccurrence() {
        throw new M3NotYetReceived("M3CompositeSearchImage#firstOccurrence/0");
    }

    public int lastOccurrence() {
        throw new M3NotYetReceived("M3CompositeSearchImage#lastOccurrence/0");
    }

    public int occurrenceAt(int ordinal) {
        throw new M3NotYetReceived("M3CompositeSearchImage#occurrenceAt/1");
    }

    public int[] occurrences() {
        throw new M3NotYetReceived("M3CompositeSearchImage#occurrences/0");
    }

    public int nextOccurrenceAtOrAfter(int fromIndex) {
        throw new M3NotYetReceived("M3CompositeSearchImage#nextOccurrenceAtOrAfter/1");
    }

    public int previousOccurrenceAtOrBefore(int fromIndex) {
        throw new M3NotYetReceived("M3CompositeSearchImage#previousOccurrenceAtOrBefore/1");
    }
}
