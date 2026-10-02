/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.PackedRingAtoms
 * at Synexia PR #7675 head 8fa47689547a1efeabbb35d2caf1683d902a87e4.
 */
package com.m3.collections;

/** Shared zero-object scalar arithmetic for primitive circular-array collections. */
final class M3PackedRingAtoms {
    private M3PackedRingAtoms() { }

    static int powerOfTwoIndex(int head, int logical, int capacity) {
        return (head + logical) & (capacity - 1);
    }

    static int powerOfTwoPrevious(int index, int capacity) {
        return (index - 1) & (capacity - 1);
    }

    static int powerOfTwoNext(int index, int capacity) {
        return (index + 1) & (capacity - 1);
    }

    static int normalizeHeadAfterRemoval(int head, int size) {
        return size == 0 ? 0 : head;
    }
}
