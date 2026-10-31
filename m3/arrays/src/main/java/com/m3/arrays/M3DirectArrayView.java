// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.nio.ByteBuffer;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexDirectArrayView}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexDirectArrayView.java}
 * blob {@code 6cbef8b42871f5e9f21343e5876bcafe31b8bc3b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 560e2026aa8f41885661eb0533f487244fc63f5290bfda54ce83bcf7b98c1593}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3DirectArrayView}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
 * Map family {@code ARRAYS}; naming-law family {@code ARRAYS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: ElementType, Operation, Stage, Segment, RangeConsumer, RangeSource, SelectedSegmentConsumer}</li>
 *   <li>{@code member of(ElementType, Segment...) (parameter type)}</li>
 *   <li>{@code member elementType() (return type)}</li>
 *   <li>{@code member selectAvailability(MindexAvailabilityMask) (parameter visible)}</li>
 *   <li>{@code member selectAvailability(MindexAvailabilityMask, long) (parameter visible)}</li>
 *   <li>{@code member transformTo(ByteBuffer, int, Stage...) (parameter stages)}</li>
 * </ul>
 */
public final class M3DirectArrayView {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code type} of type {@code ElementType}. */
    private int[] typeLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code arenas} of type {@code ByteBuffer[]}. */
    private int[] arenasLane;

    /** primitive lane for source field {@code ordinals} of type {@code int[]}. */
    private int[] ordinals;

    /** primitive lane for source field {@code byteOffsets} of type {@code int[]}. */
    private int[] byteOffsets;

    /** primitive lane for source field {@code elementCounts} of type {@code int[]}. */
    private int[] elementCounts;

    /** primitive lane for source field {@code length} of type {@code int}. */
    private int length;

    private M3DirectArrayView() {
        throw new M3NotYetReceived("M3DirectArrayView#<init>/0");
    }

    public static M3DirectArrayView compose(M3DirectArrayView... views) {
        throw new M3NotYetReceived("M3DirectArrayView#compose/1");
    }

    public int length() {
        throw new M3NotYetReceived("M3DirectArrayView#length/0");
    }

    public int segmentCount() {
        throw new M3NotYetReceived("M3DirectArrayView#segmentCount/0");
    }

    public M3DirectArrayView slice(int from, int count) {
        throw new M3NotYetReceived("M3DirectArrayView#slice/2");
    }

    public M3DirectArrayView first(int count) {
        throw new M3NotYetReceived("M3DirectArrayView#first/1");
    }

    public M3DirectArrayView last(int count) {
        throw new M3NotYetReceived("M3DirectArrayView#last/1");
    }

    public M3DirectArrayView subspan(int from) {
        throw new M3NotYetReceived("M3DirectArrayView#subspan/1");
    }

    public M3DirectArrayView subspan(int from, int count) {
        throw new M3NotYetReceived("M3DirectArrayView#subspan/2");
    }

    public M3DirectArrayView rotateLeft(int distance) {
        throw new M3NotYetReceived("M3DirectArrayView#rotateLeft/1");
    }

    public long retainedPrimitiveDescriptorBytes() {
        throw new M3NotYetReceived("M3DirectArrayView#retainedPrimitiveDescriptorBytes/0");
    }

    public M3DirectArrayView selectRanges(int... ranges) {
        throw new M3NotYetReceived("M3DirectArrayView#selectRanges/1");
    }

    public M3DirectArrayView selectRanges(long maxDescriptorBytes, int... ranges) {
        throw new M3NotYetReceived("M3DirectArrayView#selectRanges/2");
    }

    public ByteBuffer[] asReadOnlyBuffers() {
        throw new M3NotYetReceived("M3DirectArrayView#asReadOnlyBuffers/0");
    }

    public void copyTo(ByteBuffer target, int targetElementOffset) {
        throw new M3NotYetReceived("M3DirectArrayView#copyTo/2");
    }
}
