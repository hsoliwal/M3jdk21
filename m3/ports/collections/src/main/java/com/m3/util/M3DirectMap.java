// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

import java.util.function.BiConsumer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collection.MIndexDirectMap}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/collection/MIndexDirectMap.java}
 * blob {@code 34a7719f3e77be517b1e755b70dffd6d0fdce339} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b82a7e6d2044bc992bbb324fede60edd455a6c9bb88dc4acc087af49ac19f0bf}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collection.M3DirectMap}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S02}.
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
 *   <li>{@code source supertypes not received: implements MIndexMap<K, V>, implements MIndexIdentityMap}</li>
 *   <li>{@code member forEachIdentity(MIndexIdentityMap.IntIntConsumer) (parameter action)}</li>
 * </ul>
 */
public final class M3DirectMap<K, V> {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code keyDomain} of type {@code MIndexDomain<K>}. */
    private int[] keyDomainLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code valueDomain} of type {@code MIndexDomain<V>}. */
    private int[] valueDomainLane;

    /** primitive lane for source field {@code valuesByKey} of type {@code int[]}. */
    private int[] valuesByKey;

    /** primitive lane for source field {@code size} of type {@code int}. */
    private int size;

    public M3DirectMap(M3Domain<K> keyDomain, M3Domain<V> valueDomain, int expectedMaximumIdentity) {
        throw new M3NotYetReceived("M3DirectMap#<init>/3");
    }

    public M3Domain<K> keyDomain() {
        throw new M3NotYetReceived("M3DirectMap#keyDomain/0");
    }

    public M3Domain<V> valueDomain() {
        throw new M3NotYetReceived("M3DirectMap#valueDomain/0");
    }

    public V put(K key, V value) {
        throw new M3NotYetReceived("M3DirectMap#put/2");
    }

    public V get(K key) {
        throw new M3NotYetReceived("M3DirectMap#get/1");
    }

    public boolean containsKey(K key) {
        throw new M3NotYetReceived("M3DirectMap#containsKey/1");
    }

    public V remove(K key) {
        throw new M3NotYetReceived("M3DirectMap#remove/1");
    }

    public int size() {
        throw new M3NotYetReceived("M3DirectMap#size/0");
    }

    public void clear() {
        throw new M3NotYetReceived("M3DirectMap#clear/0");
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        throw new M3NotYetReceived("M3DirectMap#forEach/1");
    }

    public String storageKind() {
        throw new M3NotYetReceived("M3DirectMap#storageKind/0");
    }

    public void compact() {
        throw new M3NotYetReceived("M3DirectMap#compact/0");
    }
}
