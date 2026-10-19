// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexLongSelect}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/data-structure/src/main/java/com/synexia/mindex/structure/MIndexLongSelect.java}
 * blob {@code 7a3633429df36e24596132a45bf5a2c56bcb712d} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 736756a2cc8008e2f1b7535576592eb106063e72b6b8ded1418169fc7c943708}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3LongSelect}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S06}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3LongSelect {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3LongSelect() {
        throw new M3NotYetReceived("M3LongSelect#<init>/0");
    }

    public static long kthSmallest(long[] values, int ordinal) {
        throw new M3NotYetReceived("M3LongSelect#kthSmallest/2");
    }

    public static long kthSmallest(long[] values, int fromInclusive, int toExclusive, int ordinal) {
        throw new M3NotYetReceived("M3LongSelect#kthSmallest/4");
    }

    public static long kthSmallestPreservingInput(long[] values, int ordinal, M3LongSelectWorkspace workspace) {
        throw new M3NotYetReceived("M3LongSelect#kthSmallestPreservingInput/3");
    }

    public static long kthLargestPreservingInput(long[] values, int ordinal, M3LongSelectWorkspace workspace) {
        throw new M3NotYetReceived("M3LongSelect#kthLargestPreservingInput/3");
    }

    public static long kthLargest(long[] values, int ordinal) {
        throw new M3NotYetReceived("M3LongSelect#kthLargest/2");
    }
}
