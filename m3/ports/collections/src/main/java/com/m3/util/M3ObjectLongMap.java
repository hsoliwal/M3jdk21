// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexObjectLongMap}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/collections/MIndexObjectLongMap.java}
 * blob {@code 8e4c23c0dafb28d43707f042a2d0e6e211530531} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 12663b9886e249a655f20a32371bf1ba2ffffb84e0a95478703d1af48d913b03}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3ObjectLongMap}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S03}.
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
 *   <li>{@code constant MAX_ENTRIES (int)}</li>
 *   <li>{@code member putIndexed(MIndexObject<?>, long) (parameter key)}</li>
 *   <li>{@code member getOrDefaultIndexed(MIndexObject<?>, long) (parameter key)}</li>
 *   <li>{@code member containsKeyIndexed(MIndexObject<?>) (parameter key)}</li>
 *   <li>{@code member removeIndexed(MIndexObject<?>) (parameter key)}</li>
 *   <li>{@code member forEach(ObjectLongConsumer) (parameter visitor)}</li>
 * </ul>
 */
public final class M3ObjectLongMap {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code index} of type {@code DenseObjectIndex}. */
    private int[] indexLane;

    /** primitive lane for source field {@code values} of type {@code long[]}. */
    private long[] values;

    public M3ObjectLongMap() {
        throw new M3NotYetReceived("M3ObjectLongMap#<init>/0");
    }

    public M3ObjectLongMap(int maxEntries) {
        throw new M3NotYetReceived("M3ObjectLongMap#<init>/1");
    }

    public int size() {
        throw new M3NotYetReceived("M3ObjectLongMap#size/0");
    }

    public boolean isEmpty() {
        throw new M3NotYetReceived("M3ObjectLongMap#isEmpty/0");
    }

    public int maxEntries() {
        throw new M3NotYetReceived("M3ObjectLongMap#maxEntries/0");
    }

    public long put(Object key, long value) {
        throw new M3NotYetReceived("M3ObjectLongMap#put/2");
    }

    public long getOrDefault(Object key, long fallback) {
        throw new M3NotYetReceived("M3ObjectLongMap#getOrDefault/2");
    }

    public boolean containsKey(Object key) {
        throw new M3NotYetReceived("M3ObjectLongMap#containsKey/1");
    }

    public long remove(Object key) {
        throw new M3NotYetReceived("M3ObjectLongMap#remove/1");
    }

    public Object keyAt(int row) {
        throw new M3NotYetReceived("M3ObjectLongMap#keyAt/1");
    }

    public long valueAt(int row) {
        throw new M3NotYetReceived("M3ObjectLongMap#valueAt/1");
    }

    public void clear() {
        throw new M3NotYetReceived("M3ObjectLongMap#clear/0");
    }
}
