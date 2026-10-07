// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
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

    static int index(int head, int logical, int capacity) {
        int physical = head + logical;
        return physical >= capacity ? physical - capacity : physical;
    }

    static int next(int index, int capacity) {
        int candidate = index + 1;
        return candidate == capacity ? 0 : candidate;
    }

    static int normalizeHeadAfterRemoval(int head, int size) {
        return size == 0 ? 0 : head;
    }
}
