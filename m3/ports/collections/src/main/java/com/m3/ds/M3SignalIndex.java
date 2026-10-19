// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.projection.MIndexSignalIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/projection/MIndexSignalIndex.java}
 * blob {@code 6491a03ffe2bc9d135a918aaf915d43ff37407f1} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code be0fd0fcabdb56b6ea70ba4d213b85d8ea04a8a0c5d9a44b20138f92e27f5fa7}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.projection.M3SignalIndex}); projection
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
 *   <li>{@code nested types not received: Layer, Signal, Scope, Limits, Query, Term, PreparedTerm, Junction, SameAnalysis, Key, Ints, Builder, Selection, PostingProbeBackend, PreparedQuery, Visit, FallbackSelection, Rows}</li>
 *   <li>{@code member equal(Signal, long) (return type)}</li>
 *   <li>{@code member between(Signal, long, long) (return type)}</li>
 *   <li>{@code member all(Query...) (return type)}</li>
 *   <li>{@code member any(Query...) (return type)}</li>
 *   <li>{@code member sameAnalysis(Query) (return type)}</li>
 *   <li>{@code member scope() (return type)}</li>
 *   <li>{@code member allValues(IProgressMonitor) (return type)}</li>
 *   <li>{@code member selectRows(int[], IProgressMonitor) (return type)}</li>
 *   <li>{@code member select(Query, IProgressMonitor) (return type)}</li>
 *   <li>{@code member select(Query, PostingProbeBackend, IProgressMonitor) (return type)}</li>
 *   <li>{@code member prepare(Query, IProgressMonitor) (return type)}</li>
 *   <li>{@code member selectWithin(Query, Selection, IProgressMonitor) (return type)}</li>
 *   <li>{@code member selectWithin(Query, Selection, PostingProbeBackend, IProgressMonitor) (return type)}</li>
 *   <li>{@code member prepareSurfaceFallback(Selection, int, IntUnaryOperator, IntPredicate, IProgressMonitor) (return type)}</li>
 *   <li>{@code member admitIds(long[], IProgressMonitor) (return type)}</li>
 *   <li>{@code member admitRows(int[], IProgressMonitor) (return type)}</li>
 * </ul>
 */
public final class M3SignalIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code scope} of type {@code Scope}. */
    private int[] scopeLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code limits} of type {@code Limits}. */
    private int[] limitsLane;

    /** primitive lane for source field {@code ids} of type {@code long[]}. */
    private long[] ids;

    /** primitive lane for source field {@code sortedIds} of type {@code long[]}. */
    private long[] sortedIds;

    /** primitive lane for source field {@code values} of type {@code long[]}. */
    private long[] values;

    /** int handle lane (offsets into parent-owned storage) for source field {@code contentSha256} of type {@code String}. */
    private int[] contentSha256Lane;

    /** primitive lane for source field {@code sortedRows} of type {@code int[]}. */
    private int[] sortedRows;

    /** primitive lane for source field {@code analysisOwners} of type {@code int[]}. */
    private int[] analysisOwners;

    /** primitive lane for source field {@code kinds} of type {@code int[]}. */
    private int[] kinds;

    /** primitive lane for source field {@code starts} of type {@code int[]}. */
    private int[] starts;

    /** primitive lane for source field {@code postings} of type {@code int[]}. */
    private int[] postings;

    private M3SignalIndex() {
        throw new M3NotYetReceived("M3SignalIndex#<init>/0");
    }

    public String contentSha256() {
        throw new M3NotYetReceived("M3SignalIndex#contentSha256/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3SignalIndex#size/0");
    }

    public int analysisCount() {
        throw new M3NotYetReceived("M3SignalIndex#analysisCount/0");
    }

    public int indexedTermCount() {
        throw new M3NotYetReceived("M3SignalIndex#indexedTermCount/0");
    }

    public long valueIdAt(int row) {
        throw new M3NotYetReceived("M3SignalIndex#valueIdAt/1");
    }

    public long primitivePayloadBytes() {
        throw new M3NotYetReceived("M3SignalIndex#primitivePayloadBytes/0");
    }

    public int rowOf(long valueId) {
        throw new M3NotYetReceived("M3SignalIndex#rowOf/1");
    }
}
