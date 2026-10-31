// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

import java.util.function.BiConsumer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexBiMap}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexBiMap.java}
 * blob {@code c6a75f137904e4d67efe0a92cb1c1128bcf2d9fe} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code dd7d6c30bea930558e713d05fb1844a9cc1cd07e432083fa490a59af5e82f82b}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3BiMap}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
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
 *   <li>{@code member MIndexBiMap(MIndexSpace<K>, MIndexSpace<V>) (parameter keys)}</li>
 *   <li>{@code member MIndexBiMap(MIndexSpace<K>, MIndexSpace<V>, int) (parameter keys)}</li>
 *   <li>{@code member keySpace() (return type)}</li>
 *   <li>{@code member valueSpace() (return type)}</li>
 *   <li>{@code member forEachId(MIndexMap.IdEntryConsumer) (parameter action)}</li>
 *   <li>{@code member freeze(MIndexCompositeIndex) (parameter index)}</li>
 * </ul>
 */
public final class M3BiMap<K, V> {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code forward} of type {@code MIndexMap<K, V>}. */
    private int[] forwardLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code reverse} of type {@code MIndexMap<V, K>}. */
    private int[] reverseLane;

    private M3BiMap() {
        throw new M3NotYetReceived("M3BiMap#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3BiMap#size/0");
    }

    public boolean isEmpty() {
        throw new M3NotYetReceived("M3BiMap#isEmpty/0");
    }

    public boolean containsKey(K key) {
        throw new M3NotYetReceived("M3BiMap#containsKey/1");
    }

    public boolean containsKeyId(int keyId) {
        throw new M3NotYetReceived("M3BiMap#containsKeyId/1");
    }

    public boolean containsValue(V value) {
        throw new M3NotYetReceived("M3BiMap#containsValue/1");
    }

    public boolean containsValueId(int valueId) {
        throw new M3NotYetReceived("M3BiMap#containsValueId/1");
    }

    public V get(K key) {
        throw new M3NotYetReceived("M3BiMap#get/1");
    }

    public K keyForValue(V value) {
        throw new M3NotYetReceived("M3BiMap#keyForValue/1");
    }

    public int valueIdOrDefault(int keyId, int fallback) {
        throw new M3NotYetReceived("M3BiMap#valueIdOrDefault/2");
    }

    public int keyIdOrDefault(int valueId, int fallback) {
        throw new M3NotYetReceived("M3BiMap#keyIdOrDefault/2");
    }

    public boolean put(K key, V value) {
        throw new M3NotYetReceived("M3BiMap#put/2");
    }

    public boolean putIds(int keyId, int valueId) {
        throw new M3NotYetReceived("M3BiMap#putIds/2");
    }

    public boolean forcePut(K key, V value) {
        throw new M3NotYetReceived("M3BiMap#forcePut/2");
    }

    public boolean forcePutIds(int keyId, int valueId) {
        throw new M3NotYetReceived("M3BiMap#forcePutIds/2");
    }

    public boolean replaceIds(int keyId, int expectedValueId, int newValueId) {
        throw new M3NotYetReceived("M3BiMap#replaceIds/3");
    }

    public boolean remove(K key) {
        throw new M3NotYetReceived("M3BiMap#remove/1");
    }

    public boolean removeKeyId(int keyId) {
        throw new M3NotYetReceived("M3BiMap#removeKeyId/1");
    }

    public boolean removeIds(int keyId, int expectedValueId) {
        throw new M3NotYetReceived("M3BiMap#removeIds/2");
    }

    public M3BiMap<V, K> inverse() {
        throw new M3NotYetReceived("M3BiMap#inverse/0");
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        throw new M3NotYetReceived("M3BiMap#forEach/1");
    }

    public int[] snapshotSortedLane() {
        throw new M3NotYetReceived("M3BiMap#snapshotSortedLane/0");
    }

    public void clear() {
        throw new M3NotYetReceived("M3BiMap#clear/0");
    }

    public void compact() {
        throw new M3NotYetReceived("M3BiMap#compact/0");
    }
}
