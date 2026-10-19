// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.foundation.MIndexMonotoneLongDeque}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation/MIndexMonotoneLongDeque.java}
 * blob {@code 4ec10eec0a0717695c8475bc57d3e3b551e6c656} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code d6e8c7cc7b82aa8ec0674d81f5d7297b58402a589c849fa1bc4d8562894081a8}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.foundation.M3MonotoneLongDeque}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S04}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Mode}</li>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member MIndexMonotoneLongDeque(int, Mode) (parameter mode)}</li>
 *   <li>{@code member windowMaximum(int[], int, int[], MIndexExecutionContext) (parameter context)}</li>
 * </ul>
 */
public final class M3MonotoneLongDeque {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code values} of type {@code long[]}. */
    private long[] values;

    /** primitive lane for source field {@code indices} of type {@code int[]}. */
    private int[] indices;

    /** int handle lane (offsets into parent-owned storage) for source field {@code mode} of type {@code Mode}. */
    private int[] modeLane;

    /** primitive lane for source field {@code head} of type {@code int}. */
    private int head;

    /** primitive lane for source field {@code size} of type {@code int}. */
    private int size;

    /** primitive lane for source field {@code lastIndex} of type {@code int}. */
    private int lastIndex;

    private M3MonotoneLongDeque() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#<init>/0");
    }

    public int capacity() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#capacity/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#size/0");
    }

    public boolean isEmpty() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#isEmpty/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#retainedPrimitiveBytes/0");
    }

    public void clear() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#clear/0");
    }

    public void evictBefore(int minimumIndexInclusive) {
        throw new M3NotYetReceived("M3MonotoneLongDeque#evictBefore/1");
    }

    public void offer(int index, long value) {
        throw new M3NotYetReceived("M3MonotoneLongDeque#offer/2");
    }

    public int firstIndex() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#firstIndex/0");
    }

    public long firstValue() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#firstValue/0");
    }

    public int lastIndex() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#lastIndex/0");
    }

    public long lastValue() {
        throw new M3NotYetReceived("M3MonotoneLongDeque#lastValue/0");
    }
}
