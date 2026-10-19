// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

import java.util.function.Consumer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexFrozenMultiMap}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexFrozenMultiMap.java}
 * blob {@code aa4a0700934cfebb3743cf367ee5c31a73a99519} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code c52ecb3e398866d04f57daddca04ecdbf514ea3cd5ab16360be73b214bdba322}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3FrozenMultiMap}); projection
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
 *   <li>{@code source supertypes not received: implements MIndexCanonicalCollection}</li>
 *   <li>{@code member copyOf(MIndexMultiMap<K, V>, MIndexCompositeIndex) (parameter source)}</li>
 *   <li>{@code member ofIds(MIndexSpace<K>, MIndexSpace<V>, MIndexCompositeIndex, int[]) (parameter keySpace)}</li>
 *   <li>{@code member keySpace() (return type)}</li>
 *   <li>{@code member valueSpace() (return type)}</li>
 *   <li>{@code member compositeIndex() (return type)}</li>
 *   <li>{@code member forEachId(MIndexMap.IdEntryConsumer) (parameter action)}</li>
 *   <li>{@code member mutableCopy() (return type)}</li>
 * </ul>
 */
public final class M3FrozenMultiMap<K, V> {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code keySpace} of type {@code MIndexSpace<K>}. */
    private int[] keySpaceLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code valueSpace} of type {@code MIndexSpace<V>}. */
    private int[] valueSpaceLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code index} of type {@code MIndexCompositeIndex}. */
    private int[] indexLane;

    /** primitive lane for source field {@code canonicalId} of type {@code int}. */
    private int canonicalId;

    private M3FrozenMultiMap() {
        throw new M3NotYetReceived("M3FrozenMultiMap#<init>/0");
    }

    public int canonicalId() {
        throw new M3NotYetReceived("M3FrozenMultiMap#canonicalId/0");
    }

    public int kind() {
        throw new M3NotYetReceived("M3FrozenMultiMap#kind/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3FrozenMultiMap#size/0");
    }

    public boolean isEmpty() {
        throw new M3NotYetReceived("M3FrozenMultiMap#isEmpty/0");
    }

    public int keyCount() {
        throw new M3NotYetReceived("M3FrozenMultiMap#keyCount/0");
    }

    public boolean containsKey(K key) {
        throw new M3NotYetReceived("M3FrozenMultiMap#containsKey/1");
    }

    public boolean containsEntry(K key, V value) {
        throw new M3NotYetReceived("M3FrozenMultiMap#containsEntry/2");
    }

    public int valueCount(K key) {
        throw new M3NotYetReceived("M3FrozenMultiMap#valueCount/1");
    }

    public M3FrozenMultiMap<K, V> with(K key, V value) {
        throw new M3NotYetReceived("M3FrozenMultiMap#with/2");
    }

    public M3FrozenMultiMap<K, V> withIds(int keyId, int valueId) {
        throw new M3NotYetReceived("M3FrozenMultiMap#withIds/2");
    }

    public M3FrozenMultiMap<K, V> without(K key, V value) {
        throw new M3NotYetReceived("M3FrozenMultiMap#without/2");
    }

    public M3FrozenMultiMap<K, V> withoutIds(int keyId, int valueId) {
        throw new M3NotYetReceived("M3FrozenMultiMap#withoutIds/2");
    }

    public M3FrozenMultiMap<K, V> withoutKey(K key) {
        throw new M3NotYetReceived("M3FrozenMultiMap#withoutKey/1");
    }

    public void forEachValue(K key, Consumer<? super V> action) {
        throw new M3NotYetReceived("M3FrozenMultiMap#forEachValue/2");
    }

    public int[] copyLane() {
        throw new M3NotYetReceived("M3FrozenMultiMap#copyLane/0");
    }

    public long structuralHash64() {
        throw new M3NotYetReceived("M3FrozenMultiMap#structuralHash64/0");
    }

    public long signal64() {
        throw new M3NotYetReceived("M3FrozenMultiMap#signal64/0");
    }

    public boolean equals(Object other) {
        throw new M3NotYetReceived("M3FrozenMultiMap#equals/1");
    }

    public int hashCode() {
        throw new M3NotYetReceived("M3FrozenMultiMap#hashCode/0");
    }
}
