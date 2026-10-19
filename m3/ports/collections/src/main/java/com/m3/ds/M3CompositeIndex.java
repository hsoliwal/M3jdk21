// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexCompositeIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexCompositeIndex.java}
 * blob {@code 0e893c7db9e622578ea7fefb40b11d12963bf77d} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 63036d4b785e3a2c096ccbc3eefd2e4803e793639da2b047bfda81376f450886}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3CompositeIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S05}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3CompositeIndex {
    public static final int KIND_LIST = 1;

    public static final int KIND_SET = 2;

    public static final int KIND_MAP = 3;

    public static final int KIND_TUPLE = 4;

    public static final int KIND_GRAPH_EDGE_SET = 5;

    public static final int KIND_TABLE = 6;

    public static final int KIND_DEQUE = 7;

    public static final int KIND_MULTI_MAP = 8;

    public static final int KIND_PRIORITY_QUEUE = 9;

    public static final int KIND_SHAPE = 10;

    public static final int KIND_BAG = 11;

    public static final int KIND_ORDERED_SET = 12;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code domainIds} of type {@code Map<Object, Integer>}. */
    private int[] domainIdsLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code domainRepresentatives} of type {@code Map<Object, Object>}. */
    private int[] domainRepresentativesLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code compositeSpace} of type {@code MIndexCompositeSpace}. */
    private int[] compositeSpaceLane;

    /** primitive lane for source field {@code nextDomainId} of type {@code int}. */
    private int nextDomainId;

    /** primitive lane for source field {@code arena} of type {@code int[]}. */
    private int[] arena;

    /** primitive lane for source field {@code arenaSize} of type {@code int}. */
    private int arenaSize;

    /** primitive lane for source field {@code offsets} of type {@code int[]}. */
    private int[] offsets;

    /** primitive lane for source field {@code lengths} of type {@code int[]}. */
    private int[] lengths;

    /** primitive lane for source field {@code kinds} of type {@code int[]}. */
    private int[] kinds;

    /** primitive lane for source field {@code primaryDomains} of type {@code int[]}. */
    private int[] primaryDomains;

    /** primitive lane for source field {@code secondaryDomains} of type {@code int[]}. */
    private int[] secondaryDomains;

    /** primitive lane for source field {@code hashes} of type {@code long[]}. */
    private long[] hashes;

    /** primitive lane for source field {@code signals} of type {@code long[]}. */
    private long[] signals;

    /** primitive lane for source field {@code table} of type {@code int[]}. */
    private int[] table;

    /** primitive lane for source field {@code size} of type {@code int}. */
    private int size;

    public M3CompositeIndex() {
        throw new M3NotYetReceived("M3CompositeIndex#<init>/0");
    }

    public int intern(int kind, int[] lane) {
        throw new M3NotYetReceived("M3CompositeIndex#intern/2");
    }

    public int intern(int kind, Object primaryDomain, Object secondaryDomain, int[] lane) {
        throw new M3NotYetReceived("M3CompositeIndex#intern/4");
    }

    public int findId(int kind, Object primaryDomain, Object secondaryDomain, int[] lane) {
        throw new M3NotYetReceived("M3CompositeIndex#findId/4");
    }

    public int size() {
        throw new M3NotYetReceived("M3CompositeIndex#size/0");
    }

    public boolean containsId(int id) {
        throw new M3NotYetReceived("M3CompositeIndex#containsId/1");
    }

    public M3CompositeSpace space() {
        throw new M3NotYetReceived("M3CompositeIndex#space/0");
    }

    public static int[] builtInKinds() {
        throw new M3NotYetReceived("M3CompositeIndex#builtInKinds/0");
    }

    public static boolean isBuiltInKind(int kind) {
        throw new M3NotYetReceived("M3CompositeIndex#isBuiltInKind/1");
    }

    public static String kindName(int kind) {
        throw new M3NotYetReceived("M3CompositeIndex#kindName/1");
    }

    public int kind(int id) {
        throw new M3NotYetReceived("M3CompositeIndex#kind/1");
    }

    public int length(int id) {
        throw new M3NotYetReceived("M3CompositeIndex#length/1");
    }

    public int valueAt(int id, int index) {
        throw new M3NotYetReceived("M3CompositeIndex#valueAt/2");
    }

    public int[] copyLane(int id) {
        throw new M3NotYetReceived("M3CompositeIndex#copyLane/1");
    }

    public long structuralHash64(int id) {
        throw new M3NotYetReceived("M3CompositeIndex#structuralHash64/1");
    }

    public long signal64(int id) {
        throw new M3NotYetReceived("M3CompositeIndex#signal64/1");
    }

    public boolean sameCanonical(int leftId, int rightId) {
        throw new M3NotYetReceived("M3CompositeIndex#sameCanonical/2");
    }
}
