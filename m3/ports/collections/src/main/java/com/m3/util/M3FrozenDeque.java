// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexFrozenDeque}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexFrozenDeque.java}
 * blob {@code 10c8291f02041304da474df96295352df9021407} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 634929d4bbfb68d67b4ceecc6ff9407d8623dee83121db73078f45a796c86ada}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3FrozenDeque}); projection
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
 *   <li>{@code member copyOf(MIndexDeque<E>, MIndexCompositeIndex) (parameter index)}</li>
 *   <li>{@code member ofIds(MIndexSpace<E>, MIndexCompositeIndex, int[]) (parameter space)}</li>
 *   <li>{@code member space() (return type)}</li>
 *   <li>{@code member compositeIndex() (return type)}</li>
 * </ul>
 */
public final class M3FrozenDeque<E> {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code space} of type {@code MIndexSpace<E>}. */
    private int[] spaceLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code index} of type {@code MIndexCompositeIndex}. */
    private int[] indexLane;

    /** primitive lane for source field {@code canonicalId} of type {@code int}. */
    private int canonicalId;

    private M3FrozenDeque() {
        throw new M3NotYetReceived("M3FrozenDeque#<init>/0");
    }

    public int canonicalId() {
        throw new M3NotYetReceived("M3FrozenDeque#canonicalId/0");
    }

    public int kind() {
        throw new M3NotYetReceived("M3FrozenDeque#kind/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3FrozenDeque#size/0");
    }

    public boolean isEmpty() {
        throw new M3NotYetReceived("M3FrozenDeque#isEmpty/0");
    }

    public E first() {
        throw new M3NotYetReceived("M3FrozenDeque#first/0");
    }

    public int firstId() {
        throw new M3NotYetReceived("M3FrozenDeque#firstId/0");
    }

    public E last() {
        throw new M3NotYetReceived("M3FrozenDeque#last/0");
    }

    public int lastId() {
        throw new M3NotYetReceived("M3FrozenDeque#lastId/0");
    }

    public M3FrozenDeque<E> withFirst(E value) {
        throw new M3NotYetReceived("M3FrozenDeque#withFirst/1");
    }

    public M3FrozenDeque<E> withFirstId(int id) {
        throw new M3NotYetReceived("M3FrozenDeque#withFirstId/1");
    }

    public M3FrozenDeque<E> withLast(E value) {
        throw new M3NotYetReceived("M3FrozenDeque#withLast/1");
    }

    public M3FrozenDeque<E> withLastId(int id) {
        throw new M3NotYetReceived("M3FrozenDeque#withLastId/1");
    }

    public M3FrozenDeque<E> withoutFirst() {
        throw new M3NotYetReceived("M3FrozenDeque#withoutFirst/0");
    }

    public M3FrozenDeque<E> withoutLast() {
        throw new M3NotYetReceived("M3FrozenDeque#withoutLast/0");
    }

    public void forEach(Consumer<? super E> action) {
        throw new M3NotYetReceived("M3FrozenDeque#forEach/1");
    }

    public void forEachId(IntConsumer action) {
        throw new M3NotYetReceived("M3FrozenDeque#forEachId/1");
    }

    public int[] copyIds() {
        throw new M3NotYetReceived("M3FrozenDeque#copyIds/0");
    }

    public M3Deque<E> mutableCopy() {
        throw new M3NotYetReceived("M3FrozenDeque#mutableCopy/0");
    }

    public long structuralHash64() {
        throw new M3NotYetReceived("M3FrozenDeque#structuralHash64/0");
    }

    public long signal64() {
        throw new M3NotYetReceived("M3FrozenDeque#signal64/0");
    }

    public boolean equals(Object other) {
        throw new M3NotYetReceived("M3FrozenDeque#equals/1");
    }

    public int hashCode() {
        throw new M3NotYetReceived("M3FrozenDeque#hashCode/0");
    }
}
