// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexIntDeque}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/data-structure/src/main/java/com/synexia/mindex/structure/MIndexIntDeque.java}
 * blob {@code 4e0e49870be05fee8aaf69c11a50b113084b7e35} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 5765a635558a6387a87a3ee5584dcf112beb775e97822561d12798a934d96628}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3IntDeque}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S02}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3IntDeque {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code values} of type {@code int[]}. */
    private int[] values;

    /** primitive lane for source field {@code head} of type {@code int}. */
    private int head;

    /** primitive lane for source field {@code size} of type {@code int}. */
    private int size;

    public M3IntDeque() {
        throw new M3NotYetReceived("M3IntDeque#<init>/0");
    }

    public M3IntDeque(int capacity) {
        throw new M3NotYetReceived("M3IntDeque#<init>/1");
    }

    public int size() {
        throw new M3NotYetReceived("M3IntDeque#size/0");
    }

    public boolean isEmpty() {
        throw new M3NotYetReceived("M3IntDeque#isEmpty/0");
    }

    public void addFirst(int value) {
        throw new M3NotYetReceived("M3IntDeque#addFirst/1");
    }

    public void addLast(int value) {
        throw new M3NotYetReceived("M3IntDeque#addLast/1");
    }

    public int peekFirst() {
        throw new M3NotYetReceived("M3IntDeque#peekFirst/0");
    }

    public int peekLast() {
        throw new M3NotYetReceived("M3IntDeque#peekLast/0");
    }

    public int removeFirst() {
        throw new M3NotYetReceived("M3IntDeque#removeFirst/0");
    }

    public int removeLast() {
        throw new M3NotYetReceived("M3IntDeque#removeLast/0");
    }

    public void clear() {
        throw new M3NotYetReceived("M3IntDeque#clear/0");
    }
}
